package com.android.settings.dashboard.suggestions;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.settings.suggestions.Suggestion;
import android.util.Pair;
import com.android.settings.Settings;
import com.android.settings.display.NightDisplayPreferenceController;
import com.android.settings.fingerprint.FingerprintEnrollSuggestionActivity;
import com.android.settings.fingerprint.FingerprintSuggestionActivity;
import com.android.settings.notification.ZenOnboardingActivity;
import com.android.settings.notification.ZenSuggestionActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ScreenLockSuggestionActivity;
import com.android.settings.support.NewDeviceIntroSuggestionActivity;
import com.android.settings.wallpaper.WallpaperSuggestionActivity;
import com.android.settings.wifi.calling.WifiCallingSuggestionActivity;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

/* loaded from: classes.dex */
public class SuggestionFeatureProviderImpl implements SuggestionFeatureProvider {
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @Override // com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
    public boolean isSuggestionEnabled(Context context) {
        return !((ActivityManager) context.getSystemService("activity")).isLowRamDevice();
    }

    @Override // com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
    public ComponentName getSuggestionServiceComponent() {
        return new ComponentName("com.android.settings.intelligence", "com.android.settings.intelligence.suggestions.SuggestionService");
    }

    @Override // com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
    public boolean isSuggestionComplete(Context context, ComponentName componentName) {
        String className = componentName.getClassName();
        if (className.equals(WallpaperSuggestionActivity.class.getName())) {
            return WallpaperSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(FingerprintSuggestionActivity.class.getName())) {
            return FingerprintSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(FingerprintEnrollSuggestionActivity.class.getName())) {
            return FingerprintEnrollSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(ScreenLockSuggestionActivity.class.getName())) {
            return ScreenLockSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(WifiCallingSuggestionActivity.class.getName())) {
            return WifiCallingSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(Settings.NightDisplaySuggestionActivity.class.getName())) {
            return NightDisplayPreferenceController.isSuggestionComplete(context);
        }
        if (className.equals(NewDeviceIntroSuggestionActivity.class.getName())) {
            return NewDeviceIntroSuggestionActivity.isSuggestionComplete(context);
        }
        if (className.equals(ZenSuggestionActivity.class.getName())) {
            return ZenOnboardingActivity.isSuggestionComplete(context);
        }
        return false;
    }

    @Override // com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
    public SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences("suggestions", 0);
    }

    public SuggestionFeatureProviderImpl(Context context) {
        this.mMetricsFeatureProvider = FeatureFactory.getFactory(context.getApplicationContext()).getMetricsFeatureProvider();
    }

    @Override // com.android.settings.dashboard.suggestions.SuggestionFeatureProvider
    public void dismissSuggestion(Context context, SuggestionControllerMixin suggestionControllerMixin, Suggestion suggestion) {
        if (suggestionControllerMixin == null || suggestion == null || context == null) {
            return;
        }
        this.mMetricsFeatureProvider.action(context, 387, suggestion.getId(), new Pair[0]);
        suggestionControllerMixin.dismissSuggestion(suggestion);
    }
}
