package com.specflow.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AuthController {

    public String loginPage() {
        return "auth/login";
    }

    public String login() {
        // TODO: Map to UC02
        return "redirect:/projects";
    }

    public String registerPage() {
        return "auth/register";
    }

    public String register() {
        // TODO: Map to UC01
        return "redirect:/login";
    }

    public String logout() {
        return "redirect:/login";
    }
}
