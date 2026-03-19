package com.android.mtkex.chips;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.mtkex.chips.Queries;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseRecipientAdapter extends BaseAdapter implements Filterable, AccountSpecifier {
    private static int mQueryType;
    private Account mAccount;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private CharSequence mCurrentConstraint;
    private final DelayedMessageHandler mDelayedMessageHandler;
    private List<RecipientEntry> mEntries;
    private EntriesUpdatedObserver mEntriesUpdatedObserver;
    private LinkedHashMap<Long, List<RecipientEntry>> mEntryMap;
    private Set<String> mExistingDestinations;
    private final Handler mHandler;
    private final LayoutInflater mInflater;
    private List<RecipientEntry> mNonAggregatedEntries;
    private final LruCache<Uri, byte[]> mPhotoCacheMap;
    private final int mPreferredMaxResultCount;
    private final Queries.Query mQuery;
    private int mQueryPhoneNum;
    private int mRemainingDirectoryCount;
    private boolean mShowPhoneAndEmail;
    private List<RecipientEntry> mTempEntries;
    private static boolean piLoggable = !"user".equals(SystemProperties.get("ro.build.type", "user"));
    private static boolean mShowDuplicateResults = false;

    protected static class DirectoryListQuery {
        public static final Uri URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "directories");
        public static final String[] PROJECTION = {"_id", "accountName", "accountType", "displayName", "packageName", "typeResourceId"};
    }

    public static final class DirectorySearchParams {
        public String accountName;
        public String accountType;
        public CharSequence constraint;
        public long directoryId;
        public String directoryType;
        public String displayName;
        public DirectoryFilter filter;
    }

    protected interface EntriesUpdatedObserver {
        void onChanged(List<RecipientEntry> list);
    }

    private static class PhotoQuery {
        public static final String[] PROJECTION = {"data15"};
    }

    static int access$2010(BaseRecipientAdapter baseRecipientAdapter) {
        int i = baseRecipientAdapter.mRemainingDirectoryCount;
        baseRecipientAdapter.mRemainingDirectoryCount = i - 1;
        return i;
    }

    protected static class TemporaryEntry {
        public final long contactId;
        public final long dataId;
        public final String destination;
        private int destinationKind;
        public final String destinationLabel;
        public final int destinationType;
        public final String displayName;
        public final int displayNameSource;
        public final boolean isGalContact;
        public final String thumbnailUriString;

        public TemporaryEntry(Cursor cursor, boolean z) {
            this.displayName = cursor.getString(0);
            this.destination = cursor.getString(1);
            this.destinationType = cursor.getInt(2);
            this.destinationLabel = cursor.getString(3);
            this.contactId = cursor.getLong(4);
            this.dataId = cursor.getLong(5);
            this.thumbnailUriString = cursor.getString(6);
            this.displayNameSource = cursor.getInt(7);
            this.isGalContact = z;
        }

        public void setDestinationKind(int i) {
            this.destinationKind = i;
        }

        public int getDestinationKind() {
            return this.destinationKind;
        }
    }

    private static class DefaultFilterResult {
        public final List<RecipientEntry> entries;
        public final LinkedHashMap<Long, List<RecipientEntry>> entryMap;
        public final Set<String> existingDestinations;
        public final List<RecipientEntry> nonAggregatedEntries;
        public final List<DirectorySearchParams> paramsList;

        public DefaultFilterResult(List<RecipientEntry> list, LinkedHashMap<Long, List<RecipientEntry>> linkedHashMap, List<RecipientEntry> list2, Set<String> set, List<DirectorySearchParams> list3) {
            this.entries = list;
            this.entryMap = linkedHashMap;
            this.nonAggregatedEntries = list2;
            this.existingDestinations = set;
            this.paramsList = list3;
        }
    }

    private final class DefaultFilter extends Filter {
        private DefaultFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) throws Throwable {
            Cursor cursorDoQuery;
            List<DirectorySearchParams> list;
            BaseRecipientAdapter.printSensitiveDebugLog("BaseRecipientAdapter", "start filtering. constraint: " + ((Object) charSequence) + ", thread:" + Thread.currentThread());
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (TextUtils.isEmpty(charSequence)) {
                BaseRecipientAdapter.this.clearTempEntries();
                return filterResults;
            }
            Cursor cursor = null;
            try {
                cursorDoQuery = BaseRecipientAdapter.this.doQuery(charSequence, BaseRecipientAdapter.this.mPreferredMaxResultCount, null);
                try {
                    if (cursorDoQuery == null) {
                        Log.w("BaseRecipientAdapter", "null cursor returned for default Email filter query.");
                    } else {
                        LinkedHashMap linkedHashMap = new LinkedHashMap();
                        ArrayList arrayList = new ArrayList();
                        HashSet hashSet = new HashSet();
                        int i = 0;
                        while (cursorDoQuery.moveToNext()) {
                            if (BaseRecipientAdapter.mQueryType == 1) {
                                TemporaryEntry temporaryEntry = new TemporaryEntry(cursorDoQuery, false);
                                if (!BaseRecipientAdapter.this.mShowPhoneAndEmail || i < BaseRecipientAdapter.this.mQueryPhoneNum) {
                                    temporaryEntry.setDestinationKind(2);
                                } else {
                                    temporaryEntry.setDestinationKind(1);
                                }
                                i++;
                                BaseRecipientAdapter.putOneEntry(temporaryEntry, true, linkedHashMap, arrayList, hashSet);
                            } else {
                                BaseRecipientAdapter.putOneEntry(new TemporaryEntry(cursorDoQuery, false), true, linkedHashMap, arrayList, hashSet);
                            }
                        }
                        List listConstructEntryList = BaseRecipientAdapter.this.constructEntryList(linkedHashMap, arrayList);
                        int size = BaseRecipientAdapter.this.mPreferredMaxResultCount - hashSet.size();
                        if (size > 0) {
                            Log.d("BaseRecipientAdapter", "More entries should be needed (current: " + hashSet.size() + ", remaining limit: " + size + ") ");
                            Cursor cursorQuery = BaseRecipientAdapter.this.mContentResolver.query(DirectoryListQuery.URI, DirectoryListQuery.PROJECTION, null, null, null);
                            try {
                                list = BaseRecipientAdapter.setupOtherDirectories(BaseRecipientAdapter.this.mContext, cursorQuery, BaseRecipientAdapter.this.mAccount);
                                cursor = cursorQuery;
                            } catch (Throwable th) {
                                th = th;
                                cursor = cursorQuery;
                                if (cursorDoQuery != null) {
                                    Log.d("BaseRecipientAdapter", "[DefaultFilter.performFiltering] close defaultDirectoryCursor");
                                    cursorDoQuery.close();
                                }
                                if (cursor != null) {
                                    Log.d("BaseRecipientAdapter", "[DefaultFilter.performFiltering] close directoryCursor");
                                    cursor.close();
                                }
                                throw th;
                            }
                        } else {
                            list = null;
                        }
                        filterResults.values = new DefaultFilterResult(listConstructEntryList, linkedHashMap, arrayList, hashSet, list);
                        filterResults.count = 1;
                    }
                    if (cursorDoQuery != null) {
                        Log.d("BaseRecipientAdapter", "[DefaultFilter.performFiltering] close defaultDirectoryCursor");
                        cursorDoQuery.close();
                    }
                    if (cursor != null) {
                        Log.d("BaseRecipientAdapter", "[DefaultFilter.performFiltering] close directoryCursor");
                        cursor.close();
                    }
                    return filterResults;
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (Throwable th3) {
                th = th3;
                cursorDoQuery = null;
            }
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            BaseRecipientAdapter.this.mCurrentConstraint = charSequence;
            BaseRecipientAdapter.this.clearTempEntries();
            if (filterResults.values != null) {
                DefaultFilterResult defaultFilterResult = (DefaultFilterResult) filterResults.values;
                BaseRecipientAdapter.this.mEntryMap = defaultFilterResult.entryMap;
                BaseRecipientAdapter.this.mNonAggregatedEntries = defaultFilterResult.nonAggregatedEntries;
                BaseRecipientAdapter.this.mExistingDestinations = defaultFilterResult.existingDestinations;
                BaseRecipientAdapter.this.updateEntries(defaultFilterResult.entries);
                if (defaultFilterResult.paramsList != null) {
                    BaseRecipientAdapter.this.startSearchOtherDirectories(charSequence, defaultFilterResult.paramsList, BaseRecipientAdapter.this.mPreferredMaxResultCount - defaultFilterResult.existingDestinations.size());
                }
            }
        }

        @Override
        public CharSequence convertResultToString(Object obj) {
            RecipientEntry recipientEntry = (RecipientEntry) obj;
            String displayName = recipientEntry.getDisplayName();
            String destination = recipientEntry.getDestination();
            if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
                return destination;
            }
            return new Rfc822Token(displayName, destination, null).toString();
        }
    }

    protected class DirectoryFilter extends Filter {
        private int mLimit;
        private final DirectorySearchParams mParams;

        public DirectoryFilter(DirectorySearchParams directorySearchParams) {
            this.mParams = directorySearchParams;
        }

        public synchronized void setLimit(int i) {
            this.mLimit = i;
        }

        public synchronized int getLimit() {
            return this.mLimit;
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) throws Throwable {
            Cursor cursorDoQuery;
            Log.d("BaseRecipientAdapter", "DirectoryFilter#performFiltering. directoryId: " + this.mParams.directoryId + ", constraint: " + ((Object) charSequence) + ", thread: " + Thread.currentThread());
            Filter.FilterResults filterResults = new Filter.FilterResults();
            filterResults.values = null;
            int i = 0;
            filterResults.count = 0;
            if (!TextUtils.isEmpty(charSequence)) {
                ArrayList arrayList = new ArrayList();
                try {
                    cursorDoQuery = BaseRecipientAdapter.this.doQuery(charSequence, getLimit(), Long.valueOf(this.mParams.directoryId));
                    if (cursorDoQuery != null) {
                        while (cursorDoQuery.moveToNext()) {
                            try {
                                if (BaseRecipientAdapter.mQueryType == 1) {
                                    TemporaryEntry temporaryEntry = new TemporaryEntry(cursorDoQuery, true);
                                    if (!BaseRecipientAdapter.this.mShowPhoneAndEmail || temporaryEntry.contactId != 0) {
                                        if (BaseRecipientAdapter.this.mShowPhoneAndEmail && i >= BaseRecipientAdapter.this.mQueryPhoneNum) {
                                            temporaryEntry.setDestinationKind(1);
                                        } else {
                                            temporaryEntry.setDestinationKind(2);
                                        }
                                        i++;
                                        arrayList.add(temporaryEntry);
                                    }
                                } else {
                                    arrayList.add(new TemporaryEntry(cursorDoQuery, true));
                                }
                            } catch (Throwable th) {
                                th = th;
                                if (cursorDoQuery != null) {
                                    Log.d("BaseRecipientAdapter", "[DirectoryFilter.performFiltering] close cursor");
                                    cursorDoQuery.close();
                                }
                                throw th;
                            }
                        }
                    }
                    if (cursorDoQuery != null) {
                        Log.d("BaseRecipientAdapter", "[DirectoryFilter.performFiltering] close cursor");
                        cursorDoQuery.close();
                    }
                    if (!arrayList.isEmpty()) {
                        filterResults.values = arrayList;
                        filterResults.count = 1;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    cursorDoQuery = null;
                }
            }
            BaseRecipientAdapter.printSensitiveVerboseLog("BaseRecipientAdapter", "finished loading directory \"" + this.mParams.displayName + "\" with query " + ((Object) charSequence));
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            Log.d("BaseRecipientAdapter", "DirectoryFilter#publishResult. constraint: " + ((Object) charSequence) + ", mCurrentConstraint: " + ((Object) BaseRecipientAdapter.this.mCurrentConstraint));
            BaseRecipientAdapter.this.mDelayedMessageHandler.removeDelayedLoadMessage();
            if (TextUtils.equals(charSequence, BaseRecipientAdapter.this.mCurrentConstraint)) {
                if (filterResults.count > 0) {
                    Iterator it = ((ArrayList) filterResults.values).iterator();
                    while (it.hasNext()) {
                        BaseRecipientAdapter.putOneEntry((TemporaryEntry) it.next(), this.mParams.directoryId == 0, BaseRecipientAdapter.this.mEntryMap, BaseRecipientAdapter.this.mNonAggregatedEntries, BaseRecipientAdapter.this.mExistingDestinations);
                    }
                }
                BaseRecipientAdapter.access$2010(BaseRecipientAdapter.this);
                if (BaseRecipientAdapter.this.mRemainingDirectoryCount > 0) {
                    Log.d("BaseRecipientAdapter", "Resend delayed load message. Current mRemainingDirectoryLoad: " + BaseRecipientAdapter.this.mRemainingDirectoryCount);
                    BaseRecipientAdapter.this.mDelayedMessageHandler.sendDelayedLoadMessage();
                }
                if (filterResults.count > 0 || BaseRecipientAdapter.this.mRemainingDirectoryCount == 0) {
                    BaseRecipientAdapter.this.clearTempEntries();
                }
            }
            BaseRecipientAdapter.this.updateEntries(BaseRecipientAdapter.this.constructEntryList(BaseRecipientAdapter.this.mEntryMap, BaseRecipientAdapter.this.mNonAggregatedEntries));
        }
    }

    private final class DelayedMessageHandler extends Handler {
        private DelayedMessageHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (BaseRecipientAdapter.this.mRemainingDirectoryCount > 0) {
                BaseRecipientAdapter.this.updateEntries(BaseRecipientAdapter.this.constructEntryList(BaseRecipientAdapter.this.mEntryMap, BaseRecipientAdapter.this.mNonAggregatedEntries));
            }
        }

        public void sendDelayedLoadMessage() {
            sendMessageDelayed(obtainMessage(1, 0, 0, null), 1000L);
        }

        public void removeDelayedLoadMessage() {
            removeMessages(1);
        }
    }

    public BaseRecipientAdapter(Context context) {
        this(context, 100, 0);
    }

    public BaseRecipientAdapter(Context context, int i, int i2) {
        this.mHandler = new Handler();
        this.mDelayedMessageHandler = new DelayedMessageHandler();
        this.mShowPhoneAndEmail = false;
        this.mQueryPhoneNum = 0;
        Log.d("BaseRecipientAdapter", "[BaseRecipientAdapter] preferredMaxResultCount: " + i + ", queryMode: " + i2);
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mInflater = LayoutInflater.from(context);
        this.mPreferredMaxResultCount = i;
        this.mPhotoCacheMap = new LruCache<>(100);
        mQueryType = i2;
        if (i2 == 0) {
            this.mQuery = Queries.EMAIL;
            return;
        }
        if (i2 == 1) {
            this.mQuery = Queries.PHONE;
            return;
        }
        this.mQuery = Queries.EMAIL;
        Log.e("BaseRecipientAdapter", "Unsupported query type: " + i2);
    }

    public int getQueryType() {
        return mQueryType;
    }

    @Override
    public Filter getFilter() {
        return new DefaultFilter();
    }

    public Map<String, RecipientEntry> getMatchingRecipients(Set<String> set) {
        return null;
    }

    public static List<DirectorySearchParams> setupOtherDirectories(Context context, Cursor cursor, Account account) {
        Log.d("BaseRecipientAdapter", "[setupOtherDirectories]");
        PackageManager packageManager = context.getPackageManager();
        ArrayList arrayList = new ArrayList();
        DirectorySearchParams directorySearchParams = null;
        while (cursor.moveToNext()) {
            long j = cursor.getLong(0);
            if (j != 1) {
                DirectorySearchParams directorySearchParams2 = new DirectorySearchParams();
                String string = cursor.getString(4);
                int i = cursor.getInt(5);
                directorySearchParams2.directoryId = j;
                directorySearchParams2.displayName = cursor.getString(3);
                directorySearchParams2.accountName = cursor.getString(1);
                directorySearchParams2.accountType = cursor.getString(2);
                if (string != null && i != 0) {
                    try {
                        directorySearchParams2.directoryType = packageManager.getResourcesForApplication(string).getString(i);
                        if (directorySearchParams2.directoryType == null) {
                            Log.e("BaseRecipientAdapter", "Cannot resolve directory name: " + i + "@" + string);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("BaseRecipientAdapter", "Cannot resolve directory name: " + i + "@" + string, e);
                    }
                }
                if (directorySearchParams != null || account == null || !account.name.equals(directorySearchParams2.accountName) || !account.type.equals(directorySearchParams2.accountType)) {
                    arrayList.add(directorySearchParams2);
                } else {
                    directorySearchParams = directorySearchParams2;
                }
            }
        }
        if (directorySearchParams != null) {
            arrayList.add(1, directorySearchParams);
        }
        return arrayList;
    }

    protected void startSearchOtherDirectories(CharSequence charSequence, List<DirectorySearchParams> list, int i) {
        Log.d("BaseRecipientAdapter", "[startSearchOtherDirectories]");
        int size = list.size();
        for (int i2 = 1; i2 < size; i2++) {
            DirectorySearchParams directorySearchParams = list.get(i2);
            directorySearchParams.constraint = charSequence;
            if (directorySearchParams.filter == null) {
                directorySearchParams.filter = new DirectoryFilter(directorySearchParams);
            }
            directorySearchParams.filter.setLimit(i);
            directorySearchParams.filter.filter(charSequence);
        }
        this.mRemainingDirectoryCount = size - 1;
        this.mDelayedMessageHandler.sendDelayedLoadMessage();
    }

    private static void putOneEntry(TemporaryEntry temporaryEntry, boolean z, LinkedHashMap<Long, List<RecipientEntry>> linkedHashMap, List<RecipientEntry> list, Set<String> set) {
        if (mShowDuplicateResults || !set.contains(temporaryEntry.destination)) {
            set.add(temporaryEntry.destination);
            if (!z) {
                list.add(RecipientEntry.constructTopLevelEntry(temporaryEntry.displayName, temporaryEntry.displayNameSource, temporaryEntry.destination, temporaryEntry.destinationType, temporaryEntry.destinationLabel, temporaryEntry.contactId, temporaryEntry.dataId, temporaryEntry.thumbnailUriString, true, temporaryEntry.isGalContact));
                if (mQueryType == 1) {
                    if (temporaryEntry.getDestinationKind() != 1) {
                        if (temporaryEntry.getDestinationKind() == 2) {
                            list.get(list.size() - 1).setDestinationKind(2);
                            return;
                        }
                        return;
                    }
                    list.get(list.size() - 1).setDestinationKind(1);
                    return;
                }
                return;
            }
            if (linkedHashMap.containsKey(Long.valueOf(temporaryEntry.contactId))) {
                List<RecipientEntry> list2 = linkedHashMap.get(Long.valueOf(temporaryEntry.contactId));
                list2.add(RecipientEntry.constructSecondLevelEntry(temporaryEntry.displayName, temporaryEntry.displayNameSource, temporaryEntry.destination, temporaryEntry.destinationType, temporaryEntry.destinationLabel, temporaryEntry.contactId, temporaryEntry.dataId, temporaryEntry.thumbnailUriString, true, temporaryEntry.isGalContact));
                if (mQueryType == 1) {
                    if (temporaryEntry.getDestinationKind() != 1) {
                        if (temporaryEntry.getDestinationKind() == 2) {
                            list2.get(list2.size() - 1).setDestinationKind(2);
                            return;
                        }
                        return;
                    }
                    list2.get(list2.size() - 1).setDestinationKind(1);
                    return;
                }
                return;
            }
            ArrayList arrayList = new ArrayList();
            arrayList.add(RecipientEntry.constructTopLevelEntry(temporaryEntry.displayName, temporaryEntry.displayNameSource, temporaryEntry.destination, temporaryEntry.destinationType, temporaryEntry.destinationLabel, temporaryEntry.contactId, temporaryEntry.dataId, temporaryEntry.thumbnailUriString, true, temporaryEntry.isGalContact));
            if (mQueryType == 1) {
                if (temporaryEntry.getDestinationKind() == 1) {
                    arrayList.get(arrayList.size() - 1).setDestinationKind(1);
                } else if (temporaryEntry.getDestinationKind() == 2) {
                    arrayList.get(arrayList.size() - 1).setDestinationKind(2);
                }
            }
            linkedHashMap.put(Long.valueOf(temporaryEntry.contactId), arrayList);
        }
    }

    private List<RecipientEntry> constructEntryList(LinkedHashMap<Long, List<RecipientEntry>> linkedHashMap, List<RecipientEntry> list) {
        ArrayList arrayList = new ArrayList();
        Iterator<Map.Entry<Long, List<RecipientEntry>>> it = linkedHashMap.entrySet().iterator();
        int i = 0;
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            List<RecipientEntry> value = it.next().getValue();
            int size = value.size();
            int i2 = i;
            for (int i3 = 0; i3 < size; i3++) {
                RecipientEntry recipientEntry = value.get(i3);
                arrayList.add(recipientEntry);
                tryFetchPhoto(recipientEntry);
                i2++;
            }
            if (i2 < this.mPreferredMaxResultCount) {
                i = i2;
            } else {
                i = i2;
                break;
            }
        }
        if (i < this.mPreferredMaxResultCount) {
            for (RecipientEntry recipientEntry2 : list) {
                if (i >= this.mPreferredMaxResultCount) {
                    break;
                }
                arrayList.add(recipientEntry2);
                tryFetchPhoto(recipientEntry2);
                i++;
            }
        }
        return arrayList;
    }

    public void registerUpdateObserver(EntriesUpdatedObserver entriesUpdatedObserver) {
        this.mEntriesUpdatedObserver = entriesUpdatedObserver;
    }

    private void updateEntries(List<RecipientEntry> list) {
        this.mEntries = list;
        if (this.mEntriesUpdatedObserver != null) {
            this.mEntriesUpdatedObserver.onChanged(list);
        }
        notifyDataSetChanged();
    }

    private void cacheCurrentEntries() {
        this.mTempEntries = this.mEntries;
    }

    private void clearTempEntries() {
        this.mTempEntries = null;
    }

    protected List<RecipientEntry> getEntries() {
        return this.mTempEntries != null ? this.mTempEntries : this.mEntries;
    }

    private void tryFetchPhoto(RecipientEntry recipientEntry) {
        Uri photoThumbnailUri = recipientEntry.getPhotoThumbnailUri();
        if (photoThumbnailUri != null) {
            byte[] bArr = this.mPhotoCacheMap.get(photoThumbnailUri);
            if (bArr != null) {
                recipientEntry.setPhotoBytes(bArr);
                return;
            }
            printSensitiveDebugLog("BaseRecipientAdapter", "No photo cache for " + recipientEntry.getDisplayName() + ". Fetch one asynchronously");
            fetchPhotoAsync(recipientEntry, photoThumbnailUri);
        }
    }

    private void fetchPhotoAsync(final RecipientEntry recipientEntry, final Uri uri) {
        new AsyncTask<Void, Void, byte[]>() {
            @Override
            protected byte[] doInBackground(Void... voidArr) {
                Cursor cursorQuery = BaseRecipientAdapter.this.mContentResolver.query(uri, PhotoQuery.PROJECTION, null, null, null);
                if (cursorQuery == null) {
                    try {
                        InputStream inputStreamOpenInputStream = BaseRecipientAdapter.this.mContentResolver.openInputStream(uri);
                        if (inputStreamOpenInputStream != null) {
                            byte[] bArr = new byte[16384];
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            while (true) {
                                try {
                                    int i = inputStreamOpenInputStream.read(bArr);
                                    if (i != -1) {
                                        byteArrayOutputStream.write(bArr, 0, i);
                                    } else {
                                        inputStreamOpenInputStream.close();
                                        return byteArrayOutputStream.toByteArray();
                                    }
                                } catch (Throwable th) {
                                    inputStreamOpenInputStream.close();
                                    throw th;
                                }
                            }
                        } else {
                            return null;
                        }
                    } catch (IOException e) {
                        return null;
                    }
                } else {
                    try {
                        if (cursorQuery.moveToFirst()) {
                            return cursorQuery.getBlob(0);
                        }
                        return null;
                    } finally {
                        cursorQuery.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(byte[] bArr) {
                recipientEntry.setPhotoBytes(bArr);
                if (bArr != null) {
                    BaseRecipientAdapter.this.mPhotoCacheMap.put(uri, bArr);
                    BaseRecipientAdapter.this.notifyDataSetChanged();
                }
            }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new Void[0]);
    }

    protected void fetchPhoto(RecipientEntry recipientEntry, Uri uri) {
        byte[] bArr = this.mPhotoCacheMap.get(uri);
        if (bArr != null) {
            recipientEntry.setPhotoBytes(bArr);
            return;
        }
        Cursor cursorQuery = this.mContentResolver.query(uri, PhotoQuery.PROJECTION, null, null, null);
        if (cursorQuery != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    byte[] blob = cursorQuery.getBlob(0);
                    recipientEntry.setPhotoBytes(blob);
                    this.mPhotoCacheMap.put(uri, blob);
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private Cursor doQuery(CharSequence charSequence, int i, Long l) {
        int i2 = i + 5;
        Uri.Builder builderAppendQueryParameter = this.mQuery.getContentFilterUri().buildUpon().appendPath(charSequence.toString()).appendQueryParameter("limit", String.valueOf(i2));
        if (l != null) {
            builderAppendQueryParameter.appendQueryParameter("directory", String.valueOf(l));
        }
        if (this.mAccount != null) {
            builderAppendQueryParameter.appendQueryParameter("name_for_primary_account", this.mAccount.name);
            builderAppendQueryParameter.appendQueryParameter("type_for_primary_account", this.mAccount.type);
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        Cursor cursorQuery = this.mContentResolver.query(builderAppendQueryParameter.build(), this.mQuery.getProjection(), null, null, "display_name");
        long jCurrentTimeMillis2 = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("[doQuery] 1st query, constraint: ");
        sb.append((Object) charSequence);
        sb.append(", result count: ");
        sb.append(cursorQuery != null ? Integer.valueOf(cursorQuery.getCount()) : "null");
        printSensitiveDebugLog("BaseRecipientAdapter", sb.toString());
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Time for autocomplete (query: ");
        sb2.append((Object) charSequence);
        sb2.append(", directoryId: ");
        sb2.append(l);
        sb2.append(", num_of_results: ");
        sb2.append(cursorQuery != null ? Integer.valueOf(cursorQuery.getCount()) : "null");
        sb2.append("): ");
        sb2.append(jCurrentTimeMillis2 - jCurrentTimeMillis);
        sb2.append(" ms");
        printSensitiveDebugLog("BaseRecipientAdapter", sb2.toString());
        if (mQueryType == 1 && this.mShowPhoneAndEmail) {
            this.mQueryPhoneNum = cursorQuery != null ? cursorQuery.getCount() : 0;
            Queries.Query query = Queries.EMAIL;
            Uri.Builder builderAppendQueryParameter2 = query.getContentFilterUri().buildUpon().appendPath(charSequence.toString()).appendQueryParameter("limit", String.valueOf(i2));
            if (l != null) {
                builderAppendQueryParameter2.appendQueryParameter("directory", String.valueOf(l));
            }
            if (this.mAccount != null) {
                builderAppendQueryParameter2.appendQueryParameter("name_for_primary_account", this.mAccount.name);
                builderAppendQueryParameter2.appendQueryParameter("type_for_primary_account", this.mAccount.type);
            }
            Cursor cursorQuery2 = this.mContentResolver.query(builderAppendQueryParameter2.build(), query.getProjection(), null, null, "display_name");
            StringBuilder sb3 = new StringBuilder();
            sb3.append("[doQuery] 2nd query, constraint: ");
            sb3.append((Object) charSequence);
            sb3.append(", result count: ");
            sb3.append(cursorQuery2 != null ? Integer.valueOf(cursorQuery2.getCount()) : "null");
            printSensitiveDebugLog("BaseRecipientAdapter", sb3.toString());
            return new MergeCursor(new Cursor[]{cursorQuery, cursorQuery2});
        }
        return cursorQuery;
    }

    @Override
    public int getCount() {
        List<RecipientEntry> entries = getEntries();
        if (entries != null) {
            return entries.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (i >= getEntries().size()) {
            return null;
        }
        return getEntries().get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int i) {
        return getEntries().get(i).getEntryType();
    }

    @Override
    public boolean isEnabled(int i) {
        return getEntries().get(i).isSelectable();
    }

    private static class DropDownListViewHolder {
        TextView dest;
        TextView destType;
        ImageView img;
        TextView name;

        private DropDownListViewHolder() {
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        DropDownListViewHolder dropDownListViewHolder;
        String str;
        String upperCase;
        if (view == null) {
            view = this.mInflater.inflate(getItemLayout(), viewGroup, false);
            dropDownListViewHolder = new DropDownListViewHolder();
            if (view != null) {
                dropDownListViewHolder.name = (TextView) view.findViewById(getDisplayNameId());
                dropDownListViewHolder.dest = (TextView) view.findViewById(getDestinationId());
                dropDownListViewHolder.destType = (TextView) view.findViewById(getDestinationTypeId());
                dropDownListViewHolder.img = (ImageView) view.findViewById(getPhotoId());
                view.setTag(dropDownListViewHolder);
            }
        } else {
            dropDownListViewHolder = (DropDownListViewHolder) view.getTag();
        }
        RecipientEntry recipientEntry = getEntries().get(i);
        String displayName = recipientEntry.getDisplayName();
        String destination = recipientEntry.getDestination();
        if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
            str = recipientEntry.isFirstLevel() ? null : destination;
        } else {
            destination = displayName;
            str = destination;
        }
        TextView textView = dropDownListViewHolder.name;
        TextView textView2 = dropDownListViewHolder.dest;
        TextView textView3 = dropDownListViewHolder.destType;
        ImageView imageView = dropDownListViewHolder.img;
        textView.setText(destination);
        if (!TextUtils.isEmpty(str)) {
            textView2.setText(str);
        } else {
            textView2.setText((CharSequence) null);
        }
        if (textView3 != null) {
            if (this.mShowPhoneAndEmail) {
                if (recipientEntry.getDestinationKind() == 1) {
                    upperCase = Queries.EMAIL.getTypeLabel(this.mContext.getResources(), recipientEntry.getDestinationType(), recipientEntry.getDestinationLabel()).toString().toUpperCase();
                } else {
                    upperCase = Queries.PHONE.getTypeLabel(this.mContext.getResources(), recipientEntry.getDestinationType(), recipientEntry.getDestinationLabel()).toString().toUpperCase();
                }
            } else {
                upperCase = this.mQuery.getTypeLabel(this.mContext.getResources(), recipientEntry.getDestinationType(), recipientEntry.getDestinationLabel()).toString().toUpperCase();
            }
            textView3.setText(upperCase);
        }
        if (recipientEntry.isFirstLevel()) {
            textView.setVisibility(0);
            if (imageView != null) {
                imageView.setVisibility(0);
                byte[] photoBytes = recipientEntry.getPhotoBytes();
                if (photoBytes != null) {
                    Bitmap bitmap = recipientEntry.getBitmap();
                    if (bitmap == null) {
                        bitmap = BitmapFactory.decodeByteArray(photoBytes, 0, photoBytes.length);
                        recipientEntry.setBitmap(bitmap);
                    }
                    imageView.setImageBitmap(bitmap);
                } else {
                    imageView.setImageResource(getDefaultPhotoResource());
                }
            }
        } else {
            textView.setVisibility(8);
            if (imageView != null) {
                imageView.setVisibility(4);
            }
        }
        return view;
    }

    protected int getItemLayout() {
        return R.layout.chips_recipient_dropdown_item;
    }

    protected int getDefaultPhotoResource() {
        return R.drawable.ic_default_contact;
    }

    protected int getDisplayNameId() {
        return android.R.id.title;
    }

    protected int getDestinationId() {
        return android.R.id.text1;
    }

    protected int getDestinationTypeId() {
        return android.R.id.text2;
    }

    protected int getPhotoId() {
        return android.R.id.icon;
    }

    public Account getAccount() {
        return this.mAccount;
    }

    public boolean getShowPhoneAndEmail() {
        return this.mShowPhoneAndEmail;
    }

    public void updatePhotoCacheByUri(Uri uri) {
        Cursor cursorQuery;
        if (uri != null && (cursorQuery = this.mContentResolver.query(uri, PhotoQuery.PROJECTION, null, null, null)) != null) {
            try {
                if (cursorQuery.moveToFirst()) {
                    this.mPhotoCacheMap.put(uri, cursorQuery.getBlob(0));
                }
            } finally {
                cursorQuery.close();
            }
        }
    }

    private static void printSensitiveDebugLog(String str, String str2) {
        if (piLoggable) {
            Log.d(str, str2);
        }
    }

    private static void printSensitiveVerboseLog(String str, String str2) {
        if (piLoggable) {
            Log.d(str, str2);
        }
    }
}
