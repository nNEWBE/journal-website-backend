package com.research.gbjournal.dto.board;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BoardMemberDTO {

    private Long id;
    private String name;
    private String role;
    private String unit;
    private String institution;
    private String expertise;
    private String bio;
    private String imageUrl;
    private String orcid;
    private String googleScholarUrl;
    private int sortOrder;
}
