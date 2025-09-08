package org.dci.aimealplanner.controllers.recipes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.Difficulty;
import org.dci.aimealplanner.models.recipes.RecipeDTO;
import org.dci.aimealplanner.models.recipes.UpdateRecipeDTO;
import org.dci.aimealplanner.services.ingredients.IngredientCategoryService;
import org.dci.aimealplanner.services.ingredients.IngredientService;
import org.dci.aimealplanner.services.ingredients.IngredientUnitRatioService;
import org.dci.aimealplanner.services.ingredients.UnitService;
import org.dci.aimealplanner.services.recipes.MealCategoryService;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserService;
import org.dci.aimealplanner.services.utils.PdfService;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Controller
@RequiredArgsConstructor
@RequestMapping("/recipes")
public class RecipeController {
    private final RecipeService recipeService;
    private final MealCategoryService mealCategoryService;
    private final IngredientService ingredientService;
    private final UnitService  unitService;
    private final IngredientUnitRatioService  ingredientUnitRatioService;
    private final IngredientCategoryService ingredientCategoryService;
    private final UserService userService;
    private final PdfService pdfService;

    @GetMapping
    public String showRecipes(@RequestParam(required = false) String title,
                              @RequestParam(required = false) Integer preparationTime,
                              @RequestParam(required = false) Difficulty difficulty,
                              @RequestParam(required = false) Set<Long> ingredientIds,
                              @RequestParam(required = false) Set<Long> categoryIds,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "6") int size,
                              Authentication authentication,
                              Model model) {
        Page<Recipe> recipesPage = recipeService.filterRecipes(title, categoryIds,
                ingredientIds, preparationTime, difficulty, page, size);

        model.addAttribute("recipesPage", recipesPage);
        model.addAttribute("currentPage",page);
        model.addAttribute("hasPrevious",recipesPage.hasPrevious());
        model.addAttribute("hasNext",recipesPage.hasNext());
        model.addAttribute("size",size);

        if (title != null && !title.isBlank()) {
            model.addAttribute("title", title);
        }

        if (ingredientIds != null && !ingredientIds.isEmpty()) {
            model.addAttribute("ingredientIds", ingredientIds);
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            model.addAttribute("categoryIds", categoryIds);
        }

        if (preparationTime != null && preparationTime > 0) {
            model.addAttribute("preparationTime", preparationTime);
        }

        if (difficulty != null) {
            model.addAttribute("difficulty", difficulty);
        }

        return "recipes/recipes_list";
    }

    @GetMapping("/new")
    public String newRecipe(Authentication authentication,
                            Model model,
                            HttpServletRequest request) {
        String email = AuthUtils.getUserEmail(authentication);
        Recipe recipe = new Recipe();
        recipe.setIngredients(new ArrayList<>());
        recipe.setMealCategories(new HashSet<>());
        model.addAttribute("recipe", RecipeDTO.from(recipe));
        prepareFormModel(model, email, request.getHeader("Referer"));
        return "recipes/recipe_form";
    }

    @PostMapping("/create")
    public String createRecipe(@Valid @ModelAttribute("recipe") UpdateRecipeDTO updateRecipeDTO,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Authentication authentication,
                               @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                               Model model) {
        String email = AuthUtils.getUserEmail(authentication);

        if (bindingResult.hasErrors()) {
            model.addAttribute("recipe", updateRecipeDTO);
            prepareFormModel(model, email, redirectUrl);
            return "recipes/recipe_form";
        }

        recipeService.addNewRecipe(updateRecipeDTO, imageFile, email);
        return "redirect:/home/index";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id,
                               Authentication authentication,
                               HttpServletRequest request,
                               Model model) {
        String email = AuthUtils.getUserEmail(authentication);
        Recipe recipe = recipeService.findById(id);
        RecipeDTO recipeDTO = RecipeDTO.from(recipe);
        model.addAttribute("recipe", recipeDTO);

        prepareFormModel(model, email, request.getHeader("Referer"));

        return "recipes/recipe_form";
    }

    @PostMapping("/update/{id}")
    public String updateRecipe(@PathVariable Long id, @Valid @ModelAttribute UpdateRecipeDTO updateRecipeDTO,
                               BindingResult bindingResult,
                               @RequestParam(required = false) MultipartFile imageFile,
                               Authentication authentication,
                               @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                               Model model) {
        String email = AuthUtils.getUserEmail(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("recipe", updateRecipeDTO);
            prepareFormModel(model, email, redirectUrl);
            return "recipes/recipe_form";
        }

        Recipe updated = recipeService.updateRecipe(id, updateRecipeDTO, imageFile, email);
        return "redirect:/home/index";
    }

    @GetMapping("/{id}")
    public String showRecipeDetail(@PathVariable Long id,
                                   Authentication authentication,
                                   HttpServletRequest request,
                                   Model model) {
        Recipe recipe = recipeService.findById(id);
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);

        if (recipe.getAuthor() != null) {
            model.addAttribute("author", recipe.getAuthor());
        }

        model.addAttribute("recipe", RecipeDTO.from(recipe));
        model.addAttribute("loggedInUser", loggedUser);
        model.addAttribute("currentUserId", loggedUser.getId());
        model.addAttribute("redirectUrl", request.getHeader("Referer"));

        return "recipes/recipe-details";
    }

    @GetMapping("/delete/{id}")
    public String deleteRecipe(@PathVariable Long id, Authentication authentication){
        String email = AuthUtils.getUserEmail(authentication);
        Recipe recipe = recipeService.findById(id);
        if (recipe.getAuthor() != null && recipe.getAuthor().getId().equals(userService.findByEmail(email).getId())) {
            recipeService.deleteById(id);
        }
        return "redirect:/recipes/my-recipes";
    }

    @GetMapping(value = "/generate/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        Recipe recipe = recipeService.findById(id);
        User author = recipe.getAuthor();
        byte[] pdfBytes = pdfService.generatePdf(recipe, author);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.builder("attachment").filename(recipe.getTitle() + ".pdf")
                        .build());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


    private void prepareFormModel(Model model,
                                  String userEmail,
                                  String redirectUrl) {
        model.addAttribute("loggedInUser", userService.findByEmail(userEmail));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("categories", mealCategoryService.findAll());
        model.addAttribute("ingredientList", ingredientService.findAll());
        model.addAttribute("unitList", unitService.findAll());
        model.addAttribute("ingredientCategories", ingredientCategoryService.findAll());
        model.addAttribute("redirectUrl", redirectUrl);
    }




}
