package com.fundmatrix.repository;

import com.fundmatrix.domain.Notification;
import com.fundmatrix.domain.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedDateDesc(Long userId);

    List<Notification> findByUser_IdAndStatusOrderByCreatedDateDesc(Long userId, NotificationStatus status);

    long countByUser_IdAndStatus(Long userId, NotificationStatus status);
}
