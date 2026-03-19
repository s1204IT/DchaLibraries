package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
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
    private static final int CM_ACTIVATION = 1;
    private static final int CM_CONFIRM = 0;
    private static final String VALUE_OFF = "0";
    private static final String VALUE_ON = "1";
    private static final String VALUE_SEPARATOR = ":";
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
    protected Activity mParentActivity;
    private String mPhoneNumber;
    private int mPrefId;
    private CharSequence mSummaryOff;
    private CharSequence mSummaryOn;

    public interface GetDefaultNumberListener {
        String onGetDefaultNumber(EditPhoneNumberPreference editPhoneNumberPreference);
    }

    public interface OnDialogClosedListener {
        void onDialogClosed(EditPhoneNumberPreference editPhoneNumberPreference, int i);
    }

    public EditPhoneNumberPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mEncodedText = null;
        setDialogLayoutResource(R.layout.pref_dialog_editphonenumber);
        this.mContactListIntent = new Intent("android.intent.action.PICK");
        this.mContactListIntent.setType("vnd.android.cursor.dir/phone_v2");
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.EditPhoneNumberPreference, 0, R.style.EditPhoneNumberPreference);
        this.mEnableText = typedArrayObtainStyledAttributes.getString(3);
        this.mDisableText = typedArrayObtainStyledAttributes.getString(2);
        this.mChangeNumberText = typedArrayObtainStyledAttributes.getString(0);
        this.mConfirmationMode = typedArrayObtainStyledAttributes.getInt(1, 0);
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, android.R.styleable.CheckBoxPreference, 0, 0);
        this.mSummaryOn = typedArrayObtainStyledAttributes2.getString(0);
        this.mSummaryOff = typedArrayObtainStyledAttributes2.getString(1);
        typedArrayObtainStyledAttributes2.recycle();
    }

    public EditPhoneNumberPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onBindView(View view) {
        CharSequence summary;
        int i;
        super.onBindView(view);
        TextView textView = (TextView) view.findViewById(android.R.id.summary);
        if (textView != null) {
            if (this.mConfirmationMode == 1) {
                if (this.mChecked) {
                    summary = this.mSummaryOn == null ? getSummary() : this.mSummaryOn;
                } else {
                    summary = this.mSummaryOff == null ? getSummary() : this.mSummaryOff;
                }
            } else {
                summary = getSummary();
            }
            if (summary != null) {
                textView.setText(summary);
                i = 0;
            } else {
                i = 8;
            }
            if (i != textView.getVisibility()) {
                textView.setVisibility(i);
            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        String strOnGetDefaultNumber;
        this.mButtonClicked = -2;
        super.onBindDialogView(view);
        EditText editText = getEditText();
        this.mContactPickButton = (ImageButton) view.findViewById(R.id.select_contact);
        if (editText != null) {
            if (this.mGetDefaultNumberListener != null && (strOnGetDefaultNumber = this.mGetDefaultNumberListener.onGetDefaultNumber(this)) != null) {
                this.mPhoneNumber = strOnGetDefaultNumber;
            }
            editText.setText(BidiFormatter.getInstance().unicodeWrap(this.mPhoneNumber, TextDirectionHeuristics.LTR));
            editText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
            editText.setKeyListener(DialerKeyListener.getInstance());
            editText.setOnFocusChangeListener(this.mDialogFocusChangeListener);
        }
        if (this.mContactPickButton != null) {
            this.mContactPickButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    if (EditPhoneNumberPreference.this.mParentActivity != null) {
                        EditPhoneNumberPreference.this.mParentActivity.startActivityForResult(EditPhoneNumberPreference.this.mContactListIntent, EditPhoneNumberPreference.this.mPrefId);
                    }
                }
            });
        }
    }

    @Override
    protected void onAddEditTextToDialogView(View view, EditText editText) {
        ViewGroup viewGroup = (ViewGroup) view.findViewById(R.id.edit_container);
        if (viewGroup != null) {
            viewGroup.addView(editText, -1, -2);
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        if (this.mConfirmationMode == 1) {
            if (this.mChecked) {
                builder.setPositiveButton(this.mChangeNumberText, this);
                builder.setNeutralButton(this.mDisableText, this);
            } else {
                builder.setPositiveButton(this.mEnableText, this);
                builder.setNeutralButton((CharSequence) null, (DialogInterface.OnClickListener) null);
            }
        }
        builder.setIcon(R.mipmap.ic_launcher_phone);
    }

    public void setDialogOnFocusChangeListener(View.OnFocusChangeListener onFocusChangeListener) {
        this.mDialogFocusChangeListener = onFocusChangeListener;
    }

    public void setDialogOnClosedListener(OnDialogClosedListener onDialogClosedListener) {
        this.mDialogOnClosedListener = onDialogClosedListener;
    }

    public void setParentActivity(Activity activity, int i) {
        this.mParentActivity = activity;
        this.mPrefId = i;
        this.mGetDefaultNumberListener = null;
    }

    public void setParentActivity(Activity activity, int i, GetDefaultNumberListener getDefaultNumberListener) {
        this.mParentActivity = activity;
        this.mPrefId = i;
        this.mGetDefaultNumberListener = getDefaultNumberListener;
    }

    public void onPickActivityResult(String str) {
        EditText editText = getEditText();
        if (editText != null) {
            editText.setText(str);
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (this.mConfirmationMode == 1 && i == -3) {
            setToggled(!isToggled());
        }
        this.mButtonClicked = i;
        super.onClick(dialogInterface, i);
    }

    @Override
    protected void onDialogClosed(boolean z) {
        if (this.mButtonClicked == -1 || this.mButtonClicked == -3) {
            setPhoneNumber(getEditText().getText().toString());
            super.onDialogClosed(z);
            setText(getStringValue());
        } else {
            super.onDialogClosed(z);
        }
        if (this.mDialogOnClosedListener != null) {
            this.mDialogOnClosedListener.onDialogClosed(this, this.mButtonClicked);
        }
    }

    public boolean isToggled() {
        return this.mChecked;
    }

    public EditPhoneNumberPreference setToggled(boolean z) {
        this.mChecked = z;
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

    public EditPhoneNumberPreference setPhoneNumber(String str) {
        this.mPhoneNumber = str;
        setText(getStringValue());
        notifyChanged();
        return this;
    }

    @Override
    protected void onSetInitialValue(boolean z, Object obj) {
        setValueFromString(z ? getPersistedString(getStringValue()) : (String) obj);
    }

    @Override
    public boolean shouldDisableDependents() {
        if (this.mConfirmationMode != 1 || this.mEncodedText == null) {
            return TextUtils.isEmpty(this.mPhoneNumber) && this.mConfirmationMode == 0;
        }
        return this.mEncodedText.split(VALUE_SEPARATOR, 2)[0].equals("1");
    }

    @Override
    protected boolean persistString(String str) {
        this.mEncodedText = str;
        return super.persistString(str);
    }

    public EditPhoneNumberPreference setSummaryOn(CharSequence charSequence) {
        this.mSummaryOn = charSequence;
        if (isToggled()) {
            notifyChanged();
        }
        return this;
    }

    public EditPhoneNumberPreference setSummaryOn(int i) {
        return setSummaryOn(getContext().getString(i));
    }

    public CharSequence getSummaryOn() {
        return this.mSummaryOn;
    }

    public EditPhoneNumberPreference setSummaryOff(CharSequence charSequence) {
        this.mSummaryOff = charSequence;
        if (!isToggled()) {
            notifyChanged();
        }
        return this;
    }

    public EditPhoneNumberPreference setSummaryOff(int i) {
        return setSummaryOff(getContext().getString(i));
    }

    public CharSequence getSummaryOff() {
        return this.mSummaryOff;
    }

    protected void setValueFromString(String str) {
        String[] strArrSplit = str.split(VALUE_SEPARATOR, 2);
        setToggled(strArrSplit[0].equals("1"));
        setPhoneNumber(strArrSplit[1]);
    }

    protected String getStringValue() {
        StringBuilder sb = new StringBuilder();
        sb.append(isToggled() ? "1" : "0");
        sb.append(VALUE_SEPARATOR);
        sb.append(getPhoneNumber());
        return sb.toString();
    }

    public void showPhoneNumberDialog() {
        showDialog(null);
    }
}
