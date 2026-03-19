package android.icu.text;

import android.icu.impl.Normalizer2Impl;

public final class UnicodeCompressor implements SCSU {
    private static boolean[] sSingleTagTable = {false, true, true, true, true, true, true, true, true, false, false, true, true, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private static boolean[] sUnicodeTagTable = {false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false};
    private int fCurrentWindow = 0;
    private int[] fOffsets = new int[8];
    private int fMode = 0;
    private int[] fIndexCount = new int[256];
    private int[] fTimeStamps = new int[8];
    private int fTimeStamp = 0;

    public UnicodeCompressor() {
        reset();
    }

    public static byte[] compress(String str) {
        return compress(str.toCharArray(), 0, str.length());
    }

    public static byte[] compress(char[] cArr, int i, int i2) {
        UnicodeCompressor unicodeCompressor = new UnicodeCompressor();
        int iMax = Math.max(4, (3 * (i2 - i)) + 1);
        byte[] bArr = new byte[iMax];
        int iCompress = unicodeCompressor.compress(cArr, i, i2, null, bArr, 0, iMax);
        byte[] bArr2 = new byte[iCompress];
        System.arraycopy(bArr, 0, bArr2, 0, iCompress);
        return bArr2;
    }

    public int compress(char[] cArr, int i, int i2, int[] iArr, byte[] bArr, int i3, int i4) {
        int i5;
        if (bArr.length < 4 || i4 - i3 < 4) {
            throw new IllegalArgumentException("byteBuffer.length < 4");
        }
        int i6 = i;
        int i7 = i3;
        while (true) {
            byte b = 0;
            if (i6 < i2 && i7 < i4) {
                char c = 128;
                switch (this.fMode) {
                    case 0:
                        while (i6 < i2 && i7 < i4) {
                            int i8 = i6 + 1;
                            char c2 = cArr[i6];
                            int i9 = i8 < i2 ? cArr[i8] : -1;
                            if (c2 < 128) {
                                int i10 = c2 & 255;
                                if (sSingleTagTable[i10]) {
                                    int i11 = i7 + 1;
                                    if (i11 >= i4) {
                                        i6 = i8 - 1;
                                    } else {
                                        bArr[i7] = 1;
                                        i7 = i11;
                                    }
                                }
                                i5 = i7 + 1;
                                bArr[i7] = (byte) i10;
                                i6 = i8;
                                i7 = i5;
                            } else {
                                if (inDynamicWindow(c2, this.fCurrentWindow)) {
                                    i5 = i7 + 1;
                                    bArr[i7] = (byte) ((c2 - this.fOffsets[this.fCurrentWindow]) + 128);
                                } else if (isCompressible(c2)) {
                                    int iFindDynamicWindow = findDynamicWindow(c2);
                                    if (iFindDynamicWindow != -1) {
                                        int i12 = i8 + 1;
                                        int i13 = i12 < i2 ? cArr[i12] : -1;
                                        if (inDynamicWindow(i9, iFindDynamicWindow) && inDynamicWindow(i13, iFindDynamicWindow)) {
                                            int i14 = i7 + 1;
                                            if (i14 >= i4) {
                                                i6 = i8 - 1;
                                            } else {
                                                bArr[i7] = (byte) (16 + iFindDynamicWindow);
                                                i7 = i14 + 1;
                                                bArr[i14] = (byte) ((c2 - this.fOffsets[iFindDynamicWindow]) + 128);
                                                int[] iArr2 = this.fTimeStamps;
                                                int i15 = this.fTimeStamp + 1;
                                                this.fTimeStamp = i15;
                                                iArr2[iFindDynamicWindow] = i15;
                                                this.fCurrentWindow = iFindDynamicWindow;
                                                i6 = i8;
                                            }
                                        } else {
                                            int i16 = i7 + 1;
                                            if (i16 >= i4) {
                                                i6 = i8 - 1;
                                            } else {
                                                bArr[i7] = (byte) (1 + iFindDynamicWindow);
                                                i7 = i16 + 1;
                                                bArr[i16] = (byte) ((c2 - this.fOffsets[iFindDynamicWindow]) + 128);
                                                i6 = i8;
                                            }
                                        }
                                    } else {
                                        int iFindStaticWindow = findStaticWindow(c2);
                                        if (iFindStaticWindow == -1 || inStaticWindow(i9, iFindStaticWindow)) {
                                            int iMakeIndex = makeIndex(c2);
                                            int[] iArr3 = this.fIndexCount;
                                            iArr3[iMakeIndex] = iArr3[iMakeIndex] + 1;
                                            int i17 = i8 + 1;
                                            int i18 = i17 < i2 ? cArr[i17] : -1;
                                            if (this.fIndexCount[iMakeIndex] <= 1 && !(iMakeIndex == makeIndex(i9) && iMakeIndex == makeIndex(i18))) {
                                                if (i7 + 3 < i4) {
                                                    int i19 = i7 + 1;
                                                    bArr[i7] = 15;
                                                    int i20 = c2 >>> '\b';
                                                    int i21 = c2 & 255;
                                                    if (sUnicodeTagTable[i20]) {
                                                        bArr[i19] = -16;
                                                        i19++;
                                                    }
                                                    int i22 = i19 + 1;
                                                    bArr[i19] = (byte) i20;
                                                    i7 = i22 + 1;
                                                    bArr[i22] = (byte) i21;
                                                    this.fMode = 1;
                                                    i6 = i8;
                                                } else {
                                                    i6 = i8 - 1;
                                                }
                                            } else if (i7 + 2 >= i4) {
                                                i6 = i8 - 1;
                                            } else {
                                                int lRDefinedWindow = getLRDefinedWindow();
                                                int i23 = i7 + 1;
                                                bArr[i7] = (byte) (24 + lRDefinedWindow);
                                                int i24 = i23 + 1;
                                                bArr[i23] = (byte) iMakeIndex;
                                                int i25 = i24 + 1;
                                                bArr[i24] = (byte) ((c2 - sOffsetTable[iMakeIndex]) + 128);
                                                this.fOffsets[lRDefinedWindow] = sOffsetTable[iMakeIndex];
                                                this.fCurrentWindow = lRDefinedWindow;
                                                int[] iArr4 = this.fTimeStamps;
                                                int i26 = this.fTimeStamp + 1;
                                                this.fTimeStamp = i26;
                                                iArr4[lRDefinedWindow] = i26;
                                                i6 = i8;
                                                i7 = i25;
                                            }
                                        } else {
                                            int i27 = i7 + 1;
                                            if (i27 >= i4) {
                                                i6 = i8 - 1;
                                            } else {
                                                bArr[i7] = (byte) (1 + iFindStaticWindow);
                                                i7 = i27 + 1;
                                                bArr[i27] = (byte) (c2 - sOffsets[iFindStaticWindow]);
                                                i6 = i8;
                                            }
                                        }
                                    }
                                } else if (i9 == -1 || !isCompressible(i9)) {
                                    if (i7 + 3 < i4) {
                                        int i28 = i7 + 1;
                                        bArr[i7] = 15;
                                        int i29 = c2 >>> '\b';
                                        int i30 = c2 & 255;
                                        if (sUnicodeTagTable[i29]) {
                                            bArr[i28] = -16;
                                            i28++;
                                        }
                                        int i31 = i28 + 1;
                                        bArr[i28] = (byte) i29;
                                        i7 = i31 + 1;
                                        bArr[i31] = (byte) i30;
                                        this.fMode = 1;
                                        i6 = i8;
                                    } else {
                                        i6 = i8 - 1;
                                    }
                                } else if (i7 + 2 >= i4) {
                                    i6 = i8 - 1;
                                } else {
                                    int i32 = i7 + 1;
                                    bArr[i7] = 14;
                                    int i33 = i32 + 1;
                                    bArr[i32] = (byte) (c2 >>> '\b');
                                    i5 = i33 + 1;
                                    bArr[i33] = (byte) (c2 & 255);
                                }
                                i6 = i8;
                                i7 = i5;
                            }
                            break;
                        }
                        break;
                    case 1:
                        while (i6 < i2 && i7 < i4) {
                            int i34 = i6 + 1;
                            char c3 = cArr[i6];
                            int i35 = i34 < i2 ? cArr[i34] : -1;
                            if (isCompressible(c3) && (i35 == -1 || isCompressible(i35))) {
                                if (c3 < c) {
                                    int i36 = c3 & 255;
                                    if (i35 == -1 || i35 >= c || sSingleTagTable[i36]) {
                                        int i37 = i7 + 1;
                                        if (i37 >= i4) {
                                            i6 = i34 - 1;
                                        } else {
                                            bArr[i7] = b;
                                            i7 = i37 + 1;
                                            bArr[i37] = (byte) i36;
                                            i6 = i34;
                                        }
                                    } else {
                                        int i38 = i7 + 1;
                                        if (i38 < i4) {
                                            int i39 = this.fCurrentWindow;
                                            bArr[i7] = (byte) (224 + i39);
                                            i7 = i38 + 1;
                                            bArr[i38] = (byte) i36;
                                            int[] iArr5 = this.fTimeStamps;
                                            int i40 = this.fTimeStamp + 1;
                                            this.fTimeStamp = i40;
                                            iArr5[i39] = i40;
                                            this.fMode = b;
                                            i6 = i34;
                                        } else {
                                            i6 = i34 - 1;
                                        }
                                    }
                                } else {
                                    int iFindDynamicWindow2 = findDynamicWindow(c3);
                                    if (iFindDynamicWindow2 == -1) {
                                        int iMakeIndex2 = makeIndex(c3);
                                        int[] iArr6 = this.fIndexCount;
                                        iArr6[iMakeIndex2] = iArr6[iMakeIndex2] + 1;
                                        int i41 = i34 + 1;
                                        int i42 = i41 < i2 ? cArr[i41] : -1;
                                        if (this.fIndexCount[iMakeIndex2] > 1 || (iMakeIndex2 == makeIndex(i35) && iMakeIndex2 == makeIndex(i42))) {
                                            if (i7 + 2 < i4) {
                                                int lRDefinedWindow2 = getLRDefinedWindow();
                                                int i43 = i7 + 1;
                                                bArr[i7] = (byte) (232 + lRDefinedWindow2);
                                                int i44 = i43 + 1;
                                                bArr[i43] = (byte) iMakeIndex2;
                                                int i45 = i44 + 1;
                                                bArr[i44] = (byte) ((c3 - sOffsetTable[iMakeIndex2]) + 128);
                                                this.fOffsets[lRDefinedWindow2] = sOffsetTable[iMakeIndex2];
                                                this.fCurrentWindow = lRDefinedWindow2;
                                                int[] iArr7 = this.fTimeStamps;
                                                int i46 = this.fTimeStamp + 1;
                                                this.fTimeStamp = i46;
                                                iArr7[lRDefinedWindow2] = i46;
                                                this.fMode = 0;
                                                i6 = i34;
                                                i7 = i45;
                                            } else {
                                                i6 = i34 - 1;
                                            }
                                        } else if (i7 + 2 >= i4) {
                                            i6 = i34 - 1;
                                        } else {
                                            int i47 = c3 >>> '\b';
                                            int i48 = c3 & 255;
                                            if (sUnicodeTagTable[i47]) {
                                                bArr[i7] = -16;
                                                i7++;
                                            }
                                            int i49 = i7 + 1;
                                            bArr[i7] = (byte) i47;
                                            i7 = i49 + 1;
                                            bArr[i49] = (byte) i48;
                                            i6 = i34;
                                            b = 0;
                                            c = 128;
                                        }
                                    } else if (inDynamicWindow(i35, iFindDynamicWindow2)) {
                                        int i50 = i7 + 1;
                                        if (i50 < i4) {
                                            bArr[i7] = (byte) (224 + iFindDynamicWindow2);
                                            i7 = i50 + 1;
                                            bArr[i50] = (byte) ((c3 - this.fOffsets[iFindDynamicWindow2]) + c);
                                            int[] iArr8 = this.fTimeStamps;
                                            int i51 = this.fTimeStamp + 1;
                                            this.fTimeStamp = i51;
                                            iArr8[iFindDynamicWindow2] = i51;
                                            this.fCurrentWindow = iFindDynamicWindow2;
                                            this.fMode = b;
                                            i6 = i34;
                                        } else {
                                            i6 = i34 - 1;
                                        }
                                    } else if (i7 + 2 >= i4) {
                                        i6 = i34 - 1;
                                    } else {
                                        int i52 = c3 >>> '\b';
                                        int i53 = c3 & 255;
                                        if (sUnicodeTagTable[i52]) {
                                            bArr[i7] = -16;
                                            i7++;
                                        }
                                        int i54 = i7 + 1;
                                        bArr[i7] = (byte) i52;
                                        i7 = i54 + 1;
                                        bArr[i54] = (byte) i53;
                                        i6 = i34;
                                    }
                                }
                            } else if (i7 + 2 >= i4) {
                                i6 = i34 - 1;
                            } else {
                                int i55 = c3 >>> '\b';
                                int i56 = c3 & 255;
                                if (sUnicodeTagTable[i55]) {
                                    bArr[i7] = -16;
                                    i7++;
                                }
                                int i57 = i7 + 1;
                                bArr[i7] = (byte) i55;
                                i7 = i57 + 1;
                                bArr[i57] = (byte) i56;
                                i6 = i34;
                                b = 0;
                                c = 128;
                            }
                            break;
                        }
                        break;
                }
            }
        }
        if (iArr != null) {
            iArr[0] = i6 - i;
        }
        return i7 - i3;
    }

    public void reset() {
        this.fOffsets[0] = 128;
        this.fOffsets[1] = 192;
        this.fOffsets[2] = 1024;
        this.fOffsets[3] = 1536;
        this.fOffsets[4] = 2304;
        this.fOffsets[5] = 12352;
        this.fOffsets[6] = 12448;
        this.fOffsets[7] = 65280;
        for (int i = 0; i < 8; i++) {
            this.fTimeStamps[i] = 0;
        }
        for (int i2 = 0; i2 <= 255; i2++) {
            this.fIndexCount[i2] = 0;
        }
        this.fTimeStamp = 0;
        this.fCurrentWindow = 0;
        this.fMode = 0;
    }

    private static int makeIndex(int i) {
        if (i >= 192 && i < 320) {
            return 249;
        }
        if (i >= 592 && i < 720) {
            return 250;
        }
        if (i >= 880 && i < 1008) {
            return 251;
        }
        if (i >= 1328 && i < 1424) {
            return 252;
        }
        if (i >= 12352 && i < 12448) {
            return 253;
        }
        if (i >= 12448 && i < 12576) {
            return 254;
        }
        if (i >= 65376 && i < 65439) {
            return 255;
        }
        if (i >= 128 && i < 13312) {
            return (i / 128) & 255;
        }
        if (i >= 57344 && i <= 65535) {
            return ((i - Normalizer2Impl.Hangul.HANGUL_BASE) / 128) & 255;
        }
        return 0;
    }

    private boolean inDynamicWindow(int i, int i2) {
        return i >= this.fOffsets[i2] && i < this.fOffsets[i2] + 128;
    }

    private static boolean inStaticWindow(int i, int i2) {
        return i >= sOffsets[i2] && i < sOffsets[i2] + 128;
    }

    private static boolean isCompressible(int i) {
        return i < 13312 || i >= 57344;
    }

    private int findDynamicWindow(int i) {
        for (int i2 = 7; i2 >= 0; i2--) {
            if (inDynamicWindow(i, i2)) {
                int[] iArr = this.fTimeStamps;
                iArr[i2] = iArr[i2] + 1;
                return i2;
            }
        }
        return -1;
    }

    private static int findStaticWindow(int i) {
        for (int i2 = 7; i2 >= 0; i2--) {
            if (inStaticWindow(i, i2)) {
                return i2;
            }
        }
        return -1;
    }

    private int getLRDefinedWindow() {
        int i = -1;
        int i2 = Integer.MAX_VALUE;
        for (int i3 = 7; i3 >= 0; i3--) {
            if (this.fTimeStamps[i3] < i2) {
                i2 = this.fTimeStamps[i3];
                i = i3;
            }
        }
        return i;
    }
}
