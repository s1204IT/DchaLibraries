package com.mediatek.phone;

import android.os.SystemProperties;
import android.util.Log;
import com.android.phone.settings.SettingsConstants;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;

public class PhoneFeatureConstants {

    public static final class FeatureOption {
        public static boolean isMtkFemtoCellSupport() {
            boolean z = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.vendor.mtk_femto_cell_support"));
            Log.d("FeatureOption", "isMtkFemtoCellSupport(): " + z);
            return z;
        }

        public static boolean isMtkLteSupport() {
            boolean z = RatConfiguration.isLteFddSupported() || RatConfiguration.isLteTddSupported();
            Log.d("FeatureOption", "isMtkLteSupport(): " + z);
            return z;
        }

        public static boolean isMtkSvlteSupport() {
            boolean z = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.boot.opt_c2k_lte_mode"));
            Log.d("FeatureOption", "isMtkSvlteSupport(): " + z);
            return z;
        }

        public static boolean isMtkSrlteSupport() {
            boolean z = "2".equals(SystemProperties.get("ro.boot.opt_c2k_lte_mode"));
            Log.d("FeatureOption", "isMtkSrlteSupport(): " + z);
            return z;
        }

        public static boolean isMtkTddDataOnlySupport() {
            boolean z = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.vendor.mtk_tdd_data_only_support"));
            Log.d("FeatureOption", "isMtkTddDataOnlySupport(): " + z);
            return z;
        }

        public static boolean isMtkCtaSet() {
            boolean z = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("ro.vendor.mtk_cta_set"));
            Log.d("FeatureOption", "isMtkCtaSet(): " + z);
            return z;
        }

        public static boolean isMtkC2k5MSupport() {
            boolean z = RatConfiguration.isC2kSupported() && RatConfiguration.isLteFddSupported() && RatConfiguration.isLteTddSupported() && RatConfiguration.isWcdmaSupported() && RatConfiguration.isGsmSupported() && !RatConfiguration.isTdscdmaSupported();
            Log.d("FeatureOption", "isMtkC2k5M(): " + z);
            return z;
        }

        public static boolean isMtkC2k4MSupport() {
            boolean z = RatConfiguration.isC2kSupported() && RatConfiguration.isLteFddSupported() && RatConfiguration.isLteTddSupported() && RatConfiguration.isGsmSupported() && !RatConfiguration.isWcdmaSupported() && !RatConfiguration.isTdscdmaSupported();
            Log.d("FeatureOption", "isMtkC2k4M(): " + z);
            return z;
        }

        public static boolean isMtkC2k3MSupport() {
            boolean z = RatConfiguration.isC2kSupported() && RatConfiguration.isWcdmaSupported() && RatConfiguration.isGsmSupported() && !RatConfiguration.isLteFddSupported() && !RatConfiguration.isLteTddSupported() && !RatConfiguration.isTdscdmaSupported();
            Log.d("FeatureOption", "isMtkC2k3M(): " + z);
            return z;
        }
    }
}
