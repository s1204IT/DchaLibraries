package com.mediatek.vcalendar;

public interface VCalStatusChangedListener {
    void vCalOperationExceptionOccured(int i, int i2, int i3);

    void vCalOperationFinished(int i, int i2, Object obj);
}
