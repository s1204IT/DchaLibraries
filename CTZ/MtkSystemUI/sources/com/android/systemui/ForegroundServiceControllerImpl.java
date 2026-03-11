package com.android.systemui;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.SparseArray;
import java.util.Arrays;

public class ForegroundServiceControllerImpl implements ForegroundServiceController {
    private final Context mContext;
    private final SparseArray<UserServices> mUserServices = new SparseArray<>();
    private final Object mMutex = new Object();

    public ForegroundServiceControllerImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isDungeonNeededForUser(int i) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(i);
            if (userServices == null) {
                return false;
            }
            return userServices.isDungeonNeeded();
        }
    }

    @Override
    public boolean isSystemAlertWarningNeeded(int i, String str) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(i);
            if (userServices == null) {
                return false;
            }
            return userServices.getStandardLayoutKey(str) == null;
        }
    }

    @Override
    public String getStandardLayoutKey(int i, String str) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(i);
            if (userServices == null) {
                return null;
            }
            return userServices.getStandardLayoutKey(str);
        }
    }

    @Override
    public ArraySet<Integer> getAppOps(int i, String str) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(i);
            if (userServices == null) {
                return null;
            }
            return userServices.getFeatures(str);
        }
    }

    @Override
    public void onAppOpChanged(int i, int i2, String str, boolean z) {
        int userId = UserHandle.getUserId(i2);
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(userId);
            if (userServices == null) {
                userServices = new UserServices();
                this.mUserServices.put(userId, userServices);
            }
            if (z) {
                userServices.addOp(str, i);
            } else {
                userServices.removeOp(str, i);
            }
        }
    }

    @Override
    public void addNotification(StatusBarNotification statusBarNotification, int i) {
        updateNotification(statusBarNotification, i);
    }

    @Override
    public boolean removeNotification(StatusBarNotification statusBarNotification) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(statusBarNotification.getUserId());
            if (userServices == null) {
                return false;
            }
            if (isDungeonNotification(statusBarNotification)) {
                userServices.setRunningServices(null, 0L);
                return true;
            }
            return userServices.removeNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
        }
    }

    @Override
    public void updateNotification(StatusBarNotification statusBarNotification, int i) {
        synchronized (this.mMutex) {
            UserServices userServices = this.mUserServices.get(statusBarNotification.getUserId());
            if (userServices == null) {
                userServices = new UserServices();
                this.mUserServices.put(statusBarNotification.getUserId(), userServices);
            }
            if (isDungeonNotification(statusBarNotification)) {
                Bundle bundle = statusBarNotification.getNotification().extras;
                if (bundle != null) {
                    userServices.setRunningServices(bundle.getStringArray("android.foregroundApps"), statusBarNotification.getNotification().when);
                }
            } else {
                userServices.removeNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                if ((statusBarNotification.getNotification().flags & 64) != 0) {
                    if (i > 1) {
                        userServices.addImportantNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                    }
                    if (Notification.Builder.recoverBuilder(this.mContext, statusBarNotification.getNotification()).usesStandardHeader()) {
                        userServices.addStandardLayoutNotification(statusBarNotification.getPackageName(), statusBarNotification.getKey());
                    }
                }
            }
        }
    }

    @Override
    public boolean isDungeonNotification(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getId() == 40 && statusBarNotification.getTag() == null && statusBarNotification.getPackageName().equals("android");
    }

    @Override
    public boolean isSystemAlertNotification(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getPackageName().equals("android") && statusBarNotification.getTag() != null && statusBarNotification.getTag().contains("AlertWindowNotification");
    }

    private static class UserServices {
        private ArrayMap<String, ArraySet<Integer>> mAppOps;
        private ArrayMap<String, ArraySet<String>> mImportantNotifications;
        private String[] mRunning;
        private long mServiceStartTime;
        private ArrayMap<String, ArraySet<String>> mStandardLayoutNotifications;

        private UserServices() {
            this.mRunning = null;
            this.mServiceStartTime = 0L;
            this.mImportantNotifications = new ArrayMap<>(1);
            this.mStandardLayoutNotifications = new ArrayMap<>(1);
            this.mAppOps = new ArrayMap<>(1);
        }

        public void setRunningServices(String[] strArr, long j) {
            this.mRunning = strArr != null ? (String[]) Arrays.copyOf(strArr, strArr.length) : null;
            this.mServiceStartTime = j;
        }

        public void addOp(String str, int i) {
            if (this.mAppOps.get(str) == null) {
                this.mAppOps.put(str, new ArraySet<>(3));
            }
            this.mAppOps.get(str).add(Integer.valueOf(i));
        }

        public boolean removeOp(String str, int i) {
            ArraySet<Integer> arraySet = this.mAppOps.get(str);
            if (arraySet == null) {
                return false;
            }
            boolean zRemove = arraySet.remove(Integer.valueOf(i));
            if (arraySet.size() == 0) {
                this.mAppOps.remove(str);
            }
            return zRemove;
        }

        public void addImportantNotification(String str, String str2) {
            addNotification(this.mImportantNotifications, str, str2);
        }

        public boolean removeImportantNotification(String str, String str2) {
            return removeNotification(this.mImportantNotifications, str, str2);
        }

        public void addStandardLayoutNotification(String str, String str2) {
            addNotification(this.mStandardLayoutNotifications, str, str2);
        }

        public boolean removeStandardLayoutNotification(String str, String str2) {
            return removeNotification(this.mStandardLayoutNotifications, str, str2);
        }

        public boolean removeNotification(String str, String str2) {
            return removeStandardLayoutNotification(str, str2) | removeImportantNotification(str, str2) | false;
        }

        public void addNotification(ArrayMap<String, ArraySet<String>> arrayMap, String str, String str2) {
            if (arrayMap.get(str) == null) {
                arrayMap.put(str, new ArraySet<>());
            }
            arrayMap.get(str).add(str2);
        }

        public boolean removeNotification(ArrayMap<String, ArraySet<String>> arrayMap, String str, String str2) {
            ArraySet<String> arraySet = arrayMap.get(str);
            if (arraySet == null) {
                return false;
            }
            boolean zRemove = arraySet.remove(str2);
            if (arraySet.size() == 0) {
                arrayMap.remove(str);
            }
            return zRemove;
        }

        public boolean isDungeonNeeded() {
            if (this.mRunning != null && System.currentTimeMillis() - this.mServiceStartTime >= 5000) {
                for (String str : this.mRunning) {
                    ArraySet<String> arraySet = this.mImportantNotifications.get(str);
                    if (arraySet == null || arraySet.size() == 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        public ArraySet<Integer> getFeatures(String str) {
            return this.mAppOps.get(str);
        }

        public String getStandardLayoutKey(String str) {
            ArraySet<String> arraySet = this.mStandardLayoutNotifications.get(str);
            if (arraySet == null || arraySet.size() == 0) {
                return null;
            }
            return arraySet.valueAt(0);
        }

        public String toString() {
            return "UserServices{mRunning=" + Arrays.toString(this.mRunning) + ", mServiceStartTime=" + this.mServiceStartTime + ", mImportantNotifications=" + this.mImportantNotifications + ", mStandardLayoutNotifications=" + this.mStandardLayoutNotifications + '}';
        }
    }
}
