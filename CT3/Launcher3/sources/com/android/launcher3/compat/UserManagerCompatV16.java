package com.android.launcher3.compat;

import java.util.ArrayList;
import java.util.List;

public class UserManagerCompatV16 extends UserManagerCompat {
    UserManagerCompatV16() {
    }

    @Override
    public List<UserHandleCompat> getUserProfiles() {
        List<UserHandleCompat> profiles = new ArrayList<>(1);
        profiles.add(UserHandleCompat.myUserHandle());
        return profiles;
    }

    @Override
    public UserHandleCompat getUserForSerialNumber(long serialNumber) {
        return UserHandleCompat.myUserHandle();
    }

    @Override
    public long getSerialNumberForUser(UserHandleCompat user) {
        return 0L;
    }

    @Override
    public CharSequence getBadgedLabelForUser(CharSequence label, UserHandleCompat user) {
        return label;
    }

    @Override
    public long getUserCreationTime(UserHandleCompat user) {
        return 0L;
    }

    @Override
    public void enableAndResetCache() {
    }

    @Override
    public boolean isQuietModeEnabled(UserHandleCompat user) {
        return false;
    }
}
