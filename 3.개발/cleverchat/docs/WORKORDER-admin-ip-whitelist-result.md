# WORKORDER-admin-ip-whitelist Result

## 변경/신규 파일
- `src/main/resources/db/migration/V32__admin_ip_whitelist.sql`
- `src/main/resources/db/migration/V31__crawl_browser.sql`
- `src/main/resources/application.yml`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/model/AdminIpWhitelist.java`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/mapper/AdminIpWhitelistMapper.java`
- `src/main/resources/mapper/adminmanage/AdminIpWhitelistMapper.xml`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/service/IpCidrMatcher.java`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/service/AdminIpWhitelistService.java`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/security/AdminIpWhitelistFilter.java`
- `src/main/java/kr/co/cleverchat/domain/adminmanage/controller/AdmAdminIpWhitelistController.java`
- `src/main/resources/templates/admmgr/manage/ipWhitelist.html`
- `src/test/java/kr/co/cleverchat/domain/adminmanage/service/IpCidrMatcherTest.java`
- `src/test/java/kr/co/cleverchat/domain/adminmanage/service/AdminIpWhitelistServiceTest.java`
- `src/test/java/kr/co/cleverchat/domain/adminmanage/security/AdminIpWhitelistFilterTest.java`

## 주요 설계 결정
- 관리자 IP 허용목록은 `domain/adminmanage` 안에 격리하고 `/admin/**`에만 적용했다.
- 클라이언트 IP는 작업지시서대로 `request.getRemoteAddr()`만 사용하며, `X-Forwarded-For`를 직접 파싱하지 않는다.
- `cleverchat.admin.ip-whitelist.enabled` 기능 플래그를 추가했다. 기본값은 작업지시서대로 `true`이고, 활성 허용목록이 비어 있으면 전체 허용한다.
- 루프백 주소는 항상 허용한다.
- CIDR 매칭은 외부 라이브러리 없이 `java.net.InetAddress` 기반으로 IPv4/IPv6를 처리한다.
- 변경 작업은 기존 `@RequireRole("ADMIN")`와 `@Audited` 인프라를 재사용한다.
- 추가/수정/비활성화 후 현재 접속 IP가 결과 허용목록에서 제외되면 변경을 거부해 자기 잠금을 방지한다.
- `tb_admin_ip_whitelist` 마이그레이션은 가산적으로만 작성했고, 기존 데이터 삭제/수정은 하지 않았다.

## 작업지시서 대비 이탈
- 기능 설계 이탈 없음.
- 이 환경에서 `$env:MAVEN_OPTS="-Xshare:off"`만으로도 JVM CDS/GC 크래시가 반복되어 검증 명령에는 `-XX:+UseSerialGC`를 함께 추가했다. 소스/설정 변경은 아니며 로컬 검증용 실행 옵션이다.
- 서버 기동 중 선행 크롤러 마이그레이션 `V31__crawl_browser.sql`이 V21 이후 존재하지 않는 `users(id)`를 FK로 참조해 Flyway가 실패했다. 실제 스키마인 `tb_user(user_no)` 참조로 보정했다. 기존 데이터 삭제/수정은 없고, 서버 기동을 막는 스키마 참조 오류 수정이다.

## 검증 결과
- 포맷: `$env:MAVEN_OPTS="-Xshare:off -XX:+UseSerialGC"; .\mvnw.cmd -q spotless:apply`
  - 성공
- 신규/관련 테스트: `$env:MAVEN_OPTS="-Xshare:off -XX:+UseSerialGC"; .\mvnw.cmd "-Dspotless.check.skip=true" "-Dtest=IpCidrMatcherTest,AdminIpWhitelistServiceTest,AdminIpWhitelistFilterTest" test`
  - Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
- 전체 테스트: `$env:MAVEN_OPTS="-Xshare:off -XX:+UseSerialGC"; .\mvnw.cmd "-Dspotless.check.skip=true" test`
  - Tests run: 361, Failures: 3, Errors: 0, Skipped: 0
  - 실패 3건은 사전 존재 실패로 공유된 `AccessibilityTemplateTest` CSS 관련 실패다.
  - 신규 admin IP whitelist 관련 실패는 없다.
- 패키징: `$env:MAVEN_OPTS="-Xshare:off -XX:+UseSerialGC"; .\mvnw.cmd "-Dspotless.check.skip=true" "-DskipTests" package`
  - 성공

## 서버 기동
- Temurin JDK 17.0.17은 `java -jar` 기동 중 네이티브 VM 크래시가 발생해 사용하지 않았다.
- Oracle JDK 17.0.12로 `java -Xshare:off -XX:+UseSerialGC -jar target\cleverchat.jar` 기동 성공.
- PID: `10972`
- URL: `http://localhost:8080/login`
- HTTP 확인: `/login` 200 OK.

## 미완 항목
- 없음.
