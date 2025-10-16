package com.example.priceingestor.repository;

import com.example.priceingestor.model.CoinMarket;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PriceRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public PriceRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int[] insertBatchIgnoreDuplicates(String source, List<CoinMarket> markets) {
        // Bucket to nearest minute to match unique index (source, symbol, ts_bucket)
        ZonedDateTime nowUtc = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.MINUTES);
        Timestamp tsBucket = Timestamp.from(nowUtc.toInstant());

        String sql = "INSERT INTO prices (source, symbol, coin_id, name, price, market_cap, pct_change_24h, ts_bucket) " +
            "VALUES (:source, :symbol, :coin_id, :name, :price, :market_cap, :pct_change_24h, :ts_bucket) " +
            "ON CONFLICT DO NOTHING";

        MapSqlParameterSource[] batch = markets.stream().map(m -> new MapSqlParameterSource()
            .addValue("source", source)
            .addValue("symbol", m.symbol())
            .addValue("coin_id", m.id())
            .addValue("name", m.name())
            .addValue("price", m.current_price())
            .addValue("market_cap", m.market_cap())
            .addValue("pct_change_24h", m.price_change_percentage_24h())
            .addValue("ts_bucket", tsBucket)
        ).toArray(MapSqlParameterSource[]::new);

        return jdbc.batchUpdate(sql, batch);
    }
}


