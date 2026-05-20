# Phase1A-F CSRF 검증 인터셉터 작업지시

> 작성일: 2026-05-20  
> 작업 원칙: 이 문서는 작업지시서이다. 본 문서 작성 단계에서는 코드 수정, 테스트 코드 생성, 설정 변경을 수행하지 않는다.  
> 구현 전제: 관리자 인증/인가, 세션, CSRF는 Spring Security CSRF가 아니라 기존 자체 `HandlerInterceptor`와 세션 VO 구조를 기준으로 한다.

## 1. 목적과 핵심 판단

관리자 영역의 POST 요청에 대해 Phase1A-E에서 발급한 세션 CSRF 값을 검증하고, 검증 성공 시 새 CSRF 값을 응답 헤더로 재발급한다. 목적은 기존 자체 인증 흐름을 유지하면서 Ajax POST 요청의 위조 가능성을 줄이고, 연속 Ajax 호출이 갱신된 토큰으로 계속 동작하게 하는 것이다.

핵심 판단:

- Spring Security CSRF는 사용하지 않는다.
- `org.springframework.security.web.csrf.*`, `HttpSecurity#csrf`, `_csrf` 표준 request attribute, `CsrfTokenRepository`는 이번 범위가 아니다.
- 현재 프로젝트는 `AuthInterceptor`, `AdminSession`, `WebMvcConfig` 기반의 자체 관리자 세션 구조를 사용한다.
- CSRF 값은 `AdminSession` VO 필드가 아니라 `HttpSession` attribute로 유지한다.
- 검증 대상은 인증된 관리자 세션의 `/admin/**` POST 요청이다.
- GET, HEAD, OPTIONS 등 안전 메서드는 검증하지 않는다.
- 검증 성공 시에만 토큰을 회전하고 `X-CSRF-Token`, `X-CSRF-FormId` 응답 헤더로 새 값을 내려준다.
- 검증 실패 시 일반 요청은 403, Ajax/API 요청은 JSON 오류 응답을 반환한다.

## 2. 대상 파일 후보

| 구분 | 파일 | 판단 | 작업 내용 |
| --- | --- | --- | --- |
| 필수 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/CsrfInterceptor.java` | 신규 후보 | `/admin/**` POST CSRF 검증, 성공 시 회전, 실패 응답 처리 |
| 필수 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/config/WebMvcConfig.java` | 수정 후보 | 인증 인터셉터 뒤에 CSRF 인터셉터 등록 |
| 권고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/CsrfTokenIssuer.java` | 재사용/보강 후보 | Phase1A-E 발급 로직 재사용, 상수와 재발급 책임 집중 |
| 참고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AuthInterceptor.java` | 참고만 | 인증 실패 응답 형식과 `/admin/api/` 판정 방식 확인 |
| 참고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java` | 참고만 | 세션 VO 구조 확인. 이번 단계에서도 수정하지 않음 |
| 참고 | `3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js` | 참고만 | Ajax POST 파라미터명, 응답 헤더 반영 계약 확인 |
| 참고 | `3.개발/cleverchat/src/main/resources/templates/admmgr/common/head.html` | 참고만 | Phase1A-D meta 주입 결과 확인. 이번 단계에서는 수정하지 않음 |

## 3. Phase1A-E 세션 Attribute 규칙 인용

Phase1A-F는 Phase1A-E의 attribute 규칙을 그대로 사용한다. 이름 변경, 별칭 추가, Spring Security 표준명 혼용은 금지한다.

Phase1A-E 원문 규칙:

```text
세션 attribute명은 다음 2개로 고정한다.

| attribute명 | 값 | 용도 |
| --- | --- | --- |
| `csrfToken` | CSRF 토큰 본문 | Ajax POST 요청의 `csrfToken` 파라미터와 비교 |
| `csrfFormId` | CSRF form 식별자 | Ajax POST 요청의 `csrfFormId` 파라미터와 비교 |
```

상수 선언 규칙도 Phase1A-E와 동일하게 유지한다.

```java
public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
public static final String CSRF_FORM_ID_SESSION_ATTRIBUTE = "csrfFormId";
```

## 4. 클라이언트 계약

현재 `ADM.Common.js`는 Ajax POST에서 다음 계약을 사용한다.

| 항목 | 계약 |
| --- | --- |
| meta token 이름 | `_csrf_token` |
| meta form id 이름 | `_csrf_form_id` |
| POST 파라미터 token | `csrfToken` |
| POST 파라미터 form id | `csrfFormId` |
| token 응답 헤더 | `X-CSRF-Token` |
| form id 응답 헤더 | `X-CSRF-FormId` |
| Ajax 판정 헤더 | `X-Requested-With: XMLHttpRequest` |

Phase1A-F 구현은 클라이언트 변경 없이 위 계약을 받아야 한다.

## 5. 검증 대상 범위

검증 대상:

- `/admin/**` 경로의 POST 요청
- `AdminSession.SESSION_KEY`가 존재하는 인증된 관리자 세션
- Ajax POST와 일반 form POST 모두 포함

검증 제외:

- GET, HEAD, OPTIONS
- `/login`, `/logout` 등 `/admin/**` 밖의 자체 인증 경로
- 정적 리소스 경로
- 인증 실패 요청. 인증 실패 처리는 기존 `AuthInterceptor`가 먼저 담당한다.

인터셉터 등록 순서:

1. `AuthInterceptor`
2. `CsrfInterceptor`

인증 인터셉터가 먼저 관리자 세션 존재 여부를 판단하고, CSRF 인터셉터는 인증된 세션의 POST 요청만 검증한다.

## 6. 검증 흐름

`CsrfInterceptor#preHandle` 권고 흐름:

1. 요청 경로와 HTTP 메서드를 확인한다.
2. POST가 아니면 통과한다.
3. `request.getSession(false)`로 기존 세션만 조회한다.
4. 세션이 없거나 `AdminSession.SESSION_KEY`가 없으면 기존 인증 인터셉터 순서 문제로 보고 403 또는 JSON 오류를 반환한다.
5. 세션 attribute `csrfToken`, `csrfFormId`를 조회한다.
6. 요청 파라미터 `csrfToken`, `csrfFormId`를 조회한다.
7. 네 값 중 하나라도 null 또는 빈 문자열이면 실패 처리한다.
8. 세션 값과 요청 값을 상수 시간 비교로 검증한다.
9. 둘 중 하나라도 불일치하면 실패 처리한다.
10. 검증 성공 시 새 `csrfToken`, `csrfFormId`를 발급해 같은 세션에 저장한다.
11. 새 값을 `X-CSRF-Token`, `X-CSRF-FormId` 응답 헤더로 내려준다.
12. 컨트롤러로 통과시킨다.

상수 시간 비교:

- `String#equals` 직접 비교 대신 `MessageDigest.isEqual(byte[], byte[])` 사용을 권고한다.
- 비교 전 null, 빈 값, 길이 상한을 먼저 방어한다.
- UTF-8 바이트 배열로 변환해 비교한다.
- 토큰 값을 로그, 예외 메시지, 화면 메시지에 남기지 않는다.

## 7. Ajax 요청 판정과 실패 응답

Ajax/API 요청 판정 후보:

- `X-Requested-With` 헤더가 `XMLHttpRequest`
- 요청 경로가 `/admin/api/`로 시작
- `Accept` 헤더에 `application/json` 포함

권고는 위 조건 중 하나라도 만족하면 JSON 응답으로 처리하는 것이다. 기존 `AuthInterceptor`의 `/admin/api/` JSON 응답 관행과 맞춘다.

일반 요청 실패 응답:

- HTTP status: `403`
- redirect 하지 않는다.
- 토큰 값을 본문에 포함하지 않는다.

Ajax/API 실패 응답:

```json
{"success":false,"error":{"code":"CSRF_INVALID","message":"잘못된 접근입니다."}}
```

응답 속성:

- HTTP status: `403`
- `Content-Type: application/json`
- `response.setCharacterEncoding("UTF-8")`를 호출한다.
- 실패 응답에는 `X-CSRF-Token`, `X-CSRF-FormId`를 재발급하지 않는다.

## 8. 회전 정책

회전 원칙:

- 검증 성공 시에만 회전한다.
- 실패 요청에서는 회전하지 않는다.
- 회전은 세션 attribute 저장과 응답 헤더 쓰기를 한 흐름에서 수행한다.
- 새 token과 새 form id는 모두 발급한다. 하나만 회전하지 않는다.
- 컨트롤러 예외 여부와 무관하게 `preHandle`에서 검증 성공 후 즉시 회전하는 방식을 우선 검토한다.

재발급 헤더:

| 헤더 | 값 |
| --- | --- |
| `X-CSRF-Token` | 새 `csrfToken` |
| `X-CSRF-FormId` | 새 `csrfFormId` |

주의:

- 같은 토큰을 재사용해 헤더만 내려주면 안 된다.
- 실패 요청에서 새 토큰을 내려주면 공격자가 토큰을 갱신할 수 있으므로 금지한다.
- 다중 탭/동시 Ajax 요청은 이전 토큰으로 보낸 후속 요청이 실패할 수 있다. 이 정책은 보안 우선 정책으로 문서화하고, 필요 시 후속 단계에서 이전 토큰 1개 허용 같은 완화책을 별도 검토한다.

## 9. 구현 지시

`CsrfInterceptor` 책임:

- POST 여부 판단
- Ajax/API 요청 여부 판단
- 세션 attribute와 요청 파라미터 추출
- 상수 시간 비교
- 검증 실패 응답 작성
- 검증 성공 시 `CsrfTokenIssuer`를 통해 재발급
- 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId` 설정

`CsrfTokenIssuer` 책임:

- Phase1A-E에서 정의한 attribute명 상수 유지
- `SecureRandom` 기반 token/form id 생성
- `issue(HttpSession session)` 또는 동등 메서드로 두 값을 함께 저장
- 필요 시 새 값을 반환할 수 있는 DTO 또는 record 제공

`WebMvcConfig` 책임:

- `CsrfInterceptor`를 생성자 주입으로 받는다.
- `AuthInterceptor` 등록 뒤에 `CsrfInterceptor`를 `/admin/**`에 등록한다.
- path exclude를 추가해야 한다면 `/admin/**` 안에서 실제 제외가 필요한 경로만 최소화한다.

## 10. 금지 범위

이번 Phase1A-F 작업에서 다음 변경은 금지한다.

- Spring Security CSRF 활성화
- `SecurityFilterChain`, `HttpSecurity#csrf`, `CsrfTokenRepository` 추가
- `AdminSession` 필드 추가 또는 생성자 시그니처 변경
- `LoginController` 인증 정책 변경
- `AuthInterceptor` 인증 판정 정책 변경
- `ADM.Common.js` 파라미터명 또는 헤더명 변경
- `head.html` meta 태그명 변경
- GET 요청 CSRF 검증 추가
- 실패 요청에서 CSRF 토큰 재발급
- 토큰 값을 로그, audit log, redirect URL, 예외 메시지, 화면 메시지에 출력
- DB schema, mapper, entity 변경
- 관리자 메뉴/화면 레이아웃 변경

## 11. 검증 명령

명령은 프로젝트 루트 `C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT` 또는 WSL 경로 `/mnt/c/02.Project/02.자바/01.WorkSpace/CLEVERCHAT`에서 실행한다.

### 11.1 CSRF 인터셉터 생성 확인

```bash
rg -n 'class CsrfInterceptor|implements HandlerInterceptor|preHandle' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- `CsrfInterceptor`가 `HandlerInterceptor`로 구현되어 있다.
- `preHandle`에서 POST 검증 흐름이 확인된다.

### 11.2 인터셉터 등록 순서 확인

```bash
sed -n '1,120p' '3.개발/cleverchat/src/main/java/kr/co/cleverchat/config/WebMvcConfig.java'
```

기대 결과:

- `authInterceptor` 등록 뒤에 `csrfInterceptor`가 등록된다.
- 두 인터셉터 모두 `/admin/**`를 대상으로 한다.

### 11.3 Phase1A-E attribute명 유지 확인

```bash
rg -n 'csrfToken|csrfFormId|CSRF_TOKEN_SESSION_ATTRIBUTE|CSRF_FORM_ID_SESSION_ATTRIBUTE' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- 세션 attribute명은 `csrfToken`, `csrfFormId`다.
- 요청 파라미터명도 `csrfToken`, `csrfFormId`다.

### 11.4 응답 헤더 재발급 확인

```bash
rg -n 'X-CSRF-Token|X-CSRF-FormId|setHeader' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- 검증 성공 흐름에서 두 응답 헤더를 모두 설정한다.

### 11.5 실패 응답 확인

```bash
rg -n 'CSRF_INVALID|SC_FORBIDDEN|403|application/json|X-Requested-With|XMLHttpRequest' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- 실패 status는 403이다.
- Ajax/API 요청은 JSON 오류 응답을 반환한다.

### 11.6 상수 시간 비교 확인

```bash
rg -n 'MessageDigest\\.isEqual|getBytes\\(StandardCharsets\\.UTF_8\\)|equals\\(' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- CSRF 토큰 비교에는 `MessageDigest.isEqual` 또는 동등한 상수 시간 비교가 사용된다.
- CSRF 토큰 본문 비교에 `String#equals`만 단독 사용하지 않는다.

### 11.7 Spring Security CSRF 미사용 확인

```bash
rg -n 'CsrfToken|CsrfTokenRepository|CookieCsrfTokenRepository|HttpSessionCsrfTokenRepository|csrf\\(' \
  '3.개발/cleverchat/src/main/java' \
  '3.개발/cleverchat/src/main/resources'
```

기대 결과:

- Spring Security CSRF 구성 또는 repository 추가 결과 없음.
- 자체 `CsrfInterceptor`, `CsrfTokenIssuer` 명칭이 검색되는 것은 허용한다.

### 11.8 클라이언트/템플릿 무변경 확인

```bash
git diff -- \
  '3.개발/cleverchat/src/main/resources/templates/admmgr/common/head.html' \
  '3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js'
```

기대 결과:

- diff 없음.

### 11.9 세션 VO 무변경 확인

```bash
git diff -- '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java'
```

기대 결과:

- diff 없음.

### 11.10 로그 노출 차단

```bash
rg -n 'csrfToken|csrfFormId|CSRF_TOKEN|CSRF_FORM|X-CSRF' \
  '3.개발/cleverchat/src/main/java' \
  '3.개발/cleverchat/src/main/resources' \
  | rg -n 'log\\.|System\\.out|printStackTrace|redirect:|message|error'
```

기대 결과:

- 토큰 값이 로그, 화면 메시지, redirect URL에 출력되는 코드 없음.
- `CSRF_INVALID` 같은 고정 오류 코드는 허용한다.

### 11.11 컴파일 검증

```bash
cd '3.개발/cleverchat' && ./mvnw test-compile
```

기대 결과:

- 컴파일 성공.
- `AdminSession` 생성자 변경 등 파급 수정 없음.

## 12. 단위 테스트 후보

가능하면 다음 테스트를 추가한다.

- POST가 아닌 요청은 CSRF 검증 없이 통과한다.
- 세션 token/form id와 요청 파라미터가 모두 일치하면 통과한다.
- 검증 성공 시 세션의 `csrfToken`, `csrfFormId`가 새 값으로 바뀐다.
- 검증 성공 시 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId`가 내려간다.
- token 누락 시 403을 반환한다.
- form id 누락 시 403을 반환한다.
- token 불일치 시 403을 반환한다.
- form id 불일치 시 403을 반환한다.
- Ajax 요청 실패 시 JSON 오류 본문을 반환한다.
- 실패 요청에서는 세션 token/form id를 회전하지 않는다.

## 13. 스모크 시나리오

### 13.1 정상 Ajax POST

1. 정상 관리자 계정으로 로그인한다.
2. 관리자 화면에서 `_csrf_token`, `_csrf_form_id` meta content가 존재하는지 확인한다.
3. `ADM.ajaxPost`를 사용하는 관리자 API를 호출한다.
4. HTTP status가 200 계열인지 확인한다.
5. 응답 헤더에 `X-CSRF-Token`, `X-CSRF-FormId`가 모두 있는지 확인한다.
6. Ajax 완료 후 meta content가 응답 헤더 값으로 갱신되는지 확인한다.

### 13.2 위조 token Ajax POST

1. 정상 로그인 상태에서 Ajax POST 파라미터 `csrfToken`을 임의 값으로 바꿔 요청한다.
2. HTTP status가 403인지 확인한다.
3. JSON 본문 code가 `CSRF_INVALID`인지 확인한다.
4. 응답 헤더에 새 CSRF 값이 내려오지 않는지 확인한다.

### 13.3 위조 form id Ajax POST

1. 정상 로그인 상태에서 Ajax POST 파라미터 `csrfFormId`를 임의 값으로 바꿔 요청한다.
2. HTTP status가 403인지 확인한다.
3. JSON 본문 code가 `CSRF_INVALID`인지 확인한다.
4. 세션 token/form id가 회전하지 않았는지 확인한다.

### 13.4 누락 파라미터

1. `csrfToken` 또는 `csrfFormId`를 제거한 POST 요청을 보낸다.
2. HTTP status가 403인지 확인한다.
3. 실패 응답에 토큰 값이 포함되지 않는지 확인한다.

### 13.5 일반 form POST 실패

1. Ajax 헤더 없이 `/admin/**` POST를 보낸다.
2. CSRF 값을 누락하거나 위조한다.
3. HTTP status가 403인지 확인한다.
4. 로그인 페이지로 redirect되지 않는지 확인한다.

### 13.6 GET 요청 통과

1. 관리자 화면 GET 요청을 보낸다.
2. CSRF 파라미터 없이도 화면이 정상 렌더링되는지 확인한다.

### 13.7 비로그인 Ajax POST

1. 세션 없이 `/admin/api/**` POST를 보낸다.
2. 기존 인증 인터셉터가 401 JSON을 반환하는지 확인한다.
3. CSRF 인터셉터가 인증 실패 응답을 덮어쓰지 않는지 확인한다.

### 13.8 재로그인 후 이전 token 실패

1. 로그인 후 token/form id를 기록한다.
2. 로그아웃 후 다시 로그인한다.
3. 이전 token/form id로 POST를 보낸다.
4. HTTP status가 403인지 확인한다.
5. 새 token/form id로 POST하면 성공하는지 확인한다.

## 14. 보안 체크리스트

| 항목 | 상태 | 비고 |
| --- | --- | --- |
| Spring Security CSRF 미사용 | [ ] 대기 | 자체 인터셉터 구조 유지 |
| `/admin/**` POST 검증 | [ ] 대기 | 안전 메서드 제외 |
| `csrfToken` 검증 | [ ] 대기 | Phase1A-E attribute명 유지 |
| `csrfFormId` 검증 | [ ] 대기 | Phase1A-E attribute명 유지 |
| 상수 시간 비교 | [ ] 대기 | `MessageDigest.isEqual` 권고 |
| 성공 시 회전 | [ ] 대기 | 세션 저장 및 응답 헤더 |
| 실패 시 미회전 | [ ] 대기 | 공격자 갱신 방지 |
| Ajax JSON 오류 | [ ] 대기 | 403 + `CSRF_INVALID` |
| 토큰 로그 노출 없음 | [ ] 대기 | 로그/예외/redirect 금지 |
| `AdminSession` VO 무변경 | [ ] 대기 | 세션 VO 비대화 방지 |

## 15. PR 범위 제한

이번 PR은 다음 범위로 제한한다.

- `CsrfInterceptor` 추가
- `WebMvcConfig` 인터셉터 등록 추가
- 필요 시 `CsrfTokenIssuer` 재사용성 보강
- CSRF 검증 및 회전 단위 테스트 추가

다음 작업은 별도 PR로 분리한다.

- Spring Security 기반 CSRF 전환
- 관리자 화면 form hidden input 일괄 삽입
- `ADM.Common.js` 동시 요청 완화 로직 추가
- 로그인/인증 정책 변경
- 세션 VO 구조 변경
- 화면 레이아웃 또는 메뉴 변경

## 16. 커밋 후보

권고 커밋:

```text
Phase1A-F: feat(auth): add admin csrf validation interceptor
Phase1A-F: feat(auth): rotate csrf headers after valid post
Phase1A-F: test(auth): cover csrf interceptor validation flow
```

단일 커밋으로 묶는 경우:

```text
Phase1A-F: feat(auth): validate and rotate admin csrf tokens
```

## 17. 산출물 체크리스트

| 항목 | 상태 | 비고 |
| --- | --- | --- |
| `CsrfInterceptor` 추가 | [ ] 대기 | POST 검증 |
| `WebMvcConfig` 등록 | [ ] 대기 | 인증 뒤 CSRF 순서 |
| 성공 시 `X-CSRF-Token` 재발급 | [ ] 대기 | Ajax meta 갱신 |
| 성공 시 `X-CSRF-FormId` 재발급 | [ ] 대기 | Ajax meta 갱신 |
| 실패 시 403 처리 | [ ] 대기 | 일반 요청 |
| 실패 시 JSON 오류 처리 | [ ] 대기 | Ajax/API 요청 |
| 실패 시 미회전 | [ ] 대기 | 보안 필수 |
| 클라이언트 무변경 | [ ] 대기 | 기존 계약 유지 |
| `./mvnw test-compile` 통과 | [ ] 대기 | 구현 PR에서 수행 |

## 18. 롤백 절차

문제 발생 시 다음 순서로 롤백한다.

1. `WebMvcConfig`에서 `CsrfInterceptor` 등록을 제거한다.
2. `CsrfInterceptor` 클래스와 관련 테스트를 제거한다.
3. `CsrfTokenIssuer`를 보강했다면 Phase1A-E 최초 발급에 필요한 부분만 남긴다.
4. `ADM.Common.js`, `head.html`, `AdminSession`, `AuthInterceptor`에 변경이 섞여 있으면 본 범위 위반이므로 별도 diff로 분리한다.
5. `cd '3.개발/cleverchat' && ./mvnw test-compile`을 실행한다.

## 19. Phase1A-D/E/F 관계표

| 단계 | 역할 | Phase1A-F와의 관계 |
| --- | --- | --- |
| Phase1A-D | head meta 주입 | `session.csrfToken`, `session.csrfFormId`를 meta로 렌더링 |
| Phase1A-E | 세션 CSRF 발급 | 로그인 성공 시 `csrfToken`, `csrfFormId` 최초 생성 |
| Phase1A-F | CSRF 검증 인터셉터 | 본 문서. POST 검증, 성공 시 회전, 응답 헤더 재발급 |

작업 순서 권고:

```text
Phase1A-E(세션 CSRF 발급) -> Phase1A-D(head meta 주입) -> Phase1A-F(CSRF 검증 인터셉터)
```

Phase1A-F 적용 전제:

- 로그인 성공 후 세션에 `csrfToken`, `csrfFormId`가 존재한다.
- 관리자 화면 head에 `_csrf_token`, `_csrf_form_id` meta가 렌더링된다.
- `ADM.Common.js`가 Ajax POST에 `csrfToken`, `csrfFormId` 파라미터를 자동 첨부한다.

## 20. 참고

현재 확인된 자체 인증 관련 파일:

- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AuthInterceptor.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/config/WebMvcConfig.java`

현재 확인된 클라이언트 계약:

- `ADM.Common.js`는 `_csrf_token`, `_csrf_form_id` meta를 읽는다.
- Ajax POST 파라미터명은 `csrfToken`, `csrfFormId`다.
- Ajax POST 요청은 `X-Requested-With: XMLHttpRequest`를 설정한다.
- Ajax 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId`가 있으면 meta content를 갱신한다.
