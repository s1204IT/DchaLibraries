package com.mediatek.settings.deviceinfo;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import java.util.Locale;

/* loaded from: classes.dex */
public class BasebandVersion2PreferenceController extends AbstractPreferenceController implements PreferenceControllerMixin {
    public BasebandVersion2PreferenceController(Context context) {
        super(context);
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public boolean isAvailable() {
        return !Utils.isWifiOnly(this.mContext) && needShowCdmaBasebandVersion();
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public String getPreferenceKey() {
        return "baseband_version_2";
    }

    @Override // com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get("vendor.cdma.version.baseband", this.mContext.getResources().getString(R.string.device_info_default)));
        preference.setTitle(getCustomizedBasebandTitle(this.mContext, getPreferenceKey()));
    }

    public static boolean needShowCdmaBasebandVersion() {
        return RatConfiguration.isC2kSupported() && !onlyOneModem();
    }

    private static boolean onlyOneModem() {
        return SystemProperties.get("ro.vendor.mtk_ril_mode", "").contains("1rild");
    }

    public static String getCustomizedBasebandTitle(Context context, String str) throws Resources.NotFoundException {
        String string = context.getResources().getString(R.string.baseband_version);
        String country = Locale.getDefault().getCountry();
        boolean z = country.equals(Locale.CHINA.getCountry()) || country.equals(Locale.TAIWAN.getCountry());
        if (str == "baseband_version_2") {
            StringBuilder sb = new StringBuilder();
            sb.append(z ? "CDMA" : "CDMA ");
            sb.append(string);
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(z ? "GSM" : "GSM ");
        sb2.append(string);
        return sb2.toString();
    }
}
