package androidx.car.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.car.R;

public class ClickThroughToolbar extends Toolbar {
    private boolean mAllowClickPassThrough;

    public ClickThroughToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttributes(context, attrs, 0);
    }

    private void initAttributes(Context context, AttributeSet attrs, int defStyleAttrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClickThroughToolbar, defStyleAttrs, 0);
        this.mAllowClickPassThrough = a.getBoolean(R.styleable.ClickThroughToolbar_clickThrough, false);
        a.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (this.mAllowClickPassThrough) {
            return false;
        }
        return super.onTouchEvent(ev);
    }
}
