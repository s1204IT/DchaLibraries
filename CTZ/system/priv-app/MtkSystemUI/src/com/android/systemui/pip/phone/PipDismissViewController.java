package com.android.systemui.pip.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.recents.misc.SystemServicesProxy;

/* loaded from: classes.dex */
public class PipDismissViewController {
    private Context mContext;
    private View mDismissView;
    private WindowManager mWindowManager;

    public PipDismissViewController(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
    }

    public void createDismissTarget() throws Resources.NotFoundException {
        if (this.mDismissView == null) {
            Rect rect = new Rect();
            SystemServicesProxy.getInstance(this.mContext).getStableInsets(rect);
            Point point = new Point();
            this.mWindowManager.getDefaultDisplay().getRealSize(point);
            int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_dismiss_gradient_height);
            int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(R.dimen.pip_dismiss_text_bottom_margin);
            this.mDismissView = LayoutInflater.from(this.mContext).inflate(R.layout.pip_dismiss_view, (ViewGroup) null);
            this.mDismissView.setSystemUiVisibility(256);
            this.mDismissView.forceHasOverlappingRendering(false);
            Drawable drawable = this.mContext.getResources().getDrawable(R.drawable.pip_dismiss_scrim);
            drawable.setAlpha(216);
            this.mDismissView.setBackground(drawable);
            ((FrameLayout.LayoutParams) this.mDismissView.findViewById(R.id.pip_dismiss_text).getLayoutParams()).bottomMargin = rect.bottom + dimensionPixelSize2;
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, dimensionPixelSize, 0, point.y - dimensionPixelSize, 2024, 280, -3);
            layoutParams.setTitle("pip-dismiss-overlay");
            layoutParams.privateFlags |= 16;
            layoutParams.gravity = 49;
            this.mWindowManager.addView(this.mDismissView, layoutParams);
        }
        this.mDismissView.animate().cancel();
    }

    public void showDismissTarget() {
        this.mDismissView.animate().alpha(1.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(100L).setDuration(350L).start();
    }

    public void destroyDismissTarget() {
        if (this.mDismissView != null) {
            this.mDismissView.animate().alpha(0.0f).setInterpolator(Interpolators.LINEAR).setStartDelay(0L).setDuration(225L).withEndAction(new Runnable() { // from class: com.android.systemui.pip.phone.PipDismissViewController.1
                @Override // java.lang.Runnable
                public void run() {
                    PipDismissViewController.this.mWindowManager.removeViewImmediate(PipDismissViewController.this.mDismissView);
                    PipDismissViewController.this.mDismissView = null;
                }
            }).start();
        }
    }
}
