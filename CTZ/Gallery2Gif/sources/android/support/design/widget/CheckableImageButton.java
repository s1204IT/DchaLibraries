package android.support.design.widget;

import android.support.v7.widget.AppCompatImageButton;
import android.widget.Checkable;

public class CheckableImageButton extends AppCompatImageButton implements Checkable {
    private static final int[] DRAWABLE_STATE_CHECKED = {android.R.attr.state_checked};
    private boolean checked;

    @Override
    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            refreshDrawableState();
            sendAccessibilityEvent(2048);
        }
    }

    @Override
    public boolean isChecked() {
        return this.checked;
    }

    @Override
    public void toggle() {
        setChecked(!this.checked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (this.checked) {
            return mergeDrawableStates(super.onCreateDrawableState(DRAWABLE_STATE_CHECKED.length + extraSpace), DRAWABLE_STATE_CHECKED);
        }
        return super.onCreateDrawableState(extraSpace);
    }
}
