package com.android.server.am;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.annotations.GuardedBy;
import com.android.server.input.InputManagerService;

final class UserSwitchingDialog extends AlertDialog implements ViewTreeObserver.OnWindowShownListener {
    private static final int MSG_START_USER = 1;
    private static final String TAG = "ActivityManagerUserSwitchingDialog";
    private static final int WINDOW_SHOWN_TIMEOUT_MS = 3000;
    private final Handler mHandler;
    private final ActivityManagerService mService;

    @GuardedBy("this")
    private boolean mStartedUser;
    private final int mUserId;

    public UserSwitchingDialog(ActivityManagerService service, Context context, UserInfo oldUser, UserInfo newUser, boolean aboveSystem) {
        String viewMessage;
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        UserSwitchingDialog.this.startUser();
                        break;
                }
            }
        };
        this.mService = service;
        this.mUserId = newUser.id;
        setCancelable(false);
        Resources res = getContext().getResources();
        View view = LayoutInflater.from(getContext()).inflate(R.layout.popup_menu_item_layout, (ViewGroup) null);
        if (UserManager.isSplitSystemUser() && newUser.id == 0) {
            viewMessage = res.getString(R.string.issued_to, oldUser.name);
        } else {
            viewMessage = res.getString(R.string.issued_on, newUser.name);
        }
        ((TextView) view.findViewById(R.id.message)).setText(viewMessage);
        setView(view);
        if (aboveSystem) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.privateFlags = InputManagerService.BTN_MOUSE;
        getWindow().setAttributes(attrs);
    }

    @Override
    public void show() {
        super.show();
        View decorView = getWindow().getDecorView();
        if (decorView != null) {
            decorView.getViewTreeObserver().addOnWindowShownListener(this);
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 3000L);
    }

    public void onWindowShown() {
        startUser();
    }

    void startUser() {
        synchronized (this) {
            if (!this.mStartedUser) {
                this.mService.mUserController.startUserInForeground(this.mUserId, this);
                this.mStartedUser = true;
                View decorView = getWindow().getDecorView();
                if (decorView != null) {
                    decorView.getViewTreeObserver().removeOnWindowShownListener(this);
                }
                this.mHandler.removeMessages(1);
            }
        }
    }
}
