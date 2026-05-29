package kr.co.cleverchat.common.accessibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class AccessibilityTemplateTest {

    private static final Path TEMPLATE_ROOT = Path.of("src/main/resources/templates");
    private static final Path STATIC_ROOT = Path.of("src/main/resources/static");

    private static final List<String> ADMIN_TABLE_TEMPLATES =
            List.of(
                    "admmgr/scenario/scenarioList.html",
                    "admmgr/scenario/scenarioView.html",
                    "admmgr/search/logList.html",
                    "admmgr/search/blockList.html",
                    "admmgr/search/popularList.html",
                    "admmgr/crawl/targetList.html",
                    "admmgr/crawl/documentList.html",
                    "admmgr/crawl/runList.html",
                    "admmgr/chat/failureList.html",
                    "admmgr/chat/feedbackList.html",
                    "admmgr/chat/recommendationList.html",
                    "admmgr/chat/sessionList.html",
                    "admmgr/chat/sessionDetail.html",
                    "admmgr/ops/auditLogList.html",
                    "admmgr/ops/noticeList.html",
                    "admmgr/ops/notificationList.html");

    private static final List<String> FORM_AND_ACTION_TEMPLATES =
            List.of(
                    "chat/chat.html",
                    "chat/history.html",
                    "admmgr/scenario/scenarioGraphEdit.html",
                    "admmgr/search/logList.html",
                    "admmgr/search/blockList.html",
                    "admmgr/search/popularList.html",
                    "admmgr/crawl/targetList.html",
                    "admmgr/crawl/runList.html",
                    "admmgr/chat/failureList.html",
                    "admmgr/chat/feedbackList.html",
                    "admmgr/chat/recommendationList.html",
                    "admmgr/chat/sessionList.html",
                    "admmgr/ops/auditLogList.html",
                    "admmgr/ops/noticeList.html",
                    "admmgr/ops/notificationList.html");

    private static final Map<String, String[]> CONTRAST_PAIRS =
            Map.of(
                    "chat body text", new String[] {"#172033", "#f5f7fb"},
                    "chat muted text", new String[] {"#50617a", "#ffffff"},
                    "chat primary button", new String[] {"#ffffff", "#1f2937"},
                    "admin body text", new String[] {"#1f2937", "#ffffff"},
                    "admin table text", new String[] {"#334155", "#ffffff"},
                    "admin muted text", new String[] {"#64748b", "#ffffff"});

    private static final Pattern TH_WITHOUT_SCOPE =
            Pattern.compile("<th(?![^>]*\\bscope=)[\\s>]", Pattern.CASE_INSENSITIVE);

    @Test
    void chatPageProvidesLiveRegionsAndNamedInputAreas() throws Exception {
        String html = readTemplate("chat/chat.html");

        assertThat(html)
                .contains(
                        "id=\"statusLine\" role=\"status\"",
                        "id=\"errorLine\" role=\"alert\"",
                        "aria-describedby=\"freeTextHelp\"",
                        "id=\"scenarioList\" role=\"list\"",
                        "id=\"recommendList\" role=\"list\"",
                        "id=\"optionList\" role=\"list\"",
                        "aria-label=\"Conversation messages\"");
    }

    @Test
    void adminDataTablesHaveCaptionsAndScopedColumnHeaders() throws Exception {
        for (String template : ADMIN_TABLE_TEMPLATES) {
            String html = readTemplate(template);

            assertThat(html).as(template + " should expose a table caption.").contains("<caption>");
            assertThat(TH_WITHOUT_SCOPE.matcher(html).find())
                    .as(template + " should use th scope for column headers.")
                    .isFalse();
        }
    }

    @Test
    void keyFormsAndActionsExposeAccessibleNames() throws Exception {
        for (String template : FORM_AND_ACTION_TEMPLATES) {
            Document document = Jsoup.parse(readTemplate(template));

            for (Element control : document.select("input:not([type=hidden]), select, textarea")) {
                assertThat(hasControlName(document, control))
                        .as(template + " should name form control: " + describe(control))
                        .isTrue();
            }

            for (Element action : document.select("button, a.btn, a.button-link")) {
                assertThat(hasActionName(action))
                        .as(template + " should name action: " + describe(action))
                        .isTrue();
            }
        }
    }

    @Test
    void staticColorPairsMeetWcagContrastBaseline() throws Exception {
        String chatCss =
                Files.readString(
                        STATIC_ROOT.resolve("asset/chat/chat.css"), StandardCharsets.UTF_8);
        String adminCss =
                Files.readString(
                        STATIC_ROOT.resolve("asset/admmgr/style2/css/sub.css"),
                        StandardCharsets.UTF_8);

        for (Map.Entry<String, String[]> entry : CONTRAST_PAIRS.entrySet()) {
            String foreground = entry.getValue()[0];
            String background = entry.getValue()[1];

            assertThat(chatCss + adminCss)
                    .as(entry.getKey() + " foreground should be declared in CSS")
                    .contains(foreground);
            assertThat(chatCss + adminCss)
                    .as(entry.getKey() + " background should be declared in CSS")
                    .contains(background);
            assertThat(contrastRatio(foreground, background))
                    .as(entry.getKey() + " should meet WCAG AA 4.5:1")
                    .isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    void ajaxResultAreasAreAnnouncedToScreenReaders() throws Exception {
        assertThat(readTemplate("admmgr/scenario/scenarioGraphEdit.html"))
                .contains("id=\"graphMsg\"", "role=\"status\"", "aria-live=\"polite\"");
        assertThat(readTemplate("admmgr/search/logList.html"))
                .contains("id=\"searchStatus\" role=\"status\" aria-live=\"polite\"");
        assertThat(readTemplate("admmgr/search/popularList.html"))
                .contains("id=\"searchStatus\" role=\"status\" aria-live=\"polite\"");
        assertThat(readTemplate("admmgr/crawl/targetList.html"))
                .contains("id=\"crawlStatus\" role=\"status\" aria-live=\"polite\"");
        assertThat(readTemplate("admmgr/crawl/runList.html"))
                .contains("id=\"crawlRunStatus\" role=\"status\" aria-live=\"polite\"");
        assertThat(readTemplate("admmgr/chat/failureList.html"))
                .contains("id=\"failureStatus\" role=\"status\" aria-live=\"polite\"");
    }

    @Test
    void notificationScreenExposesEmailTypeAndRecipientHelp() throws Exception {
        String html = readTemplate("admmgr/ops/notificationList.html");

        assertThat(html)
                .contains(
                        "value=\"EMAIL_SMTP\"",
                        "Email SMTP",
                        "aria-describedby=\"notificationEndpointHelp\"",
                        "Webhook URL env key or email recipient env key.");
    }

    @Test
    void dynamicGraphEditorControlsKeepAccessibleNames() throws Exception {
        String js =
                Files.readString(
                        STATIC_ROOT.resolve("asset/admmgr/style2/js/ADM.ScenarioGraphEdit.js"),
                        StandardCharsets.UTF_8);

        assertThat(js)
                .contains(
                        "aria-label=\"노드 키\"",
                        "aria-label=\"선택지 버튼 문구\"",
                        "aria-label=\"선택지 추가\"",
                        "aria-label=\"노드 삭제\"",
                        "aria-label=\"선택지 삭제\"",
                        "aria-label=\"메타데이터 항목명\"");
    }

    @Test
    void chatAndAdminStylesExposeVisibleKeyboardFocus() throws Exception {
        String chatCss =
                Files.readString(
                        STATIC_ROOT.resolve("asset/chat/chat.css"), StandardCharsets.UTF_8);
        String adminCss =
                Files.readString(
                        STATIC_ROOT.resolve("asset/admmgr/style2/css/sub.css"),
                        StandardCharsets.UTF_8);

        assertThat(chatCss)
                .contains(":focus-visible", "outline: 3px solid #f59e0b", "outline-offset: 3px");
        assertThat(adminCss)
                .contains(":focus-visible", "outline:3px solid #f59e0b", "outline-offset:3px");
    }

    private String readTemplate(String relativePath) throws Exception {
        return Files.readString(TEMPLATE_ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private boolean hasControlName(Document document, Element control) {
        String id = control.id();
        return hasNonBlankAttribute(control, "aria-label")
                || hasNonBlankAttribute(control, "aria-labelledby")
                || thymeleafAttrContains(control, "aria-label")
                || thymeleafAttrContains(control, "aria-labelledby")
                || (!id.isBlank() && !document.select("label[for=" + id + "]").isEmpty())
                || control.parents().stream()
                        .anyMatch(parent -> "label".equalsIgnoreCase(parent.tagName()));
    }

    private boolean hasActionName(Element action) {
        return !action.text().isBlank()
                || hasNonBlankAttribute(action, "aria-label")
                || hasNonBlankAttribute(action, "aria-labelledby")
                || thymeleafAttrContains(action, "aria-label")
                || thymeleafAttrContains(action, "aria-labelledby")
                || hasNonBlankAttribute(action, "title");
    }

    private boolean hasNonBlankAttribute(Element element, String attribute) {
        return element.hasAttr(attribute) && !element.attr(attribute).isBlank();
    }

    private boolean thymeleafAttrContains(Element element, String attribute) {
        return element.hasAttr("th:attr") && element.attr("th:attr").contains(attribute);
    }

    private String describe(Element element) {
        String id = element.id().isBlank() ? "" : "#" + element.id();
        String name = element.attr("name").isBlank() ? "" : "[name=" + element.attr("name") + "]";
        return element.tagName() + id + name;
    }

    private double contrastRatio(String foreground, String background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private double relativeLuminance(String hex) {
        int red = Integer.parseInt(hex.substring(1, 3), 16);
        int green = Integer.parseInt(hex.substring(3, 5), 16);
        int blue = Integer.parseInt(hex.substring(5, 7), 16);
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue);
    }

    private double linear(int channel) {
        double value = channel / 255.0;
        if (value <= 0.03928) {
            return value / 12.92;
        }
        return Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
