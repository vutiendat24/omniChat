package com.omnichat.notification.repository;

import com.omnichat.notification.domain.entity.BouncedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BouncedEmailRepository extends JpaRepository<BouncedEmail, Long> {
    boolean existsByEmail(String email);
}
