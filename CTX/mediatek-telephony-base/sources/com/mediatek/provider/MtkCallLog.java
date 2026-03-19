package com.mediatek.provider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.Cursor;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.BaseColumns;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import com.android.internal.telephony.CallerInfo;
import java.util.List;
import mediatek.telephony.MtkTelephony;

public class MtkCallLog {
    private static final String LOG_TAG = "MtkCallLog";
    public static final String SHADOW_AUTHORITY = "call_log_shadow";
    private static final boolean VERBOSE_LOG = !MtkTelephony.Carriers.USER.equals(Build.TYPE);

    public static class Calls extends CallLog.Calls {
        public static final int AUTO_REJECT_TYPE = 8;
        public static final String CACHED_INDICATE_PHONE_SIM = "indicate_phone_or_sim_contact";
        public static final String CACHED_IS_SDN_CONTACT = "is_sdn_contact";
        public static final String CONFERENCE_CALL_ID = "conference_call_id";
        public static final String DATA_ID = "data_id";
        private static final int MIN_DURATION_FOR_NORMALIZED_NUMBER_UPDATE_MS = 10000;
        public static final String RAW_CONTACT_ID = "raw_contact_id";
        public static final String SORT_DATE = "sort_date";

        public static Uri addCall(CallerInfo callerInfo, Context context, String str, String str2, String str3, int i, int i2, int i3, PhoneAccountHandle phoneAccountHandle, long j, int i4, Long l, boolean z, UserHandle userHandle, boolean z2, long j2, int i5) {
            TelecomManager telecomManagerFrom;
            String schemeSpecificPart;
            String str4;
            String id;
            String strFlattenToString;
            int i6;
            int i7;
            UserHandle userHandleOf;
            Cursor cursorQuery;
            PhoneAccount phoneAccount;
            Uri subscriptionAddress;
            if (MtkCallLog.VERBOSE_LOG) {
                Log.v(MtkCallLog.LOG_TAG, String.format("Add call: number=%s, user=%s, for all=%s", str, userHandle, Boolean.valueOf(z)));
            }
            ContentResolver contentResolver = context.getContentResolver();
            try {
                telecomManagerFrom = TelecomManager.from(context);
            } catch (UnsupportedOperationException e) {
                telecomManagerFrom = null;
            }
            if (telecomManagerFrom != null && phoneAccountHandle != null && (phoneAccount = telecomManagerFrom.getPhoneAccount(phoneAccountHandle)) != null && (subscriptionAddress = phoneAccount.getSubscriptionAddress()) != null) {
                schemeSpecificPart = subscriptionAddress.getSchemeSpecificPart();
            } else {
                schemeSpecificPart = null;
            }
            int i8 = 4;
            if (i != 2) {
                if (i != 4) {
                    i8 = (TextUtils.isEmpty(str) || i == 3) ? 3 : 1;
                }
            } else {
                i8 = 2;
            }
            if (i8 != 1) {
                str4 = "";
                if (callerInfo != null) {
                    callerInfo.name = "";
                }
            } else {
                str4 = str;
            }
            if (phoneAccountHandle != null) {
                strFlattenToString = phoneAccountHandle.getComponentName().flattenToString();
                id = phoneAccountHandle.getId();
            } else {
                id = null;
                strFlattenToString = null;
            }
            ContentValues contentValues = new ContentValues(6);
            contentValues.put(MtkTelephony.SmsCb.CbChannel.NUMBER, str4);
            contentValues.put("post_dial_digits", str2);
            contentValues.put("via_number", str3);
            contentValues.put("presentation", Integer.valueOf(i8));
            contentValues.put("type", Integer.valueOf(i2));
            contentValues.put("features", Integer.valueOf(i3));
            contentValues.put("date", Long.valueOf(j));
            contentValues.put("duration", Long.valueOf(i4));
            if (l != null) {
                contentValues.put("data_usage", l);
            }
            contentValues.put("subscription_component_name", strFlattenToString);
            contentValues.put("subscription_id", id);
            contentValues.put("phone_account_address", schemeSpecificPart);
            contentValues.put("new", (Integer) 1);
            contentValues.put("add_for_all_users", Integer.valueOf(z ? 1 : 0));
            if (i2 == 3) {
                contentValues.put("is_read", Integer.valueOf(z2 ? 1 : 0));
            }
            if (callerInfo != null) {
                contentValues.put("name", callerInfo.name);
                contentValues.put("numbertype", Integer.valueOf(callerInfo.numberType));
                contentValues.put("numberlabel", callerInfo.numberLabel);
            }
            if (callerInfo != null && callerInfo.contactIdOrZero > 0) {
                if (callerInfo.normalizedNumber != null) {
                    cursorQuery = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"_id"}, "contact_id =? AND data4 =?", new String[]{String.valueOf(callerInfo.contactIdOrZero), callerInfo.normalizedNumber}, null);
                } else {
                    cursorQuery = contentResolver.query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Callable.CONTENT_FILTER_URI, Uri.encode(callerInfo.phoneNumber != null ? callerInfo.phoneNumber : str4)), new String[]{"_id"}, "contact_id =?", new String[]{String.valueOf(callerInfo.contactIdOrZero)}, null);
                }
                Cursor cursor = cursorQuery;
                if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                            i6 = 0;
                            String string = cursor.getString(0);
                            updateDataUsageStatForData(contentResolver, string);
                            if (i4 >= MIN_DURATION_FOR_NORMALIZED_NUMBER_UPDATE_MS && i2 == 2 && TextUtils.isEmpty(callerInfo.normalizedNumber)) {
                                updateNormalizedNumber(context, contentResolver, string, str4);
                            }
                        } else {
                            i6 = 0;
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } else {
                i6 = 0;
            }
            UserManager userManager = (UserManager) context.getSystemService(UserManager.class);
            int userHandle2 = userManager.getUserHandle();
            if (j2 > 0) {
                i7 = i6;
            } else {
                i7 = z ? 1 : 0;
            }
            if (i7 != 0) {
                Uri uriAddEntryAndRemoveExpiredEntries = addEntryAndRemoveExpiredEntries(context, userManager, UserHandle.SYSTEM, contentValues);
                if (uriAddEntryAndRemoveExpiredEntries == null || MtkCallLog.SHADOW_AUTHORITY.equals(uriAddEntryAndRemoveExpiredEntries.getAuthority())) {
                    return null;
                }
                if (userHandle2 != 0) {
                    uriAddEntryAndRemoveExpiredEntries = null;
                }
                List users = userManager.getUsers(true);
                int size = users.size();
                while (i6 < size) {
                    UserHandle userHandle3 = ((UserInfo) users.get(i6)).getUserHandle();
                    int identifier = userHandle3.getIdentifier();
                    if (!userHandle3.isSystem()) {
                        if (!shouldHaveSharedCallLogEntries(context, userManager, identifier)) {
                            if (MtkCallLog.VERBOSE_LOG) {
                                Log.v(MtkCallLog.LOG_TAG, "Shouldn't have calllog entries. userId=" + identifier);
                            }
                        } else if (userManager.isUserRunning(userHandle3) && userManager.isUserUnlocked(userHandle3)) {
                            Uri uriAddEntryAndRemoveExpiredEntries2 = addEntryAndRemoveExpiredEntries(context, userManager, userHandle3, contentValues);
                            if (identifier == userHandle2) {
                                uriAddEntryAndRemoveExpiredEntries = uriAddEntryAndRemoveExpiredEntries2;
                            }
                        }
                    }
                    i6++;
                }
                return uriAddEntryAndRemoveExpiredEntries;
            }
            if (userHandle == null) {
                userHandleOf = UserHandle.of(userHandle2);
            } else {
                userHandleOf = userHandle;
            }
            return addEntryAndRemoveExpiredEntries(context, userManager, userHandleOf, contentValues, j2, i5);
        }

        private static void updateDataUsageStatForData(ContentResolver contentResolver, String str) {
            contentResolver.update(ContactsContract.DataUsageFeedback.FEEDBACK_URI.buildUpon().appendPath(str).appendQueryParameter("type", "call").build(), new ContentValues(), null, null);
        }

        private static void updateNormalizedNumber(Context context, ContentResolver contentResolver, String str, String str2) {
            if (TextUtils.isEmpty(str2) || TextUtils.isEmpty(str) || TextUtils.isEmpty(getCurrentCountryIso(context))) {
                return;
            }
            String numberToE164 = PhoneNumberUtils.formatNumberToE164(str2, getCurrentCountryIso(context));
            if (TextUtils.isEmpty(numberToE164)) {
                return;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("data4", numberToE164);
            contentResolver.update(ContactsContract.Data.CONTENT_URI, contentValues, "_id=?", new String[]{str});
        }

        private static Uri addEntryAndRemoveExpiredEntries(Context context, UserManager userManager, UserHandle userHandle, ContentValues contentValues) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(userManager.isUserUnlocked(userHandle) ? CONTENT_URI : SHADOW_CONTENT_URI, userHandle.getIdentifier());
            if (MtkCallLog.VERBOSE_LOG) {
                Log.v(MtkCallLog.LOG_TAG, String.format("Inserting to %s", uriMaybeAddUserId));
            }
            try {
                Uri uriInsert = contentResolver.insert(uriMaybeAddUserId, contentValues);
                contentResolver.delete(uriMaybeAddUserId, "_id IN (SELECT _id FROM calls ORDER BY date DESC LIMIT -1 OFFSET 500)", null);
                return uriInsert;
            } catch (IllegalArgumentException e) {
                Log.w(MtkCallLog.LOG_TAG, "Failed to insert calllog", e);
                return null;
            }
        }

        private static Uri addEntryAndRemoveExpiredEntries(Context context, UserManager userManager, UserHandle userHandle, ContentValues contentValues, long j, int i) throws Throwable {
            Log.i(MtkCallLog.LOG_TAG, "addEntryAndRemoveExpiredEntries conf id " + j);
            if (j > 0) {
                contentValues.put(CONFERENCE_CALL_ID, Long.valueOf(j));
                ConferenceCalls.updateConferenceDurationIfNeeded(context, userHandle, j, i);
                Uri uriUpdateCallEntryIfExist = updateCallEntryIfExist(context, userManager, userHandle, contentValues, j);
                if (uriUpdateCallEntryIfExist != null) {
                    if (MtkCallLog.VERBOSE_LOG) {
                        Log.i(MtkCallLog.LOG_TAG, "Entry exist, update " + uriUpdateCallEntryIfExist);
                    }
                    return uriUpdateCallEntryIfExist;
                }
            }
            return addEntryAndRemoveExpiredEntries(context, userManager, userHandle, contentValues);
        }

        private static Uri updateCallEntryIfExist(Context context, UserManager userManager, UserHandle userHandle, ContentValues contentValues, long j) throws Throwable {
            Cursor cursorQuery;
            Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(userManager.isUserUnlocked(userHandle) ? CONTENT_URI : SHADOW_CONTENT_URI, userHandle.getIdentifier());
            ContentResolver contentResolver = context.getContentResolver();
            String asString = contentValues.getAsString(MtkTelephony.SmsCb.CbChannel.NUMBER);
            long jLongValue = contentValues.getAsLong("date").longValue();
            if (MtkCallLog.VERBOSE_LOG) {
                Log.i(MtkCallLog.LOG_TAG, "updateCallEntryIfExist + number " + asString + ", date " + jLongValue + " conference call id " + j);
            }
            String[] strArr = {asString, String.valueOf(jLongValue), String.valueOf(j)};
            try {
                cursorQuery = contentResolver.query(uriMaybeAddUserId, new String[]{"_id"}, "number=? AND date=? AND conference_call_id=?", strArr, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() > 0) {
                            contentResolver.update(uriMaybeAddUserId, contentValues, "number=? AND date=? AND conference_call_id=?", strArr);
                            cursorQuery.moveToFirst();
                            Uri uriWithAppendedId = ContentUris.withAppendedId(uriMaybeAddUserId, cursorQuery.getInt(cursorQuery.getColumnIndex("_id")));
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return uriWithAppendedId;
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return null;
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        }

        private static String getCurrentCountryIso(Context context) {
            Country countryDetectCountry;
            CountryDetector countryDetector = (CountryDetector) context.getSystemService("country_detector");
            if (countryDetector != null && (countryDetectCountry = countryDetector.detectCountry()) != null) {
                return countryDetectCountry.getCountryIso();
            }
            return null;
        }
    }

    public static final class ConferenceCalls implements BaseColumns {
        public static final String CONFERENCE_DATE = "conference_date";
        public static final String CONFERENCE_DURATION = "conference_duration";
        public static final Uri CONTENT_URI = Uri.parse("content://call_log/conference_calls");
        public static final String GROUP_ID = "group_id";

        private ConferenceCalls() {
        }

        public static synchronized Uri addConferenceCall(Context context, UserHandle userHandle, long j, int i) {
            Uri uriInsert;
            Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(CONTENT_URI, userHandle.getIdentifier());
            Log.i(MtkCallLog.LOG_TAG, "addConferenceCall " + uriMaybeAddUserId + " date " + j + " duration " + i);
            if (i < 0) {
                i = 0;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put(CONFERENCE_DATE, Long.valueOf(j));
            contentValues.put(CONFERENCE_DURATION, Integer.valueOf(i));
            try {
                uriInsert = context.getContentResolver().insert(uriMaybeAddUserId, contentValues);
            } catch (Exception e) {
                e.printStackTrace();
                uriInsert = null;
            }
            Log.i(MtkCallLog.LOG_TAG, "addConferenceCall result uri " + uriInsert);
            return uriInsert;
        }

        public static synchronized void updateConferenceDurationIfNeeded(Context context, UserHandle userHandle, long j, int i) {
            Cursor cursorQuery;
            Log.i(MtkCallLog.LOG_TAG, "updateConferenceDurationIfNeeded " + userHandle + ", id " + j + " duration " + i);
            Uri uriMaybeAddUserId = ContentProvider.maybeAddUserId(CONTENT_URI, userHandle.getIdentifier());
            StringBuilder sb = new StringBuilder();
            sb.append("Modify uri ");
            sb.append(uriMaybeAddUserId);
            Log.i(MtkCallLog.LOG_TAG, sb.toString());
            ContentResolver contentResolver = context.getContentResolver();
            try {
                cursorQuery = contentResolver.query(uriMaybeAddUserId, new String[]{CONFERENCE_DURATION}, "_id=?", new String[]{String.valueOf(j)}, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() > 0) {
                            cursorQuery.moveToFirst();
                            int i2 = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow(CONFERENCE_DURATION));
                            if (i > i2) {
                                Log.v(MtkCallLog.LOG_TAG, "new: " + i + ", old: " + i2);
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(CONFERENCE_DURATION, Integer.valueOf(i));
                                contentResolver.update(ContentUris.withAppendedId(uriMaybeAddUserId, j), contentValues, null, null);
                            } else {
                                Log.v(MtkCallLog.LOG_TAG, "new: " + i + ", old: " + i2 + ", no need to update.");
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (Throwable th2) {
                th = th2;
                cursorQuery = null;
            }
        }
    }
}
