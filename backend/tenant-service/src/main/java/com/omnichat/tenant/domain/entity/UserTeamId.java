package com.omnichat.tenant.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserTeamId implements Serializable {

    @Column(name = "user_id", length = 255)
    private String userId;

    @Column(name = "team_id", length = 36)
    private String teamId;
}
