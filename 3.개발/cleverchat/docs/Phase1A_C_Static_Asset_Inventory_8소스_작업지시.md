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

정적 리소스 인벤토리는 화면 단위로 실제 참조 여부를 기록한다. 1차 매트릭스는 다음 형식을 사용한다.

| 화면/템플릿 | CSS | JS | image | font | 판단 |
| --- | --- | --- | --- | --- | --- |
| scenario 목록 | `admin-layout.css`, `sub.css` | `ADM.Common.js` | 확인 필요 | `font/` | 후보 |
| scenario 상세 | `admin-layout.css`, `sub.css` | `ADM.Common.js` | 확인 필요 | `font/` | 후보 |
| scenario 등록/수정 | `admin-layout.css`, `sub.css` | `ADM.Common.js` | 확인 필요 | `font/` | 후보 |

화면별 판단 기준:

- 실제 JSP, HTML, Thymeleaf, Vue, React 등 구현 파일에서 참조가 확인된 자산만 복사 대상으로 확정한다.
- CSS 내부 상대 경로 참조는 CSS 파일 기준으로 역추적한다.
- 화면에서 직접 참조하지 않더라도 CSS 내부에서 필수로 참조하는 폰트는 종속 자산으로 포함한다.
- image 자산은 현재 기준 디렉터리가 없으므로 화면 또는 CSS에서 구체 파일명이 확인되기 전까지 복사 대상에서 제외한다.

## 5. 복사 범위

후속 복사 작업의 허용 범위는 다음과 같다.

허용:

- `style2/css/admin-layout.css`
- `style2/css/sub.css`
- `style2/js/ADM.Common.js`
- `style2/font/` 하위 9개 폰트 파일
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

- [ ] CleverChat scenario 화면 파일 위치 확인
- [ ] 화면별 CSS/JS 직접 참조 확인
- [ ] CSS 내부 `url()` 참조 확인
- [ ] `sub.css`의 `../font/` 상대 경로 유지 가능 여부 확인
- [ ] 기존 CleverChat 정적 리소스 중복 여부 확인
- [ ] 폰트 및 외부 라이브러리 라이선스 확인
- [ ] 최종 복사 대상 파일 목록 확정
- [ ] 금지 범위 위반 여부 확인
- [ ] 브라우저 네트워크 404 검증
