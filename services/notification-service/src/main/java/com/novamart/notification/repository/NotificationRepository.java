package com.novamart.notification.repository;

import com.novamart.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * One query serves both the shopper view and the admin view.
     *
     * <p>Passing {@code userId} as null means "every user" and is only ever done
     * for an administrator; the controller decides which, so a shopper can never
     * widen their own scope by omitting a parameter.
     */
    @Query("""
            select n from Notification n
            where (:userId is null or n.userId = :userId)
              and (:type is null or n.type = :type)
            """)
    Page<Notification> findFiltered(@Param("userId") UUID userId,
                                    @Param("type") Notification.Type type,
                                    Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    java.util.List<Notification> findByUserIdAndReadFalse(UUID userId);

    java.util.Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
}
