package com.omnichat.integration.controller;

import com.omnichat.integration.service.ChannelConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/channels")
public class ChannelController {

    private final ChannelConnectionService channelConnectionService;

    public ChannelController(ChannelConnectionService channelConnectionService) {
        this.channelConnectionService = channelConnectionService;
    }

    @PostMapping("/{id}/disconnect")
    public ResponseEntity<Void> disconnectChannel(@PathVariable("id") Long id) {
        channelConnectionService.disconnectChannel(id);
        return ResponseEntity.ok().build();
    }
}
