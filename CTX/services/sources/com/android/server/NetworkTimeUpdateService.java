package com.android.server;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.NtpTrustedTime;
import android.util.TimeUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class NetworkTimeUpdateService extends Binder {
    private static final String ACTION_POLL = "com.android.server.NetworkTimeUpdateService.action.POLL";
    private static final boolean DBG = true;
    private static final int EVENT_AUTO_TIME_CHANGED = 1;
    private static final int EVENT_NETWORK_CHANGED = 3;
    private static final int EVENT_POLL_NETWORK_TIME = 2;
    private static final long NOT_SET = -1;
    private static final int POLL_REQUEST = 0;
    private static final String[] SERVERLIST = {"asia.pool.ntp.org"};
    private static final String TAG = "NetworkTimeUpdateService";
    private final AlarmManager mAlarmManager;
    private final ConnectivityManager mCM;
    private final Context mContext;
    private String mDefaultServer;
    private Handler mHandler;
    private NetworkTimeUpdateCallback mNetworkTimeUpdateCallback;
    private final PendingIntent mPendingPollIntent;
    private final long mPollingIntervalMs;
    private final long mPollingIntervalShorterMs;
    private SettingsObserver mSettingsObserver;
    private final NtpTrustedTime mTime;
    private final int mTimeErrorThresholdMs;
    private int mTryAgainCounter;
    private final int mTryAgainTimesMax;
    private final PowerManager.WakeLock mWakeLock;
    private long mNitzTimeSetTime = -1;
    private Network mDefaultNetwork = null;
    private ArrayList<String> mNtpServers = new ArrayList<>();
    private BroadcastReceiver mNitzReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(NetworkTimeUpdateService.TAG, "Received " + action);
            if ("android.intent.action.NETWORK_SET_TIME".equals(action)) {
                Log.d(NetworkTimeUpdateService.TAG, "mNitzReceiver Receive ACTION_NETWORK_SET_TIME");
                NetworkTimeUpdateService.this.mNitzTimeSetTime = SystemClock.elapsedRealtime();
            }
        }
    };

    public NetworkTimeUpdateService(Context context) {
        this.mContext = context;
        this.mTime = NtpTrustedTime.getInstance(context);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService(AlarmManager.class);
        this.mCM = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
        this.mPendingPollIntent = PendingIntent.getBroadcast(this.mContext, 0, new Intent(ACTION_POLL, (Uri) null), 0);
        this.mPollingIntervalMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureMax);
        this.mPollingIntervalShorterMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureMin);
        this.mTryAgainTimesMax = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceColorTemperatureSensorRate);
        this.mTimeErrorThresholdMs = this.mContext.getResources().getInteger(R.integer.config_displayWhiteBalanceDecreaseDebounce);
        this.mWakeLock = ((PowerManager) context.getSystemService(PowerManager.class)).newWakeLock(1, TAG);
        this.mDefaultServer = this.mTime.getServer();
        this.mNtpServers.add(this.mDefaultServer);
        for (String str : SERVERLIST) {
            this.mNtpServers.add(str);
        }
        this.mTryAgainCounter = 0;
    }

    public void systemRunning() {
        registerForTelephonyIntents();
        registerForAlarms();
        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        this.mHandler = new MyHandler(handlerThread.getLooper());
        this.mNetworkTimeUpdateCallback = new NetworkTimeUpdateCallback();
        this.mCM.registerDefaultNetworkCallback(this.mNetworkTimeUpdateCallback, this.mHandler);
        this.mSettingsObserver = new SettingsObserver(this.mHandler, 1);
        this.mSettingsObserver.observe(this.mContext);
    }

    private void registerForTelephonyIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.NETWORK_SET_TIME");
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

    private void onPollNetworkTime(int i) {
        Log.d(TAG, "onPollNetworkTime start");
        if (this.mDefaultNetwork == null) {
            return;
        }
        this.mWakeLock.acquire();
        try {
            onPollNetworkTimeUnderWakeLock(i);
        } finally {
            this.mWakeLock.release();
        }
    }

    private void onPollNetworkTimeUnderWakeLock(int i) {
        if (this.mTime.getCacheAge() >= this.mPollingIntervalMs) {
            Log.d(TAG, "Stale NTP fix; forcing refresh");
            int size = this.mTryAgainCounter % this.mNtpServers.size();
            Log.d(TAG, "mTryAgainCounter = " + this.mTryAgainCounter + ";mNtpServers.size() = " + this.mNtpServers.size() + ";index = " + size + ";mNtpServers = " + this.mNtpServers.get(size));
            if (this.mTime instanceof NtpTrustedTime) {
                this.mTime.setServer(this.mNtpServers.get(size));
                this.mTime.forceRefresh();
                this.mTime.setServer(this.mDefaultServer);
            } else {
                this.mTime.forceRefresh();
            }
        }
        if (this.mTime.getCacheAge() < this.mPollingIntervalMs) {
            resetAlarm(this.mPollingIntervalMs);
            if (isAutomaticTimeRequested()) {
                updateSystemClock(i);
                return;
            }
            return;
        }
        this.mTryAgainCounter++;
        if (this.mTryAgainTimesMax < 0 || this.mTryAgainCounter <= this.mTryAgainTimesMax) {
            resetAlarm(this.mPollingIntervalShorterMs);
        } else {
            this.mTryAgainCounter = 0;
            resetAlarm(this.mPollingIntervalMs);
        }
    }

    private long getNitzAge() {
        if (this.mNitzTimeSetTime == -1) {
            return JobStatus.NO_LATEST_RUNTIME;
        }
        return SystemClock.elapsedRealtime() - this.mNitzTimeSetTime;
    }

    private void updateSystemClock(int i) {
        if (!(i == 1)) {
            if (getNitzAge() < this.mPollingIntervalMs) {
                Log.d(TAG, "Ignoring NTP update due to recent NITZ");
                return;
            } else if (Math.abs(this.mTime.currentTimeMillis() - System.currentTimeMillis()) < this.mTimeErrorThresholdMs) {
                Log.d(TAG, "Ignoring NTP update due to low skew");
                return;
            }
        }
        SystemClock.setCurrentTimeMillis(this.mTime.currentTimeMillis());
    }

    private void resetAlarm(long j) {
        this.mAlarmManager.cancel(this.mPendingPollIntent);
        this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + j, this.mPendingPollIntent);
    }

    private boolean isAutomaticTimeRequested() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "auto_time", 0) != 0;
    }

    private class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                case 2:
                case 3:
                    Log.d(NetworkTimeUpdateService.TAG, "MyHandler::handleMessage what = " + message.what);
                    NetworkTimeUpdateService.this.onPollNetworkTime(message.what);
                    break;
            }
        }
    }

    private class NetworkTimeUpdateCallback extends ConnectivityManager.NetworkCallback {
        private NetworkTimeUpdateCallback() {
        }

        @Override
        public void onAvailable(Network network) {
            Log.d(NetworkTimeUpdateService.TAG, String.format("New default network %s; checking time.", network));
            NetworkTimeUpdateService.this.mDefaultNetwork = network;
            NetworkTimeUpdateService.this.onPollNetworkTime(3);
        }

        @Override
        public void onLost(Network network) {
            if (network.equals(NetworkTimeUpdateService.this.mDefaultNetwork)) {
                NetworkTimeUpdateService.this.mDefaultNetwork = null;
            }
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private Handler mHandler;
        private int mMsg;

        SettingsObserver(Handler handler, int i) {
            super(handler);
            this.mHandler = handler;
            this.mMsg = i;
        }

        void observe(Context context) {
            context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("auto_time"), false, this);
        }

        @Override
        public void onChange(boolean z) {
            this.mHandler.obtainMessage(this.mMsg).sendToTarget();
        }
    }

    @Override
    protected void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, printWriter)) {
            printWriter.print("PollingIntervalMs: ");
            TimeUtils.formatDuration(this.mPollingIntervalMs, printWriter);
            printWriter.print("\nPollingIntervalShorterMs: ");
            TimeUtils.formatDuration(this.mPollingIntervalShorterMs, printWriter);
            printWriter.println("\nTryAgainTimesMax: " + this.mTryAgainTimesMax);
            printWriter.print("TimeErrorThresholdMs: ");
            TimeUtils.formatDuration((long) this.mTimeErrorThresholdMs, printWriter);
            printWriter.println("\nTryAgainCounter: " + this.mTryAgainCounter);
            printWriter.println("NTP cache age: " + this.mTime.getCacheAge());
            printWriter.println("NTP cache certainty: " + this.mTime.getCacheCertainty());
            printWriter.println();
        }
    }
}
