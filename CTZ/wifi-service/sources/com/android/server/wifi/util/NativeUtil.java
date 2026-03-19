package com.android.server.wifi.util;

import android.text.TextUtils;
import com.android.server.wifi.ByteBufferReader;
import com.mediatek.server.wifi.MtkGbkSsid;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import libcore.util.HexEncoding;

public class NativeUtil {
    public static final byte[] ANY_MAC_BYTES = {0, 0, 0, 0, 0, 0};
    private static final String ANY_MAC_STR = "any";
    private static final int MAC_LENGTH = 6;
    private static final int MAC_OUI_LENGTH = 3;
    private static final int MAC_STR_LENGTH = 17;

    public static ArrayList<Byte> stringToByteArrayList(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        if (MtkGbkSsid.isGbkSsid(str)) {
            return MtkGbkSsid.stringToByteArrayList(str);
        }
        try {
            ByteBuffer byteBufferEncode = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(str));
            byte[] bArr = new byte[byteBufferEncode.remaining()];
            byteBufferEncode.get(bArr);
            return byteArrayToArrayList(bArr);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("cannot be utf-8 encoded", e);
        }
    }

    public static String stringFromByteArrayList(ArrayList<Byte> arrayList) {
        if (arrayList == null) {
            throw new IllegalArgumentException("null byte array list");
        }
        byte[] bArr = new byte[arrayList.size()];
        int i = 0;
        Iterator<Byte> it = arrayList.iterator();
        while (it.hasNext()) {
            bArr[i] = it.next().byteValue();
            i++;
        }
        return new String(bArr, StandardCharsets.UTF_8);
    }

    public static byte[] stringToByteArray(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String stringFromByteArray(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("null byte array");
        }
        return new String(bArr);
    }

    public static byte[] macAddressToByteArray(String str) {
        if (TextUtils.isEmpty(str) || "any".equals(str)) {
            return ANY_MAC_BYTES;
        }
        String strReplace = str.replace(":", "");
        if (strReplace.length() != 12) {
            throw new IllegalArgumentException("invalid mac string length: " + strReplace);
        }
        return HexEncoding.decode(strReplace.toCharArray(), false);
    }

    public static String macAddressFromByteArray(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("null mac bytes");
        }
        if (bArr.length != 6) {
            throw new IllegalArgumentException("invalid macArray length: " + bArr.length);
        }
        StringBuilder sb = new StringBuilder(17);
        for (int i = 0; i < bArr.length; i++) {
            if (i != 0) {
                sb.append(":");
            }
            sb.append(new String(HexEncoding.encode(bArr, i, 1)));
        }
        return sb.toString().toLowerCase();
    }

    public static byte[] macAddressOuiToByteArray(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null mac string");
        }
        String strReplace = str.replace(":", "");
        if (strReplace.length() != 6) {
            throw new IllegalArgumentException("invalid mac oui string length: " + strReplace);
        }
        return HexEncoding.decode(strReplace.toCharArray(), false);
    }

    public static Long macAddressToLong(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("null mac bytes");
        }
        if (bArr.length != 6) {
            throw new IllegalArgumentException("invalid macArray length: " + bArr.length);
        }
        try {
            return Long.valueOf(ByteBufferReader.readInteger(ByteBuffer.wrap(bArr), ByteOrder.BIG_ENDIAN, bArr.length));
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            throw new IllegalArgumentException("invalid macArray");
        }
    }

    public static String removeEnclosingQuotes(String str) {
        int length = str.length();
        if (length >= 2 && str.charAt(0) == '\"') {
            int i = length - 1;
            if (str.charAt(i) == '\"') {
                return str.substring(1, i);
            }
        }
        return str;
    }

    public static String addEnclosingQuotes(String str) {
        return "\"" + str + "\"";
    }

    public static ArrayList<Byte> hexOrQuotedStringToBytes(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null string");
        }
        int length = str.length();
        if (length > 1 && str.charAt(0) == '\"' && str.charAt(length - 1) == '\"') {
            return stringToByteArrayList(str.substring(1, str.length() - 1));
        }
        return byteArrayToArrayList(hexStringToByteArray(str));
    }

    public static String bytesToHexOrQuotedString(ArrayList<Byte> arrayList) {
        if (arrayList == null) {
            throw new IllegalArgumentException("null ssid bytes");
        }
        byte[] bArrByteArrayFromArrayList = byteArrayFromArrayList(arrayList);
        if (!arrayList.contains((byte) 0)) {
            try {
                return "\"" + StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bArrByteArrayFromArrayList)).toString() + "\"";
            } catch (CharacterCodingException e) {
            }
        }
        return hexStringFromByteArray(bArrByteArrayFromArrayList);
    }

    public static ArrayList<Byte> decodeSsid(String str) {
        return hexOrQuotedStringToBytes(str);
    }

    public static String encodeSsid(ArrayList<Byte> arrayList) {
        return bytesToHexOrQuotedString(arrayList);
    }

    public static ArrayList<Byte> byteArrayToArrayList(byte[] bArr) {
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    public static byte[] byteArrayFromArrayList(ArrayList<Byte> arrayList) {
        byte[] bArr = new byte[arrayList.size()];
        Iterator<Byte> it = arrayList.iterator();
        int i = 0;
        while (it.hasNext()) {
            bArr[i] = it.next().byteValue();
            i++;
        }
        return bArr;
    }

    public static byte[] hexStringToByteArray(String str) {
        if (str == null) {
            throw new IllegalArgumentException("null hex string");
        }
        return HexEncoding.decode(str.toCharArray(), false);
    }

    public static String hexStringFromByteArray(byte[] bArr) {
        if (bArr == null) {
            throw new IllegalArgumentException("null hex bytes");
        }
        return new String(HexEncoding.encode(bArr)).toLowerCase();
    }

    public static String wpsDevTypeStringFromByteArray(byte[] bArr) {
        return String.format("%d-%s-%d", Integer.valueOf(((bArr[0] & 255) << 8) | (bArr[1] & 255)), new String(HexEncoding.encode(Arrays.copyOfRange(bArr, 2, 6))), Integer.valueOf((bArr[7] & 255) | ((bArr[6] & 255) << 8)));
    }
}
