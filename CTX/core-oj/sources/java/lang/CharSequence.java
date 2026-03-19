package java.lang;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public interface CharSequence {
    char charAt(int i);

    int length();

    CharSequence subSequence(int i, int i2);

    String toString();

    default IntStream chars() {
        return StreamSupport.intStream(new Supplier() {
            @Override
            public final Object get() {
                return Spliterators.spliterator(new PrimitiveIterator.OfInt() {
                    int cur = 0;

                    @Override
                    public boolean hasNext() {
                        return this.cur < CharSequence.this.length();
                    }

                    @Override
                    public int nextInt() {
                        if (hasNext()) {
                            CharSequence charSequence = CharSequence.this;
                            int i = this.cur;
                            this.cur = i + 1;
                            return charSequence.charAt(i);
                        }
                        throw new NoSuchElementException();
                    }

                    @Override
                    public void forEachRemaining(IntConsumer intConsumer) {
                        while (this.cur < CharSequence.this.length()) {
                            intConsumer.accept(CharSequence.this.charAt(this.cur));
                            this.cur++;
                        }
                    }
                }, (long) r0.length(), 16);
            }
        }, 16464, false);
    }

    default IntStream codePoints() {
        return StreamSupport.intStream(new Supplier() {
            @Override
            public final Object get() {
                return Spliterators.spliteratorUnknownSize(new PrimitiveIterator.OfInt() {
                    int cur = 0;

                    @Override
                    public void forEachRemaining(IntConsumer intConsumer) throws Throwable {
                        int i;
                        int length = CharSequence.this.length();
                        int i2 = this.cur;
                        while (i2 < length) {
                            try {
                                i = i2 + 1;
                                try {
                                    char cCharAt = CharSequence.this.charAt(i2);
                                    if (!Character.isHighSurrogate(cCharAt) || i >= length) {
                                        intConsumer.accept(cCharAt);
                                    } else {
                                        char cCharAt2 = CharSequence.this.charAt(i);
                                        if (Character.isLowSurrogate(cCharAt2)) {
                                            i++;
                                            intConsumer.accept(Character.toCodePoint(cCharAt, cCharAt2));
                                        } else {
                                            intConsumer.accept(cCharAt);
                                        }
                                    }
                                    i2 = i;
                                } catch (Throwable th) {
                                    th = th;
                                    this.cur = i;
                                    throw th;
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                i = i2;
                            }
                        }
                        this.cur = i2;
                    }

                    @Override
                    public boolean hasNext() {
                        return this.cur < CharSequence.this.length();
                    }

                    @Override
                    public int nextInt() {
                        int length = CharSequence.this.length();
                        if (this.cur >= length) {
                            throw new NoSuchElementException();
                        }
                        CharSequence charSequence = CharSequence.this;
                        int i = this.cur;
                        this.cur = i + 1;
                        char cCharAt = charSequence.charAt(i);
                        if (Character.isHighSurrogate(cCharAt) && this.cur < length) {
                            char cCharAt2 = CharSequence.this.charAt(this.cur);
                            if (Character.isLowSurrogate(cCharAt2)) {
                                this.cur++;
                                return Character.toCodePoint(cCharAt, cCharAt2);
                            }
                        }
                        return cCharAt;
                    }
                }, 16);
            }
        }, 16, false);
    }
}
