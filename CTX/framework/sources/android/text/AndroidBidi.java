package android.text;

import android.icu.lang.UCharacter;
import android.icu.text.Bidi;
import android.icu.text.BidiClassifier;
import android.text.Layout;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting(visibility = VisibleForTesting.Visibility.PACKAGE)
public class AndroidBidi {
    private static final EmojiBidiOverride sEmojiBidiOverride = new EmojiBidiOverride();

    public static class EmojiBidiOverride extends BidiClassifier {
        private static final int NO_OVERRIDE = UCharacter.getIntPropertyMaxValue(4096) + 1;

        public EmojiBidiOverride() {
            super(null);
        }

        @Override
        public int classify(int i) {
            if (Emoji.isNewEmoji(i)) {
                return 10;
            }
            return NO_OVERRIDE;
        }
    }

    public static int bidi(int i, char[] cArr, byte[] bArr) {
        byte b;
        if (cArr == null || bArr == null) {
            throw new NullPointerException();
        }
        int length = cArr.length;
        if (bArr.length < length) {
            throw new IndexOutOfBoundsException();
        }
        switch (i) {
            case -2:
                b = 127;
                break;
            case -1:
                b = 1;
                break;
            case 0:
            case 1:
            default:
                b = 0;
                break;
            case 2:
                b = 126;
                break;
        }
        Bidi bidi = new Bidi(length, 0);
        bidi.setCustomClassifier(sEmojiBidiOverride);
        bidi.setPara(cArr, b, (byte[]) null);
        for (int i2 = 0; i2 < length; i2++) {
            bArr[i2] = bidi.getLevelAt(i2);
        }
        return (bidi.getParaLevel() & 1) == 0 ? 1 : -1;
    }

    public static Layout.Directions directions(int i, byte[] bArr, int i2, char[] cArr, int i3, int i4) {
        int i5;
        int i6;
        boolean z;
        if (i4 == 0) {
            return Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        int i7 = i == 1 ? 0 : 1;
        int i8 = bArr[i2];
        int i9 = i2 + i4;
        int i10 = 1;
        int i11 = i8;
        for (int i12 = i2 + 1; i12 < i9; i12++) {
            int i13 = bArr[i12];
            if (i13 != i11) {
                i10++;
                i11 = i13;
            }
        }
        if ((i11 & 1) != (i7 & 1)) {
            int i14 = i4;
            while (true) {
                i14--;
                if (i14 < 0) {
                    break;
                }
                char c = cArr[i3 + i14];
                if (c == '\n') {
                    i14--;
                    break;
                }
                if (c != ' ' && c != '\t') {
                    break;
                }
            }
            i5 = i14 + 1;
            if (i5 != i4) {
                i10++;
            }
        } else {
            i5 = i4;
        }
        if (i10 == 1 && i8 == i7) {
            return (i8 & 1) != 0 ? Layout.DIRS_ALL_RIGHT_TO_LEFT : Layout.DIRS_ALL_LEFT_TO_RIGHT;
        }
        int[] iArr = new int[i10 * 2];
        int i15 = i2 + i5;
        int i16 = i2;
        int i17 = 1;
        int i18 = i8;
        int i19 = i18;
        int i20 = i8 << 26;
        int i21 = i19;
        for (int i22 = i16; i22 < i15; i22++) {
            int i23 = bArr[i22];
            if (i23 != i21) {
                if (i23 > i19) {
                    i19 = i23;
                } else if (i23 < i18) {
                    i18 = i23;
                }
                int i24 = i17 + 1;
                iArr[i17] = (i22 - i16) | i20;
                i17 = i24 + 1;
                iArr[i24] = i22 - i2;
                i16 = i22;
                i20 = i23 << 26;
                i21 = i23;
            }
        }
        iArr[i17] = (i15 - i16) | i20;
        if (i5 < i4) {
            int i25 = i17 + 1;
            iArr[i25] = i5;
            iArr[i25 + 1] = (i4 - i5) | (i7 << 26);
        }
        if ((i18 & 1) == i7) {
            i18++;
            z = i19 > i18;
            i6 = 1;
        } else {
            i6 = 1;
            z = i10 > 1;
        }
        if (z) {
            for (int i26 = i19 - i6; i26 >= i18; i26--) {
                int i27 = 0;
                while (i27 < iArr.length) {
                    if (bArr[iArr[i27]] >= i26) {
                        int i28 = i27 + 2;
                        while (i28 < iArr.length && bArr[iArr[i28]] >= i26) {
                            i28 += 2;
                        }
                        for (int i29 = i28 - 2; i27 < i29; i29 -= 2) {
                            int i30 = iArr[i27];
                            iArr[i27] = iArr[i29];
                            iArr[i29] = i30;
                            int i31 = i27 + 1;
                            int i32 = iArr[i31];
                            int i33 = i29 + 1;
                            iArr[i31] = iArr[i33];
                            iArr[i33] = i32;
                            i27 += 2;
                        }
                        i27 = i28 + 2;
                    }
                    i27 += 2;
                }
            }
        }
        return new Layout.Directions(iArr);
    }
}
