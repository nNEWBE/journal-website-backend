package com.research.gbjournal.repository;

import com.research.gbjournal.entity.SubmissionFile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionFileRepository extends JpaRepository<SubmissionFile, Long> {

    @EntityGraph(attributePaths = {"submission"})
    Optional<SubmissionFile> findByStoredFilename(String storedFilename);
}
