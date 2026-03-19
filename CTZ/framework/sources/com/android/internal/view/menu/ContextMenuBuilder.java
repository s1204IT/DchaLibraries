package com.android.internal.view.menu;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.os.health.ServiceHealthStats;
import android.util.EventLog;
import android.view.ContextMenu;
import android.view.View;

public class ContextMenuBuilder extends MenuBuilder implements ContextMenu {
    public ContextMenuBuilder(Context context) {
        super(context);
    }

    @Override
    public ContextMenu setHeaderIcon(Drawable drawable) {
        return (ContextMenu) super.setHeaderIconInt(drawable);
    }

    @Override
    public ContextMenu setHeaderIcon(int i) {
        return (ContextMenu) super.setHeaderIconInt(i);
    }

    @Override
    public ContextMenu setHeaderTitle(CharSequence charSequence) {
        return (ContextMenu) super.setHeaderTitleInt(charSequence);
    }

    @Override
    public ContextMenu setHeaderTitle(int i) {
        return (ContextMenu) super.setHeaderTitleInt(i);
    }

    @Override
    public ContextMenu setHeaderView(View view) {
        return (ContextMenu) super.setHeaderViewInt(view);
    }

    public MenuDialogHelper showDialog(View view, IBinder iBinder) {
        if (view != null) {
            view.createContextMenu(this);
        }
        if (getVisibleItems().size() > 0) {
            EventLog.writeEvent(ServiceHealthStats.MEASUREMENT_START_SERVICE_COUNT, 1);
            MenuDialogHelper menuDialogHelper = new MenuDialogHelper(this);
            menuDialogHelper.show(iBinder);
            return menuDialogHelper;
        }
        return null;
    }

    public MenuPopupHelper showPopup(Context context, View view, float f, float f2) {
        if (view != null) {
            view.createContextMenu(this);
        }
        if (getVisibleItems().size() > 0) {
            EventLog.writeEvent(ServiceHealthStats.MEASUREMENT_START_SERVICE_COUNT, 1);
            view.getLocationOnScreen(new int[2]);
            MenuPopupHelper menuPopupHelper = new MenuPopupHelper(context, this, view, false, 16844033);
            menuPopupHelper.show(Math.round(f), Math.round(f2));
            return menuPopupHelper;
        }
        return null;
    }
}
