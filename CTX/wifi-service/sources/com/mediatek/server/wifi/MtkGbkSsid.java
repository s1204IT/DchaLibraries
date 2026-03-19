package com.mediatek.server.wifi;

import android.net.wifi.WifiSsid;
import android.util.Log;
import com.android.server.wifi.util.NativeUtil;
import com.android.server.wifi.wificond.HiddenNetwork;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MtkGbkSsid {
    private static final String TAG = "MtkGbkSsid";
    private static ArrayList<String> mList = new ArrayList<>();

    private MtkGbkSsid() {
    }

    public static void clear() {
        synchronized (mList) {
            mList.clear();
        }
    }

    public static void checkAndSetGbk(WifiSsid wifiSsid) {
        byte[] byteArray = wifiSsid.octets.toByteArray();
        if (isNotUtf8(byteArray, 0, byteArray.length)) {
            Log.d(TAG, "SSID " + wifiSsid.toString() + " is GBK encoding");
            wifiSsid.mIsGbkEncoding = true;
            String string = wifiSsid.toString();
            synchronized (mList) {
                if (!mList.contains(string)) {
                    mList.add(string);
                }
            }
        }
    }

    public static ArrayList<Byte> stringToByteArrayList(String str) {
        ArrayList<Byte> arrayList = new ArrayList<>();
        try {
            for (byte b : str.getBytes("GBK")) {
                arrayList.add(new Byte(b));
            }
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "UnsupportedEncodingException: " + e.toString());
        }
        return arrayList;
    }

    public static byte[] stringToByteArray(String str) {
        try {
            return str.getBytes("GBK");
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "UnsupportedEncodingException: " + e.toString());
            return null;
        }
    }

    public static boolean isGbkSsid(String str) {
        boolean zContains;
        synchronized (mList) {
            zContains = mList.contains(NativeUtil.removeEnclosingQuotes(str));
        }
        return zContains;
    }

    public static HiddenNetwork needAddExtraGbkSsid(String str) {
        String strRemoveEnclosingQuotes = NativeUtil.removeEnclosingQuotes(str);
        if (isAllASCII(NativeUtil.decodeSsid(str)) || isGbkSsid(strRemoveEnclosingQuotes)) {
            return null;
        }
        HiddenNetwork hiddenNetwork = new HiddenNetwork();
        try {
            hiddenNetwork.ssid = NativeUtil.byteArrayFromArrayList(stringToByteArrayList(strRemoveEnclosingQuotes));
            return hiddenNetwork;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument " + strRemoveEnclosingQuotes, e);
            return null;
        }
    }

    private static boolean isAllASCII(ArrayList<Byte> arrayList) {
        if (arrayList == null) {
            return false;
        }
        for (int i = 0; i < arrayList.size(); i++) {
            if (!isASCII(arrayList.get(i).byteValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNotUtf8(byte[] bArr, int i, int i2) {
        boolean z = true;
        boolean z2 = true;
        boolean z3 = false;
        int i3 = 0;
        int i4 = 0;
        while (i < i2 && i < bArr.length) {
            byte b = bArr[i];
            if (!isASCII(b)) {
                z3 = !z3;
                if (z3 && i < bArr.length - 1 && !isGBKChar(b, bArr[i + 1])) {
                    z2 = false;
                }
                z = false;
            } else {
                z3 = false;
            }
            if (i3 == 0) {
                if ((b & 255) < 128) {
                    continue;
                } else {
                    int utf8CharLen = getUtf8CharLen(b);
                    if (utf8CharLen == 0) {
                        return true;
                    }
                    i3 = utf8CharLen - 1;
                    i4 = i;
                }
            } else {
                if ((b & 192) != 128) {
                    break;
                }
                i3--;
            }
            i++;
        }
        if (i3 <= 0 || z) {
            return false;
        }
        if (z2) {
            return true;
        }
        int utf8CharLen2 = getUtf8CharLen(bArr[i4]);
        for (int i5 = i4; i5 < i4 + utf8CharLen2 && i5 < bArr.length; i5++) {
            if (!isASCII(bArr[i5])) {
                bArr[i5] = 32;
            }
        }
        return false;
    }

    private static int getUtf8CharLen(byte b) {
        if (b >= -4 && b <= -3) {
            return 6;
        }
        if (b >= -8) {
            return 5;
        }
        if (b >= -16) {
            return 4;
        }
        if (b >= -32) {
            return 3;
        }
        if (b >= -64) {
            return 2;
        }
        return 0;
    }

    private static boolean isASCII(byte b) {
        if ((b & 128) == 0) {
            return true;
        }
        return false;
    }

    private static boolean isGBKChar(byte b, byte b2) {
        int i = b & 255;
        int i2 = b2 & 255;
        if (i >= 161 && i <= 169 && i2 >= 161 && i2 <= 254) {
            return true;
        }
        if (i >= 176 && i <= 247 && i2 >= 161 && i2 <= 254) {
            return true;
        }
        if (i >= 129 && i <= 160 && i2 >= 64 && i2 <= 254) {
            return true;
        }
        if (i < 170 || i > 254 || i2 < 64 || i2 > 160 || i2 == 127) {
            if (i >= 168 && i <= 169 && i2 >= 64 && i2 <= 160 && i2 != 127) {
                return true;
            }
            if (i >= 170 && i <= 175 && i2 >= 161 && i2 <= 254 && i2 != 127) {
                return true;
            }
            if (i >= 248 && i <= 254 && i2 >= 161 && i2 <= 254) {
                return true;
            }
            if (i >= 161 && i <= 167 && i2 >= 64 && i2 <= 160 && i2 != 127) {
                return true;
            }
            return false;
        }
        return true;
    }
}
