package com.tiencv.authservice.service;

import com.tiencv.authservice.model.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByEmail(String email);
}
