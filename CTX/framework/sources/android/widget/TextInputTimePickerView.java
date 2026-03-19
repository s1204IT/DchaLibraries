package android.widget;

import android.content.Context;
import android.os.LocaleList;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.MathUtils;
import android.view.View;
import android.widget.AdapterView;
import com.android.internal.R;

public class TextInputTimePickerView extends RelativeLayout {
    private static final int AM = 0;
    public static final int AMPM = 2;
    public static final int HOURS = 0;
    public static final int MINUTES = 1;
    private static final int PM = 1;
    private final Spinner mAmPmSpinner;
    private final TextView mErrorLabel;
    private boolean mErrorShowing;
    private final EditText mHourEditText;
    private boolean mHourFormatStartsAtZero;
    private final TextView mHourLabel;
    private final TextView mInputSeparatorView;
    private boolean mIs24Hour;
    private OnValueTypedListener mListener;
    private final EditText mMinuteEditText;
    private final TextView mMinuteLabel;

    interface OnValueTypedListener {
        void onValueChanged(int i, int i2);
    }

    public TextInputTimePickerView(Context context) {
        this(context, null);
    }

    public TextInputTimePickerView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TextInputTimePickerView(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TextInputTimePickerView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        inflate(context, R.layout.time_picker_text_input_material, this);
        this.mHourEditText = (EditText) findViewById(R.id.input_hour);
        this.mMinuteEditText = (EditText) findViewById(R.id.input_minute);
        this.mInputSeparatorView = (TextView) findViewById(R.id.input_separator);
        this.mErrorLabel = (TextView) findViewById(R.id.label_error);
        this.mHourLabel = (TextView) findViewById(R.id.label_hour);
        this.mMinuteLabel = (TextView) findViewById(R.id.label_minute);
        this.mHourEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                TextInputTimePickerView.this.parseAndSetHourInternal(editable.toString());
            }
        });
        this.mMinuteEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i3, int i4, int i5) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                TextInputTimePickerView.this.parseAndSetMinuteInternal(editable.toString());
            }
        });
        this.mAmPmSpinner = (Spinner) findViewById(R.id.am_pm_spinner);
        String[] amPmStrings = TimePicker.getAmPmStrings(context);
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, 17367049);
        arrayAdapter.add(TimePickerClockDelegate.obtainVerbatim(amPmStrings[0]));
        arrayAdapter.add(TimePickerClockDelegate.obtainVerbatim(amPmStrings[1]));
        this.mAmPmSpinner.setAdapter((SpinnerAdapter) arrayAdapter);
        this.mAmPmSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i3, long j) {
                if (i3 == 0) {
                    TextInputTimePickerView.this.mListener.onValueChanged(2, 0);
                } else {
                    TextInputTimePickerView.this.mListener.onValueChanged(2, 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    void setListener(OnValueTypedListener onValueTypedListener) {
        this.mListener = onValueTypedListener;
    }

    void setHourFormat(int i) {
        this.mHourEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});
        this.mMinuteEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(i)});
        LocaleList locales = this.mContext.getResources().getConfiguration().getLocales();
        this.mHourEditText.setImeHintLocales(locales);
        this.mMinuteEditText.setImeHintLocales(locales);
    }

    boolean validateInput() {
        boolean z;
        if (!parseAndSetHourInternal(this.mHourEditText.getText().toString()) || !parseAndSetMinuteInternal(this.mMinuteEditText.getText().toString())) {
            z = false;
        } else {
            z = true;
        }
        setError(!z);
        return z;
    }

    void updateSeparator(String str) {
        this.mInputSeparatorView.setText(str);
    }

    private void setError(boolean z) {
        this.mErrorShowing = z;
        this.mErrorLabel.setVisibility(z ? 0 : 4);
        this.mHourLabel.setVisibility(z ? 4 : 0);
        this.mMinuteLabel.setVisibility(z ? 4 : 0);
    }

    void updateTextInputValues(int i, int i2, int i3, boolean z, boolean z2) {
        this.mIs24Hour = z;
        this.mHourFormatStartsAtZero = z2;
        this.mAmPmSpinner.setVisibility(z ? 4 : 0);
        if (i3 != 0) {
            this.mAmPmSpinner.setSelection(1);
        } else {
            this.mAmPmSpinner.setSelection(0);
        }
        this.mHourEditText.setText(String.format("%d", Integer.valueOf(i)));
        this.mMinuteEditText.setText(String.format("%02d", Integer.valueOf(i2)));
        if (this.mErrorShowing) {
            validateInput();
        }
    }

    private boolean parseAndSetHourInternal(String str) {
        try {
            int i = Integer.parseInt(str);
            if (isValidLocalizedHour(i)) {
                this.mListener.onValueChanged(0, getHourOfDayFromLocalizedHour(i));
                return true;
            }
            int i2 = !this.mHourFormatStartsAtZero ? 1 : 0;
            this.mListener.onValueChanged(0, getHourOfDayFromLocalizedHour(MathUtils.constrain(i, i2, this.mIs24Hour ? 23 : 11 + i2)));
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean parseAndSetMinuteInternal(String str) {
        try {
            int i = Integer.parseInt(str);
            if (i >= 0 && i <= 59) {
                this.mListener.onValueChanged(1, i);
                return true;
            }
            this.mListener.onValueChanged(1, MathUtils.constrain(i, 0, 59));
            return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidLocalizedHour(int i) {
        int i2 = !this.mHourFormatStartsAtZero ? 1 : 0;
        return i >= i2 && i <= (this.mIs24Hour ? 23 : 11) + i2;
    }

    private int getHourOfDayFromLocalizedHour(int i) {
        if (this.mIs24Hour) {
            if (this.mHourFormatStartsAtZero || i != 24) {
                return i;
            }
            return 0;
        }
        if (!this.mHourFormatStartsAtZero && i == 12) {
            i = 0;
        }
        if (this.mAmPmSpinner.getSelectedItemPosition() == 1) {
            return i + 12;
        }
        return i;
    }
}
