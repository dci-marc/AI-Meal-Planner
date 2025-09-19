package org.dci.aimealplanner.controllers.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.Difficulty;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.models.recipes.RecipeForm;
import org.dci.aimealplanner.models.users.UserBasicDTO;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/recipes")
@RequiredArgsConstructor
public class MyRecipesController {

    private final UserInformationService userInformationService;
    private final UserService userService;
    private final RecipeService recipeService;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public String myRecipesPage(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Authentication authentication,
                                Model model) {
        return renderList(SourceType.USER, "My Recipes", "/dashboard/recipes/my",
                page, size, authentication, model);
    }

    @GetMapping("/ai")
    @PreAuthorize("isAuthenticated()")
    public String aiRecipesPage(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "12") int size,
                                Authentication authentication,
                                Model model) {
        return renderList(SourceType.AI, "AI Recipes for Me", "/dashboard/recipes/ai",
                page, size, authentication, model);
    }

    private String renderList(SourceType sourceType,
                              String title,
                              String basePath,
                              int page, int size,
                              Authentication authentication,
                              Model model) {
        UserBasicDTO current = userInformationService.getUserBasicDTO(authentication);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Recipe> recipesPage = recipeService.findByAuthorAndSourceType
                (current.id(), sourceType, pageable);

        model.addAttribute("title", title);
        model.addAttribute("scope", sourceType == SourceType.USER ? "my" : "ai");
        model.addAttribute("basePath", basePath);
        model.addAttribute("page", recipesPage);
        return "dashboard/recipes/my";
    }

    @GetMapping("/new")
    @PreAuthorize("isAuthenticated()")
    public String newRecipeForm(Model model) {
        model.addAttribute("form", RecipeForm.blank());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("mode", "create");
        return "dashboard/recipes/user_recipe_form";
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String createRecipe(Authentication authentication,
                               @Valid @ModelAttribute("form") RecipeForm form,
                               BindingResult br,
                               Model model,
                               RedirectAttributes ra) {
        if (br.hasErrors()) {
            model.addAttribute("difficulties", Difficulty.values());
            model.addAttribute("mode", "create");
            return "dashboard/recipes/user_recipe_form";
        }
        var user = userService.findByEmail(AuthUtils.getUserEmail(authentication));
        recipeService.createUserRecipe(user.getId(), form);
        ra.addFlashAttribute("saved", true);
        return "redirect:/recipes/my";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editRecipeForm(@PathVariable Long id,
                                 Authentication authentication,
                                 Model model) {
        var current = userInformationService.getUserBasicDTO(authentication);
        var entity = recipeService.getUserRecipeForEdit(current.id(), id);
        model.addAttribute("form", RecipeForm.from(entity));
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("mode", "edit");
        model.addAttribute("recipeId", id);
        return "redirect:/recipes/edit/" + id;
    }

    @PostMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public String updateRecipe(@PathVariable Long id,
                               Authentication authentication,
                               @Valid @ModelAttribute("form") RecipeForm form,
                               BindingResult br,
                               Model model,
                               RedirectAttributes ra) {
        var current = userInformationService.getUserBasicDTO(authentication);
        if (br.hasErrors()) {
            model.addAttribute("difficulties", Difficulty.values());
            model.addAttribute("mode", "edit");
            model.addAttribute("recipeId", id);
            return "dashboard/recipes/user_recipe_form";
        }
        recipeService.updateUserRecipe(current.id(), id, form);
        ra.addFlashAttribute("updated", true);
        return "redirect:/recipes/my";
    }
}