package org.dci.aimealplanner.services.ingredients;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.*;
import org.dci.aimealplanner.exceptions.InvalidIngredientException;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientFromAI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngredientResolverService {
    private final GroqApiClient groqApiClient;
    private final IngredientService ingredientService;
    private final IngredientCategoryService ingredientCategoryService;
    private final IngredientUnitRatioService ingredientUnitRatioService;
    private final UnitService unitService;

    @Transactional
    public Ingredient resolveOrCreate(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new InvalidIngredientException("Ingredient name is blank");
        }
        String name = rawName.trim();

        Optional<Ingredient> existing = ingredientService.findByNameIgnoreCase(name);
        if (existing.isPresent()) return existing.get();

        IngredientFromAI ai = groqApiClient.generateIngredient(name);
        return persistFromAI(ai, name);
    }

    @Transactional
    public Unit ensureUnit(String code, String display) {
        if (code == null || code.isBlank()) throw new InvalidIngredientException("Unit code is blank");
        String norm = code.trim().toLowerCase();
        Unit unit = unitService.findByCode(norm);
        if (unit != null) return unit;
        Unit newUnit = new Unit();
        newUnit.setCode(norm);
        newUnit.setDisplayName((display != null && !display.isBlank()) ? display : norm);
        return unitService.save(newUnit);
    }


    private Ingredient persistFromAI(IngredientFromAI ai, String fallbackName) {
        String catName = (ai.getCategory() == null || ai.getCategory().isBlank()) ? "Other" : ai.getCategory().trim();
        IngredientCategory category = ingredientCategoryService.findByNameIgnoreCase(catName);
        if (category == null) {
            category = new IngredientCategory();
            category.setName(capitalize(catName));
            category = ingredientCategoryService.save(category);
        }

        NutritionFact nutritionFact = new NutritionFact();
        if (ai.getNutrition() != null) {
            nutritionFact.setKcal(safe(ai.getNutrition().getKcal()));
            nutritionFact.setProtein(safe(ai.getNutrition().getProtein()));
            nutritionFact.setCarbs(safe(ai.getNutrition().getCarbs()));
            nutritionFact.setFat(safe(ai.getNutrition().getFat()));
            nutritionFact.setFiber(safe(ai.getNutrition().getFiber()));
            nutritionFact.setSugar(safe(ai.getNutrition().getSugar()));
        }

        Ingredient ingredient = new Ingredient();
        ingredient.setName(capitalize(firstNonBlank(ai.getName(), fallbackName)));
        ingredient.setCategory(category);
        ingredient.setNutritionFact(nutritionFact);
        ingredient.setUnitRatios(new ArrayList<>());
        ingredient = ingredientService.save(ingredient);

        if (ai.getUnits() != null) {
            for (var unit : ai.getUnits()) {
                ensureUnit(unit.getCode(), unit.getDisplay());
            }
        }

        if (ai.getRatios() != null) {
            var ratios = new ArrayList<IngredientUnitRatio>();
            for (var ratio : ai.getRatios()) {
                if (ratio == null || ratio.getFromUnitCode() == null || ratio.getToUnitCode() == null) continue;
                if (!"g".equalsIgnoreCase(ratio.getToUnitCode())) continue;
                Unit unit = ensureUnit(ratio.getFromUnitCode(), null);
                IngredientUnitRatio iur = new IngredientUnitRatio();
                iur.setIngredient(ingredient);
                iur.setUnit(unit);
                iur.setRatio(ratio.getFactor());
                ratios.add(iur);
            }
            if (!ratios.isEmpty()) {
                ingredientUnitRatioService.saveAll(ratios);
                ingredient.getUnitRatios().addAll(ratios);
            }
        }

        return ingredient;
    }

    private Double safe(Number n) { return n == null ? 0.0 : n.doubleValue(); }
    private String firstNonBlank(String a, String b) { return (a != null && !a.isBlank()) ? a : b; }
    private String capitalize(String s) { return (s == null || s.isBlank()) ? s : s.substring(0,1).toUpperCase() + s.substring(1); }
}
