package android.view;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.LongSparseArray;
import android.view.View;
import android.view.accessibility.AccessibilityInteractionClient;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.os.SomeArgs;
import com.android.internal.util.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

final class AccessibilityInteractionController {
    private static final boolean ENFORCE_NODE_TREE_CONSISTENT = false;
    private AddNodeInfosForViewId mAddNodeInfosForViewId;
    private final Handler mHandler;
    private final long mMyLooperThreadId;
    private final int mMyProcessId;
    private final AccessibilityNodePrefetcher mPrefetcher;
    private final ArrayList<AccessibilityNodeInfo> mTempAccessibilityNodeInfoList = new ArrayList<>();
    private final ArrayList<View> mTempArrayList = new ArrayList<>();
    private final Point mTempPoint = new Point();
    private final Rect mTempRect = new Rect();
    private final Rect mTempRect1 = new Rect();
    private final Rect mTempRect2 = new Rect();
    private final ViewRootImpl mViewRootImpl;

    public AccessibilityInteractionController(ViewRootImpl viewRootImpl) {
        Looper looper = viewRootImpl.mHandler.getLooper();
        this.mMyLooperThreadId = looper.getThread().getId();
        this.mMyProcessId = Process.myPid();
        this.mHandler = new PrivateHandler(looper);
        this.mViewRootImpl = viewRootImpl;
        this.mPrefetcher = new AccessibilityNodePrefetcher();
    }

    private boolean isShown(View view) {
        return view.mAttachInfo != null && view.mAttachInfo.mWindowVisibility == 0 && view.isShown();
    }

    public void findAccessibilityNodeInfoByAccessibilityIdClientThread(long accessibilityNodeId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        Message message = this.mHandler.obtainMessage();
        message.what = 2;
        message.arg1 = flags;
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi2 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        message.obj = args;
        if (interrogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void findAccessibilityNodeInfoByAccessibilityIdUiThread(Message message) {
        View root;
        int flags = message.arg1;
        SomeArgs args = (SomeArgs) message.obj;
        int accessibilityViewId = args.argi1;
        int virtualDescendantId = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        Region interactiveRegion = (Region) args.arg3;
        args.recycle();
        List<AccessibilityNodeInfo> infos = this.mTempAccessibilityNodeInfoList;
        infos.clear();
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                if (accessibilityViewId == Integer.MAX_VALUE) {
                    root = this.mViewRootImpl.mView;
                } else {
                    root = findViewByAccessibilityId(accessibilityViewId);
                }
                if (root != null && isShown(root)) {
                    this.mPrefetcher.prefetchAccessibilityNodeInfos(root, virtualDescendantId, flags, infos);
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                    if (spec != null) {
                        spec.recycle();
                    }
                    adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                    callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
                    infos.clear();
                } catch (RemoteException e) {
                }
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
                infos.clear();
            } catch (RemoteException e2) {
            }
        }
    }

    public void findAccessibilityNodeInfosByViewIdClientThread(long accessibilityNodeId, String viewId, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        Message message = this.mHandler.obtainMessage();
        message.what = 3;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = viewId;
        args.arg4 = interactiveRegion;
        message.obj = args;
        if (interrogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void findAccessibilityNodeInfosByViewIdUiThread(Message message) {
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int interactionId = args.argi1;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        String viewId = (String) args.arg3;
        Region interactiveRegion = (Region) args.arg4;
        args.recycle();
        List<AccessibilityNodeInfo> infos = this.mTempAccessibilityNodeInfoList;
        infos.clear();
        try {
            if (this.mViewRootImpl.mView == null || this.mViewRootImpl.mAttachInfo == null) {
                try {
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
            View root = accessibilityViewId != Integer.MAX_VALUE ? findViewByAccessibilityId(accessibilityViewId) : this.mViewRootImpl.mView;
            if (root != null) {
                int resolvedViewId = root.getContext().getResources().getIdentifier(viewId, null, null);
                if (resolvedViewId <= 0) {
                    try {
                        this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                        applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                        if (spec != null) {
                            spec.recycle();
                        }
                        adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                        callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
                        return;
                    } catch (RemoteException e2) {
                        return;
                    }
                }
                if (this.mAddNodeInfosForViewId == null) {
                    this.mAddNodeInfosForViewId = new AddNodeInfosForViewId();
                }
                this.mAddNodeInfosForViewId.init(resolvedViewId, infos);
                root.findViewByPredicate(this.mAddNodeInfosForViewId);
                this.mAddNodeInfosForViewId.reset();
            }
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
            } catch (RemoteException e3) {
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
            } catch (RemoteException e4) {
            }
        }
    }

    public void findAccessibilityNodeInfosByTextClientThread(long accessibilityNodeId, String text, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) {
        Message message = this.mHandler.obtainMessage();
        message.what = 4;
        message.arg1 = flags;
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = text;
        args.arg2 = callback;
        args.arg3 = spec;
        args.argi1 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi2 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi3 = interactionId;
        args.arg4 = interactiveRegion;
        message.obj = args;
        if (interrogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void findAccessibilityNodeInfosByTextUiThread(Message message) {
        View root;
        int flags = message.arg1;
        SomeArgs args = (SomeArgs) message.obj;
        String text = (String) args.arg1;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg2;
        MagnificationSpec spec = (MagnificationSpec) args.arg3;
        int accessibilityViewId = args.argi1;
        int virtualDescendantId = args.argi2;
        int interactionId = args.argi3;
        Region interactiveRegion = (Region) args.arg4;
        args.recycle();
        List<AccessibilityNodeInfo> infos = null;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                if (accessibilityViewId != Integer.MAX_VALUE) {
                    root = findViewByAccessibilityId(accessibilityViewId);
                } else {
                    root = this.mViewRootImpl.mView;
                }
                if (root != null && isShown(root)) {
                    AccessibilityNodeProvider provider = root.getAccessibilityNodeProvider();
                    if (provider != null) {
                        infos = virtualDescendantId != Integer.MAX_VALUE ? provider.findAccessibilityNodeInfosByText(text, virtualDescendantId) : provider.findAccessibilityNodeInfosByText(text, -1);
                    } else if (virtualDescendantId == Integer.MAX_VALUE) {
                        ArrayList<View> foundViews = this.mTempArrayList;
                        foundViews.clear();
                        root.findViewsWithText(foundViews, text, 7);
                        if (!foundViews.isEmpty()) {
                            infos = this.mTempAccessibilityNodeInfoList;
                            infos.clear();
                            int viewCount = foundViews.size();
                            for (int i = 0; i < viewCount; i++) {
                                View foundView = foundViews.get(i);
                                if (isShown(foundView)) {
                                    AccessibilityNodeProvider provider2 = foundView.getAccessibilityNodeProvider();
                                    if (provider2 != null) {
                                        List<AccessibilityNodeInfo> infosFromProvider = provider2.findAccessibilityNodeInfosByText(text, -1);
                                        if (infosFromProvider != null) {
                                            infos.addAll(infosFromProvider);
                                        }
                                    } else {
                                        infos.add(foundView.createAccessibilityNodeInfo());
                                    }
                                }
                            }
                        }
                    }
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    applyAppScaleAndMagnificationSpecIfNeeded(infos, spec);
                    if (spec != null) {
                        spec.recycle();
                    }
                    adjustIsVisibleToUserIfNeeded(infos, interactiveRegion);
                    callback.setFindAccessibilityNodeInfosResult(infos, interactionId);
                } catch (RemoteException e) {
                }
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded((List<AccessibilityNodeInfo>) null, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded((List<AccessibilityNodeInfo>) null, interactiveRegion);
                callback.setFindAccessibilityNodeInfosResult(null, interactionId);
            } catch (RemoteException e2) {
            }
        }
    }

    public void findFocusClientThread(long accessibilityNodeId, int focusType, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interogatingPid, long interrogatingTid, MagnificationSpec spec) {
        Message message = this.mHandler.obtainMessage();
        message.what = 5;
        message.arg1 = flags;
        message.arg2 = focusType;
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = interactionId;
        args.argi2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        args.argi3 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        message.obj = args;
        if (interogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void findFocusUiThread(Message message) {
        View root;
        int flags = message.arg1;
        int focusType = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int interactionId = args.argi1;
        int accessibilityViewId = args.argi2;
        int virtualDescendantId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        Region interactiveRegion = (Region) args.arg3;
        args.recycle();
        AccessibilityNodeInfo focused = null;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                if (accessibilityViewId != Integer.MAX_VALUE) {
                    root = findViewByAccessibilityId(accessibilityViewId);
                } else {
                    root = this.mViewRootImpl.mView;
                }
                if (root != null && isShown(root)) {
                    switch (focusType) {
                        case 1:
                            View target = root.findFocus();
                            if (target != null && isShown(target)) {
                                AccessibilityNodeProvider provider = target.getAccessibilityNodeProvider();
                                if (provider != null) {
                                    focused = provider.findFocus(focusType);
                                }
                                if (focused == null) {
                                    focused = target.createAccessibilityNodeInfo();
                                }
                            }
                            break;
                        case 2:
                            View host = this.mViewRootImpl.mAccessibilityFocusedHost;
                            if (host != null && ViewRootImpl.isViewDescendantOf(host, root) && isShown(host)) {
                                if (host.getAccessibilityNodeProvider() != null) {
                                    if (this.mViewRootImpl.mAccessibilityFocusedVirtualView != null) {
                                        focused = AccessibilityNodeInfo.obtain(this.mViewRootImpl.mAccessibilityFocusedVirtualView);
                                    }
                                } else if (virtualDescendantId == Integer.MAX_VALUE) {
                                    focused = host.createAccessibilityNodeInfo();
                                }
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown focus type: " + focusType);
                    }
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    applyAppScaleAndMagnificationSpecIfNeeded(focused, spec);
                    if (spec != null) {
                        spec.recycle();
                    }
                    adjustIsVisibleToUserIfNeeded(focused, interactiveRegion);
                    callback.setFindAccessibilityNodeInfoResult(focused, interactionId);
                } catch (RemoteException e) {
                }
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded((AccessibilityNodeInfo) null, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded((AccessibilityNodeInfo) null, interactiveRegion);
                callback.setFindAccessibilityNodeInfoResult(null, interactionId);
            } catch (RemoteException e2) {
            }
        }
    }

    public void focusSearchClientThread(long accessibilityNodeId, int direction, Region interactiveRegion, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interogatingPid, long interrogatingTid, MagnificationSpec spec) {
        Message message = this.mHandler.obtainMessage();
        message.what = 6;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi2 = direction;
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = spec;
        args.arg3 = interactiveRegion;
        message.obj = args;
        if (interogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void focusSearchUiThread(Message message) {
        View root;
        View nextView;
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int direction = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        MagnificationSpec spec = (MagnificationSpec) args.arg2;
        Region interactiveRegion = (Region) args.arg3;
        args.recycle();
        AccessibilityNodeInfo next = null;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                if (accessibilityViewId != Integer.MAX_VALUE) {
                    root = findViewByAccessibilityId(accessibilityViewId);
                } else {
                    root = this.mViewRootImpl.mView;
                }
                if (root != null && isShown(root) && (nextView = root.focusSearch(direction)) != null) {
                    next = nextView.createAccessibilityNodeInfo();
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    applyAppScaleAndMagnificationSpecIfNeeded(next, spec);
                    if (spec != null) {
                        spec.recycle();
                    }
                    adjustIsVisibleToUserIfNeeded(next, interactiveRegion);
                    callback.setFindAccessibilityNodeInfoResult(next, interactionId);
                } catch (RemoteException e) {
                }
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                applyAppScaleAndMagnificationSpecIfNeeded((AccessibilityNodeInfo) null, spec);
                if (spec != null) {
                    spec.recycle();
                }
                adjustIsVisibleToUserIfNeeded((AccessibilityNodeInfo) null, interactiveRegion);
                callback.setFindAccessibilityNodeInfoResult(null, interactionId);
            } catch (RemoteException e2) {
            }
        }
    }

    public void performAccessibilityActionClientThread(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interogatingPid, long interrogatingTid) {
        Message message = this.mHandler.obtainMessage();
        message.what = 1;
        message.arg1 = flags;
        message.arg2 = AccessibilityNodeInfo.getAccessibilityViewId(accessibilityNodeId);
        SomeArgs args = SomeArgs.obtain();
        args.argi1 = AccessibilityNodeInfo.getVirtualDescendantId(accessibilityNodeId);
        args.argi2 = action;
        args.argi3 = interactionId;
        args.arg1 = callback;
        args.arg2 = arguments;
        message.obj = args;
        if (interogatingPid == this.mMyProcessId && interrogatingTid == this.mMyLooperThreadId) {
            AccessibilityInteractionClient.getInstanceForThread(interrogatingTid).setSameThreadMessage(message);
        } else {
            this.mHandler.sendMessage(message);
        }
    }

    private void perfromAccessibilityActionUiThread(Message message) {
        View target;
        int flags = message.arg1;
        int accessibilityViewId = message.arg2;
        SomeArgs args = (SomeArgs) message.obj;
        int virtualDescendantId = args.argi1;
        int action = args.argi2;
        int interactionId = args.argi3;
        IAccessibilityInteractionConnectionCallback callback = (IAccessibilityInteractionConnectionCallback) args.arg1;
        Bundle arguments = (Bundle) args.arg2;
        args.recycle();
        boolean succeeded = false;
        try {
            if (this.mViewRootImpl.mView != null && this.mViewRootImpl.mAttachInfo != null) {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = flags;
                if (accessibilityViewId != Integer.MAX_VALUE) {
                    target = findViewByAccessibilityId(accessibilityViewId);
                } else {
                    target = this.mViewRootImpl.mView;
                }
                if (target != null && isShown(target)) {
                    AccessibilityNodeProvider provider = target.getAccessibilityNodeProvider();
                    if (provider != null) {
                        succeeded = virtualDescendantId != Integer.MAX_VALUE ? provider.performAction(virtualDescendantId, action, arguments) : provider.performAction(-1, action, arguments);
                    } else if (virtualDescendantId == Integer.MAX_VALUE) {
                        succeeded = target.performAccessibilityAction(action, arguments);
                    }
                }
                try {
                    this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                    callback.setPerformAccessibilityActionResult(succeeded, interactionId);
                } catch (RemoteException e) {
                }
            }
        } finally {
            try {
                this.mViewRootImpl.mAttachInfo.mAccessibilityFetchFlags = 0;
                callback.setPerformAccessibilityActionResult(false, interactionId);
            } catch (RemoteException e2) {
            }
        }
    }

    private View findViewByAccessibilityId(int accessibilityId) {
        View root = this.mViewRootImpl.mView;
        if (root == null) {
            return null;
        }
        View foundView = root.findViewByAccessibilityId(accessibilityId);
        if (foundView == null || isShown(foundView)) {
            return foundView;
        }
        return null;
    }

    private void applyAppScaleAndMagnificationSpecIfNeeded(List<AccessibilityNodeInfo> infos, MagnificationSpec spec) {
        if (infos != null) {
            float applicationScale = this.mViewRootImpl.mAttachInfo.mApplicationScale;
            if (shouldApplyAppScaleAndMagnificationSpec(applicationScale, spec)) {
                int infoCount = infos.size();
                for (int i = 0; i < infoCount; i++) {
                    AccessibilityNodeInfo info = infos.get(i);
                    applyAppScaleAndMagnificationSpecIfNeeded(info, spec);
                }
            }
        }
    }

    private void adjustIsVisibleToUserIfNeeded(List<AccessibilityNodeInfo> infos, Region interactiveRegion) {
        if (interactiveRegion != null && infos != null) {
            int infoCount = infos.size();
            for (int i = 0; i < infoCount; i++) {
                AccessibilityNodeInfo info = infos.get(i);
                adjustIsVisibleToUserIfNeeded(info, interactiveRegion);
            }
        }
    }

    private void adjustIsVisibleToUserIfNeeded(AccessibilityNodeInfo info, Region interactiveRegion) {
        if (interactiveRegion != null && info != null) {
            Rect boundsInScreen = this.mTempRect;
            info.getBoundsInScreen(boundsInScreen);
            if (interactiveRegion.quickReject(boundsInScreen)) {
                info.setVisibleToUser(false);
            }
        }
    }

    private void applyAppScaleAndMagnificationSpecIfNeeded(Point point, MagnificationSpec spec) {
        float applicationScale = this.mViewRootImpl.mAttachInfo.mApplicationScale;
        if (shouldApplyAppScaleAndMagnificationSpec(applicationScale, spec)) {
            if (applicationScale != 1.0f) {
                point.x = (int) (point.x * applicationScale);
                point.y = (int) (point.y * applicationScale);
            }
            if (spec != null) {
                point.x = (int) (point.x * spec.scale);
                point.y = (int) (point.y * spec.scale);
                point.x += (int) spec.offsetX;
                point.y += (int) spec.offsetY;
            }
        }
    }

    private void applyAppScaleAndMagnificationSpecIfNeeded(AccessibilityNodeInfo info, MagnificationSpec spec) {
        if (info != null) {
            float applicationScale = this.mViewRootImpl.mAttachInfo.mApplicationScale;
            if (shouldApplyAppScaleAndMagnificationSpec(applicationScale, spec)) {
                Rect boundsInParent = this.mTempRect;
                Rect boundsInScreen = this.mTempRect1;
                info.getBoundsInParent(boundsInParent);
                info.getBoundsInScreen(boundsInScreen);
                if (applicationScale != 1.0f) {
                    boundsInParent.scale(applicationScale);
                    boundsInScreen.scale(applicationScale);
                }
                if (spec != null) {
                    boundsInParent.scale(spec.scale);
                    boundsInScreen.scale(spec.scale);
                    boundsInScreen.offset((int) spec.offsetX, (int) spec.offsetY);
                }
                info.setBoundsInParent(boundsInParent);
                info.setBoundsInScreen(boundsInScreen);
                if (spec != null) {
                    View.AttachInfo attachInfo = this.mViewRootImpl.mAttachInfo;
                    if (attachInfo.mDisplay != null) {
                        float scale = attachInfo.mApplicationScale * spec.scale;
                        Rect visibleWinFrame = this.mTempRect1;
                        visibleWinFrame.left = (int) ((attachInfo.mWindowLeft * scale) + spec.offsetX);
                        visibleWinFrame.top = (int) ((attachInfo.mWindowTop * scale) + spec.offsetY);
                        visibleWinFrame.right = (int) (visibleWinFrame.left + (this.mViewRootImpl.mWidth * scale));
                        visibleWinFrame.bottom = (int) (visibleWinFrame.top + (this.mViewRootImpl.mHeight * scale));
                        attachInfo.mDisplay.getRealSize(this.mTempPoint);
                        int displayWidth = this.mTempPoint.x;
                        int displayHeight = this.mTempPoint.y;
                        Rect visibleDisplayFrame = this.mTempRect2;
                        visibleDisplayFrame.set(0, 0, displayWidth, displayHeight);
                        visibleWinFrame.intersect(visibleDisplayFrame);
                        if (!visibleWinFrame.intersects(boundsInScreen.left, boundsInScreen.top, boundsInScreen.right, boundsInScreen.bottom)) {
                            info.setVisibleToUser(false);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldApplyAppScaleAndMagnificationSpec(float appScale, MagnificationSpec spec) {
        return (appScale == 1.0f && (spec == null || spec.isNop())) ? false : true;
    }

    private class AccessibilityNodePrefetcher {
        private static final int MAX_ACCESSIBILITY_NODE_INFO_BATCH_SIZE = 50;
        private final ArrayList<View> mTempViewList;

        private AccessibilityNodePrefetcher() {
            this.mTempViewList = new ArrayList<>();
        }

        public void prefetchAccessibilityNodeInfos(View view, int virtualViewId, int fetchFlags, List<AccessibilityNodeInfo> outInfos) {
            AccessibilityNodeInfo root;
            AccessibilityNodeProvider provider = view.getAccessibilityNodeProvider();
            if (provider == null) {
                AccessibilityNodeInfo root2 = view.createAccessibilityNodeInfo();
                if (root2 != null) {
                    outInfos.add(root2);
                    if ((fetchFlags & 1) != 0) {
                        prefetchPredecessorsOfRealNode(view, outInfos);
                    }
                    if ((fetchFlags & 2) != 0) {
                        prefetchSiblingsOfRealNode(view, outInfos);
                    }
                    if ((fetchFlags & 4) != 0) {
                        prefetchDescendantsOfRealNode(view, outInfos);
                        return;
                    }
                    return;
                }
                return;
            }
            if (virtualViewId != Integer.MAX_VALUE) {
                root = provider.createAccessibilityNodeInfo(virtualViewId);
            } else {
                root = provider.createAccessibilityNodeInfo(-1);
            }
            if (root != null) {
                outInfos.add(root);
                if ((fetchFlags & 1) != 0) {
                    prefetchPredecessorsOfVirtualNode(root, view, provider, outInfos);
                }
                if ((fetchFlags & 2) != 0) {
                    prefetchSiblingsOfVirtualNode(root, view, provider, outInfos);
                }
                if ((fetchFlags & 4) != 0) {
                    prefetchDescendantsOfVirtualNode(root, provider, outInfos);
                }
            }
        }

        private void enforceNodeTreeConsistent(List<AccessibilityNodeInfo> nodes) {
            LongSparseArray<AccessibilityNodeInfo> nodeMap = new LongSparseArray<>();
            int nodeCount = nodes.size();
            for (int i = 0; i < nodeCount; i++) {
                AccessibilityNodeInfo node = nodes.get(i);
                nodeMap.put(node.getSourceNodeId(), node);
            }
            AccessibilityNodeInfo root = nodeMap.valueAt(0);
            for (AccessibilityNodeInfo parent = root; parent != null; parent = nodeMap.get(parent.getParentNodeId())) {
                root = parent;
            }
            AccessibilityNodeInfo accessFocus = null;
            AccessibilityNodeInfo inputFocus = null;
            HashSet<AccessibilityNodeInfo> seen = new HashSet<>();
            Queue<AccessibilityNodeInfo> fringe = new LinkedList<>();
            fringe.add(root);
            while (!fringe.isEmpty()) {
                AccessibilityNodeInfo current = fringe.poll();
                if (!seen.add(current)) {
                    throw new IllegalStateException("Duplicate node: " + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                }
                if (current.isAccessibilityFocused()) {
                    if (accessFocus != null) {
                        throw new IllegalStateException("Duplicate accessibility focus:" + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                    }
                    accessFocus = current;
                }
                if (current.isFocused()) {
                    if (inputFocus != null) {
                        throw new IllegalStateException("Duplicate input focus: " + current + " in window:" + AccessibilityInteractionController.this.mViewRootImpl.mAttachInfo.mAccessibilityWindowId);
                    }
                    inputFocus = current;
                }
                int childCount = current.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    long childId = current.getChildId(j);
                    AccessibilityNodeInfo child = nodeMap.get(childId);
                    if (child != null) {
                        fringe.add(child);
                    }
                }
            }
            for (int j2 = nodeMap.size() - 1; j2 >= 0; j2--) {
                AccessibilityNodeInfo info = nodeMap.valueAt(j2);
                if (!seen.contains(info)) {
                    throw new IllegalStateException("Disconnected node: " + info);
                }
            }
        }

        private void prefetchPredecessorsOfRealNode(View view, List<AccessibilityNodeInfo> outInfos) {
            for (ViewParent parent = view.getParentForAccessibility(); (parent instanceof View) && outInfos.size() < 50; parent = parent.getParentForAccessibility()) {
                View parentView = (View) parent;
                AccessibilityNodeInfo info = parentView.createAccessibilityNodeInfo();
                if (info != null) {
                    outInfos.add(info);
                }
            }
        }

        private void prefetchSiblingsOfRealNode(View current, List<AccessibilityNodeInfo> outInfos) {
            AccessibilityNodeInfo info;
            ViewParent parent = current.getParentForAccessibility();
            if (parent instanceof ViewGroup) {
                ViewGroup parentGroup = (ViewGroup) parent;
                ArrayList<View> children = this.mTempViewList;
                children.clear();
                try {
                    parentGroup.addChildrenForAccessibility(children);
                    int childCount = children.size();
                    for (int i = 0; i < childCount; i++) {
                        if (outInfos.size() < 50) {
                            View child = children.get(i);
                            if (child.getAccessibilityViewId() != current.getAccessibilityViewId() && AccessibilityInteractionController.this.isShown(child)) {
                                AccessibilityNodeProvider provider = child.getAccessibilityNodeProvider();
                                if (provider == null) {
                                    info = child.createAccessibilityNodeInfo();
                                } else {
                                    info = provider.createAccessibilityNodeInfo(-1);
                                }
                                if (info != null) {
                                    outInfos.add(info);
                                }
                            }
                        } else {
                            return;
                        }
                    }
                } finally {
                    children.clear();
                }
            }
        }

        private void prefetchDescendantsOfRealNode(View root, List<AccessibilityNodeInfo> outInfos) {
            if (root instanceof ViewGroup) {
                HashMap<View, AccessibilityNodeInfo> addedChildren = new HashMap<>();
                ArrayList<View> children = this.mTempViewList;
                children.clear();
                try {
                    root.addChildrenForAccessibility(children);
                    int childCount = children.size();
                    for (int i = 0; i < childCount; i++) {
                        if (outInfos.size() < 50) {
                            View child = children.get(i);
                            if (AccessibilityInteractionController.this.isShown(child)) {
                                AccessibilityNodeProvider provider = child.getAccessibilityNodeProvider();
                                if (provider == null) {
                                    AccessibilityNodeInfo info = child.createAccessibilityNodeInfo();
                                    if (info != null) {
                                        outInfos.add(info);
                                        addedChildren.put(child, null);
                                    }
                                } else {
                                    AccessibilityNodeInfo info2 = provider.createAccessibilityNodeInfo(-1);
                                    if (info2 != null) {
                                        outInfos.add(info2);
                                        addedChildren.put(child, info2);
                                    }
                                }
                            }
                        } else {
                            return;
                        }
                    }
                    children.clear();
                    if (outInfos.size() < 50) {
                        for (Map.Entry<View, AccessibilityNodeInfo> entry : addedChildren.entrySet()) {
                            View addedChild = entry.getKey();
                            AccessibilityNodeInfo virtualRoot = entry.getValue();
                            if (virtualRoot == null) {
                                prefetchDescendantsOfRealNode(addedChild, outInfos);
                            } else {
                                prefetchDescendantsOfVirtualNode(virtualRoot, addedChild.getAccessibilityNodeProvider(), outInfos);
                            }
                        }
                    }
                } finally {
                    children.clear();
                }
            }
        }

        private void prefetchPredecessorsOfVirtualNode(AccessibilityNodeInfo root, View providerHost, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos) {
            AccessibilityNodeInfo parent;
            long parentNodeId = root.getParentNodeId();
            int accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
            while (accessibilityViewId != Integer.MAX_VALUE && outInfos.size() < 50) {
                int virtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(parentNodeId);
                if (virtualDescendantId != Integer.MAX_VALUE || accessibilityViewId == providerHost.getAccessibilityViewId()) {
                    if (virtualDescendantId != Integer.MAX_VALUE) {
                        parent = provider.createAccessibilityNodeInfo(virtualDescendantId);
                    } else {
                        parent = provider.createAccessibilityNodeInfo(-1);
                    }
                    if (parent != null) {
                        outInfos.add(parent);
                        parentNodeId = parent.getParentNodeId();
                        accessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
                    } else {
                        return;
                    }
                } else {
                    prefetchPredecessorsOfRealNode(providerHost, outInfos);
                    return;
                }
            }
        }

        private void prefetchSiblingsOfVirtualNode(AccessibilityNodeInfo current, View providerHost, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos) {
            AccessibilityNodeInfo parent;
            long parentNodeId = current.getParentNodeId();
            int parentAccessibilityViewId = AccessibilityNodeInfo.getAccessibilityViewId(parentNodeId);
            int parentVirtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(parentNodeId);
            if (parentVirtualDescendantId != Integer.MAX_VALUE || parentAccessibilityViewId == providerHost.getAccessibilityViewId()) {
                if (parentVirtualDescendantId != Integer.MAX_VALUE) {
                    parent = provider.createAccessibilityNodeInfo(parentVirtualDescendantId);
                } else {
                    parent = provider.createAccessibilityNodeInfo(-1);
                }
                if (parent != null) {
                    int childCount = parent.getChildCount();
                    for (int i = 0; i < childCount && outInfos.size() < 50; i++) {
                        long childNodeId = parent.getChildId(i);
                        if (childNodeId != current.getSourceNodeId()) {
                            int childVirtualDescendantId = AccessibilityNodeInfo.getVirtualDescendantId(childNodeId);
                            AccessibilityNodeInfo child = provider.createAccessibilityNodeInfo(childVirtualDescendantId);
                            if (child != null) {
                                outInfos.add(child);
                            }
                        }
                    }
                    return;
                }
                return;
            }
            prefetchSiblingsOfRealNode(providerHost, outInfos);
        }

        private void prefetchDescendantsOfVirtualNode(AccessibilityNodeInfo root, AccessibilityNodeProvider provider, List<AccessibilityNodeInfo> outInfos) {
            int initialOutInfosSize = outInfos.size();
            int childCount = root.getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (outInfos.size() < 50) {
                    long childNodeId = root.getChildId(i);
                    AccessibilityNodeInfo child = provider.createAccessibilityNodeInfo(AccessibilityNodeInfo.getVirtualDescendantId(childNodeId));
                    if (child != null) {
                        outInfos.add(child);
                    }
                } else {
                    return;
                }
            }
            if (outInfos.size() < 50) {
                int addedChildCount = outInfos.size() - initialOutInfosSize;
                for (int i2 = 0; i2 < addedChildCount; i2++) {
                    prefetchDescendantsOfVirtualNode(outInfos.get(initialOutInfosSize + i2), provider, outInfos);
                }
            }
        }
    }

    private class PrivateHandler extends Handler {
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFOS_BY_VIEW_ID = 3;
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_ACCESSIBILITY_ID = 2;
        private static final int MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_TEXT = 4;
        private static final int MSG_FIND_FOCUS = 5;
        private static final int MSG_FOCUS_SEARCH = 6;
        private static final int MSG_PERFORM_ACCESSIBILITY_ACTION = 1;

        public PrivateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public String getMessageName(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    return "MSG_PERFORM_ACCESSIBILITY_ACTION";
                case 2:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_ACCESSIBILITY_ID";
                case 3:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFOS_BY_VIEW_ID";
                case 4:
                    return "MSG_FIND_ACCESSIBILITY_NODE_INFO_BY_TEXT";
                case 5:
                    return "MSG_FIND_FOCUS";
                case 6:
                    return "MSG_FOCUS_SEARCH";
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }

        @Override
        public void handleMessage(Message message) {
            int type = message.what;
            switch (type) {
                case 1:
                    AccessibilityInteractionController.this.perfromAccessibilityActionUiThread(message);
                    return;
                case 2:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfoByAccessibilityIdUiThread(message);
                    return;
                case 3:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfosByViewIdUiThread(message);
                    return;
                case 4:
                    AccessibilityInteractionController.this.findAccessibilityNodeInfosByTextUiThread(message);
                    return;
                case 5:
                    AccessibilityInteractionController.this.findFocusUiThread(message);
                    return;
                case 6:
                    AccessibilityInteractionController.this.focusSearchUiThread(message);
                    return;
                default:
                    throw new IllegalArgumentException("Unknown message type: " + type);
            }
        }
    }

    private final class AddNodeInfosForViewId implements Predicate<View> {
        private List<AccessibilityNodeInfo> mInfos;
        private int mViewId;

        private AddNodeInfosForViewId() {
            this.mViewId = -1;
        }

        public void init(int viewId, List<AccessibilityNodeInfo> infos) {
            this.mViewId = viewId;
            this.mInfos = infos;
        }

        public void reset() {
            this.mViewId = -1;
            this.mInfos = null;
        }

        @Override
        public boolean apply(View view) {
            if (view.getId() == this.mViewId && AccessibilityInteractionController.this.isShown(view)) {
                this.mInfos.add(view.createAccessibilityNodeInfo());
                return false;
            }
            return false;
        }
    }
}
