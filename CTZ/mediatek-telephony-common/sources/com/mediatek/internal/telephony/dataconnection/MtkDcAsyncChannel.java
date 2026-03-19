package com.mediatek.internal.telephony.dataconnection;

import android.os.Message;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.dataconnection.DcAsyncChannel;

public class MtkDcAsyncChannel extends DcAsyncChannel {
    public static final int REQ_GET_APNTYPE = 266254;
    public static final int RSP_GET_APNTYPE = 266255;

    public MtkDcAsyncChannel(DataConnection dataConnection, String str) {
        super(dataConnection, str);
    }

    public String[] getApnTypeSync() {
        if (isCallerOnDifferentThread()) {
            Message messageSendMessageSynchronously = sendMessageSynchronously(REQ_GET_APNTYPE);
            if (messageSendMessageSynchronously != null && messageSendMessageSynchronously.what == 266255) {
                return (String[]) messageSendMessageSynchronously.obj;
            }
            log("getApnTypeSync error response=" + messageSendMessageSynchronously);
            return null;
        }
        return ((MtkDataConnection) this.mDc).getApnType();
    }

    public void notifyVoiceCallEvent(boolean z, boolean z2) {
        sendMessage(262171, z ? 1 : 0, z2 ? 1 : 0);
    }
}
