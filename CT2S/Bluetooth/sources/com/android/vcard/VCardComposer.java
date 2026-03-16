package com.android.vcard;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Entity;
import android.content.EntityIterator;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.bluetooth.opp.BluetoothShare;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCardComposer {
    private static final boolean DEBUG = false;
    public static final String FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO = "Failed to get database information";
    public static final String FAILURE_REASON_NOT_INITIALIZED = "The vCard composer object is not correctly initialized";
    public static final String FAILURE_REASON_NO_ENTRY = "There's no exportable in the database";
    public static final String FAILURE_REASON_UNSUPPORTED_URI = "The Uri vCard composer received is not supported by the composer.";
    private static final String LOG_TAG = "VCardComposer";
    public static final String NO_ERROR = "No error";
    private static final String SHIFT_JIS = "SHIFT_JIS";
    private static final String UTF_8 = "UTF-8";
    private static final String[] sContactsProjection;
    private static final Map<Integer, String> sImMap = new HashMap();
    private final String mCharset;
    private final ContentResolver mContentResolver;
    private Uri mContentUriForRawContactsEntity;
    private Cursor mCursor;
    private boolean mCursorSuppliedFromOutside;
    private String mErrorReason;
    private boolean mFirstVCardEmittedInDoCoMoCase;
    private int mIdColumn;
    private boolean mInitDone;
    private final boolean mIsDoCoMo;
    private VCardPhoneNumberTranslationCallback mPhoneTranslationCallback;
    private boolean mTerminateCalled;
    private final int mVCardType;

    static {
        sImMap.put(0, VCardConstants.PROPERTY_X_AIM);
        sImMap.put(1, VCardConstants.PROPERTY_X_MSN);
        sImMap.put(2, VCardConstants.PROPERTY_X_YAHOO);
        sImMap.put(6, VCardConstants.PROPERTY_X_ICQ);
        sImMap.put(7, VCardConstants.PROPERTY_X_JABBER);
        sImMap.put(3, VCardConstants.PROPERTY_X_SKYPE_USERNAME);
        sContactsProjection = new String[]{"_id"};
    }

    public VCardComposer(Context context) {
        this(context, VCardConfig.VCARD_TYPE_DEFAULT, null, true);
    }

    public VCardComposer(Context context, int vcardType) {
        this(context, vcardType, null, true);
    }

    public VCardComposer(Context context, int vcardType, String charset) {
        this(context, vcardType, charset, true);
    }

    public VCardComposer(Context context, int vcardType, boolean careHandlerErrors) {
        this(context, vcardType, null, careHandlerErrors);
    }

    public VCardComposer(Context context, int vcardType, String charset, boolean careHandlerErrors) {
        this(context, context.getContentResolver(), vcardType, charset, careHandlerErrors);
    }

    public VCardComposer(Context context, ContentResolver resolver, int vcardType, String charset, boolean careHandlerErrors) {
        boolean shouldAppendCharsetParam = true;
        this.mErrorReason = NO_ERROR;
        this.mTerminateCalled = true;
        this.mVCardType = vcardType;
        this.mContentResolver = resolver;
        this.mIsDoCoMo = VCardConfig.isDoCoMo(vcardType);
        charset = TextUtils.isEmpty(charset) ? "UTF-8" : charset;
        if (VCardConfig.isVersion30(vcardType) && "UTF-8".equalsIgnoreCase(charset)) {
            shouldAppendCharsetParam = DEBUG;
        }
        if (this.mIsDoCoMo || shouldAppendCharsetParam) {
            if (!SHIFT_JIS.equalsIgnoreCase(charset) && TextUtils.isEmpty(charset)) {
                this.mCharset = SHIFT_JIS;
            } else {
                this.mCharset = charset;
            }
        } else if (TextUtils.isEmpty(charset)) {
            this.mCharset = "UTF-8";
        } else {
            this.mCharset = charset;
        }
        Log.d(LOG_TAG, "Use the charset \"" + this.mCharset + "\"");
    }

    public boolean init() {
        return init(null, null);
    }

    @Deprecated
    public boolean initWithRawContactsEntityUri(Uri contentUriForRawContactsEntity) {
        return init(ContactsContract.Contacts.CONTENT_URI, sContactsProjection, null, null, null, contentUriForRawContactsEntity);
    }

    public boolean init(String selection, String[] selectionArgs) {
        return init(ContactsContract.Contacts.CONTENT_URI, sContactsProjection, selection, selectionArgs, null, null);
    }

    public boolean init(Uri contentUri, String selection, String[] selectionArgs, String sortOrder) {
        return init(contentUri, sContactsProjection, selection, selectionArgs, sortOrder, null);
    }

    public boolean init(Uri contentUri, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        return init(contentUri, sContactsProjection, selection, selectionArgs, sortOrder, contentUriForRawContactsEntity);
    }

    public boolean init(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        if ("com.android.contacts".equals(contentUri.getAuthority())) {
            return (initInterFirstPart(contentUriForRawContactsEntity) && initInterCursorCreationPart(contentUri, projection, selection, selectionArgs, sortOrder) && initInterMainPart()) ? initInterLastPart() : DEBUG;
        }
        this.mErrorReason = FAILURE_REASON_UNSUPPORTED_URI;
        return DEBUG;
    }

    public boolean init(Cursor cursor) {
        if (!initInterFirstPart(null)) {
            return DEBUG;
        }
        this.mCursorSuppliedFromOutside = true;
        this.mCursor = cursor;
        return initInterMainPart() ? initInterLastPart() : DEBUG;
    }

    private boolean initInterFirstPart(Uri contentUriForRawContactsEntity) {
        if (contentUriForRawContactsEntity == null) {
            contentUriForRawContactsEntity = ContactsContract.RawContactsEntity.CONTENT_URI;
        }
        this.mContentUriForRawContactsEntity = contentUriForRawContactsEntity;
        if (this.mInitDone) {
            Log.e(LOG_TAG, "init() is already called");
            return DEBUG;
        }
        return true;
    }

    private boolean initInterCursorCreationPart(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.mCursorSuppliedFromOutside = DEBUG;
        this.mCursor = this.mContentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
        if (this.mCursor != null) {
            return true;
        }
        Log.e(LOG_TAG, String.format("Cursor became null unexpectedly", new Object[0]));
        this.mErrorReason = FAILURE_REASON_FAILED_TO_GET_DATABASE_INFO;
        return DEBUG;
    }

    private boolean initInterMainPart() {
        if (this.mCursor.getCount() == 0 || !this.mCursor.moveToFirst()) {
            closeCursorIfAppropriate();
            return DEBUG;
        }
        this.mIdColumn = this.mCursor.getColumnIndex("_id");
        if (this.mIdColumn >= 0) {
            return true;
        }
        return DEBUG;
    }

    private boolean initInterLastPart() {
        this.mInitDone = true;
        this.mTerminateCalled = DEBUG;
        return true;
    }

    public String createOneEntry() {
        return createOneEntry(null);
    }

    public String createOneEntry(Method getEntityIteratorMethod) {
        if (this.mIsDoCoMo && !this.mFirstVCardEmittedInDoCoMoCase) {
            this.mFirstVCardEmittedInDoCoMoCase = true;
        }
        String vcard = createOneEntryInternal(this.mCursor.getString(this.mIdColumn), getEntityIteratorMethod);
        if (!this.mCursor.moveToNext()) {
            Log.e(LOG_TAG, "Cursor#moveToNext() returned false");
        }
        return vcard;
    }

    private String createOneEntryInternal(String contactId, Method getEntityIteratorMethod) {
        Map<String, List<ContentValues>> contentValuesListMap = new HashMap<>();
        EntityIterator entityIterator = null;
        try {
            Uri uri = this.mContentUriForRawContactsEntity;
            String[] selectionArgs = {contactId};
            if (getEntityIteratorMethod != null) {
                try {
                    try {
                        try {
                            entityIterator = (EntityIterator) getEntityIteratorMethod.invoke(null, this.mContentResolver, uri, "contact_id=?", selectionArgs, null);
                        } catch (InvocationTargetException e) {
                            Log.e(LOG_TAG, "InvocationTargetException has been thrown: ", e);
                            throw new RuntimeException("InvocationTargetException has been thrown");
                        }
                    } catch (IllegalAccessException e2) {
                        Log.e(LOG_TAG, "IllegalAccessException has been thrown: " + e2.getMessage());
                    }
                } catch (IllegalArgumentException e3) {
                    Log.e(LOG_TAG, "IllegalArgumentException has been thrown: " + e3.getMessage());
                }
            } else {
                entityIterator = ContactsContract.RawContacts.newEntityIterator(this.mContentResolver.query(uri, null, "contact_id=?", selectionArgs, null));
            }
            if (entityIterator == null) {
                Log.e(LOG_TAG, "EntityIterator is null");
            }
            if (!entityIterator.hasNext()) {
                Log.w(LOG_TAG, "Data does not exist. contactId: " + contactId);
                if (entityIterator == null) {
                    return "";
                }
                entityIterator.close();
                return "";
            }
            while (entityIterator.hasNext()) {
                Entity entity = (Entity) entityIterator.next();
                for (Entity.NamedContentValues namedContentValues : entity.getSubValues()) {
                    ContentValues contentValues = namedContentValues.values;
                    String key = contentValues.getAsString(BluetoothShare.MIMETYPE);
                    if (key != null) {
                        List<ContentValues> contentValuesList = contentValuesListMap.get(key);
                        if (contentValuesList == null) {
                            contentValuesList = new ArrayList<>();
                            contentValuesListMap.put(key, contentValuesList);
                        }
                        contentValuesList.add(contentValues);
                    }
                }
            }
            if (entityIterator != null) {
                entityIterator.close();
            }
            return buildVCard(contentValuesListMap);
        } finally {
            if (0 != 0) {
                entityIterator.close();
            }
        }
    }

    public void setPhoneNumberTranslationCallback(VCardPhoneNumberTranslationCallback callback) {
        this.mPhoneTranslationCallback = callback;
    }

    public String buildVCard(Map<String, List<ContentValues>> contentValuesListMap) {
        if (contentValuesListMap == null) {
            Log.e(LOG_TAG, "The given map is null. Ignore and return empty String");
            return "";
        }
        VCardBuilder builder = new VCardBuilder(this.mVCardType, this.mCharset);
        builder.appendNameProperties(contentValuesListMap.get("vnd.android.cursor.item/name")).appendNickNames(contentValuesListMap.get("vnd.android.cursor.item/nickname")).appendPhones(contentValuesListMap.get("vnd.android.cursor.item/phone_v2"), this.mPhoneTranslationCallback).appendEmails(contentValuesListMap.get("vnd.android.cursor.item/email_v2")).appendPostals(contentValuesListMap.get("vnd.android.cursor.item/postal-address_v2")).appendOrganizations(contentValuesListMap.get("vnd.android.cursor.item/organization")).appendWebsites(contentValuesListMap.get("vnd.android.cursor.item/website"));
        if ((this.mVCardType & VCardConfig.FLAG_REFRAIN_IMAGE_EXPORT) == 0) {
            builder.appendPhotos(contentValuesListMap.get("vnd.android.cursor.item/photo"));
        }
        builder.appendNotes(contentValuesListMap.get("vnd.android.cursor.item/note")).appendEvents(contentValuesListMap.get("vnd.android.cursor.item/contact_event")).appendIms(contentValuesListMap.get("vnd.android.cursor.item/im")).appendSipAddresses(contentValuesListMap.get("vnd.android.cursor.item/sip_address")).appendRelation(contentValuesListMap.get("vnd.android.cursor.item/relation"));
        return builder.toString();
    }

    public void terminate() {
        closeCursorIfAppropriate();
        this.mTerminateCalled = true;
    }

    private void closeCursorIfAppropriate() {
        if (!this.mCursorSuppliedFromOutside && this.mCursor != null) {
            try {
                this.mCursor.close();
            } catch (SQLiteException e) {
                Log.e(LOG_TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
            }
            this.mCursor = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.mTerminateCalled) {
                Log.e(LOG_TAG, "finalized() is called before terminate() being called");
            }
        } finally {
            super.finalize();
        }
    }

    public int getCount() {
        if (this.mCursor != null) {
            return this.mCursor.getCount();
        }
        Log.w(LOG_TAG, "This object is not ready yet.");
        return 0;
    }

    public boolean isAfterLast() {
        if (this.mCursor != null) {
            return this.mCursor.isAfterLast();
        }
        Log.w(LOG_TAG, "This object is not ready yet.");
        return DEBUG;
    }

    public String getErrorReason() {
        return this.mErrorReason;
    }
}
