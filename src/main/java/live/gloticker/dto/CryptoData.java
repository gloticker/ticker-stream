package live.gloticker.dto;

import live.gloticker.constant.MarketType;

public record CryptoData(
		String symbol,
		String name,
		String price,
		String lastUpdated,
		MarketType type) implements MarketData {
}
