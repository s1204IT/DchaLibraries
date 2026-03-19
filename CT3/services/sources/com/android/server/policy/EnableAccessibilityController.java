package com.android.server.policy;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.MathUtils;
import android.view.MotionEvent;
import android.view.WindowManagerInternal;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import com.android.server.LocalServices;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnableAccessibilityController {
    private static final int ENABLE_ACCESSIBILITY_DELAY_MILLIS = 6000;
    public static final int MESSAGE_ENABLE_ACCESSIBILITY = 3;
    public static final int MESSAGE_SPEAK_ENABLE_CANCELED = 2;
    public static final int MESSAGE_SPEAK_WARNING = 1;
    private static final int SPEAK_WARNING_DELAY_MILLIS = 2000;
    private static final String TAG = "EnableAccessibilityController";
    private boolean mCanceled;
    private final Context mContext;
    private boolean mDestroyed;
    private float mFirstPointerDownX;
    private float mFirstPointerDownY;
    private final Runnable mOnAccessibilityEnabledCallback;
    private float mSecondPointerDownX;
    private float mSecondPointerDownY;
    private final Ringtone mTone;
    private final float mTouchSlop;
    private final TextToSpeech mTts;
    private final UserManager mUserManager;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    String text = EnableAccessibilityController.this.mContext.getString(R.string.install_carrier_app_notification_title);
                    EnableAccessibilityController.this.mTts.speak(text, 0, null);
                    break;
                case 2:
                    String text2 = EnableAccessibilityController.this.mContext.getString(R.string.invalidPuk);
                    EnableAccessibilityController.this.mTts.speak(text2, 0, null);
                    break;
                case 3:
                    EnableAccessibilityController.this.enableAccessibility();
                    EnableAccessibilityController.this.mTone.play();
                    EnableAccessibilityController.this.mTts.speak(EnableAccessibilityController.this.mContext.getString(R.string.invalidPin), 0, null);
                    break;
            }
        }
    };
    private final IAccessibilityManager mAccessibilityManager = IAccessibilityManager.Stub.asInterface(ServiceManager.getService("accessibility"));

    public EnableAccessibilityController(Context context, Runnable onAccessibilityEnabledCallback) {
        this.mContext = context;
        this.mOnAccessibilityEnabledCallback = onAccessibilityEnabledCallback;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (!EnableAccessibilityController.this.mDestroyed) {
                    return;
                }
                EnableAccessibilityController.this.mTts.shutdown();
            }
        });
        this.mTone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_NOTIFICATION_URI);
        this.mTone.setStreamType(3);
        this.mTouchSlop = context.getResources().getDimensionPixelSize(R.dimen.car_button_min_width);
    }

    public static boolean canEnableAccessibilityViaGesture(Context context) {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(context);
        return (!accessibilityManager.isEnabled() || accessibilityManager.getEnabledAccessibilityServiceList(1).isEmpty()) && Settings.Global.getInt(context.getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1 && !getInstalledSpeakingAccessibilityServices(context).isEmpty();
    }

    public static List<AccessibilityServiceInfo> getInstalledSpeakingAccessibilityServices(Context context) {
        List<AccessibilityServiceInfo> services = new ArrayList<>();
        services.addAll(AccessibilityManager.getInstance(context).getInstalledAccessibilityServiceList());
        Iterator<AccessibilityServiceInfo> iterator = services.iterator();
        while (iterator.hasNext()) {
            AccessibilityServiceInfo service = iterator.next();
            if ((service.feedbackType & 1) == 0) {
                iterator.remove();
            }
        }
        return services;
    }

    public void onDestroy() {
        this.mDestroyed = true;
    }

    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != 5 || event.getPointerCount() != 2) {
            return false;
        }
        this.mFirstPointerDownX = event.getX(0);
        this.mFirstPointerDownY = event.getY(0);
        this.mSecondPointerDownX = event.getX(1);
        this.mSecondPointerDownY = event.getY(1);
        this.mHandler.sendEmptyMessageDelayed(1, 2000L);
        this.mHandler.sendEmptyMessageDelayed(3, 6000L);
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int action = event.getActionMasked();
        if (this.mCanceled) {
            if (action == 1) {
                this.mCanceled = false;
            }
            return true;
        }
        switch (action) {
            case 2:
                float firstPointerMove = MathUtils.dist(event.getX(0), event.getY(0), this.mFirstPointerDownX, this.mFirstPointerDownY);
                if (Math.abs(firstPointerMove) > this.mTouchSlop) {
                    cancel();
                }
                float secondPointerMove = MathUtils.dist(event.getX(1), event.getY(1), this.mSecondPointerDownX, this.mSecondPointerDownY);
                if (Math.abs(secondPointerMove) > this.mTouchSlop) {
                    cancel();
                }
                return true;
            case 3:
            case 6:
                cancel();
                return true;
            case 4:
            default:
                return true;
            case 5:
                if (pointerCount > 2) {
                    cancel();
                }
                return true;
        }
    }

    private void cancel() {
        this.mCanceled = true;
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
        } else if (this.mHandler.hasMessages(3)) {
            this.mHandler.sendEmptyMessage(2);
        }
        this.mHandler.removeMessages(3);
    }

    private void enableAccessibility() {
        if (!enableAccessibility(this.mContext)) {
            return;
        }
        this.mOnAccessibilityEnabledCallback.run();
    }

    public static boolean enableAccessibility(Context context) {
        IAccessibilityManager accessibilityManager = IAccessibilityManager.Stub.asInterface(ServiceManager.getService("accessibility"));
        WindowManagerInternal windowManager = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        UserManager userManager = (UserManager) context.getSystemService("user");
        ComponentName componentName = getInstalledSpeakingAccessibilityServiceComponent(context);
        if (componentName == null) {
            return false;
        }
        boolean keyguardLocked = windowManager.isKeyguardLocked();
        boolean hasMoreThanOneUser = userManager.getUsers().size() > 1;
        try {
        } catch (RemoteException e) {
            Log.e(TAG, "cannot enable accessibilty: " + e);
        }
        if (!keyguardLocked || !hasMoreThanOneUser) {
            int userId = ActivityManager.getCurrentUser();
            accessibilityManager.enableAccessibilityService(componentName, userId);
        } else {
            if (keyguardLocked) {
                accessibilityManager.temporaryEnableAccessibilityStateUntilKeyguardRemoved(componentName, true);
            }
            return true;
        }
        return true;
    }

    public static void disableAccessibility(Context context) {
        IAccessibilityManager accessibilityManager = IAccessibilityManager.Stub.asInterface(ServiceManager.getService("accessibility"));
        ComponentName componentName = getInstalledSpeakingAccessibilityServiceComponent(context);
        if (componentName == null) {
            return;
        }
        int userId = ActivityManager.getCurrentUser();
        try {
            accessibilityManager.disableAccessibilityService(componentName, userId);
        } catch (RemoteException e) {
            Log.e(TAG, "cannot disable accessibility " + e);
        }
    }

    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService(AccessibilityManager.class);
        List<AccessibilityServiceInfo> enabledAccessibilityServiceList = accessibilityManager.getEnabledAccessibilityServiceList(1);
        return (enabledAccessibilityServiceList == null || enabledAccessibilityServiceList.isEmpty()) ? false : true;
    }

    public static ComponentName getInstalledSpeakingAccessibilityServiceComponent(Context context) {
        List<AccessibilityServiceInfo> services = getInstalledSpeakingAccessibilityServices(context);
        if (services.isEmpty()) {
            return null;
        }
        ServiceInfo serviceInfo = services.get(0).getResolveInfo().serviceInfo;
        return new ComponentName(serviceInfo.packageName, serviceInfo.name);
    }
}
