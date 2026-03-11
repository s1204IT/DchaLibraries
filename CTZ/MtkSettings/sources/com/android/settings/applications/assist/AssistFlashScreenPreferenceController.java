package com.android.settings.applications.assist;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.AssistUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.Arrays;
import java.util.List;

public class AssistFlashScreenPreferenceController extends AbstractPreferenceController implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin, LifecycleObserver, OnPause, OnResume {
    private final AssistUtils mAssistUtils;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private final SettingObserver mSettingObserver;

    public AssistFlashScreenPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mAssistUtils = new AssistUtils(context);
        this.mSettingObserver = new SettingObserver();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return getCurrentAssist() != null && allowDisablingAssistDisclosure();
    }

    @Override
    public String getPreferenceKey() {
        return "flash";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        super.displayPreference(preferenceScreen);
    }

    @Override
    public void onResume() {
        this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        updatePreference();
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference();
    }

    @Override
    public void onPause() {
        this.mSettingObserver.register(this.mContext.getContentResolver(), false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "assist_disclosure_enabled", ((Boolean) obj).booleanValue() ? 1 : 0);
        return true;
    }

    public void updatePreference() {
        if (this.mPreference == null || !(this.mPreference instanceof TwoStatePreference)) {
            return;
        }
        if (isAvailable()) {
            if (this.mScreen.findPreference(getPreferenceKey()) == null) {
                this.mScreen.addPreference(this.mPreference);
            }
        } else {
            this.mScreen.removePreference(this.mPreference);
        }
        ComponentName currentAssist = getCurrentAssist();
        this.mPreference.setEnabled(AssistContextPreferenceController.isChecked(this.mContext) && isPreInstalledAssistant(currentAssist));
        ((TwoStatePreference) this.mPreference).setChecked(willShowFlash(currentAssist));
    }

    @VisibleForTesting
    boolean willShowFlash(ComponentName componentName) {
        return AssistUtils.shouldDisclose(this.mContext, componentName);
    }

    @VisibleForTesting
    boolean isPreInstalledAssistant(ComponentName componentName) {
        return AssistUtils.isPreinstalledAssistant(this.mContext, componentName);
    }

    @VisibleForTesting
    boolean allowDisablingAssistDisclosure() {
        return AssistUtils.allowDisablingAssistDisclosure(this.mContext);
    }

    private ComponentName getCurrentAssist() {
        return this.mAssistUtils.getAssistComponentForUser(UserHandle.myUserId());
    }

    class SettingObserver extends AssistSettingObserver {
        private final Uri URI = Settings.Secure.getUriFor("assist_disclosure_enabled");
        private final Uri CONTEXT_URI = Settings.Secure.getUriFor("assist_structure_enabled");

        SettingObserver() {
        }

        @Override
        protected List<Uri> getSettingUris() {
            return Arrays.asList(this.URI, this.CONTEXT_URI);
        }

        @Override
        public void onSettingChange() {
            AssistFlashScreenPreferenceController.this.updatePreference();
        }
    }
}
