package android.widget;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import com.android.internal.widget.AutoScrollHelper;

public class DropDownListView extends ListView {
    private boolean mDrawsInPressedState;
    private boolean mHijackFocus;
    private boolean mListSelectionHidden;
    private ResolveHoverRunnable mResolveHoverRunnable;
    private AutoScrollHelper.AbsListViewAutoScroller mScrollHelper;

    public DropDownListView(Context context, boolean z) {
        this(context, z, 16842861);
    }

    public DropDownListView(Context context, boolean z, int i) {
        super(context, null, i);
        this.mHijackFocus = z;
        setCacheColorHint(0);
    }

    @Override
    boolean shouldShowSelector() {
        return isHovered() || super.shouldShowSelector();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mResolveHoverRunnable != null) {
            this.mResolveHoverRunnable.cancel();
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onHoverEvent(MotionEvent motionEvent) {
        int actionMasked = motionEvent.getActionMasked();
        if (actionMasked == 10 && this.mResolveHoverRunnable == null) {
            this.mResolveHoverRunnable = new ResolveHoverRunnable();
            this.mResolveHoverRunnable.post();
        }
        boolean zOnHoverEvent = super.onHoverEvent(motionEvent);
        if (actionMasked == 9 || actionMasked == 7) {
            int iPointToPosition = pointToPosition((int) motionEvent.getX(), (int) motionEvent.getY());
            if (iPointToPosition != -1 && iPointToPosition != this.mSelectedPosition) {
                View childAt = getChildAt(iPointToPosition - getFirstVisiblePosition());
                if (childAt.isEnabled()) {
                    requestFocus();
                    positionSelector(iPointToPosition, childAt);
                    setSelectedPositionInt(iPointToPosition);
                    setNextSelectedPositionInt(iPointToPosition);
                }
                updateSelectorState();
            }
        } else if (!super.shouldShowSelector()) {
            setSelectedPositionInt(-1);
            setNextSelectedPositionInt(-1);
        }
        return zOnHoverEvent;
    }

    @Override
    protected void drawableStateChanged() {
        if (this.mResolveHoverRunnable == null) {
            super.drawableStateChanged();
        }
    }

    public boolean onForwardedEvent(MotionEvent motionEvent, int i) {
        boolean z;
        boolean z2;
        int iFindPointerIndex;
        int actionMasked = motionEvent.getActionMasked();
        switch (actionMasked) {
            case 1:
                z2 = false;
                iFindPointerIndex = motionEvent.findPointerIndex(i);
                if (iFindPointerIndex < 0) {
                    int x = (int) motionEvent.getX(iFindPointerIndex);
                    int y = (int) motionEvent.getY(iFindPointerIndex);
                    int iPointToPosition = pointToPosition(x, y);
                    if (iPointToPosition != -1) {
                        View childAt = getChildAt(iPointToPosition - getFirstVisiblePosition());
                        setPressedItem(childAt, iPointToPosition, x, y);
                        if (actionMasked == 1) {
                            performItemClick(childAt, iPointToPosition, getItemIdAtPosition(iPointToPosition));
                        }
                        z = false;
                        z2 = true;
                    } else {
                        z = true;
                    }
                } else {
                    z = false;
                    z2 = false;
                }
                break;
            case 2:
                z2 = true;
                iFindPointerIndex = motionEvent.findPointerIndex(i);
                if (iFindPointerIndex < 0) {
                }
                break;
            case 3:
                break;
            default:
                z = false;
                z2 = true;
                break;
        }
        if (!z2 || z) {
            clearPressedItem();
        }
        if (z2) {
            if (this.mScrollHelper == null) {
                this.mScrollHelper = new AutoScrollHelper.AbsListViewAutoScroller(this);
            }
            this.mScrollHelper.setEnabled(true);
            this.mScrollHelper.onTouch(this, motionEvent);
        } else if (this.mScrollHelper != null) {
            this.mScrollHelper.setEnabled(false);
        }
        return z2;
    }

    public void setListSelectionHidden(boolean z) {
        this.mListSelectionHidden = z;
    }

    private void clearPressedItem() {
        this.mDrawsInPressedState = false;
        setPressed(false);
        updateSelectorState();
        View childAt = getChildAt(this.mMotionPosition - this.mFirstPosition);
        if (childAt != null) {
            childAt.setPressed(false);
        }
    }

    private void setPressedItem(View view, int i, float f, float f2) {
        this.mDrawsInPressedState = true;
        drawableHotspotChanged(f, f2);
        if (!isPressed()) {
            setPressed(true);
        }
        if (this.mDataChanged) {
            layoutChildren();
        }
        View childAt = getChildAt(this.mMotionPosition - this.mFirstPosition);
        if (childAt != null && childAt != view && childAt.isPressed()) {
            childAt.setPressed(false);
        }
        this.mMotionPosition = i;
        view.drawableHotspotChanged(f - view.getLeft(), f2 - view.getTop());
        if (!view.isPressed()) {
            view.setPressed(true);
        }
        setSelectedPositionInt(i);
        positionSelectorLikeTouch(i, view, f, f2);
        refreshDrawableState();
    }

    @Override
    boolean touchModeDrawsInPressedState() {
        return this.mDrawsInPressedState || super.touchModeDrawsInPressedState();
    }

    @Override
    View obtainView(int i, boolean[] zArr) {
        View viewObtainView = super.obtainView(i, zArr);
        if (viewObtainView instanceof TextView) {
            ((TextView) viewObtainView).setHorizontallyScrolling(true);
        }
        return viewObtainView;
    }

    @Override
    public boolean isInTouchMode() {
        return (this.mHijackFocus && this.mListSelectionHidden) || super.isInTouchMode();
    }

    @Override
    public boolean hasWindowFocus() {
        return this.mHijackFocus || super.hasWindowFocus();
    }

    @Override
    public boolean isFocused() {
        return this.mHijackFocus || super.isFocused();
    }

    @Override
    public boolean hasFocus() {
        return this.mHijackFocus || super.hasFocus();
    }

    private class ResolveHoverRunnable implements Runnable {
        private ResolveHoverRunnable() {
        }

        @Override
        public void run() {
            DropDownListView.this.mResolveHoverRunnable = null;
            DropDownListView.this.drawableStateChanged();
        }

        public void cancel() {
            DropDownListView.this.mResolveHoverRunnable = null;
            DropDownListView.this.removeCallbacks(this);
        }

        public void post() {
            DropDownListView.this.post(this);
        }
    }
}
