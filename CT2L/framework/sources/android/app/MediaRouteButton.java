package android.app;

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
import com.android.internal.R;
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
    private static final int[] CHECKED_STATE_SET = {16842912};
    private static final int[] ACTIVATED_STATE_SET = {16843518};

    public MediaRouteButton(Context context) {
        this(context, null);
    }

    public MediaRouteButton(Context context, AttributeSet attrs) {
        this(context, attrs, 16843693);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MediaRouteButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mRouter = (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        this.mCallback = new MediaRouterCallback();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MediaRouteButton, defStyleAttr, defStyleRes);
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
        if (this.mRouteTypes != types) {
            if (this.mAttachedToWindow && this.mRouteTypes != 0) {
                this.mRouter.removeCallback(this.mCallback);
            }
            this.mRouteTypes = types;
            if (this.mAttachedToWindow && types != 0) {
                this.mRouter.addCallback(types, this.mCallback, 8);
            }
            refreshRoute();
        }
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
        for (Context context = getContext(); context instanceof ContextWrapper; context = ((ContextWrapper) context).getBaseContext()) {
            if (context instanceof Activity) {
                return (Activity) context;
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
        return showDialogInternal() || handled;
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
        if (this.mRemoteIndicator != null) {
            int[] myDrawableState = getDrawableState();
            this.mRemoteIndicator.setState(myDrawableState);
            invalidate();
        }
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
        if (this.mRemoteIndicator != null) {
            this.mRemoteIndicator.jumpToCurrentState();
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
        int width;
        int height;
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);
        int minWidth = Math.max(this.mMinWidth, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicWidth() : 0);
        int minHeight = Math.max(this.mMinHeight, this.mRemoteIndicator != null ? this.mRemoteIndicator.getIntrinsicHeight() : 0);
        switch (widthMode) {
            case Integer.MIN_VALUE:
                width = Math.min(widthSize, getPaddingLeft() + minWidth + getPaddingRight());
                break;
            case 1073741824:
                width = widthSize;
                break;
            default:
                width = getPaddingLeft() + minWidth + getPaddingRight();
                break;
        }
        switch (heightMode) {
            case Integer.MIN_VALUE:
                height = Math.min(heightSize, getPaddingTop() + minHeight + getPaddingBottom());
                break;
            case 1073741824:
                height = heightSize;
                break;
            default:
                height = getPaddingTop() + minHeight + getPaddingBottom();
                break;
        }
        setMeasuredDimension(width, height);
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
            int drawLeft = left + (((right - left) - drawWidth) / 2);
            int drawTop = top + (((bottom - top) - drawHeight) / 2);
            this.mRemoteIndicator.setBounds(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
            this.mRemoteIndicator.draw(canvas);
        }
    }

    private void refreshRoute() {
        boolean isConnecting = false;
        if (this.mAttachedToWindow) {
            MediaRouter.RouteInfo route = this.mRouter.getSelectedRoute();
            boolean isRemote = !route.isDefault() && route.matchesTypes(this.mRouteTypes);
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
                refreshDrawableState();
            }
            setEnabled(this.mRouter.isRouteAvailable(this.mRouteTypes, 1));
        }
    }

    private final class MediaRouterCallback extends MediaRouter.SimpleCallback {
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
