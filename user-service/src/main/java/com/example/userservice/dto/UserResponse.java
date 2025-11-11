package com.example.userservice.dto;

import com.example.userservice.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String username,
    String email,
    @JsonProperty("first_name") String firstName,
    @JsonProperty("last_name") String lastName,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
            user.id(),
            user.username(),
            user.email(),
            user.firstName(),
            user.lastName(),
            user.createdAt(),
            user.updatedAt()
        );
    }
}


