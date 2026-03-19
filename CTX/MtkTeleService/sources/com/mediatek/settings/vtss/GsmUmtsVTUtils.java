package com.mediatek.settings.vtss;

import android.content.Intent;
import android.preference.PreferenceScreen;
import com.android.phone.CallBarringEditPreference;
import com.android.phone.CallForwardEditPreference;
import com.android.phone.R;

public class GsmUmtsVTUtils {
    public static void setServiceClass(Intent intent, int i) {
        intent.putExtra("service_class", i);
    }

    public static void setCFServiceClass(PreferenceScreen preferenceScreen, int i) {
        ((CallForwardEditPreference) preferenceScreen.findPreference("button_cfu_key")).setServiceClass(i);
        ((CallForwardEditPreference) preferenceScreen.findPreference("button_cfb_key")).setServiceClass(i);
        ((CallForwardEditPreference) preferenceScreen.findPreference("button_cfnry_key")).setServiceClass(i);
        ((CallForwardEditPreference) preferenceScreen.findPreference("button_cfnrc_key")).setServiceClass(i);
    }

    public static void setCBServiceClass(PreferenceScreen preferenceScreen, int i) {
        ((CallBarringEditPreference) preferenceScreen.findPreference("button_baoc_key")).setServiceClass(i);
        ((CallBarringEditPreference) preferenceScreen.findPreference("button_baoic_key")).setServiceClass(i);
        ((CallBarringEditPreference) preferenceScreen.findPreference("button_baoicxh_key")).setServiceClass(i);
        ((CallBarringEditPreference) preferenceScreen.findPreference("button_baic_key")).setServiceClass(i);
        ((CallBarringEditPreference) preferenceScreen.findPreference("button_baicr_key")).setServiceClass(i);
    }

    public static int getActionBarResId(int i, int i2) {
        if (i2 == 0) {
            if (i != 512) {
                return R.string.actionBarCFVoice;
            }
            return R.string.actionBarCFVideo;
        }
        if (i != 512) {
            return R.string.actionBarCBVoice;
        }
        return R.string.actionBarCBVideo;
    }
}
