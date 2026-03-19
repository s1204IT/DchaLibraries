package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.NanBandSpecificConfig;
import android.hardware.wifi.V1_0.NanConfigRequest;
import android.hardware.wifi.V1_0.NanEnableRequest;
import android.hardware.wifi.V1_0.NanInitiateDataPathRequest;
import android.hardware.wifi.V1_0.NanPublishRequest;
import android.hardware.wifi.V1_0.NanRespondToDataPathIndicationRequest;
import android.hardware.wifi.V1_0.NanSubscribeRequest;
import android.hardware.wifi.V1_0.NanTransmitFollowupRequest;
import android.hardware.wifi.V1_0.WifiStatus;
import android.hardware.wifi.V1_2.IWifiNanIface;
import android.hardware.wifi.V1_2.NanConfigRequestSupplemental;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.IHwInterface;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.server.wifi.aware.WifiAwareShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import libcore.util.HexEncoding;

public class WifiAwareNativeApi implements WifiAwareShellCommand.DelegatedShellCommand {
    static final String PARAM_DISCOVERY_BEACON_INTERVAL_MS = "disc_beacon_interval_ms";
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_DEFAULT = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_IDLE = 0;
    private static final int PARAM_DISCOVERY_BEACON_INTERVAL_MS_INACTIVE = 0;
    static final String PARAM_DW_24GHZ = "dw_24ghz";
    private static final int PARAM_DW_24GHZ_DEFAULT = -1;
    private static final int PARAM_DW_24GHZ_IDLE = 4;
    private static final int PARAM_DW_24GHZ_INACTIVE = 4;
    static final String PARAM_DW_5GHZ = "dw_5ghz";
    private static final int PARAM_DW_5GHZ_DEFAULT = -1;
    private static final int PARAM_DW_5GHZ_IDLE = 0;
    private static final int PARAM_DW_5GHZ_INACTIVE = 0;
    static final String PARAM_ENABLE_DW_EARLY_TERM = "enable_dw_early_term";
    private static final int PARAM_ENABLE_DW_EARLY_TERM_DEFAULT = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_IDLE = 0;
    private static final int PARAM_ENABLE_DW_EARLY_TERM_INACTIVE = 0;
    static final String PARAM_MAC_RANDOM_INTERVAL_SEC = "mac_random_interval_sec";
    private static final int PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT = 1800;
    static final String PARAM_NUM_SS_IN_DISCOVERY = "num_ss_in_discovery";
    private static final int PARAM_NUM_SS_IN_DISCOVERY_DEFAULT = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_IDLE = 0;
    private static final int PARAM_NUM_SS_IN_DISCOVERY_INACTIVE = 0;
    static final String POWER_PARAM_DEFAULT_KEY = "default";
    static final String POWER_PARAM_IDLE_KEY = "idle";
    static final String POWER_PARAM_INACTIVE_KEY = "inactive";
    private static final String SERVICE_NAME_FOR_OOB_DATA_PATH = "Wi-Fi Aware Data Path";
    private static final String TAG = "WifiAwareNativeApi";
    private static final boolean VDBG = false;
    private final WifiAwareNativeManager mHal;
    private SparseIntArray mTransactionIds;
    boolean mDbg = false;
    private Map<String, Map<String, Integer>> mSettablePowerParameters = new HashMap();
    private Map<String, Integer> mSettableParameters = new HashMap();

    public WifiAwareNativeApi(WifiAwareNativeManager wifiAwareNativeManager) {
        this.mHal = wifiAwareNativeManager;
        onReset();
    }

    private void recordTransactionId(int i) {
    }

    public IWifiNanIface mockableCastTo_1_2(android.hardware.wifi.V1_0.IWifiNanIface iWifiNanIface) {
        return IWifiNanIface.castFrom((IHwInterface) iWifiNanIface);
    }

    @Override
    public int onCommand(ShellCommand shellCommand) {
        byte b;
        PrintWriter errPrintWriter = shellCommand.getErrPrintWriter();
        String nextArgRequired = shellCommand.getNextArgRequired();
        int iHashCode = nextArgRequired.hashCode();
        if (iHashCode != -502265894) {
            if (iHashCode != -287648818) {
                if (iHashCode != 102230) {
                    b = (iHashCode == 113762 && nextArgRequired.equals("set")) ? (byte) 0 : (byte) -1;
                } else if (nextArgRequired.equals("get")) {
                    b = 2;
                }
            } else if (nextArgRequired.equals("get-power")) {
                b = 3;
            }
        } else if (nextArgRequired.equals("set-power")) {
            b = 1;
        }
        switch (b) {
            case 0:
                String nextArgRequired2 = shellCommand.getNextArgRequired();
                if (this.mSettableParameters.containsKey(nextArgRequired2)) {
                    String nextArgRequired3 = shellCommand.getNextArgRequired();
                    try {
                        this.mSettableParameters.put(nextArgRequired2, Integer.valueOf(Integer.valueOf(nextArgRequired3).intValue()));
                    } catch (NumberFormatException e) {
                        errPrintWriter.println("Can't convert value to integer -- '" + nextArgRequired3 + "'");
                        return -1;
                    }
                } else {
                    errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired2 + "'");
                }
                break;
            case 1:
                String nextArgRequired4 = shellCommand.getNextArgRequired();
                String nextArgRequired5 = shellCommand.getNextArgRequired();
                String nextArgRequired6 = shellCommand.getNextArgRequired();
                if (!this.mSettablePowerParameters.containsKey(nextArgRequired4)) {
                    errPrintWriter.println("Unknown mode name -- '" + nextArgRequired4 + "'");
                } else if (this.mSettablePowerParameters.get(nextArgRequired4).containsKey(nextArgRequired5)) {
                    try {
                        this.mSettablePowerParameters.get(nextArgRequired4).put(nextArgRequired5, Integer.valueOf(Integer.valueOf(nextArgRequired6).intValue()));
                    } catch (NumberFormatException e2) {
                        errPrintWriter.println("Can't convert value to integer -- '" + nextArgRequired6 + "'");
                        return -1;
                    }
                } else {
                    errPrintWriter.println("Unknown parameter name '" + nextArgRequired5 + "' in mode '" + nextArgRequired4 + "'");
                }
                break;
            case 2:
                String nextArgRequired7 = shellCommand.getNextArgRequired();
                if (this.mSettableParameters.containsKey(nextArgRequired7)) {
                    shellCommand.getOutPrintWriter().println(this.mSettableParameters.get(nextArgRequired7).intValue());
                } else {
                    errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired7 + "'");
                }
                break;
            case 3:
                String nextArgRequired8 = shellCommand.getNextArgRequired();
                String nextArgRequired9 = shellCommand.getNextArgRequired();
                if (!this.mSettablePowerParameters.containsKey(nextArgRequired8)) {
                    errPrintWriter.println("Unknown mode -- '" + nextArgRequired8 + "'");
                } else if (this.mSettablePowerParameters.get(nextArgRequired8).containsKey(nextArgRequired9)) {
                    shellCommand.getOutPrintWriter().println(this.mSettablePowerParameters.get(nextArgRequired8).get(nextArgRequired9).intValue());
                } else {
                    errPrintWriter.println("Unknown parameter name -- '" + nextArgRequired9 + "' in mode '" + nextArgRequired8 + "'");
                }
                break;
            default:
                errPrintWriter.println("Unknown 'wifiaware native_api <cmd>'");
                break;
        }
        return -1;
    }

    @Override
    public void onReset() {
        HashMap map = new HashMap();
        map.put(PARAM_DW_24GHZ, -1);
        map.put(PARAM_DW_5GHZ, -1);
        map.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        map.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        map.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        HashMap map2 = new HashMap();
        map2.put(PARAM_DW_24GHZ, 4);
        map2.put(PARAM_DW_5GHZ, 0);
        map2.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        map2.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        map2.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        HashMap map3 = new HashMap();
        map3.put(PARAM_DW_24GHZ, 4);
        map3.put(PARAM_DW_5GHZ, 0);
        map3.put(PARAM_DISCOVERY_BEACON_INTERVAL_MS, 0);
        map3.put(PARAM_NUM_SS_IN_DISCOVERY, 0);
        map3.put(PARAM_ENABLE_DW_EARLY_TERM, 0);
        this.mSettablePowerParameters.put("default", map);
        this.mSettablePowerParameters.put(POWER_PARAM_INACTIVE_KEY, map2);
        this.mSettablePowerParameters.put(POWER_PARAM_IDLE_KEY, map3);
        this.mSettableParameters.put(PARAM_MAC_RANDOM_INTERVAL_SEC, Integer.valueOf(PARAM_MAC_RANDOM_INTERVAL_SEC_DEFAULT));
    }

    @Override
    public void onHelp(String str, ShellCommand shellCommand) {
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        outPrintWriter.println("  " + str);
        outPrintWriter.println("    set <name> <value>: sets named parameter to value. Names: " + this.mSettableParameters.keySet());
        outPrintWriter.println("    set-power <mode> <name> <value>: sets named power parameter to value. Modes: " + this.mSettablePowerParameters.keySet() + ", Names: " + this.mSettablePowerParameters.get("default").keySet());
        StringBuilder sb = new StringBuilder();
        sb.append("    get <name>: gets named parameter value. Names: ");
        sb.append(this.mSettableParameters.keySet());
        outPrintWriter.println(sb.toString());
        outPrintWriter.println("    get-power <mode> <name>: gets named parameter value. Modes: " + this.mSettablePowerParameters.keySet() + ", Names: " + this.mSettablePowerParameters.get("default").keySet());
    }

    public boolean getCapabilities(short s) {
        if (this.mDbg) {
            Log.v(TAG, "getCapabilities: transactionId=" + ((int) s));
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "getCapabilities: null interface");
            return false;
        }
        try {
            WifiStatus capabilitiesRequest = wifiNanIface.getCapabilitiesRequest(s);
            if (capabilitiesRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "getCapabilities: error: " + statusString(capabilitiesRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "getCapabilities: exception: " + e);
            return false;
        }
    }

    public boolean enableAndConfigure(short s, ConfigRequest configRequest, boolean z, boolean z2, boolean z3, boolean z4) {
        WifiStatus wifiStatusConfigRequest;
        if (this.mDbg) {
            Log.v(TAG, "enableAndConfigure: transactionId=" + ((int) s) + ", configRequest=" + configRequest + ", notifyIdentityChange=" + z + ", initialConfiguration=" + z2 + ", isInteractive=" + z3 + ", isIdle=" + z4);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "enableAndConfigure: null interface");
            return false;
        }
        IWifiNanIface iWifiNanIfaceMockableCastTo_1_2 = mockableCastTo_1_2(wifiNanIface);
        NanConfigRequestSupplemental nanConfigRequestSupplemental = new NanConfigRequestSupplemental();
        if (iWifiNanIfaceMockableCastTo_1_2 != null) {
            nanConfigRequestSupplemental.discoveryBeaconIntervalMs = 0;
            nanConfigRequestSupplemental.numberOfSpatialStreamsInDiscovery = 0;
            nanConfigRequestSupplemental.enableDiscoveryWindowEarlyTermination = false;
            nanConfigRequestSupplemental.enableRanging = true;
        }
        try {
            if (z2) {
                NanEnableRequest nanEnableRequest = new NanEnableRequest();
                nanEnableRequest.operateInBand[0] = true;
                nanEnableRequest.operateInBand[1] = configRequest.mSupport5gBand;
                nanEnableRequest.hopCountMax = (byte) 2;
                nanEnableRequest.configParams.masterPref = (byte) configRequest.mMasterPreference;
                nanEnableRequest.configParams.disableDiscoveryAddressChangeIndication = !z;
                nanEnableRequest.configParams.disableStartedClusterIndication = !z;
                nanEnableRequest.configParams.disableJoinedClusterIndication = !z;
                nanEnableRequest.configParams.includePublishServiceIdsInBeacon = true;
                nanEnableRequest.configParams.numberOfPublishServiceIdsInBeacon = (byte) 0;
                nanEnableRequest.configParams.includeSubscribeServiceIdsInBeacon = true;
                nanEnableRequest.configParams.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
                nanEnableRequest.configParams.rssiWindowSize = (short) 8;
                nanEnableRequest.configParams.macAddressRandomizationIntervalSec = this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC).intValue();
                NanBandSpecificConfig nanBandSpecificConfig = new NanBandSpecificConfig();
                nanBandSpecificConfig.rssiClose = (byte) 60;
                nanBandSpecificConfig.rssiMiddle = (byte) 70;
                nanBandSpecificConfig.rssiCloseProximity = (byte) 60;
                nanBandSpecificConfig.dwellTimeMs = (byte) -56;
                nanBandSpecificConfig.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[0] == -1) {
                    nanBandSpecificConfig.validDiscoveryWindowIntervalVal = false;
                } else {
                    nanBandSpecificConfig.validDiscoveryWindowIntervalVal = true;
                    nanBandSpecificConfig.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[0];
                }
                nanEnableRequest.configParams.bandSpecificConfig[0] = nanBandSpecificConfig;
                NanBandSpecificConfig nanBandSpecificConfig2 = new NanBandSpecificConfig();
                nanBandSpecificConfig2.rssiClose = (byte) 60;
                nanBandSpecificConfig2.rssiMiddle = (byte) 75;
                nanBandSpecificConfig2.rssiCloseProximity = (byte) 60;
                nanBandSpecificConfig2.dwellTimeMs = (byte) -56;
                nanBandSpecificConfig2.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[1] == -1) {
                    nanBandSpecificConfig2.validDiscoveryWindowIntervalVal = false;
                } else {
                    nanBandSpecificConfig2.validDiscoveryWindowIntervalVal = true;
                    nanBandSpecificConfig2.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[1];
                }
                nanEnableRequest.configParams.bandSpecificConfig[1] = nanBandSpecificConfig2;
                nanEnableRequest.debugConfigs.validClusterIdVals = true;
                nanEnableRequest.debugConfigs.clusterIdTopRangeVal = (short) configRequest.mClusterHigh;
                nanEnableRequest.debugConfigs.clusterIdBottomRangeVal = (short) configRequest.mClusterLow;
                nanEnableRequest.debugConfigs.validIntfAddrVal = false;
                nanEnableRequest.debugConfigs.validOuiVal = false;
                nanEnableRequest.debugConfigs.ouiVal = 0;
                nanEnableRequest.debugConfigs.validRandomFactorForceVal = false;
                nanEnableRequest.debugConfigs.randomFactorForceVal = (byte) 0;
                nanEnableRequest.debugConfigs.validHopCountForceVal = false;
                nanEnableRequest.debugConfigs.hopCountForceVal = (byte) 0;
                nanEnableRequest.debugConfigs.validDiscoveryChannelVal = false;
                nanEnableRequest.debugConfigs.discoveryChannelMhzVal[0] = 0;
                nanEnableRequest.debugConfigs.discoveryChannelMhzVal[1] = 0;
                nanEnableRequest.debugConfigs.validUseBeaconsInBandVal = false;
                nanEnableRequest.debugConfigs.useBeaconsInBandVal[0] = true;
                nanEnableRequest.debugConfigs.useBeaconsInBandVal[1] = true;
                nanEnableRequest.debugConfigs.validUseSdfInBandVal = false;
                nanEnableRequest.debugConfigs.useSdfInBandVal[0] = true;
                nanEnableRequest.debugConfigs.useSdfInBandVal[1] = true;
                updateConfigForPowerSettings(nanEnableRequest.configParams, nanConfigRequestSupplemental, z3, z4);
                if (iWifiNanIfaceMockableCastTo_1_2 != null) {
                    wifiStatusConfigRequest = iWifiNanIfaceMockableCastTo_1_2.enableRequest_1_2(s, nanEnableRequest, nanConfigRequestSupplemental);
                } else {
                    wifiStatusConfigRequest = wifiNanIface.enableRequest(s, nanEnableRequest);
                }
            } else {
                NanConfigRequest nanConfigRequest = new NanConfigRequest();
                nanConfigRequest.masterPref = (byte) configRequest.mMasterPreference;
                nanConfigRequest.disableDiscoveryAddressChangeIndication = !z;
                nanConfigRequest.disableStartedClusterIndication = !z;
                nanConfigRequest.disableJoinedClusterIndication = !z;
                nanConfigRequest.includePublishServiceIdsInBeacon = true;
                nanConfigRequest.numberOfPublishServiceIdsInBeacon = (byte) 0;
                nanConfigRequest.includeSubscribeServiceIdsInBeacon = true;
                nanConfigRequest.numberOfSubscribeServiceIdsInBeacon = (byte) 0;
                nanConfigRequest.rssiWindowSize = (short) 8;
                nanConfigRequest.macAddressRandomizationIntervalSec = this.mSettableParameters.get(PARAM_MAC_RANDOM_INTERVAL_SEC).intValue();
                NanBandSpecificConfig nanBandSpecificConfig3 = new NanBandSpecificConfig();
                nanBandSpecificConfig3.rssiClose = (byte) 60;
                nanBandSpecificConfig3.rssiMiddle = (byte) 70;
                nanBandSpecificConfig3.rssiCloseProximity = (byte) 60;
                nanBandSpecificConfig3.dwellTimeMs = (byte) -56;
                nanBandSpecificConfig3.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[0] == -1) {
                    nanBandSpecificConfig3.validDiscoveryWindowIntervalVal = false;
                } else {
                    nanBandSpecificConfig3.validDiscoveryWindowIntervalVal = true;
                    nanBandSpecificConfig3.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[0];
                }
                nanConfigRequest.bandSpecificConfig[0] = nanBandSpecificConfig3;
                NanBandSpecificConfig nanBandSpecificConfig4 = new NanBandSpecificConfig();
                nanBandSpecificConfig4.rssiClose = (byte) 60;
                nanBandSpecificConfig4.rssiMiddle = (byte) 75;
                nanBandSpecificConfig4.rssiCloseProximity = (byte) 60;
                nanBandSpecificConfig4.dwellTimeMs = (byte) -56;
                nanBandSpecificConfig4.scanPeriodSec = (short) 20;
                if (configRequest.mDiscoveryWindowInterval[1] == -1) {
                    nanBandSpecificConfig4.validDiscoveryWindowIntervalVal = false;
                } else {
                    nanBandSpecificConfig4.validDiscoveryWindowIntervalVal = true;
                    nanBandSpecificConfig4.discoveryWindowIntervalVal = (byte) configRequest.mDiscoveryWindowInterval[1];
                }
                nanConfigRequest.bandSpecificConfig[1] = nanBandSpecificConfig4;
                updateConfigForPowerSettings(nanConfigRequest, nanConfigRequestSupplemental, z3, z4);
                if (iWifiNanIfaceMockableCastTo_1_2 != null) {
                    wifiStatusConfigRequest = iWifiNanIfaceMockableCastTo_1_2.configRequest_1_2(s, nanConfigRequest, nanConfigRequestSupplemental);
                } else {
                    wifiStatusConfigRequest = wifiNanIface.configRequest(s, nanConfigRequest);
                }
            }
            if (wifiStatusConfigRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "enableAndConfigure: error: " + statusString(wifiStatusConfigRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "enableAndConfigure: exception: " + e);
            return false;
        }
    }

    public boolean disable(short s) {
        if (this.mDbg) {
            Log.d(TAG, "disable");
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "disable: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusDisableRequest = wifiNanIface.disableRequest(s);
            if (wifiStatusDisableRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "disable: error: " + statusString(wifiStatusDisableRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "disable: exception: " + e);
            return false;
        }
    }

    public boolean publish(short s, byte b, PublishConfig publishConfig) {
        if (this.mDbg) {
            Log.d(TAG, "publish: transactionId=" + ((int) s) + ", publishId=" + ((int) b) + ", config=" + publishConfig);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "publish: null interface");
            return false;
        }
        NanPublishRequest nanPublishRequest = new NanPublishRequest();
        nanPublishRequest.baseConfigs.sessionId = b;
        nanPublishRequest.baseConfigs.ttlSec = (short) publishConfig.mTtlSec;
        nanPublishRequest.baseConfigs.discoveryWindowPeriod = (short) 1;
        nanPublishRequest.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(publishConfig.mServiceName, nanPublishRequest.baseConfigs.serviceName);
        nanPublishRequest.baseConfigs.discoveryMatchIndicator = 2;
        convertNativeByteArrayToArrayList(publishConfig.mServiceSpecificInfo, nanPublishRequest.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(publishConfig.mMatchFilter, publishConfig.mPublishType == 0 ? nanPublishRequest.baseConfigs.txMatchFilter : nanPublishRequest.baseConfigs.rxMatchFilter);
        nanPublishRequest.baseConfigs.useRssiThreshold = false;
        nanPublishRequest.baseConfigs.disableDiscoveryTerminationIndication = !publishConfig.mEnableTerminateNotification;
        nanPublishRequest.baseConfigs.disableMatchExpirationIndication = true;
        nanPublishRequest.baseConfigs.disableFollowupReceivedIndication = false;
        nanPublishRequest.autoAcceptDataPathRequests = false;
        nanPublishRequest.baseConfigs.rangingRequired = publishConfig.mEnableRanging;
        nanPublishRequest.baseConfigs.securityConfig.securityType = 0;
        nanPublishRequest.publishType = publishConfig.mPublishType;
        nanPublishRequest.txType = 0;
        try {
            WifiStatus wifiStatusStartPublishRequest = wifiNanIface.startPublishRequest(s, nanPublishRequest);
            if (wifiStatusStartPublishRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "publish: error: " + statusString(wifiStatusStartPublishRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "publish: exception: " + e);
            return false;
        }
    }

    public boolean subscribe(short s, byte b, SubscribeConfig subscribeConfig) {
        if (this.mDbg) {
            Log.d(TAG, "subscribe: transactionId=" + ((int) s) + ", subscribeId=" + ((int) b) + ", config=" + subscribeConfig);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "subscribe: null interface");
            return false;
        }
        NanSubscribeRequest nanSubscribeRequest = new NanSubscribeRequest();
        nanSubscribeRequest.baseConfigs.sessionId = b;
        nanSubscribeRequest.baseConfigs.ttlSec = (short) subscribeConfig.mTtlSec;
        nanSubscribeRequest.baseConfigs.discoveryWindowPeriod = (short) 1;
        nanSubscribeRequest.baseConfigs.discoveryCount = (byte) 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceName, nanSubscribeRequest.baseConfigs.serviceName);
        nanSubscribeRequest.baseConfigs.discoveryMatchIndicator = 0;
        convertNativeByteArrayToArrayList(subscribeConfig.mServiceSpecificInfo, nanSubscribeRequest.baseConfigs.serviceSpecificInfo);
        convertNativeByteArrayToArrayList(subscribeConfig.mMatchFilter, subscribeConfig.mSubscribeType == 1 ? nanSubscribeRequest.baseConfigs.txMatchFilter : nanSubscribeRequest.baseConfigs.rxMatchFilter);
        nanSubscribeRequest.baseConfigs.useRssiThreshold = false;
        nanSubscribeRequest.baseConfigs.disableDiscoveryTerminationIndication = !subscribeConfig.mEnableTerminateNotification;
        nanSubscribeRequest.baseConfigs.disableMatchExpirationIndication = true;
        nanSubscribeRequest.baseConfigs.disableFollowupReceivedIndication = false;
        nanSubscribeRequest.baseConfigs.rangingRequired = subscribeConfig.mMinDistanceMmSet || subscribeConfig.mMaxDistanceMmSet;
        nanSubscribeRequest.baseConfigs.configRangingIndications = 0;
        if (subscribeConfig.mMinDistanceMmSet) {
            nanSubscribeRequest.baseConfigs.distanceEgressCm = (short) Math.min(subscribeConfig.mMinDistanceMm / 10, 32767);
            nanSubscribeRequest.baseConfigs.configRangingIndications |= 4;
        }
        if (subscribeConfig.mMaxDistanceMmSet) {
            nanSubscribeRequest.baseConfigs.distanceIngressCm = (short) Math.min(subscribeConfig.mMaxDistanceMm / 10, 32767);
            nanSubscribeRequest.baseConfigs.configRangingIndications |= 2;
        }
        nanSubscribeRequest.baseConfigs.securityConfig.securityType = 0;
        nanSubscribeRequest.subscribeType = subscribeConfig.mSubscribeType;
        try {
            WifiStatus wifiStatusStartSubscribeRequest = wifiNanIface.startSubscribeRequest(s, nanSubscribeRequest);
            if (wifiStatusStartSubscribeRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "subscribe: error: " + statusString(wifiStatusStartSubscribeRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "subscribe: exception: " + e);
            return false;
        }
    }

    public boolean sendMessage(short s, byte b, int i, byte[] bArr, byte[] bArr2, int i2) {
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("sendMessage: transactionId=");
            sb.append((int) s);
            sb.append(", pubSubId=");
            sb.append((int) b);
            sb.append(", requestorInstanceId=");
            sb.append(i);
            sb.append(", dest=");
            sb.append(String.valueOf(HexEncoding.encode(bArr)));
            sb.append(", messageId=");
            sb.append(i2);
            sb.append(", message=");
            sb.append(bArr2 == null ? "<null>" : HexEncoding.encode(bArr2));
            sb.append(", message.length=");
            sb.append(bArr2 == null ? 0 : bArr2.length);
            Log.d(TAG, sb.toString());
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "sendMessage: null interface");
            return false;
        }
        NanTransmitFollowupRequest nanTransmitFollowupRequest = new NanTransmitFollowupRequest();
        nanTransmitFollowupRequest.discoverySessionId = b;
        nanTransmitFollowupRequest.peerId = i;
        copyArray(bArr, nanTransmitFollowupRequest.addr);
        nanTransmitFollowupRequest.isHighPriority = false;
        nanTransmitFollowupRequest.shouldUseDiscoveryWindow = true;
        convertNativeByteArrayToArrayList(bArr2, nanTransmitFollowupRequest.serviceSpecificInfo);
        nanTransmitFollowupRequest.disableFollowupResultIndication = false;
        try {
            WifiStatus wifiStatusTransmitFollowupRequest = wifiNanIface.transmitFollowupRequest(s, nanTransmitFollowupRequest);
            if (wifiStatusTransmitFollowupRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "sendMessage: error: " + statusString(wifiStatusTransmitFollowupRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage: exception: " + e);
            return false;
        }
    }

    public boolean stopPublish(short s, byte b) {
        if (this.mDbg) {
            Log.d(TAG, "stopPublish: transactionId=" + ((int) s) + ", pubSubId=" + ((int) b));
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "stopPublish: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusStopPublishRequest = wifiNanIface.stopPublishRequest(s, b);
            if (wifiStatusStopPublishRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "stopPublish: error: " + statusString(wifiStatusStopPublishRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopPublish: exception: " + e);
            return false;
        }
    }

    public boolean stopSubscribe(short s, byte b) {
        if (this.mDbg) {
            Log.d(TAG, "stopSubscribe: transactionId=" + ((int) s) + ", pubSubId=" + ((int) b));
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "stopSubscribe: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusStopSubscribeRequest = wifiNanIface.stopSubscribeRequest(s, b);
            if (wifiStatusStopSubscribeRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "stopSubscribe: error: " + statusString(wifiStatusStopSubscribeRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "stopSubscribe: exception: " + e);
            return false;
        }
    }

    public boolean createAwareNetworkInterface(short s, String str) {
        if (this.mDbg) {
            Log.v(TAG, "createAwareNetworkInterface: transactionId=" + ((int) s) + ", interfaceName=" + str);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "createAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusCreateDataInterfaceRequest = wifiNanIface.createDataInterfaceRequest(s, str);
            if (wifiStatusCreateDataInterfaceRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "createAwareNetworkInterface: error: " + statusString(wifiStatusCreateDataInterfaceRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "createAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean deleteAwareNetworkInterface(short s, String str) {
        if (this.mDbg) {
            Log.v(TAG, "deleteAwareNetworkInterface: transactionId=" + ((int) s) + ", interfaceName=" + str);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "deleteAwareNetworkInterface: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusDeleteDataInterfaceRequest = wifiNanIface.deleteDataInterfaceRequest(s, str);
            if (wifiStatusDeleteDataInterfaceRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "deleteAwareNetworkInterface: error: " + statusString(wifiStatusDeleteDataInterfaceRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "deleteAwareNetworkInterface: exception: " + e);
            return false;
        }
    }

    public boolean initiateDataPath(short s, int i, int i2, int i3, byte[] bArr, String str, byte[] bArr2, String str2, boolean z, Capabilities capabilities) {
        if (this.mDbg) {
            Log.v(TAG, "initiateDataPath: transactionId=" + ((int) s) + ", peerId=" + i + ", channelRequestType=" + i2 + ", channel=" + i3 + ", peer=" + String.valueOf(HexEncoding.encode(bArr)) + ", interfaceName=" + str);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "initiateDataPath: null interface");
            return false;
        }
        if (capabilities == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        }
        NanInitiateDataPathRequest nanInitiateDataPathRequest = new NanInitiateDataPathRequest();
        nanInitiateDataPathRequest.peerId = i;
        copyArray(bArr, nanInitiateDataPathRequest.peerDiscMacAddr);
        nanInitiateDataPathRequest.channelRequestType = i2;
        nanInitiateDataPathRequest.channel = i3;
        nanInitiateDataPathRequest.ifaceName = str;
        nanInitiateDataPathRequest.securityConfig.securityType = 0;
        if (bArr2 != null && bArr2.length != 0) {
            nanInitiateDataPathRequest.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
            nanInitiateDataPathRequest.securityConfig.securityType = 1;
            copyArray(bArr2, nanInitiateDataPathRequest.securityConfig.pmk);
        }
        if (str2 != null && str2.length() != 0) {
            nanInitiateDataPathRequest.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
            nanInitiateDataPathRequest.securityConfig.securityType = 2;
            convertNativeByteArrayToArrayList(str2.getBytes(), nanInitiateDataPathRequest.securityConfig.passphrase);
        }
        if (nanInitiateDataPathRequest.securityConfig.securityType != 0 && z) {
            convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), nanInitiateDataPathRequest.serviceNameOutOfBand);
        }
        try {
            WifiStatus wifiStatusInitiateDataPathRequest = wifiNanIface.initiateDataPathRequest(s, nanInitiateDataPathRequest);
            if (wifiStatusInitiateDataPathRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "initiateDataPath: error: " + statusString(wifiStatusInitiateDataPathRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "initiateDataPath: exception: " + e);
            return false;
        }
    }

    public boolean respondToDataPathRequest(short s, boolean z, int i, String str, byte[] bArr, String str2, boolean z2, Capabilities capabilities) {
        if (this.mDbg) {
            Log.v(TAG, "respondToDataPathRequest: transactionId=" + ((int) s) + ", accept=" + z + ", int ndpId=" + i + ", interfaceName=" + str);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "respondToDataPathRequest: null interface");
            return false;
        }
        if (capabilities == null) {
            Log.e(TAG, "initiateDataPath: null capabilities");
            return false;
        }
        NanRespondToDataPathIndicationRequest nanRespondToDataPathIndicationRequest = new NanRespondToDataPathIndicationRequest();
        nanRespondToDataPathIndicationRequest.acceptRequest = z;
        nanRespondToDataPathIndicationRequest.ndpInstanceId = i;
        nanRespondToDataPathIndicationRequest.ifaceName = str;
        nanRespondToDataPathIndicationRequest.securityConfig.securityType = 0;
        if (bArr != null && bArr.length != 0) {
            nanRespondToDataPathIndicationRequest.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
            nanRespondToDataPathIndicationRequest.securityConfig.securityType = 1;
            copyArray(bArr, nanRespondToDataPathIndicationRequest.securityConfig.pmk);
        }
        if (str2 != null && str2.length() != 0) {
            nanRespondToDataPathIndicationRequest.securityConfig.cipherType = getStrongestCipherSuiteType(capabilities.supportedCipherSuites);
            nanRespondToDataPathIndicationRequest.securityConfig.securityType = 2;
            convertNativeByteArrayToArrayList(str2.getBytes(), nanRespondToDataPathIndicationRequest.securityConfig.passphrase);
        }
        if (nanRespondToDataPathIndicationRequest.securityConfig.securityType != 0 && z2) {
            convertNativeByteArrayToArrayList(SERVICE_NAME_FOR_OOB_DATA_PATH.getBytes(StandardCharsets.UTF_8), nanRespondToDataPathIndicationRequest.serviceNameOutOfBand);
        }
        try {
            WifiStatus wifiStatusRespondToDataPathIndicationRequest = wifiNanIface.respondToDataPathIndicationRequest(s, nanRespondToDataPathIndicationRequest);
            if (wifiStatusRespondToDataPathIndicationRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "respondToDataPathRequest: error: " + statusString(wifiStatusRespondToDataPathIndicationRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "respondToDataPathRequest: exception: " + e);
            return false;
        }
    }

    public boolean endDataPath(short s, int i) {
        if (this.mDbg) {
            Log.v(TAG, "endDataPath: transactionId=" + ((int) s) + ", ndpId=" + i);
        }
        recordTransactionId(s);
        android.hardware.wifi.V1_0.IWifiNanIface wifiNanIface = this.mHal.getWifiNanIface();
        if (wifiNanIface == null) {
            Log.e(TAG, "endDataPath: null interface");
            return false;
        }
        try {
            WifiStatus wifiStatusTerminateDataPathRequest = wifiNanIface.terminateDataPathRequest(s, i);
            if (wifiStatusTerminateDataPathRequest.code == 0) {
                return true;
            }
            Log.e(TAG, "endDataPath: error: " + statusString(wifiStatusTerminateDataPathRequest));
            return false;
        } catch (RemoteException e) {
            Log.e(TAG, "endDataPath: exception: " + e);
            return false;
        }
    }

    private void updateConfigForPowerSettings(NanConfigRequest nanConfigRequest, NanConfigRequestSupplemental nanConfigRequestSupplemental, boolean z, boolean z2) {
        String str = "default";
        if (z2) {
            str = POWER_PARAM_IDLE_KEY;
        } else if (!z) {
            str = POWER_PARAM_INACTIVE_KEY;
        }
        updateSingleConfigForPowerSettings(nanConfigRequest.bandSpecificConfig[1], this.mSettablePowerParameters.get(str).get(PARAM_DW_5GHZ).intValue());
        updateSingleConfigForPowerSettings(nanConfigRequest.bandSpecificConfig[0], this.mSettablePowerParameters.get(str).get(PARAM_DW_24GHZ).intValue());
        nanConfigRequestSupplemental.discoveryBeaconIntervalMs = this.mSettablePowerParameters.get(str).get(PARAM_DISCOVERY_BEACON_INTERVAL_MS).intValue();
        nanConfigRequestSupplemental.numberOfSpatialStreamsInDiscovery = this.mSettablePowerParameters.get(str).get(PARAM_NUM_SS_IN_DISCOVERY).intValue();
        nanConfigRequestSupplemental.enableDiscoveryWindowEarlyTermination = this.mSettablePowerParameters.get(str).get(PARAM_ENABLE_DW_EARLY_TERM).intValue() != 0;
    }

    private void updateSingleConfigForPowerSettings(NanBandSpecificConfig nanBandSpecificConfig, int i) {
        if (i != -1) {
            nanBandSpecificConfig.validDiscoveryWindowIntervalVal = true;
            nanBandSpecificConfig.discoveryWindowIntervalVal = (byte) i;
        }
    }

    private int getStrongestCipherSuiteType(int i) {
        if ((i & 2) != 0) {
            return 2;
        }
        if ((i & 1) != 0) {
            return 1;
        }
        return 0;
    }

    private ArrayList<Byte> convertNativeByteArrayToArrayList(byte[] bArr, ArrayList<Byte> arrayList) {
        if (bArr == null) {
            bArr = new byte[0];
        }
        if (arrayList == null) {
            arrayList = new ArrayList<>(bArr.length);
        } else {
            arrayList.ensureCapacity(bArr.length);
        }
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    private void copyArray(byte[] bArr, byte[] bArr2) {
        if (bArr == null || bArr2 == null || bArr.length != bArr2.length) {
            Log.e(TAG, "copyArray error: from=" + bArr + ", to=" + bArr2);
            return;
        }
        for (int i = 0; i < bArr.length; i++) {
            bArr2[i] = bArr[i];
        }
    }

    private static String statusString(WifiStatus wifiStatus) {
        if (wifiStatus == null) {
            return "status=null";
        }
        return wifiStatus.code + " (" + wifiStatus.description + ")";
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WifiAwareNativeApi:");
        printWriter.println("  mSettableParameters: " + this.mSettableParameters);
        this.mHal.dump(fileDescriptor, printWriter, strArr);
    }
}
