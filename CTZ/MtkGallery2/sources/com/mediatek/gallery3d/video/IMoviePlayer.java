package com.mediatek.gallery3d.video;

import android.view.SurfaceView;
import android.view.animation.Animation;

public interface IMoviePlayer {
    boolean canSeekBackward();

    boolean canSeekForward();

    boolean canStop();

    int getCurrentPosition();

    int getDuration();

    Animation getHideAnimation();

    boolean getLoop();

    int getVideoLastDuration();

    int getVideoPosition();

    SurfaceView getVideoSurface();

    int getVideoType();

    boolean isTimeBarEnabled();

    boolean isVideoCanSeek();

    void notifyCompletion();

    void seekTo(int i);

    void setDuration(int i);

    void setLoop(boolean z);

    void showEnded();

    void showMovieController();

    void showSubtitleViewSetDialog();

    void startNextVideo(IMovieItem iMovieItem);

    void startVideo(boolean z, int i, int i2);

    void stopVideo();

    void updateProgressBar();
}
