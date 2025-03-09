package live.gloticker.service;

import static live.gloticker.constant.MarketAnalysisPrompt.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import live.gloticker.dto.GptChoice;
import live.gloticker.dto.GptMessage;
import live.gloticker.dto.GptResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GptService {
	private final RestTemplate restTemplate;

	@Value("${openai.api.key}")
	private String apiKey;

	@Value("${openai.api.url}")
	private String apiUrl;

	public String analyze(String marketData) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		Map<String, Object> requestBody = new HashMap<>();
		requestBody.put("model", "gpt-4-turbo");
		requestBody.put("messages", List.of(
			Map.of(
				"role", "system",
				"content", MESSAGE.getContent()),
			Map.of(
				"role", "user",
				"content", String.format(ANALYSIS_TEMPLATE.getContent(), marketData))));
		requestBody.put("temperature", 0.7);
		requestBody.put("max_tokens", 150);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

		try {
			var response = restTemplate.exchange(
				apiUrl,
				HttpMethod.POST,
				request,
				GptResponse.class);

			return Optional.ofNullable(response.getBody())
				.map(GptResponse::choices)
				.filter(choices -> !choices.isEmpty())
				.map(choices -> choices.get(0))
				.map(GptChoice::message)
				.map(GptMessage::content)
				.orElse("Unable to retrieve analysis results");
		} catch (Exception e) {
			log.error("Error while calling GPT API", e);
			return "Failed to call GPT API";
		}
	}
}
