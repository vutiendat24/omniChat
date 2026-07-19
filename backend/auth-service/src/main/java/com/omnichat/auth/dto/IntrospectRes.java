package com.omnichat.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IntrospectRes {
    private boolean valid;
}
