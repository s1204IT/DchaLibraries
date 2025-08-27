package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.v4.view.ViewCompat;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import com.android.launcher3.DropTarget;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.accessibility.DragAndDropAccessibilityDelegate;
import com.android.launcher3.accessibility.FolderAccessibilityHelper;
import com.android.launcher3.accessibility.WorkspaceAccessibilityHelper;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertyListBuilder;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.graphics.DragPreviewProvider;
import com.android.launcher3.util.CellAndSpan;
import com.android.launcher3.util.GridOccupancy;
import com.android.launcher3.util.ParcelableSparseArray;
import com.android.launcher3.util.Themes;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;

/* loaded from: classes.dex */
public class CellLayout extends ViewGroup {
    private static final boolean DEBUG_VISUALIZE_OCCUPIED = false;
    private static final boolean DESTRUCTIVE_REORDER = false;
    public static final int FOLDER = 2;
    public static final int FOLDER_ACCESSIBILITY_DRAG = 1;
    public static final int HOTSEAT = 1;
    private static final int INVALID_DIRECTION = -100;
    private static final boolean LOGD = false;
    public static final int MODE_ACCEPT_DROP = 4;
    public static final int MODE_DRAG_OVER = 1;
    public static final int MODE_ON_DROP = 2;
    public static final int MODE_ON_DROP_EXTERNAL = 3;
    public static final int MODE_SHOW_REORDER_HINT = 0;
    private static final int REORDER_ANIMATION_DURATION = 150;
    private static final float REORDER_PREVIEW_MAGNITUDE = 0.12f;
    private static final String TAG = "CellLayout";
    public static final int WORKSPACE = 0;
    public static final int WORKSPACE_ACCESSIBILITY_DRAG = 2;
    private final Drawable mBackground;

    @ViewDebug.ExportedProperty(category = "launcher")
    int mCellHeight;

    @ViewDebug.ExportedProperty(category = "launcher")
    int mCellWidth;
    private final float mChildScale;
    private final int mContainerType;

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mCountX;

    @ViewDebug.ExportedProperty(category = "launcher")
    private int mCountY;
    private final int[] mDirectionVector;
    private final int[] mDragCell;
    final float[] mDragOutlineAlphas;
    private final InterruptibleInOutAnimator[] mDragOutlineAnims;
    private int mDragOutlineCurrent;
    private final Paint mDragOutlinePaint;
    final Rect[] mDragOutlines;
    private boolean mDragging;
    private boolean mDropPending;
    private final TimeInterpolator mEaseOutInterpolator;
    private int mFixedCellHeight;
    private int mFixedCellWidth;
    private int mFixedHeight;
    private int mFixedWidth;
    private final ArrayList<PreviewBackground> mFolderBackgrounds;
    final PreviewBackground mFolderLeaveBehind;
    private View.OnTouchListener mInterceptTouchListener;
    private final ArrayList<View> mIntersectingViews;
    private boolean mIsDragOverlapping;
    private boolean mItemPlacementDirty;
    private final Launcher mLauncher;
    private GridOccupancy mOccupied;
    private final Rect mOccupiedRect;
    final int[] mPreviousReorderDirection;
    final ArrayMap<LayoutParams, Animator> mReorderAnimators;
    final float mReorderPreviewAnimationMagnitude;
    final ArrayMap<View, ReorderPreviewAnimation> mShakeAnimators;
    private final ShortcutAndWidgetContainer mShortcutsAndWidgets;
    private final StylusEventHelper mStylusEventHelper;
    final int[] mTempLocation;
    private final Rect mTempRect;
    private final Stack<Rect> mTempRectStack;
    private GridOccupancy mTmpOccupied;
    final int[] mTmpPoint;
    private DragAndDropAccessibilityDelegate mTouchHelper;
    private boolean mUseTouchHelper;
    private static final int[] BACKGROUND_STATE_ACTIVE = {android.R.attr.state_active};
    private static final int[] BACKGROUND_STATE_DEFAULT = EMPTY_STATE_SET;
    private static final Paint sPaint = new Paint();

    @Retention(RetentionPolicy.SOURCE)
    public @interface ContainerType {
    }

    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CellLayout(Context context, AttributeSet attributeSet, int i) throws Resources.NotFoundException {
        super(context, attributeSet, i);
        this.mDropPending = false;
        this.mTmpPoint = new int[2];
        this.mTempLocation = new int[2];
        this.mFolderBackgrounds = new ArrayList<>();
        this.mFolderLeaveBehind = new PreviewBackground();
        this.mFixedWidth = -1;
        this.mFixedHeight = -1;
        this.mIsDragOverlapping = false;
        this.mDragOutlines = new Rect[4];
        this.mDragOutlineAlphas = new float[this.mDragOutlines.length];
        this.mDragOutlineAnims = new InterruptibleInOutAnimator[this.mDragOutlines.length];
        this.mDragOutlineCurrent = 0;
        this.mDragOutlinePaint = new Paint();
        this.mReorderAnimators = new ArrayMap<>();
        this.mShakeAnimators = new ArrayMap<>();
        this.mItemPlacementDirty = false;
        this.mDragCell = new int[2];
        this.mDragging = false;
        this.mChildScale = 1.0f;
        this.mIntersectingViews = new ArrayList<>();
        this.mOccupiedRect = new Rect();
        this.mDirectionVector = new int[2];
        this.mPreviousReorderDirection = new int[2];
        this.mTempRect = new Rect();
        this.mUseTouchHelper = false;
        this.mTempRectStack = new Stack<>();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CellLayout, i, 0);
        this.mContainerType = typedArrayObtainStyledAttributes.getInteger(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        setWillNotDraw(false);
        setClipToPadding(false);
        this.mLauncher = Launcher.getLauncher(context);
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        this.mCellHeight = -1;
        this.mCellWidth = -1;
        this.mFixedCellHeight = -1;
        this.mFixedCellWidth = -1;
        this.mCountX = deviceProfile.inv.numColumns;
        this.mCountY = deviceProfile.inv.numRows;
        this.mOccupied = new GridOccupancy(this.mCountX, this.mCountY);
        this.mTmpOccupied = new GridOccupancy(this.mCountX, this.mCountY);
        this.mPreviousReorderDirection[0] = -100;
        this.mPreviousReorderDirection[1] = -100;
        this.mFolderLeaveBehind.delegateCellX = -1;
        this.mFolderLeaveBehind.delegateCellY = -1;
        setAlwaysDrawnWithCacheEnabled(false);
        Resources resources = getResources();
        this.mBackground = resources.getDrawable(R.drawable.bg_celllayout);
        this.mBackground.setCallback(this);
        this.mBackground.setAlpha(0);
        this.mReorderPreviewAnimationMagnitude = REORDER_PREVIEW_MAGNITUDE * deviceProfile.iconSizePx;
        this.mEaseOutInterpolator = Interpolators.DEACCEL_2_5;
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
        for (int i2 = 0; i2 < this.mDragOutlines.length; i2++) {
            this.mDragOutlines[i2] = new Rect(-1, -1, -1, -1);
        }
        this.mDragOutlinePaint.setColor(Themes.getAttrColor(context, R.attr.workspaceTextColor));
        int integer = resources.getInteger(R.integer.config_dragOutlineFadeTime);
        float integer2 = resources.getInteger(R.integer.config_dragOutlineMaxAlpha);
        Arrays.fill(this.mDragOutlineAlphas, 0.0f);
        for (int i3 = 0; i3 < this.mDragOutlineAnims.length; i3++) {
            InterruptibleInOutAnimator interruptibleInOutAnimator = new InterruptibleInOutAnimator(this, integer, 0.0f, integer2);
            interruptibleInOutAnimator.getAnimator().setInterpolator(this.mEaseOutInterpolator);
            interruptibleInOutAnimator.getAnimator().addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.CellLayout.1
                final /* synthetic */ InterruptibleInOutAnimator val$anim;
                final /* synthetic */ int val$thisIndex;

                AnonymousClass1(InterruptibleInOutAnimator interruptibleInOutAnimator2, int i32) {
                    interruptibleInOutAnimator = interruptibleInOutAnimator2;
                    i = i32;
                }

                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (((Bitmap) interruptibleInOutAnimator.getTag()) == null) {
                        valueAnimator.cancel();
                    } else {
                        CellLayout.this.mDragOutlineAlphas[i] = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                        CellLayout.this.invalidate(CellLayout.this.mDragOutlines[i]);
                    }
                }
            });
            interruptibleInOutAnimator2.getAnimator().addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.CellLayout.2
                final /* synthetic */ InterruptibleInOutAnimator val$anim;

                AnonymousClass2(InterruptibleInOutAnimator interruptibleInOutAnimator2) {
                    interruptibleInOutAnimator = interruptibleInOutAnimator2;
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    if (((Float) ((ValueAnimator) animator).getAnimatedValue()).floatValue() == 0.0f) {
                        interruptibleInOutAnimator.setTag(null);
                    }
                }
            });
            this.mDragOutlineAnims[i32] = interruptibleInOutAnimator2;
        }
        this.mShortcutsAndWidgets = new ShortcutAndWidgetContainer(context, this.mContainerType);
        this.mShortcutsAndWidgets.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mCountX, this.mCountY);
        this.mStylusEventHelper = new StylusEventHelper(new SimpleOnStylusPressListener(this), this);
        addView(this.mShortcutsAndWidgets);
    }

    /* renamed from: com.android.launcher3.CellLayout$1 */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ InterruptibleInOutAnimator val$anim;
        final /* synthetic */ int val$thisIndex;

        AnonymousClass1(InterruptibleInOutAnimator interruptibleInOutAnimator2, int i32) {
            interruptibleInOutAnimator = interruptibleInOutAnimator2;
            i = i32;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (((Bitmap) interruptibleInOutAnimator.getTag()) == null) {
                valueAnimator.cancel();
            } else {
                CellLayout.this.mDragOutlineAlphas[i] = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                CellLayout.this.invalidate(CellLayout.this.mDragOutlines[i]);
            }
        }
    }

    /* renamed from: com.android.launcher3.CellLayout$2 */
    class AnonymousClass2 extends AnimatorListenerAdapter {
        final /* synthetic */ InterruptibleInOutAnimator val$anim;

        AnonymousClass2(InterruptibleInOutAnimator interruptibleInOutAnimator2) {
            interruptibleInOutAnimator = interruptibleInOutAnimator2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (((Float) ((ValueAnimator) animator).getAnimatedValue()).floatValue() == 0.0f) {
                interruptibleInOutAnimator.setTag(null);
            }
        }
    }

    public void enableAccessibleDrag(boolean z, int i) {
        this.mUseTouchHelper = z;
        if (!z) {
            ViewCompat.setAccessibilityDelegate(this, null);
            setImportantForAccessibility(2);
            getShortcutsAndWidgets().setImportantForAccessibility(2);
            setOnClickListener(null);
        } else {
            if (i == 2 && !(this.mTouchHelper instanceof WorkspaceAccessibilityHelper)) {
                this.mTouchHelper = new WorkspaceAccessibilityHelper(this);
            } else if (i == 1 && !(this.mTouchHelper instanceof FolderAccessibilityHelper)) {
                this.mTouchHelper = new FolderAccessibilityHelper(this);
            }
            ViewCompat.setAccessibilityDelegate(this, this.mTouchHelper);
            setImportantForAccessibility(1);
            getShortcutsAndWidgets().setImportantForAccessibility(1);
            setOnClickListener(this.mTouchHelper);
        }
        if (getParent() != null) {
            getParent().notifySubtreeAccessibilityStateChanged(this, this, 1);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchHoverEvent(MotionEvent motionEvent) {
        if (this.mUseTouchHelper && this.mTouchHelper.dispatchHoverEvent(motionEvent)) {
            return true;
        }
        return super.dispatchHoverEvent(motionEvent);
    }

    @Override // android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (!this.mUseTouchHelper) {
            if (this.mInterceptTouchListener != null && this.mInterceptTouchListener.onTouch(this, motionEvent)) {
                return true;
            }
            return false;
        }
        return true;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        boolean zOnTouchEvent = super.onTouchEvent(motionEvent);
        if (this.mLauncher.isInState(LauncherState.OVERVIEW) && this.mStylusEventHelper.onMotionEvent(motionEvent)) {
            return true;
        }
        return zOnTouchEvent;
    }

    public void enableHardwareLayer(boolean z) {
        this.mShortcutsAndWidgets.setLayerType(z ? 2 : 0, sPaint);
    }

    public void setCellDimensions(int i, int i2) {
        this.mCellWidth = i;
        this.mFixedCellWidth = i;
        this.mCellHeight = i2;
        this.mFixedCellHeight = i2;
        this.mShortcutsAndWidgets.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mCountX, this.mCountY);
    }

    public void setGridSize(int i, int i2) {
        this.mCountX = i;
        this.mCountY = i2;
        this.mOccupied = new GridOccupancy(this.mCountX, this.mCountY);
        this.mTmpOccupied = new GridOccupancy(this.mCountX, this.mCountY);
        this.mTempRectStack.clear();
        this.mShortcutsAndWidgets.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mCountX, this.mCountY);
        requestLayout();
    }

    public void setInvertIfRtl(boolean z) {
        this.mShortcutsAndWidgets.setInvertIfRtl(z);
    }

    public void setDropPending(boolean z) {
        this.mDropPending = z;
    }

    public boolean isDropPending() {
        return this.mDropPending;
    }

    void setIsDragOverlapping(boolean z) {
        if (this.mIsDragOverlapping != z) {
            this.mIsDragOverlapping = z;
            this.mBackground.setState(this.mIsDragOverlapping ? BACKGROUND_STATE_ACTIVE : BACKGROUND_STATE_DEFAULT);
            invalidate();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> sparseArray) {
        ParcelableSparseArray jailedArray = getJailedArray(sparseArray);
        super.dispatchSaveInstanceState(jailedArray);
        sparseArray.put(R.id.cell_layout_jail_id, jailedArray);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        super.dispatchRestoreInstanceState(getJailedArray(sparseArray));
    }

    private ParcelableSparseArray getJailedArray(SparseArray<Parcelable> sparseArray) {
        Parcelable parcelable = sparseArray.get(R.id.cell_layout_jail_id);
        return parcelable instanceof ParcelableSparseArray ? (ParcelableSparseArray) parcelable : new ParcelableSparseArray();
    }

    public boolean getIsDragOverlapping() {
        return this.mIsDragOverlapping;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        if (this.mBackground.getAlpha() > 0) {
            this.mBackground.draw(canvas);
        }
        Paint paint = this.mDragOutlinePaint;
        for (int i = 0; i < this.mDragOutlines.length; i++) {
            float f = this.mDragOutlineAlphas[i];
            if (f > 0.0f) {
                Bitmap bitmap = (Bitmap) this.mDragOutlineAnims[i].getTag();
                paint.setAlpha((int) (f + 0.5f));
                canvas.drawBitmap(bitmap, (Rect) null, this.mDragOutlines[i], paint);
            }
        }
        for (int i2 = 0; i2 < this.mFolderBackgrounds.size(); i2++) {
            PreviewBackground previewBackground = this.mFolderBackgrounds.get(i2);
            cellToPoint(previewBackground.delegateCellX, previewBackground.delegateCellY, this.mTempLocation);
            canvas.save();
            canvas.translate(this.mTempLocation[0], this.mTempLocation[1]);
            previewBackground.drawBackground(canvas);
            if (!previewBackground.isClipping) {
                previewBackground.drawBackgroundStroke(canvas);
            }
            canvas.restore();
        }
        if (this.mFolderLeaveBehind.delegateCellX >= 0 && this.mFolderLeaveBehind.delegateCellY >= 0) {
            cellToPoint(this.mFolderLeaveBehind.delegateCellX, this.mFolderLeaveBehind.delegateCellY, this.mTempLocation);
            canvas.save();
            canvas.translate(this.mTempLocation[0], this.mTempLocation[1]);
            this.mFolderLeaveBehind.drawLeaveBehind(canvas);
            canvas.restore();
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (int i = 0; i < this.mFolderBackgrounds.size(); i++) {
            PreviewBackground previewBackground = this.mFolderBackgrounds.get(i);
            if (previewBackground.isClipping) {
                cellToPoint(previewBackground.delegateCellX, previewBackground.delegateCellY, this.mTempLocation);
                canvas.save();
                canvas.translate(this.mTempLocation[0], this.mTempLocation[1]);
                previewBackground.drawBackgroundStroke(canvas);
                canvas.restore();
            }
        }
    }

    public void addFolderBackground(PreviewBackground previewBackground) {
        this.mFolderBackgrounds.add(previewBackground);
    }

    public void removeFolderBackground(PreviewBackground previewBackground) {
        this.mFolderBackgrounds.remove(previewBackground);
    }

    public void setFolderLeaveBehindCell(int i, int i2) {
        View childAt = getChildAt(i, i2);
        this.mFolderLeaveBehind.setup(this.mLauncher, null, childAt.getMeasuredWidth(), childAt.getPaddingTop());
        this.mFolderLeaveBehind.delegateCellX = i;
        this.mFolderLeaveBehind.delegateCellY = i2;
        invalidate();
    }

    public void clearFolderLeaveBehind() {
        this.mFolderLeaveBehind.delegateCellX = -1;
        this.mFolderLeaveBehind.delegateCellY = -1;
        invalidate();
    }

    @Override // android.view.ViewGroup
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void restoreInstanceState(SparseArray<Parcelable> sparseArray) {
        try {
            dispatchRestoreInstanceState(sparseArray);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Ignoring an error while restoring a view instance state", e);
        }
    }

    @Override // android.view.View
    public void cancelLongPress() {
        super.cancelLongPress();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            getChildAt(i).cancelLongPress();
        }
    }

    public void setOnInterceptTouchListener(View.OnTouchListener onTouchListener) {
        this.mInterceptTouchListener = onTouchListener;
    }

    public int getCountX() {
        return this.mCountX;
    }

    public int getCountY() {
        return this.mCountY;
    }

    public boolean acceptsWidget() {
        return this.mContainerType == 0;
    }

    public boolean addViewToCellLayout(View view, int i, int i2, LayoutParams layoutParams, boolean z) {
        if (view instanceof BubbleTextView) {
            ((BubbleTextView) view).setTextVisibility(this.mContainerType != 1);
        }
        view.setScaleX(1.0f);
        view.setScaleY(1.0f);
        if (layoutParams.cellX < 0 || layoutParams.cellX > this.mCountX - 1 || layoutParams.cellY < 0 || layoutParams.cellY > this.mCountY - 1) {
            return false;
        }
        if (layoutParams.cellHSpan < 0) {
            layoutParams.cellHSpan = this.mCountX;
        }
        if (layoutParams.cellVSpan < 0) {
            layoutParams.cellVSpan = this.mCountY;
        }
        view.setId(i2);
        this.mShortcutsAndWidgets.addView(view, i, layoutParams);
        if (z) {
            markCellsAsOccupiedForView(view);
        }
        return true;
    }

    @Override // android.view.ViewGroup
    public void removeAllViews() {
        this.mOccupied.clear();
        this.mShortcutsAndWidgets.removeAllViews();
    }

    @Override // android.view.ViewGroup
    public void removeAllViewsInLayout() {
        if (this.mShortcutsAndWidgets.getChildCount() > 0) {
            this.mOccupied.clear();
            this.mShortcutsAndWidgets.removeAllViewsInLayout();
        }
    }

    @Override // android.view.ViewGroup, android.view.ViewManager
    public void removeView(View view) {
        markCellsAsUnoccupiedForView(view);
        this.mShortcutsAndWidgets.removeView(view);
    }

    @Override // android.view.ViewGroup
    public void removeViewAt(int i) {
        markCellsAsUnoccupiedForView(this.mShortcutsAndWidgets.getChildAt(i));
        this.mShortcutsAndWidgets.removeViewAt(i);
    }

    @Override // android.view.ViewGroup
    public void removeViewInLayout(View view) {
        markCellsAsUnoccupiedForView(view);
        this.mShortcutsAndWidgets.removeViewInLayout(view);
    }

    @Override // android.view.ViewGroup
    public void removeViews(int i, int i2) {
        for (int i3 = i; i3 < i + i2; i3++) {
            markCellsAsUnoccupiedForView(this.mShortcutsAndWidgets.getChildAt(i3));
        }
        this.mShortcutsAndWidgets.removeViews(i, i2);
    }

    @Override // android.view.ViewGroup
    public void removeViewsInLayout(int i, int i2) {
        for (int i3 = i; i3 < i + i2; i3++) {
            markCellsAsUnoccupiedForView(this.mShortcutsAndWidgets.getChildAt(i3));
        }
        this.mShortcutsAndWidgets.removeViewsInLayout(i, i2);
    }

    public void pointToCellExact(int i, int i2, int[] iArr) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        iArr[0] = (i - paddingLeft) / this.mCellWidth;
        iArr[1] = (i2 - paddingTop) / this.mCellHeight;
        int i3 = this.mCountX;
        int i4 = this.mCountY;
        if (iArr[0] < 0) {
            iArr[0] = 0;
        }
        if (iArr[0] >= i3) {
            iArr[0] = i3 - 1;
        }
        if (iArr[1] < 0) {
            iArr[1] = 0;
        }
        if (iArr[1] >= i4) {
            iArr[1] = i4 - 1;
        }
    }

    void pointToCellRounded(int i, int i2, int[] iArr) {
        pointToCellExact(i + (this.mCellWidth / 2), i2 + (this.mCellHeight / 2), iArr);
    }

    void cellToPoint(int i, int i2, int[] iArr) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        iArr[0] = paddingLeft + (i * this.mCellWidth);
        iArr[1] = paddingTop + (i2 * this.mCellHeight);
    }

    void cellToCenterPoint(int i, int i2, int[] iArr) {
        regionToCenterPoint(i, i2, 1, 1, iArr);
    }

    void regionToCenterPoint(int i, int i2, int i3, int i4, int[] iArr) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        iArr[0] = paddingLeft + (i * this.mCellWidth) + ((i3 * this.mCellWidth) / 2);
        iArr[1] = paddingTop + (i2 * this.mCellHeight) + ((i4 * this.mCellHeight) / 2);
    }

    void regionToRect(int i, int i2, int i3, int i4, Rect rect) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int i5 = paddingLeft + (i * this.mCellWidth);
        int i6 = paddingTop + (i2 * this.mCellHeight);
        rect.set(i5, i6, (i3 * this.mCellWidth) + i5, (i4 * this.mCellHeight) + i6);
    }

    public float getDistanceFromCell(float f, float f2, int[] iArr) {
        cellToCenterPoint(iArr[0], iArr[1], this.mTmpPoint);
        return (float) Math.hypot(f - this.mTmpPoint[0], f2 - this.mTmpPoint[1]);
    }

    public int getCellWidth() {
        return this.mCellWidth;
    }

    public int getCellHeight() {
        return this.mCellHeight;
    }

    public void setFixedSize(int i, int i2) {
        this.mFixedWidth = i;
        this.mFixedHeight = i2;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        int paddingLeft = size - (getPaddingLeft() + getPaddingRight());
        int paddingTop = size2 - (getPaddingTop() + getPaddingBottom());
        if (this.mFixedCellWidth < 0 || this.mFixedCellHeight < 0) {
            int iCalculateCellWidth = DeviceProfile.calculateCellWidth(paddingLeft, this.mCountX);
            int iCalculateCellHeight = DeviceProfile.calculateCellHeight(paddingTop, this.mCountY);
            if (iCalculateCellWidth != this.mCellWidth || iCalculateCellHeight != this.mCellHeight) {
                this.mCellWidth = iCalculateCellWidth;
                this.mCellHeight = iCalculateCellHeight;
                this.mShortcutsAndWidgets.setCellDimensions(this.mCellWidth, this.mCellHeight, this.mCountX, this.mCountY);
            }
        }
        if (this.mFixedWidth > 0 && this.mFixedHeight > 0) {
            paddingLeft = this.mFixedWidth;
            paddingTop = this.mFixedHeight;
        } else if (mode == 0 || mode2 == 0) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        this.mShortcutsAndWidgets.measure(View.MeasureSpec.makeMeasureSpec(paddingLeft, 1073741824), View.MeasureSpec.makeMeasureSpec(paddingTop, 1073741824));
        int measuredWidth = this.mShortcutsAndWidgets.getMeasuredWidth();
        int measuredHeight = this.mShortcutsAndWidgets.getMeasuredHeight();
        if (this.mFixedWidth > 0 && this.mFixedHeight > 0) {
            setMeasuredDimension(measuredWidth, measuredHeight);
        } else {
            setMeasuredDimension(size, size2);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int paddingLeft = getPaddingLeft() + ((int) Math.ceil(getUnusedHorizontalSpace() / 2.0f));
        int paddingRight = ((i3 - i) - getPaddingRight()) - ((int) Math.ceil(getUnusedHorizontalSpace() / 2.0f));
        int paddingTop = getPaddingTop();
        int paddingBottom = (i4 - i2) - getPaddingBottom();
        this.mShortcutsAndWidgets.layout(paddingLeft, paddingTop, paddingRight, paddingBottom);
        this.mBackground.getPadding(this.mTempRect);
        this.mBackground.setBounds((paddingLeft - this.mTempRect.left) - getPaddingLeft(), (paddingTop - this.mTempRect.top) - getPaddingTop(), paddingRight + this.mTempRect.right + getPaddingRight(), paddingBottom + this.mTempRect.bottom + getPaddingBottom());
    }

    public int getUnusedHorizontalSpace() {
        return ((getMeasuredWidth() - getPaddingLeft()) - getPaddingRight()) - (this.mCountX * this.mCellWidth);
    }

    public Drawable getScrimBackground() {
        return this.mBackground;
    }

    @Override // android.view.View
    protected boolean verifyDrawable(Drawable drawable) {
        return super.verifyDrawable(drawable) || drawable == this.mBackground;
    }

    public ShortcutAndWidgetContainer getShortcutsAndWidgets() {
        return this.mShortcutsAndWidgets;
    }

    public View getChildAt(int i, int i2) {
        return this.mShortcutsAndWidgets.getChildAt(i, i2);
    }

    public boolean animateChildToPosition(View view, int i, int i2, int i3, int i4, boolean z, boolean z2) {
        int i5;
        ShortcutAndWidgetContainer shortcutsAndWidgets = getShortcutsAndWidgets();
        if (shortcutsAndWidgets.indexOfChild(view) == -1) {
            return false;
        }
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        ItemInfo itemInfo = (ItemInfo) view.getTag();
        if (this.mReorderAnimators.containsKey(layoutParams)) {
            this.mReorderAnimators.get(layoutParams).cancel();
            this.mReorderAnimators.remove(layoutParams);
        }
        int i6 = layoutParams.x;
        int i7 = layoutParams.y;
        if (z2) {
            GridOccupancy gridOccupancy = z ? this.mOccupied : this.mTmpOccupied;
            gridOccupancy.markCells(layoutParams.cellX, layoutParams.cellY, layoutParams.cellHSpan, layoutParams.cellVSpan, false);
            i5 = i7;
            gridOccupancy.markCells(i, i2, layoutParams.cellHSpan, layoutParams.cellVSpan, true);
        } else {
            i5 = i7;
        }
        layoutParams.isLockedToGrid = true;
        if (z) {
            itemInfo.cellX = i;
            layoutParams.cellX = i;
            itemInfo.cellY = i2;
            layoutParams.cellY = i2;
        } else {
            layoutParams.tmpCellX = i;
            layoutParams.tmpCellY = i2;
        }
        shortcutsAndWidgets.setupLp(view);
        layoutParams.isLockedToGrid = false;
        int i8 = layoutParams.x;
        int i9 = layoutParams.y;
        layoutParams.x = i6;
        int i10 = i5;
        layoutParams.y = i10;
        if (i6 == i8 && i10 == i9) {
            layoutParams.isLockedToGrid = true;
            return true;
        }
        ValueAnimator valueAnimatorOfFloat = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.setDuration(i3);
        this.mReorderAnimators.put(layoutParams, valueAnimatorOfFloat);
        valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.CellLayout.3
            final /* synthetic */ View val$child;
            final /* synthetic */ LayoutParams val$lp;
            final /* synthetic */ int val$newX;
            final /* synthetic */ int val$newY;
            final /* synthetic */ int val$oldX;
            final /* synthetic */ int val$oldY;

            AnonymousClass3(LayoutParams layoutParams2, int i62, int i82, int i102, int i92, View view2) {
                layoutParams = layoutParams2;
                i = i62;
                i = i82;
                i = i102;
                i = i92;
                view = view2;
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float f = 1.0f - fFloatValue;
                layoutParams.x = (int) ((i * f) + (i * fFloatValue));
                layoutParams.y = (int) ((f * i) + (fFloatValue * i));
                view.requestLayout();
            }
        });
        valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.CellLayout.4
            boolean cancelled = false;
            final /* synthetic */ View val$child;
            final /* synthetic */ LayoutParams val$lp;

            AnonymousClass4(LayoutParams layoutParams2, View view2) {
                layoutParams = layoutParams2;
                view = view2;
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (!this.cancelled) {
                    layoutParams.isLockedToGrid = true;
                    view.requestLayout();
                }
                if (CellLayout.this.mReorderAnimators.containsKey(layoutParams)) {
                    CellLayout.this.mReorderAnimators.remove(layoutParams);
                }
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                this.cancelled = true;
            }
        });
        valueAnimatorOfFloat.setStartDelay(i4);
        valueAnimatorOfFloat.start();
        return true;
    }

    /* renamed from: com.android.launcher3.CellLayout$3 */
    class AnonymousClass3 implements ValueAnimator.AnimatorUpdateListener {
        final /* synthetic */ View val$child;
        final /* synthetic */ LayoutParams val$lp;
        final /* synthetic */ int val$newX;
        final /* synthetic */ int val$newY;
        final /* synthetic */ int val$oldX;
        final /* synthetic */ int val$oldY;

        AnonymousClass3(LayoutParams layoutParams2, int i62, int i82, int i102, int i92, View view2) {
            layoutParams = layoutParams2;
            i = i62;
            i = i82;
            i = i102;
            i = i92;
            view = view2;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            float f = 1.0f - fFloatValue;
            layoutParams.x = (int) ((i * f) + (i * fFloatValue));
            layoutParams.y = (int) ((f * i) + (fFloatValue * i));
            view.requestLayout();
        }
    }

    /* renamed from: com.android.launcher3.CellLayout$4 */
    class AnonymousClass4 extends AnimatorListenerAdapter {
        boolean cancelled = false;
        final /* synthetic */ View val$child;
        final /* synthetic */ LayoutParams val$lp;

        AnonymousClass4(LayoutParams layoutParams2, View view2) {
            layoutParams = layoutParams2;
            view = view2;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            if (!this.cancelled) {
                layoutParams.isLockedToGrid = true;
                view.requestLayout();
            }
            if (CellLayout.this.mReorderAnimators.containsKey(layoutParams)) {
                CellLayout.this.mReorderAnimators.remove(layoutParams);
            }
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            this.cancelled = true;
        }
    }

    void visualizeDropLocation(View view, DragPreviewProvider dragPreviewProvider, int i, int i2, int i3, int i4, boolean z, DropTarget.DragObject dragObject) {
        int width;
        int height;
        int i5 = this.mDragCell[0];
        int i6 = this.mDragCell[1];
        if (dragPreviewProvider == null || dragPreviewProvider.generatedDragOutline == null) {
            return;
        }
        Bitmap bitmap = dragPreviewProvider.generatedDragOutline;
        if (i != i5 || i2 != i6) {
            Point dragVisualizeOffset = dragObject.dragView.getDragVisualizeOffset();
            Rect dragRegion = dragObject.dragView.getDragRegion();
            this.mDragCell[0] = i;
            this.mDragCell[1] = i2;
            int i7 = this.mDragOutlineCurrent;
            this.mDragOutlineAnims[i7].animateOut();
            this.mDragOutlineCurrent = (i7 + 1) % this.mDragOutlines.length;
            Rect rect = this.mDragOutlines[this.mDragOutlineCurrent];
            if (z) {
                cellToRect(i, i2, i3, i4, rect);
                if (view instanceof LauncherAppWidgetHostView) {
                    DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
                    Utilities.shrinkRect(rect, deviceProfile.appWidgetScale.x, deviceProfile.appWidgetScale.y);
                }
            } else {
                int[] iArr = this.mTmpPoint;
                cellToPoint(i, i2, iArr);
                int i8 = iArr[0];
                int i9 = iArr[1];
                if (view != null && dragVisualizeOffset == null) {
                    ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                    int i10 = i8 + marginLayoutParams.leftMargin;
                    height = i9 + marginLayoutParams.topMargin + (((this.mCellHeight * i4) - bitmap.getHeight()) / 2);
                    width = i10 + (((this.mCellWidth * i3) - bitmap.getWidth()) / 2);
                } else if (dragVisualizeOffset != null && dragRegion != null) {
                    width = i8 + dragVisualizeOffset.x + (((this.mCellWidth * i3) - dragRegion.width()) / 2);
                    height = i9 + dragVisualizeOffset.y + ((int) Math.max(0.0f, (this.mCellHeight - getShortcutsAndWidgets().getCellContentHeight()) / 2.0f));
                } else {
                    width = i8 + (((this.mCellWidth * i3) - bitmap.getWidth()) / 2);
                    height = i9 + (((this.mCellHeight * i4) - bitmap.getHeight()) / 2);
                }
                rect.set(width, height, bitmap.getWidth() + width, bitmap.getHeight() + height);
            }
            Utilities.scaleRectAboutCenter(rect, 1.0f);
            this.mDragOutlineAnims[this.mDragOutlineCurrent].setTag(bitmap);
            this.mDragOutlineAnims[this.mDragOutlineCurrent].animateIn();
            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(getItemMoveDescription(i, i2));
            }
        }
    }

    @SuppressLint({"StringFormatMatches"})
    public String getItemMoveDescription(int i, int i2) {
        if (this.mContainerType == 1) {
            return getContext().getString(R.string.move_to_hotseat_position, Integer.valueOf(Math.max(i, i2) + 1));
        }
        return getContext().getString(R.string.move_to_empty_cell, Integer.valueOf(i2 + 1), Integer.valueOf(i + 1));
    }

    public void clearDragOutlines() {
        this.mDragOutlineAnims[this.mDragOutlineCurrent].animateOut();
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
    }

    int[] findNearestVacantArea(int i, int i2, int i3, int i4, int i5, int i6, int[] iArr, int[] iArr2) {
        return findNearestArea(i, i2, i3, i4, i5, i6, true, iArr, iArr2);
    }

    private void lazyInitTempRectStack() {
        if (this.mTempRectStack.isEmpty()) {
            for (int i = 0; i < this.mCountX * this.mCountY; i++) {
                this.mTempRectStack.push(new Rect());
            }
        }
    }

    private void recycleTempRects(Stack<Rect> stack) {
        while (!stack.isEmpty()) {
            this.mTempRectStack.push(stack.pop());
        }
    }

    private int[] findNearestArea(int i, int i2, int i3, int i4, int i5, int i6, boolean z, int[] iArr, int[] iArr2) {
        int[] iArr3;
        int i7;
        int[] iArr4;
        Rect rect;
        boolean z2;
        int i8;
        Rect rect2;
        int i9;
        Rect rect3;
        int i10 = i3;
        int i11 = i4;
        int i12 = i5;
        int i13 = i6;
        lazyInitTempRectStack();
        int i14 = (int) (i - ((this.mCellWidth * (i12 - 1)) / 2.0f));
        int i15 = (int) (i2 - ((this.mCellHeight * (i13 - 1)) / 2.0f));
        if (iArr == null) {
            iArr3 = new int[2];
        } else {
            iArr3 = iArr;
        }
        Rect rect4 = new Rect(-1, -1, -1, -1);
        Stack<Rect> stack = new Stack<>();
        int i16 = this.mCountX;
        int i17 = this.mCountY;
        if (i10 <= 0 || i11 <= 0 || i12 <= 0 || i13 <= 0 || i12 < i10 || i13 < i11) {
            return iArr3;
        }
        int i18 = 0;
        double d = Double.MAX_VALUE;
        while (i18 < i17 - (i11 - 1)) {
            int i19 = 0;
            while (i19 < i16 - (i10 - 1)) {
                if (z) {
                    for (int i20 = 0; i20 < i10; i20++) {
                        int i21 = 0;
                        while (i21 < i11) {
                            iArr4 = iArr3;
                            if (this.mOccupied.cells[i19 + i20][i18 + i21]) {
                                i8 = i14;
                                i7 = i15;
                                rect2 = rect4;
                                break;
                            }
                            i21++;
                            iArr3 = iArr4;
                        }
                    }
                    iArr4 = iArr3;
                    boolean z3 = i10 >= i12;
                    boolean z4 = i11 >= i13;
                    boolean z5 = true;
                    while (true) {
                        if (z3 && z4) {
                            break;
                        }
                        if (z5 && !z3) {
                            boolean z6 = z3;
                            int i22 = 0;
                            while (i22 < i11) {
                                Rect rect5 = rect4;
                                int i23 = i19 + i10;
                                int i24 = i15;
                                if (i23 > i16 - 1 || this.mOccupied.cells[i23][i18 + i22]) {
                                    z6 = true;
                                }
                                i22++;
                                rect4 = rect5;
                                i15 = i24;
                            }
                            i9 = i15;
                            rect3 = rect4;
                            if (!z6) {
                                i10++;
                            }
                            z3 = z6;
                        } else {
                            i9 = i15;
                            rect3 = rect4;
                            if (!z4) {
                                int i25 = 0;
                                while (i25 < i10) {
                                    int i26 = i18 + i11;
                                    int i27 = i10;
                                    if (i26 > i17 - 1 || this.mOccupied.cells[i19 + i25][i26]) {
                                        z4 = true;
                                    }
                                    i25++;
                                    i10 = i27;
                                }
                                int i28 = i10;
                                if (!z4) {
                                    i11++;
                                }
                                i10 = i28;
                            }
                        }
                        z3 |= i10 >= i12;
                        z4 |= i11 >= i13;
                        z5 = !z5;
                        rect4 = rect3;
                        i15 = i9;
                    }
                    i7 = i15;
                    rect = rect4;
                } else {
                    i7 = i15;
                    iArr4 = iArr3;
                    rect = rect4;
                    i10 = -1;
                    i11 = -1;
                }
                cellToCenterPoint(i19, i18, this.mTmpPoint);
                Rect rectPop = this.mTempRectStack.pop();
                rectPop.set(i19, i18, i19 + i10, i18 + i11);
                Iterator<Rect> it = stack.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (it.next().contains(rectPop)) {
                            z2 = true;
                            break;
                        }
                    } else {
                        z2 = false;
                        break;
                    }
                }
                stack.push(rectPop);
                i8 = i14;
                double dHypot = Math.hypot(r5[0] - i14, r5[1] - i7);
                if (dHypot > d || z2) {
                    rect2 = rect;
                    if (rectPop.contains(rect2)) {
                    }
                    i19++;
                    rect4 = rect2;
                    iArr3 = iArr4;
                    i15 = i7;
                    i14 = i8;
                    i10 = i3;
                    i11 = i4;
                    i12 = i5;
                    i13 = i6;
                } else {
                    rect2 = rect;
                }
                iArr4[0] = i19;
                iArr4[1] = i18;
                if (iArr2 != null) {
                    iArr2[0] = i10;
                    iArr2[1] = i11;
                }
                rect2.set(rectPop);
                d = dHypot;
                i19++;
                rect4 = rect2;
                iArr3 = iArr4;
                i15 = i7;
                i14 = i8;
                i10 = i3;
                i11 = i4;
                i12 = i5;
                i13 = i6;
            }
            i18++;
            i10 = i3;
            i11 = i4;
            i12 = i5;
            i13 = i6;
        }
        int[] iArr5 = iArr3;
        if (d == Double.MAX_VALUE) {
            iArr5[0] = -1;
            iArr5[1] = -1;
        }
        recycleTempRects(stack);
        return iArr5;
    }

    private int[] findNearestArea(int i, int i2, int i3, int i4, int[] iArr, boolean[][] zArr, boolean[][] zArr2, int[] iArr2) {
        int[] iArr3;
        int i5;
        int i6 = i3;
        int i7 = i4;
        if (iArr2 == null) {
            iArr3 = new int[2];
        } else {
            iArr3 = iArr2;
        }
        int i8 = this.mCountX;
        int i9 = this.mCountY;
        int i10 = Integer.MIN_VALUE;
        int i11 = 0;
        float f = Float.MAX_VALUE;
        while (i11 < i9 - (i7 - 1)) {
            int i12 = i10;
            float f2 = f;
            int i13 = 0;
            while (i13 < i8 - (i6 - 1)) {
                for (int i14 = 0; i14 < i6; i14++) {
                    for (int i15 = 0; i15 < i7; i15++) {
                        if (zArr[i13 + i14][i11 + i15] && (zArr2 == null || zArr2[i14][i15])) {
                            i5 = i13;
                            break;
                        }
                    }
                }
                int i16 = i13 - i;
                i5 = i13;
                int i17 = i11 - i2;
                float fHypot = (float) Math.hypot(i16, i17);
                int[] iArr4 = this.mTmpPoint;
                computeDirectionVector(i16, i17, iArr4);
                int i18 = (iArr[0] * iArr4[0]) + (iArr[1] * iArr4[1]);
                if (Float.compare(fHypot, f2) < 0 || (Float.compare(fHypot, f2) == 0 && i18 > i12)) {
                    iArr3[0] = i5;
                    iArr3[1] = i11;
                    f2 = fHypot;
                    i12 = i18;
                }
                i13 = i5 + 1;
                i6 = i3;
                i7 = i4;
            }
            i11++;
            f = f2;
            i10 = i12;
            i6 = i3;
            i7 = i4;
        }
        if (f == Float.MAX_VALUE) {
            iArr3[0] = -1;
            iArr3[1] = -1;
        }
        return iArr3;
    }

    private boolean addViewToTempLocation(View view, Rect rect, int[] iArr, ItemConfiguration itemConfiguration) {
        CellAndSpan cellAndSpan = itemConfiguration.map.get(view);
        boolean z = false;
        this.mTmpOccupied.markCells(cellAndSpan, false);
        this.mTmpOccupied.markCells(rect, true);
        findNearestArea(cellAndSpan.cellX, cellAndSpan.cellY, cellAndSpan.spanX, cellAndSpan.spanY, iArr, this.mTmpOccupied.cells, null, this.mTempLocation);
        if (this.mTempLocation[0] >= 0 && this.mTempLocation[1] >= 0) {
            cellAndSpan.cellX = this.mTempLocation[0];
            cellAndSpan.cellY = this.mTempLocation[1];
            z = true;
        }
        this.mTmpOccupied.markCells(cellAndSpan, true);
        return z;
    }

    private class ViewCluster {
        static final int BOTTOM = 8;
        static final int LEFT = 1;
        static final int RIGHT = 4;
        static final int TOP = 2;
        final int[] bottomEdge;
        boolean boundingRectDirty;
        final ItemConfiguration config;
        int dirtyEdges;
        final int[] leftEdge;
        final int[] rightEdge;
        final int[] topEdge;
        final ArrayList<View> views;
        final Rect boundingRect = new Rect();
        final PositionComparator comparator = new PositionComparator();

        public ViewCluster(ArrayList<View> arrayList, ItemConfiguration itemConfiguration) {
            this.leftEdge = new int[CellLayout.this.mCountY];
            this.rightEdge = new int[CellLayout.this.mCountY];
            this.topEdge = new int[CellLayout.this.mCountX];
            this.bottomEdge = new int[CellLayout.this.mCountX];
            this.views = (ArrayList) arrayList.clone();
            this.config = itemConfiguration;
            resetEdges();
        }

        void resetEdges() {
            for (int i = 0; i < CellLayout.this.mCountX; i++) {
                this.topEdge[i] = -1;
                this.bottomEdge[i] = -1;
            }
            for (int i2 = 0; i2 < CellLayout.this.mCountY; i2++) {
                this.leftEdge[i2] = -1;
                this.rightEdge[i2] = -1;
            }
            this.dirtyEdges = 15;
            this.boundingRectDirty = true;
        }

        void computeEdge(int i) {
            int size = this.views.size();
            for (int i2 = 0; i2 < size; i2++) {
                CellAndSpan cellAndSpan = this.config.map.get(this.views.get(i2));
                if (i == 4) {
                    int i3 = cellAndSpan.cellX + cellAndSpan.spanX;
                    for (int i4 = cellAndSpan.cellY; i4 < cellAndSpan.cellY + cellAndSpan.spanY; i4++) {
                        if (i3 > this.rightEdge[i4]) {
                            this.rightEdge[i4] = i3;
                        }
                    }
                } else if (i != 8) {
                    switch (i) {
                        case 1:
                            int i5 = cellAndSpan.cellX;
                            for (int i6 = cellAndSpan.cellY; i6 < cellAndSpan.cellY + cellAndSpan.spanY; i6++) {
                                if (i5 < this.leftEdge[i6] || this.leftEdge[i6] < 0) {
                                    this.leftEdge[i6] = i5;
                                }
                            }
                            break;
                        case 2:
                            int i7 = cellAndSpan.cellY;
                            for (int i8 = cellAndSpan.cellX; i8 < cellAndSpan.cellX + cellAndSpan.spanX; i8++) {
                                if (i7 < this.topEdge[i8] || this.topEdge[i8] < 0) {
                                    this.topEdge[i8] = i7;
                                }
                            }
                            break;
                    }
                } else {
                    int i9 = cellAndSpan.cellY + cellAndSpan.spanY;
                    for (int i10 = cellAndSpan.cellX; i10 < cellAndSpan.cellX + cellAndSpan.spanX; i10++) {
                        if (i9 > this.bottomEdge[i10]) {
                            this.bottomEdge[i10] = i9;
                        }
                    }
                }
            }
        }

        boolean isViewTouchingEdge(View view, int i) {
            CellAndSpan cellAndSpan = this.config.map.get(view);
            if ((this.dirtyEdges & i) == i) {
                computeEdge(i);
                this.dirtyEdges &= ~i;
            }
            if (i == 4) {
                for (int i2 = cellAndSpan.cellY; i2 < cellAndSpan.cellY + cellAndSpan.spanY; i2++) {
                    if (this.rightEdge[i2] == cellAndSpan.cellX) {
                        return true;
                    }
                }
                return false;
            }
            if (i != 8) {
                switch (i) {
                    case 1:
                        for (int i3 = cellAndSpan.cellY; i3 < cellAndSpan.cellY + cellAndSpan.spanY; i3++) {
                            if (this.leftEdge[i3] == cellAndSpan.cellX + cellAndSpan.spanX) {
                                break;
                            }
                        }
                        break;
                    case 2:
                        for (int i4 = cellAndSpan.cellX; i4 < cellAndSpan.cellX + cellAndSpan.spanX; i4++) {
                            if (this.topEdge[i4] == cellAndSpan.cellY + cellAndSpan.spanY) {
                                break;
                            }
                        }
                        break;
                }
                return true;
            }
            for (int i5 = cellAndSpan.cellX; i5 < cellAndSpan.cellX + cellAndSpan.spanX; i5++) {
                if (this.bottomEdge[i5] == cellAndSpan.cellY) {
                    return true;
                }
            }
            return false;
        }

        void shift(int i, int i2) {
            Iterator<View> it = this.views.iterator();
            while (it.hasNext()) {
                CellAndSpan cellAndSpan = this.config.map.get(it.next());
                if (i != 4) {
                    switch (i) {
                        case 1:
                            cellAndSpan.cellX -= i2;
                            break;
                        case 2:
                            cellAndSpan.cellY -= i2;
                            break;
                        default:
                            cellAndSpan.cellY += i2;
                            break;
                    }
                } else {
                    cellAndSpan.cellX += i2;
                }
            }
            resetEdges();
        }

        public void addView(View view) {
            this.views.add(view);
            resetEdges();
        }

        public Rect getBoundingRect() {
            if (this.boundingRectDirty) {
                this.config.getBoundingRectForViews(this.views, this.boundingRect);
            }
            return this.boundingRect;
        }

        class PositionComparator implements Comparator<View> {
            int whichEdge = 0;

            PositionComparator() {
            }

            /* JADX DEBUG: Method merged with bridge method: compare(Ljava/lang/Object;Ljava/lang/Object;)I */
            @Override // java.util.Comparator
            public int compare(View view, View view2) {
                CellAndSpan cellAndSpan = ViewCluster.this.config.map.get(view);
                CellAndSpan cellAndSpan2 = ViewCluster.this.config.map.get(view2);
                int i = this.whichEdge;
                if (i != 4) {
                    switch (i) {
                        case 1:
                            return (cellAndSpan2.cellX + cellAndSpan2.spanX) - (cellAndSpan.cellX + cellAndSpan.spanX);
                        case 2:
                            return (cellAndSpan2.cellY + cellAndSpan2.spanY) - (cellAndSpan.cellY + cellAndSpan.spanY);
                        default:
                            return cellAndSpan.cellY - cellAndSpan2.cellY;
                    }
                }
                return cellAndSpan.cellX - cellAndSpan2.cellX;
            }
        }

        public void sortConfigurationForEdgePush(int i) {
            this.comparator.whichEdge = i;
            Collections.sort(this.config.sortedViews, this.comparator);
        }
    }

    private boolean pushViewsToTempLocation(ArrayList<View> arrayList, Rect rect, int[] iArr, View view, ItemConfiguration itemConfiguration) {
        int i;
        int i2;
        ViewCluster viewCluster = new ViewCluster(arrayList, itemConfiguration);
        Rect boundingRect = viewCluster.getBoundingRect();
        boolean z = false;
        if (iArr[0] < 0) {
            i2 = boundingRect.right - rect.left;
            i = 1;
        } else if (iArr[0] > 0) {
            i = 4;
            i2 = rect.right - boundingRect.left;
        } else if (iArr[1] < 0) {
            i = 2;
            i2 = boundingRect.bottom - rect.top;
        } else {
            i = 8;
            i2 = rect.bottom - boundingRect.top;
        }
        if (i2 <= 0) {
            return false;
        }
        Iterator<View> it = arrayList.iterator();
        while (it.hasNext()) {
            this.mTmpOccupied.markCells(itemConfiguration.map.get(it.next()), false);
        }
        itemConfiguration.save();
        viewCluster.sortConfigurationForEdgePush(i);
        boolean z2 = false;
        while (i2 > 0 && !z2) {
            Iterator<View> it2 = itemConfiguration.sortedViews.iterator();
            while (true) {
                if (it2.hasNext()) {
                    View next = it2.next();
                    if (!viewCluster.views.contains(next) && next != view && viewCluster.isViewTouchingEdge(next, i)) {
                        if (((LayoutParams) next.getLayoutParams()).canReorder) {
                            viewCluster.addView(next);
                            this.mTmpOccupied.markCells(itemConfiguration.map.get(next), false);
                        } else {
                            z2 = true;
                            break;
                        }
                    }
                }
            }
            i2--;
            viewCluster.shift(i, 1);
        }
        Rect boundingRect2 = viewCluster.getBoundingRect();
        if (z2 || boundingRect2.left < 0 || boundingRect2.right > this.mCountX || boundingRect2.top < 0 || boundingRect2.bottom > this.mCountY) {
            itemConfiguration.restore();
        } else {
            z = true;
        }
        Iterator<View> it3 = viewCluster.views.iterator();
        while (it3.hasNext()) {
            this.mTmpOccupied.markCells(itemConfiguration.map.get(it3.next()), true);
        }
        return z;
    }

    private boolean addViewsToTempLocation(ArrayList<View> arrayList, Rect rect, int[] iArr, View view, ItemConfiguration itemConfiguration) {
        boolean z;
        if (arrayList.size() == 0) {
            return true;
        }
        Rect rect2 = new Rect();
        itemConfiguration.getBoundingRectForViews(arrayList, rect2);
        Iterator<View> it = arrayList.iterator();
        while (true) {
            z = false;
            if (!it.hasNext()) {
                break;
            }
            this.mTmpOccupied.markCells(itemConfiguration.map.get(it.next()), false);
        }
        GridOccupancy gridOccupancy = new GridOccupancy(rect2.width(), rect2.height());
        int i = rect2.top;
        int i2 = rect2.left;
        Iterator<View> it2 = arrayList.iterator();
        while (it2.hasNext()) {
            CellAndSpan cellAndSpan = itemConfiguration.map.get(it2.next());
            gridOccupancy.markCells(cellAndSpan.cellX - i2, cellAndSpan.cellY - i, cellAndSpan.spanX, cellAndSpan.spanY, true);
        }
        this.mTmpOccupied.markCells(rect, true);
        findNearestArea(rect2.left, rect2.top, rect2.width(), rect2.height(), iArr, this.mTmpOccupied.cells, gridOccupancy.cells, this.mTempLocation);
        if (this.mTempLocation[0] >= 0 && this.mTempLocation[1] >= 0) {
            int i3 = this.mTempLocation[0] - rect2.left;
            int i4 = this.mTempLocation[1] - rect2.top;
            Iterator<View> it3 = arrayList.iterator();
            while (it3.hasNext()) {
                CellAndSpan cellAndSpan2 = itemConfiguration.map.get(it3.next());
                cellAndSpan2.cellX += i3;
                cellAndSpan2.cellY += i4;
            }
            z = true;
        }
        Iterator<View> it4 = arrayList.iterator();
        while (it4.hasNext()) {
            this.mTmpOccupied.markCells(itemConfiguration.map.get(it4.next()), true);
        }
        return z;
    }

    private boolean attemptPushInDirection(ArrayList<View> arrayList, Rect rect, int[] iArr, View view, ItemConfiguration itemConfiguration) {
        if (Math.abs(iArr[0]) + Math.abs(iArr[1]) > 1) {
            int i = iArr[1];
            iArr[1] = 0;
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[1] = i;
            int i2 = iArr[0];
            iArr[0] = 0;
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = i2;
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
            int i3 = iArr[1];
            iArr[1] = 0;
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[1] = i3;
            int i4 = iArr[0];
            iArr[0] = 0;
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = i4;
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
        } else {
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
            int i5 = iArr[1];
            iArr[1] = iArr[0];
            iArr[0] = i5;
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
            if (pushViewsToTempLocation(arrayList, rect, iArr, view, itemConfiguration)) {
                return true;
            }
            iArr[0] = iArr[0] * (-1);
            iArr[1] = iArr[1] * (-1);
            int i6 = iArr[1];
            iArr[1] = iArr[0];
            iArr[0] = i6;
        }
        return false;
    }

    private boolean rearrangementExists(int i, int i2, int i3, int i4, int[] iArr, View view, ItemConfiguration itemConfiguration) {
        CellAndSpan cellAndSpan;
        if (i < 0 || i2 < 0) {
            return false;
        }
        this.mIntersectingViews.clear();
        int i5 = i3 + i;
        int i6 = i4 + i2;
        this.mOccupiedRect.set(i, i2, i5, i6);
        if (view != null && (cellAndSpan = itemConfiguration.map.get(view)) != null) {
            cellAndSpan.cellX = i;
            cellAndSpan.cellY = i2;
        }
        Rect rect = new Rect(i, i2, i5, i6);
        Rect rect2 = new Rect();
        for (View view2 : itemConfiguration.map.keySet()) {
            if (view2 != view) {
                CellAndSpan cellAndSpan2 = itemConfiguration.map.get(view2);
                LayoutParams layoutParams = (LayoutParams) view2.getLayoutParams();
                rect2.set(cellAndSpan2.cellX, cellAndSpan2.cellY, cellAndSpan2.cellX + cellAndSpan2.spanX, cellAndSpan2.cellY + cellAndSpan2.spanY);
                if (!Rect.intersects(rect, rect2)) {
                    continue;
                } else {
                    if (!layoutParams.canReorder) {
                        return false;
                    }
                    this.mIntersectingViews.add(view2);
                }
            }
        }
        itemConfiguration.intersectingViews = new ArrayList<>(this.mIntersectingViews);
        if (attemptPushInDirection(this.mIntersectingViews, this.mOccupiedRect, iArr, view, itemConfiguration) || addViewsToTempLocation(this.mIntersectingViews, this.mOccupiedRect, iArr, view, itemConfiguration)) {
            return true;
        }
        Iterator<View> it = this.mIntersectingViews.iterator();
        while (it.hasNext()) {
            if (!addViewToTempLocation(it.next(), this.mOccupiedRect, iArr, itemConfiguration)) {
                return false;
            }
        }
        return true;
    }

    private void computeDirectionVector(float f, float f2, int[] iArr) {
        double dAtan = Math.atan(f2 / f);
        iArr[0] = 0;
        iArr[1] = 0;
        if (Math.abs(Math.cos(dAtan)) > 0.5d) {
            iArr[0] = (int) Math.signum(f);
        }
        if (Math.abs(Math.sin(dAtan)) > 0.5d) {
            iArr[1] = (int) Math.signum(f2);
        }
    }

    private ItemConfiguration findReorderSolution(int i, int i2, int i3, int i4, int i5, int i6, int[] iArr, View view, boolean z, ItemConfiguration itemConfiguration) {
        copyCurrentStateToSolution(itemConfiguration, false);
        this.mOccupied.copyTo(this.mTmpOccupied);
        int[] iArrFindNearestArea = findNearestArea(i, i2, i5, i6, new int[2]);
        if (!rearrangementExists(iArrFindNearestArea[0], iArrFindNearestArea[1], i5, i6, iArr, view, itemConfiguration)) {
            if (i5 > i3 && (i4 == i6 || z)) {
                return findReorderSolution(i, i2, i3, i4, i5 - 1, i6, iArr, view, false, itemConfiguration);
            }
            if (i6 > i4) {
                return findReorderSolution(i, i2, i3, i4, i5, i6 - 1, iArr, view, true, itemConfiguration);
            }
            itemConfiguration.isSolution = false;
        } else {
            itemConfiguration.isSolution = true;
            itemConfiguration.cellX = iArrFindNearestArea[0];
            itemConfiguration.cellY = iArrFindNearestArea[1];
            itemConfiguration.spanX = i5;
            itemConfiguration.spanY = i6;
        }
        return itemConfiguration;
    }

    private void copyCurrentStateToSolution(ItemConfiguration itemConfiguration, boolean z) {
        CellAndSpan cellAndSpan;
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (z) {
                cellAndSpan = new CellAndSpan(layoutParams.tmpCellX, layoutParams.tmpCellY, layoutParams.cellHSpan, layoutParams.cellVSpan);
            } else {
                cellAndSpan = new CellAndSpan(layoutParams.cellX, layoutParams.cellY, layoutParams.cellHSpan, layoutParams.cellVSpan);
            }
            itemConfiguration.add(childAt, cellAndSpan);
        }
    }

    private void copySolutionToTempState(ItemConfiguration itemConfiguration, View view) {
        this.mTmpOccupied.clear();
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i);
            if (childAt != view) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                CellAndSpan cellAndSpan = itemConfiguration.map.get(childAt);
                if (cellAndSpan != null) {
                    layoutParams.tmpCellX = cellAndSpan.cellX;
                    layoutParams.tmpCellY = cellAndSpan.cellY;
                    layoutParams.cellHSpan = cellAndSpan.spanX;
                    layoutParams.cellVSpan = cellAndSpan.spanY;
                    this.mTmpOccupied.markCells(cellAndSpan, true);
                }
            }
        }
        this.mTmpOccupied.markCells((CellAndSpan) itemConfiguration, true);
    }

    private void animateItemsToSolution(ItemConfiguration itemConfiguration, View view, boolean z) {
        CellAndSpan cellAndSpan;
        GridOccupancy gridOccupancy = this.mTmpOccupied;
        gridOccupancy.clear();
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i);
            if (childAt != view && (cellAndSpan = itemConfiguration.map.get(childAt)) != null) {
                animateChildToPosition(childAt, cellAndSpan.cellX, cellAndSpan.cellY, 150, 0, false, false);
                gridOccupancy.markCells(cellAndSpan, true);
            }
        }
        if (z) {
            gridOccupancy.markCells((CellAndSpan) itemConfiguration, true);
        }
    }

    private void beginOrAdjustReorderPreviewAnimations(ItemConfiguration itemConfiguration, View view, int i, int i2) {
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i3 = 0; i3 < childCount; i3++) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i3);
            if (childAt != view) {
                CellAndSpan cellAndSpan = itemConfiguration.map.get(childAt);
                boolean z = (i2 != 0 || itemConfiguration.intersectingViews == null || itemConfiguration.intersectingViews.contains(childAt)) ? false : true;
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (cellAndSpan != null && !z) {
                    new ReorderPreviewAnimation(childAt, i2, layoutParams.cellX, layoutParams.cellY, cellAndSpan.cellX, cellAndSpan.cellY, cellAndSpan.spanX, cellAndSpan.spanY).animate();
                }
            }
        }
    }

    class ReorderPreviewAnimation {
        private static final float CHILD_DIVIDEND = 4.0f;
        private static final int HINT_DURATION = 650;
        public static final int MODE_HINT = 0;
        public static final int MODE_PREVIEW = 1;
        private static final int PREVIEW_DURATION = 300;
        Animator a;
        final View child;
        float finalDeltaX;
        float finalDeltaY;
        final float finalScale;
        float initDeltaX;
        float initDeltaY;
        float initScale;
        final int mode;
        boolean repeating = false;

        public ReorderPreviewAnimation(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7) {
            CellLayout.this.regionToCenterPoint(i2, i3, i6, i7, CellLayout.this.mTmpPoint);
            int i8 = CellLayout.this.mTmpPoint[0];
            int i9 = CellLayout.this.mTmpPoint[1];
            CellLayout.this.regionToCenterPoint(i4, i5, i6, i7, CellLayout.this.mTmpPoint);
            int i10 = CellLayout.this.mTmpPoint[0] - i8;
            int i11 = CellLayout.this.mTmpPoint[1] - i9;
            this.child = view;
            this.mode = i;
            setInitialAnimationValues(false);
            this.finalScale = (1.0f - (CHILD_DIVIDEND / view.getWidth())) * this.initScale;
            this.finalDeltaX = this.initDeltaX;
            this.finalDeltaY = this.initDeltaY;
            int i12 = i == 0 ? -1 : 1;
            if (i10 != i11 || i10 != 0) {
                if (i11 == 0) {
                    this.finalDeltaX += (-i12) * Math.signum(i10) * CellLayout.this.mReorderPreviewAnimationMagnitude;
                    return;
                }
                if (i10 == 0) {
                    this.finalDeltaY += (-i12) * Math.signum(i11) * CellLayout.this.mReorderPreviewAnimationMagnitude;
                    return;
                }
                float f = i11;
                float f2 = i10;
                double dAtan = Math.atan(f / f2);
                float f3 = -i12;
                this.finalDeltaX += (int) (Math.signum(f2) * f3 * Math.abs(Math.cos(dAtan) * CellLayout.this.mReorderPreviewAnimationMagnitude));
                this.finalDeltaY += (int) (f3 * Math.signum(f) * Math.abs(Math.sin(dAtan) * CellLayout.this.mReorderPreviewAnimationMagnitude));
            }
        }

        void setInitialAnimationValues(boolean z) {
            if (z) {
                if (this.child instanceof LauncherAppWidgetHostView) {
                    LauncherAppWidgetHostView launcherAppWidgetHostView = (LauncherAppWidgetHostView) this.child;
                    this.initScale = launcherAppWidgetHostView.getScaleToFit();
                    this.initDeltaX = launcherAppWidgetHostView.getTranslationForCentering().x;
                    this.initDeltaY = launcherAppWidgetHostView.getTranslationForCentering().y;
                    return;
                }
                this.initScale = 1.0f;
                this.initDeltaX = 0.0f;
                this.initDeltaY = 0.0f;
                return;
            }
            this.initScale = this.child.getScaleX();
            this.initDeltaX = this.child.getTranslationX();
            this.initDeltaY = this.child.getTranslationY();
        }

        void animate() {
            boolean z = this.finalDeltaX == this.initDeltaX && this.finalDeltaY == this.initDeltaY;
            if (CellLayout.this.mShakeAnimators.containsKey(this.child)) {
                CellLayout.this.mShakeAnimators.get(this.child).cancel();
                CellLayout.this.mShakeAnimators.remove(this.child);
                if (z) {
                    completeAnimationImmediately();
                    return;
                }
            }
            if (z) {
                return;
            }
            ValueAnimator valueAnimatorOfFloat = LauncherAnimUtils.ofFloat(0.0f, 1.0f);
            this.a = valueAnimatorOfFloat;
            if (!Utilities.isPowerSaverPreventingAnimation(CellLayout.this.getContext())) {
                valueAnimatorOfFloat.setRepeatMode(2);
                valueAnimatorOfFloat.setRepeatCount(-1);
            }
            valueAnimatorOfFloat.setDuration(this.mode == 0 ? 650L : 300L);
            valueAnimatorOfFloat.setStartDelay((int) (Math.random() * 60.0d));
            valueAnimatorOfFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.launcher3.CellLayout.ReorderPreviewAnimation.1
                AnonymousClass1() {
                }

                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                    float f = (ReorderPreviewAnimation.this.mode == 0 && ReorderPreviewAnimation.this.repeating) ? 1.0f : fFloatValue;
                    float f2 = 1.0f - f;
                    float f3 = (ReorderPreviewAnimation.this.finalDeltaX * f) + (ReorderPreviewAnimation.this.initDeltaX * f2);
                    float f4 = (f * ReorderPreviewAnimation.this.finalDeltaY) + (f2 * ReorderPreviewAnimation.this.initDeltaY);
                    ReorderPreviewAnimation.this.child.setTranslationX(f3);
                    ReorderPreviewAnimation.this.child.setTranslationY(f4);
                    float f5 = (ReorderPreviewAnimation.this.finalScale * fFloatValue) + ((1.0f - fFloatValue) * ReorderPreviewAnimation.this.initScale);
                    ReorderPreviewAnimation.this.child.setScaleX(f5);
                    ReorderPreviewAnimation.this.child.setScaleY(f5);
                }
            });
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() { // from class: com.android.launcher3.CellLayout.ReorderPreviewAnimation.2
                AnonymousClass2() {
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animator) {
                    ReorderPreviewAnimation.this.setInitialAnimationValues(true);
                    ReorderPreviewAnimation.this.repeating = true;
                }
            });
            CellLayout.this.mShakeAnimators.put(this.child, this);
            valueAnimatorOfFloat.start();
        }

        /* renamed from: com.android.launcher3.CellLayout$ReorderPreviewAnimation$1 */
        class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
            AnonymousClass1() {
            }

            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fFloatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                float f = (ReorderPreviewAnimation.this.mode == 0 && ReorderPreviewAnimation.this.repeating) ? 1.0f : fFloatValue;
                float f2 = 1.0f - f;
                float f3 = (ReorderPreviewAnimation.this.finalDeltaX * f) + (ReorderPreviewAnimation.this.initDeltaX * f2);
                float f4 = (f * ReorderPreviewAnimation.this.finalDeltaY) + (f2 * ReorderPreviewAnimation.this.initDeltaY);
                ReorderPreviewAnimation.this.child.setTranslationX(f3);
                ReorderPreviewAnimation.this.child.setTranslationY(f4);
                float f5 = (ReorderPreviewAnimation.this.finalScale * fFloatValue) + ((1.0f - fFloatValue) * ReorderPreviewAnimation.this.initScale);
                ReorderPreviewAnimation.this.child.setScaleX(f5);
                ReorderPreviewAnimation.this.child.setScaleY(f5);
            }
        }

        /* renamed from: com.android.launcher3.CellLayout$ReorderPreviewAnimation$2 */
        class AnonymousClass2 extends AnimatorListenerAdapter {
            AnonymousClass2() {
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
                ReorderPreviewAnimation.this.setInitialAnimationValues(true);
                ReorderPreviewAnimation.this.repeating = true;
            }
        }

        private void cancel() {
            if (this.a != null) {
                this.a.cancel();
            }
        }

        void completeAnimationImmediately() {
            if (this.a != null) {
                this.a.cancel();
            }
            setInitialAnimationValues(true);
            this.a = LauncherAnimUtils.ofPropertyValuesHolder(this.child, new PropertyListBuilder().scale(this.initScale).translationX(this.initDeltaX).translationY(this.initDeltaY).build()).setDuration(150L);
            this.a.setInterpolator(new DecelerateInterpolator(1.5f));
            this.a.start();
        }
    }

    private void completeAndClearReorderPreviewAnimations() {
        Iterator<ReorderPreviewAnimation> it = this.mShakeAnimators.values().iterator();
        while (it.hasNext()) {
            it.next().completeAnimationImmediately();
        }
        this.mShakeAnimators.clear();
    }

    /* JADX WARN: Removed duplicated region for block: B:54:0x0091  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private void commitTempPlacement() {
        int i;
        int i2;
        this.mTmpOccupied.copyTo(this.mOccupied);
        long idForScreen = this.mLauncher.getWorkspace().getIdForScreen(this);
        if (this.mContainerType == 1) {
            idForScreen = -1;
            i = LauncherSettings.Favorites.CONTAINER_HOTSEAT;
        } else {
            i = -100;
        }
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        int i3 = 0;
        while (i3 < childCount) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i3);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            ItemInfo itemInfo = (ItemInfo) childAt.getTag();
            if (itemInfo == null) {
                i2 = i3;
            } else {
                boolean z = (itemInfo.cellX == layoutParams.tmpCellX && itemInfo.cellY == layoutParams.tmpCellY && itemInfo.spanX == layoutParams.cellHSpan && itemInfo.spanY == layoutParams.cellVSpan) ? false : true;
                int i4 = layoutParams.tmpCellX;
                layoutParams.cellX = i4;
                itemInfo.cellX = i4;
                int i5 = layoutParams.tmpCellY;
                layoutParams.cellY = i5;
                itemInfo.cellY = i5;
                itemInfo.spanX = layoutParams.cellHSpan;
                itemInfo.spanY = layoutParams.cellVSpan;
                if (z) {
                    i2 = i3;
                    this.mLauncher.getModelWriter().modifyItemInDatabase(itemInfo, i, idForScreen, itemInfo.cellX, itemInfo.cellY, itemInfo.spanX, itemInfo.spanY);
                }
            }
            i3 = i2 + 1;
        }
    }

    private void setUseTempCoords(boolean z) {
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ((LayoutParams) this.mShortcutsAndWidgets.getChildAt(i).getLayoutParams()).useTmpCoords = z;
        }
    }

    private ItemConfiguration findConfigurationNoShuffle(int i, int i2, int i3, int i4, int i5, int i6, View view, ItemConfiguration itemConfiguration) {
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        findNearestVacantArea(i, i2, i3, i4, i5, i6, iArr, iArr2);
        if (iArr[0] >= 0 && iArr[1] >= 0) {
            copyCurrentStateToSolution(itemConfiguration, false);
            itemConfiguration.cellX = iArr[0];
            itemConfiguration.cellY = iArr[1];
            itemConfiguration.spanX = iArr2[0];
            itemConfiguration.spanY = iArr2[1];
            itemConfiguration.isSolution = true;
        } else {
            itemConfiguration.isSolution = false;
        }
        return itemConfiguration;
    }

    private void getDirectionVectorForDrop(int i, int i2, int i3, int i4, View view, int[] iArr) {
        int[] iArr2 = new int[2];
        findNearestArea(i, i2, i3, i4, iArr2);
        Rect rect = new Rect();
        regionToRect(iArr2[0], iArr2[1], i3, i4, rect);
        rect.offset(i - rect.centerX(), i2 - rect.centerY());
        Rect rect2 = new Rect();
        getViewsIntersectingRegion(iArr2[0], iArr2[1], i3, i4, view, rect2, this.mIntersectingViews);
        int iWidth = rect2.width();
        int iHeight = rect2.height();
        regionToRect(rect2.left, rect2.top, rect2.width(), rect2.height(), rect2);
        int iCenterX = (rect2.centerX() - i) / i3;
        int iCenterY = (rect2.centerY() - i2) / i4;
        if (iWidth == this.mCountX || i3 == this.mCountX) {
            iCenterX = 0;
        }
        if (iHeight == this.mCountY || i4 == this.mCountY) {
            iCenterY = 0;
        }
        if (iCenterX != 0 || iCenterY != 0) {
            computeDirectionVector(iCenterX, iCenterY, iArr);
        } else {
            iArr[0] = 1;
            iArr[1] = 0;
        }
    }

    private void getViewsIntersectingRegion(int i, int i2, int i3, int i4, View view, Rect rect, ArrayList<View> arrayList) {
        if (rect != null) {
            rect.set(i, i2, i + i3, i2 + i4);
        }
        arrayList.clear();
        Rect rect2 = new Rect(i, i2, i3 + i, i4 + i2);
        Rect rect3 = new Rect();
        int childCount = this.mShortcutsAndWidgets.getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt = this.mShortcutsAndWidgets.getChildAt(i5);
            if (childAt != view) {
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                rect3.set(layoutParams.cellX, layoutParams.cellY, layoutParams.cellX + layoutParams.cellHSpan, layoutParams.cellY + layoutParams.cellVSpan);
                if (Rect.intersects(rect2, rect3)) {
                    this.mIntersectingViews.add(childAt);
                    if (rect != null) {
                        rect.union(rect3);
                    }
                }
            }
        }
    }

    boolean isNearestDropLocationOccupied(int i, int i2, int i3, int i4, View view, int[] iArr) {
        int[] iArrFindNearestArea = findNearestArea(i, i2, i3, i4, iArr);
        getViewsIntersectingRegion(iArrFindNearestArea[0], iArrFindNearestArea[1], i3, i4, view, null, this.mIntersectingViews);
        return !this.mIntersectingViews.isEmpty();
    }

    void revertTempState() {
        completeAndClearReorderPreviewAnimations();
        if (isItemPlacementDirty()) {
            int childCount = this.mShortcutsAndWidgets.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = this.mShortcutsAndWidgets.getChildAt(i);
                LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
                if (layoutParams.tmpCellX != layoutParams.cellX || layoutParams.tmpCellY != layoutParams.cellY) {
                    layoutParams.tmpCellX = layoutParams.cellX;
                    layoutParams.tmpCellY = layoutParams.cellY;
                    animateChildToPosition(childAt, layoutParams.cellX, layoutParams.cellY, 150, 0, false, false);
                }
            }
            setItemPlacementDirty(false);
        }
    }

    boolean createAreaForResize(int i, int i2, int i3, int i4, View view, int[] iArr, boolean z) {
        int[] iArr2 = new int[2];
        regionToCenterPoint(i, i2, i3, i4, iArr2);
        ItemConfiguration itemConfigurationFindReorderSolution = findReorderSolution(iArr2[0], iArr2[1], i3, i4, i3, i4, iArr, view, true, new ItemConfiguration());
        setUseTempCoords(true);
        if (itemConfigurationFindReorderSolution != null && itemConfigurationFindReorderSolution.isSolution) {
            copySolutionToTempState(itemConfigurationFindReorderSolution, view);
            setItemPlacementDirty(true);
            animateItemsToSolution(itemConfigurationFindReorderSolution, view, z);
            if (z) {
                commitTempPlacement();
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(itemConfigurationFindReorderSolution, view, 150, 1);
            }
            this.mShortcutsAndWidgets.requestLayout();
        }
        return itemConfigurationFindReorderSolution.isSolution;
    }

    int[] performReorder(int i, int i2, int i3, int i4, int i5, int i6, View view, int[] iArr, int[] iArr2, int i7) {
        int i8;
        int[] iArrFindNearestArea = findNearestArea(i, i2, i5, i6, iArr);
        int[] iArr3 = iArr2 == null ? new int[2] : iArr2;
        if ((i7 == 2 || i7 == 3 || i7 == 4) && this.mPreviousReorderDirection[0] != -100) {
            this.mDirectionVector[0] = this.mPreviousReorderDirection[0];
            this.mDirectionVector[1] = this.mPreviousReorderDirection[1];
            if (i7 == 2 || i7 == 3) {
                this.mPreviousReorderDirection[0] = -100;
                this.mPreviousReorderDirection[1] = -100;
            }
        } else {
            getDirectionVectorForDrop(i, i2, i5, i6, view, this.mDirectionVector);
            this.mPreviousReorderDirection[0] = this.mDirectionVector[0];
            this.mPreviousReorderDirection[1] = this.mDirectionVector[1];
        }
        ItemConfiguration itemConfigurationFindReorderSolution = findReorderSolution(i, i2, i3, i4, i5, i6, this.mDirectionVector, view, true, new ItemConfiguration());
        ItemConfiguration itemConfigurationFindConfigurationNoShuffle = findConfigurationNoShuffle(i, i2, i3, i4, i5, i6, view, new ItemConfiguration());
        if (!itemConfigurationFindReorderSolution.isSolution || itemConfigurationFindReorderSolution.area() < itemConfigurationFindConfigurationNoShuffle.area()) {
            if (!itemConfigurationFindConfigurationNoShuffle.isSolution) {
                itemConfigurationFindConfigurationNoShuffle = null;
            }
        } else {
            itemConfigurationFindConfigurationNoShuffle = itemConfigurationFindReorderSolution;
        }
        if (i7 == 0) {
            if (itemConfigurationFindConfigurationNoShuffle != null) {
                beginOrAdjustReorderPreviewAnimations(itemConfigurationFindConfigurationNoShuffle, view, 0, 0);
                iArrFindNearestArea[0] = itemConfigurationFindConfigurationNoShuffle.cellX;
                iArrFindNearestArea[1] = itemConfigurationFindConfigurationNoShuffle.cellY;
                iArr3[0] = itemConfigurationFindConfigurationNoShuffle.spanX;
                iArr3[1] = itemConfigurationFindConfigurationNoShuffle.spanY;
            } else {
                iArr3[1] = -1;
                iArr3[0] = -1;
                iArrFindNearestArea[1] = -1;
                iArrFindNearestArea[0] = -1;
            }
            return iArrFindNearestArea;
        }
        boolean z = true;
        setUseTempCoords(true);
        if (itemConfigurationFindConfigurationNoShuffle != null) {
            iArrFindNearestArea[0] = itemConfigurationFindConfigurationNoShuffle.cellX;
            iArrFindNearestArea[1] = itemConfigurationFindConfigurationNoShuffle.cellY;
            iArr3[0] = itemConfigurationFindConfigurationNoShuffle.spanX;
            iArr3[1] = itemConfigurationFindConfigurationNoShuffle.spanY;
            if (i7 != 1 && i7 != 2) {
                i8 = 3;
                if (i7 == 3) {
                }
            } else {
                i8 = 3;
            }
            copySolutionToTempState(itemConfigurationFindConfigurationNoShuffle, view);
            setItemPlacementDirty(true);
            animateItemsToSolution(itemConfigurationFindConfigurationNoShuffle, view, i7 == 2);
            if (i7 == 2 || i7 == i8) {
                commitTempPlacement();
                completeAndClearReorderPreviewAnimations();
                setItemPlacementDirty(false);
            } else {
                beginOrAdjustReorderPreviewAnimations(itemConfigurationFindConfigurationNoShuffle, view, 150, 1);
            }
        } else {
            iArr3[1] = -1;
            iArr3[0] = -1;
            iArrFindNearestArea[1] = -1;
            iArrFindNearestArea[0] = -1;
            z = false;
        }
        if (i7 == 2 || !z) {
            setUseTempCoords(false);
        }
        this.mShortcutsAndWidgets.requestLayout();
        return iArrFindNearestArea;
    }

    void setItemPlacementDirty(boolean z) {
        this.mItemPlacementDirty = z;
    }

    boolean isItemPlacementDirty() {
        return this.mItemPlacementDirty;
    }

    private static class ItemConfiguration extends CellAndSpan {
        ArrayList<View> intersectingViews;
        boolean isSolution;
        final ArrayMap<View, CellAndSpan> map;
        private final ArrayMap<View, CellAndSpan> savedMap;
        final ArrayList<View> sortedViews;

        private ItemConfiguration() {
            this.map = new ArrayMap<>();
            this.savedMap = new ArrayMap<>();
            this.sortedViews = new ArrayList<>();
            this.isSolution = false;
        }

        /* synthetic */ ItemConfiguration(AnonymousClass1 anonymousClass1) {
            this();
        }

        void save() {
            for (View view : this.map.keySet()) {
                this.savedMap.get(view).copyFrom(this.map.get(view));
            }
        }

        void restore() {
            for (View view : this.savedMap.keySet()) {
                this.map.get(view).copyFrom(this.savedMap.get(view));
            }
        }

        void add(View view, CellAndSpan cellAndSpan) {
            this.map.put(view, cellAndSpan);
            this.savedMap.put(view, new CellAndSpan());
            this.sortedViews.add(view);
        }

        int area() {
            return this.spanX * this.spanY;
        }

        void getBoundingRectForViews(ArrayList<View> arrayList, Rect rect) {
            Iterator<View> it = arrayList.iterator();
            boolean z = true;
            while (it.hasNext()) {
                CellAndSpan cellAndSpan = this.map.get(it.next());
                if (z) {
                    rect.set(cellAndSpan.cellX, cellAndSpan.cellY, cellAndSpan.cellX + cellAndSpan.spanX, cellAndSpan.cellY + cellAndSpan.spanY);
                    z = false;
                } else {
                    rect.union(cellAndSpan.cellX, cellAndSpan.cellY, cellAndSpan.cellX + cellAndSpan.spanX, cellAndSpan.cellY + cellAndSpan.spanY);
                }
            }
        }
    }

    public int[] findNearestArea(int i, int i2, int i3, int i4, int[] iArr) {
        return findNearestArea(i, i2, i3, i4, i3, i4, false, iArr, null);
    }

    boolean existsEmptyCell() {
        return findCellForSpan(null, 1, 1);
    }

    public boolean findCellForSpan(int[] iArr, int i, int i2) {
        if (iArr == null) {
            iArr = new int[2];
        }
        return this.mOccupied.findVacantCell(iArr, i, i2);
    }

    void onDragEnter() {
        this.mDragging = true;
    }

    void onDragExit() {
        if (this.mDragging) {
            this.mDragging = false;
        }
        int[] iArr = this.mDragCell;
        this.mDragCell[1] = -1;
        iArr[0] = -1;
        this.mDragOutlineAnims[this.mDragOutlineCurrent].animateOut();
        this.mDragOutlineCurrent = (this.mDragOutlineCurrent + 1) % this.mDragOutlineAnims.length;
        revertTempState();
        setIsDragOverlapping(false);
    }

    void onDropChild(View view) {
        if (view != null) {
            ((LayoutParams) view.getLayoutParams()).dropped = true;
            view.requestLayout();
            markCellsAsOccupiedForView(view);
        }
    }

    public void cellToRect(int i, int i2, int i3, int i4, Rect rect) {
        int i5 = this.mCellWidth;
        int i6 = this.mCellHeight;
        int paddingLeft = getPaddingLeft() + (i * i5);
        int paddingTop = getPaddingTop() + (i2 * i6);
        rect.set(paddingLeft, paddingTop, (i3 * i5) + paddingLeft, (i4 * i6) + paddingTop);
    }

    public void markCellsAsOccupiedForView(View view) {
        if (view == null || view.getParent() != this.mShortcutsAndWidgets) {
            return;
        }
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        this.mOccupied.markCells(layoutParams.cellX, layoutParams.cellY, layoutParams.cellHSpan, layoutParams.cellVSpan, true);
    }

    public void markCellsAsUnoccupiedForView(View view) {
        if (view == null || view.getParent() != this.mShortcutsAndWidgets) {
            return;
        }
        LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
        this.mOccupied.markCells(layoutParams.cellX, layoutParams.cellY, layoutParams.cellHSpan, layoutParams.cellVSpan, false);
    }

    public int getDesiredWidth() {
        return getPaddingLeft() + getPaddingRight() + (this.mCountX * this.mCellWidth);
    }

    public int getDesiredHeight() {
        return getPaddingTop() + getPaddingBottom() + (this.mCountY * this.mCellHeight);
    }

    public boolean isOccupied(int i, int i2) {
        if (i < this.mCountX && i2 < this.mCountY) {
            return this.mOccupied.cells[i][i2];
        }
        throw new RuntimeException("Position exceeds the bound of this CellLayout");
    }

    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof LayoutParams;
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return new LayoutParams(layoutParams);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public boolean canReorder;

        @ViewDebug.ExportedProperty
        public int cellHSpan;

        @ViewDebug.ExportedProperty
        public int cellVSpan;

        @ViewDebug.ExportedProperty
        public int cellX;

        @ViewDebug.ExportedProperty
        public int cellY;
        boolean dropped;
        public boolean isLockedToGrid;
        public int tmpCellX;
        public int tmpCellY;
        public boolean useTmpCoords;

        @ViewDebug.ExportedProperty
        public int x;

        @ViewDebug.ExportedProperty
        public int y;

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellHSpan = 1;
            this.cellVSpan = 1;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((ViewGroup.MarginLayoutParams) layoutParams);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellX = layoutParams.cellX;
            this.cellY = layoutParams.cellY;
            this.cellHSpan = layoutParams.cellHSpan;
            this.cellVSpan = layoutParams.cellVSpan;
        }

        public LayoutParams(int i, int i2, int i3, int i4) {
            super(-1, -1);
            this.isLockedToGrid = true;
            this.canReorder = true;
            this.cellX = i;
            this.cellY = i2;
            this.cellHSpan = i3;
            this.cellVSpan = i4;
        }

        public void setup(int i, int i2, boolean z, int i3) {
            setup(i, i2, z, i3, 1.0f, 1.0f);
        }

        public void setup(int i, int i2, boolean z, int i3, float f, float f2) {
            if (this.isLockedToGrid) {
                int i4 = this.cellHSpan;
                int i5 = this.cellVSpan;
                int i6 = this.useTmpCoords ? this.tmpCellX : this.cellX;
                int i7 = this.useTmpCoords ? this.tmpCellY : this.cellY;
                if (z) {
                    i6 = (i3 - i6) - this.cellHSpan;
                }
                this.width = (int) ((((i4 * i) / f) - this.leftMargin) - this.rightMargin);
                this.height = (int) ((((i5 * i2) / f2) - this.topMargin) - this.bottomMargin);
                this.x = (i6 * i) + this.leftMargin;
                this.y = (i7 * i2) + this.topMargin;
            }
        }

        public String toString() {
            return "(" + this.cellX + ", " + this.cellY + ")";
        }

        public void setWidth(int i) {
            this.width = i;
        }

        public int getWidth() {
            return this.width;
        }

        public void setHeight(int i) {
            this.height = i;
        }

        public int getHeight() {
            return this.height;
        }

        public void setX(int i) {
            this.x = i;
        }

        public int getX() {
            return this.x;
        }

        public void setY(int i) {
            this.y = i;
        }

        public int getY() {
            return this.y;
        }
    }

    public static final class CellInfo extends CellAndSpan {
        public final View cell;
        final long container;
        final long screenId;

        public CellInfo(View view, ItemInfo itemInfo) {
            this.cellX = itemInfo.cellX;
            this.cellY = itemInfo.cellY;
            this.spanX = itemInfo.spanX;
            this.spanY = itemInfo.spanY;
            this.cell = view;
            this.screenId = itemInfo.screenId;
            this.container = itemInfo.container;
        }

        @Override // com.android.launcher3.util.CellAndSpan
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Cell[view=");
            sb.append(this.cell == null ? "null" : this.cell.getClass());
            sb.append(", x=");
            sb.append(this.cellX);
            sb.append(", y=");
            sb.append(this.cellY);
            sb.append("]");
            return sb.toString();
        }
    }

    public boolean hasReorderSolution(ItemInfo itemInfo) {
        int[] iArr = new int[2];
        int i = 0;
        int i2 = 0;
        while (i2 < getCountX()) {
            int i3 = i;
            while (i3 < getCountY()) {
                cellToPoint(i2, i3, iArr);
                int i4 = i3;
                if (findReorderSolution(iArr[i], iArr[1], itemInfo.minSpanX, itemInfo.minSpanY, itemInfo.spanX, itemInfo.spanY, this.mDirectionVector, null, true, new ItemConfiguration()).isSolution) {
                    return true;
                }
                i3 = i4 + 1;
                i = 0;
            }
            i2++;
            i = 0;
        }
        return false;
    }

    public boolean isRegionVacant(int i, int i2, int i3, int i4) {
        return this.mOccupied.isRegionVacant(i, i2, i3, i4);
    }
}
