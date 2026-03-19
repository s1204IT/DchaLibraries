package android.view.accessibility;

import android.os.Build;
import android.util.ArraySet;
import android.util.Log;
import android.util.LongArray;
import android.util.LongSparseArray;
import android.util.SparseArray;
import java.util.ArrayList;
import java.util.List;

public class AccessibilityCache {
    public static final int CACHE_CRITICAL_EVENTS_MASK = 4307005;
    private static final boolean CHECK_INTEGRITY = Build.IS_ENG;
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityCache";
    private final AccessibilityNodeRefresher mAccessibilityNodeRefresher;
    private boolean mIsAllWindowsCached;
    private final Object mLock = new Object();
    private long mAccessibilityFocus = 2147483647L;
    private long mInputFocus = 2147483647L;
    private final SparseArray<AccessibilityWindowInfo> mWindowCache = new SparseArray<>();
    private final SparseArray<LongSparseArray<AccessibilityNodeInfo>> mNodeCache = new SparseArray<>();
    private final SparseArray<AccessibilityWindowInfo> mTempWindowArray = new SparseArray<>();

    public AccessibilityCache(AccessibilityNodeRefresher accessibilityNodeRefresher) {
        this.mAccessibilityNodeRefresher = accessibilityNodeRefresher;
    }

    public void setWindows(List<AccessibilityWindowInfo> list) {
        synchronized (this.mLock) {
            clearWindowCache();
            if (list == null) {
                return;
            }
            int size = list.size();
            for (int i = 0; i < size; i++) {
                addWindow(list.get(i));
            }
            this.mIsAllWindowsCached = true;
        }
    }

    public void addWindow(AccessibilityWindowInfo accessibilityWindowInfo) {
        synchronized (this.mLock) {
            int id = accessibilityWindowInfo.getId();
            AccessibilityWindowInfo accessibilityWindowInfo2 = this.mWindowCache.get(id);
            if (accessibilityWindowInfo2 != null) {
                accessibilityWindowInfo2.recycle();
            }
            this.mWindowCache.put(id, AccessibilityWindowInfo.obtain(accessibilityWindowInfo));
        }
    }

    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        synchronized (this.mLock) {
            switch (accessibilityEvent.getEventType()) {
                case 1:
                case 4:
                case 16:
                case 8192:
                    refreshCachedNodeLocked(accessibilityEvent.getWindowId(), accessibilityEvent.getSourceNodeId());
                    break;
                case 8:
                    if (this.mInputFocus != 2147483647L) {
                        refreshCachedNodeLocked(accessibilityEvent.getWindowId(), this.mInputFocus);
                    }
                    this.mInputFocus = accessibilityEvent.getSourceNodeId();
                    refreshCachedNodeLocked(accessibilityEvent.getWindowId(), this.mInputFocus);
                    break;
                case 32:
                case 4194304:
                    clear();
                    break;
                case 2048:
                    synchronized (this.mLock) {
                        int windowId = accessibilityEvent.getWindowId();
                        long sourceNodeId = accessibilityEvent.getSourceNodeId();
                        if ((accessibilityEvent.getContentChangeTypes() & 1) != 0) {
                            clearSubTreeLocked(windowId, sourceNodeId);
                        } else {
                            refreshCachedNodeLocked(windowId, sourceNodeId);
                        }
                        break;
                    }
                    break;
                case 4096:
                    clearSubTreeLocked(accessibilityEvent.getWindowId(), accessibilityEvent.getSourceNodeId());
                    break;
                case 32768:
                    if (this.mAccessibilityFocus != 2147483647L) {
                        refreshCachedNodeLocked(accessibilityEvent.getWindowId(), this.mAccessibilityFocus);
                    }
                    this.mAccessibilityFocus = accessibilityEvent.getSourceNodeId();
                    refreshCachedNodeLocked(accessibilityEvent.getWindowId(), this.mAccessibilityFocus);
                    break;
                case 65536:
                    if (this.mAccessibilityFocus == accessibilityEvent.getSourceNodeId()) {
                        refreshCachedNodeLocked(accessibilityEvent.getWindowId(), this.mAccessibilityFocus);
                        this.mAccessibilityFocus = 2147483647L;
                    }
                    break;
            }
        }
        if (CHECK_INTEGRITY) {
            checkIntegrity();
        }
    }

    private void refreshCachedNodeLocked(int i, long j) {
        AccessibilityNodeInfo accessibilityNodeInfo;
        LongSparseArray<AccessibilityNodeInfo> longSparseArray = this.mNodeCache.get(i);
        if (longSparseArray == null || (accessibilityNodeInfo = longSparseArray.get(j)) == null || this.mAccessibilityNodeRefresher.refreshNode(accessibilityNodeInfo, true)) {
            return;
        }
        clearSubTreeLocked(i, j);
    }

    public AccessibilityNodeInfo getNode(int i, long j) {
        synchronized (this.mLock) {
            LongSparseArray<AccessibilityNodeInfo> longSparseArray = this.mNodeCache.get(i);
            if (longSparseArray == null) {
                return null;
            }
            AccessibilityNodeInfo accessibilityNodeInfoObtain = longSparseArray.get(j);
            if (accessibilityNodeInfoObtain != null) {
                accessibilityNodeInfoObtain = AccessibilityNodeInfo.obtain(accessibilityNodeInfoObtain);
            }
            return accessibilityNodeInfoObtain;
        }
    }

    public List<AccessibilityWindowInfo> getWindows() {
        synchronized (this.mLock) {
            if (!this.mIsAllWindowsCached) {
                return null;
            }
            int size = this.mWindowCache.size();
            if (size <= 0) {
                return null;
            }
            SparseArray<AccessibilityWindowInfo> sparseArray = this.mTempWindowArray;
            sparseArray.clear();
            for (int i = 0; i < size; i++) {
                AccessibilityWindowInfo accessibilityWindowInfoValueAt = this.mWindowCache.valueAt(i);
                sparseArray.put(accessibilityWindowInfoValueAt.getLayer(), accessibilityWindowInfoValueAt);
            }
            int size2 = sparseArray.size();
            ArrayList arrayList = new ArrayList(size2);
            for (int i2 = size2 - 1; i2 >= 0; i2--) {
                arrayList.add(AccessibilityWindowInfo.obtain(sparseArray.valueAt(i2)));
                sparseArray.removeAt(i2);
            }
            return arrayList;
        }
    }

    public AccessibilityWindowInfo getWindow(int i) {
        synchronized (this.mLock) {
            AccessibilityWindowInfo accessibilityWindowInfo = this.mWindowCache.get(i);
            if (accessibilityWindowInfo != null) {
                return AccessibilityWindowInfo.obtain(accessibilityWindowInfo);
            }
            return null;
        }
    }

    public void add(AccessibilityNodeInfo accessibilityNodeInfo) {
        synchronized (this.mLock) {
            int windowId = accessibilityNodeInfo.getWindowId();
            LongSparseArray<AccessibilityNodeInfo> longSparseArray = this.mNodeCache.get(windowId);
            if (longSparseArray == null) {
                longSparseArray = new LongSparseArray<>();
                this.mNodeCache.put(windowId, longSparseArray);
            }
            long sourceNodeId = accessibilityNodeInfo.getSourceNodeId();
            AccessibilityNodeInfo accessibilityNodeInfo2 = longSparseArray.get(sourceNodeId);
            if (accessibilityNodeInfo2 != null) {
                LongArray childNodeIds = accessibilityNodeInfo.getChildNodeIds();
                int childCount = accessibilityNodeInfo2.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    if (longSparseArray.get(sourceNodeId) == null) {
                        clearNodesForWindowLocked(windowId);
                        return;
                    }
                    long childId = accessibilityNodeInfo2.getChildId(i);
                    if (childNodeIds == null || childNodeIds.indexOf(childId) < 0) {
                        clearSubTreeLocked(windowId, childId);
                    }
                }
                long parentNodeId = accessibilityNodeInfo2.getParentNodeId();
                if (accessibilityNodeInfo.getParentNodeId() != parentNodeId) {
                    clearSubTreeLocked(windowId, parentNodeId);
                } else {
                    accessibilityNodeInfo2.recycle();
                }
            }
            AccessibilityNodeInfo accessibilityNodeInfoObtain = AccessibilityNodeInfo.obtain(accessibilityNodeInfo);
            longSparseArray.put(sourceNodeId, accessibilityNodeInfoObtain);
            if (accessibilityNodeInfoObtain.isAccessibilityFocused()) {
                this.mAccessibilityFocus = sourceNodeId;
            }
            if (accessibilityNodeInfoObtain.isFocused()) {
                this.mInputFocus = sourceNodeId;
            }
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            clearWindowCache();
            int size = this.mNodeCache.size();
            for (int i = 0; i < size; i++) {
                clearNodesForWindowLocked(this.mNodeCache.keyAt(i));
            }
            this.mAccessibilityFocus = 2147483647L;
            this.mInputFocus = 2147483647L;
        }
    }

    private void clearWindowCache() {
        for (int size = this.mWindowCache.size() - 1; size >= 0; size--) {
            this.mWindowCache.valueAt(size).recycle();
            this.mWindowCache.removeAt(size);
        }
        this.mIsAllWindowsCached = false;
    }

    private void clearNodesForWindowLocked(int i) {
        LongSparseArray<AccessibilityNodeInfo> longSparseArray = this.mNodeCache.get(i);
        if (longSparseArray == null) {
            return;
        }
        for (int size = longSparseArray.size() - 1; size >= 0; size--) {
            AccessibilityNodeInfo accessibilityNodeInfoValueAt = longSparseArray.valueAt(size);
            longSparseArray.removeAt(size);
            accessibilityNodeInfoValueAt.recycle();
        }
        this.mNodeCache.remove(i);
    }

    private void clearSubTreeLocked(int i, long j) {
        LongSparseArray<AccessibilityNodeInfo> longSparseArray = this.mNodeCache.get(i);
        if (longSparseArray != null) {
            clearSubTreeRecursiveLocked(longSparseArray, j);
        }
    }

    private void clearSubTreeRecursiveLocked(LongSparseArray<AccessibilityNodeInfo> longSparseArray, long j) {
        AccessibilityNodeInfo accessibilityNodeInfo = longSparseArray.get(j);
        if (accessibilityNodeInfo == null) {
            return;
        }
        longSparseArray.remove(j);
        int childCount = accessibilityNodeInfo.getChildCount();
        for (int i = 0; i < childCount; i++) {
            clearSubTreeRecursiveLocked(longSparseArray, accessibilityNodeInfo.getChildId(i));
        }
        accessibilityNodeInfo.recycle();
    }

    public void checkIntegrity() {
        int i;
        boolean z;
        AccessibilityCache accessibilityCache = this;
        synchronized (accessibilityCache.mLock) {
            if (accessibilityCache.mWindowCache.size() > 0 || accessibilityCache.mNodeCache.size() != 0) {
                int size = accessibilityCache.mWindowCache.size();
                AccessibilityWindowInfo accessibilityWindowInfo = null;
                AccessibilityWindowInfo accessibilityWindowInfo2 = null;
                for (int i2 = 0; i2 < size; i2++) {
                    AccessibilityWindowInfo accessibilityWindowInfoValueAt = accessibilityCache.mWindowCache.valueAt(i2);
                    if (accessibilityWindowInfoValueAt.isActive()) {
                        if (accessibilityWindowInfo != null) {
                            Log.e(LOG_TAG, "Duplicate active window:" + accessibilityWindowInfoValueAt);
                        } else {
                            accessibilityWindowInfo = accessibilityWindowInfoValueAt;
                        }
                    }
                    if (accessibilityWindowInfoValueAt.isFocused()) {
                        if (accessibilityWindowInfo2 == null) {
                            accessibilityWindowInfo2 = accessibilityWindowInfoValueAt;
                        } else {
                            Log.e(LOG_TAG, "Duplicate focused window:" + accessibilityWindowInfoValueAt);
                        }
                    }
                }
                int size2 = accessibilityCache.mNodeCache.size();
                AccessibilityNodeInfo accessibilityNodeInfo = null;
                AccessibilityNodeInfo accessibilityNodeInfo2 = null;
                int i3 = 0;
                while (i3 < size2) {
                    LongSparseArray<AccessibilityNodeInfo> longSparseArrayValueAt = accessibilityCache.mNodeCache.valueAt(i3);
                    if (longSparseArrayValueAt.size() > 0) {
                        ArraySet arraySet = new ArraySet();
                        int iKeyAt = accessibilityCache.mNodeCache.keyAt(i3);
                        int size3 = longSparseArrayValueAt.size();
                        AccessibilityNodeInfo accessibilityNodeInfo3 = accessibilityNodeInfo2;
                        AccessibilityNodeInfo accessibilityNodeInfo4 = accessibilityNodeInfo;
                        int i4 = 0;
                        while (i4 < size3) {
                            AccessibilityNodeInfo accessibilityNodeInfoValueAt = longSparseArrayValueAt.valueAt(i4);
                            if (!arraySet.add(accessibilityNodeInfoValueAt)) {
                                Log.e(LOG_TAG, "Duplicate node: " + accessibilityNodeInfoValueAt + " in window:" + iKeyAt);
                                i = i4;
                            } else {
                                if (accessibilityNodeInfoValueAt.isAccessibilityFocused()) {
                                    if (accessibilityNodeInfo4 != null) {
                                        Log.e(LOG_TAG, "Duplicate accessibility focus:" + accessibilityNodeInfoValueAt + " in window:" + iKeyAt);
                                    } else {
                                        accessibilityNodeInfo4 = accessibilityNodeInfoValueAt;
                                    }
                                }
                                if (accessibilityNodeInfoValueAt.isFocused()) {
                                    if (accessibilityNodeInfo3 != null) {
                                        Log.e(LOG_TAG, "Duplicate input focus: " + accessibilityNodeInfoValueAt + " in window:" + iKeyAt);
                                    } else {
                                        accessibilityNodeInfo3 = accessibilityNodeInfoValueAt;
                                    }
                                }
                                AccessibilityNodeInfo accessibilityNodeInfo5 = longSparseArrayValueAt.get(accessibilityNodeInfoValueAt.getParentNodeId());
                                if (accessibilityNodeInfo5 != null) {
                                    int childCount = accessibilityNodeInfo5.getChildCount();
                                    int i5 = 0;
                                    while (true) {
                                        if (i5 < childCount) {
                                            i = i4;
                                            if (longSparseArrayValueAt.get(accessibilityNodeInfo5.getChildId(i5)) != accessibilityNodeInfoValueAt) {
                                                i5++;
                                                i4 = i;
                                            } else {
                                                z = true;
                                                break;
                                            }
                                        } else {
                                            i = i4;
                                            z = false;
                                            break;
                                        }
                                    }
                                    if (!z) {
                                        Log.e(LOG_TAG, "Invalid parent-child relation between parent: " + accessibilityNodeInfo5 + " and child: " + accessibilityNodeInfoValueAt);
                                    }
                                } else {
                                    i = i4;
                                }
                                int childCount2 = accessibilityNodeInfoValueAt.getChildCount();
                                for (int i6 = 0; i6 < childCount2; i6++) {
                                    AccessibilityNodeInfo accessibilityNodeInfo6 = longSparseArrayValueAt.get(accessibilityNodeInfoValueAt.getChildId(i6));
                                    if (accessibilityNodeInfo6 != null && longSparseArrayValueAt.get(accessibilityNodeInfo6.getParentNodeId()) != accessibilityNodeInfoValueAt) {
                                        Log.e(LOG_TAG, "Invalid child-parent relation between child: " + accessibilityNodeInfoValueAt + " and parent: " + accessibilityNodeInfo5);
                                    }
                                }
                            }
                            i4 = i + 1;
                        }
                        accessibilityNodeInfo = accessibilityNodeInfo4;
                        accessibilityNodeInfo2 = accessibilityNodeInfo3;
                    }
                    i3++;
                    accessibilityCache = this;
                }
            }
        }
    }

    public static class AccessibilityNodeRefresher {
        public boolean refreshNode(AccessibilityNodeInfo accessibilityNodeInfo, boolean z) {
            return accessibilityNodeInfo.refresh(null, z);
        }
    }
}
