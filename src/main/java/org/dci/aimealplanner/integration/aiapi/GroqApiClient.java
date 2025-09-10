package org.dci.aimealplanner.integration.aiapi;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.AiResponse;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientUnitFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.recipes.RecipeFromAI;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GroqApiClient {
    private final RestClient groqRestClient;
    private final ObjectMapper objectMapper;


    public IngredientUnitFromAI getUnitRatiosForIngredient(String ingredientName) throws JsonProcessingException {
        String prompt = buildUnitRatioPrompt(ingredientName);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content", "Return ONLY valid JSON. No code fences or commentary."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );


        AiResponse response = groqRestClient.post()
                .uri("chat/completions")
                .body(body)
                .retrieve()
                .body(AiResponse.class);


        return objectMapper.readValue(response.getChoices().get(0).getMessage().getContent(), IngredientUnitFromAI.class);

    }

    private String buildUnitRatioPrompt(String ingredientName) {
        return """
                You are a culinary data assistant. Return ONLY strict JSON. No code fences or commentary.
                
                Task:
                For the given ingredient, list a few COMMON non-gram units and provide grams-per-ONE-unit.
                
                Rules (keep it simple):
                - Allowed unit_code: "ml","piece","tbsp","tsp","cup".
                - Include only units that make sense (omit the rest).
                - grams_per_unit = grams per 1 unit.
                - No extra fields.
                
                Output (JSON only):
                {
                  "ingredient": "string",
                  "units": [
                    { "unit_code": "ml|piece|tbsp|tsp|cup", "grams_per_unit": number }
                  ]
                }
                
                Ingredient: "%s"
                """.formatted(ingredientName);
    }

    public RecipeFromAI generateRecipeFromPrompt(String userPrompt) throws JsonProcessingException {
        String prompt = buildRecipePrompt(userPrompt);

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a culinary recipe generator. Return ONLY valid JSON. No code fences or commentary."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        AiResponse response = groqRestClient.post()
                .uri("chat/completions")
                .body(body)
                .retrieve()
                .body(AiResponse.class);

        String json = response.getChoices().get(0).getMessage().getContent();
        return objectMapper.readValue(json, RecipeFromAI.class);
    }

    private String buildRecipePrompt(String userPrompt) {
        return """
        TASK:
        Create a single cooking recipe that best matches this user request:
        "%s"
        
        RULES:
        - Output STRICT JSON ONLY (no comments, no markdown).
        - Keep ingredient names simple and commonly used (e.g., "chicken breast", "onion").
        - Use only these unit codes for ingredients: ["g","ml","piece","tbsp","tsp","cup"].
        - Difficulty must be one of: "EASY","MEDIUM","HARD".
        - preparation_time_minutes is total active prep + cook time (integer).
        - servings is a number (can be decimal).
        - meal_categories: provide a small list of category names (e.g., ["Dinner","Vegetarian"]).
          If unsure, set it to ["Unknown"] (exactly this casing).
        - ingredients.amount MUST be a number (no ranges, no strings like "to taste"); if optional, set amount to 0 and add a note.
        - instructions: concise, step-by-step strings (no numbering characters in the text; we will number them in UI).
        - If the user mentions dietary constraints (e.g., vegan, halal, gluten-free), respect them.
        - If the user lists ingredients, prioritize using them and keep pantry items minimal (oil, salt, pepper, common spices).
        
        JSON SHAPE:
        {
          "title": "string",
          "difficulty": "EASY|MEDIUM|HARD",
          "preparation_time_minutes":  integer,
          "servings": number,
          "meal_categories": ["string", ...],
          "ingredients": [
            { "name": "string", "amount": number, "unit_code": "g|ml|piece|tbsp|tsp|cup", "note": "string or null" }
          ],
          "instructions": ["string", ...]
        }
        """.formatted(userPrompt);
    }
}
