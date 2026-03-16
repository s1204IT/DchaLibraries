package com.android.gallery3d.ingest.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.ingest.data.SimpleDate;
import java.text.DateFormatSymbols;
import java.util.Locale;

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
        Locale currentLocale = Locale.getDefault();
        if (currentLocale.equals(sLocale)) {
            return false;
        }
        sLocale = currentLocale;
        sMonthNames = DateFormatSymbols.getInstance(sLocale).getShortMonths();
        return true;
    }

    public DateTileView(Context context) {
        super(context);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    public DateTileView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    public DateTileView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mMonth = -1;
        this.mYear = -1;
        this.mDate = -1;
        this.mMonthNames = sMonthNames;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDateTextView = (TextView) findViewById(R.id.date_tile_day);
        this.mMonthTextView = (TextView) findViewById(R.id.date_tile_month);
        this.mYearTextView = (TextView) findViewById(R.id.date_tile_year);
    }

    public void setDate(SimpleDate date) {
        setDate(date.getDay(), date.getMonth(), date.getYear());
    }

    public void setDate(int date, int month, int year) {
        if (date != this.mDate) {
            this.mDate = date;
            this.mDateTextView.setText(this.mDate > 9 ? Integer.toString(this.mDate) : "0" + this.mDate);
        }
        if (this.mMonthNames != sMonthNames) {
            this.mMonthNames = sMonthNames;
            if (month == this.mMonth) {
                this.mMonthTextView.setText(this.mMonthNames[this.mMonth]);
            }
        }
        if (month != this.mMonth) {
            this.mMonth = month;
            this.mMonthTextView.setText(this.mMonthNames[this.mMonth]);
        }
        if (year != this.mYear) {
            this.mYear = year;
            this.mYearTextView.setText(Integer.toString(this.mYear));
        }
    }
}
