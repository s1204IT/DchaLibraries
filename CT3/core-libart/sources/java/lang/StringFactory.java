package java.lang;

import android.icu.lang.UCharacterEnums;
import android.icu.text.Bidi;
import dalvik.bytecode.Opcodes;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import libcore.util.CharsetUtils;
import libcore.util.EmptyArray;

public final class StringFactory {
    private static final char REPLACEMENT_CHAR = 65533;

    public static native String newStringFromBytes(byte[] bArr, int i, int i2, int i3);

    static native String newStringFromChars(int i, int i2, char[] cArr);

    public static native String newStringFromString(String str);

    public static String newEmptyString() {
        return newStringFromChars(EmptyArray.CHAR, 0, 0);
    }

    public static String newStringFromBytes(byte[] data) {
        return newStringFromBytes(data, 0, data.length);
    }

    public static String newStringFromBytes(byte[] data, int high) {
        return newStringFromBytes(data, high, 0, data.length);
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount) {
        return newStringFromBytes(data, offset, byteCount, Charset.defaultCharset());
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount, String charsetName) throws UnsupportedEncodingException {
        return newStringFromBytes(data, offset, byteCount, Charset.forNameUEE(charsetName));
    }

    public static String newStringFromBytes(byte[] data, String charsetName) throws UnsupportedEncodingException {
        return newStringFromBytes(data, 0, data.length, Charset.forNameUEE(charsetName));
    }

    public static String newStringFromBytes(byte[] data, int offset, int byteCount, Charset charset) {
        int length;
        char[] value;
        int s;
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            throw new StringIndexOutOfBoundsException(data.length, offset, byteCount);
        }
        String canonicalCharsetName = charset.name();
        if (canonicalCharsetName.equals("UTF-8")) {
            char[] v = new char[byteCount];
            int last = offset + byteCount;
            int s2 = 0;
            int idx = offset;
            while (idx < last) {
                int idx2 = idx + 1;
                byte b0 = data[idx];
                if ((b0 & Bidi.LEVEL_OVERRIDE) == 0) {
                    int val = b0 & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    s = s2 + 1;
                    v[s2] = (char) val;
                } else if ((b0 & 224) == 192 || (b0 & 240) == 224 || (b0 & 248) == 240 || (b0 & 252) == 248 || (b0 & 254) == 252) {
                    int utfCount = 1;
                    if ((b0 & 240) == 224) {
                        utfCount = 2;
                    } else if ((b0 & 248) == 240) {
                        utfCount = 3;
                    } else if ((b0 & 252) == 248) {
                        utfCount = 4;
                    } else if ((b0 & 254) == 252) {
                        utfCount = 5;
                    }
                    if (idx2 + utfCount > last) {
                        v[s2] = REPLACEMENT_CHAR;
                        s2++;
                        idx = idx2;
                    } else {
                        int val2 = b0 & (31 >> (utfCount - 1));
                        int i = 0;
                        while (true) {
                            idx = idx2;
                            if (i < utfCount) {
                                idx2 = idx + 1;
                                byte b = data[idx];
                                if ((b & 192) != 128) {
                                    v[s2] = REPLACEMENT_CHAR;
                                    s2++;
                                    idx = idx2 - 1;
                                    break;
                                }
                                val2 = (val2 << 6) | (b & 63);
                                i++;
                            } else if (utfCount != 2 && val2 >= 55296 && val2 <= 57343) {
                                v[s2] = REPLACEMENT_CHAR;
                                s2++;
                            } else if (val2 > 1114111) {
                                v[s2] = REPLACEMENT_CHAR;
                                s2++;
                            } else {
                                if (val2 < 65536) {
                                    s = s2 + 1;
                                    v[s2] = (char) val2;
                                } else {
                                    int x = val2 & 65535;
                                    int u = (val2 >> 16) & 31;
                                    int w = (u - 1) & 65535;
                                    int hi = (w << 6) | 55296 | (x >> 10);
                                    int lo = 56320 | (x & Opcodes.OP_NEW_INSTANCE_JUMBO);
                                    int s3 = s2 + 1;
                                    v[s2] = (char) hi;
                                    v[s3] = (char) lo;
                                    s = s3 + 1;
                                }
                                idx2 = idx;
                            }
                        }
                    }
                } else {
                    s = s2 + 1;
                    v[s2] = REPLACEMENT_CHAR;
                }
                s2 = s;
                idx = idx2;
            }
            if (s2 == byteCount) {
                value = v;
                length = s2;
            } else {
                value = new char[s2];
                length = s2;
                System.arraycopy(v, 0, value, 0, s2);
            }
        } else if (canonicalCharsetName.equals("ISO-8859-1")) {
            value = new char[byteCount];
            length = byteCount;
            CharsetUtils.isoLatin1BytesToChars(data, offset, byteCount, value);
        } else if (canonicalCharsetName.equals("US-ASCII")) {
            value = new char[byteCount];
            length = byteCount;
            CharsetUtils.asciiBytesToChars(data, offset, byteCount, value);
        } else {
            CharBuffer cb = charset.decode(ByteBuffer.wrap(data, offset, byteCount));
            length = cb.length();
            if (length > 0) {
                value = new char[length];
                System.arraycopy(cb.array(), 0, value, 0, length);
            } else {
                value = EmptyArray.CHAR;
            }
        }
        return newStringFromChars(value, 0, length);
    }

    public static String newStringFromBytes(byte[] data, Charset charset) {
        return newStringFromBytes(data, 0, data.length, charset);
    }

    public static String newStringFromChars(char[] data) {
        return newStringFromChars(data, 0, data.length);
    }

    public static String newStringFromChars(char[] data, int offset, int charCount) {
        if ((offset | charCount) < 0 || charCount > data.length - offset) {
            throw new StringIndexOutOfBoundsException(data.length, offset, charCount);
        }
        return newStringFromChars(offset, charCount, data);
    }

    public static String newStringFromStringBuffer(StringBuffer stringBuffer) {
        String strNewStringFromChars;
        synchronized (stringBuffer) {
            strNewStringFromChars = newStringFromChars(stringBuffer.getValue(), 0, stringBuffer.length());
        }
        return strNewStringFromChars;
    }

    public static String newStringFromCodePoints(int[] codePoints, int offset, int count) {
        if (codePoints == null) {
            throw new NullPointerException("codePoints == null");
        }
        if ((offset | count) < 0 || count > codePoints.length - offset) {
            throw new StringIndexOutOfBoundsException(codePoints.length, offset, count);
        }
        char[] value = new char[count * 2];
        int end = offset + count;
        int length = 0;
        for (int i = offset; i < end; i++) {
            length += Character.toChars(codePoints[i], value, length);
        }
        return newStringFromChars(value, 0, length);
    }

    public static String newStringFromStringBuilder(StringBuilder stringBuilder) {
        return newStringFromChars(stringBuilder.getValue(), 0, stringBuilder.length());
    }
}
