package org.gsma.joyn.session;

import org.gsma.joyn.session.IMultimediaSessionListener;

public abstract class MultimediaSessionListener extends IMultimediaSessionListener.Stub {
    @Override
    public abstract void onNewMessage(byte[] bArr);

    @Override
    public abstract void onSessionAborted();

    @Override
    public abstract void onSessionError(int i);

    @Override
    public abstract void onSessionRinging();

    @Override
    public abstract void onSessionStarted();
}
