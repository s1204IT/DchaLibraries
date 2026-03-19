package android.support.design.widget;

import android.widget.ImageButton;

class VisibilityAwareImageButton extends ImageButton {
    private int userSetVisibility;

    @Override
    public void setVisibility(int visibility) {
        internalSetVisibility(visibility, true);
    }

    final void internalSetVisibility(int visibility, boolean fromUser) {
        super.setVisibility(visibility);
        if (fromUser) {
            this.userSetVisibility = visibility;
        }
    }

    final int getUserSetVisibility() {
        return this.userSetVisibility;
    }
}
