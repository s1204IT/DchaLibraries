package com.android.bluetooth.map;

import android.content.Context;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.util.Xml;
import com.android.bluetooth.mapapi.BluetoothMapContract;
import com.android.i18n.phonenumbers.AsYouTypeFormatter;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.vcard.VCardConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapUtils {
    public static final long CONVO_ID_TYPE_EMAIL_IM = 2;
    public static final long CONVO_ID_TYPE_SMS_MMS = 1;
    private static final byte ESCAPE_CHAR = 61;
    private static final long HANDLE_TYPE_EMAIL_MASK = 144115188075855872L;
    private static final long HANDLE_TYPE_IM_MASK = 1152921504606846976L;
    private static final long HANDLE_TYPE_MASK = -72057594037927936L;
    private static final long HANDLE_TYPE_MMS_MASK = 72057594037927936L;
    private static final long HANDLE_TYPE_SMS_CDMA_MASK = 576460752303423488L;
    private static final long HANDLE_TYPE_SMS_GSM_MASK = 288230376151711744L;
    private static final int LONG_LONG_LENGTH = 32;
    static final int MAP_EVENT_REPORT_V10 = 10;
    static final int MAP_EVENT_REPORT_V11 = 11;
    static final int MAP_EVENT_REPORT_V12 = 12;
    static final int MAP_FEATURE_BROWSING_BIT = 4;
    static final int MAP_FEATURE_CONVERSATION_VERSION_COUNTER_BIT = 8192;
    static final int MAP_FEATURE_DATABASE_INDENTIFIER_BIT = 2048;
    static final int MAP_FEATURE_DEFAULT_BITMASK = 31;
    static final int MAP_FEATURE_DEFINED_TIMESTAMP_FORMAT_BIT = 262144;
    static final int MAP_FEATURE_DELETE_BIT = 16;
    static final int MAP_FEATURE_EVENT_REPORT_V12_BIT = 128;
    static final int MAP_FEATURE_EXTENDED_EVENT_REPORT_11_BIT = 64;
    static final int MAP_FEATURE_FOLDER_VERSION_COUNTER_BIT = 4096;
    static final int MAP_FEATURE_INSTANCE_INFORMATION_BIT = 32;
    static final int MAP_FEATURE_MESSAGE_FORMAT_V11_BIT = 256;
    static final int MAP_FEATURE_MESSAGE_LISTING_FORMAT_V11_BIT = 512;
    static final int MAP_FEATURE_NOTIFICATION_BIT = 2;
    static final int MAP_FEATURE_NOTIFICATION_FILTERING_BIT = 131072;
    static final int MAP_FEATURE_NOTIFICATION_REGISTRATION_BIT = 1;
    static final int MAP_FEATURE_PARTICIPANT_CHAT_STATE_CHANGE_BIT = 32768;
    static final int MAP_FEATURE_PARTICIPANT_PRESENCE_CHANGE_BIT = 16384;
    static final int MAP_FEATURE_PBAP_CONTACT_CROSS_REFERENCE_BIT = 65536;
    static final int MAP_FEATURE_PERSISTENT_MESSAGE_HANDLE_BIT = 1024;
    static final int MAP_FEATURE_UPLOADING_BIT = 8;
    static final int MAP_MESSAGE_FORMAT_V10 = 10;
    static final int MAP_MESSAGE_FORMAT_V11 = 11;
    static final int MAP_MESSAGE_LISTING_FORMAT_V10 = 10;
    static final int MAP_MESSAGE_LISTING_FORMAT_V11 = 11;
    static final String MAP_V10_STR = "1.0";
    static final String MAP_V11_STR = "1.1";
    static final String MAP_V12_STR = "1.2";
    private static final byte SPACE = 32;
    private static final byte TAB = 9;
    private static final String TAG = "BluetoothMapUtils";
    private static final boolean V = false;
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final Pattern PATTERN = Pattern.compile("=\\?(.+?)\\?(.)\\?(.+?(?=\\?=))\\?=");

    public enum TYPE {
        NONE,
        EMAIL,
        SMS_GSM,
        SMS_CDMA,
        MMS,
        IM;

        private static TYPE[] sAllValues = values();

        public static TYPE fromOrdinal(int i) {
            if (i < sAllValues.length) {
                return sAllValues[i];
            }
            return NONE;
        }
    }

    public static String getDateTimeString(long j) {
        return new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date(j));
    }

    public static void printCursor(Cursor cursor) {
        if (D) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nprintCursor:\n");
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                if (cursor.getColumnName(i).equals(BluetoothMapContract.MessageColumns.DATE) || cursor.getColumnName(i).equals(BluetoothMapContract.ConversationColumns.LAST_THREAD_ACTIVITY) || cursor.getColumnName(i).equals(BluetoothMapContract.ChatStatusColumns.LAST_ACTIVE) || cursor.getColumnName(i).equals(BluetoothMapContract.PresenceColumns.LAST_ONLINE)) {
                    sb.append("  ");
                    sb.append(cursor.getColumnName(i));
                    sb.append(" : ");
                    sb.append(getDateTimeString(cursor.getLong(i)));
                    sb.append("\n");
                } else {
                    sb.append("  ");
                    sb.append(cursor.getColumnName(i));
                    sb.append(" : ");
                    sb.append(cursor.getString(i));
                    sb.append("\n");
                }
            }
            Log.d(TAG, sb.toString());
        }
    }

    public static String getLongAsString(long j) {
        char[] cArr = new char[16];
        int i = (int) (j & (-1));
        int i2 = (int) ((j >> 32) & (-1));
        for (int i3 = 0; i3 < 8; i3++) {
            int i4 = i2 & 15;
            int i5 = 55;
            cArr[7 - i3] = (char) (i4 + (i4 < 10 ? 48 : 55));
            i2 >>= 4;
            int i6 = i & 15;
            if (i6 < 10) {
                i5 = 48;
            }
            cArr[15 - i3] = (char) (i6 + i5);
            i >>= 4;
        }
        return new String(cArr);
    }

    public static long getLongFromString(String str) throws UnsupportedEncodingException {
        byte b;
        if (str == null) {
            throw new NullPointerException();
        }
        byte[] bytes = str.getBytes("US-ASCII");
        int length = bytes.length;
        long j = 0;
        int i = 0;
        for (int i2 = 0; i2 != length; i2++) {
            byte b2 = bytes[i2];
            if (b2 >= 48 && b2 <= 57) {
                b = (byte) (b2 - 48);
            } else if (b2 >= 65 && b2 <= 70) {
                b = (byte) (b2 - 55);
            } else if (b2 >= 97 && b2 <= 102) {
                b = (byte) (b2 - 87);
            } else {
                if (b2 > 32 && b2 != 45) {
                    throw new NumberFormatException("Invalid character:" + ((int) b2));
                }
            }
            j = (j << 4) + ((long) b);
            i++;
            if (i > 16) {
                throw new NullPointerException("String to large - count: " + i);
            }
        }
        return j;
    }

    public static String getLongLongAsString(long j, long j2) {
        char[] cArr = new char[32];
        int i = (int) (j & (-1));
        int i2 = (int) (j2 & (-1));
        int i3 = (int) ((j2 >> 32) & (-1));
        int i4 = 0;
        int i5 = i2;
        int i6 = (int) ((j >> 32) & (-1));
        int i7 = 0;
        while (true) {
            int i8 = 48;
            if (i7 >= 8) {
                break;
            }
            int i9 = i6 & 15;
            int i10 = i9 + (i9 < 10 ? 48 : 55);
            int i11 = i3 & 15;
            int i12 = i11 + (i11 < 10 ? 48 : 55);
            cArr[23 - i7] = (char) i10;
            cArr[7 - i7] = (char) i12;
            i6 >>= 4;
            i3 >>= 4;
            int i13 = i & 15;
            int i14 = i13 + (i13 < 10 ? 48 : 55);
            int i15 = i5 & 15;
            if (i15 >= 10) {
                i8 = 55;
            }
            cArr[31 - i7] = (char) i14;
            cArr[15 - i7] = (char) (i15 + i8);
            i >>= 4;
            i5 >>= 4;
            i7++;
        }
        while (i4 < 32 && cArr[i4] == '0') {
            i4++;
        }
        return new String(cArr, i4, 32 - i4);
    }

    public static String getMapHandle(long j, TYPE type) {
        if (type != null) {
            switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[type.ordinal()]) {
                case 1:
                    return getLongAsString(j | HANDLE_TYPE_MMS_MASK);
                case 2:
                    return getLongAsString(j | HANDLE_TYPE_SMS_GSM_MASK);
                case 3:
                    return getLongAsString(j | HANDLE_TYPE_SMS_CDMA_MASK);
                case 4:
                    return getLongAsString(j | HANDLE_TYPE_EMAIL_MASK);
                case 5:
                    return getLongAsString(j | HANDLE_TYPE_IM_MASK);
                case 6:
                    return "-1";
                default:
                    throw new IllegalArgumentException("Message type not supported");
            }
        }
        if (!D) {
            return "-1";
        }
        Log.e(TAG, " Invalid messageType input");
        return "-1";
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE = new int[TYPE.values().length];

        static {
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.MMS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.SMS_GSM.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.SMS_CDMA.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.EMAIL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.IM.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[TYPE.NONE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public static String getMapConvoHandle(long j, TYPE type) {
        switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$map$BluetoothMapUtils$TYPE[type.ordinal()]) {
            case 1:
            case 2:
            case 3:
                return getLongLongAsString(j, 1L);
            case 4:
            case 5:
                return getLongLongAsString(j, 2L);
            default:
                throw new IllegalArgumentException("Message type not supported");
        }
    }

    public static long getMsgHandleAsLong(String str) {
        return Long.parseLong(str, 16);
    }

    public static long getCpHandle(String str) {
        long msgHandleAsLong = getMsgHandleAsLong(str);
        if (D) {
            Log.d(TAG, "-> MAP handle:" + str);
        }
        long j = msgHandleAsLong & 72057594037927935L;
        if (D) {
            Log.d(TAG, "->CP handle:" + j);
        }
        return j;
    }

    public static TYPE getMsgTypeFromHandle(String str) {
        long msgHandleAsLong = getMsgHandleAsLong(str);
        if ((HANDLE_TYPE_MMS_MASK & msgHandleAsLong) != 0) {
            return TYPE.MMS;
        }
        if ((HANDLE_TYPE_EMAIL_MASK & msgHandleAsLong) != 0) {
            return TYPE.EMAIL;
        }
        if ((HANDLE_TYPE_SMS_GSM_MASK & msgHandleAsLong) != 0) {
            return TYPE.SMS_GSM;
        }
        if ((HANDLE_TYPE_SMS_CDMA_MASK & msgHandleAsLong) != 0) {
            return TYPE.SMS_CDMA;
        }
        if ((msgHandleAsLong & HANDLE_TYPE_IM_MASK) != 0) {
            return TYPE.IM;
        }
        throw new IllegalArgumentException("Message type not found in handle string.");
    }

    public static String stripInvalidChars(String str) {
        if (str == null) {
            return "";
        }
        char[] cArr = new char[str.length()];
        int length = str.length();
        int i = 0;
        int i2 = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if ((cCharAt >= ' ' && cCharAt <= 55295) || (cCharAt >= 57344 && cCharAt <= 65533)) {
                cArr[i2] = cCharAt;
                i2++;
            }
            i++;
        }
        if (i == i2) {
            return str;
        }
        return new String(cArr, 0, i2);
    }

    public static byte[] truncateUtf8StringToBytearray(String str, int i) throws UnsupportedEncodingException {
        byte[] bArr = new byte[str.length() + 1];
        try {
            System.arraycopy(str.getBytes("UTF-8"), 0, bArr, 0, str.length());
            if (bArr.length > i) {
                int i2 = i - 1;
                if ((bArr[i2] & 192) == 128) {
                    for (int i3 = i - 2; i3 >= 0; i3--) {
                        if ((bArr[i3] & 192) == 192) {
                            byte[] bArrCopyOf = Arrays.copyOf(bArr, i3 + 1);
                            bArrCopyOf[i3] = 0;
                            return bArrCopyOf;
                        }
                    }
                    return bArr;
                }
                byte[] bArrCopyOf2 = Arrays.copyOf(bArr, i);
                bArrCopyOf2[i2] = 0;
                return bArrCopyOf2;
            }
            return bArr;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "truncateUtf8StringToBytearray: getBytes exception ", e);
            throw e;
        }
    }

    public static String stripEncoding(String str) {
        if (str.contains("=?") && str.contains("?=")) {
            Matcher matcher = PATTERN.matcher(str);
            while (matcher.find()) {
                String strGroup = matcher.group(0);
                String strGroup2 = matcher.group(1);
                String strGroup3 = matcher.group(2);
                String strGroup4 = matcher.group(3);
                Log.v(TAG, "Matching:" + strGroup + "\nCharset: " + strGroup2 + "\nEncoding : " + strGroup3 + "\nText: " + strGroup4);
                if (strGroup3.equalsIgnoreCase("Q")) {
                    Log.d(TAG, "StripEncoding: Quoted Printable string : " + strGroup4);
                    str = str.replace(strGroup, new String(quotedPrintableToUtf8(strGroup4, strGroup2)));
                } else if (strGroup3.equalsIgnoreCase(VCardConstants.PARAM_ENCODING_B)) {
                    try {
                        Log.d(TAG, "StripEncoding: base64 string : " + strGroup4);
                        String str2 = new String(Base64.decode(strGroup4.getBytes(strGroup2), 0), strGroup2);
                        Log.d(TAG, "StripEncoding: decoded string : " + str2);
                        str = str.replace(strGroup, str2);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "stripEncoding: Unsupported charset: " + strGroup2);
                    } catch (IllegalArgumentException e2) {
                        Log.e(TAG, "stripEncoding: string not encoded as base64: " + strGroup4);
                    }
                } else {
                    Log.e(TAG, "stripEncoding: Hit unknown encoding: " + strGroup3);
                }
            }
        }
        return str;
    }

    public static byte[] quotedPrintableToUtf8(String str, String str2) {
        byte[] bytes;
        String str3;
        String str4;
        int i;
        byte[] bArr = new byte[str.length()];
        try {
            bytes = str.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            bytes = null;
        }
        if (bytes == null) {
            return "".getBytes();
        }
        int length = bytes.length - 2;
        int i2 = 0;
        int i3 = 0;
        while (i2 < length) {
            byte b = bytes[i2];
            if (b == 61) {
                int i4 = i2 + 1;
                byte b2 = bytes[i4];
                i2 = i4 + 1;
                byte b3 = bytes[i2];
                if (b2 == 13 && b3 == 10) {
                    i2++;
                } else if (((b2 >= 48 && b2 <= 57) || ((b2 >= 65 && b2 <= 70) || (b2 >= 97 && b2 <= 102))) && ((b3 >= 48 && b3 <= 57) || ((b3 >= 65 && b3 <= 70) || (b3 >= 97 && b3 <= 102)))) {
                    if (b2 <= 57) {
                        b2 = (byte) (b2 - 48);
                    } else if (b2 <= 70) {
                        b2 = (byte) ((b2 - 65) + 10);
                    } else if (b2 <= 102) {
                        b2 = (byte) ((b2 - 97) + 10);
                    }
                    if (b3 <= 57) {
                        b3 = (byte) (b3 - 48);
                    } else if (b3 <= 70) {
                        b3 = (byte) ((b3 - 65) + 10);
                    } else if (b3 <= 102) {
                        b3 = (byte) ((b3 - 97) + 10);
                    }
                    i = i3 + 1;
                    bArr[i3] = (byte) ((b2 << 4) | b3);
                } else {
                    Log.w(TAG, "Received wrongly quoted printable encoded text. Continuing at best effort...");
                    i = i3 + 1;
                    bArr[i3] = b;
                    i2 -= 2;
                }
            } else {
                i = i3 + 1;
                bArr[i3] = b;
            }
            i3 = i;
            i2++;
        }
        while (i2 < bytes.length) {
            bArr[i3] = bytes[i2];
            i3++;
            i2++;
        }
        if (str2 == null) {
            str3 = "UTF-8";
        } else {
            String upperCase = str2.toUpperCase();
            try {
                if (!Charset.isSupported(upperCase)) {
                    str3 = "UTF-8";
                } else {
                    str3 = upperCase;
                }
            } catch (IllegalCharsetNameException e2) {
                Log.w(TAG, "Received unknown charset: " + upperCase + " - using UTF-8.");
                str3 = "UTF-8";
            }
        }
        try {
            str4 = new String(bArr, 0, i3, str3);
        } catch (UnsupportedEncodingException e3) {
            try {
                str4 = new String(bArr, 0, i3, "UTF-8");
            } catch (UnsupportedEncodingException e4) {
                Log.e(TAG, "quotedPrintableToUtf8: " + e3);
                str4 = null;
            }
        }
        return str4.getBytes();
    }

    public static final String encodeQuotedPrintable(byte[] bArr) {
        if (bArr == 0) {
            return null;
        }
        BitSet bitSet = new BitSet(256);
        for (int i = 33; i <= 60; i++) {
            bitSet.set(i);
        }
        for (int i2 = 62; i2 <= 126; i2++) {
            bitSet.set(i2);
        }
        bitSet.set(9);
        bitSet.set(32);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (int i3 = 0; i3 < bArr.length; i3++) {
            int i4 = bArr[i3];
            if (i4 < 0) {
                i4 += 256;
            }
            if (bitSet.get(i4)) {
                byteArrayOutputStream.write(i4);
            } else {
                byteArrayOutputStream.write(61);
                char upperCase = Character.toUpperCase(Character.forDigit((i4 >> 4) & 15, 16));
                char upperCase2 = Character.toUpperCase(Character.forDigit(i4 & 15, 16));
                byteArrayOutputStream.write(upperCase);
                byteArrayOutputStream.write(upperCase2);
            }
        }
        try {
            return byteArrayOutputStream.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    public static String detectCountry(Context context) {
        try {
            Country countryDetectCountry = ((CountryDetector) context.getSystemService("country_detector")).detectCountry();
            if (countryDetectCountry != null) {
                return countryDetectCountry.getCountryIso();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String formatNumber(String str, Context context) {
        AsYouTypeFormatter asYouTypeFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(detectCountry(context));
        for (char c : str.toCharArray()) {
            str = asYouTypeFormatter.inputDigit(c);
        }
        return str;
    }

    public static boolean isLegalArgument(String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }
        StringWriter stringWriter = new StringWriter();
        XmlSerializer xmlSerializerNewSerializer = Xml.newSerializer();
        try {
            xmlSerializerNewSerializer.setOutput(stringWriter);
            xmlSerializerNewSerializer.startDocument(null, null);
            xmlSerializerNewSerializer.startTag("", "test");
            xmlSerializerNewSerializer.attribute("", "str", str);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "[islegalArgument] IOException: " + str);
            return false;
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, "[islegalArgument] IllegalArgumentException: " + str);
            return false;
        }
    }

    public static String removeInvalidChar(String str) {
        Log.d(TAG, "[removeInvalidChar] begin: " + str);
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if ((cCharAt >= ' ' && cCharAt <= 55295) || (cCharAt >= 57344 && cCharAt <= 65533)) {
                sb.append(cCharAt);
            }
        }
        String string = sb.toString();
        Log.d(TAG, "[removeInvalidChar] end: " + string);
        return string;
    }
}
