package org.dci.aimealplanner.services.ingredients;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.ingredients.Unit;
import org.dci.aimealplanner.exceptions.UnitNotFoundException;
import org.dci.aimealplanner.repositories.ingredients.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UnitService {
    private final UnitRepository unitRepository;

    public Unit findByCode(String code) {
        return unitRepository.findByCode(code).orElseThrow(() -> new UnitNotFoundException("Unit with code " + code + " not found"));
    }

    public Unit findByDisplayName(String displayName) {
        return unitRepository.findByDisplayName(displayName).orElseThrow(() -> new UnitNotFoundException("Unit with display name " + displayName + " not found"));
    }

    public List<Unit> findAll() {
        return unitRepository.findAll();
    }

    public Unit findById(Long id) {
        return unitRepository.findById(id).orElseThrow(() -> new UnitNotFoundException("Unit with id " + id + " not found"));
    }

    public Map<String, Unit> ensureUnitsByCode(LinkedHashSet<String> unitCodes) {
        Map<String, Unit> unitsByCode = new LinkedHashMap<String, Unit>();
        unitCodes.forEach(unitCode -> {unitRepository.findByCodeIgnoreCase(unitCode).ifPresent(unit -> unitsByCode.put(unitCode, unit));});
        return unitsByCode;
    }

    public Unit save(Unit newUnit) {
        return unitRepository.save(newUnit);
    }
}
