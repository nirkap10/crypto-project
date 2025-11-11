package com.example.userservice.service;

import com.example.userservice.dto.CreateUserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.model.User;
import com.example.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Check if username already exists
        userRepository.findByUsername(request.username())
            .ifPresent(user -> {
                throw new IllegalArgumentException("Username already exists: " + request.username());
            });

        // Check if email already exists
        userRepository.findByEmail(request.email())
            .ifPresent(user -> {
                throw new IllegalArgumentException("Email already exists: " + request.email());
            });

        // Create and save user
        User user = new User(
            request.username(),
            request.email(),
            request.firstName(),
            request.lastName()
        );

        User savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public UserResponse getUserById(Long id) {
        return userRepository.findById(id)
            .map(UserResponse::from)
            .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .map(UserResponse::from)
            .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
            .map(UserResponse::from)
            .collect(Collectors.toList());
    }
}

