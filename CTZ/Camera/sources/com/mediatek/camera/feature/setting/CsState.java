package com.mediatek.camera.feature.setting;

import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;

public class CsState {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(CsState.class.getSimpleName());
    private State mCShotState = State.STATE_INIT;
    private final Object mStateSync = new Object();

    enum State {
        STATE_INIT,
        STATE_CAPTURE_STARTED,
        STATE_CAPTURING,
        STATE_STOPPED,
        STATE_ERROR
    }

    void updateState(State state) {
        checkState(getCShotState(), state);
        setCShotState(state);
    }

    State getCShotState() {
        State state;
        synchronized (this.mStateSync) {
            state = this.mCShotState;
        }
        return state;
    }

    private void setCShotState(State state) {
        synchronized (this.mStateSync) {
            this.mCShotState = state;
        }
    }

    private void checkState(State state, State state2) {
        if (state == state2) {
        }
        switch (AnonymousClass1.$SwitchMap$com$mediatek$camera$feature$setting$CsState$State[state2.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (state != State.STATE_INIT) {
                    LogHelper.e(TAG, "[checkState]Error!");
                }
                break;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (state != State.STATE_CAPTURE_STARTED) {
                    LogHelper.e(TAG, "[checkState]Error!");
                }
                break;
            case Camera2Proxy.TEMPLATE_RECORD:
                if (state != State.STATE_CAPTURING) {
                    LogHelper.e(TAG, "[checkState]Error!");
                }
                break;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$mediatek$camera$feature$setting$CsState$State = new int[State.values().length];

        static {
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$CsState$State[State.STATE_CAPTURE_STARTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$CsState$State[State.STATE_CAPTURING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$mediatek$camera$feature$setting$CsState$State[State.STATE_STOPPED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }
}
