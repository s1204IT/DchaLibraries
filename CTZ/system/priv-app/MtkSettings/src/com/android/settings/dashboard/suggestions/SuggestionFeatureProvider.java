package com.android.settings.dashboard.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.settings.suggestions.Suggestion;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

/* loaded from: classes.dex */
public interface SuggestionFeatureProvider {
    void dismissSuggestion(Context context, SuggestionControllerMixin suggestionControllerMixin, Suggestion suggestion);

    SharedPreferences getSharedPrefs(Context context);

    ComponentName getSuggestionServiceComponent();

    boolean isSuggestionComplete(Context context, ComponentName componentName);

    boolean isSuggestionEnabled(Context context);
}
