package com.android.systemui.statusbar;

import android.R;
import android.content.Context;
import android.view.View;

public abstract class NotificationViewWrapper {
    protected final View mView;

    public abstract void setDark(boolean z, boolean z2, long j);

    public static NotificationViewWrapper wrap(Context ctx, View v) {
        if (v.findViewById(R.id.four) != null) {
            return new NotificationMediaViewWrapper(ctx, v);
        }
        if (v.getId() == 16909107) {
            return new NotificationTemplateViewWrapper(ctx, v);
        }
        return new NotificationCustomViewWrapper(v);
    }

    protected NotificationViewWrapper(View view) {
        this.mView = view;
    }

    public void notifyContentUpdated() {
    }
}
