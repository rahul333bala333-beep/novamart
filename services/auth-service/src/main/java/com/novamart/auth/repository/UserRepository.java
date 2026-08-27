package com.novamart.auth.repository;

import com.novamart.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Case-insensitive search across name and email.
     *
     * <p>Written with {@code lower(...) like lower(...)} rather than PostgreSQL's
     * {@code ilike} so the identical query runs on H2 in tests and in the
     * zero-install local profile.
     *
     * <p>{@code cast(:search as string)} is required by PostgreSQL: it cannot
     * infer the type of a bare parameter in a null check, assumes {@code bytea},
     * and then fails on the surrounding {@code lower(...)}.
     */
    @Query("""
            select u from User u
            where cast(:search as string) is null
               or lower(u.firstName) like lower(concat('%', cast(:search as string), '%'))
               or lower(u.lastName)  like lower(concat('%', cast(:search as string), '%'))
               or lower(u.email)     like lower(concat('%', cast(:search as string), '%'))
            """)
    Page<User> search(@Param("search") String search, Pageable pageable);
}
