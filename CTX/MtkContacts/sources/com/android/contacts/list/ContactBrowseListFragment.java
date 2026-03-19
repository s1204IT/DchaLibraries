package com.android.contacts.list;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.util.ContactLoaderUtils;
import com.mediatek.contacts.util.Log;
import java.util.List;

public abstract class ContactBrowseListFragment extends MultiSelectContactsListFragment<ContactListAdapter> {
    private ContactLookupTask mContactLookupTask;
    private boolean mDelaySelection;
    private ContactListFilter mFilter;
    private Handler mHandler;
    protected OnContactBrowserActionListener mListener;
    private SharedPreferences mPrefs;
    private boolean mRefreshingContactUri;
    private long mSelectedContactDirectoryId;
    private long mSelectedContactId;
    private String mSelectedContactLookupKey;
    private Uri mSelectedContactUri;
    private boolean mSelectionPersistenceRequested;
    private boolean mSelectionRequired;
    private boolean mSelectionToScreenRequested;
    private boolean mSelectionVerified;
    private boolean mSmoothScrollRequested;
    private boolean mStartedLoading;
    private int mLastSelectedPosition = -1;
    private String mPersistentSelectionPrefix = "defaultContactBrowserSelection";

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    private final class ContactLookupTask extends AsyncTask<Void, Void, Uri> {
        private boolean mIsCancelled;
        private final Uri mUri;

        public ContactLookupTask(Uri uri) {
            this.mUri = uri;
        }

        @Override
        protected Uri doInBackground(Void... voidArr) throws Throwable {
            Throwable th;
            Cursor cursorQuery;
            Cursor cursor = null;
            try {
                try {
                    ContentResolver contentResolver = ContactBrowseListFragment.this.getContext().getContentResolver();
                    cursorQuery = contentResolver.query(ContactLoaderUtils.ensureIsContactUri(contentResolver, this.mUri), new String[]{"_id", "lookup"}, null, null, null);
                    if (cursorQuery != null) {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                long j = cursorQuery.getLong(0);
                                String string = cursorQuery.getString(1);
                                if (j != 0 && !TextUtils.isEmpty(string)) {
                                    Uri lookupUri = ContactsContract.Contacts.getLookupUri(j, string);
                                    if (cursorQuery != null) {
                                        cursorQuery.close();
                                    }
                                    return lookupUri;
                                }
                            }
                        } catch (Exception e) {
                            e = e;
                            Log.e("ContactList", "Error loading the contact: " + this.mUri, e);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            return null;
                        }
                    }
                    Log.e("ContactList", "Error: No contact ID or lookup key for contact " + this.mUri);
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    if (0 != 0) {
                        cursor.close();
                    }
                    throw th;
                }
            } catch (Exception e2) {
                e = e2;
                cursorQuery = null;
            } catch (Throwable th3) {
                th = th3;
                if (0 != 0) {
                }
                throw th;
            }
        }

        public void cancel() {
            super.cancel(true);
            this.mIsCancelled = true;
        }

        @Override
        protected void onPostExecute(Uri uri) {
            if (this.mIsCancelled || !ContactBrowseListFragment.this.isAdded()) {
                return;
            }
            ContactBrowseListFragment.this.onContactUriQueryFinished(uri);
        }
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == 1) {
                        ContactBrowseListFragment.this.selectDefaultContact();
                    }
                }
            };
        }
        return this.mHandler;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        restoreFilter();
        restoreSelectedUri(false);
    }

    @Override
    protected void setSearchMode(boolean z) {
        if (isSearchMode() != z) {
            if (!z) {
                restoreSelectedUri(true);
            }
            super.setSearchMode(z);
        }
    }

    public void updateListFilter(ContactListFilter contactListFilter, boolean z) {
        Log.d("ContactList", "[updateListFilter]");
        if (this.mFilter == null && contactListFilter == null) {
            return;
        }
        if (this.mFilter != null && this.mFilter.equals(contactListFilter)) {
            setLogListEvents(false);
            return;
        }
        if (Log.isLoggable("ContactList", 2)) {
            Log.v("ContactList", "New filter: " + contactListFilter);
        }
        setListType(contactListFilter.toListType());
        setLogListEvents(true);
        this.mFilter = contactListFilter;
        this.mLastSelectedPosition = -1;
        if (z) {
            this.mSelectedContactUri = null;
            restoreSelectedUri(true);
        }
        reloadData();
    }

    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    @Override
    public void restoreSavedState(Bundle bundle) {
        super.restoreSavedState(bundle);
        if (bundle == null) {
            return;
        }
        this.mFilter = (ContactListFilter) bundle.getParcelable("filter");
        this.mSelectedContactUri = (Uri) bundle.getParcelable("selectedUri");
        this.mSelectionVerified = bundle.getBoolean("selectionVerified");
        this.mLastSelectedPosition = bundle.getInt("lastSelected");
        parseSelectedContactUri();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("filter", this.mFilter);
        bundle.putParcelable("selectedUri", this.mSelectedContactUri);
        bundle.putBoolean("selectionVerified", this.mSelectionVerified);
        bundle.putInt("lastSelected", this.mLastSelectedPosition);
    }

    protected void refreshSelectedContactUri() {
        if (this.mContactLookupTask != null) {
            this.mContactLookupTask.cancel();
        }
        if (!isSelectionVisible()) {
            return;
        }
        this.mRefreshingContactUri = true;
        if (this.mSelectedContactUri == null) {
            onContactUriQueryFinished(null);
        } else if (this.mSelectedContactDirectoryId != 0 && this.mSelectedContactDirectoryId != 1) {
            onContactUriQueryFinished(this.mSelectedContactUri);
        } else {
            this.mContactLookupTask = new ContactLookupTask(this.mSelectedContactUri);
            this.mContactLookupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }

    protected void onContactUriQueryFinished(Uri uri) {
        this.mRefreshingContactUri = false;
        this.mSelectedContactUri = uri;
        parseSelectedContactUri();
        checkSelection();
    }

    public void setSelectedContactUri(Uri uri) {
        setSelectedContactUri(uri, true, false, true, false);
    }

    @Override
    public void setQueryString(String str, boolean z) {
        this.mDelaySelection = z;
        super.setQueryString(str, z);
    }

    private void setSelectedContactUri(Uri uri, boolean z, boolean z2, boolean z3, boolean z4) {
        ContactListAdapter adapter;
        this.mSmoothScrollRequested = z2;
        this.mSelectionToScreenRequested = true;
        if ((this.mSelectedContactUri == null && uri != null) || (this.mSelectedContactUri != null && !this.mSelectedContactUri.equals(uri))) {
            this.mSelectionVerified = false;
            this.mSelectionRequired = z;
            this.mSelectionPersistenceRequested = z3;
            this.mSelectedContactUri = uri;
            parseSelectedContactUri();
            if (!z4 && (adapter = getAdapter()) != null) {
                adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
                getListView().invalidateViews();
            }
            refreshSelectedContactUri();
        }
    }

    private void parseSelectedContactUri() {
        if (this.mSelectedContactUri != null) {
            String queryParameter = this.mSelectedContactUri.getQueryParameter("directory");
            this.mSelectedContactDirectoryId = TextUtils.isEmpty(queryParameter) ? 0L : Long.parseLong(queryParameter);
            if (this.mSelectedContactUri.toString().startsWith(ContactsContract.Contacts.CONTENT_LOOKUP_URI.toString())) {
                List<String> pathSegments = this.mSelectedContactUri.getPathSegments();
                this.mSelectedContactLookupKey = Uri.encode(pathSegments.get(2));
                if (pathSegments.size() == 4) {
                    this.mSelectedContactId = ContentUris.parseId(this.mSelectedContactUri);
                    return;
                }
                return;
            }
            if (this.mSelectedContactUri.toString().startsWith(ContactsContract.Contacts.CONTENT_URI.toString()) && this.mSelectedContactUri.getPathSegments().size() >= 2) {
                this.mSelectedContactLookupKey = null;
                this.mSelectedContactId = ContentUris.parseId(this.mSelectedContactUri);
                return;
            }
            Log.e("ContactList", "Unsupported contact URI: " + this.mSelectedContactUri);
            this.mSelectedContactLookupKey = null;
            this.mSelectedContactId = 0L;
            return;
        }
        this.mSelectedContactDirectoryId = 0L;
        this.mSelectedContactLookupKey = null;
        this.mSelectedContactId = 0L;
    }

    @Override
    public ContactListAdapter getAdapter() {
        return (ContactListAdapter) super.getAdapter();
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        boolean zIsSearchMode = isSearchMode();
        if (!zIsSearchMode && this.mFilter != null) {
            adapter.setFilter(this.mFilter);
            if (this.mSelectionRequired || this.mFilter.filterType == -6) {
                adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
            }
        }
        adapter.setIncludeFavorites(!zIsSearchMode && this.mFilter.isContactsFilterType());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        super.onLoadFinished(loader, cursor);
        this.mSelectionVerified = false;
        refreshSelectedContactUri();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void checkSelection() {
        ContactListAdapter adapter;
        boolean zIsLoading;
        Log.d("ContactList", "[checkSelection]");
        if (this.mSelectionVerified || this.mRefreshingContactUri || isLoadingDirectoryList() || (adapter = getAdapter()) == null) {
            return;
        }
        int partitionCount = adapter.getPartitionCount();
        int i = 0;
        while (true) {
            if (i < partitionCount) {
                CompositeCursorAdapter.Partition partition = adapter.getPartition(i);
                if (partition instanceof DirectoryPartition) {
                    DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                    if (directoryPartition.getDirectoryId() == this.mSelectedContactDirectoryId) {
                        zIsLoading = directoryPartition.isLoading();
                        break;
                    }
                }
                i++;
            } else {
                zIsLoading = true;
                break;
            }
        }
        if (zIsLoading) {
            return;
        }
        adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
        int selectedContactPosition = adapter.getSelectedContactPosition();
        if (selectedContactPosition != -1) {
            this.mLastSelectedPosition = selectedContactPosition;
        } else {
            if (isSearchMode()) {
                if (this.mDelaySelection) {
                    selectFirstFoundContactAfterDelay();
                    if (this.mListener != null) {
                        this.mListener.onSelectionChange();
                        return;
                    }
                    return;
                }
            } else {
                if (this.mSelectionRequired) {
                    this.mSelectionRequired = false;
                    if (this.mFilter != null && (this.mFilter.filterType == -6 || this.mFilter.filterType == -2)) {
                        reloadData();
                        return;
                    } else {
                        notifyInvalidSelection();
                        return;
                    }
                }
                if (this.mFilter != null && this.mFilter.filterType == -6) {
                    notifyInvalidSelection();
                    return;
                }
            }
            saveSelectedUri(null);
            selectDefaultContact();
        }
        this.mSelectionRequired = false;
        this.mSelectionVerified = true;
        if (this.mSelectionPersistenceRequested) {
            saveSelectedUri(this.mSelectedContactUri);
            this.mSelectionPersistenceRequested = false;
        }
        if (this.mSelectionToScreenRequested) {
            requestSelectionToScreen(selectedContactPosition);
        }
        getListView().invalidateViews();
        if (this.mListener != null) {
            this.mListener.onSelectionChange();
        }
    }

    public void selectFirstFoundContactAfterDelay() {
        Handler handler = getHandler();
        handler.removeMessages(1);
        String queryString = getQueryString();
        if (queryString != null && queryString.length() >= 2) {
            handler.sendEmptyMessageDelayed(1, 500L);
        } else {
            setSelectedContactUri(null, false, false, false, false);
        }
    }

    protected void selectDefaultContact() {
        Uri firstContactUri;
        ContactListAdapter adapter = getAdapter();
        if (this.mLastSelectedPosition != -1) {
            int count = adapter.getCount();
            int i = this.mLastSelectedPosition;
            if (i >= count && count > 0) {
                i = count - 1;
            }
            firstContactUri = adapter.getContactUri(i);
        } else {
            firstContactUri = null;
        }
        if (firstContactUri == null) {
            firstContactUri = adapter.getFirstContactUri();
        }
        setSelectedContactUri(firstContactUri, false, this.mSmoothScrollRequested, false, false);
    }

    protected void requestSelectionToScreen(int i) {
        if (i != -1) {
            AutoScrollListView autoScrollListView = (AutoScrollListView) getListView();
            autoScrollListView.requestPositionToScreen(i + autoScrollListView.getHeaderViewsCount(), this.mSmoothScrollRequested);
            this.mSelectionToScreenRequested = false;
        }
    }

    @Override
    public boolean isLoading() {
        return this.mRefreshingContactUri || super.isLoading();
    }

    @Override
    protected void startLoading() {
        this.mStartedLoading = true;
        this.mSelectionVerified = false;
        super.startLoading();
    }

    @Override
    public void reloadData() {
        if (this.mStartedLoading) {
            this.mSelectionVerified = false;
            this.mLastSelectedPosition = -1;
            super.reloadData();
        }
    }

    public void setOnContactListActionListener(OnContactBrowserActionListener onContactBrowserActionListener) {
        this.mListener = onContactBrowserActionListener;
    }

    public void viewContact(int i, Uri uri, boolean z) {
        setSelectedContactUri(uri, false, false, true, false);
        if (this.mListener != null) {
            this.mListener.onViewContactAction(i, uri, z);
        }
    }

    private void notifyInvalidSelection() {
        if (this.mListener != null) {
            this.mListener.onInvalidSelection();
        }
    }

    private void saveSelectedUri(Uri uri) {
        if (isSearchMode()) {
            return;
        }
        ContactListFilter.storeToPreferences(this.mPrefs, this.mFilter);
        SharedPreferences.Editor editorEdit = this.mPrefs.edit();
        if (uri == null) {
            editorEdit.remove(getPersistentSelectionKey());
        } else {
            editorEdit.putString(getPersistentSelectionKey(), uri.toString());
        }
        editorEdit.apply();
    }

    private void restoreSelectedUri(boolean z) {
        if (this.mSelectionRequired) {
            return;
        }
        String string = this.mPrefs.getString(getPersistentSelectionKey(), null);
        if (string == null) {
            setSelectedContactUri(null, false, false, false, z);
        } else {
            setSelectedContactUri(Uri.parse(string), false, false, false, z);
        }
    }

    private void restoreFilter() {
        this.mFilter = ContactListFilter.restoreDefaultPreferences(this.mPrefs);
    }

    private String getPersistentSelectionKey() {
        if (this.mFilter == null) {
            return this.mPersistentSelectionPrefix;
        }
        return this.mPersistentSelectionPrefix + "-" + this.mFilter.getId();
    }
}
