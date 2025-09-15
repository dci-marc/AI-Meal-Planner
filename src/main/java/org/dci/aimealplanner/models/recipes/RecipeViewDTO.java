package org.dci.aimealplanner.models.recipes;

import org.dci.aimealplanner.entities.ImageMetaData;
import org.dci.aimealplanner.entities.recipes.MealCategory;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.recipes.RecipeIngredient;
import org.dci.aimealplanner.models.Difficulty;
import org.dci.aimealplanner.models.SourceType;

import javax.xml.transform.Source;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record RecipeViewDTO(
        Long id,
        String title,
        Difficulty difficulty,
        Integer preparationTimeMinutes,
        BigDecimal servings,
        SourceType sourceType,
        ImageMetaData image,
        String instructions,
        List<RecipeIngredient> ingredients,
        Set<MealCategory> mealCategories,
        BigDecimal kcalPerServ,
        BigDecimal proteinPerServ,
        BigDecimal carbsPerServ,
        BigDecimal fatPerServ
) {
    public static RecipeViewDTO from(Recipe recipe) {
        return new RecipeViewDTO(recipe.getId(),recipe.getTitle(), recipe.getDifficulty(), recipe.getPreparationTimeMinutes(), recipe.getServings(),
                recipe.getSourceType(), recipe.getImage(), recipe.getInstructions(), recipe.getIngredients(), recipe.getMealCategories(), recipe.getKcalPerServ(),
                recipe.getProteinPerServ(), recipe.getCarbsPerServ(), recipe.getFatPerServ());
    }
}
