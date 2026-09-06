package me.kveex.akttapispringed.controller;

import lombok.RequiredArgsConstructor;
import me.kveex.akttapispringed.domain.dto.AuthResponse;
import me.kveex.akttapispringed.domain.dto.LoginRequest;
import me.kveex.akttapispringed.service.AuthenticationService;
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

    @PostMapping
    public ResponseEntity<AuthResponse> authenticate(@RequestBody LoginRequest loginRequest) {
        UserDetails userDetails = authenticationService.authenticate(loginRequest.getLogin(), loginRequest.getPassword());

        String token = authenticationService.generateToken(userDetails);

        AuthResponse authResponse = AuthResponse.builder()
                .token(token)
                .expiresIn(86400)
                .build();

        return ResponseEntity.ok(authResponse);
    }
}
