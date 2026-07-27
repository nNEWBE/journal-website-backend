package com.research.gbjournal.repository;

import com.research.gbjournal.entity.BoardMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BoardMemberRepository extends JpaRepository<BoardMember, Long> {

    List<BoardMember> findAllByOrderBySortOrderAsc();
}
