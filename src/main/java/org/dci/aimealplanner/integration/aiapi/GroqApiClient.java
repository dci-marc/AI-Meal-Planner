package org.dci.aimealplanner.integration.aiapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.AiResponse;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientUnitFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.MealPlanFromAI;
import org.dci.aimealplanner.integration.aiapi.dtos.planning.MealPlanGenerationRequest;
import org.dci.aimealplanner.integration.aiapi.dtos.recipes.RecipeFromAI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GroqApiClient {

    private static final String MODEL = "llama-3.3-70b-versatile";
    private static final double TEMP_LOW = 0.1;
    private static final double TEMP_MED = 0.2;

    private static final int MAX_RETRIES = 5;
    private static final long BASE_BACKOFF_MS = 500L;
    private static final long MAX_BACKOFF_MS = 8000L;

    private static final int MAX_TOKENS_DEFAULT = 800;

    private final RestClient groqRestClient;
    private final ObjectMapper objectMapper;


    public IngredientUnitFromAI getUnitRatiosForIngredient(String ingredientName) throws JsonProcessingException {
        String prompt = buildUnitRatioPrompt(ingredientName);
        Map<String, Object> body = chatBody(prompt, TEMP_MED, MAX_TOKENS_DEFAULT);

        AiResponse response = postChatCompletionsWithRetry(body);
        String json = response.getChoices().get(0).getMessage().getContent();
        return objectMapper.readValue(json, IngredientUnitFromAI.class);
    }

    public RecipeFromAI generateRecipeFromPrompt(String userPrompt) throws JsonProcessingException {
        String prompt = buildRecipePrompt(userPrompt);
        Map<String, Object> body = chatBody(prompt, TEMP_MED, MAX_TOKENS_DEFAULT);

        AiResponse response = postChatCompletionsWithRetry(body);
        String json = response.getChoices().get(0).getMessage().getContent();
        return objectMapper.readValue(json, RecipeFromAI.class);
    }

    public IngredientFromAI generateIngredient(String userInput) {
        String prompt = buildIngredientPrompt(userInput);
        Map<String, Object> body = chatBody(prompt, TEMP_LOW, 600);

        AiResponse response = postChatCompletionsWithRetry(body);
        String json = response.getChoices().get(0).getMessage().getContent();
        try {
            return objectMapper.readValue(json, IngredientFromAI.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse AI ingredient JSON: " + json, e);
        }
    }

    public MealPlanFromAI generateMealPlanFromProfile(MealPlanGenerationRequest req) {
        String prompt = buildMealPlanPrompt(req);
        Map<String, Object> body = chatBody(prompt, 0.15, 900);

        AiResponse response = postChatCompletionsWithRetry(body);
        String json = response.getChoices().get(0).getMessage().getContent();
        try {
            return objectMapper.readValue(json, MealPlanFromAI.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse AI meal plan JSON: " + json, e);
        }
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
                { "name": "string", "amount": number, "unit_code": "g|ml|piece","tbsp","tsp","cup", "note": "string or null" }
              ],
              "instructions": ["string", ...]
            }
            """.formatted(userPrompt);
    }

    private String buildIngredientPrompt(String ingredientName) {
        return """
            ROLE:
            You are a culinary data assistant. Return ONLY strict JSON. No code fences, no markdown, no commentary.

            GOAL:
            Produce a concise, factual profile for the ingredient named: "%s".

            RULES:
            - Output MUST be valid JSON (UTF-8), with NO trailing commas, NO comments, NO additional fields.
            - Use dot as decimal separator. Use numbers for quantities (not strings).
            - Keep names simple and commonly used (e.g., "mozzarella", "olive oil").
            - Nutrition is PER 100g (even for liquids; use typical density assumptions when needed).
            - Units list must ONLY use these codes: ["g","ml","piece","tbsp","tsp","cup"].
            - Ratios convert ONE non-gram unit to grams (toUnitCode is always "g").
            - Include only units/ratios that make sense for the ingredient; omit the rest.
            - If something is reasonably unknown, omit that item rather than guessing wildly.

            CATEGORY SUGGESTION (pick the simplest that fits):
            "Dairy","Meat","Fish & Seafood","Vegetables","Fruits","Grains & Cereals",
            "Legumes","Nuts & Seeds","Oils & Fats","Herbs & Spices","Condiments",
            "Beverages","Sweeteners","Other"

            JSON SHAPE (strict):
            {
              "name": "string",
              "category": "string",
              "nutrition": {
                "kcal": number,
                "protein": number,
                "carbs": number,
                "fat": number,
                "fiber": number,
                "sugar": number
              },
              "units": [
                { "code": "g|ml|piece|tbsp|tsp|cup", "display": "string or null" }
              ],
              "ratios": [
                { "fromUnitCode": "ml|piece|tbsp|tsp|cup", "toUnitCode": "g", "factor": number }
              ]
            }

            NOTES:
            - "units" are the common measurement units a cook would use for this ingredient.
            - Each "ratios" entry means: 1 <fromUnitCode> = <factor> g.
            - For liquids like oils: include tbsp/tsp and/or ml with realistic grams-per-unit.
            - For items commonly counted (e.g., eggs, limes), "piece" may be appropriate with an average grams-per-piece ratio.
            - Do NOT include an entry for "g" in "ratios" (since it already is grams).

            INGREDIENT:
            "%s"
            """.formatted(ingredientName, ingredientName);
    }

    private String buildMealPlanPrompt(MealPlanGenerationRequest r) {
        String start = r.startDate() != null ? r.startDate().toString() : "";
        String end   = r.endDate()   != null ? r.endDate().toString()   : "";

        String dietary   = toJsonArray(r.dietaryPreferences());
        String allergies = toJsonArray(r.allergies());
        String dislikes  = toJsonArray(r.dislikedIngredients());
        String cuisines  = toJsonArray(r.preferredCuisines());

        String targetKcal = r.targetKcalPerDay() == null ? "null" : r.targetKcalPerDay().toString();

        return """
            ROLE:
            You are a nutrition-savvy meal planner. Return ONLY strict JSON. No code fences, no markdown, no commentary.

            OBJECTIVE:
            Propose a simple meal plan for the user within the date range
            start_date = "%s", end_date = "%s".
            Distribute meals across the day using ONLY these slots: ["BREAKFAST","LUNCH","DINNER","SNACK"].

            USER PROFILE:
            - age: %s
            - gender: %s
            - height_cm: %s
            - weight_kg: %s
            - activity_level: %s
            - goal: %s
            - meals_per_day: %s
            - dietary_preferences: %s
            - allergies: %s
            - disliked_ingredients: %s
            - preferred_cuisines: %s

            TARGET CALORIES:
            - target_kcal_per_day: %s  // integer or null; if null, estimate a reasonable value and include it in the JSON.

            RULES:
            - Output STRICT JSON only (UTF-8). No additional fields, no comments.
            - Dates MUST be ISO (YYYY-MM-DD) and must fall within the given range.
            - For each date, include 2–4 meals according to meals_per_day.
            - "slot" must be one of: "BREAKFAST","LUNCH","DINNER","SNACK".
            - Titles should be simple and mappable to common recipes (e.g., "Grilled Chicken Salad", "Oatmeal with Berries").
            - Respect dietary_preferences, allergies, dislikes.
            - Keep pantry items reasonable (oil, salt, pepper, common spices).
            - servings: a number (default 1 if unsure).
            - meal_categories MUST be a subset of this exact list: ["Breakfast","Lunch","Dinner","Snack"].
              If unsure, use ["Dinner"].
              DO NOT include tags like "High-Protein","Vegan","Halal" here — they are not valid categories.
            - notes: short optional string.

            OUTPUT (strict JSON):
            {
              "name": "string",
              "target_kcal_per_day": integer or null,
              "days": [
                {
                  "date": "YYYY-MM-DD",
                  "meals": [
                    { "slot": "BREAKFAST|LUNCH|DINNER|SNACK", "title": "string", "servings": number, "meal_categories": ["Breakfast"|"Lunch"|"Dinner"|"Snack"], "notes": "string or null" }
                  ]
                }
              ]
            }
            """.formatted(
                start, end,
                numOrNull(r.age()),
                quoteOrNull(r.gender()),
                numOrNull(r.heightCm()),
                numOrNull(r.weightKg()),
                quoteOrNull(r.activityLevel()),
                quoteOrNull(r.goal()),
                numOrNull(r.mealsPerDay()),
                dietary, allergies, dislikes, cuisines,
                targetKcal
        );
    }


    private Map<String, Object> chatBody(String userPrompt, double temperature, int maxTokens) {
        return Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", "Return ONLY valid JSON. No code fences or commentary."),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", temperature,
                "max_tokens", maxTokens
        );
    }

    private AiResponse postChatCompletionsWithRetry(Map<String, Object> body) {
        int attempt = 0;
        long backoff = BASE_BACKOFF_MS;

        while (true) {
            try {
                return groqRestClient.post()
                        .uri("chat/completions")
                        .body(body)
                        .retrieve()
                        .body(AiResponse.class);

            } catch (RestClientResponseException ex) {
                HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
                boolean retryable = status != null &&
                        (status == HttpStatus.TOO_MANY_REQUESTS ||
                                status.is5xxServerError());

                if (retryable && attempt < MAX_RETRIES - 1) {
                    sleepQuietly(backoff);
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    attempt++;
                    continue;
                }
                throw ex;

            } catch (ResourceAccessException ex) {
                if (attempt < MAX_RETRIES - 1) {
                    sleepQuietly(backoff);
                    backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
                    attempt++;
                    continue;
                }
                throw ex;
            }
        }
    }

    private void sleepQuietly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String quoteOrNull(String s) {
        return s == null ? "null" : "\"" + s.replace("\"", "\\\"") + "\"";
    }

    private String numOrNull(Number n) {
        return n == null ? "null" : n.toString();
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        return "[" + list.stream()
                .filter(java.util.Objects::nonNull)
                .map(v -> "\"" + v.replace("\"", "\\\"") + "\"")
                .collect(java.util.stream.Collectors.joining(",")) + "]";
    }
}
