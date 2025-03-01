package live.gloticker.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamChannel {
	INDICATOR_PRICE("indicator.price.stream"),
	STOCK_PRICE("stock.price.stream"),
	INDEX_PRICE("index.price.stream"),
	FOREX_PRICE("forex.price.stream"),
	CRYPTO_PRICE("crypto.price.stream");

	private final String channel;
}
