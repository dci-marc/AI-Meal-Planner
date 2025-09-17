package org.dci.aimealplanner.controllers.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.models.users.UserBasicDTO;
import org.dci.aimealplanner.services.planning.PlanningSnapshotService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final UserInformationService userInformationService;
    private final PlanningSnapshotService planningSnapshotService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Authentication authentication, Model model) {
        UserBasicDTO userBasicDTO = userInformationService.getUserBasicDTO(authentication);
        model.addAttribute("loggedInUser", userBasicDTO);
        return "dashboard/dashboard";
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public String profile() {
        return "redirect:/profile";
    }

    @GetMapping("/recipes")
    @PreAuthorize("isAuthenticated()")
    public String recipesHub(Authentication authentication, Model model) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        return "dashboard/recipes/recipe_hub";
    }

    @GetMapping("/meals")
    @PreAuthorize("isAuthenticated()")
    public String mealsHub(Authentication authentication, Model model) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        return "dashboard/planning/planning_hub";
    }

    @GetMapping("/meals/upcoming")
    @PreAuthorize("isAuthenticated()")
    public String upcomingMeals(Authentication authentication, Model model) {
        UserBasicDTO user = userInformationService.getUserBasicDTO(authentication);
        model.addAttribute("loggedInUser", user);

        var nextMeals = planningSnapshotService.nextMeals(user.email(), 10);
        model.addAttribute("nextMeals", nextMeals);

        return "dashboard/planning/upcoming_meals";
    }

}
