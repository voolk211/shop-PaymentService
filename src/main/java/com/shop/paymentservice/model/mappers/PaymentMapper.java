package com.shop.paymentservice.model.mappers;

import com.shop.paymentservice.model.dto.PaymentCreateDto;
import com.shop.paymentservice.model.dto.PaymentResponseDto;
import com.shop.paymentservice.model.entities.Payment;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponseDto toResponseDto(Payment payment);

    Payment toEntity(PaymentCreateDto paymentDto);

    default Page<PaymentResponseDto> toResponseDto(Page<Payment> payments) {
        return payments.map(this::toResponseDto);
    }
}
