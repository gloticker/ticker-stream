package live.gloticker.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MarketAnalysisPrompt {
	MESSAGE("당신은 전문적인 금융시장 분석가입니다. 데이터를 기반으로 객관적이고 전문적인 분석을 제공합니다."),
	ANALYSIS_TEMPLATE("""
		다음은 현재 시장 데이터입니다.
		- 데이터를 바탕으로 **시장 심리와 투자 조언을 2문장으로만 간결하게 작성**해 주세요.
		- **수치나 데이터 값은 절대 언급하지 말고**, 오로지 해석과 조언만 짧게 작성해 주세요.
		- 한국어로 작성해 주세요.
		- 문장은 자연스럽게 마무리해 주세요.

		[DATA]
		%s
		""");

	private final String content;
}
