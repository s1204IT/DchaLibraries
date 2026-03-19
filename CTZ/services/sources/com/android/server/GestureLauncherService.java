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
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MutableBoolean;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;

public class GestureLauncherService extends SystemService {

    @VisibleForTesting
    static final long CAMERA_POWER_DOUBLE_TAP_MAX_TIME_MS = 300;
    private static final boolean DBG = false;
    private static final boolean DBG_CAMERA_LIFT = false;

    @VisibleForTesting
    static final long POWER_SHORT_TAP_SEQUENCE_MAX_INTERVAL_MS = 500;
    private static final String TAG = "GestureLauncherService";
    private boolean mCameraDoubleTapPowerEnabled;
    private long mCameraGestureLastEventTime;
    private long mCameraGestureOnTimeMs;
    private long mCameraGestureSensor1LastOnTimeMs;
    private long mCameraGestureSensor2LastOnTimeMs;
    private int mCameraLaunchLastEventExtra;
    private boolean mCameraLaunchRegistered;
    private Sensor mCameraLaunchSensor;
    private boolean mCameraLiftRegistered;
    private final CameraLiftTriggerEventListener mCameraLiftTriggerListener;
    private Sensor mCameraLiftTriggerSensor;
    private Context mContext;
    private final GestureEventListener mGestureListener;
    private long mLastPowerDown;
    private final MetricsLogger mMetricsLogger;
    private int mPowerButtonConsecutiveTaps;
    private PowerManager mPowerManager;
    private final ContentObserver mSettingObserver;
    private int mUserId;
    private final BroadcastReceiver mUserReceiver;
    private PowerManager.WakeLock mWakeLock;
    private WindowManagerInternal mWindowManagerInternal;

    public GestureLauncherService(Context context) {
        this(context, new MetricsLogger());
    }

    @VisibleForTesting
    GestureLauncherService(Context context, MetricsLogger metricsLogger) {
        super(context);
        this.mGestureListener = new GestureEventListener();
        this.mCameraLiftTriggerListener = new CameraLiftTriggerEventListener();
        this.mCameraGestureOnTimeMs = 0L;
        this.mCameraGestureLastEventTime = 0L;
        this.mCameraGestureSensor1LastOnTimeMs = 0L;
        this.mCameraGestureSensor2LastOnTimeMs = 0L;
        this.mCameraLaunchLastEventExtra = 0;
        this.mUserReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
                    GestureLauncherService.this.mUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
                    GestureLauncherService.this.mContext.getContentResolver().unregisterContentObserver(GestureLauncherService.this.mSettingObserver);
                    GestureLauncherService.this.registerContentObservers();
                    GestureLauncherService.this.updateCameraRegistered();
                    GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
                }
            }
        };
        this.mSettingObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z, Uri uri, int i) {
                if (i == GestureLauncherService.this.mUserId) {
                    GestureLauncherService.this.updateCameraRegistered();
                    GestureLauncherService.this.updateCameraDoubleTapPowerEnabled();
                }
            }
        };
        this.mContext = context;
        this.mMetricsLogger = metricsLogger;
    }

    @Override
    public void onStart() {
        LocalServices.addService(GestureLauncherService.class, this);
    }

    @Override
    public void onBootPhase(int i) {
        if (i != 600 || !isGestureLauncherEnabled(this.mContext.getResources())) {
            return;
        }
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = this.mPowerManager.newWakeLock(1, TAG);
        updateCameraRegistered();
        updateCameraDoubleTapPowerEnabled();
        this.mUserId = ActivityManager.getCurrentUser();
        this.mContext.registerReceiver(this.mUserReceiver, new IntentFilter("android.intent.action.USER_SWITCHED"));
        registerContentObservers();
    }

    private void registerContentObservers() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_double_tap_power_gesture_disabled"), false, this.mSettingObserver, this.mUserId);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("camera_lift_trigger_enabled"), false, this.mSettingObserver, this.mUserId);
    }

    private void updateCameraRegistered() {
        Resources resources = this.mContext.getResources();
        if (isCameraLaunchSettingEnabled(this.mContext, this.mUserId)) {
            registerCameraLaunchGesture(resources);
        } else {
            unregisterCameraLaunchGesture();
        }
        if (isCameraLiftTriggerSettingEnabled(this.mContext, this.mUserId)) {
            registerCameraLiftTrigger(resources);
        } else {
            unregisterCameraLiftTrigger();
        }
    }

    @VisibleForTesting
    void updateCameraDoubleTapPowerEnabled() {
        boolean zIsCameraDoubleTapPowerSettingEnabled = isCameraDoubleTapPowerSettingEnabled(this.mContext, this.mUserId);
        synchronized (this) {
            this.mCameraDoubleTapPowerEnabled = zIsCameraDoubleTapPowerSettingEnabled;
        }
    }

    private void unregisterCameraLaunchGesture() {
        if (this.mCameraLaunchRegistered) {
            this.mCameraLaunchRegistered = false;
            this.mCameraGestureOnTimeMs = 0L;
            this.mCameraGestureLastEventTime = 0L;
            this.mCameraGestureSensor1LastOnTimeMs = 0L;
            this.mCameraGestureSensor2LastOnTimeMs = 0L;
            this.mCameraLaunchLastEventExtra = 0;
            ((SensorManager) this.mContext.getSystemService("sensor")).unregisterListener(this.mGestureListener);
        }
    }

    private void registerCameraLaunchGesture(Resources resources) {
        if (this.mCameraLaunchRegistered) {
            return;
        }
        this.mCameraGestureOnTimeMs = SystemClock.elapsedRealtime();
        this.mCameraGestureLastEventTime = this.mCameraGestureOnTimeMs;
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        int integer = resources.getInteger(R.integer.config_audio_ring_vol_default);
        if (integer != -1) {
            this.mCameraLaunchRegistered = false;
            String string = resources.getString(R.string.accessibility_system_action_dpad_up_label);
            this.mCameraLaunchSensor = sensorManager.getDefaultSensor(integer, true);
            if (this.mCameraLaunchSensor != null) {
                if (string.equals(this.mCameraLaunchSensor.getStringType())) {
                    this.mCameraLaunchRegistered = sensorManager.registerListener(this.mGestureListener, this.mCameraLaunchSensor, 0);
                    return;
                }
                throw new RuntimeException(String.format("Wrong configuration. Sensor type and sensor string type don't match: %s in resources, %s in the sensor.", string, this.mCameraLaunchSensor.getStringType()));
            }
        }
    }

    private void unregisterCameraLiftTrigger() {
        if (this.mCameraLiftRegistered) {
            this.mCameraLiftRegistered = false;
            ((SensorManager) this.mContext.getSystemService("sensor")).cancelTriggerSensor(this.mCameraLiftTriggerListener, this.mCameraLiftTriggerSensor);
        }
    }

    private void registerCameraLiftTrigger(Resources resources) {
        if (this.mCameraLiftRegistered) {
            return;
        }
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        int integer = resources.getInteger(R.integer.config_audio_ring_vol_steps);
        if (integer != -1) {
            this.mCameraLiftRegistered = false;
            String string = resources.getString(R.string.accessibility_system_action_hardware_a11y_shortcut_label);
            this.mCameraLiftTriggerSensor = sensorManager.getDefaultSensor(integer, true);
            if (this.mCameraLiftTriggerSensor != null) {
                if (string.equals(this.mCameraLiftTriggerSensor.getStringType())) {
                    this.mCameraLiftRegistered = sensorManager.requestTriggerSensor(this.mCameraLiftTriggerListener, this.mCameraLiftTriggerSensor);
                    return;
                }
                throw new RuntimeException(String.format("Wrong configuration. Sensor type and sensor string type don't match: %s in resources, %s in the sensor.", string, this.mCameraLiftTriggerSensor.getStringType()));
            }
        }
    }

    public static boolean isCameraLaunchSettingEnabled(Context context, int i) {
        return isCameraLaunchEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_gesture_disabled", 0, i) == 0;
    }

    public static boolean isCameraDoubleTapPowerSettingEnabled(Context context, int i) {
        return isCameraDoubleTapPowerEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_double_tap_power_gesture_disabled", 0, i) == 0;
    }

    public static boolean isCameraLiftTriggerSettingEnabled(Context context, int i) {
        return isCameraLiftTriggerEnabled(context.getResources()) && Settings.Secure.getIntForUser(context.getContentResolver(), "camera_lift_trigger_enabled", 1, i) != 0;
    }

    public static boolean isCameraLaunchEnabled(Resources resources) {
        return (resources.getInteger(R.integer.config_audio_ring_vol_default) != -1) && !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    public static boolean isCameraDoubleTapPowerEnabled(Resources resources) {
        return resources.getBoolean(R.^attr-private.colorPopupBackground);
    }

    public static boolean isCameraLiftTriggerEnabled(Resources resources) {
        return resources.getInteger(R.integer.config_audio_ring_vol_steps) != -1;
    }

    public static boolean isGestureLauncherEnabled(Resources resources) {
        return isCameraLaunchEnabled(resources) || isCameraDoubleTapPowerEnabled(resources) || isCameraLiftTriggerEnabled(resources);
    }

    public boolean interceptPowerKeyDown(KeyEvent keyEvent, boolean z, MutableBoolean mutableBoolean) {
        long eventTime;
        boolean zHandleCameraGesture;
        boolean z2;
        synchronized (this) {
            eventTime = keyEvent.getEventTime() - this.mLastPowerDown;
            if (this.mCameraDoubleTapPowerEnabled && eventTime < CAMERA_POWER_DOUBLE_TAP_MAX_TIME_MS) {
                this.mPowerButtonConsecutiveTaps++;
                z2 = z;
                zHandleCameraGesture = true;
            } else {
                if (eventTime < 500) {
                    this.mPowerButtonConsecutiveTaps++;
                } else {
                    this.mPowerButtonConsecutiveTaps = 1;
                }
                zHandleCameraGesture = false;
                z2 = false;
            }
            this.mLastPowerDown = keyEvent.getEventTime();
        }
        if (zHandleCameraGesture) {
            Slog.i(TAG, "Power button double tap gesture detected, launching camera. Interval=" + eventTime + "ms");
            zHandleCameraGesture = handleCameraGesture(false, 1);
            if (zHandleCameraGesture) {
                this.mMetricsLogger.action(255, (int) eventTime);
            }
        }
        this.mMetricsLogger.histogram("power_consecutive_short_tap_count", this.mPowerButtonConsecutiveTaps);
        this.mMetricsLogger.histogram("power_double_tap_interval", (int) eventTime);
        mutableBoolean.value = zHandleCameraGesture;
        return z2 && zHandleCameraGesture;
    }

    @VisibleForTesting
    boolean handleCameraGesture(boolean z, int i) {
        if (!(Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0)) {
            return false;
        }
        if (z) {
            this.mWakeLock.acquire(500L);
        }
        ((StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class)).onCameraLaunchGestureDetected(i);
        return true;
    }

    private final class GestureEventListener implements SensorEventListener {
        private GestureEventListener() {
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (GestureLauncherService.this.mCameraLaunchRegistered && sensorEvent.sensor == GestureLauncherService.this.mCameraLaunchSensor && GestureLauncherService.this.handleCameraGesture(true, 0)) {
                GestureLauncherService.this.mMetricsLogger.action(256);
                trackCameraLaunchEvent(sensorEvent);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        private void trackCameraLaunchEvent(SensorEvent sensorEvent) {
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            long j = jElapsedRealtime - GestureLauncherService.this.mCameraGestureOnTimeMs;
            float[] fArr = sensorEvent.values;
            double d = j;
            long j2 = (long) (((double) fArr[0]) * d);
            long j3 = (long) (d * ((double) fArr[1]));
            int i = (int) fArr[2];
            long j4 = jElapsedRealtime - GestureLauncherService.this.mCameraGestureLastEventTime;
            long j5 = j2 - GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs;
            long j6 = j3 - GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs;
            int i2 = i - GestureLauncherService.this.mCameraLaunchLastEventExtra;
            if (j4 < 0 || j5 < 0 || j6 < 0) {
                return;
            }
            EventLogTags.writeCameraGestureTriggered(j4, j5, j6, i2);
            GestureLauncherService.this.mCameraGestureLastEventTime = jElapsedRealtime;
            GestureLauncherService.this.mCameraGestureSensor1LastOnTimeMs = j2;
            GestureLauncherService.this.mCameraGestureSensor2LastOnTimeMs = j3;
            GestureLauncherService.this.mCameraLaunchLastEventExtra = i;
        }
    }

    private final class CameraLiftTriggerEventListener extends TriggerEventListener {
        private CameraLiftTriggerEventListener() {
        }

        @Override
        public void onTrigger(TriggerEvent triggerEvent) {
            if (GestureLauncherService.this.mCameraLiftRegistered && triggerEvent.sensor == GestureLauncherService.this.mCameraLiftTriggerSensor) {
                GestureLauncherService.this.mContext.getResources();
                SensorManager sensorManager = (SensorManager) GestureLauncherService.this.mContext.getSystemService("sensor");
                boolean zIsKeyguardShowingAndNotOccluded = GestureLauncherService.this.mWindowManagerInternal.isKeyguardShowingAndNotOccluded();
                boolean zIsInteractive = GestureLauncherService.this.mPowerManager.isInteractive();
                if ((zIsKeyguardShowingAndNotOccluded || !zIsInteractive) && GestureLauncherService.this.handleCameraGesture(true, 2)) {
                    MetricsLogger.action(GestureLauncherService.this.mContext, 989);
                }
                GestureLauncherService.this.mCameraLiftRegistered = sensorManager.requestTriggerSensor(GestureLauncherService.this.mCameraLiftTriggerListener, GestureLauncherService.this.mCameraLiftTriggerSensor);
            }
        }
    }
}
