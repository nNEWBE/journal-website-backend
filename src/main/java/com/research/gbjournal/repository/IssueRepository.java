package com.research.gbjournal.repository;

import com.research.gbjournal.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findByIssueKey(String issueKey);

    @Query("SELECT DISTINCT i FROM Issue i LEFT JOIN FETCH i.articles a WHERE i.current = true")
    Optional<Issue> findByCurrentTrue();

    @Query("SELECT DISTINCT i FROM Issue i LEFT JOIN FETCH i.articles a ORDER BY i.year DESC, i.issueLabel DESC")
    List<Issue> findAllByOrderByYearDescIssueLabelDesc();

    @Modifying
    @Query("UPDATE Issue i SET i.current = false WHERE i.current = true")
    int clearCurrentIssue();
}
