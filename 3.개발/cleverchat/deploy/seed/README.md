# 기본 백데이터

작성일: 2026-09-29 · 기준: 소스의 Flyway V44까지 **빈 DB에 적용한 결과**

처음 설치해도 관리 화면을 사용할 수 있도록 메뉴·공통코드·역할·메뉴 권한을 제공한다. 일반 설치는 [README.md](../../../../README.md)의 실행 스크립트가 Flyway로 기본 데이터를 구성하고 마지막에 이 목록과 관리자 연결을 검사한다.

## 포함 범위

| 대상 | 기본 구성 | 생성 방법 |
|---|---|---|
| `tb_role` | ADMIN / OPERATOR / USER, 3개 | Flyway V1 |
| `tb_auth` | CMS 권한 그룹 3개 | V19에서 기본 역할을 권한 그룹으로 구성 |
| `tb_menu` | 메뉴 27개, 이 중 수집 문서/실행 로그 2개는 개별 메뉴 숨김 | V18/V19 + V24/V27/V32/V43 |
| `tb_auth_menu_adm` | ADMIN 27개, OPERATOR 22개, USER 0개, 총 49개 연결 | 메뉴 생성 migration |
| `tb_code` | 시스템코드 루트 1개 + 그룹 20개 + 상세 73개 = 94개 | V18/V19 + V36~V40 |
| `tb_system_settings` | id=1, LOCAL, 기본 service ID cleverchat, 런타임 설정 `{}` | V43/V44, 런타임 미저장 항목은 코드 기본값 사용 |
| `tb_user` | 신규 설치 시 입력받은 최초 관리자 1명 | `start-local.ps1` + InitialOperatorSeeder |
| `tb_user_role` | 최초 계정에 ADMIN / OPERATOR 연결 | 시더의 OPERATOR + 설치 스크립트의 최초 ADMIN 부여 |

사용자명·비밀번호는 설치할 때 정한다. 운영자/일반 사용자 역할은 기본 정의에 포함되며, 공유 비밀번호를 가진 운영자/일반 사용자 계정을 추가로 만들지는 않는다. 기존 계정이 있으면 설치 스크립트는 계정·비밀번호·역할을 유지한다.

`tb_role`은 로그인 역할이고 `tb_auth`는 CMS 메뉴 권한 그룹이다. 사용자 → 로그인 역할과 권한 그룹 → 메뉴 연결이 모두 있어야 한다. 메뉴의 CRUD 표시 플래그와 별도로 서버의 역할 검사를 적용한다. USER는 관리자 메뉴 권한이 없으며 상담 사용자 역할이다.

## 파일

| 파일 | 용도 |
|---|---|
| [basic-data-manifest.json](basic-data-manifest.json) | 깨끗한 최초 설치의 역할/권한 그룹/메뉴/코드/메뉴 연결 값. 사용자 행·비밀번호·SSO 설정·업무 내용 제외 |
| [check-basic-data.sql](check-basic-data.sql) | 읽기 전용 검사: 필수 역할·메뉴 URL·코드 그룹/상세·메뉴별 권한, 끊어진 상위 연결, 시스템 설정, LOCAL 관리자 |
| [basic-data.sql](basic-data.sql) | V44 이상 구조에 **CMS 기본 데이터가 전부 비어 있을 때** 넣는 별도 초기화 SQL |
| [생성 도구](../../tools/build_basic_data.py) | manifest를 검증하고 위 두 SQL을 다시 생성. DB 접속/변경 없음 |

시나리오 본문·수집 문서·대화·실제 사용자/접속 이력은 기관별 업무 데이터다. 이 백데이터 묶음에는 포함되지 않으므로 [데이터 인계 기준](../../../../2.설계/02.DB설계서/DB관계_데이터인계.md)에 따라 별도로 넘긴다.

## 설치 시 확인 방식

`start-local.cmd`의 5단계에서 최초 관리자 생성/연결 후 아래처럼 건수를 표시한다.

```text
[INFO] Base data: roles=3, auth groups=3, menus=27, codes=94, menu permissions=49, users=1.
[OK] Default menus/codes/roles/menu permissions and the administrator link are ready.
```

건수만 맞춰 보는 것이 아니라 필수 키와 상위 관계, 메뉴별 권한 플래그를 비교한다. 최초 계정 준비 시 누락이 발견되면 READY 전에 중단한다. 기존 설치에 의도적인 메뉴/권한 변경이 있으면 차이를 경고하며 기존 설정을 자동 복원하거나 재승격하지 않는다.

`-CheckOnly`에서도 V43 이상 구조가 있으면 읽기 전용 기본 데이터 검사를 한다. 과거 버전/빈 DB는 마이그레이션 후에 검사한다. `-CheckOnly`의 종료 코드 0은 연결/실행 전 점검 완료를 뜻하며, 기존 설정 차이는 경고 내용을 함께 확인한다.

## 별도 SQL이 필요한 경우

일반적인 최초 설치는 Flyway가 이 데이터를 넣는다. 데이터 없이 구조만 이관한 환경에서 기본 CMS 데이터를 준비할 때 별도 SQL을 사용한다. DB 접속 대상/스키마를 먼저 확인한다. 스키마는 현행 migration과 동일하게 `cleverchat_dev`다.

아래 명령은 **앱 루트 `3.개발/cleverchat`**, PostgreSQL 클라이언트 설치 및 PGHOST/PGPORT/PGDATABASE/PGUSER/안전한 인증 설정이 준비된 환경을 기준으로 한다.

```powershell
# 먼저 읽기 전용 검사
psql -X -v ON_ERROR_STOP=1 -f deploy/seed/check-basic-data.sql

# 구조는 있고 CMS의 네 테이블이 모두 비어 있을 때만 초기화
psql -X -v ON_ERROR_STOP=1 -f deploy/seed/basic-data.sql
psql -X -v ON_ERROR_STOP=1 -f deploy/seed/check-basic-data.sql
```

동작은 다음과 같다.

- `tb_auth`, `tb_menu`, `tb_code`, `tb_auth_menu_adm`이 모두 비어 있으면 기본값을 넣는다. 기본 역할은 없는 코드만 추가하며 시스템 설정이 이미 있으면 유지한다.
- 네 테이블이 모두 채워져 있으면 그대로 두고 종료한다. 재실행해도 행이 중복 생성되지 않는다.
- 일부 테이블만 비어 있으면 오류로 중단한다. 일부 행만 누락되었거나 권한을 줄인 환경에도 임의로 권한을 다시 부여하지 않으므로 검사 결과에 따라 복구 범위를 결정한다.
- 전체 작업을 트랜잭션으로 처리하고, 메뉴/코드/권한 그룹 시퀀스를 맞춘다. 사용자·비밀번호·사용자 역할은 이 SQL로 변경하지 않는다.
- 시스템 설정 행이 이미 있으면 LOCAL/GATEWAY 모드·경로·운영값을 덮어쓰지 않는다.

따라서 SQL만 넣은 뒤 `missing_local_admin`이 나오면 [최초 계정 준비](../../../../5.배포/02.환경구축가이드/환경구축_배포가이드.md#최초-계정-준비)를 수행한다. 기존 사용자를 공용 관리자 계정으로 교체하지 않는다.

## 기본 메뉴·권한 목록

아래 이름은 DB 원본 값이다. 화면에서 표시 문구를 별도로 가공할 수 있다. ADMIN/OPERATOR는 기본 조회 연결을 뜻하며 세부 insert/update/delete/proc 값은 manifest/SQL에 포함되어 있다. USER의 관리자 메뉴 연결은 없다.

| 상위 | 메뉴 원본명 | URL | 표시 | ADMIN | OPERATOR |
|---|---|---|---|---|---|
| 최상위 | Dashboard | /admin | Y | Y | Y |
| 최상위 | Scenario | 그룹 | Y | Y | Y |
| 최상위 | Chat operations | 그룹 | Y | Y | Y |
| 최상위 | Search and crawl | 그룹 | Y | Y | Y |
| 최상위 | Operations | 그룹 | Y | Y | Y |
| 최상위 | CMS management | 그룹 | Y | Y | Y |
| Scenario | Scenario list | /admin/scenarios | Y | Y | Y |
| Scenario | 시나리오 정렬 | /admin/scenarios/order | Y | Y | Y |
| Chat operations | Chat sessions | /admin/chat/sessions | Y | Y | Y |
| Chat operations | Failure queue | /admin/chat/failures | Y | Y | Y |
| Chat operations | Feedback | /admin/chat/feedback | Y | Y | Y |
| Chat operations | Recommended questions | /admin/chat/recommendations | Y | Y | Y |
| Search and crawl | Search logs | /admin/search/logs | Y | Y | Y |
| Search and crawl | PII block logs | /admin/search/blocks | Y | Y | Y |
| Search and crawl | Popular queries | /admin/search/popular | Y | Y | Y |
| Search and crawl | 크롤링 관리 | /admin/crawl-targets | Y | Y | Y |
| Search and crawl | Crawl documents | /admin/crawl-documents | N | Y | Y |
| Search and crawl | Crawl runs | /admin/crawl-runs | N | Y | Y |
| Operations | Statistics | /admin/statistics | Y | Y | Y |
| Operations | Audit logs | /admin/audit-logs | Y | Y | Y |
| Operations | Notices | /admin/notices | Y | Y | Y |
| Operations | Notifications | /admin/notifications | Y | Y | Y |
| CMS management | Code management | /admin/manage/codes | Y | Y | — |
| CMS management | Menu management | /admin/manage/menus | Y | Y | — |
| CMS management | Permission management | /admin/manage/permissions | Y | Y | — |
| CMS management | 관리자 IP 허용목록 | /admin/manage/ip-whitelist | Y | Y | — |
| CMS management | 시스템 설정 | /admin/system-settings | Y | Y | — |

## 공통코드 그룹

시스템코드 루트 아래 20개 그룹이다. 상세 코드 키·값·명칭·정렬은 manifest 및 초기화 SQL에서 확인한다.

| 그룹 키 | 이름 | 상세 개수 |
|---|---|---:|
| NOTICE_STATUS | 공지 노출 상태 | 2 |
| NOTIFICATION_CHANNEL_TYPE | 알림 채널 유형 | 3 |
| CRAWL_STATUS | 크롤링 결과 상태 | 3 |
| SCENARIO_STATUS | 시나리오 상태 | 4 |
| SCENARIO_VERSION_STATUS | 시나리오 버전 상태 | 3 |
| SCENARIO_NODE_TYPE | 시나리오 노드 유형 | 4 |
| SCENARIO_LINK_TYPE | 시나리오 링크 유형 | 3 |
| CHAT_SESSION_STATE | 채팅 세션 상태 | 4 |
| CHAT_SESSION_TYPE | 채팅 세션 유형 | 2 |
| CHAT_MESSAGE_DIRECTION | 채팅 메시지 방향 | 3 |
| CHAT_FEEDBACK_RATING | 채팅 답변 평가 | 2 |
| CHAT_FAILURE_REASON | 채팅 실패 사유 | 6 |
| CRAWL_FAILURE_CODE | 크롤링 실패 코드 | 12 |
| SEARCH_SOURCE | 검색 요청 출처 | 3 |
| CRAWL_SCHEDULE_MODE | 크롤링 일정 방식 | 2 |
| CRAWL_JOB_TRIGGER_TYPE | 크롤링 작업 실행 유형 | 2 |
| CRAWL_JOB_STATUS | 크롤링 작업 상태 | 5 |
| NOTIFICATION_EVENT_SEVERITY | 알림 심각도 | 3 |
| NOTIFICATION_EVENT_STATUS | 알림 발송 상태 | 4 |
| CRAWL_NATIVE_LIB_STATUS | 크롤러 라이브러리 상태 | 3 |

## 기본값 변경 시

1. 신규 Flyway SQL로 기본값 변경을 구현한다. 이미 적용된 migration 파일은 수정하지 않는다.
2. 별도 빈 DB에 전체 migration을 적용하고, 위 다섯 테이블의 명시된 필드만 manifest에 반영한다. 실제 운영 DB나 계정 덤프를 기본값으로 사용하지 않는다.
3. `python tools/build_basic_data.py`로 SQL 두 개를 갱신한다. 설치 실행에는 Python이 필요하지 않다.
4. 새 설치의 키/권한 관계, 기존 설정 재실행 보존, 빈 CMS 초기화, 부분 누락 거부, 시퀀스 충돌을 검증하고 기준 버전/목록을 갱신한다.
