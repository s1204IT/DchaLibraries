package android.icu.impl;

import android.icu.impl.PropsVectors;

public class PVecToTrieCompactHandler implements PropsVectors.CompactHandler {
    public IntTrieBuilder builder;
    public int initialValue;

    @Override
    public void setRowIndexForErrorValue(int i) {
    }

    @Override
    public void setRowIndexForInitialValue(int i) {
        this.initialValue = i;
    }

    @Override
    public void setRowIndexForRange(int i, int i2, int i3) {
        this.builder.setRange(i, i2 + 1, i3, true);
    }

    @Override
    public void startRealValues(int i) {
        if (i > 65535) {
            throw new IndexOutOfBoundsException();
        }
        this.builder = new IntTrieBuilder(null, 100000, this.initialValue, this.initialValue, false);
    }
}
