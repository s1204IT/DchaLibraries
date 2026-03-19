package android.icu.text;

import android.icu.impl.PatternTokenizer;
import android.icu.impl.Utility;
import android.icu.lang.UCharacter;
import android.icu.text.Transliterator;
import android.icu.util.ULocale;

class UnescapeTransliterator extends Transliterator {
    private static final char END = 65535;
    private char[] spec;

    static void register() {
        Transliterator.registerFactory("Hex-Any/Unicode", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/Unicode", new char[]{2, 0, 16, 4, 6, 'U', '+', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Java", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/Java", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/C", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/C", new char[]{2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, '\b', '\b', PatternTokenizer.BACK_SLASH, 'U', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/XML", new char[]{3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/XML10", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/XML10", new char[]{2, 1, '\n', 1, 7, '&', '#', ';', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any/Perl", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any/Perl", new char[]{3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
        Transliterator.registerFactory("Hex-Any", new Transliterator.Factory() {
            @Override
            public Transliterator getInstance(String str) {
                return new UnescapeTransliterator("Hex-Any", new char[]{2, 0, 16, 4, 6, 'U', '+', 2, 0, 16, 4, 4, PatternTokenizer.BACK_SLASH, 'u', 2, 0, 16, '\b', '\b', PatternTokenizer.BACK_SLASH, 'U', 3, 1, 16, 1, 6, '&', '#', ULocale.PRIVATE_USE_EXTENSION, ';', 2, 1, '\n', 1, 7, '&', '#', ';', 3, 1, 16, 1, 6, PatternTokenizer.BACK_SLASH, ULocale.PRIVATE_USE_EXTENSION, '{', '}', 65535});
            }
        });
    }

    UnescapeTransliterator(String str, char[] cArr) {
        super(str, null);
        this.spec = cArr;
    }

    @Override
    protected void handleTransliterate(Replaceable replaceable, Transliterator.Position position, boolean z) {
        boolean z2;
        int charCount = position.start;
        int length = position.limit;
        loop0: while (charCount < length) {
            int i = 0;
            while (true) {
                if (this.spec[i] == 65535) {
                    break;
                }
                int i2 = i + 1;
                char c = this.spec[i];
                int i3 = i2 + 1;
                char c2 = this.spec[i2];
                int i4 = i3 + 1;
                char c3 = this.spec[i3];
                int i5 = i4 + 1;
                char c4 = this.spec[i4];
                int i6 = i5 + 1;
                char c5 = this.spec[i5];
                int charCount2 = charCount;
                int i7 = 0;
                while (i7 < c) {
                    if (charCount2 >= length && i7 > 0) {
                        if (z) {
                            break loop0;
                        }
                    } else {
                        int i8 = charCount2 + 1;
                        if (replaceable.charAt(charCount2) == this.spec[i6 + i7]) {
                            i7++;
                            charCount2 = i8;
                        } else {
                            charCount2 = i8;
                        }
                    }
                    z2 = false;
                    break;
                }
                z2 = true;
                if (z2) {
                    int i9 = 0;
                    int i10 = 0;
                    while (true) {
                        if (charCount2 >= length) {
                            if (charCount2 > charCount && z) {
                                break loop0;
                            } else {
                                break;
                            }
                        }
                        int iChar32At = replaceable.char32At(charCount2);
                        int iDigit = UCharacter.digit(iChar32At, c3);
                        if (iDigit < 0) {
                            break;
                        }
                        charCount2 += UTF16.getCharCount(iChar32At);
                        i9 = (i9 * c3) + iDigit;
                        i10++;
                        if (i10 == c5) {
                            break;
                        }
                    }
                    boolean z3 = i10 >= c4;
                    if (z3) {
                        int i11 = 0;
                        while (i11 < c2) {
                            if (charCount2 >= length) {
                                if (charCount2 > charCount && z) {
                                    break loop0;
                                }
                            } else {
                                int i12 = charCount2 + 1;
                                if (replaceable.charAt(charCount2) == this.spec[i6 + c + i11]) {
                                    i11++;
                                    charCount2 = i12;
                                } else {
                                    charCount2 = i12;
                                }
                            }
                            z3 = false;
                            break;
                        }
                        if (z3) {
                            String strValueOf = UTF16.valueOf(i9);
                            replaceable.replace(charCount, charCount2, strValueOf);
                            length -= (charCount2 - charCount) - strValueOf.length();
                            break;
                        }
                    } else {
                        continue;
                    }
                }
                i = c + c2 + i6;
            }
            if (charCount < length) {
                charCount += UTF16.getCharCount(replaceable.char32At(charCount));
            }
        }
        position.contextLimit += length - position.limit;
        position.limit = length;
        position.start = charCount;
    }

    @Override
    public void addSourceTargetSet(UnicodeSet unicodeSet, UnicodeSet unicodeSet2, UnicodeSet unicodeSet3) {
        UnicodeSet filterAsUnicodeSet = getFilterAsUnicodeSet(unicodeSet);
        UnicodeSet unicodeSet4 = new UnicodeSet();
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (this.spec[i] != 65535) {
            int i2 = this.spec[i] + i + this.spec[i + 1] + 5;
            char c = this.spec[i + 2];
            for (int i3 = 0; i3 < c; i3++) {
                Utility.appendNumber(sb, i3, c, 0);
            }
            for (int i4 = i + 5; i4 < i2; i4++) {
                unicodeSet4.add(this.spec[i4]);
            }
            i = i2;
        }
        unicodeSet4.addAll(sb.toString());
        unicodeSet4.retainAll(filterAsUnicodeSet);
        if (unicodeSet4.size() > 0) {
            unicodeSet2.addAll(unicodeSet4);
            unicodeSet3.addAll(0, 1114111);
        }
    }
}
