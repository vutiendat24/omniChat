package com.omnichat.tenant.repository;

import com.omnichat.tenant.domain.entity.UserTeam;
import com.omnichat.tenant.domain.entity.UserTeamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, UserTeamId> {

    @Query("SELECT ut.id.userId FROM UserTeam ut WHERE ut.id.teamId = :teamId AND ut.id.userId IN :userIds")
    Set<String> findExistingUserIdsInTeam(@Param("teamId") String teamId, @Param("userIds") List<String> userIds);

    @Modifying
    @Query("DELETE FROM UserTeam ut WHERE ut.id.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
