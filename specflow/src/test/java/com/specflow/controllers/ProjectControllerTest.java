package com.specflow.controllers;

import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.AlreadyParticipantException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.UserNotFoundException;
import com.specflow.exceptions.ValidationException;
import com.specflow.services.CrcCardService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests για το ProjectController — καλύπτει UC01 (Δημιουργία Project) και
 * UC02 (Διαγραφή Project) σε επίπεδο HTTP/redirect/flash.
 */
@WebMvcTest(controllers = ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private UseCaseService useCaseService;

    @MockBean
    private CrcCardService crcCardService;

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
    // UC01 — Δημιουργία Project (POST /projects)
    // =========================================================================

    @Nested
    @DisplayName("UC01 — POST /projects")
    class CreateProjectEndpointTests {

        @Test
        @DisplayName("TC-01-01: Happy Path — έγκυρο submit → 302 στο /projects/{id} + flash success")
        void submitCreateForm_withValidData_shouldRedirectToDetailWithFlash() throws Exception {
            when(projectService.createProject(any(), any())).thenReturn(42L);

            mockMvc.perform(post("/projects")
                            .with(user(developerUser))
                            .param("name", "Test Project Alpha")
                            .param("description", "Δοκιμαστική περιγραφή project"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/42"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Project created successfully")));
        }

        @Test
        @DisplayName("TC-01-02: Alt Flow — ValidationException → 302 στο /projects/new + flash error")
        void submitCreateForm_withEmptyName_shouldRedirectToFormWithError() throws Exception {
            when(projectService.createProject(any(), any()))
                    .thenThrow(new ValidationException("Name is required."));

            mockMvc.perform(post("/projects")
                            .with(user(developerUser))
                            .param("name", "")
                            .param("description", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/new"))
                    .andExpect(flash().attribute("errorMessage", "Name is required."))
                    .andExpect(flash().attributeExists("projectDto"));
        }
    }

    // =========================================================================
    // UC02 — Διαγραφή Project (POST /projects/{id}/delete)
    // =========================================================================

    @Nested
    @DisplayName("UC02 — POST /projects/{id}/delete")
    class DeleteProjectEndpointTests {

        @Test
        @DisplayName("TC-02-01: Happy Path — owner deletes → 302 /projects + flash success")
        void confirmDelete_byOwner_shouldRedirectToListWithFlash() throws Exception {
            mockMvc.perform(post("/projects/10/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects"))
                    .andExpect(flash().attribute("successMessage", "Project deleted successfully"));
        }

        @Test
        @DisplayName("TC-02-03: Alt Flow — UnauthorizedException → 302 /projects/{id} + flash error")
        void confirmDelete_byUnauthorizedUser_shouldRedirectToDetailWithError() throws Exception {
            doThrow(new UnauthorizedException("You do not have permission to delete this project"))
                    .when(projectService).deleteProject(eq(10L), any());

            mockMvc.perform(post("/projects/10/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/projects/10"))
                    .andExpect(flash().attribute("errorMessage",
                            "You do not have permission to delete this project"));
        }
    }

    // =========================================================================
    // UC12 — Διαμοιρασμός Project (Participants endpoints)
    // =========================================================================

    @Nested
    @DisplayName("UC12 — Participants endpoints")
    class ShareProjectEndpointTests {

        @Test
        @DisplayName("TC-12-01: Happy Path — POST /participants → 302 + flash success")
        void addParticipant_withValidData_shouldRedirectWithFlash() throws Exception {
            when(projectService.addParticipant(eq(10L), eq("testuser1"), eq(Role.COLLABORATOR)))
                    .thenReturn(555L);

            mockMvc.perform(post("/projects/10/participants")
                            .with(user(developerUser))
                            .param("username", "testuser1")
                            .param("role", "COLLABORATOR"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/10/participants"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Participant added successfully")));
        }

        @Test
        @DisplayName("TC-12-02: Alt Flow — UserNotFoundException → 302 /participants/new + flash error")
        void addParticipant_withNonExistentUsername_shouldRedirectToFormWithError() throws Exception {
            doThrow(new UserNotFoundException("User with this username not found"))
                    .when(projectService).addParticipant(eq(10L), eq("nonexistentuser99"), any());

            mockMvc.perform(post("/projects/10/participants")
                            .with(user(developerUser))
                            .param("username", "nonexistentuser99")
                            .param("role", "REVIEWER"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/10/participants/new"))
                    .andExpect(flash().attribute("errorMessage",
                            "User with this username not found"));
        }

        @Test
        @DisplayName("TC-12-03: Alt Flow — AlreadyParticipantException → 302 /participants/new + flash error")
        void addParticipant_whenAlreadyParticipant_shouldRedirectToFormWithError() throws Exception {
            doThrow(new AlreadyParticipantException("User is already a participant"))
                    .when(projectService).addParticipant(eq(10L), eq("testuser1"), any());

            mockMvc.perform(post("/projects/10/participants")
                            .with(user(developerUser))
                            .param("username", "testuser1")
                            .param("role", "COLLABORATOR"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/10/participants/new"))
                    .andExpect(flash().attribute("errorMessage", "User is already a participant"));
        }

        @Test
        @DisplayName("TC-12-04: Happy Path — POST /participants/{id}/delete → 302 + flash success")
        void removeParticipant_shouldRedirectToParticipantsWithFlash() throws Exception {
            mockMvc.perform(post("/projects/10/participants/555/delete")
                            .with(user(developerUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/10/participants"))
                    .andExpect(flash().attribute("successMessage",
                            containsString("Participant removed successfully")));
        }
    }
}
