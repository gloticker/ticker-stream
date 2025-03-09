package live.gloticker.dto;

import java.util.List;

public record GptResponse(
	List<GptChoice> choices) {
}
