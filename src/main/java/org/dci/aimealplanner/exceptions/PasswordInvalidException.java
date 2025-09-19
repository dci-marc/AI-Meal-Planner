package org.dci.aimealplanner.exceptions;

public class PasswordInvalidException extends RuntimeException {
    public PasswordInvalidException(String message) {
        super(message);
    }
}
