package android.icu.text;

import android.icu.impl.ICUBinary;
import android.icu.impl.Trie2;
import android.icu.impl.locale.LanguageTag;
import android.icu.impl.number.Padder;
import android.icu.lang.UCharacterEnums;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class RBBIDataWrapper {
    static final int ACCEPTING = 0;
    static final int DATA_FORMAT = 1114794784;
    static final int DH_CATCOUNT = 3;
    static final int DH_FORMATVERSION = 1;
    static final int DH_FTABLE = 4;
    static final int DH_FTABLELEN = 5;
    static final int DH_LENGTH = 2;
    static final int DH_MAGIC = 0;
    static final int DH_RTABLE = 6;
    static final int DH_RTABLELEN = 7;
    static final int DH_RULESOURCE = 14;
    static final int DH_RULESOURCELEN = 15;
    static final int DH_SFTABLE = 8;
    static final int DH_SFTABLELEN = 9;
    static final int DH_SIZE = 24;
    static final int DH_SRTABLE = 10;
    static final int DH_SRTABLELEN = 11;
    static final int DH_STATUSTABLE = 16;
    static final int DH_STATUSTABLELEN = 17;
    static final int DH_TRIE = 12;
    static final int DH_TRIELEN = 13;
    static final int FLAGS = 4;
    static final int FORMAT_VERSION = 67108864;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable();
    static final int LOOKAHEAD = 1;
    static final int NEXTSTATES = 4;
    static final int NUMSTATES = 0;
    static final int RBBI_BOF_REQUIRED = 2;
    static final int RBBI_LOOKAHEAD_HARD_BREAK = 1;
    static final int RESERVED = 3;
    static final int ROWLEN = 2;
    private static final int ROW_DATA = 8;
    static final int TAGIDX = 2;
    short[] fFTable;
    RBBIDataHeader fHeader;
    short[] fRTable;
    String fRuleSource;
    short[] fSFTable;
    short[] fSRTable;
    int[] fStatusTable;
    Trie2 fTrie;
    private boolean isBigEndian;

    static final class RBBIDataHeader {
        int fCatCount;
        int fFTable;
        int fFTableLen;
        int fLength;
        int fRTable;
        int fRTableLen;
        int fRuleSource;
        int fRuleSourceLen;
        int fSFTable;
        int fSFTableLen;
        int fSRTable;
        int fSRTableLen;
        int fStatusTable;
        int fStatusTableLen;
        int fTrie;
        int fTrieLen;
        int fMagic = 0;
        byte[] fFormatVersion = new byte[4];
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] bArr) {
            return (((bArr[0] << UCharacterEnums.ECharacterCategory.MATH_SYMBOL) + (bArr[1] << 16)) + (bArr[2] << 8)) + bArr[3] == 67108864;
        }
    }

    int getRowIndex(int i) {
        return 8 + (i * (this.fHeader.fCatCount + 4));
    }

    RBBIDataWrapper() {
    }

    static RBBIDataWrapper get(ByteBuffer byteBuffer) throws IOException {
        RBBIDataWrapper rBBIDataWrapper = new RBBIDataWrapper();
        ICUBinary.readHeader(byteBuffer, DATA_FORMAT, IS_ACCEPTABLE);
        rBBIDataWrapper.isBigEndian = byteBuffer.order() == ByteOrder.BIG_ENDIAN;
        rBBIDataWrapper.fHeader = new RBBIDataHeader();
        rBBIDataWrapper.fHeader.fMagic = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fFormatVersion[0] = byteBuffer.get();
        rBBIDataWrapper.fHeader.fFormatVersion[1] = byteBuffer.get();
        rBBIDataWrapper.fHeader.fFormatVersion[2] = byteBuffer.get();
        rBBIDataWrapper.fHeader.fFormatVersion[3] = byteBuffer.get();
        rBBIDataWrapper.fHeader.fLength = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fCatCount = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fFTable = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fFTableLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fRTable = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fRTableLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fSFTable = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fSFTableLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fSRTable = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fSRTableLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fTrie = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fTrieLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fRuleSource = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fRuleSourceLen = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fStatusTable = byteBuffer.getInt();
        rBBIDataWrapper.fHeader.fStatusTableLen = byteBuffer.getInt();
        ICUBinary.skipBytes(byteBuffer, 24);
        if (rBBIDataWrapper.fHeader.fMagic != 45472 || !IS_ACCEPTABLE.isDataVersionAcceptable(rBBIDataWrapper.fHeader.fFormatVersion)) {
            throw new IOException("Break Iterator Rule Data Magic Number Incorrect, or unsupported data version.");
        }
        if (rBBIDataWrapper.fHeader.fFTable >= 96 && rBBIDataWrapper.fHeader.fFTable <= rBBIDataWrapper.fHeader.fLength) {
            ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fFTable - 96);
            int i = rBBIDataWrapper.fHeader.fFTable;
            rBBIDataWrapper.fFTable = ICUBinary.getShorts(byteBuffer, rBBIDataWrapper.fHeader.fFTableLen / 2, rBBIDataWrapper.fHeader.fFTableLen & 1);
            ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fRTable - (i + rBBIDataWrapper.fHeader.fFTableLen));
            int i2 = rBBIDataWrapper.fHeader.fRTable;
            rBBIDataWrapper.fRTable = ICUBinary.getShorts(byteBuffer, rBBIDataWrapper.fHeader.fRTableLen / 2, rBBIDataWrapper.fHeader.fRTableLen & 1);
            int i3 = i2 + rBBIDataWrapper.fHeader.fRTableLen;
            if (rBBIDataWrapper.fHeader.fSFTableLen > 0) {
                ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fSFTable - i3);
                int i4 = rBBIDataWrapper.fHeader.fSFTable;
                rBBIDataWrapper.fSFTable = ICUBinary.getShorts(byteBuffer, rBBIDataWrapper.fHeader.fSFTableLen / 2, rBBIDataWrapper.fHeader.fSFTableLen & 1);
                i3 = i4 + rBBIDataWrapper.fHeader.fSFTableLen;
            }
            if (rBBIDataWrapper.fHeader.fSRTableLen > 0) {
                ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fSRTable - i3);
                int i5 = rBBIDataWrapper.fHeader.fSRTable;
                rBBIDataWrapper.fSRTable = ICUBinary.getShorts(byteBuffer, rBBIDataWrapper.fHeader.fSRTableLen / 2, rBBIDataWrapper.fHeader.fSRTableLen & 1);
                i3 = i5 + rBBIDataWrapper.fHeader.fSRTableLen;
            }
            if (rBBIDataWrapper.fSRTable == null && rBBIDataWrapper.fRTable != null) {
                rBBIDataWrapper.fSRTable = rBBIDataWrapper.fRTable;
                rBBIDataWrapper.fRTable = null;
            }
            ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fTrie - i3);
            int i6 = rBBIDataWrapper.fHeader.fTrie;
            byteBuffer.mark();
            rBBIDataWrapper.fTrie = Trie2.createFromSerialized(byteBuffer);
            byteBuffer.reset();
            if (i6 > rBBIDataWrapper.fHeader.fStatusTable) {
                throw new IOException("Break iterator Rule data corrupt");
            }
            ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fStatusTable - i6);
            int i7 = rBBIDataWrapper.fHeader.fStatusTable;
            rBBIDataWrapper.fStatusTable = ICUBinary.getInts(byteBuffer, rBBIDataWrapper.fHeader.fStatusTableLen / 4, 3 & rBBIDataWrapper.fHeader.fStatusTableLen);
            int i8 = i7 + rBBIDataWrapper.fHeader.fStatusTableLen;
            if (i8 > rBBIDataWrapper.fHeader.fRuleSource) {
                throw new IOException("Break iterator Rule data corrupt");
            }
            ICUBinary.skipBytes(byteBuffer, rBBIDataWrapper.fHeader.fRuleSource - i8);
            int i9 = rBBIDataWrapper.fHeader.fRuleSource;
            rBBIDataWrapper.fRuleSource = ICUBinary.getString(byteBuffer, rBBIDataWrapper.fHeader.fRuleSourceLen / 2, rBBIDataWrapper.fHeader.fRuleSourceLen & 1);
            if (RuleBasedBreakIterator.fDebugEnv != null && RuleBasedBreakIterator.fDebugEnv.indexOf("data") >= 0) {
                rBBIDataWrapper.dump(System.out);
            }
            return rBBIDataWrapper;
        }
        throw new IOException("Break iterator Rule data corrupt");
    }

    private int getStateTableNumStates(short[] sArr) {
        if (this.isBigEndian) {
            return (sArr[1] & 65535) | (sArr[0] << 16);
        }
        return (sArr[0] & 65535) | (sArr[1] << 16);
    }

    int getStateTableFlags(short[] sArr) {
        return sArr[this.isBigEndian ? (char) 5 : (char) 4];
    }

    void dump(PrintStream printStream) {
        if (this.fFTable.length == 0) {
            throw new NullPointerException();
        }
        printStream.println("RBBI Data Wrapper dump ...");
        printStream.println();
        printStream.println("Forward State Table");
        dumpTable(printStream, this.fFTable);
        printStream.println("Reverse State Table");
        dumpTable(printStream, this.fRTable);
        printStream.println("Forward Safe Points Table");
        dumpTable(printStream, this.fSFTable);
        printStream.println("Reverse Safe Points Table");
        dumpTable(printStream, this.fSRTable);
        dumpCharCategories(printStream);
        printStream.println("Source Rules: " + this.fRuleSource);
    }

    public static String intToString(int i, int i2) {
        StringBuilder sb = new StringBuilder(i2);
        sb.append(i);
        while (sb.length() < i2) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }

    public static String intToHexString(int i, int i2) {
        StringBuilder sb = new StringBuilder(i2);
        sb.append(Integer.toHexString(i));
        while (sb.length() < i2) {
            sb.insert(0, ' ');
        }
        return sb.toString();
    }

    private void dumpTable(PrintStream printStream, short[] sArr) {
        if (sArr == null || sArr.length == 0) {
            printStream.println("  -- null -- ");
            return;
        }
        StringBuilder sb = new StringBuilder(" Row  Acc Look  Tag");
        for (int i = 0; i < this.fHeader.fCatCount; i++) {
            sb.append(intToString(i, 5));
        }
        printStream.println(sb.toString());
        for (int i2 = 0; i2 < sb.length(); i2++) {
            printStream.print(LanguageTag.SEP);
        }
        printStream.println();
        for (int i3 = 0; i3 < getStateTableNumStates(sArr); i3++) {
            dumpRow(printStream, sArr, i3);
        }
        printStream.println();
    }

    private void dumpRow(PrintStream printStream, short[] sArr, int i) {
        StringBuilder sb = new StringBuilder((this.fHeader.fCatCount * 5) + 20);
        sb.append(intToString(i, 4));
        int rowIndex = getRowIndex(i);
        int i2 = rowIndex + 0;
        if (sArr[i2] != 0) {
            sb.append(intToString(sArr[i2], 5));
        } else {
            sb.append("     ");
        }
        int i3 = rowIndex + 1;
        if (sArr[i3] != 0) {
            sb.append(intToString(sArr[i3], 5));
        } else {
            sb.append("     ");
        }
        sb.append(intToString(sArr[rowIndex + 2], 5));
        for (int i4 = 0; i4 < this.fHeader.fCatCount; i4++) {
            sb.append(intToString(sArr[rowIndex + 4 + i4], 5));
        }
        printStream.println(sb);
    }

    private void dumpCharCategories(PrintStream printStream) {
        int i = this.fHeader.fCatCount + 1;
        String[] strArr = new String[i];
        int[] iArr = new int[i];
        for (int i2 = 0; i2 <= this.fHeader.fCatCount; i2++) {
            strArr[i2] = "";
        }
        printStream.println("\nCharacter Categories");
        printStream.println("--------------------");
        int i3 = 0;
        int i4 = 0;
        int i5 = -1;
        for (int i6 = 0; i6 <= 1114111; i6++) {
            int i7 = this.fTrie.get(i6) & (-16385);
            if (i7 < 0 || i7 > this.fHeader.fCatCount) {
                printStream.println("Error, bad category " + Integer.toHexString(i7) + " for char " + Integer.toHexString(i6));
                break;
            }
            if (i7 != i5) {
                if (i5 >= 0) {
                    if (strArr[i5].length() > iArr[i5] + 70) {
                        iArr[i5] = strArr[i5].length() + 10;
                        strArr[i5] = strArr[i5] + "\n       ";
                    }
                    strArr[i5] = strArr[i5] + Padder.FALLBACK_PADDING_STRING + Integer.toHexString(i3);
                    if (i4 != i3) {
                        strArr[i5] = strArr[i5] + LanguageTag.SEP + Integer.toHexString(i4);
                    }
                }
                i3 = i6;
                i5 = i7;
            }
            i4 = i6;
        }
        strArr[i5] = strArr[i5] + Padder.FALLBACK_PADDING_STRING + Integer.toHexString(i3);
        if (i4 != i3) {
            strArr[i5] = strArr[i5] + LanguageTag.SEP + Integer.toHexString(i4);
        }
        for (int i8 = 0; i8 <= this.fHeader.fCatCount; i8++) {
            printStream.println(intToString(i8, 5) + "  " + strArr[i8]);
        }
        printStream.println();
    }
}
