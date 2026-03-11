package com.android.settings.applications.defaultapps;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.widget.CandidateInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultEmergencyPicker extends DefaultAppPickerFragment {
    @Override
    public int getMetricsCategory() {
        return 786;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.default_emergency_settings;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        ArrayList arrayList = new ArrayList();
        List<ResolveInfo> listQueryIntentActivities = this.mPm.getPackageManager().queryIntentActivities(DefaultEmergencyPreferenceController.QUERY_INTENT, 0);
        Context context = getContext();
        Iterator<ResolveInfo> it = listQueryIntentActivities.iterator();
        PackageInfo packageInfo = null;
        while (it.hasNext()) {
            try {
                PackageInfo packageInfo2 = this.mPm.getPackageManager().getPackageInfo(it.next().activityInfo.packageName, 0);
                ApplicationInfo applicationInfo = packageInfo2.applicationInfo;
                arrayList.add(new DefaultAppInfo(context, this.mPm, applicationInfo));
                if (isSystemApp(applicationInfo) && (packageInfo == null || packageInfo.firstInstallTime > packageInfo2.firstInstallTime)) {
                    packageInfo = packageInfo2;
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
            if (packageInfo != null && TextUtils.isEmpty(getDefaultKey())) {
                setDefaultKey(packageInfo.packageName);
            }
        }
        return arrayList;
    }

    @Override
    public String getConfirmationMessage(CandidateInfo candidateInfo) {
        if (Utils.isPackageDirectBootAware(getContext(), candidateInfo.getKey())) {
            return null;
        }
        return getContext().getString(R.string.direct_boot_unaware_dialog_message);
    }

    @Override
    protected String getDefaultKey() {
        return Settings.Secure.getString(getContext().getContentResolver(), "emergency_assistance_application");
    }

    @Override
    protected boolean setDefaultKey(String str) {
        ContentResolver contentResolver = getContext().getContentResolver();
        String string = Settings.Secure.getString(contentResolver, "emergency_assistance_application");
        if (!TextUtils.isEmpty(str) && !TextUtils.equals(str, string)) {
            Settings.Secure.putString(contentResolver, "emergency_assistance_application", str);
            return true;
        }
        return false;
    }

    private boolean isSystemApp(ApplicationInfo applicationInfo) {
        return (applicationInfo == null || (applicationInfo.flags & 1) == 0) ? false : true;
    }
}
