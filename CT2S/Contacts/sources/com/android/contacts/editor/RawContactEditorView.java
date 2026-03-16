package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.SimAccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Iterator;

public class RawContactEditorView extends BaseRawContactEditorView {
    private View mAccountHeader;
    private TextView mAccountHeaderNameTextView;
    private TextView mAccountHeaderTypeTextView;
    private View mAccountSelector;
    private TextView mAccountSelectorNameTextView;
    private TextView mAccountSelectorTypeTextView;
    private boolean mAutoAddToDefaultGroup;
    private ViewGroup mFields;
    private DataKind mGroupMembershipKind;
    private GroupMembershipView mGroupMembershipView;
    private Cursor mGroupMetaData;
    private LayoutInflater mInflater;
    private StructuredNameEditorView mName;
    private TextFieldsEditorView mNickName;
    private PhoneticNameEditorView mPhoneticName;
    private long mRawContactId;
    private RawContactDelta mState;

    public RawContactEditorView(Context context) {
        super(context);
        this.mRawContactId = -1L;
        this.mAutoAddToDefaultGroup = true;
    }

    public RawContactEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRawContactId = -1L;
        this.mAutoAddToDefaultGroup = true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        View view = getPhotoEditor();
        if (view != null) {
            view.setEnabled(enabled);
        }
        if (this.mName != null) {
            this.mName.setEnabled(enabled);
        }
        if (this.mPhoneticName != null) {
            this.mPhoneticName.setEnabled(enabled);
        }
        if (this.mFields != null) {
            int count = this.mFields.getChildCount();
            for (int i = 0; i < count; i++) {
                this.mFields.getChildAt(i).setEnabled(enabled);
            }
        }
        if (this.mGroupMembershipView != null) {
            this.mGroupMembershipView.setEnabled(enabled);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mName = (StructuredNameEditorView) findViewById(R.id.edit_name);
        this.mName.setDeletable(false);
        this.mPhoneticName = (PhoneticNameEditorView) findViewById(R.id.edit_phonetic_name);
        this.mPhoneticName.setDeletable(false);
        this.mNickName = (TextFieldsEditorView) findViewById(R.id.edit_nick_name);
        this.mFields = (ViewGroup) findViewById(R.id.sect_fields);
        this.mAccountHeader = findViewById(R.id.account_header_container);
        this.mAccountHeaderTypeTextView = (TextView) findViewById(R.id.account_type);
        this.mAccountHeaderNameTextView = (TextView) findViewById(R.id.account_name);
        this.mAccountSelector = findViewById(R.id.account_selector_container);
        this.mAccountSelectorTypeTextView = (TextView) findViewById(R.id.account_type_selector);
        this.mAccountSelectorNameTextView = (TextView) findViewById(R.id.account_name_selector);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superInstanceState", super.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("superInstanceState"));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public void setState(RawContactDelta state, AccountType type, ViewIdGenerator vig, boolean isProfile) {
        this.mState = state;
        this.mFields.removeAllViews();
        if (state != null && type != null) {
            setId(vig.getId(state, null, null, -1));
            RawContactModifier.ensureKindExists(state, type, "vnd.android.cursor.item/name");
            this.mRawContactId = state.getRawContactId().longValue();
            if (isProfile) {
                String accountName = state.getAccountName();
                if (TextUtils.isEmpty(accountName)) {
                    this.mAccountHeaderNameTextView.setVisibility(8);
                    this.mAccountHeaderTypeTextView.setText(R.string.local_profile_title);
                } else {
                    this.mAccountHeaderTypeTextView.setText(this.mContext.getString(R.string.external_profile_title, type.getDisplayLabel(this.mContext)));
                    this.mAccountHeaderNameTextView.setText(accountName);
                }
            } else {
                String accountName2 = state.getAccountName();
                CharSequence accountType = type.getDisplayLabel(this.mContext);
                if (TextUtils.isEmpty(accountType)) {
                    accountType = this.mContext.getString(R.string.account_phone);
                }
                if (!TextUtils.isEmpty(accountName2)) {
                    this.mAccountHeaderNameTextView.setVisibility(0);
                    this.mAccountHeaderNameTextView.setText(this.mContext.getString(R.string.from_account_format, accountName2));
                } else {
                    this.mAccountHeaderNameTextView.setVisibility(8);
                }
                this.mAccountHeaderTypeTextView.setText(this.mContext.getString(R.string.account_type_format, accountType));
            }
            Log.d("RawContactEditorView", " isProfile = " + isProfile);
            updateAccountHeaderContentDescription();
            this.mAccountSelectorTypeTextView.setText(this.mAccountHeaderTypeTextView.getText());
            this.mAccountSelectorTypeTextView.setVisibility(this.mAccountHeaderTypeTextView.getVisibility());
            this.mAccountSelectorNameTextView.setText(this.mAccountHeaderNameTextView.getText());
            this.mAccountSelectorNameTextView.setVisibility(this.mAccountHeaderNameTextView.getVisibility());
            this.mAccountHeader.setVisibility(this.mAccountSelector.getVisibility() == 8 ? 0 : 8);
            RawContactModifier.ensureKindExists(state, type, "vnd.android.cursor.item/photo");
            setHasPhotoEditor(type.getKindForMimetype("vnd.android.cursor.item/photo") != null);
            getPhotoEditor().setEnabled(isEnabled());
            this.mName.setEnabled(isEnabled());
            this.mPhoneticName.setEnabled(isEnabled());
            this.mFields.setVisibility(0);
            this.mName.setVisibility(0);
            this.mPhoneticName.setVisibility(0);
            this.mGroupMembershipKind = type.getKindForMimetype("vnd.android.cursor.item/group_membership");
            if (this.mGroupMembershipKind != null) {
                this.mGroupMembershipView = (GroupMembershipView) this.mInflater.inflate(R.layout.item_group_membership, this.mFields, false);
                this.mGroupMembershipView.setKind(this.mGroupMembershipKind);
                this.mGroupMembershipView.setEnabled(isEnabled());
            }
            for (DataKind kind : type.getSortedDataKinds()) {
                if (kind.editable) {
                    String mimeType = kind.mimeType;
                    if ("vnd.android.cursor.item/name".equals(mimeType)) {
                        ValuesDelta primary = state.getPrimaryEntry(mimeType);
                        this.mName.setValues(type.getKindForMimetype("#displayName"), primary, state, false, vig);
                        if (state.getAccountName() != null && !state.getAccountName().equals(SimAccountType.ACCOUNT_NAME) && !state.getAccountName().equals("Sim2")) {
                            this.mPhoneticName.setValues(type.getKindForMimetype("#phoneticName"), primary, state, false, vig);
                        } else {
                            this.mPhoneticName.setEnabled(false);
                        }
                        DataKind nickNameKind = type.getKindForMimetype("vnd.android.cursor.item/nickname");
                        if (nickNameKind != null) {
                            ValuesDelta primaryNickNameEntry = state.getPrimaryEntry(nickNameKind.mimeType);
                            if (primaryNickNameEntry == null) {
                                primaryNickNameEntry = RawContactModifier.insertChild(state, nickNameKind);
                            }
                            this.mNickName.setValues(nickNameKind, primaryNickNameEntry, state, false, vig);
                            this.mNickName.setDeletable(false);
                        } else {
                            this.mPhoneticName.setPadding(0, 0, 0, (int) getResources().getDimension(R.dimen.editor_padding_between_editor_views));
                            this.mNickName.setVisibility(8);
                        }
                    } else if ("vnd.android.cursor.item/photo".equals(mimeType)) {
                        getPhotoEditor().setValues(kind, state.getPrimaryEntry(mimeType), state, false, vig);
                    } else if ("vnd.android.cursor.item/group_membership".equals(mimeType)) {
                        if (this.mGroupMembershipView != null) {
                            this.mGroupMembershipView.setState(state);
                            this.mFields.addView(this.mGroupMembershipView);
                        }
                    } else if (!"#displayName".equals(mimeType) && !"#phoneticName".equals(mimeType) && !"vnd.android.cursor.item/nickname".equals(mimeType) && kind.fieldList != null) {
                        KindSectionView section = (KindSectionView) this.mInflater.inflate(R.layout.item_kind_section, this.mFields, false);
                        section.setEnabled(isEnabled());
                        section.setState(kind, state, false, vig);
                        this.mFields.addView(section);
                    }
                }
            }
            addToDefaultGroupIfNeeded();
        }
    }

    @Override
    public void setGroupMetaData(Cursor groupMetaData) {
        this.mGroupMetaData = groupMetaData;
        addToDefaultGroupIfNeeded();
        if (this.mGroupMembershipView != null) {
            this.mGroupMembershipView.setGroupMetaData(groupMetaData);
        }
    }

    public void setAutoAddToDefaultGroup(boolean flag) {
        this.mAutoAddToDefaultGroup = flag;
    }

    private void addToDefaultGroupIfNeeded() {
        ValuesDelta entry;
        if (this.mAutoAddToDefaultGroup && this.mGroupMetaData != null && !this.mGroupMetaData.isClosed() && this.mState != null) {
            boolean hasGroupMembership = false;
            ArrayList<ValuesDelta> entries = this.mState.getMimeEntries("vnd.android.cursor.item/group_membership");
            if (entries != null) {
                Iterator<ValuesDelta> it = entries.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    ValuesDelta values = it.next();
                    Long id = values.getGroupRowId();
                    if (id != null && id.longValue() != 0) {
                        hasGroupMembership = true;
                        break;
                    }
                }
            }
            if (!hasGroupMembership) {
                long defaultGroupId = getDefaultGroupId();
                if (defaultGroupId != -1 && (entry = RawContactModifier.insertChild(this.mState, this.mGroupMembershipKind)) != null) {
                    entry.setGroupRowId(defaultGroupId);
                }
            }
        }
    }

    private long getDefaultGroupId() {
        String accountType = this.mState.getAccountType();
        String accountName = this.mState.getAccountName();
        String accountDataSet = this.mState.getDataSet();
        this.mGroupMetaData.moveToPosition(-1);
        while (this.mGroupMetaData.moveToNext()) {
            String name = this.mGroupMetaData.getString(0);
            String type = this.mGroupMetaData.getString(1);
            String dataSet = this.mGroupMetaData.getString(2);
            if (name.equals(accountName) && type.equals(accountType) && Objects.equal(dataSet, accountDataSet)) {
                long groupId = this.mGroupMetaData.getLong(3);
                if (!this.mGroupMetaData.isNull(5) && this.mGroupMetaData.getInt(5) != 0) {
                    return groupId;
                }
            }
        }
        return -1L;
    }

    public StructuredNameEditorView getNameEditor() {
        return this.mName;
    }

    public TextFieldsEditorView getPhoneticNameEditor() {
        return this.mPhoneticName;
    }

    public TextFieldsEditorView getNickNameEditor() {
        return this.mNickName;
    }

    @Override
    public long getRawContactId() {
        return this.mRawContactId;
    }
}
