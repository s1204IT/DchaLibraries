package com.mediatek.telephony;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

public class PhoneNumberFormattingTextWatcherEx implements TextWatcher {
    private static int sFormatType;
    private boolean mDeletingBackward;
    private boolean mDeletingHyphen;
    private boolean mFormatting;
    private int mHyphenStart;
    private String sCachedSimIso;

    public PhoneNumberFormattingTextWatcherEx() {
        if (this.sCachedSimIso != null) {
            return;
        }
        this.sCachedSimIso = PhoneNumberFormatUtilEx.getDefaultSimCountryIso();
        sFormatType = PhoneNumberFormatUtilEx.getFormatTypeFromCountryCode(this.sCachedSimIso);
    }

    @Override
    public synchronized void afterTextChanged(Editable text) {
        if (!this.mFormatting) {
            this.mFormatting = true;
            if (this.mDeletingHyphen && this.mHyphenStart > 0) {
                if (this.mDeletingBackward) {
                    if (this.mHyphenStart - 1 < text.length()) {
                        text.delete(this.mHyphenStart - 1, this.mHyphenStart);
                    }
                } else if (this.mHyphenStart < text.length()) {
                    text.delete(this.mHyphenStart, this.mHyphenStart + 1);
                }
            }
            PhoneNumberFormatUtilEx.formatNumber(text, sFormatType);
            this.mFormatting = false;
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if (this.mFormatting) {
            return;
        }
        int selStart = Selection.getSelectionStart(s);
        int selEnd = Selection.getSelectionEnd(s);
        if (s.length() > 1 && count == 1 && after == 0 && s.charAt(start) == '-' && selStart == selEnd) {
            this.mDeletingHyphen = true;
            this.mHyphenStart = start;
            if (selStart == start + 1) {
                this.mDeletingBackward = true;
                return;
            } else {
                this.mDeletingBackward = false;
                return;
            }
        }
        this.mDeletingHyphen = false;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
