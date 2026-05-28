package com.specflow.exceptions;

public class AlreadyParticipantException extends RuntimeException {
    public AlreadyParticipantException(String message) {
        super(message);
    }
}
