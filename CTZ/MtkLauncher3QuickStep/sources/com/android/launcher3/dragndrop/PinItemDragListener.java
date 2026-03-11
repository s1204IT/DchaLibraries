package com.android.launcher3.dragndrop;

import android.annotation.TargetApi;
import android.content.pm.LauncherApps;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.DragEvent;
import android.view.View;
import android.widget.RemoteViews;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.uioverrides.UiFactory;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.widget.PendingAddShortcutInfo;
import com.android.launcher3.widget.PendingAddWidgetInfo;
import com.android.launcher3.widget.PendingItemDragHelper;
import com.android.launcher3.widget.WidgetAddFlowHandler;

@TargetApi(26)
public class PinItemDragListener extends BaseItemDragListener {
    private final CancellationSignal mCancelSignal;
    private final LauncherApps.PinItemRequest mRequest;

    public PinItemDragListener(LauncherApps.PinItemRequest pinItemRequest, Rect rect, int i, int i2) {
        super(rect, i, i2);
        this.mRequest = pinItemRequest;
        this.mCancelSignal = new CancellationSignal();
    }

    @Override
    protected boolean onDragStart(DragEvent dragEvent) {
        if (!this.mRequest.isValid()) {
            return false;
        }
        return super.onDragStart(dragEvent);
    }

    @Override
    public boolean init(Launcher launcher, boolean z) {
        super.init(launcher, z);
        if (!z) {
            UiFactory.useFadeOutAnimationForLauncherStart(launcher, this.mCancelSignal);
            return false;
        }
        return false;
    }

    @Override
    protected PendingItemDragHelper createDragHelper() {
        Object pendingAddShortcutInfo;
        if (this.mRequest.getRequestType() == 1) {
            pendingAddShortcutInfo = new PendingAddShortcutInfo(new PinShortcutRequestActivityInfo(this.mRequest, this.mLauncher));
        } else {
            LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfoFromProviderInfo = LauncherAppWidgetProviderInfo.fromProviderInfo(this.mLauncher, this.mRequest.getAppWidgetProviderInfo(this.mLauncher));
            final PinWidgetFlowHandler pinWidgetFlowHandler = new PinWidgetFlowHandler(launcherAppWidgetProviderInfoFromProviderInfo, this.mRequest);
            pendingAddShortcutInfo = new PendingAddWidgetInfo(launcherAppWidgetProviderInfoFromProviderInfo) {
                @Override
                public WidgetAddFlowHandler getHandler() {
                    return pinWidgetFlowHandler;
                }
            };
        }
        View view = new View(this.mLauncher);
        view.setTag(pendingAddShortcutInfo);
        PendingItemDragHelper pendingItemDragHelper = new PendingItemDragHelper(view);
        if (this.mRequest.getRequestType() == 2) {
            pendingItemDragHelper.setPreview(getPreview(this.mRequest));
        }
        return pendingItemDragHelper;
    }

    @Override
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        target2.containerType = 10;
    }

    @Override
    protected void postCleanup() {
        super.postCleanup();
        this.mCancelSignal.cancel();
    }

    public static RemoteViews getPreview(LauncherApps.PinItemRequest pinItemRequest) {
        Bundle extras = pinItemRequest.getExtras();
        if (extras != null && (extras.get("appWidgetPreview") instanceof RemoteViews)) {
            return (RemoteViews) extras.get("appWidgetPreview");
        }
        return null;
    }
}
