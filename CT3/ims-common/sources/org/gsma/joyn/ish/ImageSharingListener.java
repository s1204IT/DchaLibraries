package org.gsma.joyn.ish;

import org.gsma.joyn.ish.IImageSharingListener;

public abstract class ImageSharingListener extends IImageSharingListener.Stub {
    @Override
    public abstract void onImageShared(String str);

    @Override
    public abstract void onSharingAborted();

    @Override
    public abstract void onSharingError(int i);

    @Override
    public abstract void onSharingProgress(long j, long j2);

    @Override
    public abstract void onSharingStarted();
}
