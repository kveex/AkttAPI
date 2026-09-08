package me.kveex.akttapispringed.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.kveex.akttapispringed.domain.entity.user.User;
import me.kveex.akttapispringed.domain.entity.user.UserRole;
import me.kveex.akttapispringed.repository.UserRepository;
import me.kveex.akttapispringed.service.RegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void registerUser(String login, String password) {
        if (userRepository.existsByLogin(login)) {
            throw new IllegalArgumentException("Этот логин уже занят!");
        }

        User user = User.builder()
                .login(login)
                .password(passwordEncoder.encode(password))
                .role(UserRole.USER)
                .build();

        userRepository.save(user);
    }
}
