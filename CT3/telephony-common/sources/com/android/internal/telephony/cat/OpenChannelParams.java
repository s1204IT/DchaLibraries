package com.android.internal.telephony.cat;

class OpenChannelParams extends CommandParams {
    public BearerDesc bearerDesc;
    public int bufferSize;
    public OtherAddress dataDestinationAddress;
    public GprsParams gprsParams;
    public OtherAddress localAddress;
    public TextMessage textMsg;
    public TransportProtocol transportProtocol;

    OpenChannelParams(CommandDetails cmdDet, BearerDesc bearerDesc, int size, OtherAddress localAddress, TransportProtocol transportProtocol, OtherAddress address, String apn, String login, String pwd, TextMessage textMsg) {
        super(cmdDet);
        this.bearerDesc = null;
        this.bufferSize = 0;
        this.localAddress = null;
        this.transportProtocol = null;
        this.dataDestinationAddress = null;
        this.textMsg = null;
        this.gprsParams = null;
        this.bearerDesc = bearerDesc;
        this.bufferSize = size;
        this.localAddress = localAddress;
        this.transportProtocol = transportProtocol;
        this.dataDestinationAddress = address;
        this.textMsg = textMsg;
        this.gprsParams = new GprsParams(apn, login, pwd);
    }

    public class GprsParams {
        public String accessPointName;
        public String userLogin;
        public String userPwd;

        GprsParams(String apn, String login, String pwd) {
            this.accessPointName = null;
            this.userLogin = null;
            this.userPwd = null;
            this.accessPointName = apn;
            this.userLogin = login;
            this.userPwd = pwd;
        }
    }
}
