package org.dci.aimealplanner.models.planning;

import org.dci.aimealplanner.models.recipes.RecipeCardDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NextMealDTO(
        LocalDate date,
        String mealSlot,
        RecipeCardDTO recipe,
        BigDecimal servings
) {
}
