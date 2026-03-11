package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class DeviceProfile {
    public int allAppsButtonVisualSize;
    public final int allAppsIconSizePx;
    public final float allAppsIconTextSizeSp;
    public int allAppsNumCols;
    public int allAppsNumPredictiveCols;
    public final int availableHeightPx;
    public final int availableWidthPx;
    public int cellHeightPx;
    public int cellWidthPx;
    private final int defaultPageSpacingPx;
    public final Rect defaultWidgetPadding;
    private int desiredWorkspaceLeftRightMarginPx;
    private float dragViewScale;
    public final int edgeMarginPx;
    public int folderBackgroundOffset;
    public int folderCellHeightPx;
    public int folderCellWidthPx;
    public int folderIconSizePx;
    public final int heightPx;
    private int hotseatBarHeightPx;
    public int hotseatCellHeightPx;
    public int hotseatCellWidthPx;
    public int hotseatIconSizePx;
    public int iconDrawablePaddingOriginalPx;
    public int iconDrawablePaddingPx;
    public int iconSizePx;
    public int iconTextSizePx;
    public final InvariantDeviceProfile inv;
    public final boolean isLandscape;
    public final boolean isLargeTablet;
    public final boolean isPhone;
    public final boolean isTablet;
    private int normalHotseatBarHeightPx;
    private int normalSearchBarBottomPaddingPx;
    private int normalSearchBarSpaceHeightPx;
    private int normalSearchBarTopExtraPaddingPx;
    private final int overviewModeBarItemWidthPx;
    private final int overviewModeBarSpacerWidthPx;
    private final float overviewModeIconZoneRatio;
    private final int overviewModeMaxIconZoneHeightPx;
    private final int overviewModeMinIconZoneHeightPx;
    private final int pageIndicatorHeightPx;
    private int searchBarBottomPaddingPx;
    private int searchBarSpaceHeightPx;
    private int searchBarTopExtraPaddingPx;
    private int searchBarTopPaddingPx;
    private int searchBarWidgetInternalPaddingBottom;
    private int searchBarWidgetInternalPaddingTop;
    private int shortHotseatBarHeightPx;
    private int tallSearchBarBottomPaddingPx;
    private int tallSearchBarNegativeTopPaddingPx;
    private int tallSearchBarSpaceHeightPx;
    public final boolean transposeLayoutWithOrientation;
    public final int widthPx;

    public DeviceProfile(Context context, InvariantDeviceProfile inv, Point minSize, Point maxSize, int width, int height, boolean isLandscape) {
        boolean z = false;
        this.inv = inv;
        this.isLandscape = isLandscape;
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.isTablet = res.getBoolean(R.bool.is_tablet);
        this.isLargeTablet = res.getBoolean(R.bool.is_large_tablet);
        if (!this.isTablet && !this.isLargeTablet) {
            z = true;
        }
        this.isPhone = z;
        this.transposeLayoutWithOrientation = res.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        ComponentName cn = new ComponentName(context.getPackageName(), getClass().getName());
        this.defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context, cn, null);
        this.edgeMarginPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        this.desiredWorkspaceLeftRightMarginPx = this.edgeMarginPx * 2;
        this.pageIndicatorHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_page_indicator_height);
        this.defaultPageSpacingPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        this.overviewModeMinIconZoneHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_min_icon_zone_height);
        this.overviewModeMaxIconZoneHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_max_icon_zone_height);
        this.overviewModeBarItemWidthPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_item_width);
        this.overviewModeBarSpacerWidthPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_overview_bar_spacer_width);
        this.overviewModeIconZoneRatio = res.getInteger(R.integer.config_dynamic_grid_overview_icon_zone_percentage) / 100.0f;
        this.iconDrawablePaddingOriginalPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        this.allAppsIconTextSizeSp = inv.iconTextSize;
        this.allAppsIconSizePx = Utilities.pxFromDp(inv.iconSize, dm);
        this.widthPx = width;
        this.heightPx = height;
        if (isLandscape) {
            this.availableWidthPx = maxSize.x;
            this.availableHeightPx = minSize.y;
        } else {
            this.availableWidthPx = minSize.x;
            this.availableHeightPx = maxSize.y;
        }
        updateAvailableDimensions(dm, res);
        computeAllAppsButtonSize(context);
    }

    private void computeAllAppsButtonSize(Context context) {
        Resources res = context.getResources();
        float padding = res.getInteger(R.integer.config_allAppsButtonPaddingPercent) / 100.0f;
        this.allAppsButtonVisualSize = ((int) (this.hotseatIconSizePx * (1.0f - padding))) - context.getResources().getDimensionPixelSize(R.dimen.all_apps_button_scale_down);
    }

    private void updateAvailableDimensions(DisplayMetrics dm, Resources res) {
        float scale = 1.0f;
        int drawablePadding = this.iconDrawablePaddingOriginalPx;
        updateIconSize(1.0f, drawablePadding, res, dm);
        float usedHeight = this.cellHeightPx * this.inv.numRows;
        Rect workspacePadding = getWorkspacePadding(false);
        int maxHeight = (this.availableHeightPx - workspacePadding.top) - workspacePadding.bottom;
        if (usedHeight > maxHeight) {
            scale = maxHeight / usedHeight;
            drawablePadding = 0;
        }
        updateIconSize(scale, drawablePadding, res, dm);
    }

    private void updateIconSize(float scale, int drawablePadding, Resources res, DisplayMetrics dm) {
        this.iconSizePx = (int) (Utilities.pxFromDp(this.inv.iconSize, dm) * scale);
        this.iconTextSizePx = (int) (Utilities.pxFromSp(this.inv.iconTextSize, dm) * scale);
        this.iconDrawablePaddingPx = drawablePadding;
        this.hotseatIconSizePx = (int) (Utilities.pxFromDp(this.inv.hotseatIconSize, dm) * scale);
        this.normalSearchBarSpaceHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_height);
        this.tallSearchBarSpaceHeightPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_height_tall);
        this.searchBarWidgetInternalPaddingTop = res.getDimensionPixelSize(R.dimen.qsb_internal_padding_top);
        this.searchBarWidgetInternalPaddingBottom = res.getDimensionPixelSize(R.dimen.qsb_internal_padding_bottom);
        this.normalSearchBarTopExtraPaddingPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_extra_top_padding);
        this.tallSearchBarNegativeTopPaddingPx = res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_negative_top_padding_short);
        if (this.isTablet && !isVerticalBarLayout()) {
            this.searchBarTopPaddingPx = this.searchBarWidgetInternalPaddingTop;
            this.normalSearchBarBottomPaddingPx = this.searchBarWidgetInternalPaddingBottom + res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_bottom_padding_tablet);
            this.tallSearchBarBottomPaddingPx = this.normalSearchBarBottomPaddingPx;
        } else {
            this.searchBarTopPaddingPx = this.searchBarWidgetInternalPaddingTop;
            this.normalSearchBarBottomPaddingPx = this.searchBarWidgetInternalPaddingBottom + res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_bottom_padding);
            this.tallSearchBarBottomPaddingPx = this.searchBarWidgetInternalPaddingBottom + res.getDimensionPixelSize(R.dimen.dynamic_grid_search_bar_bottom_negative_padding_short);
        }
        Paint textPaint = new Paint();
        textPaint.setTextSize(this.iconTextSizePx);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        this.cellWidthPx = this.iconSizePx;
        this.cellHeightPx = this.iconSizePx + this.iconDrawablePaddingPx + ((int) Math.ceil(fm.bottom - fm.top));
        float scaleDps = res.getDimensionPixelSize(R.dimen.dragViewScale);
        this.dragViewScale = (this.iconSizePx + scaleDps) / this.iconSizePx;
        this.normalHotseatBarHeightPx = this.iconSizePx + (this.edgeMarginPx * 4);
        this.shortHotseatBarHeightPx = this.iconSizePx + (this.edgeMarginPx * 2);
        this.hotseatCellWidthPx = this.iconSizePx;
        this.hotseatCellHeightPx = this.iconSizePx;
        int folderCellPadding = (this.isTablet || this.isLandscape) ? this.edgeMarginPx * 6 : this.edgeMarginPx * 3;
        this.folderCellWidthPx = Math.min(this.cellWidthPx + folderCellPadding, (this.availableWidthPx - (this.edgeMarginPx * 4)) / this.inv.numFolderColumns);
        this.folderCellHeightPx = this.cellHeightPx + this.edgeMarginPx;
        this.folderBackgroundOffset = -this.edgeMarginPx;
        this.folderIconSizePx = this.iconSizePx + ((-this.folderBackgroundOffset) * 2);
    }

    public void updateAppsViewNumCols(Resources res, int recyclerViewWidth) {
        int appsViewLeftMarginPx = res.getDimensionPixelSize(R.dimen.all_apps_grid_view_start_margin);
        int allAppsCellWidthGap = res.getDimensionPixelSize(R.dimen.all_apps_icon_width_gap);
        int availableAppsWidthPx = recyclerViewWidth > 0 ? recyclerViewWidth : this.availableWidthPx;
        int numAppsCols = ((availableAppsWidthPx + allAppsCellWidthGap) - appsViewLeftMarginPx) / (this.allAppsIconSizePx + allAppsCellWidthGap);
        int numPredictiveAppCols = Math.max(this.inv.minAllAppsPredictionColumns, numAppsCols);
        this.allAppsNumCols = numAppsCols;
        this.allAppsNumPredictiveCols = numPredictiveAppCols;
    }

    private int getSearchBarTotalVerticalPadding() {
        return this.searchBarTopPaddingPx + this.searchBarTopExtraPaddingPx + this.searchBarBottomPaddingPx;
    }

    public Point getSearchBarDimensForWidgetOpts(Resources res) {
        Rect searchBarBounds = getSearchBarBounds(Utilities.isRtl(res));
        if (isVerticalBarLayout()) {
            return new Point(searchBarBounds.width(), searchBarBounds.height());
        }
        int widgetInternalPadding = this.searchBarWidgetInternalPaddingTop + this.searchBarWidgetInternalPaddingBottom;
        return new Point(searchBarBounds.width(), this.searchBarSpaceHeightPx + widgetInternalPadding);
    }

    public Rect getSearchBarBounds(boolean isLayoutRtl) {
        Rect bounds = new Rect();
        if (isVerticalBarLayout()) {
            if (isLayoutRtl) {
                bounds.set(this.availableWidthPx - this.normalSearchBarSpaceHeightPx, this.edgeMarginPx, this.availableWidthPx, this.availableHeightPx - this.edgeMarginPx);
            } else {
                bounds.set(0, this.edgeMarginPx, this.normalSearchBarSpaceHeightPx, this.availableHeightPx - this.edgeMarginPx);
            }
        } else {
            int boundsBottom = this.searchBarSpaceHeightPx + getSearchBarTotalVerticalPadding();
            if (this.isTablet) {
                int width = getCurrentWidth();
                int gap = ((width - (this.edgeMarginPx * 2)) - (this.inv.numColumns * this.cellWidthPx)) / ((this.inv.numColumns + 1) * 2);
                bounds.set(this.edgeMarginPx + gap, 0, this.availableWidthPx - (this.edgeMarginPx + gap), boundsBottom);
            } else {
                bounds.set(this.desiredWorkspaceLeftRightMarginPx - this.defaultWidgetPadding.left, 0, this.availableWidthPx - (this.desiredWorkspaceLeftRightMarginPx - this.defaultWidgetPadding.right), boundsBottom);
            }
        }
        return bounds;
    }

    Rect getWorkspacePadding(boolean isLayoutRtl) {
        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        Rect padding = new Rect();
        if (isVerticalBarLayout()) {
            if (isLayoutRtl) {
                padding.set(this.normalHotseatBarHeightPx, this.edgeMarginPx, searchBarBounds.width(), this.edgeMarginPx);
            } else {
                padding.set(searchBarBounds.width(), this.edgeMarginPx, this.normalHotseatBarHeightPx, this.edgeMarginPx);
            }
        } else {
            int paddingTop = searchBarBounds.bottom;
            int paddingBottom = this.hotseatBarHeightPx + this.pageIndicatorHeightPx;
            if (this.isTablet) {
                float gapScale = 1.0f + ((this.dragViewScale - 1.0f) / 2.0f);
                int width = getCurrentWidth();
                int height = getCurrentHeight();
                int availablePaddingX = (int) Math.min(Math.max(0, width - ((int) ((this.inv.numColumns * this.cellWidthPx) + (((this.inv.numColumns - 1) * gapScale) * this.cellWidthPx)))), width * 0.14f);
                int availablePaddingY = Math.max(0, ((height - paddingTop) - paddingBottom) - ((this.inv.numRows * 2) * this.cellHeightPx));
                padding.set(availablePaddingX / 2, (availablePaddingY / 2) + paddingTop, availablePaddingX / 2, (availablePaddingY / 2) + paddingBottom);
            } else {
                padding.set(this.desiredWorkspaceLeftRightMarginPx - this.defaultWidgetPadding.left, paddingTop, this.desiredWorkspaceLeftRightMarginPx - this.defaultWidgetPadding.right, paddingBottom);
            }
        }
        return padding;
    }

    private int getWorkspacePageSpacing(boolean isLayoutRtl) {
        if (isVerticalBarLayout() || this.isLargeTablet) {
            return this.defaultPageSpacingPx;
        }
        return Math.max(this.defaultPageSpacingPx, getWorkspacePadding(isLayoutRtl).left * 2);
    }

    int getOverviewModeButtonBarHeight() {
        int zoneHeight = (int) (this.overviewModeIconZoneRatio * this.availableHeightPx);
        return Math.min(this.overviewModeMaxIconZoneHeightPx, Math.max(this.overviewModeMinIconZoneHeightPx, zoneHeight));
    }

    Rect getHotseatRect() {
        if (isVerticalBarLayout()) {
            return new Rect(this.availableWidthPx - this.normalHotseatBarHeightPx, 0, Integer.MAX_VALUE, this.availableHeightPx);
        }
        return new Rect(0, this.availableHeightPx - this.hotseatBarHeightPx, this.availableWidthPx, Integer.MAX_VALUE);
    }

    public static int calculateCellWidth(int width, int countX) {
        return width / countX;
    }

    public static int calculateCellHeight(int height, int countY) {
        return height / countY;
    }

    boolean isVerticalBarLayout() {
        if (this.isLandscape) {
            return this.transposeLayoutWithOrientation;
        }
        return false;
    }

    boolean shouldFadeAdjacentWorkspaceScreens() {
        if (isVerticalBarLayout()) {
            return true;
        }
        return this.isLargeTablet;
    }

    private int getVisibleChildCount(ViewGroup parent) {
        int visibleChildren = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            if (parent.getChildAt(i).getVisibility() != 8) {
                visibleChildren++;
            }
        }
        return visibleChildren;
    }

    public void setSearchBarHeight(int searchBarHeight) {
        if (searchBarHeight == 1) {
            this.hotseatBarHeightPx = this.shortHotseatBarHeightPx;
            this.searchBarSpaceHeightPx = this.tallSearchBarSpaceHeightPx;
            this.searchBarBottomPaddingPx = this.tallSearchBarBottomPaddingPx;
            this.searchBarTopExtraPaddingPx = this.isPhone ? this.tallSearchBarNegativeTopPaddingPx : this.normalSearchBarTopExtraPaddingPx;
            return;
        }
        this.hotseatBarHeightPx = this.normalHotseatBarHeightPx;
        this.searchBarSpaceHeightPx = this.normalSearchBarSpaceHeightPx;
        this.searchBarBottomPaddingPx = this.normalSearchBarBottomPaddingPx;
        this.searchBarTopExtraPaddingPx = this.normalSearchBarTopExtraPaddingPx;
    }

    public void layout(Launcher launcher) {
        boolean hasVerticalBarLayout = isVerticalBarLayout();
        boolean isLayoutRtl = Utilities.isRtl(launcher.getResources());
        Rect searchBarBounds = getSearchBarBounds(isLayoutRtl);
        View searchBar = launcher.getSearchDropTargetBar();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) searchBar.getLayoutParams();
        lp.width = searchBarBounds.width();
        lp.height = searchBarBounds.height();
        lp.topMargin = this.searchBarTopExtraPaddingPx;
        if (hasVerticalBarLayout) {
            lp.gravity = 3;
            LinearLayout targets = (LinearLayout) searchBar.findViewById(R.id.drag_target_bar);
            targets.setOrientation(1);
            FrameLayout.LayoutParams targetsLp = (FrameLayout.LayoutParams) targets.getLayoutParams();
            targetsLp.gravity = 48;
            targetsLp.height = -2;
        } else {
            lp.gravity = 49;
        }
        searchBar.setLayoutParams(lp);
        PagedView workspace = (PagedView) launcher.findViewById(R.id.workspace);
        FrameLayout.LayoutParams lp2 = (FrameLayout.LayoutParams) workspace.getLayoutParams();
        lp2.gravity = 17;
        Rect padding = getWorkspacePadding(isLayoutRtl);
        workspace.setLayoutParams(lp2);
        workspace.setPadding(padding.left, padding.top, padding.right, padding.bottom);
        workspace.setPageSpacing(getWorkspacePageSpacing(isLayoutRtl));
        View hotseat = launcher.findViewById(R.id.hotseat);
        FrameLayout.LayoutParams lp3 = (FrameLayout.LayoutParams) hotseat.getLayoutParams();
        float workspaceCellWidth = getCurrentWidth() / this.inv.numColumns;
        float hotseatCellWidth = getCurrentWidth() / this.inv.numHotseatIcons;
        int hotseatAdjustment = Math.round((workspaceCellWidth - hotseatCellWidth) / 2.0f);
        if (hasVerticalBarLayout) {
            lp3.gravity = 5;
            lp3.width = this.normalHotseatBarHeightPx;
            lp3.height = -1;
            hotseat.findViewById(R.id.layout).setPadding(0, this.edgeMarginPx * 2, 0, this.edgeMarginPx * 2);
        } else if (this.isTablet) {
            lp3.gravity = 80;
            lp3.width = -1;
            lp3.height = this.hotseatBarHeightPx;
            hotseat.findViewById(R.id.layout).setPadding(padding.left + hotseatAdjustment, 0, padding.right + hotseatAdjustment, this.edgeMarginPx * 2);
        } else {
            lp3.gravity = 80;
            lp3.width = -1;
            lp3.height = this.hotseatBarHeightPx;
            hotseat.findViewById(R.id.layout).setPadding(padding.left + hotseatAdjustment, 0, padding.right + hotseatAdjustment, 0);
        }
        hotseat.setLayoutParams(lp3);
        View pageIndicator = launcher.findViewById(R.id.page_indicator);
        if (pageIndicator != null) {
            if (hasVerticalBarLayout) {
                pageIndicator.setVisibility(8);
            } else {
                FrameLayout.LayoutParams lp4 = (FrameLayout.LayoutParams) pageIndicator.getLayoutParams();
                lp4.gravity = 81;
                lp4.width = -2;
                lp4.height = -2;
                lp4.bottomMargin = this.hotseatBarHeightPx;
                pageIndicator.setLayoutParams(lp4);
            }
        }
        ViewGroup overviewMode = launcher.getOverviewPanel();
        if (overviewMode == null) {
            return;
        }
        int overviewButtonBarHeight = getOverviewModeButtonBarHeight();
        FrameLayout.LayoutParams lp5 = (FrameLayout.LayoutParams) overviewMode.getLayoutParams();
        lp5.gravity = 81;
        int visibleChildCount = getVisibleChildCount(overviewMode);
        int totalItemWidth = visibleChildCount * this.overviewModeBarItemWidthPx;
        int maxWidth = totalItemWidth + ((visibleChildCount - 1) * this.overviewModeBarSpacerWidthPx);
        lp5.width = Math.min(this.availableWidthPx, maxWidth);
        lp5.height = overviewButtonBarHeight;
        overviewMode.setLayoutParams(lp5);
        if (lp5.width <= totalItemWidth || visibleChildCount <= 1) {
            return;
        }
        int margin = (lp5.width - totalItemWidth) / (visibleChildCount - 1);
        View lastChild = null;
        for (int i = 0; i < visibleChildCount; i++) {
            if (lastChild != null) {
                ViewGroup.MarginLayoutParams clp = (ViewGroup.MarginLayoutParams) lastChild.getLayoutParams();
                if (isLayoutRtl) {
                    clp.leftMargin = margin;
                } else {
                    clp.rightMargin = margin;
                }
                lastChild.setLayoutParams(clp);
                lastChild = null;
            }
            View thisChild = overviewMode.getChildAt(i);
            if (thisChild.getVisibility() != 8) {
                lastChild = thisChild;
            }
        }
    }

    private int getCurrentWidth() {
        if (this.isLandscape) {
            return Math.max(this.widthPx, this.heightPx);
        }
        return Math.min(this.widthPx, this.heightPx);
    }

    private int getCurrentHeight() {
        if (this.isLandscape) {
            return Math.min(this.widthPx, this.heightPx);
        }
        return Math.max(this.widthPx, this.heightPx);
    }
}
