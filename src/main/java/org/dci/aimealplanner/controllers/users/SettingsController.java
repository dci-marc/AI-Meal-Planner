package org.dci.aimealplanner.controllers.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.models.UserType;
import org.dci.aimealplanner.models.users.UserBasicDTO;
import org.dci.aimealplanner.services.users.UserInformationService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SettingsController {
    private final UserInformationService userInformationService;
    private final UserService userService;

    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public String settings(Authentication authentication, Model model) {
        UserBasicDTO currentUser = userInformationService.getUserBasicDTO(authentication);

        boolean isLocalUser = currentUser.userType() == UserType.LOCAL;
        boolean isGoogleUser = currentUser.userType() == UserType.GOOGLE;

        model.addAttribute("isLocalUser", isLocalUser);
        model.addAttribute("isGoogleUser", isGoogleUser);
        return "dashboard/users/account-settings";
    }

    @PostMapping("/settings/change-email")
    @PreAuthorize("isAuthenticated()")
    public String changeEmail(Authentication authentication,
                              @RequestParam String newEmail,
                              @RequestParam String password) {
        UserBasicDTO currentUser = userInformationService.getUserBasicDTO(authentication);
        if (currentUser.userType() != UserType.LOCAL) {
            return "redirect:/settings?error=" + url("Email changes are managed by Google for this account.");
        }
        try {
            userService.changeEmail(currentUser.id(), password, newEmail);
            return "redirect:/settings?emailChanged=1";
        } catch (Exception ex) {
            return "redirect:/settings?error=" + url(ex.getMessage());
        }
    }

    @PostMapping("/settings/change-password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/settings?error=" + url("Passwords do not match");
        }
        UserBasicDTO currentUser = userInformationService.getUserBasicDTO(authentication);
        if (currentUser.userType() != UserType.LOCAL) {
            return "redirect:/settings?error=" + url("Password changes are managed by Google for this account.");
        }
        try {
            userService.changePassword(currentUser.id(), currentPassword, newPassword);
            return "redirect:/settings?passwordChanged=1";
        } catch (Exception ex) {
            return "redirect:/settings?error=" + url(ex.getMessage());
        }
    }

    @PostMapping("/settings/set-local-password")
    @PreAuthorize("isAuthenticated()")
    public String setLocalPassword(Authentication authentication,
                                   @RequestParam String newPassword,
                                   @RequestParam String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/settings?error=" + url("Passwords do not match");
        }
        UserBasicDTO currentUser = userInformationService.getUserBasicDTO(authentication);
        try {
            if (currentUser.userType() != UserType.GOOGLE) {
                return "redirect:/settings?error=" + url("This action is only for Google-linked accounts.");
            }
            userService.setLocalPassword(currentUser.id(), newPassword);
            userService.switchUserType(currentUser.id(), UserType.LOCAL);

            return "redirect:/settings?localPasswordSet=1";
        } catch (Exception ex) {
            return "redirect:/settings?error=" + url(ex.getMessage());
        }
    }

    @PostMapping("/settings/unlink-google")
    @PreAuthorize("isAuthenticated()")
    public String unlinkGoogle(Authentication authentication) {
        UserBasicDTO currentUser = userInformationService.getUserBasicDTO(authentication);
        try {
            userService.unlinkGoogle(currentUser.id());
            userService.switchUserType(currentUser.id(), UserType.LOCAL);

            return "redirect:/settings?googleUnlinked=1";
        } catch (Exception ex) {
            return "redirect:/settings?error=" + url(ex.getMessage());
        }
    }

    private static String url(String s) {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
