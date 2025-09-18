package org.dci.aimealplanner.controllers.admin;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.SourceType;
import org.dci.aimealplanner.services.admin.AdminDashboardService;
import org.dci.aimealplanner.services.recipes.RecipeService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final UserInformationService userInformationService;
    private final AdminDashboardService adminOverviewService;
    private final RecipeService recipeService;
    private final AdminDashboardService adminDashboardService;


    @GetMapping
    public String adminRoot() {
        return "redirect:/admin/overview";
    }

    @GetMapping("/overview")
    public String overview(Authentication authentication, Model model, HttpServletRequest request) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));

        model.addAttribute("totalUsers", adminOverviewService.totalUsers());
        model.addAttribute("activeUsers", adminOverviewService.activeUsers());
        model.addAttribute("totalRecipes", adminOverviewService.totalRecipes());
        model.addAttribute("totalMealPlans", adminOverviewService.totalMealPlans());
        model.addAttribute("backUrl", request.getHeader("Referer"));

        return "dashboard/admin/overview";
    }

    @GetMapping("/recipes")
    public String adminRecipes(@RequestParam(defaultValue = "ALL") String source,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               Authentication authentication,
                               Model model,
                               HttpServletRequest request) {

        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));

        model.addAttribute("backUrl", request.getHeader("Referer"));

        model.addAttribute("countAll", adminOverviewService.totalRecipes());
        model.addAttribute("countUser", adminOverviewService.countUserRecipes());
        model.addAttribute("countAI", adminOverviewService.countAiRecipes());


        switch (source.toUpperCase()) {
            case "USER" -> model.addAttribute("page", adminOverviewService.findRecipes(SourceType.USER, page, size));
            case "AI" -> model.addAttribute("page", adminOverviewService.findRecipes(SourceType.AI, page, size));
            default -> model.addAttribute("page", adminOverviewService.findRecipes(null, page, size));
        }
        model.addAttribute("source", source.toUpperCase());

        return "dashboard/admin/recipes";
    }


    @PostMapping("/recipes/{id}/delete")
    public String deleteRecipe(@PathVariable Long id,
                               @RequestHeader(value = "referer", required = false) String referer,
                               RedirectAttributes ra) {
        try {
            recipeService.deleteById(id);
            ra.addFlashAttribute("success", "Recipe #" + id + " deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete recipe #" + id + ".");
        }
        return "redirect:" + (referer != null ? referer : "/admin/recipes");
    }

    @GetMapping("/users")
    public String adminUsers(@RequestParam(defaultValue = "ALL") String filter,
                             @RequestParam(defaultValue = "ANY") String provider,
                             @RequestParam(defaultValue = "") String q,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Authentication authentication,
                             Model model,
                             HttpServletRequest request) {

        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));

        model.addAttribute("backUrl", request.getHeader("Referer"));

        model.addAttribute("countAll", adminDashboardService.countAllUsers());
        model.addAttribute("countActive", adminDashboardService.countActiveUsers());
        model.addAttribute("countUnverified", adminDashboardService.countUnverifiedUsers());
        model.addAttribute("countDeleted", adminDashboardService.countDeletedUsers());

        model.addAttribute("page", adminDashboardService.findUsers(filter, provider, q, page, size));

        model.addAttribute("filter", filter.toUpperCase());
        model.addAttribute("provider", provider.toUpperCase());
        model.addAttribute("q", q);
        model.addAttribute("roles", Role.values());

        return "dashboard/admin/users";
    }

    @PostMapping("/users/{id}/verify")
    public String verifyUser(@PathVariable Long id,
                             @RequestHeader(value = "referer", required = false) String referer,
                             RedirectAttributes ra) {
        try {
            adminDashboardService.verifyUser(id);
            ra.addFlashAttribute("success", "User #" + id + " marked verified.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to verify user #" + id + ".");
        }
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @RequestHeader(value = "referer", required = false) String referer,
                             RedirectAttributes ra) {
        try {
            adminDashboardService.softDeleteUser(id);
            ra.addFlashAttribute("success", "User #" + id + " deleted (soft).");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete user #" + id + ".");
        }
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @PostMapping("/users/{id}/restore")
    public String restoreUser(@PathVariable Long id,
                              @RequestHeader(value = "referer", required = false) String referer,
                              RedirectAttributes ra) {
        try {
            adminDashboardService.restoreUser(id);
            ra.addFlashAttribute("success", "User #" + id + " restored.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to restore user #" + id + ".");
        }
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    @PostMapping("/users/{id}/role")
    public String setRole(@PathVariable Long id,
                          @RequestParam Role role,
                          @RequestHeader(value = "referer", required = false) String referer,
                          RedirectAttributes ra) {
        try {
            adminDashboardService.setRole(id, role);
            ra.addFlashAttribute("success", "Role updated for user #" + id + " to " + role + ".");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update role.");
        }
        return "redirect:" + (referer != null ? referer : "/admin/users");
    }

    public Page<org.dci.aimealplanner.entities.recipes.Recipe> findRecipes(
            SourceType sourceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (sourceType == null) {
            return recipeService.findAll(pageable);
        }
        return recipeService.findBySource(sourceType, pageable);
    }


    @GetMapping("/meal-plans")
    public String adminMealPlans(@RequestParam(defaultValue = "") String q,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Authentication authentication,
                                 Model model,
                                 HttpServletRequest request) {

        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        model.addAttribute("backUrl", request.getHeader("Referer"));

        model.addAttribute("totalMealPlans", adminDashboardService.totalMealPlans());
        model.addAttribute("page", adminDashboardService.findMealPlansSimple(q, page, size));
        model.addAttribute("q", q);

        return "dashboard/admin/meal-plans";
    }

    @PostMapping("/meal-plans/{id}/delete")
    public String deleteMealPlan(@PathVariable Long id,
                                 @RequestHeader(value = "referer", required = false) String referer,
                                 RedirectAttributes ra) {
        try {
            adminDashboardService.deleteMealPlanHard(id);
            ra.addFlashAttribute("success", "Meal plan #" + id + " deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete meal plan #" + id + ".");
        }
        return "redirect:" + (referer != null ? referer : "/admin/meal-plans");
    }

    @GetMapping("/meal-plans/{id}")
    public String viewMealPlan(@PathVariable Long id,
                               Authentication authentication,
                               Model model,
                               HttpServletRequest request) {
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));

        var mealPlan = adminDashboardService.findMealPlanById(id);
        model.addAttribute("mealPlan", mealPlan);

        var entriesByDate = adminDashboardService.getEntriesGroupedByDate(mealPlan);
        model.addAttribute("entriesByDate", entriesByDate);

        model.addAttribute("backUrl", request.getHeader("Referer"));

        return "dashboard/admin/meal-plan-view";
    }
}
