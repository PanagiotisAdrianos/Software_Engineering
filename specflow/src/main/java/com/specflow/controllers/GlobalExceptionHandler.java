package com.specflow.controllers;

import com.specflow.exceptions.EmptyCommentException;
import com.specflow.exceptions.InsufficientDataException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public String handleValidation(ValidationException ex, RedirectAttributes ra,
                                   org.springframework.web.context.request.WebRequest req) {
        ra.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/projects");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public String handleUnauthorized(UnauthorizedException ex, RedirectAttributes ra,
                                     org.springframework.web.context.request.WebRequest req) {
        ra.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/projects");
    }

    @ExceptionHandler(EmptyCommentException.class)
    public String handleEmptyComment(EmptyCommentException ex, RedirectAttributes ra,
                                     org.springframework.web.context.request.WebRequest req) {
        ra.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/projects");
    }

    @ExceptionHandler(InsufficientDataException.class)
    public String handleInsufficientData(InsufficientDataException ex, RedirectAttributes ra,
                                         org.springframework.web.context.request.WebRequest req) {
        ra.addFlashAttribute("errorMessage", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/projects");
    }

    @ExceptionHandler(NotFoundException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException ex, org.springframework.ui.Model model) {
        model.addAttribute("errorMessage", ex.getMessage());
        return "error/404";
    }
}
