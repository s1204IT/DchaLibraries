package android.icu.impl;

import java.text.FieldPosition;

public final class DontCareFieldPosition extends FieldPosition {
    public static final DontCareFieldPosition INSTANCE = new DontCareFieldPosition();

    private DontCareFieldPosition() {
        super(-913028704);
    }

    @Override
    public void setBeginIndex(int i) {
    }

    @Override
    public void setEndIndex(int i) {
    }
}
