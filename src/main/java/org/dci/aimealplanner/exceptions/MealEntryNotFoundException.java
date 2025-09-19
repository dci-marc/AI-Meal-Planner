package org.dci.aimealplanner.exceptions;

public class MealEntryNotFoundException extends RuntimeException {
    public MealEntryNotFoundException(String message) {
        super(message);
    }
}
