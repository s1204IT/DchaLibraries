package com.android.settings.display;

import android.content.Context;
import android.text.BidiFormatter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.View;
import android.widget.EditText;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.CustomEditTextPreference;
import com.android.settingslib.display.DisplayDensityUtils;
import java.text.NumberFormat;

public class DensityPreference extends CustomEditTextPreference {
    public DensityPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        setSummary(getContext().getString(R.string.density_pixel_summary, BidiFormatter.getInstance().unicodeWrap(NumberFormat.getInstance().format(getCurrentSwDp()))));
    }

    private int getCurrentSwDp() {
        return (int) (Math.min(r0.widthPixels, r0.heightPixels) / getContext().getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        EditText editText = (EditText) view.findViewById(android.R.id.edit);
        if (editText != null) {
            editText.setInputType(2);
            editText.setText(getCurrentSwDp() + "");
            Utils.setEditTextCursorPosition(editText);
        }
    }

    @Override
    protected void onDialogClosed(boolean z) {
        if (z) {
            try {
                DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
                DisplayDensityUtils.setForcedDisplayDensity(0, Math.max((160 * Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels)) / Math.max(Integer.parseInt(getText()), 320), 120));
            } catch (Exception e) {
                Slog.e("DensityPreference", "Couldn't save density", e);
            }
        }
    }
}
