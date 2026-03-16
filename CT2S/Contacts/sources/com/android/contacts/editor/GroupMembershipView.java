package com.android.contacts.editor;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.interactions.GroupCreationDialogFragment;
import com.android.contacts.util.UiClosables;
import com.google.common.base.Objects;
import java.util.ArrayList;

public class GroupMembershipView extends LinearLayout implements View.OnClickListener, AdapterView.OnItemClickListener {
    private String mAccountName;
    private String mAccountType;
    private GroupMembershipAdapter<GroupSelectionItem> mAdapter;
    private boolean mCreatedNewGroup;
    private String mDataSet;
    private long mDefaultGroupId;
    private boolean mDefaultGroupVisibilityKnown;
    private boolean mDefaultGroupVisible;
    private long mFavoritesGroupId;
    private TextView mGroupList;
    private Cursor mGroupMetaData;
    private int mHintTextColor;
    private DataKind mKind;
    private String mNoGroupString;
    private ListPopupWindow mPopup;
    private int mPrimaryTextColor;
    private RawContactDelta mState;

    public static final class GroupSelectionItem {
        private boolean mChecked;
        private final long mGroupId;
        private final String mTitle;

        public GroupSelectionItem(long groupId, String title, boolean checked) {
            this.mGroupId = groupId;
            this.mTitle = title;
            this.mChecked = checked;
        }

        public long getGroupId() {
            return this.mGroupId;
        }

        public boolean isChecked() {
            return this.mChecked;
        }

        public void setChecked(boolean checked) {
            this.mChecked = checked;
        }

        public String toString() {
            return this.mTitle;
        }
    }

    private class GroupMembershipAdapter<T> extends ArrayAdapter<T> {
        public GroupMembershipAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public boolean getItemIsCheckable(int position) {
            return position != getCount() + (-1);
        }

        @Override
        public int getItemViewType(int position) {
            return getItemIsCheckable(position) ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View itemView = super.getView(position, convertView, parent);
            if (itemView == null) {
                return null;
            }
            CheckedTextView checkedTextView = (CheckedTextView) itemView;
            if (!getItemIsCheckable(position)) {
                checkedTextView.setCheckMarkDrawable((Drawable) null);
            }
            checkedTextView.setTextColor(GroupMembershipView.this.mPrimaryTextColor);
            return checkedTextView;
        }
    }

    public GroupMembershipView(Context context) {
        super(context);
    }

    public GroupMembershipView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = this.mContext.getResources();
        this.mPrimaryTextColor = resources.getColor(R.color.primary_text_color);
        this.mHintTextColor = resources.getColor(R.color.editor_disabled_text_color);
        this.mNoGroupString = this.mContext.getString(R.string.group_edit_field_hint_text);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mGroupList != null) {
            this.mGroupList.setEnabled(enabled);
        }
    }

    public void setKind(DataKind kind) {
        this.mKind = kind;
        ImageView imageView = (ImageView) findViewById(R.id.kind_icon);
        imageView.setContentDescription(getResources().getString(kind.titleRes));
    }

    public void setGroupMetaData(Cursor groupMetaData) {
        this.mGroupMetaData = groupMetaData;
        updateView();
        if (this.mCreatedNewGroup) {
            this.mCreatedNewGroup = false;
            onClick(this);
            if (this.mPopup != null) {
                int position = this.mAdapter.getCount() - 2;
                ListView listView = this.mPopup.getListView();
                if (listView != null && !listView.isItemChecked(position)) {
                    listView.setItemChecked(position, true);
                    onItemClick(listView, null, position, listView.getItemIdAtPosition(position));
                }
            }
        }
    }

    public void setState(RawContactDelta state) {
        this.mState = state;
        this.mAccountType = this.mState.getAccountType();
        this.mAccountName = this.mState.getAccountName();
        this.mDataSet = this.mState.getDataSet();
        this.mDefaultGroupVisibilityKnown = false;
        this.mCreatedNewGroup = false;
        updateView();
    }

    private void updateView() {
        if (this.mGroupMetaData == null || this.mGroupMetaData.isClosed() || this.mAccountType == null || this.mAccountName == null) {
            setVisibility(8);
            return;
        }
        boolean accountHasGroups = false;
        this.mFavoritesGroupId = 0L;
        this.mDefaultGroupId = 0L;
        StringBuilder sb = new StringBuilder();
        this.mGroupMetaData.moveToPosition(-1);
        while (this.mGroupMetaData.moveToNext()) {
            String accountName = this.mGroupMetaData.getString(0);
            String accountType = this.mGroupMetaData.getString(1);
            String dataSet = this.mGroupMetaData.getString(2);
            if (accountName.equals(this.mAccountName) && accountType.equals(this.mAccountType) && Objects.equal(dataSet, this.mDataSet)) {
                long groupId = this.mGroupMetaData.getLong(3);
                if (!this.mGroupMetaData.isNull(6) && this.mGroupMetaData.getInt(6) != 0) {
                    this.mFavoritesGroupId = groupId;
                } else if (!this.mGroupMetaData.isNull(5) && this.mGroupMetaData.getInt(5) != 0) {
                    this.mDefaultGroupId = groupId;
                } else {
                    accountHasGroups = true;
                }
                if (groupId != this.mFavoritesGroupId && groupId != this.mDefaultGroupId && hasMembership(groupId)) {
                    String title = this.mGroupMetaData.getString(4);
                    if (!TextUtils.isEmpty(title)) {
                        if (sb.length() != 0) {
                            sb.append(", ");
                        }
                        sb.append(title);
                    }
                }
            }
        }
        if (!accountHasGroups) {
            setVisibility(8);
            return;
        }
        if (this.mGroupList == null) {
            this.mGroupList = (TextView) findViewById(R.id.group_list);
            this.mGroupList.setOnClickListener(this);
        }
        this.mGroupList.setEnabled(isEnabled());
        if (sb.length() == 0) {
            this.mGroupList.setText(this.mNoGroupString);
            this.mGroupList.setTextColor(this.mHintTextColor);
        } else {
            this.mGroupList.setText(sb);
            this.mGroupList.setTextColor(this.mPrimaryTextColor);
        }
        setVisibility(0);
        if (!this.mDefaultGroupVisibilityKnown) {
            this.mDefaultGroupVisible = (this.mDefaultGroupId == 0 || hasMembership(this.mDefaultGroupId)) ? false : true;
            this.mDefaultGroupVisibilityKnown = true;
        }
    }

    @Override
    public void onClick(View v) {
        if (UiClosables.closeQuietly(this.mPopup)) {
            this.mPopup = null;
            return;
        }
        this.mAdapter = new GroupMembershipAdapter<>(getContext(), R.layout.group_membership_list_item);
        this.mGroupMetaData.moveToPosition(-1);
        while (this.mGroupMetaData.moveToNext()) {
            String accountName = this.mGroupMetaData.getString(0);
            String accountType = this.mGroupMetaData.getString(1);
            String dataSet = this.mGroupMetaData.getString(2);
            int isDeleted = this.mGroupMetaData.getInt(8);
            if (accountName.equals(this.mAccountName) && accountType.equals(this.mAccountType) && Objects.equal(dataSet, this.mDataSet) && isDeleted == 0) {
                long groupId = this.mGroupMetaData.getLong(3);
                if (groupId != this.mFavoritesGroupId && (groupId != this.mDefaultGroupId || this.mDefaultGroupVisible)) {
                    String title = this.mGroupMetaData.getString(4);
                    boolean checked = hasMembership(groupId);
                    this.mAdapter.add(new GroupSelectionItem(groupId, title, checked));
                }
            }
        }
        this.mAdapter.add(new GroupSelectionItem(133L, getContext().getString(R.string.create_group_item_label), false));
        this.mPopup = new ListPopupWindow(getContext(), null);
        this.mPopup.setAnchorView(this.mGroupList);
        this.mPopup.setAdapter(this.mAdapter);
        this.mPopup.setModal(true);
        this.mPopup.setInputMethodMode(2);
        this.mPopup.show();
        ListView listView = this.mPopup.getListView();
        listView.setChoiceMode(2);
        listView.setOverScrollMode(0);
        int count = this.mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            listView.setItemChecked(i, this.mAdapter.getItem(i).isChecked());
        }
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        UiClosables.closeQuietly(this.mPopup);
        this.mPopup = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ValuesDelta entry;
        Long groupId;
        ListView list = (ListView) parent;
        int count = this.mAdapter.getCount();
        if (list.isItemChecked(count - 1)) {
            list.setItemChecked(count - 1, false);
            createNewGroup();
            return;
        }
        for (int i = 0; i < count; i++) {
            this.mAdapter.getItem(i).setChecked(list.isItemChecked(i));
        }
        ArrayList<ValuesDelta> entries = this.mState.getMimeEntries("vnd.android.cursor.item/group_membership");
        if (entries != null) {
            for (ValuesDelta entry2 : entries) {
                if (!entry2.isDelete() && (groupId = entry2.getGroupRowId()) != null && groupId.longValue() != this.mFavoritesGroupId && (groupId.longValue() != this.mDefaultGroupId || this.mDefaultGroupVisible)) {
                    if (!isGroupChecked(groupId.longValue())) {
                        entry2.markDeleted();
                    }
                }
            }
        }
        for (int i2 = 0; i2 < count; i2++) {
            GroupSelectionItem item = this.mAdapter.getItem(i2);
            long groupId2 = item.getGroupId();
            if (item.isChecked() && !hasMembership(groupId2) && (entry = RawContactModifier.insertChild(this.mState, this.mKind)) != null) {
                entry.setGroupRowId(groupId2);
            }
        }
        updateView();
    }

    private boolean isGroupChecked(long groupId) {
        int count = this.mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            GroupSelectionItem item = this.mAdapter.getItem(i);
            if (groupId == item.getGroupId()) {
                return item.isChecked();
            }
        }
        return false;
    }

    private boolean hasMembership(long groupId) {
        Long id;
        if (groupId == this.mDefaultGroupId && this.mState.isContactInsert()) {
            return true;
        }
        ArrayList<ValuesDelta> entries = this.mState.getMimeEntries("vnd.android.cursor.item/group_membership");
        if (entries != null) {
            for (ValuesDelta values : entries) {
                if (!values.isDelete() && (id = values.getGroupRowId()) != null && id.longValue() == groupId) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createNewGroup() {
        UiClosables.closeQuietly(this.mPopup);
        this.mPopup = null;
        GroupCreationDialogFragment.show(((Activity) getContext()).getFragmentManager(), this.mAccountType, this.mAccountName, this.mDataSet, new GroupCreationDialogFragment.OnGroupCreatedListener() {
            @Override
            public void onGroupCreated() {
                GroupMembershipView.this.mCreatedNewGroup = true;
            }
        });
    }
}
