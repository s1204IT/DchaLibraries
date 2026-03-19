package com.android.internal.view.menu;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.android.internal.R;
import com.android.internal.view.menu.MenuBuilder;
import java.util.ArrayList;

public final class IconMenuView extends ViewGroup implements MenuBuilder.ItemInvoker, MenuView, Runnable {
    private static final int ITEM_CAPTION_CYCLE_DELAY = 1000;
    private int mAnimations;
    private boolean mHasStaleChildren;
    private Drawable mHorizontalDivider;
    private int mHorizontalDividerHeight;
    private ArrayList<Rect> mHorizontalDividerRects;
    private Drawable mItemBackground;
    private boolean mLastChildrenCaptionMode;
    private int[] mLayout;
    private int mLayoutNumRows;
    private int mMaxItems;
    private int mMaxItemsPerRow;
    private int mMaxRows;
    private MenuBuilder mMenu;
    private boolean mMenuBeingLongpressed;
    private Drawable mMoreIcon;
    private int mNumActualItemsShown;
    private int mRowHeight;
    private Drawable mVerticalDivider;
    private ArrayList<Rect> mVerticalDividerRects;
    private int mVerticalDividerWidth;

    public IconMenuView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mMenuBeingLongpressed = false;
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.IconMenuView, 0, 0);
        this.mRowHeight = typedArrayObtainStyledAttributes.getDimensionPixelSize(0, 64);
        this.mMaxRows = typedArrayObtainStyledAttributes.getInt(1, 2);
        this.mMaxItems = typedArrayObtainStyledAttributes.getInt(4, 6);
        this.mMaxItemsPerRow = typedArrayObtainStyledAttributes.getInt(2, 3);
        this.mMoreIcon = typedArrayObtainStyledAttributes.getDrawable(3);
        typedArrayObtainStyledAttributes.recycle();
        TypedArray typedArrayObtainStyledAttributes2 = context.obtainStyledAttributes(attributeSet, R.styleable.MenuView, 0, 0);
        this.mItemBackground = typedArrayObtainStyledAttributes2.getDrawable(5);
        this.mHorizontalDivider = typedArrayObtainStyledAttributes2.getDrawable(2);
        this.mHorizontalDividerRects = new ArrayList<>();
        this.mVerticalDivider = typedArrayObtainStyledAttributes2.getDrawable(3);
        this.mVerticalDividerRects = new ArrayList<>();
        this.mAnimations = typedArrayObtainStyledAttributes2.getResourceId(0, 0);
        typedArrayObtainStyledAttributes2.recycle();
        if (this.mHorizontalDivider != null) {
            this.mHorizontalDividerHeight = this.mHorizontalDivider.getIntrinsicHeight();
            if (this.mHorizontalDividerHeight == -1) {
                this.mHorizontalDividerHeight = 1;
            }
        }
        if (this.mVerticalDivider != null) {
            this.mVerticalDividerWidth = this.mVerticalDivider.getIntrinsicWidth();
            if (this.mVerticalDividerWidth == -1) {
                this.mVerticalDividerWidth = 1;
            }
        }
        this.mLayout = new int[this.mMaxRows];
        setWillNotDraw(false);
        setFocusableInTouchMode(true);
        setDescendantFocusability(262144);
    }

    int getMaxItems() {
        return this.mMaxItems;
    }

    private void layoutItems(int i) {
        int childCount = getChildCount();
        if (childCount == 0) {
            this.mLayoutNumRows = 0;
            return;
        }
        for (int iMin = Math.min((int) Math.ceil(childCount / this.mMaxItemsPerRow), this.mMaxRows); iMin <= this.mMaxRows; iMin++) {
            layoutItemsUsingGravity(iMin, childCount);
            if (iMin >= childCount || doItemsFit()) {
                return;
            }
        }
    }

    private void layoutItemsUsingGravity(int i, int i2) {
        int i3 = i2 / i;
        int i4 = i - (i2 % i);
        int[] iArr = this.mLayout;
        for (int i5 = 0; i5 < i; i5++) {
            iArr[i5] = i3;
            if (i5 >= i4) {
                iArr[i5] = iArr[i5] + 1;
            }
        }
        this.mLayoutNumRows = i;
    }

    private boolean doItemsFit() {
        int[] iArr = this.mLayout;
        int i = this.mLayoutNumRows;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            int i4 = iArr[i3];
            if (i4 == 1) {
                i2++;
            } else {
                int i5 = i2;
                int i6 = i4;
                while (i6 > 0) {
                    int i7 = i5 + 1;
                    if (((LayoutParams) getChildAt(i5).getLayoutParams()).maxNumItemsOnRow < i4) {
                        return false;
                    }
                    i6--;
                    i5 = i7;
                }
                i2 = i5;
            }
        }
        return true;
    }

    Drawable getItemBackgroundDrawable() {
        return this.mItemBackground.getConstantState().newDrawable(getContext().getResources());
    }

    IconMenuItemView createMoreItemView() {
        Context context = getContext();
        IconMenuItemView iconMenuItemView = (IconMenuItemView) LayoutInflater.from(context).inflate(R.layout.icon_menu_item_layout, (ViewGroup) null);
        iconMenuItemView.initialize(context.getResources().getText(R.string.more_item_label), this.mMoreIcon);
        iconMenuItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IconMenuView.this.mMenu.changeMenuMode();
            }
        });
        return iconMenuItemView;
    }

    @Override
    public void initialize(MenuBuilder menuBuilder) {
        this.mMenu = menuBuilder;
    }

    private void positionChildren(int i, int i2) {
        int[] iArr;
        int i3;
        if (this.mHorizontalDivider != null) {
            this.mHorizontalDividerRects.clear();
        }
        if (this.mVerticalDivider != null) {
            this.mVerticalDividerRects.clear();
        }
        int i4 = this.mLayoutNumRows;
        int i5 = i4 - 1;
        int[] iArr2 = this.mLayout;
        float f = (i2 - (this.mHorizontalDividerHeight * i5)) / i4;
        LayoutParams layoutParams = null;
        int i6 = 0;
        int i7 = 0;
        float f2 = 0.0f;
        while (i6 < i4) {
            float f3 = (i - (this.mVerticalDividerWidth * (iArr2[i6] - 1))) / iArr2[i6];
            LayoutParams layoutParams2 = layoutParams;
            float f4 = 0.0f;
            int i8 = i7;
            int i9 = 0;
            while (i9 < iArr2[i6]) {
                View childAt = getChildAt(i8);
                childAt.measure(View.MeasureSpec.makeMeasureSpec((int) f3, 1073741824), View.MeasureSpec.makeMeasureSpec((int) f, 1073741824));
                layoutParams2 = (LayoutParams) childAt.getLayoutParams();
                layoutParams2.left = (int) f4;
                float f5 = f4 + f3;
                int i10 = (int) f5;
                layoutParams2.right = i10;
                int i11 = (int) f2;
                layoutParams2.top = i11;
                int i12 = (int) (f2 + f);
                layoutParams2.bottom = i12;
                int i13 = i8 + 1;
                int i14 = i4;
                if (this.mVerticalDivider != null) {
                    iArr = iArr2;
                    i3 = i13;
                    this.mVerticalDividerRects.add(new Rect(i10, i11, (int) (this.mVerticalDividerWidth + f5), i12));
                } else {
                    iArr = iArr2;
                    i3 = i13;
                }
                f4 = f5 + this.mVerticalDividerWidth;
                i9++;
                i4 = i14;
                iArr2 = iArr;
                i8 = i3;
            }
            int i15 = i4;
            int[] iArr3 = iArr2;
            if (layoutParams2 != null) {
                layoutParams2.right = i;
            }
            f2 += f;
            if (this.mHorizontalDivider != null && i6 < i5) {
                this.mHorizontalDividerRects.add(new Rect(0, (int) f2, i, (int) (this.mHorizontalDividerHeight + f2)));
                f2 += this.mHorizontalDividerHeight;
            }
            i6++;
            i7 = i8;
            layoutParams = layoutParams2;
            i4 = i15;
            iArr2 = iArr3;
        }
    }

    @Override
    protected void onMeasure(int i, int i2) {
        int iResolveSize = resolveSize(Integer.MAX_VALUE, i);
        calculateItemFittingMetadata(iResolveSize);
        layoutItems(iResolveSize);
        int i3 = this.mLayoutNumRows;
        setMeasuredDimension(iResolveSize, resolveSize(((this.mRowHeight + this.mHorizontalDividerHeight) * i3) - this.mHorizontalDividerHeight, i2));
        if (i3 > 0) {
            positionChildren(getMeasuredWidth(), getMeasuredHeight());
        }
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            View childAt = getChildAt(childCount);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            childAt.layout(layoutParams.left, layoutParams.top, layoutParams.right, layoutParams.bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = this.mHorizontalDivider;
        if (drawable != null) {
            ArrayList<Rect> arrayList = this.mHorizontalDividerRects;
            for (int size = arrayList.size() - 1; size >= 0; size--) {
                drawable.setBounds(arrayList.get(size));
                drawable.draw(canvas);
            }
        }
        Drawable drawable2 = this.mVerticalDivider;
        if (drawable2 != null) {
            ArrayList<Rect> arrayList2 = this.mVerticalDividerRects;
            for (int size2 = arrayList2.size() - 1; size2 >= 0; size2--) {
                drawable2.setBounds(arrayList2.get(size2));
                drawable2.draw(canvas);
            }
        }
    }

    @Override
    public boolean invokeItem(MenuItemImpl menuItemImpl) {
        return this.mMenu.performItemAction(menuItemImpl, 0);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    void markStaleChildren() {
        if (!this.mHasStaleChildren) {
            this.mHasStaleChildren = true;
            requestLayout();
        }
    }

    int getNumActualItemsShown() {
        return this.mNumActualItemsShown;
    }

    void setNumActualItemsShown(int i) {
        this.mNumActualItemsShown = i;
    }

    @Override
    public int getWindowAnimations() {
        return this.mAnimations;
    }

    public int[] getLayout() {
        return this.mLayout;
    }

    public int getLayoutNumRows() {
        return this.mLayoutNumRows;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == 82) {
            if (keyEvent.getAction() == 0 && keyEvent.getRepeatCount() == 0) {
                removeCallbacks(this);
                postDelayed(this, ViewConfiguration.getLongPressTimeout());
            } else if (keyEvent.getAction() == 1) {
                if (this.mMenuBeingLongpressed) {
                    setCycleShortcutCaptionMode(false);
                    return true;
                }
                removeCallbacks(this);
            }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestFocus();
    }

    @Override
    protected void onDetachedFromWindow() {
        setCycleShortcutCaptionMode(false);
        super.onDetachedFromWindow();
    }

    @Override
    public void onWindowFocusChanged(boolean z) {
        if (!z) {
            setCycleShortcutCaptionMode(false);
        }
        super.onWindowFocusChanged(z);
    }

    private void setCycleShortcutCaptionMode(boolean z) {
        if (!z) {
            removeCallbacks(this);
            setChildrenCaptionMode(false);
            this.mMenuBeingLongpressed = false;
            return;
        }
        setChildrenCaptionMode(true);
    }

    @Override
    public void run() {
        if (this.mMenuBeingLongpressed) {
            setChildrenCaptionMode(!this.mLastChildrenCaptionMode);
        } else {
            this.mMenuBeingLongpressed = true;
            setCycleShortcutCaptionMode(true);
        }
        postDelayed(this, 1000L);
    }

    private void setChildrenCaptionMode(boolean z) {
        this.mLastChildrenCaptionMode = z;
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            ((IconMenuItemView) getChildAt(childCount)).setCaptionMode(z);
        }
    }

    private void calculateItemFittingMetadata(int i) {
        int i2 = this.mMaxItemsPerRow;
        int childCount = getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            LayoutParams layoutParams = (LayoutParams) getChildAt(i3).getLayoutParams();
            layoutParams.maxNumItemsOnRow = 1;
            int i4 = i2;
            while (true) {
                if (i4 <= 0) {
                    break;
                }
                if (layoutParams.desiredWidth < i / i4) {
                    layoutParams.maxNumItemsOnRow = i4;
                    break;
                }
                i4--;
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelableOnSaveInstanceState = super.onSaveInstanceState();
        View focusedChild = getFocusedChild();
        for (int childCount = getChildCount() - 1; childCount >= 0; childCount--) {
            if (getChildAt(childCount) == focusedChild) {
                return new SavedState(parcelableOnSaveInstanceState, childCount);
            }
        }
        return new SavedState(parcelableOnSaveInstanceState, -1);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        View childAt;
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.focusedPosition < getChildCount() && (childAt = getChildAt(savedState.focusedPosition)) != null) {
            childAt.requestFocus();
        }
    }

    private static class SavedState extends View.BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        int focusedPosition;

        public SavedState(Parcelable parcelable, int i) {
            super(parcelable);
            this.focusedPosition = i;
        }

        private SavedState(Parcel parcel) {
            super(parcel);
            this.focusedPosition = parcel.readInt();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            super.writeToParcel(parcel, i);
            parcel.writeInt(this.focusedPosition);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        int bottom;
        int desiredWidth;
        int left;
        int maxNumItemsOnRow;
        int right;
        int top;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
        }
    }
}
