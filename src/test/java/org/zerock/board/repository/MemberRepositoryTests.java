package org.zerock.board.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zerock.board.entity.Member;
import org.zerock.board.repository.MemberRepository;

import java.util.stream.IntStream;

@SpringBootTest
@ActiveProfiles("test")
public class MemberRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    public void insertMembers() {
        IntStream.rangeClosed(1, 100).forEach(i -> {
            Member member = Member.builder()
                    .username("user" + i + "@test.com")  // email -> username
                    .password("1234")
                    .name("USER" + i)
                    .build();
            memberRepository.save(member);
        });
    }
}