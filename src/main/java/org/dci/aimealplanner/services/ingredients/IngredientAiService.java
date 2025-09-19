package org.dci.aimealplanner.services.ingredients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.IngredientUnitRatio;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.exceptions.InvalidIngredientException;
import org.dci.aimealplanner.integration.aiapi.GroqApiClient;
import org.dci.aimealplanner.integration.aiapi.dtos.ingredients.IngredientFromAI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IngredientAiService {
    private final GroqApiClient groqApiClient;
    private final IngredientService ingredientService;
    private final IngredientCategoryService ingredientCategoryService;
    private final UnitService unitService;
    private final IngredientUnitRatioService ingredientUnitRatioService;
    private final ObjectMapper objectMapper;

    public IngredientFromAI previewFromAi(String name) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Missing ingredient name");
        }
        return groqApiClient.generateIngredient(name.trim());
    }

    @Transactional
    public Ingredient saveFromAiPayload(String payload) throws JsonProcessingException {
        IngredientFromAI ai = objectMapper.readValue(payload, IngredientFromAI.class);
        return saveFromAi(ai);
    }

    @Transactional
    public Ingredient saveFromAi(IngredientFromAI ai) {
        String raw = ai.getName();
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(name)) {
            throw new InvalidIngredientException("AI payload missing ingredient name");
        }

        var existing = ingredientService.findByNameIgnoreCase(name);
        if (existing.isPresent()) return existing.get();

        String categoryName = (ai.getCategory() != null && !ai.getCategory().isBlank())
                ? ai.getCategory().trim() : "Other";
        var category = ingredientCategoryService.findOrCreateByName(categoryName);

        var unitCodes = new LinkedHashSet<String>();
        if (ai.getUnits() != null) {
            ai.getUnits().forEach(u -> {
                if (u != null && StringUtils.hasText(u.getCode())) {
                    unitCodes.add(u.getCode().trim().toLowerCase());
                }
            });
        }
        if (ai.getRatios() != null) {
            ai.getRatios().forEach(r -> {
                if (r != null && StringUtils.hasText(r.getFromUnitCode())) {
                    unitCodes.add(r.getFromUnitCode().trim().toLowerCase());
                }
            });
        }
        unitCodes.add("g");

        Map<String, Unit> unitsByCode = unitService.ensureUnitsByCode(unitCodes);

        var n = ai.getNutrition();
        Ingredient ingredient = ingredientService.createWithNutrition(
                name,
                category,
                n != null ? n.getKcal()    : null,
                n != null ? n.getProtein() : null,
                n != null ? n.getCarbs()   : null,
                n != null ? n.getFat()     : null,
                n != null ? n.getFiber()   : null,
                n != null ? n.getSugar()   : null
        );

        Unit gram = unitsByCode.get("g");
        if (gram != null) {
            ingredientUnitRatioService.ensureGramRow(ingredient, gram);
        }

        if (ai.getRatios() != null) {
            for (var r : ai.getRatios()) {
                if (r == null) continue;
                String from = r.getFromUnitCode();
                String to   = r.getToUnitCode();
                Double f    = r.getFactor();
                if (!StringUtils.hasText(from) || f == null) continue;
                if (StringUtils.hasText(to) && !"g".equalsIgnoreCase(to)) continue;

                Unit fromUnit = unitsByCode.get(from.trim().toLowerCase());
                if (fromUnit == null) continue;

                var row = new IngredientUnitRatio();
                row.setIngredient(ingredient);
                row.setUnit(fromUnit);
                row.setRatio(f);
                ingredientUnitRatioService.save(row);
            }
        }

        return ingredient;
    }
}
