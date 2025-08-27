package com.android.settingslib.users;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import java.util.HashMap;

/* loaded from: classes.dex */
public class AppRestrictionsHelper {
    private final Context mContext;
    private final IPackageManager mIPm;
    private final Injector mInjector;
    private final PackageManager mPackageManager;
    private final boolean mRestrictedProfile;
    HashMap<String, Boolean> mSelectedPackages = new HashMap<>();
    private final UserHandle mUser;
    private final UserManager mUserManager;

    AppRestrictionsHelper(Injector injector) {
        this.mInjector = injector;
        this.mContext = this.mInjector.getContext();
        this.mPackageManager = this.mInjector.getPackageManager();
        this.mIPm = this.mInjector.getIPackageManager();
        this.mUser = this.mInjector.getUser();
        this.mUserManager = this.mInjector.getUserManager();
        this.mRestrictedProfile = this.mUserManager.getUserInfo(this.mUser.getIdentifier()).isRestricted();
    }

    static class Injector {
        private Context mContext;
        private UserHandle mUser;

        Context getContext() {
            return this.mContext;
        }

        UserHandle getUser() {
            return this.mUser;
        }

        PackageManager getPackageManager() {
            return this.mContext.getPackageManager();
        }

        IPackageManager getIPackageManager() {
            return AppGlobals.getPackageManager();
        }

        UserManager getUserManager() {
            return (UserManager) this.mContext.getSystemService(UserManager.class);
        }
    }
}
