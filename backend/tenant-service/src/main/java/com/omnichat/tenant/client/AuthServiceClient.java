package com.omnichat.tenant.client;

import com.omnichat.tenant.dto.CreateOwnerReq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @PostMapping("/api/v1/internal/users/owner")
    ResponseEntity<Void> createOwnerAccount(@RequestBody CreateOwnerReq request);

}
