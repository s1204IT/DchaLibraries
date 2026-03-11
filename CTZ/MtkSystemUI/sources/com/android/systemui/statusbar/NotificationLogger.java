package com.android.systemui.statusbar;

import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.systemui.Dependency;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.statusbar.NotificationData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class NotificationLogger {
    protected NotificationEntryManager mEntryManager;
    private long mLastVisibilityReportUptimeMs;
    private NotificationListContainer mListContainer;
    private final ArraySet<NotificationVisibility> mCurrentlyVisibleNotifications = new ArraySet<>();
    private final NotificationListenerService mNotificationListener = (NotificationListenerService) Dependency.get(NotificationListener.class);
    private final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
    protected Handler mHandler = new Handler();
    protected final OnChildLocationsChangedListener mNotificationLocationsChangedListener = new OnChildLocationsChangedListener() {
        @Override
        public void onChildLocationsChanged() {
            if (!NotificationLogger.this.mHandler.hasCallbacks(NotificationLogger.this.mVisibilityReporter)) {
                NotificationLogger.this.mHandler.postAtTime(NotificationLogger.this.mVisibilityReporter, NotificationLogger.this.mLastVisibilityReportUptimeMs + 500);
            }
        }
    };
    protected final Runnable mVisibilityReporter = new Runnable() {
        private final ArraySet<NotificationVisibility> mTmpNewlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpCurrentlyVisibleNotifications = new ArraySet<>();
        private final ArraySet<NotificationVisibility> mTmpNoLongerVisibleNotifications = new ArraySet<>();

        @Override
        public void run() {
            NotificationLogger.this.mLastVisibilityReportUptimeMs = SystemClock.uptimeMillis();
            ArrayList<NotificationData.Entry> activeNotifications = NotificationLogger.this.mEntryManager.getNotificationData().getActiveNotifications();
            int size = activeNotifications.size();
            for (int i = 0; i < size; i++) {
                NotificationData.Entry entry = activeNotifications.get(i);
                String key = entry.notification.getKey();
                boolean zIsInVisibleLocation = NotificationLogger.this.mListContainer.isInVisibleLocation(entry.row);
                NotificationVisibility notificationVisibilityObtain = NotificationVisibility.obtain(key, i, size, zIsInVisibleLocation);
                boolean zContains = NotificationLogger.this.mCurrentlyVisibleNotifications.contains(notificationVisibilityObtain);
                if (zIsInVisibleLocation) {
                    this.mTmpCurrentlyVisibleNotifications.add(notificationVisibilityObtain);
                    if (!zContains) {
                        this.mTmpNewlyVisibleNotifications.add(notificationVisibilityObtain);
                    }
                } else {
                    notificationVisibilityObtain.recycle();
                }
            }
            this.mTmpNoLongerVisibleNotifications.addAll(NotificationLogger.this.mCurrentlyVisibleNotifications);
            this.mTmpNoLongerVisibleNotifications.removeAll((ArraySet<? extends NotificationVisibility>) this.mTmpCurrentlyVisibleNotifications);
            NotificationLogger.this.logNotificationVisibilityChanges(this.mTmpNewlyVisibleNotifications, this.mTmpNoLongerVisibleNotifications);
            NotificationLogger.this.recycleAllVisibilityObjects((ArraySet<NotificationVisibility>) NotificationLogger.this.mCurrentlyVisibleNotifications);
            NotificationLogger.this.mCurrentlyVisibleNotifications.addAll((ArraySet) this.mTmpCurrentlyVisibleNotifications);
            NotificationLogger.this.recycleAllVisibilityObjects(this.mTmpNoLongerVisibleNotifications);
            this.mTmpCurrentlyVisibleNotifications.clear();
            this.mTmpNewlyVisibleNotifications.clear();
            this.mTmpNoLongerVisibleNotifications.clear();
        }
    };
    protected IStatusBarService mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));

    public interface OnChildLocationsChangedListener {
        void onChildLocationsChanged();
    }

    public void setUpWithEntryManager(NotificationEntryManager notificationEntryManager, NotificationListContainer notificationListContainer) {
        this.mEntryManager = notificationEntryManager;
        this.mListContainer = notificationListContainer;
    }

    public void stopNotificationLogging() {
        if (!this.mCurrentlyVisibleNotifications.isEmpty()) {
            logNotificationVisibilityChanges(Collections.emptyList(), this.mCurrentlyVisibleNotifications);
            recycleAllVisibilityObjects(this.mCurrentlyVisibleNotifications);
        }
        this.mHandler.removeCallbacks(this.mVisibilityReporter);
        this.mListContainer.setChildLocationsChangedListener(null);
    }

    public void startNotificationLogging() {
        this.mListContainer.setChildLocationsChangedListener(this.mNotificationLocationsChangedListener);
        this.mNotificationLocationsChangedListener.onChildLocationsChanged();
    }

    public void logNotificationVisibilityChanges(final Collection<NotificationVisibility> collection, Collection<NotificationVisibility> collection2) {
        if (collection.isEmpty() && collection2.isEmpty()) {
            return;
        }
        final NotificationVisibility[] notificationVisibilityArrCloneVisibilitiesAsArr = cloneVisibilitiesAsArr(collection);
        final NotificationVisibility[] notificationVisibilityArrCloneVisibilitiesAsArr2 = cloneVisibilitiesAsArr(collection2);
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                NotificationLogger.lambda$logNotificationVisibilityChanges$0(this.f$0, notificationVisibilityArrCloneVisibilitiesAsArr, notificationVisibilityArrCloneVisibilitiesAsArr2, collection);
            }
        });
    }

    public static void lambda$logNotificationVisibilityChanges$0(NotificationLogger notificationLogger, NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2, Collection collection) {
        try {
            notificationLogger.mBarService.onNotificationVisibilityChanged(notificationVisibilityArr, notificationVisibilityArr2);
        } catch (RemoteException e) {
        }
        int size = collection.size();
        if (size > 0) {
            String[] strArr = new String[size];
            for (int i = 0; i < size; i++) {
                strArr[i] = notificationVisibilityArr[i].key;
            }
            try {
                notificationLogger.mNotificationListener.setNotificationsShown(strArr);
            } catch (RuntimeException e2) {
                Log.d("NotificationLogger", "failed setNotificationsShown: ", e2);
            }
        }
        notificationLogger.recycleAllVisibilityObjects(notificationVisibilityArr);
        notificationLogger.recycleAllVisibilityObjects(notificationVisibilityArr2);
    }

    public void recycleAllVisibilityObjects(ArraySet<NotificationVisibility> arraySet) {
        int size = arraySet.size();
        for (int i = 0; i < size; i++) {
            arraySet.valueAt(i).recycle();
        }
        arraySet.clear();
    }

    private void recycleAllVisibilityObjects(NotificationVisibility[] notificationVisibilityArr) {
        int length = notificationVisibilityArr.length;
        for (int i = 0; i < length; i++) {
            if (notificationVisibilityArr[i] != null) {
                notificationVisibilityArr[i].recycle();
            }
        }
    }

    private NotificationVisibility[] cloneVisibilitiesAsArr(Collection<NotificationVisibility> collection) {
        NotificationVisibility[] notificationVisibilityArr = new NotificationVisibility[collection.size()];
        int i = 0;
        for (NotificationVisibility notificationVisibility : collection) {
            if (notificationVisibility != null) {
                notificationVisibilityArr[i] = notificationVisibility.clone();
            }
            i++;
        }
        return notificationVisibilityArr;
    }

    @VisibleForTesting
    public Runnable getVisibilityReporter() {
        return this.mVisibilityReporter;
    }
}
