package com.mediatek.camera.feature.setting.focus;

interface IFocusView {

    public enum FocusViewState {
        STATE_IDLE,
        STATE_ACTIVE_FOCUSING,
        STATE_ACTIVE_FOCUSED,
        STATE_PASSIVE_FOCUSING
    }
}
