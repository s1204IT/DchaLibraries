package android.service.notification;

import android.app.INotificationManager;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.INotificationListener;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import java.util.Collections;
import java.util.List;

public abstract class NotificationListenerService extends Service {
    public static final int HINT_HOST_DISABLE_EFFECTS = 1;
    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final String SERVICE_INTERFACE = "android.service.notification.NotificationListenerService";
    public static final int TRIM_FULL = 0;
    public static final int TRIM_LIGHT = 1;
    private int mCurrentUser;
    private INotificationManager mNoMan;
    private RankingMap mRankingMap;
    private Context mSystemContext;
    private final String TAG = NotificationListenerService.class.getSimpleName() + "[" + getClass().getSimpleName() + "]";
    private INotificationListenerWrapper mWrapper = null;

    public void onNotificationPosted(StatusBarNotification sbn) {
    }

    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationPosted(sbn);
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap) {
        onNotificationRemoved(sbn);
    }

    public void onListenerConnected() {
    }

    public void onNotificationRankingUpdate(RankingMap rankingMap) {
    }

    public void onListenerHintsChanged(int hints) {
    }

    public void onInterruptionFilterChanged(int interruptionFilter) {
    }

    private final INotificationManager getNotificationInterface() {
        if (this.mNoMan == null) {
            this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }
        return this.mNoMan;
    }

    public final void cancelNotification(String pkg, String tag, int id) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationFromListener(this.mWrapper, pkg, tag, id);
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public final void cancelNotification(String key) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationsFromListener(this.mWrapper, new String[]{key});
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public final void cancelAllNotifications() {
        cancelNotifications(null);
    }

    public final void cancelNotifications(String[] keys) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationsFromListener(this.mWrapper, keys);
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public final void setOnNotificationPostedTrim(int trim) {
        if (isBound()) {
            try {
                getNotificationInterface().setOnNotificationPostedTrimFromListener(this.mWrapper, trim);
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public StatusBarNotification[] getActiveNotifications() {
        return getActiveNotifications(null, 0);
    }

    public StatusBarNotification[] getActiveNotifications(int trim) {
        return getActiveNotifications(null, trim);
    }

    public StatusBarNotification[] getActiveNotifications(String[] keys) {
        return getActiveNotifications(keys, 0);
    }

    public StatusBarNotification[] getActiveNotifications(String[] keys, int trim) {
        if (!isBound()) {
            return null;
        }
        try {
            ParceledListSlice<StatusBarNotification> parceledList = getNotificationInterface().getActiveNotificationsFromListener(this.mWrapper, keys, trim);
            List list = parceledList.getList();
            int N = list.size();
            for (int i = 0; i < N; i++) {
                Notification notification = ((StatusBarNotification) list.get(i)).getNotification();
                Notification.Builder.rebuild(getContext(), notification);
            }
            return (StatusBarNotification[]) list.toArray(new StatusBarNotification[N]);
        } catch (RemoteException ex) {
            Log.v(this.TAG, "Unable to contact notification manager", ex);
            return null;
        }
    }

    public final int getCurrentListenerHints() {
        if (!isBound()) {
            return 0;
        }
        try {
            return getNotificationInterface().getHintsFromListener(this.mWrapper);
        } catch (RemoteException ex) {
            Log.v(this.TAG, "Unable to contact notification manager", ex);
            return 0;
        }
    }

    public final int getCurrentInterruptionFilter() {
        if (!isBound()) {
            return 0;
        }
        try {
            return getNotificationInterface().getInterruptionFilterFromListener(this.mWrapper);
        } catch (RemoteException ex) {
            Log.v(this.TAG, "Unable to contact notification manager", ex);
            return 0;
        }
    }

    public final void requestListenerHints(int hints) {
        if (isBound()) {
            try {
                getNotificationInterface().requestHintsFromListener(this.mWrapper, hints);
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public final void requestInterruptionFilter(int interruptionFilter) {
        if (isBound()) {
            try {
                getNotificationInterface().requestInterruptionFilterFromListener(this.mWrapper, interruptionFilter);
            } catch (RemoteException ex) {
                Log.v(this.TAG, "Unable to contact notification manager", ex);
            }
        }
    }

    public RankingMap getCurrentRanking() {
        return this.mRankingMap;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (this.mWrapper == null) {
            this.mWrapper = new INotificationListenerWrapper();
        }
        return this.mWrapper;
    }

    private boolean isBound() {
        if (this.mWrapper != null) {
            return true;
        }
        Log.w(this.TAG, "Notification listener service not yet bound.");
        return false;
    }

    public void registerAsSystemService(Context context, ComponentName componentName, int currentUser) throws RemoteException {
        this.mSystemContext = context;
        if (this.mWrapper == null) {
            this.mWrapper = new INotificationListenerWrapper();
        }
        INotificationManager noMan = getNotificationInterface();
        noMan.registerListener(this.mWrapper, componentName, currentUser);
        this.mCurrentUser = currentUser;
    }

    public void unregisterAsSystemService() throws RemoteException {
        if (this.mWrapper != null) {
            INotificationManager noMan = getNotificationInterface();
            noMan.unregisterListener(this.mWrapper, this.mCurrentUser);
        }
    }

    private class INotificationListenerWrapper extends INotificationListener.Stub {
        private INotificationListenerWrapper() {
        }

        @Override
        public void onNotificationPosted(IStatusBarNotificationHolder sbnHolder, NotificationRankingUpdate update) {
            try {
                StatusBarNotification sbn = sbnHolder.get();
                Notification.Builder.rebuild(NotificationListenerService.this.getContext(), sbn.getNotification());
                synchronized (NotificationListenerService.this.mWrapper) {
                    NotificationListenerService.this.applyUpdate(update);
                    try {
                        NotificationListenerService.this.onNotificationPosted(sbn, NotificationListenerService.this.mRankingMap);
                    } catch (Throwable t) {
                        Log.w(NotificationListenerService.this.TAG, "Error running onNotificationPosted", t);
                    }
                }
            } catch (RemoteException e) {
                Log.w(NotificationListenerService.this.TAG, "onNotificationPosted: Error receiving StatusBarNotification", e);
            }
        }

        @Override
        public void onNotificationRemoved(IStatusBarNotificationHolder sbnHolder, NotificationRankingUpdate update) {
            try {
                StatusBarNotification sbn = sbnHolder.get();
                synchronized (NotificationListenerService.this.mWrapper) {
                    NotificationListenerService.this.applyUpdate(update);
                    try {
                        NotificationListenerService.this.onNotificationRemoved(sbn, NotificationListenerService.this.mRankingMap);
                    } catch (Throwable t) {
                        Log.w(NotificationListenerService.this.TAG, "Error running onNotificationRemoved", t);
                    }
                }
            } catch (RemoteException e) {
                Log.w(NotificationListenerService.this.TAG, "onNotificationRemoved: Error receiving StatusBarNotification", e);
            }
        }

        @Override
        public void onListenerConnected(NotificationRankingUpdate update) {
            synchronized (NotificationListenerService.this.mWrapper) {
                NotificationListenerService.this.applyUpdate(update);
                try {
                    NotificationListenerService.this.onListenerConnected();
                } catch (Throwable t) {
                    Log.w(NotificationListenerService.this.TAG, "Error running onListenerConnected", t);
                }
            }
        }

        @Override
        public void onNotificationRankingUpdate(NotificationRankingUpdate update) throws RemoteException {
            synchronized (NotificationListenerService.this.mWrapper) {
                NotificationListenerService.this.applyUpdate(update);
                try {
                    NotificationListenerService.this.onNotificationRankingUpdate(NotificationListenerService.this.mRankingMap);
                } catch (Throwable t) {
                    Log.w(NotificationListenerService.this.TAG, "Error running onNotificationRankingUpdate", t);
                }
            }
        }

        @Override
        public void onListenerHintsChanged(int hints) throws RemoteException {
            try {
                NotificationListenerService.this.onListenerHintsChanged(hints);
            } catch (Throwable t) {
                Log.w(NotificationListenerService.this.TAG, "Error running onListenerHintsChanged", t);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) throws RemoteException {
            try {
                NotificationListenerService.this.onInterruptionFilterChanged(interruptionFilter);
            } catch (Throwable t) {
                Log.w(NotificationListenerService.this.TAG, "Error running onInterruptionFilterChanged", t);
            }
        }
    }

    private void applyUpdate(NotificationRankingUpdate update) {
        this.mRankingMap = new RankingMap(update);
    }

    private Context getContext() {
        if (this.mSystemContext != null) {
            return this.mSystemContext;
        }
        return this;
    }

    public static class Ranking {
        public static final int VISIBILITY_NO_OVERRIDE = -1000;
        private boolean mIsAmbient;
        private String mKey;
        private boolean mMatchesInterruptionFilter;
        private int mRank = -1;
        private int mVisibilityOverride;

        public String getKey() {
            return this.mKey;
        }

        public int getRank() {
            return this.mRank;
        }

        public boolean isAmbient() {
            return this.mIsAmbient;
        }

        public int getVisibilityOverride() {
            return this.mVisibilityOverride;
        }

        public boolean matchesInterruptionFilter() {
            return this.mMatchesInterruptionFilter;
        }

        private void populate(String key, int rank, boolean isAmbient, boolean matchesInterruptionFilter, int visibilityOverride) {
            this.mKey = key;
            this.mRank = rank;
            this.mIsAmbient = isAmbient;
            this.mMatchesInterruptionFilter = matchesInterruptionFilter;
            this.mVisibilityOverride = visibilityOverride;
        }
    }

    public static class RankingMap implements Parcelable {
        public static final Parcelable.Creator<RankingMap> CREATOR = new Parcelable.Creator<RankingMap>() {
            @Override
            public RankingMap createFromParcel(Parcel source) {
                NotificationRankingUpdate rankingUpdate = (NotificationRankingUpdate) source.readParcelable(null);
                return new RankingMap(rankingUpdate);
            }

            @Override
            public RankingMap[] newArray(int size) {
                return new RankingMap[size];
            }
        };
        private ArraySet<Object> mIntercepted;
        private final NotificationRankingUpdate mRankingUpdate;
        private ArrayMap<String, Integer> mRanks;
        private ArrayMap<String, Integer> mVisibilityOverrides;

        private RankingMap(NotificationRankingUpdate rankingUpdate) {
            this.mRankingUpdate = rankingUpdate;
        }

        public String[] getOrderedKeys() {
            return this.mRankingUpdate.getOrderedKeys();
        }

        public boolean getRanking(String key, Ranking outRanking) {
            int rank = getRank(key);
            outRanking.populate(key, rank, isAmbient(key), !isIntercepted(key), getVisibilityOverride(key));
            return rank >= 0;
        }

        private int getRank(String key) {
            synchronized (this) {
                if (this.mRanks == null) {
                    buildRanksLocked();
                }
            }
            Integer rank = this.mRanks.get(key);
            if (rank != null) {
                return rank.intValue();
            }
            return -1;
        }

        private boolean isAmbient(String key) {
            int rank;
            int firstAmbientIndex = this.mRankingUpdate.getFirstAmbientIndex();
            return firstAmbientIndex >= 0 && (rank = getRank(key)) >= 0 && rank >= firstAmbientIndex;
        }

        private boolean isIntercepted(String key) {
            synchronized (this) {
                if (this.mIntercepted == null) {
                    buildInterceptedSetLocked();
                }
            }
            return this.mIntercepted.contains(key);
        }

        private int getVisibilityOverride(String key) {
            synchronized (this) {
                if (this.mVisibilityOverrides == null) {
                    buildVisibilityOverridesLocked();
                }
            }
            Integer overide = this.mVisibilityOverrides.get(key);
            if (overide == null) {
                return -1000;
            }
            return overide.intValue();
        }

        private void buildRanksLocked() {
            String[] orderedKeys = this.mRankingUpdate.getOrderedKeys();
            this.mRanks = new ArrayMap<>(orderedKeys.length);
            for (int i = 0; i < orderedKeys.length; i++) {
                String key = orderedKeys[i];
                this.mRanks.put(key, Integer.valueOf(i));
            }
        }

        private void buildInterceptedSetLocked() {
            String[] dndInterceptedKeys = this.mRankingUpdate.getInterceptedKeys();
            this.mIntercepted = new ArraySet<>(dndInterceptedKeys.length);
            Collections.addAll(this.mIntercepted, dndInterceptedKeys);
        }

        private void buildVisibilityOverridesLocked() {
            Bundle visibilityBundle = this.mRankingUpdate.getVisibilityOverrides();
            this.mVisibilityOverrides = new ArrayMap<>(visibilityBundle.size());
            for (String key : visibilityBundle.keySet()) {
                this.mVisibilityOverrides.put(key, Integer.valueOf(visibilityBundle.getInt(key)));
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.mRankingUpdate, flags);
        }
    }
}
