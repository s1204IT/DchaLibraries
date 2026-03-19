package com.android.contacts.model;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GeoUtil;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.compat.CompatUtils;
import com.android.contacts.group.GroupMetaData;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.PhoneDataItem;
import com.android.contacts.model.dataitem.PhotoDataItem;
import com.android.contacts.util.ContactLoaderUtils;
import com.android.contacts.util.DataStatus;
import com.android.contacts.util.UriUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.ContactsPortableUtils;
import com.mediatek.contacts.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactLoader extends AsyncTaskLoader<Contact> {
    private static final boolean DEBUG = true;
    private static final String TAG = ContactLoader.class.getSimpleName();
    private static Contact sCachedResult = null;
    private boolean mComputeFormattedPhoneNumber;
    private Contact mContact;
    private boolean mLoadGroupMetaData;
    private Uri mLookupUri;
    private final Set<Long> mNotifiedRawContactIds;
    private Loader<Contact>.ForceLoadContentObserver mObserver;
    private boolean mPostViewNotification;
    private final Uri mRequestedUri;

    private static class DirectoryQuery {
        static final String[] COLUMNS = {"displayName", "packageName", "typeResourceId", ContactSaveService.EXTRA_ACCOUNT_TYPE, ContactSaveService.EXTRA_ACCOUNT_NAME, "exportSupport"};
    }

    public ContactLoader(Context context, Uri uri, boolean z) {
        this(context, uri, false, z, false);
    }

    public ContactLoader(Context context, Uri uri, boolean z, boolean z2) {
        this(context, uri, z2, z, false);
    }

    public ContactLoader(Context context, Uri uri, boolean z, boolean z2, boolean z3) {
        super(context);
        this.mNotifiedRawContactIds = Sets.newHashSet();
        this.mLookupUri = uri;
        this.mRequestedUri = uri;
        this.mLoadGroupMetaData = z;
        this.mPostViewNotification = z2;
        this.mComputeFormattedPhoneNumber = z3;
    }

    private static class ContactQuery {
        static final String[] COLUMNS;
        static final String[] COLUMNS_INTERNAL = {"name_raw_contact_id", "display_name_source", "lookup", "display_name", "display_name_alt", "phonetic_name", "photo_id", ContactSaveService.EXTRA_STARRED_FLAG, "contact_presence", "contact_status", "contact_status_ts", "contact_status_res_package", "contact_status_label", "contact_id", "raw_contact_id", "account_name", "account_type", "data_set", "dirty", "version", "sourceid", "sync1", "sync2", "sync3", "sync4", "deleted", "data_id", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14", "data15", "data_sync1", "data_sync2", "data_sync3", "data_sync4", "data_version", "is_primary", "is_super_primary", "mimetype", "group_sourceid", "mode", "chat_capability", "status", "status_res_package", "status_icon", "status_label", "status_ts", "photo_uri", "send_to_voicemail", "custom_ringtone", "is_user_profile", "times_used", "last_time_used"};

        static {
            ArrayList arrayListNewArrayList = Lists.newArrayList(COLUMNS_INTERNAL);
            if (CompatUtils.isMarshmallowCompatible()) {
                arrayListNewArrayList.add("carrier_presence");
            }
            if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
                arrayListNewArrayList.add("send_to_voicemail_vt");
                arrayListNewArrayList.add("indicate_phone_or_sim_contact");
                arrayListNewArrayList.add("index_in_sim");
                arrayListNewArrayList.add("is_sdn_contact");
                arrayListNewArrayList.add("is_additional_number");
            }
            COLUMNS = (String[]) arrayListNewArrayList.toArray(new String[arrayListNewArrayList.size()]);
        }
    }

    public void setNewLookup(Uri uri) {
        this.mLookupUri = uri;
        this.mContact = null;
    }

    @Override
    public Contact loadInBackground() {
        Contact contactLoadContactEntity;
        boolean z;
        try {
            ContentResolver contentResolver = getContext().getContentResolver();
            Uri uriEnsureIsContactUri = ContactLoaderUtils.ensureIsContactUri(contentResolver, this.mLookupUri);
            Contact contact = sCachedResult;
            Log.sensitive(TAG, "[loadInBackground] mLookupUri=" + this.mLookupUri + ", uriCurrentFormat=" + uriEnsureIsContactUri);
            sCachedResult = null;
            if (contact != null && UriUtils.areEqual(contact.getLookupUri(), this.mLookupUri)) {
                contactLoadContactEntity = new Contact(this.mRequestedUri, contact);
                z = DEBUG;
            } else {
                if (uriEnsureIsContactUri.getLastPathSegment().equals("encoded")) {
                    contactLoadContactEntity = loadEncodedContactEntity(uriEnsureIsContactUri, this.mLookupUri);
                } else {
                    contactLoadContactEntity = loadContactEntity(contentResolver, uriEnsureIsContactUri);
                }
                z = false;
            }
            if (contactLoadContactEntity.isLoaded()) {
                if (contactLoadContactEntity.isDirectoryEntry()) {
                    if (!z) {
                        loadDirectoryMetaData(contactLoadContactEntity);
                    }
                } else if (this.mLoadGroupMetaData && contactLoadContactEntity.getGroupMetaData() == null) {
                    loadGroupMetaData(contactLoadContactEntity);
                }
                if (this.mComputeFormattedPhoneNumber) {
                    computeFormattedPhoneNumbers(contactLoadContactEntity);
                }
                if (!z) {
                    loadPhotoBinaryData(contactLoadContactEntity);
                }
            }
            return contactLoadContactEntity;
        } catch (Exception e) {
            Log.e(TAG, "Error loading the contact: " + this.mLookupUri, e);
            return Contact.forError(this.mRequestedUri, e);
        }
    }

    public static Contact parseEncodedContactEntity(Uri uri) {
        try {
            return loadEncodedContactEntity(uri, uri);
        } catch (JSONException e) {
            return null;
        }
    }

    private static Contact loadEncodedContactEntity(Uri uri, Uri uri2) throws JSONException {
        JSONObject jSONObject = new JSONObject(uri.getEncodedFragment());
        long jLongValue = Long.valueOf(uri.getQueryParameter("directory")).longValue();
        String strOptString = jSONObject.optString("display_name");
        Contact contact = new Contact(uri, uri, uri2, jLongValue, null, -1L, -1L, jSONObject.getInt("display_name_source"), 0L, jSONObject.optString("photo_uri", null), strOptString, jSONObject.optString("display_name_alt", strOptString), null, false, null, false, null, false);
        contact.setStatuses(new ImmutableMap.Builder().build());
        String strOptString2 = jSONObject.optString("account_name", null);
        String queryParameter = uri.getQueryParameter("displayName");
        if (strOptString2 != null) {
            contact.setDirectoryMetaData(queryParameter, null, strOptString2, jSONObject.getString("account_type"), jSONObject.optInt("exportSupport", 1));
        } else {
            contact.setDirectoryMetaData(queryParameter, null, null, null, jSONObject.optInt("exportSupport", 2));
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", (Integer) (-1));
        contentValues.put("contact_id", (Integer) (-1));
        RawContact rawContact = new RawContact(contentValues);
        JSONObject jSONObject2 = jSONObject.getJSONObject("vnd.android.cursor.item/contact");
        Iterator<String> itKeys = jSONObject2.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            JSONObject jSONObjectOptJSONObject = jSONObject2.optJSONObject(next);
            if (jSONObjectOptJSONObject == null) {
                JSONArray jSONArray = jSONObject2.getJSONArray(next);
                for (int i = 0; i < jSONArray.length(); i++) {
                    processOneRecord(rawContact, jSONArray.getJSONObject(i), next);
                }
            } else {
                processOneRecord(rawContact, jSONObjectOptJSONObject, next);
            }
        }
        contact.setRawContacts(new ImmutableList.Builder().add(rawContact).build());
        return contact;
    }

    private static void processOneRecord(RawContact rawContact, JSONObject jSONObject, String str) throws JSONException {
        ContentValues contentValues = new ContentValues();
        contentValues.put("mimetype", str);
        contentValues.put("_id", (Integer) (-1));
        Iterator<String> itKeys = jSONObject.keys();
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            Object obj = jSONObject.get(next);
            if (obj instanceof String) {
                contentValues.put(next, (String) obj);
            } else if (obj instanceof Integer) {
                contentValues.put(next, (Integer) obj);
            }
        }
        rawContact.addDataItemValues(contentValues);
    }

    private Contact loadContactEntity(ContentResolver contentResolver, Uri uri) {
        Cursor cursorQuery = contentResolver.query(Uri.withAppendedPath(uri, "entities"), ContactQuery.COLUMNS, null, null, "raw_contact_id");
        if (cursorQuery == null) {
            Log.e(TAG, "No cursor returned in loadContactEntity");
            return Contact.forNotFound(this.mRequestedUri);
        }
        try {
            if (!cursorQuery.moveToFirst()) {
                cursorQuery.close();
                return Contact.forNotFound(this.mRequestedUri);
            }
            Contact contactLoadContactHeaderDataEX = loadContactHeaderDataEX(cursorQuery, uri);
            long j = -1;
            RawContact rawContact = null;
            ImmutableList.Builder builder = new ImmutableList.Builder();
            ImmutableMap.Builder builder2 = new ImmutableMap.Builder();
            do {
                long j2 = cursorQuery.getLong(14);
                if (j2 != j) {
                    RawContact rawContact2 = new RawContact(loadRawContactValues(cursorQuery));
                    builder.add(rawContact2);
                    rawContact = rawContact2;
                    j = j2;
                }
                if (!cursorQuery.isNull(26)) {
                    rawContact.addDataItemValues(loadDataValues(cursorQuery));
                    if (!cursorQuery.isNull(51) || !cursorQuery.isNull(53)) {
                        builder2.put(Long.valueOf(cursorQuery.getLong(26)), new DataStatus(cursorQuery));
                    }
                }
            } while (cursorQuery.moveToNext());
            contactLoadContactHeaderDataEX.setRawContacts(builder.build());
            contactLoadContactHeaderDataEX.setStatuses(builder2.build());
            return contactLoadContactHeaderDataEX;
        } finally {
            cursorQuery.close();
        }
    }

    private void loadPhotoBinaryData(Contact contact) {
        InputStream inputStreamOpenStream;
        AssetFileDescriptor assetFileDescriptorOpenAssetFileDescriptor;
        loadThumbnailBinaryData(contact);
        String photoUri = contact.getPhotoUri();
        if (photoUri != null) {
            try {
                Uri uri = Uri.parse(photoUri);
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    inputStreamOpenStream = new URL(photoUri).openStream();
                    assetFileDescriptorOpenAssetFileDescriptor = null;
                } else {
                    assetFileDescriptorOpenAssetFileDescriptor = getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
                    inputStreamOpenStream = assetFileDescriptorOpenAssetFileDescriptor.createInputStream();
                }
                byte[] bArr = new byte[16384];
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                while (true) {
                    try {
                        int i = inputStreamOpenStream.read(bArr);
                        if (i == -1) {
                            break;
                        } else {
                            byteArrayOutputStream.write(bArr, 0, i);
                        }
                    } finally {
                        inputStreamOpenStream.close();
                        if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                            assetFileDescriptorOpenAssetFileDescriptor.close();
                        }
                    }
                }
                contact.setPhotoBinaryData(byteArrayOutputStream.toByteArray());
                if (assetFileDescriptorOpenAssetFileDescriptor != null) {
                    return;
                } else {
                    return;
                }
            } catch (IOException e) {
            }
        }
        contact.setPhotoBinaryData(contact.getThumbnailPhotoBinaryData());
    }

    private void loadThumbnailBinaryData(Contact contact) {
        long photoId = contact.getPhotoId();
        if (photoId <= 0) {
            return;
        }
        UnmodifiableIterator<RawContact> it = contact.getRawContacts().iterator();
        while (it.hasNext()) {
            Iterator<DataItem> it2 = it.next().getDataItems().iterator();
            while (true) {
                if (it2.hasNext()) {
                    DataItem next = it2.next();
                    if (next.getId() == photoId) {
                        if (next instanceof PhotoDataItem) {
                            contact.setThumbnailPhotoBinaryData(((PhotoDataItem) next).getPhoto());
                        }
                    }
                }
            }
        }
    }

    private Contact loadContactHeaderData(Cursor cursor, Uri uri) {
        long j;
        Integer numValueOf;
        int i;
        boolean z;
        Uri uriWithAppendedId;
        String queryParameter = uri.getQueryParameter("directory");
        if (queryParameter != null) {
            j = Long.parseLong(queryParameter);
        } else {
            j = 0;
        }
        long j2 = cursor.getLong(13);
        String string = cursor.getString(2);
        long j3 = cursor.getLong(0);
        int i2 = cursor.getInt(1);
        String string2 = cursor.getString(3);
        String string3 = cursor.getString(4);
        String string4 = cursor.getString(5);
        long j4 = cursor.getLong(6);
        String string5 = cursor.getString(58);
        boolean z2 = cursor.getInt(7) != 0;
        if (cursor.isNull(8)) {
            numValueOf = null;
        } else {
            numValueOf = Integer.valueOf(cursor.getInt(8));
        }
        Integer num = numValueOf;
        boolean z3 = cursor.getInt(59) == 1;
        String string6 = cursor.getString(60);
        boolean z4 = cursor.getInt(61) == 1;
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            boolean z5 = cursor.getInt(65) == 1;
            i = cursor.getInt(68);
            z = z5;
        } else {
            i = 0;
            z = false;
        }
        if (j == 0 || j == 1) {
            uriWithAppendedId = ContentUris.withAppendedId(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, string), j2);
        } else {
            uriWithAppendedId = uri;
        }
        return new Contact(this.mRequestedUri, uri, uriWithAppendedId, j, string, j2, j3, i2, j4, string5, string2, string3, string4, z2, num, z3, string6, z4, i, z);
    }

    private ContentValues loadRawContactValues(Cursor cursor) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", Long.valueOf(cursor.getLong(14)));
        cursorColumnToContentValues(cursor, contentValues, 15);
        cursorColumnToContentValues(cursor, contentValues, 16);
        cursorColumnToContentValues(cursor, contentValues, 17);
        cursorColumnToContentValues(cursor, contentValues, 18);
        cursorColumnToContentValues(cursor, contentValues, 19);
        cursorColumnToContentValues(cursor, contentValues, 20);
        cursorColumnToContentValues(cursor, contentValues, 21);
        cursorColumnToContentValues(cursor, contentValues, 22);
        cursorColumnToContentValues(cursor, contentValues, 23);
        cursorColumnToContentValues(cursor, contentValues, 24);
        cursorColumnToContentValues(cursor, contentValues, 25);
        cursorColumnToContentValues(cursor, contentValues, 13);
        cursorColumnToContentValues(cursor, contentValues, 7);
        return contentValues;
    }

    private ContentValues loadDataValues(Cursor cursor) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("_id", Long.valueOf(cursor.getLong(26)));
        cursorColumnToContentValues(cursor, contentValues, 27);
        cursorColumnToContentValues(cursor, contentValues, 28);
        cursorColumnToContentValues(cursor, contentValues, 29);
        cursorColumnToContentValues(cursor, contentValues, 30);
        cursorColumnToContentValues(cursor, contentValues, 31);
        cursorColumnToContentValues(cursor, contentValues, 32);
        cursorColumnToContentValues(cursor, contentValues, 33);
        cursorColumnToContentValues(cursor, contentValues, 34);
        cursorColumnToContentValues(cursor, contentValues, 35);
        cursorColumnToContentValues(cursor, contentValues, 36);
        cursorColumnToContentValues(cursor, contentValues, 37);
        cursorColumnToContentValues(cursor, contentValues, 38);
        cursorColumnToContentValues(cursor, contentValues, 39);
        cursorColumnToContentValues(cursor, contentValues, 40);
        cursorColumnToContentValues(cursor, contentValues, 41);
        cursorColumnToContentValues(cursor, contentValues, 42);
        cursorColumnToContentValues(cursor, contentValues, 43);
        cursorColumnToContentValues(cursor, contentValues, 44);
        cursorColumnToContentValues(cursor, contentValues, 45);
        cursorColumnToContentValues(cursor, contentValues, 46);
        cursorColumnToContentValues(cursor, contentValues, 47);
        cursorColumnToContentValues(cursor, contentValues, 48);
        cursorColumnToContentValues(cursor, contentValues, 49);
        cursorColumnToContentValues(cursor, contentValues, 50);
        cursorColumnToContentValues(cursor, contentValues, 52);
        cursorColumnToContentValues(cursor, contentValues, 62);
        cursorColumnToContentValues(cursor, contentValues, 63);
        if (CompatUtils.isMarshmallowCompatible()) {
            cursorColumnToContentValues(cursor, contentValues, 64);
        }
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            cursorColumnToContentValues(cursor, contentValues, 69);
        }
        return contentValues;
    }

    private void cursorColumnToContentValues(Cursor cursor, ContentValues contentValues, int i) {
        switch (cursor.getType(i)) {
            case 0:
                return;
            case 1:
                contentValues.put(ContactQuery.COLUMNS[i], Long.valueOf(cursor.getLong(i)));
                return;
            case 2:
            default:
                throw new IllegalStateException("Invalid or unhandled data type");
            case 3:
                contentValues.put(ContactQuery.COLUMNS[i], cursor.getString(i));
                return;
            case CompatUtils.TYPE_ASSERT:
                contentValues.put(ContactQuery.COLUMNS[i], cursor.getBlob(i));
                return;
        }
    }

    private void loadDirectoryMetaData(Contact contact) {
        Cursor cursorQuery = getContext().getContentResolver().query(ContentUris.withAppendedId(ContactsContract.Directory.CONTENT_URI, contact.getDirectoryId()), DirectoryQuery.COLUMNS, null, null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            if (cursorQuery.moveToFirst()) {
                String string = cursorQuery.getString(0);
                String string2 = cursorQuery.getString(1);
                int i = cursorQuery.getInt(2);
                String string3 = cursorQuery.getString(3);
                String string4 = cursorQuery.getString(4);
                int i2 = cursorQuery.getInt(5);
                String string5 = null;
                if (!TextUtils.isEmpty(string2)) {
                    try {
                        string5 = getContext().getPackageManager().getResourcesForApplication(string2).getString(i);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.w(TAG, "Contact directory resource not found: " + string2 + "." + i);
                    }
                }
                contact.setDirectoryMetaData(string, string5, string3, string4, i2);
            }
        } finally {
            cursorQuery.close();
        }
    }

    private static class AccountKey {
        private final String mAccountName;
        private final String mAccountType;
        private final String mDataSet;

        public AccountKey(String str, String str2, String str3) {
            this.mAccountName = str;
            this.mAccountType = str2;
            this.mDataSet = str3;
        }

        public int hashCode() {
            return Objects.hash(this.mAccountName, this.mAccountType, this.mDataSet);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof AccountKey)) {
                return false;
            }
            AccountKey accountKey = (AccountKey) obj;
            if (Objects.equals(this.mAccountName, accountKey.mAccountName) && Objects.equals(this.mAccountType, accountKey.mAccountType) && Objects.equals(this.mDataSet, accountKey.mDataSet)) {
                return ContactLoader.DEBUG;
            }
            return false;
        }
    }

    private void loadGroupMetaData(Contact contact) {
        StringBuilder sb = new StringBuilder();
        ArrayList arrayList = new ArrayList();
        HashSet hashSet = new HashSet();
        UnmodifiableIterator<RawContact> it = contact.getRawContacts().iterator();
        while (it.hasNext()) {
            RawContact next = it.next();
            String accountName = next.getAccountName();
            String accountTypeString = next.getAccountTypeString();
            String dataSet = next.getDataSet();
            AccountKey accountKey = new AccountKey(accountName, accountTypeString, dataSet);
            if (accountName != null && accountTypeString != null && !hashSet.contains(accountKey)) {
                hashSet.add(accountKey);
                if (sb.length() != 0) {
                    sb.append(" OR ");
                }
                sb.append("(account_name=? AND account_type=?");
                arrayList.add(accountName);
                arrayList.add(accountTypeString);
                sb.append(" AND deleted=0");
                if (dataSet != null) {
                    sb.append(" AND data_set=?");
                    arrayList.add(dataSet);
                } else {
                    sb.append(" AND data_set IS NULL");
                }
                sb.append(")");
            }
        }
        ImmutableList.Builder builder = new ImmutableList.Builder();
        Cursor cursorQuery = getContext().getContentResolver().query(ContactsContract.Groups.CONTENT_URI, GroupMetaDataLoader.COLUMNS, sb.toString(), (String[]) arrayList.toArray(new String[0]), null);
        if (cursorQuery != null) {
            while (cursorQuery.moveToNext()) {
                try {
                    builder.add(new GroupMetaData(getContext(), cursorQuery));
                } finally {
                    cursorQuery.close();
                }
            }
        }
        contact.setGroupMetaData(builder.build());
    }

    private void computeFormattedPhoneNumbers(Contact contact) {
        String currentCountryIso = GeoUtil.getCurrentCountryIso(getContext());
        ImmutableList<RawContact> rawContacts = contact.getRawContacts();
        int size = rawContacts.size();
        for (int i = 0; i < size; i++) {
            List<DataItem> dataItems = rawContacts.get(i).getDataItems();
            int size2 = dataItems.size();
            for (int i2 = 0; i2 < size2; i2++) {
                DataItem dataItem = dataItems.get(i2);
                if (dataItem instanceof PhoneDataItem) {
                    ((PhoneDataItem) dataItem).computeFormattedPhoneNumber(currentCountryIso);
                }
            }
        }
    }

    @Override
    public void deliverResult(Contact contact) {
        unregisterObserver();
        if (isReset() || contact == null) {
            return;
        }
        this.mContact = contact;
        if (contact.isLoaded()) {
            this.mLookupUri = contact.getLookupUri();
            if (!contact.isDirectoryEntry()) {
                Log.i(TAG, "Registering content observer for " + this.mLookupUri);
                if (this.mObserver == null) {
                    this.mObserver = new Loader.ForceLoadContentObserver(this);
                }
                getContext().getContentResolver().registerContentObserver(this.mLookupUri, DEBUG, this.mObserver);
            }
            if (this.mPostViewNotification) {
                postViewNotificationToSyncAdapter();
            }
        }
        super.deliverResult(this.mContact);
    }

    private void postViewNotificationToSyncAdapter() {
        Context context = getContext();
        UnmodifiableIterator<RawContact> it = this.mContact.getRawContacts().iterator();
        while (it.hasNext()) {
            RawContact next = it.next();
            long jLongValue = next.getId().longValue();
            if (!this.mNotifiedRawContactIds.contains(Long.valueOf(jLongValue))) {
                this.mNotifiedRawContactIds.add(Long.valueOf(jLongValue));
                AccountType accountType = next.getAccountType(context);
                String viewContactNotifyServiceClassName = accountType.getViewContactNotifyServiceClassName();
                String viewContactNotifyServicePackageName = accountType.getViewContactNotifyServicePackageName();
                if (!TextUtils.isEmpty(viewContactNotifyServiceClassName) && !TextUtils.isEmpty(viewContactNotifyServicePackageName)) {
                    Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, jLongValue);
                    Intent intent = new Intent();
                    intent.setClassName(viewContactNotifyServicePackageName, viewContactNotifyServiceClassName);
                    intent.setAction("android.intent.action.VIEW");
                    intent.setDataAndType(uriWithAppendedId, "vnd.android.cursor.item/raw_contact");
                    try {
                        context.startService(intent);
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending message to source-app", e);
                    }
                }
            }
        }
    }

    private void unregisterObserver() {
        if (this.mObserver != null) {
            getContext().getContentResolver().unregisterContentObserver(this.mObserver);
            this.mObserver = null;
        }
    }

    public Uri getLookupUri() {
        return this.mLookupUri;
    }

    @Override
    protected void onStartLoading() {
        if (this.mContact != null && this.mContact.getLookupUri() == this.mLookupUri) {
            Log.sensitive(TAG, "[onStartLoading] deliver current result, lookup uri = " + this.mContact.getLookupUri());
            deliverResult(this.mContact);
        }
        if (takeContentChanged() || this.mContact == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        super.onReset();
        cancelLoad();
        unregisterObserver();
        this.mContact = null;
    }

    public void cacheResult() {
        if (this.mContact == null || !this.mContact.isLoaded()) {
            sCachedResult = null;
        } else {
            sCachedResult = this.mContact;
        }
    }

    private Contact loadContactHeaderDataEX(Cursor cursor, Uri uri) {
        Contact contactLoadContactHeaderData = loadContactHeaderData(cursor, uri);
        if (ContactsPortableUtils.MTK_PHONE_BOOK_SUPPORT) {
            contactLoadContactHeaderData.setIndicate(cursor.getInt(66));
            contactLoadContactHeaderData.setSimIndex(cursor.getInt(67));
        }
        contactLoadContactHeaderData.setSlot(SubInfoUtils.getSlotIdUsingSubId(contactLoadContactHeaderData.getIndicate()));
        return contactLoadContactHeaderData;
    }
}
