package org.dci.aimealplanner.integration.aiapi.dtos.planning;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PlannedMealFromAI(
        String slot,
        String title,
        Double servings,
        @JsonProperty("meal_categories") List<String> mealCategories,
        String notes
) {
}
