package com.android.settings.accounts;

import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextThemeWrapper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.accounts.AuthenticatorHelper;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

class AccountPreferenceBase extends SettingsPreferenceFragment implements AuthenticatorHelper.OnAccountsUpdateListener {
    protected AuthenticatorHelper mAuthenticatorHelper;
    private DateFormat mDateFormat;
    private Object mStatusChangeListenerHandle;
    private DateFormat mTimeFormat;
    private UserManager mUm;
    protected UserHandle mUserHandle;
    private final Handler mHandler = new Handler();
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            AccountPreferenceBase.this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    AccountPreferenceBase.this.onSyncStateUpdated();
                }
            });
        }
    };

    AccountPreferenceBase() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mUm = (UserManager) getSystemService("user");
        Activity activity = getActivity();
        this.mUserHandle = Utils.getSecureTargetUser(activity.getActivityToken(), this.mUm, getArguments(), activity.getIntent().getExtras());
        this.mAuthenticatorHelper = new AuthenticatorHelper(activity, this.mUserHandle, this.mUm, this);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
    }

    protected void onAuthDescriptionsUpdated() {
    }

    protected void onSyncStateUpdated() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        this.mDateFormat = android.text.format.DateFormat.getDateFormat(activity);
        this.mTimeFormat = android.text.format.DateFormat.getTimeFormat(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(13, this.mSyncStatusObserver);
        onSyncStateUpdated();
    }

    @Override
    public void onPause() {
        super.onPause();
        ContentResolver.removeStatusChangeListener(this.mStatusChangeListenerHandle);
    }

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        return this.mAuthenticatorHelper.getAuthoritiesForAccountType(type);
    }

    public PreferenceScreen addPreferencesForType(String accountType, PreferenceScreen parent) {
        if (!this.mAuthenticatorHelper.containsAccountType(accountType)) {
            return null;
        }
        AuthenticatorDescription desc = null;
        try {
            desc = this.mAuthenticatorHelper.getAccountTypeDescription(accountType);
            if (desc == null || desc.accountPreferencesId == 0) {
                return null;
            }
            Context targetCtx = getActivity().createPackageContextAsUser(desc.packageName, 0, this.mUserHandle);
            Resources.Theme baseTheme = getResources().newTheme();
            baseTheme.applyStyle(R.style.Theme_SettingsBase, true);
            Context themedCtx = new ContextThemeWrapper(targetCtx, 0);
            themedCtx.getTheme().setTo(baseTheme);
            PreferenceScreen prefs = getPreferenceManager().inflateFromResource(themedCtx, desc.accountPreferencesId, parent);
            return prefs;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("AccountSettings", "Couldn't load preferences.xml file from " + desc.packageName);
            return null;
        } catch (Resources.NotFoundException e2) {
            Log.w("AccountSettings", "Couldn't load preferences.xml file from " + desc.packageName);
            return null;
        }
    }

    public void updateAuthDescriptions() {
        this.mAuthenticatorHelper.updateAuthDescriptions(getActivity());
        onAuthDescriptionsUpdated();
    }

    protected Drawable getDrawableForType(String accountType) {
        return this.mAuthenticatorHelper.getDrawableForType(getActivity(), accountType);
    }

    protected CharSequence getLabelForType(String accountType) {
        return this.mAuthenticatorHelper.getLabelForType(getActivity(), accountType);
    }

    protected String formatSyncDate(Date date) {
        return this.mDateFormat.format(date) + " " + this.mTimeFormat.format(date);
    }
}
