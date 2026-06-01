package kr.co.cleverchat.domain.chatbot.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.junit.jupiter.api.Test;

class AiAnswerSuggestionServiceTest {

    @Test
    void disabledServiceReturnsEmptyWithoutCallingProvider() {
        AtomicInteger calls = new AtomicInteger();
        AiAnswerSuggestionService service =
                new AiAnswerSuggestionService(
                        false,
                        prompt -> {
                            calls.incrementAndGet();
                            return new AiAnswerSuggestionResponse(
                                    "answer", prompt.citations(), "stub", true);
                        });

        var result = service.suggest("question", List.of(result("SCENARIO")), context());

        assertThat(result).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void enabledServiceReturnsGeneratedAnswerWithCitations() {
        AtomicReference<AiAnswerPrompt> promptRef = new AtomicReference<>();
        AiAnswerSuggestionService service =
                new AiAnswerSuggestionService(
                        true,
                        prompt -> {
                            promptRef.set(prompt);
                            return new AiAnswerSuggestionResponse(
                                    "suggested answer", prompt.citations(), "stub", true);
                        });

        var result =
                service.suggest(
                        "masked email a***@example.com",
                        List.of(result("CRAWL_DOCUMENT")),
                        context());

        assertThat(result).isPresent();
        assertThat(result.get().answer()).isEqualTo("suggested answer");
        assertThat(result.get().provider()).isEqualTo("stub");
        assertThat(result.get().citations()).hasSize(1);
        assertThat(promptRef.get().query()).isEqualTo("masked email a***@example.com");
        assertThat(promptRef.get().citations().get(0).sourceType()).isEqualTo("CRAWL_DOCUMENT");
    }

    @Test
    void emptySearchResultsDoNotCallProvider() {
        AtomicInteger calls = new AtomicInteger();
        AiAnswerSuggestionService service =
                new AiAnswerSuggestionService(
                        true,
                        prompt -> {
                            calls.incrementAndGet();
                            return new AiAnswerSuggestionResponse(
                                    "answer", prompt.citations(), "stub", true);
                        });

        var result = service.suggest("question", List.of(), context());

        assertThat(result).isEmpty();
        assertThat(calls).hasValue(0);
    }

    @Test
    void providerFailureDegradesToEmptySuggestion() {
        AiAnswerSuggestionService service =
                new AiAnswerSuggestionService(
                        true,
                        prompt -> {
                            throw new IllegalStateException("provider unavailable");
                        });

        var result = service.suggest("question", List.of(result("SCENARIO")), context());

        assertThat(result).isEmpty();
    }

    private AiAnswerContext context() {
        return new AiAnswerContext(100L, 300L, "CHAT_FALLBACK");
    }

    private SearchResultItem result(String matchedField) {
        SearchResultItem item = new SearchResultItem();
        item.setScenarioNo(100L);
        item.setScenarioTitle("Scenario title");
        item.setCrawlDocumentNo(200L);
        item.setCrawlUrl("https://example.test/doc");
        item.setMatchedField(matchedField);
        item.setSnippet("Short snippet");
        return item;
    }
}
