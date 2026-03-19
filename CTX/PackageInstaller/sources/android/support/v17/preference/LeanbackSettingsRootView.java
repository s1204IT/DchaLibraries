package android.support.v17.preference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

public class LeanbackSettingsRootView extends FrameLayout {
    private View.OnKeyListener mOnBackKeyListener;

    public LeanbackSettingsRootView(Context context) {
        super(context);
    }

    public LeanbackSettingsRootView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LeanbackSettingsRootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == 1 && event.getKeyCode() == 4 && this.mOnBackKeyListener != null) {
            handled = this.mOnBackKeyListener.onKey(this, event.getKeyCode(), event);
        }
        return handled || super.dispatchKeyEvent(event);
    }
}
