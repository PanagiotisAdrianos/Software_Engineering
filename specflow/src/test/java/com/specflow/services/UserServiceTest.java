package com.specflow.services;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.RoleUnchangedException;
import com.specflow.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests για το UserService — καλύπτει UC13 (Διαχείριση Χρηστών) σύμφωνα
 * με το test plan {@code test/test-plan-uc13-uc16.md}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User targetUser;

    @BeforeEach
    void setUp() {
        targetUser = new User();
        targetUser.setId(99L);
        targetUser.setUsername("targetuser");
        targetUser.setEmail("target@example.com");
        targetUser.setRole(Role.DEVELOPER);
    }

    // =========================================================================
    // UC13 — Διαχείριση Χρηστών
    // =========================================================================

    @Nested
    @DisplayName("UC13 — Διαχείριση Χρηστών (changeUserRole)")
    class ChangeUserRoleTests {

        @Test
        @DisplayName("TC-13-01: Happy Path — αλλαγή ρόλου από DEVELOPER σε COLLABORATOR")
        void changeUserRole_withDifferentRole_shouldUpdateAndSave() {
            when(userRepository.findById(99L)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.changeUserRole(99L, Role.COLLABORATOR);

            assertThat(targetUser.getRole()).isEqualTo(Role.COLLABORATOR);
            verify(userRepository, times(1)).save(targetUser);
        }

        @Test
        @DisplayName("TC-13-01b: Happy Path — αλλαγή σε ORG_OWNER")
        void changeUserRole_toOrgOwner_shouldUpdateAndSave() {
            when(userRepository.findById(99L)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            userService.changeUserRole(99L, Role.ORG_OWNER);

            assertThat(targetUser.getRole()).isEqualTo(Role.ORG_OWNER);
            verify(userRepository).save(targetUser);
        }

        @Test
        @DisplayName("TC-13-02: Alt Flow — ίδιος ρόλος → RoleUnchangedException")
        void changeUserRole_withSameRole_shouldThrowRoleUnchangedException() {
            when(userRepository.findById(99L)).thenReturn(Optional.of(targetUser));

            assertThatThrownBy(() -> userService.changeUserRole(99L, Role.DEVELOPER))
                    .isInstanceOf(RoleUnchangedException.class)
                    .hasMessage("User already has this role");

            verify(userRepository, never()).save(any(User.class));
            assertThat(targetUser.getRole()).isEqualTo(Role.DEVELOPER);
        }

        @Test
        @DisplayName("Edge — μη υπαρκτός χρήστης → NotFoundException")
        void changeUserRole_withNonExistentUser_shouldThrowNotFoundException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeUserRole(999L, Role.ADMIN))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
