package com.omnichat.websocket.handler;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class AgentHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String agentId = (String) attributes.get(AgentHandshakeInterceptor.AGENT_ID_ATTR);
        if (agentId != null) {
            return () -> agentId;
        }
        return super.determineUser(request, wsHandler, attributes);
    }
}
