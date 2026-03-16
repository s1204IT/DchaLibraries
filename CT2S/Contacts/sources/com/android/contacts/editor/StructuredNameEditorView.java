package com.android.contacts.editor;

import android.content.ContentValues;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import com.android.contacts.common.util.NameConverter;
import java.util.HashMap;
import java.util.Map;

public class StructuredNameEditorView extends TextFieldsEditorView {
    private boolean mChanged;
    private StructuredNameDataItem mSnapshot;

    public StructuredNameEditorView(Context context) {
        super(context);
    }

    public StructuredNameEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StructuredNameEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        super.setValues(kind, entry, state, readOnly, vig);
        if (this.mSnapshot == null) {
            this.mSnapshot = (StructuredNameDataItem) DataItem.createFrom(new ContentValues(getValues().getCompleteValues()));
            this.mChanged = entry.isInsert();
        } else {
            this.mChanged = false;
        }
        updateEmptiness();
    }

    @Override
    public void onFieldChanged(String column, String value) {
        if (isFieldChanged(column, value)) {
            saveValue(column, value);
            this.mChanged = true;
            if (hasShortAndLongForms()) {
                if (areOptionalFieldsVisible()) {
                    rebuildFullName(getValues());
                } else {
                    rebuildStructuredName(getValues());
                }
            }
            notifyEditorListener();
        }
    }

    @Override
    protected void onOptionalFieldVisibilityChange() {
        if (hasShortAndLongForms()) {
            if (areOptionalFieldsVisible()) {
                switchFromFullNameToStructuredName();
            } else {
                switchFromStructuredNameToFullName();
            }
        }
        super.onOptionalFieldVisibilityChange();
    }

    private void switchFromFullNameToStructuredName() {
        ValuesDelta values = getValues();
        if (!this.mChanged) {
            String[] arr$ = NameConverter.STRUCTURED_NAME_FIELDS;
            for (String field : arr$) {
                values.put(field, this.mSnapshot.getContentValues().getAsString(field));
            }
            return;
        }
        String displayName = values.getDisplayName();
        Map<String, String> structuredNameMap = NameConverter.displayNameToStructuredName(getContext(), displayName);
        if (!structuredNameMap.isEmpty()) {
            eraseFullName(values);
            for (String field2 : structuredNameMap.keySet()) {
                values.put(field2, structuredNameMap.get(field2));
            }
        }
        this.mSnapshot.getContentValues().clear();
        this.mSnapshot.getContentValues().putAll(values.getCompleteValues());
        this.mSnapshot.setDisplayName(displayName);
    }

    private void switchFromStructuredNameToFullName() {
        ValuesDelta values = getValues();
        if (!this.mChanged) {
            values.setDisplayName(this.mSnapshot.getDisplayName());
            return;
        }
        Map<String, String> structuredNameMap = valuesToStructuredNameMap(values);
        String displayName = NameConverter.structuredNameToDisplayName(getContext(), structuredNameMap);
        if (!TextUtils.isEmpty(displayName)) {
            eraseStructuredName(values);
            values.put("data1", displayName);
        }
        this.mSnapshot.getContentValues().clear();
        this.mSnapshot.setDisplayName(values.getDisplayName());
        this.mSnapshot.setMimeType("vnd.android.cursor.item/name");
        for (String field : structuredNameMap.keySet()) {
            this.mSnapshot.getContentValues().put(field, structuredNameMap.get(field));
        }
    }

    private Map<String, String> valuesToStructuredNameMap(ValuesDelta values) {
        Map<String, String> structuredNameMap = new HashMap<>();
        String[] arr$ = NameConverter.STRUCTURED_NAME_FIELDS;
        for (String key : arr$) {
            structuredNameMap.put(key, values.getAsString(key));
        }
        return structuredNameMap;
    }

    private void eraseFullName(ValuesDelta values) {
        values.setDisplayName(null);
    }

    private void rebuildFullName(ValuesDelta values) {
        Map<String, String> structuredNameMap = valuesToStructuredNameMap(values);
        String displayName = NameConverter.structuredNameToDisplayName(getContext(), structuredNameMap);
        values.setDisplayName(displayName);
    }

    private void eraseStructuredName(ValuesDelta values) {
        String[] arr$ = NameConverter.STRUCTURED_NAME_FIELDS;
        for (String field : arr$) {
            values.putNull(field);
        }
    }

    private void rebuildStructuredName(ValuesDelta values) {
        String displayName = values.getDisplayName();
        Map<String, String> structuredNameMap = NameConverter.displayNameToStructuredName(getContext(), displayName);
        for (String field : structuredNameMap.keySet()) {
            values.put(field, structuredNameMap.get(field));
        }
    }

    public void setDisplayName(String name) {
        super.setValue(0, name);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mChanged = this.mChanged;
        state.mSnapshot = this.mSnapshot.getContentValues();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.mSuperState);
        this.mChanged = ss.mChanged;
        this.mSnapshot = (StructuredNameDataItem) DataItem.createFrom(ss.mSnapshot);
    }

    private static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public boolean mChanged;
        public ContentValues mSnapshot;
        public Parcelable mSuperState;

        SavedState(Parcelable superState) {
            this.mSuperState = superState;
        }

        private SavedState(Parcel in) {
            ClassLoader loader = getClass().getClassLoader();
            this.mSuperState = in.readParcelable(loader);
            this.mChanged = in.readInt() != 0;
            this.mSnapshot = (ContentValues) in.readParcelable(loader);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeParcelable(this.mSuperState, 0);
            out.writeInt(this.mChanged ? 1 : 0);
            out.writeParcelable(this.mSnapshot, 0);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), 0);
    }
}
