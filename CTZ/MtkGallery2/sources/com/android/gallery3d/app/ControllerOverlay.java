package com.android.gallery3d.app;

public interface ControllerOverlay {

    public interface Listener {
        void onHidden();

        void onPlayPause();

        void onReplay();

        void onSeekEnd(int i, int i2, int i3);

        void onSeekMove(int i);

        void onSeekStart();

        void onShown();

        boolean powerSavingNeedShowController();
    }
}
