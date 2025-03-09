package live.gloticker.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

	private final ProxyManager<String> bucketProxyManager;

	private static final List<String> IP_HEADERS = Arrays.asList(
		"X-Forwarded-For",
		"Proxy-Client-IP",
		"WL-Proxy-Client-IP",
		"HTTP_X_FORWARDED_FOR",
		"HTTP_X_FORWARDED",
		"HTTP_X_CLUSTER_CLIENT_IP",
		"HTTP_CLIENT_IP",
		"HTTP_FORWARDED_FOR",
		"HTTP_FORWARDED",
		"HTTP_VIA",
		"REMOTE_ADDR");

	@Override
	public boolean preHandle(
		@NonNull HttpServletRequest request,
		@NonNull HttpServletResponse response,
		@NonNull Object handler) {
		String ip = getClientIP(request);

		Bucket bucket = bucketProxyManager.builder()
			.build(ip, () -> BucketConfiguration.builder()
				.addLimit(limit -> limit
					.capacity(3)
					.refillGreedy(3, Duration.ofSeconds(10)))
				.build());

		if (bucket.tryConsume(1)) {
			return true;
		}

		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		return false;
	}

	private String getClientIP(HttpServletRequest request) {
		String ip = null;

		for (String header : IP_HEADERS) {
			ip = request.getHeader(header);
			if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
				int idx = ip.indexOf(',');
				if (idx > -1) {
					ip = ip.substring(0, idx).trim();
				}
				break;
			}
		}

		if (!StringUtils.hasText(ip)) {
			ip = request.getRemoteAddr();
		}

		if ("0:0:0:0:0:0:0:1".equals(ip)) {
			ip = "127.0.0.1";
		}

		return ip;
	}
}
