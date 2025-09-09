package org.dci.aimealplanner.models;


import lombok.Getter;

@Getter
public enum ActivityLevel {
    SEDENTARY("Sedentary (little or no exercise)"),
    LIGHT("Lightly Active (light exercise 1-3 days/week)"),
    MODERATE("Moderately Active (moderate exercise 3-5 days/week)"),
    ACTIVE("Active (hard exercise 6-7 days/week)"),
    VERY_ACTIVE("Very Active (very hard exercise, physical job)");

    private final String description;

    ActivityLevel(String description) {
        this.description = description;
    }
}
