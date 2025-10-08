package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Notification;
import com.wbf.mutuelle.repositories.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public Optional<Notification> getNotificationById(Long id) {
        return notificationRepository.findById(id);
    }

    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Notification updateNotification(Long id, Notification updatedNotification) {
        return notificationRepository.findById(id).map(existing -> {
            existing.setMsg(updatedNotification.getMsg());
            existing.setSend_date(updatedNotification.getSend_date());
            existing.setEvent_date(updatedNotification.getEvent_date());
            existing.setReceiver(updatedNotification.getReceiver());
            existing.setPhone(updatedNotification.getPhone());
            existing.setRole(updatedNotification.getRole());
            return notificationRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Notification not found with id " + id));
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    public void notifyLoanStatusChange(String memberEmail, String loanStatus, String details) {

    }

    public void notifyApprovalRequired(String role, Long loanRequestId) {
        
    }
}
