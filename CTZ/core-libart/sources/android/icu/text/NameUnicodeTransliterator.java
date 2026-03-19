package android.icu.text;

import android.icu.impl.PatternProps;
import android.icu.impl.UCharacterName;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;

class NameUnicodeTransliterator extends Transliterator {
    static final char CLOSE_DELIM = '}';
    static final char OPEN_DELIM = '\\';
    static final String OPEN_PAT = "\\N~{~";
    static final char SPACE = ' ';
    static final String _ID = "Name-Any";

    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new NameUnicodeTransliterator(null);
            }
        });
    }

    public NameUnicodeTransliterator(UnicodeFilter unicodeFilter) {
        super(_ID, unicodeFilter);
    }

    @Override
    protected void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        int maxCharNameLength = UCharacterName.INSTANCE.getMaxCharNameLength() + 1;
        StringBuffer stringBuffer = new StringBuffer(maxCharNameLength);
        UnicodeSet unicodeSet = new UnicodeSet();
        UCharacterName.INSTANCE.getCharNameCharacters(unicodeSet);
        int charCount = position.start;
        int i = position.limit;
        while (true) {
            int i2 = -1;
            char c = 0;
            while (charCount < i) {
                int iChar32At = replaceable.char32At(charCount);
                switch (c) {
                    case 0:
                        if (iChar32At == 92) {
                            int pattern = Utility.parsePattern(OPEN_PAT, replaceable, charCount, i);
                            if (pattern >= 0 && pattern < i) {
                                stringBuffer.setLength(0);
                                c = 1;
                                i2 = charCount;
                                charCount = pattern;
                            } else {
                                i2 = charCount;
                            }
                            break;
                        }
                        charCount += UTF16.getCharCount(iChar32At);
                        break;
                    case 1:
                        if (PatternProps.isWhiteSpace(iChar32At)) {
                            if (stringBuffer.length() > 0 && stringBuffer.charAt(stringBuffer.length() - 1) != ' ') {
                                stringBuffer.append(SPACE);
                                if (stringBuffer.length() > maxCharNameLength) {
                                    c = 0;
                                }
                            }
                            charCount += UTF16.getCharCount(iChar32At);
                        } else if (iChar32At == 125) {
                            int length = stringBuffer.length();
                            if (length > 0 && stringBuffer.charAt(length - 1) == ' ') {
                                stringBuffer.setLength(length - 1);
                            }
                            int charFromExtendedName = UCharacter.getCharFromExtendedName(stringBuffer.toString());
                            if (charFromExtendedName != -1) {
                                int i3 = charCount + 1;
                                String strValueOf = UTF16.valueOf(charFromExtendedName);
                                replaceable.replace(i2, i3, strValueOf);
                                int length2 = (i3 - i2) - strValueOf.length();
                                charCount = i3 - length2;
                                i -= length2;
                            }
                        } else {
                            if (unicodeSet.contains(iChar32At)) {
                                UTF16.append(stringBuffer, iChar32At);
                                if (stringBuffer.length() >= maxCharNameLength) {
                                }
                                charCount += UTF16.getCharCount(iChar32At);
                            } else {
                                charCount--;
                            }
                            c = 0;
                            charCount += UTF16.getCharCount(iChar32At);
                        }
                        break;
                    default:
                        charCount += UTF16.getCharCount(iChar32At);
                        break;
                }
                while (charCount < i) {
                }
            }
            position.contextLimit += i - position.limit;
            position.limit = i;
            if (!z || i2 < 0) {
                i2 = charCount;
            }
            position.start = i2;
            return;
        }
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        UnicodeSet filterAsUnicodeSet = getFilterAsUnicodeSet(unicodeSet);
        if (!filterAsUnicodeSet.containsAll("\\N{") || !filterAsUnicodeSet.contains(125)) {
            return;
        }
        UnicodeSet unicodeSetAdd = new UnicodeSet().addAll(48, 57).addAll(65, 70).addAll(97, 122).add(60).add(62).add(40).add(41).add(45).add(32).addAll("\\N{").add(125);
        unicodeSetAdd.retainAll(filterAsUnicodeSet);
        if (unicodeSetAdd.size() > 0) {
            unicodeSet2.addAll(unicodeSetAdd);
            unicodeSet3.addAll(0, 1114111);
        }
    }
}
