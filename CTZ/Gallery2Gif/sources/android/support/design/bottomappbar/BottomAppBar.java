package android.support.design.bottomappbar;

import android.animation.Animator;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.shape.MaterialShapeDrawable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import java.util.List;

@CoordinatorLayout.DefaultBehavior(Behavior.class)
public class BottomAppBar extends Toolbar {
    private Animator attachAnimator;
    private int fabAlignmentMode;
    private boolean fabAttached;
    private final int fabOffsetEndMode;
    private final MaterialShapeDrawable materialShapeDrawable;
    private Animator menuAnimator;
    private Animator modeAnimator;
    private final BottomAppBarTopEdgeTreatment topEdgeTreatment;

    public boolean isFabAttached() {
        return this.fabAttached;
    }

    public float getCradleVerticalOffset() {
        return this.topEdgeTreatment.getCradleVerticalOffset();
    }

    private FloatingActionButton findDependentFab() {
        List<View> dependents = ((CoordinatorLayout) getParent()).getDependents(this);
        for (View view : dependents) {
            if ((view instanceof FloatingActionButton) && view.getVisibility() == 0) {
                return view;
            }
        }
        return null;
    }

    private int getFabTranslationX(int fabAlignmentMode) {
        boolean isRtl = ViewCompat.getLayoutDirection(this) == 1;
        if (fabAlignmentMode == 1) {
            return ((getMeasuredWidth() / 2) - this.fabOffsetEndMode) * (isRtl ? -1 : 1);
        }
        return 0;
    }

    private float getFabTranslationX() {
        return getFabTranslationX(this.fabAlignmentMode);
    }

    private ActionMenuView getActionMenuView() {
        for (int i = 0; i < getChildCount(); i++) {
            ?? childAt = getChildAt(i);
            if (childAt instanceof ActionMenuView) {
                return childAt;
            }
        }
        return null;
    }

    private void translateActionMenuView(ActionMenuView actionMenuView, int fabAlignmentMode, boolean fabAttached) {
        boolean isRtl = ViewCompat.getLayoutDirection(this) == 1;
        int toolbarLeftContentEnd = 0;
        for (int toolbarLeftContentEnd2 = 0; toolbarLeftContentEnd2 < getChildCount(); toolbarLeftContentEnd2++) {
            View view = getChildAt(toolbarLeftContentEnd2);
            boolean isAlignedToStart = (view.getLayoutParams() instanceof Toolbar.LayoutParams) && (((Toolbar.LayoutParams) view.getLayoutParams()).gravity & 8388615) == 8388611;
            if (isAlignedToStart) {
                toolbarLeftContentEnd = Math.max(toolbarLeftContentEnd, isRtl ? view.getLeft() : view.getRight());
            }
        }
        int end = isRtl ? actionMenuView.getRight() : actionMenuView.getLeft();
        int offset = toolbarLeftContentEnd - end;
        actionMenuView.setTranslationX((fabAlignmentMode == 1 && fabAttached) ? offset : 0.0f);
    }

    private void cancelAnimations() {
        if (this.attachAnimator != null) {
            this.attachAnimator.cancel();
        }
        if (this.menuAnimator != null) {
            this.menuAnimator.cancel();
        }
        if (this.modeAnimator != null) {
            this.modeAnimator.cancel();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        cancelAnimations();
        this.topEdgeTreatment.setHorizontalOffset(getFabTranslationX());
        this.materialShapeDrawable.setInterpolation(this.fabAttached ? 1.0f : 0.0f);
        ActionMenuView actionMenuView = getActionMenuView();
        if (actionMenuView != null) {
            actionMenuView.setAlpha(1.0f);
            translateActionMenuView(actionMenuView, this.fabAlignmentMode, this.fabAttached);
        }
        FloatingActionButton fab = findDependentFab();
        if (fab != null) {
            fab.setTranslationY(isFabAttached() ? 0.0f : (-fab.getMeasuredHeight()) + getCradleVerticalOffset());
            fab.setTranslationX(getFabTranslationX());
        }
    }

    @Override
    public void setTitle(CharSequence title) {
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
    }

    public static class Behavior extends CoordinatorLayout.Behavior<BottomAppBar> {
        private boolean updateFabPositionAndVisibility(FloatingActionButton fab, BottomAppBar child) {
            CoordinatorLayout.LayoutParams fabLayoutParams = (CoordinatorLayout.LayoutParams) fab.getLayoutParams();
            fabLayoutParams.anchorGravity = 17;
            Rect rect = new Rect();
            fab.getBackground().getPadding(rect);
            int drawablePadding = rect.bottom;
            ((ViewGroup.MarginLayoutParams) fabLayoutParams).bottomMargin = ((int) ((child.getMeasuredHeight() / 2) + child.getCradleVerticalOffset())) - drawablePadding;
            return true;
        }

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, BottomAppBar child, int layoutDirection) {
            List<View> dependents = parent.getDependents(child);
            int count = dependents.size();
            for (int i = 0; i < count; i++) {
                View view = dependents.get(i);
                if ((view instanceof FloatingActionButton) && updateFabPositionAndVisibility(view, child)) {
                    break;
                }
            }
            parent.onLayoutChild(child, layoutDirection);
            return true;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.fabAlignmentMode = this.fabAlignmentMode;
        savedState.fabAttached = this.fabAttached;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        if (!(parcelable instanceof SavedState)) {
            super.onRestoreInstanceState(parcelable);
            return;
        }
        super.onRestoreInstanceState(parcelable.getSuperState());
        this.fabAlignmentMode = parcelable.fabAlignmentMode;
        this.fabAttached = parcelable.fabAttached;
    }

    static class SavedState extends AbsSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int fabAlignmentMode;
        boolean fabAttached;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            this.fabAlignmentMode = in.readInt();
            this.fabAttached = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.fabAlignmentMode);
            parcel.writeInt(this.fabAttached ? 1 : 0);
        }
    }
}
