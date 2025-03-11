package live.gloticker.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import live.gloticker.constant.StreamChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class StreamService {
	private final RedisMessageListenerContainer container;
	private final ObjectMapper objectMapper;
	private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
	private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

	@PostConstruct
	private void init() {
		MessageListener listener = (message, pattern) -> {
			try {
				String payload = new String(message.getBody());
				log.info("[Redis] Channel: {} | Received {} items",
					new String(message.getChannel()),
					objectMapper.readTree(payload).size());
				sendToAllEmitters(payload);
			} catch (Exception e) {
				log.error("Failed to process message: ", e);
			}
		};

		container.addMessageListener(listener, new PatternTopic(StreamChannel.ALL_CHANNEL.getChannel()));
		log.info("Subscribed to all price stream channels");
	}

	@PreDestroy
	private void cleanup() {
		container.stop();
		emitters.clear();
		log.info("Cleaned up Redis container and emitters");
	}

	private void sendToAllEmitters(Object message) {
		emitters.forEach((clientId, emitter) -> {
			try {
				emitter.send(message);
			} catch (Exception e) {
				emitters.remove(clientId);
				log.debug("Removed client {} due to send failure", clientId);
			}
		});
	}

	@Scheduled(fixedRate = 15000)
	public void sendHeartbeat() {
		String ping = String.format("ping %s",
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		sendToAllEmitters(ping);
	}

	public SseEmitter subscribe() {
		String clientId = UUID.randomUUID().toString();
		SseEmitter emitter = new SseEmitter(TIMEOUT);

		emitter.onCompletion(() -> {
			if (emitters.remove(clientId) != null) {
				log.debug("Client {} completed connection. Remaining clients: {}", clientId, emitters.size());
			}
		});

		emitter.onTimeout(() -> {
			if (emitters.remove(clientId) != null) {
				log.debug("Client {} connection timed out. Remaining clients: {}", clientId, emitters.size());
			}
		});

		emitter.onError(ex -> {
			if (emitters.remove(clientId) != null) {
				log.debug("Client {} connection error: {}. Remaining clients: {}",
					clientId, ex.getMessage(), emitters.size());
			}
		});

		try {
			emitter.send(SseEmitter.event().comment("connected"));
			emitters.put(clientId, emitter);
			log.debug("New client {} subscribed. Total clients: {}", clientId, emitters.size());
		} catch (IOException e) {
			log.debug("Failed to send initial ping to client {}", clientId);
			emitters.remove(clientId);
		}

		return emitter;
	}
}
