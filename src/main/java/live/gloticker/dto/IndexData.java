package live.gloticker.dto;

import live.gloticker.constant.MarketType;

public record IndexData(
		String symbol,
		String name,
		String price,
		String marketState,
		String lastUpdated,
		MarketType type) implements MarketData {
}
