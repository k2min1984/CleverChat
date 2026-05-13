# Phase 1 명명정렬 작업지시서

- 작성일: 2026-05-13
- 대상 프로젝트: `3.개발/cleverchat`
- 선행 조건: Phase 0 결정 회의에서 `templates/admin` 하위 폴더를 `scenario` 단수형으로 정렬하고, REST JSON API는 유지하기로 확정한다.
- 작업 성격: scenario 템플릿 단독 리네임 PR 지시서. 본 문서는 실제 리네임을 수행하지 않는다.

## 0. 절대 원칙

1. Phase 1 PR은 scenario 템플릿 경로/파일명과 컨트롤러 뷰 반환 문자열만 다룬다.
2. Java 패키지, 클래스명, 메서드명, DTO, 모델, 서비스, Mapper 인터페이스는 리네임하지 않는다.
3. `resources/mapper/**`, `resources/sql/**`, Flyway, 테이블명, SQL ID는 수정하지 않는다.
4. REST JSON API URL과 응답 포맷은 유지한다.
5. 본문 HTML 구조 변경, 화면 기능 변경, 대규모 정리성 리팩토링은 포함하지 않는다.

## 1. PR 범위

### 1.1 템플릿 경로 정렬

아래 파일만 `git mv`로 이동한다. 파일 본문은 경로 참조가 필요한 경우를 제외하고 변경하지 않는다.

| 현재 | 변경 후 |
|---|---|
| `src/main/resources/templates/admin/scenarios/list.html` | `src/main/resources/templates/admin/scenario/scenarioList.html` |
| `src/main/resources/templates/admin/scenarios/form.html` | `src/main/resources/templates/admin/scenario/scenarioRegist.html` |
| `src/main/resources/templates/admin/scenarios/detail.html` | `src/main/resources/templates/admin/scenario/scenarioView.html` |
| `src/main/resources/templates/admin/scenarios/preview.html` | `src/main/resources/templates/admin/scenario/scenarioPreviewLayer.html` |

### 1.2 뷰 반환 문자열 정렬

`ScenarioPageController`에서 Thymeleaf 뷰 이름만 새 경로로 맞춘다.

| 현재 후보 | 변경 후 |
|---|---|
| `admin/scenarios/list` | `admin/scenario/scenarioList` |
| `admin/scenarios/form` | `admin/scenario/scenarioRegist` |
| `admin/scenarios/detail` | `admin/scenario/scenarioView` |
| `admin/scenarios/preview` | `admin/scenario/scenarioPreviewLayer` |

### 1.3 fragment 참조 정렬

템플릿 내부에서 `admin/scenarios/*`, `scenarios/*`, `preview` 조각을 직접 참조하는 부분이 있으면 새 경로로만 바꾼다. 레이아웃, 마크업, 스크립트 로직은 변경하지 않는다.

### 1.4 변경 금지 목록

다음 경로는 Phase 1에서 수정하지 않는다.

| 금지 경로 | 사유 |
|---|---|
| `src/main/java/**/domain/scenario/**` 중 뷰 반환 문자열 외 코드 | Java 명명/구조 전환은 Phase 3 이후 대상 |
| `src/main/resources/mapper/**` | Mapper 구조 전환은 Phase 5 대상 |
| `src/main/resources/db/migration/**` | DB 변경 없음 |
| `src/main/resources/sql/**` | SQL XML 전환 없음 |
| `src/main/resources/templates/admin/**` 중 scenario 외 폴더 | 단독 PR 범위 유지 |

### 1.5 테스트 정렬

테스트가 뷰 이름 문자열을 직접 검증하고 있으면 해당 기대값만 새 뷰 이름으로 바꾼다. 테스트 시나리오, fixture, API 기대값은 변경하지 않는다.

## 2. 변경 순서

1. 브랜치 생성: `chore/phase1-scenario-templates`
2. `git mv`로 `templates/admin/scenarios` 하위 4개 파일을 `templates/admin/scenario` 하위 새 파일명으로 이동한다.
3. `ScenarioPageController`의 뷰 반환 문자열을 새 템플릿명으로 정렬한다.
4. fragment 또는 테스트의 옛 경로 참조가 있으면 문자열만 정렬한다.
5. 검증 명령과 수동 화면 점검을 완료한다.
6. PR에는 변경 범위, 검증 결과, 롤백 기준을 포함한다.

## 3. 검증 명령

PowerShell 기준으로 아래 순서대로 확인한다.

```powershell
cd C:\02.Project\02.자바\01.WorkSpace\CLEVERCHAT\3.개발\cleverchat
.\mvnw.cmd clean test
```

```powershell
.\mvnw.cmd spring-boot:run
```

기동 후 `TemplateInputException` 또는 템플릿 경로 관련 500 오류가 없는지 확인한다.

수동 진입 점검 대상은 다음 5개다.

| 화면 | 기대 결과 |
|---|---|
| 관리자 메인 | 정상 진입 |
| scenario 목록 | `scenarioList` 템플릿 렌더링 |
| scenario 등록 | `scenarioRegist` 템플릿 렌더링 |
| scenario 상세 | `scenarioView` 템플릿 렌더링 |
| scenario 미리보기/레이어 | `scenarioPreviewLayer` 템플릿 렌더링 |

잔존 참조는 아래 명령으로 확인한다.

```powershell
rg "admin/scenarios|templates/admin/scenarios|scenarios/list|scenarios/form|scenarios/detail|scenarios/preview" src
```

변경 범위 위반은 아래 명령으로 확인한다.

```powershell
git diff --name-only
```

허용 파일은 scenario 템플릿 4개, `ScenarioPageController`, 관련 테스트의 뷰 이름 검증 파일로 제한한다.

## 4. 롤백 기준

다음 중 하나라도 발생하면 PR을 되돌린다.

1. `.\mvnw.cmd clean test`에서 기존 대비 신규 실패가 발생한다.
2. 애플리케이션 기동 중 템플릿 경로 오류가 발생한다.
3. scenario 목록/등록/상세/미리보기 중 하나라도 500 오류가 난다.
4. `git diff --name-only`에 허용 범위 밖 파일이 포함된다.
5. Phase 0 결정 1 또는 결정 2가 바뀌어 단수형 정렬이나 REST JSON 유지 전제가 깨진다.

## 5. 커밋 및 PR 양식

커밋 메시지 후보:

```text
chore(phase1): scenario 템플릿 명명 정렬
```

PR 본문 필수 섹션:

```markdown
## 변경 범위
- templates/admin/scenarios -> templates/admin/scenario 단수형 정렬
- scenario 템플릿 파일명을 8소스식 Action 접미사로 정렬
- ScenarioPageController 뷰 반환 문자열 정렬

## 변경하지 않은 것
- REST JSON API
- Java 패키지/클래스/메서드명
- Mapper/XML/SQL/Flyway
- 화면 기능과 HTML 구조

## 검증
- [ ] .\mvnw.cmd clean test
- [ ] .\mvnw.cmd spring-boot:run
- [ ] scenario 목록/등록/상세/미리보기 수동 확인
- [ ] 옛 경로 rg 결과 0건
- [ ] 변경 범위 위반 없음

## 롤백 기준
- 템플릿 경로 오류, 테스트 신규 실패, 화면 500, 범위 위반 발생 시 revert
```

## 6. 후속 Phase 연결

Phase 1은 템플릿 단독 리네임만 수행한다. 이후 작업은 Phase 2 auth 명명 정렬, Phase 3 컨트롤러 명명 정렬, Phase 4 패키지 위치 정렬, Phase 5 Mapper/SQL 구조 전환 순서로 분리한다.
