package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Notification;
import com.wbf.mutuelle.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "*", maxAge = 3000)
@RestController
@RequestMapping("/mut/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @GetMapping("/{id}")
    public Notification getNotificationById(@PathVariable Long id) {
        return notificationService.getNotificationById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found with id " + id));
    }

    @PostMapping
    public Notification createNotification(@RequestBody Notification notification) {
        return notificationService.createNotification(notification);
    }

    @PutMapping("/{id}")
    public Notification updateNotification(@PathVariable Long id, @RequestBody Notification notification) {
        return notificationService.updateNotification(id, notification);
    }

    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
    }
}
