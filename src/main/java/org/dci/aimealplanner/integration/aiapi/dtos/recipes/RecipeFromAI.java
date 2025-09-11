package org.dci.aimealplanner.integration.aiapi.dtos.recipes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.Difficulty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeFromAI {
    private String title;
    private String difficulty;

    @JsonProperty("preparation_time_minutes")
    private Integer preparationTimeMinutes;

    private BigDecimal servings;

    @JsonProperty("meal_categories")
    private List<String> mealCategories;

    private List<IngredientLine> ingredients;
    private List<String> instructions;

    public Recipe toRecipeSkeleton() {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setDifficulty(mapDifficulty(difficulty));
        recipe.setPreparationTimeMinutes(preparationTimeMinutes);
        recipe.setServings(servings);
        recipe.setInstructions(String.join("\n", instructions));

        return recipe;
    }

    public static Difficulty mapDifficulty(String aiValue) {
        if (aiValue == null) return null;

        return switch (aiValue.trim().toLowerCase()) {
            case "easy" -> Difficulty.EASY;
            case "medium" -> Difficulty.MEDIUM;
            case "hard" -> Difficulty.HARD;
            default -> throw new IllegalArgumentException("Unknown difficulty: " + aiValue);
        };
    }
}
