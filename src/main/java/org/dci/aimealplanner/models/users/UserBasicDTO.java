package org.dci.aimealplanner.models.users;

import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.models.Role;
import org.dci.aimealplanner.models.UserType;

public record UserBasicDTO(
        Long id,
        String email,
        Role role,
        UserType userType,
        String firstName,
        String lastName
) {
    public static UserBasicDTO from(User user, UserInformation userInformation) {
        if (userInformation == null) {
            return new UserBasicDTO(user.getId(), user.getEmail(), user.getRole(),
                    user.getUserType(), null, null);
        }
        return  new UserBasicDTO(user.getId(), user.getEmail(), user.getRole(),
                user.getUserType(), userInformation.getFirstName(), userInformation.getLastName());
    }
}
