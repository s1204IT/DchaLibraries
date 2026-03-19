package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallRendererListener;

public abstract class IPCallRendererListener extends IIPCallRendererListener.Stub {
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
