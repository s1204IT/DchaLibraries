package com.android.server.wifi.hotspot2;

import android.content.Context;
import android.graphics.drawable.Icon;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import com.android.server.wifi.Clock;
import com.android.server.wifi.SIMAccessor;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.WifiConfigStore;
import com.android.server.wifi.WifiKeyStore;
import com.android.server.wifi.WifiMetrics;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.NetworkDetail;
import com.android.server.wifi.hotspot2.PasspointConfigStoreData;
import com.android.server.wifi.hotspot2.PasspointEventHandler;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.HSOsuProvidersElement;
import com.android.server.wifi.hotspot2.anqp.OsuProviderInfo;
import com.android.server.wifi.util.InformationElementUtil;
import com.android.server.wifi.util.ScanResultUtil;
import com.android.server.wifi.util.WifiPermissionsUtil;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PasspointManager {
    private static final String TAG = "PasspointManager";
    private static PasspointManager sPasspointManager;
    private final AnqpCache mAnqpCache;
    private final ANQPRequestManager mAnqpRequestManager;
    private final CertificateVerifier mCertVerifier;
    private final PasspointEventHandler mHandler;
    private final WifiKeyStore mKeyStore;
    private final PasspointObjectFactory mObjectFactory;
    private final PasspointProvisioner mPasspointProvisioner;
    private final SIMAccessor mSimAccessor;
    private final WifiConfigManager mWifiConfigManager;
    private final WifiMetrics mWifiMetrics;
    private final WifiPermissionsUtil mWifiPermissionsUtil;
    private final Map<String, PasspointProvider> mProviders = new HashMap();
    private long mProviderIndex = 0;

    private class CallbackHandler implements PasspointEventHandler.Callbacks {
        private final Context mContext;

        CallbackHandler(Context context) {
            this.mContext = context;
        }

        @Override
        public void onANQPResponse(long j, Map<Constants.ANQPElementType, ANQPElement> map) {
            ANQPNetworkKey aNQPNetworkKeyOnRequestCompleted = PasspointManager.this.mAnqpRequestManager.onRequestCompleted(j, map != null);
            if (map != null && aNQPNetworkKeyOnRequestCompleted != null) {
                PasspointManager.this.mAnqpCache.addEntry(aNQPNetworkKeyOnRequestCompleted, map);
            }
        }

        @Override
        public void onIconResponse(long j, String str, byte[] bArr) {
        }

        @Override
        public void onWnmFrameReceived(WnmData wnmData) {
        }
    }

    private class DataSourceHandler implements PasspointConfigStoreData.DataSource {
        private DataSourceHandler() {
        }

        @Override
        public List<PasspointProvider> getProviders() {
            ArrayList arrayList = new ArrayList();
            Iterator it = PasspointManager.this.mProviders.entrySet().iterator();
            while (it.hasNext()) {
                arrayList.add((PasspointProvider) ((Map.Entry) it.next()).getValue());
            }
            return arrayList;
        }

        @Override
        public void setProviders(List<PasspointProvider> list) {
            PasspointManager.this.mProviders.clear();
            for (PasspointProvider passpointProvider : list) {
                PasspointManager.this.mProviders.put(passpointProvider.getConfig().getHomeSp().getFqdn(), passpointProvider);
            }
        }

        @Override
        public long getProviderIndex() {
            return PasspointManager.this.mProviderIndex;
        }

        @Override
        public void setProviderIndex(long j) {
            PasspointManager.this.mProviderIndex = j;
        }
    }

    public PasspointManager(Context context, WifiNative wifiNative, WifiKeyStore wifiKeyStore, Clock clock, SIMAccessor sIMAccessor, PasspointObjectFactory passpointObjectFactory, WifiConfigManager wifiConfigManager, WifiConfigStore wifiConfigStore, WifiMetrics wifiMetrics, WifiPermissionsUtil wifiPermissionsUtil) {
        this.mHandler = passpointObjectFactory.makePasspointEventHandler(wifiNative, new CallbackHandler(context));
        this.mKeyStore = wifiKeyStore;
        this.mSimAccessor = sIMAccessor;
        this.mObjectFactory = passpointObjectFactory;
        this.mAnqpCache = passpointObjectFactory.makeAnqpCache(clock);
        this.mAnqpRequestManager = passpointObjectFactory.makeANQPRequestManager(this.mHandler, clock);
        this.mCertVerifier = passpointObjectFactory.makeCertificateVerifier();
        this.mWifiConfigManager = wifiConfigManager;
        this.mWifiMetrics = wifiMetrics;
        wifiConfigStore.registerStoreData(passpointObjectFactory.makePasspointConfigStoreData(this.mKeyStore, this.mSimAccessor, new DataSourceHandler()));
        this.mPasspointProvisioner = passpointObjectFactory.makePasspointProvisioner(context);
        sPasspointManager = this;
        this.mWifiPermissionsUtil = wifiPermissionsUtil;
    }

    public void initializeProvisioner(Looper looper) {
        this.mPasspointProvisioner.init(looper);
    }

    public void enableVerboseLogging(int i) {
        this.mPasspointProvisioner.enableVerboseLogging(i);
    }

    public boolean addOrUpdateProvider(PasspointConfiguration passpointConfiguration, int i) {
        this.mWifiMetrics.incrementNumPasspointProviderInstallation();
        if (passpointConfiguration == null) {
            Log.e(TAG, "Configuration not provided");
            return false;
        }
        if (!passpointConfiguration.validate()) {
            Log.e(TAG, "Invalid configuration");
            return false;
        }
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i)) {
            Log.e(TAG, "UID " + i + " not visible to the current user");
            return false;
        }
        if (passpointConfiguration.getUpdateIdentifier() == Integer.MIN_VALUE && passpointConfiguration.getCredential().getCaCertificate() != null) {
            try {
                this.mCertVerifier.verifyCaCert(passpointConfiguration.getCredential().getCaCertificate());
            } catch (Exception e) {
                Log.e(TAG, "Failed to verify CA certificate: " + e.getMessage());
                return false;
            }
        }
        PasspointObjectFactory passpointObjectFactory = this.mObjectFactory;
        WifiKeyStore wifiKeyStore = this.mKeyStore;
        SIMAccessor sIMAccessor = this.mSimAccessor;
        long j = this.mProviderIndex;
        this.mProviderIndex = 1 + j;
        PasspointProvider passpointProviderMakePasspointProvider = passpointObjectFactory.makePasspointProvider(passpointConfiguration, wifiKeyStore, sIMAccessor, j, i);
        if (!passpointProviderMakePasspointProvider.installCertsAndKeys()) {
            Log.e(TAG, "Failed to install certificates and keys to keystore");
            return false;
        }
        if (this.mProviders.containsKey(passpointConfiguration.getHomeSp().getFqdn())) {
            Log.d(TAG, "Replacing configuration for " + passpointConfiguration.getHomeSp().getFqdn());
            this.mProviders.get(passpointConfiguration.getHomeSp().getFqdn()).uninstallCertsAndKeys();
            this.mProviders.remove(passpointConfiguration.getHomeSp().getFqdn());
        }
        this.mProviders.put(passpointConfiguration.getHomeSp().getFqdn(), passpointProviderMakePasspointProvider);
        this.mWifiConfigManager.saveToStore(true);
        Log.d(TAG, "Added/updated Passpoint configuration: " + passpointConfiguration.getHomeSp().getFqdn() + " by " + i);
        this.mWifiMetrics.incrementNumPasspointProviderInstallSuccess();
        return true;
    }

    public boolean removeProvider(int i, String str) {
        this.mWifiMetrics.incrementNumPasspointProviderUninstallation();
        if (!this.mProviders.containsKey(str)) {
            Log.e(TAG, "Config doesn't exist");
            return false;
        }
        if (!this.mWifiPermissionsUtil.doesUidBelongToCurrentUser(i)) {
            Log.e(TAG, "UID " + i + " not visible to the current user");
            return false;
        }
        this.mProviders.get(str).uninstallCertsAndKeys();
        this.mProviders.remove(str);
        this.mWifiConfigManager.saveToStore(true);
        Log.d(TAG, "Removed Passpoint configuration: " + str);
        this.mWifiMetrics.incrementNumPasspointProviderUninstallSuccess();
        return true;
    }

    public List<PasspointConfiguration> getProviderConfigs() {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<String, PasspointProvider>> it = this.mProviders.entrySet().iterator();
        while (it.hasNext()) {
            arrayList.add(it.next().getValue().getConfig());
        }
        return arrayList;
    }

    public Pair<PasspointProvider, PasspointMatch> matchProvider(ScanResult scanResult) {
        List<Pair<PasspointProvider, PasspointMatch>> allMatchedProviders = getAllMatchedProviders(scanResult);
        Pair<PasspointProvider, PasspointMatch> pair = null;
        if (allMatchedProviders == null) {
            return null;
        }
        Iterator<Pair<PasspointProvider, PasspointMatch>> it = allMatchedProviders.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Pair<PasspointProvider, PasspointMatch> next = it.next();
            if (next.second != PasspointMatch.HomeProvider) {
                if (next.second == PasspointMatch.RoamingProvider && pair == null) {
                    pair = next;
                }
            } else {
                pair = next;
                break;
            }
        }
        if (pair != null) {
            Object[] objArr = new Object[3];
            objArr[0] = scanResult.SSID;
            objArr[1] = ((PasspointProvider) pair.first).getConfig().getHomeSp().getFqdn();
            objArr[2] = pair.second == PasspointMatch.HomeProvider ? "Home Provider" : "Roaming Provider";
            Log.d(TAG, String.format("Matched %s to %s as %s", objArr));
        } else {
            Log.d(TAG, "Match not found for " + scanResult.SSID);
        }
        return pair;
    }

    public List<Pair<PasspointProvider, PasspointMatch>> getAllMatchedProviders(ScanResult scanResult) {
        ArrayList<Pair> arrayList = new ArrayList();
        InformationElementUtil.RoamingConsortium roamingConsortiumIE = InformationElementUtil.getRoamingConsortiumIE(scanResult.informationElements);
        InformationElementUtil.Vsa hS2VendorSpecificIE = InformationElementUtil.getHS2VendorSpecificIE(scanResult.informationElements);
        try {
            long mac = Utils.parseMac(scanResult.BSSID);
            ANQPNetworkKey aNQPNetworkKeyBuildKey = ANQPNetworkKey.buildKey(scanResult.SSID, mac, scanResult.hessid, hS2VendorSpecificIE.anqpDomainID);
            ANQPData entry = this.mAnqpCache.getEntry(aNQPNetworkKeyBuildKey);
            if (entry == null) {
                this.mAnqpRequestManager.requestANQPElements(mac, aNQPNetworkKeyBuildKey, roamingConsortiumIE.anqpOICount > 0, hS2VendorSpecificIE.hsRelease == NetworkDetail.HSRelease.R2);
                Log.d(TAG, "ANQP entry not found for: " + aNQPNetworkKeyBuildKey);
                return arrayList;
            }
            Iterator<Map.Entry<String, PasspointProvider>> it = this.mProviders.entrySet().iterator();
            while (it.hasNext()) {
                PasspointProvider value = it.next().getValue();
                PasspointMatch passpointMatchMatch = value.match(entry.getElements(), roamingConsortiumIE);
                if (passpointMatchMatch == PasspointMatch.HomeProvider || passpointMatchMatch == PasspointMatch.RoamingProvider) {
                    arrayList.add(Pair.create(value, passpointMatchMatch));
                }
            }
            if (arrayList.size() != 0) {
                for (Pair pair : arrayList) {
                    Object[] objArr = new Object[3];
                    objArr[0] = scanResult.SSID;
                    objArr[1] = ((PasspointProvider) pair.first).getConfig().getHomeSp().getFqdn();
                    objArr[2] = pair.second == PasspointMatch.HomeProvider ? "Home Provider" : "Roaming Provider";
                    Log.d(TAG, String.format("Matched %s to %s as %s", objArr));
                }
            } else {
                Log.d(TAG, "No matches not found for " + scanResult.SSID);
            }
            return arrayList;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid BSSID provided in the scan result: " + scanResult.BSSID);
            return arrayList;
        }
    }

    public static boolean addLegacyPasspointConfig(WifiConfiguration wifiConfiguration) {
        if (sPasspointManager == null) {
            Log.e(TAG, "PasspointManager have not been initialized yet");
            return false;
        }
        Log.d(TAG, "Installing legacy Passpoint configuration: " + wifiConfiguration.FQDN);
        return sPasspointManager.addWifiConfig(wifiConfiguration);
    }

    public void sweepCache() {
        this.mAnqpCache.sweep();
    }

    public void notifyANQPDone(AnqpEvent anqpEvent) {
        this.mHandler.notifyANQPDone(anqpEvent);
    }

    public void notifyIconDone(IconEvent iconEvent) {
        this.mHandler.notifyIconDone(iconEvent);
    }

    public void receivedWnmFrame(WnmData wnmData) {
        this.mHandler.notifyWnmFrameReceived(wnmData);
    }

    public boolean queryPasspointIcon(long j, String str) {
        return this.mHandler.requestIcon(j, str);
    }

    public Map<Constants.ANQPElementType, ANQPElement> getANQPElements(ScanResult scanResult) {
        try {
            ANQPData entry = this.mAnqpCache.getEntry(ANQPNetworkKey.buildKey(scanResult.SSID, Utils.parseMac(scanResult.BSSID), scanResult.hessid, InformationElementUtil.getHS2VendorSpecificIE(scanResult.informationElements).anqpDomainID));
            if (entry != null) {
                return entry.getElements();
            }
            return new HashMap();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid BSSID provided in the scan result: " + scanResult.BSSID);
            return new HashMap();
        }
    }

    public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) {
        if (scanResult == null) {
            Log.e(TAG, "Attempt to get matching config for a null ScanResult");
            return null;
        }
        if (!scanResult.isPasspointNetwork()) {
            Log.e(TAG, "Attempt to get matching config for a non-Passpoint AP");
            return null;
        }
        Pair<PasspointProvider, PasspointMatch> pairMatchProvider = matchProvider(scanResult);
        if (pairMatchProvider == null) {
            return null;
        }
        WifiConfiguration wifiConfig = ((PasspointProvider) pairMatchProvider.first).getWifiConfig();
        wifiConfig.SSID = ScanResultUtil.createQuotedSSID(scanResult.SSID);
        if (pairMatchProvider.second == PasspointMatch.HomeProvider) {
            wifiConfig.isHomeProviderNetwork = true;
        }
        return wifiConfig;
    }

    public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) {
        if (scanResult == null) {
            Log.e(TAG, "Attempt to get matching config for a null ScanResult");
            return new ArrayList();
        }
        if (!scanResult.isPasspointNetwork()) {
            Log.e(TAG, "Attempt to get matching config for a non-Passpoint AP");
            return new ArrayList();
        }
        List<Pair<PasspointProvider, PasspointMatch>> allMatchedProviders = getAllMatchedProviders(scanResult);
        ArrayList arrayList = new ArrayList();
        for (Pair<PasspointProvider, PasspointMatch> pair : allMatchedProviders) {
            WifiConfiguration wifiConfig = ((PasspointProvider) pair.first).getWifiConfig();
            wifiConfig.SSID = ScanResultUtil.createQuotedSSID(scanResult.SSID);
            if (pair.second == PasspointMatch.HomeProvider) {
                wifiConfig.isHomeProviderNetwork = true;
            }
            arrayList.add(wifiConfig);
        }
        return arrayList;
    }

    public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) {
        if (scanResult == null) {
            Log.e(TAG, "Attempt to retrieve OSU providers for a null ScanResult");
            return new ArrayList();
        }
        if (!scanResult.isPasspointNetwork()) {
            Log.e(TAG, "Attempt to retrieve OSU providers for a non-Passpoint AP");
            return new ArrayList();
        }
        Map<Constants.ANQPElementType, ANQPElement> aNQPElements = getANQPElements(scanResult);
        if (!aNQPElements.containsKey(Constants.ANQPElementType.HSOSUProviders)) {
            return new ArrayList();
        }
        HSOsuProvidersElement hSOsuProvidersElement = (HSOsuProvidersElement) aNQPElements.get(Constants.ANQPElementType.HSOSUProviders);
        ArrayList arrayList = new ArrayList();
        for (OsuProviderInfo osuProviderInfo : hSOsuProvidersElement.getProviders()) {
            arrayList.add(new OsuProvider(hSOsuProvidersElement.getOsuSsid(), osuProviderInfo.getFriendlyName(), osuProviderInfo.getServiceDescription(), osuProviderInfo.getServerUri(), osuProviderInfo.getNetworkAccessIdentifier(), osuProviderInfo.getMethodList(), (Icon) null));
        }
        return arrayList;
    }

    public void onPasspointNetworkConnected(String str) {
        PasspointProvider passpointProvider = this.mProviders.get(str);
        if (passpointProvider == null) {
            Log.e(TAG, "Passpoint network connected without provider: " + str);
            return;
        }
        if (!passpointProvider.getHasEverConnected()) {
            passpointProvider.setHasEverConnected(true);
        }
    }

    public void updateMetrics() {
        int size = this.mProviders.size();
        Iterator<Map.Entry<String, PasspointProvider>> it = this.mProviders.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().getValue().getHasEverConnected()) {
                i++;
            }
        }
        this.mWifiMetrics.updateSavedPasspointProfiles(size, i);
    }

    public void dump(PrintWriter printWriter) {
        printWriter.println("Dump of PasspointManager");
        printWriter.println("PasspointManager - Providers Begin ---");
        Iterator<Map.Entry<String, PasspointProvider>> it = this.mProviders.entrySet().iterator();
        while (it.hasNext()) {
            printWriter.println(it.next().getValue());
        }
        printWriter.println("PasspointManager - Providers End ---");
        printWriter.println("PasspointManager - Next provider ID to be assigned " + this.mProviderIndex);
        this.mAnqpCache.dump(printWriter);
    }

    private boolean addWifiConfig(WifiConfiguration wifiConfiguration) {
        PasspointConfiguration passpointConfigurationConvertFromWifiConfig;
        if (wifiConfiguration == null || (passpointConfigurationConvertFromWifiConfig = PasspointProvider.convertFromWifiConfig(wifiConfiguration)) == null) {
            return false;
        }
        WifiEnterpriseConfig wifiEnterpriseConfig = wifiConfiguration.enterpriseConfig;
        String caCertificateAlias = wifiEnterpriseConfig.getCaCertificateAlias();
        String clientCertificateAlias = wifiEnterpriseConfig.getClientCertificateAlias();
        if (passpointConfigurationConvertFromWifiConfig.getCredential().getUserCredential() != null && TextUtils.isEmpty(caCertificateAlias)) {
            Log.e(TAG, "Missing CA Certificate for user credential");
            return false;
        }
        if (passpointConfigurationConvertFromWifiConfig.getCredential().getCertCredential() != null) {
            if (TextUtils.isEmpty(caCertificateAlias)) {
                Log.e(TAG, "Missing CA certificate for Certificate credential");
                return false;
            }
            if (TextUtils.isEmpty(clientCertificateAlias)) {
                Log.e(TAG, "Missing client certificate and key for certificate credential");
                return false;
            }
        }
        WifiKeyStore wifiKeyStore = this.mKeyStore;
        SIMAccessor sIMAccessor = this.mSimAccessor;
        long j = this.mProviderIndex;
        this.mProviderIndex = 1 + j;
        this.mProviders.put(passpointConfigurationConvertFromWifiConfig.getHomeSp().getFqdn(), new PasspointProvider(passpointConfigurationConvertFromWifiConfig, wifiKeyStore, sIMAccessor, j, wifiConfiguration.creatorUid, wifiEnterpriseConfig.getCaCertificateAlias(), wifiEnterpriseConfig.getClientCertificateAlias(), wifiEnterpriseConfig.getClientCertificateAlias(), false, false));
        return true;
    }

    public boolean startSubscriptionProvisioning(int i, OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) {
        return this.mPasspointProvisioner.startSubscriptionProvisioning(i, osuProvider, iProvisioningCallback);
    }
}
