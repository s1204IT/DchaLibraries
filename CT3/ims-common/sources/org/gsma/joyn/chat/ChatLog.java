package org.gsma.joyn.chat;

import android.net.Uri;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ChatLog {

    public static class GroupChat {
        public static final String CHAIRMAN = "chairman";
        public static final String CHAT_ID = "chat_id";
        public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/chat");
        public static final String CONVERSATION_ID = "conversation_id";
        public static final String DIRECTION = "direction";
        public static final String ID = "_id";
        public static final String ISBLOCKED = "isBlocked";
        public static final String NICKNAME = "nickname";
        public static final String PARTICIPANTS_LIST = "participants";
        public static final String STATE = "state";
        public static final String SUBJECT = "subject";
        public static final String TIMESTAMP = "timestamp";
    }

    public static class GroupChatMember {
        public static final String CHAT_ID = "CHAT_ID";
        public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/groupmember");
        public static final String CONVERSATION_ID = "conversation_id";
        public static final String GROUP_MEMBER_NAME = "MEMBER_NAME";
        public static final String GROUP_MEMBER_NUMBER = "CONTACT_NUMBER";
        public static final String GROUP_MEMBER_PORTRAIT = "PORTRAIT";
        public static final String GROUP_MEMBER_TYPE = "CONTACT_ETYPE";
        public static final String ID = "_id";
    }

    public static class Message {
        public static final String BODY = "body";
        public static final String CHAT_ID = "chat_id";
        public static final String CONTACT_NUMBER = "sender";
        public static final String CONVERSATION_ID = "conversation_id";
        public static final String DIRECTION = "direction";
        public static final String DISPLAY_NAME = "display_name";
        public static final String ID = "_id";
        public static final String MESSAGE_ID = "msg_id";
        public static final String MESSAGE_STATUS = "status";
        public static final String MESSAGE_TYPE = "msg_type";
        public static final String MIME_TYPE = "mime_type";
        public static final String TIMESTAMP = "timestamp";
        public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";
        public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";
        public static final String TIMESTAMP_SENT = "timestamp_sent";
        public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/message");
        public static final Uri CONTENT_CHAT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/message/#");

        public static class Direction {
            public static final int INCOMING = 0;
            public static final int IRRELEVANT = 2;
            public static final int OUTGOING = 1;
        }

        public static class Status {

            public static class Content {
                public static final int BLOCKED = 7;
                public static final int FAILED = 5;
                public static final int READ = 2;
                public static final int SENDING = 3;
                public static final int SENT = 4;
                public static final int TO_SEND = 6;
                public static final int UNREAD = 0;
                public static final int UNREAD_REPORT = 1;
            }

            public static class System {
                public static final int ACCEPTED = 1;
                public static final int BUSY = 7;
                public static final int DECLINED = 2;
                public static final int DISCONNECTED = 6;
                public static final int FAILED = 3;
                public static final int GONE = 5;
                public static final int JOINED = 4;
                public static final int PENDING = 0;
            }
        }

        public static class Type {
            public static final int BURN = 3;
            public static final int CLOUD = 5;
            public static final int CONTENT = 0;
            public static final int EMOTICON = 6;
            public static final int PROSECUTE = 7;
            public static final int PUBLIC = 4;
            public static final int SPAM = 2;
            public static final int SYSTEM = 1;
        }
    }

    public static class MultiMessage {
        public static final String CHAT_ID = "chat_id";
        public static final String DIRECTION = "direction";
        public static final String ID = "_id";
        public static final String MESSAGE_ID = "msg_id";
        public static final String PARTICIPANTS_LIST = "participants";
        public static final String STATE = "state";
        public static final String SUBJECT = "subject";
        public static final String TIMESTAMP = "timestamp";
        public static final Uri CONTENT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/multimessage");
        public static final Uri CONTENT_CHAT_URI = Uri.parse("content://org.gsma.joyn.provider.chat/multimessage/#");

        public static class Direction {
            public static final int INCOMING = 0;
            public static final int IRRELEVANT = 2;
            public static final int OUTGOING = 1;
        }

        public static class Status {

            public static class Content {
                public static final int BLOCKED = 7;
                public static final int FAILED = 5;
                public static final int READ = 2;
                public static final int SENDING = 3;
                public static final int SENT = 4;
                public static final int TO_SEND = 6;
                public static final int UNREAD = 0;
                public static final int UNREAD_REPORT = 1;
            }

            public static class System {
                public static final int ACCEPTED = 1;
                public static final int BUSY = 7;
                public static final int DECLINED = 2;
                public static final int DISCONNECTED = 6;
                public static final int FAILED = 3;
                public static final int GONE = 5;
                public static final int JOINED = 4;
                public static final int PENDING = 0;
            }
        }

        public static class Type {
            public static final int CONTENT = 0;
            public static final int SPAM = 2;
            public static final int SYSTEM = 1;
        }
    }

    public static String getTextFromBlob(byte[] content) {
        try {
            return new String(content);
        } catch (Exception e) {
            return null;
        }
    }

    public static Geoloc getGeolocFromBlob(byte[] content) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(content);
            ObjectInputStream is = new ObjectInputStream(bis);
            Geoloc geoloc = (Geoloc) is.readObject();
            is.close();
            return geoloc;
        } catch (Exception e) {
            return null;
        }
    }
}
