package java.awt.font;

import dalvik.bytecode.Opcodes;
import dalvik.system.VMDebug;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class NumericShaper implements Serializable {
    public static final int ALL_RANGES = 524287;
    public static final int ARABIC = 2;
    public static final int BENGALI = 16;
    public static final int DEVANAGARI = 8;
    public static final int EASTERN_ARABIC = 4;
    public static final int ETHIOPIC = 65536;
    public static final int EUROPEAN = 1;
    public static final int GUJARATI = 64;
    public static final int GURMUKHI = 32;
    private static final int INDEX_ARABIC = 1;
    private static final int INDEX_BENGALI = 4;
    private static final int INDEX_DEVANAGARI = 3;
    private static final int INDEX_EASTERN_ARABIC = 2;
    private static final int INDEX_ETHIOPIC = 16;
    private static final int INDEX_EUROPEAN = 0;
    private static final int INDEX_GUJARATI = 6;
    private static final int INDEX_GURMUKHI = 5;
    private static final int INDEX_KANNADA = 10;
    private static final int INDEX_KHMER = 17;
    private static final int INDEX_LAO = 13;
    private static final int INDEX_MALAYALAM = 11;
    private static final int INDEX_MONGOLIAN = 18;
    private static final int INDEX_MYANMAR = 15;
    private static final int INDEX_ORIYA = 7;
    private static final int INDEX_TAMIL = 8;
    private static final int INDEX_TELUGU = 9;
    private static final int INDEX_THAI = 12;
    private static final int INDEX_TIBETAN = 14;
    public static final int KANNADA = 1024;
    public static final int KHMER = 131072;
    public static final int LAO = 8192;
    public static final int MALAYALAM = 2048;
    private static final int MAX_INDEX = 19;
    public static final int MONGOLIAN = 262144;
    public static final int MYANMAR = 32768;
    public static final int ORIYA = 128;
    private static final int[] STRONG_TEXT_FLAGS = {0, 0, 134217726, 134217726, 0, 69207040, -8388609, -8388609, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -65533, -1, -1, -100663297, 196611, 16415, 0, 0, 0, 67108864, -10432, -5, -32769, -4194305, -1, -1, -1, -1, -1017, -1, -32769, 67108863, 65535, -131072, -25165825, -2, Opcodes.OP_INSTANCE_OF_JUMBO, VMDebug.KIND_THREAD_EXT_FREED_OBJECTS, -65463, 2033663, -939513841, 134217726, Opcodes.OP_IGET_WIDE_JUMBO, -73728, -1, -1, 541065215, -67059616, -180225, 65535, -8192, 16383, -1, 131135, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -8, -469762049, -16703999, 537001971, -417812, -473563649, -1333765759, 133431235, -423960, -1016201729, 1577058305, 1900480, -278552, -470942209, 72193, 65475, -417812, 1676541439, -1333782143, 262083, -700594200, -1006647528, 8396230, 524224, -139282, 66059775, 30, 65475, -139284, -470811137, 1080036831, 65475, -139284, -1006633473, 8396225, 65475, -58720276, 805044223, -16547713, 1835008, -2, 917503, 268402815, 0, -17816170, 537783470, 872349791, 0, -50331649, -1050673153, -257, -2147481601, 3872, -1073741824, 237503, 0, -1, 16914171, 16777215, 0, 0, -1, -65473, 536870911, -1, -1, -2080374785, -1, -1, -249, -1, 67108863, -1, -1, 1031749119, -1, -49665, 2134769663, -8388803, -1, -12713985, -1, 134217727, 536870911, 65535, -1, -1, 2097151, -2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 8388607, 134217726, -1, -1, 131071, 253951, 6553599, 262143, 122879, -1, -1065353217, 401605055, 1023, 67043328, -1, -1, 16777215, -1, Opcodes.OP_CHECK_CAST_JUMBO, 0, 0, 536870911, 33226872, -64, 2047999, -1, -64513, 67044351, 0, -830472193, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, 0, 0, -1, -1, -1, -1, 268435455, -1, -1, 67108863, 1061158911, -1, -1426112705, 1073741823, -1, 1608515583, 265232348, 534519807, 49152, 27648, 0, -2147352576, 2031616, 0, 0, 0, 1043332228, -201605808, 992, -1, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -4194304, -1, 134217727, 2097152, 0, 0, 0, 0, 0, 0, 0, -268435456, -1, -1, 1023, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4096, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -32769, Integer.MAX_VALUE, 0, -1, -1, -1, 31, -1, -65473, -1, 32831, 8388607, 2139062143, 2139062143, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Opcodes.OP_SHL_INT_LIT8, 524157950, -2, -1, -528482305, -2, -1, -134217729, -32, -122881, -1, -1, -32769, 16777215, 0, -65536, 536870911, -1, 15, -1879048193, -1, 131071, -61441, Integer.MAX_VALUE, -1, -1, -1, -125829121, -1, -1, 1073741823, Integer.MAX_VALUE, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2097152, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 134217728, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, Opcodes.OP_SPUT_BYTE_JUMBO, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -2117, Opcodes.OP_REM_LONG, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 1, 0, 0, Integer.MIN_VALUE, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Integer.MIN_VALUE, -1, -1, -1, -1, -1, -1, -1, -1, 
    -1, -49153, -1, -63489, -1, -1, 67108863, 0, -1594359681, 1602223615, -37, -1, -1, 262143, -524288, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1073741823, -65536, -1, -196609, -1, Opcodes.OP_CONST_CLASS_JUMBO, 536805376, 0, 0, 0, -2162688, -1, -1, -1, 536870911, 0, 134217726, 134217726, -64, -1, Integer.MAX_VALUE, 486341884, 0};
    public static final int TAMIL = 256;
    public static final int TELUGU = 512;
    public static final int THAI = 4096;
    public static final int TIBETAN = 16384;
    private static final long serialVersionUID = -8022764705923730308L;
    private boolean fContextual;
    private int fDefaultContextIndex;
    private int fRanges;
    private int fSingleRangeIndex;
    private int key;
    private int mask;
    private final int[] scriptsRanges = {0, 591, 1536, Opcodes.OP_IGET_JUMBO, 1536, Opcodes.OP_IGET_JUMBO, 2304, 2431, 2432, Opcodes.OP_IGET_BOOLEAN_JUMBO, 2560, 2687, 2688, Opcodes.OP_IGET_BYTE_JUMBO, 2816, 2943, 2944, Opcodes.OP_IGET_CHAR_JUMBO, 3072, 3199, 3200, Opcodes.OP_IGET_SHORT_JUMBO, 3328, 3455, 3584, 3711, 3712, Opcodes.OP_IPUT_WIDE_JUMBO, 3840, Opcodes.OP_IPUT_OBJECT_JUMBO, 4096, 4255, 4608, 4991, 6016, Opcodes.OP_SGET_BOOLEAN_JUMBO, 6144, 6319};
    private final int[] digitsLowRanges = {0, 1584, 1584, 2358, 2486, 2614, 2742, 2870, 2998, 3126, 3254, 3382, 3616, 3744, 3824, 4112, 4920, 6064, 6112};
    private final String[] contexts = {"EUROPEAN", "ARABIC", "EASTERN_ARABIC", "DEVANAGARI", "BENGALI", "GURMUKHI", "GUJARATI", "ORIYA", "TAMIL", "TELUGU", "KANNADA", "MALAYALAM", "THAI", "LAO", "TIBETAN", "MYANMAR", "ETHIOPIC", "KHMER", "MONGOLIAN"};

    private NumericShaper(int ranges, int defaultContext, boolean isContextual) {
        this.fRanges = ranges;
        this.fDefaultContextIndex = getIndexFromRange(defaultContext);
        this.fContextual = isContextual;
        if (!this.fContextual) {
            this.fSingleRangeIndex = getIndexFromRange(ranges);
        }
    }

    private int getIndexFromRange(int range) {
        if (range == 0) {
            throw rangeException(range);
        }
        for (int index = 0; index < 19; index++) {
            if (range == (1 << index)) {
                return index;
            }
        }
        throw rangeException(range);
    }

    private int getRangeFromIndex(int index) {
        if (index < 0 || index >= 19) {
            throw rangeException(index);
        }
        return 1 << index;
    }

    private static IllegalArgumentException rangeException(int value) {
        throw new IllegalArgumentException("Illegal range argument value: " + value);
    }

    public int hashCode() {
        int result = this.fRanges + 527;
        return (((result * 31) + this.fDefaultContextIndex) * 31) + (this.fContextual ? 1 : 0);
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        try {
            NumericShaper ns = (NumericShaper) obj;
            if (this.fRanges != ns.fRanges || this.fDefaultContextIndex != ns.fDefaultContextIndex) {
                z = false;
            } else if (this.fContextual != ns.fContextual) {
            }
            return z;
        } catch (ClassCastException e) {
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("[contextual:");
        sb.append(this.fContextual);
        if (this.fContextual) {
            sb.append(", context:");
            sb.append(this.contexts[this.fDefaultContextIndex]);
        }
        sb.append(", range(s): ");
        if (this.fContextual) {
            boolean isFirst = true;
            for (int index = 0; index < 19; index++) {
                if ((this.fRanges & (1 << index)) != 0) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append(this.contexts[index]);
                }
            }
        } else {
            sb.append(this.contexts[this.fSingleRangeIndex]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static NumericShaper getContextualShaper(int ranges, int defaultContext) {
        return new NumericShaper(ranges & ALL_RANGES, defaultContext & ALL_RANGES, true);
    }

    public static NumericShaper getContextualShaper(int ranges) {
        return new NumericShaper(ranges & ALL_RANGES, 1, true);
    }

    public int getRanges() {
        return this.fRanges;
    }

    public static NumericShaper getShaper(int singleRange) {
        return new NumericShaper(singleRange & ALL_RANGES, 1, false);
    }

    public boolean isContextual() {
        return this.fContextual;
    }

    public void shape(char[] text, int start, int count, int context) {
        if (isContextual()) {
            contextualShape(text, start, count, getIndexFromRange(context));
        } else {
            nonContextualShape(text, start, count);
        }
    }

    public void shape(char[] text, int start, int count) {
        if (isContextual()) {
            contextualShape(text, start, count, this.fDefaultContextIndex);
        } else {
            nonContextualShape(text, start, count);
        }
    }

    private void contextualShape(char[] text, int start, int count, int contextIndex) {
        int currIndex;
        int index;
        if (((1 << contextIndex) & this.fRanges) == 0) {
            currIndex = 0;
        } else {
            currIndex = contextIndex;
        }
        for (int ind = start; ind < start + count; ind++) {
            if ('0' <= text[ind] && text[ind] <= '9') {
                if (currIndex != 16 || text[ind] != '0') {
                    text[ind] = (char) (this.digitsLowRanges[currIndex] + text[ind]);
                }
            } else if (isCharStrong(text[ind]) && currIndex != (index = getCharIndex(text[ind]))) {
                if (((1 << index) & this.fRanges) != 0) {
                    currIndex = index;
                } else {
                    currIndex = 0;
                }
            }
        }
    }

    private void nonContextualShape(char[] text, int start, int count) {
        char minDigit = (char) (this.fRanges == 65536 ? 49 : 48);
        for (int ind = start; ind < start + count; ind++) {
            if (minDigit <= text[ind] && text[ind] <= '9') {
                text[ind] = (char) (this.digitsLowRanges[this.fSingleRangeIndex] + text[ind]);
            }
        }
    }

    private int getCharIndex(char ch) {
        for (int i = 0; i < 19; i++) {
            int j = i * 2;
            if (this.scriptsRanges[j] <= ch && ch <= this.scriptsRanges[j + 1]) {
                return i;
            }
        }
        return 0;
    }

    private boolean isCharStrong(int chr) {
        return (STRONG_TEXT_FLAGS[chr >> 5] & (1 << (chr % 32))) != 0;
    }

    private void updateRangesFields() {
        this.fRanges = this.mask & Integer.MAX_VALUE;
        this.fContextual = (this.mask & Integer.MIN_VALUE) != 0;
        if (this.fContextual) {
            this.fRanges = this.mask & Integer.MAX_VALUE;
            this.fDefaultContextIndex = this.key;
        } else {
            this.fRanges = this.mask;
            this.fSingleRangeIndex = this.key;
        }
    }

    private void updateKeyMaskFields() {
        this.mask = this.fRanges;
        if (this.fContextual) {
            this.mask |= Integer.MIN_VALUE;
            this.key = this.fDefaultContextIndex;
        } else {
            this.key = this.fSingleRangeIndex;
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        updateKeyMaskFields();
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        updateRangesFields();
    }
}
