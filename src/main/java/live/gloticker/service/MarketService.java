package live.gloticker.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
		Set<String> keys = redisTemplate.keys(type + "*");
		if (keys == null || keys.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> values = redisTemplate.opsForValue().multiGet(keys);
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}

		return values.stream()
			.map(value -> {
				try {
					return objectMapper.readTree(value);
				} catch (Exception e) {
					log.error("Failed to parse JSON: {}", e.getMessage());
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}
}
