# Phase1A-D CSRF Meta Injection 작업지시

## 1. 목적과 핵심 판단

관리자 공통 head fragment(`head.html`)에 자체 CSRF 메타 태그를 주입해, 기존 관리자 공통 JavaScript(`ADM.Common.js`)가 Ajax POST 요청 시 세션 기반 CSRF 값을 읽을 수 있도록 한다.

이번 작업은 Spring Security CSRF 연동이 아니다. 프로젝트 정책상 관리자 인증/인가, 세션, CSRF는 Spring Security가 아니라 HandlerInterceptor와 세션 VO 기반 자체 구현을 기준으로 한다. 따라서 메타 태그 이름, 응답 헤더명, 요청 파라미터명은 현재 `ADM.Common.js`에 이미 고정된 계약을 따른다.

핵심 판단:

- 변경 대상은 `src/main/resources/templates/admmgr/common/head.html` 단일 파일이다.
- 삽입 대상은 자체 CSRF용 메타 태그 2개뿐이다.
- 클라이언트 셀렉터와 일치시키기 위해 `name` 값은 `_csrf_token`, `_csrf_form_id`를 그대로 사용한다.
- Spring Security 표준 메타명인 `_csrf`, `_csrf_header`는 사용하지 않는다.
- 공통 JS, 인증/인가 로직, 인터셉터, 컨트롤러, 템플릿 구조는 이번 단계에서 수정하지 않는다.

## 2. 후보 태그 명세

### M1. CSRF Token

```html
<meta name="_csrf_token" th:content="${session.csrfToken}">
```

용도:

- `ADM.getCsrfParam()`이 `meta[name="_csrf_token"]`으로 조회한다.
- Ajax POST 요청 파라미터 `csrfToken` 값으로 직렬화된다.
- Ajax 응답 후 `ADM.updateCsrfMeta()`가 `X-CSRF-Token` 응답 헤더 값으로 갱신한다.

### M2. CSRF Form ID

```html
<meta name="_csrf_form_id" th:content="${session.csrfFormId}">
```

용도:

- `ADM.getCsrfParam()`이 `meta[name="_csrf_form_id"]`로 조회한다.
- Ajax POST 요청 파라미터 `csrfFormId` 값으로 직렬화된다.
- Ajax 응답 후 `ADM.updateCsrfMeta()`가 `X-CSRF-FormId` 응답 헤더 값으로 갱신한다.

### 배제 후보

다음 후보는 이번 작업에서 사용하지 않는다.

```html
<meta name="_csrf" ...>
<meta name="_csrf_header" ...>
<meta name="csrf-token" ...>
<meta name="csrf-form-id" ...>
```

배제 사유:

- 현재 `ADM.Common.js`의 DOM 셀렉터와 일치하지 않는다.
- Spring Security CSRF 계약으로 오해될 수 있다.
- 이름 변경 시 클라이언트 Ajax 공통 함수까지 함께 수정해야 하므로 Phase1A-D 범위를 초과한다.

## 3. 값 주입 전략

> ⚠️ **선행 의존성 경고**
>
> 본 §3의 메타 태그 주입 작업(`head.html`에 `_csrf_token`, `_csrf_form_id` 추가)은 단독으로 수행하면 런타임에서 빈 값 또는 null이 렌더링되어 CSRF 보호가 동작하지 않는다. 다음 두 선행 작업이 반드시 먼저 완료되어 있어야 한다.
>
> 1. **세션 CSRF 발급 작업** — `Phase1A_E_세션CSRF발급_작업지시.md`
>    - 로그인 또는 관리자 진입 시 세션 attribute로 `csrfToken`, `csrfFormId` 값을 생성·저장하는 서버 측 구현이 선행되어야 한다.
>    - 세션 attribute명이 본 문서의 `${session.csrfToken}`, `${session.csrfFormId}` 표현식과 정확히 일치해야 한다.
>    - 이 작업이 누락되면 `head.html` 메타 태그의 `th:content`가 null로 렌더링된다.
>
> 2. **CSRF 검증 인터셉터 작업** — `Phase1A_F_CSRF검증인터셉터_작업지시.md`
>    - Ajax POST 요청 시 `csrfToken`, `csrfFormId` 파라미터를 검증하고, 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId`를 재발급해 내려주는 HandlerInterceptor 구현이 선행되어야 한다.
>    - 이 작업이 누락되면 메타 태그가 존재하더라도 서버 측에서 CSRF 토큰을 검증·갱신할 주체가 없어 보호가 성립하지 않는다.
>
> **작업 순서 강제**:
>
> - Phase1A-E(세션 CSRF 발급) → Phase1A-F(CSRF 검증 인터셉터) → Phase1A-D(메타 주입) 순서로 진행한다.
> - 본 Phase1A-D를 먼저 머지하면 보안적으로는 무해하지만, 클라이언트 측 Ajax 동작이 불완전한 상태가 되므로 E·F 작업이 직후 머지 큐에 대기 중일 때에만 단독 진행을 허용한다.
> - E·F 작업의 세션 attribute명이 변경되면 본 문서 §2·§3의 `th:content` 표현식도 함께 갱신해야 한다.

### A안: 세션 표현식 주입 권고

`head.html`에 다음 2줄을 추가한다.

```html
<meta name="_csrf_token" th:content="${session.csrfToken}">
<meta name="_csrf_form_id" th:content="${session.csrfFormId}">
```

권고 사유:

- 자체 CSRF가 세션 기반으로 운영되는 정책과 맞다.
- 템플릿 fragment가 렌더링되는 시점에 세션 값을 그대로 노출할 수 있다.
- `ADM.Common.js`의 기존 계약과 추가 JS 수정 없이 연결된다.

주의:

- 실제 세션 attribute명이 `csrfToken`, `csrfFormId`와 다르면 서버 측 CSRF 구현의 attribute명을 먼저 확인한 뒤 `th:content` 표현식만 맞춘다.
- attribute명 확인 작업은 문서/검증 범위에 포함하되, 이번 Phase1A-D 작업 자체는 `head.html` 메타 주입만 수행한다.

### B안: 빈 자리 선삽입

서버 측 CSRF 세션 attribute명이 아직 확정되지 않은 경우에만 임시로 다음 형태를 사용할 수 있다.

```html
<meta name="_csrf_token" content="">
<meta name="_csrf_form_id" content="">
```

단, B안은 런타임 CSRF 보호를 완성하지 못한다. 후속 단계에서 반드시 `th:content` 기반 실제 값 주입으로 전환해야 한다.

## 4. 변경 대상 파일

대상 파일:

- `src/main/resources/templates/admmgr/common/head.html`

예상 변경:

- `<meta name="ctx" th:content="@{/}">` 아래에 CSRF 메타 태그 2줄 추가
- 기존 `<title>`, CSS link, JS script 순서는 유지
- fragment 선언부와 html namespace는 변경하지 않음

권장 삽입 위치:

```html
<meta name="ctx" th:content="@{/}">
<meta name="_csrf_token" th:content="${session.csrfToken}">
<meta name="_csrf_form_id" th:content="${session.csrfFormId}">
<title th:text="${pageTitle}">CleverChat</title>
```

## 5. 작업 절차

1. `src/main/resources/templates/admmgr/common/head.html`을 연다.
2. 현재 `<head th:fragment="adminHead(pageTitle)">` 내부의 메타 태그 구성을 확인한다.
3. `<meta name="ctx" th:content="@{/}">` 바로 아래에 자체 CSRF 메타 태그 2줄을 추가한다.
4. 태그 이름이 `_csrf_token`, `_csrf_form_id`와 정확히 일치하는지 확인한다.
5. 다른 템플릿, JavaScript, Java 코드는 수정하지 않는다.
6. 아래 검증 명령을 실행해 범위와 계약 일치를 확인한다.

## 6. 검증 명령

### 6.1 메타 태그 존재 확인

```bash
rg -n 'meta name="_csrf_(token|form_id)"' src/main/resources/templates/admmgr/common/head.html
```

기대 결과:

- `_csrf_token` 1건
- `_csrf_form_id` 1건

### 6.2 Spring Security 스타일 메타 오삽입 차단

```bash
rg -n 'meta name="(_csrf|_csrf_header|csrf-token|csrf-form-id)"' src/main/resources/templates/admmgr/common/head.html
```

기대 결과:

- 결과 없음

### 6.3 클라이언트 셀렉터 일치 확인

```bash
rg -n '_csrf_token|_csrf_form_id|X-CSRF-Token|X-CSRF-FormId|csrfToken|csrfFormId' src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js
```

기대 결과:

- `meta[name="_csrf_token"]`
- `meta[name="_csrf_form_id"]`
- `X-CSRF-Token`
- `X-CSRF-FormId`
- `csrfToken`
- `csrfFormId`

### 6.4 head fragment 무결성 확인

```bash
rg -n 'head th:fragment="adminHead\(pageTitle\)"|</head>|</html>' src/main/resources/templates/admmgr/common/head.html
```

기대 결과:

- `head` fragment 선언 유지
- `</head>`, `</html>` 유지

### 6.5 빌드 검증

```bash
./mvnw test-compile
```

기대 결과:

- Thymeleaf 템플릿 파일 추가 수정으로 인한 컴파일 영향 없음
- 기존 코드 컴파일 성공

### 6.6 런타임 스모크 검증

관리자 화면 렌더링 후 브라우저 개발자 도구에서 다음을 확인한다.

```javascript
document.querySelector('meta[name="_csrf_token"]')?.content
document.querySelector('meta[name="_csrf_form_id"]')?.content
```

기대 결과:

- 두 메타 태그가 존재한다.
- 자체 CSRF 세션 값이 준비된 상태라면 content가 비어 있지 않다.
- Ajax POST 성공 후 응답 헤더 `X-CSRF-Token`, `X-CSRF-FormId`가 내려오면 메타 content가 갱신된다.

## 7. 금지 범위

이번 Phase1A-D 작업에서 다음 변경은 금지한다.

- Spring Security 의존성 추가
- `SecurityFilterChain`, `HttpSecurity`, `UserDetailsService` 기반 구성 추가
- `ADM.Common.js`의 CSRF 셀렉터, 파라미터명, 헤더명 변경
- 전역 Ajax 정책 재작성
- CSRF 인터셉터 신규 구현 또는 대규모 수정
- 관리자 템플릿 fragment 구조 변경
- 정적 자산 경로, CSS, 레이아웃 변경

## 8. PR 범위 제한

PR에는 다음 변경만 포함한다.

- `src/main/resources/templates/admmgr/common/head.html` 2줄 추가

PR 설명에는 다음 내용을 명시한다.

- 자체 CSRF 메타 태그를 관리자 공통 head fragment에 추가했다.
- 메타명은 기존 `ADM.Common.js` 계약인 `_csrf_token`, `_csrf_form_id`와 일치한다.
- Spring Security CSRF 메타 계약은 도입하지 않았다.
- 후속 검증으로 세션 attribute명과 런타임 content 값을 확인해야 한다.

## 9. 산출물 체크리스트

- [ ] `head.html`에 `_csrf_token` 메타 태그가 1개 추가되었다.
- [ ] `head.html`에 `_csrf_form_id` 메타 태그가 1개 추가되었다.
- [ ] 두 태그 모두 `meta[name="..."]` 형태로 작성되었다.
- [ ] `ADM.Common.js`의 셀렉터와 메타명이 일치한다.
- [ ] Spring Security 스타일 메타명이 추가되지 않았다.
- [ ] 변경 파일이 `head.html` 단일 파일인지 확인했다.
- [ ] `./mvnw test-compile` 또는 동등한 빌드 검증을 수행했다.
- [ ] 관리자 화면에서 메타 태그 렌더링을 확인했다.

## 10. 롤백 기준과 절차

롤백 기준:

- 관리자 화면 렌더링 중 Thymeleaf expression 오류가 발생한다.
- 세션 attribute명이 맞지 않아 런타임에서 content가 의도와 다르게 렌더링된다.
- 기존 관리자 화면 공통 head fragment 사용처에서 예기치 않은 렌더링 문제가 발생한다.

롤백 절차:

1. `src/main/resources/templates/admmgr/common/head.html`에서 추가한 CSRF 메타 태그 2줄을 제거한다.
2. `rg -n 'meta name="_csrf_(token|form_id)"' src/main/resources/templates/admmgr/common/head.html` 결과가 없는지 확인한다.
3. 관리자 화면이 기존 상태로 렌더링되는지 확인한다.

## 11. 1.A 단계 관계표

| 구분 | Phase1A-D 판단 |
| --- | --- |
| 인증/인가 기준 | Spring Security 미사용, 자체 세션/인터셉터 기준 |
| CSRF 노출 위치 | 관리자 공통 head fragment |
| 서버 값 출처 | 세션 attribute |
| 클라이언트 소비자 | `ADM.Common.js` |
| 요청 파라미터명 | `csrfToken`, `csrfFormId` |
| 응답 헤더명 | `X-CSRF-Token`, `X-CSRF-FormId` |
| 변경 범위 | `head.html` 2줄 추가 |
| 후속 확인 | 실제 세션 attribute명과 런타임 content 값 |
| 선행 작업 | `Phase1A_E_세션CSRF발급_작업지시.md`(세션 attribute `csrfToken`/`csrfFormId` 발급), `Phase1A_F_CSRF검증인터셉터_작업지시.md`(HandlerInterceptor 기반 CSRF 검증·헤더 재발급) |
| 후속 작업 | `head.html` 메타 주입 후 관리자 화면 렌더링 시 메타 태그 content 비어있지 않음 확인, `ADM.Common.js` Ajax POST 연동 검증(요청 파라미터 `csrfToken`/`csrfFormId` 송신, 응답 헤더 `X-CSRF-Token`/`X-CSRF-FormId` 수신·갱신) |

## 12. 참고

- `README.md`: Spring Security 기반 인증/인가를 사용하지 않고 HandlerInterceptor + 세션 VO 기반 구현을 기준으로 한다.
- `docs/Security_JPA_8소스_기준정리.md`: Spring Security 구성 재도입 금지, CSRF는 자체 폼 토큰 방식 또는 Spring Boot 3 환경에 맞춘 동등한 HandlerInterceptor 방식으로 구현한다.
- `src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js`: `_csrf_token`, `_csrf_form_id` 메타 태그를 읽고 Ajax POST 파라미터와 응답 헤더 갱신에 사용한다.
