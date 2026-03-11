package android.support.v7.app;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;

public class MediaRouteButton extends View {
    private boolean mAttachedToWindow;
    private ColorStateList mButtonTint;
    private final MediaRouterCallback mCallback;
    private MediaRouteDialogFactory mDialogFactory;
    private boolean mIsConnecting;
    private int mMinHeight;
    private int mMinWidth;
    private boolean mRemoteActive;
    private Drawable mRemoteIndicator;
    private RemoteIndicatorLoader mRemoteIndicatorLoader;
    private final MediaRouter mRouter;
    private MediaRouteSelector mSelector;
    private static final SparseArray<Drawable.ConstantState> sRemoteIndicatorCache = new SparseArray<>(2);
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private static final int[] CHECKABLE_STATE_SET = {R.attr.state_checkable};

    public MediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, android.support.v7.mediarouter.R.attr.mediaRouteButtonStyle);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(MediaRouterThemeHelper.createThemedButtonContext(context), attrs, defStyleAttr);
        this.mSelector = MediaRouteSelector.EMPTY;
        this.mDialogFactory = MediaRouteDialogFactory.getDefault();
        Context context2 = getContext();
        this.mRouter = MediaRouter.getInstance(context2);
        this.mCallback = new MediaRouterCallback();
        TypedArray a = context2.obtainStyledAttributes(attrs, android.support.v7.mediarouter.R.styleable.MediaRouteButton, defStyleAttr, 0);
        this.mButtonTint = a.getColorStateList(android.support.v7.mediarouter.R.styleable.MediaRouteButton_mediaRouteButtonTint);
        this.mMinWidth = a.getDimensionPixelSize(android.support.v7.mediarouter.R.styleable.MediaRouteButton_android_minWidth, 0);
        this.mMinHeight = a.getDimensionPixelSize(android.support.v7.mediarouter.R.styleable.MediaRouteButton_android_minHeight, 0);
        int remoteIndicatorResId = a.getResourceId(android.support.v7.mediarouter.R.styleable.MediaRouteButton_externalRouteEnabledDrawable, 0);
        a.recycle();
        if (remoteIndicatorResId != 0) {
            Drawable.ConstantState remoteIndicatorState = sRemoteIndicatorCache.get(remoteIndicatorResId);
            if (remoteIndicatorState != null) {
                setRemoteIndicatorDrawable(remoteIndicatorState.newDrawable());
            } else {
                this.mRemoteIndicatorLoader = new RemoteIndicatorLoader(remoteIndicatorResId);
                this.mRemoteIndicatorLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            }
        }
        updateContentDescription();
        setClickable(true);
    }

    public boolean showDialog() {
        if (!this.mAttachedToWindow) {
            return false;
        }
        FragmentManager fm = getFragmentManager();
        if (fm == null) {
            throw new IllegalStateException("The activity must be a subclass of FragmentActivity");
        }
        MediaRouter.RouteInfo route = this.mRouter.getSelectedRoute();
        if (route.isDefaultOrBluetooth() || !route.matchesSelector(this.mSelector)) {
            if (fm.findFragmentByTag("android.support.v7.mediarouter:MediaRouteChooserDialogFragment") != null) {
                Log.w("MediaRouteButton", "showDialog(): Route chooser dialog already showing!");
                return false;
            }
            MediaRouteChooserDialogFragment f = this.mDialogFactory.onCreateChooserDialogFragment();
            f.setRouteSelector(this.mSelector);
            f.show(fm, "android.support.v7.mediarouter:MediaRouteChooserDialogFragment");
            return true;
        }
        if (fm.findFragmentByTag("android.support.v7.mediarouter:MediaRouteControllerDialogFragment") != null) {
            Log.w("MediaRouteButton", "showDialog(): Route controller dialog already showing!");
            return false;
        }
        this.mDialogFactory.onCreateControllerDialogFragment().show(fm, "android.support.v7.mediarouter:MediaRouteControllerDialogFragment");
        return true;
    }

    private FragmentManager getFragmentManager() {
        Activity activity = getActivity();
        if (activity instanceof FragmentActivity) {
            return ((FragmentActivity) activity).getSupportFragmentManager();
        }
        return null;
    }

    private Activity getActivity() {
        for (Context context = getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
        }
        return null;
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();
        if (!handled) {
            playSoundEffect(0);
        }
        return showDialog() || handled;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (this.mIsConnecting) {
            mergeDrawableStates(drawableState, CHECKABLE_STATE_SET);
        } else if (this.mRemoteActive) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (this.mRemoteIndicator != null) {
            int[] myDrawableState = getDrawableState();
            this.mRemoteIndicator.setState(myDrawableState);
            invalidate();
        }
    }

    public void setRemoteIndicatorDrawable(Drawable d) {
        if (this.mRemoteIndicatorLoader != null) {
            this.mRemoteIndicatorLoader.cancel(false);
        }
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.setCallback(null);
            unscheduleDrawable(this.mRemoteIndicator);
        }
        if (d != null) {
            if (this.mButtonTint != null) {
                d = DrawableCompat.wrap(d.mutate());
                DrawableCompat.setTintList(d, this.mButtonTint);
            }
            d.setCallback(this);
            d.setState(getDrawableState());
            d.setVisible(getVisibility() == 0, false);
        }
        this.mRemoteIndicator = d;
        refreshDrawableState();
        if (this.mAttachedToWindow && this.mRemoteIndicator != null && (this.mRemoteIndicator.getCurrent() instanceof AnimationDrawable)) {
            AnimationDrawable curDrawable = (AnimationDrawable) this.mRemoteIndicator.getCurrent();
            if (this.mIsConnecting) {
                if (!curDrawable.isRunning()) {
                    curDrawable.start();
                }
            } else if (this.mRemoteActive) {
                if (curDrawable.isRunning()) {
                    curDrawable.stop();
                }
                curDrawable.selectDrawable(curDrawable.getNumberOfFrames() - 1);
            }
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mRemoteIndicator;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        if (getBackground() != null) {
            DrawableCompat.jumpToCurrentState(getBackground());
        }
        if (this.mRemoteIndicator != null) {
            DrawableCompat.jumpToCurrentState(this.mRemoteIndicator);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.setVisible(getVisibility() == 0, false);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mAttachedToWindow = true;
        if (!this.mSelector.isEmpty()) {
            this.mRouter.addCallback(this.mSelector, this.mCallback);
        }
        refreshRoute();
    }

    @Override
    public void onDetachedFromWindow() {
        this.mAttachedToWindow = false;
        if (!this.mSelector.isEmpty()) {
            this.mRouter.removeCallback(this.mCallback);
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int intrinsicWidth;
        int measuredWidth;
        int measuredHeight;
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int i = this.mMinWidth;
        if (this.mRemoteIndicator == null) {
            intrinsicWidth = 0;
        } else {
            intrinsicWidth = this.mRemoteIndicator.getIntrinsicWidth() + getPaddingLeft() + getPaddingRight();
        }
        int width = Math.max(i, intrinsicWidth);
        int height = Math.max(this.mMinHeight, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom() : 0);
        if (widthMode != Integer.MIN_VALUE) {
            if (widthMode == 1073741824) {
                measuredWidth = widthSize;
            } else {
                measuredWidth = width;
            }
        } else {
            measuredWidth = Math.min(widthSize, width);
        }
        if (heightMode != Integer.MIN_VALUE) {
            if (heightMode == 1073741824) {
                measuredHeight = heightSize;
            } else {
                measuredHeight = height;
            }
        } else {
            measuredHeight = Math.min(heightSize, height);
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mRemoteIndicator != null) {
            int left = getPaddingLeft();
            int right = getWidth() - getPaddingRight();
            int top = getPaddingTop();
            int bottom = getHeight() - getPaddingBottom();
            int drawWidth = this.mRemoteIndicator.getIntrinsicWidth();
            int drawHeight = this.mRemoteIndicator.getIntrinsicHeight();
            int drawLeft = (((right - left) - drawWidth) / 2) + left;
            int drawTop = (((bottom - top) - drawHeight) / 2) + top;
            this.mRemoteIndicator.setBounds(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
            this.mRemoteIndicator.draw(canvas);
        }
    }

    void refreshRoute() {
        MediaRouter.RouteInfo route = this.mRouter.getSelectedRoute();
        boolean isConnecting = false;
        boolean isRemote = !route.isDefaultOrBluetooth() && route.matchesSelector(this.mSelector);
        if (isRemote && route.isConnecting()) {
            isConnecting = true;
        }
        boolean needsRefresh = false;
        if (this.mRemoteActive != isRemote) {
            this.mRemoteActive = isRemote;
            needsRefresh = true;
        }
        if (this.mIsConnecting != isConnecting) {
            this.mIsConnecting = isConnecting;
            needsRefresh = true;
        }
        if (needsRefresh) {
            updateContentDescription();
            refreshDrawableState();
        }
        if (this.mAttachedToWindow) {
            setEnabled(this.mRouter.isRouteAvailable(this.mSelector, 1));
        }
        if (this.mRemoteIndicator != null && (this.mRemoteIndicator.getCurrent() instanceof AnimationDrawable)) {
            AnimationDrawable curDrawable = (AnimationDrawable) this.mRemoteIndicator.getCurrent();
            if (this.mAttachedToWindow) {
                if ((needsRefresh || isConnecting) && !curDrawable.isRunning()) {
                    curDrawable.start();
                    return;
                }
                return;
            }
            if (isRemote && !isConnecting) {
                if (curDrawable.isRunning()) {
                    curDrawable.stop();
                }
                curDrawable.selectDrawable(curDrawable.getNumberOfFrames() - 1);
            }
        }
    }

    private void updateContentDescription() {
        int resId;
        if (this.mIsConnecting) {
            resId = android.support.v7.mediarouter.R.string.mr_cast_button_connecting;
        } else if (this.mRemoteActive) {
            resId = android.support.v7.mediarouter.R.string.mr_cast_button_connected;
        } else {
            resId = android.support.v7.mediarouter.R.string.mr_cast_button_disconnected;
        }
        setContentDescription(getContext().getString(resId));
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onProviderAdded(MediaRouter router, MediaRouter.ProviderInfo provider) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onProviderRemoved(MediaRouter router, MediaRouter.ProviderInfo provider) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onProviderChanged(MediaRouter router, MediaRouter.ProviderInfo provider) {
            MediaRouteButton.this.refreshRoute();
        }
    }

    private final class RemoteIndicatorLoader extends AsyncTask<Void, Void, Drawable> {
        private final int mResId;

        RemoteIndicatorLoader(int resId) {
            this.mResId = resId;
        }

        @Override
        public Drawable doInBackground(Void... params) {
            return MediaRouteButton.this.getContext().getResources().getDrawable(this.mResId);
        }

        @Override
        public void onPostExecute(Drawable remoteIndicator) {
            cacheAndReset(remoteIndicator);
            MediaRouteButton.this.setRemoteIndicatorDrawable(remoteIndicator);
        }

        @Override
        public void onCancelled(Drawable remoteIndicator) {
            cacheAndReset(remoteIndicator);
        }

        private void cacheAndReset(Drawable remoteIndicator) {
            if (remoteIndicator != null) {
                MediaRouteButton.sRemoteIndicatorCache.put(this.mResId, remoteIndicator.getConstantState());
            }
            MediaRouteButton.this.mRemoteIndicatorLoader = null;
        }
    }
}
