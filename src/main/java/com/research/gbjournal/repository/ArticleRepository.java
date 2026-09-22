package com.research.gbjournal.repository;

import com.research.gbjournal.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

        Optional<Article> findBySlug(String slug);

        Optional<Article> findByArticleId(String articleId);

        @Query("""
                        SELECT a FROM Article a
                        WHERE (:pattern IS NULL OR
                               LOWER(a.title) LIKE :pattern OR
                               LOWER(a.abstractText) LIKE :pattern)
                          AND (:type IS NULL OR a.type = :type)
                          AND (:topic IS NULL OR a.topic = :topic)
                          AND (:issueLabel IS NULL OR a.issueLabel = :issueLabel)
                        """)
        Page<Article> searchArticles(
                        @Param("pattern") String pattern,
                        @Param("type") String type,
                        @Param("topic") String topic,
                        @Param("issueLabel") String issueLabel,
                        Pageable pageable);

        @Modifying
        @Query("UPDATE Article a SET a.metrics.views = COALESCE(a.metrics.views, 0) + 1 WHERE a.id = :id")
        void incrementViews(@Param("id") Long id);

        @Modifying
        @Query("UPDATE Article a SET a.metrics.downloads = COALESCE(a.metrics.downloads, 0) + 1 WHERE a.id = :id")
        void incrementDownloads(@Param("id") Long id);

        @Modifying
        @Query("UPDATE Article a SET a.metrics.views = 0, a.metrics.downloads = 0, a.metrics.citations = 0 WHERE a.id = :id")
        void resetMetrics(@Param("id") Long id);

        @Modifying
        @Query("UPDATE Article a SET a.metrics.views = 0, a.metrics.downloads = 0, a.metrics.citations = 0")
        void resetAllMetrics();

        @Modifying
        @Query("UPDATE Article a SET a.metrics.views = :views, a.metrics.downloads = :downloads, a.metrics.citations = :citations WHERE a.id = :id")
        void updateMetrics(@Param("id") Long id, @Param("views") int views, @Param("downloads") int downloads, @Param("citations") int citations);

        @Query("SELECT DISTINCT a.type FROM Article a ORDER BY a.type")
        java.util.List<String> findAllArticleTypes();

        @Query("SELECT DISTINCT a.topic FROM Article a ORDER BY a.topic")
        java.util.List<String> findAllTopics();
}
