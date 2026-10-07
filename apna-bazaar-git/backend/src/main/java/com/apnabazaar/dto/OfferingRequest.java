package com.apnabazaar.dto;
import java.math.BigDecimal;
public record OfferingRequest(String name, String description,
    BigDecimal basePrice, String unit, String categoryName) {}
