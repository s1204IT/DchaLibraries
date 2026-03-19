package org.gsma.joyn.ish;

import org.gsma.joyn.ish.INewImageSharingListener;

public abstract class NewImageSharingListener extends INewImageSharingListener.Stub {
    @Override
    public abstract void onNewImageSharing(String str);
}
