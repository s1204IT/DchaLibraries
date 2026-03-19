package android.net.wifi;

import android.net.ProxyInfo;
import android.os.BatteryStats;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Locale;

public class WifiSsid implements Parcelable {
    public static final Parcelable.Creator<WifiSsid> CREATOR = new Parcelable.Creator<WifiSsid>() {
        @Override
        public WifiSsid createFromParcel(Parcel in) {
            WifiSsid ssid = new WifiSsid(null);
            int length = in.readInt();
            byte[] b = new byte[length];
            in.readByteArray(b);
            ssid.octets.write(b, 0, length);
            ssid.mIsGbkEncoding = in.readInt() != 0;
            return ssid;
        }

        @Override
        public WifiSsid[] newArray(int size) {
            return new WifiSsid[size];
        }
    };
    private static final int HEX_RADIX = 16;
    public static final String NONE = "<unknown ssid>";
    private static final String TAG = "WifiSsid";
    private boolean mIsGbkEncoding;
    public final ByteArrayOutputStream octets;

    WifiSsid(WifiSsid wifiSsid) {
        this();
    }

    private WifiSsid() {
        this.octets = new ByteArrayOutputStream(32);
        this.mIsGbkEncoding = false;
    }

    public static WifiSsid createFromAsciiEncoded(String asciiEncoded) {
        WifiSsid a = new WifiSsid();
        a.convertToBytes(asciiEncoded);
        return a;
    }

    public static WifiSsid createFromHex(String hexStr) {
        int val;
        WifiSsid a = new WifiSsid();
        if (hexStr == null) {
            return a;
        }
        if (hexStr.startsWith("0x") || hexStr.startsWith("0X")) {
            hexStr = hexStr.substring(2);
        }
        for (int i = 0; i < hexStr.length() - 1; i += 2) {
            try {
                val = Integer.parseInt(hexStr.substring(i, i + 2), 16);
            } catch (NumberFormatException e) {
                val = 0;
            }
            a.octets.write(val);
        }
        a.checkAndSetIsGbkEncoding();
        return a;
    }

    private void convertToBytes(String asciiEncoded) {
        int val;
        int i = 0;
        while (i < asciiEncoded.length()) {
            char c = asciiEncoded.charAt(i);
            switch (c) {
                case '\\':
                    i++;
                    switch (asciiEncoded.charAt(i)) {
                        case '\"':
                            this.octets.write(34);
                            i++;
                            break;
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                            int val2 = asciiEncoded.charAt(i) - '0';
                            i++;
                            if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                val2 = ((val2 * 8) + asciiEncoded.charAt(i)) - 48;
                                i++;
                            }
                            if (asciiEncoded.charAt(i) >= '0' && asciiEncoded.charAt(i) <= '7') {
                                val2 = ((val2 * 8) + asciiEncoded.charAt(i)) - 48;
                                i++;
                            }
                            this.octets.write(val2);
                            break;
                        case '\\':
                            this.octets.write(92);
                            i++;
                            break;
                        case 'e':
                            this.octets.write(27);
                            i++;
                            break;
                        case 'n':
                            this.octets.write(10);
                            i++;
                            break;
                        case 'r':
                            this.octets.write(13);
                            i++;
                            break;
                        case 't':
                            this.octets.write(9);
                            i++;
                            break;
                        case 'x':
                            i++;
                            try {
                                val = Integer.parseInt(asciiEncoded.substring(i, i + 2), 16);
                            } catch (NumberFormatException e) {
                                val = -1;
                            }
                            if (val < 0) {
                                int val3 = Character.digit(asciiEncoded.charAt(i), 16);
                                if (val3 >= 0) {
                                    this.octets.write(val3);
                                    i++;
                                }
                            } else {
                                this.octets.write(val);
                                i += 2;
                            }
                            break;
                    }
                    break;
                default:
                    this.octets.write(c);
                    i++;
                    break;
            }
        }
        checkAndSetIsGbkEncoding();
    }

    public String toString() {
        byte[] ssidBytes = this.octets.toByteArray();
        if (this.octets.size() <= 0 || isArrayAllZeroes(ssidBytes)) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        boolean DBG = SystemProperties.get("persist.wifi.gbk.debug").equals(WifiEnterpriseConfig.ENGINE_ENABLE);
        boolean ssidGbkEncoding = SystemProperties.get("persist.wifi.gbk.encoding").equals(WifiEnterpriseConfig.ENGINE_ENABLE);
        Charset charset = Charset.forName("UTF-8");
        if (ssidGbkEncoding || this.mIsGbkEncoding) {
            charset = Charset.forName("GB2312");
        }
        CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer out = CharBuffer.allocate(32);
        CoderResult result = decoder.decode(ByteBuffer.wrap(ssidBytes), out, true);
        out.flip();
        if (result.isError()) {
            return NONE;
        }
        if (DBG) {
            Log.d(TAG, "persist.wifi.gbk.encoding: " + ssidGbkEncoding + ", isGbk: " + this.mIsGbkEncoding + ", toString: " + out.toString());
        }
        return out.toString();
    }

    private boolean isArrayAllZeroes(byte[] ssidBytes) {
        for (byte b : ssidBytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isHidden() {
        return isArrayAllZeroes(this.octets.toByteArray());
    }

    public byte[] getOctets() {
        return this.octets.toByteArray();
    }

    public String getHexString() {
        String out = "0x";
        byte[] ssidbytes = getOctets();
        for (int i = 0; i < this.octets.size(); i++) {
            out = out + String.format(Locale.US, "%02x", Byte.valueOf(ssidbytes[i]));
        }
        if (this.octets.size() > 0) {
            return out;
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.octets.size());
        dest.writeByteArray(this.octets.toByteArray());
        dest.writeInt(this.mIsGbkEncoding ? 1 : 0);
    }

    private static boolean isGBK(byte[] byteArray, int ssidStartPos, int ssidEndPos) {
        boolean DBG = SystemProperties.get("persist.wifi.gbk.debug").equals(WifiEnterpriseConfig.ENGINE_ENABLE);
        if (isNotUtf8(byteArray, ssidStartPos, ssidEndPos)) {
            if (DBG) {
                Log.d(TAG, "is not utf8");
                return true;
            }
            return true;
        }
        if (DBG) {
            Log.d(TAG, "is utf8 format");
            return false;
        }
        return false;
    }

    private static boolean isNotUtf8(byte[] input, int ssidStartPos, int ssidEndPos) {
        int nBytes = 0;
        int lastWildcar = 0;
        boolean isAllAscii = true;
        boolean isAllGBK = true;
        boolean isWildcardChar = false;
        for (int i = ssidStartPos; i < ssidEndPos && i < input.length; i++) {
            byte chr = input[i];
            if (!isASCII(chr)) {
                isAllAscii = false;
                isWildcardChar = !isWildcardChar;
                if (isWildcardChar && i < input.length - 1) {
                    byte chr1 = input[i + 1];
                    if (!isGBKChar(chr, chr1)) {
                        isAllGBK = false;
                    }
                }
            } else {
                isWildcardChar = false;
            }
            if (nBytes == 0) {
                if ((chr & BatteryStats.HistoryItem.CMD_NULL) >= 128) {
                    lastWildcar = i;
                    int nBytes2 = getUtf8CharLen(chr);
                    if (nBytes2 == 0) {
                        return true;
                    }
                    nBytes = nBytes2 - 1;
                } else {
                    continue;
                }
            } else {
                if ((chr & 192) != 128) {
                    break;
                }
                nBytes--;
            }
        }
        if (nBytes <= 0 || isAllAscii) {
            return false;
        }
        if (isAllGBK) {
            return true;
        }
        int nBytes3 = getUtf8CharLen(input[lastWildcar]);
        for (int j = lastWildcar; j < lastWildcar + nBytes3 && j < input.length; j++) {
            if (!isASCII(input[j])) {
                input[j] = 32;
            }
        }
        return false;
    }

    private static int getUtf8CharLen(byte firstByte) {
        if (firstByte >= -4 && firstByte <= -3) {
            return 6;
        }
        if (firstByte >= -8) {
            return 5;
        }
        if (firstByte >= -16) {
            return 4;
        }
        if (firstByte >= -32) {
            return 3;
        }
        if (firstByte >= -64) {
            return 2;
        }
        return 0;
    }

    private static boolean isASCII(byte b) {
        return (b & 128) == 0;
    }

    private static boolean isGBKChar(byte head, byte tail) {
        int b0 = head & 255;
        int b1 = tail & 255;
        if (b0 < 161 || b0 > 169 || b1 < 161 || b1 > 254) {
            if (b0 < 176 || b0 > 247 || b1 < 161 || b1 > 254) {
                if (b0 < 129 || b0 > 160 || b1 < 64 || b1 > 254) {
                    if (b0 < 170 || b0 > 254 || b1 < 64 || b1 > 160 || b1 == 127) {
                        if (b0 < 168 || b0 > 169 || b1 < 64 || b1 > 160 || b1 == 127) {
                            if (b0 < 170 || b0 > 175 || b1 < 161 || b1 > 254 || b1 == 127) {
                                if (b0 < 248 || b0 > 254 || b1 < 161 || b1 > 254) {
                                    if (b0 >= 161 && b0 <= 167 && b1 >= 64 && b1 <= 160 && b1 != 127) {
                                        return true;
                                    }
                                    return false;
                                }
                                return true;
                            }
                            return true;
                        }
                        return true;
                    }
                    return true;
                }
                return true;
            }
            return true;
        }
        return true;
    }

    private void checkAndSetIsGbkEncoding() {
        byte[] ssidBytes = this.octets.toByteArray();
        this.mIsGbkEncoding = isGBK(ssidBytes, 0, ssidBytes.length);
    }

    public boolean isGBK() {
        return this.mIsGbkEncoding;
    }
}
