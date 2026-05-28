package com.specflow.controllers;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.services.CommentService;
import com.specflow.services.CrcCardService;
import com.specflow.services.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests για το CrcCardController — καλύπτει UC06 (Δημιουργία CRC Card),
 * UC07 (Επεξεργασία CRC Card) και UC08 (Διαγραφή CRC Card) σε επίπεδο HTTP.
 */
@WebMvcTest(controllers = CrcCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CrcCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CrcCardService crcCardService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private CommentService commentService;

    private User developerUser;

    @BeforeEach
    void setUp() {
        developerUser = new User();
        developerUser.setId(1L);
        developerUser.setUsername("dev_user");
        developerUser.setPassword("encoded");
        developerUser.setEmail("dev@example.com");
        developerUser.setRole(Role.DEVELOPER);
    }

    // =========================================================================
    // UC06 — Δημιουργία CRC Card (POST /projects/{projectId}/crccards)
    // =========================================================================

    @Nested
    @DisplayName("UC06 — POST /projects/{projectId}/crccards")
    class CreateCrcCardEndpointTests {

        @Test
        @DisplayName("TC-06-01: Happy Path — έγκυρο submit → 302 στο detail + flash success")
        void submitCreateForm_withValidData_shouldRedirectToDetailWithFlash() throws Exception {
            when(crcCardService.createCrcCard(eq(100L), any())).thenReturn(700L);

            mockMvc.perform(post("/projects/100/crccards")
                            .with(user(developerUser))
                            .param("className", "OrderService")
                            .param("responsibilities", "validateOrder\nprocessPayment")
                            .param("collaborations", "PaymentGateway\nInventory"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/700"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("CRC Card saved successfully")));
        }

        @Test
        @DisplayName("TC-06-02: Alt Flow — κενό Class Name → 302 στο /new + flash error")
        void submitCreateForm_withEmptyClassName_shouldRedirectToFormWithError() throws Exception {
            when(crcCardService.createCrcCard(anyLong(), any()))
                    .thenThrow(new ValidationException("Class Name is required."));

            mockMvc.perform(post("/projects/100/crccards")
                            .with(user(developerUser))
                            .param("className", "")
                            .param("responsibilities", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/new"))
                    .andExpect(flash().attribute("errorMessage", "Class Name is required."))
                    .andExpect(flash().attributeExists("crcCardDto"));
        }

        @Test
        @DisplayName("TC-06-03: Alt Flow — διπλότυπο Class Name → 302 στο /new + flash error")
        void submitCreateForm_withDuplicateClassName_shouldRedirectToFormWithError() throws Exception {
            when(crcCardService.createCrcCard(anyLong(), any()))
                    .thenThrow(new ValidationException(
                            "A CRC Card with the name \"UserService\" already exists in this project."));

            mockMvc.perform(post("/projects/100/crccards")
                            .with(user(developerUser))
                            .param("className", "UserService"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/new"))
                    .andExpect(flash().attribute("errorMessage",
                            containsString("already exists")));
        }
    }

    // =========================================================================
    // UC07 — Επεξεργασία CRC Card (POST /projects/{projectId}/crccards/{id})
    // =========================================================================

    @Nested
    @DisplayName("UC07 — POST /projects/{projectId}/crccards/{id}")
    class UpdateCrcCardEndpointTests {

        @Test
        @DisplayName("TC-07-01: Happy Path — έγκυρο edit → 302 στο detail + flash success")
        void submitEditForm_withValidData_shouldRedirectToDetailWithFlash() throws Exception {
            when(crcCardService.updateCrcCard(eq(800L), any())).thenReturn(800L);

            mockMvc.perform(post("/projects/100/crccards/800")
                            .with(user(developerUser))
                            .param("className", "NewOrderService")
                            .param("responsibilities", "updated")
                            .param("collaborations", "updated"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/800"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Changes saved successfully")));
        }

        @Test
        @DisplayName("TC-07-02: Alt Flow — duplicate Class Name → 302 στο /edit + flash error")
        void submitEditForm_withDuplicateClassName_shouldRedirectToEditFormWithError() throws Exception {
            when(crcCardService.updateCrcCard(eq(800L), any()))
                    .thenThrow(new ValidationException(
                            "A CRC Card with the name \"CardB\" already exists in this project."));

            mockMvc.perform(post("/projects/100/crccards/800")
                            .with(user(developerUser))
                            .param("className", "CardB"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/800/edit"))
                    .andExpect(flash().attribute("errorMessage",
                            containsString("already exists")))
                    .andExpect(flash().attributeExists("crcCardDto"));
        }
    }

    // =========================================================================
    // UC08 — Διαγραφή CRC Card (POST /projects/{projectId}/crccards/{id}/delete)
    // =========================================================================

    @Nested
    @DisplayName("UC08 — POST /projects/{projectId}/crccards/{id}/delete")
    class DeleteCrcCardEndpointTests {

        @Test
        @DisplayName("TC-08-01: Happy Path — owner deletes → 302 στο /projects/{projectId} + flash success")
        void confirmDelete_byOwner_shouldRedirectToProjectDetailWithFlash() throws Exception {
            mockMvc.perform(post("/projects/100/crccards/900/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100"))
                    .andExpect(flash().attribute("successMessage",
                            "CRC Card deleted successfully"));
        }

        @Test
        @DisplayName("TC-08-03: Alt Flow — UnauthorizedException → 302 στο /crccards/{id} + flash error")
        void confirmDelete_byUnauthorizedUser_shouldRedirectToDetailWithError() throws Exception {
            doThrow(new UnauthorizedException("You do not have permission to delete this CRC Card"))
                    .when(crcCardService).deleteCrcCard(eq(900L), any());

            mockMvc.perform(post("/projects/100/crccards/900/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/900"))
                    .andExpect(flash().attribute("errorMessage",
                            "You do not have permission to delete this CRC Card"));
        }
    }
}
