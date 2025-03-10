package live.gloticker.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketService {
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public List<Object> getAllData(String type) {
		return getRedisValues(type + "*")
				.map(this::parseJsonData)
				.orElse(Collections.emptyList());
	}

	public Optional<List<String>> getRedisValues(String pattern) {
		Set<String> keys = redisTemplate.keys(pattern);
		if (keys == null || keys.isEmpty()) {
			log.debug("No data found in Redis for pattern: {}", pattern);
			return Optional.empty();
		}

		List<String> values = redisTemplate.opsForValue().multiGet(keys);
		if (values == null || values.isEmpty()) {
			log.debug("Failed to retrieve data from Redis for pattern: {}", pattern);
			return Optional.empty();
		}
		return Optional.of(values);
	}

	public Map<String, Map<String, String>> combineMarketData() {
		return getRedisValues("snapshot*")
				.map(this::parseMarketData)
				.orElse(Collections.emptyMap());
	}

	private List<Object> parseJsonData(List<String> values) {
		return values.stream()
				.map(value -> {
					try {
						return objectMapper.readValue(value, Object.class);
					} catch (Exception e) {
						log.error("Failed to parse JSON: {}", e.getMessage());
						return null;
					}
				})
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	private Map<String, Map<String, String>> parseMarketData(List<String> values) {
		Map<String, Map<String, String>> combinedData = new LinkedHashMap<>();
		values.forEach(value -> {
			try {
				Map<String, Map<String, String>> data = objectMapper.readValue(
						value,
						new TypeReference<>() {
						});
				combinedData.putAll(data);
			} catch (Exception e) {
				log.error("Failed to parse market data: {}", e.getMessage());
			}
		});
		return combinedData;
	}
}
