package com.research.gbjournal.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetTokenValidationResponse {

    private boolean valid;
    private String email;
    private String message;
}
