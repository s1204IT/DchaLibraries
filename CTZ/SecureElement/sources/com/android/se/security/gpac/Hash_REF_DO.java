package com.android.se.security.gpac;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Hash_REF_DO extends BerTlv {
    public static final int SHA1_LEN = 20;
    public static final int TAG = 193;
    private byte[] mHash;

    public Hash_REF_DO(byte[] bArr, int i, int i2) {
        super(bArr, TAG, i, i2);
        this.mHash = new byte[0];
    }

    public Hash_REF_DO(byte[] bArr) {
        super(bArr, TAG, 0, bArr == null ? 0 : bArr.length);
        this.mHash = new byte[0];
        if (bArr != null) {
            this.mHash = bArr;
        }
    }

    public Hash_REF_DO() {
        super(null, TAG, 0, 0);
        this.mHash = new byte[0];
    }

    public static boolean equals(Hash_REF_DO hash_REF_DO, Hash_REF_DO hash_REF_DO2) {
        if (hash_REF_DO == null) {
            return hash_REF_DO2 == null;
        }
        return hash_REF_DO.equals(hash_REF_DO2);
    }

    public byte[] getHash() {
        return this.mHash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        sb.append("Hash_REF_DO: ");
        try {
            build(byteArrayOutputStream);
            sb.append(BerTlv.toHex(byteArrayOutputStream.toByteArray()));
        } catch (Exception e) {
            sb.append(e.getLocalizedMessage());
        }
        return sb.toString();
    }

    @Override
    public void interpret() throws ParserException {
        this.mHash = new byte[0];
        byte[] rawData = getRawData();
        int valueIndex = getValueIndex();
        if (getValueLength() != 0 && getValueLength() != 20) {
            throw new ParserException("Invalid value length for Hash-REF-DO!");
        }
        if (getValueLength() == 20) {
            if (getValueLength() + valueIndex > rawData.length) {
                throw new ParserException("Not enough data for Hash-REF-DO!");
            }
            this.mHash = new byte[getValueLength()];
            System.arraycopy(rawData, valueIndex, this.mHash, 0, getValueLength());
        }
    }

    @Override
    public void build(ByteArrayOutputStream byteArrayOutputStream) throws DO_Exception {
        if (this.mHash.length == 20 && this.mHash.length == 0) {
            throw new DO_Exception("Hash value must be 20 bytes in length!");
        }
        byteArrayOutputStream.write(getTag());
        try {
            byteArrayOutputStream.write(this.mHash.length);
            byteArrayOutputStream.write(this.mHash);
        } catch (IOException e) {
            throw new DO_Exception("Hash could not be written!");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof Hash_REF_DO) && getTag() == obj.getTag()) {
            return Arrays.equals(this.mHash, obj.mHash);
        }
        return false;
    }
}
