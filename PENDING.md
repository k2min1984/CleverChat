# PENDING

작성일: 2026-05-29

아래 항목은 현재 저장소 기준으로 바로 구현하기보다 사용자/운영 결정이 먼저 필요한 항목이다.

## 0. 판단 원칙

- 목표는 "온프레미스 전용"이 아니라 "온프레미스 환경에서도 동일 동작 가능한 기본 경로"를 보장하는 것이다.
- 기본 경로는 PostgreSQL, 파일 로그, OS env/외부 properties, 자체 세션/CSRF, 내부망 SMTP/webhook처럼 외부 SaaS 없이 운영 가능한 구성으로 둔다.
- Redis, Slack, 외부 email provider, S3 호환 스토리지, KMS/Vault, 외부 LLM, ELK, OAuth/OpenAPI gateway는 optional adapter 또는 후속 확장으로 둔다.
- optional adapter가 꺼져 있거나 장애가 나도 핵심 업무 흐름은 no-op, 내부 기본 구현, fail-open/fail-closed 정책 중 문서화된 방식으로 예측 가능하게 degrade 되어야 한다.
- 새 결정을 할 때는 "기본 경로", "optional 확장 경로", "장애 시 동작", "비밀값 저장 위치", "온프레미스 운영 절차"를 함께 확정한다.

## 1. Redis 확대 적용 범위

- 현재 상태: 챗봇 rate limit은 memory 기본 + optional Redis store 구현 완료
- 기본 경로: Spring Session JDBC, Caffeine 매칭 캐시, memory rate limit, PostgreSQL 기반 스케줄/크롤링 상태 관리
- optional 확장: Redis rate limit store, Redis 세션, Redis 캐시, Redis/DB 기반 cluster lock, nginx `limit_req`
- 권장 작업: 당장은 Redis 전면 전환을 하지 않고, 다중 노드/HA 전환이 확정될 때 Spring Session Redis와 cluster lock을 별도 작업지시서로 분리한다.
- 결정 필요:
  - Spring Session을 Redis로 전환할지
  - 시나리오 매칭 캐시를 Redis로 전환할지
  - 스케줄러/크롤링 claim에 Redis 또는 DB lock을 사용할지
  - nginx `limit_req`를 edge rate limit으로 병행할지

## 2. M9 RAG/AI 실제 provider

- 현재 상태: provider-neutral scaffold와 no-op/stub 경계만 존재
- 기본 경로: AI 추천 disabled/no-op, 기존 M4 검색 fallback 유지
- optional 확장: 내부망 LLM, 외부 LLM API, PostgreSQL pgvector 기반 RAG
- 권장 작업: 실제 provider를 붙이기 전까지 no-op을 기본값으로 유지하고, 도입 시 PII 마스킹/차단 후 입력, citation 필수, timeout/rate limit을 먼저 확정한다.
- 결정 필요:
  - 외부 LLM API 사용 여부
  - 온프레미스/내부 LLM 요구 여부
  - pgvector 도입 여부
  - RAG 입력으로 전달 가능한 데이터 범위와 PII 정책
  - 비용/timeout/rate limit 기준

## 3. M10 외부 REST API 실제 공개 범위

- 현재 상태: `/external/api/v1/status` disabled stub 경계만 존재
- 기본 경로: external API disabled, status stub만 제공, 업무 데이터 미노출
- optional 확장: API key hash 기반 공개 API, OAuth2 client credentials, CORS allowlist, OpenAPI 문서화
- 권장 작업: 실제 공개 전까지 disabled 기본값을 유지하고, 공개가 필요하면 정적 OpenAPI YAML과 단일 API key hash 방식부터 시작한다.
- 결정 필요:
  - 공개할 업무 endpoint 범위
  - API key 단일 hash 방식 유지 또는 DB 기반 key 관리 도입
  - OAuth2 client credentials 도입 여부
  - CORS 허용 도메인
  - OpenAPI 관리 방식: springdoc 도입 또는 정적 YAML

## 4. M7 CSP nonce 전환

- 현재 상태: 공통 CSP 기본 정책 적용
- 기본 경로: 서버 공통 CSP, `frame-ancestors 'none'`, same-origin script/connect 정책 유지
- optional 확장: nonce 기반 inline 허용, CSP report endpoint, 화면별 report-only 전환 기간
- 권장 작업: 먼저 inline script/style 제거 가능한 화면을 정리하고, 남는 화면만 nonce 후보로 둔다.
- 결정 필요:
  - inline script/style을 제거할지, nonce 기반으로 허용할지
  - nonce 적용 대상 화면과 전환 일정
  - CSP report endpoint 도입 여부

## 5. 백업 외부 스토리지와 PITR 운영 수준

- 현재 상태: `pg_dump` 스크립트와 백업/복구 절차 문서 기본형 작성
- 기본 경로: PostgreSQL `pg_dump -Fc`, checksum, OS scheduler, 내부/NAS/별도 백업 서버 보관, 복구 리허설
- optional 확장: S3 호환 스토리지, WAL archiving/PITR, 백업 파일 암호화, 백업 성공/실패 알림 연동
- 권장 작업: v1은 `pg_dump` + 내부/NAS 보관 + 리허설 기록을 필수로 두고, PITR/WAL archiving은 RPO/RTO 요구가 확정되면 켠다.
- 결정 필요:
  - 외부 스토리지 종류: NAS, S3 호환, 별도 백업 서버
  - WAL archiving 적용 여부
  - RPO/RTO 최종 수치
  - 백업 파일 암호화 방식

## 6. 외부 알림 provider 고도화

- 현재 상태: generic webhook, Slack-compatible webhook, SMTP email 기본형과 `next_retry_at` 기반 retry worker 기본형 구현 완료
- 기본 경로: 내부망 generic webhook 또는 내부 SMTP, OS env로 endpoint/recipient/secret 참조
- optional 확장: Slack OAuth/Web API, email provider API, provider별 template 관리, secret manager 연동
- 권장 작업: Slack/email 전용 SaaS 기능은 optional로 두고, 내부 SMTP/generic webhook 운영 절차와 retry 주기만 먼저 확정한다.
- 결정 필요:
  - provider별 template 관리 방식
  - 기본 60초 retry worker 주기를 운영 환경에서 변경할지 여부
  - Slack OAuth/Web API 또는 email provider API 도입 여부
  - secret manager 연동 여부

## 7. M6 이상행위 탐지 고도화 기준

- 현재 상태: 로그인 실패 spike와 계정 잠금 security event 기본형 구현 완료
- 기본 경로: `security.log`, `api.log`, `audit_log` 기반의 내부 룰과 관리자 확인 흐름
- optional 확장: ELK/OpenSearch/Loki 등 외부 로그 수집, 알림 연동, 별도 룰 히스토리 테이블
- 권장 작업: 외부 GeoIP/보안 SaaS 없이도 동작하도록 IP hash/대역 allowlist, 경로별 5xx 카운트, 검색 횟수 threshold 기반 룰부터 정의한다.
- 결정 필요:
  - 관리자 IP 변경 탐지 기준: 동일 계정의 국가/대역 변경, 시간 창, 허용 IP 목록 적용 여부
  - 대량 검색 탐지 기준: 분당/시간당 검색 횟수와 관리자·챗봇 사용자 구분 여부
  - API 5xx 반복 탐지 기준: 대상 경로, 시간 창, 임계 횟수, 알림 연동 여부
  - 룰 히스토리를 DB에 저장할지, 파일 로그 기반으로만 운영할지
