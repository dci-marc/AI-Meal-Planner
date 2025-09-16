package org.dci.aimealplanner.controllers.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.exceptions.EmailAlreadyTaken;
import org.dci.aimealplanner.exceptions.PasswordInvalid;
import org.dci.aimealplanner.exceptions.VerificationTokenInvalid;
import org.dci.aimealplanner.services.users.PasswordResetService;
import org.dci.aimealplanner.services.users.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login(){
        return "auth/login";
    }

    @GetMapping("/signup")
    public String register(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult bindingResult,
                           Model model) {
        List<String> errors = new ArrayList<>();

        try {
            userService.checkEmailAvailability(user.getEmail());
            userService.checkPasswordValidity(user.getPassword());
        } catch (EmailAlreadyTaken | PasswordInvalid ex) {
            errors.add(ex.getMessage());
        }

        if (!errors.isEmpty() || bindingResult.hasErrors()) {
            model.addAttribute("errors", errors);
            model.addAttribute("user", user);
            return "auth/register";
        }

        User addedUser = userService.create(user);

        userService.sendVerificationToken(addedUser);

        return "redirect:/auth/login?registered";

    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam("token") String token, Model model) {
       try {
           userService.verifyToken(token);
           return "redirect:/auth/login?verified";
       } catch (VerificationTokenInvalid ex){
            return "redirect:/auth/login?invalidToken";
        }
    }

    @GetMapping("/forgot-password")
    public  String forgotPassword() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam("email") String email,
                               RedirectAttributes redirectAttributes) {
        passwordResetService.requestReset(email);
        redirectAttributes.addFlashAttribute("msg",
                "If an account exists with that email, a reset link has been sent.");
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password/{token}")
    public String resetPasswordForm(@PathVariable String token, Model model) {
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password/{token}")
    public String doReset(@PathVariable String token,
                          @RequestParam("password") String password,
                          @RequestParam("confirm") String confirm,
                          RedirectAttributes redirectAttributes) {
        if (!password.equals(confirm)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/auth/reset-password/" + token;
        }
        try {
            passwordResetService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("msg", "Your password has been updated. Please log in.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "The reset link is invalid or expired.");
            return "redirect:/auth/forgot-password";
        }
    }
}
