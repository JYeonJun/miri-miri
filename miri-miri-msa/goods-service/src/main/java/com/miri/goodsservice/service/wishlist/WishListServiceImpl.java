package com.miri.goodsservice.service.wishlist;

import com.miri.coremodule.dto.wishlist.FeignWishListRespDto.GoodsInWishListRespDto;
import com.miri.coremodule.dto.wishlist.FeignWishListRespDto.WishListOrderedRespDto;
import com.miri.coremodule.dto.wishlist.FeignWishListRespDto.WishListRespDto;
import com.miri.coremodule.handler.ex.CustomApiException;
import com.miri.goodsservice.domain.goods.Goods;
import com.miri.goodsservice.domain.goods.GoodsRepository;
import com.miri.goodsservice.domain.wishlist.WishList;
import com.miri.goodsservice.domain.wishlist.WishListRepository;
import com.miri.goodsservice.dto.wishlist.RequestWishListDto.AddToCartReqDto;
import com.miri.goodsservice.dto.wishlist.ResponseWishListDto.AddToWishListRespDto;
import com.miri.goodsservice.dto.wishlist.ResponseWishListDto.WishListUpdateRespDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WishListServiceImpl implements WishListService {

    private final GoodsRepository goodsRepository;
    private final WishListRepository wishListRepository;

    public WishListServiceImpl(GoodsRepository goodsRepository,
                               WishListRepository wishListRepository) {
        this.goodsRepository = goodsRepository;
        this.wishListRepository = wishListRepository;
    }

    @Override
    @Transactional
    public AddToWishListRespDto addToWishList(Long userId, AddToCartReqDto addToCartReqDto) {

        // 장바구니에 같은 상품이 이미 있는지 확인 후 예외 발생
        if (wishListRepository.existsByUserIdAndGoods_Id(userId, addToCartReqDto.getGoodsId())) {
            throw new CustomApiException("장바구니에 동일한 상품이 존재합니다.");
        }

        Goods findGoods = findGoodsByIdOrThrow(addToCartReqDto.getGoodsId());

        WishList wishList
                = wishListRepository.save(new WishList(userId, findGoods, addToCartReqDto.getGoodsQuantity()));

        return new AddToWishListRespDto(findGoods, wishList);
    }

    @Override
    public WishListRespDto getWishListGoods(Long userId, Pageable pageable) {
        Page<GoodsInWishListRespDto> goodsInWishList
                = wishListRepository.findPagingGoodsInWishList(userId, pageable);

        return new WishListRespDto(goodsInWishList);
    }

    @Override
    @Transactional
    public WishListUpdateRespDto updateGoodsQuantityInWishList(
            Long userId, Long wishListId, int goodsQuantity) {

        WishList findWishList
                = wishListRepository.findByIdAndUserId(wishListId, userId)
                .orElseThrow(() -> new CustomApiException("유효하지 않은 상품 수량 변경 요청입니다."));

        findWishList.changeQuantity(goodsQuantity);

        return new WishListUpdateRespDto(findWishList.getId(), findWishList.getGoods().getId(), goodsQuantity);
    }

    @Override
    @Transactional
    public void deleteGoodsInWishList(Long userId, Long wishListId) {
        int count = wishListRepository.deleteByIdAndUserId(wishListId, userId);
        if (count == 0) {
            throw new CustomApiException("해당 위시리스 상품을 삭제할 권한이 없습니다.");
        }
    }

    @Override
    public List<WishListOrderedRespDto> getOrderedWishLists(Long userId, List<Long> wishListIds) {
        List<WishList> findWishLists = wishListRepository.findByIdInAndUserIdWithGoods(wishListIds, userId);
        return findWishLists.stream().map((wishList -> {
            Goods goods = wishList.getGoods();
            return WishListOrderedRespDto.builder()
                    .wishListId(wishList.getId())
                    .orderQuantity(wishList.getQuantity())
                    .goodsId(goods.getId())
                    .goodsName(goods.getGoodsName())
                    .unitPrice(goods.getGoodsPrice())
                    .reservationStartTime(goods.getReservationStartTime())
                    .build();
        })).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteOrderedWishLists(List<Long> wishListIds) {
        wishListRepository.deleteByIdIn(wishListIds);
    }

    private Goods findGoodsByIdOrThrow(Long goodsId) {
        return goodsRepository.findById(goodsId)
                .orElseThrow(() -> new CustomApiException("해당 상품이 존재하지 않습니다."));
    }
}
