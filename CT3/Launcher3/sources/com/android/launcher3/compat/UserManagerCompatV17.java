package com.android.launcher3.compat;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.UserManager;
import com.android.launcher3.util.LongArrayMap;
import java.util.HashMap;

@TargetApi(17)
public class UserManagerCompatV17 extends UserManagerCompatV16 {
    protected UserManager mUserManager;
    protected HashMap<UserHandleCompat, Long> mUserToSerialMap;
    protected LongArrayMap<UserHandleCompat> mUsers;

    UserManagerCompatV17(Context context) {
        this.mUserManager = (UserManager) context.getSystemService("user");
    }

    @Override
    public long getSerialNumberForUser(UserHandleCompat user) {
        synchronized (this) {
            if (this.mUserToSerialMap != null) {
                Long serial = this.mUserToSerialMap.get(user);
                return serial == null ? 0L : serial.longValue();
            }
            return this.mUserManager.getSerialNumberForUser(user.getUser());
        }
    }

    @Override
    public UserHandleCompat getUserForSerialNumber(long serialNumber) {
        synchronized (this) {
            if (this.mUsers != null) {
                return this.mUsers.get(serialNumber);
            }
            return UserHandleCompat.fromUser(this.mUserManager.getUserForSerialNumber(serialNumber));
        }
    }

    @Override
    public void enableAndResetCache() {
        synchronized (this) {
            this.mUsers = new LongArrayMap<>();
            this.mUserToSerialMap = new HashMap<>();
            UserHandleCompat myUser = UserHandleCompat.myUserHandle();
            long serial = this.mUserManager.getSerialNumberForUser(myUser.getUser());
            this.mUsers.put(serial, myUser);
            this.mUserToSerialMap.put(myUser, Long.valueOf(serial));
        }
    }
}
