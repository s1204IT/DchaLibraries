package com.google.common.base;

import java.util.Iterator;

public final class Splitter {
    private final int limit;
    private final boolean omitEmptyStrings;
    private final Strategy strategy;
    private final CharMatcher trimmer;

    class AnonymousClass2 implements Strategy {
        final String val$separator;

        AnonymousClass2(String str) {
            this.val$separator = str;
        }

        @Override
        public SplittingIterator iterator(Splitter splitter, CharSequence charSequence) {
            return new SplittingIterator(this, splitter, charSequence) {
                final AnonymousClass2 this$0;

                {
                    this.this$0 = this;
                }

                @Override
                public int separatorEnd(int i) {
                    return this.this$0.val$separator.length() + i;
                }

                @Override
                public int separatorStart(int i) {
                    int length = this.this$0.val$separator.length();
                    int length2 = this.toSplit.length();
                    int i2 = i;
                    while (i2 <= length2 - length) {
                        for (int i3 = 0; i3 < length; i3++) {
                            if (this.toSplit.charAt(i3 + i2) != this.this$0.val$separator.charAt(i3)) {
                                break;
                            }
                        }
                        return i2;
                    }
                    return -1;
                }
            };
        }
    }

    private static abstract class SplittingIterator extends AbstractIterator<String> {
        int limit;
        int offset = 0;
        final boolean omitEmptyStrings;
        final CharSequence toSplit;
        final CharMatcher trimmer;

        protected SplittingIterator(Splitter splitter, CharSequence charSequence) {
            this.trimmer = splitter.trimmer;
            this.omitEmptyStrings = splitter.omitEmptyStrings;
            this.limit = splitter.limit;
            this.toSplit = charSequence;
        }

        @Override
        public String computeNext() {
            int i = this.offset;
            while (this.offset != -1) {
                int iSeparatorStart = separatorStart(this.offset);
                if (iSeparatorStart == -1) {
                    iSeparatorStart = this.toSplit.length();
                    this.offset = -1;
                } else {
                    this.offset = separatorEnd(iSeparatorStart);
                }
                if (this.offset == i) {
                    this.offset++;
                    if (this.offset >= this.toSplit.length()) {
                        this.offset = -1;
                    }
                } else {
                    int i2 = i;
                    while (i2 < iSeparatorStart && this.trimmer.matches(this.toSplit.charAt(i2))) {
                        i2++;
                    }
                    int length = iSeparatorStart;
                    while (length > i2 && this.trimmer.matches(this.toSplit.charAt(length - 1))) {
                        length--;
                    }
                    if (!this.omitEmptyStrings || i2 != length) {
                        if (this.limit == 1) {
                            length = this.toSplit.length();
                            this.offset = -1;
                            while (length > i2 && this.trimmer.matches(this.toSplit.charAt(length - 1))) {
                                length--;
                            }
                        } else {
                            this.limit--;
                        }
                        return this.toSplit.subSequence(i2, length).toString();
                    }
                    i = this.offset;
                }
            }
            return endOfData();
        }

        abstract int separatorEnd(int i);

        abstract int separatorStart(int i);
    }

    private interface Strategy {
        Iterator<String> iterator(Splitter splitter, CharSequence charSequence);
    }

    private Splitter(Strategy strategy) {
        this(strategy, false, CharMatcher.NONE, Integer.MAX_VALUE);
    }

    private Splitter(Strategy strategy, boolean z, CharMatcher charMatcher, int i) {
        this.strategy = strategy;
        this.omitEmptyStrings = z;
        this.trimmer = charMatcher;
        this.limit = i;
    }

    public static Splitter on(String str) {
        Preconditions.checkArgument(str.length() != 0, "The separator may not be the empty string.");
        return new Splitter(new AnonymousClass2(str));
    }

    public Iterator<String> splittingIterator(CharSequence charSequence) {
        return this.strategy.iterator(this, charSequence);
    }

    public Splitter omitEmptyStrings() {
        return new Splitter(this.strategy, true, this.trimmer, this.limit);
    }

    public Iterable<String> split(CharSequence charSequence) {
        Preconditions.checkNotNull(charSequence);
        return new Iterable<String>(this, charSequence) {
            final Splitter this$0;
            final CharSequence val$sequence;

            {
                this.this$0 = this;
                this.val$sequence = charSequence;
            }

            @Override
            public Iterator<String> iterator() {
                return this.this$0.splittingIterator(this.val$sequence);
            }

            public String toString() {
                Joiner joinerOn = Joiner.on(", ");
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                StringBuilder sbAppendTo = joinerOn.appendTo(sb, this);
                sbAppendTo.append(']');
                return sbAppendTo.toString();
            }
        };
    }
}
