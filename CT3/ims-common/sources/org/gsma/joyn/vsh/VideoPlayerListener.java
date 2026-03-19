package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.IVideoPlayerListener;

public abstract class VideoPlayerListener extends IVideoPlayerListener.Stub {
    @Override
    public abstract void onPlayerClosed();

    @Override
    public abstract void onPlayerFailed();

    @Override
    public abstract void onPlayerOpened();

    @Override
    public abstract void onPlayerStarted();

    @Override
    public abstract void onPlayerStopped();
}
