package com.android.internal.telephony.cat;

public class TransportProtocol {
    public int portNumber;
    public int protocolType;

    public TransportProtocol(int type, int port) {
        this.protocolType = 0;
        this.portNumber = 0;
        this.protocolType = type;
        this.portNumber = port;
    }
}
