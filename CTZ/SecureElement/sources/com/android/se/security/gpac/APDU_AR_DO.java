package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class APDU_AR_DO extends BerTlv {
    public static final int TAG = 208;
    private boolean mApduAllowed;
    private ArrayList<byte[]> mApduHeader;
    private ArrayList<byte[]> mFilterMask;

    public APDU_AR_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mApduAllowed = false;
        this.mApduHeader = new ArrayList<>();
        this.mFilterMask = new ArrayList<>();
    }

    public APDU_AR_DO(boolean z) {
        super(null, TAG, 0, 0);
        this.mApduAllowed = false;
        this.mApduHeader = new ArrayList<>();
        this.mFilterMask = new ArrayList<>();
        this.mApduAllowed = z;
    }

    public APDU_AR_DO(ArrayList<byte[]> arrayList, ArrayList<byte[]> arrayList2) {
        super(null, TAG, 0, 0);
        this.mApduAllowed = false;
        this.mApduHeader = new ArrayList<>();
        this.mFilterMask = new ArrayList<>();
        this.mApduHeader = arrayList;
        this.mFilterMask = arrayList2;
    }

    public boolean isApduAllowed() {
        return this.mApduAllowed;
    }

    public ArrayList<byte[]> getApduHeaderList() {
        return this.mApduHeader;
    }

    public ArrayList<byte[]> getFilterMaskList() {
        return this.mFilterMask;
    }

    @Override
    public void interpret() throws ParserException {
        this.mApduAllowed = false;
        this.mApduHeader.clear();
        this.mFilterMask.clear();
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for APDU_AR_DO!");
        }
        if (getValueLength() == 1) {
            if (rawData[valueIndex] != 0 && rawData[valueIndex] != 1) {
                throw new ParserException("Invalid value of APDU-AR-DO : " + String.format("%02x", Integer.valueOf(rawData[valueIndex] & 255)));
            }
            this.mApduAllowed = rawData[valueIndex] == 1;
            return;
        }
        if (getValueLength() % 8 == 0 && getValueLength() != 0) {
            this.mApduAllowed = true;
            for (int i = valueIndex; i < getValueLength() + valueIndex; i += 8) {
                byte[] bArr = {rawData[i + 0], rawData[i + 1], rawData[i + 2], rawData[i + 3]};
                byte[] bArr2 = {rawData[i + 4], rawData[i + 5], rawData[i + 6], rawData[i + 7]};
                this.mApduHeader.add(bArr);
                this.mFilterMask.add(bArr2);
            }
            return;
        }
        if (getValueLength() == 0) {
            this.mApduAllowed = false;
            return;
        }
        throw new ParserException("Invalid length of APDU-AR-DO!");
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        if (this.mApduHeader.size() != this.mFilterMask.size()) {
            throw new DO_Exception("APDU filter is invalid");
        }
        byteArrayOutputStream.write(getTag());
        if (this.mApduHeader.size() == 0) {
            byteArrayOutputStream.write(1);
            byteArrayOutputStream.write(this.mApduAllowed ? 1 : 0);
            return;
        }
        ByteArrayOutputStream byteArrayOutputStream2 = new ByteArrayOutputStream();
        for (int i = 0; i < this.mApduHeader.size(); i++) {
            byte[] bArr = this.mApduHeader.get(i);
            byte[] bArr2 = this.mFilterMask.get(i);
            if (bArr.length != 4 || bArr2.length != 4) {
                throw new DO_Exception("APDU filter is invalid!");
            }
            try {
                byteArrayOutputStream2.write(bArr);
                byteArrayOutputStream2.write(bArr2);
            } catch (IOException e) {
                throw new DO_Exception("APDU Filter Memory IO problem! " + e.getMessage());
            }
        }
        BerTlv.encodeLength(byteArrayOutputStream2.size(), byteArrayOutputStream);
        try {
            byteArrayOutputStream.write(byteArrayOutputStream2.toByteArray());
        } catch (IOException e2) {
            throw new DO_Exception("APDU Filter Memory IO problem! " + e2.getMessage());
        }
    }
}
