package android.app;

import android.R;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaRouter;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;
import com.android.internal.app.MediaRouteDialogPresenter;

public class MediaRouteButton extends View {
    private boolean mAttachedToWindow;
    private final MediaRouterCallback mCallback;
    private boolean mCheatSheetEnabled;
    private View.OnClickListener mExtendedSettingsClickListener;
    private boolean mIsConnecting;
    private int mMinHeight;
    private int mMinWidth;
    private boolean mRemoteActive;
    private Drawable mRemoteIndicator;
    private int mRouteTypes;
    private final MediaRouter mRouter;
    private static final int[] CHECKED_STATE_SET = {R.attr.state_checked};
    private static final int[] ACTIVATED_STATE_SET = {R.attr.state_activated};

    public MediaRouteButton(Context context) {
        this(context, null);
    }

    public MediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.mediaRouteButtonStyle);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback(this, null);
        TypedArray a = context.obtainStyledAttributes(attrs, com.android.internal.R.styleable.MediaRouteButton, defStyleAttr, defStyleRes);
        setRemoteIndicatorDrawable(a.getDrawable(3));
        this.mMinWidth = a.getDimensionPixelSize(0, 0);
        this.mMinHeight = a.getDimensionPixelSize(1, 0);
        int routeTypes = a.getInteger(2, 1);
        a.recycle();
        setClickable(true);
        setLongClickable(true);
        setRouteTypes(routeTypes);
    }

    public int getRouteTypes() {
        return this.mRouteTypes;
    }

    public void setRouteTypes(int types) {
        if (this.mRouteTypes == types) {
            return;
        }
        if (this.mAttachedToWindow && this.mRouteTypes != 0) {
            this.mRouter.removeCallback(this.mCallback);
        }
        this.mRouteTypes = types;
        if (this.mAttachedToWindow && types != 0) {
            this.mRouter.addCallback(types, this.mCallback, 8);
        }
        refreshRoute();
    }

    public void setExtendedSettingsClickListener(View.OnClickListener listener) {
        this.mExtendedSettingsClickListener = listener;
    }

    public void showDialog() {
        showDialogInternal();
    }

    boolean showDialogInternal() {
        if (!this.mAttachedToWindow) {
            return false;
        }
        DialogFragment f = MediaRouteDialogPresenter.showDialogFragment(getActivity(), this.mRouteTypes, this.mExtendedSettingsClickListener);
        return f != null;
    }

    private Activity getActivity() {
        for (?? context = getContext(); context instanceof ContextWrapper; context = context.getBaseContext()) {
            if (context instanceof Activity) {
                return context;
            }
        }
        throw new IllegalStateException("The MediaRouteButton's Context is not an Activity.");
    }

    void setCheatSheetEnabled(boolean enable) {
        this.mCheatSheetEnabled = enable;
    }

    @Override
    public boolean performClick() {
        boolean handled = super.performClick();
        if (!handled) {
            playSoundEffect(0);
        }
        if (showDialogInternal()) {
            return true;
        }
        return handled;
    }

    @Override
    public boolean performLongClick() {
        if (super.performLongClick()) {
            return true;
        }
        if (!this.mCheatSheetEnabled) {
            return false;
        }
        CharSequence contentDesc = getContentDescription();
        if (TextUtils.isEmpty(contentDesc)) {
            return false;
        }
        int[] screenPos = new int[2];
        Rect displayFrame = new Rect();
        getLocationOnScreen(screenPos);
        getWindowVisibleDisplayFrame(displayFrame);
        Context context = getContext();
        int width = getWidth();
        int height = getHeight();
        int midy = screenPos[1] + (height / 2);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        Toast cheatSheet = Toast.makeText(context, contentDesc, 0);
        if (midy < displayFrame.height()) {
            cheatSheet.setGravity(8388661, (screenWidth - screenPos[0]) - (width / 2), height);
        } else {
            cheatSheet.setGravity(81, 0, height);
        }
        cheatSheet.show();
        performHapticFeedback(0);
        return true;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (this.mIsConnecting) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        } else if (this.mRemoteActive) {
            mergeDrawableStates(drawableState, ACTIVATED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        Drawable remoteIndicator = this.mRemoteIndicator;
        if (remoteIndicator == null || !remoteIndicator.isStateful() || !remoteIndicator.setState(getDrawableState())) {
            return;
        }
        invalidateDrawable(remoteIndicator);
    }

    private void setRemoteIndicatorDrawable(Drawable d) {
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.setCallback(null);
            unscheduleDrawable(this.mRemoteIndicator);
        }
        this.mRemoteIndicator = d;
        if (d != null) {
            d.setCallback(this);
            d.setState(getDrawableState());
            d.setVisible(getVisibility() == 0, false);
        }
        refreshDrawableState();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == this.mRemoteIndicator;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (this.mRemoteIndicator == null) {
            return;
        }
        this.mRemoteIndicator.jumpToCurrentState();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (this.mRemoteIndicator == null) {
            return;
        }
        this.mRemoteIndicator.setVisible(getVisibility() == 0, false);
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth;
        int measuredHeight;
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int width = Math.max(this.mMinWidth, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicWidth() + getPaddingLeft() + getPaddingRight() : 0);
        int height = Math.max(this.mMinHeight, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicHeight() + getPaddingTop() + getPaddingBottom() : 0);
        switch (widthMode) {
            case Integer.MIN_VALUE:
                measuredWidth = Math.min(widthSize, width);
                break;
            case 0:
            default:
                measuredWidth = width;
                break;
            case 1073741824:
                measuredWidth = widthSize;
                break;
        }
        switch (heightMode) {
            case Integer.MIN_VALUE:
                measuredHeight = Math.min(heightSize, height);
                break;
            case 0:
            default:
                measuredHeight = height;
                break;
            case 1073741824:
                measuredHeight = heightSize;
                break;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mRemoteIndicator == null) {
            return;
        }
        int left = getPaddingLeft();
        int right = getWidth() - getPaddingRight();
        int top = getPaddingTop();
        int bottom = getHeight() - getPaddingBottom();
        int drawWidth = this.mRemoteIndicator.getIntrinsicWidth();
        int drawHeight = this.mRemoteIndicator.getIntrinsicHeight();
        int drawLeft = left + (((right - left) - drawWidth) / 2);
        int drawTop = top + (((bottom - top) - drawHeight) / 2);
        this.mRemoteIndicator.setBounds(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
        this.mRemoteIndicator.draw(canvas);
    }

    private void refreshRoute() {
        if (!this.mAttachedToWindow) {
            return;
        }
        MediaRouter.RouteInfo route = this.mRouter.getSelectedRoute();
        boolean isRemote = !route.isDefault() ? route.matchesTypes(this.mRouteTypes) : false;
        boolean zIsConnecting = isRemote ? route.isConnecting() : false;
        boolean needsRefresh = false;
        if (this.mRemoteActive != isRemote) {
            this.mRemoteActive = isRemote;
            needsRefresh = true;
        }
        if (this.mIsConnecting != zIsConnecting) {
            this.mIsConnecting = zIsConnecting;
            needsRefresh = true;
        }
        if (needsRefresh) {
            refreshDrawableState();
        }
        setEnabled(this.mRouter.isRouteAvailable(this.mRouteTypes, 1));
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
        MediaRouterCallback(MediaRouteButton this$0, MediaRouterCallback mediaRouterCallback) {
            this();
        }

        private MediaRouterCallback() {
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
        public void onRouteSelected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteUnselected(MediaRouter router, int type, MediaRouter.RouteInfo info) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteGrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group, int index) {
            MediaRouteButton.this.refreshRoute();
        }

        @Override
        public void onRouteUngrouped(MediaRouter router, MediaRouter.RouteInfo info, MediaRouter.RouteGroup group) {
            MediaRouteButton.this.refreshRoute();
        }
    }
}
