package com.study.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class BoardController {
    @GetMapping("/board/write")
    public String boardWriteForm() {

        return "boardwrite.html";
    }
    @PostMapping("/board/writepro")
    public String boardWritePro(String title, String content) {

        return "";
    }
}


