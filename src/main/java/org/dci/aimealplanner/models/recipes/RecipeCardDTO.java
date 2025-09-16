package org.dci.aimealplanner.models.recipes;

import java.math.BigDecimal;

public record RecipeCardDTO(
        Long id,
        String title,
        String heroImageUrl,
        Integer preparationTimeMinutes,
        BigDecimal kcalPerServ
) {
}
