package com.mediatek.vcalendar;

public interface VCalStatusChangedListener {
    void vCalOperationCanceled(int i, int i2);

    void vCalOperationExceptionOccured(int i, int i2, int i3);

    void vCalOperationFinished(int i, int i2, Object obj);

    void vCalOperationStarted(int i);

    void vCalProcessStatusUpdate(int i, int i2);
}
