package android.icu.util;

import android.icu.util.StringTrieBuilder;
import java.nio.CharBuffer;

public final class CharsTrieBuilder extends StringTrieBuilder {

    static final boolean f107assertionsDisabled;
    private char[] chars;
    private int charsLength;
    private final char[] intUnits = new char[3];

    static {
        f107assertionsDisabled = !CharsTrieBuilder.class.desiredAssertionStatus();
    }

    public CharsTrieBuilder add(CharSequence s, int value) {
        addImpl(s, value);
        return this;
    }

    public CharsTrie build(StringTrieBuilder.Option buildOption) {
        return new CharsTrie(buildCharSequence(buildOption), 0);
    }

    public CharSequence buildCharSequence(StringTrieBuilder.Option buildOption) {
        buildChars(buildOption);
        return CharBuffer.wrap(this.chars, this.chars.length - this.charsLength, this.charsLength);
    }

    private void buildChars(StringTrieBuilder.Option buildOption) {
        if (this.chars == null) {
            this.chars = new char[1024];
        }
        buildImpl(buildOption);
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

    private void ensureCapacity(int length) {
        if (length <= this.chars.length) {
            return;
        }
        int newCapacity = this.chars.length;
        do {
            newCapacity *= 2;
        } while (newCapacity <= length);
        char[] newChars = new char[newCapacity];
        System.arraycopy(this.chars, this.chars.length - this.charsLength, newChars, newChars.length - this.charsLength, this.charsLength);
        this.chars = newChars;
    }

    @Override
    @Deprecated
    protected int write(int unit) {
        int newLength = this.charsLength + 1;
        ensureCapacity(newLength);
        this.charsLength = newLength;
        this.chars[this.chars.length - this.charsLength] = (char) unit;
        return this.charsLength;
    }

    @Override
    @Deprecated
    protected int write(int offset, int length) {
        int newLength = this.charsLength + length;
        ensureCapacity(newLength);
        this.charsLength = newLength;
        int charsOffset = this.chars.length - this.charsLength;
        int charsOffset2 = charsOffset;
        int offset2 = offset;
        while (length > 0) {
            this.chars[charsOffset2] = this.strings.charAt(offset2);
            length--;
            charsOffset2++;
            offset2++;
        }
        return this.charsLength;
    }

    private int write(char[] s, int length) {
        int newLength = this.charsLength + length;
        ensureCapacity(newLength);
        this.charsLength = newLength;
        System.arraycopy(s, 0, this.chars, this.chars.length - this.charsLength, length);
        return this.charsLength;
    }

    @Override
    @Deprecated
    protected int writeValueAndFinal(int i, boolean isFinal) {
        int length;
        if (i >= 0 && i <= 16383) {
            return write((isFinal ? (char) 32768 : (char) 0) | i);
        }
        if (i < 0 || i > 1073676287) {
            this.intUnits[0] = 32767;
            this.intUnits[1] = (char) (i >> 16);
            this.intUnits[2] = (char) i;
            length = 3;
        } else {
            this.intUnits[0] = (char) ((i >> 16) + 16384);
            this.intUnits[1] = (char) i;
            length = 2;
        }
        this.intUnits[0] = (char) ((isFinal ? (char) 32768 : (char) 0) | this.intUnits[0]);
        return write(this.intUnits, length);
    }

    @Override
    @Deprecated
    protected int writeValueAndType(boolean hasValue, int value, int node) {
        int length;
        if (!hasValue) {
            return write(node);
        }
        if (value < 0 || value > 16646143) {
            this.intUnits[0] = 32704;
            this.intUnits[1] = (char) (value >> 16);
            this.intUnits[2] = (char) value;
            length = 3;
        } else if (value <= 255) {
            this.intUnits[0] = (char) ((value + 1) << 6);
            length = 1;
        } else {
            this.intUnits[0] = (char) (((value >> 10) & 32704) + 16448);
            this.intUnits[1] = (char) value;
            length = 2;
        }
        char[] cArr = this.intUnits;
        cArr[0] = (char) (cArr[0] | ((char) node));
        return write(this.intUnits, length);
    }

    @Override
    @Deprecated
    protected int writeDeltaTo(int jumpTarget) {
        int length;
        int i = this.charsLength - jumpTarget;
        if (!f107assertionsDisabled) {
            if (!(i >= 0)) {
                throw new AssertionError();
            }
        }
        if (i <= 64511) {
            return write(i);
        }
        if (i <= 67043327) {
            this.intUnits[0] = (char) ((i >> 16) + 64512);
            length = 1;
        } else {
            this.intUnits[0] = 65535;
            this.intUnits[1] = (char) (i >> 16);
            length = 2;
        }
        this.intUnits[length] = (char) i;
        return write(this.intUnits, length + 1);
    }
}
