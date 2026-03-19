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
import com.android.common.speech.LoggingEvents;
import com.android.common.widget.CompositeCursorAdapter;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class BaseEmailAddressAdapter extends CompositeCursorAdapter implements Filterable {
    private static final int ALLOWANCE_FOR_DUPLICATES = 5;
    private static final int DEFAULT_PREFERRED_MAX_RESULT_COUNT = 10;
    private static final long DIRECTORY_LOCAL_INVISIBLE = 1;
    private static final String DIRECTORY_PARAM_KEY = "directory";
    private static final String LIMIT_PARAM_KEY = "limit";
    private static final int MESSAGE_SEARCH_PENDING = 1;
    private static final int MESSAGE_SEARCH_PENDING_DELAY = 1000;
    private static final String PRIMARY_ACCOUNT_NAME = "name_for_primary_account";
    private static final String PRIMARY_ACCOUNT_TYPE = "type_for_primary_account";
    private static final String SEARCHING_CURSOR_MARKER = "searching";
    private static final String TAG = "BaseEmailAddressAdapter";
    private Account mAccount;
    protected final ContentResolver mContentResolver;
    private boolean mDirectoriesLoaded;
    private Handler mHandler;
    private int mPreferredMaxResultCount;

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

    private static class EmailQuery {
        public static final int ADDRESS = 1;
        public static final int NAME = 0;
        public static final String[] PROJECTION = {"display_name", "data1"};

        private EmailQuery() {
        }
    }

    private static class DirectoryListQuery {
        public static final int ACCOUNT_NAME = 1;
        public static final int ACCOUNT_TYPE = 2;
        public static final int DISPLAY_NAME = 3;
        public static final int ID = 0;
        public static final int PACKAGE_NAME = 4;
        public static final int TYPE_RESOURCE_ID = 5;
        public static final Uri URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "directories");
        private static final String DIRECTORY_ID = "_id";
        private static final String DIRECTORY_ACCOUNT_NAME = "accountName";
        private static final String DIRECTORY_ACCOUNT_TYPE = "accountType";
        private static final String DIRECTORY_DISPLAY_NAME = "displayName";
        private static final String DIRECTORY_PACKAGE_NAME = "packageName";
        private static final String DIRECTORY_TYPE_RESOURCE_ID = "typeResourceId";
        public static final String[] PROJECTION = {DIRECTORY_ID, DIRECTORY_ACCOUNT_NAME, DIRECTORY_ACCOUNT_TYPE, DIRECTORY_DISPLAY_NAME, DIRECTORY_PACKAGE_NAME, DIRECTORY_TYPE_RESOURCE_ID};

        private DirectoryListQuery() {
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
                Uri.Builder builderAppendQueryParameter = ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(charSequence.toString()).appendQueryParameter(BaseEmailAddressAdapter.LIMIT_PARAM_KEY, String.valueOf(BaseEmailAddressAdapter.this.mPreferredMaxResultCount));
                if (BaseEmailAddressAdapter.this.mAccount != null) {
                    builderAppendQueryParameter.appendQueryParameter(BaseEmailAddressAdapter.PRIMARY_ACCOUNT_NAME, BaseEmailAddressAdapter.this.mAccount.name);
                    builderAppendQueryParameter.appendQueryParameter(BaseEmailAddressAdapter.PRIMARY_ACCOUNT_TYPE, BaseEmailAddressAdapter.this.mAccount.type);
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
                filterResults.values = BaseEmailAddressAdapter.this.mContentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI.buildUpon().appendPath(charSequence.toString()).appendQueryParameter(BaseEmailAddressAdapter.DIRECTORY_PARAM_KEY, String.valueOf(this.mDirectoryId)).appendQueryParameter(BaseEmailAddressAdapter.LIMIT_PARAM_KEY, String.valueOf(getLimit() + 5)).build(), EmailQuery.PROJECTION, null, null, null);
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

    public void setAccount(Account account) {
        this.mAccount = account;
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
                if (j != DIRECTORY_LOCAL_INVISIBLE) {
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
                                Log.e(TAG, "Cannot resolve directory name: " + i + "@" + string);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(TAG, "Cannot resolve directory name: " + i + "@" + string, e);
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
        MatrixCursor matrixCursor = new MatrixCursor(new String[]{SEARCHING_CURSOR_MARKER});
        matrixCursor.addRow(new Object[]{LoggingEvents.EXTRA_CALLING_APP_NAME});
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
        if (cursor.getColumnName(0).equals(SEARCHING_CURSOR_MARKER)) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        String string = cursor.getString(0);
        String string2 = cursor.getString(1);
        if (TextUtils.isEmpty(string) || TextUtils.equals(string, string2)) {
            return string2;
        }
        return new Rfc822Token(string, string2, null).toString();
    }
}
