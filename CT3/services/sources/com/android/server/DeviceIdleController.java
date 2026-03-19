package com.android.server;

import android.R;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.hardware.display.DisplayManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.net.INetworkPolicyManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IDeviceIdleController;
import android.os.IMaintenanceActivityListener;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.Process;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.MutableLong;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TimeUtils;
import android.util.Xml;
import android.view.Display;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.AtomicFile;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.AlarmManagerService;
import com.android.server.AnyMotionDetector;
import com.android.server.am.BatteryStatsService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.location.LocationFudger;
import com.android.server.pm.PackageManagerService;
import com.mediatek.datashaping.DataShapingUtils;
import com.mediatek.datashaping.IDataShapingManager;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DeviceIdleController extends SystemService implements AnyMotionDetector.DeviceIdleCallback {
    private static final boolean COMPRESS_TIME = false;
    private static final String CONFIG_AUTO_POWER = "persist.config.AutoPowerModes";
    private static final boolean DEBUG = false;
    private static final int ENABLE_DEEP_DOZE = 2;
    private static final int ENABLE_LIGHT_AND_DEEP_DOZE = 3;
    private static final int ENABLE_LIGHT_DOZE = 1;
    private static final int EVENT_BUFFER_SIZE = 100;
    private static final int EVENT_DEEP_IDLE = 4;
    private static final int EVENT_DEEP_MAINTENANCE = 5;
    private static final int EVENT_LIGHT_IDLE = 2;
    private static final int EVENT_LIGHT_MAINTENANCE = 3;
    private static final int EVENT_NORMAL = 1;
    private static final int EVENT_NULL = 0;
    private static final int LIGHT_STATE_ACTIVE = 0;
    private static final int LIGHT_STATE_IDLE = 4;
    private static final int LIGHT_STATE_IDLE_MAINTENANCE = 6;
    private static final int LIGHT_STATE_INACTIVE = 1;
    private static final int LIGHT_STATE_OVERRIDE = 7;
    private static final int LIGHT_STATE_PRE_IDLE = 3;
    private static final int LIGHT_STATE_WAITING_FOR_NETWORK = 5;
    static final int MSG_FINISH_IDLE_OP = 8;
    static final int MSG_REPORT_ACTIVE = 5;
    static final int MSG_REPORT_IDLE_OFF = 4;
    static final int MSG_REPORT_IDLE_ON = 2;
    static final int MSG_REPORT_IDLE_ON_LIGHT = 3;
    static final int MSG_REPORT_MAINTENANCE_ACTIVITY = 7;
    static final int MSG_TEMP_APP_WHITELIST_TIMEOUT = 6;
    static final int MSG_WRITE_CONFIG = 1;
    private static final int STATE_ACTIVE = 0;
    private static final int STATE_IDLE = 5;
    private static final int STATE_IDLE_MAINTENANCE = 6;
    private static final int STATE_IDLE_PENDING = 2;
    private static final int STATE_INACTIVE = 1;
    private static final int STATE_LOCATING = 4;
    private static final int STATE_SENSING = 3;
    private static final String TAG = "DeviceIdleController";
    private int mActiveIdleOpCount;
    private PowerManager.WakeLock mActiveIdleWakeLock;
    private AlarmManager mAlarmManager;
    private boolean mAlarmsActive;
    private AnyMotionDetector mAnyMotionDetector;
    private IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private boolean mCharging;
    public final AtomicFile mConfigFile;
    private ConnectivityService mConnectivityService;
    private Constants mConstants;
    private Display mCurDisplay;
    private long mCurIdleBudget;
    private IDataShapingManager mDataShapingManager;
    private final AlarmManager.OnAlarmListener mDeepAlarmListener;
    private boolean mDeepEnabled;
    private final DisplayManager.DisplayListener mDisplayListener;
    private DisplayManager mDisplayManager;
    private final int[] mEventCmds;
    private final long[] mEventTimes;
    private boolean mForceIdle;
    private final LocationListener mGenericLocationListener;
    private final LocationListener mGpsLocationListener;
    final MyHandler mHandler;
    private boolean mHasGps;
    private boolean mHasNetworkLocation;
    private Intent mIdleIntent;
    private final BroadcastReceiver mIdleStartedDoneReceiver;
    private long mInactiveTimeout;
    private boolean mJobsActive;
    private Location mLastGenericLocation;
    private Location mLastGpsLocation;
    private final AlarmManager.OnAlarmListener mLightAlarmListener;
    private boolean mLightEnabled;
    private Intent mLightIdleIntent;
    private int mLightState;
    private AlarmManagerService.LocalService mLocalAlarmManager;
    private PowerManagerInternal mLocalPowerManager;
    private boolean mLocated;
    private boolean mLocating;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    private final RemoteCallbackList<IMaintenanceActivityListener> mMaintenanceActivityListeners;
    private long mMaintenanceStartTime;
    private final MotionListener mMotionListener;
    private Sensor mMotionSensor;
    private boolean mNetworkConnected;
    private INetworkPolicyManager mNetworkPolicyManager;
    Runnable mNetworkPolicyTempWhitelistCallback;
    private long mNextAlarmTime;
    private long mNextIdleDelay;
    private long mNextIdlePendingDelay;
    private long mNextLightAlarmTime;
    private long mNextLightIdleDelay;
    private long mNextSensingTimeoutAlarmTime;
    private boolean mNotMoving;
    private PowerManager mPowerManager;
    private int[] mPowerSaveWhitelistAllAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistAllAppIds;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistApps;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistAppsExceptIdle;
    private int[] mPowerSaveWhitelistExceptIdleAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistExceptIdleAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIds;
    private final SparseBooleanArray mPowerSaveWhitelistSystemAppIdsExceptIdle;
    private int[] mPowerSaveWhitelistUserAppIdArray;
    private final SparseBooleanArray mPowerSaveWhitelistUserAppIds;
    private final ArrayMap<String, Integer> mPowerSaveWhitelistUserApps;
    private final BroadcastReceiver mReceiver;
    private boolean mReportedMaintenanceActivity;
    private boolean mScreenOn;
    private final AlarmManager.OnAlarmListener mSensingTimeoutAlarmListener;
    private SensorManager mSensorManager;
    private int mState;
    private int[] mTempWhitelistAppIdArray;
    private final SparseArray<Pair<MutableLong, String>> mTempWhitelistAppIdEndTimes;

    private static String stateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
                return "IDLE_PENDING";
            case 3:
                return "SENSING";
            case 4:
                return "LOCATING";
            case 5:
                return "IDLE";
            case 6:
                return "IDLE_MAINTENANCE";
            default:
                return Integer.toString(state);
        }
    }

    private static String lightStateToString(int state) {
        switch (state) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
            default:
                return Integer.toString(state);
            case 3:
                return "PRE_IDLE";
            case 4:
                return "IDLE";
            case 5:
                return "WAITING_FOR_NETWORK";
            case 6:
                return "IDLE_MAINTENANCE";
            case 7:
                return "OVERRIDE";
        }
    }

    private void addEvent(int cmd) {
        if (this.mEventCmds[0] == cmd) {
            return;
        }
        System.arraycopy(this.mEventCmds, 0, this.mEventCmds, 1, 99);
        System.arraycopy(this.mEventTimes, 0, this.mEventTimes, 1, 99);
        this.mEventCmds[0] = cmd;
        this.mEventTimes[0] = SystemClock.elapsedRealtime();
    }

    private final class MotionListener extends TriggerEventListener implements SensorEventListener {
        boolean active;

        MotionListener(DeviceIdleController this$0, MotionListener motionListener) {
            this();
        }

        private MotionListener() {
            this.active = false;
        }

        @Override
        public void onTrigger(TriggerEvent event) {
            synchronized (DeviceIdleController.this) {
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.mSensorManager.unregisterListener(this, DeviceIdleController.this.mMotionSensor);
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        public boolean registerLocked() {
            boolean success;
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                success = DeviceIdleController.this.mSensorManager.requestTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                success = DeviceIdleController.this.mSensorManager.registerListener(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor, 3);
            }
            if (success) {
                this.active = true;
            } else {
                Slog.e(DeviceIdleController.TAG, "Unable to register for " + DeviceIdleController.this.mMotionSensor);
            }
            return success;
        }

        public void unregisterLocked() {
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                DeviceIdleController.this.mSensorManager.cancelTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                DeviceIdleController.this.mSensorManager.unregisterListener(DeviceIdleController.this.mMotionListener);
            }
            this.active = false;
        }
    }

    private final class Constants extends ContentObserver {
        private static final String KEY_IDLE_AFTER_INACTIVE_TIMEOUT = "idle_after_inactive_to";
        private static final String KEY_IDLE_FACTOR = "idle_factor";
        private static final String KEY_IDLE_PENDING_FACTOR = "idle_pending_factor";
        private static final String KEY_IDLE_PENDING_TIMEOUT = "idle_pending_to";
        private static final String KEY_IDLE_TIMEOUT = "idle_to";
        private static final String KEY_INACTIVE_TIMEOUT = "inactive_to";
        private static final String KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = "light_after_inactive_to";
        private static final String KEY_LIGHT_IDLE_FACTOR = "light_idle_factor";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = "light_idle_maintenance_max_budget";
        private static final String KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = "light_idle_maintenance_min_budget";
        private static final String KEY_LIGHT_IDLE_TIMEOUT = "light_idle_to";
        private static final String KEY_LIGHT_MAX_IDLE_TIMEOUT = "light_max_idle_to";
        private static final String KEY_LIGHT_PRE_IDLE_TIMEOUT = "light_pre_idle_to";
        private static final String KEY_LOCATING_TIMEOUT = "locating_to";
        private static final String KEY_LOCATION_ACCURACY = "location_accuracy";
        private static final String KEY_MAX_IDLE_PENDING_TIMEOUT = "max_idle_pending_to";
        private static final String KEY_MAX_IDLE_TIMEOUT = "max_idle_to";
        private static final String KEY_MAX_TEMP_APP_WHITELIST_DURATION = "max_temp_app_whitelist_duration";
        private static final String KEY_MIN_DEEP_MAINTENANCE_TIME = "min_deep_maintenance_time";
        private static final String KEY_MIN_LIGHT_MAINTENANCE_TIME = "min_light_maintenance_time";
        private static final String KEY_MIN_TIME_TO_ALARM = "min_time_to_alarm";
        private static final String KEY_MMS_TEMP_APP_WHITELIST_DURATION = "mms_temp_app_whitelist_duration";
        private static final String KEY_MOTION_INACTIVE_TIMEOUT = "motion_inactive_to";
        private static final String KEY_NOTIFICATION_WHITELIST_DURATION = "notification_whitelist_duration";
        private static final String KEY_SENSING_TIMEOUT = "sensing_to";
        private static final String KEY_SMS_TEMP_APP_WHITELIST_DURATION = "sms_temp_app_whitelist_duration";
        public long IDLE_AFTER_INACTIVE_TIMEOUT;
        public float IDLE_FACTOR;
        public float IDLE_PENDING_FACTOR;
        public long IDLE_PENDING_TIMEOUT;
        public long IDLE_TIMEOUT;
        public long INACTIVE_TIMEOUT;
        public long LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT;
        public float LIGHT_IDLE_FACTOR;
        public long LIGHT_IDLE_MAINTENANCE_MAX_BUDGET;
        public long LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
        public long LIGHT_IDLE_TIMEOUT;
        public long LIGHT_MAX_IDLE_TIMEOUT;
        public long LIGHT_PRE_IDLE_TIMEOUT;
        public long LOCATING_TIMEOUT;
        public float LOCATION_ACCURACY;
        public long MAX_IDLE_PENDING_TIMEOUT;
        public long MAX_IDLE_TIMEOUT;
        public long MAX_TEMP_APP_WHITELIST_DURATION;
        public long MIN_DEEP_MAINTENANCE_TIME;
        public long MIN_LIGHT_MAINTENANCE_TIME;
        public long MIN_TIME_TO_ALARM;
        public long MMS_TEMP_APP_WHITELIST_DURATION;
        public long MOTION_INACTIVE_TIMEOUT;
        public long NOTIFICATION_WHITELIST_DURATION;
        public long SENSING_TIMEOUT;
        public long SMS_TEMP_APP_WHITELIST_DURATION;
        private final boolean mHasWatch;
        private final KeyValueListParser mParser;
        private final ContentResolver mResolver;

        public Constants(Handler handler, ContentResolver resolver) {
            super(handler);
            this.mParser = new KeyValueListParser(',');
            this.mResolver = resolver;
            this.mHasWatch = DeviceIdleController.this.getContext().getPackageManager().hasSystemFeature("android.hardware.type.watch");
            this.mResolver.registerContentObserver(Settings.Global.getUriFor(this.mHasWatch ? "device_idle_constants_watch" : "device_idle_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (DeviceIdleController.this) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, this.mHasWatch ? "device_idle_constants_watch" : "device_idle_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(DeviceIdleController.TAG, "Bad device idle settings", e);
                }
                this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getLong(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, 300000L);
                this.LIGHT_PRE_IDLE_TIMEOUT = this.mParser.getLong(KEY_LIGHT_PRE_IDLE_TIMEOUT, LocationFudger.FASTEST_INTERVAL_MS);
                this.LIGHT_IDLE_TIMEOUT = this.mParser.getLong(KEY_LIGHT_IDLE_TIMEOUT, 300000L);
                this.LIGHT_IDLE_FACTOR = this.mParser.getFloat(KEY_LIGHT_IDLE_FACTOR, 2.0f);
                this.LIGHT_MAX_IDLE_TIMEOUT = this.mParser.getLong(KEY_LIGHT_MAX_IDLE_TIMEOUT, 900000L);
                this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = this.mParser.getLong(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, 60000L);
                this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = this.mParser.getLong(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, 300000L);
                this.MIN_LIGHT_MAINTENANCE_TIME = this.mParser.getLong(KEY_MIN_LIGHT_MAINTENANCE_TIME, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
                this.MIN_DEEP_MAINTENANCE_TIME = this.mParser.getLong(KEY_MIN_DEEP_MAINTENANCE_TIME, 30000L);
                long inactiveTimeoutDefault = ((long) ((this.mHasWatch ? 15 : 30) * 60)) * 1000;
                this.INACTIVE_TIMEOUT = this.mParser.getLong(KEY_INACTIVE_TIMEOUT, inactiveTimeoutDefault);
                this.SENSING_TIMEOUT = this.mParser.getLong(KEY_SENSING_TIMEOUT, 240000L);
                this.LOCATING_TIMEOUT = this.mParser.getLong(KEY_LOCATING_TIMEOUT, 30000L);
                this.LOCATION_ACCURACY = this.mParser.getFloat(KEY_LOCATION_ACCURACY, 20.0f);
                this.MOTION_INACTIVE_TIMEOUT = this.mParser.getLong(KEY_MOTION_INACTIVE_TIMEOUT, LocationFudger.FASTEST_INTERVAL_MS);
                long idleAfterInactiveTimeout = ((long) ((this.mHasWatch ? 15 : 30) * 60)) * 1000;
                this.IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getLong(KEY_IDLE_AFTER_INACTIVE_TIMEOUT, idleAfterInactiveTimeout);
                this.IDLE_PENDING_TIMEOUT = this.mParser.getLong(KEY_IDLE_PENDING_TIMEOUT, 300000L);
                this.MAX_IDLE_PENDING_TIMEOUT = this.mParser.getLong(KEY_MAX_IDLE_PENDING_TIMEOUT, LocationFudger.FASTEST_INTERVAL_MS);
                this.IDLE_PENDING_FACTOR = this.mParser.getFloat(KEY_IDLE_PENDING_FACTOR, 2.0f);
                this.IDLE_TIMEOUT = this.mParser.getLong(KEY_IDLE_TIMEOUT, 3600000L);
                this.MAX_IDLE_TIMEOUT = this.mParser.getLong(KEY_MAX_IDLE_TIMEOUT, 21600000L);
                this.IDLE_FACTOR = this.mParser.getFloat(KEY_IDLE_FACTOR, 2.0f);
                this.MIN_TIME_TO_ALARM = this.mParser.getLong(KEY_MIN_TIME_TO_ALARM, 3600000L);
                this.MAX_TEMP_APP_WHITELIST_DURATION = this.mParser.getLong(KEY_MAX_TEMP_APP_WHITELIST_DURATION, 300000L);
                this.MMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getLong(KEY_MMS_TEMP_APP_WHITELIST_DURATION, 60000L);
                this.SMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getLong(KEY_SMS_TEMP_APP_WHITELIST_DURATION, 20000L);
                this.NOTIFICATION_WHITELIST_DURATION = this.mParser.getLong(KEY_NOTIFICATION_WHITELIST_DURATION, 30000L);
            }
        }

        void dump(PrintWriter pw) {
            pw.println("  Settings:");
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_PRE_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_PRE_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_FACTOR);
            pw.print("=");
            pw.print(this.LIGHT_IDLE_FACTOR);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET);
            pw.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_LIGHT_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_LIGHT_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MIN_DEEP_MAINTENANCE_TIME);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_DEEP_MAINTENANCE_TIME, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SENSING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.SENSING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.LOCATING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_LOCATION_ACCURACY);
            pw.print("=");
            pw.print(this.LOCATION_ACCURACY);
            pw.print("m");
            pw.println();
            pw.print("    ");
            pw.print(KEY_MOTION_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MOTION_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_AFTER_INACTIVE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_AFTER_INACTIVE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_PENDING_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_PENDING_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_PENDING_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_PENDING_FACTOR);
            pw.print("    ");
            pw.print(KEY_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_IDLE_TIMEOUT);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_TIMEOUT, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_IDLE_FACTOR);
            pw.print("=");
            pw.println(this.IDLE_FACTOR);
            pw.print("    ");
            pw.print(KEY_MIN_TIME_TO_ALARM);
            pw.print("=");
            TimeUtils.formatDuration(this.MIN_TIME_TO_ALARM, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MAX_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.MAX_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_MMS_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.MMS_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_SMS_TEMP_APP_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.SMS_TEMP_APP_WHITELIST_DURATION, pw);
            pw.println();
            pw.print("    ");
            pw.print(KEY_NOTIFICATION_WHITELIST_DURATION);
            pw.print("=");
            TimeUtils.formatDuration(this.NOTIFICATION_WHITELIST_DURATION, pw);
            pw.println();
        }
    }

    @Override
    public void onAnyMotionResult(int result) {
        if (result != -1) {
            synchronized (this) {
                cancelSensingTimeoutAlarmLocked();
            }
        }
        if (result == 1) {
            synchronized (this) {
                handleMotionDetectedLocked(this.mConstants.INACTIVE_TIMEOUT, "sense_motion");
            }
        }
        if (result != 0) {
            return;
        }
        if (this.mState == 3) {
            synchronized (this) {
                this.mNotMoving = true;
                stepIdleStateLocked("s:stationary");
            }
        } else {
            if (this.mState != 4) {
                return;
            }
            synchronized (this) {
                this.mNotMoving = true;
                if (this.mLocated) {
                    stepIdleStateLocked("s:stationary");
                }
            }
        }
    }

    final class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            boolean deepChanged;
            boolean lightChanged;
            int i;
            switch (msg.what) {
                case 1:
                    DeviceIdleController.this.handleWriteConfigFile();
                    return;
                case 2:
                case 3:
                    EventLogTags.writeDeviceIdleOnStart();
                    if (msg.what == 2) {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(true);
                        lightChanged = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    } else {
                        deepChanged = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                        lightChanged = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(true);
                    }
                    try {
                        if (DeviceIdleController.this.getDataShapingService() != null) {
                            DeviceIdleController.this.mDataShapingManager.setDeviceIdleMode(true);
                        }
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(true);
                        IBatteryStats iBatteryStats = DeviceIdleController.this.mBatteryStats;
                        if (msg.what == 2) {
                            i = 2;
                        } else {
                            i = 1;
                        }
                        iBatteryStats.noteDeviceIdleMode(i, (String) null, Process.myUid());
                        break;
                    } catch (RemoteException e) {
                    }
                    if (deepChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOnComplete();
                    return;
                case 4:
                    EventLogTags.writeDeviceIdleOffStart("unknown");
                    boolean deepChanged2 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightChanged2 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, (String) null, Process.myUid());
                        break;
                    } catch (RemoteException e2) {
                    }
                    if (deepChanged2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    if (lightChanged2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    DeviceIdleController.this.decActiveIdleOps();
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 5:
                    String activeReason = (String) msg.obj;
                    int activeUid = msg.arg1;
                    EventLogTags.writeDeviceIdleOffStart(activeReason != null ? activeReason : "unknown");
                    boolean deepChanged3 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightChanged3 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        if (DeviceIdleController.this.getDataShapingService() != null) {
                            DeviceIdleController.this.mDataShapingManager.setDeviceIdleMode(false);
                        }
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, activeReason, activeUid);
                        break;
                    } catch (RemoteException e3) {
                    }
                    if (deepChanged3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightChanged3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 6:
                    int uid = msg.arg1;
                    DeviceIdleController.this.checkTempAppWhitelistTimeout(uid);
                    return;
                case 7:
                    boolean active = msg.arg1 == 1;
                    int size = DeviceIdleController.this.mMaintenanceActivityListeners.beginBroadcast();
                    for (int i2 = 0; i2 < size; i2++) {
                        try {
                            DeviceIdleController.this.mMaintenanceActivityListeners.getBroadcastItem(i2).onMaintenanceActivityChanged(active);
                        } catch (RemoteException e4) {
                        } catch (Throwable th) {
                            DeviceIdleController.this.mMaintenanceActivityListeners.finishBroadcast();
                            throw th;
                        }
                    }
                    DeviceIdleController.this.mMaintenanceActivityListeners.finishBroadcast();
                    return;
                case 8:
                    DeviceIdleController.this.decActiveIdleOps();
                    return;
                default:
                    return;
            }
        }
    }

    private final class BinderService extends IDeviceIdleController.Stub {
        BinderService(DeviceIdleController this$0, BinderService binderService) {
            this();
        }

        private BinderService() {
        }

        public void addPowerSaveWhitelistApp(String name) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.addPowerSaveWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public void removePowerSaveWhitelistApp(String name) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removePowerSaveWhitelistAppInternal(name);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public String[] getSystemPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getSystemPowerWhitelistExceptIdleInternal();
        }

        public String[] getSystemPowerWhitelist() {
            return DeviceIdleController.this.getSystemPowerWhitelistInternal();
        }

        public String[] getUserPowerWhitelist() {
            return DeviceIdleController.this.getUserPowerWhitelistInternal();
        }

        public String[] getFullPowerWhitelistExceptIdle() {
            return DeviceIdleController.this.getFullPowerWhitelistExceptIdleInternal();
        }

        public String[] getFullPowerWhitelist() {
            return DeviceIdleController.this.getFullPowerWhitelistInternal();
        }

        public int[] getAppIdWhitelistExceptIdle() {
            return DeviceIdleController.this.getAppIdWhitelistExceptIdleInternal();
        }

        public int[] getAppIdWhitelist() {
            return DeviceIdleController.this.getAppIdWhitelistInternal();
        }

        public int[] getAppIdUserWhitelist() {
            return DeviceIdleController.this.getAppIdUserWhitelistInternal();
        }

        public int[] getAppIdTempWhitelist() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }

        public boolean isPowerSaveWhitelistExceptIdleApp(String name) {
            return DeviceIdleController.this.isPowerSaveWhitelistExceptIdleAppInternal(name);
        }

        public boolean isPowerSaveWhitelistApp(String name) {
            return DeviceIdleController.this.isPowerSaveWhitelistAppInternal(name);
        }

        public void addPowerSaveTempWhitelistApp(String packageName, long duration, int userId, String reason) throws RemoteException {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
        }

        public long addPowerSaveTempWhitelistAppForMms(String packageName, int userId, String reason) throws RemoteException {
            long duration = DeviceIdleController.this.mConstants.MMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
            return duration;
        }

        public long addPowerSaveTempWhitelistAppForSms(String packageName, int userId, String reason) throws RemoteException {
            long duration = DeviceIdleController.this.mConstants.SMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(packageName, duration, userId, reason);
            return duration;
        }

        public void exitIdle(String reason) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long ident = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.exitIdleInternal(reason);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

        public boolean registerMaintenanceActivityListener(IMaintenanceActivityListener listener) {
            return DeviceIdleController.this.registerMaintenanceActivityListener(listener);
        }

        public void unregisterMaintenanceActivityListener(IMaintenanceActivityListener listener) {
            DeviceIdleController.this.unregisterMaintenanceActivityListener(listener);
        }

        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            DeviceIdleController.this.dump(fd, pw, args);
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ResultReceiver resultReceiver) {
            DeviceIdleController.this.new Shell().exec(this, in, out, err, args, resultReceiver);
        }
    }

    public final class LocalService {
        public LocalService() {
        }

        public void addPowerSaveTempWhitelistAppDirect(int appId, long duration, boolean sync, String reason) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppDirectInternal(0, appId, duration, sync, reason);
        }

        public long getNotificationWhitelistDuration() {
            return DeviceIdleController.this.mConstants.NOTIFICATION_WHITELIST_DURATION;
        }

        public void setNetworkPolicyTempWhitelistCallback(Runnable callback) {
            DeviceIdleController.this.setNetworkPolicyTempWhitelistCallbackInternal(callback);
        }

        public void setJobsActive(boolean active) {
            DeviceIdleController.this.setJobsActive(active);
        }

        public void setAlarmsActive(boolean active) {
            DeviceIdleController.this.setAlarmsActive(active);
        }

        public int[] getPowerSaveWhitelistUserAppIds() {
            return DeviceIdleController.this.getPowerSaveWhitelistUserAppIds();
        }
    }

    public DeviceIdleController(Context context) {
        super(context);
        this.mMaintenanceActivityListeners = new RemoteCallbackList<>();
        this.mPowerSaveWhitelistAppsExceptIdle = new ArrayMap<>();
        this.mPowerSaveWhitelistApps = new ArrayMap<>();
        this.mPowerSaveWhitelistUserApps = new ArrayMap<>();
        this.mPowerSaveWhitelistSystemAppIdsExceptIdle = new SparseBooleanArray();
        this.mPowerSaveWhitelistSystemAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistExceptIdleAppIdArray = new int[0];
        this.mPowerSaveWhitelistAllAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistAllAppIdArray = new int[0];
        this.mPowerSaveWhitelistUserAppIds = new SparseBooleanArray();
        this.mPowerSaveWhitelistUserAppIdArray = new int[0];
        this.mTempWhitelistAppIdEndTimes = new SparseArray<>();
        this.mTempWhitelistAppIdArray = new int[0];
        this.mEventCmds = new int[100];
        this.mEventTimes = new long[100];
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Uri data;
                String ssp;
                String action = intent.getAction();
                if (!action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if (!action.equals("android.intent.action.BATTERY_CHANGED")) {
                        if (!action.equals("android.intent.action.PACKAGE_REMOVED") || intent.getBooleanExtra("android.intent.extra.REPLACING", false) || (data = intent.getData()) == null || (ssp = data.getSchemeSpecificPart()) == null) {
                            return;
                        }
                        DeviceIdleController.this.removePowerSaveWhitelistAppInternal(ssp);
                        return;
                    }
                    synchronized (DeviceIdleController.this) {
                        int plugged = intent.getIntExtra("plugged", 0);
                        DeviceIdleController.this.updateChargingLocked(plugged != 0);
                    }
                    return;
                }
                DeviceIdleController.this.updateConnectivityState(intent);
            }
        };
        this.mLightAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.stepLightIdleStateLocked("s:alarm");
                }
            }
        };
        this.mSensingTimeoutAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                if (DeviceIdleController.this.mState != 3) {
                    return;
                }
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.becomeInactiveIfAppropriateLocked();
                }
            }
        };
        this.mDeepAlarmListener = new AlarmManager.OnAlarmListener() {
            @Override
            public void onAlarm() {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.stepIdleStateLocked("s:alarm");
                }
            }
        };
        this.mIdleStartedDoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(intent.getAction())) {
                    DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_DEEP_MAINTENANCE_TIME);
                } else {
                    DeviceIdleController.this.mHandler.sendEmptyMessageDelayed(8, DeviceIdleController.this.mConstants.MIN_LIGHT_MAINTENANCE_TIME);
                }
            }
        };
        this.mDisplayListener = new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
            }

            @Override
            public void onDisplayRemoved(int displayId) {
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (displayId != 0) {
                    return;
                }
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.updateDisplayLocked();
                }
            }
        };
        this.mMotionListener = new MotionListener(this, null);
        this.mGenericLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.receivedGenericLocationLocked(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        this.mGpsLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.receivedGpsLocationLocked(location);
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        this.mConfigFile = new AtomicFile(new File(getSystemDir(), "deviceidle.xml"));
        this.mHandler = new MyHandler(BackgroundThread.getHandler().getLooper());
    }

    int[] getPowerSaveWhitelistUserAppIds() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    @Override
    public void onStart() {
        PackageManager pm = getContext().getPackageManager();
        synchronized (this) {
            boolean z = getContext().getResources().getBoolean(R.^attr-private.alwaysFocusable);
            this.mDeepEnabled = z;
            this.mLightEnabled = z;
            if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals(1)) {
                this.mDeepEnabled = false;
                this.mLightEnabled = true;
            } else if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals(2)) {
                this.mDeepEnabled = true;
                this.mLightEnabled = false;
            } else if (SystemProperties.get(CONFIG_AUTO_POWER, "0").equals(3)) {
                this.mDeepEnabled = true;
                this.mLightEnabled = true;
            }
            SystemConfig sysConfig = SystemConfig.getInstance();
            ArraySet<String> allowPowerExceptIdle = sysConfig.getAllowInPowerSaveExceptIdle();
            for (int i = 0; i < allowPowerExceptIdle.size(); i++) {
                String pkg = allowPowerExceptIdle.valueAt(i);
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(pkg, PackageManagerService.DumpState.DUMP_DEXOPT);
                    int appid = UserHandle.getAppId(ai.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai.packageName, Integer.valueOf(appid));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid, true);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            ArraySet<String> allowPower = sysConfig.getAllowInPowerSave();
            for (int i2 = 0; i2 < allowPower.size(); i2++) {
                String pkg2 = allowPower.valueAt(i2);
                try {
                    ApplicationInfo ai2 = pm.getApplicationInfo(pkg2, PackageManagerService.DumpState.DUMP_DEXOPT);
                    int appid2 = UserHandle.getAppId(ai2.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(ai2.packageName, Integer.valueOf(appid2));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appid2, true);
                    this.mPowerSaveWhitelistApps.put(ai2.packageName, Integer.valueOf(appid2));
                    this.mPowerSaveWhitelistSystemAppIds.put(appid2, true);
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            this.mConstants = new Constants(this.mHandler, getContext().getContentResolver());
            readConfigFileLocked();
            updateWhitelistAppIdsLocked();
            this.mNetworkConnected = true;
            this.mScreenOn = true;
            this.mCharging = true;
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
        }
        this.mBinderService = new BinderService(this, null);
        publishBinderService("deviceidle", this.mBinderService);
        publishLocalService(LocalService.class, new LocalService());
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 500) {
            synchronized (this) {
                this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
                this.mBatteryStats = BatteryStatsService.getService();
                this.mLocalPowerManager = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
                this.mPowerManager = (PowerManager) getContext().getSystemService(PowerManager.class);
                this.mActiveIdleWakeLock = this.mPowerManager.newWakeLock(1, "deviceidle_maint");
                this.mActiveIdleWakeLock.setReferenceCounted(false);
                this.mConnectivityService = (ConnectivityService) ServiceManager.getService("connectivity");
                this.mLocalAlarmManager = (AlarmManagerService.LocalService) getLocalService(AlarmManagerService.LocalService.class);
                this.mNetworkPolicyManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
                this.mDisplayManager = (DisplayManager) getContext().getSystemService("display");
                this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
                int sigMotionSensorId = getContext().getResources().getInteger(R.integer.button_pressed_animation_delay);
                if (sigMotionSensorId > 0) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(sigMotionSensorId, true);
                }
                if (this.mMotionSensor == null && getContext().getResources().getBoolean(R.^attr-private.aspect)) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(26, true);
                }
                if (this.mMotionSensor == null) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(17, true);
                }
                if (getContext().getResources().getBoolean(R.^attr-private.autofillDatasetPickerMaxHeight)) {
                    this.mLocationManager = (LocationManager) getContext().getSystemService("location");
                    this.mLocationRequest = new LocationRequest().setQuality(100).setInterval(0L).setFastestInterval(0L).setNumUpdates(1);
                }
                float angleThreshold = getContext().getResources().getInteger(R.integer.bugreport_state_unknown) / 100.0f;
                this.mAnyMotionDetector = new AnyMotionDetector((PowerManager) getContext().getSystemService("power"), this.mHandler, this.mSensorManager, this, angleThreshold);
                this.mIdleIntent = new Intent("android.os.action.DEVICE_IDLE_MODE_CHANGED");
                this.mIdleIntent.addFlags(1342177280);
                this.mLightIdleIntent = new Intent("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
                this.mLightIdleIntent.addFlags(1342177280);
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.BATTERY_CHANGED");
                getContext().registerReceiver(this.mReceiver, filter);
                IntentFilter filter2 = new IntentFilter();
                filter2.addAction("android.intent.action.PACKAGE_REMOVED");
                filter2.addDataScheme("package");
                getContext().registerReceiver(this.mReceiver, filter2);
                IntentFilter filter3 = new IntentFilter();
                filter3.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                getContext().registerReceiver(this.mReceiver, filter3);
                this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
                this.mLocalAlarmManager.setDeviceIdleUserWhitelist(this.mPowerSaveWhitelistUserAppIdArray);
                this.mDisplayManager.registerDisplayListener(this.mDisplayListener, null);
                updateDisplayLocked();
            }
            updateConnectivityState(null);
        }
    }

    public boolean addPowerSaveWhitelistAppInternal(String name) {
        synchronized (this) {
            try {
                ApplicationInfo ai = getContext().getPackageManager().getApplicationInfo(name, PackageManagerService.DumpState.DUMP_PREFERRED_XML);
                if (this.mPowerSaveWhitelistUserApps.put(name, Integer.valueOf(UserHandle.getAppId(ai.uid))) == null) {
                    reportPowerSaveWhitelistChangedLocked();
                    updateWhitelistAppIdsLocked();
                    writeConfigFileLocked();
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        return true;
    }

    public boolean removePowerSaveWhitelistAppInternal(String name) {
        synchronized (this) {
            if (this.mPowerSaveWhitelistUserApps.remove(name) != null) {
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean getPowerSaveWhitelistAppInternal(String name) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = this.mPowerSaveWhitelistUserApps.containsKey(name);
        }
        return zContainsKey;
    }

    public String[] getSystemPowerWhitelistExceptIdleInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getSystemPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size();
            apps = new String[size];
            for (int i = 0; i < size; i++) {
                apps[i] = this.mPowerSaveWhitelistApps.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getUserPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
                apps[i] = this.mPowerSaveWhitelistUserApps.keyAt(i);
            }
        }
        return apps;
    }

    public String[] getFullPowerWhitelistExceptIdleInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size() + this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            int cur = 0;
            for (int i = 0; i < this.mPowerSaveWhitelistAppsExceptIdle.size(); i++) {
                apps[cur] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
                cur++;
            }
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistUserApps.size(); i2++) {
                apps[cur] = this.mPowerSaveWhitelistUserApps.keyAt(i2);
                cur++;
            }
        }
        return apps;
    }

    public String[] getFullPowerWhitelistInternal() {
        String[] apps;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size() + this.mPowerSaveWhitelistUserApps.size();
            apps = new String[size];
            int cur = 0;
            for (int i = 0; i < this.mPowerSaveWhitelistApps.size(); i++) {
                apps[cur] = this.mPowerSaveWhitelistApps.keyAt(i);
                cur++;
            }
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistUserApps.size(); i2++) {
                apps[cur] = this.mPowerSaveWhitelistUserApps.keyAt(i2);
                cur++;
            }
        }
        return apps;
    }

    public boolean isPowerSaveWhitelistExceptIdleAppInternal(String packageName) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = !this.mPowerSaveWhitelistAppsExceptIdle.containsKey(packageName) ? this.mPowerSaveWhitelistUserApps.containsKey(packageName) : true;
        }
        return zContainsKey;
    }

    public boolean isPowerSaveWhitelistAppInternal(String packageName) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = !this.mPowerSaveWhitelistApps.containsKey(packageName) ? this.mPowerSaveWhitelistUserApps.containsKey(packageName) : true;
        }
        return zContainsKey;
    }

    public int[] getAppIdWhitelistExceptIdleInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistExceptIdleAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistAllAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdUserWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mPowerSaveWhitelistUserAppIdArray;
        }
        return iArr;
    }

    public int[] getAppIdTempWhitelistInternal() {
        int[] iArr;
        synchronized (this) {
            iArr = this.mTempWhitelistAppIdArray;
        }
        return iArr;
    }

    void addPowerSaveTempWhitelistAppChecked(String packageName, long duration, int userId, String reason) throws RemoteException {
        getContext().enforceCallingPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int callingUid = Binder.getCallingUid();
        int userId2 = ActivityManagerNative.getDefault().handleIncomingUser(Binder.getCallingPid(), callingUid, userId, false, false, "addPowerSaveTempWhitelistApp", (String) null);
        long token = Binder.clearCallingIdentity();
        try {
            addPowerSaveTempWhitelistAppInternal(callingUid, packageName, duration, userId2, true, reason);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void addPowerSaveTempWhitelistAppInternal(int callingUid, String packageName, long duration, int userId, boolean sync, String reason) {
        try {
            int uid = getContext().getPackageManager().getPackageUidAsUser(packageName, userId);
            int appId = UserHandle.getAppId(uid);
            addPowerSaveTempWhitelistAppDirectInternal(callingUid, appId, duration, sync, reason);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    void addPowerSaveTempWhitelistAppDirectInternal(int callingUid, int appId, long duration, boolean sync, String reason) {
        long timeNow = SystemClock.elapsedRealtime();
        Runnable networkPolicyTempWhitelistCallback = null;
        synchronized (this) {
            int callingAppId = UserHandle.getAppId(callingUid);
            if (callingAppId >= 10000 && !this.mPowerSaveWhitelistSystemAppIds.get(callingAppId)) {
                throw new SecurityException("Calling app " + UserHandle.formatUid(callingUid) + " is not on whitelist");
            }
            long duration2 = Math.min(duration, this.mConstants.MAX_TEMP_APP_WHITELIST_DURATION);
            Pair<MutableLong, String> entry = this.mTempWhitelistAppIdEndTimes.get(appId);
            boolean newEntry = entry == null;
            if (newEntry) {
                entry = new Pair<>(new MutableLong(0L), reason);
                this.mTempWhitelistAppIdEndTimes.put(appId, entry);
            }
            ((MutableLong) entry.first).value = timeNow + duration2;
            if (newEntry) {
                try {
                    this.mBatteryStats.noteEvent(32785, reason, appId);
                } catch (RemoteException e) {
                }
                postTempActiveTimeoutMessage(appId, duration2);
                updateTempWhitelistAppIdsLocked();
                if (this.mNetworkPolicyTempWhitelistCallback != null) {
                    if (!sync) {
                        this.mHandler.post(this.mNetworkPolicyTempWhitelistCallback);
                    } else {
                        networkPolicyTempWhitelistCallback = this.mNetworkPolicyTempWhitelistCallback;
                    }
                }
                reportTempWhitelistChangedLocked();
            }
        }
        if (networkPolicyTempWhitelistCallback == null) {
            return;
        }
        networkPolicyTempWhitelistCallback.run();
    }

    public void setNetworkPolicyTempWhitelistCallbackInternal(Runnable callback) {
        synchronized (this) {
            this.mNetworkPolicyTempWhitelistCallback = callback;
        }
    }

    private void postTempActiveTimeoutMessage(int uid, long delay) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6, uid, 0), delay);
    }

    void checkTempAppWhitelistTimeout(int uid) {
        long timeNow = SystemClock.elapsedRealtime();
        synchronized (this) {
            Pair<MutableLong, String> entry = this.mTempWhitelistAppIdEndTimes.get(uid);
            if (entry != null) {
                if (timeNow >= ((MutableLong) entry.first).value) {
                    this.mTempWhitelistAppIdEndTimes.delete(uid);
                    updateTempWhitelistAppIdsLocked();
                    if (this.mNetworkPolicyTempWhitelistCallback != null) {
                        this.mHandler.post(this.mNetworkPolicyTempWhitelistCallback);
                    }
                    reportTempWhitelistChangedLocked();
                    try {
                        this.mBatteryStats.noteEvent(16401, (String) entry.second, uid);
                    } catch (RemoteException e) {
                    }
                } else {
                    postTempActiveTimeoutMessage(uid, ((MutableLong) entry.first).value - timeNow);
                }
            }
        }
    }

    public void exitIdleInternal(String reason) {
        synchronized (this) {
            becomeActiveLocked(reason, Binder.getCallingUid());
        }
    }

    void updateConnectivityState(Intent connIntent) {
        ConnectivityService cm;
        boolean conn;
        synchronized (this) {
            cm = this.mConnectivityService;
        }
        if (cm == null) {
            return;
        }
        NetworkInfo ni = cm.getActiveNetworkInfo();
        synchronized (this) {
            if (ni == null) {
                conn = false;
            } else if (connIntent == null) {
                conn = ni.isConnected();
            } else {
                int networkType = connIntent.getIntExtra("networkType", -1);
                if (ni.getType() != networkType) {
                    return;
                } else {
                    conn = !connIntent.getBooleanExtra("noConnectivity", false);
                }
            }
            if (conn != this.mNetworkConnected) {
                this.mNetworkConnected = conn;
                if (conn && this.mLightState == 5) {
                    stepLightIdleStateLocked("network");
                }
            }
        }
    }

    void updateDisplayLocked() {
        this.mCurDisplay = this.mDisplayManager.getDisplay(0);
        boolean screenOn = this.mCurDisplay.getState() == 2;
        if (!screenOn && this.mScreenOn) {
            this.mScreenOn = false;
            if (this.mForceIdle) {
                return;
            }
            becomeInactiveIfAppropriateLocked();
            return;
        }
        if (!screenOn) {
            return;
        }
        this.mScreenOn = true;
        if (this.mForceIdle) {
            return;
        }
        becomeActiveLocked("screen", Process.myUid());
    }

    void updateChargingLocked(boolean charging) {
        if (!charging && this.mCharging) {
            this.mCharging = false;
            if (this.mForceIdle) {
                return;
            }
            becomeInactiveIfAppropriateLocked();
            return;
        }
        if (!charging) {
            return;
        }
        this.mCharging = charging;
        if (this.mForceIdle) {
            return;
        }
        becomeActiveLocked("charging", Process.myUid());
    }

    void scheduleReportActiveLocked(String activeReason, int activeUid) {
        Message msg = this.mHandler.obtainMessage(5, activeUid, 0, activeReason);
        this.mHandler.sendMessage(msg);
    }

    void becomeActiveLocked(String activeReason, int activeUid) {
        if (this.mState == 0 && this.mLightState == 0) {
            return;
        }
        EventLogTags.writeDeviceIdle(0, activeReason);
        EventLogTags.writeDeviceIdleLight(0, activeReason);
        scheduleReportActiveLocked(activeReason, activeUid);
        this.mState = 0;
        this.mLightState = 0;
        this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
        this.mCurIdleBudget = 0L;
        this.mMaintenanceStartTime = 0L;
        resetIdleManagementLocked();
        resetLightIdleManagementLocked();
        addEvent(1);
    }

    void becomeInactiveIfAppropriateLocked() {
        if ((this.mScreenOn || this.mCharging) && !this.mForceIdle) {
            return;
        }
        if (this.mState == 0 && this.mDeepEnabled) {
            this.mState = 1;
            resetIdleManagementLocked();
            scheduleAlarmLocked(this.mInactiveTimeout, false);
            EventLogTags.writeDeviceIdle(this.mState, "no activity");
        }
        if (this.mLightState != 0 || !this.mLightEnabled) {
            return;
        }
        this.mLightState = 1;
        resetLightIdleManagementLocked();
        scheduleLightAlarmLocked(this.mConstants.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
        EventLogTags.writeDeviceIdleLight(this.mLightState, "no activity");
    }

    void resetIdleManagementLocked() {
        this.mNextIdlePendingDelay = 0L;
        this.mNextIdleDelay = 0L;
        this.mNextLightIdleDelay = 0L;
        cancelAlarmLocked();
        cancelSensingTimeoutAlarmLocked();
        cancelLocatingLocked();
        stopMonitoringMotionLocked();
        this.mAnyMotionDetector.stop();
    }

    void resetLightIdleManagementLocked() {
        cancelLightAlarmLocked();
    }

    void exitForceIdleLocked() {
        if (!this.mForceIdle) {
            return;
        }
        this.mForceIdle = false;
        if (!this.mScreenOn && !this.mCharging) {
            return;
        }
        becomeActiveLocked("exit-force", Process.myUid());
    }

    void stepLightIdleStateLocked(String reason) {
        if (this.mLightState == 7) {
            return;
        }
        EventLogTags.writeDeviceIdleLightStep();
        switch (this.mLightState) {
            case 1:
                this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
                this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
                this.mMaintenanceStartTime = 0L;
                if (!isOpsInactiveLocked()) {
                    this.mLightState = 3;
                    EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                    scheduleLightAlarmLocked(this.mConstants.LIGHT_PRE_IDLE_TIMEOUT);
                    return;
                }
                break;
            case 2:
            default:
                return;
            case 3:
            case 6:
                break;
            case 4:
            case 5:
                if (this.mNetworkConnected || this.mLightState == 5) {
                    this.mActiveIdleOpCount = 1;
                    this.mActiveIdleWakeLock.acquire();
                    this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
                    if (this.mCurIdleBudget < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                        this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
                    } else if (this.mCurIdleBudget > this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET) {
                        this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET;
                    }
                    scheduleLightAlarmLocked(this.mCurIdleBudget);
                    this.mLightState = 6;
                    EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                    addEvent(3);
                    this.mHandler.sendEmptyMessage(4);
                    return;
                }
                scheduleLightAlarmLocked(this.mNextLightIdleDelay);
                this.mLightState = 5;
                EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
                return;
        }
        if (this.mMaintenanceStartTime != 0) {
            long duration = SystemClock.elapsedRealtime() - this.mMaintenanceStartTime;
            if (duration < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                this.mCurIdleBudget += this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET - duration;
            } else {
                this.mCurIdleBudget -= duration - this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            }
        }
        this.mMaintenanceStartTime = 0L;
        scheduleLightAlarmLocked(this.mNextLightIdleDelay);
        this.mNextLightIdleDelay = Math.min(this.mConstants.LIGHT_MAX_IDLE_TIMEOUT, (long) (this.mNextLightIdleDelay * this.mConstants.LIGHT_IDLE_FACTOR));
        if (this.mNextLightIdleDelay < this.mConstants.LIGHT_IDLE_TIMEOUT) {
            this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
        }
        this.mLightState = 4;
        EventLogTags.writeDeviceIdleLight(this.mLightState, reason);
        addEvent(2);
        this.mHandler.sendEmptyMessage(3);
    }

    void stepIdleStateLocked(String reason) {
        EventLogTags.writeDeviceIdleStep();
        long now = SystemClock.elapsedRealtime();
        if (this.mConstants.MIN_TIME_TO_ALARM + now > this.mAlarmManager.getNextWakeFromIdleTime()) {
            if (this.mState != 0) {
                becomeActiveLocked("alarm", Process.myUid());
                becomeInactiveIfAppropriateLocked();
            }
            return;
        }
        switch (this.mState) {
            case 1:
                startMonitoringMotionLocked();
                scheduleAlarmLocked(this.mConstants.IDLE_AFTER_INACTIVE_TIMEOUT, false);
                this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                this.mState = 2;
                EventLogTags.writeDeviceIdle(this.mState, reason);
                break;
            case 2:
                this.mState = 3;
                EventLogTags.writeDeviceIdle(this.mState, reason);
                scheduleSensingTimeoutAlarmLocked(this.mConstants.SENSING_TIMEOUT);
                cancelLocatingLocked();
                this.mNotMoving = false;
                this.mLocated = false;
                this.mLastGenericLocation = null;
                this.mLastGpsLocation = null;
                this.mAnyMotionDetector.checkForAnyMotion();
                break;
            case 3:
                cancelSensingTimeoutAlarmLocked();
                this.mState = 4;
                EventLogTags.writeDeviceIdle(this.mState, reason);
                scheduleAlarmLocked(this.mConstants.LOCATING_TIMEOUT, false);
                if (this.mLocationManager != null && this.mLocationManager.getProvider("network") != null) {
                    this.mLocationManager.requestLocationUpdates(this.mLocationRequest, this.mGenericLocationListener, this.mHandler.getLooper());
                    this.mLocating = true;
                } else {
                    this.mHasNetworkLocation = false;
                }
                if (this.mLocationManager != null && this.mLocationManager.getProvider("gps") != null) {
                    this.mHasGps = true;
                    this.mLocationManager.requestLocationUpdates("gps", 1000L, 5.0f, this.mGpsLocationListener, this.mHandler.getLooper());
                    this.mLocating = true;
                } else {
                    this.mHasGps = false;
                }
                if (this.mLocating) {
                }
                cancelAlarmLocked();
                cancelLocatingLocked();
                this.mAnyMotionDetector.stop();
                scheduleAlarmLocked(this.mNextIdleDelay, true);
                this.mNextIdleDelay = (long) (this.mNextIdleDelay * this.mConstants.IDLE_FACTOR);
                this.mNextIdleDelay = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                if (this.mNextIdleDelay < this.mConstants.IDLE_TIMEOUT) {
                    this.mNextIdleDelay = this.mConstants.IDLE_TIMEOUT;
                }
                this.mState = 5;
                if (this.mLightState != 7) {
                    this.mLightState = 7;
                    cancelLightAlarmLocked();
                }
                EventLogTags.writeDeviceIdle(this.mState, reason);
                addEvent(4);
                this.mHandler.sendEmptyMessage(2);
                break;
            case 4:
                cancelAlarmLocked();
                cancelLocatingLocked();
                this.mAnyMotionDetector.stop();
                scheduleAlarmLocked(this.mNextIdleDelay, true);
                this.mNextIdleDelay = (long) (this.mNextIdleDelay * this.mConstants.IDLE_FACTOR);
                this.mNextIdleDelay = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                if (this.mNextIdleDelay < this.mConstants.IDLE_TIMEOUT) {
                }
                this.mState = 5;
                if (this.mLightState != 7) {
                }
                EventLogTags.writeDeviceIdle(this.mState, reason);
                addEvent(4);
                this.mHandler.sendEmptyMessage(2);
                break;
            case 5:
                this.mActiveIdleOpCount = 1;
                this.mActiveIdleWakeLock.acquire();
                scheduleAlarmLocked(this.mNextIdlePendingDelay, false);
                this.mMaintenanceStartTime = SystemClock.elapsedRealtime();
                this.mNextIdlePendingDelay = Math.min(this.mConstants.MAX_IDLE_PENDING_TIMEOUT, (long) (this.mNextIdlePendingDelay * this.mConstants.IDLE_PENDING_FACTOR));
                if (this.mNextIdlePendingDelay < this.mConstants.IDLE_PENDING_TIMEOUT) {
                    this.mNextIdlePendingDelay = this.mConstants.IDLE_PENDING_TIMEOUT;
                }
                this.mState = 6;
                EventLogTags.writeDeviceIdle(this.mState, reason);
                addEvent(5);
                this.mHandler.sendEmptyMessage(4);
                break;
            case 6:
                scheduleAlarmLocked(this.mNextIdleDelay, true);
                this.mNextIdleDelay = (long) (this.mNextIdleDelay * this.mConstants.IDLE_FACTOR);
                this.mNextIdleDelay = Math.min(this.mNextIdleDelay, this.mConstants.MAX_IDLE_TIMEOUT);
                if (this.mNextIdleDelay < this.mConstants.IDLE_TIMEOUT) {
                }
                this.mState = 5;
                if (this.mLightState != 7) {
                }
                EventLogTags.writeDeviceIdle(this.mState, reason);
                addEvent(4);
                this.mHandler.sendEmptyMessage(2);
                break;
        }
    }

    void incActiveIdleOps() {
        synchronized (this) {
            this.mActiveIdleOpCount++;
        }
    }

    void decActiveIdleOps() {
        synchronized (this) {
            this.mActiveIdleOpCount--;
            if (this.mActiveIdleOpCount <= 0) {
                exitMaintenanceEarlyIfNeededLocked();
                this.mActiveIdleWakeLock.release();
            }
        }
    }

    void setJobsActive(boolean active) {
        synchronized (this) {
            this.mJobsActive = active;
            reportMaintenanceActivityIfNeededLocked();
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    void setAlarmsActive(boolean active) {
        synchronized (this) {
            this.mAlarmsActive = active;
            if (!active) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    boolean registerMaintenanceActivityListener(IMaintenanceActivityListener listener) {
        boolean z;
        synchronized (this) {
            this.mMaintenanceActivityListeners.register(listener);
            z = this.mReportedMaintenanceActivity;
        }
        return z;
    }

    void unregisterMaintenanceActivityListener(IMaintenanceActivityListener listener) {
        synchronized (this) {
            this.mMaintenanceActivityListeners.unregister(listener);
        }
    }

    void reportMaintenanceActivityIfNeededLocked() {
        boolean active = this.mJobsActive;
        if (active == this.mReportedMaintenanceActivity) {
            return;
        }
        this.mReportedMaintenanceActivity = active;
        Message msg = this.mHandler.obtainMessage(7, this.mReportedMaintenanceActivity ? 1 : 0, 0);
        this.mHandler.sendMessage(msg);
    }

    boolean isOpsInactiveLocked() {
        return (this.mActiveIdleOpCount > 0 || this.mJobsActive || this.mAlarmsActive) ? false : true;
    }

    void exitMaintenanceEarlyIfNeededLocked() {
        if ((this.mState != 6 && this.mLightState != 6 && this.mLightState != 3) || !isOpsInactiveLocked()) {
            return;
        }
        SystemClock.elapsedRealtime();
        if (this.mState == 6) {
            stepIdleStateLocked("s:early");
        } else if (this.mLightState == 3) {
            stepLightIdleStateLocked("s:predone");
        } else {
            stepLightIdleStateLocked("s:early");
        }
    }

    void motionLocked() {
        handleMotionDetectedLocked(this.mConstants.MOTION_INACTIVE_TIMEOUT, "motion");
    }

    void handleMotionDetectedLocked(long timeout, String type) {
        boolean becomeInactive = false;
        if (this.mState != 0) {
            scheduleReportActiveLocked(type, Process.myUid());
            this.mState = 0;
            this.mInactiveTimeout = timeout;
            this.mCurIdleBudget = 0L;
            this.mMaintenanceStartTime = 0L;
            EventLogTags.writeDeviceIdle(this.mState, type);
            addEvent(1);
            becomeInactive = true;
        }
        if (this.mLightState == 7) {
            this.mLightState = 0;
            EventLogTags.writeDeviceIdleLight(this.mLightState, type);
            becomeInactive = true;
        }
        if (!becomeInactive) {
            return;
        }
        becomeInactiveIfAppropriateLocked();
    }

    void receivedGenericLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        this.mLastGenericLocation = new Location(location);
        if (location.getAccuracy() > this.mConstants.LOCATION_ACCURACY && this.mHasGps) {
            return;
        }
        this.mLocated = true;
        if (!this.mNotMoving) {
            return;
        }
        stepIdleStateLocked("s:location");
    }

    void receivedGpsLocationLocked(Location location) {
        if (this.mState != 4) {
            cancelLocatingLocked();
            return;
        }
        this.mLastGpsLocation = new Location(location);
        if (location.getAccuracy() > this.mConstants.LOCATION_ACCURACY) {
            return;
        }
        this.mLocated = true;
        if (!this.mNotMoving) {
            return;
        }
        stepIdleStateLocked("s:gps");
    }

    void startMonitoringMotionLocked() {
        if (this.mMotionSensor == null || this.mMotionListener.active) {
            return;
        }
        this.mMotionListener.registerLocked();
    }

    void stopMonitoringMotionLocked() {
        if (this.mMotionSensor == null || !this.mMotionListener.active) {
            return;
        }
        this.mMotionListener.unregisterLocked();
    }

    void cancelAlarmLocked() {
        if (this.mNextAlarmTime == 0) {
            return;
        }
        this.mNextAlarmTime = 0L;
        this.mAlarmManager.cancel(this.mDeepAlarmListener);
    }

    void cancelLightAlarmLocked() {
        if (this.mNextLightAlarmTime == 0) {
            return;
        }
        this.mNextLightAlarmTime = 0L;
        this.mAlarmManager.cancel(this.mLightAlarmListener);
    }

    void cancelLocatingLocked() {
        if (!this.mLocating) {
            return;
        }
        this.mLocationManager.removeUpdates(this.mGenericLocationListener);
        this.mLocationManager.removeUpdates(this.mGpsLocationListener);
        this.mLocating = false;
    }

    void cancelSensingTimeoutAlarmLocked() {
        if (this.mNextSensingTimeoutAlarmTime == 0) {
            return;
        }
        this.mNextSensingTimeoutAlarmTime = 0L;
        this.mAlarmManager.cancel(this.mSensingTimeoutAlarmListener);
    }

    void scheduleAlarmLocked(long delay, boolean idleUntil) {
        if (this.mMotionSensor == null) {
            return;
        }
        this.mNextAlarmTime = SystemClock.elapsedRealtime() + delay;
        if (idleUntil) {
            this.mAlarmManager.setIdleUntil(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        }
    }

    void scheduleLightAlarmLocked(long delay) {
        this.mNextLightAlarmTime = SystemClock.elapsedRealtime() + delay;
        this.mAlarmManager.set(2, this.mNextLightAlarmTime, "DeviceIdleController.light", this.mLightAlarmListener, this.mHandler);
    }

    void scheduleSensingTimeoutAlarmLocked(long delay) {
        this.mNextSensingTimeoutAlarmTime = SystemClock.elapsedRealtime() + delay;
        this.mAlarmManager.set(2, this.mNextSensingTimeoutAlarmTime, "DeviceIdleController.sensing", this.mSensingTimeoutAlarmListener, this.mHandler);
    }

    private static int[] buildAppIdArray(ArrayMap<String, Integer> systemApps, ArrayMap<String, Integer> userApps, SparseBooleanArray outAppIds) {
        outAppIds.clear();
        if (systemApps != null) {
            for (int i = 0; i < systemApps.size(); i++) {
                outAppIds.put(systemApps.valueAt(i).intValue(), true);
            }
        }
        if (userApps != null) {
            for (int i2 = 0; i2 < userApps.size(); i2++) {
                outAppIds.put(userApps.valueAt(i2).intValue(), true);
            }
        }
        int size = outAppIds.size();
        int[] appids = new int[size];
        for (int i3 = 0; i3 < size; i3++) {
            appids[i3] = outAppIds.keyAt(i3);
        }
        return appids;
    }

    private void updateWhitelistAppIdsLocked() {
        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
        this.mPowerSaveWhitelistAllAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistApps, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistAllAppIds);
        this.mPowerSaveWhitelistUserAppIdArray = buildAppIdArray(null, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistUserAppIds);
        if (this.mLocalPowerManager != null) {
            this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
        }
        if (this.mLocalAlarmManager == null) {
            return;
        }
        this.mLocalAlarmManager.setDeviceIdleUserWhitelist(this.mPowerSaveWhitelistUserAppIdArray);
    }

    private void updateTempWhitelistAppIdsLocked() {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (this.mTempWhitelistAppIdArray.length != size) {
            this.mTempWhitelistAppIdArray = new int[size];
        }
        for (int i = 0; i < size; i++) {
            this.mTempWhitelistAppIdArray[i] = this.mTempWhitelistAppIdEndTimes.keyAt(i);
        }
        if (this.mLocalPowerManager == null) {
            return;
        }
        this.mLocalPowerManager.setDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray);
    }

    private void reportPowerSaveWhitelistChangedLocked() {
        Intent intent = new Intent("android.os.action.POWER_SAVE_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    private void reportTempWhitelistChangedLocked() {
        Intent intent = new Intent("android.os.action.POWER_SAVE_TEMP_WHITELIST_CHANGED");
        intent.addFlags(1073741824);
        getContext().sendBroadcastAsUser(intent, UserHandle.SYSTEM);
    }

    void readConfigFileLocked() {
        this.mPowerSaveWhitelistUserApps.clear();
        try {
            FileInputStream stream = this.mConfigFile.openRead();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                readConfigFileLocked(parser);
                try {
                    stream.close();
                } catch (IOException e) {
                }
            } catch (XmlPullParserException e2) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException e4) {
                }
                throw th;
            }
        } catch (FileNotFoundException e5) {
        }
    }

    private void readConfigFileLocked(XmlPullParser parser) {
        int type;
        PackageManager pm = getContext().getPackageManager();
        do {
            try {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } catch (IOException e) {
                Slog.w(TAG, "Failed parsing config " + e);
                return;
            } catch (IllegalStateException e2) {
                Slog.w(TAG, "Failed parsing config " + e2);
                return;
            } catch (IndexOutOfBoundsException e3) {
                Slog.w(TAG, "Failed parsing config " + e3);
                return;
            } catch (NullPointerException e4) {
                Slog.w(TAG, "Failed parsing config " + e4);
                return;
            } catch (NumberFormatException e5) {
                Slog.w(TAG, "Failed parsing config " + e5);
                return;
            } catch (XmlPullParserException e6) {
                Slog.w(TAG, "Failed parsing config " + e6);
                return;
            }
        } while (type != 1);
        if (type != 2) {
            throw new IllegalStateException("no start tag found");
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type2 = parser.next();
            if (type2 == 1) {
                return;
            }
            if (type2 == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type2 != 3 && type2 != 4) {
                String tagName = parser.getName();
                if (tagName.equals("wl")) {
                    String name = parser.getAttributeValue(null, "n");
                    if (name != null) {
                        try {
                            ApplicationInfo ai = pm.getApplicationInfo(name, PackageManagerService.DumpState.DUMP_PREFERRED_XML);
                            this.mPowerSaveWhitelistUserApps.put(ai.packageName, Integer.valueOf(UserHandle.getAppId(ai.uid)));
                        } catch (PackageManager.NameNotFoundException e7) {
                        }
                    }
                } else {
                    Slog.w(TAG, "Unknown element under <config>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void writeConfigFileLocked() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC);
    }

    void handleWriteConfigFile() {
        ByteArrayOutputStream memStream = new ByteArrayOutputStream();
        try {
            synchronized (this) {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(memStream, StandardCharsets.UTF_8.name());
                writeConfigFileLocked(out);
            }
        } catch (IOException e) {
        }
        synchronized (this.mConfigFile) {
            FileOutputStream stream = null;
            try {
                stream = this.mConfigFile.startWrite();
                memStream.writeTo(stream);
                stream.flush();
                FileUtils.sync(stream);
                stream.close();
                this.mConfigFile.finishWrite(stream);
            } catch (IOException e2) {
                Slog.w(TAG, "Error writing config file", e2);
                this.mConfigFile.failWrite(stream);
            }
        }
    }

    void writeConfigFileLocked(XmlSerializer out) throws IOException {
        out.startDocument(null, true);
        out.startTag(null, "config");
        for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
            String name = this.mPowerSaveWhitelistUserApps.keyAt(i);
            out.startTag(null, "wl");
            out.attribute(null, "n", name);
            out.endTag(null, "wl");
        }
        out.endTag(null, "config");
        out.endDocument();
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device idle controller (deviceidle) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  step [light|deep]");
        pw.println("    Immediately step to next state, without waiting for alarm.");
        pw.println("  force-idle [light|deep]");
        pw.println("    Force directly into idle mode, regardless of other device state.");
        pw.println("  force-inactive");
        pw.println("    Force to be inactive, ready to freely step idle states.");
        pw.println("  unforce");
        pw.println("    Resume normal functioning after force-idle or force-inactive.");
        pw.println("  get [light|deep|force|screen|charging|network]");
        pw.println("    Retrieve the current given state.");
        pw.println("  disable [light|deep|all]");
        pw.println("    Completely disable device idle mode.");
        pw.println("  enable [light|deep|all]");
        pw.println("    Re-enable device idle mode after it had previously been disabled.");
        pw.println("  enabled [light|deep|all]");
        pw.println("    Print 1 if device idle mode is currently enabled, else 0.");
        pw.println("  whitelist");
        pw.println("    Print currently whitelisted apps.");
        pw.println("  whitelist [package ...]");
        pw.println("    Add (prefix with +) or remove (prefix with -) packages.");
        pw.println("  tempwhitelist");
        pw.println("    Print packages that are temporarily whitelisted.");
        pw.println("  tempwhitelist [-u] [package ..]");
        pw.println("    Temporarily place packages in whitelist for 10 seconds.");
    }

    class Shell extends ShellCommand {
        int userId = 0;

        Shell() {
        }

        public int onCommand(String cmd) {
            return DeviceIdleController.this.onShellCommand(this, cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            DeviceIdleController.dumpHelp(pw);
        }
    }

    int onShellCommand(Shell shell, String cmd) {
        long token;
        PrintWriter pw = shell.getOutPrintWriter();
        if ("step".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg = shell.getNextArg();
                if (arg != null) {
                    try {
                        if (!"deep".equals(arg)) {
                            if ("light".equals(arg)) {
                                stepLightIdleStateLocked("s:shell");
                                pw.print("Stepped to light: ");
                                pw.println(lightStateToString(this.mLightState));
                            } else {
                                pw.println("Unknown idle mode: " + arg);
                            }
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                stepIdleStateLocked("s:shell");
                pw.print("Stepped to deep: ");
                pw.println(stateToString(this.mState));
            }
            return 0;
        }
        if ("force-idle".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg2 = shell.getNextArg();
                if (arg2 != null) {
                    try {
                        if ("deep".equals(arg2)) {
                            if (!this.mDeepEnabled) {
                                pw.println("Unable to go deep idle; not enabled");
                                return -1;
                            }
                            this.mForceIdle = true;
                            becomeInactiveIfAppropriateLocked();
                            int curState = this.mState;
                            while (curState != 5) {
                                stepIdleStateLocked("s:shell");
                                if (curState == this.mState) {
                                    pw.print("Unable to go deep idle; stopped at ");
                                    pw.println(stateToString(this.mState));
                                    exitForceIdleLocked();
                                    return -1;
                                }
                                curState = this.mState;
                            }
                            pw.println("Now forced in to deep idle mode");
                        } else if ("light".equals(arg2)) {
                            this.mForceIdle = true;
                            becomeInactiveIfAppropriateLocked();
                            int curLightState = this.mLightState;
                            while (curLightState != 4) {
                                stepIdleStateLocked("s:shell");
                                if (curLightState == this.mLightState) {
                                    pw.print("Unable to go light idle; stopped at ");
                                    pw.println(lightStateToString(this.mLightState));
                                    exitForceIdleLocked();
                                    return -1;
                                }
                                curLightState = this.mLightState;
                            }
                            pw.println("Now forced in to light idle mode");
                        } else {
                            pw.println("Unknown idle mode: " + arg2);
                        }
                        Binder.restoreCallingIdentity(token);
                    } finally {
                    }
                }
            }
        } else if ("force-inactive".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                try {
                    this.mForceIdle = true;
                    becomeInactiveIfAppropriateLocked();
                    pw.print("Light state: ");
                    pw.print(lightStateToString(this.mLightState));
                    pw.print(", deep state: ");
                    pw.println(stateToString(this.mState));
                    Binder.restoreCallingIdentity(token);
                } finally {
                }
            }
        } else if ("unforce".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                try {
                    exitForceIdleLocked();
                    pw.print("Light state: ");
                    pw.print(lightStateToString(this.mLightState));
                    pw.print(", deep state: ");
                    pw.println(stateToString(this.mState));
                    Binder.restoreCallingIdentity(token);
                } finally {
                }
            }
        } else if ("get".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                String arg3 = shell.getNextArg();
                if (arg3 != null) {
                    token = Binder.clearCallingIdentity();
                    try {
                        if (arg3.equals("light")) {
                            pw.println(lightStateToString(this.mLightState));
                        } else if (arg3.equals("deep")) {
                            pw.println(stateToString(this.mState));
                        } else if (arg3.equals("force")) {
                            pw.println(this.mForceIdle);
                        } else if (arg3.equals("screen")) {
                            pw.println(this.mScreenOn);
                        } else if (arg3.equals("charging")) {
                            pw.println(this.mCharging);
                        } else if (arg3.equals("network")) {
                            pw.println(this.mNetworkConnected);
                        } else {
                            pw.println("Unknown get option: " + arg3);
                        }
                        Binder.restoreCallingIdentity(token);
                    } finally {
                    }
                } else {
                    pw.println("Argument required");
                }
            }
        } else if ("disable".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg4 = shell.getNextArg();
                boolean becomeActive = false;
                boolean valid = false;
                if (arg4 != null) {
                    try {
                        if ("deep".equals(arg4) || "all".equals(arg4)) {
                        }
                        if (arg4 != null || "light".equals(arg4) || "all".equals(arg4)) {
                            valid = true;
                            if (this.mLightEnabled) {
                                this.mLightEnabled = false;
                                becomeActive = true;
                                pw.println("Light idle mode disabled");
                            }
                        }
                        if (becomeActive) {
                            becomeActiveLocked((arg4 == null ? "all" : arg4) + "-disabled", Process.myUid());
                        }
                        if (!valid) {
                            pw.println("Unknown idle mode: " + arg4);
                        }
                        Binder.restoreCallingIdentity(token);
                    } finally {
                    }
                }
                valid = true;
                if (this.mDeepEnabled) {
                    this.mDeepEnabled = false;
                    becomeActive = true;
                    pw.println("Deep idle mode disabled");
                }
                if (arg4 != null) {
                }
                valid = true;
                if (this.mLightEnabled) {
                }
                if (becomeActive) {
                }
                if (!valid) {
                }
                Binder.restoreCallingIdentity(token);
            }
        } else if ("enable".equals(cmd)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                token = Binder.clearCallingIdentity();
                String arg5 = shell.getNextArg();
                boolean becomeInactive = false;
                boolean valid2 = false;
                if (arg5 != null) {
                    try {
                        if ("deep".equals(arg5) || "all".equals(arg5)) {
                        }
                        if (arg5 != null || "light".equals(arg5) || "all".equals(arg5)) {
                            valid2 = true;
                            if (!this.mLightEnabled) {
                                this.mLightEnabled = true;
                                becomeInactive = true;
                                pw.println("Light idle mode enable");
                            }
                        }
                        if (becomeInactive) {
                            becomeInactiveIfAppropriateLocked();
                        }
                        if (!valid2) {
                            pw.println("Unknown idle mode: " + arg5);
                        }
                        Binder.restoreCallingIdentity(token);
                    } finally {
                    }
                }
                valid2 = true;
                if (!this.mDeepEnabled) {
                    this.mDeepEnabled = true;
                    becomeInactive = true;
                    pw.println("Deep idle mode enabled");
                }
                if (arg5 != null) {
                }
                valid2 = true;
                if (!this.mLightEnabled) {
                }
                if (becomeInactive) {
                }
                if (!valid2) {
                }
                Binder.restoreCallingIdentity(token);
            }
        } else if ("enabled".equals(cmd)) {
            synchronized (this) {
                String arg6 = shell.getNextArg();
                if (arg6 == null || "all".equals(arg6)) {
                    pw.println((this.mDeepEnabled && this.mLightEnabled) ? "1" : 0);
                } else if ("deep".equals(arg6)) {
                    pw.println(this.mDeepEnabled ? "1" : 0);
                } else if ("light".equals(arg6)) {
                    pw.println(this.mLightEnabled ? "1" : 0);
                } else {
                    pw.println("Unknown idle mode: " + arg6);
                }
            }
        } else {
            if ("whitelist".equals(cmd)) {
                token = Binder.clearCallingIdentity();
                try {
                    String arg7 = shell.getNextArg();
                    if (arg7 != null) {
                        getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                        while (arg7.length() >= 1 && (arg7.charAt(0) == '-' || arg7.charAt(0) == '+' || arg7.charAt(0) == '=')) {
                            char op = arg7.charAt(0);
                            String pkg = arg7.substring(1);
                            if (op == '+') {
                                if (addPowerSaveWhitelistAppInternal(pkg)) {
                                    pw.println("Added: " + pkg);
                                } else {
                                    pw.println("Unknown package: " + pkg);
                                }
                            } else if (op != '-') {
                                pw.println(getPowerSaveWhitelistAppInternal(pkg));
                            } else if (removePowerSaveWhitelistAppInternal(pkg)) {
                                pw.println("Removed: " + pkg);
                            }
                            arg7 = shell.getNextArg();
                            if (arg7 == null) {
                            }
                        }
                        pw.println("Package must be prefixed with +, -, or =: " + arg7);
                        return -1;
                    }
                    synchronized (this) {
                        for (int j = 0; j < this.mPowerSaveWhitelistAppsExceptIdle.size(); j++) {
                            pw.print("system-excidle,");
                            pw.print(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(j));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistAppsExceptIdle.valueAt(j));
                        }
                        for (int j2 = 0; j2 < this.mPowerSaveWhitelistApps.size(); j2++) {
                            pw.print("system,");
                            pw.print(this.mPowerSaveWhitelistApps.keyAt(j2));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistApps.valueAt(j2));
                        }
                        for (int j3 = 0; j3 < this.mPowerSaveWhitelistUserApps.size(); j3++) {
                            pw.print("user,");
                            pw.print(this.mPowerSaveWhitelistUserApps.keyAt(j3));
                            pw.print(",");
                            pw.println(this.mPowerSaveWhitelistUserApps.valueAt(j3));
                        }
                    }
                    return 0;
                } finally {
                }
            }
            if (!"tempwhitelist".equals(cmd)) {
                return shell.handleDefaultCommands(cmd);
            }
            while (true) {
                String opt = shell.getNextOption();
                if (opt == null) {
                    String arg8 = shell.getNextArg();
                    if (arg8 == null) {
                        dumpTempWhitelistSchedule(pw, false);
                        return 0;
                    }
                    try {
                        addPowerSaveTempWhitelistAppChecked(arg8, JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY, shell.userId, "shell");
                        return 0;
                    } catch (RemoteException re) {
                        pw.println("Failed: " + re);
                        return 0;
                    }
                }
                if ("-u".equals(opt)) {
                    String opt2 = shell.getNextArg();
                    if (opt2 == null) {
                        pw.println("-u requires a user number");
                        return -1;
                    }
                    shell.userId = Integer.parseInt(opt2);
                }
            }
        }
        return 0;
    }

    void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        String label;
        if (getContext().checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump DeviceIdleController from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        if (args != null) {
            int userId = 0;
            int i = 0;
            while (i < args.length) {
                String arg = args[i];
                if ("-h".equals(arg)) {
                    dumpHelp(pw);
                    return;
                }
                if ("-u".equals(arg)) {
                    i++;
                    if (i < args.length) {
                        userId = Integer.parseInt(args[i]);
                    }
                } else if (!"-a".equals(arg)) {
                    if (arg.length() > 0 && arg.charAt(0) == '-') {
                        pw.println("Unknown option: " + arg);
                        return;
                    }
                    Shell shell = new Shell();
                    shell.userId = userId;
                    String[] newArgs = new String[args.length - i];
                    System.arraycopy(args, i, newArgs, 0, args.length - i);
                    shell.exec(this.mBinderService, null, fd, null, newArgs, new ResultReceiver(null));
                    return;
                }
                i++;
            }
        }
        synchronized (this) {
            this.mConstants.dump(pw);
            if (this.mEventCmds[0] != 0) {
                pw.println("  Idling history:");
                long now = SystemClock.elapsedRealtime();
                for (int i2 = 99; i2 >= 0; i2--) {
                    int cmd = this.mEventCmds[i2];
                    if (cmd != 0) {
                        switch (this.mEventCmds[i2]) {
                            case 1:
                                label = "     normal";
                                break;
                            case 2:
                                label = " light-idle";
                                break;
                            case 3:
                                label = "light-maint";
                                break;
                            case 4:
                                label = "  deep-idle";
                                break;
                            case 5:
                                label = " deep-maint";
                                break;
                            default:
                                label = "         ??";
                                break;
                        }
                        pw.print("    ");
                        pw.print(label);
                        pw.print(": ");
                        TimeUtils.formatDuration(this.mEventTimes[i2], now, pw);
                        pw.println();
                    }
                }
            }
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
            if (size > 0) {
                pw.println("  Whitelist (except idle) system apps:");
                for (int i3 = 0; i3 < size; i3++) {
                    pw.print("    ");
                    pw.println(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i3));
                }
            }
            int size2 = this.mPowerSaveWhitelistApps.size();
            if (size2 > 0) {
                pw.println("  Whitelist system apps:");
                for (int i4 = 0; i4 < size2; i4++) {
                    pw.print("    ");
                    pw.println(this.mPowerSaveWhitelistApps.keyAt(i4));
                }
            }
            int size3 = this.mPowerSaveWhitelistUserApps.size();
            if (size3 > 0) {
                pw.println("  Whitelist user apps:");
                for (int i5 = 0; i5 < size3; i5++) {
                    pw.print("    ");
                    pw.println(this.mPowerSaveWhitelistUserApps.keyAt(i5));
                }
            }
            int size4 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
            if (size4 > 0) {
                pw.println("  Whitelist (except idle) all app ids:");
                for (int i6 = 0; i6 < size4; i6++) {
                    pw.print("    ");
                    pw.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i6));
                    pw.println();
                }
            }
            int size5 = this.mPowerSaveWhitelistUserAppIds.size();
            if (size5 > 0) {
                pw.println("  Whitelist user app ids:");
                for (int i7 = 0; i7 < size5; i7++) {
                    pw.print("    ");
                    pw.print(this.mPowerSaveWhitelistUserAppIds.keyAt(i7));
                    pw.println();
                }
            }
            int size6 = this.mPowerSaveWhitelistAllAppIds.size();
            if (size6 > 0) {
                pw.println("  Whitelist all app ids:");
                for (int i8 = 0; i8 < size6; i8++) {
                    pw.print("    ");
                    pw.print(this.mPowerSaveWhitelistAllAppIds.keyAt(i8));
                    pw.println();
                }
            }
            dumpTempWhitelistSchedule(pw, true);
            int size7 = this.mTempWhitelistAppIdArray != null ? this.mTempWhitelistAppIdArray.length : 0;
            if (size7 > 0) {
                pw.println("  Temp whitelist app ids:");
                for (int i9 = 0; i9 < size7; i9++) {
                    pw.print("    ");
                    pw.print(this.mTempWhitelistAppIdArray[i9]);
                    pw.println();
                }
            }
            pw.print("  mLightEnabled=");
            pw.print(this.mLightEnabled);
            pw.print("  mDeepEnabled=");
            pw.println(this.mDeepEnabled);
            pw.print("  mForceIdle=");
            pw.println(this.mForceIdle);
            pw.print("  mMotionSensor=");
            pw.println(this.mMotionSensor);
            pw.print("  mCurDisplay=");
            pw.println(this.mCurDisplay);
            pw.print("  mScreenOn=");
            pw.println(this.mScreenOn);
            pw.print("  mNetworkConnected=");
            pw.println(this.mNetworkConnected);
            pw.print("  mCharging=");
            pw.println(this.mCharging);
            pw.print("  mMotionActive=");
            pw.println(this.mMotionListener.active);
            pw.print("  mNotMoving=");
            pw.println(this.mNotMoving);
            pw.print("  mLocating=");
            pw.print(this.mLocating);
            pw.print(" mHasGps=");
            pw.print(this.mHasGps);
            pw.print(" mHasNetwork=");
            pw.print(this.mHasNetworkLocation);
            pw.print(" mLocated=");
            pw.println(this.mLocated);
            if (this.mLastGenericLocation != null) {
                pw.print("  mLastGenericLocation=");
                pw.println(this.mLastGenericLocation);
            }
            if (this.mLastGpsLocation != null) {
                pw.print("  mLastGpsLocation=");
                pw.println(this.mLastGpsLocation);
            }
            pw.print("  mState=");
            pw.print(stateToString(this.mState));
            pw.print(" mLightState=");
            pw.println(lightStateToString(this.mLightState));
            pw.print("  mInactiveTimeout=");
            TimeUtils.formatDuration(this.mInactiveTimeout, pw);
            pw.println();
            if (this.mActiveIdleOpCount != 0) {
                pw.print("  mActiveIdleOpCount=");
                pw.println(this.mActiveIdleOpCount);
            }
            if (this.mNextAlarmTime != 0) {
                pw.print("  mNextAlarmTime=");
                TimeUtils.formatDuration(this.mNextAlarmTime, SystemClock.elapsedRealtime(), pw);
                pw.println();
            }
            if (this.mNextIdlePendingDelay != 0) {
                pw.print("  mNextIdlePendingDelay=");
                TimeUtils.formatDuration(this.mNextIdlePendingDelay, pw);
                pw.println();
            }
            if (this.mNextIdleDelay != 0) {
                pw.print("  mNextIdleDelay=");
                TimeUtils.formatDuration(this.mNextIdleDelay, pw);
                pw.println();
            }
            if (this.mNextLightIdleDelay != 0) {
                pw.print("  mNextIdleDelay=");
                TimeUtils.formatDuration(this.mNextLightIdleDelay, pw);
                pw.println();
            }
            if (this.mNextLightAlarmTime != 0) {
                pw.print("  mNextLightAlarmTime=");
                TimeUtils.formatDuration(this.mNextLightAlarmTime, SystemClock.elapsedRealtime(), pw);
                pw.println();
            }
            if (this.mCurIdleBudget != 0) {
                pw.print("  mCurIdleBudget=");
                TimeUtils.formatDuration(this.mCurIdleBudget, pw);
                pw.println();
            }
            if (this.mMaintenanceStartTime != 0) {
                pw.print("  mMaintenanceStartTime=");
                TimeUtils.formatDuration(this.mMaintenanceStartTime, SystemClock.elapsedRealtime(), pw);
                pw.println();
            }
            if (this.mJobsActive) {
                pw.print("  mJobsActive=");
                pw.println(this.mJobsActive);
            }
            if (this.mAlarmsActive) {
                pw.print("  mAlarmsActive=");
                pw.println(this.mAlarmsActive);
            }
        }
    }

    void dumpTempWhitelistSchedule(PrintWriter pw, boolean printTitle) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (size <= 0) {
            return;
        }
        String prefix = "";
        if (printTitle) {
            pw.println("  Temp whitelist schedule:");
            prefix = "    ";
        }
        long timeNow = SystemClock.elapsedRealtime();
        for (int i = 0; i < size; i++) {
            pw.print(prefix);
            pw.print("UID=");
            pw.print(this.mTempWhitelistAppIdEndTimes.keyAt(i));
            pw.print(": ");
            Pair<MutableLong, String> entry = this.mTempWhitelistAppIdEndTimes.valueAt(i);
            TimeUtils.formatDuration(((MutableLong) entry.first).value, timeNow, pw);
            pw.print(" - ");
            pw.println((String) entry.second);
        }
    }

    private IDataShapingManager getDataShapingService() {
        if (this.mDataShapingManager == null) {
            IDataShapingManager service = ServiceManager.getService("data_shaping");
            this.mDataShapingManager = service;
            return service;
        }
        return this.mDataShapingManager;
    }
}
