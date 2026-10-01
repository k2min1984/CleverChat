"""Export PostgreSQL catalog metadata only; never read application data or change DB state.

Python 3.10+. Default: local development Docker PostgreSQL.
Use --psql for a native psql connection configured with PG* environment variables/.pgpass.
Passwords are deliberately not accepted as command-line arguments.
"""

import argparse
import datetime as dt
import json
from pathlib import Path
import re
import subprocess


APP = Path(__file__).resolve().parents[1]
ROOT = APP.parents[1]
DEFAULT_OUTPUT = ROOT / "2.설계/02.DB설계서"

TABLE_NOTES = {
    "flyway_schema_history": "Flyway 마이그레이션 적용 이력. 애플리케이션에서 직접 편집하지 않는다.",
    "spring_session": "Spring Session JDBC의 HTTP 로그인 세션. 챗봇 대화 세션과 별개다.",
    "spring_session_attributes": "HTTP 세션 속성의 직렬화 데이터. 쿠키·인증정보와 함께 민감 데이터로 취급한다.",
    "tb_admin_ip_whitelist": "관리자 접근 허용 IP/CIDR 목록.",
    "tb_chat_session_event": "시나리오 전환·노드 복귀·검색 복귀 이벤트 이력.",
    "tb_crawl_coverage": "수집 실행별 목록·상세 처리 건수 및 제한 도달 여부.",
    "tb_crawl_export_file": "시스템이 생성한 JSON 파일의 경로·해시·자동 정리 결과. 파일 본문은 서버 파일시스템에 있다.",
    "tb_crawl_job": "비동기 크롤링 작업 큐와 작업 단위 실행 상태·재시도 정보.",
    "tb_crawl_native_lib_registry": "브라우저 수집용 네이티브 라이브러리 번들의 검증·활성화 이력.",
    "tb_maintenance_run": "정리 작업의 일별 실행 점유·완료 기록.",
    "tb_scenario_node_link": "답변 노드의 원문·외부 참고 링크.",
    "tb_system_settings": "id=1 단일 행의 운영 모드·Gateway·파일 경로·런타임 설정. version은 동시 수정, auth_version은 인증 세션 무효화에 사용한다.",
}

# These are documentation annotations, not comments installed in PostgreSQL.
COLUMN_NOTES = {
    "installed_rank": "마이그레이션 설치 순번", "version": "버전", "description": "설명",
    "type": "마이그레이션 종류", "script": "마이그레이션 파일명", "checksum": "Flyway 체크섬",
    "installed_by": "설치 DB 사용자", "installed_on": "설치 시각", "execution_time": "실행 시간(ms)",
    "success": "적용 성공 여부", "primary_id": "HTTP 세션 내부 식별자", "session_id": "HTTP 세션 식별자",
    "creation_time": "세션 생성 시각(epoch ms)", "last_access_time": "최근 접근 시각(epoch ms)",
    "max_inactive_interval": "비활동 만료 간격(초)", "expiry_time": "세션 만료 시각(epoch ms)",
    "principal_name": "세션 사용자 이름", "session_primary_id": "부모 HTTP 세션 내부 식별자",
    "attribute_name": "세션 속성명", "attribute_bytes": "세션 속성 직렬화 바이트",
    "admin_ip_whitelist_no": "관리자 IP 허용 항목 식별자", "ip_cidr": "허용 IP 또는 CIDR",
    "use_yn": "사용 여부(Y/N)", "frst_regr_empno": "최초 등록자 식별값", "frst_reg_dt": "최초 등록 시각",
    "lst_chgr_empno": "최종 변경자 식별값", "lst_chg_dt": "최종 변경 시각",
    "frst_regr_ip": "최초 등록 IP", "lst_chgr_ip": "최종 변경 IP",
    "session_type": "대화 세션 구분(허용값은 CHECK 제약 참조)",
    "chat_session_event_no": "대화 이벤트 식별자", "session_no": "대화 세션 식별자",
    "event_type": "상태 전이 이벤트 종류(CHECK 참조)", "from_scenario_no": "전환 전 시나리오 식별자",
    "to_scenario_no": "전환 후 시나리오 식별자", "trigger_message_no": "전환을 유발한 메시지 식별자",
    "detail": "이벤트 부가 정보 JSON", "coverage_no": "수집 커버리지 식별자", "target_no": "크롤링 대상 식별자",
    "run_log_no": "크롤링 실행 로그 식별자", "list_pages": "수집한 목록 페이지 수",
    "list_items_found": "발견한 목록 항목 수", "details_fetched": "상세 수집 성공 건수",
    "details_failed": "상세 수집 실패 건수", "truncated_yn": "설정 제한으로 수집을 중단했는지 여부",
    "content_tokens": "한국어 형태소 분석 결과를 저장한 검색용 토큰",
    "export_file_no": "JSON 파일 등록 식별자", "root_path": "생성 당시 저장 루트의 절대 경로",
    "relative_path": "저장 루트 기준 파일 상대 경로", "content_sha256": "생성 파일 내용의 SHA-256",
    "created_at": "생성 시각", "cleaned_at": "자동 정리 완료 시각", "cleanup_error": "자동 정리 실패 내용",
    "crawl_job_no": "비동기 크롤링 작업 식별자", "trigger_type": "작업 요청 구분(CHECK 참조)",
    "status": "처리 상태(CHECK 및 서비스 로직 참조)", "requested_by": "실행 요청자",
    "requested_at": "작업 요청 시각", "started_at": "처리 시작 시각", "finished_at": "처리 종료 시각",
    "attempt_count": "시도 횟수", "message": "처리 결과 또는 오류 설명",
    "native_lib_registry_no": "네이티브 번들 등록 식별자", "bundle_version": "번들 버전",
    "original_file_name": "업로드 원본 파일명", "staging_path": "번들 검증 대기 경로",
    "active_path": "활성화된 번들 경로", "checksum_sha256": "번들 SHA-256 체크섬",
    "signature": "번들 서명 정보", "allowed_sonames": "허용 공유 라이브러리 SONAME 목록",
    "uploaded_by": "업로드 사용자", "uploaded_at": "업로드 시각", "activated_by": "활성화 사용자",
    "activated_at": "활성화 시각", "task_key": "유지보수 작업 구분 키", "run_date": "작업 기준일",
    "scenario_node_link_no": "노드 링크 식별자", "node_no": "연결된 시나리오 노드 식별자",
    "label": "화면 표시 링크 문구", "url": "링크 URL", "link_type": "링크 종류(CHECK 참조)",
    "sort_order": "표시 정렬 순서", "id": "시스템 설정 단일 행 식별자(1)",
    "operation_mode": "LOCAL(독립) 또는 GATEWAY(연계)", "gateway_url": "연계 로그인 이동 주소",
    "service_id": "Gateway 권한 헤더에서 선택할 서비스 ID", "crawl_export_directory": "크롤링 JSON 기본 저장 경로",
    "auth_version": "인증 정책 세대. 변경 시 기존 인증 세션 무효화", "updated_by": "최종 설정 변경자",
    "updated_at": "최종 설정 변경 시각", "runtime_settings": "RuntimeSetting 키별 운영 설정 JSON 객체",
    "auth_source": "계정 인증 출처(LOCAL/GATEWAY)", "emp_no": "Gateway 사번", "unit": "Gateway 조직 정보",
}

SQL = r"""
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT jsonb_build_object(
  'server_version', current_setting('server_version'),
  'database', current_database(), 'schema', :'doc_schema',
  'tables', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'name', c.relname, 'comment', obj_description(c.oid),
    'columns', (SELECT jsonb_agg(jsonb_build_object(
      'position', a.attnum, 'name', a.attname, 'type', format_type(a.atttypid,a.atttypmod),
      'nullable', NOT a.attnotnull, 'default', pg_get_expr(d.adbin,d.adrelid),
      'identity', a.attidentity, 'generated', a.attgenerated, 'comment', col_description(c.oid,a.attnum)
    ) ORDER BY a.attnum) FROM pg_attribute a LEFT JOIN pg_attrdef d
      ON d.adrelid=a.attrelid AND d.adnum=a.attnum
      WHERE a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped),
    'constraints', COALESCE((SELECT jsonb_agg(jsonb_build_object(
      'name', k.conname, 'type', k.contype, 'definition', pg_get_constraintdef(k.oid,true),
      'columns', ARRAY(SELECT a.attname FROM unnest(k.conkey) WITH ORDINALITY s(num,ord)
        JOIN pg_attribute a ON a.attrelid=k.conrelid AND a.attnum=s.num ORDER BY s.ord),
      'reference_schema', rn.nspname, 'reference_table', rc.relname,
      'reference_columns', ARRAY(SELECT a.attname FROM unnest(k.confkey) WITH ORDINALITY s(num,ord)
        JOIN pg_attribute a ON a.attrelid=k.confrelid AND a.attnum=s.num ORDER BY s.ord),
      'deferrable', k.condeferrable, 'initially_deferred', k.condeferred, 'validated', k.convalidated
    ) ORDER BY k.conname) FROM pg_constraint k LEFT JOIN pg_class rc ON rc.oid=k.confrelid
      LEFT JOIN pg_namespace rn ON rn.oid=rc.relnamespace WHERE k.conrelid=c.oid), '[]'::jsonb),
    'indexes', COALESCE((SELECT jsonb_agg(jsonb_build_object(
      'name', ic.relname, 'definition', pg_get_indexdef(i.indexrelid),
      'primary', i.indisprimary, 'unique', i.indisunique, 'valid', i.indisvalid
    ) ORDER BY ic.relname) FROM pg_index i JOIN pg_class ic ON ic.oid=i.indexrelid
      WHERE i.indrelid=c.oid), '[]'::jsonb),
    'triggers', COALESCE((SELECT jsonb_agg(jsonb_build_object('name',t.tgname,
      'definition',pg_get_triggerdef(t.oid,true)) ORDER BY t.tgname)
      FROM pg_trigger t WHERE t.tgrelid=c.oid AND NOT t.tgisinternal), '[]'::jsonb)
  ) ORDER BY c.relname) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname=:'doc_schema' AND c.relkind IN ('r','p')), '[]'::jsonb),
  'sequences', COALESCE((SELECT jsonb_agg(jsonb_build_object(
    'name', s.relname, 'type', format_type(q.seqtypid,NULL), 'start', q.seqstart,
    'increment', q.seqincrement, 'min', q.seqmin, 'max', q.seqmax, 'cycle', q.seqcycle,
    'cache',q.seqcache,'owned_by_table',tc.relname,'owned_by_column',a.attname
  ) ORDER BY s.relname) FROM pg_sequence q JOIN pg_class s ON s.oid=q.seqrelid
    JOIN pg_namespace n ON n.oid=s.relnamespace
    LEFT JOIN pg_depend dep ON dep.objid=s.oid AND dep.classid='pg_class'::regclass
      AND dep.refclassid='pg_class'::regclass AND dep.deptype IN ('a','i')
    LEFT JOIN pg_class tc ON tc.oid=dep.refobjid
    LEFT JOIN pg_attribute a ON a.attrelid=dep.refobjid AND a.attnum=dep.refobjsubid
    WHERE n.nspname=:'doc_schema'), '[]'::jsonb),
  'functions', COALESCE((SELECT jsonb_agg(jsonb_build_object('name',p.proname,
    'definition',pg_get_functiondef(p.oid)) ORDER BY p.proname,p.oid)
    FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace
    WHERE n.nspname=:'doc_schema' AND p.prokind='f'), '[]'::jsonb),
  'extensions', (SELECT jsonb_agg(jsonb_build_object('name',e.extname,'version',e.extversion,
    'schema',n.nspname) ORDER BY e.extname) FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace),
  'migrations', (SELECT jsonb_agg(jsonb_build_object('version',version,'script',script,'type',type,
    'success',success) ORDER BY installed_rank) FROM :"doc_schema".flyway_schema_history)
);
COMMIT;
"""


def cell(value):
    if value is None or value == "":
        return "—"
    return str(value).replace("|", "&#124;").replace("\r", "").replace("\n", "<br>")


def group(name):
    if name.startswith(("spring_", "flyway_")):
        return "인프라"
    for prefix, label in [("tb_scenario", "시나리오"), ("tb_chat", "챗봇"),
                          ("tb_crawl", "크롤링"), ("tb_search", "검색"),
                          ("tb_popular", "검색"), ("tb_notification", "운영")]:
        if name.startswith(prefix):
            return label
    return "설정·계정·CMS·운영"


def render(data):
    tables = data["tables"]
    counts = data["counts"]
    missing = sum(not c["comment"] for t in tables for c in t["columns"])
    lines = ["# 테이블 명세서", "", f"기준일: {data['snapshot_date']} · 작성: Codex · 기준: 실행 중인 개발 DB 카탈로그 + 현재 소스", "",
             f"- DB: `{data['database']}` / 스키마: `{data['schema']}` / PostgreSQL: `{data['server_version']}`",
             f"- 테이블 **{counts['tables']}개**, 컬럼 **{counts['columns']}개**, FK **{counts['foreign_keys']}개**, 인덱스 **{counts['indexes']}개**(PK/UNIQUE 포함), 시퀀스 **{counts['sequences']}개**",
             f"- 적용 SQL 마이그레이션: **{data['migration_comparison']['applied_count']}개**, 마지막 `{data['migrations'][-1]['script']}`. Flyway 전체 이력은 스키마 생성 등을 포함하여 {len(data['migrations'])}개. 파일명·성공 상태 대조이며 Flyway 체크섬 검증을 대체하지 않는다.",
             "- 계정·대화·수집 본문 등 업무 행은 추출하지 않았다. 시퀀스의 현재 값도 포함하지 않는다.",
             "- NULL 허용은 YES/NO, DB 기본값 없음은 `—`. PK/FK/UQ는 복합키 구성원일 수 있으므로 제약조건 전체를 함께 읽는다.",
             f"- DB 컬럼 COMMENT 미등록 **{missing}개**는 `소스 보완`으로 구분했다. 이 문서는 DB COMMENT를 변경하지 않는다.",
             "- FK 정의에서 생략된 ON DELETE/ON UPDATE는 PostgreSQL 기본 NO ACTION이다. CHECK·UNIQUE·부분 인덱스는 원문을 보존한다.",
             "- V20/V21 이전 ERD의 테이블·컬럼명보다 이 물리 명세와 최신 Flyway/MyBatis 소스를 우선한다.", "",
             "관련 문서: [DB 관계·데이터 인계 주의사항](DB관계_데이터인계.md) · [인수인계 시작](../../5.배포/06.인수인계/README.md) · [기계 판독용 스냅샷](schema-catalog.json)", "",
             "## 마이그레이션 파일 대조", "",
             f"- 소스에만 있는 SQL: {cell(', '.join(data['migration_comparison']['unapplied']))}",
             f"- DB에만 있는 SQL: {cell(', '.join(data['migration_comparison']['missing_source']))}", "",
             "## 테이블 목록", "", "| 영역 | 테이블 | 설명 | 컬럼 수 |", "|---|---|---|---:|"]
    for t in tables:
        lines.append(f"| {group(t['name'])} | [{t['name']}](#{t['name']}) | {cell(t['comment'] or TABLE_NOTES.get(t['name']))} | {len(t['columns'])} |")
    for t in tables:
        lines += ["", f"<a id=\"{t['name']}\"></a>", f"## {t['name']}", "",
                  f"- DB COMMENT: {cell(t['comment'])}"]
        if t["name"] in TABLE_NOTES:
            lines += [f"- 소스 보완: {TABLE_NOTES[t['name']]}"]
        refs = data["source_references"].get(t["name"], [])
        if refs:
            lines += ["- 참조 소스: " + ", ".join(f"[{Path(p).name}](../../3.개발/cleverchat/{p})" for p in refs)]
        lines += ["", "| 순서 | 컬럼 | 타입 | NULL 허용 | 키 | DB 기본값/생성식 | 설명 | 근거 |", "|---:|---|---|:---:|---|---|---|---|"]
        for c in t["columns"]:
            keys = [label for kind,label in [("p","PK"),("f","FK"),("u","UQ")]
                    if any(k["type"]==kind and c["name"] in k["columns"] for k in t["constraints"])]
            default = c["default"]
            if c["identity"]:
                default = "IDENTITY " + {"a":"ALWAYS", "d":"BY DEFAULT"}.get(c["identity"],c["identity"])
            if c["generated"]:
                default = "GENERATED: " + str(default)
            desc = c["comment"] or COLUMN_NOTES.get(c["name"], "업무 설명 확인 필요")
            if t["name"]=="tb_system_settings" and c["name"]=="version":
                desc = "설정 동시 수정 충돌 감지용 낙관적 잠금 버전"
            lines.append("| " + " | ".join(map(cell,[c["position"],c["name"],c["type"],"YES" if c["nullable"] else "NO",",".join(keys),default,desc,"DB COMMENT" if c["comment"] else "소스 보완"])) + " |")
        lines += ["", "### 제약조건", "", "| 이름 | 종류 | 정의 |", "|---|---|---|"]
        for k in t["constraints"]:
            lines += [f"| {k['name']} | {dict(p='PK',f='FK',u='UNIQUE',c='CHECK',x='EXCLUDE').get(k['type'],k['type'])} | {cell(k['definition'])} |"]
        if not t["constraints"]:
            lines += ["| — | — | 등록 없음 |"]
        lines += ["", "### 인덱스", "", "| 이름 | 속성 | 정의 |", "|---|---|---|"]
        for i in t["indexes"]:
            attrs = ", ".join(k for k in ("PK" if i["primary"] else "", "UNIQUE" if i["unique"] else "", "INVALID" if not i["valid"] else "") if k) or "일반"
            lines += [f"| {i['name']} | {attrs} | {cell(i['definition'])} |"]
        if not t["indexes"]:
            lines += ["| — | — | 등록 없음 |"]
        for trigger in t["triggers"]:
            lines += ["", f"트리거 `{trigger['name']}`:", "```sql", trigger["definition"], "```"]
    lines += ["", "## 시퀀스", "", "현재 할당 값은 업무 데이터이므로 추출하지 않는다. 복구 시 데이터와 시퀀스 상태를 함께 복구해야 한다.", "",
              "| 시퀀스 | 소유 테이블.컬럼 | 타입 | 시작 | 증가 | 최소 | 최대 | CACHE | CYCLE |", "|---|---|---|---:|---:|---:|---:|---:|---|"]
    for s in data["sequences"]:
        lines += ["| " + " | ".join(map(cell,[s["name"],f"{s['owned_by_table']}.{s['owned_by_column']}",s["type"],s["start"],s["increment"],s["min"],s["max"],s["cache"],s["cycle"]])) + " |"]
    lines += ["", "## 확장 및 스키마 함수", "", "확장은 DB 전체 기준이다. 함수는 지정 스키마만 대상으로 한다.", ""]
    for e in data["extensions"]:
        lines += [f"- `{e['schema']}.{e['name']}` {e['version']}"]
    for f in data["functions"]:
        lines += ["", f"### {f['name']}", "", "```sql", f["definition"].rstrip(), "```"]
    lines += ["", "## 갱신 방법", "", "프로젝트 루트(`CLEVERCHAT`)에서 Python 3.10+와 실행 중인 개발 DB로 실행한다. 이 명령은 DB를 읽기 전용 트랜잭션으로 조회하고 문서 두 개만 갱신한다.", "", "```powershell",
              'python "3.개발/cleverchat/tools/export_db_spec.py" --date YYYY-MM-DD', "```", "",
              "직접 DB 접속은 `--psql --schema <스키마>`와 PGHOST/PGPORT/PGDATABASE/PGUSER 및 안전한 인증 설정을 사용한다. 다른 환경을 조사할 때에는 `--output`으로 별도 폴더를 지정하여 개발 기준 명세를 덮어쓰지 않는다.", "",
              "작성 근거: [추출 도구](../../3.개발/cleverchat/tools/export_db_spec.py), [Flyway SQL](../../3.개발/cleverchat/src/main/resources/db/migration). DB 값·비밀번호·토큰은 문서에 기입하지 않는다.", ""]
    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--container", default="cleverchat-postgres")
    parser.add_argument("--database", default="cleverchat")
    parser.add_argument("--user", default="cleverchat")
    parser.add_argument("--schema", default="cleverchat_dev")
    parser.add_argument("--psql", action="store_true", help="Use local psql with PG* connection settings")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--date", default=dt.date.today().isoformat())
    args = parser.parse_args()
    dt.date.fromisoformat(args.date)
    if not re.fullmatch(r"[A-Za-z_][A-Za-z_0-9]*", args.schema):
        parser.error("Use an ASCII PostgreSQL schema identifier")
    command = ["psql"] if args.psql else ["docker","exec","-i",args.container,"psql","-U",args.user,"-d",args.database]
    command += ["-X","-q","-A","-t","-v","ON_ERROR_STOP=1","-v",f"doc_schema={args.schema}"]
    result = subprocess.run(command,input=SQL,encoding="utf-8",capture_output=True,check=True)
    data = json.loads(result.stdout)
    if not data["tables"] or not data["migrations"]:
        raise SystemExit("No application schema or Flyway history found; nothing written")
    if any(not m["success"] for m in data["migrations"]):
        raise SystemExit("Failed Flyway migration found; resolve before publishing a baseline")
    source_scripts = sorted(p.name for p in (APP/"src/main/resources/db/migration").glob("V*__*.sql"))
    applied_scripts = sorted(m["script"] for m in data["migrations"] if m["type"]=="SQL")
    data["migration_comparison"] = {"source_count":len(source_scripts), "applied_count":len(applied_scripts),
                                    "unapplied":sorted(set(source_scripts)-set(applied_scripts)),
                                    "missing_source":sorted(set(applied_scripts)-set(source_scripts))}
    data["snapshot_date"] = args.date
    data["counts"] = {"tables":len(data["tables"]), "columns":sum(len(t["columns"]) for t in data["tables"]),
                      "foreign_keys":sum(k["type"]=="f" for t in data["tables"] for k in t["constraints"]),
                      "indexes":sum(len(t["indexes"]) for t in data["tables"]), "sequences":len(data["sequences"])}
    sources = [(str(p.relative_to(APP)).replace("\\","/"),p.read_text(encoding="utf-8"))
               for base in [APP/"src/main/resources/mapper",APP/"src/main/java"]
               for p in sorted(base.rglob("*")) if p.suffix in (".java",".xml")]
    data["source_references"] = {t["name"]:[p for p,text in sources if re.search(r"\b"+re.escape(t["name"])+r"\b",text,re.I)] for t in data["tables"]}
    args.output.mkdir(parents=True,exist_ok=True)
    (args.output/"schema-catalog.json").write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    (args.output/"테이블명세서.md").write_text(render(data),encoding="utf-8")
    print(json.dumps({"counts":data["counts"],"migrations":data["migration_comparison"],"output":str(args.output)},ensure_ascii=False))


if __name__ == "__main__":
    main()
