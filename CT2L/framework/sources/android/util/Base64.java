package android.util;

import android.os.BatteryStats;
import com.android.internal.http.multipart.StringPart;
import java.io.UnsupportedEncodingException;

public class Base64 {
    static final boolean $assertionsDisabled;
    public static final int CRLF = 4;
    public static final int DEFAULT = 0;
    public static final int NO_CLOSE = 16;
    public static final int NO_PADDING = 1;
    public static final int NO_WRAP = 2;
    public static final int URL_SAFE = 8;

    static {
        $assertionsDisabled = !Base64.class.desiredAssertionStatus();
    }

    static abstract class Coder {
        public int op;
        public byte[] output;

        public abstract int maxOutputSize(int i);

        public abstract boolean process(byte[] bArr, int i, int i2, boolean z);

        Coder() {
        }
    }

    public static byte[] decode(String str, int flags) {
        return decode(str.getBytes(), flags);
    }

    public static byte[] decode(byte[] input, int flags) {
        return decode(input, 0, input.length, flags);
    }

    public static byte[] decode(byte[] input, int offset, int len, int flags) {
        Decoder decoder = new Decoder(flags, new byte[(len * 3) / 4]);
        if (!decoder.process(input, offset, len, true)) {
            throw new IllegalArgumentException("bad base-64");
        }
        if (decoder.op == decoder.output.length) {
            return decoder.output;
        }
        byte[] temp = new byte[decoder.op];
        System.arraycopy(decoder.output, 0, temp, 0, decoder.op);
        return temp;
    }

    static class Decoder extends Coder {
        private static final int[] DECODE = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private static final int[] DECODE_WEBSAFE = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -2, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, 63, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
        private static final int EQUALS = -2;
        private static final int SKIP = -1;
        private final int[] alphabet;
        private int state;
        private int value;

        public Decoder(int flags, byte[] output) {
            this.output = output;
            this.alphabet = (flags & 8) == 0 ? DECODE : DECODE_WEBSAFE;
            this.state = 0;
            this.value = 0;
        }

        @Override
        public int maxOutputSize(int len) {
            return ((len * 3) / 4) + 10;
        }

        @Override
        public boolean process(byte[] input, int offset, int len, boolean finish) {
            int op;
            if (this.state == 6) {
                return false;
            }
            int p = offset;
            int len2 = len + offset;
            int state = this.state;
            int value = this.value;
            int op2 = 0;
            byte[] output = this.output;
            int[] alphabet = this.alphabet;
            while (true) {
                if (p < len2) {
                    if (state == 0) {
                        while (p + 4 <= len2 && (value = (alphabet[input[p] & BatteryStats.HistoryItem.CMD_NULL] << 18) | (alphabet[input[p + 1] & BatteryStats.HistoryItem.CMD_NULL] << 12) | (alphabet[input[p + 2] & BatteryStats.HistoryItem.CMD_NULL] << 6) | alphabet[input[p + 3] & BatteryStats.HistoryItem.CMD_NULL]) >= 0) {
                            output[op2 + 2] = (byte) value;
                            output[op2 + 1] = (byte) (value >> 8);
                            output[op2] = (byte) (value >> 16);
                            op2 += 3;
                            p += 4;
                        }
                        if (p >= len2) {
                            op = op2;
                        }
                    }
                    int p2 = p + 1;
                    int d = alphabet[input[p] & BatteryStats.HistoryItem.CMD_NULL];
                    switch (state) {
                        case 0:
                            if (d >= 0) {
                                value = d;
                                state++;
                            } else {
                                if (d != -1) {
                                    this.state = 6;
                                    return false;
                                }
                            }
                            break;
                        case 1:
                            if (d >= 0) {
                                value = (value << 6) | d;
                                state++;
                            } else {
                                if (d != -1) {
                                    this.state = 6;
                                    return false;
                                }
                            }
                            break;
                        case 2:
                            if (d >= 0) {
                                value = (value << 6) | d;
                                state++;
                            } else if (d == -2) {
                                output[op2] = (byte) (value >> 4);
                                state = 4;
                                op2++;
                            } else {
                                if (d != -1) {
                                    this.state = 6;
                                    return false;
                                }
                            }
                            break;
                        case 3:
                            if (d >= 0) {
                                value = (value << 6) | d;
                                output[op2 + 2] = (byte) value;
                                output[op2 + 1] = (byte) (value >> 8);
                                output[op2] = (byte) (value >> 16);
                                op2 += 3;
                                state = 0;
                            } else if (d == -2) {
                                output[op2 + 1] = (byte) (value >> 2);
                                output[op2] = (byte) (value >> 10);
                                op2 += 2;
                                state = 5;
                            } else {
                                if (d != -1) {
                                    this.state = 6;
                                    return false;
                                }
                            }
                            break;
                        case 4:
                            if (d == -2) {
                                state++;
                            } else {
                                if (d != -1) {
                                    this.state = 6;
                                    return false;
                                }
                            }
                            break;
                        case 5:
                            if (d != -1) {
                                this.state = 6;
                                return false;
                            }
                            break;
                    }
                    p = p2;
                } else {
                    op = op2;
                }
            }
        }
    }

    public static String encodeToString(byte[] input, int flags) {
        try {
            return new String(encode(input, flags), StringPart.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static String encodeToString(byte[] input, int offset, int len, int flags) {
        try {
            return new String(encode(input, offset, len, flags), StringPart.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] encode(byte[] input, int flags) {
        return encode(input, 0, input.length, flags);
    }

    public static byte[] encode(byte[] input, int offset, int len, int flags) {
        Encoder encoder = new Encoder(flags, null);
        int output_len = (len / 3) * 4;
        if (encoder.do_padding) {
            if (len % 3 > 0) {
                output_len += 4;
            }
        } else {
            switch (len % 3) {
                case 1:
                    output_len += 2;
                    break;
                case 2:
                    output_len += 3;
                    break;
            }
        }
        if (encoder.do_newline && len > 0) {
            output_len += (encoder.do_cr ? 2 : 1) * (((len - 1) / 57) + 1);
        }
        encoder.output = new byte[output_len];
        encoder.process(input, offset, len, true);
        if ($assertionsDisabled || encoder.op == output_len) {
            return encoder.output;
        }
        throw new AssertionError();
    }

    static class Encoder extends Coder {
        static final boolean $assertionsDisabled;
        private static final byte[] ENCODE;
        private static final byte[] ENCODE_WEBSAFE;
        public static final int LINE_GROUPS = 19;
        private final byte[] alphabet;
        private int count;
        public final boolean do_cr;
        public final boolean do_newline;
        public final boolean do_padding;
        private final byte[] tail;
        int tailLen;

        static {
            $assertionsDisabled = !Base64.class.desiredAssertionStatus();
            ENCODE = new byte[]{65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};
            ENCODE_WEBSAFE = new byte[]{65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 45, 95};
        }

        public Encoder(int flags, byte[] output) {
            this.output = output;
            this.do_padding = (flags & 1) == 0;
            this.do_newline = (flags & 2) == 0;
            this.do_cr = (flags & 4) != 0;
            this.alphabet = (flags & 8) == 0 ? ENCODE : ENCODE_WEBSAFE;
            this.tail = new byte[2];
            this.tailLen = 0;
            this.count = this.do_newline ? 19 : -1;
        }

        @Override
        public int maxOutputSize(int len) {
            return ((len * 8) / 5) + 10;
        }

        @Override
        public boolean process(byte[] input, int offset, int len, boolean finish) {
            int p;
            int op;
            int op2;
            int p2;
            int op3;
            byte b;
            byte b2;
            int op4;
            byte b3;
            byte[] alphabet = this.alphabet;
            byte[] output = this.output;
            int op5 = 0;
            int count = this.count;
            int p3 = offset;
            int len2 = len + offset;
            int v = -1;
            switch (this.tailLen) {
                case 1:
                    if (p3 + 2 <= len2) {
                        int p4 = p3 + 1;
                        int i = ((this.tail[0] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((input[p3] & BatteryStats.HistoryItem.CMD_NULL) << 8);
                        p3 = p4 + 1;
                        v = i | (input[p4] & BatteryStats.HistoryItem.CMD_NULL);
                        this.tailLen = 0;
                    }
                    break;
                case 2:
                    if (p3 + 1 <= len2) {
                        v = ((this.tail[0] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((this.tail[1] & BatteryStats.HistoryItem.CMD_NULL) << 8) | (input[p3] & BatteryStats.HistoryItem.CMD_NULL);
                        this.tailLen = 0;
                        p3++;
                    }
                    break;
            }
            if (v != -1) {
                int op6 = 0 + 1;
                output[0] = alphabet[(v >> 18) & 63];
                int op7 = op6 + 1;
                output[op6] = alphabet[(v >> 12) & 63];
                int op8 = op7 + 1;
                output[op7] = alphabet[(v >> 6) & 63];
                op5 = op8 + 1;
                output[op8] = alphabet[v & 63];
                count--;
                if (count == 0) {
                    if (this.do_cr) {
                        output[op5] = 13;
                        op5++;
                    }
                    op = op5 + 1;
                    output[op5] = 10;
                    count = 19;
                    p = p3;
                }
                while (p + 3 <= len2) {
                    int v2 = ((input[p] & BatteryStats.HistoryItem.CMD_NULL) << 16) | ((input[p + 1] & BatteryStats.HistoryItem.CMD_NULL) << 8) | (input[p + 2] & BatteryStats.HistoryItem.CMD_NULL);
                    output[op] = alphabet[(v2 >> 18) & 63];
                    output[op + 1] = alphabet[(v2 >> 12) & 63];
                    output[op + 2] = alphabet[(v2 >> 6) & 63];
                    output[op + 3] = alphabet[v2 & 63];
                    p3 = p + 3;
                    op5 = op + 4;
                    count--;
                    if (count == 0) {
                        if (this.do_cr) {
                            output[op5] = 13;
                            op5++;
                        }
                        op = op5 + 1;
                        output[op5] = 10;
                        count = 19;
                        p = p3;
                    }
                }
                if (!finish) {
                    if (p - this.tailLen == len2 - 1) {
                        int t = 0;
                        if (this.tailLen > 0) {
                            int t2 = 0 + 1;
                            b3 = this.tail[0];
                            t = t2;
                            p2 = p;
                        } else {
                            p2 = p + 1;
                            b3 = input[p];
                        }
                        int v3 = (b3 & BatteryStats.HistoryItem.CMD_NULL) << 4;
                        this.tailLen -= t;
                        int op9 = op + 1;
                        output[op] = alphabet[(v3 >> 6) & 63];
                        int op10 = op9 + 1;
                        output[op9] = alphabet[v3 & 63];
                        if (this.do_padding) {
                            int op11 = op10 + 1;
                            output[op10] = 61;
                            op10 = op11 + 1;
                            output[op11] = 61;
                        }
                        op2 = op10;
                        if (this.do_newline) {
                            if (this.do_cr) {
                                output[op2] = 13;
                                op2++;
                            }
                            op4 = op2 + 1;
                            output[op2] = 10;
                            op2 = op4;
                        }
                        if ($assertionsDisabled && this.tailLen != 0) {
                            throw new AssertionError();
                        }
                        if (!$assertionsDisabled && p2 != len2) {
                            throw new AssertionError();
                        }
                    } else {
                        if (p - this.tailLen == len2 - 2) {
                            int t3 = 0;
                            if (this.tailLen > 1) {
                                int t4 = 0 + 1;
                                b = this.tail[0];
                                t3 = t4;
                                p2 = p;
                            } else {
                                p2 = p + 1;
                                b = input[p];
                            }
                            int i2 = (b & BatteryStats.HistoryItem.CMD_NULL) << 10;
                            if (this.tailLen > 0) {
                                b2 = this.tail[t3];
                                t3++;
                            } else {
                                b2 = input[p2];
                                p2++;
                            }
                            int v4 = i2 | ((b2 & BatteryStats.HistoryItem.CMD_NULL) << 2);
                            this.tailLen -= t3;
                            int op12 = op + 1;
                            output[op] = alphabet[(v4 >> 12) & 63];
                            int op13 = op12 + 1;
                            output[op12] = alphabet[(v4 >> 6) & 63];
                            op2 = op13 + 1;
                            output[op13] = alphabet[v4 & 63];
                            if (this.do_padding) {
                                output[op2] = 61;
                                op2++;
                            }
                            if (this.do_newline) {
                                if (this.do_cr) {
                                    output[op2] = 13;
                                    op2++;
                                }
                                op4 = op2 + 1;
                                output[op2] = 10;
                                op2 = op4;
                            }
                        } else {
                            if (this.do_newline && op > 0 && count != 19) {
                                if (this.do_cr) {
                                    op3 = op + 1;
                                    output[op] = 13;
                                } else {
                                    op3 = op;
                                }
                                op = op3 + 1;
                                output[op3] = 10;
                            }
                            p2 = p;
                            op2 = op;
                        }
                        if ($assertionsDisabled) {
                        }
                        if (!$assertionsDisabled) {
                            throw new AssertionError();
                        }
                    }
                } else if (p == len2 - 1) {
                    byte[] bArr = this.tail;
                    int i3 = this.tailLen;
                    this.tailLen = i3 + 1;
                    bArr[i3] = input[p];
                    op2 = op;
                } else {
                    if (p == len2 - 2) {
                        byte[] bArr2 = this.tail;
                        int i4 = this.tailLen;
                        this.tailLen = i4 + 1;
                        bArr2[i4] = input[p];
                        byte[] bArr3 = this.tail;
                        int i5 = this.tailLen;
                        this.tailLen = i5 + 1;
                        bArr3[i5] = input[p + 1];
                    }
                    op2 = op;
                }
                this.op = op2;
                this.count = count;
                return true;
            }
            p = p3;
            op = op5;
            while (p + 3 <= len2) {
            }
            if (!finish) {
            }
            this.op = op2;
            this.count = count;
            return true;
        }
    }

    private Base64() {
    }
}
