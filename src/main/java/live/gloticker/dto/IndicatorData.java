package live.gloticker.dto;

import live.gloticker.constant.MarketType;

public record IndicatorData(
	String symbol,
	String name,
	String value,
	String lastUpdated,
	String rating,
	MarketType type) implements MarketData {
}
