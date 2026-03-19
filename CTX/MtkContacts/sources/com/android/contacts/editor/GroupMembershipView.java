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
import com.android.contacts.group.GroupNameEditDialogFragment;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.UiClosables;
import com.google.common.base.Objects;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;

public class GroupMembershipView extends LinearLayout implements View.OnClickListener, AdapterView.OnItemClickListener {
    private boolean mAccountHasGroups;
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
    private GroupNameEditDialogFragment mGroupNameEditDialogFragment;
    private int mHintTextColor;
    private DataKind mKind;
    private GroupNameEditDialogFragment.Listener mListener;
    private String mNoGroupString;
    private ListPopupWindow mPopup;
    private int mPrimaryTextColor;
    private RawContactDelta mState;

    public static final class GroupSelectionItem {
        private boolean mChecked;
        private final long mGroupId;
        private final String mTitle;

        public GroupSelectionItem(long j, String str, boolean z) {
            this.mGroupId = j;
            this.mTitle = str;
            this.mChecked = z;
        }

        public long getGroupId() {
            return this.mGroupId;
        }

        public boolean isChecked() {
            return this.mChecked;
        }

        public void setChecked(boolean z) {
            this.mChecked = z;
        }

        public String toString() {
            return this.mTitle;
        }
    }

    private class GroupMembershipAdapter<T> extends ArrayAdapter<T> {
        private int mNewestGroupPosition;

        public GroupMembershipAdapter(Context context, int i) {
            super(context, i);
        }

        public boolean getItemIsCheckable(int i) {
            return true;
        }

        @Override
        public int getItemViewType(int i) {
            return !getItemIsCheckable(i) ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View view2 = super.getView(i, view, viewGroup);
            if (view2 == null) {
                return null;
            }
            CheckedTextView checkedTextView = (CheckedTextView) view2;
            if (!getItemIsCheckable(i)) {
                checkedTextView.setCheckMarkDrawable((Drawable) null);
            }
            checkedTextView.setTextColor(GroupMembershipView.this.mPrimaryTextColor);
            return checkedTextView;
        }

        public void setNewestGroupPosition(int i) {
            this.mNewestGroupPosition = i;
        }
    }

    public GroupMembershipView(Context context) {
        super(context);
        this.mListener = new GroupNameEditDialogFragment.Listener() {
            @Override
            public void onGroupNameEditCancelled() {
            }

            @Override
            public void onGroupNameEditCompleted(String str) {
                GroupMembershipView.this.mCreatedNewGroup = true;
            }
        };
    }

    public GroupMembershipView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mListener = new GroupNameEditDialogFragment.Listener() {
            @Override
            public void onGroupNameEditCancelled() {
            }

            @Override
            public void onGroupNameEditCompleted(String str) {
                GroupMembershipView.this.mCreatedNewGroup = true;
            }
        };
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = getContext().getResources();
        this.mPrimaryTextColor = resources.getColor(R.color.primary_text_color);
        this.mHintTextColor = resources.getColor(R.color.editor_disabled_text_color);
        this.mNoGroupString = getContext().getString(R.string.group_edit_field_hint_text);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void setGroupNameEditDialogFragment() {
        this.mGroupNameEditDialogFragment = (GroupNameEditDialogFragment) ((Activity) getContext()).getFragmentManager().findFragmentByTag("createGroupDialog");
        if (this.mGroupNameEditDialogFragment != null) {
            this.mGroupNameEditDialogFragment.setListener(this.mListener);
        }
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mGroupList != null) {
            this.mGroupList.setEnabled(z);
        }
    }

    public void setKind(DataKind dataKind) {
        this.mKind = dataKind;
        ((ImageView) findViewById(R.id.kind_icon)).setContentDescription(getResources().getString(dataKind.titleRes));
    }

    public void setGroupMetaData(Cursor cursor) {
        this.mGroupMetaData = cursor;
        updateView();
    }

    public boolean wasGroupMetaDataBound() {
        return this.mGroupMetaData != null;
    }

    public boolean accountHasGroups() {
        return this.mAccountHasGroups;
    }

    public void setState(RawContactDelta rawContactDelta) {
        this.mState = rawContactDelta;
        this.mAccountType = this.mState.getAccountType();
        this.mAccountName = this.mState.getAccountName();
        this.mDataSet = this.mState.getDataSet();
        this.mDefaultGroupVisibilityKnown = false;
        this.mCreatedNewGroup = false;
        updateView();
        setGroupNameEditDialogFragment();
    }

    private void updateView() {
        boolean z;
        if (this.mGroupMetaData == null || this.mGroupMetaData.isClosed() || this.mAccountType == null || this.mAccountName == null) {
            setVisibility(8);
            return;
        }
        this.mFavoritesGroupId = 0L;
        this.mDefaultGroupId = 0L;
        StringBuilder sb = new StringBuilder();
        this.mGroupMetaData.moveToPosition(-1);
        while (true) {
            z = false;
            if (!this.mGroupMetaData.moveToNext()) {
                break;
            }
            String string = this.mGroupMetaData.getString(0);
            String string2 = this.mGroupMetaData.getString(1);
            String string3 = this.mGroupMetaData.getString(2);
            if (string.equals(this.mAccountName) && string2.equals(this.mAccountType) && Objects.equal(string3, this.mDataSet)) {
                long j = this.mGroupMetaData.getLong(3);
                if (!this.mGroupMetaData.isNull(6) && this.mGroupMetaData.getInt(6) != 0) {
                    this.mFavoritesGroupId = j;
                } else if (!this.mGroupMetaData.isNull(5) && this.mGroupMetaData.getInt(5) != 0) {
                    this.mDefaultGroupId = j;
                } else {
                    this.mAccountHasGroups = true;
                }
                if (j != this.mFavoritesGroupId && j != this.mDefaultGroupId && hasMembership(j)) {
                    String string4 = this.mGroupMetaData.getString(4);
                    if (!TextUtils.isEmpty(string4)) {
                        if (sb.length() != 0) {
                            sb.append(", ");
                        }
                        sb.append(string4);
                    }
                }
            }
        }
        if (!this.mAccountHasGroups) {
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
            if (this.mDefaultGroupId != 0 && !hasMembership(this.mDefaultGroupId)) {
                z = true;
            }
            this.mDefaultGroupVisible = z;
            this.mDefaultGroupVisibilityKnown = true;
        }
    }

    @Override
    public void onClick(View view) {
        int i;
        if (UiClosables.closeQuietly(this.mPopup)) {
            this.mPopup = null;
            return;
        }
        requestFocus();
        this.mAdapter = new GroupMembershipAdapter<>(getContext(), R.layout.group_membership_list_item);
        long j = -1;
        this.mGroupMetaData.moveToPosition(-1);
        while (true) {
            if (!this.mGroupMetaData.moveToNext()) {
                break;
            }
            String string = this.mGroupMetaData.getString(0);
            String string2 = this.mGroupMetaData.getString(1);
            String string3 = this.mGroupMetaData.getString(2);
            if (string.equals(this.mAccountName) && string2.equals(this.mAccountType) && Objects.equal(string3, this.mDataSet)) {
                long j2 = this.mGroupMetaData.getLong(3);
                if (j2 != this.mFavoritesGroupId && (j2 != this.mDefaultGroupId || this.mDefaultGroupVisible)) {
                    if (j2 > j) {
                        this.mAdapter.setNewestGroupPosition(this.mAdapter.getCount());
                        j = j2;
                    }
                    String string4 = this.mGroupMetaData.getString(4);
                    boolean zHasMembership = hasMembership(j2);
                    Log.i("GroupMembershipView", "[onClick] checked : " + zHasMembership);
                    this.mAdapter.add(new GroupSelectionItem(j2, string4, zHasMembership));
                }
            }
        }
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
        for (i = 0; i < count; i++) {
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
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        ValuesDelta valuesDeltaInsertChild;
        Long groupRowId;
        ListView listView = (ListView) adapterView;
        int count = this.mAdapter.getCount();
        for (int i2 = 0; i2 < count; i2++) {
            this.mAdapter.getItem(i2).setChecked(listView.isItemChecked(i2));
        }
        ArrayList<ValuesDelta> mimeEntries = this.mState.getMimeEntries("vnd.android.cursor.item/group_membership");
        if (mimeEntries != null) {
            for (ValuesDelta valuesDelta : mimeEntries) {
                if (!valuesDelta.isDelete() && (groupRowId = valuesDelta.getGroupRowId()) != null && groupRowId.longValue() != this.mFavoritesGroupId && (groupRowId.longValue() != this.mDefaultGroupId || this.mDefaultGroupVisible)) {
                    if (!isGroupChecked(groupRowId.longValue())) {
                        valuesDelta.markDeleted();
                    }
                }
            }
        }
        for (int i3 = 0; i3 < count; i3++) {
            GroupSelectionItem item = this.mAdapter.getItem(i3);
            long groupId = item.getGroupId();
            if (item.isChecked() && !hasMembership(groupId) && (valuesDeltaInsertChild = RawContactModifier.insertChild(this.mState, this.mKind)) != null) {
                valuesDeltaInsertChild.setGroupRowId(groupId);
            }
        }
        updateView();
    }

    private boolean isGroupChecked(long j) {
        int count = this.mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            GroupSelectionItem item = this.mAdapter.getItem(i);
            if (j == item.getGroupId()) {
                return item.isChecked();
            }
        }
        return false;
    }

    private boolean hasMembership(long j) {
        Long groupRowId;
        if (j == this.mDefaultGroupId && this.mState.isContactInsert()) {
            return true;
        }
        ArrayList<ValuesDelta> mimeEntries = this.mState.getMimeEntries("vnd.android.cursor.item/group_membership");
        if (mimeEntries != null) {
            for (ValuesDelta valuesDelta : mimeEntries) {
                if (!valuesDelta.isDelete() && (groupRowId = valuesDelta.getGroupRowId()) != null && groupRowId.longValue() == j) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
