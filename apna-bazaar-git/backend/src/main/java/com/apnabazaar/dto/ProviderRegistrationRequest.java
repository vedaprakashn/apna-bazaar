package com.apnabazaar.dto;
import java.util.List;
public record ProviderRegistrationRequest(
    String name, String shopName, String flatNumber,
    String whatsappNumber, String providerType, List<OfferingRequest> offerings) {}
