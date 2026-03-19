package com.android.server.wifi.aware;

import android.hardware.wifi.V1_0.NanCapabilities;
import android.hardware.wifi.V1_0.NanClusterEventInd;
import android.hardware.wifi.V1_0.NanDataPathConfirmInd;
import android.hardware.wifi.V1_0.NanDataPathRequestInd;
import android.hardware.wifi.V1_0.NanFollowupReceivedInd;
import android.hardware.wifi.V1_0.NanMatchInd;
import android.hardware.wifi.V1_0.WifiNanStatus;
import android.hardware.wifi.V1_2.IWifiNanIfaceEventCallback;
import android.hardware.wifi.V1_2.NanDataPathScheduleUpdateInd;
import android.os.ShellCommand;
import android.util.Log;
import android.util.SparseIntArray;
import com.android.server.wifi.aware.WifiAwareShellCommand;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import libcore.util.HexEncoding;
import org.json.JSONException;
import org.json.JSONObject;

public class WifiAwareNativeCallback extends IWifiNanIfaceEventCallback.Stub implements WifiAwareShellCommand.DelegatedShellCommand {
    private static final int CB_EV_CLUSTER = 0;
    private static final int CB_EV_DATA_PATH_CONFIRM = 9;
    private static final int CB_EV_DATA_PATH_REQUEST = 8;
    private static final int CB_EV_DATA_PATH_SCHED_UPDATE = 11;
    private static final int CB_EV_DATA_PATH_TERMINATED = 10;
    private static final int CB_EV_DISABLED = 1;
    private static final int CB_EV_FOLLOWUP_RECEIVED = 6;
    private static final int CB_EV_MATCH = 4;
    private static final int CB_EV_MATCH_EXPIRED = 5;
    private static final int CB_EV_PUBLISH_TERMINATED = 2;
    private static final int CB_EV_SUBSCRIBE_TERMINATED = 3;
    private static final int CB_EV_TRANSMIT_FOLLOWUP = 7;
    private static final String TAG = "WifiAwareNativeCallback";
    private static final boolean VDBG = false;
    private final WifiAwareStateManager mWifiAwareStateManager;
    boolean mDbg = false;
    boolean mIsHal12OrLater = false;
    private SparseIntArray mCallbackCounter = new SparseIntArray();

    public WifiAwareNativeCallback(WifiAwareStateManager wifiAwareStateManager) {
        this.mWifiAwareStateManager = wifiAwareStateManager;
    }

    private void incrementCbCount(int i) {
        this.mCallbackCounter.put(i, this.mCallbackCounter.get(i) + 1);
    }

    @Override
    public int onCommand(ShellCommand shellCommand) {
        boolean z;
        PrintWriter errPrintWriter = shellCommand.getErrPrintWriter();
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        String nextArgRequired = shellCommand.getNextArgRequired();
        if (((nextArgRequired.hashCode() == -1587855368 && nextArgRequired.equals("get_cb_count")) ? (byte) 0 : (byte) -1) == 0) {
            String nextOption = shellCommand.getNextOption();
            if (nextOption != null) {
                if (!"--reset".equals(nextOption)) {
                    errPrintWriter.println("Unknown option to 'get_cb_count'");
                    return -1;
                }
                z = true;
            } else {
                z = false;
            }
            JSONObject jSONObject = new JSONObject();
            for (int i = 0; i < this.mCallbackCounter.size(); i++) {
                try {
                    jSONObject.put(Integer.toString(this.mCallbackCounter.keyAt(i)), this.mCallbackCounter.valueAt(i));
                } catch (JSONException e) {
                    Log.e(TAG, "onCommand: get_cb_count e=" + e);
                }
            }
            outPrintWriter.println(jSONObject.toString());
            if (z) {
                this.mCallbackCounter.clear();
            }
            return 0;
        }
        errPrintWriter.println("Unknown 'wifiaware native_cb <cmd>'");
        return -1;
    }

    @Override
    public void onReset() {
    }

    @Override
    public void onHelp(String str, ShellCommand shellCommand) {
        PrintWriter outPrintWriter = shellCommand.getOutPrintWriter();
        outPrintWriter.println("  " + str);
        outPrintWriter.println("    get_cb_count [--reset]: gets the number of callbacks (and optionally reset count)");
    }

    @Override
    public void notifyCapabilitiesResponse(short s, WifiNanStatus wifiNanStatus, NanCapabilities nanCapabilities) {
        if (this.mDbg) {
            Log.v(TAG, "notifyCapabilitiesResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus) + ", capabilities=" + nanCapabilities);
        }
        if (wifiNanStatus.status == 0) {
            Capabilities capabilities = new Capabilities();
            capabilities.maxConcurrentAwareClusters = nanCapabilities.maxConcurrentClusters;
            capabilities.maxPublishes = nanCapabilities.maxPublishes;
            capabilities.maxSubscribes = nanCapabilities.maxSubscribes;
            capabilities.maxServiceNameLen = nanCapabilities.maxServiceNameLen;
            capabilities.maxMatchFilterLen = nanCapabilities.maxMatchFilterLen;
            capabilities.maxTotalMatchFilterLen = nanCapabilities.maxTotalMatchFilterLen;
            capabilities.maxServiceSpecificInfoLen = nanCapabilities.maxServiceSpecificInfoLen;
            capabilities.maxExtendedServiceSpecificInfoLen = nanCapabilities.maxExtendedServiceSpecificInfoLen;
            capabilities.maxNdiInterfaces = nanCapabilities.maxNdiInterfaces;
            capabilities.maxNdpSessions = nanCapabilities.maxNdpSessions;
            capabilities.maxAppInfoLen = nanCapabilities.maxAppInfoLen;
            capabilities.maxQueuedTransmitMessages = nanCapabilities.maxQueuedTransmitFollowupMsgs;
            capabilities.maxSubscribeInterfaceAddresses = nanCapabilities.maxSubscribeInterfaceAddresses;
            capabilities.supportedCipherSuites = nanCapabilities.supportedCipherSuites;
            this.mWifiAwareStateManager.onCapabilitiesUpdateResponse(s, capabilities);
            return;
        }
        Log.e(TAG, "notifyCapabilitiesResponse: error code=" + wifiNanStatus.status + " (" + wifiNanStatus.description + ")");
    }

    @Override
    public void notifyEnableResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyEnableResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status == 10) {
            Log.wtf(TAG, "notifyEnableResponse: id=" + ((int) s) + ", already enabled!?");
        }
        if (wifiNanStatus.status == 0 || wifiNanStatus.status == 10) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(s);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(s, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyConfigResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyConfigResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onConfigSuccessResponse(s);
        } else {
            this.mWifiAwareStateManager.onConfigFailedResponse(s, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyDisableResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyDisableResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status != 0) {
            Log.e(TAG, "notifyDisableResponse: failure - code=" + wifiNanStatus.status + " (" + wifiNanStatus.description + ")");
        }
        this.mWifiAwareStateManager.onDisableResponse(s, wifiNanStatus.status);
    }

    @Override
    public void notifyStartPublishResponse(short s, WifiNanStatus wifiNanStatus, byte b) {
        if (this.mDbg) {
            Log.v(TAG, "notifyStartPublishResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus) + ", publishId=" + ((int) b));
        }
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(s, true, b);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(s, true, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyStopPublishResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyStopPublishResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status != 0) {
            Log.e(TAG, "notifyStopPublishResponse: failure - code=" + wifiNanStatus.status + " (" + wifiNanStatus.description + ")");
        }
    }

    @Override
    public void notifyStartSubscribeResponse(short s, WifiNanStatus wifiNanStatus, byte b) {
        if (this.mDbg) {
            Log.v(TAG, "notifyStartSubscribeResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus) + ", subscribeId=" + ((int) b));
        }
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onSessionConfigSuccessResponse(s, false, b);
        } else {
            this.mWifiAwareStateManager.onSessionConfigFailResponse(s, false, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyStopSubscribeResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyStopSubscribeResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status != 0) {
            Log.e(TAG, "notifyStopSubscribeResponse: failure - code=" + wifiNanStatus.status + " (" + wifiNanStatus.description + ")");
        }
    }

    @Override
    public void notifyTransmitFollowupResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyTransmitFollowupResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onMessageSendQueuedSuccessResponse(s);
        } else {
            this.mWifiAwareStateManager.onMessageSendQueuedFailResponse(s, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyCreateDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyCreateDataInterfaceResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        this.mWifiAwareStateManager.onCreateDataPathInterfaceResponse(s, wifiNanStatus.status == 0, wifiNanStatus.status);
    }

    @Override
    public void notifyDeleteDataInterfaceResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyDeleteDataInterfaceResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        this.mWifiAwareStateManager.onDeleteDataPathInterfaceResponse(s, wifiNanStatus.status == 0, wifiNanStatus.status);
    }

    @Override
    public void notifyInitiateDataPathResponse(short s, WifiNanStatus wifiNanStatus, int i) {
        if (this.mDbg) {
            Log.v(TAG, "notifyInitiateDataPathResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus) + ", ndpInstanceId=" + i);
        }
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onInitiateDataPathResponseSuccess(s, i);
        } else {
            this.mWifiAwareStateManager.onInitiateDataPathResponseFail(s, wifiNanStatus.status);
        }
    }

    @Override
    public void notifyRespondToDataPathIndicationResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyRespondToDataPathIndicationResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        this.mWifiAwareStateManager.onRespondToDataPathSetupRequestResponse(s, wifiNanStatus.status == 0, wifiNanStatus.status);
    }

    @Override
    public void notifyTerminateDataPathResponse(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "notifyTerminateDataPathResponse: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        this.mWifiAwareStateManager.onEndDataPathResponse(s, wifiNanStatus.status == 0, wifiNanStatus.status);
    }

    @Override
    public void eventClusterEvent(NanClusterEventInd nanClusterEventInd) {
        if (this.mDbg) {
            Log.v(TAG, "eventClusterEvent: eventType=" + nanClusterEventInd.eventType + ", addr=" + String.valueOf(HexEncoding.encode(nanClusterEventInd.addr)));
        }
        incrementCbCount(0);
        if (nanClusterEventInd.eventType == 0) {
            this.mWifiAwareStateManager.onInterfaceAddressChangeNotification(nanClusterEventInd.addr);
            return;
        }
        if (nanClusterEventInd.eventType == 1) {
            this.mWifiAwareStateManager.onClusterChangeNotification(0, nanClusterEventInd.addr);
            return;
        }
        if (nanClusterEventInd.eventType == 2) {
            this.mWifiAwareStateManager.onClusterChangeNotification(1, nanClusterEventInd.addr);
            return;
        }
        Log.e(TAG, "eventClusterEvent: invalid eventType=" + nanClusterEventInd.eventType);
    }

    @Override
    public void eventDisabled(WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "eventDisabled: status=" + statusString(wifiNanStatus));
        }
        incrementCbCount(1);
        this.mWifiAwareStateManager.onAwareDownNotification(wifiNanStatus.status);
    }

    @Override
    public void eventPublishTerminated(byte b, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "eventPublishTerminated: sessionId=" + ((int) b) + ", status=" + statusString(wifiNanStatus));
        }
        incrementCbCount(2);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(b, wifiNanStatus.status, true);
    }

    @Override
    public void eventSubscribeTerminated(byte b, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "eventSubscribeTerminated: sessionId=" + ((int) b) + ", status=" + statusString(wifiNanStatus));
        }
        incrementCbCount(3);
        this.mWifiAwareStateManager.onSessionTerminatedNotification(b, wifiNanStatus.status, false);
    }

    @Override
    public void eventMatch(NanMatchInd nanMatchInd) {
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("eventMatch: discoverySessionId=");
            sb.append((int) nanMatchInd.discoverySessionId);
            sb.append(", peerId=");
            sb.append(nanMatchInd.peerId);
            sb.append(", addr=");
            sb.append(String.valueOf(HexEncoding.encode(nanMatchInd.addr)));
            sb.append(", serviceSpecificInfo=");
            sb.append(Arrays.toString(convertArrayListToNativeByteArray(nanMatchInd.serviceSpecificInfo)));
            sb.append(", ssi.size()=");
            sb.append(nanMatchInd.serviceSpecificInfo == null ? 0 : nanMatchInd.serviceSpecificInfo.size());
            sb.append(", matchFilter=");
            sb.append(Arrays.toString(convertArrayListToNativeByteArray(nanMatchInd.matchFilter)));
            sb.append(", mf.size()=");
            sb.append(nanMatchInd.matchFilter != null ? nanMatchInd.matchFilter.size() : 0);
            sb.append(", rangingIndicationType=");
            sb.append(nanMatchInd.rangingIndicationType);
            sb.append(", rangingMeasurementInCm=");
            sb.append(nanMatchInd.rangingMeasurementInCm);
            Log.v(TAG, sb.toString());
        }
        incrementCbCount(4);
        this.mWifiAwareStateManager.onMatchNotification(nanMatchInd.discoverySessionId, nanMatchInd.peerId, nanMatchInd.addr, convertArrayListToNativeByteArray(nanMatchInd.serviceSpecificInfo), convertArrayListToNativeByteArray(nanMatchInd.matchFilter), nanMatchInd.rangingIndicationType, nanMatchInd.rangingMeasurementInCm * 10);
    }

    @Override
    public void eventMatchExpired(byte b, int i) {
        if (this.mDbg) {
            Log.v(TAG, "eventMatchExpired: discoverySessionId=" + ((int) b) + ", peerId=" + i);
        }
        incrementCbCount(5);
    }

    @Override
    public void eventFollowupReceived(NanFollowupReceivedInd nanFollowupReceivedInd) {
        if (this.mDbg) {
            StringBuilder sb = new StringBuilder();
            sb.append("eventFollowupReceived: discoverySessionId=");
            sb.append((int) nanFollowupReceivedInd.discoverySessionId);
            sb.append(", peerId=");
            sb.append(nanFollowupReceivedInd.peerId);
            sb.append(", addr=");
            sb.append(String.valueOf(HexEncoding.encode(nanFollowupReceivedInd.addr)));
            sb.append(", serviceSpecificInfo=");
            sb.append(Arrays.toString(convertArrayListToNativeByteArray(nanFollowupReceivedInd.serviceSpecificInfo)));
            sb.append(", ssi.size()=");
            sb.append(nanFollowupReceivedInd.serviceSpecificInfo == null ? 0 : nanFollowupReceivedInd.serviceSpecificInfo.size());
            Log.v(TAG, sb.toString());
        }
        incrementCbCount(6);
        this.mWifiAwareStateManager.onMessageReceivedNotification(nanFollowupReceivedInd.discoverySessionId, nanFollowupReceivedInd.peerId, nanFollowupReceivedInd.addr, convertArrayListToNativeByteArray(nanFollowupReceivedInd.serviceSpecificInfo));
    }

    @Override
    public void eventTransmitFollowup(short s, WifiNanStatus wifiNanStatus) {
        if (this.mDbg) {
            Log.v(TAG, "eventTransmitFollowup: id=" + ((int) s) + ", status=" + statusString(wifiNanStatus));
        }
        incrementCbCount(7);
        if (wifiNanStatus.status == 0) {
            this.mWifiAwareStateManager.onMessageSendSuccessNotification(s);
        } else {
            this.mWifiAwareStateManager.onMessageSendFailNotification(s, wifiNanStatus.status);
        }
    }

    @Override
    public void eventDataPathRequest(NanDataPathRequestInd nanDataPathRequestInd) {
        if (this.mDbg) {
            Log.v(TAG, "eventDataPathRequest: discoverySessionId=" + ((int) nanDataPathRequestInd.discoverySessionId) + ", peerDiscMacAddr=" + String.valueOf(HexEncoding.encode(nanDataPathRequestInd.peerDiscMacAddr)) + ", ndpInstanceId=" + nanDataPathRequestInd.ndpInstanceId);
        }
        incrementCbCount(8);
        this.mWifiAwareStateManager.onDataPathRequestNotification(nanDataPathRequestInd.discoverySessionId, nanDataPathRequestInd.peerDiscMacAddr, nanDataPathRequestInd.ndpInstanceId);
    }

    @Override
    public void eventDataPathConfirm(NanDataPathConfirmInd nanDataPathConfirmInd) {
        if (this.mDbg) {
            Log.v(TAG, "onDataPathConfirm: ndpInstanceId=" + nanDataPathConfirmInd.ndpInstanceId + ", peerNdiMacAddr=" + String.valueOf(HexEncoding.encode(nanDataPathConfirmInd.peerNdiMacAddr)) + ", dataPathSetupSuccess=" + nanDataPathConfirmInd.dataPathSetupSuccess + ", reason=" + nanDataPathConfirmInd.status.status);
        }
        if (this.mIsHal12OrLater) {
            Log.wtf(TAG, "eventDataPathConfirm should not be called by a >=1.2 HAL!");
        }
        incrementCbCount(9);
        this.mWifiAwareStateManager.onDataPathConfirmNotification(nanDataPathConfirmInd.ndpInstanceId, nanDataPathConfirmInd.peerNdiMacAddr, nanDataPathConfirmInd.dataPathSetupSuccess, nanDataPathConfirmInd.status.status, convertArrayListToNativeByteArray(nanDataPathConfirmInd.appInfo), null);
    }

    @Override
    public void eventDataPathConfirm_1_2(android.hardware.wifi.V1_2.NanDataPathConfirmInd nanDataPathConfirmInd) {
        if (this.mDbg) {
            Log.v(TAG, "eventDataPathConfirm_1_2: ndpInstanceId=" + nanDataPathConfirmInd.V1_0.ndpInstanceId + ", peerNdiMacAddr=" + String.valueOf(HexEncoding.encode(nanDataPathConfirmInd.V1_0.peerNdiMacAddr)) + ", dataPathSetupSuccess=" + nanDataPathConfirmInd.V1_0.dataPathSetupSuccess + ", reason=" + nanDataPathConfirmInd.V1_0.status.status);
        }
        if (!this.mIsHal12OrLater) {
            Log.wtf(TAG, "eventDataPathConfirm_1_2 should not be called by a <1.2 HAL!");
        } else {
            incrementCbCount(9);
            this.mWifiAwareStateManager.onDataPathConfirmNotification(nanDataPathConfirmInd.V1_0.ndpInstanceId, nanDataPathConfirmInd.V1_0.peerNdiMacAddr, nanDataPathConfirmInd.V1_0.dataPathSetupSuccess, nanDataPathConfirmInd.V1_0.status.status, convertArrayListToNativeByteArray(nanDataPathConfirmInd.V1_0.appInfo), nanDataPathConfirmInd.channelInfo);
        }
    }

    @Override
    public void eventDataPathScheduleUpdate(NanDataPathScheduleUpdateInd nanDataPathScheduleUpdateInd) {
        if (this.mDbg) {
            Log.v(TAG, "eventDataPathScheduleUpdate");
        }
        if (!this.mIsHal12OrLater) {
            Log.wtf(TAG, "eventDataPathScheduleUpdate should not be called by a <1.2 HAL!");
        } else {
            incrementCbCount(11);
            this.mWifiAwareStateManager.onDataPathScheduleUpdateNotification(nanDataPathScheduleUpdateInd.peerDiscoveryAddress, nanDataPathScheduleUpdateInd.ndpInstanceIds, nanDataPathScheduleUpdateInd.channelInfo);
        }
    }

    @Override
    public void eventDataPathTerminated(int i) {
        if (this.mDbg) {
            Log.v(TAG, "eventDataPathTerminated: ndpInstanceId=" + i);
        }
        incrementCbCount(10);
        this.mWifiAwareStateManager.onDataPathEndNotification(i);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("WifiAwareNativeCallback:");
        printWriter.println("  mCallbackCounter: " + this.mCallbackCounter);
    }

    private byte[] convertArrayListToNativeByteArray(ArrayList<Byte> arrayList) {
        if (arrayList == null) {
            return null;
        }
        byte[] bArr = new byte[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
        return bArr;
    }

    private static String statusString(WifiNanStatus wifiNanStatus) {
        if (wifiNanStatus == null) {
            return "status=null";
        }
        return wifiNanStatus.status + " (" + wifiNanStatus.description + ")";
    }
}
