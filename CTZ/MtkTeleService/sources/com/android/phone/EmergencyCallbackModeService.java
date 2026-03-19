package com.android.phone;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.telephony.Phone;
import java.text.SimpleDateFormat;

public class EmergencyCallbackModeService extends Service {
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    private static final int ECM_TIMER_RESET = 1;
    private static final String LOG_TAG = "EmergencyCallbackModeService";
    private NotificationManager mNotificationManager = null;
    private CountDownTimer mTimer = null;
    private long mTimeLeft = 0;
    private Phone mPhone = null;
    private boolean mInEmergencyCall = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                EmergencyCallbackModeService.this.resetEcmTimer((AsyncResult) message.obj);
            }
        }
    };
    private BroadcastReceiver mEcmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                if (!intent.getBooleanExtra("phoneinECMState", false)) {
                    EmergencyCallbackModeService.this.stopSelf();
                }
            } else if (intent.getAction().equals("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS")) {
                context.startActivity(new Intent("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS").setFlags(268435456));
            }
        }
    };
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        Phone phoneInEcm = PhoneGlobals.getInstance().getPhoneInEcm();
        if (phoneInEcm == null) {
            Log.e(LOG_TAG, "Error! Emergency Callback Mode not supported for " + phoneInEcm);
            stopSelf();
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intentFilter.addAction("com.android.internal.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS");
        registerReceiver(this.mEcmReceiver, intentFilter);
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mPhone = phoneInEcm;
        this.mPhone.registerForEcmTimerReset(this.mHandler, 1, (Object) null);
        startTimerNotification();
    }

    @Override
    public void onDestroy() {
        if (this.mPhone != null) {
            unregisterReceiver(this.mEcmReceiver);
            this.mPhone.unregisterForEcmTimerReset(this.mHandler);
            this.mTimer.cancel();
        }
        if (this.mNotificationManager == null) {
            this.mNotificationManager = (NotificationManager) getSystemService("notification");
        }
        this.mNotificationManager.cancel(R.string.phone_in_ecm_notification_title);
    }

    private void startTimerNotification() {
        long j = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
        showNotification(j);
        if (this.mTimer != null) {
            this.mTimer.cancel();
        } else {
            this.mTimer = new CountDownTimer(j, 1000L) {
                @Override
                public void onTick(long j2) {
                    EmergencyCallbackModeService.this.mTimeLeft = j2;
                }

                @Override
                public void onFinish() {
                }
            };
        }
        this.mTimer.start();
    }

    private void showNotification(long j) {
        String string;
        Phone imsPhone = this.mPhone.getImsPhone();
        if (!(this.mPhone.isInEcm() || (imsPhone != null && imsPhone.isInEcm()))) {
            Log.i(LOG_TAG, "Asked to show notification but not in ECM mode");
            if (this.mTimer != null) {
                this.mTimer.cancel();
                return;
            }
            return;
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext());
        builder.setOngoing(true);
        builder.setPriority(1);
        builder.setSmallIcon(R.drawable.ic_emergency_callback_mode);
        builder.setTicker(getText(R.string.phone_entered_ecm_text));
        builder.setContentTitle(getText(R.string.phone_in_ecm_notification_title));
        builder.setColor(getResources().getColor(R.color.dialer_theme_color));
        Intent intent = new Intent(this, (Class<?>) EmergencyCallbackModeExitDialog.class);
        intent.setAction("com.android.phone.action.ACTION_SHOW_ECM_EXIT_DIALOG");
        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 67108864));
        if (this.mInEmergencyCall) {
            string = getText(R.string.phone_in_ecm_call_notification_text).toString();
        } else {
            long jCurrentTimeMillis = j + System.currentTimeMillis();
            builder.setShowWhen(true);
            builder.setChronometerCountDown(true);
            builder.setUsesChronometer(true);
            builder.setWhen(jCurrentTimeMillis);
            string = getResources().getString(R.string.phone_in_ecm_notification_complete_time, SimpleDateFormat.getTimeInstance(3).format(Long.valueOf(jCurrentTimeMillis)));
        }
        builder.setContentText(string);
        builder.setChannelId("alert");
        this.mNotificationManager.notify(R.string.phone_in_ecm_notification_title, builder.build());
    }

    private void resetEcmTimer(AsyncResult asyncResult) {
        if (((Boolean) asyncResult.result).booleanValue()) {
            this.mInEmergencyCall = true;
            this.mTimer.cancel();
            showNotification(0L);
        } else {
            this.mInEmergencyCall = false;
            startTimerNotification();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    public class LocalBinder extends Binder {
        public LocalBinder() {
        }

        EmergencyCallbackModeService getService() {
            return EmergencyCallbackModeService.this;
        }
    }

    public long getEmergencyCallbackModeTimeout() {
        return this.mTimeLeft;
    }

    public boolean getEmergencyCallbackModeCallState() {
        return this.mInEmergencyCall;
    }
}
