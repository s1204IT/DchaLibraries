package android.view.autofill;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.RemoteException;
import android.transition.Transition;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;
import com.android.internal.R;

public class AutofillPopupWindow extends PopupWindow {
    private static final String TAG = "AutofillPopupWindow";
    private boolean mFullScreen;
    private final View.OnAttachStateChangeListener mOnAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            AutofillPopupWindow.this.dismiss();
        }
    };
    private WindowManager.LayoutParams mWindowLayoutParams;
    private final WindowPresenter mWindowPresenter;

    public AutofillPopupWindow(IAutofillWindowPresenter iAutofillWindowPresenter) {
        this.mWindowPresenter = new WindowPresenter(iAutofillWindowPresenter);
        setTouchModal(false);
        setOutsideTouchable(true);
        setInputMethodMode(2);
        setFocusable(true);
    }

    @Override
    protected boolean hasContentView() {
        return true;
    }

    @Override
    protected boolean hasDecorView() {
        return true;
    }

    @Override
    protected WindowManager.LayoutParams getDecorViewLayoutParams() {
        return this.mWindowLayoutParams;
    }

    public void update(View view, int i, int i2, int i3, int i4, Rect rect) {
        int i5;
        int i6;
        int i7;
        final View view2 = view;
        int i8 = i3;
        this.mFullScreen = i8 == -1;
        setWindowLayoutType(this.mFullScreen ? 2008 : 1005);
        if (this.mFullScreen) {
            Point point = new Point();
            view2.getContext().getDisplay().getSize(point);
            int i9 = point.x;
            if (i4 != -1) {
                i7 = point.y - i4;
            } else {
                i7 = 0;
            }
            i8 = i9;
            i6 = i7;
            i5 = 0;
        } else if (rect != null) {
            final int[] iArr = {rect.left, rect.top};
            View view3 = new View(view2.getContext()) {
                @Override
                public void getLocationOnScreen(int[] iArr2) {
                    iArr2[0] = iArr[0];
                    iArr2[1] = iArr[1];
                }

                @Override
                public int getAccessibilityViewId() {
                    return view2.getAccessibilityViewId();
                }

                @Override
                public ViewTreeObserver getViewTreeObserver() {
                    return view2.getViewTreeObserver();
                }

                @Override
                public IBinder getApplicationWindowToken() {
                    return view2.getApplicationWindowToken();
                }

                @Override
                public View getRootView() {
                    return view2.getRootView();
                }

                @Override
                public int getLayoutDirection() {
                    return view2.getLayoutDirection();
                }

                @Override
                public void getWindowDisplayFrame(Rect rect2) {
                    view2.getWindowDisplayFrame(rect2);
                }

                @Override
                public void addOnAttachStateChangeListener(View.OnAttachStateChangeListener onAttachStateChangeListener) {
                    view2.addOnAttachStateChangeListener(onAttachStateChangeListener);
                }

                @Override
                public void removeOnAttachStateChangeListener(View.OnAttachStateChangeListener onAttachStateChangeListener) {
                    view2.removeOnAttachStateChangeListener(onAttachStateChangeListener);
                }

                @Override
                public boolean isAttachedToWindow() {
                    return view2.isAttachedToWindow();
                }

                @Override
                public boolean requestRectangleOnScreen(Rect rect2, boolean z) {
                    return view2.requestRectangleOnScreen(rect2, z);
                }

                @Override
                public IBinder getWindowToken() {
                    return view2.getWindowToken();
                }
            };
            view3.setLeftTopRightBottom(rect.left, rect.top, rect.right, rect.bottom);
            view3.setScrollX(view2.getScrollX());
            view3.setScrollY(view2.getScrollY());
            view2.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public final void onScrollChange(View view4, int i10, int i11, int i12, int i13) {
                    AutofillPopupWindow.lambda$update$0(iArr, view4, i10, i11, i12, i13);
                }
            });
            view3.setWillNotDraw(true);
            i5 = i;
            i6 = i2;
            view2 = view3;
        } else {
            i5 = i;
            i6 = i2;
        }
        if (!this.mFullScreen) {
            setAnimationStyle(-1);
        } else if (i4 == -1) {
            setAnimationStyle(0);
        } else {
            setAnimationStyle(R.style.AutofillHalfScreenAnimation);
        }
        if (!isShowing()) {
            setWidth(i8);
            setHeight(i4);
            showAsDropDown(view2, i5, i6);
            return;
        }
        update(view2, i5, i6, i8, i4);
    }

    static void lambda$update$0(int[] iArr, View view, int i, int i2, int i3, int i4) {
        iArr[0] = iArr[0] - (i - i3);
        iArr[1] = iArr[1] - (i2 - i4);
    }

    @Override
    protected void update(View view, WindowManager.LayoutParams layoutParams) {
        this.mWindowPresenter.show(layoutParams, getTransitionEpicenter(), isLayoutInsetDecor(), view != null ? view.getLayoutDirection() : 3);
    }

    @Override
    protected boolean findDropDownPosition(View view, WindowManager.LayoutParams layoutParams, int i, int i2, int i3, int i4, int i5, boolean z) {
        if (this.mFullScreen) {
            layoutParams.x = i;
            layoutParams.y = i2;
            layoutParams.width = i3;
            layoutParams.height = i4;
            layoutParams.gravity = i5;
            return false;
        }
        return super.findDropDownPosition(view, layoutParams, i, i2, i3, i4, i5, z);
    }

    @Override
    public void showAsDropDown(View view, int i, int i2, int i3) {
        if (Helper.sVerbose) {
            Log.v(TAG, "showAsDropDown(): anchor=" + view + ", xoff=" + i + ", yoff=" + i2 + ", isShowing(): " + isShowing());
        }
        if (isShowing()) {
            return;
        }
        setShowing(true);
        setDropDown(true);
        attachToAnchor(view, i, i2, i3);
        WindowManager.LayoutParams layoutParamsCreatePopupLayoutParams = createPopupLayoutParams(view.getWindowToken());
        this.mWindowLayoutParams = layoutParamsCreatePopupLayoutParams;
        updateAboveAnchor(findDropDownPosition(view, layoutParamsCreatePopupLayoutParams, i, i2, layoutParamsCreatePopupLayoutParams.width, layoutParamsCreatePopupLayoutParams.height, i3, getAllowScrollingAnchorParent()));
        layoutParamsCreatePopupLayoutParams.accessibilityIdOfAnchor = view.getAccessibilityViewId();
        layoutParamsCreatePopupLayoutParams.packageName = view.getContext().getPackageName();
        this.mWindowPresenter.show(layoutParamsCreatePopupLayoutParams, getTransitionEpicenter(), isLayoutInsetDecor(), view.getLayoutDirection());
    }

    @Override
    protected void attachToAnchor(View view, int i, int i2, int i3) {
        super.attachToAnchor(view, i, i2, i3);
        view.addOnAttachStateChangeListener(this.mOnAttachStateChangeListener);
    }

    @Override
    protected void detachFromAnchor() {
        View anchor = getAnchor();
        if (anchor != null) {
            anchor.removeOnAttachStateChangeListener(this.mOnAttachStateChangeListener);
        }
        super.detachFromAnchor();
    }

    @Override
    public void dismiss() {
        if (!isShowing() || isTransitioningToDismiss()) {
            return;
        }
        setShowing(false);
        setTransitioningToDismiss(true);
        this.mWindowPresenter.hide(getTransitionEpicenter());
        detachFromAnchor();
        if (getOnDismissListener() != null) {
            getOnDismissListener().onDismiss();
        }
    }

    @Override
    public int getAnimationStyle() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public Drawable getBackground() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public View getContentView() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public float getElevation() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public Transition getEnterTransition() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public Transition getExitTransition() {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public void setBackgroundDrawable(Drawable drawable) {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public void setContentView(View view) {
        if (view != null) {
            throw new IllegalStateException("You can't call this!");
        }
    }

    @Override
    public void setElevation(float f) {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public void setEnterTransition(Transition transition) {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public void setExitTransition(Transition transition) {
        throw new IllegalStateException("You can't call this!");
    }

    @Override
    public void setTouchInterceptor(View.OnTouchListener onTouchListener) {
        throw new IllegalStateException("You can't call this!");
    }

    private class WindowPresenter {
        final IAutofillWindowPresenter mPresenter;

        WindowPresenter(IAutofillWindowPresenter iAutofillWindowPresenter) {
            this.mPresenter = iAutofillWindowPresenter;
        }

        void show(WindowManager.LayoutParams layoutParams, Rect rect, boolean z, int i) {
            try {
                this.mPresenter.show(layoutParams, rect, z, i);
            } catch (RemoteException e) {
                Log.w(AutofillPopupWindow.TAG, "Error showing fill window", e);
                e.rethrowFromSystemServer();
            }
        }

        void hide(Rect rect) {
            try {
                this.mPresenter.hide(rect);
            } catch (RemoteException e) {
                Log.w(AutofillPopupWindow.TAG, "Error hiding fill window", e);
                e.rethrowFromSystemServer();
            }
        }
    }
}
