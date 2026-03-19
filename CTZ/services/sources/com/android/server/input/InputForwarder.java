package com.android.server.input;

import android.app.IInputForwarder;
import android.hardware.input.InputManagerInternal;
import android.view.InputEvent;
import com.android.server.LocalServices;

class InputForwarder extends IInputForwarder.Stub {
    private final int mDisplayId;
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);

    InputForwarder(int i) {
        this.mDisplayId = i;
    }

    public boolean forwardEvent(InputEvent inputEvent) {
        return this.mInputManagerInternal.injectInputEvent(inputEvent, this.mDisplayId, 0);
    }
}
