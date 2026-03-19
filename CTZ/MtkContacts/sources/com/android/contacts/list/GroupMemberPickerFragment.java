package com.android.contacts.list;

import android.app.Activity;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.android.contacts.activities.ContactSelectionActivity;
import com.android.contacts.group.GroupUtil;
import com.android.contacts.model.account.AccountWithDataSet;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class GroupMemberPickerFragment extends MultiSelectContactsListFragment<DefaultContactListAdapter> {
    private String mAccountDataSet;
    private String mAccountName;
    private String mAccountType;
    private ArrayList<String> mContactIds;
    private Listener mListener;
    private int mSubId = -1;

    public interface Listener {
        void onGroupMemberClicked(long j);

        void onSelectGroupMembers();
    }

    @Override
    public void onLoadFinished(Loader loader, Object obj) {
        onLoadFinished((Loader<Cursor>) loader, (Cursor) obj);
    }

    private class FilterCursorWrapper extends CursorWrapper {
        private int mCount;
        private int[] mIndex;
        private int mPos;

        public FilterCursorWrapper(Cursor cursor) {
            super(cursor);
            this.mCount = 0;
            this.mPos = 0;
            this.mCount = super.getCount();
            this.mIndex = new int[this.mCount];
            ArrayList arrayList = new ArrayList();
            if (Log.isLoggable("GroupMemberPicker", 2)) {
                Log.v("GroupMemberPicker", "RawContacts CursorWrapper start: " + this.mCount);
            }
            Bundle extras = cursor.getExtras();
            String[] stringArray = extras.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES");
            int[] intArray = extras.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS");
            ContactsSectionIndexer contactsSectionIndexer = (stringArray == null || intArray == null) ? null : new ContactsSectionIndexer(stringArray, intArray);
            for (int i = 0; i < this.mCount; i++) {
                super.moveToPosition(i);
                if (!GroupMemberPickerFragment.this.mContactIds.contains(getString(0))) {
                    int[] iArr = this.mIndex;
                    int i2 = this.mPos;
                    this.mPos = i2 + 1;
                    iArr[i2] = i;
                } else {
                    arrayList.add(Integer.valueOf(i));
                }
            }
            if (contactsSectionIndexer != null && GroupUtil.needTrimming(this.mCount, intArray, contactsSectionIndexer.getPositions())) {
                GroupUtil.updateBundle(extras, contactsSectionIndexer, arrayList, stringArray, intArray);
            }
            this.mCount = this.mPos;
            this.mPos = 0;
            super.moveToFirst();
            if (Log.isLoggable("GroupMemberPicker", 2)) {
                Log.v("GroupMemberPicker", "RawContacts CursorWrapper end: " + this.mCount);
            }
        }

        @Override
        public boolean move(int i) {
            return moveToPosition(this.mPos + i);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(this.mPos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(this.mPos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(this.mCount - 1);
        }

        @Override
        public boolean moveToPosition(int i) {
            if (i >= this.mCount) {
                this.mPos = this.mCount;
                return false;
            }
            if (i < 0) {
                this.mPos = -1;
                return false;
            }
            this.mPos = this.mIndex[i];
            return super.moveToPosition(this.mPos);
        }

        @Override
        public int getCount() {
            return this.mCount;
        }

        @Override
        public int getPosition() {
            return this.mPos;
        }
    }

    public static GroupMemberPickerFragment newInstance(String str, String str2, String str3, ArrayList<String> arrayList, int i) {
        Bundle bundle = new Bundle();
        bundle.putString(ContactSaveService.EXTRA_ACCOUNT_NAME, str);
        bundle.putString(ContactSaveService.EXTRA_ACCOUNT_TYPE, str2);
        bundle.putString("accountDataSet", str3);
        bundle.putStringArrayList(ContactSaveService.EXTRA_CONTACT_IDS, arrayList);
        bundle.putInt("accountSubId", i);
        GroupMemberPickerFragment groupMemberPickerFragment = new GroupMemberPickerFragment();
        groupMemberPickerFragment.setArguments(bundle);
        return groupMemberPickerFragment;
    }

    public GroupMemberPickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setHasOptionsMenu(true);
        setDisplayDirectoryHeader(false);
    }

    @Override
    public void onCreate(Bundle bundle) {
        if (bundle == null) {
            this.mAccountName = getArguments().getString(ContactSaveService.EXTRA_ACCOUNT_NAME);
            this.mAccountType = getArguments().getString(ContactSaveService.EXTRA_ACCOUNT_TYPE);
            this.mAccountDataSet = getArguments().getString("accountDataSet");
            this.mContactIds = getArguments().getStringArrayList(ContactSaveService.EXTRA_CONTACT_IDS);
            this.mSubId = getArguments().getInt("accountSubId");
        } else {
            this.mAccountName = bundle.getString(ContactSaveService.EXTRA_ACCOUNT_NAME);
            this.mAccountType = bundle.getString(ContactSaveService.EXTRA_ACCOUNT_TYPE);
            this.mAccountDataSet = bundle.getString("accountDataSet");
            this.mContactIds = bundle.getStringArrayList(ContactSaveService.EXTRA_CONTACT_IDS);
            this.mSubId = bundle.getInt("accountSubId");
        }
        super.onCreate(bundle);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(ContactSaveService.EXTRA_ACCOUNT_NAME, this.mAccountName);
        bundle.putString(ContactSaveService.EXTRA_ACCOUNT_TYPE, this.mAccountType);
        bundle.putString("accountDataSet", this.mAccountDataSet);
        bundle.putStringArrayList(ContactSaveService.EXTRA_CONTACT_IDS, this.mContactIds);
        bundle.putInt("accountSubId", this.mSubId);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            setVisibleScrollbarEnabled(true);
            FilterCursorWrapper filterCursorWrapper = new FilterCursorWrapper(cursor);
            bindListHeader(getContext(), getListView(), getView().findViewById(R.id.account_filter_header_container), new AccountWithDataSet(this.mAccountName, this.mAccountType, this.mAccountDataSet), filterCursorWrapper.getCount());
            super.onLoadFinished(loader, (Cursor) filterCursorWrapper);
        }
    }

    @Override
    protected DefaultContactListAdapter createListAdapter() {
        DefaultContactListAdapter defaultContactListAdapter = new DefaultContactListAdapter(getActivity());
        defaultContactListAdapter.setFilter(ContactListFilter.createGroupMembersFilter(this.mAccountType, this.mAccountName, this.mAccountDataSet));
        defaultContactListAdapter.setSectionHeaderDisplayEnabled(true);
        defaultContactListAdapter.setDisplayPhotos(true);
        return defaultContactListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        ContactSelectionActivity contactSelectionActivity = getContactSelectionActivity();
        boolean zIsSelectionMode = contactSelectionActivity == null ? false : contactSelectionActivity.isSelectionMode();
        if (zIsSelectionMode) {
            displayCheckBoxes(zIsSelectionMode);
        }
    }

    @Override
    protected void onItemClick(int i, long j) {
        if (((DefaultContactListAdapter) getAdapter()).isDisplayingCheckBoxes()) {
            super.onItemClick(i, j);
        } else if (this.mListener != null) {
            long contactId = ((DefaultContactListAdapter) getAdapter()).getContactId(i);
            if (contactId > 0) {
                this.mListener.onGroupMemberClicked(contactId);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu, menuInflater);
        menuInflater.inflate(R.menu.group_member_picker, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean zIsSearchMode;
        boolean zIsSelectionMode;
        ContactSelectionActivity contactSelectionActivity = getContactSelectionActivity();
        boolean z = this.mContactIds != null && this.mContactIds.size() > 0;
        if (contactSelectionActivity != null) {
            zIsSearchMode = contactSelectionActivity.isSearchMode();
        } else {
            zIsSearchMode = false;
        }
        if (contactSelectionActivity != null) {
            zIsSelectionMode = contactSelectionActivity.isSelectionMode();
        } else {
            zIsSelectionMode = false;
        }
        setVisible(menu, R.id.menu_search, (zIsSearchMode || zIsSelectionMode) ? false : true);
        setVisible(menu, R.id.menu_select, (!z || zIsSearchMode || zIsSelectionMode) ? false : true);
    }

    private ContactSelectionActivity getContactSelectionActivity() {
        Activity activity = getActivity();
        if (activity != null && (activity instanceof ContactSelectionActivity)) {
            return (ContactSelectionActivity) activity;
        }
        return null;
    }

    private static void setVisible(Menu menu, int i, boolean z) {
        MenuItem menuItemFindItem = menu.findItem(i);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(z);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 16908332) {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
            return true;
        }
        if (itemId == R.id.menu_select) {
            if (this.mListener != null) {
                this.mListener.onSelectGroupMembers();
            }
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public int getSubId() {
        return this.mSubId;
    }
}
