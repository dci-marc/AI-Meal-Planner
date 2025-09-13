package org.dci.aimealplanner.integration.aiapi.dtos.planning;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MealPlanFromAI(
        String name,
        @JsonProperty("target_kcal_per_day") Integer targetKcalPerDay,
        List<MealPlanDayFromAI> days
) {
}
