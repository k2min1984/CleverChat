package kr.co.cleverchat.domain.chatbot.ai;

import java.util.List;
import java.util.Optional;
import kr.co.cleverchat.domain.search.model.SearchResultItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiAnswerSuggestionService {

    private final boolean enabled;
    private final AiAnswerProvider provider;

    public AiAnswerSuggestionService(
            @Value("${cleverchat.ai.answer-suggestion.enabled:false}") boolean enabled,
            AiAnswerProvider provider) {
        this.enabled = enabled;
        this.provider = provider;
    }

    public Optional<AiAnswerSuggestionResponse> suggest(
            String query, List<SearchResultItem> searchResults, AiAnswerContext context) {
        if (!enabled
                || !StringUtils.hasText(query)
                || searchResults == null
                || searchResults.isEmpty()) {
            return Optional.empty();
        }
        try {
            AiAnswerSuggestionResponse response =
                    provider.generate(
                            new AiAnswerPrompt(
                                    query,
                                    context,
                                    searchResults.stream().map(this::toCitation).toList()));
            if (response == null
                    || !response.generated()
                    || !StringUtils.hasText(response.answer())) {
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private AiAnswerCitation toCitation(SearchResultItem item) {
        String sourceType =
                "CRAWL_DOCUMENT".equals(item.getMatchedField()) ? "CRAWL_DOCUMENT" : "SCENARIO";
        return new AiAnswerCitation(
                sourceType,
                item.getScenarioNo(),
                item.getScenarioTitle(),
                item.getCrawlDocumentNo(),
                item.getCrawlUrl(),
                item.getMatchedField(),
                item.getSnippet());
    }
}
