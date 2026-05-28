package com.specflow.controllers;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.services.CommentService;
import com.specflow.services.ProjectService;
import com.specflow.services.UseCaseService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests για το UseCaseController — καλύπτει UC03 (Δημιουργία Use Case)
 * και UC04 (Επεξεργασία Use Case) σε επίπεδο HTTP/redirect/flash.
 */
@WebMvcTest(controllers = UseCaseController.class)
@AutoConfigureMockMvc(addFilters = false)
class UseCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UseCaseService useCaseService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ActorRepository actorRepository;

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
    // UC03 — Δημιουργία Use Case (POST /projects/{projectId}/usecases)
    // =========================================================================

    @Nested
    @DisplayName("UC03 — POST /projects/{projectId}/usecases")
    class CreateUseCaseEndpointTests {

        @Test
        @DisplayName("TC-03-01: Happy Path — έγκυρο submit → 302 στο detail + flash success")
        void submitCreateForm_withValidData_shouldRedirectToDetailWithFlash() throws Exception {
            when(useCaseService.createUseCase(eq(100L), any(), any())).thenReturn(999L);

            mockMvc.perform(post("/projects/100/usecases")
                            .with(user(developerUser))
                            .param("name", "Δημιουργία Λογαριασμού")
                            .param("actorIds", "50")
                            .param("mainFlow", "1. Step one\n2. Step two"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/999"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Use Case saved successfully")));
        }

        @Test
        @DisplayName("TC-03-02: Alt Flow — κενό Name → 302 στο /new + flash error")
        void submitCreateForm_withEmptyName_shouldRedirectToFormWithError() throws Exception {
            when(useCaseService.createUseCase(anyLong(), any(), any()))
                    .thenThrow(new ValidationException("Το όνομα Use Case είναι υποχρεωτικό."));

            mockMvc.perform(post("/projects/100/usecases")
                            .with(user(developerUser))
                            .param("name", "")
                            .param("actorIds", "50"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/new"))
                    .andExpect(flash().attribute("errorMessage",
                            "Το όνομα Use Case είναι υποχρεωτικό."))
                    .andExpect(flash().attributeExists("useCaseDto"));
        }

        @Test
        @DisplayName("TC-03-03: Alt Flow — κανένας Actor → 302 στο /new + flash error")
        void submitCreateForm_withNoActors_shouldRedirectToFormWithError() throws Exception {
            when(useCaseService.createUseCase(anyLong(), any(), any()))
                    .thenThrow(new ValidationException("Πρέπει να επιλέξετε τουλάχιστον έναν Actor."));

            mockMvc.perform(post("/projects/100/usecases")
                            .with(user(developerUser))
                            .param("name", "Έγκυρο όνομα"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/new"))
                    .andExpect(flash().attribute("errorMessage",
                            containsString("Actor")));
        }
    }

    // =========================================================================
    // UC04 — Επεξεργασία Use Case (POST /projects/{projectId}/usecases/{id})
    // =========================================================================

    @Nested
    @DisplayName("UC04 — POST /projects/{projectId}/usecases/{id}")
    class UpdateUseCaseEndpointTests {

        @Test
        @DisplayName("TC-04-01: Happy Path — έγκυρο edit → 302 στο detail + flash success")
        void submitEditForm_withValidData_shouldRedirectToDetailWithFlash() throws Exception {
            when(useCaseService.updateUseCase(eq(500L), any())).thenReturn(500L);

            mockMvc.perform(post("/projects/100/usecases/500")
                            .with(user(developerUser))
                            .param("name", "Updated Name")
                            .param("actorIds", "50")
                            .param("mainFlow", "Updated flow"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/500"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Changes saved successfully")));
        }

        @Test
        @DisplayName("TC-04-03: Alt Flow — Validation error → 302 στο /edit + flash error")
        void submitEditForm_withInvalidData_shouldRedirectToEditFormWithError() throws Exception {
            when(useCaseService.updateUseCase(eq(500L), any()))
                    .thenThrow(new ValidationException("Το όνομα Use Case είναι υποχρεωτικό."));

            mockMvc.perform(post("/projects/100/usecases/500")
                            .with(user(developerUser))
                            .param("name", "")
                            .param("actorIds", "50"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/500/edit"))
                    .andExpect(flash().attribute("errorMessage",
                            "Το όνομα Use Case είναι υποχρεωτικό."))
                    .andExpect(flash().attributeExists("useCaseDto"));
        }
    }

    // =========================================================================
    // UC05 — Διαγραφή Use Case (POST /projects/{projectId}/usecases/{id}/delete)
    // =========================================================================

    @Nested
    @DisplayName("UC05 — POST /projects/{projectId}/usecases/{id}/delete")
    class DeleteUseCaseEndpointTests {

        @Test
        @DisplayName("TC-05-01: Happy Path — owner deletes → 302 /projects/{projectId} + flash success")
        void confirmDelete_byOwner_shouldRedirectToProjectDetailWithFlash() throws Exception {
            mockMvc.perform(post("/projects/100/usecases/700/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Use Case deleted successfully")));
        }
    }

    // =========================================================================
    // UC16 — Έγκριση / Απόρριψη Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC16 — Approve / Reject endpoints")
    class ApproveRejectEndpointTests {

        @Test
        @DisplayName("TC-16-01: Happy Path — POST /approve → 302 στο detail + flash success")
        void approveUseCase_shouldRedirectToDetailWithFlash() throws Exception {
            mockMvc.perform(post("/projects/100/usecases/600/approve")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/600"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Use Case approved")));
        }

        @Test
        @DisplayName("TC-16-02: Happy Path — POST /reject με reason → 302 στο detail + flash success")
        void rejectUseCase_withReason_shouldRedirectToDetailWithFlash() throws Exception {
            mockMvc.perform(post("/projects/100/usecases/600/reject")
                            .with(user(developerUser))
                            .param("reason", "Η ροή είναι ελλιπής"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/600"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Use Case rejected")));
        }

        @Test
        @DisplayName("TC-16-04: Edge — POST /reject χωρίς reason (optional) → 302 + flash success")
        void rejectUseCase_withoutReason_shouldStillRedirectWithFlash() throws Exception {
            mockMvc.perform(post("/projects/100/usecases/600/reject")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/600"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Use Case rejected")));
        }
    }
}
