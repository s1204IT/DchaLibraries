package android.icu.text;

import android.icu.text.Bidi;
import java.util.Arrays;

final class BidiLine {
    BidiLine() {
    }

    static void setTrailingWSStart(Bidi bidi) {
        byte[] bArr = bidi.dirProps;
        byte[] bArr2 = bidi.levels;
        int i = bidi.length;
        byte b = bidi.paraLevel;
        if (bArr[i - 1] == 7) {
            bidi.trailingWSStart = i;
            return;
        }
        while (i > 0 && (Bidi.DirPropFlag(bArr[i - 1]) & Bidi.MASK_WS) != 0) {
            i--;
        }
        while (i > 0 && bArr2[i - 1] == b) {
            i--;
        }
        bidi.trailingWSStart = i;
    }

    static Bidi setLine(Bidi bidi, int i, int i2) {
        Bidi bidi2 = new Bidi();
        int i3 = i2 - i;
        bidi2.resultLength = i3;
        bidi2.originalLength = i3;
        bidi2.length = i3;
        bidi2.text = new char[i3];
        System.arraycopy(bidi.text, i, bidi2.text, 0, i3);
        bidi2.paraLevel = bidi.GetParaLevelAt(i);
        bidi2.paraCount = bidi.paraCount;
        bidi2.runs = new BidiRun[0];
        bidi2.reorderingMode = bidi.reorderingMode;
        bidi2.reorderingOptions = bidi.reorderingOptions;
        if (bidi.controlCount > 0) {
            for (int i4 = i; i4 < i2; i4++) {
                if (Bidi.IsBidiControlChar(bidi.text[i4])) {
                    bidi2.controlCount++;
                }
            }
            bidi2.resultLength -= bidi2.controlCount;
        }
        bidi2.getDirPropsMemory(i3);
        bidi2.dirProps = bidi2.dirPropsMemory;
        System.arraycopy(bidi.dirProps, i, bidi2.dirProps, 0, i3);
        bidi2.getLevelsMemory(i3);
        bidi2.levels = bidi2.levelsMemory;
        System.arraycopy(bidi.levels, i, bidi2.levels, 0, i3);
        bidi2.runCount = -1;
        if (bidi.direction != 2) {
            bidi2.direction = bidi.direction;
            if (bidi.trailingWSStart <= i) {
                bidi2.trailingWSStart = 0;
            } else if (bidi.trailingWSStart < i2) {
                bidi2.trailingWSStart = bidi.trailingWSStart - i;
            } else {
                bidi2.trailingWSStart = i3;
            }
        } else {
            byte[] bArr = bidi2.levels;
            setTrailingWSStart(bidi2);
            int i5 = bidi2.trailingWSStart;
            if (i5 == 0) {
                bidi2.direction = (byte) (bidi2.paraLevel & 1);
            } else {
                byte b = (byte) (bArr[0] & 1);
                if (i5 < i3 && (bidi2.paraLevel & 1) != b) {
                    bidi2.direction = (byte) 2;
                } else {
                    int i6 = 1;
                    while (true) {
                        if (i6 == i5) {
                            bidi2.direction = b;
                            break;
                        }
                        if ((bArr[i6] & 1) == b) {
                            i6++;
                        } else {
                            bidi2.direction = (byte) 2;
                            break;
                        }
                    }
                }
            }
            switch (bidi2.direction) {
                case 0:
                    bidi2.paraLevel = (byte) ((bidi2.paraLevel + 1) & (-2));
                    bidi2.trailingWSStart = 0;
                    break;
                case 1:
                    bidi2.paraLevel = (byte) (bidi2.paraLevel | 1);
                    bidi2.trailingWSStart = 0;
                    break;
            }
        }
        bidi2.paraBidi = bidi;
        return bidi2;
    }

    static byte getLevelAt(Bidi bidi, int i) {
        if (bidi.direction != 2 || i >= bidi.trailingWSStart) {
            return bidi.GetParaLevelAt(i);
        }
        return bidi.levels[i];
    }

    static byte[] getLevels(Bidi bidi) {
        int i = bidi.trailingWSStart;
        int i2 = bidi.length;
        if (i != i2) {
            Arrays.fill(bidi.levels, i, i2, bidi.paraLevel);
            bidi.trailingWSStart = i2;
        }
        if (i2 < bidi.levels.length) {
            byte[] bArr = new byte[i2];
            System.arraycopy(bidi.levels, 0, bArr, 0, i2);
            return bArr;
        }
        return bidi.levels;
    }

    static BidiRun getLogicalRun(Bidi bidi, int i) {
        BidiRun bidiRun = new BidiRun();
        getRuns(bidi);
        int i2 = bidi.runCount;
        BidiRun bidiRun2 = bidi.runs[0];
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < i2; i5++) {
            bidiRun2 = bidi.runs[i5];
            i4 = (bidiRun2.start + bidiRun2.limit) - i3;
            if (i >= bidiRun2.start && i < i4) {
                break;
            }
            i3 = bidiRun2.limit;
        }
        bidiRun.start = bidiRun2.start;
        bidiRun.limit = i4;
        bidiRun.level = bidiRun2.level;
        return bidiRun;
    }

    static BidiRun getVisualRun(Bidi bidi, int i) {
        int i2;
        int i3 = bidi.runs[i].start;
        byte b = bidi.runs[i].level;
        if (i > 0) {
            i2 = (bidi.runs[i].limit + i3) - bidi.runs[i - 1].limit;
        } else {
            i2 = i3 + bidi.runs[0].limit;
        }
        return new BidiRun(i3, i2, b);
    }

    static void getSingleRun(Bidi bidi, byte b) {
        bidi.runs = bidi.simpleRuns;
        bidi.runCount = 1;
        bidi.runs[0] = new BidiRun(0, bidi.length, b);
    }

    private static void reorderLine(Bidi bidi, byte b, byte b2) {
        int i;
        if (b2 <= (b | 1)) {
            return;
        }
        byte b3 = (byte) (b + 1);
        BidiRun[] bidiRunArr = bidi.runs;
        byte[] bArr = bidi.levels;
        int i2 = bidi.runCount;
        if (bidi.trailingWSStart < bidi.length) {
            i2--;
        }
        while (true) {
            b2 = (byte) (b2 - 1);
            i = 0;
            if (b2 < b3) {
                break;
            }
            while (true) {
                if (i < i2 && bArr[bidiRunArr[i].start] < b2) {
                    i++;
                } else {
                    if (i >= i2) {
                        break;
                    }
                    int i3 = i;
                    do {
                        i3++;
                        if (i3 >= i2) {
                            break;
                        }
                    } while (bArr[bidiRunArr[i3].start] >= b2);
                    for (int i4 = i3 - 1; i < i4; i4--) {
                        BidiRun bidiRun = bidiRunArr[i];
                        bidiRunArr[i] = bidiRunArr[i4];
                        bidiRunArr[i4] = bidiRun;
                        i++;
                    }
                    if (i3 == i2) {
                        break;
                    } else {
                        i = i3 + 1;
                    }
                }
            }
        }
        if ((b3 & 1) == 0) {
            if (bidi.trailingWSStart == bidi.length) {
                i2--;
            }
            while (i < i2) {
                BidiRun bidiRun2 = bidiRunArr[i];
                bidiRunArr[i] = bidiRunArr[i2];
                bidiRunArr[i2] = bidiRun2;
                i++;
                i2--;
            }
        }
    }

    static int getRunFromLogicalIndex(Bidi bidi, int i) {
        BidiRun[] bidiRunArr = bidi.runs;
        int i2 = bidi.runCount;
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            int i5 = bidiRunArr[i4].limit - i3;
            int i6 = bidiRunArr[i4].start;
            if (i >= i6 && i < i6 + i5) {
                return i4;
            }
            i3 += i5;
        }
        throw new IllegalStateException("Internal ICU error in getRunFromLogicalIndex");
    }

    static void getRuns(Bidi bidi) {
        byte b;
        int i;
        if (bidi.runCount >= 0) {
            return;
        }
        if (bidi.direction != 2) {
            getSingleRun(bidi, bidi.paraLevel);
        } else {
            int i2 = bidi.length;
            byte[] bArr = bidi.levels;
            int i3 = bidi.trailingWSStart;
            int i4 = 0;
            byte b2 = -1;
            for (int i5 = 0; i5 < i3; i5++) {
                if (bArr[i5] != b2) {
                    i4++;
                    b2 = bArr[i5];
                }
            }
            if (i4 == 1 && i3 == i2) {
                getSingleRun(bidi, bArr[0]);
            } else {
                if (i3 < i2) {
                    i4++;
                }
                bidi.getRunsMemory(i4);
                BidiRun[] bidiRunArr = bidi.runsMemory;
                byte b3 = 0;
                int i6 = 0;
                byte b4 = 126;
                int i7 = 0;
                while (true) {
                    byte b5 = bArr[i7];
                    if (b5 < b4) {
                        b4 = b5;
                    }
                    if (b5 > b3) {
                        i = i7;
                        b = b5;
                    } else {
                        b = b3;
                        i = i7;
                    }
                    do {
                        i++;
                        if (i >= i3) {
                            break;
                        }
                    } while (bArr[i] == b5);
                    bidiRunArr[i6] = new BidiRun(i7, i - i7, b5);
                    i6++;
                    if (i >= i3) {
                        break;
                    }
                    i7 = i;
                    b3 = b;
                }
                if (i3 < i2) {
                    bidiRunArr[i6] = new BidiRun(i3, i2 - i3, bidi.paraLevel);
                    if (bidi.paraLevel < b4) {
                        b4 = bidi.paraLevel;
                    }
                }
                bidi.runs = bidiRunArr;
                bidi.runCount = i4;
                reorderLine(bidi, b4, b);
                int i8 = 0;
                for (int i9 = 0; i9 < i4; i9++) {
                    bidiRunArr[i9].level = bArr[bidiRunArr[i9].start];
                    BidiRun bidiRun = bidiRunArr[i9];
                    i8 += bidiRun.limit;
                    bidiRun.limit = i8;
                }
                if (i6 < i4) {
                    if ((bidi.paraLevel & 1) != 0) {
                        i6 = 0;
                    }
                    bidiRunArr[i6].level = bidi.paraLevel;
                }
            }
        }
        if (bidi.insertPoints.size > 0) {
            for (int i10 = 0; i10 < bidi.insertPoints.size; i10++) {
                Bidi.Point point = bidi.insertPoints.points[i10];
                BidiRun bidiRun2 = bidi.runs[getRunFromLogicalIndex(bidi, point.pos)];
                bidiRun2.insertRemove = point.flag | bidiRun2.insertRemove;
            }
        }
        if (bidi.controlCount > 0) {
            for (int i11 = 0; i11 < bidi.length; i11++) {
                if (Bidi.IsBidiControlChar(bidi.text[i11])) {
                    bidi.runs[getRunFromLogicalIndex(bidi, i11)].insertRemove--;
                }
            }
        }
    }

    static int[] prepareReorder(byte[] bArr, byte[] bArr2, byte[] bArr3) {
        if (bArr == null || bArr.length <= 0) {
            return null;
        }
        int length = bArr.length;
        byte b = 126;
        byte b2 = 0;
        while (length > 0) {
            length--;
            byte b3 = bArr[length];
            if (b3 < 0 || b3 > 126) {
                return null;
            }
            if (b3 < b) {
                b = b3;
            }
            if (b3 > b2) {
                b2 = b3;
            }
        }
        bArr2[0] = b;
        bArr3[0] = b2;
        int[] iArr = new int[bArr.length];
        int length2 = bArr.length;
        while (length2 > 0) {
            length2--;
            iArr[length2] = length2;
        }
        return iArr;
    }

    static int[] reorderLogical(byte[] bArr) {
        byte[] bArr2 = new byte[1];
        byte[] bArr3 = new byte[1];
        int[] iArrPrepareReorder = prepareReorder(bArr, bArr2, bArr3);
        if (iArrPrepareReorder == null) {
            return null;
        }
        byte b = bArr2[0];
        byte b2 = bArr3[0];
        if (b == b2 && (b & 1) == 0) {
            return iArrPrepareReorder;
        }
        byte b3 = (byte) (b | 1);
        do {
            int i = 0;
            while (true) {
                if (i < bArr.length && bArr[i] < b2) {
                    i++;
                } else {
                    if (i >= bArr.length) {
                        break;
                    }
                    int i2 = i;
                    do {
                        i2++;
                        if (i2 >= bArr.length) {
                            break;
                        }
                    } while (bArr[i2] >= b2);
                    int i3 = (i + i2) - 1;
                    do {
                        iArrPrepareReorder[i] = i3 - iArrPrepareReorder[i];
                        i++;
                    } while (i < i2);
                    if (i2 == bArr.length) {
                        break;
                    }
                    i = i2 + 1;
                }
            }
            b2 = (byte) (b2 - 1);
        } while (b2 >= b3);
        return iArrPrepareReorder;
    }

    static int[] reorderVisual(byte[] bArr) {
        byte[] bArr2 = new byte[1];
        byte[] bArr3 = new byte[1];
        int[] iArrPrepareReorder = prepareReorder(bArr, bArr2, bArr3);
        if (iArrPrepareReorder == null) {
            return null;
        }
        byte b = bArr2[0];
        byte b2 = bArr3[0];
        if (b == b2 && (b & 1) == 0) {
            return iArrPrepareReorder;
        }
        byte b3 = (byte) (b | 1);
        do {
            int i = 0;
            while (true) {
                if (i < bArr.length && bArr[i] < b2) {
                    i++;
                } else {
                    if (i >= bArr.length) {
                        break;
                    }
                    int i2 = i;
                    do {
                        i2++;
                        if (i2 >= bArr.length) {
                            break;
                        }
                    } while (bArr[i2] >= b2);
                    for (int i3 = i2 - 1; i < i3; i3--) {
                        int i4 = iArrPrepareReorder[i];
                        iArrPrepareReorder[i] = iArrPrepareReorder[i3];
                        iArrPrepareReorder[i3] = i4;
                        i++;
                    }
                    if (i2 == bArr.length) {
                        break;
                    }
                    i = i2 + 1;
                }
            }
            b2 = (byte) (b2 - 1);
        } while (b2 >= b3);
        return iArrPrepareReorder;
    }

    static int getVisualIndex(Bidi bidi, int i) {
        int i2;
        int i3;
        int i4;
        int i5 = 0;
        switch (bidi.direction) {
            case 0:
                i2 = i;
                break;
            case 1:
                i2 = (bidi.length - i) - 1;
                break;
            default:
                getRuns(bidi);
                BidiRun[] bidiRunArr = bidi.runs;
                int i6 = 0;
                int i7 = 0;
                while (true) {
                    if (i6 < bidi.runCount) {
                        int i8 = bidiRunArr[i6].limit - i7;
                        int i9 = i - bidiRunArr[i6].start;
                        if (i9 >= 0 && i9 < i8) {
                            i4 = bidiRunArr[i6].isEvenRun() ? i7 + i9 : ((i7 + i8) - i9) - 1;
                        } else {
                            i7 += i8;
                            i6++;
                        }
                    } else {
                        i4 = -1;
                    }
                }
                if (i6 >= bidi.runCount) {
                    return -1;
                }
                i2 = i4;
                break;
        }
        if (bidi.insertPoints.size > 0) {
            BidiRun[] bidiRunArr2 = bidi.runs;
            int i10 = 0;
            while (true) {
                int i11 = bidiRunArr2[i5].limit;
                int i12 = bidiRunArr2[i5].insertRemove;
                if ((i12 & 5) > 0) {
                    i10++;
                }
                if (i2 < bidiRunArr2[i5].limit) {
                    return i2 + i10;
                }
                if ((i12 & 10) > 0) {
                    i10++;
                }
                i5++;
            }
        } else if (bidi.controlCount > 0) {
            BidiRun[] bidiRunArr3 = bidi.runs;
            if (Bidi.IsBidiControlChar(bidi.text[i])) {
                return -1;
            }
            int i13 = 0;
            int i14 = 0;
            while (true) {
                int i15 = bidiRunArr3[i5].limit - i13;
                int i16 = bidiRunArr3[i5].insertRemove;
                if (i2 >= bidiRunArr3[i5].limit) {
                    i14 -= i16;
                    i5++;
                    i13 += i15;
                } else {
                    if (i16 == 0) {
                        return i2 - i14;
                    }
                    if (bidiRunArr3[i5].isEvenRun()) {
                        i3 = bidiRunArr3[i5].start;
                    } else {
                        i3 = i + 1;
                        i = bidiRunArr3[i5].start + i15;
                    }
                    while (i3 < i) {
                        if (Bidi.IsBidiControlChar(bidi.text[i3])) {
                            i14++;
                        }
                        i3++;
                    }
                    return i2 - i14;
                }
            }
        } else {
            return i2;
        }
    }

    static int getLogicalIndex(Bidi bidi, int i) {
        int i2;
        int i3;
        int i4;
        BidiRun[] bidiRunArr = bidi.runs;
        int i5 = bidi.runCount;
        int i6 = 0;
        if (bidi.insertPoints.size > 0) {
            int i7 = 0;
            int i8 = 0;
            int i9 = 0;
            while (true) {
                int i10 = bidiRunArr[i7].limit - i8;
                int i11 = bidiRunArr[i7].insertRemove;
                if ((i11 & 5) > 0) {
                    if (i <= i8 + i9) {
                        return -1;
                    }
                    i9++;
                }
                if (i < bidiRunArr[i7].limit + i9) {
                    i -= i9;
                    break;
                }
                if ((i11 & 10) > 0) {
                    if (i == i8 + i10 + i9) {
                        return -1;
                    }
                    i9++;
                }
                i7++;
                i8 += i10;
            }
        } else if (bidi.controlCount > 0) {
            int i12 = 0;
            int i13 = 0;
            int i14 = 0;
            while (true) {
                i2 = bidiRunArr[i12].limit - i13;
                i3 = bidiRunArr[i12].insertRemove;
                if (i < (bidiRunArr[i12].limit - i14) + i3) {
                    break;
                }
                i14 -= i3;
                i12++;
                i13 += i2;
            }
            if (i3 == 0) {
                i += i14;
            } else {
                int i15 = bidiRunArr[i12].start;
                boolean zIsEvenRun = bidiRunArr[i12].isEvenRun();
                int i16 = (i15 + i2) - 1;
                int i17 = i14;
                for (int i18 = 0; i18 < i2; i18++) {
                    if (Bidi.IsBidiControlChar(bidi.text[zIsEvenRun ? i15 + i18 : i16 - i18])) {
                        i17++;
                    }
                    if (i + i17 == i13 + i18) {
                        break;
                    }
                }
                i += i17;
            }
        }
        if (i5 <= 10) {
            while (i >= bidiRunArr[i6].limit) {
                i6++;
            }
            i4 = i6;
        } else {
            while (true) {
                i4 = (i6 + i5) >>> 1;
                if (i >= bidiRunArr[i4].limit) {
                    i6 = i4 + 1;
                } else {
                    if (i4 == 0 || i >= bidiRunArr[i4 - 1].limit) {
                        break;
                    }
                    i5 = i4;
                }
            }
        }
        int i19 = bidiRunArr[i4].start;
        if (bidiRunArr[i4].isEvenRun()) {
            if (i4 > 0) {
                i -= bidiRunArr[i4 - 1].limit;
            }
            return i19 + i;
        }
        return ((i19 + bidiRunArr[i4].limit) - i) - 1;
    }

    static int[] getLogicalMap(Bidi bidi) {
        int i;
        int i2;
        BidiRun[] bidiRunArr = bidi.runs;
        int[] iArr = new int[bidi.length];
        if (bidi.length > bidi.resultLength) {
            Arrays.fill(iArr, -1);
        }
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < bidi.runCount; i5++) {
            int i6 = bidiRunArr[i5].start;
            int i7 = bidiRunArr[i5].limit;
            if (bidiRunArr[i5].isEvenRun()) {
                while (true) {
                    int i8 = i6 + 1;
                    i2 = i4 + 1;
                    iArr[i6] = i4;
                    if (i2 >= i7) {
                        break;
                    }
                    i6 = i8;
                    i4 = i2;
                }
                i4 = i2;
            } else {
                int i9 = i6 + (i7 - i4);
                while (true) {
                    i9--;
                    i = i4 + 1;
                    iArr[i9] = i4;
                    if (i >= i7) {
                        break;
                    }
                    i4 = i;
                }
                i4 = i;
            }
        }
        if (bidi.insertPoints.size > 0) {
            int i10 = bidi.runCount;
            BidiRun[] bidiRunArr2 = bidi.runs;
            int i11 = 0;
            int i12 = 0;
            while (i3 < i10) {
                int i13 = bidiRunArr2[i3].limit - i11;
                int i14 = bidiRunArr2[i3].insertRemove;
                if ((i14 & 5) > 0) {
                    i12++;
                }
                if (i12 > 0) {
                    int i15 = bidiRunArr2[i3].start;
                    int i16 = i15 + i13;
                    while (i15 < i16) {
                        iArr[i15] = iArr[i15] + i12;
                        i15++;
                    }
                }
                if ((i14 & 10) > 0) {
                    i12++;
                }
                i3++;
                i11 += i13;
            }
        } else if (bidi.controlCount > 0) {
            int i17 = bidi.runCount;
            BidiRun[] bidiRunArr3 = bidi.runs;
            int i18 = 0;
            int i19 = 0;
            int i20 = 0;
            while (i18 < i17) {
                int i21 = bidiRunArr3[i18].limit - i19;
                int i22 = bidiRunArr3[i18].insertRemove;
                if (i20 - i22 != 0) {
                    int i23 = bidiRunArr3[i18].start;
                    boolean zIsEvenRun = bidiRunArr3[i18].isEvenRun();
                    int i24 = i23 + i21;
                    if (i22 == 0) {
                        while (i23 < i24) {
                            iArr[i23] = iArr[i23] - i20;
                            i23++;
                        }
                    } else {
                        int i25 = i20;
                        for (int i26 = 0; i26 < i21; i26++) {
                            int i27 = zIsEvenRun ? i23 + i26 : (i24 - i26) - 1;
                            if (Bidi.IsBidiControlChar(bidi.text[i27])) {
                                i25++;
                                iArr[i27] = -1;
                            } else {
                                iArr[i27] = iArr[i27] - i25;
                            }
                        }
                        i20 = i25;
                    }
                }
                i18++;
                i19 += i21;
            }
        }
        return iArr;
    }

    static int[] getVisualMap(Bidi bidi) {
        int i;
        int i2;
        BidiRun[] bidiRunArr = bidi.runs;
        int i3 = bidi.length > bidi.resultLength ? bidi.length : bidi.resultLength;
        int[] iArr = new int[i3];
        int i4 = 0;
        int i5 = 0;
        for (int i6 = 0; i6 < bidi.runCount; i6++) {
            int i7 = bidiRunArr[i6].start;
            int i8 = bidiRunArr[i6].limit;
            if (bidiRunArr[i6].isEvenRun()) {
                while (true) {
                    i2 = i5 + 1;
                    int i9 = i7 + 1;
                    iArr[i5] = i7;
                    i4++;
                    if (i4 >= i8) {
                        break;
                    }
                    i5 = i2;
                    i7 = i9;
                }
                i5 = i2;
            } else {
                int i10 = i7 + (i8 - i4);
                while (true) {
                    i = i5 + 1;
                    i10--;
                    iArr[i5] = i10;
                    i4++;
                    if (i4 >= i8) {
                        break;
                    }
                    i5 = i;
                }
                i5 = i;
            }
        }
        if (bidi.insertPoints.size > 0) {
            int i11 = bidi.runCount;
            BidiRun[] bidiRunArr2 = bidi.runs;
            int i12 = 0;
            for (int i13 = 0; i13 < i11; i13++) {
                int i14 = bidiRunArr2[i13].insertRemove;
                if ((i14 & 5) > 0) {
                    i12++;
                }
                if ((i14 & 10) > 0) {
                    i12++;
                }
            }
            int i15 = bidi.resultLength;
            int i16 = i11 - 1;
            while (i16 >= 0 && i12 > 0) {
                int i17 = bidiRunArr2[i16].insertRemove;
                if ((i17 & 10) > 0) {
                    i15--;
                    iArr[i15] = -1;
                    i12--;
                }
                int i18 = i16 > 0 ? bidiRunArr2[i16 - 1].limit : 0;
                for (int i19 = bidiRunArr2[i16].limit - 1; i19 >= i18 && i12 > 0; i19--) {
                    i15--;
                    iArr[i15] = iArr[i19];
                }
                if ((i17 & 5) > 0) {
                    i15--;
                    iArr[i15] = -1;
                    i12--;
                }
                i16--;
            }
        } else if (bidi.controlCount > 0) {
            int i20 = bidi.runCount;
            BidiRun[] bidiRunArr3 = bidi.runs;
            int i21 = 0;
            int i22 = 0;
            int i23 = 0;
            while (i21 < i20) {
                int i24 = bidiRunArr3[i21].limit - i22;
                int i25 = bidiRunArr3[i21].insertRemove;
                if (i25 == 0 && i23 == i22) {
                    i23 += i24;
                } else if (i25 == 0) {
                    int i26 = bidiRunArr3[i21].limit;
                    int i27 = i23;
                    int i28 = i22;
                    while (i28 < i26) {
                        iArr[i27] = iArr[i28];
                        i28++;
                        i27++;
                    }
                    i23 = i27;
                } else {
                    int i29 = bidiRunArr3[i21].start;
                    boolean zIsEvenRun = bidiRunArr3[i21].isEvenRun();
                    int i30 = (i29 + i24) - 1;
                    int i31 = i23;
                    for (int i32 = 0; i32 < i24; i32++) {
                        int i33 = zIsEvenRun ? i29 + i32 : i30 - i32;
                        if (!Bidi.IsBidiControlChar(bidi.text[i33])) {
                            iArr[i31] = i33;
                            i31++;
                        }
                    }
                    i23 = i31;
                }
                i21++;
                i22 += i24;
            }
        }
        if (i3 == bidi.resultLength) {
            return iArr;
        }
        int[] iArr2 = new int[bidi.resultLength];
        System.arraycopy(iArr, 0, iArr2, 0, bidi.resultLength);
        return iArr2;
    }

    static int[] invertMap(int[] iArr) {
        int length = iArr.length;
        int i = -1;
        int i2 = 0;
        for (int i3 : iArr) {
            if (i3 > i) {
                i = i3;
            }
            if (i3 >= 0) {
                i2++;
            }
        }
        int i4 = i + 1;
        int[] iArr2 = new int[i4];
        if (i2 < i4) {
            Arrays.fill(iArr2, -1);
        }
        for (int i5 = 0; i5 < length; i5++) {
            int i6 = iArr[i5];
            if (i6 >= 0) {
                iArr2[i6] = i5;
            }
        }
        return iArr2;
    }
}
