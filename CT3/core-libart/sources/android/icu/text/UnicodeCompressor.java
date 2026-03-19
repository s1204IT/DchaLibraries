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

    public static byte[] compress(String buffer) {
        return compress(buffer.toCharArray(), 0, buffer.length());
    }

    public static byte[] compress(char[] buffer, int start, int limit) {
        UnicodeCompressor comp = new UnicodeCompressor();
        int len = Math.max(4, ((limit - start) * 3) + 1);
        byte[] temp = new byte[len];
        int byteCount = comp.compress(buffer, start, limit, null, temp, 0, len);
        byte[] result = new byte[byteCount];
        System.arraycopy(temp, 0, result, 0, byteCount);
        return result;
    }

    public int compress(char[] cArr, int i, int i2, int[] iArr, byte[] bArr, int i3, int i4) {
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14 = i3;
        int i15 = i;
        if (bArr.length < 4 || i4 - i3 < 4) {
            throw new IllegalArgumentException("byteBuffer.length < 4");
        }
        while (true) {
            if (i15 < i2 && i14 < i4) {
                switch (this.fMode) {
                    case 0:
                        while (true) {
                            int i16 = i15;
                            int i17 = i14;
                            if (i16 < i2 && i17 < i4) {
                                i15 = i16 + 1;
                                char c = cArr[i16];
                                if (i15 < i2) {
                                    i10 = cArr[i15];
                                } else {
                                    i10 = -1;
                                }
                                if (c < 128) {
                                    int i18 = c & 255;
                                    if (!sSingleTagTable[i18]) {
                                        i13 = i17;
                                    } else if (i17 + 1 >= i4) {
                                        i15--;
                                        i14 = i17;
                                    } else {
                                        i13 = i17 + 1;
                                        bArr[i17] = 1;
                                    }
                                    bArr[i13] = (byte) i18;
                                    i14 = i13 + 1;
                                } else if (inDynamicWindow(c, this.fCurrentWindow)) {
                                    i14 = i17 + 1;
                                    bArr[i17] = (byte) ((c - this.fOffsets[this.fCurrentWindow]) + 128);
                                } else if (!isCompressible(c)) {
                                    if (i10 != -1 && isCompressible(i10)) {
                                        if (i17 + 2 >= i4) {
                                            i15--;
                                            i14 = i17;
                                        } else {
                                            int i19 = i17 + 1;
                                            bArr[i17] = 14;
                                            int i20 = i19 + 1;
                                            bArr[i19] = (byte) (c >>> '\b');
                                            i14 = i20 + 1;
                                            bArr[i20] = (byte) (c & 255);
                                        }
                                    }
                                } else {
                                    int iFindDynamicWindow = findDynamicWindow(c);
                                    if (iFindDynamicWindow != -1) {
                                        if (i15 + 1 < i2) {
                                            i12 = cArr[i15 + 1];
                                        } else {
                                            i12 = -1;
                                        }
                                        if (inDynamicWindow(i10, iFindDynamicWindow) && inDynamicWindow(i12, iFindDynamicWindow)) {
                                            if (i17 + 1 >= i4) {
                                                i15--;
                                                i14 = i17;
                                            } else {
                                                int i21 = i17 + 1;
                                                bArr[i17] = (byte) (iFindDynamicWindow + 16);
                                                bArr[i21] = (byte) ((c - this.fOffsets[iFindDynamicWindow]) + 128);
                                                int[] iArr2 = this.fTimeStamps;
                                                int i22 = this.fTimeStamp + 1;
                                                this.fTimeStamp = i22;
                                                iArr2[iFindDynamicWindow] = i22;
                                                this.fCurrentWindow = iFindDynamicWindow;
                                                i14 = i21 + 1;
                                            }
                                        } else if (i17 + 1 >= i4) {
                                            i15--;
                                            i14 = i17;
                                        } else {
                                            int i23 = i17 + 1;
                                            bArr[i17] = (byte) (iFindDynamicWindow + 1);
                                            bArr[i23] = (byte) ((c - this.fOffsets[iFindDynamicWindow]) + 128);
                                            i14 = i23 + 1;
                                        }
                                    } else {
                                        int iFindStaticWindow = findStaticWindow(c);
                                        if (iFindStaticWindow != -1 && !inStaticWindow(i10, iFindStaticWindow)) {
                                            if (i17 + 1 >= i4) {
                                                i15--;
                                                i14 = i17;
                                            } else {
                                                int i24 = i17 + 1;
                                                bArr[i17] = (byte) (iFindStaticWindow + 1);
                                                bArr[i24] = (byte) (c - sOffsets[iFindStaticWindow]);
                                                i14 = i24 + 1;
                                            }
                                        } else {
                                            int iMakeIndex = makeIndex(c);
                                            int[] iArr3 = this.fIndexCount;
                                            iArr3[iMakeIndex] = iArr3[iMakeIndex] + 1;
                                            if (i15 + 1 < i2) {
                                                i11 = cArr[i15 + 1];
                                            } else {
                                                i11 = -1;
                                            }
                                            if (this.fIndexCount[iMakeIndex] > 1 || (iMakeIndex == makeIndex(i10) && iMakeIndex == makeIndex(i11))) {
                                                if (i17 + 2 >= i4) {
                                                    i15--;
                                                    i14 = i17;
                                                } else {
                                                    int lRDefinedWindow = getLRDefinedWindow();
                                                    int i25 = i17 + 1;
                                                    bArr[i17] = (byte) (lRDefinedWindow + 24);
                                                    int i26 = i25 + 1;
                                                    bArr[i25] = (byte) iMakeIndex;
                                                    i14 = i26 + 1;
                                                    bArr[i26] = (byte) ((c - sOffsetTable[iMakeIndex]) + 128);
                                                    this.fOffsets[lRDefinedWindow] = sOffsetTable[iMakeIndex];
                                                    this.fCurrentWindow = lRDefinedWindow;
                                                    int[] iArr4 = this.fTimeStamps;
                                                    int i27 = this.fTimeStamp + 1;
                                                    this.fTimeStamp = i27;
                                                    iArr4[lRDefinedWindow] = i27;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        break;
                    case 1:
                        while (true) {
                            int i28 = i15;
                            int i29 = i14;
                            if (i28 < i2 && i29 < i4) {
                                i15 = i28 + 1;
                                char c2 = cArr[i28];
                                if (i15 < i2) {
                                    i5 = cArr[i15];
                                } else {
                                    i5 = -1;
                                }
                                if (!isCompressible(c2) || (i5 != -1 && !isCompressible(i5))) {
                                    if (i29 + 2 >= i4) {
                                        i15--;
                                        i14 = i29;
                                    } else {
                                        int i30 = c2 >>> '\b';
                                        int i31 = c2 & 255;
                                        if (sUnicodeTagTable[i30]) {
                                            i6 = i29 + 1;
                                            bArr[i29] = -16;
                                        } else {
                                            i6 = i29;
                                        }
                                        int i32 = i6 + 1;
                                        bArr[i6] = (byte) i30;
                                        i14 = i32 + 1;
                                        bArr[i32] = (byte) i31;
                                    }
                                } else if (c2 < 128) {
                                    int i33 = c2 & 255;
                                    if (i5 != -1 && i5 < 128 && !sSingleTagTable[i33]) {
                                        if (i29 + 1 >= i4) {
                                            i15--;
                                            i14 = i29;
                                        } else {
                                            int i34 = this.fCurrentWindow;
                                            int i35 = i29 + 1;
                                            bArr[i29] = (byte) (i34 + 224);
                                            bArr[i35] = (byte) i33;
                                            int[] iArr5 = this.fTimeStamps;
                                            int i36 = this.fTimeStamp + 1;
                                            this.fTimeStamp = i36;
                                            iArr5[i34] = i36;
                                            this.fMode = 0;
                                            i14 = i35 + 1;
                                        }
                                    } else if (i29 + 1 >= i4) {
                                        i15--;
                                        i14 = i29;
                                    } else {
                                        int i37 = i29 + 1;
                                        bArr[i29] = 0;
                                        bArr[i37] = (byte) i33;
                                        i14 = i37 + 1;
                                    }
                                } else {
                                    int iFindDynamicWindow2 = findDynamicWindow(c2);
                                    if (iFindDynamicWindow2 != -1) {
                                        if (inDynamicWindow(i5, iFindDynamicWindow2)) {
                                            if (i29 + 1 >= i4) {
                                                i15--;
                                                i14 = i29;
                                            } else {
                                                int i38 = i29 + 1;
                                                bArr[i29] = (byte) (iFindDynamicWindow2 + 224);
                                                bArr[i38] = (byte) ((c2 - this.fOffsets[iFindDynamicWindow2]) + 128);
                                                int[] iArr6 = this.fTimeStamps;
                                                int i39 = this.fTimeStamp + 1;
                                                this.fTimeStamp = i39;
                                                iArr6[iFindDynamicWindow2] = i39;
                                                this.fCurrentWindow = iFindDynamicWindow2;
                                                this.fMode = 0;
                                                i14 = i38 + 1;
                                            }
                                        } else if (i29 + 2 >= i4) {
                                            i15--;
                                            i14 = i29;
                                        } else {
                                            int i40 = c2 >>> '\b';
                                            int i41 = c2 & 255;
                                            if (sUnicodeTagTable[i40]) {
                                                i9 = i29 + 1;
                                                bArr[i29] = -16;
                                            } else {
                                                i9 = i29;
                                            }
                                            int i42 = i9 + 1;
                                            bArr[i9] = (byte) i40;
                                            i14 = i42 + 1;
                                            bArr[i42] = (byte) i41;
                                        }
                                    } else {
                                        int iMakeIndex2 = makeIndex(c2);
                                        int[] iArr7 = this.fIndexCount;
                                        iArr7[iMakeIndex2] = iArr7[iMakeIndex2] + 1;
                                        if (i15 + 1 < i2) {
                                            i7 = cArr[i15 + 1];
                                        } else {
                                            i7 = -1;
                                        }
                                        if (this.fIndexCount[iMakeIndex2] <= 1 && (iMakeIndex2 != makeIndex(i5) || iMakeIndex2 != makeIndex(i7))) {
                                            if (i29 + 2 >= i4) {
                                                i15--;
                                                i14 = i29;
                                            } else {
                                                int i43 = c2 >>> '\b';
                                                int i44 = c2 & 255;
                                                if (sUnicodeTagTable[i43]) {
                                                    i8 = i29 + 1;
                                                    bArr[i29] = -16;
                                                } else {
                                                    i8 = i29;
                                                }
                                                int i45 = i8 + 1;
                                                bArr[i8] = (byte) i43;
                                                i14 = i45 + 1;
                                                bArr[i45] = (byte) i44;
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        break;
                }
            }
        }
        if (iArr != null) {
            iArr[0] = i15 - i;
        }
        return i14 - i3;
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

    private static int makeIndex(int c) {
        if (c >= 192 && c < 320) {
            return 249;
        }
        if (c >= 592 && c < 720) {
            return 250;
        }
        if (c >= 880 && c < 1008) {
            return 251;
        }
        if (c >= 1328 && c < 1424) {
            return 252;
        }
        if (c >= 12352 && c < 12448) {
            return 253;
        }
        if (c >= 12448 && c < 12576) {
            return 254;
        }
        if (c >= 65376 && c < 65439) {
            return 255;
        }
        if (c >= 128 && c < 13312) {
            return (c / 128) & 255;
        }
        if (c >= 57344 && c <= 65535) {
            return ((c - Normalizer2Impl.Hangul.HANGUL_BASE) / 128) & 255;
        }
        return 0;
    }

    private boolean inDynamicWindow(int c, int whichWindow) {
        return c >= this.fOffsets[whichWindow] && c < this.fOffsets[whichWindow] + 128;
    }

    private static boolean inStaticWindow(int c, int whichWindow) {
        return c >= sOffsets[whichWindow] && c < sOffsets[whichWindow] + 128;
    }

    private static boolean isCompressible(int c) {
        return c < 13312 || c >= 57344;
    }

    private int findDynamicWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inDynamicWindow(c, i)) {
                int[] iArr = this.fTimeStamps;
                iArr[i] = iArr[i] + 1;
                return i;
            }
        }
        return -1;
    }

    private static int findStaticWindow(int c) {
        for (int i = 7; i >= 0; i--) {
            if (inStaticWindow(c, i)) {
                return i;
            }
        }
        return -1;
    }

    private int getLRDefinedWindow() {
        int leastRU = Integer.MAX_VALUE;
        int whichWindow = -1;
        for (int i = 7; i >= 0; i--) {
            if (this.fTimeStamps[i] < leastRU) {
                leastRU = this.fTimeStamps[i];
                whichWindow = i;
            }
        }
        return whichWindow;
    }
}
