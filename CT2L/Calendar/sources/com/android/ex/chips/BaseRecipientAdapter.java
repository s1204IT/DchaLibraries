package com.android.ex.chips;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import com.android.ex.chips.DropdownChipLayouter;
import com.android.ex.chips.PhotoManager;
import com.android.ex.chips.Queries;
import com.android.ex.chips.RecipientAlternatesAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseRecipientAdapter extends BaseAdapter implements Filterable, AccountSpecifier, PhotoManager.PhotoManagerCallback {
    private Account mAccount;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    protected CharSequence mCurrentConstraint;
    private final DelayedMessageHandler mDelayedMessageHandler;
    private DropdownChipLayouter mDropdownChipLayouter;
    private List<RecipientEntry> mEntries;
    private EntriesUpdatedObserver mEntriesUpdatedObserver;
    private LinkedHashMap<Long, List<RecipientEntry>> mEntryMap;
    private Set<String> mExistingDestinations;
    private List<RecipientEntry> mNonAggregatedEntries;
    private PhotoManager mPhotoManager;
    protected final int mPreferredMaxResultCount;
    private final Queries.Query mQueryMode;
    private final int mQueryType;
    private int mRemainingDirectoryCount;
    private List<RecipientEntry> mTempEntries;

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

    public interface EntriesUpdatedObserver {
        void onChanged(List<RecipientEntry> list);
    }

    static int access$710(BaseRecipientAdapter x0) {
        int i = x0.mRemainingDirectoryCount;
        x0.mRemainingDirectoryCount = i - 1;
        return i;
    }

    protected static class TemporaryEntry {
        public final long contactId;
        public final long dataId;
        public final String destination;
        public final String destinationLabel;
        public final int destinationType;
        public final Long directoryId;
        public final String displayName;
        public final int displayNameSource;
        public final String lookupKey;
        public final String thumbnailUriString;

        public TemporaryEntry(Cursor cursor, Long directoryId) {
            this.displayName = cursor.getString(0);
            this.destination = cursor.getString(1);
            this.destinationType = cursor.getInt(2);
            this.destinationLabel = cursor.getString(3);
            this.contactId = cursor.getLong(4);
            this.directoryId = directoryId;
            this.dataId = cursor.getLong(5);
            this.thumbnailUriString = cursor.getString(6);
            this.displayNameSource = cursor.getInt(7);
            this.lookupKey = cursor.getString(8);
        }
    }

    private static class DefaultFilterResult {
        public final List<RecipientEntry> entries;
        public final LinkedHashMap<Long, List<RecipientEntry>> entryMap;
        public final Set<String> existingDestinations;
        public final List<RecipientEntry> nonAggregatedEntries;
        public final List<DirectorySearchParams> paramsList;

        public DefaultFilterResult(List<RecipientEntry> entries, LinkedHashMap<Long, List<RecipientEntry>> entryMap, List<RecipientEntry> nonAggregatedEntries, Set<String> existingDestinations, List<DirectorySearchParams> paramsList) {
            this.entries = entries;
            this.entryMap = entryMap;
            this.nonAggregatedEntries = nonAggregatedEntries;
            this.existingDestinations = existingDestinations;
            this.paramsList = paramsList;
        }
    }

    private final class DefaultFilter extends Filter {
        private DefaultFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Filter.FilterResults results = new Filter.FilterResults();
            Cursor defaultDirectoryCursor = null;
            Cursor directoryCursor = null;
            if (!TextUtils.isEmpty(constraint)) {
                try {
                    Cursor defaultDirectoryCursor2 = BaseRecipientAdapter.this.doQuery(constraint, BaseRecipientAdapter.this.mPreferredMaxResultCount, null);
                    if (defaultDirectoryCursor2 != null) {
                        LinkedHashMap<Long, List<RecipientEntry>> entryMap = new LinkedHashMap<>();
                        List<RecipientEntry> nonAggregatedEntries = new ArrayList<>();
                        Set<String> existingDestinations = new HashSet<>();
                        while (defaultDirectoryCursor2.moveToNext()) {
                            BaseRecipientAdapter.putOneEntry(new TemporaryEntry(defaultDirectoryCursor2, null), true, entryMap, nonAggregatedEntries, existingDestinations);
                        }
                        List<RecipientEntry> entries = BaseRecipientAdapter.this.constructEntryList(entryMap, nonAggregatedEntries);
                        List<DirectorySearchParams> paramsList = BaseRecipientAdapter.this.searchOtherDirectories(existingDestinations);
                        results.values = new DefaultFilterResult(entries, entryMap, nonAggregatedEntries, existingDestinations, paramsList);
                        results.count = 1;
                    }
                    if (defaultDirectoryCursor2 != null) {
                        defaultDirectoryCursor2.close();
                    }
                    if (0 != 0) {
                        directoryCursor.close();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        defaultDirectoryCursor.close();
                    }
                    if (0 != 0) {
                        directoryCursor.close();
                    }
                    throw th;
                }
            } else {
                BaseRecipientAdapter.this.clearTempEntries();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            BaseRecipientAdapter.this.mCurrentConstraint = constraint;
            BaseRecipientAdapter.this.clearTempEntries();
            if (results.values != null) {
                DefaultFilterResult defaultFilterResult = (DefaultFilterResult) results.values;
                BaseRecipientAdapter.this.mEntryMap = defaultFilterResult.entryMap;
                BaseRecipientAdapter.this.mNonAggregatedEntries = defaultFilterResult.nonAggregatedEntries;
                BaseRecipientAdapter.this.mExistingDestinations = defaultFilterResult.existingDestinations;
                if (defaultFilterResult.entries.size() == 0 && defaultFilterResult.paramsList != null) {
                    BaseRecipientAdapter.this.cacheCurrentEntries();
                }
                BaseRecipientAdapter.this.updateEntries(defaultFilterResult.entries);
                if (defaultFilterResult.paramsList != null) {
                    int limit = BaseRecipientAdapter.this.mPreferredMaxResultCount - defaultFilterResult.existingDestinations.size();
                    BaseRecipientAdapter.this.startSearchOtherDirectories(constraint, defaultFilterResult.paramsList, limit);
                    return;
                }
                return;
            }
            BaseRecipientAdapter.this.updateEntries(Collections.emptyList());
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            RecipientEntry entry = (RecipientEntry) resultValue;
            String displayName = entry.getDisplayName();
            String emailAddress = entry.getDestination();
            return (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, emailAddress)) ? emailAddress : new Rfc822Token(displayName, emailAddress, null).toString();
        }
    }

    protected List<DirectorySearchParams> searchOtherDirectories(Set<String> existingDestinations) {
        List<DirectorySearchParams> list = null;
        int limit = this.mPreferredMaxResultCount - existingDestinations.size();
        if (limit > 0) {
            Cursor directoryCursor = null;
            try {
                directoryCursor = this.mContentResolver.query(DirectoryListQuery.URI, DirectoryListQuery.PROJECTION, null, null, null);
                list = setupOtherDirectories(this.mContext, directoryCursor, this.mAccount);
            } finally {
                if (directoryCursor != null && !directoryCursor.isClosed()) {
                    directoryCursor.close();
                }
            }
        }
        return list;
    }

    protected class DirectoryFilter extends Filter {
        private int mLimit;
        private final DirectorySearchParams mParams;

        public DirectoryFilter(DirectorySearchParams params) {
            this.mParams = params;
        }

        public synchronized void setLimit(int limit) {
            this.mLimit = limit;
        }

        public synchronized int getLimit() {
            return this.mLimit;
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Filter.FilterResults results = new Filter.FilterResults();
            results.values = null;
            results.count = 0;
            if (!TextUtils.isEmpty(constraint)) {
                ArrayList<TemporaryEntry> tempEntries = new ArrayList<>();
                Cursor cursor = null;
                try {
                    cursor = BaseRecipientAdapter.this.doQuery(constraint, getLimit(), Long.valueOf(this.mParams.directoryId));
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            tempEntries.add(new TemporaryEntry(cursor, Long.valueOf(this.mParams.directoryId)));
                        }
                    }
                    if (!tempEntries.isEmpty()) {
                        results.values = tempEntries;
                        results.count = 1;
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            BaseRecipientAdapter.this.mDelayedMessageHandler.removeDelayedLoadMessage();
            if (TextUtils.equals(constraint, BaseRecipientAdapter.this.mCurrentConstraint)) {
                if (results.count > 0) {
                    ArrayList<TemporaryEntry> tempEntries = (ArrayList) results.values;
                    for (TemporaryEntry tempEntry : tempEntries) {
                        BaseRecipientAdapter.this.putOneEntry(tempEntry, this.mParams.directoryId == 0);
                    }
                }
                BaseRecipientAdapter.access$710(BaseRecipientAdapter.this);
                if (BaseRecipientAdapter.this.mRemainingDirectoryCount > 0) {
                    BaseRecipientAdapter.this.mDelayedMessageHandler.sendDelayedLoadMessage();
                }
                if (results.count > 0 || BaseRecipientAdapter.this.mRemainingDirectoryCount == 0) {
                    BaseRecipientAdapter.this.clearTempEntries();
                }
            }
            BaseRecipientAdapter.this.updateEntries(BaseRecipientAdapter.this.constructEntryList());
        }
    }

    private final class DelayedMessageHandler extends Handler {
        private DelayedMessageHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (BaseRecipientAdapter.this.mRemainingDirectoryCount > 0) {
                BaseRecipientAdapter.this.updateEntries(BaseRecipientAdapter.this.constructEntryList());
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
        this(context, 10, 0);
    }

    public BaseRecipientAdapter(Context context, int preferredMaxResultCount, int queryMode) {
        this.mDelayedMessageHandler = new DelayedMessageHandler();
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mPreferredMaxResultCount = preferredMaxResultCount;
        this.mPhotoManager = new DefaultPhotoManager(this.mContentResolver);
        this.mQueryType = queryMode;
        if (queryMode == 0) {
            this.mQueryMode = Queries.EMAIL;
        } else if (queryMode == 1) {
            this.mQueryMode = Queries.PHONE;
        } else {
            this.mQueryMode = Queries.EMAIL;
            Log.e("BaseRecipientAdapter", "Unsupported query type: " + queryMode);
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public int getQueryType() {
        return this.mQueryType;
    }

    public void setDropdownChipLayouter(DropdownChipLayouter dropdownChipLayouter) {
        this.mDropdownChipLayouter = dropdownChipLayouter;
        this.mDropdownChipLayouter.setQuery(this.mQueryMode);
    }

    public boolean forceShowAddress() {
        return false;
    }

    public void getMatchingRecipients(ArrayList<String> inAddresses, RecipientAlternatesAdapter.RecipientMatchCallback callback) {
        RecipientAlternatesAdapter.getMatchingRecipients(getContext(), this, inAddresses, getAccount(), callback);
    }

    @Override
    public Filter getFilter() {
        return new DefaultFilter();
    }

    public Map<String, RecipientEntry> getMatchingRecipients(Set<String> addresses) {
        return null;
    }

    public static List<DirectorySearchParams> setupOtherDirectories(Context context, Cursor directoryCursor, Account account) {
        PackageManager packageManager = context.getPackageManager();
        List<DirectorySearchParams> paramsList = new ArrayList<>();
        DirectorySearchParams preferredDirectory = null;
        while (directoryCursor.moveToNext()) {
            long id = directoryCursor.getLong(0);
            if (id != 1) {
                DirectorySearchParams params = new DirectorySearchParams();
                String packageName = directoryCursor.getString(4);
                int resourceId = directoryCursor.getInt(5);
                params.directoryId = id;
                params.displayName = directoryCursor.getString(3);
                params.accountName = directoryCursor.getString(1);
                params.accountType = directoryCursor.getString(2);
                if (packageName != null && resourceId != 0) {
                    try {
                        Resources resources = packageManager.getResourcesForApplication(packageName);
                        params.directoryType = resources.getString(resourceId);
                        if (params.directoryType == null) {
                            Log.e("BaseRecipientAdapter", "Cannot resolve directory name: " + resourceId + "@" + packageName);
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e("BaseRecipientAdapter", "Cannot resolve directory name: " + resourceId + "@" + packageName, e);
                    }
                }
                if (account != null && account.name.equals(params.accountName) && account.type.equals(params.accountType)) {
                    preferredDirectory = params;
                } else {
                    paramsList.add(params);
                }
            }
        }
        if (preferredDirectory != null) {
            paramsList.add(1, preferredDirectory);
        }
        return paramsList;
    }

    protected void startSearchOtherDirectories(CharSequence constraint, List<DirectorySearchParams> paramsList, int limit) {
        int count = paramsList.size();
        for (int i = 1; i < count; i++) {
            DirectorySearchParams params = paramsList.get(i);
            params.constraint = constraint;
            if (params.filter == null) {
                params.filter = new DirectoryFilter(params);
            }
            params.filter.setLimit(limit);
            params.filter.filter(constraint);
        }
        this.mRemainingDirectoryCount = count - 1;
        this.mDelayedMessageHandler.sendDelayedLoadMessage();
    }

    protected void putOneEntry(TemporaryEntry entry, boolean isAggregatedEntry) {
        putOneEntry(entry, isAggregatedEntry, this.mEntryMap, this.mNonAggregatedEntries, this.mExistingDestinations);
    }

    private static void putOneEntry(TemporaryEntry entry, boolean isAggregatedEntry, LinkedHashMap<Long, List<RecipientEntry>> entryMap, List<RecipientEntry> nonAggregatedEntries, Set<String> existingDestinations) {
        if (!existingDestinations.contains(entry.destination)) {
            existingDestinations.add(entry.destination);
            if (!isAggregatedEntry) {
                nonAggregatedEntries.add(RecipientEntry.constructTopLevelEntry(entry.displayName, entry.displayNameSource, entry.destination, entry.destinationType, entry.destinationLabel, entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString, true, entry.lookupKey));
            } else {
                if (entryMap.containsKey(Long.valueOf(entry.contactId))) {
                    entryMap.get(Long.valueOf(entry.contactId)).add(RecipientEntry.constructSecondLevelEntry(entry.displayName, entry.displayNameSource, entry.destination, entry.destinationType, entry.destinationLabel, entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString, true, entry.lookupKey));
                    return;
                }
                List<RecipientEntry> entryList = new ArrayList<>();
                entryList.add(RecipientEntry.constructTopLevelEntry(entry.displayName, entry.displayNameSource, entry.destination, entry.destinationType, entry.destinationLabel, entry.contactId, entry.directoryId, entry.dataId, entry.thumbnailUriString, true, entry.lookupKey));
                entryMap.put(Long.valueOf(entry.contactId), entryList);
            }
        }
    }

    protected List<RecipientEntry> constructEntryList() {
        return constructEntryList(this.mEntryMap, this.mNonAggregatedEntries);
    }

    private List<RecipientEntry> constructEntryList(LinkedHashMap<Long, List<RecipientEntry>> entryMap, List<RecipientEntry> nonAggregatedEntries) {
        List<RecipientEntry> entries = new ArrayList<>();
        int validEntryCount = 0;
        for (Map.Entry<Long, List<RecipientEntry>> mapEntry : entryMap.entrySet()) {
            List<RecipientEntry> entryList = mapEntry.getValue();
            int size = entryList.size();
            for (int i = 0; i < size; i++) {
                RecipientEntry entry = entryList.get(i);
                entries.add(entry);
                this.mPhotoManager.populatePhotoBytesAsync(entry, this);
                validEntryCount++;
            }
            if (validEntryCount > this.mPreferredMaxResultCount) {
                break;
            }
        }
        if (validEntryCount <= this.mPreferredMaxResultCount) {
            for (RecipientEntry entry2 : nonAggregatedEntries) {
                if (validEntryCount > this.mPreferredMaxResultCount) {
                    break;
                }
                entries.add(entry2);
                this.mPhotoManager.populatePhotoBytesAsync(entry2, this);
                validEntryCount++;
            }
        }
        return entries;
    }

    public void registerUpdateObserver(EntriesUpdatedObserver observer) {
        this.mEntriesUpdatedObserver = observer;
    }

    protected void updateEntries(List<RecipientEntry> newEntries) {
        this.mEntries = newEntries;
        this.mEntriesUpdatedObserver.onChanged(newEntries);
        notifyDataSetChanged();
    }

    protected void cacheCurrentEntries() {
        this.mTempEntries = this.mEntries;
    }

    protected void clearTempEntries() {
        this.mTempEntries = null;
    }

    protected List<RecipientEntry> getEntries() {
        return this.mTempEntries != null ? this.mTempEntries : this.mEntries;
    }

    protected void fetchPhoto(RecipientEntry entry, PhotoManager.PhotoManagerCallback cb) {
        this.mPhotoManager.populatePhotoBytesAsync(entry, cb);
    }

    private Cursor doQuery(CharSequence constraint, int limit, Long directoryId) {
        Uri.Builder builder = this.mQueryMode.getContentFilterUri().buildUpon().appendPath(constraint.toString()).appendQueryParameter("limit", String.valueOf(limit + 5));
        if (directoryId != null) {
            builder.appendQueryParameter("directory", String.valueOf(directoryId));
        }
        if (this.mAccount != null) {
            builder.appendQueryParameter("name_for_primary_account", this.mAccount.name);
            builder.appendQueryParameter("type_for_primary_account", this.mAccount.type);
        }
        System.currentTimeMillis();
        Cursor cursor = this.mContentResolver.query(builder.build(), this.mQueryMode.getProjection(), null, null, null);
        System.currentTimeMillis();
        return cursor;
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
    public RecipientEntry getItem(int position) {
        return getEntries().get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return getEntries().get(position).getEntryType();
    }

    @Override
    public boolean isEnabled(int position) {
        return getEntries().get(position).isSelectable();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        RecipientEntry entry = getEntries().get(position);
        String constraint = this.mCurrentConstraint == null ? null : this.mCurrentConstraint.toString();
        return this.mDropdownChipLayouter.bindView(convertView, parent, entry, position, DropdownChipLayouter.AdapterType.BASE_RECIPIENT, constraint);
    }

    public Account getAccount() {
        return this.mAccount;
    }

    @Override
    public void onPhotoBytesPopulated() {
    }

    @Override
    public void onPhotoBytesAsynchronouslyPopulated() {
        notifyDataSetChanged();
    }

    @Override
    public void onPhotoBytesAsyncLoadFailed() {
    }
}
