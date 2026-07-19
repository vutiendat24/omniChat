package com.omnichat.routing.repository;

import com.omnichat.routing.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findByEmail(String email);

    boolean existsByEmail(String email);
}
