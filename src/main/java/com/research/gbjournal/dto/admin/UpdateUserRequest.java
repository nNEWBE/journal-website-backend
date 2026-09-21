package com.research.gbjournal.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String secondaryEmail;
    private String role;
    private String title;
    private String department;
    private String institution;
    private String country;
    private String orcid;
    private String researchInterests;
    private String avatarUrl;
    private String password;
    private Boolean enabled;
    private Boolean emailVerified;
}

