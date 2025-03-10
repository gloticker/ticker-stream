package live.gloticker.constant;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FingerprintHeader {
	// 1. 필수 식별자 (실제로 필요한 것만)
	USER_AGENT("User-Agent", HeaderCategory.ESSENTIAL),
	ACCEPT_LANGUAGE("Accept-Language", HeaderCategory.ESSENTIAL),

	// 2. IP 관련 (주요 프록시 헤더만)
	X_FORWARDED_FOR("X-Forwarded-For", HeaderCategory.IP),
	X_REAL_IP("X-Real-IP", HeaderCategory.IP),
	REMOTE_ADDR("REMOTE_ADDR", HeaderCategory.IP),

	// 3. 보안 관련
	SEC_FETCH_SITE("Sec-Fetch-Site", HeaderCategory.SECURITY),
	SEC_FETCH_MODE("Sec-Fetch-Mode", HeaderCategory.SECURITY);

	private final String headerName;
	private final HeaderCategory category;

	public enum HeaderCategory {
		ESSENTIAL,
		IP,
		SECURITY
	}

	public static List<String> getSecurityHeaders() {
		return Arrays.stream(values())
			.filter(header -> header.getCategory() == HeaderCategory.SECURITY)
			.map(FingerprintHeader::getHeaderName)
			.collect(Collectors.toList());
	}

	public static List<String> getIpHeaders() {
		return Arrays.stream(values())
			.filter(header -> header.getCategory() == HeaderCategory.IP)
			.map(FingerprintHeader::getHeaderName)
			.collect(Collectors.toList());
	}

	public static List<String> getEssentialHeaders() {
		return Arrays.stream(values())
			.filter(header -> header.getCategory() == HeaderCategory.ESSENTIAL)
			.map(FingerprintHeader::getHeaderName)
			.collect(Collectors.toList());
	}
}
