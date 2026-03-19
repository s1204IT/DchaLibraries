package com.android.bluetooth.hfpclient;

import android.os.HandlerThread;

public class HeadsetClientStateMachineFactory {
    public HeadsetClientStateMachine make(HeadsetClientService headsetClientService, HandlerThread handlerThread) {
        return HeadsetClientStateMachine.make(headsetClientService, handlerThread.getLooper());
    }
}
