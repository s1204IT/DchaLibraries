package androidx.car.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import androidx.car.R;

public class ColumnCalculator {
    private static ColumnCalculator sInstance;
    private static int sScreenWidth;
    private int mColumnWidth;
    private int mGutterSize;
    private int mNumOfColumns;
    private int mNumOfGutters;

    public static ColumnCalculator getInstance(Context context) {
        if (sInstance == null) {
            WindowManager windowManager = (WindowManager) context.getSystemService("window");
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            sScreenWidth = displayMetrics.widthPixels;
            sInstance = new ColumnCalculator(context);
        }
        return sInstance;
    }

    private ColumnCalculator(Context context) {
        Resources res = context.getResources();
        int marginSize = res.getDimensionPixelSize(R.dimen.car_margin);
        this.mGutterSize = res.getDimensionPixelSize(R.dimen.car_gutter_size);
        this.mNumOfColumns = res.getInteger(R.integer.car_column_number);
        if (Log.isLoggable("ColumnCalculator", 3)) {
            Log.d("ColumnCalculator", String.format("marginSize: %d; numOfColumns: %d; gutterSize: %d", Integer.valueOf(marginSize), Integer.valueOf(this.mNumOfColumns), Integer.valueOf(this.mGutterSize)));
        }
        this.mNumOfGutters = this.mNumOfColumns - 1;
        int spaceForColumns = (sScreenWidth - (2 * marginSize)) - (this.mNumOfGutters * this.mGutterSize);
        this.mColumnWidth = spaceForColumns / this.mNumOfColumns;
        if (Log.isLoggable("ColumnCalculator", 3)) {
            Log.d("ColumnCalculator", "mColumnWidth: " + this.mColumnWidth);
        }
    }

    public int getSizeForColumnSpan(int columnSpan) {
        int gutterSpan = columnSpan - 1;
        return (this.mColumnWidth * columnSpan) + (this.mGutterSize * gutterSpan);
    }
}
