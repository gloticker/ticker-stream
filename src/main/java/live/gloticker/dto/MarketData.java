package live.gloticker.dto;

public sealed interface MarketData
	permits IndicatorData, StockData, IndexData, ForexData, CryptoData {
}
