package com.android.calendar.selectcalendars;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import com.android.calendar.R;
import com.android.colorpicker.ColorStateDrawable;

public class CalendarColorSquare extends QuickContactBadge {
    public CalendarColorSquare(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CalendarColorSquare(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override
    public void setBackgroundColor(int i) {
        setImageDrawable(new ColorStateDrawable(new Drawable[]{getContext().getResources().getDrawable(R.drawable.calendar_color_square)}, i));
    }
}
