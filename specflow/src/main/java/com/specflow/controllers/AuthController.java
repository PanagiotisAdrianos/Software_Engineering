package com.specflow.controllers;

import com.specflow.dto.LoginDto;
import com.specflow.dto.RegisterDto;
import com.specflow.exceptions.ValidationException;
import com.specflow.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        if (error != null) {
            model.addAttribute("error", "Λάθος username ή κωδικός.");
        }
        if (logout != null) {
            model.addAttribute("success", "Αποσυνδεθήκατε επιτυχώς.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registerDto") RegisterDto dto,
                           RedirectAttributes ra) {
        try {
            userService.register(dto);
            ra.addFlashAttribute("success", "Η εγγραφή ολοκληρώθηκε. Συνδεθείτε.");
            return "redirect:/login";
        } catch (ValidationException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/")
    public String root() {
        return "homepage-screen";
    }
}
