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
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.dataitem.DataKind;
import com.android.contacts.common.util.DateUtils;
import com.android.contacts.datepicker.DatePicker;
import com.android.contacts.datepicker.DatePickerDialog;
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

    public EventFieldEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EventFieldEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources resources = this.mContext.getResources();
        this.mPrimaryTextColor = resources.getColor(R.color.primary_text_color);
        this.mHintTextColor = resources.getColor(R.color.editor_disabled_text_color);
        this.mNoDateString = this.mContext.getString(R.string.event_edit_field_hint_text);
        this.mDateView = (Button) findViewById(R.id.date_view);
        this.mDateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventFieldEditorView.this.showDialog(R.id.dialog_event_date_picker);
            }
        });
    }

    @Override
    protected void requestFocusForFirstEditField() {
        this.mDateView.requestFocus();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mDateView.setEnabled(!isReadOnly() && enabled);
    }

    @Override
    public void setValues(DataKind kind, ValuesDelta entry, RawContactDelta state, boolean readOnly, ViewIdGenerator vig) {
        if (kind.fieldList.size() != 1) {
            throw new IllegalStateException("kind must have 1 field");
        }
        super.setValues(kind, entry, state, readOnly, vig);
        this.mDateView.setEnabled(isEnabled() && !readOnly);
        rebuildDateView();
        updateEmptiness();
    }

    private void rebuildDateView() {
        AccountType.EditField editField = getKind().fieldList.get(0);
        String column = editField.column;
        String data = DateUtils.formatDate(getContext(), getEntry().getAsString(column), false);
        if (TextUtils.isEmpty(data)) {
            this.mDateView.setText(this.mNoDateString);
            this.mDateView.setTextColor(this.mHintTextColor);
            setDeleteButtonVisible(false);
        } else {
            this.mDateView.setText(data);
            this.mDateView.setTextColor(this.mPrimaryTextColor);
            setDeleteButtonVisible(true);
        }
    }

    @Override
    public boolean isEmpty() {
        AccountType.EditField editField = getKind().fieldList.get(0);
        String column = editField.column;
        return TextUtils.isEmpty(getEntry().getAsString(column));
    }

    @Override
    public Dialog createDialog(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("bundle must not be null");
        }
        int dialogId = bundle.getInt("dialog_id");
        switch (dialogId) {
            case R.id.dialog_event_date_picker:
                return createDatePickerDialog();
            default:
                return super.createDialog(bundle);
        }
    }

    @Override
    protected AccountType.EventEditType getType() {
        return (AccountType.EventEditType) super.getType();
    }

    @Override
    protected void onLabelRebuilt() {
        String column = getKind().fieldList.get(0).column;
        String oldValue = getEntry().getAsString(column);
        DataKind kind = getKind();
        Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
        int defaultYear = calendar.get(1);
        boolean isYearOptional = getType().isYearOptional();
        if (!isYearOptional && !TextUtils.isEmpty(oldValue)) {
            ParsePosition position = new ParsePosition(0);
            Date date2 = kind.dateFormatWithoutYear.parse(oldValue, position);
            if (date2 != null) {
                calendar.setTime(date2);
                calendar.set(defaultYear, calendar.get(2), calendar.get(5), 8, 0, 0);
                onFieldChanged(column, kind.dateFormatWithYear.format(calendar.getTime()));
                rebuildDateView();
            }
        }
    }

    private Dialog createDatePickerDialog() {
        int oldYear;
        int oldMonth;
        int oldDay;
        final String column = getKind().fieldList.get(0).column;
        String oldValue = getEntry().getAsString(column);
        final DataKind kind = getKind();
        Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
        int defaultYear = calendar.get(1);
        final boolean isYearOptional = getType().isYearOptional();
        if (TextUtils.isEmpty(oldValue)) {
            oldYear = defaultYear;
            oldMonth = calendar.get(2);
            oldDay = calendar.get(5);
        } else {
            Calendar cal = DateUtils.parseDate(oldValue, false);
            if (cal != null) {
                if (DateUtils.isYearSet(cal)) {
                    oldYear = cal.get(1);
                } else {
                    oldYear = isYearOptional ? DatePickerDialog.NO_YEAR : defaultYear;
                }
                oldMonth = cal.get(2);
                oldDay = cal.get(5);
            } else {
                return null;
            }
        }
        DatePickerDialog.OnDateSetListener callBack = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                String resultString;
                if (year == 0 && !isYearOptional) {
                    throw new IllegalStateException();
                }
                Calendar outCalendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE, Locale.US);
                outCalendar.clear();
                outCalendar.set(year == DatePickerDialog.NO_YEAR ? 2000 : year, monthOfYear, dayOfMonth, 8, 0, 0);
                if (year == 0) {
                    resultString = kind.dateFormatWithoutYear.format(outCalendar.getTime());
                } else {
                    resultString = kind.dateFormatWithYear.format(outCalendar.getTime());
                }
                EventFieldEditorView.this.onFieldChanged(column, resultString);
                EventFieldEditorView.this.rebuildDateView();
            }
        };
        return new DatePickerDialog(getContext(), callBack, oldYear, oldMonth, oldDay, isYearOptional);
    }

    @Override
    public void clearAllFields() {
        this.mDateView.setText(this.mNoDateString);
        this.mDateView.setTextColor(this.mHintTextColor);
        String column = getKind().fieldList.get(0).column;
        onFieldChanged(column, "");
    }
}
