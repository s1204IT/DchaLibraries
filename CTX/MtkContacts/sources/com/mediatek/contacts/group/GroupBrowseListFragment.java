package com.mediatek.contacts.group;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.GroupListLoader;
import com.android.contacts.R;
import com.android.contacts.list.AutoScrollListView;
import com.mediatek.contacts.util.Log;

public class GroupBrowseListFragment extends Fragment implements View.OnFocusChangeListener, View.OnTouchListener {
    private GroupBrowseListAdapter mAdapter;
    private Context mContext;
    private TextView mEmptyView;
    private Cursor mGroupListCursor;
    private AutoScrollListView mListView;
    private OnGroupBrowserActionListener mListener;
    private View mRootView;
    private Uri mSelectedGroupUri;
    private boolean mSelectionToScreenRequested;
    private boolean mSelectionVisible;
    private int mVerticalScrollbarPosition = 2;
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int i, Bundle bundle) {
            GroupBrowseListFragment.this.mEmptyView.setText((CharSequence) null);
            return new GroupListLoader(GroupBrowseListFragment.this.mContext);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (GroupBrowseListFragment.this.isAdded()) {
                GroupBrowseListFragment.this.mGroupListCursor = cursor;
                GroupBrowseListFragment.this.bindGroupList();
            } else {
                Log.w("GroupBrowseListFragment", "[onLoadFinished] This Fragment is not add to the Activity now");
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    public interface OnGroupBrowserActionListener {
        void onViewGroupAction(Uri uri);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        if (bundle != null) {
            this.mSelectedGroupUri = (Uri) bundle.getParcelable("groups.groupUri");
            if (this.mSelectedGroupUri != null) {
                this.mSelectionToScreenRequested = true;
            }
        }
        this.mRootView = layoutInflater.inflate(R.layout.group_browse_list_fragment, (ViewGroup) null);
        this.mEmptyView = (TextView) this.mRootView.findViewById(R.id.empty);
        this.mAdapter = configAdapter();
        this.mAdapter.setSelectionVisible(this.mSelectionVisible);
        this.mAdapter.setSelectedGroup(this.mSelectedGroupUri);
        this.mListView = (AutoScrollListView) this.mRootView.findViewById(R.id.list);
        this.mListView.setOnFocusChangeListener(this);
        this.mListView.setOnTouchListener(this);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mListView.setOnItemClickListener(configOnItemClickListener());
        this.mListView.setEmptyView(this.mEmptyView);
        configureVerticalScrollbar();
        return this.mRootView;
    }

    private void configureVerticalScrollbar() {
        int dimensionPixelOffset;
        this.mListView.setVerticalScrollbarPosition(this.mVerticalScrollbarPosition);
        this.mListView.setScrollBarStyle(33554432);
        int dimensionPixelOffset2 = 0;
        if (this.mVerticalScrollbarPosition == 1) {
            dimensionPixelOffset = this.mContext.getResources().getDimensionPixelOffset(R.dimen.list_visible_scrollbar_padding);
        } else {
            dimensionPixelOffset2 = this.mContext.getResources().getDimensionPixelOffset(R.dimen.list_visible_scrollbar_padding);
            dimensionPixelOffset = 0;
        }
        this.mListView.setPadding(dimensionPixelOffset, this.mListView.getPaddingTop(), dimensionPixelOffset2, this.mListView.getPaddingBottom());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mContext = null;
    }

    @Override
    public void onStart() {
        getLoaderManager().restartLoader(1, null, this.mGroupLoaderListener);
        super.onStart();
    }

    private void bindGroupList() {
        this.mEmptyView.setText(R.string.noGroups);
        if (this.mGroupListCursor == null) {
            return;
        }
        this.mAdapter.setCursor(this.mGroupListCursor);
        if (this.mSelectionToScreenRequested) {
            this.mSelectionToScreenRequested = false;
            requestSelectionToScreen();
        }
        this.mSelectedGroupUri = this.mAdapter.getSelectedGroup();
        if (this.mSelectionVisible && this.mSelectedGroupUri != null) {
            viewGroup(this.mSelectedGroupUri);
        }
    }

    private void setSelectedGroup(Uri uri) {
        this.mSelectedGroupUri = uri;
        this.mAdapter.setSelectedGroup(uri);
        this.mListView.invalidateViews();
    }

    private void viewGroup(Uri uri) {
        setSelectedGroup(uri);
        if (this.mListener != null) {
            this.mListener.onViewGroupAction(uri);
        }
    }

    protected void requestSelectionToScreen() {
        int selectedGroupPosition;
        if (this.mSelectionVisible && (selectedGroupPosition = this.mAdapter.getSelectedGroupPosition()) != -1) {
            this.mListView.requestPositionToScreen(selectedGroupPosition, true);
        }
    }

    private void hideSoftKeyboard() {
        if (this.mContext == null) {
            return;
        }
        ((InputMethodManager) this.mContext.getSystemService("input_method")).hideSoftInputFromWindow(this.mListView.getWindowToken(), 0);
    }

    @Override
    public void onFocusChange(View view, boolean z) {
        if (view == this.mListView && z) {
            hideSoftKeyboard();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view == this.mListView) {
            hideSoftKeyboard();
            return false;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("groups.groupUri", this.mSelectedGroupUri);
    }

    protected GroupBrowseListAdapter configAdapter() {
        return new GroupBrowseListAdapter(this.mContext);
    }

    protected AdapterView.OnItemClickListener configOnItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            }
        };
    }

    protected ListView getListView() {
        return this.mListView;
    }
}
