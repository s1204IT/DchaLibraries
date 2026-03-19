package com.android.server.telecom.settings;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.provider.BlockedNumberContract;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.SpannableString;
import android.text.TextDirectionHeuristics;
import android.widget.Toast;
import com.android.server.telecom.R;
import java.util.Locale;

public final class BlockedNumbersUtil {
    public static String getLocaleDefaultToUS() {
        String country = Locale.getDefault().getCountry();
        if (country == null || country.length() != 2) {
            return "US";
        }
        return country;
    }

    public static String formatNumber(String str) {
        String number = PhoneNumberUtils.formatNumber(str, getLocaleDefaultToUS());
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (number != null) {
            str = number;
        }
        return bidiFormatter.unicodeWrap(str, TextDirectionHeuristics.LTR);
    }

    public static void showToastWithFormattedNumber(Context context, int i, String str) {
        String number = formatNumber(str);
        String string = context.getString(i, number);
        int iIndexOf = string.indexOf(number);
        SpannableString spannableString = new SpannableString(string);
        PhoneNumberUtils.addTtsSpan(spannableString, iIndexOf, number.length() + iIndexOf);
        Toast.makeText(context, spannableString, 0).show();
    }

    public static void updateEmergencyCallNotification(Context context, boolean z) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        if (!z) {
            notificationManager.cancelAsUser(null, 150, new UserHandle(0));
            return;
        }
        PendingIntent activity = PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) CallBlockDisabledActivity.class), 268435456);
        String string = context.getString(R.string.phone_strings_call_blocking_turned_off_notification_title_txt);
        String string2 = context.getString(R.string.phone_strings_call_blocking_turned_off_notification_text_txt);
        Notification notificationBuild = new Notification.Builder(context).setSmallIcon(android.R.drawable.stat_sys_warning).setTicker(string2).setContentTitle(string).setContentText(string2).setContentIntent(activity).setShowWhen(true).setChannel("TelecomCallBlocking").build();
        notificationBuild.flags |= 32;
        notificationManager.notifyAsUser(null, 150, notificationBuild, new UserHandle(0));
    }

    public static boolean isEnhancedCallBlockingEnabledByPlatform(Context context) {
        PersistableBundle config = ((CarrierConfigManager) context.getSystemService("carrier_config")).getConfig();
        if (config == null) {
            config = CarrierConfigManager.getDefaultConfig();
        }
        return config.getBoolean("support_enhanced_call_blocking_bool");
    }

    public static boolean getEnhancedBlockSetting(Context context, String str) {
        return BlockedNumberContract.SystemContract.getEnhancedBlockSetting(context, str);
    }

    public static void setEnhancedBlockSetting(Context context, String str, boolean z) {
        BlockedNumberContract.SystemContract.setEnhancedBlockSetting(context, str, z);
    }
}
