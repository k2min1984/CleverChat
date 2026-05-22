# Phase1A-G 관리자 Form CSRF E2E 검증결과

> 기준 문서: `Phase1A_G_Admin_Form_CSRF_E2E_작업지시.md`  
> 기준 화면: `scenarioRegist` 관리자 폼(`/admin/scenarios/new`, POST `/admin/scenarios`)  
> 작성 원칙: 실제 CSRF 토큰 값은 문서에 기록하지 않는다. 필요한 경우 "값 존재", "값 변경됨", "값 미변경", "미수신"으로만 기록한다.

## 1. 검증 환경 메타

| 항목 | 값 |
| --- | --- |
| 검증일 | 2026-05-22 (부분 확인 시점) |
| 검증자 | (실측 미완료) |
| 브랜치 | master |
| 커밋 | 3e0919d 시점 |
| 실행 환경 | 로컬 / 검증 서버 / 기타: (실측 미완료) |
| 애플리케이션 URL | (실측 미완료) |
| 브라우저 | (실측 미완료) |
| OS | (실측 미완료) |
| 관리자 계정 구분 | (실측 미완료) |
| 비고 | 전체 S/C 케이스 실측 미완료. 응답 헤더 회전 및 stale 시드 정리 미확인. |

## 2. 사전 확인

| 확인 항목 | 기대 상태 | 결과 | 비고 |
| --- | --- | --- | --- |
| Phase1A-D 적용 | 관리자 공통 head에 `csrfToken`, `csrfFormId` meta 렌더링 | 적용됨 | head meta `csrfToken`/`csrfFormId` 값 존재(S2에서 재확인) |
| Phase1A-E 적용 | 로그인 후 세션에 CSRF 토큰과 form id 발급 | 미확인 |  |
| Phase1A-F 적용 | `/admin/**` POST CSRF 인터셉터 등록, 성공 시 토큰 회전 | 미확인 |  |
| 관리자 로그인 | `/login`으로 관리자 로그인 가능 | 미확인 |  |
| 카테고리 seed | `/admin/scenarios/new` 카테고리 select에 선택 가능한 값 1개 이상 존재 | 미확인 |  |
| DevTools 설정 | Network 탭 `Preserve log` 활성화 | 미확인 |  |
| 공통 스니펫 등록 | `window.__csrfE2E` 및 보조 호출 스니펫 등록 완료 | 미확인 |  |

## 3. 케이스별 수동 결과표

| ID | 전송 경로 | 토큰 상태 | 기대 결과 | PASS/FAIL | HTTP status | 업무 데이터 생성/변경 | 응답 CSRF 헤더 | JSON code | meta 갱신 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1 | 일반 브라우저 submit | 정상 meta, hidden 없음 | 403, 업무 데이터 미생성, non-JSON 빈 본문 | 보류 |  |  |  | 비어 있음(non-JSON) | 미갱신 | 실측 미완료 |
| S2 | meta 계약 확인 | 렌더링 | `csrfToken`, `csrfFormId` meta 값 존재 | PASS |  | - | 값 존재 | - | - | meta `csrfToken`/`csrfFormId` 값 존재 확인 |
| C1 | `ADM.Form.submit` | 정상 | 2xx/3xx, 업무 처리, 새 CSRF 응답 헤더, meta 갱신 | 보류 |  |  |  | - |  | 실측 미완료 |
| C2 | `ADM.Form.submit` | 만료/stale | 403, 업무 데이터 미생성, CSRF 응답 헤더 없음, meta 미갱신 | 보류 |  |  |  | 비어 있음(non-JSON) 또는 - |  | 실측 미완료 |
| C3 | `ADM.Form.submit` | 위조 | 403, 업무 데이터 미생성, CSRF 응답 헤더 없음, meta 미갱신 | 보류 |  |  |  | 비어 있음(non-JSON) 또는 - |  | 실측 미완료 |
| C4 | `ADM.ajaxPost` | 정상 | 2xx/3xx, 업무 처리, 새 CSRF 응답 헤더, meta 갱신 | 보류 |  |  |  | - |  | 실측 미완료 |
| C5 | `ADM.ajaxPost` | 만료/stale | 403, 공통 오류 alert, 업무 데이터 미생성, `CSRF_INVALID` | 보류 |  |  |  | CSRF_INVALID |  | 실측 미완료 |
| C6 | `ADM.ajaxPost` | 위조 | 403, 공통 오류 alert, 업무 데이터 미생성, `CSRF_INVALID` | 보류 |  |  |  | CSRF_INVALID |  | 실측 미완료 |
| C7 | `ADM.Modal.submitForm` | 정상 | 2xx/3xx, 업무 처리, 새 CSRF 응답 헤더, meta 갱신 | 보류 |  |  |  | - |  | 실측 미완료 |
| C8 | `ADM.Modal.submitForm` | 만료/stale | 403, 공통 오류 alert, 업무 데이터 미생성, `CSRF_INVALID` | 보류 |  |  |  | CSRF_INVALID |  | 실측 미완료 |
| C9 | `ADM.Modal.submitForm` | 위조 | 403, 공통 오류 alert, 업무 데이터 미생성, `CSRF_INVALID` | 보류 |  |  |  | CSRF_INVALID |  | 실측 미완료 |

## 4. DevTools 확인표

### 4.1 요청 헤더/본문

| ID | URL | Method | `X-Requested-With` | `X-CSRF-Token` 헤더 | `X-CSRF-FormId` 헤더 | `csrfToken` body param | `csrfFormId` body param | 토큰 노출 여부 | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C1 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C2 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C3 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C4 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C5 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C6 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C7 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C8 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |
| C9 | `/admin/scenarios` | POST |  |  |  |  |  |  | 미실측 |

### 4.2 응답 헤더/본문

| ID | HTTP status | `X-CSRF-Token` 응답 헤더 | `X-CSRF-FormId` 응답 헤더 | Content-Type | 본문 형태 | JSON `success` | JSON `error.code` | 비고 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1 |  |  |  |  | 비어 있음(non-JSON) | - | - | 미실측 |
| C1 |  |  |  |  |  |  | - | 미실측 |
| C2 |  |  |  |  | 비어 있음(non-JSON) 또는 - |  |  | 미실측 |
| C3 |  |  |  |  | 비어 있음(non-JSON) 또는 - |  |  | 미실측 |
| C4 |  |  |  |  |  |  | - | 미실측 |
| C5 |  |  |  |  | JSON | false | CSRF_INVALID | 미실측 |
| C6 |  |  |  |  | JSON | false | CSRF_INVALID | 미실측 |
| C7 |  |  |  |  |  |  | - | 미실측 |
| C8 |  |  |  |  | JSON | false | CSRF_INVALID | 미실측 |
| C9 |  |  |  |  | JSON | false | CSRF_INVALID | 미실측 |

### 4.3 사용자 화면 동작

| ID | 화면/alert/redirect 관찰 | 성공 콜백 실행 여부 | 오류 alert 표시 여부 | meta 변경 관찰 | 비고 |
| --- | --- | --- | --- | --- | --- |
| S1 |  |  |  |  | 미실측 |
| S2 |  |  |  |  | meta 값 존재 확인 외 사용자 화면 동작 미실측 |
| C1 |  |  |  |  | 미실측 |
| C2 |  |  |  |  | 미실측 |
| C3 |  |  |  |  | 미실측 |
| C4 |  |  |  |  | 미실측 |
| C5 |  |  |  |  | 미실측 |
| C6 |  |  |  |  | 미실측 |
| C7 |  |  |  |  | 미실측 |
| C8 |  |  |  |  | 미실측 |
| C9 |  |  |  |  | 미실측 |

## 5. stale 시드 정리표

`__csrfE2E.stale()`은 토큰 회전을 만들기 위해 정상 POST를 1회 실행하므로 `CSRF stale seed ...` 제목의 시나리오가 생성될 수 있다.

| 대상 케이스 | 생성 여부 | 정리 방법 | 정리 결과 | 담당자 | 비고 |
| --- | --- | --- | --- | --- | --- |
| C2 | 미확인 | 관리자 화면 삭제 / 삭제 API / DB 삭제 / 해당 없음 | 미정리 |  | stale 시드 정리 미확인 |
| C5 | 미확인 | 관리자 화면 삭제 / 삭제 API / DB 삭제 / 해당 없음 | 미정리 |  | stale 시드 정리 미확인 |
| C8 | 미확인 | 관리자 화면 삭제 / 삭제 API / DB 삭제 / 해당 없음 | 미정리 |  | stale 시드 정리 미확인 |

DB 직접 정리를 수행한 경우 운영자 권한 검증 환경에서만 아래 SQL을 사용했는지 확인한다.

```sql
DELETE FROM scn_scenario WHERE title LIKE 'CSRF stale seed %';
```

## 6. 결함 기록

### 6.1 결함 목록

| 결함 ID | 관련 케이스 | 심각도 | 현상 | 기대 결과 | 실제 결과 | 재현 절차 요약 | 증거 위치 | 상태 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| D-001 |  |  |  |  |  |  |  | 신규 / 확인 중 / 보류 / 완료 |
| D-002 |  |  |  |  |  |  |  | 신규 / 확인 중 / 보류 / 완료 |
| D-003 |  |  |  |  |  |  |  | 신규 / 확인 중 / 보류 / 완료 |

### 6.2 후속 PR 후보 매핑

| 후보 | 조건 | 관련 결함/케이스 | 후속 조치 필요 여부 | 비고 |
| --- | --- | --- | --- | --- |
| Phase1A-D 문서 동기화 | meta 이름 불일치로 검증자 혼선 발생 |  |  | Phase1A-D 문서를 실제 계약 `csrfToken`, `csrfFormId` 기준으로 정정 |
| Modal.submitForm 헤더 fallback | C7/C8/C9가 서버에서 헤더 미수용으로 실패하는 환경 발견 |  |  | 서버 인터셉터의 헤더 fallback 유지 여부 확인 또는 Modal body 파라미터 추가 검토 |
| `ADM.Form.submit` body 파라미터화 | 헤더 fallback 정책 폐기 또는 헤더 미허용 환경 발견 |  |  | `FormData`에 `csrfToken`, `csrfFormId` append 검토 |
| 기본 form submit CSRF | S1이 200/302로 통과한 경우 |  |  | Thymeleaf form hidden field 주입 또는 submit 이벤트 공통화 검토 |
| 다중 탭 stale 실패 | 정상 사용 중 동시 요청으로 stale 실패 빈발 |  |  | 이전 토큰 1개 허용 등 완화 정책 별도 보안 검토 |
| 카테고리 seed 부재 | 검증 환경마다 category 준비가 반복 누락 |  |  | 관리자 검증 seed 또는 운영 메모 보완 |

### 6.3 작업지시서 §8 위반 항목 체크리스트

| 위반 항목 | 발생 여부 | 관련 케이스 | 비고 |
| --- | --- | --- | --- |
| 정상 토큰 요청이 403으로 실패 |  |  |  |
| 만료/stale 또는 위조 토큰 요청이 2xx/3xx로 성공 |  |  |  |
| 실패 응답에서 새 CSRF 토큰 발급 |  |  |  |
| Ajax 차단 응답 본문에 `code: CSRF_INVALID` 없음 |  |  |  |
| Ajax 차단 응답이 HTML 에러 페이지로 반환 |  |  |  |
| CSRF 값이 URL, 화면 메시지, 로그, 결과 보고서에 노출 |  |  |  |
| `ADM.ajaxPost` 또는 `ADM.Modal.submitForm`에서 403 alert 없이 성공 콜백 실행 |  |  |  |
| 차단 요청으로 시나리오 데이터 생성 또는 변경 |  |  |  |

## 7. 최종 판정

### 7.1 집계표

| 구분 | 총 건수 | PASS | FAIL | 보류 | 비고 |
| --- | ---: | ---: | ---: | ---: | --- |
| S 케이스 | 2 | 1 | 0 | 1 | S1 보류, S2 PASS |
| C 케이스 | 9 | 0 | 0 | 9 | C1~C9 실측 미완료 |
| 전체 | 11 | 1 | 0 | 10 | 전체 판정 불가 |

### 7.2 판정 규칙

| 최종 판정 | 조건 |
| --- | --- |
| PASS | S1, S2, C1~C9 전체 PASS이며, stale 시드 정리 완료, 토큰 원문 노출 없음 |
| CONDITIONAL PASS | 기능 차단 결함은 없으나 문서 동기화, seed 준비 등 후속 PR 후보만 존재 |
| FAIL | 작업지시서 §8 공통 FAIL 항목 중 1개 이상 발생 |
| HOLD | 환경 준비 미완료, 카테고리 seed 부재, 검증 중단 등으로 전체 판정 불가 |

### 7.3 최종 판정 기록

| 항목 | 값 |
| --- | --- |
| 최종 판정 | HOLD |
| 판정 사유 | S1/S2/C1~C9 전체 실측 미완료, 응답 CSRF 헤더 회전 미확인, stale 시드 정리 미확인 |
| 필수 후속 조치 | 전체 S/C 실측, 응답 CSRF 헤더 회전 관찰, stale 시드 정리 절차 실행 |
| 검증자 서명 | (실측 미완료) |
| 검토자 서명 | (실측 미완료) |

## 부록 A. 토큰 노출 금지 재확인

| 점검 항목 | 결과 | 비고 |
| --- | --- | --- |
| 결과표에 실제 `csrfToken` 값 미기재 | 준수 | 값 존재/미수신 등 상태어만 사용 |
| 결과표에 실제 `csrfFormId` 값 미기재 | 준수 | 값 존재/미수신 등 상태어만 사용 |
| 화면 캡처 또는 첨부 자료에 토큰 원문 마스킹 | 미확인 | 첨부 자료 실측 미수행 |
| 콘솔 로그, 서버 로그, 메신저 공유 내용에 토큰 원문 미노출 | 미확인 | 로그/공유 내용 실측 미수행 |
| URL query string에 토큰 값 미노출 | 미확인 | URL 실측 미수행 |

## 부록 C. scenarioView 추가 검증 및 참고사항

### C.1 scenarioView 추가 버튼 검증

| 버튼 | 검증 결과 | 분류 | 비고 |
| --- | --- | --- | --- |
| 게시 버튼 | CSRF 검증 통과 후 업무 검증에서 `VALIDATION_ERROR` 발생 | CSRF PASS / 업무 VALIDATION_ERROR | 응답 메시지: `저장된 그래프가 없습니다.` |
| 활성화 버튼 | 현재 상태 조건상 화면에 미노출되어 트리거 불가 | 미수행 | 버튼 미노출 상태이므로 CSRF 검증 요청 자체가 발생하지 않음 |

### C.2 참고사항 (UI 자산 도입 범위)

- 현재 admin UI는 Phase1A-C/E 범위의 최소 `css`/`font`/`js`만 적용된 상태이다.
- `images/`, `icon/`, `style2/` 전체 자산은 아직 도입되지 않았다.
- 위 자산 미도입으로 일부 레이아웃이 비정상처럼 보일 수 있으나, Phase1A-G PASS/FAIL 판정에는 직접 영향이 없다.

### C.3 scenarioRegist native form POST 참고사항

- `scenarioRegist` native form POST에서 `csrfToken`/`csrfFormId` hidden 파라미터 전달이 정상화되었다(commit d11d334).
- hidden 파라미터가 요청 본문으로 정상 전달되는 사실만 확인했으며, 토큰 원문은 기록하지 않는다.
- 이는 CSRF 인터셉터의 파라미터 우선 정책에 부합한다.
- 단, 해당 경로의 인터셉터 실측은 아직 수행하지 않았다.

## 부록 D. 참고 라인

2026-05-20 작업지시서 기준 참고 위치:

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
