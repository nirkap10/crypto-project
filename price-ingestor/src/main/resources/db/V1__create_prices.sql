CREATE TABLE IF NOT EXISTS prices (
  id BIGSERIAL PRIMARY KEY,
  source TEXT NOT NULL,
  symbol TEXT NOT NULL,
  coin_id TEXT NOT NULL,
  name TEXT NOT NULL,
  price NUMERIC(38, 12) NOT NULL,
  market_cap NUMERIC(38, 2),
  pct_change_24h NUMERIC(12, 6),
  ts TIMESTAMPTZ NOT NULL DEFAULT now(),
  ts_bucket TIMESTAMPTZ NOT NULL
);

-- avoid duplicates per (source, symbol, minute)
CREATE UNIQUE INDEX IF NOT EXISTS uq_prices_source_symbol_bucket
  ON prices (source, symbol, ts_bucket);
