package android.icu.text;

import android.icu.impl.CharTrie;
import android.icu.impl.ICUBinary;
import android.icu.impl.Trie;
import android.icu.impl.locale.LanguageTag;
import java.io.IOException;
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
    static final int FORMAT_VERSION = 50397184;
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
    CharTrie fTrie;
    private boolean isBigEndian;
    private static final IsAcceptable IS_ACCEPTABLE = new IsAcceptable(null);
    static TrieFoldingFunc fTrieFoldingFunc = new TrieFoldingFunc();

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
        int fVersion;
        int fMagic = 0;
        byte[] fFormatVersion = new byte[4];
    }

    private static final class IsAcceptable implements ICUBinary.Authenticate {
        IsAcceptable(IsAcceptable isAcceptable) {
            this();
        }

        private IsAcceptable() {
        }

        @Override
        public boolean isDataVersionAcceptable(byte[] version) {
            return version[0] == 3;
        }
    }

    int getRowIndex(int state) {
        return ((this.fHeader.fCatCount + 4) * state) + 8;
    }

    static class TrieFoldingFunc implements Trie.DataManipulate {
        TrieFoldingFunc() {
        }

        @Override
        public int getFoldingOffset(int data) {
            if ((32768 & data) != 0) {
                return data & 32767;
            }
            return 0;
        }
    }

    RBBIDataWrapper() {
    }

    static RBBIDataWrapper get(ByteBuffer bytes) throws IOException {
        RBBIDataWrapper This = new RBBIDataWrapper();
        ICUBinary.readHeader(bytes, DATA_FORMAT, IS_ACCEPTABLE);
        This.isBigEndian = bytes.order() == ByteOrder.BIG_ENDIAN;
        This.fHeader = new RBBIDataHeader();
        This.fHeader.fMagic = bytes.getInt();
        This.fHeader.fVersion = bytes.getInt(bytes.position());
        This.fHeader.fFormatVersion[0] = bytes.get();
        This.fHeader.fFormatVersion[1] = bytes.get();
        This.fHeader.fFormatVersion[2] = bytes.get();
        This.fHeader.fFormatVersion[3] = bytes.get();
        This.fHeader.fLength = bytes.getInt();
        This.fHeader.fCatCount = bytes.getInt();
        This.fHeader.fFTable = bytes.getInt();
        This.fHeader.fFTableLen = bytes.getInt();
        This.fHeader.fRTable = bytes.getInt();
        This.fHeader.fRTableLen = bytes.getInt();
        This.fHeader.fSFTable = bytes.getInt();
        This.fHeader.fSFTableLen = bytes.getInt();
        This.fHeader.fSRTable = bytes.getInt();
        This.fHeader.fSRTableLen = bytes.getInt();
        This.fHeader.fTrie = bytes.getInt();
        This.fHeader.fTrieLen = bytes.getInt();
        This.fHeader.fRuleSource = bytes.getInt();
        This.fHeader.fRuleSourceLen = bytes.getInt();
        This.fHeader.fStatusTable = bytes.getInt();
        This.fHeader.fStatusTableLen = bytes.getInt();
        ICUBinary.skipBytes(bytes, 24);
        if (This.fHeader.fMagic != 45472 || (This.fHeader.fVersion != 1 && This.fHeader.fFormatVersion[0] != 3)) {
            throw new IOException("Break Iterator Rule Data Magic Number Incorrect, or unsupported data version.");
        }
        if (This.fHeader.fFTable < 96 || This.fHeader.fFTable > This.fHeader.fLength) {
            throw new IOException("Break iterator Rule data corrupt");
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fFTable - 96);
        int pos = This.fHeader.fFTable;
        This.fFTable = ICUBinary.getShorts(bytes, This.fHeader.fFTableLen / 2, This.fHeader.fFTableLen & 1);
        ICUBinary.skipBytes(bytes, This.fHeader.fRTable - (pos + This.fHeader.fFTableLen));
        int pos2 = This.fHeader.fRTable;
        This.fRTable = ICUBinary.getShorts(bytes, This.fHeader.fRTableLen / 2, This.fHeader.fRTableLen & 1);
        int pos3 = pos2 + This.fHeader.fRTableLen;
        if (This.fHeader.fSFTableLen > 0) {
            ICUBinary.skipBytes(bytes, This.fHeader.fSFTable - pos3);
            int pos4 = This.fHeader.fSFTable;
            This.fSFTable = ICUBinary.getShorts(bytes, This.fHeader.fSFTableLen / 2, This.fHeader.fSFTableLen & 1);
            pos3 = pos4 + This.fHeader.fSFTableLen;
        }
        if (This.fHeader.fSRTableLen > 0) {
            ICUBinary.skipBytes(bytes, This.fHeader.fSRTable - pos3);
            int pos5 = This.fHeader.fSRTable;
            This.fSRTable = ICUBinary.getShorts(bytes, This.fHeader.fSRTableLen / 2, This.fHeader.fSRTableLen & 1);
            pos3 = pos5 + This.fHeader.fSRTableLen;
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fTrie - pos3);
        int pos6 = This.fHeader.fTrie;
        bytes.mark();
        This.fTrie = new CharTrie(bytes, fTrieFoldingFunc);
        bytes.reset();
        if (pos6 > This.fHeader.fStatusTable) {
            throw new IOException("Break iterator Rule data corrupt");
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fStatusTable - pos6);
        int pos7 = This.fHeader.fStatusTable;
        This.fStatusTable = ICUBinary.getInts(bytes, This.fHeader.fStatusTableLen / 4, This.fHeader.fStatusTableLen & 3);
        int pos8 = pos7 + This.fHeader.fStatusTableLen;
        if (pos8 > This.fHeader.fRuleSource) {
            throw new IOException("Break iterator Rule data corrupt");
        }
        ICUBinary.skipBytes(bytes, This.fHeader.fRuleSource - pos8);
        int pos9 = This.fHeader.fRuleSource;
        This.fRuleSource = ICUBinary.getString(bytes, This.fHeader.fRuleSourceLen / 2, This.fHeader.fRuleSourceLen & 1);
        if (RuleBasedBreakIterator.fDebugEnv != null && RuleBasedBreakIterator.fDebugEnv.indexOf("data") >= 0) {
            This.dump();
        }
        return This;
    }

    private int getStateTableNumStates(short[] table) {
        if (this.isBigEndian) {
            return (table[0] << 16) | (table[1] & 65535);
        }
        return (table[1] << 16) | (table[0] & 65535);
    }

    int getStateTableFlags(short[] table) {
        return table[this.isBigEndian ? (char) 5 : (char) 4];
    }

    void dump() {
        if (this.fFTable.length == 0) {
            throw new NullPointerException();
        }
        System.out.println("RBBI Data Wrapper dump ...");
        System.out.println();
        System.out.println("Forward State Table");
        dumpTable(this.fFTable);
        System.out.println("Reverse State Table");
        dumpTable(this.fRTable);
        System.out.println("Forward Safe Points Table");
        dumpTable(this.fSFTable);
        System.out.println("Reverse Safe Points Table");
        dumpTable(this.fSRTable);
        dumpCharCategories();
        System.out.println("Source Rules: " + this.fRuleSource);
    }

    public static String intToString(int n, int width) {
        StringBuilder dest = new StringBuilder(width);
        dest.append(n);
        while (dest.length() < width) {
            dest.insert(0, ' ');
        }
        return dest.toString();
    }

    public static String intToHexString(int n, int width) {
        StringBuilder dest = new StringBuilder(width);
        dest.append(Integer.toHexString(n));
        while (dest.length() < width) {
            dest.insert(0, ' ');
        }
        return dest.toString();
    }

    private void dumpTable(short[] table) {
        if (table == null) {
            System.out.println("  -- null -- ");
            return;
        }
        StringBuilder header = new StringBuilder(" Row  Acc Look  Tag");
        for (int n = 0; n < this.fHeader.fCatCount; n++) {
            header.append(intToString(n, 5));
        }
        System.out.println(header.toString());
        for (int n2 = 0; n2 < header.length(); n2++) {
            System.out.print(LanguageTag.SEP);
        }
        System.out.println();
        for (int state = 0; state < getStateTableNumStates(table); state++) {
            dumpRow(table, state);
        }
        System.out.println();
    }

    private void dumpRow(short[] table, int state) {
        StringBuilder dest = new StringBuilder((this.fHeader.fCatCount * 5) + 20);
        dest.append(intToString(state, 4));
        int row = getRowIndex(state);
        if (table[row + 0] != 0) {
            dest.append(intToString(table[row + 0], 5));
        } else {
            dest.append("     ");
        }
        if (table[row + 1] != 0) {
            dest.append(intToString(table[row + 1], 5));
        } else {
            dest.append("     ");
        }
        dest.append(intToString(table[row + 2], 5));
        for (int col = 0; col < this.fHeader.fCatCount; col++) {
            dest.append(intToString(table[row + 4 + col], 5));
        }
        System.out.println(dest);
    }

    private void dumpCharCategories() {
        int n = this.fHeader.fCatCount;
        String[] catStrings = new String[n + 1];
        int rangeStart = 0;
        int rangeEnd = 0;
        int lastCat = -1;
        int[] lastNewline = new int[n + 1];
        for (int category = 0; category <= this.fHeader.fCatCount; category++) {
            catStrings[category] = "";
        }
        System.out.println("\nCharacter Categories");
        System.out.println("--------------------");
        for (int char32 = 0; char32 <= 1114111; char32++) {
            int category2 = this.fTrie.getCodePointValue(char32);
            int category3 = category2 & (-16385);
            if (category3 < 0 || category3 > this.fHeader.fCatCount) {
                System.out.println("Error, bad category " + Integer.toHexString(category3) + " for char " + Integer.toHexString(char32));
                break;
            }
            if (category3 == lastCat) {
                rangeEnd = char32;
            } else {
                if (lastCat >= 0) {
                    if (catStrings[lastCat].length() > lastNewline[lastCat] + 70) {
                        lastNewline[lastCat] = catStrings[lastCat].length() + 10;
                        catStrings[lastCat] = catStrings[lastCat] + "\n       ";
                    }
                    catStrings[lastCat] = catStrings[lastCat] + " " + Integer.toHexString(rangeStart);
                    if (rangeEnd != rangeStart) {
                        catStrings[lastCat] = catStrings[lastCat] + LanguageTag.SEP + Integer.toHexString(rangeEnd);
                    }
                }
                lastCat = category3;
                rangeEnd = char32;
                rangeStart = char32;
            }
        }
        catStrings[lastCat] = catStrings[lastCat] + " " + Integer.toHexString(rangeStart);
        if (rangeEnd != rangeStart) {
            catStrings[lastCat] = catStrings[lastCat] + LanguageTag.SEP + Integer.toHexString(rangeEnd);
        }
        for (int category4 = 0; category4 <= this.fHeader.fCatCount; category4++) {
            System.out.println(intToString(category4, 5) + "  " + catStrings[category4]);
        }
        System.out.println();
    }
}
