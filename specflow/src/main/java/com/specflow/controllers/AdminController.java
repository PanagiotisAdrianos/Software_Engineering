package com.specflow.controllers;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.RoleUnchangedException;
import com.specflow.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController extends BaseController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String showUserManagement(@AuthenticationPrincipal User currentUser, Model model) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("currentUser", currentUser);
        return "user-management-screen";
    }

    @GetMapping("/users/{userId}/role")
    public String showChangeRole(@PathVariable Long userId, Model model) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        return "change-role-screen";
    }

    @PostMapping("/users/{userId}/role")
    public String changeRole(@PathVariable Long userId,
                             @RequestParam Role newRole,
                             RedirectAttributes ra) {
        try {
            User user = userService.findUserById(userId);
            userService.changeUserRole(userId, newRole);
            ra.addFlashAttribute("successMessage",
                    "Role for user " + user.getUsername() + " updated to " + newRole.name());
            return "redirect:/admin/users";
        } catch (RoleUnchangedException ex) {
            ra.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/users/" + userId + "/role";
        }
    }
}
