package org.dci.aimealplanner.controllers.api;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.models.recipes.SimpleRecipeDTO;
import org.dci.aimealplanner.services.recipes.RecipeSearchService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeApiController {
    private final RecipeSearchService recipeSearchService;
    private final UserService userService;

    @GetMapping("/planner-search")
    public List<SimpleRecipeDTO> plannerSearch(@RequestParam String q,
                                               @RequestParam(defaultValue = "20") int size,
                                               Authentication authentication) {
        String email = AuthUtils.getUserEmail(authentication);
        var user = userService.findByEmail(email);

        return recipeSearchService.searchForPlanner(q, user.getId(), size)
                .map(SimpleRecipeDTO::from)
                .getContent();
    }
}
