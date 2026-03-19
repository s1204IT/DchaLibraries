package com.android.server.usage;

import android.app.PendingIntent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class AppTimeLimitController {
    private static final boolean DEBUG = false;
    private static final long MAX_OBSERVER_PER_UID = 1000;
    private static final long ONE_MINUTE = 60000;
    private static final String TAG = "AppTimeLimitController";
    private final MyHandler mHandler;
    private OnLimitReachedListener mListener;
    private final Lock mLock = new Lock();

    @GuardedBy("mLock")
    private final SparseArray<UserData> mUsers = new SparseArray<>();

    public interface OnLimitReachedListener {
        void onLimitReached(int i, int i2, long j, long j2, PendingIntent pendingIntent);
    }

    private static class Lock {
        private Lock() {
        }
    }

    private static class UserData {
        private String currentForegroundedPackage;
        private long currentForegroundedTime;
        private SparseArray<TimeLimitGroup> groups;
        private SparseIntArray observerIdCounts;
        private ArrayMap<String, ArrayList<TimeLimitGroup>> packageMap;
        private int userId;

        private UserData(int i) {
            this.packageMap = new ArrayMap<>();
            this.groups = new SparseArray<>();
            this.observerIdCounts = new SparseIntArray();
            this.userId = i;
        }
    }

    static class TimeLimitGroup {
        PendingIntent callbackIntent;
        String currentPackage;
        int observerId;
        String[] packages;
        int requestingUid;
        long timeCurrentPackageStarted;
        long timeLimit;
        long timeRemaining;
        long timeRequested;
        int userId;

        TimeLimitGroup() {
        }
    }

    private class MyHandler extends Handler {
        static final int MSG_CHECK_TIMEOUT = 1;
        static final int MSG_INFORM_LISTENER = 2;

        MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    AppTimeLimitController.this.checkTimeout((TimeLimitGroup) message.obj);
                    break;
                case 2:
                    AppTimeLimitController.this.informListener((TimeLimitGroup) message.obj);
                    break;
                default:
                    super.handleMessage(message);
                    break;
            }
        }
    }

    public AppTimeLimitController(OnLimitReachedListener onLimitReachedListener, Looper looper) {
        this.mHandler = new MyHandler(looper);
        this.mListener = onLimitReachedListener;
    }

    @VisibleForTesting
    protected long getUptimeMillis() {
        return SystemClock.uptimeMillis();
    }

    @VisibleForTesting
    protected long getObserverPerUidLimit() {
        return 1000L;
    }

    @VisibleForTesting
    protected long getMinTimeLimit() {
        return 60000L;
    }

    private UserData getOrCreateUserDataLocked(int i) {
        UserData userData = this.mUsers.get(i);
        if (userData == null) {
            UserData userData2 = new UserData(i);
            this.mUsers.put(i, userData2);
            return userData2;
        }
        return userData;
    }

    public void onUserRemoved(int i) {
        synchronized (this.mLock) {
            this.mUsers.remove(i);
        }
    }

    public void addObserver(int i, int i2, String[] strArr, long j, PendingIntent pendingIntent, int i3) {
        if (j < getMinTimeLimit()) {
            throw new IllegalArgumentException("Time limit must be >= " + getMinTimeLimit());
        }
        synchronized (this.mLock) {
            UserData orCreateUserDataLocked = getOrCreateUserDataLocked(i3);
            removeObserverLocked(orCreateUserDataLocked, i, i2, true);
            int i4 = orCreateUserDataLocked.observerIdCounts.get(i, 0);
            if (i4 < getObserverPerUidLimit()) {
                orCreateUserDataLocked.observerIdCounts.put(i, i4 + 1);
                TimeLimitGroup timeLimitGroup = new TimeLimitGroup();
                timeLimitGroup.observerId = i2;
                timeLimitGroup.callbackIntent = pendingIntent;
                timeLimitGroup.packages = strArr;
                timeLimitGroup.timeLimit = j;
                timeLimitGroup.timeRemaining = timeLimitGroup.timeLimit;
                timeLimitGroup.timeRequested = getUptimeMillis();
                timeLimitGroup.requestingUid = i;
                timeLimitGroup.timeCurrentPackageStarted = -1L;
                timeLimitGroup.userId = i3;
                orCreateUserDataLocked.groups.append(i2, timeLimitGroup);
                addGroupToPackageMapLocked(orCreateUserDataLocked, strArr, timeLimitGroup);
                if (orCreateUserDataLocked.currentForegroundedPackage != null && inPackageList(timeLimitGroup.packages, orCreateUserDataLocked.currentForegroundedPackage)) {
                    timeLimitGroup.timeCurrentPackageStarted = timeLimitGroup.timeRequested;
                    timeLimitGroup.currentPackage = orCreateUserDataLocked.currentForegroundedPackage;
                    if (timeLimitGroup.timeRemaining > 0) {
                        postCheckTimeoutLocked(timeLimitGroup, timeLimitGroup.timeRemaining);
                    }
                }
            } else {
                throw new IllegalStateException("Too many observers added by uid " + i);
            }
        }
    }

    public void removeObserver(int i, int i2, int i3) {
        synchronized (this.mLock) {
            removeObserverLocked(getOrCreateUserDataLocked(i3), i, i2, false);
        }
    }

    @VisibleForTesting
    TimeLimitGroup getObserverGroup(int i, int i2) {
        TimeLimitGroup timeLimitGroup;
        synchronized (this.mLock) {
            timeLimitGroup = (TimeLimitGroup) getOrCreateUserDataLocked(i2).groups.get(i);
        }
        return timeLimitGroup;
    }

    private static boolean inPackageList(String[] strArr, String str) {
        return ArrayUtils.contains(strArr, str);
    }

    @GuardedBy("mLock")
    private void removeObserverLocked(UserData userData, int i, int i2, boolean z) {
        TimeLimitGroup timeLimitGroup = (TimeLimitGroup) userData.groups.get(i2);
        if (timeLimitGroup != null && timeLimitGroup.requestingUid == i) {
            removeGroupFromPackageMapLocked(userData, timeLimitGroup);
            userData.groups.remove(i2);
            this.mHandler.removeMessages(1, timeLimitGroup);
            int i3 = userData.observerIdCounts.get(i);
            if (i3 > 1 || z) {
                userData.observerIdCounts.put(i, i3 - 1);
            } else {
                userData.observerIdCounts.delete(i);
            }
        }
    }

    public void moveToForeground(String str, String str2, int i) {
        synchronized (this.mLock) {
            UserData orCreateUserDataLocked = getOrCreateUserDataLocked(i);
            orCreateUserDataLocked.currentForegroundedPackage = str;
            orCreateUserDataLocked.currentForegroundedTime = getUptimeMillis();
            maybeWatchForPackageLocked(orCreateUserDataLocked, str, orCreateUserDataLocked.currentForegroundedTime);
        }
    }

    public void moveToBackground(String str, String str2, int i) {
        synchronized (this.mLock) {
            UserData orCreateUserDataLocked = getOrCreateUserDataLocked(i);
            if (!TextUtils.equals(orCreateUserDataLocked.currentForegroundedPackage, str)) {
                Slog.w(TAG, "Eh? Last foregrounded package = " + orCreateUserDataLocked.currentForegroundedPackage + " and now backgrounded = " + str);
                return;
            }
            long uptimeMillis = getUptimeMillis();
            ArrayList arrayList = (ArrayList) orCreateUserDataLocked.packageMap.get(str);
            if (arrayList != null) {
                int size = arrayList.size();
                for (int i2 = 0; i2 < size; i2++) {
                    TimeLimitGroup timeLimitGroup = (TimeLimitGroup) arrayList.get(i2);
                    if (timeLimitGroup.timeRemaining > 0) {
                        timeLimitGroup.timeRemaining -= uptimeMillis - Math.max(orCreateUserDataLocked.currentForegroundedTime, timeLimitGroup.timeRequested);
                        if (timeLimitGroup.timeRemaining <= 0) {
                            postInformListenerLocked(timeLimitGroup);
                        }
                        timeLimitGroup.currentPackage = null;
                        timeLimitGroup.timeCurrentPackageStarted = -1L;
                        this.mHandler.removeMessages(1, timeLimitGroup);
                    }
                }
            }
            orCreateUserDataLocked.currentForegroundedPackage = null;
        }
    }

    private void postInformListenerLocked(TimeLimitGroup timeLimitGroup) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2, timeLimitGroup));
    }

    private void informListener(TimeLimitGroup timeLimitGroup) {
        if (this.mListener != null) {
            this.mListener.onLimitReached(timeLimitGroup.observerId, timeLimitGroup.userId, timeLimitGroup.timeLimit, timeLimitGroup.timeLimit - timeLimitGroup.timeRemaining, timeLimitGroup.callbackIntent);
        }
        synchronized (this.mLock) {
            removeObserverLocked(getOrCreateUserDataLocked(timeLimitGroup.userId), timeLimitGroup.requestingUid, timeLimitGroup.observerId, false);
        }
    }

    @GuardedBy("mLock")
    private void maybeWatchForPackageLocked(UserData userData, String str, long j) {
        ArrayList arrayList = (ArrayList) userData.packageMap.get(str);
        if (arrayList == null) {
            return;
        }
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            TimeLimitGroup timeLimitGroup = (TimeLimitGroup) arrayList.get(i);
            if (timeLimitGroup.timeRemaining > 0) {
                timeLimitGroup.timeCurrentPackageStarted = j;
                timeLimitGroup.currentPackage = str;
                postCheckTimeoutLocked(timeLimitGroup, timeLimitGroup.timeRemaining);
            }
        }
    }

    private void addGroupToPackageMapLocked(UserData userData, String[] strArr, TimeLimitGroup timeLimitGroup) {
        for (int i = 0; i < strArr.length; i++) {
            ArrayList arrayList = (ArrayList) userData.packageMap.get(strArr[i]);
            if (arrayList == null) {
                arrayList = new ArrayList();
                userData.packageMap.put(strArr[i], arrayList);
            }
            arrayList.add(timeLimitGroup);
        }
    }

    private void removeGroupFromPackageMapLocked(UserData userData, TimeLimitGroup timeLimitGroup) {
        int size = userData.packageMap.size();
        for (int i = 0; i < size; i++) {
            ((ArrayList) userData.packageMap.valueAt(i)).remove(timeLimitGroup);
        }
    }

    private void postCheckTimeoutLocked(TimeLimitGroup timeLimitGroup, long j) {
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1, timeLimitGroup), j);
    }

    void checkTimeout(TimeLimitGroup timeLimitGroup) {
        synchronized (this.mLock) {
            UserData orCreateUserDataLocked = getOrCreateUserDataLocked(timeLimitGroup.userId);
            if (orCreateUserDataLocked.groups.get(timeLimitGroup.observerId) != timeLimitGroup) {
                return;
            }
            if (timeLimitGroup.timeRemaining <= 0) {
                return;
            }
            if (inPackageList(timeLimitGroup.packages, orCreateUserDataLocked.currentForegroundedPackage)) {
                if (timeLimitGroup.timeCurrentPackageStarted < 0) {
                    Slog.w(TAG, "startTime was not set correctly for " + timeLimitGroup);
                }
                long uptimeMillis = getUptimeMillis() - timeLimitGroup.timeCurrentPackageStarted;
                if (timeLimitGroup.timeRemaining <= uptimeMillis) {
                    timeLimitGroup.timeRemaining -= uptimeMillis;
                    postInformListenerLocked(timeLimitGroup);
                    timeLimitGroup.timeCurrentPackageStarted = -1L;
                    timeLimitGroup.currentPackage = null;
                } else {
                    postCheckTimeoutLocked(timeLimitGroup, timeLimitGroup.timeRemaining - uptimeMillis);
                }
            }
        }
    }

    void dump(PrintWriter printWriter) {
        synchronized (this.mLock) {
            printWriter.println("\n  App Time Limits");
            int size = this.mUsers.size();
            for (int i = 0; i < size; i++) {
                UserData userDataValueAt = this.mUsers.valueAt(i);
                printWriter.print("   User ");
                printWriter.println(userDataValueAt.userId);
                int size2 = userDataValueAt.groups.size();
                for (int i2 = 0; i2 < size2; i2++) {
                    TimeLimitGroup timeLimitGroup = (TimeLimitGroup) userDataValueAt.groups.valueAt(i2);
                    printWriter.print("    Group id=");
                    printWriter.print(timeLimitGroup.observerId);
                    printWriter.print(" timeLimit=");
                    printWriter.print(timeLimitGroup.timeLimit);
                    printWriter.print(" remaining=");
                    printWriter.print(timeLimitGroup.timeRemaining);
                    printWriter.print(" currentPackage=");
                    printWriter.print(timeLimitGroup.currentPackage);
                    printWriter.print(" timeCurrentPkgStarted=");
                    printWriter.print(timeLimitGroup.timeCurrentPackageStarted);
                    printWriter.print(" packages=");
                    printWriter.println(Arrays.toString(timeLimitGroup.packages));
                }
                printWriter.println();
                printWriter.print("    currentForegroundedPackage=");
                printWriter.println(userDataValueAt.currentForegroundedPackage);
            }
        }
    }
}
