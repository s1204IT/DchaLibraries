package com.mediatek.internal.telephony.cat;

import com.android.internal.telephony.cat.CommandDetails;
import com.android.internal.telephony.cat.CommandParams;
import com.android.internal.telephony.cat.TextMessage;

class OpenChannelParams extends CommandParams {
    public BearerDesc bearerDesc;
    public int bufferSize;
    public OtherAddress dataDestinationAddress;
    public GprsParams gprsParams;
    public OtherAddress localAddress;
    public TextMessage textMsg;
    public TransportProtocol transportProtocol;

    OpenChannelParams(CommandDetails commandDetails, BearerDesc bearerDesc, int i, OtherAddress otherAddress, TransportProtocol transportProtocol, OtherAddress otherAddress2, String str, String str2, String str3, TextMessage textMessage) {
        super(commandDetails);
        this.bearerDesc = null;
        this.bufferSize = 0;
        this.localAddress = null;
        this.transportProtocol = null;
        this.dataDestinationAddress = null;
        this.textMsg = null;
        this.gprsParams = null;
        this.bearerDesc = bearerDesc;
        this.bufferSize = i;
        this.localAddress = otherAddress;
        this.transportProtocol = transportProtocol;
        this.dataDestinationAddress = otherAddress2;
        this.textMsg = textMessage;
        this.gprsParams = new GprsParams(str, str2, str3);
    }

    public class GprsParams {
        public String accessPointName;
        public String userLogin;
        public String userPwd;

        GprsParams(String str, String str2, String str3) {
            this.accessPointName = null;
            this.userLogin = null;
            this.userPwd = null;
            this.accessPointName = str;
            this.userLogin = str2;
            this.userPwd = str3;
        }
    }
}
