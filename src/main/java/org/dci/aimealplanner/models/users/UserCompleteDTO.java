package org.dci.aimealplanner.models.users;

import org.dci.aimealplanner.entities.users.DietaryPreference;
import org.dci.aimealplanner.entities.users.User;
import org.dci.aimealplanner.entities.users.UserInformation;
import org.dci.aimealplanner.models.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record UserCompleteDTO(
        Long id,
        String email,
        Role role,
        UserType userType,
        String firstName,
        String lastName,
        Integer age,
        LocalDate birthDate,
        Gender gender,
        BigDecimal weightKg,
        ActivityLevel activityLevel,
        Goal goal,
        Integer mealsPerDay,
        Integer targetKcalPerDay,
        Set<DietaryPreference> dietaryPreferences

) {
    public static UserCompleteDTO from(User user, UserInformation  userInformation) {
        return  new UserCompleteDTO(user.getId(), user.getEmail(), user.getRole(), user.getUserType(),
                userInformation.getFirstName(),userInformation.getLastName(), userInformation.getAge(), userInformation.getBirthDate(), userInformation.getGender(),
                userInformation.getWeightKg(), userInformation.getActivityLevel(), userInformation.getGoal(), userInformation.getMealsPerDay(),
                userInformation.getTargetKcalPerDay(), userInformation.getDietaryPreferences());
    }
}
