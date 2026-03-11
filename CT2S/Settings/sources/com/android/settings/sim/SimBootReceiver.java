package com.android.settings.sim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import java.util.List;

public class SimBootReceiver extends BroadcastReceiver {
    private Context mContext;
    private SharedPreferences mSharedPreferences = null;
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            SimBootReceiver.this.detectChangeAndNotify();
        }
    };
    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(this.mContext);
        this.mSharedPreferences = this.mContext.getSharedPreferences("sim_state", 0);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
    }

    public void detectChangeAndNotify() {
        List<SubscriptionInfo> sil;
        int numSlots = this.mTelephonyManager.getSimCount();
        boolean isInProvisioning = Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) == 0;
        boolean notificationSent = false;
        int numSIMsDetected = 0;
        if (numSlots >= 2 && !isInProvisioning && (sil = this.mSubscriptionManager.getActiveSubscriptionInfoList()) != null && sil.size() >= 1) {
            for (int i = 0; i < numSlots; i++) {
                SubscriptionInfo sir = Utils.findRecordBySlotId(this.mContext, i);
                String key = "sim_slot_" + i;
                int lastSubId = getLastSubId(key);
                if (sir != null) {
                    numSIMsDetected++;
                    int currentSubId = sir.getSubscriptionId();
                    if (lastSubId == -2 || lastSubId != currentSubId) {
                        createNotification(this.mContext);
                        setLastSubId(key, currentSubId);
                        notificationSent = true;
                    }
                } else if (lastSubId != -1) {
                    createNotification(this.mContext);
                    setLastSubId(key, -1);
                    notificationSent = true;
                }
            }
            if (notificationSent) {
                Intent intent = new Intent();
                intent.putExtra("numSIMsDetected", numSIMsDetected);
                intent.setClassName("com.marvell.telephony", "com.marvell.telephony.SimController");
                this.mContext.startService(intent);
            }
        }
    }

    private int getLastSubId(String strSlotId) {
        return this.mSharedPreferences.getInt(strSlotId, -2);
    }

    private void setLastSubId(String strSlotId, int value) {
        SharedPreferences.Editor editor = this.mSharedPreferences.edit();
        editor.putInt(strSlotId, value);
        editor.commit();
    }

    private void createNotification(Context context) {
        Resources resources = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp).setColor(resources.getColor(R.color.sim_noitification)).setContentTitle(resources.getString(R.string.sim_notification_title)).setContentText(resources.getString(R.string.sim_notification_summary));
        Intent resultIntent = new Intent(context, (Class<?>) Settings.SimSettingsActivity.class);
        resultIntent.addFlags(268435456);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, 268435456);
        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.notify(1, builder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.cancel(1);
    }
}
