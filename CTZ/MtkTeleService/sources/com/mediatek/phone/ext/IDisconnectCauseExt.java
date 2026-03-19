package com.mediatek.phone.ext;

public interface IDisconnectCauseExt {
    int toTelecomDisconnectCauseCode(int i, int i2);

    CharSequence toTelecomDisconnectCauseDescription(int i);

    CharSequence toTelecomDisconnectCauseLabel(int i);
}
