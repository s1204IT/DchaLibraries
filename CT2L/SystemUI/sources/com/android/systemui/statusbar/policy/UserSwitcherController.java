package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.Dialog;
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
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.android.internal.util.UserIcons;
import com.android.systemui.BitmapHelper;
import com.android.systemui.GuestResumeSessionReceiver;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.tiles.UserDetailView;
import com.android.systemui.statusbar.phone.SystemUIDialog;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class UserSwitcherController {
    private Dialog mAddUserDialog;
    private boolean mAddUsersWhenLocked;
    private final Context mContext;
    private Dialog mExitGuestDialog;
    private final KeyguardMonitor mKeyguardMonitor;
    private boolean mSimpleUserSwitcher;
    private final UserManager mUserManager;
    private final ArrayList<WeakReference<BaseUserAdapter>> mAdapters = new ArrayList<>();
    private final GuestResumeSessionReceiver mGuestResumeSessionReceiver = new GuestResumeSessionReceiver();
    private ArrayList<UserRecord> mUsers = new ArrayList<>();
    private int mLastNonGuestUser = 0;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                if (UserSwitcherController.this.mExitGuestDialog != null && UserSwitcherController.this.mExitGuestDialog.isShowing()) {
                    UserSwitcherController.this.mExitGuestDialog.cancel();
                    UserSwitcherController.this.mExitGuestDialog = null;
                }
                int currentId = intent.getIntExtra("android.intent.extra.user_handle", -1);
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
                        if (currentId != 0 && record.isRestricted) {
                            UserSwitcherController.this.mUsers.remove(i);
                            i--;
                        }
                    }
                    i++;
                }
                UserSwitcherController.this.notifyAdapters();
            }
            int forcePictureLoadForId = -10000;
            if ("android.intent.action.USER_INFO_CHANGED".equals(intent.getAction())) {
                forcePictureLoadForId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
            }
            UserSwitcherController.this.refreshUsers(forcePictureLoadForId);
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
        public int getTitle() {
            return R.string.quick_settings_user_title;
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
            return this.USER_SETTINGS_INTENT;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public void setToggleState(boolean state) {
        }
    };
    private final KeyguardMonitor.Callback mCallback = new KeyguardMonitor.Callback() {
        @Override
        public void onKeyguardChanged() {
            UserSwitcherController.this.notifyAdapters();
        }
    };

    public UserSwitcherController(Context context, KeyguardMonitor keyguardMonitor) {
        this.mContext = context;
        this.mGuestResumeSessionReceiver.register(context);
        this.mKeyguardMonitor = keyguardMonitor;
        this.mUserManager = UserManager.get(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_ADDED");
        filter.addAction("android.intent.action.USER_REMOVED");
        filter.addAction("android.intent.action.USER_INFO_CHANGED");
        filter.addAction("android.intent.action.USER_SWITCHED");
        filter.addAction("android.intent.action.USER_STOPPING");
        this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.OWNER, filter, null, null);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("lockscreenSimpleUserSwitcher"), true, this.mSettingsObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("add_users_when_locked"), true, this.mSettingsObserver);
        this.mSettingsObserver.onChange(false);
        keyguardMonitor.addCallback(this.mCallback);
        refreshUsers(-10000);
    }

    public void refreshUsers(int forcePictureLoadForId) {
        SparseArray<Bitmap> bitmaps = new SparseArray<>(this.mUsers.size());
        int N = this.mUsers.size();
        for (int i = 0; i < N; i++) {
            UserRecord r = this.mUsers.get(i);
            if (r != null && r.info != null && r.info.id != forcePictureLoadForId && r.picture != null) {
                bitmaps.put(r.info.id, r.picture);
            }
        }
        final boolean addUsersWhenLocked = this.mAddUsersWhenLocked;
        new AsyncTask<SparseArray<Bitmap>, Void, ArrayList<UserRecord>>() {
            @Override
            public ArrayList<UserRecord> doInBackground(SparseArray<Bitmap>... params) {
                SparseArray<Bitmap> bitmaps2 = params[0];
                List<UserInfo> infos = UserSwitcherController.this.mUserManager.getUsers(true);
                if (infos == null) {
                    return null;
                }
                ArrayList<UserRecord> records = new ArrayList<>(infos.size());
                int currentId = ActivityManager.getCurrentUser();
                UserRecord guestRecord = null;
                int avatarSize = UserSwitcherController.this.mContext.getResources().getDimensionPixelSize(R.dimen.max_avatar_size);
                for (UserInfo info : infos) {
                    boolean isCurrent = currentId == info.id;
                    if (info.isGuest()) {
                        guestRecord = new UserRecord(info, null, true, isCurrent, false, false);
                    } else if (info.supportsSwitchTo()) {
                        Bitmap picture = bitmaps2.get(info.id);
                        if (picture == null && (picture = UserSwitcherController.this.mUserManager.getUserIcon(info.id)) != null) {
                            picture = BitmapHelper.createCircularClip(picture, avatarSize, avatarSize);
                        }
                        int index = isCurrent ? 0 : records.size();
                        records.add(index, new UserRecord(info, picture, false, isCurrent, false, false));
                    }
                }
                boolean ownerCanCreateUsers = !UserSwitcherController.this.mUserManager.hasUserRestriction("no_add_user", UserHandle.OWNER);
                boolean currentUserCanCreateUsers = currentId == 0 && ownerCanCreateUsers;
                boolean anyoneCanCreateUsers = ownerCanCreateUsers && addUsersWhenLocked;
                boolean canCreateGuest = (currentUserCanCreateUsers || anyoneCanCreateUsers) && guestRecord == null;
                boolean canCreateUser = (currentUserCanCreateUsers || anyoneCanCreateUsers) && UserSwitcherController.this.mUserManager.canAddMoreUsers();
                boolean createIsRestricted = !addUsersWhenLocked;
                if (!UserSwitcherController.this.mSimpleUserSwitcher) {
                    if (guestRecord == null) {
                        if (canCreateGuest) {
                            records.add(new UserRecord(null, null, true, false, false, createIsRestricted));
                        }
                    } else {
                        int index2 = guestRecord.isCurrent ? 0 : records.size();
                        records.add(index2, guestRecord);
                    }
                }
                if (!UserSwitcherController.this.mSimpleUserSwitcher && canCreateUser) {
                    records.add(new UserRecord(null, null, false, false, true, createIsRestricted));
                    return records;
                }
                return records;
            }

            @Override
            public void onPostExecute(ArrayList<UserRecord> userRecords) {
                if (userRecords != null) {
                    UserSwitcherController.this.mUsers = userRecords;
                    UserSwitcherController.this.notifyAdapters();
                }
            }
        }.execute(bitmaps);
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

    public void switchTo(UserRecord record) {
        int id;
        if (record.isGuest && record.info == null) {
            UserInfo guest = this.mUserManager.createGuest(this.mContext, this.mContext.getString(R.string.guest_nickname));
            if (guest != null) {
                id = guest.id;
            } else {
                return;
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
            ActivityManagerNative.getDefault().switchUser(id);
        } catch (RemoteException e) {
            Log.e("UserSwitcherController", "Couldn't switch user.", e);
        }
    }

    private void showExitGuestDialog(int id) {
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
        if (this.mLastNonGuestUser != 0 && (info = this.mUserManager.getUserInfo(this.mLastNonGuestUser)) != null && info.isEnabled() && info.supportsSwitchTo()) {
            newId = info.id;
        }
        switchToUserId(newId);
        this.mUserManager.removeUser(id);
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

    public static abstract class BaseUserAdapter extends BaseAdapter {
        final UserSwitcherController mController;

        protected BaseUserAdapter(UserSwitcherController controller) {
            this.mController = controller;
            controller.mAdapters.add(new WeakReference(this));
        }

        @Override
        public int getCount() {
            boolean secureKeyguardShowing = this.mController.mKeyguardMonitor.isShowing() && this.mController.mKeyguardMonitor.isSecure();
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
            return UserIcons.getDefaultUserIcon(item.isGuest ? -10000 : item.info.id, true);
        }

        public void refresh() {
            this.mController.refreshUsers(-10000);
        }
    }

    public static final class UserRecord {
        public final UserInfo info;
        public final boolean isAddUser;
        public final boolean isCurrent;
        public final boolean isGuest;
        public final boolean isRestricted;
        public final Bitmap picture;

        public UserRecord(UserInfo info, Bitmap picture, boolean isGuest, boolean isCurrent, boolean isAddUser, boolean isRestricted) {
            this.info = info;
            this.picture = picture;
            this.isGuest = isGuest;
            this.isCurrent = isCurrent;
            this.isAddUser = isAddUser;
            this.isRestricted = isRestricted;
        }

        public UserRecord copyWithIsCurrent(boolean _isCurrent) {
            return new UserRecord(this.info, this.picture, this.isGuest, _isCurrent, this.isAddUser, this.isRestricted);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("UserRecord(");
            if (this.info != null) {
                sb.append("name=\"" + this.info.name + "\" id=" + this.info.id);
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
            if (!ActivityManager.isUserAMonkey() && (user = UserSwitcherController.this.mUserManager.createSecondaryUser(UserSwitcherController.this.mContext.getString(R.string.user_new_user_name), 0)) != null) {
                int id = user.id;
                Bitmap icon = UserIcons.convertToBitmap(UserIcons.getDefaultUserIcon(id, false));
                UserSwitcherController.this.mUserManager.setUserIcon(id, icon);
                UserSwitcherController.this.switchToUserId(id);
            }
        }
    }

    public static boolean isUserSwitcherAvailable(UserManager um) {
        return UserManager.supportsMultipleUsers() && um.isUserSwitcherEnabled();
    }
}
