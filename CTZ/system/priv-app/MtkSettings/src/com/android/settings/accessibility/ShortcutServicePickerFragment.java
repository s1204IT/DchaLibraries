package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class ShortcutServicePickerFragment extends RadioButtonPickerFragment {
    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 6;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.core.InstrumentedPreferenceFragment
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_shortcut_service_settings;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected List<? extends CandidateInfo> getCandidates() {
        int i;
        List<AccessibilityServiceInfo> installedAccessibilityServiceList = ((AccessibilityManager) getContext().getSystemService(AccessibilityManager.class)).getInstalledAccessibilityServiceList();
        int size = installedAccessibilityServiceList.size();
        ArrayList arrayList = new ArrayList(size);
        Map frameworkShortcutFeaturesMap = AccessibilityShortcutController.getFrameworkShortcutFeaturesMap();
        for (ComponentName componentName : frameworkShortcutFeaturesMap.keySet()) {
            if (componentName.equals(AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME)) {
                i = R.drawable.ic_color_inversion;
            } else if (componentName.equals(AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME)) {
                i = R.drawable.ic_daltonizer;
            } else {
                i = R.drawable.empty_icon;
            }
            arrayList.add(new FrameworkCandidateInfo((AccessibilityShortcutController.ToggleableFrameworkFeatureInfo) frameworkShortcutFeaturesMap.get(componentName), i, componentName.flattenToString()));
        }
        for (int i2 = 0; i2 < size; i2++) {
            arrayList.add(new ServiceCandidateInfo(installedAccessibilityServiceList.get(i2)));
        }
        return arrayList;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected String getDefaultKey() {
        ComponentName componentNameUnflattenFromString;
        String shortcutTargetServiceComponentNameString = AccessibilityUtils.getShortcutTargetServiceComponentNameString(getContext(), UserHandle.myUserId());
        if (shortcutTargetServiceComponentNameString != null && (componentNameUnflattenFromString = ComponentName.unflattenFromString(shortcutTargetServiceComponentNameString)) != null) {
            return componentNameUnflattenFromString.flattenToString();
        }
        return null;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment
    protected boolean setDefaultKey(String str) {
        Settings.Secure.putString(getContext().getContentResolver(), "accessibility_shortcut_target_service", str);
        return true;
    }

    @Override // com.android.settings.widget.RadioButtonPickerFragment, com.android.settings.widget.RadioButtonPreference.OnClickListener
    public void onRadioButtonClicked(RadioButtonPreference radioButtonPreference) {
        String key = radioButtonPreference.getKey();
        if (TextUtils.isEmpty(key)) {
            super.onRadioButtonClicked(radioButtonPreference);
            return;
        }
        if (AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().containsKey(ComponentName.unflattenFromString(key))) {
            onRadioButtonConfirmed(key);
            return;
        }
        Activity activity = getActivity();
        if (activity != null) {
            ConfirmationDialogFragment.newInstance(this, key).show(activity.getFragmentManager(), "ConfirmationDialogFragment");
        }
    }

    private void onServiceConfirmed(String str) {
        onRadioButtonConfirmed(str);
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment implements DialogInterface.OnClickListener {
        private IBinder mToken;

        public static ConfirmationDialogFragment newInstance(ShortcutServicePickerFragment shortcutServicePickerFragment, String str) {
            ConfirmationDialogFragment confirmationDialogFragment = new ConfirmationDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("extra_key", str);
            confirmationDialogFragment.setArguments(bundle);
            confirmationDialogFragment.setTargetFragment(shortcutServicePickerFragment, 0);
            confirmationDialogFragment.mToken = new Binder();
            return confirmationDialogFragment;
        }

        @Override // com.android.settingslib.core.instrumentation.Instrumentable
        public int getMetricsCategory() {
            return 6;
        }

        @Override // android.app.DialogFragment
        public Dialog onCreateDialog(Bundle bundle) {
            return AccessibilityServiceWarning.createCapabilitiesDialog(getActivity(), ((AccessibilityManager) getActivity().getSystemService(AccessibilityManager.class)).getInstalledServiceInfoWithComponentName(ComponentName.unflattenFromString(getArguments().getString("extra_key"))), this);
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialogInterface, int i) {
            Fragment targetFragment = getTargetFragment();
            if (i == -1 && (targetFragment instanceof ShortcutServicePickerFragment)) {
                ((ShortcutServicePickerFragment) targetFragment).onServiceConfirmed(getArguments().getString("extra_key"));
            }
        }
    }

    private class FrameworkCandidateInfo extends CandidateInfo {
        final int mIconResId;
        final String mKey;
        final AccessibilityShortcutController.ToggleableFrameworkFeatureInfo mToggleableFrameworkFeatureInfo;

        public FrameworkCandidateInfo(AccessibilityShortcutController.ToggleableFrameworkFeatureInfo toggleableFrameworkFeatureInfo, int i, String str) {
            super(true);
            this.mToggleableFrameworkFeatureInfo = toggleableFrameworkFeatureInfo;
            this.mIconResId = i;
            this.mKey = str;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            return this.mToggleableFrameworkFeatureInfo.getLabel(ShortcutServicePickerFragment.this.getContext());
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public Drawable loadIcon() {
            return ShortcutServicePickerFragment.this.getContext().getDrawable(this.mIconResId);
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public String getKey() {
            return this.mKey;
        }
    }

    private class ServiceCandidateInfo extends CandidateInfo {
        final AccessibilityServiceInfo mServiceInfo;

        public ServiceCandidateInfo(AccessibilityServiceInfo accessibilityServiceInfo) {
            super(true);
            this.mServiceInfo = accessibilityServiceInfo;
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public CharSequence loadLabel() {
            PackageManagerWrapper packageManagerWrapper = new PackageManagerWrapper(ShortcutServicePickerFragment.this.getContext().getPackageManager());
            CharSequence charSequenceLoadLabel = this.mServiceInfo.getResolveInfo().serviceInfo.loadLabel(packageManagerWrapper.getPackageManager());
            if (charSequenceLoadLabel != null) {
                return charSequenceLoadLabel;
            }
            ComponentName componentName = this.mServiceInfo.getComponentName();
            if (componentName == null) {
                return null;
            }
            try {
                return packageManagerWrapper.getApplicationInfoAsUser(componentName.getPackageName(), 0, UserHandle.myUserId()).loadLabel(packageManagerWrapper.getPackageManager());
            } catch (PackageManager.NameNotFoundException e) {
                return null;
            }
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public Drawable loadIcon() {
            ResolveInfo resolveInfo = this.mServiceInfo.getResolveInfo();
            if (resolveInfo.getIconResource() == 0) {
                return ShortcutServicePickerFragment.this.getContext().getDrawable(R.mipmap.ic_accessibility_generic);
            }
            return resolveInfo.loadIcon(ShortcutServicePickerFragment.this.getContext().getPackageManager());
        }

        @Override // com.android.settingslib.widget.CandidateInfo
        public String getKey() {
            return this.mServiceInfo.getComponentName().flattenToString();
        }
    }
}
