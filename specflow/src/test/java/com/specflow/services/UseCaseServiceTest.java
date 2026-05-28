package com.specflow.services;

import com.specflow.domain.Actor;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.UseCase;
import com.specflow.domain.UseCaseStatus;
import com.specflow.domain.User;
import com.specflow.dto.UseCaseDto;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ActorRepository;
import com.specflow.repositories.ProjectRepository;
import com.specflow.repositories.UseCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το UseCaseService — καλύπτει τα Use Cases UC03 (Δημιουργία Use Case)
 * και UC04 (Επεξεργασία Use Case) σύμφωνα με το test plan
 * {@code test/test-plan-uc01-uc04.md}.
 */
@ExtendWith(MockitoExtension.class)
class UseCaseServiceTest {

    @Mock
    private UseCaseRepository useCaseRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ActorRepository actorRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UseCaseService useCaseService;

    private User developerUser;
    private Project project;
    private Actor actor;

    @BeforeEach
    void setUp() {
        developerUser = new User();
        developerUser.setId(1L);
        developerUser.setUsername("dev_user");
        developerUser.setRole(Role.DEVELOPER);

        project = new Project();
        project.setId(100L);
        project.setName("Existing Project");
        project.setOwner(developerUser);

        actor = new Actor();
        actor.setId(50L);
        actor.setName("Developer");
        actor.setProject(project);
    }

    // =========================================================================
    // UC03 — Δημιουργία Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC03 — Δημιουργία Use Case")
    class CreateUseCaseTests {

        @Test
        @DisplayName("TC-03-01: Happy Path — δημιουργία UC με όνομα + actor + ροές")
        void createUseCase_withValidData_shouldSaveWithPendingStatus() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("Δημιουργία Λογαριασμού");
            dto.setActorIds(List.of(50L));
            dto.setPrecondition("Ο χρήστης δεν έχει λογαριασμό");
            dto.setMainFlow("1. Εισάγει στοιχεία\n2. Σύστημα αποθηκεύει");
            dto.setAlternativeFlow("2.α.1 Email υπάρχει");
            dto.setPostcondition("Λογαριασμός δημιουργήθηκε");

            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(actorRepository.findAllById(List.of(50L))).thenReturn(List.of(actor));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> {
                UseCase uc = inv.getArgument(0);
                uc.setId(999L);
                return uc;
            });

            Long resultId = useCaseService.createUseCase(100L, dto, developerUser);

            assertThat(resultId).isEqualTo(999L);

            ArgumentCaptor<UseCase> captor = ArgumentCaptor.forClass(UseCase.class);
            verify(useCaseRepository, times(1)).save(captor.capture());

            UseCase saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Δημιουργία Λογαριασμού");
            assertThat(saved.getStatus()).isEqualTo(UseCaseStatus.PENDING);
            assertThat(saved.getProject()).isEqualTo(project);
            assertThat(saved.getAuthor()).isEqualTo(developerUser);
            assertThat(saved.getActors()).containsExactly(actor);
        }

        @Test
        @DisplayName("TC-03-02: Alt Flow — κενό Name → ValidationException")
        void createUseCase_withEmptyName_shouldThrowValidationException() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("");
            dto.setActorIds(List.of(50L));

            assertThatThrownBy(() -> useCaseService.createUseCase(100L, dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("όνομα Use Case");

            verify(useCaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-03-03: Alt Flow — κανένας Actor επιλεγμένος → ValidationException")
        void createUseCase_withNoActors_shouldThrowValidationException() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("Έγκυρο όνομα");
            dto.setActorIds(new ArrayList<>());

            assertThatThrownBy(() -> useCaseService.createUseCase(100L, dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Actor");

            verify(useCaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-03-03b: Alt Flow — null actorIds → ValidationException")
        void createUseCase_withNullActorIds_shouldThrowValidationException() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("Έγκυρο όνομα");
            dto.setActorIds(null);

            assertThatThrownBy(() -> useCaseService.createUseCase(100L, dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Actor");

            verify(useCaseRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-03-04: Edge — ελάχιστα δεδομένα (Name + 1 Actor, χωρίς ροές)")
        void createUseCase_withMinimalData_shouldSucceed() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("Minimal Use Case");
            dto.setActorIds(List.of(50L));

            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(actorRepository.findAllById(List.of(50L))).thenReturn(List.of(actor));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> {
                UseCase uc = inv.getArgument(0);
                uc.setId(1000L);
                return uc;
            });

            Long resultId = useCaseService.createUseCase(100L, dto, developerUser);

            assertThat(resultId).isEqualTo(1000L);

            ArgumentCaptor<UseCase> captor = ArgumentCaptor.forClass(UseCase.class);
            verify(useCaseRepository).save(captor.capture());
            UseCase saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Minimal Use Case");
            assertThat(saved.getPrecondition()).isNull();
            assertThat(saved.getMainFlow()).isNull();
            assertThat(saved.getAlternativeFlow()).isNull();
            assertThat(saved.getPostcondition()).isNull();
            assertThat(saved.getStatus()).isEqualTo(UseCaseStatus.PENDING);
        }
    }

    // =========================================================================
    // UC04 — Επεξεργασία Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC04 — Επεξεργασία Use Case")
    class UpdateUseCaseTests {

        private UseCase existingUseCase;

        @BeforeEach
        void setUpUseCase() {
            UseCaseDto initial = new UseCaseDto();
            initial.setName("Original Name");
            initial.setActorIds(List.of(50L));

            existingUseCase = new UseCase(initial, project, List.of(actor), developerUser);
            existingUseCase.setId(500L);
        }

        @Test
        @DisplayName("TC-04-01: Happy Path — edit PENDING UC, status παραμένει PENDING")
        void updateUseCase_whenPending_shouldKeepStatusPending() {
            existingUseCase.setStatus(UseCaseStatus.PENDING);

            UseCaseDto updateDto = new UseCaseDto();
            updateDto.setName("Updated Name");
            updateDto.setActorIds(List.of(50L));
            updateDto.setMainFlow("New flow");

            when(useCaseRepository.findById(500L)).thenReturn(Optional.of(existingUseCase));
            when(actorRepository.findAllById(List.of(50L))).thenReturn(List.of(actor));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            useCaseService.updateUseCase(500L, updateDto);

            assertThat(existingUseCase.getName()).isEqualTo("Updated Name");
            assertThat(existingUseCase.getMainFlow()).isEqualTo("New flow");
            assertThat(existingUseCase.getStatus()).isEqualTo(UseCaseStatus.PENDING);
            verify(useCaseRepository).save(existingUseCase);
        }

        @Test
        @DisplayName("TC-04-02: Happy Path — edit APPROVED UC → status επαναφέρεται σε PENDING")
        void updateUseCase_whenApproved_shouldResetToPending() {
            existingUseCase.setStatus(UseCaseStatus.APPROVED);

            UseCaseDto updateDto = new UseCaseDto();
            updateDto.setName("Τροποποιημένο Εγκεκριμένο UC");
            updateDto.setActorIds(List.of(50L));

            when(useCaseRepository.findById(500L)).thenReturn(Optional.of(existingUseCase));
            when(actorRepository.findAllById(anyList())).thenReturn(List.of(actor));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            useCaseService.updateUseCase(500L, updateDto);

            assertThat(existingUseCase.getStatus())
                    .as("Edit ενός APPROVED UC πρέπει να ενεργοποιεί resetToPending()")
                    .isEqualTo(UseCaseStatus.PENDING);
            assertThat(existingUseCase.getRejectionReason()).isNull();
            assertThat(existingUseCase.getName()).isEqualTo("Τροποποιημένο Εγκεκριμένο UC");
            verify(useCaseRepository).save(existingUseCase);
        }

        @Test
        @DisplayName("TC-04-02b: Happy Path — edit REJECTED UC → status επαναφέρεται σε PENDING")
        void updateUseCase_whenRejected_shouldResetToPending() {
            existingUseCase.setStatus(UseCaseStatus.REJECTED);
            existingUseCase.setRejectionReason("Παλιά αιτιολογία απόρριψης");

            UseCaseDto updateDto = new UseCaseDto();
            updateDto.setName("Διορθωμένο UC");
            updateDto.setActorIds(List.of(50L));

            when(useCaseRepository.findById(500L)).thenReturn(Optional.of(existingUseCase));
            when(actorRepository.findAllById(anyList())).thenReturn(List.of(actor));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            useCaseService.updateUseCase(500L, updateDto);

            assertThat(existingUseCase.getStatus()).isEqualTo(UseCaseStatus.PENDING);
            assertThat(existingUseCase.getRejectionReason())
                    .as("resetToPending() καθαρίζει το rejectionReason")
                    .isNull();
        }

        @Test
        @DisplayName("TC-04-03: Alt Flow — αφαίρεση Name κατά την επεξεργασία → ValidationException")
        void updateUseCase_withEmptyName_shouldThrowValidationException() {
            UseCaseDto updateDto = new UseCaseDto();
            updateDto.setName("");
            updateDto.setActorIds(List.of(50L));

            assertThatThrownBy(() -> useCaseService.updateUseCase(500L, updateDto))
                    .isInstanceOf(ValidationException.class);

            verify(useCaseRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC05 — Διαγραφή Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC05 — Διαγραφή Use Case")
    class DeleteUseCaseTests {

        private UseCase existingUseCase;
        private User orgOwnerUser;
        private User reviewerUser;

        @BeforeEach
        void setUpUseCase() {
            UseCaseDto initial = new UseCaseDto();
            initial.setName("To Be Deleted");
            initial.setActorIds(List.of(50L));

            existingUseCase = new UseCase(initial, project, List.of(actor), developerUser);
            existingUseCase.setId(700L);

            orgOwnerUser = new User();
            orgOwnerUser.setId(3L);
            orgOwnerUser.setUsername("owner_user");
            orgOwnerUser.setRole(Role.ORG_OWNER);

            reviewerUser = new User();
            reviewerUser.setId(4L);
            reviewerUser.setUsername("reviewer_user");
            reviewerUser.setRole(Role.REVIEWER);
        }

        @Test
        @DisplayName("TC-05-01: Happy Path — owner διαγράφει UC + cascade στις M:N συσχετίσεις")
        void deleteUseCase_byOwner_shouldDelete() {
            when(useCaseRepository.findById(700L)).thenReturn(Optional.of(existingUseCase));

            useCaseService.deleteUseCase(700L, developerUser);

            verify(useCaseRepository, times(1)).delete(existingUseCase);
        }

        @Test
        @DisplayName("TC-05-01b: Happy Path — ORG_OWNER διαγράφει UC άλλου χρήστη")
        void deleteUseCase_byOrgOwner_shouldDelete() {
            when(useCaseRepository.findById(700L)).thenReturn(Optional.of(existingUseCase));

            useCaseService.deleteUseCase(700L, orgOwnerUser);

            verify(useCaseRepository, times(1)).delete(existingUseCase);
        }

        @Test
        @DisplayName("TC-05-03: Alt Flow — Reviewer (χωρίς δικαίωμα) → UnauthorizedException")
        void deleteUseCase_byReviewer_shouldThrowUnauthorizedException() {
            when(useCaseRepository.findById(700L)).thenReturn(Optional.of(existingUseCase));

            assertThatThrownBy(() -> useCaseService.deleteUseCase(700L, reviewerUser))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("δικαίωμα");

            verify(useCaseRepository, never()).delete(any(UseCase.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό Use Case ID → NotFoundException")
        void deleteUseCase_withNonExistentId_shouldThrowNotFoundException() {
            when(useCaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCaseService.deleteUseCase(999L, developerUser))
                    .isInstanceOf(NotFoundException.class);

            verify(useCaseRepository, never()).delete(any(UseCase.class));
        }
    }

    // =========================================================================
    // UC16 — Έγκριση / Απόρριψη Use Case
    // =========================================================================

    @Nested
    @DisplayName("UC16 — Έγκριση / Απόρριψη Use Case")
    class ApproveRejectUseCaseTests {

        private UseCase pendingUseCase;

        @BeforeEach
        void setUpUseCase() {
            UseCaseDto dto = new UseCaseDto();
            dto.setName("Login");
            dto.setActorIds(List.of(50L));

            pendingUseCase = new UseCase(dto, project, List.of(actor), developerUser);
            pendingUseCase.setId(600L);
            pendingUseCase.setStatus(UseCaseStatus.PENDING);
        }

        @Test
        @DisplayName("TC-16-01: Happy Path — Approve PENDING UC → status=APPROVED + notification")
        void approve_pendingUseCase_shouldSetApprovedAndSendNotification() {
            when(useCaseRepository.findById(600L)).thenReturn(Optional.of(pendingUseCase));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            useCaseService.approve(600L);

            assertThat(pendingUseCase.getStatus()).isEqualTo(UseCaseStatus.APPROVED);
            assertThat(pendingUseCase.getRejectionReason())
                    .as("approve() καθαρίζει τυχόν υπάρχον rejectionReason")
                    .isNull();
            verify(useCaseRepository).save(pendingUseCase);
            verify(notificationService, times(1))
                    .sendApprovalNotification(pendingUseCase, developerUser);
        }

        @Test
        @DisplayName("TC-16-02: Happy Path — Reject με reason → status=REJECTED + notification")
        void reject_pendingUseCaseWithReason_shouldSetRejectedAndSendNotification() {
            when(useCaseRepository.findById(600L)).thenReturn(Optional.of(pendingUseCase));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            String reason = "Η βασική ροή είναι ελλιπής — χρειάζεται πιο αναλυτική περιγραφή.";
            useCaseService.reject(600L, reason);

            assertThat(pendingUseCase.getStatus()).isEqualTo(UseCaseStatus.REJECTED);
            assertThat(pendingUseCase.getRejectionReason()).isEqualTo(reason);
            verify(useCaseRepository).save(pendingUseCase);
            verify(notificationService, times(1))
                    .sendRejectionNotification(pendingUseCase, developerUser, reason);
        }

        @Test
        @DisplayName("TC-16-04: Edge — Reject χωρίς reason (null, optional field) → status=REJECTED")
        void reject_withNullReason_shouldStillReject() {
            when(useCaseRepository.findById(600L)).thenReturn(Optional.of(pendingUseCase));
            when(useCaseRepository.save(any(UseCase.class))).thenAnswer(inv -> inv.getArgument(0));

            useCaseService.reject(600L, null);

            assertThat(pendingUseCase.getStatus()).isEqualTo(UseCaseStatus.REJECTED);
            assertThat(pendingUseCase.getRejectionReason()).isNull();
            verify(notificationService, times(1))
                    .sendRejectionNotification(pendingUseCase, developerUser, null);
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό UC ID στο approve → NotFoundException")
        void approve_withNonExistentId_shouldThrowNotFoundException() {
            when(useCaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCaseService.approve(999L))
                    .isInstanceOf(NotFoundException.class);

            verify(useCaseRepository, never()).save(any(UseCase.class));
            verify(notificationService, never()).sendApprovalNotification(any(), any());
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό UC ID στο reject → NotFoundException")
        void reject_withNonExistentId_shouldThrowNotFoundException() {
            when(useCaseRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> useCaseService.reject(999L, "reason"))
                    .isInstanceOf(NotFoundException.class);

            verify(useCaseRepository, never()).save(any(UseCase.class));
            verify(notificationService, never())
                    .sendRejectionNotification(any(), any(), any());
        }
    }
}
