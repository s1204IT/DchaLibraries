package com.android.contacts.group;

import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.list.ContactTileAdapter;
import com.android.contacts.common.list.ContactTileView;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.GroupMemberTileAdapter;

public class GroupDetailFragment extends Fragment implements AbsListView.OnScrollListener {
    private AccountTypeManager mAccountTypeManager;
    private String mAccountTypeString;
    private ContactTileAdapter mAdapter;
    private boolean mCloseActivityAfterDelete;
    private Context mContext;
    private String mDataSet;
    private View mEmptyView;
    private long mGroupId;
    private String mGroupName;
    private TextView mGroupSize;
    private View mGroupSourceView;
    private ViewGroup mGroupSourceViewContainer;
    private TextView mGroupTitle;
    private Uri mGroupUri;
    private boolean mIsMembershipEditable;
    private boolean mIsReadOnly;
    private Listener mListener;
    private ListView mMemberListView;
    private boolean mOptionsMenuGroupDeletable;
    private boolean mOptionsMenuGroupEditable;
    private ContactPhotoManager mPhotoManager;
    private View mRootView;
    private boolean mShowGroupActionInActionBar;
    private final ContactTileView.Listener mContactTileListener = new ContactTileView.Listener() {
        @Override
        public void onContactSelected(Uri contactUri, Rect targetRect) {
            GroupDetailFragment.this.mListener.onContactSelected(contactUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Log.w("GroupDetailFragment", "unexpected invocation of onCallNumberDirectly()");
        }

        @Override
        public int getApproximateTileWidth() {
            return GroupDetailFragment.this.getView().getWidth() / GroupDetailFragment.this.mAdapter.getColumnCount();
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetadataLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            return new GroupMetaDataLoader(GroupDetailFragment.this.mContext, GroupDetailFragment.this.mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data == null || data.isClosed()) {
                Log.e("GroupDetailFragment", "Failed to load group metadata");
                return;
            }
            data.moveToPosition(-1);
            if (data.moveToNext()) {
                boolean deleted = data.getInt(8) == 1;
                if (!deleted) {
                    GroupDetailFragment.this.bindGroupMetaData(data);
                    GroupDetailFragment.this.startGroupMembersLoader();
                    return;
                }
            }
            GroupDetailFragment.this.updateSize(-1);
            GroupDetailFragment.this.updateTitle(null);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupDetailQuery(GroupDetailFragment.this.mContext, GroupDetailFragment.this.mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && !data.isClosed()) {
                GroupDetailFragment.this.updateSize(data.getCount());
                GroupDetailFragment.this.mAdapter.setContactCursor(data);
                GroupDetailFragment.this.mMemberListView.setEmptyView(GroupDetailFragment.this.mEmptyView);
                return;
            }
            Log.e("GroupDetailFragment", "Failed to load group members");
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    public interface Listener {
        void onAccountTypeUpdated(String str, String str2);

        void onContactSelected(Uri uri);

        void onEditRequested(Uri uri);

        void onGroupSizeUpdated(String str);

        void onGroupTitleUpdated(String str);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mAccountTypeManager = AccountTypeManager.getInstance(this.mContext);
        Resources res = getResources();
        int columnCount = res.getInteger(R.integer.contact_tile_column_count);
        this.mAdapter = new GroupMemberTileAdapter(activity, this.mContactTileListener, columnCount);
        configurePhotoLoader();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mContext = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        this.mRootView = inflater.inflate(R.layout.group_detail_fragment, container, false);
        this.mGroupTitle = (TextView) this.mRootView.findViewById(R.id.group_title);
        this.mGroupSize = (TextView) this.mRootView.findViewById(R.id.group_size);
        this.mGroupSourceViewContainer = (ViewGroup) this.mRootView.findViewById(R.id.group_source_view_container);
        this.mEmptyView = this.mRootView.findViewById(android.R.id.empty);
        this.mMemberListView = (ListView) this.mRootView.findViewById(android.R.id.list);
        this.mMemberListView.setItemsCanFocus(true);
        this.mMemberListView.setAdapter((ListAdapter) this.mAdapter);
        return this.mRootView;
    }

    public void loadGroup(Uri groupUri) {
        this.mGroupUri = groupUri;
        startGroupMetadataLoader();
    }

    private void configurePhotoLoader() {
        if (this.mContext != null) {
            if (this.mPhotoManager == null) {
                this.mPhotoManager = ContactPhotoManager.getInstance(this.mContext);
            }
            if (this.mMemberListView != null) {
                this.mMemberListView.setOnScrollListener(this);
            }
            if (this.mAdapter != null) {
                this.mAdapter.setPhotoLoader(this.mPhotoManager);
            }
        }
    }

    public void setListener(Listener value) {
        this.mListener = value;
    }

    public void setShowGroupSourceInActionBar(boolean show) {
        this.mShowGroupActionInActionBar = show;
    }

    private void startGroupMetadataLoader() {
        getLoaderManager().restartLoader(0, null, this.mGroupMetadataLoaderListener);
    }

    private void startGroupMembersLoader() {
        getLoaderManager().restartLoader(1, null, this.mGroupMemberListLoaderListener);
    }

    private void bindGroupMetaData(Cursor cursor) {
        cursor.moveToPosition(-1);
        if (cursor.moveToNext()) {
            this.mAccountTypeString = cursor.getString(1);
            this.mDataSet = cursor.getString(2);
            this.mGroupId = cursor.getLong(3);
            this.mGroupName = cursor.getString(4);
            this.mIsReadOnly = cursor.getInt(7) == 1;
            updateTitle(this.mGroupName);
            getActivity().invalidateOptionsMenu();
            String accountTypeString = cursor.getString(1);
            String dataSet = cursor.getString(2);
            updateAccountType(accountTypeString, dataSet);
        }
    }

    private void updateTitle(String title) {
        if (this.mGroupTitle != null) {
            this.mGroupTitle.setText(title);
        } else {
            this.mListener.onGroupTitleUpdated(title);
        }
    }

    private void updateSize(int size) {
        String groupSizeString;
        if (size == -1) {
            groupSizeString = null;
        } else {
            AccountType accountType = this.mAccountTypeManager.getAccountType(this.mAccountTypeString, this.mDataSet);
            CharSequence dispLabel = accountType.getDisplayLabel(this.mContext);
            if (!TextUtils.isEmpty(dispLabel)) {
                String groupSizeTemplateString = getResources().getQuantityString(R.plurals.num_contacts_in_group, size);
                groupSizeString = String.format(groupSizeTemplateString, Integer.valueOf(size), dispLabel);
            } else {
                String groupSizeTemplateString2 = getResources().getQuantityString(R.plurals.group_list_num_contacts_in_group, size);
                groupSizeString = String.format(groupSizeTemplateString2, Integer.valueOf(size));
            }
        }
        if (this.mGroupSize != null) {
            this.mGroupSize.setText(groupSizeString);
        } else {
            this.mListener.onGroupSizeUpdated(groupSizeString);
        }
    }

    private void updateAccountType(String accountTypeString, String dataSet) {
        AccountTypeManager manager = AccountTypeManager.getInstance(getActivity());
        final AccountType accountType = manager.getAccountType(accountTypeString, dataSet);
        this.mIsMembershipEditable = accountType.isGroupMembershipEditable();
        if (this.mShowGroupActionInActionBar) {
            this.mListener.onAccountTypeUpdated(accountTypeString, dataSet);
            return;
        }
        if (!TextUtils.isEmpty(accountType.getViewGroupActivity())) {
            if (this.mGroupSourceView == null) {
                this.mGroupSourceView = GroupDetailDisplayUtils.getNewGroupSourceView(this.mContext);
                if (this.mGroupSourceViewContainer != null) {
                    this.mGroupSourceViewContainer.addView(this.mGroupSourceView);
                }
            }
            this.mGroupSourceView.setVisibility(0);
            GroupDetailDisplayUtils.bindGroupSourceView(this.mContext, this.mGroupSourceView, accountTypeString, dataSet);
            this.mGroupSourceView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, GroupDetailFragment.this.mGroupId);
                    Intent intent = new Intent("android.intent.action.VIEW", uri);
                    intent.setClassName(accountType.syncAdapterPackageName, accountType.getViewGroupActivity());
                    try {
                        GroupDetailFragment.this.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Log.e("GroupDetailFragment", "startActivity() failed: " + e);
                        Toast.makeText(GroupDetailFragment.this.getActivity(), R.string.missing_app, 0).show();
                    }
                }
            });
            return;
        }
        if (this.mGroupSourceView != null) {
            this.mGroupSourceView.setVisibility(8);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == 2) {
            this.mPhotoManager.pause();
        } else {
            this.mPhotoManager.resume();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_group, menu);
    }

    public boolean isGroupDeletable() {
        return (this.mGroupUri == null || this.mIsReadOnly) ? false : true;
    }

    public boolean isGroupEditableAndPresent() {
        return this.mGroupUri != null && this.mIsMembershipEditable;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.mOptionsMenuGroupDeletable = isGroupDeletable() && isVisible();
        this.mOptionsMenuGroupEditable = isGroupEditableAndPresent() && isVisible();
        MenuItem editMenu = menu.findItem(R.id.menu_edit_group);
        editMenu.setVisible(this.mOptionsMenuGroupEditable);
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_group);
        deleteMenu.setVisible(this.mOptionsMenuGroupDeletable);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit_group:
                if (this.mListener != null) {
                    this.mListener.onEditRequested(this.mGroupUri);
                }
                return false;
            case R.id.menu_delete_group:
                GroupDeletionDialogFragment.show(getFragmentManager(), this.mGroupId, this.mGroupName, this.mCloseActivityAfterDelete);
                return true;
            default:
                return false;
        }
    }

    public void closeActivityAfterDelete(boolean closeActivity) {
        this.mCloseActivityAfterDelete = closeActivity;
    }

    public long getGroupId() {
        return this.mGroupId;
    }
}
