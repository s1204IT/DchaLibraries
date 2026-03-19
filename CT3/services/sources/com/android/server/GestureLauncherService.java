package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.dhcp.DhcpPacket;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MutableBoolean;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.logging.MetricsLogger;
import com.android.server.statusbar.StatusBarManagerInternal;

public class GestureLauncherService extends SystemService {
    private static final long CAMERA_POWER_DOUBLE_TAP_MAX_TIME_MS = 300;
    private static final boolean DBG = false;
    private static final String TAG = "GestureLauncherService";
    private boolean mCameraDoubleTapPowerEnabled;
    private long mCameraGestureLastEventTime;
    private long mCameraGestureOnTimeMs;
    private long mCameraGestureSensor1LastOnTimeMs;
    private long mCameraGestureSensor2LastOnTimeMs;
    private int mCameraLaunchLastEventExtra;
    private Sensor mCameraLaunchSensor;
    private Context mContext;
    private final GestureEventListener mGestureListener;
    private long mLastPowerDown;
    private boolean mRegistered;
    private final ContentObserver mSettingObserver;
    private int mUserId;
    private final BroadcastReceiver mUserReceiver;
    private PowerManager.WakeLock mWakeLock;

    public GestureLauncherService(Context context) {
        super(context);
        this.mGestureListener = new GestureEventListener(this, null);
        this.mCameraGestureOnTimeMs = 0L;
        this.mCameraGestureLastEventTime = 0L;
        this.mCameraGestureSensor1LastOnTimeMs = 0L;
        this.mCameraGestureSensor2LastOnTimeMs = 0L;
        this.mCameraLaunchLastEventExtra = 0;
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!"android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    return;
                }
                GestureLauncherService.this.mUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                GestureLauncherService.this.mContext.getContentResolver().unregisterContentObserver(GestureLauncherService.this.mSettingObserver);
                GestureLauncherService.this.registerContentObservers();
                GestureLauncherService.this.updateCameraRegistered();
                GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
            }
        };
        this.mSettingObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                if (userId != GestureLauncherService.this.mUserId) {
                    return;
                }
                GestureLauncherService.this.updateCameraRegistered();
                GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
            }
        };
        this.mContext = context;
    }

    @Override
    public void onStart() {
        LocalServices.addService(GestureLauncherService.class, this);
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase != 600) {
            return;
        }
        Resources resources = this.mContext.getResources();
        if (!isGestureLauncherEnabled(resources)) {
            return;
        }
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, TAG);
        updateCameraRegistered();
        updateCameraDoubleTapPowerEnabled();
        this.mUserId = ActivityManager.getCurrentUser();
        this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        registerContentObservers();
    }

    private void registerContentObservers() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_double_tap_power_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
    }

    private void updateCameraRegistered() {
        Resources resources = this.mContext.getResources();
        if (isCameraLaunchSettingEnabled(this.mContext, this.mUserId)) {
            registerCameraLaunchGesture(resources);
        } else {
            unregisterCameraLaunchGesture();
        }
    }

    private void updateCameraDoubleTapPowerEnabled() {
        boolean enabled = isCameraDoubleTapPowerSettingEnabled(this.mContext, this.mUserId);
        synchronized (this) {
            this.mCameraDoubleTapPowerEnabled = enabled;
        }
    }

    private void unregisterCameraLaunchGesture() {
        if (!this.mRegistered) {
            return;
        }
        this.mRegistered = false;
        this.mCameraGestureOnTimeMs = 0L;
        this.mCameraGestureLastEventTime = 0L;
        this.mCameraGestureSensor1LastOnTimeMs = 0L;
        this.mCameraGestureSensor2LastOnTimeMs = 0L;
        this.mCameraLaunchLastEventExtra = 0;
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        sensorManager.unregisterListener(this.mGestureListener);
    }

    private void registerCameraLaunchGesture(Resources resources) {
        if (this.mRegistered) {
            return;
        }
        this.mCameraGestureOnTimeMs = SystemClock.elapsedRealtime();
        this.mCameraGestureLastEventTime = this.mCameraGestureOnTimeMs;
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        int cameraLaunchGestureId = resources.getInteger(R.integer.config_extraFreeKbytesAbsolute);
        if (cameraLaunchGestureId == -1) {
            return;
        }
        this.mRegistered = false;
        String sensorName = resources.getString(R.string.PERSOSUBSTATE_RUIM_HRPD_ENTRY);
        this.mCameraLaunchSensor = sensorManager.getDefaultSensor(cameraLaunchGestureId, true);
        if (this.mCameraLaunchSensor == null) {
            return;
        }
        if (sensorName.equals(this.mCameraLaunchSensor.getStringType())) {
            this.mRegistered = sensorManager.registerListener(this.mGestureListener, this.mCameraLaunchSensor, 0);
        } else {
            String message = String.format("Wrong configuration. Sensor type and sensor string type don't match: %s in resources, %s in the sensor.", sensorName, this.mCameraLaunchSensor.getStringType());
            throw new RuntimeException(message);
        }
    }

    public static boolean isCameraLaunchSettingEnabled(Context context, int userId) {
        return isCameraLaunchEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_gesture_disabled", 0, userId) == 0;
    }

    public static boolean isCameraDoubleTapPowerSettingEnabled(Context context, int userId) {
        return isCameraDoubleTapPowerEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_double_tap_power_gesture_disabled", 0, userId) == 0;
    }

    public static boolean isCameraLaunchEnabled(Resources resources) {
        boolean configSet = resources.getInteger(R.integer.config_extraFreeKbytesAbsolute) != -1;
        return configSet && !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    public static boolean isCameraDoubleTapPowerEnabled(Resources resources) {
        return resources.getBoolean(R.^attr-private.paddingBottomNoButtons);
    }

    public static boolean isGestureLauncherEnabled(Resources resources) {
        if (isCameraLaunchEnabled(resources)) {
            return true;
        }
        return isCameraDoubleTapPowerEnabled(resources);
    }

    public boolean interceptPowerKeyDown(KeyEvent event, boolean interactive, MutableBoolean outLaunched) {
        long doubleTapInterval;
        boolean launched = false;
        boolean intercept = false;
        synchronized (this) {
            doubleTapInterval = event.getEventTime() - this.mLastPowerDown;
            if (this.mCameraDoubleTapPowerEnabled && doubleTapInterval < CAMERA_POWER_DOUBLE_TAP_MAX_TIME_MS) {
                launched = true;
                intercept = interactive;
            }
            this.mLastPowerDown = event.getEventTime();
        }
        if (launched) {
            Slog.i(TAG, "Power button double tap gesture detected, launching camera. Interval=" + doubleTapInterval + "ms");
            launched = handleCameraLaunchGesture(false, 1);
            if (launched) {
                MetricsLogger.action(this.mContext, DhcpPacket.MAX_OPTION_LEN, (int) doubleTapInterval);
            }
        }
        MetricsLogger.histogram(this.mContext, "power_double_tap_interval", (int) doubleTapInterval);
        outLaunched.value = launched;
        if (intercept) {
            return launched;
        }
        return false;
    }

    private boolean handleCameraLaunchGesture(boolean useWakelock, int source) {
        boolean userSetupComplete = Settings.Secure.getInt(this.mContext.getContentResolver(), "user_setup_complete", 0) != 0;
        if (!userSetupComplete) {
            return false;
        }
        if (useWakelock) {
            this.mWakeLock.acquire(500L);
        }
        StatusBarManagerInternal service = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        service.onCameraLaunchGestureDetected(source);
        return true;
    }

    private final class GestureEventListener implements SensorEventListener {
        GestureEventListener(GestureLauncherService this$0, GestureEventListener gestureEventListener) {
            this();
        }

        private GestureEventListener() {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (GestureLauncherService.this.mRegistered && event.sensor == GestureLauncherService.this.mCameraLaunchSensor && GestureLauncherService.this.handleCameraLaunchGesture(true, 0)) {
                MetricsLogger.action(GestureLauncherService.this.mContext, 256);
                trackCameraLaunchEvent(event);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        private void trackCameraLaunchEvent(SensorEvent event) {
            long now = SystemClock.elapsedRealtime();
            long totalDuration = now - GestureLauncherService.this.mCameraGestureOnTimeMs;
            float[] values = event.values;
            long sensor1OnTime = (long) (totalDuration * ((double) values[0]));
            long sensor2OnTime = (long) (totalDuration * ((double) values[1]));
            int extra = (int) values[2];
            long gestureOnTimeDiff = now - GestureLauncherService.this.mCameraGestureLastEventTime;
            long sensor1OnTimeDiff = sensor1OnTime - GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs;
            long sensor2OnTimeDiff = sensor2OnTime - GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs;
            int extraDiff = extra - GestureLauncherService.this.mCameraLaunchLastEventExtra;
            if (gestureOnTimeDiff < 0 || sensor1OnTimeDiff < 0 || sensor2OnTimeDiff < 0) {
                return;
            }
            EventLogTags.writeCameraGestureTriggered(gestureOnTimeDiff, sensor1OnTimeDiff, sensor2OnTimeDiff, extraDiff);
            GestureLauncherService.this.mCameraGestureLastEventTime = now;
            GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs = sensor1OnTime;
            GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs = sensor2OnTime;
            GestureLauncherService.this.mCameraLaunchLastEventExtra = extra;
        }
    }
}
