# Spring Boot 게시판 프로젝트 발표 대본

## 프로젝트 구조 설명

먼저 전체적인 프로젝트 구조를 설명드리겠습니다.

![Project Structure](https://your-image-url.com)

이 프로젝트는 다음과 같은 계층 구조로 이루어져 있습니다:
- Client (브라우저) ←dto→ Controller (Servlet) ←dto→ Service ←dto→ Repository (DAO) ←domain(entity)→ DB

각 계층은 DTO나 Entity 객체를 통해 데이터를 주고받으며, 이를 통해 느슨한 결합도를 유지합니다.

## 핵심 기술 요소 상세 설명

### 1. 계층형 아키텍처(Layered Architecture)

**계층 간 책임 분리**
- **Controller 계층**: 사용자 요청을 수신하고 응답을 반환합니다. 입력 데이터 유효성 검증을 담당합니다.
- **Service 계층**: 비즈니스 로직을 집중적으로 처리합니다. 트랜잭션 관리와 여러 Repository 간의 조합된 작업을 처리합니다.
- **Repository 계층**: 데이터 저장소와의 직접적인 상호작용을 담당합니다. JPA를 통한 데이터 CRUD 작업을 수행합니다.

**데이터 전송 메커니즘**
- Controller와 Service 간: DTO(Data Transfer Object)를 사용
- Service와 Repository 간: Entity 객체를 사용
- 양방향 변환 로직이 Service 계층에 집중되어 있음

### 2. 객체 관계 매핑(ORM) 상세

**Entity 설계**
- **BaseEntity**: 모든 엔티티의 기본 클래스로, 생성시간과 수정시간을 자동으로 관리
  - `@EntityListeners(AuditingEntityListener.class)`를 사용하여 자동 시간 기록
- **Board**: 게시글 정보를 저장
  - `@ManyToOne` 관계를 통해 Member(작성자)와 연결
- **Reply**: 댓글 정보를 저장
  - `@ManyToOne` 관계를 통해 Board(게시글)와 연결
- **Member**: 회원 정보를 저장
  - 고유한 username과 암호화된 비밀번호 관리

**고급 JPA 기능**
- **Fetch 전략**: Board와 Member 간의 지연 로딩(LAZY)을 통한 성능 최적화
- **영속성 컨텍스트**: 변경 감지(Dirty Checking)를 활용한 자동 업데이트
- **동적 쿼리**: QueryDSL을 사용한 복잡한 검색 기능 구현

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

**인증 처리 흐름**
1. 사용자가 로그인 폼에 정보 입력
2. `/loginProc`로 POST 요청 전송
3. Spring Security의 UsernamePasswordAuthenticationFilter가 요청 처리
4. CustomUserDetailsService가 데이터베이스에서 사용자 정보 조회
5. BCryptPasswordEncoder가 암호화된 비밀번호를 비교
6. 인증 성공 시 SecurityContext에 인증 정보가 저장되고 `/board/list`로 리다이렉트
7. 실패 시 `/login?error=true`로 리다이렉트되어 에러 메시지 표시

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

**회원가입 처리 흐름**
1. 사용자가 회원가입 폼에 정보 입력
2. 사용자명 중복 확인 (Ajax 요청)
3. 가입 버튼 클릭 시 `/joinProc`로 POST 요청
4. MemberController가 요청을 수신하여 MemberService로 전달
5. MemberService에서 비밀번호 암호화 (BCrypt 알고리즘)
6. DTO를 Entity로 변환하고 기본 권한(ROLE_USER) 부여
7. MemberRepository를 통해 데이터베이스에 저장
8. 성공 시 로그인 페이지로 리다이렉트

### 3. 로그아웃 기능

**웹 화면 시연**
"상단 메뉴의 로그아웃 버튼을 클릭하면 자동으로 로그아웃 처리됩니다."

**코드 레벨 설명**
```bash
src/main/java/org/zerock/board/config/SecurityConfig.java의 로그아웃 설정을 보시면
```
```java
http.logout((auth) -> auth
    .logoutUrl("/logout")
    .logoutSuccessUrl("/"));
```
"로그아웃 처리와 성공 시 리다이렉션을 설정합니다."

**로그아웃 처리 흐름**
1. 로그아웃 버튼 클릭 시 `/logout`으로 POST 요청
2. Spring Security의 LogoutFilter가 요청 처리
3. SecurityContext에서 인증 정보 제거
4. 세션 무효화 (invalidate)
5. RememberMe 쿠키 삭제 (설정된 경우)
6. 메인 페이지(`/`)로 리다이렉트

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

**목록 조회 처리 흐름**
1. 사용자가 `/board/list` 페이지 요청
2. 컨트롤러에서 페이징 정보(PageRequestDTO)를 파라미터로 받음
3. 서비스 계층에서 페이징 정보를 기반으로 게시글 목록 조회 요청
4. Repository에서 JPQL을 사용하여 게시글, 작성자, 댓글 수를 함께 조회
5. 조회 결과를 DTO로 변환하여 PageResultDTO 객체 생성
6. 화면에 목록 정보와 페이징 컴포넌트 표시

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

**상세 조회 처리 흐름**
1. 사용자가 목록에서 게시글 제목 클릭
2. `/board/read?bno=123` 형태로 URL 요청 발생
3. 컨트롤러에서 게시글 번호(bno)와 페이징 정보(requestDTO)를 파라미터로 받음
4. 서비스 계층에서 게시글 번호로 상세 정보 조회
5. Repository에서 JPQL을 사용하여 게시글, 작성자, 댓글 수를 함께 조회
6. 조회 결과를 DTO로 변환하여 Model에 추가
7. 화면에 게시글 상세 정보와 댓글 목록 표시

### 6. 글쓰기 기능

**웹 화면 시연**
"글쓰기 버튼을 클릭하면 작성 페이지로 이동합니다."

**코드 레벨 설명**

1. **컨트롤러**
```bash
src/main/java/org/zerock/board/controller/BoardController.java를 보시면
```
```java
@PostMapping("/register")
public String register(BoardDTO dto) {
    Long bno = boardService.register(dto);
    return "redirect:/board/list";
}
```

2. **서비스 계층**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java를 보시면
```
```java
@Override
public Long register(BoardDTO dto) {
    log.info("DTO------------------------");
    log.info(dto);
    
    Member member = Member.builder().username(dto.getWriterUsername()).build();
    
    Board board = Board.builder()
            .title(dto.getTitle())
            .content(dto.getContent())
            .writer(member)
            .build();
            
    boardRepository.save(board);
    
    return board.getBno();
}
```

3. **화면 템플릿**
```bash
src/main/resources/templates/board/register.html을 보시면
```
```html
<form th:action="@{/board/register}" method="post">
    <div class="form-group">
        <label >제목</label>
        <input type="text" class="form-control" name="title" placeholder="Enter Title">
    </div>
    <div class="form-group">
        <label >내용</label>
        <textarea class="form-control" rows="5" name="content"></textarea>
    </div>
    <div class="form-group">
        <label >작성자</label>
        <input type="text" class="form-control" name="writerUsername" th:value="${#authentication.principal.username}" readonly>
    </div>
    <button type="submit" class="btn btn-primary">Submit</button>
</form>
```
"로그인한 사용자의 정보를 자동으로 설정합니다."

**게시글 등록 처리 흐름**
1. 사용자가 글쓰기 버튼 클릭 시 `/board/register` 페이지 요청
2. 현재 로그인한 사용자 정보가 작성자 필드에 자동 입력
3. 제목과 내용 입력 후 submit 버튼 클릭
4. `/board/register`로 POST 요청 발생
5. 컨트롤러에서 BoardDTO 객체로 폼 데이터 바인딩
6. 서비스 계층에서 DTO를 Entity로 변환하고 저장
7. 글 등록 완료 후 목록 페이지로 리다이렉트

### 7. 글 수정 기능

**웹 화면 시연**
"수정 버튼을 클릭하면 수정 페이지로 이동합니다."

**코드 레벨 설명**

1. **컨트롤러**
```bash
src/main/java/org/zerock/board/controller/BoardController.java를 보시면
```
```java
@PostMapping("/modify")
public String modify(BoardDTO dto, @ModelAttribute("requestDTO") PageRequestDTO requestDTO) {
    boardService.modify(dto);
    return "redirect:/board/read?bno=" + dto.getBno() + "&page=" + requestDTO.getPage();
}
```

2. **서비스 계층**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java를 보시면
```
```java
@Override
public void modify(BoardDTO boardDTO) {
    // 게시글 조회
    Optional<Board> result = boardRepository.findById(boardDTO.getBno());
    
    if(result.isPresent()) {
        Board board = result.get();
        
        // 제목과 내용만 수정
        board.changeTitle(boardDTO.getTitle());
        board.changeContent(boardDTO.getContent());
        
        boardRepository.save(board);
    }
}
```

**수정 처리 흐름**
1. 사용자가 상세 페이지에서 수정 버튼 클릭
2. `/board/modify?bno=123` 형태로 URL 요청 발생
3. 컨트롤러에서 read 메소드를 통해 게시글 정보 조회하여 수정 페이지 표시
4. 사용자가 내용을 수정하고 submit 버튼 클릭
5. `/board/modify`로 POST 요청 발생
6. 컨트롤러에서 BoardDTO 객체로 폼 데이터 바인딩
7. 서비스 계층에서 게시글 번호로 Entity 조회 후 제목과 내용 수정
8. 영속성 컨텍스트의 변경 감지(Dirty Checking)를 통해 업데이트
9. 수정 완료 후 상세 페이지로 리다이렉트

### 8. 글 삭제 기능

**웹 화면 시연**
"삭제 버튼을 클릭하면 삭제 처리가 진행됩니다."

**코드 레벨 설명**

1. **컨트롤러**
```bash
src/main/java/org/zerock/board/controller/BoardController.java를 보시면
```
```java
@PostMapping("/remove")
public String remove(@RequestParam("bno") Long bno) {
    boardService.removeWithReplies(bno);
    return "redirect:/board/list";
}
```

2. **서비스 계층**
```bash
src/main/java/org/zerock/board/service/BoardServiceImpl.java를 보시면
```
```java
@Transactional
@Override
public void removeWithReplies(Long bno) {
    // 댓글부터 삭제
    replyRepository.deleteByBno(bno);
    
    // 게시글 삭제
    boardRepository.deleteById(bno);
}
```

3. **Repository 계층**
```bash
src/main/java/org/zerock/board/repository/ReplyRepository.java를 보시면
```
```java
@Modifying
@Query("DELETE FROM Reply r WHERE r.board.bno = :bno")
void deleteByBno(@Param("bno") Long bno);
```
"@Modifying 어노테이션으로 UPDATE/DELETE 쿼리 실행을 명시합니다."

**삭제 처리 흐름**
1. 사용자가 상세 페이지에서 삭제 버튼 클릭
2. 확인 대화상자에서 확인 선택
3. `/board/remove`로 POST 요청 발생
4. 컨트롤러에서 게시글 번호(bno)를 파라미터로 받음
5. 서비스 계층에서 트랜잭션 내에서 삭제 처리
6. 먼저 해당 게시글에 달린 댓글부터 삭제
7. 그 다음 게시글 삭제
8. 삭제 완료 후 목록 페이지로 리다이렉트

### 9. 댓글 처리 기능

**댓글 목록 조회**
```bash
src/main/java/org/zerock/board/controller/ReplyController.java를 보시면
```
```java
@GetMapping("/board/{bno}")
public ResponseEntity<List<ReplyDTO>> getListByBoard(@PathVariable("bno") Long bno) {
    return ResponseEntity.ok(replyService.getList(bno));
}
```
"REST API 방식으로 댓글 목록을 JSON 형태로 반환합니다."

**댓글 등록**
```java
@PostMapping("")
public ResponseEntity<Long> register(@RequestBody ReplyDTO replyDTO) {
    Long rno = replyService.register(replyDTO);
    return ResponseEntity.ok(rno);
}
```
"@RequestBody 어노테이션을 사용하여 JSON 데이터를 객체로 변환합니다."

**댓글 삭제**
```java
@DeleteMapping("/{rno}")
public ResponseEntity<String> remove(@PathVariable("rno") Long rno) {
    replyService.remove(rno);
    return ResponseEntity.ok("success");
}
```
"HTTP DELETE 메소드를 사용하여 RESTful API를 구현합니다."

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