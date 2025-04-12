# Spring Boot 게시판 프로젝트 발표 대본

## 프로젝트 구조 설명

먼저 전체적인 프로젝트 구조를 설명드리겠습니다.

![Project Structure](https://your-image-url.com)

이 프로젝트는 다음과 같은 계층 구조로 이루어져 있습니다:
- Client (브라우저) ←dto→ Controller (Servlet) ←dto→ Service ←dto→ Repository (DAO) ←domain(entity)→ DB

각 계층은 DTO나 Entity 객체를 통해 데이터를 주고받으며, 이를 통해 느슨한 결합도를 유지합니다.

## Spring Security 구현 설명

Spring Security는 이 프로젝트의 인증(Authentication)과 인가(Authorization)를 담당합니다.

### 1. Security 설정

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

### 1. 로그인 기능

**웹 화면 시연**
"먼저 로그인 기능을 보여드리겠습니다. 메인 페이지에서 로그인 버튼을 클릭하면 `/login` 페이지로 이동합니다."

**코드 레벨 설명**

1. **로그인 컨트롤러**
```bash
src/main/java/org/zerock/board/controller/MemberController.java를 보시면
```
```java
@GetMapping("/login")
public void loginGET(String error, String logout) {
    log.info("login get...");
    log.info("logout: " + logout);
}
```
"로그인 페이지를 보여주는 컨트롤러입니다."

2. **로그인 처리 필터**
```bash
src/main/java/org/zerock/board/security/CustomUserDetailsService.java를 보시면
```
```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<Member> result = memberRepository.findByUsername(username);
    if(result.isEmpty()) {
        throw new UsernameNotFoundException("Check User Email");
    }
    Member member = result.get();
    return new CustomUserDetails(member);
}
```
"사용자 정보를 데이터베이스에서 조회하여 인증에 사용합니다."

3. **로그인 화면**
```bash
src/main/resources/templates/login.html을 보시면
```
```html
<form th:action="@{/loginProc}" method="post">
    <input type="text" name="username" placeholder="Username">
    <input type="password" name="password" placeholder="Password">
    <button type="submit">로그인</button>
</form>
```
"Thymeleaf를 사용한 로그인 폼입니다."

### 2. 회원가입 기능

**웹 화면 시연**
"회원가입 버튼을 클릭하면 `/join` 페이지로 이동합니다."

**코드 레벨 설명**

1. **회원가입 컨트롤러**
```bash
src/main/java/org/zerock/board/controller/MemberController.java를 보시면
```
```java
@PostMapping("/joinProc")
public String joinProc(MemberDTO memberDTO) {
    memberService.join(memberDTO);
    return "redirect:/login";
}
```

2. **회원가입 서비스**
```bash
src/main/java/org/zerock/board/service/MemberServiceImpl.java를 보시면
```
```java
@Override
public void join(MemberDTO memberDTO) {
    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(memberDTO.getPassword());
    
    // DTO → Entity 변환
    Member member = Member.builder()
            .username(memberDTO.getUsername())
            .password(encodedPassword)
            .name(memberDTO.getName())
            .role("ROLE_USER")  // 기본 권한 설정
            .build();
            
    memberRepository.save(member);
}
```
"비밀번호 암호화와 함께 회원 정보를 저장합니다."

3. **중복 확인 API**
```bash
src/main/java/org/zerock/board/controller/MemberController.java의 다른 메소드를 보시면
```
```java
@GetMapping("/checkUsername")
@ResponseBody
public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
    return ResponseEntity.ok(memberService.checkUsernameDuplicate(username));
}
```
"비동기로 사용자명 중복을 확인합니다."

### 4. 게시글 목록 조회

**웹 화면 시연**
"/board/list 페이지에서 게시글 목록을 확인할 수 있습니다."

**코드 레벨 설명**

1. **컨트롤러**
```bash
src/main/java/org/zerock/board/controller/BoardController.java를 보시면
```
```java
@GetMapping("/list")
public void list(PageRequestDTO pageRequestDTO, Model model) {
    log.info("list............." + pageRequestDTO);
    model.addAttribute("result", boardService.getList(pageRequestDTO));
}
```

2. **서비스 계층**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java를 보시면
```
```java
@Override
public PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO) {
    Function<Object[], BoardDTO> fn = (en -> entityToDTO((Board)en[0], (Member)en[1], (Long)en[2]));
    
    Page<Object[]> result = boardRepository.getBoardWithReplyCount(
            pageRequestDTO.getPageable(Sort.by("bno").descending()));
            
    return new PageResultDTO<>(result, fn);
}
```
"페이징과 정렬이 적용된 게시글 목록을 조회합니다."

3. **Repository 계층**
```bash
src/main/java/org/zerock/board/repository/BoardRepository.java를 보시면
```
```java
@Query(value = "SELECT b, w, count(r) " +
        "FROM Board b " +
        "LEFT JOIN b.writer w " +
        "LEFT JOIN Reply r ON r.board = b " +
        "GROUP BY b",
        countQuery = "SELECT count(b) FROM Board b")
Page<Object[]> getBoardWithReplyCount(Pageable pageable);
```
"JPQL을 사용하여 게시글, 작성자, 댓글 수를 함께 조회합니다."

### 5. 상세 게시글 조회

**웹 화면 시연**
"게시글 제목을 클릭하면 상세 페이지로 이동합니다."

**코드 레벨 설명**

1. **컨트롤러**
```bash
src/main/java/org/zerock/board/controller/BoardController.java를 보시면
```
```java
@GetMapping({"/read", "/modify"})
public void read(@RequestParam("bno") Long bno, 
                 @ModelAttribute("requestDTO") PageRequestDTO requestDTO,
                 Model model) {
    log.info("bno: " + bno);
    BoardDTO boardDTO = boardService.get(bno);
    model.addAttribute("dto", boardDTO);
}
```

2. **서비스 계층**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java를 보시면
```
```java
@Override
public BoardDTO get(Long bno) {
    Object result = boardRepository.getBoardByBno(bno);
    Object[] arr = (Object[])result;
    return entityToDTO((Board)arr[0], (Member)arr[1], (Long)arr[2]);
}
```

3. **Repository 계층**
```bash
src/main/java/org/zerock/board/repository/BoardRepository.java를 보시면
```
```java
@Query("SELECT b, w, count(r) " +
        "FROM Board b LEFT JOIN b.writer w " +
        "LEFT OUTER JOIN Reply r ON r.board = b " +
        "WHERE b.bno = :bno")
Object getBoardByBno(@Param("bno") Long bno);
```
"게시글 정보와 함께 작성자 정보, 댓글 수를 조회합니다."

### 6. 글쓰기 기능

**웹 화면 시연**
"글쓰기 버튼을 클릭하면 작성 페이지로 이동합니다."

**코드 설명**
```bash
src/main/java/org/zerock/board/controller/BoardController.java의 register 메소드를 보시면
```
```java
@PostMapping("/register")
public String register(BoardDTO dto) {
    Long bno = boardService.register(dto);
    return "redirect:/board/list";
}
```
"DTO로 받은 데이터를 Service 계층으로 전달하여 처리합니다."

### 7. 글 수정 기능

**웹 화면 시연**
"수정 버튼을 클릭하면 수정 페이지로 이동합니다."

**코드 설명**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java의 modify 메소드를 보시면
```
"엔티티의 변경 감지(Dirty Checking)를 통해 수정 사항이 자동으로 반영됩니다."

### 8. 글 삭제 기능

**웹 화면 시연**
"삭제 버튼을 클릭하면 삭제 처리가 진행됩니다."

**코드 설명**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java의 removeWithReplies 메소드를 보시면
```
```java
@Transactional
public void removeWithReplies(Long bno) {
    replyRepository.deleteByBno(bno);
    boardRepository.deleteById(bno);
}
```
"트랜잭션 처리를 통해 댓글과 게시글이 안전하게 삭제됩니다."

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