package com.android.server.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.annotations.GuardedBy;
import java.util.ArrayList;
import java.util.List;

public class ActionReplacingCallback extends IAccessibilityInteractionConnectionCallback.Stub {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "ActionReplacingCallback";
    private final IAccessibilityInteractionConnection mConnectionWithReplacementActions;

    @GuardedBy("mLock")
    boolean mDone;
    private final int mInteractionId;
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    boolean mMultiNodeCallbackHappened;

    @GuardedBy("mLock")
    AccessibilityNodeInfo mNodeFromOriginalWindow;

    @GuardedBy("mLock")
    List<AccessibilityNodeInfo> mNodesFromOriginalWindow;

    @GuardedBy("mLock")
    List<AccessibilityNodeInfo> mNodesWithReplacementActions;
    private final IAccessibilityInteractionConnectionCallback mServiceCallback;

    @GuardedBy("mLock")
    boolean mSingleNodeCallbackHappened;

    public ActionReplacingCallback(IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, IAccessibilityInteractionConnection iAccessibilityInteractionConnection, int i, int i2, long j) {
        boolean z;
        this.mServiceCallback = iAccessibilityInteractionConnectionCallback;
        this.mConnectionWithReplacementActions = iAccessibilityInteractionConnection;
        this.mInteractionId = i;
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            try {
                z = true;
                try {
                    this.mConnectionWithReplacementActions.findAccessibilityNodeInfoByAccessibilityId(AccessibilityNodeInfo.ROOT_NODE_ID, (Region) null, i + 1, this, 0, i2, j, (MagnificationSpec) null, (Bundle) null);
                } catch (RemoteException e) {
                    this.mMultiNodeCallbackHappened = z;
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        } catch (RemoteException e2) {
            z = true;
        }
    }

    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo accessibilityNodeInfo, int i) {
        synchronized (this.mLock) {
            if (i == this.mInteractionId) {
                this.mNodeFromOriginalWindow = accessibilityNodeInfo;
                this.mSingleNodeCallbackHappened = true;
                boolean z = this.mMultiNodeCallbackHappened;
                if (z) {
                    replaceInfoActionsAndCallService();
                    return;
                }
                return;
            }
            Slog.e(LOG_TAG, "Callback with unexpected interactionId");
        }
    }

    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> list, int i) {
        synchronized (this.mLock) {
            if (i == this.mInteractionId) {
                this.mNodesFromOriginalWindow = list;
            } else if (i == this.mInteractionId + 1) {
                this.mNodesWithReplacementActions = list;
            } else {
                Slog.e(LOG_TAG, "Callback with unexpected interactionId");
                return;
            }
            boolean z = this.mSingleNodeCallbackHappened;
            boolean z2 = this.mMultiNodeCallbackHappened;
            this.mMultiNodeCallbackHappened = true;
            if (z) {
                replaceInfoActionsAndCallService();
            }
            if (z2) {
                replaceInfosActionsAndCallService();
            }
        }
    }

    public void setPerformAccessibilityActionResult(boolean z, int i) throws RemoteException {
        this.mServiceCallback.setPerformAccessibilityActionResult(z, i);
    }

    private void replaceInfoActionsAndCallService() {
        synchronized (this.mLock) {
            if (this.mDone) {
                return;
            }
            if (this.mNodeFromOriginalWindow != null) {
                replaceActionsOnInfoLocked(this.mNodeFromOriginalWindow);
            }
            recycleReplaceActionNodesLocked();
            AccessibilityNodeInfo accessibilityNodeInfo = this.mNodeFromOriginalWindow;
            this.mDone = true;
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfoResult(accessibilityNodeInfo, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void replaceInfosActionsAndCallService() {
        synchronized (this.mLock) {
            if (this.mDone) {
                return;
            }
            if (this.mNodesFromOriginalWindow != null) {
                for (int i = 0; i < this.mNodesFromOriginalWindow.size(); i++) {
                    replaceActionsOnInfoLocked(this.mNodesFromOriginalWindow.get(i));
                }
            }
            recycleReplaceActionNodesLocked();
            ArrayList arrayList = this.mNodesFromOriginalWindow == null ? null : new ArrayList(this.mNodesFromOriginalWindow);
            this.mDone = true;
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfosResult(arrayList, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    @GuardedBy("mLock")
    private void replaceActionsOnInfoLocked(AccessibilityNodeInfo accessibilityNodeInfo) {
        accessibilityNodeInfo.removeAllActions();
        accessibilityNodeInfo.setClickable(false);
        accessibilityNodeInfo.setFocusable(false);
        accessibilityNodeInfo.setContextClickable(false);
        accessibilityNodeInfo.setScrollable(false);
        accessibilityNodeInfo.setLongClickable(false);
        accessibilityNodeInfo.setDismissable(false);
        if (accessibilityNodeInfo.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID && this.mNodesWithReplacementActions != null) {
            for (int i = 0; i < this.mNodesWithReplacementActions.size(); i++) {
                AccessibilityNodeInfo accessibilityNodeInfo2 = this.mNodesWithReplacementActions.get(i);
                if (accessibilityNodeInfo2.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID) {
                    List<AccessibilityNodeInfo.AccessibilityAction> actionList = accessibilityNodeInfo2.getActionList();
                    if (actionList != null) {
                        for (int i2 = 0; i2 < actionList.size(); i2++) {
                            accessibilityNodeInfo.addAction(actionList.get(i2));
                        }
                        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
                        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
                    }
                    accessibilityNodeInfo.setClickable(accessibilityNodeInfo2.isClickable());
                    accessibilityNodeInfo.setFocusable(accessibilityNodeInfo2.isFocusable());
                    accessibilityNodeInfo.setContextClickable(accessibilityNodeInfo2.isContextClickable());
                    accessibilityNodeInfo.setScrollable(accessibilityNodeInfo2.isScrollable());
                    accessibilityNodeInfo.setLongClickable(accessibilityNodeInfo2.isLongClickable());
                    accessibilityNodeInfo.setDismissable(accessibilityNodeInfo2.isDismissable());
                }
            }
        }
    }

    @GuardedBy("mLock")
    private void recycleReplaceActionNodesLocked() {
        if (this.mNodesWithReplacementActions == null) {
            return;
        }
        for (int size = this.mNodesWithReplacementActions.size() - 1; size >= 0; size--) {
            this.mNodesWithReplacementActions.get(size).recycle();
        }
        this.mNodesWithReplacementActions = null;
    }
}
