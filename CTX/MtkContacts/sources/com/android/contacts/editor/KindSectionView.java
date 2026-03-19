package com.android.contacts.editor;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.editor.Editor;
import com.android.contacts.editor.RawContactEditorView;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.RawContactModifier;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.preference.ContactsPreferences;
import com.mediatek.contacts.GlobalEnv;
import com.mediatek.contacts.util.AccountTypeUtils;
import com.mediatek.contacts.util.Log;
import java.util.ArrayList;
import java.util.List;

public class KindSectionView extends LinearLayout {
    private ViewGroup mEditors;
    private boolean mHideIfEmpty;
    private ImageView mIcon;
    private boolean mIsIccAccount;
    private boolean mIsUserProfile;
    private KindSectionData mKindSectionData;
    private LayoutInflater mLayoutInflater;
    private RawContactEditorView.Listener mListener;
    private boolean mShowOneEmptyEditor;
    private ViewIdGenerator mViewIdGenerator;

    private static final class StructuredNameEditorListener implements Editor.EditorListener {
        private final RawContactEditorView.Listener mListener;
        private final long mRawContactId;
        private final ValuesDelta mValuesDelta;

        public StructuredNameEditorListener(ValuesDelta valuesDelta, long j, RawContactEditorView.Listener listener) {
            this.mValuesDelta = valuesDelta;
            this.mRawContactId = j;
            this.mListener = listener;
        }

        @Override
        public void onRequest(int i) {
            if (i == 2) {
                this.mValuesDelta.setSuperPrimary(true);
                if (this.mListener != null) {
                    this.mListener.onNameFieldChanged(this.mRawContactId, this.mValuesDelta);
                    return;
                }
                return;
            }
            if (i == 3) {
                this.mValuesDelta.setSuperPrimary(false);
            }
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            editor.clearAllFields();
        }
    }

    private static final class OtherNameKindEditorListener implements Editor.EditorListener {
        private OtherNameKindEditorListener() {
        }

        @Override
        public void onRequest(int i) {
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            editor.clearAllFields();
        }
    }

    private class NonNameEditorListener implements Editor.EditorListener {
        private NonNameEditorListener() {
        }

        @Override
        public void onRequest(int i) {
            if (i == 3 || i == 4) {
                KindSectionView.this.updateEmptyEditors(true);
            }
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            if ((KindSectionView.this.mShowOneEmptyEditor && KindSectionView.this.mEditors.getChildCount() == 1) || GlobalEnv.getSimAasEditor().updateView(KindSectionView.this.mKindSectionData.getRawContactDelta(), null, null, 3)) {
                editor.clearAllFields();
            } else {
                editor.deleteEditor();
            }
        }
    }

    private class EventEditorListener extends NonNameEditorListener {
        private EventEditorListener() {
            super();
        }

        @Override
        public void onRequest(int i) {
            super.onRequest(i);
        }

        @Override
        public void onDeleteRequested(Editor editor) {
            if ((editor instanceof EventFieldEditorView) && ((EventFieldEditorView) editor).isBirthdayType() && KindSectionView.this.mEditors.getChildCount() > 1) {
                ((EventFieldEditorView) KindSectionView.this.mEditors.getChildAt(KindSectionView.this.mEditors.getChildCount() - 1)).restoreBirthday();
            }
            super.onDeleteRequested(editor);
        }
    }

    public KindSectionView(Context context) {
        this(context, null);
    }

    public KindSectionView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mShowOneEmptyEditor = false;
        this.mHideIfEmpty = true;
        this.mIsIccAccount = false;
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        if (this.mEditors != null) {
            int childCount = this.mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                this.mEditors.getChildAt(i).setEnabled(z);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);
        this.mLayoutInflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        this.mIcon = (ImageView) findViewById(R.id.kind_icon);
    }

    public void setIsUserProfile(boolean z) {
        this.mIsUserProfile = z;
    }

    public void setShowOneEmptyEditor(boolean z) {
        this.mShowOneEmptyEditor = z;
    }

    public void setHideWhenEmpty(boolean z) {
        this.mHideIfEmpty = z;
    }

    public void setGroupMetaData(Cursor cursor) {
        for (int i = 0; i < this.mEditors.getChildCount(); i++) {
            View childAt = this.mEditors.getChildAt(i);
            if (childAt instanceof GroupMembershipView) {
                ((GroupMembershipView) childAt).setGroupMetaData(cursor);
            }
        }
    }

    public StructuredNameEditorView getNameEditorView() {
        if (!"vnd.android.cursor.item/name".equals(this.mKindSectionData.getMimeType()) || this.mEditors.getChildCount() == 0) {
            return null;
        }
        return (StructuredNameEditorView) this.mEditors.getChildAt(0);
    }

    public TextFieldsEditorView getPhoneticEditorView() {
        if (!"vnd.android.cursor.item/name".equals(this.mKindSectionData.getMimeType())) {
            return null;
        }
        for (int i = 0; i < this.mEditors.getChildCount(); i++) {
            View childAt = this.mEditors.getChildAt(i);
            if (!(childAt instanceof StructuredNameEditorView)) {
                return (TextFieldsEditorView) childAt;
            }
        }
        return null;
    }

    public void setState(KindSectionData kindSectionData, ViewIdGenerator viewIdGenerator, RawContactEditorView.Listener listener) {
        Log.d("KindSectionView", "[setState] beg");
        this.mKindSectionData = kindSectionData;
        this.mViewIdGenerator = viewIdGenerator;
        this.mListener = listener;
        DataKind dataKind = this.mKindSectionData.getDataKind();
        if (dataKind != null) {
            this.mIcon.setImageDrawable(EditorUiUtils.getMimeTypeDrawable(getContext(), dataKind.mimeType));
            if (this.mIcon.getDrawable() != null) {
                this.mIcon.setContentDescription((dataKind.titleRes == -1 || dataKind.titleRes == 0) ? "" : getResources().getString(dataKind.titleRes));
            }
        }
        rebuildFromState();
        updateEmptyEditors(false);
        Log.d("KindSectionView", "[setState] end");
    }

    private void rebuildFromState() {
        Editor.EditorListener nonNameEditorListener;
        Log.d("KindSectionView", "[rebuildFromState] beg");
        this.mEditors.removeAllViews();
        String mimeType = this.mKindSectionData.getMimeType();
        if ("vnd.android.cursor.item/name".equals(mimeType)) {
            addNameEditorViews(this.mKindSectionData.getAccountType(), this.mKindSectionData.getRawContactDelta());
        } else if ("vnd.android.cursor.item/group_membership".equals(mimeType)) {
            addGroupEditorView(this.mKindSectionData.getRawContactDelta(), this.mKindSectionData.getDataKind());
        } else {
            if ("vnd.android.cursor.item/nickname".equals(mimeType)) {
                nonNameEditorListener = new OtherNameKindEditorListener();
            } else if ("vnd.android.cursor.item/contact_event".equals(mimeType)) {
                nonNameEditorListener = new EventEditorListener();
            } else {
                nonNameEditorListener = new NonNameEditorListener();
            }
            List<ValuesDelta> visibleValuesDeltas = this.mKindSectionData.getVisibleValuesDeltas();
            for (int i = 0; i < visibleValuesDeltas.size(); i++) {
                addNonNameEditorView(this.mKindSectionData.getRawContactDelta(), this.mKindSectionData.getDataKind(), visibleValuesDeltas.get(i), nonNameEditorListener);
            }
        }
        Log.d("KindSectionView", "[rebuildFromState] end");
    }

    private void addNameEditorViews(AccountType accountType, RawContactDelta rawContactDelta) {
        boolean z = !accountType.areContactsWritable();
        ValuesDelta superPrimaryEntry = rawContactDelta.getSuperPrimaryEntry("vnd.android.cursor.item/name");
        Log.sensitive("KindSectionView", "[addNameEditorViews] nameValuesDelta=" + superPrimaryEntry + ", readOnly = " + z);
        if (z) {
            View viewInflate = this.mLayoutInflater.inflate(R.layout.structured_name_readonly_editor_view, this.mEditors, false);
            ((TextView) viewInflate.findViewById(R.id.display_name)).setText(superPrimaryEntry.getDisplayName());
            LinearLayout linearLayout = (LinearLayout) viewInflate.findViewById(R.id.account_type);
            linearLayout.setVisibility(0);
            ((ImageView) linearLayout.findViewById(R.id.account_type_icon)).setImageDrawable(accountType.getDisplayIcon(getContext()));
            ((TextView) linearLayout.findViewById(R.id.account_type_name)).setText(accountType.getDisplayLabel(getContext()));
            this.mEditors.addView(viewInflate);
            return;
        }
        StructuredNameEditorView structuredNameEditorView = (StructuredNameEditorView) this.mLayoutInflater.inflate(R.layout.structured_name_editor_view, this.mEditors, false);
        if (!this.mIsUserProfile) {
            structuredNameEditorView.setEditorListener(new StructuredNameEditorListener(superPrimaryEntry, rawContactDelta.getRawContactId().longValue(), this.mListener));
        }
        structuredNameEditorView.setDeletable(false);
        structuredNameEditorView.setValues(accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_NAME), superPrimaryEntry, rawContactDelta, false, this.mViewIdGenerator);
        structuredNameEditorView.findViewById(R.id.kind_icon).setVisibility(8);
        this.mEditors.addView(structuredNameEditorView);
        if (accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME) == null) {
            return;
        }
        TextFieldsEditorView textFieldsEditorView = (TextFieldsEditorView) this.mLayoutInflater.inflate(R.layout.text_fields_editor_view, this.mEditors, false);
        textFieldsEditorView.setEditorListener(new OtherNameKindEditorListener());
        textFieldsEditorView.setDeletable(false);
        textFieldsEditorView.setValues(accountType.getKindForMimetype(DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME), superPrimaryEntry, rawContactDelta, false, this.mViewIdGenerator);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-1, -2);
        layoutParams.setMargins(0, 0, 0, 0);
        textFieldsEditorView.setLayoutParams(layoutParams);
        this.mEditors.addView(textFieldsEditorView);
        this.mHideIfEmpty = new ContactsPreferences(getContext()).shouldHidePhoneticNamesIfEmpty();
    }

    private void addGroupEditorView(RawContactDelta rawContactDelta, DataKind dataKind) {
        Log.d("KindSectionView", "[addGroupEditorView] beg");
        GroupMembershipView groupMembershipView = (GroupMembershipView) this.mLayoutInflater.inflate(R.layout.item_group_membership, this.mEditors, false);
        groupMembershipView.setKind(dataKind);
        groupMembershipView.setEnabled(isEnabled());
        groupMembershipView.setState(rawContactDelta);
        groupMembershipView.findViewById(R.id.kind_icon).setVisibility(8);
        this.mEditors.addView(groupMembershipView);
        Log.d("KindSectionView", "[addGroupEditorView] end");
    }

    private View addNonNameEditorView(RawContactDelta rawContactDelta, DataKind dataKind, ValuesDelta valuesDelta, Editor.EditorListener editorListener) {
        View viewInflate = this.mLayoutInflater.inflate(EditorUiUtils.getLayoutResourceId(dataKind.mimeType), this.mEditors, false);
        viewInflate.setEnabled(isEnabled());
        if (viewInflate instanceof Editor) {
            Editor editor = (Editor) viewInflate;
            editor.setDeletable(true);
            editor.setEditorListener(editorListener);
            editor.setValues(dataKind, valuesDelta, rawContactDelta, !dataKind.editable, this.mViewIdGenerator);
        }
        this.mEditors.addView(viewInflate);
        return viewInflate;
    }

    public void updateEmptyEditors(boolean z) {
        boolean zEquals = "vnd.android.cursor.item/name".equals(this.mKindSectionData.getMimeType());
        boolean zEquals2 = "vnd.android.cursor.item/group_membership".equals(this.mKindSectionData.getMimeType());
        Log.sensitive("KindSectionView", "[updateEmptyEditors] isNameKindSection = " + zEquals + ", isGroupKindSection = " + zEquals2 + ", mHideIfEmpty = " + this.mHideIfEmpty);
        if (zEquals) {
            setVisibility(0);
            updateEmptyNameEditors(z);
            return;
        }
        if (zEquals2) {
            for (int i = 0; i < this.mEditors.getChildCount(); i++) {
                View childAt = this.mEditors.getChildAt(i);
                if (childAt instanceof GroupMembershipView) {
                    GroupMembershipView groupMembershipView = (GroupMembershipView) childAt;
                    if (!groupMembershipView.wasGroupMetaDataBound() || !groupMembershipView.accountHasGroups() || this.mIsUserProfile) {
                        setVisibility(8);
                        return;
                    }
                }
            }
            if (this.mHideIfEmpty) {
                setVisibility(8);
                return;
            } else {
                setVisibility(0);
                return;
            }
        }
        if (this.mEditors.getChildCount() == getEmptyEditors().size() && this.mHideIfEmpty) {
            setVisibility(8);
        } else {
            setVisibility(0);
            updateEmptyNonNameEditors(z);
        }
    }

    private void updateEmptyNameEditors(boolean z) {
        boolean z2 = false;
        for (int i = 0; i < this.mEditors.getChildCount(); i++) {
            View childAt = this.mEditors.getChildAt(i);
            if (childAt instanceof Editor) {
                Editor editor = (Editor) childAt;
                boolean z3 = true;
                if (childAt instanceof StructuredNameEditorView) {
                    if (editor.isEmpty()) {
                        if (z2) {
                            if (this.mHideIfEmpty) {
                                childAt.setVisibility(8);
                            }
                        }
                    } else {
                        showView(childAt, z);
                    }
                    z2 = z3;
                } else if (this.mHideIfEmpty && editor.isEmpty()) {
                    hideView(childAt);
                } else {
                    showView(childAt, false);
                    Log.d("KindSectionView", "[updateEmptyNameEditors] is dismiss Phonetic Name fields : " + this.mIsIccAccount);
                    if (this.mIsIccAccount) {
                        childAt.setVisibility(8);
                    }
                }
                z3 = z2;
                z2 = z3;
            } else if (this.mHideIfEmpty) {
                hideView(childAt);
            } else {
                showView(childAt, z);
            }
        }
    }

    private void updateEmptyNonNameEditors(boolean z) {
        int maxEmptyEditors;
        List<View> emptyEditors = getEmptyEditors();
        if (this.mKindSectionData.getMimeType() != null) {
            maxEmptyEditors = GlobalEnv.getSimAasEditor().getMaxEmptyEditors(this.mKindSectionData.getRawContactDelta(), this.mKindSectionData.getMimeType());
            if (AccountTypeUtils.isUsimOrCsim(this.mKindSectionData.getRawContactDelta().getAccountType()) && AccountTypeUtils.isPhoneNumType(this.mKindSectionData.getMimeType())) {
                this.mKindSectionData.getDataKind().typeOverallMax = maxEmptyEditors;
                Log.d("KindSectionView", "[updateEmptyNonNameEditors] set aas typeOverallMax = max = " + maxEmptyEditors);
            }
        } else {
            maxEmptyEditors = 1;
        }
        Log.d("KindSectionView", "[updateEmptyNonNameEditors] max =" + maxEmptyEditors + " emptyEditors.size()=" + emptyEditors.size() + ", mEditors=" + this.mEditors.getChildCount());
        if (emptyEditors.size() > maxEmptyEditors) {
            int i = 0;
            for (int i2 = 0; i2 < emptyEditors.size(); i2++) {
                View view = emptyEditors.get(i2);
                if (view.findFocus() == null) {
                    deleteView(view, z);
                    i++;
                    if (i == emptyEditors.size() - 1) {
                        return;
                    }
                }
            }
            return;
        }
        DataKind dataKind = this.mKindSectionData.getDataKind();
        RawContactDelta rawContactDelta = this.mKindSectionData.getRawContactDelta();
        if (dataKind != null && RawContactModifier.canInsert(rawContactDelta, dataKind) && emptyEditors.size() != 1 && this.mShowOneEmptyEditor) {
            String mimeType = this.mKindSectionData.getMimeType();
            if ("vnd.android.cursor.item/nickname".equals(mimeType) && this.mEditors.getChildCount() > 0) {
                return;
            }
            ValuesDelta valuesDeltaInsertChild = RawContactModifier.insertChild(rawContactDelta, dataKind);
            if (valuesDeltaInsertChild != null && valuesDeltaInsertChild.containsKey("data2") && valuesDeltaInsertChild.getAsInteger("data2", -1).intValue() == 101) {
                Log.sensitive("KindSectionView", "[updateEmptyNonNameEditors] Convert to Aas:" + valuesDeltaInsertChild.toString());
                valuesDeltaInsertChild.put("is_additional_number", 1);
            }
            showView(addNonNameEditorView(rawContactDelta, dataKind, valuesDeltaInsertChild, "vnd.android.cursor.item/contact_event".equals(mimeType) ? new EventEditorListener() : new NonNameEditorListener()), z);
        }
    }

    private void hideView(View view) {
        view.setVisibility(8);
    }

    private void deleteView(View view, boolean z) {
        if (z) {
            ((Editor) view).deleteEditor();
        } else {
            this.mEditors.removeView(view);
        }
    }

    private void showView(View view, boolean z) {
        if (z) {
            view.setVisibility(8);
            EditorAnimator.getInstance().showFieldFooter(view);
        } else {
            view.setVisibility(0);
        }
    }

    private List<View> getEmptyEditors() {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < this.mEditors.getChildCount(); i++) {
            KeyEvent.Callback childAt = this.mEditors.getChildAt(i);
            if ((childAt instanceof Editor) && ((Editor) childAt).isEmpty()) {
                arrayList.add(childAt);
            }
        }
        return arrayList;
    }

    public void setIsCurrentIccAccount(boolean z) {
        this.mIsIccAccount = z;
    }
}
