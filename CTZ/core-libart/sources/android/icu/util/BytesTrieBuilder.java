package android.icu.util;

import android.icu.lang.UCharacterEnums;
import android.icu.text.Bidi;
import android.icu.util.StringTrieBuilder;
import java.nio.ByteBuffer;

public final class BytesTrieBuilder extends StringTrieBuilder {
    static final boolean $assertionsDisabled = false;
    private byte[] bytes;
    private int bytesLength;
    private final byte[] intBytes = new byte[5];

    private static final class BytesAsCharSequence implements CharSequence {
        private int len;
        private byte[] s;

        public BytesAsCharSequence(byte[] bArr, int i) {
            this.s = bArr;
            this.len = i;
        }

        @Override
        public char charAt(int i) {
            return (char) (this.s[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED);
        }

        @Override
        public int length() {
            return this.len;
        }

        @Override
        public CharSequence subSequence(int i, int i2) {
            return null;
        }
    }

    public BytesTrieBuilder add(byte[] bArr, int i, int i2) {
        addImpl(new BytesAsCharSequence(bArr, i), i2);
        return this;
    }

    public BytesTrie build(StringTrieBuilder.Option option) {
        buildBytes(option);
        return new BytesTrie(this.bytes, this.bytes.length - this.bytesLength);
    }

    public ByteBuffer buildByteBuffer(StringTrieBuilder.Option option) {
        buildBytes(option);
        return ByteBuffer.wrap(this.bytes, this.bytes.length - this.bytesLength, this.bytesLength);
    }

    private void buildBytes(StringTrieBuilder.Option option) {
        if (this.bytes == null) {
            this.bytes = new byte[1024];
        }
        buildImpl(option);
    }

    public BytesTrieBuilder clear() {
        clearImpl();
        this.bytes = null;
        this.bytesLength = 0;
        return this;
    }

    @Override
    @Deprecated
    protected boolean matchNodesCanHaveValues() {
        return false;
    }

    @Override
    @Deprecated
    protected int getMaxBranchLinearSubNodeLength() {
        return 5;
    }

    @Override
    @Deprecated
    protected int getMinLinearMatch() {
        return 16;
    }

    @Override
    @Deprecated
    protected int getMaxLinearMatchLength() {
        return 16;
    }

    private void ensureCapacity(int i) {
        if (i > this.bytes.length) {
            int length = this.bytes.length;
            do {
                length *= 2;
            } while (length <= i);
            byte[] bArr = new byte[length];
            System.arraycopy(this.bytes, this.bytes.length - this.bytesLength, bArr, bArr.length - this.bytesLength, this.bytesLength);
            this.bytes = bArr;
        }
    }

    @Override
    @Deprecated
    protected int write(int i) {
        int i2 = this.bytesLength + 1;
        ensureCapacity(i2);
        this.bytesLength = i2;
        this.bytes[this.bytes.length - this.bytesLength] = (byte) i;
        return this.bytesLength;
    }

    @Override
    @Deprecated
    protected int write(int i, int i2) {
        int i3 = this.bytesLength + i2;
        ensureCapacity(i3);
        this.bytesLength = i3;
        int length = this.bytes.length - this.bytesLength;
        while (i2 > 0) {
            this.bytes[length] = (byte) this.strings.charAt(i);
            i2--;
            length++;
            i++;
        }
        return this.bytesLength;
    }

    private int write(byte[] bArr, int i) {
        int i2 = this.bytesLength + i;
        ensureCapacity(i2);
        this.bytesLength = i2;
        System.arraycopy(bArr, 0, this.bytes, this.bytes.length - this.bytesLength, i);
        return this.bytesLength;
    }

    @Override
    @Deprecated
    protected int writeValueAndFinal(int i, boolean z) {
        int i2;
        int i3;
        if (i >= 0 && i <= 64) {
            return write(((16 + i) << 1) | (z ? 1 : 0));
        }
        int i4 = 2;
        if (i < 0 || i > 16777215) {
            this.intBytes[0] = Bidi.LEVEL_DEFAULT_RTL;
            this.intBytes[1] = (byte) (i >> 24);
            this.intBytes[2] = (byte) (i >> 16);
            this.intBytes[3] = (byte) (i >> 8);
            this.intBytes[4] = (byte) i;
            i2 = 5;
        } else {
            if (i <= 6911) {
                this.intBytes[0] = (byte) (81 + (i >> 8));
                i3 = 1;
            } else {
                if (i <= 1179647) {
                    this.intBytes[0] = (byte) (108 + (i >> 16));
                    i4 = 1;
                } else {
                    this.intBytes[0] = Bidi.LEVEL_DEFAULT_LTR;
                    this.intBytes[1] = (byte) (i >> 16);
                }
                i3 = i4 + 1;
                this.intBytes[i4] = (byte) (i >> 8);
            }
            i2 = i3 + 1;
            this.intBytes[i3] = (byte) i;
        }
        this.intBytes[0] = (byte) ((z ? 1 : 0) | (this.intBytes[0] << 1));
        return write(this.intBytes, i2);
    }

    @Override
    @Deprecated
    protected int writeValueAndType(boolean z, int i, int i2) {
        int iWrite = write(i2);
        if (z) {
            return writeValueAndFinal(i, false);
        }
        return iWrite;
    }

    @Override
    @Deprecated
    protected int writeDeltaTo(int i) {
        int i2;
        int i3 = this.bytesLength - i;
        if (i3 <= 191) {
            return write(i3);
        }
        if (i3 <= 12287) {
            this.intBytes[0] = (byte) (192 + (i3 >> 8));
            i2 = 1;
        } else {
            if (i3 <= 917503) {
                this.intBytes[0] = (byte) (240 + (i3 >> 16));
                i2 = 2;
            } else {
                if (i3 <= 16777215) {
                    this.intBytes[0] = -2;
                    i2 = 3;
                } else {
                    this.intBytes[0] = -1;
                    this.intBytes[1] = (byte) (i3 >> 24);
                    i2 = 4;
                }
                this.intBytes[1] = (byte) (i3 >> 16);
            }
            this.intBytes[1] = (byte) (i3 >> 8);
        }
        int i4 = i2 + 1;
        this.intBytes[i2] = (byte) i3;
        return write(this.intBytes, i4);
    }
}
