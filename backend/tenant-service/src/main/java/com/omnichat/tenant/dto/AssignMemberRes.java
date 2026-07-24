package com.omnichat.tenant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberRes {

    private String message;
    private int addedCount;
    private int ignoredCount;

}
