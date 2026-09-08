package me.kveex.akttapispringed.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.kveex.akttapispringed.domain.dto.AuthResponse;
import me.kveex.akttapispringed.domain.dto.LoginRequest;
import me.kveex.akttapispringed.domain.dto.RegistrationRequest;
import me.kveex.akttapispringed.service.AuthenticationService;
import me.kveex.akttapispringed.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody LoginRequest request) {
        UserDetails userDetails = authenticationService.authenticate(request.getLogin(), request.getPassword());

        String token = authenticationService.generateToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .expiresIn(86400)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegistrationRequest request) {
        registrationService.registerUser(request.getLogin(), request.getPassword());

        return ResponseEntity.ok("Пользователь успешно зарегистрирован");
    }
}
