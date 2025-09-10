package org.dci.aimealplanner.integration.aiapi.dtos.recipes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.entities.recipes.RecipeIngredient;
import org.dci.aimealplanner.services.ingredients.IngredientLookupService;
import org.dci.aimealplanner.services.utils.TextNormalize;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientLine {
    private String name;
    private BigDecimal amount;
    @JsonProperty("unit_code")
    private String unitCode;
    private String note;

    public RecipeIngredient toRecipeIngredient(IngredientLookupService lookupService) {
        Ingredient ingredient = lookupService.findIngredientByName(name);

        if (unitCode == null || unitCode.isBlank()) {
            throw new IllegalArgumentException("Unit is required for ingredient: " + name);
        }

        Unit unit = lookupService.findUnitByCode(unitCode);

        if (amount == null) {
            throw new IllegalArgumentException("Amount is required for ingredient: " + name);
        }

        RecipeIngredient recipeIngredient = new RecipeIngredient();
        recipeIngredient.setIngredient(ingredient);
        recipeIngredient.setUnit(unit);
        recipeIngredient.setAmount(new java.math.BigDecimal(amount.toString()));
        return recipeIngredient;
    }
}
