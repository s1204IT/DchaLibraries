package com.android.internal.telephony.dataconnection;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.ProxyInfo;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.InboundSmsTracker;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.util.AsyncChannel;

public class DcAsyncChannel extends AsyncChannel {
    public static final int BASE = 266240;
    private static final int CMD_TO_STRING_COUNT = 14;
    private static final boolean DBG = false;
    public static final int REQ_GET_APNSETTING = 266244;
    public static final int REQ_GET_CID = 266242;
    public static final int REQ_GET_LINK_PROPERTIES = 266246;
    public static final int REQ_GET_NETWORK_CAPABILITIES = 266250;
    public static final int REQ_IS_INACTIVE = 266240;
    public static final int REQ_RESET = 266252;
    public static final int REQ_SET_LINK_PROPERTIES_HTTP_PROXY = 266248;
    public static final int RSP_GET_APNSETTING = 266245;
    public static final int RSP_GET_CID = 266243;
    public static final int RSP_GET_LINK_PROPERTIES = 266247;
    public static final int RSP_GET_NETWORK_CAPABILITIES = 266251;
    public static final int RSP_IS_INACTIVE = 266241;
    public static final int RSP_RESET = 266253;
    public static final int RSP_SET_LINK_PROPERTIES_HTTP_PROXY = 266249;
    private static String[] sCmdToString = new String[14];
    protected DataConnection mDc;
    private long mDcThreadId;
    DataConnection.ConnectionParams mLastConnectionParams;
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

    protected static String cmdToString(int i) {
        int i2 = i - 266240;
        if (i2 >= 0 && i2 < sCmdToString.length) {
            return sCmdToString[i2];
        }
        return AsyncChannel.cmdToString(i2 + 266240);
    }

    public enum LinkPropertyChangeAction {
        NONE,
        CHANGED,
        RESET;

        public static LinkPropertyChangeAction fromInt(int i) {
            if (i == NONE.ordinal()) {
                return NONE;
            }
            if (i == CHANGED.ordinal()) {
                return CHANGED;
            }
            if (i == RESET.ordinal()) {
                return RESET;
            }
            throw new RuntimeException("LinkPropertyChangeAction.fromInt: bad value=" + i);
        }
    }

    public DcAsyncChannel(DataConnection dataConnection, String str) {
        this.mDc = dataConnection;
        this.mDcThreadId = this.mDc.getHandler().getLooper().getThread().getId();
        this.mLogTag = str;
    }

    public void reqIsInactive() {
        sendMessage(266240);
    }

    public boolean rspIsInactive(Message message) {
        return message.arg1 == 1;
    }

    public boolean isInactiveSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(266240);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266241) {
                return rspIsInactive(messageSendMessageSynchronously);
            }
            log("rspIsInactive error response=" + messageSendMessageSynchronously);
            return false;
        }
        return this.mDc.isInactive();
    }

    public void reqCid() {
        sendMessage(REQ_GET_CID);
    }

    public int rspCid(Message message) {
        return message.arg1;
    }

    public int getCidSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_GET_CID);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266243) {
                return rspCid(messageSendMessageSynchronously);
            }
            log("rspCid error response=" + messageSendMessageSynchronously);
            return -1;
        }
        return this.mDc.getCid();
    }

    public void reqApnSetting() {
        sendMessage(REQ_GET_APNSETTING);
    }

    public ApnSetting rspApnSetting(Message message) {
        return (ApnSetting) message.obj;
    }

    public ApnSetting getApnSettingSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_GET_APNSETTING);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266245) {
                return rspApnSetting(messageSendMessageSynchronously);
            }
            log("getApnSetting error response=" + messageSendMessageSynchronously);
            return null;
        }
        return this.mDc.getApnSetting();
    }

    public void reqLinkProperties() {
        sendMessage(REQ_GET_LINK_PROPERTIES);
    }

    public LinkProperties rspLinkProperties(Message message) {
        return (LinkProperties) message.obj;
    }

    public LinkProperties getLinkPropertiesSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_GET_LINK_PROPERTIES);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266247) {
                return rspLinkProperties(messageSendMessageSynchronously);
            }
            log("getLinkProperties error response=" + messageSendMessageSynchronously);
            return null;
        }
        return this.mDc.getCopyLinkProperties();
    }

    public void reqSetLinkPropertiesHttpProxy(ProxyInfo proxyInfo) {
        sendMessage(REQ_SET_LINK_PROPERTIES_HTTP_PROXY, proxyInfo);
    }

    public void setLinkPropertiesHttpProxySync(ProxyInfo proxyInfo) {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_SET_LINK_PROPERTIES_HTTP_PROXY, proxyInfo);
            if (messageSendMessageSynchronously == null || messageSendMessageSynchronously.what != 266249) {
                log("setLinkPropertiesHttpPoxy error response=" + messageSendMessageSynchronously);
                return;
            }
            return;
        }
        this.mDc.setLinkPropertiesHttpProxy(proxyInfo);
    }

    public void reqNetworkCapabilities() {
        sendMessage(REQ_GET_NETWORK_CAPABILITIES);
    }

    public NetworkCapabilities rspNetworkCapabilities(Message message) {
        return (NetworkCapabilities) message.obj;
    }

    public NetworkCapabilities getNetworkCapabilitiesSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_GET_NETWORK_CAPABILITIES);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266251) {
                return rspNetworkCapabilities(messageSendMessageSynchronously);
            }
            return null;
        }
        return this.mDc.getNetworkCapabilities();
    }

    public void reqReset() {
        sendMessage(REQ_RESET);
    }

    public void bringUp(ApnContext apnContext, int i, int i2, boolean z, Message message, int i3) {
        this.mLastConnectionParams = new DataConnection.ConnectionParams(apnContext, i, i2, z, message, i3);
        sendMessage(InboundSmsTracker.DEST_PORT_FLAG_3GPP2, this.mLastConnectionParams);
    }

    public void tearDown(ApnContext apnContext, String str, Message message) {
        sendMessage(262148, new DataConnection.DisconnectParams(apnContext, str, message));
    }

    public void tearDownAll(String str, Message message) {
        sendMessage(262150, new DataConnection.DisconnectParams(null, str, message));
    }

    public int getDataConnectionIdSync() {
        return this.mDc.getDataConnectionId();
    }

    public String toString() {
        return this.mDc.getName();
    }

    protected boolean isCallerOnDifferentThread() {
        return this.mDcThreadId != Thread.currentThread().getId();
    }

    protected void log(String str) {
        Rlog.d(this.mLogTag, "DataConnectionAc " + str);
    }

    public String[] getPcscfAddr() {
        return this.mDc.mPcscfAddr;
    }
}
