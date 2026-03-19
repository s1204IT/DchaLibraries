package com.android.contacts.editor;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import com.android.contacts.R;
import com.android.contacts.datepicker.DatePicker;
import com.android.contacts.datepicker.DatePickerDialog;
import com.android.contacts.model.RawContactDelta;
import com.android.contacts.model.ValuesDelta;
import com.android.contacts.model.account.AccountType;
import com.android.contacts.model.dataitem.DataKind;
import com.android.contacts.util.DateUtils;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class EventFieldEditorView extends LabeledEditorView {
    private Button mDateView;
    private int mHintTextColor;
    private String mNoDateString;
    private int mPrimaryTextColor;

    public EventFieldEditorView(Context context) {
        super(context);
    }

    public EventFieldEditorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public EventFieldEditorView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = getContext().getResources();
        this.mPrimaryTextColor = resources.getColor(R.color.primary_text_color);
        this.mHintTextColor = resources.getColor(R.color.editor_disabled_text_color);
        this.mNoDateString = getContext().getString(R.string.event_edit_field_hint_text);
        this.mDateView = (Button) findViewById(R.id.date_view);
        this.mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventFieldEditorView.this.showDialog(R.id.dialog_event_date_picker);
            }
        });
    }

    @Override
    protected void requestFocusForFirstEditField() {
        this.mDateView.requestFocus();
    }

    @Override
    public void setEnabled(boolean z) {
        super.setEnabled(z);
        this.mDateView.setEnabled(!isReadOnly() && z);
    }

    @Override
    public void setValues(DataKind dataKind, ValuesDelta valuesDelta, RawContactDelta rawContactDelta, boolean z, ViewIdGenerator viewIdGenerator) {
        if (dataKind.fieldList.size() != 1) {
            throw new IllegalStateException("kind must have 1 field");
        }
        super.setValues(dataKind, valuesDelta, rawContactDelta, z, viewIdGenerator);
        this.mDateView.setEnabled(isEnabled() && !z);
        rebuildDateView();
        updateEmptiness();
    }

    private void rebuildDateView() {
        String date = DateUtils.formatDate(getContext(), getEntry().getAsString(getKind().fieldList.get(0).column), false);
        if (TextUtils.isEmpty(date)) {
            this.mDateView.setText(this.mNoDateString);
            this.mDateView.setTextColor(this.mHintTextColor);
            setDeleteButtonVisible(false);
        } else {
            this.mDateView.setText(date);
            this.mDateView.setTextColor(this.mPrimaryTextColor);
            setDeleteButtonVisible(true);
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(getEntry().getAsString(getKind().fieldList.get(0).column));
    }

    @Override
    public Dialog createDialog(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle must not be null");
        }
        if (bundle.getInt("dialog_id") == R.id.dialog_event_date_picker) {
            return createDatePickerDialog();
        }
        return super.createDialog(bundle);
    }

    @Override
    protected AccountType.EventEditType getType() {
        return (AccountType.EventEditType) super.getType();
    }

    @Override
    protected void onLabelRebuilt() {
        Date date;
        String str = getKind().fieldList.get(0).column;
        String asString = getEntry().getAsString(str);
        DataKind kind = getKind();
        Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
        boolean z = true;
        int i = calendar.get(1);
        if (getType() == null || !getType().isYearOptional()) {
            z = false;
        }
        if (!z && !TextUtils.isEmpty(asString)) {
            ParsePosition parsePosition = new ParsePosition(0);
            if (kind.dateFormatWithoutYear != null) {
                date = kind.dateFormatWithoutYear.parse(asString, parsePosition);
            } else {
                date = null;
            }
            if (date == null) {
                return;
            }
            calendar.setTime(date);
            calendar.set(i, calendar.get(2), calendar.get(5), 8, 0, 0);
            String str2 = kind.dateFormatWithYear != null ? kind.dateFormatWithYear.format(calendar.getTime()) : null;
            if (str2 == null) {
                return;
            }
            onFieldChanged(str, str2);
            rebuildDateView();
        }
    }

    private Dialog createDatePickerDialog() {
        int i;
        int i2;
        final String str = getKind().fieldList.get(0).column;
        String asString = getEntry().getAsString(str);
        final DataKind kind = getKind();
        Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
        int i3 = calendar.get(1);
        final boolean zIsYearOptional = getType().isYearOptional();
        if (TextUtils.isEmpty(asString)) {
            i2 = calendar.get(2);
            i = calendar.get(5);
        } else {
            Calendar date = DateUtils.parseDate(asString, false);
            if (date != null) {
                if (DateUtils.isYearSet(date)) {
                    i3 = date.get(1);
                } else if (zIsYearOptional) {
                    i3 = DatePickerDialog.NO_YEAR;
                }
                int i4 = date.get(2);
                i = date.get(5);
                i2 = i4;
            } else {
                return null;
            }
        }
        int i5 = i3;
        return new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int i6, int i7, int i8) {
                if (i6 == 0 && !zIsYearOptional) {
                    throw new IllegalStateException();
                }
                Calendar calendar2 = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
                calendar2.clear();
                calendar2.set(i6 == DatePickerDialog.NO_YEAR ? 2000 : i6, i7, i8, 8, 0, 0);
                String str2 = null;
                if (i6 == 0) {
                    if (kind.dateFormatWithoutYear != null) {
                        str2 = kind.dateFormatWithoutYear.format(calendar2.getTime());
                    }
                } else if (kind.dateFormatWithYear != null) {
                    str2 = kind.dateFormatWithYear.format(calendar2.getTime());
                }
                if (str2 == null) {
                    return;
                }
                EventFieldEditorView.this.onFieldChanged(str, str2);
                EventFieldEditorView.this.rebuildDateView();
            }
        }, i5, i2, i, zIsYearOptional);
    }

    @Override
    public void clearAllFields() {
        this.mDateView.setText(this.mNoDateString);
        this.mDateView.setTextColor(this.mHintTextColor);
        onFieldChanged(getKind().fieldList.get(0).column, "");
    }

    public void restoreBirthday() {
        saveValue(getKind().typeColumn, Integer.toString(3));
        rebuildValues();
    }

    public boolean isBirthdayType() {
        AccountType.EventEditType type = getType();
        return type.rawValue == 3 && !type.secondary && type.specificMax == 1 && type.customColumn == null && type.isYearOptional();
    }
}
