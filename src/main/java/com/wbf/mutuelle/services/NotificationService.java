package com.wbf.mutuelle.services;

import com.wbf.mutuelle.entities.Notification;
import com.wbf.mutuelle.exceptions.NotificationNotFoundException;
import com.wbf.mutuelle.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getAllNotifications() {
        log.info("Récupération de toutes les notifications");
        return notificationRepository.findAll();
    }

    public Notification getNotificationById(Long id) {
        log.info("Récupération de la notification avec l'id: {}", id);
        return notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification non trouvée avec l'id: " + id));
    }

    public Notification createNotification(Notification notification) {
        log.info("Création d'une nouvelle notification pour: {}", notification.getReceiver());

        // Définir la date d'envoi si non précisée
        if (notification.getSend_date() == null) {
            notification.setSend_date(new Date());
        }

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification créée avec succès, ID: {}", savedNotification.getId());
        return savedNotification;
    }

    public Notification updateNotification(Long id, Notification updatedNotification) {
        log.info("Mise à jour de la notification avec l'id: {}", id);

        Notification existing = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException("Notification non trouvée avec l'id: " + id));

        // Mise à jour des champs
        existing.setMsg(updatedNotification.getMsg());
        existing.setSend_date(updatedNotification.getSend_date());
        existing.setEvent_date(updatedNotification.getEvent_date());
        existing.setReceiver(updatedNotification.getReceiver());
        existing.setPhone(updatedNotification.getPhone());
        existing.setRole(updatedNotification.getRole());

        Notification savedNotification = notificationRepository.save(existing);
        log.info("Notification mise à jour avec succès, ID: {}", savedNotification.getId());
        return savedNotification;
    }

    public void deleteNotification(Long id) {
        log.info("Suppression de la notification avec l'id: {}", id);

        // Vérifier si la notification existe avant suppression
        if (!notificationRepository.existsById(id)) {
            throw new NotificationNotFoundException("Notification non trouvée avec l'id: " + id);
        }

        notificationRepository.deleteById(id);
        log.info("Notification supprimée avec succès, ID: {}", id);
    }

    public void notifyLoanStatusChange(String memberEmail, String loanStatus, String details) {
        log.info("Envoi de notification de changement de statut de prêt à: {}", memberEmail);

        Notification notification = new Notification();
        notification.setMsg("Changement de statut de votre prêt: " + loanStatus + ". Détails: " + details);
        notification.setReceiver(memberEmail);
        notification.setSend_date(new Date());
        notification.setRole("MEMBER");

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification de statut de prêt envoyée avec succès, ID: {}", savedNotification.getId());
    }

    public void notifyApprovalRequired(String role, Long loanRequestId) {
        log.info("Envoi de notification d'approbation requise pour le rôle: {}, demande de prêt: {}", role, loanRequestId);

        Notification notification = new Notification();
        notification.setMsg("Approbation requise pour la demande de prêt #" + loanRequestId);
        notification.setRole(role);
        notification.setSend_date(new Date());
        notification.setReceiver("Administrateur " + role);

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification d'approbation envoyée avec succès, ID: {}", savedNotification.getId());
    }

    public List<Notification> getNotificationsByReceiver(String receiver) {
        log.info("Récupération des notifications pour le destinataire: {}", receiver);
        return notificationRepository.findByReceiver(receiver);
    }

    public List<Notification> getNotificationsByRole(String role) {
        log.info("Récupération des notifications pour le rôle: {}", role);
        return notificationRepository.findByRole(role);
    }

    public List<Notification> getNotificationsByPhone(String phone) {
        log.info("Récupération des notifications pour le téléphone: {}", phone);
        return notificationRepository.findByPhone(phone);
    }

    // Méthodes avec dates utilisant @Query
    public List<Notification> getNotificationsAfterDate(Date date) {
        log.info("Récupération des notifications après la date: {}", date);
        return notificationRepository.findBySendDateAfter(date);
    }

    public List<Notification> getNotificationsBeforeEventDate(Date date) {
        log.info("Récupération des notifications avant la date d'événement: {}", date);
        return notificationRepository.findByEventDateBefore(date);
    }

    public List<Notification> getRecentNotifications(Date startDate) {
        log.info("Récupération des notifications récentes depuis: {}", startDate);
        return notificationRepository.findRecentNotifications(startDate);
    }

    public List<Notification> getNotificationsBetweenDates(Date startDate, Date endDate) {
        log.info("Récupération des notifications entre {} et {}", startDate, endDate);
        return notificationRepository.findNotificationsBetweenDates(startDate, endDate);
    }

    public List<Notification> searchNotificationsByKeyword(String keyword) {
        log.info("Recherche de notifications avec le mot-clé: {}", keyword);
        return notificationRepository.findByMessageContaining(keyword);
    }

    public Long countNotificationsByReceiver(String receiver) {
        log.info("Comptage des notifications pour le destinataire: {}", receiver);
        return notificationRepository.countByReceiver(receiver);
    }

    public List<Notification> getNotificationsByReceiverAndRole(String receiver, String role) {
        log.info("Récupération des notifications pour le destinataire: {} et rôle: {}", receiver, role);
        return notificationRepository.findByReceiverAndRole(receiver, role);
    }

    public List<Notification> getLatestNotificationsByReceiver(String receiver) {
        log.info("Récupération des dernières notifications pour le destinataire: {}", receiver);
        return notificationRepository.findLatestByReceiver(receiver);
    }

    public List<Notification> getNotificationsByRoleAndDateAfter(String role, Date startDate) {
        log.info("Récupération des notifications pour le rôle: {} après la date: {}", role, startDate);
        return notificationRepository.findByRoleAndDateAfter(role, startDate);
    }

    public void notifyRepaymentStatusChange(String email, String overdue, String s) {
    }
}