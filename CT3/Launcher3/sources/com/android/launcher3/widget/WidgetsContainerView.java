package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.BaseContainerView;
import com.android.launcher3.CellLayout;
import com.android.launcher3.DeleteDropTarget;
import com.android.launcher3.DragController;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Folder;
import com.android.launcher3.IconCache;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.PendingAddItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.WidgetPreviewLoader;
import com.android.launcher3.Workspace;
import com.android.launcher3.model.WidgetsModel;

public class WidgetsContainerView extends BaseContainerView implements View.OnLongClickListener, View.OnClickListener, DragSource {
    private WidgetsListAdapter mAdapter;
    private DragController mDragController;
    private IconCache mIconCache;
    Launcher mLauncher;
    private WidgetsRecyclerView mRecyclerView;
    private Toast mWidgetInstructionToast;
    private WidgetPreviewLoader mWidgetPreviewLoader;

    public WidgetsContainerView(Context context) {
        this(context, null);
    }

    public WidgetsContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetsContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mLauncher = (Launcher) context;
        this.mDragController = this.mLauncher.getDragController();
        this.mAdapter = new WidgetsListAdapter(context, this, this, this.mLauncher);
        this.mIconCache = LauncherAppState.getInstance().getIconCache();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mRecyclerView = (WidgetsRecyclerView) getContentView().findViewById(R.id.widgets_list_view);
        this.mRecyclerView.setAdapter(this.mAdapter);
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    public void scrollToTop() {
        this.mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onClick(View v) {
        if (!this.mLauncher.isWidgetsViewVisible() || this.mLauncher.getWorkspace().isSwitchingState() || !(v instanceof WidgetCell)) {
            return;
        }
        if (this.mWidgetInstructionToast != null) {
            this.mWidgetInstructionToast.cancel();
        }
        CharSequence msg = Utilities.wrapForTts(getContext().getText(R.string.long_press_widget_to_add), getContext().getString(R.string.long_accessible_way_to_add));
        this.mWidgetInstructionToast = Toast.makeText(getContext(), msg, 0);
        this.mWidgetInstructionToast.show();
    }

    @Override
    public boolean onLongClick(View v) {
        if (!v.isInTouchMode() || !this.mLauncher.isWidgetsViewVisible() || this.mLauncher.getWorkspace().isSwitchingState() || !this.mLauncher.isDraggingEnabled()) {
            return false;
        }
        boolean status = beginDragging(v);
        if (status && (v.getTag() instanceof PendingAddWidgetInfo)) {
            WidgetHostViewLoader hostLoader = new WidgetHostViewLoader(this.mLauncher, v);
            hostLoader.preloadWidget();
            this.mLauncher.getDragController().addDragListener(hostLoader);
        }
        return status;
    }

    private boolean beginDragging(View v) {
        if (v instanceof WidgetCell) {
            if (!beginDraggingWidget((WidgetCell) v)) {
                return false;
            }
        } else {
            Log.e("WidgetsContainerView", "Unexpected dragging view: " + v);
        }
        if (this.mLauncher.getDragController().isDragging()) {
            this.mLauncher.enterSpringLoadedDragMode();
            return true;
        }
        return true;
    }

    private boolean beginDraggingWidget(WidgetCell v) {
        Bitmap preview;
        float scale;
        WidgetImageView image = (WidgetImageView) v.findViewById(R.id.widget_preview);
        PendingAddItemInfo createItemInfo = (PendingAddItemInfo) v.getTag();
        if (image.getBitmap() == null) {
            return false;
        }
        Rect bounds = image.getBitmapBounds();
        if (createItemInfo instanceof PendingAddWidgetInfo) {
            PendingAddWidgetInfo createWidgetInfo = (PendingAddWidgetInfo) createItemInfo;
            int[] size = this.mLauncher.getWorkspace().estimateItemSize(createWidgetInfo, true);
            Bitmap icon = image.getBitmap();
            int maxWidth = Math.min((int) (icon.getWidth() * 1.25f), size[0]);
            int[] previewSizeBeforeScale = new int[1];
            preview = getWidgetPreviewLoader().generateWidgetPreview(this.mLauncher, createWidgetInfo.info, maxWidth, null, previewSizeBeforeScale);
            if (previewSizeBeforeScale[0] < icon.getWidth()) {
                int padding = (icon.getWidth() - previewSizeBeforeScale[0]) / 2;
                if (icon.getWidth() > image.getWidth()) {
                    padding = (image.getWidth() * padding) / icon.getWidth();
                }
                bounds.left += padding;
                bounds.right -= padding;
            }
            scale = bounds.width() / preview.getWidth();
        } else {
            PendingAddShortcutInfo createShortcutInfo = (PendingAddShortcutInfo) v.getTag();
            preview = Utilities.createIconBitmap(this.mIconCache.getFullResIcon(createShortcutInfo.activityInfo), this.mLauncher);
            createItemInfo.spanY = 1;
            createItemInfo.spanX = 1;
            scale = this.mLauncher.getDeviceProfile().iconSizePx / preview.getWidth();
        }
        boolean clipAlpha = ((createItemInfo instanceof PendingAddWidgetInfo) && ((PendingAddWidgetInfo) createItemInfo).previewImage == 0) ? false : true;
        this.mLauncher.lockScreenOrientation();
        this.mLauncher.getWorkspace().onDragStartedWithItem(createItemInfo, preview, clipAlpha);
        this.mDragController.startDrag(image, preview, this, createItemInfo, bounds, DragController.DRAG_ACTION_COPY, scale);
        preview.recycle();
        return true;
    }

    @Override
    public boolean supportsFlingToDelete() {
        return false;
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
        return 0.0f;
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
    protected void onUpdateBgPadding(Rect padding, Rect bgPadding) {
        if (Utilities.isRtl(getResources())) {
            getContentView().setPadding(0, bgPadding.top, bgPadding.right, bgPadding.bottom);
            this.mRecyclerView.updateBackgroundPadding(new Rect(bgPadding.left, 0, 0, 0));
        } else {
            getContentView().setPadding(bgPadding.left, bgPadding.top, 0, bgPadding.bottom);
            this.mRecyclerView.updateBackgroundPadding(new Rect(0, 0, bgPadding.right, 0));
        }
    }

    public void addWidgets(WidgetsModel model) {
        this.mRecyclerView.setWidgets(model);
        this.mAdapter.setWidgetsModel(model);
        this.mAdapter.notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return this.mAdapter.getItemCount() == 0;
    }

    private WidgetPreviewLoader getWidgetPreviewLoader() {
        if (this.mWidgetPreviewLoader == null) {
            this.mWidgetPreviewLoader = LauncherAppState.getInstance().getWidgetCache();
        }
        return this.mWidgetPreviewLoader;
    }
}
