package android.support.v7.widget;

import android.graphics.Outline;
import android.support.annotation.NonNull;

class ActionBarBackgroundDrawableV21 extends ActionBarBackgroundDrawable {
    public ActionBarBackgroundDrawableV21(ActionBarContainer container) {
        super(container);
    }

    @Override
    public void getOutline(@NonNull Outline outline) {
        if (this.mContainer.mIsSplit) {
            if (this.mContainer.mSplitBackground == null) {
                return;
            }
            this.mContainer.mSplitBackground.getOutline(outline);
        } else {
            if (this.mContainer.mBackground == null) {
                return;
            }
            this.mContainer.mBackground.getOutline(outline);
        }
    }
}
