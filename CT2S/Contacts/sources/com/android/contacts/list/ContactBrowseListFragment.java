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
import android.util.Log;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.common.list.AutoScrollListView;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.DirectoryPartition;
import com.android.contacts.common.util.ContactLoaderUtils;
import java.util.List;

public abstract class ContactBrowseListFragment extends ContactEntryListFragment<ContactListAdapter> {
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

    private final class ContactLookupTask extends AsyncTask<Void, Void, Uri> {
        private boolean mIsCancelled;
        private final Uri mUri;

        public ContactLookupTask(Uri uri) {
            this.mUri = uri;
        }

        @Override
        protected Uri doInBackground(Void... args) {
            Uri lookupUri;
            Cursor cursor = null;
            try {
                try {
                    ContentResolver resolver = ContactBrowseListFragment.this.getContext().getContentResolver();
                    Uri uriCurrentFormat = ContactLoaderUtils.ensureIsContactUri(resolver, this.mUri);
                    cursor = resolver.query(uriCurrentFormat, new String[]{"_id", "lookup"}, null, null, null);
                    if (cursor == null || !cursor.moveToFirst()) {
                        Log.e("ContactList", "Error: No contact ID or lookup key for contact " + this.mUri);
                        if (cursor != null) {
                            cursor.close();
                        }
                        lookupUri = null;
                    } else {
                        long contactId = cursor.getLong(0);
                        String lookupKey = cursor.getString(1);
                        if (contactId != 0 && !TextUtils.isEmpty(lookupKey)) {
                            lookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("ContactList", "Error loading the contact: " + this.mUri, e);
                    if (cursor != null) {
                        cursor.close();
                        lookupUri = null;
                    } else {
                        lookupUri = null;
                    }
                }
                return lookupUri;
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
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
            if (!this.mIsCancelled && ContactBrowseListFragment.this.isAdded()) {
                ContactBrowseListFragment.this.onContactUriQueryFinished(uri);
            }
        }
    }

    private Handler getHandler() {
        if (this.mHandler == null) {
            this.mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            ContactBrowseListFragment.this.selectDefaultContact();
                            break;
                    }
                }
            };
        }
        return this.mHandler;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d("ContactList", "onAttach start");
        super.onAttach(activity);
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        restoreFilter();
        restoreSelectedUri(false);
    }

    @Override
    protected void setSearchMode(boolean flag) {
        if (isSearchMode() != flag) {
            if (!flag) {
                restoreSelectedUri(true);
            }
            super.setSearchMode(flag);
        }
    }

    public void setFilter(ContactListFilter filter) {
        setFilter(filter, true);
    }

    public void setFilter(ContactListFilter filter, boolean restoreSelectedUri) {
        if (this.mFilter != null || filter != null) {
            if (this.mFilter == null || !this.mFilter.equals(filter)) {
                Log.v("ContactList", "New filter: " + filter);
                this.mFilter = filter;
                this.mLastSelectedPosition = -1;
                saveFilter();
                if (restoreSelectedUri) {
                    this.mSelectedContactUri = null;
                    restoreSelectedUri(true);
                }
                reloadData();
            }
        }
    }

    public ContactListFilter getFilter() {
        return this.mFilter;
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);
        if (savedState != null) {
            this.mFilter = (ContactListFilter) savedState.getParcelable("filter");
            this.mSelectedContactUri = (Uri) savedState.getParcelable("selectedUri");
            this.mSelectionVerified = savedState.getBoolean("selectionVerified");
            this.mLastSelectedPosition = savedState.getInt("lastSelected");
            parseSelectedContactUri();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("filter", this.mFilter);
        outState.putParcelable("selectedUri", this.mSelectedContactUri);
        outState.putBoolean("selectionVerified", this.mSelectionVerified);
        outState.putInt("lastSelected", this.mLastSelectedPosition);
    }

    protected void refreshSelectedContactUri() {
        if (this.mContactLookupTask != null) {
            this.mContactLookupTask.cancel();
        }
        if (isSelectionVisible()) {
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
    public void setQueryString(String queryString, boolean delaySelection) {
        this.mDelaySelection = delaySelection;
        super.setQueryString(queryString, delaySelection);
    }

    private void setSelectedContactUri(Uri uri, boolean required, boolean smoothScroll, boolean persistent, boolean willReloadData) {
        ContactListAdapter adapter;
        this.mSmoothScrollRequested = smoothScroll;
        this.mSelectionToScreenRequested = true;
        if ((this.mSelectedContactUri == null && uri != null) || (this.mSelectedContactUri != null && !this.mSelectedContactUri.equals(uri))) {
            this.mSelectionVerified = false;
            this.mSelectionRequired = required;
            this.mSelectionPersistenceRequested = persistent;
            this.mSelectedContactUri = uri;
            parseSelectedContactUri();
            if (!willReloadData && (adapter = getAdapter()) != null) {
                adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
                getListView().invalidateViews();
            }
            refreshSelectedContactUri();
        }
    }

    private void parseSelectedContactUri() {
        if (this.mSelectedContactUri != null) {
            String directoryParam = this.mSelectedContactUri.getQueryParameter("directory");
            this.mSelectedContactDirectoryId = TextUtils.isEmpty(directoryParam) ? 0L : Long.parseLong(directoryParam);
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
            } else {
                Log.e("ContactList", "Unsupported contact URI: " + this.mSelectedContactUri);
                this.mSelectedContactLookupKey = null;
                this.mSelectedContactId = 0L;
                return;
            }
        }
        this.mSelectedContactDirectoryId = 0L;
        this.mSelectedContactLookupKey = null;
        this.mSelectedContactId = 0L;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        ContactListAdapter adapter = getAdapter();
        if (adapter != null) {
            boolean searchMode = isSearchMode();
            if (!searchMode && this.mFilter != null) {
                adapter.setFilter(this.mFilter);
                if (this.mSelectionRequired || this.mFilter.filterType == -6) {
                    adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
                }
            }
            adapter.setIncludeProfile(!searchMode);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        super.onLoadFinished(loader, data);
        this.mSelectionVerified = false;
        refreshSelectedContactUri();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void checkSelection() {
        ContactListAdapter adapter;
        if (!this.mSelectionVerified && !this.mRefreshingContactUri && !isLoadingDirectoryList() && (adapter = getAdapter()) != null) {
            boolean directoryLoading = true;
            int count = adapter.getPartitionCount();
            int i = 0;
            while (true) {
                if (i >= count) {
                    break;
                }
                CompositeCursorAdapter.Partition partition = adapter.getPartition(i);
                if (partition instanceof DirectoryPartition) {
                    DirectoryPartition directory = (DirectoryPartition) partition;
                    if (directory.getDirectoryId() == this.mSelectedContactDirectoryId) {
                        directoryLoading = directory.isLoading();
                        break;
                    }
                }
                i++;
            }
            if (!directoryLoading) {
                adapter.setSelectedContact(this.mSelectedContactDirectoryId, this.mSelectedContactLookupKey, this.mSelectedContactId);
                int selectedPosition = adapter.getSelectedContactPosition();
                if (selectedPosition != -1) {
                    this.mLastSelectedPosition = selectedPosition;
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
                    requestSelectionToScreen(selectedPosition);
                }
                getListView().invalidateViews();
                if (this.mListener != null) {
                    this.mListener.onSelectionChange();
                }
            }
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
        Uri contactUri = null;
        ContactListAdapter adapter = getAdapter();
        if (this.mLastSelectedPosition != -1) {
            int count = adapter.getCount();
            int pos = this.mLastSelectedPosition;
            if (pos >= count && count > 0) {
                pos = count - 1;
            }
            contactUri = adapter.getContactUri(pos);
        }
        if (contactUri == null) {
            contactUri = adapter.getFirstContactUri();
        }
        setSelectedContactUri(contactUri, false, this.mSmoothScrollRequested, false, false);
    }

    protected void requestSelectionToScreen(int selectedPosition) {
        if (selectedPosition != -1) {
            AutoScrollListView listView = (AutoScrollListView) getListView();
            listView.requestPositionToScreen(listView.getHeaderViewsCount() + selectedPosition, this.mSmoothScrollRequested);
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

    public void setOnContactListActionListener(OnContactBrowserActionListener listener) {
        this.mListener = listener;
    }

    public void viewContact(Uri contactUri) {
        setSelectedContactUri(contactUri, false, false, true, false);
        if (this.mListener != null) {
            this.mListener.onViewContactAction(contactUri);
        }
    }

    private void notifyInvalidSelection() {
        if (this.mListener != null) {
            this.mListener.onInvalidSelection();
        }
    }

    private void saveSelectedUri(Uri contactUri) {
        if (!isSearchMode()) {
            ContactListFilter.storeToPreferences(this.mPrefs, this.mFilter);
            SharedPreferences.Editor editor = this.mPrefs.edit();
            if (contactUri == null) {
                editor.remove(getPersistentSelectionKey());
            } else {
                editor.putString(getPersistentSelectionKey(), contactUri.toString());
            }
            editor.apply();
        }
    }

    private void restoreSelectedUri(boolean willReloadData) {
        if (!this.mSelectionRequired) {
            String selectedUri = this.mPrefs.getString(getPersistentSelectionKey(), null);
            if (selectedUri == null) {
                setSelectedContactUri(null, false, false, false, willReloadData);
            } else {
                setSelectedContactUri(Uri.parse(selectedUri), false, false, false, willReloadData);
            }
        }
    }

    private void saveFilter() {
        ContactListFilter.storeToPreferences(this.mPrefs, this.mFilter);
    }

    private void restoreFilter() {
        this.mFilter = ContactListFilter.restoreDefaultPreferences(this.mPrefs);
    }

    private String getPersistentSelectionKey() {
        return this.mFilter == null ? this.mPersistentSelectionPrefix : this.mPersistentSelectionPrefix + "-" + this.mFilter.getId();
    }

    public boolean isOptionsMenuChanged() {
        return false;
    }
}
