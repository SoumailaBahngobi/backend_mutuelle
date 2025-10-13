package com.wbf.mutuelle.controllers;

import com.wbf.mutuelle.entities.Notification;
import com.wbf.mutuelle.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/mut/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // CRUD de base
    @GetMapping
    public ResponseEntity<List<Notification>> getAllNotifications() {
        List<Notification> notifications = notificationService.getAllNotifications();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable Long id) {
        Notification notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    @PostMapping
    public ResponseEntity<Notification> createNotification(@Valid @RequestBody Notification notification) {
        Notification createdNotification = notificationService.createNotification(notification);
        return ResponseEntity.ok(createdNotification);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notification> updateNotification(@PathVariable Long id, @Valid @RequestBody Notification notification) {
        Notification updatedNotification = notificationService.updateNotification(id, notification);
        return ResponseEntity.ok(updatedNotification);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    // Recherches spécifiques
    @GetMapping("/receiver/{receiver}")
    public ResponseEntity<List<Notification>> getNotificationsByReceiver(@PathVariable String receiver) {
        List<Notification> notifications = notificationService.getNotificationsByReceiver(receiver);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<Notification>> getNotificationsByRole(@PathVariable String role) {
        List<Notification> notifications = notificationService.getNotificationsByRole(role);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/phone/{phone}")
    public ResponseEntity<List<Notification>> getNotificationsByPhone(@PathVariable String phone) {
        List<Notification> notifications = notificationService.getNotificationsByPhone(phone);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/receiver/{receiver}/role/{role}")
    public ResponseEntity<List<Notification>> getNotificationsByReceiverAndRole(
            @PathVariable String receiver,
            @PathVariable String role) {
        List<Notification> notifications = notificationService.getNotificationsByReceiverAndRole(receiver, role);
        return ResponseEntity.ok(notifications);
    }

    // Recherches avec dates
    @GetMapping("/after-date")
    public ResponseEntity<List<Notification>> getNotificationsAfterDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        List<Notification> notifications = notificationService.getNotificationsAfterDate(date);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/before-event-date")
    public ResponseEntity<List<Notification>> getNotificationsBeforeEventDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date date) {
        List<Notification> notifications = notificationService.getNotificationsBeforeEventDate(date);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/between-dates")
    public ResponseEntity<List<Notification>> getNotificationsBetweenDates(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        List<Notification> notifications = notificationService.getNotificationsBetweenDates(startDate, endDate);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate) {
        List<Notification> notifications = notificationService.getRecentNotifications(startDate);
        return ResponseEntity.ok(notifications);
    }

    // Recherche par mot-clé
    @GetMapping("/search")
    public ResponseEntity<List<Notification>> searchNotifications(@RequestParam String keyword) {
        List<Notification> notifications = notificationService.searchNotificationsByKeyword(keyword);
        return ResponseEntity.ok(notifications);
    }

    // Comptage
    @GetMapping("/count/receiver/{receiver}")
    public ResponseEntity<Long> countNotificationsByReceiver(@PathVariable String receiver) {
        Long count = notificationService.countNotificationsByReceiver(receiver);
        return ResponseEntity.ok(count);
    }

    // Notifications métier
    @PostMapping("/notify-loan-status")
    public ResponseEntity<Void> notifyLoanStatusChange(
            @RequestParam String memberEmail,
            @RequestParam String loanStatus,
            @RequestParam String details) {
        notificationService.notifyLoanStatusChange(memberEmail, loanStatus, details);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notify-approval")
    public ResponseEntity<Void> notifyApprovalRequired(
            @RequestParam String role,
            @RequestParam Long loanRequestId) {
        notificationService.notifyApprovalRequired(role, loanRequestId);
        return ResponseEntity.ok().build();
    }

    // Dernières notifications par destinataire
    @GetMapping("/latest/receiver/{receiver}")
    public ResponseEntity<List<Notification>> getLatestNotificationsByReceiver(@PathVariable String receiver) {
        List<Notification> notifications = notificationService.getLatestNotificationsByReceiver(receiver);
        return ResponseEntity.ok(notifications);
    }

    // Notifications par rôle et date
    @GetMapping("/role/{role}/after-date")
    public ResponseEntity<List<Notification>> getNotificationsByRoleAndDateAfter(
            @PathVariable String role,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate) {
        List<Notification> notifications = notificationService.getNotificationsByRoleAndDateAfter(role, startDate);
        return ResponseEntity.ok(notifications);
    }
}