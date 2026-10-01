import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KepcoScenarioSeeder {
    private static final String BASE = "https://www.kepco.co.kr";
    private static final int MAX_CONTENT_CHARS = 1400;
    private final Map<String, PageContent> pageCache = new LinkedHashMap<>();
    private final List<NodeSeed> nodes = new ArrayList<>();
    private final Map<String, NodeSeed> byKey = new LinkedHashMap<>();
    private final List<ScenarioSeed> scenarios = new ArrayList<>();
    private int keySeq = 1;
    private boolean previewOnly;
    private String currentStartKey;

    public static void main(String[] args) throws Exception {
        KepcoScenarioSeeder seeder = new KepcoScenarioSeeder();
        seeder.previewOnly = args.length > 0 && "--preview".equals(args[0]);
        seeder.run();
    }

    private void run() throws Exception {
        MenuNode menu = crawlMenu();
        buildScenario(menu);
        if (previewOnly) {
            printPreview(menu);
            return;
        }
        persist();
        System.out.printf("Inserted %,d KEPCO scenarios with %,d nodes.%n", scenarios.size(), totalNodes());
    }

    private void printPreview(MenuNode menu) {
        System.out.printf("Generated %,d scenarios and %,d nodes.%n", scenarios.size(), totalNodes());
        printMenu(menu, 0);
    }

    private int totalNodes() {
        return scenarios.stream().mapToInt(scenario -> scenario.nodes.size()).sum();
    }

    private void printMenu(MenuNode node, int depth) {
        System.out.println("  ".repeat(depth) + "- " + node.title + (node.url == null ? "" : " <" + node.url + ">"));
        for (MenuNode child : node.children) {
            printMenu(child, depth + 1);
        }
    }

    private MenuNode crawlMenu() throws Exception {
        Document doc = fetch(BASE + "/");
        MenuNode rootNode = knownMenu(doc);
        enrichBusinessMenu(rootNode);
        return rootNode;
    }

    private MenuNode knownMenu(Document doc) {
        Map<String, String> urls = urlsByLabel(doc);
        MenuNode root = new MenuNode("KEPCO", BASE + "/");

        MenuNode about = top(root, "KEPCO");
        group(about, urls, "KEPCO 소개", "KEPCO 개요", "연혁", "CEO 인사말", "경영방침", "CI");
        group(about, urls, "조직안내", "조직도", "해외지사·법인", "출자회사");
        leaf(about, urls, "전국 본부 · 사업소");
        group(about, urls, "투자정보", "IR정보", "재무정보", "주식정보", "전자공고");
        leaf(about, urls, "인재채용");

        MenuNode business = top(root, "사업분야");
        leaf(business, urls, "송배전사업");
        leaf(business, urls, "판매 · 수요관리");
        leaf(business, urls, "에너지신사업");
        leaf(business, urls, "해외사업");
        leaf(business, urls, "연구개발");

        MenuNode disclosure = top(root, "정보공개");
        group(disclosure, urls, "정보공개제도", "정보공개안내", "정보공개절차", "비공개 대상 정보", "서식 및 수요분석");
        leaf(disclosure, urls, "사전정보공표");
        group(disclosure, urls, "공공데이터제공", "공공데이터제공 제도", "공공데이터제공 목록", "공공데이터 설문조사");
        leaf(disclosure, urls, "사업실명제");
        group(disclosure, urls, "송변전 건설 정보공개", "송변전 건설 사업현황", "건설절차", "보상 및 지원", "전자파 정보");
        group(disclosure, urls, "경영공시", "경영공시 전체보기", "기타");
        group(disclosure, urls, "내부규정", "사규 및 하위 규범", "제ㆍ개정 예고");

        MenuNode esg = top(root, "ESG경영");
        group(esg, urls, "한전의 ESG", "ESG 개요", "핵심성과", "추진체계", "중대성 평가", "이해관계자 참여");
        group(esg, urls, "환경E", "환경경영", "탄소중립");
        group(esg, urls, "사회S", "사회공헌", "안전경영", "동반성장(기업센터)", "인권경영", "인재경영");
        group(esg, urls, "지배구조G", "이사회", "윤리경영", "청렴경영", "재취업 정보공유");
        leaf(esg, urls, "ESG 리포트");
        leaf(esg, urls, "ESG 데이터");

        MenuNode media = top(root, "홍보센터");
        group(media, urls, "뉴스룸", "뉴스 · 미디어", "공지사항", "보도·설명자료", "소셜미디어", "사보", "홍보영상", "광고");
        group(media, urls, "전시관람", "본사홍보관", "전기박물관");
        group(media, urls, "문화 · 스포츠", "한전아트센터", "스포츠단");

        MenuNode customer = top(root, "고객소통");
        leaf(customer, urls, "고객소통안내");
        leaf(customer, urls, "온라인 민원");
        leaf(customer, urls, "본사방문 및 서면 민원");
        group(customer, urls, "신문고", "부패/부조리 신고", "KEPCO 옴부즈만");
        group(customer, urls, "안전신고 및 제안", "안전신고", "안전제안");
        leaf(customer, urls, "정전피해배상제도");
        leaf(customer, urls, "공공시설 개방");
        group(customer, urls, "자료실", "전력통계", "배전인력조회");

        return root;
    }

    private Map<String, String> urlsByLabel(Document doc) {
        Map<String, String> urls = new LinkedHashMap<>();
        for (Element a : doc.select("a[href]")) {
            String label = cleanTitle(a.text());
            String href = a.absUrl("href");
            if (validMenuLink(label, href)) {
                urls.putIfAbsent(label, href);
            }
        }
        return urls;
    }

    private MenuNode top(MenuNode root, String title) {
        MenuNode node = new MenuNode(title, null);
        root.children.add(node);
        return node;
    }

    private void group(MenuNode parent, Map<String, String> urls, String title, String... children) {
        MenuNode node = new MenuNode(title, urls.getOrDefault(title, firstChildUrl(urls, children)));
        parent.children.add(node);
        for (String child : children) {
            leaf(node, urls, child);
        }
    }

    private String firstChildUrl(Map<String, String> urls, String... children) {
        for (String child : children) {
            String url = urls.get(child);
            if (url != null) return url;
        }
        return null;
    }

    private void leaf(MenuNode parent, Map<String, String> urls, String title) {
        String url = urls.get(title);
        if (url != null) parent.children.add(new MenuNode(title, url));
    }

    private void enrichBusinessMenu(MenuNode root) {
        MenuNode business = findChild(root, "사업분야");
        if (business == null) return;
        for (MenuNode child : business.children) {
            PageContent page = page(child.url);
            List<PageSection> h2s = page.sections.stream()
                    .filter(section -> section.level == 2)
                    .filter(section -> !"사업분야".equals(section.title))
                    .filter(section -> !section.title.contains("더보기"))
                    .toList();
            if (h2s.size() < 2) continue;
            child.children.clear();
            for (PageSection section : h2s) {
                MenuNode sectionNode = new MenuNode(cleanTitle(section.title), child.url);
                child.children.add(sectionNode);
                List<PageSection> h3s = page.sections.stream()
                        .filter(s -> s.level == 3 && s.parentTitle.equals(section.title))
                        .toList();
                if (h3s.size() >= 2) {
                    for (PageSection h3 : h3s) {
                        sectionNode.children.add(new MenuNode(cleanTitle(h3.title), child.url));
                    }
                }
            }
        }
    }

    private void buildScenario(MenuNode menu) {
        int menuCount = menu.children.size();
        int menuIndex = 0;
        for (MenuNode top : menu.children) {
            int sortOrder = Math.max(1, menuCount - menuIndex) * 100;
            menuIndex++;
            nodes.clear();
            byKey.clear();
            String scenarioPrefix = normalize(top.title);
            currentStartKey = scenarioPrefix + "_start";
            NodeSeed start = question(currentStartKey, top.title + " 안내", top.title + "에 대해 안내해 드리겠습니다.\n\n다음 중 궁금하신 내용을 선택해 주세요.");
            for (MenuNode child : top.children) {
                List<String> childPath = new ArrayList<>();
                childPath.add(top.title);
                childPath.add(child.title);
                NodeSeed childNode = buildMenuNode(child, childPath);
                start.options.add(new OptionSeed(child.title, childNode.key));
            }
            scenarios.add(
                    new ScenarioSeed(
                            top.title,
                            "KEPCO 홈페이지의 " + top.title + " 메뉴 구조와 상세 내용을 기반으로 구성한 상담 주제입니다.",
                            sortOrder,
                            new ArrayList<>(nodes)));
        }
    }

    private NodeSeed buildMenuNode(MenuNode menu, List<String> path) {
        if (menu.children.isEmpty()) {
            return answerNode(menu, path);
        }
        String key = key(path);
        StringBuilder content = new StringBuilder();
        content.append(menu.title).append("에 대해 안내해 드리겠습니다.\n\n");
        content.append("다음 중 궁금하신 내용을 선택해 주세요.");
        NodeSeed node = question(key, menu.title, content.toString());
        for (MenuNode child : menu.children) {
            List<String> childPath = new ArrayList<>(path);
            childPath.add(child.title);
            NodeSeed childNode = buildMenuNode(child, childPath);
            node.options.add(new OptionSeed(child.title, childNode.key));
        }
        return node;
    }

    private NodeSeed answerNode(MenuNode menu, List<String> path) {
        PageContent page = page(menu.url);
        String title = path.get(path.size() - 1);
        String content = answerContent(title, path, page);
        NodeSeed node = answer(key(path), title, content);
        if (menu.url != null && !menu.url.isBlank()) {
            node.links.add(new LinkSeed(title + " 바로가기", menu.url));
        }
        return node;
    }

    private String answerContent(String title, List<String> path, PageContent page) {
        StringBuilder content = new StringBuilder();
        content.append(title).append("에 대해 안내해 드리겠습니다.\n\n");
        content.append(formatFocusedAnswer(title, page));
        content.append("\n\n확인 경로: ").append(String.join(" | ", path));
        if (page.url != null && !page.url.isBlank()) {
            content.append("\n\n자세한 내용은 아래 바로가기 버튼을 통해 확인하실 수 있습니다.");
        }
        return content.toString();
    }

    private String formatFocusedAnswer(String title, PageContent page) {
        PageSection section = focusedSection(title, page);
        String sliced = sliceKnownSection(title, page.summary);
        if (!sliced.isBlank()) {
            String numbered = numberedSummary(sliced);
            if (!numbered.isBlank()) return numbered;
            return bulletSummary(title, sliced, 5);
        }
        if (section != null) {
            List<PageSection> children =
                    page.sections.stream()
                            .filter(s -> s.level > section.level && normalize(s.parentTitle).equals(normalize(section.title)))
                            .filter(s -> !s.text.isBlank())
                            .limit(5)
                            .toList();
            if (!children.isEmpty()) {
                StringBuilder text = new StringBuilder();
                String overview = removeChildText(section.text, children);
                if (!overview.isBlank()) {
                    text.append("개요\n");
                    text.append("- ").append(summarizeSentence(overview, 260)).append("\n\n");
                }
                text.append("핵심 내용\n");
                for (PageSection child : children) {
                    text.append("- ").append(child.title).append(": ")
                            .append(summarizeSentence(child.text, 260))
                            .append("\n");
                }
                return text.toString().trim();
            }
            return bulletSummary(title, section.text, 5);
        }
        return bulletSummary(title, page.summary, 5);
    }

    private String sliceKnownSection(String title, String text) {
        List<String> sectionTitles =
                List.of(
                        "송변전사업", "송전사업", "변전사업", "HVDC", "배전사업", "전력공급", "배전운영", "전력계량",
                        "전력판매", "수요관리",
                        "마이크로그리드 사업", "태양광 발전사업", "전기차 플랫폼 사업", "청정 수소사업",
                        "해외사업 현황", "발전사업", "그리드 사업", "에너지 신사업", "화력", "원자력", "재생에너지",
                        "연구개발 목표", "미래전력망 구축", "탄소중립선도", "경영효율향상", "공급안정·고장감소", "안전·재난·환경 대응");
        if (!sectionTitles.contains(title)) return "";
        String source = cleanText(text);
        int start = source.indexOf(title + " ");
        if (start < 0) return "";
        int betterStart = source.indexOf(title + " ", start + title.length());
        if (betterStart >= 0) start = betterStart;
        int end = source.length();
        for (String next : sectionTitles) {
            if (next.equals(title)) continue;
            int idx = source.indexOf(next + " ", start + title.length());
            if (idx > start && idx < end) end = idx;
        }
        if (end <= start) return "";
        return cleanText(source.substring(start, end));
    }

    private String numberedSummary(String text) {
        String source = cleanText(text);
        if (!Pattern.compile("\\b0[1-9]\\s+").matcher(source).find()) return "";
        String[] parts = source.split("(?=\\b0[1-9]\\s+)");
        String overview = cleanText(parts[0]);
        StringBuilder out = new StringBuilder();
        if (!overview.isBlank()) {
            out.append("개요\n");
            out.append("- ").append(summarizeSentence(removeRepeatedTitle(overview), 260)).append("\n\n");
        }
        out.append("핵심 내용\n");
        int count = 0;
        for (int i = 1; i < parts.length; i++) {
            String item = cleanText(parts[i]).replaceFirst("^0[1-9]\\s+", "");
            if (item.isBlank()) continue;
            String heading = firstWord(item);
            String body = item;
            int bodyStart = Math.max(item.indexOf(heading + "은"), item.indexOf(heading + "는"));
            if (bodyStart > 0) body = item.substring(bodyStart);
            out.append("- ").append(heading).append(": ").append(summarizeSentence(body, 260)).append("\n");
            count++;
            if (count >= 6) break;
        }
        return out.toString().trim();
    }

    private String firstWord(String text) {
        String cleaned = cleanText(text);
        int space = cleaned.indexOf(' ');
        return space < 0 ? cleaned : cleaned.substring(0, space);
    }

    private String removeRepeatedTitle(String text) {
        String cleaned = cleanText(text);
        int first = cleaned.indexOf(' ');
        if (first > 0 && first < cleaned.length() - 1) {
            String title = cleaned.substring(0, first);
            String rest = cleaned.substring(first + 1);
            if (rest.startsWith(title)) return rest;
        }
        return cleaned;
    }

    private PageSection focusedSection(String title, PageContent page) {
        for (PageSection section : page.sections) {
            if (normalize(section.title).equals(normalize(title)) && !section.text.isBlank()) {
                return section;
            }
        }
        for (PageSection section : page.sections) {
            if (normalize(section.title).contains(normalize(title)) && !section.text.isBlank()) {
                return section;
            }
        }
        return null;
    }

    private String bulletSummary(String title, String text, int maxBullets) {
        List<String> sentences = sentences(text);
        StringBuilder out = new StringBuilder("핵심 내용\n");
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.length() < 12 || isNoise(sentence)) continue;
            out.append("- ").append(summarizeSentence(sentence, 260)).append("\n");
            count++;
            if (count >= maxBullets) break;
        }
        if (count == 0) {
            out.append("- ").append(title).append(" 메뉴는 KEPCO 홈페이지에서 제공하는 상세 안내 페이지입니다.\n");
            out.append("- 세부 안내, 최신 게시물, 신청·조회 화면 등은 아래 바로가기 버튼을 통해 원문 페이지에서 확인할 수 있습니다.\n");
            out.append("- 게시판형 메뉴의 경우 최신 목록은 접속 시점에 따라 달라질 수 있으므로 원문 페이지 확인이 필요합니다.\n");
        }
        return out.toString().trim();
    }

    private List<String> sentences(String text) {
        String cleaned = cleanText(text)
                .replace("다. ", "다.|")
                .replace(". ", ".|")
                .replace("? ", "?|")
                .replace("! ", "!|");
        List<String> result = new ArrayList<>();
        for (String part : cleaned.split(Pattern.quote("|"))) {
            String sentence = cleanText(part);
            if (!sentence.isBlank()) result.add(sentence);
        }
        return result;
    }

    private String summarizeSentence(String text, int max) {
        String cleaned = cleanText(text);
        if (cleaned.length() <= max) return cleaned;
        int cut = Math.max(cleaned.lastIndexOf("다.", max), cleaned.lastIndexOf(".", max));
        if (cut < 80) cut = max;
        return cleaned.substring(0, Math.min(cut + 1, cleaned.length())).trim();
    }

    private String removeChildText(String text, List<PageSection> children) {
        String result = cleanText(text);
        for (PageSection child : children) {
            String childText = cleanText(child.text);
            if (!childText.isBlank() && result.contains(childText)) {
                result = result.replace(childText, "");
            }
        }
        return cleanText(result);
    }

    private PageContent page(String url) {
        if (url == null || url.isBlank()) return PageContent.empty();
        return pageCache.computeIfAbsent(url, this::crawlPage);
    }

    private PageContent crawlPage(String url) {
        try {
            Document doc = fetch(url);
            Element main = contentRoot(doc);
            main.select("script, style, nav, header, footer, .breadcrumb, .location, .share, .sns, .quick").remove();
            String title = cleanTitle(textOf(doc.selectFirst("h1")));
            if (title.isBlank()) title = cleanTitle(doc.title().replace("| 한국전력공사", ""));
            List<PageSection> sections = sections(main);
            String summary = cleanText(main.text());
            summary = dropMenuNoise(summary);
            return new PageContent(url, title, trimText(summary, MAX_CONTENT_CHARS), sections);
        } catch (Exception e) {
            return new PageContent(url, "", "페이지 내용을 수집하지 못했습니다: " + e.getMessage(), List.of());
        }
    }

    private Element contentRoot(Document doc) {
        Elements candidates = doc.select("#contents, #content, .contents, .content, .sub-content, main");
        Element best = null;
        int bestScore = -1;
        for (Element candidate : candidates) {
            String text = candidate.text();
            int score = 0;
            if (candidate.select("h1").size() > 0) score += 100;
            if (candidate.select("h2,h3").size() > 0) score += 50;
            if (text.contains("페이지 아래로 이동")) score += 30;
            if (text.contains("전체메뉴")) score -= 80;
            score += Math.min(text.length(), 3000) / 100;
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        if (best == null) best = doc.body();
        Element h1 = best.selectFirst("h1");
        if (h1 == null) return best;
        Element wrapper = h1.parent();
        for (int i = 0; i < 4 && wrapper != null; i++, wrapper = wrapper.parent()) {
            String text = wrapper.text();
            if (text.length() > 500 && !text.contains("전체메뉴")) {
                return wrapper;
            }
        }
        return best;
    }

    private List<PageSection> sections(Element main) {
        List<PageSection> sections = new ArrayList<>();
        Elements headings = main.select("h2, h3, h4");
        for (Element h : headings) {
            String title = cleanTitle(h.text());
            if (title.isBlank() || isNoise(title)) continue;
            int level = Integer.parseInt(h.tagName().substring(1));
            StringBuilder text = new StringBuilder();
            Element cur = h.nextElementSibling();
            while (cur != null && !cur.tagName().matches("h[234]")) {
                if (!cur.tagName().matches("script|style|nav")) {
                    String t = cleanText(cur.text());
                    if (!t.isBlank()) text.append(t).append("\n");
                }
                cur = cur.nextElementSibling();
            }
            String parent = "";
            if (level > 2) {
                Element prev = h.previousElementSibling();
                while (prev != null) {
                    if ("h2".equals(prev.tagName())) {
                        parent = cleanTitle(prev.text());
                        break;
                    }
                    prev = prev.previousElementSibling();
                }
            }
            sections.add(new PageSection(level, title, parent, trimText(text.toString(), MAX_CONTENT_CHARS)));
        }
        return sections;
    }

    private Document fetch(String url) throws Exception {
        return Jsoup.connect(url)
                .userAgent("CleverChatCrawler/1.0")
                .timeout(15000)
                .maxBodySize(5 * 1024 * 1024)
                .get();
    }

    private Element findHeading(Element root, String text) {
        for (Element e : root.select("h1,h2,h3,h4,strong,a,button,span")) {
            if (cleanTitle(e.text()).equals(text)) return e;
        }
        return null;
    }

    private Element nextMenuScope(Element heading) {
        Element parent = heading.parent();
        for (int i = 0; i < 4 && parent != null; i++, parent = parent.parent()) {
            if (parent.select("a[href]").size() >= 2) return parent;
        }
        return heading.parent();
    }

    private void fillChildrenFromScope(MenuNode top, Element scope, String topName) {
        Set<String> seen = new LinkedHashSet<>();
        for (Element a : scope.select("a[href]")) {
            String label = cleanTitle(a.text());
            String href = a.absUrl("href");
            if (!validMenuLink(label, href) || label.equals(topName)) continue;
            if (seen.add(label + href)) top.children.add(new MenuNode(label, href));
            if (top.children.size() >= 18) break;
        }
    }

    private void fillChildrenByFollowingLinks(MenuNode top, Element root, String topName) {
        boolean collect = false;
        Set<String> seen = new LinkedHashSet<>();
        for (Element e : root.select("h2,h3,a")) {
            String text = cleanTitle(e.text());
            if (text.equals(topName)) {
                collect = true;
                continue;
            }
            if (collect && e.tagName().matches("h2") && !text.equals(topName)) break;
            if (collect && "a".equals(e.tagName())) {
                String href = e.absUrl("href");
                if (validMenuLink(text, href) && seen.add(text + href)) {
                    top.children.add(new MenuNode(text, href));
                }
            }
        }
    }

    private boolean validMenuLink(String label, String href) {
        if (label.isBlank() || href.isBlank()) return false;
        if (!href.startsWith(BASE)) return false;
        if (label.contains("메뉴 열기") || label.contains("닫기") || label.contains("본문")) return false;
        return href.contains("/home/");
    }

    private NodeSeed question(String key, String title, String content) {
        return node(key, "QUESTION", title, content);
    }

    private NodeSeed answer(String key, String title, String content) {
        return node(key, "ANSWER", title, content);
    }

    private NodeSeed node(String key, String type, String title, String content) {
        if (byKey.containsKey(key)) return byKey.get(key);
        NodeSeed node = new NodeSeed(key, type, title, content, nodes.size() + 1);
        nodes.add(node);
        byKey.put(key, node);
        return node;
    }

    private String key(List<String> path) {
        String raw = String.join("_", path);
        String compact = normalize(raw).replaceAll("[^a-z0-9]+", "_");
        if (compact.length() > 64) compact = compact.substring(0, 64);
        String key = "kepco_" + compact;
        if (!byKey.containsKey(key)) return key;
        return key + "_" + (keySeq++);
    }

    private void persist() throws Exception {
        String url = env("DB_URL", "jdbc:postgresql://dev.c2r.co.kr:45432/postgres?currentSchema=cleverchat_dev,public");
        String user = env("DB_USER", "cleverchat");
        String password = env("DB_PASSWORD", "cleverchat");
        try (Connection con = DriverManager.getConnection(url, user, password)) {
            con.setAutoCommit(false);
            try {
                markExistingScenariosDeleted(con);
                long categoryId = ensureCategory(con);
                for (ScenarioSeed scenario : scenarios) {
                    long scenarioId = insertScenario(con, categoryId, scenario);
                    long versionId = insertVersion(con, scenarioId);
                    Map<String, Long> nodeIds = insertNodes(con, versionId, scenario.nodes);
                    insertLinks(con, nodeIds, scenario.nodes);
                    insertOptions(con, nodeIds, scenario.nodes);
                    long startId = nodeIds.get(scenario.nodes.get(0).key);
                    updateVersionStart(con, versionId, startId);
                    publishAndActivate(con, scenarioId, versionId);
                    insertKeywords(con, scenarioId, scenario);
                }
                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    private void markExistingScenariosDeleted(Connection con) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tb_scenario SET status='DELETED', lst_chg_dt=now() WHERE status <> 'DELETED'")) {
            ps.executeUpdate();
        }
    }

    private long ensureCategory(Connection con) throws Exception {
        try (PreparedStatement select = con.prepareStatement(
                "SELECT scenario_category_no FROM tb_scenario_category WHERE name=? AND use_yn='Y' ORDER BY scenario_category_no LIMIT 1")) {
            select.setString(1, "KEPCO");
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        try (PreparedStatement insert = con.prepareStatement(
                "INSERT INTO tb_scenario_category (name, sort_order, use_yn) VALUES (?, 1, 'Y') RETURNING scenario_category_no")) {
            insert.setString(1, "KEPCO");
            try (ResultSet rs = insert.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertScenario(Connection con, long categoryId, ScenarioSeed scenario) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario (category_no, title, description, status, sort_order) VALUES (?, ?, ?, 'ACTIVE', ?) RETURNING scenario_no")) {
            ps.setLong(1, categoryId);
            ps.setString(2, scenario.title);
            ps.setString(3, scenario.description);
            ps.setInt(4, scenario.sortOrder);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private long insertVersion(Connection con, long scenarioId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario_version (scenario_no, version_no, status, frst_regr_empno, published_at) VALUES (?, 1, 'PUBLISHED', 'crawler', now()) RETURNING scenario_version_no")) {
            ps.setLong(1, scenarioId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private Map<String, Long> insertNodes(Connection con, long versionId, List<NodeSeed> scenarioNodes) throws Exception {
        Map<String, Long> ids = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario_node (version_no, node_key, node_type, title, content, sort_order, metadata) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb) RETURNING scenario_node_no")) {
            for (NodeSeed node : scenarioNodes) {
                ps.setLong(1, versionId);
                ps.setString(2, node.key);
                ps.setString(3, node.type);
                ps.setString(4, node.title);
                ps.setString(5, node.content);
                ps.setInt(6, node.sortOrder);
                ps.setString(7, "{\"source\":\"KEPCO\",\"generatedAt\":\"" + OffsetDateTime.now() + "\"}");
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    ids.put(node.key, rs.getLong(1));
                }
            }
        }
        return ids;
    }

    private void insertLinks(Connection con, Map<String, Long> nodeIds, List<NodeSeed> scenarioNodes) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario_node_link (node_no, label, url, link_type, sort_order, use_yn) VALUES (?, ?, ?, 'EXTERNAL', ?, 'Y')")) {
            for (NodeSeed node : scenarioNodes) {
                int sort = 1;
                for (LinkSeed link : node.links) {
                    ps.setLong(1, nodeIds.get(node.key));
                    ps.setString(2, link.label);
                    ps.setString(3, link.url);
                    ps.setInt(4, sort++);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void insertOptions(Connection con, Map<String, Long> nodeIds, List<NodeSeed> scenarioNodes) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario_node_option (node_no, next_node_no, label, sort_order, use_yn) VALUES (?, ?, ?, ?, 'Y')")) {
            for (NodeSeed node : scenarioNodes) {
                int sort = 1;
                for (OptionSeed option : node.options) {
                    ps.setLong(1, nodeIds.get(node.key));
                    ps.setLong(2, nodeIds.get(option.nextKey));
                    ps.setString(3, option.label);
                    ps.setInt(4, sort++);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private void updateVersionStart(Connection con, long versionId, long startId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tb_scenario_version SET start_node_no=? WHERE scenario_version_no=?")) {
            ps.setLong(1, startId);
            ps.setLong(2, versionId);
            ps.executeUpdate();
        }
    }

    private void publishAndActivate(Connection con, long scenarioId, long versionId) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE tb_scenario SET active_version_no=?, status='ACTIVE', lst_chg_dt=now() WHERE scenario_no=?")) {
            ps.setLong(1, versionId);
            ps.setLong(2, scenarioId);
            ps.executeUpdate();
        }
    }

    private void insertKeywords(Connection con, long scenarioId, ScenarioSeed scenario) throws Exception {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.add("KEPCO");
        keywords.add("한국전력");
        keywords.add("한전");
        keywords.add(scenario.title);
        for (NodeSeed node : scenario.nodes) {
            keywords.add(node.title);
            if (keywords.size() >= 18) break;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tb_scenario_keyword (scenario_no, keyword, weight, use_yn) VALUES (?, ?, 100, 'Y')")) {
            for (String keyword : keywords) {
                ps.setLong(1, scenarioId);
                ps.setString(2, keyword);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String textOf(Element element) {
        return element == null ? "" : element.text();
    }

    private static String cleanTitle(String value) {
        return cleanText(value)
                .replace("메뉴 열기", "")
                .replace("새창열림", "")
                .replaceAll("^\\d+\\s*", "")
                .trim();
    }

    private static String cleanText(String value) {
        if (value == null) return "";
        return value.replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .replaceAll("\\s+([,.])", "$1")
                .trim();
    }

    private static String dropMenuNoise(String value) {
        int home = value.indexOf("홈 /");
        if (home >= 0) value = value.substring(home + 3);
        return value.replace("페이지 아래로 이동 페이지 아래로 이동", "").trim();
    }

    private static String trimText(String value, int max) {
        String text = cleanText(value);
        if (text.length() <= max) return text;
        int cut = Math.max(text.lastIndexOf('.', max), text.lastIndexOf("다.", max));
        if (cut < 300) cut = max;
        return text.substring(0, Math.min(cut + 1, text.length())).trim();
    }

    private static String normalize(String value) {
        return cleanText(value)
                .toLowerCase(Locale.ROOT)
                .replace("·", "")
                .replace("ㆍ", "")
                .replaceAll("[^a-z0-9가-힣]+", "");
    }

    private static boolean isNoise(String title) {
        return title.equals("전체메뉴") || title.equals("관련사이트") || title.equals("전국사업소") || title.equals("검색");
    }

    private MenuNode findChild(MenuNode node, String title) {
        for (MenuNode child : node.children) {
            if (child.title.equals(title)) return child;
        }
        return null;
    }

    private record MenuNode(String title, String url, List<MenuNode> children) {
        MenuNode(String title, String url) {
            this(title, url, new ArrayList<>());
        }
    }

    private record PageContent(String url, String title, String summary, List<PageSection> sections) {
        static PageContent empty() {
            return new PageContent("", "", "", List.of());
        }
    }

    private record PageSection(int level, String title, String parentTitle, String text) {}

    private record OptionSeed(String label, String nextKey) {}

    private record LinkSeed(String label, String url) {}

    private record ScenarioSeed(String title, String description, int sortOrder, List<NodeSeed> nodes) {}

    private static class NodeSeed {
        final String key;
        final String type;
        final String title;
        final String content;
        final int sortOrder;
        final List<OptionSeed> options = new ArrayList<>();
        final List<LinkSeed> links = new ArrayList<>();

        NodeSeed(String key, String type, String title, String content, int sortOrder) {
            this.key = key;
            this.type = type;
            this.title = title;
            this.content = content;
            this.sortOrder = sortOrder;
        }
    }
}
