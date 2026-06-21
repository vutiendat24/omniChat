package com.omnichat.integration.repository;

import com.omnichat.integration.entity.ChannelConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelConnectionRepository extends JpaRepository<ChannelConnection, Long> {

    Optional<ChannelConnection> findByIdAndStatus(Long id, ChannelConnection.ConnectionStatus status);
}
