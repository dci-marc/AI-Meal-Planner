package org.dci.aimealplanner.controllers.api;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeApiController {
    private final RecipeService recipeService;

    @GetMapping("/featured")
    public List<RecipeCardDTO> featured(@RequestParam(defaultValue = "6") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        return recipeService.getFeaturedForHomepage(safeLimit);
    }
}
