package org.dci.aimealplanner.services.users;

import lombok.RequiredArgsConstructor;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.repositories.users.UserInformationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserInformationService {
    private final UserInformationRepository userInformationRepository;


    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (User) authentication.getPrincipal();
    }


    public Optional<UserInformation> getUserInformationForCurrentUser(){
        User currentUser = getCurrentAuthenticatedUser();
        return userInformationRepository.findByUser(currentUser);
    }

    public UserInformation saveProfile(UserInformation userInformation) {
        User currentUser = getCurrentAuthenticatedUser();
        Optional<UserInformation> existing = userInformationRepository.findByUser(currentUser);

        if (existing.isPresent()) {
            UserInformation existingInfo = existing.get();
            userInformation.setUser(existingInfo.getUser());
        }
        userInformation.setUser(currentUser);
        return userInformationRepository.save(userInformation);
    }

    public void deleteProfileIfOwner(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        userInformationRepository.findById(id).ifPresent(info -> {
            if (info.getUser().getId().equals(currentUser.getId())) {
                userInformationRepository.delete(info);
            }
        });
    }

}
