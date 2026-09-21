package com.research.gbjournal.repository;

import com.research.gbjournal.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByOrderByCreatedAtDesc();

    List<Notification> findTop50ByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    void markAsRead(Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = false WHERE n.id = :id")
    void markAsUnread(Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true")
    void markAllAsRead();
}
