package org.dci.aimealplanner.exceptions;

public class IngredientNotFoundException extends RuntimeException {
    public IngredientNotFoundException(String message) {
        super(message);
    }
}
