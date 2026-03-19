package android.icu.util;

import android.icu.impl.Utility;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacterEnums;
import java.nio.ByteBuffer;

public class ByteArrayWrapper implements Comparable<ByteArrayWrapper> {
    public byte[] bytes;
    public int size;

    public ByteArrayWrapper() {
    }

    public ByteArrayWrapper(byte[] bArr, int i) {
        if ((bArr == null && i != 0) || i < 0 || (bArr != null && i > bArr.length)) {
            throw new IndexOutOfBoundsException("illegal size: " + i);
        }
        this.bytes = bArr;
        this.size = i;
    }

    public ByteArrayWrapper(ByteBuffer byteBuffer) {
        this.size = byteBuffer.limit();
        this.bytes = new byte[this.size];
        byteBuffer.get(this.bytes, 0, this.size);
    }

    public ByteArrayWrapper ensureCapacity(int i) {
        if (this.bytes == null || this.bytes.length < i) {
            byte[] bArr = new byte[i];
            if (this.bytes != null) {
                copyBytes(this.bytes, 0, bArr, 0, this.size);
            }
            this.bytes = bArr;
        }
        return this;
    }

    public final ByteArrayWrapper set(byte[] bArr, int i, int i2) {
        this.size = 0;
        append(bArr, i, i2);
        return this;
    }

    public final ByteArrayWrapper append(byte[] bArr, int i, int i2) {
        int i3 = i2 - i;
        ensureCapacity(this.size + i3);
        copyBytes(bArr, i, this.bytes, this.size, i3);
        this.size += i3;
        return this;
    }

    public final byte[] releaseBytes() {
        byte[] bArr = this.bytes;
        this.bytes = null;
        this.size = 0;
        return bArr;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.size; i++) {
            if (i != 0) {
                sb.append(Padder.FALLBACK_PADDING_STRING);
            }
            sb.append(Utility.hex(this.bytes[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED, 2));
        }
        return sb.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        try {
            ByteArrayWrapper byteArrayWrapper = (ByteArrayWrapper) obj;
            if (this.size != byteArrayWrapper.size) {
                return false;
            }
            for (int i = 0; i < this.size; i++) {
                if (this.bytes[i] != byteArrayWrapper.bytes[i]) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public int hashCode() {
        int length = this.bytes.length;
        for (int i = 0; i < this.size; i++) {
            length = this.bytes[i] + (37 * length);
        }
        return length;
    }

    @Override
    public int compareTo(ByteArrayWrapper byteArrayWrapper) {
        if (this == byteArrayWrapper) {
            return 0;
        }
        int i = this.size < byteArrayWrapper.size ? this.size : byteArrayWrapper.size;
        for (int i2 = 0; i2 < i; i2++) {
            if (this.bytes[i2] != byteArrayWrapper.bytes[i2]) {
                return (this.bytes[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) - (byteArrayWrapper.bytes[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
            }
        }
        return this.size - byteArrayWrapper.size;
    }

    private static final void copyBytes(byte[] bArr, int i, byte[] bArr2, int i2, int i3) {
        if (i3 >= 64) {
            System.arraycopy(bArr, i, bArr2, i2, i3);
            return;
        }
        while (true) {
            i3--;
            if (i3 >= 0) {
                bArr2[i2] = bArr[i];
                i++;
                i2++;
            } else {
                return;
            }
        }
    }
}
