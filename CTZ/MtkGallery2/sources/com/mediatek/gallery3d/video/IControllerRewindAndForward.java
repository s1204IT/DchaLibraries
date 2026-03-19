package com.mediatek.gallery3d.video;

import com.android.gallery3d.app.ControllerOverlay;

public interface IControllerRewindAndForward extends ControllerOverlay {

    public interface IRewindAndForwardListener extends ControllerOverlay.Listener {
        void onForward();

        void onRewind();

        void onStopVideo();
    }

    boolean getPlayPauseEanbled();

    boolean getTimeBarEanbled();

    void setIListener(IRewindAndForwardListener iRewindAndForwardListener);

    void showControllerButtonsView(boolean z, boolean z2, boolean z3);
}
