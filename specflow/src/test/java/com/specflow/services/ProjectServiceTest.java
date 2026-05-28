package com.specflow.services;

import com.specflow.domain.Participant;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.ProjectDto;
import com.specflow.exceptions.AlreadyParticipantException;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.UserNotFoundException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.ParticipantRepository;
import com.specflow.repositories.ProjectRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το ProjectService — καλύπτει τα Use Cases UC01 (Δημιουργία Project)
 * και UC02 (Διαγραφή Project) σύμφωνα με το test plan
 * {@code test/test-plan-uc01-uc04.md}.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    private User developerUser;
    private User otherUser;
    private User orgOwnerUser;

    @BeforeEach
    void setUp() {
        developerUser = new User();
        developerUser.setId(1L);
        developerUser.setUsername("dev_user");
        developerUser.setRole(Role.DEVELOPER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("other_user");
        otherUser.setRole(Role.DEVELOPER);

        orgOwnerUser = new User();
        orgOwnerUser.setId(3L);
        orgOwnerUser.setUsername("owner_user");
        orgOwnerUser.setRole(Role.ORG_OWNER);
    }

    // =========================================================================
    // UC01 — Δημιουργία Project
    // =========================================================================

    @Nested
    @DisplayName("UC01 — Δημιουργία Project")
    class CreateProjectTests {

        @Test
        @DisplayName("TC-01-01: Happy Path — δημιουργία Project με όνομα και περιγραφή")
        void createProject_withValidNameAndDescription_shouldSaveAndReturnId() {
            ProjectDto dto = new ProjectDto();
            dto.setName("Test Project Alpha");
            dto.setDescription("Δοκιμαστική περιγραφή project");

            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> {
                Project p = inv.getArgument(0);
                p.setId(42L);
                return p;
            });

            Long resultId = projectService.createProject(dto, developerUser);

            assertThat(resultId).isEqualTo(42L);

            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository, times(1)).save(captor.capture());

            Project saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Test Project Alpha");
            assertThat(saved.getDescription()).isEqualTo("Δοκιμαστική περιγραφή project");
            assertThat(saved.getOwner()).isEqualTo(developerUser);
        }

        @Test
        @DisplayName("TC-01-02: Alt Flow — κενό Name → ValidationException")
        void createProject_withEmptyName_shouldThrowValidationException() {
            ProjectDto dto = new ProjectDto();
            dto.setName("");
            dto.setDescription("Οποιαδήποτε περιγραφή");

            assertThatThrownBy(() -> projectService.createProject(dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Name is required.");

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-01-02b: Alt Flow — null Name → ValidationException")
        void createProject_withNullName_shouldThrowValidationException() {
            ProjectDto dto = new ProjectDto();
            dto.setName(null);

            assertThatThrownBy(() -> projectService.createProject(dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Name is required.");

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-01-03: Edge — whitespace-only Name → ValidationException")
        void createProject_withWhitespaceOnlyName_shouldThrowValidationException() {
            ProjectDto dto = new ProjectDto();
            dto.setName("   ");

            assertThatThrownBy(() -> projectService.createProject(dto, developerUser))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Name is required.");

            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("Boundary — Name > 100 χαρακτήρες → ValidationException")
        void createProject_withNameOver100Chars_shouldThrowValidationException() {
            ProjectDto dto = new ProjectDto();
            dto.setName("a".repeat(101));

            assertThatThrownBy(() -> projectService.createProject(dto, developerUser))
                    .isInstanceOf(ValidationException.class);

            verify(projectRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC02 — Διαγραφή Project
    // =========================================================================

    @Nested
    @DisplayName("UC02 — Διαγραφή Project")
    class DeleteProjectTests {

        private Project project;

        @BeforeEach
        void setUpProject() {
            project = new Project();
            project.setId(10L);
            project.setName("Existing Project");
            project.setOwner(developerUser);
        }

        @Test
        @DisplayName("TC-02-01: Happy Path — owner διαγράφει το Project")
        void deleteProject_byOwner_shouldDelete() {
            when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

            projectService.deleteProject(10L, developerUser);

            verify(projectRepository, times(1)).delete(project);
        }

        @Test
        @DisplayName("TC-02-01b: Happy Path — ORG_OWNER διαγράφει project άλλου χρήστη")
        void deleteProject_byOrgOwner_shouldDelete() {
            when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

            projectService.deleteProject(10L, orgOwnerUser);

            verify(projectRepository, times(1)).delete(project);
        }

        @Test
        @DisplayName("TC-02-03: Alt Flow — μη εξουσιοδοτημένος χρήστης → UnauthorizedException")
        void deleteProject_byUnauthorizedUser_shouldThrowUnauthorizedException() {
            when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.deleteProject(10L, otherUser))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You do not have permission to delete this project");

            verify(projectRepository, never()).delete(any(Project.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό Project ID → NotFoundException")
        void deleteProject_withNonExistentId_shouldThrowNotFoundException() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(999L, developerUser))
                    .isInstanceOf(NotFoundException.class);

            verify(projectRepository, never()).delete(any(Project.class));
        }
    }

    // =========================================================================
    // UC12 — Διαμοιρασμός Project
    // =========================================================================

    @Nested
    @DisplayName("UC12 — Διαμοιρασμός Project")
    class ShareProjectTests {

        private Project project;
        private User targetUser;

        @BeforeEach
        void setUpProject() {
            project = new Project();
            project.setId(200L);
            project.setName("Shared Project");
            project.setOwner(orgOwnerUser);

            targetUser = new User();
            targetUser.setId(99L);
            targetUser.setUsername("testuser1");
            targetUser.setRole(Role.DEVELOPER);
        }

        @Test
        @DisplayName("TC-12-01: Happy Path — προσθήκη Participant με valid username + role")
        void addParticipant_withValidUsername_shouldSaveAndReturnId() {
            when(projectRepository.findById(200L)).thenReturn(Optional.of(project));
            when(userRepository.findByUsername("testuser1")).thenReturn(Optional.of(targetUser));
            when(participantRepository.existsByProjectAndUser(project, targetUser)).thenReturn(false);
            when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> {
                Participant p = inv.getArgument(0);
                p.setId(555L);
                return p;
            });

            Long resultId = projectService.addParticipant(200L, "testuser1", Role.COLLABORATOR);

            assertThat(resultId).isEqualTo(555L);

            ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
            verify(participantRepository, times(1)).save(captor.capture());

            Participant saved = captor.getValue();
            assertThat(saved.getProject()).isEqualTo(project);
            assertThat(saved.getUser()).isEqualTo(targetUser);
            assertThat(saved.getRole()).isEqualTo(Role.COLLABORATOR);
        }

        @Test
        @DisplayName("TC-12-02: Alt Flow — άκυρο username → UserNotFoundException")
        void addParticipant_withNonExistentUsername_shouldThrowUserNotFoundException() {
            when(projectRepository.findById(200L)).thenReturn(Optional.of(project));
            when(userRepository.findByUsername("nonexistentuser99"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    projectService.addParticipant(200L, "nonexistentuser99", Role.REVIEWER))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User with this username not found");

            verify(participantRepository, never()).save(any(Participant.class));
        }

        @Test
        @DisplayName("TC-12-03: Alt Flow — ήδη participant → AlreadyParticipantException")
        void addParticipant_whenUserAlreadyParticipant_shouldThrowException() {
            when(projectRepository.findById(200L)).thenReturn(Optional.of(project));
            when(userRepository.findByUsername("testuser1")).thenReturn(Optional.of(targetUser));
            when(participantRepository.existsByProjectAndUser(project, targetUser))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    projectService.addParticipant(200L, "testuser1", Role.COLLABORATOR))
                    .isInstanceOf(AlreadyParticipantException.class)
                    .hasMessage("User is already a participant");

            verify(participantRepository, never()).save(any(Participant.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό Project ID → NotFoundException")
        void addParticipant_withNonExistentProject_shouldThrowNotFoundException() {
            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    projectService.addParticipant(999L, "anyone", Role.COLLABORATOR))
                    .isInstanceOf(NotFoundException.class);

            verify(participantRepository, never()).save(any(Participant.class));
        }

        @Test
        @DisplayName("TC-12-04: Happy Path — αφαίρεση Participant")
        void removeParticipant_withValidIds_shouldDelete() {
            Participant participant = new Participant(project, targetUser, Role.COLLABORATOR);
            participant.setId(555L);

            when(participantRepository.findById(555L)).thenReturn(Optional.of(participant));

            projectService.removeParticipant(200L, 555L);

            verify(participantRepository, times(1)).delete(participant);
        }

        @Test
        @DisplayName("TC-12-04b: Alt — participant ανήκει σε άλλο project → NotFoundException")
        void removeParticipant_whenBelongsToOtherProject_shouldThrowNotFoundException() {
            Project otherProject = new Project();
            otherProject.setId(300L);
            Participant participant = new Participant(otherProject, targetUser, Role.COLLABORATOR);
            participant.setId(555L);

            when(participantRepository.findById(555L)).thenReturn(Optional.of(participant));

            assertThatThrownBy(() -> projectService.removeParticipant(200L, 555L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("δεν ανήκει");

            verify(participantRepository, never()).delete(any(Participant.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό participant ID → NotFoundException")
        void removeParticipant_withNonExistentId_shouldThrowNotFoundException() {
            when(participantRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.removeParticipant(200L, 999L))
                    .isInstanceOf(NotFoundException.class);

            verify(participantRepository, never()).delete(any(Participant.class));
        }
    }
}
