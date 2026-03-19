package com.mediatek.server.telecom;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import com.mediatek.server.telecom.ext.ExtensionManager;

public class MtkTelecomGlobals {
    private static final String TAG = MtkTelecomGlobals.class.getSimpleName();
    private static MtkTelecomGlobals sInstance;
    private final Context mContext;
    private Handler mMainThreadHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                Toast.makeText(MtkTelecomGlobals.this.mContext, (String) message.obj, 0).show();
            }
        }
    };

    private MtkTelecomGlobals(Context context) {
        this.mContext = context;
        ExtensionManager.registerApplicationContext(this.mContext);
    }

    private void onCreate() {
        ExtensionManager.registerApplicationContext(this.mContext);
    }

    public Context getContext() {
        return this.mContext;
    }

    public void showToast(int i) {
        showToast(this.mContext.getString(i));
    }

    public void showToast(String str) {
        this.mMainThreadHandler.obtainMessage(1, str).sendToTarget();
    }

    public static synchronized void createInstance(Context context) {
        if (sInstance != null) {
            return;
        }
        sInstance = new MtkTelecomGlobals(context);
        sInstance.onCreate();
    }

    public static MtkTelecomGlobals getInstance() {
        return sInstance;
    }
}
