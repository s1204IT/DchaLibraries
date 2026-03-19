package android.inputmethodservice;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

public class SoftInputWindow extends Dialog {
    private final Rect mBounds;
    final Callback mCallback;
    final KeyEvent.DispatcherState mDispatcherState;
    final int mGravity;
    final KeyEvent.Callback mKeyEventCallback;
    final String mName;
    final boolean mTakesFocus;
    final int mWindowType;

    public interface Callback {
        void onBackPressed();
    }

    public void setToken(IBinder iBinder) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.token = iBinder;
        getWindow().setAttributes(attributes);
    }

    public SoftInputWindow(Context context, String str, int i, Callback callback, KeyEvent.Callback callback2, KeyEvent.DispatcherState dispatcherState, int i2, int i3, boolean z) {
        super(context, i);
        this.mBounds = new Rect();
        this.mName = str;
        this.mCallback = callback;
        this.mKeyEventCallback = callback2;
        this.mDispatcherState = dispatcherState;
        this.mWindowType = i2;
        this.mGravity = i3;
        this.mTakesFocus = z;
        initDockWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        this.mDispatcherState.reset();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        getWindow().getDecorView().getHitRect(this.mBounds);
        if (motionEvent.isWithinBoundsNoHistory(this.mBounds.left, this.mBounds.top, this.mBounds.right - 1, this.mBounds.bottom - 1)) {
            return super.dispatchTouchEvent(motionEvent);
        }
        MotionEvent motionEventClampNoHistory = motionEvent.clampNoHistory(this.mBounds.left, this.mBounds.top, this.mBounds.right - 1, this.mBounds.bottom - 1);
        boolean zDispatchTouchEvent = super.dispatchTouchEvent(motionEventClampNoHistory);
        motionEventClampNoHistory.recycle();
        return zDispatchTouchEvent;
    }

    public void setGravity(int i) {
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.gravity = i;
        updateWidthHeight(attributes);
        getWindow().setAttributes(attributes);
    }

    public int getGravity() {
        return getWindow().getAttributes().gravity;
    }

    private void updateWidthHeight(WindowManager.LayoutParams layoutParams) {
        if (layoutParams.gravity == 48 || layoutParams.gravity == 80) {
            layoutParams.width = -1;
            layoutParams.height = -2;
        } else {
            layoutParams.width = -2;
            layoutParams.height = -1;
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (this.mKeyEventCallback != null && this.mKeyEventCallback.onKeyDown(i, keyEvent)) {
            return true;
        }
        return super.onKeyDown(i, keyEvent);
    }

    @Override
    public boolean onKeyLongPress(int i, KeyEvent keyEvent) {
        if (this.mKeyEventCallback != null && this.mKeyEventCallback.onKeyLongPress(i, keyEvent)) {
            return true;
        }
        return super.onKeyLongPress(i, keyEvent);
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyEvent) {
        if (this.mKeyEventCallback != null && this.mKeyEventCallback.onKeyUp(i, keyEvent)) {
            return true;
        }
        return super.onKeyUp(i, keyEvent);
    }

    @Override
    public boolean onKeyMultiple(int i, int i2, KeyEvent keyEvent) {
        if (this.mKeyEventCallback != null && this.mKeyEventCallback.onKeyMultiple(i, i2, keyEvent)) {
            return true;
        }
        return super.onKeyMultiple(i, i2, keyEvent);
    }

    @Override
    public void onBackPressed() {
        if (this.mCallback != null) {
            this.mCallback.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void initDockWindow() {
        int i;
        int i2;
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.type = this.mWindowType;
        attributes.setTitle(this.mName);
        attributes.gravity = this.mGravity;
        updateWidthHeight(attributes);
        getWindow().setAttributes(attributes);
        if (!this.mTakesFocus) {
            i = 264;
            i2 = 266;
        } else {
            i = 288;
            i2 = 298;
        }
        getWindow().setFlags(i, i2);
    }
}
