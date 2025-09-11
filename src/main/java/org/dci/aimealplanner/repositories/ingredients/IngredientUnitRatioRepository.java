package org.dci.aimealplanner.repositories.ingredients;

import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IngredientUnitRatioRepository extends JpaRepository<IngredientUnitRatio, Long> {

    List<IngredientUnitRatio> findByIngredient(Ingredient ingredient);
    List<IngredientUnitRatio> findByIngredientIdIn(List<Long> ingredientIds);
    List<IngredientUnitRatio> findByIngredientId(Long  ingredientId);
    Optional<IngredientUnitRatio> findByIngredientAndUnit(Ingredient ingredient, Unit unit);

    Optional<IngredientUnitRatio> findFirstByIngredientIdAndUnitId(Long ingredientId, Long unitId);
    boolean existsByIngredientIdAndUnitId(Long ingredientId, Long unitId);
}
