package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IExtendChatListener;

public abstract class ExtendChatListener extends IExtendChatListener.Stub {
    @Override
    public abstract void onNewMessage(ExtendMessage extendMessage);

    @Override
    public abstract void onReportMessageDelivered(String str, String str2);

    @Override
    public abstract void onReportMessageDisplayed(String str, String str2);

    @Override
    public abstract void onReportMessageFailed(String str, int i, String str2);

    @Override
    public abstract void onReportMessageSent(String str);

    @Override
    public void onReportMessageInviteError(String msgId, String warningText, boolean isForbidden) {
    }
}
