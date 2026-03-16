package com.android.contacts.group;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.editor.SelectAccountDialogFragment;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.util.AccountsListAdapter;
import com.android.contacts.common.util.ViewUtil;
import com.android.contacts.group.SuggestedMemberListAdapter;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;

public class GroupEditorFragment extends Fragment implements SelectAccountDialogFragment.Listener {
    protected static final String[] PROJECTION_CONTACT = {"_id", "display_name", "display_name_alt", "sort_key", "starred", "contact_presence", "contact_chat_capability", "photo_id", "photo_thumb_uri", "lookup", "phonetic_name", "has_phone_number", "is_user_profile"};
    private String mAccountName;
    private String mAccountType;
    private String mAction;
    private SuggestedMemberListAdapter mAutoCompleteAdapter;
    private AutoCompleteTextView mAutoCompleteTextView;
    private ContentResolver mContentResolver;
    private Context mContext;
    private String mDataSet;
    private long mGroupId;
    private boolean mGroupNameIsReadOnly;
    private TextView mGroupNameView;
    private Uri mGroupUri;
    private Bundle mIntentExtras;
    private int mLastGroupEditorId;
    private LayoutInflater mLayoutInflater;
    private ListView mListView;
    private Listener mListener;
    private MemberListAdapter mMemberListAdapter;
    private ContactPhotoManager mPhotoManager;
    private ViewGroup mRootView;
    private Status mStatus;
    private String mOriginalGroupName = "";
    private ArrayList<Member> mListMembersToAdd = new ArrayList<>();
    private ArrayList<Member> mListMembersToRemove = new ArrayList<>();
    private ArrayList<Member> mListToDisplay = new ArrayList<>();
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            return new GroupMetaDataLoader(GroupEditorFragment.this.mContext, GroupEditorFragment.this.mGroupUri);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            GroupEditorFragment.this.bindGroupMetaData(data);
            GroupEditorFragment.this.getLoaderManager().initLoader(2, null, GroupEditorFragment.this.mGroupMemberListLoaderListener);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupEditorQuery(GroupEditorFragment.this.mContext, GroupEditorFragment.this.mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<Member> listExistingMembers = new ArrayList<>();
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                long contactId = data.getLong(0);
                long rawContactId = data.getLong(1);
                String lookupKey = data.getString(4);
                String displayName = data.getString(2);
                String photoUri = data.getString(3);
                listExistingMembers.add(new Member(rawContactId, lookupKey, contactId, displayName, photoUri));
            }
            GroupEditorFragment.this.addExistingMembers(listExistingMembers);
            GroupEditorFragment.this.getLoaderManager().destroyLoader(2);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };
    private final LoaderManager.LoaderCallbacks<Cursor> mContactLoaderListener = new LoaderManager.LoaderCallbacks<Cursor>() {
        private long mRawContactId;

        @Override
        public Loader<Cursor> onCreateLoader2(int id, Bundle args) {
            String memberId = args.getString("memberLookupUri");
            this.mRawContactId = args.getLong("rawContactId");
            return new CursorLoader(GroupEditorFragment.this.mContext, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, memberId), GroupEditorFragment.PROJECTION_CONTACT, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (cursor.moveToFirst()) {
                long contactId = cursor.getLong(0);
                String displayName = cursor.getString(1);
                String lookupKey = cursor.getString(9);
                String photoUri = cursor.getString(8);
                GroupEditorFragment.this.getLoaderManager().destroyLoader(3);
                Member member = new Member(this.mRawContactId, lookupKey, contactId, displayName, photoUri);
                GroupEditorFragment.this.addMember(member);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    public interface Listener {
        void onAccountsNotFound();

        void onGroupNotFound();

        void onReverted();

        void onSaveFinished(int i, Intent intent);
    }

    public enum Status {
        SELECTING_ACCOUNT,
        LOADING,
        EDITING,
        SAVING,
        CLOSING
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        this.mLayoutInflater = inflater;
        this.mRootView = (ViewGroup) inflater.inflate(R.layout.group_editor_fragment, container, false);
        return this.mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity;
        this.mPhotoManager = ContactPhotoManager.getInstance(this.mContext);
        this.mMemberListAdapter = new MemberListAdapter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
            if (this.mStatus != Status.SELECTING_ACCOUNT) {
                if (this.mStatus == Status.LOADING) {
                    startGroupMetaDataLoader();
                    return;
                } else {
                    setupEditorForAccount();
                    return;
                }
            }
            return;
        }
        if ("android.intent.action.EDIT".equals(this.mAction)) {
            startGroupMetaDataLoader();
            return;
        }
        if ("android.intent.action.INSERT".equals(this.mAction)) {
            Account account = this.mIntentExtras == null ? null : (Account) this.mIntentExtras.getParcelable("com.android.contacts.extra.ACCOUNT");
            String dataSet = this.mIntentExtras == null ? null : this.mIntentExtras.getString("com.android.contacts.extra.DATA_SET");
            if (account != null) {
                this.mAccountName = account.name;
                this.mAccountType = account.type;
                this.mDataSet = dataSet;
                setupEditorForAccount();
                return;
            }
            selectAccountAndCreateGroup();
            return;
        }
        throw new IllegalArgumentException("Unknown Action String " + this.mAction + ". Only support android.intent.action.EDIT or android.intent.action.INSERT");
    }

    private void startGroupMetaDataLoader() {
        this.mStatus = Status.LOADING;
        getLoaderManager().initLoader(1, null, this.mGroupMetaDataLoaderListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("action", this.mAction);
        outState.putParcelable("groupUri", this.mGroupUri);
        outState.putLong("groupId", this.mGroupId);
        outState.putSerializable("status", this.mStatus);
        outState.putString("accountName", this.mAccountName);
        outState.putString("accountType", this.mAccountType);
        outState.putString("dataSet", this.mDataSet);
        outState.putBoolean("groupNameIsReadOnly", this.mGroupNameIsReadOnly);
        outState.putString("originalGroupName", this.mOriginalGroupName);
        outState.putParcelableArrayList("membersToAdd", this.mListMembersToAdd);
        outState.putParcelableArrayList("membersToRemove", this.mListMembersToRemove);
        outState.putParcelableArrayList("membersToDisplay", this.mListToDisplay);
    }

    private void onRestoreInstanceState(Bundle state) {
        this.mAction = state.getString("action");
        this.mGroupUri = (Uri) state.getParcelable("groupUri");
        this.mGroupId = state.getLong("groupId");
        this.mStatus = (Status) state.getSerializable("status");
        this.mAccountName = state.getString("accountName");
        this.mAccountType = state.getString("accountType");
        this.mDataSet = state.getString("dataSet");
        this.mGroupNameIsReadOnly = state.getBoolean("groupNameIsReadOnly");
        this.mOriginalGroupName = state.getString("originalGroupName");
        this.mListMembersToAdd = state.getParcelableArrayList("membersToAdd");
        this.mListMembersToRemove = state.getParcelableArrayList("membersToRemove");
        this.mListToDisplay = state.getParcelableArrayList("membersToDisplay");
    }

    public void setContentResolver(ContentResolver resolver) {
        this.mContentResolver = resolver;
        if (this.mAutoCompleteAdapter != null) {
            this.mAutoCompleteAdapter.setContentResolver(this.mContentResolver);
        }
    }

    private void selectAccountAndCreateGroup() {
        List<AccountWithDataSet> accounts = AccountTypeManager.getInstance(this.mContext).getGroupWritableAccounts();
        if (accounts.isEmpty()) {
            Log.e("GroupEditorFragment", "No accounts were found.");
            if (this.mListener != null) {
                this.mListener.onAccountsNotFound();
                return;
            }
            return;
        }
        if (accounts.size() == 1) {
            this.mAccountName = accounts.get(0).name;
            this.mAccountType = accounts.get(0).type;
            this.mDataSet = accounts.get(0).dataSet;
            setupEditorForAccount();
            return;
        }
        this.mStatus = Status.SELECTING_ACCOUNT;
        SelectAccountDialogFragment.show(getFragmentManager(), this, R.string.dialog_new_group_account, AccountsListAdapter.AccountListFilter.ACCOUNTS_GROUP_WRITABLE, null);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        this.mAccountName = account.name;
        this.mAccountType = account.type;
        this.mDataSet = account.dataSet;
        setupEditorForAccount();
    }

    @Override
    public void onAccountSelectorCancelled() {
        if (this.mListener != null) {
            this.mListener.onGroupNotFound();
        }
    }

    private AccountType getAccountType() {
        return AccountTypeManager.getInstance(this.mContext).getAccountType(this.mAccountType, this.mDataSet);
    }

    private boolean isGroupMembershipEditable() {
        if (this.mAccountType == null) {
            return false;
        }
        return getAccountType().isGroupMembershipEditable();
    }

    private void setupEditorForAccount() {
        View editorView;
        AccountType accountType = getAccountType();
        boolean editable = isGroupMembershipEditable();
        boolean isNewEditor = false;
        this.mMemberListAdapter.setIsGroupMembershipEditable(editable);
        int newGroupEditorId = editable ? R.layout.group_editor_view : R.layout.external_group_editor_view;
        if (newGroupEditorId != this.mLastGroupEditorId) {
            View oldEditorView = this.mRootView.findViewWithTag("currentEditorForAccount");
            if (oldEditorView != null) {
                this.mRootView.removeView(oldEditorView);
            }
            editorView = this.mLayoutInflater.inflate(newGroupEditorId, this.mRootView, false);
            editorView.setTag("currentEditorForAccount");
            this.mAutoCompleteAdapter = null;
            this.mLastGroupEditorId = newGroupEditorId;
            isNewEditor = true;
        } else {
            editorView = this.mRootView.findViewWithTag("currentEditorForAccount");
            if (editorView == null) {
                throw new IllegalStateException("Group editor view not found");
            }
        }
        this.mGroupNameView = (TextView) editorView.findViewById(R.id.group_name);
        this.mAutoCompleteTextView = (AutoCompleteTextView) editorView.findViewById(R.id.add_member_field);
        this.mListView = (ListView) editorView.findViewById(android.R.id.list);
        this.mListView.setAdapter((ListAdapter) this.mMemberListAdapter);
        if (editorView.findViewById(R.id.account_header) != null) {
            CharSequence accountTypeDisplayLabel = accountType.getDisplayLabel(this.mContext);
            ImageView accountIcon = (ImageView) editorView.findViewById(R.id.account_icon);
            TextView accountTypeTextView = (TextView) editorView.findViewById(R.id.account_type);
            TextView accountNameTextView = (TextView) editorView.findViewById(R.id.account_name);
            if (!TextUtils.isEmpty(this.mAccountName)) {
                accountNameTextView.setText(this.mContext.getString(R.string.from_account_format, this.mAccountName));
            }
            accountTypeTextView.setText(accountTypeDisplayLabel);
            accountIcon.setImageDrawable(accountType.getDisplayIcon(this.mContext));
        }
        if (this.mAutoCompleteTextView != null) {
            this.mAutoCompleteAdapter = new SuggestedMemberListAdapter(this.mContext, android.R.layout.simple_dropdown_item_1line);
            this.mAutoCompleteAdapter.setContentResolver(this.mContentResolver);
            this.mAutoCompleteAdapter.setAccountType(this.mAccountType);
            this.mAutoCompleteAdapter.setAccountName(this.mAccountName);
            this.mAutoCompleteAdapter.setDataSet(this.mDataSet);
            this.mAutoCompleteTextView.setAdapter(this.mAutoCompleteAdapter);
            this.mAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SuggestedMemberListAdapter.SuggestedMember member = (SuggestedMemberListAdapter.SuggestedMember) view.getTag();
                    if (member != null) {
                        GroupEditorFragment.this.loadMemberToAddToGroup(member.getRawContactId(), String.valueOf(member.getContactId()));
                        GroupEditorFragment.this.mAutoCompleteAdapter.addNewMember(member.getContactId());
                        GroupEditorFragment.this.mAutoCompleteTextView.setText("");
                    }
                }
            });
            this.mAutoCompleteAdapter.updateExistingMembersList(this.mListToDisplay);
        }
        this.mGroupNameView.setFocusable(!this.mGroupNameIsReadOnly);
        if (isNewEditor) {
            this.mRootView.addView(editorView);
        }
        this.mStatus = Status.EDITING;
    }

    public void load(String action, Uri groupUri, Bundle intentExtras) {
        this.mAction = action;
        this.mGroupUri = groupUri;
        this.mGroupId = groupUri != null ? ContentUris.parseId(this.mGroupUri) : 0L;
        this.mIntentExtras = intentExtras;
    }

    private void bindGroupMetaData(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            Log.i("GroupEditorFragment", "Group not found with URI: " + this.mGroupUri + " Closing activity now.");
            if (this.mListener != null) {
                this.mListener.onGroupNotFound();
                return;
            }
            return;
        }
        this.mOriginalGroupName = cursor.getString(4);
        this.mAccountName = cursor.getString(0);
        this.mAccountType = cursor.getString(1);
        this.mDataSet = cursor.getString(2);
        this.mGroupNameIsReadOnly = cursor.getInt(7) == 1;
        if (isSimGroup()) {
            this.mGroupNameIsReadOnly = false;
        }
        setupEditorForAccount();
        this.mGroupNameView.setText(this.mOriginalGroupName);
    }

    private boolean isSimGroup() {
        return this.mAccountType.equals("com.android.contact.sim") || this.mAccountType.equals("com.android.contact.sim2");
    }

    public void loadMemberToAddToGroup(long rawContactId, String contactId) {
        Bundle args = new Bundle();
        args.putLong("rawContactId", rawContactId);
        args.putString("memberLookupUri", contactId);
        getLoaderManager().restartLoader(3, args, this.mContactLoaderListener);
    }

    public void setListener(Listener value) {
        this.mListener = value;
    }

    public void onDoneClicked() {
        if (isGroupMembershipEditable()) {
            save();
        } else {
            doRevertAction();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.edit_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_discard:
                return revert();
            default:
                return false;
        }
    }

    private boolean revert() {
        if (!hasNameChange() && !hasMembershipChange()) {
            doRevertAction();
            return true;
        }
        CancelEditDialogFragment.show(this);
        return true;
    }

    private void doRevertAction() {
        this.mStatus = Status.CLOSING;
        if (this.mListener != null) {
            this.mListener.onReverted();
        }
    }

    public static class CancelEditDialogFragment extends DialogFragment {
        public static void show(GroupEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).setIconAttribute(android.R.attr.alertDialogIcon).setMessage(R.string.cancel_confirmation_dialog_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int whichButton) {
                    ((GroupEditorFragment) CancelEditDialogFragment.this.getTargetFragment()).doRevertAction();
                }
            }).setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null).create();
            return dialog;
        }
    }

    public boolean save() {
        Intent saveIntent;
        if (!hasValidGroupName() || this.mStatus != Status.EDITING) {
            this.mStatus = Status.CLOSING;
            if (this.mListener != null) {
                this.mListener.onReverted();
            }
            return false;
        }
        getLoaderManager().destroyLoader(2);
        if (!hasNameChange() && !hasMembershipChange()) {
            onSaveCompleted(false, this.mGroupUri);
            return true;
        }
        this.mStatus = Status.SAVING;
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }
        if ("android.intent.action.INSERT".equals(this.mAction)) {
            long[] membersToAddArray = convertToArray(this.mListMembersToAdd);
            saveIntent = ContactSaveService.createNewGroupIntent(activity, new AccountWithDataSet(this.mAccountName, this.mAccountType, this.mDataSet), this.mGroupNameView.getText().toString(), membersToAddArray, activity.getClass(), "saveCompleted");
        } else if ("android.intent.action.EDIT".equals(this.mAction)) {
            long[] membersToAddArray2 = convertToArray(this.mListMembersToAdd);
            long[] membersToRemoveArray = convertToArray(this.mListMembersToRemove);
            saveIntent = ContactSaveService.createGroupUpdateIntent(activity, this.mGroupId, getUpdatedName(), membersToAddArray2, membersToRemoveArray, activity.getClass(), "saveCompleted", isSimGroup());
        } else {
            throw new IllegalStateException("Invalid intent action type " + this.mAction);
        }
        activity.startService(saveIntent);
        return true;
    }

    public void onSaveCompleted(boolean hadChanges, Uri groupUri) {
        int resultCode;
        Intent resultIntent;
        boolean success = groupUri != null;
        Log.d("GroupEditorFragment", "onSaveCompleted(" + groupUri + ")");
        if (hadChanges) {
            Toast.makeText(this.mContext, success ? R.string.groupSavedToast : R.string.groupSavedErrorToast, 0).show();
        }
        if (success && groupUri != null) {
            String requestAuthority = groupUri.getAuthority();
            resultIntent = new Intent();
            if ("contacts".equals(requestAuthority)) {
                long groupId = ContentUris.parseId(groupUri);
                Uri legacyContentUri = Uri.parse("content://contacts/groups");
                Uri legacyUri = ContentUris.withAppendedId(legacyContentUri, groupId);
                resultIntent.setData(legacyUri);
            } else {
                resultIntent.setData(groupUri);
            }
            resultCode = -1;
        } else {
            resultCode = 0;
            resultIntent = null;
        }
        this.mStatus = Status.CLOSING;
        if (this.mListener != null) {
            this.mListener.onSaveFinished(resultCode, resultIntent);
        }
    }

    private boolean hasValidGroupName() {
        return (this.mGroupNameView == null || TextUtils.isEmpty(this.mGroupNameView.getText())) ? false : true;
    }

    private boolean hasNameChange() {
        return (this.mGroupNameView == null || this.mGroupNameView.getText().toString().equals(this.mOriginalGroupName)) ? false : true;
    }

    private boolean hasMembershipChange() {
        return this.mListMembersToAdd.size() > 0 || this.mListMembersToRemove.size() > 0;
    }

    private String getUpdatedName() {
        String groupNameFromTextView = this.mGroupNameView.getText().toString();
        if (groupNameFromTextView.equals(this.mOriginalGroupName)) {
            return null;
        }
        return groupNameFromTextView;
    }

    private static long[] convertToArray(List<Member> listMembers) {
        int size = listMembers.size();
        long[] membersArray = new long[size];
        for (int i = 0; i < size; i++) {
            membersArray[i] = listMembers.get(i).getRawContactId();
        }
        return membersArray;
    }

    private void addExistingMembers(List<Member> members) {
        this.mListToDisplay.clear();
        this.mListToDisplay.addAll(members);
        this.mListToDisplay.addAll(this.mListMembersToAdd);
        this.mListToDisplay.removeAll(this.mListMembersToRemove);
        this.mMemberListAdapter.notifyDataSetChanged();
        if (this.mAutoCompleteAdapter != null) {
            this.mAutoCompleteAdapter.updateExistingMembersList(members);
        }
    }

    private void addMember(Member member) {
        this.mListMembersToAdd.add(member);
        this.mListToDisplay.add(member);
        this.mMemberListAdapter.notifyDataSetChanged();
        this.mAutoCompleteAdapter.addNewMember(member.getContactId());
    }

    private void removeMember(Member member) {
        if (this.mListMembersToAdd.contains(member)) {
            this.mListMembersToAdd.remove(member);
        } else {
            this.mListMembersToRemove.add(member);
        }
        this.mListToDisplay.remove(member);
        this.mMemberListAdapter.notifyDataSetChanged();
        this.mAutoCompleteAdapter.removeMember(member.getContactId());
    }

    public static class Member implements Parcelable {
        public static final Parcelable.Creator<Member> CREATOR = new Parcelable.Creator<Member>() {
            @Override
            public Member createFromParcel(Parcel in) {
                return new Member(in);
            }

            @Override
            public Member[] newArray(int size) {
                return new Member[size];
            }
        };
        private final long mContactId;
        private final String mDisplayName;
        private final String mLookupKey;
        private final Uri mLookupUri;
        private final Uri mPhotoUri;
        private final long mRawContactId;

        public Member(long rawContactId, String lookupKey, long contactId, String displayName, String photoUri) {
            this.mRawContactId = rawContactId;
            this.mContactId = contactId;
            this.mLookupKey = lookupKey;
            this.mLookupUri = ContactsContract.Contacts.getLookupUri(contactId, lookupKey);
            this.mDisplayName = displayName;
            this.mPhotoUri = photoUri != null ? Uri.parse(photoUri) : null;
        }

        public long getRawContactId() {
            return this.mRawContactId;
        }

        public long getContactId() {
            return this.mContactId;
        }

        public Uri getLookupUri() {
            return this.mLookupUri;
        }

        public String getLookupKey() {
            return this.mLookupKey;
        }

        public String getDisplayName() {
            return this.mDisplayName;
        }

        public Uri getPhotoUri() {
            return this.mPhotoUri;
        }

        public boolean equals(Object object) {
            if (!(object instanceof Member)) {
                return false;
            }
            Member otherMember = (Member) object;
            return Objects.equal(this.mLookupUri, otherMember.getLookupUri());
        }

        public int hashCode() {
            if (this.mLookupUri == null) {
                return 0;
            }
            return this.mLookupUri.hashCode();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.mRawContactId);
            dest.writeLong(this.mContactId);
            dest.writeParcelable(this.mLookupUri, flags);
            dest.writeString(this.mLookupKey);
            dest.writeString(this.mDisplayName);
            dest.writeParcelable(this.mPhotoUri, flags);
        }

        private Member(Parcel in) {
            this.mRawContactId = in.readLong();
            this.mContactId = in.readLong();
            this.mLookupUri = (Uri) in.readParcelable(getClass().getClassLoader());
            this.mLookupKey = in.readString();
            this.mDisplayName = in.readString();
            this.mPhotoUri = (Uri) in.readParcelable(getClass().getClassLoader());
        }
    }

    private final class MemberListAdapter extends BaseAdapter {
        private boolean mIsGroupMembershipEditable;

        private MemberListAdapter() {
            this.mIsGroupMembershipEditable = true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result;
            if (convertView == null) {
                result = GroupEditorFragment.this.mLayoutInflater.inflate(this.mIsGroupMembershipEditable ? R.layout.group_member_item : R.layout.external_group_member_item, parent, false);
            } else {
                result = convertView;
            }
            final Member member = getItem(position);
            QuickContactBadge badge = (QuickContactBadge) result.findViewById(R.id.badge);
            badge.assignContactUri(member.getLookupUri());
            TextView name = (TextView) result.findViewById(R.id.name);
            name.setText(member.getDisplayName());
            View deleteButton = result.findViewById(R.id.delete_button_container);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        GroupEditorFragment.this.removeMember(member);
                    }
                });
            }
            TelephonyManager tm = TelephonyManager.from(GroupEditorFragment.this.getActivity());
            SubscriptionManager subscriptionManager = SubscriptionManager.from(GroupEditorFragment.this.mContext);
            if (GroupEditorFragment.this.mAccountType == null || !GroupEditorFragment.this.mAccountType.equals("com.android.contact.sim") || !tm.isMultiSimEnabled()) {
                if (GroupEditorFragment.this.mAccountType != null && GroupEditorFragment.this.mAccountType.equals("com.android.contact.sim2") && tm.isMultiSimEnabled()) {
                    int[] subs = SubscriptionManager.getSubId(1);
                    if (subs != null) {
                        SubscriptionInfo subinfo = subscriptionManager.getActiveSubscriptionInfo(subs[0]);
                        Bitmap iconBitmap = subinfo.createIconBitmap(GroupEditorFragment.this.mContext);
                        badge.setImageBitmap(iconBitmap);
                    }
                } else {
                    ContactPhotoManager.DefaultImageRequest request = new ContactPhotoManager.DefaultImageRequest(member.getDisplayName(), member.getLookupKey(), true);
                    GroupEditorFragment.this.mPhotoManager.loadPhoto(badge, member.getPhotoUri(), ViewUtil.getConstantPreLayoutWidth(badge), false, true, request);
                }
            } else {
                int[] subs2 = SubscriptionManager.getSubId(0);
                if (subs2 != null) {
                    SubscriptionInfo subinfo2 = subscriptionManager.getActiveSubscriptionInfo(subs2[0]);
                    Bitmap iconBitmap2 = subinfo2.createIconBitmap(GroupEditorFragment.this.mContext);
                    badge.setImageBitmap(iconBitmap2);
                }
            }
            return result;
        }

        @Override
        public int getCount() {
            return GroupEditorFragment.this.mListToDisplay.size();
        }

        @Override
        public Member getItem(int position) {
            return (Member) GroupEditorFragment.this.mListToDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setIsGroupMembershipEditable(boolean editable) {
            this.mIsGroupMembershipEditable = editable;
        }
    }
}
