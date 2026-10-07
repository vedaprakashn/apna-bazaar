package com.apnabazaar.dto;
import java.math.BigDecimal;
public record TodayItemDto(String name, BigDecimal price, String pickupTime,
    String pickupLocation, String deliveryType, String orderingStatus) {}
