package com.omnichat.auth.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserDto {
    private Long id;
    private String email;
    private Set<String> roles;
    private String teamName;
}
