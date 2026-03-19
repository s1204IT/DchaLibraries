package com.mediatek.calendar.features;

import java.util.ArrayList;

public class Features {
    private static final ArrayList<Integer> COMMON_FEATURES = new ArrayList<>();

    public static boolean isThemeManagerEnabled() {
        return isFeatureEnabled(4);
    }

    private static boolean isFeatureEnabled(int i) {
        return COMMON_FEATURES.contains(Integer.valueOf(i));
    }
}
