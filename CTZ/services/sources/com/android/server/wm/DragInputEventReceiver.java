package com.android.server.wm;

import android.os.Looper;
import android.util.Slog;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import com.mediatek.server.wm.WmsExt;

class DragInputEventReceiver extends InputEventReceiver {
    private final DragDropController mDragDropController;
    private boolean mIsStartEvent;
    private boolean mMuteInput;
    private boolean mStylusButtonDownAtStart;

    DragInputEventReceiver(InputChannel inputChannel, Looper looper, DragDropController dragDropController) {
        super(inputChannel, looper);
        this.mIsStartEvent = true;
        this.mMuteInput = false;
        this.mDragDropController = dragDropController;
    }

    public void onInputEvent(InputEvent inputEvent, int i) {
        try {
            if ((inputEvent instanceof MotionEvent) && (inputEvent.getSource() & 2) != 0 && !this.mMuteInput) {
                MotionEvent motionEvent = (MotionEvent) inputEvent;
                float rawX = motionEvent.getRawX();
                float rawY = motionEvent.getRawY();
                boolean z = (motionEvent.getButtonState() & 32) != 0;
                if (this.mIsStartEvent) {
                    this.mStylusButtonDownAtStart = z;
                    this.mIsStartEvent = false;
                }
                switch (motionEvent.getAction()) {
                    case 0:
                        if (WindowManagerDebugConfig.DEBUG_DRAG) {
                            Slog.w(WmsExt.TAG, "Unexpected ACTION_DOWN in drag layer");
                        }
                        return;
                    case 1:
                        if (WindowManagerDebugConfig.DEBUG_DRAG) {
                            Slog.d(WmsExt.TAG, "Got UP on move channel; dropping at " + rawX + "," + rawY);
                        }
                        this.mMuteInput = true;
                        break;
                    case 2:
                        if (this.mStylusButtonDownAtStart && !z) {
                            if (WindowManagerDebugConfig.DEBUG_DRAG) {
                                Slog.d(WmsExt.TAG, "Button no longer pressed; dropping at " + rawX + "," + rawY);
                            }
                            this.mMuteInput = true;
                        }
                        break;
                    case 3:
                        if (WindowManagerDebugConfig.DEBUG_DRAG) {
                            Slog.d(WmsExt.TAG, "Drag cancelled!");
                        }
                        this.mMuteInput = true;
                        break;
                    default:
                        return;
                }
                this.mDragDropController.handleMotionEvent(!this.mMuteInput, rawX, rawY);
                finishInputEvent(inputEvent, true);
            }
        } catch (Exception e) {
            Slog.e(WmsExt.TAG, "Exception caught by drag handleMotion", e);
        } finally {
            finishInputEvent(inputEvent, false);
        }
    }
}
