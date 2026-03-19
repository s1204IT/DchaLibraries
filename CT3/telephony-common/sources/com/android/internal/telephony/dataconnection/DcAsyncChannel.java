package com.android.internal.telephony.dataconnection;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.cdma.sms.SmsEnvelope;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.util.AsyncChannel;

public class DcAsyncChannel extends AsyncChannel {
    public static final int BASE = 266240;
    private static final int CMD_TO_STRING_COUNT = 16;
    private static final boolean DBG = false;
    public static final int REQ_GET_APNSETTING = 266244;
    public static final int REQ_GET_APNTYPE = 266254;
    public static final int REQ_GET_CID = 266242;
    public static final int REQ_GET_LINK_PROPERTIES = 266246;
    public static final int REQ_GET_NETWORK_CAPABILITIES = 266250;
    public static final int REQ_IS_INACTIVE = 266240;
    public static final int REQ_RESET = 266252;
    public static final int REQ_SET_LINK_PROPERTIES_HTTP_PROXY = 266248;
    public static final int RSP_GET_APNSETTING = 266245;
    public static final int RSP_GET_APNTYPE = 266255;
    public static final int RSP_GET_CID = 266243;
    public static final int RSP_GET_LINK_PROPERTIES = 266247;
    public static final int RSP_GET_NETWORK_CAPABILITIES = 266251;
    public static final int RSP_IS_INACTIVE = 266241;
    public static final int RSP_RESET = 266253;
    public static final int RSP_SET_LINK_PROPERTIES_HTTP_PROXY = 266249;
    private static String[] sCmdToString = new String[16];
    private DataConnection mDc;
    private long mDcThreadId;
    private String mLogTag;

    static {
        sCmdToString[0] = "REQ_IS_INACTIVE";
        sCmdToString[1] = "RSP_IS_INACTIVE";
        sCmdToString[2] = "REQ_GET_CID";
        sCmdToString[3] = "RSP_GET_CID";
        sCmdToString[4] = "REQ_GET_APNSETTING";
        sCmdToString[5] = "RSP_GET_APNSETTING";
        sCmdToString[6] = "REQ_GET_LINK_PROPERTIES";
        sCmdToString[7] = "RSP_GET_LINK_PROPERTIES";
        sCmdToString[8] = "REQ_SET_LINK_PROPERTIES_HTTP_PROXY";
        sCmdToString[9] = "RSP_SET_LINK_PROPERTIES_HTTP_PROXY";
        sCmdToString[10] = "REQ_GET_NETWORK_CAPABILITIES";
        sCmdToString[11] = "RSP_GET_NETWORK_CAPABILITIES";
        sCmdToString[12] = "REQ_RESET";
        sCmdToString[13] = "RSP_RESET";
    }

    protected static String cmdToString(int cmd) {
        int cmd2 = cmd - 266240;
        if (cmd2 >= 0 && cmd2 < sCmdToString.length) {
            return sCmdToString[cmd2];
        }
        return AsyncChannel.cmdToString(cmd2 + 266240);
    }

    public enum LinkPropertyChangeAction {
        NONE,
        CHANGED,
        RESET;

        public static LinkPropertyChangeAction[] valuesCustom() {
            return values();
        }

        public static LinkPropertyChangeAction fromInt(int value) {
            if (value == NONE.ordinal()) {
                return NONE;
            }
            if (value == CHANGED.ordinal()) {
                return CHANGED;
            }
            if (value == RESET.ordinal()) {
                return RESET;
            }
            throw new RuntimeException("LinkPropertyChangeAction.fromInt: bad value=" + value);
        }
    }

    public DcAsyncChannel(DataConnection dc, String logTag) {
        this.mDc = dc;
        this.mDcThreadId = this.mDc.getHandler().getLooper().getThread().getId();
        this.mLogTag = logTag;
    }

    public void reqIsInactive() {
        sendMessage(266240);
    }

    public boolean rspIsInactive(Message response) {
        if (response.arg1 == 1) {
            return true;
        }
        return DBG;
    }

    public boolean isInactiveSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(266240);
            if (response != null && response.what == 266241) {
                return rspIsInactive(response);
            }
            log("rspIsInactive error response=" + response);
            return DBG;
        }
        return this.mDc.getIsInactive();
    }

    public void reqCid() {
        sendMessage(REQ_GET_CID);
    }

    public int rspCid(Message response) {
        int retVal = response.arg1;
        return retVal;
    }

    public int getCidSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_GET_CID);
            if (response != null && response.what == 266243) {
                int value = rspCid(response);
                return value;
            }
            log("rspCid error response=" + response);
            return -1;
        }
        int value2 = this.mDc.getCid();
        return value2;
    }

    public void reqApnSetting() {
        sendMessage(REQ_GET_APNSETTING);
    }

    public ApnSetting rspApnSetting(Message response) {
        ApnSetting retVal = (ApnSetting) response.obj;
        return retVal;
    }

    public ApnSetting getApnSettingSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_GET_APNSETTING);
            if (response != null && response.what == 266245) {
                return rspApnSetting(response);
            }
            log("getApnSetting error response=" + response);
            return null;
        }
        return this.mDc.getApnSetting();
    }

    public void reqLinkProperties() {
        sendMessage(REQ_GET_LINK_PROPERTIES);
    }

    public LinkProperties rspLinkProperties(Message response) {
        LinkProperties retVal = (LinkProperties) response.obj;
        return retVal;
    }

    public LinkProperties getLinkPropertiesSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_GET_LINK_PROPERTIES);
            if (response != null && response.what == 266247) {
                return rspLinkProperties(response);
            }
            log("getLinkProperties error response=" + response);
            return null;
        }
        return this.mDc.getCopyLinkProperties();
    }

    public void reqSetLinkPropertiesHttpProxy(ProxyInfo proxy) {
        sendMessage(REQ_SET_LINK_PROPERTIES_HTTP_PROXY, proxy);
    }

    public void setLinkPropertiesHttpProxySync(ProxyInfo proxy) {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_SET_LINK_PROPERTIES_HTTP_PROXY, proxy);
            if (response != null && response.what == 266249) {
                return;
            }
            log("setLinkPropertiesHttpPoxy error response=" + response);
            return;
        }
        this.mDc.setLinkPropertiesHttpProxy(proxy);
    }

    public void reqNetworkCapabilities() {
        sendMessage(REQ_GET_NETWORK_CAPABILITIES);
    }

    public NetworkCapabilities rspNetworkCapabilities(Message response) {
        NetworkCapabilities retVal = (NetworkCapabilities) response.obj;
        return retVal;
    }

    public NetworkCapabilities getNetworkCapabilitiesSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_GET_NETWORK_CAPABILITIES);
            if (response != null && response.what == 266251) {
                return rspNetworkCapabilities(response);
            }
            return null;
        }
        return this.mDc.getCopyNetworkCapabilities();
    }

    public void reqReset() {
        sendMessage(REQ_RESET);
    }

    public void bringUp(ApnContext apnContext, int profileId, int rilRadioTechnology, Message onCompletedMsg, int connectionGeneration) {
        sendMessage(SmsEnvelope.TELESERVICE_MWI, new DataConnection.ConnectionParams(apnContext, profileId, rilRadioTechnology, onCompletedMsg, connectionGeneration));
    }

    public void tearDown(ApnContext apnContext, String reason, Message onCompletedMsg) {
        sendMessage(262148, new DataConnection.DisconnectParams(apnContext, reason, onCompletedMsg));
    }

    public void tearDownAll(String reason, Message onCompletedMsg) {
        sendMessage(262150, new DataConnection.DisconnectParams(null, reason, onCompletedMsg));
    }

    public int getDataConnectionIdSync() {
        return this.mDc.getDataConnectionId();
    }

    public String toString() {
        return this.mDc.getName();
    }

    private boolean isCallerOnDifferentThread() {
        long curThreadId = Thread.currentThread().getId();
        if (this.mDcThreadId != curThreadId) {
            return true;
        }
        return DBG;
    }

    private void log(String s) {
        Rlog.d(this.mLogTag, "DataConnectionAc " + s);
    }

    public String[] getPcscfAddr() {
        return this.mDc.mPcscfAddr;
    }

    public String[] getApnTypeSync() {
        if (isCallerOnDifferentThread()) {
            Message response = sendMessageSynchronously(REQ_GET_APNTYPE);
            if (response != null && response.what == 266255) {
                return (String[]) response.obj;
            }
            log("getApnTypeSync error response=" + response);
            return null;
        }
        return this.mDc.getApnType();
    }

    public void notifyVoiceCallEvent(boolean bInVoiceCall, boolean bSupportConcurrent) {
        sendMessage(262163, bInVoiceCall ? 1 : 0, bSupportConcurrent ? 1 : 0);
    }
}
