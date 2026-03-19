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

    @Deprecated
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

    static int DirPropFlag(byte b) {
        return 1 << b;
    }

    boolean testDirPropFlagAt(int i, int i2) {
        return (i & DirPropFlag(this.dirProps[i2])) != 0;
    }

    static final int DirPropFlagLR(byte b) {
        return DirPropFlagLR[b & 1];
    }

    static final int DirPropFlagE(byte b) {
        return DirPropFlagE[b & 1];
    }

    static final int DirPropFlagO(byte b) {
        return DirPropFlagO[b & 1];
    }

    static final byte DirFromStrong(byte b) {
        return b == 0 ? (byte) 0 : (byte) 1;
    }

    static final byte NoOverride(byte b) {
        return (byte) (b & LEVEL_DEFAULT_RTL);
    }

    static byte GetLRFromLevel(byte b) {
        return (byte) (b & 1);
    }

    static boolean IsDefaultLevel(byte b) {
        return (b & LEVEL_DEFAULT_LTR) == 126;
    }

    static boolean IsBidiControlChar(int i) {
        return (i & (-4)) == 8204 || (i >= 8234 && i <= 8238) || (i >= 8294 && i <= 8297);
    }

    void verifyValidPara() {
        if (this != this.paraBidi) {
            throw new IllegalStateException();
        }
    }

    void verifyValidParaOrLine() {
        Bidi bidi = this.paraBidi;
        if (this == bidi) {
            return;
        }
        if (bidi == null || bidi != bidi.paraBidi) {
            throw new IllegalStateException();
        }
    }

    void verifyRange(int i, int i2, int i3) {
        if (i < i2 || i >= i3) {
            throw new IllegalArgumentException("Value " + i + " is out of range " + i2 + " to " + i3);
        }
    }

    public Bidi() {
        this(0, 0);
    }

    public Bidi(int i, int i2) {
        this.dirPropsMemory = new byte[1];
        this.levelsMemory = new byte[1];
        this.paras_limit = new int[10];
        this.paras_level = new byte[10];
        this.runsMemory = new BidiRun[0];
        this.simpleRuns = new BidiRun[]{new BidiRun()};
        this.customClassifier = null;
        this.insertPoints = new InsertPoints();
        if (i < 0 || i2 < 0) {
            throw new IllegalArgumentException();
        }
        this.bdp = UBiDiProps.INSTANCE;
        if (i > 0) {
            getInitialDirPropsMemory(i);
            getInitialLevelsMemory(i);
        } else {
            this.mayAllocateText = true;
        }
        if (i2 > 0) {
            if (i2 > 1) {
                getInitialRunsMemory(i2);
                return;
            }
            return;
        }
        this.mayAllocateRuns = true;
    }

    private Object getMemory(String str, Object obj, Class<?> cls, boolean z, int i) {
        int length = Array.getLength(obj);
        if (i == length) {
            return obj;
        }
        if (!z) {
            if (i <= length) {
                return obj;
            }
            throw new OutOfMemoryError("Failed to allocate memory for " + str);
        }
        try {
            return Array.newInstance(cls, i);
        } catch (Exception e) {
            throw new OutOfMemoryError("Failed to allocate memory for " + str);
        }
    }

    private void getDirPropsMemory(boolean z, int i) {
        this.dirPropsMemory = (byte[]) getMemory("DirProps", this.dirPropsMemory, Byte.TYPE, z, i);
    }

    void getDirPropsMemory(int i) {
        getDirPropsMemory(this.mayAllocateText, i);
    }

    private void getLevelsMemory(boolean z, int i) {
        this.levelsMemory = (byte[]) getMemory("Levels", this.levelsMemory, Byte.TYPE, z, i);
    }

    void getLevelsMemory(int i) {
        getLevelsMemory(this.mayAllocateText, i);
    }

    private void getRunsMemory(boolean z, int i) {
        this.runsMemory = (BidiRun[]) getMemory("Runs", this.runsMemory, BidiRun.class, z, i);
    }

    void getRunsMemory(int i) {
        getRunsMemory(this.mayAllocateRuns, i);
    }

    private void getInitialDirPropsMemory(int i) {
        getDirPropsMemory(true, i);
    }

    private void getInitialLevelsMemory(int i) {
        getLevelsMemory(true, i);
    }

    private void getInitialRunsMemory(int i) {
        getRunsMemory(true, i);
    }

    public void setInverse(boolean z) {
        this.isInverse = z;
        this.reorderingMode = z ? 4 : 0;
    }

    public boolean isInverse() {
        return this.isInverse;
    }

    public void setReorderingMode(int i) {
        if (i < 0 || i >= 7) {
            return;
        }
        this.reorderingMode = i;
        this.isInverse = i == 4;
    }

    public int getReorderingMode() {
        return this.reorderingMode;
    }

    public void setReorderingOptions(int i) {
        if ((i & 2) != 0) {
            this.reorderingOptions = i & (-2);
        } else {
            this.reorderingOptions = i;
        }
    }

    public int getReorderingOptions() {
        return this.reorderingOptions;
    }

    public static byte getBaseDirection(CharSequence charSequence) {
        if (charSequence == null || charSequence.length() == 0) {
            return (byte) 3;
        }
        int length = charSequence.length();
        int iOffsetByCodePoints = 0;
        while (iOffsetByCodePoints < length) {
            byte directionality = UCharacter.getDirectionality(UCharacter.codePointAt(charSequence, iOffsetByCodePoints));
            if (directionality == 0) {
                return (byte) 0;
            }
            if (directionality == 1 || directionality == 13) {
                return (byte) 1;
            }
            iOffsetByCodePoints = UCharacter.offsetByCodePoints(charSequence, iOffsetByCodePoints, 1);
        }
        return (byte) 3;
    }

    private byte firstL_R_AL() {
        int iCharCount = 0;
        byte b = 10;
        while (iCharCount < this.prologue.length()) {
            int iCodePointAt = this.prologue.codePointAt(iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            byte customizedClass = (byte) getCustomizedClass(iCodePointAt);
            if (b == 10) {
                if (customizedClass == 0 || customizedClass == 1 || customizedClass == 13) {
                    b = customizedClass;
                }
            } else if (customizedClass == 7) {
                b = 10;
            }
        }
        return b;
    }

    private void checkParaCount() {
        int i = this.paraCount;
        if (i <= this.paras_level.length) {
            return;
        }
        int length = this.paras_level.length;
        int[] iArr = this.paras_limit;
        byte[] bArr = this.paras_level;
        int i2 = i * 2;
        try {
            this.paras_limit = new int[i2];
            this.paras_level = new byte[i2];
            System.arraycopy(iArr, 0, this.paras_limit, 0, length);
            System.arraycopy(bArr, 0, this.paras_level, 0, length);
        } catch (Exception e) {
            throw new OutOfMemoryError("Failed to allocate memory for paras");
        }
    }

    private void getDirProps() {
        byte b;
        byte b2;
        byte b3;
        boolean z;
        byte b4;
        int i;
        char c;
        byte b5;
        byte bFirstL_R_AL;
        int i2 = 0;
        this.flags = 0;
        boolean zIsDefaultLevel = IsDefaultLevel(this.paraLevel);
        boolean z2 = zIsDefaultLevel && (this.reorderingMode == 5 || this.reorderingMode == 6);
        this.lastArabicPos = -1;
        boolean z3 = (this.reorderingOptions & 2) != 0;
        int[] iArr = new int[126];
        byte[] bArr = new byte[126];
        if ((this.reorderingOptions & 4) != 0) {
            this.length = 0;
        }
        byte b6 = (byte) (this.paraLevel & 1);
        if (zIsDefaultLevel) {
            this.paras_level[0] = b6;
            if (this.prologue == null || (bFirstL_R_AL = firstL_R_AL()) == 10) {
                b = 1;
            } else {
                if (bFirstL_R_AL == 0) {
                    this.paras_level[0] = 0;
                } else {
                    this.paras_level[0] = 1;
                }
                b = 0;
            }
            b2 = b6;
        } else {
            this.paras_level[0] = this.paraLevel;
            b = 0;
            b2 = 10;
        }
        int i3 = 0;
        byte b7 = b;
        byte b8 = b2;
        int i4 = 0;
        int i5 = -1;
        while (i4 < this.originalLength) {
            int iCharAt = UTF16.charAt(this.text, i2, this.originalLength, i4);
            int charCount = UTF16.getCharCount(iCharAt) + i4;
            int i6 = charCount - 1;
            byte customizedClass = (byte) getCustomizedClass(iCharAt);
            this.flags |= DirPropFlag(customizedClass);
            this.dirProps[i6] = customizedClass;
            if (i6 > i4) {
                b4 = b6;
                this.flags |= DirPropFlag((byte) 18);
                int i7 = i6;
                while (true) {
                    z = zIsDefaultLevel;
                    i = -1;
                    i7--;
                    this.dirProps[i7] = 18;
                    if (i7 <= i4) {
                        break;
                    } else {
                        zIsDefaultLevel = z;
                    }
                }
            } else {
                z = zIsDefaultLevel;
                b4 = b6;
                i = -1;
            }
            if (z3 && IsBidiControlChar(iCharAt)) {
                i3++;
            }
            if (customizedClass == 0) {
                if (b7 == 1) {
                    this.paras_level[this.paraCount - 1] = 0;
                    b7 = 0;
                } else if (b7 == 2) {
                    if (i5 <= 125) {
                        this.flags |= DirPropFlag((byte) 20);
                    }
                    b7 = 3;
                }
                i4 = charCount;
                b6 = b4;
                zIsDefaultLevel = z;
                i2 = 0;
                b8 = 0;
            } else if (customizedClass == 1 || customizedClass == 13) {
                c = LF;
                if (b7 == 1) {
                    this.paras_level[this.paraCount - 1] = 1;
                    b7 = 0;
                } else if (b7 == 2) {
                    if (i5 <= 125) {
                        this.dirProps[iArr[i5]] = 21;
                        this.flags = DirPropFlag((byte) 21) | this.flags;
                    }
                    b7 = 3;
                }
                if (customizedClass == 13) {
                    this.lastArabicPos = i6;
                }
                i4 = charCount;
                b6 = b4;
                zIsDefaultLevel = z;
                i2 = 0;
                b8 = 1;
            } else if (customizedClass < 19 || customizedClass > 21) {
                if (customizedClass == 22) {
                    if (b7 == 2 && i5 <= 125) {
                        this.flags |= DirPropFlag((byte) 20);
                    }
                    if (i5 >= 0) {
                        if (i5 <= 125) {
                            b7 = bArr[i5];
                        }
                        i5--;
                        i4 = charCount;
                        b6 = b4;
                        zIsDefaultLevel = z;
                        i2 = 0;
                    }
                } else {
                    if (customizedClass == 7) {
                        if (charCount >= this.originalLength || iCharAt != 13) {
                            c = LF;
                        } else {
                            char c2 = this.text[charCount];
                            c = LF;
                            if (c2 == '\n') {
                                b5 = b8;
                            }
                        }
                        this.paras_limit[this.paraCount - 1] = charCount;
                        if (z2) {
                            b5 = b8;
                            if (b5 == 1) {
                                this.paras_level[this.paraCount - 1] = 1;
                            }
                        } else {
                            b5 = b8;
                        }
                        if ((this.reorderingOptions & 4) != 0) {
                            this.length = charCount;
                            this.controlCount = i3;
                        }
                        if (charCount < this.originalLength) {
                            this.paraCount++;
                            checkParaCount();
                            if (z) {
                                this.paras_level[this.paraCount - 1] = b4;
                                b7 = 1;
                                b8 = b4;
                            } else {
                                this.paras_level[this.paraCount - 1] = this.paraLevel;
                                b8 = b5;
                                b7 = 0;
                            }
                            i4 = charCount;
                            i5 = i;
                        }
                        b6 = b4;
                        zIsDefaultLevel = z;
                        i2 = 0;
                    }
                    b8 = b5;
                    i4 = charCount;
                    b6 = b4;
                    zIsDefaultLevel = z;
                    i2 = 0;
                }
                b5 = b8;
                c = LF;
                b8 = b5;
                i4 = charCount;
                b6 = b4;
                zIsDefaultLevel = z;
                i2 = 0;
            } else {
                i5++;
                if (i5 <= 125) {
                    iArr[i5] = i6;
                    bArr[i5] = b7;
                }
                if (customizedClass == 19) {
                    this.dirProps[i6] = 20;
                    i4 = charCount;
                    b6 = b4;
                    zIsDefaultLevel = z;
                    i2 = 0;
                    b7 = 2;
                } else {
                    i4 = charCount;
                    b6 = b4;
                    zIsDefaultLevel = z;
                    i2 = 0;
                    b7 = 3;
                }
            }
        }
        boolean z4 = zIsDefaultLevel;
        byte b9 = b8;
        if (i5 > 125) {
            i5 = 125;
            b7 = 2;
        }
        while (true) {
            if (i5 < 0) {
                break;
            }
            if (b7 == 2) {
                this.flags |= DirPropFlag((byte) 20);
                break;
            } else {
                b7 = bArr[i5];
                i5--;
            }
        }
        if ((this.reorderingOptions & 4) == 0) {
            b3 = 1;
            this.paras_limit[this.paraCount - 1] = this.originalLength;
            this.controlCount = i3;
        } else if (this.length < this.originalLength) {
            b3 = 1;
            this.paraCount--;
        } else {
            b3 = 1;
        }
        if (z2 && b9 == b3) {
            this.paras_level[this.paraCount - b3] = b3;
        }
        if (z4) {
            this.paraLevel = this.paras_level[0];
        }
        for (int i8 = 0; i8 < this.paraCount; i8++) {
            this.flags |= DirPropFlagLR(this.paras_level[i8]);
        }
        if (!this.orderParagraphsLTR || (this.flags & DirPropFlag((byte) 7)) == 0) {
            return;
        }
        this.flags |= DirPropFlag((byte) 0);
    }

    byte GetParaLevelAt(int i) {
        if (this.defaultParaLevel == 0 || i < this.paras_limit[0]) {
            return this.paraLevel;
        }
        int i2 = 1;
        while (i2 < this.paraCount && i >= this.paras_limit[i2]) {
            i2++;
        }
        if (i2 >= this.paraCount) {
            i2 = this.paraCount - 1;
        }
        return this.paras_level[i2];
    }

    private void bracketInit(BracketData bracketData) {
        bracketData.isoRunLast = 0;
        bracketData.isoRuns[0] = new IsoRun();
        bracketData.isoRuns[0].start = (short) 0;
        bracketData.isoRuns[0].limit = (short) 0;
        bracketData.isoRuns[0].level = GetParaLevelAt(0);
        IsoRun isoRun = bracketData.isoRuns[0];
        IsoRun isoRun2 = bracketData.isoRuns[0];
        IsoRun isoRun3 = bracketData.isoRuns[0];
        byte bGetParaLevelAt = (byte) (GetParaLevelAt(0) & 1);
        isoRun3.contextDir = bGetParaLevelAt;
        isoRun2.lastBase = bGetParaLevelAt;
        isoRun.lastStrong = bGetParaLevelAt;
        bracketData.isoRuns[0].contextPos = 0;
        bracketData.openings = new Opening[20];
        bracketData.isNumbersSpecial = this.reorderingMode == 1 || this.reorderingMode == 6;
    }

    private void bracketProcessB(BracketData bracketData, byte b) {
        bracketData.isoRunLast = 0;
        bracketData.isoRuns[0].limit = (short) 0;
        bracketData.isoRuns[0].level = b;
        IsoRun isoRun = bracketData.isoRuns[0];
        IsoRun isoRun2 = bracketData.isoRuns[0];
        byte b2 = (byte) (b & 1);
        bracketData.isoRuns[0].contextDir = b2;
        isoRun2.lastBase = b2;
        isoRun.lastStrong = b2;
        bracketData.isoRuns[0].contextPos = 0;
    }

    private void bracketProcessBoundary(BracketData bracketData, int i, byte b, byte b2) {
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        if ((DirPropFlag(this.dirProps[i]) & MASK_ISO) != 0) {
            return;
        }
        if (NoOverride(b2) > NoOverride(b)) {
            b = b2;
        }
        isoRun.limit = isoRun.start;
        isoRun.level = b2;
        byte b3 = (byte) (b & 1);
        isoRun.contextDir = b3;
        isoRun.lastBase = b3;
        isoRun.lastStrong = b3;
        isoRun.contextPos = i;
    }

    private void bracketProcessLRI_RLI(BracketData bracketData, byte b) {
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        isoRun.lastBase = (byte) 10;
        short s = isoRun.limit;
        bracketData.isoRunLast++;
        IsoRun isoRun2 = bracketData.isoRuns[bracketData.isoRunLast];
        if (isoRun2 == null) {
            IsoRun[] isoRunArr = bracketData.isoRuns;
            int i = bracketData.isoRunLast;
            IsoRun isoRun3 = new IsoRun();
            isoRunArr[i] = isoRun3;
            isoRun2 = isoRun3;
        }
        isoRun2.limit = s;
        isoRun2.start = s;
        isoRun2.level = b;
        byte b2 = (byte) (b & 1);
        isoRun2.contextDir = b2;
        isoRun2.lastBase = b2;
        isoRun2.lastStrong = b2;
        isoRun2.contextPos = 0;
    }

    private void bracketProcessPDI(BracketData bracketData) {
        bracketData.isoRunLast--;
        bracketData.isoRuns[bracketData.isoRunLast].lastBase = (byte) 10;
    }

    private void bracketAddOpening(BracketData bracketData, char c, int i) {
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        if (isoRun.limit >= bracketData.openings.length) {
            Opening[] openingArr = bracketData.openings;
            try {
                int length = bracketData.openings.length;
                bracketData.openings = new Opening[length * 2];
                System.arraycopy(openingArr, 0, bracketData.openings, 0, length);
            } catch (Exception e) {
                throw new OutOfMemoryError("Failed to allocate memory for openings");
            }
        }
        Opening opening = bracketData.openings[isoRun.limit];
        if (opening == null) {
            Opening[] openingArr2 = bracketData.openings;
            short s = isoRun.limit;
            Opening opening2 = new Opening();
            openingArr2[s] = opening2;
            opening = opening2;
        }
        opening.position = i;
        opening.match = c;
        opening.contextDir = isoRun.contextDir;
        opening.contextPos = isoRun.contextPos;
        opening.flags = (short) 0;
        isoRun.limit = (short) (isoRun.limit + 1);
    }

    private void fixN0c(BracketData bracketData, int i, int i2, byte b) {
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        while (true) {
            i++;
            if (i < isoRun.limit) {
                Opening opening = bracketData.openings[i];
                if (opening.match < 0) {
                    if (i2 >= opening.contextPos) {
                        if (i2 >= opening.position) {
                            continue;
                        } else if (b != opening.contextDir) {
                            int i3 = opening.position;
                            this.dirProps[i3] = b;
                            int i4 = -opening.match;
                            this.dirProps[i4] = b;
                            opening.match = 0;
                            fixN0c(bracketData, i, i3, b);
                            fixN0c(bracketData, i, i4, b);
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            } else {
                return;
            }
        }
    }

    private byte bracketProcessClosing(BracketData bracketData, int i, int i2) {
        boolean z;
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        Opening opening = bracketData.openings[i];
        byte b = (byte) (isoRun.level & 1);
        if ((b != 0 || (opening.flags & FOUND_L) <= 0) && (b != 1 || (opening.flags & FOUND_R) <= 0)) {
            if ((opening.flags & (FOUND_L | FOUND_R)) != 0) {
                z = i == isoRun.start;
                if (b != opening.contextDir) {
                    b = opening.contextDir;
                }
            } else {
                isoRun.limit = (short) i;
                return (byte) 10;
            }
        } else {
            z = true;
        }
        this.dirProps[opening.position] = b;
        this.dirProps[i2] = b;
        fixN0c(bracketData, i, opening.position, b);
        if (z) {
            isoRun.limit = (short) i;
            while (isoRun.limit > isoRun.start && bracketData.openings[isoRun.limit - 1].position == opening.position) {
                isoRun.limit = (short) (isoRun.limit - 1);
            }
        } else {
            opening.match = -i2;
            for (int i3 = i - 1; i3 >= isoRun.start && bracketData.openings[i3].position == opening.position; i3--) {
                bracketData.openings[i3].match = 0;
            }
            for (int i4 = i + 1; i4 < isoRun.limit; i4++) {
                Opening opening2 = bracketData.openings[i4];
                if (opening2.position >= i2) {
                    break;
                }
                if (opening2.match > 0) {
                    opening2.match = 0;
                }
            }
        }
        return b;
    }

    private void bracketProcessChar(BracketData bracketData, int i) {
        IsoRun isoRun = bracketData.isoRuns[bracketData.isoRunLast];
        byte b = this.dirProps[i];
        byte bDirFromStrong = 0;
        if (b == 10) {
            char c = this.text[i];
            int i2 = isoRun.limit - 1;
            while (true) {
                if (i2 < isoRun.start) {
                    break;
                }
                if (bracketData.openings[i2].match != c) {
                    i2--;
                } else {
                    byte bBracketProcessClosing = bracketProcessClosing(bracketData, i2, i);
                    if (bBracketProcessClosing != 10) {
                        isoRun.lastBase = (byte) 10;
                        isoRun.contextDir = bBracketProcessClosing;
                        isoRun.contextPos = i;
                        byte b2 = this.levels[i];
                        if ((b2 & LEVEL_OVERRIDE) != 0) {
                            byte b3 = (byte) (b2 & 1);
                            isoRun.lastStrong = b3;
                            short sDirPropFlag = (short) DirPropFlag(b3);
                            for (int i3 = isoRun.start; i3 < i2; i3++) {
                                Opening opening = bracketData.openings[i3];
                                opening.flags = (short) (opening.flags | sDirPropFlag);
                            }
                            byte[] bArr = this.levels;
                            bArr[i] = (byte) (bArr[i] & LEVEL_DEFAULT_RTL);
                        }
                        byte[] bArr2 = this.levels;
                        int i4 = bracketData.openings[i2].position;
                        bArr2[i4] = (byte) (bArr2[i4] & LEVEL_DEFAULT_RTL);
                        return;
                    }
                    c = 0;
                }
            }
        }
        byte b4 = this.levels[i];
        if ((b4 & LEVEL_OVERRIDE) != 0) {
            bDirFromStrong = (byte) (b4 & 1);
            if (b != 8 && b != 9 && b != 10) {
                this.dirProps[i] = bDirFromStrong;
            }
            isoRun.lastBase = bDirFromStrong;
            isoRun.lastStrong = bDirFromStrong;
            isoRun.contextDir = bDirFromStrong;
            isoRun.contextPos = i;
        } else if (b <= 1 || b == 13) {
            bDirFromStrong = DirFromStrong(b);
            isoRun.lastBase = b;
            isoRun.lastStrong = b;
            isoRun.contextDir = bDirFromStrong;
            isoRun.contextPos = i;
        } else if (b == 2) {
            isoRun.lastBase = (byte) 2;
            if (isoRun.lastStrong == 0) {
                if (!bracketData.isNumbersSpecial) {
                    this.dirProps[i] = 23;
                }
                isoRun.contextDir = (byte) 0;
                isoRun.contextPos = i;
            } else {
                if (isoRun.lastStrong == 13) {
                    this.dirProps[i] = 5;
                } else {
                    this.dirProps[i] = 24;
                }
                isoRun.contextDir = (byte) 1;
                isoRun.contextPos = i;
                bDirFromStrong = 1;
            }
        } else if (b == 5) {
            isoRun.lastBase = (byte) 5;
            isoRun.contextDir = (byte) 1;
            isoRun.contextPos = i;
            bDirFromStrong = 1;
        } else {
            if (b == 17) {
                b = isoRun.lastBase;
                if (b == 10) {
                    this.dirProps[i] = b;
                }
            } else {
                isoRun.lastBase = b;
            }
            bDirFromStrong = b;
        }
        if (bDirFromStrong <= 1 || bDirFromStrong == 13) {
            short sDirPropFlag2 = (short) DirPropFlag(DirFromStrong(bDirFromStrong));
            for (int i5 = isoRun.start; i5 < isoRun.limit; i5++) {
                if (i > bracketData.openings[i5].position) {
                    Opening opening2 = bracketData.openings[i5];
                    opening2.flags = (short) (opening2.flags | sDirPropFlag2);
                }
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
        char c;
        byte bGetParaLevelAt = GetParaLevelAt(0);
        this.isolateCount = 0;
        byte bDirectionFromFlags = directionFromFlags();
        if (bDirectionFromFlags != 2) {
            return bDirectionFromFlags;
        }
        if (this.reorderingMode > 1) {
            int i = 0;
            while (i < this.paraCount) {
                int i2 = this.paras_limit[i];
                byte b = this.paras_level[i];
                for (int i3 = i == 0 ? 0 : this.paras_limit[i - 1]; i3 < i2; i3++) {
                    this.levels[i3] = b;
                }
                i++;
            }
            return bDirectionFromFlags;
        }
        byte b2 = 10;
        if ((this.flags & (MASK_EXPLICIT | MASK_ISO)) == 0) {
            BracketData bracketData = new BracketData();
            bracketInit(bracketData);
            int i4 = 0;
            while (i4 < this.paraCount) {
                int i5 = this.paras_limit[i4];
                byte b3 = this.paras_level[i4];
                for (int i6 = i4 == 0 ? 0 : this.paras_limit[i4 - 1]; i6 < i5; i6++) {
                    this.levels[i6] = b3;
                    byte b4 = this.dirProps[i6];
                    if (b4 != 18) {
                        if (b4 == 7) {
                            int i7 = i6 + 1;
                            if (i7 < this.length && (this.text[i6] != '\r' || this.text[i7] != '\n')) {
                                bracketProcessB(bracketData, b3);
                            }
                        } else {
                            bracketProcessChar(bracketData, i6);
                        }
                    }
                }
                i4++;
            }
            return bDirectionFromFlags;
        }
        short[] sArr = new short[127];
        BracketData bracketData2 = new BracketData();
        bracketInit(bracketData2);
        sArr[0] = bGetParaLevelAt;
        this.flags = 0;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        int i12 = 0;
        byte b5 = bGetParaLevelAt;
        byte b6 = b5;
        for (int i13 = 0; i13 < this.length; i13++) {
            byte b7 = this.dirProps[i13];
            switch (b7) {
                case 7:
                    this.flags |= DirPropFlag((byte) 7);
                    this.levels[i13] = GetParaLevelAt(i13);
                    int i14 = i13 + 1;
                    if (i14 < this.length) {
                        char c2 = this.text[i13];
                        c = CR;
                        if (c2 == '\r') {
                            b2 = 10;
                            if (this.text[i14] == '\n') {
                            }
                        } else {
                            b2 = 10;
                        }
                        byte bGetParaLevelAt2 = GetParaLevelAt(i14);
                        sArr[0] = bGetParaLevelAt2;
                        bracketProcessB(bracketData2, bGetParaLevelAt2);
                        b5 = bGetParaLevelAt2;
                        b6 = b5;
                        i8 = 0;
                        i10 = 0;
                        i11 = 0;
                        i12 = 0;
                    } else {
                        c = CR;
                        b2 = 10;
                    }
                    break;
                case 8:
                case 9:
                case 10:
                case 13:
                case 17:
                case 19:
                default:
                    c = CR;
                    if (NoOverride(b5) != NoOverride(b6)) {
                        bracketProcessBoundary(bracketData2, i9, b6, b5);
                        this.flags |= DirPropFlagMultiRuns;
                        if ((b5 & LEVEL_OVERRIDE) != 0) {
                            this.flags |= DirPropFlagO(b5);
                        } else {
                            this.flags |= DirPropFlagE(b5);
                        }
                    }
                    this.levels[i13] = b5;
                    bracketProcessChar(bracketData2, i13);
                    this.flags |= DirPropFlag(this.dirProps[i13]);
                    b6 = b5;
                    break;
                case 11:
                case 12:
                case 14:
                case 15:
                    this.flags |= DirPropFlag((byte) 18);
                    this.levels[i13] = b6;
                    byte bNoOverride = (b7 == 11 || b7 == 12) ? (byte) ((b5 + 2) & 126) : (byte) ((NoOverride(b5) + 1) | 1);
                    if (bNoOverride <= 125 && i8 == 0 && i10 == 0) {
                        if (b7 == 12 || b7 == 15) {
                            bNoOverride = (byte) (bNoOverride | LEVEL_OVERRIDE);
                        }
                        i11++;
                        sArr[i11] = bNoOverride;
                        i9 = i13;
                        b5 = bNoOverride;
                    } else if (i8 == 0) {
                        i10++;
                    }
                    c = CR;
                    b2 = 10;
                    break;
                case 16:
                    this.flags |= DirPropFlag((byte) 18);
                    this.levels[i13] = b6;
                    if (i8 <= 0) {
                        if (i10 > 0) {
                            i10--;
                        } else if (i11 > 0 && sArr[i11] < 256) {
                            i11--;
                            b5 = (byte) sArr[i11];
                            i9 = i13;
                        }
                    }
                    c = CR;
                    b2 = 10;
                    break;
                case 18:
                    this.levels[i13] = b6;
                    this.flags |= DirPropFlag((byte) 18);
                    c = CR;
                    b2 = 10;
                    break;
                case 20:
                case 21:
                    this.flags |= DirPropFlag(b2) | DirPropFlagLR(b5);
                    this.levels[i13] = NoOverride(b5);
                    if (NoOverride(b5) != NoOverride(b6)) {
                        bracketProcessBoundary(bracketData2, i9, b6, b5);
                        this.flags |= DirPropFlagMultiRuns;
                    }
                    byte bNoOverride2 = b7 == 20 ? (byte) ((b5 + 2) & 126) : (byte) ((NoOverride(b5) + 1) | 1);
                    if (bNoOverride2 <= 125 && i8 == 0 && i10 == 0) {
                        this.flags = DirPropFlag(b7) | this.flags;
                        int i15 = i12 + 1;
                        if (i15 > this.isolateCount) {
                            this.isolateCount = i15;
                        }
                        i11++;
                        sArr[i11] = (short) (bNoOverride2 + 256);
                        bracketProcessLRI_RLI(bracketData2, bNoOverride2);
                        i12 = i15;
                        i9 = i13;
                        b6 = b5;
                        b2 = 10;
                        b5 = bNoOverride2;
                        c = CR;
                    } else {
                        this.dirProps[i13] = 9;
                        i8++;
                        b6 = b5;
                        c = CR;
                        b2 = 10;
                    }
                    break;
                case 22:
                    if (NoOverride(b5) != NoOverride(b6)) {
                        bracketProcessBoundary(bracketData2, i9, b6, b5);
                        this.flags |= DirPropFlagMultiRuns;
                    }
                    if (i8 > 0) {
                        i8--;
                        this.dirProps[i13] = 9;
                    } else if (i12 > 0) {
                        this.flags |= DirPropFlag((byte) 22);
                        while (sArr[i11] < 256) {
                            i11--;
                        }
                        i11--;
                        i12--;
                        bracketProcessPDI(bracketData2);
                        i9 = i13;
                        i10 = 0;
                    } else {
                        this.dirProps[i13] = 9;
                    }
                    byte b8 = (byte) (sArr[i11] & (-257));
                    this.flags |= DirPropFlag(b2) | DirPropFlagLR(b8);
                    this.levels[i13] = NoOverride(b8);
                    b5 = b8;
                    b6 = b5;
                    c = CR;
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
        int i;
        this.flags = 0;
        this.isolateCount = 0;
        int i2 = 0;
        int i3 = this.paras_limit[0];
        byte b = this.paraLevel;
        int i4 = 0;
        for (int i5 = 0; i5 < this.length; i5++) {
            byte b2 = this.levels[i5];
            byte b3 = this.dirProps[i5];
            if (b3 == 20 || b3 == 21) {
                i4++;
                if (i4 > this.isolateCount) {
                    this.isolateCount = i4;
                }
            } else if (b3 == 22) {
                i4--;
            } else if (b3 == 7) {
                i4 = 0;
            }
            if (this.defaultParaLevel != 0 && i5 == i3 && (i = i2 + 1) < this.paraCount) {
                b = this.paras_level[i];
                i3 = this.paras_limit[i];
                i2 = i;
            }
            int i6 = b2 & LEVEL_OVERRIDE;
            byte b4 = (byte) (b2 & LEVEL_DEFAULT_RTL);
            if (b4 < b || 125 < b4) {
                if (b4 == 0) {
                    if (b3 != 7) {
                        this.levels[i5] = (byte) (b | i6);
                        b4 = b;
                    }
                } else {
                    throw new IllegalArgumentException("level " + ((int) b4) + " out of bounds at " + i5);
                }
            }
            if (i6 != 0) {
                this.flags = DirPropFlagO(b4) | this.flags;
            } else {
                this.flags = DirPropFlagE(b4) | DirPropFlag(b3) | this.flags;
            }
        }
        if ((this.flags & MASK_EMBEDDING) != 0) {
            this.flags |= DirPropFlagLR(this.paraLevel);
        }
        return directionFromFlags();
    }

    private static short GetStateProps(short s) {
        return (short) (s & 31);
    }

    private static short GetActionProps(short s) {
        return (short) (s >> 5);
    }

    private static short GetState(byte b) {
        return (short) (b & 15);
    }

    private static short GetAction(byte b) {
        return (short) (b >> 4);
    }

    private static class ImpTabPair {
        short[][] impact;
        byte[][][] imptab;

        ImpTabPair(byte[][] bArr, byte[][] bArr2, short[] sArr, short[] sArr2) {
            this.imptab = new byte[][][]{bArr, bArr2};
            this.impact = new short[][]{sArr, sArr2};
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

        private LevState() {
        }
    }

    private void addPoint(int i, int i2) {
        Point point = new Point();
        int length = this.insertPoints.points.length;
        if (length == 0) {
            this.insertPoints.points = new Point[10];
            length = 10;
        }
        if (this.insertPoints.size >= length) {
            Point[] pointArr = this.insertPoints.points;
            this.insertPoints.points = new Point[length * 2];
            System.arraycopy(pointArr, 0, this.insertPoints.points, 0, length);
        }
        point.pos = i;
        point.flag = i2;
        this.insertPoints.points[this.insertPoints.size] = point;
        this.insertPoints.size++;
    }

    private void setLevelsOutsideIsolates(int i, int i2, byte b) {
        int i3 = 0;
        while (i < i2) {
            byte b2 = this.dirProps[i];
            if (b2 == 22) {
                i3--;
            }
            if (i3 == 0) {
                this.levels[i] = b;
            }
            if (b2 == 20 || b2 == 21) {
                i3++;
            }
            i++;
        }
    }

    private void processPropertySeq(LevState levState, short s, int i, int i2) {
        int i3;
        int i4;
        byte[][] bArr = levState.impTab;
        short[] sArr = levState.impAct;
        short s2 = levState.state;
        byte b = bArr[s2][s];
        levState.state = GetState(b);
        short s3 = sArr[GetAction(b)];
        byte b2 = bArr[levState.state][7];
        if (s3 != 0) {
            switch (s3) {
                case 1:
                    levState.startON = i;
                    i3 = i;
                    break;
                case 2:
                    i3 = levState.startON;
                    break;
                case 3:
                    setLevelsOutsideIsolates(levState.startON, i, (byte) (levState.runLevel + 1));
                    i3 = i;
                    break;
                case 4:
                    setLevelsOutsideIsolates(levState.startON, i, (byte) (levState.runLevel + 2));
                    i3 = i;
                    break;
                case 5:
                    if (levState.startL2EN >= 0) {
                        addPoint(levState.startL2EN, 1);
                    }
                    levState.startL2EN = -1;
                    if (this.insertPoints.points.length == 0 || this.insertPoints.size <= this.insertPoints.confirmed) {
                        levState.lastStrongRTL = -1;
                        if ((bArr[s2][7] & 1) != 0 && levState.startON > 0) {
                            i4 = levState.startON;
                        } else {
                            i4 = i;
                        }
                        if (s == 5) {
                            addPoint(i, 1);
                            this.insertPoints.confirmed = this.insertPoints.size;
                        }
                        i3 = i4;
                    } else {
                        for (int i5 = levState.lastStrongRTL + 1; i5 < i; i5++) {
                            this.levels[i5] = (byte) ((this.levels[i5] - 2) & (-2));
                        }
                        this.insertPoints.confirmed = this.insertPoints.size;
                        levState.lastStrongRTL = -1;
                        if (s == 5) {
                            addPoint(i, 1);
                            this.insertPoints.confirmed = this.insertPoints.size;
                        }
                        i3 = i;
                    }
                    break;
                case 6:
                    if (this.insertPoints.points.length > 0) {
                        this.insertPoints.size = this.insertPoints.confirmed;
                    }
                    levState.startON = -1;
                    levState.startL2EN = -1;
                    levState.lastStrongRTL = i2 - 1;
                    i3 = i;
                    break;
                case 7:
                    if (s == 3 && this.dirProps[i] == 5 && this.reorderingMode != 6) {
                        if (levState.startL2EN == -1) {
                            levState.lastStrongRTL = i2 - 1;
                        } else {
                            if (levState.startL2EN >= 0) {
                                addPoint(levState.startL2EN, 1);
                                levState.startL2EN = -2;
                            }
                            addPoint(i, 1);
                        }
                    } else if (levState.startL2EN == -1) {
                        levState.startL2EN = i;
                    }
                    i3 = i;
                    break;
                case 8:
                    levState.lastStrongRTL = i2 - 1;
                    levState.startON = -1;
                    i3 = i;
                    break;
                case 9:
                    int i6 = i - 1;
                    while (i6 >= 0 && (this.levels[i6] & 1) == 0) {
                        i6--;
                    }
                    if (i6 >= 0) {
                        addPoint(i6, 4);
                        this.insertPoints.confirmed = this.insertPoints.size;
                    }
                    levState.startON = i;
                    i3 = i;
                    break;
                case 10:
                    addPoint(i, 1);
                    addPoint(i, 2);
                    i3 = i;
                    break;
                case 11:
                    this.insertPoints.size = this.insertPoints.confirmed;
                    if (s == 5) {
                        addPoint(i, 4);
                        this.insertPoints.confirmed = this.insertPoints.size;
                    }
                    i3 = i;
                    break;
                case 12:
                    byte b3 = (byte) (levState.runLevel + b2);
                    for (int i7 = levState.startON; i7 < i; i7++) {
                        if (this.levels[i7] < b3) {
                            this.levels[i7] = b3;
                        }
                    }
                    this.insertPoints.confirmed = this.insertPoints.size;
                    levState.startON = i;
                    i3 = i;
                    break;
                case 13:
                    byte b4 = levState.runLevel;
                    int i8 = i - 1;
                    while (i8 >= levState.startON) {
                        int i9 = b4 + 3;
                        if (this.levels[i8] == i9) {
                            while (this.levels[i8] == i9) {
                                byte[] bArr2 = this.levels;
                                bArr2[i8] = (byte) (bArr2[i8] - 2);
                                i8--;
                            }
                            while (this.levels[i8] == b4) {
                                i8--;
                            }
                        }
                        if (this.levels[i8] == b4 + 2) {
                            this.levels[i8] = b4;
                        } else {
                            this.levels[i8] = (byte) (b4 + 1);
                        }
                        i8--;
                    }
                    i3 = i;
                    break;
                case 14:
                    byte b5 = (byte) (levState.runLevel + 1);
                    for (int i10 = i - 1; i10 >= levState.startON; i10--) {
                        if (this.levels[i10] > b5) {
                            byte[] bArr3 = this.levels;
                            bArr3[i10] = (byte) (bArr3[i10] - 2);
                        }
                    }
                    i3 = i;
                    break;
                default:
                    throw new IllegalStateException("Internal ICU error in processPropertySeq");
            }
        } else {
            i3 = i;
        }
        if (b2 != 0 || i3 < i) {
            byte b6 = (byte) (levState.runLevel + b2);
            if (i3 >= levState.runStart) {
                while (i3 < i2) {
                    this.levels[i3] = b6;
                    i3++;
                }
                return;
            }
            setLevelsOutsideIsolates(i3, i2, b6);
        }
    }

    private byte lastL_R_AL() {
        int length = this.prologue.length();
        while (length > 0) {
            int iCodePointBefore = this.prologue.codePointBefore(length);
            length -= Character.charCount(iCodePointBefore);
            byte customizedClass = (byte) getCustomizedClass(iCodePointBefore);
            if (customizedClass == 0) {
                return (byte) 0;
            }
            if (customizedClass == 1 || customizedClass == 13) {
                return (byte) 1;
            }
            if (customizedClass == 7) {
                return (byte) 4;
            }
        }
        return (byte) 4;
    }

    private byte firstL_R_AL_EN_AN() {
        int iCharCount = 0;
        while (iCharCount < this.epilogue.length()) {
            int iCodePointAt = this.epilogue.codePointAt(iCharCount);
            iCharCount += Character.charCount(iCodePointAt);
            byte customizedClass = (byte) getCustomizedClass(iCodePointAt);
            if (customizedClass == 0) {
                return (byte) 0;
            }
            if (customizedClass == 1 || customizedClass == 13) {
                return (byte) 1;
            }
            if (customizedClass == 2) {
                return (byte) 2;
            }
            if (customizedClass == 5) {
                return (byte) 3;
            }
        }
        return (byte) 4;
    }

    private void resolveImplicitLevels(int i, int i2, short s, short s2) {
        int i3;
        short s3;
        short s4;
        int i4;
        byte b;
        byte bFirstL_R_AL_EN_AN;
        short s5;
        byte bLastL_R_AL;
        LevState levState = new LevState();
        boolean z = i < this.lastArabicPos && (GetParaLevelAt(i) & 1) > 0 && (this.reorderingMode == 5 || this.reorderingMode == 6);
        levState.startL2EN = -1;
        levState.lastStrongRTL = -1;
        levState.runStart = i;
        levState.runLevel = this.levels[i];
        levState.impTab = this.impTabPair.imptab[levState.runLevel & 1];
        levState.impAct = this.impTabPair.impact[levState.runLevel & 1];
        short s6 = (i != 0 || this.prologue == null || (bLastL_R_AL = lastL_R_AL()) == 4) ? s : bLastL_R_AL;
        if (this.dirProps[i] == 22) {
            levState.startON = this.isolates[this.isolateCount].startON;
            i3 = this.isolates[this.isolateCount].start1;
            s3 = this.isolates[this.isolateCount].stateImp;
            levState.state = this.isolates[this.isolateCount].state;
            this.isolateCount--;
        } else {
            levState.startON = -1;
            short s7 = this.dirProps[i] == 17 ? (short) (1 + s6) : (short) 0;
            levState.state = (short) 0;
            processPropertySeq(levState, s6, i, i);
            i3 = i;
            s3 = s7;
        }
        int i5 = i;
        int i6 = i3;
        short s8 = 1;
        int i7 = -1;
        int i8 = i5;
        while (i8 <= i2) {
            if (i8 >= i2) {
                int i9 = i2 - 1;
                while (i9 > i && (DirPropFlag(this.dirProps[i9]) & MASK_BN_EXPLICIT) != 0) {
                    i9--;
                }
                byte b2 = this.dirProps[i9];
                if (b2 == 20 || b2 == 21) {
                    s4 = (i2 == this.length || this.epilogue == null || (bFirstL_R_AL_EN_AN = firstL_R_AL_EN_AN()) == 4) ? s2 : bFirstL_R_AL_EN_AN;
                    i4 = i2 - 1;
                    while (i4 > i && (DirPropFlag(this.dirProps[i4]) & MASK_BN_EXPLICIT) != 0) {
                        i4--;
                    }
                    b = this.dirProps[i4];
                    if ((b == 20 && b != 21) || i2 >= this.length) {
                        processPropertySeq(levState, s4, i2, i2);
                    }
                    this.isolateCount++;
                    if (this.isolates[this.isolateCount] == null) {
                        this.isolates[this.isolateCount] = new Isolate();
                    }
                    this.isolates[this.isolateCount].stateImp = s3;
                    this.isolates[this.isolateCount].state = levState.state;
                    this.isolates[this.isolateCount].start1 = i6;
                    this.isolates[this.isolateCount].startON = levState.startON;
                    return;
                }
                s5 = s2;
            } else {
                byte b3 = this.dirProps[i8];
                if (b3 == 7) {
                    this.isolateCount = -1;
                }
                if (z) {
                    if (b3 == 13) {
                        b3 = 1;
                    } else if (b3 == 2) {
                        if (i7 <= i8) {
                            for (int i10 = i8 + 1; i10 < i2; i10++) {
                                byte b4 = this.dirProps[i10];
                                if (b4 == 0 || b4 == 1 || b4 == 13) {
                                    s8 = b4;
                                    i7 = i10;
                                }
                            }
                            i7 = i2;
                            s8 = 1;
                        }
                        if (s8 == 13) {
                            b3 = 5;
                        }
                    }
                }
                s5 = groupProp[b3];
            }
            short s9 = impTabProps[s3][s5];
            short sGetStateProps = GetStateProps(s9);
            short sGetActionProps = GetActionProps(s9);
            if (i8 == i2 && sGetActionProps == 0) {
                sGetActionProps = 1;
            }
            if (sGetActionProps != 0) {
                short s10 = impTabProps[s3][15];
                switch (sGetActionProps) {
                    case 1:
                        processPropertySeq(levState, s10, i6, i8);
                        i6 = i8;
                        break;
                    case 2:
                        i5 = i8;
                        break;
                    case 3:
                        processPropertySeq(levState, s10, i6, i5);
                        processPropertySeq(levState, (short) 4, i5, i8);
                        i6 = i8;
                        break;
                    case 4:
                        processPropertySeq(levState, s10, i6, i5);
                        i6 = i5;
                        i5 = i8;
                        break;
                    default:
                        throw new IllegalStateException("Internal ICU error in resolveImplicitLevels");
                }
            }
            i8++;
            s3 = sGetStateProps;
        }
        if (i2 == this.length) {
        }
        i4 = i2 - 1;
        while (i4 > i) {
            i4--;
        }
        b = this.dirProps[i4];
        if (b == 20) {
            this.isolateCount++;
            if (this.isolates[this.isolateCount] == null) {
            }
            this.isolates[this.isolateCount].stateImp = s3;
            this.isolates[this.isolateCount].state = levState.state;
            this.isolates[this.isolateCount].start1 = i6;
            this.isolates[this.isolateCount].startON = levState.startON;
            return;
        }
        this.isolateCount++;
        if (this.isolates[this.isolateCount] == null) {
        }
        this.isolates[this.isolateCount].stateImp = s3;
        this.isolates[this.isolateCount].state = levState.state;
        this.isolates[this.isolateCount].start1 = i6;
        this.isolates[this.isolateCount].startON = levState.startON;
        return;
        processPropertySeq(levState, s4, i2, i2);
    }

    private void adjustWSLevels() {
        if ((this.flags & MASK_WS) != 0) {
            int i = this.trailingWSStart;
            while (i > 0) {
                while (i > 0) {
                    i--;
                    int iDirPropFlag = DirPropFlag(this.dirProps[i]);
                    if ((MASK_WS & iDirPropFlag) == 0) {
                        break;
                    }
                    if (this.orderParagraphsLTR && (DirPropFlag((byte) 7) & iDirPropFlag) != 0) {
                        this.levels[i] = 0;
                    } else {
                        this.levels[i] = GetParaLevelAt(i);
                    }
                }
                while (true) {
                    if (i > 0) {
                        i--;
                        int iDirPropFlag2 = DirPropFlag(this.dirProps[i]);
                        if ((MASK_BN_EXPLICIT & iDirPropFlag2) != 0) {
                            this.levels[i] = this.levels[i + 1];
                        } else if (this.orderParagraphsLTR && (DirPropFlag((byte) 7) & iDirPropFlag2) != 0) {
                            this.levels[i] = 0;
                            break;
                        } else if ((iDirPropFlag2 & MASK_B_S) != 0) {
                            this.levels[i] = GetParaLevelAt(i);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setContext(String str, String str2) {
        if (str == null || str.length() <= 0) {
            str = null;
        }
        this.prologue = str;
        if (str2 == null || str2.length() <= 0) {
            str2 = null;
        }
        this.epilogue = str2;
    }

    private void setParaSuccess() {
        this.prologue = null;
        this.epilogue = null;
        this.paraBidi = this;
    }

    int Bidi_Min(int i, int i2) {
        return i < i2 ? i : i2;
    }

    int Bidi_Abs(int i) {
        return i >= 0 ? i : -i;
    }

    void setParaRunsOnly(char[] cArr, byte b) {
        int i;
        byte b2;
        int i2;
        this.reorderingMode = 0;
        int length = cArr.length;
        if (length == 0) {
            setPara(cArr, b, (byte[]) null);
            this.reorderingMode = 3;
            return;
        }
        int i3 = this.reorderingOptions;
        int i4 = 2;
        if ((i3 & 1) > 0) {
            this.reorderingOptions &= -2;
            this.reorderingOptions |= 2;
        }
        byte b3 = 1;
        byte b4 = (byte) (b & 1);
        setPara(cArr, b4, (byte[]) null);
        byte[] bArr = new byte[this.length];
        System.arraycopy(getLevels(), 0, bArr, 0, this.length);
        int i5 = this.trailingWSStart;
        String strWriteReordered = writeReordered(2);
        int[] visualMap = getVisualMap();
        this.reorderingOptions = i3;
        int i6 = this.length;
        byte b5 = this.direction;
        this.reorderingMode = 5;
        setPara(strWriteReordered, (byte) (b4 ^ 1), (byte[]) null);
        BidiLine.getRuns(this);
        int i7 = this.runCount;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        while (i8 < i7) {
            int i11 = this.runs[i8].limit - i9;
            if (i11 >= i4) {
                int i12 = this.runs[i8].start;
                int i13 = i10;
                int i14 = i12 + 1;
                while (i14 < i12 + i11) {
                    int i15 = visualMap[i14];
                    int i16 = visualMap[i14 - 1];
                    int i17 = i12;
                    if (Bidi_Abs(i15 - i16) != 1 || bArr[i15] != bArr[i16]) {
                        i13++;
                    }
                    i14++;
                    i12 = i17;
                }
                i10 = i13;
            }
            i8++;
            i9 += i11;
            i4 = 2;
        }
        if (i10 > 0) {
            getRunsMemory(i7 + i10);
            if (this.runCount == 1) {
                this.runsMemory[0] = this.runs[0];
            } else {
                System.arraycopy(this.runs, 0, this.runsMemory, 0, this.runCount);
            }
            this.runs = this.runsMemory;
            this.runCount += i10;
            for (int i18 = i7; i18 < this.runCount; i18++) {
                if (this.runs[i18] == null) {
                    this.runs[i18] = new BidiRun(0, 0, (byte) 0);
                }
            }
        }
        int i19 = i7 - 1;
        while (i19 >= 0) {
            int i20 = i19 + i10;
            int i21 = i19 == 0 ? this.runs[0].limit : this.runs[i19].limit - this.runs[i19 - 1].limit;
            int i22 = this.runs[i19].start;
            int i23 = this.runs[i19].level & b3;
            if (i21 < 2) {
                if (i10 > 0) {
                    this.runs[i20].copyFrom(this.runs[i19]);
                }
                int i24 = visualMap[i22];
                this.runs[i20].start = i24;
                this.runs[i20].level = (byte) (bArr[i24] ^ i23);
                i2 = i5;
            } else {
                if (i23 <= 0) {
                    i = (i21 + i22) - b3;
                    b2 = -1;
                } else {
                    b2 = b3;
                    i22 = (i21 + i22) - b3;
                    i = i22;
                }
                int i25 = i20;
                int i26 = i;
                while (i != i22) {
                    int i27 = visualMap[i];
                    int i28 = i + b2;
                    int i29 = visualMap[i28];
                    byte b6 = b2;
                    int i30 = i5;
                    if (Bidi_Abs(i27 - i29) != 1 || bArr[i27] != bArr[i29]) {
                        int iBidi_Min = Bidi_Min(visualMap[i26], i27);
                        this.runs[i25].start = iBidi_Min;
                        this.runs[i25].level = (byte) (bArr[iBidi_Min] ^ i23);
                        this.runs[i25].limit = this.runs[i19].limit;
                        this.runs[i19].limit -= Bidi_Abs(i - i26) + 1;
                        int i31 = this.runs[i19].insertRemove & 10;
                        this.runs[i25].insertRemove = i31;
                        BidiRun bidiRun = this.runs[i19];
                        bidiRun.insertRemove = (~i31) & bidiRun.insertRemove;
                        i10--;
                        i25--;
                        i26 = i28;
                    }
                    i = i28;
                    b2 = b6;
                    i5 = i30;
                }
                i2 = i5;
                if (i10 > 0) {
                    this.runs[i25].copyFrom(this.runs[i19]);
                }
                int iBidi_Min2 = Bidi_Min(visualMap[i26], visualMap[i22]);
                this.runs[i25].start = iBidi_Min2;
                this.runs[i25].level = (byte) (bArr[iBidi_Min2] ^ i23);
            }
            i19--;
            i5 = i2;
            b3 = 1;
        }
        this.paraLevel = (byte) (this.paraLevel ^ 1);
        this.text = cArr;
        this.length = i6;
        this.originalLength = length;
        this.direction = b5;
        this.levels = bArr;
        this.trailingWSStart = i5;
        if (this.runCount > 1) {
            this.direction = (byte) 2;
        }
        this.reorderingMode = 3;
    }

    public void setPara(String str, byte b, byte[] bArr) {
        if (str == null) {
            setPara(new char[0], b, bArr);
        } else {
            setPara(str.toCharArray(), b, bArr);
        }
    }

    public void setPara(char[] cArr, byte b, byte[] bArr) {
        int i;
        byte bGetParaLevelAt;
        byte b2;
        short sGetLRFromLevel;
        short sGetLRFromLevel2;
        int i2;
        int i3;
        byte bGetParaLevelAt2;
        short sGetLRFromLevel3;
        if (b < 126) {
            verifyRange(b, 0, 126);
        }
        if (cArr == null) {
            cArr = new char[0];
        }
        if (this.reorderingMode == 3) {
            setParaRunsOnly(cArr, b);
            return;
        }
        this.paraBidi = null;
        this.text = cArr;
        int length = this.text.length;
        this.resultLength = length;
        this.originalLength = length;
        this.length = length;
        this.paraLevel = b;
        this.direction = (byte) (b & 1);
        this.paraCount = 1;
        this.dirProps = new byte[0];
        this.levels = new byte[0];
        this.runs = new BidiRun[0];
        this.isGoodLogicalToVisualRunsMap = false;
        this.insertPoints.size = 0;
        this.insertPoints.confirmed = 0;
        this.defaultParaLevel = IsDefaultLevel(b) ? b : (byte) 0;
        if (this.length == 0) {
            if (IsDefaultLevel(b)) {
                this.paraLevel = (byte) (1 & this.paraLevel);
                this.defaultParaLevel = (byte) 0;
            }
            this.flags = DirPropFlagLR(b);
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
        if (bArr == null) {
            getLevelsMemory(this.length);
            this.levels = this.levelsMemory;
            this.direction = resolveExplicitLevels();
        } else {
            this.levels = bArr;
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
                        if (bArr == null && this.paraCount <= 1 && (this.flags & DirPropFlagMultiRuns) == 0) {
                            resolveImplicitLevels(0, this.length, GetLRFromLevel(GetParaLevelAt(0)), GetLRFromLevel(GetParaLevelAt(this.length - 1)));
                        } else {
                            bGetParaLevelAt = GetParaLevelAt(0);
                            b2 = this.levels[0];
                            if (bGetParaLevelAt >= b2) {
                                sGetLRFromLevel = GetLRFromLevel(b2);
                            } else {
                                sGetLRFromLevel = GetLRFromLevel(bGetParaLevelAt);
                            }
                            sGetLRFromLevel2 = sGetLRFromLevel;
                            i2 = 0;
                            while (true) {
                                if (i2 > 0 && this.dirProps[i2 - 1] == 7) {
                                    sGetLRFromLevel2 = GetLRFromLevel(GetParaLevelAt(i2));
                                }
                                short s = sGetLRFromLevel2;
                                i3 = i2;
                                while (true) {
                                    i3++;
                                    if (i3 < this.length || (this.levels[i3] != b2 && (DirPropFlag(this.dirProps[i3]) & MASK_BN_EXPLICIT) == 0)) {
                                    }
                                }
                                if (i3 < this.length) {
                                    bGetParaLevelAt2 = GetParaLevelAt(this.length - 1);
                                } else {
                                    bGetParaLevelAt2 = this.levels[i3];
                                }
                                if (NoOverride(b2) >= NoOverride(bGetParaLevelAt2)) {
                                    sGetLRFromLevel3 = GetLRFromLevel(bGetParaLevelAt2);
                                } else {
                                    sGetLRFromLevel3 = GetLRFromLevel(b2);
                                }
                                if ((b2 & LEVEL_OVERRIDE) != 0) {
                                    resolveImplicitLevels(i2, i3, s, sGetLRFromLevel3);
                                } else {
                                    while (true) {
                                        byte[] bArr2 = this.levels;
                                        int i4 = i2 + 1;
                                        bArr2[i2] = (byte) (bArr2[i2] & LEVEL_DEFAULT_RTL);
                                        if (i4 < i3) {
                                            i2 = i4;
                                        }
                                    }
                                }
                                if (i3 >= this.length) {
                                    i2 = i3;
                                    b2 = bGetParaLevelAt2;
                                    sGetLRFromLevel2 = sGetLRFromLevel3;
                                }
                            }
                        }
                        adjustWSLevels();
                        break;
                    case 1:
                        this.impTabPair = impTab_NUMBERS_SPECIAL;
                        if (bArr == null) {
                            bGetParaLevelAt = GetParaLevelAt(0);
                            b2 = this.levels[0];
                            if (bGetParaLevelAt >= b2) {
                            }
                            sGetLRFromLevel2 = sGetLRFromLevel;
                            i2 = 0;
                            while (true) {
                                if (i2 > 0) {
                                    sGetLRFromLevel2 = GetLRFromLevel(GetParaLevelAt(i2));
                                }
                                short s2 = sGetLRFromLevel2;
                                i3 = i2;
                                while (true) {
                                    i3++;
                                    if (i3 < this.length) {
                                    }
                                    if (i3 < this.length) {
                                    }
                                    if (NoOverride(b2) >= NoOverride(bGetParaLevelAt2)) {
                                    }
                                    if ((b2 & LEVEL_OVERRIDE) != 0) {
                                    }
                                    if (i3 >= this.length) {
                                    }
                                }
                                i2 = i3;
                                b2 = bGetParaLevelAt2;
                                sGetLRFromLevel2 = sGetLRFromLevel3;
                            }
                        }
                        break;
                    case 2:
                        this.impTabPair = impTab_GROUP_NUMBERS_WITH_R;
                        if (bArr == null) {
                        }
                        break;
                    case 3:
                        throw new InternalError("Internal ICU error in setPara");
                    case 4:
                        this.impTabPair = impTab_INVERSE_NUMBERS_AS_L;
                        if (bArr == null) {
                        }
                        break;
                    case 5:
                        if ((this.reorderingOptions & 1) != 0) {
                            this.impTabPair = impTab_INVERSE_LIKE_DIRECT_WITH_MARKS;
                        } else {
                            this.impTabPair = impTab_INVERSE_LIKE_DIRECT;
                        }
                        if (bArr == null) {
                        }
                        break;
                    case 6:
                        if ((this.reorderingOptions & 1) != 0) {
                            this.impTabPair = impTab_INVERSE_FOR_NUMBERS_SPECIAL_WITH_MARKS;
                        } else {
                            this.impTabPair = impTab_INVERSE_FOR_NUMBERS_SPECIAL;
                        }
                        if (bArr == null) {
                        }
                        break;
                    default:
                        if (bArr == null) {
                        }
                        break;
                }
                break;
        }
        if (this.defaultParaLevel > 0 && (this.reorderingOptions & 1) != 0 && (this.reorderingMode == 5 || this.reorderingMode == 6)) {
            for (int i5 = 0; i5 < this.paraCount; i5++) {
                int i6 = this.paras_limit[i5] - 1;
                if (this.paras_level[i5] != 0) {
                    if (i5 != 0) {
                        i = this.paras_limit[i5 - 1];
                    } else {
                        i = 0;
                    }
                    int i7 = i6;
                    while (true) {
                        if (i7 < i) {
                            break;
                        }
                        byte b3 = this.dirProps[i7];
                        if (b3 == 0) {
                            if (i7 < i6) {
                                while (this.dirProps[i6] == 7) {
                                    i6--;
                                }
                            }
                            addPoint(i6, 4);
                        } else if ((DirPropFlag(b3) & MASK_R_AL) != 0) {
                            break;
                        } else {
                            i7--;
                        }
                    }
                }
            }
        }
        if ((this.reorderingOptions & 2) != 0) {
            this.resultLength -= this.controlCount;
        } else {
            this.resultLength += this.insertPoints.size;
        }
        setParaSuccess();
    }

    public void setPara(AttributedCharacterIterator attributedCharacterIterator) {
        byte b;
        byte bByteValue;
        Boolean bool = (Boolean) attributedCharacterIterator.getAttribute(TextAttribute.RUN_DIRECTION);
        if (bool == null) {
            b = LEVEL_DEFAULT_LTR;
        } else {
            b = bool.equals(TextAttribute.RUN_DIRECTION_LTR) ? (byte) 0 : (byte) 1;
        }
        int endIndex = attributedCharacterIterator.getEndIndex() - attributedCharacterIterator.getBeginIndex();
        byte[] bArr = new byte[endIndex];
        char[] cArr = new char[endIndex];
        char cFirst = attributedCharacterIterator.first();
        byte[] bArr2 = null;
        int i = 0;
        while (cFirst != 65535) {
            cArr[i] = cFirst;
            Integer num = (Integer) attributedCharacterIterator.getAttribute(TextAttribute.BIDI_EMBEDDING);
            if (num != null && (bByteValue = num.byteValue()) != 0) {
                if (bByteValue < 0) {
                    bArr[i] = (byte) ((0 - bByteValue) | (-128));
                } else {
                    bArr[i] = bByteValue;
                }
                bArr2 = bArr;
            }
            cFirst = attributedCharacterIterator.next();
            i++;
        }
        NumericShaper numericShaper = (NumericShaper) attributedCharacterIterator.getAttribute(TextAttribute.NUMERIC_SHAPING);
        if (numericShaper != null) {
            numericShaper.shape(cArr, 0, endIndex);
        }
        setPara(cArr, b, bArr2);
    }

    public void orderParagraphsLTR(boolean z) {
        this.orderParagraphsLTR = z;
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

    public BidiRun getParagraphByIndex(int i) {
        verifyValidParaOrLine();
        verifyRange(i, 0, this.paraCount);
        Bidi bidi = this.paraBidi;
        int i2 = i != 0 ? bidi.paras_limit[i - 1] : 0;
        BidiRun bidiRun = new BidiRun();
        bidiRun.start = i2;
        bidiRun.limit = bidi.paras_limit[i];
        bidiRun.level = GetParaLevelAt(i2);
        return bidiRun;
    }

    public BidiRun getParagraph(int i) {
        verifyValidParaOrLine();
        Bidi bidi = this.paraBidi;
        int i2 = 0;
        verifyRange(i, 0, bidi.length);
        while (i >= bidi.paras_limit[i2]) {
            i2++;
        }
        return getParagraphByIndex(i2);
    }

    public int getParagraphIndex(int i) {
        verifyValidParaOrLine();
        Bidi bidi = this.paraBidi;
        int i2 = 0;
        verifyRange(i, 0, bidi.length);
        while (i >= bidi.paras_limit[i2]) {
            i2++;
        }
        return i2;
    }

    public void setCustomClassifier(BidiClassifier bidiClassifier) {
        this.customClassifier = bidiClassifier;
    }

    public BidiClassifier getCustomClassifier() {
        return this.customClassifier;
    }

    public int getCustomizedClass(int i) {
        int iClassify;
        if (this.customClassifier == null || (iClassify = this.customClassifier.classify(i)) == 23) {
            iClassify = this.bdp.getClass(i);
        }
        if (iClassify >= 23) {
            return 10;
        }
        return iClassify;
    }

    public Bidi setLine(int i, int i2) {
        verifyValidPara();
        verifyRange(i, 0, i2);
        verifyRange(i2, 0, this.length + 1);
        if (getParagraphIndex(i) != getParagraphIndex(i2 - 1)) {
            throw new IllegalArgumentException();
        }
        return BidiLine.setLine(this, i, i2);
    }

    public byte getLevelAt(int i) {
        verifyValidParaOrLine();
        verifyRange(i, 0, this.length);
        return BidiLine.getLevelAt(this, i);
    }

    public byte[] getLevels() {
        verifyValidParaOrLine();
        if (this.length <= 0) {
            return new byte[0];
        }
        return BidiLine.getLevels(this);
    }

    public BidiRun getLogicalRun(int i) {
        verifyValidParaOrLine();
        verifyRange(i, 0, this.length);
        return BidiLine.getLogicalRun(this, i);
    }

    public int countRuns() {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        return this.runCount;
    }

    public BidiRun getVisualRun(int i) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(i, 0, this.runCount);
        return BidiLine.getVisualRun(this, i);
    }

    public int getVisualIndex(int i) {
        verifyValidParaOrLine();
        verifyRange(i, 0, this.length);
        return BidiLine.getVisualIndex(this, i);
    }

    public int getLogicalIndex(int i) {
        verifyValidParaOrLine();
        verifyRange(i, 0, this.resultLength);
        if (this.insertPoints.size == 0 && this.controlCount == 0) {
            if (this.direction == 0) {
                return i;
            }
            if (this.direction == 1) {
                return (this.length - i) - 1;
            }
        }
        BidiLine.getRuns(this);
        return BidiLine.getLogicalIndex(this, i);
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

    public static int[] reorderLogical(byte[] bArr) {
        return BidiLine.reorderLogical(bArr);
    }

    public static int[] reorderVisual(byte[] bArr) {
        return BidiLine.reorderVisual(bArr);
    }

    public static int[] invertMap(int[] iArr) {
        if (iArr == null) {
            return null;
        }
        return BidiLine.invertMap(iArr);
    }

    public Bidi(String str, int i) {
        this(str.toCharArray(), 0, null, 0, str.length(), i);
    }

    public Bidi(AttributedCharacterIterator attributedCharacterIterator) {
        this();
        setPara(attributedCharacterIterator);
    }

    public Bidi(char[] cArr, int i, byte[] bArr, int i2, int i3, int i4) {
        byte b;
        byte[] bArr2;
        this();
        if (i4 != 1) {
            switch (i4) {
                case 126:
                    b = LEVEL_DEFAULT_LTR;
                    break;
                case 127:
                    b = LEVEL_DEFAULT_RTL;
                    break;
                default:
                    b = 0;
                    break;
            }
        } else {
            b = 1;
        }
        if (bArr == null) {
            bArr2 = null;
        } else {
            byte[] bArr3 = new byte[i3];
            for (int i5 = 0; i5 < i3; i5++) {
                byte b2 = bArr[i5 + i2];
                if (b2 < 0) {
                    b2 = (byte) ((-b2) | (-128));
                }
                bArr3[i5] = b2;
            }
            bArr2 = bArr3;
        }
        if (i == 0 && i3 == cArr.length) {
            setPara(cArr, b, bArr2);
            return;
        }
        char[] cArr2 = new char[i3];
        System.arraycopy(cArr, i, cArr2, 0, i3);
        setPara(cArr2, b, bArr2);
    }

    public Bidi createLineBidi(int i, int i2) {
        return setLine(i, i2);
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
        int iCountRuns = countRuns();
        if (this.logicalToVisualRunsMap == null || this.logicalToVisualRunsMap.length < iCountRuns) {
            this.logicalToVisualRunsMap = new int[iCountRuns];
        }
        long[] jArr = new long[iCountRuns];
        for (int i = 0; i < iCountRuns; i++) {
            jArr[i] = (((long) this.runs[i].start) << 32) + ((long) i);
        }
        Arrays.sort(jArr);
        for (int i2 = 0; i2 < iCountRuns; i2++) {
            this.logicalToVisualRunsMap[i2] = (int) (jArr[i2] & (-1));
        }
        this.isGoodLogicalToVisualRunsMap = true;
    }

    public int getRunLevel(int i) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(i, 0, this.runCount);
        getLogicalToVisualRunsMap();
        return this.runs[this.logicalToVisualRunsMap[i]].level;
    }

    public int getRunStart(int i) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(i, 0, this.runCount);
        getLogicalToVisualRunsMap();
        return this.runs[this.logicalToVisualRunsMap[i]].start;
    }

    public int getRunLimit(int i) {
        verifyValidParaOrLine();
        BidiLine.getRuns(this);
        verifyRange(i, 0, this.runCount);
        getLogicalToVisualRunsMap();
        int i2 = this.logicalToVisualRunsMap[i];
        return this.runs[i2].start + (i2 == 0 ? this.runs[i2].limit : this.runs[i2].limit - this.runs[i2 - 1].limit);
    }

    public static boolean requiresBidi(char[] cArr, int i, int i2) {
        while (i < i2) {
            if (((1 << UCharacter.getDirection(cArr[i])) & 57378) != 0) {
                return true;
            }
            i++;
        }
        return false;
    }

    public static void reorderVisually(byte[] bArr, int i, Object[] objArr, int i2, int i3) {
        byte[] bArr2 = new byte[i3];
        System.arraycopy(bArr, i, bArr2, 0, i3);
        int[] iArrReorderVisual = reorderVisual(bArr2);
        Object[] objArr2 = new Object[i3];
        System.arraycopy(objArr, i2, objArr2, 0, i3);
        for (int i4 = 0; i4 < i3; i4++) {
            objArr[i2 + i4] = objArr2[iArrReorderVisual[i4]];
        }
    }

    public String writeReordered(int i) {
        verifyValidParaOrLine();
        if (this.length == 0) {
            return "";
        }
        return BidiWriter.writeReordered(this, i);
    }

    public static String writeReverse(String str, int i) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        if (str.length() > 0) {
            return BidiWriter.writeReverse(str, i);
        }
        return "";
    }
}
