package com.android.launcher3;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Choreographer;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.TextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DragController;
import com.android.launcher3.DropTarget;
import com.android.launcher3.FolderIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.PageIndicator;
import com.android.launcher3.SearchDropTargetBar;
import com.android.launcher3.Stats;
import com.android.launcher3.UninstallDropTarget;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.accessibility.OverviewScreenAccessibilityDelegate;
import com.android.launcher3.accessibility.WorkspaceAccessibilityHelper;
import com.android.launcher3.compat.AppWidgetManagerCompat;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.compat.UserHandleCompat;
import com.android.launcher3.util.LongArrayMap;
import com.android.launcher3.util.WallpaperUtils;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.mediatek.launcher3.LauncherHelper;
import com.mediatek.launcher3.LauncherLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Workspace extends PagedView implements DropTarget, DragSource, DragScroller, View.OnTouchListener, DragController.DragListener, LauncherTransitionable, ViewGroup.OnHierarchyChangeListener, Insettable, UninstallDropTarget.UninstallSource, LauncherAccessibilityDelegate.AccessibilityDragSource, Stats.LaunchSourceProvider {
    private static boolean ENFORCE_DRAG_EVENT_ORDER = false;
    static Rect mLandscapeCellLayoutMetrics = null;
    static Rect mPortraitCellLayoutMetrics = null;
    private static final Rect sTempRect = new Rect();
    boolean mAddNewPageOnDrag;
    private boolean mAddToExistingFolderOnDrop;
    private final Interpolator mAlphaInterpolator;
    boolean mAnimatingViewIntoPlace;
    private final Runnable mBindPages;
    private final Canvas mCanvas;
    boolean mChildrenLayersEnabled;
    private boolean mCreateUserFolderOnDrop;
    private float mCurrentScale;
    Launcher.CustomContentCallbacks mCustomContentCallbacks;
    private String mCustomContentDescription;
    private long mCustomContentShowTime;
    boolean mCustomContentShowing;
    private int mDefaultPage;
    private boolean mDeferDropAfterUninstall;
    boolean mDeferRemoveExtraEmptyScreen;
    Runnable mDeferredAction;
    Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
    private Point mDisplaySize;
    DragController mDragController;
    FolderIcon.FolderRingAnimator mDragFolderRingAnimator;
    private CellLayout.CellInfo mDragInfo;
    private int mDragMode;
    Bitmap mDragOutline;
    private FolderIcon mDragOverFolderIcon;
    private int mDragOverX;
    private int mDragOverY;
    private CellLayout mDragOverlappingLayout;
    private ShortcutAndWidgetContainer mDragSourceInternal;
    CellLayout mDragTargetLayout;
    float[] mDragViewVisualCenter;
    private CellLayout mDropToLayout;
    private final Alarm mFolderCreationAlarm;
    IconCache mIconCache;
    private boolean mInScrollArea;
    boolean mIsDragOccuring;
    private boolean mIsSwitchingState;
    private float mLastCustomContentScrollProgress;
    float mLastOverlaySroll;
    int mLastReorderX;
    int mLastReorderY;
    float mLastSetWallpaperOffsetSteps;
    Launcher mLauncher;
    Launcher.LauncherOverlay mLauncherOverlay;
    private LayoutTransition mLayoutTransition;
    private float mMaxDistanceForFolderCreation;
    int mNumPagesForWallpaperParallax;
    private int mOriginalDefaultPage;
    private HolographicOutlineHelper mOutlineHelper;
    private float mOverlayTranslation;
    private float mOverviewModeShrinkFactor;
    private View.AccessibilityDelegate mPagesAccessibilityDelegate;
    Runnable mRemoveEmptyScreenRunnable;
    private final Alarm mReorderAlarm;
    private final ArrayList<Integer> mRestoredPages;
    private SparseArray<Parcelable> mSavedStates;
    ArrayList<Long> mScreenOrder;
    boolean mScrollInteractionBegan;
    private SpringLoadedDragController mSpringLoadedDragController;
    private float mSpringLoadedShrinkFactor;
    boolean mStartedSendingScrollEvents;
    private State mState;
    private WorkspaceStateTransitionAnimation mStateTransitionAnimation;
    private boolean mStripScreensOnPageStopMoving;
    int[] mTargetCell;
    private int[] mTempCell;
    private float[] mTempCellLayoutCenterCoordinates;
    private int[] mTempEstimate;
    private Matrix mTempInverseMatrix;
    private Matrix mTempMatrix;
    private int[] mTempPt;
    private int[] mTempVisiblePagesRange;
    private final int[] mTempXY;
    private long mTouchDownTime;
    private float mTransitionProgress;
    private int mUnboundedScrollX;
    private boolean mUninstallSuccessful;
    boolean mWallpaperIsLiveWallpaper;
    final WallpaperManager mWallpaperManager;
    WallpaperOffsetInterpolator mWallpaperOffset;
    IBinder mWindowToken;
    private boolean mWorkspaceFadeInAdjacentScreens;
    LongArrayMap<CellLayout> mWorkspaceScreens;
    private float mXDown;
    private float mYDown;

    interface ItemOperator {
        boolean evaluate(ItemInfo itemInfo, View view, View view2);
    }

    enum State {
        NORMAL(SearchDropTargetBar.State.SEARCH_BAR, false),
        NORMAL_HIDDEN(SearchDropTargetBar.State.INVISIBLE_TRANSLATED, false),
        SPRING_LOADED(SearchDropTargetBar.State.DROP_TARGET, false),
        OVERVIEW(SearchDropTargetBar.State.INVISIBLE, true),
        OVERVIEW_HIDDEN(SearchDropTargetBar.State.INVISIBLE, true);

        public final SearchDropTargetBar.State searchDropTargetBarState;
        public final boolean shouldUpdateWidget;

        public static State[] valuesCustom() {
            return values();
        }

        State(SearchDropTargetBar.State searchBarState, boolean shouldUpdateWidget) {
            this.searchDropTargetBarState = searchBarState;
            this.shouldUpdateWidget = shouldUpdateWidget;
        }
    }

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mTouchDownTime = -1L;
        this.mCustomContentShowTime = -1L;
        this.mWorkspaceScreens = new LongArrayMap<>();
        this.mScreenOrder = new ArrayList<>();
        this.mDeferRemoveExtraEmptyScreen = false;
        this.mAddNewPageOnDrag = true;
        this.mTargetCell = new int[2];
        this.mDragOverX = -1;
        this.mDragOverY = -1;
        this.mLastCustomContentScrollProgress = -1.0f;
        this.mCustomContentDescription = "";
        this.mDragTargetLayout = null;
        this.mDragOverlappingLayout = null;
        this.mDropToLayout = null;
        this.mTempCell = new int[2];
        this.mTempPt = new int[2];
        this.mTempEstimate = new int[2];
        this.mDragViewVisualCenter = new float[2];
        this.mTempCellLayoutCenterCoordinates = new float[2];
        this.mTempInverseMatrix = new Matrix();
        this.mTempMatrix = new Matrix();
        this.mState = State.NORMAL;
        this.mIsSwitchingState = false;
        this.mAnimatingViewIntoPlace = false;
        this.mIsDragOccuring = false;
        this.mChildrenLayersEnabled = true;
        this.mStripScreensOnPageStopMoving = false;
        this.mInScrollArea = false;
        this.mDragOutline = null;
        this.mTempXY = new int[2];
        this.mTempVisiblePagesRange = new int[2];
        this.mLastSetWallpaperOffsetSteps = 0.0f;
        this.mDisplaySize = new Point();
        this.mFolderCreationAlarm = new Alarm();
        this.mReorderAlarm = new Alarm();
        this.mDragFolderRingAnimator = null;
        this.mDragOverFolderIcon = null;
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mCanvas = new Canvas();
        this.mDragMode = 0;
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
        this.mRestoredPages = new ArrayList<>();
        this.mLastOverlaySroll = 0.0f;
        this.mBindPages = new Runnable() {
            @Override
            public void run() {
                Workspace.this.mLauncher.getModel().bindRemainingSynchronousPages();
            }
        };
        this.mAlphaInterpolator = new DecelerateInterpolator(3.0f);
        this.mOutlineHelper = HolographicOutlineHelper.obtain(context);
        this.mLauncher = (Launcher) context;
        this.mStateTransitionAnimation = new WorkspaceStateTransitionAnimation(this.mLauncher, this);
        Resources res = getResources();
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        this.mWorkspaceFadeInAdjacentScreens = grid.shouldFadeAdjacentWorkspaceScreens();
        this.mFadeInAdjacentScreens = false;
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
        this.mSpringLoadedShrinkFactor = res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        this.mOverviewModeShrinkFactor = res.getInteger(R.integer.config_workspaceOverviewShrinkPercentage) / 100.0f;
        int i = a.getInt(0, 1);
        this.mDefaultPage = i;
        this.mOriginalDefaultPage = i;
        a.recycle();
        setOnHierarchyChangeListener(this);
        setHapticFeedbackEnabled(false);
        initWorkspace();
        setMotionEventSplittingEnabled(true);
    }

    @Override
    public void setInsets(Rect insets) {
        this.mInsets.set(insets);
        CellLayout customScreen = getScreenWithId(-301L);
        if (customScreen == null) {
            return;
        }
        KeyEvent.Callback childAt = customScreen.getShortcutsAndWidgets().getChildAt(0);
        if (!(childAt instanceof Insettable)) {
            return;
        }
        ((Insettable) childAt).setInsets(this.mInsets);
    }

    public int[] estimateItemSize(ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getChildCount() > 0) {
            CellLayout cl = (CellLayout) getChildAt(numCustomPages());
            Rect r = estimateItemPosition(cl, 0, 0, itemInfo.spanX, itemInfo.spanY);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] = (int) (size[0] * this.mSpringLoadedShrinkFactor);
                size[1] = (int) (size[1] * this.mSpringLoadedShrinkFactor);
            }
            return size;
        }
        size[0] = Integer.MAX_VALUE;
        size[1] = Integer.MAX_VALUE;
        return size;
    }

    public Rect estimateItemPosition(CellLayout cl, int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDragStart: source = " + source + ", info = " + info + ", dragAction = " + dragAction);
        }
        if (ENFORCE_DRAG_EVENT_ORDER) {
            enfoceDragParity("onDragStart", 0, 0);
        }
        this.mIsDragOccuring = true;
        updateChildrenLayersEnabled(false);
        this.mLauncher.lockScreenOrientation();
        this.mLauncher.onInteractionBegin();
        InstallShortcutReceiver.enableInstallQueue();
        if (!this.mAddNewPageOnDrag) {
            return;
        }
        this.mDeferRemoveExtraEmptyScreen = false;
        addExtraEmptyScreenOnDrag();
    }

    public void setAddNewPageOnDrag(boolean addPage) {
        this.mAddNewPageOnDrag = addPage;
    }

    public void deferRemoveExtraEmptyScreen() {
        this.mDeferRemoveExtraEmptyScreen = true;
    }

    @Override
    public void onDragEnd() {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDragEnd: mIsDragOccuring = " + this.mIsDragOccuring);
        }
        if (ENFORCE_DRAG_EVENT_ORDER) {
            enfoceDragParity("onDragEnd", 0, 0);
        }
        if (!this.mDeferRemoveExtraEmptyScreen) {
            removeExtraEmptyScreen(true, this.mDragSourceInternal != null);
        }
        this.mIsDragOccuring = false;
        updateChildrenLayersEnabled(false);
        this.mLauncher.unlockScreenOrientation(false);
        InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
        this.mDragSourceInternal = null;
        this.mLauncher.onInteractionEnd();
    }

    protected void initWorkspace() {
        this.mCurrentPage = this.mDefaultPage;
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        this.mIconCache = app.getIconCache();
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);
        setMinScale(this.mOverviewModeShrinkFactor);
        setupLayoutTransition();
        this.mWallpaperOffset = new WallpaperOffsetInterpolator();
        Display display = this.mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(this.mDisplaySize);
        this.mMaxDistanceForFolderCreation = grid.iconSizePx * 0.55f;
        setWallpaperDimension();
        setEdgeGlowColor(getResources().getColor(R.color.workspace_edge_effect_color));
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
    public void onChildViewAdded(View parent, View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = (CellLayout) child;
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);
        cl.setImportantForAccessibility(2);
        super.onChildViewAdded(parent, child);
    }

    @Override
    protected boolean shouldDrawChild(View child) {
        CellLayout cl = (CellLayout) child;
        if (super.shouldDrawChild(child)) {
            return this.mIsSwitchingState || cl.getShortcutsAndWidgets().getAlpha() > 0.0f || cl.getBackgroundAlpha() > 0.0f;
        }
        return false;
    }

    public Folder getOpenFolder() {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        int count = dragLayer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = dragLayer.getChildAt(i);
            if (child instanceof Folder) {
                Folder folder = (Folder) child;
                if (folder.getInfo().opened) {
                    return folder;
                }
            }
        }
        return null;
    }

    boolean isTouchActive() {
        return this.mTouchState != 0;
    }

    public void removeAllWorkspaceScreens() {
        disableLayoutTransitions();
        if (hasCustomContent()) {
            removeCustomContentPage();
        }
        removeAllViews();
        this.mScreenOrder.clear();
        this.mWorkspaceScreens.clear();
        enableLayoutTransitions();
    }

    public long insertNewWorkspaceScreenBeforeEmptyScreen(long screenId) {
        int insertIndex = this.mScreenOrder.indexOf(-201L);
        if (insertIndex < 0) {
            insertIndex = this.mScreenOrder.size();
        }
        return insertNewWorkspaceScreen(screenId, insertIndex);
    }

    public long insertNewWorkspaceScreen(long screenId) {
        return insertNewWorkspaceScreen(screenId, getChildCount());
    }

    public long insertNewWorkspaceScreen(long screenId, int insertIndex) {
        if (this.mWorkspaceScreens.containsKey(screenId)) {
            throw new RuntimeException("Screen id " + screenId + " already exists!");
        }
        CellLayout newScreen = (CellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, (ViewGroup) this, false);
        newScreen.setOnLongClickListener(this.mLongClickListener);
        newScreen.setOnClickListener(this.mLauncher);
        newScreen.setSoundEffectsEnabled(false);
        this.mWorkspaceScreens.put(screenId, newScreen);
        this.mScreenOrder.add(insertIndex, Long.valueOf(screenId));
        addView(newScreen, insertIndex);
        LauncherAccessibilityDelegate delegate = LauncherAppState.getInstance().getAccessibilityDelegate();
        if (delegate != null && delegate.isInAccessibleDrag()) {
            newScreen.enableAccessibleDrag(true, 2);
        }
        return screenId;
    }

    public void createCustomContentContainer() {
        CellLayout customScreen = (CellLayout) this.mLauncher.getLayoutInflater().inflate(R.layout.workspace_screen, (ViewGroup) this, false);
        customScreen.disableDragTarget();
        customScreen.disableJailContent();
        this.mWorkspaceScreens.put(-301L, customScreen);
        this.mScreenOrder.add(0, -301L);
        customScreen.setPadding(0, 0, 0, 0);
        addFullScreenPage(customScreen);
        this.mDefaultPage = this.mOriginalDefaultPage + 1;
        if (this.mRestorePage != -1001) {
            this.mRestorePage++;
        } else {
            setCurrentPage(getCurrentPage() + 1);
        }
    }

    public void removeCustomContentPage() {
        CellLayout customScreen = getScreenWithId(-301L);
        if (customScreen == null) {
            throw new RuntimeException("Expected custom content screen to exist");
        }
        this.mWorkspaceScreens.remove(-301L);
        this.mScreenOrder.remove((Object) (-301L));
        customScreen.clear();
        removeView(customScreen);
        if (this.mCustomContentCallbacks != null) {
            this.mCustomContentCallbacks.onScrollProgressChanged(0.0f);
            this.mCustomContentCallbacks.onHide();
        }
        this.mCustomContentCallbacks = null;
        this.mDefaultPage = this.mOriginalDefaultPage - 1;
        if (this.mRestorePage != -1001) {
            this.mRestorePage--;
        } else {
            setCurrentPage(getCurrentPage() - 1);
        }
    }

    public void addExtraEmptyScreenOnDrag() {
        boolean lastChildOnScreen = false;
        boolean childOnFinalScreen = false;
        this.mRemoveEmptyScreenRunnable = null;
        if (this.mDragSourceInternal != null) {
            if (this.mDragSourceInternal.getChildCount() == 1) {
                lastChildOnScreen = true;
            }
            CellLayout cl = (CellLayout) this.mDragSourceInternal.getParent();
            if (indexOfChild(cl) == getChildCount() - 1) {
                childOnFinalScreen = true;
            }
        }
        if ((lastChildOnScreen && childOnFinalScreen) || this.mWorkspaceScreens.containsKey(-201L)) {
            return;
        }
        insertNewWorkspaceScreen(-201L);
    }

    public boolean addExtraEmptyScreen() {
        if (!this.mWorkspaceScreens.containsKey(-201L)) {
            insertNewWorkspaceScreen(-201L);
            return true;
        }
        return false;
    }

    private void convertFinalScreenToEmptyScreenIfNecessary() {
        if (this.mLauncher.isWorkspaceLoading()) {
            Launcher.addDumpLog("Launcher.Workspace", "    - workspace loading, skip", true);
            return;
        }
        if (hasExtraEmptyScreen() || this.mScreenOrder.size() == 0) {
            return;
        }
        long finalScreenId = this.mScreenOrder.get(this.mScreenOrder.size() - 1).longValue();
        if (finalScreenId == -301) {
            return;
        }
        CellLayout finalScreen = this.mWorkspaceScreens.get(finalScreenId);
        if (finalScreen.getShortcutsAndWidgets().getChildCount() != 0 || finalScreen.isDropPending()) {
            return;
        }
        this.mWorkspaceScreens.remove(finalScreenId);
        this.mScreenOrder.remove(Long.valueOf(finalScreenId));
        this.mWorkspaceScreens.put(-201L, finalScreen);
        this.mScreenOrder.add(-201L);
        this.mLauncher.getModel().updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
    }

    public void removeExtraEmptyScreen(boolean animate, boolean stripEmptyScreens) {
        removeExtraEmptyScreenDelayed(animate, null, 0, stripEmptyScreens);
    }

    public void removeExtraEmptyScreenDelayed(final boolean animate, final Runnable onComplete, int delay, final boolean stripEmptyScreens) {
        if (this.mLauncher.isWorkspaceLoading()) {
            Launcher.addDumpLog("Launcher.Workspace", "    - workspace loading, skip", true);
            return;
        }
        if (delay > 0) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    Workspace.this.removeExtraEmptyScreenDelayed(animate, onComplete, 0, stripEmptyScreens);
                }
            }, delay);
            return;
        }
        convertFinalScreenToEmptyScreenIfNecessary();
        if (hasExtraEmptyScreen()) {
            int emptyIndex = this.mScreenOrder.indexOf(-201L);
            if (getNextPage() == emptyIndex) {
                snapToPage(getNextPage() - 1, 400);
                fadeAndRemoveEmptyScreen(400, 150, onComplete, stripEmptyScreens);
                return;
            } else {
                snapToPage(getNextPage(), 0);
                fadeAndRemoveEmptyScreen(0, 150, onComplete, stripEmptyScreens);
                return;
            }
        }
        if (stripEmptyScreens) {
            stripEmptyScreens();
        }
        if (onComplete == null) {
            return;
        }
        onComplete.run();
    }

    private void fadeAndRemoveEmptyScreen(int delay, int duration, final Runnable onComplete, final boolean stripEmptyScreens) {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("alpha", 0.0f);
        PropertyValuesHolder bgAlpha = PropertyValuesHolder.ofFloat("backgroundAlpha", 0.0f);
        final CellLayout cl = this.mWorkspaceScreens.get(-201L);
        this.mRemoveEmptyScreenRunnable = new Runnable() {
            @Override
            public void run() {
                if (!Workspace.this.hasExtraEmptyScreen()) {
                    return;
                }
                Workspace.this.mWorkspaceScreens.remove(-201L);
                Workspace.this.mScreenOrder.remove((Object) (-201L));
                Workspace.this.removeView(cl);
                if (!stripEmptyScreens) {
                    return;
                }
                Workspace.this.stripEmptyScreens();
            }
        };
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(cl, alpha, bgAlpha);
        oa.setDuration(duration);
        oa.setStartDelay(delay);
        oa.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (Workspace.this.mRemoveEmptyScreenRunnable != null) {
                    Workspace.this.mRemoveEmptyScreenRunnable.run();
                }
                if (onComplete == null) {
                    return;
                }
                onComplete.run();
            }
        });
        oa.start();
    }

    public boolean hasExtraEmptyScreen() {
        int nScreens = getChildCount();
        return this.mWorkspaceScreens.containsKey(-201L) && nScreens - numCustomPages() > 1;
    }

    public long commitExtraEmptyScreen() {
        if (this.mLauncher.isWorkspaceLoading()) {
            Launcher.addDumpLog("Launcher.Workspace", "    - workspace loading, skip", true);
            return -1L;
        }
        int index = getPageIndexForScreenId(-201L);
        CellLayout cl = this.mWorkspaceScreens.get(-201L);
        this.mWorkspaceScreens.remove(-201L);
        this.mScreenOrder.remove((Object) (-201L));
        long newId = LauncherAppState.getLauncherProvider().generateNewScreenId();
        this.mWorkspaceScreens.put(newId, cl);
        this.mScreenOrder.add(Long.valueOf(newId));
        if (getPageIndicator() != null) {
            getPageIndicator().updateMarker(index, getPageIndicatorMarker(index));
        }
        this.mLauncher.getModel().updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        return newId;
    }

    public CellLayout getScreenWithId(long screenId) {
        CellLayout layout = this.mWorkspaceScreens.get(screenId);
        return layout;
    }

    public long getIdForScreen(CellLayout layout) {
        int index = this.mWorkspaceScreens.indexOfValue(layout);
        if (index != -1) {
            return this.mWorkspaceScreens.keyAt(index);
        }
        return -1L;
    }

    public int getPageIndexForScreenId(long screenId) {
        return indexOfChild(this.mWorkspaceScreens.get(screenId));
    }

    public long getScreenIdForPageIndex(int index) {
        if (index >= 0 && index < this.mScreenOrder.size()) {
            return this.mScreenOrder.get(index).longValue();
        }
        return -1L;
    }

    public ArrayList<Long> getScreenOrder() {
        return this.mScreenOrder;
    }

    public void stripEmptyScreens() {
        if (this.mLauncher.isWorkspaceLoading()) {
            Launcher.addDumpLog("Launcher.Workspace", "    - workspace loading, skip", true);
            return;
        }
        if (isPageMoving()) {
            this.mStripScreensOnPageStopMoving = true;
            return;
        }
        int currentPage = getNextPage();
        ArrayList<Long> removeScreens = new ArrayList<>();
        int total = this.mWorkspaceScreens.size();
        for (int i = 0; i < total; i++) {
            long id = this.mWorkspaceScreens.keyAt(i);
            CellLayout cl = this.mWorkspaceScreens.valueAt(i);
            if (id >= 0 && cl.getShortcutsAndWidgets().getChildCount() == 0) {
                removeScreens.add(Long.valueOf(id));
            }
        }
        LauncherAccessibilityDelegate delegate = LauncherAppState.getInstance().getAccessibilityDelegate();
        int minScreens = numCustomPages() + 1;
        int pageShift = 0;
        for (Long id2 : removeScreens) {
            CellLayout cl2 = this.mWorkspaceScreens.get(id2.longValue());
            this.mWorkspaceScreens.remove(id2.longValue());
            this.mScreenOrder.remove(id2);
            if (getChildCount() > minScreens) {
                if (indexOfChild(cl2) < currentPage) {
                    pageShift++;
                }
                if (delegate != null && delegate.isInAccessibleDrag()) {
                    cl2.enableAccessibleDrag(false, 2);
                }
                removeView(cl2);
            } else {
                this.mRemoveEmptyScreenRunnable = null;
                this.mWorkspaceScreens.put(-201L, cl2);
                this.mScreenOrder.add(-201L);
            }
        }
        if (!removeScreens.isEmpty()) {
            this.mLauncher.getModel().updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        }
        if (pageShift < 0) {
            return;
        }
        setCurrentPage(currentPage - pageShift);
    }

    void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, false, false);
    }

    void addInScreenFromBind(View child, long container, long screenId, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, false, true);
    }

    void addInScreen(View child, long container, long screenId, int x, int y, int spanX, int spanY, boolean insert) {
        addInScreen(child, container, screenId, x, y, spanX, spanY, insert, false);
    }

    void addInScreen(View view, long container, long screenId, int x, int y, int spanX, int spanY, boolean insert, boolean computeXYFromRank) {
        CellLayout layout;
        CellLayout.LayoutParams lp;
        if (container == -100 && getScreenWithId(screenId) == null) {
            Log.e("Launcher.Workspace", "Skipping child, screenId " + screenId + " not found");
            new Throwable().printStackTrace();
            return;
        }
        if (screenId == -201) {
            throw new RuntimeException("Screen id should not be EXTRA_EMPTY_SCREEN_ID");
        }
        if (container == -101) {
            layout = this.mLauncher.getHotseat().getLayout();
            view.setOnKeyListener(new HotseatIconKeyEventListener());
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(false);
            }
            if (computeXYFromRank) {
                x = this.mLauncher.getHotseat().getCellXFromOrder((int) screenId);
                y = this.mLauncher.getHotseat().getCellYFromOrder((int) screenId);
            } else {
                screenId = this.mLauncher.getHotseat().getOrderInHotseat(x, y);
            }
        } else {
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(true);
            }
            layout = getScreenWithId(screenId);
            view.setOnKeyListener(new IconKeyEventListener());
        }
        ViewGroup.LayoutParams genericLp = view.getLayoutParams();
        if (genericLp == null || !(genericLp instanceof CellLayout.LayoutParams)) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp = (CellLayout.LayoutParams) genericLp;
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }
        if (spanX < 0 && spanY < 0) {
            lp.isLockedToGrid = false;
        }
        ItemInfo info = (ItemInfo) view.getTag();
        int childId = this.mLauncher.getViewIdForItem(info);
        boolean markCellsAsOccupied = !(view instanceof Folder);
        if (markCellsAsOccupied && !this.mIsDragOccuring && container == -100) {
            for (int i = lp.cellX; i < lp.cellX + lp.cellHSpan; i++) {
                for (int j = lp.cellY; j < lp.cellY + lp.cellVSpan; j++) {
                    if (i >= layout.getCountX() || j >= layout.getCountY()) {
                        Launcher.addDumpLog("Launcher.Workspace", "Position exceeds the bound of this CellLayout.i:" + i + ",layout.getCountX():" + layout.getCountX() + ",j:" + j + ",layout.getCountY():" + layout.getCountY(), true);
                        return;
                    } else {
                        if (layout.isOccupied(i, j)) {
                            Launcher.addDumpLog("Launcher.Workspace", "layout.isOccupied, screenId:" + screenId + ",x:" + i + ",y:" + j + ",lp.cellHSpan:" + lp.cellHSpan + ", lp.cellVSpan" + lp.cellVSpan, true);
                            return;
                        }
                    }
                }
            }
        }
        if (!layout.addViewToCellLayout(view, insert ? 0 : -1, childId, lp, markCellsAsOccupied)) {
            Launcher.addDumpLog("Launcher.Workspace", "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout", true);
        }
        if (!(view instanceof Folder)) {
            view.setHapticFeedbackEnabled(false);
            view.setOnLongClickListener(this.mLongClickListener);
        }
        if (view instanceof DropTarget) {
            this.mDragController.addDropTarget((DropTarget) view);
        }
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouch(View v, MotionEvent event) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("Launcher.Workspace", "onTouch: v = " + v + ", event = " + event + ", isFinishedSwitchingState() = " + isFinishedSwitchingState() + ", mState = " + this.mState + ", mScrollX = " + getScrollX());
        }
        if (workspaceInModalState() || !isFinishedSwitchingState()) {
            return true;
        }
        return (workspaceInModalState() || indexOfChild(v) == this.mCurrentPage) ? false : true;
    }

    public boolean isSwitchingState() {
        return this.mIsSwitchingState;
    }

    public boolean isFinishedSwitchingState() {
        return !this.mIsSwitchingState || this.mTransitionProgress > 0.5f;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        this.mLauncher.onWindowVisibilityChanged(visibility);
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (workspaceInModalState() || !isFinishedSwitchingState()) {
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (LauncherLog.DEBUG_MOTION) {
            LauncherLog.d("Launcher.Workspace", "onInterceptTouchEvent: ev = " + ev + ", mScrollX = " + getScrollX());
        }
        switch (ev.getAction() & 255) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                LauncherHelper.beginSection("Workspace.ACTION_DOWN");
                this.mXDown = ev.getX();
                this.mYDown = ev.getY();
                this.mTouchDownTime = System.currentTimeMillis();
                LauncherHelper.endSection();
                break;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 6:
                LauncherHelper.beginSection("Workspace.ACTION_UP");
                if (this.mTouchState == 0) {
                    CellLayout currentPage = (CellLayout) getChildAt(this.mCurrentPage);
                    if (currentPage != null) {
                        onWallpaperTap(ev);
                    }
                }
                LauncherHelper.endSection();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (getScreenIdForPageIndex(getCurrentPage()) == -301 && this.mCustomContentCallbacks != null && !this.mCustomContentCallbacks.isScrollingAllowed()) {
            return false;
        }
        return super.onGenericMotionEvent(event);
    }

    protected void reinflateWidgetsIfNecessary() {
        int clCount = getChildCount();
        for (int i = 0; i < clCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
            int itemCount = swc.getChildCount();
            for (int j = 0; j < itemCount; j++) {
                View v = swc.getChildAt(j);
                if (v != null && (v.getTag() instanceof LauncherAppWidgetInfo)) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                    if (lahv != null && lahv.isReinflateRequired()) {
                        this.mLauncher.removeItem(lahv, info, false);
                        this.mLauncher.bindAppWidget(info);
                    }
                }
            }
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (isFinishedSwitchingState()) {
            float deltaX = ev.getX() - this.mXDown;
            float absDeltaX = Math.abs(deltaX);
            float absDeltaY = Math.abs(ev.getY() - this.mYDown);
            if (Float.compare(absDeltaX, 0.0f) == 0) {
                return;
            }
            float slope = absDeltaY / absDeltaX;
            float theta = (float) Math.atan(slope);
            if (absDeltaX > this.mTouchSlop || absDeltaY > this.mTouchSlop) {
                cancelCurrentPageLongPress();
            }
            boolean passRightSwipesToCustomContent = this.mTouchDownTime - this.mCustomContentShowTime > 200;
            boolean swipeInIgnoreDirection = !this.mIsRtl ? deltaX <= 0.0f : deltaX >= 0.0f;
            boolean onCustomContentScreen = getScreenIdForPageIndex(getCurrentPage()) == -301;
            if (swipeInIgnoreDirection && onCustomContentScreen && passRightSwipesToCustomContent) {
                return;
            }
            if ((!onCustomContentScreen || this.mCustomContentCallbacks == null || this.mCustomContentCallbacks.isScrollingAllowed()) && theta <= 1.0471976f) {
                if (theta > 0.5235988f) {
                    float extraRatio = (float) Math.sqrt((theta - 0.5235988f) / 0.5235988f);
                    super.determineScrollingStart(ev, (4.0f * extraRatio) + 1.0f);
                } else {
                    super.determineScrollingStart(ev, 0.5f);
                }
            }
        }
    }

    @Override
    protected void onPageBeginMoving() {
        super.onPageBeginMoving();
        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else if (this.mNextPage != -1) {
            enableChildrenCache(this.mCurrentPage, this.mNextPage);
        } else {
            enableChildrenCache(this.mCurrentPage - 1, this.mCurrentPage + 1);
        }
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }
        if (this.mDragController.isDragging() && workspaceInModalState()) {
            this.mDragController.forceTouchMove();
        }
        if (this.mDelayedResizeRunnable != null) {
            this.mDelayedResizeRunnable.run();
            this.mDelayedResizeRunnable = null;
        }
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
            this.mDelayedSnapToPageRunnable = null;
        }
        if (!this.mStripScreensOnPageStopMoving) {
            return;
        }
        stripEmptyScreens();
        this.mStripScreensOnPageStopMoving = false;
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
        if (!this.mStartedSendingScrollEvents) {
            return;
        }
        this.mStartedSendingScrollEvents = false;
        this.mLauncherOverlay.onScrollInteractionEnd();
    }

    @Override
    protected int getUnboundedScrollX() {
        if (isScrollingOverlay()) {
            return this.mUnboundedScrollX;
        }
        return super.getUnboundedScrollX();
    }

    private boolean isScrollingOverlay() {
        if (this.mLauncherOverlay == null) {
            return false;
        }
        if (!this.mIsRtl || this.mUnboundedScrollX <= this.mMaxScrollX) {
            return !this.mIsRtl && this.mUnboundedScrollX < 0;
        }
        return true;
    }

    @Override
    protected void snapToDestination() {
        if (isScrollingOverlay()) {
            int finalScroll = this.mIsRtl ? this.mMaxScrollX : 0;
            this.mWasInOverscroll = false;
            scrollTo(finalScroll, getScrollY());
            return;
        }
        super.snapToDestination();
    }

    @Override
    public void scrollTo(int x, int y) {
        this.mUnboundedScrollX = x;
        super.scrollTo(x, y);
    }

    @Override
    protected void overScroll(float amount) {
        boolean shouldOverScroll;
        boolean shouldScrollOverlay;
        boolean shouldZeroOverlay;
        if (amount <= 0.0f && (!hasCustomContent() || this.mIsRtl)) {
            shouldOverScroll = true;
        } else {
            shouldOverScroll = amount >= 0.0f && !(hasCustomContent() && this.mIsRtl);
        }
        if (this.mLauncherOverlay == null) {
            shouldScrollOverlay = false;
        } else if (amount > 0.0f || this.mIsRtl) {
            shouldScrollOverlay = amount >= 0.0f ? this.mIsRtl : false;
        } else {
            shouldScrollOverlay = true;
        }
        if (this.mLauncherOverlay == null || this.mLastOverlaySroll == 0.0f) {
            shouldZeroOverlay = false;
        } else if (amount < 0.0f || this.mIsRtl) {
            shouldZeroOverlay = amount <= 0.0f ? this.mIsRtl : false;
        } else {
            shouldZeroOverlay = true;
        }
        if (shouldScrollOverlay) {
            if (!this.mStartedSendingScrollEvents && this.mScrollInteractionBegan) {
                this.mStartedSendingScrollEvents = true;
                this.mLauncherOverlay.onScrollInteractionBegin();
            }
            this.mLastOverlaySroll = Math.abs(amount / getViewportWidth());
            this.mLauncherOverlay.onScrollChange(this.mLastOverlaySroll, this.mIsRtl);
        } else if (shouldOverScroll) {
            dampedOverScroll(amount);
        }
        if (!shouldZeroOverlay) {
            return;
        }
        this.mLauncherOverlay.onScrollChange(0.0f, this.mIsRtl);
    }

    @Override
    protected Matrix getPageShiftMatrix() {
        if (Float.compare(this.mOverlayTranslation, 0.0f) != 0) {
            this.mTempMatrix.set(getMatrix());
            this.mTempMatrix.postTranslate(-this.mOverlayTranslation, 0.0f);
            return this.mTempMatrix;
        }
        return super.getPageShiftMatrix();
    }

    @Override
    protected void getEdgeVerticalPostion(int[] pos) {
        View child = getChildAt(getPageCount() - 1);
        pos[0] = child.getTop();
        pos[1] = child.getBottom();
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        if (hasCustomContent() && getNextPage() == 0 && !this.mCustomContentShowing) {
            this.mCustomContentShowing = true;
            if (this.mCustomContentCallbacks == null) {
                return;
            }
            this.mCustomContentCallbacks.onShow(false);
            this.mCustomContentShowTime = System.currentTimeMillis();
            return;
        }
        if (!hasCustomContent() || getNextPage() == 0 || !this.mCustomContentShowing) {
            return;
        }
        this.mCustomContentShowing = false;
        if (this.mCustomContentCallbacks == null) {
            return;
        }
        this.mCustomContentCallbacks.onHide();
    }

    protected Launcher.CustomContentCallbacks getCustomContentCallbacks() {
        return this.mCustomContentCallbacks;
    }

    protected void setWallpaperDimension() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "setWallpaperDimension");
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            public Void doInBackground(Void... args) {
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("Launcher.Workspace", "setWallpaperDimension.doInBackground");
                }
                SharedPreferences sp = Workspace.this.mLauncher.getSharedPreferences("com.android.launcher3.WallpaperCropActivity", 4);
                WallpaperUtils.suggestWallpaperDimension(Workspace.this.mLauncher.getResources(), sp, Workspace.this.mLauncher.getWindowManager(), Workspace.this.mWallpaperManager, Workspace.this.mLauncher.overrideWallpaperDimensions());
                return null;
            }
        }.executeOnExecutor(Utilities.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        snapToPage(whichPage, 950, r);
    }

    protected void snapToPage(int whichPage, int duration, Runnable r) {
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
        }
        this.mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, duration);
    }

    public void snapToScreenId(long screenId) {
        snapToScreenId(screenId, null);
    }

    protected void snapToScreenId(long screenId, Runnable r) {
        snapToPage(getPageIndexForScreenId(screenId), r);
    }

    class WallpaperOffsetInterpolator implements Choreographer.FrameCallback {
        boolean mAnimating;
        float mAnimationStartOffset;
        long mAnimationStartTime;
        int mNumScreens;
        boolean mWaitingForUpdate;
        float mFinalOffset = 0.0f;
        float mCurrentOffset = 0.5f;
        private final int ANIMATION_DURATION = 250;
        private final int MIN_PARALLAX_PAGE_SPAN = 3;
        Choreographer mChoreographer = Choreographer.getInstance();
        Interpolator mInterpolator = new DecelerateInterpolator(1.5f);

        public WallpaperOffsetInterpolator() {
        }

        @Override
        public void doFrame(long frameTimeNanos) {
            updateOffset(false);
        }

        private void updateOffset(boolean force) {
            if (!this.mWaitingForUpdate && !force) {
                return;
            }
            this.mWaitingForUpdate = false;
            if (!computeScrollOffset() || Workspace.this.mWindowToken == null) {
                return;
            }
            try {
                Workspace.this.mWallpaperManager.setWallpaperOffsets(Workspace.this.mWindowToken, Workspace.this.mWallpaperOffset.getCurrX(), 0.5f);
                setWallpaperOffsetSteps();
            } catch (IllegalArgumentException e) {
                Log.e("Launcher.Workspace", "Error updating wallpaper offset: " + e);
            }
        }

        public boolean computeScrollOffset() {
            float oldOffset = this.mCurrentOffset;
            if (this.mAnimating) {
                long durationSinceAnimation = System.currentTimeMillis() - this.mAnimationStartTime;
                float t0 = durationSinceAnimation / 250.0f;
                float t1 = this.mInterpolator.getInterpolation(t0);
                this.mCurrentOffset = this.mAnimationStartOffset + ((this.mFinalOffset - this.mAnimationStartOffset) * t1);
                if (this.mCurrentOffset > 1.0f) {
                    this.mCurrentOffset = this.mFinalOffset;
                }
                this.mAnimating = durationSinceAnimation < 250;
            } else {
                this.mCurrentOffset = this.mFinalOffset;
            }
            if (Math.abs(this.mCurrentOffset - this.mFinalOffset) > 1.0E-7f) {
                scheduleUpdate();
            }
            return Math.abs(oldOffset - this.mCurrentOffset) > 1.0E-7f;
        }

        public float wallpaperOffsetForScroll(int scroll) {
            int parallaxPageSpan;
            int numScrollingPages = getNumScreensExcludingEmptyAndCustom();
            if (Workspace.this.mWallpaperIsLiveWallpaper) {
                parallaxPageSpan = numScrollingPages - 1;
            } else {
                parallaxPageSpan = Math.max(3, numScrollingPages - 1);
            }
            Workspace.this.mNumPagesForWallpaperParallax = parallaxPageSpan;
            if (Workspace.this.getChildCount() <= 1) {
                if (Workspace.this.mIsRtl) {
                    return 1.0f - (1.0f / Workspace.this.mNumPagesForWallpaperParallax);
                }
                return 0.0f;
            }
            int emptyExtraPages = numEmptyScreensToIgnore();
            int firstIndex = Workspace.this.numCustomPages();
            int lastIndex = (Workspace.this.getChildCount() - 1) - emptyExtraPages;
            if (Workspace.this.mIsRtl) {
                firstIndex = lastIndex;
                lastIndex = firstIndex;
            }
            int firstPageScrollX = Workspace.this.getScrollForPage(firstIndex);
            int scrollRange = Workspace.this.getScrollForPage(lastIndex) - firstPageScrollX;
            if (scrollRange == 0) {
                return 0.0f;
            }
            int adjustedScroll = (scroll - firstPageScrollX) - Workspace.this.getLayoutTransitionOffsetForPage(0);
            float offset = Math.max(0.0f, Math.min(1.0f, adjustedScroll / scrollRange));
            if (!Workspace.this.mWallpaperIsLiveWallpaper && numScrollingPages < 3 && Workspace.this.mIsRtl) {
                return (((parallaxPageSpan - numScrollingPages) + 1) * offset) / parallaxPageSpan;
            }
            return ((numScrollingPages - 1) * offset) / parallaxPageSpan;
        }

        private float wallpaperOffsetForCurrentScroll() {
            return wallpaperOffsetForScroll(Workspace.this.getScrollX());
        }

        private int numEmptyScreensToIgnore() {
            int numScrollingPages = Workspace.this.getChildCount() - Workspace.this.numCustomPages();
            if (numScrollingPages >= 3 && Workspace.this.hasExtraEmptyScreen()) {
                return 1;
            }
            return 0;
        }

        private int getNumScreensExcludingEmptyAndCustom() {
            int numScrollingPages = (Workspace.this.getChildCount() - numEmptyScreensToIgnore()) - Workspace.this.numCustomPages();
            return numScrollingPages;
        }

        public void syncWithScroll() {
            float offset = wallpaperOffsetForCurrentScroll();
            Workspace.this.mWallpaperOffset.setFinalX(offset);
            updateOffset(true);
        }

        public float getCurrX() {
            return this.mCurrentOffset;
        }

        private void animateToFinal() {
            this.mAnimating = true;
            this.mAnimationStartOffset = this.mCurrentOffset;
            this.mAnimationStartTime = System.currentTimeMillis();
        }

        private void setWallpaperOffsetSteps() {
            float xOffset = 1.0f / Workspace.this.mNumPagesForWallpaperParallax;
            if (xOffset == Workspace.this.mLastSetWallpaperOffsetSteps) {
                return;
            }
            Workspace.this.mWallpaperManager.setWallpaperOffsetSteps(xOffset, 1.0f);
            Workspace.this.mLastSetWallpaperOffsetSteps = xOffset;
        }

        public void setFinalX(float x) {
            scheduleUpdate();
            this.mFinalOffset = Math.max(0.0f, Math.min(x, 1.0f));
            if (getNumScreensExcludingEmptyAndCustom() == this.mNumScreens) {
                return;
            }
            if (this.mNumScreens > 0) {
                animateToFinal();
            }
            this.mNumScreens = getNumScreensExcludingEmptyAndCustom();
        }

        private void scheduleUpdate() {
            if (this.mWaitingForUpdate) {
                return;
            }
            this.mChoreographer.postFrameCallback(this);
            this.mWaitingForUpdate = true;
        }

        public void jumpToFinal() {
            this.mCurrentOffset = this.mFinalOffset;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        this.mWallpaperOffset.syncWithScroll();
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
        if (isSwitchingState()) {
            return;
        }
        super.determineScrollingStart(ev, touchSlopScale);
    }

    @Override
    public void announceForAccessibility(CharSequence text) {
        if (this.mLauncher.isAppsViewVisible()) {
            return;
        }
        super.announceForAccessibility(text);
    }

    public void showOutlinesTemporarily() {
        if (this.mIsPageMoving || isTouchActive()) {
            return;
        }
        snapToPage(this.mCurrentPage);
    }

    private void updatePageAlphaValues(int screenCenter) {
        if (!this.mWorkspaceFadeInAdjacentScreens || workspaceInModalState() || this.mIsSwitchingState) {
            return;
        }
        for (int i = numCustomPages(); i < getChildCount(); i++) {
            CellLayout child = (CellLayout) getChildAt(i);
            if (child != null) {
                float scrollProgress = getScrollProgress(screenCenter, child, i);
                float alpha = 1.0f - Math.abs(scrollProgress);
                child.getShortcutsAndWidgets().setAlpha(alpha);
            }
        }
    }

    @Override
    @TargetApi(21)
    public void enableAccessibleDrag(boolean enable) {
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout child = (CellLayout) getChildAt(i);
            child.enableAccessibleDrag(enable, 2);
        }
        if (enable) {
            setOnClickListener(null);
        } else {
            setOnClickListener(this.mLauncher);
        }
        this.mLauncher.getSearchDropTargetBar().enableAccessibleDrag(enable);
        this.mLauncher.getHotseat().getLayout().enableAccessibleDrag(enable, 2);
    }

    public boolean hasCustomContent() {
        return this.mScreenOrder.size() > 0 && this.mScreenOrder.get(0).longValue() == -301;
    }

    public int numCustomPages() {
        return hasCustomContent() ? 1 : 0;
    }

    public boolean isOnOrMovingToCustomContent() {
        return hasCustomContent() && getNextPage() == 0 && this.mRestorePage == -1001;
    }

    private void updateStateForCustomContent(int screenCenter) {
        float translationX = 0.0f;
        float progress = 0.0f;
        if (hasCustomContent()) {
            int index = this.mScreenOrder.indexOf(-301L);
            int scrollDelta = (getScrollX() - getScrollForPage(index)) - getLayoutTransitionOffsetForPage(index);
            float scrollRange = getScrollForPage(index + 1) - getScrollForPage(index);
            float translationX2 = scrollRange - scrollDelta;
            float progress2 = (scrollRange - scrollDelta) / scrollRange;
            if (this.mIsRtl) {
                translationX = Math.min(0.0f, translationX2);
            } else {
                translationX = Math.max(0.0f, translationX2);
            }
            progress = Math.max(0.0f, progress2);
        }
        if (Float.compare(progress, this.mLastCustomContentScrollProgress) == 0) {
            return;
        }
        CellLayout cc = this.mWorkspaceScreens.get(-301L);
        if (progress > 0.0f && cc.getVisibility() != 0 && !workspaceInModalState()) {
            cc.setVisibility(0);
        }
        this.mLastCustomContentScrollProgress = progress;
        if (this.mState == State.NORMAL) {
            this.mLauncher.getDragLayer().setBackgroundAlpha(progress != 1.0f ? 0.8f * progress : 0.0f);
        }
        if (this.mLauncher.getHotseat() != null) {
            this.mLauncher.getHotseat().setTranslationX(translationX);
        }
        if (getPageIndicator() != null) {
            getPageIndicator().setTranslationX(translationX);
        }
        if (this.mCustomContentCallbacks == null) {
            return;
        }
        this.mCustomContentCallbacks.onScrollProgressChanged(progress);
    }

    @Override
    protected View.OnClickListener getPageIndicatorClickListener() {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService("accessibility");
        if (!am.isTouchExplorationEnabled()) {
            return null;
        }
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Workspace.this.mLauncher.showOverviewMode(true);
            }
        };
        return listener;
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        updatePageAlphaValues(screenCenter);
        updateStateForCustomContent(screenCenter);
        enableHwLayersOnVisiblePages();
        if (getChildCount() == 0 && LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "screenScrolled: getChildCount() = " + getChildCount());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mWindowToken = getWindowToken();
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "onAttachedToWindow: mWindowToken = " + this.mWindowToken);
        }
        computeScroll();
        if (this.mDragController == null) {
            return;
        }
        this.mDragController.setWindowToken(this.mWindowToken);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "onDetachedFromWindow: mWindowToken = " + this.mWindowToken);
        }
        super.onDetachedFromWindow();
        this.mWindowToken = null;
    }

    protected void onResume() {
        View.OnClickListener listener;
        if (getPageIndicator() != null && (listener = getPageIndicatorClickListener()) != null) {
            getPageIndicator().setOnClickListener(listener);
        }
        if (LauncherAppState.getInstance().hasWallpaperChangedSinceLastCheck()) {
            setWallpaperDimension();
        }
        this.mWallpaperIsLiveWallpaper = this.mWallpaperManager.getWallpaperInfo() != null;
        this.mLastSetWallpaperOffsetSteps = 0.0f;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            this.mWallpaperOffset.syncWithScroll();
            this.mWallpaperOffset.jumpToFinal();
        }
        super.onLayout(changed, left, top, right, bottom);
        if (LauncherLog.DEBUG_LAYOUT) {
            LauncherLog.d("Launcher.Workspace", "onLayout: changed = " + changed + ", left = " + left + ", top = " + top + ", right = " + right + ", bottom = " + bottom);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        post(this.mBindPages);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!this.mLauncher.isAppsViewVisible()) {
            Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            }
            return super.onRequestFocusInDescendants(direction, previouslyFocusedRect);
        }
        return false;
    }

    @Override
    public int getDescendantFocusability() {
        if (workspaceInModalState()) {
            return 393216;
        }
        return super.getDescendantFocusability();
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (this.mLauncher.isAppsViewVisible()) {
            return;
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.addFocusables(views, direction);
        } else {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    public boolean workspaceInModalState() {
        return this.mState != State.NORMAL;
    }

    void enableChildrenCache(int fromPage, int toPage) {
        if (fromPage > toPage) {
            fromPage = toPage;
            toPage = fromPage;
        }
        int screenCount = getChildCount();
        int fromPage2 = Math.max(fromPage, 0);
        int toPage2 = Math.min(toPage, screenCount - 1);
        for (int i = fromPage2; i <= toPage2; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        int screenCount = getChildCount();
        for (int i = 0; i < screenCount; i++) {
            CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
            if (!isHardwareAccelerated()) {
                layout.setChildrenDrawingCacheEnabled(false);
            }
        }
    }

    public void updateChildrenLayersEnabled(boolean force) {
        boolean zIsPageMoving = (force || (this.mState != State.OVERVIEW ? this.mIsSwitchingState : true) || this.mAnimatingViewIntoPlace) ? true : isPageMoving();
        if (zIsPageMoving == this.mChildrenLayersEnabled) {
            return;
        }
        this.mChildrenLayersEnabled = zIsPageMoving;
        if (this.mChildrenLayersEnabled) {
            enableHwLayersOnVisiblePages();
            return;
        }
        for (int i = 0; i < getPageCount(); i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            cl.enableHardwareLayer(false);
        }
    }

    private void enableHwLayersOnVisiblePages() {
        if (this.mChildrenLayersEnabled) {
            int screenCount = getChildCount();
            getVisiblePages(this.mTempVisiblePagesRange);
            int leftScreen = this.mTempVisiblePagesRange[0];
            int rightScreen = this.mTempVisiblePagesRange[1];
            if (leftScreen == rightScreen) {
                if (rightScreen < screenCount - 1) {
                    rightScreen++;
                } else if (leftScreen > 0) {
                    leftScreen--;
                }
            }
            CellLayout customScreen = this.mWorkspaceScreens.get(-301L);
            int i = 0;
            while (i < screenCount) {
                CellLayout layout = (CellLayout) getPageAt(i);
                boolean zShouldDrawChild = (layout == customScreen || leftScreen > i || i > rightScreen) ? false : shouldDrawChild(layout);
                if (LauncherLog.DEBUG_DRAW) {
                    LauncherLog.d("Launcher.Workspace", "enableHwLayersOnVisiblePages 1: i = " + i + ",enableLayer = " + zShouldDrawChild + ",leftScreen = " + leftScreen + ", rightScreen = " + rightScreen + ", screenCount = " + screenCount + ", customScreen = " + customScreen + ",shouldDrawChild(layout) = " + shouldDrawChild(layout));
                }
                layout.enableHardwareLayer(zShouldDrawChild);
                i++;
            }
        }
    }

    public void buildPageHardwareLayers() {
        updateChildrenLayersEnabled(true);
        if (getWindowToken() != null) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.buildHardwareLayer();
            }
        }
        updateChildrenLayersEnabled(false);
    }

    protected void onWallpaperTap(MotionEvent ev) {
        int[] position = this.mTempCell;
        getLocationOnScreen(position);
        int pointerIndex = ev.getActionIndex();
        position[0] = position[0] + ((int) ev.getX(pointerIndex));
        position[1] = position[1] + ((int) ev.getY(pointerIndex));
        this.mWallpaperManager.sendWallpaperCommand(getWindowToken(), ev.getAction() == 1 ? "android.wallpaper.tap" : "android.wallpaper.secondaryTap", position[0], position[1], 0, null);
    }

    private static Rect getDrawableBounds(Drawable d) {
        Rect bounds = new Rect();
        d.copyBounds(bounds);
        if (bounds.width() == 0 || bounds.height() == 0) {
            bounds.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        } else {
            bounds.offsetTo(0, 0);
        }
        if (d instanceof PreloadIconDrawable) {
            int inset = -((PreloadIconDrawable) d).getOutset();
            bounds.inset(inset, inset);
        }
        return bounds;
    }

    public void onDragStartedWithItem(PendingAddItemInfo info, Bitmap b, boolean clipAlpha) {
        int[] size = estimateItemSize(info, false);
        this.mDragOutline = createDragOutline(b, 2, size[0], size[1], clipAlpha);
    }

    public void exitWidgetResizeMode() {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        dragLayer.clearAllResizeFrames();
    }

    @Override
    protected void getFreeScrollPageRange(int[] range) {
        getOverviewModePages(range);
    }

    private void getOverviewModePages(int[] range) {
        int start = numCustomPages();
        int end = getChildCount() - 1;
        range[0] = Math.max(0, Math.min(start, getChildCount() - 1));
        range[1] = Math.max(0, end);
    }

    @Override
    public void onStartReordering() {
        super.onStartReordering();
        disableLayoutTransitions();
    }

    @Override
    public void onEndReordering() {
        super.onEndReordering();
        if (this.mLauncher.isWorkspaceLoading()) {
            return;
        }
        this.mScreenOrder.clear();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            this.mScreenOrder.add(Long.valueOf(getIdForScreen(cl)));
        }
        this.mLauncher.getModel().updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        enableLayoutTransitions();
    }

    public boolean isInOverviewMode() {
        return this.mState == State.OVERVIEW;
    }

    int getOverviewModeTranslationY() {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        Rect workspacePadding = grid.getWorkspacePadding(Utilities.isRtl(getResources()));
        int overviewButtonBarHeight = grid.getOverviewModeButtonBarHeight();
        int scaledHeight = (int) (this.mOverviewModeShrinkFactor * getNormalChildHeight());
        int workspaceTop = this.mInsets.top + workspacePadding.top;
        int workspaceBottom = (getViewportHeight() - this.mInsets.bottom) - workspacePadding.bottom;
        int overviewTop = this.mInsets.top;
        int overviewBottom = (getViewportHeight() - this.mInsets.bottom) - overviewButtonBarHeight;
        int workspaceOffsetTopEdge = workspaceTop + (((workspaceBottom - workspaceTop) - scaledHeight) / 2);
        int overviewOffsetTopEdge = overviewTop + (((overviewBottom - overviewTop) - scaledHeight) / 2);
        return (-workspaceOffsetTopEdge) + overviewOffsetTopEdge;
    }

    public Animator setStateWithAnimation(State toState, int toPage, boolean animated, HashMap<View, Integer> layerViews) {
        boolean z;
        Animator workspaceAnim = this.mStateTransitionAnimation.getAnimationToState(this.mState, toState, toPage, animated, layerViews);
        if (this.mState.shouldUpdateWidget) {
            z = false;
        } else {
            z = toState.shouldUpdateWidget;
        }
        this.mState = toState;
        updateAccessibilityFlags();
        if (z) {
            this.mLauncher.notifyWidgetProvidersChanged();
        }
        return workspaceAnim;
    }

    State getState() {
        return this.mState;
    }

    public void updateAccessibilityFlags() {
        if (Utilities.ATLEAST_LOLLIPOP) {
            int total = getPageCount();
            for (int i = numCustomPages(); i < total; i++) {
                updateAccessibilityFlags((CellLayout) getPageAt(i), i);
            }
            setImportantForAccessibility((this.mState == State.NORMAL || this.mState == State.OVERVIEW) ? 0 : 4);
            return;
        }
        int accessible = this.mState == State.NORMAL ? 0 : 4;
        setImportantForAccessibility(accessible);
    }

    private void updateAccessibilityFlags(CellLayout page, int pageNo) {
        int accessible;
        if (this.mState == State.OVERVIEW) {
            page.setImportantForAccessibility(1);
            page.getShortcutsAndWidgets().setImportantForAccessibility(4);
            page.setContentDescription(getPageDescription(pageNo));
            if (this.mPagesAccessibilityDelegate == null) {
                this.mPagesAccessibilityDelegate = new OverviewScreenAccessibilityDelegate(this);
            }
            page.setAccessibilityDelegate(this.mPagesAccessibilityDelegate);
            return;
        }
        if (this.mState == State.NORMAL) {
            accessible = 0;
        } else {
            accessible = 4;
        }
        page.setImportantForAccessibility(2);
        page.getShortcutsAndWidgets().setImportantForAccessibility(accessible);
        page.setContentDescription(null);
        page.setAccessibilityDelegate(null);
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        this.mIsSwitchingState = true;
        this.mTransitionProgress = 0.0f;
        invalidate();
        updateChildrenLayersEnabled(false);
        hideCustomContentIfNecessary();
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
        this.mTransitionProgress = t;
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        LauncherHelper.beginSection("Workspace.onLauncherTransitionEnd");
        this.mIsSwitchingState = false;
        updateChildrenLayersEnabled(false);
        showCustomContentIfNecessary();
        LauncherHelper.endSection();
    }

    void updateCustomContentVisibility() {
        int visibility = this.mState == State.NORMAL ? 0 : 4;
        if (!hasCustomContent()) {
            return;
        }
        this.mWorkspaceScreens.get(-301L).setVisibility(visibility);
    }

    void showCustomContentIfNecessary() {
        boolean show = this.mState == State.NORMAL;
        if (!show || !hasCustomContent()) {
            return;
        }
        this.mWorkspaceScreens.get(-301L).setVisibility(0);
    }

    void hideCustomContentIfNecessary() {
        boolean hide = this.mState != State.NORMAL;
        if (!hide || !hasCustomContent()) {
            return;
        }
        disableLayoutTransitions();
        this.mWorkspaceScreens.get(-301L).setVisibility(4);
        enableLayoutTransitions();
    }

    public static Drawable getTextViewIcon(TextView tv) {
        Drawable[] drawables = tv.getCompoundDrawables();
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                return drawables[i];
            }
        }
        return null;
    }

    private static void drawDragView(View v, Canvas destCanvas, int padding) {
        Rect clipRect = sTempRect;
        v.getDrawingRect(clipRect);
        boolean textVisible = false;
        destCanvas.save();
        if (v instanceof TextView) {
            Drawable d = getTextViewIcon((TextView) v);
            Rect bounds = getDrawableBounds(d);
            clipRect.set(0, 0, bounds.width() + padding, bounds.height() + padding);
            destCanvas.translate((padding / 2) - bounds.left, (padding / 2) - bounds.top);
            d.draw(destCanvas);
        } else {
            if ((v instanceof FolderIcon) && ((FolderIcon) v).getTextVisible()) {
                ((FolderIcon) v).setTextVisible(false);
                textVisible = true;
            }
            destCanvas.translate((-v.getScrollX()) + (padding / 2), (-v.getScrollY()) + (padding / 2));
            destCanvas.clipRect(clipRect, Region.Op.REPLACE);
            v.draw(destCanvas);
            if (textVisible) {
                ((FolderIcon) v).setTextVisible(true);
            }
        }
        destCanvas.restore();
    }

    public Bitmap createDragBitmap(View v, AtomicInteger expectedPadding) {
        Bitmap b;
        int padding = expectedPadding.get();
        if (v instanceof TextView) {
            Drawable d = getTextViewIcon((TextView) v);
            Rect bounds = getDrawableBounds(d);
            b = Bitmap.createBitmap(bounds.width() + padding, bounds.height() + padding, Bitmap.Config.ARGB_8888);
            expectedPadding.set((padding - bounds.left) - bounds.top);
        } else {
            b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        }
        this.mCanvas.setBitmap(b);
        drawDragView(v, this.mCanvas, padding);
        this.mCanvas.setBitmap(null);
        return b;
    }

    private Bitmap createDragOutline(View v, int padding) {
        int outlineColor = getResources().getColor(R.color.outline_color);
        Bitmap b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        this.mCanvas.setBitmap(b);
        drawDragView(v, this.mCanvas, padding);
        this.mOutlineHelper.applyExpensiveOutlineWithBlur(b, this.mCanvas, outlineColor, outlineColor);
        this.mCanvas.setBitmap(null);
        return b;
    }

    private Bitmap createDragOutline(Bitmap orig, int padding, int w, int h, boolean clipAlpha) {
        int outlineColor = getResources().getColor(R.color.outline_color);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        this.mCanvas.setBitmap(b);
        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / orig.getWidth(), (h - padding) / orig.getHeight());
        int scaledWidth = (int) (orig.getWidth() * scaleFactor);
        int scaledHeight = (int) (orig.getHeight() * scaleFactor);
        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);
        this.mCanvas.drawBitmap(orig, src, dst, (Paint) null);
        this.mOutlineHelper.applyExpensiveOutlineWithBlur(b, this.mCanvas, outlineColor, outlineColor, clipAlpha);
        this.mCanvas.setBitmap(null);
        return b;
    }

    public void startDrag(CellLayout.CellInfo cellInfo) {
        startDrag(cellInfo, false);
    }

    @Override
    public void startDrag(CellLayout.CellInfo cellInfo, boolean accessible) {
        View child = cellInfo.cell;
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "startDrag cellInfo = " + cellInfo + ",child = " + child);
        }
        if (child != null && child.getTag() == null) {
            LauncherLog.d("Launcher.Workspace", "Abnormal start drag: cellInfo = " + cellInfo + ",child = " + child);
            return;
        }
        if (!child.isInTouchMode()) {
            if (LauncherLog.DEBUG) {
                LauncherLog.i("Launcher.Workspace", "The child " + child + " is not in touch mode.");
                return;
            }
            return;
        }
        boolean isValid = child.getParent().getParent() instanceof CellLayout;
        if (!isValid) {
            LauncherLog.e("Launcher.Workspace", "child = " + child + ",  child.getParent() = " + child.getParent() + "  ,child.getParent().getParent() = " + child.getParent().getParent());
            return;
        }
        this.mDragInfo = cellInfo;
        child.setVisibility(4);
        CellLayout layout = (CellLayout) child.getParent().getParent();
        layout.prepareChildForDrag(child);
        beginDragShared(child, this, accessible);
    }

    public void beginDragShared(View child, DragSource source, boolean accessible) {
        beginDragShared(child, new Point(), source, accessible);
    }

    public void beginDragShared(View child, Point relativeTouchPos, DragSource source, boolean accessible) {
        child.clearFocus();
        child.setPressed(false);
        this.mDragOutline = createDragOutline(child, 2);
        this.mLauncher.onDragStarted(child);
        AtomicInteger padding = new AtomicInteger(2);
        Bitmap b = createDragBitmap(child, padding);
        int bmpWidth = b.getWidth();
        int bmpHeight = b.getHeight();
        float scale = this.mLauncher.getDragLayer().getLocationInDragLayer(child, this.mTempXY);
        int dragLayerX = Math.round(this.mTempXY[0] - ((bmpWidth - (child.getWidth() * scale)) / 2.0f));
        int dragLayerY = Math.round((this.mTempXY[1] - ((bmpHeight - (bmpHeight * scale)) / 2.0f)) - (padding.get() / 2));
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "beginDragShared: child = " + child + ", source = " + source + ", dragLayerX = " + dragLayerX + ", dragLayerY = " + dragLayerY);
        }
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        Point dragVisualizeOffset = null;
        Rect rect = null;
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            int iconSize = grid.iconSizePx;
            int top = child.getPaddingTop();
            int left = (bmpWidth - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;
            if (icon.isLayoutHorizontal()) {
                if (icon.getIcon().getBounds().contains(relativeTouchPos.x, relativeTouchPos.y)) {
                    dragLayerX = Math.round(this.mTempXY[0]);
                } else {
                    dragLayerX = Math.round((this.mTempXY[0] + relativeTouchPos.x) - (bmpWidth / 2));
                }
            }
            dragLayerY += top;
            dragVisualizeOffset = new Point((-padding.get()) / 2, padding.get() / 2);
            rect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = grid.folderIconSizePx;
            dragVisualizeOffset = new Point((-padding.get()) / 2, (padding.get() / 2) - child.getPaddingTop());
            rect = new Rect(0, child.getPaddingTop(), child.getWidth(), previewSize);
        }
        if (child instanceof BubbleTextView) {
            ((BubbleTextView) child).clearPressedBackground();
        }
        if (child.getTag() == null || !(child.getTag() instanceof ItemInfo)) {
            String msg = "Drag started with a view that has no tag set. This will cause a crash (issue 11627249) down the line. View: " + child + "  tag: " + child.getTag();
            throw new IllegalStateException(msg);
        }
        if (child.getParent() instanceof ShortcutAndWidgetContainer) {
            this.mDragSourceInternal = (ShortcutAndWidgetContainer) child.getParent();
        }
        DragView dv = this.mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(), DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, rect, scale, accessible);
        dv.setIntrinsicIconScaleFactor(source.getIntrinsicIconScaleFactor());
        b.recycle();
    }

    public boolean transitionStateShouldAllowDrop() {
        boolean z = true;
        if (isSwitchingState() && this.mTransitionProgress <= 0.5f) {
            return false;
        }
        if (this.mState != State.NORMAL && this.mState != State.SPRING_LOADED) {
            z = false;
        }
        return z;
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject d) {
        int spanX;
        int spanY;
        CellLayout dropTargetLayout = this.mDropToLayout;
        if (d.dragSource != this) {
            if (dropTargetLayout == null || !transitionStateShouldAllowDrop()) {
                return false;
            }
            this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
            if (this.mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, this.mDragViewVisualCenter, null);
            }
            if (this.mDragInfo != null) {
                CellLayout.CellInfo dragCellInfo = this.mDragInfo;
                spanX = dragCellInfo.spanX;
                spanY = dragCellInfo.spanY;
            } else {
                ItemInfo dragInfo = (ItemInfo) d.dragInfo;
                spanX = dragInfo.spanX;
                spanY = dragInfo.spanY;
            }
            int minSpanX = spanX;
            int minSpanY = spanY;
            if (d.dragInfo instanceof PendingAddWidgetInfo) {
                minSpanX = ((PendingAddWidgetInfo) d.dragInfo).minSpanX;
                minSpanY = ((PendingAddWidgetInfo) d.dragInfo).minSpanY;
            }
            this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, dropTargetLayout, this.mTargetCell);
            float distance = dropTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            if (this.mCreateUserFolderOnDrop && willCreateUserFolder((ItemInfo) d.dragInfo, dropTargetLayout, this.mTargetCell, distance, true)) {
                return true;
            }
            if (this.mAddToExistingFolderOnDrop && willAddToExistingUserFolder((ItemInfo) d.dragInfo, dropTargetLayout, this.mTargetCell, distance)) {
                return true;
            }
            int[] resultSpan = new int[2];
            this.mTargetCell = dropTargetLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, null, this.mTargetCell, resultSpan, 4);
            boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0;
            if (!foundCell) {
                boolean isHotseat = this.mLauncher.isHotseatLayout(dropTargetLayout);
                if (this.mTargetCell != null && isHotseat) {
                    Hotseat hotseat = this.mLauncher.getHotseat();
                    if (hotseat.isAllAppsButtonRank(hotseat.getOrderInHotseat(this.mTargetCell[0], this.mTargetCell[1]))) {
                        return false;
                    }
                }
                this.mLauncher.showOutOfSpaceMessage(isHotseat);
                return false;
            }
        }
        long screenId = getIdForScreen(dropTargetLayout);
        if (screenId == -201) {
            commitExtraEmptyScreen();
            return true;
        }
        return true;
    }

    boolean willCreateUserFolder(ItemInfo info, CellLayout target, int[] targetCell, float distance, boolean considerTimeout) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        return willCreateUserFolder(info, dropOverView, considerTimeout);
    }

    boolean willCreateUserFolder(ItemInfo info, View dropOverView, boolean considerTimeout) {
        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY)) {
                return false;
            }
        }
        boolean hasntMoved = false;
        if (this.mDragInfo != null) {
            hasntMoved = dropOverView == this.mDragInfo.cell;
        }
        if (dropOverView == null || hasntMoved || (considerTimeout && !this.mCreateUserFolderOnDrop)) {
            return false;
        }
        boolean aboveShortcut = dropOverView.getTag() instanceof ShortcutInfo;
        boolean willBecomeShortcut = info.itemType == 0 || info.itemType == 1;
        if (aboveShortcut) {
            return willBecomeShortcut;
        }
        return false;
    }

    boolean willAddToExistingUserFolder(Object dragInfo, CellLayout target, int[] targetCell, float distance) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        return willAddToExistingUserFolder(dragInfo, dropOverView);
    }

    boolean willAddToExistingUserFolder(Object dragInfo, View dropOverView) {
        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.cellY)) {
                return false;
            }
        }
        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(dragInfo)) {
                return true;
            }
        }
        return false;
    }

    boolean createUserFolderIfNecessary(View newView, long container, CellLayout target, int[] targetCell, float distance, boolean external, DragView dragView, Runnable postAnimationRunnable) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View v = target.getChildAt(targetCell[0], targetCell[1]);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "createUserFolderIfNecessary: newView = " + newView + ", mDragInfo = " + this.mDragInfo + ", container = " + container + ", target = " + target + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = " + targetCell[1] + ", external = " + external + ", dragView = " + dragView + ", v = " + v + ", mCreateUserFolderOnDrop = " + this.mCreateUserFolderOnDrop);
        }
        boolean hasntMoved = false;
        if (this.mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(this.mDragInfo.cell);
            hasntMoved = this.mDragInfo.cellX == targetCell[0] && this.mDragInfo.cellY == targetCell[1] && cellParent == target;
        }
        if (v == null || hasntMoved || !this.mCreateUserFolderOnDrop) {
            if (!LauncherLog.DEBUG) {
                return false;
            }
            LauncherLog.d("Launcher.Workspace", "Do not create user folder: hasntMoved = " + hasntMoved + ", mCreateUserFolderOnDrop = " + this.mCreateUserFolderOnDrop + ", v = " + v);
            return false;
        }
        this.mCreateUserFolderOnDrop = false;
        long screenId = getIdForScreen(target);
        boolean aboveShortcut = v.getTag() instanceof ShortcutInfo;
        boolean willBecomeShortcut = newView.getTag() instanceof ShortcutInfo;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "createUserFolderIfNecessary: aboveShortcut = " + aboveShortcut + ", willBecomeShortcut = " + willBecomeShortcut);
        }
        if (!aboveShortcut || !willBecomeShortcut) {
            return false;
        }
        ShortcutInfo sourceInfo = (ShortcutInfo) newView.getTag();
        ShortcutInfo destInfo = (ShortcutInfo) v.getTag();
        if (!external) {
            getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
        }
        Rect folderLocation = new Rect();
        float scale = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation);
        target.removeView(v);
        FolderIcon fi = this.mLauncher.addFolder(target, container, screenId, targetCell[0], targetCell[1]);
        destInfo.cellX = -1;
        destInfo.cellY = -1;
        sourceInfo.cellX = -1;
        sourceInfo.cellY = -1;
        boolean animate = dragView != null;
        if (animate) {
            fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, folderLocation, scale, postAnimationRunnable);
            return true;
        }
        fi.addItem(destInfo);
        fi.addItem(sourceInfo);
        return true;
    }

    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell, float distance, DropTarget.DragObject d, boolean external) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "createUserFolderIfNecessary: newView = " + newView + ", target = " + target + ", targetCell[0] = " + targetCell[0] + ", targetCell[1] = " + targetCell[1] + ", external = " + external + ", d = " + d + ", dropOverView = " + dropOverView);
        }
        if (!this.mAddToExistingFolderOnDrop) {
            return false;
        }
        this.mAddToExistingFolderOnDrop = false;
        if (dropOverView instanceof FolderIcon) {
            FolderIcon fi = (FolderIcon) dropOverView;
            if (fi.acceptDrop(d.dragInfo)) {
                fi.onDrop(d);
                if (!external) {
                    getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
                }
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("Launcher.Workspace", "addToExistingFolderIfNecessary: fi = " + fi + ", d = " + d);
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
    public void onDrop(DropTarget.DragObject d) {
        final LauncherAppWidgetHostView hostView;
        AppWidgetProviderInfo pInfo;
        this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
        final CellLayout dropTargetLayout = this.mDropToLayout;
        if (dropTargetLayout != null) {
            if (this.mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, this.mDragViewVisualCenter, null);
            }
        }
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDrop 1: drag view = " + d.dragView + ", dragInfo = " + d.dragInfo + ", dragSource  = " + d.dragSource + ", dropTargetLayout = " + dropTargetLayout + ", mDragInfo = " + this.mDragInfo + ", mInScrollArea = " + this.mInScrollArea + ", this = " + this);
        }
        int snapScreen = -1;
        boolean resizeOnDrop = false;
        if (d.dragSource != this) {
            int[] touchXY = {(int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1]};
            onDropExternal(touchXY, d.dragInfo, dropTargetLayout, false, d);
            return;
        }
        if (this.mDragInfo != null) {
            View cell = this.mDragInfo.cell;
            Runnable resizeRunnable = null;
            if (dropTargetLayout != null && !d.cancelled) {
                boolean hasMovedLayouts = getParentCellLayoutForView(cell) != dropTargetLayout;
                boolean hasMovedIntoHotseat = this.mLauncher.isHotseatLayout(dropTargetLayout);
                long container = hasMovedIntoHotseat ? -101 : -100;
                long screenId = this.mTargetCell[0] < 0 ? this.mDragInfo.screenId : getIdForScreen(dropTargetLayout);
                int spanX = this.mDragInfo != null ? this.mDragInfo.spanX : 1;
                int spanY = this.mDragInfo != null ? this.mDragInfo.spanY : 1;
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, this.mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                if (LauncherLog.DEBUG_DRAG) {
                    LauncherLog.d("Launcher.Workspace", "onDrop 2: cell = " + cell + ", screenId = " + screenId + ", mInScrollArea = " + this.mInScrollArea + ", mTargetCell = " + this.mTargetCell + ", this = " + this);
                }
                if ((!this.mInScrollArea && createUserFolderIfNecessary(cell, container, dropTargetLayout, this.mTargetCell, distance, false, d.dragView, null)) || addToExistingFolderIfNecessary(cell, dropTargetLayout, this.mTargetCell, distance, d, false)) {
                    return;
                }
                ItemInfo item = (ItemInfo) d.dragInfo;
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                int[] resultSpan = new int[2];
                this.mTargetCell = dropTargetLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell, this.mTargetCell, resultSpan, 2);
                boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0 && resultSpan[0] > 0 && resultSpan[1] > 0;
                if (LauncherLog.DEBUG) {
                    LauncherLog.d("Launcher.Workspace", "onDrop 3: foundCell = " + foundCell + "mTargetCell = (" + this.mTargetCell[0] + ", " + this.mTargetCell[1] + "), resultSpan = (" + resultSpan[0] + "," + resultSpan[1] + "), item.span = (" + item.spanX + ", " + item.spanY + ") ,item.minSpan = (" + item.minSpanX + ", " + item.minSpanY + "),minSpan = (" + minSpanX + "," + minSpanY + ").");
                }
                if (foundCell && (cell instanceof AppWidgetHostView) && (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY)) {
                    resizeOnDrop = true;
                    item.spanX = resultSpan[0];
                    item.spanY = resultSpan[1];
                    AppWidgetHostView awhv = (AppWidgetHostView) cell;
                    AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, this.mLauncher, resultSpan[0], resultSpan[1]);
                }
                if (getScreenIdForPageIndex(this.mCurrentPage) != screenId && !hasMovedIntoHotseat) {
                    snapScreen = getPageIndexForScreenId(screenId);
                    snapToPage(snapScreen);
                }
                if (foundCell) {
                    final ItemInfo info = (ItemInfo) cell.getTag();
                    if (hasMovedLayouts) {
                        CellLayout parentCell = getParentCellLayoutForView(cell);
                        if (parentCell != null) {
                            parentCell.removeView(cell);
                        } else if (LauncherAppState.isDogfoodBuild()) {
                            throw new NullPointerException("mDragInfo.cell has null parent");
                        }
                        addInScreen(cell, container, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
                    }
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
                    int i = this.mTargetCell[0];
                    lp.tmpCellX = i;
                    lp.cellX = i;
                    int i2 = this.mTargetCell[1];
                    lp.tmpCellY = i2;
                    lp.cellY = i2;
                    lp.cellHSpan = item.spanX;
                    lp.cellVSpan = item.spanY;
                    lp.isLockedToGrid = true;
                    if (container != -101 && (cell instanceof LauncherAppWidgetHostView) && (pInfo = (hostView = (LauncherAppWidgetHostView) cell).getAppWidgetInfo()) != null && pInfo.resizeMode != 0 && !d.accessibleDrag) {
                        final Runnable addResizeFrame = new Runnable() {
                            @Override
                            public void run() {
                                DragLayer dragLayer = Workspace.this.mLauncher.getDragLayer();
                                dragLayer.addResizeFrame(info, hostView, dropTargetLayout);
                            }
                        };
                        resizeRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (!Workspace.this.isPageMoving()) {
                                    addResizeFrame.run();
                                } else {
                                    Workspace.this.mDelayedResizeRunnable = addResizeFrame;
                                }
                            }
                        };
                    }
                    LauncherModel.modifyItemInDatabase(this.mLauncher, info, container, screenId, lp.cellX, lp.cellY, item.spanX, item.spanY);
                } else {
                    CellLayout.LayoutParams lp2 = (CellLayout.LayoutParams) cell.getLayoutParams();
                    this.mTargetCell[0] = lp2.cellX;
                    this.mTargetCell[1] = lp2.cellY;
                    CellLayout layout = (CellLayout) cell.getParent().getParent();
                    layout.markCellsAsOccupiedForView(cell);
                }
            }
            if (cell.getParent() == null) {
                LauncherLog.e("Launcher.Workspace", "error ,cell.getParent() == null");
                return;
            }
            CellLayout parent = (CellLayout) cell.getParent().getParent();
            final Runnable finalResizeRunnable = resizeRunnable;
            Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    Workspace.this.mAnimatingViewIntoPlace = false;
                    Workspace.this.updateChildrenLayersEnabled(false);
                    if (finalResizeRunnable == null) {
                        return;
                    }
                    finalResizeRunnable.run();
                }
            };
            this.mAnimatingViewIntoPlace = true;
            if (d.dragView.hasDrawn()) {
                ItemInfo info2 = (ItemInfo) cell.getTag();
                boolean isWidget = info2.itemType == 4 || info2.itemType == 5;
                if (isWidget) {
                    int animationType = resizeOnDrop ? 2 : 0;
                    animateWidgetDrop(info2, parent, d.dragView, onCompleteRunnable, animationType, cell, false);
                } else {
                    int duration = snapScreen < 0 ? -1 : 300;
                    this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, cell, duration, onCompleteRunnable, this);
                }
            } else {
                d.deferDragViewCleanupPostAnimation = false;
                cell.setVisibility(0);
            }
            parent.onDropChild(cell);
        }
    }

    public void getPageAreaRelativeToDragLayer(Rect outArea) {
        CellLayout child = (CellLayout) getChildAt(getNextPage());
        if (child == null) {
            return;
        }
        ShortcutAndWidgetContainer boundingLayout = child.getShortcutsAndWidgets();
        this.mTempXY[0] = getViewportOffsetX() + getPaddingLeft() + boundingLayout.getLeft();
        this.mTempXY[1] = child.getTop() + boundingLayout.getTop();
        float scale = this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempXY);
        outArea.set(this.mTempXY[0], this.mTempXY[1], (int) (this.mTempXY[0] + (boundingLayout.getMeasuredWidth() * scale)), (int) (this.mTempXY[1] + (boundingLayout.getMeasuredHeight() * scale)));
    }

    @Override
    public void onDragEnter(DropTarget.DragObject d) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDragEnter: d = " + d + ", mDragTargetLayout = " + this.mDragTargetLayout);
        }
        if (ENFORCE_DRAG_EVENT_ORDER) {
            enfoceDragParity("onDragEnter", 1, 1);
        }
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
        if (workspaceInModalState()) {
            return;
        }
        this.mLauncher.getDragLayer().showPageHints();
    }

    static Rect getCellLayoutMetrics(Launcher launcher, int orientation) {
        LauncherAppState app = LauncherAppState.getInstance();
        InvariantDeviceProfile inv = app.getInvariantDeviceProfile();
        Display display = launcher.getWindowManager().getDefaultDisplay();
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        int countX = inv.numColumns;
        int countY = inv.numRows;
        boolean isLayoutRtl = Utilities.isRtl(launcher.getResources());
        if (orientation == 0) {
            if (mLandscapeCellLayoutMetrics == null) {
                Rect padding = inv.landscapeProfile.getWorkspacePadding(isLayoutRtl);
                int width = (largestSize.x - padding.left) - padding.right;
                int height = (smallestSize.y - padding.top) - padding.bottom;
                mLandscapeCellLayoutMetrics = new Rect();
                mLandscapeCellLayoutMetrics.set(DeviceProfile.calculateCellWidth(width, countX), DeviceProfile.calculateCellHeight(height, countY), 0, 0);
            }
            return mLandscapeCellLayoutMetrics;
        }
        if (orientation == 1) {
            if (mPortraitCellLayoutMetrics == null) {
                Rect padding2 = inv.portraitProfile.getWorkspacePadding(isLayoutRtl);
                int width2 = (smallestSize.x - padding2.left) - padding2.right;
                int height2 = (largestSize.y - padding2.top) - padding2.bottom;
                mPortraitCellLayoutMetrics = new Rect();
                mPortraitCellLayoutMetrics.set(DeviceProfile.calculateCellWidth(width2, countX), DeviceProfile.calculateCellHeight(height2, countY), 0, 0);
            }
            return mPortraitCellLayoutMetrics;
        }
        return null;
    }

    @Override
    public void onDragExit(DropTarget.DragObject d) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDragExit: d = " + d);
        }
        if (ENFORCE_DRAG_EVENT_ORDER) {
            enfoceDragParity("onDragExit", -1, 0);
        }
        if (!this.mInScrollArea) {
            this.mDropToLayout = this.mDragTargetLayout;
        } else if (isPageMoving()) {
            this.mDropToLayout = (CellLayout) getPageAt(getNextPage());
        } else {
            this.mDropToLayout = this.mDragOverlappingLayout;
        }
        if (this.mDragMode == 1) {
            this.mCreateUserFolderOnDrop = true;
        } else if (this.mDragMode == 2) {
            this.mAddToExistingFolderOnDrop = true;
        }
        onResetScrollArea();
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "doDragExit: drag source = " + (d != null ? d.dragSource : null) + ", drag info = " + (d != null ? d.dragInfo : null) + ", mDragTargetLayout = " + this.mDragTargetLayout + ", mIsPageMoving = " + this.mIsPageMoving);
        }
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);
        this.mSpringLoadedDragController.cancel();
        this.mLauncher.getDragLayer().hidePageHints();
    }

    private void enfoceDragParity(String event, int update, int expectedValue) {
        enfoceDragParity(this, event, update, expectedValue);
        for (int i = 0; i < getChildCount(); i++) {
            enfoceDragParity(getChildAt(i), event, update, expectedValue);
        }
    }

    private void enfoceDragParity(View v, String event, int update, int expectedValue) {
        Object tag = v.getTag(R.id.drag_event_parity);
        int value = (tag == null ? 0 : ((Integer) tag).intValue()) + update;
        v.setTag(R.id.drag_event_parity, Integer.valueOf(value));
        if (value == expectedValue) {
            return;
        }
        Log.e("Launcher.Workspace", event + ": Drag contract violated: " + value);
    }

    void setCurrentDropLayout(CellLayout layout) {
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.revertTempState();
            this.mDragTargetLayout.onDragExit();
        }
        this.mDragTargetLayout = layout;
        if (this.mDragTargetLayout != null) {
            this.mDragTargetLayout.onDragEnter();
        }
        cleanupReorder(true);
        cleanupFolderCreation();
        setCurrentDropOverCell(-1, -1);
    }

    void setCurrentDragOverlappingLayout(CellLayout layout) {
        if (this.mDragOverlappingLayout != null) {
            this.mDragOverlappingLayout.setIsDragOverlapping(false);
        }
        this.mDragOverlappingLayout = layout;
        if (this.mDragOverlappingLayout != null) {
            this.mDragOverlappingLayout.setIsDragOverlapping(true);
        }
        invalidate();
    }

    void setCurrentDropOverCell(int x, int y) {
        if (x == this.mDragOverX && y == this.mDragOverY) {
            return;
        }
        this.mDragOverX = x;
        this.mDragOverY = y;
        setDragMode(0);
    }

    void setDragMode(int dragMode) {
        if (dragMode == this.mDragMode) {
            return;
        }
        if (dragMode == 0) {
            cleanupAddToFolder();
            cleanupReorder(false);
            cleanupFolderCreation();
        } else if (dragMode == 2) {
            cleanupReorder(true);
            cleanupFolderCreation();
        } else if (dragMode == 1) {
            cleanupAddToFolder();
            cleanupReorder(true);
        } else if (dragMode == 3) {
            cleanupAddToFolder();
            cleanupFolderCreation();
        }
        this.mDragMode = dragMode;
    }

    private void cleanupFolderCreation() {
        if (this.mDragFolderRingAnimator != null) {
            this.mDragFolderRingAnimator.animateToNaturalState();
            this.mDragFolderRingAnimator = null;
        }
        this.mFolderCreationAlarm.setOnAlarmListener(null);
        this.mFolderCreationAlarm.cancelAlarm();
    }

    private void cleanupAddToFolder() {
        if (this.mDragOverFolderIcon == null) {
            return;
        }
        this.mDragOverFolderIcon.onDragExit(null);
        this.mDragOverFolderIcon = null;
    }

    private void cleanupReorder(boolean cancelAlarm) {
        if (cancelAlarm) {
            this.mReorderAlarm.cancelAlarm();
        }
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
    }

    void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
        xy[0] = xy[0] - v.getLeft();
        xy[1] = xy[1] - v.getTop();
    }

    boolean isPointInSelfOverHotseat(int x, int y, Rect r) {
        if (r == null) {
            new Rect();
        }
        this.mTempPt[0] = x;
        this.mTempPt[1] = y;
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempPt, true);
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        Rect r2 = grid.getHotseatRect();
        return r2.contains(this.mTempPt[0], this.mTempPt[1]);
    }

    void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
        this.mTempPt[0] = (int) xy[0];
        this.mTempPt[1] = (int) xy[1];
        this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(this, this.mTempPt, true);
        this.mLauncher.getDragLayer().mapCoordInSelfToDescendent(hotseat.getLayout(), this.mTempPt);
        xy[0] = this.mTempPt[0];
        xy[1] = this.mTempPt[1];
    }

    void mapPointFromChildToSelf(View v, float[] xy) {
        xy[0] = xy[0] + v.getLeft();
        xy[1] = xy[1] + v.getTop();
    }

    private static float squaredDistance(float[] point1, float[] point2) {
        float distanceX = point1[0] - point2[0];
        float distanceY = point2[1] - point2[1];
        return (distanceX * distanceX) + (distanceY * distanceY);
    }

    private CellLayout findMatchingPageForDragOver(DragView dragView, float originX, float originY, boolean exact) {
        int screenCount = getChildCount();
        CellLayout bestMatchingScreen = null;
        float smallestDistSoFar = Float.MAX_VALUE;
        for (int i = 0; i < screenCount; i++) {
            if (this.mScreenOrder.get(i).longValue() != -301) {
                CellLayout cl = (CellLayout) getChildAt(i);
                float[] touchXy = {originX, originY};
                cl.getMatrix().invert(this.mTempInverseMatrix);
                mapPointFromSelfToChild(cl, touchXy, this.mTempInverseMatrix);
                if (touchXy[0] >= 0.0f && touchXy[0] <= cl.getWidth() && touchXy[1] >= 0.0f && touchXy[1] <= cl.getHeight()) {
                    return cl;
                }
                if (!exact) {
                    float[] cellLayoutCenter = this.mTempCellLayoutCenterCoordinates;
                    cellLayoutCenter[0] = cl.getWidth() / 2;
                    cellLayoutCenter[1] = cl.getHeight() / 2;
                    mapPointFromChildToSelf(cl, cellLayoutCenter);
                    touchXy[0] = originX;
                    touchXy[1] = originY;
                    float dist = squaredDistance(touchXy, cellLayoutCenter);
                    if (dist < smallestDistSoFar) {
                        smallestDistSoFar = dist;
                        bestMatchingScreen = cl;
                    }
                }
            }
        }
        return bestMatchingScreen;
    }

    private boolean isDragWidget(DropTarget.DragObject d) {
        if (d.dragInfo instanceof LauncherAppWidgetInfo) {
            return true;
        }
        return d.dragInfo instanceof PendingAddWidgetInfo;
    }

    private boolean isExternalDragWidget(DropTarget.DragObject d) {
        if (d.dragSource != this) {
            return isDragWidget(d);
        }
        return false;
    }

    @Override
    public void onDragOver(DropTarget.DragObject d) {
        if (LauncherLog.DEBUG_DRAG) {
            LauncherLog.d("Launcher.Workspace", "onDragOver: d = " + d + ", dragInfo = " + d.dragInfo + ", mInScrollArea = " + this.mInScrollArea + ", mIsSwitchingState = " + this.mIsSwitchingState);
        }
        if (this.mInScrollArea || !transitionStateShouldAllowDrop()) {
            return;
        }
        Rect r = new Rect();
        CellLayout layout = null;
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (item == null) {
            if (LauncherAppState.isDogfoodBuild()) {
                throw new NullPointerException("DragObject has null info");
            }
            return;
        }
        if (item.spanX < 0 || item.spanY < 0) {
            throw new RuntimeException("Improper spans found");
        }
        this.mDragViewVisualCenter = d.getVisualCenter(this.mDragViewVisualCenter);
        View view = this.mDragInfo == null ? null : this.mDragInfo.cell;
        if (workspaceInModalState()) {
            if (this.mLauncher.getHotseat() != null && !isExternalDragWidget(d) && isPointInSelfOverHotseat(d.x, d.y, r)) {
                layout = this.mLauncher.getHotseat().getLayout();
            }
            if (layout == null) {
                layout = findMatchingPageForDragOver(d.dragView, d.x, d.y, false);
            }
            if (layout != this.mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
                boolean isInSpringLoadedMode = this.mState == State.SPRING_LOADED;
                if (isInSpringLoadedMode) {
                    if (this.mLauncher.isHotseatLayout(layout)) {
                        this.mSpringLoadedDragController.cancel();
                    } else {
                        this.mSpringLoadedDragController.setAlarm(this.mDragTargetLayout);
                    }
                }
            }
        } else {
            if (this.mLauncher.getHotseat() != null && !isDragWidget(d) && isPointInSelfOverHotseat(d.x, d.y, r)) {
                layout = this.mLauncher.getHotseat().getLayout();
            }
            if (layout == null) {
                layout = getCurrentDropLayout();
            }
            if (layout != this.mDragTargetLayout) {
                setCurrentDropLayout(layout);
                setCurrentDragOverlappingLayout(layout);
            }
        }
        if (this.mDragTargetLayout != null) {
            if (this.mLauncher.isHotseatLayout(this.mDragTargetLayout)) {
                mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(this.mDragTargetLayout, this.mDragViewVisualCenter, null);
            }
            int minSpanX = item.spanX;
            int minSpanY = item.spanY;
            if (item.minSpanX > 0 && item.minSpanY > 0) {
                minSpanX = item.minSpanX;
                minSpanY = item.minSpanY;
            }
            this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, this.mDragTargetLayout, this.mTargetCell);
            int reorderX = this.mTargetCell[0];
            int reorderY = this.mTargetCell[1];
            setCurrentDropOverCell(this.mTargetCell[0], this.mTargetCell[1]);
            float targetCellDistance = this.mDragTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            manageFolderFeedback(this.mDragTargetLayout, this.mTargetCell, targetCellDistance, d);
            boolean nearestDropOccupied = this.mDragTargetLayout.isNearestDropLocationOccupied((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], item.spanX, item.spanY, view, this.mTargetCell);
            if (!nearestDropOccupied) {
                this.mDragTargetLayout.visualizeDropLocation(view, this.mDragOutline, this.mTargetCell[0], this.mTargetCell[1], item.spanX, item.spanY, false, d);
            } else if ((this.mDragMode == 0 || this.mDragMode == 3) && !this.mReorderAlarm.alarmPending() && (this.mLastReorderX != reorderX || this.mLastReorderY != reorderY)) {
                int[] resultSpan = new int[2];
                this.mDragTargetLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, item.spanX, item.spanY, view, this.mTargetCell, resultSpan, 0);
                ReorderAlarmListener listener = new ReorderAlarmListener(this.mDragViewVisualCenter, minSpanX, minSpanY, item.spanX, item.spanY, d, view);
                this.mReorderAlarm.setOnAlarmListener(listener);
                this.mReorderAlarm.setAlarm(350L);
            }
            if ((this.mDragMode == 1 || this.mDragMode == 2 || !nearestDropOccupied) && this.mDragTargetLayout != null) {
                this.mDragTargetLayout.revertTempState();
            }
        }
    }

    private void manageFolderFeedback(CellLayout targetLayout, int[] targetCell, float distance, DropTarget.DragObject dragObject) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return;
        }
        View dragOverView = this.mDragTargetLayout.getChildAt(this.mTargetCell[0], this.mTargetCell[1]);
        ItemInfo info = (ItemInfo) dragObject.dragInfo;
        boolean userFolderPending = willCreateUserFolder(info, dragOverView, false);
        if (this.mDragMode == 0 && userFolderPending && !this.mFolderCreationAlarm.alarmPending()) {
            FolderCreationAlarmListener listener = new FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1]);
            if (!dragObject.accessibleDrag) {
                this.mFolderCreationAlarm.setOnAlarmListener(listener);
                this.mFolderCreationAlarm.setAlarm(0L);
            } else {
                listener.onAlarm(this.mFolderCreationAlarm);
            }
            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(WorkspaceAccessibilityHelper.getDescriptionForDropOver(dragOverView, getContext()));
                return;
            }
            return;
        }
        boolean willAddToFolder = willAddToExistingUserFolder(info, dragOverView);
        if (willAddToFolder && this.mDragMode == 0) {
            if (this.mLauncher != null) {
                this.mLauncher.closeFolder();
            }
            this.mDragOverFolderIcon = (FolderIcon) dragOverView;
            this.mDragOverFolderIcon.onDragEnter(info);
            if (targetLayout != null) {
                targetLayout.clearDragOutlines();
            }
            setDragMode(2);
            if (dragObject.stateAnnouncer != null) {
                dragObject.stateAnnouncer.announce(WorkspaceAccessibilityHelper.getDescriptionForDropOver(dragOverView, getContext()));
                return;
            }
            return;
        }
        if (this.mDragMode == 2 && !willAddToFolder) {
            setDragMode(0);
        }
        if (this.mDragMode != 1 || userFolderPending) {
            return;
        }
        setDragMode(0);
    }

    class FolderCreationAlarmListener implements OnAlarmListener {
        int cellX;
        int cellY;
        CellLayout layout;

        public FolderCreationAlarmListener(CellLayout layout, int cellX, int cellY) {
            this.layout = layout;
            this.cellX = cellX;
            this.cellY = cellY;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            if (Workspace.this.mDragFolderRingAnimator != null) {
                Workspace.this.mDragFolderRingAnimator.animateToNaturalState();
            }
            Workspace.this.mDragFolderRingAnimator = new FolderIcon.FolderRingAnimator(Workspace.this.mLauncher, null);
            Workspace.this.mDragFolderRingAnimator.setCell(this.cellX, this.cellY);
            Workspace.this.mDragFolderRingAnimator.setCellLayout(this.layout);
            Workspace.this.mDragFolderRingAnimator.animateToAcceptState();
            this.layout.showFolderAccept(Workspace.this.mDragFolderRingAnimator);
            this.layout.clearDragOutlines();
            Workspace.this.setDragMode(1);
        }
    }

    class ReorderAlarmListener implements OnAlarmListener {
        View child;
        DropTarget.DragObject dragObject;
        float[] dragViewCenter;
        int minSpanX;
        int minSpanY;
        int spanX;
        int spanY;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX, int spanY, DropTarget.DragObject dragObject, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragObject = dragObject;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            Workspace.this.mTargetCell = Workspace.this.findNearestArea((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.minSpanX, this.minSpanY, Workspace.this.mDragTargetLayout, Workspace.this.mTargetCell);
            Workspace.this.mLastReorderX = Workspace.this.mTargetCell[0];
            Workspace.this.mLastReorderY = Workspace.this.mTargetCell[1];
            Workspace.this.mTargetCell = Workspace.this.mDragTargetLayout.performReorder((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.minSpanX, this.minSpanY, this.spanX, this.spanY, this.child, Workspace.this.mTargetCell, resultSpan, 1);
            if (Workspace.this.mTargetCell[0] < 0 || Workspace.this.mTargetCell[1] < 0) {
                Workspace.this.mDragTargetLayout.revertTempState();
            } else {
                Workspace.this.setDragMode(3);
            }
            boolean resize = (resultSpan[0] == this.spanX && resultSpan[1] == this.spanY) ? false : true;
            Workspace.this.mDragTargetLayout.visualizeDropLocation(this.child, Workspace.this.mDragOutline, Workspace.this.mTargetCell[0], Workspace.this.mTargetCell[1], resultSpan[0], resultSpan[1], resize, this.dragObject);
        }
    }

    @Override
    public void getHitRectRelativeToDragLayer(Rect outRect) {
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this, outRect);
    }

    private void onDropExternal(int[] touchXY, Object dragInfo, CellLayout cellLayout, boolean insertAtFirst, DropTarget.DragObject d) {
        View view;
        Runnable exitSpringLoadedRunnable = new Runnable() {
            @Override
            public void run() {
                Workspace.this.mLauncher.exitSpringLoadedDragModeDelayed(true, 300, null);
            }
        };
        ItemInfo info = (ItemInfo) dragInfo;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (this.mDragInfo != null) {
            spanX = this.mDragInfo.spanX;
            spanY = this.mDragInfo.spanY;
        }
        final long container = this.mLauncher.isHotseatLayout(cellLayout) ? -101 : -100;
        final long screenId = getIdForScreen(cellLayout);
        if (screenId == -201) {
            if (LauncherLog.DEBUG) {
                LauncherLog.d("Launcher.Workspace", "onDropExternal: screenId = " + screenId + "mLauncher.isWorkspaceLoading() = " + this.mLauncher.isWorkspaceLoading());
            }
            int index = getPageIndexForScreenId(-201L);
            CellLayout cl = this.mWorkspaceScreens.get(-201L);
            this.mWorkspaceScreens.remove(-201L);
            this.mScreenOrder.remove((Object) (-201L));
            long newId = LauncherAppState.getLauncherProvider().generateNewScreenId();
            this.mWorkspaceScreens.put(newId, cl);
            this.mScreenOrder.add(Long.valueOf(newId));
            if (getPageIndicator() != null) {
                getPageIndicator().updateMarker(index, getPageIndicatorMarker(index));
            }
            this.mLauncher.getModel().updateWorkspaceScreenOrder(this.mLauncher, this.mScreenOrder);
        }
        if (!this.mLauncher.isHotseatLayout(cellLayout) && screenId != getScreenIdForPageIndex(this.mCurrentPage) && this.mState != State.SPRING_LOADED) {
            snapToScreenId(screenId, null);
        }
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "onDropExternal: touchXY[0] = " + (touchXY != null ? touchXY[0] : -1) + ", touchXY[1] = " + (touchXY != null ? touchXY[1] : -1) + ", dragInfo = " + dragInfo + ",info = " + info + ", cellLayout = " + cellLayout + ", insertAtFirst = " + insertAtFirst + ", dragInfo = " + d.dragInfo + ", screenId = " + screenId + ", container = " + container);
        }
        if (!(info instanceof PendingAddItemInfo)) {
            switch (info.itemType) {
                case PackageInstallerCompat.STATUS_INSTALLED:
                case PackageInstallerCompat.STATUS_INSTALLING:
                    if (info.container == -1 && (info instanceof AppInfo)) {
                        info = ((AppInfo) info).makeShortcut();
                    }
                    view = this.mLauncher.createShortcut(cellLayout, (ShortcutInfo) info);
                    break;
                case PackageInstallerCompat.STATUS_FAILED:
                    view = FolderIcon.fromXml(R.layout.folder_icon, this.mLauncher, cellLayout, (FolderInfo) info, this.mIconCache);
                    break;
                default:
                    throw new IllegalStateException("Unknown item type: " + info.itemType);
            }
            if (touchXY != null) {
                this.mTargetCell = findNearestArea(touchXY[0], touchXY[1], spanX, spanY, cellLayout, this.mTargetCell);
                float distance = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                d.postAnimationRunnable = exitSpringLoadedRunnable;
                if (createUserFolderIfNecessary(view, container, cellLayout, this.mTargetCell, distance, true, d.dragView, d.postAnimationRunnable) || addToExistingFolderIfNecessary(view, cellLayout, this.mTargetCell, distance, d, true)) {
                    return;
                }
            }
            if (touchXY != null) {
                this.mTargetCell = cellLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, 1, 1, null, this.mTargetCell, null, 3);
            } else {
                cellLayout.findCellForSpan(this.mTargetCell, 1, 1);
            }
            LauncherModel.addOrMoveItemInDatabase(this.mLauncher, info, container, screenId, this.mTargetCell[0], this.mTargetCell[1]);
            addInScreen(view, container, screenId, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY, insertAtFirst);
            cellLayout.onDropChild(view);
            cellLayout.getShortcutsAndWidgets().measureChild(view);
            if (d.dragView != null) {
                setFinalTransitionTransform(cellLayout);
                this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view, exitSpringLoadedRunnable, this);
                resetTransitionTransform(cellLayout);
                return;
            }
            return;
        }
        final PendingAddItemInfo pendingInfo = (PendingAddItemInfo) dragInfo;
        boolean findNearestVacantCell = true;
        if (pendingInfo.itemType == 1) {
            this.mTargetCell = findNearestArea(touchXY[0], touchXY[1], spanX, spanY, cellLayout, this.mTargetCell);
            float distance2 = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            if (willCreateUserFolder((ItemInfo) d.dragInfo, cellLayout, this.mTargetCell, distance2, true) || willAddToExistingUserFolder((ItemInfo) d.dragInfo, cellLayout, this.mTargetCell, distance2)) {
                findNearestVacantCell = false;
            }
        }
        final ItemInfo item = (ItemInfo) d.dragInfo;
        if (findNearestVacantCell) {
            int minSpanX = item.spanX;
            int minSpanY = item.spanY;
            if (item.minSpanX > 0 && item.minSpanY > 0) {
                minSpanX = item.minSpanX;
                minSpanY = item.minSpanY;
            }
            int[] resultSpan = new int[2];
            this.mTargetCell = cellLayout.performReorder((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, info.spanX, info.spanY, null, this.mTargetCell, resultSpan, 3);
            updateWidgetSize = (resultSpan[0] == item.spanX && resultSpan[1] == item.spanY) ? false : true;
            item.spanX = resultSpan[0];
            item.spanY = resultSpan[1];
        }
        Runnable onAnimationCompleteRunnable = new Runnable() {
            @Override
            public void run() {
                Workspace.this.deferRemoveExtraEmptyScreen();
                Workspace.this.mLauncher.addPendingItem(pendingInfo, container, screenId, Workspace.this.mTargetCell, item.spanX, item.spanY);
            }
        };
        boolean isWidget = pendingInfo.itemType == 4 || pendingInfo.itemType == 5;
        AppWidgetHostView finalView = isWidget ? ((PendingAddWidgetInfo) pendingInfo).boundWidget : null;
        if (finalView != null && updateWidgetSize) {
            AppWidgetResizeFrame.updateWidgetSizeRanges(finalView, this.mLauncher, item.spanX, item.spanY);
        }
        int animationStyle = 0;
        if (isWidget && ((PendingAddWidgetInfo) pendingInfo).info != null && ((PendingAddWidgetInfo) pendingInfo).info.configure != null) {
            animationStyle = 1;
        }
        animateWidgetDrop(info, cellLayout, d.dragView, onAnimationCompleteRunnable, animationStyle, finalView, true);
    }

    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
        int[] unScaledSize = this.mLauncher.getWorkspace().estimateItemSize(widgetInfo, false);
        int visibility = layout.getVisibility();
        layout.setVisibility(0);
        int width = View.MeasureSpec.makeMeasureSpec(unScaledSize[0], 1073741824);
        int height = View.MeasureSpec.makeMeasureSpec(unScaledSize[1], 1073741824);
        Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1], Bitmap.Config.ARGB_8888);
        this.mCanvas.setBitmap(b);
        layout.measure(width, height);
        layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
        layout.draw(this.mCanvas);
        this.mCanvas.setBitmap(null);
        layout.setVisibility(visibility);
        return b;
    }

    private void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY, DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell, boolean scale) {
        float dragViewScaleX;
        float dragViewScaleY;
        int spanX = info.spanX;
        int spanY = info.spanY;
        Rect r = estimateItemPosition(layout, targetCell[0], targetCell[1], spanX, spanY);
        loc[0] = r.left;
        loc[1] = r.top;
        setFinalTransitionTransform(layout);
        float cellLayoutScale = this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(layout, loc, true);
        resetTransitionTransform(layout);
        if (scale) {
            dragViewScaleX = (r.width() * 1.0f) / dragView.getMeasuredWidth();
            dragViewScaleY = (r.height() * 1.0f) / dragView.getMeasuredHeight();
        } else {
            dragViewScaleX = 1.0f;
            dragViewScaleY = 1.0f;
        }
        loc[0] = (int) (((double) loc[0]) - (((double) ((dragView.getMeasuredWidth() - (r.width() * cellLayoutScale)) / 2.0f)) - Math.ceil(layout.getUnusedHorizontalSpace() / 2.0f)));
        loc[1] = (int) (loc[1] - ((dragView.getMeasuredHeight() - (r.height() * cellLayoutScale)) / 2.0f));
        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView, final Runnable onCompleteRunnable, int animationType, final View finalView, boolean external) {
        Rect from = new Rect();
        this.mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, from);
        int[] finalPos = new int[2];
        float[] scaleXY = new float[2];
        boolean scalePreview = !(info instanceof PendingAddShortcutInfo);
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, this.mTargetCell, scalePreview);
        Resources res = this.mLauncher.getResources();
        int duration = res.getInteger(R.integer.config_dropAnimMaxDuration) - 200;
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "animateWidgetDrop: info = " + info + ", animationType = " + animationType + ", finalPos = (" + finalPos[0] + ", " + finalPos[1] + "), scaleXY = (" + scaleXY[0] + ", " + scaleXY[1] + "), scalePreview = " + scalePreview + ",external = " + external);
        }
        if ((finalView instanceof AppWidgetHostView) && external) {
            this.mLauncher.getDragLayer().removeView(finalView);
        }
        boolean isWidget = info.itemType == 4 || info.itemType == 5;
        if ((animationType == 2 || external) && finalView != null) {
            Bitmap crossFadeBitmap = createWidgetBitmap(info, finalView);
            dragView.setCrossFadeBitmap(crossFadeBitmap);
            dragView.crossFade((int) (duration * 0.8f));
        } else if (isWidget && external) {
            float fMin = Math.min(scaleXY[0], scaleXY[1]);
            scaleXY[1] = fMin;
            scaleXY[0] = fMin;
        }
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (animationType == 4) {
            this.mLauncher.getDragLayer().animateViewIntoPosition(dragView, finalPos, 0.0f, 0.1f, 0.1f, 0, onCompleteRunnable, duration);
            return;
        }
        int endStyle = animationType == 1 ? 2 : 0;
        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                if (finalView != null) {
                    finalView.setVisibility(0);
                }
                if (onCompleteRunnable == null) {
                    return;
                }
                onCompleteRunnable.run();
            }
        };
        dragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0], finalPos[1], 1.0f, 1.0f, 1.0f, scaleXY[0], scaleXY[1], onComplete, endStyle, duration, this);
    }

    public void setFinalTransitionTransform(CellLayout layout) {
        if (!isSwitchingState()) {
            return;
        }
        this.mCurrentScale = getScaleX();
        setScaleX(this.mStateTransitionAnimation.getFinalScale());
        setScaleY(this.mStateTransitionAnimation.getFinalScale());
    }

    public void resetTransitionTransform(CellLayout layout) {
        if (!isSwitchingState()) {
            return;
        }
        setScaleX(this.mCurrentScale);
        setScaleY(this.mCurrentScale);
    }

    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(getNextPage());
    }

    public int getCurrentPageOffsetFromCustomContent() {
        return getNextPage() - numCustomPages();
    }

    int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(pixelX, pixelY, spanX, spanY, recycle);
    }

    void setup(DragController dragController) {
        this.mSpringLoadedDragController = new SpringLoadedDragController(this.mLauncher);
        this.mDragController = dragController;
        updateChildrenLayersEnabled(false);
    }

    @Override
    public void onDropCompleted(final View target, final DropTarget.DragObject d, final boolean isFlingToDelete, final boolean success) {
        if (LauncherLog.DEBUG) {
            LauncherLog.d("Launcher.Workspace", "onDropCompleted: target = " + target + ", d = " + d + ", isFlingToDelete = " + isFlingToDelete + ", mDragInfo = " + this.mDragInfo + ", success = " + success);
        }
        if (this.mDeferDropAfterUninstall) {
            this.mDeferredAction = new Runnable() {
                @Override
                public void run() {
                    Workspace.this.onDropCompleted(target, d, isFlingToDelete, success);
                    Workspace.this.mDeferredAction = null;
                }
            };
            return;
        }
        boolean beingCalledAfterUninstall = this.mDeferredAction != null;
        if (!success || (beingCalledAfterUninstall && !this.mUninstallSuccessful)) {
            if (this.mDragInfo != null && target != null) {
                CellLayout cellLayout = this.mLauncher.getCellLayout(this.mDragInfo.container, this.mDragInfo.screenId);
                if (cellLayout != null) {
                    cellLayout.onDropChild(this.mDragInfo.cell);
                } else if (LauncherAppState.isDogfoodBuild()) {
                    throw new RuntimeException("Invalid state: cellLayout == null in Workspace#onDropCompleted. Please file a bug. ");
                }
            }
        } else if (target != this && this.mDragInfo != null) {
            removeWorkspaceItem(this.mDragInfo.cell);
        }
        if ((d.cancelled || (beingCalledAfterUninstall && !this.mUninstallSuccessful)) && this.mDragInfo.cell != null) {
            this.mDragInfo.cell.setVisibility(0);
        }
        this.mDragOutline = null;
        this.mDragInfo = null;
    }

    public void removeWorkspaceItem(View view) {
        CellLayout parentCell = getParentCellLayoutForView(view);
        if (parentCell != null) {
            parentCell.removeView(view);
        } else if (LauncherAppState.isDogfoodBuild()) {
            Log.e("Launcher.Workspace", "mDragInfo.cell has null parent");
        }
        if (!(view instanceof DropTarget)) {
            return;
        }
        this.mDragController.removeDropTarget((DropTarget) view);
    }

    @Override
    public void deferCompleteDropAfterUninstallActivity() {
        this.mDeferDropAfterUninstall = true;
    }

    @Override
    public void onUninstallActivityReturned(boolean success) {
        this.mDeferDropAfterUninstall = false;
        this.mUninstallSuccessful = success;
        if (this.mDeferredAction == null) {
            return;
        }
        this.mDeferredAction.run();
    }

    void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getShortcutsAndWidgets().getChildCount();
        long screenId = getIdForScreen(cl);
        int container = -100;
        if (this.mLauncher.isHotseatLayout(cl)) {
            screenId = -1;
            container = -101;
        }
        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            if (info != null && info.requiresDbUpdate) {
                info.requiresDbUpdate = false;
                LauncherModel.modifyItemInDatabase(this.mLauncher, info, container, screenId, info.cellX, info.cellY, info.spanX, info.spanY);
            }
        }
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        return 1.0f;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return false;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return true;
    }

    @Override
    public void onFlingToDelete(DropTarget.DragObject d, PointF vec) {
    }

    @Override
    public void onFlingToDeleteCompleted() {
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        this.mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (this.mSavedStates == null) {
            return;
        }
        this.mRestoredPages.add(Integer.valueOf(child));
        CellLayout cl = (CellLayout) getChildAt(child);
        if (cl == null) {
            return;
        }
        cl.restoreInstanceState(this.mSavedStates);
    }

    public void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!this.mRestoredPages.contains(Integer.valueOf(i))) {
                restoreInstanceStateForChild(i);
            }
        }
        this.mRestoredPages.clear();
        this.mSavedStates = null;
    }

    @Override
    public void scrollLeft() {
        if (!workspaceInModalState() && !this.mIsSwitchingState) {
            super.scrollLeft();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder == null) {
            return;
        }
        openFolder.completeDragExit();
    }

    @Override
    public void scrollRight() {
        if (!workspaceInModalState() && !this.mIsSwitchingState) {
            super.scrollRight();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder == null) {
            return;
        }
        openFolder.completeDragExit();
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        boolean isPortrait = !this.mLauncher.getDeviceProfile().isLandscape;
        if (this.mLauncher.getHotseat() != null && isPortrait) {
            Rect r = new Rect();
            this.mLauncher.getHotseat().getHitRect(r);
            if (r.contains(x, y)) {
                return false;
            }
        }
        if (workspaceInModalState() || this.mIsSwitchingState || getOpenFolder() != null) {
            return false;
        }
        this.mInScrollArea = true;
        int page = getNextPage() + (direction == 0 ? -1 : 1);
        setCurrentDropLayout(null);
        if (page < 0 || page >= getChildCount() || getScreenIdForPageIndex(page) == -301) {
            return false;
        }
        CellLayout layout = (CellLayout) getChildAt(page);
        setCurrentDragOverlappingLayout(layout);
        invalidate();
        return true;
    }

    @Override
    public boolean onExitScrollArea() {
        if (!this.mInScrollArea) {
            return false;
        }
        invalidate();
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
        this.mInScrollArea = false;
        return true;
    }

    private void onResetScrollArea() {
        setCurrentDragOverlappingLayout(null);
        this.mInScrollArea = false;
    }

    CellLayout getParentCellLayoutForView(View v) {
        ArrayList<CellLayout> layouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layout : layouts) {
            if (layout.getShortcutsAndWidgets().indexOfChild(v) > -1) {
                return layout;
            }
        }
        return null;
    }

    ArrayList<CellLayout> getWorkspaceAndHotseatCellLayouts() {
        ArrayList<CellLayout> layouts = new ArrayList<>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            layouts.add((CellLayout) getChildAt(screen));
        }
        if (this.mLauncher.getHotseat() != null) {
            layouts.add(this.mLauncher.getHotseat().getLayout());
        }
        return layouts;
    }

    ArrayList<ShortcutAndWidgetContainer> getAllShortcutAndWidgetContainers() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = new ArrayList<>();
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            childrenLayouts.add(((CellLayout) getChildAt(screen)).getShortcutsAndWidgets());
        }
        if (this.mLauncher.getHotseat() != null) {
            childrenLayouts.add(this.mLauncher.getHotseat().getLayout().getShortcutsAndWidgets());
        }
        return childrenLayouts;
    }

    public Folder getFolderForTag(final Object tag) {
        return (Folder) getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((v instanceof Folder) && ((Folder) v).getInfo() == tag) {
                    return ((Folder) v).getInfo().opened;
                }
                return false;
            }
        });
    }

    public View getHomescreenIconByItemId(final long id) {
        return getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info != null && info.id == id;
            }
        });
    }

    public View getViewForTag(final Object tag) {
        return getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return info == tag;
            }
        });
    }

    public LauncherAppWidgetHostView getWidgetForAppWidgetId(final int appWidgetId) {
        return (LauncherAppWidgetHostView) getFirstMatch(new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                return (info instanceof LauncherAppWidgetInfo) && ((LauncherAppWidgetInfo) info).appWidgetId == appWidgetId;
            }
        });
    }

    private View getFirstMatch(final ItemOperator operator) {
        final View[] value = new View[1];
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if (!operator.evaluate(info, v, parent)) {
                    return false;
                }
                value[0] = v;
                return true;
            }
        });
        return value[0];
    }

    void clearDropTargets() {
        mapOverItems(false, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View view, View parent) {
                if (view instanceof DropTarget) {
                    Workspace.this.mDragController.removeDropTarget((DropTarget) view);
                    return false;
                }
                return false;
            }
        });
    }

    void removeItemsByPackageName(final HashSet<String> packageNames, final UserHandleCompat user) {
        HashSet<ItemInfo> infos = new HashSet<>();
        final HashSet<ComponentName> cns = new HashSet<>();
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layoutParent : cellLayouts) {
            ViewGroup layout = layoutParent.getShortcutsAndWidgets();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = layout.getChildAt(i);
                infos.add((ItemInfo) view.getTag());
            }
        }
        LauncherModel.ItemInfoFilter filter = new LauncherModel.ItemInfoFilter() {
            @Override
            public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                if (packageNames.contains(cn.getPackageName()) && info.user.equals(user)) {
                    cns.add(cn);
                    return true;
                }
                return false;
            }
        };
        LauncherModel.filterItemInfos(infos, filter);
        removeItemsByComponentName(cns, user);
    }

    void removeItemsByComponentName(final HashSet<ComponentName> componentNames, final UserHandleCompat user) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout cellLayout : cellLayouts) {
            ShortcutAndWidgetContainer layout = cellLayout.getShortcutsAndWidgets();
            final HashMap<ItemInfo, View> children = new HashMap<>();
            for (int j = 0; j < layout.getChildCount(); j++) {
                View view = layout.getChildAt(j);
                children.put((ItemInfo) view.getTag(), view);
            }
            final ArrayList<View> childrenToRemove = new ArrayList<>();
            final HashMap<FolderInfo, ArrayList<ShortcutInfo>> folderAppsToRemove = new HashMap<>();
            LauncherModel.ItemInfoFilter filter = new LauncherModel.ItemInfoFilter() {
                @Override
                public boolean filterItem(ItemInfo parent, ItemInfo info, ComponentName cn) {
                    ArrayList<ShortcutInfo> appsToRemove;
                    if (parent instanceof FolderInfo) {
                        if (componentNames.contains(cn) && info.user.equals(user)) {
                            FolderInfo folder = (FolderInfo) parent;
                            if (folderAppsToRemove.containsKey(folder)) {
                                appsToRemove = (ArrayList) folderAppsToRemove.get(folder);
                            } else {
                                appsToRemove = new ArrayList<>();
                                folderAppsToRemove.put(folder, appsToRemove);
                            }
                            appsToRemove.add((ShortcutInfo) info);
                            return true;
                        }
                        return false;
                    }
                    if (componentNames.contains(cn) && info.user.equals(user)) {
                        childrenToRemove.add((View) children.get(info));
                        return true;
                    }
                    for (ComponentName item : componentNames) {
                        if (item.getPackageName().equals(cn.getPackageName())) {
                            childrenToRemove.add((View) children.get(info));
                            return true;
                        }
                    }
                    return false;
                }
            };
            LauncherModel.filterItemInfos(children.keySet(), filter);
            for (FolderInfo folder : folderAppsToRemove.keySet()) {
                ArrayList<ShortcutInfo> appsToRemove = folderAppsToRemove.get(folder);
                for (ShortcutInfo info : appsToRemove) {
                    folder.remove(info);
                }
            }
            for (View view2 : childrenToRemove) {
                cellLayout.removeViewInLayout(view2);
                if (view2 instanceof DropTarget) {
                    this.mDragController.removeDropTarget((DropTarget) view2);
                }
            }
            if (childrenToRemove.size() > 0) {
                layout.requestLayout();
                layout.invalidate();
            }
        }
        removeExtraEmptyScreen(false, true);
        stripEmptyScreens();
    }

    void mapOverItems(boolean recurse, ItemOperator op) {
        ArrayList<ShortcutAndWidgetContainer> containers = getAllShortcutAndWidgetContainers();
        int containerCount = containers.size();
        for (int containerIdx = 0; containerIdx < containerCount; containerIdx++) {
            ShortcutAndWidgetContainer container = containers.get(containerIdx);
            int itemCount = container.getChildCount();
            for (int itemIdx = 0; itemIdx < itemCount; itemIdx++) {
                View item = container.getChildAt(itemIdx);
                ItemInfo info = (ItemInfo) item.getTag();
                if (recurse && (info instanceof FolderInfo) && (item instanceof FolderIcon)) {
                    FolderIcon folder = (FolderIcon) item;
                    ArrayList<View> folderChildren = folder.getFolder().getItemsInReadingOrder();
                    int childCount = folderChildren.size();
                    for (int childIdx = 0; childIdx < childCount; childIdx++) {
                        View child = folderChildren.get(childIdx);
                        if (op.evaluate((ItemInfo) child.getTag(), child, folder)) {
                            return;
                        }
                    }
                } else if (op.evaluate(info, item, null)) {
                    return;
                }
            }
        }
    }

    void updateShortcuts(ArrayList<ShortcutInfo> shortcuts) {
        final HashSet<ShortcutInfo> updates = new HashSet<>(shortcuts);
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                boolean zHasNotCompleted;
                if ((info instanceof ShortcutInfo) && (v instanceof BubbleTextView) && updates.contains(info)) {
                    ShortcutInfo si = (ShortcutInfo) info;
                    BubbleTextView shortcut = (BubbleTextView) v;
                    Drawable oldIcon = Workspace.getTextViewIcon(shortcut);
                    if (!(oldIcon instanceof PreloadIconDrawable)) {
                        zHasNotCompleted = false;
                    } else {
                        zHasNotCompleted = ((PreloadIconDrawable) oldIcon).hasNotCompleted();
                    }
                    shortcut.applyFromShortcutInfo(si, Workspace.this.mIconCache, si.isPromise() != zHasNotCompleted);
                    if (parent != null) {
                        parent.invalidate();
                    }
                }
                return false;
            }
        });
    }

    public void removeAbandonedPromise(String packageName, UserHandleCompat user) {
        HashSet<String> packages = new HashSet<>(1);
        packages.add(packageName);
        LauncherModel.deletePackageFromDatabase(this.mLauncher, packageName, user);
        removeItemsByPackageName(packages, user);
    }

    public void updateRestoreItems(final HashSet<ItemInfo> updates) {
        mapOverItems(true, new ItemOperator() {
            @Override
            public boolean evaluate(ItemInfo info, View v, View parent) {
                if ((info instanceof ShortcutInfo) && (v instanceof BubbleTextView) && updates.contains(info)) {
                    ((BubbleTextView) v).applyState(false);
                } else if ((v instanceof PendingAppWidgetHostView) && (info instanceof LauncherAppWidgetInfo) && updates.contains(info)) {
                    ((PendingAppWidgetHostView) v).applyState();
                }
                return false;
            }
        });
    }

    public void widgetsRestored(ArrayList<LauncherAppWidgetInfo> changedInfo) {
        AppWidgetProviderInfo widgetInfo;
        if (changedInfo.isEmpty()) {
            return;
        }
        DeferredWidgetRefresh widgetRefresh = new DeferredWidgetRefresh(changedInfo, this.mLauncher.getAppWidgetHost());
        LauncherAppWidgetInfo item = changedInfo.get(0);
        if (item.hasRestoreFlag(1)) {
            widgetInfo = AppWidgetManagerCompat.getInstance(this.mLauncher).findProvider(item.providerName, item.user);
        } else {
            widgetInfo = AppWidgetManagerCompat.getInstance(this.mLauncher).getAppWidgetInfo(item.appWidgetId);
        }
        if (widgetInfo != null) {
            widgetRefresh.run();
            return;
        }
        for (LauncherAppWidgetInfo info : changedInfo) {
            if (info.hostView instanceof PendingAppWidgetHostView) {
                info.installProgress = 100;
                ((PendingAppWidgetHostView) info.hostView).applyState();
            }
        }
    }

    private void moveToScreen(int page, boolean animate) {
        if (!workspaceInModalState()) {
            if (animate) {
                snapToPage(page);
            } else {
                setCurrentPage(page);
            }
        }
        View child = getChildAt(page);
        if (child == null) {
            return;
        }
        child.requestFocus();
    }

    void moveToDefaultScreen(boolean animate) {
        moveToScreen(this.mDefaultPage, animate);
    }

    @Override
    protected PageIndicator.PageMarkerResources getPageIndicatorMarker(int pageIndex) {
        long screenId = getScreenIdForPageIndex(pageIndex);
        if (screenId == -201) {
            int count = this.mScreenOrder.size() - numCustomPages();
            if (count > 1) {
                return new PageIndicator.PageMarkerResources(R.drawable.ic_pageindicator_current, R.drawable.ic_pageindicator_add);
            }
        }
        return super.getPageIndicatorMarker(pageIndex);
    }

    @Override
    protected String getPageIndicatorDescription() {
        String settings = getResources().getString(R.string.settings_button_text);
        return getCurrentPageDescription() + ", " + settings;
    }

    @Override
    protected String getCurrentPageDescription() {
        if (hasCustomContent() && getNextPage() == 0) {
            return this.mCustomContentDescription;
        }
        int page = this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage;
        return getPageDescription(page);
    }

    private String getPageDescription(int page) {
        int delta = numCustomPages();
        int nScreens = getChildCount() - delta;
        int extraScreenId = this.mScreenOrder.indexOf(-201L);
        if (extraScreenId >= 0 && nScreens > 1) {
            if (page == extraScreenId) {
                return getContext().getString(R.string.workspace_new_page);
            }
            nScreens--;
        }
        if (nScreens == 0) {
            return getContext().getString(R.string.all_apps_home_button_label);
        }
        return getContext().getString(R.string.workspace_scroll_format, Integer.valueOf((page + 1) - delta), Integer.valueOf(nScreens));
    }

    @Override
    public void fillInLaunchSourceData(View v, Bundle sourceData) {
        sourceData.putString("container", "homescreen");
        sourceData.putInt("container_page", getCurrentPage());
    }

    private class DeferredWidgetRefresh implements Runnable {
        private final LauncherAppWidgetHost mHost;
        private final ArrayList<LauncherAppWidgetInfo> mInfos;
        private final Handler mHandler = new Handler();
        private boolean mRefreshPending = true;

        public DeferredWidgetRefresh(ArrayList<LauncherAppWidgetInfo> infos, LauncherAppWidgetHost host) {
            this.mInfos = infos;
            this.mHost = host;
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
            for (LauncherAppWidgetInfo info : this.mInfos) {
                if (info.hostView instanceof PendingAppWidgetHostView) {
                    PendingAppWidgetHostView view = (PendingAppWidgetHostView) info.hostView;
                    Workspace.this.mLauncher.removeItem(view, info, false);
                    Workspace.this.mLauncher.bindAppWidget(info);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                LauncherLog.d("Launcher.Workspace", "onTouchEvent: Set HW layer on in touch down event");
                updateChildrenLayersEnabled(true);
                break;
            case PackageInstallerCompat.STATUS_INSTALLING:
                LauncherLog.d("Launcher.Workspace", "onTouchEvent: Set HW layer off in touch up event");
                updateChildrenLayersEnabled(false);
                break;
        }
        return super.onTouchEvent(event);
    }
}
