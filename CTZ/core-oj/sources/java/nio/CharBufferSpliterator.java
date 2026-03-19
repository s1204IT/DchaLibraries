package java.nio;

import java.util.Spliterator;
import java.util.function.IntConsumer;

class CharBufferSpliterator implements Spliterator.OfInt {
    static final boolean $assertionsDisabled = false;
    private final CharBuffer buffer;
    private int index;
    private final int limit;

    CharBufferSpliterator(CharBuffer charBuffer) {
        this(charBuffer, charBuffer.position(), charBuffer.limit());
    }

    CharBufferSpliterator(CharBuffer charBuffer, int i, int i2) {
        this.buffer = charBuffer;
        this.index = i > i2 ? i2 : i;
        this.limit = i2;
    }

    @Override
    public Spliterator.OfInt trySplit() {
        int i = this.index;
        int i2 = (this.limit + i) >>> 1;
        if (i >= i2) {
            return null;
        }
        CharBuffer charBuffer = this.buffer;
        this.index = i2;
        return new CharBufferSpliterator(charBuffer, i, i2);
    }

    @Override
    public void forEachRemaining(IntConsumer intConsumer) {
        if (intConsumer == null) {
            throw new NullPointerException();
        }
        CharBuffer charBuffer = this.buffer;
        int i = this.limit;
        this.index = i;
        for (int i2 = this.index; i2 < i; i2++) {
            intConsumer.accept(charBuffer.getUnchecked(i2));
        }
    }

    @Override
    public boolean tryAdvance(IntConsumer intConsumer) {
        if (intConsumer == null) {
            throw new NullPointerException();
        }
        if (this.index >= 0 && this.index < this.limit) {
            CharBuffer charBuffer = this.buffer;
            int i = this.index;
            this.index = i + 1;
            intConsumer.accept(charBuffer.getUnchecked(i));
            return true;
        }
        return false;
    }

    @Override
    public long estimateSize() {
        return this.limit - this.index;
    }

    @Override
    public int characteristics() {
        return 16464;
    }
}
