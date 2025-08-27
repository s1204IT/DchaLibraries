package com.android.systemui.statusbar.tv;

import android.graphics.Rect;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.CommandQueue;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class TvStatusBar extends SystemUI implements CommandQueue.Callbacks {
    private IStatusBarService mBarService;

    @Override // com.android.systemui.SystemUI
    public void start() {
        putComponent(TvStatusBar.class, this);
        CommandQueue commandQueue = (CommandQueue) getComponent(CommandQueue.class);
        commandQueue.addCallbacks(this);
        int[] iArr = new int[9];
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        Rect rect = new Rect();
        Rect rect2 = new Rect();
        this.mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));
        try {
            this.mBarService.registerStatusBar(commandQueue, arrayList2, arrayList3, iArr, arrayList, rect, rect2);
        } catch (RemoteException e) {
        }
    }
}
