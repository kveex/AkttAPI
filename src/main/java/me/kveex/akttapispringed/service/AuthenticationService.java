package me.kveex.akttapispringed.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface AuthenticationService {
    UserDetails authenticate(String login, String password);
    String generateToken(UserDetails userDetails);
    UserDetails validateToken(String token);
}
