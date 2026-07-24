package com.omnichat.tenant.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberReq {

    @NotEmpty(message = "Danh sách userIds không được để trống")
    @Size(max = 50, message = "Chỉ được gán tối đa 50 thành viên trong một thao tác")
    private List<String> userIds;

}
