package org.dci.aimealplanner.services.recipes;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.recipes.Recipe;
import org.dci.aimealplanner.repositories.recipes.RecipeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeSearchService {
    private final RecipeRepository recipeRepository;

    public Page<Recipe> searchForPlanner(String q, Long userId, int size) {
        String term = (q == null) ? "" : q.trim();
        if (term.length() < 2) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(size, 50)),
                Sort.by(Sort.Direction.ASC, "title"));
        return recipeRepository.searchForPlanner(term, userId, pageable);
    }
}
