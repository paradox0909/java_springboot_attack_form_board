package org.zerock.board.service;

import org.zerock.board.dto.MemberDTO;
import org.zerock.board.entity.Member;

public interface MemberService {
    long join(MemberDTO memberDTO);
    Member getMember(String username);
    boolean isUsernameTaken(String username);
}