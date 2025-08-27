package com.android.settingslib.fuelgauge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.KeyValueListParser;
import android.util.Slog;

/* loaded from: classes.dex */
public class BatterySaverUtils {

    private static class Parameters {
        public final int endNth;
        private final Context mContext;
        public final int startNth;

        public Parameters(Context context) {
            this.mContext = context;
            String string = Settings.Global.getString(this.mContext.getContentResolver(), "low_power_mode_suggestion_params");
            KeyValueListParser keyValueListParser = new KeyValueListParser(',');
            try {
                keyValueListParser.setString(string);
            } catch (IllegalArgumentException e) {
                Slog.wtf("BatterySaverUtils", "Bad constants: " + string);
            }
            this.startNth = keyValueListParser.getInt("start_nth", 4);
            this.endNth = keyValueListParser.getInt("end_nth", 8);
        }
    }

    public static synchronized boolean setPowerSaveMode(Context context, boolean z, boolean z2) {
        ContentResolver contentResolver = context.getContentResolver();
        if (z && z2 && maybeShowBatterySaverConfirmation(context)) {
            return false;
        }
        if (z && !z2) {
            setBatterySaverConfirmationAcknowledged(context);
        }
        if (!((PowerManager) context.getSystemService(PowerManager.class)).setPowerSaveMode(z)) {
            return false;
        }
        if (z) {
            int i = Settings.Secure.getInt(contentResolver, "low_power_manual_activation_count", 0) + 1;
            Settings.Secure.putInt(contentResolver, "low_power_manual_activation_count", i);
            Parameters parameters = new Parameters(context);
            if (i >= parameters.startNth && i <= parameters.endNth && Settings.Global.getInt(contentResolver, "low_power_trigger_level", 0) == 0 && Settings.Secure.getInt(contentResolver, "suppress_auto_battery_saver_suggestion", 0) == 0) {
                showAutoBatterySaverSuggestion(context);
            }
        }
        return true;
    }

    private static boolean maybeShowBatterySaverConfirmation(Context context) {
        if (Settings.Secure.getInt(context.getContentResolver(), "low_power_warning_acknowledged", 0) != 0) {
            return false;
        }
        context.sendBroadcast(getSystemUiBroadcast("PNW.startSaverConfirmation"));
        return true;
    }

    private static void showAutoBatterySaverSuggestion(Context context) {
        context.sendBroadcast(getSystemUiBroadcast("PNW.autoSaverSuggestion"));
    }

    private static Intent getSystemUiBroadcast(String str) {
        Intent intent = new Intent(str);
        intent.setFlags(268435456);
        intent.setPackage("com.android.systemui");
        return intent;
    }

    private static void setBatterySaverConfirmationAcknowledged(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "low_power_warning_acknowledged", 1);
    }

    public static void suppressAutoBatterySaver(Context context) {
        Settings.Secure.putInt(context.getContentResolver(), "suppress_auto_battery_saver_suggestion", 1);
    }

    public static void setAutoBatterySaverTriggerLevel(Context context, int i) {
        if (i > 0) {
            suppressAutoBatterySaver(context);
        }
        Settings.Global.putInt(context.getContentResolver(), "low_power_trigger_level", i);
    }
}
