# 권한/메뉴/로그인 작업지시서

> 작성일: 2026-05-12
> 기준: `8.소스/OverseasNPP_20260511`
> 대상: `3.개발/cleverchat`

## 1. 핵심 판단

참고 소스는 인증/인가에 Spring Security를 사용하지 않는다. cleverchat의 권한, 메뉴, 로그인도 Spring Security 방식으로 확장하지 말고 참고 소스의 세션 VO + MVC Interceptor + MyBatis XML mapper 방식을 따른다.

단, cleverchat은 Spring Boot 3.3/Java 17 프로젝트이므로 참고 소스의 Java 8 문법 제한과 `javax.servlet` 패키지는 그대로 적용하지 않는다. 구현 시에는 `jakarta.servlet`과 현재 패키지 구조를 사용한다.

## 2. 참고 소스 구현 기준

| 영역 | 참고 파일 | 적용 기준 |
|---|---|---|
| 로그인 화면/처리 | `kr/admmgr/member/AdmLoginController.java` | GET 화면, POST 로그인 처리, 실패 시 redirect/flash, OTP 선택 적용 |
| 로그인 세션 | `com/c2r/core/vo/AdminVO.java` | 세션 키 상수, 사용자 기본정보, 권한번호, 메뉴 권한 캐시 보관 |
| 요청 인증/인가 | `com/c2r/util/interceptor/AuthInterceptor.java` | 세션 유무, 권한 변경 감지, 비활성 메뉴 URL 차단, URL 권한 확인 |
| 메뉴 캐시 | `com/c2r/core/info/MenuInfo.java` | 메뉴 트리, 비활성 메뉴 URL, 권한 플래그 캐시 |
| CSRF/XSS | `com/c2r/WebSecurity.java`, `CsrfInterceptor.java` | 폼 토큰 생성/검증, 재사용 방지, 파일명/리다이렉트 방어 |
| 메뉴/권한 SQL | `tb_menu.xml`, `tb_auth.xml`, `tb_auth_menu_adm.xml` | MyBatis XML mapper 기준 |

## 3. 구현 순서

1. Spring Security 의존 코드 사용 지점을 전수 확인한다.
   - `pom.xml`
   - `SecurityConfig.java`
   - `domain/auth/security/**`
   - `DatabaseUserDetailsService`
   - `SecurityContextHolder` 사용부
   - 템플릿 `sec:authorize`

2. cleverchat용 세션 VO를 만든다.
   - 예: `domain/auth/session/AdminSession`
   - 사용자 ID, 이름, 역할/권한번호, 메뉴 권한 Map, 권한 로딩 시각, 클라이언트 IP를 보관한다.
   - 세션 키는 상수로 관리한다.

3. 로그인 컨트롤러/서비스를 참고 소스 방식으로 재구성한다.
   - GET `/login`은 폼과 CSRF 토큰을 내려준다.
   - POST `/login`은 사용자 조회, 비밀번호 검증, 활성/잠금 상태 확인, 로그인 로그 기록, 세션 저장 후 관리자 홈으로 redirect한다.
   - 실패 메시지는 URL 파라미터 남발보다 flash/session 1회성 메시지를 우선한다.

4. MVC Interceptor를 추가한다.
   - 공개 경로: `/`, 정적 리소스, `/login`, `/logout`, 챗봇 공개 API, health check
   - 관리자 경로: 세션 필수
   - 관리자 메뉴 URL: 세션 권한 캐시 기준으로 접근 허용 여부 확인
   - Ajax/API 요청은 401/403 JSON, 화면 요청은 로그인 또는 권한 오류 화면으로 이동한다.

5. 메뉴/권한 모델을 정리한다.
   - 메뉴 테이블은 트리 구조와 URL, 사용 여부를 가진다.
   - 권한-메뉴 매핑은 조회/등록/수정/삭제/처리 플래그를 가진다.
   - 로그인 직후 세션에 메뉴 번호 Set과 URL별 권한 플래그 Map을 적재한다.

6. 화면 조건부 노출을 변경한다.
   - `sec:authorize`는 제거한다.
   - 메뉴/권한 헬퍼 또는 모델 속성으로 버튼/메뉴 노출 여부를 판단한다.

7. Spring Security 제거를 마무리한다.
   - `spring-boot-starter-security`, `thymeleaf-extras-springsecurity6`, `spring-security-test` 제거
   - `SecurityConfig.java` 제거
   - Spring Security 타입 의존 클래스 제거 또는 일반 서비스로 전환
   - 비밀번호 해시는 필요 시 `spring-security-crypto`만 별도 의존성으로 유지한다.

## 4. 완료 기준

- 인증/권한 진입점이 `SecurityFilterChain`이 아니라 `HandlerInterceptor`이다.
- 로그인 성공 후 세션 VO가 생성되고, 관리자 요청은 세션 VO 기준으로 통과한다.
- 메뉴 노출과 URL 직접 호출 차단이 같은 권한 데이터로 동작한다.
- 비활성 메뉴 URL은 직접 호출해도 차단된다.
- `sec:authorize`, `SecurityContextHolder`, `UserDetailsService`가 남아 있지 않다.
- JPA 의존성, `@Entity`, `JpaRepository`가 없다.

## 5. 검증 항목

```bash
rg -n "spring-boot-starter-security|thymeleaf-extras-springsecurity|spring-security-test|SecurityFilterChain|HttpSecurity|UserDetailsService|SecurityContextHolder|sec:authorize" pom.xml src
rg -n "spring-boot-starter-data-jpa|JpaRepository|@Entity" pom.xml src
rg -n "HandlerInterceptor|addInterceptor|AdminSession|csrfToken|loginProc|menuPerms" src/main/java src/main/resources/templates
```

수동 테스트:

- 미로그인 상태로 관리자 URL 접근 시 로그인 화면으로 이동한다.
- 로그인 실패 시 세션이 생성되지 않고 실패 로그가 남는다.
- 로그인 성공 시 세션 VO와 권한 캐시가 생성된다.
- 권한 없는 메뉴 URL 직접 호출은 403 또는 권한 오류 화면으로 처리된다.
- 비활성 메뉴 URL 직접 호출은 권한이 있어도 차단된다.
