package com.research.gbjournal.dto.navigation;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavItemDTO {
    private Long id;
    private String clientId;
    private String label;
    private String href;
    private String dropdownHeader;
    private String footerLabel;
    private String footerHref;
    private int displayOrder;
    private boolean openInNewTab;
    private boolean enabled;
    @Builder.Default
    private List<NavSubItemDTO> dropdown = new ArrayList<>();
}
