package com.google.common.base;

import java.util.Arrays;

public abstract class CharMatcher implements Predicate<Character> {
    public static final CharMatcher ANY;
    public static final CharMatcher DIGIT;
    public static final CharMatcher INVISIBLE;
    public static final CharMatcher JAVA_DIGIT;
    public static final CharMatcher JAVA_ISO_CONTROL;
    public static final CharMatcher JAVA_LETTER;
    public static final CharMatcher JAVA_LETTER_OR_DIGIT;
    public static final CharMatcher JAVA_LOWER_CASE;
    public static final CharMatcher JAVA_UPPER_CASE;
    private static final String NINES;
    public static final CharMatcher NONE;
    public static final CharMatcher SINGLE_WIDTH;
    public static final CharMatcher WHITESPACE;
    static final int WHITESPACE_SHIFT;
    final String description;
    public static final CharMatcher BREAKING_WHITESPACE = new CharMatcher() {
        @Override
        public boolean matches(char c) {
            if (c != ' ' && c != 133 && c != 5760) {
                if (c == 8199) {
                    return false;
                }
                if (c != 8287 && c != 12288) {
                    switch (c) {
                        default:
                            switch (c) {
                                case 8232:
                                case 8233:
                                    break;
                                default:
                                    if (c < 8192 || c > 8202) {
                                    }
                                    break;
                            }
                            return false;
                        case '\t':
                        case '\n':
                        case 11:
                        case '\f':
                        case '\r':
                            return true;
                    }
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return "CharMatcher.BREAKING_WHITESPACE";
        }
    };
    public static final CharMatcher ASCII = inRange(0, 127, "CharMatcher.ASCII");

    public abstract boolean matches(char c);

    static {
        StringBuilder sb = new StringBuilder("0٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０".length());
        for (int i = 0; i < "0٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０".length(); i++) {
            sb.append((char) ("0٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０".charAt(i) + '\t'));
        }
        NINES = sb.toString();
        DIGIT = new RangesMatcher("CharMatcher.DIGIT", "0٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０".toCharArray(), NINES.toCharArray());
        JAVA_DIGIT = new CharMatcher("CharMatcher.JAVA_DIGIT") {
            @Override
            public boolean matches(char c) {
                return Character.isDigit(c);
            }
        };
        JAVA_LETTER = new CharMatcher("CharMatcher.JAVA_LETTER") {
            @Override
            public boolean matches(char c) {
                return Character.isLetter(c);
            }
        };
        JAVA_LETTER_OR_DIGIT = new CharMatcher("CharMatcher.JAVA_LETTER_OR_DIGIT") {
            @Override
            public boolean matches(char c) {
                return Character.isLetterOrDigit(c);
            }
        };
        JAVA_UPPER_CASE = new CharMatcher("CharMatcher.JAVA_UPPER_CASE") {
            @Override
            public boolean matches(char c) {
                return Character.isUpperCase(c);
            }
        };
        JAVA_LOWER_CASE = new CharMatcher("CharMatcher.JAVA_LOWER_CASE") {
            @Override
            public boolean matches(char c) {
                return Character.isLowerCase(c);
            }
        };
        JAVA_ISO_CONTROL = inRange((char) 0, (char) 31).or(inRange((char) 127, (char) 159)).withToString("CharMatcher.JAVA_ISO_CONTROL");
        INVISIBLE = new RangesMatcher("CharMatcher.INVISIBLE", "\u0000\u007f\u00ad\u0600\u061c\u06dd\u070f\u1680\u180e\u2000\u2028\u205f\u2066\u2067\u2068\u2069\u206a\u3000\ud800\ufeff\ufff9\ufffa".toCharArray(), "  \u00ad\u0604\u061c\u06dd\u070f\u1680\u180e\u200f \u2064\u2066\u2067\u2068\u2069\u206f\u3000\uf8ff\ufeff\ufff9\ufffb".toCharArray());
        SINGLE_WIDTH = new RangesMatcher("CharMatcher.SINGLE_WIDTH", "\u0000־א׳\u0600ݐ\u0e00Ḁ℀ﭐﹰ｡".toCharArray(), "ӹ־ת״ۿݿ\u0e7f₯℺﷿\ufeffￜ".toCharArray());
        ANY = new FastMatcher("CharMatcher.ANY") {
            @Override
            public boolean matches(char c) {
                return true;
            }

            @Override
            public CharMatcher or(CharMatcher charMatcher) {
                Preconditions.checkNotNull(charMatcher);
                return this;
            }
        };
        NONE = new FastMatcher("CharMatcher.NONE") {
            @Override
            public boolean matches(char c) {
                return false;
            }

            @Override
            public CharMatcher or(CharMatcher charMatcher) {
                return (CharMatcher) Preconditions.checkNotNull(charMatcher);
            }
        };
        WHITESPACE_SHIFT = Integer.numberOfLeadingZeros("\u2002\u3000\r\u0085\u200a\u2005\u2000\u3000\u2029\u000b\u3000\u2008\u2003\u205f\u3000\u1680\t \u2006\u2001  \f\u2009\u3000\u2004\u3000\u3000\u2028\n \u3000".length() - 1);
        WHITESPACE = new FastMatcher("WHITESPACE") {
            @Override
            public boolean matches(char c) {
                return "\u2002\u3000\r\u0085\u200a\u2005\u2000\u3000\u2029\u000b\u3000\u2008\u2003\u205f\u3000\u1680\t \u2006\u2001  \f\u2009\u3000\u2004\u3000\u3000\u2028\n \u3000".charAt((48906 * c) >>> WHITESPACE_SHIFT) == c;
            }
        };
    }

    private static class RangesMatcher extends CharMatcher {
        private final char[] rangeEnds;
        private final char[] rangeStarts;

        RangesMatcher(String str, char[] cArr, char[] cArr2) {
            super(str);
            this.rangeStarts = cArr;
            this.rangeEnds = cArr2;
            Preconditions.checkArgument(cArr.length == cArr2.length);
            int i = 0;
            while (i < cArr.length) {
                Preconditions.checkArgument(cArr[i] <= cArr2[i]);
                int i2 = i + 1;
                if (i2 < cArr.length) {
                    Preconditions.checkArgument(cArr2[i] < cArr[i2]);
                }
                i = i2;
            }
        }

        @Override
        public boolean matches(char c) {
            int iBinarySearch = Arrays.binarySearch(this.rangeStarts, c);
            if (iBinarySearch >= 0) {
                return true;
            }
            int i = (~iBinarySearch) - 1;
            return i >= 0 && c <= this.rangeEnds[i];
        }
    }

    private static String showCharacter(char c) {
        char[] cArr = {'\\', 'u', 0, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            cArr[5 - i] = "0123456789ABCDEF".charAt(c & 15);
            c = (char) (c >> 4);
        }
        return String.copyValueOf(cArr);
    }

    public static CharMatcher inRange(char c, char c2) {
        Preconditions.checkArgument(c2 >= c);
        return inRange(c, c2, "CharMatcher.inRange('" + showCharacter(c) + "', '" + showCharacter(c2) + "')");
    }

    static CharMatcher inRange(final char c, final char c2, String str) {
        return new FastMatcher(str) {
            @Override
            public boolean matches(char c3) {
                return c <= c3 && c3 <= c2;
            }
        };
    }

    CharMatcher(String str) {
        this.description = str;
    }

    protected CharMatcher() {
        this.description = super.toString();
    }

    public CharMatcher or(CharMatcher charMatcher) {
        return new Or(this, (CharMatcher) Preconditions.checkNotNull(charMatcher));
    }

    private static class Or extends CharMatcher {
        final CharMatcher first;
        final CharMatcher second;

        Or(CharMatcher charMatcher, CharMatcher charMatcher2, String str) {
            super(str);
            this.first = (CharMatcher) Preconditions.checkNotNull(charMatcher);
            this.second = (CharMatcher) Preconditions.checkNotNull(charMatcher2);
        }

        Or(CharMatcher charMatcher, CharMatcher charMatcher2) {
            this(charMatcher, charMatcher2, "CharMatcher.or(" + charMatcher + ", " + charMatcher2 + ")");
        }

        @Override
        public boolean matches(char c) {
            return this.first.matches(c) || this.second.matches(c);
        }

        @Override
        CharMatcher withToString(String str) {
            return new Or(this.first, this.second, str);
        }
    }

    CharMatcher withToString(String str) {
        throw new UnsupportedOperationException();
    }

    static abstract class FastMatcher extends CharMatcher {
        FastMatcher(String str) {
            super(str);
        }
    }

    @Override
    @Deprecated
    public boolean apply(Character ch) {
        return matches(ch.charValue());
    }

    public String toString() {
        return this.description;
    }
}
