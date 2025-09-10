package org.dci.aimealplanner.bootstrap.seeding;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientUnitFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.UnitRatios;
import org.dci.aimealplanner.integration.foodapi.FoodApiClient;
import org.dci.aimealplanner.integration.foodapi.OpenFoodFactsClient;
import org.dci.aimealplanner.integration.foodapi.dto.FoodItem;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.UserType;
import org.dci.aimealplanner.repositories.recipes.RecipeRepository;
import org.dci.aimealplanner.services.ingredients.IngredientCategoryService;
import org.dci.aimealplanner.services.ingredients.IngredientService;
import org.dci.aimealplanner.services.ingredients.IngredientUnitRatioService;
import org.dci.aimealplanner.services.ingredients.UnitService;
import org.dci.aimealplanner.services.recipes.MealCategoryService;
import org.dci.aimealplanner.services.users.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ApplicationSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ApplicationSeeder.class);
    private final FoodApiClient foodApiClient;
    private final OpenFoodFactsClient openFoodFactsClient;
    private final GroqApiClient  groqApiClient;
    private final IngredientService ingredientService;
    private final IngredientCategoryService ingredientCategoryService;
    private final MealCategoryService mealCategoryService;
    private final IngredientUnitRatioService ingredientUnitRatioService;
    private final UnitService unitService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    private final RecipeRepository recipeRepository;



    @Override
    public void run(ApplicationArguments args) throws Exception {
        //seedIngredients();
        // seedMealCategory();
        //retrieveUnits();
        //addAnAdmin();
        System.out.println(mealCategoryService.findAll().stream().map(mealCategory -> mealCategory.getName()).toList());

    }

    private void addAnAdmin() {
        if (!userService.isAdminExist()) {
            User admin = new User();
            admin.setEmail("admin@ai-meal-planner.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setUserType(UserType.LOCAL);
            admin.setEmailVerified(true);
            userService.create(admin);
        }
    }

    private void retrieveUnits() throws JsonProcessingException {
        List<Ingredient> ingredients = ingredientService.findAll();


        ingredients.forEach(ingredient -> {
            if (!ingredientUnitRatioService.rationExists(ingredient)) {
                try {
                    IngredientUnitFromAI ingredientUnitFromAI = groqApiClient.getUnitRatiosForIngredient(ingredient.getName());
                    List<UnitRatios> unitRatios = ingredientUnitFromAI.getUnits();
                    unitRatios.forEach(unitRatio -> {
                        Unit unit = unitService.findByCode(unitRatio.getUnitCode());
                        IngredientUnitRatio ingredientUnitRatio = new IngredientUnitRatio();
                        ingredientUnitRatio.setIngredient(ingredient);
                        ingredientUnitRatio.setUnit(unit);
                        ingredientUnitRatio.setRatio(Double.valueOf(unitRatio.getGramsPerUnit()));
                        ingredientUnitRatioService.create(ingredientUnitRatio);
                    });
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }

        });

    }

    private void seedIngredients() {
        List<String> seeds = IngredientSeedData.commonIngredients();

        int createdOrUpdated = 0;
        int skipped = 0;
        int missing = 0;
        int failed = 0;

        for (String name : seeds) {
            try {
                if (ingredientService.exists(name)) {
                    skipped++;
                    continue;
                }
                Optional<FoodItem> apiIngredientObject = foodApiClient.searchFood(name);
                if (apiIngredientObject.isEmpty()) {
                    missing++;
                    log.warn("No USDA hit for '{}'", name);
                    continue;
                }

                if (!apiIngredientObject.get().allNutritionFactsAvailable()) {
                    skipped++;
                    continue;
                }

                ingredientService.upsertFromUsda(name, apiIngredientObject.get());
                createdOrUpdated++;

            } catch (Exception e) {
                failed++;
                log.warn("Failed seeding '{}': {}", name, e.getMessage());
            }
            log.info("Seeding summary: created/updated={}, skipped={}, missing={}, failed={}",
                    createdOrUpdated, skipped, missing, failed);
        }
    }

    public void seedMealCategory() {
        mealCategoryService.addAll(openFoodFactsClient.getOffCategories().toMealCategories());
    }

    private List<String> getDbIngredientNames() {
        return ingredientService.findAll().stream()
                .map(ing -> ing.getName().trim().toLowerCase())
                .toList();
    }

    private List<String> findMissingIngredients(List<String> candidates) {
        Set<String> existing = new java.util.HashSet<>(getDbIngredientNames());
        return candidates.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .filter(s -> !existing.contains(s.toLowerCase()))
                .toList();
    }

    private void seedOneIfMissing(String name) {
        if (ingredientService.exists(name)) return;
        var apiIngredient = foodApiClient.searchFood(name);
        if (apiIngredient.isPresent() && apiIngredient.get().allNutritionFactsAvailable()) {
            ingredientService.upsertFromUsda(name, apiIngredient.get());
        }
    }

    private void seedNames(List<String> names) {
        for (String n : names) {
            seedOneIfMissing(n);
        }
    }
}
