package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import com.android.internal.R;

@Deprecated
public class TwoLineListItem extends RelativeLayout {
    private TextView mText1;
    private TextView mText2;

    public TwoLineListItem(Context context) {
        this(context, null, 0);
    }

    public TwoLineListItem(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TwoLineListItem(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public TwoLineListItem(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        context.obtainStyledAttributes(attributeSet, R.styleable.TwoLineListItem, i, i2).recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mText1 = (TextView) findViewById(16908308);
        this.mText2 = (TextView) findViewById(16908309);
    }

    public TextView getText1() {
        return this.mText1;
    }

    public TextView getText2() {
        return this.mText2;
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return TwoLineListItem.class.getName();
    }
}
