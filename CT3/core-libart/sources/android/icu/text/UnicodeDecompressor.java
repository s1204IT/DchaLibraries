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

    public static String decompress(byte[] buffer) {
        char[] buf = decompress(buffer, 0, buffer.length);
        return new String(buf);
    }

    public static char[] decompress(byte[] buffer, int start, int limit) {
        UnicodeDecompressor comp = new UnicodeDecompressor();
        int len = Math.max(2, (limit - start) * 2);
        char[] temp = new char[len];
        int charCount = comp.decompress(buffer, start, limit, null, temp, 0, len);
        char[] result = new char[charCount];
        System.arraycopy(temp, 0, result, 0, charCount);
        return result;
    }

    public int decompress(byte[] byteBuffer, int byteBufferStart, int byteBufferLimit, int[] bytesRead, char[] charBuffer, int charBufferStart, int charBufferLimit) {
        int bytePos = byteBufferStart;
        int ucPos = charBufferStart;
        if (charBuffer.length < 2 || charBufferLimit - charBufferStart < 2) {
            throw new IllegalArgumentException("charBuffer.length < 2");
        }
        if (this.fBufferLength > 0) {
            int newBytes = 0;
            if (this.fBufferLength != 3) {
                newBytes = this.fBuffer.length - this.fBufferLength;
                if (byteBufferLimit - byteBufferStart < newBytes) {
                    newBytes = byteBufferLimit - byteBufferStart;
                }
                System.arraycopy(byteBuffer, byteBufferStart, this.fBuffer, this.fBufferLength, newBytes);
            }
            this.fBufferLength = 0;
            int count = decompress(this.fBuffer, 0, this.fBuffer.length, null, charBuffer, charBufferStart, charBufferLimit);
            ucPos = charBufferStart + count;
            bytePos = byteBufferStart + newBytes;
        }
        while (true) {
            if (bytePos < byteBufferLimit && ucPos < charBufferLimit) {
                switch (this.fMode) {
                    case 0:
                        while (true) {
                            int ucPos2 = ucPos;
                            int bytePos2 = bytePos;
                            if (bytePos2 >= byteBufferLimit || ucPos2 >= charBufferLimit) {
                                break;
                            } else {
                                bytePos = bytePos2 + 1;
                                int aByte = byteBuffer[bytePos2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                switch (aByte) {
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
                                        ucPos = ucPos2 + 1;
                                        charBuffer[ucPos2] = (char) aByte;
                                        break;
                                    case 1:
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                    case 6:
                                    case 7:
                                    case 8:
                                        if (bytePos >= byteBufferLimit) {
                                            int bytePos3 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos3, this.fBuffer, 0, byteBufferLimit - bytePos3);
                                            this.fBufferLength = byteBufferLimit - bytePos3;
                                            bytePos = bytePos3 + this.fBufferLength;
                                            ucPos = ucPos2;
                                        } else {
                                            int bytePos4 = bytePos + 1;
                                            int dByte = byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                            ucPos = ucPos2 + 1;
                                            charBuffer[ucPos2] = (char) (((dByte >= 0 && dByte < 128) ? sOffsets[aByte - 1] : this.fOffsets[aByte - 1] - 128) + dByte);
                                            bytePos = bytePos4;
                                        }
                                        break;
                                    case 11:
                                        if (bytePos + 1 >= byteBufferLimit) {
                                            int bytePos5 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos5, this.fBuffer, 0, byteBufferLimit - bytePos5);
                                            this.fBufferLength = byteBufferLimit - bytePos5;
                                            bytePos = bytePos5 + this.fBufferLength;
                                            ucPos = ucPos2;
                                        } else {
                                            int bytePos6 = bytePos + 1;
                                            int aByte2 = byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                            this.fCurrentWindow = (aByte2 & 224) >> 5;
                                            bytePos = bytePos6 + 1;
                                            this.fOffsets[this.fCurrentWindow] = ((((aByte2 & 31) << 8) | (byteBuffer[bytePos6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) * 128) + 65536;
                                            ucPos = ucPos2;
                                        }
                                        break;
                                    case 12:
                                        ucPos = ucPos2;
                                        break;
                                    case 14:
                                        if (bytePos + 1 >= byteBufferLimit) {
                                            int bytePos7 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos7, this.fBuffer, 0, byteBufferLimit - bytePos7);
                                            this.fBufferLength = byteBufferLimit - bytePos7;
                                            bytePos = bytePos7 + this.fBufferLength;
                                            ucPos = ucPos2;
                                        } else {
                                            int bytePos8 = bytePos + 1;
                                            ucPos = ucPos2 + 1;
                                            int i = byteBuffer[bytePos] << 8;
                                            bytePos = bytePos8 + 1;
                                            charBuffer[ucPos2] = (char) (i | (byteBuffer[bytePos8] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED));
                                        }
                                        break;
                                    case 15:
                                        this.fMode = 1;
                                        break;
                                    case 16:
                                    case 17:
                                    case 18:
                                    case 19:
                                    case 20:
                                    case 21:
                                    case 22:
                                    case 23:
                                        this.fCurrentWindow = aByte - 16;
                                        ucPos = ucPos2;
                                        break;
                                    case 24:
                                    case 25:
                                    case 26:
                                    case 27:
                                    case 28:
                                    case 29:
                                    case 30:
                                    case 31:
                                        if (bytePos >= byteBufferLimit) {
                                            int bytePos9 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos9, this.fBuffer, 0, byteBufferLimit - bytePos9);
                                            this.fBufferLength = byteBufferLimit - bytePos9;
                                            bytePos = bytePos9 + this.fBufferLength;
                                            ucPos = ucPos2;
                                        } else {
                                            this.fCurrentWindow = aByte - 24;
                                            this.fOffsets[this.fCurrentWindow] = sOffsetTable[byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED];
                                            ucPos = ucPos2;
                                            bytePos++;
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
                                        if (this.fOffsets[this.fCurrentWindow] <= 65535) {
                                            ucPos = ucPos2 + 1;
                                            charBuffer[ucPos2] = (char) ((this.fOffsets[this.fCurrentWindow] + aByte) - 128);
                                        } else if (ucPos2 + 1 >= charBufferLimit) {
                                            int bytePos10 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos10, this.fBuffer, 0, byteBufferLimit - bytePos10);
                                            this.fBufferLength = byteBufferLimit - bytePos10;
                                            bytePos = bytePos10 + this.fBufferLength;
                                            ucPos = ucPos2;
                                        } else {
                                            int normalizedBase = this.fOffsets[this.fCurrentWindow] - 65536;
                                            int ucPos3 = ucPos2 + 1;
                                            charBuffer[ucPos2] = (char) ((normalizedBase >> 10) + 55296);
                                            charBuffer[ucPos3] = (char) ((normalizedBase & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE + (aByte & 127));
                                            ucPos = ucPos3 + 1;
                                        }
                                        break;
                                    default:
                                        ucPos = ucPos2;
                                        break;
                                }
                            }
                        }
                        break;
                    case 1:
                        while (true) {
                            int ucPos4 = ucPos;
                            int bytePos11 = bytePos;
                            if (bytePos11 >= byteBufferLimit || ucPos4 >= charBufferLimit) {
                                break;
                            } else {
                                bytePos = bytePos11 + 1;
                                int aByte3 = byteBuffer[bytePos11] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                switch (aByte3) {
                                    case 224:
                                    case 225:
                                    case 226:
                                    case 227:
                                    case 228:
                                    case 229:
                                    case 230:
                                    case 231:
                                        this.fCurrentWindow = aByte3 - 224;
                                        this.fMode = 0;
                                        break;
                                    case 232:
                                    case 233:
                                    case 234:
                                    case 235:
                                    case 236:
                                    case 237:
                                    case 238:
                                    case 239:
                                        if (bytePos >= byteBufferLimit) {
                                            int bytePos12 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos12, this.fBuffer, 0, byteBufferLimit - bytePos12);
                                            this.fBufferLength = byteBufferLimit - bytePos12;
                                            bytePos = bytePos12 + this.fBufferLength;
                                            ucPos = ucPos4;
                                        } else {
                                            this.fCurrentWindow = aByte3 - 232;
                                            this.fOffsets[this.fCurrentWindow] = sOffsetTable[byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED];
                                            this.fMode = 0;
                                            bytePos++;
                                        }
                                        break;
                                    case 240:
                                        if (bytePos >= byteBufferLimit - 1) {
                                            int bytePos13 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos13, this.fBuffer, 0, byteBufferLimit - bytePos13);
                                            this.fBufferLength = byteBufferLimit - bytePos13;
                                            bytePos = bytePos13 + this.fBufferLength;
                                            ucPos = ucPos4;
                                        } else {
                                            int bytePos14 = bytePos + 1;
                                            ucPos = ucPos4 + 1;
                                            int i2 = byteBuffer[bytePos] << 8;
                                            bytePos = bytePos14 + 1;
                                            charBuffer[ucPos4] = (char) (i2 | (byteBuffer[bytePos14] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED));
                                        }
                                        break;
                                    case 241:
                                        if (bytePos + 1 >= byteBufferLimit) {
                                            int bytePos15 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos15, this.fBuffer, 0, byteBufferLimit - bytePos15);
                                            this.fBufferLength = byteBufferLimit - bytePos15;
                                            bytePos = bytePos15 + this.fBufferLength;
                                            ucPos = ucPos4;
                                        } else {
                                            int bytePos16 = bytePos + 1;
                                            int aByte4 = byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                                            this.fCurrentWindow = (aByte4 & 224) >> 5;
                                            bytePos = bytePos16 + 1;
                                            this.fOffsets[this.fCurrentWindow] = ((((aByte4 & 31) << 8) | (byteBuffer[bytePos16] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED)) * 128) + 65536;
                                            this.fMode = 0;
                                        }
                                        break;
                                    default:
                                        if (bytePos >= byteBufferLimit) {
                                            int bytePos17 = bytePos - 1;
                                            System.arraycopy(byteBuffer, bytePos17, this.fBuffer, 0, byteBufferLimit - bytePos17);
                                            this.fBufferLength = byteBufferLimit - bytePos17;
                                            bytePos = bytePos17 + this.fBufferLength;
                                            ucPos = ucPos4;
                                        } else {
                                            ucPos = ucPos4 + 1;
                                            charBuffer[ucPos4] = (char) ((aByte3 << 8) | (byteBuffer[bytePos] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED));
                                            bytePos++;
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                }
            }
        }
        if (bytesRead != null) {
            bytesRead[0] = bytePos - byteBufferStart;
        }
        return ucPos - charBufferStart;
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
