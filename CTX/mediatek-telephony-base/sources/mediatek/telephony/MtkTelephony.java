package mediatek.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SqliteWrapper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.SmsCbMessage;
import android.telephony.SubscriptionManager;
import java.util.Set;

public final class MtkTelephony {
    public static final String SMS_STATE_CHANGED_ACTION = "android.provider.Telephony.SMS_STATE_CHANGED";
    private static final String TAG = "MtkTelephony";

    public static final class MtkMwi implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://mwimsg");
        public static final String FROM = "from_account";
        public static final String GOT_CONTENT = "got_content";
        public static final String MSG_ACCOUNT = "msg_account";
        public static final String MSG_CONTEXT = "msg_context";
        public static final String MSG_DATE = "msg_date";
        public static final String MSG_ID = "msg_id";
        public static final String PRIORITY = "priority";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String SUBJECT = "subject";
        public static final String TO = "to_account";
    }

    public static final class MtkThreadSettings implements BaseColumns {
        public static final String MUTE = "mute";
        public static final String MUTE_START = "mute_start";
        public static final String NOTIFICATION_ENABLE = "notification_enable";
        public static final String RINGTONE = "ringtone";
        public static final String SPAM = "spam";
        public static final String THREAD_ID = "thread_id";
        public static final String TOP = "top";
        public static final String VIBRATE = "vibrate";
        public static final String WALLPAPER = "_data";
    }

    public interface TextBasedSmsCbColumns {
        public static final String BODY = "body";
        public static final String CHANNEL_ID = "channel_id";
        public static final String DATE = "date";
        public static final String LOCKED = "locked";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String SUBSCRIPTION_ID = "sub_id";
        public static final String THREAD_ID = "thread_id";
    }

    public static final class WapPush implements BaseColumns {
        public static final String ACTION = "action";
        public static final String ADDR = "address";
        public static final Uri CONTENT_URI = Uri.parse("content://wappush");
        public static final Uri CONTENT_URI_SI = Uri.parse("content://wappush/si");
        public static final Uri CONTENT_URI_SL = Uri.parse("content://wappush/sl");
        public static final Uri CONTENT_URI_THREAD = Uri.parse("content://wappush/thread_id");
        public static final String CREATE = "created";
        public static final String DATE = "date";
        public static final String DEFAULT_SORT_ORDER = "date ASC";
        public static final String ERROR = "error";
        public static final String EXPIRATION = "expiration";
        public static final String LOCKED = "locked";
        public static final String READ = "read";
        public static final String SEEN = "seen";
        public static final String SERVICE_ADDR = "service_center";
        public static final String SIID = "siid";
        public static final int STATUS_LOCKED = 1;
        public static final int STATUS_READ = 1;
        public static final int STATUS_SEEN = 1;
        public static final int STATUS_UNLOCKED = 0;
        public static final int STATUS_UNREAD = 0;
        public static final int STATUS_UNSEEN = 0;
        public static final String SUBSCRIPTION_ID = "sub_id";
        public static final String TEXT = "text";
        public static final String THREAD_ID = "thread_id";
        public static final String TYPE = "type";
        public static final int TYPE_SI = 0;
        public static final int TYPE_SL = 1;
        public static final String URL = "url";
    }

    private MtkTelephony() {
    }

    public static final class SmsCb implements BaseColumns, TextBasedSmsCbColumns {
        public static final String DEFAULT_SORT_ORDER = "date DESC";
        public static final Uri CONTENT_URI = Uri.parse("content://cb/messages");
        public static final Uri ADDRESS_URI = Uri.parse("content://cb/addresses");

        public interface CanonicalAddressesColumns extends BaseColumns {
            public static final String ADDRESS = "address";
        }

        public static final class CbChannel implements BaseColumns {
            public static final Uri CONTENT_URI = Uri.parse("content://cb/channel");
            public static final String ENABLE = "enable";
            public static final String NAME = "name";
            public static final String NUMBER = "number";
        }

        public static final class Conversations implements BaseColumns, TextBasedSmsCbColumns {
            public static final String ADDRESS_ID = "address_id";
            public static final Uri CONTENT_URI = Uri.parse("content://cb/threads");
            public static final String DEFAULT_SORT_ORDER = "date DESC";
            public static final String MESSAGE_COUNT = "msg_count";
            public static final String SNIPPET = "snippet";
        }

        public static final Cursor query(ContentResolver contentResolver, String[] strArr) {
            return contentResolver.query(CONTENT_URI, strArr, null, null, "date DESC");
        }

        public static final Cursor query(ContentResolver contentResolver, String[] strArr, String str, String str2) {
            Uri uri = CONTENT_URI;
            if (str2 == null) {
                str2 = "date DESC";
            }
            return contentResolver.query(uri, strArr, str, null, str2);
        }

        public static Uri addMessageToUri(int i, ContentResolver contentResolver, Uri uri, int i2, long j, boolean z, String str) {
            ContentValues contentValues = new ContentValues(5);
            contentValues.put("sub_id", Integer.valueOf(i));
            contentValues.put("date", Long.valueOf(j));
            contentValues.put("read", Integer.valueOf(z ? 1 : 0));
            contentValues.put(TextBasedSmsCbColumns.BODY, str);
            contentValues.put(TextBasedSmsCbColumns.CHANNEL_ID, Integer.valueOf(i2));
            return contentResolver.insert(uri, contentValues);
        }

        public static final class Intents {
            public static final SmsCbMessage[] getMessagesFromIntent(Intent intent) {
                SmsCbMessage[] parcelableArrayExtra = intent.getParcelableArrayExtra("message");
                if (parcelableArrayExtra == null) {
                    return null;
                }
                SmsCbMessage[] smsCbMessageArr = new SmsCbMessage[parcelableArrayExtra.length];
                for (int i = 0; i < parcelableArrayExtra.length; i++) {
                    smsCbMessageArr[i] = parcelableArrayExtra[i];
                }
                return smsCbMessageArr;
            }
        }
    }

    public static class MtkCellBroadcasts implements BaseColumns {
        public static final String[] QUERY_COLUMNS = {"_id", "geo_scope", "plmn", "lac", "cid", "serial_number", "service_category", "language", TextBasedSmsCbColumns.BODY, "date", "read", "format", MtkMwi.PRIORITY, "etws_warning_type", "cmas_message_class", "cmas_category", "cmas_response_type", "cmas_severity", "cmas_urgency", "cmas_certainty", "sub_id"};
        public static final String SUBSCRIPTION_ID = "sub_id";

        private MtkCellBroadcasts() {
        }
    }

    public static class MtkSms implements BaseColumns, Telephony.TextBasedSmsColumns {
        public static final String IPMSG_ID = "ipmsg_id";
        public static final String RECEIVED_LENGTH = "rec_len";
        public static final String RECEIVED_TIME = "recv_time";
        public static final String REFERENCE_ID = "ref_id";
        public static final int STATUS_REPLACED_BY_SC = 2;
        public static final String TOTAL_LENGTH = "total_len";
        public static final String UPLOAD_FLAG = "upload_flag";

        public static Uri addMessageToUri(int i, ContentResolver contentResolver, Uri uri, String str, String str2, String str3, String str4, Long l, boolean z, boolean z2, long j) {
            ContentValues contentValues = new ContentValues(9);
            Rlog.v(MtkTelephony.TAG, "Telephony addMessageToUri sub id: " + i);
            contentValues.put("sub_id", Integer.valueOf(i));
            contentValues.put("address", str);
            if (l != null) {
                contentValues.put("date", l);
            }
            if (str4 != null) {
                contentValues.put("service_center", str4);
            }
            contentValues.put("read", Integer.valueOf(z ? 1 : 0));
            contentValues.put(MtkMwi.SUBJECT, str3);
            contentValues.put(TextBasedSmsCbColumns.BODY, str2);
            if (z2) {
                contentValues.put(MtkThreads.STATUS, (Integer) 32);
            }
            if (j != -1) {
                contentValues.put("thread_id", Long.valueOf(j));
            }
            return contentResolver.insert(uri, contentValues);
        }

        public static final class Inbox implements BaseColumns, Telephony.TextBasedSmsColumns {
            private Inbox() {
            }

            public static Uri addMessage(int i, ContentResolver contentResolver, String str, String str2, String str3, String str4, Long l, boolean z) {
                return MtkSms.addMessageToUri(i, contentResolver, Telephony.Sms.Inbox.CONTENT_URI, str, str2, str3, str4, l, z, false, -1L);
            }
        }

        public static final class Sent implements BaseColumns, Telephony.TextBasedSmsColumns {
            private Sent() {
            }

            public static Uri addMessage(int i, ContentResolver contentResolver, String str, String str2, String str3, String str4, Long l) {
                return MtkSms.addMessageToUri(i, contentResolver, Telephony.Sms.Sent.CONTENT_URI, str, str2, str3, str4, l, true, false, -1L);
            }
        }

        public static final class Intents {
            private Intents() {
            }

            public static MtkSmsMessage[] getMessagesFromIntent(Intent intent) {
                try {
                    Object[] objArr = (Object[]) intent.getSerializableExtra("pdus");
                    if (objArr == null) {
                        Rlog.e(MtkTelephony.TAG, "pdus does not exist in the intent");
                        return null;
                    }
                    String stringExtra = intent.getStringExtra("format");
                    int intExtra = intent.getIntExtra("subscription", SubscriptionManager.getDefaultSmsSubscriptionId());
                    Rlog.v(MtkTelephony.TAG, " getMessagesFromIntent sub_id : " + intExtra);
                    int length = objArr.length;
                    MtkSmsMessage[] mtkSmsMessageArr = new MtkSmsMessage[length];
                    for (int i = 0; i < length; i++) {
                        mtkSmsMessageArr[i] = MtkSmsMessage.createFromPdu((byte[]) objArr[i], stringExtra);
                        if (mtkSmsMessageArr[i] != null) {
                            mtkSmsMessageArr[i].setSubId(intExtra);
                        }
                    }
                    return mtkSmsMessageArr;
                } catch (ClassCastException e) {
                    Rlog.e(MtkTelephony.TAG, "getMessagesFromIntent: " + e);
                    return null;
                }
            }
        }
    }

    public static final class MtkThreads implements Telephony.ThreadsColumns {
        public static final int CELL_BROADCAST_THREAD = 3;
        public static final String DATE_SENT = "date_sent";
        public static final int IP_MESSAGE_GUIDE_THREAD = 10;
        public static final String LATEST_IMPORTANT_DATE = "li_date";
        public static final String LATEST_IMPORTANT_SNIPPET = "li_snippet";
        public static final String LATEST_IMPORTANT_SNIPPET_CHARSET = "li_snippet_cs";
        public static final String READ_COUNT = "readcount";
        public static final String STATUS = "status";
        public static final int WAPPUSH_THREAD = 2;
        private static final String[] ID_PROJECTION = {"_id"};
        private static final Uri THREAD_ID_CONTENT_URI = Uri.parse("content://mms-sms/threadID");

        private MtkThreads() {
        }

        public static long getOrCreateThreadId(Context context, Set<String> set, String str) {
            Uri.Builder builderBuildUpon = THREAD_ID_CONTENT_URI.buildUpon();
            if (str != null && str.length() > 0) {
                builderBuildUpon.appendQueryParameter("backupRestoreIndex", str);
            }
            for (String strExtractAddrSpec : set) {
                if (Telephony.Mms.isEmailAddress(strExtractAddrSpec)) {
                    strExtractAddrSpec = Telephony.Mms.extractAddrSpec(strExtractAddrSpec);
                }
                builderBuildUpon.appendQueryParameter("recipient", strExtractAddrSpec);
            }
            Uri uriBuild = builderBuildUpon.build();
            Cursor cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), uriBuild, ID_PROJECTION, (String) null, (String[]) null, (String) null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        Rlog.d(MtkTelephony.TAG, "getOrCreateThreadId for BackupRestore threadId = " + cursorQuery.getLong(0));
                        return cursorQuery.getLong(0);
                    }
                    Rlog.e(MtkTelephony.TAG, "getOrCreateThreadId for BackupRestore returned no rows!");
                } finally {
                    cursorQuery.close();
                }
            }
            Rlog.e(MtkTelephony.TAG, "getOrCreateThreadId for BackupRestore failed with uri " + uriBuild.toString());
            throw new IllegalArgumentException("Unable to find or allocate a thread ID.");
        }

        public static long getThreadId(Context context, String str) {
            Uri.Builder builderBuildUpon = Uri.parse("content://sms/thread_id").buildUpon();
            if (Telephony.Mms.isEmailAddress(str)) {
                str = Telephony.Mms.extractAddrSpec(str);
            }
            builderBuildUpon.appendQueryParameter("recipient", str);
            Uri uriBuild = builderBuildUpon.build();
            Cursor cursorQuery = SqliteWrapper.query(context, context.getContentResolver(), uriBuild, ID_PROJECTION, (String) null, (String[]) null, (String) null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        return cursorQuery.getLong(0);
                    }
                    Rlog.e(MtkTelephony.TAG, "getThreadId returned no rows!");
                } finally {
                    cursorQuery.close();
                }
            }
            Rlog.e(MtkTelephony.TAG, "getThreadId failed with uri " + uriBuild.toString());
            throw new IllegalArgumentException("Unable to find a thread ID.");
        }
    }

    public static final class MtkMmsSms implements BaseColumns {
        public static final Uri CONTENT_URI_QUICKTEXT = Uri.parse("content://mms-sms/quicktext");

        private MtkMmsSms() {
        }
    }

    public static final class MtkMms {
        public static final String SERVICE_CENTER = "service_center";
        public static final String STATUS_EXT = "st_ext";

        private MtkMms() {
        }
    }

    public static final class Carriers implements BaseColumns {
        public static final String APN = "apn";
        public static final String APN_SET_ID = "apn_set_id";
        public static final String AUTH_TYPE = "authtype";

        @Deprecated
        public static final String BEARER = "bearer";

        @Deprecated
        public static final String BEARER_BITMASK = "bearer_bitmask";
        public static final int CARRIER_DELETED = 5;
        public static final int CARRIER_DELETED_BUT_PRESENT_IN_XML = 6;
        public static final int CARRIER_EDITED = 4;
        public static final String CARRIER_ENABLED = "carrier_enabled";
        public static final String CSD_NUM = "csdnum";
        public static final String CURRENT = "current";
        public static final String DEFAULT_SORT_ORDER = "name ASC";
        public static final String EDITED = "edited";
        public static final String ENFORCE_KEY = "enforced";
        public static final String IMSI = "imsi";
        public static final String MAX_CONNS = "max_conns";
        public static final String MAX_CONNS_TIME = "max_conns_time";
        public static final String MCC = "mcc";
        public static final String MMSC = "mmsc";
        public static final String MMSPORT = "mmsport";
        public static final String MMSPROXY = "mmsproxy";
        public static final String MNC = "mnc";
        public static final String MODEM_COGNITIVE = "modem_cognitive";
        public static final String MTU = "mtu";
        public static final String MVNO_MATCH_DATA = "mvno_match_data";
        public static final String MVNO_TYPE = "mvno_type";
        public static final String NAME = "name";
        public static final String NAP_ID = "napid";
        public static final String NETWORK_TYPE_BITMASK = "network_type_bitmask";
        public static final int NO_SET_SET = 0;
        public static final String NUMERIC = "numeric";
        public static final String OMACP_ID = "omacpid";
        public static final String OWNED_BY = "owned_by";
        public static final int OWNED_BY_DPC = 0;
        public static final int OWNED_BY_OTHERS = 1;
        public static final String PASSWORD = "password";
        public static final String PNN = "pnn";
        public static final String PORT = "port";
        public static final String PPP = "ppp";
        public static final String PROFILE_ID = "profile_id";
        public static final String PROTOCOL = "protocol";
        public static final String PROXY = "proxy";
        public static final String PROXY_ID = "proxyid";
        public static final String ROAMING_PROTOCOL = "roaming_protocol";
        public static final String SERVER = "server";
        public static final String SOURCE_TYPE = "sourcetype";
        public static final String SPN = "spn";
        public static final String SUBSCRIPTION_ID = "sub_id";
        public static final String TYPE = "type";
        public static final int UNEDITED = 0;
        public static final String USER = "user";
        public static final int USER_DELETED = 2;
        public static final int USER_DELETED_BUT_PRESENT_IN_XML = 3;
        public static final String USER_EDITABLE = "user_editable";
        public static final int USER_EDITED = 1;
        public static final String USER_VISIBLE = "user_visible";
        public static final String WAIT_TIME = "wait_time";
        public static final Uri CONTENT_URI = Uri.parse("content://telephony/carriers");
        public static final Uri DPC_URI = Uri.parse("content://telephony/carriers/dpc");
        public static final Uri FILTERED_URI = Uri.parse("content://telephony/carriers/filtered");
        public static final Uri ENFORCE_MANAGED_URI = Uri.parse("content://telephony/carriers/enforce_managed");
        public static final Uri CONTENT_URI_DM = Uri.parse("content://telephony/carriers_dm");

        private Carriers() {
        }
    }
}
