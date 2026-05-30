package ru.studymarket.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.studymarket.domain.Role;
import ru.studymarket.domain.UserAccount;
import ru.studymarket.exception.DuplicateUserException;
import ru.studymarket.exception.ResourceNotFoundException;
import ru.studymarket.form.ProfileForm;
import ru.studymarket.form.RegistrationForm;
import ru.studymarket.repository.RoleRepository;
import ru.studymarket.repository.UserRepository;

@Service
@Transactional
public class UserService implements UserDetailsService {

    public static final String USER_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TelegramNotificationService telegramNotificationService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       TelegramNotificationService telegramNotificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.telegramNotificationService = telegramNotificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

        return org.springframework.security.core.userdetails.User
                .withUsername(account.getUsername())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .authorities(account.getRoles().stream()
                        .map(Role::getName)
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }

    public UserAccount register(RegistrationForm form) {
        if (userRepository.existsByUsernameIgnoreCase(form.getUsername())) {
            throw new DuplicateUserException("Такой логин уже занят");
        }
        if (userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            throw new DuplicateUserException("Этот email уже используется");
        }

        Role role = roleRepository.findByName(USER_ROLE)
                .orElseGet(() -> roleRepository.save(new Role(USER_ROLE)));
        UserAccount account = new UserAccount(
                form.getUsername().trim(),
                form.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(form.getPassword()),
                form.getFullName().trim(),
                normalizeBlank(form.getTelegramChatId())
        );
        account.getRoles().add(role);
        UserAccount saved = userRepository.save(account);
        telegramNotificationService.notifyUserAboutRegistration(saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public UserAccount requiredByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    public UserAccount updateProfile(String username, ProfileForm form) {
        UserAccount account = requiredByUsername(username);
        userRepository.findByEmailIgnoreCase(form.getEmail().trim().toLowerCase())
                .filter(found -> !found.getId().equals(account.getId()))
                .ifPresent(found -> {
                    throw new DuplicateUserException("Этот email уже используется");
                });
        account.setFullName(form.getFullName().trim());
        account.setEmail(form.getEmail().trim().toLowerCase());
        account.setTelegramChatId(normalizeBlank(form.getTelegramChatId()));
        account.setAvatarUrl(normalizeBlank(form.getAvatarUrl()));
        return userRepository.save(account);
    }

    public ProfileForm toProfileForm(UserAccount account) {
        ProfileForm form = new ProfileForm();
        form.setFullName(account.getFullName());
        form.setEmail(account.getEmail());
        form.setTelegramChatId(account.getTelegramChatId());
        form.setAvatarUrl(account.getAvatarUrl());
        return form;
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
