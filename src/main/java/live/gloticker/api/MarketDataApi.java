package live.gloticker.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import live.gloticker.service.MarketDataService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class MarketDataApi {
	private final MarketDataService marketDataService;

	@GetMapping(value = "/market", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamMarketData() {
		return marketDataService.subscribe();
	}

	@ExceptionHandler(AsyncRequestTimeoutException.class)
	public ResponseEntity<Void> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex) {
		return ResponseEntity.noContent().build();
	}
}
