package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.IVideoRendererListener;

public abstract class VideoRendererListener extends IVideoRendererListener.Stub {
    @Override
    public abstract void onRendererClosed();

    @Override
    public abstract void onRendererFailed();

    @Override
    public abstract void onRendererOpened();

    @Override
    public abstract void onRendererStarted();

    @Override
    public abstract void onRendererStopped();
}
