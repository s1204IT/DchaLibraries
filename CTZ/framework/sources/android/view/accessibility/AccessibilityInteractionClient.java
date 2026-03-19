package android.view.accessibility;

import android.accessibilityservice.IAccessibilityServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityCache;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class AccessibilityInteractionClient extends IAccessibilityInteractionConnectionCallback.Stub {
    private static final boolean CHECK_INTEGRITY = true;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityInteractionClient";
    public static final int NO_ID = -1;
    private static final long TIMEOUT_INTERACTION_MILLIS = 5000;
    private AccessibilityNodeInfo mFindAccessibilityNodeInfoResult;
    private List<AccessibilityNodeInfo> mFindAccessibilityNodeInfosResult;
    private boolean mPerformAccessibilityActionResult;
    private Message mSameThreadMessage;
    private static final Object sStaticLock = new Object();
    private static final LongSparseArray<AccessibilityInteractionClient> sClients = new LongSparseArray<>();
    private static final SparseArray<IAccessibilityServiceConnection> sConnectionCache = new SparseArray<>();
    private static AccessibilityCache sAccessibilityCache = new AccessibilityCache(new AccessibilityCache.AccessibilityNodeRefresher());
    private final AtomicInteger mInteractionIdCounter = new AtomicInteger();
    private final Object mInstanceLock = new Object();
    private volatile int mInteractionId = -1;

    public static AccessibilityInteractionClient getInstance() {
        return getInstanceForThread(Thread.currentThread().getId());
    }

    public static AccessibilityInteractionClient getInstanceForThread(long j) {
        AccessibilityInteractionClient accessibilityInteractionClient;
        synchronized (sStaticLock) {
            accessibilityInteractionClient = sClients.get(j);
            if (accessibilityInteractionClient == null) {
                accessibilityInteractionClient = new AccessibilityInteractionClient();
                sClients.put(j, accessibilityInteractionClient);
            }
        }
        return accessibilityInteractionClient;
    }

    public static IAccessibilityServiceConnection getConnection(int i) {
        IAccessibilityServiceConnection iAccessibilityServiceConnection;
        synchronized (sConnectionCache) {
            iAccessibilityServiceConnection = sConnectionCache.get(i);
        }
        return iAccessibilityServiceConnection;
    }

    public static void addConnection(int i, IAccessibilityServiceConnection iAccessibilityServiceConnection) {
        synchronized (sConnectionCache) {
            sConnectionCache.put(i, iAccessibilityServiceConnection);
        }
    }

    public static void removeConnection(int i) {
        synchronized (sConnectionCache) {
            sConnectionCache.remove(i);
        }
    }

    @VisibleForTesting
    public static void setCache(AccessibilityCache accessibilityCache) {
        sAccessibilityCache = accessibilityCache;
    }

    private AccessibilityInteractionClient() {
    }

    public void setSameThreadMessage(Message message) {
        synchronized (this.mInstanceLock) {
            this.mSameThreadMessage = message;
            this.mInstanceLock.notifyAll();
        }
    }

    public AccessibilityNodeInfo getRootInActiveWindow(int i) {
        return findAccessibilityNodeInfoByAccessibilityId(i, Integer.MAX_VALUE, AccessibilityNodeInfo.ROOT_NODE_ID, false, 4, null);
    }

    public AccessibilityWindowInfo getWindow(int i, int i2) {
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                AccessibilityWindowInfo window = sAccessibilityCache.getWindow(i2);
                if (window != null) {
                    return window;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    AccessibilityWindowInfo window2 = connection.getWindow(i2);
                    if (window2 != null) {
                        sAccessibilityCache.addWindow(window2);
                        return window2;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while calling remote getWindow", e);
            return null;
        }
    }

    public List<AccessibilityWindowInfo> getWindows(int i) {
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                List<AccessibilityWindowInfo> windows = sAccessibilityCache.getWindows();
                if (windows != null) {
                    return windows;
                }
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    List<AccessibilityWindowInfo> windows2 = connection.getWindows();
                    if (windows2 != null) {
                        sAccessibilityCache.setWindows(windows2);
                        return windows2;
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while calling remote getWindows", e);
        }
        return Collections.emptyList();
    }

    public AccessibilityNodeInfo findAccessibilityNodeInfoByAccessibilityId(int i, int i2, long j, boolean z, int i3, Bundle bundle) {
        int i4;
        long j2;
        if ((i3 & 2) != 0 && (i3 & 1) == 0) {
            throw new IllegalArgumentException("FLAG_PREFETCH_SIBLINGS requires FLAG_PREFETCH_PREDECESSORS");
        }
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                if (!z) {
                    i4 = i2;
                    j2 = j;
                    AccessibilityNodeInfo node = sAccessibilityCache.getNode(i4, j2);
                    if (node != null) {
                        return node;
                    }
                } else {
                    i4 = i2;
                    j2 = j;
                }
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String[] strArrFindAccessibilityNodeInfoByAccessibilityId = connection.findAccessibilityNodeInfoByAccessibilityId(i4, j2, andIncrement, this, i3, Thread.currentThread().getId(), bundle);
                    if (strArrFindAccessibilityNodeInfoByAccessibilityId != null) {
                        List<AccessibilityNodeInfo> findAccessibilityNodeInfosResultAndClear = getFindAccessibilityNodeInfosResultAndClear(andIncrement);
                        finalizeAndCacheAccessibilityNodeInfos(findAccessibilityNodeInfosResultAndClear, i, z, strArrFindAccessibilityNodeInfoByAccessibilityId);
                        if (findAccessibilityNodeInfosResultAndClear != null && !findAccessibilityNodeInfosResultAndClear.isEmpty()) {
                            for (int i5 = 1; i5 < findAccessibilityNodeInfosResultAndClear.size(); i5++) {
                                findAccessibilityNodeInfosResultAndClear.get(i5).recycle();
                            }
                            return findAccessibilityNodeInfosResultAndClear.get(0);
                        }
                        return null;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByAccessibilityId", e);
            return null;
        }
    }

    private static String idToString(int i, long j) {
        return i + "/" + AccessibilityNodeInfo.idToString(j);
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByViewId(int i, int i2, long j, String str) {
        List<AccessibilityNodeInfo> findAccessibilityNodeInfosResultAndClear;
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String[] strArrFindAccessibilityNodeInfosByViewId = connection.findAccessibilityNodeInfosByViewId(i2, j, str, andIncrement, this, Thread.currentThread().getId());
                    if (strArrFindAccessibilityNodeInfosByViewId != null && (findAccessibilityNodeInfosResultAndClear = getFindAccessibilityNodeInfosResultAndClear(andIncrement)) != null) {
                        finalizeAndCacheAccessibilityNodeInfos(findAccessibilityNodeInfosResultAndClear, i, false, strArrFindAccessibilityNodeInfosByViewId);
                        return findAccessibilityNodeInfosResultAndClear;
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfoByViewIdInActiveWindow", e);
        }
        return Collections.emptyList();
    }

    public List<AccessibilityNodeInfo> findAccessibilityNodeInfosByText(int i, int i2, long j, String str) {
        List<AccessibilityNodeInfo> findAccessibilityNodeInfosResultAndClear;
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String[] strArrFindAccessibilityNodeInfosByText = connection.findAccessibilityNodeInfosByText(i2, j, str, andIncrement, this, Thread.currentThread().getId());
                    if (strArrFindAccessibilityNodeInfosByText != null && (findAccessibilityNodeInfosResultAndClear = getFindAccessibilityNodeInfosResultAndClear(andIncrement)) != null) {
                        finalizeAndCacheAccessibilityNodeInfos(findAccessibilityNodeInfosResultAndClear, i, false, strArrFindAccessibilityNodeInfosByText);
                        return findAccessibilityNodeInfosResultAndClear;
                    }
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Error while calling remote findAccessibilityNodeInfosByViewText", e);
        }
        return Collections.emptyList();
    }

    public AccessibilityNodeInfo findFocus(int i, int i2, long j, int i3) {
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String[] strArrFindFocus = connection.findFocus(i2, j, i3, andIncrement, this, Thread.currentThread().getId());
                    if (strArrFindFocus != null) {
                        AccessibilityNodeInfo findAccessibilityNodeInfoResultAndClear = getFindAccessibilityNodeInfoResultAndClear(andIncrement);
                        finalizeAndCacheAccessibilityNodeInfo(findAccessibilityNodeInfoResultAndClear, i, false, strArrFindFocus);
                        return findAccessibilityNodeInfoResultAndClear;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Error while calling remote findFocus", e);
            return null;
        }
    }

    public AccessibilityNodeInfo focusSearch(int i, int i2, long j, int i3) {
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    String[] strArrFocusSearch = connection.focusSearch(i2, j, i3, andIncrement, this, Thread.currentThread().getId());
                    if (strArrFocusSearch != null) {
                        AccessibilityNodeInfo findAccessibilityNodeInfoResultAndClear = getFindAccessibilityNodeInfoResultAndClear(andIncrement);
                        finalizeAndCacheAccessibilityNodeInfo(findAccessibilityNodeInfoResultAndClear, i, false, strArrFocusSearch);
                        return findAccessibilityNodeInfoResultAndClear;
                    }
                    return null;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return null;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Error while calling remote accessibilityFocusSearch", e);
            return null;
        }
    }

    public boolean performAccessibilityAction(int i, int i2, long j, int i3, Bundle bundle) {
        try {
            IAccessibilityServiceConnection connection = getConnection(i);
            if (connection != null) {
                int andIncrement = this.mInteractionIdCounter.getAndIncrement();
                long jClearCallingIdentity = Binder.clearCallingIdentity();
                try {
                    if (connection.performAccessibilityAction(i2, j, i3, bundle, andIncrement, this, Thread.currentThread().getId())) {
                        return getPerformAccessibilityActionResultAndClear(andIncrement);
                    }
                    return false;
                } finally {
                    Binder.restoreCallingIdentity(jClearCallingIdentity);
                }
            }
            return false;
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Error while calling remote performAccessibilityAction", e);
            return false;
        }
    }

    public void clearCache() {
        sAccessibilityCache.clear();
    }

    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        sAccessibilityCache.onAccessibilityEvent(accessibilityEvent);
    }

    private AccessibilityNodeInfo getFindAccessibilityNodeInfoResultAndClear(int i) {
        AccessibilityNodeInfo accessibilityNodeInfo;
        synchronized (this.mInstanceLock) {
            accessibilityNodeInfo = waitForResultTimedLocked(i) ? this.mFindAccessibilityNodeInfoResult : null;
            clearResultLocked();
        }
        return accessibilityNodeInfo;
    }

    @Override
    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo accessibilityNodeInfo, int i) {
        synchronized (this.mInstanceLock) {
            if (i > this.mInteractionId) {
                this.mFindAccessibilityNodeInfoResult = accessibilityNodeInfo;
                this.mInteractionId = i;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private List<AccessibilityNodeInfo> getFindAccessibilityNodeInfosResultAndClear(int i) {
        List<AccessibilityNodeInfo> listEmptyList;
        synchronized (this.mInstanceLock) {
            if (waitForResultTimedLocked(i)) {
                listEmptyList = this.mFindAccessibilityNodeInfosResult;
            } else {
                listEmptyList = Collections.emptyList();
            }
            clearResultLocked();
            if (Build.IS_DEBUGGABLE) {
                checkFindAccessibilityNodeInfoResultIntegrity(listEmptyList);
            }
        }
        return listEmptyList;
    }

    @Override
    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> list, int i) {
        synchronized (this.mInstanceLock) {
            if (i > this.mInteractionId) {
                if (list != null) {
                    if (!(Binder.getCallingPid() != Process.myPid())) {
                        this.mFindAccessibilityNodeInfosResult = new ArrayList(list);
                    } else {
                        this.mFindAccessibilityNodeInfosResult = list;
                    }
                } else {
                    this.mFindAccessibilityNodeInfosResult = Collections.emptyList();
                }
                this.mInteractionId = i;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private boolean getPerformAccessibilityActionResultAndClear(int i) {
        boolean z;
        synchronized (this.mInstanceLock) {
            z = waitForResultTimedLocked(i) ? this.mPerformAccessibilityActionResult : false;
            clearResultLocked();
        }
        return z;
    }

    @Override
    public void setPerformAccessibilityActionResult(boolean z, int i) {
        synchronized (this.mInstanceLock) {
            if (i > this.mInteractionId) {
                this.mPerformAccessibilityActionResult = z;
                this.mInteractionId = i;
            }
            this.mInstanceLock.notifyAll();
        }
    }

    private void clearResultLocked() {
        this.mInteractionId = -1;
        this.mFindAccessibilityNodeInfoResult = null;
        this.mFindAccessibilityNodeInfosResult = null;
        this.mPerformAccessibilityActionResult = false;
    }

    private boolean waitForResultTimedLocked(int i) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        while (true) {
            try {
                Message sameProcessMessageAndClear = getSameProcessMessageAndClear();
                if (sameProcessMessageAndClear != null) {
                    sameProcessMessageAndClear.getTarget().handleMessage(sameProcessMessageAndClear);
                }
            } catch (InterruptedException e) {
            }
            if (this.mInteractionId == i) {
                return true;
            }
            if (this.mInteractionId > i) {
                return false;
            }
            long jUptimeMillis2 = 5000 - (SystemClock.uptimeMillis() - jUptimeMillis);
            if (jUptimeMillis2 <= 0) {
                return false;
            }
            this.mInstanceLock.wait(jUptimeMillis2);
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo, int i, boolean z, String[] strArr) {
        CharSequence packageName;
        if (accessibilityNodeInfo != null) {
            accessibilityNodeInfo.setConnectionId(i);
            if (!ArrayUtils.isEmpty(strArr) && ((packageName = accessibilityNodeInfo.getPackageName()) == null || !ArrayUtils.contains(strArr, packageName.toString()))) {
                accessibilityNodeInfo.setPackageName(strArr[0]);
            }
            accessibilityNodeInfo.setSealed(true);
            if (!z) {
                sAccessibilityCache.add(accessibilityNodeInfo);
            }
        }
    }

    private void finalizeAndCacheAccessibilityNodeInfos(List<AccessibilityNodeInfo> list, int i, boolean z, String[] strArr) {
        if (list != null) {
            int size = list.size();
            for (int i2 = 0; i2 < size; i2++) {
                finalizeAndCacheAccessibilityNodeInfo(list.get(i2), i, z, strArr);
            }
        }
    }

    private Message getSameProcessMessageAndClear() {
        Message message;
        synchronized (this.mInstanceLock) {
            message = this.mSameThreadMessage;
            this.mSameThreadMessage = null;
        }
        return message;
    }

    private void checkFindAccessibilityNodeInfoResultIntegrity(List<AccessibilityNodeInfo> list) {
        if (list.size() == 0) {
            return;
        }
        AccessibilityNodeInfo accessibilityNodeInfo = list.get(0);
        int size = list.size();
        for (int i = 1; i < size; i++) {
            int i2 = i;
            while (true) {
                if (i2 < size) {
                    AccessibilityNodeInfo accessibilityNodeInfo2 = list.get(i2);
                    if (accessibilityNodeInfo.getParentNodeId() == accessibilityNodeInfo2.getSourceNodeId()) {
                        accessibilityNodeInfo = accessibilityNodeInfo2;
                        break;
                    }
                    i2++;
                }
            }
        }
        if (accessibilityNodeInfo == null) {
            Log.e(LOG_TAG, "No root.");
        }
        HashSet hashSet = new HashSet();
        LinkedList linkedList = new LinkedList();
        linkedList.add(accessibilityNodeInfo);
        while (!linkedList.isEmpty()) {
            AccessibilityNodeInfo accessibilityNodeInfo3 = (AccessibilityNodeInfo) linkedList.poll();
            if (!hashSet.add(accessibilityNodeInfo3)) {
                Log.e(LOG_TAG, "Duplicate node.");
                return;
            }
            int childCount = accessibilityNodeInfo3.getChildCount();
            for (int i3 = 0; i3 < childCount; i3++) {
                long childId = accessibilityNodeInfo3.getChildId(i3);
                for (int i4 = 0; i4 < size; i4++) {
                    AccessibilityNodeInfo accessibilityNodeInfo4 = list.get(i4);
                    if (accessibilityNodeInfo4.getSourceNodeId() == childId) {
                        linkedList.add(accessibilityNodeInfo4);
                    }
                }
            }
        }
        int size2 = list.size() - hashSet.size();
        if (size2 > 0) {
            Log.e(LOG_TAG, size2 + " Disconnected nodes.");
        }
    }
}
