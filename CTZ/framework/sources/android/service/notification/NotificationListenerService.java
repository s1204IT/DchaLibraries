package android.service.notification;

import android.annotation.SystemApi;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.Person;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.keystore.KeyProperties;
import android.service.notification.INotificationListener;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.widget.RemoteViews;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class NotificationListenerService extends Service {
    public static final int HINT_HOST_DISABLE_CALL_EFFECTS = 4;
    public static final int HINT_HOST_DISABLE_EFFECTS = 1;
    public static final int HINT_HOST_DISABLE_NOTIFICATION_EFFECTS = 2;
    public static final int INTERRUPTION_FILTER_ALARMS = 4;
    public static final int INTERRUPTION_FILTER_ALL = 1;
    public static final int INTERRUPTION_FILTER_NONE = 3;
    public static final int INTERRUPTION_FILTER_PRIORITY = 2;
    public static final int INTERRUPTION_FILTER_UNKNOWN = 0;
    public static final int NOTIFICATION_CHANNEL_OR_GROUP_ADDED = 1;
    public static final int NOTIFICATION_CHANNEL_OR_GROUP_DELETED = 3;
    public static final int NOTIFICATION_CHANNEL_OR_GROUP_UPDATED = 2;
    public static final int REASON_APP_CANCEL = 8;
    public static final int REASON_APP_CANCEL_ALL = 9;
    public static final int REASON_CANCEL = 2;
    public static final int REASON_CANCEL_ALL = 3;
    public static final int REASON_CHANNEL_BANNED = 17;
    public static final int REASON_CLICK = 1;
    public static final int REASON_ERROR = 4;
    public static final int REASON_GROUP_OPTIMIZATION = 13;
    public static final int REASON_GROUP_SUMMARY_CANCELED = 12;
    public static final int REASON_LISTENER_CANCEL = 10;
    public static final int REASON_LISTENER_CANCEL_ALL = 11;
    public static final int REASON_PACKAGE_BANNED = 7;
    public static final int REASON_PACKAGE_CHANGED = 5;
    public static final int REASON_PACKAGE_SUSPENDED = 14;
    public static final int REASON_PROFILE_TURNED_OFF = 15;
    public static final int REASON_SNOOZED = 18;
    public static final int REASON_TIMEOUT = 19;
    public static final int REASON_UNAUTOBUNDLED = 16;
    public static final int REASON_USER_STOPPED = 6;
    public static final String SERVICE_INTERFACE = "android.service.notification.NotificationListenerService";

    @Deprecated
    public static final int SUPPRESSED_EFFECT_SCREEN_OFF = 1;

    @Deprecated
    public static final int SUPPRESSED_EFFECT_SCREEN_ON = 2;

    @SystemApi
    public static final int TRIM_FULL = 0;

    @SystemApi
    public static final int TRIM_LIGHT = 1;
    protected int mCurrentUser;
    private Handler mHandler;
    protected INotificationManager mNoMan;

    @GuardedBy("mLock")
    private RankingMap mRankingMap;
    protected Context mSystemContext;
    private final String TAG = getClass().getSimpleName();
    private final Object mLock = new Object();
    protected NotificationListenerWrapper mWrapper = null;
    private boolean isConnected = false;

    @Retention(RetentionPolicy.SOURCE)
    public @interface ChannelOrGroupModificationTypes {
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        this.mHandler = new MyHandler(getMainLooper());
    }

    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
    }

    public void onNotificationPosted(StatusBarNotification statusBarNotification, RankingMap rankingMap) {
        onNotificationPosted(statusBarNotification);
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification, RankingMap rankingMap) {
        onNotificationRemoved(statusBarNotification);
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification, RankingMap rankingMap, int i) {
        onNotificationRemoved(statusBarNotification, rankingMap);
    }

    public void onNotificationRemoved(StatusBarNotification statusBarNotification, RankingMap rankingMap, NotificationStats notificationStats, int i) {
        onNotificationRemoved(statusBarNotification, rankingMap, i);
    }

    public void onListenerConnected() {
    }

    public void onListenerDisconnected() {
    }

    public void onNotificationRankingUpdate(RankingMap rankingMap) {
    }

    public void onListenerHintsChanged(int i) {
    }

    public void onNotificationChannelModified(String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) {
    }

    public void onNotificationChannelGroupModified(String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) {
    }

    public void onInterruptionFilterChanged(int i) {
    }

    protected final INotificationManager getNotificationInterface() {
        if (this.mNoMan == null) {
            this.mNoMan = INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        }
        return this.mNoMan;
    }

    @Deprecated
    public final void cancelNotification(String str, String str2, int i) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationFromListener(this.mWrapper, str, str2, i);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void cancelNotification(String str) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationsFromListener(this.mWrapper, new String[]{str});
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void cancelAllNotifications() {
        cancelNotifications(null);
    }

    public final void cancelNotifications(String[] strArr) {
        if (isBound()) {
            try {
                getNotificationInterface().cancelNotificationsFromListener(this.mWrapper, strArr);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    @SystemApi
    public final void snoozeNotification(String str, String str2) {
        if (isBound()) {
            try {
                getNotificationInterface().snoozeNotificationUntilContextFromListener(this.mWrapper, str, str2);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void snoozeNotification(String str, long j) {
        if (isBound()) {
            try {
                getNotificationInterface().snoozeNotificationUntilFromListener(this.mWrapper, str, j);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void setNotificationsShown(String[] strArr) {
        if (isBound()) {
            try {
                getNotificationInterface().setNotificationsShownFromListener(this.mWrapper, strArr);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void updateNotificationChannel(String str, UserHandle userHandle, NotificationChannel notificationChannel) {
        if (isBound()) {
            try {
                getNotificationInterface().updateNotificationChannelFromPrivilegedListener(this.mWrapper, str, userHandle, notificationChannel);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
                throw e.rethrowFromSystemServer();
            }
        }
    }

    public final List<NotificationChannel> getNotificationChannels(String str, UserHandle userHandle) {
        if (!isBound()) {
            return null;
        }
        try {
            return getNotificationInterface().getNotificationChannelsFromPrivilegedListener(this.mWrapper, str, userHandle).getList();
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public final List<NotificationChannelGroup> getNotificationChannelGroups(String str, UserHandle userHandle) {
        if (!isBound()) {
            return null;
        }
        try {
            return getNotificationInterface().getNotificationChannelGroupsFromPrivilegedListener(this.mWrapper, str, userHandle).getList();
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            throw e.rethrowFromSystemServer();
        }
    }

    @SystemApi
    public final void setOnNotificationPostedTrim(int i) {
        if (isBound()) {
            try {
                getNotificationInterface().setOnNotificationPostedTrimFromListener(this.mWrapper, i);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public StatusBarNotification[] getActiveNotifications() {
        StatusBarNotification[] activeNotifications = getActiveNotifications(null, 0);
        return activeNotifications != null ? activeNotifications : new StatusBarNotification[0];
    }

    public final StatusBarNotification[] getSnoozedNotifications() {
        try {
            return cleanUpNotificationList(getNotificationInterface().getSnoozedNotificationsFromListener(this.mWrapper, 0));
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            return null;
        }
    }

    @SystemApi
    public StatusBarNotification[] getActiveNotifications(int i) {
        StatusBarNotification[] activeNotifications = getActiveNotifications(null, i);
        return activeNotifications != null ? activeNotifications : new StatusBarNotification[0];
    }

    public StatusBarNotification[] getActiveNotifications(String[] strArr) {
        StatusBarNotification[] activeNotifications = getActiveNotifications(strArr, 0);
        return activeNotifications != null ? activeNotifications : new StatusBarNotification[0];
    }

    @SystemApi
    public StatusBarNotification[] getActiveNotifications(String[] strArr, int i) {
        if (!isBound()) {
            return null;
        }
        try {
            return cleanUpNotificationList(getNotificationInterface().getActiveNotificationsFromListener(this.mWrapper, strArr, i));
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            return null;
        }
    }

    private StatusBarNotification[] cleanUpNotificationList(ParceledListSlice<StatusBarNotification> parceledListSlice) throws PackageManager.NameNotFoundException {
        if (parceledListSlice == null || parceledListSlice.getList() == null) {
            return new StatusBarNotification[0];
        }
        List list = parceledListSlice.getList();
        ArrayList arrayList = null;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            StatusBarNotification statusBarNotification = (StatusBarNotification) list.get(i);
            Notification notification = statusBarNotification.getNotification();
            try {
                createLegacyIconExtras(notification);
                maybePopulateRemoteViews(notification);
                maybePopulatePeople(notification);
            } catch (IllegalArgumentException e) {
                if (arrayList == null) {
                    arrayList = new ArrayList(size);
                }
                arrayList.add(statusBarNotification);
                Log.w(this.TAG, "get(Active/Snoozed)Notifications: can't rebuild notification from " + statusBarNotification.getPackageName());
            }
        }
        if (arrayList != null) {
            list.removeAll(arrayList);
        }
        return (StatusBarNotification[]) list.toArray(new StatusBarNotification[list.size()]);
    }

    public final int getCurrentListenerHints() {
        if (!isBound()) {
            return 0;
        }
        try {
            return getNotificationInterface().getHintsFromListener(this.mWrapper);
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            return 0;
        }
    }

    public final int getCurrentInterruptionFilter() {
        if (!isBound()) {
            return 0;
        }
        try {
            return getNotificationInterface().getInterruptionFilterFromListener(this.mWrapper);
        } catch (RemoteException e) {
            Log.v(this.TAG, "Unable to contact notification manager", e);
            return 0;
        }
    }

    public final void requestListenerHints(int i) {
        if (isBound()) {
            try {
                getNotificationInterface().requestHintsFromListener(this.mWrapper, i);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public final void requestInterruptionFilter(int i) {
        if (isBound()) {
            try {
                getNotificationInterface().requestInterruptionFilterFromListener(this.mWrapper, i);
            } catch (RemoteException e) {
                Log.v(this.TAG, "Unable to contact notification manager", e);
            }
        }
    }

    public RankingMap getCurrentRanking() {
        RankingMap rankingMap;
        synchronized (this.mLock) {
            rankingMap = this.mRankingMap;
        }
        return rankingMap;
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (this.mWrapper == null) {
            this.mWrapper = new NotificationListenerWrapper();
        }
        return this.mWrapper;
    }

    protected boolean isBound() {
        if (this.mWrapper == null) {
            Log.w(this.TAG, "Notification listener service not yet bound.");
            return false;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        onListenerDisconnected();
        super.onDestroy();
    }

    @SystemApi
    public void registerAsSystemService(Context context, ComponentName componentName, int i) throws RemoteException {
        if (this.mWrapper == null) {
            this.mWrapper = new NotificationListenerWrapper();
        }
        this.mSystemContext = context;
        INotificationManager notificationInterface = getNotificationInterface();
        this.mHandler = new MyHandler(context.getMainLooper());
        this.mCurrentUser = i;
        notificationInterface.registerListener(this.mWrapper, componentName, i);
    }

    @SystemApi
    public void unregisterAsSystemService() throws RemoteException {
        if (this.mWrapper != null) {
            getNotificationInterface().unregisterListener(this.mWrapper, this.mCurrentUser);
        }
    }

    public static void requestRebind(ComponentName componentName) {
        try {
            INotificationManager.Stub.asInterface(ServiceManager.getService(Context.NOTIFICATION_SERVICE)).requestBindListener(componentName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public final void requestUnbind() {
        if (this.mWrapper != null) {
            try {
                getNotificationInterface().requestUnbindListener(this.mWrapper);
                this.isConnected = false;
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    }

    private void createLegacyIconExtras(Notification notification) {
        Drawable drawableLoadDrawable;
        Icon smallIcon = notification.getSmallIcon();
        Icon largeIcon = notification.getLargeIcon();
        if (smallIcon != null && smallIcon.getType() == 2) {
            notification.extras.putInt(Notification.EXTRA_SMALL_ICON, smallIcon.getResId());
            notification.icon = smallIcon.getResId();
        }
        if (largeIcon != null && (drawableLoadDrawable = largeIcon.loadDrawable(getContext())) != null && (drawableLoadDrawable instanceof BitmapDrawable)) {
            Bitmap bitmap = ((BitmapDrawable) drawableLoadDrawable).getBitmap();
            notification.extras.putParcelable(Notification.EXTRA_LARGE_ICON, bitmap);
            notification.largeIcon = bitmap;
        }
    }

    private void maybePopulateRemoteViews(Notification notification) throws PackageManager.NameNotFoundException {
        if (getContext().getApplicationInfo().targetSdkVersion < 24) {
            Notification.Builder builderRecoverBuilder = Notification.Builder.recoverBuilder(getContext(), notification);
            RemoteViews remoteViewsCreateContentView = builderRecoverBuilder.createContentView();
            RemoteViews remoteViewsCreateBigContentView = builderRecoverBuilder.createBigContentView();
            RemoteViews remoteViewsCreateHeadsUpContentView = builderRecoverBuilder.createHeadsUpContentView();
            notification.contentView = remoteViewsCreateContentView;
            notification.bigContentView = remoteViewsCreateBigContentView;
            notification.headsUpContentView = remoteViewsCreateHeadsUpContentView;
        }
    }

    private void maybePopulatePeople(Notification notification) {
        ArrayList parcelableArrayList;
        if (getContext().getApplicationInfo().targetSdkVersion < 28 && (parcelableArrayList = notification.extras.getParcelableArrayList(Notification.EXTRA_PEOPLE_LIST)) != null && parcelableArrayList.isEmpty()) {
            int size = parcelableArrayList.size();
            String[] strArr = new String[size];
            for (int i = 0; i < size; i++) {
                strArr[i] = ((Person) parcelableArrayList.get(i)).resolveToLegacyUri();
            }
            notification.extras.putStringArray(Notification.EXTRA_PEOPLE, strArr);
        }
    }

    protected class NotificationListenerWrapper extends INotificationListener.Stub {
        protected NotificationListenerWrapper() {
        }

        @Override
        public void onNotificationPosted(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate) throws PackageManager.NameNotFoundException {
            try {
                StatusBarNotification statusBarNotification = iStatusBarNotificationHolder.get();
                try {
                    NotificationListenerService.this.createLegacyIconExtras(statusBarNotification.getNotification());
                    NotificationListenerService.this.maybePopulateRemoteViews(statusBarNotification.getNotification());
                    NotificationListenerService.this.maybePopulatePeople(statusBarNotification.getNotification());
                } catch (IllegalArgumentException e) {
                    Log.w(NotificationListenerService.this.TAG, "onNotificationPosted: can't rebuild notification from " + statusBarNotification.getPackageName());
                    statusBarNotification = null;
                }
                synchronized (NotificationListenerService.this.mLock) {
                    NotificationListenerService.this.applyUpdateLocked(notificationRankingUpdate);
                    if (statusBarNotification != null) {
                        SomeArgs someArgsObtain = SomeArgs.obtain();
                        someArgsObtain.arg1 = statusBarNotification;
                        someArgsObtain.arg2 = NotificationListenerService.this.mRankingMap;
                        NotificationListenerService.this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
                    } else {
                        NotificationListenerService.this.mHandler.obtainMessage(4, NotificationListenerService.this.mRankingMap).sendToTarget();
                    }
                }
            } catch (RemoteException e2) {
                Log.w(NotificationListenerService.this.TAG, "onNotificationPosted: Error receiving StatusBarNotification", e2);
            }
        }

        @Override
        public void onNotificationRemoved(IStatusBarNotificationHolder iStatusBarNotificationHolder, NotificationRankingUpdate notificationRankingUpdate, NotificationStats notificationStats, int i) {
            try {
                StatusBarNotification statusBarNotification = iStatusBarNotificationHolder.get();
                synchronized (NotificationListenerService.this.mLock) {
                    NotificationListenerService.this.applyUpdateLocked(notificationRankingUpdate);
                    SomeArgs someArgsObtain = SomeArgs.obtain();
                    someArgsObtain.arg1 = statusBarNotification;
                    someArgsObtain.arg2 = NotificationListenerService.this.mRankingMap;
                    someArgsObtain.arg3 = Integer.valueOf(i);
                    someArgsObtain.arg4 = notificationStats;
                    NotificationListenerService.this.mHandler.obtainMessage(2, someArgsObtain).sendToTarget();
                }
            } catch (RemoteException e) {
                Log.w(NotificationListenerService.this.TAG, "onNotificationRemoved: Error receiving StatusBarNotification", e);
            }
        }

        @Override
        public void onListenerConnected(NotificationRankingUpdate notificationRankingUpdate) {
            synchronized (NotificationListenerService.this.mLock) {
                NotificationListenerService.this.applyUpdateLocked(notificationRankingUpdate);
            }
            NotificationListenerService.this.isConnected = true;
            NotificationListenerService.this.mHandler.obtainMessage(3).sendToTarget();
        }

        @Override
        public void onNotificationRankingUpdate(NotificationRankingUpdate notificationRankingUpdate) throws RemoteException {
            synchronized (NotificationListenerService.this.mLock) {
                NotificationListenerService.this.applyUpdateLocked(notificationRankingUpdate);
                NotificationListenerService.this.mHandler.obtainMessage(4, NotificationListenerService.this.mRankingMap).sendToTarget();
            }
        }

        @Override
        public void onListenerHintsChanged(int i) throws RemoteException {
            NotificationListenerService.this.mHandler.obtainMessage(5, i, 0).sendToTarget();
        }

        @Override
        public void onInterruptionFilterChanged(int i) throws RemoteException {
            NotificationListenerService.this.mHandler.obtainMessage(6, i, 0).sendToTarget();
        }

        public void onNotificationEnqueued(IStatusBarNotificationHolder iStatusBarNotificationHolder) throws RemoteException {
        }

        public void onNotificationSnoozedUntilContext(IStatusBarNotificationHolder iStatusBarNotificationHolder, String str) throws RemoteException {
        }

        @Override
        public void onNotificationChannelModification(String str, UserHandle userHandle, NotificationChannel notificationChannel, int i) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = userHandle;
            someArgsObtain.arg3 = notificationChannel;
            someArgsObtain.arg4 = Integer.valueOf(i);
            NotificationListenerService.this.mHandler.obtainMessage(7, someArgsObtain).sendToTarget();
        }

        @Override
        public void onNotificationChannelGroupModification(String str, UserHandle userHandle, NotificationChannelGroup notificationChannelGroup, int i) {
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = userHandle;
            someArgsObtain.arg3 = notificationChannelGroup;
            someArgsObtain.arg4 = Integer.valueOf(i);
            NotificationListenerService.this.mHandler.obtainMessage(8, someArgsObtain).sendToTarget();
        }
    }

    @GuardedBy("mLock")
    public final void applyUpdateLocked(NotificationRankingUpdate notificationRankingUpdate) {
        this.mRankingMap = new RankingMap(notificationRankingUpdate);
    }

    protected Context getContext() {
        if (this.mSystemContext != null) {
            return this.mSystemContext;
        }
        return this;
    }

    public static class Ranking {
        public static final int USER_SENTIMENT_NEGATIVE = -1;
        public static final int USER_SENTIMENT_NEUTRAL = 0;
        public static final int USER_SENTIMENT_POSITIVE = 1;
        public static final int VISIBILITY_NO_OVERRIDE = -1000;
        private NotificationChannel mChannel;
        private boolean mHidden;
        private int mImportance;
        private CharSequence mImportanceExplanation;
        private boolean mIsAmbient;
        private String mKey;
        private boolean mMatchesInterruptionFilter;
        private String mOverrideGroupKey;
        private ArrayList<String> mOverridePeople;
        private boolean mShowBadge;
        private ArrayList<SnoozeCriterion> mSnoozeCriteria;
        private int mSuppressedVisualEffects;
        private int mVisibilityOverride;
        private int mRank = -1;
        private int mUserSentiment = 0;

        @Retention(RetentionPolicy.SOURCE)
        public @interface UserSentiment {
        }

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

        public int getSuppressedVisualEffects() {
            return this.mSuppressedVisualEffects;
        }

        public boolean matchesInterruptionFilter() {
            return this.mMatchesInterruptionFilter;
        }

        public int getImportance() {
            return this.mImportance;
        }

        public CharSequence getImportanceExplanation() {
            return this.mImportanceExplanation;
        }

        public String getOverrideGroupKey() {
            return this.mOverrideGroupKey;
        }

        public NotificationChannel getChannel() {
            return this.mChannel;
        }

        public int getUserSentiment() {
            return this.mUserSentiment;
        }

        @SystemApi
        public List<String> getAdditionalPeople() {
            return this.mOverridePeople;
        }

        @SystemApi
        public List<SnoozeCriterion> getSnoozeCriteria() {
            return this.mSnoozeCriteria;
        }

        public boolean canShowBadge() {
            return this.mShowBadge;
        }

        public boolean isSuspended() {
            return this.mHidden;
        }

        @VisibleForTesting
        public void populate(String str, int i, boolean z, int i2, int i3, int i4, CharSequence charSequence, String str2, NotificationChannel notificationChannel, ArrayList<String> arrayList, ArrayList<SnoozeCriterion> arrayList2, boolean z2, int i5, boolean z3) {
            this.mKey = str;
            this.mRank = i;
            this.mIsAmbient = i4 < 2;
            this.mMatchesInterruptionFilter = z;
            this.mVisibilityOverride = i2;
            this.mSuppressedVisualEffects = i3;
            this.mImportance = i4;
            this.mImportanceExplanation = charSequence;
            this.mOverrideGroupKey = str2;
            this.mChannel = notificationChannel;
            this.mOverridePeople = arrayList;
            this.mSnoozeCriteria = arrayList2;
            this.mShowBadge = z2;
            this.mUserSentiment = i5;
            this.mHidden = z3;
        }

        public static String importanceToString(int i) {
            if (i == -1000) {
                return "UNSPECIFIED";
            }
            switch (i) {
                case 0:
                    return KeyProperties.DIGEST_NONE;
                case 1:
                    return "MIN";
                case 2:
                    return "LOW";
                case 3:
                    return "DEFAULT";
                case 4:
                case 5:
                    return "HIGH";
                default:
                    return "UNKNOWN(" + String.valueOf(i) + ")";
            }
        }
    }

    public static class RankingMap implements Parcelable {
        public static final Parcelable.Creator<RankingMap> CREATOR = new Parcelable.Creator<RankingMap>() {
            @Override
            public RankingMap createFromParcel(Parcel parcel) {
                return new RankingMap((NotificationRankingUpdate) parcel.readParcelable(null));
            }

            @Override
            public RankingMap[] newArray(int i) {
                return new RankingMap[i];
            }
        };
        private ArrayMap<String, NotificationChannel> mChannels;
        private ArrayMap<String, Boolean> mHidden;
        private ArrayMap<String, Integer> mImportance;
        private ArrayMap<String, String> mImportanceExplanation;
        private ArraySet<Object> mIntercepted;
        private ArrayMap<String, String> mOverrideGroupKeys;
        private ArrayMap<String, ArrayList<String>> mOverridePeople;
        private final NotificationRankingUpdate mRankingUpdate;
        private ArrayMap<String, Integer> mRanks;
        private ArrayMap<String, Boolean> mShowBadge;
        private ArrayMap<String, ArrayList<SnoozeCriterion>> mSnoozeCriteria;
        private ArrayMap<String, Integer> mSuppressedVisualEffects;
        private ArrayMap<String, Integer> mUserSentiment;
        private ArrayMap<String, Integer> mVisibilityOverrides;

        private RankingMap(NotificationRankingUpdate notificationRankingUpdate) {
            this.mRankingUpdate = notificationRankingUpdate;
        }

        public String[] getOrderedKeys() {
            return this.mRankingUpdate.getOrderedKeys();
        }

        public boolean getRanking(String str, Ranking ranking) {
            int rank = getRank(str);
            ranking.populate(str, rank, !isIntercepted(str), getVisibilityOverride(str), getSuppressedVisualEffects(str), getImportance(str), getImportanceExplanation(str), getOverrideGroupKey(str), getChannel(str), getOverridePeople(str), getSnoozeCriteria(str), getShowBadge(str), getUserSentiment(str), getHidden(str));
            return rank >= 0;
        }

        private int getRank(String str) {
            synchronized (this) {
                if (this.mRanks == null) {
                    buildRanksLocked();
                }
            }
            Integer num = this.mRanks.get(str);
            if (num != null) {
                return num.intValue();
            }
            return -1;
        }

        private boolean isIntercepted(String str) {
            synchronized (this) {
                if (this.mIntercepted == null) {
                    buildInterceptedSetLocked();
                }
            }
            return this.mIntercepted.contains(str);
        }

        private int getVisibilityOverride(String str) {
            synchronized (this) {
                if (this.mVisibilityOverrides == null) {
                    buildVisibilityOverridesLocked();
                }
            }
            Integer num = this.mVisibilityOverrides.get(str);
            if (num == null) {
                return -1000;
            }
            return num.intValue();
        }

        private int getSuppressedVisualEffects(String str) {
            synchronized (this) {
                if (this.mSuppressedVisualEffects == null) {
                    buildSuppressedVisualEffectsLocked();
                }
            }
            Integer num = this.mSuppressedVisualEffects.get(str);
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        private int getImportance(String str) {
            synchronized (this) {
                if (this.mImportance == null) {
                    buildImportanceLocked();
                }
            }
            Integer num = this.mImportance.get(str);
            if (num == null) {
                return 3;
            }
            return num.intValue();
        }

        private String getImportanceExplanation(String str) {
            synchronized (this) {
                if (this.mImportanceExplanation == null) {
                    buildImportanceExplanationLocked();
                }
            }
            return this.mImportanceExplanation.get(str);
        }

        private String getOverrideGroupKey(String str) {
            synchronized (this) {
                if (this.mOverrideGroupKeys == null) {
                    buildOverrideGroupKeys();
                }
            }
            return this.mOverrideGroupKeys.get(str);
        }

        private NotificationChannel getChannel(String str) {
            synchronized (this) {
                if (this.mChannels == null) {
                    buildChannelsLocked();
                }
            }
            return this.mChannels.get(str);
        }

        private ArrayList<String> getOverridePeople(String str) {
            synchronized (this) {
                if (this.mOverridePeople == null) {
                    buildOverridePeopleLocked();
                }
            }
            return this.mOverridePeople.get(str);
        }

        private ArrayList<SnoozeCriterion> getSnoozeCriteria(String str) {
            synchronized (this) {
                if (this.mSnoozeCriteria == null) {
                    buildSnoozeCriteriaLocked();
                }
            }
            return this.mSnoozeCriteria.get(str);
        }

        private boolean getShowBadge(String str) {
            synchronized (this) {
                if (this.mShowBadge == null) {
                    buildShowBadgeLocked();
                }
            }
            Boolean bool = this.mShowBadge.get(str);
            if (bool == null) {
                return false;
            }
            return bool.booleanValue();
        }

        private int getUserSentiment(String str) {
            synchronized (this) {
                if (this.mUserSentiment == null) {
                    buildUserSentimentLocked();
                }
            }
            Integer num = this.mUserSentiment.get(str);
            if (num == null) {
                return 0;
            }
            return num.intValue();
        }

        private boolean getHidden(String str) {
            synchronized (this) {
                if (this.mHidden == null) {
                    buildHiddenLocked();
                }
            }
            Boolean bool = this.mHidden.get(str);
            if (bool == null) {
                return false;
            }
            return bool.booleanValue();
        }

        private void buildRanksLocked() {
            String[] orderedKeys = this.mRankingUpdate.getOrderedKeys();
            this.mRanks = new ArrayMap<>(orderedKeys.length);
            for (int i = 0; i < orderedKeys.length; i++) {
                this.mRanks.put(orderedKeys[i], Integer.valueOf(i));
            }
        }

        private void buildInterceptedSetLocked() {
            String[] interceptedKeys = this.mRankingUpdate.getInterceptedKeys();
            this.mIntercepted = new ArraySet<>(interceptedKeys.length);
            Collections.addAll(this.mIntercepted, interceptedKeys);
        }

        private void buildVisibilityOverridesLocked() {
            Bundle visibilityOverrides = this.mRankingUpdate.getVisibilityOverrides();
            this.mVisibilityOverrides = new ArrayMap<>(visibilityOverrides.size());
            for (String str : visibilityOverrides.keySet()) {
                this.mVisibilityOverrides.put(str, Integer.valueOf(visibilityOverrides.getInt(str)));
            }
        }

        private void buildSuppressedVisualEffectsLocked() {
            Bundle suppressedVisualEffects = this.mRankingUpdate.getSuppressedVisualEffects();
            this.mSuppressedVisualEffects = new ArrayMap<>(suppressedVisualEffects.size());
            for (String str : suppressedVisualEffects.keySet()) {
                this.mSuppressedVisualEffects.put(str, Integer.valueOf(suppressedVisualEffects.getInt(str)));
            }
        }

        private void buildImportanceLocked() {
            String[] orderedKeys = this.mRankingUpdate.getOrderedKeys();
            int[] importance = this.mRankingUpdate.getImportance();
            this.mImportance = new ArrayMap<>(orderedKeys.length);
            for (int i = 0; i < orderedKeys.length; i++) {
                this.mImportance.put(orderedKeys[i], Integer.valueOf(importance[i]));
            }
        }

        private void buildImportanceExplanationLocked() {
            Bundle importanceExplanation = this.mRankingUpdate.getImportanceExplanation();
            this.mImportanceExplanation = new ArrayMap<>(importanceExplanation.size());
            for (String str : importanceExplanation.keySet()) {
                this.mImportanceExplanation.put(str, importanceExplanation.getString(str));
            }
        }

        private void buildOverrideGroupKeys() {
            Bundle overrideGroupKeys = this.mRankingUpdate.getOverrideGroupKeys();
            this.mOverrideGroupKeys = new ArrayMap<>(overrideGroupKeys.size());
            for (String str : overrideGroupKeys.keySet()) {
                this.mOverrideGroupKeys.put(str, overrideGroupKeys.getString(str));
            }
        }

        private void buildChannelsLocked() {
            Bundle channels = this.mRankingUpdate.getChannels();
            this.mChannels = new ArrayMap<>(channels.size());
            for (String str : channels.keySet()) {
                this.mChannels.put(str, (NotificationChannel) channels.getParcelable(str));
            }
        }

        private void buildOverridePeopleLocked() {
            Bundle overridePeople = this.mRankingUpdate.getOverridePeople();
            this.mOverridePeople = new ArrayMap<>(overridePeople.size());
            for (String str : overridePeople.keySet()) {
                this.mOverridePeople.put(str, overridePeople.getStringArrayList(str));
            }
        }

        private void buildSnoozeCriteriaLocked() {
            Bundle snoozeCriteria = this.mRankingUpdate.getSnoozeCriteria();
            this.mSnoozeCriteria = new ArrayMap<>(snoozeCriteria.size());
            for (String str : snoozeCriteria.keySet()) {
                this.mSnoozeCriteria.put(str, snoozeCriteria.getParcelableArrayList(str));
            }
        }

        private void buildShowBadgeLocked() {
            Bundle showBadge = this.mRankingUpdate.getShowBadge();
            this.mShowBadge = new ArrayMap<>(showBadge.size());
            for (String str : showBadge.keySet()) {
                this.mShowBadge.put(str, Boolean.valueOf(showBadge.getBoolean(str)));
            }
        }

        private void buildUserSentimentLocked() {
            Bundle userSentiment = this.mRankingUpdate.getUserSentiment();
            this.mUserSentiment = new ArrayMap<>(userSentiment.size());
            for (String str : userSentiment.keySet()) {
                this.mUserSentiment.put(str, Integer.valueOf(userSentiment.getInt(str)));
            }
        }

        private void buildHiddenLocked() {
            Bundle hidden = this.mRankingUpdate.getHidden();
            this.mHidden = new ArrayMap<>(hidden.size());
            for (String str : hidden.keySet()) {
                this.mHidden.put(str, Boolean.valueOf(hidden.getBoolean(str)));
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeParcelable(this.mRankingUpdate, i);
        }
    }

    private final class MyHandler extends Handler {
        public static final int MSG_ON_INTERRUPTION_FILTER_CHANGED = 6;
        public static final int MSG_ON_LISTENER_CONNECTED = 3;
        public static final int MSG_ON_LISTENER_HINTS_CHANGED = 5;
        public static final int MSG_ON_NOTIFICATION_CHANNEL_GROUP_MODIFIED = 8;
        public static final int MSG_ON_NOTIFICATION_CHANNEL_MODIFIED = 7;
        public static final int MSG_ON_NOTIFICATION_POSTED = 1;
        public static final int MSG_ON_NOTIFICATION_RANKING_UPDATE = 4;
        public static final int MSG_ON_NOTIFICATION_REMOVED = 2;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        @Override
        public void handleMessage(Message message) {
            if (!NotificationListenerService.this.isConnected) {
            }
            switch (message.what) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    StatusBarNotification statusBarNotification = (StatusBarNotification) someArgs.arg1;
                    RankingMap rankingMap = (RankingMap) someArgs.arg2;
                    someArgs.recycle();
                    NotificationListenerService.this.onNotificationPosted(statusBarNotification, rankingMap);
                    break;
                case 2:
                    SomeArgs someArgs2 = (SomeArgs) message.obj;
                    StatusBarNotification statusBarNotification2 = (StatusBarNotification) someArgs2.arg1;
                    RankingMap rankingMap2 = (RankingMap) someArgs2.arg2;
                    int iIntValue = ((Integer) someArgs2.arg3).intValue();
                    NotificationStats notificationStats = (NotificationStats) someArgs2.arg4;
                    someArgs2.recycle();
                    NotificationListenerService.this.onNotificationRemoved(statusBarNotification2, rankingMap2, notificationStats, iIntValue);
                    break;
                case 3:
                    NotificationListenerService.this.onListenerConnected();
                    break;
                case 4:
                    NotificationListenerService.this.onNotificationRankingUpdate((RankingMap) message.obj);
                    break;
                case 5:
                    NotificationListenerService.this.onListenerHintsChanged(message.arg1);
                    break;
                case 6:
                    NotificationListenerService.this.onInterruptionFilterChanged(message.arg1);
                    break;
                case 7:
                    SomeArgs someArgs3 = (SomeArgs) message.obj;
                    NotificationListenerService.this.onNotificationChannelModified((String) someArgs3.arg1, (UserHandle) someArgs3.arg2, (NotificationChannel) someArgs3.arg3, ((Integer) someArgs3.arg4).intValue());
                    break;
                case 8:
                    SomeArgs someArgs4 = (SomeArgs) message.obj;
                    NotificationListenerService.this.onNotificationChannelGroupModified((String) someArgs4.arg1, (UserHandle) someArgs4.arg2, (NotificationChannelGroup) someArgs4.arg3, ((Integer) someArgs4.arg4).intValue());
                    break;
            }
        }
    }
}
