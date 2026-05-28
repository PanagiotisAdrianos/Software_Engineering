package com.specflow.controllers;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.EmptyCommentException;
import com.specflow.services.CommentService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests για το CommentController — καλύπτει UC14 (Προσθήκη Σχολίου)
 * και UC15 (Διαγραφή Σχολίου).
 */
@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    private User reviewerUser;

    @BeforeEach
    void setUp() {
        reviewerUser = new User();
        reviewerUser.setId(10L);
        reviewerUser.setUsername("reviewer_user");
        reviewerUser.setPassword("encoded");
        reviewerUser.setEmail("reviewer@example.com");
        reviewerUser.setRole(Role.REVIEWER);
    }

    // =========================================================================
    // UC14 — POST /comments
    // =========================================================================

    @Nested
    @DisplayName("UC14 — POST /comments")
    class AddCommentEndpointTests {

        @Test
        @DisplayName("TC-14-01: Happy Path (USE_CASE target) — redirect στο UC detail + flash")
        void addComment_onUseCase_shouldRedirectToUseCaseDetail() throws Exception {
            when(commentService.createComment(
                    eq(CommentService.TARGET_USE_CASE), eq(500L),
                    anyString(), eq(10L))).thenReturn(1000L);

            mockMvc.perform(post("/comments")
                            .with(user(reviewerUser))
                            .param("targetType", CommentService.TARGET_USE_CASE)
                            .param("targetId", "500")
                            .param("projectId", "100")
                            .param("text", "Παρατήρηση για το UC."))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/500"))
                    .andExpect(flash().attribute("successMessage", "Comment added"));
        }

        @Test
        @DisplayName("TC-14-02: Happy Path (CRC_CARD target) — redirect στο CRC detail + flash")
        void addComment_onCrcCard_shouldRedirectToCrcCardDetail() throws Exception {
            when(commentService.createComment(
                    eq(CommentService.TARGET_CRC_CARD), eq(800L),
                    anyString(), eq(10L))).thenReturn(1001L);

            mockMvc.perform(post("/comments")
                            .with(user(reviewerUser))
                            .param("targetType", CommentService.TARGET_CRC_CARD)
                            .param("targetId", "800")
                            .param("projectId", "100")
                            .param("text", "Παρατήρηση για την κλάση."))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/800"))
                    .andExpect(flash().attribute("successMessage", "Comment added"));
        }

        @Test
        @DisplayName("TC-14-03: Alt Flow — EmptyCommentException → redirect + flash error")
        void addComment_withEmptyText_shouldRedirectWithError() throws Exception {
            when(commentService.createComment(
                    anyString(), anyLong(), anyString(), anyLong()))
                    .thenThrow(new EmptyCommentException("Comment cannot be empty"));

            mockMvc.perform(post("/comments")
                            .with(user(reviewerUser))
                            .param("targetType", CommentService.TARGET_USE_CASE)
                            .param("targetId", "500")
                            .param("projectId", "100")
                            .param("text", ""))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/500"))
                    .andExpect(flash().attribute("errorMessage", "Comment cannot be empty"));
        }
    }

    // =========================================================================
    // UC15 — POST /comments/{id}/delete
    // =========================================================================

    @Nested
    @DisplayName("UC15 — POST /comments/{id}/delete")
    class DeleteCommentEndpointTests {

        @Test
        @DisplayName("TC-15-01: Happy Path — συντάκτης διαγράφει σχόλιο → redirect + flash")
        void deleteComment_byAuthor_shouldRedirectWithFlash() throws Exception {
            mockMvc.perform(post("/comments/2000/delete")
                            .with(user(reviewerUser))
                            .param("targetType", CommentService.TARGET_USE_CASE)
                            .param("targetId", "500")
                            .param("projectId", "100"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/usecases/500"))
                    .andExpect(flash().attribute("successMessage", "Comment deleted"));
        }

        @Test
        @DisplayName("TC-15-01b: Happy Path (CRC target) — redirect στο CRC detail")
        void deleteComment_onCrcCardTarget_shouldRedirectToCrcCardDetail() throws Exception {
            mockMvc.perform(post("/comments/2000/delete")
                            .with(user(reviewerUser))
                            .param("targetType", CommentService.TARGET_CRC_CARD)
                            .param("targetId", "800")
                            .param("projectId", "100"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/projects/100/crccards/800"))
                    .andExpect(flash().attribute("successMessage", containsString("deleted")));
        }
    }
}
