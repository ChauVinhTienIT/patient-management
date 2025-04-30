package com.tiencv.authservice.controller;

import com.tiencv.authservice.dto.LoginRequestDTO;
import com.tiencv.authservice.dto.LoginResponseDTO;
import com.tiencv.authservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Generate access token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Validated LoginRequestDTO loginRequestDTO
    ) {
        Optional<String> accessTokenOptional = authService.authenticate(loginRequestDTO);
        if (accessTokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String accessToken = accessTokenOptional.get();
        return ResponseEntity.ok(new LoginResponseDTO(accessToken));
    }

    @Operation(summary = "Validate access token")
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Check if the Authorization header is present and starts with "Bearer "
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract the token from the Authorization header
        // Remove "Bearer " prefix
        // Validate the token using the authService
        String accessToken = authorizationHeader.substring("Bearer ".length());
        return authService.validateToken(accessToken)
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
