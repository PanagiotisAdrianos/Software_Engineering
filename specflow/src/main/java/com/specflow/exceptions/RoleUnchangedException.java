package com.specflow.exceptions;

public class RoleUnchangedException extends RuntimeException {
    public RoleUnchangedException(String message) {
        super(message);
    }
}
