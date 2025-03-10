package live.gloticker.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.constraints.Pattern;
import live.gloticker.dto.AnalysisData;
import live.gloticker.service.AnalysisService;
import live.gloticker.service.MarketService;
import live.gloticker.service.StreamService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/market")
public class MarketDataApi {
	private final StreamService streamService;
	private final MarketService marketService;
	private final AnalysisService analysisService;

	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamMarketData() {
		return streamService.subscribe();
	}

	@GetMapping("/{type}")
	public ResponseEntity<List<Object>> getData(@PathVariable("type") @Pattern(regexp = "snapshot|chart") String type) {
		return ResponseEntity.ok(marketService.getAllData(type));
	}

	@GetMapping("/analysis")
	public ResponseEntity<AnalysisData> getAnalysis() {
		return analysisService.getLatestAnalysis()
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	@ExceptionHandler(AsyncRequestTimeoutException.class)
	public ResponseEntity<Void> handleAsyncRequestTimeoutException() {
		return ResponseEntity.noContent().build();
	}
}