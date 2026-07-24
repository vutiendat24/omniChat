package com.omnichat.tenant.controller;

import com.omnichat.tenant.dto.CreateTeamReq;
import com.omnichat.tenant.dto.TeamRes;
import com.omnichat.tenant.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamRes> createTeam(
            @PathVariable String tenantId,
            @Valid @RequestBody CreateTeamReq request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(tenantId, request));
    }
}
