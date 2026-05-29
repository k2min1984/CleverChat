package kr.co.cleverchat.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다."),
    STATE_CONFLICT(HttpStatus.CONFLICT, "현재 상태에서 처리할 수 없습니다."),
    SESSION_EXPIRED(HttpStatus.GONE, "Session expired."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests."),
    SEARCH_PII_BLOCKED(HttpStatus.BAD_REQUEST, "Search query contains personal information."),
    SEARCH_RESULT_EMPTY(HttpStatus.NOT_FOUND, "No search results."),
    CRAWL_URL_BLOCKED(HttpStatus.BAD_REQUEST, "Crawl URL is not allowed."),
    CRAWL_ROBOTS_BLOCKED(HttpStatus.BAD_REQUEST, "Crawl URL is blocked by robots.txt."),
    CRAWL_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "Crawl fetch failed."),
    DATA_DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Encrypted data cannot be read."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
