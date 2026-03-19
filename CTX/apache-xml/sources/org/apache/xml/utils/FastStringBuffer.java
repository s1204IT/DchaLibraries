package org.apache.xml.utils;

import java.lang.reflect.Array;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;

public class FastStringBuffer {
    private static final int CARRY_WS = 4;
    static final boolean DEBUG_FORCE_FIXED_CHUNKSIZE = true;
    static final int DEBUG_FORCE_INIT_BITS = 0;
    static final char[] SINGLE_SPACE = {' '};
    public static final int SUPPRESS_BOTH = 3;
    public static final int SUPPRESS_LEADING_WS = 1;
    public static final int SUPPRESS_TRAILING_WS = 2;
    char[][] m_array;
    int m_chunkBits;
    int m_chunkMask;
    int m_chunkSize;
    int m_firstFree;
    FastStringBuffer m_innerFSB;
    int m_lastChunk;
    int m_maxChunkBits;
    int m_rebundleBits;

    public FastStringBuffer(int i, int i2, int i3) {
        this.m_chunkBits = 15;
        this.m_maxChunkBits = 15;
        this.m_rebundleBits = 2;
        this.m_lastChunk = 0;
        this.m_firstFree = 0;
        this.m_innerFSB = null;
        this.m_array = new char[16][];
        if (i > i) {
        }
        this.m_chunkBits = i;
        this.m_maxChunkBits = i;
        this.m_rebundleBits = i3;
        this.m_chunkSize = 1 << i;
        this.m_chunkMask = this.m_chunkSize - 1;
        this.m_array[0] = new char[this.m_chunkSize];
    }

    public FastStringBuffer(int i, int i2) {
        this(i, i2, 2);
    }

    public FastStringBuffer(int i) {
        this(i, 15, 2);
    }

    public FastStringBuffer() {
        this(10, 15, 2);
    }

    public final int size() {
        return (this.m_lastChunk << this.m_chunkBits) + this.m_firstFree;
    }

    public final int length() {
        return (this.m_lastChunk << this.m_chunkBits) + this.m_firstFree;
    }

    public final void reset() {
        this.m_lastChunk = 0;
        this.m_firstFree = 0;
        FastStringBuffer fastStringBuffer = this;
        while (fastStringBuffer.m_innerFSB != null) {
            fastStringBuffer = fastStringBuffer.m_innerFSB;
        }
        this.m_chunkBits = fastStringBuffer.m_chunkBits;
        this.m_chunkSize = fastStringBuffer.m_chunkSize;
        this.m_chunkMask = fastStringBuffer.m_chunkMask;
        this.m_innerFSB = null;
        this.m_array = (char[][]) Array.newInstance((Class<?>) char.class, 16, 0);
        this.m_array[0] = new char[this.m_chunkSize];
    }

    public final void setLength(int i) {
        this.m_lastChunk = i >>> this.m_chunkBits;
        if (this.m_lastChunk == 0 && this.m_innerFSB != null) {
            this.m_innerFSB.setLength(i, this);
            return;
        }
        this.m_firstFree = i & this.m_chunkMask;
        if (this.m_firstFree == 0 && this.m_lastChunk > 0) {
            this.m_lastChunk--;
            this.m_firstFree = this.m_chunkSize;
        }
    }

    private final void setLength(int i, FastStringBuffer fastStringBuffer) {
        this.m_lastChunk = i >>> this.m_chunkBits;
        if (this.m_lastChunk == 0 && this.m_innerFSB != null) {
            this.m_innerFSB.setLength(i, fastStringBuffer);
            return;
        }
        fastStringBuffer.m_chunkBits = this.m_chunkBits;
        fastStringBuffer.m_maxChunkBits = this.m_maxChunkBits;
        fastStringBuffer.m_rebundleBits = this.m_rebundleBits;
        fastStringBuffer.m_chunkSize = this.m_chunkSize;
        fastStringBuffer.m_chunkMask = this.m_chunkMask;
        fastStringBuffer.m_array = this.m_array;
        fastStringBuffer.m_innerFSB = this.m_innerFSB;
        fastStringBuffer.m_lastChunk = this.m_lastChunk;
        fastStringBuffer.m_firstFree = i & this.m_chunkMask;
    }

    public final String toString() {
        int i = (this.m_lastChunk << this.m_chunkBits) + this.m_firstFree;
        return getString(new StringBuffer(i), 0, 0, i).toString();
    }

    public final void append(char c) {
        char[] cArr;
        if (this.m_firstFree < this.m_chunkSize) {
            cArr = this.m_array[this.m_lastChunk];
        } else {
            int length = this.m_array.length;
            if (this.m_lastChunk + 1 == length) {
                char[][] cArr2 = new char[length + 16][];
                System.arraycopy(this.m_array, 0, cArr2, 0, length);
                this.m_array = cArr2;
            }
            char[][] cArr3 = this.m_array;
            int i = this.m_lastChunk + 1;
            this.m_lastChunk = i;
            cArr = cArr3[i];
            if (cArr == null) {
                if (this.m_lastChunk == (1 << this.m_rebundleBits) && this.m_chunkBits < this.m_maxChunkBits) {
                    this.m_innerFSB = new FastStringBuffer(this);
                }
                char[][] cArr4 = this.m_array;
                int i2 = this.m_lastChunk;
                char[] cArr5 = new char[this.m_chunkSize];
                cArr4[i2] = cArr5;
                cArr = cArr5;
            }
            this.m_firstFree = 0;
        }
        int i3 = this.m_firstFree;
        this.m_firstFree = i3 + 1;
        cArr[i3] = c;
    }

    public final void append(String str) {
        int length;
        if (str == null || (length = str.length()) == 0) {
            return;
        }
        char[] cArr = this.m_array[this.m_lastChunk];
        int i = this.m_chunkSize - this.m_firstFree;
        int i2 = 0;
        while (length > 0) {
            if (i > length) {
                i = length;
            }
            int i3 = i2 + i;
            str.getChars(i2, i3, this.m_array[this.m_lastChunk], this.m_firstFree);
            length -= i;
            if (length > 0) {
                int length2 = this.m_array.length;
                if (this.m_lastChunk + 1 == length2) {
                    char[][] cArr2 = new char[length2 + 16][];
                    System.arraycopy(this.m_array, 0, cArr2, 0, length2);
                    this.m_array = cArr2;
                }
                char[][] cArr3 = this.m_array;
                int i4 = this.m_lastChunk + 1;
                this.m_lastChunk = i4;
                if (cArr3[i4] == null) {
                    if (this.m_lastChunk == (1 << this.m_rebundleBits) && this.m_chunkBits < this.m_maxChunkBits) {
                        this.m_innerFSB = new FastStringBuffer(this);
                    }
                    this.m_array[this.m_lastChunk] = new char[this.m_chunkSize];
                }
                i = this.m_chunkSize;
                this.m_firstFree = 0;
            }
            i2 = i3;
        }
        this.m_firstFree += i;
    }

    public final void append(StringBuffer stringBuffer) {
        int length;
        if (stringBuffer == null || (length = stringBuffer.length()) == 0) {
            return;
        }
        char[] cArr = this.m_array[this.m_lastChunk];
        int i = this.m_chunkSize - this.m_firstFree;
        int i2 = 0;
        while (length > 0) {
            if (i > length) {
                i = length;
            }
            int i3 = i2 + i;
            stringBuffer.getChars(i2, i3, this.m_array[this.m_lastChunk], this.m_firstFree);
            length -= i;
            if (length > 0) {
                int length2 = this.m_array.length;
                if (this.m_lastChunk + 1 == length2) {
                    char[][] cArr2 = new char[length2 + 16][];
                    System.arraycopy(this.m_array, 0, cArr2, 0, length2);
                    this.m_array = cArr2;
                }
                char[][] cArr3 = this.m_array;
                int i4 = this.m_lastChunk + 1;
                this.m_lastChunk = i4;
                if (cArr3[i4] == null) {
                    if (this.m_lastChunk == (1 << this.m_rebundleBits) && this.m_chunkBits < this.m_maxChunkBits) {
                        this.m_innerFSB = new FastStringBuffer(this);
                    }
                    this.m_array[this.m_lastChunk] = new char[this.m_chunkSize];
                }
                i = this.m_chunkSize;
                this.m_firstFree = 0;
            }
            i2 = i3;
        }
        this.m_firstFree += i;
    }

    public final void append(char[] cArr, int i, int i2) {
        if (i2 == 0) {
            return;
        }
        char[] cArr2 = this.m_array[this.m_lastChunk];
        int i3 = this.m_chunkSize - this.m_firstFree;
        while (i2 > 0) {
            if (i3 > i2) {
                i3 = i2;
            }
            System.arraycopy(cArr, i, this.m_array[this.m_lastChunk], this.m_firstFree, i3);
            i2 -= i3;
            i += i3;
            if (i2 > 0) {
                int length = this.m_array.length;
                if (this.m_lastChunk + 1 == length) {
                    char[][] cArr3 = new char[length + 16][];
                    System.arraycopy(this.m_array, 0, cArr3, 0, length);
                    this.m_array = cArr3;
                }
                char[][] cArr4 = this.m_array;
                int i4 = this.m_lastChunk + 1;
                this.m_lastChunk = i4;
                if (cArr4[i4] == null) {
                    if (this.m_lastChunk == (1 << this.m_rebundleBits) && this.m_chunkBits < this.m_maxChunkBits) {
                        this.m_innerFSB = new FastStringBuffer(this);
                    }
                    this.m_array[this.m_lastChunk] = new char[this.m_chunkSize];
                }
                i3 = this.m_chunkSize;
                this.m_firstFree = 0;
            }
        }
        this.m_firstFree += i3;
    }

    public final void append(FastStringBuffer fastStringBuffer) {
        int length;
        if (fastStringBuffer == null || (length = fastStringBuffer.length()) == 0) {
            return;
        }
        char[] cArr = this.m_array[this.m_lastChunk];
        int i = this.m_chunkSize - this.m_firstFree;
        int i2 = 0;
        while (length > 0) {
            if (i > length) {
                i = length;
            }
            int i3 = ((fastStringBuffer.m_chunkSize + i2) - 1) >>> fastStringBuffer.m_chunkBits;
            int i4 = fastStringBuffer.m_chunkMask & i2;
            int i5 = fastStringBuffer.m_chunkSize - i4;
            if (i5 > i) {
                i5 = i;
            }
            System.arraycopy(fastStringBuffer.m_array[i3], i4, this.m_array[this.m_lastChunk], this.m_firstFree, i5);
            if (i5 != i) {
                System.arraycopy(fastStringBuffer.m_array[i3 + 1], 0, this.m_array[this.m_lastChunk], this.m_firstFree + i5, i - i5);
            }
            length -= i;
            i2 += i;
            if (length > 0) {
                int length2 = this.m_array.length;
                if (this.m_lastChunk + 1 == length2) {
                    char[][] cArr2 = new char[length2 + 16][];
                    System.arraycopy(this.m_array, 0, cArr2, 0, length2);
                    this.m_array = cArr2;
                }
                char[][] cArr3 = this.m_array;
                int i6 = this.m_lastChunk + 1;
                this.m_lastChunk = i6;
                if (cArr3[i6] == null) {
                    if (this.m_lastChunk == (1 << this.m_rebundleBits) && this.m_chunkBits < this.m_maxChunkBits) {
                        this.m_innerFSB = new FastStringBuffer(this);
                    }
                    this.m_array[this.m_lastChunk] = new char[this.m_chunkSize];
                }
                i = this.m_chunkSize;
                this.m_firstFree = 0;
            }
        }
        this.m_firstFree += i;
    }

    public boolean isWhitespace(int i, int i2) {
        boolean zIsWhiteSpace;
        int i3 = i >>> this.m_chunkBits;
        int i4 = i & this.m_chunkMask;
        int i5 = this.m_chunkSize - i4;
        while (i2 > 0) {
            if (i2 <= i5) {
                i5 = i2;
            }
            if (i3 == 0 && this.m_innerFSB != null) {
                zIsWhiteSpace = this.m_innerFSB.isWhitespace(i4, i5);
            } else {
                zIsWhiteSpace = XMLCharacterRecognizer.isWhiteSpace(this.m_array[i3], i4, i5);
            }
            if (!zIsWhiteSpace) {
                return false;
            }
            i2 -= i5;
            i3++;
            i5 = this.m_chunkSize;
            i4 = 0;
        }
        return true;
    }

    public String getString(int i, int i2) {
        int i3 = this.m_chunkMask & i;
        int i4 = i >>> this.m_chunkBits;
        if (i3 + i2 < this.m_chunkMask && this.m_innerFSB == null) {
            return getOneChunkString(i4, i3, i2);
        }
        return getString(new StringBuffer(i2), i4, i3, i2).toString();
    }

    protected String getOneChunkString(int i, int i2, int i3) {
        return new String(this.m_array[i], i2, i3);
    }

    StringBuffer getString(StringBuffer stringBuffer, int i, int i2) {
        return getString(stringBuffer, i >>> this.m_chunkBits, i & this.m_chunkMask, i2);
    }

    StringBuffer getString(StringBuffer stringBuffer, int i, int i2, int i3) {
        int i4 = (i << this.m_chunkBits) + i2 + i3;
        int i5 = i4 >>> this.m_chunkBits;
        int i6 = i4 & this.m_chunkMask;
        while (i < i5) {
            if (i == 0 && this.m_innerFSB != null) {
                this.m_innerFSB.getString(stringBuffer, i2, this.m_chunkSize - i2);
            } else {
                stringBuffer.append(this.m_array[i], i2, this.m_chunkSize - i2);
            }
            i2 = 0;
            i++;
        }
        if (i5 == 0 && this.m_innerFSB != null) {
            this.m_innerFSB.getString(stringBuffer, i2, i6 - i2);
        } else if (i6 > i2) {
            stringBuffer.append(this.m_array[i5], i2, i6 - i2);
        }
        return stringBuffer;
    }

    public char charAt(int i) {
        int i2 = i >>> this.m_chunkBits;
        if (i2 == 0 && this.m_innerFSB != null) {
            return this.m_innerFSB.charAt(i & this.m_chunkMask);
        }
        return this.m_array[i2][i & this.m_chunkMask];
    }

    public void sendSAXcharacters(ContentHandler contentHandler, int i, int i2) throws SAXException {
        int i3 = i >>> this.m_chunkBits;
        int i4 = this.m_chunkMask & i;
        if (i4 + i2 < this.m_chunkMask && this.m_innerFSB == null) {
            contentHandler.characters(this.m_array[i3], i4, i2);
            return;
        }
        int i5 = i + i2;
        int i6 = i5 >>> this.m_chunkBits;
        int i7 = i5 & this.m_chunkMask;
        while (i3 < i6) {
            if (i3 == 0 && this.m_innerFSB != null) {
                this.m_innerFSB.sendSAXcharacters(contentHandler, i4, this.m_chunkSize - i4);
            } else {
                contentHandler.characters(this.m_array[i3], i4, this.m_chunkSize - i4);
            }
            i4 = 0;
            i3++;
        }
        if (i6 == 0 && this.m_innerFSB != null) {
            this.m_innerFSB.sendSAXcharacters(contentHandler, i4, i7 - i4);
        } else if (i7 > i4) {
            contentHandler.characters(this.m_array[i6], i4, i7 - i4);
        }
    }

    public int sendNormalizedSAXcharacters(ContentHandler contentHandler, int i, int i2) throws SAXException {
        int iSendNormalizedSAXcharacters;
        int i3 = i2 + i;
        int i4 = i & this.m_chunkMask;
        int i5 = i3 >>> this.m_chunkBits;
        int i6 = i3 & this.m_chunkMask;
        int i7 = 1;
        for (int i8 = i >>> this.m_chunkBits; i8 < i5; i8++) {
            if (i8 == 0 && this.m_innerFSB != null) {
                iSendNormalizedSAXcharacters = this.m_innerFSB.sendNormalizedSAXcharacters(contentHandler, i4, this.m_chunkSize - i4);
            } else {
                iSendNormalizedSAXcharacters = sendNormalizedSAXcharacters(this.m_array[i8], i4, this.m_chunkSize - i4, contentHandler, i7);
            }
            i7 = iSendNormalizedSAXcharacters;
            i4 = 0;
        }
        if (i5 == 0 && this.m_innerFSB != null) {
            return this.m_innerFSB.sendNormalizedSAXcharacters(contentHandler, i4, i6 - i4);
        }
        if (i6 > i4) {
            return sendNormalizedSAXcharacters(this.m_array[i5], i4, i6 - i4, contentHandler, i7 | 2);
        }
        return i7;
    }

    static int sendNormalizedSAXcharacters(char[] cArr, int i, int i2, ContentHandler contentHandler, int i3) throws SAXException {
        boolean z = (i3 & 1) != 0;
        boolean z2 = (i3 & 4) != 0;
        int i4 = i2 + i;
        if (z) {
            while (i < i4 && XMLCharacterRecognizer.isWhiteSpace(cArr[i])) {
                i++;
            }
            if (i == i4) {
                return i3;
            }
        }
        while (i < i4) {
            int i5 = i;
            while (i5 < i4 && !XMLCharacterRecognizer.isWhiteSpace(cArr[i5])) {
                i5++;
            }
            if (i != i5) {
                if (z2) {
                    contentHandler.characters(SINGLE_SPACE, 0, 1);
                    z2 = false;
                }
                contentHandler.characters(cArr, i, i5 - i);
            }
            i = i5;
            while (i < i4 && XMLCharacterRecognizer.isWhiteSpace(cArr[i])) {
                i++;
            }
            if (i5 != i) {
                z2 = true;
            }
        }
        return (i3 & 2) | (z2 ? 4 : 0);
    }

    public static void sendNormalizedSAXcharacters(char[] cArr, int i, int i2, ContentHandler contentHandler) throws SAXException {
        sendNormalizedSAXcharacters(cArr, i, i2, contentHandler, 3);
    }

    public void sendSAXComment(LexicalHandler lexicalHandler, int i, int i2) throws SAXException {
        lexicalHandler.comment(getString(i, i2).toCharArray(), 0, i2);
    }

    private void getChars(int i, int i2, char[] cArr, int i3) {
    }

    private FastStringBuffer(FastStringBuffer fastStringBuffer) {
        this.m_chunkBits = 15;
        this.m_maxChunkBits = 15;
        this.m_rebundleBits = 2;
        this.m_lastChunk = 0;
        this.m_firstFree = 0;
        this.m_innerFSB = null;
        this.m_chunkBits = fastStringBuffer.m_chunkBits;
        this.m_maxChunkBits = fastStringBuffer.m_maxChunkBits;
        this.m_rebundleBits = fastStringBuffer.m_rebundleBits;
        this.m_chunkSize = fastStringBuffer.m_chunkSize;
        this.m_chunkMask = fastStringBuffer.m_chunkMask;
        this.m_array = fastStringBuffer.m_array;
        this.m_innerFSB = fastStringBuffer.m_innerFSB;
        this.m_lastChunk = fastStringBuffer.m_lastChunk - 1;
        this.m_firstFree = fastStringBuffer.m_chunkSize;
        fastStringBuffer.m_array = new char[16][];
        fastStringBuffer.m_innerFSB = this;
        fastStringBuffer.m_lastChunk = 1;
        fastStringBuffer.m_firstFree = 0;
        fastStringBuffer.m_chunkBits += this.m_rebundleBits;
        fastStringBuffer.m_chunkSize = 1 << fastStringBuffer.m_chunkBits;
        fastStringBuffer.m_chunkMask = fastStringBuffer.m_chunkSize - 1;
    }
}
