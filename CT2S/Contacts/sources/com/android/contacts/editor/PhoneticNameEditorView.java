package com.android.contacts.editor;

import android.content.Context;
import android.util.AttributeSet;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.model.dataitem.StructuredNameDataItem;
import com.android.contacts.common.util.NameConverter;

public class PhoneticNameEditorView extends TextFieldsEditorView {

    private static class PhoneticValuesDelta extends ValuesDelta {
        private String mPhoneticName;
        private ValuesDelta mValues;

        public PhoneticValuesDelta(ValuesDelta values) {
            this.mValues = values;
            buildPhoneticName();
        }

        @Override
        public void put(String key, String value) {
            if (key.equals("#phoneticName")) {
                this.mPhoneticName = value;
                parsePhoneticName(value);
            } else {
                this.mValues.put(key, value);
                buildPhoneticName();
            }
        }

        @Override
        public String getAsString(String key) {
            return key.equals("#phoneticName") ? this.mPhoneticName : this.mValues.getAsString(key);
        }

        private void parsePhoneticName(String value) {
            StructuredNameDataItem dataItem = NameConverter.parsePhoneticName(value, null);
            this.mValues.setPhoneticFamilyName(dataItem.getPhoneticFamilyName());
            this.mValues.setPhoneticMiddleName(dataItem.getPhoneticMiddleName());
            this.mValues.setPhoneticGivenName(dataItem.getPhoneticGivenName());
        }

        private void buildPhoneticName() {
            String family = this.mValues.getPhoneticFamilyName();
            String middle = this.mValues.getPhoneticMiddleName();
            String given = this.mValues.getPhoneticGivenName();
            this.mPhoneticName = NameConverter.buildPhoneticName(family, middle, given);
        }

        @Override
        public Long getId() {
            return this.mValues.getId();
        }

        @Override
        public boolean isVisible() {
            return this.mValues.isVisible();
        }
    }

    public static boolean isUnstructuredPhoneticNameColumn(String column) {
        return "#phoneticName".equals(column);
    }

    public PhoneticNameEditorView(Context context) {
        super(context);
    }

    public PhoneticNameEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PhoneticNameEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        if (!(entry instanceof PhoneticValuesDelta)) {
            entry = new PhoneticValuesDelta(entry);
        }
        super.setValues(kind, entry, state, readOnly, vig);
        updateEmptiness();
    }

    @Override
    public void onFieldChanged(String column, String value) {
        if (isFieldChanged(column, value)) {
            if (hasShortAndLongForms()) {
                boolean isEditingUnstructuredPhoneticName = !areOptionalFieldsVisible();
                if (isEditingUnstructuredPhoneticName == isUnstructuredPhoneticNameColumn(column)) {
                    super.onFieldChanged(column, value);
                    return;
                }
                return;
            }
            super.onFieldChanged(column, value);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), 0);
    }
}
