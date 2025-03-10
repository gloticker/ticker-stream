package live.gloticker.config;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

	private static final int DEFAULT_CAPACITY = 6;
	private static final Duration WINDOW_DURATION = Duration.ofSeconds(10);
	private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
	private static final String RETRY_AFTER_HEADER = "Retry-After";
	private static final String X_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
	private static final String X_RATE_LIMIT_RESET = "X-RateLimit-Reset";

	private final ProxyManager<String> bucketProxyManager;
	private final BrowserFingerprint browserFingerprint;

	@Override
	public boolean preHandle(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull Object handler) {

		String clientId = browserFingerprint.generateFingerprint(request);
		Bucket bucket = bucketProxyManager.builder()
			.build(clientId, this::getDefaultBucketConfiguration);

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		// 공통 헤더 설정
		response.setHeader(X_RATE_LIMIT_LIMIT, String.valueOf(DEFAULT_CAPACITY));

		if (!probe.isConsumed()) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			long secondsToWait = probe.getNanosToWaitForRefill() / 1_000_000_000;

			log.debug("Rate limit exceeded - Client: {}, Remaining: {}, Wait: {}s",
				clientId, probe.getRemainingTokens(), secondsToWait);

			response.setHeader(RETRY_AFTER_HEADER, String.valueOf(secondsToWait));
			response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
			response.setHeader(X_RATE_LIMIT_RESET, String.valueOf(System.currentTimeMillis() / 1000 + secondsToWait));
			return false;
		}

		log.debug("Request allowed - Client: {}, Remaining: {}",
			clientId, probe.getRemainingTokens());

		response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
		long nextRefillInSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
		response.setHeader(X_RATE_LIMIT_RESET, String.valueOf(System.currentTimeMillis() / 1000 + nextRefillInSeconds));

		return true;
	}

	private BucketConfiguration getDefaultBucketConfiguration() {
		return BucketConfiguration.builder()
			.addLimit(limitBuilder -> limitBuilder
				.capacity(DEFAULT_CAPACITY)
				.refillIntervally(DEFAULT_CAPACITY, WINDOW_DURATION))
			.build();
	}
}
