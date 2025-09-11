package org.dci.aimealplanner.services.ingredients;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.repositories.ingredients.IngredientUnitRatioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IngredientUnitRatioService {
    private final IngredientUnitRatioRepository ingredientUnitRatioRepository;

    public IngredientUnitRatio create(IngredientUnitRatio ingredientUnitRation) {
        return ingredientUnitRatioRepository.save(ingredientUnitRation);
    }

    public List<IngredientUnitRatio> findAll() {
        return ingredientUnitRatioRepository.findAll();
    }

    public boolean rationExists(Ingredient ingredient) {
        return !ingredientUnitRatioRepository.findByIngredient(ingredient).isEmpty();
    }

    public List<IngredientUnitRatio> findByIngredientIdIn(List<Long> ingredientIds) {
        return ingredientUnitRatioRepository.findByIngredientIdIn(ingredientIds);
    }

    public List<IngredientUnitRatio> findByIngredient(Ingredient ingredient) {
        return ingredientUnitRatioRepository.findByIngredient(ingredient);
    }

    public List<IngredientUnitRatio> findByIngredientId(Long id) {
        return ingredientUnitRatioRepository.findByIngredientId(id);
    }

    public IngredientUnitRatio findRatio(Ingredient ingredient, Unit unit) {
        return ingredientUnitRatioRepository.findByIngredientAndUnit(ingredient, unit)
                .orElseThrow(() -> new RuntimeException("Ratio not found"));
    }

    public IngredientUnitRatio save(IngredientUnitRatio row) {
        return ingredientUnitRatioRepository.save(row);
    }

    @Transactional
    public IngredientUnitRatio upsert(Ingredient ingredient, Unit unit, double ratio) {
        var existing = ingredientUnitRatioRepository.findFirstByIngredientIdAndUnitId(ingredient.getId(), unit.getId())
                .orElseGet(() -> {
                    var r = new IngredientUnitRatio();
                    r.setIngredient(ingredient);
                    r.setUnit(unit);
                    return r;
                });
        existing.setRatio(ratio);
        return ingredientUnitRatioRepository.save(existing);
    }

    @Transactional
    public void ensureGramRow(Ingredient ingredient, Unit gramUnit) {
        upsert(ingredient, gramUnit, 1.0d);
    }

}
