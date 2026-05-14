# 1.A-③ 정적 리소스 인벤토리 작업지시서

## 1. 목적과 핵심 판단

본 문서는 CleverChat scenario 화면에 도입할 정적 리소스 인벤토리를 8소스 기준으로 정의하기 위한 작업지시서다. 작업 범위는 `8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2` 하위 CSS, JS, image, font 자산 중 실제 화면에서 참조할 가능성이 높은 항목을 식별하고, 후속 복사 작업의 기준을 정하는 데 한정한다.

이번 단계에서는 코드 수정, 리소스 복사, 경로 변경, 번들링 설정 변경을 수행하지 않는다. 산출물은 본 문서 1건이다.

핵심 판단은 다음과 같다.

- 8소스 `style2` 자산을 CleverChat scenario 화면의 우선 기준으로 삼는다.
- 전체 `asset` 또는 전체 `style2` 복사는 금지한다.
- 초기 기준 자산은 `admin-layout.css`, `sub.css`, `ADM.Common.js`, `images/`, `font/`로 정의한다.
- 현재 확인 기준 `style2/images/` 디렉터리는 8소스에 부재하므로 `images/`는 화면 참조가 확인될 때 후속 결정한다.
- `sub.css`는 `../font/` 하위 폰트 9개를 `@font-face`로 직접 참조하므로 `sub.css` 도입 시 `font/`는 1:1 동시 도입해야 한다.

## 2. 8소스 자산 현황

기준 경로:

```text
8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2
```

확인된 하위 구조:

```text
style2/
  css/
    admin-layout.css
    login.css
    sub.css
  js/
    ADM.Common.js
    ADM.FileUpload.js
    ADM.TreeList.js
    lib/
      chart.umd.min.js
  font/
    Inter-Bold.woff2
    Inter-ExtraBold.woff2
    Inter-Medium.woff2
    Inter-Regular.woff2
    Inter-SemiBold.woff2
    NotoSansKR-Bold.ttf
    NotoSansKR-Medium.ttf
    NotoSansKR-Regular.ttf
    NotoSansKR-SemiBold.ttf
```

특이사항:

- `style2/images/` 디렉터리는 현재 8소스 기준으로 확인되지 않는다.
- `ADM.Form.js`는 일부 8소스 템플릿에서 참조될 수 있으나 `style2/js/` 하위 실파일이 확인되지 않으므로 도입 보류한다.
- `login.css`, `ADM.FileUpload.js`, `ADM.TreeList.js`, `chart.umd.min.js`는 scenario 화면의 실제 참조가 확인될 때만 후보로 승격한다.

## 3. Baseline 5종 정의

초기 기준 자산은 다음 5개 그룹으로 둔다.

| ID | 기준 자산 | 8소스 위치 | 초기 판단 | 비고 |
| --- | --- | --- | --- | --- |
| A1 | `admin-layout.css` | `style2/css/admin-layout.css` | 도입 후보 | 관리자 레이아웃 공통 스타일 기준 |
| A2 | `sub.css` | `style2/css/sub.css` | 도입 후보 | scenario 본문, 목록, 폼 스타일 기준 |
| A3 | `ADM.Common.js` | `style2/js/ADM.Common.js` | 도입 후보 | 공통 UI 동작 기준 |
| A4 | `images/` | `style2/images/` | 후속 결정 | 현재 8소스 디렉터리 부재 |
| A5 | `font/` | `style2/font/` | A2와 동시 도입 | `sub.css`의 `../font/` 참조 유지 필요 |

## 4. CleverChat Scenario 화면 매트릭스

정적 리소스 인벤토리는 화면 단위로 실제 참조 여부를 기록한다.

조사 범위:

- 기준일: 2026-05-13
- 대상: `src/main/resources/templates/` 및 `src/main/webapp/` 하위 JSP/HTML/Thymeleaf 템플릿
- 결과: scenario 관련 화면은 Thymeleaf 템플릿 4건이며, JSP 0건, 순수 HTML 0건이다. `src/main/webapp/` 디렉터리는 확인되지 않았다.

실측 요약:

- 외부 CSS 참조(`<link rel="stylesheet">`): 0건. 4개 화면 모두 인라인 `<style>` 블록만 사용한다.
- 외부 JS 참조(`<script src=...>`): 0건.
- image 참조(`<img src=...>`, CSS `url(...)`): 0건.
- font 파일 참조(`@font-face`): 0건. 인라인 `font-family: Arial, "Noto Sans KR", sans-serif` 폰트 스택만 사용한다.

| 화면/템플릿 | 실파일 경로 | 외부 CSS 참조 | 외부 JS 참조 | image 참조 | font 참조 | style2 매핑 필요 |
| --- | --- | --- | --- | --- | --- | --- |
| scenario 목록 | `3.개발/cleverchat/src/main/resources/templates/admin/scenarios/list.html` | 0건, 인라인 `<style>` 사용 | 0건 | 0건 | 0건, OS fallback 폰트 스택 | 필요 |
| scenario 상세 | `3.개발/cleverchat/src/main/resources/templates/admin/scenarios/detail.html` | 0건, 인라인 `<style>` 사용 | 0건 | 0건 | 0건, OS fallback 폰트 스택 | 필요 |
| scenario 등록/수정 | `3.개발/cleverchat/src/main/resources/templates/admin/scenarios/form.html` | 0건, 인라인 `<style>` 사용 | 0건 | 0건 | 0건, OS fallback 폰트 스택 | 필요 |
| scenario 미리보기 | `3.개발/cleverchat/src/main/resources/templates/admin/scenarios/preview.html` | 0건, 인라인 `<style>` 사용 | 0건 | 0건 | 0건, OS fallback 폰트 스택 | 필요 |

화면별 판단 기준:

- 실제 JSP, HTML, Thymeleaf, Vue, React 등 구현 파일에서 참조가 확인된 자산만 복사 대상으로 확정한다.
- CSS 내부 상대 경로 참조는 CSS 파일 기준으로 역추적한다.
- 화면에서 직접 참조하지 않더라도 CSS 내부에서 필수로 참조하는 폰트는 종속 자산으로 포함한다.
- image 자산은 현재 기준 디렉터리가 없으므로 화면 또는 CSS에서 구체 파일명이 확인되기 전까지 복사 대상에서 제외한다.
- scenario 4개 화면은 모두 인라인 스타일 상태이므로 후속 단계에서 8소스 `style2/css/admin-layout.css` 및 `style2/css/sub.css` 매핑 대상이다.
- `sub.css` 도입 시 `style2/font/Inter-*.woff2` 및 `style2/font/NotoSansKR-*.ttf`는 CSS 종속 자산으로 함께 검토한다.

## 5. 복사 범위

후속 복사 작업의 허용 범위는 다음과 같다.

허용:

- `style2/css/admin-layout.css`
  - CSS 내부 `url(...)` 참조: 1건, 인라인 SVG `data:` URI이므로 별도 외부 자산 복사 대상 없음.
  - `@font-face` 참조: 0건.
- `style2/css/sub.css`
  - CSS 내부 `url(...)`/`@font-face` 외부 자산 종속: `../font/Inter-Regular.woff2`, `../font/Inter-Medium.woff2`, `../font/Inter-SemiBold.woff2`, `../font/Inter-Bold.woff2`, `../font/Inter-ExtraBold.woff2`, `../font/NotoSansKR-Regular.ttf`, `../font/NotoSansKR-Medium.ttf`, `../font/NotoSansKR-SemiBold.ttf`, `../font/NotoSansKR-Bold.ttf`.
  - 인라인 SVG `data:` URI 2건은 CSS 내장 값이므로 별도 image 복사 대상 없음.
- `style2/js/ADM.Common.js`
- `style2/font/` 하위 9개 폰트 파일
  - `sub.css` 기준 상대 경로 `../font/` 유지를 위해 `style2/css/`와 `style2/font/`의 상대 위치를 함께 유지한다.
- 화면 또는 CSS에서 파일 단위 참조가 확인된 image 파일

조건부 허용:

- `style2/js/ADM.FileUpload.js`: scenario 화면에 파일 업로드 UI가 실제 존재하고 참조가 확인된 경우
- `style2/js/ADM.TreeList.js`: scenario 화면에 트리 UI가 실제 존재하고 참조가 확인된 경우
- `style2/js/lib/chart.umd.min.js`: scenario 화면에 Chart.js 기반 차트가 실제 존재하고 참조가 확인된 경우
- `style2/css/login.css`: scenario 화면이 아닌 로그인 화면 작업 범위에서만 별도 검토

제외:

- 실파일이 없는 `ADM.Form.js`
- 참조가 확인되지 않은 `style2` 하위 전체 파일
- `asset/admmgr` 전체
- `asset` 전체

## 6. 라이선스 확인

복사 전 라이선스 확인은 필수다. 확인 결과는 후속 작업 문서 또는 PR 설명에 남긴다.

확인 대상:

- `Inter-*.woff2`
- `NotoSansKR-*.ttf`
- `chart.umd.min.js`
- 기타 외부 라이브러리로 식별되는 JS/CSS

확인 기준:

- 폰트 파일의 배포 라이선스와 프로젝트 내 재배포 가능 여부를 확인한다.
- minified JS는 원본 라이브러리명, 버전, 라이선스를 확인한다.
- 라이선스 파일이 8소스에 없으면 공식 배포처 기준으로 확인하고 출처 URL 또는 확인 근거를 기록한다.
- 라이선스가 불명확한 파일은 복사하지 않고 보류한다.

## 7. 중복 파일 정책

기존 CleverChat 프로젝트에 동일명 또는 유사 목적 자산이 이미 있는 경우 다음 순서로 판단한다.

1. 현재 CleverChat에서 실제 사용 중인 파일을 우선 보존한다.
2. 8소스 파일은 파일명, 크기, 해시, 내용 차이를 비교한 뒤 신규 도입 여부를 결정한다.
3. 동일 파일이면 중복 복사하지 않는다.
4. 동일명이나 내용이 다르면 덮어쓰지 않고 별도 경로 또는 별도 파일명 정책을 문서화한 뒤 결정한다.
5. 공통 라이브러리의 중복 버전이 발견되면 런타임 충돌 가능성을 먼저 검토한다.

권장 비교 명령:

```bash
find 8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2 -type f | sort
find 3.개발/cleverchat -type f \( -name '*.css' -o -name '*.js' -o -name '*.woff2' -o -name '*.ttf' \) | sort
sha256sum <source-file> <target-file>
```

## 8. 경로 정렬 기준

후속 복사 시 경로는 8소스 구조를 무리하게 평탄화하지 않고, CleverChat의 기존 정적 리소스 관례에 맞춘다.

정렬 원칙:

- CSS, JS, font, image는 타입별 디렉터리를 유지한다.
- CSS 내부 `../font/` 참조를 유지할 수 있도록 `css/`와 `font/`의 상대 위치를 함께 설계한다.
- 기존 CleverChat 정적 리소스 루트가 있으면 그 구조를 우선한다.
- 신규 경로가 필요하면 `admmgr/style2` 출처가 추적 가능하도록 경로 또는 문서에 명시한다.
- 경로명은 운영 환경에서 대소문자 구분 문제가 생기지 않도록 원본 파일명을 그대로 유지한다.

예시 경로 후보:

```text
static/asset/admmgr/style2/css/
static/asset/admmgr/style2/js/
static/asset/admmgr/style2/font/
static/asset/admmgr/style2/images/
```

위 예시는 확정 경로가 아니다. 실제 CleverChat 정적 리소스 배치 관례 확인 후 확정한다.

## 9. 검증 명령

문서 단계 검증:

```bash
test -f '3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md'
find '8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2' -maxdepth 3 -type f | sort
rg -n "@font-face|url\\(" '8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/sub.css'
```

후속 복사 검증:

```bash
find <target-static-root> -type f | sort
rg -n "admin-layout.css|sub.css|ADM.Common.js" <cleverchat-view-root>
rg -n "../font/|url\\(" <target-static-root>/css/sub.css
```

브라우저 검증:

- scenario 목록, 상세, 등록/수정 화면에서 CSS 404가 없는지 확인한다.
- JS 콘솔 오류가 없는지 확인한다.
- 폰트 파일 요청이 200 또는 캐시 히트로 처리되는지 확인한다.
- 화면 깨짐, 버튼/폼 간격, 테이블 폭, 모달/레이어 동작을 확인한다.

## 10. 금지 범위

이번 1.A-③ 단계에서 금지하는 작업은 다음과 같다.

- 코드 수정 금지
- 리소스 복사 금지
- 전체 `asset` 복사 금지
- 전체 `asset/admmgr` 복사 금지
- 전체 `style2` 일괄 복사 금지
- 기존 CleverChat 정적 리소스 덮어쓰기 금지
- 라이선스 미확인 폰트/라이브러리 도입 금지
- 실파일이 없는 참조를 임의 생성 금지
- 화면 참조가 확인되지 않은 이미지 일괄 도입 금지

## 11. 산출물

이번 작업 산출물:

```text
3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md
```

후속 단계 산출물 후보:

- scenario 화면별 정적 리소스 참조 매트릭스
- 복사 대상 파일 목록
- 라이선스 확인표
- 중복 파일 비교표
- 최종 배치 경로표
- 브라우저 네트워크 검증 결과

## 12. 1.A-①/②/③ 단계 관계

1.A 단계는 8소스 우선 정렬 원칙에 따라 화면 구조, 레이아웃 기준, 정적 자산 기준을 순차적으로 확정한다.

| 단계 | 역할 | 산출 기준 |
| --- | --- | --- |
| 1.A-① | scenario 화면 기준 구조 정리 | 화면/기능/템플릿 후보 확정 |
| 1.A-② | 관리자 레이아웃 템플릿 기준 정리 | 공통 레이아웃, 헤더, 사이드바, 본문 구조 확정 |
| 1.A-③ | 정적 리소스 인벤토리 기준 정리 | CSS/JS/image/font 도입 후보와 복사 정책 확정 |

정렬 원칙:

- 8소스에 존재하는 화면, 레이아웃, 자산을 우선 기준으로 삼는다.
- 8소스에 없는 파일은 임의 보완하지 않고 보류 또는 후속 결정으로 표시한다.
- 1.A-①의 화면 후보와 1.A-②의 레이아웃 기준에 실제 필요한 정적 자산만 1.A-③에서 복사 후보로 승격한다.
- 1.A-③은 복사 실행 문서가 아니라 복사 전 판단 문서다.

## 부록 A. 초기 인벤토리 후보

| 구분 | 파일/디렉터리 | 상태 | 처리 |
| --- | --- | --- | --- |
| CSS | `style2/css/admin-layout.css` | 존재 | 후보 |
| CSS | `style2/css/sub.css` | 존재 | 후보 |
| CSS | `style2/css/login.css` | 존재 | scenario 범위 제외 |
| JS | `style2/js/ADM.Common.js` | 존재 | 후보 |
| JS | `style2/js/ADM.FileUpload.js` | 존재 | 조건부 |
| JS | `style2/js/ADM.TreeList.js` | 존재 | 조건부 |
| JS | `style2/js/lib/chart.umd.min.js` | 존재 | 조건부, 라이선스 확인 |
| JS | `style2/js/ADM.Form.js` | 부재 | 보류 |
| image | `style2/images/` | 부재 | 후속 결정 |
| font | `style2/font/Inter-*.woff2` | 존재 | `sub.css`와 동시 후보 |
| font | `style2/font/NotoSansKR-*.ttf` | 존재 | `sub.css`와 동시 후보 |

## 부록 B. 후속 작업 체크리스트

- [x] CleverChat scenario 화면 파일 위치 확인(Thymeleaf 4건: `list.html`, `detail.html`, `form.html`, `preview.html`)
- [x] 화면별 CSS/JS 직접 참조 확인(외부 CSS 0건, 외부 JS 0건, 각 화면 인라인 `<style>` 1건)
- [x] CSS 내부 `url()` 참조 확인(`admin-layout.css` 1건, `sub.css` 11건; 외부 image 참조 0건, 폰트 9건)
- [x] `sub.css`의 `../font/` 상대 경로 유지 가능 여부 확인(`style2/css/`와 `style2/font/` 상대 위치 유지 시 가능)
- [x] 기존 CleverChat 정적 리소스 중복 여부 확인(`static/` 하위 파일 0건, 충돌 0건. 상세: 부록 C)
- [ ] 폰트 및 외부 라이브러리 라이선스 확인
- [ ] 최종 복사 대상 파일 목록 확정
- [ ] 금지 범위 위반 여부 확인
- [ ] 브라우저 네트워크 404 검증

## 부록 B-1. CSS 내부 url() / @font-face 실측 결과

대상 파일:

- `8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/admin-layout.css`
- `8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/sub.css`

실측 명령:

```bash
rg -n "url\\(|@font-face|src:" \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/admin-layout.css" \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/sub.css"
```

| CSS | 라인 | 종류 | 참조 | 외부 자산 |
| --- | ---: | --- | --- | --- |
| `admin-layout.css` | 109 | `url()` | 인라인 SVG `data:` URI | 없음 |
| `sub.css` | 7 | `@font-face` | `../font/Inter-Regular.woff2` | `Inter-Regular.woff2` |
| `sub.css` | 8 | `@font-face` | `../font/Inter-Medium.woff2` | `Inter-Medium.woff2` |
| `sub.css` | 9 | `@font-face` | `../font/Inter-SemiBold.woff2` | `Inter-SemiBold.woff2` |
| `sub.css` | 10 | `@font-face` | `../font/Inter-Bold.woff2` | `Inter-Bold.woff2` |
| `sub.css` | 11 | `@font-face` | `../font/Inter-ExtraBold.woff2` | `Inter-ExtraBold.woff2` |
| `sub.css` | 12 | (메모) | fontawesome 등 외부 아이콘 폰트 import 0건 확인 (Inter 블록 종료부) | 없음 |
| `sub.css` | 14 | `@font-face` | `../font/NotoSansKR-Regular.ttf` | `NotoSansKR-Regular.ttf` |
| `sub.css` | 15 | `@font-face` | `../font/NotoSansKR-Medium.ttf` | `NotoSansKR-Medium.ttf` |
| `sub.css` | 16 | `@font-face` | `../font/NotoSansKR-SemiBold.ttf` | `NotoSansKR-SemiBold.ttf` |
| `sub.css` | 17 | `@font-face` | `../font/NotoSansKR-Bold.ttf` | `NotoSansKR-Bold.ttf` |
| `sub.css` | 18 | (메모) | fontawesome 등 외부 아이콘 폰트 import 0건 확인 (NotoSansKR 블록 종료부) | 없음 |
| `sub.css` | 717 | `url()` | 인라인 SVG `data:` URI | 없음 |
| `sub.css` | 1382 | `url()` | 인라인 SVG `data:` URI | 없음 |

요약: `admin-layout.css` 외부 자산 종속은 0건이며, `sub.css` 외부 자산 종속은 폰트 9건이다. 본 검수에서 sub.css의 `@font-face` 라인 범위를 7~12(Inter 5건 + 메모) 및 14~18(NotoSansKR 4건 + 메모)로 보정했으며, fontawesome 등 외부 아이콘 폰트 라이브러리 import는 두 CSS 모두에서 확인되지 않았다. `images/` 디렉터리 또는 외부 image 파일 참조도 확인되지 않았다.

## 부록 B-2. 변경 출처 및 커밋 분리 기준

본 절은 1.A-⑤ 작업 이후 본 문서에 누적된 변경을 8소스 CSS 실측 범위와 CleverChat 화면 실측 범위로 구분하기 위한 기록이다. 코드 수정, 리소스 복사, 파일 이동은 이 기준의 대상이 아니다.

WBS 기준 작업 범위:

- 1.A-⑤: 8소스 CSS `url()`/`@font-face` 종속 자산 실측 반영. 본 문서의 직접 대상은 §5, 부록 B 체크리스트 3·4번, 부록 B-1이다.
- 1.A-⑥: CleverChat scenario 화면 4건 정적 참조 실측 매트릭스 반영. 본 문서의 직접 대상은 §4와 부록 B 체크리스트 1·2번이다.

변경 출처 분류:

| 변경 위치 | 변경 내용 | 출처 판정 |
| --- | --- | --- |
| §4 조사 범위 | 기준일, 대상 템플릿 경로, scenario Thymeleaf 4건 확인 | CleverChat 화면 실측 결과 |
| §4 실측 요약 | 외부 CSS/JS/image/font 참조 0건 및 인라인 스타일 확인 | CleverChat 화면 실측 결과 |
| §4 매트릭스 본문 | `list.html`, `detail.html`, `form.html`, `preview.html` 4건별 참조 현황 | CleverChat 화면 실측 결과 |
| §4 화면별 판단 기준 마지막 2줄 | `admin-layout.css`, `sub.css` 매핑 대상 및 `sub.css` 폰트 종속 검토 | 1.A-⑤ CSS 실측의 §4 파생 반영 |
| §5 복사 범위 | `admin-layout.css`, `sub.css`의 `url()`/`@font-face` 종속 자산과 상대 경로 유지 조건 | 1.A-⑤ CSS 실측 직접 결과 |
| 부록 B 체크리스트 1, 2번 | scenario 화면 파일 위치 및 화면별 CSS/JS 직접 참조 확인 | CleverChat 화면 실측 결과 |
| 부록 B 체크리스트 3, 4번 | CSS 내부 `url()` 및 `sub.css` `../font/` 상대 경로 확인 | 1.A-⑤ CSS 실측 직접 결과 |
| 부록 B-1 | CSS 내부 `url()`/`@font-face` 라인별 실측 표 | 1.A-⑤ CSS 실측 직접 결과 |

판정:

- §4의 조사 범위, 실측 요약, 매트릭스 본문은 1.A-⑤ CSS `url()`/font 참조 실측의 직접 결과가 아니다. 측정 대상이 `src/main/resources/templates/admin/scenarios/*.html` 화면 4건이고, 측정 항목도 화면의 외부 CSS/JS/image/font 직접 참조 여부이므로 별도 화면 실측 결과로 본다.
- §4 화면별 판단 기준의 마지막 2줄은 예외적으로 1.A-⑤ CSS 실측의 파생 반영이다. `sub.css`의 Inter/NotoSansKR 폰트 종속 9건은 부록 B-1 실측 없이는 확정할 수 없기 때문이다.
- §4 변경은 이전 작업 잔여로 분류하지 않는다. 현재 diff는 7b2932c에서 작업지시서가 추가된 뒤 동일 워킹트리에 누적된 변경이며, 파일 추가 이전부터 남아 있던 미완료 작업 잔여로 판단할 근거는 없다.

커밋 분리 권고:

| 권고 커밋 | 포함 범위 | 권고 메시지 |
| --- | --- | --- |
| A | §5 CSS 종속 자산 표기, 부록 B-1, 부록 B 체크리스트 3·4번, §4 화면별 판단 기준 마지막 2줄 | `docs(phase1a): 8소스 CSS url()/@font-face 종속 자산 실측 반영` |
| B | §4 조사 범위·실측 요약·매트릭스 본문, 부록 B 체크리스트 1·2번 | `docs(phase1a): scenario 화면 4건 정적 참조 실측 매트릭스 반영` |

부록 B-2 자체는 변경 출처와 분리 근거를 남기는 메타 기록이므로 커밋 A에 동봉하거나 별도 문서 커밋으로 분리한다. §4 안에 1.A-⑤ 파생 반영 2줄과 1.A-⑥ 화면 실측 본문이 함께 있으므로 `git add -p`로 hunk를 분리한다. 커밋 순서는 A 다음 B를 권고한다.

분리 이유: 1.A-⑤는 8소스 CSS 파일의 `url()`/`@font-face` 라인 스캔이고, 화면 실측은 CleverChat Thymeleaf 템플릿의 외부 참조 여부 스캔이다. 두 변경을 한 커밋에 묶으면 CSS 종속 자산 실측만 회수하거나 재검증할 때 §4 화면 매트릭스까지 함께 영향을 받는다.

## 부록 C. 1.A-⑦ 기존 CleverChat 정적 리소스 중복 스캔 결과

본 절은 8소스 `style2` 자산과 기존 CleverChat 정적 리소스의 동일 파일명 및 동일 역할 자산 존재 여부를 확인한 기록이다. 이번 작업에서는 코드 수정, 리소스 복사, 파일 이동을 수행하지 않는다.

실측 기준:

- 기준일: 2026-05-14
- CleverChat 정적 리소스 루트: `3.개발/cleverchat/src/main/resources/static/`
- 8소스 기준 루트: `8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/`

실측 명령:

```bash
find '3.개발/cleverchat/src/main/resources/static' -mindepth 1 -maxdepth 5 -print | sort
find '3.개발/cleverchat' -type f \( -iname '*.css' -o -iname '*.js' -o -iname '*.woff' -o -iname '*.woff2' -o -iname '*.ttf' -o -iname '*.otf' -o -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' -o -iname '*.gif' -o -iname '*.svg' -o -iname '*.ico' -o -iname '*.webp' \) | sort
find '8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2' -maxdepth 3 -type f | sort
```

실측 결과:

- `3.개발/cleverchat/src/main/resources/static/` 하위 파일 및 서브디렉터리: 0건
- `3.개발/cleverchat` 전체의 CSS/JS/font/image 계열 파일: 0건
- scenario Thymeleaf 4건은 외부 CSS/JS/image/font 파일 참조 없이 인라인 `<style>`만 사용한다.
- 따라서 기존 CleverChat 정적 리소스와 8소스 `style2` 자산 사이의 동일 파일명 충돌 및 동일 역할 파일 충돌은 확인되지 않았다.

8소스 `style2` 자산별 충돌 매트릭스:

| 구분 | 8소스 자산 | 동일 파일명 CleverChat 측 | 동일 역할 CleverChat 측 | 충돌 여부 | 분류/판정 |
| --- | --- | --- | --- | --- | --- |
| CSS | `style2/css/admin-layout.css` | 없음 | 없음. scenario 화면은 인라인 스타일만 사용 | 없음 | 신규 도입 후보 |
| CSS | `style2/css/sub.css` | 없음 | 없음. scenario 화면은 인라인 스타일만 사용 | 없음 | 신규 도입 후보 |
| CSS | `style2/css/login.css` | 없음 | 없음 | 없음 | scenario 범위 외, 본 스캔 비대상 |
| JS | `style2/js/ADM.Common.js` | 없음 | 없음 | 없음 | 신규 도입 후보 |
| JS | `style2/js/ADM.FileUpload.js` | 없음 | 없음 | 없음 | 조건부 후보 |
| JS | `style2/js/ADM.TreeList.js` | 없음 | 없음 | 없음 | 조건부 후보 |
| JS | `style2/js/lib/chart.umd.min.js` | 없음 | 없음 | 없음 | 조건부 후보, 라이선스 확인 필요 |
| font | `style2/font/Inter-*.woff2` 5건 | 없음 | 없음. 기존 font 파일 0건 | 없음 | `sub.css` 종속 후보, 라이선스 확인 필요 |
| font | `style2/font/NotoSansKR-*.ttf` 4건 | 없음 | 없음. 기존 font 파일 0건 | 없음 | `sub.css` 종속 후보, 라이선스 확인 필요 |
| image | `style2/images/` | 없음 | 없음. 기존 image 파일 0건 | 없음 | 추가 확인 필요. 8소스 `admmgr/style2/images/` 부재 |

4분류 매핑:

| 분류 | 해당 자산 | 판단 |
| --- | --- | --- |
| 유지 | 없음 | 기존 CleverChat 정적 리소스가 0건이므로 유지 대상으로 분류할 기존 파일이 없다. |
| 교체 | 없음 | 동일 파일명 또는 동일 역할 충돌이 없어 교체 대상이 없다. |
| 병합 | 없음 | 기존 자산과 8소스 자산을 합쳐야 하는 케이스가 없다. |
| 추가 확인 필요 | `style2/images/`, 인라인 스타일과 신규 CSS 적용 우선순위, `Inter-*.woff2`, `NotoSansKR-*.ttf`, `chart.umd.min.js` | `style2/images/`는 8소스 `admmgr/style2` 기준 디렉터리가 부재한다. 인라인 스타일과 신규 CSS 적용 우선순위는 화면 적용 단계에서 확인한다. 폰트와 Chart.js 파일은 복사 전 라이선스 확인이 필요하다. |

권장 처리 방안:

- `admin-layout.css`, `sub.css`, `ADM.Common.js`, `font/`는 기존 CleverChat 정적 리소스와 충돌하지 않으므로 후속 도입 시 신규 추가 후보로 유지한다.
- 도입 경로는 CSS의 `../font/` 상대 참조가 깨지지 않도록 `static/asset/admmgr/style2/css/`, `static/asset/admmgr/style2/font/`처럼 8소스의 `style2` 구조를 보존하는 방안을 우선 검토한다.
- `ADM.FileUpload.js`, `ADM.TreeList.js`, `chart.umd.min.js`, `login.css`는 scenario 화면의 실제 참조 또는 UI 필요성이 확인될 때만 후보로 승격한다.
- `style2/images/`는 8소스 `admmgr/style2` 기준 실디렉터리가 없으므로 후속 화면 참조 실측 전까지 복사 대상에서 제외한다.
- 폰트 9건과 `chart.umd.min.js`는 라이선스와 재배포 가능 여부 확인 전까지 실제 복사 또는 도입을 보류한다.

## 부록 D. 1.A-⑧ 라이선스 확인 결과

본 절은 §6 라이선스 확인 기준에 따라 §3 Baseline 5종 및 §5 조건부 허용 1종, 합계 6개 항목의 라이선스 확인 결과를 일괄 정리한 기록이다. 본 검수에서 보정한 부록 B-1의 라인 번호 7~12, 14~18 및 fontawesome 메모를 입력으로 삼는다. 이번 작업에서는 코드 수정, 리소스 복사, 파일 이동, 라이선스 사본 추가를 수행하지 않는다.

확인 기준일: 2026-05-14

확인 범위:

- §3 Baseline 5종 중 자산 식별이 가능한 4종(`admin-layout.css`, `sub.css`, `ADM.Common.js`, `font/`)
- §3 Baseline 중 `font/`는 Inter family와 NotoSansKR family 두 항목으로 분리하여 집계한다. (`images/`는 8소스 디렉터리 부재로 본 절 대상 외)
- §5 조건부 허용 항목 중 외부 라이브러리 식별이 명확한 `chart.umd.min.js` 1종

| 번호 | 항목 | 출처 | 라이선스 판정 | 사본/근거 | 복사 도입 결정 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `admin-layout.css` | 8소스 `style2/css/admin-layout.css` | 8소스 내부 자체 작성으로 추정. 외부 라이브러리 식별 단서 없음. fontawesome 등 외부 아이콘 폰트 import 0건(부록 B-1). | 별도 라이선스 사본 불필요. 출처: 8소스 저장소 자체. | 신규 도입 후보 유지. PR 설명에 출처 표기. |
| 2 | `sub.css` | 8소스 `style2/css/sub.css` | 8소스 내부 자체 작성으로 추정. 외부 라이브러리 식별 단서 없음. fontawesome 등 외부 아이콘 폰트 import 0건(부록 B-1, 라인 12·18 메모). | 별도 라이선스 사본 불필요. 출처: 8소스 저장소 자체. | 신규 도입 후보 유지. PR 설명에 출처 표기. |
| 3 | `ADM.Common.js` | 8소스 `style2/js/ADM.Common.js` | 8소스 내부 자체 작성으로 추정. minified 외부 라이브러리 식별 단서 없음. | 별도 라이선스 사본 불필요. 출처: 8소스 저장소 자체. | 신규 도입 후보 유지. PR 설명에 출처 표기. |
| 4 | Inter family (`Inter-Regular.woff2`, `Inter-Medium.woff2`, `Inter-SemiBold.woff2`, `Inter-Bold.woff2`, `Inter-ExtraBold.woff2`) 5건 | 8소스 `style2/font/Inter-*.woff2` (부록 B-1 라인 7~11) | SIL Open Font License 1.1 추정. 8소스에 OFL 사본 미동봉. 공식 배포처(rsms/inter, Google Fonts) 확인 절차 미완료. | 사본 미확보. 공식 배포처 OFL 1.1 본문 및 출처 URL 확보 후 PR 설명/`docs/licenses/`에 첨부 필요. | **OFL 사본 확보 전 복사 대상 제외.** `sub.css` 도입 시점에 OFL 사본이 함께 확보되지 않으면 `sub.css`도 후속 결정으로 보류한다. |
| 5 | NotoSansKR family (`NotoSansKR-Regular.ttf`, `NotoSansKR-Medium.ttf`, `NotoSansKR-SemiBold.ttf`, `NotoSansKR-Bold.ttf`) 4건 | 8소스 `style2/font/NotoSansKR-*.ttf` (부록 B-1 라인 14~17) | SIL Open Font License 1.1 추정. 8소스에 OFL 사본 미동봉. 공식 배포처(Google Fonts Noto Sans KR) 확인 절차 미완료. | 사본 미확보. 공식 배포처 OFL 1.1 본문 및 출처 URL 확보 후 PR 설명/`docs/licenses/`에 첨부 필요. | **OFL 사본 확보 전 복사 대상 제외.** `sub.css` 도입 시점에 OFL 사본이 함께 확보되지 않으면 `sub.css`도 후속 결정으로 보류한다. |
| 6 | `chart.umd.min.js` | 8소스 `style2/js/lib/chart.umd.min.js` | Chart.js MIT License로 추정. minified 파일 상단 `/*! Chart.js ... | (c) ... | MIT License */` 헤더 주석에 라이선스 명시. | 헤더 주석 자체가 MIT 고지 역할. 별도 LICENSE 파일 동봉은 운영 정책에 따라 추가 검토. | **헤더 주석 보존 조건으로 도입 후보 승격 가능.** scenario 화면에 Chart.js 기반 차트 참조가 확인되는 시점에 §5 조건부 허용에서 허용으로 전환한다. minify 재처리·헤더 제거·재포맷 금지. |

집계:

- 신규 도입 후보 유지(자체): 3건 — `admin-layout.css`, `sub.css`, `ADM.Common.js`
- 복사 대상 제외(사본 확보 전): 9건 — Inter `*.woff2` 5건 + NotoSansKR `*.ttf` 4건 (Baseline 표 기준 2개 항목)
- 조건부 승격 가능(헤더 주석 보존 조건): 1건 — `chart.umd.min.js`

후속 작업 연계:

- 부록 B 후속 작업 체크리스트의 "폰트 및 외부 라이브러리 라이선스 확인" 항목은 본 부록 D를 근거로 1차 정리되었으나, OFL 사본 확보 및 `chart.umd.min.js` 헤더 주석 보존 조건 명시가 완료될 때까지 미체크 상태로 둔다.
- §5 복사 범위의 `style2/font/` 9건 항목은 본 부록 D 4·5번 결정에 따라 OFL 사본 확보 전까지 복사 보류로 운영한다. 동일 사유로 §5의 `sub.css` 항목도 단독 도입을 권장하지 않는다.
- §5 조건부 허용의 `chart.umd.min.js` 항목은 본 부록 D 6번 결정에 따라 화면 참조 확인 시 헤더 주석 보존 조건과 함께 허용으로 승격한다.
- fontawesome 등 외부 아이콘 폰트 라이브러리는 부록 B-1 라인 12·18 메모 기준 미사용으로 확인되었으므로 별도 라이선스 확인 대상에서 제외한다. 후속 화면 작업에서 fontawesome 도입이 새로 요구될 경우 본 부록과 별도 항목으로 추가 확인한다.
