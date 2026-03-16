package com.android.contacts.common.list;

import android.R;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.common.widget.CompositeCursorAdapter;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.list.ContactEntryListAdapter;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.contacts.common.util.ContactListViewUtils;
import java.util.Locale;

public abstract class ContactEntryListFragment<T extends ContactEntryListAdapter> extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnFocusChangeListener, View.OnTouchListener, AbsListView.OnScrollListener, AdapterView.OnItemClickListener {
    private T mAdapter;
    private ContactsPreferences mContactsPrefs;
    private Context mContext;
    private boolean mDarkTheme;
    private int mDisplayOrder;
    private boolean mForceLoad;
    private boolean mIncludeProfile;
    private boolean mLegacyCompatibility;
    private Parcelable mListState;
    private ListView mListView;
    private boolean mLoadPriorityDirectoriesOnly;
    private LoaderManager mLoaderManager;
    private boolean mPhotoLoaderEnabled;
    private ContactPhotoManager mPhotoManager;
    private String mQueryString;
    private boolean mSearchMode;
    private boolean mSectionHeaderDisplayEnabled;
    private boolean mSelectionVisible;
    private boolean mShowEmptyListForEmptyQuery;
    private int mSortOrder;
    protected boolean mUserProfileExists;
    private View mView;
    private boolean mVisibleScrollbarEnabled;
    private boolean mQuickContactEnabled = true;
    private boolean mAdjustSelectionBoundsEnabled = true;
    private int mVerticalScrollbarPosition = getDefaultVerticalScrollbarPosition();
    private int mDirectorySearchMode = 0;
    private boolean mEnabled = true;
    private int mDirectoryResultLimit = 20;
    private int mDirectoryListStatus = 0;
    private Handler mDelayedDirectorySearchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ContactEntryListFragment.this.loadDirectoryPartition(msg.arg1, (DirectoryPartition) msg.obj);
            }
        }
    };
    private ContactsPreferences.ChangeListener mPreferencesChangeListener = new ContactsPreferences.ChangeListener() {
        @Override
        public void onChange() {
            ContactEntryListFragment.this.loadPreferences();
            ContactEntryListFragment.this.reloadData();
        }
    };

    protected abstract T createListAdapter();

    protected abstract View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup);

    protected abstract void onItemClick(int i, long j);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setContext(activity);
        setLoaderManager(super.getLoaderManager());
    }

    public void setContext(Context context) {
        this.mContext = context;
        configurePhotoLoader();
    }

    @Override
    public Context getContext() {
        return this.mContext;
    }

    public void setEnabled(boolean enabled) {
        if (this.mEnabled != enabled) {
            this.mEnabled = enabled;
            if (this.mAdapter != null) {
                if (this.mEnabled) {
                    reloadData();
                } else {
                    this.mAdapter.clearPartitions();
                }
            }
        }
    }

    public void setLoaderManager(LoaderManager loaderManager) {
        this.mLoaderManager = loaderManager;
    }

    @Override
    public LoaderManager getLoaderManager() {
        return this.mLoaderManager;
    }

    public T getAdapter() {
        return this.mAdapter;
    }

    @Override
    public View getView() {
        return this.mView;
    }

    public ListView getListView() {
        return this.mListView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("sectionHeaderDisplayEnabled", this.mSectionHeaderDisplayEnabled);
        outState.putBoolean("photoLoaderEnabled", this.mPhotoLoaderEnabled);
        outState.putBoolean("quickContactEnabled", this.mQuickContactEnabled);
        outState.putBoolean("adjustSelectionBoundsEnabled", this.mAdjustSelectionBoundsEnabled);
        outState.putBoolean("includeProfile", this.mIncludeProfile);
        outState.putBoolean("searchMode", this.mSearchMode);
        outState.putBoolean("visibleScrollbarEnabled", this.mVisibleScrollbarEnabled);
        outState.putInt("scrollbarPosition", this.mVerticalScrollbarPosition);
        outState.putInt("directorySearchMode", this.mDirectorySearchMode);
        outState.putBoolean("selectionVisible", this.mSelectionVisible);
        outState.putBoolean("legacyCompatibility", this.mLegacyCompatibility);
        outState.putString("queryString", this.mQueryString);
        outState.putInt("directoryResultLimit", this.mDirectoryResultLimit);
        outState.putBoolean("darkTheme", this.mDarkTheme);
        if (this.mListView != null) {
            outState.putParcelable("liststate", this.mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        restoreSavedState(bundle);
        this.mAdapter = (T) createListAdapter();
        this.mContactsPrefs = new ContactsPreferences(this.mContext);
    }

    public void restoreSavedState(Bundle savedState) {
        if (savedState != null) {
            this.mSectionHeaderDisplayEnabled = savedState.getBoolean("sectionHeaderDisplayEnabled");
            this.mPhotoLoaderEnabled = savedState.getBoolean("photoLoaderEnabled");
            this.mQuickContactEnabled = savedState.getBoolean("quickContactEnabled");
            this.mAdjustSelectionBoundsEnabled = savedState.getBoolean("adjustSelectionBoundsEnabled");
            this.mIncludeProfile = savedState.getBoolean("includeProfile");
            this.mSearchMode = savedState.getBoolean("searchMode");
            this.mVisibleScrollbarEnabled = savedState.getBoolean("visibleScrollbarEnabled");
            this.mVerticalScrollbarPosition = savedState.getInt("scrollbarPosition");
            this.mDirectorySearchMode = savedState.getInt("directorySearchMode");
            this.mSelectionVisible = savedState.getBoolean("selectionVisible");
            this.mLegacyCompatibility = savedState.getBoolean("legacyCompatibility");
            this.mQueryString = savedState.getString("queryString");
            this.mDirectoryResultLimit = savedState.getInt("directoryResultLimit");
            this.mDarkTheme = savedState.getBoolean("darkTheme");
            this.mListState = savedState.getParcelable("liststate");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.mContactsPrefs.registerChangeListener(this.mPreferencesChangeListener);
        this.mForceLoad = loadPreferences();
        this.mDirectoryListStatus = 0;
        this.mLoadPriorityDirectoriesOnly = true;
        startLoading();
    }

    protected void startLoading() {
        if (this.mAdapter != null) {
            configureAdapter();
            int partitionCount = this.mAdapter.getPartitionCount();
            for (int i = 0; i < partitionCount; i++) {
                CompositeCursorAdapter.Partition partition = this.mAdapter.getPartition(i);
                if (partition instanceof DirectoryPartition) {
                    DirectoryPartition directoryPartition = (DirectoryPartition) partition;
                    if (directoryPartition.getStatus() == 0 && (directoryPartition.isPriorityDirectory() || !this.mLoadPriorityDirectoriesOnly)) {
                        startLoadingDirectoryPartition(i);
                    }
                } else {
                    getLoaderManager().initLoader(i, null, this);
                }
            }
            this.mLoadPriorityDirectoriesOnly = false;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == -1) {
            DirectoryListLoader loader = new DirectoryListLoader(this.mContext);
            loader.setDirectorySearchMode(this.mAdapter.getDirectorySearchMode());
            loader.setLocalInvisibleDirectoryEnabled(false);
            return loader;
        }
        CursorLoader loader2 = createCursorLoader(this.mContext);
        long directoryId = (args == null || !args.containsKey("directoryId")) ? 0L : args.getLong("directoryId");
        this.mAdapter.configureLoader(loader2, directoryId);
        return loader2;
    }

    public CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, null, null, null, null, null);
    }

    private void startLoadingDirectoryPartition(int partitionIndex) {
        DirectoryPartition partition = (DirectoryPartition) this.mAdapter.getPartition(partitionIndex);
        partition.setStatus(1);
        long directoryId = partition.getDirectoryId();
        if (!this.mForceLoad) {
            Bundle args = new Bundle();
            args.putLong("directoryId", directoryId);
            getLoaderManager().initLoader(partitionIndex, args, this);
        } else if (directoryId == 0) {
            loadDirectoryPartition(partitionIndex, partition);
        } else {
            loadDirectoryPartitionDelayed(partitionIndex, partition);
        }
    }

    private void loadDirectoryPartitionDelayed(int partitionIndex, DirectoryPartition partition) {
        this.mDelayedDirectorySearchHandler.removeMessages(1, partition);
        Message msg = this.mDelayedDirectorySearchHandler.obtainMessage(1, partitionIndex, 0, partition);
        this.mDelayedDirectorySearchHandler.sendMessageDelayed(msg, 300L);
    }

    protected void loadDirectoryPartition(int partitionIndex, DirectoryPartition partition) {
        Bundle args = new Bundle();
        args.putLong("directoryId", partition.getDirectoryId());
        getLoaderManager().restartLoader(partitionIndex, args, this);
    }

    private void removePendingDirectorySearchRequests() {
        this.mDelayedDirectorySearchHandler.removeMessages(1);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (this.mEnabled) {
            int loaderId = loader.getId();
            if (loaderId == -1) {
                this.mDirectoryListStatus = 2;
                this.mAdapter.changeDirectories(data);
                startLoading();
                return;
            }
            onPartitionLoaded(loaderId, data);
            if (isSearchMode()) {
                int directorySearchMode = getDirectorySearchMode();
                if (directorySearchMode != 0) {
                    if (this.mDirectoryListStatus == 0) {
                        this.mDirectoryListStatus = 1;
                        getLoaderManager().initLoader(-1, null, this);
                        return;
                    } else {
                        startLoading();
                        return;
                    }
                }
                return;
            }
            this.mDirectoryListStatus = 0;
            getLoaderManager().destroyLoader(-1);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected void onPartitionLoaded(int partitionIndex, Cursor data) {
        if (partitionIndex < this.mAdapter.getPartitionCount()) {
            this.mAdapter.changeCursor(partitionIndex, data);
            setProfileHeader();
            if (!isLoading()) {
                completeRestoreInstanceState();
            }
        }
    }

    public boolean isLoading() {
        return (this.mAdapter != null && this.mAdapter.isLoading()) || isLoadingDirectoryList();
    }

    public boolean isLoadingDirectoryList() {
        return isSearchMode() && getDirectorySearchMode() != 0 && (this.mDirectoryListStatus == 0 || this.mDirectoryListStatus == 1);
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mContactsPrefs.unregisterChangeListener();
        this.mAdapter.clearPartitions();
    }

    protected void reloadData() {
        removePendingDirectorySearchRequests();
        this.mAdapter.onDataReload();
        this.mLoadPriorityDirectoriesOnly = true;
        this.mForceLoad = true;
        startLoading();
    }

    protected void setProfileHeader() {
        this.mUserProfileExists = false;
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        if (this.mSectionHeaderDisplayEnabled != flag) {
            this.mSectionHeaderDisplayEnabled = flag;
            if (this.mAdapter != null) {
                this.mAdapter.setSectionHeaderDisplayEnabled(flag);
            }
            configureVerticalScrollbar();
        }
    }

    public boolean isSectionHeaderDisplayEnabled() {
        return this.mSectionHeaderDisplayEnabled;
    }

    public void setVisibleScrollbarEnabled(boolean flag) {
        if (this.mVisibleScrollbarEnabled != flag) {
            this.mVisibleScrollbarEnabled = flag;
            configureVerticalScrollbar();
        }
    }

    public boolean isVisibleScrollbarEnabled() {
        return this.mVisibleScrollbarEnabled;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (this.mVerticalScrollbarPosition != position) {
            this.mVerticalScrollbarPosition = position;
            configureVerticalScrollbar();
        }
    }

    private void configureVerticalScrollbar() {
        boolean hasScrollbar = isVisibleScrollbarEnabled() && isSectionHeaderDisplayEnabled();
        if (this.mListView != null) {
            this.mListView.setFastScrollEnabled(hasScrollbar);
            this.mListView.setFastScrollAlwaysVisible(hasScrollbar);
            this.mListView.setVerticalScrollbarPosition(this.mVerticalScrollbarPosition);
            this.mListView.setScrollBarStyle(33554432);
        }
    }

    public void setPhotoLoaderEnabled(boolean flag) {
        this.mPhotoLoaderEnabled = flag;
        configurePhotoLoader();
    }

    public boolean isPhotoLoaderEnabled() {
        return this.mPhotoLoaderEnabled;
    }

    public boolean isSelectionVisible() {
        return this.mSelectionVisible;
    }

    public void setSelectionVisible(boolean flag) {
        this.mSelectionVisible = flag;
    }

    public void setQuickContactEnabled(boolean flag) {
        this.mQuickContactEnabled = flag;
    }

    public void setIncludeProfile(boolean flag) {
        this.mIncludeProfile = flag;
        if (this.mAdapter != null) {
            this.mAdapter.setIncludeProfile(flag);
        }
    }

    protected void setSearchMode(boolean flag) {
        if (this.mSearchMode != flag) {
            this.mSearchMode = flag;
            setSectionHeaderDisplayEnabled(!this.mSearchMode);
            if (!flag) {
                this.mDirectoryListStatus = 0;
                getLoaderManager().destroyLoader(-1);
            }
            if (this.mAdapter != null) {
                this.mAdapter.setSearchMode(flag);
                this.mAdapter.clearPartitions();
                if (!flag) {
                    this.mAdapter.removeDirectoriesAfterDefault();
                }
                this.mAdapter.configureDefaultPartition(false, flag);
            }
            if (this.mListView != null) {
                this.mListView.setFastScrollEnabled(flag ? false : true);
            }
        }
    }

    public final boolean isSearchMode() {
        return this.mSearchMode;
    }

    public final String getQueryString() {
        return this.mQueryString;
    }

    public void setQueryString(String queryString, boolean delaySelection) {
        if (!TextUtils.equals(this.mQueryString, queryString)) {
            if (this.mShowEmptyListForEmptyQuery && this.mAdapter != null && this.mListView != null) {
                if (TextUtils.isEmpty(this.mQueryString)) {
                    this.mListView.setAdapter((ListAdapter) this.mAdapter);
                } else if (TextUtils.isEmpty(queryString)) {
                    this.mListView.setAdapter((ListAdapter) null);
                }
            }
            this.mQueryString = queryString;
            setSearchMode(!TextUtils.isEmpty(this.mQueryString) || this.mShowEmptyListForEmptyQuery);
            if (this.mAdapter != null) {
                this.mAdapter.setQueryString(queryString);
                reloadData();
            }
        }
    }

    public int getDirectorySearchMode() {
        return this.mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int mode) {
        this.mDirectorySearchMode = mode;
    }

    public boolean isLegacyCompatibilityMode() {
        return this.mLegacyCompatibility;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        this.mLegacyCompatibility = flag;
    }

    protected int getContactNameDisplayOrder() {
        return this.mDisplayOrder;
    }

    protected void setContactNameDisplayOrder(int displayOrder) {
        this.mDisplayOrder = displayOrder;
        if (this.mAdapter != null) {
            this.mAdapter.setContactNameDisplayOrder(displayOrder);
        }
    }

    public int getSortOrder() {
        return this.mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.mSortOrder = sortOrder;
        if (this.mAdapter != null) {
            this.mAdapter.setSortOrder(sortOrder);
        }
    }

    public void setDirectoryResultLimit(int limit) {
        this.mDirectoryResultLimit = limit;
    }

    protected boolean loadPreferences() {
        boolean changed = false;
        if (getContactNameDisplayOrder() != this.mContactsPrefs.getDisplayOrder()) {
            setContactNameDisplayOrder(this.mContactsPrefs.getDisplayOrder());
            changed = true;
        }
        if (getSortOrder() != this.mContactsPrefs.getSortOrder()) {
            setSortOrder(this.mContactsPrefs.getSortOrder());
            return true;
        }
        return changed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        onCreateView(inflater, container);
        boolean searchMode = isSearchMode();
        this.mAdapter.setSearchMode(searchMode);
        this.mAdapter.configureDefaultPartition(false, searchMode);
        this.mAdapter.setPhotoLoader(this.mPhotoManager);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        if (!isSearchMode()) {
            this.mListView.setFocusableInTouchMode(true);
            this.mListView.requestFocus();
        }
        return this.mView;
    }

    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        this.mView = inflateView(inflater, container);
        this.mListView = (ListView) this.mView.findViewById(R.id.list);
        if (this.mListView == null) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
        }
        View emptyView = this.mView.findViewById(R.id.empty);
        if (emptyView != null) {
            this.mListView.setEmptyView(emptyView);
        }
        this.mListView.setOnItemClickListener(this);
        this.mListView.setOnFocusChangeListener(this);
        this.mListView.setOnTouchListener(this);
        this.mListView.setFastScrollEnabled(!isSearchMode());
        this.mListView.setDividerHeight(0);
        this.mListView.setSaveEnabled(false);
        configureVerticalScrollbar();
        configurePhotoLoader();
        getAdapter().setFragmentRootView(getView());
        ContactListViewUtils.applyCardPaddingToView(getResources(), this.mListView, this.mView);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() != null && getView() != null && !hidden) {
            ContactListViewUtils.applyCardPaddingToView(getResources(), this.mListView, getView());
        }
    }

    protected void configurePhotoLoader() {
        if (isPhotoLoaderEnabled() && this.mContext != null) {
            if (this.mPhotoManager == null) {
                this.mPhotoManager = ContactPhotoManager.getInstance(this.mContext);
            }
            if (this.mListView != null) {
                this.mListView.setOnScrollListener(this);
            }
            if (this.mAdapter != null) {
                this.mAdapter.setPhotoLoader(this.mPhotoManager);
            }
        }
    }

    protected void configureAdapter() {
        if (this.mAdapter != null) {
            this.mAdapter.setQuickContactEnabled(this.mQuickContactEnabled);
            this.mAdapter.setAdjustSelectionBoundsEnabled(this.mAdjustSelectionBoundsEnabled);
            this.mAdapter.setIncludeProfile(this.mIncludeProfile);
            this.mAdapter.setQueryString(this.mQueryString);
            this.mAdapter.setDirectorySearchMode(this.mDirectorySearchMode);
            this.mAdapter.setPinnedPartitionHeadersEnabled(false);
            this.mAdapter.setContactNameDisplayOrder(this.mDisplayOrder);
            this.mAdapter.setSortOrder(this.mSortOrder);
            this.mAdapter.setSectionHeaderDisplayEnabled(this.mSectionHeaderDisplayEnabled);
            this.mAdapter.setSelectionVisible(this.mSelectionVisible);
            this.mAdapter.setDirectoryResultLimit(this.mDirectoryResultLimit);
            this.mAdapter.setDarkTheme(this.mDarkTheme);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == 2) {
            this.mPhotoManager.pause();
        } else if (isPhotoLoaderEnabled()) {
            this.mPhotoManager.resume();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();
        int adjPosition = position - this.mListView.getHeaderViewsCount();
        if (adjPosition >= 0) {
            onItemClick(adjPosition, id);
        }
    }

    private void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.mContext.getSystemService("input_method");
        inputMethodManager.hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        if (view == this.mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == this.mListView) {
            hideSoftKeyboard();
            return false;
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        removePendingDirectorySearchRequests();
    }

    protected void completeRestoreInstanceState() {
        if (this.mListState != null) {
            this.mListView.onRestoreInstanceState(this.mListState);
            this.mListState = null;
        }
    }

    public void onPickerResult(Intent data) {
        throw new UnsupportedOperationException("Picker result handler is not implemented.");
    }

    private int getDefaultVerticalScrollbarPosition() {
        Locale locale = Locale.getDefault();
        int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
        switch (layoutDirection) {
            case 1:
                return 1;
            default:
                return 2;
        }
    }
}
