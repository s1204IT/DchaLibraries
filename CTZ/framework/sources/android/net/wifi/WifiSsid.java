package android.net.wifi;

import android.os.Parcel;
import android.os.Parcelable;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.util.Arrays;
import java.util.Locale;

public class WifiSsid implements Parcelable {
    public static final Parcelable.Creator<WifiSsid> CREATOR = new Parcelable.Creator<WifiSsid>() {
        @Override
        public WifiSsid createFromParcel(Parcel parcel) {
            WifiSsid wifiSsid = new WifiSsid();
            int i = parcel.readInt();
            byte[] bArr = new byte[i];
            parcel.readByteArray(bArr);
            wifiSsid.octets.write(bArr, 0, i);
            wifiSsid.mIsGbkEncoding = parcel.readInt() != 0;
            return wifiSsid;
        }

        @Override
        public WifiSsid[] newArray(int i) {
            return new WifiSsid[i];
        }
    };
    private static final int HEX_RADIX = 16;
    public static final String NONE = "<unknown ssid>";
    private static final String TAG = "WifiSsid";
    public boolean mIsGbkEncoding;
    public final ByteArrayOutputStream octets;

    private WifiSsid() {
        this.octets = new ByteArrayOutputStream(32);
        this.mIsGbkEncoding = false;
    }

    public static WifiSsid createFromByteArray(byte[] bArr) {
        WifiSsid wifiSsid = new WifiSsid();
        if (bArr != null) {
            wifiSsid.octets.write(bArr, 0, bArr.length);
        }
        return wifiSsid;
    }

    public static WifiSsid createFromAsciiEncoded(String str) {
        WifiSsid wifiSsid = new WifiSsid();
        wifiSsid.convertToBytes(str);
        return wifiSsid;
    }

    public static WifiSsid createFromHex(String str) {
        int i;
        WifiSsid wifiSsid = new WifiSsid();
        if (str == null) {
            return wifiSsid;
        }
        if (str.startsWith("0x") || str.startsWith("0X")) {
            str = str.substring(2);
        }
        int i2 = 0;
        while (i2 < str.length() - 1) {
            int i3 = i2 + 2;
            try {
                i = Integer.parseInt(str.substring(i2, i3), 16);
            } catch (NumberFormatException e) {
                i = 0;
            }
            wifiSsid.octets.write(i);
            i2 = i3;
        }
        return wifiSsid;
    }

    private void convertToBytes(String str) {
        int i;
        int i2 = 0;
        while (i2 < str.length()) {
            char cCharAt = str.charAt(i2);
            if (cCharAt == '\\') {
                i2++;
                char cCharAt2 = str.charAt(i2);
                if (cCharAt2 != '\"') {
                    if (cCharAt2 == '\\') {
                        this.octets.write(92);
                        i2++;
                    } else if (cCharAt2 == 'e') {
                        this.octets.write(27);
                        i2++;
                    } else if (cCharAt2 == 'n') {
                        this.octets.write(10);
                        i2++;
                    } else if (cCharAt2 == 'r') {
                        this.octets.write(13);
                        i2++;
                    } else if (cCharAt2 == 't') {
                        this.octets.write(9);
                        i2++;
                    } else if (cCharAt2 == 'x') {
                        i2++;
                        int i3 = i2 + 2;
                        try {
                            i = Integer.parseInt(str.substring(i2, i3), 16);
                        } catch (NumberFormatException e) {
                            i = -1;
                        }
                        if (i < 0) {
                            int iDigit = Character.digit(str.charAt(i2), 16);
                            if (iDigit >= 0) {
                                this.octets.write(iDigit);
                                i2++;
                            }
                        } else {
                            this.octets.write(i);
                            i2 = i3;
                        }
                    } else {
                        switch (cCharAt2) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                                int iCharAt = str.charAt(i2) - '0';
                                i2++;
                                if (str.charAt(i2) >= '0' && str.charAt(i2) <= '7') {
                                    iCharAt = ((iCharAt * 8) + str.charAt(i2)) - 48;
                                    i2++;
                                }
                                if (str.charAt(i2) >= '0' && str.charAt(i2) <= '7') {
                                    iCharAt = ((iCharAt * 8) + str.charAt(i2)) - 48;
                                    i2++;
                                }
                                this.octets.write(iCharAt);
                                break;
                        }
                    }
                } else {
                    this.octets.write(34);
                    i2++;
                }
            } else {
                this.octets.write(cCharAt);
                i2++;
            }
        }
    }

    public String toString() {
        byte[] byteArray = this.octets.toByteArray();
        if (this.octets.size() <= 0 || isArrayAllZeroes(byteArray)) {
            return "";
        }
        Charset charsetForName = Charset.forName("UTF-8");
        if (this.mIsGbkEncoding) {
            charsetForName = Charset.forName("GB2312");
        }
        CharsetDecoder charsetDecoderOnUnmappableCharacter = charsetForName.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
        CharBuffer charBufferAllocate = CharBuffer.allocate(32);
        CoderResult coderResultDecode = charsetDecoderOnUnmappableCharacter.decode(ByteBuffer.wrap(byteArray), charBufferAllocate, true);
        charBufferAllocate.flip();
        if (coderResultDecode.isError()) {
            return NONE;
        }
        return charBufferAllocate.toString();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WifiSsid)) {
            return false;
        }
        return Arrays.equals(this.octets.toByteArray(), ((WifiSsid) obj).octets.toByteArray());
    }

    public int hashCode() {
        return Arrays.hashCode(this.octets.toByteArray());
    }

    private boolean isArrayAllZeroes(byte[] bArr) {
        for (byte b : bArr) {
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
        byte[] octets = getOctets();
        String str = "0x";
        for (int i = 0; i < this.octets.size(); i++) {
            str = str + String.format(Locale.US, "%02x", Byte.valueOf(octets[i]));
        }
        if (this.octets.size() > 0) {
            return str;
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(this.octets.size());
        parcel.writeByteArray(this.octets.toByteArray());
        parcel.writeInt(this.mIsGbkEncoding ? 1 : 0);
    }
}
