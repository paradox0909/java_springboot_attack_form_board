package org.zerock.board.service;

import org.zerock.board.dto.BoardDTO;
import org.zerock.board.dto.PageRequestDTO;
import org.zerock.board.dto.PageResultDTO;
import org.zerock.board.entity.Board;
import org.zerock.board.entity.Member;

public interface BoardService {

    Long register(BoardDTO dto);
    PageResultDTO<BoardDTO, Object[]> getList(PageRequestDTO pageRequestDTO);
    BoardDTO get(Long bno);
    void removeWithReplies(Long bno);
    void modify(BoardDTO boardDTO);

    default Board dtoToEntity(BoardDTO dto) {
        Member member = Member.builder().id(Integer.parseInt(dto.getWriterId())).build();
        Board board = Board.builder()
                .bno(dto.getBno())
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(member)
                .build();
        return board;
    }

    default BoardDTO entityToDTO(Board board, Member member, Long replyCount) {
        BoardDTO boardDTO = BoardDTO.builder()
                .bno(board.getBno())
                .title(board.getTitle())
                .content(board.getContent())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .writerUsername(member.getUsername())
                .writerName(member.getName())
                .replyCount(replyCount.intValue())
                .build();
        return boardDTO;
    }
}
