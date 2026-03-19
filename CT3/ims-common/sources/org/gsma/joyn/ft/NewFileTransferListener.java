package org.gsma.joyn.ft;

import org.gsma.joyn.ft.INewFileTransferListener;

public abstract class NewFileTransferListener extends INewFileTransferListener.Stub {
    @Override
    public abstract void onNewFileTransfer(String str);

    @Override
    public abstract void onReportFileDelivered(String str);

    @Override
    public abstract void onReportFileDisplayed(String str);

    @Override
    public void onNewFileTransferReceived(String transferId, boolean isAutoAccept, boolean isGroup, String chatSessionId, String ChatId, int timeLen) {
    }

    @Override
    public void onFileDeliveredReport(String transferId, String contact) {
    }

    @Override
    public void onFileDisplayedReport(String transferId, String contact) {
    }

    @Override
    public void onNewBurnFileTransfer(String transferId, boolean isGroup, String chatSessionId, String ChatId) {
    }

    @Override
    public void onNewPublicAccountChatFile(String transferId, boolean isAutoAccept, boolean isGroup, String chatSessionId, String ChatId) {
    }
}
