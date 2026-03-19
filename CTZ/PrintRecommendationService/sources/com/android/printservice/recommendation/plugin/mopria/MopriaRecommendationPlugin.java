package com.android.printservice.recommendation.plugin.mopria;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.text.TextUtils;
import com.android.printservice.recommendation.R;
import com.android.printservice.recommendation.plugin.hp.MDnsUtils;
import com.android.printservice.recommendation.plugin.hp.ServiceRecommendationPlugin;
import com.android.printservice.recommendation.plugin.hp.VendorInfo;
import java.net.InetAddress;
import java.util.ArrayList;

public class MopriaRecommendationPlugin extends ServiceRecommendationPlugin {
    public MopriaRecommendationPlugin(Context context) {
        super(context, R.string.plugin_vendor_morpia, new VendorInfo(context.getResources(), R.array.known_print_vendor_info_for_mopria), new String[]{"_ipp._tcp", "_ipps._tcp"});
    }

    @Override
    public boolean matchesCriteria(String str, NsdServiceInfo nsdServiceInfo) {
        String string = MDnsUtils.getString(nsdServiceInfo.getAttributes().get("pdl"));
        return !TextUtils.isEmpty(string) && (string.contains("application/pdf") || string.contains("application/PCLm") || string.contains("image/pwg-raster"));
    }

    @Override
    public ArrayList<InetAddress> getPrinters() {
        return this.mListener.getPrinters();
    }
}
