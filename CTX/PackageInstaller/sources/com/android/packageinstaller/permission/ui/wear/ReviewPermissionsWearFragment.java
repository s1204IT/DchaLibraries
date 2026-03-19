package com.android.packageinstaller.permission.ui.wear;

import android.app.Activity;
import android.content.DialogInterface;
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
import android.support.wearable.view.WearableDialogHelper;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import com.android.packageinstaller.R;
import com.android.packageinstaller.permission.model.AppPermissionGroup;
import com.android.packageinstaller.permission.model.AppPermissions;
import com.android.packageinstaller.permission.utils.Utils;
import java.util.Iterator;
import java.util.List;

public class ReviewPermissionsWearFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private AppPermissions mAppPermissions;
    private boolean mHasConfirmedRevoke;
    private PreferenceCategory mNewPermissionsCategory;

    public static ReviewPermissionsWearFragment newInstance(PackageInfo packageInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("com.android.packageinstaller.permission.ui.extra.PACKAGE_INFO", packageInfo);
        ReviewPermissionsWearFragment reviewPermissionsWearFragment = new ReviewPermissionsWearFragment();
        reviewPermissionsWearFragment.setArguments(bundle);
        reviewPermissionsWearFragment.setRetainInstance(true);
        return reviewPermissionsWearFragment;
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
                ReviewPermissionsWearFragment.this.getActivity().finish();
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
    public void onResume() {
        super.onResume();
        this.mAppPermissions.refresh();
        loadPreferences();
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
        PreferenceCategory preferenceCategory = this.mNewPermissionsCategory;
        this.mNewPermissionsCategory = null;
        boolean zIsPackageUpdated = isPackageUpdated();
        int i = 100;
        PreferenceCategory preferenceCategory2 = null;
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
                    switchPreference.setTitle(appPermissionGroup.getLabel());
                    switchPreference.setPersistent(false);
                    switchPreference.setOrder(i);
                    switchPreference.setOnPreferenceChangeListener(this);
                    i++;
                }
                switchPreference.setChecked(appPermissionGroup.areRuntimePermissionsGranted());
                if (appPermissionGroup.isPolicyFixed()) {
                    switchPreference.setEnabled(false);
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
                    if (preferenceCategory2 == null) {
                        preferenceCategory2 = new PreferenceCategory(activity);
                        preferenceCategory2.setTitle(R.string.current_permissions_category);
                        preferenceCategory2.setOrder(2);
                        preferenceScreen.addPreference(preferenceCategory2);
                    }
                    preferenceCategory2.addPreference(switchPreference);
                }
            }
        }
        addTitlePreferenceToScreen(preferenceScreen);
        addActionPreferencesToScreen(preferenceScreen);
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

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        Log.d("ReviewPermWear", "onPreferenceChange " + ((Object) preference.getTitle()));
        if (this.mHasConfirmedRevoke) {
            return true;
        }
        if (preference instanceof SwitchPreference) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            if (!switchPreference.isChecked()) {
                return true;
            }
            showWarnRevokeDialog(switchPreference);
            return false;
        }
        return false;
    }

    private void showWarnRevokeDialog(final SwitchPreference switchPreference) {
        new WearableDialogHelper.DialogBuilder(getContext()).setPositiveIcon(R.drawable.cancel_button).setNegativeIcon(R.drawable.confirm_button).setPositiveButton(R.string.cancel, (DialogInterface.OnClickListener) null).setNegativeButton(R.string.grant_dialog_button_deny_anyway, new DialogInterface.OnClickListener() {
            @Override
            public final void onClick(DialogInterface dialogInterface, int i) {
                ReviewPermissionsWearFragment.lambda$showWarnRevokeDialog$0(this.f$0, switchPreference, dialogInterface, i);
            }
        }).setMessage(R.string.old_sdk_deny_warning).show();
    }

    public static void lambda$showWarnRevokeDialog$0(ReviewPermissionsWearFragment reviewPermissionsWearFragment, SwitchPreference switchPreference, DialogInterface dialogInterface, int i) {
        switchPreference.setChecked(false);
        reviewPermissionsWearFragment.mHasConfirmedRevoke = true;
    }

    private void confirmPermissionsReview() {
        PreferenceGroup preferenceScreen = this.mNewPermissionsCategory != null ? this.mNewPermissionsCategory : getPreferenceScreen();
        int preferenceCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (preference instanceof TwoStatePreference) {
                TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
                AppPermissionGroup permissionGroup = this.mAppPermissions.getPermissionGroup(preference.getKey());
                if (twoStatePreference.isChecked()) {
                    permissionGroup.grantRuntimePermissions(false);
                } else {
                    permissionGroup.revokeRuntimePermissions(false);
                }
                permissionGroup.resetReviewRequired();
            }
        }
    }

    private void addTitlePreferenceToScreen(PreferenceScreen preferenceScreen) {
        int i;
        Activity activity = getActivity();
        Preference preference = new Preference(activity);
        preferenceScreen.addPreference(preference);
        preference.setIcon(this.mAppPermissions.getPackageInfo().applicationInfo.loadIcon(activity.getPackageManager()));
        String string = this.mAppPermissions.getAppLabel().toString();
        if (isPackageUpdated()) {
            i = R.string.permission_review_title_template_update;
        } else {
            i = R.string.permission_review_title_template_install;
        }
        SpannableString spannableString = new SpannableString(getString(i, new Object[]{string}));
        int iIndexOf = spannableString.toString().indexOf(string, 0);
        int length = string.length();
        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.colorAccent, typedValue, true);
        spannableString.setSpan(new ForegroundColorSpan(activity.getColor(typedValue.resourceId)), iIndexOf, length + iIndexOf, 0);
        preference.setTitle(spannableString);
        preference.setSelectable(false);
        preference.setLayoutResource(R.layout.wear_review_permission_title_pref);
    }

    private void addActionPreferencesToScreen(PreferenceScreen preferenceScreen) {
        final Activity activity = getActivity();
        Preference preference = new Preference(activity);
        preference.setTitle(R.string.review_button_cancel);
        preference.setOrder(100000);
        preference.setEnabled(true);
        preference.setLayoutResource(R.layout.wear_review_permission_action_pref);
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference2) {
                return ReviewPermissionsWearFragment.lambda$addActionPreferencesToScreen$1(this.f$0, activity, preference2);
            }
        });
        preferenceScreen.addPreference(preference);
        Preference preference2 = new Preference(activity);
        preference2.setTitle(R.string.review_button_continue);
        preference2.setOrder(100001);
        preference2.setEnabled(true);
        preference2.setLayoutResource(R.layout.wear_review_permission_action_pref);
        preference2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public final boolean onPreferenceClick(Preference preference3) {
                return ReviewPermissionsWearFragment.lambda$addActionPreferencesToScreen$2(this.f$0, preference3);
            }
        });
        preferenceScreen.addPreference(preference2);
    }

    public static boolean lambda$addActionPreferencesToScreen$1(ReviewPermissionsWearFragment reviewPermissionsWearFragment, Activity activity, Preference preference) {
        reviewPermissionsWearFragment.executeCallback(false);
        activity.setResult(0);
        activity.finish();
        return true;
    }

    public static boolean lambda$addActionPreferencesToScreen$2(ReviewPermissionsWearFragment reviewPermissionsWearFragment, Preference preference) {
        reviewPermissionsWearFragment.confirmPermissionsReview();
        reviewPermissionsWearFragment.executeCallback(true);
        reviewPermissionsWearFragment.getActivity().finish();
        return true;
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
