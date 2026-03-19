package com.mediatek.internal.telephony.cat;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class OtherAddress {
    public InetAddress address;
    public int addressType;
    public byte[] rawAddress;

    public OtherAddress(int i, byte[] bArr, int i2) throws UnknownHostException {
        this.addressType = 0;
        this.rawAddress = null;
        this.address = null;
        try {
            this.addressType = i;
            if (33 == this.addressType) {
                this.rawAddress = new byte[4];
                System.arraycopy(bArr, i2, this.rawAddress, 0, this.rawAddress.length);
                this.address = InetAddress.getByAddress(this.rawAddress);
            } else if (87 == this.addressType) {
                this.rawAddress = new byte[16];
                System.arraycopy(bArr, i2, this.rawAddress, 0, this.rawAddress.length);
                this.address = InetAddress.getByAddress(this.rawAddress);
            } else {
                MtkCatLog.e("[BIP]", "OtherAddress: unknown type: " + i);
            }
        } catch (IndexOutOfBoundsException e) {
            MtkCatLog.d("[BIP]", "OtherAddress: out of bounds");
            this.rawAddress = null;
            this.address = null;
        } catch (UnknownHostException e2) {
            MtkCatLog.e("[BIP]", "OtherAddress: UnknownHostException");
            this.rawAddress = null;
            this.address = null;
        }
    }
}
