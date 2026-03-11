package com.mediatek.settings.cdma;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class OmhEventHandler extends Handler {
    private static OmhEventHandler mHandler;
    private Context mContext;
    private int mState;

    private OmhEventHandler(Context context) {
        super(context.getMainLooper());
        this.mState = 0;
        this.mContext = context;
    }

    private static synchronized void createInstance(Context context) {
        if (mHandler == null) {
            mHandler = new OmhEventHandler(context);
        }
    }

    public static OmhEventHandler getInstance(Context context) {
        if (mHandler == null) {
            createInstance(context);
        }
        return mHandler;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.d("OmhEventHandler", "handleMessage, msg = " + msg + ", while state = " + this.mState);
        switch (msg.what) {
            case 100:
                if (this.mState == 0) {
                    this.mState = 1;
                } else {
                    Log.w("OmhEventHandler", "SET_BUSY when state = " + this.mState);
                }
                break;
            case 101:
                if (this.mState == 0) {
                    if (msg.arg1 == 1000) {
                        this.mState = 3;
                        CdmaUtils.startOmhWarningDialog(this.mContext);
                    } else if (msg.arg1 == 1001) {
                        CdmaUtils.startOmhDataPickDialog(this.mContext, msg.arg2);
                    }
                } else if (this.mState == 1) {
                    if (msg.arg1 == 1000) {
                        this.mState = 2;
                    }
                } else {
                    Log.w("OmhEventHandler", "NEW_REQUEST when state = " + this.mState);
                }
                break;
            case 102:
                if (this.mState == 1) {
                    this.mState = 0;
                } else if (this.mState == 2) {
                    this.mState = 3;
                    CdmaUtils.startOmhWarningDialog(this.mContext);
                } else {
                    Log.w("OmhEventHandler", "CLEAR_BUSY when state = " + this.mState);
                }
                break;
            case 103:
                if (this.mState == 3) {
                    this.mState = 0;
                } else {
                    Log.w("OmhEventHandler", "FINISH_REQUEST when state = " + this.mState);
                    this.mState = 0;
                }
                break;
        }
    }
}
