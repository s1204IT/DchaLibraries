package com.android.settings;

import android.app.Fragment;
import android.app.KeyguardManager;
import android.os.Bundle;
import android.os.UserManager;
import android.view.MenuItem;

public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {
    private boolean mDark;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private boolean mIsKeyguardLocked = false;
    private boolean mRestoring;

    @Override
    protected void onCreate(Bundle savedState) {
        boolean zIsKeyguardLocked;
        int credentialOwnerUserId = Utils.getCredentialOwnerUserId(this, Utils.getUserIdFromBundle(this, getIntent().getExtras()));
        if (Utils.isManagedProfile(UserManager.get(this), credentialOwnerUserId)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsWork);
        } else if (getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.darkTheme", false)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsDark);
            this.mDark = true;
        }
        super.onCreate(savedState);
        getWindow().addFlags(8192);
        if (savedState == null) {
            zIsKeyguardLocked = ((KeyguardManager) getSystemService(KeyguardManager.class)).isKeyguardLocked();
        } else {
            zIsKeyguardLocked = savedState.getBoolean("STATE_IS_KEYGUARD_LOCKED", false);
        }
        this.mIsKeyguardLocked = zIsKeyguardLocked;
        if (this.mIsKeyguardLocked && getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showWhenLocked", false)) {
            getWindow().addFlags(524288);
        }
        CharSequence msg = getIntent().getStringExtra("com.android.settings.ConfirmCredentials.title");
        setTitle(msg);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        this.mRestoring = savedState != null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("STATE_IS_KEYGUARD_LOCKED", this.mIsKeyguardLocked);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isChangingConfigurations() || this.mRestoring || !this.mDark || !this.mFirstTimeVisible) {
            return;
        }
        this.mFirstTimeVisible = false;
        prepareEnterAnimation();
        this.mEnterAnimationPending = true;
    }

    private ConfirmDeviceCredentialBaseFragment getFragment() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment == null || !(fragment instanceof ConfirmDeviceCredentialBaseFragment)) {
            return null;
        }
        return (ConfirmDeviceCredentialBaseFragment) fragment;
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (!this.mEnterAnimationPending) {
            return;
        }
        startEnterAnimation();
        this.mEnterAnimationPending = false;
    }

    public void prepareEnterAnimation() {
        getFragment().prepareEnterAnimation();
    }

    public void startEnterAnimation() {
        getFragment().startEnterAnimation();
    }
}
