package com.google.common.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CharMatcher {
    public static final CharMatcher ANY;
    public static final CharMatcher DIGIT;
    public static final CharMatcher INVISIBLE;
    public static final CharMatcher JAVA_DIGIT;
    public static final CharMatcher JAVA_ISO_CONTROL;
    public static final CharMatcher JAVA_LETTER;
    public static final CharMatcher JAVA_LETTER_OR_DIGIT;
    public static final CharMatcher JAVA_LOWER_CASE;
    public static final CharMatcher JAVA_UPPER_CASE;
    public static final CharMatcher NONE;
    public static final CharMatcher SINGLE_WIDTH;
    public static final CharMatcher WHITESPACE = anyOf("\t\n\u000b\f\r \u0085\u1680\u2028\u2029\u205f\u3000 \u180e ").or(inRange(8192, 8202)).precomputed();
    public static final CharMatcher BREAKING_WHITESPACE = anyOf("\t\n\u000b\f\r \u0085\u1680\u2028\u2029\u205f\u3000").or(inRange(8192, 8198)).or(inRange(8200, 8202)).precomputed();
    public static final CharMatcher ASCII = inRange(0, 127);

    public abstract boolean matches(char c);

    static {
        CharMatcher digit = inRange('0', '9');
        char[] arr$ = "٠۰߀०০੦૦୦௦౦೦൦๐໐༠၀႐០᠐᥆᧐᭐᮰᱀᱐꘠꣐꤀꩐０".toCharArray();
        for (char base : arr$) {
            digit = digit.or(inRange(base, (char) (base + '\t')));
        }
        DIGIT = digit.precomputed();
        JAVA_DIGIT = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Character.isDigit(c);
            }
        };
        JAVA_LETTER = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Character.isLetter(c);
            }
        };
        JAVA_LETTER_OR_DIGIT = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Character.isLetterOrDigit(c);
            }
        };
        JAVA_UPPER_CASE = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Character.isUpperCase(c);
            }
        };
        JAVA_LOWER_CASE = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return Character.isLowerCase(c);
            }
        };
        JAVA_ISO_CONTROL = inRange((char) 0, (char) 31).or(inRange((char) 127, (char) 159));
        INVISIBLE = inRange((char) 0, ' ').or(inRange((char) 127, (char) 160)).or(is((char) 173)).or(inRange((char) 1536, (char) 1539)).or(anyOf("\u06dd\u070f\u1680឴឵\u180e")).or(inRange((char) 8192, (char) 8207)).or(inRange((char) 8232, (char) 8239)).or(inRange((char) 8287, (char) 8292)).or(inRange((char) 8298, (char) 8303)).or(is((char) 12288)).or(inRange((char) 55296, (char) 63743)).or(anyOf("\ufeff\ufff9\ufffa\ufffb")).precomputed();
        SINGLE_WIDTH = inRange((char) 0, (char) 1273).or(is((char) 1470)).or(inRange((char) 1488, (char) 1514)).or(is((char) 1523)).or(is((char) 1524)).or(inRange((char) 1536, (char) 1791)).or(inRange((char) 1872, (char) 1919)).or(inRange((char) 3584, (char) 3711)).or(inRange((char) 7680, (char) 8367)).or(inRange((char) 8448, (char) 8506)).or(inRange((char) 64336, (char) 65023)).or(inRange((char) 65136, (char) 65279)).or(inRange((char) 65377, (char) 65500)).precomputed();
        ANY = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return true;
            }

            @Override
            public int indexIn(CharSequence sequence) {
                return sequence.length() == 0 ? -1 : 0;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                Preconditions.checkNotNull(other);
                return this;
            }

            @Override
            public CharMatcher negate() {
                return NONE;
            }

            @Override
            public CharMatcher precomputed() {
                return this;
            }
        };
        NONE = new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return false;
            }

            @Override
            public int indexIn(CharSequence sequence) {
                Preconditions.checkNotNull(sequence);
                return -1;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                return (CharMatcher) Preconditions.checkNotNull(other);
            }

            @Override
            public CharMatcher negate() {
                return ANY;
            }

            @Override
            void setBits(LookupTable table) {
            }

            @Override
            public CharMatcher precomputed() {
                return this;
            }
        };
    }

    public static CharMatcher is(final char match) {
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return c == match;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? other : super.or(other);
            }

            @Override
            public CharMatcher negate() {
                return isNot(match);
            }

            @Override
            void setBits(LookupTable table) {
                table.set(match);
            }

            @Override
            public CharMatcher precomputed() {
                return this;
            }
        };
    }

    public static CharMatcher isNot(final char match) {
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return c != match;
            }

            @Override
            public CharMatcher or(CharMatcher other) {
                return other.matches(match) ? ANY : this;
            }

            @Override
            public CharMatcher negate() {
                return is(match);
            }
        };
    }

    public static CharMatcher anyOf(CharSequence sequence) {
        switch (sequence.length()) {
            case 0:
                return NONE;
            case 1:
                return is(sequence.charAt(0));
            case 2:
                final char match1 = sequence.charAt(0);
                final char match2 = sequence.charAt(1);
                return new CharMatcher() {
                    @Override
                    public boolean matches(char c) {
                        return c == match1 || c == match2;
                    }

                    @Override
                    void setBits(LookupTable table) {
                        table.set(match1);
                        table.set(match2);
                    }

                    @Override
                    public CharMatcher precomputed() {
                        return this;
                    }
                };
            default:
                final char[] chars = sequence.toString().toCharArray();
                Arrays.sort(chars);
                return new CharMatcher() {
                    @Override
                    public boolean matches(char c) {
                        return Arrays.binarySearch(chars, c) >= 0;
                    }

                    @Override
                    void setBits(LookupTable table) {
                        char[] arr$ = chars;
                        for (char c : arr$) {
                            table.set(c);
                        }
                    }
                };
        }
    }

    public static CharMatcher inRange(final char startInclusive, final char endInclusive) {
        Preconditions.checkArgument(endInclusive >= startInclusive);
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return startInclusive <= c && c <= endInclusive;
            }

            @Override
            void setBits(LookupTable table) {
                char c = startInclusive;
                while (true) {
                    table.set(c);
                    char c2 = (char) (c + 1);
                    if (c == endInclusive) {
                        return;
                    } else {
                        c = c2;
                    }
                }
            }

            @Override
            public CharMatcher precomputed() {
                return this;
            }
        };
    }

    protected CharMatcher() {
    }

    public CharMatcher negate() {
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return !this.matches(c);
            }

            @Override
            public CharMatcher negate() {
                return this;
            }
        };
    }

    public CharMatcher or(CharMatcher other) {
        return new Or(Arrays.asList(this, (CharMatcher) Preconditions.checkNotNull(other)));
    }

    private static class Or extends CharMatcher {
        List<CharMatcher> components;

        Or(List<CharMatcher> components) {
            this.components = components;
        }

        @Override
        public boolean matches(char c) {
            for (CharMatcher matcher : this.components) {
                if (matcher.matches(c)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public CharMatcher or(CharMatcher other) {
            ArrayList arrayList = new ArrayList(this.components);
            arrayList.add(Preconditions.checkNotNull(other));
            return new Or(arrayList);
        }

        @Override
        void setBits(LookupTable table) {
            for (CharMatcher matcher : this.components) {
                matcher.setBits(table);
            }
        }
    }

    public CharMatcher precomputed() {
        return Platform.precomputeCharMatcher(this);
    }

    CharMatcher precomputedInternal() {
        final LookupTable table = new LookupTable();
        setBits(table);
        return new CharMatcher() {
            @Override
            public boolean matches(char c) {
                return table.get(c);
            }

            @Override
            public CharMatcher precomputed() {
                return this;
            }
        };
    }

    void setBits(LookupTable table) {
        char c = 0;
        while (true) {
            if (matches(c)) {
                table.set(c);
            }
            char c2 = (char) (c + 1);
            if (c == 65535) {
                return;
            } else {
                c = c2;
            }
        }
    }

    private static final class LookupTable {
        int[] data;

        private LookupTable() {
            this.data = new int[2048];
        }

        void set(char index) {
            int[] iArr = this.data;
            int i = index >> 5;
            iArr[i] = iArr[i] | (1 << index);
        }

        boolean get(char index) {
            return (this.data[index >> 5] & (1 << index)) != 0;
        }
    }

    public int indexIn(CharSequence sequence) {
        int length = sequence.length();
        for (int i = 0; i < length; i++) {
            if (matches(sequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    public String trimLeadingFrom(CharSequence sequence) {
        int len = sequence.length();
        int first = 0;
        while (first < len && matches(sequence.charAt(first))) {
            first++;
        }
        return sequence.subSequence(first, len).toString();
    }

    public String trimAndCollapseFrom(CharSequence sequence, char replacement) {
        int first = negate().indexIn(sequence);
        if (first == -1) {
            return "";
        }
        StringBuilder builder = new StringBuilder(sequence.length());
        boolean inMatchingGroup = false;
        for (int i = first; i < sequence.length(); i++) {
            char c = sequence.charAt(i);
            if (apply(Character.valueOf(c))) {
                inMatchingGroup = true;
            } else {
                if (inMatchingGroup) {
                    builder.append(replacement);
                    inMatchingGroup = false;
                }
                builder.append(c);
            }
        }
        return builder.toString();
    }

    public boolean apply(Character character) {
        return matches(character.charValue());
    }
}
