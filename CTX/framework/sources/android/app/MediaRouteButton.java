package android.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRouter;
import android.util.AttributeSet;
import android.view.View;
import com.android.internal.R;
import com.android.internal.app.MediaRouteDialogPresenter;

public class MediaRouteButton extends View {
    private boolean mAttachedToWindow;
    private final MediaRouterCallback mCallback;
    private View.OnClickListener mExtendedSettingsClickListener;
    private boolean mIsConnecting;
    private int mMinHeight;
    private int mMinWidth;
    private boolean mRemoteActive;
    private Drawable mRemoteIndicator;
    private int mRouteTypes;
    private final MediaRouter mRouter;
    private static final int[] CHECKED_STATE_SET = {16842912};
    private static final int[] ACTIVATED_STATE_SET = {16843518};

    public MediaRouteButton(Context context) {
        this(context, null);
    }

    public MediaRouteButton(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 16843693);
    }

    public MediaRouteButton(Context context, AttributeSet attributeSet, int i) {
        this(context, attributeSet, i, 0);
    }

    public MediaRouteButton(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.MediaRouteButton, i, i2);
        setRemoteIndicatorDrawable(typedArrayObtainStyledAttributes.getDrawable(3));
        this.mMinWidth = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 0);
        this.mMinHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(1, 0);
        int integer = typedArrayObtainStyledAttributes.getInteger(2, 1);
        typedArrayObtainStyledAttributes.recycle();
        setClickable(true);
        setRouteTypes(integer);
    }

    public int getRouteTypes() {
        return this.mRouteTypes;
    }

    public void setRouteTypes(int i) {
        if (this.mRouteTypes != i) {
            if (this.mAttachedToWindow && this.mRouteTypes != 0) {
                this.mRouter.removeCallback(this.mCallback);
            }
            this.mRouteTypes = i;
            if (this.mAttachedToWindow && i != 0) {
                this.mRouter.addCallback(i, this.mCallback, 8);
            }
            refreshRoute();
        }
    }

    public void setExtendedSettingsClickListener(View.OnClickListener onClickListener) {
        this.mExtendedSettingsClickListener = onClickListener;
    }

    public void showDialog() {
        showDialogInternal();
    }

    boolean showDialogInternal() {
        return this.mAttachedToWindow && MediaRouteDialogPresenter.showDialogFragment(getActivity(), this.mRouteTypes, this.mExtendedSettingsClickListener) != null;
    }

    private Activity getActivity() {
        for (Context context = getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }
        throw new IllegalStateException("The MediaRouteButton's Context is not an Activity.");
    }

    @Override
    public void setContentDescription(CharSequence charSequence) {
        super.setContentDescription(charSequence);
        setTooltipText(charSequence);
    }

    @Override
    public boolean performClick() {
        boolean zPerformClick = super.performClick();
        if (!zPerformClick) {
            playSoundEffect(0);
        }
        return showDialogInternal() || zPerformClick;
    }

    @Override
    protected int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        if (this.mIsConnecting) {
            mergeDrawableStates(iArrOnCreateDrawableState, CHECKED_STATE_SET);
        } else if (this.mRemoteActive) {
            mergeDrawableStates(iArrOnCreateDrawableState, ACTIVATED_STATE_SET);
        }
        return iArrOnCreateDrawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable drawable = this.mRemoteIndicator;
        if (drawable != null && drawable.isStateful() && drawable.setState(getDrawableState())) {
            invalidateDrawable(drawable);
        }
    }

    private void setRemoteIndicatorDrawable(Drawable drawable) {
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.setCallback(null);
            unscheduleDrawable(this.mRemoteIndicator);
        }
        this.mRemoteIndicator = drawable;
        if (drawable != null) {
            drawable.setCallback(this);
            drawable.setState(getDrawableState());
            drawable.setVisible(getVisibility() == 0, false);
        }
        refreshDrawableState();
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mRemoteIndicator;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.jumpToCurrentState();
        }
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.setVisible(getVisibility() == 0, false);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        if (this.mRouteTypes != 0) {
            this.mRouter.addCallback(this.mRouteTypes, this.mCallback, 8);
        }
        refreshRoute();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mAttachedToWindow = false;
        if (this.mRouteTypes != 0) {
            this.mRouter.removeCallback(this.mCallback);
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int intrinsicWidth;
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int i3 = this.mMinWidth;
        if (this.mRemoteIndicator == null) {
            intrinsicWidth = 0;
        } else {
            intrinsicWidth = this.mRemoteIndicator.getIntrinsicWidth() + getPaddingLeft() + getPaddingRight();
        }
        int iMax = Math.max(i3, intrinsicWidth);
        int iMax2 = Math.max(this.mMinHeight, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom() : 0);
        if (mode != Integer.MIN_VALUE) {
            if (mode != 1073741824) {
                size = iMax;
            }
        } else {
            size = Math.min(size, iMax);
        }
        if (mode2 != Integer.MIN_VALUE) {
            if (mode2 != 1073741824) {
                size2 = iMax2;
            }
        } else {
            size2 = Math.min(size2, iMax2);
        }
        setMeasuredDimension(size, size2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mRemoteIndicator == null) {
            return;
        }
        int paddingLeft = getPaddingLeft();
        int width = getWidth() - getPaddingRight();
        int paddingTop = getPaddingTop();
        int height = getHeight() - getPaddingBottom();
        int intrinsicWidth = this.mRemoteIndicator.getIntrinsicWidth();
        int intrinsicHeight = this.mRemoteIndicator.getIntrinsicHeight();
        int i = paddingLeft + (((width - paddingLeft) - intrinsicWidth) / 2);
        int i2 = paddingTop + (((height - paddingTop) - intrinsicHeight) / 2);
        this.mRemoteIndicator.setBounds(i, i2, intrinsicWidth + i, intrinsicHeight + i2);
        this.mRemoteIndicator.draw(canvas);
    }

    private void refreshRoute() {
        MediaRouter.RouteInfo selectedRoute = this.mRouter.getSelectedRoute();
        boolean z = false;
        boolean z2 = !selectedRoute.isDefault() && selectedRoute.matchesTypes(this.mRouteTypes);
        boolean z3 = z2 && selectedRoute.isConnecting();
        if (this.mRemoteActive != z2) {
            this.mRemoteActive = z2;
            z = true;
        }
        if (this.mIsConnecting != z3) {
            this.mIsConnecting = z3;
            z = true;
        }
        if (z) {
            refreshDrawableState();
        }
        if (this.mAttachedToWindow) {
            setEnabled(this.mRouter.isRouteAvailable(this.mRouteTypes, 1));
        }
        if (this.mRemoteIndicator != null && (this.mRemoteIndicator.getCurrent() instanceof AnimationDrawable)) {
            AnimationDrawable animationDrawable = (AnimationDrawable) this.mRemoteIndicator.getCurrent();
            if (this.mAttachedToWindow) {
                if ((z || z3) && !animationDrawable.isRunning()) {
                    animationDrawable.start();
                    return;
                }
                return;
            }
            if (z2 && !z3) {
                if (animationDrawable.isRunning()) {
                    animationDrawable.stop();
                }
                animationDrawable.selectDrawable(animationDrawable.getNumberOfFrames() - 1);
            }
        }
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        private MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteRemoved(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteChanged(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteSelected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteUnselected(MediaRouter mediaRouter, int i, MediaRouter.RouteInfo routeInfo) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteGrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup, int i) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteUngrouped(MediaRouter mediaRouter, MediaRouter.RouteInfo routeInfo, MediaRouter.RouteGroup routeGroup) {
            MediaRouteButton.this.refreshRoute();
        }
    }
}
