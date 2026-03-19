package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.util.Log;
import com.android.internal.util.UserIcons;
import com.android.settingslib.drawable.UserIconDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.UserInfoController;
import java.util.ArrayList;
import java.util.Iterator;

public class UserInfoControllerImpl implements UserInfoController {
    private final Context mContext;
    private String mUserAccount;
    private Drawable mUserDrawable;
    private AsyncTask<Void, Void, UserInfoQueryResult> mUserInfoTask;
    private String mUserName;
    private final ArrayList<UserInfoController.OnUserInfoChangedListener> mCallbacks = new ArrayList<>();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                UserInfoControllerImpl.this.reloadUserInfo();
            }
        }
    };
    private final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.provider.Contacts.PROFILE_CHANGED".equals(action) || "android.intent.action.USER_INFO_CHANGED".equals(action)) {
                try {
                    if (intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()) == ActivityManager.getService().getCurrentUser().id) {
                        UserInfoControllerImpl.this.reloadUserInfo();
                    }
                } catch (RemoteException e) {
                    Log.e("UserInfoController", "Couldn't get current user id for profile change", e);
                }
            }
        }
    };

    public UserInfoControllerImpl(Context context) {
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this.mReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.provider.Contacts.PROFILE_CHANGED");
        intentFilter2.addAction("android.intent.action.USER_INFO_CHANGED");
        this.mContext.registerReceiverAsUser(this.mProfileReceiver, UserHandle.ALL, intentFilter2, null, null);
    }

    @Override
    public void addCallback(UserInfoController.OnUserInfoChangedListener onUserInfoChangedListener) {
        this.mCallbacks.add(onUserInfoChangedListener);
        onUserInfoChangedListener.onUserInfoChanged(this.mUserName, this.mUserDrawable, this.mUserAccount);
    }

    @Override
    public void removeCallback(UserInfoController.OnUserInfoChangedListener onUserInfoChangedListener) {
        this.mCallbacks.remove(onUserInfoChangedListener);
    }

    @Override
    public void reloadUserInfo() {
        if (this.mUserInfoTask != null) {
            this.mUserInfoTask.cancel(false);
            this.mUserInfoTask = null;
        }
        queryForUserInformation();
    }

    private void queryForUserInformation() {
        try {
            UserInfo currentUser = ActivityManager.getService().getCurrentUser();
            final Context contextCreatePackageContextAsUser = this.mContext.createPackageContextAsUser("android", 0, new UserHandle(currentUser.id));
            final int i = currentUser.id;
            final boolean zIsGuest = currentUser.isGuest();
            final String str = currentUser.name;
            final boolean z = this.mContext.getThemeResId() != 2131886646;
            Resources resources = this.mContext.getResources();
            final int iMax = Math.max(resources.getDimensionPixelSize(R.dimen.multi_user_avatar_expanded_size), resources.getDimensionPixelSize(R.dimen.multi_user_avatar_keyguard_size));
            this.mUserInfoTask = new AsyncTask<Void, Void, UserInfoQueryResult>() {
                @Override
                protected UserInfoQueryResult doInBackground(Void... voidArr) {
                    Drawable defaultUserIcon;
                    Cursor cursorQuery;
                    UserManager userManager = UserManager.get(UserInfoControllerImpl.this.mContext);
                    String string = str;
                    Bitmap userIcon = userManager.getUserIcon(i);
                    if (userIcon != null) {
                        defaultUserIcon = new UserIconDrawable(iMax).setIcon(userIcon).setBadgeIfManagedUser(UserInfoControllerImpl.this.mContext, i).bake();
                    } else {
                        defaultUserIcon = UserIcons.getDefaultUserIcon(contextCreatePackageContextAsUser.getResources(), zIsGuest ? -10000 : i, z);
                    }
                    if (userManager.getUsers().size() <= 1 && (cursorQuery = contextCreatePackageContextAsUser.getContentResolver().query(ContactsContract.Profile.CONTENT_URI, new String[]{"_id", "display_name"}, null, null, null)) != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                string = cursorQuery.getString(cursorQuery.getColumnIndex("display_name"));
                            }
                        } finally {
                            cursorQuery.close();
                        }
                    }
                    return new UserInfoQueryResult(string, defaultUserIcon, userManager.getUserAccount(i));
                }

                @Override
                protected void onPostExecute(UserInfoQueryResult userInfoQueryResult) {
                    UserInfoControllerImpl.this.mUserName = userInfoQueryResult.getName();
                    UserInfoControllerImpl.this.mUserDrawable = userInfoQueryResult.getAvatar();
                    UserInfoControllerImpl.this.mUserAccount = userInfoQueryResult.getUserAccount();
                    UserInfoControllerImpl.this.mUserInfoTask = null;
                    UserInfoControllerImpl.this.notifyChanged();
                }
            };
            this.mUserInfoTask.execute(new Void[0]);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("UserInfoController", "Couldn't create user context", e);
            throw new RuntimeException(e);
        } catch (RemoteException e2) {
            Log.e("UserInfoController", "Couldn't get user info", e2);
            throw new RuntimeException(e2);
        }
    }

    private void notifyChanged() {
        Iterator<UserInfoController.OnUserInfoChangedListener> it = this.mCallbacks.iterator();
        while (it.hasNext()) {
            it.next().onUserInfoChanged(this.mUserName, this.mUserDrawable, this.mUserAccount);
        }
    }

    public void onDensityOrFontScaleChanged() {
        reloadUserInfo();
    }

    private static class UserInfoQueryResult {
        private Drawable mAvatar;
        private String mName;
        private String mUserAccount;

        public UserInfoQueryResult(String str, Drawable drawable, String str2) {
            this.mName = str;
            this.mAvatar = drawable;
            this.mUserAccount = str2;
        }

        public String getName() {
            return this.mName;
        }

        public Drawable getAvatar() {
            return this.mAvatar;
        }

        public String getUserAccount() {
            return this.mUserAccount;
        }
    }
}
