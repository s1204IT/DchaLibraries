package com.android.launcher3.model;

import android.content.Context;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.Comparator;

public abstract class AbstractUserComparator<T extends ItemInfo> implements Comparator<T> {
    private final UserHandleCompat mMyUser = UserHandleCompat.myUserHandle();
    private final UserManagerCompat mUserManager;

    public AbstractUserComparator(Context context) {
        this.mUserManager = UserManagerCompat.getInstance(context);
    }

    @Override
    public int compare(T lhs, T rhs) {
        if (this.mMyUser.equals(lhs.user)) {
            return -1;
        }
        Long aUserSerial = Long.valueOf(this.mUserManager.getSerialNumberForUser(lhs.user));
        Long bUserSerial = Long.valueOf(this.mUserManager.getSerialNumberForUser(rhs.user));
        return aUserSerial.compareTo(bUserSerial);
    }
}
