package com.android.launcher3.compat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class UserManagerCompatVL extends UserManagerCompat {
    private static final String USER_CREATION_TIME_KEY = "user_creation_time_";
    private final Context mContext;
    private final PackageManager mPm;
    protected final UserManager mUserManager;
    protected ArrayMap<UserHandle, Long> mUserToSerialMap;
    protected LongArrayMap<UserHandle> mUsers;

    UserManagerCompatVL(Context context) {
        this.mUserManager = (UserManager) context.getSystemService("user");
        this.mPm = context.getPackageManager();
        this.mContext = context;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public long getSerialNumberForUser(UserHandle userHandle) {
        synchronized (this) {
            if (this.mUserToSerialMap != null) {
                Long l = this.mUserToSerialMap.get(userHandle);
                return l == null ? 0L : l.longValue();
            }
            return this.mUserManager.getSerialNumberForUser(userHandle);
        }
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public UserHandle getUserForSerialNumber(long j) {
        synchronized (this) {
            if (this.mUsers != null) {
                return this.mUsers.get(j);
            }
            return this.mUserManager.getUserForSerialNumber(j);
        }
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public boolean isQuietModeEnabled(UserHandle userHandle) {
        return false;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public boolean isUserUnlocked(UserHandle userHandle) {
        return true;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public boolean isDemoUser() {
        return false;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public boolean requestQuietModeEnabled(boolean z, UserHandle userHandle) {
        return false;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public boolean isAnyProfileQuietModeEnabled() {
        return false;
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public void enableAndResetCache() {
        synchronized (this) {
            this.mUsers = new LongArrayMap<>();
            this.mUserToSerialMap = new ArrayMap<>();
            List<UserHandle> userProfiles = this.mUserManager.getUserProfiles();
            if (userProfiles != null) {
                for (UserHandle userHandle : userProfiles) {
                    long serialNumberForUser = this.mUserManager.getSerialNumberForUser(userHandle);
                    this.mUsers.put(serialNumberForUser, userHandle);
                    this.mUserToSerialMap.put(userHandle, Long.valueOf(serialNumberForUser));
                }
            }
        }
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public List<UserHandle> getUserProfiles() {
        synchronized (this) {
            if (this.mUsers != null) {
                return new ArrayList(this.mUserToSerialMap.keySet());
            }
            List<UserHandle> userProfiles = this.mUserManager.getUserProfiles();
            return userProfiles == null ? Collections.emptyList() : userProfiles;
        }
    }

    @Override // com.android.launcher3.compat.UserManagerCompat
    public CharSequence getBadgedLabelForUser(CharSequence charSequence, UserHandle userHandle) {
        if (userHandle == null) {
            return charSequence;
        }
        return this.mPm.getUserBadgedLabel(charSequence, userHandle);
    }
}
