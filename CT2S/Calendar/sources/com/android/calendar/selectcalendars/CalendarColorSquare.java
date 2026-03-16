package com.android.calendar.selectcalendars;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.QuickContactBadge;
import com.android.calendar.R;
import com.android.colorpicker.ColorStateDrawable;

public class CalendarColorSquare extends QuickContactBadge {
    public CalendarColorSquare(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CalendarColorSquare(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setBackgroundColor(int color) {
        Drawable[] colorDrawable = {getContext().getResources().getDrawable(R.drawable.calendar_color_square)};
        setImageDrawable(new ColorStateDrawable(colorDrawable, color));
    }
}
