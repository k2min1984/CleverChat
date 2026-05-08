/**
 * 크롤링 도메인.
 *
 * 수집 URL 관리, 수동/자동(Spring {@code @Scheduled}) 실행, robots.txt 준수,
 * Jsoup 파싱, 광고/불필요 문구 제거, 중복 제거(URL/본문 해시),
 * 실행·실패 로그 적재.
 */
package kr.co.cleverchat.domain.crawl;
