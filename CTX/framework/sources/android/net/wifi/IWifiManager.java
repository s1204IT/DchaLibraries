package android.net.wifi;

import android.content.pm.ParceledListSlice;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.wifi.ISoftApCallback;
import android.net.wifi.hotspot2.IProvisioningCallback;
import android.net.wifi.hotspot2.OsuProvider;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.WorkSource;
import java.util.List;
import mediatek.net.wifi.HotspotClient;

public interface IWifiManager extends IInterface {
    void acquireMulticastLock(IBinder iBinder, String str) throws RemoteException;

    boolean acquireWifiLock(IBinder iBinder, int i, String str, WorkSource workSource) throws RemoteException;

    int addOrUpdateNetwork(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    boolean addOrUpdatePasspointConfiguration(PasspointConfiguration passpointConfiguration, String str) throws RemoteException;

    boolean allowDevice(String str, String str2) throws RemoteException;

    boolean blockClient(HotspotClient hotspotClient) throws RemoteException;

    void deauthenticateNetwork(long j, boolean z) throws RemoteException;

    void disableEphemeralNetwork(String str, String str2) throws RemoteException;

    boolean disableNetwork(int i, String str) throws RemoteException;

    boolean disallowDevice(String str) throws RemoteException;

    void disconnect(String str) throws RemoteException;

    boolean enableNetwork(int i, boolean z, String str) throws RemoteException;

    void enableTdls(String str, boolean z) throws RemoteException;

    void enableTdlsWithMacAddress(String str, boolean z) throws RemoteException;

    void enableVerboseLogging(int i) throws RemoteException;

    void enableWifiConnectivityManager(boolean z) throws RemoteException;

    void factoryReset(String str) throws RemoteException;

    List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) throws RemoteException;

    List<HotspotClient> getAllowedDevices() throws RemoteException;

    String getClientDeviceName(String str) throws RemoteException;

    String getClientIp(String str) throws RemoteException;

    ParceledListSlice getConfiguredNetworks() throws RemoteException;

    WifiInfo getConnectionInfo(String str) throws RemoteException;

    String getCountryCode() throws RemoteException;

    Network getCurrentNetwork() throws RemoteException;

    String getCurrentNetworkWpsNfcConfigurationToken() throws RemoteException;

    DhcpInfo getDhcpInfo() throws RemoteException;

    List<HotspotClient> getHotspotClients() throws RemoteException;

    List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) throws RemoteException;

    WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) throws RemoteException;

    List<PasspointConfiguration> getPasspointConfigurations() throws RemoteException;

    ParceledListSlice getPrivilegedConfiguredNetworks() throws RemoteException;

    List<ScanResult> getScanResults(String str) throws RemoteException;

    int getSupportedFeatures() throws RemoteException;

    int getVerboseLoggingLevel() throws RemoteException;

    WifiConfiguration getWifiApConfiguration() throws RemoteException;

    int getWifiApEnabledState() throws RemoteException;

    int getWifiEnabledState() throws RemoteException;

    Messenger getWifiServiceMessenger(String str) throws RemoteException;

    void initializeMulticastFiltering() throws RemoteException;

    boolean isAllDevicesAllowed() throws RemoteException;

    boolean isDualBandSupported() throws RemoteException;

    boolean isMulticastEnabled() throws RemoteException;

    boolean isScanAlwaysAvailable() throws RemoteException;

    int matchProviderWithCurrentNetwork(String str) throws RemoteException;

    boolean needs5GHzToAnyApBandConversion() throws RemoteException;

    void queryPasspointIcon(long j, String str) throws RemoteException;

    void reassociate(String str) throws RemoteException;

    void reconnect(String str) throws RemoteException;

    void registerSoftApCallback(IBinder iBinder, ISoftApCallback iSoftApCallback, int i) throws RemoteException;

    void releaseMulticastLock() throws RemoteException;

    boolean releaseWifiLock(IBinder iBinder) throws RemoteException;

    boolean removeNetwork(int i, String str) throws RemoteException;

    boolean removePasspointConfiguration(String str, String str2) throws RemoteException;

    WifiActivityEnergyInfo reportActivityInfo() throws RemoteException;

    void requestActivityInfo(ResultReceiver resultReceiver) throws RemoteException;

    void restoreBackupData(byte[] bArr) throws RemoteException;

    void restoreSupplicantBackupData(byte[] bArr, byte[] bArr2) throws RemoteException;

    byte[] retrieveBackupData() throws RemoteException;

    boolean setAllDevicesAllowed(boolean z, boolean z2) throws RemoteException;

    void setCountryCode(String str) throws RemoteException;

    void setPowerSavingMode(boolean z) throws RemoteException;

    boolean setWifiApConfiguration(WifiConfiguration wifiConfiguration, String str) throws RemoteException;

    boolean setWifiEnabled(String str, boolean z) throws RemoteException;

    int startLocalOnlyHotspot(Messenger messenger, IBinder iBinder, String str) throws RemoteException;

    boolean startScan(String str) throws RemoteException;

    boolean startSoftAp(WifiConfiguration wifiConfiguration) throws RemoteException;

    void startSubscriptionProvisioning(OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) throws RemoteException;

    void startWatchLocalOnlyHotspot(Messenger messenger, IBinder iBinder) throws RemoteException;

    void stopLocalOnlyHotspot() throws RemoteException;

    boolean stopSoftAp() throws RemoteException;

    void stopWatchLocalOnlyHotspot() throws RemoteException;

    boolean unblockClient(HotspotClient hotspotClient) throws RemoteException;

    void unregisterSoftApCallback(int i) throws RemoteException;

    void updateInterfaceIpState(String str, int i) throws RemoteException;

    void updateWifiLockWorkSource(IBinder iBinder, WorkSource workSource) throws RemoteException;

    public static abstract class Stub extends Binder implements IWifiManager {
        private static final String DESCRIPTOR = "android.net.wifi.IWifiManager";
        static final int TRANSACTION_acquireMulticastLock = 38;
        static final int TRANSACTION_acquireWifiLock = 33;
        static final int TRANSACTION_addOrUpdateNetwork = 9;
        static final int TRANSACTION_addOrUpdatePasspointConfiguration = 10;
        static final int TRANSACTION_allowDevice = 73;
        static final int TRANSACTION_blockClient = 69;
        static final int TRANSACTION_deauthenticateNetwork = 15;
        static final int TRANSACTION_disableEphemeralNetwork = 57;
        static final int TRANSACTION_disableNetwork = 18;
        static final int TRANSACTION_disallowDevice = 74;
        static final int TRANSACTION_disconnect = 21;
        static final int TRANSACTION_enableNetwork = 17;
        static final int TRANSACTION_enableTdls = 51;
        static final int TRANSACTION_enableTdlsWithMacAddress = 52;
        static final int TRANSACTION_enableVerboseLogging = 54;
        static final int TRANSACTION_enableWifiConnectivityManager = 56;
        static final int TRANSACTION_factoryReset = 58;
        static final int TRANSACTION_getAllMatchingWifiConfigs = 7;
        static final int TRANSACTION_getAllowedDevices = 75;
        static final int TRANSACTION_getClientDeviceName = 68;
        static final int TRANSACTION_getClientIp = 67;
        static final int TRANSACTION_getConfiguredNetworks = 4;
        static final int TRANSACTION_getConnectionInfo = 24;
        static final int TRANSACTION_getCountryCode = 28;
        static final int TRANSACTION_getCurrentNetwork = 59;
        static final int TRANSACTION_getCurrentNetworkWpsNfcConfigurationToken = 53;
        static final int TRANSACTION_getDhcpInfo = 31;
        static final int TRANSACTION_getHotspotClients = 66;
        static final int TRANSACTION_getMatchingOsuProviders = 8;
        static final int TRANSACTION_getMatchingWifiConfig = 6;
        static final int TRANSACTION_getPasspointConfigurations = 12;
        static final int TRANSACTION_getPrivilegedConfiguredNetworks = 5;
        static final int TRANSACTION_getScanResults = 20;
        static final int TRANSACTION_getSupportedFeatures = 1;
        static final int TRANSACTION_getVerboseLoggingLevel = 55;
        static final int TRANSACTION_getWifiApConfiguration = 48;
        static final int TRANSACTION_getWifiApEnabledState = 47;
        static final int TRANSACTION_getWifiEnabledState = 26;
        static final int TRANSACTION_getWifiServiceMessenger = 50;
        static final int TRANSACTION_initializeMulticastFiltering = 36;
        static final int TRANSACTION_isAllDevicesAllowed = 71;
        static final int TRANSACTION_isDualBandSupported = 29;
        static final int TRANSACTION_isMulticastEnabled = 37;
        static final int TRANSACTION_isScanAlwaysAvailable = 32;
        static final int TRANSACTION_matchProviderWithCurrentNetwork = 14;
        static final int TRANSACTION_needs5GHzToAnyApBandConversion = 30;
        static final int TRANSACTION_queryPasspointIcon = 13;
        static final int TRANSACTION_reassociate = 23;
        static final int TRANSACTION_reconnect = 22;
        static final int TRANSACTION_registerSoftApCallback = 64;
        static final int TRANSACTION_releaseMulticastLock = 39;
        static final int TRANSACTION_releaseWifiLock = 35;
        static final int TRANSACTION_removeNetwork = 16;
        static final int TRANSACTION_removePasspointConfiguration = 11;
        static final int TRANSACTION_reportActivityInfo = 2;
        static final int TRANSACTION_requestActivityInfo = 3;
        static final int TRANSACTION_restoreBackupData = 61;
        static final int TRANSACTION_restoreSupplicantBackupData = 62;
        static final int TRANSACTION_retrieveBackupData = 60;
        static final int TRANSACTION_setAllDevicesAllowed = 72;
        static final int TRANSACTION_setCountryCode = 27;
        static final int TRANSACTION_setPowerSavingMode = 76;
        static final int TRANSACTION_setWifiApConfiguration = 49;
        static final int TRANSACTION_setWifiEnabled = 25;
        static final int TRANSACTION_startLocalOnlyHotspot = 43;
        static final int TRANSACTION_startScan = 19;
        static final int TRANSACTION_startSoftAp = 41;
        static final int TRANSACTION_startSubscriptionProvisioning = 63;
        static final int TRANSACTION_startWatchLocalOnlyHotspot = 45;
        static final int TRANSACTION_stopLocalOnlyHotspot = 44;
        static final int TRANSACTION_stopSoftAp = 42;
        static final int TRANSACTION_stopWatchLocalOnlyHotspot = 46;
        static final int TRANSACTION_unblockClient = 70;
        static final int TRANSACTION_unregisterSoftApCallback = 65;
        static final int TRANSACTION_updateInterfaceIpState = 40;
        static final int TRANSACTION_updateWifiLockWorkSource = 34;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWifiManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IWifiManager)) {
                return (IWifiManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int supportedFeatures = getSupportedFeatures();
                    parcel2.writeNoException();
                    parcel2.writeInt(supportedFeatures);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    WifiActivityEnergyInfo wifiActivityEnergyInfoReportActivityInfo = reportActivityInfo();
                    parcel2.writeNoException();
                    if (wifiActivityEnergyInfoReportActivityInfo != null) {
                        parcel2.writeInt(1);
                        wifiActivityEnergyInfoReportActivityInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    requestActivityInfo(parcel.readInt() != 0 ? ResultReceiver.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice configuredNetworks = getConfiguredNetworks();
                    parcel2.writeNoException();
                    if (configuredNetworks != null) {
                        parcel2.writeInt(1);
                        configuredNetworks.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    ParceledListSlice privilegedConfiguredNetworks = getPrivilegedConfiguredNetworks();
                    parcel2.writeNoException();
                    if (privilegedConfiguredNetworks != null) {
                        parcel2.writeInt(1);
                        privilegedConfiguredNetworks.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    WifiConfiguration matchingWifiConfig = getMatchingWifiConfig(parcel.readInt() != 0 ? ScanResult.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    if (matchingWifiConfig != null) {
                        parcel2.writeInt(1);
                        matchingWifiConfig.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<WifiConfiguration> allMatchingWifiConfigs = getAllMatchingWifiConfigs(parcel.readInt() != 0 ? ScanResult.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allMatchingWifiConfigs);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<OsuProvider> matchingOsuProviders = getMatchingOsuProviders(parcel.readInt() != 0 ? ScanResult.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeTypedList(matchingOsuProviders);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iAddOrUpdateNetwork = addOrUpdateNetwork(parcel.readInt() != 0 ? WifiConfiguration.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iAddOrUpdateNetwork);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zAddOrUpdatePasspointConfiguration = addOrUpdatePasspointConfiguration(parcel.readInt() != 0 ? PasspointConfiguration.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zAddOrUpdatePasspointConfiguration ? 1 : 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemovePasspointConfiguration = removePasspointConfiguration(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemovePasspointConfiguration ? 1 : 0);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<PasspointConfiguration> passpointConfigurations = getPasspointConfigurations();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(passpointConfigurations);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    queryPasspointIcon(parcel.readLong(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iMatchProviderWithCurrentNetwork = matchProviderWithCurrentNetwork(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iMatchProviderWithCurrentNetwork);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    deauthenticateNetwork(parcel.readLong(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zRemoveNetwork = removeNetwork(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zRemoveNetwork ? 1 : 0);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zEnableNetwork = enableNetwork(parcel.readInt(), parcel.readInt() != 0, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zEnableNetwork ? 1 : 0);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDisableNetwork = disableNetwork(parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDisableNetwork ? 1 : 0);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartScan = startScan(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartScan ? 1 : 0);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<ScanResult> scanResults = getScanResults(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedList(scanResults);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    disconnect(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    reconnect(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    reassociate(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    WifiInfo connectionInfo = getConnectionInfo(parcel.readString());
                    parcel2.writeNoException();
                    if (connectionInfo != null) {
                        parcel2.writeInt(1);
                        connectionInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean wifiEnabled = setWifiEnabled(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(wifiEnabled ? 1 : 0);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    int wifiEnabledState = getWifiEnabledState();
                    parcel2.writeNoException();
                    parcel2.writeInt(wifiEnabledState);
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCountryCode(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    String countryCode = getCountryCode();
                    parcel2.writeNoException();
                    parcel2.writeString(countryCode);
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsDualBandSupported = isDualBandSupported();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsDualBandSupported ? 1 : 0);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zNeeds5GHzToAnyApBandConversion = needs5GHzToAnyApBandConversion();
                    parcel2.writeNoException();
                    parcel2.writeInt(zNeeds5GHzToAnyApBandConversion ? 1 : 0);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    DhcpInfo dhcpInfo = getDhcpInfo();
                    parcel2.writeNoException();
                    if (dhcpInfo != null) {
                        parcel2.writeInt(1);
                        dhcpInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsScanAlwaysAvailable = isScanAlwaysAvailable();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsScanAlwaysAvailable ? 1 : 0);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zAcquireWifiLock = acquireWifiLock(parcel.readStrongBinder(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0 ? WorkSource.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zAcquireWifiLock ? 1 : 0);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateWifiLockWorkSource(parcel.readStrongBinder(), parcel.readInt() != 0 ? WorkSource.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zReleaseWifiLock = releaseWifiLock(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    parcel2.writeInt(zReleaseWifiLock ? 1 : 0);
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    initializeMulticastFiltering();
                    parcel2.writeNoException();
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsMulticastEnabled = isMulticastEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsMulticastEnabled ? 1 : 0);
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    acquireMulticastLock(parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    releaseMulticastLock();
                    parcel2.writeNoException();
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    updateInterfaceIpState(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStartSoftAp = startSoftAp(parcel.readInt() != 0 ? WifiConfiguration.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zStartSoftAp ? 1 : 0);
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zStopSoftAp = stopSoftAp();
                    parcel2.writeNoException();
                    parcel2.writeInt(zStopSoftAp ? 1 : 0);
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iStartLocalOnlyHotspot = startLocalOnlyHotspot(parcel.readInt() != 0 ? Messenger.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iStartLocalOnlyHotspot);
                    return true;
                case 44:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopLocalOnlyHotspot();
                    parcel2.writeNoException();
                    return true;
                case 45:
                    parcel.enforceInterface(DESCRIPTOR);
                    startWatchLocalOnlyHotspot(parcel.readInt() != 0 ? Messenger.CREATOR.createFromParcel(parcel) : null, parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 46:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopWatchLocalOnlyHotspot();
                    parcel2.writeNoException();
                    return true;
                case 47:
                    parcel.enforceInterface(DESCRIPTOR);
                    int wifiApEnabledState = getWifiApEnabledState();
                    parcel2.writeNoException();
                    parcel2.writeInt(wifiApEnabledState);
                    return true;
                case 48:
                    parcel.enforceInterface(DESCRIPTOR);
                    WifiConfiguration wifiApConfiguration = getWifiApConfiguration();
                    parcel2.writeNoException();
                    if (wifiApConfiguration != null) {
                        parcel2.writeInt(1);
                        wifiApConfiguration.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 49:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean wifiApConfiguration2 = setWifiApConfiguration(parcel.readInt() != 0 ? WifiConfiguration.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(wifiApConfiguration2 ? 1 : 0);
                    return true;
                case 50:
                    parcel.enforceInterface(DESCRIPTOR);
                    Messenger wifiServiceMessenger = getWifiServiceMessenger(parcel.readString());
                    parcel2.writeNoException();
                    if (wifiServiceMessenger != null) {
                        parcel2.writeInt(1);
                        wifiServiceMessenger.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 51:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableTdls(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 52:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableTdlsWithMacAddress(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 53:
                    parcel.enforceInterface(DESCRIPTOR);
                    String currentNetworkWpsNfcConfigurationToken = getCurrentNetworkWpsNfcConfigurationToken();
                    parcel2.writeNoException();
                    parcel2.writeString(currentNetworkWpsNfcConfigurationToken);
                    return true;
                case 54:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableVerboseLogging(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 55:
                    parcel.enforceInterface(DESCRIPTOR);
                    int verboseLoggingLevel = getVerboseLoggingLevel();
                    parcel2.writeNoException();
                    parcel2.writeInt(verboseLoggingLevel);
                    return true;
                case 56:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableWifiConnectivityManager(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 57:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableEphemeralNetwork(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 58:
                    parcel.enforceInterface(DESCRIPTOR);
                    factoryReset(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 59:
                    parcel.enforceInterface(DESCRIPTOR);
                    Network currentNetwork = getCurrentNetwork();
                    parcel2.writeNoException();
                    if (currentNetwork != null) {
                        parcel2.writeInt(1);
                        currentNetwork.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 60:
                    parcel.enforceInterface(DESCRIPTOR);
                    byte[] bArrRetrieveBackupData = retrieveBackupData();
                    parcel2.writeNoException();
                    parcel2.writeByteArray(bArrRetrieveBackupData);
                    return true;
                case 61:
                    parcel.enforceInterface(DESCRIPTOR);
                    restoreBackupData(parcel.createByteArray());
                    parcel2.writeNoException();
                    return true;
                case 62:
                    parcel.enforceInterface(DESCRIPTOR);
                    restoreSupplicantBackupData(parcel.createByteArray(), parcel.createByteArray());
                    parcel2.writeNoException();
                    return true;
                case 63:
                    parcel.enforceInterface(DESCRIPTOR);
                    startSubscriptionProvisioning(parcel.readInt() != 0 ? OsuProvider.CREATOR.createFromParcel(parcel) : null, IProvisioningCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 64:
                    parcel.enforceInterface(DESCRIPTOR);
                    registerSoftApCallback(parcel.readStrongBinder(), ISoftApCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 65:
                    parcel.enforceInterface(DESCRIPTOR);
                    unregisterSoftApCallback(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 66:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<HotspotClient> hotspotClients = getHotspotClients();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(hotspotClients);
                    return true;
                case 67:
                    parcel.enforceInterface(DESCRIPTOR);
                    String clientIp = getClientIp(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(clientIp);
                    return true;
                case 68:
                    parcel.enforceInterface(DESCRIPTOR);
                    String clientDeviceName = getClientDeviceName(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(clientDeviceName);
                    return true;
                case 69:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zBlockClient = blockClient(parcel.readInt() != 0 ? HotspotClient.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zBlockClient ? 1 : 0);
                    return true;
                case 70:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zUnblockClient = unblockClient(parcel.readInt() != 0 ? HotspotClient.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    parcel2.writeInt(zUnblockClient ? 1 : 0);
                    return true;
                case 71:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsAllDevicesAllowed = isAllDevicesAllowed();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsAllDevicesAllowed ? 1 : 0);
                    return true;
                case 72:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean allDevicesAllowed = setAllDevicesAllowed(parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    parcel2.writeInt(allDevicesAllowed ? 1 : 0);
                    return true;
                case 73:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zAllowDevice = allowDevice(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zAllowDevice ? 1 : 0);
                    return true;
                case 74:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zDisallowDevice = disallowDevice(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zDisallowDevice ? 1 : 0);
                    return true;
                case 75:
                    parcel.enforceInterface(DESCRIPTOR);
                    List<HotspotClient> allowedDevices = getAllowedDevices();
                    parcel2.writeNoException();
                    parcel2.writeTypedList(allowedDevices);
                    return true;
                case 76:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPowerSavingMode(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IWifiManager {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public int getSupportedFeatures() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WifiActivityEnergyInfo reportActivityInfo() throws RemoteException {
                WifiActivityEnergyInfo wifiActivityEnergyInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        wifiActivityEnergyInfoCreateFromParcel = WifiActivityEnergyInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        wifiActivityEnergyInfoCreateFromParcel = null;
                    }
                    return wifiActivityEnergyInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void requestActivityInfo(ResultReceiver resultReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resultReceiver != null) {
                        parcelObtain.writeInt(1);
                        resultReceiver.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getConfiguredNetworks() throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ParceledListSlice getPrivilegedConfiguredNetworks() throws RemoteException {
                ParceledListSlice parceledListSliceCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        parceledListSliceCreateFromParcel = ParceledListSlice.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        parceledListSliceCreateFromParcel = null;
                    }
                    return parceledListSliceCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WifiConfiguration getMatchingWifiConfig(ScanResult scanResult) throws RemoteException {
                WifiConfiguration wifiConfigurationCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        parcelObtain.writeInt(1);
                        scanResult.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        wifiConfigurationCreateFromParcel = WifiConfiguration.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        wifiConfigurationCreateFromParcel = null;
                    }
                    return wifiConfigurationCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<WifiConfiguration> getAllMatchingWifiConfigs(ScanResult scanResult) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        parcelObtain.writeInt(1);
                        scanResult.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(WifiConfiguration.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<OsuProvider> getMatchingOsuProviders(ScanResult scanResult) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (scanResult != null) {
                        parcelObtain.writeInt(1);
                        scanResult.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(OsuProvider.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int addOrUpdateNetwork(WifiConfiguration wifiConfiguration, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (wifiConfiguration != null) {
                        parcelObtain.writeInt(1);
                        wifiConfiguration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean addOrUpdatePasspointConfiguration(PasspointConfiguration passpointConfiguration, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (passpointConfiguration != null) {
                        parcelObtain.writeInt(1);
                        passpointConfiguration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removePasspointConfiguration(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<PasspointConfiguration> getPasspointConfigurations() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(PasspointConfiguration.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void queryPasspointIcon(long j, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int matchProviderWithCurrentNetwork(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void deauthenticateNetwork(long j, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeLong(j);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean removeNetwork(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean enableNetwork(int i, boolean z, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean disableNetwork(int i, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startScan(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<ScanResult> getScanResults(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(ScanResult.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disconnect(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reconnect(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reassociate(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WifiInfo getConnectionInfo(String str) throws RemoteException {
                WifiInfo wifiInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        wifiInfoCreateFromParcel = WifiInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        wifiInfoCreateFromParcel = null;
                    }
                    return wifiInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setWifiEnabled(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getWifiEnabledState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCountryCode(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCountryCode() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isDualBandSupported() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean needs5GHzToAnyApBandConversion() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public DhcpInfo getDhcpInfo() throws RemoteException {
                DhcpInfo dhcpInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        dhcpInfoCreateFromParcel = DhcpInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        dhcpInfoCreateFromParcel = null;
                    }
                    return dhcpInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isScanAlwaysAvailable() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean acquireWifiLock(IBinder iBinder, int i, String str, WorkSource workSource) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str);
                    boolean z = true;
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateWifiLockWorkSource(IBinder iBinder, WorkSource workSource) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    if (workSource != null) {
                        parcelObtain.writeInt(1);
                        workSource.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean releaseWifiLock(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void initializeMulticastFiltering() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isMulticastEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void acquireMulticastLock(IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void releaseMulticastLock() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void updateInterfaceIpState(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean startSoftAp(WifiConfiguration wifiConfiguration) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (wifiConfiguration != null) {
                        parcelObtain.writeInt(1);
                        wifiConfiguration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean stopSoftAp() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int startLocalOnlyHotspot(Messenger messenger, IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        parcelObtain.writeInt(1);
                        messenger.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopLocalOnlyHotspot() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(44, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startWatchLocalOnlyHotspot(Messenger messenger, IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (messenger != null) {
                        parcelObtain.writeInt(1);
                        messenger.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(45, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopWatchLocalOnlyHotspot() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(46, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getWifiApEnabledState() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WifiConfiguration getWifiApConfiguration() throws RemoteException {
                WifiConfiguration wifiConfigurationCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        wifiConfigurationCreateFromParcel = WifiConfiguration.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        wifiConfigurationCreateFromParcel = null;
                    }
                    return wifiConfigurationCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setWifiApConfiguration(WifiConfiguration wifiConfiguration, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (wifiConfiguration != null) {
                        parcelObtain.writeInt(1);
                        wifiConfiguration.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(49, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Messenger getWifiServiceMessenger(String str) throws RemoteException {
                Messenger messengerCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(50, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        messengerCreateFromParcel = Messenger.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        messengerCreateFromParcel = null;
                    }
                    return messengerCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableTdls(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(51, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableTdlsWithMacAddress(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(52, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCurrentNetworkWpsNfcConfigurationToken() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(53, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableVerboseLogging(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(54, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int getVerboseLoggingLevel() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(55, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableWifiConnectivityManager(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(56, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disableEphemeralNetwork(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(57, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void factoryReset(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(58, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public Network getCurrentNetwork() throws RemoteException {
                Network networkCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(59, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        networkCreateFromParcel = Network.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        networkCreateFromParcel = null;
                    }
                    return networkCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public byte[] retrieveBackupData() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(60, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createByteArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreBackupData(byte[] bArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    this.mRemote.transact(61, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void restoreSupplicantBackupData(byte[] bArr, byte[] bArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeByteArray(bArr);
                    parcelObtain.writeByteArray(bArr2);
                    this.mRemote.transact(62, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startSubscriptionProvisioning(OsuProvider osuProvider, IProvisioningCallback iProvisioningCallback) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (osuProvider != null) {
                        parcelObtain.writeInt(1);
                        osuProvider.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iProvisioningCallback != null ? iProvisioningCallback.asBinder() : null);
                    this.mRemote.transact(63, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerSoftApCallback(IBinder iBinder, ISoftApCallback iSoftApCallback, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeStrongBinder(iSoftApCallback != null ? iSoftApCallback.asBinder() : null);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(64, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void unregisterSoftApCallback(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(65, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<HotspotClient> getHotspotClients() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(66, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(HotspotClient.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getClientIp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(67, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getClientDeviceName(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(68, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean blockClient(HotspotClient hotspotClient) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (hotspotClient != null) {
                        parcelObtain.writeInt(1);
                        hotspotClient.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(69, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean unblockClient(HotspotClient hotspotClient) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = true;
                    if (hotspotClient != null) {
                        parcelObtain.writeInt(1);
                        hotspotClient.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(70, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() == 0) {
                        z = false;
                    }
                    return z;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isAllDevicesAllowed() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(71, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean setAllDevicesAllowed(boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(72, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean allowDevice(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(73, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean disallowDevice(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(74, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public List<HotspotClient> getAllowedDevices() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(75, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createTypedArrayList(HotspotClient.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPowerSavingMode(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(76, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
