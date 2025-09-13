package org.dci.aimealplanner.integration.aiapi.dtos.planning;

import java.util.List;

public record MealPlanDayFromAI(
        String date,
        List<PlannedMealFromAI> meals
) {
}
