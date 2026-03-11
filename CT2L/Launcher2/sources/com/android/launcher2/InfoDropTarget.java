package com.android.launcher2;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.os.Process;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.view.ViewGroup;
import com.android.launcher.R;
import com.android.launcher2.DropTarget;

public class InfoDropTarget extends ButtonDropTarget {
    private TransitionDrawable mDrawable;
    private ColorStateList mOriginalTextColor;

    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mOriginalTextColor = getTextColors();
        Resources r = getResources();
        this.mHoverColor = r.getColor(R.color.info_target_hover_tint);
        this.mDrawable = (TransitionDrawable) getCurrentDrawable();
        if (this.mDrawable != null) {
            this.mDrawable.setCrossFadeEnabled(true);
        }
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == 2 && !LauncherApplication.isScreenLarge()) {
            setText("");
        }
    }

    private boolean isFromAllApps(DragSource source) {
        return source instanceof AppsCustomizePagedView;
    }

    @Override
    public boolean acceptDrop(DropTarget.DragObject d) {
        UserHandle user;
        ComponentName componentName = null;
        if (d.dragInfo instanceof ApplicationInfo) {
            componentName = ((ApplicationInfo) d.dragInfo).componentName;
        } else if (d.dragInfo instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) d.dragInfo).intent.getComponent();
        } else if (d.dragInfo instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) d.dragInfo).componentName;
        }
        if (d.dragInfo instanceof ItemInfo) {
            user = ((ItemInfo) d.dragInfo).user;
        } else {
            user = Process.myUserHandle();
        }
        if (componentName != null) {
            this.mLauncher.startApplicationDetailsActivity(componentName, user);
        }
        d.deferDragViewCleanupPostAnimation = false;
        return false;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean isVisible = true;
        if (!isFromAllApps(source)) {
            isVisible = false;
        }
        this.mActive = isVisible;
        this.mDrawable.resetTransition();
        setTextColor(this.mOriginalTextColor);
        ((ViewGroup) getParent()).setVisibility(isVisible ? 0 : 8);
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        this.mActive = false;
    }

    @Override
    public void onDragEnter(DropTarget.DragObject d) {
        super.onDragEnter(d);
        this.mDrawable.startTransition(this.mTransitionDuration);
        setTextColor(this.mHoverColor);
    }

    @Override
    public void onDragExit(DropTarget.DragObject d) {
        super.onDragExit(d);
        if (!d.dragComplete) {
            this.mDrawable.resetTransition();
            setTextColor(this.mOriginalTextColor);
        }
    }
}
