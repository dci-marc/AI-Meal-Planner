package org.dci.aimealplanner.controllers.recipes;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/user")
@RequiredArgsConstructor
public class MyRecipesApiController {

    private final RecipeService recipeService;
    private final UserService userService;

    @GetMapping("/my-recipes")
    @PreAuthorize("isAuthenticated()")
    public Page<RecipeCardDTO> myRecipes(Authentication authentication,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "12") int size) {
        var user = userService.findByEmail(AuthUtils.getUserEmail(authentication));
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return recipeService.findByAuthorAndSourceType(user.getId(), SourceType.USER, pageable)
                .map(this::toCard);
    }

    @GetMapping("/ai-recipes")
    @PreAuthorize("isAuthenticated()")
    public Page<RecipeCardDTO> myAiRecipes(Authentication authentication,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "12") int size) {
        var user = userService.findByEmail(AuthUtils.getUserEmail(authentication));
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return recipeService.findByAuthorAndSourceType(user.getId(), SourceType.AI, pageable)
                .map(this::toCard);
    }

    private RecipeCardDTO toCard(Recipe r) {
        return new RecipeCardDTO(
                r.getId(),
                r.getTitle(),
                r.getImage() != null ? r.getImage().getImageUrl() : null,
                r.getPreparationTimeMinutes(),
                r.getKcalPerServ()
        );
    }
}


