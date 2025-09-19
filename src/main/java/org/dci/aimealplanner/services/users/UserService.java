package org.dci.aimealplanner.services.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.exceptions.EmailAlreadyTakenException;
import org.dci.aimealplanner.exceptions.PasswordInvalidException;
import org.dci.aimealplanner.exceptions.UserNotFoundException;
import org.dci.aimealplanner.exceptions.VerificationTokenInvalidException;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.UserType;
import org.dci.aimealplanner.repositories.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{6,}$";

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String rawEmail) throws UsernameNotFoundException {
        final String email = normalize(rawEmail);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email + " not found."));

        if (user.getRole() == null) {
            throw new SecurityException("User " + user.getId() + " has no role assigned");
        }

        boolean enabled = user.isEmailVerified() && !user.isDeleted();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "{noop}")
                .roles(user.getRole().name())
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .disabled(!enabled)
                .build();
    }

    public void checkEmailAvailability(String email) {
        if(userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyTakenException(String.format("Email: %s is already taken.", email));
        }
    }

    public void checkPasswordValidity(String password) {
        if (!ifPasswordMatchesPattern(password)) {
            throw new PasswordInvalidException("Password must be at least 6 characters and contain uppercase," +
                    " lowercase, number and special character");
        }
    }

    public boolean ifPasswordMatchesPattern(String password) {
        return password.matches(PASSWORD_PATTERN);
    }

    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);
        user.setUserType(UserType.LOCAL);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    public User update(User user) {
        return userRepository.save(user);
    }

    public User findByVerificationToken(String token) {
        return userRepository.findByVerificationToken(token)
                .orElseThrow(() ->
                        new VerificationTokenInvalidException("User with verification token %s not found.".
                                formatted(token))
                );
    }

    public void sendVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(),token);
    }

    public void verifyToken(String token) {
        User user = findByVerificationToken(token);
        user.setEmailVerified(true);
        user.setVerificationToken(null);

        update(user);
    }

    public User findByEmail(String email) {
       return userRepository.findByEmail(email).orElseThrow(() -> new EmailAlreadyTakenException(email));
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id.toString() + " not found."));
    }

    public boolean emailIsNotTaken(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }

    public boolean isAdminExist() {
        return userRepository.findByRole(Role.ADMIN).isPresent();
    }

    @Transactional
    public void changeEmail(Long userId, String currentPassword, String newEmail) {
        User user = findById(userId);

        if (user.getUserType() != UserType.LOCAL) {
            throw new IllegalStateException("Email changes are managed by Google for this account.");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        String normalized = normalize(newEmail);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("New email is required.");
        }
        if (normalized.equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from the current email.");
        }
        if (!emailIsNotTaken(normalized)) {
            throw new EmailAlreadyTakenException("Email: %s is already taken.".formatted(normalized));
        }

        user.setEmail(normalized);
        user.setEmailVerified(false);
        sendVerificationToken(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findById(userId);

        if (user.getUserType() != UserType.LOCAL) {
            throw new IllegalStateException("Password changes are managed by Google for this account.");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        checkPasswordValidity(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        update(user);
    }

    @Transactional
    public void setLocalPassword(Long userId, String newPassword) {
        User user = findById(userId);

        if (user.getUserType() != UserType.GOOGLE) {
            throw new IllegalStateException("This action is only for Google-linked accounts.");
        }

        checkPasswordValidity(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        update(user);
    }

    @Transactional
    public void unlinkGoogle(Long userId) {
        User user = findById(userId);

        if (user.getUserType() != UserType.GOOGLE) {
            throw new IllegalStateException("This account is not linked to Google.");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalStateException("Set a local password before disconnecting Google.");
        }

        user.setUserType(UserType.LOCAL);
        update(user);
    }

    @Transactional
    public void switchUserType(Long userId, UserType newType) {
        User user = findById(userId);
        user.setUserType(newType);
        update(user);
    }

    public long countByDeletedFalse() {
        return userRepository.countByIsDeletedIsFalse();
    }

    public long countByEmailVerifiedTrueAndDeletedFalse() {
        return userRepository.countByEmailVerifiedTrueAndIsDeletedFalse();
    }

    public long countByIsDeletedIsFalse() {
        return userRepository.countByIsDeletedIsFalse();
    }

    public long countByEmailVerifiedTrueAndIsDeletedFalse() {
        return userRepository.countByEmailVerifiedTrueAndIsDeletedFalse();
    }

    public long countByEmailVerifiedFalseAndIsDeletedFalse() {
        return userRepository.countByEmailVerifiedFalseAndIsDeletedFalse();
    }

    public long countByIsDeletedTrue() {
        return userRepository.countByIsDeletedTrue();
    }

    public Page<User> searchDeleted(String like, Pageable pageable) {
        return userRepository.searchDeleted(like, pageable);
    }

    public Page<User> searchDeletedByType(String like, UserType type, Pageable pageable) {
        return userRepository.searchDeletedByType(like, type, pageable);
    }

    public Page<User> searchActiveVerified(String like, Pageable pageable) {
        return userRepository.searchActiveVerified(like, pageable);
    }

    public Page<User> searchActiveVerifiedByType(String like, UserType type, Pageable pageable) {
        return userRepository.searchActiveVerifiedByType(like, type, pageable);
    }

    public Page<User> searchActiveUnverified(String like, Pageable pageable) {
        return userRepository.searchActiveUnverified(like, pageable);
    }

    public Page<User> searchActiveUnverifiedByType(String like, UserType type, Pageable pageable) {
        return userRepository.searchActiveUnverifiedByType(like, type, pageable);
    }

    public Page<User> searchActive(String like, Pageable pageable) {
        return userRepository.searchActive(like, pageable);
    }

    public Page<User> searchActiveByType(String like, UserType type, Pageable pageable) {
        return userRepository.searchActiveByType(like, type, pageable);
    }
}
