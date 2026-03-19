package java.lang;

import android.icu.lang.UCharacterEnums;
import android.icu.text.UTF16;
import dalvik.annotation.optimization.FastNative;
import dalvik.bytecode.Opcodes;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import libcore.util.CharsetUtils;
import libcore.util.EmptyArray;

public final class StringFactory {
    private static final char REPLACEMENT_CHAR = 65533;
    private static final int[] TABLE_UTF8_NEEDED = {0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @FastNative
    public static native String newStringFromBytes(byte[] bArr, int i, int i2, int i3);

    @FastNative
    static native String newStringFromChars(int i, int i2, char[] cArr);

    @FastNative
    public static native String newStringFromString(String str);

    public static String newEmptyString() {
        return newStringFromChars(EmptyArray.CHAR, 0, 0);
    }

    public static String newStringFromBytes(byte[] bArr) {
        return newStringFromBytes(bArr, 0, bArr.length);
    }

    public static String newStringFromBytes(byte[] bArr, int i) {
        return newStringFromBytes(bArr, i, 0, bArr.length);
    }

    public static String newStringFromBytes(byte[] bArr, int i, int i2) {
        return newStringFromBytes(bArr, i, i2, Charset.defaultCharset());
    }

    public static String newStringFromBytes(byte[] bArr, int i, int i2, String str) throws UnsupportedEncodingException {
        return newStringFromBytes(bArr, i, i2, Charset.forNameUEE(str));
    }

    public static String newStringFromBytes(byte[] bArr, String str) throws UnsupportedEncodingException {
        return newStringFromBytes(bArr, 0, bArr.length, Charset.forNameUEE(str));
    }

    public static String newStringFromBytes(byte[] bArr, int i, int i2, Charset charset) {
        int length;
        char[] cArr;
        char[] cArr2;
        int i3;
        int i4;
        int i5;
        int i6 = i;
        if ((i6 | i2) < 0 || i2 > bArr.length - i6) {
            throw new StringIndexOutOfBoundsException(bArr.length, i6, i2);
        }
        String strName = charset.name();
        if (strName.equals("UTF-8")) {
            char[] cArr3 = new char[i2];
            int i7 = i6 + i2;
            int i8 = 0;
            int i9 = 0;
            int i10 = 0;
            int i11 = 0;
            loop0: while (true) {
                int i12 = 128;
                while (true) {
                    int i13 = 191;
                    while (i6 < i7) {
                        i3 = i6 + 1;
                        int i14 = bArr[i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                        if (i8 == 0) {
                            if ((i14 & 128) == 0) {
                                cArr3[i9] = (char) i14;
                                i9++;
                            } else {
                                if ((i14 & 64) == 0) {
                                    i5 = i9 + 1;
                                    cArr3[i9] = REPLACEMENT_CHAR;
                                } else {
                                    i8 = TABLE_UTF8_NEEDED[i14 & 63];
                                    if (i8 == 0) {
                                        i5 = i9 + 1;
                                        cArr3[i9] = REPLACEMENT_CHAR;
                                    } else {
                                        int i15 = (63 >> i8) & i14;
                                        if (i14 == 224) {
                                            i12 = 160;
                                        } else if (i14 == 237) {
                                            i13 = 159;
                                        } else if (i14 == 240) {
                                            i12 = 144;
                                        } else if (i14 == 244) {
                                            i13 = 143;
                                        }
                                        i10 = i15;
                                    }
                                }
                                i9 = i5;
                            }
                        } else {
                            if (i14 < i12 || i14 > i13) {
                                break;
                            }
                            i10 = (i10 << 6) | (i14 & 63);
                            i11++;
                            if (i8 != i11) {
                                break;
                            }
                            if (i10 < 65536) {
                                i4 = i9 + 1;
                                cArr3[i9] = (char) i10;
                            } else {
                                int i16 = i9 + 1;
                                cArr3[i9] = (char) ((i10 >> 10) + 55232);
                                cArr3[i16] = (char) ((i10 & Opcodes.OP_NEW_INSTANCE_JUMBO) + UTF16.TRAIL_SURROGATE_MIN_VALUE);
                                i4 = i16 + 1;
                            }
                            i9 = i4;
                            i8 = 0;
                            i10 = 0;
                            i11 = 0;
                            i12 = 128;
                            i13 = 191;
                        }
                        i6 = i3;
                    }
                    i12 = 128;
                    i6 = i3;
                }
                cArr3[i9] = REPLACEMENT_CHAR;
                i9++;
                i8 = 0;
                i10 = 0;
                i11 = 0;
                i6 = i3 - 1;
            }
            if (i8 != 0) {
                cArr3[i9] = REPLACEMENT_CHAR;
                i9++;
            }
            if (i9 == i2) {
                cArr = cArr3;
            } else {
                cArr = new char[i9];
                System.arraycopy(cArr3, 0, cArr, 0, i9);
            }
            length = i9;
        } else {
            if (strName.equals("ISO-8859-1")) {
                cArr2 = new char[i2];
                CharsetUtils.isoLatin1BytesToChars(bArr, i6, i2, cArr2);
            } else if (strName.equals("US-ASCII")) {
                cArr2 = new char[i2];
                CharsetUtils.asciiBytesToChars(bArr, i6, i2, cArr2);
            } else {
                CharBuffer charBufferDecode = charset.decode(ByteBuffer.wrap(bArr, i, i2));
                length = charBufferDecode.length();
                if (length > 0) {
                    char[] cArr4 = new char[length];
                    System.arraycopy(charBufferDecode.array(), 0, cArr4, 0, length);
                    cArr = cArr4;
                } else {
                    cArr = EmptyArray.CHAR;
                }
            }
            length = i2;
            cArr = cArr2;
        }
        return newStringFromChars(cArr, 0, length);
    }

    public static String newStringFromBytes(byte[] bArr, Charset charset) {
        return newStringFromBytes(bArr, 0, bArr.length, charset);
    }

    public static String newStringFromChars(char[] cArr) {
        return newStringFromChars(cArr, 0, cArr.length);
    }

    public static String newStringFromChars(char[] cArr, int i, int i2) {
        if ((i | i2) < 0 || i2 > cArr.length - i) {
            throw new StringIndexOutOfBoundsException(cArr.length, i, i2);
        }
        return newStringFromChars(i, i2, cArr);
    }

    public static String newStringFromStringBuffer(StringBuffer stringBuffer) {
        String strNewStringFromChars;
        synchronized (stringBuffer) {
            strNewStringFromChars = newStringFromChars(stringBuffer.getValue(), 0, stringBuffer.length());
        }
        return strNewStringFromChars;
    }

    public static String newStringFromCodePoints(int[] iArr, int i, int i2) {
        if (iArr == null) {
            throw new NullPointerException("codePoints == null");
        }
        if ((i | i2) < 0 || i2 > iArr.length - i) {
            throw new StringIndexOutOfBoundsException(iArr.length, i, i2);
        }
        char[] cArr = new char[i2 * 2];
        int i3 = i2 + i;
        int chars = 0;
        while (i < i3) {
            chars += Character.toChars(iArr[i], cArr, chars);
            i++;
        }
        return newStringFromChars(cArr, 0, chars);
    }

    public static String newStringFromStringBuilder(StringBuilder sb) {
        return newStringFromChars(sb.getValue(), 0, sb.length());
    }
}
