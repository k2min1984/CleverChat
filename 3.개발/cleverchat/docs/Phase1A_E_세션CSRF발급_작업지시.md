# Phase1A-E 세션 CSRF 발급 작업지시

> 작성일: 2026-05-19  
> 작업 원칙: 이 문서는 작업지시서이다. 본 문서 작성 단계에서는 코드 수정, 테스트 코드 생성, 설정 변경을 수행하지 않는다.  
> 구현 전제: 관리자 인증/인가, 세션, CSRF는 Spring Security CSRF가 아니라 자체 `HandlerInterceptor`와 세션 VO 구조를 기준으로 한다.

## 1. 목적과 핵심 판단

로그인 성공 및 신규 관리자 세션 생성 시점에 CSRF 식별값을 발급해 `HttpSession` attribute로 저장한다. 후속 Phase1A-D의 head meta 주입과 Phase1A-F의 CSRF 검증 인터셉터가 동일한 세션 attribute를 읽어 자체 CSRF 보호 흐름을 구성할 수 있게 하는 것이 목적이다.

핵심 판단:

- Spring Security CSRF는 사용하지 않는다.
- `org.springframework.security.web.csrf.*`, `HttpSecurity#csrf`, `_csrf` 표준 request attribute, `CsrfTokenRepository`는 이번 범위가 아니다.
- 현재 프로젝트는 `AuthInterceptor`, `AdminSession`, `LoginController` 기반의 자체 관리자 세션 구조를 사용한다.
- CSRF 값은 `AdminSession` VO 필드로 넣지 않고, 별도 `HttpSession` attribute로 저장한다.
- 발급 시점은 로그인 검증 성공 후 기존 세션을 `invalidate()`하고 새 세션을 생성한 직후다.
- 검증, 응답 헤더 재발급, Ajax POST 차단은 후속 Phase1A-F 범위다.

## 2. 대상 파일 후보

| 구분 | 파일 | 판단 | 작업 내용 |
| --- | --- | --- | --- |
| 필수 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/LoginController.java` | 필수 | 로그인 성공 후 신규 세션에 CSRF attribute 2개 저장 |
| 권고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/CsrfTokenIssuer.java` | 권고 | 토큰 생성 책임 분리, 상수와 생성 규칙 집중 |
| 참고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AuthInterceptor.java` | 참고만 | 인증 인터셉터 구조 확인. 이번 단계에서는 수정하지 않음 |
| 참고 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java` | 참고만 | 세션 VO 구조 확인. 이번 단계에서는 수정하지 않음 |
| 참고 | `3.개발/cleverchat/src/main/resources/templates/admmgr/common/head.html` | 참고만 | Phase1A-D meta 주입 문서와 attribute명 계약 확인. 이번 단계에서는 수정하지 않음 |
| 참고 | `3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js` | 참고만 | 클라이언트 파라미터명 계약 확인. 이번 단계에서는 수정하지 않음 |

## 3. 세션 Attribute 명

세션 attribute명은 다음 2개로 고정한다.

| attribute명 | 값 | 용도 |
| --- | --- | --- |
| `csrfToken` | CSRF 토큰 본문 | Ajax POST 요청의 `csrfToken` 파라미터와 비교 |
| `csrfFormId` | CSRF form 식별자 | Ajax POST 요청의 `csrfFormId` 파라미터와 비교 |

명명 사유:

- Phase1A-D meta 주입 후보의 `${session.csrfToken}`, `${session.csrfFormId}`와 일치한다.
- 기존 `ADM.Common.js`가 Ajax POST 파라미터명으로 `csrfToken`, `csrfFormId`를 사용한다.
- Spring Security 표준명인 `_csrf`, `_csrf_header`와 의도적으로 구분된다.

상수 선언 규칙:

```java
public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
public static final String CSRF_FORM_ID_SESSION_ATTRIBUTE = "csrfFormId";
```

권고 위치:

- `CsrfTokenIssuer`를 분리하는 경우 해당 클래스에 선언한다.
- 분리하지 않는 경우 `LoginController` 내부 private 상수가 아니라, 후속 Phase1A-F에서 재사용 가능한 패키지 공개 상수로 정리한다.

## 4. 토큰 생성 규칙

생성 규칙:

| 항목 | 규칙 |
| --- | --- |
| 난수 생성기 | `java.security.SecureRandom` 정적 인스턴스 1개 사용 |
| `csrfToken` 엔트로피 | 256bit 이상. `byte[32]` 권고 |
| `csrfFormId` 엔트로피 | 128bit 이상. `byte[16]` 권고 |
| 인코딩 | `Base64.getUrlEncoder().withoutPadding()` |
| 문자 집합 | URL-safe Base64. `A-Z`, `a-z`, `0-9`, `_`, `-` |
| padding | 사용하지 않음 |
| 로그 출력 | 금지 |

예상 길이:

- `byte[32]` URL-safe Base64 without padding: 43자
- `byte[16]` URL-safe Base64 without padding: 22자

금지:

- `UUID.randomUUID()` 단독 사용
- `Random`, `Math.random()` 사용
- 현재 시각, 사용자 ID, 세션 ID를 조합한 토큰 생성
- 토큰 값을 audit log, debug log, 예외 메시지, redirect URL에 남기는 처리

주의: `CsrfTokenIssuer`를 분리하는 경우 정적 `SecureRandom`과 인코더 사용이 동시 요청 환경에서 thread-safe하게 동작하도록 상태 공유를 최소화한다.

## 5. 발급 및 갱신 시점

### 5.1 로그인 성공 시 최초 발급

`LoginController#authenticate`에서 다음 순서를 지킨다.

1. 사용자 존재, 활성 상태, 잠금 상태, 비밀번호 검증을 모두 통과한다.
2. 로그인 성공 audit을 기록한다.
3. 기존 세션이 있으면 `existingSession.invalidate()`를 호출한다.
4. `request.getSession(true)`로 새 세션을 생성한다.
5. 새 세션에 `AdminSession.SESSION_KEY`를 저장한다.
6. 같은 새 세션에 `csrfToken`, `csrfFormId`를 저장한다.
7. `/admin`으로 redirect한다.

주의:

- 기존 세션에 CSRF 값을 저장하면 안 된다.
- `invalidate()` 이전에 만든 값을 기존 세션에 남기면 안 된다.
- 신규 세션 생성 후 `AdminSession`과 CSRF attribute가 같은 세션에 저장되어야 한다.

### 5.2 재로그인 시 회전

재로그인 또는 다른 계정 로그인 성공 시 기존 세션을 폐기하고 새 세션을 만들기 때문에 `csrfToken`, `csrfFormId`도 새 값으로 회전되어야 한다.

### 5.3 요청별 회전 제외

매 POST 성공 후 CSRF 값을 재발급하고 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId`로 내려주는 처리는 Phase1A-F 범위다. 본 Phase1A-E에서는 로그인 성공 시 최초 발급만 구현한다.

## 6. 구현 지시

권고안은 `CsrfTokenIssuer` 분리다.

`CsrfTokenIssuer` 책임:

- `SecureRandom` 정적 인스턴스 보유
- `csrfToken` 생성
- `csrfFormId` 생성
- 세션 attribute명 상수 제공
- `issue(HttpSession session)` 같은 단일 메서드로 두 attribute를 함께 저장

`LoginController` 책임:

- 로그인 성공 후 새 세션 생성 흐름 유지
- `AdminSession.SESSION_KEY` 저장 직후 또는 직전에 CSRF 발급 호출
- 실패 로그인, 로그아웃 흐름은 변경하지 않음

비분리안:

- 변경량을 더 줄여야 하는 경우 `LoginController`에 private 생성 메서드를 둘 수 있다.
- 단, 후속 Phase1A-F 검증 인터셉터가 같은 attribute명과 생성 규칙을 재사용해야 하므로, 비분리안은 후속 작업에서 중복 제거가 필요하다.

## 7. 금지 범위

이번 Phase1A-E 작업에서 다음 변경은 금지한다.

- Spring Security CSRF 활성화
- `SecurityFilterChain`, `HttpSecurity#csrf`, `CsrfTokenRepository` 추가
- `AdminSession` 필드 추가 또는 생성자 시그니처 변경
- `AuthInterceptor` 인증 판정 로직 변경
- CSRF 검증 인터셉터 구현
- Ajax 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId` 발급 구현
- `head.html` meta 태그 수정
- `ADM.Common.js` 수정
- 로그인 화면 form hidden input 추가
- DB schema, mapper, entity 변경
- 토큰 값을 로그 또는 화면에 노출하는 디버그 코드 추가

## 8. 검증 명령

명령은 프로젝트 루트 `C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT` 또는 WSL 경로 `/mnt/c/02.Project/02.자바/01.WorkSpace/CLEVERCHAT`에서 실행한다.

### 8.1 세션 attribute 저장 확인

```bash
rg -n 'csrfToken|csrfFormId|CSRF_TOKEN_SESSION_ATTRIBUTE|CSRF_FORM_ID_SESSION_ATTRIBUTE|setAttribute' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- `csrfToken`, `csrfFormId` attribute명이 확인된다.
- 로그인 성공 후 새 `HttpSession`에 두 값이 `setAttribute`되는 흐름이 확인된다.

### 8.2 난수 생성 규칙 확인

```bash
rg -n 'SecureRandom|Base64\.getUrlEncoder\(\)\.withoutPadding\(\)|new byte\[(32|16)\]' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- `SecureRandom` 사용이 확인된다.
- URL-safe Base64 without padding 사용이 확인된다.
- 32바이트, 16바이트 토큰 원천 배열이 확인된다.

### 8.3 취약 난수 사용 차단

```bash
rg -n 'Math\.random|new Random|UUID\.randomUUID|currentTimeMillis|nanoTime' \
  '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security'
```

기대 결과:

- CSRF 토큰 생성 코드에서 결과 없음.
- 다른 기존 코드가 검색되는 경우 CSRF 발급 코드와 무관한지 별도 확인한다.

### 8.4 Spring Security CSRF 미사용 확인

```bash
rg -n 'CsrfToken|CsrfTokenRepository|CookieCsrfTokenRepository|HttpSessionCsrfTokenRepository|csrf\(' \
  '3.개발/cleverchat/src/main/java' \
  '3.개발/cleverchat/src/main/resources'
```

기대 결과:

- Spring Security CSRF 구성 또는 repository 추가 결과 없음.
- `PasswordEncoder` 등 Spring Security crypto 사용은 본 검증의 실패 사유가 아니다.

### 8.5 세션 VO 무변경 확인

```bash
git diff -- '3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java'
```

기대 결과:

- diff 없음.

### 8.6 클라이언트/템플릿 무변경 확인

```bash
git diff -- \
  '3.개발/cleverchat/src/main/resources/templates/admmgr/common/head.html' \
  '3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js'
```

기대 결과:

- diff 없음.

### 8.7 로그 노출 차단

```bash
rg -n 'csrfToken|csrfFormId|CSRF_TOKEN|CSRF_FORM' \
  '3.개발/cleverchat/src/main/java' \
  '3.개발/cleverchat/src/main/resources' \
  | rg -n 'log\.|System\.out|printStackTrace|redirect:|message|error'
```

기대 결과:

- 토큰 값을 로그, 화면 메시지, redirect URL에 출력하는 코드 없음.

### 8.8 컴파일 검증

```bash
cd '3.개발/cleverchat' && ./mvnw test-compile
```

기대 결과:

- 컴파일 성공.
- `AdminSession` 생성자 변경 등 파급 수정 없음.

### 8.9 단위 테스트 후보

가능하면 다음 테스트를 추가한다.

- `CsrfTokenIssuer`가 `csrfToken`, `csrfFormId`를 모두 세션에 저장한다.
- `csrfToken` 길이가 43자이고 URL-safe 문자만 포함한다.
- `csrfFormId` 길이가 22자이고 URL-safe 문자만 포함한다.
- 두 번 발급하면 두 attribute 값이 바뀐다.
- 토큰 생성기가 null 또는 빈 문자열을 저장하지 않는다.

단위 테스트 추가가 과하다고 판단되면 최소한 `./mvnw test-compile`과 런타임 스모크를 수행한다.

## 9. 스모크 시나리오

### 9.1 정상 로그인

1. `/login`으로 접속한다.
2. 정상 관리자 계정으로 로그인한다.
3. `/admin`으로 redirect되는지 확인한다.
4. 로그인 전후 `JSESSIONID`가 바뀌었는지 확인한다.
5. 서버 디버거 또는 임시 테스트 코드로 새 세션에 다음 attribute가 존재하는지 확인한다.
   - `csrfToken`
   - `csrfFormId`
6. `csrfToken`은 43자, `csrfFormId`는 22자인지 확인한다.
7. 두 값이 URL-safe Base64 문자 집합만 포함하는지 확인한다.

### 9.2 실패 로그인

1. 잘못된 비밀번호로 로그인한다.
2. `/login?error`로 redirect되는지 확인한다.
3. 새 관리자 세션이 생성되지 않는지 확인한다.
4. `csrfToken`, `csrfFormId`가 성공 세션처럼 발급되지 않는지 확인한다.

### 9.3 재로그인 회전

1. 정상 로그인 후 `csrfToken`, `csrfFormId` 값을 기록한다.
2. 로그아웃한다.
3. 다시 로그인한다.
4. 새 `JSESSIONID`가 발급되는지 확인한다.
5. 새 `csrfToken`, `csrfFormId`가 이전 값과 다른지 확인한다.

### 9.4 Phase1A-D 연동 확인

Phase1A-D가 적용된 환경에서는 관리자 화면 head meta가 다음 값을 세션에서 읽는지 확인한다.

```javascript
document.querySelector('meta[name="_csrf_token"]')?.content
document.querySelector('meta[name="_csrf_form_id"]')?.content
```

기대 결과:

- 로그인 직후 두 meta content가 비어 있지 않다.
- 값은 세션 attribute `csrfToken`, `csrfFormId`와 일치한다.

## 10. PR 범위 제한

이번 PR은 다음 범위로 제한한다.

- 로그인 성공 시 신규 세션에 CSRF attribute 최초 발급
- 필요 시 `CsrfTokenIssuer` 클래스 추가
- 해당 생성기 단위 테스트 추가

다음 작업은 별도 PR로 분리한다.

- Phase1A-D: head meta 주입
- Phase1A-F: CSRF 검증 인터셉터 및 응답 헤더 재발급
- 공통 Ajax 오류 처리 개선
- 관리자 화면 form hidden input 일괄 삽입

## 11. 커밋 후보

### 11.1 분리안 권고

```text
Phase1A-E: feat(auth): add session csrf token issuer
Phase1A-E: feat(auth): issue csrf attributes on admin login
Phase1A-E: test(auth): cover session csrf token issuer
```

### 11.2 비분리안

```text
Phase1A-E: feat(auth): issue csrf attributes on admin login
Phase1A-E: test(auth): cover login csrf session attributes
```

## 12. 산출물 체크리스트

| 항목 | 상태 | 비고 |
| --- | --- | --- |
| `csrfToken` 세션 attribute 발급 | [ ] 대기 | 로그인 성공 후 신규 세션 |
| `csrfFormId` 세션 attribute 발급 | [ ] 대기 | 로그인 성공 후 신규 세션 |
| `SecureRandom` 기반 생성 | [ ] 대기 | `Random`, `Math.random` 금지 |
| URL-safe Base64 without padding | [ ] 대기 | meta/Ajax 파라미터 안전성 |
| Spring Security CSRF 미사용 | [ ] 대기 | 자체 인터셉터 구조 유지 |
| `AdminSession` VO 무변경 | [ ] 대기 | 세션 VO 비대화 방지 |
| 토큰 로그 노출 없음 | [ ] 대기 | 보안 필수 |
| `./mvnw test-compile` 통과 | [ ] 대기 | 구현 PR에서 수행 |

## 13. 롤백 절차

문제 발생 시 다음 순서로 롤백한다.

1. `LoginController`의 CSRF 발급 호출을 제거한다.
2. `CsrfTokenIssuer`를 추가했다면 해당 클래스와 테스트를 제거한다.
3. `AdminSession`, `AuthInterceptor`, `head.html`, `ADM.Common.js`에 변경이 섞여 있으면 해당 변경은 본 범위 위반이므로 별도 diff로 분리한다.
4. `./mvnw test-compile`을 다시 실행한다.

## 14. 1.A 관계표

| 단계 | 역할 | 본 문서와의 관계 |
| --- | --- | --- |
| Phase1A-D | head meta 주입 | `session.csrfToken`, `session.csrfFormId`를 렌더링 |
| Phase1A-E | 세션 CSRF 발급 | 본 문서. 로그인 성공 시 세션 attribute 최초 생성 |
| Phase1A-F | CSRF 검증 인터셉터 | POST 요청 파라미터 검증 및 토큰 회전 |

작업 순서 권고:

```text
Phase1A-E(세션 CSRF 발급) -> Phase1A-F(CSRF 검증 인터셉터) -> Phase1A-D(head meta 주입)
```

Phase1A-D가 먼저 적용된 경우에도 본 Phase1A-E 구현이 완료되어야 meta content가 실제 값으로 채워진다.

운영 경고: Phase1A-D가 Phase1A-E보다 먼저 배포된 환경에서는 로그인 직후까지 meta content 공백 여부를 모니터링한다.

## 15. 참고

현재 확인된 자체 인증 관련 파일:

- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/LoginController.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AdminSession.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/AuthInterceptor.java`
- `3.개발/cleverchat/src/main/java/kr/co/cleverchat/config/WebMvcConfig.java`

현재 확인된 클라이언트 계약:

- `ADM.Common.js`는 `_csrf_token`, `_csrf_form_id` meta를 읽는다.
- Ajax POST 파라미터명은 `csrfToken`, `csrfFormId`다.
- 후속 응답 헤더명 후보는 `X-CSRF-Token`, `X-CSRF-FormId`다.
