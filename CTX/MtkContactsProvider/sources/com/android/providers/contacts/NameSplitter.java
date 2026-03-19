package com.android.providers.contacts;

import android.content.ContentValues;
import android.text.TextUtils;
import android.util.ArraySet;
import java.lang.Character;
import java.util.Locale;
import java.util.StringTokenizer;

public class NameSplitter {
    private final ArraySet<String> mConjuctions;
    private final String mLanguage;
    private final ArraySet<String> mLastNamePrefixesSet;
    private final Locale mLocale;
    private final int mMaxSuffixLength;
    private final ArraySet<String> mPrefixesSet;
    private final ArraySet<String> mSuffixesSet;
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();
    private static final String KOREAN_LANGUAGE = Locale.KOREAN.getLanguage().toLowerCase();
    private static final String CHINESE_LANGUAGE = Locale.CHINESE.getLanguage().toLowerCase();
    private static final String[] KOREAN_TWO_CHARCTER_FAMILY_NAMES = {"강전", "남궁", "독고", "동방", "망절", "사공", "서문", "선우", "소봉", "어금", "장곡", "제갈", "황보"};

    public static class Name {
        public String familyName;
        public int fullNameStyle;
        public String givenNames;
        public String middleName;
        public String phoneticFamilyName;
        public String phoneticGivenName;
        public String phoneticMiddleName;
        public int phoneticNameStyle;
        public String prefix;
        public String suffix;

        public String getPrefix() {
            return this.prefix;
        }

        public String getGivenNames() {
            return this.givenNames;
        }

        public String getMiddleName() {
            return this.middleName;
        }

        public String getFamilyName() {
            return this.familyName;
        }

        public String getSuffix() {
            return this.suffix;
        }

        public void fromValues(ContentValues contentValues) {
            this.prefix = contentValues.getAsString("data4");
            this.givenNames = contentValues.getAsString("data2");
            this.middleName = contentValues.getAsString("data5");
            this.familyName = contentValues.getAsString("data3");
            this.suffix = contentValues.getAsString("data6");
            Integer asInteger = contentValues.getAsInteger("data10");
            this.fullNameStyle = asInteger == null ? 0 : asInteger.intValue();
            this.phoneticFamilyName = contentValues.getAsString("data9");
            this.phoneticMiddleName = contentValues.getAsString("data8");
            this.phoneticGivenName = contentValues.getAsString("data7");
            Integer asInteger2 = contentValues.getAsInteger("data11");
            this.phoneticNameStyle = asInteger2 != null ? asInteger2.intValue() : 0;
        }

        public void toValues(ContentValues contentValues) {
            putValueIfPresent(contentValues, "data4", this.prefix);
            putValueIfPresent(contentValues, "data2", this.givenNames);
            putValueIfPresent(contentValues, "data5", this.middleName);
            putValueIfPresent(contentValues, "data3", this.familyName);
            putValueIfPresent(contentValues, "data6", this.suffix);
            contentValues.put("data10", Integer.valueOf(this.fullNameStyle));
            putValueIfPresent(contentValues, "data9", this.phoneticFamilyName);
            putValueIfPresent(contentValues, "data8", this.phoneticMiddleName);
            putValueIfPresent(contentValues, "data7", this.phoneticGivenName);
            contentValues.put("data11", Integer.valueOf(this.phoneticNameStyle));
        }

        private void putValueIfPresent(ContentValues contentValues, String str, String str2) {
            if (str2 != null) {
                contentValues.put(str, str2);
            }
        }

        public void clear() {
            this.prefix = null;
            this.givenNames = null;
            this.middleName = null;
            this.familyName = null;
            this.suffix = null;
            this.fullNameStyle = 0;
            this.phoneticFamilyName = null;
            this.phoneticMiddleName = null;
            this.phoneticGivenName = null;
            this.phoneticNameStyle = 0;
        }

        public boolean isEmpty() {
            return TextUtils.isEmpty(this.givenNames) && TextUtils.isEmpty(this.middleName) && TextUtils.isEmpty(this.familyName) && TextUtils.isEmpty(this.suffix) && TextUtils.isEmpty(this.phoneticFamilyName) && TextUtils.isEmpty(this.phoneticMiddleName) && TextUtils.isEmpty(this.phoneticGivenName);
        }

        public String toString() {
            return "[prefix: " + this.prefix + " given: " + this.givenNames + " middle: " + this.middleName + " family: " + this.familyName + " suffix: " + this.suffix + " ph/given: " + this.phoneticGivenName + " ph/middle: " + this.phoneticMiddleName + " ph/family: " + this.phoneticFamilyName + "]";
        }
    }

    private static class NameTokenizer extends StringTokenizer {
        private int mCommaBitmask;
        private int mDotBitmask;
        private int mEndPointer;
        private int mStartPointer;
        private final String[] mTokens;

        static int access$008(NameTokenizer nameTokenizer) {
            int i = nameTokenizer.mStartPointer;
            nameTokenizer.mStartPointer = i + 1;
            return i;
        }

        static int access$012(NameTokenizer nameTokenizer, int i) {
            int i2 = nameTokenizer.mStartPointer + i;
            nameTokenizer.mStartPointer = i2;
            return i2;
        }

        static int access$110(NameTokenizer nameTokenizer) {
            int i = nameTokenizer.mEndPointer;
            nameTokenizer.mEndPointer = i - 1;
            return i;
        }

        public NameTokenizer(String str) {
            super(str, " .,", true);
            this.mTokens = new String[10];
            while (hasMoreTokens() && this.mEndPointer < 10) {
                String strNextToken = nextToken();
                if (strNextToken.length() <= 0 || strNextToken.charAt(0) != ' ') {
                    if (this.mEndPointer > 0 && strNextToken.charAt(0) == '.') {
                        this.mDotBitmask |= 1 << (this.mEndPointer - 1);
                    } else if (this.mEndPointer > 0 && strNextToken.charAt(0) == ',') {
                        this.mCommaBitmask |= 1 << (this.mEndPointer - 1);
                    } else {
                        this.mTokens[this.mEndPointer] = strNextToken;
                        this.mEndPointer++;
                    }
                }
            }
        }

        public boolean hasDot(int i) {
            return ((1 << i) & this.mDotBitmask) != 0;
        }

        public boolean hasComma(int i) {
            return ((1 << i) & this.mCommaBitmask) != 0;
        }
    }

    public NameSplitter(String str, String str2, String str3, String str4, Locale locale) {
        this.mPrefixesSet = convertToSet(str);
        this.mLastNamePrefixesSet = convertToSet(str2);
        this.mSuffixesSet = convertToSet(str3);
        this.mConjuctions = convertToSet(str4);
        this.mLocale = locale == null ? Locale.getDefault() : locale;
        this.mLanguage = this.mLocale.getLanguage().toLowerCase();
        int length = 0;
        for (String str5 : this.mSuffixesSet) {
            if (str5.length() > length) {
                length = str5.length();
            }
        }
        this.mMaxSuffixLength = length;
    }

    private static ArraySet<String> convertToSet(String str) {
        ArraySet<String> arraySet = new ArraySet<>();
        if (str != null) {
            for (String str2 : str.split(",")) {
                arraySet.add(str2.trim().toUpperCase());
            }
        }
        return arraySet;
    }

    public int tokenize(String[] strArr, String str) {
        int i = 0;
        if (str == null) {
            return 0;
        }
        NameTokenizer nameTokenizer = new NameTokenizer(str);
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer) {
            String str2 = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
            int i2 = nameTokenizer.mStartPointer;
            while (i2 < nameTokenizer.mEndPointer) {
                strArr[i] = nameTokenizer.mTokens[i2];
                i2++;
                i++;
            }
            return i;
        }
        return 0;
    }

    public void split(Name name, String str) {
        if (str == null) {
            return;
        }
        int iGuessFullNameStyle = guessFullNameStyle(str);
        if (iGuessFullNameStyle == 2) {
            iGuessFullNameStyle = getAdjustedFullNameStyle(iGuessFullNameStyle);
        }
        split(name, str, iGuessFullNameStyle);
    }

    public void split(Name name, String str, int i) {
        if (str == null) {
        }
        name.fullNameStyle = i;
        switch (i) {
            case 3:
                splitChineseName(name, str);
                break;
            case 4:
                splitJapaneseName(name, str);
                break;
            case 5:
                splitKoreanName(name, str);
                break;
            default:
                splitWesternName(name, str);
                break;
        }
    }

    private void splitWesternName(Name name, String str) {
        NameTokenizer nameTokenizer = new NameTokenizer(str);
        parsePrefix(name, nameTokenizer);
        if (nameTokenizer.mEndPointer > 2) {
            parseSuffix(name, nameTokenizer);
        }
        if (name.prefix == null && nameTokenizer.mEndPointer - nameTokenizer.mStartPointer == 1) {
            name.givenNames = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
            return;
        }
        parseLastName(name, nameTokenizer);
        parseMiddleName(name, nameTokenizer);
        parseGivenNames(name, nameTokenizer);
    }

    private void splitChineseName(Name name, String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str);
        while (stringTokenizer.hasMoreTokens()) {
            String strNextToken = stringTokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = strNextToken;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = strNextToken;
            } else if (name.middleName == null) {
                name.middleName = name.givenNames;
                name.givenNames = strNextToken;
            } else {
                name.middleName += name.givenNames;
                name.givenNames = strNextToken;
            }
        }
        if (name.givenNames != null && name.familyName == null && name.middleName == null) {
            int length = str.length();
            if (length == 2) {
                name.familyName = str.substring(0, 1);
                name.givenNames = str.substring(1);
            } else if (length == 3) {
                name.familyName = str.substring(0, 1);
                name.middleName = str.substring(1, 2);
                name.givenNames = str.substring(2);
            } else if (length == 4) {
                name.familyName = str.substring(0, 2);
                name.middleName = str.substring(2, 3);
                name.givenNames = str.substring(3);
            }
        }
    }

    private void splitJapaneseName(Name name, String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str);
        while (stringTokenizer.hasMoreTokens()) {
            String strNextToken = stringTokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = strNextToken;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = strNextToken;
            } else {
                name.givenNames += " " + strNextToken;
            }
        }
    }

    private void splitKoreanName(Name name, String str) {
        StringTokenizer stringTokenizer = new StringTokenizer(str);
        int i = 1;
        if (stringTokenizer.countTokens() > 1) {
            while (stringTokenizer.hasMoreTokens()) {
                String strNextToken = stringTokenizer.nextToken();
                if (name.givenNames == null) {
                    name.givenNames = strNextToken;
                } else if (name.familyName == null) {
                    name.familyName = name.givenNames;
                    name.givenNames = strNextToken;
                } else {
                    name.givenNames += " " + strNextToken;
                }
            }
            return;
        }
        String[] strArr = KOREAN_TWO_CHARCTER_FAMILY_NAMES;
        int length = strArr.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            if (!str.startsWith(strArr[i2])) {
                i2++;
            } else {
                i = 2;
                break;
            }
        }
        name.familyName = str.substring(0, i);
        if (str.length() > i) {
            name.givenNames = str.substring(i);
        }
    }

    public String join(Name name, boolean z, boolean z2) {
        String str = z2 ? name.prefix : null;
        switch (name.fullNameStyle) {
            case 2:
            case 3:
            case 5:
                return join(str, name.familyName, name.middleName, name.givenNames, name.suffix, false, false, false);
            case 4:
                return join(str, name.familyName, name.middleName, name.givenNames, name.suffix, true, false, false);
            default:
                if (z) {
                    return join(str, name.givenNames, name.middleName, name.familyName, name.suffix, true, false, true);
                }
                return join(str, name.familyName, name.givenNames, name.middleName, name.suffix, true, true, true);
        }
    }

    public String joinPhoneticName(Name name) {
        return join(null, name.phoneticFamilyName, name.phoneticMiddleName, name.phoneticGivenName, null, true, false, false);
    }

    private String join(String str, String str2, String str3, String str4, String str5, boolean z, boolean z2, boolean z3) {
        String strTrim;
        String strTrim2;
        String strTrim3;
        String strTrim4;
        String strTrim5;
        String strNormalizedSuffix = null;
        if (str != null) {
            strTrim = str.trim();
        } else {
            strTrim = null;
        }
        if (str2 != null) {
            strTrim2 = str2.trim();
        } else {
            strTrim2 = null;
        }
        if (str3 != null) {
            strTrim3 = str3.trim();
        } else {
            strTrim3 = null;
        }
        if (str4 != null) {
            strTrim4 = str4.trim();
        } else {
            strTrim4 = null;
        }
        if (str5 != null) {
            strTrim5 = str5.trim();
        } else {
            strTrim5 = null;
        }
        boolean z4 = true;
        boolean z5 = !TextUtils.isEmpty(strTrim);
        boolean z6 = !TextUtils.isEmpty(strTrim2);
        boolean z7 = !TextUtils.isEmpty(strTrim3);
        boolean z8 = !TextUtils.isEmpty(strTrim4);
        boolean z9 = !TextUtils.isEmpty(strTrim5);
        if (z5) {
            strNormalizedSuffix = strTrim;
        }
        if (z6) {
            if (strNormalizedSuffix != null) {
                z4 = false;
            } else {
                strNormalizedSuffix = strTrim2;
            }
        }
        if (z7) {
            if (strNormalizedSuffix != null) {
                z4 = false;
            } else {
                strNormalizedSuffix = strTrim3;
            }
        }
        if (z8) {
            if (strNormalizedSuffix != null) {
                z4 = false;
            } else {
                strNormalizedSuffix = strTrim4;
            }
        }
        if (z9) {
            if (strNormalizedSuffix == null) {
                strNormalizedSuffix = normalizedSuffix(strTrim5);
            } else {
                z4 = false;
            }
        }
        if (z4) {
            return strNormalizedSuffix;
        }
        StringBuilder sb = new StringBuilder();
        if (z5) {
            sb.append(strTrim);
        }
        if (z6) {
            if (z5) {
                sb.append(' ');
            }
            sb.append(strTrim2);
        }
        if (z7) {
            if (z5 || z6) {
                if (z2) {
                    sb.append(',');
                }
                if (z) {
                    sb.append(' ');
                }
            }
            sb.append(strTrim3);
        }
        if (z8) {
            if ((z5 || z6 || z7) && z) {
                sb.append(' ');
            }
            sb.append(strTrim4);
        }
        if (z9) {
            if (z5 || z6 || z7 || z8) {
                if (z3) {
                    sb.append(',');
                }
                if (z) {
                    sb.append(' ');
                }
            }
            sb.append(normalizedSuffix(strTrim5));
        }
        return sb.toString();
    }

    private String normalizedSuffix(String str) {
        int length = str.length();
        if (length == 0 || str.charAt(length - 1) == '.') {
            return str;
        }
        String str2 = str + '.';
        if (this.mSuffixesSet.contains(str2.toUpperCase())) {
            return str2;
        }
        return str;
    }

    public int getAdjustedFullNameStyle(int i) {
        if (i == 0) {
            if (JAPANESE_LANGUAGE.equals(this.mLanguage)) {
                return 4;
            }
            if (KOREAN_LANGUAGE.equals(this.mLanguage)) {
                return 5;
            }
            if (CHINESE_LANGUAGE.equals(this.mLanguage)) {
                return 3;
            }
            return 1;
        }
        if (i == 2) {
            if (JAPANESE_LANGUAGE.equals(this.mLanguage)) {
                return 4;
            }
            return KOREAN_LANGUAGE.equals(this.mLanguage) ? 5 : 3;
        }
        return i;
    }

    private void parsePrefix(Name name, NameTokenizer nameTokenizer) {
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer) {
            String str = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
            if (this.mPrefixesSet.contains(str.toUpperCase())) {
                if (nameTokenizer.hasDot(nameTokenizer.mStartPointer)) {
                    str = str + '.';
                }
                name.prefix = str;
                NameTokenizer.access$008(nameTokenizer);
            }
        }
    }

    private void parseSuffix(Name name, NameTokenizer nameTokenizer) {
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer) {
            String str = nameTokenizer.mTokens[nameTokenizer.mEndPointer - 1];
            if (nameTokenizer.mEndPointer - nameTokenizer.mStartPointer > 2 && nameTokenizer.hasComma(nameTokenizer.mEndPointer - 2)) {
                if (nameTokenizer.hasDot(nameTokenizer.mEndPointer - 1)) {
                    str = str + '.';
                }
                name.suffix = str;
                NameTokenizer.access$110(nameTokenizer);
                return;
            }
            if (str.length() > this.mMaxSuffixLength) {
                return;
            }
            String upperCase = str.toUpperCase();
            if (!this.mSuffixesSet.contains(upperCase)) {
                if (nameTokenizer.hasDot(nameTokenizer.mEndPointer - 1)) {
                    str = str + '.';
                }
                String str2 = upperCase + ".";
                int i = nameTokenizer.mEndPointer - 1;
                while (str2.length() <= this.mMaxSuffixLength) {
                    if (!this.mSuffixesSet.contains(str2)) {
                        if (i != nameTokenizer.mStartPointer) {
                            i--;
                            if (nameTokenizer.hasDot(i)) {
                                str = nameTokenizer.mTokens[i] + "." + str;
                            } else {
                                str = nameTokenizer.mTokens[i] + " " + str;
                            }
                            str2 = nameTokenizer.mTokens[i].toUpperCase() + "." + str2;
                        } else {
                            return;
                        }
                    } else {
                        name.suffix = str;
                        nameTokenizer.mEndPointer = i;
                        return;
                    }
                }
                return;
            }
            name.suffix = str;
            NameTokenizer.access$110(nameTokenizer);
        }
    }

    private void parseLastName(Name name, NameTokenizer nameTokenizer) {
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer) {
            if (nameTokenizer.hasComma(nameTokenizer.mStartPointer)) {
                name.familyName = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
                NameTokenizer.access$008(nameTokenizer);
                return;
            }
            if (nameTokenizer.mStartPointer + 1 >= nameTokenizer.mEndPointer || !nameTokenizer.hasComma(nameTokenizer.mStartPointer + 1) || !isFamilyNamePrefix(nameTokenizer.mTokens[nameTokenizer.mStartPointer])) {
                name.familyName = nameTokenizer.mTokens[nameTokenizer.mEndPointer - 1];
                NameTokenizer.access$110(nameTokenizer);
                if (nameTokenizer.mEndPointer - nameTokenizer.mStartPointer > 0) {
                    String str = nameTokenizer.mTokens[nameTokenizer.mEndPointer - 1];
                    if (isFamilyNamePrefix(str)) {
                        if (nameTokenizer.hasDot(nameTokenizer.mEndPointer - 1)) {
                            str = str + '.';
                        }
                        name.familyName = str + " " + name.familyName;
                        NameTokenizer.access$110(nameTokenizer);
                        return;
                    }
                    return;
                }
                return;
            }
            String str2 = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
            if (nameTokenizer.hasDot(nameTokenizer.mStartPointer)) {
                str2 = str2 + '.';
            }
            name.familyName = str2 + " " + nameTokenizer.mTokens[nameTokenizer.mStartPointer + 1];
            NameTokenizer.access$012(nameTokenizer, 2);
        }
    }

    private boolean isFamilyNamePrefix(String str) {
        String upperCase = str.toUpperCase();
        if (!this.mLastNamePrefixesSet.contains(upperCase)) {
            if (!this.mLastNamePrefixesSet.contains(upperCase + ".")) {
                return false;
            }
        }
        return true;
    }

    private void parseMiddleName(Name name, NameTokenizer nameTokenizer) {
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer && nameTokenizer.mEndPointer - nameTokenizer.mStartPointer > 1) {
            if (nameTokenizer.mEndPointer - nameTokenizer.mStartPointer == 2 || !this.mConjuctions.contains(nameTokenizer.mTokens[nameTokenizer.mEndPointer - 2].toUpperCase())) {
                name.middleName = nameTokenizer.mTokens[nameTokenizer.mEndPointer - 1];
                if (nameTokenizer.hasDot(nameTokenizer.mEndPointer - 1)) {
                    name.middleName += '.';
                }
                NameTokenizer.access$110(nameTokenizer);
            }
        }
    }

    private void parseGivenNames(Name name, NameTokenizer nameTokenizer) {
        if (nameTokenizer.mStartPointer != nameTokenizer.mEndPointer) {
            if (nameTokenizer.mEndPointer - nameTokenizer.mStartPointer == 1) {
                name.givenNames = nameTokenizer.mTokens[nameTokenizer.mStartPointer];
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = nameTokenizer.mStartPointer; i < nameTokenizer.mEndPointer; i++) {
                if (i != nameTokenizer.mStartPointer) {
                    sb.append(' ');
                }
                sb.append(nameTokenizer.mTokens[i]);
                if (nameTokenizer.hasDot(i)) {
                    sb.append('.');
                }
            }
            name.givenNames = sb.toString();
        }
    }

    public void guessNameStyle(Name name) {
        guessFullNameStyle(name);
        guessPhoneticNameStyle(name);
        name.fullNameStyle = getAdjustedNameStyleBasedOnPhoneticNameStyle(name.fullNameStyle, name.phoneticNameStyle);
    }

    public int getAdjustedNameStyleBasedOnPhoneticNameStyle(int i, int i2) {
        if (i2 != 0 && (i == 0 || i == 2)) {
            if (i2 == 4) {
                return 4;
            }
            if (i2 == 5) {
                return 5;
            }
            if (i == 2 && i2 == 3) {
                return 3;
            }
        }
        return i;
    }

    private void guessFullNameStyle(Name name) {
        if (name.fullNameStyle != 0) {
            return;
        }
        int iGuessFullNameStyle = guessFullNameStyle(name.givenNames);
        if (iGuessFullNameStyle != 0 && iGuessFullNameStyle != 2 && iGuessFullNameStyle != 1) {
            name.fullNameStyle = iGuessFullNameStyle;
            return;
        }
        int iGuessFullNameStyle2 = guessFullNameStyle(name.familyName);
        if (iGuessFullNameStyle2 != 0) {
            if (iGuessFullNameStyle2 != 2 && iGuessFullNameStyle2 != 1) {
                name.fullNameStyle = iGuessFullNameStyle2;
                return;
            }
            iGuessFullNameStyle = iGuessFullNameStyle2;
        }
        int iGuessFullNameStyle3 = guessFullNameStyle(name.middleName);
        if (iGuessFullNameStyle3 != 0) {
            if (iGuessFullNameStyle3 != 2 && iGuessFullNameStyle3 != 1) {
                name.fullNameStyle = iGuessFullNameStyle3;
                return;
            }
            iGuessFullNameStyle = iGuessFullNameStyle3;
        }
        int iGuessFullNameStyle4 = guessFullNameStyle(name.prefix);
        if (iGuessFullNameStyle4 != 0) {
            if (iGuessFullNameStyle4 != 2 && iGuessFullNameStyle4 != 1) {
                name.fullNameStyle = iGuessFullNameStyle4;
                return;
            }
            iGuessFullNameStyle = iGuessFullNameStyle4;
        }
        int iGuessFullNameStyle5 = guessFullNameStyle(name.suffix);
        if (iGuessFullNameStyle5 != 0) {
            if (iGuessFullNameStyle5 != 2 && iGuessFullNameStyle5 != 1) {
                name.fullNameStyle = iGuessFullNameStyle5;
                return;
            }
            iGuessFullNameStyle = iGuessFullNameStyle5;
        }
        name.fullNameStyle = iGuessFullNameStyle;
    }

    public int guessFullNameStyle(String str) {
        int iCharCount = 0;
        if (str == null) {
            return 0;
        }
        int length = str.length();
        int i = 0;
        while (iCharCount < length) {
            int iCodePointAt = Character.codePointAt(str, iCharCount);
            if (Character.isLetter(iCodePointAt)) {
                Character.UnicodeBlock unicodeBlockOf = Character.UnicodeBlock.of(iCodePointAt);
                if (!isLatinUnicodeBlock(unicodeBlockOf)) {
                    if (isCJKUnicodeBlock(unicodeBlockOf)) {
                        return guessCJKNameStyle(str, iCharCount + Character.charCount(iCodePointAt));
                    }
                    if (isJapanesePhoneticUnicodeBlock(unicodeBlockOf)) {
                        return 4;
                    }
                    if (isKoreanUnicodeBlock(unicodeBlockOf)) {
                        return 5;
                    }
                }
                i = 1;
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return i;
    }

    private int guessCJKNameStyle(String str, int i) {
        int length = str.length();
        while (i < length) {
            int iCodePointAt = Character.codePointAt(str, i);
            if (Character.isLetter(iCodePointAt)) {
                Character.UnicodeBlock unicodeBlockOf = Character.UnicodeBlock.of(iCodePointAt);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlockOf)) {
                    return 4;
                }
                if (isKoreanUnicodeBlock(unicodeBlockOf)) {
                    return 5;
                }
            }
            i += Character.charCount(iCodePointAt);
        }
        return 2;
    }

    private void guessPhoneticNameStyle(Name name) {
        if (name.phoneticNameStyle != 0) {
            return;
        }
        int iGuessPhoneticNameStyle = guessPhoneticNameStyle(name.phoneticFamilyName);
        if (iGuessPhoneticNameStyle != 0 && iGuessPhoneticNameStyle != 2) {
            name.phoneticNameStyle = iGuessPhoneticNameStyle;
            return;
        }
        int iGuessPhoneticNameStyle2 = guessPhoneticNameStyle(name.phoneticGivenName);
        if (iGuessPhoneticNameStyle2 != 0 && iGuessPhoneticNameStyle2 != 2) {
            name.phoneticNameStyle = iGuessPhoneticNameStyle2;
            return;
        }
        int iGuessPhoneticNameStyle3 = guessPhoneticNameStyle(name.phoneticMiddleName);
        if (iGuessPhoneticNameStyle3 != 0 && iGuessPhoneticNameStyle3 != 2) {
            name.phoneticNameStyle = iGuessPhoneticNameStyle3;
        }
    }

    public int guessPhoneticNameStyle(String str) {
        if (str == null) {
            return 0;
        }
        int length = str.length();
        int iCharCount = 0;
        while (iCharCount < length) {
            int iCodePointAt = Character.codePointAt(str, iCharCount);
            if (Character.isLetter(iCodePointAt)) {
                Character.UnicodeBlock unicodeBlockOf = Character.UnicodeBlock.of(iCodePointAt);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlockOf)) {
                    return 4;
                }
                if (isKoreanUnicodeBlock(unicodeBlockOf)) {
                    return 5;
                }
                if (isLatinUnicodeBlock(unicodeBlockOf)) {
                    return 3;
                }
            }
            iCharCount += Character.charCount(iCodePointAt);
        }
        return 0;
    }

    private static boolean isLatinUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.BASIC_LATIN || unicodeBlock == Character.UnicodeBlock.LATIN_1_SUPPLEMENT || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_A || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_B || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || unicodeBlock == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || unicodeBlock == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.HANGUL_SYLLABLES || unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO || unicodeBlock == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.KATAKANA || unicodeBlock == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS || unicodeBlock == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS || unicodeBlock == Character.UnicodeBlock.HIRAGANA;
    }
}
