package com.example.priceingestor.client;

import com.example.priceingestor.model.CoinMarket;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Component
public class MarketClient {

  private final WebClient http;
  private final String source;

  public MarketClient(
      WebClient.Builder builder,
      @Value("${ingestor.base-url}") String baseUrl,
      @Value("${ingestor.api-key-header}") String keyHeader,
      @Value("${ingestor.api-key}") String apiKey,
      @Value("${ingestor.source}") String source
  ) {
    WebClient.Builder b = builder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

    if (apiKey != null && !apiKey.isBlank()) {
      b.defaultHeader(keyHeader, apiKey);
    }
    this.http = b.build();
    this.source = source;
  }

  public String source() { return source; }

  // CoinGecko-style endpoint; adjust uri/params for your provider
  public Flux<CoinMarket> topMarkets(String vsCurrency, int perPage, int page) {
    return http.get()
        .uri(uri -> uri.path("/coins/markets")
            .queryParam("vs_currency", vsCurrency)
            .queryParam("order", "volume_desc")
            .queryParam("per_page", perPage)
            .queryParam("page", page)
            .queryParam("price_change_percentage", "24h")
            .build())
        .retrieve()
        .onStatus(s -> s.value() == 429, resp -> Mono.error(new RuntimeException("Rate limited")))
        .bodyToFlux(CoinMarket.class)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
            .filter(ex -> ex instanceof WebClientResponseException || ex instanceof RuntimeException))
        .timeout(Duration.ofSeconds(15));
  }
}
