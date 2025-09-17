package org.dci.aimealplanner.services.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.controllers.auth.AuthUtils;
import org.dci.aimealplanner.entities.users.DietaryPreference;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.models.users.UserBasicDTO;
import org.dci.aimealplanner.models.users.UserCompleteDTO;
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

        Optional<UserInformation> existingOpt = userInformationRepository.findByUser(currentUser);
        UserInformation target = existingOpt.orElseGet(() -> {
            userInformation.setUser(currentUser);
            return userInformation;
        });

        if (existingOpt.isPresent()) {
            userInformation.setId(existingOpt.get().getId());
            userInformation.setUser(existingOpt.get().getUser());
        }

        if (dietaryPreferenceIds != null) {
            Set<DietaryPreference> newPrefs = dietaryPreferenceIds.isEmpty()
                    ? new HashSet<>()
                    : new HashSet<>(dietaryPreferenceService.findAllByIds(dietaryPreferenceIds));
            userInformation.setDietaryPreferences(newPrefs);
        } else {
            if (userInformation.getDietaryPreferences() == null) {
                userInformation.setDietaryPreferences(
                        existingOpt.map(UserInformation::getDietaryPreferences).orElseGet(HashSet::new)
                );
            }
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

    public UserBasicDTO getUserBasicDTO(Authentication authentication) {
        User currentUser = getCurrentAuthenticatedUser(authentication);
        if (userInformationAlreadyExists(currentUser)) {
            UserInformation userInformation = getUserInformationByUser(currentUser);
            return UserBasicDTO.from(currentUser, userInformation);
        }
        return UserBasicDTO.from(currentUser, null);
    }

    public UserBasicDTO getUserBasicDTO(User user) {
        if (userInformationAlreadyExists(user)) {
            UserInformation userInformation = getUserInformationByUser(user);
            return UserBasicDTO.from(user, userInformation);
        }
        return UserBasicDTO.from(user, null);
    }

    public UserCompleteDTO getUserCompleteDTO(Authentication authentication) {
        User currentUser = getCurrentAuthenticatedUser(authentication);
        UserInformation userInformation = getUserInformationByUser(currentUser);
        return UserCompleteDTO.from(currentUser, userInformation);

    }

    public List<UserInformation> userInformationByIdIn(Set<Long> userIds) {
        return userInformationRepository.findByUser_IdIn(userIds);
    }

    public boolean userInformationAlreadyExists(User user) {
        return userInformationRepository.findByUser(user).isPresent();
    }
}
