package com.mediatek.phone.ext;

public class DefaultDisconnectCauseExt implements IDisconnectCauseExt {
    @Override
    public int toTelecomDisconnectCauseCode(int i, int i2) {
        return i2;
    }

    @Override
    public CharSequence toTelecomDisconnectCauseLabel(int i) {
        return "";
    }

    @Override
    public CharSequence toTelecomDisconnectCauseDescription(int i) {
        return "";
    }
}
