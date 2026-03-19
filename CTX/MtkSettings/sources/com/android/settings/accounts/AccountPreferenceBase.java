package com.android.settings.accounts;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SyncStatusObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.utils.ThreadUtils;
import java.text.DateFormat;
import java.util.Date;

abstract class AccountPreferenceBase extends SettingsPreferenceFragment implements AuthenticatorHelper.OnAccountsUpdateListener {
    protected static final boolean VERBOSE = Log.isLoggable("AccountPreferenceBase", 2);
    protected AccountTypePreferenceLoader mAccountTypePreferenceLoader;
    protected AuthenticatorHelper mAuthenticatorHelper;
    private DateFormat mDateFormat;
    private Object mStatusChangeListenerHandle;
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        @Override
        public final void onStatusChanged(int i) {
            ThreadUtils.postOnMainThread(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.onSyncStateUpdated();
                }
            });
        }
    };
    private DateFormat mTimeFormat;
    private UserManager mUm;
    protected UserHandle mUserHandle;

    AccountPreferenceBase() {
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mUm = (UserManager) getSystemService("user");
        Activity activity = getActivity();
        this.mUserHandle = Utils.getSecureTargetUser(activity.getActivityToken(), this.mUm, getArguments(), activity.getIntent().getExtras());
        this.mAuthenticatorHelper = new AuthenticatorHelper(activity, this.mUserHandle, this);
        this.mAccountTypePreferenceLoader = new AccountTypePreferenceLoader(this, this.mAuthenticatorHelper, this.mUserHandle);
    }

    @Override
    public void onAccountsUpdate(UserHandle userHandle) {
    }

    protected void onAuthDescriptionsUpdated() {
    }

    protected void onSyncStateUpdated() {
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
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

    public void updateAuthDescriptions() {
        this.mAuthenticatorHelper.updateAuthDescriptions(getActivity());
        onAuthDescriptionsUpdated();
    }

    protected Drawable getDrawableForType(String str) {
        return this.mAuthenticatorHelper.getDrawableForType(getActivity(), str);
    }

    protected CharSequence getLabelForType(String str) {
        return this.mAuthenticatorHelper.getLabelForType(getActivity(), str);
    }

    protected String formatSyncDate(Date date) {
        return this.mDateFormat.format(date) + " " + this.mTimeFormat.format(date);
    }
}
