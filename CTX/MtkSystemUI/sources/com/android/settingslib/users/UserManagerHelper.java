package com.android.settingslib.users;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import com.android.internal.util.UserIcons;
import java.util.Iterator;
import java.util.List;

@Deprecated
public final class UserManagerHelper {
    private final ActivityManager mActivityManager;
    private final Context mContext;
    private OnUsersUpdateListener mUpdateListener;
    private final BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            UserManagerHelper.this.mUpdateListener.onUsersUpdate();
        }
    };
    private final UserManager mUserManager;

    public interface OnUsersUpdateListener {
        void onUsersUpdate();
    }

    public UserManagerHelper(Context context) {
        this.mContext = context;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
    }

    public void registerOnUsersUpdateListener(OnUsersUpdateListener onUsersUpdateListener) {
        this.mUpdateListener = onUsersUpdateListener;
        registerReceiver();
    }

    public void unregisterOnUsersUpdateListener() {
        unregisterReceiver();
    }

    public boolean isHeadlessSystemUser() {
        return SystemProperties.getBoolean("android.car.systemuser.headless", false);
    }

    public UserInfo getForegroundUserInfo() {
        return this.mUserManager.getUserInfo(getForegroundUserId());
    }

    public int getForegroundUserId() {
        ActivityManager activityManager = this.mActivityManager;
        return ActivityManager.getCurrentUser();
    }

    public List<UserInfo> getAllUsersExcludesSystemUser() {
        return getAllUsersExceptUser(0);
    }

    public List<UserInfo> getAllUsersExceptUser(int i) {
        List<UserInfo> users = this.mUserManager.getUsers(true);
        Iterator<UserInfo> it = users.iterator();
        while (it.hasNext()) {
            if (it.next().id == i) {
                it.remove();
            }
        }
        return users;
    }

    public List<UserInfo> getAllUsers() {
        if (isHeadlessSystemUser()) {
            return getAllUsersExcludesSystemUser();
        }
        return this.mUserManager.getUsers(true);
    }

    public boolean userIsSystemUser(UserInfo userInfo) {
        return userInfo.id == 0;
    }

    public boolean foregroundUserIsGuestUser() {
        return getForegroundUserInfo().isGuest();
    }

    public boolean foregroundUserHasUserRestriction(String str) {
        return this.mUserManager.hasUserRestriction(str, getForegroundUserInfo().getUserHandle());
    }

    public boolean foregroundUserCanAddUsers() {
        return !foregroundUserHasUserRestriction("no_add_user");
    }

    public UserInfo createNewUser(String str) {
        UserInfo userInfoCreateUser = this.mUserManager.createUser(str, 0);
        if (userInfoCreateUser == null) {
            Log.w("UserManagerHelper", "can't create user.");
            return null;
        }
        assignDefaultIcon(userInfoCreateUser);
        return userInfoCreateUser;
    }

    public void switchToUser(UserInfo userInfo) {
        if (userInfo.id == getForegroundUserId()) {
            return;
        }
        switchToUserId(userInfo.id);
    }

    public void startNewGuestSession(String str) {
        UserInfo userInfoCreateGuest = this.mUserManager.createGuest(this.mContext, str);
        if (userInfoCreateGuest == null) {
            Log.w("UserManagerHelper", "can't create user.");
        } else {
            assignDefaultIcon(userInfoCreateGuest);
            switchToUserId(userInfoCreateGuest.id);
        }
    }

    public Bitmap getUserIcon(UserInfo userInfo) {
        Bitmap userIcon = this.mUserManager.getUserIcon(userInfo.id);
        if (userIcon == null) {
            return assignDefaultIcon(userInfo);
        }
        return userIcon;
    }

    public Bitmap getUserDefaultIcon(UserInfo userInfo) {
        return UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(this.mContext.getResources(), userInfo.id, false));
    }

    public Bitmap getGuestDefaultIcon() {
        return UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(this.mContext.getResources(), -10000, false));
    }

    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mUserChangeReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    private Bitmap assignDefaultIcon(UserInfo userInfo) {
        Bitmap guestDefaultIcon = userInfo.isGuest() ? getGuestDefaultIcon() : getUserDefaultIcon(userInfo);
        this.mUserManager.setUserIcon(userInfo.id, guestDefaultIcon);
        return guestDefaultIcon;
    }

    private void switchToUserId(int i) {
        try {
            this.mActivityManager.switchUser(i);
        } catch (Exception e) {
            Log.e("UserManagerHelper", "Couldn't switch user.", e);
        }
    }

    private void unregisterReceiver() {
        this.mContext.unregisterReceiver(this.mUserChangeReceiver);
    }
}
