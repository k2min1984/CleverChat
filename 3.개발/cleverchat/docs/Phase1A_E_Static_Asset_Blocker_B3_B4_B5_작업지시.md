# 1.A-E 정적 자산 블로커 B3/B4/B5 점검 작업지시서

> 작성일: 2026-05-19  
> 기준 문서: `3.개발/cleverchat/docs/Phase1A_D_OFL_Acquire_Inter_NotoSansKR_작업지시.md` §7  
> 작업 원칙: 이 문서는 작업지시서이다. 코드 수정, 라이선스 파일 생성, 리소스 복사, 디렉터리 생성은 수행하지 않는다.

## 1. 목적과 핵심 판단

본 문서는 Phase1A_D §7의 1.A-① 착수 전 블로커 중 B3, B4, B5를 후속 점검하기 위한 작업지시서다.

대상 블로커:

| 번호 | 점검 대상 | 완료 판단 |
| --- | --- | --- |
| B3 | Inter/NotoSansKR 공식 출처 URL 기록 검증 | 라이선스 산출물 META 블록에 `Source-Primary`, `Source-License`, `Source-Checked-Date`가 모두 기록되고 공식 출처로 판정됨 |
| B4 | `sub.css`와 `font/` 상대 경로 배치 확정 | 8소스 `style2/css/sub.css`의 `../font/` 9건 참조를 깨지 않는 CleverChat 최종 배치 경로가 명시됨 |
| B5 | 복사 금지 해제 근거 기록 | 1.A-③ §5 또는 WBS에 Inter/NotoSansKR 복사 금지 해제 근거가 기록됨 |

핵심 판단:

- B3는 Phase1A_D의 OFL 사본 확보 산출물이 존재한다는 전제에서 META 6필드와 공식 출처 URL을 정적 검증한다.
- B4는 Phase1A_C §8에서 "확정 경로가 아니다"로 남은 항목이므로, 본 문서에서 8소스 `style2` 구조 기준의 최종 배치 후보와 검증 명령을 확정한다.
- B5는 실제 복사 허가가 아니라, 복사 금지 해제 판단의 근거가 문서에 남았는지를 확인하는 절차다.

## 2. 입력 문서

| 입력 | 확인 위치 | 사용 목적 |
| --- | --- | --- |
| Phase1A_D OFL 확보 작업지시 | `Phase1A_D_OFL_Acquire_Inter_NotoSansKR_작업지시.md` §3, §4, §6, §7 | B3 META 필드와 공식 출처 URL 기준, B3~B5 블로커 정의 확인 |
| Phase1A_C 정적 리소스 인벤토리 | `Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` §5, §8, 부록 B-1, 부록 C, 부록 D | `sub.css` 폰트 종속 9건, 복사 금지 항목, 기존 static 충돌 0건 확인 |
| WBS | `1.기획/WBS.md` 1.A 관련 항목 | B5 해제 근거가 WBS에 기록되는 경우의 대체 확인 위치 |
| 8소스 style2 루트 | `8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/` | 원본 `css/`, `font/`, `js/` 상대 구조 확인 |
| CleverChat static 루트 | `3.개발/cleverchat/src/main/resources/static/` | 최종 배치 후보의 Spring Boot 정적 파일 루트 확인 |

## 3. 결정 항목

### 3.1 B3 공식 출처 URL 기록 검증

B3 점검 대상 라이선스 산출물 후보:

| 대상 | 예상 파일 | 필수 META 필드 |
| --- | --- | --- |
| Inter | `3.개발/cleverchat/docs/licenses/Inter-OFL.txt` | `Asset`, `License`, `Source-Primary`, `Source-License`, `Source-Checked-Date`, `Related-Inventory` |
| NotoSansKR | `3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt` | `Asset`, `License`, `Source-Primary`, `Source-License`, `Source-Checked-Date`, `Related-Inventory` |

판정 기준:

- `Source-Primary`는 공식 프로젝트, Google Fonts, 또는 Noto 공식 문서 URL이어야 한다.
- `Source-License`는 OFL 원문 또는 ZIP 내부 LICENSE/OFL 위치를 식별할 수 있어야 한다.
- `Source-Checked-Date`는 실제 확인일을 `YYYY-MM-DD`로 기록한다.
- PR 설명의 URL만으로 B3를 완료 처리하지 않는다. 문서 산출물 또는 후속 WBS/검수 기록에 URL과 확인일이 함께 남아야 한다.

점검란:

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| Inter META 6필드 확인 | [ ] 대기 | |
| Inter 공식 URL 확인 | [ ] 대기 | |
| NotoSansKR META 6필드 확인 | [ ] 대기 | |
| NotoSansKR 공식 URL 확인 | [ ] 대기 | |

### 3.2 B4 `sub.css`/`font/` 상대 경로 배치 확정

8소스 기준 구조:

```text
8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/
  css/
    sub.css
  font/
    Inter-Regular.woff2
    Inter-Medium.woff2
    Inter-SemiBold.woff2
    Inter-Bold.woff2
    Inter-ExtraBold.woff2
    NotoSansKR-Regular.ttf
    NotoSansKR-Medium.ttf
    NotoSansKR-SemiBold.ttf
    NotoSansKR-Bold.ttf
```

`sub.css`는 `style2/css/sub.css` 위치에서 `../font/...`를 참조한다. 따라서 CleverChat에서도 `css/`와 `font/`가 같은 부모 디렉터리 아래에 있어야 한다.

최종 배치 경로 후보:

| 후보 | CleverChat 물리 경로 | 브라우저 URL | 상대 경로 보존 | 판단 |
| --- | --- | --- | --- | --- |
| 1안 | `src/main/resources/static/asset/admmgr/style2/css/sub.css` + `src/main/resources/static/asset/admmgr/style2/font/...` | `/asset/admmgr/style2/css/sub.css`, `/asset/admmgr/style2/font/...` | 가능 | 권고 |
| 2안 | `src/main/resources/static/admmgr/style2/css/sub.css` + `src/main/resources/static/admmgr/style2/font/...` | `/admmgr/style2/css/sub.css`, `/admmgr/style2/font/...` | 가능 | 8소스 URL 체계와 불일치하므로 비권고 |
| 3안 | `src/main/resources/static/css/sub.css` + `src/main/resources/static/font/...` | `/css/sub.css`, `/font/...` | 가능 | 출처 추적성이 낮고 관리자 자산 범위가 흐려지므로 비권고 |

결정:

- B4 권고안은 1안이다.
- 1안은 Phase1A_Admin_Layout_Template §6의 `/asset/admmgr/style2/...` URL 체계와 일치한다.
- 1안은 Phase1A_C 부록 C의 "8소스의 `style2` 구조를 보존하는 방안 우선 검토"와 일치한다.
- B4 완료 처리는 실제 파일 복사가 아니라, 후속 복사 작업지시서 또는 PR 계획에 1안 경로가 명시되고 아래 검증 명령이 통과할 때로 한다.

점검란:

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| 8소스 `sub.css` `../font/` 9건 재확인 | [ ] 대기 | |
| CleverChat static 루트 기존 충돌 0건 재확인 | [ ] 대기 | |
| 최종 배치 경로 1안 채택 기록 | [x] 완료 | WBS §3.2 1.A-E, Phase1A_C §5.2·§5.4 |
| 후속 복사 전 경로 매핑 명령 통과 | [ ] 대기 | |

### 3.3 B5 복사 금지 해제 근거 기록

B5는 Inter/NotoSansKR 폰트 복사를 즉시 허용하는 절차가 아니다. B1~B4가 충족된 뒤, 복사 금지 상태를 해제할 근거가 문서에 명시되었는지를 확인한다.

기록 위치 후보:

| 후보 | 위치 | 기록해야 할 내용 | 판단 |
| --- | --- | --- | --- |
| A | `Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` §5.2 또는 §5.4 | OFL 사본 확보, 공식 출처 URL, B4 배치 경로, 복사 금지 해제 일자/근거 | 권고 |
| B | `1.기획/WBS.md` 1.A 항목 | B3/B4 완료와 복사 금지 해제 근거 문서 링크 | 보조 |
| C | 후속 1.A-① 구현 PR 설명 | 산출물 링크와 경로 매핑 요약 | 단독 근거로는 불충분 |

권고 기록 문구:

```text
Inter/NotoSansKR font 9건은 OFL 1.1 사본과 공식 출처 URL이 확보되고,
`sub.css`/`font/` 최종 배치 경로를 `static/asset/admmgr/style2/{css,font}/`로 확정했으므로
Phase1A_D §7 B3/B4 및 Phase1A_E B5 근거에 따라 복사 금지 상태를 해제한다.
```

점검란:

| 항목 | 상태 | 근거 |
| --- | --- | --- |
| 1.A-③ §5.2 또는 §5.4 해제 근거 기록 | [ ] 대기 | |
| WBS 보조 기록 필요 여부 판단 | [ ] 대기 | |
| PR 설명 단독 완료 처리 방지 | [ ] 대기 | |
| B1~B5 완료 순서 확인 | [ ] 대기 | |

## 4. 산출물

본 작업의 산출물은 다음 문서 1건이다.

| 산출물 | 경로 | 비고 |
| --- | --- | --- |
| B3/B4/B5 점검 작업지시서 | `3.개발/cleverchat/docs/Phase1A_E_Static_Asset_Blocker_B3_B4_B5_작업지시.md` | 본 문서 |

본문 내 점검란 4개:

- B3 공식 출처 URL 기록 검증 점검란
- B4 `sub.css`/`font/` 상대 경로 배치 확정 점검란
- B5 복사 금지 해제 근거 기록 점검란
- §8 산출물 체크리스트

## 5. 검증 명령

명령은 프로젝트 루트 `C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT` 또는 WSL 경로 `/mnt/c/02.Project/02.자바/01.WorkSpace/CLEVERCHAT`에서 실행한다.

### 5.1 B3 META 6필드 정적 점검

```bash
rg -n "^(Asset|License|Source-Primary|Source-License|Source-Checked-Date|Related-Inventory):" \
  "3.개발/cleverchat/docs/licenses/Inter-OFL.txt" \
  "3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt"
```

```bash
rg -n "https://github.com/rsms/inter|https://github.com/google/fonts|https://fonts.google.com|https://notofonts.github.io" \
  "3.개발/cleverchat/docs/licenses/Inter-OFL.txt" \
  "3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt"
```

```bash
rg -n "^Source-Checked-Date: [0-9]{4}-[0-9]{2}-[0-9]{2}$" \
  "3.개발/cleverchat/docs/licenses/Inter-OFL.txt" \
  "3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt"
```

기대 결과:

- 파일별 META 6필드가 모두 출력된다.
- Inter와 NotoSansKR 모두 공식 출처 URL이 출력된다.
- `Source-Checked-Date`가 `YYYY-MM-DD` 형식으로 출력된다.

### 5.2 B4 8소스 `sub.css` `../font/` 9건 실측

```bash
rg -n "@font-face|\\.\\./font/(Inter|NotoSansKR).+\\.(woff2|ttf)" \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/sub.css"
```

```bash
find "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/font" \
  -maxdepth 1 -type f \( -name "Inter-*.woff2" -o -name "NotoSansKR-*.ttf" \) | sort
```

기대 결과:

- `sub.css`에서 `../font/Inter-*.woff2` 5건과 `../font/NotoSansKR-*.ttf` 4건이 확인된다.
- 8소스 `style2/font/`에서 폰트 파일 9건이 확인된다.

### 5.3 B4 CleverChat static 기존 상태 확인

```bash
find "3.개발/cleverchat/src/main/resources/static" -mindepth 1 -maxdepth 6 -print | sort
```

```bash
rg -n "admin-layout.css|sub.css|ADM.Common.js|Inter-|NotoSansKR" \
  "3.개발/cleverchat/src/main/resources" \
  "3.개발/cleverchat/docs"
```

기대 결과:

- 현재 static 루트에 기존 충돌 파일이 없거나, 후속 복사 전 충돌 여부가 명확히 식별된다.
- 문서에는 `sub.css`와 font 9건의 보류/복사 금지 근거가 확인된다.

### 5.4 B4 후보 1안 경로 매핑 점검

```bash
printf '%s\n' \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/css/sub.css -> 3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/css/sub.css" \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/font/Inter-*.woff2 -> 3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/font/Inter-*.woff2" \
  "8.소스/OverseasNPP_20260511/src/main/webapp/asset/admmgr/style2/font/NotoSansKR-*.ttf -> 3.개발/cleverchat/src/main/resources/static/asset/admmgr/style2/font/NotoSansKR-*.ttf"
```

```bash
rg -n "/asset/admmgr/style2/css/sub.css|/asset/admmgr/style2/font/" \
  "3.개발/cleverchat/docs/Phase1A_Admin_Layout_Template_8소스_작업지시.md" \
  "3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md" \
  "3.개발/cleverchat/docs/Phase1A_E_Static_Asset_Blocker_B3_B4_B5_작업지시.md"
```

기대 결과:

- 후보 1안의 물리 경로와 URL이 문서에서 일관되게 확인된다.
- `sub.css`의 `../font/` 참조는 `static/asset/admmgr/style2/css/` 기준으로 `static/asset/admmgr/style2/font/`를 가리킨다.

### 5.5 B5 해제 근거 줄 추출

```bash
rg -n "복사 금지 상태를 해제|복사 금지 해제|B3|B4|B5|Inter/NotoSansKR|OFL 1.1 사본" \
  "3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md" \
  "1.기획/WBS.md"
```

기대 결과:

- 1.A-③ §5.2 또는 §5.4에 해제 근거가 있으면 해당 줄이 출력된다.
- WBS에 보조 기록이 있으면 해당 줄이 출력된다.
- 출력이 없으면 B5는 미완료로 유지한다.

## 6. 금지 범위

이번 작업에서 금지되는 작업:

- 코드 수정 금지
- 라이선스 파일 생성 금지
- 폰트 파일 복사 금지
- `sub.css` 복사 금지
- `admin-layout.css`, `ADM.Common.js` 등 기타 리소스 복사 금지
- `src/main/resources/static/asset/admmgr/style2/` 디렉터리 생성 금지
- 8소스 원본 파일 수정 금지
- `sub.css`의 `@font-face` 라인 삭제 또는 파일명 변경 금지
- `../font/` 경로를 임의로 바꾸어 블로커를 우회하는 작업 금지
- 본 문서 외 Phase1A_C, Phase1A_D, WBS 본문 수정 금지

## 7. 다음 단계 진입 조건

1.A-① scenario 화면 적용 또는 `sub.css`/font 후속 복사 단계에 진입하려면 Phase1A_D §7의 B1~B5가 모두 완료되어야 한다.

| 우선순위 | 블로커 | 진입 조건 |
| --- | --- | --- |
| 1 | B1 | Inter OFL 사본 확보 |
| 2 | B2 | NotoSansKR OFL 사본 확보 |
| 3 | B3 | 각 산출물 META 블록에 공식 출처 URL과 확인일 기록 |
| 4 | B4 | `sub.css`와 `font/` 최종 배치 경로를 1안으로 확정하고 검증 명령 통과 |
| 5 | B5 | 1.A-③ §5 또는 WBS에 복사 금지 해제 근거 기록 |

진입 가능 상태:

- B1~B5가 모두 완료되어야 `style2/css/sub.css`와 `style2/font/` 9건을 후속 복사 후보로 재판정할 수 있다.
- B3 또는 B4가 미완료이면 `sub.css` 단독 도입은 계속 보류한다.
- B5가 미완료이면 OFL 사본이 확보되어도 복사 금지 상태를 해제하지 않는다.

## 8. 산출물 체크리스트

- [x] B3 공식 출처 URL 기록 검증 항목 정의
- [x] B4 최종 배치 경로 후보 1·2·3안 정의
- [x] B4 권고안으로 `static/asset/admmgr/style2/{css,font}/` 구조 명시
- [x] B4 8소스 `style2` 구조 기준 검증 명령 포함
- [x] B5 1.A-③ §5 또는 WBS 기록 위치 정의
- [x] 코드 수정, 라이선스 파일 생성, 리소스 복사 금지 범위 명시
- [x] 다음 단계 진입 조건 정의

## 9. 1.A 단계 관계표

| 단계 | 역할 | 본 문서와의 관계 |
| --- | --- | --- |
| 1.A-③ | 정적 리소스 인벤토리 및 최종 복사 대상 확정 | B5 해제 근거의 우선 기록 위치 |
| 1.A-⑨ | 최종 복사 대상 확정 | `sub.css` 보류, font 9건 복사 금지 상태의 선행 판단 |
| 1.A-D | Inter/NotoSansKR OFL 사본 확보 | B1~B3의 입력 산출물 정의 |
| 1.A-E | B3/B4/B5 후속 블로커 점검 | 본 문서. 경로 확정 후보와 문서 검증 명령 제공 |
| 1.A-① | scenario 화면 적용 기준 정리 | B1~B5 완료 후 `sub.css`/font 도입 여부 재판정 가능 |
