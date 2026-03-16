package com.android.dex;

import com.android.dex.Dex;
import com.android.dex.util.ByteArrayByteInput;
import com.android.dex.util.ByteInput;

public final class EncodedValue implements Comparable<EncodedValue> {
    private final byte[] data;

    public EncodedValue(byte[] data) {
        this.data = data;
    }

    public ByteInput asByteInput() {
        return new ByteArrayByteInput(this.data);
    }

    public byte[] getBytes() {
        return this.data;
    }

    public void writeTo(Dex.Section out) {
        out.write(this.data);
    }

    @Override
    public int compareTo(EncodedValue other) {
        int size = Math.min(this.data.length, other.data.length);
        for (int i = 0; i < size; i++) {
            if (this.data[i] != other.data[i]) {
                return (this.data[i] & Character.DIRECTIONALITY_UNDEFINED) - (other.data[i] & Character.DIRECTIONALITY_UNDEFINED);
            }
        }
        return this.data.length - other.data.length;
    }

    public String toString() {
        return Integer.toHexString(this.data[0] & Character.DIRECTIONALITY_UNDEFINED) + "...(" + this.data.length + ")";
    }
}
