# Phase 1.A 관리자 레이아웃/템플릿 정렬 작업지시서

> 작성일: 2026-05-13  
> 기준 소스: `8.소스/OverseasNPP_20260511`  
> 대상 프로젝트: `3.개발/cleverchat`  
> 작업 원칙: 이 문서는 작업지시서이다. 실제 리네임, 이동, 코드 수정은 수행하지 않는다.

## 1. 핵심 판단

Phase 1.A의 목표는 CleverChat 관리자 시나리오 화면을 8소스의 `kr.admmgr` 화면 구조와 명명 규칙에 맞춰 정렬하는 것이다.

이번 Phase의 범위는 다음으로 제한한다.

| 구분 | Phase 1.A 포함 | Phase 1.A 제외 |
|---|---|---|
| 템플릿 경로 | `templates/admin/scenarios/*.html`을 `templates/admmgr/scenario/*.html` 기준으로 정렬 | 관리자 URL 변경 |
| 화면 파일명 | 8소스의 `List`, `Regist`, `View`, `Layer` 명명 규칙 적용 | 도메인/서비스/DTO 이름 변경 |
| 컨트롤러 view 반환 | `AdmScenarioController`의 반환 view 문자열만 새 경로로 매핑 | `@RequestMapping("/admin/scenarios")` 변경 |
| 레이아웃 | 관리자 공통 레이아웃 적용 범위 정의 | 로그인/인증/권한 체계 전환 |
| 정적 리소스 | `/asset/admmgr/style2/...` URL 체계 정합성 점검 | 리소스 대량 복사/교체 |

따라서 `kr.co.cleverchat.domain.scenario` 패키지, `/admin/scenarios` URL, DB 스키마, MyBatis/JPA 정책, 인증/권한 정책은 변경하지 않는다. 이들은 후속 Phase에서 별도 작업으로 다룬다.

## 2. 8소스 기준

8소스의 관리자 화면은 `kr.admmgr` 패키지와 `templates/admmgr` 화면 경로를 기준으로 맞물린다. CleverChat은 패키지를 즉시 바꾸지 않되, 화면 경로와 파일명은 아래 규칙을 우선 적용한다.

| 항목 | 8소스 기준 | CleverChat Phase 1.A 적용 |
|---|---|---|
| 관리자 템플릿 루트 | `templates/admmgr` | `src/main/resources/templates/admmgr` |
| 업무 디렉터리 | `admmgr/<업무>` | `admmgr/scenario` |
| 목록 화면 | `<entity>List.html` | `scenarioList.html` |
| 등록/수정 화면 | `<entity>Regist.html` | `scenarioRegist.html` |
| 상세 화면 | `<entity>View.html` | `scenarioView.html` |
| 팝업/레이어 화면 | `<entity>Layer.html`, `<entity>SearchLayer.html` | `scenarioPreviewLayer.html` |
| 관리자 공통 레이아웃 | `layout/admin-layout` | `layout/admin-layout` 또는 Phase 1.A-① 대체 골격 |
| 공통 fragment | `common/fragments.html` | `common/fragments.html` |
| 관리자 정적 URL | `/asset/admmgr/style2/...` | `/asset/admmgr/style2/...` |

화면명은 기능명을 앞에 두고 화면 성격을 뒤에 붙인다. 예: `menuList.html`, `exportRegist.html`, `qnaView.html`, `ipAccRegistLayer.html`.

### 2.1 Phase 1.A 구현 범위 분리

Phase 1.A는 한 번에 모두 구현하지 않고 아래 세부 작업으로 분리한다.

| 구분 | 작업명 | 구현 범위 | 이번 PR 포함 여부 |
|---|---|---|---|
| 1.A-① | 즉시 정렬 PR | 시나리오 관리자 view 반환 문자열과 템플릿 경로/파일명 정렬 | 포함 |
| 1.A-② | 공통 레이아웃 fragment | `layout/admin-layout`, `common/fragments.html`, sidebar/header/footer/navigation 적용 정리 | 별도 PR |
| 1.A-③ | 정적 리소스 인벤토리 | `/asset/admmgr/style2/...` 대상 CSS/JS/image/font 목록화 및 복사 필요성 판단 | 별도 작업지시서 |

1.A-③ 정적 리소스 인벤토리는 본 작업에서 파일 복사나 리소스 교체를 수행하지 않는다. 필요한 리소스 목록, 출처, 목표 경로, 사용 화면만 별도 작업지시서로 분리해 확정한 뒤 후속 작업에서 처리한다.

## 3. AdmScenarioController 반환 뷰 매핑

대상 파일: `3.개발/cleverchat/src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java`

현재 `@RequestMapping("/admin/scenarios")`는 유지한다. 변경 대상은 `return "..."`으로 반환하는 Thymeleaf view 경로뿐이다.

| 현재 라인 | 메서드 | 현재 반환 view | Phase 1.A 목표 view | 목표 파일 |
|---:|---|---|---|---|
| 46 | `scenarioList` | `admin/scenarios/list` | `admmgr/scenario/scenarioList` | `templates/admmgr/scenario/scenarioList.html` |
| 53 | `scenarioRegist` | `admin/scenarios/form` | `admmgr/scenario/scenarioRegist` | `templates/admmgr/scenario/scenarioRegist.html` |
| 60 | `scenarioRegistProc` 검증 실패 | `admin/scenarios/form` | `admmgr/scenario/scenarioRegist` | `templates/admmgr/scenario/scenarioRegist.html` |
| 71 | `scenarioView` | `admin/scenarios/detail` | `admmgr/scenario/scenarioView` | `templates/admmgr/scenario/scenarioView.html` |
| 84 | `scenarioModify` | `admin/scenarios/form` | `admmgr/scenario/scenarioRegist` | `templates/admmgr/scenario/scenarioRegist.html` |
| 92 | `scenarioModifyProc` 검증 실패 | `admin/scenarios/form` | `admmgr/scenario/scenarioRegist` | `templates/admmgr/scenario/scenarioRegist.html` |
| 135 | `scenarioPreviewLayer` | `admin/scenarios/preview` | `admmgr/scenario/scenarioPreviewLayer` | `templates/admmgr/scenario/scenarioPreviewLayer.html` |

등록과 수정은 8소스의 `Regist` 단일 화면 분기 규칙을 따른다.

| 상황 | 판단 조건 | 화면 |
|---|---|---|
| 신규 등록 | `scenario == null` 또는 id 없음 | `scenarioRegist.html`에서 등록 모드 |
| 수정 | `scenario != null` 또는 id 있음 | `scenarioRegist.html`에서 수정 모드 |
| 검증 실패 | `BindingResult.hasErrors()` | 같은 `scenarioRegist.html` 재표시 |

`redirect:/admin/scenarios/...` 반환은 view 경로가 아니므로 Phase 1.A에서 변경하지 않는다.

## 4. 공통 관리자 레이아웃 적용 범위

8소스의 일반 관리자 화면은 `layout:decorate="~{layout/admin-layout}"`를 사용한다. Phase 1.A에서는 다음 기준으로 적용한다.

| 대상 | 적용 여부 | 기준 |
|---|---|---|
| `scenarioList.html` | 적용 | 목록 화면은 sidebar, top header, breadcrumb가 필요한 관리자 본문이다. |
| `scenarioRegist.html` | 적용 | 등록/수정 화면은 관리자 본문이다. |
| `scenarioView.html` | 적용 | 상세 화면은 관리자 본문이다. |
| `scenarioPreviewLayer.html` | 원칙적으로 미적용 | `*Layer.html` 계열은 팝업/레이어/부분 화면이므로 sidebar/header를 중복 적용하지 않는다. |
| `admin/index.html` | Phase 1.A 직접 대상 아님 | 시나리오 템플릿 정렬 후 별도 관리자 홈 정렬 Phase에서 검토한다. |

적용 방식은 두 단계로 나눈다.

| 단계 | 내용 | 조건 |
|---|---|---|
| 1.A-① 즉시 정렬 | 파일 경로/파일명/view 반환 문자열을 정렬하고, 레이아웃은 현재 의존성으로 가능한 최소 골격을 적용한다. | 새 의존성 없이 처리 |
| 1.A-② 레이아웃 완전 적용 | 8소스와 같은 `layout/admin-layout`, `layout:fragment` 구조로 통합한다. | Thymeleaf Layout Dialect 의존성 도입 여부 PM 승인 필요 |

현재 프로젝트에 `thymeleaf-layout-dialect`가 없으면 `layout:*` 속성을 그대로 도입하지 않는다. 이 경우 1.A-①에서는 `th:replace` 기반 header/footer/sidebar fragment 또는 단순 공통 골격으로 대체하고, 의존성 추가는 1.A-②에서 별도 PR로 분리한다.

## 5. Thymeleaf fragment 구조

8소스의 공통 fragment 기준 파일은 `templates/common/fragments.html`이다. Phase 1.A에서 CleverChat에 적용할 구조는 다음과 같다.

| fragment | 용도 | 적용 기준 |
|---|---|---|
| `amt(val)` | 금액/숫자 포맷 | 시나리오 화면에는 즉시 필수 아님 |
| `help(text)` | 도움말 툴팁 | 필드 설명이 필요한 경우만 사용 |
| `authBtn(menuUrl, action, html)` | 권한 기반 버튼 표시 | 메뉴/권한 Phase 이후 적용 |
| `adminHeader` | 관리자 상단 영역 | 1.A-①에서 필요 시 신규 정의 |
| `adminSidebar` | 관리자 좌측 메뉴 | 1.A-①에서 필요 시 신규 정의 |
| `adminFooter` | 관리자 하단 영역 | 1.A-①에서 필요 시 신규 정의 |
| `adminNavigation` | breadcrumb/navigation | 1.A-①에서 필요 시 신규 정의 |

1.A-①의 HTML 기본 골격은 다음 형태를 목표로 한다. Layout Dialect를 사용하지 않는 경우의 기준이다.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="ko">
<head>
    <meta charset="UTF-8">
    <title>시나리오 관리</title>
    <link rel="stylesheet" th:href="@{/asset/admmgr/style2/css/admin-layout.css}">
    <link rel="stylesheet" th:href="@{/asset/admmgr/style2/css/sub.css}">
</head>
<body>
    <div class="admin-layout">
        <th:block th:replace="~{common/fragments :: adminSidebar}"></th:block>
        <div class="admin-main">
            <th:block th:replace="~{common/fragments :: adminHeader}"></th:block>
            <main class="content-area">
                <!-- page content -->
            </main>
            <th:block th:replace="~{common/fragments :: adminFooter}"></th:block>
        </div>
    </div>
</body>
</html>
```

1.A-②에서 Layout Dialect가 승인되면 8소스와 동일하게 아래 구조로 전환한다.

```html
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/admin-layout}" lang="ko">
<head>
    <th:block layout:fragment="styles">
        <link rel="stylesheet" th:href="@{/asset/admmgr/style2/css/sub.css}">
    </th:block>
</head>
<body>
    <main layout:fragment="content">
        <!-- page content -->
    </main>
    <th:block layout:fragment="scripts">
        <!-- page scripts -->
    </th:block>
</body>
</html>
```

## 6. 정적 리소스 경로 정합

8소스는 정적 파일을 `src/main/webapp/asset/...` 아래에 두고 URL은 `/asset/...`로 참조한다. Spring Boot 기반 CleverChat은 정적 파일 위치를 `src/main/resources/static/asset/...`로 둔다.

| 리소스 | 8소스 물리 경로 | CleverChat 목표 물리 경로 | URL |
|---|---|---|---|
| 관리자 레이아웃 CSS | `src/main/webapp/asset/admmgr/style2/css/admin-layout.css` | `src/main/resources/static/asset/admmgr/style2/css/admin-layout.css` | `/asset/admmgr/style2/css/admin-layout.css` |
| 관리자 서브 CSS | `src/main/webapp/asset/admmgr/style2/css/sub.css` | `src/main/resources/static/asset/admmgr/style2/css/sub.css` | `/asset/admmgr/style2/css/sub.css` |
| 관리자 공통 JS | `src/main/webapp/asset/admmgr/style2/js/ADM.Common.js` | `src/main/resources/static/asset/admmgr/style2/js/ADM.Common.js` | `/asset/admmgr/style2/js/ADM.Common.js` |
| 관리자 이미지 | `src/main/webapp/asset/admmgr/style2/images/...` | `src/main/resources/static/asset/admmgr/style2/images/...` | `/asset/admmgr/style2/images/...` |
| 관리자 폰트 | `src/main/webapp/asset/admmgr/style2/font/...` | `src/main/resources/static/asset/admmgr/style2/font/...` | `/asset/admmgr/style2/font/...` |

템플릿에서는 반드시 Thymeleaf URL 문법을 사용한다.

```html
<link rel="stylesheet" th:href="@{/asset/admmgr/style2/css/admin-layout.css}">
<script th:src="@{/asset/admmgr/style2/js/ADM.Common.js}"></script>
```

금지 기준은 다음과 같다.

| 금지 항목 | 사유 |
|---|---|
| `../asset/...` 상대 경로 | 템플릿 경로 이동 시 깨질 수 있음 |
| `/resources/static/...` URL | 브라우저에서 접근하는 URL이 아님 |
| `/static/asset/...` URL | Spring Boot 정적 매핑 기준과 불일치 |
| `admin/scenarios/*.css` 같은 화면별 임시 경로 | 8소스 공통 관리자 체계와 불일치 |

이번 작업에서는 정적 리소스를 복사하지 않는다. `/asset/admmgr/style2/...` URL 체계는 템플릿 참조 정합성만 점검하고, 실제 CSS/JS/image/font 파일의 존재 여부와 복사 대상은 1.A-③ 정적 리소스 인벤토리 작업지시서에서 별도로 다룬다.

## 7. 작업 순서

실제 구현 Phase에서는 아래 순서로 진행한다.

Phase 1.A-① 즉시 정렬 PR의 구현 범위는 템플릿 경로/파일명 정렬과 controller view 반환 문자열 변경으로 제한한다. 1.A-② 공통 레이아웃 fragment 정리와 1.A-③ 정적 리소스 인벤토리는 별도 PR 또는 별도 작업지시서로 분리한다.

1. 대상 파일을 백업하지 말고 Git 변경으로 추적한다.
2. `src/main/resources/templates/admmgr/scenario/` 디렉터리를 생성한다.
3. 기존 `templates/admin/scenarios/list.html`을 `templates/admmgr/scenario/scenarioList.html` 기준으로 이동/리네임한다.
4. 기존 `form.html`을 `scenarioRegist.html`, `detail.html`을 `scenarioView.html`, `preview.html`을 `scenarioPreviewLayer.html` 기준으로 이동/리네임한다.
5. `AdmScenarioController`의 view 반환 문자열 7건을 매핑표대로 변경한다.
6. 일반 화면 3건에는 관리자 공통 레이아웃 적용 여부를 반영한다. `scenarioPreviewLayer.html`은 `*Layer.html` 원칙에 따라 레이아웃을 제외한다.
7. 정적 리소스 참조를 `/asset/admmgr/style2/...` 기준으로 점검하고, URL 문법을 `@{...}`로 통일한다.

작업 중 `@RequestMapping`, `@GetMapping`, `@PostMapping`, redirect URL, 서비스 호출, DTO, DB 접근 코드는 변경하지 않는다.

## 8. 검증 명령

Windows 기준 검증 명령은 다음을 사용한다.

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd -q test-compile
```

view 문자열 잔존 여부는 `Select-String`으로 확인한다.

```powershell
Select-String -Path "src\main\java\**\*.java" -Pattern '"admin/scenarios/(list|form|detail|preview)"'
Select-String -Path "src\main\resources\templates\**\*.html" -Pattern 'templates/admin/scenarios|admin/scenarios/(list|form|detail|preview)'
Select-String -Path "src\main\java\**\*.java" -Pattern '"admmgr/scenario/scenario(List|Regist|View|PreviewLayer)"'
Select-String -Path "src\main\resources\templates\**\*.html" -Pattern '/resources/static|/static/asset|\.\./asset'
Select-String -Path "src\main\resources\templates\admmgr\scenario\*.html" -Pattern '/asset/admmgr/style2/'
Select-String -Path "src\main\resources\templates\admmgr\scenario\*.html" -Pattern 'layout:decorate|adminHeader|adminSidebar|adminFooter|adminNavigation'
```

템플릿 파일 위치와 잔존 경로는 다음 명령으로 확인한다.

```powershell
$expected = @(
  "src\main\resources\templates\admmgr\scenario\scenarioList.html",
  "src\main\resources\templates\admmgr\scenario\scenarioRegist.html",
  "src\main\resources\templates\admmgr\scenario\scenarioView.html",
  "src\main\resources\templates\admmgr\scenario\scenarioPreviewLayer.html"
)
$expected | ForEach-Object { if (-not (Test-Path $_)) { Write-Error "missing template: $_" } }
Get-ChildItem "src\main\resources\templates\admin\scenarios" -Filter "*.html" -ErrorAction SilentlyContinue
Select-String -Path "src\main\java\**\*.java","src\main\resources\templates\**\*.html" -Pattern 'templates\\admin\\scenarios|templates/admin/scenarios|admin/scenarios/(list|form|detail|preview)'
```

`Get-ChildItem "src\main\resources\templates\admin\scenarios"` 결과는 0건이어야 하며, `templates/admmgr/scenario` 목표 파일은 4건 모두 존재해야 한다. 마지막 `Select-String`은 기존 view 경로 잔존 여부를 확인하며 결과가 없어야 통과로 본다.

런타임 스모크 URL은 기존 URL을 유지해서 확인한다.

| URL | 기대 결과 |
|---|---|
| `GET /admin/scenarios` | 목록 화면 렌더링 |
| `GET /admin/scenarios/new` | 등록 화면 렌더링 |
| `GET /admin/scenarios/{id}` | 상세 화면 렌더링 |
| `GET /admin/scenarios/{id}/edit` | 수정 화면 렌더링 |
| `GET /admin/scenarios/versions/{versionId}/preview` | preview layer 화면 렌더링 |

PASS 기준은 다음과 같다.

| 항목 | 기준 |
|---|---|
| 컴파일 | `mvnw.cmd -q test-compile` 성공 |
| view 잔존 | Java 반환 문자열에 `admin/scenarios/list|form|detail|preview` 잔존 0건 |
| 템플릿 위치 | `templates/admmgr/scenario/scenario*.html` 4건 존재 |
| URL 유지 | `/admin/scenarios` 계열 URL 변경 0건 |
| 정적 리소스 | `/asset/admmgr/style2/...` 또는 기존 유효 경로만 사용 |
| 레이아웃 | 일반 화면과 Layer 화면의 적용 기준이 분리되어 있음 |

## 9. PR 범위 제한

허용 변경은 다음으로 제한한다.

| 허용 | 파일/내용 |
|---|---|
| 템플릿 이동/리네임 | `templates/admin/scenarios/*.html` → `templates/admmgr/scenario/*.html` |
| view 반환 문자열 변경 | `AdmScenarioController`의 7개 view return |
| 공통 fragment 추가/수정 | `templates/common/fragments.html`, 필요 시 관리자 골격 fragment |
| 레이아웃 템플릿 추가 | `templates/layout/admin-layout.html`, `templates/layout/embed-layout.html` 단, 의존성 정책 확인 후 |
| 정적 리소스 경로 보정 | 템플릿 내 CSS/JS/image URL 참조 문자열만 보정. 파일 복사·교체는 1.A-③ 범위 |

금지 변경은 다음과 같다.

| 금지 | 사유 |
|---|---|
| `/admin/scenarios` URL 변경 | 라우팅 영향이 커서 별도 Phase 대상 |
| `kr.co.cleverchat` 패키지 변경 | 패키지 정렬은 별도 Phase 대상 |
| DB schema/mapper 변경 | 화면 경로 정렬과 무관 |
| 인증/권한 로직 변경 | Auth/Menu/Login Phase와 충돌 가능 |
| 대량 CSS 재작성 | 레이아웃 정렬 범위 초과 |
| 8소스 리소스 전체 복사 | 필요한 파일만 후속 Phase에서 선별 |
| `thymeleaf-layout-dialect` 무단 추가 | PM 승인 필요 |

권장 커밋 메시지는 다음 형식을 사용한다.

```text
Phase1A: align admin scenario templates to admmgr naming
```

## 10. 롤백 기준

다음 중 하나라도 발생하면 롤백한다.

| 트리거 | 판단 기준 |
|---|---|
| 컴파일 실패 | `mvnw.cmd -q test-compile` 실패 |
| view resolve 실패 | 5개 스모크 URL 중 view not found 발생 |
| URL 회귀 | 기존 `/admin/scenarios` 진입 URL이 변경되거나 404 발생 |
| 레이아웃 의존성 오류 | Layout Dialect 미도입 상태에서 `layout:*` 처리 오류 발생 |
| 정적 리소스 오류 | 관리자 화면 핵심 CSS/JS가 404로 로드 실패 |

롤백 절차는 다음을 따른다.

```powershell
git status --short
git revert <Phase1A-commit-sha>
.\mvnw.cmd -q test-compile
```

PR 병합 전이면 revert 커밋 대신 작업 브랜치에서 변경 파일을 원복한다. 단, 다른 사람이 같은 파일에 추가한 변경은 되돌리지 않는다.

롤백 후 후속 조치는 다음 순서로 수행한다.

1. 실패 로그와 재현 URL을 PR 코멘트에 기록한다.
2. 실패 원인을 `view mapping`, `layout dependency`, `static resource`, `template syntax` 중 하나로 분류한다.
3. Layout Dialect 필요성이 원인인 경우 1.A-②로 분리하고 PM 승인 요청을 등록한다.

## 11. 완료 기준

Phase 1.A 구현 완료 기준은 다음과 같다.

| 항목 | 완료 기준 |
|---|---|
| 변경 파일 수 | 9건 이하 |
| 시나리오 화면 파일 | `scenarioList.html`, `scenarioRegist.html`, `scenarioView.html`, `scenarioPreviewLayer.html` 4건 |
| 컨트롤러 view 반환 | 7건 모두 `admmgr/scenario/...`로 변경 |
| URL 변경 | 0건 |
| 패키지 변경 | 0건 |
| DB/mapper 변경 | 0건 |
| 의존성 변경 | 0건. 단, 1.A-② 승인 PR은 예외 |
| 일반 화면 레이아웃 | 목록/등록수정/상세에 관리자 공통 레이아웃 기준 반영 |
| Layer 화면 | sidebar/header 중복 미적용 |
| 검증 | `test-compile` 및 `Select-String` 기준 통과 |

## 12. 후속 Phase 연결

| Phase | 내용 | 선행 조건 |
|---|---|---|
| 1.A-② | 공통 레이아웃 fragment 정리 및 `layout/admin-layout`, `embed-layout` 적용 | Layout Dialect 도입 여부 승인 |
| 1.A-③ | 정적 리소스 인벤토리 작성 및 복사 대상 확정 | 시나리오 화면 정렬 완료 |
| 1.A-④ | 관리자 홈 `admin/index.html`의 `admmgr/dashboard` 정렬 | 공통 레이아웃/정적 리소스 기준 확정 |
| 1.B | 관리자 URL 체계 `/admmgr/...` 전환 검토 | 메뉴/권한 정책 확정 |
| 1.C | 패키지/클래스 명명 정렬 검토 | 기능 안정화 후 |
| 2.x | 인증/권한/메뉴, 정적 리소스, 대시보드 통합 | Phase 1.A 결과 반영 |

## 부록 A. 변경 대상 파일/라인 목록

현재 기준으로 확인된 직접 대상은 다음과 같다.

| 파일 | 현재 라인/대상 | 목표 |
|---|---|---|
| `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` | 46 `admin/scenarios/list` | `admmgr/scenario/scenarioList` |
| `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` | 53, 60, 84, 92 `admin/scenarios/form` | `admmgr/scenario/scenarioRegist` |
| `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` | 71 `admin/scenarios/detail` | `admmgr/scenario/scenarioView` |
| `src/main/java/kr/co/cleverchat/domain/scenario/controller/AdmScenarioController.java` | 135 `admin/scenarios/preview` | `admmgr/scenario/scenarioPreviewLayer` |
| `src/main/resources/templates/admin/scenarios/list.html` | 파일 이동/리네임 대상 | `src/main/resources/templates/admmgr/scenario/scenarioList.html` |
| `src/main/resources/templates/admin/scenarios/form.html` | 파일 이동/리네임 대상 | `src/main/resources/templates/admmgr/scenario/scenarioRegist.html` |
| `src/main/resources/templates/admin/scenarios/detail.html` | 파일 이동/리네임 대상 | `src/main/resources/templates/admmgr/scenario/scenarioView.html` |
| `src/main/resources/templates/admin/scenarios/preview.html` | 파일 이동/리네임 대상 | `src/main/resources/templates/admmgr/scenario/scenarioPreviewLayer.html` |
| `src/main/resources/templates/common/fragments.html` | 필요 시 공통 fragment 추가 | `adminHeader`, `adminSidebar`, `adminFooter`, `adminNavigation` |

라인 번호는 2026-05-13 현재 파일 기준이다. 구현 시 최신 브랜치에서 다시 확인한다.

## 부록 B. 8소스 참조 파일

| 참조 파일 | 참조 목적 |
|---|---|
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/layout/admin-layout.html` | 관리자 header/sidebar/navigation/content 레이아웃 기준 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/layout/embed-layout.html` | sidebar/header 없는 임베드/부분 화면 기준 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/common/fragments.html` | 공통 fragment 명명과 사용 방식 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/admmgr/manage/menuList.html` | `layout:decorate`, `layout:fragment`, `List` 화면 기준 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/admmgr/exportKor/exportRegist.html` | `Regist` 화면과 등록/수정 공용 분기 기준 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/admmgr/exportKor/exportView.html` | `View` 상세 화면 기준 |
| `8.소스/OverseasNPP_20260511/src/main/resources/templates/admmgr/security/ipAccRegistLayer.html` | `Layer` 화면의 레이아웃 제외 기준 |
