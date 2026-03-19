package com.mediatek.camera.feature.setting.focus;

import android.hardware.Camera;
import java.util.List;

interface IFocus {

    public enum AfModeState {
        STATE_INVALID,
        STATE_SINGLE,
        STATE_MULTI
    }

    public interface Listener {
        void autoFocus();

        void cancelAutoFocus();

        void disableUpdateFocusState(boolean z);

        String getCurrentFocusMode();

        boolean isFocusCanDo();

        boolean needWaitAfTriggerDone();

        void overrideFocusMode(String str, List<String> list);

        void resetConfiguration();

        void restoreContinue();

        void setWaitCancelAutoFocus(boolean z);

        void updateFocusArea(List<Camera.Area> list, List<Camera.Area> list2);

        void updateFocusCallback();

        void updateFocusMode(String str);
    }

    public enum LockState {
        STATE_UNLOCKED,
        STATE_LOCKING,
        STATE_LOCKED
    }
}
