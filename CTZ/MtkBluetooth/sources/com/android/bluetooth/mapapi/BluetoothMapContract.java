package com.android.bluetooth.mapapi;

import android.net.Uri;

public final class BluetoothMapContract {
    public static final String DELIVERY_STATE_DELIVERED = "delivered";
    public static final String DELIVERY_STATE_SENT = "sent";
    public static final String EXTRA_ACCOUNT_ID = "AccountId";
    public static final String EXTRA_BLUETOOTH_STATE = "BluetoothState";
    public static final String EXTRA_CHAT_STATE = "ChatState";
    public static final String EXTRA_CONVERSATION_ID = "ConversationId";
    public static final String EXTRA_LAST_ACTIVE = "LastActive";
    public static final String EXTRA_PRESENCE_STATE = "PresenceState";
    public static final String EXTRA_PRESENCE_STATUS = "PresenceStatus";
    public static final String EXTRA_UPDATE_ACCOUNT_ID = "UpdateAccountId";
    public static final String EXTRA_UPDATE_FOLDER_ID = "UpdateFolderId";
    public static final String FILE_MSG_DOWNLOAD = "DOWNLOAD";
    public static final String FILE_MSG_DOWNLOAD_NO_ATTACHMENTS = "DOWNLOAD_NO_ATTACHMENTS";
    public static final String FILE_MSG_NO_ATTACHMENTS = "NO_ATTACHMENTS";
    public static final String FILTER_ORIGINATOR_SUBSTRING = "org_sub_str";
    public static final String FILTER_PERIOD_BEGIN = "t_begin";
    public static final String FILTER_PERIOD_END = "t_end";
    public static final String FILTER_READ_STATUS = "read";
    public static final String FILTER_RECIPIENT_SUBSTRING = "rec_sub_str";
    public static final String FILTER_THREAD_ID = "thread_id";
    public static final long FOLDER_ID_DELETED = 5;
    public static final long FOLDER_ID_DRAFT = 3;
    public static final long FOLDER_ID_INBOX = 1;
    public static final long FOLDER_ID_OTHER = 0;
    public static final long FOLDER_ID_OUTBOX = 4;
    public static final long FOLDER_ID_SENT = 2;
    public static final String FOLDER_NAME_DELETED = "DELETED";
    public static final String FOLDER_NAME_DRAFT = "DRAFT";
    public static final String FOLDER_NAME_INBOX = "INBOX";
    public static final String FOLDER_NAME_OTHER = "OTHER";
    public static final String FOLDER_NAME_OUTBOX = "OUTBOX";
    public static final String FOLDER_NAME_SENT = "SENT";
    public static final String METHOD_SET_BLUETOOTH_STATE = "SetBtState";
    public static final String METHOD_SET_OWNER_STATUS = "SetOwnerStatus";
    public static final String METHOD_UPDATE_FOLDER = "UpdateFolder";
    public static final String PROVIDER_INTERFACE_EMAIL = "android.bluetooth.action.BLUETOOTH_MAP_PROVIDER";
    public static final String PROVIDER_INTERFACE_IM = "android.bluetooth.action.BLUETOOTH_MAP_IM_PROVIDER";
    public static final String RECEPTION_STATE_COMPLETE = "complete";
    public static final String RECEPTION_STATE_FRACTIONED = "fractioned";
    public static final String RECEPTION_STATE_NOTIFICATION = "notification";
    public static final String TABLE_ACCOUNT = "Account";
    public static final String TABLE_CONVERSATION = "Conversation";
    public static final String TABLE_CONVOCONTACT = "ConvoContact";
    public static final String TABLE_FOLDER = "Folder";
    public static final String TABLE_MESSAGE = "Message";
    public static final String TABLE_MESSAGE_PART = "Part";
    public static final String[] BT_MESSAGE_PROJECTION = {"_id", MessageColumns.DATE, MessageColumns.SUBJECT, MessageColumns.BODY, MessageColumns.MESSAGE_SIZE, MessageColumns.FOLDER_ID, MessageColumns.FLAG_READ, MessageColumns.FLAG_PROTECTED, MessageColumns.FLAG_HIGH_PRIORITY, MessageColumns.FLAG_ATTACHMENT, MessageColumns.ATTACHMENT_SIZE, MessageColumns.FROM_LIST, MessageColumns.TO_LIST, EmailMessageColumns.CC_LIST, EmailMessageColumns.BCC_LIST, EmailMessageColumns.REPLY_TO_LIST, MessageColumns.RECEPTION_STATE, MessageColumns.DEVILERY_STATE, "thread_id"};
    public static final String[] BT_INSTANT_MESSAGE_PROJECTION = {"_id", MessageColumns.DATE, MessageColumns.SUBJECT, MessageColumns.MESSAGE_SIZE, MessageColumns.FOLDER_ID, MessageColumns.FLAG_READ, MessageColumns.FLAG_PROTECTED, MessageColumns.FLAG_HIGH_PRIORITY, MessageColumns.FLAG_ATTACHMENT, MessageColumns.ATTACHMENT_SIZE, MessageColumns.ATTACHMENT_MINE_TYPES, MessageColumns.FROM_LIST, MessageColumns.TO_LIST, MessageColumns.RECEPTION_STATE, MessageColumns.DEVILERY_STATE, "thread_id", "thread_name"};
    public static final String[] BT_ACCOUNT_PROJECTION = {"_id", AccountColumns.ACCOUNT_DISPLAY_NAME, AccountColumns.FLAG_EXPOSE};
    public static final String[] BT_IM_ACCOUNT_PROJECTION = {"_id", AccountColumns.ACCOUNT_DISPLAY_NAME, AccountColumns.FLAG_EXPOSE, AccountColumns.ACCOUNT_UCI, AccountColumns.ACCOUNT_UCI_PREFIX};
    public static final String[] BT_FOLDER_PROJECTION = {"_id", "name", "account_id", FolderColumns.PARENT_FOLDER_ID};
    public static final String[] BT_CONVERSATION_PROJECTION = {"thread_id", "thread_name", ConversationColumns.READ_STATUS, ConversationColumns.LAST_THREAD_ACTIVITY, ConversationColumns.VERSION_COUNTER, ConversationColumns.SUMMARY, ConvoContactColumns.UCI, "name", ConvoContactColumns.NICKNAME, ChatStatusColumns.CHAT_STATE, ChatStatusColumns.LAST_ACTIVE, ConvoContactColumns.X_BT_UID, PresenceColumns.PRESENCE_STATE, PresenceColumns.STATUS_TEXT, PresenceColumns.PRIORITY};
    public static final String[] BT_CONTACT_CHATSTATE_PRESENCE_PROJECTION = {ConvoContactColumns.UCI, ConvoContactColumns.CONVO_ID, "name", ConvoContactColumns.NICKNAME, ConvoContactColumns.X_BT_UID, ChatStatusColumns.CHAT_STATE, ChatStatusColumns.LAST_ACTIVE, PresenceColumns.PRESENCE_STATE, PresenceColumns.PRIORITY, PresenceColumns.STATUS_TEXT, PresenceColumns.LAST_ONLINE};
    public static final String[] BT_CONTACT_PROJECTION = {ConvoContactColumns.UCI, ConvoContactColumns.CONVO_ID, ConvoContactColumns.X_BT_UID, "name", ConvoContactColumns.NICKNAME};
    public static final String[] BT_CHATSTATUS_PROJECTION = {ChatStatusColumns.CHAT_STATE, ChatStatusColumns.LAST_ACTIVE};
    public static final String[] BT_PRESENCE_PROJECTION = {PresenceColumns.PRESENCE_STATE, PresenceColumns.PRIORITY, PresenceColumns.STATUS_TEXT, PresenceColumns.LAST_ONLINE};

    public interface AccountColumns {
        public static final String ACCOUNT_DISPLAY_NAME = "account_display_name";
        public static final String ACCOUNT_UCI = "account_uci";
        public static final String ACCOUNT_UCI_PREFIX = "account_uci_PREFIX";
        public static final String FLAG_EXPOSE = "flag_expose";
        public static final String _ID = "_id";
    }

    public interface ChatState {
        public static final int ACITVE = 2;
        public static final int COMPOSING = 3;
        public static final int GONE = 5;
        public static final int INACITVE = 1;
        public static final int PAUSED = 4;
        public static final int UNKNOWN = 0;
    }

    public interface ChatStatusColumns {
        public static final String CHAT_STATE = "chat_state";
        public static final String LAST_ACTIVE = "last_active";
    }

    public interface ConversationColumns extends ConvoContactColumns {
        public static final String LAST_THREAD_ACTIVITY = "last_thread_activity";
        public static final String READ_STATUS = "read_status";
        public static final String SUMMARY = "convo_summary";
        public static final String THREAD_ID = "thread_id";
        public static final String THREAD_NAME = "thread_name";
        public static final String VERSION_COUNTER = "version_counter";
    }

    public interface ConvoContactColumns extends ChatStatusColumns, PresenceColumns {
        public static final String CONVO_ID = "convo_id";
        public static final String NAME = "name";
        public static final String NICKNAME = "nickname";
        public static final String UCI = "x_bt_uci";
        public static final String X_BT_UID = "x_bt_uid";
    }

    public interface EmailMessageColumns {
        public static final String BCC_LIST = "bcc_list";
        public static final String CC_LIST = "cc_list";
        public static final String REPLY_TO_LIST = "reply_to_List";
    }

    public interface FolderColumns {
        public static final String ACCOUNT_ID = "account_id";
        public static final String NAME = "name";
        public static final String PARENT_FOLDER_ID = "parent_id";
        public static final String _ID = "_id";
    }

    public interface MessageColumns extends EmailMessageColumns {
        public static final String ACCOUNT_ID = "account_id";
        public static final String ATTACHMENT_MINE_TYPES = "attachment_mime_types";
        public static final String ATTACHMENT_SIZE = "attachment_size";
        public static final String BODY = "body";
        public static final String DATE = "date";
        public static final String DEVILERY_STATE = "delivery_state";
        public static final String FLAG_ATTACHMENT = "flag_attachment";
        public static final String FLAG_HIGH_PRIORITY = "high_priority";
        public static final String FLAG_PROTECTED = "flag_protected";
        public static final String FLAG_READ = "flag_read";
        public static final String FOLDER_ID = "folder_id";
        public static final String FROM_LIST = "from_list";
        public static final String MESSAGE_SIZE = "message_size";
        public static final String RECEPTION_STATE = "reception_state";
        public static final String SUBJECT = "subject";
        public static final String THREAD_ID = "thread_id";
        public static final String THREAD_NAME = "thread_name";
        public static final String TO_LIST = "to_list";
        public static final String _ID = "_id";
    }

    public interface MessagePartColumns {
        public static final String CHARSET = "charset";
        public static final String CONTENT_ID = "cid";
        public static final String FILENAME = "filename";
        public static final String RAW_DATA = "raw_data";
        public static final String TEXT = "text";
        public static final String _ID = "_id";
    }

    public interface PresenceColumns {
        public static final String LAST_ONLINE = "last_online";
        public static final String PRESENCE_STATE = "presence_state";
        public static final String PRIORITY = "priority";
        public static final String STATUS_TEXT = "status_text";
    }

    public interface PresenceState {
        public static final int AWAY = 3;
        public static final int BUSY = 5;
        public static final int DO_NOT_DISTURB = 4;
        public static final int IN_A_MEETING = 6;
        public static final int OFFLINE = 1;
        public static final int ONLINE = 2;
        public static final int UNKNOWN = 0;
    }

    private BluetoothMapContract() {
    }

    public static Uri buildAccountUri(String str) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(TABLE_ACCOUNT).build();
    }

    public static Uri buildAccountUriwithId(String str, String str2) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(TABLE_ACCOUNT).appendPath(str2).build();
    }

    public static Uri buildMessageUri(String str) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(TABLE_MESSAGE).build();
    }

    public static Uri buildMessageUri(String str, String str2) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_MESSAGE).build();
    }

    public static Uri buildMessageUriWithId(String str, String str2, String str3) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_MESSAGE).appendPath(str3).build();
    }

    public static Uri buildFolderUri(String str, String str2) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_FOLDER).build();
    }

    public static Uri buildConversationUri(String str, String str2) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_CONVERSATION).build();
    }

    public static Uri buildConvoContactsUri(String str) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(TABLE_CONVOCONTACT).build();
    }

    public static Uri buildConvoContactsUri(String str, String str2) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_CONVOCONTACT).build();
    }

    public static Uri buildConvoContactsUriWithId(String str, String str2, String str3) {
        return new Uri.Builder().scheme("content").authority(str).appendPath(str2).appendPath(TABLE_CONVOCONTACT).appendPath(str3).build();
    }
}
