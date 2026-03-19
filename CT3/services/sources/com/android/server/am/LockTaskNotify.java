package com.android.server.am;

import android.R;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class LockTaskNotify {
    private static final String TAG = "LockTaskNotify";
    private final Context mContext;
    private final H mHandler = new H(this, null);
    private Toast mLastToast;

    public LockTaskNotify(Context context) {
        this.mContext = context;
    }

    public void showToast(int lockTaskModeState) {
        this.mHandler.obtainMessage(3, lockTaskModeState, 0).sendToTarget();
    }

    public void handleShowToast(int lockTaskModeState) {
        String text = null;
        if (lockTaskModeState == 1) {
            text = this.mContext.getString(R.string.lockscreen_carrier_default);
        } else if (lockTaskModeState == 2) {
            text = this.mContext.getString(R.string.lockscreen_access_pattern_start);
        }
        if (text == null) {
            return;
        }
        if (this.mLastToast != null) {
            this.mLastToast.cancel();
        }
        this.mLastToast = makeAllUserToastAndShow(text);
    }

    public void show(boolean starting) {
        int showString = R.string.lockscreen_failed_attempts_almost_at_wipe;
        if (starting) {
            showString = R.string.lockscreen_emergency_call;
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

        H(LockTaskNotify this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    LockTaskNotify.this.handleShowToast(msg.arg1);
                    break;
            }
        }
    }
}
