package org.dci.aimealplanner.services.ingredients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.*;
import org.dci.aimealplanner.exceptions.IngredientNotFoundException;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientFromAI;
import org.dci.aimealplanner.integration.foodapi.dto.FoodItem;
import org.dci.aimealplanner.repositories.ingredients.IngredientRepository;
import org.dci.aimealplanner.repositories.ingredients.IngredientSummary;
import org.dci.aimealplanner.repositories.ingredients.NutritionFactRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IngredientService {
    private final IngredientRepository ingredientRepository;
    private final IngredientCategoryService ingredientCategoryService;
    private final NutritionFactRepository nutritionFactRepository;


    public boolean exists(String name) {
        return ingredientRepository.findByName(name).isPresent();
    }

    @Transactional
    public Ingredient upsertFromUsda(String requestedName, FoodItem foodItem) {
        String name = normalize(requestedName);

        Ingredient ingredient = ingredientRepository.findByNameIgnoreCaseLike(name).orElseGet(() -> {
                    Ingredient ing = new Ingredient();
                    ing.setName(name);
                    return ing;
                });


        ingredient.setCategory(ingredientCategoryService.findCategory(foodItem.getFoodCategory(), foodItem.getDescription()));
        NutritionFact nutritionFact = foodItem.toNutritionFact();
        nutritionFactRepository.save(nutritionFact);
        ingredient.setNutritionFact(nutritionFact);

        return ingredientRepository.save(ingredient);
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public List<Ingredient> findAll() {
        return  ingredientRepository.findAll();
    }

    public Ingredient findById(Long id) {
        return ingredientRepository.findById(id).orElseThrow(() -> new IngredientNotFoundException("Ingredient not found"));
    }

    public Page<IngredientSummary> search(String query, PageRequest pageRequest) {
        return ingredientRepository.smartSearch(query, pageRequest);
    }

    public List<IngredientSummary> findByIdIn(List<Long> ids) {
        return ingredientRepository.findByIdIn(ids);
    }

    public String normalizeName(String raw) {
        if (raw == null) return "";
        String s = raw.trim().replaceAll("\\s+", " ").toLowerCase();
        return s;
    }

    public Optional<Ingredient> findByNameIgnoreCase(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String collapsed = name.trim().replaceAll("\\s+", " ");
        return ingredientRepository.findFirstByNameIgnoreCase(collapsed);
    }

    public boolean existsByNameIgnoreCase(String name) {
        return findByNameIgnoreCase(name).isPresent();
    }

    public Ingredient createWithNutrition(String name, IngredientCategory category, Double kCal, Double protein, Double carbs, Double fat, Double fiber, Double sugar) {
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setCategory(category);
        NutritionFact nutritionFact = new NutritionFact();
        nutritionFact.setKcal(kCal);
        nutritionFact.setProtein(protein);
        nutritionFact.setCarbs(carbs);
        nutritionFact.setFiber(fiber);
        nutritionFact.setSugar(sugar);
        ingredient.setNutritionFact(nutritionFact);
        return ingredientRepository.save(ingredient);
    }

    public Ingredient save(Ingredient ingredient) {
        return ingredientRepository.save(ingredient);
    }
}
