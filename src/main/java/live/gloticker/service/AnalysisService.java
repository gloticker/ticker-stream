package live.gloticker.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import live.gloticker.constant.MarketDataType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {
	private final MarketService marketService;
	private final GptService gptService;
	private final StringRedisTemplate redisTemplate;

	private static final String ANALYSIS_KEY = "analysis";

	@PostConstruct
	public void initializeAnalysis() {
		scheduleMarketAnalysis();
	}

	public String getLatestAnalysis() {
		return Optional.ofNullable(redisTemplate.opsForValue().get(ANALYSIS_KEY))
				.orElse("No analysis available");
	}

	@Scheduled(cron = "0 0 8,15,23 * * *", zone = "Asia/Seoul")
	public void scheduleMarketAnalysis() {
		try {
			String analysis = analyzeMarket();
			redisTemplate.opsForValue().set(ANALYSIS_KEY, analysis);
			log.info("Market analysis updated successfully");
		} catch (Exception e) {
			log.error("Failed to update market analysis", e);
		}
	}

	public Map<String, String> getAllData() {
		Map<String, Map<String, String>> marketData = marketService.combineMarketData();
		return processDataInOrder(marketData);
	}

	private String analyzeMarket() {
		return Optional.of(getAllData())
				.filter(data -> !data.isEmpty())
				.map(this::getMarketAnalysis)
				.orElse("Unable to fetch market data");
	}

	private String getMarketAnalysis(Map<String, String> marketData) {
		try {
			String formattedData = formatMarketData(marketData);
			return gptService.analyze(formattedData);
		} catch (Exception e) {
			log.error("Error while generating market analysis", e);
			return "Failed to generate market analysis";
		}
	}

	private String formatMarketData(Map<String, String> data) {
		return data.entrySet().stream()
				.map(entry -> entry.getKey() + ": " + entry.getValue())
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");
	}

	private Map<String, String> processDataInOrder(Map<String, Map<String, String>> data) {
		Map<String, String> result = new LinkedHashMap<>();
		Stream.of(MarketDataType.values())
				.sorted(Comparator.comparingInt(MarketDataType::getOrder))
				.forEach(type -> processCategory(data, result, type));
		return result;
	}

	private void processCategory(
			Map<String, Map<String, String>> data,
			Map<String, String> result,
			MarketDataType type) {
		data.forEach((symbol, symbolData) -> {
			if (type.getMatcher().test(symbol, symbolData)) {
				result.put(symbol, type.getValueExtractor().apply(symbolData));
			}
		});
	}
}
