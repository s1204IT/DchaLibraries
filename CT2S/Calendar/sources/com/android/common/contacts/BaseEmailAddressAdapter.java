package com.android.common.contacts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.text.util.Rfc822Token;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import com.android.common.widget.CompositeCursorAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseEmailAddressAdapter extends CompositeCursorAdapter implements Filterable {
    private Account mAccount;
    protected final ContentResolver mContentResolver;
    private boolean mDirectoriesLoaded;
    private Handler mHandler;
    private int mPreferredMaxResultCount;

    private static class DirectoryListQuery {
        public static final Uri URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "directories");
        public static final String[] PROJECTION = {"_id", "accountName", "accountType", "displayName", "packageName", "typeResourceId"};
    }

    private static class EmailQuery {
        public static final String[] PROJECTION = {"display_name", "data1"};
    }

    protected abstract void bindView(View view, String str, String str2, String str3, String str4);

    protected abstract void bindViewLoading(View view, String str, String str2);

    protected abstract View inflateItemView(ViewGroup viewGroup);

    protected abstract View inflateItemViewLoading(ViewGroup viewGroup);

    public static final class DirectoryPartition extends CompositeCursorAdapter.Partition {
        public String accountName;
        public String accountType;
        public CharSequence constraint;
        public long directoryId;
        public String directoryType;
        public String displayName;
        public DirectoryPartitionFilter filter;
        public boolean loading;

        public DirectoryPartition() {
            super(false, false);
        }
    }

    private final class DefaultPartitionFilter extends Filter {
        private DefaultPartitionFilter() {
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence constraint) {
            Cursor directoryCursor = null;
            if (!BaseEmailAddressAdapter.this.mDirectoriesLoaded) {
                directoryCursor = BaseEmailAddressAdapter.this.mContentResolver.query(DirectoryListQuery.URI, DirectoryListQuery.PROJECTION, null, null, null);
                BaseEmailAddressAdapter.this.mDirectoriesLoaded = true;
            }
            Filter.FilterResults results = new Filter.FilterResults();
            Cursor cursor = null;
            if (!TextUtils.isEmpty(constraint)) {
                Uri.Builder builder = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(constraint.toString()).appendQueryParameter("limit", String.valueOf(BaseEmailAddressAdapter.this.mPreferredMaxResultCount));
                if (BaseEmailAddressAdapter.this.mAccount != null) {
                    builder.appendQueryParameter("name_for_primary_account", BaseEmailAddressAdapter.this.mAccount.name);
                    builder.appendQueryParameter("type_for_primary_account", BaseEmailAddressAdapter.this.mAccount.type);
                }
                Uri uri = builder.build();
                cursor = BaseEmailAddressAdapter.this.mContentResolver.query(uri, EmailQuery.PROJECTION, null, null, null);
                results.count = cursor.getCount();
            }
            results.values = new Cursor[]{directoryCursor, cursor};
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            if (results.values != null) {
                Cursor[] cursors = (Cursor[]) results.values;
                BaseEmailAddressAdapter.this.onDirectoryLoadFinished(constraint, cursors[0], cursors[1]);
            }
            results.count = BaseEmailAddressAdapter.this.getCount();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            return BaseEmailAddressAdapter.this.makeDisplayString((Cursor) resultValue);
        }
    }

    private final class DirectoryPartitionFilter extends Filter {
        private final long mDirectoryId;
        private int mLimit;
        private final int mPartitionIndex;

        public DirectoryPartitionFilter(int partitionIndex, long directoryId) {
            this.mPartitionIndex = partitionIndex;
            this.mDirectoryId = directoryId;
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
            if (!TextUtils.isEmpty(constraint)) {
                Uri uri = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(constraint.toString()).appendQueryParameter("directory", String.valueOf(this.mDirectoryId)).appendQueryParameter("limit", String.valueOf(getLimit() + 5)).build();
                Cursor cursor = BaseEmailAddressAdapter.this.mContentResolver.query(uri, EmailQuery.PROJECTION, null, null, null);
                results.values = cursor;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, Filter.FilterResults results) {
            Cursor cursor = (Cursor) results.values;
            BaseEmailAddressAdapter.this.onPartitionLoadFinished(constraint, this.mPartitionIndex, cursor);
            results.count = BaseEmailAddressAdapter.this.getCount();
        }
    }

    public BaseEmailAddressAdapter(Context context) {
        this(context, 10);
    }

    public BaseEmailAddressAdapter(Context context, int preferredMaxResultCount) {
        super(context);
        this.mContentResolver = context.getContentResolver();
        this.mPreferredMaxResultCount = preferredMaxResultCount;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                BaseEmailAddressAdapter.this.showSearchPendingIfNotComplete(msg.arg1);
            }
        };
    }

    @Override
    protected int getItemViewType(int partitionIndex, int position) {
        DirectoryPartition partition = (DirectoryPartition) getPartition(partitionIndex);
        return partition.loading ? 1 : 0;
    }

    @Override
    protected View newView(Context context, int partitionIndex, Cursor cursor, int position, ViewGroup parent) {
        DirectoryPartition partition = (DirectoryPartition) getPartition(partitionIndex);
        return partition.loading ? inflateItemViewLoading(parent) : inflateItemView(parent);
    }

    @Override
    protected void bindView(View v, int partition, Cursor cursor, int position) {
        DirectoryPartition directoryPartition = (DirectoryPartition) getPartition(partition);
        String directoryType = directoryPartition.directoryType;
        String directoryName = directoryPartition.displayName;
        if (directoryPartition.loading) {
            bindViewLoading(v, directoryType, directoryName);
            return;
        }
        String displayName = cursor.getString(0);
        String emailAddress = cursor.getString(1);
        if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, emailAddress)) {
            displayName = emailAddress;
            emailAddress = null;
        }
        bindView(v, directoryType, directoryName, displayName, emailAddress);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    protected boolean isEnabled(int partitionIndex, int position) {
        return !isLoading(partitionIndex);
    }

    private boolean isLoading(int partitionIndex) {
        return ((DirectoryPartition) getPartition(partitionIndex)).loading;
    }

    @Override
    public Filter getFilter() {
        return new DefaultPartitionFilter();
    }

    protected void onDirectoryLoadFinished(CharSequence constraint, Cursor directoryCursor, Cursor defaultPartitionCursor) {
        if (directoryCursor != null) {
            PackageManager packageManager = getContext().getPackageManager();
            DirectoryPartition preferredDirectory = null;
            List<DirectoryPartition> directories = new ArrayList<>();
            while (directoryCursor.moveToNext()) {
                long id = directoryCursor.getLong(0);
                if (id != 1) {
                    DirectoryPartition partition = new DirectoryPartition();
                    partition.directoryId = id;
                    partition.displayName = directoryCursor.getString(3);
                    partition.accountName = directoryCursor.getString(1);
                    partition.accountType = directoryCursor.getString(2);
                    String packageName = directoryCursor.getString(4);
                    int resourceId = directoryCursor.getInt(5);
                    if (packageName != null && resourceId != 0) {
                        try {
                            Resources resources = packageManager.getResourcesForApplication(packageName);
                            partition.directoryType = resources.getString(resourceId);
                            if (partition.directoryType == null) {
                                Log.e("BaseEmailAddressAdapter", "Cannot resolve directory name: " + resourceId + "@" + packageName);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e("BaseEmailAddressAdapter", "Cannot resolve directory name: " + resourceId + "@" + packageName, e);
                        }
                    }
                    if (this.mAccount != null && this.mAccount.name.equals(partition.accountName) && this.mAccount.type.equals(partition.accountType)) {
                        preferredDirectory = partition;
                    } else {
                        directories.add(partition);
                    }
                }
            }
            if (preferredDirectory != null) {
                directories.add(1, preferredDirectory);
            }
            Iterator<DirectoryPartition> it = directories.iterator();
            while (it.hasNext()) {
                addPartition(it.next());
            }
        }
        int count = getPartitionCount();
        setNotificationsEnabled(false);
        if (defaultPartitionCursor != null) {
            try {
                if (getPartitionCount() > 0) {
                    changeCursor(0, defaultPartitionCursor);
                }
            } catch (Throwable th) {
                setNotificationsEnabled(true);
                throw th;
            }
        }
        int defaultPartitionCount = defaultPartitionCursor == null ? 0 : defaultPartitionCursor.getCount();
        int limit = this.mPreferredMaxResultCount - defaultPartitionCount;
        for (int i = 1; i < count; i++) {
            DirectoryPartition partition2 = (DirectoryPartition) getPartition(i);
            partition2.constraint = constraint;
            if (limit > 0) {
                if (!partition2.loading) {
                    partition2.loading = true;
                    changeCursor(i, null);
                }
            } else {
                partition2.loading = false;
                changeCursor(i, null);
            }
        }
        setNotificationsEnabled(true);
        for (int i2 = 1; i2 < count; i2++) {
            DirectoryPartition partition3 = (DirectoryPartition) getPartition(i2);
            if (partition3.loading) {
                this.mHandler.removeMessages(1, partition3);
                Message msg = this.mHandler.obtainMessage(1, i2, 0, partition3);
                this.mHandler.sendMessageDelayed(msg, 1000L);
                if (partition3.filter == null) {
                    partition3.filter = new DirectoryPartitionFilter(i2, partition3.directoryId);
                }
                partition3.filter.setLimit(limit);
                partition3.filter.filter(constraint);
            } else if (partition3.filter != null) {
                partition3.filter.filter(null);
            }
        }
    }

    void showSearchPendingIfNotComplete(int partitionIndex) {
        if (partitionIndex < getPartitionCount()) {
            DirectoryPartition partition = (DirectoryPartition) getPartition(partitionIndex);
            if (partition.loading) {
                changeCursor(partitionIndex, createLoadingCursor());
            }
        }
    }

    private Cursor createLoadingCursor() {
        MatrixCursor cursor = new MatrixCursor(new String[]{"searching"});
        cursor.addRow(new Object[]{""});
        return cursor;
    }

    public void onPartitionLoadFinished(CharSequence constraint, int partitionIndex, Cursor cursor) {
        if (partitionIndex < getPartitionCount()) {
            DirectoryPartition partition = (DirectoryPartition) getPartition(partitionIndex);
            if (partition.loading && TextUtils.equals(constraint, partition.constraint)) {
                partition.loading = false;
                this.mHandler.removeMessages(1, partition);
                changeCursor(partitionIndex, removeDuplicatesAndTruncate(partitionIndex, cursor));
                return;
            } else {
                if (cursor != null) {
                    cursor.close();
                    return;
                }
                return;
            }
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private Cursor removeDuplicatesAndTruncate(int partition, Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() > 10 || hasDuplicates(cursor, partition)) {
            int count = 0;
            MatrixCursor newCursor = new MatrixCursor(EmailQuery.PROJECTION);
            cursor.moveToPosition(-1);
            while (cursor.moveToNext() && count < 10) {
                String displayName = cursor.getString(0);
                String emailAddress = cursor.getString(1);
                if (!isDuplicate(emailAddress, partition)) {
                    newCursor.addRow(new Object[]{displayName, emailAddress});
                    count++;
                }
            }
            cursor.close();
            return newCursor;
        }
        return cursor;
    }

    private boolean hasDuplicates(Cursor cursor, int partition) {
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String emailAddress = cursor.getString(1);
            if (isDuplicate(emailAddress, partition)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicate(String emailAddress, int excludePartition) {
        Cursor cursor;
        int partitionCount = getPartitionCount();
        for (int partition = 0; partition < partitionCount; partition++) {
            if (partition != excludePartition && !isLoading(partition) && (cursor = getCursor(partition)) != null) {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    String address = cursor.getString(1);
                    if (TextUtils.equals(emailAddress, address)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private final String makeDisplayString(Cursor cursor) {
        if (cursor.getColumnName(0).equals("searching")) {
            return "";
        }
        String displayName = cursor.getString(0);
        String emailAddress = cursor.getString(1);
        return (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, emailAddress)) ? emailAddress : new Rfc822Token(displayName, emailAddress, null).toString();
    }
}
