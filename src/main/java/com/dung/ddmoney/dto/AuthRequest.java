package com.dung.ddmoney.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Email format is invalid or contains accents")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(regexp = "^[\\x20-\\x7E]+$", message = "Password must not contain accents")
    private String password;
}
