package com.android.settings.widget;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.android.settingslib.CustomEditTextPreference;

/* loaded from: classes.dex */
public class ValidatedEditTextPreference extends CustomEditTextPreference {
    private boolean mIsPassword;
    private boolean mIsSummaryPassword;
    private final EditTextWatcher mTextWatcher;
    private Validator mValidator;

    public interface Validator {
        boolean isTextValid(String str);
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mTextWatcher = new EditTextWatcher();
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mTextWatcher = new EditTextWatcher();
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTextWatcher = new EditTextWatcher();
    }

    public ValidatedEditTextPreference(Context context) {
        super(context);
        this.mTextWatcher = new EditTextWatcher();
    }

    @Override // com.android.settingslib.CustomEditTextPreference
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = (EditText) view.findViewById(R.id.edit);
        if (editText != null && !TextUtils.isEmpty(editText.getText())) {
            editText.setSelection(editText.getText().length());
        }
        if (this.mValidator != null && editText != null) {
            editText.removeTextChangedListener(this.mTextWatcher);
            if (this.mIsPassword) {
                editText.setInputType(145);
                editText.setMaxLines(1);
            }
            editText.addTextChangedListener(this.mTextWatcher);
        }
    }

    @Override // android.support.v7.preference.Preference
    public void onBindViewHolder(PreferenceViewHolder preferenceViewHolder) {
        super.onBindViewHolder(preferenceViewHolder);
        TextView textView = (TextView) preferenceViewHolder.findViewById(R.id.summary);
        if (textView == null) {
            return;
        }
        if (this.mIsSummaryPassword) {
            textView.setInputType(129);
        } else {
            textView.setInputType(1);
        }
    }

    public void setIsPassword(boolean z) {
        this.mIsPassword = z;
    }

    public void setIsSummaryPassword(boolean z) {
        this.mIsSummaryPassword = z;
    }

    public boolean isPassword() {
        return this.mIsPassword;
    }

    public void setValidator(Validator validator) {
        this.mValidator = validator;
    }

    private class EditTextWatcher implements TextWatcher {
        private EditTextWatcher() {
        }

        @Override // android.text.TextWatcher
        public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override // android.text.TextWatcher
        public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        @Override // android.text.TextWatcher
        public void afterTextChanged(Editable editable) {
            EditText editText = ValidatedEditTextPreference.this.getEditText();
            if (ValidatedEditTextPreference.this.mValidator != null && editText != null) {
                AlertDialog alertDialog = (AlertDialog) ValidatedEditTextPreference.this.getDialog();
                alertDialog.getButton(-1).setEnabled(ValidatedEditTextPreference.this.mValidator.isTextValid(editText.getText().toString()));
            }
        }
    }
}
