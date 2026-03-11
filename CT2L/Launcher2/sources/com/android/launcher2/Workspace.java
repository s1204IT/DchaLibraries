package com.android.launcher2;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import android.os.IBinder;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.launcher.R;
import com.android.launcher2.CellLayout;
import com.android.launcher2.DragController;
import com.android.launcher2.DropTarget;
import com.android.launcher2.FolderIcon;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Workspace extends SmoothPagedView implements View.OnTouchListener, ViewGroup.OnHierarchyChangeListener, DragController.DragListener, DragScroller, DragSource, DropTarget, LauncherTransitionable {
    static Rect mLandscapeCellLayoutMetrics = null;
    static Rect mPortraitCellLayoutMetrics = null;
    private boolean mAddToExistingFolderOnDrop;
    boolean mAnimatingViewIntoPlace;
    private Drawable mBackground;
    private float mBackgroundAlpha;
    private ValueAnimator mBackgroundFadeInAnimation;
    private ValueAnimator mBackgroundFadeOutAnimation;
    private final Runnable mBindPages;
    private int mCameraDistance;
    boolean mChildrenLayersEnabled;
    private float mChildrenOutlineAlpha;
    private ObjectAnimator mChildrenOutlineFadeInAnimation;
    private ObjectAnimator mChildrenOutlineFadeOutAnimation;
    private boolean mCreateUserFolderOnDrop;
    private float mCurrentRotationY;
    private float mCurrentScaleX;
    private float mCurrentScaleY;
    private float mCurrentTranslationX;
    private float mCurrentTranslationY;
    private int mDefaultPage;
    private Runnable mDelayedResizeRunnable;
    private Runnable mDelayedSnapToPageRunnable;
    private Point mDisplaySize;
    private DragController mDragController;
    private DropTarget.DragEnforcer mDragEnforcer;
    private FolderIcon.FolderRingAnimator mDragFolderRingAnimator;
    private CellLayout.CellInfo mDragInfo;
    private int mDragMode;
    private Bitmap mDragOutline;
    private FolderIcon mDragOverFolderIcon;
    private int mDragOverX;
    private int mDragOverY;
    private CellLayout mDragOverlappingLayout;
    private CellLayout mDragTargetLayout;
    private float[] mDragViewVisualCenter;
    boolean mDrawBackground;
    private CellLayout mDropToLayout;
    private final Alarm mFolderCreationAlarm;
    private IconCache mIconCache;
    private boolean mInScrollArea;
    boolean mIsDragOccuring;
    private boolean mIsStaticWallpaper;
    private boolean mIsSwitchingState;
    private int mLastReorderX;
    private int mLastReorderY;
    private Launcher mLauncher;
    private float mMaxDistanceForFolderCreation;
    private float[] mNewAlphas;
    private float[] mNewBackgroundAlphas;
    private float[] mNewRotationYs;
    private float[] mNewScaleXs;
    private float[] mNewScaleYs;
    private float[] mNewTranslationXs;
    private float[] mNewTranslationYs;
    private float[] mOldAlphas;
    private float[] mOldBackgroundAlphas;
    private float[] mOldScaleXs;
    private float[] mOldScaleYs;
    private float[] mOldTranslationXs;
    private float[] mOldTranslationYs;
    private int mOriginalPageSpacing;
    private final HolographicOutlineHelper mOutlineHelper;
    private float mOverscrollFade;
    private boolean mOverscrollTransformsSet;
    private final Alarm mReorderAlarm;
    private final ArrayList<Integer> mRestoredPages;
    private float mSavedRotationY;
    private int mSavedScrollX;
    private SparseArray<Parcelable> mSavedStates;
    private float mSavedTranslationX;
    private SpringLoadedDragController mSpringLoadedDragController;
    private int mSpringLoadedPageSpacing;
    private float mSpringLoadedShrinkFactor;
    private State mState;
    private int[] mTargetCell;
    private int[] mTempCell;
    private float[] mTempCellLayoutCenterCoordinates;
    private float[] mTempDragBottomRightCoordinates;
    private float[] mTempDragCoordinates;
    private int[] mTempEstimate;
    private Matrix mTempInverseMatrix;
    private final Rect mTempRect;
    private int[] mTempVisiblePagesRange;
    private final int[] mTempXY;
    private float mTransitionProgress;
    boolean mUpdateWallpaperOffsetImmediately;
    int mWallpaperHeight;
    private final WallpaperManager mWallpaperManager;
    WallpaperOffsetInterpolator mWallpaperOffset;
    private float mWallpaperScrollRatio;
    private int mWallpaperTravelWidth;
    int mWallpaperWidth;
    private IBinder mWindowToken;
    private boolean mWorkspaceFadeInAdjacentScreens;
    private float mXDown;
    private float mYDown;
    private final ZoomInInterpolator mZoomInInterpolator;

    enum State {
        NORMAL,
        SPRING_LOADED,
        SMALL
    }

    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mChildrenOutlineAlpha = 0.0f;
        this.mDrawBackground = true;
        this.mBackgroundAlpha = 0.0f;
        this.mWallpaperScrollRatio = 1.0f;
        this.mTargetCell = new int[2];
        this.mDragOverX = -1;
        this.mDragOverY = -1;
        this.mDragTargetLayout = null;
        this.mDragOverlappingLayout = null;
        this.mDropToLayout = null;
        this.mTempCell = new int[2];
        this.mTempEstimate = new int[2];
        this.mDragViewVisualCenter = new float[2];
        this.mTempDragCoordinates = new float[2];
        this.mTempCellLayoutCenterCoordinates = new float[2];
        this.mTempDragBottomRightCoordinates = new float[2];
        this.mTempInverseMatrix = new Matrix();
        this.mState = State.NORMAL;
        this.mIsSwitchingState = false;
        this.mAnimatingViewIntoPlace = false;
        this.mIsDragOccuring = false;
        this.mChildrenLayersEnabled = true;
        this.mInScrollArea = false;
        this.mOutlineHelper = new HolographicOutlineHelper();
        this.mDragOutline = null;
        this.mTempRect = new Rect();
        this.mTempXY = new int[2];
        this.mTempVisiblePagesRange = new int[2];
        this.mOverscrollFade = 0.0f;
        this.mUpdateWallpaperOffsetImmediately = false;
        this.mDisplaySize = new Point();
        this.mFolderCreationAlarm = new Alarm();
        this.mReorderAlarm = new Alarm();
        this.mDragFolderRingAnimator = null;
        this.mDragOverFolderIcon = null;
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mDragMode = 0;
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
        this.mRestoredPages = new ArrayList<>();
        this.mBindPages = new Runnable() {
            @Override
            public void run() {
                Workspace.this.mLauncher.getModel().bindRemainingSynchronousPages();
            }
        };
        this.mZoomInInterpolator = new ZoomInInterpolator();
        this.mContentIsRefreshable = false;
        this.mOriginalPageSpacing = this.mPageSpacing;
        this.mDragEnforcer = new DropTarget.DragEnforcer(context);
        setDataIsReady();
        this.mLauncher = (Launcher) context;
        Resources res = getResources();
        this.mWorkspaceFadeInAdjacentScreens = res.getBoolean(R.bool.config_workspaceFadeAdjacentScreens);
        this.mFadeInAdjacentScreens = false;
        this.mWallpaperManager = WallpaperManager.getInstance(context);
        int cellCountX = 4;
        int cellCountY = 4;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
        if (LauncherApplication.isScreenLarge()) {
            TypedArray actionBarSizeTypedArray = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
            float actionBarHeight = actionBarSizeTypedArray.getDimension(0, 0.0f);
            Point minDims = new Point();
            Point maxDims = new Point();
            this.mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
            cellCountX = 1;
            while (CellLayout.widthInPortrait(res, cellCountX + 1) <= minDims.x) {
                cellCountX++;
            }
            cellCountY = 1;
            while (CellLayout.heightInLandscape(res, cellCountY + 1) + actionBarHeight <= minDims.y) {
                cellCountY++;
            }
        }
        this.mSpringLoadedShrinkFactor = res.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        this.mSpringLoadedPageSpacing = res.getDimensionPixelSize(R.dimen.workspace_spring_loaded_page_spacing);
        this.mCameraDistance = res.getInteger(R.integer.config_cameraDistance);
        int cellCountX2 = a.getInt(1, cellCountX);
        int cellCountY2 = a.getInt(2, cellCountY);
        this.mDefaultPage = a.getInt(0, 1);
        a.recycle();
        setOnHierarchyChangeListener(this);
        LauncherModel.updateWorkspaceLayoutCells(cellCountX2, cellCountY2);
        setHapticFeedbackEnabled(false);
        initWorkspace();
        setMotionEventSplittingEnabled(true);
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    public int[] estimateItemSize(int hSpan, int vSpan, ItemInfo itemInfo, boolean springLoaded) {
        int[] size = new int[2];
        if (getChildCount() > 0) {
            CellLayout cl = (CellLayout) this.mLauncher.getWorkspace().getChildAt(0);
            Rect r = estimateItemPosition(cl, itemInfo, 0, 0, hSpan, vSpan);
            size[0] = r.width();
            size[1] = r.height();
            if (springLoaded) {
                size[0] = (int) (size[0] * this.mSpringLoadedShrinkFactor);
                size[1] = (int) (size[1] * this.mSpringLoadedShrinkFactor);
            }
        } else {
            size[0] = Integer.MAX_VALUE;
            size[1] = Integer.MAX_VALUE;
        }
        return size;
    }

    public Rect estimateItemPosition(CellLayout cl, ItemInfo pendingInfo, int hCell, int vCell, int hSpan, int vSpan) {
        Rect r = new Rect();
        cl.cellToRect(hCell, vCell, hSpan, vSpan, r);
        return r;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        this.mIsDragOccuring = true;
        updateChildrenLayersEnabled(false);
        this.mLauncher.lockScreenOrientation();
        setChildrenBackgroundAlphaMultipliers(1.0f);
        InstallShortcutReceiver.enableInstallQueue();
        UninstallShortcutReceiver.enableUninstallQueue();
    }

    @Override
    public void onDragEnd() {
        this.mIsDragOccuring = false;
        updateChildrenLayersEnabled(false);
        this.mLauncher.unlockScreenOrientation(false);
        InstallShortcutReceiver.disableAndFlushInstallQueue(getContext());
        UninstallShortcutReceiver.disableAndFlushUninstallQueue(getContext());
    }

    protected void initWorkspace() {
        Context context = getContext();
        this.mCurrentPage = this.mDefaultPage;
        Launcher.setScreen(this.mCurrentPage);
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        this.mIconCache = app.getIconCache();
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);
        setChildrenDrawnWithCacheEnabled(true);
        Resources res = getResources();
        try {
            this.mBackground = res.getDrawable(R.drawable.apps_customize_bg);
        } catch (Resources.NotFoundException e) {
        }
        this.mWallpaperOffset = new WallpaperOffsetInterpolator();
        Display display = this.mLauncher.getWindowManager().getDefaultDisplay();
        display.getSize(this.mDisplaySize);
        this.mWallpaperTravelWidth = (int) (this.mDisplaySize.x * wallpaperTravelToScreenWidthRatio(this.mDisplaySize.x, this.mDisplaySize.y));
        this.mMaxDistanceForFolderCreation = 0.55f * res.getDimensionPixelSize(R.dimen.app_icon_size);
        this.mFlingThresholdVelocity = (int) (500.0f * this.mDensity);
    }

    @Override
    protected int getScrollMode() {
        return 1;
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        CellLayout cl = (CellLayout) child;
        cl.setOnInterceptTouchListener(this);
        cl.setClickable(true);
        cl.setContentDescription(getContext().getString(R.string.workspace_description_format, Integer.valueOf(getChildCount())));
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
    }

    @Override
    protected boolean shouldDrawChild(View child) {
        CellLayout cl = (CellLayout) child;
        return super.shouldDrawChild(child) && (cl.getShortcutsAndWidgets().getAlpha() > 0.0f || cl.getBackgroundAlpha() > 0.0f);
    }

    Folder getOpenFolder() {
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

    void addInScreen(View child, long container, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, container, screen, x, y, spanX, spanY, false);
    }

    void addInScreen(View view, long container, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        CellLayout layout;
        CellLayout.LayoutParams lp;
        if (container == -100 && (screen < 0 || screen >= getChildCount())) {
            Log.e("Launcher.Workspace", "The screen must be >= 0 and < " + getChildCount() + " (was " + screen + "); skipping child");
            return;
        }
        if (container == -101) {
            layout = this.mLauncher.getHotseat().getLayout();
            view.setOnKeyListener(null);
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(false);
            }
            if (screen < 0) {
                screen = this.mLauncher.getHotseat().getOrderInHotseat(x, y);
            } else {
                x = this.mLauncher.getHotseat().getCellXFromOrder(screen);
                y = this.mLauncher.getHotseat().getCellYFromOrder(screen);
            }
        } else {
            if (view instanceof FolderIcon) {
                ((FolderIcon) view).setTextVisible(true);
            }
            layout = (CellLayout) getChildAt(screen);
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
        int childId = LauncherModel.getCellLayoutChildId(container, screen, x, y, spanX, spanY);
        boolean markCellsAsOccupied = !(view instanceof Folder);
        if (!layout.addViewToCellLayout(view, insert ? 0 : -1, childId, lp, markCellsAsOccupied)) {
            Log.w("Launcher.Workspace", "Failed to add to item at (" + lp.cellX + "," + lp.cellY + ") to CellLayout");
        }
        if (!(view instanceof Folder)) {
            view.setHapticFeedbackEnabled(false);
            view.setOnLongClickListener(this.mLongClickListener);
        }
        if (view instanceof DropTarget) {
            this.mDragController.addDropTarget((DropTarget) view);
        }
    }

    private boolean hitsPage(int index, float x, float y) {
        View page = getChildAt(index);
        if (page == null) {
            return false;
        }
        float[] localXY = {x, y};
        mapPointFromSelfToChild(page, localXY);
        return localXY[0] >= 0.0f && localXY[0] < ((float) page.getWidth()) && localXY[1] >= 0.0f && localXY[1] < ((float) page.getHeight());
    }

    @Override
    protected boolean hitsPreviousPage(float x, float y) {
        int current = this.mNextPage == -1 ? this.mCurrentPage : this.mNextPage;
        return LauncherApplication.isScreenLarge() && hitsPage(current + (-1), x, y);
    }

    @Override
    protected boolean hitsNextPage(float x, float y) {
        int current = this.mNextPage == -1 ? this.mCurrentPage : this.mNextPage;
        return LauncherApplication.isScreenLarge() && hitsPage(current + 1, x, y);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return isSmall() || !isFinishedSwitchingState();
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
        if (isSmall() || !isFinishedSwitchingState()) {
            return false;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & 255) {
            case 0:
                this.mXDown = ev.getX();
                this.mYDown = ev.getY();
                break;
            case 1:
            case 6:
                if (this.mTouchState == 0) {
                    CellLayout currentPage = (CellLayout) getChildAt(this.mCurrentPage);
                    if (!currentPage.lastDownOnOccupiedCell()) {
                        onWallpaperTap(ev);
                    }
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    protected void reinflateWidgetsIfNecessary() {
        int clCount = getChildCount();
        for (int i = 0; i < clCount; i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            ShortcutAndWidgetContainer swc = cl.getShortcutsAndWidgets();
            int itemCount = swc.getChildCount();
            for (int j = 0; j < itemCount; j++) {
                View v = swc.getChildAt(j);
                if (v.getTag() instanceof LauncherAppWidgetInfo) {
                    LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) v.getTag();
                    LauncherAppWidgetHostView lahv = (LauncherAppWidgetHostView) info.hostView;
                    if (lahv != null && lahv.orientationChangedSincedInflation()) {
                        this.mLauncher.removeAppWidget(info);
                        cl.removeView(lahv);
                        this.mLauncher.bindAppWidget(info);
                    }
                }
            }
        }
    }

    @Override
    protected void determineScrollingStart(MotionEvent ev) {
        if (!isSmall() && isFinishedSwitchingState()) {
            float deltaX = Math.abs(ev.getX() - this.mXDown);
            float deltaY = Math.abs(ev.getY() - this.mYDown);
            if (Float.compare(deltaX, 0.0f) != 0) {
                float slope = deltaY / deltaX;
                float theta = (float) Math.atan(slope);
                if (deltaX > this.mTouchSlop || deltaY > this.mTouchSlop) {
                    cancelCurrentPageLongPress();
                }
                if (theta <= 1.0471976f) {
                    if (theta > 0.5235988f) {
                        float extraRatio = (float) Math.sqrt((theta - 0.5235988f) / 0.5235988f);
                        super.determineScrollingStart(ev, 1.0f + (4.0f * extraRatio));
                    } else {
                        super.determineScrollingStart(ev);
                    }
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
        if (LauncherApplication.isScreenLarge()) {
            showOutlines();
            this.mIsStaticWallpaper = this.mWallpaperManager.getWallpaperInfo() == null;
        }
        if (!this.mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getChildCount(); i++) {
                ((CellLayout) getPageAt(i)).setShortcutAndWidgetAlpha(1.0f);
            }
        }
        showScrollingIndicator(false);
    }

    @Override
    protected void onPageEndMoving() {
        super.onPageEndMoving();
        if (isHardwareAccelerated()) {
            updateChildrenLayersEnabled(false);
        } else {
            clearChildrenCache();
        }
        if (this.mDragController.isDragging()) {
            if (isSmall()) {
                this.mDragController.forceTouchMove();
            }
        } else {
            if (LauncherApplication.isScreenLarge()) {
                hideOutlines();
            }
            if (!this.mDragController.isDragging()) {
                hideScrollingIndicator(false);
            }
        }
        if (this.mDelayedResizeRunnable != null) {
            this.mDelayedResizeRunnable.run();
            this.mDelayedResizeRunnable = null;
        }
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
            this.mDelayedSnapToPageRunnable = null;
        }
    }

    @Override
    protected void notifyPageSwitchListener() {
        super.notifyPageSwitchListener();
        Launcher.setScreen(this.mCurrentPage);
    }

    private float wallpaperTravelToScreenWidthRatio(int width, int height) {
        float aspectRatio = width / height;
        return (0.30769226f * aspectRatio) + 1.0076923f;
    }

    private int getScrollRange() {
        return getChildOffset(getChildCount() - 1) - getChildOffset(0);
    }

    protected void setWallpaperDimension() {
        Point minDims = new Point();
        Point maxDims = new Point();
        this.mLauncher.getWindowManager().getDefaultDisplay().getCurrentSizeRange(minDims, maxDims);
        int maxDim = Math.max(maxDims.x, maxDims.y);
        int minDim = Math.min(minDims.x, minDims.y);
        if (LauncherApplication.isScreenLarge()) {
            this.mWallpaperWidth = (int) (maxDim * wallpaperTravelToScreenWidthRatio(maxDim, minDim));
            this.mWallpaperHeight = maxDim;
        } else {
            this.mWallpaperWidth = Math.max((int) (minDim * 2.0f), maxDim);
            this.mWallpaperHeight = maxDim;
        }
        new Thread("setWallpaperDimension") {
            @Override
            public void run() {
                Workspace.this.mWallpaperManager.suggestDesiredDimensions(Workspace.this.mWallpaperWidth, Workspace.this.mWallpaperHeight);
            }
        }.start();
    }

    private float wallpaperOffsetForCurrentScroll() {
        this.mWallpaperManager.setWallpaperOffsetSteps(1.0f / (getChildCount() - 1), 1.0f);
        float layoutScale = this.mLayoutScale;
        this.mLayoutScale = 1.0f;
        int scrollRange = getScrollRange();
        float adjustedScrollX = Math.max(0, Math.min(getScrollX(), this.mMaxScrollX));
        float adjustedScrollX2 = adjustedScrollX * this.mWallpaperScrollRatio;
        this.mLayoutScale = layoutScale;
        float scrollProgress = adjustedScrollX2 / scrollRange;
        if (!LauncherApplication.isScreenLarge() || !this.mIsStaticWallpaper) {
            return scrollProgress;
        }
        int wallpaperTravelWidth = Math.min(this.mWallpaperTravelWidth, this.mWallpaperWidth);
        float offsetInDips = (wallpaperTravelWidth * scrollProgress) + ((this.mWallpaperWidth - wallpaperTravelWidth) / 2);
        return offsetInDips / this.mWallpaperWidth;
    }

    private void syncWallpaperOffsetWithScroll() {
        boolean enableWallpaperEffects = isHardwareAccelerated();
        if (enableWallpaperEffects) {
            this.mWallpaperOffset.setFinalX(wallpaperOffsetForCurrentScroll());
        }
    }

    private void updateWallpaperOffsets() {
        boolean keepUpdating;
        boolean updateNow;
        if (this.mUpdateWallpaperOffsetImmediately) {
            updateNow = true;
            keepUpdating = false;
            this.mWallpaperOffset.jumpToFinal();
            this.mUpdateWallpaperOffsetImmediately = false;
        } else {
            keepUpdating = this.mWallpaperOffset.computeScrollOffset();
            updateNow = keepUpdating;
        }
        if (updateNow && this.mWindowToken != null) {
            this.mWallpaperManager.setWallpaperOffsets(this.mWindowToken, this.mWallpaperOffset.getCurrX(), this.mWallpaperOffset.getCurrY());
        }
        if (keepUpdating) {
            invalidate();
        }
    }

    @Override
    protected void updateCurrentPageScroll() {
        super.updateCurrentPageScroll();
        computeWallpaperScrollRatio(this.mCurrentPage);
    }

    @Override
    protected void snapToPage(int whichPage) {
        super.snapToPage(whichPage);
        computeWallpaperScrollRatio(whichPage);
    }

    @Override
    protected void snapToPage(int whichPage, int duration) {
        super.snapToPage(whichPage, duration);
        computeWallpaperScrollRatio(whichPage);
    }

    protected void snapToPage(int whichPage, Runnable r) {
        if (this.mDelayedSnapToPageRunnable != null) {
            this.mDelayedSnapToPageRunnable.run();
        }
        this.mDelayedSnapToPageRunnable = r;
        snapToPage(whichPage, 950);
    }

    private void computeWallpaperScrollRatio(int page) {
        float layoutScale = this.mLayoutScale;
        int scaled = getChildOffset(page) - getRelativeChildOffset(page);
        this.mLayoutScale = 1.0f;
        float unscaled = getChildOffset(page) - getRelativeChildOffset(page);
        this.mLayoutScale = layoutScale;
        if (scaled > 0) {
            this.mWallpaperScrollRatio = (1.0f * unscaled) / scaled;
        } else {
            this.mWallpaperScrollRatio = 1.0f;
        }
    }

    class WallpaperOffsetInterpolator {
        boolean mIsMovingFast;
        long mLastWallpaperOffsetUpdateTime;
        boolean mOverrideHorizontalCatchupConstant;
        float mFinalHorizontalWallpaperOffset = 0.0f;
        float mFinalVerticalWallpaperOffset = 0.5f;
        float mHorizontalWallpaperOffset = 0.0f;
        float mVerticalWallpaperOffset = 0.5f;
        float mHorizontalCatchupConstant = 0.35f;
        float mVerticalCatchupConstant = 0.35f;

        public WallpaperOffsetInterpolator() {
        }

        public void setOverrideHorizontalCatchupConstant(boolean override) {
            this.mOverrideHorizontalCatchupConstant = override;
        }

        public boolean computeScrollOffset() {
            float fractionToCatchUpIn1MsHorizontal;
            if (Float.compare(this.mHorizontalWallpaperOffset, this.mFinalHorizontalWallpaperOffset) == 0 && Float.compare(this.mVerticalWallpaperOffset, this.mFinalVerticalWallpaperOffset) == 0) {
                this.mIsMovingFast = false;
                return false;
            }
            boolean isLandscape = Workspace.this.mDisplaySize.x > Workspace.this.mDisplaySize.y;
            long currentTime = System.currentTimeMillis();
            long timeSinceLastUpdate = Math.max(1L, Math.min(33L, currentTime - this.mLastWallpaperOffsetUpdateTime));
            float xdiff = Math.abs(this.mFinalHorizontalWallpaperOffset - this.mHorizontalWallpaperOffset);
            if (!this.mIsMovingFast && xdiff > 0.07d) {
                this.mIsMovingFast = true;
            }
            if (this.mOverrideHorizontalCatchupConstant) {
                fractionToCatchUpIn1MsHorizontal = this.mHorizontalCatchupConstant;
            } else if (this.mIsMovingFast) {
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.5f : 0.75f;
            } else {
                fractionToCatchUpIn1MsHorizontal = isLandscape ? 0.27f : 0.5f;
            }
            float fractionToCatchUpIn1MsVertical = this.mVerticalCatchupConstant;
            float fractionToCatchUpIn1MsHorizontal2 = fractionToCatchUpIn1MsHorizontal / 33.0f;
            float fractionToCatchUpIn1MsVertical2 = fractionToCatchUpIn1MsVertical / 33.0f;
            float hOffsetDelta = this.mFinalHorizontalWallpaperOffset - this.mHorizontalWallpaperOffset;
            float vOffsetDelta = this.mFinalVerticalWallpaperOffset - this.mVerticalWallpaperOffset;
            boolean jumpToFinalValue = Math.abs(hOffsetDelta) < 1.0E-5f && Math.abs(vOffsetDelta) < 1.0E-5f;
            if (!LauncherApplication.isScreenLarge() || jumpToFinalValue) {
                this.mHorizontalWallpaperOffset = this.mFinalHorizontalWallpaperOffset;
                this.mVerticalWallpaperOffset = this.mFinalVerticalWallpaperOffset;
            } else {
                float percentToCatchUpVertical = Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsVertical2);
                float percentToCatchUpHorizontal = Math.min(1.0f, timeSinceLastUpdate * fractionToCatchUpIn1MsHorizontal2);
                this.mHorizontalWallpaperOffset += percentToCatchUpHorizontal * hOffsetDelta;
                this.mVerticalWallpaperOffset += percentToCatchUpVertical * vOffsetDelta;
            }
            this.mLastWallpaperOffsetUpdateTime = System.currentTimeMillis();
            return true;
        }

        public float getCurrX() {
            return this.mHorizontalWallpaperOffset;
        }

        public float getCurrY() {
            return this.mVerticalWallpaperOffset;
        }

        public void setFinalX(float x) {
            this.mFinalHorizontalWallpaperOffset = Math.max(0.0f, Math.min(x, 1.0f));
        }

        public void jumpToFinal() {
            this.mHorizontalWallpaperOffset = this.mFinalHorizontalWallpaperOffset;
            this.mVerticalWallpaperOffset = this.mFinalVerticalWallpaperOffset;
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        syncWallpaperOffsetWithScroll();
    }

    void showOutlines() {
        if (!isSmall() && !this.mIsSwitchingState) {
            if (this.mChildrenOutlineFadeOutAnimation != null) {
                this.mChildrenOutlineFadeOutAnimation.cancel();
            }
            if (this.mChildrenOutlineFadeInAnimation != null) {
                this.mChildrenOutlineFadeInAnimation.cancel();
            }
            this.mChildrenOutlineFadeInAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 1.0f);
            this.mChildrenOutlineFadeInAnimation.setDuration(100L);
            this.mChildrenOutlineFadeInAnimation.start();
        }
    }

    void hideOutlines() {
        if (!isSmall() && !this.mIsSwitchingState) {
            if (this.mChildrenOutlineFadeInAnimation != null) {
                this.mChildrenOutlineFadeInAnimation.cancel();
            }
            if (this.mChildrenOutlineFadeOutAnimation != null) {
                this.mChildrenOutlineFadeOutAnimation.cancel();
            }
            this.mChildrenOutlineFadeOutAnimation = LauncherAnimUtils.ofFloat(this, "childrenOutlineAlpha", 0.0f);
            this.mChildrenOutlineFadeOutAnimation.setDuration(375L);
            this.mChildrenOutlineFadeOutAnimation.setStartDelay(0L);
            this.mChildrenOutlineFadeOutAnimation.start();
        }
    }

    public void showOutlinesTemporarily() {
        if (!this.mIsPageMoving && !isTouchActive()) {
            snapToPage(this.mCurrentPage);
        }
    }

    public void setChildrenOutlineAlpha(float alpha) {
        this.mChildrenOutlineAlpha = alpha;
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout cl = (CellLayout) getChildAt(i);
            cl.setBackgroundAlpha(alpha);
        }
    }

    public float getChildrenOutlineAlpha() {
        return this.mChildrenOutlineAlpha;
    }

    private void animateBackgroundGradient(float finalAlpha, boolean animated) {
        if (this.mBackground != null) {
            if (this.mBackgroundFadeInAnimation != null) {
                this.mBackgroundFadeInAnimation.cancel();
                this.mBackgroundFadeInAnimation = null;
            }
            if (this.mBackgroundFadeOutAnimation != null) {
                this.mBackgroundFadeOutAnimation.cancel();
                this.mBackgroundFadeOutAnimation = null;
            }
            float startAlpha = getBackgroundAlpha();
            if (finalAlpha != startAlpha) {
                if (animated) {
                    this.mBackgroundFadeOutAnimation = LauncherAnimUtils.ofFloat(this, startAlpha, finalAlpha);
                    this.mBackgroundFadeOutAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            Workspace.this.setBackgroundAlpha(((Float) animation.getAnimatedValue()).floatValue());
                        }
                    });
                    this.mBackgroundFadeOutAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
                    this.mBackgroundFadeOutAnimation.setDuration(350L);
                    this.mBackgroundFadeOutAnimation.start();
                    return;
                }
                setBackgroundAlpha(finalAlpha);
            }
        }
    }

    public void setBackgroundAlpha(float alpha) {
        if (alpha != this.mBackgroundAlpha) {
            this.mBackgroundAlpha = alpha;
            invalidate();
        }
    }

    public float getBackgroundAlpha() {
        return this.mBackgroundAlpha;
    }

    float backgroundAlphaInterpolator(float r) {
        if (r < 0.1f) {
            return 0.0f;
        }
        if (r <= 0.4f) {
            return (r - 0.1f) / (0.4f - 0.1f);
        }
        return 1.0f;
    }

    private void updatePageAlphaValues(int screenCenter) {
        boolean isInOverscroll = this.mOverScrollX < 0 || this.mOverScrollX > this.mMaxScrollX;
        if (this.mWorkspaceFadeInAdjacentScreens && this.mState == State.NORMAL && !this.mIsSwitchingState && !isInOverscroll) {
            for (int i = 0; i < getChildCount(); i++) {
                CellLayout child = (CellLayout) getChildAt(i);
                if (child != null) {
                    float scrollProgress = getScrollProgress(screenCenter, child, i);
                    float alpha = 1.0f - Math.abs(scrollProgress);
                    child.getShortcutsAndWidgets().setAlpha(alpha);
                    if (!this.mIsDragOccuring) {
                        child.setBackgroundAlphaMultiplier(backgroundAlphaInterpolator(Math.abs(scrollProgress)));
                    } else {
                        child.setBackgroundAlphaMultiplier(1.0f);
                    }
                }
            }
        }
    }

    private void setChildrenBackgroundAlphaMultipliers(float a) {
        for (int i = 0; i < getChildCount(); i++) {
            CellLayout child = (CellLayout) getChildAt(i);
            child.setBackgroundAlphaMultiplier(a);
        }
    }

    @Override
    protected void screenScrolled(int screenCenter) {
        int index;
        float pivotX;
        boolean isLeftPage;
        boolean isRtl = isLayoutRtl();
        super.screenScrolled(screenCenter);
        updatePageAlphaValues(screenCenter);
        enableHwLayersOnVisiblePages();
        if (this.mOverScrollX < 0 || this.mOverScrollX > this.mMaxScrollX) {
            int upperIndex = getChildCount() - 1;
            if (isRtl) {
                index = this.mOverScrollX < 0 ? upperIndex : 0;
                pivotX = index == 0 ? 0.25f : 0.75f;
            } else {
                index = this.mOverScrollX < 0 ? 0 : upperIndex;
                pivotX = index == 0 ? 0.75f : 0.25f;
            }
            CellLayout cl = (CellLayout) getChildAt(index);
            float scrollProgress = getScrollProgress(screenCenter, cl, index);
            if (isRtl) {
                isLeftPage = index > 0;
            } else {
                isLeftPage = index == 0;
            }
            cl.setOverScrollAmount(Math.abs(scrollProgress), isLeftPage);
            float rotation = (-24.0f) * scrollProgress;
            cl.setRotationY(rotation);
            setFadeForOverScroll(Math.abs(scrollProgress));
            if (!this.mOverscrollTransformsSet) {
                this.mOverscrollTransformsSet = true;
                cl.setCameraDistance(this.mDensity * this.mCameraDistance);
                cl.setPivotX(cl.getMeasuredWidth() * pivotX);
                cl.setPivotY(cl.getMeasuredHeight() * 0.5f);
                cl.setOverscrollTransformsDirty(true);
                return;
            }
            return;
        }
        if (this.mOverscrollFade != 0.0f) {
            setFadeForOverScroll(0.0f);
        }
        if (this.mOverscrollTransformsSet) {
            this.mOverscrollTransformsSet = false;
            ((CellLayout) getChildAt(0)).resetOverscrollTransforms();
            ((CellLayout) getChildAt(getChildCount() - 1)).resetOverscrollTransforms();
        }
    }

    @Override
    protected void overScroll(float amount) {
        acceleratedOverScroll(amount);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mWindowToken = getWindowToken();
        computeScroll();
        this.mDragController.setWindowToken(this.mWindowToken);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mWindowToken = null;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mFirstLayout && this.mCurrentPage >= 0 && this.mCurrentPage < getChildCount()) {
            this.mUpdateWallpaperOffsetImmediately = true;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateWallpaperOffsets();
        if (this.mBackground != null && this.mBackgroundAlpha > 0.0f && this.mDrawBackground) {
            int alpha = (int) (this.mBackgroundAlpha * 255.0f);
            this.mBackground.setAlpha(alpha);
            this.mBackground.setBounds(getScrollX(), 0, getScrollX() + getMeasuredWidth(), getMeasuredHeight());
            this.mBackground.draw(canvas);
        }
        super.onDraw(canvas);
        post(this.mBindPages);
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!this.mLauncher.isAllAppsVisible()) {
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
        if (isSmall()) {
            return 393216;
        }
        return super.getDescendantFocusability();
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!this.mLauncher.isAllAppsVisible()) {
            Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                openFolder.addFocusables(views, direction);
            } else {
                super.addFocusables(views, direction, focusableMode);
            }
        }
    }

    public boolean isSmall() {
        return this.mState == State.SMALL || this.mState == State.SPRING_LOADED;
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
        boolean small = this.mState == State.SMALL || this.mIsSwitchingState;
        boolean enableChildrenLayers = force || small || this.mAnimatingViewIntoPlace || isPageMoving();
        if (enableChildrenLayers != this.mChildrenLayersEnabled) {
            this.mChildrenLayersEnabled = enableChildrenLayers;
            if (this.mChildrenLayersEnabled) {
                enableHwLayersOnVisiblePages();
                return;
            }
            for (int i = 0; i < getPageCount(); i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.disableHardwareLayers();
            }
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
            for (int i = 0; i < screenCount; i++) {
                CellLayout layout = (CellLayout) getPageAt(i);
                if (leftScreen > i || i > rightScreen || !shouldDrawChild(layout)) {
                    layout.disableHardwareLayers();
                }
            }
            for (int i2 = 0; i2 < screenCount; i2++) {
                CellLayout layout2 = (CellLayout) getPageAt(i2);
                if (leftScreen <= i2 && i2 <= rightScreen && shouldDrawChild(layout2)) {
                    layout2.enableHardwareLayers();
                }
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

    static class ZInterpolator implements TimeInterpolator {
        private float focalLength;

        public ZInterpolator(float foc) {
            this.focalLength = foc;
        }

        @Override
        public float getInterpolation(float input) {
            return (1.0f - (this.focalLength / (this.focalLength + input))) / (1.0f - (this.focalLength / (this.focalLength + 1.0f)));
        }
    }

    static class InverseZInterpolator implements TimeInterpolator {
        private ZInterpolator zInterpolator;

        public InverseZInterpolator(float foc) {
            this.zInterpolator = new ZInterpolator(foc);
        }

        @Override
        public float getInterpolation(float input) {
            return 1.0f - this.zInterpolator.getInterpolation(1.0f - input);
        }
    }

    static class ZoomOutInterpolator implements TimeInterpolator {
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(0.75f);
        private final ZInterpolator zInterpolator = new ZInterpolator(0.13f);

        ZoomOutInterpolator() {
        }

        @Override
        public float getInterpolation(float input) {
            return this.decelerate.getInterpolation(this.zInterpolator.getInterpolation(input));
        }
    }

    static class ZoomInInterpolator implements TimeInterpolator {
        private final InverseZInterpolator inverseZInterpolator = new InverseZInterpolator(0.35f);
        private final DecelerateInterpolator decelerate = new DecelerateInterpolator(3.0f);

        ZoomInInterpolator() {
        }

        @Override
        public float getInterpolation(float input) {
            return this.decelerate.getInterpolation(this.inverseZInterpolator.getInterpolation(input));
        }
    }

    public void onDragStartedWithItem(View v) {
        Canvas canvas = new Canvas();
        this.mDragOutline = createDragOutline(v, canvas, 2);
    }

    public void onDragStartedWithItem(PendingAddItemInfo info, Bitmap b, boolean clipAlpha) {
        Canvas canvas = new Canvas();
        int[] size = estimateItemSize(info.spanX, info.spanY, info, false);
        this.mDragOutline = createDragOutline(b, canvas, 2, size[0], size[1], clipAlpha);
    }

    public void exitWidgetResizeMode() {
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        dragLayer.clearAllResizeFrames();
    }

    private void initAnimationArrays() {
        int childCount = getChildCount();
        if (this.mOldTranslationXs == null) {
            this.mOldTranslationXs = new float[childCount];
            this.mOldTranslationYs = new float[childCount];
            this.mOldScaleXs = new float[childCount];
            this.mOldScaleYs = new float[childCount];
            this.mOldBackgroundAlphas = new float[childCount];
            this.mOldAlphas = new float[childCount];
            this.mNewTranslationXs = new float[childCount];
            this.mNewTranslationYs = new float[childCount];
            this.mNewScaleXs = new float[childCount];
            this.mNewScaleYs = new float[childCount];
            this.mNewBackgroundAlphas = new float[childCount];
            this.mNewAlphas = new float[childCount];
            this.mNewRotationYs = new float[childCount];
        }
    }

    Animator getChangeStateAnimation(State state, boolean animated) {
        return getChangeStateAnimation(state, animated, 0);
    }

    Animator getChangeStateAnimation(State state, boolean animated, int delay) {
        if (this.mState == state) {
            return null;
        }
        initAnimationArrays();
        AnimatorSet anim = animated ? LauncherAnimUtils.createAnimatorSet() : null;
        setCurrentPage(getNextPage());
        State oldState = this.mState;
        boolean oldStateIsNormal = oldState == State.NORMAL;
        boolean oldStateIsSpringLoaded = oldState == State.SPRING_LOADED;
        boolean oldStateIsSmall = oldState == State.SMALL;
        this.mState = state;
        boolean stateIsNormal = state == State.NORMAL;
        boolean stateIsSpringLoaded = state == State.SPRING_LOADED;
        boolean stateIsSmall = state == State.SMALL;
        float finalScaleFactor = 1.0f;
        float finalBackgroundAlpha = stateIsSpringLoaded ? 1.0f : 0.0f;
        boolean zoomIn = true;
        if (state != State.NORMAL) {
            finalScaleFactor = this.mSpringLoadedShrinkFactor - (stateIsSmall ? 0.1f : 0.0f);
            setPageSpacing(this.mSpringLoadedPageSpacing);
            if (oldStateIsNormal && stateIsSmall) {
                zoomIn = false;
                setLayoutScale(finalScaleFactor);
                updateChildrenLayersEnabled(false);
            } else {
                finalBackgroundAlpha = 1.0f;
                setLayoutScale(finalScaleFactor);
            }
        } else {
            setPageSpacing(this.mOriginalPageSpacing);
            setLayoutScale(1.0f);
        }
        int duration = zoomIn ? getResources().getInteger(R.integer.config_workspaceUnshrinkTime) : getResources().getInteger(R.integer.config_appsCustomizeWorkspaceShrinkTime);
        int i = 0;
        while (i < getChildCount()) {
            CellLayout cl = (CellLayout) getChildAt(i);
            float finalAlpha = (!this.mWorkspaceFadeInAdjacentScreens || stateIsSpringLoaded || i == this.mCurrentPage) ? 1.0f : 0.0f;
            float currentAlpha = cl.getShortcutsAndWidgets().getAlpha();
            float initialAlpha = currentAlpha;
            if ((oldStateIsSmall && stateIsNormal) || (oldStateIsNormal && stateIsSmall)) {
                if (i == this.mCurrentPage || !animated || oldStateIsSpringLoaded) {
                    finalAlpha = 1.0f;
                } else {
                    initialAlpha = 0.0f;
                    finalAlpha = 0.0f;
                }
            }
            this.mOldAlphas[i] = initialAlpha;
            this.mNewAlphas[i] = finalAlpha;
            if (animated) {
                this.mOldTranslationXs[i] = cl.getTranslationX();
                this.mOldTranslationYs[i] = cl.getTranslationY();
                this.mOldScaleXs[i] = cl.getScaleX();
                this.mOldScaleYs[i] = cl.getScaleY();
                this.mOldBackgroundAlphas[i] = cl.getBackgroundAlpha();
                this.mNewTranslationXs[i] = 0.0f;
                this.mNewTranslationYs[i] = 0.0f;
                this.mNewScaleXs[i] = finalScaleFactor;
                this.mNewScaleYs[i] = finalScaleFactor;
                this.mNewBackgroundAlphas[i] = finalBackgroundAlpha;
            } else {
                cl.setTranslationX(0.0f);
                cl.setTranslationY(0.0f);
                cl.setScaleX(finalScaleFactor);
                cl.setScaleY(finalScaleFactor);
                cl.setBackgroundAlpha(finalBackgroundAlpha);
                cl.setShortcutAndWidgetAlpha(finalAlpha);
            }
            i++;
        }
        if (animated) {
            for (int index = 0; index < getChildCount(); index++) {
                final int i2 = index;
                final CellLayout cl2 = (CellLayout) getChildAt(i2);
                float currentAlpha2 = cl2.getShortcutsAndWidgets().getAlpha();
                if (this.mOldAlphas[i2] == 0.0f && this.mNewAlphas[i2] == 0.0f) {
                    cl2.setTranslationX(this.mNewTranslationXs[i2]);
                    cl2.setTranslationY(this.mNewTranslationYs[i2]);
                    cl2.setScaleX(this.mNewScaleXs[i2]);
                    cl2.setScaleY(this.mNewScaleYs[i2]);
                    cl2.setBackgroundAlpha(this.mNewBackgroundAlphas[i2]);
                    cl2.setShortcutAndWidgetAlpha(this.mNewAlphas[i2]);
                    cl2.setRotationY(this.mNewRotationYs[i2]);
                } else {
                    LauncherViewPropertyAnimator a = new LauncherViewPropertyAnimator(cl2);
                    a.translationX(this.mNewTranslationXs[i2]).translationY(this.mNewTranslationYs[i2]).scaleX(this.mNewScaleXs[i2]).scaleY(this.mNewScaleYs[i2]).setDuration(duration).setInterpolator(this.mZoomInInterpolator);
                    anim.play(a);
                    if (this.mOldAlphas[i2] != this.mNewAlphas[i2] || currentAlpha2 != this.mNewAlphas[i2]) {
                        LauncherViewPropertyAnimator alphaAnim = new LauncherViewPropertyAnimator(cl2.getShortcutsAndWidgets());
                        alphaAnim.alpha(this.mNewAlphas[i2]).setDuration(duration).setInterpolator(this.mZoomInInterpolator);
                        anim.play(alphaAnim);
                    }
                    if (this.mOldBackgroundAlphas[i2] != 0.0f || this.mNewBackgroundAlphas[i2] != 0.0f) {
                        ValueAnimator bgAnim = LauncherAnimUtils.ofFloat(cl2, 0.0f, 1.0f).setDuration(duration);
                        bgAnim.setInterpolator(this.mZoomInInterpolator);
                        bgAnim.addUpdateListener(new LauncherAnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(float a2, float b) {
                                cl2.setBackgroundAlpha((Workspace.this.mOldBackgroundAlphas[i2] * a2) + (Workspace.this.mNewBackgroundAlphas[i2] * b));
                            }
                        });
                        anim.play(bgAnim);
                    }
                }
            }
            anim.setStartDelay(delay);
        }
        if (stateIsSpringLoaded) {
            animateBackgroundGradient(getResources().getInteger(R.integer.config_appsCustomizeSpringLoadedBgAlpha) / 100.0f, false);
            return anim;
        }
        animateBackgroundGradient(0.0f, true);
        return anim;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
        this.mIsSwitchingState = true;
        updateChildrenLayersEnabled(false);
        cancelScrollingIndicatorAnimations();
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
        this.mIsSwitchingState = false;
        this.mWallpaperOffset.setOverrideHorizontalCatchupConstant(false);
        updateChildrenLayersEnabled(false);
        if (!this.mWorkspaceFadeInAdjacentScreens) {
            for (int i = 0; i < getChildCount(); i++) {
                CellLayout cl = (CellLayout) getChildAt(i);
                cl.setShortcutAndWidgetAlpha(1.0f);
            }
        }
    }

    @Override
    public View getContent() {
        return this;
    }

    private void drawDragView(View v, Canvas destCanvas, int padding, boolean pruneToDrawable) {
        Rect clipRect = this.mTempRect;
        v.getDrawingRect(clipRect);
        boolean textVisible = false;
        destCanvas.save();
        if ((v instanceof TextView) && pruneToDrawable) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            clipRect.set(0, 0, d.getIntrinsicWidth() + padding, d.getIntrinsicHeight() + padding);
            destCanvas.translate(padding / 2, padding / 2);
            d.draw(destCanvas);
        } else {
            if (v instanceof FolderIcon) {
                if (((FolderIcon) v).getTextVisible()) {
                    ((FolderIcon) v).setTextVisible(false);
                    textVisible = true;
                }
            } else if (v instanceof BubbleTextView) {
                clipRect.bottom = (r3.getExtendedPaddingTop() - 3) + ((BubbleTextView) v).getLayout().getLineTop(0);
            } else if (v instanceof TextView) {
                TextView tv = (TextView) v;
                clipRect.bottom = (tv.getExtendedPaddingTop() - tv.getCompoundDrawablePadding()) + tv.getLayout().getLineTop(0);
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

    public Bitmap createDragBitmap(View v, Canvas canvas, int padding) {
        Bitmap b;
        if (v instanceof TextView) {
            Drawable d = ((TextView) v).getCompoundDrawables()[1];
            b = Bitmap.createBitmap(d.getIntrinsicWidth() + padding, d.getIntrinsicHeight() + padding, Bitmap.Config.ARGB_8888);
        } else {
            b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        }
        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, true);
        canvas.setBitmap(null);
        return b;
    }

    private Bitmap createDragOutline(View v, Canvas canvas, int padding) {
        int outlineColor = getResources().getColor(android.R.color.white);
        Bitmap b = Bitmap.createBitmap(v.getWidth() + padding, v.getHeight() + padding, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        drawDragView(v, canvas, padding, true);
        this.mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor);
        canvas.setBitmap(null);
        return b;
    }

    private Bitmap createDragOutline(Bitmap orig, Canvas canvas, int padding, int w, int h, boolean clipAlpha) {
        int outlineColor = getResources().getColor(android.R.color.white);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(b);
        Rect src = new Rect(0, 0, orig.getWidth(), orig.getHeight());
        float scaleFactor = Math.min((w - padding) / orig.getWidth(), (h - padding) / orig.getHeight());
        int scaledWidth = (int) (orig.getWidth() * scaleFactor);
        int scaledHeight = (int) (orig.getHeight() * scaleFactor);
        Rect dst = new Rect(0, 0, scaledWidth, scaledHeight);
        dst.offset((w - scaledWidth) / 2, (h - scaledHeight) / 2);
        canvas.drawBitmap(orig, src, dst, (Paint) null);
        this.mOutlineHelper.applyMediumExpensiveOutlineWithBlur(b, canvas, outlineColor, outlineColor, clipAlpha);
        canvas.setBitmap(null);
        return b;
    }

    void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;
        if (child.isInTouchMode()) {
            this.mDragInfo = cellInfo;
            child.setVisibility(4);
            CellLayout layout = (CellLayout) child.getParent().getParent();
            layout.prepareChildForDrag(child);
            child.clearFocus();
            child.setPressed(false);
            Canvas canvas = new Canvas();
            this.mDragOutline = createDragOutline(child, canvas, 2);
            beginDragShared(child, this);
        }
    }

    public void beginDragShared(View child, DragSource source) {
        Resources r = getResources();
        Bitmap b = createDragBitmap(child, new Canvas(), 2);
        int bmpWidth = b.getWidth();
        int bmpHeight = b.getHeight();
        float scale = this.mLauncher.getDragLayer().getLocationInDragLayer(child, this.mTempXY);
        int dragLayerX = Math.round(this.mTempXY[0] - ((bmpWidth - (child.getWidth() * scale)) / 2.0f));
        int dragLayerY = Math.round((this.mTempXY[1] - ((bmpHeight - (bmpHeight * scale)) / 2.0f)) - 1.0f);
        Point dragVisualizeOffset = null;
        Rect dragRect = null;
        if ((child instanceof BubbleTextView) || (child instanceof PagedViewIcon)) {
            int iconSize = r.getDimensionPixelSize(R.dimen.app_icon_size);
            int iconPaddingTop = r.getDimensionPixelSize(R.dimen.app_icon_padding_top);
            int top = child.getPaddingTop();
            int left = (bmpWidth - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;
            dragLayerY += top;
            dragVisualizeOffset = new Point(-1, iconPaddingTop - 1);
            dragRect = new Rect(left, top, right, bottom);
        } else if (child instanceof FolderIcon) {
            int previewSize = r.getDimensionPixelSize(R.dimen.folder_preview_size);
            dragRect = new Rect(0, 0, child.getWidth(), previewSize);
        }
        if (child instanceof BubbleTextView) {
            BubbleTextView icon = (BubbleTextView) child;
            icon.clearPressedOrFocusedBackground();
        }
        this.mDragController.startDrag(b, dragLayerX, dragLayerY, source, child.getTag(), DragController.DRAG_ACTION_MOVE, dragVisualizeOffset, dragRect, scale);
        b.recycle();
        showScrollingIndicator(false);
    }

    void addApplicationShortcut(ShortcutInfo info, CellLayout target, long container, int screen, int cellX, int cellY, boolean insertAtFirst, int intersectX, int intersectY) {
        View view = this.mLauncher.createShortcut(R.layout.application, target, info);
        int[] cellXY = new int[2];
        target.findCellForSpanThatIntersects(cellXY, 1, 1, intersectX, intersectY);
        addInScreen(view, container, screen, cellXY[0], cellXY[1], 1, 1, insertAtFirst);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, info, container, screen, cellXY[0], cellXY[1]);
    }

    public boolean transitionStateShouldAllowDrop() {
        return (!isSwitchingState() || this.mTransitionProgress > 0.5f) && this.mState != State.SMALL;
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
            this.mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, this.mDragViewVisualCenter);
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
            if (willCreateUserFolder((ItemInfo) d.dragInfo, dropTargetLayout, this.mTargetCell, distance, true) || willAddToExistingUserFolder((ItemInfo) d.dragInfo, dropTargetLayout, this.mTargetCell, distance)) {
                return true;
            }
            int[] resultSpan = new int[2];
            this.mTargetCell = dropTargetLayout.createArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, null, this.mTargetCell, resultSpan, 3);
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
        return true;
    }

    boolean willCreateUserFolder(ItemInfo info, CellLayout target, int[] targetCell, float distance, boolean considerTimeout) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }
        boolean hasntMoved = false;
        if (this.mDragInfo != null) {
            hasntMoved = dropOverView == this.mDragInfo.cell;
        }
        if (dropOverView == null || hasntMoved) {
            return false;
        }
        if (considerTimeout && !this.mCreateUserFolderOnDrop) {
            return false;
        }
        boolean aboveShortcut = dropOverView.getTag() instanceof ShortcutInfo;
        boolean willBecomeShortcut = info.itemType == 0 || info.itemType == 1;
        return aboveShortcut && willBecomeShortcut;
    }

    boolean willAddToExistingUserFolder(Object dragInfo, CellLayout target, int[] targetCell, float distance) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (dropOverView != null) {
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) dropOverView.getLayoutParams();
            if (lp.useTmpCoords && (lp.tmpCellX != lp.cellX || lp.tmpCellY != lp.tmpCellY)) {
                return false;
            }
        }
        if (!(dropOverView instanceof FolderIcon)) {
            return false;
        }
        FolderIcon fi = (FolderIcon) dropOverView;
        return fi.acceptDrop(dragInfo);
    }

    boolean createUserFolderIfNecessary(View newView, long container, CellLayout target, int[] targetCell, float distance, boolean external, DragView dragView, Runnable postAnimationRunnable) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View v = target.getChildAt(targetCell[0], targetCell[1]);
        boolean hasntMoved = false;
        if (this.mDragInfo != null) {
            CellLayout cellParent = getParentCellLayoutForView(this.mDragInfo.cell);
            hasntMoved = this.mDragInfo.cellX == targetCell[0] && this.mDragInfo.cellY == targetCell[1] && cellParent == target;
        }
        if (v == null || hasntMoved || !this.mCreateUserFolderOnDrop) {
            return false;
        }
        this.mCreateUserFolderOnDrop = false;
        int screen = targetCell == null ? this.mDragInfo.screen : indexOfChild(target);
        boolean aboveShortcut = v.getTag() instanceof ShortcutInfo;
        boolean willBecomeShortcut = newView.getTag() instanceof ShortcutInfo;
        if (aboveShortcut && willBecomeShortcut) {
            ShortcutInfo sourceInfo = (ShortcutInfo) newView.getTag();
            ShortcutInfo destInfo = (ShortcutInfo) v.getTag();
            if (!external) {
                getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
            }
            Rect folderLocation = new Rect();
            float scale = this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(v, folderLocation);
            target.removeView(v);
            FolderIcon fi = this.mLauncher.addFolder(target, container, screen, targetCell[0], targetCell[1]);
            destInfo.cellX = -1;
            destInfo.cellY = -1;
            sourceInfo.cellX = -1;
            sourceInfo.cellY = -1;
            boolean animate = dragView != null;
            if (animate) {
                fi.performCreateAnimation(destInfo, v, sourceInfo, dragView, folderLocation, scale, postAnimationRunnable);
            } else {
                fi.addItem(destInfo);
                fi.addItem(sourceInfo);
            }
            return true;
        }
        return false;
    }

    boolean addToExistingFolderIfNecessary(View newView, CellLayout target, int[] targetCell, float distance, DropTarget.DragObject d, boolean external) {
        if (distance > this.mMaxDistanceForFolderCreation) {
            return false;
        }
        View dropOverView = target.getChildAt(targetCell[0], targetCell[1]);
        if (!this.mAddToExistingFolderOnDrop) {
            return false;
        }
        this.mAddToExistingFolderOnDrop = false;
        if (!(dropOverView instanceof FolderIcon)) {
            return false;
        }
        FolderIcon fi = (FolderIcon) dropOverView;
        if (!fi.acceptDrop(d.dragInfo)) {
            return false;
        }
        fi.onDrop(d);
        if (!external) {
            getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
        }
        return true;
    }

    @Override
    public void onDrop(DropTarget.DragObject d) {
        final LauncherAppWidgetHostView hostView;
        AppWidgetProviderInfo pinfo;
        this.mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, this.mDragViewVisualCenter);
        final CellLayout dropTargetLayout = this.mDropToLayout;
        if (dropTargetLayout != null) {
            if (this.mLauncher.isHotseatLayout(dropTargetLayout)) {
                mapPointFromSelfToHotseatLayout(this.mLauncher.getHotseat(), this.mDragViewVisualCenter);
            } else {
                mapPointFromSelfToChild(dropTargetLayout, this.mDragViewVisualCenter, null);
            }
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
            if (dropTargetLayout != null) {
                boolean hasMovedLayouts = getParentCellLayoutForView(cell) != dropTargetLayout;
                boolean hasMovedIntoHotseat = this.mLauncher.isHotseatLayout(dropTargetLayout);
                long container = hasMovedIntoHotseat ? -101L : -100L;
                int screen = this.mTargetCell[0] < 0 ? this.mDragInfo.screen : indexOfChild(dropTargetLayout);
                int spanX = this.mDragInfo != null ? this.mDragInfo.spanX : 1;
                int spanY = this.mDragInfo != null ? this.mDragInfo.spanY : 1;
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], spanX, spanY, dropTargetLayout, this.mTargetCell);
                float distance = dropTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                if ((this.mInScrollArea || !createUserFolderIfNecessary(cell, container, dropTargetLayout, this.mTargetCell, distance, false, d.dragView, null)) && !addToExistingFolderIfNecessary(cell, dropTargetLayout, this.mTargetCell, distance, d, false)) {
                    ItemInfo item = (ItemInfo) d.dragInfo;
                    int minSpanX = item.spanX;
                    int minSpanY = item.spanY;
                    if (item.minSpanX > 0 && item.minSpanY > 0) {
                        minSpanX = item.minSpanX;
                        minSpanY = item.minSpanY;
                    }
                    int[] resultSpan = new int[2];
                    this.mTargetCell = dropTargetLayout.createArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, spanX, spanY, cell, this.mTargetCell, resultSpan, 1);
                    boolean foundCell = this.mTargetCell[0] >= 0 && this.mTargetCell[1] >= 0;
                    if (foundCell && (cell instanceof AppWidgetHostView) && (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY)) {
                        resizeOnDrop = true;
                        item.spanX = resultSpan[0];
                        item.spanY = resultSpan[1];
                        AppWidgetHostView awhv = (AppWidgetHostView) cell;
                        AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, this.mLauncher, resultSpan[0], resultSpan[1]);
                    }
                    if (this.mCurrentPage != screen && !hasMovedIntoHotseat) {
                        snapScreen = screen;
                        snapToPage(screen);
                    }
                    if (foundCell) {
                        final ItemInfo info = (ItemInfo) cell.getTag();
                        if (hasMovedLayouts) {
                            getParentCellLayoutForView(cell).removeView(cell);
                            addInScreen(cell, container, screen, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY);
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
                        cell.setId(LauncherModel.getCellLayoutChildId(container, this.mDragInfo.screen, this.mTargetCell[0], this.mTargetCell[1], this.mDragInfo.spanX, this.mDragInfo.spanY));
                        if (container != -101 && (cell instanceof LauncherAppWidgetHostView) && (pinfo = (hostView = (LauncherAppWidgetHostView) cell).getAppWidgetInfo()) != null && pinfo.resizeMode != 0) {
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
                        LauncherModel.moveItemInDatabase(this.mLauncher, info, container, screen, lp.cellX, lp.cellY);
                    } else {
                        CellLayout.LayoutParams lp2 = (CellLayout.LayoutParams) cell.getLayoutParams();
                        this.mTargetCell[0] = lp2.cellX;
                        this.mTargetCell[1] = lp2.cellY;
                        CellLayout layout = (CellLayout) cell.getParent().getParent();
                        layout.markCellsAsOccupiedForView(cell);
                    }
                } else {
                    return;
                }
            }
            CellLayout parent = (CellLayout) cell.getParent().getParent();
            final Runnable finalResizeRunnable = resizeRunnable;
            Runnable onCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    Workspace.this.mAnimatingViewIntoPlace = false;
                    Workspace.this.updateChildrenLayersEnabled(false);
                    if (finalResizeRunnable != null) {
                        finalResizeRunnable.run();
                    }
                }
            };
            this.mAnimatingViewIntoPlace = true;
            if (d.dragView.hasDrawn()) {
                ItemInfo info2 = (ItemInfo) cell.getTag();
                if (info2.itemType == 4) {
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

    public void setFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            this.mSavedScrollX = getScrollX();
            CellLayout cl = (CellLayout) getChildAt(screen);
            this.mSavedTranslationX = cl.getTranslationX();
            this.mSavedRotationY = cl.getRotationY();
            int newX = getChildOffset(screen) - getRelativeChildOffset(screen);
            setScrollX(newX);
            cl.setTranslationX(0.0f);
            cl.setRotationY(0.0f);
        }
    }

    public void resetFinalScrollForPageChange(int screen) {
        if (screen >= 0) {
            CellLayout cl = (CellLayout) getChildAt(screen);
            setScrollX(this.mSavedScrollX);
            cl.setTranslationX(this.mSavedTranslationX);
            cl.setRotationY(this.mSavedRotationY);
        }
    }

    @Override
    public void onDragEnter(DropTarget.DragObject d) {
        this.mDragEnforcer.onDragEnter();
        this.mCreateUserFolderOnDrop = false;
        this.mAddToExistingFolderOnDrop = false;
        this.mDropToLayout = null;
        CellLayout layout = getCurrentDropLayout();
        setCurrentDropLayout(layout);
        setCurrentDragOverlappingLayout(layout);
        if (LauncherApplication.isScreenLarge()) {
            showOutlines();
        }
    }

    static Rect getCellLayoutMetrics(Launcher launcher, int orientation) {
        Resources res = launcher.getResources();
        Display display = launcher.getWindowManager().getDefaultDisplay();
        Point smallestSize = new Point();
        Point largestSize = new Point();
        display.getCurrentSizeRange(smallestSize, largestSize);
        if (orientation == 0) {
            if (mLandscapeCellLayoutMetrics == null) {
                int paddingLeft = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
                int paddingRight = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
                int paddingTop = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
                int paddingBottom = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
                int width = (largestSize.x - paddingLeft) - paddingRight;
                int height = (smallestSize.y - paddingTop) - paddingBottom;
                mLandscapeCellLayoutMetrics = new Rect();
                CellLayout.getMetrics(mLandscapeCellLayoutMetrics, res, width, height, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(), orientation);
            }
            return mLandscapeCellLayoutMetrics;
        }
        if (orientation == 1) {
            if (mPortraitCellLayoutMetrics == null) {
                int paddingLeft2 = res.getDimensionPixelSize(R.dimen.workspace_left_padding_land);
                int paddingRight2 = res.getDimensionPixelSize(R.dimen.workspace_right_padding_land);
                int paddingTop2 = res.getDimensionPixelSize(R.dimen.workspace_top_padding_land);
                int paddingBottom2 = res.getDimensionPixelSize(R.dimen.workspace_bottom_padding_land);
                int width2 = (smallestSize.x - paddingLeft2) - paddingRight2;
                int height2 = (largestSize.y - paddingTop2) - paddingBottom2;
                mPortraitCellLayoutMetrics = new Rect();
                CellLayout.getMetrics(mPortraitCellLayoutMetrics, res, width2, height2, LauncherModel.getCellCountX(), LauncherModel.getCellCountY(), orientation);
            }
            return mPortraitCellLayoutMetrics;
        }
        return null;
    }

    @Override
    public void onDragExit(DropTarget.DragObject d) {
        this.mDragEnforcer.onDragExit();
        if (this.mInScrollArea) {
            if (isPageMoving()) {
                this.mDropToLayout = (CellLayout) getPageAt(getNextPage());
            } else {
                this.mDropToLayout = this.mDragOverlappingLayout;
            }
        } else {
            this.mDropToLayout = this.mDragTargetLayout;
        }
        if (this.mDragMode == 1) {
            this.mCreateUserFolderOnDrop = true;
        } else if (this.mDragMode == 2) {
            this.mAddToExistingFolderOnDrop = true;
        }
        onResetScrollArea();
        setCurrentDropLayout(null);
        setCurrentDragOverlappingLayout(null);
        this.mSpringLoadedDragController.cancel();
        if (!this.mIsPageMoving) {
            hideOutlines();
        }
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
        if (x != this.mDragOverX || y != this.mDragOverY) {
            this.mDragOverX = x;
            this.mDragOverY = y;
            setDragMode(0);
        }
    }

    void setDragMode(int dragMode) {
        if (dragMode != this.mDragMode) {
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
    }

    private void cleanupFolderCreation() {
        if (this.mDragFolderRingAnimator != null) {
            this.mDragFolderRingAnimator.animateToNaturalState();
        }
        this.mFolderCreationAlarm.cancelAlarm();
    }

    private void cleanupAddToFolder() {
        if (this.mDragOverFolderIcon != null) {
            this.mDragOverFolderIcon.onDragExit(null);
            this.mDragOverFolderIcon = null;
        }
    }

    private void cleanupReorder(boolean cancelAlarm) {
        if (cancelAlarm) {
            this.mReorderAlarm.cancelAlarm();
        }
        this.mLastReorderX = -1;
        this.mLastReorderY = -1;
    }

    @Override
    public DropTarget getDropTargetDelegate(DropTarget.DragObject d) {
        return null;
    }

    void mapPointFromSelfToChild(View v, float[] xy) {
        mapPointFromSelfToChild(v, xy, null);
    }

    void mapPointFromSelfToChild(View v, float[] xy, Matrix cachedInverseMatrix) {
        if (cachedInverseMatrix == null) {
            v.getMatrix().invert(this.mTempInverseMatrix);
            cachedInverseMatrix = this.mTempInverseMatrix;
        }
        int scrollX = getScrollX();
        if (this.mNextPage != -1) {
            scrollX = this.mScroller.getFinalX();
        }
        xy[0] = (xy[0] + scrollX) - v.getLeft();
        xy[1] = (xy[1] + getScrollY()) - v.getTop();
        cachedInverseMatrix.mapPoints(xy);
    }

    void mapPointFromSelfToHotseatLayout(Hotseat hotseat, float[] xy) {
        hotseat.getLayout().getMatrix().invert(this.mTempInverseMatrix);
        xy[0] = (xy[0] - hotseat.getLeft()) - hotseat.getLayout().getLeft();
        xy[1] = (xy[1] - hotseat.getTop()) - hotseat.getLayout().getTop();
        this.mTempInverseMatrix.mapPoints(xy);
    }

    void mapPointFromChildToSelf(View v, float[] xy) {
        v.getMatrix().mapPoints(xy);
        int scrollX = getScrollX();
        if (this.mNextPage != -1) {
            scrollX = this.mScroller.getFinalX();
        }
        xy[0] = xy[0] - (scrollX - v.getLeft());
        xy[1] = xy[1] - (getScrollY() - v.getTop());
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
            CellLayout cl = (CellLayout) getChildAt(i);
            float[] touchXy = {originX, originY};
            cl.getMatrix().invert(this.mTempInverseMatrix);
            mapPointFromSelfToChild(cl, touchXy, this.mTempInverseMatrix);
            if (touchXy[0] < 0.0f || touchXy[0] > cl.getWidth() || touchXy[1] < 0.0f || touchXy[1] > cl.getHeight()) {
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
            } else {
                return cl;
            }
        }
        return bestMatchingScreen;
    }

    private float[] getDragViewVisualCenter(int x, int y, int xOffset, int yOffset, DragView dragView, float[] recycle) {
        float[] res;
        if (recycle == null) {
            res = new float[2];
        } else {
            res = recycle;
        }
        int left = (x + getResources().getDimensionPixelSize(R.dimen.dragViewOffsetX)) - xOffset;
        int top = (y + getResources().getDimensionPixelSize(R.dimen.dragViewOffsetY)) - yOffset;
        res[0] = (dragView.getDragRegion().width() / 2) + left;
        res[1] = (dragView.getDragRegion().height() / 2) + top;
        return res;
    }

    private boolean isDragWidget(DropTarget.DragObject d) {
        return (d.dragInfo instanceof LauncherAppWidgetInfo) || (d.dragInfo instanceof PendingAddWidgetInfo);
    }

    private boolean isExternalDragWidget(DropTarget.DragObject d) {
        return d.dragSource != this && isDragWidget(d);
    }

    @Override
    public void onDragOver(DropTarget.DragObject d) {
        if (!this.mInScrollArea && !this.mIsSwitchingState && this.mState != State.SMALL) {
            Rect r = new Rect();
            CellLayout layout = null;
            ItemInfo item = (ItemInfo) d.dragInfo;
            if (item.spanX < 0 || item.spanY < 0) {
                throw new RuntimeException("Improper spans found");
            }
            this.mDragViewVisualCenter = getDragViewVisualCenter(d.x, d.y, d.xOffset, d.yOffset, d.dragView, this.mDragViewVisualCenter);
            View child = this.mDragInfo == null ? null : this.mDragInfo.cell;
            if (isSmall()) {
                if (this.mLauncher.getHotseat() != null && !isExternalDragWidget(d)) {
                    this.mLauncher.getHotseat().getHitRect(r);
                    if (r.contains(d.x, d.y)) {
                        layout = this.mLauncher.getHotseat().getLayout();
                    }
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
                if (this.mLauncher.getHotseat() != null && !isDragWidget(d)) {
                    this.mLauncher.getHotseat().getHitRect(r);
                    if (r.contains(d.x, d.y)) {
                        layout = this.mLauncher.getHotseat().getLayout();
                    }
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
                ItemInfo info = (ItemInfo) d.dragInfo;
                this.mTargetCell = findNearestArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], item.spanX, item.spanY, this.mDragTargetLayout, this.mTargetCell);
                setCurrentDropOverCell(this.mTargetCell[0], this.mTargetCell[1]);
                float targetCellDistance = this.mDragTargetLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                View dragOverView = this.mDragTargetLayout.getChildAt(this.mTargetCell[0], this.mTargetCell[1]);
                manageFolderFeedback(info, this.mDragTargetLayout, this.mTargetCell, targetCellDistance, dragOverView);
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                boolean nearestDropOccupied = this.mDragTargetLayout.isNearestDropLocationOccupied((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], item.spanX, item.spanY, child, this.mTargetCell);
                if (!nearestDropOccupied) {
                    this.mDragTargetLayout.visualizeDropLocation(child, this.mDragOutline, (int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], this.mTargetCell[0], this.mTargetCell[1], item.spanX, item.spanY, false, d.dragView.getDragVisualizeOffset(), d.dragView.getDragRegion());
                } else if ((this.mDragMode == 0 || this.mDragMode == 3) && !this.mReorderAlarm.alarmPending() && (this.mLastReorderX != this.mTargetCell[0] || this.mLastReorderY != this.mTargetCell[1])) {
                    ReorderAlarmListener listener = new ReorderAlarmListener(this.mDragViewVisualCenter, minSpanX, minSpanY, item.spanX, item.spanY, d.dragView, child);
                    this.mReorderAlarm.setOnAlarmListener(listener);
                    this.mReorderAlarm.setAlarm(250L);
                }
                if ((this.mDragMode == 1 || this.mDragMode == 2 || !nearestDropOccupied) && this.mDragTargetLayout != null) {
                    this.mDragTargetLayout.revertTempState();
                }
            }
        }
    }

    private void manageFolderFeedback(ItemInfo info, CellLayout targetLayout, int[] targetCell, float distance, View dragOverView) {
        boolean userFolderPending = willCreateUserFolder(info, targetLayout, targetCell, distance, false);
        if (this.mDragMode == 0 && userFolderPending && !this.mFolderCreationAlarm.alarmPending()) {
            this.mFolderCreationAlarm.setOnAlarmListener(new FolderCreationAlarmListener(targetLayout, targetCell[0], targetCell[1]));
            this.mFolderCreationAlarm.setAlarm(0L);
            return;
        }
        boolean willAddToFolder = willAddToExistingUserFolder(info, targetLayout, targetCell, distance);
        if (willAddToFolder && this.mDragMode == 0) {
            this.mDragOverFolderIcon = (FolderIcon) dragOverView;
            this.mDragOverFolderIcon.onDragEnter(info);
            if (targetLayout != null) {
                targetLayout.clearDragOutlines();
            }
            setDragMode(2);
            return;
        }
        if (this.mDragMode == 2 && !willAddToFolder) {
            setDragMode(0);
        }
        if (this.mDragMode == 1 && !userFolderPending) {
            setDragMode(0);
        }
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
            if (Workspace.this.mDragFolderRingAnimator == null) {
                Workspace.this.mDragFolderRingAnimator = new FolderIcon.FolderRingAnimator(Workspace.this.mLauncher, null);
            }
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
        DragView dragView;
        float[] dragViewCenter;
        int minSpanX;
        int minSpanY;
        int spanX;
        int spanY;

        public ReorderAlarmListener(float[] dragViewCenter, int minSpanX, int minSpanY, int spanX, int spanY, DragView dragView, View child) {
            this.dragViewCenter = dragViewCenter;
            this.minSpanX = minSpanX;
            this.minSpanY = minSpanY;
            this.spanX = spanX;
            this.spanY = spanY;
            this.child = child;
            this.dragView = dragView;
        }

        @Override
        public void onAlarm(Alarm alarm) {
            int[] resultSpan = new int[2];
            Workspace.this.mTargetCell = Workspace.this.findNearestArea((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.spanX, this.spanY, Workspace.this.mDragTargetLayout, Workspace.this.mTargetCell);
            Workspace.this.mLastReorderX = Workspace.this.mTargetCell[0];
            Workspace.this.mLastReorderY = Workspace.this.mTargetCell[1];
            Workspace.this.mTargetCell = Workspace.this.mDragTargetLayout.createArea((int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], this.minSpanX, this.minSpanY, this.spanX, this.spanY, this.child, Workspace.this.mTargetCell, resultSpan, 0);
            if (Workspace.this.mTargetCell[0] < 0 || Workspace.this.mTargetCell[1] < 0) {
                Workspace.this.mDragTargetLayout.revertTempState();
            } else {
                Workspace.this.setDragMode(3);
            }
            boolean resize = (resultSpan[0] == this.spanX && resultSpan[1] == this.spanY) ? false : true;
            Workspace.this.mDragTargetLayout.visualizeDropLocation(this.child, Workspace.this.mDragOutline, (int) Workspace.this.mDragViewVisualCenter[0], (int) Workspace.this.mDragViewVisualCenter[1], Workspace.this.mTargetCell[0], Workspace.this.mTargetCell[1], resultSpan[0], resultSpan[1], resize, this.dragView.getDragVisualizeOffset(), this.dragView.getDragRegion());
        }
    }

    @Override
    public void getHitRect(Rect outRect) {
        outRect.set(0, 0, this.mDisplaySize.x, this.mDisplaySize.y);
    }

    private void onDropExternal(int[] touchXY, Object dragInfo, CellLayout cellLayout, boolean insertAtFirst, DropTarget.DragObject d) {
        View view;
        Runnable exitSpringLoadedRunnable = new Runnable() {
            @Override
            public void run() {
                Workspace.this.mLauncher.exitSpringLoadedDragModeDelayed(true, false, null);
            }
        };
        ItemInfo info = (ItemInfo) dragInfo;
        int spanX = info.spanX;
        int spanY = info.spanY;
        if (this.mDragInfo != null) {
            spanX = this.mDragInfo.spanX;
            spanY = this.mDragInfo.spanY;
        }
        final long container = this.mLauncher.isHotseatLayout(cellLayout) ? -101L : -100L;
        final int screen = indexOfChild(cellLayout);
        if (!this.mLauncher.isHotseatLayout(cellLayout) && screen != this.mCurrentPage && this.mState != State.SPRING_LOADED) {
            snapToPage(screen);
        }
        if (info instanceof PendingAddItemInfo) {
            final PendingAddItemInfo pendingInfo = (PendingAddItemInfo) dragInfo;
            boolean findNearestVacantCell = true;
            if (pendingInfo.itemType == 1) {
                this.mTargetCell = findNearestArea(touchXY[0], touchXY[1], spanX, spanY, cellLayout, this.mTargetCell);
                float distance = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
                if (willCreateUserFolder((ItemInfo) d.dragInfo, cellLayout, this.mTargetCell, distance, true) || willAddToExistingUserFolder((ItemInfo) d.dragInfo, cellLayout, this.mTargetCell, distance)) {
                    findNearestVacantCell = false;
                }
            }
            final ItemInfo item = (ItemInfo) d.dragInfo;
            boolean updateWidgetSize = false;
            if (findNearestVacantCell) {
                int minSpanX = item.spanX;
                int minSpanY = item.spanY;
                if (item.minSpanX > 0 && item.minSpanY > 0) {
                    minSpanX = item.minSpanX;
                    minSpanY = item.minSpanY;
                }
                int[] resultSpan = new int[2];
                this.mTargetCell = cellLayout.createArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], minSpanX, minSpanY, info.spanX, info.spanY, null, this.mTargetCell, resultSpan, 2);
                if (resultSpan[0] != item.spanX || resultSpan[1] != item.spanY) {
                    updateWidgetSize = true;
                }
                item.spanX = resultSpan[0];
                item.spanY = resultSpan[1];
            }
            Runnable onAnimationCompleteRunnable = new Runnable() {
                @Override
                public void run() {
                    switch (pendingInfo.itemType) {
                        case 1:
                            Workspace.this.mLauncher.processShortcutFromDrop(pendingInfo.componentName, container, screen, Workspace.this.mTargetCell, null);
                            return;
                        case 2:
                        case 3:
                        default:
                            throw new IllegalStateException("Unknown item type: " + pendingInfo.itemType);
                        case 4:
                            int[] span = {item.spanX, item.spanY};
                            Workspace.this.mLauncher.addAppWidgetFromDrop((PendingAddWidgetInfo) pendingInfo, container, screen, Workspace.this.mTargetCell, span, null);
                            return;
                    }
                }
            };
            View finalView = pendingInfo.itemType == 4 ? ((PendingAddWidgetInfo) pendingInfo).boundWidget : null;
            if ((finalView instanceof AppWidgetHostView) && updateWidgetSize) {
                AppWidgetHostView awhv = (AppWidgetHostView) finalView;
                AppWidgetResizeFrame.updateWidgetSizeRanges(awhv, this.mLauncher, item.spanX, item.spanY);
            }
            int animationStyle = 0;
            if (pendingInfo.itemType == 4 && ((PendingAddWidgetInfo) pendingInfo).info.configure != null) {
                animationStyle = 1;
            }
            animateWidgetDrop(info, cellLayout, d.dragView, onAnimationCompleteRunnable, animationStyle, finalView, true);
            return;
        }
        switch (info.itemType) {
            case 0:
            case 1:
                if (info.container == -1 && (info instanceof ApplicationInfo)) {
                    info = new ShortcutInfo((ApplicationInfo) info);
                }
                view = this.mLauncher.createShortcut(R.layout.application, cellLayout, (ShortcutInfo) info);
                break;
            case 2:
                view = FolderIcon.fromXml(R.layout.folder_icon, this.mLauncher, cellLayout, (FolderInfo) info, this.mIconCache);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }
        if (touchXY != null) {
            this.mTargetCell = findNearestArea(touchXY[0], touchXY[1], spanX, spanY, cellLayout, this.mTargetCell);
            float distance2 = cellLayout.getDistanceFromCell(this.mDragViewVisualCenter[0], this.mDragViewVisualCenter[1], this.mTargetCell);
            d.postAnimationRunnable = exitSpringLoadedRunnable;
            if (createUserFolderIfNecessary(view, container, cellLayout, this.mTargetCell, distance2, true, d.dragView, d.postAnimationRunnable) || addToExistingFolderIfNecessary(view, cellLayout, this.mTargetCell, distance2, d, true)) {
                return;
            }
        }
        if (touchXY != null) {
            this.mTargetCell = cellLayout.createArea((int) this.mDragViewVisualCenter[0], (int) this.mDragViewVisualCenter[1], 1, 1, 1, 1, null, this.mTargetCell, null, 2);
        } else {
            cellLayout.findCellForSpan(this.mTargetCell, 1, 1);
        }
        addInScreen(view, container, screen, this.mTargetCell[0], this.mTargetCell[1], info.spanX, info.spanY, insertAtFirst);
        cellLayout.onDropChild(view);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        cellLayout.getShortcutsAndWidgets().measureChild(view);
        LauncherModel.addOrMoveItemInDatabase(this.mLauncher, info, container, screen, lp.cellX, lp.cellY);
        if (d.dragView != null) {
            setFinalTransitionTransform(cellLayout);
            this.mLauncher.getDragLayer().animateViewIntoPosition(d.dragView, view, exitSpringLoadedRunnable);
            resetTransitionTransform(cellLayout);
        }
    }

    public Bitmap createWidgetBitmap(ItemInfo widgetInfo, View layout) {
        int[] unScaledSize = this.mLauncher.getWorkspace().estimateItemSize(widgetInfo.spanX, widgetInfo.spanY, widgetInfo, false);
        int visibility = layout.getVisibility();
        layout.setVisibility(0);
        int width = View.MeasureSpec.makeMeasureSpec(unScaledSize[0], 1073741824);
        int height = View.MeasureSpec.makeMeasureSpec(unScaledSize[1], 1073741824);
        Bitmap b = Bitmap.createBitmap(unScaledSize[0], unScaledSize[1], Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        layout.measure(width, height);
        layout.layout(0, 0, unScaledSize[0], unScaledSize[1]);
        layout.draw(c);
        c.setBitmap(null);
        layout.setVisibility(visibility);
        return b;
    }

    private void getFinalPositionForDropAnimation(int[] loc, float[] scaleXY, DragView dragView, CellLayout layout, ItemInfo info, int[] targetCell, boolean external, boolean scale) {
        float dragViewScaleX;
        float dragViewScaleY;
        int spanX = info.spanX;
        int spanY = info.spanY;
        Rect r = estimateItemPosition(layout, info, targetCell[0], targetCell[1], spanX, spanY);
        loc[0] = r.left;
        loc[1] = r.top;
        setFinalTransitionTransform(layout);
        float cellLayoutScale = this.mLauncher.getDragLayer().getDescendantCoordRelativeToSelf(layout, loc);
        resetTransitionTransform(layout);
        if (scale) {
            dragViewScaleX = (1.0f * r.width()) / dragView.getMeasuredWidth();
            dragViewScaleY = (1.0f * r.height()) / dragView.getMeasuredHeight();
        } else {
            dragViewScaleX = 1.0f;
            dragViewScaleY = 1.0f;
        }
        loc[0] = (int) (loc[0] - ((dragView.getMeasuredWidth() - (r.width() * cellLayoutScale)) / 2.0f));
        loc[1] = (int) (loc[1] - ((dragView.getMeasuredHeight() - (r.height() * cellLayoutScale)) / 2.0f));
        scaleXY[0] = dragViewScaleX * cellLayoutScale;
        scaleXY[1] = dragViewScaleY * cellLayoutScale;
    }

    public void animateWidgetDrop(ItemInfo info, CellLayout cellLayout, DragView dragView, final Runnable onCompleteRunnable, int animationType, final View finalView, boolean external) {
        int endStyle;
        Rect from = new Rect();
        this.mLauncher.getDragLayer().getViewRectRelativeToSelf(dragView, from);
        int[] finalPos = new int[2];
        float[] scaleXY = new float[2];
        boolean scalePreview = !(info instanceof PendingAddShortcutInfo);
        getFinalPositionForDropAnimation(finalPos, scaleXY, dragView, cellLayout, info, this.mTargetCell, external, scalePreview);
        Resources res = this.mLauncher.getResources();
        int duration = res.getInteger(R.integer.config_dropAnimMaxDuration) - 200;
        if ((finalView instanceof AppWidgetHostView) && external) {
            Log.d("Launcher.Workspace", "6557954 Animate widget drop, final view is appWidgetHostView");
            this.mLauncher.getDragLayer().removeView(finalView);
        }
        if ((animationType == 2 || external) && finalView != null) {
            Bitmap crossFadeBitmap = createWidgetBitmap(info, finalView);
            dragView.setCrossFadeBitmap(crossFadeBitmap);
            dragView.crossFade((int) (duration * 0.8f));
        } else if (info.itemType == 4 && external) {
            float fMin = Math.min(scaleXY[0], scaleXY[1]);
            scaleXY[1] = fMin;
            scaleXY[0] = fMin;
        }
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        if (animationType == 4) {
            this.mLauncher.getDragLayer().animateViewIntoPosition(dragView, finalPos, 0.0f, 0.1f, 0.1f, 0, onCompleteRunnable, duration);
            return;
        }
        if (animationType == 1) {
            endStyle = 2;
        } else {
            endStyle = 0;
        }
        Runnable onComplete = new Runnable() {
            @Override
            public void run() {
                if (finalView != null) {
                    finalView.setVisibility(0);
                }
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        };
        dragLayer.animateViewIntoPosition(dragView, from.left, from.top, finalPos[0], finalPos[1], 1.0f, 1.0f, 1.0f, scaleXY[0], scaleXY[1], onComplete, endStyle, duration, this);
    }

    public void setFinalTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            int index = indexOfChild(layout);
            this.mCurrentScaleX = layout.getScaleX();
            this.mCurrentScaleY = layout.getScaleY();
            this.mCurrentTranslationX = layout.getTranslationX();
            this.mCurrentTranslationY = layout.getTranslationY();
            this.mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(this.mNewScaleXs[index]);
            layout.setScaleY(this.mNewScaleYs[index]);
            layout.setTranslationX(this.mNewTranslationXs[index]);
            layout.setTranslationY(this.mNewTranslationYs[index]);
            layout.setRotationY(this.mNewRotationYs[index]);
        }
    }

    public void resetTransitionTransform(CellLayout layout) {
        if (isSwitchingState()) {
            this.mCurrentScaleX = layout.getScaleX();
            this.mCurrentScaleY = layout.getScaleY();
            this.mCurrentTranslationX = layout.getTranslationX();
            this.mCurrentTranslationY = layout.getTranslationY();
            this.mCurrentRotationY = layout.getRotationY();
            layout.setScaleX(this.mCurrentScaleX);
            layout.setScaleY(this.mCurrentScaleY);
            layout.setTranslationX(this.mCurrentTranslationX);
            layout.setTranslationY(this.mCurrentTranslationY);
            layout.setRotationY(this.mCurrentRotationY);
        }
    }

    public CellLayout getCurrentDropLayout() {
        return (CellLayout) getChildAt(getNextPage());
    }

    public int[] findNearestArea(int pixelX, int pixelY, int spanX, int spanY, CellLayout layout, int[] recycle) {
        return layout.findNearestArea(pixelX, pixelY, spanX, spanY, recycle);
    }

    void setup(DragController dragController) {
        this.mSpringLoadedDragController = new SpringLoadedDragController(this.mLauncher);
        this.mDragController = dragController;
        updateChildrenLayersEnabled(false);
        setWallpaperDimension();
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete, boolean success) {
        CellLayout cellLayout;
        if (success) {
            if (target != this && this.mDragInfo != null) {
                getParentCellLayoutForView(this.mDragInfo.cell).removeView(this.mDragInfo.cell);
                if (this.mDragInfo.cell instanceof DropTarget) {
                    this.mDragController.removeDropTarget((DropTarget) this.mDragInfo.cell);
                }
            }
        } else if (this.mDragInfo != null) {
            if (this.mLauncher.isHotseatLayout(target)) {
                cellLayout = this.mLauncher.getHotseat().getLayout();
            } else {
                cellLayout = (CellLayout) getChildAt(this.mDragInfo.screen);
            }
            cellLayout.onDropChild(this.mDragInfo.cell);
        }
        if (d.cancelled && this.mDragInfo.cell != null) {
            this.mDragInfo.cell.setVisibility(0);
        }
        this.mDragOutline = null;
        this.mDragInfo = null;
        hideScrollingIndicator(false);
    }

    void updateItemLocationsInDatabase(CellLayout cl) {
        int count = cl.getShortcutsAndWidgets().getChildCount();
        int screen = indexOfChild(cl);
        int container = -100;
        if (this.mLauncher.isHotseatLayout(cl)) {
            screen = -1;
            container = -101;
        }
        for (int i = 0; i < count; i++) {
            View v = cl.getShortcutsAndWidgets().getChildAt(i);
            ItemInfo info = (ItemInfo) v.getTag();
            if (info != null && info.requiresDbUpdate) {
                info.requiresDbUpdate = false;
                LauncherModel.modifyItemInDatabase(this.mLauncher, info, container, screen, info.cellX, info.cellY, info.spanX, info.spanY);
            }
        }
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public void onFlingToDelete(DropTarget.DragObject d, int x, int y, PointF vec) {
    }

    @Override
    public void onFlingToDeleteCompleted() {
    }

    @Override
    public boolean isDropEnabled() {
        return true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        Launcher.setScreen(this.mCurrentPage);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        this.mSavedStates = container;
    }

    public void restoreInstanceStateForChild(int child) {
        if (this.mSavedStates != null) {
            this.mRestoredPages.add(Integer.valueOf(child));
            CellLayout cl = (CellLayout) getChildAt(child);
            cl.restoreInstanceState(this.mSavedStates);
        }
    }

    public void restoreInstanceStateForRemainingPages() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (!this.mRestoredPages.contains(Integer.valueOf(i))) {
                restoreInstanceStateForChild(i);
            }
        }
        this.mRestoredPages.clear();
    }

    @Override
    public void scrollLeft() {
        if (!isSmall() && !this.mIsSwitchingState) {
            super.scrollLeft();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public void scrollRight() {
        if (!isSmall() && !this.mIsSwitchingState) {
            super.scrollRight();
        }
        Folder openFolder = getOpenFolder();
        if (openFolder != null) {
            openFolder.completeDragExit();
        }
    }

    @Override
    public boolean onEnterScrollArea(int x, int y, int direction) {
        boolean isPortrait = !LauncherApplication.isScreenLandscape(getContext());
        if (this.mLauncher.getHotseat() != null && isPortrait) {
            Rect r = new Rect();
            this.mLauncher.getHotseat().getHitRect(r);
            if (r.contains(x, y)) {
                return false;
            }
        }
        boolean result = false;
        if (!isSmall() && !this.mIsSwitchingState) {
            this.mInScrollArea = true;
            int page = getNextPage() + (direction == 0 ? -1 : 1);
            setCurrentDropLayout(null);
            if (page >= 0 && page < getChildCount()) {
                CellLayout layout = (CellLayout) getChildAt(page);
                setCurrentDragOverlappingLayout(layout);
                invalidate();
                result = true;
            }
        }
        return result;
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

    public Folder getFolderForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child instanceof Folder) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag && f.getInfo().opened) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            int count = layout.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = layout.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    void clearDropTargets() {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                KeyEvent.Callback childAt = layout.getChildAt(j);
                if (childAt instanceof DropTarget) {
                    this.mDragController.removeDropTarget((DropTarget) childAt);
                }
            }
        }
    }

    void removeItemsByPackageName(ArrayList<String> packages, UserHandle user) {
        LauncherAppWidgetInfo info;
        ComponentName cn;
        HashSet<String> packageNames = new HashSet<>();
        packageNames.addAll(packages);
        HashSet<ComponentName> cns = new HashSet<>();
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (CellLayout layoutParent : cellLayouts) {
            ViewGroup layout = layoutParent.getShortcutsAndWidgets();
            int childCount = layout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = layout.getChildAt(i);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info2 = (ShortcutInfo) tag;
                    ComponentName cn2 = info2.intent.getComponent();
                    if (cn2 != null && packageNames.contains(cn2.getPackageName()) && info2.user.equals(user)) {
                        cns.add(cn2);
                    }
                } else if (tag instanceof FolderInfo) {
                    FolderInfo info3 = (FolderInfo) tag;
                    for (ShortcutInfo s : info3.contents) {
                        ComponentName cn3 = s.intent.getComponent();
                        if (cn3 != null && packageNames.contains(cn3.getPackageName()) && info3.user.equals(user)) {
                            cns.add(cn3);
                        }
                    }
                } else if ((tag instanceof LauncherAppWidgetInfo) && (cn = (info = (LauncherAppWidgetInfo) tag).providerName) != null && packageNames.contains(cn.getPackageName()) && info.user.equals(user)) {
                    cns.add(cn);
                }
            }
        }
        removeItemsByComponentName(cns, user);
    }

    void removeItemsByApplicationInfo(ArrayList<ApplicationInfo> appInfos, UserHandle user) {
        HashSet<ComponentName> cns = new HashSet<>();
        for (ApplicationInfo info : appInfos) {
            cns.add(info.componentName);
        }
        removeItemsByComponentName(cns, user);
    }

    void removeItemsByComponentName(final HashSet<ComponentName> componentNames, final UserHandle user) {
        ArrayList<CellLayout> cellLayouts = getWorkspaceAndHotseatCellLayouts();
        for (final CellLayout layoutParent : cellLayouts) {
            final ViewGroup layout = layoutParent.getShortcutsAndWidgets();
            post(new Runnable() {
                @Override
                public void run() {
                    LauncherAppWidgetInfo info;
                    ComponentName provider;
                    ArrayList<View> childrenToRemove = new ArrayList<>();
                    childrenToRemove.clear();
                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        View view = layout.getChildAt(j);
                        Object tag = view.getTag();
                        if ((!(tag instanceof ShortcutInfo) && !(tag instanceof LauncherAppWidgetInfo)) || ((ItemInfo) tag).user.equals(user)) {
                            if (tag instanceof ShortcutInfo) {
                                ShortcutInfo info2 = (ShortcutInfo) tag;
                                Intent intent = info2.intent;
                                ComponentName name = intent.getComponent();
                                if (name != null && componentNames.contains(name)) {
                                    LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, info2);
                                    childrenToRemove.add(view);
                                }
                            } else if (tag instanceof FolderInfo) {
                                FolderInfo info3 = (FolderInfo) tag;
                                ArrayList<ShortcutInfo> contents = info3.contents;
                                int contentsCount = contents.size();
                                ArrayList<ShortcutInfo> appsToRemoveFromFolder = new ArrayList<>();
                                for (int k = 0; k < contentsCount; k++) {
                                    ShortcutInfo appInfo = contents.get(k);
                                    Intent intent2 = appInfo.intent;
                                    ComponentName name2 = intent2.getComponent();
                                    if (name2 != null && componentNames.contains(name2) && user.equals(appInfo.user)) {
                                        appsToRemoveFromFolder.add(appInfo);
                                    }
                                }
                                for (ShortcutInfo item : appsToRemoveFromFolder) {
                                    info3.remove(item);
                                    LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, item);
                                }
                            } else if ((tag instanceof LauncherAppWidgetInfo) && (provider = (info = (LauncherAppWidgetInfo) tag).providerName) != null && componentNames.contains(provider)) {
                                LauncherModel.deleteItemFromDatabase(Workspace.this.mLauncher, info);
                                childrenToRemove.add(view);
                            }
                        }
                    }
                    int childCount2 = childrenToRemove.size();
                    for (int j2 = 0; j2 < childCount2; j2++) {
                        View view2 = childrenToRemove.get(j2);
                        layoutParent.removeViewInLayout(view2);
                        if (view2 instanceof DropTarget) {
                            Workspace.this.mDragController.removeDropTarget((DropTarget) view2);
                        }
                    }
                    if (childCount2 > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            });
        }
        final Context context = getContext();
        post(new Runnable() {
            @Override
            public void run() {
                String spKey = LauncherApplication.getSharedPreferencesKey();
                SharedPreferences sp = context.getSharedPreferences(spKey, 0);
                Set<String> newApps = sp.getStringSet("apps.new.list", null);
                if (newApps != null) {
                    synchronized (newApps) {
                        Iterator<String> iter = newApps.iterator();
                        while (iter.hasNext()) {
                            try {
                                Intent intent = Intent.parseUri(iter.next(), 0);
                                if (componentNames.contains(intent.getComponent())) {
                                    iter.remove();
                                }
                                ArrayList<ItemInfo> shortcuts = LauncherModel.getWorkspaceShortcutItemInfosWithIntent(intent);
                                for (ItemInfo info : shortcuts) {
                                    LauncherModel.deleteItemFromDatabase(context, info);
                                }
                            } catch (URISyntaxException e) {
                            }
                        }
                    }
                }
            }
        });
    }

    void updateShortcuts(ArrayList<ApplicationInfo> apps) {
        ArrayList<ShortcutAndWidgetContainer> childrenLayouts = getAllShortcutAndWidgetContainers();
        for (ShortcutAndWidgetContainer layout : childrenLayouts) {
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    Intent intent = info.intent;
                    ComponentName name = intent.getComponent();
                    if (info.itemType == 0 && "android.intent.action.MAIN".equals(intent.getAction()) && name != null) {
                        int appCount = apps.size();
                        for (int k = 0; k < appCount; k++) {
                            ApplicationInfo app = apps.get(k);
                            if (app.componentName.equals(name)) {
                                BubbleTextView shortcut = (BubbleTextView) view;
                                info.updateIcon(this.mIconCache);
                                info.title = app.title.toString();
                                shortcut.applyFromShortcutInfo(info, this.mIconCache);
                            }
                        }
                    }
                }
            }
        }
    }

    void moveToDefaultScreen(boolean animate) {
        if (!isSmall()) {
            if (animate) {
                snapToPage(this.mDefaultPage);
            } else {
                setCurrentPage(this.mDefaultPage);
            }
        }
        getChildAt(this.mDefaultPage).requestFocus();
    }

    @Override
    public void syncPages() {
    }

    @Override
    public void syncPageItems(int page, boolean immediate) {
    }

    @Override
    protected String getCurrentPageDescription() {
        int page = this.mNextPage != -1 ? this.mNextPage : this.mCurrentPage;
        return String.format(getContext().getString(R.string.workspace_scroll_format), Integer.valueOf(page + 1), Integer.valueOf(getChildCount()));
    }

    @Override
    public void getLocationInDragLayer(int[] loc) {
        this.mLauncher.getDragLayer().getLocationInDragLayer(this, loc);
    }

    void setFadeForOverScroll(float fade) {
        if (isScrollingIndicatorEnabled()) {
            this.mOverscrollFade = fade;
            float reducedFade = 0.5f + ((1.0f - fade) * 0.5f);
            ViewGroup parent = (ViewGroup) getParent();
            ImageView qsbDivider = (ImageView) parent.findViewById(R.id.qsb_divider);
            ImageView dockDivider = (ImageView) parent.findViewById(R.id.dock_divider);
            View scrollIndicator = getScrollingIndicator();
            cancelScrollingIndicatorAnimations();
            if (qsbDivider != null) {
                qsbDivider.setAlpha(reducedFade);
            }
            if (dockDivider != null) {
                dockDivider.setAlpha(reducedFade);
            }
            scrollIndicator.setAlpha(1.0f - fade);
        }
    }
}
