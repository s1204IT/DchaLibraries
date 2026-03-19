package com.android.settings.applications.assist;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import com.android.internal.app.AssistUtils;
import com.android.settings.applications.assist.DefaultVoiceInputPicker;
import com.android.settings.applications.assist.VoiceInputHelper;
import com.android.settings.applications.defaultapps.DefaultAppPreferenceController;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import java.util.List;

public class DefaultVoiceInputPreferenceController extends DefaultAppPreferenceController implements LifecycleObserver, OnPause, OnResume {
    private AssistUtils mAssistUtils;
    private VoiceInputHelper mHelper;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private SettingObserver mSettingObserver;

    public DefaultVoiceInputPreferenceController(Context context, Lifecycle lifecycle) throws Throwable {
        super(context);
        this.mSettingObserver = new SettingObserver();
        this.mAssistUtils = new AssistUtils(context);
        this.mHelper = new VoiceInputHelper(context);
        this.mHelper.buildUi();
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return !DefaultVoiceInputPicker.isCurrentAssistVoiceService(this.mAssistUtils.getAssistComponentForUser(this.mUserId), DefaultVoiceInputPicker.getCurrentService(this.mHelper));
    }

    @Override
    public String getPreferenceKey() {
        return "voice_input_settings";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        super.displayPreference(preferenceScreen);
    }

    @Override
    public void onResume() throws Throwable {
        this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        updatePreference();
    }

    @Override
    public void updateState(Preference preference) throws Throwable {
        super.updateState(this.mPreference);
        updatePreference();
    }

    @Override
    public void onPause() {
        this.mSettingObserver.register(this.mContext.getContentResolver(), false);
    }

    @Override
    protected DefaultAppInfo getDefaultAppInfo() {
        String defaultAppKey = getDefaultAppKey();
        if (defaultAppKey == null) {
            return null;
        }
        for (VoiceInputHelper.InteractionInfo interactionInfo : this.mHelper.mAvailableInteractionInfos) {
            if (TextUtils.equals(defaultAppKey, interactionInfo.key)) {
                return new DefaultVoiceInputPicker.VoiceInputDefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, interactionInfo, true);
            }
        }
        for (VoiceInputHelper.RecognizerInfo recognizerInfo : this.mHelper.mAvailableRecognizerInfos) {
            if (TextUtils.equals(defaultAppKey, recognizerInfo.key)) {
                return new DefaultVoiceInputPicker.VoiceInputDefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, recognizerInfo, true);
            }
        }
        return null;
    }

    @Override
    protected Intent getSettingIntent(DefaultAppInfo defaultAppInfo) {
        DefaultAppInfo defaultAppInfo2 = getDefaultAppInfo();
        if (defaultAppInfo2 == null || !(defaultAppInfo2 instanceof DefaultVoiceInputPicker.VoiceInputDefaultAppInfo)) {
            return null;
        }
        return ((DefaultVoiceInputPicker.VoiceInputDefaultAppInfo) defaultAppInfo2).getSettingIntent();
    }

    private void updatePreference() throws Throwable {
        if (this.mPreference == null) {
            return;
        }
        this.mHelper.buildUi();
        if (isAvailable()) {
            if (this.mScreen.findPreference(getPreferenceKey()) == null) {
                this.mScreen.addPreference(this.mPreference);
                return;
            }
            return;
        }
        this.mScreen.removePreference(this.mPreference);
    }

    private String getDefaultAppKey() {
        ComponentName currentService = DefaultVoiceInputPicker.getCurrentService(this.mHelper);
        if (currentService == null) {
            return null;
        }
        return currentService.flattenToShortString();
    }

    class SettingObserver extends AssistSettingObserver {
        SettingObserver() {
        }

        @Override
        protected List<Uri> getSettingUris() {
            return null;
        }

        @Override
        public void onSettingChange() throws Throwable {
            DefaultVoiceInputPreferenceController.this.updatePreference();
        }
    }
}
