package org.dci.aimealplanner.repositories.users;

import org.dci.aimealplanner.entities.users.DietaryPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DietaryPreferenceRepository extends JpaRepository<DietaryPreference, Long> {
    List<DietaryPreference> findAllByIdIn(List<Long> ids);

}
