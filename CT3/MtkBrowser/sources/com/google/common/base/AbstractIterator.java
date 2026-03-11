package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import java.util.NoSuchElementException;

@GwtCompatible
abstract class AbstractIterator<T> implements Iterator<T> {

    private static final int[] f6comgooglecommonbaseAbstractIterator$StateSwitchesValues = null;
    private T next;
    private State state = State.NOT_READY;

    private static int[] m264getcomgooglecommonbaseAbstractIterator$StateSwitchesValues() {
        if (f6comgooglecommonbaseAbstractIterator$StateSwitchesValues != null) {
            return f6comgooglecommonbaseAbstractIterator$StateSwitchesValues;
        }
        int[] iArr = new int[State.valuesCustom().length];
        try {
            iArr[State.DONE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[State.FAILED.ordinal()] = 3;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[State.NOT_READY.ordinal()] = 4;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[State.READY.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        f6comgooglecommonbaseAbstractIterator$StateSwitchesValues = iArr;
        return iArr;
    }

    protected abstract T computeNext();

    protected AbstractIterator() {
    }

    private enum State {
        READY,
        NOT_READY,
        DONE,
        FAILED;

        public static State[] valuesCustom() {
            return values();
        }
    }

    protected final T endOfData() {
        this.state = State.DONE;
        return null;
    }

    @Override
    public final boolean hasNext() {
        Preconditions.checkState(this.state != State.FAILED);
        switch (m264getcomgooglecommonbaseAbstractIterator$StateSwitchesValues()[this.state.ordinal()]) {
            case 1:
                return false;
            case 2:
                return true;
            default:
                return tryToComputeNext();
        }
    }

    private boolean tryToComputeNext() {
        this.state = State.FAILED;
        this.next = computeNext();
        if (this.state != State.DONE) {
            this.state = State.READY;
            return true;
        }
        return false;
    }

    @Override
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        this.state = State.NOT_READY;
        T result = this.next;
        this.next = null;
        return result;
    }

    @Override
    public final void remove() {
        throw new UnsupportedOperationException();
    }
}
