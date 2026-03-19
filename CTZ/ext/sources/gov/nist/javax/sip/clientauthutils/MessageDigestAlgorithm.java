package gov.nist.javax.sip.clientauthutils;

import gov.nist.core.Separators;
import gov.nist.core.StackLogger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MessageDigestAlgorithm {
    private static final char[] toHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    static String calculateResponse(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, StackLogger stackLogger) {
        String str10;
        if (stackLogger.isLoggingEnabled()) {
            stackLogger.logDebug("trying to authenticate using : " + str + ", " + str2 + ", " + str3 + ", " + str4 + ", " + str5 + ", " + str6 + ", " + str7 + ", " + str8 + ", " + str9);
        }
        if (str2 == null || str6 == null || str7 == null || str3 == null) {
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");
        }
        if (str5 == null || str5.length() == 0) {
            throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
        }
        if (str9 == null || str9.trim().length() == 0 || str9.trim().equalsIgnoreCase("auth")) {
            str10 = str6 + Separators.COLON + str7;
        } else {
            if (str8 == null) {
                str8 = "";
            }
            str10 = str6 + Separators.COLON + str7 + Separators.COLON + H(str8);
        }
        if (str5 != null && str9 != null && str4 != null && (str9.equalsIgnoreCase("auth") || str9.equalsIgnoreCase("auth-int"))) {
            return KD(str2, str3 + Separators.COLON + str4 + Separators.COLON + str5 + Separators.COLON + str9 + Separators.COLON + H(str10));
        }
        return KD(str2, str3 + Separators.COLON + H(str10));
    }

    static String calculateResponse(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11, StackLogger stackLogger) {
        String string;
        String str12;
        if (stackLogger.isLoggingEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("trying to authenticate using : ");
            sb.append(str);
            sb.append(", ");
            sb.append(str2);
            sb.append(", ");
            sb.append(str3);
            sb.append(", ");
            sb.append(str4 != null && str4.trim().length() > 0);
            sb.append(", ");
            sb.append(str5);
            sb.append(", ");
            sb.append(str6);
            sb.append(", ");
            sb.append(str7);
            sb.append(", ");
            sb.append(str8);
            sb.append(", ");
            sb.append(str9);
            sb.append(", ");
            sb.append(str10);
            sb.append(", ");
            sb.append(str11);
            stackLogger.logDebug(sb.toString());
        }
        if (str2 == null || str3 == null || str4 == null || str8 == null || str9 == null || str5 == null) {
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");
        }
        if (str == null || str.trim().length() == 0 || str.trim().equalsIgnoreCase("MD5")) {
            string = str2 + Separators.COLON + str3 + Separators.COLON + str4;
        } else {
            if (str7 == null || str7.length() == 0) {
                throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append(H(str2 + Separators.COLON + str3 + Separators.COLON + str4));
            sb2.append(Separators.COLON);
            sb2.append(str5);
            sb2.append(Separators.COLON);
            sb2.append(str7);
            string = sb2.toString();
        }
        if (str11 == null || str11.trim().length() == 0 || str11.trim().equalsIgnoreCase("auth")) {
            str12 = str8 + Separators.COLON + str9;
        } else {
            if (str10 == null) {
                str10 = "";
            }
            str12 = str8 + Separators.COLON + str9 + Separators.COLON + H(str10);
        }
        if (str7 != null && str11 != null && str6 != null && (str11.equalsIgnoreCase("auth") || str11.equalsIgnoreCase("auth-int"))) {
            return KD(H(string), str5 + Separators.COLON + str6 + Separators.COLON + str7 + Separators.COLON + str11 + Separators.COLON + H(str12));
        }
        return KD(H(string), str5 + Separators.COLON + H(str12));
    }

    private static String H(String str) {
        try {
            return toHexString(MessageDigest.getInstance("MD5").digest(str.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to instantiate an MD5 algorithm", e);
        }
    }

    private static String KD(String str, String str2) {
        return H(str + Separators.COLON + str2);
    }

    private static String toHexString(byte[] bArr) {
        char[] cArr = new char[bArr.length * 2];
        int i = 0;
        for (int i2 = 0; i2 < bArr.length; i2++) {
            int i3 = i + 1;
            cArr[i] = toHex[(bArr[i2] >> 4) & 15];
            i = i3 + 1;
            cArr[i3] = toHex[bArr[i2] & 15];
        }
        return new String(cArr);
    }
}
