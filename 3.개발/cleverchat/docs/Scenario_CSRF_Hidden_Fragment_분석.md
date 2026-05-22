# Scenario Native Form CSRF Hidden Fragment 공통화 분석

## 조사 범위

- 등록/수정 화면 템플릿: `src/main/resources/templates/admmgr/scenario/scenarioRegist.html`
- 상세 화면 템플릿: `src/main/resources/templates/admmgr/scenario/scenarioView.html`
- CSRF 검증 인터셉터: `src/main/java/kr/co/cleverchat/domain/auth/security/CsrfInterceptor.java`
- 공통 fragment 후보 위치: `src/main/resources/templates/admmgr/common/_csrfHidden.html`

## 결론

현재 `scenarioRegist.html`과 `scenarioView.html`의 native form에는 동일한 CSRF hidden input 2개가 5개 form에 반복되어 있다. 반복되는 값은 모두 세션의 `csrfToken`, `csrfFormId`를 같은 파라미터명으로 전송하므로 `admmgr/common/_csrfHidden.html` fragment로 공통화할 수 있다.

권장 형태는 wrapper element를 남기지 않는 `th:block` fragment다. 호출부도 form 내부에서 `<th:block th:replace="~{admmgr/common/_csrfHidden :: csrfHidden}"></th:block>` 형태로 치환하면 렌더링 결과는 기존과 동일한 hidden input 2개만 남는다. 따라서 DOM 구조, CSS selector, form submit payload에 대한 영향은 낮다.

## 현재 중복 현황

| 화면 | 위치 | form 목적 | 현재 중복 hidden | 공통화 가능 여부 |
|---|---:|---|---|---|
| `scenarioRegist.html` | L15-L17 | 시나리오 저장 | `csrfToken`, `csrfFormId` | 가능 |
| `scenarioView.html` | L23-L25 | 활성 시나리오 비활성화 | `csrfToken`, `csrfFormId` | 가능 |
| `scenarioView.html` | L32-L34 | 새 초안 생성 | `csrfToken`, `csrfFormId` | 가능 |
| `scenarioView.html` | L47-L50 | 초안 버전 게시 | `csrfToken`, `csrfFormId` + `scenarioId` | CSRF 2개만 가능 |
| `scenarioView.html` | L53-L56 | 게시 버전 활성화 | `csrfToken`, `csrfFormId` + `versionId` | CSRF 2개만 가능 |

중복 제거 후보는 총 5개 form, 10줄이다. 게시/활성화 form의 `scenarioId`, `versionId`는 각 업무 처리에 필요한 form 고유 payload이므로 CSRF fragment에 포함하지 않고 기존 form 본문에 유지해야 한다.

## Fragment 후보

권장 fragment 파일 경로:

```text
src/main/resources/templates/admmgr/common/_csrfHidden.html
```

권장 fragment 구조:

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<body>
<th:block th:fragment="csrfHidden">
    <input type="hidden" name="csrfToken" th:value="${session.csrfToken}">
    <input type="hidden" name="csrfFormId" th:value="${session.csrfFormId}">
</th:block>
</body>
</html>
```

호출부 후보:

```html
<th:block th:replace="~{admmgr/common/_csrfHidden :: csrfHidden}"></th:block>
```

`div`, `span` 같은 wrapper를 쓰지 않는 이유는 form 내부 DOM을 불필요하게 변경하지 않기 위해서다. `th:block`은 Thymeleaf 처리 후 실제 wrapper element를 출력하지 않으므로 현재 hidden input 2개만 렌더링되는 상태를 유지할 수 있다.

## th:replace 적용 위치

| 우선순위 | 파일 | 기존 위치 | 적용 위치 | 유의사항 |
|---|---|---:|---|---|
| P1 | `scenarioRegist.html` | L16-L17 | `form th:object` 바로 아래 | 단순 저장 form으로 영향 범위가 가장 작음 |
| P1 | `scenarioView.html` | L24-L25 | 비활성화 form 내부 첫 줄 | 조건부 form이지만 반복 컨텍스트 밖이라 단순함 |
| P1 | `scenarioView.html` | L33-L34 | 새 초안 form 내부 첫 줄 | 별도 hidden payload가 없는 단순 form |
| P2 | `scenarioView.html` | L48-L49 | 게시 form 내부, `scenarioId` hidden 앞 | `th:each` 내부 form이므로 form 내부 위치 유지 필요 |
| P2 | `scenarioView.html` | L54-L55 | 활성화 form 내부, `versionId` hidden 앞 | `th:each` 내부 form이므로 `version.id` hidden은 그대로 유지 |

P2 대상은 반복 행 내부에 있으나 fragment 자체가 `version`을 참조하지 않기 때문에 Thymeleaf 변수 스코프 충돌 가능성은 낮다. 다만 적용 리뷰에서는 fragment가 form 바깥으로 이동하지 않았는지, `scenarioId`와 `versionId`가 누락되지 않았는지 확인해야 한다.

## CSRF 계약과 영향도

`CsrfInterceptor`는 POST 요청에서 아래 이름의 파라미터를 우선 조회하고, 값이 없으면 헤더를 조회한다.

| 계약 항목 | 현재 값 | 근거 | fragment 반영 필요 |
|---|---|---|---|
| token parameter | `csrfToken` | `CSRF_TOKEN_PARAMETER` | 이름 변경 금지 |
| form id parameter | `csrfFormId` | `CSRF_FORM_ID_PARAMETER` | 이름 변경 금지 |
| token session source | `session.csrfToken` | 현재 템플릿 hidden value | 표현식 유지 |
| form id session source | `session.csrfFormId` | 현재 템플릿 hidden value | 표현식 유지 |

파라미터명과 세션 표현식이 그대로 유지되면 서버 검증 계약은 바뀌지 않는다. Ajax 경로는 `X-CSRF-Token`, `X-CSRF-FormId` 헤더도 사용할 수 있고 인터셉터가 파라미터 우선으로 처리하므로, native form fragment 공통화와 공존 가능하다.

## 회귀 위험

| 위험 | 수준 | 설명 | 완화 방법 |
|---|---|---|---|
| 파라미터명 오타 | 중간 | `csrfToken`, `csrfFormId` 중 하나라도 바뀌면 POST가 403 처리됨 | fragment 작성 시 인터셉터 상수와 이름 대조 |
| form 외부 삽입 | 중간 | `th:replace`가 form 밖에 있으면 submit payload에서 CSRF 값 누락 | 적용 위치를 opening form 직후로 고정 |
| 업무 hidden 누락 | 중간 | 게시 `scenarioId`, 활성화 `versionId`를 fragment로 오인해 제거하면 후속 처리 오류 가능 | CSRF hidden 2개만 치환하고 업무 hidden은 유지 |
| 반복 컨텍스트 영향 | 낮음 | 게시/활성화 form은 `th:each` 내부지만 fragment는 세션만 참조 | P2로 분리 적용 후 화면 렌더 확인 |
| DOM/CSS 영향 | 낮음 | `th:block` 사용 시 wrapper가 렌더링되지 않음 | wrapper element 미사용 |
| 세션 값 부재 | 낮음 | 기존에도 같은 세션 값을 직접 참조하므로 신규 위험은 아님 | 로그인/관리자 진입 흐름의 기존 CSRF 발급 유지 |

## 적용 우선순위

| 단계 | 범위 | 이유 | 커밋 후보 |
|---|---|---|---|
| P1 | `_csrfHidden.html` 신설, 등록/비활성화/새 초안 form 3건 적용 | 반복 컨텍스트 밖의 단순 form이라 회귀 위험이 가장 낮음 | `refactor(admmgr): extract csrf hidden inputs to fragment` |
| P2 | 게시/활성화 form 2건 적용 | `th:each` 내부지만 CSRF fragment 자체는 세션만 참조해 적용 가능 | `refactor(scenario-view): apply csrf hidden fragment in versioned forms` |
| P3 | 향후 admin native form 신규 작성 시 fragment 재사용 | 같은 CSRF 계약을 쓰는 form 중복 재발 방지 | 후속 화면별 작업에 포함 |

## 검증 체크리스트

적용 단계에서 아래 항목을 확인하면 된다.

| 구분 | 확인 항목 | 기대 결과 |
|---|---|---|
| 렌더링 | 등록/상세 화면 HTML에 `csrfToken`, `csrfFormId` hidden input이 존재 | 기존과 동일하게 2개 hidden 출력 |
| 등록/수정 | 시나리오 저장 form POST | 403 없이 저장 또는 기존 검증 오류 처리 |
| 비활성화 | `ACTIVE` 시나리오 비활성화 POST | 403 없이 상태 전환 |
| 새 초안 | 상세 화면 새 초안 POST | 403 없이 새 버전 생성 |
| 게시 | `DRAFT` 버전 게시 POST | CSRF 403 없이 기존 게시 검증 흐름 진입 |
| 활성화 | `PUBLISHED` 버전 활성화 POST | CSRF 403 없이 기존 활성화 검증 흐름 진입 |

## 문서 작성 단계 커밋 후보

현재 분석 문서 생성만 커밋할 경우 후보는 다음과 같다.

```text
docs(scenario): analyze csrf hidden fragment commonization scope
```

대상 파일:

```text
3.개발/cleverchat/docs/Scenario_CSRF_Hidden_Fragment_분석.md
```
