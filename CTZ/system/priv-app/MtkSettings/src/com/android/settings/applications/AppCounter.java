package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.UserInfo;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.settingslib.wrapper.PackageManagerWrapper;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class AppCounter extends AsyncTask<Void, Void, Integer> {
    protected final PackageManagerWrapper mPm;
    protected final UserManager mUm;

    protected abstract boolean includeInCount(ApplicationInfo applicationInfo);

    protected abstract void onCountComplete(int i);

    public AppCounter(Context context, PackageManagerWrapper packageManagerWrapper) {
        this.mPm = packageManagerWrapper;
        this.mUm = (UserManager) context.getSystemService("user");
    }

    /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
    @Override // android.os.AsyncTask
    protected Integer doInBackground(Void... voidArr) {
        int i = 0;
        for (UserInfo userInfo : this.mUm.getProfiles(UserHandle.myUserId())) {
            Iterator<ApplicationInfo> it = this.mPm.getInstalledApplicationsAsUser(33280 | (userInfo.isAdmin() ? 4194304 : 0), userInfo.id).iterator();
            while (it.hasNext()) {
                if (includeInCount(it.next())) {
                    i++;
                }
            }
        }
        return Integer.valueOf(i);
    }

    /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
    @Override // android.os.AsyncTask
    protected void onPostExecute(Integer num) {
        onCountComplete(num.intValue());
    }

    void executeInForeground() {
        onPostExecute(doInBackground(new Void[0]));
    }
}
