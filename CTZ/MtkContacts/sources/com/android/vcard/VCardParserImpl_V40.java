package com.android.vcard;

import java.util.Set;

class VCardParserImpl_V40 extends VCardParserImpl_V30 {
    @Override
    protected int getVersion() {
        return 2;
    }

    @Override
    protected String getVersionString() {
        return "4.0";
    }

    @Override
    protected String maybeUnescapeText(String str) {
        return unescapeText(str);
    }

    public static String unescapeText(String str) {
        StringBuilder sb = new StringBuilder();
        int length = str.length();
        int i = 0;
        while (i < length) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '\\' && i < length - 1) {
                i++;
                char cCharAt2 = str.charAt(i);
                if (cCharAt2 == 'n' || cCharAt2 == 'N') {
                    sb.append("\n");
                } else {
                    sb.append(cCharAt2);
                }
            } else {
                sb.append(cCharAt);
            }
            i++;
        }
        return sb.toString();
    }

    public static String unescapeCharacter(char c) {
        if (c == 'n' || c == 'N') {
            return "\n";
        }
        return String.valueOf(c);
    }

    @Override
    protected Set<String> getKnownPropertyNameSet() {
        return VCardParser_V40.sKnownPropertyNameSet;
    }
}
