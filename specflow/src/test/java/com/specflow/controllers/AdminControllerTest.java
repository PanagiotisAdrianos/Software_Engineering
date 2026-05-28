package com.specflow.controllers;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.RoleUnchangedException;
import com.specflow.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests για το AdminController — καλύπτει UC13 (Admin Panel: αλλαγή ρόλων).
 * Σημ.: Με {@code addFilters=false} παρακάμπτεται το RBAC. Το TC-13-03
 * (Security check) τεκμηριώνεται στο test plan ως manual verification.
 */
@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin_user");
        adminUser.setPassword("encoded");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole(Role.ADMIN);

        targetUser = new User();
        targetUser.setId(99L);
        targetUser.setUsername("targetuser");
        targetUser.setEmail("target@example.com");
        targetUser.setRole(Role.DEVELOPER);
    }

    @Test
    @DisplayName("TC-13-01: Happy Path — POST /admin/users/{id}/role → 302 + flash success")
    void changeRole_withValidNewRole_shouldRedirectWithFlash() throws Exception {
        when(userService.findUserById(99L)).thenReturn(targetUser);

        mockMvc.perform(post("/admin/users/99/role")
                        .with(user(adminUser))
                        .param("newRole", "COLLABORATOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage",
                        containsString("Role for user targetuser updated to COLLABORATOR")));
    }

    @Test
    @DisplayName("TC-13-02: Alt Flow — RoleUnchangedException → 302 /admin/users/{id}/role + flash error")
    void changeRole_withSameRole_shouldRedirectToFormWithError() throws Exception {
        when(userService.findUserById(99L)).thenReturn(targetUser);
        doThrow(new RoleUnchangedException("User already has this role"))
                .when(userService).changeUserRole(eq(99L), eq(Role.DEVELOPER));

        mockMvc.perform(post("/admin/users/99/role")
                        .with(user(adminUser))
                        .param("newRole", "DEVELOPER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/99/role"))
                .andExpect(flash().attribute("errorMessage", "User already has this role"));
    }
}
