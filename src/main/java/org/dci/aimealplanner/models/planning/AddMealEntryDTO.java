package org.dci.aimealplanner.models.planning;

import org.dci.aimealplanner.models.recipes.MealSlot;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddMealEntryDTO(
        Long mealPlanId,
        LocalDate entryDate,
        MealSlot mealSlot,
        Long recipeId,
        BigDecimal servings
) {
}
