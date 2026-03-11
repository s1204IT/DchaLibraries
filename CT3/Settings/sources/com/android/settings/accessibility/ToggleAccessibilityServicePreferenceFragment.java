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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ConfirmDeviceCredentialActivity;
import com.android.settings.R;
import com.android.settings.widget.ToggleSwitch;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;
import java.util.List;

public class ToggleAccessibilityServicePreferenceFragment extends ToggleFeaturePreferenceFragment implements DialogInterface.OnClickListener {
    private ComponentName mComponentName;
    private LockPatternUtils mLockPatternUtils;
    private final SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ToggleAccessibilityServicePreferenceFragment.this.updateSwitchBarToggleSwitch();
        }
    };
    private int mShownDialogId;

    @Override
    protected int getMetricsCategory() {
        return 4;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    @Override
    public void onResume() {
        this.mSettingsContentObserver.register(getContentResolver());
        updateSwitchBarToggleSwitch();
        super.onResume();
    }

    @Override
    public void onPause() {
        this.mSettingsContentObserver.unregister(getContentResolver());
        super.onPause();
    }

    public void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName toggledService = ComponentName.unflattenFromString(preferenceKey);
        AccessibilityUtils.setAccessibilityServiceState(getActivity(), toggledService, enabled);
    }

    private AccessibilityServiceInfo getAccessibilityServiceInfo() {
        List<AccessibilityServiceInfo> serviceInfos = AccessibilityManager.getInstance(getActivity()).getInstalledAccessibilityServiceList();
        int serviceInfoCount = serviceInfos.size();
        for (int i = 0; i < serviceInfoCount; i++) {
            AccessibilityServiceInfo serviceInfo = serviceInfos.get(i);
            ResolveInfo resolveInfo = serviceInfo.getResolveInfo();
            if (this.mComponentName.getPackageName().equals(resolveInfo.serviceInfo.packageName) && this.mComponentName.getClassName().equals(resolveInfo.serviceInfo.name)) {
                return serviceInfo;
            }
        }
        return null;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DefaultWfcSettingsExt.PAUSE:
                this.mShownDialogId = 1;
                AccessibilityServiceInfo info = getAccessibilityServiceInfo();
                if (info == null) {
                    return null;
                }
                AlertDialog ad = new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.enable_service_title, new Object[]{info.getResolveInfo().loadLabel(getPackageManager())})).setView(createEnableDialogContentView(info)).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
                View.OnTouchListener filterTouchListener = new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if ((event.getFlags() & 1) == 0) {
                            return false;
                        }
                        if (event.getAction() == 1) {
                            Toast.makeText(v.getContext(), R.string.touch_filtered_warning, 0).show();
                        }
                        return true;
                    }
                };
                ad.create();
                ad.getButton(-1).setOnTouchListener(filterTouchListener);
                return ad;
            case DefaultWfcSettingsExt.CREATE:
                this.mShownDialogId = 2;
                AccessibilityServiceInfo info2 = getAccessibilityServiceInfo();
                if (info2 == null) {
                    return null;
                }
                return new AlertDialog.Builder(getActivity()).setTitle(getString(R.string.disable_service_title, new Object[]{info2.getResolveInfo().loadLabel(getPackageManager())})).setMessage(getString(R.string.disable_service_message, new Object[]{info2.getResolveInfo().loadLabel(getPackageManager())})).setCancelable(true).setPositiveButton(android.R.string.ok, this).setNegativeButton(android.R.string.cancel, this).create();
            default:
                throw new IllegalArgumentException();
        }
    }

    public void updateSwitchBarToggleSwitch() {
        boolean zContains;
        String settingValue = Settings.Secure.getString(getContentResolver(), "enabled_accessibility_services");
        if (settingValue == null) {
            zContains = false;
        } else {
            zContains = settingValue.contains(this.mComponentName.flattenToString());
        }
        this.mSwitchBar.setCheckedInternal(zContains);
    }

    private boolean isFullDiskEncrypted() {
        return StorageManager.isNonDefaultBlockEncrypted();
    }

    private View createEnableDialogContentView(AccessibilityServiceInfo info) {
        LayoutInflater inflater = (LayoutInflater) getSystemService("layout_inflater");
        View content = inflater.inflate(R.layout.enable_accessibility_service_dialog_content, (ViewGroup) null);
        TextView encryptionWarningView = (TextView) content.findViewById(R.id.encryption_warning);
        if (isFullDiskEncrypted()) {
            String text = getString(R.string.enable_service_encryption_warning, new Object[]{info.getResolveInfo().loadLabel(getPackageManager())});
            encryptionWarningView.setText(text);
            encryptionWarningView.setVisibility(0);
        } else {
            encryptionWarningView.setVisibility(8);
        }
        TextView capabilitiesHeaderView = (TextView) content.findViewById(R.id.capabilities_header);
        capabilitiesHeaderView.setText(getString(R.string.capabilities_list_title, new Object[]{info.getResolveInfo().loadLabel(getPackageManager())}));
        LinearLayout capabilitiesView = (LinearLayout) content.findViewById(R.id.capabilities);
        View capabilityView = inflater.inflate(android.R.layout.alert_dialog_title_material, (ViewGroup) null);
        ImageView imageView = (ImageView) capabilityView.findViewById(android.R.id.full);
        imageView.setImageDrawable(getActivity().getDrawable(android.R.drawable.ic_expand_bundle));
        TextView labelView = (TextView) capabilityView.findViewById(android.R.id.fullscreenArea);
        labelView.setText(getString(R.string.capability_title_receiveAccessibilityEvents));
        TextView descriptionView = (TextView) capabilityView.findViewById(android.R.id.future);
        descriptionView.setText(getString(R.string.capability_desc_receiveAccessibilityEvents));
        List<AccessibilityServiceInfo.CapabilityInfo> capabilities = info.getCapabilityInfos();
        capabilitiesView.addView(capabilityView);
        int capabilityCount = capabilities.size();
        for (int i = 0; i < capabilityCount; i++) {
            AccessibilityServiceInfo.CapabilityInfo capability = capabilities.get(i);
            View capabilityView2 = inflater.inflate(android.R.layout.alert_dialog_title_material, (ViewGroup) null);
            ImageView imageView2 = (ImageView) capabilityView2.findViewById(android.R.id.full);
            imageView2.setImageDrawable(getActivity().getDrawable(android.R.drawable.ic_expand_bundle));
            TextView labelView2 = (TextView) capabilityView2.findViewById(android.R.id.fullscreenArea);
            labelView2.setText(getString(capability.titleResId));
            TextView descriptionView2 = (TextView) capabilityView2.findViewById(android.R.id.future);
            descriptionView2.setText(getString(capability.descResId));
            capabilitiesView.addView(capabilityView2);
        }
        return content;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1) {
            return;
        }
        if (resultCode == -1) {
            handleConfirmServiceEnabled(true);
            if (!isFullDiskEncrypted()) {
                return;
            }
            this.mLockPatternUtils.clearEncryptionPassword();
            Settings.Global.putInt(getContentResolver(), "require_password_to_decrypt", 0);
            return;
        }
        handleConfirmServiceEnabled(false);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                boolean checked = this.mShownDialogId == 2;
                handleConfirmServiceEnabled(checked);
                return;
            case -1:
                if (this.mShownDialogId == 1) {
                    if (isFullDiskEncrypted()) {
                        String title = createConfirmCredentialReasonMessage();
                        Intent intent = ConfirmDeviceCredentialActivity.createIntent(title, null);
                        startActivityForResult(intent, 1);
                        return;
                    }
                    handleConfirmServiceEnabled(true);
                    return;
                }
                handleConfirmServiceEnabled(false);
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void handleConfirmServiceEnabled(boolean confirmed) {
        this.mSwitchBar.setCheckedInternal(confirmed);
        getArguments().putBoolean("checked", confirmed);
        onPreferenceToggled(this.mPreferenceKey, confirmed);
    }

    private String createConfirmCredentialReasonMessage() {
        int resId = R.string.enable_service_password_reason;
        switch (this.mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case 65536:
                resId = R.string.enable_service_pattern_reason;
                break;
            case 131072:
            case 196608:
                resId = R.string.enable_service_pin_reason;
                break;
        }
        return getString(resId, new Object[]{getAccessibilityServiceInfo().getResolveInfo().loadLabel(getPackageManager())});
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    ToggleAccessibilityServicePreferenceFragment.this.mSwitchBar.setCheckedInternal(false);
                    ToggleAccessibilityServicePreferenceFragment.this.getArguments().putBoolean("checked", false);
                    ToggleAccessibilityServicePreferenceFragment.this.showDialog(1);
                } else {
                    ToggleAccessibilityServicePreferenceFragment.this.mSwitchBar.setCheckedInternal(true);
                    ToggleAccessibilityServicePreferenceFragment.this.getArguments().putBoolean("checked", true);
                    ToggleAccessibilityServicePreferenceFragment.this.showDialog(2);
                }
                return true;
            }
        });
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        super.onProcessArguments(arguments);
        String settingsTitle = arguments.getString("settings_title");
        String settingsComponentName = arguments.getString("settings_component_name");
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent("android.intent.action.MAIN").setComponent(ComponentName.unflattenFromString(settingsComponentName.toString()));
            if (!getPackageManager().queryIntentActivities(settingsIntent, 0).isEmpty()) {
                this.mSettingsTitle = settingsTitle;
                this.mSettingsIntent = settingsIntent;
                setHasOptionsMenu(true);
            }
        }
        this.mComponentName = (ComponentName) arguments.getParcelable("component_name");
    }
}
