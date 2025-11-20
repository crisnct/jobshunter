package com.jobshunter.controller;

import com.jobshunter.dto.AuthResponse;
import com.jobshunter.dto.LoginRequest;
import com.jobshunter.dto.RegisterRequest;
import com.jobshunter.dto.RegistrationResponse;
import com.jobshunter.service.authentication.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request);
        return new RegistrationResponse(
            "User registered. Please verify your email using the token sent via email (check logs in dev).",
            token);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return new AuthResponse(token);
    }

    @GetMapping("/verify")
    public Map<String, String> verify(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return Map.of("message", "Email verified. You can log in now.");
    }
}
