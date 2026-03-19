package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.INewVideoSharingListener;

public abstract class NewVideoSharingListener extends INewVideoSharingListener.Stub {
    @Override
    public abstract void onNewVideoSharing(String str);
}
