package org.dci.aimealplanner.repositories.uploads;

import org.dci.aimealplanner.entities.recipes.ImageMetaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageMetaDataRepository extends JpaRepository<ImageMetaData, Long> {
}
