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
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
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

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !DefaultVoiceInputPicker.isCurrentAssistVoiceService(this.mAssistUtils.getAssistComponentForUser(this.mUserId), DefaultVoiceInputPicker.getCurrentService(this.mHelper));
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "voice_input_settings";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        this.mScreen = preferenceScreen;
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
        super.displayPreference(preferenceScreen);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() throws Throwable {
        this.mSettingObserver.register(this.mContext.getContentResolver(), true);
        updatePreference();
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) throws Throwable {
        super.updateState(this.mPreference);
        updatePreference();
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnPause
    public void onPause() {
        this.mSettingObserver.register(this.mContext.getContentResolver(), false);
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
    protected DefaultAppInfo getDefaultAppInfo() {
        String defaultAppKey = getDefaultAppKey();
        if (defaultAppKey == null) {
            return null;
        }
        Iterator<VoiceInputHelper.InteractionInfo> it = this.mHelper.mAvailableInteractionInfos.iterator();
        while (it.hasNext()) {
            VoiceInputHelper.InteractionInfo next = it.next();
            if (TextUtils.equals(defaultAppKey, next.key)) {
                return new DefaultVoiceInputPicker.VoiceInputDefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, next, true);
            }
        }
        Iterator<VoiceInputHelper.RecognizerInfo> it2 = this.mHelper.mAvailableRecognizerInfos.iterator();
        while (it2.hasNext()) {
            VoiceInputHelper.RecognizerInfo next2 = it2.next();
            if (TextUtils.equals(defaultAppKey, next2.key)) {
                return new DefaultVoiceInputPicker.VoiceInputDefaultAppInfo(this.mContext, this.mPackageManager, this.mUserId, next2, true);
            }
        }
        return null;
    }

    @Override // com.android.settings.applications.defaultapps.DefaultAppPreferenceController
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

        @Override // com.android.settings.applications.assist.AssistSettingObserver
        protected List<Uri> getSettingUris() {
            return null;
        }

        @Override // com.android.settings.applications.assist.AssistSettingObserver
        public void onSettingChange() throws Throwable {
            DefaultVoiceInputPreferenceController.this.updatePreference();
        }
    }
}
