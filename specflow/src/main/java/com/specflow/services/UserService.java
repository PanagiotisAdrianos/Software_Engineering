package com.specflow.services;

import com.specflow.domain.Role;
import com.specflow.domain.User;
import com.specflow.dto.RegisterDto;
import com.specflow.exceptions.NotFoundException;
import com.specflow.exceptions.RoleUnchangedException;
import com.specflow.exceptions.ValidationException;
import com.specflow.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User register(RegisterDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ValidationException("Το username χρησιμοποιείται ήδη.");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ValidationException("Το email χρησιμοποιείται ήδη.");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.DEVELOPER);
        return userRepository.save(user);
    }

    // ===== UC13: User Management =====

    @Transactional(readOnly = true)
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ο χρήστης δεν βρέθηκε."));
    }

    @Transactional
    public void changeUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Ο χρήστης δεν βρέθηκε."));
        checkRoleChanged(user, newRole);
        user.setRole(newRole);
        userRepository.save(user);
    }

    private void checkRoleChanged(User user, Role newRole) {
        if (user.getRole() == newRole) {
            throw new RoleUnchangedException("User already has this role");
        }
    }
}
