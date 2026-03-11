package com.android.settingslib.development;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.R;

public abstract class AbstractLogdSizePreferenceController extends DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener {
    static final String DEFAULT_SNET_TAG = "I";
    static final String LOW_RAM_CONFIG_PROPERTY_KEY = "ro.config.low_ram";
    static final String SELECT_LOGD_DEFAULT_SIZE_VALUE = "262144";
    static final String SELECT_LOGD_MINIMUM_SIZE_VALUE = "65536";
    static final String SELECT_LOGD_SIZE_PROPERTY = "persist.logd.size";
    static final String SELECT_LOGD_SNET_TAG_PROPERTY = "persist.log.tag.snet_event_log";
    private ListPreference mLogdSize;

    public AbstractLogdSizePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return "select_logd_size";
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        if (isAvailable()) {
            this.mLogdSize = (ListPreference) preferenceScreen.findPreference("select_logd_size");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == this.mLogdSize) {
            writeLogdSizeOption(obj);
            return true;
        }
        return false;
    }

    private String defaultLogdSizeValue() {
        String str = SystemProperties.get("ro.logd.size");
        if (str == null || str.length() == 0) {
            if (SystemProperties.get(LOW_RAM_CONFIG_PROPERTY_KEY).equals("true")) {
                return SELECT_LOGD_MINIMUM_SIZE_VALUE;
            }
            return SELECT_LOGD_DEFAULT_SIZE_VALUE;
        }
        return str;
    }

    public void updateLogdSizeValues() {
        if (this.mLogdSize != null) {
            String str = SystemProperties.get("persist.log.tag");
            String strDefaultLogdSizeValue = SystemProperties.get(SELECT_LOGD_SIZE_PROPERTY);
            if (str != null && str.startsWith("Settings")) {
                strDefaultLogdSizeValue = "32768";
            }
            LocalBroadcastManager.getInstance(this.mContext).sendBroadcastSync(new Intent("com.android.settingslib.development.AbstractLogdSizePreferenceController.LOGD_SIZE_UPDATED").putExtra("CURRENT_LOGD_VALUE", strDefaultLogdSizeValue));
            if (strDefaultLogdSizeValue == null || strDefaultLogdSizeValue.length() == 0) {
                strDefaultLogdSizeValue = defaultLogdSizeValue();
            }
            String[] stringArray = this.mContext.getResources().getStringArray(R.array.select_logd_size_values);
            String[] stringArray2 = this.mContext.getResources().getStringArray(R.array.select_logd_size_titles);
            int i = 2;
            if (SystemProperties.get(LOW_RAM_CONFIG_PROPERTY_KEY).equals("true")) {
                this.mLogdSize.setEntries(R.array.select_logd_size_lowram_titles);
                stringArray2 = this.mContext.getResources().getStringArray(R.array.select_logd_size_lowram_titles);
                i = 1;
            }
            String[] stringArray3 = this.mContext.getResources().getStringArray(R.array.select_logd_size_summaries);
            for (int i2 = 0; i2 < stringArray2.length; i2++) {
                if (strDefaultLogdSizeValue.equals(stringArray[i2]) || strDefaultLogdSizeValue.equals(stringArray2[i2])) {
                    i = i2;
                    break;
                }
            }
            this.mLogdSize.setValue(stringArray[i]);
            this.mLogdSize.setSummary(stringArray3[i]);
        }
    }

    public void writeLogdSizeOption(Object obj) {
        String string;
        String str;
        boolean z = obj != null && obj.toString().equals("32768");
        String str2 = SystemProperties.get("persist.log.tag");
        if (str2 == null) {
            str2 = "";
        }
        String strReplaceFirst = str2.replaceAll(",+Settings", "").replaceFirst("^Settings,*", "").replaceAll(",+", ",").replaceFirst(",+$", "");
        if (z) {
            obj = SELECT_LOGD_MINIMUM_SIZE_VALUE;
            String str3 = SystemProperties.get(SELECT_LOGD_SNET_TAG_PROPERTY);
            if ((str3 == null || str3.length() == 0) && ((str = SystemProperties.get("log.tag.snet_event_log")) == null || str.length() == 0)) {
                SystemProperties.set(SELECT_LOGD_SNET_TAG_PROPERTY, DEFAULT_SNET_TAG);
            }
            if (strReplaceFirst.length() != 0) {
                strReplaceFirst = "," + strReplaceFirst;
            }
            strReplaceFirst = "Settings" + strReplaceFirst;
        }
        if (!strReplaceFirst.equals(str2)) {
            SystemProperties.set("persist.log.tag", strReplaceFirst);
        }
        String strDefaultLogdSizeValue = defaultLogdSizeValue();
        if (obj != null && obj.toString().length() != 0) {
            string = obj.toString();
        } else {
            string = strDefaultLogdSizeValue;
        }
        if (strDefaultLogdSizeValue.equals(string)) {
            string = "";
        }
        SystemProperties.set(SELECT_LOGD_SIZE_PROPERTY, string);
        SystemProperties.set("ctl.start", "logd-reinit");
        SystemPropPoker.getInstance().poke();
        updateLogdSizeValues();
    }
}
