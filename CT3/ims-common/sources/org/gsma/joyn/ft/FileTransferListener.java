package org.gsma.joyn.ft;

import org.gsma.joyn.ft.IFileTransferListener;

public abstract class FileTransferListener extends IFileTransferListener.Stub {
    @Override
    public abstract void onFileTransferred(String str);

    @Override
    public abstract void onTransferAborted();

    @Override
    public abstract void onTransferError(int i);

    @Override
    public abstract void onTransferProgress(long j, long j2);

    @Override
    public abstract void onTransferStarted();

    @Override
    public void onTransferPaused() {
    }

    @Override
    public void onTransferResumed(String oldFTid, String newFTId) {
    }
}
