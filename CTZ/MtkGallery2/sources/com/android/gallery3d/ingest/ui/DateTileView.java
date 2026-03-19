package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.SimpleDate;
import java.text.DateFormatSymbols;
import java.util.Locale;
import mf.org.apache.xerces.impl.xs.SchemaSymbols;

public class DateTileView extends FrameLayout {
    private static Locale sLocale;
    private static String[] sMonthNames = DateFormatSymbols.getInstance().getShortMonths();
    private int mDate;
    private TextView mDateTextView;
    private int mMonth;
    private String[] mMonthNames;
    private TextView mMonthTextView;
    private int mYear;
    private TextView mYearTextView;

    static {
        refreshLocale();
    }

    public static boolean refreshLocale() {
        Locale locale = Locale.getDefault();
        if (!locale.equals(sLocale)) {
            sLocale = locale;
            sMonthNames = DateFormatSymbols.getInstance(sLocale).getShortMonths();
            return true;
        }
        return false;
    }

    public DateTileView(Context context) {
        super(context);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    public DateTileView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    public DateTileView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    @Override
    public void onMeasure(int i, int i2) {
        super.onMeasure(i, i);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDateTextView = (TextView) findViewById(R.id.date_tile_day);
        this.mMonthTextView = (TextView) findViewById(R.id.date_tile_month);
        this.mYearTextView = (TextView) findViewById(R.id.date_tile_year);
    }

    public void setDate(SimpleDate simpleDate) {
        setDate(simpleDate.getDay(), simpleDate.getMonth(), simpleDate.getYear());
    }

    public void setDate(int i, int i2, int i3) {
        String string;
        if (i != this.mDate) {
            this.mDate = i;
            TextView textView = this.mDateTextView;
            if (this.mDate > 9) {
                string = Integer.toString(this.mDate);
            } else {
                string = SchemaSymbols.ATTVAL_FALSE_0 + this.mDate;
            }
            textView.setText(string);
        }
        if (this.mMonthNames != sMonthNames) {
            this.mMonthNames = sMonthNames;
            if (i2 == this.mMonth) {
                this.mMonthTextView.setText(this.mMonthNames[this.mMonth]);
            }
        }
        if (i2 != this.mMonth) {
            this.mMonth = i2;
            this.mMonthTextView.setText(this.mMonthNames[this.mMonth]);
        }
        if (i3 != this.mYear) {
            this.mYear = i3;
            this.mYearTextView.setText(Integer.toString(this.mYear));
        }
    }
}
