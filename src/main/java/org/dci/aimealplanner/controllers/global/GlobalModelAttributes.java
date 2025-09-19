package org.dci.aimealplanner.controllers.global;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
@Component
public class GlobalModelAttributes {
    private final UserInformationService userInformationService;

    @ModelAttribute
    public void addLoggedInUser(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("loggedInUser", userInformationService.getUserBasicDTO(authentication));
        }
    }
}