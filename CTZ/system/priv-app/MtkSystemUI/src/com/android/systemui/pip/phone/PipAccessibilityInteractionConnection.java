package com.android.systemui.pip.phone;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class PipAccessibilityInteractionConnection extends IAccessibilityInteractionConnection.Stub {
    private List<AccessibilityNodeInfo> mAccessibilityNodeInfoList;
    private AccessibilityCallbacks mCallbacks;
    private Handler mHandler;
    private PipMotionHelper mMotionHelper;
    private Rect mTmpBounds = new Rect();

    public interface AccessibilityCallbacks {
        void onAccessibilityShowMenu();
    }

    public PipAccessibilityInteractionConnection(PipMotionHelper pipMotionHelper, AccessibilityCallbacks accessibilityCallbacks, Handler handler) {
        this.mHandler = handler;
        this.mMotionHelper = pipMotionHelper;
        this.mCallbacks = accessibilityCallbacks;
    }

    public void findAccessibilityNodeInfoByAccessibilityId(long j, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec, Bundle bundle) {
        try {
            iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfosResult(j == AccessibilityNodeInfo.ROOT_NODE_ID ? getNodeList() : null, i);
        } catch (RemoteException e) {
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:17:0x005c  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void performAccessibilityAction(long j, int i, Bundle bundle, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2) {
        boolean z = true;
        if (j == AccessibilityNodeInfo.ROOT_NODE_ID) {
            if (i == 16) {
                this.mHandler.post(new Runnable() { // from class: com.android.systemui.pip.phone.-$$Lambda$PipAccessibilityInteractionConnection$yj5JMyeINsNwnRK777qXcVORJV0
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.mCallbacks.onAccessibilityShowMenu();
                    }
                });
            } else if (i == 262144) {
                this.mMotionHelper.expandPip();
            } else if (i == 1048576) {
                this.mMotionHelper.dismissPip();
            } else if (i == 16908354) {
                int i5 = bundle.getInt("ACTION_ARGUMENT_MOVE_WINDOW_X");
                int i6 = bundle.getInt("ACTION_ARGUMENT_MOVE_WINDOW_Y");
                new Rect().set(this.mMotionHelper.getBounds());
                this.mTmpBounds.offsetTo(i5, i6);
                this.mMotionHelper.movePip(this.mTmpBounds);
            }
        } else {
            z = false;
        }
        try {
            iAccessibilityInteractionConnectionCallback.setPerformAccessibilityActionResult(z, i2);
        } catch (RemoteException e) {
        }
    }

    public void findAccessibilityNodeInfosByViewId(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) {
        try {
            iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult((AccessibilityNodeInfo) null, i);
        } catch (RemoteException e) {
        }
    }

    public void findAccessibilityNodeInfosByText(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) {
        try {
            iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult((AccessibilityNodeInfo) null, i);
        } catch (RemoteException e) {
        }
    }

    public void findFocus(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) {
        try {
            iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult((AccessibilityNodeInfo) null, i2);
        } catch (RemoteException e) {
        }
    }

    public void focusSearch(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) {
        try {
            iAccessibilityInteractionConnectionCallback.setFindAccessibilityNodeInfoResult((AccessibilityNodeInfo) null, i2);
        } catch (RemoteException e) {
        }
    }

    public static AccessibilityNodeInfo obtainRootAccessibilityNodeInfo() {
        AccessibilityNodeInfo accessibilityNodeInfoObtain = AccessibilityNodeInfo.obtain();
        accessibilityNodeInfoObtain.setSourceNodeId(AccessibilityNodeInfo.ROOT_NODE_ID, -3);
        accessibilityNodeInfoObtain.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK);
        accessibilityNodeInfoObtain.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS);
        accessibilityNodeInfoObtain.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_MOVE_WINDOW);
        accessibilityNodeInfoObtain.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
        accessibilityNodeInfoObtain.setImportantForAccessibility(true);
        accessibilityNodeInfoObtain.setClickable(true);
        accessibilityNodeInfoObtain.setVisibleToUser(true);
        return accessibilityNodeInfoObtain;
    }

    private List<AccessibilityNodeInfo> getNodeList() {
        if (this.mAccessibilityNodeInfoList == null) {
            this.mAccessibilityNodeInfoList = new ArrayList(1);
        }
        AccessibilityNodeInfo accessibilityNodeInfoObtainRootAccessibilityNodeInfo = obtainRootAccessibilityNodeInfo();
        this.mAccessibilityNodeInfoList.clear();
        this.mAccessibilityNodeInfoList.add(accessibilityNodeInfoObtainRootAccessibilityNodeInfo);
        return this.mAccessibilityNodeInfoList;
    }
}
