package android.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.internal.telephony.CallerInfo;
import java.util.List;

public class CallLog {
    public static final String AUTHORITY = "call_log";
    public static final Uri CONTENT_URI = Uri.parse("content://call_log");

    public static class Calls implements BaseColumns {
        public static final String CACHED_FORMATTED_NUMBER = "formatted_number";
        public static final String CACHED_LOOKUP_URI = "lookup_uri";
        public static final String CACHED_MATCHED_NUMBER = "matched_number";
        public static final String CACHED_NAME = "name";
        public static final String CACHED_NORMALIZED_NUMBER = "normalized_number";
        public static final String CACHED_NUMBER_LABEL = "numberlabel";
        public static final String CACHED_NUMBER_TYPE = "numbertype";
        public static final String CACHED_PHOTO_ID = "photo_id";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/calls";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/calls";
        public static final String COUNTRY_ISO = "countryiso";
        public static final String DATA_USAGE = "data_usage";
        public static final String DATE = "date";
        public static final String DEFAULT_SORT_ORDER = "date DESC";
        public static final String DURATION = "duration";
        public static final String EXTRA_CALL_TYPE_FILTER = "android.provider.extra.CALL_TYPE_FILTER";
        public static final String FEATURES = "features";
        public static final int FEATURES_VIDEO = 1;
        public static final String GEOCODED_LOCATION = "geocoded_location";
        public static final int INCOMING_TYPE = 1;
        public static final String IS_READ = "is_read";
        public static final String LIMIT_PARAM_KEY = "limit";
        private static final int MIN_DURATION_FOR_NORMALIZED_NUMBER_UPDATE_MS = 10000;
        public static final int MISSED_TYPE = 3;
        public static final String NEW = "new";
        public static final String NUMBER = "number";
        public static final String NUMBER_PRESENTATION = "presentation";
        public static final String OFFSET_PARAM_KEY = "offset";
        public static final int OUTGOING_TYPE = 2;
        public static final String PHONE_ACCOUNT_COMPONENT_NAME = "subscription_component_name";
        public static final String PHONE_ACCOUNT_ID = "subscription_id";
        public static final int PRESENTATION_ALLOWED = 1;
        public static final int PRESENTATION_PAYPHONE = 4;
        public static final int PRESENTATION_RESTRICTED = 2;
        public static final int PRESENTATION_UNKNOWN = 3;
        public static final String SUB_ID = "sub_id";
        public static final String TRANSCRIPTION = "transcription";
        public static final String TYPE = "type";
        public static final int VOICEMAIL_TYPE = 4;
        public static final String VOICEMAIL_URI = "voicemail_uri";
        public static final Uri CONTENT_URI = Uri.parse("content://call_log/calls");
        public static final Uri CONTENT_FILTER_URI = Uri.parse("content://call_log/calls/filter");
        public static final String ALLOW_VOICEMAILS_PARAM_KEY = "allow_voicemails";
        public static final Uri CONTENT_URI_WITH_VOICEMAIL = CONTENT_URI.buildUpon().appendQueryParameter(ALLOW_VOICEMAILS_PARAM_KEY, "true").build();

        public static Uri addCall(CallerInfo ci, Context context, String number, int presentation, int callType, int features, PhoneAccountHandle accountHandle, long start, int duration, Long dataUsage) {
            return addCall(ci, context, number, presentation, callType, features, accountHandle, start, duration, dataUsage, false);
        }

        public static Uri addCall(CallerInfo ci, Context context, String number, int presentation, int callType, int features, PhoneAccountHandle accountHandle, long start, int duration, Long dataUsage, boolean addForAllUsers) {
            Cursor cursor;
            ContentResolver resolver = context.getContentResolver();
            int numberPresentation = 1;
            if (presentation == 2) {
                numberPresentation = 2;
            } else if (presentation == 4) {
                numberPresentation = 4;
            } else if (TextUtils.isEmpty(number) || presentation == 3) {
                numberPresentation = 3;
            }
            if (numberPresentation != 1) {
                number = ProxyInfo.LOCAL_EXCL_LIST;
                if (ci != null) {
                    ci.name = ProxyInfo.LOCAL_EXCL_LIST;
                }
            }
            String accountComponentString = null;
            String accountId = null;
            if (accountHandle != null) {
                accountComponentString = accountHandle.getComponentName().flattenToString();
                accountId = accountHandle.getId();
            }
            ContentValues values = new ContentValues(6);
            values.put("number", number);
            values.put(NUMBER_PRESENTATION, Integer.valueOf(numberPresentation));
            values.put("type", Integer.valueOf(callType));
            values.put(FEATURES, Integer.valueOf(features));
            values.put("date", Long.valueOf(start));
            values.put("duration", Long.valueOf(duration));
            if (dataUsage != null) {
                values.put(DATA_USAGE, dataUsage);
            }
            values.put(PHONE_ACCOUNT_COMPONENT_NAME, accountComponentString);
            values.put("subscription_id", accountId);
            values.put(NEW, (Integer) 1);
            if (callType == 3) {
                values.put("is_read", (Integer) 0);
            }
            if (ci != null) {
                values.put("name", ci.name);
                values.put(CACHED_NUMBER_TYPE, Integer.valueOf(ci.numberType));
                values.put(CACHED_NUMBER_LABEL, ci.numberLabel);
            }
            if (ci != null && ci.contactIdOrZero > 0) {
                if (ci.normalizedNumber != null) {
                    String normalizedPhoneNumber = ci.normalizedNumber;
                    cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"_id"}, "contact_id =? AND data4 =?", new String[]{String.valueOf(ci.contactIdOrZero), normalizedPhoneNumber}, null);
                } else {
                    String phoneNumber = ci.phoneNumber != null ? ci.phoneNumber : number;
                    cursor = resolver.query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Callable.CONTENT_FILTER_URI, Uri.encode(phoneNumber)), new String[]{"_id"}, "contact_id =?", new String[]{String.valueOf(ci.contactIdOrZero)}, null);
                }
                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                            String dataId = cursor.getString(0);
                            updateDataUsageStatForData(resolver, dataId);
                            if (duration >= 10000 && callType == 2 && TextUtils.isEmpty(ci.normalizedNumber)) {
                                updateNormalizedNumber(context, resolver, dataId, number);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
            Uri result = null;
            if (addForAllUsers) {
                UserManager userManager = (UserManager) context.getSystemService("user");
                List<UserInfo> users = userManager.getUsers(true);
                int currentUserId = userManager.getUserHandle();
                int count = users.size();
                for (int i = 0; i < count; i++) {
                    UserInfo user = users.get(i);
                    UserHandle userHandle = user.getUserHandle();
                    if (userManager.isUserRunning(userHandle) && !userManager.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS, userHandle) && !user.isManagedProfile()) {
                        Uri uri = addEntryAndRemoveExpiredEntries(context, ContentProvider.maybeAddUserId(CONTENT_URI, user.id), values);
                        if (user.id == currentUserId) {
                            result = uri;
                        }
                    }
                }
                return result;
            }
            Uri result2 = addEntryAndRemoveExpiredEntries(context, CONTENT_URI, values);
            return result2;
        }

        public static String getLastOutgoingCall(Context context) {
            String string;
            ContentResolver resolver = context.getContentResolver();
            Cursor c = null;
            try {
                c = resolver.query(CONTENT_URI, new String[]{"number"}, "type = 2", null, "date DESC LIMIT 1");
                if (c == null || !c.moveToFirst()) {
                    string = ProxyInfo.LOCAL_EXCL_LIST;
                } else {
                    string = c.getString(0);
                    if (c != null) {
                        c.close();
                    }
                }
                return string;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }

        private static Uri addEntryAndRemoveExpiredEntries(Context context, Uri uri, ContentValues values) {
            ContentResolver resolver = context.getContentResolver();
            Uri result = resolver.insert(uri, values);
            resolver.delete(uri, "_id IN (SELECT _id FROM calls ORDER BY date DESC LIMIT -1 OFFSET 500)", null);
            return result;
        }

        private static void updateDataUsageStatForData(ContentResolver resolver, String dataId) {
            Uri feedbackUri = ContactsContract.DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(dataId).appendQueryParameter("type", "call").build();
            resolver.update(feedbackUri, new ContentValues(), null, null);
        }

        private static void updateNormalizedNumber(Context context, ContentResolver resolver, String dataId, String number) {
            if (!TextUtils.isEmpty(number) && !TextUtils.isEmpty(dataId)) {
                String countryIso = getCurrentCountryIso(context);
                if (!TextUtils.isEmpty(countryIso)) {
                    String normalizedNumber = PhoneNumberUtils.formatNumberToE164(number, getCurrentCountryIso(context));
                    if (!TextUtils.isEmpty(normalizedNumber)) {
                        ContentValues values = new ContentValues();
                        values.put("data4", normalizedNumber);
                        resolver.update(ContactsContract.Data.CONTENT_URI, values, "_id=?", new String[]{dataId});
                    }
                }
            }
        }

        private static String getCurrentCountryIso(Context context) {
            Country country;
            CountryDetector detector = (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
            if (detector == null || (country = detector.detectCountry()) == null) {
                return null;
            }
            String countryIso = country.getCountryIso();
            return countryIso;
        }
    }
}
