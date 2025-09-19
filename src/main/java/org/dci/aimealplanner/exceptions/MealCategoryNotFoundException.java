package org.dci.aimealplanner.exceptions;

public class MealCategoryNotFoundException extends RuntimeException {
    public MealCategoryNotFoundException(String message) {
        super(message);
    }
}
