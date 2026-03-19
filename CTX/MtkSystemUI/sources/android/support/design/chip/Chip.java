package android.support.design.chip;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.chip.ChipDrawable;
import android.support.design.internal.ViewUtils;
import android.support.design.ripple.RippleUtils;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.ExploreByTouchHelper;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.CompoundButton;
import java.util.List;

public class Chip extends AppCompatCheckBox implements ChipDrawable.Delegate {
    private static final int[] SELECTED_STATE = {android.R.attr.state_selected};
    private ChipDrawable chipDrawable;
    private boolean closeIconFocused;
    private boolean closeIconHovered;
    private boolean closeIconPressed;
    private boolean deferredCheckedValue;
    private int focusedVirtualView;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListenerInternal;
    private View.OnClickListener onCloseIconClickListener;
    private final Rect rect;
    private final RectF rectF;
    private final ChipTouchHelper touchHelper;

    public Chip(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.chipStyle);
    }

    public Chip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.focusedVirtualView = Integer.MIN_VALUE;
        this.rect = new Rect();
        this.rectF = new RectF();
        ChipDrawable drawable = ChipDrawable.createFromAttributes(context, attrs, defStyleAttr, R.style.Widget_MaterialComponents_Chip_Action);
        setChipDrawable(drawable);
        this.touchHelper = new ChipTouchHelper(this);
        ViewCompat.setAccessibilityDelegate(this, this.touchHelper);
        ViewCompat.setImportantForAccessibility(this, 1);
        initOutlineProvider();
        setChecked(this.deferredCheckedValue);
    }

    private void initOutlineProvider() {
        if (Build.VERSION.SDK_INT >= 21) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                @TargetApi(21)
                public void getOutline(View view, Outline outline) {
                    if (Chip.this.chipDrawable != null) {
                        Chip.this.chipDrawable.getOutline(outline);
                    } else {
                        outline.setAlpha(0.0f);
                    }
                }
            });
        }
    }

    public void setChipDrawable(ChipDrawable drawable) {
        if (this.chipDrawable != drawable) {
            unapplyChipDrawable(this.chipDrawable);
            this.chipDrawable = drawable;
            applyChipDrawable(this.chipDrawable);
            if (RippleUtils.USE_FRAMEWORK_RIPPLE) {
                RippleDrawable ripple = new RippleDrawable(RippleUtils.convertToRippleDrawableColor(this.chipDrawable.getRippleColor()), this.chipDrawable, null);
                this.chipDrawable.setUseCompatRipple(false);
                ViewCompat.setBackground(this, ripple);
            } else {
                this.chipDrawable.setUseCompatRipple(true);
                ViewCompat.setBackground(this, this.chipDrawable);
            }
        }
    }

    private void unapplyChipDrawable(ChipDrawable chipDrawable) {
        if (chipDrawable != null) {
            chipDrawable.setDelegate(null);
        }
    }

    private void applyChipDrawable(ChipDrawable chipDrawable) {
        chipDrawable.setDelegate(this);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(state, SELECTED_STATE);
        }
        return state;
    }

    @Override
    public void onChipDrawableSizeChange() {
        requestLayout();
        if (Build.VERSION.SDK_INT >= 21) {
            invalidateOutline();
        }
    }

    @Override
    public void setChecked(boolean checked) {
        if (this.chipDrawable == null) {
            this.deferredCheckedValue = checked;
            return;
        }
        if (this.chipDrawable.isCheckable()) {
            boolean wasChecked = isChecked();
            super.setChecked(checked);
            if (wasChecked != checked && this.onCheckedChangeListenerInternal != null) {
                this.onCheckedChangeListenerInternal.onCheckedChanged(this, checked);
            }
        }
    }

    void setOnCheckedChangeListenerInternal(CompoundButton.OnCheckedChangeListener listener) {
        this.onCheckedChangeListenerInternal = listener;
    }

    public boolean performCloseIconClick() {
        boolean result;
        playSoundEffect(0);
        if (this.onCloseIconClickListener != null) {
            this.onCloseIconClickListener.onClick(this);
            result = true;
        } else {
            result = false;
        }
        this.touchHelper.sendEventForVirtualView(0, 1);
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = false;
        int action = event.getActionMasked();
        boolean eventInCloseIcon = getCloseIconTouchBounds().contains(event.getX(), event.getY());
        switch (action) {
            case 0:
                if (eventInCloseIcon) {
                    setCloseIconPressed(true);
                    handled = true;
                }
                break;
            case 1:
                if (this.closeIconPressed) {
                    performCloseIconClick();
                    handled = true;
                }
                setCloseIconPressed(false);
                break;
            case 2:
                if (this.closeIconPressed) {
                    if (!eventInCloseIcon) {
                        setCloseIconPressed(false);
                    }
                    handled = true;
                }
                break;
            case 3:
                setCloseIconPressed(false);
                break;
        }
        return handled || super.onTouchEvent(event);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == 7) {
            setCloseIconHovered(getCloseIconTouchBounds().contains(event.getX(), event.getY()));
        } else if (action == 10) {
            setCloseIconHovered(false);
        }
        return super.onHoverEvent(event);
    }

    @Override
    protected boolean dispatchHoverEvent(MotionEvent event) {
        return this.touchHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (focused) {
            setFocusedVirtualView(-1);
        } else {
            setFocusedVirtualView(Integer.MIN_VALUE);
        }
        invalidate();
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean focusChanged = false;
        int keyCode2 = event.getKeyCode();
        if (keyCode2 == 61) {
            int focusChangeDirection = 0;
            if (event.hasNoModifiers()) {
                focusChangeDirection = 2;
            } else if (event.hasModifiers(1)) {
                focusChangeDirection = 1;
            }
            if (focusChangeDirection != 0) {
                ViewParent parent = getParent();
                View nextFocus = this;
                do {
                    nextFocus = nextFocus.focusSearch(focusChangeDirection);
                    if (nextFocus == null || nextFocus == this) {
                        break;
                    }
                } while (nextFocus.getParent() == parent);
                if (nextFocus != null) {
                    nextFocus.requestFocus();
                    return true;
                }
            }
        } else if (keyCode2 != 66) {
            switch (keyCode2) {
                case 21:
                    if (event.hasNoModifiers()) {
                        focusChanged = moveFocus(ViewUtils.isLayoutRtl(this));
                    }
                    break;
                case 22:
                    if (event.hasNoModifiers()) {
                        focusChanged = moveFocus(!ViewUtils.isLayoutRtl(this));
                    }
                    break;
                case 23:
                    switch (this.focusedVirtualView) {
                        case -1:
                            performClick();
                            break;
                        case 0:
                            performCloseIconClick();
                            break;
                    }
                    return true;
            }
        }
        if (focusChanged) {
            invalidate();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean moveFocus(boolean positive) {
        ensureFocus();
        if (positive) {
            if (this.focusedVirtualView != -1) {
                return false;
            }
            setFocusedVirtualView(0);
            return true;
        }
        if (this.focusedVirtualView != 0) {
            return false;
        }
        setFocusedVirtualView(-1);
        return true;
    }

    private void ensureFocus() {
        if (this.focusedVirtualView == Integer.MIN_VALUE) {
            setFocusedVirtualView(-1);
        }
    }

    @Override
    public void getFocusedRect(Rect r) {
        if (this.focusedVirtualView == 0) {
            r.set(getCloseIconTouchBoundsInt());
        } else {
            super.getFocusedRect(r);
        }
    }

    private void setFocusedVirtualView(int virtualView) {
        if (this.focusedVirtualView != virtualView) {
            if (this.focusedVirtualView == 0) {
                setCloseIconFocused(false);
            }
            this.focusedVirtualView = virtualView;
            if (virtualView == 0) {
                setCloseIconFocused(true);
            }
        }
    }

    private void setCloseIconPressed(boolean pressed) {
        if (this.closeIconPressed != pressed) {
            this.closeIconPressed = pressed;
            refreshDrawableState();
        }
    }

    private void setCloseIconHovered(boolean hovered) {
        if (this.closeIconHovered != hovered) {
            this.closeIconHovered = hovered;
            refreshDrawableState();
        }
    }

    private void setCloseIconFocused(boolean focused) {
        if (this.closeIconFocused != focused) {
            this.closeIconFocused = focused;
            refreshDrawableState();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        boolean changed = false;
        if (this.chipDrawable != null && this.chipDrawable.isCloseIconStateful()) {
            changed = this.chipDrawable.setCloseIconState(createCloseIconDrawableState());
        }
        if (changed) {
            invalidate();
        }
    }

    private int[] createCloseIconDrawableState() {
        int count = 0;
        if (isEnabled()) {
            count = 0 + 1;
        }
        if (this.closeIconFocused) {
            count++;
        }
        if (this.closeIconHovered) {
            count++;
        }
        if (this.closeIconPressed) {
            count++;
        }
        if (isChecked()) {
            count++;
        }
        int[] stateSet = new int[count];
        int i = 0;
        if (isEnabled()) {
            stateSet[0] = 16842910;
            i = 0 + 1;
        }
        if (this.closeIconFocused) {
            stateSet[i] = 16842908;
            i++;
        }
        if (this.closeIconHovered) {
            stateSet[i] = 16843623;
            i++;
        }
        if (this.closeIconPressed) {
            stateSet[i] = 16842919;
            i++;
        }
        if (isChecked()) {
            stateSet[i] = 16842913;
            int i2 = i + 1;
        }
        return stateSet;
    }

    private boolean hasCloseIcon() {
        return (this.chipDrawable == null || this.chipDrawable.getCloseIcon() == null) ? false : true;
    }

    private RectF getCloseIconTouchBounds() {
        this.rectF.setEmpty();
        if (hasCloseIcon()) {
            this.chipDrawable.getCloseIconTouchBounds(this.rectF);
        }
        return this.rectF;
    }

    private Rect getCloseIconTouchBoundsInt() {
        RectF bounds = getCloseIconTouchBounds();
        this.rect.set((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
        return this.rect;
    }

    @Override
    @TargetApi(24)
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        if (getCloseIconTouchBounds().contains(event.getX(), event.getY()) && isEnabled()) {
            return PointerIcon.getSystemIcon(getContext(), 1002);
        }
        return null;
    }

    private class ChipTouchHelper extends ExploreByTouchHelper {
        ChipTouchHelper(Chip view) {
            super(view);
        }

        @Override
        protected int getVirtualViewAt(float x, float y) {
            return (Chip.this.hasCloseIcon() && Chip.this.getCloseIconTouchBounds().contains(x, y)) ? 0 : -1;
        }

        @Override
        protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
            virtualViewIds.add(-1);
            if (Chip.this.hasCloseIcon()) {
                virtualViewIds.add(0);
            }
        }

        @Override
        protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
            if (Chip.this.hasCloseIcon()) {
                node.setContentDescription(Chip.this.getContext().getString(R.string.mtrl_chip_close_icon_content_description));
                node.setBoundsInParent(Chip.this.getCloseIconTouchBoundsInt());
                node.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                node.setEnabled(Chip.this.isEnabled());
                return;
            }
            node.setContentDescription("");
        }

        @Override
        protected void onPopulateNodeForHost(AccessibilityNodeInfoCompat node) {
            node.setCheckable(Chip.this.chipDrawable != null && Chip.this.chipDrawable.isCheckable());
            node.setClassName(Chip.class.getName());
            StringBuilder sb = new StringBuilder();
            sb.append(Chip.class.getSimpleName());
            sb.append(". ");
            sb.append((Object) (Chip.this.chipDrawable != null ? Chip.this.chipDrawable.getChipText() : ""));
            node.setContentDescription(sb.toString());
        }

        @Override
        protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
            if (action == 16 && virtualViewId == 0) {
                return Chip.this.performCloseIconClick();
            }
            return false;
        }
    }
}
