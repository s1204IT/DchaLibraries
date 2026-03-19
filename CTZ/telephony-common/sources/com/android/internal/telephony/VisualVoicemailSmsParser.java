package com.android.internal.telephony;

import android.os.Bundle;

public class VisualVoicemailSmsParser {
    private static final String[] ALLOWED_ALTERNATIVE_FORMAT_EVENT = {"MBOXUPDATE", "UNRECOGNIZED"};

    public static class WrappedMessageData {
        public final Bundle fields;
        public final String prefix;

        public String toString() {
            return "WrappedMessageData [type=" + this.prefix + " fields=" + this.fields + "]";
        }

        WrappedMessageData(String str, Bundle bundle) {
            this.prefix = str;
            this.fields = bundle;
        }
    }

    public static WrappedMessageData parse(String str, String str2) {
        int i;
        int iIndexOf;
        try {
            if (!str2.startsWith(str)) {
                return null;
            }
            int length = str.length();
            if (str2.charAt(length) != ':' || (iIndexOf = str2.indexOf(":", (i = length + 1))) == -1) {
                return null;
            }
            String strSubstring = str2.substring(i, iIndexOf);
            Bundle smsBody = parseSmsBody(str2.substring(iIndexOf + 1));
            if (smsBody == null) {
                return null;
            }
            return new WrappedMessageData(strSubstring, smsBody);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static Bundle parseSmsBody(String str) {
        Bundle bundle = new Bundle();
        for (String str2 : str.split(";")) {
            if (str2.length() != 0) {
                int iIndexOf = str2.indexOf("=");
                if (iIndexOf == -1 || iIndexOf == 0) {
                    return null;
                }
                bundle.putString(str2.substring(0, iIndexOf), str2.substring(iIndexOf + 1));
            }
        }
        return bundle;
    }

    public static WrappedMessageData parseAlternativeFormat(String str) {
        Bundle smsBody;
        try {
            int iIndexOf = str.indexOf("?");
            if (iIndexOf == -1) {
                return null;
            }
            String strSubstring = str.substring(0, iIndexOf);
            if (!isAllowedAlternativeFormatEvent(strSubstring) || (smsBody = parseSmsBody(str.substring(iIndexOf + 1))) == null) {
                return null;
            }
            return new WrappedMessageData(strSubstring, smsBody);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    private static boolean isAllowedAlternativeFormatEvent(String str) {
        for (String str2 : ALLOWED_ALTERNATIVE_FORMAT_EVENT) {
            if (str2.equals(str)) {
                return true;
            }
        }
        return false;
    }
}
