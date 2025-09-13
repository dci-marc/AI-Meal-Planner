package org.dci.aimealplanner.models.planning;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateMealPlanDTO(
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal kcalTargetPerDay
) {
}
