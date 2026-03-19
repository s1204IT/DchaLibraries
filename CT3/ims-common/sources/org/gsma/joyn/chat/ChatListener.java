package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IChatListener;

public abstract class ChatListener extends IChatListener.Stub {
    @Override
    public abstract void onComposingEvent(boolean z);

    @Override
    public abstract void onNewGeoloc(GeolocMessage geolocMessage);

    @Override
    public abstract void onNewMessage(ChatMessage chatMessage);

    @Override
    public abstract void onReportFailedMessage(String str, int i, String str2);

    @Override
    public abstract void onReportMessageDelivered(String str);

    @Override
    public abstract void onReportMessageDisplayed(String str);

    @Override
    public abstract void onReportMessageFailed(String str);

    @Override
    public abstract void onReportMessageSent(String str);
}
