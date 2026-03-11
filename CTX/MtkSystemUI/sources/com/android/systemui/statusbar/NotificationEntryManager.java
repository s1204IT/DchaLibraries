package com.android.systemui.statusbar;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.NotificationVisibility;
import com.android.internal.util.NotificationMessagingUtil;
import com.android.systemui.DejankUtils;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.ForegroundServiceController;
import com.android.systemui.R;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationLifetimeExtender;
import com.android.systemui.statusbar.notification.InflationException;
import com.android.systemui.statusbar.notification.NotificationInflater;
import com.android.systemui.statusbar.notification.RowInflaterTask;
import com.android.systemui.statusbar.notification.VisualStabilityManager;
import com.android.systemui.statusbar.phone.NotificationGroupManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import com.android.systemui.util.leak.LeakDetector;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NotificationEntryManager implements Dumpable, ExpandableNotificationRow.ExpansionLogger, NotificationInflater.InflationCallback, VisualStabilityManager.Callback {
    protected static final boolean CHATTY = StatusBar.DEBUG;
    protected Callback mCallback;
    protected final Context mContext;
    protected boolean mDisableNotificationAlerts;
    protected HeadsUpManager mHeadsUpManager;
    protected ContentObserver mHeadsUpObserver;
    protected NotificationListenerService.RankingMap mLatestRankingMap;
    protected NotificationListContainer mListContainer;
    protected final NotificationMessagingUtil mMessagingUtil;
    protected NotificationData mNotificationData;
    private ExpandableNotificationRow.OnAppOpsClickListener mOnAppOpsClickListener;
    protected PowerManager mPowerManager;
    protected NotificationPresenter mPresenter;
    protected SystemServicesProxy mSystemServicesProxy;
    protected final HashMap<String, NotificationData.Entry> mPendingNotifications = new HashMap<>();
    protected final NotificationClicker mNotificationClicker = new NotificationClicker();
    protected final ArraySet<NotificationData.Entry> mHeadsUpEntriesToRemoveOnSwitch = new ArraySet<>();
    protected final NotificationLockscreenUserManager mLockscreenUserManager = (NotificationLockscreenUserManager) Dependency.get(NotificationLockscreenUserManager.class);
    protected final NotificationGroupManager mGroupManager = (NotificationGroupManager) Dependency.get(NotificationGroupManager.class);
    protected final NotificationGutsManager mGutsManager = (NotificationGutsManager) Dependency.get(NotificationGutsManager.class);
    protected final NotificationRemoteInputManager mRemoteInputManager = (NotificationRemoteInputManager) Dependency.get(NotificationRemoteInputManager.class);
    protected final NotificationMediaManager mMediaManager = (NotificationMediaManager) Dependency.get(NotificationMediaManager.class);
    protected final MetricsLogger mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
    protected final DeviceProvisionedController mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
    protected final VisualStabilityManager mVisualStabilityManager = (VisualStabilityManager) Dependency.get(VisualStabilityManager.class);
    protected final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
    protected final ForegroundServiceController mForegroundServiceController = (ForegroundServiceController) Dependency.get(ForegroundServiceController.class);
    protected final NotificationListener mNotificationListener = (NotificationListener) Dependency.get(NotificationListener.class);
    private final SmartReplyController mSmartReplyController = (SmartReplyController) Dependency.get(SmartReplyController.class);
    private final NotificationLifetimeExtender mFGSExtender = new ForegroundServiceLifetimeExtender();
    private final Map<NotificationData.Entry, NotificationLifetimeExtender> mRetainedNotifications = new ArrayMap();
    protected boolean mUseHeadsUp = false;
    private final ArraySet<String> mKeysKeptForRemoteInput = new ArraySet<>();
    private final DeviceProvisionedController.DeviceProvisionedListener mDeviceProvisionedListener = new DeviceProvisionedController.DeviceProvisionedListener() {
        @Override
        public void onDeviceProvisionedChanged() {
            NotificationEntryManager.this.updateNotifications();
        }
    };
    protected IStatusBarService mBarService = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"));

    public interface Callback {
        void onBindRow(NotificationData.Entry entry, PackageManager packageManager, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow);

        void onNotificationAdded(NotificationData.Entry entry);

        void onNotificationClicked(StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow);

        void onNotificationRemoved(String str, StatusBarNotification statusBarNotification);

        void onNotificationUpdated(StatusBarNotification statusBarNotification);

        void onPerformRemoveNotification(StatusBarNotification statusBarNotification);

        boolean shouldPeek(NotificationData.Entry entry, StatusBarNotification statusBarNotification);
    }

    final class NotificationClicker implements View.OnClickListener {
        private NotificationClicker() {
        }

        @Override
        public void onClick(View view) {
            if (!(view instanceof ExpandableNotificationRow)) {
                Log.e("NotificationEntryMgr", "NotificationClicker called on a view that is not a notification row.");
                return;
            }
            NotificationEntryManager.this.mPresenter.wakeUpIfDozing(SystemClock.uptimeMillis(), view);
            final ExpandableNotificationRow expandableNotificationRow = (ExpandableNotificationRow) view;
            StatusBarNotification statusBarNotification = expandableNotificationRow.getStatusBarNotification();
            if (statusBarNotification == null) {
                Log.e("NotificationEntryMgr", "NotificationClicker called on an unclickable notification,");
                return;
            }
            if (expandableNotificationRow.getProvider() != null && expandableNotificationRow.getProvider().isMenuVisible()) {
                expandableNotificationRow.animateTranslateNotification(0.0f);
                return;
            }
            expandableNotificationRow.setJustClicked(true);
            DejankUtils.postAfterTraversal(new Runnable() {
                @Override
                public final void run() {
                    expandableNotificationRow.setJustClicked(false);
                }
            });
            NotificationEntryManager.this.mCallback.onNotificationClicked(statusBarNotification, expandableNotificationRow);
        }

        public void register(ExpandableNotificationRow expandableNotificationRow, StatusBarNotification statusBarNotification) {
            Notification notification = statusBarNotification.getNotification();
            if (notification.contentIntent != null || notification.fullScreenIntent != null) {
                expandableNotificationRow.setOnClickListener(this);
            } else {
                expandableNotificationRow.setOnClickListener(null);
            }
        }
    }

    public NotificationListenerService.RankingMap getLatestRankingMap() {
        return this.mLatestRankingMap;
    }

    public void setLatestRankingMap(NotificationListenerService.RankingMap rankingMap) {
        this.mLatestRankingMap = rankingMap;
    }

    public void setDisableNotificationAlerts(boolean z) {
        this.mDisableNotificationAlerts = z;
        this.mHeadsUpObserver.onChange(true);
    }

    public void onHeadsUpStateChanged(NotificationData.Entry entry, boolean z) {
        if (!z && this.mHeadsUpEntriesToRemoveOnSwitch.contains(entry)) {
            removeNotification(entry.key, getLatestRankingMap());
            this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
            if (this.mHeadsUpEntriesToRemoveOnSwitch.isEmpty()) {
                setLatestRankingMap(null);
                return;
            }
            return;
        }
        updateNotificationRanking(null);
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("NotificationEntryManager state:");
        printWriter.print("  mPendingNotifications=");
        if (this.mPendingNotifications.size() == 0) {
            printWriter.println("null");
        } else {
            Iterator<NotificationData.Entry> it = this.mPendingNotifications.values().iterator();
            while (it.hasNext()) {
                printWriter.println(it.next().notification);
            }
        }
        printWriter.println("  Lifetime-extended notifications:");
        if (this.mRetainedNotifications.isEmpty()) {
            printWriter.println("    None");
        } else {
            for (Map.Entry<NotificationData.Entry, NotificationLifetimeExtender> entry : this.mRetainedNotifications.entrySet()) {
                printWriter.println("    " + entry.getKey().notification + " retained by " + entry.getValue().getClass().getName());
            }
        }
        printWriter.print("  mUseHeadsUp=");
        printWriter.println(this.mUseHeadsUp);
        printWriter.print("  mKeysKeptForRemoteInput: ");
        printWriter.println(this.mKeysKeptForRemoteInput);
    }

    public NotificationEntryManager(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mMessagingUtil = new NotificationMessagingUtil(context);
        this.mSystemServicesProxy = SystemServicesProxy.getInstance(this.mContext);
        this.mGroupManager.setPendingEntries(this.mPendingNotifications);
        this.mFGSExtender.setCallback(new NotificationLifetimeExtender.NotificationSafeToRemoveCallback() {
            @Override
            public final void onSafeToRemove(String str) {
                NotificationEntryManager notificationEntryManager = this.f$0;
                notificationEntryManager.removeNotification(str, notificationEntryManager.mLatestRankingMap);
            }
        });
    }

    public void setUpWithPresenter(NotificationPresenter notificationPresenter, NotificationListContainer notificationListContainer, Callback callback, HeadsUpManager headsUpManager) {
        this.mPresenter = notificationPresenter;
        this.mCallback = callback;
        this.mNotificationData = new NotificationData(notificationPresenter);
        this.mHeadsUpManager = headsUpManager;
        this.mNotificationData.setHeadsUpManager(this.mHeadsUpManager);
        this.mListContainer = notificationListContainer;
        this.mHeadsUpObserver = new ContentObserver(this.mPresenter.getHandler()) {
            @Override
            public void onChange(boolean z) {
                boolean z2 = NotificationEntryManager.this.mUseHeadsUp;
                NotificationEntryManager notificationEntryManager = NotificationEntryManager.this;
                boolean z3 = false;
                if (!NotificationEntryManager.this.mDisableNotificationAlerts && Settings.Global.getInt(NotificationEntryManager.this.mContext.getContentResolver(), "heads_up_notifications_enabled", 0) != 0) {
                    z3 = true;
                }
                notificationEntryManager.mUseHeadsUp = z3;
                StringBuilder sb = new StringBuilder();
                sb.append("heads up is ");
                sb.append(NotificationEntryManager.this.mUseHeadsUp ? "enabled" : "disabled");
                Log.d("NotificationEntryMgr", sb.toString());
                if (z2 != NotificationEntryManager.this.mUseHeadsUp && !NotificationEntryManager.this.mUseHeadsUp) {
                    Log.d("NotificationEntryMgr", "dismissing any existing heads up notification on disable event");
                    NotificationEntryManager.this.mHeadsUpManager.releaseAllImmediately();
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("heads_up_notifications_enabled"), true, this.mHeadsUpObserver);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("ticker_gets_heads_up"), true, this.mHeadsUpObserver);
        this.mDeviceProvisionedController.addCallback(this.mDeviceProvisionedListener);
        this.mHeadsUpObserver.onChange(true);
        final NotificationGutsManager notificationGutsManager = this.mGutsManager;
        Objects.requireNonNull(notificationGutsManager);
        this.mOnAppOpsClickListener = new ExpandableNotificationRow.OnAppOpsClickListener() {
            @Override
            public final boolean onClick(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem) {
                return notificationGutsManager.openGuts(view, i, i2, menuItem);
            }
        };
    }

    @VisibleForTesting
    protected Map<NotificationData.Entry, NotificationLifetimeExtender> getRetainedNotificationMap() {
        return this.mRetainedNotifications;
    }

    public NotificationData getNotificationData() {
        return this.mNotificationData;
    }

    public ExpandableNotificationRow.LongPressListener getNotificationLongClicker() {
        final NotificationGutsManager notificationGutsManager = this.mGutsManager;
        Objects.requireNonNull(notificationGutsManager);
        return new ExpandableNotificationRow.LongPressListener() {
            @Override
            public final boolean onLongPress(View view, int i, int i2, NotificationMenuRowPlugin.MenuItem menuItem) {
                return notificationGutsManager.openGuts(view, i, i2, menuItem);
            }
        };
    }

    @Override
    public void logNotificationExpansion(final String str, final boolean z, final boolean z2) {
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mBarService.onNotificationExpansionChanged(str, z, z2);
            }
        });
    }

    @Override
    public void onReorderingAllowed() {
        updateNotifications();
    }

    private boolean shouldSuppressFullScreenIntent(NotificationData.Entry entry) {
        if (this.mPresenter.isDeviceInVrMode()) {
            return true;
        }
        return this.mNotificationData.shouldSuppressFullScreenIntent(entry);
    }

    private void inflateViews(final NotificationData.Entry entry, ViewGroup viewGroup) {
        final PackageManager packageManagerForUser = StatusBar.getPackageManagerForUser(this.mContext, entry.notification.getUser().getIdentifier());
        final StatusBarNotification statusBarNotification = entry.notification;
        if (entry.row != null) {
            entry.reset();
            updateNotification(entry, packageManagerForUser, statusBarNotification, entry.row);
        } else {
            new RowInflaterTask().inflate(this.mContext, viewGroup, entry, new RowInflaterTask.RowInflationFinishedListener() {
                @Override
                public final void onInflationFinished(ExpandableNotificationRow expandableNotificationRow) {
                    NotificationEntryManager.lambda$inflateViews$2(this.f$0, entry, packageManagerForUser, statusBarNotification, expandableNotificationRow);
                }
            });
        }
    }

    public static void lambda$inflateViews$2(NotificationEntryManager notificationEntryManager, NotificationData.Entry entry, PackageManager packageManager, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow) {
        notificationEntryManager.bindRow(entry, packageManager, statusBarNotification, expandableNotificationRow);
        notificationEntryManager.updateNotification(entry, packageManager, statusBarNotification, expandableNotificationRow);
    }

    private void bindRow(NotificationData.Entry entry, PackageManager packageManager, StatusBarNotification statusBarNotification, final ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setExpansionLogger(this, entry.notification.getKey());
        expandableNotificationRow.setGroupManager(this.mGroupManager);
        expandableNotificationRow.setHeadsUpManager(this.mHeadsUpManager);
        expandableNotificationRow.setOnExpandClickListener(this.mPresenter);
        expandableNotificationRow.setInflationCallback(this);
        expandableNotificationRow.setLongPressListener(getNotificationLongClicker());
        this.mListContainer.bindRow(expandableNotificationRow);
        this.mRemoteInputManager.bindRow(expandableNotificationRow);
        String packageName = statusBarNotification.getPackageName();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 8704);
            if (applicationInfo != null) {
                packageName = String.valueOf(packageManager.getApplicationLabel(applicationInfo));
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
        expandableNotificationRow.setAppName(packageName);
        expandableNotificationRow.setOnDismissRunnable(new Runnable() {
            @Override
            public final void run() {
                this.f$0.performRemoveNotification(expandableNotificationRow.getStatusBarNotification());
            }
        });
        expandableNotificationRow.setDescendantFocusability(393216);
        if (NotificationRemoteInputManager.ENABLE_REMOTE_INPUT) {
            expandableNotificationRow.setDescendantFocusability(131072);
        }
        expandableNotificationRow.setAppOpsOnClickListener(this.mOnAppOpsClickListener);
        this.mCallback.onBindRow(entry, packageManager, statusBarNotification, expandableNotificationRow);
    }

    public void performRemoveNotification(StatusBarNotification statusBarNotification) {
        int i;
        int i2 = 1;
        NotificationVisibility notificationVisibilityObtain = NotificationVisibility.obtain(statusBarNotification.getKey(), this.mNotificationData.getRank(statusBarNotification.getKey()), this.mNotificationData.getActiveNotifications().size(), true);
        NotificationData.Entry entry = this.mNotificationData.get(statusBarNotification.getKey());
        if (NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY && this.mKeysKeptForRemoteInput.contains(statusBarNotification.getKey())) {
            this.mKeysKeptForRemoteInput.remove(statusBarNotification.getKey());
        }
        this.mRemoteInputManager.onPerformRemoveNotification(statusBarNotification, entry);
        String packageName = statusBarNotification.getPackageName();
        String tag = statusBarNotification.getTag();
        int id = statusBarNotification.getId();
        int userId = statusBarNotification.getUserId();
        try {
            if (!isHeadsUp(statusBarNotification.getKey())) {
                if (this.mListContainer.hasPulsingNotifications()) {
                    i2 = 2;
                    i = i2;
                    this.mBarService.onNotificationClear(packageName, tag, id, userId, statusBarNotification.getKey(), i, notificationVisibilityObtain);
                    removeNotification(statusBarNotification.getKey(), null);
                } else {
                    i = 3;
                    this.mBarService.onNotificationClear(packageName, tag, id, userId, statusBarNotification.getKey(), i, notificationVisibilityObtain);
                    removeNotification(statusBarNotification.getKey(), null);
                }
            } else {
                i = i2;
                this.mBarService.onNotificationClear(packageName, tag, id, userId, statusBarNotification.getKey(), i, notificationVisibilityObtain);
                removeNotification(statusBarNotification.getKey(), null);
            }
        } catch (RemoteException e) {
        }
        this.mCallback.onPerformRemoveNotification(statusBarNotification);
    }

    void handleNotificationError(StatusBarNotification statusBarNotification, String str) {
        removeNotification(statusBarNotification.getKey(), null);
        try {
            this.mBarService.onNotificationError(statusBarNotification.getPackageName(), statusBarNotification.getTag(), statusBarNotification.getId(), statusBarNotification.getUid(), statusBarNotification.getInitialPid(), str, statusBarNotification.getUserId());
        } catch (RemoteException e) {
        }
    }

    private void abortExistingInflation(String str) {
        if (this.mPendingNotifications.containsKey(str)) {
            this.mPendingNotifications.get(str).abortTask();
            this.mPendingNotifications.remove(str);
        }
        NotificationData.Entry entry = this.mNotificationData.get(str);
        if (entry != null) {
            entry.abortTask();
        }
    }

    @Override
    public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
        handleNotificationError(statusBarNotification, exc.getMessage());
    }

    private void addEntry(NotificationData.Entry entry) {
        boolean zShouldPeek = shouldPeek(entry);
        if (CHATTY) {
            StringBuilder sb = new StringBuilder();
            sb.append("addEntry, isHeadsUped: ");
            sb.append(zShouldPeek);
            sb.append(", key: ");
            sb.append(entry != null ? entry.key : null);
            Log.d("NotificationEntryMgr", sb.toString());
        }
        if (zShouldPeek) {
            this.mHeadsUpManager.showNotification(entry);
            setNotificationShown(entry.notification);
        }
        addNotificationViews(entry);
        this.mCallback.onNotificationAdded(entry);
    }

    @Override
    public void onAsyncInflationFinished(NotificationData.Entry entry) {
        this.mPendingNotifications.remove(entry.key);
        boolean z = this.mNotificationData.get(entry.key) == null;
        if (CHATTY) {
            StringBuilder sb = new StringBuilder();
            sb.append("onAsyncInflationFinished, isNew: ");
            sb.append(z);
            sb.append(", removed: ");
            sb.append(entry.row.isRemoved());
            sb.append(", key: ");
            sb.append(entry != null ? entry.key : null);
            Log.d("NotificationEntryMgr", sb.toString());
        }
        if (z && !entry.row.isRemoved()) {
            addEntry(entry);
        } else if (!z && entry.row.hasLowPriorityStateUpdated()) {
            this.mVisualStabilityManager.onLowPriorityUpdated(entry);
            this.mPresenter.updateNotificationViews();
        }
        entry.row.setLowPriorityStateUpdated(false);
    }

    public void removeNotification(String str, NotificationListenerService.RankingMap rankingMap) {
        boolean z;
        boolean z2;
        boolean z3;
        NotificationData.Entry entry = this.mPendingNotifications.get(str);
        if (entry != null && this.mFGSExtender.shouldExtendLifetimeForPendingNotification(entry)) {
            extendLifetime(entry, this.mFGSExtender);
            return;
        }
        abortExistingInflation(str);
        boolean z4 = true;
        if (this.mHeadsUpManager.isHeadsUp(str)) {
            z = !this.mHeadsUpManager.removeNotification(str, (this.mRemoteInputManager.getController().isSpinning(str) && !NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY) || !this.mVisualStabilityManager.isReorderingAllowed());
        } else {
            z = false;
        }
        this.mMediaManager.onNotificationRemoved(str);
        NotificationData.Entry entry2 = this.mNotificationData.get(str);
        if (NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY && shouldKeepForRemoteInput(entry2) && entry2.row != null && !entry2.row.isDismissed()) {
            CharSequence charSequence = entry2.remoteInputText;
            if (TextUtils.isEmpty(charSequence)) {
                charSequence = entry2.remoteInputTextWhenReset;
            }
            StatusBarNotification statusBarNotificationRebuildNotificationWithRemoteInput = rebuildNotificationWithRemoteInput(entry2, charSequence, false);
            entry2.onRemoteInputInserted();
            try {
                updateNotificationInternal(statusBarNotificationRebuildNotificationWithRemoteInput, null);
                z3 = z;
                z2 = true;
            } catch (InflationException e) {
                z2 = false;
                z3 = false;
            }
            if (z2) {
                Log.w("NotificationEntryMgr", "Keeping notification around after sending remote input " + entry2.key);
                addKeyKeptForRemoteInput(entry2.key);
                return;
            }
            z = z3;
        }
        if (NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY && shouldKeepForSmartReply(entry2) && entry2.row != null && !entry2.row.isDismissed()) {
            try {
                updateNotificationInternal(rebuildNotificationForCanceledSmartReplies(entry2), null);
            } catch (InflationException e2) {
                z4 = false;
            }
            this.mSmartReplyController.stopSending(entry2);
            if (z4) {
                Log.w("NotificationEntryMgr", "Keeping notification around after sending smart reply " + entry2.key);
                addKeyKeptForRemoteInput(entry2.key);
                return;
            }
        }
        if (entry2 != null && this.mFGSExtender.shouldExtendLifetime(entry2)) {
            extendLifetime(entry2, this.mFGSExtender);
            return;
        }
        this.mSmartReplyController.stopSending(entry2);
        if (z) {
            this.mLatestRankingMap = rankingMap;
            this.mHeadsUpEntriesToRemoveOnSwitch.add(this.mHeadsUpManager.getEntry(str));
            return;
        }
        if (this.mRemoteInputManager.onRemoveNotification(entry2)) {
            this.mLatestRankingMap = rankingMap;
            return;
        }
        if (entry2 != null && this.mGutsManager.getExposedGuts() != null && this.mGutsManager.getExposedGuts() == entry2.row.getGuts() && entry2.row.getGuts() != null && !entry2.row.getGuts().isLeavebehind()) {
            Log.w("NotificationEntryMgr", "Keeping notification because it's showing guts. " + str);
            this.mLatestRankingMap = rankingMap;
            this.mGutsManager.setKeyToRemoveOnGutsClosed(str);
            return;
        }
        if (entry2 != null) {
            this.mForegroundServiceController.removeNotification(entry2.notification);
        }
        if (entry2 != null && entry2.row != null) {
            entry2.row.setRemoved();
            this.mListContainer.cleanUpViewState(entry2.row);
        }
        handleGroupSummaryRemoved(str);
        StatusBarNotification statusBarNotificationRemoveNotificationViews = removeNotificationViews(str, rankingMap);
        cancelLifetimeExtension(entry2);
        this.mCallback.onNotificationRemoved(str, statusBarNotificationRemoveNotificationViews);
    }

    private void extendLifetime(NotificationData.Entry entry, NotificationLifetimeExtender notificationLifetimeExtender) {
        NotificationLifetimeExtender notificationLifetimeExtender2 = this.mRetainedNotifications.get(entry);
        if (notificationLifetimeExtender2 != null && notificationLifetimeExtender2 != notificationLifetimeExtender) {
            notificationLifetimeExtender2.setShouldManageLifetime(entry, false);
        }
        this.mRetainedNotifications.put(entry, notificationLifetimeExtender);
        notificationLifetimeExtender.setShouldManageLifetime(entry, true);
    }

    private void cancelLifetimeExtension(NotificationData.Entry entry) {
        NotificationLifetimeExtender notificationLifetimeExtenderRemove = this.mRetainedNotifications.remove(entry);
        if (notificationLifetimeExtenderRemove != null) {
            notificationLifetimeExtenderRemove.setShouldManageLifetime(entry, false);
        }
    }

    public StatusBarNotification rebuildNotificationWithRemoteInput(NotificationData.Entry entry, CharSequence charSequence, boolean z) {
        CharSequence[] charSequenceArr;
        StatusBarNotification statusBarNotification = entry.notification;
        Notification.Builder builderRecoverBuilder = Notification.Builder.recoverBuilder(this.mContext, statusBarNotification.getNotification().clone());
        if (charSequence != null) {
            CharSequence[] charSequenceArray = statusBarNotification.getNotification().extras.getCharSequenceArray("android.remoteInputHistory");
            if (charSequenceArray != null) {
                CharSequence[] charSequenceArr2 = new CharSequence[charSequenceArray.length + 1];
                System.arraycopy(charSequenceArray, 0, charSequenceArr2, 1, charSequenceArray.length);
                charSequenceArr = charSequenceArr2;
            } else {
                charSequenceArr = new CharSequence[1];
            }
            charSequenceArr[0] = String.valueOf(charSequence);
            builderRecoverBuilder.setRemoteInputHistory(charSequenceArr);
        }
        builderRecoverBuilder.setShowRemoteInputSpinner(z);
        builderRecoverBuilder.setHideSmartReplies(true);
        Notification notificationBuild = builderRecoverBuilder.build();
        notificationBuild.contentView = statusBarNotification.getNotification().contentView;
        notificationBuild.bigContentView = statusBarNotification.getNotification().bigContentView;
        notificationBuild.headsUpContentView = statusBarNotification.getNotification().headsUpContentView;
        return new StatusBarNotification(statusBarNotification.getPackageName(), statusBarNotification.getOpPkg(), statusBarNotification.getId(), statusBarNotification.getTag(), statusBarNotification.getUid(), statusBarNotification.getInitialPid(), notificationBuild, statusBarNotification.getUser(), statusBarNotification.getOverrideGroupKey(), statusBarNotification.getPostTime());
    }

    @VisibleForTesting
    StatusBarNotification rebuildNotificationForCanceledSmartReplies(NotificationData.Entry entry) {
        return rebuildNotificationWithRemoteInput(entry, null, false);
    }

    private boolean shouldKeepForSmartReply(NotificationData.Entry entry) {
        return entry != null && this.mSmartReplyController.isSendingSmartReply(entry.key);
    }

    private boolean shouldKeepForRemoteInput(NotificationData.Entry entry) {
        if (entry == null) {
            return false;
        }
        return this.mRemoteInputManager.getController().isSpinning(entry.key) || entry.hasJustSentRemoteInput();
    }

    private StatusBarNotification removeNotificationViews(String str, NotificationListenerService.RankingMap rankingMap) {
        NotificationData.Entry entryRemove = this.mNotificationData.remove(str, rankingMap);
        if (entryRemove == null) {
            Log.w("NotificationEntryMgr", "removeNotification for unknown key: " + str);
            return null;
        }
        updateNotifications();
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackGarbage(entryRemove);
        return entryRemove.notification;
    }

    private void handleGroupSummaryRemoved(String str) {
        NotificationData.Entry entry = this.mNotificationData.get(str);
        if (entry != null && entry.row != null && entry.row.isSummaryWithChildren()) {
            if (entry.notification.getOverrideGroupKey() != null && !entry.row.isDismissed()) {
                return;
            }
            List<ExpandableNotificationRow> notificationChildren = entry.row.getNotificationChildren();
            for (int i = 0; i < notificationChildren.size(); i++) {
                ExpandableNotificationRow expandableNotificationRow = notificationChildren.get(i);
                NotificationData.Entry entry2 = expandableNotificationRow.getEntry();
                boolean z = (expandableNotificationRow.getStatusBarNotification().getNotification().flags & 64) != 0;
                boolean z2 = NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY && (shouldKeepForRemoteInput(entry2) || shouldKeepForSmartReply(entry2));
                if (!z && !z2) {
                    expandableNotificationRow.setKeepInParent(true);
                    expandableNotificationRow.setRemoved();
                }
            }
        }
    }

    public void updateNotificationsOnDensityOrFontScaleChanged() {
        ArrayList<NotificationData.Entry> notificationsForCurrentUser = this.mNotificationData.getNotificationsForCurrentUser();
        for (int i = 0; i < notificationsForCurrentUser.size(); i++) {
            NotificationData.Entry entry = notificationsForCurrentUser.get(i);
            boolean z = this.mGutsManager.getExposedGuts() != null && entry.row.getGuts() == this.mGutsManager.getExposedGuts();
            entry.row.onDensityOrFontScaleChanged();
            if (z) {
                this.mGutsManager.onDensityOrFontScaleChanged(entry.row);
            }
        }
    }

    protected void updateNotification(NotificationData.Entry entry, PackageManager packageManager, StatusBarNotification statusBarNotification, ExpandableNotificationRow expandableNotificationRow) {
        expandableNotificationRow.setNeedsRedaction(this.mLockscreenUserManager.needsRedaction(entry));
        boolean zIsAmbient = this.mNotificationData.isAmbient(statusBarNotification.getKey());
        boolean z = this.mNotificationData.get(entry.key) != null;
        boolean zIsLowPriority = expandableNotificationRow.isLowPriority();
        expandableNotificationRow.setIsLowPriority(zIsAmbient);
        expandableNotificationRow.setLowPriorityStateUpdated(z && zIsLowPriority != zIsAmbient);
        this.mNotificationClicker.register(expandableNotificationRow, statusBarNotification);
        try {
            entry.targetSdk = packageManager.getApplicationInfo(statusBarNotification.getPackageName(), 0).targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("NotificationEntryMgr", "Failed looking up ApplicationInfo for " + statusBarNotification.getPackageName(), e);
        }
        expandableNotificationRow.setLegacy(entry.targetSdk >= 9 && entry.targetSdk < 21);
        entry.setIconTag(R.id.icon_is_pre_L, Boolean.valueOf(entry.targetSdk < 21));
        entry.autoRedacted = entry.notification.getNotification().publicVersion == null;
        entry.row = expandableNotificationRow;
        entry.row.setOnActivatedListener(this.mPresenter);
        boolean zIsImportantMessaging = this.mMessagingUtil.isImportantMessaging(statusBarNotification, this.mNotificationData.getImportance(statusBarNotification.getKey()));
        boolean z2 = zIsImportantMessaging && !this.mPresenter.isPresenterFullyCollapsed();
        expandableNotificationRow.setUseIncreasedCollapsedHeight(zIsImportantMessaging);
        expandableNotificationRow.setUseIncreasedHeadsUpHeight(z2);
        expandableNotificationRow.updateNotification(entry);
    }

    protected void addNotificationViews(NotificationData.Entry entry) {
        if (entry == null) {
            return;
        }
        this.mNotificationData.add(entry);
        tagForeground(entry.notification);
        updateNotifications();
    }

    protected NotificationData.Entry createNotificationViews(StatusBarNotification statusBarNotification) throws InflationException {
        if (CHATTY) {
            Log.d("NotificationEntryMgr", "createNotificationViews(notification=" + statusBarNotification);
        }
        NotificationData.Entry entry = new NotificationData.Entry(statusBarNotification);
        ((LeakDetector) Dependency.get(LeakDetector.class)).trackInstance(entry);
        entry.createIcons(this.mContext, statusBarNotification);
        inflateViews(entry, this.mListContainer.getViewParentForNotification(entry));
        return entry;
    }

    private void addNotificationInternal(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) throws InflationException {
        String key = statusBarNotification.getKey();
        if (CHATTY) {
            Log.d("NotificationEntryMgr", "addNotification key=" + key);
        }
        this.mNotificationData.updateRanking(rankingMap);
        NotificationData.Entry entryCreateNotificationViews = createNotificationViews(statusBarNotification);
        if (!shouldPeek(entryCreateNotificationViews) && statusBarNotification.getNotification().fullScreenIntent != null) {
            if (shouldSuppressFullScreenIntent(entryCreateNotificationViews)) {
                if (CHATTY) {
                    Log.d("NotificationEntryMgr", "No Fullscreen intent: suppressed by DND: " + key);
                }
            } else if (this.mNotificationData.getImportance(key) < 4) {
                if (CHATTY) {
                    Log.d("NotificationEntryMgr", "No Fullscreen intent: not important enough: " + key);
                }
            } else {
                SystemServicesProxy.getInstance(this.mContext).awakenDreamsAsync();
                if (CHATTY) {
                    Log.d("NotificationEntryMgr", "Notification has fullScreenIntent; sending fullScreenIntent");
                }
                try {
                    EventLog.writeEvent(36002, key);
                    statusBarNotification.getNotification().fullScreenIntent.send();
                    entryCreateNotificationViews.notifyFullScreenIntentLaunched();
                    this.mMetricsLogger.count("note_fullscreen", 1);
                } catch (PendingIntent.CanceledException e) {
                }
            }
        }
        abortExistingInflation(key);
        this.mForegroundServiceController.addNotification(statusBarNotification, this.mNotificationData.getImportance(key));
        this.mPendingNotifications.put(key, entryCreateNotificationViews);
        this.mGroupManager.onPendingEntryAdded(entryCreateNotificationViews);
    }

    @VisibleForTesting
    protected void tagForeground(StatusBarNotification statusBarNotification) {
        ArraySet<Integer> appOps = this.mForegroundServiceController.getAppOps(statusBarNotification.getUserId(), statusBarNotification.getPackageName());
        if (appOps != null) {
            int size = appOps.size();
            for (int i = 0; i < size; i++) {
                updateNotificationsForAppOp(appOps.valueAt(i).intValue(), statusBarNotification.getUid(), statusBarNotification.getPackageName(), true);
            }
        }
    }

    public void addNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        try {
            addNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    public void updateNotificationsForAppOp(int i, int i2, String str, boolean z) {
        String standardLayoutKey = this.mForegroundServiceController.getStandardLayoutKey(UserHandle.getUserId(i2), str);
        if (standardLayoutKey != null) {
            this.mNotificationData.updateAppOp(i, i2, str, standardLayoutKey, z);
            updateNotifications();
        }
    }

    private boolean alertAgain(NotificationData.Entry entry, Notification notification) {
        return entry == null || !entry.hasInterrupted() || (notification.flags & 8) == 0;
    }

    private void updateNotificationInternal(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) throws InflationException {
        if (CHATTY) {
            Log.d("NotificationEntryMgr", "updateNotification(" + statusBarNotification + ")");
        }
        String key = statusBarNotification.getKey();
        abortExistingInflation(key);
        NotificationData.Entry entry = this.mNotificationData.get(key);
        if (entry == null) {
            if (CHATTY) {
                Log.d("NotificationEntryMgr", "updateNotification, entry == null, (" + statusBarNotification + ")");
                return;
            }
            return;
        }
        this.mHeadsUpEntriesToRemoveOnSwitch.remove(entry);
        this.mRemoteInputManager.onUpdateNotification(entry);
        this.mSmartReplyController.stopSending(entry);
        if (key.equals(this.mGutsManager.getKeyToRemoveOnGutsClosed())) {
            this.mGutsManager.setKeyToRemoveOnGutsClosed(null);
            Log.w("NotificationEntryMgr", "Notification that was kept for guts was updated. " + key);
        }
        cancelLifetimeExtension(entry);
        Notification notification = statusBarNotification.getNotification();
        this.mNotificationData.updateRanking(rankingMap);
        StatusBarNotification statusBarNotification2 = entry.notification;
        entry.notification = statusBarNotification;
        this.mGroupManager.onEntryUpdated(entry, statusBarNotification2);
        entry.updateIcons(this.mContext, statusBarNotification);
        inflateViews(entry, this.mListContainer.getViewParentForNotification(entry));
        this.mForegroundServiceController.updateNotification(statusBarNotification, this.mNotificationData.getImportance(key));
        updateHeadsUp(key, entry, shouldPeek(entry, statusBarNotification), alertAgain(entry, notification));
        updateNotifications();
        if (!statusBarNotification.isClearable()) {
            this.mListContainer.snapViewIfNeeded(entry.row);
        }
        this.mCallback.onNotificationUpdated(statusBarNotification);
    }

    public void updateNotification(StatusBarNotification statusBarNotification, NotificationListenerService.RankingMap rankingMap) {
        try {
            updateNotificationInternal(statusBarNotification, rankingMap);
        } catch (InflationException e) {
            handleInflationException(statusBarNotification, e);
        }
    }

    public void updateNotifications() {
        this.mNotificationData.filterAndSort();
        this.mPresenter.updateNotificationViews();
    }

    public void updateNotificationRanking(NotificationListenerService.RankingMap rankingMap) {
        this.mNotificationData.updateRanking(rankingMap);
        updateNotifications();
    }

    protected boolean shouldPeek(NotificationData.Entry entry) {
        return shouldPeek(entry, entry.notification);
    }

    public boolean shouldPeek(NotificationData.Entry entry, StatusBarNotification statusBarNotification) {
        if (!this.mUseHeadsUp || this.mPresenter.isDeviceInVrMode() || this.mNotificationData.shouldFilterOut(entry)) {
            return false;
        }
        if (!(this.mPowerManager.isScreenOn() && !this.mSystemServicesProxy.isDreaming()) && !this.mPresenter.isDozing()) {
            return false;
        }
        if (!this.mPresenter.isDozing() && this.mNotificationData.shouldSuppressPeek(entry)) {
            return false;
        }
        if ((this.mPresenter.isDozing() && this.mNotificationData.shouldSuppressAmbient(entry)) || entry.hasJustLaunchedFullScreenIntent() || isSnoozedPackage(statusBarNotification)) {
            return false;
        }
        if (this.mNotificationData.getImportance(statusBarNotification.getKey()) < (this.mPresenter.isDozing() ? 3 : 4)) {
            return false;
        }
        return !(statusBarNotification.isGroup() && statusBarNotification.getNotification().suppressAlertingDueToGrouping()) && this.mCallback.shouldPeek(entry, statusBarNotification);
    }

    protected void setNotificationShown(StatusBarNotification statusBarNotification) {
        setNotificationsShown(new String[]{statusBarNotification.getKey()});
    }

    protected void setNotificationsShown(String[] strArr) {
        try {
            this.mNotificationListener.setNotificationsShown(strArr);
        } catch (RuntimeException e) {
            Log.d("NotificationEntryMgr", "failed setNotificationsShown: ", e);
        }
    }

    protected boolean isSnoozedPackage(StatusBarNotification statusBarNotification) {
        return this.mHeadsUpManager.isSnoozed(statusBarNotification.getPackageName());
    }

    protected void updateHeadsUp(String str, NotificationData.Entry entry, boolean z, boolean z2) {
        if (isHeadsUp(str)) {
            if (!z) {
                if (CHATTY) {
                    Log.d("NotificationEntryMgr", "updateHeadsUp, removeNotification: " + str);
                }
                this.mHeadsUpManager.removeNotification(str, false);
                return;
            }
            if (CHATTY) {
                Log.d("NotificationEntryMgr", "updateHeadsUp, updateNotification: " + str);
            }
            this.mHeadsUpManager.updateNotification(entry, z2);
            return;
        }
        if (z && z2) {
            if (CHATTY) {
                Log.d("NotificationEntryMgr", "updateHeadsUp, showNotification: " + str);
            }
            this.mHeadsUpManager.showNotification(entry);
        }
    }

    protected boolean isHeadsUp(String str) {
        return this.mHeadsUpManager.isHeadsUp(str);
    }

    public boolean isNotificationKeptForRemoteInput(String str) {
        return this.mKeysKeptForRemoteInput.contains(str);
    }

    public void removeKeyKeptForRemoteInput(String str) {
        this.mKeysKeptForRemoteInput.remove(str);
    }

    public void addKeyKeptForRemoteInput(String str) {
        if (NotificationRemoteInputManager.FORCE_REMOTE_INPUT_HISTORY) {
            this.mKeysKeptForRemoteInput.add(str);
        }
    }
}
