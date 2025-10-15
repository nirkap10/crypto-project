package com.example.priceingestor.model;

public record CoinMarket(
    String id,
    String symbol,
    String name,
    Double current_price,
    Double market_cap,
    Double price_change_percentage_24h
) {}
