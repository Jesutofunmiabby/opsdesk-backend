package com.opsdesk.opsdeskbackend.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Emails are saved in lower case, so callers pass a lower-case email.
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
