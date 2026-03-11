package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import com.android.launcher3.Utilities;
import com.android.launcher3.util.LongArrayMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@TargetApi(21)
public class UserManagerCompatVL extends UserManagerCompatV17 {
    private static final String USER_CREATION_TIME_KEY = "user_creation_time_";
    private final Context mContext;
    private final PackageManager mPm;

    UserManagerCompatVL(Context context) {
        super(context);
        this.mPm = context.getPackageManager();
        this.mContext = context;
    }

    @Override
    public void enableAndResetCache() {
        synchronized (this) {
            this.mUsers = new LongArrayMap<>();
            this.mUserToSerialMap = new HashMap<>();
            List<UserHandle> users = this.mUserManager.getUserProfiles();
            if (users != null) {
                for (UserHandle user : users) {
                    long serial = this.mUserManager.getSerialNumberForUser(user);
                    UserHandleCompat userCompat = UserHandleCompat.fromUser(user);
                    this.mUsers.put(serial, userCompat);
                    this.mUserToSerialMap.put(userCompat, Long.valueOf(serial));
                }
            }
        }
    }

    @Override
    public List<UserHandleCompat> getUserProfiles() {
        synchronized (this) {
            if (this.mUsers != null) {
                List<UserHandleCompat> users = new ArrayList<>();
                users.addAll(this.mUserToSerialMap.keySet());
                return users;
            }
            List<UserHandle> users2 = this.mUserManager.getUserProfiles();
            if (users2 == null) {
                return Collections.emptyList();
            }
            ArrayList<UserHandleCompat> compatUsers = new ArrayList<>(users2.size());
            for (UserHandle user : users2) {
                compatUsers.add(UserHandleCompat.fromUser(user));
            }
            return compatUsers;
        }
    }

    @Override
    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user) {
        if (user == null) {
            return label;
        }
        return this.mPm.getUserBadgedLabel(label, user.getUser());
    }

    @Override
    public long getUserCreationTime(UserHandleCompat user) {
        if (Utilities.ATLEAST_MARSHMALLOW) {
            return this.mUserManager.getUserCreationTime(user.getUser());
        }
        SharedPreferences prefs = Utilities.getPrefs(this.mContext);
        String key = USER_CREATION_TIME_KEY + getSerialNumberForUser(user);
        if (!prefs.contains(key)) {
            prefs.edit().putLong(key, System.currentTimeMillis()).apply();
        }
        return prefs.getLong(key, 0L);
    }
}
