package org.dci.aimealplanner.services.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.users.DietaryPreference;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.repositories.users.UserInformationRepository;
import org.dci.aimealplanner.services.recipes.DietaryPreferenceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserInformationService {

    private final UserInformationRepository userInformationRepository;
    private final UserService userService;
    private final DietaryPreferenceService dietaryPreferenceService;

    private User getCurrentAuthenticatedUser(Authentication authentication) {
        String email = AuthUtils.getUserEmail(authentication);
        return userService.findByEmail(email);
    }

    public Optional<UserInformation> getUserInformationForCurrentUser(Authentication authentication) {
        User currentUser = getCurrentAuthenticatedUser(authentication);
        return userInformationRepository.findByUser(currentUser);
    }

    public UserInformation saveProfile(UserInformation userInformation,
                                       List<Long> dietaryPreferenceIds,
                                       Authentication authentication) {
        User currentUser = getCurrentAuthenticatedUser(authentication);

        if (userInformation.getBirthDate() != null &&
                userInformation.getBirthDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future.");
        }

        if (dietaryPreferenceIds != null && !dietaryPreferenceIds.isEmpty()) {
            Set<DietaryPreference> prefs =
                    new HashSet<>(dietaryPreferenceService.findAllByIds(dietaryPreferenceIds));
            userInformation.setDietaryPreferences(prefs);
        } else {
            userInformation.getDietaryPreferences().clear();
        }

        Optional<UserInformation> existing = userInformationRepository.findByUser(currentUser);
        if (existing.isPresent()) {
            UserInformation existingInfo = existing.get();
            userInformation.setId(existingInfo.getId());
            userInformation.setUser(existingInfo.getUser());
        } else {
            userInformation.setUser(currentUser);
        }

        return userInformationRepository.save(userInformation);
    }

    public void deleteProfileIfOwner(Long id, Authentication authentication) {
        User currentUser = getCurrentAuthenticatedUser(authentication);
        userInformationRepository.findById(id).ifPresent(info -> {
            if (info.getUser() != null && info.getUser().getId().equals(currentUser.getId())) {
                userInformationRepository.delete(info);
            }
        });
    }

    public UserInformation getByUserId(Long userId) {
        return userInformationRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public UserInformation getUserInformationByUser(User currectUser) {
        return userInformationRepository.findByUser(currectUser).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
