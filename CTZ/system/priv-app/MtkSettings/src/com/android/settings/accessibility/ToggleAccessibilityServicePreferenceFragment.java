package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.widget.ToggleSwitch;
import com.android.settingslib.accessibility.AccessibilityUtils;
import java.util.List;

/* loaded from: classes.dex */
public class ToggleAccessibilityServicePreferenceFragment extends ToggleFeaturePreferenceFragment implements DialogInterface.OnClickListener {
    private ComponentName mComponentName;
    private LockPatternUtils mLockPatternUtils;
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler()) { // from class: com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment.1
        @Override // android.database.ContentObserver
        public void onChange(boolean z, Uri uri) {
            ToggleAccessibilityServicePreferenceFragment.this.updateSwitchBarToggleSwitch();
        }
    };
    private int mShownDialogId;

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 4;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment, com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        this.mSettingsContentObserver.register(getContentResolver());
        updateSwitchBarToggleSwitch();
        super.onResume();
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onPause() {
        this.mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    public void onPreferenceToggled(String str, boolean z) {
        AccessibilityUtils.setAccessibilityServiceState(getActivity(), ComponentName.unflattenFromString(str), z);
    }

    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        List<AccessibilityServiceInfo> installedAccessibilityServiceList = AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList();
        int size = installedAccessibilityServiceList.size();
        for (int i = 0; i < size; i++) {
            AccessibilityServiceInfo accessibilityServiceInfo = installedAccessibilityServiceList.get(i);
            ResolveInfo resolveInfo = accessibilityServiceInfo.getResolveInfo();
            if (this.mComponentName.getPackageName().equals(resolveInfo.serviceInfo.packageName) && this.mComponentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                return accessibilityServiceInfo;
            }
        }
        return null;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public Dialog onCreateDialog(int i) {
        switch (i) {
            case 1:
                this.mShownDialogId = 1;
                AccessibilityServiceInfo accessibilityServiceInfo = getAccessibilityServiceInfo();
                if (accessibilityServiceInfo == null) {
                    return null;
                }
                return AccessibilityServiceWarning.createCapabilitiesDialog(getActivity(), accessibilityServiceInfo, this);
            case 2:
                this.mShownDialogId = 2;
                AccessibilityServiceInfo accessibilityServiceInfo2 = getAccessibilityServiceInfo();
                if (accessibilityServiceInfo2 == null) {
                    return null;
                }
                return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.disable_service_title, new Object[]{accessibilityServiceInfo2.getResolveInfo().loadLabel(getPackageManager())})).setMessage(getString(R.string.disable_service_message, new Object[]{accessibilityServiceInfo2.getResolveInfo().loadLabel(getPackageManager())})).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.DialogCreatable
    public int getDialogMetricsCategory(int i) {
        if (i == 1) {
            return 583;
        }
        return 584;
    }

    private void updateSwitchBarToggleSwitch() {
        this.mSwitchBar.setCheckedInternal(AccessibilityUtils.getEnabledServicesFromSettings(getActivity()).contains(this.mComponentName));
    }

    private boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    @Override // android.app.Fragment
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 1) {
            if (i2 == -1) {
                handleConfirmServiceEnabled(true);
                if (isFullDiskEncrypted()) {
                    this.mLockPatternUtils.clearEncryptionPassword();
                    Settings.Global.putInt(getContentResolver(), "require_password_to_decrypt", 0);
                    return;
                }
                return;
            }
            handleConfirmServiceEnabled(false);
        }
    }

    @Override // android.content.DialogInterface.OnClickListener
    public void onClick(DialogInterface dialogInterface, int i) {
        boolean z = false;
        switch (i) {
            case -2:
                if (this.mShownDialogId == 2) {
                    z = true;
                }
                handleConfirmServiceEnabled(z);
                return;
            case -1:
                if (this.mShownDialogId == 1) {
                    if (isFullDiskEncrypted()) {
                        startActivityForResult(ConfirmDeviceCredentialActivity.createIntent(createConfirmCredentialReasonMessage(), null), 1);
                        return;
                    } else {
                        handleConfirmServiceEnabled(true);
                        return;
                    }
                }
                handleConfirmServiceEnabled(false);
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void handleConfirmServiceEnabled(boolean z) {
        this.mSwitchBar.setCheckedInternal(z);
        getArguments().putBoolean("checked", z);
        onPreferenceToggled(this.mPreferenceKey, z);
    }

    private String createConfirmCredentialReasonMessage() {
        int i;
        int keyguardStoredPasswordQuality = this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId());
        if (keyguardStoredPasswordQuality == 65536) {
            i = R.string.enable_service_pattern_reason;
        } else if (keyguardStoredPasswordQuality == 131072 || keyguardStoredPasswordQuality == 196608) {
            i = R.string.enable_service_pin_reason;
        } else {
            i = R.string.enable_service_password_reason;
        }
        return getString(i, new Object[]{getAccessibilityServiceInfo().getResolveInfo().loadLabel(getPackageManager())});
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() { // from class: com.android.settings.accessibility.ToggleAccessibilityServicePreferenceFragment.2
            @Override // com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean z) {
                if (!z) {
                    ToggleAccessibilityServicePreferenceFragment.this.mSwitchBar.setCheckedInternal(true);
                    ToggleAccessibilityServicePreferenceFragment.this.getArguments().putBoolean("checked", true);
                    ToggleAccessibilityServicePreferenceFragment.this.showDialog(2);
                } else {
                    ToggleAccessibilityServicePreferenceFragment.this.mSwitchBar.setCheckedInternal(false);
                    ToggleAccessibilityServicePreferenceFragment.this.getArguments().putBoolean("checked", false);
                    ToggleAccessibilityServicePreferenceFragment.this.showDialog(1);
                }
                return true;
            }
        });
    }

    @Override // com.android.settings.accessibility.ToggleFeaturePreferenceFragment
    protected void onProcessArguments(Bundle bundle) {
        super.onProcessArguments(bundle);
        String string = bundle.getString("settings_title");
        String string2 = bundle.getString("settings_component_name");
        if (!TextUtils.isEmpty(string) && !TextUtils.isEmpty(string2)) {
            Intent component = new Intent("android.intent.action.MAIN").setComponent(ComponentName.unflattenFromString(string2.toString()));
            if (!getPackageManager().queryIntentActivities(component, 0).isEmpty()) {
                this.mSettingsTitle = string;
                this.mSettingsIntent = component;
                setHasOptionsMenu(true);
            }
        }
        this.mComponentName = (ComponentName) bundle.getParcelable("component_name");
    }
}
