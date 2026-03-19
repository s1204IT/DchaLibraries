package com.mediatek.camera.feature.setting.focus;

public interface IFocusController {

    public enum AutoFocusState {
        INACTIVE,
        ACTIVE_SCAN,
        ACTIVE_FOCUSED,
        ACTIVE_UNFOCUSED,
        PASSIVE_SCAN,
        PASSIVE_FOCUSED,
        PASSIVE_UNFOCUSED
    }

    public interface FocusStateListener {
        void onFocusStatusUpdate(AutoFocusState autoFocusState, long j);
    }

    void setFocusStateListener(FocusStateListener focusStateListener);
}
