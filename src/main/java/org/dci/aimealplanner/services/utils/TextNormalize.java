package org.dci.aimealplanner.services.utils;

public final class TextNormalize {
    private TextNormalize() {}
    public static String normName(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ").toLowerCase();
    }
    public static String normUnitCode(String s) {
        if (s == null) return null;
        return s.trim().toLowerCase();
    }
}
