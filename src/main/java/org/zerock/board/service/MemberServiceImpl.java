package org.zerock.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.zerock.board.dto.MemberDTO;
import org.zerock.board.entity.Member;
import org.zerock.board.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    public long join(MemberDTO memberDTO) {

        if (memberRepository.existsByUsername(memberDTO.getUsername())) {
            return 0;
        }

        Member member = Member.builder()
                .username(memberDTO.getUsername())
                .password(bCryptPasswordEncoder.encode(memberDTO.getPassword()))
                .name(memberDTO.getName())
                .role("ROLE_USER")
                .build();
        memberRepository.save(member);
        return member.getId();
    }

    @Override
    public Member getMember(String username) {
        return memberRepository.findByUsername(username);
    }

    @Override
    public boolean isUsernameTaken(String username) {
        return memberRepository.existsByUsername(username);
    }
}
