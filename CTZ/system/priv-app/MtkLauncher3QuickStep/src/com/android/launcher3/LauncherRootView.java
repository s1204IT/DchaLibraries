package com.android.launcher3;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;

/* loaded from: classes.dex */
public class LauncherRootView extends InsettableFrameLayout {
    private View mAlignedView;

    @ViewDebug.ExportedProperty(category = "launcher")
    private final Rect mConsumedInsets;
    private final Launcher mLauncher;
    private final Paint mOpaquePaint;
    private WindowStateListener mWindowStateListener;

    public interface WindowStateListener {
        void onWindowFocusChanged(boolean z);

        void onWindowVisibilityChanged(int i);
    }

    public LauncherRootView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mConsumedInsets = new Rect();
        this.mOpaquePaint = new Paint(1);
        this.mOpaquePaint.setColor(ViewCompat.MEASURED_STATE_MASK);
        this.mOpaquePaint.setStyle(Paint.Style.FILL);
        this.mLauncher = Launcher.getLauncher(context);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        if (getChildCount() > 0) {
            this.mAlignedView = getChildAt(0);
        }
        super.onFinishInflate();
    }

    /* JADX DEBUG: Don't trust debug lines info. Repeating lines: [70=4] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0079  */
    /* JADX WARN: Removed duplicated region for block: B:28:0x0095  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x00d4  */
    @Override // android.view.View
    @TargetApi(23)
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    protected boolean fitSystemWindows(Rect rect) {
        Rect rect2;
        boolean z;
        boolean z2;
        this.mConsumedInsets.setEmpty();
        if (this.mLauncher.isInMultiWindowModeCompat() && (rect.left > 0 || rect.right > 0 || rect.bottom > 0)) {
            this.mConsumedInsets.left = rect.left;
            this.mConsumedInsets.right = rect.right;
            this.mConsumedInsets.bottom = rect.bottom;
            rect2 = new Rect(0, rect.top, 0, 0);
        } else {
            if ((rect.right <= 0 && rect.left <= 0) || (Utilities.ATLEAST_MARSHMALLOW && !((ActivityManager) getContext().getSystemService(ActivityManager.class)).isLowRamDevice())) {
                rect2 = rect;
                z = false;
                this.mLauncher.getSystemUiController().updateUiState(3, z ? 2 : 0);
                this.mLauncher.getDeviceProfile().updateInsets(rect2);
                z2 = !rect2.equals(this.mInsets);
                setInsets(rect2);
                if (this.mAlignedView != null) {
                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mAlignedView.getLayoutParams();
                    if (marginLayoutParams.leftMargin != this.mConsumedInsets.left || marginLayoutParams.rightMargin != this.mConsumedInsets.right || marginLayoutParams.bottomMargin != this.mConsumedInsets.bottom) {
                        marginLayoutParams.leftMargin = this.mConsumedInsets.left;
                        marginLayoutParams.rightMargin = this.mConsumedInsets.right;
                        marginLayoutParams.topMargin = this.mConsumedInsets.top;
                        marginLayoutParams.bottomMargin = this.mConsumedInsets.bottom;
                        this.mAlignedView.setLayoutParams(marginLayoutParams);
                    }
                }
                if (z2) {
                    this.mLauncher.getStateManager().reapplyState(true);
                }
                return true;
            }
            this.mConsumedInsets.left = rect.left;
            this.mConsumedInsets.right = rect.right;
            rect2 = new Rect(0, rect.top, 0, rect.bottom);
        }
        z = true;
        this.mLauncher.getSystemUiController().updateUiState(3, z ? 2 : 0);
        this.mLauncher.getDeviceProfile().updateInsets(rect2);
        z2 = !rect2.equals(this.mInsets);
        setInsets(rect2);
        if (this.mAlignedView != null) {
        }
        if (z2) {
        }
        return true;
    }

    @Override // com.android.launcher3.InsettableFrameLayout, com.android.launcher3.Insettable
    public void setInsets(Rect rect) {
        if (!rect.equals(this.mInsets)) {
            super.setInsets(rect);
        }
    }

    public void dispatchInsets() {
        this.mLauncher.getDeviceProfile().updateInsets(this.mInsets);
        super.setInsets(this.mInsets);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (this.mConsumedInsets.right > 0) {
            canvas.drawRect(r0 - this.mConsumedInsets.right, 0.0f, getWidth(), getHeight(), this.mOpaquePaint);
        }
        if (this.mConsumedInsets.left > 0) {
            canvas.drawRect(0.0f, 0.0f, this.mConsumedInsets.left, getHeight(), this.mOpaquePaint);
        }
        if (this.mConsumedInsets.bottom > 0) {
            canvas.drawRect(0.0f, r0 - this.mConsumedInsets.bottom, getWidth(), getHeight(), this.mOpaquePaint);
        }
    }

    public void setWindowStateListener(WindowStateListener windowStateListener) {
        this.mWindowStateListener = windowStateListener;
    }

    @Override // android.view.View
    public void onWindowFocusChanged(boolean z) {
        super.onWindowFocusChanged(z);
        if (this.mWindowStateListener != null) {
            this.mWindowStateListener.onWindowFocusChanged(z);
        }
    }

    @Override // android.view.View
    protected void onWindowVisibilityChanged(int i) {
        super.onWindowVisibilityChanged(i);
        if (this.mWindowStateListener != null) {
            this.mWindowStateListener.onWindowVisibilityChanged(i);
        }
    }
}
