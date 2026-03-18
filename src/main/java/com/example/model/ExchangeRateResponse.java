package com.example.model;

public record ExchangeRateResponse(
        Currency baseCurrency,
        Currency targetCurrency,
        double rate
) {
}