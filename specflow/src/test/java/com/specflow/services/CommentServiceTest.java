package com.specflow.services;

import com.specflow.domain.Comment;
import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.UseCase;
import com.specflow.domain.User;
import com.specflow.dto.CrcCardDto;
import com.specflow.dto.UseCaseDto;
import com.specflow.exceptions.EmptyCommentException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.repositories.CommentRepository;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.UseCaseRepository;
import com.specflow.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το CommentService — καλύπτει UC14 (Προσθήκη Σχολίου) και
 * UC15 (Διαγραφή Σχολίου) σύμφωνα με το test plan
 * {@code test/test-plan-uc13-uc16.md}.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UseCaseRepository useCaseRepository;

    @Mock
    private CrcCardRepository crcCardRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentService commentService;

    private User reviewerUser;
    private User otherUser;
    private Project project;
    private UseCase useCase;
    private CrcCard crcCard;

    @BeforeEach
    void setUp() {
        reviewerUser = new User();
        reviewerUser.setId(10L);
        reviewerUser.setUsername("reviewer_user");
        reviewerUser.setEmail("reviewer@example.com");
        reviewerUser.setRole(Role.REVIEWER);

        otherUser = new User();
        otherUser.setId(11L);
        otherUser.setUsername("other_user");
        otherUser.setRole(Role.COLLABORATOR);

        project = new Project();
        project.setId(100L);
        project.setName("Project Alpha");

        UseCaseDto ucDto = new UseCaseDto();
        ucDto.setName("Login");
        useCase = new UseCase(ucDto, project, List.of());
        useCase.setId(500L);

        CrcCardDto crcDto = new CrcCardDto();
        crcDto.setClassName("UserService");
        crcCard = new CrcCard(crcDto, project);
        crcCard.setId(800L);
    }

    // =========================================================================
    // UC14 — Προσθήκη Σχολίου
    // =========================================================================

    @Nested
    @DisplayName("UC14 — Προσθήκη Σχολίου")
    class CreateCommentTests {

        @Test
        @DisplayName("TC-14-01: Happy Path — σχόλιο σε Use Case (polymorphic)")
        void createComment_onUseCase_shouldSaveWithUseCaseTarget() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(reviewerUser));
            when(useCaseRepository.findById(500L)).thenReturn(Optional.of(useCase));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1000L);
                return c;
            });

            Long resultId = commentService.createComment(
                    CommentService.TARGET_USE_CASE, 500L,
                    "Αυτό το UC χρειάζεται αναθεώρηση.", 10L);

            assertThat(resultId).isEqualTo(1000L);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());

            Comment saved = captor.getValue();
            assertThat(saved.getBody()).isEqualTo("Αυτό το UC χρειάζεται αναθεώρηση.");
            assertThat(saved.getAuthor()).isEqualTo(reviewerUser);
            assertThat(saved.getUseCase()).isEqualTo(useCase);
            assertThat(saved.getProject()).isEqualTo(project);
            assertThat(saved.getCrcCard()).as("Polymorphic isolation: crcCard πρέπει να είναι null").isNull();
            assertThat(saved.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("TC-14-02: Happy Path — σχόλιο σε CRC Card (polymorphic)")
        void createComment_onCrcCard_shouldSaveWithCrcCardTarget() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(reviewerUser));
            when(crcCardRepository.findById(800L)).thenReturn(Optional.of(crcCard));
            when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
                Comment c = inv.getArgument(0);
                c.setId(1001L);
                return c;
            });

            Long resultId = commentService.createComment(
                    CommentService.TARGET_CRC_CARD, 800L,
                    "Η κλάση χρειάζεται επιπλέον responsibility.", 10L);

            assertThat(resultId).isEqualTo(1001L);

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());

            Comment saved = captor.getValue();
            assertThat(saved.getCrcCard()).isEqualTo(crcCard);
            assertThat(saved.getProject()).isEqualTo(project);
            assertThat(saved.getUseCase()).as("Polymorphic isolation: useCase πρέπει να είναι null").isNull();
        }

        @Test
        @DisplayName("TC-14-03: Alt Flow — κενό text → EmptyCommentException")
        void createComment_withEmptyText_shouldThrowEmptyCommentException() {
            assertThatThrownBy(() -> commentService.createComment(
                    CommentService.TARGET_USE_CASE, 500L, "", 10L))
                    .isInstanceOf(EmptyCommentException.class)
                    .hasMessage("Comment cannot be empty");

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("TC-14-03b: Alt Flow — null text → EmptyCommentException")
        void createComment_withNullText_shouldThrowEmptyCommentException() {
            assertThatThrownBy(() -> commentService.createComment(
                    CommentService.TARGET_USE_CASE, 500L, null, 10L))
                    .isInstanceOf(EmptyCommentException.class);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("TC-14-03c: Edge — whitespace-only text → EmptyCommentException")
        void createComment_withWhitespaceText_shouldThrowEmptyCommentException() {
            assertThatThrownBy(() -> commentService.createComment(
                    CommentService.TARGET_USE_CASE, 500L, "   ", 10L))
                    .isInstanceOf(EmptyCommentException.class);

            verify(commentRepository, never()).save(any(Comment.class));
        }

        @Test
        @DisplayName("Alt — άγνωστο targetType → NotFoundException")
        void createComment_withUnknownTargetType_shouldThrowNotFoundException() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(reviewerUser));

            assertThatThrownBy(() -> commentService.createComment(
                    "UNKNOWN_TYPE", 500L, "Some text", 10L))
                    .isInstanceOf(NotFoundException.class);

            verify(commentRepository, never()).save(any(Comment.class));
        }
    }

    // =========================================================================
    // UC15 — Διαγραφή Σχολίου
    // =========================================================================

    @Nested
    @DisplayName("UC15 — Διαγραφή Σχολίου")
    class DeleteCommentTests {

        private Comment comment;

        @BeforeEach
        void setUpComment() {
            comment = new Comment();
            comment.setId(2000L);
            comment.setBody("To be deleted");
            comment.setAuthor(reviewerUser);
            comment.setProject(project);
            comment.setUseCase(useCase);
        }

        @Test
        @DisplayName("TC-15-01: Happy Path — συντάκτης διαγράφει δικό του σχόλιο")
        void removeComment_byAuthor_shouldDelete() {
            when(commentRepository.findById(2000L)).thenReturn(Optional.of(comment));

            commentService.removeComment(2000L, reviewerUser);

            verify(commentRepository, times(1)).delete(comment);
        }

        @Test
        @DisplayName("Alt — non-author προσπαθεί να διαγράψει → UnauthorizedException")
        void removeComment_byNonAuthor_shouldThrowUnauthorizedException() {
            when(commentRepository.findById(2000L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.removeComment(2000L, otherUser))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You do not have permission to delete this comment");

            verify(commentRepository, never()).delete(any(Comment.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό σχόλιο → NotFoundException")
        void removeComment_withNonExistentId_shouldThrowNotFoundException() {
            when(commentRepository.findById(9999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.removeComment(9999L, reviewerUser))
                    .isInstanceOf(NotFoundException.class);

            verify(commentRepository, never()).delete(any(Comment.class));
        }
    }
}
