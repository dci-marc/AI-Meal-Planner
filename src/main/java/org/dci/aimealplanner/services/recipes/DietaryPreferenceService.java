package org.dci.aimealplanner.services.recipes;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.DietaryPreference;
import org.dci.aimealplanner.repositories.users.DietaryPreferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DietaryPreferenceService {
    private final DietaryPreferenceRepository dietaryPreferenceRepository;

    public List<DietaryPreference> findAll() {
        return dietaryPreferenceRepository.findAll();
    }

    public List<DietaryPreference> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return dietaryPreferenceRepository.findAllByIdIn(ids);
    }
}
