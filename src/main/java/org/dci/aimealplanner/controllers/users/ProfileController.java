package org.dci.aimealplanner.controllers.users;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.models.ActivityLevel;
import org.dci.aimealplanner.models.Gender;
import org.dci.aimealplanner.models.Goal;
import org.dci.aimealplanner.services.recipes.DietaryPreferenceService;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserInformationService userInformationService;
    private final DietaryPreferenceService dietaryPreferenceService;
    private final UserService userService;

    @GetMapping
    public String showProfile(Authentication authentication, Model model) {
        String email = AuthUtils.getUserEmail(authentication);
        UserInformation info = userInformationService.getUserInformationForCurrentUser(authentication)
                .orElseGet(UserInformation::new);

        model.addAttribute("userInformation", info);
        prepareFormModel(model, email);
        return "profile/user-profile";
    }

    @PostMapping
    public String saveProfile(Authentication authentication,
                              @Valid @ModelAttribute("userInformation") UserInformation userInformation,
                              BindingResult bindingResult,
                              @RequestParam(value = "dietaryPreferenceIds", required = false) List<Long> dietaryPreferenceIds,
                              Model model) {
        String email = AuthUtils.getUserEmail(authentication);

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, email);
            return "profile/user-profile";
        }

        userInformationService.saveProfile(userInformation, dietaryPreferenceIds, authentication);
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteProfile(@PathVariable Long id, Authentication authentication) {
        userInformationService.deleteProfileIfOwner(id, authentication);
        return "redirect:/dashboard";
    }

    @GetMapping("/skip")
    public String skipProfile() {
        return "redirect:/dashboard";
    }

    private void prepareFormModel(Model model, String userEmail) {
        model.addAttribute("loggedInUser", userService.findByEmail(userEmail));
        model.addAttribute("activityLevels", ActivityLevel.values());
        model.addAttribute("genderList", Gender.values());
        model.addAttribute("goals", Goal.values());
        model.addAttribute("preferences", dietaryPreferenceService.findAll());
    }
}