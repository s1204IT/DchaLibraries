package com.android.launcher3;

import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import com.android.launcher3.DropTarget;
import com.android.launcher3.compat.UserHandleCompat;

public class InfoDropTarget extends ButtonDropTarget {
    public InfoDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHoverColor = getResources().getColor(R.color.info_target_hover_tint);
        setDrawable(R.drawable.ic_info_launcher);
    }

    public static void startDetailsActivityForInfo(Object info, Launcher launcher) {
        UserHandleCompat user;
        ComponentName componentName = null;
        if (info instanceof AppInfo) {
            componentName = ((AppInfo) info).componentName;
        } else if (info instanceof ShortcutInfo) {
            componentName = ((ShortcutInfo) info).intent.getComponent();
        } else if (info instanceof PendingAddItemInfo) {
            componentName = ((PendingAddItemInfo) info).componentName;
        }
        if (info instanceof ItemInfo) {
            user = ((ItemInfo) info).user;
        } else {
            user = UserHandleCompat.myUserHandle();
        }
        if (componentName == null) {
            return;
        }
        launcher.startApplicationDetailsActivity(componentName, user);
    }

    @Override
    protected boolean supportsDrop(DragSource source, Object info) {
        if (source.supportsAppInfoDropTarget()) {
            return supportsDrop(getContext(), info);
        }
        return false;
    }

    public static boolean supportsDrop(Context context, Object info) {
        if (info instanceof AppInfo) {
            return true;
        }
        return info instanceof PendingAddItemInfo;
    }

    @Override
    void completeDrop(DropTarget.DragObject d) {
        startDetailsActivityForInfo(d.dragInfo, this.mLauncher);
    }
}
