package com.example.model;

public record ExchangeRateCalculation(
        Currency baseCurrency,
        Currency targetCurrency,
        double rate,
        double amount,
        double convertedAmount
) {}