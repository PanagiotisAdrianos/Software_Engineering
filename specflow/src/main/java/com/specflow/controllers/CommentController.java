package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/projects/{projectId}/comments")
public class CommentController extends BaseController {

    public String addCommentToUseCase() {
        // TODO: Map to UC14
        return "redirect:/projects/";
    }

    public String addCommentToCrcCard() {
        // TODO: Map to UC14
        return "redirect:/projects/";
    }

    public String deleteComment() {
        // TODO: Delete comment
        return "redirect:/projects/";
    }
}
