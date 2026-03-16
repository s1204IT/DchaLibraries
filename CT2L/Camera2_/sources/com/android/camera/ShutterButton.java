package com.android.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import com.android.camera.debug.Log;
import com.android.camera.ui.TouchCoordinate;
import java.util.ArrayList;
import java.util.List;

public class ShutterButton extends ImageView {
    public static final float ALPHA_WHEN_DISABLED = 0.2f;
    public static final float ALPHA_WHEN_ENABLED = 1.0f;
    private static final Log.Tag TAG = new Log.Tag("ShutterButton");
    private List<OnShutterButtonListener> mListeners;
    private boolean mOldPressed;
    private TouchCoordinate mTouchCoordinate;
    private boolean mTouchEnabled;

    public interface OnShutterButtonListener {
        void onShutterButtonClick();

        void onShutterButtonFocus(boolean z);

        void onShutterCoordinate(TouchCoordinate touchCoordinate);
    }

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTouchEnabled = true;
        this.mListeners = new ArrayList();
    }

    public void addOnShutterButtonListener(OnShutterButtonListener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public void removeOnShutterButtonListener(OnShutterButtonListener listener) {
        if (this.mListeners.contains(listener)) {
            this.mListeners.remove(listener);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent m) {
        if (!this.mTouchEnabled) {
            return false;
        }
        if (m.getActionMasked() == 1) {
            this.mTouchCoordinate = new TouchCoordinate(m.getX(), m.getY(), getMeasuredWidth(), getMeasuredHeight());
        }
        return super.dispatchTouchEvent(m);
    }

    public void enableTouch(boolean enable) {
        this.mTouchEnabled = enable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final boolean pressed = isPressed();
        if (pressed != this.mOldPressed) {
            if (!pressed) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        ShutterButton.this.callShutterButtonFocus(pressed);
                    }
                });
            } else {
                callShutterButtonFocus(pressed);
            }
            this.mOldPressed = pressed;
        }
    }

    private void callShutterButtonFocus(boolean pressed) {
        for (OnShutterButtonListener listener : this.mListeners) {
            listener.onShutterButtonFocus(pressed);
        }
    }

    @Override
    public boolean performClick() {
        boolean result = super.performClick();
        if (getVisibility() == 0) {
            for (OnShutterButtonListener listener : this.mListeners) {
                listener.onShutterCoordinate(this.mTouchCoordinate);
                this.mTouchCoordinate = null;
                listener.onShutterButtonClick();
            }
        }
        return result;
    }
}
