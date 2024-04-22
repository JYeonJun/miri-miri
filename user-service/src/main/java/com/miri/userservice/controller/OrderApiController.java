package com.miri.userservice.controller;

import static com.miri.userservice.dto.order.RequestOrderDto.CreateOrderReqDto;

import com.miri.userservice.dto.common.ResponseDto;
import com.miri.userservice.dto.order.ResponseOrderDto.CreateOrderRespDto;
import com.miri.userservice.dto.order.ResponseOrderDto.OrderGoodsListRespDto;
import com.miri.userservice.security.PrincipalDetails;
import com.miri.userservice.service.order.OrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 주문 기능
    @PostMapping("/orders")
    public ResponseEntity<?> createOrder(@RequestBody @Valid CreateOrderReqDto reqDto,
                                         BindingResult bindingResult,
                                         @AuthenticationPrincipal PrincipalDetails principalDetails) {
        CreateOrderRespDto result = orderService.createOrder(principalDetails.getUser().getId(), reqDto);
        return new ResponseEntity<>(new ResponseDto<>(1, "주문이 완료되었습니다.", result), HttpStatus.CREATED);
    }

    @GetMapping("/orders")
    public ResponseEntity<?> getOrderGoodsList(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                               @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        OrderGoodsListRespDto result
                = orderService.getOrderGoodsList(principalDetails.getUser().getId(), pageable);
        return new ResponseEntity<>(new ResponseDto<>(1, "주문한 상품 목록 조회가 완료되었습니다.", result), HttpStatus.OK);
    }
}
