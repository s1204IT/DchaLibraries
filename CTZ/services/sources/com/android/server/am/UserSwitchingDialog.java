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

class UserSwitchingDialog extends AlertDialog implements ViewTreeObserver.OnWindowShownListener {
    private static final int MSG_START_USER = 1;
    private static final String TAG = "ActivityManagerUserSwitchingDialog";
    private static final int WINDOW_SHOWN_TIMEOUT_MS = 3000;
    protected final Context mContext;
    private final Handler mHandler;
    protected final UserInfo mNewUser;
    protected final UserInfo mOldUser;
    private final ActivityManagerService mService;

    @GuardedBy("this")
    private boolean mStartedUser;
    private final String mSwitchingFromSystemUserMessage;
    private final String mSwitchingToSystemUserMessage;
    private final int mUserId;

    public UserSwitchingDialog(ActivityManagerService activityManagerService, Context context, UserInfo userInfo, UserInfo userInfo2, boolean z, String str, String str2) {
        super(context);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == 1) {
                    UserSwitchingDialog.this.startUser();
                }
            }
        };
        this.mContext = context;
        this.mService = activityManagerService;
        this.mUserId = userInfo2.id;
        this.mOldUser = userInfo;
        this.mNewUser = userInfo2;
        this.mSwitchingFromSystemUserMessage = str;
        this.mSwitchingToSystemUserMessage = str2;
        inflateContent();
        if (z) {
            getWindow().setType(2010);
        }
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.privateFlags = 272;
        getWindow().setAttributes(attributes);
    }

    void inflateContent() {
        String string;
        setCancelable(false);
        Resources resources = getContext().getResources();
        String str = null;
        View viewInflate = LayoutInflater.from(getContext()).inflate(R.layout.preference_list_fragment, (ViewGroup) null);
        if (UserManager.isSplitSystemUser() && this.mNewUser.id == 0) {
            string = resources.getString(R.string.mmcc_illegal_me, this.mOldUser.name);
        } else if (UserManager.isDeviceInDemoMode(this.mContext)) {
            if (this.mOldUser.isDemo()) {
                string = resources.getString(R.string.biometric_dialog_default_title);
            } else {
                string = resources.getString(R.string.biometric_error_canceled);
            }
        } else {
            if (this.mOldUser.id == 0) {
                str = this.mSwitchingFromSystemUserMessage;
            } else if (this.mNewUser.id == 0) {
                str = this.mSwitchingToSystemUserMessage;
            }
            if (str == null) {
                string = resources.getString(R.string.mmcc_illegal_ms_msim_template, this.mNewUser.name);
            } else {
                string = str;
            }
        }
        ((TextView) viewInflate.findViewById(R.id.message)).setText(string);
        setView(viewInflate);
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
                this.mService.mUserController.startUserInForeground(this.mUserId);
                dismiss();
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
