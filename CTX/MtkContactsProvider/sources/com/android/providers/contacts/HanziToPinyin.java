package com.android.providers.contacts;

import android.icu.text.Transliterator;
import android.text.TextUtils;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HanziToPinyin {
    private static final ArrayList<Character> SPECIAL_CHARS_LIST_LOWER;
    private static final ArrayList<Character> SPECIAL_CHARS_LIST_UPPER;
    private static HanziToPinyin sInstance;
    private static Map<Character, Character> sMuiSupportMap = new HashMap();
    private Transliterator mAsciiTransliterator;
    private Transliterator mPinyinTransliterator;

    public static class Token {
        public String source;
        public String target;
        public int type;

        public Token() {
        }

        public Token(int i, String str, String str2) {
            this.type = i;
            this.source = str;
            this.target = str2;
        }
    }

    private HanziToPinyin() {
        try {
            this.mPinyinTransliterator = Transliterator.getInstance("Han-Latin/Names; Latin-Ascii; Any-Upper");
            this.mAsciiTransliterator = Transliterator.getInstance("Latin-Ascii");
        } catch (IllegalArgumentException e) {
            Log.w("HanziToPinyin", "Han-Latin/Names transliterator data is missing, HanziToPinyin is disabled");
        }
    }

    public boolean hasChineseTransliterator() {
        return this.mPinyinTransliterator != null;
    }

    public static HanziToPinyin getInstance() {
        HanziToPinyin hanziToPinyin;
        synchronized (HanziToPinyin.class) {
            if (sInstance == null) {
                sInstance = new HanziToPinyin();
            }
            hanziToPinyin = sInstance;
        }
        return hanziToPinyin;
    }

    private void tokenize(char c, Token token) {
        token.source = Character.toString(c);
        if (c < 128) {
            token.type = 1;
            token.target = token.source;
            return;
        }
        if (c < 592 || (7680 <= c && c < 7935)) {
            token.type = 1;
            token.target = this.mAsciiTransliterator == null ? token.source : this.mAsciiTransliterator.transliterate(token.source);
            return;
        }
        token.type = 2;
        token.target = this.mPinyinTransliterator.transliterate(token.source);
        if (TextUtils.isEmpty(token.target) || TextUtils.equals(token.source, token.target)) {
            token.type = 3;
            token.target = token.source;
        }
    }

    public String transliterate(String str) {
        if (!hasChineseTransliterator() || TextUtils.isEmpty(str)) {
            return null;
        }
        return this.mPinyinTransliterator.transliterate(str);
    }

    public ArrayList<Token> getTokens(String str) {
        ArrayList<Token> arrayList = new ArrayList<>();
        if (!hasChineseTransliterator() || TextUtils.isEmpty(str)) {
            return arrayList;
        }
        int length = str.length();
        StringBuilder sb = new StringBuilder();
        Token token = new Token();
        int i = 1;
        for (int i2 = 0; i2 < length; i2++) {
            char cCharAt = str.charAt(i2);
            if (Character.isSpaceChar(cCharAt)) {
                if (sb.length() > 0) {
                    addToken(sb, arrayList, i);
                }
            } else {
                tokenize(cCharAt, token);
                if (token.type == 2) {
                    if (sb.length() > 0) {
                        addToken(sb, arrayList, i);
                    }
                    arrayList.add(token);
                    token = new Token();
                } else {
                    if (i != token.type && sb.length() > 0) {
                        addToken(sb, arrayList, i);
                    }
                    sb.append(token.target);
                }
                i = token.type;
            }
        }
        if (sb.length() > 0) {
            addToken(sb, arrayList, i);
        }
        return arrayList;
    }

    private void addToken(StringBuilder sb, ArrayList<Token> arrayList, int i) {
        String string = sb.toString();
        arrayList.add(new Token(i, string, string));
        sb.setLength(0);
    }

    static {
        sMuiSupportMap.put((char) 1040, '2');
        sMuiSupportMap.put((char) 1041, '2');
        sMuiSupportMap.put((char) 1042, '2');
        sMuiSupportMap.put((char) 1043, '2');
        sMuiSupportMap.put((char) 1044, '3');
        sMuiSupportMap.put((char) 1045, '3');
        sMuiSupportMap.put((char) 1046, '3');
        sMuiSupportMap.put((char) 1047, '3');
        sMuiSupportMap.put((char) 1048, '4');
        sMuiSupportMap.put((char) 1049, '4');
        sMuiSupportMap.put((char) 1050, '4');
        sMuiSupportMap.put((char) 1051, '4');
        sMuiSupportMap.put((char) 1052, '5');
        sMuiSupportMap.put((char) 1053, '5');
        sMuiSupportMap.put((char) 1054, '5');
        sMuiSupportMap.put((char) 1055, '5');
        sMuiSupportMap.put((char) 1056, '6');
        sMuiSupportMap.put((char) 1057, '6');
        sMuiSupportMap.put((char) 1058, '6');
        sMuiSupportMap.put((char) 1059, '6');
        sMuiSupportMap.put((char) 1060, '7');
        sMuiSupportMap.put((char) 1061, '7');
        sMuiSupportMap.put((char) 1062, '7');
        sMuiSupportMap.put((char) 1063, '7');
        sMuiSupportMap.put((char) 1064, '8');
        sMuiSupportMap.put((char) 1065, '8');
        sMuiSupportMap.put((char) 1066, '8');
        sMuiSupportMap.put((char) 1067, '8');
        sMuiSupportMap.put((char) 1068, '9');
        sMuiSupportMap.put((char) 1069, '9');
        sMuiSupportMap.put((char) 1070, '9');
        sMuiSupportMap.put((char) 1071, '9');
        sMuiSupportMap.put((char) 1072, '2');
        sMuiSupportMap.put((char) 1073, '2');
        sMuiSupportMap.put((char) 1074, '2');
        sMuiSupportMap.put((char) 1075, '2');
        sMuiSupportMap.put((char) 1076, '3');
        sMuiSupportMap.put((char) 1077, '3');
        sMuiSupportMap.put((char) 1078, '3');
        sMuiSupportMap.put((char) 1079, '3');
        sMuiSupportMap.put((char) 1080, '4');
        sMuiSupportMap.put((char) 1081, '4');
        sMuiSupportMap.put((char) 1082, '4');
        sMuiSupportMap.put((char) 1083, '4');
        sMuiSupportMap.put((char) 1084, '5');
        sMuiSupportMap.put((char) 1085, '5');
        sMuiSupportMap.put((char) 1086, '5');
        sMuiSupportMap.put((char) 1087, '5');
        sMuiSupportMap.put((char) 1088, '6');
        sMuiSupportMap.put((char) 1089, '6');
        sMuiSupportMap.put((char) 1090, '6');
        sMuiSupportMap.put((char) 1091, '6');
        sMuiSupportMap.put((char) 1092, '7');
        sMuiSupportMap.put((char) 1093, '7');
        sMuiSupportMap.put((char) 1094, '7');
        sMuiSupportMap.put((char) 1095, '7');
        sMuiSupportMap.put((char) 1096, '8');
        sMuiSupportMap.put((char) 1097, '8');
        sMuiSupportMap.put((char) 1098, '8');
        sMuiSupportMap.put((char) 1099, '8');
        sMuiSupportMap.put((char) 1100, '9');
        sMuiSupportMap.put((char) 1101, '9');
        sMuiSupportMap.put((char) 1102, '9');
        sMuiSupportMap.put((char) 1103, '9');
        sMuiSupportMap.put((char) 1025, '3');
        sMuiSupportMap.put((char) 1105, '3');
        sMuiSupportMap.put((char) 1576, '2');
        sMuiSupportMap.put((char) 1577, '2');
        sMuiSupportMap.put((char) 1578, '2');
        sMuiSupportMap.put((char) 1579, '2');
        sMuiSupportMap.put((char) 1569, '3');
        sMuiSupportMap.put((char) 1575, '3');
        sMuiSupportMap.put((char) 1587, '4');
        sMuiSupportMap.put((char) 1588, '4');
        sMuiSupportMap.put((char) 1589, '4');
        sMuiSupportMap.put((char) 1590, '4');
        sMuiSupportMap.put((char) 1583, '5');
        sMuiSupportMap.put((char) 1584, '5');
        sMuiSupportMap.put((char) 1585, '5');
        sMuiSupportMap.put((char) 1586, '5');
        sMuiSupportMap.put((char) 1580, '6');
        sMuiSupportMap.put((char) 1581, '6');
        sMuiSupportMap.put((char) 1582, '6');
        sMuiSupportMap.put((char) 1606, '7');
        sMuiSupportMap.put((char) 1607, '7');
        sMuiSupportMap.put((char) 1608, '7');
        sMuiSupportMap.put((char) 1609, '7');
        sMuiSupportMap.put((char) 1601, '8');
        sMuiSupportMap.put((char) 1602, '8');
        sMuiSupportMap.put((char) 1603, '8');
        sMuiSupportMap.put((char) 1604, '8');
        sMuiSupportMap.put((char) 1605, '8');
        sMuiSupportMap.put((char) 1591, '9');
        sMuiSupportMap.put((char) 1592, '9');
        sMuiSupportMap.put((char) 1593, '9');
        sMuiSupportMap.put((char) 1594, '9');
        sMuiSupportMap.put((char) 1491, '2');
        sMuiSupportMap.put((char) 1492, '2');
        sMuiSupportMap.put((char) 1493, '2');
        sMuiSupportMap.put((char) 1488, '3');
        sMuiSupportMap.put((char) 1489, '3');
        sMuiSupportMap.put((char) 1490, '3');
        sMuiSupportMap.put((char) 1502, '4');
        sMuiSupportMap.put((char) 1504, '4');
        sMuiSupportMap.put((char) 1500, '5');
        sMuiSupportMap.put((char) 1499, '5');
        sMuiSupportMap.put((char) 1494, '6');
        sMuiSupportMap.put((char) 1495, '6');
        sMuiSupportMap.put((char) 1496, '6');
        sMuiSupportMap.put((char) 1512, '7');
        sMuiSupportMap.put((char) 1513, '7');
        sMuiSupportMap.put((char) 1514, '7');
        sMuiSupportMap.put((char) 1510, '8');
        sMuiSupportMap.put((char) 1511, '8');
        sMuiSupportMap.put((char) 1505, '9');
        sMuiSupportMap.put((char) 1506, '9');
        sMuiSupportMap.put((char) 1507, '9');
        SPECIAL_CHARS_LIST_UPPER = new ArrayList<>();
        SPECIAL_CHARS_LIST_LOWER = new ArrayList<>();
        SPECIAL_CHARS_LIST_UPPER.add((char) 1025);
        SPECIAL_CHARS_LIST_LOWER.add((char) 1105);
    }
}
