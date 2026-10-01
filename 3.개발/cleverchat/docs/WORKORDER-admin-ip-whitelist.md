# 작업지시서 — 관리자 IP 화이트리스트(단일 IP + CIDR) + 액션 감사

> 대상 실행자: Codex
> 성격: 관리자(`/admin/**`) 접근을 **IP 허용목록**으로 제한. 단일 IP와 **CIDR 대역** 모두 지원. **자기 잠금(lockout) 방지 2중 안전장치** 포함. 변경은 **기존 감사 인프라**로 기록. DMZ(프록시 뒤) 환경 가정.
> 불변: 기존 챗봇/검색/시나리오/크롤 로직과 무관. 기존 감사·인증 인프라는 **재사용**(중복 생성 금지).

---

## 0. 목표
DMZ에 노출되는 관리자 화면을 **허용 IP/대역만 접근**하도록 제한한다. 관리자가 화면에서 허용목록(단일 IP·CIDR)을 관리하고, 모든 변경은 감사로그에 남는다. 잘못 설정해도 **관리자가 잠기지 않도록** 안전장치를 둔다.

---

## 1. 기존 자산 (재사용 — 새로 만들지 말 것)
- **감사(액션 히스토리)는 이미 완비**: `common/audit/`의 `@Audited(action, targetType)` AOP → `tb_audit_log`(actor, action, target_type, target_id, detail JSONB, **ip**, frst_reg_dt). 조회 화면 `/admin/audit-logs`(`domain/ops/controller/AdmOpsController` + `OpsService.auditLogs` + `OpsMapper.findAuditLogs`). → **본 기능의 "액션 히스토리" 요구는 새 액션에 `@Audited` 부착만으로 충족**(인프라/화면 신규 불필요).
- **IP 추출**: `AuditAspect`/`AuditTrailRecorder`가 `X-Forwarded-For` 우선 → 없으면 `getRemoteAddr()` 패턴 보유.
- **포워드 헤더**: `application.yml`에 `server.forward-headers-strategy: framework` 설정됨 → `HttpServletRequest.getRemoteAddr()`가 **프록시 뒤 실제 클라이언트 IP**를 반영(ForwardedHeaderFilter). **본 기능은 이 framework 해석 IP를 표준으로 사용**(원시 XFF 직접 파싱보다 일관·안전).
- **인증/인가**: `domain/auth/security/AuthInterceptor`(세션 인증 + `AdminManageService.canAccessPath` 메뉴권한), `@RequireRole`.
- **관리 도메인**: `domain/adminmanage`(코드/메뉴/권한 관리, `/admin/manage/*`). 본 화면도 여기에 일관되게 추가.
- **캐시**: Caffeine 사용 중(매 요청 검사라 캐싱 권장).
- **컬럼 컨벤션**(adminmanage 테이블): `frst_regr_empno, frst_reg_dt, lst_chgr_empno, lst_chg_dt, frst_regr_ip, lst_chgr_ip, use_yn`.
- **주의**: 프로젝트엔 `spring-security-web`이 없음(전체 시큐리티 스타터 미사용) → `IpAddressMatcher` 못 씀. **CIDR 매칭 유틸을 직접 구현**(아래 3.5).

---

## 2. DB 마이그레이션
신규 `Vxx__admin_ip_whitelist.sql` — **버전번호는 착수 시 `db/migration` 최대값+1**(크롤러 작업지시서가 V31을 쓰면 본건은 그 다음 번호로 충돌 회피).
```sql
CREATE TABLE tb_admin_ip_whitelist (
    admin_ip_whitelist_no BIGSERIAL PRIMARY KEY,
    ip_cidr        varchar(64) NOT NULL,   -- 단일 IP(1.2.3.4) 또는 CIDR(1.2.3.0/24, IPv6 가능)
    description    varchar(200),
    use_yn         char(1) NOT NULL DEFAULT 'Y',
    frst_regr_empno varchar(64),
    frst_reg_dt    timestamptz NOT NULL DEFAULT now(),
    lst_chgr_empno varchar(64),
    lst_chg_dt     timestamptz NOT NULL DEFAULT now(),
    frst_regr_ip   varchar(64),
    lst_chgr_ip    varchar(64),
    CONSTRAINT ck_admin_ip_whitelist_useyn CHECK (use_yn IN ('Y','N'))
);
CREATE INDEX ix_admin_ip_whitelist_useyn ON tb_admin_ip_whitelist (use_yn);
```
- (선택) 메뉴 등록: 기존 `tb_menu` 패턴으로 `/admin/manage/ip-whitelist` 메뉴 1행 추가(권한 관리에서 노출 제어). 가산적.
- 마이그레이션은 가산적만. 데이터 삭제 금지.

---

## 3. 구현 상세

### 3.1 모델 / 매퍼
- `domain/adminmanage/model/AdminIpWhitelist.java`(필드: adminIpWhitelistNo, ipCidr, description, useYn, 등록/변경 메타).
- `domain/adminmanage/mapper/AdminIpWhitelistMapper.java`(+ XML): `findActive()`(use_yn='Y'), `findAll()`, `findById`, `insert`, `update`, `disable`(use_yn='N'). (adminmanage 매퍼 컨벤션 따름)

### 3.2 서비스 — `AdminIpWhitelistService`
- `boolean isAllowed(String clientIp)`:
  - **안전장치 A(빈 목록=전체 허용)**: 활성(use_yn='Y') 항목이 **0건이면 true**(기능 설정 전엔 아무도 안 잠김).
  - **브레이크글라스**: 루프백(`127.0.0.1`, `::1`)은 **항상 허용**(서버 로컬/온서버 접근 보장).
  - 그 외: 활성 목록의 각 항목과 매칭(단일 IP는 정확 일치, CIDR는 대역 포함) 중 하나라도 맞으면 true.
  - **캐싱**: 활성 목록을 Caffeine로 캐시(예: TTL 60초 또는 변경 시 무효화). 매 admin 요청 검사 부하 완화.
- CRUD: `addEntry/updateEntry/disableEntry` — 각각 `@RequireRole`(ADMIN) + **`@Audited`**:
  - `@Audited(action="ADMIN_IP_WHITELIST_ADD",    targetType="ADMIN_IP_WHITELIST")`
  - `@Audited(action="ADMIN_IP_WHITELIST_UPDATE", targetType="ADMIN_IP_WHITELIST")`
  - `@Audited(action="ADMIN_IP_WHITELIST_DISABLE",targetType="ADMIN_IP_WHITELIST")`
  - → 기존 감사 인프라가 자동 기록 + `/admin/audit-logs`에 노출.
- **안전장치 B(현재 IP 보호 — 자기 잠금 방지)**: add/update/disable로 **결과 활성목록을 산출**했을 때, **요청 관리자의 현재 IP가 그 결과로 허용되지 않으면 변경을 거부**하고 명확한 오류 반환(예: "현재 접속 IP가 허용목록에서 제외되어 변경할 수 없습니다"). → 관리자가 스스로를 잠그는 실수 차단. (현재 IP는 컨트롤러에서 `request.getRemoteAddr()`로 전달.)
- 변경 시 캐시 무효화.

### 3.3 enforcement 필터 — `AdminIpWhitelistFilter`(서블릿 Filter)
- 대상: `/admin/**`(관리자 페이지 + 관리자 API 경로). 챗봇/검색 등 일반 경로는 **미적용**.
- 클라이언트 IP = `request.getRemoteAddr()`(framework forward-headers로 실제 IP 해석됨). 
  - **XFF 위조 주의**: forward-headers-strategy=framework는 신뢰 프록시 체인 기준으로 해석함. 원시 `X-Forwarded-For`를 직접 신뢰 파싱하지 말 것(클라이언트 위조 가능). framework 해석값 사용.
- `adminIpWhitelistService.isAllowed(ip)` false면 **403**(로그인 화면 이전에 차단) + (선택)ops/audit에 "ADMIN_IP_BLOCKED" 기록(actor unknown 가능).
- **순서**: 인증 인터셉터보다 앞(또는 충분히 높은 우선순위 Filter)에서 IP를 먼저 차단. `SecurityHeadersFilter` 등 기존 필터와 순서 충돌 없게 `@Order` 지정.
- 기능 토글: `cleverchat.admin.ip-whitelist.enabled`(기본 true, 단 안전장치 A로 빈 목록이면 무해). 비상 시 false로 전체 우회 가능.

### 3.4 관리 화면 (adminmanage 도메인)
- 컨트롤러 `/admin/manage/ip-whitelist`(목록/추가/수정/비활성), `/admin/manage/*` 기존 패턴·템플릿 스타일과 일관.
- 화면: 목록(IP/CIDR, 설명, 사용여부, 등록자/일시) + 추가/수정 폼 + 비활성 처리.
- 입력 검증: ipCidr 형식 검증(단일 IP 또는 CIDR), 설명 길이.
- 변경은 서비스의 @Audited 경로를 타게.

### 3.5 CIDR 매칭 유틸 — `IpCidrMatcher`(common 또는 adminmanage util)
- `boolean matches(String ipCidrRule, String clientIp)`:
  - 규칙에 `/` 없으면 **단일 IP 정확 일치**(InetAddress 정규화 비교 — `1.2.3.4` 등).
  - `/` 있으면 **CIDR 포함 검사**: `InetAddress`로 파싱, 프리픽스 길이만큼 바이트 비교(마스킹). **IPv4·IPv6 모두 지원**(주소 패밀리 다르면 false).
  - 잘못된 형식은 false(예외 던지지 말고 안전 실패) + 입력 단계 검증에서 사전 차단.
- 표준 라이브러리(`java.net.InetAddress`)만으로 구현(외부 의존성 추가 금지).

---

## 4. 동작 요구 (수용 기준)
1. 활성 허용목록 0건 → 모든 IP 허용(기능 무해, 설정 전).
2. 항목 있으면 → 매칭 IP/대역만 `/admin/**` 접근, 그 외 403.
3. 루프백은 항상 허용(브레이크글라스).
4. 단일 IP·CIDR(IPv4/IPv6) 모두 정상 매칭.
5. 관리자가 자신의 현재 IP를 제외하는 변경 시도 → **거부 + 경고**(자기 잠금 방지).
6. 추가/수정/비활성 모두 `tb_audit_log`에 기록되고 `/admin/audit-logs`에서 조회됨.
7. DMZ 프록시 뒤에서 **실제 클라이언트 IP** 기준으로 판정(프록시 IP 아님).
8. 기존 챗봇/검색/시나리오/크롤/일반 사용자 경로 영향 없음.

---

## 5. 테스트
- `IpCidrMatcher`: 단일 IP 일치/불일치, CIDR 포함/제외, IPv4·IPv6, 잘못된 형식 안전 실패.
- 서비스: 빈 목록→전체 허용, 루프백 허용, 매칭 동작, **자기 잠금 거부**(현재 IP 제외 변경 차단), 캐시 무효화.
- 필터: 비허용 IP 403, 허용 IP 통과, `/admin/**`만 적용(일반 경로 통과).
- 감사: 변경 시 @Audited 기록(매퍼 mock으로 검증).
- 기존 테스트 회귀 0(사전 존재 `AccessibilityTemplateTest` 3건 CSS 제외).

---

## 6. 빌드 / 검증
- 이 머신 빌드 JVM CDS 손상 → maven 호출 전 `$env:MAVEN_OPTS="-Xshare:off"`.
- `.\mvnw.cmd -q spotless:apply` 후 `.\mvnw.cmd "-Dspotless.check.skip=true" test`.

---

## 7. 완료 기준 (DoD)
- [ ] tb_admin_ip_whitelist 마이그레이션(가산적) + (선택)메뉴 등록.
- [ ] 단일 IP + CIDR(IPv4/IPv6) 매칭 유틸 + 테스트.
- [ ] `/admin/**` IP enforcement 필터(프록시 실제 IP, 루프백 허용, 토글).
- [ ] 안전장치 A(빈 목록=허용) + B(현재 IP 제외 변경 거부) 둘 다.
- [ ] 관리 화면(추가/수정/비활성) + 변경 `@Audited` → `/admin/audit-logs` 노출.
- [ ] 기존 기능/테스트 불변, spotless 적용, 신규 실패 0.

---

## 8. 하지 말 것 / 주의
- 감사 인프라를 새로 만들지 말 것 — 기존 `@Audited`/`tb_audit_log`/`/admin/audit-logs` 재사용.
- 원시 `X-Forwarded-For`를 직접 신뢰 파싱하지 말 것(위조 위험) — framework 해석 IP 사용.
- 루프백 브레이크글라스·빈목록 허용·현재IP 보호 중 **어느 하나라도 빼지 말 것**(잠금 사고 방지).
- IP enforcement를 `/admin/**` 밖(챗봇/검색/일반 사용자)으로 확대 적용하지 말 것.
- CIDR 매칭에 외부 라이브러리 추가 금지(java.net만).
- 마이그레이션 가산적만, 데이터 삭제 금지. 커밋/푸시는 사용자 지시 시에만.
