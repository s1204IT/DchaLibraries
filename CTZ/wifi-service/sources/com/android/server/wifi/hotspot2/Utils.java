package com.android.server.wifi.hotspot2;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public abstract class Utils {
    private static final int EUI48Length = 6;
    private static final long EUI48Mask = 281474976710655L;
    private static final int EUI64Length = 8;
    private static final String[] PLMNText = {"org", "3gppnetwork", "mcc*", "mnc*", "wlan"};
    public static final long UNSET_TIME = -1;

    public static String hs2LogTag(Class cls) {
        return "HS20";
    }

    public static List<String> splitDomain(String str) {
        if (str.endsWith(".")) {
            str = str.substring(0, str.length() - 1);
        }
        int iIndexOf = str.indexOf(64);
        if (iIndexOf >= 0) {
            str = str.substring(iIndexOf + 1);
        }
        String[] strArrSplit = str.toLowerCase().split("\\.");
        LinkedList linkedList = new LinkedList();
        for (String str2 : strArrSplit) {
            linkedList.addFirst(str2);
        }
        return linkedList;
    }

    public static long parseMac(String str) {
        if (str == null) {
            throw new IllegalArgumentException("Null MAC adddress");
        }
        long j = 0;
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2++) {
            int iFromHex = fromHex(str.charAt(i2), true);
            if (iFromHex >= 0) {
                j = (j << 4) | ((long) iFromHex);
                i++;
            }
        }
        if (i < 12 || (i & 1) == 1) {
            throw new IllegalArgumentException("Bad MAC address: '" + str + "'");
        }
        return j;
    }

    public static String macToString(long j) {
        int i;
        if (((-281474976710656L) & j) == 0) {
            i = 6;
        } else {
            i = 8;
        }
        StringBuilder sb = new StringBuilder();
        boolean z = true;
        for (int i2 = (i - 1) * 8; i2 >= 0; i2 -= 8) {
            if (!z) {
                sb.append(':');
            } else {
                z = false;
            }
            sb.append(String.format("%02x", Long.valueOf((j >>> i2) & 255)));
        }
        return sb.toString();
    }

    public static String getMccMnc(List<String> list) {
        if (list.size() != PLMNText.length) {
            return null;
        }
        for (int i = 0; i < PLMNText.length; i++) {
            String str = PLMNText[i];
            if (!list.get(i).regionMatches(0, str, 0, str.endsWith("*") ? str.length() - 1 : str.length())) {
                return null;
            }
        }
        String str2 = list.get(2).substring(3) + list.get(3).substring(3);
        for (int i2 = 0; i2 < str2.length(); i2++) {
            char cCharAt = str2.charAt(i2);
            if (cCharAt < '0' || cCharAt > '9') {
                return null;
            }
        }
        return str2;
    }

    public static String roamingConsortiumsToString(long[] jArr) {
        if (jArr == null) {
            return "null";
        }
        ArrayList arrayList = new ArrayList(jArr.length);
        for (long j : jArr) {
            arrayList.add(Long.valueOf(j));
        }
        return roamingConsortiumsToString(arrayList);
    }

    public static String roamingConsortiumsToString(Collection<Long> collection) {
        StringBuilder sb = new StringBuilder();
        Iterator<Long> it = collection.iterator();
        boolean z = true;
        while (it.hasNext()) {
            long jLongValue = it.next().longValue();
            if (!z) {
                sb.append(", ");
            } else {
                z = false;
            }
            if (Long.numberOfLeadingZeros(jLongValue) > 40) {
                sb.append(String.format("%06x", Long.valueOf(jLongValue)));
            } else {
                sb.append(String.format("%010x", Long.valueOf(jLongValue)));
            }
        }
        return sb.toString();
    }

    public static String toUnicodeEscapedString(String str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt >= ' ' && cCharAt < 127) {
                sb.append(cCharAt);
            } else {
                sb.append("\\u");
                sb.append(String.format("%04x", Integer.valueOf(cCharAt)));
            }
        }
        return sb.toString();
    }

    public static String toHexString(byte[] bArr) {
        if (bArr == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(bArr.length * 3);
        boolean z = true;
        for (byte b : bArr) {
            if (!z) {
                sb.append(' ');
            } else {
                z = false;
            }
            sb.append(String.format("%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    public static String toHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder(bArr.length * 2);
        for (byte b : bArr) {
            sb.append(String.format("%02x", Integer.valueOf(b & 255)));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String str) {
        if ((str.length() & 1) == 1) {
            throw new NumberFormatException("Odd length hex string: " + str.length());
        }
        byte[] bArr = new byte[str.length() >> 1];
        int i = 0;
        for (int i2 = 0; i2 < str.length(); i2 += 2) {
            bArr[i] = (byte) (((fromHex(str.charAt(i2), false) & 15) << 4) | (fromHex(str.charAt(i2 + 1), false) & 15));
            i++;
        }
        return bArr;
    }

    public static int fromHex(char c, boolean z) throws NumberFormatException {
        if (c <= '9' && c >= '0') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return (c + '\n') - 97;
        }
        if (c <= 'F' && c >= 'A') {
            return (c + '\n') - 65;
        }
        if (z) {
            return -1;
        }
        throw new NumberFormatException("Bad hex-character: " + c);
    }

    private static char toAscii(int i) {
        if (i < 32 || i >= 127) {
            return '.';
        }
        return (char) i;
    }

    static boolean isDecimal(String str) {
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return false;
            }
        }
        return true;
    }

    public static <T extends Comparable> int compare(Comparable<T> comparable, T t) {
        if (comparable == null) {
            return t == null ? 0 : -1;
        }
        if (t == null) {
            return 1;
        }
        return comparable.compareTo(t);
    }

    public static String bytesToBingoCard(ByteBuffer byteBuffer, int i) {
        ByteBuffer byteBufferDuplicate = byteBuffer.duplicate();
        byteBufferDuplicate.limit(byteBufferDuplicate.position() + i);
        return bytesToBingoCard(byteBufferDuplicate);
    }

    public static String bytesToBingoCard(ByteBuffer byteBuffer) {
        ByteBuffer byteBufferDuplicate = byteBuffer.duplicate();
        StringBuilder sb = new StringBuilder();
        while (byteBufferDuplicate.hasRemaining()) {
            sb.append(String.format("%02x ", Integer.valueOf(byteBufferDuplicate.get() & 255)));
        }
        ByteBuffer byteBufferDuplicate2 = byteBuffer.duplicate();
        sb.append(' ');
        while (byteBufferDuplicate2.hasRemaining()) {
            sb.append(String.format("%c", Character.valueOf(toAscii(byteBufferDuplicate2.get() & 255))));
        }
        return sb.toString();
    }

    public static String toHMS(long j) {
        if (j < 0) {
            j = -j;
        }
        long j2 = j / 1000;
        long j3 = j - (1000 * j2);
        long j4 = j2 / 60;
        long j5 = j2 - (j4 * 60);
        long j6 = j4 / 60;
        long j7 = j4 - (60 * j6);
        Object[] objArr = new Object[5];
        objArr[0] = j < 0 ? "-" : "";
        objArr[1] = Long.valueOf(j6);
        objArr[2] = Long.valueOf(j7);
        objArr[3] = Long.valueOf(j5);
        objArr[4] = Long.valueOf(j3);
        return String.format("%s%d:%02d:%02d.%03d", objArr);
    }

    public static String toUTCString(long j) {
        if (j < 0) {
            return "unset";
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(j);
        return String.format("%4d/%02d/%02d %2d:%02d:%02dZ", Integer.valueOf(calendar.get(1)), Integer.valueOf(calendar.get(2) + 1), Integer.valueOf(calendar.get(5)), Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)), Integer.valueOf(calendar.get(13)));
    }

    public static String unquote(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() > 1 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}
