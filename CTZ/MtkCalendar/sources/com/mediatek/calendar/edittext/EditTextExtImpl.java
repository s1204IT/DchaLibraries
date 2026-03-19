package com.mediatek.calendar.edittext;

import android.content.Context;
import android.os.Vibrator;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;
import com.mediatek.calendar.LogUtil;

public class EditTextExtImpl implements IEditTextExt {
    @Override
    public void setLengthInputFilter(EditText editText, Context context, int i) {
        InputFilter[] inputFilterArrCreateInputFilter = createInputFilter(editText, context, i);
        if (inputFilterArrCreateInputFilter != null) {
            editText.setFilters(inputFilterArrCreateInputFilter);
        }
    }

    private InputFilter[] createInputFilter(EditText editText, final Context context, final int i) {
        return new InputFilter[]{new InputFilter.LengthFilter(i) {
            @Override
            public CharSequence filter(CharSequence charSequence, int i2, int i3, Spanned spanned, int i4, int i5) {
                if (charSequence != null && charSequence.length() > 0) {
                    if (((spanned == null ? 0 : spanned.length()) + i4) - i5 == i) {
                        Context context2 = context;
                        Context context3 = context;
                        Vibrator vibrator = (Vibrator) context2.getSystemService("vibrator");
                        boolean zHasVibrator = vibrator.hasVibrator();
                        if (zHasVibrator) {
                            vibrator.vibrate(new long[]{100, 100}, -1);
                        }
                        LogUtil.w("EditTextExtensionImpl", "input out of range,hasVibrator:" + zHasVibrator);
                        return "";
                    }
                }
                if (spanned != null && charSequence != null) {
                    return super.filter(charSequence, i2, i3, spanned, i4, i5);
                }
                return "";
            }
        }};
    }
}
