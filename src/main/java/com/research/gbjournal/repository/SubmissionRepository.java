package com.research.gbjournal.repository;

import com.research.gbjournal.entity.Submission;
import com.research.gbjournal.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findBySubmissionId(String submissionId);

    List<Submission> findBySubmittingAuthorOrderByUpdatedAtDesc(User author);

    Page<Submission> findByAssignedEditorOrderByUpdatedAtDesc(User editor, Pageable pageable);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.status NOT IN " +
           "(com.research.gbjournal.entity.Submission.SubmissionStatus.DRAFT, " +
           "com.research.gbjournal.entity.Submission.SubmissionStatus.WITHDRAWN, " +
           "com.research.gbjournal.entity.Submission.SubmissionStatus.REJECTED)")
    long countActiveSubmissions();

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.status IN " +
           "(com.research.gbjournal.entity.Submission.SubmissionStatus.UNDER_REVIEW, " +
           "com.research.gbjournal.entity.Submission.SubmissionStatus.REVIEWER_INVITATION)")
    long countUnderReview();

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.status = " +
           "com.research.gbjournal.entity.Submission.SubmissionStatus.ACCEPTED")
    long countAccepted();

    @Query("SELECT s FROM Submission s WHERE s.status <> " +
           "com.research.gbjournal.entity.Submission.SubmissionStatus.DRAFT " +
           "ORDER BY s.updatedAt DESC")
    Page<Submission> findAllActiveByOrderByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE " +
           "(:status IS NULL OR s.status = :status) " +
           "AND (:type IS NULL OR s.type = :type) " +
           "ORDER BY s.updatedAt DESC")
    Page<Submission> findByStatusAndType(
            @Param("status") Submission.SubmissionStatus status,
            @Param("type") String type,
            Pageable pageable);
}
