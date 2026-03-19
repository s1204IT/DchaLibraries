package com.android.contacts.group;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsUtils;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.ActionBarAdapter;
import com.android.contacts.activities.PeopleActivity;
import com.android.contacts.interactions.GroupDeletionDialogFragment;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.list.ContactsSectionIndexer;
import com.android.contacts.list.MultiSelectContactsListFragment;
import com.android.contacts.list.MultiSelectEntryContactListAdapter;
import com.android.contacts.logging.Logger;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.ImplicitIntentsUtil;
import com.android.contactsbind.FeedbackHelper;
import com.google.common.primitives.Longs;
import com.mediatek.contacts.group.SimGroupUtils;
import com.mediatek.contacts.group.UpdateSimGroupMembersAsyncTask;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.contacts.util.MtkToast;
import com.mediatek.contacts.util.ProgressHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GroupMembersFragment extends MultiSelectContactsListFragment<GroupMembersAdapter> implements ContactSaveService.DeleteEndListener {
    private ActionBarAdapter mActionBarAdapter;
    private PeopleActivity mActivity;
    private boolean mDisableOptionItemSelected;
    private GroupMetaData mGroupMetaData;
    private Uri mGroupUri;
    private boolean mIsEditMode;
    private int mSubId = SubInfoUtils.getInvalidSubId();
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataCallbacks = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int i, Bundle bundle) {
            return new GroupMetaDataLoader(GroupMembersFragment.this.mActivity, GroupMembersFragment.this.mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor == null || cursor.isClosed() || !cursor.moveToNext()) {
                Log.e("GroupMembers", "Failed to load group metadata for " + GroupMembersFragment.this.mGroupUri);
                Toast.makeText(GroupMembersFragment.this.getContext(), R.string.groupLoadErrorToast, 0).show();
                GroupMembersFragment.this.mHandler.sendEmptyMessage(1);
                return;
            }
            GroupMembersFragment.this.mGroupMetaData = new GroupMetaData(GroupMembersFragment.this.getActivity(), cursor);
            GroupMembersFragment.this.mSubId = AccountTypeUtils.getSubIdBySimAccountName(GroupMembersFragment.this.getActivity(), GroupMembersFragment.this.mGroupMetaData.accountName);
            Log.d("GroupMembers", "[onLoadFinished] mSubId is " + GroupMembersFragment.this.mSubId);
            GroupMembersFragment.this.onGroupMetadataLoaded();
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private Set<String> mGroupMemberContactIds = new HashSet();
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                if (GroupMembersFragment.this.mActivity.isGroupView()) {
                    GroupMembersFragment.this.mActivity.onBackPressed();
                } else {
                    Log.d("GroupMembers", "[handleMessage] not group view, igore msg.");
                }
            }
        }
    };
    private final ActionBarAdapter.Listener mActionBarListener = new ActionBarAdapter.Listener() {
        @Override
        public void onAction(int i) {
            switch (i) {
                case 2:
                    if (GroupMembersFragment.this.mIsEditMode) {
                        GroupMembersFragment.this.displayDeleteButtons(true);
                        GroupMembersFragment.this.mActionBarAdapter.setActionBarTitle(GroupMembersFragment.this.getString(R.string.title_edit_group));
                    } else {
                        GroupMembersFragment.this.displayCheckBoxes(true);
                    }
                    GroupMembersFragment.this.mActivity.invalidateOptionsMenu();
                    break;
                case 3:
                    GroupMembersFragment.this.mActionBarAdapter.setSearchMode(false);
                    if (GroupMembersFragment.this.mIsEditMode) {
                        GroupMembersFragment.this.mIsEditMode = false;
                        GroupMembersFragment.this.displayDeleteButtons(false);
                    } else {
                        GroupMembersFragment.this.displayCheckBoxes(false);
                    }
                    GroupMembersFragment.this.mActivity.invalidateOptionsMenu();
                    break;
            }
        }

        @Override
        public void onUpButtonPressed() {
            GroupMembersFragment.this.mActivity.onBackPressed();
        }
    };
    private final MultiSelectContactsListFragment.OnCheckBoxListActionListener mCheckBoxListener = new MultiSelectContactsListFragment.OnCheckBoxListActionListener() {
        @Override
        public void onStartDisplayingCheckBoxes() {
            GroupMembersFragment.this.mActionBarAdapter.setSelectionMode(true);
        }

        @Override
        public void onSelectedContactIdsChanged() {
            if (GroupMembersFragment.this.mActionBarAdapter != null) {
                if (GroupMembersFragment.this.mIsEditMode) {
                    GroupMembersFragment.this.mActionBarAdapter.setActionBarTitle(GroupMembersFragment.this.getString(R.string.title_edit_group));
                } else {
                    GroupMembersFragment.this.mActionBarAdapter.setSelectionCount(GroupMembersFragment.this.getSelectedContactIds().size());
                }
            }
        }

        @Override
        public void onStopDisplayingCheckBoxes() {
            GroupMembersFragment.this.mActionBarAdapter.setSelectionMode(false);
        }
    };
    private ProgressHandler mProgressHandler = new ProgressHandler();

    public static abstract class Query {
        public static final String[] EMAIL_PROJECTION = {"contact_id", "_id", "is_super_primary", "times_used", "data1"};
        public static final String[] PHONE_PROJECTION = {"contact_id", "_id", "is_super_primary", "times_used", "data1"};
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
            if (Log.isLoggable("GroupMembers", 2)) {
                Log.v("GroupMembers", "Group members CursorWrapper start: " + this.mCount);
            }
            Bundle extras = cursor.getExtras();
            String[] stringArray = extras.getStringArray("android.provider.extra.ADDRESS_BOOK_INDEX_TITLES");
            int[] intArray = extras.getIntArray("android.provider.extra.ADDRESS_BOOK_INDEX_COUNTS");
            ContactsSectionIndexer contactsSectionIndexer = (stringArray == null || intArray == null) ? null : new ContactsSectionIndexer(stringArray, intArray);
            GroupMembersFragment.this.mGroupMemberContactIds.clear();
            for (int i = 0; i < this.mCount; i++) {
                super.moveToPosition(i);
                String string = getString(0);
                if (!GroupMembersFragment.this.mGroupMemberContactIds.contains(string)) {
                    int[] iArr = this.mIndex;
                    int i2 = this.mPos;
                    this.mPos = i2 + 1;
                    iArr[i2] = i;
                    GroupMembersFragment.this.mGroupMemberContactIds.add(string);
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
            if (Log.isLoggable("GroupMembers", 2)) {
                Log.v("GroupMembers", "Group members CursorWrapper end: " + this.mCount);
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

    public static GroupMembersFragment newInstance(Uri uri) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("groupUri", uri);
        GroupMembersFragment groupMembersFragment = new GroupMembersFragment();
        groupMembersFragment.setArguments(bundle);
        return groupMembersFragment;
    }

    public GroupMembersFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setHasOptionsMenu(true);
        setListType(3);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        if (this.mGroupMetaData == null) {
            Log.e("GroupMembers", "[onCreateOptionsMenu]mGroupMetaData == null !!!");
        } else {
            menuInflater.inflate(R.menu.view_group, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        this.mOptionMenu = menu;
        boolean zIsSelectionMode = this.mActionBarAdapter.isSelectionMode();
        boolean z = false;
        boolean z2 = this.mGroupMetaData != null && this.mGroupMetaData.editable;
        boolean z3 = this.mGroupMetaData != null && this.mGroupMetaData.readOnly;
        setVisible(getContext(), menu, R.id.menu_multi_send_email, (this.mIsEditMode || isGroupEmpty()) ? false : true);
        setVisible(getContext(), menu, R.id.menu_multi_send_message, (this.mIsEditMode || isGroupEmpty()) ? false : true);
        setVisible(getContext(), menu, R.id.menu_add, z2 && !zIsSelectionMode);
        setVisible(getContext(), menu, R.id.menu_rename_group, (z3 || zIsSelectionMode) ? false : true);
        setVisible(getContext(), menu, R.id.menu_delete_group, (z3 || zIsSelectionMode) ? false : true);
        setVisible(getContext(), menu, R.id.menu_edit_group, (!z2 || this.mIsEditMode || zIsSelectionMode || isGroupEmpty()) ? false : true);
        Context context = getContext();
        if (z2 && zIsSelectionMode && !this.mIsEditMode) {
            z = true;
        }
        setVisible(context, menu, R.id.menu_remove_from_group, z);
    }

    private boolean isGroupEmpty() {
        return getAdapter() != 0 && ((GroupMembersAdapter) getAdapter()).isEmpty();
    }

    private static void setVisible(Context context, Menu menu, int i, boolean z) {
        MenuItem menuItemFindItem = menu.findItem(i);
        if (menuItemFindItem != null) {
            menuItemFindItem.setVisible(z);
            Drawable icon = menuItemFindItem.getIcon();
            if (icon != null) {
                icon.mutate().setColorFilter(ContextCompat.getColor(context, R.color.actionbar_icon_color), PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    private class ContactDataHelperClass {
        private List<String> items;
        private String mostUsedItemId;
        private int mostUsedTimes;
        private String primaryItemId;

        private ContactDataHelperClass() {
            this.items = new ArrayList();
            this.mostUsedItemId = null;
            this.primaryItemId = null;
        }

        public void addItem(String str, int i, boolean z) {
            if (this.mostUsedItemId == null || i > this.mostUsedTimes) {
                this.mostUsedItemId = str;
                this.mostUsedTimes = i;
            }
            if (z) {
                this.primaryItemId = str;
            }
            this.items.add(str);
        }

        public boolean hasDefaultItem() {
            return this.primaryItemId != null || this.items.size() == 1;
        }

        public String getDefaultSelectionItemId() {
            if (this.primaryItemId != null) {
                return this.primaryItemId;
            }
            return this.mostUsedItemId;
        }
    }

    private void sendToGroup(long[] jArr, String str, String str2) {
        String str3;
        String string;
        ContactDataHelperClass contactDataHelperClass;
        if (jArr == null || jArr.length == 0) {
            return;
        }
        HashMap map = new HashMap();
        ArrayList arrayList = new ArrayList();
        String strConvertArrayToString = GroupUtil.convertArrayToString(jArr);
        StringBuilder sb = new StringBuilder();
        if (ContactsUtils.SCHEME_MAILTO.equals(str)) {
            str3 = "mimetype='vnd.android.cursor.item/email_v2'";
        } else {
            str3 = "mimetype='vnd.android.cursor.item/phone_v2'";
        }
        sb.append(str3);
        sb.append(" AND ");
        sb.append("contact_id");
        sb.append(" IN (");
        sb.append(strConvertArrayToString);
        sb.append(")");
        Cursor cursorQuery = getContext().getContentResolver().query(ContactsContract.Data.CONTENT_URI, ContactsUtils.SCHEME_MAILTO.equals(str) ? Query.EMAIL_PROJECTION : Query.PHONE_PROJECTION, sb.toString(), null, null);
        if (cursorQuery == null) {
            return;
        }
        try {
            cursorQuery.moveToPosition(-1);
            while (cursorQuery.moveToNext()) {
                boolean z = false;
                String string2 = cursorQuery.getString(0);
                String string3 = cursorQuery.getString(1);
                if (cursorQuery.getInt(2) != 0) {
                    z = true;
                }
                int i = cursorQuery.getInt(3);
                String string4 = cursorQuery.getString(4);
                if (!TextUtils.isEmpty(string4)) {
                    if (!map.containsKey(string2)) {
                        contactDataHelperClass = new ContactDataHelperClass();
                        map.put(string2, contactDataHelperClass);
                    } else {
                        contactDataHelperClass = (ContactDataHelperClass) map.get(string2);
                    }
                    contactDataHelperClass.addItem(string3, i, z);
                    arrayList.add(string4);
                }
            }
            cursorQuery.close();
            Iterator it = map.values().iterator();
            while (it.hasNext()) {
                if (!((ContactDataHelperClass) it.next()).hasDefaultItem()) {
                    ArrayList arrayList2 = new ArrayList();
                    Iterator it2 = map.values().iterator();
                    while (it2.hasNext()) {
                        String defaultSelectionItemId = ((ContactDataHelperClass) it2.next()).getDefaultSelectionItemId();
                        if (defaultSelectionItemId != null) {
                            arrayList2.add(Long.valueOf(Long.parseLong(defaultSelectionItemId)));
                        }
                    }
                    startSendToSelectionPickerActivity(jArr, Longs.toArray(arrayList2), str, str2);
                    return;
                }
            }
            if (arrayList.size() == 0 || map.size() < jArr.length) {
                Context context = getContext();
                if (ContactsUtils.SCHEME_MAILTO.equals(str)) {
                    string = getString(R.string.groupSomeContactsNoEmailsToast);
                } else {
                    string = getString(R.string.groupSomeContactsNoPhonesToast);
                }
                Toast.makeText(context, string, 1).show();
            }
            if (arrayList.size() == 0) {
                return;
            }
            GroupUtil.startSendToSelectionActivity(this, TextUtils.join(",", arrayList), str, str2);
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
    }

    private void startSendToSelectionPickerActivity(long[] jArr, long[] jArr2, String str, String str2) {
        startActivity(GroupUtil.createSendToSelectionPickerIntent(getContext(), jArr, jArr2, str, str2, this.mSubId));
    }

    private void startGroupAddMemberActivity() {
        startActivityForResult(GroupUtil.createPickMemberIntent(getContext(), this.mGroupMetaData, getMemberContactIds(), this.mSubId), 100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        long[] jArrConvertStringSetToLongArray;
        long[] jArrConvertStringSetToLongArray2;
        Log.d("GroupMembers", "[onOptionsItemSelected] item = " + ((Object) menuItem.getTitle()) + ", mDisableOptionItemSelected = " + this.mDisableOptionItemSelected);
        if (this.mDisableOptionItemSelected) {
            return false;
        }
        int itemId = menuItem.getItemId();
        if (this.mGroupMetaData == null) {
            Log.e("GroupMembers", "[onOptionsItemSelected]mGroupMetaData invalid=" + this.mGroupMetaData);
            if (itemId == R.id.menu_add || itemId == R.id.menu_rename_group || itemId == R.id.menu_remove_from_group) {
                return false;
            }
        }
        if (itemId == 16908332) {
            this.mActivity.onBackPressed();
        } else if (itemId == R.id.menu_add) {
            startGroupAddMemberActivity();
        } else if (itemId == R.id.menu_multi_send_email) {
            if (this.mActionBarAdapter.isSelectionMode()) {
                jArrConvertStringSetToLongArray2 = ((GroupMembersAdapter) getAdapter()).getSelectedContactIdsArray();
            } else {
                jArrConvertStringSetToLongArray2 = GroupUtil.convertStringSetToLongArray(this.mGroupMemberContactIds);
            }
            sendToGroup(jArrConvertStringSetToLongArray2, ContactsUtils.SCHEME_MAILTO, getString(R.string.menu_sendEmailOption));
        } else if (itemId == R.id.menu_multi_send_message) {
            if (this.mActionBarAdapter.isSelectionMode()) {
                jArrConvertStringSetToLongArray = ((GroupMembersAdapter) getAdapter()).getSelectedContactIdsArray();
            } else {
                jArrConvertStringSetToLongArray = GroupUtil.convertStringSetToLongArray(this.mGroupMemberContactIds);
            }
            sendToGroup(jArrConvertStringSetToLongArray, ContactsUtils.SCHEME_SMSTO, getString(R.string.menu_sendMessageOption));
        } else if (itemId == R.id.menu_rename_group) {
            GroupNameEditDialogFragment.newInstanceForUpdate(new AccountWithDataSet(this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet), "updateGroup", this.mGroupMetaData.groupId, this.mGroupMetaData.groupName).show(getFragmentManager(), "groupNameEditDialog");
        } else if (itemId == R.id.menu_delete_group) {
            deleteGroup();
        } else if (itemId == R.id.menu_edit_group) {
            this.mIsEditMode = true;
            this.mActionBarAdapter.setSelectionMode(true);
            displayDeleteButtons(true);
        } else if (itemId == R.id.menu_remove_from_group) {
            logListEvent();
            removeSelectedContacts();
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        return true;
    }

    private void removeSelectedContacts() {
        if (!SimGroupUtils.checkServiceState(true, this.mSubId, getActivity())) {
            return;
        }
        long[] selectedContactIdsArray = ((GroupMembersAdapter) getAdapter()).getSelectedContactIdsArray();
        if (this.mSubId > 0) {
            new UpdateSimGroupMembersAsyncTask(1, getContext(), selectedContactIdsArray, this.mGroupMetaData.groupId, this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet, this.mGroupMetaData.groupName, this.mSubId).execute(new Void[0]);
        } else {
            new UpdateGroupMembersAsyncTask(1, getContext(), selectedContactIdsArray, this.mGroupMetaData.groupId, this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet).execute(new Void[0]);
        }
        this.mActionBarAdapter.setSelectionMode(false);
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i2 == -1 && intent != null && i == 100 && SimGroupUtils.checkServiceState(true, this.mSubId, getActivity())) {
            long[] longArrayExtra = intent.getLongArrayExtra("com.android.contacts.action.CONTACT_IDS");
            if (longArrayExtra == null) {
                long longExtra = intent.getLongExtra("com.android.contacts.action.CONTACT_ID", -1L);
                if (longExtra > -1) {
                    longArrayExtra = new long[]{longExtra};
                }
            }
            long[] jArr = longArrayExtra;
            if (this.mSubId > 0) {
                new UpdateSimGroupMembersAsyncTask(0, getContext(), jArr, this.mGroupMetaData.groupId, this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet, this.mGroupMetaData.groupName, this.mSubId).execute(new Void[0]);
            } else {
                new UpdateGroupMembersAsyncTask(0, getContext(), jArr, this.mGroupMetaData.groupId, this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet).execute(new Void[0]);
            }
        }
    }

    private void logListEvent() {
        Logger.logListEvent(7, getListType(), ((GroupMembersAdapter) getAdapter()).getCount(), -1, ((GroupMembersAdapter) getAdapter()).getSelectedContactIdsArray().length);
    }

    private void deleteGroup() {
        if (!SimGroupUtils.checkServiceState(true, this.mSubId, getActivity())) {
            return;
        }
        if (getMemberCount() == 0) {
            if (this.mSubId > 0) {
                getContext().startService(SimGroupUtils.createGroupDeletionIntentForIcc(getContext(), this.mGroupMetaData.groupId, this.mSubId, this.mGroupMetaData.groupName));
                return;
            } else {
                getContext().startService(ContactSaveService.createGroupDeletionIntent(getContext(), this.mGroupMetaData.groupId));
                return;
            }
        }
        GroupDeletionDialogFragment.show(getFragmentManager(), this.mGroupMetaData.groupId, this.mGroupMetaData.groupName, this.mSubId);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mActivity = (PeopleActivity) getActivity();
        this.mActionBarAdapter = new ActionBarAdapter(this.mActivity, this.mActionBarListener, this.mActivity.getSupportActionBar(), this.mActivity.getToolbar(), R.string.enter_contact_name, this);
        this.mActionBarAdapter.setShowHomeIcon(true);
        ContactsRequest contactsRequest = new ContactsRequest();
        contactsRequest.setActionCode(20);
        this.mActionBarAdapter.initialize(bundle, contactsRequest);
        if (this.mGroupMetaData != null) {
            this.mActivity.setTitle(this.mGroupMetaData.groupName);
            if (this.mGroupMetaData.editable) {
                setCheckBoxListListener(this.mCheckBoxListener);
            }
        }
    }

    @Override
    public ActionBarAdapter getActionBarAdapter() {
        return this.mActionBarAdapter;
    }

    public void displayDeleteButtons(boolean z) {
        ((GroupMembersAdapter) getAdapter()).setDisplayDeleteButtons(z);
    }

    public ArrayList<String> getMemberContactIds() {
        return new ArrayList<>(this.mGroupMemberContactIds);
    }

    public int getMemberCount() {
        return this.mGroupMemberContactIds.size();
    }

    public boolean isEditMode() {
        return this.mIsEditMode;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if (bundle == null) {
            this.mGroupUri = (Uri) getArguments().getParcelable("groupUri");
        } else {
            this.mIsEditMode = bundle.getBoolean("editMode");
            this.mGroupUri = (Uri) bundle.getParcelable("groupUri");
            this.mGroupMetaData = (GroupMetaData) bundle.getParcelable("groupMetadata");
            this.mSubId = bundle.getInt("subId");
        }
        Log.d("GroupMembers", "[onCreate] mGroupUri is " + this.mGroupUri + ", mSubId is " + this.mSubId);
        maybeAttachCheckBoxListener();
        ContactSaveService.setDeleteEndListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mActionBarAdapter.setListener(this.mActionBarListener);
        this.mDisableOptionItemSelected = false;
    }

    @Override
    protected void startLoading() {
        Log.d("GroupMembers", "[startLoading] mGroupMetaData = " + this.mGroupMetaData);
        if (this.mGroupMetaData == null || !this.mGroupMetaData.isValid()) {
            getLoaderManager().restartLoader(100, null, this.mGroupMetaDataCallbacks);
        } else {
            onGroupMetadataLoaded();
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            setVisibleScrollbarEnabled(true);
            FilterCursorWrapper filterCursorWrapper = new FilterCursorWrapper(cursor);
            bindMembersCount(filterCursorWrapper.getCount());
            super.onLoadFinished(loader, (Cursor) filterCursorWrapper);
            this.mActivity.invalidateOptionsMenu();
            this.mActionBarAdapter.updateOverflowButtonColor();
            if (!isInactive() && isEditMode() && getGroupCount() == 0) {
                exitEditMode();
            }
        }
    }

    private void bindMembersCount(int i) {
        View viewFindViewById = getView().findViewById(R.id.account_filter_header_container);
        View viewFindViewById2 = getView().findViewById(R.id.empty_group);
        if (i > 0) {
            if (this.mGroupMetaData != null) {
                bindListHeader(getContext(), getListView(), viewFindViewById, new AccountWithDataSet(this.mGroupMetaData.accountName, this.mGroupMetaData.accountType, this.mGroupMetaData.dataSet), i);
            } else {
                Log.e("GroupMembers", "[bindMembersCount] mGroupMetaData is null and ignore !!!");
            }
            viewFindViewById2.setVisibility(8);
            return;
        }
        hideHeaderAndAddPadding(getContext(), getListView(), viewFindViewById);
        viewFindViewById2.setVisibility(0);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d("GroupMembers", "[onSaveInstanceState] mSubId = " + this.mSubId);
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.setListener(null);
            this.mActionBarAdapter.onSaveInstanceState(bundle);
        }
        this.mDisableOptionItemSelected = true;
        bundle.putBoolean("editMode", this.mIsEditMode);
        bundle.putParcelable("groupUri", this.mGroupUri);
        bundle.putParcelable("groupMetadata", this.mGroupMetaData);
        bundle.putInt("subId", this.mSubId);
    }

    private void onGroupMetadataLoaded() {
        if (Log.isLoggable("GroupMembers", 2)) {
            Log.v("GroupMembers", "Loaded " + this.mGroupMetaData);
        }
        maybeAttachCheckBoxListener();
        this.mActivity.setTitle(this.mGroupMetaData.groupName);
        this.mActivity.invalidateOptionsMenu();
        this.mActivity.updateDrawerGroupMenu(this.mGroupMetaData.groupId);
        super.startLoading();
    }

    private void maybeAttachCheckBoxListener() {
        if (this.mGroupMetaData != null && this.mGroupMetaData.editable) {
            setCheckBoxListListener(this.mCheckBoxListener);
        }
    }

    @Override
    protected GroupMembersAdapter createListAdapter() {
        GroupMembersAdapter groupMembersAdapter = new GroupMembersAdapter(getContext());
        groupMembersAdapter.setSectionHeaderDisplayEnabled(true);
        groupMembersAdapter.setDisplayPhotos(true);
        groupMembersAdapter.setDeleteContactListener(new DeletionListener());
        return groupMembersAdapter;
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        if (this.mGroupMetaData != null) {
            if (((GroupMembersAdapter) getAdapter()).getGroupId() != this.mGroupMetaData.groupId) {
                ((GroupMembersAdapter) getAdapter()).onDataReload();
            }
            ((GroupMembersAdapter) getAdapter()).setGroupId(this.mGroupMetaData.groupId);
        }
    }

    @Override
    protected View inflateView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        View viewInflate = layoutInflater.inflate(R.layout.contact_list_content, (ViewGroup) null);
        View viewInflate2 = layoutInflater.inflate(R.layout.empty_group_view, (ViewGroup) null);
        ImageView imageView = (ImageView) viewInflate2.findViewById(R.id.empty_group_image);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.setMargins(0, getResources().getDisplayMetrics().heightPixels / getResources().getInteger(R.integer.empty_group_view_image_margin_divisor), 0, 0);
        layoutParams.gravity = 1;
        imageView.setLayoutParams(layoutParams);
        ((FrameLayout) viewInflate.findViewById(R.id.contact_list)).addView(viewInflate2);
        ((Button) viewInflate2.findViewById(R.id.add_member_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GroupMembersFragment.this.startActivityForResult(GroupUtil.createPickMemberIntent(GroupMembersFragment.this.getContext(), GroupMembersFragment.this.mGroupMetaData, GroupMembersFragment.this.getMemberContactIds(), GroupMembersFragment.this.mSubId), 100);
            }
        });
        return viewInflate;
    }

    @Override
    protected void onItemClick(int i, long j) {
        Uri contactUri = ((GroupMembersAdapter) getAdapter()).getContactUri(i);
        if (contactUri == null) {
            return;
        }
        if (((GroupMembersAdapter) getAdapter()).isDisplayingCheckBoxes()) {
            super.onItemClick(i, j);
        } else {
            Logger.logListEvent(2, 3, ((GroupMembersAdapter) getAdapter()).getCount(), i, 0);
            ImplicitIntentsUtil.startQuickContact(getActivity(), contactUri, 9);
        }
    }

    @Override
    protected boolean onItemLongClick(int i, long j) {
        if (this.mActivity != null && this.mIsEditMode) {
            return true;
        }
        return super.onItemLongClick(i, j);
    }

    private final class DeletionListener implements MultiSelectEntryContactListAdapter.DeleteContactListener {
        private DeletionListener() {
        }

        @Override
        public void onContactDeleteClicked(int i) {
            if (!SimGroupUtils.checkServiceState(true, GroupMembersFragment.this.mSubId, GroupMembersFragment.this.getActivity())) {
                return;
            }
            try {
                long[] jArr = {((GroupMembersAdapter) GroupMembersFragment.this.getAdapter()).getContactId(i)};
                if (GroupMembersFragment.this.mSubId > 0) {
                    new UpdateSimGroupMembersAsyncTask(1, GroupMembersFragment.this.getContext(), jArr, GroupMembersFragment.this.mGroupMetaData.groupId, GroupMembersFragment.this.mGroupMetaData.accountName, GroupMembersFragment.this.mGroupMetaData.accountType, GroupMembersFragment.this.mGroupMetaData.dataSet, GroupMembersFragment.this.mGroupMetaData.groupName, GroupMembersFragment.this.mSubId).execute(new Void[0]);
                } else {
                    new UpdateGroupMembersAsyncTask(1, GroupMembersFragment.this.getContext(), jArr, GroupMembersFragment.this.mGroupMetaData.groupId, GroupMembersFragment.this.mGroupMetaData.accountName, GroupMembersFragment.this.mGroupMetaData.accountType, GroupMembersFragment.this.mGroupMetaData.dataSet).execute(new Void[0]);
                }
            } catch (Exception e) {
                Log.e("GroupMembers", "[onContactDeleteClicked]exception:" + e.toString());
            }
        }
    }

    public boolean isCurrentGroup(long j) {
        return this.mGroupMetaData != null && this.mGroupMetaData.groupId == j;
    }

    public boolean isInactive() {
        return !isAdded() || isRemoving() || isDetached();
    }

    @Override
    public void onDestroy() {
        if (this.mActionBarAdapter != null) {
            this.mActionBarAdapter.setListener(null);
        }
        ContactSaveService.removeDeleteEndListener(this);
        this.mProgressHandler.dismissDialog(getFragmentManager());
        super.onDestroy();
    }

    public void updateExistingGroupFragment(Uri uri, String str) {
        toastForSaveAction(str);
        Log.e("GroupMembers", "[updateExistingGroupFragment] action:" + str + ", oldUri:" + this.mGroupUri.toString() + ", newUri:" + uri.toString());
        if ((!isEditMode() || getGroupCount() != 1) && !GroupUtil.ACTION_REMOVE_FROM_GROUP.equals(str)) {
            this.mGroupUri = uri;
            this.mGroupMetaData = null;
            reloadData();
            this.mActivity.invalidateOptionsMenu();
        }
    }

    public void toastForSaveAction(String str) {
        int i;
        i = -1;
        switch (str) {
            case "updateGroup":
                i = R.string.groupUpdatedToast;
                break;
            case "removeFromGroup":
                i = R.string.groupMembersRemovedToast;
                break;
            case "createGroup":
                i = R.string.groupCreatedToast;
                break;
            case "addToGroup":
                i = R.string.groupMembersAddedToast;
                break;
            case "switchGroup":
                break;
            default:
                FeedbackHelper.sendFeedback(getContext(), "GroupMembers", "toastForSaveAction passed unknown action: " + str, new IllegalArgumentException("Unhandled contact save action " + str));
                break;
        }
        toast(i);
    }

    private void toast(int i) {
        if (i == R.string.groupMembersRemovedToast && getContext() != null) {
            MtkToast.toast(getContext(), i);
            return;
        }
        if (i >= 0 && getContext() != null) {
            Toast.makeText(getContext(), i, 0).show();
            return;
        }
        if (i >= 0) {
            Log.e("GroupMembers", "[toast] igore. resId=" + i + ", getContext()=" + getContext());
        }
    }

    private int getGroupCount() {
        if (getAdapter() != 0) {
            return ((GroupMembersAdapter) getAdapter()).getCount();
        }
        return -1;
    }

    public void exitEditMode() {
        this.mIsEditMode = false;
        this.mActionBarAdapter.setSelectionMode(false);
        displayDeleteButtons(false);
    }

    @Override
    public void onDeleteEnd() {
        this.mProgressHandler.dismissDialog(getFragmentManager());
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                GroupMembersFragment.this.mActivity.switchToAllContacts();
            }
        });
    }

    @Override
    public void onDeleteStart() {
        this.mProgressHandler.showDialog(getFragmentManager());
    }
}
