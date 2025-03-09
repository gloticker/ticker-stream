package live.gloticker.constant;

import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketDataType {
	INDEX((symbol, data) -> symbol.startsWith("^"), data -> data.get("current_value"), 1),
	FEAR_AND_GREED((symbol, data) -> "Fear&Greed".equals(symbol), data -> data.get("score"),
		2),
	STOCK((symbol, data) -> data.containsKey("market_state"),
		data -> data.containsKey("otc_price") ? data.get("otc_price") : data.get("current_price"),
		3),
	CRYPTO((symbol, data) -> !symbol.equals("BTC.D") && data.containsKey("current_price") && !data.containsKey(
		"market_state"), data -> data.get("current_price"), 4),
	DOMINANCE((symbol, data) -> "BTC.D".equals(symbol), data -> data.get("value"), 5),
	FOREX((symbol, data) -> data.containsKey("rate"), data -> data.get("rate"), 6);

	private final BiPredicate<String, Map<String, String>> matcher;
	private final Function<Map<String, String>, String> valueExtractor;
	private final int order;
}
