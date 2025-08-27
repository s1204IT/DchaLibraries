package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.icu.text.ListFormatter;
import android.support.v7.preference.Preference;
import android.util.Log;
import com.android.settings.R;
import com.android.settingslib.applications.PermissionsSummaryHelper;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AppPermissionPreferenceController extends AppInfoPreferenceControllerBase {
    private static final String EXTRA_HIDE_INFO_BUTTON = "hideInfoButton";
    private static final String TAG = "PermissionPrefControl";
    private String mPackageName;
    final PermissionsSummaryHelper.PermissionsResultCallback mPermissionCallback;

    public AppPermissionPreferenceController(Context context, String str) {
        super(context, str);
        this.mPermissionCallback = new PermissionsSummaryHelper.PermissionsResultCallback() { // from class: com.android.settings.applications.appinfo.AppPermissionPreferenceController.1
            @Override // com.android.settingslib.applications.PermissionsSummaryHelper.PermissionsResultCallback
            public void onPermissionSummaryResult(int i, int i2, int i3, List<CharSequence> list) throws Resources.NotFoundException {
                String string;
                if (AppPermissionPreferenceController.this.mParent.getActivity() != null) {
                    Resources resources = AppPermissionPreferenceController.this.mContext.getResources();
                    if (i2 == 0) {
                        string = resources.getString(R.string.runtime_permissions_summary_no_permissions_requested);
                        AppPermissionPreferenceController.this.mPreference.setEnabled(false);
                    } else {
                        ArrayList arrayList = new ArrayList(list);
                        if (i3 > 0) {
                            arrayList.add(resources.getQuantityString(R.plurals.runtime_permissions_additional_count, i3, Integer.valueOf(i3)));
                        }
                        if (arrayList.size() == 0) {
                            string = resources.getString(R.string.runtime_permissions_summary_no_permissions_granted);
                        } else {
                            string = ListFormatter.getInstance().format(arrayList);
                        }
                        AppPermissionPreferenceController.this.mPreference.setEnabled(true);
                    }
                    AppPermissionPreferenceController.this.mPreference.setSummary(string);
                }
            }
        };
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        PermissionsSummaryHelper.getPermissionSummary(this.mContext, this.mPackageName, this.mPermissionCallback);
    }

    @Override // com.android.settings.applications.appinfo.AppInfoPreferenceControllerBase, com.android.settingslib.core.AbstractPreferenceController
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            startManagePermissionsActivity();
            return true;
        }
        return false;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    private void startManagePermissionsActivity() {
        Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
        intent.putExtra("android.intent.extra.PACKAGE_NAME", this.mParent.getAppEntry().info.packageName);
        intent.putExtra(EXTRA_HIDE_INFO_BUTTON, true);
        try {
            Activity activity = this.mParent.getActivity();
            AppInfoDashboardFragment appInfoDashboardFragment = this.mParent;
            activity.startActivityForResult(intent, 1);
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No app can handle android.intent.action.MANAGE_APP_PERMISSIONS");
        }
    }
}
