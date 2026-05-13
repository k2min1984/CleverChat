# OverseasNPP 8소스 기준 패키지/폴더/클래스 명명 차이 분석

- 작성일: 2026-05-13
- 비교 기준: `8.소스/OverseasNPP_20260511`
- 비교 대상: `3.개발/cleverchat`
- 대상 패키지: `scenario`, `chatbot`, `auth`, `common`
- 작업 범위: 분석 문서 작성만 수행. 소스 파일 수정, 리네임, 이동, 삭제, 커밋은 수행하지 않는다.

> 방향 정정 (2026-05-13)
>
> `8.소스/OverseasNPP_20260511`은 참고자료가 아니라 CleverChat 개발의 원본 베이스 소스다. 실제 패키지 구조는 `kr.core`(공통 코어), `kr.admmgr`(관리자 업무 root), `kr.admmgr.<기능별>`(업무 패키지), `com`(공통/외부 유틸) 축이다. 따라서 아래 분석의 권장 방향은 "CleverChat 현행 유지"가 아니라 "기능별 업무 패키지 중심 8소스 우선 정렬"이다. 기술적으로 불가피한 예외는 `1.기획/결정사항.md` §11.3에 명시된 항목만 인정하며, 예외가 아닌 패키지/컨트롤러/화면/액션명/SQL XML/SQL ID/관리자 UI 구성은 8소스 기준으로 정렬한다.

## 1. 기준 구조 요약

| 구분 | OverseasNPP_20260511 기준 | CleverChat 현재 |
|---|---|---|
| 최상위 Java 패키지 | `kr.core`, `kr.admmgr`, `com` | `kr.co.cleverchat.*` |
| 코어 패키지 방식 | `kr.core`에 공통 코어, 설정, 세션/부트스트랩 성격 배치 | `config`, `web`, `common`으로 분산 |
| 업무 패키지 방식 | `kr.admmgr`를 관리자 업무 root로 두고 `kr.admmgr.<기능별>` 아래 컨트롤러 중심 배치 | `domain.<업무>.{controller,service,mapper,model,dto}` 레이어 배치 |
| 공통/외부 유틸 방식 | `com` 계열에 공통/외부 유틸, 핸들러, 로그, 기반 유틸 배치 | `common.api`, `common.error`, `common.audit` |
| CleverChat 정렬 방향 | 기능별 업무 패키지 중심 구조로 단계 정렬 | 현행 `domain/common` 기반 Spring Boot 스타일은 최종 목표 구조가 아님 |
| 컨트롤러 명명 | `Adm<업무>Controller` | `<업무><역할>Controller`, `*ApiController`, `*PageController` |
| 서비스 레이어 | 별도 `Service` 계층 없음. 컨트롤러에서 `CDao` 사용 | `*Service` 계층 존재 |
| Mapper 방식 | Mapper 인터페이스 없음. `resources/sql/postgresql/tb_*.xml` 직접 사용 | `*Mapper.java` + `resources/mapper/<domain>/*Mapper.xml` |
| SQL XML 명명 | `tb_<table>.xml`, namespace도 테이블명 중심 | `<Entity>Mapper.xml`, namespace는 Mapper 인터페이스 |
| 화면 템플릿 | `templates/admmgr/<업무>/<entity>{List|Regist|View|Layer}.html` | `templates/admin/<업무>/list.html`, `form.html`, `detail.html` |
| URL 스타일 | `/admmgr/<업무>/<action>.do` | `/admin/<resource>`, `/admin/api/<resource>` |
| 인증 세션 | `AdminVO.getSessionVO(request)` 중심 | `AdminSession`, `CurrentAdminProvider`, `@CurrentUser` |

## 2. Auth 명명 차이와 매핑

| CleverChat 현재 | 8소스 기준으로 맞출 경우 | 변경 성격 | 비고 |
|---|---|---|---|
| `kr.co.cleverchat.domain.auth` | `kr.admmgr.member`, `kr.admmgr.security` | 패키지 분리 | 로그인/계정은 `member`, IP/접속이력/보안은 `security`로 분리 |
| `domain.auth.security.LoginController` | `kr.admmgr.member.AdmLoginController` | 클래스/패키지명 변경 | 8소스에 `AdmLoginController.java` 존재 |
| `domain.auth.security.AuthInterceptor` | `com` 계열 인터셉터 | 공통 유틸 패키지 이동 | 8소스의 `com` 하위 인터셉터 위치와 일치 |
| `domain.auth.security.AdminSession` | `com` 계열 `AdminVO` | 모델 통합 | 정적 세션 조회 패턴으로 전환 필요 |
| `domain.auth.security.CurrentAdminProvider` | `AdminVO.getSessionVO(request)` | 주입 방식 변경 | 어노테이션/Provider보다 정적 VO 접근에 가까움 |
| `domain.auth.model.UserAccount` | `com` 계열 `AdminVO` 또는 `UserAdmVO` | 모델명 변경 | 8소스는 관리자 세션/계정 VO 중심 |
| `domain.auth.service.LoginAuditService` | `com` 계열 `ConnectAction` 호출 | 서비스 제거 | 감사 기록은 컨트롤러 또는 공통 로그 헬퍼에서 직접 호출 |
| `domain.auth.service.InitialAdminSeeder` | `kr.core` 초기화 코드 또는 별도 운영 스크립트 | 위치 변경 | 8소스에 동일한 시더 클래스 패턴은 없음 |
| `domain.auth.mapper.UserMapper` | `resources/sql/postgresql/tb_user_adm.xml` | Mapper 인터페이스 제거 | XML namespace 및 호출 방식 변경 동반 |
| `domain.auth.mapper.LoginLogMapper` | `resources/sql/postgresql/tb_conn_act.xml`, `tb_conn_hist.xml` | Mapper 인터페이스 제거 | 접속 행위/이력 테이블 기준 |
| `templates/login.html` | `templates/admmgr/member/login.html` | 템플릿 위치 변경 | 8소스 실제 파일은 `admmgr/member/login.html` |

## 3. Scenario 명명 차이와 매핑

8소스에는 `scenario`와 1:1로 대응되는 업무 패키지가 없으므로, 8소스의 관리자 업무 명명 규칙을 적용한 신규 업무 패키지로 보는 것이 자연스럽다.

| CleverChat 현재 | 8소스 기준으로 맞출 경우 | 변경 성격 | 비고 |
|---|---|---|---|
| `kr.co.cleverchat.domain.scenario` | `kr.admmgr.scenario` | 업무 패키지 평탄화 | `controller/service/mapper/model/dto/event` 레이어 폴더 제거 |
| `ScenarioPageController` | `AdmScenarioController` | 클래스명 변경/병합 | 화면 메서드는 `Adm*Controller` 안으로 정리 |
| `ScenarioApiController` | `AdmScenarioController` 또는 `AdmScenarioApiController` | 클래스명 변경/병합 | 8소스 스타일은 API/Page 분리보다 업무 컨트롤러 중심 |
| `ScenarioCategoryApiController` | `AdmScenarioCategoryController` | 클래스명 변경 | 카테고리 기능을 별도 컨트롤러로 유지할 때 |
| `ScenarioKeywordApiController` | `AdmScenarioKeywordController` | 클래스명 변경 | 키워드/유사어 기능을 별도 컨트롤러로 유지할 때 |
| `ScenarioService`, `ScenarioCategoryService`, `ScenarioKeywordService` | 컨트롤러 + `CDao` 직접 호출 | 구조 변경 | 단순 리네임이 아니라 레이어 제거 |
| `ScenarioGraphValidator` | `com` 계열 `CheckScenarioGraph` 후보 | 유틸 패키지 이동 | 기존 `CheckObject`, `CheckParam` 명명과 맞춤 |
| `ScenarioMatchingCacheInvalidator` | 명시 호출 헬퍼 또는 제거 | 구조 변경 | 8소스에는 이벤트 기반 캐시 무효화 패턴 없음 |
| `model/Scenario*.java` | `ScenarioVO` 또는 `Map<String,Object>` | 모델 단순화 | 8소스는 단일 VO/Map 사용 비중이 큼 |
| `dto/Scenario*Dtos.java` | DTO 제거, `Map<String,Object>` 중심 | DTO 제거 | 타입 안전성 손실과 검증 방식 변경 필요 |
| `mapper/Scenario*Mapper.java` | `resources/sql/postgresql/tb_scenario*.xml` | Mapper 인터페이스 제거 | XML 파일명은 테이블명 기준 |
| `templates/admin/scenarios/list.html` | `templates/admmgr/scenario/scenarioList.html` | 템플릿명 변경 | `{entity}List` 규칙 |
| `templates/admin/scenarios/form.html` | `templates/admmgr/scenario/scenarioRegist.html` | 템플릿명 변경 | `form` 대신 `Regist` |
| `templates/admin/scenarios/detail.html` | `templates/admmgr/scenario/scenarioView.html` | 템플릿명 변경 | `detail` 대신 `View` |
| `templates/admin/scenarios/preview.html` | `templates/admmgr/scenario/scenarioPreviewLayer.html` | 템플릿명 변경 | 팝업/부분 화면은 `Layer` 접미사 |

## 4. Chatbot 명명 차이와 매핑

8소스에는 챗봇 런타임 업무가 명시적으로 존재하지 않는다. 다만 `admmgr/ai`에는 AI 관리 화면(`chat.html`, `crawlList.html`, `modelList.html`, `pipelineList.html`, `vectorDbList.html`)이 있으므로 운영자 AI 관리는 `kr.admmgr.ai`, 관리자 채팅 업무는 `kr.admmgr.chat`으로 분리하는 안을 우선 후보로 둔다. 별도 사용자 root 신설은 이번 기준 구조에 포함하지 않는다.

| CleverChat 현재 | 8소스 기준으로 맞출 경우 | 변경 성격 | 비고 |
|---|---|---|---|
| `kr.co.cleverchat.domain.chatbot` | `kr.admmgr.ai` + `kr.admmgr.chat` | 업무 분리 | 운영 AI 관리와 관리자 채팅 업무 분리 필요 |
| `ChatRuntimeApiController` | `kr.admmgr.chat.AdmChatController` 또는 별도 사용자 root 후속 검토 | 패키지/클래스명 변경 | 사용자 런타임 전용 root는 8소스 기준 구조 밖의 예외 결정 필요 |
| 운영자 채팅/AI 관리 컨트롤러 | `kr.admmgr.ai.AdmAiController` 또는 `AdmChatController` | 클래스명 변경 | 8소스 실제 `AdmAiController.java` 존재 |
| `ChatRuntimeService` | 컨트롤러 + `CDao` 직접 호출 | 구조 변경 | 서비스 레이어 제거 시 영향 큼 |
| `ScenarioMatchingService` | `com` 계열 `ScenarioMatcher` | 유틸 패키지 이동 | 정적 유틸 또는 공통 헬퍼화 |
| `ChatFailureRecorder` | `ConnectAction` 또는 전용 로그 헬퍼 호출 | 구조 변경 | 8소스 감사/행위 기록 스타일과 맞춤 |
| `model/Chat*.java` | `ChatVO` 또는 `Map<String,Object>` | 모델 단순화 | 단일 VO/Map 중심 전환 |
| `dto/ChatRuntimeDtos.java` | DTO 제거, `Map<String,Object>` 중심 | DTO 제거 | API 계약 재검토 필요 |
| `mapper/Chat*Mapper.java` | `resources/sql/postgresql/tb_chat_*.xml` | Mapper 인터페이스 제거 | 현재 테이블 기준 XML로 분리 가능 |

## 5. Common 명명 차이와 매핑

| CleverChat 현재 | 8소스 기준으로 맞출 경우 | 변경 성격 | 비고 |
|---|---|---|---|
| `kr.co.cleverchat.common.api.ApiResponse` | 제거 또는 제한적 JSON 응답 헬퍼 | 구조 변경 | 8소스에는 공통 JSON 래퍼 스타일이 없음 |
| `kr.co.cleverchat.common.api.PageResponse` | `com` 계열 `Paging` | 클래스/패키지명 변경 | 8소스 실제 `Paging.java` 존재 |
| `common.error.GlobalExceptionHandler` | `com` 계열 `GlobalExceptionHandler` | 패키지 이동 | 8소스 위치와 일치 |
| `common.error.ErrorCode` | 제거 또는 메시지 상수화 | 구조 변경 | 8소스에는 enum 기반 에러코드 계층이 없음 |
| `common.error.BusinessException` | 일반 예외 + 전역 핸들러 | 구조 변경 | 커스텀 예외 계층 단순화 |
| `common.audit.Audited` | 제거 | 구조 변경 | 8소스는 AOP 감사 어노테이션 패턴이 아님 |
| `common.audit.AuditAspect` | 제거 | 구조 변경 | 컨트롤러 명시 호출 방식 |
| `common.audit.AuditTrailRecorder` | `com` 계열 `ConnectAction` | 클래스/패키지명 변경 | 8소스 실제 `ConnectAction.java` 존재 |
| `common.audit.AuditLogMapper` | `resources/sql/postgresql/tb_conn_act.xml` | Mapper 인터페이스 제거 | 접속 행위 XML 기준 |
| `config.WebMvcConfig` | `kr.core.WebMvcConfig` | 패키지 이동 | 8소스 실제 `WebMvcConfig.java` 존재 |
| `config.PasswordConfig` | `com` 계열 `PasswordUtil` 중심 | Bean 구성 변경 | 비밀번호 정책/해시 호환성 확인 필요 |
| `web.HomeController` | `kr.core.SessionController` 또는 `MainController` | 패키지/클래스명 변경 | 8소스 실제 `SessionController.java` 존재 |

## 6. 전체 패키지 변환 요약

8소스 실제 구조 기준의 최종 패키지 축은 `kr.core`, `kr.admmgr`, `kr.admmgr.<기능별>`, `com`이다. 아래 표의 `kr.co.cleverchat.domain.*`와 `kr.co.cleverchat.common.*`는 현행 구조 식별을 위한 표기이며, 최종 목표는 `domain/common` 레이어 중심 구조가 아니라 기능별 업무 패키지 중심 구조다.

| CleverChat 현재 | 8소스 기준 후보 |
|---|---|
| `kr.co.cleverchat.CleverChatApplication` | `kr.core.CleverChatApplication` |
| `kr.co.cleverchat.config.*` | `kr.core.*` |
| `kr.co.cleverchat.common.api.*` | `com` 계열 공통 기반 유틸 또는 제거 |
| `kr.co.cleverchat.common.error.*` | `com` 계열 핸들러 |
| `kr.co.cleverchat.common.audit.*` | `com` 계열 로그 또는 제거 |
| `kr.co.cleverchat.domain.auth.*` | `kr.admmgr.member.*`, `kr.admmgr.security.*`, `com` 계열 세션/VO 유틸 |
| `kr.co.cleverchat.domain.scenario.*` | `kr.admmgr.scenario.*` |
| `kr.co.cleverchat.domain.chatbot.*` | `kr.admmgr.ai.*`, `kr.admmgr.chat.*` |
| `kr.co.cleverchat.web.*` | `kr.core.*` |
| `src/main/resources/mapper/<domain>/*.xml` | `src/main/resources/sql/postgresql/tb_*.xml` |
| `src/main/resources/templates/admin/*` | `src/main/resources/templates/admmgr/*` |

## 7. 단순 리네임으로 끝나지 않는 차이

| 항목 | 차이 | 영향도 |
|---|---|---|
| REST JSON 응답 | `@RestController` + `ApiResponse<T>`에서 `.do` 화면/폼 중심으로 전환 필요 | 매우 큼 |
| 서비스 레이어 | `*Service` 계층 제거 여부 결정 필요 | 매우 큼 |
| Mapper 구조 | Mapper 인터페이스 제거와 `CDao(sqlId)` 호출 전환 필요 | 큼 |
| DTO/모델 | DTO/POJO/record에서 VO/Map 중심으로 전환 필요 | 큼 |
| 감사 로그 | AOP/어노테이션에서 컨트롤러 명시 호출로 전환 필요 | 중간 |
| 인증 사용자 주입 | `@CurrentUser`/Provider에서 `AdminVO.getSessionVO(request)`로 전환 필요 | 중간 |
| Java/Spring 버전 | 현재 Spring Boot 3/Jakarta 기반과 8소스의 Java 8/`javax` 계열 차이 | 매우 큼 |

## 8. 권장 리팩토링 순서

### Phase 0. 변경 범위 결정

1. 8소스를 CleverChat 개발의 원본 베이스 소스로 고정한다. CleverChat 현행 구조는 기준이 아니라 조정 대상이다.
2. 8소스 실제 패키지 축은 `kr.core`(공통 코어), `kr.admmgr`(관리자 업무 root), `kr.admmgr.<기능별>`(업무 패키지), `com`(공통/외부 유틸)로 고정한다.
3. CleverChat 현행 `domain/common` 기반 Spring Boot 스타일은 최종 목표 구조가 아니며, 기능별 업무 패키지 중심 구조로 단계 정렬한다.
4. `1.기획/결정사항.md` §11.3 기술적 예외만 현행 유지로 고정한다. Java 17/Spring Boot 3/Jakarta/Thymeleaf/REST JSON/Spring Security 제거/Mapper 점진 이동/JPA 미사용/DTO 부분 유지/`@Audited` 병행이 해당한다.
5. 예외가 아닌 패키지, 컨트롤러, 템플릿, 관리자 레이아웃/CSS/JS, SQL XML 경로, namespace, SQL ID는 8소스 방식으로 정렬한다.

위 항목은 Phase 0의 결정값이다. 후속 PR은 "현행 유지 여부 검토"가 아니라 "8소스 정렬을 기본값으로 두고 §11.3 예외만 제외"하는 방식으로 작성한다.

### Phase 1. 문서/계획 고정

1. scenario를 첫 정렬 도메인으로 확정한다.
2. 관리자 공통 레이아웃, `admmgr/scenario` 화면, `AdmScenario*Controller`, `kr.admmgr.scenario`, `sql/postgresql/tb_scenario*.xml` 정렬 범위를 Phase 1 작업지시서에 고정한다.
3. 코드 리네임은 Phase 1 실행 PR에서 수행하고, 본 분석 문서는 기준과 범위만 제공한다.

### Step A. 낮은 위험도 리네임

1. 템플릿 경로/파일명부터 `templates/admmgr/<domain>/<entity>{List|Regist|View|Layer}.html` 규칙으로 맞춘다.
2. 컨트롤러 반환 뷰 문자열을 함께 수정한다.
3. `GlobalExceptionHandler`, `WebMvcConfig`, `HomeController`처럼 의존성이 낮은 공통 클래스의 위치와 명칭을 조정한다.

### Step B. 컨트롤러 명명 정렬

1. `LoginController`를 `AdmLoginController`로 정렬한다.
2. `Scenario*Controller`를 `AdmScenario*Controller`로 정렬한다.
3. `ChatRuntimeApiController`는 사용자 런타임이면 `FrontChatController`, 운영자 기능이면 `AdmChatController` 또는 `AdmAiController`로 분리한다.

### Step C. 패키지 평탄화

1. `domain.auth`를 `kr.admmgr.member`, `kr.admmgr.security`로 분리한다.
2. `domain.scenario`를 `kr.admmgr.scenario`로 평탄화한다.
3. `domain.chatbot`은 관리자 기능 기준으로 `kr.admmgr.ai`와 `kr.admmgr.chat` 역할을 나눈다. 별도 사용자 root 신설은 이번 기준 구조에 포함하지 않는다.

### Step D. 구조 전환

1. 서비스 레이어 제거 여부를 결정한 뒤 `CDao` 또는 현행 Service 유지 중 하나로 일관화한다.
2. Mapper 인터페이스 제거 시 XML namespace, `mapper-locations`, 호출부를 한 PR 안에서 동시에 바꾼다.
3. DTO/모델 단순화는 API 계약과 화면 검증 방식이 흔들리므로 마지막에 수행한다.

## 9. `templates/admin` 폴더 구조 실제 검증

Phase 0에서는 템플릿 리네임을 수행하지 않고, 현재 구조와 8소스 기준 구조의 차이만 확정한다.

| 구분 | 실측 결과 | 판단 |
|---|---|---|
| CleverChat | `src/main/resources/templates/admin/scenarios/` 1개 업무 폴더만 존재한다. `admin` 하위에 `chatbot`, `auth` 등 다른 업무 폴더는 없다. | 현재 불일치 대상은 복수형 `scenarios` 1개로 제한된다. |
| CleverChat 관리자 루트 | `src/main/resources/templates/admin/index.html`만 루트 직속으로 존재한다. | `login.html`은 현재 `templates/admin` 하위에 없다. |
| 8소스 | `src/main/resources/templates/admmgr/<member,manage,ai,board,security,consult,exportKor,exportUsa,exportV2,exportEtc,print,dashboard>` 구조다. | 업무 폴더명은 모두 단수형 또는 업무명 고유명으로 관리된다. |
| 8소스 로그인 | `src/main/resources/templates/admmgr/member/login.html` | 로그인은 `member` 업무 폴더에 위치한다. |

현재 CleverChat 템플릿 매핑 후보는 다음과 같다.

| CleverChat 현재 | 8소스 기준 후보 | 비고 |
|---|---|---|
| `templates/admin/index.html` | `templates/admmgr/dashboard/dashboard.html` 또는 `templates/admmgr/index.html` | 관리자 첫 화면 정책 결정 필요 |
| `templates/admin/scenarios/list.html` | `templates/admmgr/scenario/scenarioList.html` | `scenarios` 복수형을 `scenario` 단수형 업무 폴더로 정렬 |
| `templates/admin/scenarios/form.html` | `templates/admmgr/scenario/scenarioRegist.html` | `form` 대신 `Regist` |
| `templates/admin/scenarios/detail.html` | `templates/admmgr/scenario/scenarioView.html` | `detail` 대신 `View` |
| `templates/admin/scenarios/preview.html` | `templates/admmgr/scenario/scenarioPreviewLayer.html` | 미리보기/부분 화면은 `Layer` 후보 |

정렬 부담은 아직 낮다. 현 시점에는 `templates/admin/scenarios` 1개만 복수형이므로, M3/M4에서 `chatbot` 또는 사용자 채팅 화면 폴더가 추가되기 전에 단수/복수 정책을 정하면 후속 리네임 범위를 줄일 수 있다.

## 10. 8소스 액션명 컨벤션

8소스는 관리자 화면 URL, 컨트롤러 메서드, 템플릿 파일명에 업무 엔티티와 액션 접미사를 붙이는 패턴이 강하다. 예시는 `AdmBoardMngController`의 `mngList`, `mngView`, `mngRegist`, `mngRegistProc`, `mngDeleteProc`, `boardMngLayer`와 `templates/admmgr/manage/boardMngList.html`, `boardMngRegistLayer.html`이다.

| 접미사 | 의미 | 8소스 사용 방식 | CleverChat 대응 후보 |
|---|---|---|---|
| `*List` | 목록 화면 | `/admmgr/<업무>/<entity>List.do`, `<entity>List.html` | `GET /admin/scenarios`, `list.html` |
| `*View` | 상세 화면 | `/admmgr/<업무>/<entity>View.do`, `<entity>View.html` | `GET /admin/scenarios/{id}`, `detail.html` |
| `*Regist` | 등록 또는 등록/수정 통합 화면 | `/admmgr/<업무>/<entity>Regist.do`, `<entity>Regist.html` 또는 `*RegistLayer.html` | `GET /admin/scenarios/new`, `GET /admin/scenarios/{id}/edit`, `form.html` |
| `*Modify` | 수정 화면 또는 수정 전용 액션 | 8소스에는 `Regist` 통합 사례가 더 뚜렷하므로 신규 도입 시 범위 결정 필요 | 현행 edit 화면을 분리 유지할 경우 후보 |
| `*Remove` | 삭제 진입 또는 삭제 의미의 업무 액션 | 8소스 실측에는 `DeleteProc` 사례도 있으므로 `Remove`/`Delete` 용어 합의 필요 | `DELETE /admin/api/scenarios/{id}` |
| `*Proc` | 폼 처리, 저장, 삭제 등 POST 처리 | `/admmgr/<업무>/<entity>RegistProc.do`, `/admmgr/<업무>/<entity>DeleteProc.do` | `POST /admin/api/scenarios`, `PUT/PATCH`, `DELETE` |
| `*Layer` | 팝업, 모달, 부분 화면 | `/admmgr/<업무>/<entity>Layer.do`, `<entity>Layer.html`, `*RegistLayer.html` | `preview.html` 또는 fragment 모달 |

권장 표기 패턴은 다음과 같이 정리할 수 있다.

| 항목 | 8소스 기준 패턴 |
|---|---|
| URL | `/admmgr/<업무>/<entity><Action>.do` |
| 컨트롤러 메서드 | `<entity><Action>()` 또는 업무 축약어 + `<Action>()` |
| 템플릿 | `admmgr/<업무>/<entity><Action>.html` |
| 처리 URL | `/admmgr/<업무>/<entity><Action>Proc.do` |
| SQL ID | `select_list_cnt`, `select_list`, `select_view`, `insert`, `update`, `delete`, `update_<verb>` |

CleverChat 현행 REST 라우트와의 대응은 다음과 같다. 이 표는 명명 분석용이며, Phase 0에서 URL 또는 컨트롤러를 변경하지 않는다.

| CleverChat 현재 | 8소스식 명명 후보 | 결정 포인트 |
|---|---|---|
| `GET /admin/scenarios` | `scenarioList` | 화면명만 맞출지 URL까지 `.do`로 바꿀지 결정 |
| `GET /admin/scenarios/new` | `scenarioRegist` | 등록/수정 화면 통합 여부 결정 |
| `GET /admin/scenarios/{id}` | `scenarioView` | 상세 화면과 API 상세 응답 분리 여부 결정 |
| `GET /admin/scenarios/{id}/edit` | `scenarioRegist` 또는 `scenarioModify` | 8소스식 통합이면 `Regist`, 분리 유지면 `Modify` |
| `POST /admin/api/scenarios` | `scenarioRegistProc` | REST JSON 유지 여부와 함께 결정 |
| `PUT/PATCH /admin/api/scenarios/{id}` | `scenarioRegistProc` 또는 `scenarioModifyProc` | 저장 처리 통합 여부 결정 |
| `DELETE /admin/api/scenarios/{id}` | `scenarioRemoveProc` 또는 `scenarioDeleteProc` | `Remove`/`Delete` 용어 합의 필요 |
| `preview.html` | `scenarioPreviewLayer` | `Layer` URL 분리 또는 현행 fragment 유지 결정 |

따라서 액션명 정렬은 단순 파일명 변경으로 끝나지 않는다. `REST JSON 응답`, `Regist` 통합, `Layer` 화면 분리, 삭제 접미사(`Remove`/`Delete`)가 함께 결정되어야 실제 리네임 범위가 확정된다.

## 11. chatbot 신규 SQL/XML 명명 규칙

chatbot 신규 SQL/XML은 8소스의 테이블명 중심 SQL 파일 규칙을 따른다. 기존 Mapper 인터페이스와 XML 이동은 chatbot 정렬 PR에서 영향 범위를 보고 점진 수행하되, 최종 기준은 `sql/postgresql/tb_chat_*.xml` + `tb_chat_*` namespace다.

| 항목 | 신규 규칙 |
|---|---|
| XML 경로 | `src/main/resources/sql/postgresql/tb_chat_<엔티티>.xml` |
| namespace | 파일명과 동일하게 `tb_chat_<엔티티>` |
| 테이블명 | XML 파일명, namespace와 동일하게 `tb_chat_<엔티티>` |
| SQL ID 기본형 | `select_list_cnt`, `select_list`, `select_view`, `insert`, `update`, `delete` |
| SQL ID 확장형 | 상태 변경 등 업무 동사는 `update_<verb>`, 이력 추가는 `insert_<verb>`처럼 접미사를 붙인다. |
| 공유 기준 | 운영자 AI(`admmgr.ai`)와 관리자 채팅(`admmgr.chat`)이 같은 테이블을 쓰는 경우 같은 `tb_chat_*` XML을 공유한다. 패키지별 XML 분리는 하지 않는다. |

현재 CleverChat 매퍼와 신규 명명 후보는 다음과 같다.

| 현재 XML | 신규 SQL/XML 후보 | 비고 |
|---|---|---|
| `mapper/chatbot/ChatSessionMapper.xml` | `sql/postgresql/tb_chat_session.xml` | 대화 세션 |
| `mapper/chatbot/ChatMessageMapper.xml` | `sql/postgresql/tb_chat_message.xml` | 대화 메시지 |
| `mapper/chatbot/ChatFailureMapper.xml` | `sql/postgresql/tb_chat_failure.xml` | 실패/미매칭 로그 |
| `mapper/chatbot/ChatRecommendationMapper.xml` | `sql/postgresql/tb_chat_recommendation.xml` | 추천/후속 질의 |

적용 원칙은 다음과 같다.

1. 신규 chatbot 테이블과 SQL은 `tb_chat_<엔티티>`를 기본값으로 작성한다.
2. 기존 `mapper/chatbot/*Mapper.xml`은 본 분석 문서에서 이동하거나 리네임하지 않는다. 실제 이동은 chatbot 정렬 PR에서 수행한다.
3. Mapper 인터페이스를 유지하는 동안 호출 호환 때문에 기존 namespace가 임시로 남을 수 있다. 단, 새로 만드는 SQL 파일과 최종 전환 기준은 테이블명 namespace다.
4. `admmgr.ai` 운영자 기능과 `admmgr.chat` 관리자 채팅 기능이 같은 데이터를 조회하면 XML을 패키지별로 나누지 않고 SQL ID 또는 파라미터로 조회 목적을 구분한다.
5. `message/msg`, `failure/fail`, `recommendation/recommend` 약어는 ERD 확정 후 하나로 고정한다. Phase 0 기준 문서에서는 풀네임(`message`, `failure`, `recommendation`)을 우선 후보로 둔다.

### 11.A 8.소스 실제 SQL ID 샘플 부록

신규 chatbot SQL/XML을 작성할 때는 아래 8소스 원본 XML의 namespace와 SQL ID 패턴을 우선 참고한다. 샘플 기준 경로는 `8.소스/OverseasNPP_20260511/src/main/resources/sql/postgresql/tb_*.xml`이며, `target/classes` 산출물은 제외한다.

| 분류 | 실제 파일 | namespace | SQL ID 샘플 | chatbot 신규 SQL 적용 기준 |
|---|---|---|---|---|
| 기본 CRUD + 목록/상세 | `tb_board_mng.xml` | `tb_board_mng` | `select_list_cnt`, `select_list`, `select_all`, `select_view`, `insert`, `update`, `delete` | 기본형은 `select_list_cnt`, `select_list`, `select_view`, `insert`, `update`, `delete`를 먼저 둔다. |
| 관리자 계정 확장 액션 | `tb_user_adm.xml` | `tb_user_adm` | `select_list_cnt`, `select_list`, `select_view`, `insert`, `update`, `update_merge`, `update_login_dt`, `update_aprv_yn`, `delete`, `delete_physical`, `update_reset_pw`, `update_change_pw`, `update_lock_on`, `update_security_agree`, `select_search_list_cnt`, `select_search_list` | 상태 변경/업무 동작은 `update_<verb>`로 확장하고, 물리 삭제는 `delete_physical`로 분리한다. |
| 접속 행위 로그 | `tb_conn_act.xml` | `tb_conn_act` | `select_list_cnt`, `select_list`, `insert` | 이력/로그 적재 테이블은 조회와 적재 중심으로 두고 불필요한 `update`, `delete`를 만들지 않는다. |
| 접속 이력 로그 | `tb_conn_hist.xml` | `tb_conn_hist` | `select_list_cnt`, `select_list`, `insert` | chatbot 대화/실패 이력도 이력성 테이블이면 `insert` + `select_list*`를 기본으로 한다. |
| 메뉴성/단순 조회 | `tb_menu.xml` | `tb_menu` | `select_list`, `select_all_list`, `select_pk`, `select_menu_url_cnt`, `insert`, `update`, `update_nm`, `update_sort`, `delete`, `select_menu_list` | 보조 조회는 `select_<purpose>` 형태를 허용하되 기본 목록/상세 ID보다 뒤에 둔다. |
| 권한성/코드성 관리 | `tb_auth.xml` | `tb_auth` | `select_list`, `select_pk`, `insert`, `update_nm`, `update_rank`, `update_sort`, `update_dc`, `delete`, `delete_physical` | 코드성 속성 변경은 `update_<column-or-purpose>` 형태를 사용한다. |

위 샘플에서 확인되는 공통 원칙은 다음과 같다.

1. XML 파일명과 namespace는 테이블명과 1:1로 맞춘다. 예: `tb_user_adm.xml` + `namespace="tb_user_adm"`.
2. 목록 수/목록/상세/등록/수정/삭제 기본형은 `select_list_cnt`, `select_list`, `select_view`, `insert`, `update`, `delete`를 우선한다.
3. 업무 동작이 필요한 경우 `update_<verb>` 또는 `update_<purpose>`처럼 기본 `update` 뒤에 접미사를 붙인다.
4. 논리 삭제 관용은 `<update id="delete">`로 유지하고, 물리 삭제는 `<delete id="delete_physical">`로 분리한다.
5. 이력/로그성 테이블은 `insert`와 `select_list*` 중심으로 작성하고 수정/삭제 SQL은 업무상 필요가 있을 때만 추가한다.
6. 검색 전용 목록은 `select_search_list_cnt`, `select_search_list`처럼 `search` 목적어를 명시한다.
7. 신규 chatbot SQL은 `tb_chat_<엔티티>` namespace를 쓰며, 기존 Mapper XML은 chatbot 정렬 PR에서 같은 기준으로 이동한다.

## 12. Phase 0 결정 회의 안건

Phase 0 회의는 아래 결정 축을 기준으로 진행한다. 특히 12.1의 우선 결정 3건은 후속 Phase의 PR 범위와 검증 기준을 잠그는 입력값이므로 회의 초반 30분 안에 먼저 확정한다.

### 12.1 우선 결정 3건

| 우선 | 안건 | 선택지 | 권장안 | 미결 시 영향 | 결정 이유 |
|---|---|---|---|---|---|
| ★1 | `templates/admin`에서 `templates/admmgr`로 관리자 화면 기준 전환 | (A) 현행 `admin/scenarios` 유지 / (B) `admmgr/scenario` 8소스 정렬 | (B) `admmgr/scenario` 8소스 정렬 | Phase 1 scenario 화면 PR 범위와 잔존 참조 검사식이 확정되지 않는다. | 8소스가 원본 베이스이므로 관리자 화면 루트와 업무 폴더는 `admmgr/<업무>`를 따른다. |
| ★2 | 8소스 액션명(`*List/*Regist/*View/*Delete/*Proc/*Layer`) 채택 범위와 REST JSON 유지 여부 | (A) 전체 `.do` 전환 / (B) 화면 파일명·뷰명·컨트롤러 메서드명은 8소스 정렬 + REST JSON 유지 / (C) 채택 보류 | (B) 8소스 명명 정렬 + REST JSON 유지 | URL, 컨트롤러 메서드, 테스트 기대값, 화면 파일명 변경 범위가 모두 흔들린다. | REST JSON은 결정사항.md §11.3 기술적 예외다. 예외가 아닌 화면/액션/메서드 명명은 8소스 기준으로 정렬한다. |
| ★3 | SQL XML 경로/namespace/SQL ID 규칙 | (A) Mapper namespace 유지 / (B) `sql/postgresql/tb_*.xml` + `tb_*` namespace + 8소스 SQL ID 정렬 / (C) 도메인별 보류 | (B) 8소스 SQL XML 규칙 채택. Mapper 인터페이스 호출 전환은 점진 수행 | M3/M4 이후 신규 SQL 파일명, namespace, SQL ID 기준이 불명확해진다. | SQL 작성 기준은 8소스와 맞추고, Mapper 인터페이스 제거 여부만 §11.3 예외에 따라 도메인별 PR에서 점진 처리한다. |

우선 결정 ★2의 권장안 (B)를 채택하면 접미사별 적용 범위는 아래처럼 정리한다.

| 8소스 접미사 | Phase 1 적용 | 후속 검토 | REST JSON 영향 |
|---|---|---|---|
| `*List` | scenario 목록 템플릿명, 뷰 반환 문자열, 화면 메서드명에 적용 | URL `.do` 전환 여부 | 유지 |
| `*Regist` | scenario 등록/수정 템플릿명, 뷰 반환 문자열, 화면 메서드명에 적용 | 등록/수정 통합 UX 세부 정책 | 유지 |
| `*View` | scenario 상세 템플릿명, 뷰 반환 문자열, 화면 메서드명에 적용 | 상세 URL `.do` 전환 여부 | 유지 |
| `*Delete` | 삭제 처리 메서드명 후보에 적용 | REST DELETE와 `.do` Proc 병행 여부 | 유지 |
| `*Proc` | 저장/삭제 처리 메서드명 후보에 적용 | `.do` 폼 전환 시 URL까지 확대 | 유지 |
| `*Layer` | 미리보기/부분 화면 템플릿명과 뷰명에 적용 | Layer 전용 URL 분리 여부 | 유지 |

### 12.2 전체 결정 표

| # | 결정 사항 | 선택지 | 권장 |
|---|---|---|---|
| 1 | Java/Spring 기준 | 현행 유지 / 8소스 기준 다운그레이드 | 기술 예외로 Java 17/Spring Boot 3 유지 |
| 2 | API 스타일 | REST JSON 유지 / `.do` 폼 중심 전환 | 기술 예외로 REST JSON 유지. 화면/액션명은 8소스식 정렬 |
| 3 | Mapper 방식 | Mapper 인터페이스 점진 유지 / `CDao` XML 직접 호출 일괄 전환 | SQL XML 경로/namespace/ID는 8소스 정렬, Mapper 인터페이스 제거는 도메인별 점진 전환 |
| 4 | Chatbot 패키지 | `admmgr.ai` 통합 / `admmgr.chat` 분리 / 별도 사용자 root 신설 | 운영자/관리자 기능은 `kr.admmgr.ai` + `kr.admmgr.chat` 기준. 별도 사용자 root는 이번 8소스 기준 구조에 포함하지 않고 후속 예외 검토 |
| 5 | 적용 단위 | 전체 일괄 / 도메인별 순차 | scenario → auth → chatbot → search 순차 |
| ★6 | 관리자 템플릿 루트/업무 폴더 | 현행 `templates/admin/scenarios` 유지 / `templates/admmgr/scenario` 정렬 | Phase 1에서 `templates/admmgr/scenario` 정렬 |
| ★7 | 액션명 채택 범위 | 명칭만 정렬 / URL까지 `.do` 전환 / 채택 안 함 | 화면 파일명, 뷰명, 컨트롤러 메서드명은 8소스식으로 정렬하고 REST JSON 유지 |
| 8 | 등록/수정 화면 | `Regist` 통합 / `Regist` + `Modify` 분리 | 8소스 기준으로 `Regist` 통합 우선 |
| 9 | 부분 화면 | `*Layer` URL 분리 / 현행 fragment 모달 유지 | 템플릿명은 `Layer` 채택, URL 분리는 REST JSON 예외와 함께 후속 검토 |
| ★10 | SQL namespace | `tb_<엔티티>` 채택 / Mapper 인터페이스 namespace 유지 | 신규/정렬 SQL은 `tb_*`, 기존 Mapper 인터페이스 호출은 점진 전환 |
| 11 | chatbot 테이블 약어 | 풀네임 / 축약명 | ERD 확정 전까지 풀네임 우선 |
| 12 | 삭제 접미사 통일 | `*Remove`/`remove` 유지 / `*Delete`·`*DeleteProc` 통일 / 화면은 `Remove`, SQL은 `delete` 혼용 | `*Delete`·`*DeleteProc`로 통일. 8소스 SQL ID도 `delete`, `delete_physical` 어휘를 쓰므로 컨트롤러/URL/메서드/SQL ID 일치성을 높인다. |
| 13 | 운영자 채팅 패키지 | `admmgr.ai` 통합 / `admmgr.chat` 신설 / 별도 사용자 root 신설 | `admmgr.chat` 신설 분리. `admmgr.ai`는 모델·파이프라인 책임, `admmgr.chat`은 관리자 채팅 업무 책임으로 둔다. 별도 사용자 root는 §11.3 예외 수준의 후속 결정 없이는 도입하지 않는다. |

### 12.3 결정 의존 관계

1. 결정 ★6이 확정되어야 Phase 1의 scenario 기준 폴더/화면/명명/디자인 정렬 PR 범위가 잠긴다.
2. 결정 ★7은 #2, #8, #9, #12와 함께 봐야 한다. REST JSON은 기술 예외로 유지하지만, 템플릿 경로/뷰 반환 문자열/컨트롤러 메서드명은 8소스 액션명으로 정렬한다.
3. 결정 ★10은 M3/M4 이후 신규 SQL 작성 위치와 namespace 기준을 정한다. 기존 `mapper/chatbot/*Mapper.xml` 이동은 도메인별 SQL 전환 PR에서 점진 수행한다.
4. 결정 #12는 컨트롤러 메서드명, URL, SQL ID 문서화에 영향을 주므로 Step B 컨트롤러 명명 정렬 전에 확정한다.
5. 결정 #13은 운영자 채팅 화면이 추가될 때 패키지와 URL 베이스를 정하는 입력값이므로 Step C 패키지 평탄화 전에 확정한다.

## 13. 결론

8소스 기준과 CleverChat의 가장 큰 차이는 단순 명명보다 아키텍처 방향이다. 8소스 실제 구조는 `kr.core` 공통 코어, `kr.admmgr` 관리자 업무 root, `kr.admmgr.<기능별>` 업무 패키지, `com` 공통/외부 유틸 축을 사용하고, 컨트롤러와 XML SQL 중심으로 동작한다. CleverChat은 현재 `domain/common` 기반 Spring Boot 스타일의 도메인별 레이어 구조, 서비스 계층, Mapper 인터페이스, DTO/모델, REST 응답 래퍼를 사용하지만, 이 구조는 최종 목표가 아니라 단계 정렬 대상이다.

방향 정정 이후에는 8소스가 원본 베이스 소스이며 CleverChat 현행 구조는 조정 대상이다. 따라서 8소스 정렬 작업은 다음 두 갈래로 분리하되, 기술적 예외는 `1.기획/결정사항.md` §11.3에 명시된 항목만 인정한다.

1. 명명 정렬: 패키지명은 `kr.core`, `kr.admmgr`, `kr.admmgr.<기능별>`, `com` 축으로 정렬하고, 컨트롤러명, 화면 파일명, 액션명, 관리자 UI 레이아웃/CSS/JS, XML 파일 위치, namespace, SQL ID를 8소스 기준으로 정렬한다.
2. 구조 전환: 서비스/Mapper/DTO/REST 응답 방식 중 §11.3 예외가 아닌 항목을 도메인별 PR로 8소스 스타일에 맞춘다.

실제 구현은 scenario 도메인부터 시작한다. 이후 auth, chatbot, search 순서로 같은 원칙을 반복 적용한다.
