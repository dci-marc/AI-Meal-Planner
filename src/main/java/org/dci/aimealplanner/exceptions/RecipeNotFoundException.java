package org.dci.aimealplanner.exceptions;

public class RecipeNotFoundException extends RuntimeException {
    public RecipeNotFoundException(String message) {
        super(message);
    }
}
