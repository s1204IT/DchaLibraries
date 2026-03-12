package com.android.settings.intelligence.suggestions.model;

import com.android.settings.intelligence.suggestions.model.SuggestionCategory;
import java.util.ArrayList;
import java.util.List;

public class SuggestionCategoryRegistry {
    public static final List<SuggestionCategory> CATEGORIES = new ArrayList();
    static final String CATEGORY_KEY_DEFERRED_SETUP = "com.android.settings.suggested.category.DEFERRED_SETUP";
    static final String CATEGORY_KEY_FIRST_IMPRESSION = "com.android.settings.suggested.category.FIRST_IMPRESSION";
    static final String CATEGORY_KEY_HIGH_PRIORITY = "com.android.settings.suggested.category.HIGH_PRIORITY";

    static {
        CATEGORIES.add(buildCategory(CATEGORY_KEY_DEFERRED_SETUP, true, 1209600000L));
        CATEGORIES.add(buildCategory(CATEGORY_KEY_HIGH_PRIORITY, true, 259200000L));
        CATEGORIES.add(buildCategory(CATEGORY_KEY_FIRST_IMPRESSION, true, 1209600000L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.LOCK_SCREEN", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.TRUST_AGENT", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.EMAIL", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.PARTNER_ACCOUNT", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.GESTURE", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.HOTWORD", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.DEFAULT", false, -1L));
        CATEGORIES.add(buildCategory("com.android.settings.suggested.category.SETTINGS_ONLY", false, -1L));
    }

    private static SuggestionCategory buildCategory(String str, boolean z, long j) {
        return new SuggestionCategory.Builder().setCategory(str).setExclusive(z).setExclusiveExpireDaysInMillis(j).build();
    }
}
