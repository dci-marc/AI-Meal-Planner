package org.dci.aimealplanner.exceptions;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(String message) {
        super(message);
    }
}
