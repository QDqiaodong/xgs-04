package com.bookexchange.repository;

import com.bookexchange.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreateTimeDesc(Long userId);

    List<Notification> findByUserIdAndTypeOrderByCreateTimeDesc(Long userId, String type);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    int markAsReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreateTimeDesc(Long userId);

    boolean existsByUserIdAndTypeAndRelatedIdAndIsReadFalse(Long userId, String type, Long relatedId);
}
