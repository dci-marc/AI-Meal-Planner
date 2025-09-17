package org.dci.aimealplanner.models.recipes;

import jakarta.validation.constraints.*;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.Difficulty;

import java.math.BigDecimal;

public record RecipeForm(
        @NotBlank @Size(max = 255) String title,
        Difficulty difficulty,
        @NotNull @Min(0) Integer preparationTimeMinutes,
        @NotNull @DecimalMin("0.1") BigDecimal servings,
        @Size(max = 2048) String heroImageUrl,
        @Size(max = 65535) String instructions,
        @DecimalMin("0.0") BigDecimal kcalPerServ,
        @DecimalMin("0.0") BigDecimal proteinPerServ,
        @DecimalMin("0.0") BigDecimal carbsPerServ,
        @DecimalMin("0.0") BigDecimal fatPerServ
) {
    public static RecipeForm blank() {
        return new RecipeForm(
                "", Difficulty.EASY, 0, new BigDecimal("1"),
                null, null, null, null, null, null
        );
    }
    public static RecipeForm from(Recipe r) {
        return new RecipeForm(
                r.getTitle(),
                r.getDifficulty(),
                r.getPreparationTimeMinutes(),
                r.getServings(),
                r.getImage() != null ? r.getImage().getImageUrl() : null,
                r.getInstructions(),
                r.getKcalPerServ(),
                r.getProteinPerServ(),
                r.getCarbsPerServ(),
                r.getFatPerServ()
        );
    }
}
