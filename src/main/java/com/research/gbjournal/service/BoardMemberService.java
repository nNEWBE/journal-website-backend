package com.research.gbjournal.service;

import com.research.gbjournal.dto.board.BoardMemberDTO;
import com.research.gbjournal.entity.BoardMember;
import com.research.gbjournal.repository.BoardMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardMemberService {

    private final BoardMemberRepository boardMemberRepository;

    @Transactional(readOnly = true)
    public List<BoardMemberDTO> getAllMembers() {
        return boardMemberRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private BoardMemberDTO toDTO(BoardMember m) {
        return BoardMemberDTO.builder()
                .id(m.getId())
                .name(m.getName())
                .role(m.getRole())
                .unit(m.getUnit())
                .institution(m.getInstitution())
                .expertise(m.getExpertise())
                .bio(m.getBio())
                .imageUrl(m.getImageUrl())
                .orcid(m.getOrcid())
                .googleScholarUrl(m.getGoogleScholarUrl())
                .sortOrder(m.getSortOrder())
                .build();
    }
}
