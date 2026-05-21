# Phase1A-G 관리자 Form CSRF E2E 작업지시

> 작성일: 2026-05-20  
> 작업 원칙: 이 문서는 수동 E2E 검증 작업지시서이다. 본 단계에서는 코드 수정, 테스트 코드 생성, 설정 변경을 수행하지 않는다.  
> 기준 화면: `scenarioRegist` 관리자 폼(`/admin/scenarios/new`, POST `/admin/scenarios`)  
> 검증 목적: 정상 토큰, 만료/stale 토큰, 위조 토큰을 `ADM.Form.submit`, `ADM.ajaxPost`, `ADM.Modal.submitForm` 세 경로에서 수동 검증한다.

## 1. 목적과 핵심 판단

Phase1A-D/E/F 적용 후 관리자 POST 요청의 CSRF 보호가 실제 브라우저 흐름에서 동작하는지 확인한다. 검증은 `scenarioRegist` 화면을 기준으로 하되, 공통 JavaScript 전송 경로 3개를 모두 통과시킨다.

핵심 판단:

- 검증 매트릭스는 3개 토큰 상태와 3개 전송 경로의 조합이다.
- 토큰 상태는 정상, 만료/stale, 위조로 구분한다.
- 전송 경로는 `ADM.Form.submit`, `ADM.ajaxPost`, `ADM.Modal.submitForm`이다.
- `scenarioRegist`의 실제 HTML 폼은 기본 submit 구조이므로, 공통 함수 경로 검증은 브라우저 콘솔 보조 스니펫으로 호출한다.
- 성공 요청은 2xx/3xx 처리 후 새 `X-CSRF-Token`, `X-CSRF-FormId` 응답 헤더로 meta 값이 갱신되어야 한다.
- 실패 요청은 403이어야 하며 실패 응답에서 CSRF 값을 재발급하지 않아야 한다.
- 토큰 값은 결과 보고서, 로그, 화면 캡처 본문에 남기지 않는다.

## 2. 선행 조건

다음 작업이 같은 브랜치 또는 검증 환경에 반영되어 있어야 한다.

| 선행 | 확인 내용 |
| --- | --- |
| Phase1A-D | 관리자 공통 head에 CSRF meta가 렌더링됨 |
| Phase1A-E | 로그인 후 세션에 `csrfToken`, `csrfFormId`가 발급됨 |
| Phase1A-F | `/admin/**` POST CSRF 인터셉터가 등록되고 검증 성공 시 토큰을 회전함 |
| 관리자 로그인 | `/login`으로 관리자 로그인 가능 |
| 시나리오 카테고리 | `/admin/scenarios/new`에서 카테고리 select에 선택 가능한 값이 1개 이상 존재 |

카테고리가 없으면 `scenarioRegist` 저장 검증이 유효한 업무 POST까지 도달하지 못한다. 검증 전 DB seed 또는 관리자 API로 카테고리를 먼저 준비한다.

## 3. 검증 대상 파일

아래 파일은 참고만 한다. 본 Phase1A-G 작업에서 수정하지 않는다.

| 구분 | 파일 | 확인 포인트 |
| --- | --- | --- |
| 화면 | `3.개발/cleverchat/src/main/resources/templates/admmgr/scenario/scenarioRegist.html` | GET `/admin/scenarios/new`, POST action `/admin/scenarios` |
| 공통 head | `3.개발/cleverchat/src/main/resources/templates/admmgr/common/head.html` | `csrfToken`, `csrfFormId` meta 렌더링 |
| 공통 JS | `3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js` | `ADM.Form.submit`, `ADM.ajaxPost`, `ADM.Modal.submitForm` |
| 인터셉터 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/CsrfInterceptor.java` | parameter/header 검증, 403 응답, 토큰 회전 |
| 발급기 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/auth/security/CsrfTokenIssuer.java` | 세션 attribute명 |
| 컨트롤러 | `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` | `scenarioRegist`, `scenarioRegistProc` |

## 4. 실제 코드 기준 클라이언트 계약

현재 실제 코드 기준 계약은 다음과 같다.

| 항목 | 실제 계약 |
| --- | --- |
| meta token 이름 | `csrfToken` |
| meta form id 이름 | `csrfFormId` |
| POST 파라미터 token | `csrfToken` |
| POST 파라미터 form id | `csrfFormId` |
| 요청 헤더 token | `X-CSRF-Token` |
| 요청 헤더 form id | `X-CSRF-FormId` |
| 응답 헤더 token | `X-CSRF-Token` |
| 응답 헤더 form id | `X-CSRF-FormId` |
| Ajax 판정 헤더 | `X-Requested-With: XMLHttpRequest` |

주의:

- Phase1A-D 초안에는 meta 이름이 `_csrf_token`, `_csrf_form_id`로 기재되어 있었으나, 현재 실제 `head.html`과 `ADM.Common.js`는 `csrfToken`, `csrfFormId`를 사용한다.
- Phase1A-G 수동 검증은 실제 코드 기준인 `csrfToken`, `csrfFormId`를 따른다.
- 문서 간 계약명 불일치가 재발하면 문서 동기화 PR 후보로 기록한다.
- `ADM.Form.submit`은 현재 `FormData` body에 `csrfToken`, `csrfFormId`를 append하지 않고 `X-CSRF-Token`, `X-CSRF-FormId` 헤더로만 송신하므로, `CsrfInterceptor`의 헤더 fallback에 의존한다.

## 5. 검증 환경 준비

1. 애플리케이션을 로컬 또는 검증 서버에서 실행한다.
2. 관리자 계정으로 로그인한다.
3. 브라우저 개발자도구 Network 탭에서 `Preserve log`를 켠다.
4. `/admin/scenarios/new`에 접근한다.
5. 콘솔에서 아래 공통 스니펫을 등록한다.

```javascript
window.__csrfE2E = {
  tokenMeta: function(){ return document.querySelector('meta[name="csrfToken"]'); },
  formIdMeta: function(){ return document.querySelector('meta[name="csrfFormId"]'); },
  snapshot: function(){
    return {
      token: this.tokenMeta() && this.tokenMeta().content,
      formId: this.formIdMeta() && this.formIdMeta().content
    };
  },
  restore: function(s){
    if (s && this.tokenMeta()) this.tokenMeta().content = s.token;
    if (s && this.formIdMeta()) this.formIdMeta().content = s.formId;
  },
  forge: function(){
    if (this.tokenMeta()) this.tokenMeta().content = 'forged-token';
    if (this.formIdMeta()) this.formIdMeta().content = 'forged-form-id';
  },
  stale: function(){
    var s = this.snapshot();
    return fetch('/admin/scenarios', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'X-CSRF-Token': s.token,
        'X-CSRF-FormId': s.formId
      },
      body: new URLSearchParams({
        categoryId: document.querySelector('#categoryId').value,
        title: 'CSRF stale seed ' + Date.now(),
        description: 'rotate token before stale request'
      })
    }).then(function(res){
      var newToken = res.headers.get('X-CSRF-Token');
      var newFormId = res.headers.get('X-CSRF-FormId');
      if (newToken && newFormId) {
        document.querySelector('meta[name="csrfToken"]').content = newToken;
        document.querySelector('meta[name="csrfFormId"]').content = newFormId;
      }
      return s;
    });
  },
  fill: function(prefix){
    document.querySelector('#categoryId').value = document.querySelector('#categoryId option[value]:not([value=""])').value;
    document.querySelector('#title').value = prefix + ' ' + Date.now();
    document.querySelector('#description').value = 'Phase1A-G CSRF E2E manual verification';
  }
};
```

보조 호출 스니펫:

```javascript
window.__csrfE2E.submitByAdmForm = function(prefix){
  __csrfE2E.fill(prefix || 'C1 ADM.Form.submit');
  ADM.Form.submit(document.querySelector('form'));
};

window.__csrfE2E.submitByAjaxPost = function(prefix){
  __csrfE2E.fill(prefix || 'C4 ADM.ajaxPost');
  var data = new URLSearchParams({
    categoryId: document.querySelector('#categoryId').value,
    title: document.querySelector('#title').value,
    description: document.querySelector('#description').value
  }).toString();
  ADM.ajaxPost('/admin/scenarios', data, function(xhr){
    console.log('ADM.ajaxPost status:', xhr.status, 'location/body:', xhr.responseText.slice(0, 120));
  });
};

window.__csrfE2E.submitByModal = function(prefix){
  __csrfE2E.fill(prefix || 'C7 ADM.Modal.submitForm');
  ADM.Modal.submitForm(document.querySelector('form'), function(html){
    console.log('ADM.Modal.submitForm response:', html.slice(0, 120));
  });
};
```

## 6. 검증 케이스 매트릭스

| ID | 전송 경로 | 토큰 상태 | 기대 결과 |
| --- | --- | --- | --- |
| C1 | `ADM.Form.submit` | 정상 | POST 성공, 신규 시나리오 생성 또는 redirect 응답, meta 토큰 갱신 |
| C2 | `ADM.Form.submit` | 만료/stale | 403, 업무 데이터 미생성, meta 토큰 미갱신 |
| C3 | `ADM.Form.submit` | 위조 | 403, 업무 데이터 미생성, meta 토큰 미갱신 |
| C4 | `ADM.ajaxPost` | 정상 | POST 성공, 신규 시나리오 생성 또는 redirect 응답, meta 토큰 갱신 |
| C5 | `ADM.ajaxPost` | 만료/stale | 403, 공통 오류 alert, 업무 데이터 미생성, meta 토큰 미갱신 |
| C6 | `ADM.ajaxPost` | 위조 | 403, 공통 오류 alert, 업무 데이터 미생성, meta 토큰 미갱신 |
| C7 | `ADM.Modal.submitForm` | 정상 | POST 성공, 신규 시나리오 생성 또는 procScript/redirect 응답, meta 토큰 갱신 |
| C8 | `ADM.Modal.submitForm` | 만료/stale | 403, 공통 오류 alert, 업무 데이터 미생성, meta 토큰 미갱신 |
| C9 | `ADM.Modal.submitForm` | 위조 | 403, 공통 오류 alert, 업무 데이터 미생성, meta 토큰 미갱신 |
| S1 | 일반 브라우저 submit | 정상 | 403 차단, 업무 데이터 미생성. form에 hidden CSRF가 없어 인터셉터가 차단함 |
| S2 | meta 계약 확인 | 렌더링 | `csrfToken`, `csrfFormId` meta가 빈 값이 아님 |

## 7. 케이스별 절차

각 케이스는 가능하면 새 탭 또는 새 로그인 세션에서 시작한다. 한 케이스가 성공하면 토큰이 회전되므로 다음 케이스 전 `/admin/scenarios/new`를 새로고침한다.

### S2. meta 계약 확인

1. `/admin/scenarios/new`를 연다.
2. 콘솔에서 `__csrfE2E.snapshot()`을 실행한다.
3. `token`, `formId`가 모두 빈 값이 아니면 PASS.
4. meta name이 `_csrf_token`, `_csrf_form_id`로만 존재하거나 값이 비어 있으면 FAIL.

### S1. 일반 브라우저 submit 403 차단 확인

1. `/admin/scenarios/new`를 새로고침한다.
2. 카테고리, 제목, 설명을 입력한다.
3. 저장 버튼을 클릭한다.
4. POST `/admin/scenarios` 요청이 403인지 확인한다.
5. 업무 데이터가 생성되지 않으면 PASS.
6. 성공 시 FAIL: hidden CSRF 미주입 상태에서 인터셉터 동작을 우회한 것이다.
7. 이 케이스는 공통 JS 경로 검증이 아니라 화면 기본 submit 차단 동작 확인용으로 기록한다.

### C1. ADM.Form.submit 정상 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.submitByAdmForm('C1 ADM.Form normal')`을 실행한다.
3. Network에서 POST `/admin/scenarios` 요청을 확인한다.
4. 요청 헤더에 `X-CSRF-Token`, `X-CSRF-FormId`가 포함되었는지 확인한다.
5. 응답이 2xx/3xx 계열이고 응답 헤더에 새 CSRF 값이 있으면 PASS.

### C2. ADM.Form.submit 만료/stale 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.stale().then(function(old){ __csrfE2E.restore(old); __csrfE2E.submitByAdmForm('C2 ADM.Form stale'); })`을 실행한다.
3. 두 번째 POST `/admin/scenarios` 요청이 403인지 확인한다.
4. 두 번째 응답에 `X-CSRF-Token`, `X-CSRF-FormId`가 없고 업무 데이터가 생성되지 않으면 PASS.

### C3. ADM.Form.submit 위조 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.forge(); __csrfE2E.submitByAdmForm('C3 ADM.Form forged')`를 실행한다.
3. POST `/admin/scenarios` 요청이 403인지 확인한다.
4. 응답에서 새 CSRF 헤더가 내려오지 않고 업무 데이터가 생성되지 않으면 PASS.

### C4. ADM.ajaxPost 정상 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.submitByAjaxPost('C4 ajaxPost normal')`을 실행한다.
3. Network에서 요청 body에 `csrfToken`, `csrfFormId` 파라미터가 포함되는지 확인한다.
4. 요청 헤더에도 `X-CSRF-Token`, `X-CSRF-FormId`가 포함되는지 확인한다.
5. 응답이 성공이고 meta가 응답 헤더의 새 값으로 갱신되면 PASS.

### C5. ADM.ajaxPost 만료/stale 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.stale().then(function(old){ __csrfE2E.restore(old); __csrfE2E.submitByAjaxPost('C5 ajaxPost stale'); })`을 실행한다.
3. 두 번째 POST가 403인지 확인한다.
4. 공통 오류 alert가 표시되고 업무 데이터가 생성되지 않으면 PASS.

### C6. ADM.ajaxPost 위조 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.forge(); __csrfE2E.submitByAjaxPost('C6 ajaxPost forged')`를 실행한다.
3. POST가 403인지 확인한다.
4. 공통 오류 alert가 표시되고 업무 데이터가 생성되지 않으면 PASS.

### C7. ADM.Modal.submitForm 정상 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.submitByModal('C7 Modal normal')`을 실행한다.
3. Network에서 POST `/admin/scenarios` 요청을 확인한다.
4. 요청 헤더에 `X-CSRF-Token`, `X-CSRF-FormId`가 포함되는지 확인한다.
5. 응답이 성공이고 meta가 응답 헤더의 새 값으로 갱신되면 PASS.

### C8. ADM.Modal.submitForm 만료/stale 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.stale().then(function(old){ __csrfE2E.restore(old); __csrfE2E.submitByModal('C8 Modal stale'); })`을 실행한다.
3. 두 번째 POST가 403인지 확인한다.
4. 공통 오류 alert가 표시되고 업무 데이터가 생성되지 않으면 PASS.

### C9. ADM.Modal.submitForm 위조 토큰

1. `/admin/scenarios/new`를 새로고침한다.
2. 콘솔에서 `__csrfE2E.forge(); __csrfE2E.submitByModal('C9 Modal forged')`를 실행한다.
3. POST가 403인지 확인한다.
4. 공통 오류 alert가 표시되고 업무 데이터가 생성되지 않으면 PASS.

### stale 시드 정리

`__csrfE2E.stale()`은 토큰 회전을 만들기 위해 정상 POST를 1회 실행하므로 `CSRF stale seed ...` 제목의 시나리오가 생성될 수 있다. C2/C5/C8 검증 종료 후 다음 중 하나로 시드 데이터를 정리한다.

1. 관리자 화면에서 `/admin/scenarios?q=CSRF stale seed` 또는 동등한 검색 조건으로 생성 목록을 확인한 뒤 삭제한다.
2. 삭제 API가 검증 환경에 제공되면 해당 API로 제거한다.
3. DB seed 검증 환경에서 운영자 권한으로만 `DELETE FROM scn_scenario WHERE title LIKE 'CSRF stale seed %'`를 실행한다.

## 8. PASS/FAIL 판정 기준

공통 PASS:

- 정상 토큰 요청은 서버 업무 처리까지 도달한다.
- 정상 토큰 요청의 응답에는 새 `X-CSRF-Token`, `X-CSRF-FormId`가 포함된다.
- 공통 JS 경로는 응답 헤더를 meta에 반영해 다음 요청이 새 토큰으로 진행된다.
- 만료/stale 또는 위조 토큰 요청은 403으로 차단된다.
- 차단 응답에서는 새 CSRF 응답 헤더가 내려오지 않는다.
- 차단 요청으로 시나리오 데이터가 생성 또는 변경되지 않는다.
- Ajax 차단 응답(C5, C6, C8, C9 및 `X-Requested-With: XMLHttpRequest` 또는 `Accept: application/json` 포함 요청)은 본문이 `{"success":false,"error":{"code":"CSRF_INVALID", ...}}` 형태여야 한다.
- Ajax가 아닌 차단 응답(S1, 일반 form submit)은 본문이 비어 있고 상태 코드만 403이어야 한다.

공통 FAIL:

- 정상 토큰 요청이 403으로 실패한다.
- 만료/stale 또는 위조 토큰 요청이 2xx/3xx로 성공한다.
- 실패 응답에서 새 CSRF 토큰이 발급된다.
- Ajax 차단 응답의 본문에 `code: CSRF_INVALID`가 없거나 다른 코드/구조이면 FAIL.
- Ajax 차단 응답이 HTML 에러 페이지로 반환되면 FAIL.
- CSRF 값이 URL, 화면 메시지, 로그, 결과 보고서에 노출된다.
- `ADM.ajaxPost` 또는 `ADM.Modal.submitForm`에서 403 alert 없이 성공 콜백이 실행된다.

## 9. 결과 보고서 양식

```text
검증일:
검증자:
브랜치/커밋:
실행 환경:
브라우저:

사전 확인:
- 관리자 로그인:
- 카테고리 존재:
- meta csrfToken/csrfFormId 렌더링:

케이스 결과:
| ID | PASS/FAIL | HTTP status | 업무 데이터 생성/변경 | 응답 CSRF 헤더 | 응답 본문 JSON code | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| S1 | | | | | 비어 있음(non-JSON) | |
| S2 | | | | | - | |
| C1 | | | | | - | |
| C2 | | | | | 비어 있음(non-JSON) 또는 - | |
| C3 | | | | | 비어 있음(non-JSON) 또는 - | |
| C4 | | | | | - | |
| C5 | | | | | CSRF_INVALID | |
| C6 | | | | | CSRF_INVALID | |
| C7 | | | | | - | |
| C8 | | | | | CSRF_INVALID | |
| C9 | | | | | CSRF_INVALID | |

결함 후보:
- 문서 계약 불일치 재발: Phase1A-D 문서의 meta 이름과 실제 `csrfToken`/`csrfFormId` 불일치
- Modal.submitForm 관련:
- 기타:
```

토큰 원문 값은 보고서에 붙여 넣지 않는다. 필요한 경우 "값 존재", "값 변경됨", "값 미변경"만 기록한다.

## 10. 금지 범위

이번 Phase1A-G 검증에서 다음 작업은 금지한다.

- Java, JavaScript, HTML, SQL 코드 수정
- 테스트 코드 추가 또는 변경
- 설정 파일 변경
- DB schema 변경
- CSRF 토큰명, 헤더명, 파라미터명 변경
- Spring Security CSRF 활성화
- 실패 케이스를 통과시키기 위한 인터셉터 완화
- 실제 토큰 값을 문서, 이슈, 로그, 메신저에 공유

## 11. 산출물 체크리스트

- `docs/Phase1A_G_Admin_Form_CSRF_E2E_작업지시.md` 작성
- 정상/만료/위조 3케이스 정의
- `ADM.Form.submit`, `ADM.ajaxPost`, `ADM.Modal.submitForm` 경로 포함
- `scenarioRegist` 기준 수동 검증 절차 포함
- PASS/FAIL 기준 포함
- 결과 보고서 양식 포함
- stale 시드 정리 완료
- 코드 수정 금지 범위 명시
- 실제 코드 기준 meta 이름과 초안 불일치 이력 명시

## 12. 롤백/후속 PR 후보 매핑

| 후보 | 조건 | 후속 조치 |
| --- | --- | --- |
| Phase1A-D 문서 동기화 | meta 이름 불일치로 검증자 혼선 발생 | Phase1A-D 문서를 실제 계약 `csrfToken`, `csrfFormId` 기준으로 정정 |
| Modal.submitForm 헤더 fallback | C7/C8/C9가 서버에서 헤더 미수용으로 실패하는 환경 발견 | 서버 인터셉터의 헤더 fallback 유지 여부 확인 또는 Modal body 파라미터 추가를 별도 PR로 검토 |
| `ADM.Form.submit` body 파라미터화 | 헤더 fallback 정책 폐기 또는 헤더 미허용 환경 발견 | `ADM.Form.submit`이 `FormData`에 `csrfToken`, `csrfFormId`를 append 하도록 별도 PR로 변경 |
| 기본 form submit CSRF | S1이 200/302로 통과한 경우(=인터셉터 우회) | Thymeleaf form hidden field 주입 또는 submit 이벤트 공통화 검토 |
| 다중 탭 stale 실패 | 정상 사용 중 동시 요청으로 stale 실패 빈발 | 이전 토큰 1개 허용 등 완화 정책 별도 보안 검토 |
| 카테고리 seed 부재 | 검증 환경마다 category 준비가 반복 누락 | 관리자 검증 seed 또는 운영 메모 보완 |

현재 실제 `CsrfInterceptor`는 요청 파라미터가 없으면 `X-CSRF-Token`, `X-CSRF-FormId` 헤더를 fallback으로 읽는다. 따라서 현재 코드 기준으로는 `ADM.Form.submit`과 `Modal.submitForm`이 body가 아닌 헤더로만 CSRF를 전달해도 정상 케이스가 PASS해야 한다.

## 13. 참고 라인

2026-05-20 현재 파일 기준 참고 위치:

| 파일 | 라인 | 내용 |
| --- | --- | --- |
| `head.html` | 6-8 | `ctx`, `csrfToken`, `csrfFormId` meta |
| `scenarioRegist.html` | 15-33 | `scenarioForm` POST form |
| `ADM.Common.js` | 154-196 | `ADM.Form.submit` |
| `ADM.Common.js` | 160-161 | `ADM.Form.submit` 헤더-only CSRF 송신 |
| `ADM.Common.js` | 223-243 | `ADM.getCsrfParam`, `ADM.setCsrfHeaders` |
| `ADM.Common.js` | 245-260 | CSRF meta 갱신 |
| `ADM.Common.js` | 262-293 | `ADM.ajaxPost` |
| `ADM.Common.js` | 391-424 | `ADM.Modal.submitForm` |
| `CsrfInterceptor.java` | 42-53 | 세션/요청 CSRF 비교 및 성공 시 재발급 |
| `CsrfInterceptor.java` | 62-68 | 파라미터 우선, 헤더 fallback |
| `CsrfInterceptor.java` | 94-103 | 403/JSON 실패 응답 |
| `AdmScenarioController.java` | 49-65 | `scenarioRegist`, `scenarioRegistProc` |
