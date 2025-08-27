package com.android.quickstep;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.android.launcher3.MainThreadExecutor;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.DiscoveryBounce;
import com.android.launcher3.util.UiThreadHelper;
import com.android.systemui.shared.recents.ISystemUiProxy;
import com.android.systemui.shared.system.SettingsCompat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/* loaded from: classes.dex */
public class OverviewInteractionState {
    private static final String HAS_ENABLED_QUICKSTEP_ONCE = "launcher.has_enabled_quickstep_once";
    private static OverviewInteractionState INSTANCE = null;
    private static final int MSG_SET_BACK_BUTTON_ALPHA = 201;
    private static final int MSG_SET_PROXY = 200;
    private static final int MSG_SET_SWIPE_UP_ENABLED = 202;
    private static final String SWIPE_UP_ENABLED_DEFAULT_RES_NAME = "config_swipe_up_gesture_default";
    private static final String SWIPE_UP_SETTING_AVAILABLE_RES_NAME = "config_swipe_up_gesture_setting_available";
    private static final String TAG = "OverviewFlags";
    private final Context mContext;
    private ISystemUiProxy mISystemUiProxy;
    private Runnable mOnSwipeUpSettingChangedListener;
    private boolean mSwipeUpEnabled;
    private final SwipeUpGestureEnabledSettingObserver mSwipeUpSettingObserver;
    private float mBackButtonAlpha = 1.0f;
    private final Handler mUiHandler = new Handler(new Handler.Callback() { // from class: com.android.quickstep.-$$Lambda$OverviewInteractionState$j9cvcxLwTdHZmfJdn7vbeqVDrmE
        @Override // android.os.Handler.Callback
        public final boolean handleMessage(Message message) {
            return this.f$0.handleUiMessage(message);
        }
    });
    private final Handler mBgHandler = new Handler(UiThreadHelper.getBackgroundLooper(), new Handler.Callback() { // from class: com.android.quickstep.-$$Lambda$OverviewInteractionState$lUCjlXPvcc5dI6thv7Aq9QPgFjM
        @Override // android.os.Handler.Callback
        public final boolean handleMessage(Message message) {
            return this.f$0.handleBgMessage(message);
        }
    });

    public static OverviewInteractionState getInstance(final Context context) {
        if (INSTANCE == null) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                INSTANCE = new OverviewInteractionState(context.getApplicationContext());
            } else {
                try {
                    return (OverviewInteractionState) new MainThreadExecutor().submit(new Callable() { // from class: com.android.quickstep.-$$Lambda$OverviewInteractionState$QjvJRsHSD02woBsyrz783sSx2jY
                        @Override // java.util.concurrent.Callable
                        public final Object call() {
                            return OverviewInteractionState.getInstance(context);
                        }
                    }).get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return INSTANCE;
    }

    private OverviewInteractionState(Context context) {
        this.mSwipeUpEnabled = true;
        this.mContext = context;
        if (getSystemBooleanRes(SWIPE_UP_SETTING_AVAILABLE_RES_NAME)) {
            this.mSwipeUpSettingObserver = new SwipeUpGestureEnabledSettingObserver(this.mUiHandler, context.getContentResolver());
            this.mSwipeUpSettingObserver.register();
        } else {
            this.mSwipeUpSettingObserver = null;
            this.mSwipeUpEnabled = getSystemBooleanRes(SWIPE_UP_ENABLED_DEFAULT_RES_NAME);
        }
    }

    public boolean isSwipeUpGestureEnabled() {
        return this.mSwipeUpEnabled;
    }

    public float getBackButtonAlpha() {
        return this.mBackButtonAlpha;
    }

    public void setBackButtonAlpha(float f, boolean z) {
        if (!this.mSwipeUpEnabled) {
            f = 1.0f;
        }
        this.mUiHandler.removeMessages(MSG_SET_BACK_BUTTON_ALPHA);
        this.mUiHandler.obtainMessage(MSG_SET_BACK_BUTTON_ALPHA, z ? 1 : 0, 0, Float.valueOf(f)).sendToTarget();
    }

    public void setSystemUiProxy(ISystemUiProxy iSystemUiProxy) {
        this.mBgHandler.obtainMessage(200, iSystemUiProxy).sendToTarget();
    }

    private boolean handleUiMessage(Message message) {
        if (message.what == MSG_SET_BACK_BUTTON_ALPHA) {
            this.mBackButtonAlpha = ((Float) message.obj).floatValue();
        }
        this.mBgHandler.obtainMessage(message.what, message.arg1, message.arg2, message.obj).sendToTarget();
        return true;
    }

    private boolean handleBgMessage(Message message) {
        switch (message.what) {
            case 200:
                this.mISystemUiProxy = (ISystemUiProxy) message.obj;
                break;
            case MSG_SET_BACK_BUTTON_ALPHA /* 201 */:
                applyBackButtonAlpha(((Float) message.obj).floatValue(), message.arg1 == 1);
                return true;
            case MSG_SET_SWIPE_UP_ENABLED /* 202 */:
                this.mSwipeUpEnabled = message.arg1 != 0;
                resetHomeBounceSeenOnQuickstepEnabledFirstTime();
                if (this.mOnSwipeUpSettingChangedListener != null) {
                    this.mOnSwipeUpSettingChangedListener.run();
                    break;
                }
                break;
        }
        applyFlags();
        return true;
    }

    public void setOnSwipeUpSettingChangedListener(Runnable runnable) {
        this.mOnSwipeUpSettingChangedListener = runnable;
    }

    @WorkerThread
    private void applyFlags() {
        if (this.mISystemUiProxy == null) {
            return;
        }
        int i = 0;
        if (!this.mSwipeUpEnabled) {
            i = 7;
        }
        try {
            this.mISystemUiProxy.setInteractionState(i);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to update overview interaction flags", e);
        }
    }

    @WorkerThread
    private void applyBackButtonAlpha(float f, boolean z) {
        if (this.mISystemUiProxy == null) {
            return;
        }
        try {
            this.mISystemUiProxy.setBackButtonAlpha(f, z);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to update overview back button alpha", e);
        }
    }

    private class SwipeUpGestureEnabledSettingObserver extends ContentObserver {
        private final int defaultValue;
        private Handler mHandler;
        private ContentResolver mResolver;

        SwipeUpGestureEnabledSettingObserver(Handler handler, ContentResolver contentResolver) {
            super(handler);
            this.mHandler = handler;
            this.mResolver = contentResolver;
            this.defaultValue = OverviewInteractionState.this.getSystemBooleanRes(OverviewInteractionState.SWIPE_UP_ENABLED_DEFAULT_RES_NAME) ? 1 : 0;
        }

        public void register() {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(SettingsCompat.SWIPE_UP_SETTING_NAME), false, this);
            OverviewInteractionState.this.mSwipeUpEnabled = getValue();
            OverviewInteractionState.this.resetHomeBounceSeenOnQuickstepEnabledFirstTime();
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean z) {
            super.onChange(z);
            this.mHandler.removeMessages(OverviewInteractionState.MSG_SET_SWIPE_UP_ENABLED);
            this.mHandler.obtainMessage(OverviewInteractionState.MSG_SET_SWIPE_UP_ENABLED, getValue() ? 1 : 0, 0).sendToTarget();
        }

        private boolean getValue() {
            return Settings.Secure.getInt(this.mResolver, SettingsCompat.SWIPE_UP_SETTING_NAME, this.defaultValue) == 1;
        }
    }

    private boolean getSystemBooleanRes(String str) {
        Resources system = Resources.getSystem();
        int identifier = system.getIdentifier(str, "bool", "android");
        if (identifier != 0) {
            return system.getBoolean(identifier);
        }
        Log.e(TAG, "Failed to get system resource ID. Incompatible framework version?");
        return false;
    }

    private void resetHomeBounceSeenOnQuickstepEnabledFirstTime() {
        if (this.mSwipeUpEnabled && !Utilities.getPrefs(this.mContext).getBoolean(HAS_ENABLED_QUICKSTEP_ONCE, true)) {
            Utilities.getPrefs(this.mContext).edit().putBoolean(HAS_ENABLED_QUICKSTEP_ONCE, true).putBoolean(DiscoveryBounce.HOME_BOUNCE_SEEN, false).apply();
        }
    }
}
