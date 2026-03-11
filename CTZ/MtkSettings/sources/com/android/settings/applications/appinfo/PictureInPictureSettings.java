package com.android.settings.applications.appinfo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.IconDrawableFactory;
import android.util.Pair;
import android.view.View;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.EmptyTextSettings;
import com.android.settings.widget.AppPreference;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class PictureInPictureSettings extends EmptyTextSettings {
    private Context mContext;
    private IconDrawableFactory mIconDrawableFactory;
    private PackageManagerWrapper mPackageManager;
    private UserManager mUserManager;
    private static final String TAG = PictureInPictureSettings.class.getSimpleName();

    @VisibleForTesting
    static final List<String> IGNORE_PACKAGE_LIST = new ArrayList();

    static {
        IGNORE_PACKAGE_LIST.add("com.android.systemui");
    }

    static class AppComparator implements Comparator<Pair<ApplicationInfo, Integer>> {
        private final Collator mCollator = Collator.getInstance();
        private final PackageManager mPm;

        public AppComparator(PackageManager packageManager) {
            this.mPm = packageManager;
        }

        @Override
        public final int compare(Pair<ApplicationInfo, Integer> pair, Pair<ApplicationInfo, Integer> pair2) {
            CharSequence charSequenceLoadLabel = ((ApplicationInfo) pair.first).loadLabel(this.mPm);
            if (charSequenceLoadLabel == null) {
                charSequenceLoadLabel = ((ApplicationInfo) pair.first).name;
            }
            CharSequence charSequenceLoadLabel2 = ((ApplicationInfo) pair2.first).loadLabel(this.mPm);
            if (charSequenceLoadLabel2 == null) {
                charSequenceLoadLabel2 = ((ApplicationInfo) pair2.first).name;
            }
            int iCompare = this.mCollator.compare(charSequenceLoadLabel.toString(), charSequenceLoadLabel2.toString());
            if (iCompare != 0) {
                return iCompare;
            }
            return ((Integer) pair.second).intValue() - ((Integer) pair2.second).intValue();
        }
    }

    public static boolean checkPackageHasPictureInPictureActivities(String str, ActivityInfo[] activityInfoArr) {
        if (!IGNORE_PACKAGE_LIST.contains(str) && activityInfoArr != null) {
            for (int length = activityInfoArr.length - 1; length >= 0; length--) {
                if (activityInfoArr[length].supportsPictureInPicture()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mPackageManager = new PackageManagerWrapper(this.mContext.getPackageManager());
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mIconDrawableFactory = IconDrawableFactory.newInstance(this.mContext);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.removeAll();
        PackageManager packageManager = this.mPackageManager.getPackageManager();
        ArrayList<Pair<ApplicationInfo, Integer>> arrayListCollectPipApps = collectPipApps(UserHandle.myUserId());
        Collections.sort(arrayListCollectPipApps, new AppComparator(packageManager));
        Context prefContext = getPrefContext();
        for (Pair<ApplicationInfo, Integer> pair : arrayListCollectPipApps) {
            final ApplicationInfo applicationInfo = (ApplicationInfo) pair.first;
            int iIntValue = ((Integer) pair.second).intValue();
            UserHandle userHandleOf = UserHandle.of(iIntValue);
            final String str = applicationInfo.packageName;
            CharSequence charSequenceLoadLabel = applicationInfo.loadLabel(packageManager);
            AppPreference appPreference = new AppPreference(prefContext);
            appPreference.setIcon(this.mIconDrawableFactory.getBadgedIcon(applicationInfo, iIntValue));
            appPreference.setTitle(packageManager.getUserBadgedLabel(charSequenceLoadLabel, userHandleOf));
            appPreference.setSummary(PictureInPictureDetails.getPreferenceSummary(prefContext, applicationInfo.uid, str));
            appPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AppInfoBase.startAppInfoFragment(PictureInPictureDetails.class, R.string.picture_in_picture_app_detail_title, str, applicationInfo.uid, PictureInPictureSettings.this, -1, PictureInPictureSettings.this.getMetricsCategory());
                    return true;
                }
            });
            preferenceScreen.addPreference(appPreference);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setEmptyText(R.string.picture_in_picture_empty_text);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.picture_in_picture_settings;
    }

    @Override
    public int getMetricsCategory() {
        return 812;
    }

    ArrayList<Pair<ApplicationInfo, Integer>> collectPipApps(int i) {
        ArrayList<Pair<ApplicationInfo, Integer>> arrayList = new ArrayList<>();
        ArrayList arrayList2 = new ArrayList();
        Iterator it = this.mUserManager.getProfiles(i).iterator();
        while (it.hasNext()) {
            arrayList2.add(Integer.valueOf(((UserInfo) it.next()).id));
        }
        Iterator it2 = arrayList2.iterator();
        while (it2.hasNext()) {
            int iIntValue = ((Integer) it2.next()).intValue();
            for (PackageInfo packageInfo : this.mPackageManager.getInstalledPackagesAsUser(1, iIntValue)) {
                if (checkPackageHasPictureInPictureActivities(packageInfo.packageName, packageInfo.activities)) {
                    arrayList.add(new Pair<>(packageInfo.applicationInfo, Integer.valueOf(iIntValue)));
                }
            }
        }
        return arrayList;
    }
}
