package com.android.systemui;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManagerGlobal;
import com.android.systemui.statusbar.phone.SystemUIDialog;

public class GuestResumeSessionReceiver extends BroadcastReceiver {
    private Dialog mNewSessionDialog;

    public void register(Context context) {
        IntentFilter f = new IntentFilter("android.intent.action.USER_SWITCHED");
        context.registerReceiverAsUser(this, UserHandle.SYSTEM, f, null, null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!"android.intent.action.USER_SWITCHED".equals(action)) {
            return;
        }
        cancelDialog();
        int userId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
        if (userId == -10000) {
            Log.e("GuestResumeSessionReceiver", intent + " sent to GuestResumeSessionReceiver without EXTRA_USER_HANDLE");
            return;
        }
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (!currentUser.isGuest()) {
                return;
            }
            ContentResolver cr = context.getContentResolver();
            int notFirstLogin = Settings.System.getIntForUser(cr, "systemui.guest_has_logged_in", 0, userId);
            if (notFirstLogin != 0) {
                this.mNewSessionDialog = new ResetSessionDialog(context, userId);
                this.mNewSessionDialog.show();
            } else {
                Settings.System.putIntForUser(cr, "systemui.guest_has_logged_in", 1, userId);
            }
        } catch (RemoteException e) {
        }
    }

    public static void wipeGuestSession(Context context, int userId) {
        UserManager userManager = (UserManager) context.getSystemService("user");
        try {
            UserInfo currentUser = ActivityManagerNative.getDefault().getCurrentUser();
            if (currentUser.id != userId) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ") is not current user (" + currentUser.id + ")");
                return;
            }
            if (!currentUser.isGuest()) {
                Log.w("GuestResumeSessionReceiver", "User requesting to start a new session (" + userId + ") is not a guest");
                return;
            }
            boolean marked = userManager.markGuestForDeletion(currentUser.id);
            if (!marked) {
                Log.w("GuestResumeSessionReceiver", "Couldn't mark the guest for deletion for user " + userId);
                return;
            }
            UserInfo newGuest = userManager.createGuest(context, currentUser.name);
            try {
                if (newGuest != null) {
                    ActivityManagerNative.getDefault().switchUser(newGuest.id);
                    userManager.removeUser(currentUser.id);
                } else {
                    Log.e("GuestResumeSessionReceiver", "Could not create new guest, switching back to system user");
                    ActivityManagerNative.getDefault().switchUser(0);
                    userManager.removeUser(currentUser.id);
                    WindowManagerGlobal.getWindowManagerService().lockNow((Bundle) null);
                }
            } catch (RemoteException e) {
                Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager or WindowManager is dead");
            }
        } catch (RemoteException e2) {
            Log.e("GuestResumeSessionReceiver", "Couldn't wipe session because ActivityManager is dead");
        }
    }

    private void cancelDialog() {
        if (this.mNewSessionDialog == null || !this.mNewSessionDialog.isShowing()) {
            return;
        }
        this.mNewSessionDialog.cancel();
        this.mNewSessionDialog = null;
    }

    private static class ResetSessionDialog extends SystemUIDialog implements DialogInterface.OnClickListener {
        private final int mUserId;

        public ResetSessionDialog(Context context, int userId) {
            super(context);
            setTitle(context.getString(R.string.guest_wipe_session_title));
            setMessage(context.getString(R.string.guest_wipe_session_message));
            setCanceledOnTouchOutside(false);
            setButton(-2, context.getString(R.string.guest_wipe_session_wipe), this);
            setButton(-1, context.getString(R.string.guest_wipe_session_dontwipe), this);
            this.mUserId = userId;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == -2) {
                GuestResumeSessionReceiver.wipeGuestSession(getContext(), this.mUserId);
                dismiss();
            } else {
                if (which != -1) {
                    return;
                }
                cancel();
            }
        }
    }
}
