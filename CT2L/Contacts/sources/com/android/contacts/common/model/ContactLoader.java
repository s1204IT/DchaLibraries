package com.android.contacts.common.model;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.GroupMetaData;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountTypeWithDataSet;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.PhoneDataItem;
import com.android.contacts.common.model.dataitem.PhotoDataItem;
import com.android.contacts.common.util.ContactLoaderUtils;
import com.android.contacts.common.util.DataStatus;
import com.android.contacts.common.util.UriUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactLoader extends AsyncTaskLoader<Contact> {
    private boolean mComputeFormattedPhoneNumber;
    private Contact mContact;
    private boolean mLoadGroupMetaData;
    private boolean mLoadInvitableAccountTypes;
    private Uri mLookupUri;
    private final Set<Long> mNotifiedRawContactIds;
    private Loader<Contact>.ForceLoadContentObserver mObserver;
    private boolean mPostViewNotification;
    private final Uri mRequestedUri;
    private static final String TAG = ContactLoader.class.getSimpleName();
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static Contact sCachedResult = null;

    private static class ContactQuery {
        static final String[] COLUMNS = {"name_raw_contact_id", "display_name_source", "lookup", "display_name", "display_name_alt", "phonetic_name", "photo_id", "starred", "contact_presence", "contact_status", "contact_status_ts", "contact_status_res_package", "contact_status_label", "contact_id", "raw_contact_id", "account_name", "account_type", "data_set", "dirty", "version", "sourceid", "sync1", "sync2", "sync3", "sync4", "deleted", "data_id", "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10", "data11", "data12", "data13", "data14", "data15", "data_sync1", "data_sync2", "data_sync3", "data_sync4", "data_version", "is_primary", "is_super_primary", "mimetype", "group_sourceid", "mode", "chat_capability", "status", "status_res_package", "status_icon", "status_label", "status_ts", "photo_uri", "send_to_voicemail", "custom_ringtone", "is_user_profile", "times_used", "last_time_used"};
    }

    private static class DirectoryQuery {
        static final String[] COLUMNS = {"displayName", "packageName", "typeResourceId", "accountType", "accountName", "exportSupport"};
    }

    private static class GroupQuery {
        static final String[] COLUMNS = {"account_name", "account_type", "data_set", "_id", "title", "auto_add", "favorites"};
    }

    public ContactLoader(Context context, Uri lookupUri, boolean postViewNotification) {
        this(context, lookupUri, false, false, postViewNotification, false);
    }

    public ContactLoader(Context context, Uri lookupUri, boolean loadGroupMetaData, boolean loadInvitableAccountTypes, boolean postViewNotification, boolean computeFormattedPhoneNumber) {
        super(context);
        this.mNotifiedRawContactIds = Sets.newHashSet();
        this.mLookupUri = lookupUri;
        this.mRequestedUri = lookupUri;
        this.mLoadGroupMetaData = loadGroupMetaData;
        this.mLoadInvitableAccountTypes = loadInvitableAccountTypes;
        this.mPostViewNotification = postViewNotification;
        this.mComputeFormattedPhoneNumber = computeFormattedPhoneNumber;
    }

    @Override
    public Contact loadInBackground() {
        Contact result;
        boolean resultIsCached;
        try {
            ContentResolver resolver = getContext().getContentResolver();
            Uri uriCurrentFormat = ContactLoaderUtils.ensureIsContactUri(resolver, this.mLookupUri);
            Contact cachedResult = sCachedResult;
            sCachedResult = null;
            if (cachedResult != null && UriUtils.areEqual(cachedResult.getLookupUri(), this.mLookupUri)) {
                result = new Contact(this.mRequestedUri, cachedResult);
                resultIsCached = true;
            } else {
                if (uriCurrentFormat.getLastPathSegment().equals("encoded")) {
                    result = loadEncodedContactEntity(uriCurrentFormat, this.mLookupUri);
                } else {
                    result = loadContactEntity(resolver, uriCurrentFormat);
                }
                resultIsCached = false;
            }
            if (result.isLoaded()) {
                if (result.isDirectoryEntry()) {
                    if (!resultIsCached) {
                        loadDirectoryMetaData(result);
                    }
                } else if (this.mLoadGroupMetaData && result.getGroupMetaData() == null) {
                    loadGroupMetaData(result);
                }
                if (this.mComputeFormattedPhoneNumber) {
                    computeFormattedPhoneNumbers(result);
                }
                if (!resultIsCached) {
                    loadPhotoBinaryData(result);
                }
                if (this.mLoadInvitableAccountTypes && result.getInvitableAccountTypes() == null) {
                    loadInvitableAccountTypes(result);
                    return result;
                }
                return result;
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error loading the contact: " + this.mLookupUri, e);
            Contact result2 = Contact.forNotFound(this.mRequestedUri);
            return result2;
        }
    }

    private static Contact loadEncodedContactEntity(Uri uri, Uri lookupUri) throws JSONException {
        String jsonString = uri.getEncodedFragment();
        JSONObject json = new JSONObject(jsonString);
        long directoryId = Long.valueOf(uri.getQueryParameter("directory")).longValue();
        String displayName = json.optString("display_name");
        String altDisplayName = json.optString("display_name_alt", displayName);
        int displayNameSource = json.getInt("display_name_source");
        String photoUri = json.optString("photo_uri", null);
        Contact contact = new Contact(uri, uri, lookupUri, directoryId, null, -1L, -1L, displayNameSource, 0L, photoUri, displayName, altDisplayName, null, false, null, false, null, false);
        contact.setStatuses(new ImmutableMap.Builder().build());
        String accountName = json.optString("account_name", null);
        String directoryName = uri.getQueryParameter("displayName");
        if (accountName != null) {
            String accountType = json.getString("account_type");
            contact.setDirectoryMetaData(directoryName, null, accountName, accountType, json.optInt("exportSupport", 1));
        } else {
            contact.setDirectoryMetaData(directoryName, null, null, null, json.optInt("exportSupport", 2));
        }
        ContentValues values = new ContentValues();
        values.put("_id", (Integer) (-1));
        values.put("contact_id", (Integer) (-1));
        RawContact rawContact = new RawContact(values);
        JSONObject items = json.getJSONObject("vnd.android.cursor.item/contact");
        Iterator<String> itKeys = items.keys();
        while (itKeys.hasNext()) {
            String mimetype = itKeys.next();
            JSONObject obj = items.optJSONObject(mimetype);
            if (obj == null) {
                JSONArray array = items.getJSONArray(mimetype);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.getJSONObject(i);
                    processOneRecord(rawContact, item, mimetype);
                }
            } else {
                processOneRecord(rawContact, obj, mimetype);
            }
        }
        contact.setRawContacts(new ImmutableList.Builder().add(rawContact).build());
        return contact;
    }

    private static void processOneRecord(RawContact rawContact, JSONObject item, String mimetype) throws JSONException {
        ContentValues itemValues = new ContentValues();
        itemValues.put("mimetype", mimetype);
        itemValues.put("_id", (Integer) (-1));
        Iterator<String> itKeys = item.keys();
        while (itKeys.hasNext()) {
            String name = itKeys.next();
            Object o = item.get(name);
            if (o instanceof String) {
                itemValues.put(name, (String) o);
            } else if (o instanceof Integer) {
                itemValues.put(name, (Integer) o);
            }
        }
        rawContact.addDataItemValues(itemValues);
    }

    private Contact loadContactEntity(ContentResolver resolver, Uri contactUri) {
        Uri entityUri = Uri.withAppendedPath(contactUri, "entities");
        Cursor cursor = resolver.query(entityUri, ContactQuery.COLUMNS, null, null, "raw_contact_id");
        if (cursor == null) {
            Log.e(TAG, "No cursor returned in loadContactEntity");
            return Contact.forNotFound(this.mRequestedUri);
        }
        try {
            if (!cursor.moveToFirst()) {
                cursor.close();
                return Contact.forNotFound(this.mRequestedUri);
            }
            Contact contact = loadContactHeaderData(cursor, contactUri);
            long currentRawContactId = -1;
            RawContact rawContact = null;
            ImmutableList.Builder<RawContact> rawContactsBuilder = new ImmutableList.Builder<>();
            ImmutableMap.Builder<Long, DataStatus> statusesBuilder = new ImmutableMap.Builder<>();
            do {
                long rawContactId = cursor.getLong(14);
                if (rawContactId != currentRawContactId) {
                    currentRawContactId = rawContactId;
                    rawContact = new RawContact(loadRawContactValues(cursor));
                    rawContactsBuilder.add(rawContact);
                }
                if (!cursor.isNull(26)) {
                    ContentValues data = loadDataValues(cursor);
                    rawContact.addDataItemValues(data);
                    if (!cursor.isNull(51) || !cursor.isNull(53)) {
                        DataStatus status = new DataStatus(cursor);
                        long dataId = cursor.getLong(26);
                        statusesBuilder.put(Long.valueOf(dataId), status);
                    }
                }
            } while (cursor.moveToNext());
            contact.setRawContacts(rawContactsBuilder.build());
            contact.setStatuses(statusesBuilder.build());
            return contact;
        } finally {
            cursor.close();
        }
    }

    private void loadPhotoBinaryData(Contact contactData) {
        InputStream inputStream;
        AssetFileDescriptor fd;
        loadThumbnailBinaryData(contactData);
        String photoUri = contactData.getPhotoUri();
        if (photoUri != null) {
            try {
                Uri uri = Uri.parse(photoUri);
                String scheme = uri.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    inputStream = new URL(photoUri).openStream();
                    fd = null;
                } else {
                    fd = getContext().getContentResolver().openAssetFileDescriptor(uri, "r");
                    inputStream = fd.createInputStream();
                }
                byte[] buffer = new byte[16384];
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (true) {
                    try {
                        int size = inputStream.read(buffer);
                        if (size == -1) {
                            break;
                        } else {
                            baos.write(buffer, 0, size);
                        }
                    } finally {
                        inputStream.close();
                        if (fd != null) {
                            fd.close();
                        }
                    }
                }
                contactData.setPhotoBinaryData(baos.toByteArray());
                if (fd != null) {
                    return;
                } else {
                    return;
                }
            } catch (IOException e) {
            }
        }
        contactData.setPhotoBinaryData(contactData.getThumbnailPhotoBinaryData());
    }

    private void loadThumbnailBinaryData(Contact contactData) {
        long photoId = contactData.getPhotoId();
        if (photoId > 0) {
            for (RawContact rawContact : contactData.getRawContacts()) {
                Iterator<DataItem> it = rawContact.getDataItems().iterator();
                while (true) {
                    if (it.hasNext()) {
                        DataItem dataItem = it.next();
                        if (dataItem.getId() == photoId) {
                            if (dataItem instanceof PhotoDataItem) {
                                PhotoDataItem photo = (PhotoDataItem) dataItem;
                                contactData.setThumbnailPhotoBinaryData(photo.getPhoto());
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadInvitableAccountTypes(Contact contactData) {
        ImmutableList.Builder<AccountType> resultListBuilder = new ImmutableList.Builder<>();
        if (!contactData.isUserProfile()) {
            Map<AccountTypeWithDataSet, AccountType> invitables = AccountTypeManager.getInstance(getContext()).getUsableInvitableAccountTypes();
            if (!invitables.isEmpty()) {
                Map<AccountTypeWithDataSet, AccountType> resultMap = Maps.newHashMap(invitables);
                for (RawContact rawContact : contactData.getRawContacts()) {
                    AccountTypeWithDataSet type = AccountTypeWithDataSet.get(rawContact.getAccountTypeString(), rawContact.getDataSet());
                    resultMap.remove(type);
                }
                resultListBuilder.addAll((Iterable<? extends AccountType>) resultMap.values());
            }
        }
        contactData.setInvitableAccountTypes(resultListBuilder.build());
    }

    private Contact loadContactHeaderData(Cursor cursor, Uri contactUri) {
        Uri lookupUri;
        String directoryParameter = contactUri.getQueryParameter("directory");
        long directoryId = directoryParameter == null ? 0L : Long.parseLong(directoryParameter);
        long contactId = cursor.getLong(13);
        String lookupKey = cursor.getString(2);
        long nameRawContactId = cursor.getLong(0);
        int displayNameSource = cursor.getInt(1);
        String displayName = cursor.getString(3);
        String altDisplayName = cursor.getString(4);
        String phoneticName = cursor.getString(5);
        long photoId = cursor.getLong(6);
        String photoUri = cursor.getString(58);
        boolean starred = cursor.getInt(7) != 0;
        Integer presence = cursor.isNull(8) ? null : Integer.valueOf(cursor.getInt(8));
        boolean sendToVoicemail = cursor.getInt(59) == 1;
        String customRingtone = cursor.getString(60);
        boolean isUserProfile = cursor.getInt(61) == 1;
        if (directoryId == 0 || directoryId == 1) {
            lookupUri = ContentUris.withAppendedId(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey), contactId);
        } else {
            lookupUri = contactUri;
        }
        return new Contact(this.mRequestedUri, contactUri, lookupUri, directoryId, lookupKey, contactId, nameRawContactId, displayNameSource, photoId, photoUri, displayName, altDisplayName, phoneticName, starred, presence, sendToVoicemail, customRingtone, isUserProfile);
    }

    private ContentValues loadRawContactValues(Cursor cursor) {
        ContentValues cv = new ContentValues();
        cv.put("_id", Long.valueOf(cursor.getLong(14)));
        cursorColumnToContentValues(cursor, cv, 15);
        cursorColumnToContentValues(cursor, cv, 16);
        cursorColumnToContentValues(cursor, cv, 17);
        cursorColumnToContentValues(cursor, cv, 18);
        cursorColumnToContentValues(cursor, cv, 19);
        cursorColumnToContentValues(cursor, cv, 20);
        cursorColumnToContentValues(cursor, cv, 21);
        cursorColumnToContentValues(cursor, cv, 22);
        cursorColumnToContentValues(cursor, cv, 23);
        cursorColumnToContentValues(cursor, cv, 24);
        cursorColumnToContentValues(cursor, cv, 25);
        cursorColumnToContentValues(cursor, cv, 13);
        cursorColumnToContentValues(cursor, cv, 7);
        return cv;
    }

    private ContentValues loadDataValues(Cursor cursor) {
        ContentValues cv = new ContentValues();
        cv.put("_id", Long.valueOf(cursor.getLong(26)));
        cursorColumnToContentValues(cursor, cv, 27);
        cursorColumnToContentValues(cursor, cv, 28);
        cursorColumnToContentValues(cursor, cv, 29);
        cursorColumnToContentValues(cursor, cv, 30);
        cursorColumnToContentValues(cursor, cv, 31);
        cursorColumnToContentValues(cursor, cv, 32);
        cursorColumnToContentValues(cursor, cv, 33);
        cursorColumnToContentValues(cursor, cv, 34);
        cursorColumnToContentValues(cursor, cv, 35);
        cursorColumnToContentValues(cursor, cv, 36);
        cursorColumnToContentValues(cursor, cv, 37);
        cursorColumnToContentValues(cursor, cv, 38);
        cursorColumnToContentValues(cursor, cv, 39);
        cursorColumnToContentValues(cursor, cv, 40);
        cursorColumnToContentValues(cursor, cv, 41);
        cursorColumnToContentValues(cursor, cv, 42);
        cursorColumnToContentValues(cursor, cv, 43);
        cursorColumnToContentValues(cursor, cv, 44);
        cursorColumnToContentValues(cursor, cv, 45);
        cursorColumnToContentValues(cursor, cv, 46);
        cursorColumnToContentValues(cursor, cv, 47);
        cursorColumnToContentValues(cursor, cv, 48);
        cursorColumnToContentValues(cursor, cv, 49);
        cursorColumnToContentValues(cursor, cv, 50);
        cursorColumnToContentValues(cursor, cv, 52);
        cursorColumnToContentValues(cursor, cv, 62);
        cursorColumnToContentValues(cursor, cv, 63);
        return cv;
    }

    private void cursorColumnToContentValues(Cursor cursor, ContentValues values, int index) {
        switch (cursor.getType(index)) {
            case 0:
                return;
            case 1:
                values.put(ContactQuery.COLUMNS[index], Long.valueOf(cursor.getLong(index)));
                return;
            case 2:
            default:
                throw new IllegalStateException("Invalid or unhandled data type");
            case 3:
                values.put(ContactQuery.COLUMNS[index], cursor.getString(index));
                return;
            case 4:
                values.put(ContactQuery.COLUMNS[index], cursor.getBlob(index));
                return;
        }
    }

    private void loadDirectoryMetaData(Contact result) {
        long directoryId = result.getDirectoryId();
        Cursor cursor = getContext().getContentResolver().query(ContentUris.withAppendedId(ContactsContract.Directory.CONTENT_URI, directoryId), DirectoryQuery.COLUMNS, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String displayName = cursor.getString(0);
                    String packageName = cursor.getString(1);
                    int typeResourceId = cursor.getInt(2);
                    String accountType = cursor.getString(3);
                    String accountName = cursor.getString(4);
                    int exportSupport = cursor.getInt(5);
                    String directoryType = null;
                    if (!TextUtils.isEmpty(packageName)) {
                        PackageManager pm = getContext().getPackageManager();
                        try {
                            Resources resources = pm.getResourcesForApplication(packageName);
                            directoryType = resources.getString(typeResourceId);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.w(TAG, "Contact directory resource not found: " + packageName + "." + typeResourceId);
                        }
                    }
                    result.setDirectoryMetaData(displayName, directoryType, accountType, accountName, exportSupport);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private static class AccountKey {
        private final String mAccountName;
        private final String mAccountType;
        private final String mDataSet;

        public AccountKey(String accountName, String accountType, String dataSet) {
            this.mAccountName = accountName;
            this.mAccountType = accountType;
            this.mDataSet = dataSet;
        }

        public int hashCode() {
            return Objects.hash(this.mAccountName, this.mAccountType, this.mDataSet);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof AccountKey)) {
                return false;
            }
            AccountKey other = (AccountKey) obj;
            return Objects.equals(this.mAccountName, other.mAccountName) && Objects.equals(this.mAccountType, other.mAccountType) && Objects.equals(this.mDataSet, other.mDataSet);
        }
    }

    private void loadGroupMetaData(Contact result) {
        boolean favorites;
        StringBuilder selection = new StringBuilder();
        ArrayList<String> selectionArgs = new ArrayList<>();
        HashSet<AccountKey> accountsSeen = new HashSet<>();
        for (RawContact rawContact : result.getRawContacts()) {
            String accountName = rawContact.getAccountName();
            String accountType = rawContact.getAccountTypeString();
            String dataSet = rawContact.getDataSet();
            AccountKey accountKey = new AccountKey(accountName, accountType, dataSet);
            if (accountName != null && accountType != null && !accountsSeen.contains(accountKey)) {
                accountsSeen.add(accountKey);
                if (selection.length() != 0) {
                    selection.append(" OR ");
                }
                selection.append("(account_name=? AND account_type=?");
                selectionArgs.add(accountName);
                selectionArgs.add(accountType);
                if (dataSet != null) {
                    selection.append(" AND data_set=?");
                    selectionArgs.add(dataSet);
                } else {
                    selection.append(" AND data_set IS NULL");
                }
                selection.append(")");
            }
        }
        ImmutableList.Builder<GroupMetaData> groupListBuilder = new ImmutableList.Builder<>();
        Cursor cursor = getContext().getContentResolver().query(ContactsContract.Groups.CONTENT_URI, GroupQuery.COLUMNS, selection.toString(), (String[]) selectionArgs.toArray(new String[0]), null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    String accountName2 = cursor.getString(0);
                    String accountType2 = cursor.getString(1);
                    String dataSet2 = cursor.getString(2);
                    long groupId = cursor.getLong(3);
                    String title = cursor.getString(4);
                    boolean defaultGroup = (cursor.isNull(5) || cursor.getInt(5) == 0) ? false : true;
                    if (cursor.isNull(6)) {
                        favorites = false;
                    } else {
                        favorites = cursor.getInt(6) != 0;
                    }
                    groupListBuilder.add(new GroupMetaData(accountName2, accountType2, dataSet2, groupId, title, defaultGroup, favorites));
                } finally {
                    cursor.close();
                }
            }
        }
        result.setGroupMetaData(groupListBuilder.build());
    }

    private void computeFormattedPhoneNumbers(Contact contactData) {
        String countryIso = GeoUtil.getCurrentCountryIso(getContext());
        ImmutableList<RawContact> rawContacts = contactData.getRawContacts();
        int rawContactCount = rawContacts.size();
        for (int rawContactIndex = 0; rawContactIndex < rawContactCount; rawContactIndex++) {
            RawContact rawContact = rawContacts.get(rawContactIndex);
            List<DataItem> dataItems = rawContact.getDataItems();
            int dataCount = dataItems.size();
            for (int dataIndex = 0; dataIndex < dataCount; dataIndex++) {
                DataItem dataItem = dataItems.get(dataIndex);
                if (dataItem instanceof PhoneDataItem) {
                    PhoneDataItem phoneDataItem = (PhoneDataItem) dataItem;
                    phoneDataItem.computeFormattedPhoneNumber(countryIso);
                }
            }
        }
    }

    @Override
    public void deliverResult(Contact result) {
        unregisterObserver();
        if (!isReset() && result != null) {
            this.mContact = result;
            if (result.isLoaded()) {
                this.mLookupUri = result.getLookupUri();
                if (!result.isDirectoryEntry()) {
                    Log.i(TAG, "Registering content observer for " + this.mLookupUri);
                    if (this.mObserver == null) {
                        this.mObserver = new Loader.ForceLoadContentObserver(this);
                    }
                    getContext().getContentResolver().registerContentObserver(this.mLookupUri, true, this.mObserver);
                }
                if (this.mPostViewNotification) {
                    postViewNotificationToSyncAdapter();
                }
            }
            super.deliverResult(this.mContact);
        }
    }

    private void postViewNotificationToSyncAdapter() {
        Context context = getContext();
        for (RawContact rawContact : this.mContact.getRawContacts()) {
            long rawContactId = rawContact.getId().longValue();
            if (!this.mNotifiedRawContactIds.contains(Long.valueOf(rawContactId))) {
                this.mNotifiedRawContactIds.add(Long.valueOf(rawContactId));
                AccountType accountType = rawContact.getAccountType(context);
                String serviceName = accountType.getViewContactNotifyServiceClassName();
                String servicePackageName = accountType.getViewContactNotifyServicePackageName();
                if (!TextUtils.isEmpty(serviceName) && !TextUtils.isEmpty(servicePackageName)) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, rawContactId);
                    Intent intent = new Intent();
                    intent.setClassName(servicePackageName, serviceName);
                    intent.setAction("android.intent.action.VIEW");
                    intent.setDataAndType(uri, "vnd.android.cursor.item/raw_contact");
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
        if (this.mContact != null) {
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
}
