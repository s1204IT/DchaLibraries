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
import java.util.Iterator;
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
    private RawContactEntitlesInfoCallback mRawContactEntitlesInfoCallback;
    private boolean mTerminateCalled;
    private final int mVCardType;

    public static class RawContactEntitlesInfo {
        public final long contactId;
        public final Uri rawContactEntitlesUri;
    }

    public interface RawContactEntitlesInfoCallback {
        RawContactEntitlesInfo getRawContactEntitlesInfo(long j);
    }

    static {
        sImMap.put(0, "X-AIM");
        sImMap.put(1, "X-MSN");
        sImMap.put(2, "X-YAHOO");
        sImMap.put(6, "X-ICQ");
        sImMap.put(7, "X-JABBER");
        sImMap.put(3, "X-SKYPE-USERNAME");
        sContactsProjection = new String[]{"_id"};
    }

    public VCardComposer(Context context, int i, boolean z) {
        this(context, i, null, z);
    }

    public VCardComposer(Context context, int i, String str, boolean z) {
        this(context, context.getContentResolver(), i, str, z);
    }

    public VCardComposer(Context context, ContentResolver contentResolver, int i, String str, boolean z) {
        this.mErrorReason = "No error";
        boolean z2 = true;
        this.mTerminateCalled = true;
        this.mVCardType = i;
        this.mContentResolver = contentResolver;
        this.mIsDoCoMo = VCardConfig.isDoCoMo(i);
        str = TextUtils.isEmpty(str) ? "UTF-8" : str;
        if (VCardConfig.isVersion30(i) && "UTF-8".equalsIgnoreCase(str)) {
            z2 = false;
        }
        if (this.mIsDoCoMo || z2) {
            if (!"SHIFT_JIS".equalsIgnoreCase(str) && TextUtils.isEmpty(str)) {
                this.mCharset = "SHIFT_JIS";
            } else {
                this.mCharset = str;
            }
        } else if (TextUtils.isEmpty(str)) {
            this.mCharset = "UTF-8";
        } else {
            this.mCharset = str;
        }
        Log.d("VCardComposer", "Use the charset \"" + this.mCharset + "\"");
    }

    public boolean init() {
        return init(null, null);
    }

    public boolean init(String str, String[] strArr) {
        return init(ContactsContract.Contacts.CONTENT_URI, sContactsProjection, str, strArr, null, null);
    }

    public boolean init(Uri uri, String str, String[] strArr, String str2, Uri uri2) {
        return init(uri, sContactsProjection, str, strArr, str2, uri2);
    }

    public boolean init(Uri uri, String[] strArr, String str, String[] strArr2, String str2, Uri uri2) {
        if (!"com.android.contacts".equals(uri.getAuthority())) {
            this.mErrorReason = "The Uri vCard composer received is not supported by the composer.";
            return false;
        }
        if (initInterFirstPart(uri2) && initInterCursorCreationPart(uri, strArr, str, strArr2, str2) && initInterMainPart()) {
            return initInterLastPart();
        }
        return false;
    }

    private boolean initInterFirstPart(Uri uri) {
        if (uri == null) {
            uri = ContactsContract.RawContactsEntity.CONTENT_URI;
        }
        this.mContentUriForRawContactsEntity = uri;
        if (this.mInitDone) {
            Log.e("VCardComposer", "init() is already called");
            return false;
        }
        return true;
    }

    private boolean initInterCursorCreationPart(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        this.mCursorSuppliedFromOutside = false;
        this.mCursor = this.mContentResolver.query(uri, strArr, str, strArr2, str2);
        if (this.mCursor == null) {
            Log.e("VCardComposer", String.format("Cursor became null unexpectedly", new Object[0]));
            this.mErrorReason = "Failed to get database information";
            return false;
        }
        return true;
    }

    private boolean initInterMainPart() {
        if (this.mCursor.getCount() == 0 || !this.mCursor.moveToFirst()) {
            closeCursorIfAppropriate();
            return false;
        }
        this.mIdColumn = this.mCursor.getColumnIndex("contact_id");
        if (this.mIdColumn < 0) {
            this.mIdColumn = this.mCursor.getColumnIndex("_id");
        }
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

    public String createOneEntry(Method method) {
        if (this.mIsDoCoMo && !this.mFirstVCardEmittedInDoCoMoCase) {
            this.mFirstVCardEmittedInDoCoMoCase = true;
        }
        String strCreateOneEntryInternal = createOneEntryInternal(this.mCursor.getLong(this.mIdColumn), method);
        if (!this.mCursor.moveToNext()) {
            Log.e("VCardComposer", "Cursor#moveToNext() returned false");
        }
        return strCreateOneEntryInternal;
    }

    private String createOneEntryInternal(long j, Method method) {
        EntityIterator entityIteratorNewEntityIterator;
        HashMap map = new HashMap();
        EntityIterator entityIterator = null;
        try {
            Uri uri = this.mContentUriForRawContactsEntity;
            if (this.mRawContactEntitlesInfoCallback != null) {
                RawContactEntitlesInfo rawContactEntitlesInfo = this.mRawContactEntitlesInfoCallback.getRawContactEntitlesInfo(j);
                uri = rawContactEntitlesInfo.rawContactEntitlesUri;
                j = rawContactEntitlesInfo.contactId;
            }
            Uri uri2 = uri;
            String[] strArr = {String.valueOf(j)};
            if (method != null) {
                try {
                    try {
                        entityIteratorNewEntityIterator = (EntityIterator) method.invoke(null, this.mContentResolver, uri2, "contact_id=?", strArr, null);
                    } catch (IllegalArgumentException e) {
                        Log.e("VCardComposer", "IllegalArgumentException has been thrown: " + e.getMessage());
                        if (entityIterator == null) {
                        }
                    }
                } catch (IllegalAccessException e2) {
                    Log.e("VCardComposer", "IllegalAccessException has been thrown: " + e2.getMessage());
                    if (entityIterator == null) {
                    }
                } catch (InvocationTargetException e3) {
                    Log.e("VCardComposer", "InvocationTargetException has been thrown: ", e3);
                    throw new RuntimeException("InvocationTargetException has been thrown");
                }
            } else {
                entityIteratorNewEntityIterator = ContactsContract.RawContacts.newEntityIterator(this.mContentResolver.query(uri2, null, "contact_id=?", strArr, null));
            }
            entityIterator = entityIteratorNewEntityIterator;
            if (entityIterator == null) {
                Log.e("VCardComposer", "EntityIterator is null");
                return "";
            }
            if (!entityIterator.hasNext()) {
                Log.w("VCardComposer", "Data does not exist. contactId: " + j);
                if (entityIterator != null) {
                    entityIterator.close();
                }
                return "";
            }
            while (entityIterator.hasNext()) {
                Entity entity = (Entity) entityIterator.next();
                Iterator<Entity.NamedContentValues> it = entity.getSubValues().iterator();
                while (it.hasNext()) {
                    ContentValues contentValues = it.next().values;
                    String asString = contentValues.getAsString("mimetype");
                    if (asString != null) {
                        List<ContentValues> arrayList = map.get(asString);
                        if (arrayList == null) {
                            arrayList = new ArrayList<>();
                            map.put(asString, arrayList);
                        }
                        setPhoneTypeToMobileByAas(entity, contentValues, asString);
                        arrayList.add(contentValues);
                    }
                }
            }
            if (entityIterator != null) {
                entityIterator.close();
            }
            return buildVCard(map);
        } finally {
            if (0 != 0) {
                entityIterator.close();
            }
        }
    }

    public String buildVCard(Map<String, List<ContentValues>> map) {
        if (map == null) {
            Log.e("VCardComposer", "The given map is null. Ignore and return empty String");
            return "";
        }
        VCardBuilder vCardBuilder = new VCardBuilder(this.mVCardType, this.mCharset);
        vCardBuilder.appendNameProperties(map.get("vnd.android.cursor.item/name")).appendNickNames(map.get("vnd.android.cursor.item/nickname")).appendPhones(map.get("vnd.android.cursor.item/phone_v2"), this.mPhoneTranslationCallback).appendEmails(map.get("vnd.android.cursor.item/email_v2")).appendPostals(map.get("vnd.android.cursor.item/postal-address_v2")).appendOrganizations(map.get("vnd.android.cursor.item/organization")).appendWebsites(map.get("vnd.android.cursor.item/website"));
        if ((this.mVCardType & 8388608) == 0) {
            vCardBuilder.appendPhotos(map.get("vnd.android.cursor.item/photo"));
        }
        vCardBuilder.appendNotes(map.get("vnd.android.cursor.item/note")).appendEvents(map.get("vnd.android.cursor.item/contact_event")).appendIms(map.get("vnd.android.cursor.item/im")).appendSipAddresses(map.get("vnd.android.cursor.item/sip_address")).appendRelation(map.get("vnd.android.cursor.item/relation"));
        return vCardBuilder.toString();
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
        if (this.mCursor == null) {
            Log.w("VCardComposer", "This object is not ready yet.");
            return 0;
        }
        return this.mCursor.getCount();
    }

    public boolean isAfterLast() {
        if (this.mCursor == null) {
            Log.w("VCardComposer", "This object is not ready yet.");
            return false;
        }
        return this.mCursor.isAfterLast();
    }

    private void setPhoneTypeToMobileByAas(Entity entity, ContentValues contentValues, String str) {
        String asString = entity.getEntityValues().getAsString("account_type");
        if ("vnd.android.cursor.item/phone_v2".equals(str) && isAasEnabled(asString)) {
            contentValues.put("data2", (Integer) 2);
            Log.i("VCardComposer", "AAS: set phone type to be Mobile.");
        }
    }

    private boolean isAasEnabled(String str) {
        if ("USIM Account".equals(str)) {
            return true;
        }
        return false;
    }
}
