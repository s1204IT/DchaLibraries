package com.android.packageinstaller.permission.ui.handheld;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.RemoteCallback;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.model.Permission;
import com.android.packageinstaller.permission.ui.ConfirmActionDialogFragment;
import com.android.packageinstaller.permission.utils.ArrayUtils;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ReviewPermissionsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, View.OnClickListener, ConfirmActionDialogFragment.OnActionConfirmedListener {
    private AppPermissions mAppPermissions;
    private Button mCancelButton;
    private Button mContinueButton;
    private PreferenceCategory mCurrentPermissionsCategory;
    private boolean mHasConfirmedRevoke;
    private Button mMoreInfoButton;
    private PreferenceCategory mNewPermissionsCategory;

    public static ReviewPermissionsFragment newInstance(PackageInfo packageInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("com.android.packageinstaller.permission.ui.extra.PACKAGE_INFO", packageInfo);
        ReviewPermissionsFragment reviewPermissionsFragment = new ReviewPermissionsFragment();
        reviewPermissionsFragment.setArguments(bundle);
        reviewPermissionsFragment.setRetainInstance(true);
        return reviewPermissionsFragment;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        PackageInfo packageInfo = (PackageInfo) getArguments().getParcelable("com.android.packageinstaller.permission.ui.extra.PACKAGE_INFO");
        if (packageInfo == null) {
            activity.finish();
            return;
        }
        this.mAppPermissions = new AppPermissions(activity, packageInfo, null, false, new Runnable() {
            @Override
            public void run() {
                ReviewPermissionsFragment.this.getActivity().finish();
            }
        });
        if (this.mAppPermissions.getPermissionGroups().isEmpty()) {
            activity.finish();
            return;
        }
        boolean z = false;
        Iterator<AppPermissionGroup> it = this.mAppPermissions.getPermissionGroups().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            } else if (it.next().isReviewRequired()) {
                z = true;
                break;
            }
        }
        if (!z) {
            activity.finish();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        bindUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mAppPermissions.refresh();
        loadPreferences();
    }

    @Override
    public void onClick(View view) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (view == this.mContinueButton) {
            confirmPermissionsReview();
            executeCallback(true);
        } else if (view == this.mCancelButton) {
            executeCallback(false);
            activity.setResult(0);
        } else if (view == this.mMoreInfoButton) {
            Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
            intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mAppPermissions.getPackageInfo().packageName);
            intent.putExtra("com.android.packageinstaller.extra.ALL_PERMISSIONS", true);
            getActivity().startActivity(intent);
        }
        activity.finish();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (this.mHasConfirmedRevoke) {
            return true;
        }
        if (preference instanceof SwitchPreference) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            if (!switchPreference.isChecked()) {
                return true;
            }
            showWarnRevokeDialog(switchPreference.getKey());
            return false;
        }
        return false;
    }

    @Override
    public void onActionConfirmed(String str) {
        Preference preferenceFindPreference = getPreferenceManager().findPreference(str);
        if (preferenceFindPreference instanceof SwitchPreference) {
            ((SwitchPreference) preferenceFindPreference).setChecked(false);
            this.mHasConfirmedRevoke = true;
        }
    }

    private void showWarnRevokeDialog(String str) {
        ConfirmActionDialogFragment confirmActionDialogFragmentNewInstance = ConfirmActionDialogFragment.newInstance(getString(R.string.old_sdk_deny_warning), str);
        confirmActionDialogFragmentNewInstance.show(getFragmentManager(), confirmActionDialogFragmentNewInstance.getClass().getName());
    }

    private void confirmPermissionsReview() {
        ArrayList arrayList = new ArrayList();
        if (this.mNewPermissionsCategory != null) {
            arrayList.add(this.mNewPermissionsCategory);
            arrayList.add(this.mCurrentPermissionsCategory);
        } else {
            arrayList.add(getPreferenceScreen());
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            PreferenceGroup preferenceGroup = (PreferenceGroup) arrayList.get(i);
            int preferenceCount = preferenceGroup.getPreferenceCount();
            for (int i2 = 0; i2 < preferenceCount; i2++) {
                Preference preference = preferenceGroup.getPreference(i2);
                if (preference instanceof TwoStatePreference) {
                    TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
                    AppPermissionGroup permissionGroup = this.mAppPermissions.getPermissionGroup(preference.getKey());
                    if (twoStatePreference.isChecked()) {
                        int size2 = permissionGroup.getPermissions().size();
                        String[] strArrAppendString = null;
                        for (int i3 = 0; i3 < size2; i3++) {
                            Permission permission = permissionGroup.getPermissions().get(i3);
                            if (permission.isReviewRequired()) {
                                strArrAppendString = ArrayUtils.appendString(strArrAppendString, permission.getName());
                            }
                        }
                        if (strArrAppendString != null) {
                            permissionGroup.grantRuntimePermissions(false, strArrAppendString);
                        }
                    } else {
                        permissionGroup.revokeRuntimePermissions(false);
                    }
                    permissionGroup.resetReviewRequired();
                }
            }
        }
    }

    private void bindUi() {
        int i;
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        ((ImageView) activity.findViewById(R.id.app_icon)).setImageDrawable(this.mAppPermissions.getPackageInfo().applicationInfo.loadIcon(activity.getPackageManager()));
        if (isPackageUpdated()) {
            i = R.string.permission_review_title_template_update;
        } else {
            i = R.string.permission_review_title_template_install;
        }
        Spanned spannedFromHtml = Html.fromHtml(getString(i, new Object[]{this.mAppPermissions.getAppLabel()}), 0);
        activity.setTitle(spannedFromHtml.toString());
        ((TextView) activity.findViewById(R.id.permissions_message)).setText(spannedFromHtml);
        this.mContinueButton = (Button) getActivity().findViewById(R.id.continue_button);
        this.mContinueButton.setOnClickListener(this);
        this.mCancelButton = (Button) getActivity().findViewById(R.id.cancel_button);
        this.mCancelButton.setOnClickListener(this);
        this.mMoreInfoButton = (Button) getActivity().findViewById(R.id.permission_more_info_button);
        this.mMoreInfoButton.setOnClickListener(this);
    }

    private void loadPreferences() {
        Preference preferenceFindPreference;
        SwitchPreference switchPreference;
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen == null) {
            preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(preferenceScreen);
        } else {
            preferenceScreen.removeAll();
        }
        this.mCurrentPermissionsCategory = null;
        PreferenceCategory preferenceCategory = this.mNewPermissionsCategory;
        this.mNewPermissionsCategory = null;
        boolean zIsPackageUpdated = isPackageUpdated();
        for (AppPermissionGroup appPermissionGroup : this.mAppPermissions.getPermissionGroups()) {
            if (Utils.shouldShowPermission(appPermissionGroup, this.mAppPermissions.getPackageInfo().packageName) && "android".equals(appPermissionGroup.getDeclaringPackage())) {
                if (preferenceCategory != null) {
                    preferenceFindPreference = preferenceCategory.findPreference(appPermissionGroup.getName());
                } else {
                    preferenceFindPreference = null;
                }
                if (preferenceFindPreference instanceof SwitchPreference) {
                    switchPreference = (SwitchPreference) preferenceFindPreference;
                } else {
                    switchPreference = new SwitchPreference(getActivity());
                    switchPreference.setKey(appPermissionGroup.getName());
                    switchPreference.setIcon(Utils.applyTint(getContext(), Utils.loadDrawable(activity.getPackageManager(), appPermissionGroup.getIconPkg(), appPermissionGroup.getIconResId()), android.R.attr.colorControlNormal));
                    switchPreference.setTitle(appPermissionGroup.getLabel());
                    switchPreference.setSummary(appPermissionGroup.getDescription());
                    switchPreference.setPersistent(false);
                    switchPreference.setOnPreferenceChangeListener(this);
                }
                switchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted() || appPermissionGroup.isReviewRequired());
                if (appPermissionGroup.isPolicyFixed()) {
                    switchPreference.setEnabled(false);
                    switchPreference.setSummary(getString(R.string.permission_summary_enforced_by_policy));
                } else {
                    switchPreference.setEnabled(true);
                }
                if (appPermissionGroup.isReviewRequired()) {
                    if (!zIsPackageUpdated) {
                        preferenceScreen.addPreference(switchPreference);
                    } else {
                        if (this.mNewPermissionsCategory == null) {
                            this.mNewPermissionsCategory = new PreferenceCategory(activity);
                            this.mNewPermissionsCategory.setTitle(R.string.new_permissions_category);
                            this.mNewPermissionsCategory.setOrder(1);
                            preferenceScreen.addPreference(this.mNewPermissionsCategory);
                        }
                        this.mNewPermissionsCategory.addPreference(switchPreference);
                    }
                } else {
                    if (this.mCurrentPermissionsCategory == null) {
                        this.mCurrentPermissionsCategory = new PreferenceCategory(activity);
                        this.mCurrentPermissionsCategory.setTitle(R.string.current_permissions_category);
                        this.mCurrentPermissionsCategory.setOrder(2);
                        preferenceScreen.addPreference(this.mCurrentPermissionsCategory);
                    }
                    this.mCurrentPermissionsCategory.addPreference(switchPreference);
                }
            }
        }
    }

    private boolean isPackageUpdated() {
        List<AppPermissionGroup> permissionGroups = this.mAppPermissions.getPermissionGroups();
        int size = permissionGroups.size();
        for (int i = 0; i < size; i++) {
            if (!permissionGroups.get(i).isReviewRequired()) {
                return true;
            }
        }
        return false;
    }

    private void executeCallback(boolean z) {
        IntentSender intentSender;
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (z && (intentSender = (IntentSender) activity.getIntent().getParcelableExtra("android.intent.extra.INTENT")) != null) {
            try {
                int i = activity.getIntent().getBooleanExtra("android.intent.extra.RESULT_NEEDED", false) ? 33554432 : 0;
                activity.startIntentSenderForResult(intentSender, -1, null, i, i, 0);
            } catch (IntentSender.SendIntentException e) {
            }
        } else {
            RemoteCallback parcelableExtra = activity.getIntent().getParcelableExtra("android.intent.extra.REMOTE_CALLBACK");
            if (parcelableExtra != null) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("android.intent.extra.RETURN_RESULT", z);
                parcelableExtra.sendResult(bundle);
            }
        }
    }
}
