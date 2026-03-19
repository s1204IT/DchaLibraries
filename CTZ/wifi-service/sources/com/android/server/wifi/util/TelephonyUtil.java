package com.android.server.wifi.util;

import android.net.wifi.WifiConfiguration;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.WifiNative;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.mediatek.server.wifi.MtkEapSimUtility;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.HashMap;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class TelephonyUtil {
    public static final String DEFAULT_EAP_PREFIX = "\u0000";
    private static final HashMap<Integer, String> EAP_METHOD_PREFIX = new HashMap<>();
    private static final String IMSI_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPwithSHA-256andMGF1Padding";
    public static final String TAG = "TelephonyUtil";
    private static final String THREE_GPP_NAI_REALM_FORMAT = "wlan.mnc%s.mcc%s.3gppnetwork.org";

    static {
        EAP_METHOD_PREFIX.put(5, "0");
        EAP_METHOD_PREFIX.put(4, "1");
        EAP_METHOD_PREFIX.put(6, "6");
    }

    public static Pair<String, String> getSimIdentity(TelephonyManager telephonyManager, TelephonyUtil telephonyUtil, WifiConfiguration wifiConfiguration) {
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        String subscriberId = telephonyManager.getSubscriberId();
        String simOperator = "";
        if (telephonyManager.getSimState() == 5) {
            simOperator = telephonyManager.getSimOperator();
        }
        try {
            ImsiEncryptionInfo carrierInfoForImsiEncryption = telephonyManager.getCarrierInfoForImsiEncryption(2);
            String strBuildIdentity = buildIdentity(getSimMethodForConfig(wifiConfiguration), subscriberId, simOperator, false);
            if (strBuildIdentity == null) {
                Log.e(TAG, "Failed to build the identity");
                return null;
            }
            String strBuildEncryptedIdentity = buildEncryptedIdentity(telephonyUtil, getSimMethodForConfig(wifiConfiguration), subscriberId, simOperator, carrierInfoForImsiEncryption);
            if (strBuildEncryptedIdentity == null) {
                strBuildEncryptedIdentity = "";
            }
            return Pair.create(strBuildIdentity, strBuildEncryptedIdentity);
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get imsi encryption info: " + e.getMessage());
            return null;
        }
    }

    @VisibleForTesting
    public String encryptDataUsingPublicKey(PublicKey publicKey, byte[] bArr) {
        try {
            Cipher cipher = Cipher.getInstance(IMSI_CIPHER_TRANSFORMATION);
            cipher.init(1, publicKey);
            byte[] bArrDoFinal = cipher.doFinal(bArr);
            return Base64.encodeToString(bArrDoFinal, 0, bArrDoFinal.length, 0);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage());
            return null;
        }
    }

    private static String buildEncryptedIdentity(TelephonyUtil telephonyUtil, int i, String str, String str2, ImsiEncryptionInfo imsiEncryptionInfo) {
        String str3;
        if (imsiEncryptionInfo == null || (str3 = EAP_METHOD_PREFIX.get(Integer.valueOf(i))) == null) {
            return null;
        }
        String strEncryptDataUsingPublicKey = telephonyUtil.encryptDataUsingPublicKey(imsiEncryptionInfo.getPublicKey(), (str3 + str).getBytes());
        if (strEncryptDataUsingPublicKey == null) {
            Log.e(TAG, "Failed to encrypt IMSI");
            return null;
        }
        String strBuildIdentity = buildIdentity(i, strEncryptDataUsingPublicKey, str2, true);
        if (imsiEncryptionInfo.getKeyIdentifier() != null) {
            return strBuildIdentity + "," + imsiEncryptionInfo.getKeyIdentifier();
        }
        return strBuildIdentity;
    }

    private static String buildIdentity(int i, String str, String str2, boolean z) {
        String strSubstring;
        String strSubstring2;
        if (str == null || str.isEmpty()) {
            Log.e(TAG, "No IMSI or IMSI is null");
            return null;
        }
        String str3 = z ? DEFAULT_EAP_PREFIX : EAP_METHOD_PREFIX.get(Integer.valueOf(i));
        if (str3 == null) {
            return null;
        }
        if (str2 != null && !str2.isEmpty()) {
            strSubstring = str2.substring(0, 3);
            strSubstring2 = str2.substring(3);
            if (strSubstring2.length() == 2) {
                strSubstring2 = "0" + strSubstring2;
            }
        } else {
            strSubstring = str.substring(0, 3);
            strSubstring2 = str.substring(3, 6);
        }
        return str3 + str + "@" + String.format(THREE_GPP_NAI_REALM_FORMAT, strSubstring2, strSubstring);
    }

    private static int getSimMethodForConfig(WifiConfiguration wifiConfiguration) {
        int i;
        if (wifiConfiguration == null || wifiConfiguration.enterpriseConfig == null) {
            return -1;
        }
        int eapMethod = wifiConfiguration.enterpriseConfig.getEapMethod();
        if (eapMethod == 0) {
            switch (wifiConfiguration.enterpriseConfig.getPhase2Method()) {
                case 5:
                    i = 4;
                    break;
                case 6:
                    i = 5;
                    break;
                case 7:
                    i = 6;
                    break;
                default:
                    i = eapMethod;
                    break;
            }
        }
        if (isSimEapMethod(i)) {
            return i;
        }
        return -1;
    }

    public static boolean isSimConfig(WifiConfiguration wifiConfiguration) {
        return getSimMethodForConfig(wifiConfiguration) != -1;
    }

    public static boolean isSimEapMethod(int i) {
        return i == 4 || i == 5 || i == 6;
    }

    private static int parseHex(char c) {
        if ('0' <= c && c <= '9') {
            return c - '0';
        }
        if ('a' <= c && c <= 'f') {
            return (c - 'a') + 10;
        }
        if ('A' <= c && c <= 'F') {
            return (c - 'A') + 10;
        }
        throw new NumberFormatException("" + c + " is not a valid hex digit");
    }

    private static byte[] parseHex(String str) {
        int i = 0;
        if (str == null) {
            return new byte[0];
        }
        if (str.length() % 2 != 0) {
            throw new NumberFormatException(str + " is not a valid hex string");
        }
        byte[] bArr = new byte[(str.length() / 2) + 1];
        bArr[0] = (byte) (str.length() / 2);
        int i2 = 1;
        while (i < str.length()) {
            bArr[i2] = (byte) (((parseHex(str.charAt(i)) * 16) + parseHex(str.charAt(i + 1))) & Constants.BYTE_MASK);
            i += 2;
            i2++;
        }
        return bArr;
    }

    private static String makeHex(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bArr) {
            sb.append(String.format("%02x", Byte.valueOf(b)));
        }
        return sb.toString();
    }

    private static String makeHex(byte[] bArr, int i, int i2) {
        StringBuilder sb = new StringBuilder();
        for (int i3 = 0; i3 < i2; i3++) {
            sb.append(String.format("%02x", Byte.valueOf(bArr[i + i3])));
        }
        return sb.toString();
    }

    private static byte[] concatHex(byte[] bArr, byte[] bArr2) {
        int i;
        byte[] bArr3 = new byte[bArr.length + bArr2.length];
        if (bArr.length != 0) {
            i = 0;
            for (byte b : bArr) {
                bArr3[i] = b;
                i++;
            }
        } else {
            i = 0;
        }
        if (bArr2.length != 0) {
            for (byte b2 : bArr2) {
                bArr3[i] = b2;
                i++;
            }
        }
        return bArr3;
    }

    public static String getGsmSimAuthResponse(String[] strArr, TelephonyManager telephonyManager) {
        char c;
        if (telephonyManager == null) {
            Log.e(TAG, "No valid TelephonyManager");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String str : strArr) {
            if (str != null && !str.isEmpty()) {
                Log.d(TAG, "RAND = " + str);
                try {
                    String strEncodeToString = Base64.encodeToString(parseHex(str), 2);
                    String iccAuthentication = MtkEapSimUtility.getIccAuthentication(2, 128, strEncodeToString);
                    if (iccAuthentication != null) {
                        c = 2;
                    } else {
                        iccAuthentication = MtkEapSimUtility.getIccAuthentication(1, 128, strEncodeToString);
                        c = 1;
                    }
                    Log.v(TAG, "Raw Response - " + iccAuthentication);
                    if (iccAuthentication == null || iccAuthentication.length() <= 4) {
                        Log.e(TAG, "bad response - " + iccAuthentication);
                        return null;
                    }
                    byte[] bArrDecode = Base64.decode(iccAuthentication, 0);
                    Log.v(TAG, "Hex Response -" + makeHex(bArrDecode));
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("appType is  ");
                    sb2.append(c == 2 ? "USIM" : "SIM");
                    Log.v(TAG, sb2.toString());
                    if (c == 1) {
                        if (bArrDecode.length < 12) {
                            Log.e(TAG, "malfomed response - " + iccAuthentication);
                            return null;
                        }
                        String strMakeHex = makeHex(bArrDecode, 0, 4);
                        String strMakeHex2 = makeHex(bArrDecode, 4, 8);
                        sb.append(":" + strMakeHex2 + ":" + strMakeHex);
                        Log.v(TAG, "kc:" + strMakeHex2 + " sres:" + strMakeHex);
                    } else {
                        byte b = bArrDecode[0];
                        if (b >= bArrDecode.length) {
                            Log.e(TAG, "malfomed response - " + iccAuthentication);
                            return null;
                        }
                        String strMakeHex3 = makeHex(bArrDecode, 1, b);
                        int i = b + 1;
                        if (i >= bArrDecode.length) {
                            Log.e(TAG, "malfomed response - " + iccAuthentication);
                            return null;
                        }
                        byte b2 = bArrDecode[i];
                        if (i + b2 > bArrDecode.length) {
                            Log.e(TAG, "malfomed response - " + iccAuthentication);
                            return null;
                        }
                        String strMakeHex4 = makeHex(bArrDecode, 1 + i, b2);
                        sb.append(":" + strMakeHex4 + ":" + strMakeHex3);
                        Log.v(TAG, "kc:" + strMakeHex4 + " sres:" + strMakeHex3);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "malformed challenge");
                }
            }
        }
        return sb.toString();
    }

    public static class SimAuthRequestData {
        public String[] data;
        public int networkId;
        public int protocol;
        public String ssid;

        public SimAuthRequestData() {
        }

        public SimAuthRequestData(int i, int i2, String str, String[] strArr) {
            this.networkId = i;
            this.protocol = i2;
            this.ssid = str;
            this.data = strArr;
        }
    }

    public static class SimAuthResponseData {
        public String response;
        public String type;

        public SimAuthResponseData(String str, String str2) {
            this.type = str;
            this.response = str2;
        }
    }

    public static SimAuthResponseData get3GAuthResponse(SimAuthRequestData simAuthRequestData, TelephonyManager telephonyManager) {
        byte[] hex;
        byte[] hex2;
        StringBuilder sb = new StringBuilder();
        String str = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTH;
        boolean z = false;
        if (simAuthRequestData.data.length == 2) {
            try {
                hex = parseHex(simAuthRequestData.data[0]);
            } catch (NumberFormatException e) {
                hex = null;
            }
            try {
                hex2 = parseHex(simAuthRequestData.data[1]);
            } catch (NumberFormatException e2) {
                Log.e(TAG, "malformed challenge");
                hex2 = null;
            }
        } else {
            Log.e(TAG, "malformed challenge");
            hex2 = null;
            hex = null;
        }
        String iccAuthentication = "";
        if (hex != null && hex2 != null) {
            String strEncodeToString = Base64.encodeToString(concatHex(hex, hex2), 2);
            if (telephonyManager != null) {
                iccAuthentication = MtkEapSimUtility.getIccAuthentication(2, 129, strEncodeToString);
                Log.v(TAG, "Raw Response - " + iccAuthentication);
            } else {
                Log.e(TAG, "No valid TelephonyManager");
            }
        }
        if (iccAuthentication != null && iccAuthentication.length() > 4) {
            byte[] bArrDecode = Base64.decode(iccAuthentication, 0);
            Log.e(TAG, "Hex Response - " + makeHex(bArrDecode));
            byte b = bArrDecode[0];
            if (b == -37) {
                Log.v(TAG, "successful 3G authentication ");
                byte b2 = bArrDecode[1];
                String strMakeHex = makeHex(bArrDecode, 2, b2);
                byte b3 = bArrDecode[b2 + 2];
                String strMakeHex2 = makeHex(bArrDecode, b2 + 3, b3);
                int i = b2 + b3;
                String strMakeHex3 = makeHex(bArrDecode, i + 4, bArrDecode[i + 3]);
                sb.append(":" + strMakeHex3 + ":" + strMakeHex2 + ":" + strMakeHex);
                Log.v(TAG, "ik:" + strMakeHex3 + "ck:" + strMakeHex2 + " res:" + strMakeHex);
            } else if (b == -36) {
                Log.e(TAG, "synchronisation failure");
                String strMakeHex4 = makeHex(bArrDecode, 2, bArrDecode[1]);
                sb.append(":" + strMakeHex4);
                Log.v(TAG, "auts:" + strMakeHex4);
                str = WifiNative.SIM_AUTH_RESP_TYPE_UMTS_AUTS;
            } else {
                Log.e(TAG, "bad response - unknown tag = " + ((int) b));
            }
            z = true;
        } else {
            Log.e(TAG, "bad response - " + iccAuthentication);
        }
        if (!z) {
            return null;
        }
        String string = sb.toString();
        Log.v(TAG, "Supplicant Response -" + string);
        return new SimAuthResponseData(str, string);
    }
}
