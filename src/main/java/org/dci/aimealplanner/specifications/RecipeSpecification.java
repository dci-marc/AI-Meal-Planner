package org.dci.aimealplanner.specifications;

import jakarta.persistence.criteria.*;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.recipes.MealCategory;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.recipes.RecipeIngredient;
import org.dci.aimealplanner.models.Difficulty;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Set;

public class RecipeSpecification {
    public static Specification<Recipe> byTitleContains(String title) {
        return (root, query, criteriaBuilder) -> {
            if (title == null || title.trim().isBlank()) {
                return null;
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Recipe> byDifficulty(Difficulty difficulty) {
        return (root, query, criteriaBuilder) -> {
            if (difficulty == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("difficulty"), difficulty);
        };
    }

    public static Specification<Recipe> byPreparationTimeLessThan(Integer preparationTime) {
        return (root, query, criteriaBuilder) -> {
            if (preparationTime == null || preparationTime.intValue() < 0) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("preparationTime"), preparationTime);
        };
    }

    public static Specification<Recipe> byCategoryIds(Set<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) {
                return null;
            }

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Recipe> root2 = subquery.from(Recipe.class);

            Join<Recipe, MealCategory> mealCategoryJoin = root2.join("mealCategories", JoinType.INNER);

            subquery.select(criteriaBuilder.countDistinct(mealCategoryJoin.get("id")));

            subquery.where(
                    criteriaBuilder.equal(root2.get("id"), root.get("id")),
                    mealCategoryJoin.get("id").in(categoryIds)
            );

            return criteriaBuilder.equal(subquery, (long) categoryIds.size());
        };
    }

    public static Specification<Recipe> byIngredientContains(Set<Long> ingredientIds) {
        return (root, query, criteriaBuilder) -> {
            if (ingredientIds == null || ingredientIds.isEmpty()) {
                return null;
            }
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Recipe> root2 = subquery.from(Recipe.class);

            Join<Recipe, RecipeIngredient> recipeIngredients = root2.join("recipeIngredients", JoinType.INNER);
            Join<RecipeIngredient, Ingredient> rIIJoin = recipeIngredients.join("ingredient", JoinType.INNER);

            subquery.select(criteriaBuilder.countDistinct(rIIJoin.get("id")))
                    .where(
                            criteriaBuilder.equal(root2.get("id"), root.get("id")),
                            rIIJoin.get("id").in(ingredientIds)
                    );
            return criteriaBuilder.equal(subquery, (long) ingredientIds.size());
        };
    }

}
