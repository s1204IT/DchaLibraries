package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.IIPCallListener;

public abstract class IPCallListener extends IIPCallListener.Stub {
    @Override
    public abstract void onCallAborted();

    @Override
    public abstract void onCallContinue();

    @Override
    public abstract void onCallError(int i);

    @Override
    public abstract void onCallHeld();

    @Override
    public abstract void onCallStarted();
}
