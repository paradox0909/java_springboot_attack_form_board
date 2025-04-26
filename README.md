# Spring Boot 게시판 프로젝트 발표 대본

## 프로젝트 구조 설명

먼저 전체적인 프로젝트 구조를 설명드리겠습니다.

이 프로젝트는 다음과 같은 계층 구조로 이루어져 있습니다:
- Client (브라우저) ←dto→ Controller (Servlet) ←dto→ Service ←dto→ Repository (DAO) ←domain(entity)→ DB

각 계층은 DTO나 Entity 객체를 통해 데이터를 주고받으며, 이를 통해 느슨한 결합도를 유지합니다.

## 핵심 기술 요소 상세 설명

### 1. 계층형 아키텍처(Layered Architecture)

**계층 간 책임 분리**
- **Controller 계층** (src/main/java/org/zerock/board/controller/): 사용자 요청을 수신하고 응답을 반환합니다. 입력 데이터 유효성 검증을 담당합니다.
- **Service 계층** (src/main/java/org/zerock/board/service/): 비즈니스 로직을 집중적으로 처리합니다. 트랜잭션 관리와 여러 Repository 간의 조합된 작업을 처리합니다.
- **Repository 계층** (src/main/java/org/zerock/board/repository/): 데이터 저장소와의 직접적인 상호작용을 담당합니다. JPA를 통한 데이터 CRUD 작업을 수행합니다.

**데이터 전송 메커니즘**
- Controller와 Service 간: DTO(Data Transfer Object)를 사용 (src/main/java/org/zerock/board/dto/)
- Service와 Repository 간: Entity 객체를 사용 (src/main/java/org/zerock/board/entity/)
- 양방향 변환 로직이 Service 계층에 집중되어 있음

### 2. 객체 관계 매핑(ORM) 상세

**Entity 설계**
- **BaseEntity** (src/main/java/org/zerock/board/entity/BaseEntity.java): 모든 엔티티의 기본 클래스로, 생성시간과 수정시간을 자동으로 관리
  - `@EntityListeners(AuditingEntityListener.class)`를 사용하여 자동 시간 기록
- **Board** (src/main/java/org/zerock/board/entity/Board.java): 게시글 정보를 저장
  - `@ManyToOne` 관계를 통해 Member(작성자)와 연결
- **Reply** (src/main/java/org/zerock/board/entity/Reply.java): 댓글 정보를 저장
  - `@ManyToOne` 관계를 통해 Board(게시글)와 연결
- **Member** (src/main/java/org/zerock/board/entity/Member.java): 회원 정보를 저장
  - 고유한 username과 암호화된 비밀번호 관리

**고급 JPA 기능**
- **Fetch 전략**: Board와 Member 간의 지연 로딩(LAZY)을 통한 성능 최적화
- **영속성 컨텍스트**: 변경 감지(Dirty Checking)를 활용한 자동 업데이트
- **동적 쿼리** (src/main/java/org/zerock/board/repository/search/): QueryDSL을 사용한 복잡한 검색 기능 구현

### 3. 보안 체계 상세 구현

**인증 처리 흐름 상세**
1. **사용자 인증 정보 수집**:
   - 로그인 폼에서 username과 password를 수집
   - UsernamePasswordAuthenticationToken 생성

2. **인증 처리**:
   - AuthenticationManager가 인증 처리
   - CustomUserDetailsService가 DB에서 사용자 정보 조회
   - BCryptPasswordEncoder가 암호화된 비밀번호 비교

3. **인증 결과 처리**:
   - 성공 시 SecurityContext에 인증 정보 저장
   - 실패 시 AuthenticationException 발생 및 에러 페이지로 리다이렉트

**권한 관리 상세**
- **계층형 권한 구조**: ADMIN > MANAGER > USER
- **메소드 수준 보안**: `@PreAuthorize` 어노테이션을 사용한 세밀한 권한 제어
- **동적 권한 검사**: Authentication 객체를 활용한 소유자 확인 로직

## Spring Security 구현 설명

Spring Security는 이 프로젝트의 인증(Authentication)과 인가(Authorization)를 담당합니다.

### 1. Security 설정 (src/main/java/org/zerock/board/config/SecurityConfig.java)

**SecurityConfig.java 파일 구조**
```bash
src/main/java/org/zerock/board/config/SecurityConfig.java를 보시면
```

1. **비밀번호 암호화 설정** (15-19라인)
```java
@Bean
public BCryptPasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
}
```
"모든 비밀번호는 BCrypt 알고리즘으로 암호화되어 저장됩니다."

2. **권한 계층 설정** (21-30라인)
```java
@Bean
public RoleHierarchy roleHierarchy() {
    RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
    hierarchy.setHierarchy("ROLE_ADMIN > ROLE_MANAGER\n" +
            "ROLE_MANAGER > ROLE_USER");
    return hierarchy;
}
```
"ADMIN > MANAGER > USER 순으로 권한이 계층화되어 있어, 상위 권한은 하위 권한의 모든 기능을 사용할 수 있습니다."

3. **URL 접근 권한 설정** (32-45라인)
```java
http.authorizeHttpRequests((auth) -> auth
    .requestMatchers("/css/**", "/vendor/**", "/favicon.ico/**").permitAll()
    .requestMatchers("/", "/login", "/loginProc", "/join", "/joinProc", "/checkUsername").permitAll()
    .requestMatchers("/admin").hasRole("ADMIN")
    .requestMatchers("/manager").hasRole("MANAGER")
    .requestMatchers("/board/**").hasAnyRole("USER")
    .requestMatchers("/replies/**").hasAnyRole("USER")
    .anyRequest().authenticated()
);
```
"각 URL 패턴별로 필요한 권한을 설정합니다."

4. **로그인/로그아웃 설정** (47-60라인)
```java
http.formLogin((auth) -> auth
    .loginPage("/login")
    .loginProcessingUrl("/loginProc")
    .defaultSuccessUrl("/board/list", true));

http.logout((auth) -> auth
    .logoutUrl("/logout")
    .logoutSuccessUrl("/"));
```
"로그인과 로그아웃의 처리 경로와 성공 시 리다이렉션을 설정합니다."

5. **세션 관리 설정** (62-72라인)
```java
http.sessionManagement((auth) -> auth
    .maximumSessions(1)
    .maxSessionsPreventsLogin(true));

http.sessionManagement((auth) -> auth
    .sessionFixation().changeSessionId());
```
"동시 로그인 제한과 세션 고정 공격 방지를 설정합니다."

### 2. 보안 처리 흐름

1. **인증 처리 흐름**
```
1. 사용자가 로그인 폼에서 정보 입력
2. /loginProc로 POST 요청
3. UsernamePasswordAuthenticationFilter가 요청 처리
4. UserDetailsService가 사용자 정보 조회
5. BCryptPasswordEncoder로 비밀번호 검증
6. 성공 시 SecurityContext에 인증 정보 저장
```

2. **인가 처리 흐름**
```
1. 사용자가 보호된 리소스 요청
2. FilterSecurityInterceptor가 요청 확인
3. 현재 사용자의 권한 확인
4. 접근 권한이 있으면 리소스 제공, 없으면 403 에러
```

### 3. CSRF 보안

현재는 개발 편의를 위해 CSRF 보호가 비활성화되어 있습니다:
```java
http.csrf(AbstractHttpConfigurer::disable);
```

프로덕션 환경에서는 CSRF 토큰을 활성화하여 CSRF 공격을 방지해야 합니다.

## 주요 기능 설명

### 1. 로그인 기능 (src/main/java/org/zerock/board/controller/MemberController.java, src/main/java/org/zerock/board/service/CustomUserDetailsService.java)

**로그인 처리 흐름**
1. `/login` 페이지 (src/main/resources/templates/member/login.html)에서 사용자 로그인 폼을 제공합니다.
2. 사용자가 ID와 비밀번호를 입력하고 제출하면 `/loginProc` 경로로 POST 요청이 전송됩니다.
3. `SecurityConfig`에 설정된 `formLogin` 설정에 따라 Spring Security가 로그인 처리를 담당합니다.
4. `UserDetailsService` 구현체인 `CustomUserDetailsService`가 사용자 정보를 데이터베이스에서 조회합니다.
5. `BCryptPasswordEncoder`를 사용하여 암호화된 비밀번호를 비교합니다.
6. 인증이 성공하면 `SecurityContext`에 인증 정보가 저장되고 `/board/list`로 리다이렉트됩니다.
7. 인증이 실패하면 `/login?error=true`로 리다이렉트됩니다.

**관련 클래스**
- `SecurityConfig` (src/main/java/org/zerock/board/config/SecurityConfig.java): Spring Security 설정
- `CustomUserDetailsService` (src/main/java/org/zerock/board/service/CustomUserDetailsService.java): 사용자 정보 조회 및 인증
- `Member` 엔티티 (src/main/java/org/zerock/board/entity/Member.java): 데이터베이스의 회원 정보
- `MemberRepository` (src/main/java/org/zerock/board/repository/MemberRepository.java): 데이터베이스와의 상호작용 (JPA 사용)

### 2. 회원가입 기능 (src/main/java/org/zerock/board/controller/MemberController.java)

**회원가입 처리 흐름**
1. `/join` 페이지 (src/main/resources/templates/member/join.html)에서 사용자가 회원가입 폼을 작성합니다.
2. 제출 시 `/joinProc` 경로로 POST 요청이 전송됩니다.
3. `MemberController`의 `joinProc` 메서드가 요청을 처리합니다.
4. `MemberDTO` 객체에 클라이언트가 제출한 데이터가 바인딩됩니다.
5. `memberService.join(memberDTO)`를 호출하여 회원가입 로직을 수행합니다.
6. 서비스 계층에서 비밀번호를 암호화하고 DTO를 Entity로 변환합니다.
7. `result`가 0이면 회원가입 실패, 0이 아니면 성공으로 간주합니다.
8. 성공 시 `/login` 페이지로 리다이렉트하여 로그인할 수 있도록 합니다.

**관련 클래스**
- `MemberDTO` (src/main/java/org/zerock/board/dto/MemberDTO.java): 클라이언트에서 제출한 데이터를 담는 객체
- `MemberService` 인터페이스 및 구현체 (src/main/java/org/zerock/board/service/MemberService.java, src/main/java/org/zerock/board/service/MemberServiceImpl.java): 비즈니스 로직 처리
- `Member` 엔티티 클래스 (src/main/java/org/zerock/board/entity/Member.java): 데이터베이스의 Member 테이블과 매핑
- `MemberRepository` (src/main/java/org/zerock/board/repository/MemberRepository.java): 데이터베이스와의 상호작용 (JPA 사용)

### 3. 로그아웃 기능 (src/main/java/org/zerock/board/config/SecurityConfig.java)

**로그아웃 처리 흐름**
1. 사용자가 로그아웃 버튼을 클릭하면 `/logout` 경로로 POST 요청이 전송됩니다.
2. `SecurityConfig`에 설정된 `logout` 설정에 따라 Spring Security가 로그아웃을 처리합니다.
3. `SecurityContextHolder.getContext().getAuthentication()`을 통해 현재 사용자의 인증 정보를 확인합니다.
4. 인증 정보가 `null`이 아니면 사용자는 로그인 상태이므로 로그아웃 처리를 진행합니다.
5. `SecurityContext`에서 인증 정보를 제거하고 세션을 무효화합니다.
6. 로그아웃 성공 후 메인 페이지(`/`)로 리다이렉트됩니다.

**관련 클래스**
- `SecurityConfig` (src/main/java/org/zerock/board/config/SecurityConfig.java): 로그아웃 설정
- `SecurityContextHolder`: 인증 정보 관리

### 4. 게시글 조회 기능 (src/main/java/org/zerock/board/controller/BoardController.java)

**게시글 목록 조회 흐름**
1. 사용자가 `/board/list` 페이지 (src/main/resources/templates/board/list.html)에 접근합니다.
2. `BoardController`의 `list` 메서드가 요청을 처리합니다.
3. `PageRequestDTO`를 파라미터로 받아 페이징 정보를 처리합니다.
4. `boardService.getList(pageRequestDTO)`를 호출하여 게시글 목록을 조회합니다.
5. 서비스 계층에서 `BoardRepository`를 통해 데이터베이스에서 게시글을 조회합니다.
6. JPQL을 사용하여 게시글, 작성자, 댓글 수를 함께 조회합니다.
7. 조회 결과를 `PageResultDTO`에 담아 화면에 전달합니다.

**관련 클래스**
- `BoardController` (src/main/java/org/zerock/board/controller/BoardController.java): 요청 처리
- `BoardService` (src/main/java/org/zerock/board/service/BoardService.java, src/main/java/org/zerock/board/service/BoardServiceImpl.java): 비즈니스 로직
- `BoardRepository` (src/main/java/org/zerock/board/repository/BoardRepository.java): 데이터베이스 조회
- `PageRequestDTO` (src/main/java/org/zerock/board/dto/PageRequestDTO.java): 페이징 요청 정보
- `PageResultDTO` (src/main/java/org/zerock/board/dto/PageResultDTO.java): 페이징 결과 정보

### 5. 상세 게시글 조회 기능 (src/main/java/org/zerock/board/controller/BoardController.java)

**상세 게시글 조회 흐름**
1. 사용자가 게시글 제목을 클릭하면 `/board/read?bno=숫자` 형태로 요청이 전송됩니다.
2. `BoardController`의 `read` 메서드가 요청을 처리합니다.
3. 게시글 번호(`bno`)를 파라미터로 받아 `boardService.get(bno)`를 호출합니다.
4. 서비스 계층에서 `BoardRepository`를 통해 게시글 정보를 조회합니다.
5. JPQL을 사용하여 게시글, 작성자, 댓글 수를 함께 조회합니다.
6. 조회 결과를 `BoardDTO`에 담아 화면 (src/main/resources/templates/board/read.html)에 전달합니다.

**관련 클래스**
- `BoardController` (src/main/java/org/zerock/board/controller/BoardController.java): `read` 메서드
- `BoardService` (src/main/java/org/zerock/board/service/BoardServiceImpl.java): `get` 메서드
- `BoardRepository` (src/main/java/org/zerock/board/repository/BoardRepository.java): `getBoardByBno` 메서드
- `BoardDTO` (src/main/java/org/zerock/board/dto/BoardDTO.java): 게시글 정보 전달 객체

### 6. 글쓰기 기능 (src/main/java/org/zerock/board/controller/BoardController.java)

**게시글 작성 흐름**
1. 사용자가 글쓰기 버튼을 클릭하면 `/board/register` 페이지 (src/main/resources/templates/board/register.html)로 이동합니다.
2. 사용자가 제목, 내용을 입력하고 제출하면 `/board/register`로 POST 요청이 전송됩니다.
3. `BoardController`의 `register` 메서드가 요청을 처리합니다.
4. `BoardDTO`에 클라이언트가 제출한 데이터가 바인딩됩니다.
5. `boardService.register(dto)`를 호출하여 게시글 등록 로직을 수행합니다.
6. 서비스 계층에서 DTO를 Entity로 변환하고 `BoardRepository`를 통해 데이터베이스에 저장합니다.
7. 등록된 게시글의 번호(`bno`)를 반환하고 목록 페이지로 리다이렉트합니다.

**관련 클래스**
- `BoardController` (src/main/java/org/zerock/board/controller/BoardController.java): `register` 메서드
- `BoardService` (src/main/java/org/zerock/board/service/BoardServiceImpl.java): `register` 메서드
- `BoardRepository` (src/main/java/org/zerock/board/repository/BoardRepository.java): `save` 메서드
- `BoardDTO` (src/main/java/org/zerock/board/dto/BoardDTO.java): 게시글 정보 전달 객체
- `Board` 엔티티 (src/main/java/org/zerock/board/entity/Board.java): 데이터베이스의 Board 테이블과 매핑

### 7. 글 수정 기능 (src/main/java/org/zerock/board/controller/BoardController.java)

**게시글 수정 흐름**
1. 사용자가 수정 버튼을 클릭하면 `/board/modify?bno=숫자` 형태로 요청이 전송됩니다.
2. `BoardController`의 `read` 메서드가 `modify` 경로도 함께 처리하여 수정 페이지 (src/main/resources/templates/board/modify.html)를 표시합니다.
3. 사용자가 내용을 수정하고 제출하면 `/board/modify`로 POST 요청이 전송됩니다.
4. `BoardController`의 `modify` 메서드가 요청을 처리합니다.
5. `BoardDTO`에 수정된 데이터가 바인딩되고 `boardService.modify(dto)`를 호출합니다.
6. 서비스 계층에서 `BoardRepository`를 통해 엔티티를 조회하고 제목과 내용을 수정합니다.
7. 영속성 컨텍스트의 변경 감지(Dirty Checking)를 통해 데이터베이스가 자동으로 업데이트됩니다.
8. 수정 완료 후 상세 페이지로 리다이렉트합니다.

**관련 클래스**
- `BoardController` (src/main/java/org/zerock/board/controller/BoardController.java): `modify` 메서드
- `BoardService` (src/main/java/org/zerock/board/service/BoardServiceImpl.java): `modify` 메서드
- `BoardRepository` (src/main/java/org/zerock/board/repository/BoardRepository.java): `findById` 메서드
- `Board` 엔티티 (src/main/java/org/zerock/board/entity/Board.java): `changeTitle`, `changeContent` 메서드

### 8. 글 삭제 기능 (src/main/java/org/zerock/board/controller/BoardController.java)

**게시글 삭제 흐름**
1. 사용자가 삭제 버튼을 클릭하면 확인 대화상자가 표시되고, 확인 시 `/board/remove` 경로로 POST 요청이 전송됩니다.
2. `BoardController`의 `remove` 메서드가 요청을 처리합니다.
3. 게시글 번호(`bno`)를 파라미터로 받아 `boardService.removeWithReplies(bno)`를 호출합니다.
4. 서비스 계층에서 트랜잭션 내에서 댓글부터 삭제하고 게시글을 삭제합니다.
5. `@Transactional` 어노테이션을 통해 모든 작업이 성공적으로 완료되거나 모두 롤백되도록 보장합니다.
6. 삭제 완료 후 목록 페이지로 리다이렉트합니다.

**관련 클래스**
- `BoardController` (src/main/java/org/zerock/board/controller/BoardController.java): `remove` 메서드
- `BoardService` (src/main/java/org/zerock/board/service/BoardServiceImpl.java): `removeWithReplies` 메서드
- `ReplyRepository` (src/main/java/org/zerock/board/repository/ReplyRepository.java): `deleteByBno` 메서드
- `BoardRepository` (src/main/java/org/zerock/board/repository/BoardRepository.java): `deleteById` 메서드

## 주요 기술 스택

- Spring Boot 3.2.3
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL
- Gradle
- Docker

## 실행 방법

1. Gradle로 직접 실행:
```bash
./gradlew bootRun
```

2. Docker로 실행:
```bash
docker build -t board-image .
docker-compose up
```

이상으로 발표를 마치겠습니다. 질문이 있으시다면 답변드리겠습니다. 