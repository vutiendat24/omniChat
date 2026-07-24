package com.omnichat.auth.exception;

import org.springframework.http.HttpStatus;

public class RoleException extends RuntimeException {
    private final HttpStatus status;

    public RoleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
