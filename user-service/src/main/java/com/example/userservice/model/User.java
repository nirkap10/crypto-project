package com.example.userservice.model;

import java.time.LocalDateTime;

public record User(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public User {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be null or blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be null or blank");
        }
    }

    // Constructor for creating a new user (without id and timestamps)
    public User(String username, String email, String firstName, String lastName) {
        this(null, username, email, firstName, lastName, null, null);
    }
}


