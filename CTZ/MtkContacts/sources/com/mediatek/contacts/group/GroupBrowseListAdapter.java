package com.mediatek.contacts.group;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.group.GroupListItem;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.account.AccountWithDataSet;
import com.android.contacts.util.AccountFilterUtil;
import com.google.common.base.Objects;
import com.mediatek.contacts.model.account.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.SubInfoUtils;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.List;

public class GroupBrowseListAdapter extends BaseAdapter {
    private static final String TAG = GroupBrowseListAdapter.class.getSimpleName();
    private final AccountTypeManager mAccountTypeManager;
    private final Context mContext;
    private Cursor mCursor;
    private final LayoutInflater mLayoutInflater;
    private Uri mSelectedGroupUri;
    private boolean mSelectionVisible;

    public GroupBrowseListAdapter(Context context) {
        this.mContext = context;
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mAccountTypeManager = AccountTypeManager.getInstance(this.mContext);
    }

    public void setCursor(Cursor cursor) {
        this.mCursor = cursor;
        if (this.mSelectedGroupUri == null && cursor != null && cursor.getCount() > 0) {
            GroupListItem item = getItem(0);
            if (item != null) {
                this.mSelectedGroupUri = getGroupUriFromIdAndAccountInfo(item.getGroupId(), item.getAccountName(), item.getAccountType());
            } else {
                this.mSelectedGroupUri = getGroupUriFromIdAndAccountInfo(0L, null, null);
            }
        }
        notifyDataSetChanged();
    }

    public int getSelectedGroupPosition() {
        if (this.mSelectedGroupUri == null || this.mCursor == null || this.mCursor.getCount() == 0) {
            return -1;
        }
        this.mCursor.moveToPosition(-1);
        int i = 0;
        while (this.mCursor.moveToNext()) {
            if (this.mSelectedGroupUri.equals(getGroupUriFromIdAndAccountInfo(this.mCursor.getLong(3), this.mCursor.getString(0), this.mCursor.getString(1)))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void setSelectionVisible(boolean z) {
        this.mSelectionVisible = z;
    }

    public void setSelectedGroup(Uri uri) {
        this.mSelectedGroupUri = uri;
    }

    private boolean isSelectedGroup(Uri uri) {
        return this.mSelectedGroupUri != null && this.mSelectedGroupUri.equals(uri);
    }

    public Uri getSelectedGroup() {
        return this.mSelectedGroupUri;
    }

    @Override
    public int getCount() {
        if (this.mCursor == null || this.mCursor.isClosed()) {
            return 0;
        }
        return this.mCursor.getCount();
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public GroupListItem getItem(int i) {
        boolean z;
        if (this.mCursor == null || this.mCursor.isClosed() || !this.mCursor.moveToPosition(i)) {
            Log.e(TAG, "[getItem] mCursor: " + this.mCursor + ", position: " + i);
            return null;
        }
        String string = this.mCursor.getString(0);
        String string2 = this.mCursor.getString(1);
        String string3 = this.mCursor.getString(2);
        long j = this.mCursor.getLong(3);
        String string4 = this.mCursor.getString(4);
        int i2 = this.mCursor.getInt(5);
        boolean z2 = this.mCursor.getInt(6) == 1;
        String string5 = this.mCursor.getString(7);
        int i3 = i - 1;
        if (i3 < 0 || !this.mCursor.moveToPosition(i3)) {
            z = true;
        } else {
            String string6 = this.mCursor.getString(0);
            String string7 = this.mCursor.getString(1);
            String string8 = this.mCursor.getString(2);
            if (string.equals(string6) && string2.equals(string7) && Objects.equal(string3, string8)) {
                z = false;
            }
        }
        return new GroupListItem(string, string2, string3, j, string4, z, i2, z2, string5);
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        GroupListItemViewCache groupListItemViewCache;
        GroupListItem item = getItem(i);
        if (view == null) {
            view = this.mLayoutInflater.inflate(R.layout.mtk_group_browse_list_item_with_checkbox, viewGroup, false);
            groupListItemViewCache = new GroupListItemViewCache(view);
            view.setTag(groupListItemViewCache);
        } else {
            groupListItemViewCache = (GroupListItemViewCache) view.getTag();
        }
        if (item.isFirstGroupInAccount()) {
            bindHeaderView(item, groupListItemViewCache);
            groupListItemViewCache.accountHeader.setVisibility(0);
            groupListItemViewCache.divider.setVisibility(8);
            if (i == 0) {
                groupListItemViewCache.accountHeaderExtraTopPadding.setVisibility(0);
            } else {
                groupListItemViewCache.accountHeaderExtraTopPadding.setVisibility(8);
            }
        } else {
            groupListItemViewCache.accountHeader.setVisibility(8);
            groupListItemViewCache.divider.setVisibility(0);
            groupListItemViewCache.accountHeaderExtraTopPadding.setVisibility(8);
        }
        Uri groupUriFromIdAndAccountInfo = getGroupUriFromIdAndAccountInfo(item.getGroupId(), item.getAccountName(), item.getAccountType());
        String quantityString = this.mContext.getResources().getQuantityString(R.plurals.group_list_num_contacts_in_group, item.getMemberCount(), Integer.valueOf(item.getMemberCount()));
        groupListItemViewCache.setUri(groupUriFromIdAndAccountInfo);
        groupListItemViewCache.groupTitle.setText(item.getTitle());
        groupListItemViewCache.groupMemberCount.setText(quantityString);
        if (this.mSelectionVisible) {
            view.setActivated(isSelectedGroup(groupUriFromIdAndAccountInfo));
        }
        setViewWithCheckBox(view, i);
        return view;
    }

    private void bindHeaderView(GroupListItem groupListItem, GroupListItemViewCache groupListItemViewCache) {
        AccountType accountType = this.mAccountTypeManager.getAccountType(groupListItem.getAccountType(), groupListItem.getDataSet());
        groupListItemViewCache.accountType.setText(accountType.getDisplayLabel(this.mContext));
        groupListItemViewCache.accountName.setText(groupListItem.getAccountName());
        if (AccountWithDataSetEx.isLocalPhone(accountType.accountType) || (AccountTypeUtils.isAccountTypeIccCard(accountType.accountType) && SubInfoUtils.getActivatedSubInfoCount() == 1)) {
            groupListItemViewCache.accountName.setVisibility(8);
            return;
        }
        groupListItemViewCache.accountName.setVisibility(0);
        String accountDisplayNameByAccount = AccountFilterUtil.getAccountDisplayNameByAccount(groupListItem.getAccountType(), groupListItem.getAccountName());
        if (accountDisplayNameByAccount == null) {
            groupListItemViewCache.accountName.setText(groupListItem.getAccountName());
        } else {
            groupListItemViewCache.accountName.setText(accountDisplayNameByAccount);
        }
    }

    public static class GroupListItemViewCache {
        public final View accountHeader;
        public final View accountHeaderExtraTopPadding;
        public final TextView accountName;
        public final TextView accountType;
        public final View divider;
        public final TextView groupMemberCount;
        public final TextView groupTitle;
        private Uri mUri;

        public GroupListItemViewCache(View view) {
            this.accountType = (TextView) view.findViewById(R.id.account_type);
            this.accountName = (TextView) view.findViewById(R.id.account_name);
            this.groupTitle = (TextView) view.findViewById(R.id.label);
            this.groupMemberCount = (TextView) view.findViewById(R.id.count);
            this.accountHeader = view.findViewById(R.id.group_list_header);
            this.accountHeaderExtraTopPadding = view.findViewById(R.id.header_extra_top_padding);
            this.divider = view.findViewById(R.id.divider);
        }

        public void setUri(Uri uri) {
            this.mUri = uri;
        }
    }

    private Uri getGroupUriFromIdAndAccountInfo(long j, String str, String str2) {
        Uri uriWithAppendedId = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, j);
        if (str != null && str2 != null) {
            return groupUriWithAccountInfo(uriWithAppendedId, str, str2);
        }
        return uriWithAppendedId;
    }

    private Uri groupUriWithAccountInfo(Uri uri, String str, String str2) {
        if (uri == null) {
            return uri;
        }
        List<AccountWithDataSet> listBlockForWritableAccounts = AccountTypeManager.getInstance(this.mContext).blockForWritableAccounts();
        int i = 0;
        int invalidSubId = SubInfoUtils.getInvalidSubId();
        for (AccountWithDataSet accountWithDataSet : listBlockForWritableAccounts) {
            if (accountWithDataSet.name.equals(str) && accountWithDataSet.type.equals(str2)) {
                AccountWithDataSet accountWithDataSet2 = listBlockForWritableAccounts.get(i);
                if (accountWithDataSet2 instanceof AccountWithDataSetEx) {
                    invalidSubId = ((AccountWithDataSetEx) accountWithDataSet2).getSubId();
                }
            }
            i++;
        }
        return uri.buildUpon().appendPath(String.valueOf(invalidSubId)).appendPath(str).appendPath(str2).build();
    }

    protected void setViewWithCheckBox(View view, int i) {
    }
}
