package com.research.gbjournal.repository;

import com.research.gbjournal.entity.PageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageContentRepository extends JpaRepository<PageContent, Long> {

    List<PageContent> findByPageKeyOrderByDisplayOrderAsc(String pageKey);

    List<PageContent> findByPageKeyAndPublishedTrueOrderByDisplayOrderAsc(String pageKey);

    Optional<PageContent> findByPageKeyAndSectionKey(String pageKey, String sectionKey);

    List<PageContent> findAllByOrderByPageKeyAscDisplayOrderAsc();

    void deleteByPageKey(String pageKey);
}
