package android.support.design.bottomappbar;

import android.animation.Animator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.internal.ThemeEnforcement;
import android.support.design.resources.MaterialResources;
import android.support.design.shape.MaterialShapeDrawable;
import android.support.design.shape.ShapePathModel;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
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

    public BottomAppBar(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.bottomAppBarStyle);
    }

    public BottomAppBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = ThemeEnforcement.obtainStyledAttributes(context, attrs, R.styleable.BottomAppBar, defStyleAttr, R.style.Widget_MaterialComponents_BottomAppBar);
        ColorStateList backgroundTint = MaterialResources.getColorStateList(context, a, R.styleable.BottomAppBar_backgroundTint);
        float fabCradleDiameter = a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleDiameter, 0);
        float fabCornerRadius = a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleRoundedCornerRadius, 0);
        float fabVerticalOffset = a.getDimensionPixelOffset(R.styleable.BottomAppBar_fabCradleVerticalOffset, 0);
        this.fabAttached = a.getBoolean(R.styleable.BottomAppBar_fabAttached, true);
        this.fabAlignmentMode = a.getInt(R.styleable.BottomAppBar_fabAlignmentMode, 0);
        a.recycle();
        this.fabOffsetEndMode = getResources().getDimensionPixelOffset(R.dimen.mtrl_bottomappbar_fabOffsetEndMode);
        this.topEdgeTreatment = new BottomAppBarTopEdgeTreatment(fabCradleDiameter, fabCornerRadius, fabVerticalOffset);
        ShapePathModel appBarModel = new ShapePathModel();
        appBarModel.setTopEdge(this.topEdgeTreatment);
        this.materialShapeDrawable = new MaterialShapeDrawable(appBarModel);
        this.materialShapeDrawable.setStrokeWidth(1.0f);
        this.materialShapeDrawable.setShadowEnabled(true);
        this.materialShapeDrawable.setPaintStyle(Paint.Style.FILL);
        DrawableCompat.setTintList(this.materialShapeDrawable, backgroundTint);
        ViewCompat.setBackground(this, this.materialShapeDrawable);
    }

    public boolean isFabAttached() {
        return this.fabAttached;
    }

    public float getCradleVerticalOffset() {
        return this.topEdgeTreatment.getCradleVerticalOffset();
    }

    private FloatingActionButton findDependentFab() {
        List<View> dependents = ((CoordinatorLayout) getParent()).getDependents(this);
        for (View v : dependents) {
            if ((v instanceof FloatingActionButton) && v.getVisibility() == 0) {
                return (FloatingActionButton) v;
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
            View view = getChildAt(i);
            if (view instanceof ActionMenuView) {
                return (ActionMenuView) view;
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
            fabLayoutParams.bottomMargin = ((int) ((child.getMeasuredHeight() / 2) + child.getCradleVerticalOffset())) - drawablePadding;
            return true;
        }

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, BottomAppBar child, int layoutDirection) {
            List<View> dependents = parent.getDependents(child);
            int count = dependents.size();
            for (int i = 0; i < count; i++) {
                View dependent = dependents.get(i);
                if ((dependent instanceof FloatingActionButton) && updateFabPositionAndVisibility((FloatingActionButton) dependent, child)) {
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
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        this.fabAlignmentMode = savedState.fabAlignmentMode;
        this.fabAttached = savedState.fabAttached;
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
