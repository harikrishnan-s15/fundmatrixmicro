package com.fundmatrix.service;

import com.fundmatrix.common.exception.ResourceNotFoundException;
import com.fundmatrix.domain.Notification;
import com.fundmatrix.domain.User;
import com.fundmatrix.domain.enums.NotificationCategory;
import com.fundmatrix.domain.enums.NotificationStatus;
import com.fundmatrix.dto.NotificationDto;
import com.fundmatrix.repository.NotificationRepository;
import com.fundmatrix.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;


@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUser;
    private final Mapper mapper;

    public NotificationService(NotificationRepository notificationRepository,
                               CurrentUserService currentUser, Mapper mapper) {
        this.notificationRepository = notificationRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }


    @Transactional
    public void notify(User user, NotificationCategory category, String message) {
        if (user == null) {
            return;
        }
        Notification n = Notification.builder()
                .user(user)
                .category(category)
                .message(message)
                .status(NotificationStatus.UNREAD)
                .createdDate(Instant.now())
                .build();
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> myNotifications() {
        return notificationRepository.findByUser_IdOrderByCreatedDateDesc(currentUser.getId())
                .stream().map(mapper::toNotificationDto).toList();
    }

    @Transactional(readOnly = true)
    public long myUnreadCount() {
        return notificationRepository.countByUser_IdAndStatus(currentUser.getId(), NotificationStatus.UNREAD);
    }

    @Transactional
    public NotificationDto updateStatus(Long id, NotificationStatus status) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", id));
        if (!n.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
        n.setStatus(status);
        return mapper.toNotificationDto(notificationRepository.save(n));
    }

    @Transactional
    public void markAllRead() {
        var list = notificationRepository.findByUser_IdAndStatusOrderByCreatedDateDesc(
                currentUser.getId(), NotificationStatus.UNREAD);
        list.forEach(n -> n.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(list);
    }
}
