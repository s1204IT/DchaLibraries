package android.support.v7.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.widget.ListViewAutoScrollHelper;
import android.support.v7.appcompat.R;
import android.support.v7.graphics.drawable.DrawableWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.lang.reflect.Field;

class DropDownListView extends ListView {
    public static final int INVALID_POSITION = -1;
    public static final int NO_POSITION = -1;
    private ViewPropertyAnimatorCompat mClickAnimation;
    private boolean mDrawsInPressedState;
    private boolean mHijackFocus;
    private Field mIsChildViewEnabled;
    private boolean mListSelectionHidden;
    private int mMotionPosition;
    private ResolveHoverRunnable mResolveHoverRunnable;
    private ListViewAutoScrollHelper mScrollHelper;
    private int mSelectionBottomPadding;
    private int mSelectionLeftPadding;
    private int mSelectionRightPadding;
    private int mSelectionTopPadding;
    private GateKeeperDrawable mSelector;
    private final Rect mSelectorRect;

    DropDownListView(Context context, boolean hijackFocus) {
        super(context, null, R.attr.dropDownListViewStyle);
        this.mSelectorRect = new Rect();
        this.mSelectionLeftPadding = 0;
        this.mSelectionTopPadding = 0;
        this.mSelectionRightPadding = 0;
        this.mSelectionBottomPadding = 0;
        this.mHijackFocus = hijackFocus;
        setCacheColorHint(0);
        try {
            this.mIsChildViewEnabled = AbsListView.class.getDeclaredField("mIsChildViewEnabled");
            this.mIsChildViewEnabled.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
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

    @Override
    public void setSelector(Drawable sel) {
        this.mSelector = sel != null ? new GateKeeperDrawable(sel) : null;
        super.setSelector(this.mSelector);
        Rect padding = new Rect();
        if (sel != null) {
            sel.getPadding(padding);
        }
        this.mSelectionLeftPadding = padding.left;
        this.mSelectionTopPadding = padding.top;
        this.mSelectionRightPadding = padding.right;
        this.mSelectionBottomPadding = padding.bottom;
    }

    @Override
    protected void drawableStateChanged() {
        if (this.mResolveHoverRunnable != null) {
            return;
        }
        super.drawableStateChanged();
        setSelectorEnabled(true);
        updateSelectorStateCompat();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        drawSelectorCompat(canvas);
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            this.mMotionPosition = pointToPosition((int) ev.getX(), (int) ev.getY());
        }
        if (this.mResolveHoverRunnable != null) {
            this.mResolveHoverRunnable.cancel();
        }
        return super.onTouchEvent(ev);
    }

    public int lookForSelectablePosition(int position, boolean lookDown) {
        int position2;
        ListAdapter adapter = getAdapter();
        if (adapter == null || isInTouchMode()) {
            return -1;
        }
        int count = adapter.getCount();
        if (!getAdapter().areAllItemsEnabled()) {
            if (lookDown) {
                position2 = Math.max(0, position);
                while (position2 < count && !adapter.isEnabled(position2)) {
                    position2++;
                }
            } else {
                position2 = Math.min(position, count - 1);
                while (position2 >= 0 && !adapter.isEnabled(position2)) {
                    position2--;
                }
            }
            if (position2 < 0 || position2 >= count) {
                return -1;
            }
            return position2;
        }
        if (position < 0 || position >= count) {
            return -1;
        }
        return position;
    }

    public int measureHeightOfChildrenCompat(int widthMeasureSpec, int startPosition, int endPosition, int maxHeight, int disallowPartialChildPosition) {
        int paddingBottom;
        ViewGroup.LayoutParams childLp;
        int heightMeasureSpec;
        int paddingTop = getListPaddingTop();
        int paddingBottom2 = getListPaddingBottom();
        getListPaddingLeft();
        getListPaddingRight();
        int reportedDividerHeight = getDividerHeight();
        Drawable divider = getDivider();
        ListAdapter adapter = getAdapter();
        if (adapter == null) {
            return paddingTop + paddingBottom2;
        }
        int returnedHeight = paddingTop + paddingBottom2;
        int dividerHeight = (reportedDividerHeight <= 0 || divider == null) ? 0 : reportedDividerHeight;
        View child = null;
        int viewType = 0;
        int count = adapter.getCount();
        int prevHeightWithoutPartialChild = 0;
        int returnedHeight2 = returnedHeight;
        int returnedHeight3 = 0;
        while (returnedHeight3 < count) {
            int newType = adapter.getItemViewType(returnedHeight3);
            if (newType != viewType) {
                child = null;
                viewType = newType;
            }
            int paddingTop2 = paddingTop;
            child = adapter.getView(returnedHeight3, child, this);
            ViewGroup.LayoutParams childLp2 = child.getLayoutParams();
            if (childLp2 == null) {
                paddingBottom = paddingBottom2;
                childLp = generateDefaultLayoutParams();
                child.setLayoutParams(childLp);
            } else {
                paddingBottom = paddingBottom2;
                childLp = childLp2;
            }
            if (childLp.height > 0) {
                int heightMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(childLp.height, 1073741824);
                heightMeasureSpec = heightMeasureSpec2;
            } else {
                heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
            }
            child.measure(widthMeasureSpec, heightMeasureSpec);
            child.forceLayout();
            if (returnedHeight3 > 0) {
                returnedHeight2 += dividerHeight;
            }
            returnedHeight2 += child.getMeasuredHeight();
            if (returnedHeight2 >= maxHeight) {
                return (disallowPartialChildPosition < 0 || returnedHeight3 <= disallowPartialChildPosition || prevHeightWithoutPartialChild <= 0 || returnedHeight2 == maxHeight) ? maxHeight : prevHeightWithoutPartialChild;
            }
            if (disallowPartialChildPosition >= 0 && returnedHeight3 >= disallowPartialChildPosition) {
                prevHeightWithoutPartialChild = returnedHeight2;
            }
            returnedHeight3++;
            paddingTop = paddingTop2;
            paddingBottom2 = paddingBottom;
        }
        return returnedHeight2;
    }

    private void setSelectorEnabled(boolean enabled) {
        if (this.mSelector != null) {
            this.mSelector.setEnabled(enabled);
        }
    }

    private static class GateKeeperDrawable extends DrawableWrapper {
        private boolean mEnabled;

        GateKeeperDrawable(Drawable drawable) {
            super(drawable);
            this.mEnabled = true;
        }

        void setEnabled(boolean enabled) {
            this.mEnabled = enabled;
        }

        @Override
        public boolean setState(int[] stateSet) {
            if (this.mEnabled) {
                return super.setState(stateSet);
            }
            return false;
        }

        @Override
        public void draw(Canvas canvas) {
            if (this.mEnabled) {
                super.draw(canvas);
            }
        }

        @Override
        public void setHotspot(float x, float y) {
            if (this.mEnabled) {
                super.setHotspot(x, y);
            }
        }

        @Override
        public void setHotspotBounds(int left, int top, int right, int bottom) {
            if (this.mEnabled) {
                super.setHotspotBounds(left, top, right, bottom);
            }
        }

        @Override
        public boolean setVisible(boolean visible, boolean restart) {
            if (this.mEnabled) {
                return super.setVisible(visible, restart);
            }
            return false;
        }
    }

    @Override
    public boolean onHoverEvent(@NonNull MotionEvent ev) {
        if (Build.VERSION.SDK_INT < 26) {
            return super.onHoverEvent(ev);
        }
        int action = ev.getActionMasked();
        if (action == 10 && this.mResolveHoverRunnable == null) {
            this.mResolveHoverRunnable = new ResolveHoverRunnable();
            this.mResolveHoverRunnable.post();
        }
        boolean handled = super.onHoverEvent(ev);
        if (action == 9 || action == 7) {
            int position = pointToPosition((int) ev.getX(), (int) ev.getY());
            if (position != -1 && position != getSelectedItemPosition()) {
                View hoveredItem = getChildAt(position - getFirstVisiblePosition());
                if (hoveredItem.isEnabled()) {
                    setSelectionFromTop(position, hoveredItem.getTop() - getTop());
                }
                updateSelectorStateCompat();
            }
        } else {
            setSelection(-1);
        }
        return handled;
    }

    @Override
    protected void onDetachedFromWindow() {
        this.mResolveHoverRunnable = null;
        super.onDetachedFromWindow();
    }

    public boolean onForwardedEvent(MotionEvent event, int activePointerId) {
        boolean handledEvent = true;
        boolean clearPressedItem = false;
        int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case 1:
                handledEvent = false;
            case 2:
                int activeIndex = event.findPointerIndex(activePointerId);
                if (activeIndex < 0) {
                    handledEvent = false;
                } else {
                    int x = (int) event.getX(activeIndex);
                    int y = (int) event.getY(activeIndex);
                    int position = pointToPosition(x, y);
                    if (position == -1) {
                        clearPressedItem = true;
                    } else {
                        View child = getChildAt(position - getFirstVisiblePosition());
                        setPressedItem(child, position, x, y);
                        handledEvent = true;
                        if (actionMasked == 1) {
                            clickPressedItem(child, position);
                        }
                    }
                }
                break;
            case 3:
                handledEvent = false;
                break;
        }
        if (!handledEvent || clearPressedItem) {
            clearPressedItem();
        }
        if (handledEvent) {
            if (this.mScrollHelper == null) {
                this.mScrollHelper = new ListViewAutoScrollHelper(this);
            }
            this.mScrollHelper.setEnabled(true);
            this.mScrollHelper.onTouch(this, event);
        } else if (this.mScrollHelper != null) {
            this.mScrollHelper.setEnabled(false);
        }
        return handledEvent;
    }

    private void clickPressedItem(View child, int position) {
        long id = getItemIdAtPosition(position);
        performItemClick(child, position, id);
    }

    void setListSelectionHidden(boolean hideListSelection) {
        this.mListSelectionHidden = hideListSelection;
    }

    private void updateSelectorStateCompat() {
        Drawable selector = getSelector();
        if (selector != null && touchModeDrawsInPressedStateCompat() && isPressed()) {
            selector.setState(getDrawableState());
        }
    }

    private void drawSelectorCompat(Canvas canvas) {
        Drawable selector;
        if (!this.mSelectorRect.isEmpty() && (selector = getSelector()) != null) {
            selector.setBounds(this.mSelectorRect);
            selector.draw(canvas);
        }
    }

    private void positionSelectorLikeTouchCompat(int position, View sel, float x, float y) {
        positionSelectorLikeFocusCompat(position, sel);
        Drawable selector = getSelector();
        if (selector != null && position != -1) {
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    private void positionSelectorLikeFocusCompat(int position, View sel) {
        Drawable selector = getSelector();
        boolean manageState = (selector == null || position == -1) ? false : true;
        if (manageState) {
            selector.setVisible(false, false);
        }
        positionSelectorCompat(position, sel);
        if (manageState) {
            Rect bounds = this.mSelectorRect;
            float x = bounds.exactCenterX();
            float y = bounds.exactCenterY();
            selector.setVisible(getVisibility() == 0, false);
            DrawableCompat.setHotspot(selector, x, y);
        }
    }

    private void positionSelectorCompat(int position, View sel) {
        Rect selectorRect = this.mSelectorRect;
        selectorRect.set(sel.getLeft(), sel.getTop(), sel.getRight(), sel.getBottom());
        selectorRect.left -= this.mSelectionLeftPadding;
        selectorRect.top -= this.mSelectionTopPadding;
        selectorRect.right += this.mSelectionRightPadding;
        selectorRect.bottom += this.mSelectionBottomPadding;
        try {
            boolean isChildViewEnabled = this.mIsChildViewEnabled.getBoolean(this);
            if (sel.isEnabled() != isChildViewEnabled) {
                this.mIsChildViewEnabled.set(this, Boolean.valueOf(!isChildViewEnabled));
                if (position != -1) {
                    refreshDrawableState();
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void clearPressedItem() {
        this.mDrawsInPressedState = false;
        setPressed(false);
        drawableStateChanged();
        View motionView = getChildAt(this.mMotionPosition - getFirstVisiblePosition());
        if (motionView != null) {
            motionView.setPressed(false);
        }
        if (this.mClickAnimation != null) {
            this.mClickAnimation.cancel();
            this.mClickAnimation = null;
        }
    }

    private void setPressedItem(View child, int position, float x, float y) {
        View motionView;
        this.mDrawsInPressedState = true;
        if (Build.VERSION.SDK_INT >= 21) {
            drawableHotspotChanged(x, y);
        }
        if (!isPressed()) {
            setPressed(true);
        }
        layoutChildren();
        if (this.mMotionPosition != -1 && (motionView = getChildAt(this.mMotionPosition - getFirstVisiblePosition())) != null && motionView != child && motionView.isPressed()) {
            motionView.setPressed(false);
        }
        this.mMotionPosition = position;
        float childX = x - child.getLeft();
        float childY = y - child.getTop();
        if (Build.VERSION.SDK_INT >= 21) {
            child.drawableHotspotChanged(childX, childY);
        }
        if (!child.isPressed()) {
            child.setPressed(true);
        }
        positionSelectorLikeTouchCompat(position, child, x, y);
        setSelectorEnabled(false);
        refreshDrawableState();
    }

    private boolean touchModeDrawsInPressedStateCompat() {
        return this.mDrawsInPressedState;
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
