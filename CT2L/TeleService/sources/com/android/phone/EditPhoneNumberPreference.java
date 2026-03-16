package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DialerKeyListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class EditPhoneNumberPreference extends EditTextPreference {
    private int mButtonClicked;
    private CharSequence mChangeNumberText;
    private boolean mChecked;
    private int mConfirmationMode;
    private Intent mContactListIntent;
    private ImageButton mContactPickButton;
    private View.OnFocusChangeListener mDialogFocusChangeListener;
    private OnDialogClosedListener mDialogOnClosedListener;
    private CharSequence mDisableText;
    private CharSequence mEnableText;
    private String mEncodedText;
    private GetDefaultNumberListener mGetDefaultNumberListener;
    private Activity mParentActivity;
    private String mPhoneNumber;
    private int mPrefId;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;

    interface GetDefaultNumberListener {
        String onGetDefaultNumber(EditPhoneNumberPreference editPhoneNumberPreference);
    }

    interface OnDialogClosedListener {
        void onDialogClosed(EditPhoneNumberPreference editPhoneNumberPreference, int i);
    }

    public EditPhoneNumberPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mEncodedText = null;
        setDialogLayoutResource(R.layout.pref_dialog_editphonenumber);
        this.mContactListIntent = new Intent("android.intent.action.GET_CONTENT");
        this.mContactListIntent.setType("vnd.android.cursor.item/phone_v2");
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.EditPhoneNumberPreference, 0, R.style.EditPhoneNumberPreference);
        this.mEnableText = a.getString(0);
        this.mDisableText = a.getString(1);
        this.mChangeNumberText = a.getString(2);
        this.mConfirmationMode = a.getInt(3, 0);
        a.recycle();
        TypedArray a2 = context.obtainStyledAttributes(attrs, android.R.styleable.CheckBoxPreference, 0, 0);
        this.mSummaryOn = a2.getString(0);
        this.mSummaryOff = a2.getString(1);
        a2.recycle();
    }

    public EditPhoneNumberPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        CharSequence sum;
        int vis;
        super.onBindView(view);
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        if (summaryView != null) {
            if (this.mConfirmationMode == 1) {
                if (this.mChecked) {
                    sum = this.mSummaryOn == null ? getSummary() : this.mSummaryOn;
                } else {
                    sum = this.mSummaryOff == null ? getSummary() : this.mSummaryOff;
                }
            } else {
                sum = getSummary();
            }
            if (sum != null) {
                summaryView.setText(sum);
                vis = 0;
            } else {
                vis = 8;
            }
            if (vis != summaryView.getVisibility()) {
                summaryView.setVisibility(vis);
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        String defaultNumber;
        this.mButtonClicked = -2;
        super.onBindDialogView(view);
        EditText editText = getEditText();
        this.mContactPickButton = (ImageButton) view.findViewById(R.id.select_contact);
        if (editText != null) {
            if (this.mGetDefaultNumberListener != null && (defaultNumber = this.mGetDefaultNumberListener.onGetDefaultNumber(this)) != null) {
                this.mPhoneNumber = defaultNumber;
            }
            editText.setText(this.mPhoneNumber);
            editText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            editText.setKeyListener(DialerKeyListener.getInstance());
            editText.setOnFocusChangeListener(this.mDialogFocusChangeListener);
        }
        if (this.mContactPickButton != null) {
            this.mContactPickButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (EditPhoneNumberPreference.this.mParentActivity != null) {
                        EditPhoneNumberPreference.this.mParentActivity.startActivityForResult(EditPhoneNumberPreference.this.mContactListIntent, EditPhoneNumberPreference.this.mPrefId);
                    }
                }
            });
        }
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        ViewGroup container = (ViewGroup) dialogView.findViewById(R.id.edit_container);
        if (container != null) {
            container.addView(editText, -1, -2);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (this.mConfirmationMode == 1) {
            if (this.mChecked) {
                builder.setPositiveButton(this.mChangeNumberText, this);
                builder.setNeutralButton(this.mDisableText, this);
            } else {
                builder.setPositiveButton((CharSequence) null, (DialogInterface.OnClickListener) null);
                builder.setNeutralButton(this.mEnableText, this);
            }
        }
        builder.setIcon(R.mipmap.ic_launcher_phone);
    }

    public void setDialogOnClosedListener(OnDialogClosedListener l) {
        this.mDialogOnClosedListener = l;
    }

    public void setParentActivity(Activity parent, int identifier) {
        this.mParentActivity = parent;
        this.mPrefId = identifier;
        this.mGetDefaultNumberListener = null;
    }

    public void setParentActivity(Activity parent, int identifier, GetDefaultNumberListener l) {
        this.mParentActivity = parent;
        this.mPrefId = identifier;
        this.mGetDefaultNumberListener = l;
    }

    public void onPickActivityResult(String pickedValue) {
        EditText editText = getEditText();
        if (editText != null) {
            editText.setText(pickedValue);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (this.mConfirmationMode == 1 && which == -3) {
            setToggled(isToggled() ? false : true);
        }
        this.mButtonClicked = which;
        super.onClick(dialog, which);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (this.mButtonClicked == -1 || this.mButtonClicked == -3) {
            setPhoneNumber(getEditText().getText().toString());
            super.onDialogClosed(positiveResult);
            setText(getStringValue());
        } else {
            super.onDialogClosed(positiveResult);
        }
        if (this.mDialogOnClosedListener != null) {
            this.mDialogOnClosedListener.onDialogClosed(this, this.mButtonClicked);
        }
    }

    public boolean isToggled() {
        return this.mChecked;
    }

    public EditPhoneNumberPreference setToggled(boolean checked) {
        this.mChecked = checked;
        setText(getStringValue());
        notifyChanged();
        return this;
    }

    public String getPhoneNumber() {
        return PhoneNumberUtils.stripSeparators(this.mPhoneNumber);
    }

    protected String getRawPhoneNumber() {
        return this.mPhoneNumber;
    }

    public EditPhoneNumberPreference setPhoneNumber(String number) {
        this.mPhoneNumber = number;
        setText(getStringValue());
        notifyChanged();
        return this;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValueFromString(restoreValue ? getPersistedString(getStringValue()) : (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        if (this.mConfirmationMode != 1 || this.mEncodedText == null) {
            return TextUtils.isEmpty(this.mPhoneNumber) && this.mConfirmationMode == 0;
        }
        String[] inValues = this.mEncodedText.split(":", 2);
        boolean shouldDisable = inValues[0].equals("1");
        return shouldDisable;
    }

    @Override
    protected boolean persistString(String value) {
        this.mEncodedText = value;
        return super.persistString(value);
    }

    public EditPhoneNumberPreference setSummaryOn(CharSequence summary) {
        this.mSummaryOn = summary;
        if (isToggled()) {
            notifyChanged();
        }
        return this;
    }

    public CharSequence getSummaryOn() {
        return this.mSummaryOn;
    }

    protected void setValueFromString(String value) {
        String[] inValues = value.split(":", 2);
        setToggled(inValues[0].equals("1"));
        setPhoneNumber(inValues[1]);
    }

    protected String getStringValue() {
        return (isToggled() ? "1" : "0") + ":" + getPhoneNumber();
    }
}
