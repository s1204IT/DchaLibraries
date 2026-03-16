package com.android.server.am;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

public class LockTaskNotify {
    private static final String TAG = "LockTaskNotify";
    private AccessibilityManager mAccessibilityManager;
    private final Context mContext;
    private final H mHandler = new H();
    private Toast mLastToast;

    public LockTaskNotify(Context context) {
        this.mContext = context;
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
    }

    public void showToast(boolean isLocked) {
        this.mHandler.obtainMessage(3, isLocked ? 1 : 0, 0).sendToTarget();
    }

    public void handleShowToast(boolean isLocked) {
        String text = this.mContext.getString(isLocked ? R.string.nas_upgrade_notification_learn_more_content : R.string.nas_upgrade_notification_enable_action);
        if (!isLocked && (this.mAccessibilityManager.isEnabled() || !this.mContext.getPackageManager().hasSystemFeature("android.hardware.touchscreen.multitouch"))) {
            text = this.mContext.getString(R.string.nas_upgrade_notification_learn_more_action);
        }
        if (this.mLastToast != null) {
            this.mLastToast.cancel();
        }
        this.mLastToast = makeAllUserToastAndShow(text);
    }

    public void show(boolean starting) {
        int showString = R.string.needPuk;
        if (starting) {
            showString = R.string.nas_upgrade_notification_title;
        }
        makeAllUserToastAndShow(this.mContext.getString(showString));
    }

    private Toast makeAllUserToastAndShow(String text) {
        Toast toast = Toast.makeText(this.mContext, text, 1);
        toast.getWindowParams().privateFlags |= 16;
        toast.show();
        return toast;
    }

    private final class H extends Handler {
        private static final int SHOW_TOAST = 3;

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    LockTaskNotify.this.handleShowToast(msg.arg1 != 0);
                    break;
            }
        }
    }
}
