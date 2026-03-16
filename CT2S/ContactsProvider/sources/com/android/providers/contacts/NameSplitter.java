package com.android.providers.contacts;

import android.content.ContentValues;
import android.text.TextUtils;
import java.lang.Character;
import java.util.HashSet;
import java.util.Locale;
import java.util.StringTokenizer;

public class NameSplitter {
    private final HashSet<String> mConjuctions;
    private final String mLanguage;
    private final HashSet<String> mLastNamePrefixesSet;
    private final Locale mLocale;
    private final int mMaxSuffixLength;
    private final HashSet<String> mPrefixesSet;
    private final HashSet<String> mSuffixesSet;
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

        public void fromValues(ContentValues values) {
            this.prefix = values.getAsString("data4");
            this.givenNames = values.getAsString("data2");
            this.middleName = values.getAsString("data5");
            this.familyName = values.getAsString("data3");
            this.suffix = values.getAsString("data6");
            Integer integer = values.getAsInteger("data10");
            this.fullNameStyle = integer == null ? 0 : integer.intValue();
            this.phoneticFamilyName = values.getAsString("data9");
            this.phoneticMiddleName = values.getAsString("data8");
            this.phoneticGivenName = values.getAsString("data7");
            Integer integer2 = values.getAsInteger("data11");
            this.phoneticNameStyle = integer2 != null ? integer2.intValue() : 0;
        }

        public void toValues(ContentValues values) {
            putValueIfPresent(values, "data4", this.prefix);
            putValueIfPresent(values, "data2", this.givenNames);
            putValueIfPresent(values, "data5", this.middleName);
            putValueIfPresent(values, "data3", this.familyName);
            putValueIfPresent(values, "data6", this.suffix);
            values.put("data10", Integer.valueOf(this.fullNameStyle));
            putValueIfPresent(values, "data9", this.phoneticFamilyName);
            putValueIfPresent(values, "data8", this.phoneticMiddleName);
            putValueIfPresent(values, "data7", this.phoneticGivenName);
            values.put("data11", Integer.valueOf(this.phoneticNameStyle));
        }

        private void putValueIfPresent(ContentValues values, String name, String value) {
            if (value != null) {
                values.put(name, value);
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

        static int access$008(NameTokenizer x0) {
            int i = x0.mStartPointer;
            x0.mStartPointer = i + 1;
            return i;
        }

        static int access$012(NameTokenizer x0, int x1) {
            int i = x0.mStartPointer + x1;
            x0.mStartPointer = i;
            return i;
        }

        static int access$110(NameTokenizer x0) {
            int i = x0.mEndPointer;
            x0.mEndPointer = i - 1;
            return i;
        }

        public NameTokenizer(String fullName) {
            super(fullName, " .,", true);
            this.mTokens = new String[10];
            while (hasMoreTokens() && this.mEndPointer < 10) {
                String token = nextToken();
                if (token.length() > 0) {
                    char c = token.charAt(0);
                    if (c != ' ') {
                    }
                }
                if (this.mEndPointer > 0 && token.charAt(0) == '.') {
                    this.mDotBitmask |= 1 << (this.mEndPointer - 1);
                } else if (this.mEndPointer > 0 && token.charAt(0) == ',') {
                    this.mCommaBitmask |= 1 << (this.mEndPointer - 1);
                } else {
                    this.mTokens[this.mEndPointer] = token;
                    this.mEndPointer++;
                }
            }
        }

        public boolean hasDot(int index) {
            return (this.mDotBitmask & (1 << index)) != 0;
        }

        public boolean hasComma(int index) {
            return (this.mCommaBitmask & (1 << index)) != 0;
        }
    }

    public NameSplitter(String commonPrefixes, String commonLastNamePrefixes, String commonSuffixes, String commonConjunctions, Locale locale) {
        this.mPrefixesSet = convertToSet(commonPrefixes);
        this.mLastNamePrefixesSet = convertToSet(commonLastNamePrefixes);
        this.mSuffixesSet = convertToSet(commonSuffixes);
        this.mConjuctions = convertToSet(commonConjunctions);
        this.mLocale = locale == null ? Locale.getDefault() : locale;
        this.mLanguage = this.mLocale.getLanguage().toLowerCase();
        int maxLength = 0;
        for (String suffix : this.mSuffixesSet) {
            if (suffix.length() > maxLength) {
                maxLength = suffix.length();
            }
        }
        this.mMaxSuffixLength = maxLength;
    }

    private static HashSet<String> convertToSet(String strings) {
        HashSet<String> set = new HashSet<>();
        if (strings != null) {
            String[] split = strings.split(",");
            for (String str : split) {
                set.add(str.trim().toUpperCase());
            }
        }
        return set;
    }

    public int tokenize(String[] tokens, String fullName) {
        int count = 0;
        if (fullName != null) {
            NameTokenizer tokenizer = new NameTokenizer(fullName);
            if (tokenizer.mStartPointer != tokenizer.mEndPointer) {
                String str = tokenizer.mTokens[tokenizer.mStartPointer];
                count = 0;
                int i = tokenizer.mStartPointer;
                while (i < tokenizer.mEndPointer) {
                    tokens[count] = tokenizer.mTokens[i];
                    i++;
                    count++;
                }
            }
        }
        return count;
    }

    public void split(Name name, String fullName) {
        if (fullName != null) {
            int fullNameStyle = guessFullNameStyle(fullName);
            if (fullNameStyle == 2) {
                fullNameStyle = getAdjustedFullNameStyle(fullNameStyle);
            }
            split(name, fullName, fullNameStyle);
        }
    }

    public void split(Name name, String fullName, int fullNameStyle) {
        if (fullName != null) {
            name.fullNameStyle = fullNameStyle;
            switch (fullNameStyle) {
                case 3:
                    splitChineseName(name, fullName);
                    break;
                case 4:
                    splitJapaneseName(name, fullName);
                    break;
                case 5:
                    splitKoreanName(name, fullName);
                    break;
                default:
                    splitWesternName(name, fullName);
                    break;
            }
        }
    }

    private void splitWesternName(Name name, String fullName) {
        NameTokenizer tokens = new NameTokenizer(fullName);
        parsePrefix(name, tokens);
        if (tokens.mEndPointer > 2) {
            parseSuffix(name, tokens);
        }
        if (name.prefix == null && tokens.mEndPointer - tokens.mStartPointer == 1) {
            name.givenNames = tokens.mTokens[tokens.mStartPointer];
            return;
        }
        parseLastName(name, tokens);
        parseMiddleName(name, tokens);
        parseGivenNames(name, tokens);
    }

    private void splitChineseName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = token;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = token;
            } else if (name.middleName == null) {
                name.middleName = name.givenNames;
                name.givenNames = token;
            } else {
                name.middleName += name.givenNames;
                name.givenNames = token;
            }
        }
        if (name.givenNames != null && name.familyName == null && name.middleName == null) {
            int length = fullName.length();
            if (length == 2) {
                name.familyName = fullName.substring(0, 1);
                name.givenNames = fullName.substring(1);
            } else if (length == 3) {
                name.familyName = fullName.substring(0, 1);
                name.middleName = fullName.substring(1, 2);
                name.givenNames = fullName.substring(2);
            } else if (length == 4) {
                name.familyName = fullName.substring(0, 2);
                name.middleName = fullName.substring(2, 3);
                name.givenNames = fullName.substring(3);
            }
        }
    }

    private void splitJapaneseName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (name.givenNames == null) {
                name.givenNames = token;
            } else if (name.familyName == null) {
                name.familyName = name.givenNames;
                name.givenNames = token;
            } else {
                name.givenNames += " " + token;
            }
        }
    }

    private void splitKoreanName(Name name, String fullName) {
        StringTokenizer tokenizer = new StringTokenizer(fullName);
        if (tokenizer.countTokens() > 1) {
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (name.givenNames == null) {
                    name.givenNames = token;
                } else if (name.familyName == null) {
                    name.familyName = name.givenNames;
                    name.givenNames = token;
                } else {
                    name.givenNames += " " + token;
                }
            }
            return;
        }
        int familyNameLength = 1;
        String[] arr$ = KOREAN_TWO_CHARCTER_FAMILY_NAMES;
        int len$ = arr$.length;
        int i$ = 0;
        while (true) {
            if (i$ >= len$) {
                break;
            }
            String twoLengthFamilyName = arr$[i$];
            if (!fullName.startsWith(twoLengthFamilyName)) {
                i$++;
            } else {
                familyNameLength = 2;
                break;
            }
        }
        name.familyName = fullName.substring(0, familyNameLength);
        if (fullName.length() > familyNameLength) {
            name.givenNames = fullName.substring(familyNameLength);
        }
    }

    public String join(Name name, boolean givenNameFirst, boolean includePrefix) {
        String prefix = includePrefix ? name.prefix : null;
        switch (name.fullNameStyle) {
            case 2:
            case 3:
            case 5:
                return join(prefix, name.familyName, name.middleName, name.givenNames, name.suffix, false, false, false);
            case 4:
                return join(prefix, name.familyName, name.middleName, name.givenNames, name.suffix, true, false, false);
            default:
                if (givenNameFirst) {
                    return join(prefix, name.givenNames, name.middleName, name.familyName, name.suffix, true, false, true);
                }
                return join(prefix, name.familyName, name.givenNames, name.middleName, name.suffix, true, true, true);
        }
    }

    public String joinPhoneticName(Name name) {
        return join(null, name.phoneticFamilyName, name.phoneticMiddleName, name.phoneticGivenName, null, true, false, false);
    }

    private String join(String prefix, String part1, String part2, String part3, String suffix, boolean useSpace, boolean useCommaAfterPart1, boolean useCommaAfterPart3) {
        String prefix2 = prefix == null ? null : prefix.trim();
        String part12 = part1 == null ? null : part1.trim();
        String part22 = part2 == null ? null : part2.trim();
        String part32 = part3 == null ? null : part3.trim();
        String suffix2 = suffix == null ? null : suffix.trim();
        boolean hasPrefix = !TextUtils.isEmpty(prefix2);
        boolean hasPart1 = !TextUtils.isEmpty(part12);
        boolean hasPart2 = !TextUtils.isEmpty(part22);
        boolean hasPart3 = !TextUtils.isEmpty(part32);
        boolean hasSuffix = !TextUtils.isEmpty(suffix2);
        boolean isSingleWord = true;
        String singleWord = null;
        if (hasPrefix) {
            singleWord = prefix2;
        }
        if (hasPart1) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part12;
            }
        }
        if (hasPart2) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part22;
            }
        }
        if (hasPart3) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = part32;
            }
        }
        if (hasSuffix) {
            if (singleWord != null) {
                isSingleWord = false;
            } else {
                singleWord = normalizedSuffix(suffix2);
            }
        }
        if (!isSingleWord) {
            StringBuilder sb = new StringBuilder();
            if (hasPrefix) {
                sb.append(prefix2);
            }
            if (hasPart1) {
                if (hasPrefix) {
                    sb.append(' ');
                }
                sb.append(part12);
            }
            if (hasPart2) {
                if (hasPrefix || hasPart1) {
                    if (useCommaAfterPart1) {
                        sb.append(',');
                    }
                    if (useSpace) {
                        sb.append(' ');
                    }
                }
                sb.append(part22);
            }
            if (hasPart3) {
                if ((hasPrefix || hasPart1 || hasPart2) && useSpace) {
                    sb.append(' ');
                }
                sb.append(part32);
            }
            if (hasSuffix) {
                if (hasPrefix || hasPart1 || hasPart2 || hasPart3) {
                    if (useCommaAfterPart3) {
                        sb.append(',');
                    }
                    if (useSpace) {
                        sb.append(' ');
                    }
                }
                sb.append(normalizedSuffix(suffix2));
            }
            String singleWord2 = sb.toString();
            return singleWord2;
        }
        return singleWord;
    }

    private String normalizedSuffix(String suffix) {
        int length = suffix.length();
        if (length != 0 && suffix.charAt(length - 1) != '.') {
            String withDot = suffix + '.';
            return this.mSuffixesSet.contains(withDot.toUpperCase()) ? withDot : suffix;
        }
        return suffix;
    }

    public int getAdjustedFullNameStyle(int nameStyle) {
        if (nameStyle == 0) {
            if (JAPANESE_LANGUAGE.equals(this.mLanguage)) {
                return 4;
            }
            if (KOREAN_LANGUAGE.equals(this.mLanguage)) {
                return 5;
            }
            return CHINESE_LANGUAGE.equals(this.mLanguage) ? 3 : 1;
        }
        if (nameStyle == 2) {
            if (JAPANESE_LANGUAGE.equals(this.mLanguage)) {
                return 4;
            }
            return KOREAN_LANGUAGE.equals(this.mLanguage) ? 5 : 3;
        }
        return nameStyle;
    }

    private void parsePrefix(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer != tokens.mEndPointer) {
            String firstToken = tokens.mTokens[tokens.mStartPointer];
            if (this.mPrefixesSet.contains(firstToken.toUpperCase())) {
                if (tokens.hasDot(tokens.mStartPointer)) {
                    firstToken = firstToken + '.';
                }
                name.prefix = firstToken;
                NameTokenizer.access$008(tokens);
            }
        }
    }

    private void parseSuffix(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer != tokens.mEndPointer) {
            String lastToken = tokens.mTokens[tokens.mEndPointer - 1];
            if (tokens.mEndPointer - tokens.mStartPointer > 2 && tokens.hasComma(tokens.mEndPointer - 2)) {
                if (tokens.hasDot(tokens.mEndPointer - 1)) {
                    lastToken = lastToken + '.';
                }
                name.suffix = lastToken;
                NameTokenizer.access$110(tokens);
                return;
            }
            if (lastToken.length() <= this.mMaxSuffixLength) {
                String normalized = lastToken.toUpperCase();
                if (!this.mSuffixesSet.contains(normalized)) {
                    if (tokens.hasDot(tokens.mEndPointer - 1)) {
                        lastToken = lastToken + '.';
                    }
                    String normalized2 = normalized + ".";
                    int pos = tokens.mEndPointer - 1;
                    while (normalized2.length() <= this.mMaxSuffixLength) {
                        if (!this.mSuffixesSet.contains(normalized2)) {
                            if (pos != tokens.mStartPointer) {
                                pos--;
                                lastToken = tokens.hasDot(pos) ? tokens.mTokens[pos] + "." + lastToken : tokens.mTokens[pos] + " " + lastToken;
                                normalized2 = tokens.mTokens[pos].toUpperCase() + "." + normalized2;
                            } else {
                                return;
                            }
                        } else {
                            name.suffix = lastToken;
                            tokens.mEndPointer = pos;
                            return;
                        }
                    }
                    return;
                }
                name.suffix = lastToken;
                NameTokenizer.access$110(tokens);
            }
        }
    }

    private void parseLastName(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer != tokens.mEndPointer) {
            if (tokens.hasComma(tokens.mStartPointer)) {
                name.familyName = tokens.mTokens[tokens.mStartPointer];
                NameTokenizer.access$008(tokens);
                return;
            }
            if (tokens.mStartPointer + 1 >= tokens.mEndPointer || !tokens.hasComma(tokens.mStartPointer + 1) || !isFamilyNamePrefix(tokens.mTokens[tokens.mStartPointer])) {
                name.familyName = tokens.mTokens[tokens.mEndPointer - 1];
                NameTokenizer.access$110(tokens);
                if (tokens.mEndPointer - tokens.mStartPointer > 0) {
                    String lastNamePrefix = tokens.mTokens[tokens.mEndPointer - 1];
                    if (isFamilyNamePrefix(lastNamePrefix)) {
                        if (tokens.hasDot(tokens.mEndPointer - 1)) {
                            lastNamePrefix = lastNamePrefix + '.';
                        }
                        name.familyName = lastNamePrefix + " " + name.familyName;
                        NameTokenizer.access$110(tokens);
                        return;
                    }
                    return;
                }
                return;
            }
            String familyNamePrefix = tokens.mTokens[tokens.mStartPointer];
            if (tokens.hasDot(tokens.mStartPointer)) {
                familyNamePrefix = familyNamePrefix + '.';
            }
            name.familyName = familyNamePrefix + " " + tokens.mTokens[tokens.mStartPointer + 1];
            NameTokenizer.access$012(tokens, 2);
        }
    }

    private boolean isFamilyNamePrefix(String word) {
        String normalized = word.toUpperCase();
        return this.mLastNamePrefixesSet.contains(normalized) || this.mLastNamePrefixesSet.contains(new StringBuilder().append(normalized).append(".").toString());
    }

    private void parseMiddleName(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer != tokens.mEndPointer && tokens.mEndPointer - tokens.mStartPointer > 1) {
            if (tokens.mEndPointer - tokens.mStartPointer == 2 || !this.mConjuctions.contains(tokens.mTokens[tokens.mEndPointer - 2].toUpperCase())) {
                name.middleName = tokens.mTokens[tokens.mEndPointer - 1];
                if (tokens.hasDot(tokens.mEndPointer - 1)) {
                    name.middleName += '.';
                }
                NameTokenizer.access$110(tokens);
            }
        }
    }

    private void parseGivenNames(Name name, NameTokenizer tokens) {
        if (tokens.mStartPointer != tokens.mEndPointer) {
            if (tokens.mEndPointer - tokens.mStartPointer == 1) {
                name.givenNames = tokens.mTokens[tokens.mStartPointer];
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = tokens.mStartPointer; i < tokens.mEndPointer; i++) {
                if (i != tokens.mStartPointer) {
                    sb.append(' ');
                }
                sb.append(tokens.mTokens[i]);
                if (tokens.hasDot(i)) {
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

    public int getAdjustedNameStyleBasedOnPhoneticNameStyle(int nameStyle, int phoneticNameStyle) {
        if (phoneticNameStyle != 0) {
            if (nameStyle == 0 || nameStyle == 2) {
                if (phoneticNameStyle == 4) {
                    return 4;
                }
                if (phoneticNameStyle == 5) {
                    return 5;
                }
                if (nameStyle == 2 && phoneticNameStyle == 3) {
                    return 3;
                }
                return nameStyle;
            }
            return nameStyle;
        }
        return nameStyle;
    }

    private void guessFullNameStyle(Name name) {
        if (name.fullNameStyle == 0) {
            int bestGuess = guessFullNameStyle(name.givenNames);
            if (bestGuess != 0 && bestGuess != 2 && bestGuess != 1) {
                name.fullNameStyle = bestGuess;
                return;
            }
            int guess = guessFullNameStyle(name.familyName);
            if (guess != 0) {
                if (guess != 2 && guess != 1) {
                    name.fullNameStyle = guess;
                    return;
                }
                bestGuess = guess;
            }
            int guess2 = guessFullNameStyle(name.middleName);
            if (guess2 != 0) {
                if (guess2 != 2 && guess2 != 1) {
                    name.fullNameStyle = guess2;
                    return;
                }
                bestGuess = guess2;
            }
            int guess3 = guessFullNameStyle(name.prefix);
            if (guess3 != 0) {
                if (guess3 != 2 && guess3 != 1) {
                    name.fullNameStyle = guess3;
                    return;
                }
                bestGuess = guess3;
            }
            int guess4 = guessFullNameStyle(name.suffix);
            if (guess4 != 0) {
                if (guess4 != 2 && guess4 != 1) {
                    name.fullNameStyle = guess4;
                    return;
                }
                bestGuess = guess4;
            }
            name.fullNameStyle = bestGuess;
        }
    }

    public int guessFullNameStyle(String name) {
        if (name == null) {
            return 0;
        }
        int nameStyle = 0;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(codePoint);
                if (!isLatinUnicodeBlock(unicodeBlock)) {
                    if (isCJKUnicodeBlock(unicodeBlock)) {
                        int nameStyle2 = guessCJKNameStyle(name, Character.charCount(codePoint) + offset);
                        return nameStyle2;
                    }
                    if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                        return 4;
                    }
                    if (isKoreanUnicodeBlock(unicodeBlock)) {
                        return 5;
                    }
                }
                nameStyle = 1;
            }
            offset += Character.charCount(codePoint);
        }
        return nameStyle;
    }

    private int guessCJKNameStyle(String name, int offset) {
        int length = name.length();
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return 4;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return 5;
                }
            }
            offset += Character.charCount(codePoint);
        }
        return 2;
    }

    private void guessPhoneticNameStyle(Name name) {
        if (name.phoneticNameStyle == 0) {
            int bestGuess = guessPhoneticNameStyle(name.phoneticFamilyName);
            if (bestGuess != 0 && bestGuess != 2) {
                name.phoneticNameStyle = bestGuess;
                return;
            }
            int guess = guessPhoneticNameStyle(name.phoneticGivenName);
            if (guess != 0 && guess != 2) {
                name.phoneticNameStyle = guess;
                return;
            }
            int guess2 = guessPhoneticNameStyle(name.phoneticMiddleName);
            if (guess2 != 0 && guess2 != 2) {
                name.phoneticNameStyle = guess2;
            }
        }
    }

    public int guessPhoneticNameStyle(String name) {
        if (name == null) {
            return 0;
        }
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return 4;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return 5;
                }
                if (isLatinUnicodeBlock(unicodeBlock)) {
                    return 3;
                }
            }
            offset += Character.charCount(codePoint);
        }
        return 0;
    }

    private static boolean isLatinUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.BASIC_LATIN || unicodeBlock == Character.UnicodeBlock.LATIN_1_SUPPLEMENT || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_A || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_B || unicodeBlock == Character.UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(Character.UnicodeBlock block) {
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || block == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT || block == Character.UnicodeBlock.CJK_COMPATIBILITY || block == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.HANGUL_SYLLABLES || unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO || unicodeBlock == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(Character.UnicodeBlock unicodeBlock) {
        return unicodeBlock == Character.UnicodeBlock.KATAKANA || unicodeBlock == Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS || unicodeBlock == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS || unicodeBlock == Character.UnicodeBlock.HIRAGANA;
    }
}
