package live.gloticker.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StreamChannel {
	ALL_CHANNEL("*.price.stream"),
	INDEX_CHANNEL("index.price.stream"),
	STOCK_CHANNEL("stock.price.stream"),
	CRYPTO_CHANNEL("crypto.price.stream"),
	FOREX_CHANNEL("forex.price.stream");

	private final String channel;
}
