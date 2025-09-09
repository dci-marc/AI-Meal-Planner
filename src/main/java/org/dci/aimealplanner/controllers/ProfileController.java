package org.dci.aimealplanner.controllers;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserInformationService userInformationService;


    @GetMapping
    public String showProfile(@AuthenticationPrincipal User user,Model model) {
        userInformationService.getUserInformationForCurrentUser()
                        .ifPresentOrElse(info -> model.addAttribute("userInformation", info),
                                () -> model.addAttribute("userInformation", new UserInformation()));

        model.addAttribute("userInformation", new UserInformation());
        return "profile/user-profile";
    }

    @PostMapping
    public String saveProfile(UserInformation userInformation) {
        userInformationService.saveProfile(userInformation);
        return "redirect:/dashboard";
    }

    @DeleteMapping("/{id}")
    public String deleteProfile(@PathVariable Long id) {
       userInformationService.deleteProfileIfOwner(id);
        return "redirect:/dashboard";
    }

    @GetMapping("/skip")
    public String skipProfile() {
        return "redirect:/dashboard";
    }

    public String editProfile(Model model) {

        return userInformationService.getUserInformationForCurrentUser()
                .map(info -> {
                    model.addAttribute("userInformation", info);
                    return "profile/edit-profile";
                })
                .orElse("redirect:/profile");
    }
}