package org.dci.aimealplanner.controllers.recipes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.recipes.RecipeFromAI;
import org.dci.aimealplanner.models.Difficulty;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.models.recipes.RecipeDTO;
import org.dci.aimealplanner.models.recipes.RecipeViewDTO;
import org.dci.aimealplanner.models.recipes.UpdateRecipeDTO;
import org.dci.aimealplanner.models.users.UserBasicDTO;
import org.dci.aimealplanner.services.ingredients.IngredientCategoryService;
import org.dci.aimealplanner.services.recipes.MealCategoryService;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserInformationService;
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

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recipes")
public class RecipeController {
    private final RecipeService recipeService;
    private final MealCategoryService mealCategoryService;
    private final IngredientCategoryService ingredientCategoryService;
    private final UserService userService;
    private final UserInformationService userInformationService;
    private final PdfService pdfService;
    private final GroqApiClient groqApiClient;

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
        Page<Recipe> recipesPage = recipeService.filterRecipes(title,
                categoryIds, ingredientIds,
                preparationTime, difficulty,
                page, size, SourceType.USER);

        Set<Long> authorIds = recipesPage.getContent().stream()
                .map(r -> r.getAuthor().getId())
                .collect(Collectors.toSet());

        List<UserInformation> infos = userInformationService.userInformationByIdIn(authorIds);

        Map<Long, UserBasicDTO> authors = infos.stream()
                .collect(Collectors.toMap(
                        ui -> ui.getUser().getId(),
                        ui -> UserBasicDTO.from(ui.getUser(), ui)
                ));

        if (authentication != null) {
            model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        }

        model.addAttribute("authorsMap", authors);
        model.addAttribute("recipesPage", recipesPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("hasPrevious", recipesPage.hasPrevious());
        model.addAttribute("hasNext", recipesPage.hasNext());
        model.addAttribute("size", size);
        model.addAttribute("categories", mealCategoryService.findAll());
        model.addAttribute("difficulties", Difficulty.values());
        if (title != null && !title.isBlank()) model.addAttribute("title", title);
        if (ingredientIds != null && !ingredientIds.isEmpty()) model.addAttribute("ingredientIds", ingredientIds);
        if (categoryIds != null && !categoryIds.isEmpty()) model.addAttribute("categoryIds", categoryIds);
        if (preparationTime != null && preparationTime > 0) model.addAttribute("preparationTime", preparationTime);
        if (difficulty != null) model.addAttribute("difficulty", difficulty);
        return "recipes/recipes_list";
    }

    @GetMapping("/new")
    public String newRecipe(Authentication authentication, Model model, HttpServletRequest request) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        Recipe recipe = new Recipe();
        recipe.setIngredients(new ArrayList<>());
        recipe.setMealCategories(new HashSet<>());
        model.addAttribute("recipe", RecipeDTO.from(recipe));
        prepareFormModel(model, authentication, request.getHeader("Referer"));
        return "recipes/recipe_form";
    }

    @PostMapping("/create")
    public String createRecipe(@Valid @ModelAttribute("recipe") UpdateRecipeDTO updateRecipeDTO,
                               BindingResult bindingResult,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               Authentication authentication,
                               @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                               Model model,
                               HttpServletRequest request) {
        String email = AuthUtils.getUserEmail(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("recipe", updateRecipeDTO);
            prepareFormModel(model, authentication, redirectUrl);
            return "recipes/recipe_form";
        }
        recipeService.addNewRecipe(updateRecipeDTO, imageFile, email);
        String target = resolveRedirectUrl(redirectUrl, request, "/recipes");
        return "redirect:" + target;
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
        prepareFormModel(model, authentication, request.getHeader("Referer"));
        return "recipes/recipe_form";
    }

    @PostMapping("/update/{id}")
    public String updateRecipe(@PathVariable Long id,
                               @Valid @ModelAttribute UpdateRecipeDTO updateRecipeDTO,
                               BindingResult bindingResult,
                               @RequestParam(required = false) MultipartFile imageFile,
                               Authentication authentication,
                               @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                               Model model,
                               HttpServletRequest request) {
        String email = AuthUtils.getUserEmail(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("recipe", updateRecipeDTO);
            prepareFormModel(model, authentication, redirectUrl);
            return "recipes/recipe_form";
        }
        recipeService.updateRecipe(id, updateRecipeDTO, imageFile, email);
        String target = resolveRedirectUrl(redirectUrl, request, "/recipes");
        return "redirect:" + target;
    }

    @GetMapping("/{id}")
    public String showRecipeDetail(@PathVariable Long id,
                                   Authentication authentication,
                                   HttpServletRequest request,
                                   Model model) {
        Recipe recipe = recipeService.findById(id);
        UserBasicDTO authorDTO = userInformationService.getUserBasicDTO(recipe.getAuthor());
        UserBasicDTO loggedUser =  userInformationService.getUserBasicDTO(authentication);
        if (recipe.getAuthor() != null) {
            model.addAttribute("author", authorDTO);
        }
        model.addAttribute("recipe", RecipeViewDTO.from(recipe));
        model.addAttribute("loggedInUser", loggedUser);
        model.addAttribute("currentUserId", loggedUser.id());
        model.addAttribute("redirectUrl", request.getHeader("Referer"));
        return "recipes/recipe-details";
    }

    @GetMapping("/delete/{id}")
    public String deleteRecipe(@PathVariable Long id, Authentication authentication) {
        String email = AuthUtils.getUserEmail(authentication);
        Recipe recipe = recipeService.findById(id);
        if (recipe.getAuthor() != null && recipe.getAuthor().getId().equals(userService.findByEmail(email).getId())) {
            recipeService.deleteById(id);
        }
        return "redirect:/recipes";
    }

    @GetMapping(value = "/generate-pdf/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        Recipe recipe = recipeService.findById(id);
        User author = recipe.getAuthor();
        UserBasicDTO authorDTO = userInformationService.getUserBasicDTO(author);
        byte[] pdfBytes = pdfService.generatePdf(recipe, authorDTO);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(recipe.getTitle() + ".pdf").build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    @GetMapping("/ask-ai")
    public String askAi(Authentication authentication, Model model) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        return "recipes/ask_ai";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam String prompt, Authentication authentication, Model model) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        try {
            RecipeFromAI aiRecipe = groqApiClient.generateRecipeFromPrompt(prompt);
            model.addAttribute("aiRecipe", aiRecipe);
            return "recipes/generate";
        } catch (Exception e) {
            model.addAttribute("error", "Sorry, I couldn't generate a recipe. " + e.getMessage());
            return "recipes/ask_ai";
        }
    }

    private void prepareFormModel(Model model, Authentication authentication, String redirectUrl) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("categories", mealCategoryService.findAll());
        model.addAttribute("ingredientCategories", ingredientCategoryService.findAll());
        model.addAttribute("redirectUrl", (redirectUrl == null || redirectUrl.isBlank()) ? "/recipes" : redirectUrl);
    }

    @PostMapping("/save-ai")
    public String saveAi(@ModelAttribute RecipeFromAI aiRecipe, Authentication authentication) {
        String email = AuthUtils.getUserEmail(authentication);
        Recipe recipe = recipeService.saveFromAI(aiRecipe, email);
        return "redirect:/recipes/" + recipe.getId();
    }

    private String resolveRedirectUrl(String candidate, HttpServletRequest request, String fallback) {
        String safeFallback = (fallback == null || fallback.isBlank()) ? "/recipes" : fallback;
        if (candidate == null || candidate.isBlank()) return safeFallback;
        if (candidate.startsWith("/")) return candidate;
        return safeFallback;
    }
}
