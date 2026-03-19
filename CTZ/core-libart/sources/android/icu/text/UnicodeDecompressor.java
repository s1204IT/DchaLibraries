package android.icu.text;

import android.icu.lang.UCharacterEnums;
import dalvik.bytecode.Opcodes;

public final class UnicodeDecompressor implements SCSU {
    private static final int BUFSIZE = 3;
    private int fCurrentWindow = 0;
    private int[] fOffsets = new int[8];
    private int fMode = 0;
    private byte[] fBuffer = new byte[3];
    private int fBufferLength = 0;

    public UnicodeDecompressor() {
        reset();
    }

    public static String decompress(byte[] bArr) {
        return new String(decompress(bArr, 0, bArr.length));
    }

    public static char[] decompress(byte[] bArr, int i, int i2) {
        UnicodeDecompressor unicodeDecompressor = new UnicodeDecompressor();
        int iMax = Math.max(2, (i2 - i) * 2);
        char[] cArr = new char[iMax];
        int iDecompress = unicodeDecompressor.decompress(bArr, i, i2, null, cArr, 0, iMax);
        char[] cArr2 = new char[iDecompress];
        System.arraycopy(cArr, 0, cArr2, 0, iDecompress);
        return cArr2;
    }

    public int decompress(byte[] bArr, int i, int i2, int[] iArr, char[] cArr, int i3, int i4) {
        int i5;
        int iDecompress;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        if (cArr.length < 2 || i4 - i3 < 2) {
            throw new IllegalArgumentException("charBuffer.length < 2");
        }
        if (this.fBufferLength > 0) {
            if (this.fBufferLength != 3) {
                int length = this.fBuffer.length - this.fBufferLength;
                int i13 = i2 - i;
                if (i13 < length) {
                    length = i13;
                }
                System.arraycopy(bArr, i, this.fBuffer, this.fBufferLength, length);
                i12 = length;
            } else {
                i12 = 0;
            }
            this.fBufferLength = 0;
            i5 = 0;
            iDecompress = i3 + decompress(this.fBuffer, 0, this.fBuffer.length, null, cArr, i3, i4);
            i6 = i + i12;
        } else {
            i5 = 0;
            iDecompress = i3;
            i6 = i;
        }
        while (true) {
            if (i6 < i2 && iDecompress < i4) {
                switch (this.fMode) {
                    case 0:
                        while (i6 < i2 && iDecompress < i4) {
                            i7 = i6 + 1;
                            int i14 = bArr[i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                            switch (i14) {
                                case 0:
                                case 9:
                                case 10:
                                case 13:
                                case 32:
                                case 33:
                                case 34:
                                case 35:
                                case 36:
                                case 37:
                                case 38:
                                case 39:
                                case 40:
                                case 41:
                                case 42:
                                case 43:
                                case 44:
                                case 45:
                                case 46:
                                case 47:
                                case 48:
                                case 49:
                                case 50:
                                case 51:
                                case 52:
                                case 53:
                                case 54:
                                case 55:
                                case 56:
                                case 57:
                                case 58:
                                case 59:
                                case 60:
                                case 61:
                                case 62:
                                case 63:
                                case 64:
                                case 65:
                                case 66:
                                case 67:
                                case 68:
                                case 69:
                                case 70:
                                case 71:
                                case 72:
                                case 73:
                                case 74:
                                case 75:
                                case 76:
                                case 77:
                                case 78:
                                case 79:
                                case 80:
                                case 81:
                                case 82:
                                case 83:
                                case 84:
                                case 85:
                                case 86:
                                case 87:
                                case 88:
                                case 89:
                                case 90:
                                case 91:
                                case 92:
                                case 93:
                                case 94:
                                case 95:
                                case 96:
                                case 97:
                                case 98:
                                case 99:
                                case 100:
                                case 101:
                                case 102:
                                case 103:
                                case 104:
                                case 105:
                                case 106:
                                case 107:
                                case 108:
                                case 109:
                                case 110:
                                case 111:
                                case 112:
                                case 113:
                                case 114:
                                case 115:
                                case 116:
                                case 117:
                                case 118:
                                case 119:
                                case 120:
                                case 121:
                                case 122:
                                case 123:
                                case 124:
                                case 125:
                                case 126:
                                case 127:
                                    i8 = iDecompress + 1;
                                    cArr[iDecompress] = (char) i14;
                                    i6 = i7;
                                    iDecompress = i8;
                                    break;
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                case 8:
                                    if (i7 < i2) {
                                        int i15 = i7 + 1;
                                        int i16 = bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                        int i17 = iDecompress + 1;
                                        cArr[iDecompress] = (char) (i16 + ((i16 < 0 || i16 >= 128) ? this.fOffsets[i14 - 1] - 128 : sOffsets[i14 - 1]));
                                        i6 = i15;
                                        iDecompress = i17;
                                    } else {
                                        int i18 = i7 - 1;
                                        int i19 = i2 - i18;
                                        System.arraycopy(bArr, i18, this.fBuffer, i5, i19);
                                        this.fBufferLength = i19;
                                        i6 = this.fBufferLength + i18;
                                    }
                                    break;
                                case 11:
                                    int i20 = i7 + 1;
                                    if (i20 < i2) {
                                        int i21 = bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                        this.fCurrentWindow = (i21 & 224) >> 5;
                                        i9 = i20 + 1;
                                        this.fOffsets[this.fCurrentWindow] = (((bArr[i20] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | ((i21 & 31) << 8)) * 128) + 65536;
                                        i6 = i9;
                                    } else {
                                        int i22 = i7 - 1;
                                        int i23 = i2 - i22;
                                        System.arraycopy(bArr, i22, this.fBuffer, i5, i23);
                                        this.fBufferLength = i23;
                                        i6 = this.fBufferLength + i22;
                                    }
                                    break;
                                case 12:
                                default:
                                    i6 = i7;
                                    break;
                                case 14:
                                    int i24 = i7 + 1;
                                    if (i24 < i2) {
                                        cArr[iDecompress] = (char) ((bArr[i24] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (bArr[i7] << 8));
                                        iDecompress++;
                                        i6 = i24 + 1;
                                    } else {
                                        int i25 = i7 - 1;
                                        int i26 = i2 - i25;
                                        System.arraycopy(bArr, i25, this.fBuffer, i5, i26);
                                        this.fBufferLength = i26;
                                        i6 = this.fBufferLength + i25;
                                    }
                                    break;
                                case 15:
                                    this.fMode = 1;
                                    i6 = i7;
                                    break;
                                case 16:
                                case 17:
                                case 18:
                                case 19:
                                case 20:
                                case 21:
                                case 22:
                                case 23:
                                    this.fCurrentWindow = i14 - 16;
                                    i6 = i7;
                                    break;
                                case 24:
                                case 25:
                                case 26:
                                case 27:
                                case 28:
                                case 29:
                                case 30:
                                case 31:
                                    if (i7 < i2) {
                                        this.fCurrentWindow = i14 - 24;
                                        i9 = i7 + 1;
                                        this.fOffsets[this.fCurrentWindow] = sOffsetTable[bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED];
                                        i6 = i9;
                                    } else {
                                        int i27 = i7 - 1;
                                        int i28 = i2 - i27;
                                        System.arraycopy(bArr, i27, this.fBuffer, i5, i28);
                                        this.fBufferLength = i28;
                                        i6 = this.fBufferLength + i27;
                                    }
                                    break;
                                case 128:
                                case 129:
                                case 130:
                                case 131:
                                case 132:
                                case 133:
                                case 134:
                                case 135:
                                case 136:
                                case 137:
                                case 138:
                                case 139:
                                case 140:
                                case 141:
                                case 142:
                                case 143:
                                case 144:
                                case 145:
                                case 146:
                                case 147:
                                case 148:
                                case 149:
                                case 150:
                                case 151:
                                case 152:
                                case 153:
                                case 154:
                                case 155:
                                case 156:
                                case 157:
                                case 158:
                                case 159:
                                case 160:
                                case 161:
                                case 162:
                                case 163:
                                case 164:
                                case 165:
                                case 166:
                                case 167:
                                case 168:
                                case 169:
                                case 170:
                                case 171:
                                case 172:
                                case 173:
                                case 174:
                                case 175:
                                case 176:
                                case 177:
                                case 178:
                                case 179:
                                case 180:
                                case 181:
                                case 182:
                                case 183:
                                case 184:
                                case 185:
                                case 186:
                                case 187:
                                case 188:
                                case 189:
                                case 190:
                                case 191:
                                case 192:
                                case 193:
                                case 194:
                                case 195:
                                case 196:
                                case 197:
                                case 198:
                                case 199:
                                case 200:
                                case 201:
                                case 202:
                                case 203:
                                case 204:
                                case 205:
                                case 206:
                                case 207:
                                case 208:
                                case 209:
                                case 210:
                                case 211:
                                case 212:
                                case 213:
                                case 214:
                                case 215:
                                case 216:
                                case 217:
                                case 218:
                                case 219:
                                case 220:
                                case 221:
                                case 222:
                                case 223:
                                case 224:
                                case 225:
                                case 226:
                                case 227:
                                case 228:
                                case 229:
                                case 230:
                                case 231:
                                case 232:
                                case 233:
                                case 234:
                                case 235:
                                case 236:
                                case 237:
                                case 238:
                                case 239:
                                case 240:
                                case 241:
                                case 242:
                                case 243:
                                case 244:
                                case 245:
                                case 246:
                                case 247:
                                case 248:
                                case 249:
                                case 250:
                                case 251:
                                case 252:
                                case 253:
                                case 254:
                                case 255:
                                    if (this.fOffsets[this.fCurrentWindow] > 65535) {
                                        int i29 = iDecompress + 1;
                                        if (i29 < i4) {
                                            int i30 = this.fOffsets[this.fCurrentWindow] - 65536;
                                            cArr[iDecompress] = (char) (55296 + (i30 >> 10));
                                            iDecompress = i29 + 1;
                                            cArr[i29] = (char) (UTF16.TRAIL_SURROGATE_MIN_VALUE + (i30 & Opcodes.OP_NEW_INSTANCE_JUMBO) + (i14 & 127));
                                            i6 = i7;
                                        } else {
                                            int i31 = i7 - 1;
                                            int i32 = i2 - i31;
                                            System.arraycopy(bArr, i31, this.fBuffer, i5, i32);
                                            this.fBufferLength = i32;
                                            i6 = this.fBufferLength + i31;
                                        }
                                    } else {
                                        i8 = iDecompress + 1;
                                        cArr[iDecompress] = (char) ((i14 + this.fOffsets[this.fCurrentWindow]) - 128);
                                        i6 = i7;
                                        iDecompress = i8;
                                    }
                                    break;
                            }
                        }
                        break;
                    case 1:
                        while (true) {
                            if (i6 >= i2 || iDecompress >= i4) {
                                break;
                            }
                            i7 = i6 + 1;
                            int i33 = bArr[i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                            switch (i33) {
                                case 224:
                                case 225:
                                case 226:
                                case 227:
                                case 228:
                                case 229:
                                case 230:
                                case 231:
                                    this.fCurrentWindow = i33 - 224;
                                    this.fMode = i5;
                                    break;
                                case 232:
                                case 233:
                                case 234:
                                case 235:
                                case 236:
                                case 237:
                                case 238:
                                case 239:
                                    if (i7 < i2) {
                                        this.fCurrentWindow = i33 - 232;
                                        this.fOffsets[this.fCurrentWindow] = sOffsetTable[bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED];
                                        this.fMode = i5;
                                        i6 = i7 + 1;
                                    } else {
                                        int i34 = i7 - 1;
                                        int i35 = i2 - i34;
                                        System.arraycopy(bArr, i34, this.fBuffer, i5, i35);
                                        this.fBufferLength = i35;
                                        i6 = this.fBufferLength + i34;
                                    }
                                    break;
                                case 240:
                                    if (i7 < i2 - 1) {
                                        int i36 = i7 + 1;
                                        i10 = iDecompress + 1;
                                        i11 = i36 + 1;
                                        cArr[iDecompress] = (char) ((bArr[i36] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | (bArr[i7] << 8));
                                        iDecompress = i10;
                                        i6 = i11;
                                    } else {
                                        int i37 = i7 - 1;
                                        int i38 = i2 - i37;
                                        System.arraycopy(bArr, i37, this.fBuffer, i5, i38);
                                        this.fBufferLength = i38;
                                        i6 = this.fBufferLength + i37;
                                    }
                                    break;
                                case 241:
                                    int i39 = i7 + 1;
                                    if (i39 < i2) {
                                        int i40 = bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                        this.fCurrentWindow = (i40 & 224) >> 5;
                                        this.fOffsets[this.fCurrentWindow] = 65536 + (128 * ((bArr[i39] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED) | ((i40 & 31) << 8)));
                                        this.fMode = i5;
                                        i6 = i39 + 1;
                                    } else {
                                        int i41 = i7 - 1;
                                        int i42 = i2 - i41;
                                        System.arraycopy(bArr, i41, this.fBuffer, i5, i42);
                                        this.fBufferLength = i42;
                                        i6 = this.fBufferLength + i41;
                                    }
                                    break;
                                default:
                                    if (i7 < i2) {
                                        i10 = iDecompress + 1;
                                        i11 = i7 + 1;
                                        cArr[iDecompress] = (char) ((i33 << 8) | (bArr[i7] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED));
                                        iDecompress = i10;
                                        i6 = i11;
                                    } else {
                                        int i43 = i7 - 1;
                                        int i44 = i2 - i43;
                                        System.arraycopy(bArr, i43, this.fBuffer, i5, i44);
                                        this.fBufferLength = i44;
                                        i6 = this.fBufferLength + i43;
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        }
        int i45 = i5;
        if (iArr != null) {
            iArr[i45] = i6 - i;
        }
        return iDecompress - i3;
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
        this.fCurrentWindow = 0;
        this.fMode = 0;
        this.fBufferLength = 0;
    }
}
