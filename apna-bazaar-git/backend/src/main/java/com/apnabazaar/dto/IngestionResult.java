package com.apnabazaar.dto;
import java.util.List;
public record IngestionResult(int rowsProcessed, int rowsSkipped, int providersUpdated, int itemsCreated, List<String> errors) {}
