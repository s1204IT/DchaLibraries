package javax.obex;

import android.os.SystemProperties;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class ObexHelper {
    public static final int BASE_PACKET_LENGTH = 3;
    public static final int LOWER_LIMIT_MAX_PACKET_SIZE = 255;
    public static final int MAX_CLIENT_PACKET_SIZE = 64512;
    public static final int MAX_PACKET_SIZE_INT = 65534;
    public static final int OBEX_AUTH_REALM_CHARSET_ASCII = 0;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_1 = 1;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_2 = 2;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_3 = 3;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_4 = 4;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_5 = 5;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_6 = 6;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_7 = 7;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_8 = 8;
    public static final int OBEX_AUTH_REALM_CHARSET_ISO_8859_9 = 9;
    public static final int OBEX_AUTH_REALM_CHARSET_UNICODE = 255;
    public static final int OBEX_BYTE_SEQ_HEADER_LEN = 3;
    public static final int OBEX_OPCODE_ABORT = 255;
    public static final int OBEX_OPCODE_CONNECT = 128;
    public static final int OBEX_OPCODE_DISCONNECT = 129;
    public static final int OBEX_OPCODE_FINAL_BIT_MASK = 128;
    public static final int OBEX_OPCODE_GET = 3;
    public static final int OBEX_OPCODE_GET_FINAL = 131;
    public static final int OBEX_OPCODE_PUT = 2;
    public static final int OBEX_OPCODE_PUT_FINAL = 130;
    public static final int OBEX_OPCODE_RESERVED = 4;
    public static final int OBEX_OPCODE_RESERVED_FINAL = 132;
    public static final int OBEX_OPCODE_SETPATH = 133;
    public static final byte OBEX_SRMP_WAIT = 1;
    public static final byte OBEX_SRM_DISABLE = 0;
    public static final byte OBEX_SRM_ENABLE = 1;
    public static final byte OBEX_SRM_SUPPORT = 2;
    private static final String TAG = "ObexHelper";
    public static final boolean VDBG = !SystemProperties.get("ro.build.type", "").equals("user");

    private ObexHelper() {
    }

    public static byte[] updateHeaderSet(HeaderSet headerSet, byte[] bArr) throws IOException {
        byte[] bArr2 = null;
        int i = 0;
        while (i < bArr.length) {
            try {
                int i2 = bArr[i] & 255;
                int i3 = i2 & 192;
                if (i3 == 0 || i3 == 64) {
                    int i4 = i + 1;
                    int i5 = ((bArr[i4] & 255) << 8) + (255 & bArr[i4 + 1]);
                    i = i4 + 2;
                    if (i5 <= 3) {
                        Log.e(TAG, "Remote sent an OBEX packet with incorrect header length = " + i5);
                    } else {
                        int i6 = i5 - 3;
                        byte[] bArr3 = new byte[i6];
                        System.arraycopy(bArr, i, bArr3, 0, i6);
                        boolean z = i6 != 0 && (i6 <= 0 || bArr3[i6 + (-1)] == 0);
                        switch (i2) {
                            case HeaderSet.TYPE:
                                if (!z) {
                                    try {
                                        headerSet.setHeader(i2, new String(bArr3, 0, bArr3.length, "ISO8859_1"));
                                    } catch (UnsupportedEncodingException e) {
                                        throw e;
                                    }
                                } else {
                                    headerSet.setHeader(i2, new String(bArr3, 0, bArr3.length - 1, "ISO8859_1"));
                                }
                                i += i6;
                                break;
                            case HeaderSet.TIME_ISO_8601:
                                try {
                                    String str = new String(bArr3, "ISO8859_1");
                                    Calendar calendar = Calendar.getInstance();
                                    if (str.length() == 16 && str.charAt(15) == 'Z') {
                                        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    }
                                    calendar.set(1, Integer.parseInt(str.substring(0, 4)));
                                    calendar.set(2, Integer.parseInt(str.substring(4, 6)));
                                    calendar.set(5, Integer.parseInt(str.substring(6, 8)));
                                    calendar.set(11, Integer.parseInt(str.substring(9, 11)));
                                    calendar.set(12, Integer.parseInt(str.substring(11, 13)));
                                    calendar.set(13, Integer.parseInt(str.substring(13, 15)));
                                    headerSet.setHeader(68, calendar);
                                    i += i6;
                                } catch (UnsupportedEncodingException e2) {
                                    throw e2;
                                }
                                break;
                            case HeaderSet.BODY:
                            case HeaderSet.END_OF_BODY:
                                bArr2 = new byte[i6 + 1];
                                bArr2[0] = (byte) i2;
                                System.arraycopy(bArr, i, bArr2, 1, i6);
                                i += i6;
                                break;
                            case HeaderSet.AUTH_CHALLENGE:
                                headerSet.mAuthChall = new byte[i6];
                                System.arraycopy(bArr, i, headerSet.mAuthChall, 0, i6);
                                i += i6;
                                break;
                            case HeaderSet.AUTH_RESPONSE:
                                headerSet.mAuthResp = new byte[i6];
                                System.arraycopy(bArr, i, headerSet.mAuthResp, 0, i6);
                                i += i6;
                                break;
                            default:
                                if (i3 == 0) {
                                    headerSet.setHeader(i2, convertToUnicode(bArr3, true));
                                } else {
                                    headerSet.setHeader(i2, bArr3);
                                }
                                i += i6;
                                break;
                        }
                    }
                } else if (i3 == 128) {
                    int i7 = i + 1;
                    try {
                        headerSet.setHeader(i2, Byte.valueOf(bArr[i7]));
                    } catch (Exception e3) {
                    }
                    i = i7 + 1;
                } else if (i3 == 192) {
                    int i8 = i + 1;
                    byte[] bArr4 = new byte[4];
                    System.arraycopy(bArr, i8, bArr4, 0, 4);
                    if (i2 != 196) {
                        if (i2 == 203) {
                            try {
                                headerSet.mConnectionID = new byte[4];
                                System.arraycopy(bArr4, 0, headerSet.mConnectionID, 0, 4);
                            } catch (Exception e4) {
                                throw new IOException("Header was not formatted properly", e4);
                            }
                        } else {
                            headerSet.setHeader(i2, Long.valueOf(convertToLong(bArr4)));
                        }
                    } else {
                        Calendar calendar2 = Calendar.getInstance();
                        calendar2.setTime(new Date(convertToLong(bArr4) * 1000));
                        headerSet.setHeader(196, calendar2);
                    }
                    i = i8 + 4;
                }
            } catch (IOException e5) {
                throw new IOException("Header was not formatted properly", e5);
            }
        }
        return bArr2;
    }

    public static byte[] createHeader(HeaderSet headerSet, boolean z) {
        byte[] byteArray;
        byte[] bArr = new byte[2];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            try {
                if (headerSet.mConnectionID != null && headerSet.getHeader(70) == null) {
                    byteArrayOutputStream.write(-53);
                    byteArrayOutputStream.write(headerSet.mConnectionID);
                }
                Long l = (Long) headerSet.getHeader(192);
                if (l != null) {
                    byteArrayOutputStream.write(-64);
                    byteArrayOutputStream.write(convertToByteArray(l.longValue()));
                    if (z) {
                        headerSet.setHeader(192, null);
                    }
                }
                String str = (String) headerSet.getHeader(1);
                if (str != null) {
                    byteArrayOutputStream.write(1);
                    byte[] bArrConvertToUnicodeByteArray = convertToUnicodeByteArray(str);
                    int length = bArrConvertToUnicodeByteArray.length + 3;
                    bArr[0] = (byte) ((length >> 8) & 255);
                    bArr[1] = (byte) (length & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArrConvertToUnicodeByteArray);
                    if (z) {
                        headerSet.setHeader(1, null);
                    }
                } else if (headerSet.getEmptyNameHeader()) {
                    byteArrayOutputStream.write(1);
                    bArr[0] = 0;
                    bArr[1] = 3;
                    byteArrayOutputStream.write(bArr);
                }
                String str2 = (String) headerSet.getHeader(66);
                if (str2 != null) {
                    byteArrayOutputStream.write(66);
                    try {
                        byte[] bytes = str2.getBytes("ISO8859_1");
                        int length2 = bytes.length + 4;
                        bArr[0] = (byte) ((length2 >> 8) & 255);
                        bArr[1] = (byte) (length2 & 255);
                        byteArrayOutputStream.write(bArr);
                        byteArrayOutputStream.write(bytes);
                        byteArrayOutputStream.write(0);
                        if (z) {
                            headerSet.setHeader(66, null);
                        }
                    } catch (UnsupportedEncodingException e) {
                        throw e;
                    }
                }
                Long l2 = (Long) headerSet.getHeader(195);
                if (l2 != null) {
                    byteArrayOutputStream.write(-61);
                    byteArrayOutputStream.write(convertToByteArray(l2.longValue()));
                    if (z) {
                        headerSet.setHeader(195, null);
                    }
                }
                Calendar calendar = (Calendar) headerSet.getHeader(68);
                if (calendar != null) {
                    StringBuffer stringBuffer = new StringBuffer();
                    int i = calendar.get(1);
                    for (int i2 = i; i2 < 1000; i2 *= 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i);
                    int i3 = calendar.get(2);
                    if (i3 < 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i3);
                    int i4 = calendar.get(5);
                    if (i4 < 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i4);
                    stringBuffer.append("T");
                    int i5 = calendar.get(11);
                    if (i5 < 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i5);
                    int i6 = calendar.get(12);
                    if (i6 < 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i6);
                    int i7 = calendar.get(13);
                    if (i7 < 10) {
                        stringBuffer.append("0");
                    }
                    stringBuffer.append(i7);
                    if (calendar.getTimeZone().getID().equals("UTC")) {
                        stringBuffer.append("Z");
                    }
                    try {
                        byte[] bytes2 = stringBuffer.toString().getBytes("ISO8859_1");
                        int length3 = bytes2.length + 3;
                        bArr[0] = (byte) ((length3 >> 8) & 255);
                        bArr[1] = (byte) (length3 & 255);
                        byteArrayOutputStream.write(68);
                        byteArrayOutputStream.write(bArr);
                        byteArrayOutputStream.write(bytes2);
                        if (z) {
                            headerSet.setHeader(68, null);
                        }
                    } catch (UnsupportedEncodingException e2) {
                        throw e2;
                    }
                }
                Calendar calendar2 = (Calendar) headerSet.getHeader(196);
                if (calendar2 != null) {
                    byteArrayOutputStream.write(196);
                    byteArrayOutputStream.write(convertToByteArray(calendar2.getTime().getTime() / 1000));
                    if (z) {
                        headerSet.setHeader(196, null);
                    }
                }
                String str3 = (String) headerSet.getHeader(5);
                if (str3 != null) {
                    byteArrayOutputStream.write(5);
                    byte[] bArrConvertToUnicodeByteArray2 = convertToUnicodeByteArray(str3);
                    int length4 = bArrConvertToUnicodeByteArray2.length + 3;
                    bArr[0] = (byte) ((length4 >> 8) & 255);
                    bArr[1] = (byte) (length4 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArrConvertToUnicodeByteArray2);
                    if (z) {
                        headerSet.setHeader(5, null);
                    }
                }
                byte[] bArr2 = (byte[]) headerSet.getHeader(70);
                if (bArr2 != null) {
                    byteArrayOutputStream.write(70);
                    int length5 = bArr2.length + 3;
                    bArr[0] = (byte) ((length5 >> 8) & 255);
                    bArr[1] = (byte) (length5 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArr2);
                    if (z) {
                        headerSet.setHeader(70, null);
                    }
                }
                byte[] bArr3 = (byte[]) headerSet.getHeader(71);
                if (bArr3 != null) {
                    byteArrayOutputStream.write(71);
                    int length6 = bArr3.length + 3;
                    bArr[0] = (byte) ((length6 >> 8) & 255);
                    bArr[1] = (byte) (length6 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArr3);
                    if (z) {
                        headerSet.setHeader(71, null);
                    }
                }
                byte[] bArr4 = (byte[]) headerSet.getHeader(74);
                if (bArr4 != null) {
                    byteArrayOutputStream.write(74);
                    int length7 = bArr4.length + 3;
                    bArr[0] = (byte) ((length7 >> 8) & 255);
                    bArr[1] = (byte) (length7 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArr4);
                    if (z) {
                        headerSet.setHeader(74, null);
                    }
                }
                byte[] bArr5 = (byte[]) headerSet.getHeader(76);
                if (bArr5 != null) {
                    byteArrayOutputStream.write(76);
                    int length8 = bArr5.length + 3;
                    bArr[0] = (byte) ((length8 >> 8) & 255);
                    bArr[1] = (byte) (length8 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArr5);
                    if (z) {
                        headerSet.setHeader(76, null);
                    }
                }
                byte[] bArr6 = (byte[]) headerSet.getHeader(79);
                if (bArr6 != null) {
                    byteArrayOutputStream.write(79);
                    int length9 = bArr6.length + 3;
                    bArr[0] = (byte) ((length9 >> 8) & 255);
                    bArr[1] = (byte) (length9 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(bArr6);
                    if (z) {
                        headerSet.setHeader(79, null);
                    }
                }
                for (int i8 = 0; i8 < 16; i8++) {
                    int i9 = i8 + 48;
                    String str4 = (String) headerSet.getHeader(i9);
                    if (str4 != null) {
                        byteArrayOutputStream.write(((byte) i8) + 48);
                        byte[] bArrConvertToUnicodeByteArray3 = convertToUnicodeByteArray(str4);
                        int length10 = bArrConvertToUnicodeByteArray3.length + 3;
                        bArr[0] = (byte) ((length10 >> 8) & 255);
                        bArr[1] = (byte) (length10 & 255);
                        byteArrayOutputStream.write(bArr);
                        byteArrayOutputStream.write(bArrConvertToUnicodeByteArray3);
                        if (z) {
                            headerSet.setHeader(i9, null);
                        }
                    }
                    int i10 = i8 + 112;
                    byte[] bArr7 = (byte[]) headerSet.getHeader(i10);
                    if (bArr7 != null) {
                        byteArrayOutputStream.write(((byte) i8) + 112);
                        int length11 = bArr7.length + 3;
                        bArr[0] = (byte) ((length11 >> 8) & 255);
                        bArr[1] = (byte) (length11 & 255);
                        byteArrayOutputStream.write(bArr);
                        byteArrayOutputStream.write(bArr7);
                        if (z) {
                            headerSet.setHeader(i10, null);
                        }
                    }
                    int i11 = i8 + ResponseCodes.OBEX_HTTP_MULT_CHOICE;
                    Byte b = (Byte) headerSet.getHeader(i11);
                    if (b != null) {
                        byteArrayOutputStream.write(((byte) i8) + 176);
                        byteArrayOutputStream.write(b.byteValue());
                        if (z) {
                            headerSet.setHeader(i11, null);
                        }
                    }
                    int i12 = i8 + 240;
                    Long l3 = (Long) headerSet.getHeader(i12);
                    if (l3 != null) {
                        byteArrayOutputStream.write(((byte) i8) + 240);
                        byteArrayOutputStream.write(convertToByteArray(l3.longValue()));
                        if (z) {
                            headerSet.setHeader(i12, null);
                        }
                    }
                }
                if (headerSet.mAuthChall != null) {
                    byteArrayOutputStream.write(77);
                    int length12 = headerSet.mAuthChall.length + 3;
                    bArr[0] = (byte) ((length12 >> 8) & 255);
                    bArr[1] = (byte) (length12 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(headerSet.mAuthChall);
                    if (z) {
                        headerSet.mAuthChall = null;
                    }
                }
                if (headerSet.mAuthResp != null) {
                    byteArrayOutputStream.write(78);
                    int length13 = headerSet.mAuthResp.length + 3;
                    bArr[0] = (byte) ((length13 >> 8) & 255);
                    bArr[1] = (byte) (length13 & 255);
                    byteArrayOutputStream.write(bArr);
                    byteArrayOutputStream.write(headerSet.mAuthResp);
                    if (z) {
                        headerSet.mAuthResp = null;
                    }
                }
                Byte b2 = (Byte) headerSet.getHeader(HeaderSet.SINGLE_RESPONSE_MODE);
                if (b2 != null) {
                    byteArrayOutputStream.write(-105);
                    byteArrayOutputStream.write(b2.byteValue());
                    if (z) {
                        headerSet.setHeader(HeaderSet.SINGLE_RESPONSE_MODE, null);
                    }
                }
                Byte b3 = (Byte) headerSet.getHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER);
                if (b3 != null) {
                    byteArrayOutputStream.write(-104);
                    byteArrayOutputStream.write(b3.byteValue());
                    if (z) {
                        headerSet.setHeader(HeaderSet.SINGLE_RESPONSE_MODE_PARAMETER, null);
                    }
                }
                byteArray = byteArrayOutputStream.toByteArray();
                byteArrayOutputStream.close();
            } catch (Exception e3) {
            }
        } catch (IOException e4) {
            byteArray = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
        } catch (Throwable th) {
            byteArrayOutputStream.toByteArray();
            try {
                byteArrayOutputStream.close();
                throw th;
            } catch (Exception e5) {
                throw th;
            }
        }
        return byteArray;
    }

    public static int findHeaderEnd(byte[] bArr, int i, int i2) {
        int i3;
        int i4 = 0;
        int i5 = i;
        int i6 = -1;
        while (i4 < i2 && i5 < bArr.length) {
            int i7 = (bArr[i5] < 0 ? bArr[i5] + 256 : bArr[i5]) & 192;
            if (i7 == 0 || i7 == 64) {
                int i8 = i5 + 1;
                int i9 = (bArr[i8] < 0 ? bArr[i8] + 256 : bArr[i8]) << 8;
                int i10 = i8 + 1;
                int i11 = (i9 + (bArr[i10] < 0 ? bArr[i10] + 256 : bArr[i10])) - 3;
                i5 = i10 + 1 + i11;
                i3 = i11 + 3 + i4;
            } else if (i7 != 128) {
                if (i7 == 192) {
                    i5 += 5;
                    i3 = i4 + 5;
                } else {
                    i3 = i4;
                }
            } else {
                i5 = i5 + 1 + 1;
                i3 = i4 + 2;
            }
            int i12 = i3;
            i6 = i4;
            i4 = i12;
        }
        if (i6 == 0) {
            if (i4 >= i2) {
                return -1;
            }
            return bArr.length;
        }
        return i6 + i;
    }

    public static long convertToLong(byte[] bArr) {
        long j = 0;
        long j2 = 0;
        for (int length = bArr.length - 1; length >= 0; length--) {
            long j3 = bArr[length];
            if (j3 < 0) {
                j3 += 256;
            }
            j |= j3 << ((int) j2);
            j2 += 8;
        }
        return j;
    }

    public static byte[] convertToByteArray(long j) {
        return new byte[]{(byte) ((j >> 24) & 255), (byte) ((j >> 16) & 255), (byte) ((j >> 8) & 255), (byte) (j & 255)};
    }

    public static byte[] convertToUnicodeByteArray(String str) {
        if (str == null) {
            return null;
        }
        char[] charArray = str.toCharArray();
        byte[] bArr = new byte[(charArray.length * 2) + 2];
        for (int i = 0; i < charArray.length; i++) {
            int i2 = i * 2;
            bArr[i2] = (byte) (charArray[i] >> '\b');
            bArr[i2 + 1] = (byte) charArray[i];
        }
        bArr[bArr.length - 2] = 0;
        bArr[bArr.length - 1] = 0;
        return bArr;
    }

    public static byte[] getTagValue(byte b, byte[] bArr) {
        int iFindTag = findTag(b, bArr);
        if (iFindTag == -1) {
            return null;
        }
        int i = iFindTag + 1;
        int i2 = bArr[i] & 255;
        byte[] bArr2 = new byte[i2];
        System.arraycopy(bArr, i + 1, bArr2, 0, i2);
        return bArr2;
    }

    public static int findTag(byte b, byte[] bArr) {
        if (bArr == null) {
            return -1;
        }
        int i = 0;
        while (i < bArr.length && bArr[i] != b) {
            i += (bArr[i + 1] & 255) + 2;
        }
        if (i >= bArr.length) {
            return -1;
        }
        return i;
    }

    public static String convertToUnicode(byte[] bArr, boolean z) {
        if (bArr == 0 || bArr.length == 0) {
            return null;
        }
        int length = bArr.length;
        if (length % 2 != 0) {
            throw new IllegalArgumentException("Byte array not of a valid form");
        }
        int i = length >> 1;
        if (z) {
            i--;
        }
        char[] cArr = new char[i];
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = 2 * i2;
            int i4 = bArr[i3];
            int i5 = bArr[i3 + 1];
            if (i4 < 0) {
                i4 += 256;
            }
            if (i5 < 0) {
                i5 += 256;
            }
            if (i4 == 0 && i5 == 0) {
                return new String(cArr, 0, i2);
            }
            cArr[i2] = (char) (i5 | (i4 << 8));
        }
        return new String(cArr);
    }

    public static byte[] computeMd5Hash(byte[] bArr) {
        try {
            return MessageDigest.getInstance("MD5").digest(bArr);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] computeAuthenticationChallenge(byte[] bArr, String str, boolean z, boolean z2) throws IOException {
        byte[] bArr2;
        if (bArr.length != 16) {
            throw new IllegalArgumentException("Nonce must be 16 bytes long");
        }
        if (str == null) {
            bArr2 = new byte[21];
        } else {
            if (str.length() >= 255) {
                throw new IllegalArgumentException("Realm must be less then 255 bytes");
            }
            byte[] bArr3 = new byte[str.length() + 24];
            bArr3[21] = 2;
            bArr3[22] = (byte) (str.length() + 1);
            bArr3[23] = 1;
            System.arraycopy(str.getBytes("ISO8859_1"), 0, bArr3, 24, str.length());
            bArr2 = bArr3;
        }
        bArr2[0] = 0;
        bArr2[1] = 16;
        System.arraycopy(bArr, 0, bArr2, 2, 16);
        bArr2[18] = 1;
        bArr2[19] = 1;
        bArr2[20] = 0;
        if (!z) {
            bArr2[20] = (byte) (bArr2[20] | 2);
        }
        if (z2) {
            bArr2[20] = (byte) (bArr2[20] | 1);
        }
        return bArr2;
    }

    public static int getMaxTxPacketSize(ObexTransport obexTransport) {
        return validateMaxPacketSize(obexTransport.getMaxTransmitPacketSize());
    }

    public static int getMaxRxPacketSize(ObexTransport obexTransport) {
        return validateMaxPacketSize(obexTransport.getMaxReceivePacketSize());
    }

    private static int validateMaxPacketSize(int i) {
        if (VDBG && i > 65534) {
            Log.w(TAG, "The packet size supported for the connection (" + i + ") is larger than the configured OBEX packet size: " + MAX_PACKET_SIZE_INT);
        }
        if (i == -1) {
            return MAX_PACKET_SIZE_INT;
        }
        if (i < 255) {
            throw new IllegalArgumentException(i + " is less that the lower limit: 255");
        }
        return i;
    }
}
