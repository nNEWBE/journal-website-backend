package com.research.gbjournal.dto.auth;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 150)
    private String fullName;

    @Size(max = 200)
    private String institution;

    @Size(max = 150)
    private String department;

    @Size(max = 10)
    private String country;

    @Size(max = 30)
    private String orcid;

    @Size(max = 500)
    private String researchInterests;

    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String avatarUrl;
}
