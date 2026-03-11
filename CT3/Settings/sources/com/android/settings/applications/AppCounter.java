package com.android.settings.applications;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;

public abstract class AppCounter extends AsyncTask<Void, Void, Integer> {
    protected final IPackageManager mIpm = AppGlobals.getPackageManager();
    protected final PackageManager mPm;
    protected final UserManager mUm;

    protected abstract boolean includeInCount(ApplicationInfo applicationInfo);

    protected abstract void onCountComplete(int i);

    public AppCounter(Context context) {
        this.mPm = context.getPackageManager();
        this.mUm = UserManager.get(context);
    }

    @Override
    public Integer doInBackground(Void... params) {
        int count = 0;
        for (UserInfo user : this.mUm.getProfiles(UserHandle.myUserId())) {
            try {
                ParceledListSlice<ApplicationInfo> list = this.mIpm.getInstalledApplications((user.isAdmin() ? 8192 : 0) | 33280, user.id);
                for (ApplicationInfo info : list.getList()) {
                    if (includeInCount(info)) {
                        count++;
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return Integer.valueOf(count);
    }

    @Override
    public void onPostExecute(Integer count) {
        onCountComplete(count.intValue());
    }
}
