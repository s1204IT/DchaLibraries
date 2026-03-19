package com.android.printservice.recommendation.plugin.google;

import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.util.ArrayMap;
import com.android.printservice.recommendation.PrintServicePlugin;
import com.android.printservice.recommendation.R;
import com.android.printservice.recommendation.util.MDNSFilteredDiscovery;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CloudPrintPlugin implements PrintServicePlugin {
    private final MDNSFilteredDiscovery mMDNSFilteredDiscovery;
    private static final String LOG_TAG = CloudPrintPlugin.class.getSimpleName();
    private static final Set<String> PRINTER_SERVICE_TYPE = new HashSet<String>() {
        {
            add("_privet._tcp");
        }
    };
    private static final Set<String> POSSIBLE_CONNECTION_STATES = new HashSet<String>() {
        {
            add("online");
            add("offline");
            add("connecting");
            add("not-configured");
        }
    };

    public CloudPrintPlugin(Context context) {
        this.mMDNSFilteredDiscovery = new MDNSFilteredDiscovery(context, PRINTER_SERVICE_TYPE, new MDNSFilteredDiscovery.PrinterFilter() {
            @Override
            public final boolean matchesCriteria(NsdServiceInfo nsdServiceInfo) {
                return CloudPrintPlugin.lambda$new$0(nsdServiceInfo);
            }
        });
    }

    static boolean lambda$new$0(NsdServiceInfo nsdServiceInfo) {
        byte[] bArr;
        byte[] bArr2;
        byte[] bArr3;
        ArrayMap arrayMap = new ArrayMap(nsdServiceInfo.getAttributes().size());
        for (Map.Entry<String, byte[]> entry : nsdServiceInfo.getAttributes().entrySet()) {
            arrayMap.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        byte[] bArr4 = (byte[]) arrayMap.get("txtvers");
        return (bArr4 == null || bArr4.length != 1 || bArr4[0] != 49 || arrayMap.get("ty") == null || (bArr = (byte[]) arrayMap.get("url")) == null || bArr.length == 0 || (bArr2 = (byte[]) arrayMap.get("type")) == null || !"printer".equals(new String(bArr2, StandardCharsets.UTF_8).toLowerCase()) || arrayMap.get("id") == null || (bArr3 = (byte[]) arrayMap.get("cs")) == null || !POSSIBLE_CONNECTION_STATES.contains(new String(bArr3, StandardCharsets.UTF_8).toLowerCase()) || !(nsdServiceInfo.getHost() instanceof Inet4Address)) ? false : true;
    }

    @Override
    public CharSequence getPackageName() {
        return "com.google.android.apps.cloudprint";
    }

    @Override
    public void start(PrintServicePlugin.PrinterDiscoveryCallback printerDiscoveryCallback) throws Exception {
        this.mMDNSFilteredDiscovery.start(printerDiscoveryCallback);
    }

    @Override
    public int getName() {
        return R.string.plugin_vendor_google_cloud_print;
    }

    @Override
    public void stop() throws Exception {
        this.mMDNSFilteredDiscovery.stop();
    }
}
