package kr.co.cleverchat.common.search;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.ko.KoreanPartOfSpeechStopFilter;
import org.apache.lucene.analysis.ko.KoreanTokenizer.DecompoundMode;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

/**
 * Korean morphological analyzer backed by Lucene Nori (mecab-ko-dic bundled).
 *
 * <p>Runs fully offline — no external service — so it fits the on-premise deployment assumption.
 * The underlying {@link Analyzer} is thread-safe and reused across calls. Both the crawl indexing
 * path and the search query path tokenize through this single component so that index-time and
 * query-time tokens line up.
 */
@Component
public class KoreanMorphAnalyzer {

    private static final String FIELD = "morph";

    /** Guard against pathological inputs; crawl content is already capped upstream. */
    private static final int MAX_INPUT_LENGTH = 200_000;

    // MIXED keeps both the full compound noun and its parts (e.g. "주차장" -> "주차장", "주차", "장")
    // so a query matches whether the user types the compound or a component. The same analyzer is
    // used at index time and query time, keeping the two token streams consistent.
    private final Analyzer analyzer =
            new KoreanAnalyzer(
                    null,
                    DecompoundMode.MIXED,
                    KoreanPartOfSpeechStopFilter.DEFAULT_STOP_TAGS,
                    false);

    /**
     * Tokenizes the given text into morphemes and returns them joined by single spaces, suitable
     * for storing in a {@code to_tsvector('simple', ...)} column. Returns an empty string for
     * null/blank input.
     */
    public String tokenize(String text) {
        return String.join(" ", tokens(text));
    }

    /** Tokenizes the given text into a list of morphemes (lower-cased, stop tags removed). */
    public List<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String input =
                text.length() > MAX_INPUT_LENGTH ? text.substring(0, MAX_INPUT_LENGTH) : text;
        List<String> result = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream(FIELD, input)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                String token = term.toString();
                if (!token.isBlank()) {
                    result.add(token);
                }
            }
            stream.end();
        } catch (IOException e) {
            // tokenStream over an in-memory string should not perform real IO
            throw new UncheckedIOException("Korean morphological analysis failed", e);
        }
        return result;
    }
}
