package org.apache.harmony.security.asn1;

public final class BitString {
    public final byte[] bytes;
    public final int unusedBits;
    private static final byte[] SET_MASK = {Byte.MIN_VALUE, 64, 32, 16, 8, 4, 2, 1};
    private static final byte[] RESET_MASK = {Byte.MAX_VALUE, -65, -33, -17, -9, -5, -3, -2};

    public BitString(byte[] bytes, int unusedBits) {
        if (unusedBits < 0 || unusedBits > 7) {
            throw new IllegalArgumentException("Number of unused bits MUST be in range 0-7");
        }
        if (bytes.length == 0 && unusedBits != 0) {
            throw new IllegalArgumentException("For empty bit string unused bits MUST be 0");
        }
        this.bytes = bytes;
        this.unusedBits = unusedBits;
    }

    public BitString(boolean[] values) {
        this.unusedBits = values.length % 8;
        int size = values.length / 8;
        this.bytes = new byte[this.unusedBits != 0 ? size + 1 : size];
        for (int i = 0; i < values.length; i++) {
            setBit(i, values[i]);
        }
    }

    public boolean getBit(int bit) {
        int offset = bit % 8;
        int index = bit / 8;
        return (this.bytes[index] & SET_MASK[offset]) != 0;
    }

    public void setBit(int bit, boolean value) {
        int offset = bit % 8;
        int index = bit / 8;
        if (value) {
            byte[] bArr = this.bytes;
            bArr[index] = (byte) (bArr[index] | SET_MASK[offset]);
        } else {
            byte[] bArr2 = this.bytes;
            bArr2[index] = (byte) (bArr2[index] & RESET_MASK[offset]);
        }
    }

    public boolean[] toBooleanArray() {
        boolean[] result = new boolean[(this.bytes.length * 8) - this.unusedBits];
        for (int i = 0; i < result.length; i++) {
            result[i] = getBit(i);
        }
        return result;
    }
}
