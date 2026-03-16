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
import com.android.internal.telephony.PhoneFactory;

public class EmergencyCallbackModeService extends Service {
    private NotificationManager mNotificationManager = null;
    private CountDownTimer mTimer = null;
    private long mTimeLeft = 0;
    private Phone mPhone = null;
    private boolean mInEmergencyCall = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    EmergencyCallbackModeService.this.resetEcmTimer((AsyncResult) msg.obj);
                    break;
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
            } else if (intent.getAction().equals("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS")) {
                context.startActivity(new Intent("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS").setFlags(268435456));
            }
        }
    };
    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        if (PhoneFactory.getDefaultPhone().getPhoneType() != 2 && PhoneFactory.getDefaultPhone().getImsPhone() == null) {
            Log.e("EmergencyCallbackModeService", "Error! Emergency Callback Mode not supported for " + PhoneFactory.getDefaultPhone().getPhoneName() + " phones");
            stopSelf();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        filter.addAction("android.intent.action.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS");
        registerReceiver(this.mEcmReceiver, filter);
        this.mNotificationManager = (NotificationManager) getSystemService("notification");
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mPhone.registerForEcmTimerReset(this.mHandler, 1, (Object) null);
        startTimerNotification();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(this.mEcmReceiver);
        this.mPhone.unregisterForEcmTimerReset(this.mHandler);
        this.mNotificationManager.cancel(R.string.phone_in_ecm_notification_title);
        this.mTimer.cancel();
    }

    private void startTimerNotification() {
        long ecmTimeout = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
        showNotification(ecmTimeout);
        if (this.mTimer != null) {
            this.mTimer.cancel();
        } else {
            this.mTimer = new CountDownTimer(ecmTimeout, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    EmergencyCallbackModeService.this.mTimeLeft = millisUntilFinished;
                    EmergencyCallbackModeService.this.showNotification(millisUntilFinished);
                }

                @Override
                public void onFinish() {
                }
            };
        }
        this.mTimer.start();
    }

    private void showNotification(long millisUntilFinished) {
        String text;
        boolean isInEcm = Boolean.parseBoolean(SystemProperties.get("ril.cdma.inecmmode"));
        if (!isInEcm) {
            Log.i("EmergencyCallbackModeService", "Asked to show notification but not in ECM mode");
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
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent("com.android.phone.action.ACTION_SHOW_ECM_EXIT_DIALOG"), 0);
        builder.setContentIntent(contentIntent);
        if (this.mInEmergencyCall) {
            text = getText(R.string.phone_in_ecm_call_notification_text).toString();
        } else {
            int minutes = (int) (millisUntilFinished / 60000);
            String time = String.format("%d:%02d", Integer.valueOf(minutes), Long.valueOf((millisUntilFinished % 60000) / 1000));
            text = String.format(getResources().getQuantityText(R.plurals.phone_in_ecm_notification_time, minutes).toString(), time);
        }
        builder.setContentText(text);
        this.mNotificationManager.notify(R.string.phone_in_ecm_notification_title, builder.build());
    }

    private void resetEcmTimer(AsyncResult r) {
        boolean isTimerCanceled = ((Boolean) r.result).booleanValue();
        if (isTimerCanceled) {
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
