package com.android.settings.applications.manageapplications;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.text.TextUtils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

/* loaded from: classes.dex */
public class ResetAppPrefPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnCreate, OnSaveInstanceState {
    private ResetAppsHelper mResetAppsHelper;

    public ResetAppPrefPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        this.mResetAppsHelper = new ResetAppsHelper(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        this.mResetAppsHelper.buildResetDialog();
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "reset_app_prefs";
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnCreate
    public void onCreate(Bundle bundle) {
        this.mResetAppsHelper.onRestoreInstanceState(bundle);
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnSaveInstanceState
    public void onSaveInstanceState(Bundle bundle) {
        this.mResetAppsHelper.onSaveInstanceState(bundle);
    }
}
