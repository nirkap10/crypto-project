package com.example.priceingestor;

import com.example.priceingestor.client.MarketClient;
import com.example.priceingestor.model.CoinMarket;
import com.example.priceingestor.repository.PriceRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PriceIngestorApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceIngestorApplication.class, args);
	}

    @Bean
    CommandLineRunner ingestOnce(
        MarketClient client,
        PriceRepository repository,
        @Value("${ingestor.vs-currency}") String vsCurrency,
        @Value("${ingestor.source}") String source
    ) {
        return args -> {
            // Fetch 1 page, then take top 10 by 24h volume is already implied by order
            List<CoinMarket> markets = client
                .topMarkets(vsCurrency, 10, 1)
                .collectList()
                .block();

            if (markets == null || markets.isEmpty()) {
                return;
            }

            repository.insertBatchIgnoreDuplicates(source, markets);
        };
    }
}
