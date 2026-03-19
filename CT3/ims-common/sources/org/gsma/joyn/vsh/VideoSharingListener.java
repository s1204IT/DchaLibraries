package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.IVideoSharingListener;

public abstract class VideoSharingListener extends IVideoSharingListener.Stub {
    @Override
    public abstract void onSharingAborted();

    @Override
    public abstract void onSharingError(int i);

    @Override
    public abstract void onSharingStarted();
}
