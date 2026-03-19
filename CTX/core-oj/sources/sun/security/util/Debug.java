package sun.security.util;

import java.math.BigInteger;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.util.locale.LanguageTag;

public class Debug {
    private static String args;
    private static final char[] hexDigits = "0123456789abcdef".toCharArray();
    private String prefix;

    public static Debug getInstance(String str) {
        return getInstance(str, str);
    }

    public static Debug getInstance(String str, String str2) {
        if (isOn(str)) {
            Debug debug = new Debug();
            debug.prefix = str2;
            return debug;
        }
        return null;
    }

    public static boolean isOn(String str) {
        if (args == null) {
            return false;
        }
        return (args.indexOf("all") == -1 && args.indexOf(str) == -1) ? false : true;
    }

    public void println(String str) {
        System.err.println(this.prefix + ": " + str);
    }

    public void println() {
        System.err.println(this.prefix + ":");
    }

    public static String toHexString(BigInteger bigInteger) {
        String string = bigInteger.toString(16);
        StringBuffer stringBuffer = new StringBuffer(string.length() * 2);
        if (string.startsWith(LanguageTag.SEP)) {
            stringBuffer.append("   -");
            string = string.substring(1);
        } else {
            stringBuffer.append("    ");
        }
        if (string.length() % 2 != 0) {
            string = "0" + string;
        }
        int i = 0;
        while (i < string.length()) {
            int i2 = i + 2;
            stringBuffer.append(string.substring(i, i2));
            if (i2 != string.length()) {
                if (i2 % 64 == 0) {
                    stringBuffer.append("\n    ");
                } else if (i2 % 8 == 0) {
                    stringBuffer.append(" ");
                }
            }
            i = i2;
        }
        return stringBuffer.toString();
    }

    private static String marshal(String str) {
        if (str != null) {
            StringBuffer stringBuffer = new StringBuffer();
            Matcher matcher = Pattern.compile("[Pp][Ee][Rr][Mm][Ii][Ss][Ss][Ii][Oo][Nn]=[a-zA-Z_$][a-zA-Z0-9_$]*([.][a-zA-Z_$][a-zA-Z0-9_$]*)*").matcher(new StringBuffer(str));
            StringBuffer stringBuffer2 = new StringBuffer();
            while (matcher.find()) {
                stringBuffer.append(matcher.group().replaceFirst("[Pp][Ee][Rr][Mm][Ii][Ss][Ss][Ii][Oo][Nn]=", "permission="));
                stringBuffer.append("  ");
                matcher.appendReplacement(stringBuffer2, "");
            }
            matcher.appendTail(stringBuffer2);
            Matcher matcher2 = Pattern.compile("[Cc][Oo][Dd][Ee][Bb][Aa][Ss][Ee]=[^, ;]*").matcher(stringBuffer2);
            StringBuffer stringBuffer3 = new StringBuffer();
            while (matcher2.find()) {
                stringBuffer.append(matcher2.group().replaceFirst("[Cc][Oo][Dd][Ee][Bb][Aa][Ss][Ee]=", "codebase="));
                stringBuffer.append("  ");
                matcher2.appendReplacement(stringBuffer3, "");
            }
            matcher2.appendTail(stringBuffer3);
            stringBuffer.append(stringBuffer3.toString().toLowerCase(Locale.ENGLISH));
            return stringBuffer.toString();
        }
        return null;
    }

    public static String toString(byte[] bArr) {
        if (bArr == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder(bArr.length * 3);
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & Character.DIRECTIONALITY_UNDEFINED;
            if (i != 0) {
                sb.append(':');
            }
            sb.append(hexDigits[i2 >>> 4]);
            sb.append(hexDigits[i2 & 15]);
        }
        return sb.toString();
    }
}
