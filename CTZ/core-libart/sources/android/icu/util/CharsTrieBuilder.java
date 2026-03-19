package android.icu.util;

import android.icu.impl.Normalizer2Impl;
import android.icu.util.StringTrieBuilder;
import java.nio.CharBuffer;

public final class CharsTrieBuilder extends StringTrieBuilder {
    static final boolean $assertionsDisabled = false;
    private char[] chars;
    private int charsLength;
    private final char[] intUnits = new char[3];

    public CharsTrieBuilder add(CharSequence charSequence, int i) {
        addImpl(charSequence, i);
        return this;
    }

    public CharsTrie build(StringTrieBuilder.Option option) {
        return new CharsTrie(buildCharSequence(option), 0);
    }

    public CharSequence buildCharSequence(StringTrieBuilder.Option option) {
        buildChars(option);
        return CharBuffer.wrap(this.chars, this.chars.length - this.charsLength, this.charsLength);
    }

    private void buildChars(StringTrieBuilder.Option option) {
        if (this.chars == null) {
            this.chars = new char[1024];
        }
        buildImpl(option);
    }

    public CharsTrieBuilder clear() {
        clearImpl();
        this.chars = null;
        this.charsLength = 0;
        return this;
    }

    @Override
    @Deprecated
    protected boolean matchNodesCanHaveValues() {
        return true;
    }

    @Override
    @Deprecated
    protected int getMaxBranchLinearSubNodeLength() {
        return 5;
    }

    @Override
    @Deprecated
    protected int getMinLinearMatch() {
        return 48;
    }

    @Override
    @Deprecated
    protected int getMaxLinearMatchLength() {
        return 16;
    }

    private void ensureCapacity(int i) {
        if (i > this.chars.length) {
            int length = this.chars.length;
            do {
                length *= 2;
            } while (length <= i);
            char[] cArr = new char[length];
            System.arraycopy(this.chars, this.chars.length - this.charsLength, cArr, cArr.length - this.charsLength, this.charsLength);
            this.chars = cArr;
        }
    }

    @Override
    @Deprecated
    protected int write(int i) {
        int i2 = this.charsLength + 1;
        ensureCapacity(i2);
        this.charsLength = i2;
        this.chars[this.chars.length - this.charsLength] = (char) i;
        return this.charsLength;
    }

    @Override
    @Deprecated
    protected int write(int i, int i2) {
        int i3 = this.charsLength + i2;
        ensureCapacity(i3);
        this.charsLength = i3;
        int length = this.chars.length - this.charsLength;
        while (i2 > 0) {
            this.chars[length] = this.strings.charAt(i);
            i2--;
            length++;
            i++;
        }
        return this.charsLength;
    }

    private int write(char[] cArr, int i) {
        int i2 = this.charsLength + i;
        ensureCapacity(i2);
        this.charsLength = i2;
        System.arraycopy(cArr, 0, this.chars, this.chars.length - this.charsLength, i);
        return this.charsLength;
    }

    @Override
    @Deprecated
    protected int writeValueAndFinal(int i, boolean z) {
        if (i >= 0 && i <= 16383) {
            return write(i | (z ? (char) 32768 : (char) 0));
        }
        int i2 = 2;
        if (i < 0 || i > 1073676287) {
            this.intUnits[0] = 32767;
            this.intUnits[1] = (char) (i >> 16);
            this.intUnits[2] = (char) i;
            i2 = 3;
        } else {
            this.intUnits[0] = (char) (16384 + (i >> 16));
            this.intUnits[1] = (char) i;
        }
        this.intUnits[0] = (char) (this.intUnits[0] | (z ? (char) 32768 : (char) 0));
        return write(this.intUnits, i2);
    }

    @Override
    @Deprecated
    protected int writeValueAndType(boolean z, int i, int i2) {
        if (!z) {
            return write(i2);
        }
        int i3 = 2;
        if (i < 0 || i > 16646143) {
            this.intUnits[0] = 32704;
            this.intUnits[1] = (char) (i >> 16);
            this.intUnits[2] = (char) i;
            i3 = 3;
        } else if (i <= 255) {
            this.intUnits[0] = (char) ((i + 1) << 6);
            i3 = 1;
        } else {
            this.intUnits[0] = (char) (16448 + (32704 & (i >> 10)));
            this.intUnits[1] = (char) i;
        }
        char[] cArr = this.intUnits;
        cArr[0] = (char) (((char) i2) | cArr[0]);
        return write(this.intUnits, i3);
    }

    @Override
    @Deprecated
    protected int writeDeltaTo(int i) {
        int i2 = this.charsLength - i;
        if (i2 <= 64511) {
            return write(i2);
        }
        int i3 = 1;
        if (i2 <= 67043327) {
            this.intUnits[0] = (char) (Normalizer2Impl.MIN_NORMAL_MAYBE_YES + (i2 >> 16));
        } else {
            this.intUnits[0] = 65535;
            this.intUnits[1] = (char) (i2 >> 16);
            i3 = 2;
        }
        this.intUnits[i3] = (char) i2;
        return write(this.intUnits, i3 + 1);
    }
}
