package com.novamart.auth.repository;

import com.novamart.auth.domain.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdOrderByDefaultAddressDescCreatedAtAsc(UUID userId);

    Optional<Address> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    /**
     * Clears the default flag across a user's other addresses in one statement.
     * Loading them all to flip a boolean would be a needless read of the whole
     * address book on every save.
     */
    @Modifying
    @Query("update Address a set a.defaultAddress = false where a.userId = :userId and a.id <> :keepId")
    void clearDefaultExcept(@Param("userId") UUID userId, @Param("keepId") UUID keepId);
}
