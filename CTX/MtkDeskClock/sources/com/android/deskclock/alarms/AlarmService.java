package com.android.deskclock.alarms;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmService extends Service {
    public static final String ALARM_ALERT_ACTION = "com.android.deskclock.ALARM_ALERT";
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    public static final String ALARM_DONE_ACTION = "com.android.deskclock.ALARM_DONE";
    private static final String ALARM_REQUEST_SHUTDOWN_ACTION = "mediatek.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN";
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    private static final String NORMAL_SHUTDOWN_ACTION = "android.intent.action.normal.shutdown";
    public static final String PRE_SHUTDOWN_ACTION = "android.intent.action.ACTION_PRE_SHUTDOWN";
    public static final String PRIVACY_PROTECTION_CLOCK = "com.mediatek.ppl.NOTIFY_LOCK";
    public static final String STOP_ALARM_ACTION = "STOP_ALARM";
    private static boolean mStopPlayReceiverRegistered = false;
    private TelephonyManager mTelephonyManager;
    private final IBinder mBinder = new Binder();
    private boolean mIsBound = false;
    private final PhoneStateChangeListener mPhoneStateListener = new PhoneStateChangeListener();
    private boolean mIsRegistered = false;
    private AlarmInstance mCurrentAlarm = null;
    private Context mContext = null;
    private AlarmInstance mInstance = null;
    private final BroadcastReceiver mActionsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Exception {
            String action = intent.getAction();
            LogUtils.i("AlarmService received intent %s", action);
            if (AlarmService.this.mCurrentAlarm != null && AlarmService.this.mCurrentAlarm.mAlarmState == 5) {
                if (AlarmService.this.mIsBound) {
                    LogUtils.i("AlarmActivity bound; AlarmService no-op", new Object[0]);
                    return;
                }
                byte b = -1;
                int iHashCode = action.hashCode();
                if (iHashCode != -620878759) {
                    if (iHashCode == 1660414551 && action.equals(AlarmService.ALARM_DISMISS_ACTION)) {
                        b = 1;
                    }
                } else if (action.equals(AlarmService.ALARM_SNOOZE_ACTION)) {
                    b = 0;
                }
                switch (b) {
                    case 0:
                        AlarmStateManager.setSnoozeState(context, AlarmService.this.mCurrentAlarm, true);
                        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_intent);
                        break;
                    case 1:
                        AlarmStateManager.deleteInstanceAndUpdateParent(context, AlarmService.this.mCurrentAlarm);
                        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_intent);
                        break;
                }
                return;
            }
            LogUtils.i("No valid firing alarm", new Object[0]);
        }
    };
    private final BroadcastReceiver mStopPlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) throws Exception {
            LogUtils.v("AlarmService mStopPlayReceiver: " + intent.getAction(), new Object[0]);
            if (AlarmService.this.mCurrentAlarm != null) {
                AlarmStateManager.deleteInstanceAndUpdateParent(context, AlarmService.this.mCurrentAlarm);
            } else {
                LogUtils.v("mStopPlayReceiver mCurrentAlarm is null, just return", new Object[0]);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        this.mIsBound = true;
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        this.mIsBound = false;
        return super.onUnbind(intent);
    }

    public static void stopAlarm(Context context, AlarmInstance alarmInstance) {
        context.startService(AlarmInstance.createIntent(context, AlarmService.class, alarmInstance.mId).setAction(STOP_ALARM_ACTION));
    }

    private void startAlarm(AlarmInstance alarmInstance) throws Exception {
        LogUtils.v("AlarmService.start with instance: " + alarmInstance.mId, new Object[0]);
        if (this.mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, this.mCurrentAlarm);
            stopCurrentAlarm();
        }
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        this.mCurrentAlarm = alarmInstance;
        AlarmNotifications.showAlarmNotification(this, this.mCurrentAlarm);
        this.mTelephonyManager.listen(this.mPhoneStateListener.init(), 32);
        AlarmKlaxon.start(this, this.mCurrentAlarm);
        sendBroadcast(new Intent(ALARM_ALERT_ACTION));
    }

    private void stopCurrentAlarm() {
        if (this.mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop", new Object[0]);
            return;
        }
        LogUtils.v("AlarmService.stop with instance: %s", Long.valueOf(this.mCurrentAlarm.mId));
        AlarmKlaxon.stop(this);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        sendBroadcast(new Intent(ALARM_DONE_ACTION));
        stopForeground(true);
        this.mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        IntentFilter intentFilter = new IntentFilter(ALARM_SNOOZE_ACTION);
        intentFilter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(this.mActionsReceiver, intentFilter);
        this.mIsRegistered = true;
        this.mContext = this;
    }

    @Override
    public int onStartCommand(Intent intent, int i, int i2) throws Exception {
        byte b;
        LogUtils.v("AlarmService.onStartCommand() with %s", intent);
        if (intent == null) {
            return 2;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PRE_SHUTDOWN_ACTION);
        intentFilter.addAction(PRIVACY_PROTECTION_CLOCK);
        registerReceiver(this.mStopPlayReceiver, intentFilter);
        mStopPlayReceiverRegistered = true;
        long id = AlarmInstance.getId(intent.getData());
        String action = intent.getAction();
        int iHashCode = action.hashCode();
        if (iHashCode != -887322649) {
            b = (iHashCode == 377969588 && action.equals(STOP_ALARM_ACTION)) ? (byte) 1 : (byte) -1;
        } else if (action.equals(AlarmStateManager.CHANGE_STATE_ACTION)) {
            b = 0;
        }
        switch (b) {
            case 0:
                AlarmStateManager.handleIntent(this, intent);
                if (intent.getIntExtra(AlarmStateManager.ALARM_STATE_EXTRA, -1) == 5) {
                    this.mInstance = AlarmInstance.getInstance(getContentResolver(), id);
                }
                LogUtils.v("AlarmService instance[%s]", this.mInstance);
                if (this.mInstance == null) {
                    LogUtils.e("No instance found to start alarm: %d", Long.valueOf(id));
                    if (this.mCurrentAlarm != null) {
                        AlarmAlertWakeLock.releaseCpuLock();
                    }
                } else if (this.mCurrentAlarm != null && this.mCurrentAlarm.mId == this.mInstance.mId) {
                    LogUtils.e("Alarm already started for instance: %d", Long.valueOf(id));
                } else {
                    startAlarm(this.mInstance);
                }
                return 2;
            case 1:
                if (this.mCurrentAlarm != null && this.mCurrentAlarm.mId != id) {
                    LogUtils.e("Can't stop alarm for instance: %d because current alarm is: %d", Long.valueOf(id), Long.valueOf(this.mCurrentAlarm.mId));
                } else {
                    stopCurrentAlarm();
                    stopSelf();
                }
                return 2;
            default:
                return 2;
        }
    }

    @Override
    public void onDestroy() {
        LogUtils.v("AlarmService.onDestroy() called", new Object[0]);
        if (this.mCurrentAlarm != null) {
            stopCurrentAlarm();
        }
        if (mStopPlayReceiverRegistered) {
            unregisterReceiver(this.mStopPlayReceiver);
            mStopPlayReceiverRegistered = false;
        }
        if (this.mIsRegistered) {
            unregisterReceiver(this.mActionsReceiver);
            this.mIsRegistered = false;
        }
        super.onDestroy();
    }

    private final class PhoneStateChangeListener extends PhoneStateListener {
        private int mPhoneCallState;

        private PhoneStateChangeListener() {
        }

        PhoneStateChangeListener init() {
            this.mPhoneCallState = -1;
            return this;
        }

        @Override
        public void onCallStateChanged(int i, String str) {
            if (this.mPhoneCallState == -1) {
                this.mPhoneCallState = i;
            }
            if (i != 0 && i != this.mPhoneCallState) {
                AlarmService.this.startService(AlarmStateManager.createStateChangeIntent(AlarmService.this, "AlarmService", AlarmService.this.mCurrentAlarm, 6));
            }
        }
    }
}
