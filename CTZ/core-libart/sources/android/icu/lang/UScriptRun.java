package android.icu.lang;

import android.icu.text.UTF16;

@Deprecated
public final class UScriptRun {
    private char[] emptyCharArray;
    private int fixupCount;
    private int parenSP;
    private int pushCount;
    private int scriptCode;
    private int scriptLimit;
    private int scriptStart;
    private char[] text;
    private int textIndex;
    private int textLimit;
    private int textStart;
    private static int PAREN_STACK_DEPTH = 32;
    private static ParenStackEntry[] parenStack = new ParenStackEntry[PAREN_STACK_DEPTH];
    private static int[] pairedChars = {40, 41, 60, 62, 91, 93, 123, 125, 171, 187, 8216, 8217, 8220, 8221, 8249, 8250, 12296, 12297, 12298, 12299, 12300, 12301, 12302, 12303, 12304, 12305, 12308, 12309, 12310, 12311, 12312, 12313, 12314, 12315};
    private static int pairedCharPower = 1 << highBit(pairedChars.length);
    private static int pairedCharExtra = pairedChars.length - pairedCharPower;

    @Deprecated
    public UScriptRun() {
        this.emptyCharArray = new char[0];
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        reset((char[]) null, 0, 0);
    }

    @Deprecated
    public UScriptRun(String str) {
        this.emptyCharArray = new char[0];
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        reset(str);
    }

    @Deprecated
    public UScriptRun(String str, int i, int i2) {
        this.emptyCharArray = new char[0];
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        reset(str, i, i2);
    }

    @Deprecated
    public UScriptRun(char[] cArr) {
        this.emptyCharArray = new char[0];
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        reset(cArr);
    }

    @Deprecated
    public UScriptRun(char[] cArr, int i, int i2) {
        this.emptyCharArray = new char[0];
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        reset(cArr, i, i2);
    }

    @Deprecated
    public final void reset() {
        while (stackIsNotEmpty()) {
            pop();
        }
        this.scriptStart = this.textStart;
        this.scriptLimit = this.textStart;
        this.scriptCode = -1;
        this.parenSP = -1;
        this.pushCount = 0;
        this.fixupCount = 0;
        this.textIndex = this.textStart;
    }

    @Deprecated
    public final void reset(int i, int i2) throws IllegalArgumentException {
        int length;
        if (this.text != null) {
            length = this.text.length;
        } else {
            length = 0;
        }
        if (i < 0 || i2 < 0 || i > length - i2) {
            throw new IllegalArgumentException();
        }
        this.textStart = i;
        this.textLimit = i + i2;
        reset();
    }

    @Deprecated
    public final void reset(char[] cArr, int i, int i2) {
        if (cArr == null) {
            cArr = this.emptyCharArray;
        }
        this.text = cArr;
        reset(i, i2);
    }

    @Deprecated
    public final void reset(char[] cArr) {
        int length;
        if (cArr != null) {
            length = cArr.length;
        } else {
            length = 0;
        }
        reset(cArr, 0, length);
    }

    @Deprecated
    public final void reset(String str, int i, int i2) {
        char[] charArray;
        if (str != null) {
            charArray = str.toCharArray();
        } else {
            charArray = null;
        }
        reset(charArray, i, i2);
    }

    @Deprecated
    public final void reset(String str) {
        int length;
        if (str != null) {
            length = str.length();
        } else {
            length = 0;
        }
        reset(str, 0, length);
    }

    @Deprecated
    public final int getScriptStart() {
        return this.scriptStart;
    }

    @Deprecated
    public final int getScriptLimit() {
        return this.scriptLimit;
    }

    @Deprecated
    public final int getScriptCode() {
        return this.scriptCode;
    }

    @Deprecated
    public final boolean next() {
        if (this.scriptLimit >= this.textLimit) {
            return false;
        }
        this.scriptCode = 0;
        this.scriptStart = this.scriptLimit;
        syncFixup();
        while (true) {
            if (this.textIndex >= this.textLimit) {
                break;
            }
            int iCharAt = UTF16.charAt(this.text, this.textStart, this.textLimit, this.textIndex - this.textStart);
            int charCount = UTF16.getCharCount(iCharAt);
            int script = UScript.getScript(iCharAt);
            int pairIndex = getPairIndex(iCharAt);
            this.textIndex += charCount;
            if (pairIndex >= 0) {
                if ((pairIndex & 1) == 0) {
                    push(pairIndex, this.scriptCode);
                } else {
                    int i = pairIndex & (-2);
                    while (stackIsNotEmpty() && top().pairIndex != i) {
                        pop();
                    }
                    if (stackIsNotEmpty()) {
                        script = top().scriptCode;
                    }
                }
            }
            if (sameScript(this.scriptCode, script)) {
                if (this.scriptCode <= 1 && script > 1) {
                    this.scriptCode = script;
                    fixup(this.scriptCode);
                }
                if (pairIndex >= 0 && (pairIndex & 1) != 0) {
                    pop();
                }
            } else {
                this.textIndex -= charCount;
                break;
            }
        }
        this.scriptLimit = this.textIndex;
        return true;
    }

    private static boolean sameScript(int i, int i2) {
        return i <= 1 || i2 <= 1 || i == i2;
    }

    private static final class ParenStackEntry {
        int pairIndex;
        int scriptCode;

        public ParenStackEntry(int i, int i2) {
            this.pairIndex = i;
            this.scriptCode = i2;
        }
    }

    private static final int mod(int i) {
        return i % PAREN_STACK_DEPTH;
    }

    private static final int inc(int i, int i2) {
        return mod(i + i2);
    }

    private static final int inc(int i) {
        return inc(i, 1);
    }

    private static final int dec(int i, int i2) {
        return mod((i + PAREN_STACK_DEPTH) - i2);
    }

    private static final int dec(int i) {
        return dec(i, 1);
    }

    private static final int limitInc(int i) {
        if (i < PAREN_STACK_DEPTH) {
            return i + 1;
        }
        return i;
    }

    private final boolean stackIsEmpty() {
        return this.pushCount <= 0;
    }

    private final boolean stackIsNotEmpty() {
        return !stackIsEmpty();
    }

    private final void push(int i, int i2) {
        this.pushCount = limitInc(this.pushCount);
        this.fixupCount = limitInc(this.fixupCount);
        this.parenSP = inc(this.parenSP);
        parenStack[this.parenSP] = new ParenStackEntry(i, i2);
    }

    private final void pop() {
        if (stackIsEmpty()) {
            return;
        }
        parenStack[this.parenSP] = null;
        if (this.fixupCount > 0) {
            this.fixupCount--;
        }
        this.pushCount--;
        this.parenSP = dec(this.parenSP);
        if (stackIsEmpty()) {
            this.parenSP = -1;
        }
    }

    private final ParenStackEntry top() {
        return parenStack[this.parenSP];
    }

    private final void syncFixup() {
        this.fixupCount = 0;
    }

    private final void fixup(int i) {
        int iDec = dec(this.parenSP, this.fixupCount);
        while (true) {
            int i2 = this.fixupCount;
            this.fixupCount = i2 - 1;
            if (i2 > 0) {
                iDec = inc(iDec);
                parenStack[iDec].scriptCode = i;
            } else {
                return;
            }
        }
    }

    private static final byte highBit(int i) {
        if (i <= 0) {
            return (byte) -32;
        }
        byte b = 0;
        if (i >= 65536) {
            i >>= 16;
            b = (byte) 16;
        }
        if (i >= 256) {
            i >>= 8;
            b = (byte) (b + 8);
        }
        if (i >= 16) {
            i >>= 4;
            b = (byte) (b + 4);
        }
        if (i >= 4) {
            i >>= 2;
            b = (byte) (b + 2);
        }
        if (i >= 2) {
            return (byte) (b + 1);
        }
        return b;
    }

    private static int getPairIndex(int i) {
        int i2;
        int i3 = pairedCharPower;
        if (i >= pairedChars[pairedCharExtra]) {
            i2 = pairedCharExtra;
        } else {
            i2 = 0;
        }
        while (i3 > 1) {
            i3 >>= 1;
            int i4 = i2 + i3;
            if (i >= pairedChars[i4]) {
                i2 = i4;
            }
        }
        if (pairedChars[i2] != i) {
            return -1;
        }
        return i2;
    }
}
