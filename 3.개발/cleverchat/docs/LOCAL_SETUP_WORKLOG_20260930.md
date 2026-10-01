# CleverSearch · CLEVERCHAT 설정 및 검증 작업일지

- 시작일: 2026-09-30
- 요청: 두 프로젝트의 소스 분석, DB 및 실행 환경/VS Code 설정, 기존 시나리오 검증, 발견 결함 수정, 실사용 준비.
- CleverSearch: `C:\02.Project\01.파이썬\01.WorkSpace\CleverSearch`
- CLEVERCHAT: `C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT`
- CLEVERCHAT 앱: 위 경로의 `3.개발\cleverchat`

## 진행 상태

로컬 설치·DB·서버·VS Code 설정과 아래 시나리오 검증을 완료했다. 아래 초기 조사 기록은 당시 이력이며, 최종 결과는 문서 마지막 및 설정완료_사용안내.md를 따른다.

## 초기 조사

1. 두 프로젝트 모두 Git 저장소이며 기존 미커밋 변경 및 미추적 파일이 많다. 변경을 되돌리지 않고, 작업 전 상태 목록을 별도 보관한다.
2. CleverSearch는 Python FastAPI, SQLAlchemy, PostgreSQL, OpenSearch를 사용한다. README의 기존 실행기는 원격 DB 환경 파일을 기본 선택한다. 로컬 검증에서는 명시적 별도 환경 파일과 DB를 사용할 예정이다.
3. CleverSearch 시나리오에는 전체 데이터 초기화가 있다. 기존 데이터를 지우지 않도록 새 로컬 검증 DB/인덱스에 적용한다.
4. CLEVERCHAT은 JDK 17, Maven Wrapper, Spring Boot, MyBatis, PostgreSQL/Flyway를 사용한다. `start-local.cmd`와 초기 설치 자동화 스크립트가 이미 있다.
5. 현재 Docker Desktop은 설치되어 있으나 Linux 엔진 파이프가 없어 정지 상태로 확인됐다. 제한된 실행 환경에서는 사용자 도구 경로가 보이지 않아 시스템 접근 승인 후 다시 조사했다.
6. 승인된 시스템 조회에서는 Python 3.12와 VS Code를 확인했다. Java 명령은 PATH에서 발견되지 않았다. JDK 실제 설치 여부는 추가 조사한다.

## 변경 기록

- 본 작업일지 생성. 애플리케이션 소스/DB는 아직 변경하지 않았다.

## 검증 기록

- README, 의존성 파일, 테스트 시나리오, 기존 변경 목록 읽기.
- Docker 연결 확인: 실패(엔진 미실행). 설치되어 있지 않다는 의미는 아니다.
- 테스트 실행: 아직 시작하지 않음.

## 이슈 및 조치

| ID | 이슈 | 상태/조치 |
|---|---|---|
| ENV-01 | Docker Linux 엔진 미실행 | 기동 후 재확인 예정 |
| ENV-02 | JDK 17 명령 미발견 | 설치 위치 조사 예정 |
| SEARCH-01 | 시작 기본값이 원격 DB를 선택 | 로컬 환경 명시 및 실행 구성 준비 예정 |
| COMMON-01 | 기존 미커밋 변경 다수 | 기준 상태 기록 후 필요한 변경만 적용 |

모든 검증 결과에는 실제 실행 결과와 미실행 항목을 구분한다. 비밀번호·토큰은 작업일지에 기록하지 않는다.

## 개발 도구 설치
- Eclipse Temurin 17.0.20.1+1 Windows x64 JDK를 D:\00.WORK\AI\dev-tools\temurin17\jdk-17.0.20.1+1에 설치. 공식 API의 SHA256 일치 확인.
- VS Code Extension Pack for Java 설치.
- 공식 출처: https://adoptium.net/installation/archives 및 https://code.visualstudio.com/docs/java/java-tutorial


## 로컬 환경 구성
- 별도 PostgreSQL 15432/OpenSearch 19200을 127.0.0.1에 생성. 기존 컨테이너/볼륨 보존.
- 실습 DB cleversearch_local과 테스트 DB cleversearch_test 분리.
- .env.workstation/.env.workstation-test 생성(비밀값 임의 생성). HTTP 로컬 개발용이며 MFA 필수 옵션은 이 환경에서만 false.
- VS Code Python/Java 설정과 통합 작업영역 생성.


## CleverSearch 1차 테스트 및 수정
- 최초 pytest: 30건 중 25 통과, 5 실패.
- 실제 결함: 스케줄러 start가 호출부의 interval_seconds를 받지 못함. 간격 인자를 복원하고 실제 대기에도 반영.
- 실제 결함: 5개 API 모듈의 인덱스가 고정되어 환경변수로 지정한 인덱스와 불일치. settings.OPENSEARCH_INDEX로 통일.
- 오래된 테스트 기대값: 인증 없는 X-Role 401, 날짜 legacy keyword 필드, 용량 초과 413, v1 일괄 20개 제한을 현재 계약과 일치시킴. 제한 자체를 제거하지 않음.
- 테스트 DB 보호: 행을 삭제하는 테스트가 기본 원격 DB를 참조하지 않도록 conftest에서 별도 로컬 _test DB 확인.
- 변경 전 각 파일을 source-backups/search-first에 보관.
- `app/api/v1/file.py`
- `app/api/user/file.py`
- `app/api/v1/search.py`
- `app/api/user/search.py`
- `app/api/admin/index.py`
- `app/services/system_service.py`
- `tests/test_auth_security.py`
- `tests/test_search_chosung_mode.py`
- `tests/test_upload_limits.py`
- `tests/conftest.py`

## 실행 도구 및 추가 결과
- CleverSearch 수정 후 자동 테스트 30/30 통과.
- CLEVERCHAT 최초 단위 테스트 440개 중 오류 16개: SUBST 경로에서 상위 문서 경로 상실 6개, JDK의 8.3 임시 경로에서 Unix domain socket 연결 오류 10개. 실제 원본 경로와 명시적 Java 소켓 임시 디렉터리로 재검증 중.
- 각 앱 start-workstation.ps1 및 VS Code 실행/테스트 작업을 추가. DB는 localhost로 제한.
- CLEVERCHAT 최초 계정 localadmin과 무작위 비밀번호를 Git 제외 .env.workstation.json에 저장. 비밀번호는 로그에 출력하지 않음.


## OCR 누락 수정
- Tesseract 실행기는 있었으나 언어는 eng/osd뿐이었다. 한국어 kor 모델을 공식 tesseract-ocr/tessdata_fast에서 내려받아 별도 tessdata에 설치.
- 로컬 실행/테스트 환경에 TESSDATA_PREFIX 지정 후 이번 작업에서 시작한 Search 프로세스만 재기동.
- 출처: https://github.com/tesseract-ocr/tessdata_fast


## CLEVERCHAT 실제 HTTP에서 발견한 CSRF 결함
- 관리자 그래프 PUT에 CSRF 헤더가 없어도 200으로 저장됨. 인터셉터가 POST만 검사하던 원인 확인. 모든 비안전 메서드에 토큰 검증 및 재발급 적용. PUT/PATCH/DELETE 누락 거부와 토큰 재사용 거부 6개 회귀 테스트 추가.
- 1차 HTTP 검증에서 타 사용자 대화 조회는 실제 403으로 정상 차단. 최초 스크립트의 404 기대를 403으로 수정. 자유입력 자동 시작은 최신 코드에서 검색 세션을 만드는 정책이므로 검색 결과와 선택 전환을 검증하도록 보정.
- F5 attach 설정의 실제 디버그 포트 미기동 문제를 발견해 JDK17 직접 launch로 수정. 기존 서버와 충돌하지 않도록 8081 사용, 같은 로컬 DB 참조.

## CleverSearch 다중 업로드 검증 수정
- 2026-04-29 보안조치 문서의 20개/200MB 제한이 user 경로에는 빠져 있고 legacy 경로 실측 합계는 반환되지 않는 size 필드를 합산해 항상 0이 되는 결함 발견.
- 공통 사전 검사에서 실제 스트림 크기·개수·개별 30MB·총 200MB를 검증하고 스트림을 되감음. 초과 시 일부 문서가 먼저 색인되는 현상도 방지. 누락/위조 size, 후반 파일 용량초과, 되감기 회귀 테스트 추가.
- 기본 검색 볼륨 삭제 보호도 환경별 OPENSEARCH_INDEX 및 alias에 맞춰 수정.

## 화면 및 외부 크롤링 점검
- 검색 관리자 18개 탭 렌더링, 가중치 20→25 저장→기본값20 복원, 기본 검색 품질평가 5/5 통과. 두 화면 콘솔 오류 없음.
- 검색 카드 미리보기에 <mark> 태그가 문자열로 노출됨. 기존의 HTML 이스케이프 후 강조 함수로 두 사용자 HTML의 미리보기 처리 통일.
- CLEVERCHAT 실제 화면에서 주제→선택지→답변 링크→종료 확인. 관리자 메뉴 링크 20개 HTTP 200.
- 크롤러 전용 Chromium 131 / Playwright 1.49 런타임을 공식 CLI로 설치. 출처: https://playwright.dev/java/docs/browsers
- 한전 전자공고 실제 첫 게시물(2026-08-27 주식명의개서 정지공고)은 alt 없는 이미지 본문임을 원문 브라우저에서 확인. 기존 크롤러는 제목 보존 fallback을 지원하므로 모든 글에 줄바꿈을 강제하는 오래된 테스트를 제목/상세 URL 검증으로 정정. 문서 수·상세 수·내용 비어있지 않음·메뉴 혼입 방지 검증은 유지. 이미지 안의 텍스트 OCR은 이 Java 크롤러에서 미지원이며 완료로 판정하지 않음.
- 실제 요청 스크립트의 2차 재실행은 새 버전이 기존 그래프를 복제하는 정책 때문에 빈 그래프 기대가 맞지 않았음. 별도의 빈 초안 fixture로 분리하여 최종 57/57 통과.
- Search와 Chat F5는 각각 8001/8081을 사용해 계속 실행 중인 8000/8080과 포트 충돌 방지.

## 외부 테스트의 오래된 건수 조건 보정
- 실제 외부 테스트 6건 중 4통과/2실패: 사규 수173건(기존172), 정적 메뉴58개(기존57)로 공식 사이트가 증가해 고정 스냅샷 조건에서 실패.
- 사규는 현재 원문 목록의 내림차순 번호와 페이지 크기에서 전체 건수/페이지 수를 독립적으로 읽고 실제 수집 건수와 일치 검사. 실패0·잘림없음 조건 유지.
- 정적 메뉴는 핵심 페이지가 존재하는지 확인하고 사이트맵의 모든 발견 URL을 파싱해야 통과하도록 변경. 단순 고정 건수 업데이트 방식은 사용하지 않음.
- 출처: https://www.kepco.co.kr/home/disclosure/regulations/internalrule/boardList.do 및 https://www.kepco.co.kr/home/index.do

## 최종 상태와 변경 파일 목록

- 두 서버 최종 health 확인: Search HTTP200, Chat UP. 포트 8000/8080 및 DB15432/15433/검색19200은 localhost에 바인딩.
- Search 자동33/33 및 실제 HTTP37/37, Chat 기본 단위·통합479통과/외부6skip 및 실제 HTTP57/57. 외부6건은 별도 명시 실행 결과를 외부크롤링_검증결과.md에 기록.
- VS Code JSON 6개 파싱, 시작/중지 PowerShell 구문 검사, 비밀 환경파일5개 Git 제외 확인.
- source-backups에는 각 수정 직전 원본을 저장했다. baseline은 작업 전 기존 변경 목록이다. 기존 dirty 변경을 이번 작업 결과로 간주하지 않는다.

### CleverSearch 변경 파일

- app/api/v1/file.py, app/api/user/file.py: 환경 인덱스 사용, 실제 다중 업로드 제한 검증.
- app/api/v1/search.py, app/api/user/search.py, app/api/admin/index.py: 환경 인덱스 사용.
- app/services/system_service.py: 스케줄러 간격 인자/대기 반영, 기본 볼륨 보호 설정값 적용.
- app/services/upload_security_service.py: 공통 batch 사전 검증과 스트림 되감기.
- tests/conftest.py: 삭제형 테스트용 격리 DB guard.
- tests/test_auth_security.py, tests/test_search_chosung_mode.py: 실제 API/인덱스 계약에 맞춘 기대값.
- tests/test_upload_limits.py: 제한 계약과 실측/되감기/색인 전 차단 회귀 검증.
- static/index.html, static/user/index.html: 안전한 미리보기 하이라이트.
- .env.workstation, .env.workstation-test, .env.workstation-accounts.json: 이 장치 전용 비공개 로컬 설정.
- compose.workstation.yml: 별도 localhost 데이터 인프라.
- .vscode/settings.json, tasks.json, launch.json 및 scripts/start-workstation.ps1: 실행/테스트/디버그 환경.
- docs/LOCAL_SETUP_*_20260930.md: 사용 안내/작업/승인/검증 사본.

### CLEVERCHAT 변경 파일 (3.개발/cleverchat 기준)

- src/main/java/kr/co/cleverchat/domain/auth/security/CsrfInterceptor.java: 모든 상태 변경 메서드 CSRF 검증.
- src/test/java/kr/co/cleverchat/domain/auth/security/CsrfInterceptorTest.java: PUT/PATCH/DELETE 누락·재사용 차단6건.
- src/test/java/kr/co/cleverchat/domain/crawl/browser/KepcoBoardCrawlerIntegrationTest.java: 이미지 본문 fallback 및 실시간 공표 총건수 검증.
- src/test/java/kr/co/cleverchat/domain/crawl/service/KepcoStaticMenuIntegrationTest.java: 사이트맵 변경에 대응하며 전체 발견 메뉴 파싱 검증.
- docker/docker-compose.yml: PostgreSQL 호스트 포트127.0.0.1 바인딩.
- .gitignore: workstation 비밀파일 제외 추가.
- .env.workstation.json, .env.workstation-debug: 로컬 JDK/DB/초기 계정 및 F5 환경.
- .vscode/settings.json, tasks.json, launch.json 및 tools/start-workstation.ps1: Java17, 실제 테스트 경로, Windows 소켓, Chromium 경로, 실행/테스트/디버그.
- docs/LOCAL_SETUP_*_20260930.md: 사용 안내/작업/승인/검증 사본.

### 로컬 DB의 의도된 변경

- Search: 새 DB 스키마와 기본 계정, 준비파일5건, 시나리오 검색 로그/최근검색어, 별도 테스트DB.
- Chat: Flyway V44, 기본 메뉴/권한/코드, 신규 localadmin, 검증 분류1개/활성 시나리오1개/빈 초안1개, 검증 버전·대화·피드백·감사 로그.
- 외부 한전 테스트는 테스트 프로세스 내 읽기 수집이며 업무DB에 한전 전체 자료를 적재하지 않았다.

### 남은 범위

운영 연동 설정, Java 크롤러의 이미지 OCR, 대용량 부하 검증, 미제공 ‘동우’ 참조 자료는 사용 안내의 제한사항에 명시했다. 로컬 검증을 운영 서비스 전체 보증으로 표시하지 않는다.
## 최종 실행 확인
- 2026-09-30: 16-finalize.ps1 실행 허용·완료. 두 서버 health, localhost 포트, 비밀파일5개 Git 제외, PowerShell 및 JSON 구문 확인. MD를 두 프로젝트 docs에 동기화했고 VS Code CLI가 작업영역 열기에 성공(exit0).
- 외부 테스트 최종6개 모두 통과(4개 전체 실행+수정한2개 재검증). 기본 Chat479개와 합쳐 고유485개 테스트 검증. 증거는 외부크롤링_검증결과.md.


## 후속 설명: 남은 사항의 프로젝트 구분
- 사용자 요청에 따라 실제 설정·코드를 다시 확인. 회사 운영DB/부하/공개설정은 양쪽, 회사 자료수집은 Search, SSO·vLLM·메일·웹이미지OCR은 Chat으로 구분.
- 기존 '연결정보 없음' 표현을 실제 운영 대상 연결 검증 미수행으로 정정. Search의 기존 원격 설정이 있었음을 명시.
- 프로젝트별_남은사항_쉬운설명.md 생성 및 사용 안내 6장 수정. 소스/서버 동작은 변경하지 않음.


## 후속 요청: 2026-10-01 웹 전체 수동 시험 시나리오
- 사용자 요청에 따라 실제 메뉴/폼과 현재 코드를 대조하여 클릭 순서·예시 입력·정상 결과·복원 방법 작성.
- CleverSearch 70개(기본62/조건부8), CLEVERCHAT 88개(기본79/조건부9), 총158개. 전부 미실행인 결과표 준비.
- 관리자 Search18개/Chat20개 메뉴와 상담·검색·시나리오 작성/게시·시스템 설정5탭을 대응표에 포함.
- 내일 재부팅/절전 후 접속이 안 될 경우 Docker Desktop 및 VS Code 작업으로 시작하는 방법과 계정 파일 위치 수록.
- 신규/중복 Word와 기존5종 및 거절 자료 등 시험 파일11개 준비. 신규 Word의 텍스트 추출 확인, 중복 파일 바이트 동일 확인.
- 발견/수정: Search 화면100MB 대 서버30MB 불일치 → 안내2곳·선택 검사1곳을30MB로 일치시킴. 백업은 source-backups\20260930-manual-scenario\upload.html. 브라우저에30MB 표시 확인.
- 현행 UI 제약 기록: Search 결과의 ⋮ 동작 없음, Chat 이력 API history는JSON, 키워드/시나리오삭제/추천등록 UI 미제공. 존재하지 않는 버튼을 클릭 절차로 안내하지 않음.
- 동우 담당이라고 전달받은 Chat 시스템 설정은 화면/저장과 실제 AI·SSO 연동을 구분. 연동 정보·별도 시험 데이터가 필요한 항목은 조건부로 기록.
- 검증: manual-scenario-document-check.json. 번호158개/결과표 대응/문서 링크/Word/시험 파일 크기 검사 완료. 프로젝트docs 사본8개 해시 일치 확인.
- 이번 문서 준비에서 실제158개 수동 시나리오를 실행한 것은 아님. 결과표는 모두 미실행. 기존 DB/시나리오/권한/설정 저장 변경 없음.

## 후속 오류 신고: 검색 페이지 CleverSearch 로고 클릭
- 증상: 로고 클릭 후 https://127.0.0.1:8000/ 연결 오류.
- 원인: 현재 서버는 HTTP 200 정상 응답이나 static/index.html 및 static/user/index.html의 goHome()이 HTTPS 로컬 주소에 고정됨. 앞선 클릭 검증에서 이 동작 누락.
- 수정: 두 파일의 고정 URL 및 URL 전용 검사를 제거하고 현재 출처의 /로 이동. 기존 검색 상태 초기화 유지. 보안 설정/서버 프로토콜 변경 없음.
- 원본 백업: source-backups\20260930-logo-home. 중간 치환 잔여 구문 정리 후 두 파일 JS 구문 검사 통과.
- 검증: 연구개발계획서 검색2건→로고 마우스 클릭, 운영지침 검색3건→로고 Enter; 두 경로 모두 HTTP 홈·빈 검색창 확인. 사용자 탭 정상 홈 복구.
- 기록: 검색로고_이동오류_수정기록_20260930.md. 내일 수동 시험 CS-01에 로고 회귀 절차 추가; 사용자 결과는 미실행 유지.
- 데이터 영향: 실제 검색2회에 따른 검색 로그/최근검색어 갱신 외 문서·계정·DB구조 변경 없음. Chat 소스 변경 없음.


## 후속 오류 신고: 신규 업로드 Word 내용 깨짐
- 기존 한글 본문과 ZIP은 정상이나 XML의 선언 없는 w14/wp14 참조 발견. 실제 깨진 화면과 인과관계는 미확인.
- DOCX 두 개를 백업 후 python-docx로 재작성, 맑은 고딕 지정. 생성기도 수정. 네 문장과 검색어 유지, 중복 검사 파일 동일 바이트 확인.
- CleverSearch 실제 parse_word 함수의 정확한 한글 추출 확인. DB 업로드/삭제와 사용자 결과표 수정 없음.
- 실제 Word 페이지는 제공된 Windows 런타임에 LibreOffice가 없어 미검증. 텍스트 추출 통과를 화면 표시 보증으로 표현하지 않음.
- 본문 확인 MD 및 점검 상세 MD 작성, 시작 안내 보완. 구버전 업로드 시 새 해시와의 중복 검사 전제 확인 필요.
