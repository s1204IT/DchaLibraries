package android.icu.text;

import android.icu.impl.UBiDiProps;
import android.icu.lang.UCharacter;
import java.awt.font.NumericShaper;
import java.awt.font.TextAttribute;
import java.lang.reflect.Array;
import java.text.AttributedCharacterIterator;
import java.util.Arrays;

public class Bidi {
    static final byte AL = 13;
    static final byte AN = 5;
    static final byte B = 7;
    static final byte BN = 18;
    public static final int CLASS_DEFAULT = 23;
    private static final char CR = '\r';
    static final byte CS = 6;
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = 126;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = 127;
    public static final int DIRECTION_LEFT_TO_RIGHT = 0;
    public static final int DIRECTION_RIGHT_TO_LEFT = 1;
    public static final short DO_MIRRORING = 2;
    static final byte EN = 2;
    static final byte ENL = 23;
    static final byte ENR = 24;
    static final byte ES = 3;
    static final byte ET = 4;
    static final int FIRSTALLOC = 10;
    static final byte FSI = 19;
    private static final int IMPTABLEVELS_COLUMNS = 8;
    private static final int IMPTABLEVELS_RES = 7;
    private static final int IMPTABPROPS_COLUMNS = 16;
    private static final int IMPTABPROPS_RES = 15;
    public static final short INSERT_LRM_FOR_NUMERIC = 4;
    static final int ISOLATE = 256;
    public static final short KEEP_BASE_COMBINING = 1;
    static final byte L = 0;
    public static final byte LEVEL_DEFAULT_LTR = 126;
    public static final byte LEVEL_DEFAULT_RTL = 127;
    public static final byte LEVEL_OVERRIDE = -128;
    private static final char LF = '\n';
    static final int LOOKING_FOR_PDI = 3;
    static final byte LRE = 11;
    static final byte LRI = 20;
    static final int LRM_AFTER = 2;
    static final int LRM_BEFORE = 1;
    static final byte LRO = 12;
    public static final byte LTR = 0;
    public static final int MAP_NOWHERE = -1;
    public static final byte MAX_EXPLICIT_LEVEL = 125;
    public static final byte MIXED = 2;
    public static final byte NEUTRAL = 3;
    static final int NOT_SEEKING_STRONG = 0;
    static final byte NSM = 17;
    static final byte ON = 10;
    public static final int OPTION_DEFAULT = 0;
    public static final int OPTION_INSERT_MARKS = 1;
    public static final int OPTION_REMOVE_CONTROLS = 2;
    public static final int OPTION_STREAMING = 4;
    public static final short OUTPUT_REVERSE = 16;
    static final byte PDF = 16;
    static final byte PDI = 22;
    static final byte R = 1;
    public static final short REMOVE_BIDI_CONTROLS = 8;
    static final short REORDER_COUNT = 7;
    public static final short REORDER_DEFAULT = 0;
    public static final short REORDER_GROUP_NUMBERS_WITH_R = 2;
    public static final short REORDER_INVERSE_FOR_NUMBERS_SPECIAL = 6;
    public static final short REORDER_INVERSE_LIKE_DIRECT = 5;
    public static final short REORDER_INVERSE_NUMBERS_AS_L = 4;
    static final short REORDER_LAST_LOGICAL_TO_VISUAL = 1;
    public static final short REORDER_NUMBERS_SPECIAL = 1;
    public static final short REORDER_RUNS_ONLY = 3;
    static final byte RLE = 14;
    static final byte RLI = 21;
    static final int RLM_AFTER = 8;
    static final int RLM_BEFORE = 4;
    static final byte RLO = 15;
    public static final byte RTL = 1;
    static final byte S = 8;
    static final int SEEKING_STRONG_FOR_FSI = 2;
    static final int SEEKING_STRONG_FOR_PARA = 1;
    static final int SIMPLE_OPENINGS_COUNT = 20;
    static final int SIMPLE_PARAS_COUNT = 10;
    static final byte WS = 9;
    private static final short _AN = 3;
    private static final short _B = 6;
    private static final short _EN = 2;
    private static final short _L = 0;
    private static final short _ON = 4;
    private static final short _R = 1;
    private static final short _S = 5;
    final UBiDiProps bdp;
    int controlCount;
    BidiClassifier customClassifier;
    byte defaultParaLevel;
    byte[] dirProps;
    byte[] dirPropsMemory;
    byte direction;
    String epilogue;
    int flags;
    ImpTabPair impTabPair;
    InsertPoints insertPoints;
    boolean isGoodLogicalToVisualRunsMap;
    boolean isInverse;
    int isolateCount;
    Isolate[] isolates;
    int lastArabicPos;
    int length;
    byte[] levels;
    byte[] levelsMemory;
    int[] logicalToVisualRunsMap;
    boolean mayAllocateRuns;
    boolean mayAllocateText;
    boolean orderParagraphsLTR;
    int originalLength;
    Bidi paraBidi;
    int paraCount;
    byte paraLevel;
    byte[] paras_level;
    int[] paras_limit;
    String prologue;
    int reorderingMode;
    int reorderingOptions;
    int resultLength;
    int runCount;
    BidiRun[] runs;
    BidiRun[] runsMemory;
    BidiRun[] simpleRuns;
    char[] text;
    int trailingWSStart;
    static final byte FOUND_L = (byte) DirPropFlag((byte) 0);
    static final byte FOUND_R = (byte) DirPropFlag((byte) 1);
    static final int DirPropFlagMultiRuns = DirPropFlag((byte) 31);
    static final int[] DirPropFlagLR = {DirPropFlag((byte) 0), DirPropFlag((byte) 1)};
    static final int[] DirPropFlagE = {DirPropFlag((byte) 11), DirPropFlag((byte) 14)};
    static final int[] DirPropFlagO = {DirPropFlag((byte) 12), DirPropFlag((byte) 15)};
    static final int MASK_LTR = ((((((DirPropFlag((byte) 0) | DirPropFlag((byte) 2)) | DirPropFlag((byte) 23)) | DirPropFlag((byte) 24)) | DirPropFlag((byte) 5)) | DirPropFlag((byte) 11)) | DirPropFlag((byte) 12)) | DirPropFlag((byte) 20);
    static final int MASK_RTL = (((DirPropFlag((byte) 1) | DirPropFlag((byte) 13)) | DirPropFlag((byte) 14)) | DirPropFlag((byte) 15)) | DirPropFlag((byte) 21);
    static final int MASK_R_AL = DirPropFlag((byte) 1) | DirPropFlag((byte) 13);
    static final int MASK_STRONG_EN_AN = (((DirPropFlag((byte) 0) | DirPropFlag((byte) 1)) | DirPropFlag((byte) 13)) | DirPropFlag((byte) 2)) | DirPropFlag((byte) 5);
    static final int MASK_EXPLICIT = (((DirPropFlag((byte) 11) | DirPropFlag((byte) 12)) | DirPropFlag((byte) 14)) | DirPropFlag((byte) 15)) | DirPropFlag((byte) 16);
    static final int MASK_BN_EXPLICIT = DirPropFlag((byte) 18) | MASK_EXPLICIT;
    static final int MASK_ISO = ((DirPropFlag((byte) 20) | DirPropFlag((byte) 21)) | DirPropFlag((byte) 19)) | DirPropFlag((byte) 22);
    static final int MASK_B_S = DirPropFlag((byte) 7) | DirPropFlag((byte) 8);
    static final int MASK_WS = ((MASK_B_S | DirPropFlag((byte) 9)) | MASK_BN_EXPLICIT) | MASK_ISO;
    static final int MASK_POSSIBLE_N = (((DirPropFlag((byte) 10) | DirPropFlag((byte) 6)) | DirPropFlag((byte) 3)) | DirPropFlag((byte) 4)) | MASK_WS;
    static final int MASK_EMBEDDING = DirPropFlag((byte) 17) | MASK_POSSIBLE_N;
    private static final short[] groupProp = {0, 1, 2, 7, 8, 3, 9, 6, 5, 4, 4, 10, 10, 12, 10, 10, 10, 11, 10, 4, 4, 4, 4, 13, 14};
    private static final short[][] impTabProps = {new short[]{1, 2, 4, 5, 7, 15, 17, 7, 9, 7, 0, 7, 3, 18, 21, 4}, new short[]{1, 34, 36, 37, 39, 47, 49, 39, 41, 39, 1, 1, 35, 50, 53, 0}, new short[]{33, 2, 36, 37, 39, 47, 49, 39, 41, 39, 2, 2, 35, 50, 53, 1}, new short[]{33, 34, 38, 38, 40, 48, 49, 40, 40, 40, 3, 3, 3, 50, 53, 1}, new short[]{33, 34, 4, 37, 39, 47, 49, 74, 11, 74, 4, 4, 35, 18, 21, 2}, new short[]{33, 34, 36, 5, 39, 47, 49, 39, 41, 76, 5, 5, 35, 50, 53, 3}, new short[]{33, 34, 6, 6, 40, 48, 49, 40, 40, 77, 6, 6, 35, 18, 21, 3}, new short[]{33, 34, 36, 37, 7, 47, 49, 7, 78, 7, 7, 7, 35, 50, 53, 4}, new short[]{33, 34, 38, 38, 8, 48, 49, 8, 8, 8, 8, 8, 35, 50, 53, 4}, new short[]{33, 34, 4, 37, 7, 47, 49, 7, 9, 7, 9, 9, 35, 18, 21, 4}, new short[]{97, 98, 4, 101, 135, 111, 113, 135, 142, 135, 10, 135, 99, 18, 21, 2}, new short[]{33, 34, 4, 37, 39, 47, 49, 39, 11, 39, 11, 11, 35, 18, 21, 2}, new short[]{97, 98, 100, 5, 135, 111, 113, 135, 142, 135, 12, 135, 99, 114, 117, 3}, new short[]{97, 98, 6, 6, 136, 112, 113, 136, 136, 136, 13, 136, 99, 18, 21, 3}, new short[]{33, 34, 132, 37, 7, 47, 49, 7, 14, 7, 14, 14, 35, 146, 149, 4}, new short[]{33, 34, 36, 37, 39, 15, 49, 39, 41, 39, 15, 39, 35, 50, 53, 5}, new short[]{33, 34, 38, 38, 40, 16, 49, 40, 40, 40, 16, 40, 35, 50, 53, 5}, new short[]{33, 34, 36, 37, 39, 47, 17, 39, 41, 39, 17, 39, 35, 50, 53, 6}, new short[]{33, 34, 18, 37, 39, 47, 49, 83, 20, 83, 18, 18, 35, 18, 21, 0}, new short[]{97, 98, 18, 101, 135, 111, 113, 135, 142, 135, 19, 135, 99, 18, 21, 0}, new short[]{33, 34, 18, 37, 39, 47, 49, 39, 20, 39, 20, 20, 35, 18, 21, 0}, new short[]{33, 34, 21, 37, 39, 47, 49, 86, 23, 86, 21, 21, 35, 18, 21, 3}, new short[]{97, 98, 21, 101, 135, 111, 113, 135, 142, 135, 22, 135, 99, 18, 21, 3}, new short[]{33, 34, 21, 37, 39, 47, 49, 39, 23, 39, 23, 23, 35, 18, 21, 3}};
    private static final byte[][] impTabL_DEFAULT = {new byte[]{0, 1, 0, 2, 0, 0, 0, 0}, new byte[]{0, 1, 3, 3, 20, 20, 0, 1}, new byte[]{0, 1, 0, 2, 21, 21, 0, 2}, new byte[]{0, 1, 3, 3, 20, 20, 0, 2}, new byte[]{0, 33, 51, 51, 4, 4, 0, 0}, new byte[]{0, 33, 0, 50, 5, 5, 0, 0}};
    private static final byte[][] impTabR_DEFAULT = {new byte[]{1, 0, 2, 2, 0, 0, 0, 0}, new byte[]{1, 0, 1, 3, 20, 20, 0, 1}, new byte[]{1, 0, 2, 2, 0, 0, 0, 1}, new byte[]{1, 0, 1, 3, 5, 5, 0, 1}, new byte[]{33, 0, 33, 3, 4, 4, 0, 0}, new byte[]{1, 0, 1, 3, 5, 5, 0, 0}};
    private static final short[] impAct0 = {0, 1, 2, 3, 4};
    private static final ImpTabPair impTab_DEFAULT = new ImpTabPair(impTabL_DEFAULT, impTabR_DEFAULT, impAct0, impAct0);
    private static final byte[][] impTabL_NUMBERS_SPECIAL = {new byte[]{0, 2, 17, 17, 0, 0, 0, 0}, new byte[]{0, 66, 1, 1, 0, 0, 0, 0}, new byte[]{0, 2, 4, 4, 19, 19, 0, 1}, new byte[]{0, 34, 52, 52, 3, 3, 0, 0}, new byte[]{0, 2, 4, 4, 19, 19, 0, 2}};
    private static final ImpTabPair impTab_NUMBERS_SPECIAL = new ImpTabPair(impTabL_NUMBERS_SPECIAL, impTabR_DEFAULT, impAct0, impAct0);
    private static final byte[][] impTabL_GROUP_NUMBERS_WITH_R = {new byte[]{0, 3, 17, 17, 0, 0, 0, 0}, new byte[]{32, 3, 1, 1, 2, 32, 32, 2}, new byte[]{32, 3, 1, 1, 2, 32, 32, 1}, new byte[]{0, 3, 5, 5, 20, 0, 0, 1}, new byte[]{32, 3, 5, 5, 4, 32, 32, 1}, new byte[]{0, 3, 5, 5, 20, 0, 0, 2}};
    private static final byte[][] impTabR_GROUP_NUMBERS_WITH_R = {new byte[]{2, 0, 1, 1, 0, 0, 0, 0}, new byte[]{2, 0, 1, 1, 0, 0, 0, 1}, new byte[]{2, 0, 20, 20, 19, 0, 0, 1}, new byte[]{34, 0, 4, 4, 3, 0, 0, 0}, new byte[]{34, 0, 4, 4, 3, 0, 0, 1}};
    private static final ImpTabPair impTab_GROUP_NUMBERS_WITH_R = new ImpTabPair(impTabL_GROUP_NUMBERS_WITH_R, impTabR_GROUP_NUMBERS_WITH_R, impAct0, impAct0);
    private static final byte[][] impTabL_INVERSE_NUMBERS_AS_L = {new byte[]{0, 1, 0, 0, 0, 0, 0, 0}, new byte[]{0, 1, 0, 0, 20, 20, 0, 1}, new byte[]{0, 1, 0, 0, 21, 21, 0, 2}, new byte[]{0, 1, 0, 0, 20, 20, 0, 2}, new byte[]{32, 1, 32, 32, 4, 4, 32, 1}, new byte[]{32, 1, 32, 32, 5, 5, 32, 1}};
    private static final byte[][] impTabR_INVERSE_NUMBERS_AS_L = {new byte[]{1, 0, 1, 1, 0, 0, 0, 0}, new byte[]{1, 0, 1, 1, 20, 20, 0, 1}, new byte[]{1, 0, 1, 1, 0, 0, 0, 1}, new byte[]{1, 0, 1, 1, 5, 5, 0, 1}, new byte[]{33, 0, 33, 33, 4, 4, 0, 0}, new byte[]{1, 0, 1, 1, 5, 5, 0, 0}};
    private static final ImpTabPair impTab_INVERSE_NUMBERS_AS_L = new ImpTabPair(impTabL_INVERSE_NUMBERS_AS_L, impTabR_INVERSE_NUMBERS_AS_L, impAct0, impAct0);
    private static final byte[][] impTabR_INVERSE_LIKE_DIRECT = {new byte[]{1, 0, 2, 2, 0, 0, 0, 0}, new byte[]{1, 0, 1, 2, 19, 19, 0, 1}, new byte[]{1, 0, 2, 2, 0, 0, 0, 1}, new byte[]{33, 48, 6, 4, 3, 3, 48, 0}, new byte[]{33, 48, 6, 4, 5, 5, 48, 3}, new byte[]{33, 48, 6, 4, 5, 5, 48, 2}, new byte[]{33, 48, 6, 4, 3, 3, 48, 1}};
    private static final short[] impAct1 = {0, 1, 13, 14};
    private static final ImpTabPair impTab_INVERSE_LIKE_DIRECT = new ImpTabPair(impTabL_DEFAULT, impTabR_INVERSE_LIKE_DIRECT, impAct0, impAct1);
    private static final byte[][] impTabL_INVERSE_LIKE_DIRECT_WITH_MARKS = {new byte[]{0, 99, 0, 1, 0, 0, 0, 0}, new byte[]{0, 99, 0, 1, 18, 48, 0, 4}, new byte[]{32, 99, 32, 1, 2, 48, 32, 3}, new byte[]{0, 99, 85, 86, 20, 48, 0, 3}, new byte[]{48, 67, 85, 86, 4, 48, 48, 3}, new byte[]{48, 67, 5, 86, 20, 48, 48, 4}, new byte[]{48, 67, 85, 6, 20, 48, 48, 4}};
    private static final byte[][] impTabR_INVERSE_LIKE_DIRECT_WITH_MARKS = {new byte[]{19, 0, 1, 1, 0, 0, 0, 0}, new byte[]{35, 0, 1, 1, 2, 64, 0, 1}, new byte[]{35, 0, 1, 1, 2, 64, 0, 0}, new byte[]{3, 0, 3, 54, 20, 64, 0, 1}, new byte[]{83, 64, 5, 54, 4, 64, 64, 0}, new byte[]{83, 64, 5, 54, 4, 64, 64, 1}, new byte[]{83, 64, 6, 6, 4, 64, 64, 3}};
    private static final short[] impAct2 = {0, 1, 2, 5, 6, 7, 8};
    private static final short[] impAct3 = {0, 1, 9, 10, 11, 12};
    private static final ImpTabPair impTab_INVERSE_LIKE_DIRECT_WITH_MARKS = new ImpTabPair(impTabL_INVERSE_LIKE_DIRECT_WITH_MARKS, impTabR_INVERSE_LIKE_DIRECT_WITH_MARKS, impAct2, impAct3);
    private static final ImpTabPair impTab_INVERSE_FOR_NUMBERS_SPECIAL = new ImpTabPair(impTabL_NUMBERS_SPECIAL, impTabR_INVERSE_LIKE_DIRECT, impAct0, impAct1);
    private static final byte[][] impTabL_INVERSE_FOR_NUMBERS_SPECIAL_WITH_MARKS = {new byte[]{0, 98, 1, 1, 0, 0, 0, 0}, new byte[]{0, 98, 1, 1, 0, 48, 0, 4}, new byte[]{0, 98, 84, 84, 19, 48, 0, 3}, new byte[]{48, 66, 84, 84, 3, 48, 48, 3}, new byte[]{48, 66, 4, 4, 19, 48, 48, 4}};
    private static final ImpTabPair impTab_INVERSE_FOR_NUMBERS_SPECIAL_WITH_MARKS = new ImpTabPair(impTabL_INVERSE_FOR_NUMBERS_SPECIAL_WITH_MARKS, impTabR_INVERSE_LIKE_DIRECT_WITH_MARKS, impAct2, impAct3);

    static class Point {
        int flag;
        int pos;

        Point() {
        }
    }

    static class InsertPoints {
        int confirmed;
        Point[] points = new Point[0];
        int size;

        InsertPoints() {
        }
    }

    static class Opening {
        byte contextDir;
        int contextPos;
        byte filler;
        short flags;
        int match;
        int position;

        Opening() {
        }
    }

    static class IsoRun {
        byte contextDir;
        int contextPos;
        byte lastBase;
        byte lastStrong;
        byte level;
        short limit;
        short start;

        IsoRun() {
        }
    }

    static class BracketData {
        boolean isNumbersSpecial;
        int isoRunLast;
        Opening[] openings = new Opening[20];
        IsoRun[] isoRuns = new IsoRun[127];

        BracketData() {
        }
    }

    static class Isolate {
        int start1;
        int startON;
        short state;
        short stateImp;

        Isolate() {
        }
    }

    static int DirPropFlag(byte dir) {
        return 1 << dir;
    }

    boolean testDirPropFlagAt(int flag, int index) {
        return (DirPropFlag(this.dirProps[index]) & flag) != 0;
    }

    static final int DirPropFlagLR(byte level) {
        return DirPropFlagLR[level & 1];
    }

    static final int DirPropFlagE(byte level) {
        return DirPropFlagE[level & 1];
    }

    static final int DirPropFlagO(byte level) {
        return DirPropFlagO[level & 1];
    }

    static final byte DirFromStrong(byte strong) {
        return strong == 0 ? (byte) 0 : (byte) 1;
    }

    static final byte NoOverride(byte level) {
        return (byte) (level & LEVEL_DEFAULT_RTL);
    }

    static byte GetLRFromLevel(byte level) {
        return (byte) (level & 1);
    }

    static boolean IsDefaultLevel(byte level) {
        return (level & LEVEL_DEFAULT_LTR) == 126;
    }

    static boolean IsBidiControlChar(int c) {
        if ((c & (-4)) == 8204) {
            return true;
        }
        if (c < 8234 || c > 8238) {
            return c >= 8294 && c <= 8297;
        }
        return true;
    }

    void verifyValidPara() {
        if (this == this.paraBidi) {
        } else {
            throw new IllegalStateException();
        }
    }

    void verifyValidParaOrLine() {
        Bidi para = this.paraBidi;
        if (this == para) {
            return;
        }
        if (para != null && para == para.paraBidi) {
        } else {
            throw new IllegalStateException();
        }
    }

    void verifyRange(int index, int start, int limit) {
        if (index >= start && index < limit) {
        } else {
            throw new IllegalArgumentException("Value " + index + " is out of range " + start + " to " + limit);
        }
    }

    public Bidi() {
        this(0, 0);
    }

    public Bidi(int maxLength, int maxRunCount) {
        this.dirPropsMemory = new byte[1];
        this.levelsMemory = new byte[1];
        this.paras_limit = new int[10];
        this.paras_level = new byte[10];
        this.runsMemory = new BidiRun[0];
        this.simpleRuns = new BidiRun[]{new BidiRun()};
        this.customClassifier = null;
        this.insertPoints = new InsertPoints();
        if (maxLength < 0 || maxRunCount < 0) {
            throw new IllegalArgumentException();
        }
        this.bdp = UBiDiProps.INSTANCE;
        if (maxLength > 0) {
            getInitialDirPropsMemory(maxLength);
            getInitialLevelsMemory(maxLength);
        } else {
            this.mayAllocateText = true;
        }
        if (maxRunCount > 0) {
            if (maxRunCount <= 1) {
                return;
            }
            getInitialRunsMemory(maxRunCount);
            return;
        }
        this.mayAllocateRuns = true;
    }

    private Object getMemory(String label, Object array, Class<?> arrayClass, boolean mayAllocate, int sizeNeeded) {
        int len = Array.getLength(array);
        if (sizeNeeded == len) {
            return array;
        }
        if (!mayAllocate) {
            if (sizeNeeded <= len) {
                return array;
            }
            throw new OutOfMemoryError("Failed to allocate memory for " + label);
        }
        try {
            return Array.newInstance(arrayClass, sizeNeeded);
        } catch (Exception e) {
            throw new OutOfMemoryError("Failed to allocate memory for " + label);
        }
    }

    private void getDirPropsMemory(boolean mayAllocate, int len) {
        Object array = getMemory("DirProps", this.dirPropsMemory, Byte.TYPE, mayAllocate, len);
        this.dirPropsMemory = (byte[]) array;
    }

    void getDirPropsMemory(int len) {
        getDirPropsMemory(this.mayAllocateText, len);
    }

    private void getLevelsMemory(boolean mayAllocate, int len) {
        Object array = getMemory("Levels", this.levelsMemory, Byte.TYPE, mayAllocate, len);
        this.levelsMemory = (byte[]) array;
    }

    void getLevelsMemory(int len) {
        getLevelsMemory(this.mayAllocateText, len);
    }

    private void getRunsMemory(boolean mayAllocate, int len) {
        Object array = getMemory("Runs", this.runsMemory, BidiRun.class, mayAllocate, len);
        this.runsMemory = (BidiRun[]) array;
    }

    void getRunsMemory(int len) {
        getRunsMemory(this.mayAllocateRuns, len);
    }

    private void getInitialDirPropsMemory(int len) {
        getDirPropsMemory(true, len);
    }

    private void getInitialLevelsMemory(int len) {
        getLevelsMemory(true, len);
    }

    private void getInitialRunsMemory(int len) {
        getRunsMemory(true, len);
    }

    public void setInverse(boolean isInverse) {
        this.isInverse = isInverse;
        this.reorderingMode = isInverse ? 4 : 0;
    }

    public boolean isInverse() {
        return this.isInverse;
    }

    public void setReorderingMode(int reorderingMode) {
        if (reorderingMode < 0 || reorderingMode >= 7) {
            return;
        }
        this.reorderingMode = reorderingMode;
        this.isInverse = reorderingMode == 4;
    }

    public int getReorderingMode() {
        return this.reorderingMode;
    }

    public void setReorderingOptions(int options) {
        if ((options & 2) != 0) {
            this.reorderingOptions = options & (-2);
        } else {
            this.reorderingOptions = options;
        }
    }

    public int getReorderingOptions() {
        return this.reorderingOptions;
    }

    public static byte getBaseDirection(CharSequence paragraph) {
        if (paragraph == null || paragraph.length() == 0) {
            return (byte) 3;
        }
        int length = paragraph.length();
        int i = 0;
        while (i < length) {
            int c = UCharacter.codePointAt(paragraph, i);
            byte direction = UCharacter.getDirectionality(c);
            if (direction == 0) {
                return (byte) 0;
            }
            if (direction == 1 || direction == 13) {
                return (byte) 1;
            }
            i = UCharacter.offsetByCodePoints(paragraph, i, 1);
        }
        return (byte) 3;
    }

    private byte firstL_R_AL() {
        byte result = 10;
        int i = 0;
        while (i < this.prologue.length()) {
            int uchar = this.prologue.codePointAt(i);
            i += Character.charCount(uchar);
            byte dirProp = (byte) getCustomizedClass(uchar);
            if (result == 10) {
                if (dirProp == 0 || dirProp == 1 || dirProp == 13) {
                    result = dirProp;
                }
            } else if (dirProp == 7) {
                result = 10;
            }
        }
        return result;
    }

    private void checkParaCount() {
        int count = this.paraCount;
        if (count <= this.paras_level.length) {
            return;
        }
        int oldLength = this.paras_level.length;
        int[] saveLimits = this.paras_limit;
        byte[] saveLevels = this.paras_level;
        try {
            this.paras_limit = new int[count * 2];
            this.paras_level = new byte[count * 2];
            System.arraycopy(saveLimits, 0, this.paras_limit, 0, oldLength);
            System.arraycopy(saveLevels, 0, this.paras_level, 0, oldLength);
        } catch (Exception e) {
            throw new OutOfMemoryError("Failed to allocate memory for paras");
        }
    }

    private void getDirProps() {
        boolean isDefaultLevelInverse;
        byte state;
        byte dirProp;
        this.flags = 0;
        boolean isDefaultLevel = IsDefaultLevel(this.paraLevel);
        if (!isDefaultLevel) {
            isDefaultLevelInverse = false;
        } else {
            isDefaultLevelInverse = this.reorderingMode == 5 || this.reorderingMode == 6;
        }
        this.lastArabicPos = -1;
        int controlCount = 0;
        boolean removeBidiControls = (this.reorderingOptions & 2) != 0;
        byte lastStrong = 10;
        int[] isolateStartStack = new int[126];
        byte[] previousStateStack = new byte[126];
        int stackLast = -1;
        if ((this.reorderingOptions & 4) != 0) {
            this.length = 0;
        }
        byte defaultParaLevel = (byte) (this.paraLevel & 1);
        if (isDefaultLevel) {
            this.paras_level[0] = defaultParaLevel;
            lastStrong = defaultParaLevel;
            if (this.prologue != null && (dirProp = firstL_R_AL()) != 10) {
                if (dirProp == 0) {
                    this.paras_level[0] = 0;
                } else {
                    this.paras_level[0] = 1;
                }
                state = 0;
            } else {
                state = 1;
            }
        } else {
            this.paras_level[0] = this.paraLevel;
            state = 0;
        }
        int i = 0;
        while (i < this.originalLength) {
            int i0 = i;
            int uchar = UTF16.charAt(this.text, 0, this.originalLength, i);
            i += UTF16.getCharCount(uchar);
            int i1 = i - 1;
            byte dirProp2 = (byte) getCustomizedClass(uchar);
            this.flags |= DirPropFlag(dirProp2);
            this.dirProps[i1] = dirProp2;
            if (i1 > i0) {
                this.flags |= DirPropFlag((byte) 18);
                do {
                    i1--;
                    this.dirProps[i1] = 18;
                } while (i1 > i0);
            }
            if (removeBidiControls && IsBidiControlChar(uchar)) {
                controlCount++;
            }
            if (dirProp2 == 0) {
                if (state == 1) {
                    this.paras_level[this.paraCount - 1] = 0;
                    state = 0;
                } else if (state == 2) {
                    if (stackLast <= 125) {
                        this.flags |= DirPropFlag((byte) 20);
                    }
                    state = 3;
                }
                lastStrong = 0;
            } else if (dirProp2 == 1 || dirProp2 == 13) {
                if (state == 1) {
                    this.paras_level[this.paraCount - 1] = 1;
                    state = 0;
                } else if (state == 2) {
                    if (stackLast <= 125) {
                        this.dirProps[isolateStartStack[stackLast]] = 21;
                        this.flags |= DirPropFlag((byte) 21);
                    }
                    state = 3;
                }
                lastStrong = 1;
                if (dirProp2 == 13) {
                    this.lastArabicPos = i - 1;
                }
            } else if (dirProp2 >= 19 && dirProp2 <= 21) {
                stackLast++;
                if (stackLast <= 125) {
                    isolateStartStack[stackLast] = i - 1;
                    previousStateStack[stackLast] = state;
                }
                if (dirProp2 == 19) {
                    this.dirProps[i - 1] = 20;
                    state = 2;
                } else {
                    state = 3;
                }
            } else if (dirProp2 == 22) {
                if (state == 2 && stackLast <= 125) {
                    this.flags |= DirPropFlag((byte) 20);
                }
                if (stackLast >= 0) {
                    if (stackLast <= 125) {
                        state = previousStateStack[stackLast];
                    }
                    stackLast--;
                }
            } else if (dirProp2 == 7 && (i >= this.originalLength || uchar != 13 || this.text[i] != '\n')) {
                this.paras_limit[this.paraCount - 1] = i;
                if (isDefaultLevelInverse && lastStrong == 1) {
                    this.paras_level[this.paraCount - 1] = 1;
                }
                if ((this.reorderingOptions & 4) != 0) {
                    this.length = i;
                    this.controlCount = controlCount;
                }
                if (i < this.originalLength) {
                    this.paraCount++;
                    checkParaCount();
                    if (isDefaultLevel) {
                        this.paras_level[this.paraCount - 1] = defaultParaLevel;
                        state = 1;
                        lastStrong = defaultParaLevel;
                    } else {
                        this.paras_level[this.paraCount - 1] = this.paraLevel;
                        state = 0;
                    }
                    stackLast = -1;
                }
            }
        }
        if (stackLast > 125) {
            stackLast = 125;
            state = 2;
        }
        while (true) {
            if (stackLast < 0) {
                break;
            }
            if (state == 2) {
                this.flags |= DirPropFlag((byte) 20);
                break;
            } else {
                state = previousStateStack[stackLast];
                stackLast--;
            }
        }
        if ((this.reorderingOptions & 4) != 0) {
            if (this.length < this.originalLength) {
                this.paraCount--;
            }
        } else {
            this.paras_limit[this.paraCount - 1] = this.originalLength;
            this.controlCount = controlCount;
        }
        if (isDefaultLevelInverse && lastStrong == 1) {
            this.paras_level[this.paraCount - 1] = 1;
        }
        if (isDefaultLevel) {
            this.paraLevel = this.paras_level[0];
        }
        for (int i2 = 0; i2 < this.paraCount; i2++) {
            this.flags |= DirPropFlagLR(this.paras_level[i2]);
        }
        if (!this.orderParagraphsLTR || (this.flags & DirPropFlag((byte) 7)) == 0) {
            return;
        }
        this.flags |= DirPropFlag((byte) 0);
    }

    byte GetParaLevelAt(int pindex) {
        if (this.defaultParaLevel == 0 || pindex < this.paras_limit[0]) {
            return this.paraLevel;
        }
        int i = 1;
        while (i < this.paraCount && pindex >= this.paras_limit[i]) {
            i++;
        }
        if (i >= this.paraCount) {
            i = this.paraCount - 1;
        }
        return this.paras_level[i];
    }

    private void bracketInit(BracketData bd) {
        boolean z = true;
        bd.isoRunLast = 0;
        bd.isoRuns[0] = new IsoRun();
        bd.isoRuns[0].start = (short) 0;
        bd.isoRuns[0].limit = (short) 0;
        bd.isoRuns[0].level = GetParaLevelAt(0);
        IsoRun isoRun = bd.isoRuns[0];
        byte bGetParaLevelAt = (byte) (GetParaLevelAt(0) & 1);
        bd.isoRuns[0].contextDir = bGetParaLevelAt;
        bd.isoRuns[0].lastBase = bGetParaLevelAt;
        isoRun.lastStrong = bGetParaLevelAt;
        bd.isoRuns[0].contextPos = 0;
        bd.openings = new Opening[20];
        if (this.reorderingMode != 1 && this.reorderingMode != 6) {
            z = false;
        }
        bd.isNumbersSpecial = z;
    }

    private void bracketProcessB(BracketData bd, byte level) {
        bd.isoRunLast = 0;
        bd.isoRuns[0].limit = (short) 0;
        bd.isoRuns[0].level = level;
        IsoRun isoRun = bd.isoRuns[0];
        byte b = (byte) (level & 1);
        bd.isoRuns[0].contextDir = b;
        bd.isoRuns[0].lastBase = b;
        isoRun.lastStrong = b;
        bd.isoRuns[0].contextPos = 0;
    }

    private void bracketProcessBoundary(BracketData bd, int lastCcPos, byte contextLevel, byte embeddingLevel) {
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        if ((DirPropFlag(this.dirProps[lastCcPos]) & MASK_ISO) != 0) {
            return;
        }
        if (NoOverride(embeddingLevel) > NoOverride(contextLevel)) {
            contextLevel = embeddingLevel;
        }
        pLastIsoRun.limit = pLastIsoRun.start;
        pLastIsoRun.level = embeddingLevel;
        byte b = (byte) (contextLevel & 1);
        pLastIsoRun.contextDir = b;
        pLastIsoRun.lastBase = b;
        pLastIsoRun.lastStrong = b;
        pLastIsoRun.contextPos = lastCcPos;
    }

    private void bracketProcessLRI_RLI(BracketData bd, byte level) {
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        pLastIsoRun.lastBase = (byte) 10;
        short lastLimit = pLastIsoRun.limit;
        bd.isoRunLast++;
        IsoRun pLastIsoRun2 = bd.isoRuns[bd.isoRunLast];
        if (pLastIsoRun2 == null) {
            pLastIsoRun2 = new IsoRun();
            bd.isoRuns[bd.isoRunLast] = pLastIsoRun2;
        }
        pLastIsoRun2.limit = lastLimit;
        pLastIsoRun2.start = lastLimit;
        pLastIsoRun2.level = level;
        byte b = (byte) (level & 1);
        pLastIsoRun2.contextDir = b;
        pLastIsoRun2.lastBase = b;
        pLastIsoRun2.lastStrong = b;
        pLastIsoRun2.contextPos = 0;
    }

    private void bracketProcessPDI(BracketData bd) {
        bd.isoRunLast--;
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        pLastIsoRun.lastBase = (byte) 10;
    }

    private void bracketAddOpening(BracketData bd, char match, int position) {
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        if (pLastIsoRun.limit >= bd.openings.length) {
            Opening[] saveOpenings = bd.openings;
            try {
                int count = bd.openings.length;
                bd.openings = new Opening[count * 2];
                System.arraycopy(saveOpenings, 0, bd.openings, 0, count);
            } catch (Exception e) {
                throw new OutOfMemoryError("Failed to allocate memory for openings");
            }
        }
        Opening pOpening = bd.openings[pLastIsoRun.limit];
        if (pOpening == null) {
            pOpening = new Opening();
            bd.openings[pLastIsoRun.limit] = pOpening;
        }
        pOpening.position = position;
        pOpening.match = match;
        pOpening.contextDir = pLastIsoRun.contextDir;
        pOpening.contextPos = pLastIsoRun.contextPos;
        pOpening.flags = (short) 0;
        pLastIsoRun.limit = (short) (pLastIsoRun.limit + 1);
    }

    private void fixN0c(BracketData bd, int openingIndex, int newPropPosition, byte newProp) {
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        for (int k = openingIndex + 1; k < pLastIsoRun.limit; k++) {
            Opening qOpening = bd.openings[k];
            if (qOpening.match < 0) {
                if (newPropPosition < qOpening.contextPos) {
                    return;
                }
                if (newPropPosition >= qOpening.position) {
                    continue;
                } else {
                    if (newProp == qOpening.contextDir) {
                        return;
                    }
                    int openingPosition = qOpening.position;
                    this.dirProps[openingPosition] = newProp;
                    int closingPosition = -qOpening.match;
                    this.dirProps[closingPosition] = newProp;
                    qOpening.match = 0;
                    fixN0c(bd, k, openingPosition, newProp);
                    fixN0c(bd, k, closingPosition, newProp);
                }
            }
        }
    }

    private byte bracketProcessClosing(BracketData bd, int openIdx, int position) {
        byte newProp;
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        Opening pOpening = bd.openings[openIdx];
        byte direction = (byte) (pLastIsoRun.level & 1);
        boolean stable = true;
        if ((direction == 0 && (pOpening.flags & FOUND_L) > 0) || (direction == 1 && (pOpening.flags & FOUND_R) > 0)) {
            newProp = direction;
        } else if ((pOpening.flags & (FOUND_L | FOUND_R)) != 0) {
            stable = openIdx == pLastIsoRun.start;
            if (direction != pOpening.contextDir) {
                newProp = pOpening.contextDir;
            } else {
                newProp = direction;
            }
        } else {
            pLastIsoRun.limit = (short) openIdx;
            return (byte) 10;
        }
        this.dirProps[pOpening.position] = newProp;
        this.dirProps[position] = newProp;
        fixN0c(bd, openIdx, pOpening.position, newProp);
        if (stable) {
            pLastIsoRun.limit = (short) openIdx;
            while (pLastIsoRun.limit > pLastIsoRun.start && bd.openings[pLastIsoRun.limit - 1].position == pOpening.position) {
                pLastIsoRun.limit = (short) (pLastIsoRun.limit - 1);
            }
        } else {
            pOpening.match = -position;
            for (int k = openIdx - 1; k >= pLastIsoRun.start && bd.openings[k].position == pOpening.position; k--) {
                bd.openings[k].match = 0;
            }
            for (int k2 = openIdx + 1; k2 < pLastIsoRun.limit; k2++) {
                Opening qOpening = bd.openings[k2];
                if (qOpening.position >= position) {
                    break;
                }
                if (qOpening.match > 0) {
                    qOpening.match = 0;
                }
            }
        }
        return newProp;
    }

    private void bracketProcessChar(BracketData bd, int position) {
        byte newProp;
        IsoRun pLastIsoRun = bd.isoRuns[bd.isoRunLast];
        byte dirProp = this.dirProps[position];
        if (dirProp == 10) {
            char c = this.text[position];
            int idx = pLastIsoRun.limit - 1;
            while (true) {
                if (idx < pLastIsoRun.start) {
                    break;
                }
                if (bd.openings[idx].match != c) {
                    idx--;
                } else {
                    byte newProp2 = bracketProcessClosing(bd, idx, position);
                    if (newProp2 == 10) {
                        c = 0;
                    } else {
                        pLastIsoRun.lastBase = (byte) 10;
                        pLastIsoRun.contextDir = newProp2;
                        pLastIsoRun.contextPos = position;
                        byte level = this.levels[position];
                        if ((level & LEVEL_OVERRIDE) != 0) {
                            byte newProp3 = (byte) (level & 1);
                            pLastIsoRun.lastStrong = newProp3;
                            short flag = (short) DirPropFlag(newProp3);
                            for (int i = pLastIsoRun.start; i < idx; i++) {
                                Opening opening = bd.openings[i];
                                opening.flags = (short) (opening.flags | flag);
                            }
                            byte[] bArr = this.levels;
                            bArr[position] = (byte) (bArr[position] & LEVEL_DEFAULT_RTL);
                        }
                        byte[] bArr2 = this.levels;
                        int i2 = bd.openings[idx].position;
                        bArr2[i2] = (byte) (bArr2[i2] & LEVEL_DEFAULT_RTL);
                        return;
                    }
                }
            }
        }
        byte level2 = this.levels[position];
        if ((level2 & LEVEL_OVERRIDE) != 0) {
            newProp = (byte) (level2 & 1);
            if (dirProp != 8 && dirProp != 9 && dirProp != 10) {
                this.dirProps[position] = newProp;
            }
            pLastIsoRun.lastBase = newProp;
            pLastIsoRun.lastStrong = newProp;
            pLastIsoRun.contextDir = newProp;
            pLastIsoRun.contextPos = position;
        } else if (dirProp <= 1 || dirProp == 13) {
            newProp = DirFromStrong(dirProp);
            pLastIsoRun.lastBase = dirProp;
            pLastIsoRun.lastStrong = dirProp;
            pLastIsoRun.contextDir = newProp;
            pLastIsoRun.contextPos = position;
        } else if (dirProp == 2) {
            pLastIsoRun.lastBase = (byte) 2;
            if (pLastIsoRun.lastStrong == 0) {
                newProp = 0;
                if (!bd.isNumbersSpecial) {
                    this.dirProps[position] = 23;
                }
                pLastIsoRun.contextDir = (byte) 0;
                pLastIsoRun.contextPos = position;
            } else {
                newProp = 1;
                if (pLastIsoRun.lastStrong == 13) {
                    this.dirProps[position] = 5;
                } else {
                    this.dirProps[position] = 24;
                }
                pLastIsoRun.contextDir = (byte) 1;
                pLastIsoRun.contextPos = position;
            }
        } else if (dirProp == 5) {
            newProp = 1;
            pLastIsoRun.lastBase = (byte) 5;
            pLastIsoRun.contextDir = (byte) 1;
            pLastIsoRun.contextPos = position;
        } else if (dirProp == 17) {
            newProp = pLastIsoRun.lastBase;
            if (newProp == 10) {
                this.dirProps[position] = newProp;
            }
        } else {
            newProp = dirProp;
            pLastIsoRun.lastBase = dirProp;
        }
        if (newProp > 1 && newProp != 13) {
            return;
        }
        short flag2 = (short) DirPropFlag(DirFromStrong(newProp));
        for (int i3 = pLastIsoRun.start; i3 < pLastIsoRun.limit; i3++) {
            if (position > bd.openings[i3].position) {
                Opening opening2 = bd.openings[i3];
                opening2.flags = (short) (opening2.flags | flag2);
            }
        }
    }

    private byte directionFromFlags() {
        if ((this.flags & MASK_RTL) == 0 && ((this.flags & DirPropFlag((byte) 5)) == 0 || (this.flags & MASK_POSSIBLE_N) == 0)) {
            return (byte) 0;
        }
        if ((this.flags & MASK_LTR) == 0) {
            return (byte) 1;
        }
        return (byte) 2;
    }

    private byte resolveExplicitLevels() {
        byte newLevel;
        byte newLevel2;
        int start;
        int start2;
        byte level = GetParaLevelAt(0);
        this.isolateCount = 0;
        byte dirct = directionFromFlags();
        if (dirct != 2) {
            return dirct;
        }
        if (this.reorderingMode > 1) {
            for (int paraIndex = 0; paraIndex < this.paraCount; paraIndex++) {
                if (paraIndex == 0) {
                    start2 = 0;
                } else {
                    start2 = this.paras_limit[paraIndex - 1];
                }
                int limit = this.paras_limit[paraIndex];
                byte level2 = this.paras_level[paraIndex];
                for (int i = start2; i < limit; i++) {
                    this.levels[i] = level2;
                }
            }
            return dirct;
        }
        if ((this.flags & (MASK_EXPLICIT | MASK_ISO)) == 0) {
            BracketData bracketData = new BracketData();
            bracketInit(bracketData);
            for (int paraIndex2 = 0; paraIndex2 < this.paraCount; paraIndex2++) {
                if (paraIndex2 == 0) {
                    start = 0;
                } else {
                    start = this.paras_limit[paraIndex2 - 1];
                }
                int limit2 = this.paras_limit[paraIndex2];
                byte level3 = this.paras_level[paraIndex2];
                for (int i2 = start; i2 < limit2; i2++) {
                    this.levels[i2] = level3;
                    byte dirProp = this.dirProps[i2];
                    if (dirProp != 18) {
                        if (dirProp == 7) {
                            if (i2 + 1 < this.length && (this.text[i2] != '\r' || this.text[i2 + 1] != '\n')) {
                                bracketProcessB(bracketData, level3);
                            }
                        } else {
                            bracketProcessChar(bracketData, i2);
                        }
                    }
                }
            }
            return dirct;
        }
        byte embeddingLevel = level;
        byte previousLevel = level;
        int lastCcPos = 0;
        short[] stack = new short[127];
        int stackLast = 0;
        int overflowIsolateCount = 0;
        int overflowEmbeddingCount = 0;
        int validIsolateCount = 0;
        BracketData bracketData2 = new BracketData();
        bracketInit(bracketData2);
        stack[0] = level;
        this.flags = 0;
        for (int i3 = 0; i3 < this.length; i3++) {
            byte dirProp2 = this.dirProps[i3];
            switch (dirProp2) {
                case 7:
                    this.flags |= DirPropFlag((byte) 7);
                    this.levels[i3] = GetParaLevelAt(i3);
                    if (i3 + 1 < this.length && (this.text[i3] != '\r' || this.text[i3 + 1] != '\n')) {
                        overflowIsolateCount = 0;
                        overflowEmbeddingCount = 0;
                        validIsolateCount = 0;
                        stackLast = 0;
                        embeddingLevel = GetParaLevelAt(i3 + 1);
                        previousLevel = embeddingLevel;
                        stack[0] = embeddingLevel;
                        bracketProcessB(bracketData2, embeddingLevel);
                    }
                    break;
                case 8:
                case 9:
                case 10:
                case 13:
                case 17:
                case 19:
                default:
                    if (NoOverride(embeddingLevel) != NoOverride(previousLevel)) {
                        bracketProcessBoundary(bracketData2, lastCcPos, previousLevel, embeddingLevel);
                        this.flags |= DirPropFlagMultiRuns;
                        if ((embeddingLevel & LEVEL_OVERRIDE) != 0) {
                            this.flags |= DirPropFlagO(embeddingLevel);
                        } else {
                            this.flags |= DirPropFlagE(embeddingLevel);
                        }
                    }
                    previousLevel = embeddingLevel;
                    this.levels[i3] = embeddingLevel;
                    bracketProcessChar(bracketData2, i3);
                    this.flags |= DirPropFlag(this.dirProps[i3]);
                    break;
                case 11:
                case 12:
                case 14:
                case 15:
                    this.flags |= DirPropFlag((byte) 18);
                    this.levels[i3] = previousLevel;
                    if (dirProp2 == 11 || dirProp2 == 12) {
                        newLevel2 = (byte) ((embeddingLevel + 2) & 126);
                    } else {
                        newLevel2 = (byte) ((NoOverride(embeddingLevel) + 1) | 1);
                    }
                    if (newLevel2 <= 125 && overflowIsolateCount == 0 && overflowEmbeddingCount == 0) {
                        lastCcPos = i3;
                        embeddingLevel = newLevel2;
                        if (dirProp2 == 12 || dirProp2 == 15) {
                            embeddingLevel = (byte) (embeddingLevel | LEVEL_OVERRIDE);
                        }
                        stackLast++;
                        stack[stackLast] = embeddingLevel;
                    } else if (overflowIsolateCount == 0) {
                        overflowEmbeddingCount++;
                    }
                    break;
                case 16:
                    this.flags |= DirPropFlag((byte) 18);
                    this.levels[i3] = previousLevel;
                    if (overflowIsolateCount <= 0) {
                        if (overflowEmbeddingCount > 0) {
                            overflowEmbeddingCount--;
                        } else if (stackLast > 0 && stack[stackLast] < 256) {
                            lastCcPos = i3;
                            stackLast--;
                            embeddingLevel = (byte) stack[stackLast];
                        }
                    }
                    break;
                case 18:
                    this.levels[i3] = previousLevel;
                    this.flags |= DirPropFlag((byte) 18);
                    break;
                case 20:
                case 21:
                    this.flags |= DirPropFlag((byte) 10) | DirPropFlagLR(embeddingLevel);
                    this.levels[i3] = NoOverride(embeddingLevel);
                    if (NoOverride(embeddingLevel) != NoOverride(previousLevel)) {
                        bracketProcessBoundary(bracketData2, lastCcPos, previousLevel, embeddingLevel);
                        this.flags |= DirPropFlagMultiRuns;
                    }
                    previousLevel = embeddingLevel;
                    if (dirProp2 == 20) {
                        newLevel = (byte) ((embeddingLevel + 2) & 126);
                    } else {
                        newLevel = (byte) ((NoOverride(embeddingLevel) + 1) | 1);
                    }
                    if (newLevel <= 125 && overflowIsolateCount == 0 && overflowEmbeddingCount == 0) {
                        this.flags |= DirPropFlag(dirProp2);
                        lastCcPos = i3;
                        validIsolateCount++;
                        if (validIsolateCount > this.isolateCount) {
                            this.isolateCount = validIsolateCount;
                        }
                        embeddingLevel = newLevel;
                        stackLast++;
                        stack[stackLast] = (short) (newLevel + 256);
                        bracketProcessLRI_RLI(bracketData2, embeddingLevel);
                    } else {
                        this.dirProps[i3] = 9;
                        overflowIsolateCount++;
                    }
                    break;
                case 22:
                    if (NoOverride(embeddingLevel) != NoOverride(previousLevel)) {
                        bracketProcessBoundary(bracketData2, lastCcPos, previousLevel, embeddingLevel);
                        this.flags |= DirPropFlagMultiRuns;
                    }
                    if (overflowIsolateCount > 0) {
                        overflowIsolateCount--;
                        this.dirProps[i3] = 9;
                    } else if (validIsolateCount > 0) {
                        this.flags |= DirPropFlag((byte) 22);
                        lastCcPos = i3;
                        overflowEmbeddingCount = 0;
                        while (stack[stackLast] < 256) {
                            stackLast--;
                        }
                        stackLast--;
                        validIsolateCount--;
                        bracketProcessPDI(bracketData2);
                    } else {
                        this.dirProps[i3] = 9;
                    }
                    embeddingLevel = (byte) (stack[stackLast] & (-257));
                    this.flags |= DirPropFlag((byte) 10) | DirPropFlagLR(embeddingLevel);
                    previousLevel = embeddingLevel;
                    this.levels[i3] = NoOverride(embeddingLevel);
                    break;
            }
        }
        if ((this.flags & MASK_EMBEDDING) != 0) {
            this.flags |= DirPropFlagLR(this.paraLevel);
        }
        if (this.orderParagraphsLTR && (this.flags & DirPropFlag((byte) 7)) != 0) {
            this.flags |= DirPropFlag((byte) 0);
        }
        return directionFromFlags();
    }

    private byte checkExplicitLevels() {
        int isolateCount = 0;
        this.flags = 0;
        this.isolateCount = 0;
        for (int i = 0; i < this.length; i++) {
            byte level = this.levels[i];
            byte dirProp = this.dirProps[i];
            if (dirProp == 20 || dirProp == 21) {
                isolateCount++;
                if (isolateCount > this.isolateCount) {
                    this.isolateCount = isolateCount;
                }
            } else if (dirProp == 22) {
                isolateCount--;
            } else if (dirProp == 7) {
                isolateCount = 0;
            }
            if ((level & LEVEL_OVERRIDE) != 0) {
                level = (byte) (level & LEVEL_DEFAULT_RTL);
                this.flags |= DirPropFlagO(level);
            } else {
                this.flags |= DirPropFlagE(level) | DirPropFlag(dirProp);
            }
            if ((level < GetParaLevelAt(i) && (level != 0 || dirProp != 7)) || 125 < level) {
                throw new IllegalArgumentException("level " + ((int) level) + " out of bounds at " + i);
            }
        }
        if ((this.flags & MASK_EMBEDDING) != 0) {
            this.flags |= DirPropFlagLR(this.paraLevel);
        }
        return directionFromFlags();
    }

    private static short GetStateProps(short cell) {
        return (short) (cell & 31);
    }

    private static short GetActionProps(short cell) {
        return (short) (cell >> 5);
    }

    private static short GetState(byte cell) {
        return (short) (cell & 15);
    }

    private static short GetAction(byte cell) {
        return (short) (cell >> 4);
    }

    private static class ImpTabPair {
        short[][] impact;
        byte[][][] imptab;

        ImpTabPair(byte[][] table1, byte[][] table2, short[] act1, short[] act2) {
            this.imptab = new byte[][][]{table1, table2};
            this.impact = new short[][]{act1, act2};
        }
    }

    private static class LevState {
        short[] impAct;
        byte[][] impTab;
        int lastStrongRTL;
        byte runLevel;
        int runStart;
        int startL2EN;
        int startON;
        short state;

        LevState(LevState levState) {
            this();
        }

        private LevState() {
        }
    }

    private void addPoint(int pos, int flag) {
        Point point = new Point();
        int len = this.insertPoints.points.length;
        if (len == 0) {
            this.insertPoints.points = new Point[10];
            len = 10;
        }
        if (this.insertPoints.size >= len) {
            Point[] savePoints = this.insertPoints.points;
            this.insertPoints.points = new Point[len * 2];
            System.arraycopy(savePoints, 0, this.insertPoints.points, 0, len);
        }
        point.pos = pos;
        point.flag = flag;
        this.insertPoints.points[this.insertPoints.size] = point;
        this.insertPoints.size++;
    }

    private void setLevelsOutsideIsolates(int start, int limit, byte level) {
        int isolateCount = 0;
        for (int k = start; k < limit; k++) {
            byte dirProp = this.dirProps[k];
            if (dirProp == 22) {
                isolateCount--;
            }
            if (isolateCount == 0) {
                this.levels[k] = level;
            }
            if (dirProp == 20 || dirProp == 21) {
                isolateCount++;
            }
        }
    }

    private void processPropertySeq(LevState levState, short _prop, int start, int limit) {
        byte[][] impTab = levState.impTab;
        short[] impAct = levState.impAct;
        short oldStateSeq = levState.state;
        byte cell = impTab[oldStateSeq][_prop];
        levState.state = GetState(cell);
        short actionSeq = impAct[GetAction(cell)];
        byte addLevel = impTab[levState.state][7];
        if (actionSeq != 0) {
            switch (actionSeq) {
                case 1:
                    levState.startON = start;
                    break;
                case 2:
                    start = levState.startON;
                    break;
                case 3:
                    setLevelsOutsideIsolates(levState.startON, start, (byte) (levState.runLevel + 1));
                    break;
                case 4:
                    setLevelsOutsideIsolates(levState.startON, start, (byte) (levState.runLevel + 2));
                    break;
                case 5:
                    if (levState.startL2EN >= 0) {
                        addPoint(levState.startL2EN, 1);
                    }
                    levState.startL2EN = -1;
                    if (this.insertPoints.points.length == 0 || this.insertPoints.size <= this.insertPoints.confirmed) {
                        levState.lastStrongRTL = -1;
                        if ((impTab[oldStateSeq][7] & 1) != 0 && levState.startON > 0) {
                            start = levState.startON;
                        }
                        if (_prop == 5) {
                            addPoint(start, 1);
                            this.insertPoints.confirmed = this.insertPoints.size;
                        }
                    } else {
                        for (int k = levState.lastStrongRTL + 1; k < start; k++) {
                            this.levels[k] = (byte) ((this.levels[k] - 2) & (-2));
                        }
                        this.insertPoints.confirmed = this.insertPoints.size;
                        levState.lastStrongRTL = -1;
                        if (_prop == 5) {
                            addPoint(start, 1);
                            this.insertPoints.confirmed = this.insertPoints.size;
                        }
                    }
                    break;
                case 6:
                    if (this.insertPoints.points.length > 0) {
                        this.insertPoints.size = this.insertPoints.confirmed;
                    }
                    levState.startON = -1;
                    levState.startL2EN = -1;
                    levState.lastStrongRTL = limit - 1;
                    break;
                case 7:
                    if (_prop == 3 && this.dirProps[start] == 5 && this.reorderingMode != 6) {
                        if (levState.startL2EN == -1) {
                            levState.lastStrongRTL = limit - 1;
                        } else {
                            if (levState.startL2EN >= 0) {
                                addPoint(levState.startL2EN, 1);
                                levState.startL2EN = -2;
                            }
                            addPoint(start, 1);
                        }
                    } else if (levState.startL2EN == -1) {
                        levState.startL2EN = start;
                    }
                    break;
                case 8:
                    levState.lastStrongRTL = limit - 1;
                    levState.startON = -1;
                    break;
                case 9:
                    int k2 = start - 1;
                    while (k2 >= 0 && (this.levels[k2] & 1) == 0) {
                        k2--;
                    }
                    if (k2 >= 0) {
                        addPoint(k2, 4);
                        this.insertPoints.confirmed = this.insertPoints.size;
                    }
                    levState.startON = start;
                    break;
                case 10:
                    addPoint(start, 1);
                    addPoint(start, 2);
                    break;
                case 11:
                    this.insertPoints.size = this.insertPoints.confirmed;
                    if (_prop == 5) {
                        addPoint(start, 4);
                        this.insertPoints.confirmed = this.insertPoints.size;
                    }
                    break;
                case 12:
                    byte level = (byte) (levState.runLevel + addLevel);
                    for (int k3 = levState.startON; k3 < start; k3++) {
                        if (this.levels[k3] < level) {
                            this.levels[k3] = level;
                        }
                    }
                    this.insertPoints.confirmed = this.insertPoints.size;
                    levState.startON = start;
                    break;
                case 13:
                    byte level2 = levState.runLevel;
                    int k4 = start - 1;
                    while (k4 >= levState.startON) {
                        if (this.levels[k4] == level2 + 3) {
                            while (this.levels[k4] == level2 + 3) {
                                this.levels[k4] = (byte) (r12[k4] - 2);
                                k4--;
                            }
                            while (this.levels[k4] == level2) {
                                k4--;
                            }
                        }
                        if (this.levels[k4] == level2 + 2) {
                            this.levels[k4] = level2;
                        } else {
                            this.levels[k4] = (byte) (level2 + 1);
                        }
                        k4--;
                    }
                    break;
                case 14:
                    byte level3 = (byte) (levState.runLevel + 1);
                    for (int k5 = start - 1; k5 >= levState.startON; k5--) {
                        if (this.levels[k5] > level3) {
                            this.levels[k5] = (byte) (r12[k5] - 2);
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("Internal ICU error in processPropertySeq");
            }
        }
        if (addLevel == 0 && start >= start) {
            return;
        }
        byte level4 = (byte) (levState.runLevel + addLevel);
        if (start >= levState.runStart) {
            for (int k6 = start; k6 < limit; k6++) {
                this.levels[k6] = level4;
            }
            return;
        }
        setLevelsOutsideIsolates(start, limit, level4);
    }

    private byte lastL_R_AL() {
        int i = this.prologue.length();
        while (i > 0) {
            int uchar = this.prologue.codePointBefore(i);
            i -= Character.charCount(uchar);
            byte dirProp = (byte) getCustomizedClass(uchar);
            if (dirProp == 0) {
                return (byte) 0;
            }
            if (dirProp == 1 || dirProp == 13) {
                return (byte) 1;
            }
            if (dirProp == 7) {
                return (byte) 4;
            }
        }
        return (byte) 4;
    }

    private byte firstL_R_AL_EN_AN() {
        int i = 0;
        while (i < this.epilogue.length()) {
            int uchar = this.epilogue.codePointAt(i);
            i += Character.charCount(uchar);
            byte dirProp = (byte) getCustomizedClass(uchar);
            if (dirProp == 0) {
                return (byte) 0;
            }
            if (dirProp == 1 || dirProp == 13) {
                return (byte) 1;
            }
            if (dirProp == 2) {
                return (byte) 2;
            }
            if (dirProp == 5) {
                return (byte) 3;
            }
        }
        return (byte) 4;
    }

    private void resolveImplicitLevels(int start, int limit, short sor, short eor) {
        boolean inverseRTL;
        int start1;
        short stateImp;
        int i;
        byte dirProp;
        byte firstStrong;
        short gprop;
        byte lastStrong;
        LevState levState = new LevState(null);
        short nextStrongProp = 1;
        int nextStrongPos = -1;
        if (start >= this.lastArabicPos || (GetParaLevelAt(start) & 1) <= 0) {
            inverseRTL = false;
        } else {
            inverseRTL = this.reorderingMode == 5 || this.reorderingMode == 6;
        }
        levState.startL2EN = -1;
        levState.lastStrongRTL = -1;
        levState.runStart = start;
        levState.runLevel = this.levels[start];
        levState.impTab = this.impTabPair.imptab[levState.runLevel & 1];
        levState.impAct = this.impTabPair.impact[levState.runLevel & 1];
        if (start == 0 && this.prologue != null && (lastStrong = lastL_R_AL()) != 4) {
            sor = lastStrong;
        }
        if (this.dirProps[start] == 22) {
            levState.startON = this.isolates[this.isolateCount].startON;
            start1 = this.isolates[this.isolateCount].start1;
            stateImp = this.isolates[this.isolateCount].stateImp;
            levState.state = this.isolates[this.isolateCount].state;
            this.isolateCount--;
        } else {
            levState.startON = -1;
            start1 = start;
            if (this.dirProps[start] == 17) {
                stateImp = (short) (sor + 1);
            } else {
                stateImp = 0;
            }
            levState.state = (short) 0;
            processPropertySeq(levState, sor, start, start);
        }
        int start2 = start;
        for (int i2 = start; i2 <= limit; i2++) {
            if (i2 >= limit) {
                int k = limit - 1;
                while (k > start && (DirPropFlag(this.dirProps[k]) & MASK_BN_EXPLICIT) != 0) {
                    k--;
                }
                byte dirProp2 = this.dirProps[k];
                if (dirProp2 != 20 && dirProp2 != 21) {
                    gprop = eor;
                } else {
                    if (limit == this.length && this.epilogue != null && (firstStrong = firstL_R_AL_EN_AN()) != 4) {
                        eor = firstStrong;
                    }
                    i = limit - 1;
                    while (i > start && (DirPropFlag(this.dirProps[i]) & MASK_BN_EXPLICIT) != 0) {
                        i--;
                    }
                    dirProp = this.dirProps[i];
                    if ((dirProp != 20 || dirProp == 21) && limit < this.length) {
                        this.isolateCount++;
                        if (this.isolates[this.isolateCount] == null) {
                            this.isolates[this.isolateCount] = new Isolate();
                        }
                        this.isolates[this.isolateCount].stateImp = stateImp;
                        this.isolates[this.isolateCount].state = levState.state;
                        this.isolates[this.isolateCount].start1 = start1;
                        this.isolates[this.isolateCount].startON = levState.startON;
                        return;
                    }
                    processPropertySeq(levState, eor, limit, limit);
                }
            } else {
                byte prop = this.dirProps[i2];
                if (prop == 7) {
                    this.isolateCount = -1;
                }
                if (inverseRTL) {
                    if (prop == 13) {
                        prop = 1;
                    } else if (prop == 2) {
                        if (nextStrongPos <= i2) {
                            nextStrongProp = 1;
                            nextStrongPos = limit;
                            for (int j = i2 + 1; j < limit; j++) {
                                byte prop1 = this.dirProps[j];
                                if (prop1 == 0 || prop1 == 1 || prop1 == 13) {
                                    nextStrongProp = prop1;
                                    nextStrongPos = j;
                                }
                            }
                        }
                        if (nextStrongProp == 13) {
                            prop = 5;
                        }
                    }
                }
                gprop = groupProp[prop];
            }
            short oldStateImp = stateImp;
            short cell = impTabProps[oldStateImp][gprop];
            stateImp = GetStateProps(cell);
            short actionImp = GetActionProps(cell);
            if (i2 == limit && actionImp == 0) {
                actionImp = 1;
            }
            if (actionImp != 0) {
                short resProp = impTabProps[oldStateImp][15];
                switch (actionImp) {
                    case 1:
                        processPropertySeq(levState, resProp, start1, i2);
                        start1 = i2;
                        break;
                    case 2:
                        start2 = i2;
                        break;
                    case 3:
                        processPropertySeq(levState, resProp, start1, start2);
                        processPropertySeq(levState, (short) 4, start2, i2);
                        start1 = i2;
                        break;
                    case 4:
                        processPropertySeq(levState, resProp, start1, start2);
                        start1 = start2;
                        start2 = i2;
                        break;
                    default:
                        throw new IllegalStateException("Internal ICU error in resolveImplicitLevels");
                }
            }
        }
        if (limit == this.length) {
            eor = firstStrong;
        }
        i = limit - 1;
        while (i > start) {
            i--;
        }
        dirProp = this.dirProps[i];
        if (dirProp != 20) {
            this.isolateCount++;
            if (this.isolates[this.isolateCount] == null) {
            }
            this.isolates[this.isolateCount].stateImp = stateImp;
            this.isolates[this.isolateCount].state = levState.state;
            this.isolates[this.isolateCount].start1 = start1;
            this.isolates[this.isolateCount].startON = levState.startON;
            return;
        }
        this.isolateCount++;
        if (this.isolates[this.isolateCount] == null) {
        }
        this.isolates[this.isolateCount].stateImp = stateImp;
        this.isolates[this.isolateCount].state = levState.state;
        this.isolates[this.isolateCount].start1 = start1;
        this.isolates[this.isolateCount].startON = levState.startON;
        return;
        processPropertySeq(levState, eor, limit, limit);
    }

    private void adjustWSLevels() {
        if ((this.flags & MASK_WS) == 0) {
            return;
        }
        int i = this.trailingWSStart;
        while (i > 0) {
            while (i > 0) {
                i--;
                int flag = DirPropFlag(this.dirProps[i]);
                if ((MASK_WS & flag) == 0) {
                    break;
                }
                if (this.orderParagraphsLTR && (DirPropFlag((byte) 7) & flag) != 0) {
                    this.levels[i] = 0;
                } else {
                    this.levels[i] = GetParaLevelAt(i);
                }
            }
            while (true) {
                if (i > 0) {
                    i--;
                    int flag2 = DirPropFlag(this.dirProps[i]);
                    if ((MASK_BN_EXPLICIT & flag2) != 0) {
                        this.levels[i] = this.levels[i + 1];
                    } else if (this.orderParagraphsLTR && (DirPropFlag((byte) 7) & flag2) != 0) {
                        this.levels[i] = 0;
                        break;
                    } else if ((MASK_B_S & flag2) != 0) {
                        this.levels[i] = GetParaLevelAt(i);
                        break;
                    }
                }
            }
        }
    }

    public void setContext(String prologue, String epilogue) {
        if (prologue == null || prologue.length() <= 0) {
            prologue = null;
        }
        this.prologue = prologue;
        if (epilogue == null || epilogue.length() <= 0) {
            epilogue = null;
        }
        this.epilogue = epilogue;
    }

    private void setParaSuccess() {
        this.prologue = null;
        this.epilogue = null;
        this.paraBidi = this;
    }

    int Bidi_Min(int x, int y) {
        return x < y ? x : y;
    }

    int Bidi_Abs(int x) {
        return x >= 0 ? x : -x;
    }

    void setParaRunsOnly(char[] parmText, byte parmParaLevel) {
        int start;
        int limit;
        int step;
        this.reorderingMode = 0;
        int parmLength = parmText.length;
        if (parmLength == 0) {
            setPara(parmText, parmParaLevel, (byte[]) null);
            this.reorderingMode = 3;
            return;
        }
        int saveOptions = this.reorderingOptions;
        if ((saveOptions & 1) > 0) {
            this.reorderingOptions &= -2;
            this.reorderingOptions |= 2;
        }
        byte parmParaLevel2 = (byte) (parmParaLevel & 1);
        setPara(parmText, parmParaLevel2, (byte[]) null);
        byte[] saveLevels = new byte[this.length];
        System.arraycopy(getLevels(), 0, saveLevels, 0, this.length);
        int saveTrailingWSStart = this.trailingWSStart;
        String visualText = writeReordered(2);
        int[] visualMap = getVisualMap();
        this.reorderingOptions = saveOptions;
        int saveLength = this.length;
        byte saveDirection = this.direction;
        this.reorderingMode = 5;
        setPara(visualText, (byte) (parmParaLevel2 ^ 1), (byte[]) null);
        BidiLine.getRuns(this);
        int addedRuns = 0;
        int oldRunCount = this.runCount;
        int visualStart = 0;
        int i = 0;
        while (i < oldRunCount) {
            int runLength = this.runs[i].limit - visualStart;
            if (runLength >= 2) {
                int logicalStart = this.runs[i].start;
                for (int j = logicalStart + 1; j < logicalStart + runLength; j++) {
                    int index = visualMap[j];
                    int index1 = visualMap[j - 1];
                    if (Bidi_Abs(index - index1) != 1 || saveLevels[index] != saveLevels[index1]) {
                        addedRuns++;
                    }
                }
            }
            i++;
            visualStart += runLength;
        }
        if (addedRuns > 0) {
            getRunsMemory(oldRunCount + addedRuns);
            if (this.runCount == 1) {
                this.runsMemory[0] = this.runs[0];
            } else {
                System.arraycopy(this.runs, 0, this.runsMemory, 0, this.runCount);
            }
            this.runs = this.runsMemory;
            this.runCount += addedRuns;
            for (int i2 = oldRunCount; i2 < this.runCount; i2++) {
                if (this.runs[i2] == null) {
                    this.runs[i2] = new BidiRun(0, 0, (byte) 0);
                }
            }
        }
        int i3 = oldRunCount - 1;
        while (i3 >= 0) {
            int newI = i3 + addedRuns;
            int runLength2 = i3 == 0 ? this.runs[0].limit : this.runs[i3].limit - this.runs[i3 - 1].limit;
            int logicalStart2 = this.runs[i3].start;
            int indexOddBit = this.runs[i3].level & 1;
            if (runLength2 < 2) {
                if (addedRuns > 0) {
                    this.runs[newI].copyFrom(this.runs[i3]);
                }
                int logicalPos = visualMap[logicalStart2];
                this.runs[newI].start = logicalPos;
                this.runs[newI].level = (byte) (saveLevels[logicalPos] ^ indexOddBit);
            } else {
                if (indexOddBit > 0) {
                    start = logicalStart2;
                    limit = (logicalStart2 + runLength2) - 1;
                    step = 1;
                } else {
                    start = (logicalStart2 + runLength2) - 1;
                    limit = logicalStart2;
                    step = -1;
                }
                for (int j2 = start; j2 != limit; j2 += step) {
                    int index2 = visualMap[j2];
                    int index12 = visualMap[j2 + step];
                    if (Bidi_Abs(index2 - index12) != 1 || saveLevels[index2] != saveLevels[index12]) {
                        int logicalPos2 = Bidi_Min(visualMap[start], index2);
                        this.runs[newI].start = logicalPos2;
                        this.runs[newI].level = (byte) (saveLevels[logicalPos2] ^ indexOddBit);
                        this.runs[newI].limit = this.runs[i3].limit;
                        this.runs[i3].limit -= Bidi_Abs(j2 - start) + 1;
                        int insertRemove = this.runs[i3].insertRemove & 10;
                        this.runs[newI].insertRemove = insertRemove;
                        this.runs[i3].insertRemove &= ~insertRemove;
                        start = j2 + step;
                        addedRuns--;
                        newI--;
                    }
                }
                if (addedRuns > 0) {
                    this.runs[newI].copyFrom(this.runs[i3]);
                }
                int logicalPos3 = Bidi_Min(visualMap[start], visualMap[limit]);
                this.runs[newI].start = logicalPos3;
                this.runs[newI].level = (byte) (saveLevels[logicalPos3] ^ indexOddBit);
            }
            i3--;
        }
        this.paraLevel = (byte) (this.paraLevel ^ 1);
        this.text = parmText;
        this.length = saveLength;
        this.originalLength = parmLength;
        this.direction = saveDirection;
        this.levels = saveLevels;
        this.trailingWSStart = saveTrailingWSStart;
        if (this.runCount > 1) {
            this.direction = (byte) 2;
        }
        this.reorderingMode = 3;
    }

    public void setPara(String text, byte paraLevel, byte[] embeddingLevels) {
        if (text == null) {
            setPara(new char[0], paraLevel, embeddingLevels);
        } else {
            setPara(text.toCharArray(), paraLevel, embeddingLevels);
        }
    }

    public void setPara(char[] chars, byte paraLevel, byte[] embeddingLevels) {
        int limit;
        byte level;
        byte nextLevel;
        short eor;
        byte level2;
        short sor;
        if (paraLevel < 126) {
            verifyRange(paraLevel, 0, 126);
        }
        if (chars == null) {
            chars = new char[0];
        }
        if (this.reorderingMode == 3) {
            setParaRunsOnly(chars, paraLevel);
            return;
        }
        this.paraBidi = null;
        this.text = chars;
        int length = this.text.length;
        this.resultLength = length;
        this.originalLength = length;
        this.length = length;
        this.paraLevel = paraLevel;
        this.direction = (byte) (paraLevel & 1);
        this.paraCount = 1;
        this.dirProps = new byte[0];
        this.levels = new byte[0];
        this.runs = new BidiRun[0];
        this.isGoodLogicalToVisualRunsMap = false;
        this.insertPoints.size = 0;
        this.insertPoints.confirmed = 0;
        this.defaultParaLevel = IsDefaultLevel(paraLevel) ? paraLevel : (byte) 0;
        if (this.length == 0) {
            if (IsDefaultLevel(paraLevel)) {
                this.paraLevel = (byte) (this.paraLevel & 1);
                this.defaultParaLevel = (byte) 0;
            }
            this.flags = DirPropFlagLR(paraLevel);
            this.runCount = 0;
            this.paraCount = 0;
            setParaSuccess();
            return;
        }
        this.runCount = -1;
        getDirPropsMemory(this.length);
        this.dirProps = this.dirPropsMemory;
        getDirProps();
        this.trailingWSStart = this.length;
        if (embeddingLevels == null) {
            getLevelsMemory(this.length);
            this.levels = this.levelsMemory;
            this.direction = resolveExplicitLevels();
        } else {
            this.levels = embeddingLevels;
            this.direction = checkExplicitLevels();
        }
        if (this.isolateCount > 0 && (this.isolates == null || this.isolates.length < this.isolateCount)) {
            this.isolates = new Isolate[this.isolateCount + 3];
        }
        this.isolateCount = -1;
        switch (this.direction) {
            case 0:
                this.trailingWSStart = 0;
                break;
            case 1:
                this.trailingWSStart = 0;
                break;
            default:
                switch (this.reorderingMode) {
                    case 0:
                        this.impTabPair = impTab_DEFAULT;
                        if (embeddingLevels == null && this.paraCount <= 1 && (this.flags & DirPropFlagMultiRuns) == 0) {
                            resolveImplicitLevels(0, this.length, GetLRFromLevel(GetParaLevelAt(0)), GetLRFromLevel(GetParaLevelAt(this.length - 1)));
                        } else {
                            limit = 0;
                            level = GetParaLevelAt(0);
                            nextLevel = this.levels[0];
                            if (level >= nextLevel) {
                                eor = GetLRFromLevel(nextLevel);
                            } else {
                                eor = GetLRFromLevel(level);
                            }
                            do {
                                int start = limit;
                                level2 = nextLevel;
                                if (limit <= 0 && this.dirProps[start - 1] == 7) {
                                    sor = GetLRFromLevel(GetParaLevelAt(start));
                                } else {
                                    sor = eor;
                                }
                                while (true) {
                                    limit++;
                                    if (limit < this.length || (this.levels[limit] != level2 && (DirPropFlag(this.dirProps[limit]) & MASK_BN_EXPLICIT) == 0)) {
                                    }
                                }
                                if (limit >= this.length) {
                                    nextLevel = this.levels[limit];
                                } else {
                                    nextLevel = GetParaLevelAt(this.length - 1);
                                }
                                if (NoOverride(level2) >= NoOverride(nextLevel)) {
                                    eor = GetLRFromLevel(nextLevel);
                                } else {
                                    eor = GetLRFromLevel(level2);
                                }
                                if ((level2 & LEVEL_OVERRIDE) != 0) {
                                    resolveImplicitLevels(start, limit, sor, eor);
                                } else {
                                    while (true) {
                                        byte[] bArr = this.levels;
                                        int start2 = start + 1;
                                        bArr[start] = (byte) (bArr[start] & LEVEL_DEFAULT_RTL);
                                        if (start2 < limit) {
                                            start = start2;
                                        }
                                    }
                                }
                            } while (limit < this.length);
                        }
                        adjustWSLevels();
                        break;
                    case 1:
                        this.impTabPair = impTab_NUMBERS_SPECIAL;
                        if (embeddingLevels == null) {
                            limit = 0;
                            level = GetParaLevelAt(0);
                            nextLevel = this.levels[0];
                            if (level >= nextLevel) {
                            }
                            do {
                                int start3 = limit;
                                level2 = nextLevel;
                                if (limit <= 0) {
                                    sor = eor;
                                    while (true) {
                                        limit++;
                                        if (limit < this.length) {
                                        }
                                        if (limit >= this.length) {
                                        }
                                        if (NoOverride(level2) >= NoOverride(nextLevel)) {
                                        }
                                        if ((level2 & LEVEL_OVERRIDE) != 0) {
                                        }
                                    }
                                }
                            } while (limit < this.length);
                            adjustWSLevels();
                            break;
                        }
                        break;
                    case 2:
                        this.impTabPair = impTab_GROUP_NUMBERS_WITH_R;
                        if (embeddingLevels == null) {
                        }
                        break;
                    case 3:
                        throw new InternalError("Internal ICU error in setPara");
                    case 4:
                        this.impTabPair = impTab_INVERSE_NUMBERS_AS_L;
                        if (embeddingLevels == null) {
                        }
                        break;
                    case 5:
                        if ((this.reorderingOptions & 1) != 0) {
                            this.impTabPair = impTab_INVERSE_LIKE_DIRECT_WITH_MARKS;
                        } else {
                            this.impTabPair = impTab_INVERSE_LIKE_DIRECT;
                        }
                        if (embeddingLevels == null) {
                        }
                        break;
                    case 6:
                        if ((this.reorderingOptions & 1) != 0) {
                            this.impTabPair = impTab_INVERSE_FOR_NUMBERS_SPECIAL_WITH_MARKS;
                        } else {
                            this.impTabPair = impTab_INVERSE_FOR_NUMBERS_SPECIAL;
                        }
                        if (embeddingLevels == null) {
                        }
                        break;
                    default:
                        if (embeddingLevels == null) {
                        }
                        break;
                }
                break;
        }
        if (this.defaultParaLevel > 0 && (this.reorderingOptions & 1) != 0 && (this.reorderingMode == 5 || this.reorderingMode == 6)) {
            int i = 0;
            while (i < this.paraCount) {
                int last = this.paras_limit[i] - 1;
                if (this.paras_level[i] != 0) {
                    int start4 = i == 0 ? 0 : this.paras_limit[i - 1];
                    int j = last;
                    while (true) {
                        if (j < start4) {
                            break;
                        }
                        byte dirProp = this.dirProps[j];
                        if (dirProp == 0) {
                            if (j < last) {
                                while (this.dirProps[last] == 7) {
                                    last--;
                                }
                            }
                            addPoint(last, 4);
                        } else if ((DirPropFlag(dirProp) & MASK_R_AL) == 0) {
                            j--;
                        }
                    }
                }
                i++;
            }
        }
        if ((this.reorderingOptions & 2) != 0) {
            this.resultLength -= this.controlCount;
        } else {
            this.resultLength += this.insertPoints.size;
        }
        setParaSuccess();
    }

    public void setPara(AttributedCharacterIterator paragraph) {
        byte paraLvl;
        byte level;
        Boolean runDirection = (Boolean) paragraph.getAttribute(TextAttribute.RUN_DIRECTION);
        if (runDirection == null) {
            paraLvl = LEVEL_DEFAULT_LTR;
        } else {
            paraLvl = runDirection.equals(TextAttribute.RUN_DIRECTION_LTR) ? (byte) 0 : (byte) 1;
        }
        byte[] lvls = null;
        int len = paragraph.getEndIndex() - paragraph.getBeginIndex();
        byte[] embeddingLevels = new byte[len];
        char[] txt = new char[len];
        int i = 0;
        char ch = paragraph.first();
        while (ch != 65535) {
            txt[i] = ch;
            Integer embedding = (Integer) paragraph.getAttribute(TextAttribute.BIDI_EMBEDDING);
            if (embedding != null && (level = embedding.byteValue()) != 0) {
                if (level < 0) {
                    lvls = embeddingLevels;
                    embeddingLevels[i] = (byte) ((0 - level) | (-128));
                } else {
                    lvls = embeddingLevels;
                    embeddingLevels[i] = level;
                }
            }
            ch = paragraph.next();
            i++;
        }
        NumericShaper shaper = (NumericShaper) paragraph.getAttribute(TextAttribute.NUMERIC_SHAPING);
        if (shaper != null) {
            shaper.shape(txt, 0, len);
        }
        setPara(txt, paraLvl, lvls);
    }

    public void orderParagraphsLTR(boolean ordarParaLTR) {
        this.orderParagraphsLTR = ordarParaLTR;
    }

    public boolean isOrderParagraphsLTR() {
        return this.orderParagraphsLTR;
    }

    public byte getDirection() {
        verifyValidParaOrLine();
        return this.direction;
    }

    public String getTextAsString() {
        verifyValidParaOrLine();
        return new String(this.text);
    }

    public char[] getText() {
        verifyValidParaOrLine();
        return this.text;
    }

    public int getLength() {
        verifyValidParaOrLine();
        return this.originalLength;
    }

    public int getProcessedLength() {
        verifyValidParaOrLine();
        return this.length;
    }

    public int getResultLength() {
        verifyValidParaOrLine();
        return this.resultLength;
    }

    public byte getParaLevel() {
        verifyValidParaOrLine();
        return this.paraLevel;
    }

    public int countParagraphs() {
        verifyValidParaOrLine();
        return this.paraCount;
    }

    public BidiRun getParagraphByIndex(int paraIndex) {
        int paraStart;
        verifyValidParaOrLine();
        verifyRange(paraIndex, 0, this.paraCount);
        Bidi bidi = this.paraBidi;
        if (paraIndex == 0) {
            paraStart = 0;
        } else {
            paraStart = bidi.paras_limit[paraIndex - 1];
        }
        BidiRun bidiRun = new BidiRun();
        bidiRun.start = paraStart;
        bidiRun.limit = bidi.paras_limit[paraIndex];
        bidiRun.level = GetParaLevelAt(paraStart);
        return bidiRun;
    }

    public BidiRun getParagraph(int charIndex) {
        verifyValidParaOrLine();
        Bidi bidi = this.paraBidi;
        verifyRange(charIndex, 0, bidi.length);
        int paraIndex = 0;
        while (charIndex >= bidi.paras_limit[paraIndex]) {
            paraIndex++;
        }
        return getParagraphByIndex(paraIndex);
    }

    public int getParagraphIndex(int charIndex) {
        verifyValidParaOrLine();
        Bidi bidi = this.paraBidi;
        verifyRange(charIndex, 0, bidi.length);
        int paraIndex = 0;
        while (charIndex >= bidi.paras_limit[paraIndex]) {
            paraIndex++;
        }
        return paraIndex;
    }

    public void setCustomClassifier(BidiClassifier classifier) {
        this.customClassifier = classifier;
    }

    public BidiClassifier getCustomClassifier() {
        return this.customClassifier;
    }

    public int getCustomizedClass(int c) {
        int dir;
        if (this.customClassifier == null || (dir = this.customClassifier.classify(c)) == 23) {
            dir = this.bdp.getClass(c);
        }
        if (dir >= 23) {
            return 10;
        }
        return dir;
    }

    public Bidi setLine(int start, int limit) {
        verifyValidPara();
        verifyRange(start, 0, limit);
        verifyRange(limit, 0, this.length + 1);
        if (getParagraphIndex(start) != getParagraphIndex(limit - 1)) {
            throw new IllegalArgumentException();
        }
        return BidiLine.setLine(this, start, limit);
    }

    public byte getLevelAt(int charIndex) {
        verifyValidParaOrLine();
        verifyRange(charIndex, 0, this.length);
        return BidiLine.getLevelAt(this, charIndex);
    }

    public byte[] getLevels() {
        verifyValidParaOrLine();
        if (this.length <= 0) {
            return new byte[0];
        }
        return BidiLine.getLevels(this);
    }

    public BidiRun getLogicalRun(int logicalPosition) {
        verifyValidParaOrLine();
        verifyRange(logicalPosition, 0, this.length);
        return BidiLine.getLogicalRun(this, logicalPosition);
    }

    public int countRuns() {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        return this.runCount;
    }

    public BidiRun getVisualRun(int runIndex) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(runIndex, 0, this.runCount);
        return BidiLine.getVisualRun(this, runIndex);
    }

    public int getVisualIndex(int logicalIndex) {
        verifyValidParaOrLine();
        verifyRange(logicalIndex, 0, this.length);
        return BidiLine.getVisualIndex(this, logicalIndex);
    }

    public int getLogicalIndex(int visualIndex) {
        verifyValidParaOrLine();
        verifyRange(visualIndex, 0, this.resultLength);
        if (this.insertPoints.size == 0 && this.controlCount == 0) {
            if (this.direction == 0) {
                return visualIndex;
            }
            if (this.direction == 1) {
                return (this.length - visualIndex) - 1;
            }
        }
        BidiLine.getRuns(this);
        return BidiLine.getLogicalIndex(this, visualIndex);
    }

    public int[] getLogicalMap() {
        countRuns();
        if (this.length <= 0) {
            return new int[0];
        }
        return BidiLine.getLogicalMap(this);
    }

    public int[] getVisualMap() {
        countRuns();
        if (this.resultLength <= 0) {
            return new int[0];
        }
        return BidiLine.getVisualMap(this);
    }

    public static int[] reorderLogical(byte[] levels) {
        return BidiLine.reorderLogical(levels);
    }

    public static int[] reorderVisual(byte[] levels) {
        return BidiLine.reorderVisual(levels);
    }

    public static int[] invertMap(int[] srcMap) {
        if (srcMap == null) {
            return null;
        }
        return BidiLine.invertMap(srcMap);
    }

    public Bidi(String paragraph, int flags) {
        this(paragraph.toCharArray(), 0, null, 0, paragraph.length(), flags);
    }

    public Bidi(AttributedCharacterIterator paragraph) {
        this();
        setPara(paragraph);
    }

    public Bidi(char[] text, int textStart, byte[] embeddings, int embStart, int paragraphLength, int flags) {
        byte paraLvl;
        byte[] paraEmbeddings;
        this();
        switch (flags) {
            case 1:
                paraLvl = 1;
                break;
            case 126:
                paraLvl = LEVEL_DEFAULT_LTR;
                break;
            case 127:
                paraLvl = LEVEL_DEFAULT_RTL;
                break;
            default:
                paraLvl = 0;
                break;
        }
        if (embeddings == null) {
            paraEmbeddings = null;
        } else {
            paraEmbeddings = new byte[paragraphLength];
            for (int i = 0; i < paragraphLength; i++) {
                byte lev = embeddings[i + embStart];
                if (lev < 0) {
                    lev = (byte) ((-lev) | (-128));
                } else if (lev == 0) {
                    lev = paraLvl;
                    if (paraLvl > 125) {
                        lev = (byte) (lev & 1);
                    }
                }
                paraEmbeddings[i] = lev;
            }
        }
        if (textStart == 0 && embStart == 0 && paragraphLength == text.length) {
            setPara(text, paraLvl, paraEmbeddings);
            return;
        }
        char[] paraText = new char[paragraphLength];
        System.arraycopy(text, textStart, paraText, 0, paragraphLength);
        setPara(paraText, paraLvl, paraEmbeddings);
    }

    public Bidi createLineBidi(int lineStart, int lineLimit) {
        return setLine(lineStart, lineLimit);
    }

    public boolean isMixed() {
        return (isLeftToRight() || isRightToLeft()) ? false : true;
    }

    public boolean isLeftToRight() {
        return getDirection() == 0 && (this.paraLevel & 1) == 0;
    }

    public boolean isRightToLeft() {
        return getDirection() == 1 && (this.paraLevel & 1) == 1;
    }

    public boolean baseIsLeftToRight() {
        return getParaLevel() == 0;
    }

    public int getBaseLevel() {
        return getParaLevel();
    }

    public int getRunCount() {
        return countRuns();
    }

    void getLogicalToVisualRunsMap() {
        if (this.isGoodLogicalToVisualRunsMap) {
            return;
        }
        int count = countRuns();
        if (this.logicalToVisualRunsMap == null || this.logicalToVisualRunsMap.length < count) {
            this.logicalToVisualRunsMap = new int[count];
        }
        long[] keys = new long[count];
        for (int i = 0; i < count; i++) {
            keys[i] = (((long) this.runs[i].start) << 32) + ((long) i);
        }
        Arrays.sort(keys);
        for (int i2 = 0; i2 < count; i2++) {
            this.logicalToVisualRunsMap[i2] = (int) (keys[i2] & (-1));
        }
        this.isGoodLogicalToVisualRunsMap = true;
    }

    public int getRunLevel(int run) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(run, 0, this.runCount);
        getLogicalToVisualRunsMap();
        return this.runs[this.logicalToVisualRunsMap[run]].level;
    }

    public int getRunStart(int run) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(run, 0, this.runCount);
        getLogicalToVisualRunsMap();
        return this.runs[this.logicalToVisualRunsMap[run]].start;
    }

    public int getRunLimit(int run) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(run, 0, this.runCount);
        getLogicalToVisualRunsMap();
        int idx = this.logicalToVisualRunsMap[run];
        int len = idx == 0 ? this.runs[idx].limit : this.runs[idx].limit - this.runs[idx - 1].limit;
        return this.runs[idx].start + len;
    }

    public static boolean requiresBidi(char[] text, int start, int limit) {
        for (int i = start; i < limit; i++) {
            if (((1 << UCharacter.getDirection(text[i])) & 57378) != 0) {
                return true;
            }
        }
        return false;
    }

    public static void reorderVisually(byte[] levels, int levelStart, Object[] objects, int objectStart, int count) {
        byte[] reorderLevels = new byte[count];
        System.arraycopy(levels, levelStart, reorderLevels, 0, count);
        int[] indexMap = reorderVisual(reorderLevels);
        Object[] temp = new Object[count];
        System.arraycopy(objects, objectStart, temp, 0, count);
        for (int i = 0; i < count; i++) {
            objects[objectStart + i] = temp[indexMap[i]];
        }
    }

    public String writeReordered(int options) {
        verifyValidParaOrLine();
        if (this.length == 0) {
            return "";
        }
        return BidiWriter.writeReordered(this, options);
    }

    public static String writeReverse(String src, int options) {
        if (src == null) {
            throw new IllegalArgumentException();
        }
        if (src.length() > 0) {
            return BidiWriter.writeReverse(src, options);
        }
        return "";
    }
}
