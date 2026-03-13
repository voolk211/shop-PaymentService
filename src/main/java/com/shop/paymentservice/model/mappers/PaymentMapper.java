package com.shop.paymentservice.model.mappers;

import com.shop.paymentservice.model.dto.PaymentDto;
import com.shop.paymentservice.model.entities.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentDto toDto(Payment payment);

    Payment toEntity(PaymentDto paymentDto);

}
