package com.android.deskclock.alarms;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.android.deskclock.AlarmAlertWakeLock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmService extends Service {
    private int mInitialCallState;
    private TelephonyManager mTelephonyManager;
    private AlarmInstance mCurrentAlarm = null;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String ignored) {
            if (state != 0 && state != AlarmService.this.mInitialCallState) {
                AlarmService.this.sendBroadcast(AlarmStateManager.createStateChangeIntent(AlarmService.this, "AlarmService", AlarmService.this.mCurrentAlarm, 6));
            }
        }
    };

    public static void startAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction("START_ALARM");
        AlarmAlertWakeLock.acquireCpuWakeLock(context);
        context.startService(intent);
    }

    public static void stopAlarm(Context context, AlarmInstance instance) {
        Intent intent = AlarmInstance.createIntent(context, AlarmService.class, instance.mId);
        intent.setAction("STOP_ALARM");
        context.startService(intent);
    }

    private void startAlarm(AlarmInstance instance) {
        LogUtils.v("AlarmService.start with instance: " + instance.mId, new Object[0]);
        if (this.mCurrentAlarm != null) {
            AlarmStateManager.setMissedState(this, this.mCurrentAlarm);
            stopCurrentAlarm();
        }
        AlarmAlertWakeLock.acquireCpuWakeLock(this);
        this.mCurrentAlarm = instance;
        AlarmNotifications.showAlarmNotification(this, this.mCurrentAlarm);
        this.mInitialCallState = this.mTelephonyManager.getCallState();
        this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        boolean inCall = this.mInitialCallState != 0;
        AlarmKlaxon.start(this, this.mCurrentAlarm, inCall);
        sendBroadcast(new Intent("com.android.deskclock.ALARM_ALERT"));
    }

    private void stopCurrentAlarm() {
        if (this.mCurrentAlarm == null) {
            LogUtils.v("There is no current alarm to stop", new Object[0]);
            return;
        }
        LogUtils.v("AlarmService.stop with instance: " + this.mCurrentAlarm.mId, new Object[0]);
        AlarmKlaxon.stop(this);
        this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        sendBroadcast(new Intent("com.android.deskclock.ALARM_DONE"));
        this.mCurrentAlarm = null;
        AlarmAlertWakeLock.releaseCpuLock();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.v("AlarmService.onStartCommand() with intent: " + intent.toString(), new Object[0]);
        long instanceId = AlarmInstance.getId(intent.getData());
        if ("START_ALARM".equals(intent.getAction())) {
            ContentResolver cr = getContentResolver();
            AlarmInstance instance = AlarmInstance.getInstance(cr, instanceId);
            if (instance == null) {
                LogUtils.e("No instance found to start alarm: " + instanceId, new Object[0]);
                if (this.mCurrentAlarm != null) {
                    AlarmAlertWakeLock.releaseCpuLock();
                }
            } else if (this.mCurrentAlarm != null && this.mCurrentAlarm.mId == instanceId) {
                LogUtils.e("Alarm already started for instance: " + instanceId, new Object[0]);
            } else {
                startAlarm(instance);
            }
        } else if ("STOP_ALARM".equals(intent.getAction())) {
            if (this.mCurrentAlarm != null && this.mCurrentAlarm.mId != instanceId) {
                LogUtils.e("Can't stop alarm for instance: " + instanceId + " because current alarm is: " + this.mCurrentAlarm.mId, new Object[0]);
            } else {
                stopSelf();
            }
        }
        return 2;
    }

    @Override
    public void onDestroy() {
        LogUtils.v("AlarmService.onDestroy() called", new Object[0]);
        super.onDestroy();
        stopCurrentAlarm();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
