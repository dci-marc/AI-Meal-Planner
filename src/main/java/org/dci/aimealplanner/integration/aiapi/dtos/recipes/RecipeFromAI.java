package org.dci.aimealplanner.integration.aiapi.dtos.recipes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeFromAI {
    private String title;
    private String difficulty;
    private Integer preparation_time_minutes;
    private BigDecimal servings;
    private List<String> meal_categories;
    private List<IngredientLine> ingredients;
    private List<String> instructions;
}
