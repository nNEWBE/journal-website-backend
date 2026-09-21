package com.research.gbjournal.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailRequest {

    @NotBlank(message = "New email address is required")
    @Email(message = "Please provide a valid institutional email address")
    private String newEmail;

    @NotBlank(message = "Current account password is required to verify identity")
    private String password;
}
