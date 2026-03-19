package com.android.internal.telephony.cat;

public enum FontSize {
    NORMAL(0),
    LARGE(1),
    SMALL(2);

    private int mValue;

    FontSize(int i) {
        this.mValue = i;
    }

    public static FontSize fromInt(int i) {
        for (FontSize fontSize : values()) {
            if (fontSize.mValue == i) {
                return fontSize;
            }
        }
        return null;
    }
}
