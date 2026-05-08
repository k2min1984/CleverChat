/**
 * 비즈니스 도메인 모듈 루트.
 *
 * <h2>모듈 구성</h2>
 * <ul>
 *   <li>{@code auth}     — 인증·인가, 사용자/권한</li>
 *   <li>{@code admin}    — 관리자 화면 공통(레이아웃·대시보드 진입)</li>
 *   <li>{@code scenario} — 시나리오·노드·카테고리·키워드/유사어</li>
 *   <li>{@code chatbot}  — 챗봇 사용자 화면, 진행 엔진, 대화 이력</li>
 *   <li>{@code search}   — 키워드/유사어/부분일치 검색, 검색 로그</li>
 *   <li>{@code crawl}    — 크롤링 URL 관리, 자동/수동 수집, 파싱</li>
 *   <li>{@code ops}      — 통계·공지·감사 조회 등 운영 보조</li>
 * </ul>
 *
 * <h2>모듈 내 하위 패키지 규약</h2>
 * <ul>
 *   <li>{@code controller} — Spring MVC 컨트롤러</li>
 *   <li>{@code service}    — 트랜잭션 경계, 비즈니스 로직</li>
 *   <li>{@code mapper}     — MyBatis 매퍼 인터페이스 ({@code @MapperScan} 대상)</li>
 *   <li>{@code dto}        — 요청/응답 DTO, Bean Validation 적용 대상</li>
 *   <li>{@code model}      — 도메인 모델/엔티티/값객체</li>
 * </ul>
 */
package kr.co.cleverchat.domain;
