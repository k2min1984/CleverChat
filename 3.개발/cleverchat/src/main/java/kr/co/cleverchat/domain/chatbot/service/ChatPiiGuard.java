package kr.co.cleverchat.domain.chatbot.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ChatPiiGuard {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE =
            Pattern.compile(
                    "(?<!\\d)(?:01[016789][- .]?\\d{3,4}[- .]?\\d{4}|0\\d{1,2}[- .]?\\d{3,4}[- .]?\\d{4})(?!\\d)");
    private static final Pattern RESIDENT_REGISTRATION_NUMBER =
            Pattern.compile("(?<!\\d)\\d{6}[- ]?[1-4]\\d{6}(?!\\d)");
    private static final Pattern CARD_CANDIDATE =
            Pattern.compile("(?<!\\d)(?:\\d[ -]?){13,19}(?!\\d)");

    public List<String> detectTypes(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> types = new LinkedHashSet<>();
        if (EMAIL.matcher(text).find()) {
            types.add("EMAIL");
        }
        if (PHONE.matcher(text).find()) {
            types.add("PHONE");
        }
        if (RESIDENT_REGISTRATION_NUMBER.matcher(text).find()) {
            types.add("RRN");
        }
        var cardMatcher = CARD_CANDIDATE.matcher(text);
        while (cardMatcher.find()) {
            String digits = cardMatcher.group().replaceAll("\\D", "");
            if (isValidCardNumber(digits)) {
                types.add("CARD");
                break;
            }
        }
        return new ArrayList<>(types);
    }

    public boolean hasHighRiskTypes(List<String> types) {
        return types != null && (types.contains("RRN") || types.contains("CARD"));
    }

    public String maskLowRisk(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = EMAIL.matcher(text).replaceAll(match -> maskEmail(match.group()));
        return PHONE.matcher(masked).replaceAll(match -> maskPhone(match.group()));
    }

    private String maskEmail(String value) {
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        int keep = Math.min(2, local.length());
        return local.substring(0, keep) + "***" + domain;
    }

    private String maskPhone(String value) {
        String digits = value.replaceAll("\\D", "");
        int digitIndex = 0;
        StringBuilder masked = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!Character.isDigit(ch)) {
                masked.append(ch);
                continue;
            }
            boolean keep = digitIndex < 3 || digitIndex >= digits.length() - 2;
            masked.append(keep ? ch : '*');
            digitIndex++;
        }
        return masked.toString();
    }

    private boolean isValidCardNumber(String digits) {
        if (digits == null || digits.length() < 13 || digits.length() > 19) {
            return false;
        }
        int sum = 0;
        boolean doubleDigit = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (n < 0 || n > 9) {
                return false;
            }
            if (doubleDigit) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
}
