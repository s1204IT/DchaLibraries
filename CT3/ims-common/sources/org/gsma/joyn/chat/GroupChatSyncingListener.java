package org.gsma.joyn.chat;

import org.gsma.joyn.chat.IGroupChatSyncingListener;

public abstract class GroupChatSyncingListener extends IGroupChatSyncingListener.Stub {
    @Override
    public abstract void onSyncDone(int i);

    @Override
    public abstract void onSyncInfo(String str, ConferenceEventData conferenceEventData);

    @Override
    public abstract void onSyncStart(int i);
}
