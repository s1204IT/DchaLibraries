package com.android.settings.applications;

import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.ArrayList;
import java.util.List;

public abstract class AppLister extends AsyncTask<Void, Void, List<UserAppInfo>> {
    protected final PackageManagerWrapper mPm;
    protected final UserManager mUm;

    protected abstract boolean includeInCount(ApplicationInfo applicationInfo);

    protected abstract void onAppListBuilt(List<UserAppInfo> list);

    public AppLister(PackageManagerWrapper packageManagerWrapper, UserManager userManager) {
        this.mPm = packageManagerWrapper;
        this.mUm = userManager;
    }

    @Override
    protected List<UserAppInfo> doInBackground(Void... voidArr) {
        ArrayList arrayList = new ArrayList();
        for (UserInfo userInfo : this.mUm.getProfiles(UserHandle.myUserId())) {
            for (ApplicationInfo applicationInfo : this.mPm.getInstalledApplicationsAsUser(33280 | (userInfo.isAdmin() ? 4194304 : 0), userInfo.id)) {
                if (includeInCount(applicationInfo)) {
                    arrayList.add(new UserAppInfo(userInfo, applicationInfo));
                }
            }
        }
        return arrayList;
    }

    @Override
    protected void onPostExecute(List<UserAppInfo> list) {
        onAppListBuilt(list);
    }
}
