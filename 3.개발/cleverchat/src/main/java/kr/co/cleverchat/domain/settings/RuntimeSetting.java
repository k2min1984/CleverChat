package kr.co.cleverchat.domain.settings;

/** Whitelisted settings shared by validation, the administrator form and runtime consumers. */
public enum RuntimeSetting {
    CRAWL_ENABLED(
            "crawl",
            "수집 작업 실행",
            "boolean",
            true,
            "cleverchat.crawl.worker.enabled",
            0,
            0,
            "끄면 새 작업을 시작하지 않습니다. 진행 중인 작업은 계속됩니다."),
    BROWSER_ENABLED(
            "crawl",
            "브라우저 게시판 수집",
            "boolean",
            false,
            "cleverchat.crawl.browser.enabled",
            0,
            0,
            "동적 게시판에 사용합니다. 서버에 Chromium 실행 환경이 필요합니다."),
    DISCOVERY_LIMIT(
            "crawl",
            "발견 URL 최대 개수",
            "number",
            0,
            "cleverchat.crawl.max-discovered-pages",
            0,
            100000,
            "0은 제한 없음입니다."),
    BOARD_PAGE_LIMIT(
            "crawl",
            "게시판별 최대 목록 페이지",
            "number",
            0,
            "cleverchat.crawl.browser.max-pages",
            0,
            100000,
            "0은 마지막 페이지까지 수집합니다."),
    BOARD_DETAIL_LIMIT(
            "crawl",
            "게시판별 최대 수집 문서",
            "number",
            0,
            "cleverchat.crawl.browser.max-details",
            0,
            100000,
            "0은 제한 없음입니다."),
    JOB_TIMEOUT_MINUTES(
            "crawl",
            "미완료 작업 복구 기준(분)",
            "number",
            180,
            "cleverchat.crawl.browser.job-timeout-minutes",
            10,
            1440,
            "실행 상태로 남은 작업을 다시 처리할 때의 기준입니다."),
    JOB_ATTEMPTS(
            "crawl",
            "작업 최대 시도 횟수",
            "number",
            2,
            "cleverchat.crawl.browser.max-attempts",
            1,
            10,
            "최초 시도를 포함합니다."),
    BOARD_ATTEMPTS(
            "crawl",
            "게시판 최대 시도 횟수",
            "number",
            3,
            "cleverchat.crawl.browser.board-retry-attempts",
            1,
            10,
            "최초 시도를 포함합니다."),
    BOARD_RETRY_DELAY_MS(
            "crawl",
            "게시판 재시도 대기(밀리초)",
            "number",
            10000,
            "cleverchat.crawl.browser.board-retry-delay-ms",
            1000,
            60000,
            "재시도 횟수에 따라 대기 시간이 늘어납니다."),
    BROWSER_TIMEOUT_MS(
            "crawl",
            "브라우저 응답 대기(밀리초)",
            "number",
            30000,
            "cleverchat.crawl.browser.navigation-timeout-ms",
            1000,
            120000,
            "페이지 이동과 요소 대기 시간입니다."),
    HTTP_TIMEOUT_SECONDS(
            "crawl", "일반 페이지 응답 대기(초)", "number", 10, null, 1, 120, "일반 HTML 수집 요청에 적용합니다."),
    SEARCH_POOL(
            "search",
            "검색 후보 개수",
            "number",
            10,
            "chat.search.pool-limit",
            1,
            100,
            "화면에 보여줄 개수 이상으로 지정하세요."),
    SEARCH_DISPLAY(
            "search",
            "검색 그룹별 표시 기준 개수",
            "number",
            5,
            "chat.search.display-max",
            1,
            50,
            "시나리오·문서 각 그룹이 이 개수를 넘으면 관련도 기준으로 결과를 좁힙니다."),
    SEARCH_CRAWL(
            "search",
            "문서 후보 초과 시 최대 표시 개수",
            "number",
            3,
            "chat.search.max-crawl-documents",
            1,
            50,
            "문서 후보가 표시 기준 개수를 넘을 때 적용할 상한입니다."),
    SEARCH_RELATIVE(
            "search",
            "상위 결과 대비 관련도 기준",
            "decimal",
            0.5,
            "chat.search.relative-threshold",
            0,
            1,
            "0~1 사이 값입니다. 높일수록 관련 결과를 좁힙니다."),
    SEARCH_MIN_SCORE(
            "search",
            "검색 최소 점수",
            "decimal",
            0.0,
            "chat.search.min-score",
            0,
            100000,
            "0이면 최소 점수를 두지 않습니다. 모든 결과가 미달이면 최상위 1건을 보여줍니다."),
    SEARCH_MORE(
            "search",
            "검색 결과 더보기",
            "boolean",
            true,
            "chat.search.more-enabled",
            0,
            0,
            "추가 검색 결과를 보여줍니다."),
    CHAT_TTL_MINUTES(
            "chat", "대화 만료 시간(분)", "number", 30, null, 1, 1440, "새 대화 또는 대화 활동 시 만료 시간을 갱신합니다."),
    CHAT_HISTORY_DAYS(
            "chat", "이전 대화 조회 범위(일)", "number", 90, null, 1, 3650, "조회 범위만 변경하며 대화를 삭제하지 않습니다."),
    CHAT_HISTORY_LIMIT("chat", "이전 대화 최대 표시 건수", "number", 50, null, 1, 200, "이전 대화 목록에 적용합니다."),
    CHAT_NO_MATCH(
            "chat",
            "답변을 찾지 못했을 때 안내",
            "text",
            "질문에 맞는 답변을 찾지 못했습니다.",
            null,
            1,
            1000,
            "일반 텍스트로 표시합니다."),
    CHAT_SEARCH_PROMPT(
            "chat",
            "검색 결과 선택 안내",
            "text",
            "관련 자료를 찾았어요. 아래에서 선택해 주세요.",
            null,
            1,
            1000,
            "검색 결과 목록 위에 표시합니다."),
    CHAT_RATE_REQUESTS(
            "chat",
            "허용 요청 횟수",
            "number",
            30,
            "cleverchat.chat.rate-limit.max-requests",
            1,
            10000,
            "아래 기준 시간 안에서 허용할 요청 수입니다."),
    CHAT_RATE_SECONDS(
            "chat",
            "요청 제한 기준 시간(초)",
            "number",
            60,
            "cleverchat.chat.rate-limit.window-seconds",
            1,
            3600,
            "사용자 대화 요청 제한에 적용합니다."),
    LOGIN_FAILURES(
            "login",
            "로그인 실패 허용 횟수",
            "number",
            5,
            "cleverchat.auth.max-failed-attempts",
            2,
            20,
            "독립 로그인에만 적용합니다. SSO 인증 정책은 게이트웨이에서 관리합니다."),
    LOGIN_LOCK_MINUTES(
            "login",
            "계정 잠금 시간(분)",
            "number",
            30,
            "cleverchat.auth.lock-minutes",
            1,
            1440,
            "변경 이후 새로 잠기는 계정에 적용합니다."),
    SESSION_MINUTES(
            "login",
            "로그인 세션 유지 시간(분)",
            "number",
            30,
            null,
            5,
            1440,
            "다음 요청부터 세션의 비활동 만료 시간을 갱신합니다."),
    RETENTION_ENABLED(
            "retention",
            "수집 데이터 자동 정리",
            "boolean",
            true,
            null,
            0,
            0,
            "보관 기간이 지난 실패·중복 문서와 실행 로그를 정리합니다. 성공 문서는 유지합니다."),
    RETENTION_TIME(
            "retention",
            "자동 정리 시각",
            "time",
            "03:45",
            null,
            0,
            0,
            "한국 시간 기준, 하루 한 번 실행합니다. 저장 시 즉시 삭제하지 않습니다."),
    RUN_RETENTION_DAYS(
            "retention", "수집 실행 로그 보관일", "number", 365, null, 30, 3650, "로그 등록 시각을 기준으로 보관합니다."),
    DOCUMENT_RETENTION_DAYS(
            "retention",
            "실패·중복 문서 보관일",
            "number",
            90,
            null,
            30,
            3650,
            "문서 등록 시각 기준입니다. 성공한 수집 문서는 삭제하지 않습니다."),
    SEARCH_RETENTION_DAYS(
            "retention",
            "검색 로그 기본 보관일",
            "number",
            90,
            null,
            30,
            3650,
            "검색 관리의 정리 작업에서 기본값으로 사용합니다."),
    JSON_RETENTION_ENABLED(
            "retention",
            "JSON 파일 자동 정리",
            "boolean",
            false,
            null,
            0,
            0,
            "이 기능 적용 후 시스템이 생성·기록한 파일만 정리합니다."),
    JSON_RETENTION_DAYS(
            "retention",
            "JSON 파일 보관일",
            "number",
            90,
            null,
            30,
            3650,
            "원래 저장 위치와 내용이 유지된 만료 파일만 삭제합니다."),
    AI_ENABLED(
            "ai",
            "AI 답변 사용",
            "boolean",
            false,
            "cleverchat.ai.answer-suggestion.enabled",
            0,
            0,
            "검색 근거가 있을 때 vLLM으로 답변을 생성합니다. 실패하면 기존 검색 답변을 사용합니다."),
    AI_BASE_URL(
            "ai",
            "vLLM API 기본 주소",
            "url",
            "",
            "cleverchat.ai.vllm.base-url",
            0,
            1000,
            "예: http://개발서버:8000/v1 — 게이트웨이 경유 주소도 지정할 수 있습니다."),
    AI_MODEL("ai", "모델명", "text", "", "cleverchat.ai.vllm.model", 0, 200, "서버에 등록된 모델 이름을 입력하세요."),
    AI_TIMEOUT_SECONDS(
            "ai",
            "AI 응답 대기 시간(초)",
            "number",
            60,
            "cleverchat.ai.vllm.timeout-seconds",
            1,
            300,
            "지정한 시간 안에 응답이 없으면 기존 검색 답변을 사용합니다."),
    AI_MAX_TOKENS(
            "ai",
            "답변 최대 토큰",
            "number",
            1024,
            "cleverchat.ai.vllm.max-tokens",
            16,
            8192,
            "모델이 지원하는 범위 안에서 설정하세요."),
    AI_TEMPERATURE(
            "ai",
            "답변 다양성",
            "decimal",
            0.2,
            "cleverchat.ai.vllm.temperature",
            0,
            2,
            "낮을수록 일관된 답변을 생성합니다.");
    public final String section, label, type, property, help;
    public final Object defaultValue;
    public final double min, max;

    RuntimeSetting(
            String section,
            String label,
            String type,
            Object defaultValue,
            String property,
            double min,
            double max,
            String help) {
        this.section = section;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.property = property;
        this.min = min;
        this.max = max;
        this.help = help;
    }

    public String getKey() {
        return name();
    }

    public String getSection() {
        return section;
    }

    public String getLabel() {
        return label;
    }

    public String getType() {
        return type;
    }

    public String getHelp() {
        return help;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getMaxLength() {
        return (int) max;
    }
}
