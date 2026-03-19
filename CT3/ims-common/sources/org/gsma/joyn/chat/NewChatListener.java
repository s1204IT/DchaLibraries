package org.gsma.joyn.chat;

import org.gsma.joyn.chat.INewChatListener;

public abstract class NewChatListener extends INewChatListener.Stub {
    @Override
    public abstract void onNewGroupChat(String str);

    @Override
    public abstract void onNewSingleChat(String str, ChatMessage chatMessage);

    public void onNewPublicAccountChat(String chatId, ChatMessage message) {
    }
}
