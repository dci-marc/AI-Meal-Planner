package org.dci.aimealplanner.integration.aiapi.dtos.planning;

import java.time.LocalDate;
import java.util.List;

public record MealPlanGenerationRequest(
        LocalDate startDate,
        LocalDate endDate,
        Integer age,
        String gender,
        Integer heightCm,
        Double weightKg,
        String activityLevel,
        String goal,
        Integer mealsPerDay,
        List<String> dietaryPreferences,
        List<String> allergies,
        List<String> dislikedIngredients,
        List<String> preferredCuisines,
        Integer targetKcalPerDay
        ) {
}
