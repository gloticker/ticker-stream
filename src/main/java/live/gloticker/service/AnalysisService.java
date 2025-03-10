package live.gloticker.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import live.gloticker.constant.MarketDataType;
import live.gloticker.dto.AnalysisData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {
	private final MarketService marketService;
	private final GptService gptService;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	private static final String ANALYSIS_KEY = "analysis";
	private static final long ANALYSIS_FRESHNESS_HOURS = 3;
	private static final ZoneId NEW_YORK_ZONE = ZoneId.of("America/New_York");
	private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

	@PostConstruct
	public void initializeAnalysis() {
		Optional<AnalysisData> existingAnalysis = getStoredAnalysis();

		if (existingAnalysis.isPresent()) {
			LocalDateTime lastAnalysisTime = existingAnalysis.get().timestamp();
			ZonedDateTime now = ZonedDateTime.now(NEW_YORK_ZONE);
			long hoursSinceLastAnalysis = ChronoUnit.HOURS.between(
				lastAnalysisTime,
				now.toLocalDateTime());

			log.debug("Last analysis time (NY): {}, Current NY time: {}, Hours since last analysis: {}",
				lastAnalysisTime, now, hoursSinceLastAnalysis);

			if (hoursSinceLastAnalysis < ANALYSIS_FRESHNESS_HOURS) {
				log.info("Recent analysis found ({} hours ago). Skipping initial analysis.", hoursSinceLastAnalysis);
				return;
			}
		}

		scheduleMarketAnalysis();
	}

	public Optional<AnalysisData> getLatestAnalysis() {
		return getStoredAnalysis();
	}

	private Optional<AnalysisData> getStoredAnalysis() {
		try {
			return Optional.ofNullable(redisTemplate.opsForValue().get(ANALYSIS_KEY))
				.map(json -> {
					try {
						return objectMapper.readValue(json, AnalysisData.class);
					} catch (JsonProcessingException e) {
						log.error("Failed to parse stored analysis", e);
						return null;
					}
				});
		} catch (Exception e) {
			log.error("Error retrieving analysis from Redis", e);
			return Optional.empty();
		}
	}

	@Scheduled(cron = "0 0 8,15,23 * * *", zone = "Asia/Seoul")
	public void scheduleMarketAnalysis() {
		try {
			String analysis = analyzeMarket();
			AnalysisData analysisData = new AnalysisData(
				analysis,
				ZonedDateTime.now(NEW_YORK_ZONE).toLocalDateTime());
			redisTemplate.opsForValue().set(ANALYSIS_KEY, objectMapper.writeValueAsString(analysisData));

			ZonedDateTime seoulTime = ZonedDateTime.now(SEOUL_ZONE);
			ZonedDateTime nyTime = ZonedDateTime.now(NEW_YORK_ZONE);
			log.info("Market analysis updated at - Seoul: {}, New York: {}",
				seoulTime.toLocalDateTime(),
				nyTime.toLocalDateTime());
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
