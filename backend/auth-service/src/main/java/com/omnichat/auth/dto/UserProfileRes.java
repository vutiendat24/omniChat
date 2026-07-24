package com.omnichat.auth.dto;

import com.omnichat.auth.domain.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileRes {
    private Long id;
    private String email;
    private String fullName;
    private String avatar;
    private UserStatus status;
}
