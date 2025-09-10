package org.dci.aimealplanner.services.ingredients;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Ingredient;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.repositories.ingredients.IngredientRepository;
import org.dci.aimealplanner.repositories.ingredients.UnitRepository;
import org.dci.aimealplanner.services.utils.TextNormalize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class IngredientLookupService {
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;

    public Ingredient findIngredientByName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new RuntimeException("Ingredient not found: <blank>");
        }

        String q = TextNormalize.normName(rawName);

        Optional<Ingredient> exact = ingredientRepository.findByNameIgnoreCaseLike(q);
        if (exact.isPresent()) return exact.get();

        String singular = singularize(q);
        if (!singular.equals(q)) {
            Optional<Ingredient> singularExact = ingredientRepository.findByNameIgnoreCaseLike(singular);
            if (singularExact.isPresent()) return singularExact.get();
        }

        List<Ingredient> candidates = ingredientRepository.searchByNameFuzzy(q);
        if (!candidates.isEmpty()) {

            String wordBoundary = ".*\\b" + Pattern.quote(singular) + "\\b.*";
            return candidates.stream()
                    .filter(i -> i.getName().toLowerCase().matches(wordBoundary))
                    .findFirst()
                    .orElse(candidates.get(0));
        }

        List<Ingredient> contains = ingredientRepository.findTop10ByNameContainingIgnoreCaseOrderByNameAsc(q);
        if (!contains.isEmpty()) return contains.get(0);

        throw new RuntimeException("Ingredient not found: " + rawName);
    }

    public Unit findUnitByCode(String rawCode) {
        String normalized = TextNormalize.normUnitCode(rawCode);
        return unitRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + rawCode));
    }

    private String singularize(String s) {

        if (s.endsWith("ies") && s.length() > 3) return s.substring(0, s.length() - 3) + "y";
        if (s.endsWith("es") && s.length() > 3)  return s.substring(0, s.length() - 2);
        if (s.endsWith("s")  && s.length() > 2)  return s.substring(0, s.length() - 1);
        return s;
    }
}
