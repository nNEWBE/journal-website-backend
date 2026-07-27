package com.research.gbjournal.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 150, message = "Full name must be 2-150 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    private String password;

    @Size(max = 200)
    private String institution;

    @Size(max = 150)
    private String department;

    @Size(max = 10)
    private String country;

    @Size(max = 30)
    @Pattern(regexp = "^(\\d{4}-\\d{4}-\\d{4}-\\d{3}[0-9X])?$",
             message = "Invalid ORCID format (expected: 0000-0000-0000-000X)")
    private String orcid;

    @Size(max = 500)
    private String researchInterests;
}
