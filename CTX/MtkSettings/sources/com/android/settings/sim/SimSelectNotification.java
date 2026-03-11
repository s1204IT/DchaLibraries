package com.android.settings.sim;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.support.v4.app.NotificationCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.sim.TelephonyUtils;
import java.util.List;

public class SimSelectNotification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int size;
        Log.d("SimSelectNotification", "onReceive, action=" + intent.getAction());
        UtilsExt.getSimManagementExt(context).customBroadcast(intent);
        if (UtilsExt.shouldDisableForAutoSanity()) {
            Log.d("SimSelectNotification", "disable for auto sanity.");
            return;
        }
        if (TelephonyUtils.isAirplaneModeOn(context)) {
            Log.d("SimSelectNotification", "airplane mode is on, ignore.");
            return;
        }
        if (SystemProperties.getInt("ro.vendor.mtk_external_sim_support", 0) == 1 && SystemProperties.getInt("vendor.gsm.disable.sim.dialog", 0) == 1) {
            Log.d("SimSelectNotification", "RSIM present, ignore.");
            return;
        }
        List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
        int intExtra = intent.getIntExtra("simDetectStatus", 0);
        Log.d("SimSelectNotification", "sub info update, type=" + intExtra + ", subs=" + activeSubscriptionInfoList);
        if (intExtra == 4) {
            Log.d("SimSelectNotification", "extra value no change, return.");
            return;
        }
        if (activeSubscriptionInfoList == null) {
            size = 0;
        } else {
            size = activeSubscriptionInfoList.size();
        }
        int intExtra2 = intent.getIntExtra("simCount", size);
        if (activeSubscriptionInfoList != null && intExtra2 != activeSubscriptionInfoList.size()) {
            Log.d("SimSelectNotification", "SIM count is changed again, extraSimCount=" + intExtra2 + ", currentSimCount=" + activeSubscriptionInfoList.size());
            return;
        }
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(context);
        int simCount = telephonyManager.getSimCount();
        boolean z = !Utils.isDeviceProvisioned(context);
        Log.d("SimSelectNotification", "numSlots=" + simCount + ", isInProvisioning=" + z);
        if (simCount < 2 || z) {
            return;
        }
        cancelNotification(context);
        List<SubscriptionInfo> activeSubscriptionInfoList2 = subscriptionManagerFrom.getActiveSubscriptionInfoList();
        if (activeSubscriptionInfoList2 == null || activeSubscriptionInfoList2.size() < 1) {
            Log.d("SimSelectNotification", "Subscription list is empty");
            return;
        }
        subscriptionManagerFrom.clearDefaultsForInactiveSubIds();
        boolean zIsUsableSubIdValue = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean zIsUsableSubIdValue2 = SubscriptionManager.isUsableSubIdValue(SubscriptionManager.getDefaultSmsSubscriptionId());
        Log.d("SimSelectNotification", "dataSelected=" + zIsUsableSubIdValue + ", smsSelected=" + zIsUsableSubIdValue2);
        if (zIsUsableSubIdValue && zIsUsableSubIdValue2) {
            Log.d("SimSelectNotification", "Data & SMS default sims are selected. No notification");
            return;
        }
        createNotification(context);
        if (!UtilsExt.getSimManagementExt(context).isSimDialogNeeded()) {
            Log.d("SimSelectNotification", "sim dialog not needed, return.");
            return;
        }
        if (activeSubscriptionInfoList2.size() == 1) {
            Log.d("SimSelectNotification", "sim size == 1, SimDialogActivity shown.");
            Intent intent2 = new Intent(context, (Class<?>) SimDialogActivity.class);
            intent2.addFlags(402653184);
            intent2.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 3);
            intent2.putExtra(SimDialogActivity.PREFERRED_SIM, activeSubscriptionInfoList2.get(0).getSimSlotIndex());
            context.startActivity(intent2);
            return;
        }
        if (!zIsUsableSubIdValue) {
            Log.d("SimSelectNotification", "SimDialogActivity shown for multiple sims.");
            Intent intent3 = new Intent(context, (Class<?>) SimDialogActivity.class);
            intent3.addFlags(402653184);
            intent3.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, 0);
            context.startActivity(intent3);
        }
    }

    private void createNotification(Context context) {
        Resources resources = context.getResources();
        NotificationChannel notificationChannel = new NotificationChannel("sim_select_notification_channel", resources.getString(R.string.sim_selection_channel_title), 2);
        NotificationCompat.Builder contentText = new NotificationCompat.Builder(context, "sim_select_notification_channel").setSmallIcon(R.drawable.ic_sim_card_alert_white_48dp).setColor(context.getColor(R.color.sim_noitification)).setContentTitle(resources.getString(R.string.sim_notification_title)).setContentText(resources.getString(R.string.sim_notification_summary));
        Intent intent = new Intent(context, (Class<?>) Settings.SimSettingsActivity.class);
        intent.addFlags(268435456);
        contentText.setContentIntent(PendingIntent.getActivity(context, 0, intent, 268435456));
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.notify(1, contentText.build());
    }

    public static void cancelNotification(Context context) {
        ((NotificationManager) context.getSystemService("notification")).cancel(1);
    }
}
