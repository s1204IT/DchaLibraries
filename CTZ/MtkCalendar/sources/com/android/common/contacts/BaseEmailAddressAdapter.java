package com.android.common.contacts;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
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
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Cursor cursorQuery;
            Cursor cursorQuery2 = null;
            if (!BaseEmailAddressAdapter.this.mDirectoriesLoaded) {
                cursorQuery = BaseEmailAddressAdapter.this.mContentResolver.query(DirectoryListQuery.URI, DirectoryListQuery.PROJECTION, null, null, null);
                BaseEmailAddressAdapter.this.mDirectoriesLoaded = true;
            } else {
                cursorQuery = null;
            }
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (!TextUtils.isEmpty(charSequence)) {
                Uri.Builder builderAppendQueryParameter = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(charSequence.toString()).appendQueryParameter("limit", String.valueOf(BaseEmailAddressAdapter.this.mPreferredMaxResultCount));
                if (BaseEmailAddressAdapter.this.mAccount != null) {
                    builderAppendQueryParameter.appendQueryParameter("name_for_primary_account", BaseEmailAddressAdapter.this.mAccount.name);
                    builderAppendQueryParameter.appendQueryParameter("type_for_primary_account", BaseEmailAddressAdapter.this.mAccount.type);
                }
                cursorQuery2 = BaseEmailAddressAdapter.this.mContentResolver.query(builderAppendQueryParameter.build(), EmailQuery.PROJECTION, null, null, null);
                filterResults.count = cursorQuery2.getCount();
            }
            filterResults.values = new Cursor[]{cursorQuery, cursorQuery2};
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            if (filterResults.values != null) {
                Cursor[] cursorArr = (Cursor[]) filterResults.values;
                BaseEmailAddressAdapter.this.onDirectoryLoadFinished(charSequence, cursorArr[0], cursorArr[1]);
            }
            filterResults.count = BaseEmailAddressAdapter.this.getCount();
        }

        @Override
        public CharSequence convertResultToString(Object obj) {
            return BaseEmailAddressAdapter.this.makeDisplayString((Cursor) obj);
        }
    }

    private final class DirectoryPartitionFilter extends Filter {
        private final long mDirectoryId;
        private int mLimit;
        private final int mPartitionIndex;

        public DirectoryPartitionFilter(int i, long j) {
            this.mPartitionIndex = i;
            this.mDirectoryId = j;
        }

        public synchronized void setLimit(int i) {
            this.mLimit = i;
        }

        public synchronized int getLimit() {
            return this.mLimit;
        }

        @Override
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (!TextUtils.isEmpty(charSequence)) {
                filterResults.values = BaseEmailAddressAdapter.this.mContentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(charSequence.toString()).appendQueryParameter("directory", String.valueOf(this.mDirectoryId)).appendQueryParameter("limit", String.valueOf(getLimit() + 5)).build(), EmailQuery.PROJECTION, null, null, null);
            }
            return filterResults;
        }

        @Override
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            BaseEmailAddressAdapter.this.onPartitionLoadFinished(charSequence, this.mPartitionIndex, (Cursor) filterResults.values);
            filterResults.count = BaseEmailAddressAdapter.this.getCount();
        }
    }

    public BaseEmailAddressAdapter(Context context) {
        this(context, 10);
    }

    public BaseEmailAddressAdapter(Context context, int i) {
        super(context);
        this.mContentResolver = context.getContentResolver();
        this.mPreferredMaxResultCount = i;
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                BaseEmailAddressAdapter.this.showSearchPendingIfNotComplete(message.arg1);
            }
        };
    }

    @Override
    protected int getItemViewType(int i, int i2) {
        return ((DirectoryPartition) getPartition(i)).loading ? 1 : 0;
    }

    @Override
    protected View newView(Context context, int i, Cursor cursor, int i2, ViewGroup viewGroup) {
        if (((DirectoryPartition) getPartition(i)).loading) {
            return inflateItemViewLoading(viewGroup);
        }
        return inflateItemView(viewGroup);
    }

    @Override
    protected void bindView(View view, int i, Cursor cursor, int i2) {
        String str;
        String str2;
        DirectoryPartition directoryPartition = (DirectoryPartition) getPartition(i);
        String str3 = directoryPartition.directoryType;
        String str4 = directoryPartition.displayName;
        if (directoryPartition.loading) {
            bindViewLoading(view, str3, str4);
            return;
        }
        String string = cursor.getString(0);
        String string2 = cursor.getString(1);
        if (TextUtils.isEmpty(string) || TextUtils.equals(string, string2)) {
            str = null;
            str2 = string2;
        } else {
            str2 = string;
            str = string2;
        }
        bindView(view, str3, str4, str2, str);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    protected boolean isEnabled(int i, int i2) {
        return !isLoading(i);
    }

    private boolean isLoading(int i) {
        return ((DirectoryPartition) getPartition(i)).loading;
    }

    @Override
    public Filter getFilter() {
        return new DefaultPartitionFilter();
    }

    protected void onDirectoryLoadFinished(CharSequence charSequence, Cursor cursor, Cursor cursor2) {
        int count;
        if (cursor != null) {
            PackageManager packageManager = getContext().getPackageManager();
            ArrayList arrayList = new ArrayList();
            DirectoryPartition directoryPartition = null;
            while (cursor.moveToNext()) {
                long j = cursor.getLong(0);
                if (j != 1) {
                    DirectoryPartition directoryPartition2 = new DirectoryPartition();
                    directoryPartition2.directoryId = j;
                    directoryPartition2.displayName = cursor.getString(3);
                    directoryPartition2.accountName = cursor.getString(1);
                    directoryPartition2.accountType = cursor.getString(2);
                    String string = cursor.getString(4);
                    int i = cursor.getInt(5);
                    if (string != null && i != 0) {
                        try {
                            directoryPartition2.directoryType = packageManager.getResourcesForApplication(string).getString(i);
                            if (directoryPartition2.directoryType == null) {
                                Log.e("BaseEmailAddressAdapter", "Cannot resolve directory name: " + i + "@" + string);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e("BaseEmailAddressAdapter", "Cannot resolve directory name: " + i + "@" + string, e);
                        }
                    }
                    if (this.mAccount == null || !this.mAccount.name.equals(directoryPartition2.accountName) || !this.mAccount.type.equals(directoryPartition2.accountType)) {
                        arrayList.add(directoryPartition2);
                    } else {
                        directoryPartition = directoryPartition2;
                    }
                }
            }
            if (directoryPartition != null) {
                arrayList.add(1, directoryPartition);
            }
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                addPartition((DirectoryPartition) it.next());
            }
        }
        int partitionCount = getPartitionCount();
        setNotificationsEnabled(false);
        if (cursor2 != null) {
            try {
                if (getPartitionCount() > 0) {
                    changeCursor(0, cursor2);
                }
            } catch (Throwable th) {
                setNotificationsEnabled(true);
                throw th;
            }
        }
        if (cursor2 != null) {
            count = cursor2.getCount();
        } else {
            count = 0;
        }
        int i2 = this.mPreferredMaxResultCount - count;
        for (int i3 = 1; i3 < partitionCount; i3++) {
            DirectoryPartition directoryPartition3 = (DirectoryPartition) getPartition(i3);
            directoryPartition3.constraint = charSequence;
            if (i2 > 0) {
                if (!directoryPartition3.loading) {
                    directoryPartition3.loading = true;
                    changeCursor(i3, null);
                }
            } else {
                directoryPartition3.loading = false;
                changeCursor(i3, null);
            }
        }
        setNotificationsEnabled(true);
        for (int i4 = 1; i4 < partitionCount; i4++) {
            DirectoryPartition directoryPartition4 = (DirectoryPartition) getPartition(i4);
            if (directoryPartition4.loading) {
                this.mHandler.removeMessages(1, directoryPartition4);
                this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, i4, 0, directoryPartition4), 1000L);
                if (directoryPartition4.filter == null) {
                    directoryPartition4.filter = new DirectoryPartitionFilter(i4, directoryPartition4.directoryId);
                }
                directoryPartition4.filter.setLimit(i2);
                directoryPartition4.filter.filter(charSequence);
            } else {
                if (directoryPartition4.filter != null) {
                    directoryPartition4.filter.filter(null);
                }
            }
        }
    }

    void showSearchPendingIfNotComplete(int i) {
        if (i < getPartitionCount() && ((DirectoryPartition) getPartition(i)).loading) {
            changeCursor(i, createLoadingCursor());
        }
    }

    private Cursor createLoadingCursor() {
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{"searching"});
        matrixCursor.addRow(new Object[]{""});
        return matrixCursor;
    }

    public void onPartitionLoadFinished(CharSequence charSequence, int i, Cursor cursor) {
        if (i >= getPartitionCount()) {
            if (cursor != null) {
                cursor.close();
                return;
            }
            return;
        }
        DirectoryPartition directoryPartition = (DirectoryPartition) getPartition(i);
        if (directoryPartition.loading && TextUtils.equals(charSequence, directoryPartition.constraint)) {
            directoryPartition.loading = false;
            this.mHandler.removeMessages(1, directoryPartition);
            changeCursor(i, removeDuplicatesAndTruncate(i, cursor));
        } else if (cursor != null) {
            cursor.close();
        }
    }

    private Cursor removeDuplicatesAndTruncate(int i, Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() <= 10 && !hasDuplicates(cursor, i)) {
            return cursor;
        }
        MatrixCursor matrixCursor = new MatrixCursor(EmailQuery.PROJECTION);
        cursor.moveToPosition(-1);
        int i2 = 0;
        while (cursor.moveToNext() && i2 < 10) {
            String string = cursor.getString(0);
            String string2 = cursor.getString(1);
            if (!isDuplicate(string2, i)) {
                matrixCursor.addRow(new Object[]{string, string2});
                i2++;
            }
        }
        cursor.close();
        return matrixCursor;
    }

    private boolean hasDuplicates(Cursor cursor, int i) {
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            if (isDuplicate(cursor.getString(1), i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDuplicate(String str, int i) {
        Cursor cursor;
        int partitionCount = getPartitionCount();
        for (int i2 = 0; i2 < partitionCount; i2++) {
            if (i2 != i && !isLoading(i2) && (cursor = getCursor(i2)) != null) {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    if (TextUtils.equals(str, cursor.getString(1))) {
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
        String string = cursor.getString(0);
        String string2 = cursor.getString(1);
        if (TextUtils.isEmpty(string) || TextUtils.equals(string, string2)) {
            return string2;
        }
        return new Rfc822Token(string, string2, null).toString();
    }
}
