package org.zerock.board.dto;

import lombok.*;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {
    private String username;
    private String password;
    private String name;
}
