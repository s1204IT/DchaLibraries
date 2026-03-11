package android.support.v17.leanback.widget;

import android.R;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageView;

class CheckableImageView extends ImageView implements Checkable {
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private boolean mChecked;

    public CheckableImageView(Context context) {
        this(context, null);
    }

    public CheckableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    public void toggle() {
        setChecked(!this.mChecked);
    }

    @Override
    public boolean isChecked() {
        return this.mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.mChecked != checked) {
            this.mChecked = checked;
            refreshDrawableState();
        }
    }
}
