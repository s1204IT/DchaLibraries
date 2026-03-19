package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallPlayerListener;

public abstract class IPCallPlayerListener extends IIPCallPlayerListener.Stub {
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
