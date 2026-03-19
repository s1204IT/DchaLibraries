package com.android.server.wifi.wificond;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.BitSet;

public class NativeScanResult implements Parcelable {
    private static final int CAPABILITY_SIZE = 16;
    public static final Parcelable.Creator<NativeScanResult> CREATOR = new Parcelable.Creator<NativeScanResult>() {
        @Override
        public NativeScanResult createFromParcel(Parcel parcel) {
            NativeScanResult nativeScanResult = new NativeScanResult();
            nativeScanResult.ssid = parcel.createByteArray();
            nativeScanResult.bssid = parcel.createByteArray();
            nativeScanResult.infoElement = parcel.createByteArray();
            nativeScanResult.frequency = parcel.readInt();
            nativeScanResult.signalMbm = parcel.readInt();
            nativeScanResult.tsf = parcel.readLong();
            int i = parcel.readInt();
            nativeScanResult.capability = new BitSet(16);
            for (int i2 = 0; i2 < 16; i2++) {
                if (((1 << i2) & i) != 0) {
                    nativeScanResult.capability.set(i2);
                }
            }
            nativeScanResult.associated = parcel.readInt() != 0;
            nativeScanResult.radioChainInfos = new ArrayList<>();
            parcel.readTypedList(nativeScanResult.radioChainInfos, RadioChainInfo.CREATOR);
            return nativeScanResult;
        }

        @Override
        public NativeScanResult[] newArray(int i) {
            return new NativeScanResult[i];
        }
    };
    public boolean associated;
    public byte[] bssid;
    public BitSet capability;
    public int frequency;
    public byte[] infoElement;
    public ArrayList<RadioChainInfo> radioChainInfos;
    public int signalMbm;
    public byte[] ssid;
    public long tsf;

    public NativeScanResult() {
    }

    public NativeScanResult(NativeScanResult nativeScanResult) {
        this.ssid = (byte[]) nativeScanResult.ssid.clone();
        this.bssid = (byte[]) nativeScanResult.bssid.clone();
        this.infoElement = (byte[]) nativeScanResult.infoElement.clone();
        this.frequency = nativeScanResult.frequency;
        this.signalMbm = nativeScanResult.signalMbm;
        this.tsf = nativeScanResult.tsf;
        this.capability = (BitSet) nativeScanResult.capability.clone();
        this.associated = nativeScanResult.associated;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByteArray(this.ssid);
        parcel.writeByteArray(this.bssid);
        parcel.writeByteArray(this.infoElement);
        parcel.writeInt(this.frequency);
        parcel.writeInt(this.signalMbm);
        parcel.writeLong(this.tsf);
        int i2 = 0;
        for (int i3 = 0; i3 < 16; i3++) {
            if (this.capability.get(i3)) {
                i2 |= 1 << i3;
            }
        }
        parcel.writeInt(i2);
        parcel.writeInt(this.associated ? 1 : 0);
        parcel.writeTypedList(this.radioChainInfos);
    }
}
