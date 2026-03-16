package android.net.wifi;

import android.net.ProxyInfo;
import android.os.Parcel;
import android.os.Parcelable;
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
            WifiSsid ssid = new WifiSsid();
            int length = in.readInt();
            byte[] b = new byte[length];
            in.readByteArray(b);
            ssid.octets.write(b, 0, length);
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
    private static final int UTF8_2_BYTE_HEADER = 192;
    private static final int UTF8_2_BYTE_SIZE_MASK = 224;
    private static final int UTF8_3_BYTE_HEADER = 224;
    private static final int UTF8_3_BYTE_SIZE_MASK = 240;
    private static final int UTF8_SBU_HEADER = 128;
    private static final int UTF8_SBU_MASK = 192;
    public boolean NOT_UTF8;
    public final ByteArrayOutputStream octets;

    private WifiSsid() {
        this.octets = new ByteArrayOutputStream(32);
        this.NOT_UTF8 = false;
    }

    public static boolean is_utf8_encoded(byte[] encode_byte) {
        int i = 0;
        while (i < encode_byte.length) {
            if ((encode_byte[i] & 128) > 0) {
                if ((encode_byte[i] & 224) == 192) {
                    if (i + 1 >= encode_byte.length) {
                        break;
                    }
                    if ((encode_byte[i + 1] & 192) != 128) {
                        return false;
                    }
                    i += 2;
                } else {
                    if ((encode_byte[i] & 240) != 224) {
                        return false;
                    }
                    if (i + 1 >= encode_byte.length) {
                        break;
                    }
                    if ((encode_byte[i + 1] & 192) != 128 || (encode_byte[i + 2] & 192) != 128) {
                        return false;
                    }
                    i += 3;
                }
            } else {
                i++;
            }
        }
        return true;
    }

    public static WifiSsid createFromAsciiEncoded(String asciiEncoded) {
        WifiSsid a = new WifiSsid();
        a.convertToBytes(asciiEncoded);
        return a;
    }

    public static WifiSsid createFromHex(String hexStr) {
        int val;
        WifiSsid a = new WifiSsid();
        if (hexStr != null) {
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
        }
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
    }

    public String toString() {
        Charset charset;
        byte[] ssidBytes = this.octets.toByteArray();
        if (this.octets.size() <= 0 || isArrayAllZeroes(ssidBytes)) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (is_utf8_encoded(ssidBytes)) {
            charset = Charset.forName("UTF-8");
        } else {
            this.NOT_UTF8 = true;
            charset = Charset.forName("GBK");
        }
        CharsetDecoder decoder = charset.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer out = CharBuffer.allocate(32);
        CoderResult result = decoder.decode(ByteBuffer.wrap(ssidBytes), out, true);
        out.flip();
        if (result.isError()) {
            return NONE;
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
        return out;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.octets.size());
        dest.writeByteArray(this.octets.toByteArray());
    }
}
