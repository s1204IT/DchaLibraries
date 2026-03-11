package com.android.launcher3;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import com.android.launcher3.DropTarget;
import com.android.launcher3.util.FlingAnimation;

public class DeleteDropTarget extends ButtonDropTarget {
    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHoverColor = getResources().getColor(R.color.delete_target_hover_tint);
        setDrawable(R.drawable.ic_remove_launcher);
    }

    public static boolean supportsDrop(Object info) {
        if ((info instanceof ShortcutInfo) || (info instanceof LauncherAppWidgetInfo)) {
            return true;
        }
        return info instanceof FolderInfo;
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        if (source.supportsDeleteDropTarget()) {
            return supportsDrop(info);
        }
        return false;
    }

    @Override
    void completeDrop(DropTarget.DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;
        if (!(d.dragSource instanceof Workspace) && !(d.dragSource instanceof Folder)) {
            return;
        }
        removeWorkspaceOrFolderItem(this.mLauncher, item, null);
    }

    public static void removeWorkspaceOrFolderItem(Launcher launcher, ItemInfo item, View view) {
        launcher.removeItem(view, item, true);
        launcher.getWorkspace().stripEmptyScreens();
        launcher.getDragLayer().announceForAccessibility(launcher.getString(R.string.item_removed));
    }

    @Override
    public void onFlingToDelete(final DropTarget.DragObject d, PointF vel) {
        d.dragView.setColor(0);
        d.dragView.updateInitialScaleToCurrentScale();
        DragLayer dragLayer = this.mLauncher.getDragLayer();
        FlingAnimation fling = new FlingAnimation(d, vel, getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(), this.mDrawable.getIntrinsicWidth(), this.mDrawable.getIntrinsicHeight()), dragLayer);
        final int duration = fling.getDuration();
        final long startTime = AnimationUtils.currentAnimationTimeMillis();
        TimeInterpolator tInterpolator = new TimeInterpolator() {
            private int mCount = -1;
            private float mOffset = 0.0f;

            @Override
            public float getInterpolation(float t) {
                if (this.mCount < 0) {
                    this.mCount++;
                } else if (this.mCount == 0) {
                    this.mOffset = Math.min(0.5f, (AnimationUtils.currentAnimationTimeMillis() - startTime) / duration);
                    this.mCount++;
                }
                return Math.min(1.0f, this.mOffset + t);
            }
        };
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                DeleteDropTarget.this.mLauncher.exitSpringLoadedDragMode();
                DeleteDropTarget.this.completeDrop(d);
                DeleteDropTarget.this.mLauncher.getDragController().onDeferredEndFling(d);
            }
        };
        dragLayer.animateView(d.dragView, fling, duration, tInterpolator, onAnimationEndRunnable, 0, null);
    }
}
