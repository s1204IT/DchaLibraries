package com.android.launcher3.dragndrop;

import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.states.InternalStateHandler;
import com.android.launcher3.widget.PendingItemDragHelper;
import java.util.UUID;

/* loaded from: classes.dex */
public abstract class BaseItemDragListener extends InternalStateHandler implements View.OnDragListener, DragSource, DragOptions.PreDragCondition {
    public static final String EXTRA_PIN_ITEM_DRAG_LISTENER = "pin_item_drag_listener";
    private static final String MIME_TYPE_PREFIX = "com.android.launcher3.drag_and_drop/";
    private static final String TAG = "BaseItemDragListener";
    private DragController mDragController;
    private long mDragStartTime;
    private final String mId = UUID.randomUUID().toString();
    protected Launcher mLauncher;
    private final int mPreviewBitmapWidth;
    private final Rect mPreviewRect;
    private final int mPreviewViewWidth;

    protected abstract PendingItemDragHelper createDragHelper();

    public BaseItemDragListener(Rect rect, int i, int i2) {
        this.mPreviewRect = rect;
        this.mPreviewBitmapWidth = i;
        this.mPreviewViewWidth = i2;
    }

    public String getMimeType() {
        return MIME_TYPE_PREFIX + this.mId;
    }

    @Override // com.android.launcher3.states.InternalStateHandler
    public boolean init(Launcher launcher, boolean z) {
        AbstractFloatingView.closeAllOpenViews(launcher, z);
        launcher.getStateManager().goToState(LauncherState.NORMAL, z);
        launcher.getDragLayer().setOnDragListener(this);
        launcher.getRotationHelper().setStateHandlerRequest(2);
        this.mLauncher = launcher;
        this.mDragController = launcher.getDragController();
        return false;
    }

    @Override // android.view.View.OnDragListener
    public boolean onDrag(View view, DragEvent dragEvent) {
        if (this.mLauncher == null || this.mDragController == null) {
            postCleanup();
            return false;
        }
        if (dragEvent.getAction() == 1) {
            if (onDragStart(dragEvent)) {
                return true;
            }
            postCleanup();
            return false;
        }
        return this.mDragController.onDragEvent(this.mDragStartTime, dragEvent);
    }

    protected boolean onDragStart(DragEvent dragEvent) throws Resources.NotFoundException, PackageManager.NameNotFoundException {
        ClipDescription clipDescription = dragEvent.getClipDescription();
        if (clipDescription == null || !clipDescription.hasMimeType(getMimeType())) {
            Log.e(TAG, "Someone started a dragAndDrop before us.");
            return false;
        }
        Point point = new Point((int) dragEvent.getX(), (int) dragEvent.getY());
        DragOptions dragOptions = new DragOptions();
        dragOptions.systemDndStartPoint = point;
        dragOptions.preDragCondition = this;
        createDragHelper().startDrag(new Rect(this.mPreviewRect), this.mPreviewBitmapWidth, this.mPreviewViewWidth, point, this, dragOptions);
        this.mDragStartTime = SystemClock.uptimeMillis();
        return true;
    }

    @Override // com.android.launcher3.dragndrop.DragOptions.PreDragCondition
    public boolean shouldStartDrag(double d) {
        return !this.mLauncher.isWorkspaceLocked();
    }

    @Override // com.android.launcher3.dragndrop.DragOptions.PreDragCondition
    public void onPreDragStart(DropTarget.DragObject dragObject) {
        this.mLauncher.getDragLayer().setAlpha(1.0f);
        dragObject.dragView.setColor(this.mLauncher.getResources().getColor(R.color.delete_target_hover_tint));
    }

    @Override // com.android.launcher3.dragndrop.DragOptions.PreDragCondition
    public void onPreDragEnd(DropTarget.DragObject dragObject, boolean z) {
        if (z) {
            dragObject.dragView.setColor(0);
        }
    }

    @Override // com.android.launcher3.DragSource
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
        postCleanup();
    }

    protected void postCleanup() {
        clearReference();
        if (this.mLauncher != null) {
            Intent intent = new Intent(this.mLauncher.getIntent());
            intent.removeExtra(EXTRA_PIN_ITEM_DRAG_LISTENER);
            this.mLauncher.setIntent(intent);
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.android.launcher3.dragndrop.-$$Lambda$GXOZhabmkyV94fsaekT73At9yKU
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.removeListener();
            }
        });
    }

    public void removeListener() {
        if (this.mLauncher != null) {
            this.mLauncher.getRotationHelper().setStateHandlerRequest(0);
            this.mLauncher.getDragLayer().setOnDragListener(null);
        }
    }
}
