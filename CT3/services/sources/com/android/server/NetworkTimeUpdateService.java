package com.android.server;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.TimeUtils;
import android.util.TrustedTime;
import android.widget.Toast;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NetworkTimeUpdateService extends Binder {
    private static final String ACTION_POLL = "com.android.server.NetworkTimeUpdateService.action.POLL";
    private static final String BOOT_SYS_PROPERTY = "persist.sys.first_time_boot";
    private static final boolean DBG = true;
    private static final String DECRYPT_STATE = "trigger_restart_framework";
    private static final int EVENT_AUTO_TIME_CHANGED = 1;
    private static final int EVENT_GPS_TIME_SYNC_CHANGED = 4;
    private static final int EVENT_NETWORK_CHANGED = 3;
    private static final int EVENT_POLL_NETWORK_TIME = 2;
    private static final int NETWORK_CHANGE_EVENT_DELAY_MS = 1000;
    private static final long NOT_SET = -1;
    private static final String TAG = "NetworkTimeUpdateService";
    private AlarmManager mAlarmManager;
    private Context mContext;
    private String mDefaultServer;
    private Handler mGpsHandler;
    private HandlerThread mGpsThread;
    private GpsTimeSyncObserver mGpsTimeSyncObserver;
    private Thread mGpsTimerThread;
    private Handler mHandler;
    private LocationManager mLocationManager;
    private PendingIntent mPendingPollIntent;
    private final long mPollingIntervalMs;
    private final long mPollingIntervalShorterMs;
    private SettingsObserver mSettingsObserver;
    private TrustedTime mTime;
    private final int mTimeErrorThresholdMs;
    private int mTryAgainCounter;
    private final int mTryAgainTimesMax;
    private final PowerManager.WakeLock mWakeLock;
    private static int mDefaultYear = 2014;
    private static int POLL_REQUEST = 0;
    private static final String[] SERVERLIST = {"hshh.org", "time.apple.com", "time-a.nist.gov"};
    private long mNitzTimeSetTime = -1;
    private long mNitzZoneSetTime = -1;
    private long mLastNtpFetchTime = -1;
    private ArrayList<String> mNtpServers = new ArrayList<>();
    private BroadcastReceiver mNitzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(NetworkTimeUpdateService.TAG, "Received " + action);
            if ("android.intent.action.NETWORK_SET_TIME".equals(action)) {
                Log.d(NetworkTimeUpdateService.TAG, "mNitzReceiver Receive ACTION_NETWORK_SET_TIME");
                NetworkTimeUpdateService.this.mNitzTimeSetTime = SystemClock.elapsedRealtime();
            } else {
                if (!"android.intent.action.NETWORK_SET_TIMEZONE".equals(action)) {
                    return;
                }
                Log.d(NetworkTimeUpdateService.TAG, "mNitzReceiver Receive ACTION_NETWORK_SET_TIMEZONE");
                NetworkTimeUpdateService.this.mNitzZoneSetTime = SystemClock.elapsedRealtime();
            }
        }
    };
    private BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!"android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                return;
            }
            Log.d(NetworkTimeUpdateService.TAG, "Received CONNECTIVITY_ACTION ");
            Message message = NetworkTimeUpdateService.this.mHandler.obtainMessage(3);
            NetworkTimeUpdateService.this.mHandler.sendMessageDelayed(message, 1000L);
        }
    };
    private boolean mIsGpsTimeSyncRunning = false;
    private Handler mGpsToastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String timeoutMsg = (String) msg.obj;
            Toast.makeText(NetworkTimeUpdateService.this.mContext, timeoutMsg, 1).show();
        }
    };
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            NetworkTimeUpdateService.this.mGpsTimerThread.interrupt();
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public NetworkTimeUpdateService(Context context) {
        this.mContext = context;
        this.mTime = NtpTrustedTime.getInstance(context);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        Intent pollIntent = new Intent(ACTION_POLL, (Uri) null);
        this.mPendingPollIntent = PendingIntent.getBroadcast(this.mContext, POLL_REQUEST, pollIntent, 0);
        this.mPollingIntervalMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureMax);
        this.mPollingIntervalShorterMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureMin);
        this.mTryAgainTimesMax = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureSensorRate);
        this.mTimeErrorThresholdMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceDecreaseDebounce);
        this.mWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(1, TAG);
        this.mDefaultServer = this.mTime.getServer();
        this.mNtpServers.add(this.mDefaultServer);
        this.mTryAgainCounter = 0;
    }

    public void systemRunning() {
        registerForTelephonyIntents();
        registerForAlarms();
        registerForConnectivityIntents();
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new MyHandler(thread.getLooper());
        this.mHandler.obtainMessage(2).sendToTarget();
        this.mSettingsObserver = new SettingsObserver(this.mHandler, 1);
        this.mSettingsObserver.observe(this.mContext);
        Log.d(TAG, "add GPS time sync handler and looper");
        this.mGpsThread = new HandlerThread(TAG);
        this.mGpsThread.start();
        this.mGpsHandler = new MyHandler(this.mGpsThread.getLooper());
        this.mGpsTimeSyncObserver = new GpsTimeSyncObserver(this.mGpsHandler, 4);
        this.mGpsTimeSyncObserver.observe(this.mContext);
        mDefaultYear = this.mContext.getResources().getInteger(com.mediatek.internal.R.integer.default_restore_year);
        String tempString = SystemProperties.get(BOOT_SYS_PROPERTY, "");
        boolean isFirstBoot = tempString != null && "".equals(tempString);
        if (!isFirstBoot) {
            return;
        }
        String tempString2 = SystemProperties.get("ro.kernel.qemu", "");
        boolean isEmulator = "1".equals(tempString2);
        if (isEmulator) {
            Log.d(TAG, "isEmulator:" + tempString2);
            return;
        }
        String decryptState = SystemProperties.get("vold.decrypt", "");
        Log.d(TAG, "decryptState:" + decryptState);
        if (!"".equals(decryptState) && !DECRYPT_STATE.equals(decryptState)) {
            return;
        }
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        Log.d(TAG, "First boot:" + tempString2 + " with date:" + today);
        if (today.year < mDefaultYear || 2040 <= today.year) {
            today.set(1, 0, mDefaultYear);
            Log.d(TAG, "Set the year to " + mDefaultYear);
            SystemClock.setCurrentTimeMillis(today.toMillis(false));
        } else {
            Log.d(TAG, "Do not set the Defautlt Year. because System Year:" + today.year + " > Default Year:" + mDefaultYear);
        }
        SystemProperties.set(BOOT_SYS_PROPERTY, "false");
    }

    private void registerForTelephonyIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.NETWORK_SET_TIME");
        intentFilter.addAction("android.intent.action.NETWORK_SET_TIMEZONE");
        this.mContext.registerReceiver(this.mNitzReceiver, intentFilter);
    }

    private void registerForAlarms() {
        this.mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkTimeUpdateService.this.mHandler.obtainMessage(2).sendToTarget();
            }
        }, new IntentFilter(ACTION_POLL));
    }

    private void registerForConnectivityIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mConnectivityReceiver, intentFilter);
    }

    private void onPollNetworkTime(int event) {
        Log.d(TAG, "onPollNetworkTime start");
        if (isAutomaticTimeRequested()) {
            Log.d(TAG, "isAutomaticTimeRequested() = True");
            this.mWakeLock.acquire();
            try {
                onPollNetworkTimeUnderWakeLock(event);
            } finally {
                this.mWakeLock.release();
            }
        }
    }

    private void onPollNetworkTimeUnderWakeLock(int event) {
        long refTime = SystemClock.elapsedRealtime();
        Log.d(TAG, "mNitzTimeSetTime: " + this.mNitzTimeSetTime + ",refTime: " + refTime);
        if (this.mNitzTimeSetTime != -1 && refTime - this.mNitzTimeSetTime < this.mPollingIntervalMs) {
            resetAlarm(this.mPollingIntervalMs);
            return;
        }
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "System time = " + currentTime);
        if (this.mLastNtpFetchTime == -1 || refTime >= this.mLastNtpFetchTime + this.mPollingIntervalMs || event == 1) {
            Log.d(TAG, "Before Ntp fetch");
            if (this.mTime.getCacheAge() >= this.mPollingIntervalMs) {
                int index = this.mTryAgainCounter % this.mNtpServers.size();
                Log.d(TAG, "mTryAgainCounter = " + this.mTryAgainCounter + ";mNtpServers.size() = " + this.mNtpServers.size() + ";index = " + index + ";mNtpServers = " + this.mNtpServers.get(index));
                if (this.mTime instanceof NtpTrustedTime) {
                    this.mTime.setServer(this.mNtpServers.get(index));
                    this.mTime.forceRefresh();
                    this.mTime.setServer(this.mDefaultServer);
                } else {
                    this.mTime.forceRefresh();
                }
            }
            if (this.mTime.getCacheAge() >= this.mPollingIntervalMs) {
                this.mTryAgainCounter++;
                if (this.mTryAgainTimesMax < 0 || this.mTryAgainCounter <= this.mTryAgainTimesMax) {
                    resetAlarm(this.mPollingIntervalShorterMs);
                    return;
                } else {
                    this.mTryAgainCounter = 0;
                    resetAlarm(this.mPollingIntervalMs);
                    return;
                }
            }
            long ntp = this.mTime.currentTimeMillis();
            this.mTryAgainCounter = 0;
            if (Math.abs(ntp - currentTime) > this.mTimeErrorThresholdMs || this.mLastNtpFetchTime == -1) {
                if (this.mLastNtpFetchTime == -1 && Math.abs(ntp - currentTime) <= this.mTimeErrorThresholdMs) {
                    Log.d(TAG, "For initial setup, rtc = " + currentTime);
                }
                Log.d(TAG, "Ntp time to be set = " + ntp);
                if (ntp / 1000 < 2147483647L) {
                    SystemClock.setCurrentTimeMillis(ntp);
                }
            } else {
                Log.d(TAG, "Ntp time is close enough = " + ntp);
            }
            this.mLastNtpFetchTime = SystemClock.elapsedRealtime();
        }
        resetAlarm(this.mPollingIntervalMs);
    }

    private void resetAlarm(long interval) {
        this.mAlarmManager.cancel(this.mPendingPollIntent);
        long now = SystemClock.elapsedRealtime();
        long next = now + interval;
        this.mAlarmManager.setExact(3, next, this.mPendingPollIntent);
    }

    private boolean isAutomaticTimeRequested() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time", 0) != 0;
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                case 2:
                case 3:
                    Log.d(NetworkTimeUpdateService.TAG, "MyHandler::handleMessage what = " + msg.what);
                    NetworkTimeUpdateService.this.onPollNetworkTime(msg.what);
                    break;
                case 4:
                    boolean gpsTimeSyncStatus = NetworkTimeUpdateService.this.getGpsTimeSyncState();
                    Log.d(NetworkTimeUpdateService.TAG, "GPS Time sync is changed to " + gpsTimeSyncStatus);
                    NetworkTimeUpdateService.this.onGpsTimeChanged(gpsTimeSyncStatus);
                    break;
            }
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private Handler mHandler;
        private int mMsg;

        SettingsObserver(Handler handler, int msg) {
            super(handler);
            this.mHandler = handler;
            this.mMsg = msg;
        }

        void observe(Context context) {
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(Settings.Global.getUriFor("auto_time"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.mHandler.obtainMessage(this.mMsg).sendToTarget();
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            pw.println("Permission Denial: can't dump NetworkTimeUpdateService from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " without permission android.permission.DUMP");
            return;
        }
        pw.print("PollingIntervalMs: ");
        TimeUtils.formatDuration(this.mPollingIntervalMs, pw);
        pw.print("\nPollingIntervalShorterMs: ");
        TimeUtils.formatDuration(this.mPollingIntervalShorterMs, pw);
        pw.println("\nTryAgainTimesMax: " + this.mTryAgainTimesMax);
        pw.print("TimeErrorThresholdMs: ");
        TimeUtils.formatDuration(this.mTimeErrorThresholdMs, pw);
        pw.println("\nTryAgainCounter: " + this.mTryAgainCounter);
        pw.print("LastNtpFetchTime: ");
        TimeUtils.formatDuration(this.mLastNtpFetchTime, pw);
        pw.println();
    }

    private boolean getGpsTimeSyncState() {
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), "auto_time_gps") > 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private static class GpsTimeSyncObserver extends ContentObserver {
        private Handler mHandler;
        private int mMsg;

        GpsTimeSyncObserver(Handler handler, int msg) {
            super(handler);
            this.mHandler = handler;
            this.mMsg = msg;
        }

        void observe(Context context) {
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("auto_time_gps"), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.mHandler.obtainMessage(this.mMsg).sendToTarget();
        }
    }

    public void onGpsTimeChanged(boolean enable) {
        if (enable) {
            startUsingGpsWithTimeout(180000, this.mContext.getString(com.mediatek.internal.R.string.gps_time_sync_fail_str));
        } else {
            if (this.mGpsTimerThread == null) {
                return;
            }
            this.mGpsTimerThread.interrupt();
        }
    }

    public void startUsingGpsWithTimeout(final int milliseconds, final String timeoutMsg) {
        if (this.mIsGpsTimeSyncRunning) {
            Log.d(TAG, "WARNING: Gps Time Sync is already run");
            return;
        }
        this.mIsGpsTimeSyncRunning = true;
        Log.d(TAG, "start using GPS for GPS time sync timeout=" + milliseconds + " timeoutMsg=" + timeoutMsg);
        this.mLocationManager = (LocationManager) this.mContext.getSystemService("location");
        this.mLocationManager.requestLocationUpdates("gps", 1000L, 0.0f, this.mLocationListener);
        this.mGpsTimerThread = new Thread() {
            @Override
            public void run() {
                boolean isTimeout = false;
                try {
                    Thread.sleep(milliseconds);
                    isTimeout = true;
                } catch (InterruptedException e) {
                }
                Log.d(NetworkTimeUpdateService.TAG, "isTimeout=" + isTimeout);
                if (isTimeout) {
                    Message m = new Message();
                    m.obj = timeoutMsg;
                    NetworkTimeUpdateService.this.mGpsToastHandler.sendMessage(m);
                }
                NetworkTimeUpdateService.this.mLocationManager.removeUpdates(NetworkTimeUpdateService.this.mLocationListener);
                NetworkTimeUpdateService.this.mIsGpsTimeSyncRunning = false;
            }
        };
        this.mGpsTimerThread.start();
    }
}
