package com.android.internal.telephony.cat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Process;
import android.os.ServiceManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneProxy;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.WapPushManagerParams;
import com.android.internal.telephony.cat.AppInterface;
import com.android.internal.telephony.cat.BearerDescription;
import com.android.internal.telephony.cat.CatCmdMessage;
import com.android.internal.telephony.cat.InterfaceTransportLevel;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DataCallResponse;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class BipProxy extends Handler {
    static final int EVENT_DATA_STATE_CHANGED = 30;
    static final int MSG_ID_SETUP_DATA_CALL = 10;
    static final int MSG_ID_TEARDOWN_DATA_CALL = 11;
    private static final int NW_OEM_ID = 1;
    static boolean channelStatusEvent = false;
    static boolean dataAvailableEvent = false;
    private CatService mCatService;
    private CommandsInterface mCmdIf;
    private Context mContext;
    private DcTrackerBase mDct;
    private DefaultBearerStateReceiver mDefaultBearerStateReceiver;
    private Phone mPhone;
    private int mSlotId;
    final int TCP_CHANNEL_BUFFER_SIZE = 16384;
    final int UDP_CHANNEL_BUFFER_SIZE = 1500;
    final int MAX_CHANNEL_NUM = 7;
    final int MAX_REQUESTED_LEN = 237;
    private ServiceStateTracker mSst = null;
    private BipChannel[] mBipChannels = new BipChannel[7];

    interface BipChannel {
        void close(CatCmdMessage catCmdMessage);

        int getStatus();

        void onSessionEnd();

        boolean open(CatCmdMessage catCmdMessage);

        void receive(CatCmdMessage catCmdMessage);

        void send(CatCmdMessage catCmdMessage);

        void setStatus(int i);
    }

    public BipProxy(int id, CatService catService, CommandsInterface cmdIf, Context context) {
        this.mCatService = null;
        this.mCatService = catService;
        this.mCmdIf = cmdIf;
        this.mContext = context;
        this.mDefaultBearerStateReceiver = new DefaultBearerStateReceiver(context);
        this.mPhone = PhoneFactory.getPhone(id);
        this.mDct = ((PhoneBase) ((PhoneProxy) this.mPhone).getActivePhone()).mDcTracker;
        this.mSlotId = id;
    }

    public boolean canHandleNewChannel() {
        for (int i = 0; i < this.mBipChannels.length; i++) {
            if (this.mBipChannels[i] == null) {
                return true;
            }
        }
        return false;
    }

    public void handleBipCommand(CatCmdMessage cmdMsg) {
        if (cmdMsg == null) {
            for (int i = 0; i < this.mBipChannels.length; i++) {
                if (this.mBipChannels[i] != null) {
                    this.mBipChannels[i].onSessionEnd();
                }
            }
            return;
        }
        AppInterface.CommandType curCmdType = cmdMsg.getCmdType();
        switch (curCmdType) {
            case OPEN_CHANNEL:
                CatCmdMessage.ChannelSettings channelSettings = cmdMsg.getChannelSettings();
                if (channelSettings != null) {
                    if (allChannelsClosed()) {
                        this.mDefaultBearerStateReceiver.startListening();
                    }
                    int i2 = 0;
                    while (true) {
                        if (i2 < this.mBipChannels.length) {
                            if (this.mBipChannels[i2] != null) {
                                i2++;
                            } else {
                                channelSettings.channel = i2 + 1;
                            }
                        }
                    }
                    if (channelSettings.channel == 0) {
                        this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 1, null);
                    } else {
                        switch (channelSettings.protocol) {
                            case TCP_SERVER:
                                this.mBipChannels[channelSettings.channel - 1] = new TcpServerChannel();
                                break;
                            case TCP_CLIENT_REMOTE:
                            case TCP_CLIENT_LOCAL:
                                this.mBipChannels[channelSettings.channel - 1] = new TcpClientChannel();
                                break;
                            case UDP_CLIENT_REMOTE:
                            case UDP_CLIENT_LOCAL:
                                this.mBipChannels[channelSettings.channel - 1] = new UdpClientChannel();
                                break;
                            default:
                                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                                break;
                        }
                        if (setupDataConnection(cmdMsg)) {
                            CatLog.d(this, "Continue processing open channel");
                            if (!this.mBipChannels[channelSettings.channel - 1].open(cmdMsg)) {
                                cleanupBipChannel(channelSettings.channel);
                            }
                        }
                    }
                }
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                break;
            case SEND_DATA:
            case RECEIVE_DATA:
            case CLOSE_CHANNEL:
                if (cmdMsg.getDataSettings() != null) {
                    try {
                        BipChannel curChannel = this.mBipChannels[cmdMsg.getDataSettings().channel - 1];
                        if (curChannel != null) {
                            if (curCmdType == AppInterface.CommandType.SEND_DATA) {
                                curChannel.send(cmdMsg);
                            } else if (curCmdType == AppInterface.CommandType.RECEIVE_DATA) {
                                curChannel.receive(cmdMsg);
                            } else if (curCmdType == AppInterface.CommandType.CLOSE_CHANNEL) {
                                curChannel.close(cmdMsg);
                            } else {
                                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.CMD_DATA_NOT_UNDERSTOOD, false, 0, null);
                            }
                        } else {
                            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
                        return;
                    }
                }
                break;
            case GET_CHANNEL_STATUS:
                int[] status = new int[7];
                for (int i3 = 0; i3 < 7; i3++) {
                    if (this.mBipChannels[i3] != null) {
                        status[i3] = this.mBipChannels[i3].getStatus();
                    } else {
                        status[i3] = 0;
                    }
                }
                ResponseData resp = new ChannelStatusResponseData(status);
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, resp);
                break;
        }
    }

    private boolean allChannelsClosed() {
        BipChannel[] arr$ = this.mBipChannels;
        for (BipChannel channel : arr$) {
            if (channel != null) {
                return false;
            }
        }
        return true;
    }

    private void cleanupBipChannel(int channel) {
        this.mBipChannels[channel - 1] = null;
        if (allChannelsClosed()) {
            this.mDefaultBearerStateReceiver.stopListening();
            this.mCmdIf.unregisterForDataNetworkStateChanged(this);
            if (this.mSst != null) {
                this.mSst.unregisterForDataConnectionDetached(this);
            }
            IBinder b = ServiceManager.getService("network_management");
            INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
            try {
                service.removeNetwork(1);
            } catch (Exception e) {
                CatLog.d(this, "Error removing network 1: " + e.getMessage());
            }
        }
    }

    private void sendChannelStatusEvent(int channelStatus) {
        byte[] additionalInfo = {-72, 2, 0, 0};
        additionalInfo[2] = (byte) ((channelStatus >> 8) & 255);
        additionalInfo[3] = (byte) (channelStatus & 255);
        this.mCatService.onEventDownload(new CatEventMessage(EventList.CHANNEL_STATUS.value(), additionalInfo, true));
    }

    private void sendDataAvailableEvent(int channelStatus, int dataAvailable) {
        byte[] additionalInfo = {-72, 2, 0, 0, -73, 1, 0};
        additionalInfo[2] = (byte) ((channelStatus >> 8) & 255);
        additionalInfo[3] = (byte) (channelStatus & 255);
        additionalInfo[6] = (byte) (dataAvailable & 255);
        this.mCatService.onEventDownload(new CatEventMessage(EventList.DATA_AVAILABLE.value(), additionalInfo, true));
    }

    private class ConnectionSetupFailedException extends IOException {
        public ConnectionSetupFailedException(String message) {
            super(message);
        }
    }

    private void teardownBipDataChannel(int channel, CatCmdMessage cmdMsg) {
        CatLog.d(this, "teardownBipDataChannel -clear for channel " + channel);
        cleanupBipChannel(channel);
        this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
    }

    private void closeDataConnection(CatCmdMessage.ChannelSettings channelSettings, CatCmdMessage cmdMsg) {
        NetworkInfo[] netInfos;
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
        ArrayList<ApnSetting> as = null;
        boolean defaultPdp = false;
        if (this.mDct instanceof DcTracker) {
            as = ((DcTracker) this.mDct).buildWaitingApns("fota", radioTech);
            CatLog.d(this, "APN settings: " + as);
        }
        if (channelSettings.networkAccessName == null && channelSettings.cid == null) {
            defaultPdp = true;
            CatLog.d(this, "closeDataConnection- channel is based on default PDP");
        }
        if (as != null && !as.isEmpty()) {
            String fotaApnName = as.get(0).apn;
            if (fotaApnName.equalsIgnoreCase(channelSettings.networkAccessName) && (netInfos = cm.getAllNetworkInfo()) != null && netInfos.length != 0) {
                for (NetworkInfo info : netInfos) {
                    if (info != null && info.isAvailable() && info.getType() == 10) {
                        CatLog.d(this, "closeDataConnection" + info.toString());
                        if (info.getState() == NetworkInfo.State.CONNECTED || info.getState() == NetworkInfo.State.CONNECTING || info.getState() == NetworkInfo.State.SUSPENDED) {
                            cleanupBipChannel(channelSettings.channel);
                            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
                            return;
                        }
                    }
                }
            }
        }
        if (!defaultPdp) {
            teardownDataConnection(cmdMsg, channelSettings.cid.intValue());
        } else {
            CatLog.d(this, "closeDataConnection- No need to tear down data connection - this is default PDP");
            teardownBipDataChannel(channelSettings.channel, cmdMsg);
        }
    }

    private NetworkInfo findAvailableDefaultBearer(NetworkInfo[] networkInfos) {
        ArrayList<NetworkInfo> availableBearers = new ArrayList<>();
        for (NetworkInfo info : networkInfos) {
            if (info != null && info.isAvailable()) {
                switch (info.getType()) {
                    case 0:
                    case 1:
                    case 6:
                        availableBearers.add(info);
                        break;
                }
            }
        }
        if (availableBearers.size() == 0) {
            return null;
        }
        NetworkInfo candidateBearer = null;
        for (NetworkInfo info2 : availableBearers) {
            NetworkInfo.State state = info2.getState();
            if (state == NetworkInfo.State.CONNECTED) {
                return info2;
            }
            if (state == NetworkInfo.State.CONNECTING || state == NetworkInfo.State.SUSPENDED) {
                candidateBearer = info2;
            }
        }
        return candidateBearer;
    }

    private boolean setupDefaultDataConnection(CatCmdMessage cmdMsg) throws ConnectionSetupFailedException {
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (!cm.getMobileDataEnabled()) {
            CatLog.d(this, "User does not allow mobile data connections");
            int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
            ArrayList<ApnSetting> as = null;
            if (this.mDct instanceof DcTracker) {
                this.mPhone.getServiceState();
                if (radioTech == 14) {
                    as = ((DcTracker) this.mDct).buildWaitingApns("ia", radioTech);
                } else {
                    as = ((DcTracker) this.mDct).buildWaitingApns("default", radioTech);
                }
                CatLog.d(this, "radioTech" + radioTech + "APN settings: " + as);
            }
            Message resultMsg = obtainMessage(10, cmdMsg);
            if (as != null && !as.isEmpty()) {
                int authType = as.get(0).authType;
                if (authType == -1) {
                    authType = TextUtils.isEmpty(as.get(0).user) ? 0 : 3;
                }
                this.mCmdIf.setupDataCall(Integer.toString(1), Integer.toString(1001), as.get(0).apn, as.get(0).user, as.get(0).password, Integer.toString(authType), as.get(0).protocol, resultMsg);
                return false;
            }
            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
            throw new ConnectionSetupFailedException("No default bearer available");
        }
        NetworkInfo[] netInfos = cm.getAllNetworkInfo();
        CatCmdMessage.ChannelSettings newChannel = cmdMsg.getChannelSettings();
        boolean result = false;
        if (netInfos == null || netInfos.length == 0 || findAvailableDefaultBearer(netInfos) == null) {
            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
            throw new ConnectionSetupFailedException("No default bearer available");
        }
        NetworkInfo netInfo = findAvailableDefaultBearer(netInfos);
        NetworkInfo.State state = netInfo.getState();
        ConnectionSetupFailedException setupFailedException = null;
        switch (AnonymousClass1.$SwitchMap$android$net$NetworkInfo$State[state.ordinal()]) {
            case 1:
                CatLog.d(this, "Default bearer is connected");
                result = true;
                break;
            case 2:
                CatLog.d(this, "Default bearer is connecting.  Waiting for connect");
                Message resultMsg2 = obtainMessage(10, cmdMsg);
                this.mDefaultBearerStateReceiver.setOngoingSetupMessage(resultMsg2);
                result = false;
                break;
            case 3:
                CatLog.d(this, "Default bearer not connected, busy on voice call");
                ResponseData resp = new OpenChannelResponseData(newChannel.bufSize, null, newChannel.bearerDescription);
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, resp);
                setupFailedException = new ConnectionSetupFailedException("Default bearer suspended!");
                break;
            default:
                CatLog.d(this, "Default bearer is Disconnected");
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
                setupFailedException = new ConnectionSetupFailedException("Default bearer is disconnected!");
                break;
        }
        if (setupFailedException != null) {
            throw setupFailedException;
        }
        return result;
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$net$NetworkInfo$State = new int[NetworkInfo.State.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$State[NetworkInfo.State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[NetworkInfo.State.CONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$State[NetworkInfo.State.SUSPENDED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType = new int[AppInterface.CommandType.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.OPEN_CHANNEL.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.SEND_DATA.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.RECEIVE_DATA.ordinal()] = 3;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.CLOSE_CHANNEL.ordinal()] = 4;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$AppInterface$CommandType[AppInterface.CommandType.GET_CHANNEL_STATUS.ordinal()] = 5;
            } catch (NoSuchFieldError e8) {
            }
            $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol = new int[InterfaceTransportLevel.TransportProtocol.values().length];
            try {
                $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol[InterfaceTransportLevel.TransportProtocol.TCP_SERVER.ordinal()] = 1;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol[InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_REMOTE.ordinal()] = 2;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol[InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_LOCAL.ordinal()] = 3;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol[InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_REMOTE.ordinal()] = 4;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$cat$InterfaceTransportLevel$TransportProtocol[InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_LOCAL.ordinal()] = 5;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    private boolean isIdle() {
        TelephonyManager tm = (TelephonyManager) this.mContext.getSystemService("phone");
        int count = tm.getPhoneCount();
        for (int i = 0; i < count; i++) {
            int[] subIds = SubscriptionManager.getSubId(i);
            if (subIds != null) {
                if (tm.getCallState(subIds[0]) != 0) {
                    return false;
                }
            } else {
                CatLog.d(this, "Fail to getSubId, slot ID = " + i);
            }
        }
        return true;
    }

    private boolean setupSpecificPdpConnection(CatCmdMessage cmdMsg) throws ConnectionSetupFailedException {
        int authType;
        CatCmdMessage.ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (newChannel.networkAccessName == null) {
            CatLog.d(this, "no accessname for PS bearer req");
            return setupDefaultDataConnection(cmdMsg);
        }
        if (!isIdle()) {
            CatLog.d(this, "Bearer not setup, busy on voice call");
            ResponseData resp = new OpenChannelResponseData(newChannel.bufSize, null, newChannel.bearerDescription);
            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.TERMINAL_CRNTLY_UNABLE_TO_PROCESS, true, 2, resp);
            throw new ConnectionSetupFailedException("Busy on voice call");
        }
        CatLog.d(this, "Detected new data connection parameters");
        Message resultMsg = obtainMessage(10, cmdMsg);
        if (newChannel.userLogin == null && newChannel.userPassword == null) {
            authType = 0;
        } else {
            authType = 3;
        }
        this.mCmdIf.setupDataCall(Integer.toString(1), Integer.toString(1001), newChannel.networkAccessName, newChannel.userLogin, newChannel.userPassword, Integer.toString(authType), "IPV4V6", resultMsg);
        return false;
    }

    private boolean setupDataConnection(CatCmdMessage cmdMsg) {
        boolean result = false;
        CatCmdMessage.ChannelSettings newChannel = cmdMsg.getChannelSettings();
        if (newChannel.protocol != InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_REMOTE && newChannel.protocol != InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_REMOTE) {
            CatLog.d(this, "No data connection needed for this channel");
            return true;
        }
        BearerDescription bd = newChannel.bearerDescription;
        try {
            if (bd.type == BearerDescription.BearerType.DEFAULT_BEARER || bd.type == BearerDescription.BearerType.MOBILE_PS || bd.type == BearerDescription.BearerType.MOBILE_PS_EXTENDED_QOS || bd.type == BearerDescription.BearerType.E_UTRAN) {
                CatLog.d(this, "setup Specific Pdp Connection");
                result = setupSpecificPdpConnection(cmdMsg);
            } else {
                CatLog.d(this, "Unsupported bearer type");
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BEYOND_TERMINAL_CAPABILITY, false, 0, null);
            }
        } catch (ConnectionSetupFailedException csfe) {
            CatLog.d(this, "setupDataConnection Failed: " + csfe.getMessage());
            this.mBipChannels[newChannel.channel - 1] = null;
            cleanupBipChannel(newChannel.channel);
        }
        return result;
    }

    private boolean teardownDataConnection(CatCmdMessage cmdMsg, int cid) {
        Message resultMsg = obtainMessage(11, cmdMsg);
        IBinder b = ServiceManager.getService("network_management");
        INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
        try {
            service.removeInterfaceFromNetwork("ccinet" + (cid - 1), 1);
        } catch (Exception e) {
            CatLog.d(this, "Failed to remove Interface - ccinet" + (cid - 1) + e);
        }
        this.mCmdIf.deactivateDataCall(cid, 0, resultMsg);
        return true;
    }

    private void onSetupConnectionCompleted(AsyncResult ar) {
        if (ar != null) {
            CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
            DataCallResponse response = (DataCallResponse) ar.result;
            if (ar.exception != null) {
                CatLog.d(this, "onSetupConnectionCompleted failed, ar.exception=" + ar.exception + " response=" + response);
                CatLog.d(this, "Failed to setup data connection for channel: " + cmdMsg.getChannelSettings().channel);
                cmdMsg.getChannelSettings().cid = null;
                ResponseData resp = new OpenChannelResponseData(cmdMsg.getChannelSettings().bufSize, null, cmdMsg.getChannelSettings().bearerDescription);
                this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.NETWORK_CRNTLY_UNABLE_TO_PROCESS, false, 0, resp);
                cleanupBipChannel(cmdMsg.getChannelSettings().channel);
                return;
            }
            if (response != null && response.status == 0) {
                CatLog.d(this, "onSetupConnectionCompleted received DataCallState: " + response.toString());
                int cid = response.cid;
                String interfaceName = response.ifname;
                CatLog.d(this, "Succeeded to setup data connection for channel " + cmdMsg.getChannelSettings().channel + " cid=" + cid + " ifname=" + interfaceName);
                cmdMsg.getChannelSettings().cid = Integer.valueOf(cid);
                if (channelStatusEvent) {
                    CatLog.d(this, "channelStatusEvent is true, we need monitor this cid status");
                    this.mCmdIf.registerForDataNetworkStateChanged(this, 30, cmdMsg);
                    CatLog.d(this, "channelStatusEvent is true, we also monitor the ps detached");
                    this.mSst = ((PhoneBase) ((PhoneProxy) this.mPhone).getActivePhone()).getServiceStateTracker();
                    this.mSst.registerForDataConnectionDetached(this, 270345, cmdMsg);
                }
                IBinder b = ServiceManager.getService("network_management");
                INetworkManagementService service = INetworkManagementService.Stub.asInterface(b);
                try {
                    service.createPhysicalNetwork(1);
                } catch (Exception e) {
                    CatLog.d(this, "Error creating network 1: " + e.getMessage());
                }
                try {
                    InetAddress addr = InetAddress.getByAddress(cmdMsg.getChannelSettings().destinationAddress);
                    service.addInterfaceToNetwork(interfaceName, 1);
                    service.addLegacyRouteForNetId(1, RouteInfo.makeHostRoute(addr, interfaceName), Process.myUid());
                } catch (Exception e2) {
                    CatLog.d(this, "Failed to add host route" + e2);
                }
                CatLog.d(this, "Continue processing open channel");
                if (!this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].open(cmdMsg)) {
                    cleanupBipChannel(cmdMsg.getChannelSettings().channel);
                    return;
                }
                return;
            }
            CatLog.d(this, "onSetupConnectionCompleted, response.status != success");
            cmdMsg.getChannelSettings().cid = null;
            ResponseData resp2 = new OpenChannelResponseData(cmdMsg.getChannelSettings().bufSize, null, cmdMsg.getChannelSettings().bearerDescription);
            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
            cleanupBipChannel(cmdMsg.getChannelSettings().channel);
        }
    }

    private void onTeardownConnectionCompleted(AsyncResult ar) {
        int channel;
        if (ar != null) {
            CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
            if (cmdMsg.getCmdType() == AppInterface.CommandType.OPEN_CHANNEL) {
                channel = cmdMsg.getChannelSettings().channel;
            } else if (cmdMsg.getCmdType() == AppInterface.CommandType.CLOSE_CHANNEL) {
                channel = cmdMsg.getDataSettings().channel;
            } else {
                return;
            }
            if (ar.exception != null) {
                CatLog.d(this, "Failed to teardown data connection for channel: " + channel + " " + ar.exception.getMessage());
            } else {
                CatLog.d(this, "Succedded to teardown data connection for channel: " + channel);
            }
            cleanupBipChannel(channel);
            this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
        }
    }

    private void onDataConnectionDetached(AsyncResult ar) {
        CatLog.d(this, "onDataConnectionDetached ");
        CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
        if (ar.exception != null) {
            CatLog.d(this, "onDataConnectionDetached(ar): exception; likely radio not available, ignore");
            return;
        }
        if (cmdMsg.getChannelSettings().channel > 0) {
            int channelStatus = (cmdMsg.getChannelSettings().channel << 8) + 5;
            this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].setStatus(channelStatus);
            CatLog.d(this, "mChannelStatus is " + channelStatus);
            sendChannelStatusEvent(channelStatus);
            cleanupBipChannel(cmdMsg.getChannelSettings().channel);
        }
    }

    private void onDataStateChanged(AsyncResult ar) {
        CatLog.d(this, "onDataStateChanged(ar): E");
        CatCmdMessage cmdMsg = (CatCmdMessage) ar.userObj;
        ArrayList<DataCallResponse> dataCallStates = (ArrayList) ar.result;
        if (ar.exception != null) {
            CatLog.d(this, "onDataStateChanged(ar): exception; likely radio not available, ignore");
            return;
        }
        CatLog.d(this, "onDataStateChanged(ar): DataCallState size=" + dataCallStates.size());
        if (dataCallStates.size() == 0 && cmdMsg.getChannelSettings().channel > 0) {
            int channelStatus = (cmdMsg.getChannelSettings().channel << 8) + 5;
            this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].setStatus(channelStatus);
            CatLog.d(this, "mChannelStatus is " + channelStatus);
            sendChannelStatusEvent(channelStatus);
            cleanupBipChannel(cmdMsg.getChannelSettings().channel);
            return;
        }
        for (DataCallResponse dataCallState : dataCallStates) {
            CatLog.d(this, "dataCallState " + dataCallState.toString());
            if (dataCallState.cid == cmdMsg.getChannelSettings().cid.intValue() && dataCallState.active == 0) {
                int channelStatus2 = (cmdMsg.getChannelSettings().channel << 8) + 5;
                this.mBipChannels[cmdMsg.getChannelSettings().channel - 1].setStatus(channelStatus2);
                CatLog.d(this, "mChannelStatus is " + channelStatus2);
                sendChannelStatusEvent(channelStatus2);
                cleanupBipChannel(cmdMsg.getChannelSettings().channel);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 10:
                if (msg.obj != null) {
                    onSetupConnectionCompleted((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 11:
                if (msg.obj != null) {
                    onTeardownConnectionCompleted((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 30:
                onDataStateChanged((AsyncResult) msg.obj);
                return;
            case 270345:
                onDataConnectionDetached((AsyncResult) msg.obj);
                return;
            default:
                throw new AssertionError("Unrecognized message: " + msg.what);
        }
    }

    class TcpServerChannel implements BipChannel {
        ServerSocket mServerSocket;
        Socket mSocket;
        CatCmdMessage.ChannelSettings mChannelSettings = null;
        int mChannelStatus = 0;
        ServerThread mThread = null;
        byte[] mRxBuf = new byte[16384];
        int mRxPos = 0;
        int mRxLen = 0;
        byte[] mTxBuf = new byte[16384];
        int mTxPos = 0;
        int mTxLen = 0;

        TcpServerChannel() {
        }

        @Override
        public boolean open(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            this.mChannelSettings = cmdMsg.getChannelSettings();
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            if (this.mChannelSettings.bufSize > 16384) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                this.mChannelSettings.bufSize = 16384;
            } else if (this.mChannelSettings.bufSize > 0) {
                this.mRxBuf = new byte[this.mChannelSettings.bufSize];
                this.mTxBuf = new byte[this.mChannelSettings.bufSize];
            } else {
                this.mChannelSettings.bufSize = 16384;
            }
            try {
                this.mServerSocket = new ServerSocket(this.mChannelSettings.port);
                CatLog.d(this, "Open server socket on port " + this.mChannelSettings.port + " for channel " + this.mChannelSettings.channel);
                this.mChannelStatus = (this.mChannelSettings.channel << 8) + 16384;
                ResponseData resp = new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
                this.mThread = new ServerThread();
                this.mThread.start();
                return true;
            } catch (IOException e) {
                ResponseData resp2 = new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                CatLog.d(this, "IOException " + e.getMessage());
                return false;
            }
        }

        @Override
        public void close(CatCmdMessage cmdMsg) {
            if ((this.mSocket == null && this.mServerSocket == null) || ((this.mSocket == null && this.mServerSocket.isClosed()) || ((this.mServerSocket == null && this.mSocket.isClosed()) || (this.mSocket.isClosed() && this.mServerSocket.isClosed())))) {
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
            }
            if ((cmdMsg.getCommandQualifier() & 1) == 1) {
                if (this.mSocket != null && !this.mSocket.isClosed()) {
                    try {
                        this.mSocket.close();
                    } catch (IOException e) {
                    }
                }
                this.mSocket = null;
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
                return;
            }
            if (this.mSocket != null && !this.mSocket.isClosed()) {
                try {
                    this.mSocket.close();
                } catch (IOException e2) {
                }
            }
            this.mSocket = null;
            if (this.mServerSocket != null && !this.mServerSocket.isClosed()) {
                try {
                    this.mServerSocket.close();
                } catch (IOException e3) {
                }
            }
            this.mServerSocket = null;
            this.mRxPos = 0;
            this.mRxLen = 0;
            this.mTxPos = 0;
            this.mTxLen = 0;
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, null);
            BipProxy.this.sendChannelStatusEvent(this.mChannelStatus);
        }

        @Override
        public void send(CatCmdMessage cmdMsg) {
            CatCmdMessage.DataSettings dataSettings = cmdMsg.getDataSettings();
            CatLog.d(this, "SEND_DATA on channel no: " + dataSettings.channel);
            for (int i = 0; i < dataSettings.data.length && this.mTxPos < this.mTxBuf.length; i++) {
                byte[] bArr = this.mTxBuf;
                int i2 = this.mTxPos;
                this.mTxPos = i2 + 1;
                bArr[i2] = dataSettings.data[i];
            }
            this.mTxLen += dataSettings.data.length;
            CatLog.d(this, "Tx buffer now contains " + this.mTxLen + " bytes.");
            if (cmdMsg.getCommandQualifier() == 1) {
                this.mTxPos = 0;
                int len = this.mTxLen;
                this.mTxLen = 0;
                CatLog.d(this, "Sent data to socket " + len + " bytes.");
                if (this.mSocket == null) {
                    CatLog.d(this, "Socket not available.");
                    ResponseData resp = new SendDataResponseData(0);
                    BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp);
                    return;
                } else {
                    try {
                        this.mSocket.getOutputStream().write(this.mTxBuf, 0, len);
                    } catch (IOException e) {
                        CatLog.d(this, "IOException " + e.getMessage());
                        ResponseData resp2 = new SendDataResponseData(0);
                        BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                        return;
                    }
                }
            }
            int avail = 238;
            if (this.mChannelSettings != null && (avail = this.mChannelSettings.bufSize - this.mTxLen) > 255) {
                avail = 255;
            }
            ResponseData resp3 = new SendDataResponseData(avail);
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, resp3);
        }

        @Override
        public void receive(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            CatLog.d(this, "RECEIVE_DATA on channel no: " + cmdMsg.getDataSettings().channel);
            int requested = cmdMsg.getDataSettings().length;
            if (requested > 237) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                requested = 237;
            }
            if (requested > this.mRxLen) {
                requested = this.mRxLen;
                result = ResultCode.PRFRMD_WITH_MISSING_INFO;
            }
            this.mRxLen -= requested;
            int available = 255;
            if (this.mRxLen < 255) {
                available = this.mRxLen;
            }
            byte[] data = null;
            if (requested > 0) {
                data = new byte[requested];
                System.arraycopy(this.mRxBuf, this.mRxPos, data, 0, requested);
                this.mRxPos += requested;
            }
            ResponseData resp = new ReceiveDataResponseData(data, available);
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
        }

        @Override
        public int getStatus() {
            if (this.mChannelSettings.channel == 0) {
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            return this.mChannelStatus;
        }

        @Override
        public void setStatus(int channelStatus) {
            this.mChannelStatus = channelStatus;
        }

        @Override
        public void onSessionEnd() {
            if (this.mSocket != null) {
                if (!this.mSocket.isClosed()) {
                    try {
                        this.mSocket.close();
                    } catch (IOException e) {
                    }
                }
                this.mSocket = null;
            }
            if (this.mThread == null || !this.mThread.isAlive()) {
                this.mThread = new ServerThread();
                this.mThread.start();
            }
        }

        class ServerThread extends Thread {
            ServerThread() {
            }

            @Override
            public void run() {
                CatLog.d(this, "Server thread start on channel no: " + (TcpServerChannel.this.mChannelSettings == null ? "empty Channel" : Integer.valueOf(TcpServerChannel.this.mChannelSettings.channel)));
                while (true) {
                    if (TcpServerChannel.this.mSocket == null || TcpServerChannel.this.mSocket.isClosed()) {
                        TcpServerChannel.this.mChannelStatus = (TcpServerChannel.this.mChannelSettings.channel << 8) + 16384;
                        try {
                            CatLog.d(this, "Wait for connection");
                            TcpServerChannel.this.mSocket = TcpServerChannel.this.mServerSocket.accept();
                            if (TcpServerChannel.this.mSocket != null && TcpServerChannel.this.mSocket.isConnected()) {
                                TcpServerChannel.this.mChannelStatus = WapPushManagerParams.FURTHER_PROCESSING + (TcpServerChannel.this.mChannelSettings.channel << 8);
                                BipProxy.this.sendChannelStatusEvent(TcpServerChannel.this.mChannelStatus);
                            }
                        } catch (IOException e) {
                            CatLog.d(this, "IOException " + e.getMessage());
                            CatLog.d(this, "Server thread end on channel no: " + (TcpServerChannel.this.mChannelSettings == null ? "empty Channel" : Integer.valueOf(TcpServerChannel.this.mChannelSettings.channel)));
                            return;
                        }
                    }
                    if (TcpServerChannel.this.mSocket != null) {
                        while (true) {
                            try {
                                TcpServerChannel.this.mRxLen = TcpServerChannel.this.mSocket.getInputStream().read(TcpServerChannel.this.mRxBuf);
                                if (TcpServerChannel.this.mRxLen < 0) {
                                    break;
                                }
                                if (TcpServerChannel.this.mRxLen == 0) {
                                    CatLog.d(this, "No data read.");
                                } else {
                                    TcpServerChannel.this.mRxPos = 0;
                                    int available = 255;
                                    if (TcpServerChannel.this.mRxLen < 255) {
                                        available = TcpServerChannel.this.mRxLen;
                                    }
                                    if (BipProxy.dataAvailableEvent) {
                                        BipProxy.this.sendDataAvailableEvent(TcpServerChannel.this.mChannelStatus, (byte) (available & 255));
                                    }
                                }
                            } catch (IOException e2) {
                                CatLog.d(this, "Read on No: " + TcpServerChannel.this.mChannelSettings.channel + ", IOException " + e2.getMessage());
                                TcpServerChannel.this.mSocket = null;
                                TcpServerChannel.this.mRxBuf = new byte[TcpServerChannel.this.mChannelSettings.bufSize];
                                TcpServerChannel.this.mTxBuf = new byte[TcpServerChannel.this.mChannelSettings.bufSize];
                                TcpServerChannel.this.mRxPos = 0;
                                TcpServerChannel.this.mRxLen = 0;
                                TcpServerChannel.this.mTxPos = 0;
                                TcpServerChannel.this.mTxLen = 0;
                            }
                        }
                        CatLog.d(this, "client closed.");
                        try {
                            TcpServerChannel.this.mSocket.close();
                        } catch (IOException e3) {
                        }
                        TcpServerChannel.this.mSocket = null;
                        TcpServerChannel.this.mChannelStatus = (TcpServerChannel.this.mChannelSettings.channel << 8) + 16384;
                        BipProxy.this.sendChannelStatusEvent(TcpServerChannel.this.mChannelStatus);
                    } else {
                        CatLog.d(this, "No Socket connection for server thread on channel no: " + TcpServerChannel.this.mChannelSettings.channel);
                    }
                }
            }
        }
    }

    class TcpClientChannel implements BipChannel {
        Socket mSocket;
        CatCmdMessage.ChannelSettings mChannelSettings = null;
        int mChannelStatus = 0;
        TcpClientThread mThread = null;
        byte[] mRxBuf = new byte[16384];
        int mRxPos = 0;
        int mRxLen = 0;
        byte[] mTxBuf = new byte[16384];
        int mTxPos = 0;
        int mTxLen = 0;

        TcpClientChannel() {
        }

        @Override
        public boolean open(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            this.mChannelSettings = cmdMsg.getChannelSettings();
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            if (this.mChannelSettings.bufSize > 16384) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                this.mChannelSettings.bufSize = 16384;
            } else {
                this.mRxBuf = new byte[this.mChannelSettings.bufSize];
                this.mTxBuf = new byte[this.mChannelSettings.bufSize];
            }
            if ((this.mSocket == null && cmdMsg.getCommandQualifier() == 1 && this.mChannelSettings.bearerDescription.type == BearerDescription.BearerType.E_UTRAN) || this.mChannelSettings.protocol == InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_REMOTE) {
                this.mThread = new TcpClientThread(cmdMsg);
                this.mThread.start();
            } else {
                this.mChannelStatus = WapPushManagerParams.FURTHER_PROCESSING + (this.mChannelSettings.channel << 8);
                ResponseData resp = new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
            }
            return true;
        }

        @Override
        public void close(CatCmdMessage cmdMsg) {
            if (this.mSocket != null && !this.mSocket.isClosed()) {
                try {
                    this.mSocket.close();
                } catch (IOException e) {
                    CatLog.d(this, "IOException " + e.getMessage());
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e2) {
            }
            this.mSocket = null;
            this.mRxPos = 0;
            this.mRxLen = 0;
            this.mTxPos = 0;
            this.mTxLen = 0;
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            BipProxy.this.closeDataConnection(this.mChannelSettings, cmdMsg);
        }

        private void sendData(CatCmdMessage cmdMsg) {
            if (cmdMsg != null) {
                CatCmdMessage.DataSettings dataSettings = cmdMsg.getDataSettings();
                CatLog.d(this, "SEND_DATA on channel no: " + dataSettings.channel);
                CatLog.d(this, "Transfer data into tx buffer");
                for (int i = 0; i < dataSettings.data.length && this.mTxPos < this.mTxBuf.length; i++) {
                    byte[] bArr = this.mTxBuf;
                    int i2 = this.mTxPos;
                    this.mTxPos = i2 + 1;
                    bArr[i2] = dataSettings.data[i];
                }
                this.mTxLen += dataSettings.data.length;
                CatLog.d(this, "Tx buffer now contains " + this.mTxLen + " bytes.");
                if (cmdMsg.getCommandQualifier() == 1) {
                    this.mTxPos = 0;
                    int len = this.mTxLen;
                    this.mTxLen = 0;
                    CatLog.d(this, "Sent data to socket " + len + " bytes.");
                    if (this.mSocket == null) {
                        CatLog.d(this, "Socket not available.");
                        ResponseData resp = new SendDataResponseData(0);
                        BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp);
                        return;
                    } else {
                        try {
                            this.mSocket.getOutputStream().write(this.mTxBuf, 0, len);
                        } catch (IOException e) {
                            CatLog.d(this, "IOException " + e.getMessage());
                            ResponseData resp2 = new SendDataResponseData(0);
                            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                            return;
                        }
                    }
                }
                int avail = 238;
                if (this.mChannelSettings != null && (avail = this.mChannelSettings.bufSize - this.mTxLen) > 255) {
                    avail = 255;
                }
                CatLog.d(this, "TR with " + avail + " bytes available in Tx Buffer on channel no: " + dataSettings.channel);
                ResponseData resp3 = new SendDataResponseData(avail);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, resp3);
            }
        }

        @Override
        public void send(CatCmdMessage cmdMsg) {
            if (this.mSocket == null) {
                this.mThread = new TcpClientThread(cmdMsg);
                this.mThread.start();
            } else {
                sendData(cmdMsg);
            }
        }

        @Override
        public void receive(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            CatLog.d(this, "RECEIVE_DATA on channel no: " + cmdMsg.getDataSettings().channel);
            int requested = cmdMsg.getDataSettings().length;
            if (requested > 237) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                requested = 237;
            }
            if (requested > this.mRxLen) {
                requested = this.mRxLen;
                result = ResultCode.PRFRMD_WITH_MISSING_INFO;
            }
            this.mRxLen -= requested;
            int available = 255;
            if (this.mRxLen < 255) {
                available = this.mRxLen;
            }
            byte[] data = null;
            if (requested > 0) {
                data = new byte[requested];
                System.arraycopy(this.mRxBuf, this.mRxPos, data, 0, requested);
                this.mRxPos += requested;
            }
            ResponseData resp = new ReceiveDataResponseData(data, available);
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
        }

        @Override
        public int getStatus() {
            if (this.mChannelSettings.channel == 0) {
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            return this.mChannelStatus;
        }

        @Override
        public void setStatus(int channelStatus) {
            this.mChannelStatus = channelStatus;
        }

        @Override
        public void onSessionEnd() {
            if (this.mThread == null || !this.mThread.isAlive()) {
                this.mThread = new TcpClientThread(null);
                this.mThread.start();
            }
        }

        class TcpClientThread extends Thread {
            CatCmdMessage mCmdMsg;

            public TcpClientThread(CatCmdMessage cmdMsg) {
                this.mCmdMsg = null;
                this.mCmdMsg = cmdMsg;
            }

            @Override
            public void run() {
                InetAddress addr;
                CatLog.d(this, "TcpClientThread run, mSocket =" + TcpClientChannel.this.mSocket + " mCmdMsg =" + this.mCmdMsg);
                if (TcpClientChannel.this.mSocket == null && this.mCmdMsg != null) {
                    try {
                        if (TcpClientChannel.this.mChannelSettings.protocol == InterfaceTransportLevel.TransportProtocol.TCP_CLIENT_REMOTE) {
                            addr = InetAddress.getByAddress(TcpClientChannel.this.mChannelSettings.destinationAddress);
                        } else {
                            addr = InetAddress.getLocalHost();
                        }
                        TcpClientChannel.this.mSocket = new Socket(addr, TcpClientChannel.this.mChannelSettings.port);
                        CatLog.d(this, "Connected client socket to " + addr.getHostAddress() + ":" + TcpClientChannel.this.mChannelSettings.port + " for channel " + TcpClientChannel.this.mChannelSettings.channel);
                        if (this.mCmdMsg.getCmdType() == AppInterface.CommandType.OPEN_CHANNEL) {
                            TcpClientChannel.this.mChannelStatus = WapPushManagerParams.FURTHER_PROCESSING + (TcpClientChannel.this.mChannelSettings.channel << 8);
                            ResponseData resp = new OpenChannelResponseData(TcpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(TcpClientChannel.this.mChannelStatus), TcpClientChannel.this.mChannelSettings.bearerDescription);
                            BipProxy.this.mCatService.sendTerminalResponse(this.mCmdMsg.mCmdDet, ResultCode.OK, false, 0, resp);
                        }
                    } catch (IOException e) {
                        CatLog.d(this, "IOException " + e.getMessage());
                        if (this.mCmdMsg.getCmdType() == AppInterface.CommandType.OPEN_CHANNEL) {
                            ResponseData resp2 = new OpenChannelResponseData(TcpClientChannel.this.mChannelSettings.bufSize, Integer.valueOf(TcpClientChannel.this.mChannelStatus), TcpClientChannel.this.mChannelSettings.bearerDescription);
                            BipProxy.this.mCatService.sendTerminalResponse(this.mCmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                        } else if (this.mCmdMsg.getCmdType() == AppInterface.CommandType.SEND_DATA) {
                            ResponseData resp3 = new SendDataResponseData(0);
                            BipProxy.this.mCatService.sendTerminalResponse(this.mCmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp3);
                        }
                        BipProxy.this.closeDataConnection(TcpClientChannel.this.mChannelSettings, this.mCmdMsg);
                        return;
                    }
                }
                if (this.mCmdMsg != null && this.mCmdMsg.getCmdType() == AppInterface.CommandType.SEND_DATA) {
                    TcpClientChannel.this.sendData(this.mCmdMsg);
                }
                if (TcpClientChannel.this.mSocket != null) {
                    try {
                        TcpClientChannel.this.mRxLen = TcpClientChannel.this.mSocket.getInputStream().read(TcpClientChannel.this.mRxBuf);
                    } catch (IOException e2) {
                        CatLog.d(this, "Read on No: " + TcpClientChannel.this.mChannelSettings.channel + ", IOException " + e2.getMessage());
                        TcpClientChannel.this.mSocket = null;
                        TcpClientChannel.this.mRxBuf = new byte[TcpClientChannel.this.mChannelSettings.bufSize];
                        TcpClientChannel.this.mTxBuf = new byte[TcpClientChannel.this.mChannelSettings.bufSize];
                        TcpClientChannel.this.mRxPos = 0;
                        TcpClientChannel.this.mRxLen = 0;
                        TcpClientChannel.this.mTxPos = 0;
                        TcpClientChannel.this.mTxLen = 0;
                    }
                    if (TcpClientChannel.this.mRxLen <= 0) {
                        CatLog.d(this, "No data read.");
                    } else {
                        TcpClientChannel.this.mRxPos = 0;
                        int available = 255;
                        if (TcpClientChannel.this.mRxLen < 255) {
                            available = TcpClientChannel.this.mRxLen;
                        }
                        BipProxy.this.sendDataAvailableEvent(TcpClientChannel.this.mChannelStatus, (byte) (available & 255));
                    }
                }
                CatLog.d(this, "Client thread end on channel no: " + (TcpClientChannel.this.mChannelSettings == null ? "empty Channel" : Integer.valueOf(TcpClientChannel.this.mChannelSettings.channel)));
            }
        }
    }

    class UdpClientChannel implements BipChannel {
        DatagramSocket mDatagramSocket;
        CatCmdMessage.ChannelSettings mChannelSettings = null;
        int mChannelStatus = 0;
        UdpClientThread mThread = null;
        byte[] mRxBuf = new byte[1500];
        int mRxPos = 0;
        int mRxLen = 0;
        byte[] mTxBuf = new byte[1500];
        int mTxPos = 0;
        int mTxLen = 0;
        InetAddress addr = null;

        UdpClientChannel() {
        }

        @Override
        public boolean open(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            this.mChannelSettings = cmdMsg.getChannelSettings();
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            if (this.mChannelSettings.bufSize > 1500) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                this.mChannelSettings.bufSize = 1500;
            } else if (this.mChannelSettings.bufSize > 0) {
                this.mRxBuf = new byte[this.mChannelSettings.bufSize];
                this.mTxBuf = new byte[this.mChannelSettings.bufSize];
            } else {
                this.mChannelSettings.bufSize = 1500;
            }
            try {
                if (this.mChannelSettings.protocol == InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_REMOTE) {
                    this.addr = InetAddress.getByAddress(this.mChannelSettings.destinationAddress);
                } else {
                    this.addr = InetAddress.getLocalHost();
                }
                CatLog.d(this, "Creating " + (this.mChannelSettings.protocol == InterfaceTransportLevel.TransportProtocol.UDP_CLIENT_REMOTE ? "remote" : "local") + " client socket to " + this.addr.getHostAddress() + ":" + this.mChannelSettings.port + " for channel " + this.mChannelSettings.channel);
                this.mDatagramSocket = new DatagramSocket();
                CatLog.d(this, "Connected UDP client socket to " + this.addr.getHostAddress() + ":" + this.mChannelSettings.port + " for channel " + this.mChannelSettings.channel);
                this.mChannelStatus = WapPushManagerParams.FURTHER_PROCESSING + (this.mChannelSettings.channel << 8);
                ResponseData resp = new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
                this.mThread = new UdpClientThread();
                this.mThread.start();
                return true;
            } catch (IOException e) {
                CatLog.d(this, "OPEN_CHANNEL - UDP Client connection failed: " + e.getMessage());
                ResponseData resp2 = new OpenChannelResponseData(this.mChannelSettings.bufSize, Integer.valueOf(this.mChannelStatus), this.mChannelSettings.bearerDescription);
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                BipProxy.this.closeDataConnection(this.mChannelSettings, cmdMsg);
                return false;
            }
        }

        @Override
        public void close(CatCmdMessage cmdMsg) {
            if (this.mDatagramSocket == null || this.mDatagramSocket.isClosed()) {
                BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 3, null);
            }
            if (this.mDatagramSocket != null && !this.mDatagramSocket.isClosed()) {
                this.mDatagramSocket.close();
            }
            this.mDatagramSocket = null;
            this.mRxPos = 0;
            this.mRxLen = 0;
            this.mTxPos = 0;
            this.mTxLen = 0;
            this.mChannelStatus = this.mChannelSettings.channel << 8;
            BipProxy.this.closeDataConnection(this.mChannelSettings, cmdMsg);
        }

        @Override
        public void send(final CatCmdMessage cmdMsg) {
            CatCmdMessage.DataSettings dataSettings = cmdMsg.getDataSettings();
            CatLog.d(this, "SEND_DATA on channel no: " + dataSettings.channel);
            CatLog.d(this, "Transfer data into tx buffer");
            for (int i = 0; i < dataSettings.data.length && this.mTxPos < this.mTxBuf.length; i++) {
                byte[] bArr = this.mTxBuf;
                int i2 = this.mTxPos;
                this.mTxPos = i2 + 1;
                bArr[i2] = dataSettings.data[i];
            }
            this.mTxLen += dataSettings.data.length;
            CatLog.d(this, "Tx buffer now contains " + this.mTxLen + " bytes.");
            if (cmdMsg.getCommandQualifier() == 1) {
                this.mTxPos = 0;
                int len = this.mTxLen;
                this.mTxLen = 0;
                CatLog.d(this, "Sent data to socket " + len + " bytes.");
                if (this.mDatagramSocket == null) {
                    CatLog.d(this, "Socket not available.");
                    ResponseData resp = new SendDataResponseData(0);
                    BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp);
                    return;
                }
                final DatagramPacket dp = new DatagramPacket(this.mTxBuf, len, this.addr, this.mChannelSettings.port);
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            UdpClientChannel.this.mDatagramSocket.send(dp);
                        } catch (IOException e) {
                            CatLog.d(this, "IOException " + e.getMessage());
                            ResponseData resp2 = new SendDataResponseData(0);
                            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp2);
                        } catch (IllegalArgumentException e2) {
                            CatLog.d(this, "IllegalArgumentException " + e2.getMessage());
                            ResponseData resp3 = new SendDataResponseData(0);
                            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.BIP_ERROR, true, 0, resp3);
                        }
                    }
                }.start();
            }
            int avail = 238;
            if (this.mChannelSettings != null && (avail = this.mChannelSettings.bufSize - this.mTxLen) > 255) {
                avail = 255;
            }
            CatLog.d(this, "TR with " + avail + " bytes available in Tx Buffer on channel no: " + dataSettings.channel);
            ResponseData resp2 = new SendDataResponseData(avail);
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, ResultCode.OK, false, 0, resp2);
        }

        @Override
        public void receive(CatCmdMessage cmdMsg) {
            ResultCode result = ResultCode.OK;
            CatLog.d(this, "RECEIVE_DATA on channel no: " + cmdMsg.getDataSettings().channel);
            int requested = cmdMsg.getDataSettings().length;
            if (requested > 237) {
                result = ResultCode.PRFRMD_WITH_MODIFICATION;
                requested = 237;
            }
            if (requested > this.mRxLen) {
                requested = this.mRxLen;
                result = ResultCode.PRFRMD_WITH_MISSING_INFO;
            }
            this.mRxLen -= requested;
            int available = 255;
            if (this.mRxLen < 255) {
                available = this.mRxLen;
            }
            byte[] data = null;
            if (requested > 0) {
                data = new byte[requested];
                System.arraycopy(this.mRxBuf, this.mRxPos, data, 0, requested);
                this.mRxPos += requested;
            }
            ResponseData resp = new ReceiveDataResponseData(data, available);
            BipProxy.this.mCatService.sendTerminalResponse(cmdMsg.mCmdDet, result, false, 0, resp);
        }

        @Override
        public int getStatus() {
            if (this.mChannelSettings.channel == 0) {
                this.mChannelStatus = this.mChannelSettings.channel << 8;
            }
            CatLog.d(this, "UdpClientChannel getStatus, mChannelStatus is " + this.mChannelStatus);
            return this.mChannelStatus;
        }

        @Override
        public void setStatus(int channelStatus) {
            this.mChannelStatus = channelStatus;
        }

        @Override
        public void onSessionEnd() {
            if (this.mThread == null || !this.mThread.isAlive()) {
                this.mThread = new UdpClientThread();
                this.mThread.start();
            }
        }

        class UdpClientThread extends Thread {
            UdpClientThread() {
            }

            @Override
            public void run() {
                CatLog.d(this, "UDP Client thread start on channel no: " + (UdpClientChannel.this.mChannelSettings == null ? "empty Channel" : Integer.valueOf(UdpClientChannel.this.mChannelSettings.channel)));
                if (UdpClientChannel.this.mDatagramSocket != null) {
                    DatagramPacket packet = null;
                    boolean success = false;
                    try {
                        CatLog.d(this, "UDP Client listening on port : " + UdpClientChannel.this.mDatagramSocket.getLocalPort());
                        DatagramPacket packet2 = new DatagramPacket(UdpClientChannel.this.mRxBuf, UdpClientChannel.this.mRxBuf.length);
                        try {
                            UdpClientChannel.this.mDatagramSocket.receive(packet2);
                            success = true;
                            packet = packet2;
                        } catch (IOException e) {
                            e = e;
                            packet = packet2;
                            CatLog.d(this, "Read on No: " + UdpClientChannel.this.mChannelSettings.channel + ", IOException " + e.getMessage());
                        } catch (IllegalArgumentException e2) {
                            e = e2;
                            packet = packet2;
                            CatLog.d(this, "IllegalArgumentException: " + e.getMessage());
                        }
                    } catch (IOException e3) {
                        e = e3;
                    } catch (IllegalArgumentException e4) {
                        e = e4;
                    }
                    if (success) {
                        UdpClientChannel.this.mRxLen = packet.getLength();
                    } else {
                        UdpClientChannel.this.mDatagramSocket = null;
                        UdpClientChannel.this.mRxBuf = new byte[UdpClientChannel.this.mChannelSettings.bufSize];
                        UdpClientChannel.this.mTxBuf = new byte[UdpClientChannel.this.mChannelSettings.bufSize];
                        UdpClientChannel.this.mRxPos = 0;
                        UdpClientChannel.this.mRxLen = 0;
                        UdpClientChannel.this.mTxPos = 0;
                        UdpClientChannel.this.mTxLen = 0;
                    }
                    if (UdpClientChannel.this.mRxLen <= 0) {
                        CatLog.d(this, "No data read.");
                    } else {
                        CatLog.d(this, UdpClientChannel.this.mRxLen + " data read.");
                        UdpClientChannel.this.mRxPos = 0;
                        int available = 255;
                        if (UdpClientChannel.this.mRxLen < 255) {
                            available = UdpClientChannel.this.mRxLen;
                        }
                        BipProxy.this.sendDataAvailableEvent(UdpClientChannel.this.mChannelStatus, (byte) (available & 255));
                    }
                }
                CatLog.d(this, "UDP Client thread end on channel no: " + (UdpClientChannel.this.mChannelSettings == null ? "empty Channel" : Integer.valueOf(UdpClientChannel.this.mChannelSettings.channel)));
            }
        }
    }

    class DefaultBearerStateReceiver extends BroadcastReceiver {
        ConnectivityManager mCm;
        Context mContext;
        Message mOngoingSetupMessage = null;
        final Object mSetupMessageLock = new Object();
        IntentFilter mFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        boolean mIsRegistered = false;

        public DefaultBearerStateReceiver(Context context) {
            this.mContext = context;
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }

        public void startListening() {
            if (!this.mIsRegistered) {
                this.mContext.registerReceiver(this, this.mFilter);
                this.mIsRegistered = true;
            }
        }

        public void stopListening() {
            if (this.mIsRegistered) {
                this.mContext.unregisterReceiver(this);
                this.mOngoingSetupMessage = null;
                this.mIsRegistered = false;
            }
        }

        public void setOngoingSetupMessage(Message msg) {
            synchronized (this.mSetupMessageLock) {
                this.mOngoingSetupMessage = msg;
            }
        }

        private void onDisconnected() {
            CatLog.d(this, "onDisconnected");
            synchronized (this.mSetupMessageLock) {
                if (this.mOngoingSetupMessage != null) {
                    Message msg = this.mOngoingSetupMessage;
                    this.mOngoingSetupMessage = null;
                    ConnectionSetupFailedException csfe = BipProxy.this.new ConnectionSetupFailedException("Default bearer failed to connect");
                    AsyncResult.forMessage(msg, (Object) null, csfe);
                    msg.sendToTarget();
                }
            }
        }

        private void onConnected() {
            CatLog.d(this, "onConnected");
            synchronized (this.mSetupMessageLock) {
                if (this.mOngoingSetupMessage != null) {
                    Message msg = this.mOngoingSetupMessage;
                    this.mOngoingSetupMessage = null;
                    LinkProperties linkProperties = this.mCm.getLinkProperties(0);
                    String ifname = linkProperties.getInterfaceName();
                    DataCallResponse response = new DataCallResponse();
                    response.cid = Integer.parseInt(ifname.replaceAll("\\D+", "")) + 1;
                    response.ifname = ifname;
                    response.status = 0;
                    AsyncResult.forMessage(msg, response, (Throwable) null);
                    msg.sendToTarget();
                }
            }
        }

        private void onStillConnecting() {
            CatLog.d(this, "onStillConnecting");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                CatLog.d(this, "Received unexpected broadcast: " + intent.getAction());
                return;
            }
            boolean noConnection = intent.getBooleanExtra("noConnectivity", false);
            NetworkInfo otherNetInfo = (NetworkInfo) intent.getParcelableExtra("otherNetwork");
            if (!noConnection) {
                onConnected();
            } else if (otherNetInfo != null) {
                onStillConnecting();
            } else {
                onDisconnected();
            }
        }
    }
}
