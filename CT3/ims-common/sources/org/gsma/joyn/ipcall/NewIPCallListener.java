package org.gsma.joyn.ipcall;

import org.gsma.joyn.ipcall.INewIPCallListener;

public abstract class NewIPCallListener extends INewIPCallListener.Stub {
    @Override
    public abstract void onNewIPCall(String str);
}
