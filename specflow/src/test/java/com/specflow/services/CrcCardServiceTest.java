package com.specflow.services;

import com.specflow.domain.CrcCard;
import com.specflow.domain.Project;
import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.CrcCardDto;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.UnauthorizedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.CrcCardRepository;
import com.specflow.repositories.ProjectRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το CrcCardService — καλύπτει τα Use Cases UC06 (Δημιουργία CRC Card),
 * UC07 (Επεξεργασία CRC Card) και UC08 (Διαγραφή CRC Card) σύμφωνα με το test plan
 * {@code test/test-plan-uc05-uc08.md}.
 */
@ExtendWith(MockitoExtension.class)
class CrcCardServiceTest {

    @Mock
    private CrcCardRepository crcCardRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private CrcCardService crcCardService;

    private User developerUser;
    private User otherUser;
    private User orgOwnerUser;
    private Project project;

    @BeforeEach
    void setUp() {
        developerUser = new User();
        developerUser.setId(1L);
        developerUser.setUsername("dev_user");
        developerUser.setRole(Role.DEVELOPER);

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("reviewer_user");
        otherUser.setRole(Role.REVIEWER);

        orgOwnerUser = new User();
        orgOwnerUser.setId(3L);
        orgOwnerUser.setUsername("owner_user");
        orgOwnerUser.setRole(Role.ORG_OWNER);

        project = new Project();
        project.setId(100L);
        project.setName("Existing Project");
        project.setOwner(developerUser);
    }

    // =========================================================================
    // UC06 — Δημιουργία CRC Card
    // =========================================================================

    @Nested
    @DisplayName("UC06 — Δημιουργία CRC Card")
    class CreateCrcCardTests {

        @Test
        @DisplayName("TC-06-01: Happy Path — δημιουργία CRC Card με μοναδικό Class Name")
        void createCrcCard_withUniqueClassName_shouldSaveAndReturnId() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("OrderService");
            dto.setResponsibilities("validateOrder\nprocessPayment");
            dto.setCollaborations("PaymentGateway\nInventory");

            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.existsByClassNameAndProject("OrderService", project))
                    .thenReturn(false);
            when(crcCardRepository.save(any(CrcCard.class))).thenAnswer(inv -> {
                CrcCard c = inv.getArgument(0);
                c.setId(700L);
                return c;
            });

            Long resultId = crcCardService.createCrcCard(100L, dto);

            assertThat(resultId).isEqualTo(700L);

            ArgumentCaptor<CrcCard> captor = ArgumentCaptor.forClass(CrcCard.class);
            verify(crcCardRepository, times(1)).save(captor.capture());

            CrcCard saved = captor.getValue();
            assertThat(saved.getClassName()).isEqualTo("OrderService");
            assertThat(saved.getResponsibilities()).isEqualTo("validateOrder\nprocessPayment");
            assertThat(saved.getCollaborations()).isEqualTo("PaymentGateway\nInventory");
            assertThat(saved.getProject()).isEqualTo(project);
        }

        @Test
        @DisplayName("TC-06-02: Alt Flow — κενό Class Name → ValidationException")
        void createCrcCard_withEmptyClassName_shouldThrowValidationException() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("");
            dto.setResponsibilities("anything");

            assertThatThrownBy(() -> crcCardService.createCrcCard(100L, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Class Name is required.");

            verify(crcCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-06-02b: Alt Flow — null Class Name → ValidationException")
        void createCrcCard_withNullClassName_shouldThrowValidationException() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName(null);

            assertThatThrownBy(() -> crcCardService.createCrcCard(100L, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Class Name is required.");

            verify(crcCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-06-02c: Edge — whitespace-only Class Name → ValidationException")
        void createCrcCard_withWhitespaceOnlyClassName_shouldThrowValidationException() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("   ");

            assertThatThrownBy(() -> crcCardService.createCrcCard(100L, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Class Name is required.");

            verify(crcCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-06-03: Alt Flow — διπλότυπο Class Name στο ίδιο Project → ValidationException")
        void createCrcCard_withDuplicateClassName_shouldThrowValidationException() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("UserService");

            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(crcCardRepository.existsByClassNameAndProject("UserService", project))
                    .thenReturn(true);

            assertThatThrownBy(() -> crcCardService.createCrcCard(100L, dto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("UserService")
                    .hasMessageContaining("already exists");

            verify(crcCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό Project → NotFoundException")
        void createCrcCard_withNonExistentProject_shouldThrowNotFoundException() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("OrderService");

            when(projectRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crcCardService.createCrcCard(999L, dto))
                    .isInstanceOf(NotFoundException.class);

            verify(crcCardRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC07 — Επεξεργασία CRC Card
    // =========================================================================

    @Nested
    @DisplayName("UC07 — Επεξεργασία CRC Card")
    class UpdateCrcCardTests {

        private CrcCard existingCard;

        @BeforeEach
        void setUpCrcCard() {
            CrcCardDto initial = new CrcCardDto();
            initial.setClassName("OrderService");
            initial.setResponsibilities("original responsibility");
            initial.setCollaborations("original collaboration");

            existingCard = new CrcCard(initial, project);
            existingCard.setId(800L);
        }

        @Test
        @DisplayName("TC-07-01: Happy Path — μετονομασία σε μοναδικό νέο Class Name")
        void updateCrcCard_withNewUniqueName_shouldSaveAndReturnId() {
            CrcCardDto updateDto = new CrcCardDto();
            updateDto.setClassName("NewOrderService");
            updateDto.setResponsibilities("updated responsibility");
            updateDto.setCollaborations("updated collaboration");

            when(crcCardRepository.findById(800L)).thenReturn(Optional.of(existingCard));
            when(crcCardRepository.existsByClassNameAndProjectAndIdNot(
                    "NewOrderService", project, 800L)).thenReturn(false);
            when(crcCardRepository.save(any(CrcCard.class))).thenAnswer(inv -> inv.getArgument(0));

            Long resultId = crcCardService.updateCrcCard(800L, updateDto);

            assertThat(resultId).isEqualTo(800L);
            assertThat(existingCard.getClassName()).isEqualTo("NewOrderService");
            assertThat(existingCard.getResponsibilities()).isEqualTo("updated responsibility");
            assertThat(existingCard.getCollaborations()).isEqualTo("updated collaboration");
            verify(crcCardRepository).save(existingCard);
        }

        @Test
        @DisplayName("TC-07-02: Alt Flow — μετονομασία σε όνομα που ανήκει σε άλλη κάρτα → ValidationException")
        void updateCrcCard_withDuplicateOtherCardName_shouldThrowValidationException() {
            CrcCardDto updateDto = new CrcCardDto();
            updateDto.setClassName("CardB");
            updateDto.setResponsibilities("anything");

            when(crcCardRepository.findById(800L)).thenReturn(Optional.of(existingCard));
            when(crcCardRepository.existsByClassNameAndProjectAndIdNot(
                    "CardB", project, 800L)).thenReturn(true);

            assertThatThrownBy(() -> crcCardService.updateCrcCard(800L, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("CardB")
                    .hasMessageContaining("already exists");

            verify(crcCardRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-07-03: Edge — αποθήκευση χωρίς αλλαγή Class Name (self-exclusion via early-exit)")
        void updateCrcCard_withSameClassName_shouldSkipUniquenessCheck() {
            CrcCardDto updateDto = new CrcCardDto();
            updateDto.setClassName("OrderService"); // ίδιο με το existingCard
            updateDto.setResponsibilities("only responsibilities changed");

            when(crcCardRepository.findById(800L)).thenReturn(Optional.of(existingCard));
            when(crcCardRepository.save(any(CrcCard.class))).thenAnswer(inv -> inv.getArgument(0));

            Long resultId = crcCardService.updateCrcCard(800L, updateDto);

            assertThat(resultId).isEqualTo(800L);
            assertThat(existingCard.getResponsibilities()).isEqualTo("only responsibilities changed");

            // Κρίσιμη επαλήθευση: το uniqueness check ΔΕΝ καλείται όταν το Class Name δεν άλλαξε
            verify(crcCardRepository, never())
                    .existsByClassNameAndProjectAndIdNot(anyString(), any(Project.class), anyLong());
            verify(crcCardRepository).save(existingCard);
        }

        @Test
        @DisplayName("TC-07-03b: Alt Flow — κενό Class Name κατά την επεξεργασία → ValidationException")
        void updateCrcCard_withEmptyClassName_shouldThrowValidationException() {
            CrcCardDto updateDto = new CrcCardDto();
            updateDto.setClassName("");

            assertThatThrownBy(() -> crcCardService.updateCrcCard(800L, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("Class Name is required.");

            verify(crcCardRepository, never()).save(any());
        }
    }

    // =========================================================================
    // UC08 — Διαγραφή CRC Card
    // =========================================================================

    @Nested
    @DisplayName("UC08 — Διαγραφή CRC Card")
    class DeleteCrcCardTests {

        private CrcCard existingCard;

        @BeforeEach
        void setUpCrcCard() {
            CrcCardDto dto = new CrcCardDto();
            dto.setClassName("ToBeDeleted");

            existingCard = new CrcCard(dto, project);
            existingCard.setId(900L);
        }

        @Test
        @DisplayName("TC-08-01: Happy Path — owner διαγράφει CRC Card + cascade στις M:N")
        void deleteCrcCard_byOwner_shouldDelete() {
            when(crcCardRepository.findById(900L)).thenReturn(Optional.of(existingCard));

            crcCardService.deleteCrcCard(900L, developerUser);

            verify(crcCardRepository, times(1)).delete(existingCard);
        }

        @Test
        @DisplayName("TC-08-01b: Happy Path — ORG_OWNER διαγράφει CRC Card άλλου χρήστη")
        void deleteCrcCard_byOrgOwner_shouldDelete() {
            when(crcCardRepository.findById(900L)).thenReturn(Optional.of(existingCard));

            crcCardService.deleteCrcCard(900L, orgOwnerUser);

            verify(crcCardRepository, times(1)).delete(existingCard);
        }

        @Test
        @DisplayName("TC-08-03: Alt Flow — μη εξουσιοδοτημένος χρήστης (Reviewer) → UnauthorizedException")
        void deleteCrcCard_byUnauthorizedUser_shouldThrowUnauthorizedException() {
            when(crcCardRepository.findById(900L)).thenReturn(Optional.of(existingCard));

            assertThatThrownBy(() -> crcCardService.deleteCrcCard(900L, otherUser))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("You do not have permission to delete this CRC Card");

            verify(crcCardRepository, never()).delete(any(CrcCard.class));
        }

        @Test
        @DisplayName("Edge — μη υπαρκτό CRC Card ID → NotFoundException")
        void deleteCrcCard_withNonExistentId_shouldThrowNotFoundException() {
            when(crcCardRepository.findById(eq(999L))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> crcCardService.deleteCrcCard(999L, developerUser))
                    .isInstanceOf(NotFoundException.class);

            verify(crcCardRepository, never()).delete(any(CrcCard.class));
        }
    }
}
