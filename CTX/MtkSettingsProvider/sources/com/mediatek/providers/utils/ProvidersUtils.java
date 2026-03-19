package com.mediatek.providers.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.providers.settings.R;
import com.mediatek.provider.MtkSettingsExt;
import com.mediatek.providers.settings.ext.IDatabaseHelperExt;
import com.mediatek.providers.settings.ext.OpSettingsProviderCustomizationFactoryBase;

public class ProvidersUtils {
    private Context mContext;
    private IDatabaseHelperExt mDatebaseHelpExt;
    private Resources mRes;

    public ProvidersUtils(Context context) {
        this.mContext = context;
        this.mRes = this.mContext.getResources();
        initDatabaseHelperPlgin(this.mContext);
    }

    private void initDatabaseHelperPlgin(Context context) {
        this.mDatebaseHelpExt = OpSettingsProviderCustomizationFactoryBase.getOpFactory(context).makeDatabaseHelp(context);
    }

    public void loadCustomSystemSettings(SQLiteStatement sQLiteStatement) {
        Log.d("ProvidersUtils", "loadCustomSystemSettings");
        loadIntegerSetting(sQLiteStatement, "background_power_saving_enable", R.integer.def_bg_power_saving);
        loadIntegerSetting(sQLiteStatement, MtkSettingsExt.System.VOICE_WAKEUP_MODE, R.integer.def_voice_wakeup_mode);
    }

    public void loadCustomGlobalSettings(SQLiteStatement sQLiteStatement) {
        int i;
        Log.d("ProvidersUtils", "loadCustomGlobalSettings");
        loadSetting(sQLiteStatement, "telephony_misc_feature_config", getIntegerValue("telephony_misc_feature_config", R.integer.def_telephony_misc_feature_config));
        loadBooleanSetting(sQLiteStatement, "auto_time_gps", R.bool.def_auto_time_gps);
        loadSetting(sQLiteStatement, "install_non_market_apps", getBooleanValue("install_non_market_apps", R.bool.def_install_non_market_apps));
        String str = SystemProperties.get("persist.radio.multisim.config");
        if (str.equals("dsds") || str.equals("dsda")) {
            i = R.integer.def_dual_sim_mode;
        } else if (str.equals("tsts")) {
            i = R.integer.def_triple_sim_mode;
        } else if (str.equals("fsfs")) {
            i = R.integer.def_four_sim_mode;
        } else {
            i = R.integer.def_single_sim_mode;
        }
        loadIntegerSetting(sQLiteStatement, "msim_mode_setting", i);
        loadBooleanSetting(sQLiteStatement, "data_service_enabled", R.bool.def_data_service_enabled);
        loadIntegerSetting(sQLiteStatement, "show_first_crash_dialog", R.integer.def_show_fisrtcrash_dlg);
        loadStringSetting(sQLiteStatement, "private_dns_default_mode", R.string.def_private_dns_default_mode);
    }

    public void loadCustomSecureSettings(SQLiteStatement sQLiteStatement) {
        Log.d("ProvidersUtils", "loadCustomSecureSettings");
    }

    private void loadSetting(SQLiteStatement sQLiteStatement, String str, Object obj) {
        sQLiteStatement.bindString(1, str);
        sQLiteStatement.bindString(2, obj.toString());
        sQLiteStatement.execute();
    }

    private void loadStringSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, this.mRes.getString(i));
    }

    private void loadBooleanSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, this.mRes.getBoolean(i) ? "1" : "0");
    }

    private void loadIntegerSetting(SQLiteStatement sQLiteStatement, String str, int i) {
        loadSetting(sQLiteStatement, str, Integer.toString(this.mRes.getInteger(i)));
    }

    public String getBooleanValue(String str, int i) {
        return this.mDatebaseHelpExt.getResBoolean(this.mContext, str, this.mRes.getBoolean(i) ? "1" : "0");
    }

    public String getStringValue(String str, int i) {
        return this.mDatebaseHelpExt.getResStr(this.mContext, str, this.mRes.getString(i));
    }

    public String getIntegerValue(String str, int i) {
        return this.mDatebaseHelpExt.getResInteger(this.mContext, str, Integer.toString(this.mRes.getInteger(i)));
    }

    public void loadNewOperatorSettings() {
        Log.v("OperatorConfigChangedReceiver", "loadNewOperatorSettings");
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Settings.Secure.putString(contentResolver, "location_providers_allowed", getStringValue("location_providers_allowed", R.string.def_location_providers_allowed));
        Settings.System.putString(contentResolver, "haptic_feedback_enabled", getBooleanValue("haptic_feedback_enabled", R.bool.def_haptic_feedback));
        Settings.Global.putString(contentResolver, "auto_time", getBooleanValue("auto_time", R.bool.def_auto_time));
        Settings.Global.putString(contentResolver, "auto_time_zone", getBooleanValue("auto_time_zone", R.bool.def_auto_time_zone));
        Settings.Global.putString(contentResolver, "install_non_market_apps", getBooleanValue("install_non_market_apps", R.bool.def_install_non_market_apps));
        Settings.Global.putString(contentResolver, "telephony_misc_feature_config", getIntegerValue("telephony_misc_feature_config", R.integer.def_telephony_misc_feature_config));
    }
}
