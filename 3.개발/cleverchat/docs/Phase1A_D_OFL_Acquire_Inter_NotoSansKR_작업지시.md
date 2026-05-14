# 1.A-D Inter/NotoSansKR OFL 사본 확보 작업지시서

> 작성일: 2026-05-14  
> 기준 문서: `3.개발/cleverchat/docs/Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` 부록 D 4·5번  
> 대상 프로젝트: `3.개발/cleverchat`  
> 작업 원칙: 이 문서는 작업지시서이다. 코드 수정, 리소스 복사, 파일 이동은 수행하지 않는다.

## 1. 목적과 핵심 판단

본 문서는 8소스 `style2/font/` 하위 Inter family 5건과 NotoSansKR family 4건을 후속 도입 후보로 재검토하기 전에 필요한 SIL Open Font License 1.1 사본 확보 절차를 정의한다.

핵심 판단은 다음과 같다.

- Inter와 NotoSansKR은 OFL 1.1 사본이 프로젝트에 동봉되기 전까지 복사 금지 상태를 유지한다.
- 확보 대상은 OFL 1.1 영문 원문이다. 번역본, 요약본, 블로그 설명, 패키지 매니저 메타데이터만으로는 대체하지 않는다.
- 공식 출처 URL과 확인일을 라이선스 사본 상단 META 블록에 기록한다.
- 산출물은 `docs/licenses/Inter-OFL.txt`, `docs/licenses/NotoSansKR-OFL.txt` 2건으로 고정한다.
- 본 작업은 라이선스 사본 확보 작업지시이며, 폰트 파일 복사 또는 `sub.css` 도입을 승인하지 않는다.

## 2. 대상 자산

대상 자산은 `Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md` 부록 D 4·5번의 복사 금지 항목이다.

| 구분 | 8소스 파일 | 건수 | 현재 판단 | 해제 전제 |
| --- | --- | ---: | --- | --- |
| Inter family | `style2/font/Inter-Regular.woff2`, `Inter-Medium.woff2`, `Inter-SemiBold.woff2`, `Inter-Bold.woff2`, `Inter-ExtraBold.woff2` | 5 | 복사 금지 | Inter OFL 1.1 사본 및 공식 출처 URL 확보 |
| NotoSansKR family | `style2/font/NotoSansKR-Regular.ttf`, `NotoSansKR-Medium.ttf`, `NotoSansKR-SemiBold.ttf`, `NotoSansKR-Bold.ttf` | 4 | 복사 금지 | NotoSansKR OFL 1.1 사본 및 공식 출처 URL 확보 |

## 3. 공식 출처 URL 기록 정책

공식 출처는 다음 우선순위로 기록한다.

| 구분 | 1순위 공식 출처 | 보조 확인 출처 | 기록 방식 |
| --- | --- | --- | --- |
| Inter | `https://github.com/rsms/inter` | `https://github.com/google/fonts/blob/main/ofl/inter/OFL.txt`, `https://fonts.google.com/specimen/Inter` | 1순위 URL과 실제 OFL 원문 URL을 모두 남긴다. |
| NotoSansKR | `https://fonts.google.com/noto/specimen/Noto+Sans+KR` | `https://github.com/google/fonts`, `https://notofonts.github.io/noto-docs/website/use/` | Google Fonts 표본 URL과 실제 OFL 원문 URL을 모두 남긴다. |

URL 기록 원칙:

- 웹 UI URL과 raw 원문 URL을 구분해 기록한다.
- 확인일은 `YYYY-MM-DD` 형식으로 기록한다.
- GitHub 원문을 사용할 경우 branch 또는 commit 기준을 기록한다.
- 다운로드 ZIP에서 확보한 경우 ZIP 다운로드 URL, ZIP 파일명, 내부 LICENSE/OFL 경로를 기록한다.
- 출처가 리다이렉트되면 최초 접근 URL과 최종 도착 URL을 모두 기록한다.

## 4. 산출물 정의

산출물 경로는 다음으로 고정한다.

```text
3.개발/cleverchat/docs/licenses/Inter-OFL.txt
3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt
```

각 파일은 UTF-8, LF, BOM 없음으로 저장한다. 파일 상단에는 8~10줄의 META 블록을 두고, 그 아래에 OFL 1.1 영문 원문을 변경 없이 붙인다.

META 블록 형식:

```text
Project: CleverChat
Asset: Inter
License: SIL Open Font License, Version 1.1
Source-Primary: https://github.com/rsms/inter
Source-License: <OFL 원문 URL 또는 ZIP 내부 경로>
Source-Checked-Date: 2026-05-14
Acquired-By: <작업자 또는 PR 번호>
Related-WBS: 1.A-D
Related-Inventory: Phase1A_C_Static_Asset_Inventory_8소스_작업지시.md 부록 D 4번

SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007
...
```

NotoSansKR 파일은 `Asset`, `Source-Primary`, `Source-License`, `Related-Inventory`만 NotoSansKR 기준으로 바꾼다.

## 5. 작업 절차

1. 공식 출처 URL을 연다.
2. OFL 1.1 원문 파일 또는 공식 ZIP 내부의 OFL/LICENSE 파일을 찾는다.
3. 해당 원문이 "SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007"을 포함하는지 확인한다.
4. 원문 상단의 copyright, reserved font name, license notice를 삭제하지 않고 보존한다.
5. `3.개발/cleverchat/docs/licenses/` 디렉터리를 준비하고 산출물 2건을 생성한다.
6. 각 산출물 상단에 META 블록을 추가한다.
7. 본문에는 OFL 1.1 영문 원문을 줄바꿈과 문구 변경 없이 붙인다.
8. 인코딩, BOM, 핵심 키워드, 출처 URL 기록 여부를 검증한다.
9. 검증 결과와 산출물 경로를 PR 설명 또는 작업 로그에 기록한다.

## 6. 검증 명령

PowerShell 기준 검증 명령은 다음과 같다.

```powershell
$files = @(
  "3.개발/cleverchat/docs/licenses/Inter-OFL.txt",
  "3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt"
)

foreach ($file in $files) {
  if (!(Test-Path $file)) { throw "Missing: $file" }
  $bytes = [System.IO.File]::ReadAllBytes($file)
  if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
    throw "BOM detected: $file"
  }
  $text = [System.IO.File]::ReadAllText($file, [System.Text.UTF8Encoding]::new($false))
  if ($text -notmatch "SIL OPEN FONT LICENSE Version 1\.1") { throw "Missing OFL title: $file" }
  if ($text -notmatch "26 February 2007") { throw "Missing OFL date: $file" }
  if ($text -notmatch "Source-Primary: https://") { throw "Missing Source-Primary: $file" }
  if ($text -notmatch "Source-License: ") { throw "Missing Source-License: $file" }
}
```

추가 수동 확인:

- Inter 산출물에는 `Inter` 또는 `The Inter Project Authors` 출처가 확인되어야 한다.
- NotoSansKR 산출물에는 Google Fonts, Noto, 또는 Noto Sans KR 공식 출처가 확인되어야 한다.
- OFL 본문 내 조항 번호 1~5와 TERMINATION, DISCLAIMER 절이 누락되지 않아야 한다.

## 7. 1.A-① 착수 전 블로커 조건

1.A-① scenario 화면 적용 또는 관리자 레이아웃 착수 전에 다음 5개 조건이 모두 해제되어야 한다.

| 번호 | 해제 조건 | 판정 기준 |
| --- | --- | --- |
| B1 | Inter OFL 사본 확보 | `3.개발/cleverchat/docs/licenses/Inter-OFL.txt` 존재 및 검증 통과 |
| B2 | NotoSansKR OFL 사본 확보 | `3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt` 존재 및 검증 통과 |
| B3 | 공식 출처 URL 기록 | 각 산출물 META 블록에 `Source-Primary`, `Source-License`, `Source-Checked-Date` 기록 |
| B4 | `sub.css`와 `font/` 상대 경로 배치 확정 | `sub.css`의 `../font/` 참조를 깨지 않는 도입 경로가 작업지시서에 명시됨 |
| B5 | 복사 범위 재판정 | 1.A-③ §5 또는 후속 WBS에 Inter/NotoSansKR 복사 금지 해제 근거가 기록됨 |

우회 금지 항목은 다음과 같다.

- OFL 사본 없이 Inter/NotoSansKR 폰트 파일만 먼저 복사하지 않는다.
- OFL 사본 없이 `sub.css`를 먼저 도입하지 않는다.
- `@font-face` 라인을 삭제하거나 폰트 파일명을 바꾸는 방식으로 블로커를 우회하지 않는다.
- PR 설명에 URL만 남기고 `3.개발/cleverchat/docs/licenses/` 산출물을 생략하지 않는다.

차단 범위:

- 차단 대상: `style2/font/Inter-*.woff2`, `style2/font/NotoSansKR-*.ttf`, 해당 폰트를 필수 참조하는 `sub.css` 도입.
- 비차단 대상: `admin-layout.css`, `ADM.Common.js`처럼 폰트 OFL 사본과 직접 관련 없는 1.A-③ 도입 확정 항목.

## 8. 산출물 체크리스트

- [ ] `3.개발/cleverchat/docs/licenses/Inter-OFL.txt` 생성
- [ ] `3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt` 생성
- [ ] Inter 공식 출처 URL 기록
- [ ] NotoSansKR 공식 출처 URL 기록
- [ ] OFL 1.1 영문 원문 제목 확인
- [ ] OFL 1.1 본문 조항 누락 여부 확인
- [ ] UTF-8 LF, BOM 없음 확인
- [ ] PR 설명 또는 작업 로그에 산출물 경로 기록

## 9. 금지 범위

본 작업에서 금지하는 작업은 다음과 같다.

- 코드 수정 금지
- 리소스 복사 금지
- 폰트 파일 이동 금지
- `sub.css` 수정 금지
- OFL 본문 번역 또는 요약본으로 대체 금지
- 공식 출처가 아닌 2차 배포 사이트의 라이선스 본문만 사용 금지
- 라이선스 파일명 또는 경로 임의 변경 금지

## 10. 산출물

본 작업지시 완료 시 기대 산출물은 다음 2건이다.

```text
3.개발/cleverchat/docs/licenses/Inter-OFL.txt
3.개발/cleverchat/docs/licenses/NotoSansKR-OFL.txt
```

본 문서는 위 산출물의 생성 기준을 정의할 뿐이며, 현재 단계에서 산출물 자체를 생성하지 않는다.

## 11. 1.A 단계 관계표

| 단계 | 역할 | 본 문서와의 관계 |
| --- | --- | --- |
| 1.A-① | scenario 화면 적용 기준 정리 | OFL 사본 미확보 시 `sub.css` 및 폰트 도입 착수 차단 |
| 1.A-③ | 정적 리소스 인벤토리 및 최종 복사 대상 확정 | 부록 D 4·5번의 복사 금지 해제 조건을 본 문서가 구체화 |
| 1.A-⑨ | 최종 복사 대상 확정 | 도입 확정 2, 보류 4, 추가 확인 1, 복사 금지 4그룹 11건 기준 유지 |
| 1.A-D | Inter/NotoSansKR OFL 사본 확보 | `docs/licenses/` 산출물 2건 확보 후 후속 복사 재판정 가능 |
