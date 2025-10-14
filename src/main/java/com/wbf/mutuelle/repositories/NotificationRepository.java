package com.wbf.mutuelle.repositories;

import com.wbf.mutuelle.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Recherche par destinataire
    List<Notification> findByReceiver(String receiver);

    // Recherche par rôle
    List<Notification> findByRole(String role);

    // Recherche par numéro de téléphone
    List<Notification> findByPhone(String phone);

    // Recherche combinée par destinataire et rôle
    List<Notification> findByReceiverAndRole(String receiver, String role);

    // RECHERCHES AVEC DATES - Utilisation de @Query pour éviter les problèmes de underscore
    @Query("SELECT n FROM Notification n WHERE n.send_date > :date ORDER BY n.send_date DESC")
    List<Notification> findBySendDateAfter(@Param("date") Date date);

    @Query("SELECT n FROM Notification n WHERE n.event_date < :date ORDER BY n.event_date DESC")
    List<Notification> findByEventDateBefore(@Param("date") Date date);

    @Query("SELECT n FROM Notification n WHERE n.send_date BETWEEN :startDate AND :endDate ORDER BY n.send_date DESC")
    List<Notification> findNotificationsBetweenDates(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

    // Requête personnalisée pour les notifications récentes
    @Query("SELECT n FROM Notification n WHERE n.send_date >= :startDate ORDER BY n.send_date DESC")
    List<Notification> findRecentNotifications(@Param("startDate") Date startDate);

    // Compter le nombre de notifications par destinataire
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.receiver = :receiver")
    Long countByReceiver(@Param("receiver") String receiver);

    // Recherche de notifications contenant un texte spécifique dans le message
    @Query("SELECT n FROM Notification n WHERE LOWER(n.msg) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Notification> findByMessageContaining(@Param("keyword") String keyword);

    // Recherche par destinataire avec pagination
    @Query("SELECT n FROM Notification n WHERE n.receiver = :receiver ORDER BY n.send_date DESC")
    List<Notification> findLatestByReceiver(@Param("receiver") String receiver);

    // Recherche par rôle et date
    @Query("SELECT n FROM Notification n WHERE n.role = :role AND n.send_date >= :startDate ORDER BY n.send_date DESC")
    List<Notification> findByRoleAndDateAfter(@Param("role") String role, @Param("startDate") Date startDate);
}