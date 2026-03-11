package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.internal.util.UserIcons;
import com.android.settingslib.RestrictedLockUtils;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.ActivityStarter;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController {
    private final ActivityStarter mActivityStarter;
    private Dialog mAddUserDialog;
    private boolean mAddUsersWhenLocked;
    private final Context mContext;
    private Dialog mExitGuestDialog;
    private final Handler mHandler;
    private final KeyguardMonitor mKeyguardMonitor;
    private boolean mPauseRefreshUsers;
    private PhoneStateListener mPhoneStateListener;
    private Intent mSecondaryUserServiceIntent;
    private boolean mSimpleUserSwitcher;
    private final UserManager mUserManager;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    private ArrayList<UserRecord> mUsers = new ArrayList<>();
    private int mLastNonGuestUser = 0;
    private int mSecondaryUser = -10000;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean unpauseRefreshUsers = false;
            int forcePictureLoadForId = -10000;
            if ("com.android.systemui.REMOVE_GUEST".equals(intent.getAction())) {
                int currentUser = ActivityManager.getCurrentUser();
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentUser);
                if (userInfo != null && userInfo.isGuest()) {
                    UserSwitcherController.this.showExitGuestDialog(currentUser);
                    return;
                }
                return;
            }
            if ("com.android.systemui.LOGOUT_USER".equals(intent.getAction())) {
                UserSwitcherController.this.logoutCurrentUser();
            } else if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                int currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                UserInfo userInfo2 = UserSwitcherController.this.mUserManager.getUserInfo(currentId);
                int N = UserSwitcherController.this.mUsers.size();
                int i = 0;
                while (i < N) {
                    UserRecord record = (UserRecord) UserSwitcherController.this.mUsers.get(i);
                    if (record.info != null) {
                        boolean shouldBeCurrent = record.info.id == currentId;
                        if (record.isCurrent != shouldBeCurrent) {
                            UserSwitcherController.this.mUsers.set(i, record.copyWithIsCurrent(shouldBeCurrent));
                        }
                        if (shouldBeCurrent && !record.isGuest) {
                            UserSwitcherController.this.mLastNonGuestUser = record.info.id;
                        }
                        if ((userInfo2 == null || !userInfo2.isAdmin()) && record.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i);
                            i--;
                        }
                    }
                    i++;
                }
                UserSwitcherController.this.notifyAdapters();
                if (UserSwitcherController.this.mSecondaryUser != -10000) {
                    context.stopServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(UserSwitcherController.this.mSecondaryUser));
                    UserSwitcherController.this.mSecondaryUser = -10000;
                }
                if (userInfo2 != null && !userInfo2.isPrimary()) {
                    context.startServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(userInfo2.id));
                    UserSwitcherController.this.mSecondaryUser = userInfo2.id;
                }
                if (UserManager.isSplitSystemUser() && userInfo2 != null && !userInfo2.isGuest() && userInfo2.id != 0) {
                    showLogoutNotification(currentId);
                }
                if (userInfo2 != null && userInfo2.isGuest()) {
                    UserSwitcherController.this.showGuestNotification(currentId);
                }
                unpauseRefreshUsers = true;
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                forcePictureLoadForId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            } else if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
                if (userId != 0) {
                    return;
                }
            }
            UserSwitcherController.this.refreshUsers(forcePictureLoadForId);
            if (!unpauseRefreshUsers) {
                return;
            }
            UserSwitcherController.this.mUnpauseRefreshUsers.run();
        }

        private void showLogoutNotification(int userId) {
            PendingIntent logoutPI = PendingIntent.getBroadcastAsUser(UserSwitcherController.this.mContext, 0, new Intent("com.android.systemui.LOGOUT_USER"), 0, UserHandle.SYSTEM);
            Notification.Builder builder = new Notification.Builder(UserSwitcherController.this.mContext).setVisibility(-1).setPriority(-2).setSmallIcon(R.drawable.ic_person).setContentTitle(UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_title)).setContentText(UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_text)).setContentIntent(logoutPI).setOngoing(true).setShowWhen(false).addAction(R.drawable.ic_delete, UserSwitcherController.this.mContext.getString(R.string.user_logout_notification_action), logoutPI);
            SystemUI.overrideNotificationAppName(UserSwitcherController.this.mContext, builder);
            NotificationManager.from(UserSwitcherController.this.mContext).notifyAsUser("logout_user", 1011, builder.build(), new UserHandle(userId));
        }
    };
    private final Runnable mUnpauseRefreshUsers = new Runnable() {
        @Override
        public void run() {
            UserSwitcherController.this.mHandler.removeCallbacks(this);
            UserSwitcherController.this.mPauseRefreshUsers = false;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            UserSwitcherController.this.mSimpleUserSwitcher = Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0;
            UserSwitcherController.this.mAddUsersWhenLocked = Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "add_users_when_locked", 0) != 0;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    public final QSTile.DetailAdapter userDetailAdapter = new QSTile.DetailAdapter() {
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        @Override
        public CharSequence getTitle() {
            return UserSwitcherController.this.mContext.getString(R.string.quick_settings_user_title);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            UserDetailView v;
            if (!(convertView instanceof UserDetailView)) {
                v = UserDetailView.inflate(context, parent, false);
                v.createAndSetAdapter(UserSwitcherController.this);
            } else {
                v = (UserDetailView) convertView;
            }
            v.refreshAdapter();
            return v;
        }

        @Override
        public Intent getSettingsIntent() {
            if (BenesseExtension.getDchaState() != 0) {
                return null;
            }
            return this.USER_SETTINGS_INTENT;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public void setToggleState(boolean state) {
        }

        @Override
        public int getMetricsCategory() {
            return 125;
        }
    };
    private final KeyguardMonitor.Callback mCallback = new KeyguardMonitor.Callback() {
        @Override
        public void onKeyguardChanged() {
            UserSwitcherController.this.notifyAdapters();
        }
    };

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler, ActivityStarter activityStarter) {
        this.mContext = context;
        this.mGuestResumeSessionReceiver.register(context);
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mActivityStarter = activityStarter;
        this.mUserManager = UserManager.get(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_STOPPED");
        filter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, filter, null, null);
        this.mSecondaryUserServiceIntent = new Intent(context, (Class<?>) SystemUISecondaryUserService.class);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction("com.android.systemui.REMOVE_GUEST");
        filter2.addAction("com.android.systemui.LOGOUT_USER");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, filter2, "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("allow_user_switching_when_system_user_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        listenForCallState();
        refreshUsers(-10000);
    }

    public void refreshUsers(int forcePictureLoadForId) {
        if (forcePictureLoadForId != -10000) {
            this.mForcePictureLoadForUserId.put(forcePictureLoadForId, true);
        }
        if (this.mPauseRefreshUsers) {
            return;
        }
        boolean forceAllUsers = this.mForcePictureLoadForUserId.get(-1);
        SparseArray<Bitmap> bitmaps = new SparseArray<>(this.mUsers.size());
        int N = this.mUsers.size();
        for (int i = 0; i < N; i++) {
            UserRecord r = this.mUsers.get(i);
            if (r != null && r.picture != null && r.info != null && !forceAllUsers && !this.mForcePictureLoadForUserId.get(r.info.id)) {
                bitmaps.put(r.info.id, r.picture);
            }
        }
        this.mForcePictureLoadForUserId.clear();
        final boolean addUsersWhenLocked = this.mAddUsersWhenLocked;
        new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
            @Override
            public ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... params) {
                boolean z;
                boolean zCanAddMoreUsers;
                SparseArray<Bitmap> bitmaps2 = params[0];
                List<UserInfo> infos = UserSwitcherController.this.mUserManager.getUsers(true);
                if (infos == null) {
                    return null;
                }
                ArrayList<UserRecord> records = new ArrayList<>(infos.size());
                int currentId = ActivityManager.getCurrentUser();
                boolean canSwitchUsers = UserSwitcherController.this.mUserManager.canSwitchUsers();
                UserInfo currentUserInfo = null;
                UserRecord guestRecord = null;
                for (UserInfo info : infos) {
                    boolean isCurrent = currentId == info.id;
                    if (isCurrent) {
                        currentUserInfo = info;
                    }
                    boolean z2 = !canSwitchUsers ? isCurrent : true;
                    if (info.isEnabled()) {
                        if (info.isGuest()) {
                            guestRecord = new UserRecord(info, null, true, isCurrent, false, false, canSwitchUsers);
                        } else if (info.supportsSwitchToByUser()) {
                            Bitmap picture = bitmaps2.get(info.id);
                            if (picture == null && (picture = UserSwitcherController.this.mUserManager.getUserIcon(info.id)) != null) {
                                int avatarSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(R.dimen.max_avatar_size);
                                picture = Bitmap.createScaledBitmap(picture, avatarSize, avatarSize, true);
                            }
                            int index = isCurrent ? 0 : records.size();
                            records.add(index, new UserRecord(info, picture, false, isCurrent, false, false, z2));
                        }
                    }
                }
                boolean systemCanCreateUsers = !UserSwitcherController.this.mUserManager.hasBaseUserRestriction("no_add_user", UserHandle.SYSTEM);
                if (currentUserInfo == null || (!currentUserInfo.isAdmin() && currentUserInfo.id != 0)) {
                    z = false;
                } else {
                    z = systemCanCreateUsers;
                }
                boolean z3 = systemCanCreateUsers ? addUsersWhenLocked : false;
                boolean canCreateGuest = (z || z3) && guestRecord == null;
                if (!z && !z3) {
                    zCanAddMoreUsers = false;
                } else {
                    zCanAddMoreUsers = UserSwitcherController.this.mUserManager.canAddMoreUsers();
                }
                boolean createIsRestricted = !addUsersWhenLocked;
                if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                    if (guestRecord == null) {
                        if (canCreateGuest) {
                            UserRecord guestRecord2 = new UserRecord(null, null, true, false, false, createIsRestricted, canSwitchUsers);
                            UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(guestRecord2);
                            records.add(guestRecord2);
                        }
                    } else {
                        int index2 = guestRecord.isCurrent ? 0 : records.size();
                        records.add(index2, guestRecord);
                    }
                }
                if (!UserSwitcherController.this.mSimpleUserSwitcher && zCanAddMoreUsers) {
                    UserRecord addUserRecord = new UserRecord(null, null, false, false, true, createIsRestricted, canSwitchUsers);
                    UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(addUserRecord);
                    records.add(addUserRecord);
                }
                return records;
            }

            @Override
            public void onPostExecute(ArrayList<UserRecord> userRecords) {
                if (userRecords == null) {
                    return;
                }
                UserSwitcherController.this.mUsers = userRecords;
                UserSwitcherController.this.notifyAdapters();
            }
        }.execute(bitmaps);
    }

    private void pauseRefreshUsers() {
        if (this.mPauseRefreshUsers) {
            return;
        }
        this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000L);
        this.mPauseRefreshUsers = true;
    }

    public void notifyAdapters() {
        for (int i = this.mAdapters.size() - 1; i >= 0; i--) {
            BaseUserAdapter adapter = this.mAdapters.get(i).get();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(i);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public boolean useFullscreenUserSwitcher() {
        int overrideUseFullscreenUserSwitcher = Settings.System.getInt(this.mContext.getContentResolver(), "enable_fullscreen_user_switcher", -1);
        if (overrideUseFullscreenUserSwitcher != -1) {
            return overrideUseFullscreenUserSwitcher != 0;
        }
        return this.mContext.getResources().getBoolean(R.bool.config_enableFullscreenUserSwitcher);
    }

    public void logoutCurrentUser() {
        int currentUser = ActivityManager.getCurrentUser();
        if (currentUser == 0) {
            return;
        }
        pauseRefreshUsers();
        ActivityManager.logoutCurrentUser();
    }

    public void removeUserId(int userId) {
        if (userId == 0) {
            Log.w("UserSwitcherController", "User " + userId + " could not removed.");
            return;
        }
        if (ActivityManager.getCurrentUser() == userId) {
            switchToUserId(0);
        }
        if (!this.mUserManager.removeUser(userId)) {
            return;
        }
        refreshUsers(-10000);
    }

    public void switchTo(UserRecord record) {
        int id;
        if (record.isGuest && record.info == null) {
            UserInfo guest = this.mUserManager.createGuest(this.mContext, this.mContext.getString(R.string.guest_nickname));
            if (guest == null) {
                return;
            } else {
                id = guest.id;
            }
        } else {
            if (record.isAddUser) {
                showAddUserDialog();
                return;
            }
            id = record.info.id;
        }
        if (ActivityManager.getCurrentUser() == id) {
            if (record.isGuest) {
                showExitGuestDialog(id);
                return;
            }
            return;
        }
        switchToUserId(id);
    }

    public void switchToUserId(int id) {
        try {
            pauseRefreshUsers();
            ActivityManagerNative.getDefault().switchUser(id);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    public void showExitGuestDialog(int id) {
        if (this.mExitGuestDialog != null && this.mExitGuestDialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, id);
        this.mExitGuestDialog.show();
    }

    private void showAddUserDialog() {
        if (this.mAddUserDialog != null && this.mAddUserDialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        this.mAddUserDialog = new AddUserDialog(this.mContext);
        this.mAddUserDialog.show();
    }

    public void exitGuest(int id) {
        UserInfo info;
        int newId = 0;
        if (this.mLastNonGuestUser != 0 && (info = this.mUserManager.getUserInfo(this.mLastNonGuestUser)) != null && info.isEnabled() && info.supportsSwitchToByUser()) {
            newId = info.id;
        }
        switchToUserId(newId);
        this.mUserManager.removeUser(id);
    }

    private void listenForCallState() {
        TelephonyManager telephonyManagerFrom = TelephonyManager.from(this.mContext);
        PhoneStateListener phoneStateListener = new PhoneStateListener() {
            private int mCallState;

            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (this.mCallState == state) {
                    return;
                }
                this.mCallState = state;
                int currentUserId = ActivityManager.getCurrentUser();
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(currentUserId);
                if (userInfo != null && userInfo.isGuest()) {
                    UserSwitcherController.this.showGuestNotification(currentUserId);
                }
                UserSwitcherController.this.refreshUsers(-10000);
            }
        };
        this.mPhoneStateListener = phoneStateListener;
        telephonyManagerFrom.listen(phoneStateListener, 32);
    }

    public void showGuestNotification(int guestUserId) {
        boolean canSwitchUsers = this.mUserManager.canSwitchUsers();
        PendingIntent broadcastAsUser = canSwitchUsers ? PendingIntent.getBroadcastAsUser(this.mContext, 0, new Intent("com.android.systemui.REMOVE_GUEST"), 0, UserHandle.SYSTEM) : null;
        Notification.Builder builder = new Notification.Builder(this.mContext).setVisibility(-1).setPriority(-2).setSmallIcon(R.drawable.ic_person).setContentTitle(this.mContext.getString(R.string.guest_notification_title)).setContentText(this.mContext.getString(R.string.guest_notification_text)).setContentIntent(broadcastAsUser).setShowWhen(false).addAction(R.drawable.ic_delete, this.mContext.getString(R.string.guest_notification_remove_action), broadcastAsUser);
        SystemUI.overrideNotificationAppName(this.mContext, builder);
        NotificationManager.from(this.mContext).notifyAsUser("remove_guest", 1010, builder.build(), new UserHandle(guestUserId));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UserSwitcherController state:");
        pw.println("  mLastNonGuestUser=" + this.mLastNonGuestUser);
        pw.print("  mUsers.size=");
        pw.println(this.mUsers.size());
        for (int i = 0; i < this.mUsers.size(); i++) {
            UserRecord u = this.mUsers.get(i);
            pw.print("    ");
            pw.println(u.toString());
        }
    }

    public String getCurrentUserName(Context context) {
        UserRecord item;
        if (this.mUsers.isEmpty() || (item = this.mUsers.get(0)) == null || item.info == null) {
            return null;
        }
        return item.isGuest ? context.getString(R.string.guest_nickname) : item.info.name;
    }

    public void onDensityOrFontScaleChanged() {
        refreshUsers(-1);
    }

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;

        protected BaseUserAdapter(UserSwitcherController controller) {
            this.mController = controller;
            controller.mAdapters.add(new WeakReference(this));
        }

        @Override
        public int getCount() {
            boolean secureKeyguardShowing = false;
            if (this.mController.mKeyguardMonitor.isShowing() && this.mController.mKeyguardMonitor.isSecure() && !this.mController.mKeyguardMonitor.canSkipBouncer()) {
                secureKeyguardShowing = true;
            }
            if (!secureKeyguardShowing) {
                return this.mController.mUsers.size();
            }
            int N = this.mController.mUsers.size();
            int count = 0;
            for (int i = 0; i < N && !((UserRecord) this.mController.mUsers.get(i)).isRestricted; i++) {
                count++;
            }
            return count;
        }

        @Override
        public UserRecord getItem(int position) {
            return (UserRecord) this.mController.mUsers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void switchTo(UserRecord record) {
            this.mController.switchTo(record);
        }

        public String getName(Context context, UserRecord item) {
            if (item.isGuest) {
                if (item.isCurrent) {
                    return context.getString(R.string.guest_exit_guest);
                }
                return context.getString(item.info == null ? R.string.guest_new_guest : R.string.guest_nickname);
            }
            if (item.isAddUser) {
                return context.getString(R.string.user_add_user);
            }
            return item.info.name;
        }

        public Drawable getDrawable(Context context, UserRecord item) {
            if (item.isAddUser) {
                return context.getDrawable(R.drawable.ic_add_circle_qs);
            }
            return UserIcons.getDefaultUserIcon(item.resolveId(), true);
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    public void checkIfAddUserDisallowedByAdminOnly(UserRecord record) {
        RestrictedLockUtils.EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_add_user", ActivityManager.getCurrentUser());
        if (admin != null && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_add_user", ActivityManager.getCurrentUser())) {
            record.isDisabledByAdmin = true;
            record.enforcedAdmin = admin;
        } else {
            record.isDisabledByAdmin = false;
            record.enforcedAdmin = null;
        }
    }

    public void startActivity(Intent intent) {
        this.mActivityStarter.startActivity(intent, true);
    }

    public static final class UserRecord {
        public RestrictedLockUtils.EnforcedAdmin enforcedAdmin;
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public boolean isDisabledByAdmin;
        public final boolean isGuest;
        public final boolean isRestricted;
        public boolean isSwitchToEnabled;
        public final Bitmap picture;

        public UserRecord(UserInfo info, Bitmap picture, boolean isGuest, boolean isCurrent, boolean isAddUser, boolean isRestricted, boolean isSwitchToEnabled) {
            this.info = info;
            this.picture = picture;
            this.isGuest = isGuest;
            this.isCurrent = isCurrent;
            this.isAddUser = isAddUser;
            this.isRestricted = isRestricted;
            this.isSwitchToEnabled = isSwitchToEnabled;
        }

        public UserRecord copyWithIsCurrent(boolean _isCurrent) {
            return new UserRecord(this.info, this.picture, this.isGuest, _isCurrent, this.isAddUser, this.isRestricted, this.isSwitchToEnabled);
        }

        public int resolveId() {
            if (this.isGuest || this.info == null) {
                return -10000;
            }
            return this.info.id;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"").append(this.info.name).append("\" id=").append(this.info.id);
            } else if (this.isGuest) {
                sb.append("<add guest placeholder>");
            } else if (this.isAddUser) {
                sb.append("<add user placeholder>");
            }
            if (this.isGuest) {
                sb.append(" <isGuest>");
            }
            if (this.isAddUser) {
                sb.append(" <isAddUser>");
            }
            if (this.isCurrent) {
                sb.append(" <isCurrent>");
            }
            if (this.picture != null) {
                sb.append(" <hasPicture>");
            }
            if (this.isRestricted) {
                sb.append(" <isRestricted>");
            }
            if (this.isDisabledByAdmin) {
                sb.append(" <isDisabledByAdmin>");
                sb.append(" enforcedAdmin=").append(this.enforcedAdmin);
            }
            if (this.isSwitchToEnabled) {
                sb.append(" <isSwitchToEnabled>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    private final class ExitGuestDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mGuestId;

        public ExitGuestDialog(Context context, int guestId) {
            super(context);
            setTitle(R.string.guest_exit_guest_dialog_title);
            setMessage(context.getString(R.string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(android.R.string.cancel), this);
            setButton(-1, context.getString(R.string.guest_exit_guest_dialog_remove), this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = guestId;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                cancel();
            } else {
                dismiss();
                UserSwitcherController.this.exitGuest(this.mGuestId);
            }
        }
    }

    private final class AddUserDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        public AddUserDialog(Context context) {
            super(context);
            setTitle(R.string.user_add_user_title);
            setMessage(context.getString(R.string.user_add_user_message_short));
            setButton(-2, context.getString(android.R.string.cancel), this);
            setButton(-1, context.getString(android.R.string.ok), this);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            UserInfo user;
            if (which == -2) {
                cancel();
                return;
            }
            dismiss();
            if (ActivityManager.isUserAMonkey() || (user = UserSwitcherController.this.mUserManager.createUser(UserSwitcherController.this.mContext.getString(R.string.user_new_user_name), 0)) == null) {
                return;
            }
            int id = user.id;
            Bitmap icon = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(id, false));
            UserSwitcherController.this.mUserManager.setUserIcon(id, icon);
            UserSwitcherController.this.switchToUserId(id);
        }
    }
}
