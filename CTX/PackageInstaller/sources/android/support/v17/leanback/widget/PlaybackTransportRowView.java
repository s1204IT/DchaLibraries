package android.support.v17.leanback.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class PlaybackTransportRowView extends LinearLayout {
    private OnUnhandledKeyListener mOnUnhandledKeyListener;

    public interface OnUnhandledKeyListener {
        boolean onUnhandledKey(KeyEvent keyEvent);
    }

    public PlaybackTransportRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlaybackTransportRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (super.dispatchKeyEvent(event)) {
            return true;
        }
        return this.mOnUnhandledKeyListener != null && this.mOnUnhandledKeyListener.onUnhandledKey(event);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        View focused = findFocus();
        if (focused != null && focused.requestFocus(direction, previouslyFocusedRect)) {
            return true;
        }
        View progress = findViewById(R.id.playback_progress);
        if (progress != null && progress.isFocusable() && progress.requestFocus(direction, previouslyFocusedRect)) {
            return true;
        }
        return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
    }

    @Override
    public View focusSearch(View focused, int direction) {
        View view;
        if (focused != null) {
            if (direction == 33) {
                int index = indexOfChild(getFocusedChild());
                for (int index2 = index - 1; index2 >= 0; index2--) {
                    View view2 = getChildAt(index2);
                    if (view2.hasFocusable()) {
                        return view2;
                    }
                }
            } else {
                if (direction == 130) {
                    int index3 = indexOfChild(getFocusedChild());
                    do {
                        index3++;
                        if (index3 < getChildCount()) {
                            view = getChildAt(index3);
                        }
                    } while (!view.hasFocusable());
                    return view;
                }
                if ((direction == 17 || direction == 66) && (getFocusedChild() instanceof ViewGroup)) {
                    return FocusFinder.getInstance().findNextFocus((ViewGroup) getFocusedChild(), focused, direction);
                }
            }
        }
        return super.focusSearch(focused, direction);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
