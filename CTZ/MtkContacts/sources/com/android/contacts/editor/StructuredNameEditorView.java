package com.android.contacts.editor;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import com.android.contacts.R;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.dataitem.DataItem;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.model.dataitem.StructuredNameDataItem;
import com.mediatek.contacts.editor.ContactEditorUtilsEx;
import com.mediatek.contacts.util.Log;

public class StructuredNameEditorView extends TextFieldsEditorView {
    private boolean mChanged;
    private TextFieldsEditorView mPhoneticView;
    private StructuredNameDataItem mSnapshot;

    public StructuredNameEditorView(Context context) {
        super(context);
    }

    public StructuredNameEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public StructuredNameEditorView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = getResources();
        this.mCollapseButtonDescription = resources.getString(R.string.collapse_name_fields_description);
        this.mExpandButtonDescription = resources.getString(R.string.expand_name_fields_description);
    }

    @Override
    public void setValues(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta, boolean z, ViewIdGenerator viewIdGenerator) {
        Log.sensitive("StructuredNameEditorView", "[setValues] DataKind=" + dataKind + ", entry=" + valuesDelta + ", state=" + rawContactDelta);
        super.setValues(dataKind, valuesDelta, rawContactDelta, z, viewIdGenerator);
        if (this.mSnapshot == null) {
            this.mSnapshot = (StructuredNameDataItem) DataItem.createFrom(new ContentValues(getValues().getCompleteValues()));
            this.mChanged = valuesDelta.isInsert();
        } else {
            this.mChanged = false;
        }
        updateEmptiness();
        this.mDeleteContainer.setVisibility(8);
    }

    @Override
    public void onFieldChanged(String str, String str2) {
        Log.d("StructuredNameEditorView", "[onFieldChanged] beg, column=" + str + ", value=" + Log.anonymize(str2));
        if (!isFieldChanged(str, str2)) {
            return;
        }
        saveValue(str, str2);
        this.mChanged = true;
        notifyEditorListener();
    }

    @Override
    public void updatePhonetic(String str, String str2) {
        EditText editText;
        if (this.mPhoneticView != null) {
            ViewGroup viewGroup = (ViewGroup) this.mPhoneticView.findViewById(R.id.editors);
            if ("data3".equals(str)) {
                editText = (EditText) viewGroup.getChildAt(0);
            } else if ("data2".equals(str)) {
                editText = (EditText) viewGroup.getChildAt(2);
            } else if ("data5".equals(str)) {
                editText = (EditText) viewGroup.getChildAt(1);
            } else {
                editText = null;
            }
            if (editText != null) {
                editText.setText(str2);
            }
        }
    }

    @Override
    public String getPhonetic(String str) {
        EditText editText;
        if (this.mPhoneticView == null) {
            return "";
        }
        ViewGroup viewGroup = (ViewGroup) this.mPhoneticView.findViewById(R.id.editors);
        if ("data3".equals(str)) {
            editText = (EditText) viewGroup.getChildAt(0);
        } else if ("data2".equals(str)) {
            editText = (EditText) viewGroup.getChildAt(2);
        } else if ("data5".equals(str)) {
            editText = (EditText) viewGroup.getChildAt(1);
        } else {
            editText = null;
        }
        if (editText == null) {
            return "";
        }
        return editText.getText().toString();
    }

    public void setPhoneticView(TextFieldsEditorView textFieldsEditorView) {
        this.mPhoneticView = textFieldsEditorView;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mChanged = this.mChanged;
        savedState.mSnapshot = this.mSnapshot.getContentValues();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.mSuperState);
        this.mChanged = savedState.mChanged;
        this.mSnapshot = ContactEditorUtilsEx.restoreStructuredNameDataItem(savedState.mSnapshot);
    }

    private static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        public boolean mChanged;
        public ContentValues mSnapshot;
        public Parcelable mSuperState;

        SavedState(Parcelable parcelable) {
            this.mSuperState = parcelable;
        }

        private SavedState(Parcel parcel) {
            ClassLoader classLoader = getClass().getClassLoader();
            this.mSuperState = parcel.readParcelable(classLoader);
            this.mChanged = parcel.readInt() != 0;
            this.mSnapshot = (ContentValues) parcel.readParcelable(classLoader);
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.mSuperState, 0);
            parcel.writeInt(this.mChanged ? 1 : 0);
            parcel.writeParcelable(this.mSnapshot, 0);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
