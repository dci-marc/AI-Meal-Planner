package org.dci.aimealplanner.controllers.planning;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.planning.MealPlan;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.models.planning.AddMealEntryDTO;
import org.dci.aimealplanner.models.planning.CreateMealPlanDTO;
import org.dci.aimealplanner.models.recipes.MealSlot;
import org.dci.aimealplanner.services.planning.MealPlanAiService;
import org.dci.aimealplanner.services.planning.MealPlanningService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/meal-plans")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanningService mealPlanningService;
    private final UserService userService;
    private final UserInformationService userInformationService;
    private final MealPlanAiService mealPlanAiService;

    @GetMapping
    public String list(Model model, Authentication authentication) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);
        List<MealPlan> plans = mealPlanningService.listPlansForUser(loggedUser.getId());
        model.addAttribute("plans", plans);
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(loggedUser));
        return "planning/mealplans_list";
    }

    @GetMapping("/new")
    public String newMealPlan(Authentication authentication, Model model, HttpServletRequest request) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);

        CreateMealPlanDTO dto = new CreateMealPlanDTO(
                "My Plan",
                LocalDate.now(),
                LocalDate.now().plusDays(6),
                null
        );
        model.addAttribute("mealPlan", dto);
        model.addAttribute("loggedInUser", loggedUser);
        model.addAttribute("redirectUrl", request.getHeader("Referer"));
        model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(loggedUser));
        return "planning/mealplan_form";
    }

    @PostMapping("/create")
    public String createMealPlan(@Valid @ModelAttribute("mealPlan") CreateMealPlanDTO createMealPlanDTO,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                 Model model,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);

        if (bindingResult.hasErrors()) {
            model.addAttribute("mealPlan", createMealPlanDTO);
            model.addAttribute("loggedInUser", loggedUser);
            model.addAttribute("redirectUrl", redirectUrl);
            model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(loggedUser));
            return "planning/mealplan_form";
        }
        MealPlan plan = mealPlanningService.createPlan(loggedUser.getId(), createMealPlanDTO);
        ra.addFlashAttribute("success", "Meal plan created successfully.");
        String target = resolveRedirectUrl(redirectUrl, request, "/meal-plans");
        return "redirect:" + target + "/" + plan.getId();
    }

    @PostMapping("/{id}/ai-populate")
    public String aiPopulate(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);
        try {
            mealPlanAiService.populatePlanWithAi(id, loggedUser);
            ra.addFlashAttribute("success", "Plan populated with AI-generated recipes.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/meal-plans/" + id;
    }

    @GetMapping("/{id}")
    public String showMealPlanDetail(@PathVariable Long id,
                                     Authentication authentication,
                                     HttpServletRequest request,
                                     Model model,
                                     RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);

        try {
            MealPlan plan = mealPlanningService.getPlanForUser(id, loggedUser.getId());
            Map<LocalDate, ?> entriesByDate = mealPlanningService.getEntriesGroupedByDateForUser(id, loggedUser.getId());

            model.addAttribute("mealPlan", plan);
            model.addAttribute("entriesByDate", entriesByDate);
            model.addAttribute("mealSlots", MealSlot.values());
            model.addAttribute("addEntry", new AddMealEntryDTO(
                    plan.getId(), plan.getStartDate(), MealSlot.BREAKFAST, null, null
            ));
            model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(loggedUser));
            model.addAttribute("redirectUrl", request.getHeader("Referer"));
            return "planning/mealplan_detail";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/meal-plans";
        }
    }

    @PostMapping("/{id}/entries")
    public String addEntry(@PathVariable Long id,
                           @ModelAttribute AddMealEntryDTO addEntryDTO,
                           Authentication authentication,
                           RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);
        try {
            mealPlanningService.addEntry(loggedUser.getId(), addEntryDTO);
            ra.addFlashAttribute("success", "Meal entry added.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/meal-plans/" + id;
    }

    @GetMapping("/delete/{id}")
    public String deleteMealPlan(@PathVariable Long id,
                                 Authentication authentication,
                                 RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);
        try {
            mealPlanningService.deletePlan(id, loggedUser.getId());
            ra.addFlashAttribute("success", "Meal plan deleted.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/meal-plans";
    }

    @PostMapping("/entries/{entryId}/delete")
    public String deleteEntry(@PathVariable Long entryId,
                              @RequestParam("planId") Long planId,
                              Authentication authentication,
                              RedirectAttributes ra) {
        String email = AuthUtils.getUserEmail(authentication);
        User loggedUser = userService.findByEmail(email);
        try {
            mealPlanningService.removeEntry(entryId, loggedUser.getId());
            ra.addFlashAttribute("success", "Meal entry removed.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/meal-plans/" + planId;
    }

    private String resolveRedirectUrl(String candidate, HttpServletRequest request, String fallback) {
        String safeFallback = (fallback == null || fallback.isBlank()) ? "/meal-plans" : fallback;
        if (candidate == null || candidate.isBlank()) return safeFallback;
        if (candidate.startsWith("/")) return candidate;
        return safeFallback;
    }
}
