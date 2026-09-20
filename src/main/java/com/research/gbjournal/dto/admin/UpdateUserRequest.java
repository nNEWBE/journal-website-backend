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
    private String role;
    private String title;
    private String department;
    private String institution;
    private String orcid;
    private String avatarUrl;
    private String password;
    private Boolean enabled;
}
