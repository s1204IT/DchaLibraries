package com.android.server.wifi.p2p;

import android.hardware.wifi.supplicant.V1_0.ISupplicantP2pIfaceCallback;
import android.hardware.wifi.supplicant.V1_0.WpsConfigMethods;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pProvDiscEvent;
import android.net.wifi.p2p.WifiP2pWfdInfo;
import android.net.wifi.p2p.nsd.WifiP2pServiceResponse;
import android.util.Log;
import com.android.server.wifi.p2p.WifiP2pServiceImpl;
import com.android.server.wifi.util.NativeUtil;
import java.util.ArrayList;
import java.util.Arrays;

public class SupplicantP2pIfaceCallback extends ISupplicantP2pIfaceCallback.Stub {
    private static final boolean DBG = true;
    private static final String TAG = "SupplicantP2pIfaceCallback";
    private final String mInterface;
    private final WifiP2pMonitor mMonitor;

    public SupplicantP2pIfaceCallback(String str, WifiP2pMonitor wifiP2pMonitor) {
        this.mInterface = str;
        this.mMonitor = wifiP2pMonitor;
    }

    protected static void logd(String str) {
        Log.d(TAG, str);
    }

    @Override
    public void onNetworkAdded(int i) {
    }

    @Override
    public void onNetworkRemoved(int i) {
    }

    @Override
    public void onDeviceFound(byte[] bArr, byte[] bArr2, byte[] bArr3, String str, short s, byte b, int i, byte[] bArr4) {
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        wifiP2pDevice.deviceName = str;
        if (str == null) {
            Log.e(TAG, "Missing device name.");
            return;
        }
        try {
            wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray(bArr2);
            try {
                wifiP2pDevice.primaryDeviceType = NativeUtil.wpsDevTypeStringFromByteArray(bArr3);
                wifiP2pDevice.deviceCapability = b;
                wifiP2pDevice.groupCapability = i;
                wifiP2pDevice.wpsConfigMethodsSupported = s;
                wifiP2pDevice.status = 3;
                if (bArr4 != null && bArr4.length >= 6) {
                    wifiP2pDevice.wfdInfo = new WifiP2pWfdInfo((bArr4[0] << 8) + bArr4[1], (bArr4[2] << 8) + bArr4[3], (bArr4[4] << 8) + bArr4[5]);
                }
                logd("Device discovered on " + this.mInterface + ": " + wifiP2pDevice);
                this.mMonitor.broadcastP2pDeviceFound(this.mInterface, wifiP2pDevice);
            } catch (Exception e) {
                Log.e(TAG, "Could not encode device primary type.", e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "Could not decode device address.", e2);
        }
    }

    @Override
    public void onDeviceLost(byte[] bArr) {
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        try {
            wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray(bArr);
            wifiP2pDevice.status = 4;
            logd("Device lost on " + this.mInterface + ": " + wifiP2pDevice);
            this.mMonitor.broadcastP2pDeviceLost(this.mInterface, wifiP2pDevice);
        } catch (Exception e) {
            Log.e(TAG, "Could not decode device address.", e);
        }
    }

    @Override
    public void onFindStopped() {
        logd("Search stopped on " + this.mInterface);
        this.mMonitor.broadcastP2pFindStopped(this.mInterface);
    }

    @Override
    public void onGoNegotiationRequest(byte[] bArr, short s) {
        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        try {
            wifiP2pConfig.deviceAddress = NativeUtil.macAddressFromByteArray(bArr);
            wifiP2pConfig.wps = new WpsInfo();
            if (s == 1) {
                wifiP2pConfig.wps.setup = 1;
            } else {
                switch (s) {
                    case 4:
                        wifiP2pConfig.wps.setup = 0;
                        break;
                    case 5:
                        wifiP2pConfig.wps.setup = 2;
                        break;
                    default:
                        wifiP2pConfig.wps.setup = 0;
                        break;
                }
            }
            logd("Group Owner negotiation initiated on " + this.mInterface + ": " + wifiP2pConfig);
            this.mMonitor.broadcastP2pGoNegotiationRequest(this.mInterface, wifiP2pConfig);
        } catch (Exception e) {
            Log.e(TAG, "Could not decode device address.", e);
        }
    }

    @Override
    public void onGoNegotiationCompleted(int i) {
        logd("Group Owner negotiation completed with status: " + i);
        WifiP2pServiceImpl.P2pStatus p2pStatusHalStatusToP2pStatus = halStatusToP2pStatus(i);
        if (p2pStatusHalStatusToP2pStatus == WifiP2pServiceImpl.P2pStatus.SUCCESS) {
            this.mMonitor.broadcastP2pGoNegotiationSuccess(this.mInterface);
        } else {
            this.mMonitor.broadcastP2pGoNegotiationFailure(this.mInterface, p2pStatusHalStatusToP2pStatus);
        }
    }

    @Override
    public void onGroupFormationSuccess() {
        logd("Group formation successful on " + this.mInterface);
        this.mMonitor.broadcastP2pGroupFormationSuccess(this.mInterface);
    }

    @Override
    public void onGroupFormationFailure(String str) {
        logd("Group formation failed on " + this.mInterface + ": " + str);
        this.mMonitor.broadcastP2pGroupFormationFailure(this.mInterface, str);
    }

    @Override
    public void onGroupStarted(String str, boolean z, ArrayList<Byte> arrayList, int i, byte[] bArr, String str2, byte[] bArr2, boolean z2) {
        if (str == null) {
            Log.e(TAG, "Missing group interface name.");
            return;
        }
        logd("Group " + str + " started on " + this.mInterface);
        WifiP2pGroup wifiP2pGroup = new WifiP2pGroup();
        wifiP2pGroup.setInterface(str);
        try {
            wifiP2pGroup.setNetworkName(NativeUtil.removeEnclosingQuotes(NativeUtil.encodeSsid(arrayList)));
            wifiP2pGroup.setIsGroupOwner(z);
            wifiP2pGroup.setPassphrase(str2);
            if (z2) {
                wifiP2pGroup.setNetworkId(-2);
            } else {
                wifiP2pGroup.setNetworkId(-1);
            }
            WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
            try {
                wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray(bArr2);
                wifiP2pGroup.setOwner(wifiP2pDevice);
                this.mMonitor.broadcastP2pGroupStarted(this.mInterface, wifiP2pGroup);
            } catch (Exception e) {
                Log.e(TAG, "Could not decode Group Owner address.", e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "Could not encode SSID.", e2);
        }
    }

    @Override
    public void onGroupRemoved(String str, boolean z) {
        if (str == null) {
            Log.e(TAG, "Missing group name.");
            return;
        }
        logd("Group " + str + " removed from " + this.mInterface);
        WifiP2pGroup wifiP2pGroup = new WifiP2pGroup();
        wifiP2pGroup.setInterface(str);
        wifiP2pGroup.setIsGroupOwner(z);
        this.mMonitor.broadcastP2pGroupRemoved(this.mInterface, wifiP2pGroup);
    }

    @Override
    public void onInvitationReceived(byte[] bArr, byte[] bArr2, byte[] bArr3, int i, int i2) {
        WifiP2pGroup wifiP2pGroup = new WifiP2pGroup();
        wifiP2pGroup.setNetworkId(i);
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        try {
            wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray(bArr);
            wifiP2pGroup.addClient(wifiP2pDevice);
            WifiP2pDevice wifiP2pDevice2 = new WifiP2pDevice();
            try {
                wifiP2pDevice2.deviceAddress = NativeUtil.macAddressFromByteArray(bArr2);
                wifiP2pGroup.setOwner(wifiP2pDevice2);
                logd("Invitation received on " + this.mInterface + ": " + wifiP2pGroup);
                this.mMonitor.broadcastP2pInvitationReceived(this.mInterface, wifiP2pGroup);
            } catch (Exception e) {
                Log.e(TAG, "Could not decode Group Owner MAC address.", e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "Could not decode MAC address.", e2);
        }
    }

    @Override
    public void onInvitationResult(byte[] bArr, int i) {
        logd("Invitation completed with status: " + i);
        this.mMonitor.broadcastP2pInvitationResult(this.mInterface, halStatusToP2pStatus(i));
    }

    @Override
    public void onProvisionDiscoveryCompleted(byte[] bArr, boolean z, byte b, short s, String str) {
        if (b != 0) {
            Log.e(TAG, "Provision discovery failed: " + ((int) b));
            this.mMonitor.broadcastP2pProvisionDiscoveryFailure(this.mInterface);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Provision discovery ");
        sb.append(z ? "request" : "response");
        sb.append(" for WPS Config method: ");
        sb.append((int) s);
        logd(sb.toString());
        WifiP2pProvDiscEvent wifiP2pProvDiscEvent = new WifiP2pProvDiscEvent();
        wifiP2pProvDiscEvent.device = new WifiP2pDevice();
        try {
            wifiP2pProvDiscEvent.device.deviceAddress = NativeUtil.macAddressFromByteArray(bArr);
            if ((s & WpsConfigMethods.PUSHBUTTON) != 0) {
                if (z) {
                    wifiP2pProvDiscEvent.event = 1;
                    this.mMonitor.broadcastP2pProvisionDiscoveryPbcRequest(this.mInterface, wifiP2pProvDiscEvent);
                    return;
                } else {
                    wifiP2pProvDiscEvent.event = 2;
                    this.mMonitor.broadcastP2pProvisionDiscoveryPbcResponse(this.mInterface, wifiP2pProvDiscEvent);
                    return;
                }
            }
            if (!z && (s & WpsConfigMethods.KEYPAD) != 0) {
                wifiP2pProvDiscEvent.event = 4;
                wifiP2pProvDiscEvent.pin = str;
                this.mMonitor.broadcastP2pProvisionDiscoveryShowPin(this.mInterface, wifiP2pProvDiscEvent);
                return;
            }
            if (!z && (s & 8) != 0) {
                wifiP2pProvDiscEvent.event = 3;
                this.mMonitor.broadcastP2pProvisionDiscoveryEnterPin(this.mInterface, wifiP2pProvDiscEvent);
                return;
            }
            if (z && (s & 8) != 0) {
                wifiP2pProvDiscEvent.event = 4;
                wifiP2pProvDiscEvent.pin = str;
                this.mMonitor.broadcastP2pProvisionDiscoveryShowPin(this.mInterface, wifiP2pProvDiscEvent);
            } else if (z && (s & WpsConfigMethods.KEYPAD) != 0) {
                wifiP2pProvDiscEvent.event = 3;
                this.mMonitor.broadcastP2pProvisionDiscoveryEnterPin(this.mInterface, wifiP2pProvDiscEvent);
            } else {
                Log.e(TAG, "Unsupported config methods: " + ((int) s));
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not decode MAC address.", e);
        }
    }

    @Override
    public void onServiceDiscoveryResponse(byte[] bArr, short s, ArrayList<Byte> arrayList) {
        logd("Service discovery response received on " + this.mInterface);
        try {
            this.mMonitor.broadcastP2pServiceDiscoveryResponse(this.mInterface, WifiP2pServiceResponse.newInstance(NativeUtil.macAddressFromByteArray(bArr), NativeUtil.byteArrayFromArrayList(arrayList)));
        } catch (Exception e) {
            Log.e(TAG, "Could not process service discovery response.", e);
        }
    }

    private WifiP2pDevice createStaEventDevice(byte[] bArr, byte[] bArr2) {
        WifiP2pDevice wifiP2pDevice = new WifiP2pDevice();
        if (Arrays.equals(NativeUtil.ANY_MAC_BYTES, bArr2)) {
            bArr2 = bArr;
        }
        wifiP2pDevice.interfaceAddress = NativeUtil.macAddressFromByteArray(bArr);
        try {
            wifiP2pDevice.deviceAddress = NativeUtil.macAddressFromByteArray(bArr2);
            return wifiP2pDevice;
        } catch (Exception e) {
            Log.e(TAG, "Could not decode MAC address", e);
            return null;
        }
    }

    @Override
    public void onStaAuthorized(byte[] bArr, byte[] bArr2) {
        logd("STA authorized on " + this.mInterface);
        WifiP2pDevice wifiP2pDeviceCreateStaEventDevice = createStaEventDevice(bArr, bArr2);
        if (wifiP2pDeviceCreateStaEventDevice == null) {
            return;
        }
        this.mMonitor.broadcastP2pApStaConnected(this.mInterface, wifiP2pDeviceCreateStaEventDevice);
    }

    @Override
    public void onStaDeauthorized(byte[] bArr, byte[] bArr2) {
        logd("STA deauthorized on " + this.mInterface);
        WifiP2pDevice wifiP2pDeviceCreateStaEventDevice = createStaEventDevice(bArr, bArr2);
        if (wifiP2pDeviceCreateStaEventDevice == null) {
            return;
        }
        this.mMonitor.broadcastP2pApStaDisconnected(this.mInterface, wifiP2pDeviceCreateStaEventDevice);
    }

    private static WifiP2pServiceImpl.P2pStatus halStatusToP2pStatus(int i) {
        WifiP2pServiceImpl.P2pStatus p2pStatus = WifiP2pServiceImpl.P2pStatus.UNKNOWN;
        switch (i) {
            case 0:
            case 12:
                return WifiP2pServiceImpl.P2pStatus.SUCCESS;
            case 1:
                return WifiP2pServiceImpl.P2pStatus.INFORMATION_IS_CURRENTLY_UNAVAILABLE;
            case 2:
                return WifiP2pServiceImpl.P2pStatus.INCOMPATIBLE_PARAMETERS;
            case 3:
                return WifiP2pServiceImpl.P2pStatus.LIMIT_REACHED;
            case 4:
                return WifiP2pServiceImpl.P2pStatus.INVALID_PARAMETER;
            case 5:
                return WifiP2pServiceImpl.P2pStatus.UNABLE_TO_ACCOMMODATE_REQUEST;
            case 6:
                return WifiP2pServiceImpl.P2pStatus.PREVIOUS_PROTOCOL_ERROR;
            case 7:
                return WifiP2pServiceImpl.P2pStatus.NO_COMMON_CHANNEL;
            case 8:
                return WifiP2pServiceImpl.P2pStatus.UNKNOWN_P2P_GROUP;
            case 9:
                return WifiP2pServiceImpl.P2pStatus.BOTH_GO_INTENT_15;
            case 10:
                return WifiP2pServiceImpl.P2pStatus.INCOMPATIBLE_PROVISIONING_METHOD;
            case 11:
                return WifiP2pServiceImpl.P2pStatus.REJECTED_BY_USER;
            default:
                return p2pStatus;
        }
    }
}
