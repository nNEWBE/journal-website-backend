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
                .map(m -> toDTO(m))
                .toList();
    }

    @Transactional
    public BoardMemberDTO createMember(BoardMemberDTO dto) {
        BoardMember member = BoardMember.builder()
                .name(dto.getName())
                .role(dto.getRole())
                .unit(dto.getUnit())
                .institution(dto.getInstitution())
                .expertise(dto.getExpertise())
                .bio(dto.getBio())
                .imageUrl(dto.getImageUrl())
                .orcid(dto.getOrcid())
                .googleScholarUrl(dto.getGoogleScholarUrl())
                .sortOrder(dto.getSortOrder() > 0 ? dto.getSortOrder() : (int) boardMemberRepository.count() + 1)
                .build();
        boardMemberRepository.save(member);
        return toDTO(member);
    }

    @Transactional
    public BoardMemberDTO updateMember(Long id, BoardMemberDTO dto) {
        BoardMember member = boardMemberRepository.findById(id)
                .orElseThrow(() -> new com.research.gbjournal.exception.ResourceNotFoundException("BoardMember", "id", id));
        member.setName(dto.getName());
        member.setRole(dto.getRole());
        member.setUnit(dto.getUnit());
        member.setInstitution(dto.getInstitution());
        member.setExpertise(dto.getExpertise());
        member.setBio(dto.getBio());
        member.setImageUrl(dto.getImageUrl());
        member.setOrcid(dto.getOrcid());
        member.setGoogleScholarUrl(dto.getGoogleScholarUrl());
        if (dto.getSortOrder() > 0) member.setSortOrder(dto.getSortOrder());
        boardMemberRepository.save(member);
        return toDTO(member);
    }

    @Transactional
    public void deleteMember(Long id) {
        boardMemberRepository.deleteById(id);
    }

    public BoardMemberDTO toDTO(BoardMember m) {
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
