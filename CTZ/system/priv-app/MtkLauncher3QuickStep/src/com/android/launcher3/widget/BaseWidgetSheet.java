package com.android.launcher3.widget;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.graphics.ColorScrim;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.AbstractSlideInView;

/* loaded from: classes.dex */
abstract class BaseWidgetSheet extends AbstractSlideInView implements View.OnClickListener, View.OnLongClickListener, DragSource {
    protected final ColorScrim mColorScrim;
    private Toast mWidgetInstructionToast;

    protected abstract int getElementsRowCount();

    public BaseWidgetSheet(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mColorScrim = ColorScrim.createExtractedColorScrim(this);
    }

    @Override // android.view.View.OnClickListener
    public final void onClick(View view) {
        if (this.mWidgetInstructionToast != null) {
            this.mWidgetInstructionToast.cancel();
        }
        this.mWidgetInstructionToast = Toast.makeText(getContext(), Utilities.wrapForTts(getContext().getText(R.string.long_press_widget_to_add), getContext().getString(R.string.long_accessible_way_to_add)), 0);
        this.mWidgetInstructionToast.show();
    }

    @Override // android.view.View.OnLongClickListener
    public final boolean onLongClick(View view) {
        if (!ItemLongClickListener.canStartDrag(this.mLauncher)) {
            return false;
        }
        if (view instanceof WidgetCell) {
            return beginDraggingWidget((WidgetCell) view);
        }
        return true;
    }

    @Override // com.android.launcher3.views.AbstractSlideInView
    protected void setTranslationShift(float f) {
        super.setTranslationShift(f);
        this.mColorScrim.setProgress(1.0f - this.mTranslationShift);
    }

    private boolean beginDraggingWidget(WidgetCell widgetCell) throws Resources.NotFoundException, PackageManager.NameNotFoundException {
        WidgetImageView widgetView = widgetCell.getWidgetView();
        if (widgetView.getBitmap() == null) {
            return false;
        }
        int[] iArr = new int[2];
        this.mLauncher.getDragLayer().getLocationInDragLayer(widgetView, iArr);
        new PendingItemDragHelper(widgetCell).startDrag(widgetView.getBitmapBounds(), widgetView.getBitmap().getWidth(), widgetView.getWidth(), new Point(iArr[0], iArr[1]), this, new DragOptions());
        close(true);
        return true;
    }

    @Override // com.android.launcher3.DragSource
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
    }

    @Override // com.android.launcher3.views.AbstractSlideInView
    protected void onCloseComplete() {
        super.onCloseComplete();
        clearNavBarColor();
    }

    protected void clearNavBarColor() {
        this.mLauncher.getSystemUiController().updateUiState(2, 0);
    }

    protected void setupNavBarColor() {
        int i;
        boolean attrBoolean = Themes.getAttrBoolean(this.mLauncher, R.attr.isMainColorDark);
        SystemUiController systemUiController = this.mLauncher.getSystemUiController();
        if (attrBoolean) {
            i = 2;
        } else {
            i = 1;
        }
        systemUiController.updateUiState(2, i);
    }

    @Override // com.android.launcher3.logging.UserEventDispatcher.LogContainerProvider
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        target2.containerType = 5;
        target2.cardinality = getElementsRowCount();
    }

    @Override // com.android.launcher3.AbstractFloatingView
    public final void logActionCommand(int i) {
        LauncherLogProto.Target targetNewContainerTarget = LoggerUtils.newContainerTarget(5);
        targetNewContainerTarget.cardinality = getElementsRowCount();
        this.mLauncher.getUserEventDispatcher().logActionCommand(i, targetNewContainerTarget);
    }
}
