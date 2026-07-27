package com.research.gbjournal.dto.editorial;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EditorialDecisionRequest {

    /** ACCEPT, REJECT, REVISION_REQUESTED */
    @NotBlank(message = "Decision is required")
    private String decision;

    private String note;
}
