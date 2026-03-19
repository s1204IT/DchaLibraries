package com.android.internal.telephony.cat;

public enum FontSize {
    NORMAL(0),
    LARGE(1),
    SMALL(2);

    private int mValue;

    public static FontSize[] valuesCustom() {
        return values();
    }

    FontSize(int value) {
        this.mValue = value;
    }

    public static FontSize fromInt(int value) {
        for (FontSize e : valuesCustom()) {
            if (e.mValue == value) {
                return e;
            }
        }
        return null;
    }
}
