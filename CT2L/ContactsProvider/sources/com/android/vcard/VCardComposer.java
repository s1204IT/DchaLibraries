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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCardComposer {
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
        sImMap.put(0, "X-AIM");
        sImMap.put(1, "X-MSN");
        sImMap.put(2, "X-YAHOO");
        sImMap.put(6, "X-ICQ");
        sImMap.put(7, "X-JABBER");
        sImMap.put(3, "X-SKYPE-USERNAME");
        sContactsProjection = new String[]{"_id"};
    }

    public VCardComposer(Context context, int vcardType, boolean careHandlerErrors) {
        this(context, vcardType, null, careHandlerErrors);
    }

    public VCardComposer(Context context, int vcardType, String charset, boolean careHandlerErrors) {
        this(context, context.getContentResolver(), vcardType, charset, careHandlerErrors);
    }

    public VCardComposer(Context context, ContentResolver resolver, int vcardType, String charset, boolean careHandlerErrors) {
        boolean shouldAppendCharsetParam = true;
        this.mErrorReason = "No error";
        this.mTerminateCalled = true;
        this.mVCardType = vcardType;
        this.mContentResolver = resolver;
        this.mIsDoCoMo = VCardConfig.isDoCoMo(vcardType);
        charset = TextUtils.isEmpty(charset) ? "UTF-8" : charset;
        if (VCardConfig.isVersion30(vcardType) && "UTF-8".equalsIgnoreCase(charset)) {
            shouldAppendCharsetParam = false;
        }
        if (this.mIsDoCoMo || shouldAppendCharsetParam) {
            if (!"SHIFT_JIS".equalsIgnoreCase(charset) && TextUtils.isEmpty(charset)) {
                this.mCharset = "SHIFT_JIS";
            } else {
                this.mCharset = charset;
            }
        } else if (TextUtils.isEmpty(charset)) {
            this.mCharset = "UTF-8";
        } else {
            this.mCharset = charset;
        }
        Log.d("VCardComposer", "Use the charset \"" + this.mCharset + "\"");
    }

    public boolean init() {
        return init(null, null);
    }

    public boolean init(String selection, String[] selectionArgs) {
        return init(ContactsContract.Contacts.CONTENT_URI, sContactsProjection, selection, selectionArgs, null, null);
    }

    public boolean init(Uri contentUri, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        return init(contentUri, sContactsProjection, selection, selectionArgs, sortOrder, contentUriForRawContactsEntity);
    }

    public boolean init(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder, Uri contentUriForRawContactsEntity) {
        if (!"com.android.contacts".equals(contentUri.getAuthority())) {
            this.mErrorReason = "The Uri vCard composer received is not supported by the composer.";
            return false;
        }
        if (initInterFirstPart(contentUriForRawContactsEntity) && initInterCursorCreationPart(contentUri, projection, selection, selectionArgs, sortOrder) && initInterMainPart()) {
            return initInterLastPart();
        }
        return false;
    }

    private boolean initInterFirstPart(Uri contentUriForRawContactsEntity) {
        if (contentUriForRawContactsEntity == null) {
            contentUriForRawContactsEntity = ContactsContract.RawContactsEntity.CONTENT_URI;
        }
        this.mContentUriForRawContactsEntity = contentUriForRawContactsEntity;
        if (this.mInitDone) {
            Log.e("VCardComposer", "init() is already called");
            return false;
        }
        return true;
    }

    private boolean initInterCursorCreationPart(Uri contentUri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        this.mCursorSuppliedFromOutside = false;
        this.mCursor = this.mContentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder);
        if (this.mCursor != null) {
            return true;
        }
        Log.e("VCardComposer", String.format("Cursor became null unexpectedly", new Object[0]));
        this.mErrorReason = "Failed to get database information";
        return false;
    }

    private boolean initInterMainPart() {
        if (this.mCursor.getCount() == 0 || !this.mCursor.moveToFirst()) {
            closeCursorIfAppropriate();
            return false;
        }
        this.mIdColumn = this.mCursor.getColumnIndex("_id");
        return this.mIdColumn >= 0;
    }

    private boolean initInterLastPart() {
        this.mInitDone = true;
        this.mTerminateCalled = false;
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
            Log.e("VCardComposer", "Cursor#moveToNext() returned false");
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
                            Log.e("VCardComposer", "InvocationTargetException has been thrown: ", e);
                            throw new RuntimeException("InvocationTargetException has been thrown");
                        }
                    } catch (IllegalAccessException e2) {
                        Log.e("VCardComposer", "IllegalAccessException has been thrown: " + e2.getMessage());
                    }
                } catch (IllegalArgumentException e3) {
                    Log.e("VCardComposer", "IllegalArgumentException has been thrown: " + e3.getMessage());
                }
            } else {
                entityIterator = ContactsContract.RawContacts.newEntityIterator(this.mContentResolver.query(uri, null, "contact_id=?", selectionArgs, null));
            }
            if (entityIterator == null) {
                Log.e("VCardComposer", "EntityIterator is null");
            }
            if (!entityIterator.hasNext()) {
                Log.w("VCardComposer", "Data does not exist. contactId: " + contactId);
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
                    String key = contentValues.getAsString("mimetype");
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

    public String buildVCard(Map<String, List<ContentValues>> contentValuesListMap) {
        if (contentValuesListMap == null) {
            Log.e("VCardComposer", "The given map is null. Ignore and return empty String");
            return "";
        }
        VCardBuilder builder = new VCardBuilder(this.mVCardType, this.mCharset);
        builder.appendNameProperties(contentValuesListMap.get("vnd.android.cursor.item/name")).appendNickNames(contentValuesListMap.get("vnd.android.cursor.item/nickname")).appendPhones(contentValuesListMap.get("vnd.android.cursor.item/phone_v2"), this.mPhoneTranslationCallback).appendEmails(contentValuesListMap.get("vnd.android.cursor.item/email_v2")).appendPostals(contentValuesListMap.get("vnd.android.cursor.item/postal-address_v2")).appendOrganizations(contentValuesListMap.get("vnd.android.cursor.item/organization")).appendWebsites(contentValuesListMap.get("vnd.android.cursor.item/website"));
        if ((this.mVCardType & 8388608) == 0) {
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
                Log.e("VCardComposer", "SQLiteException on Cursor#close(): " + e.getMessage());
            }
            this.mCursor = null;
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (!this.mTerminateCalled) {
                Log.e("VCardComposer", "finalized() is called before terminate() being called");
            }
        } finally {
            super.finalize();
        }
    }

    public int getCount() {
        if (this.mCursor != null) {
            return this.mCursor.getCount();
        }
        Log.w("VCardComposer", "This object is not ready yet.");
        return 0;
    }

    public boolean isAfterLast() {
        if (this.mCursor != null) {
            return this.mCursor.isAfterLast();
        }
        Log.w("VCardComposer", "This object is not ready yet.");
        return false;
    }
}
