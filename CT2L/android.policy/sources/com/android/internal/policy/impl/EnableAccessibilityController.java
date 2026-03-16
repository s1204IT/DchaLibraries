package com.android.internal.policy.impl;

import android.R;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
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
import android.util.MathUtils;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.IAccessibilityManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnableAccessibilityController {
    private static final int ENABLE_ACCESSIBILITY_DELAY_MILLIS = 6000;
    public static final int MESSAGE_ENABLE_ACCESSIBILITY = 3;
    public static final int MESSAGE_SPEAK_ENABLE_CANCELED = 2;
    public static final int MESSAGE_SPEAK_WARNING = 1;
    private static final int SPEAK_WARNING_DELAY_MILLIS = 2000;
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
                case EnableAccessibilityController.MESSAGE_SPEAK_WARNING:
                    String text = EnableAccessibilityController.this.mContext.getString(R.string.mediasize_iso_c2);
                    EnableAccessibilityController.this.mTts.speak(text, 0, null);
                    break;
                case EnableAccessibilityController.MESSAGE_SPEAK_ENABLE_CANCELED:
                    String text2 = EnableAccessibilityController.this.mContext.getString(R.string.mediasize_iso_c4);
                    EnableAccessibilityController.this.mTts.speak(text2, 0, null);
                    break;
                case EnableAccessibilityController.MESSAGE_ENABLE_ACCESSIBILITY:
                    EnableAccessibilityController.this.enableAccessibility();
                    EnableAccessibilityController.this.mTone.play();
                    EnableAccessibilityController.this.mTts.speak(EnableAccessibilityController.this.mContext.getString(R.string.mediasize_iso_c3), 0, null);
                    break;
            }
        }
    };
    private final IWindowManager mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
    private final IAccessibilityManager mAccessibilityManager = IAccessibilityManager.Stub.asInterface(ServiceManager.getService("accessibility"));

    public EnableAccessibilityController(Context context, Runnable onAccessibilityEnabledCallback) {
        this.mContext = context;
        this.mOnAccessibilityEnabledCallback = onAccessibilityEnabledCallback;
        this.mUserManager = (UserManager) this.mContext.getSystemService("user");
        this.mTts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (EnableAccessibilityController.this.mDestroyed) {
                    EnableAccessibilityController.this.mTts.shutdown();
                }
            }
        });
        this.mTone = RingtoneManager.getRingtone(context, Settings.System.DEFAULT_NOTIFICATION_URI);
        this.mTone.setStreamType(3);
        this.mTouchSlop = context.getResources().getDimensionPixelSize(R.dimen.btn_textSize);
    }

    public static boolean canEnableAccessibilityViaGesture(Context context) {
        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(context);
        if (!accessibilityManager.isEnabled() || accessibilityManager.getEnabledAccessibilityServiceList(1).isEmpty()) {
            return Settings.Global.getInt(context.getContentResolver(), "enable_accessibility_global_gesture_enabled", 0) == 1 && !getInstalledSpeakingAccessibilityServices(context).isEmpty();
        }
        return false;
    }

    private static List<AccessibilityServiceInfo> getInstalledSpeakingAccessibilityServices(Context context) {
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
        } else {
            switch (action) {
                case MESSAGE_SPEAK_ENABLE_CANCELED:
                    float firstPointerMove = MathUtils.dist(event.getX(0), event.getY(0), this.mFirstPointerDownX, this.mFirstPointerDownY);
                    if (Math.abs(firstPointerMove) > this.mTouchSlop) {
                        cancel();
                    }
                    float secondPointerMove = MathUtils.dist(event.getX(1), event.getY(1), this.mSecondPointerDownX, this.mSecondPointerDownY);
                    if (Math.abs(secondPointerMove) > this.mTouchSlop) {
                        cancel();
                    }
                    break;
                case MESSAGE_ENABLE_ACCESSIBILITY:
                case 6:
                    cancel();
                    break;
                case 5:
                    if (pointerCount > 2) {
                        cancel();
                    }
                    break;
            }
        }
        return true;
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
        List<AccessibilityServiceInfo> services = getInstalledSpeakingAccessibilityServices(this.mContext);
        if (!services.isEmpty()) {
            boolean keyguardLocked = false;
            try {
                keyguardLocked = this.mWindowManager.isKeyguardLocked();
            } catch (RemoteException e) {
            }
            boolean hasMoreThanOneUser = this.mUserManager.getUsers().size() > 1;
            AccessibilityServiceInfo service = services.get(0);
            boolean enableTouchExploration = (service.flags & 4) != 0;
            if (!enableTouchExploration) {
                int serviceCount = services.size();
                int i = 1;
                while (true) {
                    if (i >= serviceCount) {
                        break;
                    }
                    AccessibilityServiceInfo candidate = services.get(i);
                    if ((candidate.flags & 4) == 0) {
                        i++;
                    } else {
                        enableTouchExploration = true;
                        service = candidate;
                        break;
                    }
                }
            }
            ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
            if (!keyguardLocked || !hasMoreThanOneUser) {
                int userId = ActivityManager.getCurrentUser();
                String enabledServiceString = componentName.flattenToString();
                ContentResolver resolver = this.mContext.getContentResolver();
                Settings.Secure.putStringForUser(resolver, "enabled_accessibility_services", enabledServiceString, userId);
                Settings.Secure.putStringForUser(resolver, "touch_exploration_granted_accessibility_services", enabledServiceString, userId);
                if (enableTouchExploration) {
                    Settings.Secure.putIntForUser(resolver, "touch_exploration_enabled", 1, userId);
                }
                Settings.Secure.putIntForUser(resolver, "accessibility_script_injection", 1, userId);
                Settings.Secure.putIntForUser(resolver, "accessibility_enabled", 1, userId);
            } else if (keyguardLocked) {
                try {
                    this.mAccessibilityManager.temporaryEnableAccessibilityStateUntilKeyguardRemoved(componentName, enableTouchExploration);
                } catch (RemoteException e2) {
                }
            }
            this.mOnAccessibilityEnabledCallback.run();
        }
    }
}
