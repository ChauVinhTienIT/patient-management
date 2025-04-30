package com.tiencv.authservice.service;

import com.tiencv.authservice.dto.LoginRequestDTO;

import java.util.Optional;

public interface AuthService {
    /**
     * Authenticate user and generate access token.
     *
     * @param loginRequestDTO the login request DTO
     * @return the access token if authentication is successful, otherwise null
     */
    Optional<String> authenticate(LoginRequestDTO loginRequestDTO);

    boolean validateToken(String accessToken);
}
