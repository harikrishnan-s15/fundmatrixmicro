package com.fundmatrix.controller;

import com.fundmatrix.domain.enums.NotificationStatus;
import com.fundmatrix.dto.MessageResponse;
import com.fundmatrix.dto.NotificationDto;
import com.fundmatrix.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the current user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationDto> mine() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notificationService.myUnreadCount());
    }

    @PatchMapping("/{id}/read")
    public NotificationDto markRead(@PathVariable Long id) {
        return notificationService.updateStatus(id, NotificationStatus.READ);
    }

    @PatchMapping("/{id}/dismiss")
    public NotificationDto dismiss(@PathVariable Long id) {
        return notificationService.updateStatus(id, NotificationStatus.DISMISSED);
    }

    @PostMapping("/read-all")
    public MessageResponse markAllRead() {
        notificationService.markAllRead();
        return new MessageResponse("All notifications marked as read");
    }
}
