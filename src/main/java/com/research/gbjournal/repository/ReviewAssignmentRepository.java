package com.research.gbjournal.repository;

import com.research.gbjournal.entity.ReviewAssignment;
import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {

    List<ReviewAssignment> findByReviewerOrderByInvitedAtDesc(User reviewer);

    List<ReviewAssignment> findBySubmissionOrderByInvitedAtDesc(Submission submission);

    @Query("SELECT COUNT(r) FROM ReviewAssignment r WHERE r.reviewer = :reviewer AND r.status = 'COMPLETED'")
    long countCompletedByReviewer(@Param("reviewer") User reviewer);

    @Query("SELECT COUNT(r) FROM ReviewAssignment r WHERE r.reviewer = :reviewer AND r.status = 'ACCEPTED'")
    long countActiveByReviewer(@Param("reviewer") User reviewer);

    boolean existsBySubmissionAndReviewerAndStatusNot(
            Submission submission,
            User reviewer,
            ReviewAssignment.ReviewStatus status);

    @Query("SELECT COUNT(r) FROM ReviewAssignment r WHERE r.status IN ('INVITED', 'ACCEPTED')")
    long countActiveReviewers();
}
