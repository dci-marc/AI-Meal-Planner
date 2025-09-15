package org.dci.aimealplanner.models.recipes;

import org.dci.aimealplanner.entities.recipes.Recipe;

public record SimpleRecipeDTO(
        Long id,
        String title
) {
    public static SimpleRecipeDTO from(Recipe recipe) {
        return new SimpleRecipeDTO(recipe.getId(), recipe.getTitle());
    }
}
