package org.gsma.joyn.chat;

public class GroupChatIntent {
    public static final String ACTION_NEW_INVITATION = "org.gsma.joyn.chat.action.NEW_GROUP_CHAT";
    public static final String ACTION_REINVITATION = "org.gsma.joyn.chat.action.NEW_REINVITATION";
    public static final String ACTION_SESSION_REPLACED = "org.gsma.joyn.chat.action.REPLACED_GROUP_CHAT";
    public static final String EXTRA_CHAT_ID = "chatId";
    public static final String EXTRA_CHAT_MESSAGE = "chatmessage";
    public static final String EXTRA_CONTACT = "contact";
    public static final String EXTRA_DISPLAY_NAME = "contactDisplayname";
    public static final String EXTRA_SESSION_IDENTITY = "sessionIdentity";
    public static final String EXTRA_SUBJECT = "subject";
}
