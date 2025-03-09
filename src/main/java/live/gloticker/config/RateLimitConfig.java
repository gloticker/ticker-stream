package live.gloticker.config;

import java.time.Duration;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

@Configuration
public class RateLimitConfig implements DisposableBean {
	private static final int RATE_LIMIT_DB_INDEX = 1;

	@Value("${spring.data.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	private RedisClient redisClient;
	private StatefulRedisConnection<String, byte[]> redisConnection;

	@Bean
	public ProxyManager<String> bucketProxyManager() {
		redisClient = redisClient();
		redisConnection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

		return LettuceBasedProxyManager.builderFor(redisConnection)
			.withExpirationStrategy(
				ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
			.build();
	}

	private RedisClient redisClient() {
		return RedisClient.create(RedisURI.builder()
			.withHost(redisHost)
			.withPort(redisPort)
			.withDatabase(RATE_LIMIT_DB_INDEX)
			.build());
	}

	@Override
	public void destroy() {
		if (redisConnection != null) {
			redisConnection.close();
		}
		if (redisClient != null) {
			redisClient.shutdown();
		}
	}
}
