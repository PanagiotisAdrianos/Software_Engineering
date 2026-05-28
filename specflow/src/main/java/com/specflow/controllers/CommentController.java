package com.specflow.controllers;

import com.specflow.domain.Comment;
import com.specflow.domain.User;
import com.specflow.exceptions.EmptyCommentException;
import com.specflow.services.CommentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/comments")
public class CommentController extends BaseController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public String showComments(@RequestParam String targetType,
                               @RequestParam Long targetId,
                               Model model) {
        model.addAttribute("comments", commentService.findCommentsForTarget(targetType, targetId));
        model.addAttribute("targetType", targetType);
        model.addAttribute("targetId", targetId);
        return "fragments/comment-section :: section";
    }

    @PostMapping
    public String addComment(@RequestParam String targetType,
                             @RequestParam Long targetId,
                             @RequestParam(name = "projectId", required = false) Long projectId,
                             @RequestParam String text,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes ra) {
        try {
            commentService.createComment(targetType, targetId, text, currentUser.getId());
            ra.addFlashAttribute("successMessage", "Comment added");
        } catch (EmptyCommentException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return redirectToTarget(targetType, targetId, projectId);
    }

    @GetMapping("/{id}/delete-confirm")
    public String confirmDelete(@PathVariable Long id, Model model) {
        Comment comment = commentService.findCommentById(id);
        Long projectId = comment.getProject() != null ? comment.getProject().getId() : null;
        String targetType;
        Long targetId;
        if (comment.getUseCase() != null) {
            targetType = CommentService.TARGET_USE_CASE;
            targetId = comment.getUseCase().getId();
        } else {
            targetType = CommentService.TARGET_CRC_CARD;
            targetId = comment.getCrcCard().getId();
        }
        model.addAttribute("comment", comment);
        model.addAttribute("projectId", projectId);
        model.addAttribute("targetType", targetType);
        model.addAttribute("targetId", targetId);
        return "confirm-delete-comment";
    }

    @PostMapping("/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam String targetType,
                                @RequestParam Long targetId,
                                @RequestParam(name = "projectId", required = false) Long projectId,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes ra) {
        commentService.removeComment(id, currentUser);
        ra.addFlashAttribute("successMessage", "Comment deleted");
        return redirectToTarget(targetType, targetId, projectId);
    }

    private String redirectToTarget(String targetType, Long targetId, Long projectId) {
        if (CommentService.TARGET_USE_CASE.equalsIgnoreCase(targetType)) {
            return "redirect:/projects/" + (projectId != null ? projectId : 0)
                    + "/usecases/" + targetId;
        } else {
            return "redirect:/projects/" + (projectId != null ? projectId : 0)
                    + "/crccards/" + targetId;
        }
    }
}
