package android.telephony;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import com.android.i18n.phonenumbers.AsYouTypeFormatter;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import java.util.Locale;

public class PhoneNumberFormattingTextWatcher implements TextWatcher {
    private AsYouTypeFormatter mFormatter;
    private boolean mSelfChange;
    private boolean mStopFormatting;

    public PhoneNumberFormattingTextWatcher() {
        this(Locale.getDefault().getCountry());
    }

    public PhoneNumberFormattingTextWatcher(String str) {
        this.mSelfChange = false;
        if (str == null) {
            throw new IllegalArgumentException();
        }
        this.mFormatter = PhoneNumberUtil.getInstance().getAsYouTypeFormatter(str);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (!this.mSelfChange && !this.mStopFormatting && i2 > 0 && hasSeparator(charSequence, i, i2)) {
            stopFormatting();
        }
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        if (!this.mSelfChange && !this.mStopFormatting && i3 > 0 && hasSeparator(charSequence, i, i3)) {
            stopFormatting();
        }
    }

    @Override
    public synchronized void afterTextChanged(Editable editable) {
        boolean z = true;
        if (this.mStopFormatting) {
            if (editable.length() == 0) {
                z = false;
            }
            this.mStopFormatting = z;
        } else {
            if (this.mSelfChange) {
                return;
            }
            String strReformat = reformat(editable, Selection.getSelectionEnd(editable));
            if (strReformat != null) {
                int rememberedPosition = this.mFormatter.getRememberedPosition();
                this.mSelfChange = true;
                editable.replace(0, editable.length(), strReformat, 0, strReformat.length());
                if (strReformat.equals(editable.toString())) {
                    Selection.setSelection(editable, rememberedPosition);
                }
                this.mSelfChange = false;
            }
            PhoneNumberUtils.ttsSpanAsPhoneNumber(editable, 0, editable.length());
        }
    }

    private String reformat(CharSequence charSequence, int i) {
        int i2 = i - 1;
        this.mFormatter.clear();
        int length = charSequence.length();
        char c = 0;
        boolean z = false;
        String formattedNumber = null;
        for (int i3 = 0; i3 < length; i3++) {
            char cCharAt = charSequence.charAt(i3);
            if (PhoneNumberUtils.isNonSeparator(cCharAt)) {
                if (c != 0) {
                    formattedNumber = getFormattedNumber(c, z);
                    z = false;
                }
                c = cCharAt;
            }
            if (i3 == i2) {
                z = true;
            }
        }
        if (c != 0) {
            return getFormattedNumber(c, z);
        }
        return formattedNumber;
    }

    private String getFormattedNumber(char c, boolean z) {
        return z ? this.mFormatter.inputDigitAndRememberPosition(c) : this.mFormatter.inputDigit(c);
    }

    private void stopFormatting() {
        this.mStopFormatting = true;
        this.mFormatter.clear();
    }

    private boolean hasSeparator(CharSequence charSequence, int i, int i2) {
        for (int i3 = i; i3 < i + i2; i3++) {
            if (!PhoneNumberUtils.isNonSeparator(charSequence.charAt(i3))) {
                return true;
            }
        }
        return false;
    }
}
