package live.gloticker.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
	private static final long TIMEOUT = 0L;

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

	private void removeDeadEmitters(List<SseEmitter> deadEmitters) {
		if (!deadEmitters.isEmpty()) {
			emitters.removeAll(deadEmitters);
			log.debug("Removed {} disconnected clients. Remaining: {}",
				deadEmitters.size(), emitters.size());
		}
	}

	private void sendToEmitters(Object message, List<SseEmitter> deadEmitters) {
		emitters.forEach(emitter -> {
			try {
				emitter.send(message);
			} catch (Exception e) {
				deadEmitters.add(emitter);
			}
		});
	}

	@Scheduled(fixedRate = 15000)
	public void sendHeartbeat() {
		List<SseEmitter> deadEmitters = new ArrayList<>();
		String ping = String.format("ping %s",
			LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

		sendToEmitters(ping, deadEmitters);
		removeDeadEmitters(deadEmitters);
	}

	private void sendToAllEmitters(Object message) {
		List<SseEmitter> deadEmitters = new ArrayList<>();
		sendToEmitters(message, deadEmitters);
		removeDeadEmitters(deadEmitters);
	}

	public SseEmitter subscribe() {
		SseEmitter emitter = new SseEmitter(TIMEOUT);

		emitter.onCompletion(() -> {
			if (emitters.remove(emitter)) {
				log.debug("Client completed connection. Remaining clients: {}", emitters.size());
			}
		});

		emitter.onTimeout(() -> {
			if (emitters.remove(emitter)) {
				log.debug("Client connection timed out. Remaining clients: {}", emitters.size());
			}
		});

		emitter.onError(ex -> {
			if (emitters.remove(emitter)) {
				log.debug("Client connection error: {}. Remaining clients: {}",
					ex.getMessage(), emitters.size());
			}
		});

		try {
			emitter.send(SseEmitter.event().comment("connected"));
			emitters.add(emitter);
			log.debug("New client subscribed. Total clients: {}", emitters.size());
		} catch (IOException e) {
			log.debug("Failed to send initial ping");
		}

		return emitter;
	}
}
