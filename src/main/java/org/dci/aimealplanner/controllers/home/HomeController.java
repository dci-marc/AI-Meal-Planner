package org.dci.aimealplanner.controllers.home;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.models.recipes.RecipeCardDTO;
import org.dci.aimealplanner.services.planning.PlanningSnapshotService;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final UserInformationService  userInformationService;
    private final RecipeService recipeService;
    private final PlanningSnapshotService  planningSnapshotService;

    @GetMapping({"/", "/index"})
    public String index(Authentication authentication, Model model) {
        if (authentication != null) {
            String email = AuthUtils.getUserEmail(authentication);
            model.addAttribute("nextMeals", planningSnapshotService.nextMeals(email, 2));
        }
        List<RecipeCardDTO> featured = recipeService.getFeaturedForHomepage(3);
        model.addAttribute("featuredRecipes", featured);
        return "home/index";
    }

    @GetMapping("/account")
    public String account() {
        return "home/user_dashboard";
    }
}
