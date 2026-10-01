package kr.co.cleverchat.common.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class KoreanMorphAnalyzerTest {

    private final KoreanMorphAnalyzer analyzer = new KoreanMorphAnalyzer();

    @Test
    void extractsStemFromConjugatedVerb() {
        // "검색했더니" should reduce to the stem "검색" so that a query for "검색" matches.
        List<String> tokens = analyzer.tokens("검색했더니 결과가 나왔다");
        assertThat(tokens).contains("검색", "결과");
    }

    @Test
    void splitsCompoundSentenceIntoMorphemes() {
        List<String> tokens = analyzer.tokens("주차장 이용 안내 게시물 상세 내용");
        assertThat(tokens).contains("주차장", "이용", "안내", "게시물", "상세", "내용");
    }

    @Test
    void joinsTokensWithSpaces() {
        String joined = analyzer.tokenize("공지사항 검색");
        assertThat(joined).contains("공지").contains("검색");
        assertThat(joined).doesNotContain("  ");
    }

    @Test
    void returnsEmptyForNullOrBlank() {
        assertThat(analyzer.tokens(null)).isEmpty();
        assertThat(analyzer.tokens("   ")).isEmpty();
        assertThat(analyzer.tokenize(null)).isEmpty();
    }
}
