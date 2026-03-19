package com.android.calendar;

import android.content.Context;
import android.text.Editable;
import android.util.AttributeSet;
import com.android.mtkex.chips.MTKRecipientEditTextView;

public class ChipsAddressTextView extends MTKRecipientEditTextView {
    public ChipsAddressTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setThreshold(1);
    }

    @Override
    public boolean enoughToFilter() {
        int length;
        Editable text = getText();
        if (text != null && (length = text.length()) > 2 && text.charAt(length - 1) == ' ') {
            int i = length - 2;
            if (text.charAt(i) == ',' || text.charAt(i) == ';') {
                return false;
            }
        }
        return super.enoughToFilter();
    }
}
