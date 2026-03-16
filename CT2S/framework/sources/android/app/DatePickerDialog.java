package android.app;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import com.android.internal.R;
import java.util.Calendar;

public class DatePickerDialog extends AlertDialog implements DialogInterface.OnClickListener, DatePicker.OnDateChangedListener {
    private static final String DAY = "day";
    private static final String MONTH = "month";
    private static final String YEAR = "year";
    private final Calendar mCalendar;
    private final DatePicker mDatePicker;
    private final OnDateSetListener mDateSetListener;
    private boolean mTitleNeedsUpdate;
    private final DatePicker.ValidationCallback mValidationCallback;

    public interface OnDateSetListener {
        void onDateSet(DatePicker datePicker, int i, int i2, int i3);
    }

    public DatePickerDialog(Context context, OnDateSetListener callBack, int year, int monthOfYear, int dayOfMonth) {
        this(context, 0, callBack, year, monthOfYear, dayOfMonth);
    }

    static int resolveDialogTheme(Context context, int resid) {
        if (resid == 0) {
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(16843948, outValue, true);
            return outValue.resourceId;
        }
        return resid;
    }

    public DatePickerDialog(Context context, int theme, OnDateSetListener listener, int year, int monthOfYear, int dayOfMonth) {
        super(context, resolveDialogTheme(context, theme));
        this.mTitleNeedsUpdate = true;
        this.mValidationCallback = new DatePicker.ValidationCallback() {
            @Override
            public void onValidationChanged(boolean valid) {
                Button positive = DatePickerDialog.this.getButton(-1);
                if (positive != null) {
                    positive.setEnabled(valid);
                }
            }
        };
        this.mDateSetListener = listener;
        this.mCalendar = Calendar.getInstance();
        Context themeContext = getContext();
        LayoutInflater inflater = LayoutInflater.from(themeContext);
        View view = inflater.inflate(R.layout.date_picker_dialog, (ViewGroup) null);
        setView(view);
        setButton(-1, themeContext.getString(17039370), this);
        setButton(-2, themeContext.getString(17039360), this);
        setButtonPanelLayoutHint(1);
        this.mDatePicker = (DatePicker) view.findViewById(R.id.datePicker);
        this.mDatePicker.init(year, monthOfYear, dayOfMonth, this);
        this.mDatePicker.setValidationCallback(this.mValidationCallback);
    }

    @Override
    public void onDateChanged(DatePicker view, int year, int month, int day) {
        this.mDatePicker.init(year, month, day, this);
        updateTitle(year, month, day);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                cancel();
                break;
            case -1:
                if (this.mDateSetListener != null) {
                    this.mDateSetListener.onDateSet(this.mDatePicker, this.mDatePicker.getYear(), this.mDatePicker.getMonth(), this.mDatePicker.getDayOfMonth());
                }
                break;
        }
    }

    public DatePicker getDatePicker() {
        return this.mDatePicker;
    }

    public void updateDate(int year, int monthOfYear, int dayOfMonth) {
        this.mDatePicker.updateDate(year, monthOfYear, dayOfMonth);
    }

    private void updateTitle(int year, int month, int day) {
        if (!this.mDatePicker.getCalendarViewShown()) {
            this.mCalendar.set(1, year);
            this.mCalendar.set(2, month);
            this.mCalendar.set(5, day);
            String title = DateUtils.formatDateTime(this.mContext, this.mCalendar.getTimeInMillis(), 98326);
            setTitle(title);
            this.mTitleNeedsUpdate = true;
            return;
        }
        if (this.mTitleNeedsUpdate) {
            this.mTitleNeedsUpdate = false;
            setTitle(R.string.date_picker_dialog_title);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putInt("year", this.mDatePicker.getYear());
        state.putInt(MONTH, this.mDatePicker.getMonth());
        state.putInt(DAY, this.mDatePicker.getDayOfMonth());
        return state;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        int year = savedInstanceState.getInt("year");
        int month = savedInstanceState.getInt(MONTH);
        int day = savedInstanceState.getInt(DAY);
        this.mDatePicker.init(year, month, day, this);
    }
}
