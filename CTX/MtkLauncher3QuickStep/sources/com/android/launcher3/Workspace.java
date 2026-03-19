package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetHost;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.LauncherStateManager;
import com.android.launcher3.Workspace;
import com.android.launcher3.accessibility.AccessibleDragListenerAdapter;
import com.android.launcher3.accessibility.WorkspaceAccessibilityHelper;
import com.android.launcher3.anim.AnimatorSetBuilder;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.badge.FolderBadgeInfo;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.dragndrop.DragView;
import com.android.launcher3.dragndrop.SpringLoadedDragController;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIcon;
import com.android.launcher3.folder.PreviewBackground;
import com.android.launcher3.graphics.DragPreviewProvider;
import com.android.launcher3.graphics.PreloadIconDrawable;
import com.android.launcher3.pageindicators.WorkspacePageIndicator;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.shortcuts.ShortcutDragPreviewProvider;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.touch.WorkspaceTouchListener;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.ItemInfoMatcher;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.PackageUserKey;
import com.android.launcher3.util.WallpaperOffsetInterpolator;
import com.android.launcher3.widget.LauncherAppWidgetHostView;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PendingAppWidgetHostView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Workspace extends PagedView<WorkspacePageIndicator> implements DropTarget, DragSource, View.OnTouchListener, DragController.DragListener, Insettable, LauncherStateManager.StateHandler {
    private static final int ADJACENT_SCREEN_DROP_DURATION = 300;
    private static final float ALLOW_DROP_TRANSITION_PROGRESS = 0.25f;
    public static final int ANIMATE_INTO_POSITION_AND_DISAPPEAR = 0;
    public static final int ANIMATE_INTO_POSITION_AND_REMAIN = 1;
    public static final int ANIMATE_INTO_POSITION_AND_RESIZE = 2;
    public static final int CANCEL_TWO_STAGE_WIDGET_DROP_ANIMATION = 4;
    public static final int COMPLETE_TWO_STAGE_WIDGET_DROP_ANIMATION = 3;
    private static final int DEFAULT_PAGE = 0;
    private static final int DRAG_MODE_ADD_TO_FOLDER = 2;
    private static final int DRAG_MODE_CREATE_FOLDER = 1;
    private static final int DRAG_MODE_NONE = 0;
    private static final int DRAG_MODE_REORDER = 3;
    private static final boolean ENFORCE_DRAG_EVENT_ORDER = false;
    public static final long EXTRA_EMPTY_SCREEN_ID = -201;
    private static final int FADE_EMPTY_SCREEN_DURATION = 150;
    private static final float FINISHED_SWITCHING_STATE_TRANSITION_PROGRESS = 0.5f;
    public static final long FIRST_SCREEN_ID = 0;
    private static final int FOLDER_CREATION_TIMEOUT = 0;
    private static final boolean MAP_NO_RECURSE = false;
    private static final boolean MAP_RECURSE = true;
    static final float MAX_SWIPE_ANGLE = 1.0471976f;
    public static final int REORDER_TIMEOUT = 650;
    private static final int SNAP_OFF_EMPTY_SCREEN_DURATION = 400;
    static final float START_DAMPING_TOUCH_SLOP_ANGLE = 0.5235988f;
    private static final String TAG = "Launcher.Workspace";
    static final float TOUCH_SLOP_DAMPING_FACTOR = 4.0f;
    private boolean mAddToExistingFolderOnDrop;
    boolean mChildrenLayersEnabled;
    private boolean mCreateUserFolderOnDrop;
    private float mCurrentScale;
    boolean mDeferRemoveExtraEmptyScreen;
    DragController mDragController;
    private CellLayout.CellInfo mDragInfo;
    private int mDragMode;
    private FolderIcon mDragOverFolderIcon;
    private int mDragOverX;
    private int mDragOverY;
    private CellLayout mDragOverlappingLayout;
    private ShortcutAndWidgetContainer mDragSourceInternal;
    CellLayout mDragTargetLayout;
    float[] mDragViewVisualCenter;
    private CellLayout mDropToLayout;
    private PreviewBackground mFolderCreateBg;
    private final Alarm mFolderCreationAlarm;
    private boolean mForceDrawAdjacentPages;
    private boolean mIsSwitchingState;
    float mLastOverlayScroll;
    int mLastReorderX;
    int mLastReorderY;
    final Launcher mLauncher;
    Launcher.LauncherOverlay mLauncherOverlay;
    private LayoutTransition mLayoutTransition;
    private float mMaxDistanceForFolderCreation;
    private Runnable mOnOverlayHiddenCallback;
    private DragPreviewProvider mOutlineProvider;
    boolean mOverlayShown;
    private float mOverlayTranslation;
    Runnable mRemoveEmptyScreenRunnable;
    private final Alarm mReorderAlarm;
    private final ArrayList<Integer> mRestoredPages;
    private SparseArray<Parcelable> mSavedStates;
    final ArrayList<Long> mScreenOrder;
    boolean mScrollInteractionBegan;
    private SpringLoadedDragController mSpringLoadedDragController;
    boolean mStartedSendingScrollEvents;
    private final WorkspaceStateTransitionAnimation mStateTransitionAnimation;
    private boolean mStripScreensOnPageStopMoving;
    int[] mTargetCell;
    private final float[] mTempTouchCoordinates;
    private final int[] mTempXY;
    private float mTransitionProgress;
    private boolean mUnlockWallpaperFromDefaultPageOnLayout;
    final WallpaperManager mWallpaperManager;
    final WallpaperOffsetInterpolator mWallpaperOffset;
    private boolean mWorkspaceFadeInAdjacentScreens;
    final LongArrayMap<CellLayout> mWorkspaceScreens;
    private float mXDown;
    private float mYDown;

    public interface ItemOperator {
        boolean evaluate(ItemInfo itemInfo, View view);
    }

    public Workspace(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public Workspace(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mWorkspaceScreens = new LongArrayMap<>();
        this.mScreenOrder = new ArrayList<>();
        this.mDeferRemoveExtraEmptyScreen = false;
        this.mTargetCell = new int[2];
        this.mDragOverX = -1;
        this.mDragOverY = -1;
        this.mDragTargetLayout = null;
        this.mDragOverlappingLayout = null;
        this.mDropToLayout = null;
        this.mTempXY = new int[2];
        this.mDragViewVisualCenter = new float[2];
        this.mTempTouchCoordinates = new float[2];
        this.mIsSwitchingState = false;
        this.mChildrenLayersEnabled = true;
        this.mStripScreensOnPageStopMoving = false;
        this.mOutlineProvider = null;
        this.mFolderCreationAlarm = new Alarm();
        this.mReorderAlarm = new Alarm();
        this.mDragOverFolderIcon = null;
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mDragMode = 0;
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
        this.mRestoredPages = new ArrayList<>();
        this.mLastOverlayScroll = 0.0f;
        this.mOverlayShown = false;
        this.mForceDrawAdjacentPages = false;
        this.mLauncher = Launcher.getLauncher(context);
        this.mStateTransitionAnimation = new WorkspaceStateTransitionAnimation(this.mLauncher, this);
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        this.mWallpaperOffset = new WallpaperOffsetInterpolator(this);
        setHapticFeedbackEnabled(false);
        initWorkspace();
        setMotionEventSplittingEnabled(true);
        setOnTouchListener(new WorkspaceTouchListener(this.mLauncher, this));
    }

    @Override
    public void setInsets(Rect rect) {
        this.mInsets.set(rect);
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        this.mMaxDistanceForFolderCreation = 0.55f * deviceProfile.iconSizePx;
        this.mWorkspaceFadeInAdjacentScreens = deviceProfile.shouldFadeAdjacentWorkspaceScreens();
        Rect rect2 = deviceProfile.workspacePadding;
        setPadding(rect2.left, rect2.top, rect2.right, rect2.bottom);
        if (deviceProfile.shouldFadeAdjacentWorkspaceScreens()) {
            setPageSpacing(deviceProfile.defaultPageSpacingPx);
        } else {
            setPageSpacing(Math.max(deviceProfile.defaultPageSpacingPx, rect2.left + 1));
        }
        int i = deviceProfile.cellLayoutPaddingLeftRightPx;
        int i2 = deviceProfile.cellLayoutBottomPaddingPx;
        for (int size = this.mWorkspaceScreens.size() - 1; size >= 0; size--) {
            this.mWorkspaceScreens.valueAt(size).setPadding(i, 0, i, i2);
        }
    }

    public int[] estimateItemSize(ItemInfo itemInfo) {
        int[] iArr = new int[2];
        if (getChildCount() > 0) {
            CellLayout cellLayout = (CellLayout) getChildAt(0);
            boolean z = itemInfo.itemType == 4;
            Rect rectEstimateItemPosition = estimateItemPosition(cellLayout, 0, 0, itemInfo.spanX, itemInfo.spanY);
            float fShrinkRect = 1.0f;
            if (z) {
                DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
                fShrinkRect = Utilities.shrinkRect(rectEstimateItemPosition, deviceProfile.appWidgetScale.x, deviceProfile.appWidgetScale.y);
            }
            iArr[0] = rectEstimateItemPosition.width();
            iArr[1] = rectEstimateItemPosition.height();
            if (z) {
                iArr[0] = (int) (iArr[0] / fShrinkRect);
                iArr[1] = (int) (iArr[1] / fShrinkRect);
            }
            return iArr;
        }
        iArr[0] = Integer.MAX_VALUE;
        iArr[1] = Integer.MAX_VALUE;
        return iArr;
    }

    public float getWallpaperOffsetForCenterPage() {
        return this.mWallpaperOffset.wallpaperOffsetForScroll(getScrollForPage(getPageNearestToCenterOfScreen()));
    }

    public Rect estimateItemPosition(CellLayout cellLayout, int i, int i2, int i3, int i4) {
        Rect rect = new Rect();
        cellLayout.cellToRect(i, i2, i3, i4, rect);
        return rect;
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        if (this.mDragInfo != null && this.mDragInfo.cell != null) {
            ((CellLayout) this.mDragInfo.cell.getParent().getParent()).markCellsAsUnoccupiedForView(this.mDragInfo.cell);
        }
        if (this.mOutlineProvider != null && dragObject.dragView != null) {
            this.mOutlineProvider.generateDragOutline(dragObject.dragView.getPreviewBitmap());
        }
        updateChildrenLayersEnabled();
        if (!dragOptions.isAccessibleDrag || dragObject.dragSource == this) {
            this.mDeferRemoveExtraEmptyScreen = false;
            addExtraEmptyScreenOnDrag();
            if (dragObject.dragInfo.itemType == 4 && dragObject.dragSource != this) {
                int pageNearestToCenterOfScreen = getPageNearestToCenterOfScreen();
                while (true) {
                    if (pageNearestToCenterOfScreen >= getPageCount()) {
                        break;
                    }
                    if (!((CellLayout) getPageAt(pageNearestToCenterOfScreen)).hasReorderSolution(dragObject.dragInfo)) {
                        pageNearestToCenterOfScreen++;
                    } else {
                        setCurrentPage(pageNearestToCenterOfScreen);
                        break;
                    }
                }
            }
        }
        this.mLauncher.getStateManager().goToState(LauncherState.SPRING_LOADED);
    }

    public void deferRemoveExtraEmptyScreen() {
        this.mDeferRemoveExtraEmptyScreen = true;
    }

    @Override
    public void onDragEnd() {
        if (!this.mDeferRemoveExtraEmptyScreen) {
            removeExtraEmptyScreen(true, this.mDragSourceInternal != null);
        }
        updateChildrenLayersEnabled();
        this.mDragInfo = null;
        this.mOutlineProvider = null;
        this.mDragSourceInternal = null;
    }

    protected void initWorkspace() {
        this.mCurrentPage = 0;
        setClipToPadding(false);
        setupLayoutTransition();
        setWallpaperDimension();
    }

    private void setupLayoutTransition() {
        this.mLayoutTransition = new LayoutTransition();
        this.mLayoutTransition.enableTransitionType(3);
        this.mLayoutTransition.enableTransitionType(1);
        this.mLayoutTransition.disableTransitionType(2);
        this.mLayoutTransition.disableTransitionType(0);
        setLayoutTransition(this.mLayoutTransition);
    }

    void enableLayoutTransitions() {
        setLayoutTransition(this.mLayoutTransition);
    }

    void disableLayoutTransitions() {
        setLayoutTransition(null);
    }

    @Override
    public void onViewAdded(View view) {
        if (!(view instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cellLayout = (CellLayout) view;
        cellLayout.setOnInterceptTouchListener(this);
        cellLayout.setImportantForAccessibility(2);
        super.onViewAdded(view);
    }

    public boolean isTouchActive() {
        return this.mTouchState != 0;
    }

    public void bindAndInitFirstWorkspaceScreen(View view) {
    }

    public void removeAllWorkspaceScreens() {
        disableLayoutTransitions();
        View viewFindViewById = findViewById(R.id.search_container_workspace);
        if (viewFindViewById != null) {
            ((ViewGroup) viewFindViewById.getParent()).removeView(viewFindViewById);
        }
        removeFolderListeners();
        removeAllViews();
        this.mScreenOrder.clear();
        this.mWorkspaceScreens.clear();
        bindAndInitFirstWorkspaceScreen(viewFindViewById);
        enableLayoutTransitions();
    }

    public void insertNewWorkspaceScreenBeforeEmptyScreen(long j) {
        int iIndexOf = this.mScreenOrder.indexOf(-201L);
        if (iIndexOf < 0) {
            iIndexOf = this.mScreenOrder.size();
        }
        insertNewWorkspaceScreen(j, iIndexOf);
    }

    public void insertNewWorkspaceScreen(long j) {
        insertNewWorkspaceScreen(j, getChildCount());
    }

    public CellLayout insertNewWorkspaceScreen(long j, int i) {
        if (this.mWorkspaceScreens.containsKey(j)) {
            throw new RuntimeException("Screen id " + j + " already exists!");
        }
        CellLayout cellLayout = (CellLayout) LayoutInflater.from(getContext()).inflate(R.layout.workspace_screen, (ViewGroup) this, false);
        int i2 = this.mLauncher.getDeviceProfile().cellLayoutPaddingLeftRightPx;
        cellLayout.setPadding(i2, 0, i2, this.mLauncher.getDeviceProfile().cellLayoutBottomPaddingPx);
        this.mWorkspaceScreens.put(j, cellLayout);
        this.mScreenOrder.add(i, Long.valueOf(j));
        addView(cellLayout, i);
        this.mStateTransitionAnimation.applyChildState(this.mLauncher.getStateManager().getState(), cellLayout, i);
        if (this.mLauncher.getAccessibilityDelegate().isInAccessibleDrag()) {
            cellLayout.enableAccessibleDrag(true, 2);
        }
        return cellLayout;
    }

    public void addExtraEmptyScreenOnDrag() {
        boolean z;
        this.mRemoveEmptyScreenRunnable = null;
        boolean z2 = false;
        if (this.mDragSourceInternal != null) {
            z = this.mDragSourceInternal.getChildCount() == 1;
            if (indexOfChild((CellLayout) this.mDragSourceInternal.getParent()) == getChildCount() - 1) {
                z2 = true;
            }
        } else {
            z = false;
        }
        if ((!z || !z2) && !this.mWorkspaceScreens.containsKey(-201L)) {
            insertNewWorkspaceScreen(-201L);
        }
    }

    public boolean addExtraEmptyScreen() {
        if (!this.mWorkspaceScreens.containsKey(-201L)) {
            insertNewWorkspaceScreen(-201L);
            return true;
        }
        return false;
    }

    private void convertFinalScreenToEmptyScreenIfNecessary() {
        if (this.mLauncher.isWorkspaceLoading() || hasExtraEmptyScreen() || this.mScreenOrder.size() == 0) {
            return;
        }
        long jLongValue = this.mScreenOrder.get(this.mScreenOrder.size() - 1).longValue();
        CellLayout cellLayout = this.mWorkspaceScreens.get(jLongValue);
        if (cellLayout.getShortcutsAndWidgets().getChildCount() == 0 && !cellLayout.isDropPending()) {
            this.mWorkspaceScreens.remove(jLongValue);
            this.mScreenOrder.remove(Long.valueOf(jLongValue));
            this.mWorkspaceScreens.put(-201L, cellLayout);
            this.mScreenOrder.add(-201L);
            LauncherModel.updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        }
    }

    public void removeExtraEmptyScreen(boolean z, boolean z2) {
        removeExtraEmptyScreenDelayed(z, null, 0, z2);
    }

    public void removeExtraEmptyScreenDelayed(final boolean z, final Runnable runnable, int i, final boolean z2) {
        if (this.mLauncher.isWorkspaceLoading()) {
            return;
        }
        if (i > 0) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    Workspace.this.removeExtraEmptyScreenDelayed(z, runnable, 0, z2);
                }
            }, i);
            return;
        }
        convertFinalScreenToEmptyScreenIfNecessary();
        if (hasExtraEmptyScreen()) {
            if (getNextPage() == this.mScreenOrder.indexOf(-201L)) {
                snapToPage(getNextPage() - 1, SNAP_OFF_EMPTY_SCREEN_DURATION);
                fadeAndRemoveEmptyScreen(SNAP_OFF_EMPTY_SCREEN_DURATION, 150, runnable, z2);
                return;
            } else {
                snapToPage(getNextPage(), 0);
                fadeAndRemoveEmptyScreen(0, 150, runnable, z2);
                return;
            }
        }
        if (z2) {
            stripEmptyScreens();
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    private void fadeAndRemoveEmptyScreen(int i, int i2, final Runnable runnable, final boolean z) {
        PropertyValuesHolder propertyValuesHolderOfFloat = PropertyValuesHolder.ofFloat("alpha", 0.0f);
        PropertyValuesHolder propertyValuesHolderOfFloat2 = PropertyValuesHolder.ofFloat("backgroundAlpha", 0.0f);
        final CellLayout cellLayout = this.mWorkspaceScreens.get(-201L);
        this.mRemoveEmptyScreenRunnable = new Runnable() {
            @Override
            public void run() {
                if (Workspace.this.hasExtraEmptyScreen()) {
                    Workspace.this.mWorkspaceScreens.remove(-201L);
                    Workspace.this.mScreenOrder.remove((Object) (-201L));
                    Workspace.this.removeView(cellLayout);
                    if (z) {
                        Workspace.this.stripEmptyScreens();
                    }
                    Workspace.this.showPageIndicatorAtCurrentScroll();
                }
            }
        };
        ObjectAnimator objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(cellLayout, propertyValuesHolderOfFloat, propertyValuesHolderOfFloat2);
        objectAnimatorOfPropertyValuesHolder.setDuration(i2);
        objectAnimatorOfPropertyValuesHolder.setStartDelay(i);
        objectAnimatorOfPropertyValuesHolder.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                if (Workspace.this.mRemoveEmptyScreenRunnable != null) {
                    Workspace.this.mRemoveEmptyScreenRunnable.run();
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
        objectAnimatorOfPropertyValuesHolder.start();
    }

    public boolean hasExtraEmptyScreen() {
        return this.mWorkspaceScreens.containsKey(-201L) && getChildCount() > 1;
    }

    public long commitExtraEmptyScreen() {
        if (this.mLauncher.isWorkspaceLoading()) {
            return -1L;
        }
        CellLayout cellLayout = this.mWorkspaceScreens.get(-201L);
        this.mWorkspaceScreens.remove(-201L);
        this.mScreenOrder.remove((Object) (-201L));
        long j = LauncherSettings.Settings.call(getContext().getContentResolver(), LauncherSettings.Settings.METHOD_NEW_SCREEN_ID).getLong(LauncherSettings.Settings.EXTRA_VALUE);
        this.mWorkspaceScreens.put(j, cellLayout);
        this.mScreenOrder.add(Long.valueOf(j));
        LauncherModel.updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        return j;
    }

    public CellLayout getScreenWithId(long j) {
        return this.mWorkspaceScreens.get(j);
    }

    public long getIdForScreen(CellLayout cellLayout) {
        int iIndexOfValue = this.mWorkspaceScreens.indexOfValue(cellLayout);
        if (iIndexOfValue != -1) {
            return this.mWorkspaceScreens.keyAt(iIndexOfValue);
        }
        return -1L;
    }

    public int getPageIndexForScreenId(long j) {
        return indexOfChild(this.mWorkspaceScreens.get(j));
    }

    public long getScreenIdForPageIndex(int i) {
        if (i >= 0 && i < this.mScreenOrder.size()) {
            return this.mScreenOrder.get(i).longValue();
        }
        return -1L;
    }

    public ArrayList<Long> getScreenOrder() {
        return this.mScreenOrder;
    }

    public void stripEmptyScreens() {
        if (this.mLauncher.isWorkspaceLoading()) {
            return;
        }
        if (isPageInTransition()) {
            this.mStripScreensOnPageStopMoving = true;
            return;
        }
        int nextPage = getNextPage();
        ArrayList<Long> arrayList = new ArrayList();
        int size = this.mWorkspaceScreens.size();
        for (int i = 0; i < size; i++) {
            long jKeyAt = this.mWorkspaceScreens.keyAt(i);
            if (this.mWorkspaceScreens.valueAt(i).getShortcutsAndWidgets().getChildCount() == 0) {
                arrayList.add(Long.valueOf(jKeyAt));
            }
        }
        boolean zIsInAccessibleDrag = this.mLauncher.getAccessibilityDelegate().isInAccessibleDrag();
        int i2 = 0;
        for (Long l : arrayList) {
            CellLayout cellLayout = this.mWorkspaceScreens.get(l.longValue());
            this.mWorkspaceScreens.remove(l.longValue());
            this.mScreenOrder.remove(l);
            if (getChildCount() > 1) {
                if (indexOfChild(cellLayout) < nextPage) {
                    i2++;
                }
                if (zIsInAccessibleDrag) {
                    cellLayout.enableAccessibleDrag(false, 2);
                }
                removeView(cellLayout);
            } else {
                this.mRemoveEmptyScreenRunnable = null;
                this.mWorkspaceScreens.put(-201L, cellLayout);
                this.mScreenOrder.add(-201L);
            }
        }
        if (!arrayList.isEmpty()) {
            LauncherModel.updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        }
        if (i2 >= 0) {
            setCurrentPage(nextPage - i2);
        }
    }

    public void addInScreenFromBind(View view, ItemInfo itemInfo) {
        int i;
        int cellYFromOrder;
        int i2 = itemInfo.cellX;
        int i3 = itemInfo.cellY;
        if (itemInfo.container == -101) {
            int i4 = (int) itemInfo.screenId;
            int cellXFromOrder = this.mLauncher.getHotseat().getCellXFromOrder(i4);
            cellYFromOrder = this.mLauncher.getHotseat().getCellYFromOrder(i4);
            i = cellXFromOrder;
        } else {
            i = i2;
            cellYFromOrder = i3;
        }
        addInScreen(view, itemInfo.container, itemInfo.screenId, i, cellYFromOrder, itemInfo.spanX, itemInfo.spanY);
    }

    public void addInScreen(View view, ItemInfo itemInfo) {
        addInScreen(view, itemInfo.container, itemInfo.screenId, itemInfo.cellX, itemInfo.cellY, itemInfo.spanX, itemInfo.spanY);
    }

    private void addInScreen(View view, long j, long j2, int i, int i2, int i3, int i4) {
        CellLayout screenWithId;
        CellLayout.LayoutParams layoutParams;
        if (j == -100 && getScreenWithId(j2) == null) {
            Log.e(TAG, "Skipping child, screenId " + j2 + " not found");
            new Throwable().printStackTrace();
            return;
        }
        if (j2 == -201) {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }
        if (j == -101) {
            screenWithId = this.mLauncher.getHotseat().getLayout();
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(false);
            }
        } else {
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(true);
            }
            screenWithId = getScreenWithId(j2);
        }
        CellLayout cellLayout = screenWithId;
        ViewGroup.LayoutParams layoutParams2 = view.getLayoutParams();
        if (layoutParams2 == null || !(layoutParams2 instanceof CellLayout.LayoutParams)) {
            layoutParams = new CellLayout.LayoutParams(i, i2, i3, i4);
        } else {
            layoutParams = (CellLayout.LayoutParams) layoutParams2;
            layoutParams.cellX = i;
            layoutParams.cellY = i2;
            layoutParams.cellHSpan = i3;
            layoutParams.cellVSpan = i4;
        }
        if (i3 < 0 && i4 < 0) {
            layoutParams.isLockedToGrid = false;
        }
        if (!cellLayout.addViewToCellLayout(view, -1, this.mLauncher.getViewIdForItem((ItemInfo) view.getTag()), layoutParams, !(view instanceof Folder))) {
            Log.e(TAG, "Failed to add to item at (" + layoutParams.cellX + "," + layoutParams.cellY + ") to CellLayout");
        }
        view.setHapticFeedbackEnabled(false);
        view.setOnLongClickListener(ItemLongClickListener.INSTANCE_WORKSPACE);
        if (view instanceof DropTarget) {
            this.mDragController.addDropTarget((DropTarget) view);
        }
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return shouldConsumeTouch(view);
    }

    private boolean shouldConsumeTouch(View view) {
        return (workspaceIconsCanBeDragged() && (workspaceInModalState() || indexOfChild(view) == this.mCurrentPage)) ? false : true;
    }

    public boolean isSwitchingState() {
        return this.mIsSwitchingState;
    }

    public boolean isFinishedSwitchingState() {
        return !this.mIsSwitchingState || this.mTransitionProgress > 0.5f;
    }

    @Override
    public boolean dispatchUnhandledMove(View view, int i) {
        if (workspaceInModalState() || !isFinishedSwitchingState()) {
            return false;
        }
        return super.dispatchUnhandledMove(view, i);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == 0) {
            this.mXDown = motionEvent.getX();
            this.mYDown = motionEvent.getY();
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    protected void determineScrollingStart(MotionEvent motionEvent) {
        if (isFinishedSwitchingState()) {
            float fAbs = Math.abs(motionEvent.getX() - this.mXDown);
            float fAbs2 = Math.abs(motionEvent.getY() - this.mYDown);
            if (Float.compare(fAbs, 0.0f) == 0) {
                return;
            }
            float fAtan = (float) Math.atan(fAbs2 / fAbs);
            if (fAbs > this.mTouchSlop || fAbs2 > this.mTouchSlop) {
                cancelCurrentPageLongPress();
            }
            if (fAtan > MAX_SWIPE_ANGLE) {
                return;
            }
            if (fAtan > START_DAMPING_TOUCH_SLOP_ANGLE) {
                super.determineScrollingStart(motionEvent, 1.0f + (TOUCH_SLOP_DAMPING_FACTOR * ((float) Math.sqrt((fAtan - START_DAMPING_TOUCH_SLOP_ANGLE) / START_DAMPING_TOUCH_SLOP_ANGLE))));
            } else {
                super.determineScrollingStart(motionEvent);
            }
        }
    }

    @Override
    protected void onPageBeginTransition() {
        super.onPageBeginTransition();
        updateChildrenLayersEnabled();
    }

    @Override
    protected void onPageEndTransition() {
        super.onPageEndTransition();
        updateChildrenLayersEnabled();
        if (this.mDragController.isDragging() && workspaceInModalState()) {
            this.mDragController.forceTouchMove();
        }
        if (this.mStripScreensOnPageStopMoving) {
            stripEmptyScreens();
            this.mStripScreensOnPageStopMoving = false;
        }
    }

    @Override
    protected void onScrollInteractionBegin() {
        super.onScrollInteractionEnd();
        this.mScrollInteractionBegan = true;
    }

    @Override
    protected void onScrollInteractionEnd() {
        super.onScrollInteractionEnd();
        this.mScrollInteractionBegan = false;
        if (this.mStartedSendingScrollEvents) {
            this.mStartedSendingScrollEvents = false;
            this.mLauncherOverlay.onScrollInteractionEnd();
        }
    }

    public void setLauncherOverlay(Launcher.LauncherOverlay launcherOverlay) {
        this.mLauncherOverlay = launcherOverlay;
        this.mStartedSendingScrollEvents = false;
        onOverlayScrollChanged(0.0f);
    }

    private boolean isScrollingOverlay() {
        return this.mLauncherOverlay != null && ((this.mIsRtl && getUnboundedScrollX() > this.mMaxScrollX) || (!this.mIsRtl && getUnboundedScrollX() < 0));
    }

    @Override
    protected void snapToDestination() {
        if (isScrollingOverlay()) {
            this.mWasInOverscroll = false;
            snapToPageImmediately(0);
        } else {
            super.snapToDestination();
        }
    }

    @Override
    protected void onScrollChanged(int i, int i2, int i3, int i4) {
        super.onScrollChanged(i, i2, i3, i4);
        if (!(this.mIsSwitchingState || (getLayoutTransition() != null && getLayoutTransition().isRunning()))) {
            showPageIndicatorAtCurrentScroll();
        }
        updatePageAlphaValues();
        enableHwLayersOnVisiblePages();
    }

    public void showPageIndicatorAtCurrentScroll() {
        if (this.mPageIndicator != 0) {
            ((WorkspacePageIndicator) this.mPageIndicator).setScroll(getScrollX(), computeMaxScrollX());
        }
    }

    @Override
    protected void overScroll(float f) {
        boolean z = false;
        boolean z2 = this.mLauncherOverlay != null && ((f <= 0.0f && !this.mIsRtl) || (f >= 0.0f && this.mIsRtl));
        if (this.mLauncherOverlay != null && this.mLastOverlayScroll != 0.0f && ((f >= 0.0f && !this.mIsRtl) || (f <= 0.0f && this.mIsRtl))) {
            z = true;
        }
        if (z2) {
            if (!this.mStartedSendingScrollEvents && this.mScrollInteractionBegan) {
                this.mStartedSendingScrollEvents = true;
                this.mLauncherOverlay.onScrollInteractionBegin();
            }
            this.mLastOverlayScroll = Math.abs(f / getMeasuredWidth());
            this.mLauncherOverlay.onScrollChange(this.mLastOverlayScroll, this.mIsRtl);
        } else {
            dampedOverScroll(f);
        }
        if (z) {
            this.mLauncherOverlay.onScrollChange(0.0f, this.mIsRtl);
        }
    }

    @Override
    protected boolean shouldFlingForVelocity(int i) {
        return Float.compare(Math.abs(this.mOverlayTranslation), 0.0f) == 0 && super.shouldFlingForVelocity(i);
    }

    public void onOverlayScrollChanged(float f) {
        if (Float.compare(f, 1.0f) == 0) {
            if (!this.mOverlayShown) {
                this.mLauncher.getUserEventDispatcher().logActionOnContainer(3, 3, 1, 0);
            }
            this.mOverlayShown = true;
        } else if (Float.compare(f, 0.0f) == 0) {
            if (this.mOverlayShown) {
                this.mLauncher.getUserEventDispatcher().logActionOnContainer(3, 4, 1, -1);
            } else if (Float.compare(this.mOverlayTranslation, 0.0f) != 0) {
                announcePageForAccessibility();
            }
            this.mOverlayShown = false;
            tryRunOverlayCallback();
        }
        float fMin = Math.min(1.0f, Math.max(f - 0.0f, 0.0f) / 1.0f);
        float interpolation = 1.0f - Interpolators.DEACCEL_3.getInterpolation(fMin);
        float measuredWidth = this.mLauncher.getDragLayer().getMeasuredWidth() * fMin;
        if (this.mIsRtl) {
            measuredWidth = -measuredWidth;
        }
        this.mOverlayTranslation = measuredWidth;
        this.mLauncher.getDragLayer().setTranslationX(measuredWidth);
        this.mLauncher.getDragLayer().getAlphaProperty(0).setValue(interpolation);
    }

    private boolean tryRunOverlayCallback() {
        if (this.mOnOverlayHiddenCallback == null) {
            return true;
        }
        if (this.mOverlayShown || !hasWindowFocus()) {
            return false;
        }
        this.mOnOverlayHiddenCallback.run();
        this.mOnOverlayHiddenCallback = null;
        return true;
    }

    public boolean runOnOverlayHidden(final Runnable runnable) {
        if (this.mOnOverlayHiddenCallback == null) {
            this.mOnOverlayHiddenCallback = runnable;
        } else {
            final Runnable runnable2 = this.mOnOverlayHiddenCallback;
            this.mOnOverlayHiddenCallback = new Runnable() {
                @Override
                public final void run() {
                    Workspace.lambda$runOnOverlayHidden$0(runnable2, runnable);
                }
            };
        }
        if (!tryRunOverlayCallback()) {
            final ViewTreeObserver viewTreeObserver = getViewTreeObserver();
            if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
                    @Override
                    public void onWindowFocusChanged(boolean z) {
                        if (Workspace.this.tryRunOverlayCallback() && viewTreeObserver.isAlive()) {
                            viewTreeObserver.removeOnWindowFocusChangeListener(this);
                        }
                    }
                });
                return true;
            }
            return true;
        }
        return false;
    }

    static void lambda$runOnOverlayHidden$0(Runnable runnable, Runnable runnable2) {
        runnable.run();
        runnable2.run();
    }

    @Override
    protected void notifyPageSwitchListener(int i) {
        super.notifyPageSwitchListener(i);
        if (i != this.mCurrentPage) {
            this.mLauncher.getUserEventDispatcher().logActionOnContainer(3, i < this.mCurrentPage ? 4 : 3, 1, i);
        }
    }

    protected void setWallpaperDimension() {
        Utilities.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                Point point = LauncherAppState.getIDP(Workspace.this.getContext()).defaultWallpaperSize;
                if (point.x != Workspace.this.mWallpaperManager.getDesiredMinimumWidth() || point.y != Workspace.this.mWallpaperManager.getDesiredMinimumHeight()) {
                    Workspace.this.mWallpaperManager.suggestDesiredDimensions(point.x, point.y);
                }
            }
        });
    }

    public void lockWallpaperToDefaultPage() {
        this.mWallpaperOffset.setLockToDefaultPage(true);
    }

    public void unlockWallpaperFromDefaultPageOnNextLayout() {
        if (this.mWallpaperOffset.isLockedToDefaultPage()) {
            this.mUnlockWallpaperFromDefaultPageOnLayout = true;
            requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        this.mWallpaperOffset.syncWithScroll();
    }

    public void computeScrollWithoutInvalidation() {
        computeScrollHelper(false);
    }

    @Override
    protected void determineScrollingStart(MotionEvent motionEvent, float f) {
        if (!isSwitchingState()) {
            super.determineScrollingStart(motionEvent, f);
        }
    }

    @Override
    public void announceForAccessibility(CharSequence charSequence) {
        if (!this.mLauncher.isInState(LauncherState.ALL_APPS)) {
            super.announceForAccessibility(charSequence);
        }
    }

    public void showOutlinesTemporarily() {
        if (!this.mIsPageInTransition && !isTouchActive()) {
            snapToPage(this.mCurrentPage);
        }
    }

    private void updatePageAlphaValues() {
        int i;
        if (!workspaceInModalState() && !this.mIsSwitchingState && !this.mDragController.isDragging()) {
            int scrollX = getScrollX() + (getMeasuredWidth() / 2);
            for (int i2 = 0; i2 < getChildCount(); i2++) {
                CellLayout cellLayout = (CellLayout) getChildAt(i2);
                if (cellLayout != null) {
                    float fAbs = 1.0f - Math.abs(getScrollProgress(scrollX, cellLayout, i2));
                    if (this.mWorkspaceFadeInAdjacentScreens) {
                        cellLayout.getShortcutsAndWidgets().setAlpha(fAbs);
                    } else {
                        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
                        if (fAbs > 0.0f) {
                            i = 0;
                        } else {
                            i = 4;
                        }
                        shortcutsAndWidgets.setImportantForAccessibility(i);
                    }
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IBinder windowToken = getWindowToken();
        this.mWallpaperOffset.setWindowToken(windowToken);
        computeScroll();
        this.mDragController.setWindowToken(windowToken);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mWallpaperOffset.setWindowToken(null);
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.mUnlockWallpaperFromDefaultPageOnLayout) {
            this.mWallpaperOffset.setLockToDefaultPage(false);
            this.mUnlockWallpaperFromDefaultPageOnLayout = false;
        }
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            this.mWallpaperOffset.syncWithScroll();
            this.mWallpaperOffset.jumpToFinal();
        }
        super.onLayout(z, i, i2, i3, i4);
        updatePageAlphaValues();
    }

    @Override
    public int getDescendantFocusability() {
        if (workspaceInModalState()) {
            return 393216;
        }
        return super.getDescendantFocusability();
    }

    private boolean workspaceInModalState() {
        return !this.mLauncher.isInState(LauncherState.NORMAL);
    }

    public boolean workspaceIconsCanBeDragged() {
        return this.mLauncher.getStateManager().getState().workspaceIconsCanBeDragged;
    }

    private void updateChildrenLayersEnabled() {
        boolean z = this.mIsSwitchingState || isPageInTransition();
        if (z != this.mChildrenLayersEnabled) {
            this.mChildrenLayersEnabled = z;
            if (this.mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
                return;
            }
            for (int i = 0; i < getPageCount(); i++) {
                ((CellLayout) getChildAt(i)).enableHardwareLayer(false);
            }
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (this.mChildrenLayersEnabled) {
            int childCount = getChildCount();
            int[] visibleChildrenRange = getVisibleChildrenRange();
            int iBoundToRange = visibleChildrenRange[0];
            int iBoundToRange2 = visibleChildrenRange[1];
            if (this.mForceDrawAdjacentPages) {
                iBoundToRange = Utilities.boundToRange(getCurrentPage() - 1, 0, iBoundToRange2);
                iBoundToRange2 = Utilities.boundToRange(getCurrentPage() + 1, iBoundToRange, getPageCount() - 1);
            }
            if (iBoundToRange == iBoundToRange2) {
                if (iBoundToRange2 < childCount - 1) {
                    iBoundToRange2++;
                } else if (iBoundToRange > 0) {
                    iBoundToRange--;
                }
            }
            int i = 0;
            while (i < childCount) {
                ((CellLayout) getPageAt(i)).enableHardwareLayer(iBoundToRange <= i && i <= iBoundToRange2);
                i++;
            }
        }
    }

    public void onWallpaperTap(MotionEvent motionEvent) {
        int[] iArr = this.mTempXY;
        getLocationOnScreen(iArr);
        int actionIndex = motionEvent.getActionIndex();
        iArr[0] = iArr[0] + ((int) motionEvent.getX(actionIndex));
        iArr[1] = iArr[1] + ((int) motionEvent.getY(actionIndex));
        this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), motionEvent.getAction() == 1 ? "android.wallpaper.tap" : "android.wallpaper.secondaryTap", iArr[0], iArr[1], 0, null);
    }

    public void prepareDragWithProvider(DragPreviewProvider dragPreviewProvider) {
        this.mOutlineProvider = dragPreviewProvider;
    }

    public void snapToPageFromOverView(int i) {
        snapToPage(i, 250, Interpolators.ZOOM_IN);
    }

    private void onStartStateTransition(LauncherState launcherState) {
        this.mIsSwitchingState = true;
        this.mTransitionProgress = 0.0f;
        updateChildrenLayersEnabled();
    }

    private void onEndStateTransition() {
        this.mIsSwitchingState = false;
        this.mForceDrawAdjacentPages = false;
        this.mTransitionProgress = 1.0f;
        updateChildrenLayersEnabled();
        updateAccessibilityFlags();
    }

    @Override
    public void setState(LauncherState launcherState) {
        onStartStateTransition(launcherState);
        this.mStateTransitionAnimation.setState(launcherState);
        onEndStateTransition();
    }

    @Override
    public void setStateWithAnimation(LauncherState launcherState, AnimatorSetBuilder animatorSetBuilder, LauncherStateManager.AnimationConfig animationConfig) {
        StateTransitionListener stateTransitionListener = new StateTransitionListener(launcherState);
        this.mStateTransitionAnimation.setStateWithAnimation(launcherState, animatorSetBuilder, animationConfig);
        if (launcherState.hasMultipleVisiblePages) {
            this.mForceDrawAdjacentPages = true;
        }
        invalidate();
        ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
        valueAnimatorOfFloat.addUpdateListener(stateTransitionListener);
        valueAnimatorOfFloat.setDuration(animationConfig.duration);
        valueAnimatorOfFloat.addListener(stateTransitionListener);
        animatorSetBuilder.play(valueAnimatorOfFloat);
    }

    public void updateAccessibilityFlags() {
        int i = this.mLauncher.getStateManager().getState().workspaceAccessibilityFlag;
        if (!this.mLauncher.getAccessibilityDelegate().isInAccessibleDrag()) {
            int pageCount = getPageCount();
            for (int i2 = 0; i2 < pageCount; i2++) {
                updateAccessibilityFlags(i, (CellLayout) getPageAt(i2));
            }
            setImportantForAccessibility(i);
        }
    }

    private void updateAccessibilityFlags(int i, CellLayout cellLayout) {
        cellLayout.setImportantForAccessibility(2);
        cellLayout.getShortcutsAndWidgets().setImportantForAccessibility(i);
        cellLayout.setContentDescription(null);
        cellLayout.setAccessibilityDelegate(null);
    }

    public void startDrag(CellLayout.CellInfo cellInfo, DragOptions dragOptions) {
        View view = cellInfo.cell;
        this.mDragInfo = cellInfo;
        view.setVisibility(4);
        if (dragOptions.isAccessibleDrag) {
            this.mDragController.addDragListener(new AccessibleDragListenerAdapter(this, 2) {
                @Override
                protected void enableAccessibleDrag(boolean z) {
                    super.enableAccessibleDrag(z);
                    setEnableForLayout(Workspace.this.mLauncher.getHotseat().getLayout(), z);
                }
            });
        }
        beginDragShared(view, this, dragOptions);
    }

    public void beginDragShared(View view, DragSource dragSource, DragOptions dragOptions) {
        Object tag = view.getTag();
        if (!(tag instanceof ItemInfo)) {
            throw new IllegalStateException("Drag started with a view that has no tag set. This will cause a crash (issue 11627249) down the line. View: " + view + "  tag: " + view.getTag());
        }
        beginDragShared(view, dragSource, (ItemInfo) tag, new DragPreviewProvider(view), dragOptions);
    }

    public DragView beginDragShared(View view, DragSource dragSource, ItemInfo itemInfo, DragPreviewProvider dragPreviewProvider, DragOptions dragOptions) {
        float animatedScale;
        Rect rect;
        Point point;
        PopupContainerWithArrow popupContainerWithArrowShowForIcon;
        boolean z = view instanceof BubbleTextView;
        if (z) {
            Drawable icon = ((BubbleTextView) view).getIcon();
            if (icon instanceof FastBitmapDrawable) {
                animatedScale = ((FastBitmapDrawable) icon).getAnimatedScale();
            } else {
                animatedScale = 1.0f;
            }
        }
        view.clearFocus();
        view.setPressed(false);
        this.mOutlineProvider = dragPreviewProvider;
        Bitmap bitmapCreateDragBitmap = dragPreviewProvider.createDragBitmap();
        int i = dragPreviewProvider.previewPadding / 2;
        float scaleAndPosition = dragPreviewProvider.getScaleAndPosition(bitmapCreateDragBitmap, this.mTempXY);
        int i2 = this.mTempXY[0];
        int i3 = this.mTempXY[1];
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        if (z) {
            Rect rect2 = new Rect();
            ((BubbleTextView) view).getIconBounds(rect2);
            i3 += rect2.top;
            Point point2 = new Point(-i, i);
            rect = rect2;
            point = point2;
        } else if (view instanceof FolderIcon) {
            int i4 = deviceProfile.folderIconSizePx;
            point = new Point(-i, i - view.getPaddingTop());
            rect = new Rect(0, view.getPaddingTop(), view.getWidth(), i4);
        } else if (!(dragPreviewProvider instanceof ShortcutDragPreviewProvider)) {
            rect = null;
            point = null;
        } else {
            point = new Point(-i, i);
            rect = null;
        }
        if (z) {
            ((BubbleTextView) view).clearPressedBackground();
        }
        if (view.getParent() instanceof ShortcutAndWidgetContainer) {
            this.mDragSourceInternal = (ShortcutAndWidgetContainer) view.getParent();
        }
        if (z && !dragOptions.isAccessibleDrag && (popupContainerWithArrowShowForIcon = PopupContainerWithArrow.showForIcon((BubbleTextView) view)) != null) {
            dragOptions.preDragCondition = popupContainerWithArrowShowForIcon.createPreDragCondition();
            this.mLauncher.getUserEventDispatcher().resetElapsedContainerMillis("dragging started");
        }
        DragView dragViewStartDrag = this.mDragController.startDrag(bitmapCreateDragBitmap, i2, i3, dragSource, itemInfo, point, rect, scaleAndPosition * animatedScale, scaleAndPosition, dragOptions);
        dragViewStartDrag.setIntrinsicIconScaleFactor(dragOptions.intrinsicIconScaleFactor);
        return dragViewStartDrag;
    }

    private boolean transitionStateShouldAllowDrop() {
        return (!isSwitchingState() || this.mTransitionProgress > ALLOW_DROP_TRANSITION_PROGRESS) && workspaceIconsCanBeDragged();
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject dragObject) {
        CellLayout cellLayout;
        int i;
        int i2;
        int i3;
        int i4;
        CellLayout cellLayout2 = this.mDropToLayout;
        if (dragObject.dragSource != this) {
            if (cellLayout2 != null && transitionStateShouldAllowDrop()) {
                this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
                if (this.mLauncher.isHotseatLayout(cellLayout2)) {
                    mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
                } else {
                    mapPointFromSelfToChild(cellLayout2, this.mDragViewVisualCenter);
                }
                if (this.mDragInfo != null) {
                    CellLayout.CellInfo cellInfo = this.mDragInfo;
                    int i5 = cellInfo.spanX;
                    i2 = cellInfo.spanY;
                    i = i5;
                } else {
                    i = dragObject.dragInfo.spanX;
                    i2 = dragObject.dragInfo.spanY;
                }
                if (dragObject.dragInfo instanceof PendingAddWidgetInfo) {
                    i3 = ((PendingAddWidgetInfo) dragObject.dragInfo).minSpanX;
                    i4 = ((PendingAddWidgetInfo) dragObject.dragInfo).minSpanY;
                } else {
                    i3 = i;
                    i4 = i2;
                }
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i3, i4, cellLayout2, this.mTargetCell);
                float distanceFromCell = cellLayout2.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                if (this.mCreateUserFolderOnDrop && willCreateUserFolder(dragObject.dragInfo, cellLayout2, this.mTargetCell, distanceFromCell, true)) {
                    return true;
                }
                if (this.mAddToExistingFolderOnDrop && willAddToExistingUserFolder(dragObject.dragInfo, cellLayout2, this.mTargetCell, distanceFromCell)) {
                    return true;
                }
                cellLayout = cellLayout2;
                this.mTargetCell = cellLayout2.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i3, i4, i, i2, null, this.mTargetCell, new int[2], 4);
                if (!(this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0)) {
                    onNoCellFound(cellLayout);
                    return false;
                }
            } else {
                return false;
            }
        } else {
            cellLayout = cellLayout2;
        }
        if (getIdForScreen(cellLayout) == -201) {
            commitExtraEmptyScreen();
        }
        return true;
    }

    boolean willCreateUserFolder(ItemInfo itemInfo, CellLayout cellLayout, int[] iArr, float f, boolean z) {
        if (f > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        return willCreateUserFolder(itemInfo, cellLayout.getChildAt(iArr[0], iArr[1]), z);
    }

    boolean willCreateUserFolder(ItemInfo itemInfo, View view, boolean z) {
        if (view != null) {
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
            if (layoutParams.useTmpCoords && (layoutParams.tmpCellX != layoutParams.cellX || layoutParams.tmpCellY != layoutParams.cellY)) {
                return false;
            }
        }
        boolean z2 = this.mDragInfo != null && view == this.mDragInfo.cell;
        if (view == null || z2 || (z && !this.mCreateUserFolderOnDrop)) {
            return false;
        }
        return (view.getTag() instanceof ShortcutInfo) && (itemInfo.itemType == 0 || itemInfo.itemType == 1 || itemInfo.itemType == 6);
    }

    boolean willAddToExistingUserFolder(ItemInfo itemInfo, CellLayout cellLayout, int[] iArr, float f) {
        if (f > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        return willAddToExistingUserFolder(itemInfo, cellLayout.getChildAt(iArr[0], iArr[1]));
    }

    boolean willAddToExistingUserFolder(ItemInfo itemInfo, View view) {
        if (view != null) {
            CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view.getLayoutParams();
            if (layoutParams.useTmpCoords && (layoutParams.tmpCellX != layoutParams.cellX || layoutParams.tmpCellY != layoutParams.cellY)) {
                return false;
            }
        }
        if (!(view instanceof FolderIcon) || !((FolderIcon) view).acceptDrop(itemInfo)) {
            return false;
        }
        return true;
    }

    boolean createUserFolderIfNecessary(View view, long j, CellLayout cellLayout, int[] iArr, float f, boolean z, DragView dragView) {
        boolean z2;
        if (f > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View childAt = cellLayout.getChildAt(iArr[0], iArr[1]);
        if (this.mDragInfo != null) {
            z2 = this.mDragInfo.cellX == iArr[0] && this.mDragInfo.cellY == iArr[1] && getParentCellLayoutForView(this.mDragInfo.cell) == cellLayout;
        }
        if (childAt == null || z2 || !this.mCreateUserFolderOnDrop) {
            return false;
        }
        this.mCreateUserFolderOnDrop = false;
        long idForScreen = getIdForScreen(cellLayout);
        boolean z3 = childAt.getTag() instanceof ShortcutInfo;
        boolean z4 = view.getTag() instanceof ShortcutInfo;
        if (!z3 || !z4) {
            return false;
        }
        ShortcutInfo shortcutInfo = (ShortcutInfo) view.getTag();
        ShortcutInfo shortcutInfo2 = (ShortcutInfo) childAt.getTag();
        if (!z) {
            getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
        }
        Rect rect = new Rect();
        float descendantRectRelativeToSelf = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(childAt, rect);
        cellLayout.removeView(childAt);
        FolderIcon folderIconAddFolder = this.mLauncher.addFolder(cellLayout, j, idForScreen, iArr[0], iArr[1]);
        shortcutInfo2.cellX = -1;
        shortcutInfo2.cellY = -1;
        shortcutInfo.cellX = -1;
        shortcutInfo.cellY = -1;
        if (dragView != null) {
            folderIconAddFolder.setFolderBackground(this.mFolderCreateBg);
            this.mFolderCreateBg = new PreviewBackground();
            folderIconAddFolder.performCreateAnimation(shortcutInfo2, childAt, shortcutInfo, dragView, rect, descendantRectRelativeToSelf);
        } else {
            folderIconAddFolder.prepareCreateAnimation(childAt);
            folderIconAddFolder.addItem(shortcutInfo2);
            folderIconAddFolder.addItem(shortcutInfo);
        }
        return true;
    }

    boolean addToExistingFolderIfNecessary(View view, CellLayout cellLayout, int[] iArr, float f, DropTarget.DragObject dragObject, boolean z) {
        if (f > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View childAt = cellLayout.getChildAt(iArr[0], iArr[1]);
        if (!this.mAddToExistingFolderOnDrop) {
            return false;
        }
        this.mAddToExistingFolderOnDrop = false;
        if (childAt instanceof FolderIcon) {
            FolderIcon folderIcon = (FolderIcon) childAt;
            if (folderIcon.acceptDrop(dragObject.dragInfo)) {
                folderIcon.onDrop(dragObject, false);
                if (!z) {
                    getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepareAccessibilityDrop() {
    }

    @Override
    public void onDrop(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        DropTarget.DragObject dragObject2;
        boolean z;
        char c;
        ?? r11;
        long j;
        char c2;
        boolean z2;
        boolean z3;
        Runnable runnable;
        int i;
        ?? r12;
        int i2;
        boolean z4;
        boolean z5;
        boolean z6;
        char c3;
        View view;
        char c4;
        View view2;
        char c5;
        View view3;
        char c6;
        char c7;
        int i3;
        long j2;
        ItemInfo itemInfo;
        CellLayout cellLayout;
        final LauncherAppWidgetHostView launcherAppWidgetHostView;
        AppWidgetProviderInfo appWidgetInfo;
        this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
        CellLayout cellLayout2 = this.mDropToLayout;
        if (cellLayout2 != null) {
            if (this.mLauncher.isHotseatLayout(cellLayout2)) {
                mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(cellLayout2, this.mDragViewVisualCenter);
            }
        }
        if (dragObject.dragSource != this || this.mDragInfo == null) {
            dragObject2 = dragObject;
            z = false;
            onDropExternal(new int[]{(int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1]}, cellLayout2, dragObject2);
        } else {
            View view4 = this.mDragInfo.cell;
            Runnable runnable2 = null;
            if (cellLayout2 != null && !dragObject.cancelled) {
                boolean z7 = getParentCellLayoutForView(view4) != cellLayout2;
                boolean zIsHotseatLayout = this.mLauncher.isHotseatLayout(cellLayout2);
                long j3 = zIsHotseatLayout ? -101L : -100L;
                long idForScreen = this.mTargetCell[0] < 0 ? this.mDragInfo.screenId : getIdForScreen(cellLayout2);
                int i4 = this.mDragInfo != null ? this.mDragInfo.spanX : 1;
                int i5 = this.mDragInfo != null ? this.mDragInfo.spanY : 1;
                int i6 = i4;
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i4, i5, cellLayout2, this.mTargetCell);
                float distanceFromCell = cellLayout2.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                if (createUserFolderIfNecessary(view4, j3, cellLayout2, this.mTargetCell, distanceFromCell, false, dragObject.dragView) || addToExistingFolderIfNecessary(view4, cellLayout2, this.mTargetCell, distanceFromCell, dragObject, false)) {
                    this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, 500L);
                    return;
                }
                ItemInfo itemInfo2 = dragObject.dragInfo;
                int i7 = itemInfo2.spanX;
                int i8 = itemInfo2.spanY;
                if (itemInfo2.minSpanX > 0 && itemInfo2.minSpanY > 0) {
                    i7 = itemInfo2.minSpanX;
                    i8 = itemInfo2.minSpanY;
                }
                int i9 = i8;
                boolean z8 = itemInfo2.screenId == idForScreen && itemInfo2.container == j3 && itemInfo2.cellX == this.mTargetCell[0] && itemInfo2.cellY == this.mTargetCell[1];
                boolean z9 = z8 && this.mIsSwitchingState;
                if (isFinishedSwitchingState() || z9) {
                    i2 = i5;
                } else {
                    i2 = i5;
                    if (!cellLayout2.isRegionVacant(this.mTargetCell[0], this.mTargetCell[1], i6, i2)) {
                        z4 = true;
                    }
                    int[] iArr = new int[2];
                    if (!z4) {
                        int[] iArr2 = this.mTargetCell;
                        this.mTargetCell[1] = -1;
                        iArr2[0] = -1;
                        z5 = z4;
                        view = view4;
                        z6 = false;
                        c3 = 1;
                    } else {
                        z5 = z4;
                        View view5 = view4;
                        z6 = false;
                        c3 = 1;
                        this.mTargetCell = cellLayout2.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i7, i9, i6, i2, view5, this.mTargetCell, iArr, 2);
                        view = view5;
                    }
                    c4 = (this.mTargetCell[z6 ? 1 : 0] >= 0 || this.mTargetCell[c3] < 0) ? z6 ? 1 : 0 : c3;
                    if (c4 == 0) {
                        View view6 = view;
                        boolean z10 = view6 instanceof AppWidgetHostView;
                        view2 = view6;
                        if (z10) {
                            if (iArr[z6 ? 1 : 0] == itemInfo2.spanX) {
                                view2 = view6;
                                if (iArr[c3] != itemInfo2.spanY) {
                                }
                                if (c4 == 0) {
                                    if (getScreenIdForPageIndex(this.mCurrentPage) == idForScreen || zIsHotseatLayout) {
                                        i3 = -1;
                                    } else {
                                        int pageIndexForScreenId = getPageIndexForScreenId(idForScreen);
                                        snapToPage(pageIndexForScreenId);
                                        i3 = pageIndexForScreenId;
                                    }
                                    ItemInfo itemInfo3 = (ItemInfo) view3.getTag();
                                    if (z7) {
                                        CellLayout parentCellLayoutForView = getParentCellLayoutForView(view3);
                                        if (parentCellLayoutForView != null) {
                                            parentCellLayoutForView.removeView(view3);
                                        }
                                        j2 = idForScreen;
                                        itemInfo = itemInfo2;
                                        cellLayout = cellLayout2;
                                        addInScreen(view3, j3, idForScreen, this.mTargetCell[z6 ? 1 : 0], this.mTargetCell[c3], itemInfo3.spanX, itemInfo3.spanY);
                                    } else {
                                        j2 = idForScreen;
                                        itemInfo = itemInfo2;
                                        cellLayout = cellLayout2;
                                    }
                                    CellLayout.LayoutParams layoutParams = (CellLayout.LayoutParams) view3.getLayoutParams();
                                    int i10 = this.mTargetCell[z6 ? 1 : 0];
                                    layoutParams.tmpCellX = i10;
                                    layoutParams.cellX = i10;
                                    c7 = 1;
                                    int i11 = this.mTargetCell[1];
                                    layoutParams.tmpCellY = i11;
                                    layoutParams.cellY = i11;
                                    ItemInfo itemInfo4 = itemInfo;
                                    layoutParams.cellHSpan = itemInfo4.spanX;
                                    layoutParams.cellVSpan = itemInfo4.spanY;
                                    layoutParams.isLockedToGrid = true;
                                    if (j3 == -101 || !(view3 instanceof LauncherAppWidgetHostView) || (appWidgetInfo = (launcherAppWidgetHostView = (LauncherAppWidgetHostView) view3).getAppWidgetInfo()) == null || appWidgetInfo.resizeMode == 0) {
                                        dragObject2 = dragObject;
                                    } else {
                                        dragObject2 = dragObject;
                                        if (!dragObject2.accessibleDrag) {
                                            final CellLayout cellLayout3 = cellLayout;
                                            runnable2 = new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (!Workspace.this.isPageInTransition()) {
                                                        AppWidgetResizeFrame.showForWidget(launcherAppWidgetHostView, cellLayout3);
                                                    }
                                                }
                                            };
                                        }
                                    }
                                    this.mLauncher.getModelWriter().modifyItemInDatabase(itemInfo3, j3, j2, layoutParams.cellX, layoutParams.cellY, itemInfo4.spanX, itemInfo4.spanY);
                                    c6 = c5;
                                    i = i3;
                                    runnable = runnable2;
                                } else {
                                    char c8 = c3;
                                    dragObject2 = dragObject;
                                    if (!z5) {
                                        onNoCellFound(cellLayout2);
                                    }
                                    CellLayout.LayoutParams layoutParams2 = (CellLayout.LayoutParams) view3.getLayoutParams();
                                    this.mTargetCell[z6 ? 1 : 0] = layoutParams2.cellX;
                                    this.mTargetCell[c8 == true ? 1 : 0] = layoutParams2.cellY;
                                    ((CellLayout) view3.getParent().getParent()).markCellsAsOccupiedForView(view3);
                                    c6 = c5;
                                    runnable = null;
                                    i = -1;
                                    c7 = c8;
                                }
                                j = 500;
                                c2 = c6;
                                c = c7;
                                r11 = z6;
                                r12 = view3;
                                z2 = z8;
                                z3 = z9;
                            }
                            itemInfo2.spanX = iArr[z6 ? 1 : 0];
                            itemInfo2.spanY = iArr[c3];
                            AppWidgetResizeFrame.updateWidgetSizeRanges((AppWidgetHostView) view6, this.mLauncher, iArr[z6 ? 1 : 0], iArr[c3]);
                            c5 = c3;
                            view3 = view6;
                            if (c4 == 0) {
                            }
                            j = 500;
                            c2 = c6;
                            c = c7;
                            r11 = z6;
                            r12 = view3;
                            z2 = z8;
                            z3 = z9;
                        }
                    } else {
                        view2 = view;
                    }
                    c5 = z6 ? 1 : 0;
                    view3 = view2;
                    if (c4 == 0) {
                    }
                    j = 500;
                    c2 = c6;
                    c = c7;
                    r11 = z6;
                    r12 = view3;
                    z2 = z8;
                    z3 = z9;
                }
                z4 = false;
                int[] iArr3 = new int[2];
                if (!z4) {
                }
                if (this.mTargetCell[z6 ? 1 : 0] >= 0) {
                    if (c4 == 0) {
                    }
                    c5 = z6 ? 1 : 0;
                    view3 = view2;
                    if (c4 == 0) {
                    }
                    j = 500;
                    c2 = c6;
                    c = c7;
                    r11 = z6;
                    r12 = view3;
                    z2 = z8;
                    z3 = z9;
                }
            } else {
                c = 1;
                dragObject2 = dragObject;
                r11 = 0;
                j = 500;
                c2 = 0;
                z2 = false;
                z3 = false;
                runnable = null;
                i = -1;
                r12 = view4;
            }
            ?? r15 = (CellLayout) r12.getParent().getParent();
            if (dragObject2.dragView.hasDrawn()) {
                if (z3) {
                    this.mLauncher.getDragController().animateDragViewToOriginalPosition(runnable, r12, 150);
                    this.mLauncher.getStateManager().goToState(LauncherState.NORMAL);
                    this.mLauncher.getDropTargetBar().onDragEnd();
                    r15.onDropChild(r12);
                    return;
                }
                ItemInfo itemInfo5 = (ItemInfo) r12.getTag();
                ?? r2 = c;
                if (itemInfo5.itemType != 4) {
                    r2 = c;
                    if (itemInfo5.itemType != 5) {
                        r2 = r11;
                    }
                }
                if (r2 != 0) {
                    animateWidgetDrop(itemInfo5, r15, dragObject2.dragView, null, c2 != 0 ? 2 : r11, r12, false);
                } else {
                    this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject2.dragView, r12, i >= 0 ? 300 : -1, this);
                }
            } else {
                dragObject2.deferDragViewCleanupPostAnimation = r11;
                r12.setVisibility(r11);
            }
            r15.onDropChild(r12);
            this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, j, runnable);
            z = z2;
        }
        if (dragObject2.stateAnnouncer != null && !z) {
            dragObject2.stateAnnouncer.completeAction(R.string.item_moved);
        }
    }

    public void onNoCellFound(View view) {
        if (this.mLauncher.isHotseatLayout(view)) {
            this.mLauncher.getHotseat();
            showOutOfSpaceMessage(true);
        } else {
            showOutOfSpaceMessage(false);
        }
    }

    private void showOutOfSpaceMessage(boolean z) {
        Toast.makeText(this.mLauncher, this.mLauncher.getString(z ? R.string.hotseat_out_of_space : R.string.out_of_space), 0).show();
    }

    public void getPageAreaRelativeToDragLayer(Rect rect) {
        CellLayout cellLayout = (CellLayout) getChildAt(getNextPage());
        if (cellLayout == null) {
            return;
        }
        ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
        this.mTempXY[0] = getPaddingLeft() + shortcutsAndWidgets.getLeft();
        this.mTempXY[1] = cellLayout.getTop() + shortcutsAndWidgets.getTop();
        float descendantCoordRelativeToSelf = this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempXY);
        rect.set(this.mTempXY[0], this.mTempXY[1], (int) (this.mTempXY[0] + (shortcutsAndWidgets.getMeasuredWidth() * descendantCoordRelativeToSelf)), (int) (this.mTempXY[1] + (descendantCoordRelativeToSelf * shortcutsAndWidgets.getMeasuredHeight())));
    }

    @Override
    public void onDragEnter(DropTarget.DragObject dragObject) {
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mDropToLayout = null;
        this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
        setDropLayoutForDragObject(dragObject, this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1]);
    }

    @Override
    public void onDragExit(DropTarget.DragObject dragObject) {
        this.mDropToLayout = this.mDragTargetLayout;
        if (this.mDragMode == 1) {
            this.mCreateUserFolderOnDrop = true;
        } else if (this.mDragMode == 2) {
            this.mAddToExistingFolderOnDrop = true;
        }
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);
        this.mSpringLoadedDragController.cancel();
    }

    private void enforceDragParity(String str, int i, int i2) {
        enforceDragParity(this, str, i, i2);
        for (int i3 = 0; i3 < getChildCount(); i3++) {
            enforceDragParity(getChildAt(i3), str, i, i2);
        }
    }

    private void enforceDragParity(View view, String str, int i, int i2) {
        Object tag = view.getTag(R.id.drag_event_parity);
        int iIntValue = (tag == null ? 0 : ((Integer) tag).intValue()) + i;
        view.setTag(R.id.drag_event_parity, Integer.valueOf(iIntValue));
        if (iIntValue != i2) {
            Log.e(TAG, str + ": Drag contract violated: " + iIntValue);
        }
    }

    void setCurrentDropLayout(CellLayout cellLayout) {
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.revertTempState();
            this.mDragTargetLayout.onDragExit();
        }
        this.mDragTargetLayout = cellLayout;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    void setCurrentDragOverlappingLayout(CellLayout cellLayout) {
        if (this.mDragOverlappingLayout != null) {
            this.mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        this.mDragOverlappingLayout = cellLayout;
        if (this.mDragOverlappingLayout != null) {
            this.mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        this.mLauncher.getDragLayer().getScrim().invalidate();
    }

    public CellLayout getCurrentDragOverlappingLayout() {
        return this.mDragOverlappingLayout;
    }

    void setCurrentDropOverCell(int i, int i2) {
        if (i != this.mDragOverX || i2 != this.mDragOverY) {
            this.mDragOverX = i;
            this.mDragOverY = i2;
            setDragMode(0);
        }
    }

    void setDragMode(int i) {
        if (i != this.mDragMode) {
            if (i == 0) {
                cleanupAddToFolder();
                cleanupReorder(false);
                cleanupFolderCreation();
            } else if (i == 2) {
                cleanupReorder(true);
                cleanupFolderCreation();
            } else if (i == 1) {
                cleanupAddToFolder();
                cleanupReorder(true);
            } else if (i == 3) {
                cleanupAddToFolder();
                cleanupFolderCreation();
            }
            this.mDragMode = i;
        }
    }

    private void cleanupFolderCreation() {
        if (this.mFolderCreateBg != null) {
            this.mFolderCreateBg.animateToRest();
        }
        this.mFolderCreationAlarm.setOnAlarmListener(null);
        this.mFolderCreationAlarm.cancelAlarm();
    }

    private void cleanupAddToFolder() {
        if (this.mDragOverFolderIcon != null) {
            this.mDragOverFolderIcon.onDragExit();
            this.mDragOverFolderIcon = null;
        }
    }

    private void cleanupReorder(boolean z) {
        if (z) {
            this.mReorderAlarm.cancelAlarm();
        }
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
    }

    void mapPointFromSelfToChild(View view, float[] fArr) {
        fArr[0] = fArr[0] - view.getLeft();
        fArr[1] = fArr[1] - view.getTop();
    }

    boolean isPointInSelfOverHotseat(int i, int i2) {
        this.mTempXY[0] = i;
        this.mTempXY[1] = i2;
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempXY, true);
        Hotseat hotseat = this.mLauncher.getHotseat();
        return this.mTempXY[0] >= hotseat.getLeft() && this.mTempXY[0] <= hotseat.getRight() && this.mTempXY[1] >= hotseat.getTop() && this.mTempXY[1] <= hotseat.getBottom();
    }

    void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] fArr) {
        this.mTempXY[0] = (int) fArr[0];
        this.mTempXY[1] = (int) fArr[1];
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempXY, true);
        this.mLauncher.getDragLayer().mapCoordInSelfToDescendant(hotseat.getLayout(), this.mTempXY);
        fArr[0] = this.mTempXY[0];
        fArr[1] = this.mTempXY[1];
    }

    private boolean isDragWidget(DropTarget.DragObject dragObject) {
        return (dragObject.dragInfo instanceof LauncherAppWidgetInfo) || (dragObject.dragInfo instanceof PendingAddWidgetInfo);
    }

    @Override
    public void onDragOver(DropTarget.DragObject dragObject) {
        ItemInfo itemInfo;
        int i;
        int i2;
        int i3;
        if (!transitionStateShouldAllowDrop() || (itemInfo = dragObject.dragInfo) == null) {
            return;
        }
        if (itemInfo.spanX >= 0 && itemInfo.spanY >= 0) {
            this.mDragViewVisualCenter = dragObject.getVisualCenter(this.mDragViewVisualCenter);
            View view = this.mDragInfo == null ? null : this.mDragInfo.cell;
            if (setDropLayoutForDragObject(dragObject, this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1])) {
                if (this.mLauncher.isHotseatLayout(this.mDragTargetLayout)) {
                    this.mSpringLoadedDragController.cancel();
                } else {
                    this.mSpringLoadedDragController.setAlarm(this.mDragTargetLayout);
                }
            }
            if (this.mDragTargetLayout != null) {
                if (this.mLauncher.isHotseatLayout(this.mDragTargetLayout)) {
                    mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
                } else {
                    mapPointFromSelfToChild(this.mDragTargetLayout, this.mDragViewVisualCenter);
                }
                int i4 = itemInfo.spanX;
                int i5 = itemInfo.spanY;
                if (itemInfo.minSpanX > 0 && itemInfo.minSpanY > 0) {
                    i4 = itemInfo.minSpanX;
                    i5 = itemInfo.minSpanY;
                }
                int i6 = i4;
                int i7 = i5;
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i6, i7, this.mDragTargetLayout, this.mTargetCell);
                int i8 = this.mTargetCell[0];
                int i9 = this.mTargetCell[1];
                setCurrentDropOverCell(this.mTargetCell[0], this.mTargetCell[1]);
                manageFolderFeedback(this.mDragTargetLayout, this.mTargetCell, this.mDragTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell), dragObject);
                boolean zIsNearestDropLocationOccupied = this.mDragTargetLayout.isNearestDropLocationOccupied((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], itemInfo.spanX, itemInfo.spanY, view, this.mTargetCell);
                if (!zIsNearestDropLocationOccupied) {
                    i = 1;
                    this.mDragTargetLayout.visualizeDropLocation(view, this.mOutlineProvider, this.mTargetCell[0], this.mTargetCell[1], itemInfo.spanX, itemInfo.spanY, false, dragObject);
                } else {
                    i = 1;
                    if ((this.mDragMode == 0 || this.mDragMode == 3) && !this.mReorderAlarm.alarmPending() && (this.mLastReorderX != i8 || this.mLastReorderY != i9)) {
                        this.mDragTargetLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i6, i7, itemInfo.spanX, itemInfo.spanY, view, this.mTargetCell, new int[2], 0);
                        i2 = 2;
                        i3 = 1;
                        this.mReorderAlarm.setOnAlarmListener(new ReorderAlarmListener(this.mDragViewVisualCenter, i6, i7, itemInfo.spanX, itemInfo.spanY, dragObject, view));
                        this.mReorderAlarm.setAlarm(650L);
                    }
                    if ((this.mDragMode != i3 || this.mDragMode == i2 || !zIsNearestDropLocationOccupied) && this.mDragTargetLayout != null) {
                        this.mDragTargetLayout.revertTempState();
                        return;
                    }
                    return;
                }
                i3 = i;
                i2 = 2;
                if (this.mDragMode != i3) {
                }
                this.mDragTargetLayout.revertTempState();
                return;
            }
            return;
        }
        throw new RuntimeException("Improper spans found");
    }

    private boolean setDropLayoutForDragObject(DropTarget.DragObject dragObject, float f, float f2) {
        CellLayout cellLayoutVerifyInsidePage;
        if (this.mLauncher.getHotseat() != null && !isDragWidget(dragObject) && isPointInSelfOverHotseat(dragObject.x, dragObject.y)) {
            cellLayoutVerifyInsidePage = this.mLauncher.getHotseat().getLayout();
        } else {
            cellLayoutVerifyInsidePage = null;
        }
        int nextPage = getNextPage();
        if (cellLayoutVerifyInsidePage == null && !isPageInTransition()) {
            this.mTempTouchCoordinates[0] = Math.min(f, dragObject.x);
            this.mTempTouchCoordinates[1] = dragObject.y;
            cellLayoutVerifyInsidePage = verifyInsidePage((this.mIsRtl ? 1 : -1) + nextPage, this.mTempTouchCoordinates);
        }
        if (cellLayoutVerifyInsidePage == null && !isPageInTransition()) {
            this.mTempTouchCoordinates[0] = Math.max(f, dragObject.x);
            this.mTempTouchCoordinates[1] = dragObject.y;
            cellLayoutVerifyInsidePage = verifyInsidePage((this.mIsRtl ? -1 : 1) + nextPage, this.mTempTouchCoordinates);
        }
        if (cellLayoutVerifyInsidePage == null && nextPage >= 0 && nextPage < getPageCount()) {
            cellLayoutVerifyInsidePage = (CellLayout) getChildAt(nextPage);
        }
        if (cellLayoutVerifyInsidePage == this.mDragTargetLayout) {
            return false;
        }
        setCurrentDropLayout(cellLayoutVerifyInsidePage);
        setCurrentDragOverlappingLayout(cellLayoutVerifyInsidePage);
        return true;
    }

    private CellLayout verifyInsidePage(int i, float[] fArr) {
        if (i >= 0 && i < getPageCount()) {
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            mapPointFromSelfToChild(cellLayout, fArr);
            if (fArr[0] >= 0.0f && fArr[0] <= cellLayout.getWidth() && fArr[1] >= 0.0f && fArr[1] <= cellLayout.getHeight()) {
                return cellLayout;
            }
            return null;
        }
        return null;
    }

    private void manageFolderFeedback(CellLayout cellLayout, int[] iArr, float f, DropTarget.DragObject dragObject) {
        if (f > this.mMaxDistanceForFolderCreation) {
            return;
        }
        View childAt = this.mDragTargetLayout.getChildAt(this.mTargetCell[0], this.mTargetCell[1]);
        ItemInfo itemInfo = dragObject.dragInfo;
        boolean zWillCreateUserFolder = willCreateUserFolder(itemInfo, childAt, false);
        if (this.mDragMode == 0 && zWillCreateUserFolder && !this.mFolderCreationAlarm.alarmPending()) {
            FolderCreationAlarmListener folderCreationAlarmListener = new FolderCreationAlarmListener(cellLayout, iArr[0], iArr[1]);
            if (!dragObject.accessibleDrag) {
                this.mFolderCreationAlarm.setOnAlarmListener(folderCreationAlarmListener);
                this.mFolderCreationAlarm.setAlarm(0L);
            } else {
                folderCreationAlarmListener.onAlarm(this.mFolderCreationAlarm);
            }
            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(WorkspaceAccessibilityHelper.getDescriptionForDropOver(childAt, getContext()));
                return;
            }
            return;
        }
        boolean zWillAddToExistingUserFolder = willAddToExistingUserFolder(itemInfo, childAt);
        if (!zWillAddToExistingUserFolder || this.mDragMode != 0) {
            if (this.mDragMode == 2 && !zWillAddToExistingUserFolder) {
                setDragMode(0);
            }
            if (this.mDragMode == 1 && !zWillCreateUserFolder) {
                setDragMode(0);
                return;
            }
            return;
        }
        this.mDragOverFolderIcon = (FolderIcon) childAt;
        this.mDragOverFolderIcon.onDragEnter(itemInfo);
        if (cellLayout != null) {
            cellLayout.clearDragOutlines();
        }
        setDragMode(2);
        if (dragObject.stateAnnouncer != null) {
            dragObject.stateAnnouncer.announce(WorkspaceAccessibilityHelper.getDescriptionForDropOver(childAt, getContext()));
        }
    }

    class FolderCreationAlarmListener implements OnAlarmListener {
        final PreviewBackground bg = new PreviewBackground();
        final int cellX;
        final int cellY;
        final CellLayout layout;

        public FolderCreationAlarmListener(CellLayout cellLayout, int i, int i2) {
            this.layout = cellLayout;
            this.cellX = i;
            this.cellY = i2;
            BubbleTextView bubbleTextView = (BubbleTextView) cellLayout.getChildAt(i, i2);
            this.bg.setup(Workspace.this.mLauncher, null, bubbleTextView.getMeasuredWidth(), bubbleTextView.getPaddingTop());
            this.bg.isClipping = false;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            Workspace.this.mFolderCreateBg = this.bg;
            Workspace.this.mFolderCreateBg.animateToAccept(this.layout, this.cellX, this.cellY);
            this.layout.clearDragOutlines();
            Workspace.this.setDragMode(1);
        }
    }

    class ReorderAlarmListener implements OnAlarmListener {
        final View child;
        final DropTarget.DragObject dragObject;
        final float[] dragViewCenter;
        final int minSpanX;
        final int minSpanY;
        final int spanX;
        final int spanY;

        public ReorderAlarmListener(float[] fArr, int i, int i2, int i3, int i4, DropTarget.DragObject dragObject, View view) {
            this.dragViewCenter = fArr;
            this.minSpanX = i;
            this.minSpanY = i2;
            this.spanX = i3;
            this.spanY = i4;
            this.child = view;
            this.dragObject = dragObject;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            int[] iArr = new int[2];
            Workspace.this.mTargetCell = Workspace.this.findNearestArea((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.minSpanX, this.minSpanY, Workspace.this.mDragTargetLayout, Workspace.this.mTargetCell);
            Workspace.this.mLastReorderX = Workspace.this.mTargetCell[0];
            Workspace.this.mLastReorderY = Workspace.this.mTargetCell[1];
            Workspace.this.mTargetCell = Workspace.this.mDragTargetLayout.performReorder((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.minSpanX, this.minSpanY, this.spanX, this.spanY, this.child, Workspace.this.mTargetCell, iArr, 1);
            if (Workspace.this.mTargetCell[0] < 0 || Workspace.this.mTargetCell[1] < 0) {
                Workspace.this.mDragTargetLayout.revertTempState();
            } else {
                Workspace.this.setDragMode(3);
            }
            Workspace.this.mDragTargetLayout.visualizeDropLocation(this.child, Workspace.this.mOutlineProvider, Workspace.this.mTargetCell[0], Workspace.this.mTargetCell[1], iArr[0], iArr[1], (iArr[0] == this.spanX && iArr[1] == this.spanY) ? false : true, this.dragObject);
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect rect) {
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, rect);
    }

    private void onDropExternal(int[] iArr, CellLayout cellLayout, DropTarget.DragObject dragObject) {
        long j;
        ItemInfo shortcutInfo;
        ItemInfo itemInfo;
        View viewCreateShortcut;
        ItemInfo itemInfo2;
        View view;
        char c;
        DropTarget.DragObject dragObject2;
        CellLayout cellLayout2;
        boolean z;
        PendingAddItemInfo pendingAddItemInfo;
        int i;
        long j2;
        ItemInfo itemInfo3;
        boolean z2;
        int i2;
        ShortcutInfo shortcutInfoCreateShortcutInfo;
        if ((dragObject.dragInfo instanceof PendingAddShortcutInfo) && (shortcutInfoCreateShortcutInfo = ((PendingAddShortcutInfo) dragObject.dragInfo).activityInfo.createShortcutInfo()) != null) {
            dragObject.dragInfo = shortcutInfoCreateShortcutInfo;
        }
        ItemInfo itemInfo4 = dragObject.dragInfo;
        int i3 = itemInfo4.spanX;
        int i4 = itemInfo4.spanY;
        if (this.mDragInfo != null) {
            i3 = this.mDragInfo.spanX;
            i4 = this.mDragInfo.spanY;
        }
        int i5 = i3;
        int i6 = i4;
        if (this.mLauncher.isHotseatLayout(cellLayout)) {
            j = -101;
        } else {
            j = -100;
        }
        final long j3 = j;
        long idForScreen = getIdForScreen(cellLayout);
        if (!this.mLauncher.isHotseatLayout(cellLayout) && idForScreen != getScreenIdForPageIndex(this.mCurrentPage) && !this.mLauncher.isInState(LauncherState.SPRING_LOADED)) {
            snapToPage(getPageIndexForScreenId(idForScreen));
        }
        if (itemInfo4 instanceof PendingAddItemInfo) {
            PendingAddItemInfo pendingAddItemInfo2 = (PendingAddItemInfo) itemInfo4;
            if (pendingAddItemInfo2.itemType == 1) {
                this.mTargetCell = findNearestArea(iArr[0], iArr[1], i5, i6, cellLayout, this.mTargetCell);
                float distanceFromCell = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                z = (willCreateUserFolder(dragObject.dragInfo, cellLayout, this.mTargetCell, distanceFromCell, true) || willAddToExistingUserFolder(dragObject.dragInfo, cellLayout, this.mTargetCell, distanceFromCell)) ? false : true;
            }
            final ItemInfo itemInfo5 = dragObject.dragInfo;
            if (z) {
                int i7 = itemInfo5.spanX;
                int i8 = itemInfo5.spanY;
                if (itemInfo5.minSpanX > 0 && itemInfo5.minSpanY > 0) {
                    i7 = itemInfo5.minSpanX;
                    i8 = itemInfo5.minSpanY;
                }
                int[] iArr2 = new int[2];
                pendingAddItemInfo = pendingAddItemInfo2;
                i = 1;
                j2 = idForScreen;
                itemInfo3 = itemInfo4;
                this.mTargetCell = cellLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], i7, i8, itemInfo4.spanX, itemInfo4.spanY, null, this.mTargetCell, iArr2, 3);
                boolean z3 = (iArr2[0] == itemInfo5.spanX && iArr2[1] == itemInfo5.spanY) ? false : true;
                itemInfo5.spanX = iArr2[0];
                itemInfo5.spanY = iArr2[1];
                z2 = z3;
            } else {
                pendingAddItemInfo = pendingAddItemInfo2;
                i = 1;
                j2 = idForScreen;
                itemInfo3 = itemInfo4;
                z2 = false;
            }
            final PendingAddItemInfo pendingAddItemInfo3 = pendingAddItemInfo;
            ItemInfo itemInfo6 = itemInfo3;
            final long j4 = j2;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    Workspace.this.deferRemoveExtraEmptyScreen();
                    Workspace.this.mLauncher.addPendingItem(pendingAddItemInfo3, j3, j4, Workspace.this.mTargetCell, itemInfo5.spanX, itemInfo5.spanY);
                }
            };
            PendingAddItemInfo pendingAddItemInfo4 = pendingAddItemInfo;
            int i9 = (pendingAddItemInfo4.itemType == 4 || pendingAddItemInfo4.itemType == 5) ? i : 0;
            AppWidgetHostView appWidgetHostView = i9 != 0 ? ((PendingAddWidgetInfo) pendingAddItemInfo4).boundWidget : null;
            if (appWidgetHostView != null && z2) {
                AppWidgetResizeFrame.updateWidgetSizeRanges(appWidgetHostView, this.mLauncher, itemInfo5.spanX, itemInfo5.spanY);
            }
            if (i9 != 0) {
                PendingAddWidgetInfo pendingAddWidgetInfo = (PendingAddWidgetInfo) pendingAddItemInfo4;
                i2 = (pendingAddWidgetInfo.info == null || !pendingAddWidgetInfo.getHandler().needsConfigure()) ? 0 : i;
            }
            animateWidgetDrop(itemInfo6, cellLayout, dragObject.dragView, runnable, i2, appWidgetHostView, true);
            return;
        }
        this.mLauncher.getStateManager().goToState(LauncherState.NORMAL, 500L);
        int i10 = itemInfo4.itemType;
        if (i10 != 6) {
            switch (i10) {
                case 0:
                case 1:
                    if (itemInfo4.container == -1) {
                        if (itemInfo4 instanceof AppInfo) {
                            shortcutInfo = ((AppInfo) itemInfo4).makeShortcut();
                            dragObject.dragInfo = shortcutInfo;
                        } else if (itemInfo4 instanceof ShortcutInfo) {
                            shortcutInfo = new ShortcutInfo((ShortcutInfo) itemInfo4);
                            dragObject.dragInfo = shortcutInfo;
                        }
                        itemInfo = shortcutInfo;
                        viewCreateShortcut = this.mLauncher.createShortcut(cellLayout, (ShortcutInfo) shortcutInfo);
                    } else {
                        shortcutInfo = itemInfo4;
                        itemInfo = shortcutInfo;
                        viewCreateShortcut = this.mLauncher.createShortcut(cellLayout, (ShortcutInfo) shortcutInfo);
                    }
                    break;
                case 2:
                    itemInfo = itemInfo4;
                    viewCreateShortcut = FolderIcon.fromXml(R.layout.folder_icon, this.mLauncher, cellLayout, (FolderInfo) itemInfo4);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type: " + itemInfo4.itemType);
            }
        }
        if (iArr != null) {
            this.mTargetCell = findNearestArea(iArr[0], iArr[1], i5, i6, cellLayout, this.mTargetCell);
            float distanceFromCell2 = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            if (!createUserFolderIfNecessary(viewCreateShortcut, j3, cellLayout, this.mTargetCell, distanceFromCell2, true, dragObject.dragView)) {
                if (addToExistingFolderIfNecessary(viewCreateShortcut, cellLayout, this.mTargetCell, distanceFromCell2, dragObject, true)) {
                    return;
                }
            } else {
                return;
            }
        }
        if (iArr != null) {
            cellLayout2 = cellLayout;
            itemInfo2 = itemInfo;
            view = viewCreateShortcut;
            c = 1;
            dragObject2 = dragObject;
            this.mTargetCell = cellLayout2.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, 1, 1, null, this.mTargetCell, null, 3);
        } else {
            itemInfo2 = itemInfo;
            view = viewCreateShortcut;
            c = 1;
            dragObject2 = dragObject;
            cellLayout2 = cellLayout;
            cellLayout2.findCellForSpan(this.mTargetCell, 1, 1);
        }
        this.mLauncher.getModelWriter().addOrMoveItemInDatabase(itemInfo2, j3, idForScreen, this.mTargetCell[0], this.mTargetCell[c]);
        DropTarget.DragObject dragObject3 = dragObject2;
        View view2 = view;
        addInScreen(view, j3, idForScreen, this.mTargetCell[0], this.mTargetCell[c], itemInfo2.spanX, itemInfo2.spanY);
        cellLayout2.onDropChild(view2);
        cellLayout.getShortcutsAndWidgets().measureChild(view2);
        if (dragObject3.dragView != null) {
            setFinalTransitionTransform();
            this.mLauncher.getDragLayer().animateViewIntoPosition(dragObject3.dragView, view2, this);
            resetTransitionTransform();
        }
    }

    public Bitmap createWidgetBitmap(ItemInfo itemInfo, View view) {
        int[] iArrEstimateItemSize = estimateItemSize(itemInfo);
        int visibility = view.getVisibility();
        view.setVisibility(0);
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(iArrEstimateItemSize[0], 1073741824);
        int iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(iArrEstimateItemSize[1], 1073741824);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iArrEstimateItemSize[0], iArrEstimateItemSize[1], Bitmap.Config.ARGB_8888);
        view.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
        view.layout(0, 0, iArrEstimateItemSize[0], iArrEstimateItemSize[1]);
        view.draw(new Canvas(bitmapCreateBitmap));
        view.setVisibility(visibility);
        return bitmapCreateBitmap;
    }

    private void getFinalPositionForDropAnimation(int[] iArr, float[] fArr, DragView dragView, CellLayout cellLayout, ItemInfo itemInfo, int[] iArr2, boolean z) {
        Rect rectEstimateItemPosition = estimateItemPosition(cellLayout, iArr2[0], iArr2[1], itemInfo.spanX, itemInfo.spanY);
        if (itemInfo.itemType == 4) {
            DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
            Utilities.shrinkRect(rectEstimateItemPosition, deviceProfile.appWidgetScale.x, deviceProfile.appWidgetScale.y);
        }
        iArr[0] = rectEstimateItemPosition.left;
        iArr[1] = rectEstimateItemPosition.top;
        setFinalTransitionTransform();
        float descendantCoordRelativeToSelf = this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(cellLayout, iArr, true);
        resetTransitionTransform();
        if (z) {
            iArr[0] = (int) (((double) iArr[0]) - (((double) ((dragView.getMeasuredWidth() - (rectEstimateItemPosition.width() * descendantCoordRelativeToSelf)) / 2.0f)) - Math.ceil(cellLayout.getUnusedHorizontalSpace() / 2.0f)));
            iArr[1] = (int) (iArr[1] - ((dragView.getMeasuredHeight() - (rectEstimateItemPosition.height() * descendantCoordRelativeToSelf)) / 2.0f));
            fArr[0] = ((rectEstimateItemPosition.width() * 1.0f) / dragView.getMeasuredWidth()) * descendantCoordRelativeToSelf;
            fArr[1] = ((1.0f * rectEstimateItemPosition.height()) / dragView.getMeasuredHeight()) * descendantCoordRelativeToSelf;
            return;
        }
        float initialScale = dragView.getInitialScale() * descendantCoordRelativeToSelf;
        float f = initialScale - 1.0f;
        iArr[0] = (int) (iArr[0] + ((dragView.getWidth() * f) / 2.0f));
        iArr[1] = (int) (iArr[1] + ((f * dragView.getHeight()) / 2.0f));
        fArr[1] = initialScale;
        fArr[0] = initialScale;
        if (dragView.getDragRegion() != null) {
            iArr[0] = (int) (iArr[0] + (r0.left * descendantCoordRelativeToSelf));
            iArr[1] = (int) (iArr[1] + (descendantCoordRelativeToSelf * r0.top));
        }
    }

    public void animateWidgetDrop(ItemInfo itemInfo, CellLayout cellLayout, DragView dragView, final Runnable runnable, int i, final View view, boolean z) {
        int i2;
        Rect rect = new Rect();
        this.mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, rect);
        int[] iArr = new int[2];
        float[] fArr = new float[2];
        getFinalPositionForDropAnimation(iArr, fArr, dragView, cellLayout, itemInfo, this.mTargetCell, !(itemInfo instanceof PendingAddShortcutInfo));
        int integer = this.mLauncher.getResources().getInteger(R.integer.config_dropAnimMaxDuration) - 200;
        boolean z2 = itemInfo.itemType == 4 || itemInfo.itemType == 5;
        if ((i == 2 || z) && view != null) {
            dragView.setCrossFadeBitmap(createWidgetBitmap(itemInfo, view));
            dragView.crossFade((int) (integer * 0.8f));
        } else {
            if (z2 && z) {
                i2 = 1;
                float fMin = Math.min(fArr[0], fArr[1]);
                fArr[1] = fMin;
                fArr[0] = fMin;
            }
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (i != 4) {
                this.mLauncher.getDragLayer().animateViewIntoPosition(dragView, iArr, 0.0f, 0.1f, 0.1f, 0, runnable, integer);
                return;
            } else {
                dragLayer.animateViewIntoPosition(dragView, rect.left, rect.top, iArr[0], iArr[i2], 1.0f, 1.0f, 1.0f, fArr[0], fArr[i2], new Runnable() {
                    @Override
                    public void run() {
                        if (view != null) {
                            view.setVisibility(0);
                        }
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                }, i == i2 ? 2 : 0, integer, this);
                return;
            }
        }
        i2 = 1;
        DragLayer dragLayer2 = this.mLauncher.getDragLayer();
        if (i != 4) {
        }
    }

    public void setFinalTransitionTransform() {
        if (isSwitchingState()) {
            this.mCurrentScale = getScaleX();
            setScaleX(this.mStateTransitionAnimation.getFinalScale());
            setScaleY(this.mStateTransitionAnimation.getFinalScale());
        }
    }

    public void resetTransitionTransform() {
        if (isSwitchingState()) {
            setScaleX(this.mCurrentScale);
            setScaleY(this.mCurrentScale);
        }
    }

    public CellLayout.CellInfo getDragInfo() {
        return this.mDragInfo;
    }

    int[] findNearestArea(int i, int i2, int i3, int i4, CellLayout cellLayout, int[] iArr) {
        return cellLayout.findNearestArea(i, i2, i3, i4, iArr);
    }

    void setup(DragController dragController) {
        this.mSpringLoadedDragController = new SpringLoadedDragController(this.mLauncher);
        this.mDragController = dragController;
        updateChildrenLayersEnabled();
    }

    @Override
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
        CellLayout cellLayout;
        if (z) {
            if (view != this && this.mDragInfo != null) {
                removeWorkspaceItem(this.mDragInfo.cell);
            }
        } else if (this.mDragInfo != null && (cellLayout = this.mLauncher.getCellLayout(this.mDragInfo.container, this.mDragInfo.screenId)) != null) {
            cellLayout.onDropChild(this.mDragInfo.cell);
        }
        View homescreenIconByItemId = getHomescreenIconByItemId(dragObject.originalDragInfo.id);
        if (dragObject.cancelled && homescreenIconByItemId != null) {
            homescreenIconByItemId.setVisibility(0);
        }
        this.mDragInfo = null;
    }

    public void removeWorkspaceItem(View view) {
        CellLayout parentCellLayoutForView = getParentCellLayoutForView(view);
        if (parentCellLayoutForView != null) {
            parentCellLayoutForView.removeView(view);
        }
        if (view instanceof DropTarget) {
            this.mDragController.removeDropTarget((DropTarget) view);
        }
    }

    public void removeFolderListeners() {
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if (view instanceof FolderIcon) {
                    ((FolderIcon) view).removeListeners();
                    return false;
                }
                return false;
            }
        });
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> sparseArray) {
        this.mSavedStates = sparseArray;
    }

    public void restoreInstanceStateForChild(int i) {
        if (this.mSavedStates != null) {
            this.mRestoredPages.add(Integer.valueOf(i));
            CellLayout cellLayout = (CellLayout) getChildAt(i);
            if (cellLayout != null) {
                cellLayout.restoreInstanceState(this.mSavedStates);
            }
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (!this.mRestoredPages.contains(Integer.valueOf(i))) {
                restoreInstanceStateForChild(i);
            }
        }
        this.mRestoredPages.clear();
        this.mSavedStates = null;
    }

    @Override
    public boolean scrollLeft() {
        boolean zScrollLeft;
        if (!workspaceInModalState() && !this.mIsSwitchingState) {
            zScrollLeft = super.scrollLeft();
        } else {
            zScrollLeft = false;
        }
        Folder open = Folder.getOpen(this.mLauncher);
        if (open != null) {
            open.completeDragExit();
        }
        return zScrollLeft;
    }

    @Override
    public boolean scrollRight() {
        boolean zScrollRight;
        if (!workspaceInModalState() && !this.mIsSwitchingState) {
            zScrollRight = super.scrollRight();
        } else {
            zScrollRight = false;
        }
        Folder open = Folder.getOpen(this.mLauncher);
        if (open != null) {
            open.completeDragExit();
        }
        return zScrollRight;
    }

    CellLayout getParentCellLayoutForView(View view) {
        for (CellLayout cellLayout : getWorkspaceAndHotseatCellLayouts()) {
            if (cellLayout.getShortcutsAndWidgets().indexOfChild(view) > -1) {
                return cellLayout;
            }
        }
        return null;
    }

    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> arrayList = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            arrayList.add((CellLayout) getChildAt(i));
        }
        if (this.mLauncher.getHotseat() != null) {
            arrayList.add(this.mLauncher.getHotseat().getLayout());
        }
        return arrayList;
    }

    ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
        ArrayList<ShortcutAndWidgetContainer> arrayList = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            arrayList.add(((CellLayout) getChildAt(i)).getShortcutsAndWidgets());
        }
        if (this.mLauncher.getHotseat() != null) {
            arrayList.add(this.mLauncher.getHotseat().getLayout().getShortcutsAndWidgets());
        }
        return arrayList;
    }

    public View getHomescreenIconByItemId(final long j) {
        return getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                return itemInfo != null && itemInfo.id == j;
            }
        });
    }

    public View getViewForTag(final Object obj) {
        return getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                return itemInfo == obj;
            }
        });
    }

    public LauncherAppWidgetHostView getWidgetForAppWidgetId(final int i) {
        return (LauncherAppWidgetHostView) getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                return (itemInfo instanceof LauncherAppWidgetInfo) && ((LauncherAppWidgetInfo) itemInfo).appWidgetId == i;
            }
        });
    }

    public View getFirstMatch(final ItemOperator itemOperator) {
        final View[] viewArr = new View[1];
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if (!itemOperator.evaluate(itemInfo, view)) {
                    return false;
                }
                viewArr[0] = view;
                return true;
            }
        });
        return viewArr[0];
    }

    void clearDropTargets() {
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if (view instanceof DropTarget) {
                    Workspace.this.mDragController.removeDropTarget((DropTarget) view);
                    return false;
                }
                return false;
            }
        });
    }

    public void removeItemsByMatcher(ItemInfoMatcher itemInfoMatcher) {
        View view;
        for (CellLayout cellLayout : getWorkspaceAndHotseatCellLayouts()) {
            ShortcutAndWidgetContainer shortcutsAndWidgets = cellLayout.getShortcutsAndWidgets();
            LongArrayMap longArrayMap = new LongArrayMap();
            ArrayList arrayList = new ArrayList();
            for (int i = 0; i < shortcutsAndWidgets.getChildCount(); i++) {
                View childAt = shortcutsAndWidgets.getChildAt(i);
                if (childAt.getTag() instanceof ItemInfo) {
                    ItemInfo itemInfo = (ItemInfo) childAt.getTag();
                    arrayList.add(itemInfo);
                    longArrayMap.put(itemInfo.id, childAt);
                }
            }
            for (ItemInfo itemInfo2 : itemInfoMatcher.filterItemInfos(arrayList)) {
                View view2 = (View) longArrayMap.get(itemInfo2.id);
                if (view2 != 0) {
                    cellLayout.removeViewInLayout(view2);
                    if (view2 instanceof DropTarget) {
                        this.mDragController.removeDropTarget((DropTarget) view2);
                    }
                } else if (itemInfo2.container >= 0 && (view = (View) longArrayMap.get(itemInfo2.container)) != null) {
                    FolderInfo folderInfo = (FolderInfo) view.getTag();
                    folderInfo.prepareAutoUpdate();
                    folderInfo.remove((ShortcutInfo) itemInfo2, false);
                }
            }
        }
        stripEmptyScreens();
    }

    void mapOverItems(boolean z, ItemOperator itemOperator) {
        ArrayList<ShortcutAndWidgetContainer> allShortcutAndWidgetContainers = getAllShortcutAndWidgetContainers();
        int size = allShortcutAndWidgetContainers.size();
        for (int i = 0; i < size; i++) {
            ShortcutAndWidgetContainer shortcutAndWidgetContainer = allShortcutAndWidgetContainers.get(i);
            int childCount = shortcutAndWidgetContainer.getChildCount();
            for (int i2 = 0; i2 < childCount; i2++) {
                View childAt = shortcutAndWidgetContainer.getChildAt(i2);
                ItemInfo itemInfo = (ItemInfo) childAt.getTag();
                if (z && (itemInfo instanceof FolderInfo) && (childAt instanceof FolderIcon)) {
                    ArrayList<View> itemsInReadingOrder = ((FolderIcon) childAt).getFolder().getItemsInReadingOrder();
                    int size2 = itemsInReadingOrder.size();
                    for (int i3 = 0; i3 < size2; i3++) {
                        View view = itemsInReadingOrder.get(i3);
                        if (itemOperator.evaluate((ItemInfo) view.getTag(), view)) {
                            return;
                        }
                    }
                } else if (itemOperator.evaluate(itemInfo, childAt)) {
                    return;
                }
            }
        }
    }

    void updateShortcuts(ArrayList<ShortcutInfo> arrayList) {
        int size = arrayList.size();
        final HashSet hashSet = new HashSet(size);
        final HashSet hashSet2 = new HashSet();
        for (int i = 0; i < size; i++) {
            ShortcutInfo shortcutInfo = arrayList.get(i);
            hashSet.add(shortcutInfo);
            hashSet2.add(Long.valueOf(shortcutInfo.container));
        }
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if ((itemInfo instanceof ShortcutInfo) && (view instanceof BubbleTextView) && hashSet.contains(itemInfo)) {
                    ShortcutInfo shortcutInfo2 = (ShortcutInfo) itemInfo;
                    BubbleTextView bubbleTextView = (BubbleTextView) view;
                    Drawable icon = bubbleTextView.getIcon();
                    bubbleTextView.applyFromShortcutInfo(shortcutInfo2, shortcutInfo2.isPromise() != ((icon instanceof PreloadIconDrawable) && ((PreloadIconDrawable) icon).hasNotCompleted()));
                }
                return false;
            }
        });
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if ((itemInfo instanceof FolderInfo) && hashSet2.contains(Long.valueOf(itemInfo.id))) {
                    ((FolderInfo) itemInfo).itemsChanged(false);
                }
                return false;
            }
        });
    }

    public void updateIconBadges(final Set<PackageUserKey> set) {
        final PackageUserKey packageUserKey = new PackageUserKey(null, null);
        final HashSet hashSet = new HashSet();
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if ((itemInfo instanceof ShortcutInfo) && (view instanceof BubbleTextView) && packageUserKey.updateFromItemInfo(itemInfo) && set.contains(packageUserKey)) {
                    ((BubbleTextView) view).applyBadgeState(itemInfo, true);
                    hashSet.add(Long.valueOf(itemInfo.container));
                    return false;
                }
                return false;
            }
        });
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if ((itemInfo instanceof FolderInfo) && hashSet.contains(Long.valueOf(itemInfo.id)) && (view instanceof FolderIcon)) {
                    FolderBadgeInfo folderBadgeInfo = new FolderBadgeInfo();
                    Iterator<ShortcutInfo> it = ((FolderInfo) itemInfo).contents.iterator();
                    while (it.hasNext()) {
                        folderBadgeInfo.addBadgeInfo(Workspace.this.mLauncher.getBadgeInfoForItem(it.next()));
                    }
                    ((FolderIcon) view).setBadgeInfo(folderBadgeInfo);
                    return false;
                }
                return false;
            }
        });
    }

    public void removeAbandonedPromise(String str, UserHandle userHandle) {
        HashSet hashSet = new HashSet(1);
        hashSet.add(str);
        ItemInfoMatcher itemInfoMatcherOfPackages = ItemInfoMatcher.ofPackages(hashSet, userHandle);
        this.mLauncher.getModelWriter().deleteItemsFromDatabase(itemInfoMatcherOfPackages);
        removeItemsByMatcher(itemInfoMatcherOfPackages);
    }

    public void updateRestoreItems(final HashSet<ItemInfo> hashSet) {
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo itemInfo, View view) {
                if ((itemInfo instanceof ShortcutInfo) && (view instanceof BubbleTextView) && hashSet.contains(itemInfo)) {
                    ((BubbleTextView) view).applyPromiseState(false);
                } else if ((view instanceof PendingAppWidgetHostView) && (itemInfo instanceof LauncherAppWidgetInfo) && hashSet.contains(itemInfo)) {
                    ((PendingAppWidgetHostView) view).applyState();
                }
                return false;
            }
        });
    }

    public void widgetsRestored(final ArrayList<LauncherAppWidgetInfo> arrayList) {
        LauncherAppWidgetProviderInfo launcherAppWidgetInfo;
        if (!arrayList.isEmpty()) {
            DeferredWidgetRefresh deferredWidgetRefresh = new DeferredWidgetRefresh(arrayList, this.mLauncher.getAppWidgetHost());
            LauncherAppWidgetInfo launcherAppWidgetInfo2 = arrayList.get(0);
            if (launcherAppWidgetInfo2.hasRestoreFlag(1)) {
                launcherAppWidgetInfo = AppWidgetManagerCompat.getInstance(this.mLauncher).findProvider(launcherAppWidgetInfo2.providerName, launcherAppWidgetInfo2.user);
            } else {
                launcherAppWidgetInfo = AppWidgetManagerCompat.getInstance(this.mLauncher).getLauncherAppWidgetInfo(launcherAppWidgetInfo2.appWidgetId);
            }
            if (launcherAppWidgetInfo == null) {
                mapOverItems(false, new ItemOperator() {
                    @Override
                    public boolean evaluate(ItemInfo itemInfo, View view) {
                        if ((view instanceof PendingAppWidgetHostView) && arrayList.contains(itemInfo)) {
                            ((LauncherAppWidgetInfo) itemInfo).installProgress = 100;
                            ((PendingAppWidgetHostView) view).applyState();
                            return false;
                        }
                        return false;
                    }
                });
            } else {
                deferredWidgetRefresh.run();
            }
        }
    }

    void moveToDefaultScreen() {
        if (!workspaceInModalState() && getNextPage() != 0) {
            snapToPage(0);
        }
        View childAt = getChildAt(0);
        if (childAt != null) {
            childAt.requestFocus();
        }
    }

    @Override
    public int getExpectedHeight() {
        return (getMeasuredHeight() <= 0 || !this.mIsLayoutValid) ? this.mLauncher.getDeviceProfile().heightPx : getMeasuredHeight();
    }

    @Override
    public int getExpectedWidth() {
        return (getMeasuredWidth() <= 0 || !this.mIsLayoutValid) ? this.mLauncher.getDeviceProfile().widthPx : getMeasuredWidth();
    }

    @Override
    protected boolean canAnnouncePageDescription() {
        return Float.compare(this.mOverlayTranslation, 0.0f) == 0;
    }

    @Override
    protected String getCurrentPageDescription() {
        return getPageDescription(this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage);
    }

    private String getPageDescription(int i) {
        int childCount = getChildCount();
        int iIndexOf = this.mScreenOrder.indexOf(-201L);
        if (iIndexOf >= 0 && childCount > 1) {
            if (i == iIndexOf) {
                return getContext().getString(R.string.workspace_new_page);
            }
            childCount--;
        }
        return childCount == 0 ? getContext().getString(R.string.all_apps_home_button_label) : getContext().getString(R.string.workspace_scroll_format, Integer.valueOf(i + 1), Integer.valueOf(childCount));
    }

    @Override
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        target.gridX = itemInfo.cellX;
        target.gridY = itemInfo.cellY;
        target.pageIndex = getCurrentPage();
        target2.containerType = 1;
        if (itemInfo.container == -101) {
            target.rank = itemInfo.rank;
            target2.containerType = 2;
        } else if (itemInfo.container >= 0) {
            target2.containerType = 3;
        }
    }

    private class DeferredWidgetRefresh implements Runnable, LauncherAppWidgetHost.ProviderChangedListener {
        private final LauncherAppWidgetHost mHost;
        private final ArrayList<LauncherAppWidgetInfo> mInfos;
        private final Handler mHandler = new Handler();
        private boolean mRefreshPending = true;

        DeferredWidgetRefresh(ArrayList<LauncherAppWidgetInfo> arrayList, LauncherAppWidgetHost launcherAppWidgetHost) {
            this.mInfos = arrayList;
            this.mHost = launcherAppWidgetHost;
            this.mHost.addProviderChangeListener(this);
            this.mHandler.postDelayed(this, 10000L);
        }

        @Override
        public void run() {
            this.mHost.removeProviderChangeListener(this);
            this.mHandler.removeCallbacks(this);
            if (!this.mRefreshPending) {
                return;
            }
            this.mRefreshPending = false;
            final ArrayList arrayList = new ArrayList(this.mInfos.size());
            Workspace.this.mapOverItems(false, new ItemOperator() {
                @Override
                public final boolean evaluate(ItemInfo itemInfo, View view) {
                    return Workspace.DeferredWidgetRefresh.lambda$run$0(this.f$0, arrayList, itemInfo, view);
                }
            });
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                ((PendingAppWidgetHostView) it.next()).reInflate();
            }
        }

        public static boolean lambda$run$0(DeferredWidgetRefresh deferredWidgetRefresh, ArrayList arrayList, ItemInfo itemInfo, View view) {
            if ((view instanceof PendingAppWidgetHostView) && deferredWidgetRefresh.mInfos.contains(itemInfo)) {
                arrayList.add((PendingAppWidgetHostView) view);
                return false;
            }
            return false;
        }

        @Override
        public void notifyWidgetProvidersChanged() {
            run();
        }
    }

    private class StateTransitionListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener {
        private final LauncherState mToState;

        StateTransitionListener(LauncherState launcherState) {
            this.mToState = launcherState;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            Workspace.this.mTransitionProgress = valueAnimator.getAnimatedFraction();
        }

        @Override
        public void onAnimationStart(Animator animator) {
            Workspace.this.onStartStateTransition(this.mToState);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            Workspace.this.onEndStateTransition();
        }
    }
}
