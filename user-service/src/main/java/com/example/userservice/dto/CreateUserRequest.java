package com.example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    String username,
    
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    String email,
    
    @JsonProperty("first_name")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String firstName,
    
    @JsonProperty("last_name")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String lastName
) {}

