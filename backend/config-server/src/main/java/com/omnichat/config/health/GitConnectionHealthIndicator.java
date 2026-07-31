package com.omnichat.config.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Component("gitConnection")
public class GitConnectionHealthIndicator implements HealthIndicator {

    @Value("${spring.cloud.config.server.git.uri:#{null}}")
    private String gitUri;

    @Value("${spring.profiles.active:native}")
    private String activeProfile;

    @Override
    public Health health() {
        if (!"git".equalsIgnoreCase(activeProfile)) {
            return Health.up().withDetail("status", "Git profile is not active. Current profile: " + activeProfile).build();
        }

        if (gitUri == null || gitUri.isEmpty()) {
            return Health.down().withDetail("error", "Git URI is not configured").build();
        }

        try {
            // Very basic check for HTTPS URLs (Assuming github/gitlab)
            if (gitUri.startsWith("http://") || gitUri.startsWith("https://")) {
                URL url = new URL(gitUri);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.connect();
                
                int code = connection.getResponseCode();
                if (code >= 200 && code < 400) {
                    return Health.up().withDetail("gitUri", gitUri).build();
                } else {
                    return Health.down().withDetail("error", "HTTP Code: " + code).withDetail("gitUri", gitUri).build();
                }
            } else {
                return Health.up().withDetail("info", "SSH or file based URI - skipping explicit network check").withDetail("gitUri", gitUri).build();
            }
        } catch (Exception e) {
            return Health.down(e).withDetail("gitUri", gitUri).build();
        }
    }
}
