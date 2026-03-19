package com.android.server;

import android.R;
import android.app.ActivityManager;
import android.app.ActivityManagerInternal;
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
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
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
import com.android.internal.annotations.GuardedBy;
import com.android.internal.app.IBatteryStats;
import com.android.internal.os.AtomicFile;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.AnyMotionDetector;
import com.android.server.UiModeManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.backup.BackupAgentTimeoutParameters;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.net.NetworkPolicyManagerInternal;
import com.android.server.pm.DumpState;
import com.android.server.usage.AppStandbyController;
import com.mediatek.server.MtkDataShaping;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class DeviceIdleController extends SystemService implements AnyMotionDetector.DeviceIdleCallback {
    private static final boolean COMPRESS_TIME = false;
    private static final boolean DEBUG = false;
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
    private static final int MSG_FINISH_IDLE_OP = 8;
    private static final int MSG_REPORT_ACTIVE = 5;
    private static final int MSG_REPORT_IDLE_OFF = 4;
    private static final int MSG_REPORT_IDLE_ON = 2;
    private static final int MSG_REPORT_IDLE_ON_LIGHT = 3;
    private static final int MSG_REPORT_MAINTENANCE_ACTIVITY = 7;
    private static final int MSG_REPORT_TEMP_APP_WHITELIST_CHANGED = 9;
    private static final int MSG_TEMP_APP_WHITELIST_TIMEOUT = 6;
    private static final int MSG_WRITE_CONFIG = 1;
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
    private final AppStateTracker mAppStateTracker;
    private IBatteryStats mBatteryStats;
    BinderService mBinderService;
    private boolean mCharging;
    public final AtomicFile mConfigFile;
    private ConnectivityService mConnectivityService;
    private Constants mConstants;
    private long mCurIdleBudget;
    private final AlarmManager.OnAlarmListener mDeepAlarmListener;
    private boolean mDeepEnabled;
    private final int[] mEventCmds;
    private final String[] mEventReasons;
    private final long[] mEventTimes;
    private boolean mForceIdle;
    private final LocationListener mGenericLocationListener;
    private PowerManager.WakeLock mGoingIdleWakeLock;
    private final LocationListener mGpsLocationListener;
    final MyHandler mHandler;
    private boolean mHasGps;
    private boolean mHasNetworkLocation;
    private Intent mIdleIntent;
    private final BroadcastReceiver mIdleStartedDoneReceiver;
    private long mInactiveTimeout;
    private final BroadcastReceiver mInteractivityReceiver;
    private boolean mJobsActive;
    private Location mLastGenericLocation;
    private Location mLastGpsLocation;
    private final AlarmManager.OnAlarmListener mLightAlarmListener;
    private boolean mLightEnabled;
    private Intent mLightIdleIntent;
    private int mLightState;
    private ActivityManagerInternal mLocalActivityManager;
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
    private NetworkPolicyManagerInternal mNetworkPolicyManagerInternal;
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
    private final ArraySet<String> mPowerSaveWhitelistUserAppsExceptIdle;
    private final BroadcastReceiver mReceiver;
    private ArrayMap<String, Integer> mRemovedFromSystemWhitelistApps;
    private boolean mReportedMaintenanceActivity;
    private boolean mScreenLocked;
    private ActivityManagerInternal.ScreenObserver mScreenObserver;
    private boolean mScreenOn;
    private final AlarmManager.OnAlarmListener mSensingTimeoutAlarmListener;
    private SensorManager mSensorManager;
    private int mState;
    private int[] mTempWhitelistAppIdArray;
    private final SparseArray<Pair<MutableLong, String>> mTempWhitelistAppIdEndTimes;

    private static String stateToString(int i) {
        switch (i) {
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
                return Integer.toString(i);
        }
    }

    private static String lightStateToString(int i) {
        switch (i) {
            case 0:
                return "ACTIVE";
            case 1:
                return "INACTIVE";
            case 2:
            default:
                return Integer.toString(i);
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

    private void addEvent(int i, String str) {
        if (this.mEventCmds[0] != i) {
            System.arraycopy(this.mEventCmds, 0, this.mEventCmds, 1, 99);
            System.arraycopy(this.mEventTimes, 0, this.mEventTimes, 1, 99);
            System.arraycopy(this.mEventReasons, 0, this.mEventReasons, 1, 99);
            this.mEventCmds[0] = i;
            this.mEventTimes[0] = SystemClock.elapsedRealtime();
            this.mEventReasons[0] = str;
        }
    }

    private final class MotionListener extends TriggerEventListener implements SensorEventListener {
        boolean active;

        private MotionListener() {
            this.active = false;
        }

        @Override
        public void onTrigger(TriggerEvent triggerEvent) {
            synchronized (DeviceIdleController.this) {
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            synchronized (DeviceIdleController.this) {
                DeviceIdleController.this.mSensorManager.unregisterListener(this, DeviceIdleController.this.mMotionSensor);
                this.active = false;
                DeviceIdleController.this.motionLocked();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }

        public boolean registerLocked() {
            boolean zRegisterListener;
            if (DeviceIdleController.this.mMotionSensor.getReportingMode() == 2) {
                zRegisterListener = DeviceIdleController.this.mSensorManager.requestTriggerSensor(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor);
            } else {
                zRegisterListener = DeviceIdleController.this.mSensorManager.registerListener(DeviceIdleController.this.mMotionListener, DeviceIdleController.this.mMotionSensor, 3);
            }
            if (!zRegisterListener) {
                Slog.e(DeviceIdleController.TAG, "Unable to register for " + DeviceIdleController.this.mMotionSensor);
            } else {
                this.active = true;
            }
            return zRegisterListener;
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
        private static final String KEY_WAIT_FOR_UNLOCK = "wait_for_unlock";
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
        public boolean WAIT_FOR_UNLOCK;
        private final KeyValueListParser mParser;
        private final ContentResolver mResolver;
        private final boolean mSmallBatteryDevice;

        public Constants(Handler handler, ContentResolver contentResolver) {
            super(handler);
            this.mParser = new KeyValueListParser(',');
            this.mResolver = contentResolver;
            this.mSmallBatteryDevice = ActivityManager.isSmallBatteryDevice();
            this.mResolver.registerContentObserver(Settings.Global.getUriFor("device_idle_constants"), false, this);
            updateConstants();
        }

        @Override
        public void onChange(boolean z, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (DeviceIdleController.this) {
                try {
                    this.mParser.setString(Settings.Global.getString(this.mResolver, "device_idle_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(DeviceIdleController.TAG, "Bad device idle settings", e);
                }
                this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, 180000L);
                this.LIGHT_PRE_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_PRE_IDLE_TIMEOUT, 180000L);
                this.LIGHT_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_TIMEOUT, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.LIGHT_IDLE_FACTOR = this.mParser.getFloat(KEY_LIGHT_IDLE_FACTOR, 2.0f);
                this.LIGHT_MAX_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_LIGHT_MAX_IDLE_TIMEOUT, 900000L);
                this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, 60000L);
                this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET = this.mParser.getDurationMillis(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MIN_LIGHT_MAINTENANCE_TIME = this.mParser.getDurationMillis(KEY_MIN_LIGHT_MAINTENANCE_TIME, 5000L);
                this.MIN_DEEP_MAINTENANCE_TIME = this.mParser.getDurationMillis(KEY_MIN_DEEP_MAINTENANCE_TIME, 30000L);
                this.INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_INACTIVE_TIMEOUT, ((long) ((this.mSmallBatteryDevice ? 15 : 30) * 60)) * 1000);
                this.SENSING_TIMEOUT = this.mParser.getDurationMillis(KEY_SENSING_TIMEOUT, 240000L);
                this.LOCATING_TIMEOUT = this.mParser.getDurationMillis(KEY_LOCATING_TIMEOUT, 30000L);
                this.LOCATION_ACCURACY = this.mParser.getFloat(KEY_LOCATION_ACCURACY, 20.0f);
                this.MOTION_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_MOTION_INACTIVE_TIMEOUT, 600000L);
                this.IDLE_AFTER_INACTIVE_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_AFTER_INACTIVE_TIMEOUT, ((long) ((this.mSmallBatteryDevice ? 15 : 30) * 60)) * 1000);
                this.IDLE_PENDING_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_PENDING_TIMEOUT, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MAX_IDLE_PENDING_TIMEOUT = this.mParser.getDurationMillis(KEY_MAX_IDLE_PENDING_TIMEOUT, 600000L);
                this.IDLE_PENDING_FACTOR = this.mParser.getFloat(KEY_IDLE_PENDING_FACTOR, 2.0f);
                this.IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_IDLE_TIMEOUT, AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                this.MAX_IDLE_TIMEOUT = this.mParser.getDurationMillis(KEY_MAX_IDLE_TIMEOUT, 21600000L);
                this.IDLE_FACTOR = this.mParser.getFloat(KEY_IDLE_FACTOR, 2.0f);
                this.MIN_TIME_TO_ALARM = this.mParser.getDurationMillis(KEY_MIN_TIME_TO_ALARM, AppStandbyController.SettingsObserver.DEFAULT_STRONG_USAGE_TIMEOUT);
                this.MAX_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_MAX_TEMP_APP_WHITELIST_DURATION, BackupAgentTimeoutParameters.DEFAULT_FULL_BACKUP_AGENT_TIMEOUT_MILLIS);
                this.MMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_MMS_TEMP_APP_WHITELIST_DURATION, 60000L);
                this.SMS_TEMP_APP_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_SMS_TEMP_APP_WHITELIST_DURATION, 20000L);
                this.NOTIFICATION_WHITELIST_DURATION = this.mParser.getDurationMillis(KEY_NOTIFICATION_WHITELIST_DURATION, 30000L);
                this.WAIT_FOR_UNLOCK = this.mParser.getBoolean(KEY_WAIT_FOR_UNLOCK, false);
            }
        }

        void dump(PrintWriter printWriter) {
            printWriter.println("  Settings:");
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_PRE_IDLE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_PRE_IDLE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_IDLE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_IDLE_FACTOR);
            printWriter.print("=");
            printWriter.print(this.LIGHT_IDLE_FACTOR);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_MAX_IDLE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_MAX_IDLE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_IDLE_MAINTENANCE_MIN_BUDGET);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LIGHT_IDLE_MAINTENANCE_MAX_BUDGET);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LIGHT_IDLE_MAINTENANCE_MAX_BUDGET, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MIN_LIGHT_MAINTENANCE_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MIN_LIGHT_MAINTENANCE_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MIN_DEEP_MAINTENANCE_TIME);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MIN_DEEP_MAINTENANCE_TIME, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_INACTIVE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.INACTIVE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_SENSING_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.SENSING_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LOCATING_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.LOCATING_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_LOCATION_ACCURACY);
            printWriter.print("=");
            printWriter.print(this.LOCATION_ACCURACY);
            printWriter.print("m");
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MOTION_INACTIVE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MOTION_INACTIVE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_IDLE_AFTER_INACTIVE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.IDLE_AFTER_INACTIVE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_IDLE_PENDING_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.IDLE_PENDING_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MAX_IDLE_PENDING_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_PENDING_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_IDLE_PENDING_FACTOR);
            printWriter.print("=");
            printWriter.println(this.IDLE_PENDING_FACTOR);
            printWriter.print("    ");
            printWriter.print(KEY_IDLE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.IDLE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MAX_IDLE_TIMEOUT);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MAX_IDLE_TIMEOUT, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_IDLE_FACTOR);
            printWriter.print("=");
            printWriter.println(this.IDLE_FACTOR);
            printWriter.print("    ");
            printWriter.print(KEY_MIN_TIME_TO_ALARM);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MIN_TIME_TO_ALARM, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MAX_TEMP_APP_WHITELIST_DURATION);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MAX_TEMP_APP_WHITELIST_DURATION, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_MMS_TEMP_APP_WHITELIST_DURATION);
            printWriter.print("=");
            TimeUtils.formatDuration(this.MMS_TEMP_APP_WHITELIST_DURATION, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_SMS_TEMP_APP_WHITELIST_DURATION);
            printWriter.print("=");
            TimeUtils.formatDuration(this.SMS_TEMP_APP_WHITELIST_DURATION, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_NOTIFICATION_WHITELIST_DURATION);
            printWriter.print("=");
            TimeUtils.formatDuration(this.NOTIFICATION_WHITELIST_DURATION, printWriter);
            printWriter.println();
            printWriter.print("    ");
            printWriter.print(KEY_WAIT_FOR_UNLOCK);
            printWriter.print("=");
            printWriter.println(this.WAIT_FOR_UNLOCK);
        }
    }

    @Override
    public void onAnyMotionResult(int i) {
        if (i != -1) {
            synchronized (this) {
                cancelSensingTimeoutAlarmLocked();
            }
        }
        if (i == 1 || i == -1) {
            synchronized (this) {
                handleMotionDetectedLocked(this.mConstants.INACTIVE_TIMEOUT, "non_stationary");
            }
            return;
        }
        if (i == 0) {
            if (this.mState == 3) {
                synchronized (this) {
                    this.mNotMoving = true;
                    stepIdleStateLocked("s:stationary");
                }
            } else if (this.mState == 4) {
                synchronized (this) {
                    this.mNotMoving = true;
                    if (this.mLocated) {
                        stepIdleStateLocked("s:stationary");
                    }
                }
            }
        }
    }

    final class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            boolean deviceIdleMode;
            boolean lightDeviceIdleMode;
            String str;
            switch (message.what) {
                case 1:
                    DeviceIdleController.this.handleWriteConfigFile();
                    return;
                case 2:
                case 3:
                    EventLogTags.writeDeviceIdleOnStart();
                    if (message.what == 2) {
                        deviceIdleMode = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(true);
                        lightDeviceIdleMode = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    } else {
                        deviceIdleMode = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                        lightDeviceIdleMode = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(true);
                    }
                    try {
                        MtkDataShaping.setDeviceIdleMode(true);
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(true);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(message.what == 2 ? 2 : 1, (String) null, Process.myUid());
                        break;
                    } catch (RemoteException e) {
                    }
                    if (deviceIdleMode) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightDeviceIdleMode) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOnComplete();
                    DeviceIdleController.this.mGoingIdleWakeLock.release();
                    return;
                case 4:
                    EventLogTags.writeDeviceIdleOffStart(UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN);
                    boolean deviceIdleMode2 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightDeviceIdleMode2 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, (String) null, Process.myUid());
                        break;
                    } catch (RemoteException e2) {
                    }
                    if (deviceIdleMode2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    if (lightDeviceIdleMode2) {
                        DeviceIdleController.this.incActiveIdleOps();
                        DeviceIdleController.this.getContext().sendOrderedBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL, null, DeviceIdleController.this.mIdleStartedDoneReceiver, null, 0, null, null);
                    }
                    DeviceIdleController.this.decActiveIdleOps();
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 5:
                    String str2 = (String) message.obj;
                    int i = message.arg1;
                    if (str2 != null) {
                        str = str2;
                    } else {
                        str = UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN;
                    }
                    EventLogTags.writeDeviceIdleOffStart(str);
                    boolean deviceIdleMode3 = DeviceIdleController.this.mLocalPowerManager.setDeviceIdleMode(false);
                    boolean lightDeviceIdleMode3 = DeviceIdleController.this.mLocalPowerManager.setLightDeviceIdleMode(false);
                    try {
                        MtkDataShaping.setDeviceIdleMode(false);
                        DeviceIdleController.this.mNetworkPolicyManager.setDeviceIdleMode(false);
                        DeviceIdleController.this.mBatteryStats.noteDeviceIdleMode(0, str2, i);
                        break;
                    } catch (RemoteException e3) {
                    }
                    if (deviceIdleMode3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mIdleIntent, UserHandle.ALL);
                    }
                    if (lightDeviceIdleMode3) {
                        DeviceIdleController.this.getContext().sendBroadcastAsUser(DeviceIdleController.this.mLightIdleIntent, UserHandle.ALL);
                    }
                    EventLogTags.writeDeviceIdleOffComplete();
                    return;
                case 6:
                    DeviceIdleController.this.checkTempAppWhitelistTimeout(message.arg1);
                    return;
                case 7:
                    boolean z = message.arg1 == 1;
                    int iBeginBroadcast = DeviceIdleController.this.mMaintenanceActivityListeners.beginBroadcast();
                    for (int i2 = 0; i2 < iBeginBroadcast; i2++) {
                        try {
                            DeviceIdleController.this.mMaintenanceActivityListeners.getBroadcastItem(i2).onMaintenanceActivityChanged(z);
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
                case 9:
                    DeviceIdleController.this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(message.arg1, message.arg2 == 1);
                    return;
                default:
                    return;
            }
        }
    }

    private final class BinderService extends IDeviceIdleController.Stub {
        private BinderService() {
        }

        public void addPowerSaveWhitelistApp(String str) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.addPowerSaveWhitelistAppInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removePowerSaveWhitelistApp(String str) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removePowerSaveWhitelistAppInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void removeSystemPowerWhitelistApp(String str) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.removeSystemPowerWhitelistAppInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public void restoreSystemPowerWhitelistApp(String str) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.restoreSystemPowerWhitelistAppInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public String[] getRemovedSystemPowerWhitelistApps() {
            return DeviceIdleController.this.getRemovedSystemPowerWhitelistAppsInternal();
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

        public boolean isPowerSaveWhitelistExceptIdleApp(String str) {
            return DeviceIdleController.this.isPowerSaveWhitelistExceptIdleAppInternal(str);
        }

        public boolean isPowerSaveWhitelistApp(String str) {
            return DeviceIdleController.this.isPowerSaveWhitelistAppInternal(str);
        }

        public void addPowerSaveTempWhitelistApp(String str, long j, int i, String str2) throws RemoteException {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(str, j, i, str2);
        }

        public long addPowerSaveTempWhitelistAppForMms(String str, int i, String str2) throws RemoteException {
            long j = DeviceIdleController.this.mConstants.MMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(str, j, i, str2);
            return j;
        }

        public long addPowerSaveTempWhitelistAppForSms(String str, int i, String str2) throws RemoteException {
            long j = DeviceIdleController.this.mConstants.SMS_TEMP_APP_WHITELIST_DURATION;
            DeviceIdleController.this.addPowerSaveTempWhitelistAppChecked(str, j, i, str2);
            return j;
        }

        public void exitIdle(String str) {
            DeviceIdleController.this.getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                DeviceIdleController.this.exitIdleInternal(str);
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        public boolean registerMaintenanceActivityListener(IMaintenanceActivityListener iMaintenanceActivityListener) {
            return DeviceIdleController.this.registerMaintenanceActivityListener(iMaintenanceActivityListener);
        }

        public void unregisterMaintenanceActivityListener(IMaintenanceActivityListener iMaintenanceActivityListener) {
            DeviceIdleController.this.unregisterMaintenanceActivityListener(iMaintenanceActivityListener);
        }

        protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
            DeviceIdleController.this.dump(fileDescriptor, printWriter, strArr);
        }

        public void onShellCommand(FileDescriptor fileDescriptor, FileDescriptor fileDescriptor2, FileDescriptor fileDescriptor3, String[] strArr, ShellCallback shellCallback, ResultReceiver resultReceiver) {
            DeviceIdleController.this.new Shell().exec(this, fileDescriptor, fileDescriptor2, fileDescriptor3, strArr, shellCallback, resultReceiver);
        }
    }

    public class LocalService {
        public LocalService() {
        }

        public void addPowerSaveTempWhitelistApp(int i, String str, long j, int i2, boolean z, String str2) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppInternal(i, str, j, i2, z, str2);
        }

        public void addPowerSaveTempWhitelistAppDirect(int i, long j, boolean z, String str) {
            DeviceIdleController.this.addPowerSaveTempWhitelistAppDirectInternal(0, i, j, z, str);
        }

        public long getNotificationWhitelistDuration() {
            return DeviceIdleController.this.mConstants.NOTIFICATION_WHITELIST_DURATION;
        }

        public void setJobsActive(boolean z) {
            DeviceIdleController.this.setJobsActive(z);
        }

        public void setAlarmsActive(boolean z) {
            DeviceIdleController.this.setAlarmsActive(z);
        }

        public boolean isAppOnWhitelist(int i) {
            return DeviceIdleController.this.isAppOnWhitelistInternal(i);
        }

        public int[] getPowerSaveWhitelistUserAppIds() {
            return DeviceIdleController.this.getPowerSaveWhitelistUserAppIds();
        }

        public int[] getPowerSaveTempWhitelistAppIds() {
            return DeviceIdleController.this.getAppIdTempWhitelistInternal();
        }
    }

    public DeviceIdleController(Context context) {
        super(context);
        this.mMaintenanceActivityListeners = new RemoteCallbackList<>();
        this.mPowerSaveWhitelistAppsExceptIdle = new ArrayMap<>();
        this.mPowerSaveWhitelistUserAppsExceptIdle = new ArraySet<>();
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
        this.mRemovedFromSystemWhitelistApps = new ArrayMap<>();
        this.mEventCmds = new int[100];
        this.mEventTimes = new long[100];
        this.mEventReasons = new String[100];
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                byte b;
                Uri data;
                String schemeSpecificPart;
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                boolean z = true;
                if (iHashCode != -1538406691) {
                    if (iHashCode != -1172645946) {
                        b = (iHashCode == 525384130 && action.equals("android.intent.action.PACKAGE_REMOVED")) ? (byte) 2 : (byte) -1;
                    } else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                        b = 0;
                    }
                } else if (action.equals("android.intent.action.BATTERY_CHANGED")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        DeviceIdleController.this.updateConnectivityState(intent);
                        return;
                    case 1:
                        synchronized (DeviceIdleController.this) {
                            int intExtra = intent.getIntExtra("plugged", 0);
                            DeviceIdleController deviceIdleController = DeviceIdleController.this;
                            if (intExtra == 0) {
                                z = false;
                            }
                            deviceIdleController.updateChargingLocked(z);
                            break;
                        }
                        return;
                    case 2:
                        if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (data = intent.getData()) != null && (schemeSpecificPart = data.getSchemeSpecificPart()) != null) {
                            DeviceIdleController.this.removePowerSaveWhitelistAppInternal(schemeSpecificPart);
                            return;
                        }
                        return;
                    default:
                        return;
                }
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
                if (DeviceIdleController.this.mState == 3) {
                    synchronized (DeviceIdleController.this) {
                        DeviceIdleController.this.becomeInactiveIfAppropriateLocked();
                    }
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
        this.mInteractivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.updateInteractivityLocked();
                }
            }
        };
        this.mMotionListener = new MotionListener();
        this.mGenericLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.receivedGenericLocationLocked(location);
                }
            }

            @Override
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onProviderDisabled(String str) {
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
            public void onStatusChanged(String str, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String str) {
            }

            @Override
            public void onProviderDisabled(String str) {
            }
        };
        this.mScreenObserver = new ActivityManagerInternal.ScreenObserver() {
            public void onAwakeStateChanged(boolean z) {
            }

            public void onKeyguardStateChanged(boolean z) {
                synchronized (DeviceIdleController.this) {
                    DeviceIdleController.this.keyguardShowingLocked(z);
                }
            }
        };
        this.mConfigFile = new AtomicFile(new File(getSystemDir(), "deviceidle.xml"));
        this.mHandler = new MyHandler(BackgroundThread.getHandler().getLooper());
        this.mAppStateTracker = new AppStateTracker(context, FgThread.get().getLooper());
        LocalServices.addService(AppStateTracker.class, this.mAppStateTracker);
    }

    boolean isAppOnWhitelistInternal(int i) {
        boolean z;
        synchronized (this) {
            z = Arrays.binarySearch(this.mPowerSaveWhitelistAllAppIdArray, i) >= 0;
        }
        return z;
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
        PackageManager packageManager = getContext().getPackageManager();
        synchronized (this) {
            boolean z = getContext().getResources().getBoolean(R.^attr-private.floatingToolbarItemBackgroundDrawable);
            this.mDeepEnabled = z;
            this.mLightEnabled = z;
            SystemConfig systemConfig = SystemConfig.getInstance();
            ArraySet allowInPowerSaveExceptIdle = systemConfig.getAllowInPowerSaveExceptIdle();
            for (int i = 0; i < allowInPowerSaveExceptIdle.size(); i++) {
                try {
                    ApplicationInfo applicationInfo = packageManager.getApplicationInfo((String) allowInPowerSaveExceptIdle.valueAt(i), DumpState.DUMP_DEXOPT);
                    int appId = UserHandle.getAppId(applicationInfo.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(applicationInfo.packageName, Integer.valueOf(appId));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appId, true);
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            ArraySet allowInPowerSave = systemConfig.getAllowInPowerSave();
            for (int i2 = 0; i2 < allowInPowerSave.size(); i2++) {
                try {
                    ApplicationInfo applicationInfo2 = packageManager.getApplicationInfo((String) allowInPowerSave.valueAt(i2), DumpState.DUMP_DEXOPT);
                    int appId2 = UserHandle.getAppId(applicationInfo2.uid);
                    this.mPowerSaveWhitelistAppsExceptIdle.put(applicationInfo2.packageName, Integer.valueOf(appId2));
                    this.mPowerSaveWhitelistSystemAppIdsExceptIdle.put(appId2, true);
                    this.mPowerSaveWhitelistApps.put(applicationInfo2.packageName, Integer.valueOf(appId2));
                    this.mPowerSaveWhitelistSystemAppIds.put(appId2, true);
                } catch (PackageManager.NameNotFoundException e2) {
                }
            }
            this.mConstants = new Constants(this.mHandler, getContext().getContentResolver());
            readConfigFileLocked();
            updateWhitelistAppIdsLocked();
            this.mNetworkConnected = true;
            this.mScreenOn = true;
            this.mScreenLocked = false;
            this.mCharging = true;
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
        }
        this.mBinderService = new BinderService();
        publishBinderService("deviceidle", this.mBinderService);
        publishLocalService(LocalService.class, new LocalService());
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 500) {
            synchronized (this) {
                this.mAlarmManager = (AlarmManager) getContext().getSystemService("alarm");
                this.mBatteryStats = BatteryStatsService.getService();
                this.mLocalActivityManager = (ActivityManagerInternal) getLocalService(ActivityManagerInternal.class);
                this.mLocalPowerManager = (PowerManagerInternal) getLocalService(PowerManagerInternal.class);
                this.mPowerManager = (PowerManager) getContext().getSystemService(PowerManager.class);
                this.mActiveIdleWakeLock = this.mPowerManager.newWakeLock(1, "deviceidle_maint");
                this.mActiveIdleWakeLock.setReferenceCounted(false);
                this.mGoingIdleWakeLock = this.mPowerManager.newWakeLock(1, "deviceidle_going_idle");
                this.mGoingIdleWakeLock.setReferenceCounted(true);
                this.mConnectivityService = (ConnectivityService) ServiceManager.getService("connectivity");
                this.mNetworkPolicyManager = INetworkPolicyManager.Stub.asInterface(ServiceManager.getService("netpolicy"));
                this.mNetworkPolicyManagerInternal = (NetworkPolicyManagerInternal) getLocalService(NetworkPolicyManagerInternal.class);
                this.mSensorManager = (SensorManager) getContext().getSystemService("sensor");
                int integer = getContext().getResources().getInteger(R.integer.config_accessibilityColorMode);
                if (integer > 0) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(integer, true);
                }
                if (this.mMotionSensor == null && getContext().getResources().getBoolean(R.^attr-private.borderBottom)) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(26, true);
                }
                if (this.mMotionSensor == null) {
                    this.mMotionSensor = this.mSensorManager.getDefaultSensor(17, true);
                }
                if (getContext().getResources().getBoolean(R.^attr-private.borderLeft)) {
                    this.mLocationManager = (LocationManager) getContext().getSystemService("location");
                    this.mLocationRequest = new LocationRequest().setQuality(100).setInterval(0L).setFastestInterval(0L).setNumUpdates(1);
                }
                this.mAnyMotionDetector = new AnyMotionDetector((PowerManager) getContext().getSystemService("power"), this.mHandler, this.mSensorManager, this, getContext().getResources().getInteger(R.integer.config_accumulatedBatteryUsageStatsSpanSize) / 100.0f);
                this.mAppStateTracker.onSystemServicesReady();
                this.mIdleIntent = new Intent("android.os.action.DEVICE_IDLE_MODE_CHANGED");
                this.mIdleIntent.addFlags(1342177280);
                this.mLightIdleIntent = new Intent("android.os.action.LIGHT_DEVICE_IDLE_MODE_CHANGED");
                this.mLightIdleIntent.addFlags(1342177280);
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
                getContext().registerReceiver(this.mReceiver, intentFilter);
                IntentFilter intentFilter2 = new IntentFilter();
                intentFilter2.addAction("android.intent.action.PACKAGE_REMOVED");
                intentFilter2.addDataScheme(com.android.server.pm.Settings.ATTR_PACKAGE);
                getContext().registerReceiver(this.mReceiver, intentFilter2);
                IntentFilter intentFilter3 = new IntentFilter();
                intentFilter3.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                getContext().registerReceiver(this.mReceiver, intentFilter3);
                IntentFilter intentFilter4 = new IntentFilter();
                intentFilter4.addAction("android.intent.action.SCREEN_OFF");
                intentFilter4.addAction("android.intent.action.SCREEN_ON");
                getContext().registerReceiver(this.mInteractivityReceiver, intentFilter4);
                this.mLocalActivityManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
                this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
                this.mLocalActivityManager.registerScreenObserver(this.mScreenObserver);
                passWhiteListsToForceAppStandbyTrackerLocked();
                updateInteractivityLocked();
            }
            updateConnectivityState(null);
        }
    }

    public boolean addPowerSaveWhitelistAppInternal(String str) {
        synchronized (this) {
            try {
                try {
                    if (this.mPowerSaveWhitelistUserApps.put(str, Integer.valueOf(UserHandle.getAppId(getContext().getPackageManager().getApplicationInfo(str, DumpState.DUMP_CHANGES).uid))) == null) {
                        reportPowerSaveWhitelistChangedLocked();
                        updateWhitelistAppIdsLocked();
                        writeConfigFileLocked();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return true;
    }

    public boolean removePowerSaveWhitelistAppInternal(String str) {
        synchronized (this) {
            if (this.mPowerSaveWhitelistUserApps.remove(str) != null) {
                reportPowerSaveWhitelistChangedLocked();
                updateWhitelistAppIdsLocked();
                writeConfigFileLocked();
                return true;
            }
            return false;
        }
    }

    public boolean getPowerSaveWhitelistAppInternal(String str) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = this.mPowerSaveWhitelistUserApps.containsKey(str);
        }
        return zContainsKey;
    }

    void resetSystemPowerWhitelistInternal() {
        synchronized (this) {
            this.mPowerSaveWhitelistApps.putAll((ArrayMap<? extends String, ? extends Integer>) this.mRemovedFromSystemWhitelistApps);
            this.mRemovedFromSystemWhitelistApps.clear();
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
        }
    }

    public boolean restoreSystemPowerWhitelistAppInternal(String str) {
        synchronized (this) {
            if (!this.mRemovedFromSystemWhitelistApps.containsKey(str)) {
                return false;
            }
            this.mPowerSaveWhitelistApps.put(str, this.mRemovedFromSystemWhitelistApps.remove(str));
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
            return true;
        }
    }

    public boolean removeSystemPowerWhitelistAppInternal(String str) {
        synchronized (this) {
            if (!this.mPowerSaveWhitelistApps.containsKey(str)) {
                return false;
            }
            this.mRemovedFromSystemWhitelistApps.put(str, this.mPowerSaveWhitelistApps.remove(str));
            reportPowerSaveWhitelistChangedLocked();
            updateWhitelistAppIdsLocked();
            writeConfigFileLocked();
            return true;
        }
    }

    public boolean addPowerSaveWhitelistExceptIdleInternal(String str) {
        synchronized (this) {
            try {
                try {
                    if (this.mPowerSaveWhitelistAppsExceptIdle.put(str, Integer.valueOf(UserHandle.getAppId(getContext().getPackageManager().getApplicationInfo(str, DumpState.DUMP_CHANGES).uid))) == null) {
                        this.mPowerSaveWhitelistUserAppsExceptIdle.add(str);
                        reportPowerSaveWhitelistChangedLocked();
                        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                        passWhiteListsToForceAppStandbyTrackerLocked();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return true;
    }

    public void resetPowerSaveWhitelistExceptIdleInternal() {
        synchronized (this) {
            if (this.mPowerSaveWhitelistAppsExceptIdle.removeAll(this.mPowerSaveWhitelistUserAppsExceptIdle)) {
                reportPowerSaveWhitelistChangedLocked();
                this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
                this.mPowerSaveWhitelistUserAppsExceptIdle.clear();
                passWhiteListsToForceAppStandbyTrackerLocked();
            }
        }
    }

    public boolean getPowerSaveWhitelistExceptIdleInternal(String str) {
        boolean zContainsKey;
        synchronized (this) {
            zContainsKey = this.mPowerSaveWhitelistAppsExceptIdle.containsKey(str);
        }
        return zContainsKey;
    }

    public String[] getSystemPowerWhitelistExceptIdleInternal() {
        String[] strArr;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
            strArr = new String[size];
            for (int i = 0; i < size; i++) {
                strArr[i] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i);
            }
        }
        return strArr;
    }

    public String[] getSystemPowerWhitelistInternal() {
        String[] strArr;
        synchronized (this) {
            int size = this.mPowerSaveWhitelistApps.size();
            strArr = new String[size];
            for (int i = 0; i < size; i++) {
                strArr[i] = this.mPowerSaveWhitelistApps.keyAt(i);
            }
        }
        return strArr;
    }

    public String[] getRemovedSystemPowerWhitelistAppsInternal() {
        String[] strArr;
        synchronized (this) {
            int size = this.mRemovedFromSystemWhitelistApps.size();
            strArr = new String[size];
            for (int i = 0; i < size; i++) {
                strArr[i] = this.mRemovedFromSystemWhitelistApps.keyAt(i);
            }
        }
        return strArr;
    }

    public String[] getUserPowerWhitelistInternal() {
        String[] strArr;
        synchronized (this) {
            strArr = new String[this.mPowerSaveWhitelistUserApps.size()];
            for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
                strArr[i] = this.mPowerSaveWhitelistUserApps.keyAt(i);
            }
        }
        return strArr;
    }

    public String[] getFullPowerWhitelistExceptIdleInternal() {
        String[] strArr;
        synchronized (this) {
            strArr = new String[this.mPowerSaveWhitelistAppsExceptIdle.size() + this.mPowerSaveWhitelistUserApps.size()];
            int i = 0;
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistAppsExceptIdle.size(); i2++) {
                strArr[i] = this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i2);
                i++;
            }
            for (int i3 = 0; i3 < this.mPowerSaveWhitelistUserApps.size(); i3++) {
                strArr[i] = this.mPowerSaveWhitelistUserApps.keyAt(i3);
                i++;
            }
        }
        return strArr;
    }

    public String[] getFullPowerWhitelistInternal() {
        String[] strArr;
        synchronized (this) {
            strArr = new String[this.mPowerSaveWhitelistApps.size() + this.mPowerSaveWhitelistUserApps.size()];
            int i = 0;
            for (int i2 = 0; i2 < this.mPowerSaveWhitelistApps.size(); i2++) {
                strArr[i] = this.mPowerSaveWhitelistApps.keyAt(i2);
                i++;
            }
            for (int i3 = 0; i3 < this.mPowerSaveWhitelistUserApps.size(); i3++) {
                strArr[i] = this.mPowerSaveWhitelistUserApps.keyAt(i3);
                i++;
            }
        }
        return strArr;
    }

    public boolean isPowerSaveWhitelistExceptIdleAppInternal(String str) {
        boolean z;
        synchronized (this) {
            z = this.mPowerSaveWhitelistAppsExceptIdle.containsKey(str) || this.mPowerSaveWhitelistUserApps.containsKey(str);
        }
        return z;
    }

    public boolean isPowerSaveWhitelistAppInternal(String str) {
        boolean z;
        synchronized (this) {
            z = this.mPowerSaveWhitelistApps.containsKey(str) || this.mPowerSaveWhitelistUserApps.containsKey(str);
        }
        return z;
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

    void addPowerSaveTempWhitelistAppChecked(String str, long j, int i, String str2) throws RemoteException {
        getContext().enforceCallingPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int callingUid = Binder.getCallingUid();
        int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), callingUid, i, false, false, "addPowerSaveTempWhitelistApp", (String) null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            addPowerSaveTempWhitelistAppInternal(callingUid, str, j, iHandleIncomingUser, true, str2);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void removePowerSaveTempWhitelistAppChecked(String str, int i) throws RemoteException {
        getContext().enforceCallingPermission("android.permission.CHANGE_DEVICE_IDLE_TEMP_WHITELIST", "No permission to change device idle whitelist");
        int iHandleIncomingUser = ActivityManager.getService().handleIncomingUser(Binder.getCallingPid(), Binder.getCallingUid(), i, false, false, "removePowerSaveTempWhitelistApp", (String) null);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            removePowerSaveTempWhitelistAppInternal(str, iHandleIncomingUser);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void addPowerSaveTempWhitelistAppInternal(int i, String str, long j, int i2, boolean z, String str2) {
        try {
            addPowerSaveTempWhitelistAppDirectInternal(i, UserHandle.getAppId(getContext().getPackageManager().getPackageUidAsUser(str, i2)), j, z, str2);
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    void addPowerSaveTempWhitelistAppDirectInternal(int i, int i2, long j, boolean z, String str) {
        boolean z2;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (this) {
            int appId = UserHandle.getAppId(i);
            if (appId >= 10000 && !this.mPowerSaveWhitelistSystemAppIds.get(appId)) {
                throw new SecurityException("Calling app " + UserHandle.formatUid(i) + " is not on whitelist");
            }
            long jMin = Math.min(j, this.mConstants.MAX_TEMP_APP_WHITELIST_DURATION);
            Pair<MutableLong, String> pair = this.mTempWhitelistAppIdEndTimes.get(i2);
            z2 = false;
            boolean z3 = pair == null;
            if (z3) {
                pair = new Pair<>(new MutableLong(0L), str);
                this.mTempWhitelistAppIdEndTimes.put(i2, pair);
            }
            ((MutableLong) pair.first).value = jElapsedRealtime + jMin;
            if (z3) {
                try {
                    this.mBatteryStats.noteEvent(32785, str, i2);
                } catch (RemoteException e) {
                }
                postTempActiveTimeoutMessage(i2, jMin);
                updateTempWhitelistAppIdsLocked(i2, true);
                if (!z) {
                    this.mHandler.obtainMessage(9, i2, 1).sendToTarget();
                } else {
                    z2 = true;
                }
                reportTempWhitelistChangedLocked();
            }
        }
        if (z2) {
            this.mNetworkPolicyManagerInternal.onTempPowerSaveWhitelistChange(i2, true);
        }
    }

    private void removePowerSaveTempWhitelistAppInternal(String str, int i) {
        try {
            removePowerSaveTempWhitelistAppDirectInternal(UserHandle.getAppId(getContext().getPackageManager().getPackageUidAsUser(str, i)));
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    private void removePowerSaveTempWhitelistAppDirectInternal(int i) {
        synchronized (this) {
            int iIndexOfKey = this.mTempWhitelistAppIdEndTimes.indexOfKey(i);
            if (iIndexOfKey < 0) {
                return;
            }
            String str = (String) this.mTempWhitelistAppIdEndTimes.valueAt(iIndexOfKey).second;
            this.mTempWhitelistAppIdEndTimes.removeAt(iIndexOfKey);
            onAppRemovedFromTempWhitelistLocked(i, str);
        }
    }

    private void postTempActiveTimeoutMessage(int i, long j) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(6, i, 0), j);
    }

    void checkTempAppWhitelistTimeout(int i) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        synchronized (this) {
            Pair<MutableLong, String> pair = this.mTempWhitelistAppIdEndTimes.get(i);
            if (pair == null) {
                return;
            }
            if (jElapsedRealtime >= ((MutableLong) pair.first).value) {
                this.mTempWhitelistAppIdEndTimes.delete(i);
                onAppRemovedFromTempWhitelistLocked(i, (String) pair.second);
            } else {
                postTempActiveTimeoutMessage(i, ((MutableLong) pair.first).value - jElapsedRealtime);
            }
        }
    }

    @GuardedBy("this")
    private void onAppRemovedFromTempWhitelistLocked(int i, String str) {
        updateTempWhitelistAppIdsLocked(i, false);
        this.mHandler.obtainMessage(9, i, 0).sendToTarget();
        reportTempWhitelistChangedLocked();
        try {
            this.mBatteryStats.noteEvent(16401, str, i);
        } catch (RemoteException e) {
        }
    }

    public void exitIdleInternal(String str) {
        synchronized (this) {
            becomeActiveLocked(str, Binder.getCallingUid());
        }
    }

    void updateConnectivityState(Intent intent) {
        ConnectivityService connectivityService;
        synchronized (this) {
            connectivityService = this.mConnectivityService;
        }
        if (connectivityService == null) {
            return;
        }
        NetworkInfo activeNetworkInfo = connectivityService.getActiveNetworkInfo();
        synchronized (this) {
            boolean zIsConnected = false;
            if (activeNetworkInfo != null) {
                if (intent == null) {
                    zIsConnected = activeNetworkInfo.isConnected();
                } else {
                    if (activeNetworkInfo.getType() != intent.getIntExtra("networkType", -1)) {
                        return;
                    } else {
                        zIsConnected = !intent.getBooleanExtra("noConnectivity", false);
                    }
                }
            }
            if (zIsConnected != this.mNetworkConnected) {
                this.mNetworkConnected = zIsConnected;
                if (zIsConnected && this.mLightState == 5) {
                    stepLightIdleStateLocked("network");
                }
            }
        }
    }

    void updateInteractivityLocked() {
        boolean zIsInteractive = this.mPowerManager.isInteractive();
        if (!zIsInteractive && this.mScreenOn) {
            this.mScreenOn = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
                return;
            }
            return;
        }
        if (zIsInteractive) {
            this.mScreenOn = true;
            if (this.mForceIdle) {
                return;
            }
            if (!this.mScreenLocked || !this.mConstants.WAIT_FOR_UNLOCK) {
                becomeActiveLocked("screen", Process.myUid());
            }
        }
    }

    void updateChargingLocked(boolean z) {
        if (!z && this.mCharging) {
            this.mCharging = false;
            if (!this.mForceIdle) {
                becomeInactiveIfAppropriateLocked();
                return;
            }
            return;
        }
        if (z) {
            this.mCharging = z;
            if (!this.mForceIdle) {
                becomeActiveLocked("charging", Process.myUid());
            }
        }
    }

    void keyguardShowingLocked(boolean z) {
        if (this.mScreenLocked != z) {
            this.mScreenLocked = z;
            if (this.mScreenOn && !this.mForceIdle && !this.mScreenLocked) {
                becomeActiveLocked("unlocked", Process.myUid());
            }
        }
    }

    void scheduleReportActiveLocked(String str, int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(5, i, 0, str));
    }

    void becomeActiveLocked(String str, int i) {
        if (this.mState != 0 || this.mLightState != 0) {
            EventLogTags.writeDeviceIdle(0, str);
            EventLogTags.writeDeviceIdleLight(0, str);
            scheduleReportActiveLocked(str, i);
            this.mState = 0;
            this.mLightState = 0;
            this.mInactiveTimeout = this.mConstants.INACTIVE_TIMEOUT;
            this.mCurIdleBudget = 0L;
            this.mMaintenanceStartTime = 0L;
            resetIdleManagementLocked();
            resetLightIdleManagementLocked();
            addEvent(1, str);
        }
    }

    void becomeInactiveIfAppropriateLocked() {
        if ((!this.mScreenOn && !this.mCharging) || this.mForceIdle) {
            if (this.mState == 0 && this.mDeepEnabled) {
                this.mState = 1;
                resetIdleManagementLocked();
                scheduleAlarmLocked(this.mInactiveTimeout, false);
                EventLogTags.writeDeviceIdle(this.mState, "no activity");
            }
            if (this.mLightState == 0 && this.mLightEnabled) {
                this.mLightState = 1;
                resetLightIdleManagementLocked();
                scheduleLightAlarmLocked(this.mConstants.LIGHT_IDLE_AFTER_INACTIVE_TIMEOUT);
                EventLogTags.writeDeviceIdleLight(this.mLightState, "no activity");
            }
        }
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
        if (this.mForceIdle) {
            this.mForceIdle = false;
            if (this.mScreenOn || this.mCharging) {
                becomeActiveLocked("exit-force", Process.myUid());
            }
        }
    }

    void stepLightIdleStateLocked(String str) {
        if (this.mLightState == 7) {
            return;
        }
        EventLogTags.writeDeviceIdleLightStep();
        int i = this.mLightState;
        if (i == 1) {
            this.mCurIdleBudget = this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
            this.mMaintenanceStartTime = 0L;
            if (!isOpsInactiveLocked()) {
                this.mLightState = 3;
                EventLogTags.writeDeviceIdleLight(this.mLightState, str);
                scheduleLightAlarmLocked(this.mConstants.LIGHT_PRE_IDLE_TIMEOUT);
                return;
            }
        } else {
            switch (i) {
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
                        EventLogTags.writeDeviceIdleLight(this.mLightState, str);
                        addEvent(3, null);
                        this.mHandler.sendEmptyMessage(4);
                    } else {
                        scheduleLightAlarmLocked(this.mNextLightIdleDelay);
                        this.mLightState = 5;
                        EventLogTags.writeDeviceIdleLight(this.mLightState, str);
                    }
                    break;
            }
            return;
        }
        if (this.mMaintenanceStartTime != 0) {
            long jElapsedRealtime = SystemClock.elapsedRealtime() - this.mMaintenanceStartTime;
            if (jElapsedRealtime < this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET) {
                this.mCurIdleBudget += this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET - jElapsedRealtime;
            } else {
                this.mCurIdleBudget -= jElapsedRealtime - this.mConstants.LIGHT_IDLE_MAINTENANCE_MIN_BUDGET;
            }
        }
        this.mMaintenanceStartTime = 0L;
        scheduleLightAlarmLocked(this.mNextLightIdleDelay);
        this.mNextLightIdleDelay = Math.min(this.mConstants.LIGHT_MAX_IDLE_TIMEOUT, (long) (this.mNextLightIdleDelay * this.mConstants.LIGHT_IDLE_FACTOR));
        if (this.mNextLightIdleDelay < this.mConstants.LIGHT_IDLE_TIMEOUT) {
            this.mNextLightIdleDelay = this.mConstants.LIGHT_IDLE_TIMEOUT;
        }
        this.mLightState = 4;
        EventLogTags.writeDeviceIdleLight(this.mLightState, str);
        addEvent(2, null);
        this.mGoingIdleWakeLock.acquire();
        this.mHandler.sendEmptyMessage(3);
    }

    void stepIdleStateLocked(String str) {
        EventLogTags.writeDeviceIdleStep();
        if (SystemClock.elapsedRealtime() + this.mConstants.MIN_TIME_TO_ALARM > this.mAlarmManager.getNextWakeFromIdleTime()) {
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
                EventLogTags.writeDeviceIdle(this.mState, str);
                break;
            case 2:
                this.mState = 3;
                EventLogTags.writeDeviceIdle(this.mState, str);
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
                EventLogTags.writeDeviceIdle(this.mState, str);
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
                EventLogTags.writeDeviceIdle(this.mState, str);
                addEvent(4, null);
                this.mGoingIdleWakeLock.acquire();
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
                EventLogTags.writeDeviceIdle(this.mState, str);
                addEvent(4, null);
                this.mGoingIdleWakeLock.acquire();
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
                EventLogTags.writeDeviceIdle(this.mState, str);
                addEvent(5, null);
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
                EventLogTags.writeDeviceIdle(this.mState, str);
                addEvent(4, null);
                this.mGoingIdleWakeLock.acquire();
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

    void setJobsActive(boolean z) {
        synchronized (this) {
            this.mJobsActive = z;
            reportMaintenanceActivityIfNeededLocked();
            if (!z) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    void setAlarmsActive(boolean z) {
        synchronized (this) {
            this.mAlarmsActive = z;
            if (!z) {
                exitMaintenanceEarlyIfNeededLocked();
            }
        }
    }

    boolean registerMaintenanceActivityListener(IMaintenanceActivityListener iMaintenanceActivityListener) {
        boolean z;
        synchronized (this) {
            this.mMaintenanceActivityListeners.register(iMaintenanceActivityListener);
            z = this.mReportedMaintenanceActivity;
        }
        return z;
    }

    void unregisterMaintenanceActivityListener(IMaintenanceActivityListener iMaintenanceActivityListener) {
        synchronized (this) {
            this.mMaintenanceActivityListeners.unregister(iMaintenanceActivityListener);
        }
    }

    void reportMaintenanceActivityIfNeededLocked() {
        boolean z = this.mJobsActive;
        if (z == this.mReportedMaintenanceActivity) {
            return;
        }
        this.mReportedMaintenanceActivity = z;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7, this.mReportedMaintenanceActivity ? 1 : 0, 0));
    }

    boolean isOpsInactiveLocked() {
        return (this.mActiveIdleOpCount > 0 || this.mJobsActive || this.mAlarmsActive) ? false : true;
    }

    void exitMaintenanceEarlyIfNeededLocked() {
        if ((this.mState == 6 || this.mLightState == 6 || this.mLightState == 3) && isOpsInactiveLocked()) {
            SystemClock.elapsedRealtime();
            if (this.mState == 6) {
                stepIdleStateLocked("s:early");
            } else if (this.mLightState == 3) {
                stepLightIdleStateLocked("s:predone");
            } else {
                stepLightIdleStateLocked("s:early");
            }
        }
    }

    void motionLocked() {
        handleMotionDetectedLocked(this.mConstants.MOTION_INACTIVE_TIMEOUT, "motion");
    }

    void handleMotionDetectedLocked(long j, String str) {
        boolean z;
        if (this.mState != 0) {
            if (!(this.mLightState == 4 || this.mLightState == 5 || this.mLightState == 6)) {
                scheduleReportActiveLocked(str, Process.myUid());
                addEvent(1, str);
            }
            this.mState = 0;
            this.mInactiveTimeout = j;
            this.mCurIdleBudget = 0L;
            this.mMaintenanceStartTime = 0L;
            EventLogTags.writeDeviceIdle(this.mState, str);
            z = true;
        } else {
            z = false;
        }
        if (this.mLightState == 7) {
            this.mLightState = 0;
            EventLogTags.writeDeviceIdleLight(this.mLightState, str);
            z = true;
        }
        if (z) {
            becomeInactiveIfAppropriateLocked();
        }
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
        if (this.mNotMoving) {
            stepIdleStateLocked("s:location");
        }
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
        if (this.mNotMoving) {
            stepIdleStateLocked("s:gps");
        }
    }

    void startMonitoringMotionLocked() {
        if (this.mMotionSensor != null && !this.mMotionListener.active) {
            this.mMotionListener.registerLocked();
        }
    }

    void stopMonitoringMotionLocked() {
        if (this.mMotionSensor != null && this.mMotionListener.active) {
            this.mMotionListener.unregisterLocked();
        }
    }

    void cancelAlarmLocked() {
        if (this.mNextAlarmTime != 0) {
            this.mNextAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mDeepAlarmListener);
        }
    }

    void cancelLightAlarmLocked() {
        if (this.mNextLightAlarmTime != 0) {
            this.mNextLightAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mLightAlarmListener);
        }
    }

    void cancelLocatingLocked() {
        if (this.mLocating) {
            this.mLocationManager.removeUpdates(this.mGenericLocationListener);
            this.mLocationManager.removeUpdates(this.mGpsLocationListener);
            this.mLocating = false;
        }
    }

    void cancelSensingTimeoutAlarmLocked() {
        if (this.mNextSensingTimeoutAlarmTime != 0) {
            this.mNextSensingTimeoutAlarmTime = 0L;
            this.mAlarmManager.cancel(this.mSensingTimeoutAlarmListener);
        }
    }

    void scheduleAlarmLocked(long j, boolean z) {
        if (this.mMotionSensor == null) {
            return;
        }
        this.mNextAlarmTime = SystemClock.elapsedRealtime() + j;
        if (z) {
            this.mAlarmManager.setIdleUntil(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        } else {
            this.mAlarmManager.set(2, this.mNextAlarmTime, "DeviceIdleController.deep", this.mDeepAlarmListener, this.mHandler);
        }
    }

    void scheduleLightAlarmLocked(long j) {
        this.mNextLightAlarmTime = SystemClock.elapsedRealtime() + j;
        this.mAlarmManager.set(2, this.mNextLightAlarmTime, "DeviceIdleController.light", this.mLightAlarmListener, this.mHandler);
    }

    void scheduleSensingTimeoutAlarmLocked(long j) {
        this.mNextSensingTimeoutAlarmTime = SystemClock.elapsedRealtime() + j;
        this.mAlarmManager.set(2, this.mNextSensingTimeoutAlarmTime, "DeviceIdleController.sensing", this.mSensingTimeoutAlarmListener, this.mHandler);
    }

    private static int[] buildAppIdArray(ArrayMap<String, Integer> arrayMap, ArrayMap<String, Integer> arrayMap2, SparseBooleanArray sparseBooleanArray) {
        sparseBooleanArray.clear();
        if (arrayMap != null) {
            for (int i = 0; i < arrayMap.size(); i++) {
                sparseBooleanArray.put(arrayMap.valueAt(i).intValue(), true);
            }
        }
        if (arrayMap2 != null) {
            for (int i2 = 0; i2 < arrayMap2.size(); i2++) {
                sparseBooleanArray.put(arrayMap2.valueAt(i2).intValue(), true);
            }
        }
        int size = sparseBooleanArray.size();
        int[] iArr = new int[size];
        for (int i3 = 0; i3 < size; i3++) {
            iArr[i3] = sparseBooleanArray.keyAt(i3);
        }
        return iArr;
    }

    private void updateWhitelistAppIdsLocked() {
        this.mPowerSaveWhitelistExceptIdleAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistAppsExceptIdle, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistExceptIdleAppIds);
        this.mPowerSaveWhitelistAllAppIdArray = buildAppIdArray(this.mPowerSaveWhitelistApps, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistAllAppIds);
        this.mPowerSaveWhitelistUserAppIdArray = buildAppIdArray(null, this.mPowerSaveWhitelistUserApps, this.mPowerSaveWhitelistUserAppIds);
        if (this.mLocalActivityManager != null) {
            this.mLocalActivityManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray, this.mPowerSaveWhitelistExceptIdleAppIdArray);
        }
        if (this.mLocalPowerManager != null) {
            this.mLocalPowerManager.setDeviceIdleWhitelist(this.mPowerSaveWhitelistAllAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
    }

    private void updateTempWhitelistAppIdsLocked(int i, boolean z) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (this.mTempWhitelistAppIdArray.length != size) {
            this.mTempWhitelistAppIdArray = new int[size];
        }
        for (int i2 = 0; i2 < size; i2++) {
            this.mTempWhitelistAppIdArray[i2] = this.mTempWhitelistAppIdEndTimes.keyAt(i2);
        }
        if (this.mLocalActivityManager != null) {
            this.mLocalActivityManager.updateDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray, i, z);
        }
        if (this.mLocalPowerManager != null) {
            this.mLocalPowerManager.setDeviceIdleTempWhitelist(this.mTempWhitelistAppIdArray);
        }
        passWhiteListsToForceAppStandbyTrackerLocked();
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

    private void passWhiteListsToForceAppStandbyTrackerLocked() {
        this.mAppStateTracker.setPowerSaveWhitelistAppIds(this.mPowerSaveWhitelistExceptIdleAppIdArray, this.mPowerSaveWhitelistUserAppIdArray, this.mTempWhitelistAppIdArray);
    }

    void readConfigFileLocked() {
        this.mPowerSaveWhitelistUserApps.clear();
        try {
            try {
                FileInputStream fileInputStreamOpenRead = this.mConfigFile.openRead();
                try {
                    XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                    xmlPullParserNewPullParser.setInput(fileInputStreamOpenRead, StandardCharsets.UTF_8.name());
                    readConfigFileLocked(xmlPullParserNewPullParser);
                    fileInputStreamOpenRead.close();
                } catch (XmlPullParserException e) {
                    fileInputStreamOpenRead.close();
                } catch (Throwable th) {
                    try {
                        fileInputStreamOpenRead.close();
                    } catch (IOException e2) {
                    }
                    throw th;
                }
            } catch (FileNotFoundException e3) {
            }
        } catch (IOException e4) {
        }
    }

    private void readConfigFileLocked(XmlPullParser xmlPullParser) {
        int next;
        byte b;
        PackageManager packageManager = getContext().getPackageManager();
        do {
            try {
                next = xmlPullParser.next();
                if (next == 2) {
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
        } while (next != 1);
        if (next != 2) {
            throw new IllegalStateException("no start tag found");
        }
        int depth = xmlPullParser.getDepth();
        while (true) {
            int next2 = xmlPullParser.next();
            if (next2 != 1) {
                if (next2 != 3 || xmlPullParser.getDepth() > depth) {
                    if (next2 != 3 && next2 != 4) {
                        String name = xmlPullParser.getName();
                        int iHashCode = name.hashCode();
                        if (iHashCode != 3797) {
                            b = (iHashCode == 111376009 && name.equals("un-wl")) ? (byte) 1 : (byte) -1;
                            switch (b) {
                                case 0:
                                    String attributeValue = xmlPullParser.getAttributeValue(null, "n");
                                    if (attributeValue != null) {
                                        try {
                                            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(attributeValue, DumpState.DUMP_CHANGES);
                                            this.mPowerSaveWhitelistUserApps.put(applicationInfo.packageName, Integer.valueOf(UserHandle.getAppId(applicationInfo.uid)));
                                        } catch (PackageManager.NameNotFoundException e7) {
                                        }
                                    }
                                    break;
                                case 1:
                                    String attributeValue2 = xmlPullParser.getAttributeValue(null, "n");
                                    if (this.mPowerSaveWhitelistApps.containsKey(attributeValue2)) {
                                        this.mRemovedFromSystemWhitelistApps.put(attributeValue2, this.mPowerSaveWhitelistApps.remove(attributeValue2));
                                    }
                                    break;
                                default:
                                    Slog.w(TAG, "Unknown element under <config>: " + xmlPullParser.getName());
                                    XmlUtils.skipCurrentTag(xmlPullParser);
                                    break;
                            }
                        } else {
                            if (name.equals("wl")) {
                                b = 0;
                            }
                            switch (b) {
                            }
                        }
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    void writeConfigFileLocked() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000L);
    }

    void handleWriteConfigFile() {
        FileOutputStream fileOutputStreamStartWrite;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            synchronized (this) {
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
                writeConfigFileLocked(fastXmlSerializer);
            }
        } catch (IOException e) {
        }
        synchronized (this.mConfigFile) {
            try {
                fileOutputStreamStartWrite = this.mConfigFile.startWrite();
                try {
                    byteArrayOutputStream.writeTo(fileOutputStreamStartWrite);
                    fileOutputStreamStartWrite.flush();
                    FileUtils.sync(fileOutputStreamStartWrite);
                    fileOutputStreamStartWrite.close();
                    this.mConfigFile.finishWrite(fileOutputStreamStartWrite);
                } catch (IOException e2) {
                    e = e2;
                    Slog.w(TAG, "Error writing config file", e);
                    this.mConfigFile.failWrite(fileOutputStreamStartWrite);
                }
            } catch (IOException e3) {
                e = e3;
                fileOutputStreamStartWrite = null;
            }
        }
    }

    void writeConfigFileLocked(XmlSerializer xmlSerializer) throws IOException {
        xmlSerializer.startDocument(null, true);
        xmlSerializer.startTag(null, "config");
        for (int i = 0; i < this.mPowerSaveWhitelistUserApps.size(); i++) {
            String strKeyAt = this.mPowerSaveWhitelistUserApps.keyAt(i);
            xmlSerializer.startTag(null, "wl");
            xmlSerializer.attribute(null, "n", strKeyAt);
            xmlSerializer.endTag(null, "wl");
        }
        for (int i2 = 0; i2 < this.mRemovedFromSystemWhitelistApps.size(); i2++) {
            xmlSerializer.startTag(null, "un-wl");
            xmlSerializer.attribute(null, "n", this.mRemovedFromSystemWhitelistApps.keyAt(i2));
            xmlSerializer.endTag(null, "un-wl");
        }
        xmlSerializer.endTag(null, "config");
        xmlSerializer.endDocument();
    }

    static void dumpHelp(PrintWriter printWriter) {
        printWriter.println("Device idle controller (deviceidle) commands:");
        printWriter.println("  help");
        printWriter.println("    Print this help text.");
        printWriter.println("  step [light|deep]");
        printWriter.println("    Immediately step to next state, without waiting for alarm.");
        printWriter.println("  force-idle [light|deep]");
        printWriter.println("    Force directly into idle mode, regardless of other device state.");
        printWriter.println("  force-inactive");
        printWriter.println("    Force to be inactive, ready to freely step idle states.");
        printWriter.println("  unforce");
        printWriter.println("    Resume normal functioning after force-idle or force-inactive.");
        printWriter.println("  get [light|deep|force|screen|charging|network]");
        printWriter.println("    Retrieve the current given state.");
        printWriter.println("  disable [light|deep|all]");
        printWriter.println("    Completely disable device idle mode.");
        printWriter.println("  enable [light|deep|all]");
        printWriter.println("    Re-enable device idle mode after it had previously been disabled.");
        printWriter.println("  enabled [light|deep|all]");
        printWriter.println("    Print 1 if device idle mode is currently enabled, else 0.");
        printWriter.println("  whitelist");
        printWriter.println("    Print currently whitelisted apps.");
        printWriter.println("  whitelist [package ...]");
        printWriter.println("    Add (prefix with +) or remove (prefix with -) packages.");
        printWriter.println("  sys-whitelist [package ...|reset]");
        printWriter.println("    Prefix the package with '-' to remove it from the system whitelist or '+' to put it back in the system whitelist.");
        printWriter.println("    Note that only packages that were earlier removed from the system whitelist can be added back.");
        printWriter.println("    reset will reset the whitelist to the original state");
        printWriter.println("    Prints the system whitelist if no arguments are specified");
        printWriter.println("  except-idle-whitelist [package ...|reset]");
        printWriter.println("    Prefix the package with '+' to add it to whitelist or '=' to check if it is already whitelisted");
        printWriter.println("    [reset] will reset the whitelist to it's original state");
        printWriter.println("    Note that unlike <whitelist> cmd, changes made using this won't be persisted across boots");
        printWriter.println("  tempwhitelist");
        printWriter.println("    Print packages that are temporarily whitelisted.");
        printWriter.println("  tempwhitelist [-u USER] [-d DURATION] [-r] [package]");
        printWriter.println("    Temporarily place package in whitelist for DURATION milliseconds.");
        printWriter.println("    If no DURATION is specified, 10 seconds is used");
        printWriter.println("    If [-r] option is used, then the package is removed from temp whitelist and any [-d] is ignored");
        printWriter.println("  motion");
        printWriter.println("    Simulate a motion event to bring the device out of deep doze");
    }

    class Shell extends ShellCommand {
        int userId = 0;

        Shell() {
        }

        public int onCommand(String str) {
            return DeviceIdleController.this.onShellCommand(this, str);
        }

        public void onHelp() {
            DeviceIdleController.dumpHelp(getOutPrintWriter());
        }
    }

    int onShellCommand(Shell shell, String str) {
        long jClearCallingIdentity;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        PrintWriter outPrintWriter = shell.getOutPrintWriter();
        if ("step".equals(str)) {
            getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
            synchronized (this) {
                jClearCallingIdentity = Binder.clearCallingIdentity();
                String nextArg = shell.getNextArg();
                if (nextArg != null) {
                    try {
                        if (!"deep".equals(nextArg)) {
                            if ("light".equals(nextArg)) {
                                stepLightIdleStateLocked("s:shell");
                                outPrintWriter.print("Stepped to light: ");
                                outPrintWriter.println(lightStateToString(this.mLightState));
                            } else {
                                outPrintWriter.println("Unknown idle mode: " + nextArg);
                            }
                        }
                    } catch (Throwable th) {
                        throw th;
                    }
                }
                stepIdleStateLocked("s:shell");
                outPrintWriter.print("Stepped to deep: ");
                outPrintWriter.println(stateToString(this.mState));
            }
        } else {
            boolean z5 = true;
            if ("force-idle".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    String nextArg2 = shell.getNextArg();
                    if (nextArg2 != null) {
                        try {
                            if ("deep".equals(nextArg2)) {
                                if (this.mDeepEnabled) {
                                    outPrintWriter.println("Unable to go deep idle; not enabled");
                                    return -1;
                                }
                                this.mForceIdle = true;
                                becomeInactiveIfAppropriateLocked();
                                int i = this.mState;
                                while (i != 5) {
                                    stepIdleStateLocked("s:shell");
                                    if (i == this.mState) {
                                        outPrintWriter.print("Unable to go deep idle; stopped at ");
                                        outPrintWriter.println(stateToString(this.mState));
                                        exitForceIdleLocked();
                                        return -1;
                                    }
                                    i = this.mState;
                                }
                                outPrintWriter.println("Now forced in to deep idle mode");
                            } else if ("light".equals(nextArg2)) {
                                this.mForceIdle = true;
                                becomeInactiveIfAppropriateLocked();
                                int i2 = this.mLightState;
                                while (i2 != 4) {
                                    stepLightIdleStateLocked("s:shell");
                                    if (i2 == this.mLightState) {
                                        outPrintWriter.print("Unable to go light idle; stopped at ");
                                        outPrintWriter.println(lightStateToString(this.mLightState));
                                        exitForceIdleLocked();
                                        return -1;
                                    }
                                    i2 = this.mLightState;
                                }
                                outPrintWriter.println("Now forced in to light idle mode");
                            } else {
                                outPrintWriter.println("Unknown idle mode: " + nextArg2);
                            }
                        } finally {
                        }
                    } else if (this.mDeepEnabled) {
                    }
                }
            } else if ("force-inactive".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        this.mForceIdle = true;
                        becomeInactiveIfAppropriateLocked();
                        outPrintWriter.print("Light state: ");
                        outPrintWriter.print(lightStateToString(this.mLightState));
                        outPrintWriter.print(", deep state: ");
                        outPrintWriter.println(stateToString(this.mState));
                    } finally {
                    }
                }
            } else if ("unforce".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        exitForceIdleLocked();
                        outPrintWriter.print("Light state: ");
                        outPrintWriter.print(lightStateToString(this.mLightState));
                        outPrintWriter.print(", deep state: ");
                        outPrintWriter.println(stateToString(this.mState));
                    } finally {
                    }
                }
            } else if ("get".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    String nextArg3 = shell.getNextArg();
                    if (nextArg3 != null) {
                        jClearCallingIdentity = Binder.clearCallingIdentity();
                        try {
                            switch (nextArg3) {
                                case "light":
                                    outPrintWriter.println(lightStateToString(this.mLightState));
                                    break;
                                case "deep":
                                    outPrintWriter.println(stateToString(this.mState));
                                    break;
                                case "force":
                                    outPrintWriter.println(this.mForceIdle);
                                    break;
                                case "screen":
                                    outPrintWriter.println(this.mScreenOn);
                                    break;
                                case "charging":
                                    outPrintWriter.println(this.mCharging);
                                    break;
                                case "network":
                                    outPrintWriter.println(this.mNetworkConnected);
                                    break;
                                default:
                                    outPrintWriter.println("Unknown get option: " + nextArg3);
                                    break;
                            }
                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                        } finally {
                        }
                    } else {
                        outPrintWriter.println("Argument required");
                    }
                }
            } else if ("disable".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    String nextArg4 = shell.getNextArg();
                    if (nextArg4 != null) {
                        try {
                            if (!"deep".equals(nextArg4) && !"all".equals(nextArg4)) {
                                z3 = false;
                            }
                            z4 = z3;
                            if (nextArg4 == null && !"light".equals(nextArg4) && !"all".equals(nextArg4)) {
                                z5 = z4;
                            } else if (this.mLightEnabled) {
                                this.mLightEnabled = false;
                                outPrintWriter.println("Light idle mode disabled");
                                z3 = true;
                            }
                            if (z3) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(nextArg4 == null ? "all" : nextArg4);
                                sb.append("-disabled");
                                becomeActiveLocked(sb.toString(), Process.myUid());
                            }
                            if (!z5) {
                                outPrintWriter.println("Unknown idle mode: " + nextArg4);
                            }
                        } finally {
                        }
                    }
                    if (this.mDeepEnabled) {
                        this.mDeepEnabled = false;
                        outPrintWriter.println("Deep idle mode disabled");
                        z3 = true;
                        z4 = z3;
                        if (nextArg4 == null) {
                        }
                        if (this.mLightEnabled) {
                        }
                        if (z3) {
                        }
                        if (!z5) {
                        }
                    } else {
                        z3 = false;
                        z4 = true;
                        if (nextArg4 == null) {
                        }
                        if (this.mLightEnabled) {
                        }
                        if (z3) {
                        }
                        if (!z5) {
                        }
                    }
                }
            } else if ("enable".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    String nextArg5 = shell.getNextArg();
                    if (nextArg5 != null) {
                        try {
                            if (!"deep".equals(nextArg5) && !"all".equals(nextArg5)) {
                                z = false;
                            }
                            z2 = z;
                            if (nextArg5 == null && !"light".equals(nextArg5) && !"all".equals(nextArg5)) {
                                z5 = z2;
                            } else if (!this.mLightEnabled) {
                                this.mLightEnabled = true;
                                outPrintWriter.println("Light idle mode enable");
                                z = true;
                            }
                            if (z) {
                                becomeInactiveIfAppropriateLocked();
                            }
                            if (!z5) {
                                outPrintWriter.println("Unknown idle mode: " + nextArg5);
                            }
                        } finally {
                        }
                    }
                    if (this.mDeepEnabled) {
                        z = false;
                        z2 = true;
                        if (nextArg5 == null) {
                        }
                        if (!this.mLightEnabled) {
                        }
                        if (z) {
                        }
                        if (!z5) {
                        }
                    } else {
                        this.mDeepEnabled = true;
                        outPrintWriter.println("Deep idle mode enabled");
                        z = true;
                        z2 = z;
                        if (nextArg5 == null) {
                        }
                        if (!this.mLightEnabled) {
                        }
                        if (z) {
                        }
                        if (!z5) {
                        }
                    }
                }
            } else if ("enabled".equals(str)) {
                synchronized (this) {
                    String nextArg6 = shell.getNextArg();
                    if (nextArg6 == null || "all".equals(nextArg6)) {
                        outPrintWriter.println((this.mDeepEnabled && this.mLightEnabled) ? "1" : 0);
                    } else if ("deep".equals(nextArg6)) {
                        outPrintWriter.println(this.mDeepEnabled ? "1" : 0);
                    } else if ("light".equals(nextArg6)) {
                        outPrintWriter.println(this.mLightEnabled ? "1" : 0);
                    } else {
                        outPrintWriter.println("Unknown idle mode: " + nextArg6);
                    }
                }
            } else if ("whitelist".equals(str)) {
                String nextArg7 = shell.getNextArg();
                if (nextArg7 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    while (nextArg7.length() >= 1 && (nextArg7.charAt(0) == '-' || nextArg7.charAt(0) == '+' || nextArg7.charAt(0) == '=')) {
                        try {
                            char cCharAt = nextArg7.charAt(0);
                            String strSubstring = nextArg7.substring(1);
                            if (cCharAt == '+') {
                                if (addPowerSaveWhitelistAppInternal(strSubstring)) {
                                    outPrintWriter.println("Added: " + strSubstring);
                                } else {
                                    outPrintWriter.println("Unknown package: " + strSubstring);
                                }
                            } else if (cCharAt != '-') {
                                outPrintWriter.println(getPowerSaveWhitelistAppInternal(strSubstring));
                            } else if (removePowerSaveWhitelistAppInternal(strSubstring)) {
                                outPrintWriter.println("Removed: " + strSubstring);
                            }
                            nextArg7 = shell.getNextArg();
                            if (nextArg7 == null) {
                            }
                        } finally {
                        }
                    }
                    outPrintWriter.println("Package must be prefixed with +, -, or =: " + nextArg7);
                    return -1;
                }
                synchronized (this) {
                    for (int i3 = 0; i3 < this.mPowerSaveWhitelistAppsExceptIdle.size(); i3++) {
                        outPrintWriter.print("system-excidle,");
                        outPrintWriter.print(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i3));
                        outPrintWriter.print(",");
                        outPrintWriter.println(this.mPowerSaveWhitelistAppsExceptIdle.valueAt(i3));
                    }
                    for (int i4 = 0; i4 < this.mPowerSaveWhitelistApps.size(); i4++) {
                        outPrintWriter.print("system,");
                        outPrintWriter.print(this.mPowerSaveWhitelistApps.keyAt(i4));
                        outPrintWriter.print(",");
                        outPrintWriter.println(this.mPowerSaveWhitelistApps.valueAt(i4));
                    }
                    for (int i5 = 0; i5 < this.mPowerSaveWhitelistUserApps.size(); i5++) {
                        outPrintWriter.print("user,");
                        outPrintWriter.print(this.mPowerSaveWhitelistUserApps.keyAt(i5));
                        outPrintWriter.print(",");
                        outPrintWriter.println(this.mPowerSaveWhitelistUserApps.valueAt(i5));
                    }
                }
            } else if ("tempwhitelist".equals(str)) {
                long j = 10000;
                boolean z6 = false;
                while (true) {
                    String nextOption = shell.getNextOption();
                    if (nextOption == null) {
                        String nextArg8 = shell.getNextArg();
                        if (nextArg8 != null) {
                            try {
                                if (z6) {
                                    removePowerSaveTempWhitelistAppChecked(nextArg8, shell.userId);
                                } else {
                                    addPowerSaveTempWhitelistAppChecked(nextArg8, j, shell.userId, "shell");
                                }
                            } catch (Exception e) {
                                outPrintWriter.println("Failed: " + e);
                                return -1;
                            }
                        } else {
                            if (z6) {
                                outPrintWriter.println("[-r] requires a package name");
                                return -1;
                            }
                            dumpTempWhitelistSchedule(outPrintWriter, false);
                        }
                    } else if ("-u".equals(nextOption)) {
                        String nextArg9 = shell.getNextArg();
                        if (nextArg9 == null) {
                            outPrintWriter.println("-u requires a user number");
                            return -1;
                        }
                        shell.userId = Integer.parseInt(nextArg9);
                    } else if ("-d".equals(nextOption)) {
                        String nextArg10 = shell.getNextArg();
                        if (nextArg10 == null) {
                            outPrintWriter.println("-d requires a duration");
                            return -1;
                        }
                        j = Long.parseLong(nextArg10);
                    } else if ("-r".equals(nextOption)) {
                        z6 = true;
                    }
                }
            } else if ("except-idle-whitelist".equals(str)) {
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String nextArg11 = shell.getNextArg();
                    if (nextArg11 == null) {
                        outPrintWriter.println("No arguments given");
                        return -1;
                    }
                    if (!"reset".equals(nextArg11)) {
                        while (nextArg11.length() >= 1 && (nextArg11.charAt(0) == '-' || nextArg11.charAt(0) == '+' || nextArg11.charAt(0) == '=')) {
                            char cCharAt2 = nextArg11.charAt(0);
                            String strSubstring2 = nextArg11.substring(1);
                            if (cCharAt2 != '+') {
                                if (cCharAt2 != '=') {
                                    outPrintWriter.println("Unknown argument: " + nextArg11);
                                    return -1;
                                }
                                outPrintWriter.println(getPowerSaveWhitelistExceptIdleInternal(strSubstring2));
                            } else if (addPowerSaveWhitelistExceptIdleInternal(strSubstring2)) {
                                outPrintWriter.println("Added: " + strSubstring2);
                            } else {
                                outPrintWriter.println("Unknown package: " + strSubstring2);
                            }
                            nextArg11 = shell.getNextArg();
                            if (nextArg11 == null) {
                            }
                        }
                        outPrintWriter.println("Package must be prefixed with +, -, or =: " + nextArg11);
                        return -1;
                    }
                    resetPowerSaveWhitelistExceptIdleInternal();
                } finally {
                }
            } else if ("sys-whitelist".equals(str)) {
                String nextArg12 = shell.getNextArg();
                if (nextArg12 != null) {
                    getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        if (!"reset".equals(nextArg12)) {
                            while (nextArg12.length() >= 1 && (nextArg12.charAt(0) == '-' || nextArg12.charAt(0) == '+')) {
                                char cCharAt3 = nextArg12.charAt(0);
                                String strSubstring3 = nextArg12.substring(1);
                                if (cCharAt3 != '+') {
                                    if (cCharAt3 == '-' && removeSystemPowerWhitelistAppInternal(strSubstring3)) {
                                        outPrintWriter.println("Removed " + strSubstring3);
                                    }
                                } else if (restoreSystemPowerWhitelistAppInternal(strSubstring3)) {
                                    outPrintWriter.println("Restored " + strSubstring3);
                                }
                                nextArg12 = shell.getNextArg();
                                if (nextArg12 == null) {
                                }
                            }
                            outPrintWriter.println("Package must be prefixed with + or - " + nextArg12);
                            return -1;
                        }
                        resetSystemPowerWhitelistInternal();
                    } finally {
                    }
                } else {
                    synchronized (this) {
                        for (int i6 = 0; i6 < this.mPowerSaveWhitelistApps.size(); i6++) {
                            outPrintWriter.print(this.mPowerSaveWhitelistApps.keyAt(i6));
                            outPrintWriter.print(",");
                            outPrintWriter.println(this.mPowerSaveWhitelistApps.valueAt(i6));
                        }
                    }
                }
            } else {
                if (!"motion".equals(str)) {
                    return shell.handleDefaultCommands(str);
                }
                getContext().enforceCallingOrSelfPermission("android.permission.DEVICE_POWER", null);
                synchronized (this) {
                    jClearCallingIdentity = Binder.clearCallingIdentity();
                    try {
                        motionLocked();
                        outPrintWriter.print("Light state: ");
                        outPrintWriter.print(lightStateToString(this.mLightState));
                        outPrintWriter.print(", deep state: ");
                        outPrintWriter.println(stateToString(this.mState));
                    } finally {
                    }
                }
            }
        }
        return 0;
    }

    void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        String str;
        if (DumpUtils.checkDumpPermission(getContext(), TAG, printWriter)) {
            if (strArr != null) {
                int i = 0;
                int i2 = 0;
                while (i < strArr.length) {
                    String str2 = strArr[i];
                    if ("-h".equals(str2)) {
                        dumpHelp(printWriter);
                        return;
                    }
                    if ("-u".equals(str2)) {
                        i++;
                        if (i < strArr.length) {
                            i2 = Integer.parseInt(strArr[i]);
                        }
                    } else if (!"-a".equals(str2)) {
                        if (str2.length() > 0 && str2.charAt(0) == '-') {
                            printWriter.println("Unknown option: " + str2);
                            return;
                        }
                        Shell shell = new Shell();
                        shell.userId = i2;
                        String[] strArr2 = new String[strArr.length - i];
                        System.arraycopy(strArr, i, strArr2, 0, strArr.length - i);
                        shell.exec(this.mBinderService, null, fileDescriptor, null, strArr2, null, new ResultReceiver(null));
                        return;
                    }
                    i++;
                }
            }
            synchronized (this) {
                this.mConstants.dump(printWriter);
                if (this.mEventCmds[0] != 0) {
                    printWriter.println("  Idling history:");
                    long jElapsedRealtime = SystemClock.elapsedRealtime();
                    for (int i3 = 99; i3 >= 0; i3--) {
                        if (this.mEventCmds[i3] != 0) {
                            switch (this.mEventCmds[i3]) {
                                case 1:
                                    str = "     normal";
                                    break;
                                case 2:
                                    str = " light-idle";
                                    break;
                                case 3:
                                    str = "light-maint";
                                    break;
                                case 4:
                                    str = "  deep-idle";
                                    break;
                                case 5:
                                    str = " deep-maint";
                                    break;
                                default:
                                    str = "         ??";
                                    break;
                            }
                            printWriter.print("    ");
                            printWriter.print(str);
                            printWriter.print(": ");
                            TimeUtils.formatDuration(this.mEventTimes[i3], jElapsedRealtime, printWriter);
                            if (this.mEventReasons[i3] != null) {
                                printWriter.print(" (");
                                printWriter.print(this.mEventReasons[i3]);
                                printWriter.print(")");
                            }
                            printWriter.println();
                        }
                    }
                }
                int size = this.mPowerSaveWhitelistAppsExceptIdle.size();
                if (size > 0) {
                    printWriter.println("  Whitelist (except idle) system apps:");
                    for (int i4 = 0; i4 < size; i4++) {
                        printWriter.print("    ");
                        printWriter.println(this.mPowerSaveWhitelistAppsExceptIdle.keyAt(i4));
                    }
                }
                int size2 = this.mPowerSaveWhitelistApps.size();
                if (size2 > 0) {
                    printWriter.println("  Whitelist system apps:");
                    for (int i5 = 0; i5 < size2; i5++) {
                        printWriter.print("    ");
                        printWriter.println(this.mPowerSaveWhitelistApps.keyAt(i5));
                    }
                }
                int size3 = this.mRemovedFromSystemWhitelistApps.size();
                if (size3 > 0) {
                    printWriter.println("  Removed from whitelist system apps:");
                    for (int i6 = 0; i6 < size3; i6++) {
                        printWriter.print("    ");
                        printWriter.println(this.mRemovedFromSystemWhitelistApps.keyAt(i6));
                    }
                }
                int size4 = this.mPowerSaveWhitelistUserApps.size();
                if (size4 > 0) {
                    printWriter.println("  Whitelist user apps:");
                    for (int i7 = 0; i7 < size4; i7++) {
                        printWriter.print("    ");
                        printWriter.println(this.mPowerSaveWhitelistUserApps.keyAt(i7));
                    }
                }
                int size5 = this.mPowerSaveWhitelistExceptIdleAppIds.size();
                if (size5 > 0) {
                    printWriter.println("  Whitelist (except idle) all app ids:");
                    for (int i8 = 0; i8 < size5; i8++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistExceptIdleAppIds.keyAt(i8));
                        printWriter.println();
                    }
                }
                int size6 = this.mPowerSaveWhitelistUserAppIds.size();
                if (size6 > 0) {
                    printWriter.println("  Whitelist user app ids:");
                    for (int i9 = 0; i9 < size6; i9++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistUserAppIds.keyAt(i9));
                        printWriter.println();
                    }
                }
                int size7 = this.mPowerSaveWhitelistAllAppIds.size();
                if (size7 > 0) {
                    printWriter.println("  Whitelist all app ids:");
                    for (int i10 = 0; i10 < size7; i10++) {
                        printWriter.print("    ");
                        printWriter.print(this.mPowerSaveWhitelistAllAppIds.keyAt(i10));
                        printWriter.println();
                    }
                }
                dumpTempWhitelistSchedule(printWriter, true);
                int length = this.mTempWhitelistAppIdArray != null ? this.mTempWhitelistAppIdArray.length : 0;
                if (length > 0) {
                    printWriter.println("  Temp whitelist app ids:");
                    for (int i11 = 0; i11 < length; i11++) {
                        printWriter.print("    ");
                        printWriter.print(this.mTempWhitelistAppIdArray[i11]);
                        printWriter.println();
                    }
                }
                printWriter.print("  mLightEnabled=");
                printWriter.print(this.mLightEnabled);
                printWriter.print("  mDeepEnabled=");
                printWriter.println(this.mDeepEnabled);
                printWriter.print("  mForceIdle=");
                printWriter.println(this.mForceIdle);
                printWriter.print("  mMotionSensor=");
                printWriter.println(this.mMotionSensor);
                printWriter.print("  mScreenOn=");
                printWriter.println(this.mScreenOn);
                printWriter.print("  mScreenLocked=");
                printWriter.println(this.mScreenLocked);
                printWriter.print("  mNetworkConnected=");
                printWriter.println(this.mNetworkConnected);
                printWriter.print("  mCharging=");
                printWriter.println(this.mCharging);
                printWriter.print("  mMotionActive=");
                printWriter.println(this.mMotionListener.active);
                printWriter.print("  mNotMoving=");
                printWriter.println(this.mNotMoving);
                printWriter.print("  mLocating=");
                printWriter.print(this.mLocating);
                printWriter.print(" mHasGps=");
                printWriter.print(this.mHasGps);
                printWriter.print(" mHasNetwork=");
                printWriter.print(this.mHasNetworkLocation);
                printWriter.print(" mLocated=");
                printWriter.println(this.mLocated);
                if (this.mLastGenericLocation != null) {
                    printWriter.print("  mLastGenericLocation=");
                    printWriter.println(this.mLastGenericLocation);
                }
                if (this.mLastGpsLocation != null) {
                    printWriter.print("  mLastGpsLocation=");
                    printWriter.println(this.mLastGpsLocation);
                }
                printWriter.print("  mState=");
                printWriter.print(stateToString(this.mState));
                printWriter.print(" mLightState=");
                printWriter.println(lightStateToString(this.mLightState));
                printWriter.print("  mInactiveTimeout=");
                TimeUtils.formatDuration(this.mInactiveTimeout, printWriter);
                printWriter.println();
                if (this.mActiveIdleOpCount != 0) {
                    printWriter.print("  mActiveIdleOpCount=");
                    printWriter.println(this.mActiveIdleOpCount);
                }
                if (this.mNextAlarmTime != 0) {
                    printWriter.print("  mNextAlarmTime=");
                    TimeUtils.formatDuration(this.mNextAlarmTime, SystemClock.elapsedRealtime(), printWriter);
                    printWriter.println();
                }
                if (this.mNextIdlePendingDelay != 0) {
                    printWriter.print("  mNextIdlePendingDelay=");
                    TimeUtils.formatDuration(this.mNextIdlePendingDelay, printWriter);
                    printWriter.println();
                }
                if (this.mNextIdleDelay != 0) {
                    printWriter.print("  mNextIdleDelay=");
                    TimeUtils.formatDuration(this.mNextIdleDelay, printWriter);
                    printWriter.println();
                }
                if (this.mNextLightIdleDelay != 0) {
                    printWriter.print("  mNextIdleDelay=");
                    TimeUtils.formatDuration(this.mNextLightIdleDelay, printWriter);
                    printWriter.println();
                }
                if (this.mNextLightAlarmTime != 0) {
                    printWriter.print("  mNextLightAlarmTime=");
                    TimeUtils.formatDuration(this.mNextLightAlarmTime, SystemClock.elapsedRealtime(), printWriter);
                    printWriter.println();
                }
                if (this.mCurIdleBudget != 0) {
                    printWriter.print("  mCurIdleBudget=");
                    TimeUtils.formatDuration(this.mCurIdleBudget, printWriter);
                    printWriter.println();
                }
                if (this.mMaintenanceStartTime != 0) {
                    printWriter.print("  mMaintenanceStartTime=");
                    TimeUtils.formatDuration(this.mMaintenanceStartTime, SystemClock.elapsedRealtime(), printWriter);
                    printWriter.println();
                }
                if (this.mJobsActive) {
                    printWriter.print("  mJobsActive=");
                    printWriter.println(this.mJobsActive);
                }
                if (this.mAlarmsActive) {
                    printWriter.print("  mAlarmsActive=");
                    printWriter.println(this.mAlarmsActive);
                }
            }
        }
    }

    void dumpTempWhitelistSchedule(PrintWriter printWriter, boolean z) {
        int size = this.mTempWhitelistAppIdEndTimes.size();
        if (size > 0) {
            String str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            if (z) {
                printWriter.println("  Temp whitelist schedule:");
                str = "    ";
            }
            long jElapsedRealtime = SystemClock.elapsedRealtime();
            for (int i = 0; i < size; i++) {
                printWriter.print(str);
                printWriter.print("UID=");
                printWriter.print(this.mTempWhitelistAppIdEndTimes.keyAt(i));
                printWriter.print(": ");
                Pair<MutableLong, String> pairValueAt = this.mTempWhitelistAppIdEndTimes.valueAt(i);
                TimeUtils.formatDuration(((MutableLong) pairValueAt.first).value, jElapsedRealtime, printWriter);
                printWriter.print(" - ");
                printWriter.println((String) pairValueAt.second);
            }
        }
    }
}
