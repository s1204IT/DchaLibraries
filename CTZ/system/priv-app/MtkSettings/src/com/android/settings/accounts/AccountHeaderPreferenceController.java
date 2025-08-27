package com.android.settings.accounts;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;

/* loaded from: classes.dex */
public class AccountHeaderPreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver, OnResume {
    private final Account mAccount;
    private final Activity mActivity;
    private LayoutPreference mHeaderPreference;
    private final PreferenceFragment mHost;
    private final UserHandle mUserHandle;

    public AccountHeaderPreferenceController(Context context, Lifecycle lifecycle, Activity activity, PreferenceFragment preferenceFragment, Bundle bundle) {
        super(context);
        this.mActivity = activity;
        this.mHost = preferenceFragment;
        if (bundle != null && bundle.containsKey("account")) {
            this.mAccount = (Account) bundle.getParcelable("account");
        } else {
            this.mAccount = null;
        }
        if (bundle != null && bundle.containsKey("user_handle")) {
            this.mUserHandle = (UserHandle) bundle.getParcelable("user_handle");
        } else {
            this.mUserHandle = null;
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return (this.mAccount == null || this.mUserHandle == null) ? false : true;
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "account_header";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mHeaderPreference = (LayoutPreference) preferenceScreen.findPreference("account_header");
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnResume
    public void onResume() {
        EntityHeaderController.newInstance(this.mActivity, this.mHost, this.mHeaderPreference.findViewById(R.id.entity_header)).setLabel(this.mAccount.name).setIcon(new AuthenticatorHelper(this.mContext, this.mUserHandle, null).getDrawableForType(this.mContext, this.mAccount.type)).done(this.mActivity, true);
    }
}
