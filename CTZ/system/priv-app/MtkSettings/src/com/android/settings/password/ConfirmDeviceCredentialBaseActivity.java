package com.android.settings.password;

import android.app.Fragment;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserManager;
import android.view.MenuItem;
import android.widget.LinearLayout;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.password.ConfirmLockPassword;
import com.android.settings.password.ConfirmLockPattern;

/* loaded from: classes.dex */
public abstract class ConfirmDeviceCredentialBaseActivity extends SettingsActivity {
    private ConfirmCredentialTheme mConfirmCredentialTheme;
    private boolean mEnterAnimationPending;
    private boolean mFirstTimeVisible = true;
    private boolean mIsKeyguardLocked = false;
    private boolean mRestoring;

    enum ConfirmCredentialTheme {
        NORMAL,
        DARK,
        WORK
    }

    private boolean isInternalActivity() {
        return (this instanceof ConfirmLockPassword.InternalActivity) || (this instanceof ConfirmLockPattern.InternalActivity);
    }

    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    protected void onCreate(Bundle bundle) throws PackageManager.NameNotFoundException {
        boolean zIsKeyguardLocked;
        if (UserManager.get(this).isManagedProfile(Utils.getCredentialOwnerUserId(this, Utils.getUserIdFromBundle(this, getIntent().getExtras(), isInternalActivity())))) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsWork);
            this.mConfirmCredentialTheme = ConfirmCredentialTheme.WORK;
        } else if (getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.darkTheme", false)) {
            setTheme(R.style.Theme_ConfirmDeviceCredentialsDark);
            this.mConfirmCredentialTheme = ConfirmCredentialTheme.DARK;
        } else {
            setTheme(SetupWizardUtils.getTheme(getIntent()));
            this.mConfirmCredentialTheme = ConfirmCredentialTheme.NORMAL;
        }
        super.onCreate(bundle);
        if (this.mConfirmCredentialTheme == ConfirmCredentialTheme.NORMAL) {
            ((LinearLayout) findViewById(R.id.content_parent)).setFitsSystemWindows(false);
        }
        getWindow().addFlags(8192);
        if (bundle == null) {
            zIsKeyguardLocked = ((KeyguardManager) getSystemService(KeyguardManager.class)).isKeyguardLocked();
        } else {
            zIsKeyguardLocked = bundle.getBoolean("STATE_IS_KEYGUARD_LOCKED", false);
        }
        this.mIsKeyguardLocked = zIsKeyguardLocked;
        if (this.mIsKeyguardLocked && getIntent().getBooleanExtra("com.android.settings.ConfirmCredentials.showWhenLocked", false)) {
            getWindow().addFlags(524288);
        }
        setTitle(getIntent().getStringExtra("com.android.settings.ConfirmCredentials.title"));
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        this.mRestoring = bundle != null;
    }

    @Override // com.android.settings.SettingsActivity, android.app.Activity
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean("STATE_IS_KEYGUARD_LOCKED", this.mIsKeyguardLocked);
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 16908332) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override // com.android.settings.SettingsActivity, com.android.settingslib.drawer.SettingsDrawerActivity, android.app.Activity
    public void onResume() {
        super.onResume();
        if (!isChangingConfigurations() && !this.mRestoring && this.mConfirmCredentialTheme == ConfirmCredentialTheme.DARK && this.mFirstTimeVisible) {
            this.mFirstTimeVisible = false;
            prepareEnterAnimation();
            this.mEnterAnimationPending = true;
        }
    }

    private ConfirmDeviceCredentialBaseFragment getFragment() {
        Fragment fragmentFindFragmentById = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragmentFindFragmentById != null && (fragmentFindFragmentById instanceof ConfirmDeviceCredentialBaseFragment)) {
            return (ConfirmDeviceCredentialBaseFragment) fragmentFindFragmentById;
        }
        return null;
    }

    @Override // android.app.Activity
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        if (this.mEnterAnimationPending) {
            startEnterAnimation();
            this.mEnterAnimationPending = false;
        }
    }

    @Override // com.android.settings.SettingsActivity
    public boolean isLaunchableInTaskModePinned() {
        return true;
    }

    public void prepareEnterAnimation() {
        getFragment().prepareEnterAnimation();
    }

    public void startEnterAnimation() {
        getFragment().startEnterAnimation();
    }

    public ConfirmCredentialTheme getConfirmCredentialTheme() {
        return this.mConfirmCredentialTheme;
    }
}
