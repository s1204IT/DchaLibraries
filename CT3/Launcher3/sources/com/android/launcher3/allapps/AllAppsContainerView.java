package com.android.launcher3.allapps;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import com.android.launcher3.AppInfo;
import com.android.launcher3.BaseContainerView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeleteDropTarget;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Folder;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherTransitionable;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.allapps.AllAppsSearchBarController;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import com.android.launcher3.allapps.HeaderElevationController;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.util.ComponentKey;
import java.util.ArrayList;
import java.util.List;

public class AllAppsContainerView extends BaseContainerView implements DragSource, LauncherTransitionable, View.OnTouchListener, View.OnLongClickListener, AllAppsSearchBarController.Callbacks {
    private final AllAppsGridAdapter mAdapter;
    private final AlphabeticalAppsList mApps;
    private AllAppsRecyclerView mAppsRecyclerView;
    private final Point mBoundsCheckLastTouchDownPos;
    private final Rect mContentBounds;
    private HeaderElevationController mElevationController;
    private final Point mIconLastTouchPos;
    private final RecyclerView.ItemDecoration mItemDecoration;
    private final Launcher mLauncher;
    private final RecyclerView.LayoutManager mLayoutManager;
    private int mNumAppsPerRow;
    private int mNumPredictedAppsPerRow;
    private int mRecyclerViewTopBottomPadding;
    private AllAppsSearchBarController mSearchBarController;
    private View mSearchContainer;
    private ExtendedEditText mSearchInput;
    private SpannableStringBuilder mSearchQueryBuilder;
    private int mSectionNamesMargin;

    public AllAppsContainerView(Context context) {
        this(context, null);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AllAppsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContentBounds = new Rect();
        this.mSearchQueryBuilder = null;
        this.mBoundsCheckLastTouchDownPos = new Point(-1, -1);
        this.mIconLastTouchPos = new Point();
        Resources res = context.getResources();
        this.mLauncher = (Launcher) context;
        this.mSectionNamesMargin = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        this.mApps = new AlphabeticalAppsList(context);
        this.mAdapter = new AllAppsGridAdapter(this.mLauncher, this.mApps, this, this.mLauncher, this);
        this.mApps.setAdapter(this.mAdapter);
        this.mLayoutManager = this.mAdapter.getLayoutManager();
        this.mItemDecoration = this.mAdapter.getItemDecoration();
        this.mRecyclerViewTopBottomPadding = res.getDimensionPixelSize(R.dimen.all_apps_list_top_bottom_padding);
        this.mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
    }

    public void setPredictedApps(List<ComponentKey> apps) {
        this.mApps.setPredictedApps(apps);
    }

    public void setApps(List<AppInfo> apps) {
        this.mApps.setApps(apps);
    }

    public void addApps(List<AppInfo> apps) {
        this.mApps.addApps(apps);
    }

    public void updateApps(List<AppInfo> apps) {
        this.mApps.updateApps(apps);
    }

    public void removeApps(List<AppInfo> apps) {
        this.mApps.removeApps(apps);
    }

    public void setSearchBarController(AllAppsSearchBarController searchController) {
        if (this.mSearchBarController != null) {
            throw new RuntimeException("Expected search bar controller to only be set once");
        }
        this.mSearchBarController = searchController;
        this.mSearchBarController.initialize(this.mApps, this.mSearchInput, this.mLauncher, this);
        this.mAdapter.setSearchController(this.mSearchBarController);
        updateBackgroundAndPaddings();
    }

    public void scrollToTop() {
        this.mAppsRecyclerView.scrollToTop();
    }

    public void startAppsSearch() {
        if (this.mSearchBarController == null) {
            return;
        }
        this.mSearchBarController.focusSearchField();
    }

    public void reset() {
        this.mSearchBarController.reset();
        this.mAppsRecyclerView.reset();
    }

    @Override
    protected void onFinishInflate() {
        HeaderElevationController controllerV16;
        super.onFinishInflate();
        getContentView().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    return;
                }
                AllAppsContainerView.this.mAppsRecyclerView.requestFocus();
            }
        });
        this.mSearchContainer = findViewById(R.id.search_container);
        this.mSearchInput = (ExtendedEditText) findViewById(R.id.search_box_input);
        if (Utilities.ATLEAST_LOLLIPOP) {
            controllerV16 = new HeaderElevationController.ControllerVL(this.mSearchContainer);
        } else {
            controllerV16 = new HeaderElevationController.ControllerV16(this.mSearchContainer);
        }
        this.mElevationController = controllerV16;
        this.mAppsRecyclerView = (AllAppsRecyclerView) findViewById(R.id.apps_list_view);
        this.mAppsRecyclerView.setApps(this.mApps);
        this.mAppsRecyclerView.setLayoutManager(this.mLayoutManager);
        this.mAppsRecyclerView.setAdapter(this.mAdapter);
        this.mAppsRecyclerView.setHasFixedSize(true);
        this.mAppsRecyclerView.addOnScrollListener(this.mElevationController);
        this.mAppsRecyclerView.setElevationController(this.mElevationController);
        if (this.mItemDecoration != null) {
            this.mAppsRecyclerView.addItemDecoration(this.mItemDecoration);
        }
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().widthPixels, Integer.MIN_VALUE);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(getResources().getDisplayMetrics().heightPixels, Integer.MIN_VALUE);
        BubbleTextView icon = (BubbleTextView) layoutInflater.inflate(R.layout.all_apps_icon, (ViewGroup) this, false);
        icon.applyDummyInfo();
        icon.measure(widthMeasureSpec, heightMeasureSpec);
        BubbleTextView predIcon = (BubbleTextView) layoutInflater.inflate(R.layout.all_apps_prediction_bar_icon, (ViewGroup) this, false);
        predIcon.applyDummyInfo();
        predIcon.measure(widthMeasureSpec, heightMeasureSpec);
        this.mAppsRecyclerView.setPremeasuredIconHeights(predIcon.getMeasuredHeight(), icon.getMeasuredHeight());
        updateBackgroundAndPaddings();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        AlphabeticalAppsList.MergeAlgorithm mergeAlgorithm;
        this.mContentBounds.set(this.mContentPadding.left, this.mContentPadding.top, View.MeasureSpec.getSize(widthMeasureSpec) - this.mContentPadding.right, View.MeasureSpec.getSize(heightMeasureSpec) - this.mContentPadding.bottom);
        int availableWidth = (!this.mContentBounds.isEmpty() ? this.mContentBounds.width() : View.MeasureSpec.getSize(widthMeasureSpec)) - (this.mAppsRecyclerView.getMaxScrollbarWidth() * 2);
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        grid.updateAppsViewNumCols(getResources(), availableWidth);
        if (this.mNumAppsPerRow != grid.allAppsNumCols || this.mNumPredictedAppsPerRow != grid.allAppsNumPredictiveCols) {
            this.mNumAppsPerRow = grid.allAppsNumCols;
            this.mNumPredictedAppsPerRow = grid.allAppsNumPredictiveCols;
            boolean mergeSectionsFully = this.mSectionNamesMargin == 0 || !grid.isPhone;
            if (mergeSectionsFully) {
                mergeAlgorithm = new FullMergeAlgorithm();
            } else {
                mergeAlgorithm = new SimpleSectionMergeAlgorithm((int) Math.ceil(this.mNumAppsPerRow / 2.0f), 3, 2);
            }
            if (this.mNumAppsPerRow > 0) {
                this.mAppsRecyclerView.setNumAppsPerRow(grid, this.mNumAppsPerRow);
                this.mAdapter.setNumAppsPerRow(this.mNumAppsPerRow);
                this.mApps.setNumAppsPerRow(this.mNumAppsPerRow, this.mNumPredictedAppsPerRow, mergeAlgorithm);
                int iconSize = availableWidth / this.mNumAppsPerRow;
                int iconSpacing = (iconSize - grid.allAppsIconSizePx) / 2;
                this.mSearchInput.setPaddingRelative(iconSpacing, 0, iconSpacing, 0);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onUpdateBgPadding(Rect padding, Rect bgPadding) {
        this.mAppsRecyclerView.updateBackgroundPadding(bgPadding);
        this.mAdapter.updateBackgroundPadding(bgPadding);
        this.mElevationController.updateBackgroundPadding(bgPadding);
        int maxScrollBarWidth = this.mAppsRecyclerView.getMaxScrollbarWidth();
        int startInset = Math.max(this.mSectionNamesMargin, maxScrollBarWidth);
        int topBottomPadding = this.mRecyclerViewTopBottomPadding;
        if (Utilities.isRtl(getResources())) {
            this.mAppsRecyclerView.setPadding(padding.left + maxScrollBarWidth, topBottomPadding, padding.right + startInset, topBottomPadding);
        } else {
            this.mAppsRecyclerView.setPadding(padding.left + startInset, topBottomPadding, padding.right + maxScrollBarWidth, topBottomPadding);
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) this.mSearchContainer.getLayoutParams();
        lp.leftMargin = padding.left;
        lp.rightMargin = padding.right;
        this.mSearchContainer.setLayoutParams(lp);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean isKeyNotWhitespace;
        if (!this.mSearchBarController.isSearchFieldFocused() && event.getAction() == 0) {
            int unicodeChar = event.getUnicodeChar();
            if (unicodeChar <= 0 || Character.isWhitespace(unicodeChar)) {
                isKeyNotWhitespace = false;
            } else {
                isKeyNotWhitespace = Character.isSpaceChar(unicodeChar) ? false : true;
            }
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, this.mSearchQueryBuilder, event.getKeyCode(), event);
                if (gotKey && this.mSearchQueryBuilder.length() > 0) {
                    this.mSearchBarController.focusSearchField();
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouch(View v, MotionEvent ev) {
        switch (ev.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
            case PackageInstallerCompat.STATUS_FAILED:
                this.mIconLastTouchPos.set((int) ev.getX(), (int) ev.getY());
                break;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        if (!v.isInTouchMode() || !this.mLauncher.isAppsViewVisible() || this.mLauncher.getWorkspace().isSwitchingState() || !this.mLauncher.isDraggingEnabled()) {
            return false;
        }
        this.mLauncher.getWorkspace().beginDragShared(v, this.mIconLastTouchPos, this, false);
        this.mLauncher.enterSpringLoadedDragMode();
        return false;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return true;
    }

    @Override
    public boolean supportsAppInfoDropTarget() {
        return true;
    }

    @Override
    public boolean supportsDeleteDropTarget() {
        return false;
    }

    @Override
    public float getIntrinsicIconScaleFactor() {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        return grid.allAppsIconSizePx / grid.iconSizePx;
    }

    @Override
    public void onFlingToDeleteCompleted() {
        this.mLauncher.exitSpringLoadedDragModeDelayed(true, 300, null);
        this.mLauncher.unlockScreenOrientation(false);
    }

    @Override
    public void onDropCompleted(View target, DropTarget.DragObject d, boolean isFlingToDelete, boolean success) {
        if (isFlingToDelete || !success || (target != this.mLauncher.getWorkspace() && !(target instanceof DeleteDropTarget) && !(target instanceof Folder))) {
            this.mLauncher.exitSpringLoadedDragModeDelayed(true, 300, null);
        }
        this.mLauncher.unlockScreenOrientation(false);
        if (success) {
            return;
        }
        boolean showOutOfSpaceMessage = false;
        if (target instanceof Workspace) {
            int currentScreen = this.mLauncher.getCurrentWorkspaceScreen();
            Workspace workspace = (Workspace) target;
            CellLayout layout = (CellLayout) workspace.getChildAt(currentScreen);
            ItemInfo itemInfo = (ItemInfo) d.dragInfo;
            if (layout != null) {
                showOutOfSpaceMessage = !layout.findCellForSpan(null, itemInfo.spanX, itemInfo.spanY);
            }
        }
        if (showOutOfSpaceMessage) {
            this.mLauncher.showOutOfSpaceMessage(false);
        }
        d.deferDragViewCleanupPostAnimation = false;
    }

    @Override
    public void onLauncherTransitionPrepare(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStart(Launcher l, boolean animated, boolean toWorkspace) {
    }

    @Override
    public void onLauncherTransitionStep(Launcher l, float t) {
    }

    @Override
    public void onLauncherTransitionEnd(Launcher l, boolean animated, boolean toWorkspace) {
        if (!toWorkspace) {
            return;
        }
        reset();
    }

    private boolean handleTouchEvent(MotionEvent ev) {
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                if (this.mContentBounds.isEmpty()) {
                    if (ev.getX() < getPaddingLeft() || ev.getX() > getWidth() - getPaddingRight()) {
                        this.mBoundsCheckLastTouchDownPos.set(x, y);
                        return true;
                    }
                    return false;
                }
                Rect tmpRect = new Rect(this.mContentBounds);
                tmpRect.inset((-grid.allAppsIconSizePx) / 2, 0);
                if (ev.getX() < tmpRect.left || ev.getX() > tmpRect.right) {
                    this.mBoundsCheckLastTouchDownPos.set(x, y);
                    return true;
                }
                return false;
            case PackageInstallerCompat.STATUS_INSTALLING:
                if (this.mBoundsCheckLastTouchDownPos.x > -1) {
                    ViewConfiguration viewConfig = ViewConfiguration.get(getContext());
                    float dx = ev.getX() - this.mBoundsCheckLastTouchDownPos.x;
                    float dy = ev.getY() - this.mBoundsCheckLastTouchDownPos.y;
                    float distance = (float) Math.hypot(dx, dy);
                    if (distance < viewConfig.getScaledTouchSlop()) {
                        Launcher launcher = (Launcher) getContext();
                        launcher.showWorkspace(true);
                        return true;
                    }
                }
                break;
            case PackageInstallerCompat.STATUS_FAILED:
            default:
                return false;
            case 3:
                break;
        }
        this.mBoundsCheckLastTouchDownPos.set(-1, -1);
        return false;
    }

    @Override
    public void onSearchResult(String query, ArrayList<ComponentKey> apps) {
        if (apps == null) {
            return;
        }
        if (this.mApps.setOrderedFilter(apps)) {
            this.mAppsRecyclerView.onSearchResultsChanged();
        }
        this.mAdapter.setLastSearchQuery(query);
    }

    @Override
    public void clearSearchResult() {
        if (this.mApps.setOrderedFilter(null)) {
            this.mAppsRecyclerView.onSearchResultsChanged();
        }
        this.mSearchQueryBuilder.clear();
        this.mSearchQueryBuilder.clearSpans();
        Selection.setSelection(this.mSearchQueryBuilder, 0);
    }
}
