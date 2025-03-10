package live.gloticker.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import live.gloticker.constant.FingerprintHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrowserFingerprint {

	private static final String UNKNOWN_VALUE = "unknown";
	private static final int MAX_HEADER_LENGTH = 1000;
	private static final Pattern VALID_HEADER_PATTERN = Pattern.compile("[a-zA-Z0-9\\-_.,;: /\\[\\]()]+");

	public String generateFingerprint(HttpServletRequest request) {
		String essentialPart = appendHeaders(request, FingerprintHeader.getEssentialHeaders());
		String ip = getClientIP(request);
		String securityPart = appendHeaders(request, FingerprintHeader.getSecurityHeaders());

		String fingerprint = String.join("|", essentialPart, ip, securityPart);
		return hashFingerprint(fingerprint);
	}

	private String getClientIP(HttpServletRequest request) {
		String ip = FingerprintHeader.getIpHeaders().stream()
			.map(header -> getHeaderValue(request, header))
			.filter(StringUtils::hasText)
			.filter(value -> !UNKNOWN_VALUE.equalsIgnoreCase(value))
			.filter(this::isValidHeader)
			.findFirst()
			.orElseGet(request::getRemoteAddr);

		return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
	}

	private String appendHeaders(HttpServletRequest request, List<String> headers) {
		return headers.stream()
			.map(header -> getHeaderValue(request, header))
			.filter(this::isValidHeader)
			.collect(java.util.stream.Collectors.joining("|"));
	}

	private String getHeaderValue(HttpServletRequest request, String headerName) {
		String value = request.getHeader(headerName);
		return StringUtils.hasText(value) ? value : UNKNOWN_VALUE;
	}

	private boolean isValidHeader(String value) {
		return StringUtils.hasText(value)
			&& !UNKNOWN_VALUE.equalsIgnoreCase(value)
			&& value.length() < MAX_HEADER_LENGTH
			&& VALID_HEADER_PATTERN.matcher(value).matches();
	}

	private String hashFingerprint(String fingerprint) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			log.error("Failed to hash fingerprint", e);
			return fingerprint;
		}
	}
}
