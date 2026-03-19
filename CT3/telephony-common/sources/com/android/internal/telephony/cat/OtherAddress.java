package com.android.internal.telephony.cat;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OtherAddress {
    public InetAddress address;
    public int addressType;
    public byte[] rawAddress;

    public OtherAddress(int type, byte[] rawData, int index) throws UnknownHostException {
        this.addressType = 0;
        this.rawAddress = null;
        this.address = null;
        try {
            this.addressType = type;
            if (33 == this.addressType) {
                this.rawAddress = new byte[4];
                System.arraycopy(rawData, index, this.rawAddress, 0, this.rawAddress.length);
                this.address = InetAddress.getByAddress(this.rawAddress);
            } else if (87 == this.addressType) {
            }
        } catch (IndexOutOfBoundsException e) {
            CatLog.d("[BIP]", "OtherAddress: out of bounds");
            this.rawAddress = null;
            this.address = null;
        }
    }
}
