package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
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
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.UserIcons;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.SystemUISecondaryUserService;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController {
    private final ActivityStarter mActivityStarter;
    private Dialog mAddUserDialog;
    private boolean mAddUsersWhenLocked;
    protected final Context mContext;
    private Dialog mExitGuestDialog;
    protected final Handler mHandler;
    private final KeyguardMonitor mKeyguardMonitor;
    private boolean mPauseRefreshUsers;
    private Intent mSecondaryUserServiceIntent;
    private boolean mSimpleUserSwitcher;
    protected final UserManager mUserManager;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    private ArrayList<UserRecord> mUsers = new ArrayList<>();
    private int mLastNonGuestUser = 0;
    private boolean mResumeUserOnGuestLogout = true;
    private int mSecondaryUser = -10000;
    private SparseBooleanArray mForcePictureLoadForUserId = new SparseBooleanArray(2);
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        private int mCallState;

        @Override
        public void onCallStateChanged(int i, String str) {
            if (this.mCallState == i) {
                return;
            }
            this.mCallState = i;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean z = false;
            int intExtra = -10000;
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                int intExtra2 = intent.getIntExtra("android.intent.extra.user_handle", -1);
                UserInfo userInfo = UserSwitcherController.this.mUserManager.getUserInfo(intExtra2);
                int size = UserSwitcherController.this.mUsers.size();
                int i = 0;
                while (i < size) {
                    UserRecord userRecord = (UserRecord) UserSwitcherController.this.mUsers.get(i);
                    if (userRecord.info != null) {
                        boolean z2 = userRecord.info.id == intExtra2;
                        if (userRecord.isCurrent != z2) {
                            UserSwitcherController.this.mUsers.set(i, userRecord.copyWithIsCurrent(z2));
                        }
                        if (z2 && !userRecord.isGuest) {
                            UserSwitcherController.this.mLastNonGuestUser = userRecord.info.id;
                        }
                        if ((userInfo == null || !userInfo.isAdmin()) && userRecord.isRestricted) {
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
                if (userInfo != null && userInfo.id != 0) {
                    context.startServiceAsUser(UserSwitcherController.this.mSecondaryUserServiceIntent, UserHandle.of(userInfo.id));
                    UserSwitcherController.this.mSecondaryUser = userInfo.id;
                }
                z = true;
            } else if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                intExtra = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            } else if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction()) && intent.getIntExtra("android.intent.extra.user_handle", -10000) != 0) {
                return;
            }
            UserSwitcherController.this.refreshUsers(intExtra);
            if (z) {
                UserSwitcherController.this.mUnpauseRefreshUsers.run();
            }
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
        public void onChange(boolean z) {
            UserSwitcherController.this.mSimpleUserSwitcher = Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "lockscreenSimpleUserSwitcher", 0) != 0;
            UserSwitcherController.this.mAddUsersWhenLocked = Settings.Global.getInt(UserSwitcherController.this.mContext.getContentResolver(), "add_users_when_locked", 0) != 0;
            UserSwitcherController.this.refreshUsers(-10000);
        }
    };
    public final DetailAdapter userDetailAdapter = new DetailAdapter() {
        private final Intent USER_SETTINGS_INTENT = new Intent("android.settings.USER_SETTINGS");

        @Override
        public CharSequence getTitle() {
            return UserSwitcherController.this.mContext.getString(R.string.quick_settings_user_title);
        }

        @Override
        public View createDetailView(Context context, View view, ViewGroup viewGroup) {
            UserDetailView userDetailViewInflate;
            if (!(view instanceof UserDetailView)) {
                userDetailViewInflate = UserDetailView.inflate(context, viewGroup, false);
                userDetailViewInflate.createAndSetAdapter(UserSwitcherController.this);
            } else {
                userDetailViewInflate = (UserDetailView) view;
            }
            userDetailViewInflate.refreshAdapter();
            return userDetailViewInflate;
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
        public void setToggleState(boolean z) {
        }

        @Override
        public int getMetricsCategory() {
            return 125;
        }
    };
    private final KeyguardMonitor.Callback mCallback = new AnonymousClass7();

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor, Handler handler, ActivityStarter activityStarter) {
        this.mContext = context;
        this.mGuestResumeSessionReceiver.register(context);
        this.mKeyguardMonitor = keyguardMonitor;
        this.mHandler = handler;
        this.mActivityStarter = activityStarter;
        this.mUserManager = UserManager.get(context);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_ADDED");
        intentFilter.addAction("android.intent.action.USER_REMOVED");
        intentFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter.addAction("android.intent.action.USER_SWITCHED");
        intentFilter.addAction("android.intent.action.USER_STOPPED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, intentFilter, null, null);
        this.mSecondaryUserServiceIntent = new Intent(context, (Class<?>) SystemUISecondaryUserService.class);
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.SYSTEM, new IntentFilter(), "com.android.systemui.permission.SELF", null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("allow_user_switching_when_system_user_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        listenForCallState();
        refreshUsers(-10000);
    }

    public void refreshUsers(int i) {
        if (i != -10000) {
            this.mForcePictureLoadForUserId.put(i, true);
        }
        if (this.mPauseRefreshUsers) {
            return;
        }
        boolean z = this.mForcePictureLoadForUserId.get(-1);
        SparseArray sparseArray = new SparseArray(this.mUsers.size());
        int size = this.mUsers.size();
        for (int i2 = 0; i2 < size; i2++) {
            UserRecord userRecord = this.mUsers.get(i2);
            if (userRecord != null && userRecord.picture != null && userRecord.info != null && !z && !this.mForcePictureLoadForUserId.get(userRecord.info.id)) {
                sparseArray.put(userRecord.info.id, userRecord.picture);
            }
        }
        this.mForcePictureLoadForUserId.clear();
        final boolean z2 = this.mAddUsersWhenLocked;
        new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
            @Override
            public ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... sparseArrayArr) {
                int size2;
                SparseArray<Bitmap> sparseArray2 = sparseArrayArr[0];
                List<UserInfo> users = UserSwitcherController.this.mUserManager.getUsers(true);
                UserRecord userRecord2 = null;
                if (users == null) {
                    return null;
                }
                ArrayList<UserRecord> arrayList = new ArrayList<>(users.size());
                int currentUser = ActivityManager.getCurrentUser();
                boolean zCanSwitchUsers = UserSwitcherController.this.mUserManager.canSwitchUsers();
                UserInfo userInfo = null;
                for (UserInfo userInfo2 : users) {
                    boolean z3 = currentUser == userInfo2.id;
                    UserInfo userInfo3 = z3 ? userInfo2 : userInfo;
                    boolean z4 = zCanSwitchUsers || z3;
                    if (userInfo2.isEnabled()) {
                        if (userInfo2.isGuest()) {
                            userRecord2 = new UserRecord(userInfo2, null, true, z3, false, false, zCanSwitchUsers);
                        } else if (userInfo2.supportsSwitchToByUser()) {
                            Bitmap userIcon = sparseArray2.get(userInfo2.id);
                            if (userIcon == null && (userIcon = UserSwitcherController.this.mUserManager.getUserIcon(userInfo2.id)) != null) {
                                int dimensionPixelSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(R.dimen.max_avatar_size);
                                userIcon = Bitmap.createScaledBitmap(userIcon, dimensionPixelSize, dimensionPixelSize, true);
                            }
                            Bitmap bitmap = userIcon;
                            if (!z3) {
                                size2 = arrayList.size();
                            } else {
                                size2 = 0;
                            }
                            arrayList.add(size2, new UserRecord(userInfo2, bitmap, false, z3, false, false, z4));
                        }
                    }
                    userInfo = userInfo3;
                }
                if (arrayList.size() > 1 || userRecord2 != null) {
                    Prefs.putBoolean(UserSwitcherController.this.mContext, "HasSeenMultiUser", true);
                }
                boolean z5 = !UserSwitcherController.this.mUserManager.hasBaseUserRestriction("no_add_user", UserHandle.SYSTEM);
                boolean z6 = userInfo != null && (userInfo.isAdmin() || userInfo.id == 0) && z5;
                boolean z7 = z5 && z2;
                boolean z8 = (z6 || z7) && userRecord2 == null;
                boolean z9 = (z6 || z7) && UserSwitcherController.this.mUserManager.canAddMoreUsers();
                boolean z10 = !z2;
                if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                    if (userRecord2 == null) {
                        if (z8) {
                            UserRecord userRecord3 = new UserRecord(null, null, true, false, false, z10, zCanSwitchUsers);
                            UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord3);
                            arrayList.add(userRecord3);
                        }
                    } else {
                        arrayList.add(userRecord2.isCurrent ? 0 : arrayList.size(), userRecord2);
                    }
                }
                if (!UserSwitcherController.this.mSimpleUserSwitcher && z9) {
                    UserRecord userRecord4 = new UserRecord(null, null, false, false, true, z10, zCanSwitchUsers);
                    UserSwitcherController.this.checkIfAddUserDisallowedByAdminOnly(userRecord4);
                    arrayList.add(userRecord4);
                }
                return arrayList;
            }

            @Override
            public void onPostExecute(ArrayList<UserRecord> arrayList) {
                if (arrayList != null) {
                    UserSwitcherController.this.mUsers = arrayList;
                    UserSwitcherController.this.notifyAdapters();
                }
            }
        }.execute(sparseArray);
    }

    private void pauseRefreshUsers() {
        if (!this.mPauseRefreshUsers) {
            this.mHandler.postDelayed(this.mUnpauseRefreshUsers, 3000L);
            this.mPauseRefreshUsers = true;
        }
    }

    public void notifyAdapters() {
        for (int size = this.mAdapters.size() - 1; size >= 0; size--) {
            BaseUserAdapter baseUserAdapter = this.mAdapters.get(size).get();
            if (baseUserAdapter != null) {
                baseUserAdapter.notifyDataSetChanged();
            } else {
                this.mAdapters.remove(size);
            }
        }
    }

    public boolean isSimpleUserSwitcher() {
        return this.mSimpleUserSwitcher;
    }

    public boolean useFullscreenUserSwitcher() {
        int i = Settings.System.getInt(this.mContext.getContentResolver(), "enable_fullscreen_user_switcher", -1);
        if (i != -1) {
            return i != 0;
        }
        return this.mContext.getResources().getBoolean(R.bool.config_enableFullscreenUserSwitcher);
    }

    public void switchTo(UserRecord userRecord) {
        int i;
        UserInfo userInfo;
        if (userRecord.isGuest && userRecord.info == null) {
            UserInfo userInfoCreateGuest = this.mUserManager.createGuest(this.mContext, this.mContext.getString(R.string.guest_nickname));
            if (userInfoCreateGuest == null) {
                return;
            } else {
                i = userInfoCreateGuest.id;
            }
        } else {
            if (userRecord.isAddUser) {
                showAddUserDialog();
                return;
            }
            i = userRecord.info.id;
        }
        int currentUser = ActivityManager.getCurrentUser();
        if (currentUser == i) {
            if (userRecord.isGuest) {
                showExitGuestDialog(i);
            }
        } else if (UserManager.isGuestUserEphemeral() && (userInfo = this.mUserManager.getUserInfo(currentUser)) != null && userInfo.isGuest()) {
            showExitGuestDialog(currentUser, userRecord.resolveId());
        } else {
            switchToUserId(i);
        }
    }

    public int getSwitchableUserCount() {
        int size = this.mUsers.size();
        int i = 0;
        for (int i2 = 0; i2 < size; i2++) {
            UserRecord userRecord = this.mUsers.get(i2);
            if (userRecord.info != null && userRecord.info.supportsSwitchTo()) {
                i++;
            }
        }
        return i;
    }

    protected void switchToUserId(int i) {
        try {
            pauseRefreshUsers();
            ActivityManager.getService().switchUser(i);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    private void showExitGuestDialog(int i) {
        int i2;
        UserInfo userInfo;
        if (this.mResumeUserOnGuestLogout && this.mLastNonGuestUser != 0 && (userInfo = this.mUserManager.getUserInfo(this.mLastNonGuestUser)) != null && userInfo.isEnabled() && userInfo.supportsSwitchToByUser()) {
            i2 = userInfo.id;
        } else {
            i2 = 0;
        }
        showExitGuestDialog(i, i2);
    }

    protected void showExitGuestDialog(int i, int i2) {
        if (this.mExitGuestDialog != null && this.mExitGuestDialog.isShowing()) {
            this.mExitGuestDialog.cancel();
        }
        this.mExitGuestDialog = new ExitGuestDialog(this.mContext, i, i2);
        this.mExitGuestDialog.show();
    }

    public void showAddUserDialog() {
        if (this.mAddUserDialog != null && this.mAddUserDialog.isShowing()) {
            this.mAddUserDialog.cancel();
        }
        this.mAddUserDialog = new AddUserDialog(this.mContext);
        this.mAddUserDialog.show();
    }

    protected void exitGuest(int i, int i2) {
        switchToUserId(i2);
        this.mUserManager.removeUser(i);
    }

    private void listenForCallState() {
        TelephonyManager.from(this.mContext).listen(this.mPhoneStateListener, 32);
    }

    public String getCurrentUserName(Context context) {
        UserRecord userRecord;
        if (this.mUsers.isEmpty() || (userRecord = this.mUsers.get(0)) == null || userRecord.info == null) {
            return null;
        }
        return userRecord.isGuest ? context.getString(R.string.guest_nickname) : userRecord.info.name;
    }

    public void onDensityOrFontScaleChanged() {
        refreshUsers(-1);
    }

    @VisibleForTesting
    public void addAdapter(WeakReference<BaseUserAdapter> weakReference) {
        this.mAdapters.add(weakReference);
    }

    @VisibleForTesting
    public ArrayList<UserRecord> getUsers() {
        return this.mUsers;
    }

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;
        private final KeyguardMonitor mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);

        protected BaseUserAdapter(UserSwitcherController userSwitcherController) {
            this.mController = userSwitcherController;
            userSwitcherController.addAdapter(new WeakReference<>(this));
        }

        @Override
        public int getCount() {
            if (!(this.mKeyguardMonitor.isShowing() && this.mKeyguardMonitor.isSecure() && !this.mKeyguardMonitor.canSkipBouncer())) {
                return this.mController.getUsers().size();
            }
            int size = this.mController.getUsers().size();
            int i = 0;
            for (int i2 = 0; i2 < size && !this.mController.getUsers().get(i2).isRestricted; i2++) {
                i++;
            }
            return i;
        }

        @Override
        public UserRecord getItem(int i) {
            return this.mController.getUsers().get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public void switchTo(UserRecord userRecord) {
            this.mController.switchTo(userRecord);
        }

        public String getName(Context context, UserRecord userRecord) {
            if (userRecord.isGuest) {
                if (userRecord.isCurrent) {
                    return context.getString(R.string.guest_exit_guest);
                }
                return context.getString(userRecord.info == null ? R.string.guest_new_guest : R.string.guest_nickname);
            }
            if (userRecord.isAddUser) {
                return context.getString(R.string.user_add_user);
            }
            return userRecord.info.name;
        }

        public Drawable getDrawable(Context context, UserRecord userRecord) {
            if (userRecord.isAddUser) {
                return context.getDrawable(R.drawable.ic_add_circle_qs);
            }
            Drawable defaultUserIcon = UserIcons.getDefaultUserIcon(context.getResources(), userRecord.resolveId(), false);
            if (userRecord.isGuest) {
                defaultUserIcon.setColorFilter(Utils.getColorAttr(context, android.R.attr.colorForeground), PorterDuff.Mode.SRC_IN);
            }
            return defaultUserIcon;
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    public void checkIfAddUserDisallowedByAdminOnly(UserRecord userRecord) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, "no_add_user", ActivityManager.getCurrentUser());
        if (enforcedAdminCheckIfRestrictionEnforced != null && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, "no_add_user", ActivityManager.getCurrentUser())) {
            userRecord.isDisabledByAdmin = true;
            userRecord.enforcedAdmin = enforcedAdminCheckIfRestrictionEnforced;
        } else {
            userRecord.isDisabledByAdmin = false;
            userRecord.enforcedAdmin = null;
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

        public UserRecord(UserInfo userInfo, Bitmap bitmap, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) {
            this.info = userInfo;
            this.picture = bitmap;
            this.isGuest = z;
            this.isCurrent = z2;
            this.isAddUser = z3;
            this.isRestricted = z4;
            this.isSwitchToEnabled = z5;
        }

        public UserRecord copyWithIsCurrent(boolean z) {
            return new UserRecord(this.info, this.picture, this.isGuest, z, this.isAddUser, this.isRestricted, this.isSwitchToEnabled);
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
                sb.append("name=\"");
                sb.append(this.info.name);
                sb.append("\" id=");
                sb.append(this.info.id);
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
                sb.append(" enforcedAdmin=");
                sb.append(this.enforcedAdmin);
            }
            if (this.isSwitchToEnabled) {
                sb.append(" <isSwitchToEnabled>");
            }
            sb.append(')');
            return sb.toString();
        }
    }

    class AnonymousClass7 implements KeyguardMonitor.Callback {
        AnonymousClass7() {
        }

        @Override
        public void onKeyguardShowingChanged() {
            if (UserSwitcherController.this.mKeyguardMonitor.isShowing()) {
                UserSwitcherController.this.notifyAdapters();
                return;
            }
            Handler handler = UserSwitcherController.this.mHandler;
            final UserSwitcherController userSwitcherController = UserSwitcherController.this;
            handler.post(new Runnable() {
                @Override
                public final void run() {
                    userSwitcherController.notifyAdapters();
                }
            });
        }
    }

    private final class ExitGuestDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mGuestId;
        private final int mTargetId;

        public ExitGuestDialog(Context context, int i, int i2) {
            super(context);
            setTitle(R.string.guest_exit_guest_dialog_title);
            setMessage(context.getString(R.string.guest_exit_guest_dialog_message));
            setButton(-2, context.getString(android.R.string.cancel), this);
            setButton(-1, context.getString(R.string.guest_exit_guest_dialog_remove), this);
            SystemUIDialog.setWindowOnTop(this);
            setCanceledOnTouchOutside(false);
            this.mGuestId = i;
            this.mTargetId = i2;
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            if (i == -2) {
                cancel();
            } else {
                dismiss();
                UserSwitcherController.this.exitGuest(this.mGuestId, this.mTargetId);
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
            SystemUIDialog.setWindowOnTop(this);
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            UserInfo userInfoCreateUser;
            if (i == -2) {
                cancel();
                return;
            }
            dismiss();
            if (ActivityManager.isUserAMonkey() || (userInfoCreateUser = UserSwitcherController.this.mUserManager.createUser(UserSwitcherController.this.mContext.getString(R.string.user_new_user_name), 0)) == null) {
                return;
            }
            int i2 = userInfoCreateUser.id;
            UserSwitcherController.this.mUserManager.setUserIcon(i2, UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(UserSwitcherController.this.mContext.getResources(), i2, false)));
            UserSwitcherController.this.switchToUserId(i2);
        }
    }
}
