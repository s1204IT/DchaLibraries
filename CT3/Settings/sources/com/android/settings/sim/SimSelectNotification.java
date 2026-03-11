package com.android.settings.sim;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Settings;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.cdma.CdmaUtils;
import com.mediatek.settings.cdma.OmhEventHandler;
import com.mediatek.settings.ext.ISettingsMiscExt;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.List;

public class SimSelectNotification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean zEquals;
        if (UtilsExt.shouldDisableForAutoSanity()) {
            return;
        }
        if (TelephonyUtils.isAirplaneModeOn(context)) {
            Log.d("SimSelectNotification", "airplane mode is on, ignore!");
            return;
        }
        List<SubscriptionInfo> subs = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        int detectedType = intent.getIntExtra("simDetectStatus", 0);
        Log.d("SimSelectNotification", "sub info update, type = " + detectedType + ", subs = " + subs);
        if (detectedType == 4) {
            return;
        }
        if (subs != null && subs.size() > 1) {
            CdmaUtils.checkCdmaSimStatus(context, subs.size());
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        int numSlots = telephonyManager.getSimCount();
        boolean isInProvisioning = Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) == 0;
        if (numSlots < 2 || isInProvisioning) {
            return;
        }
        cancelNotification(context);
        String simStatus = intent.getStringExtra("ss");
        if ("ABSENT".equals(simStatus)) {
            zEquals = true;
        } else {
            zEquals = "LOADED".equals(simStatus);
        }
        if (!zEquals) {
            Log.d("SimSelectNotification", "sim state is not Absent or Loaded");
        } else {
            Log.d("SimSelectNotification", "simstatus = " + simStatus);
        }
        for (int i = 0; i < numSlots; i++) {
            int state = telephonyManager.getSimState(i);
            if (state != 1 && state != 5 && state != 0) {
                Log.d("SimSelectNotification", "All sims not in valid state yet");
            }
        }
        List<SubscriptionInfo> sil = subscriptionManager.getActiveSubscriptionInfoList();
        if (sil == null || sil.size() < 1) {
            Log.d("SimSelectNotification", "Subscription list is empty");
            return;
        }
        subscriptionManager.clearDefaultsForInactiveSubIds();
        boolean dataSelected = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean smsSelected = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultSmsSubscriptionId());
        Log.d("SimSelectNotification", "dataSelected = " + dataSelected + " smsSelected = " + dataSelected);
        if (dataSelected && smsSelected && !SystemProperties.get("ro.cmcc_light_cust_support").equals("1")) {
            Log.d("SimSelectNotification", "Data & SMS default sims are selected. No notification");
            return;
        }
        createNotification(context);
        if (!isSimDialogNeeded(context)) {
            Log.d("SimSelectNotification", "sim dialog not needed, RETURN!");
            return;
        }
        if (sil.size() == 1) {
            if (SystemProperties.get("ro.cmcc_light_cust_support").equals("1")) {
                Log.d("SimSelectNotification", "size == 1,show no notification");
                return;
            }
            Intent newIntent = new Intent(context, (Class<?>) SimDialogActivity.class);
            newIntent.addFlags(268435456);
            newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 3);
            newIntent.putExtra(SimDialogActivity.PREFERRED_SIM, sil.get(0).getSimSlotIndex());
            context.startActivity(newIntent);
            OmhEventHandler.getInstance(context).sendEmptyMessage(100);
            return;
        }
        if (dataSelected && !SystemProperties.get("ro.cmcc_light_cust_support").equals("1")) {
            return;
        }
        Intent newIntent2 = new Intent(context, (Class<?>) SimDialogActivity.class);
        newIntent2.addFlags(268435456);
        newIntent2.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
        context.startActivity(newIntent2);
        OmhEventHandler.getInstance(context).sendEmptyMessage(100);
    }

    private void createNotification(Context context) {
        Resources resources = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp).setColor(context.getColor(R.color.sim_noitification)).setContentTitle(resources.getString(R.string.sim_notification_title)).setContentText(resources.getString(R.string.sim_notification_summary));
        customizeSimDisplay(context, builder);
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

    private void customizeSimDisplay(Context context, NotificationCompat.Builder builder) {
        Resources resources = context.getResources();
        String title = resources.getString(R.string.sim_notification_title);
        String text = resources.getString(R.string.sim_notification_summary);
        ISettingsMiscExt miscExt = UtilsExt.getMiscPlugin(context);
        String title2 = miscExt.customizeSimDisplayString(title, -1);
        String text2 = miscExt.customizeSimDisplayString(text, -1);
        builder.setContentTitle(title2);
        builder.setContentText(text2);
    }

    private boolean isSimDialogNeeded(Context context) {
        return UtilsExt.getSimManagmentExtPlugin(context).isSimDialogNeeded();
    }
}
