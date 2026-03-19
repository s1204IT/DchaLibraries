package com.android.server.connectivity;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkUtils;
import android.net.Uri;
import android.net.dns.ResolvUtil;
import android.os.Binder;
import android.os.INetworkManagementService;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Pair;
import android.util.Slog;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.connectivity.DnsManager;
import com.android.server.slice.SliceClientPermissions;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DnsManager {
    private static final int DNS_RESOLVER_DEFAULT_MAX_SAMPLES = 64;
    private static final int DNS_RESOLVER_DEFAULT_MIN_SAMPLES = 8;
    private static final int DNS_RESOLVER_DEFAULT_SAMPLE_VALIDITY_SECONDS = 1800;
    private static final int DNS_RESOLVER_DEFAULT_SUCCESS_THRESHOLD_PERCENT = 25;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mMaxSamples;
    private int mMinSamples;
    private final INetworkManagementService mNMS;
    private int mNumDnsEntries;
    private String mPrivateDnsMode;
    private String mPrivateDnsSpecifier;
    private int mSampleValidity;
    private int mSuccessThreshold;
    private final MockableSystemProperties mSystemProperties;
    private static final String TAG = DnsManager.class.getSimpleName();
    private static final PrivateDnsConfig PRIVATE_DNS_OFF = new PrivateDnsConfig();
    private final Map<Integer, PrivateDnsConfig> mPrivateDnsMap = new HashMap();
    private final Map<Integer, PrivateDnsValidationStatuses> mPrivateDnsValidationMap = new HashMap();

    public static class PrivateDnsConfig {
        public final String hostname;
        public final InetAddress[] ips;
        public final boolean useTls;

        public PrivateDnsConfig() {
            this(false);
        }

        public PrivateDnsConfig(boolean z) {
            this.useTls = z;
            this.hostname = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.ips = new InetAddress[0];
        }

        public PrivateDnsConfig(String str, InetAddress[] inetAddressArr) {
            this.useTls = !TextUtils.isEmpty(str);
            this.hostname = this.useTls ? str : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.ips = inetAddressArr == null ? new InetAddress[0] : inetAddressArr;
        }

        public PrivateDnsConfig(PrivateDnsConfig privateDnsConfig) {
            this.useTls = privateDnsConfig.useTls;
            this.hostname = privateDnsConfig.hostname;
            this.ips = privateDnsConfig.ips;
        }

        public boolean inStrictMode() {
            return this.useTls && !TextUtils.isEmpty(this.hostname);
        }

        public String toString() {
            return PrivateDnsConfig.class.getSimpleName() + "{" + this.useTls + ":" + this.hostname + SliceClientPermissions.SliceAuthority.DELIMITER + Arrays.toString(this.ips) + "}";
        }
    }

    public static PrivateDnsConfig getPrivateDnsConfig(ContentResolver contentResolver) {
        String privateDnsMode = getPrivateDnsMode(contentResolver);
        boolean z = (TextUtils.isEmpty(privateDnsMode) || "off".equals(privateDnsMode)) ? false : true;
        if ("hostname".equals(privateDnsMode)) {
            return new PrivateDnsConfig(getStringSetting(contentResolver, "private_dns_specifier"), null);
        }
        return new PrivateDnsConfig(z);
    }

    public static PrivateDnsConfig tryBlockingResolveOf(Network network, String str) {
        try {
            return new PrivateDnsConfig(str, ResolvUtil.blockingResolveAllLocally(network, str));
        } catch (UnknownHostException e) {
            return new PrivateDnsConfig(str, null);
        }
    }

    public static Uri[] getPrivateDnsSettingsUris() {
        return new Uri[]{Settings.Global.getUriFor("private_dns_default_mode"), Settings.Global.getUriFor("private_dns_mode"), Settings.Global.getUriFor("private_dns_specifier")};
    }

    public static class PrivateDnsValidationUpdate {
        public final String hostname;
        public final InetAddress ipAddress;
        public final int netId;
        public final boolean validated;

        public PrivateDnsValidationUpdate(int i, InetAddress inetAddress, String str, boolean z) {
            this.netId = i;
            this.ipAddress = inetAddress;
            this.hostname = str;
            this.validated = z;
        }
    }

    private static class PrivateDnsValidationStatuses {
        private Map<Pair<String, InetAddress>, ValidationStatus> mValidationMap;

        enum ValidationStatus {
            IN_PROGRESS,
            FAILED,
            SUCCEEDED
        }

        private PrivateDnsValidationStatuses() {
            this.mValidationMap = new HashMap();
        }

        private boolean hasValidatedServer() {
            Iterator<ValidationStatus> it = this.mValidationMap.values().iterator();
            while (it.hasNext()) {
                if (it.next() == ValidationStatus.SUCCEEDED) {
                    return true;
                }
            }
            return false;
        }

        private void updateTrackedDnses(String[] strArr, String str) {
            HashSet<Pair<String, InetAddress>> hashSet = new HashSet();
            for (String str2 : strArr) {
                try {
                    hashSet.add(new Pair(str, InetAddress.parseNumericAddress(str2)));
                } catch (IllegalArgumentException e) {
                }
            }
            Iterator<Map.Entry<Pair<String, InetAddress>, ValidationStatus>> it = this.mValidationMap.entrySet().iterator();
            while (it.hasNext()) {
                if (!hashSet.contains(it.next().getKey())) {
                    it.remove();
                }
            }
            for (Pair<String, InetAddress> pair : hashSet) {
                if (!this.mValidationMap.containsKey(pair)) {
                    this.mValidationMap.put(pair, ValidationStatus.IN_PROGRESS);
                }
            }
        }

        private void updateStatus(PrivateDnsValidationUpdate privateDnsValidationUpdate) {
            Pair<String, InetAddress> pair = new Pair<>(privateDnsValidationUpdate.hostname, privateDnsValidationUpdate.ipAddress);
            if (!this.mValidationMap.containsKey(pair)) {
                return;
            }
            if (privateDnsValidationUpdate.validated) {
                this.mValidationMap.put(pair, ValidationStatus.SUCCEEDED);
            } else {
                this.mValidationMap.put(pair, ValidationStatus.FAILED);
            }
        }

        private LinkProperties fillInValidatedPrivateDns(final LinkProperties linkProperties) {
            linkProperties.setValidatedPrivateDnsServers(Collections.EMPTY_LIST);
            this.mValidationMap.forEach(new BiConsumer() {
                @Override
                public final void accept(Object obj, Object obj2) {
                    DnsManager.PrivateDnsValidationStatuses.lambda$fillInValidatedPrivateDns$0(linkProperties, (Pair) obj, (DnsManager.PrivateDnsValidationStatuses.ValidationStatus) obj2);
                }
            });
            return linkProperties;
        }

        static void lambda$fillInValidatedPrivateDns$0(LinkProperties linkProperties, Pair pair, ValidationStatus validationStatus) {
            if (validationStatus == ValidationStatus.SUCCEEDED) {
                linkProperties.addValidatedPrivateDnsServer((InetAddress) pair.second);
            }
        }
    }

    public DnsManager(Context context, INetworkManagementService iNetworkManagementService, MockableSystemProperties mockableSystemProperties) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mNMS = iNetworkManagementService;
        this.mSystemProperties = mockableSystemProperties;
    }

    public PrivateDnsConfig getPrivateDnsConfig() {
        return getPrivateDnsConfig(this.mContentResolver);
    }

    public void removeNetwork(Network network) {
        this.mPrivateDnsMap.remove(Integer.valueOf(network.netId));
        this.mPrivateDnsValidationMap.remove(Integer.valueOf(network.netId));
    }

    public PrivateDnsConfig updatePrivateDns(Network network, PrivateDnsConfig privateDnsConfig) {
        Slog.w(TAG, "updatePrivateDns(" + network + ", " + privateDnsConfig + ")");
        if (privateDnsConfig != null) {
            return this.mPrivateDnsMap.put(Integer.valueOf(network.netId), privateDnsConfig);
        }
        return this.mPrivateDnsMap.remove(Integer.valueOf(network.netId));
    }

    public void updatePrivateDnsStatus(int i, LinkProperties linkProperties) {
        PrivateDnsConfig orDefault = this.mPrivateDnsMap.getOrDefault(Integer.valueOf(i), PRIVATE_DNS_OFF);
        PrivateDnsValidationStatuses privateDnsValidationStatuses = orDefault.useTls ? this.mPrivateDnsValidationMap.get(Integer.valueOf(i)) : null;
        boolean z = privateDnsValidationStatuses != null && privateDnsValidationStatuses.hasValidatedServer();
        boolean zInStrictMode = orDefault.inStrictMode();
        String str = zInStrictMode ? orDefault.hostname : null;
        boolean z2 = zInStrictMode || z;
        linkProperties.setUsePrivateDns(z2);
        linkProperties.setPrivateDnsServerName(str);
        if (z2 && privateDnsValidationStatuses != null) {
            privateDnsValidationStatuses.fillInValidatedPrivateDns(linkProperties);
        } else {
            linkProperties.setValidatedPrivateDnsServers(Collections.EMPTY_LIST);
        }
    }

    public void updatePrivateDnsValidation(PrivateDnsValidationUpdate privateDnsValidationUpdate) {
        PrivateDnsValidationStatuses privateDnsValidationStatuses = this.mPrivateDnsValidationMap.get(Integer.valueOf(privateDnsValidationUpdate.netId));
        if (privateDnsValidationStatuses == null) {
            return;
        }
        privateDnsValidationStatuses.updateStatus(privateDnsValidationUpdate);
    }

    public void setDnsConfigurationForNetwork(int i, final LinkProperties linkProperties, boolean z) {
        String[] strArrMakeStrings;
        String[] strArr;
        String[] strArrMakeStrings2 = NetworkUtils.makeStrings(linkProperties.getDnsServers());
        String[] domainStrings = getDomainStrings(linkProperties.getDomains());
        updateParametersSettings();
        int[] iArr = {this.mSampleValidity, this.mSuccessThreshold, this.mMinSamples, this.mMaxSamples};
        PrivateDnsConfig orDefault = this.mPrivateDnsMap.getOrDefault(Integer.valueOf(i), PRIVATE_DNS_OFF);
        boolean z2 = orDefault.useTls;
        boolean zInStrictMode = orDefault.inStrictMode();
        String str = zInStrictMode ? orDefault.hostname : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        try {
            if (zInStrictMode) {
                strArrMakeStrings = NetworkUtils.makeStrings((Collection) Arrays.stream(orDefault.ips).filter(new Predicate() {
                    @Override
                    public final boolean test(Object obj) {
                        return linkProperties.isReachable((InetAddress) obj);
                    }
                }).collect(Collectors.toList()));
            } else if (!z2) {
                strArrMakeStrings = new String[0];
            } else {
                strArr = strArrMakeStrings2;
                if (!z2) {
                    if (!this.mPrivateDnsValidationMap.containsKey(Integer.valueOf(i))) {
                        this.mPrivateDnsValidationMap.put(Integer.valueOf(i), new PrivateDnsValidationStatuses());
                    }
                    this.mPrivateDnsValidationMap.get(Integer.valueOf(i)).updateTrackedDnses(strArr, str);
                } else {
                    this.mPrivateDnsValidationMap.remove(Integer.valueOf(i));
                }
                Slog.d(TAG, String.format("setDnsConfigurationForNetwork(%d, %s, %s, %s, %s, %s)", Integer.valueOf(i), Arrays.toString(strArrMakeStrings2), Arrays.toString(domainStrings), Arrays.toString(iArr), str, Arrays.toString(strArr)));
                this.mNMS.setDnsConfigurationForNetwork(i, strArrMakeStrings2, domainStrings, iArr, str, strArr);
                if (z) {
                    setDefaultDnsSystemProperties(linkProperties.getDnsServers());
                }
                flushVmDnsCache();
                return;
            }
            this.mNMS.setDnsConfigurationForNetwork(i, strArrMakeStrings2, domainStrings, iArr, str, strArr);
            if (z) {
            }
            flushVmDnsCache();
            return;
        } catch (Exception e) {
            Slog.e(TAG, "Error setting DNS configuration: " + e);
            return;
        }
        strArr = strArrMakeStrings;
        if (!z2) {
        }
        Slog.d(TAG, String.format("setDnsConfigurationForNetwork(%d, %s, %s, %s, %s, %s)", Integer.valueOf(i), Arrays.toString(strArrMakeStrings2), Arrays.toString(domainStrings), Arrays.toString(iArr), str, Arrays.toString(strArr)));
    }

    public void setDefaultDnsSystemProperties(Collection<InetAddress> collection) {
        Iterator<InetAddress> it = collection.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            setNetDnsProperty(i, it.next().getHostAddress());
        }
        for (int i2 = i + 1; i2 <= this.mNumDnsEntries; i2++) {
            setNetDnsProperty(i2, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
        }
        this.mNumDnsEntries = i;
    }

    private void flushVmDnsCache() {
        Intent intent = new Intent("android.intent.action.CLEAR_DNS_CACHE");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    private void updateParametersSettings() {
        this.mSampleValidity = getIntSetting("dns_resolver_sample_validity_seconds", 1800);
        if (this.mSampleValidity < 0 || this.mSampleValidity > 65535) {
            Slog.w(TAG, "Invalid sampleValidity=" + this.mSampleValidity + ", using default=1800");
            this.mSampleValidity = 1800;
        }
        this.mSuccessThreshold = getIntSetting("dns_resolver_success_threshold_percent", 25);
        if (this.mSuccessThreshold < 0 || this.mSuccessThreshold > 100) {
            Slog.w(TAG, "Invalid successThreshold=" + this.mSuccessThreshold + ", using default=25");
            this.mSuccessThreshold = 25;
        }
        this.mMinSamples = getIntSetting("dns_resolver_min_samples", 8);
        this.mMaxSamples = getIntSetting("dns_resolver_max_samples", 64);
        if (this.mMinSamples < 0 || this.mMinSamples > this.mMaxSamples || this.mMaxSamples > 64) {
            Slog.w(TAG, "Invalid sample count (min, max)=(" + this.mMinSamples + ", " + this.mMaxSamples + "), using default=(8, 64)");
            this.mMinSamples = 8;
            this.mMaxSamples = 64;
        }
    }

    private int getIntSetting(String str, int i) {
        return Settings.Global.getInt(this.mContentResolver, str, i);
    }

    private void setNetDnsProperty(int i, String str) {
        try {
            this.mSystemProperties.set("net.dns" + i, str);
        } catch (Exception e) {
            Slog.e(TAG, "Error setting unsupported net.dns property: ", e);
        }
    }

    private static String getPrivateDnsMode(ContentResolver contentResolver) {
        String stringSetting = getStringSetting(contentResolver, "private_dns_mode");
        if (TextUtils.isEmpty(stringSetting)) {
            stringSetting = getStringSetting(contentResolver, "private_dns_default_mode");
        }
        return TextUtils.isEmpty(stringSetting) ? "opportunistic" : stringSetting;
    }

    private static String getStringSetting(ContentResolver contentResolver, String str) {
        return Settings.Global.getString(contentResolver, str);
    }

    private static String[] getDomainStrings(String str) {
        return TextUtils.isEmpty(str) ? new String[0] : str.split(" ");
    }
}
