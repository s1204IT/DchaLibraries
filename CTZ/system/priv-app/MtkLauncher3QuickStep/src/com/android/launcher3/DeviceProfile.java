package com.android.launcher3;

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.android.launcher3.badge.BadgeRenderer;
import com.android.launcher3.graphics.IconNormalizer;

/* loaded from: classes.dex */
public class DeviceProfile {
    private static final float MAX_HORIZONTAL_PADDING_PERCENT = 0.14f;
    private static final float TALL_DEVICE_ASPECT_RATIO_THRESHOLD = 2.0f;
    public int allAppsCellHeightPx;
    public int allAppsIconDrawablePaddingPx;
    public int allAppsIconSizePx;
    public float allAppsIconTextSizePx;
    public final int availableHeightPx;
    public final int availableWidthPx;
    public int cellHeightPx;
    public final int cellLayoutBottomPaddingPx;
    public final int cellLayoutPaddingLeftRightPx;
    public int cellWidthPx;
    public final int defaultPageSpacingPx;
    public final Rect defaultWidgetPadding;
    public final int desiredWorkspaceLeftRightMarginPx;
    public int dropTargetBarSizePx;
    public final int edgeMarginPx;
    public int folderCellHeightPx;
    public int folderCellWidthPx;
    public int folderChildDrawablePaddingPx;
    public int folderChildIconSizePx;
    public int folderChildTextSizePx;
    public int folderIconOffsetYPx;
    public int folderIconSizePx;
    public final int heightPx;
    public final int hotseatBarBottomPaddingPx;
    public final int hotseatBarSidePaddingPx;
    public int hotseatBarSizePx;
    public final int hotseatBarTopPaddingPx;
    public int hotseatCellHeightPx;
    public int iconDrawablePaddingOriginalPx;
    public int iconDrawablePaddingPx;
    public int iconSizePx;
    public int iconTextSizePx;
    public final InvariantDeviceProfile inv;
    public final boolean isLandscape;
    public final boolean isLargeTablet;
    public final boolean isMultiWindowMode;
    public final boolean isPhone;
    public final boolean isTablet;
    public BadgeRenderer mBadgeRenderer;
    private boolean mIsSeascape;
    private final int topWorkspacePadding;
    public final boolean transposeLayoutWithOrientation;
    public final int verticalDragHandleSizePx;
    public final int widthPx;
    public int workspaceCellPaddingXPx;
    public float workspaceSpringLoadShrinkFactor;
    public final int workspaceSpringLoadedBottomSpace;
    public final PointF appWidgetScale = new PointF(1.0f, 1.0f);
    private final Rect mInsets = new Rect();
    public final Rect workspacePadding = new Rect();
    private final Rect mHotseatPadding = new Rect();

    public interface OnDeviceProfileChangeListener {
        void onDeviceProfileChanged(DeviceProfile deviceProfile);
    }

    public DeviceProfile(Context context, InvariantDeviceProfile invariantDeviceProfile, Point point, Point point2, int i, int i2, boolean z, boolean z2) {
        int i3;
        int dimensionPixelSize;
        this.inv = invariantDeviceProfile;
        this.isLandscape = z;
        this.isMultiWindowMode = z2;
        Resources resources = context.getResources();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();
        this.isTablet = resources.getBoolean(R.bool.is_tablet);
        this.isLargeTablet = resources.getBoolean(R.bool.is_large_tablet);
        this.isPhone = (this.isTablet || this.isLargeTablet) ? false : true;
        this.transposeLayoutWithOrientation = resources.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        if (!isVerticalBarLayout()) {
            i3 = 1;
        } else {
            i3 = 2;
        }
        Context context2 = getContext(context, i3);
        Resources resources2 = context2.getResources();
        this.defaultWidgetPadding = AppWidgetHostView.getDefaultPaddingForWidget(context2, new ComponentName(context2.getPackageName(), getClass().getName()), null);
        this.edgeMarginPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_edge_margin);
        this.desiredWorkspaceLeftRightMarginPx = isVerticalBarLayout() ? 0 : this.edgeMarginPx;
        this.cellLayoutPaddingLeftRightPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_padding);
        this.cellLayoutBottomPaddingPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_cell_layout_bottom_padding);
        this.verticalDragHandleSizePx = resources2.getDimensionPixelSize(R.dimen.vertical_drag_handle_size);
        this.defaultPageSpacingPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_page_spacing);
        this.topWorkspacePadding = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_workspace_top_padding);
        this.iconDrawablePaddingOriginalPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_icon_drawable_padding);
        this.dropTargetBarSizePx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_drop_target_size);
        this.workspaceSpringLoadedBottomSpace = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_min_spring_loaded_space);
        this.workspaceCellPaddingXPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_cell_padding_x);
        this.hotseatBarTopPaddingPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_top_padding);
        this.hotseatBarBottomPaddingPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_bottom_padding);
        this.hotseatBarSidePaddingPx = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_side_padding);
        if (isVerticalBarLayout()) {
            dimensionPixelSize = Utilities.pxFromDp(invariantDeviceProfile.iconSize, displayMetrics);
        } else {
            dimensionPixelSize = resources2.getDimensionPixelSize(R.dimen.dynamic_grid_hotseat_size) + this.hotseatBarTopPaddingPx + this.hotseatBarBottomPaddingPx;
        }
        this.hotseatBarSizePx = dimensionPixelSize;
        this.widthPx = i;
        this.heightPx = i2;
        if (z) {
            this.availableWidthPx = point2.x;
            this.availableHeightPx = point.y;
        } else {
            this.availableWidthPx = point.x;
            this.availableHeightPx = point2.y;
        }
        updateAvailableDimensions(displayMetrics, resources2);
        boolean z3 = Float.compare(((float) Math.max(this.widthPx, this.heightPx)) / ((float) Math.min(this.widthPx, this.heightPx)), TALL_DEVICE_ASPECT_RATIO_THRESHOLD) >= 0;
        if (!isVerticalBarLayout() && this.isPhone && z3) {
            this.hotseatBarSizePx += ((getCellSize().y - this.iconSizePx) - this.iconDrawablePaddingPx) - this.verticalDragHandleSizePx;
            updateAvailableDimensions(displayMetrics, resources2);
        }
        updateWorkspacePadding();
        this.mBadgeRenderer = new BadgeRenderer(this.iconSizePx);
    }

    public DeviceProfile copy(Context context) {
        Point point = new Point(this.availableWidthPx, this.availableHeightPx);
        return new DeviceProfile(context, this.inv, point, point, this.widthPx, this.heightPx, this.isLandscape, this.isMultiWindowMode);
    }

    public DeviceProfile getMultiWindowProfile(Context context, Point point) {
        point.set(Math.min(this.availableWidthPx, point.x), Math.min(this.availableHeightPx, point.y));
        DeviceProfile deviceProfile = new DeviceProfile(context, this.inv, point, point, point.x, point.y, this.isLandscape, true);
        if (((deviceProfile.getCellSize().y - deviceProfile.iconSizePx) - this.iconDrawablePaddingPx) - deviceProfile.iconTextSizePx < deviceProfile.iconDrawablePaddingPx * 2) {
            deviceProfile.adjustToHideWorkspaceLabels();
        }
        deviceProfile.appWidgetScale.set(deviceProfile.getCellSize().x / getCellSize().x, deviceProfile.getCellSize().y / getCellSize().y);
        deviceProfile.updateWorkspacePadding();
        return deviceProfile;
    }

    public DeviceProfile getFullScreenProfile() {
        return this.isLandscape ? this.inv.landscapeProfile : this.inv.portraitProfile;
    }

    private void adjustToHideWorkspaceLabels() {
        this.iconTextSizePx = 0;
        this.iconDrawablePaddingPx = 0;
        this.cellHeightPx = this.iconSizePx;
        this.allAppsCellHeightPx = this.allAppsIconSizePx + this.allAppsIconDrawablePaddingPx + Utilities.calculateTextHeight(this.allAppsIconTextSizePx) + (this.allAppsIconDrawablePaddingPx * (isVerticalBarLayout() ? 2 : 1) * 2);
    }

    private void updateAvailableDimensions(DisplayMetrics displayMetrics, Resources resources) {
        updateIconSize(1.0f, resources, displayMetrics);
        float f = this.cellHeightPx * this.inv.numRows;
        float f2 = this.availableHeightPx - getTotalWorkspacePadding().y;
        if (f > f2) {
            updateIconSize(f2 / f, resources, displayMetrics);
        }
        updateAvailableFolderCellDimensions(displayMetrics, resources);
    }

    private void updateIconSize(float f, Resources resources, DisplayMetrics displayMetrics) {
        boolean zIsVerticalBarLayout = isVerticalBarLayout();
        this.iconSizePx = (int) (Utilities.pxFromDp(zIsVerticalBarLayout ? this.inv.landscapeIconSize : this.inv.iconSize, displayMetrics) * f);
        this.iconTextSizePx = (int) (Utilities.pxFromSp(this.inv.iconTextSize, displayMetrics) * f);
        this.iconDrawablePaddingPx = (int) (this.iconDrawablePaddingOriginalPx * f);
        this.cellHeightPx = this.iconSizePx + this.iconDrawablePaddingPx + Utilities.calculateTextHeight(this.iconTextSizePx);
        int i = (getCellSize().y - this.cellHeightPx) / 2;
        if (this.iconDrawablePaddingPx > i && !zIsVerticalBarLayout && !this.isMultiWindowMode) {
            this.cellHeightPx -= this.iconDrawablePaddingPx - i;
            this.iconDrawablePaddingPx = i;
        }
        this.cellWidthPx = this.iconSizePx + this.iconDrawablePaddingPx;
        this.allAppsIconTextSizePx = this.iconTextSizePx;
        this.allAppsIconSizePx = this.iconSizePx;
        this.allAppsIconDrawablePaddingPx = this.iconDrawablePaddingPx;
        this.allAppsCellHeightPx = getCellSize().y;
        if (zIsVerticalBarLayout) {
            adjustToHideWorkspaceLabels();
        }
        if (zIsVerticalBarLayout) {
            this.hotseatBarSizePx = this.iconSizePx;
        }
        this.hotseatCellHeightPx = this.iconSizePx;
        if (zIsVerticalBarLayout) {
            this.workspaceSpringLoadShrinkFactor = resources.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f;
        } else {
            this.workspaceSpringLoadShrinkFactor = Math.min(resources.getInteger(R.integer.config_workspaceSpringLoadShrinkPercentage) / 100.0f, 1.0f - ((this.dropTargetBarSizePx + this.workspaceSpringLoadedBottomSpace) / (((this.availableHeightPx - this.hotseatBarSizePx) - this.verticalDragHandleSizePx) - this.topWorkspacePadding)));
        }
        this.folderIconSizePx = IconNormalizer.getNormalizedCircleSize(this.iconSizePx);
        this.folderIconOffsetYPx = (this.iconSizePx - this.folderIconSizePx) / 2;
    }

    private void updateAvailableFolderCellDimensions(DisplayMetrics displayMetrics, Resources resources) {
        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.folder_label_padding_top) + resources.getDimensionPixelSize(R.dimen.folder_label_padding_bottom) + Utilities.calculateTextHeight(resources.getDimension(R.dimen.folder_label_text_size));
        updateFolderCellSize(1.0f, displayMetrics, resources);
        int i = this.edgeMarginPx;
        Point totalWorkspacePadding = getTotalWorkspacePadding();
        float fMin = Math.min(((this.availableWidthPx - totalWorkspacePadding.x) - i) / (this.folderCellWidthPx * this.inv.numFolderColumns), ((this.availableHeightPx - totalWorkspacePadding.y) - i) / ((this.folderCellHeightPx * this.inv.numFolderRows) + dimensionPixelSize));
        if (fMin < 1.0f) {
            updateFolderCellSize(fMin, displayMetrics, resources);
        }
    }

    private void updateFolderCellSize(float f, DisplayMetrics displayMetrics, Resources resources) {
        this.folderChildIconSizePx = (int) (Utilities.pxFromDp(this.inv.iconSize, displayMetrics) * f);
        this.folderChildTextSizePx = (int) (resources.getDimensionPixelSize(R.dimen.folder_child_text_size) * f);
        int iCalculateTextHeight = Utilities.calculateTextHeight(this.folderChildTextSizePx);
        int dimensionPixelSize = (int) (resources.getDimensionPixelSize(R.dimen.folder_cell_x_padding) * f);
        this.folderCellWidthPx = this.folderChildIconSizePx + (dimensionPixelSize * 2);
        this.folderCellHeightPx = this.folderChildIconSizePx + (2 * ((int) (resources.getDimensionPixelSize(R.dimen.folder_cell_y_padding) * f))) + iCalculateTextHeight;
        this.folderChildDrawablePaddingPx = Math.max(0, ((this.folderCellHeightPx - this.folderChildIconSizePx) - iCalculateTextHeight) / 3);
    }

    public void updateInsets(Rect rect) {
        this.mInsets.set(rect);
        updateWorkspacePadding();
    }

    public Rect getInsets() {
        return this.mInsets;
    }

    public Point getCellSize() {
        Point point = new Point();
        Point totalWorkspacePadding = getTotalWorkspacePadding();
        point.x = calculateCellWidth((this.availableWidthPx - totalWorkspacePadding.x) - (this.cellLayoutPaddingLeftRightPx * 2), this.inv.numColumns);
        point.y = calculateCellHeight((this.availableHeightPx - totalWorkspacePadding.y) - this.cellLayoutBottomPaddingPx, this.inv.numRows);
        return point;
    }

    public Point getTotalWorkspacePadding() {
        updateWorkspacePadding();
        return new Point(this.workspacePadding.left + this.workspacePadding.right, this.workspacePadding.top + this.workspacePadding.bottom);
    }

    private void updateWorkspacePadding() {
        Rect rect = this.workspacePadding;
        if (isVerticalBarLayout()) {
            rect.top = 0;
            rect.bottom = this.edgeMarginPx;
            rect.left = this.hotseatBarSidePaddingPx;
            rect.right = this.hotseatBarSidePaddingPx;
            if (isSeascape()) {
                rect.left += this.hotseatBarSizePx;
                rect.right += this.verticalDragHandleSizePx;
                return;
            } else {
                rect.left += this.verticalDragHandleSizePx;
                rect.right += this.hotseatBarSizePx;
                return;
            }
        }
        int i = this.hotseatBarSizePx + this.verticalDragHandleSizePx;
        if (this.isTablet) {
            int iMin = ((int) Math.min(Math.max(0, this.widthPx - ((this.inv.numColumns * this.cellWidthPx) + ((this.inv.numColumns - 1) * this.cellWidthPx))), this.widthPx * MAX_HORIZONTAL_PADDING_PERCENT)) / 2;
            int iMax = Math.max(0, ((((this.heightPx - this.topWorkspacePadding) - i) - ((this.inv.numRows * 2) * this.cellHeightPx)) - this.hotseatBarTopPaddingPx) - this.hotseatBarBottomPaddingPx) / 2;
            rect.set(iMin, this.topWorkspacePadding + iMax, iMin, i + iMax);
            return;
        }
        rect.set(this.desiredWorkspaceLeftRightMarginPx, this.topWorkspacePadding, this.desiredWorkspaceLeftRightMarginPx, i);
    }

    public Rect getHotseatLayoutPadding() {
        if (isVerticalBarLayout()) {
            if (isSeascape()) {
                this.mHotseatPadding.set(this.mInsets.left, this.mInsets.top, this.hotseatBarSidePaddingPx, this.mInsets.bottom);
            } else {
                this.mHotseatPadding.set(this.hotseatBarSidePaddingPx, this.mInsets.top, this.mInsets.right, this.mInsets.bottom);
            }
        } else {
            int iRound = Math.round(((this.widthPx / this.inv.numColumns) - (this.widthPx / this.inv.numHotseatIcons)) / TALL_DEVICE_ASPECT_RATIO_THRESHOLD);
            this.mHotseatPadding.set(this.workspacePadding.left + iRound + this.cellLayoutPaddingLeftRightPx, this.hotseatBarTopPaddingPx, iRound + this.workspacePadding.right + this.cellLayoutPaddingLeftRightPx, this.hotseatBarBottomPaddingPx + this.mInsets.bottom + this.cellLayoutBottomPaddingPx);
        }
        return this.mHotseatPadding;
    }

    public Rect getAbsoluteOpenFolderBounds() {
        if (isVerticalBarLayout()) {
            return new Rect(this.mInsets.left + this.dropTargetBarSizePx + this.edgeMarginPx, this.mInsets.top, ((this.mInsets.left + this.availableWidthPx) - this.hotseatBarSizePx) - this.edgeMarginPx, this.mInsets.top + this.availableHeightPx);
        }
        return new Rect(this.mInsets.left + this.edgeMarginPx, this.mInsets.top + this.dropTargetBarSizePx + this.edgeMarginPx, (this.mInsets.left + this.availableWidthPx) - this.edgeMarginPx, (((this.mInsets.top + this.availableHeightPx) - this.hotseatBarSizePx) - this.verticalDragHandleSizePx) - this.edgeMarginPx);
    }

    public static int calculateCellWidth(int i, int i2) {
        return i / i2;
    }

    public static int calculateCellHeight(int i, int i2) {
        return i / i2;
    }

    public boolean isVerticalBarLayout() {
        return this.isLandscape && this.transposeLayoutWithOrientation;
    }

    public boolean updateIsSeascape(WindowManager windowManager) {
        if (isVerticalBarLayout()) {
            boolean z = windowManager.getDefaultDisplay().getRotation() == 3;
            if (this.mIsSeascape != z) {
                this.mIsSeascape = z;
                return true;
            }
        }
        return false;
    }

    public boolean isSeascape() {
        return isVerticalBarLayout() && this.mIsSeascape;
    }

    public boolean shouldFadeAdjacentWorkspaceScreens() {
        return isVerticalBarLayout() || this.isLargeTablet;
    }

    public int getCellHeight(int i) {
        switch (i) {
            case 0:
                return this.cellHeightPx;
            case 1:
                return this.hotseatCellHeightPx;
            case 2:
                return this.folderCellHeightPx;
            default:
                return 0;
        }
    }

    private static Context getContext(Context context, int i) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.orientation = i;
        return context.createConfigurationContext(configuration);
    }
}
