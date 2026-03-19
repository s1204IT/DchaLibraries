package com.android.se.security.gpac;

public class Response_RefreshTag_DO extends BerTlv {
    public static final int TAG = 57120;
    private long mRefreshTag;
    private byte[] mRefreshTagArray;

    public Response_RefreshTag_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mRefreshTagArray = null;
    }

    public long getRefreshTag() {
        return this.mRefreshTag;
    }

    public byte[] getRefreshTagArray() {
        return this.mRefreshTagArray;
    }

    @Override
    public void interpret() throws ParserException {
        this.mRefreshTag = 0L;
        if (super.getValueLength() != 8) {
            throw new ParserException("Invalid length of RefreshTag DO!");
        }
        byte[] rawData = super.getRawData();
        int valueIndex = super.getValueIndex();
        if (super.getValueLength() + valueIndex > rawData.length) {
            throw new ParserException("Not enough data for RefreshTag DO!");
        }
        this.mRefreshTagArray = new byte[super.getValueLength()];
        System.arraycopy(rawData, valueIndex, this.mRefreshTagArray, 0, this.mRefreshTagArray.length);
        int i = valueIndex + 1;
        this.mRefreshTag = ((long) rawData[valueIndex]) << 56;
        int i2 = i + 1;
        this.mRefreshTag += ((long) rawData[i]) << 48;
        int i3 = i2 + 1;
        this.mRefreshTag += ((long) rawData[i2]) << 40;
        int i4 = i3 + 1;
        this.mRefreshTag += ((long) rawData[i3]) << 32;
        int i5 = i4 + 1;
        this.mRefreshTag += ((long) rawData[i4]) << 24;
        int i6 = i5 + 1;
        this.mRefreshTag += ((long) rawData[i5]) << 16;
        this.mRefreshTag += ((long) rawData[i6]) << 8;
        this.mRefreshTag += (long) rawData[i6 + 1];
    }
}
