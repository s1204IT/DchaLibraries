package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.R;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.android.settingslib.wifi.AccessPoint;

public class PreferenceImageView extends ImageView {
    private int mMaxHeight;
    private int mMaxWidth;

    public PreferenceImageView(Context context) {
        this(context, null);
    }

    public PreferenceImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mMaxWidth = Preference.DEFAULT_ORDER;
        this.mMaxHeight = Preference.DEFAULT_ORDER;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferenceImageView, defStyleAttr, 0);
        setMaxWidth(a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxWidth, Preference.DEFAULT_ORDER));
        setMaxHeight(a.getDimensionPixelSize(R.styleable.PreferenceImageView_maxHeight, Preference.DEFAULT_ORDER));
        a.recycle();
    }

    @Override
    public void setMaxWidth(int maxWidth) {
        this.mMaxWidth = maxWidth;
        super.setMaxWidth(maxWidth);
    }

    @Override
    public int getMaxWidth() {
        return this.mMaxWidth;
    }

    @Override
    public void setMaxHeight(int maxHeight) {
        this.mMaxHeight = maxHeight;
        super.setMaxHeight(maxHeight);
    }

    @Override
    public int getMaxHeight() {
        return this.mMaxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode == Integer.MIN_VALUE || widthMode == 0) {
            int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
            int maxWidth = getMaxWidth();
            if (maxWidth != Integer.MAX_VALUE && (maxWidth < widthSize || widthMode == 0)) {
                widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxWidth, AccessPoint.UNREACHABLE_RSSI);
            }
        }
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == Integer.MIN_VALUE || heightMode == 0) {
            int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
            int maxHeight = getMaxHeight();
            if (maxHeight != Integer.MAX_VALUE && (maxHeight < heightSize || heightMode == 0)) {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(maxHeight, AccessPoint.UNREACHABLE_RSSI);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
