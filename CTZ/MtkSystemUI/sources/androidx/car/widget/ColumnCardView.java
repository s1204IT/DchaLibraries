package androidx.car.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.car.R;
import androidx.car.utils.ColumnCalculator;

public final class ColumnCardView extends CardView {
    private ColumnCalculator mColumnCalculator;
    private int mColumnSpan;

    public ColumnCardView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ColumnCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ColumnCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttrs) {
        this.mColumnCalculator = ColumnCalculator.getInstance(context);
        int defaultColumnSpan = getResources().getInteger(R.integer.column_card_default_column_span);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ColumnCardView, defStyleAttrs, 0);
        this.mColumnSpan = ta.getInteger(R.styleable.ColumnCardView_columnSpan, defaultColumnSpan);
        ta.recycle();
        if (Log.isLoggable("ColumnCardView", 3)) {
            Log.d("ColumnCardView", "Column span: " + this.mColumnSpan);
        }
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = this.mColumnCalculator.getSizeForColumnSpan(this.mColumnSpan);
        super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, 1073741824), heightMeasureSpec);
    }
}
