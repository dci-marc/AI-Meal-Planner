package org.dci.aimealplanner.controllers.ingredients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientFromAI;
import org.dci.aimealplanner.services.ingredients.IngredientAiService;
import org.dci.aimealplanner.services.ingredients.IngredientService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@Controller
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;
    private final IngredientAiService ingredientAiService;
    private final ObjectMapper objectMapper;

    @GetMapping("/new-ai")
    public String newIngredientAi(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "return", required = false) String returnUrl,
            HttpServletRequest request,
            Model model
    ) {
        String fallback = "/";
        String resolvedReturn = resolveReturnUrl(returnUrl, request, fallback);

        String userInput = name != null ? name.trim() : "";
        if (!StringUtils.hasText(userInput)) {
            model.addAttribute("returnUrl", resolvedReturn);
            model.addAttribute("error", "Missing ingredient name.");
            return "ingredients/new-ai-preview";
        }

        if (ingredientService.existsByNameIgnoreCase(userInput)) {
            ingredientService.findByNameIgnoreCase(userInput).ifPresent(existing -> {
                model.addAttribute("existingId", existing.getId());
                model.addAttribute("existingName", existing.getName());
            });
            model.addAttribute("name", userInput);
            model.addAttribute("returnUrl", resolvedReturn);
            model.addAttribute("dup", true);
            return "ingredients/new-ai-preview";
        }

        try {
            IngredientFromAI ai = ingredientAiService.previewFromAi(userInput);
            String aiJson = objectMapper.writeValueAsString(ai);

            model.addAttribute("name", userInput);
            model.addAttribute("returnUrl", resolvedReturn);
            model.addAttribute("ai", ai);
            model.addAttribute("aiJson", aiJson);
            return "ingredients/new-ai-preview";
        } catch (Exception ex) {
            model.addAttribute("name", userInput);
            model.addAttribute("returnUrl", resolvedReturn);
            model.addAttribute("error", "Couldn’t fetch ingredient details from AI. Please try again.");
            return "ingredients/new-ai-preview";
        }
    }

    @PostMapping("/new-ai/save")
    @Transactional
    public String saveIngredientAi(
            @RequestParam("payload") String payload,
            @RequestParam("returnUrl") String returnUrl,
            HttpServletRequest request,
            RedirectAttributes ra
    ) throws JsonProcessingException {

        String fallback = "/";
        String resolvedReturn = resolveReturnUrl(returnUrl, request, fallback);

        try {
            var ingredient = ingredientAiService.saveFromAiPayload(payload);
            ra.addFlashAttribute("success", "Ingredient \"" + ingredient.getName() + "\" added.");

            String redirect = resolvedReturn.contains("?")
                    ? resolvedReturn + "&createdIngredient=" + ingredient.getId()
                    : resolvedReturn + "?createdIngredient=" + ingredient.getId();
            return "redirect:" + redirect;

        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + resolvedReturn;

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Saving AI ingredient failed.");
            return "redirect:" + resolvedReturn;
        }
    }


    private String resolveReturnUrl(String candidate, HttpServletRequest req, String fallback) {
        if (!StringUtils.hasText(candidate)) {
            String ref = req.getHeader("Referer");
            candidate = StringUtils.hasText(ref) ? ref : fallback;
        }
        candidate = candidate.trim();


        if (candidate.startsWith("/") && !candidate.startsWith("//")) return candidate;

        try {
            URI u = URI.create(candidate);
            if (u.isAbsolute()) {
                boolean sameScheme = req.getScheme().equalsIgnoreCase(u.getScheme());
                boolean sameHost   = req.getServerName().equalsIgnoreCase(u.getHost());
                int reqPort = req.getServerPort();
                int urlPort = (u.getPort() == -1 ? defaultPort(u.getScheme()) : u.getPort());
                boolean samePort   = reqPort == urlPort;

                if (sameScheme && sameHost && samePort) {
                    String path  = (u.getRawPath() != null ? u.getRawPath() : "/");
                    String query = (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                    String frag  = (u.getRawFragment() != null ? "#" + u.getRawFragment() : "");
                    return path + query + frag;
                }
            }
        } catch (Exception ignore) {

        }

        return fallback;
    }

    private int defaultPort(String scheme) {
        if ("http".equalsIgnoreCase(scheme)) return 80;
        if ("https".equalsIgnoreCase(scheme)) return 443;
        return -1;
    }
}
