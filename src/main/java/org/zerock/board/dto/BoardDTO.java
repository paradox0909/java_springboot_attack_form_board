package org.zerock.board.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {
    private Long bno;
    private String title, content;
    private String writerId, writerName, writerUsername;
    private LocalDateTime regDate, modDate;
    private int replyCount;
}
