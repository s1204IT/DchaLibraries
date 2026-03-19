package com.android.org.bouncycastle.util;

public class IPAddress {
    public static boolean isValid(String str) {
        return isValidIPv4(str) || isValidIPv6(str);
    }

    public static boolean isValidWithNetMask(String str) {
        return isValidIPv4WithNetmask(str) || isValidIPv6WithNetmask(str);
    }

    public static boolean isValidIPv4(String str) {
        int iIndexOf;
        if (str.length() == 0) {
            return false;
        }
        String str2 = str + ".";
        int i = 0;
        int i2 = 0;
        while (i < str2.length() && (iIndexOf = str2.indexOf(46, i)) > i) {
            if (i2 == 4) {
                return false;
            }
            try {
                int i3 = Integer.parseInt(str2.substring(i, iIndexOf));
                if (i3 < 0 || i3 > 255) {
                    return false;
                }
                i = iIndexOf + 1;
                i2++;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return i2 == 4;
    }

    public static boolean isValidIPv4WithNetmask(String str) {
        int iIndexOf = str.indexOf("/");
        String strSubstring = str.substring(iIndexOf + 1);
        if (iIndexOf <= 0 || !isValidIPv4(str.substring(0, iIndexOf))) {
            return false;
        }
        return isValidIPv4(strSubstring) || isMaskValue(strSubstring, 32);
    }

    public static boolean isValidIPv6WithNetmask(String str) {
        int iIndexOf = str.indexOf("/");
        String strSubstring = str.substring(iIndexOf + 1);
        if (iIndexOf <= 0 || !isValidIPv6(str.substring(0, iIndexOf))) {
            return false;
        }
        return isValidIPv6(strSubstring) || isMaskValue(strSubstring, 128);
    }

    private static boolean isMaskValue(String str, int i) {
        try {
            int i2 = Integer.parseInt(str);
            return i2 >= 0 && i2 <= i;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidIPv6(String str) {
        int iIndexOf;
        if (str.length() == 0) {
            return false;
        }
        String str2 = str + ":";
        int i = 0;
        int i2 = 0;
        boolean z = false;
        while (i < str2.length() && (iIndexOf = str2.indexOf(58, i)) >= i) {
            if (i2 == 8) {
                return false;
            }
            if (i != iIndexOf) {
                String strSubstring = str2.substring(i, iIndexOf);
                if (iIndexOf == str2.length() - 1 && strSubstring.indexOf(46) > 0) {
                    if (!isValidIPv4(strSubstring)) {
                        return false;
                    }
                    i2++;
                } else {
                    try {
                        int i3 = Integer.parseInt(str2.substring(i, iIndexOf), 16);
                        if (i3 < 0 || i3 > 65535) {
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }
            } else {
                if (iIndexOf != 1 && iIndexOf != str2.length() - 1 && z) {
                    return false;
                }
                z = true;
            }
            i = iIndexOf + 1;
            i2++;
        }
        return i2 == 8 || z;
    }
}
