package com.research.gbjournal.dto.navigation;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavSubItemDTO {
    private Long id;
    private String clientId;
    private String label;
    private String href;
    private String description;
    private String iconName;
    private int displayOrder;
    private boolean enabled;
}
