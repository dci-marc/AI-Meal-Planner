package org.dci.aimealplanner.services.recipes;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.recipes.ImageMetaData;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.entities.ingredients.NutritionFact;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.entities.recipes.MealCategory;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.recipes.RecipeIngredient;
import org.dci.aimealplanner.exceptions.IngredientNotFoundException;
import org.dci.aimealplanner.exceptions.RecipeNotFoundException;
import org.dci.aimealplanner.integration.aiapi.dtos.recipes.RecipeFromAI;
import org.dci.aimealplanner.models.Difficulty;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.models.recipes.RecipeForm;
import org.dci.aimealplanner.models.recipes.UpdateRecipeDTO;
import org.dci.aimealplanner.repositories.recipes.RecipeRepository;
import org.dci.aimealplanner.services.ingredients.*;
import org.dci.aimealplanner.services.utils.CloudinaryService;
import org.dci.aimealplanner.services.users.UserService;
import org.dci.aimealplanner.specifications.RecipeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final CloudinaryService cloudinaryService;
    private final UserService userService;
    private final UnitService unitService;
    private final IngredientService ingredientService;
    private final IngredientLookupService lookup;
    private final MealCategoryService mealCategoryService;
    private final IngredientUnitRatioService ingredientUnitRatioService;
    private final IngredientResolverService ingredientResolverService;

    @Transactional
    public Recipe addNewRecipe(UpdateRecipeDTO recipeDTO, MultipartFile imageFile, String email) {
        List<RecipeIngredient> recipeIngredients = normalizeAndResolveIngredients(recipeDTO);
        Recipe recipe = new Recipe();
        updateRecipeFields(recipe, recipeDTO, imageFile, email);

        recipe.setFeatured(false);

        if (recipeIngredients != null) {
            for (RecipeIngredient recipeIngredient : recipeIngredients) {
                recipeIngredient.setRecipe(recipe);
            }
            recipe.setIngredients(recipeIngredients);
        }

        calculateNutritionFacts(recipe);
        recipe.setSourceType(SourceType.USER);

        return recipeRepository.save(recipe);
    }

    @Transactional
    public Recipe updateRecipe(Long id, UpdateRecipeDTO recipeDTO, MultipartFile imageFile, String email) {
        Recipe existingRecipe = recipeRepository.findById(id).orElseThrow();

        Map<Long, RecipeIngredient> existingById = existingRecipe.getIngredients().stream()
                .filter(recipeIngredient -> recipeIngredient.getId() != null)
                .collect(Collectors.toMap(RecipeIngredient::getId, ri -> ri));

        List<RecipeIngredient> target = new ArrayList<>();

        if (recipeDTO.ingredients() != null) {
            for (RecipeIngredient posted : recipeDTO.ingredients()) {
                Long childId = posted.getId();
                Long ingId   = posted.getIngredient() != null ? posted.getIngredient().getId() : null;
                Long unitId  = posted.getUnit() != null ? posted.getUnit().getId() : null;

                if (ingId == null || unitId == null || posted.getAmount() == null) continue;

                Ingredient ing = ingredientService.findById(ingId);
                Unit unit = unitService.findById(unitId);

                RecipeIngredient managedChild;
                if (childId != null) {

                    managedChild = existingById.get(childId);
                    if (managedChild == null) {
                        throw new IngredientNotFoundException("Ingredient row not found: id=" + childId);
                    }

                    managedChild.setIngredient(ing);
                    managedChild.setUnit(unit);
                    managedChild.setAmount(posted.getAmount());
                } else {

                    managedChild = new RecipeIngredient();
                    managedChild.setRecipe(existingRecipe);
                    managedChild.setIngredient(ing);
                    managedChild.setUnit(unit);
                    managedChild.setAmount(posted.getAmount());
                }

                target.add(managedChild);
            }
        }

        existingRecipe.getIngredients().clear();
        existingRecipe.getIngredients().addAll(target);

        calculateNutritionFacts(existingRecipe);


        updateRecipeFields(existingRecipe, recipeDTO, imageFile, email);

        return recipeRepository.save(existingRecipe);
    }


    public Recipe findById(long id) {
        return recipeRepository.findById(id).orElseThrow(() -> new RecipeNotFoundException("Recipe with id " + id + " not found"));
    }

    private List<RecipeIngredient> normalizeAndResolveIngredients(UpdateRecipeDTO recipeDTO) {
        if (recipeDTO.ingredients() == null) return null;

        List<RecipeIngredient> normalizeRecipeIngredients = new ArrayList<>();
        List<RecipeIngredient> recipeIngredients = recipeDTO.ingredients();
        for (RecipeIngredient recipeIngredient : recipeIngredients) {
            boolean noIngredient = (recipeIngredient.getIngredient() == null || recipeIngredient.getIngredient().getId() == null);
            boolean noAmount = (recipeIngredient.getAmount() == null);

            if (!noIngredient && !noAmount) {
                Unit unit = unitService.findById(recipeIngredient.getUnit().getId());
                Ingredient ingredient = ingredientService.findById(recipeIngredient.getIngredient().getId());
                BigDecimal amount = recipeIngredient.getAmount();
                RecipeIngredient newRecipeIngredient = new RecipeIngredient();
                newRecipeIngredient.setIngredient(ingredient);
                newRecipeIngredient.setAmount(amount);
                newRecipeIngredient.setUnit(unit);
                if (recipeIngredient.getId() != null) {
                    newRecipeIngredient.setId(recipeIngredient.getId());
                }
                normalizeRecipeIngredients.add(newRecipeIngredient);
            }
        }
        return normalizeRecipeIngredients;
    }


    private void updateRecipeFields(Recipe recipe, UpdateRecipeDTO recipeDTO, MultipartFile imageFile, String email) {
        recipe.setDifficulty(recipeDTO.difficulty());
        recipe.setPreparationTimeMinutes(recipeDTO.preparationTimeMinutes());
        recipe.setServings(recipeDTO.servings());
        recipe.setTitle(recipeDTO.title());
        recipe.setInstructions(recipeDTO.instructions());
        recipe.setMealCategories(recipeDTO.mealCategories());

        Boolean featuredFromDto = extractFeaturedFlag(recipeDTO);
        if (featuredFromDto != null) {
            recipe.setFeatured(featuredFromDto);
        }

        if (!imageFile.isEmpty()) {
            Map<String, String> uploadedData = cloudinaryService.upload(imageFile);

            ImageMetaData imageMetaData = new ImageMetaData();
            imageMetaData.setImageUrl(uploadedData.get("url"));
            imageMetaData.setPublicId(uploadedData.get("publicId"));
            recipe.setImage(imageMetaData);
        }
        recipe.setAuthor(userService.findByEmail(email));
    }


    public void deleteById(Long id) {
        recipeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Recipe> findAll() {
        return recipeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findAll(Pageable pageable) {
        return recipeRepository.findAll(pageable);
    }

    public Page<Recipe> filterRecipes(String title, Set<Long> categoryIds,
                                      Set<Long> ingredientIds, Integer preparationTime,
                                      Difficulty difficulty, int page, int size, SourceType sourceType) {
        Specification<Recipe> recipeSpecification = RecipeSpecification.byDifficulty(difficulty).and(
                RecipeSpecification.byPreparationTimeLessThan(preparationTime).and(
                        RecipeSpecification.byTitleContains(title).and(
                                RecipeSpecification.byCategoryIds(categoryIds).and(
                                        RecipeSpecification.byIngredientContains(ingredientIds)))).and(RecipeSpecification.bySourceType(sourceType)));

        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());

        return recipeRepository.findAll(recipeSpecification, pageable);
    }

    @Transactional
    public Recipe saveFromAI(RecipeFromAI recipeFromAI, String email) {
        Recipe recipe = recipeFromAI.toRecipeSkeleton();

        recipe.setFeatured(false);

        if (recipe.getIngredients() == null) {
            recipe.setIngredients(new ArrayList<>());
        } else {
            recipe.getIngredients().clear();
        }

        if (recipeFromAI.getIngredients() != null) {
            for (var line : recipeFromAI.getIngredients()) {
                BigDecimal amt = line.getAmount();
                if (amt == null || amt.compareTo(new BigDecimal("0.001")) < 0) {
                    continue;
                }

                Ingredient ingredient = ingredientResolverService.resolveOrCreate(line.getName());
                Unit unit = ingredientResolverService.ensureUnit(line.getUnitCode(), null);

                RecipeIngredient ri = new RecipeIngredient();
                ri.setRecipe(recipe);
                ri.setIngredient(ingredient);
                ri.setUnit(unit);
                ri.setAmount(amt);

                recipe.getIngredients().add(ri);
            }
        }

        if (recipe.getMealCategories() == null) {
            recipe.setMealCategories(new java.util.HashSet<>());
        }
        MealCategory unknownCat = null;
        try { unknownCat = mealCategoryService.findByName("Unknown"); } catch (RuntimeException ignored) {}

        if (recipeFromAI.getMealCategories() != null) {
            for (String raw : recipeFromAI.getMealCategories()) {
                if (raw == null || raw.isBlank()) continue;
                String name = raw.trim();
                MealCategory cat = null;
                try { cat = mealCategoryService.findByName(name); } catch (RuntimeException ignored) {}
                if (cat != null) recipe.getMealCategories().add(cat);
                else if (unknownCat != null) recipe.getMealCategories().add(unknownCat);
            }
        }

        recipe.setAuthor(userService.findByEmail(email));
        recipe.setSourceType(SourceType.AI);

        calculateNutritionFacts(recipe);

        return recipeRepository.save(recipe);
    }


    @Transactional
    public void calculateNutritionFacts(Recipe recipe) {
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            recipe.setKcalPerServ(null);
            recipe.setProteinPerServ(null);
            recipe.setCarbsPerServ(null);
            recipe.setFatPerServ(null);
            return;
        }
        final Set<String> GRAM_CODES = Set.of("g","gram","grams","gr");

        BigDecimal totalKcal   = BigDecimal.ZERO;
        BigDecimal totalCarbs  = BigDecimal.ZERO;
        BigDecimal totalFat    = BigDecimal.ZERO;
        BigDecimal totalProtein= BigDecimal.ZERO;

        int lines = 0, used = 0, skipped = 0;

        for (RecipeIngredient line : recipe.getIngredients()) {
            lines++;
            if (line == null) { skipped++; continue; }
            if (line.getIngredient() == null || line.getUnit() == null || line.getAmount() == null) {
                skipped++;
                continue;
            }
            if (line.getAmount().compareTo(BigDecimal.ZERO) <= 0) { skipped++; continue; }

            NutritionFact nf = line.getIngredient().getNutritionFact();
            if (nf == null) {
                skipped++;

                continue;
            }

            String code = Optional.ofNullable(line.getUnit().getCode()).orElse("").trim().toLowerCase();

            BigDecimal gramsForLine = null;
            if (GRAM_CODES.contains(code)) {
                gramsForLine = line.getAmount();
            } else {
                IngredientUnitRatio iur = ingredientUnitRatioService.findRatio(line.getIngredient(), line.getUnit());
                if (iur == null || iur.getRatio() == null || iur.getRatio() <= 0) {
                    skipped++;
                    continue;
                }
                gramsForLine = line.getAmount().multiply(BigDecimal.valueOf(iur.getRatio()));
            }

            if (gramsForLine == null || gramsForLine.compareTo(BigDecimal.ZERO) <= 0) {
                skipped++;
                continue;
            }

            BigDecimal factor = gramsForLine.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

            if (nf.getKcal()    != null) totalKcal    = totalKcal   .add(BigDecimal.valueOf(nf.getKcal()   ).multiply(factor));
            if (nf.getProtein() != null) totalProtein = totalProtein.add(BigDecimal.valueOf(nf.getProtein()).multiply(factor));
            if (nf.getCarbs()   != null) totalCarbs   = totalCarbs  .add(BigDecimal.valueOf(nf.getCarbs()  ).multiply(factor));
            if (nf.getFat()     != null) totalFat     = totalFat    .add(BigDecimal.valueOf(nf.getFat()    ).multiply(factor));

            used++;
        }

        BigDecimal servings = (recipe.getServings() == null || recipe.getServings().compareTo(BigDecimal.ZERO) <= 0)
                ? BigDecimal.ONE : recipe.getServings();

        recipe.setKcalPerServ   ( totalKcal   .divide(servings, 2, RoundingMode.HALF_UP) );
        recipe.setProteinPerServ( totalProtein.divide(servings, 2, RoundingMode.HALF_UP) );
        recipe.setCarbsPerServ  ( totalCarbs  .divide(servings, 2, RoundingMode.HALF_UP) );
        recipe.setFatPerServ    ( totalFat    .divide(servings, 2, RoundingMode.HALF_UP) );

        if (used == 0 && lines > 0) {
        } else if (skipped > 0) {
        }
    }

    private static String safeName(Ingredient ing) {
        return ing == null ? "(null)" : Optional.ofNullable(ing.getName()).orElse("(unnamed)");
    }


    private Boolean extractFeaturedFlag(Object dto) {
        try {
            var m = dto.getClass().getMethod("featured");
            Object v = m.invoke(dto);
            if (v instanceof Boolean b) return b;
        } catch (Exception ignored) {}
        try {
            var m = dto.getClass().getMethod("isFeatured");
            Object v = m.invoke(dto);
            if (v instanceof Boolean b) return b;
        } catch (Exception ignored) {}
        try {

            var m = dto.getClass().getMethod("getFeatured");
            Object v = m.invoke(dto);
            if (v instanceof Boolean b) return b;
        } catch (Exception ignored) {}
        return null;
    }

    public List<RecipeCardDTO> getFeaturedForHomepage(int limit) {
        return recipeRepository.featuredOrNewest(Math.max(1, Math.min(limit, 12)))
                .stream()
                .map(this::toCard)
                .toList();
    }

    private RecipeCardDTO toCard(Recipe recipe) {
        String heroImageUrl = (recipe.getImage() != null) ? recipe.getImage().getImageUrl() : null;

        BigDecimal totalCalories = null;
        if (recipe.getKcalPerServ() != null) {
            totalCalories = recipe.getKcalPerServ();
        }

        return new RecipeCardDTO(
                recipe.getId(),
                recipe.getTitle(),
                heroImageUrl,
                recipe.getPreparationTimeMinutes(),
                totalCalories
        );
    }

    @Transactional
    public Recipe createUserRecipe(Long userId, RecipeForm form) {
        var user = userService.findById(userId);
        var r = new Recipe();
        applyForm(r, form);
        r.setSourceType(SourceType.USER);
        r.setAuthor(user);
        r.setFeatured(false);
        return recipeRepository.save(r);
    }

    @Transactional(readOnly = true)
    public Recipe getUserRecipeForEdit(Long userId, Long recipeId) {
        var r = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found"));
        if (r.getAuthor() == null || !r.getAuthor().getId().equals(userId)) {
            throw new SecurityException("Not allowed to edit this recipe");
        }
        return r;
    }

    @Transactional
    public Recipe updateUserRecipe(Long userId, Long recipeId, RecipeForm form) {
        var r = getUserRecipeForEdit(userId, recipeId);
        applyForm(r, form);
        return recipeRepository.save(r);
    }

    private void applyForm(Recipe r, RecipeForm f) {
        r.setTitle(f.title());
        r.setDifficulty(f.difficulty() != null ? f.difficulty() : Difficulty.EASY);
        r.setPreparationTimeMinutes(f.preparationTimeMinutes());
        r.setServings(f.servings());
        r.setInstructions(f.instructions());
        r.setKcalPerServ(f.kcalPerServ());
        r.setProteinPerServ(f.proteinPerServ());
        r.setCarbsPerServ(f.carbsPerServ());
        r.setFatPerServ(f.fatPerServ());

        if (StringUtils.hasText(f.heroImageUrl())) {
            if (r.getImage() == null) r.setImage(new ImageMetaData());
            r.getImage().setImageUrl(f.heroImageUrl());
        } else {
            r.setImage(null);
        }
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findByAuthorAndSourceType(Long authorId, SourceType sourceType, Pageable pageable) {
        if (authorId == null) throw new IllegalArgumentException("authorId is required");
        if (sourceType == null) throw new IllegalArgumentException("sourceType is required");
        return recipeRepository.findByAuthor_IdAndSourceType(authorId, sourceType, pageable);
    }

    public long countAll() {
        return recipeRepository.count();
    }

    public long countBySource(SourceType sourceType) {
        return recipeRepository.countBySourceType(sourceType);
    }

    @Transactional(readOnly = true)
    public Page<Recipe> findBySource(SourceType sourceType, Pageable pageable) {
        if (sourceType == null) {
            return recipeRepository.findAll(pageable);
        }
        return recipeRepository.findBySourceType(sourceType, pageable);
    }
}
