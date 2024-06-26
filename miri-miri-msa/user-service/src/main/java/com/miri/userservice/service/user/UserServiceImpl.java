package com.miri.userservice.service.user;

import com.miri.coremodule.dto.ResponseDto;
import com.miri.coremodule.dto.goods.FeignGoodsRespDto.RegisterGoodsListRespDto;
import com.miri.coremodule.dto.order.FeignOrderRespDto.OrderGoodsListRespDto;
import com.miri.coremodule.dto.wishlist.FeignWishListRespDto.WishListRespDto;
import com.miri.coremodule.handler.ex.CustomApiException;
import com.miri.userservice.client.GoodsServiceClient;
import com.miri.userservice.client.OrderServiceClient;
import com.miri.userservice.domain.email.EmailVerificationCode;
import com.miri.userservice.domain.email.EmailVerificationCodeRepository;
import com.miri.userservice.domain.user.User;
import com.miri.userservice.domain.user.UserRepository;
import com.miri.userservice.domain.user.UserRole;
import com.miri.userservice.dto.user.RequestUserDto.SignUpReqDto;
import com.miri.userservice.dto.user.RequestUserDto.UpdateUserPasswordReqDto;
import com.miri.userservice.dto.user.RequestUserDto.UpdateUserProfileReqDto;
import com.miri.userservice.dto.user.ResponseUserDto.GetUserRespDto;
import com.miri.userservice.dto.user.ResponseUserDto.UpdateUserProfileRespDto;
import com.miri.userservice.util.AESUtils;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmailVerificationCodeRepository emailVerificationCodeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AESUtils aesUtils;
    private final GoodsServiceClient goodsServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final AsyncUserService asyncUserService;

    public UserServiceImpl(UserRepository userRepository,
                           EmailVerificationCodeRepository emailVerificationCodeRepository,
                           BCryptPasswordEncoder passwordEncoder, AESUtils aesUtils,
                           GoodsServiceClient goodsServiceClient,
                           OrderServiceClient orderServiceClient, AsyncUserService asyncUserService) {
        this.userRepository = userRepository;
        this.emailVerificationCodeRepository = emailVerificationCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.aesUtils = aesUtils;
        this.goodsServiceClient = goodsServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.asyncUserService = asyncUserService;
    }

    @Override
    @Transactional
    public void createUser(SignUpReqDto signUpReqDto) {
        String userEmail = verifyEmailAndGetUserEmail(signUpReqDto.getEmailVerificationToken());
        createUserInRepository(signUpReqDto, userEmail);
    }

    private String verifyEmailAndGetUserEmail(String emailVerificationToken) {
        EmailVerificationCode emailVerificationCode = emailVerificationCodeRepository.findById(emailVerificationToken)
                .orElseThrow(() -> new CustomApiException("인증되지 않은 이메일입니다."));
        String email = emailVerificationCode.getEmail();
        emailVerificationCodeRepository.delete(emailVerificationCode);
        return aesUtils.decodeUnique(email);
    }

    private void createUserInRepository(SignUpReqDto signUpReqDto, String userEmail) {
        userRepository.save(User.builder()
                .email(userEmail)
                .userName(signUpReqDto.getUserName())
                .password(passwordEncoder.encode(signUpReqDto.getPassword()))
                .phoneNumber(signUpReqDto.getPhoneNumber())
                .address(signUpReqDto.getAddress())
                .role(UserRole.USER)
                .build());
    }

    @Override
    @Transactional
    public UpdateUserProfileRespDto updateUserProfile(Long userId, UpdateUserProfileReqDto userProfile) {

        User findUser = findUserByIdOrThrow(userId);
        findUser.changeUserProfile(userProfile.getPhoneNumber(), userProfile.getAddress());

        return new UpdateUserProfileRespDto(findUser);
    }

    @Override
    @Transactional
    public void updateUserPassword(Long userId, UpdateUserPasswordReqDto userPassword) {
        User findUser = findUserByIdOrThrow(userId);
        findUser.changePassword(passwordEncoder.encode(userPassword.getPassword()));
    }


    @Override
    public GetUserRespDto getUserInfo(Long userId) {
        User findUser = findUserByIdOrThrow(userId);

        CompletableFuture<RegisterGoodsListRespDto> registerGoodsListFuture = asyncUserService.getRegisteredGoodsListAsync(userId);
        CompletableFuture<WishListRespDto> wishListGoodsFuture = asyncUserService.getWishListGoodsAsync(userId);
        CompletableFuture<OrderGoodsListRespDto> orderGoodsListFuture = asyncUserService.getOrderGoodsListAsync(userId);

        CompletableFuture.allOf(registerGoodsListFuture, wishListGoodsFuture, orderGoodsListFuture).join();

        return new GetUserRespDto(
                findUser,
                registerGoodsListFuture.join(),
                wishListGoodsFuture.join(),
                orderGoodsListFuture.join()
        );
    }

    @Override
    public String findUserNameById(Long userId) {
        User findUser = findUserByIdOrThrow(userId);
        return findUser.getUserName();
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomApiException("해당 사용자가 존재하지 않습니다."));
    }
}
