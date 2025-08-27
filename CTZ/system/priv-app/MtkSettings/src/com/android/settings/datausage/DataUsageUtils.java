package com.android.settings.datausage;

import android.R;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import java.util.List;

/* loaded from: classes.dex */
public final class DataUsageUtils {
    public static CharSequence formatDataUsage(Context context, long j) {
        Formatter.BytesResult bytes = Formatter.formatBytes(context.getResources(), j, 8);
        return BidiFormatter.getInstance().unicodeWrap(context.getString(R.string.config_defaultAssistantAccessComponent, bytes.value, bytes.units));
    }

    public static boolean hasEthernet(Context context) {
        long totalBytes;
        boolean zIsNetworkSupported = ConnectivityManager.from(context).isNetworkSupported(9);
        try {
            INetworkStatsSession iNetworkStatsSessionOpenSession = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats")).openSession();
            if (iNetworkStatsSessionOpenSession != null) {
                totalBytes = iNetworkStatsSessionOpenSession.getSummaryForNetwork(NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE).getTotalBytes();
                TrafficStats.closeQuietly(iNetworkStatsSessionOpenSession);
            } else {
                totalBytes = 0;
            }
            return zIsNetworkSupported && totalBytes > 0;
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean hasMobileData(Context context) {
        ConnectivityManager connectivityManagerFrom = ConnectivityManager.from(context);
        return connectivityManagerFrom != null && connectivityManagerFrom.isNetworkSupported(0);
    }

    public static boolean hasWifiRadio(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(ConnectivityManager.class);
        return connectivityManager != null && connectivityManager.isNetworkSupported(1);
    }

    public static boolean hasSim(Context context) {
        int simState = ((TelephonyManager) context.getSystemService(TelephonyManager.class)).getSimState();
        return (simState == 1 || simState == 0) ? false : true;
    }

    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager subscriptionManagerFrom = SubscriptionManager.from(context);
        if (subscriptionManagerFrom == null) {
            return -1;
        }
        SubscriptionInfo defaultDataSubscriptionInfo = subscriptionManagerFrom.getDefaultDataSubscriptionInfo();
        if (defaultDataSubscriptionInfo == null) {
            List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManagerFrom.getActiveSubscriptionInfoList();
            if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() == 0) {
                return -1;
            }
            defaultDataSubscriptionInfo = activeSubscriptionInfoList.get(0);
        }
        return defaultDataSubscriptionInfo.getSubscriptionId();
    }

    static NetworkTemplate getDefaultTemplate(Context context, int i) {
        if (hasMobileData(context) && i != -1) {
            TelephonyManager telephonyManagerFrom = TelephonyManager.from(context);
            return NetworkTemplate.normalize(NetworkTemplate.buildTemplateMobileAll(telephonyManagerFrom.getSubscriberId(i)), telephonyManagerFrom.getMergedSubscriberIds());
        }
        if (hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }
}
