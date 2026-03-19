package com.android.printservice.recommendation.plugin.hp;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import com.android.printservice.recommendation.R;
import java.util.Locale;

public class HPRecommendationPlugin extends ServiceRecommendationPlugin {
    private static String[] mSupportedDesignJet = {"HP DESIGNJET T120", "HP DESIGNJET T520", "HP DESIGNJET T930", "HP DESIGNJET T1530", "HP DESIGNJET T2530", "HP DESIGNJET T730", "HP DESIGNJET T830"};

    private boolean isPrintSupported(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String upperCase = str.toUpperCase(Locale.US);
        if (upperCase.contains("DESIGNJET")) {
            return isSupportedDesignjet(str);
        }
        if (upperCase.contains("LATEX") || upperCase.contains("SCITEX")) {
            return false;
        }
        return (upperCase.contains("PAGEWIDE") && upperCase.contains("XL")) ? false : true;
    }

    private static boolean isSupportedDesignjet(String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        String upperCase = str.toUpperCase(Locale.US);
        boolean z = false;
        for (String str2 : mSupportedDesignJet) {
            if (upperCase.contains(str2)) {
                z = true;
            }
        }
        return z;
    }

    public HPRecommendationPlugin(Context context) {
        super(context, R.string.plugin_vendor_hp, new VendorInfo(context.getResources(), R.array.known_print_vendor_info_for_hp), new String[]{"_pdl-datastream._tcp", "_ipp._tcp", "_ipps._tcp"});
    }

    @Override
    public boolean matchesCriteria(String str, NsdServiceInfo nsdServiceInfo) {
        if (!TextUtils.equals(str, this.mVendorInfo.mVendorID)) {
            return false;
        }
        String string = MDnsUtils.getString(nsdServiceInfo.getAttributes().get("pdl"));
        if ((TextUtils.equals("T", MDnsUtils.getString(nsdServiceInfo.getAttributes().get("hplfpmobileprinter"))) || isPrintSupported(MDnsUtils.getString(nsdServiceInfo.getAttributes().get("ty")))) && !TextUtils.isEmpty(string)) {
            return string.contains("application/vnd.hp-PCL") || string.contains("application/pdf") || string.contains("application/PCLm") || string.contains("image/pwg-raster");
        }
        return false;
    }
}
