package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.InflationTask;
import com.android.systemui.statusbar.NotificationContentView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.util.Assert;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/* loaded from: classes.dex */
public class NotificationInflater {
    private static final InflationExecutor EXECUTOR = new InflationExecutor();

    @VisibleForTesting
    static final int FLAG_REINFLATE_ALL = -1;

    @VisibleForTesting
    static final int FLAG_REINFLATE_EXPANDED_VIEW = 2;
    private InflationCallback mCallback;
    private boolean mIsChildInGroup;
    private boolean mIsLowPriority;
    private boolean mRedactAmbient;
    private RemoteViews.OnClickHandler mRemoteViewClickHandler;
    private final ExpandableNotificationRow mRow;
    private boolean mUsesIncreasedHeadsUpHeight;
    private boolean mUsesIncreasedHeight;

    public NotificationInflater(ExpandableNotificationRow expandableNotificationRow) {
        this.mRow = expandableNotificationRow;
    }

    public void setIsLowPriority(boolean z) {
        this.mIsLowPriority = z;
    }

    public void setIsChildInGroup(boolean z) {
        if (z != this.mIsChildInGroup) {
            this.mIsChildInGroup = z;
            if (this.mIsLowPriority) {
                inflateNotificationViews(3);
            }
        }
    }

    public void setUsesIncreasedHeight(boolean z) {
        this.mUsesIncreasedHeight = z;
    }

    public void setUsesIncreasedHeadsUpHeight(boolean z) {
        this.mUsesIncreasedHeadsUpHeight = z;
    }

    public void setRemoteViewClickHandler(RemoteViews.OnClickHandler onClickHandler) {
        this.mRemoteViewClickHandler = onClickHandler;
    }

    public void setRedactAmbient(boolean z) {
        if (this.mRedactAmbient != z) {
            this.mRedactAmbient = z;
            if (this.mRow.getEntry() == null) {
                return;
            }
            inflateNotificationViews(16);
        }
    }

    public void inflateNotificationViews() {
        inflateNotificationViews(FLAG_REINFLATE_ALL);
    }

    @VisibleForTesting
    void inflateNotificationViews(int i) {
        if (this.mRow.isRemoved()) {
            return;
        }
        AsyncInflationTask asyncInflationTask = new AsyncInflationTask(this.mRow.getEntry().notification, i, this.mRow, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, this.mCallback, this.mRemoteViewClickHandler);
        if (this.mCallback != null && this.mCallback.doInflateSynchronous()) {
            asyncInflationTask.onPostExecute(asyncInflationTask.doInBackground(new Void[0]));
        } else {
            asyncInflationTask.execute(new Void[0]);
        }
    }

    @VisibleForTesting
    InflationProgress inflateNotificationViews(int i, Notification.Builder builder, Context context) {
        InflationProgress inflationProgressCreateRemoteViews = createRemoteViews(i, builder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, context);
        apply(inflationProgressCreateRemoteViews, i, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, null);
        return inflationProgressCreateRemoteViews;
    }

    private static InflationProgress createRemoteViews(int i, Notification.Builder builder, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, Context context) {
        InflationProgress inflationProgress = new InflationProgress();
        boolean z6 = z && !z2;
        if ((i & 1) != 0) {
            inflationProgress.newContentView = createContentView(builder, z6, z3);
        }
        if ((i & 2) != 0) {
            inflationProgress.newExpandedView = createExpandedView(builder, z6);
        }
        if ((i & 4) != 0) {
            inflationProgress.newHeadsUpView = builder.createHeadsUpContentView(z4);
        }
        if ((i & 8) != 0) {
            inflationProgress.newPublicView = builder.makePublicContentView();
        }
        if ((i & 16) != 0) {
            inflationProgress.newAmbientView = z5 ? builder.makePublicAmbientNotification() : builder.makeAmbientNotification();
        }
        inflationProgress.packageContext = context;
        inflationProgress.headsUpStatusBarText = builder.getHeadsUpStatusBarText(false);
        inflationProgress.headsUpStatusBarTextPublic = builder.getHeadsUpStatusBarText(true);
        return inflationProgress;
    }

    public static CancellationSignal apply(final InflationProgress inflationProgress, int i, ExpandableNotificationRow expandableNotificationRow, boolean z, RemoteViews.OnClickHandler onClickHandler, InflationCallback inflationCallback) {
        HashMap map;
        NotificationContentView notificationContentView;
        NotificationContentView notificationContentView2;
        NotificationData.Entry entry;
        NotificationContentView notificationContentView3;
        NotificationData.Entry entry2;
        boolean z2;
        NotificationContentView notificationContentView4;
        NotificationData.Entry entry3;
        NotificationContentView notificationContentView5;
        NotificationData.Entry entry4;
        boolean z3;
        NotificationData.Entry entry5;
        boolean z4;
        NotificationData.Entry entry6 = expandableNotificationRow.getEntry();
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        HashMap map2 = new HashMap();
        if ((i & 1) != 0) {
            map = map2;
            notificationContentView = publicLayout;
            notificationContentView2 = privateLayout;
            entry = entry6;
            applyRemoteView(inflationProgress, i, 1, expandableNotificationRow, z, !canReapplyRemoteView(inflationProgress.newContentView, entry6.cachedContentView), onClickHandler, inflationCallback, entry6, privateLayout, privateLayout.getContractedChild(), privateLayout.getVisibleWrapper(0), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.1
                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public void setResultView(View view) {
                    inflationProgress.inflatedContentView = view;
                }

                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return inflationProgress.newContentView;
                }
            });
        } else {
            map = map2;
            notificationContentView = publicLayout;
            notificationContentView2 = privateLayout;
            entry = entry6;
        }
        if ((i & 2) != 0 && inflationProgress.newExpandedView != null) {
            NotificationData.Entry entry7 = entry;
            NotificationContentView notificationContentView6 = notificationContentView2;
            notificationContentView3 = notificationContentView6;
            z2 = true;
            entry2 = entry7;
            applyRemoteView(inflationProgress, i, 2, expandableNotificationRow, z, !canReapplyRemoteView(inflationProgress.newExpandedView, entry7.cachedBigContentView), onClickHandler, inflationCallback, entry7, notificationContentView6, notificationContentView6.getExpandedChild(), notificationContentView6.getVisibleWrapper(1), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.2
                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public void setResultView(View view) {
                    inflationProgress.inflatedExpandedView = view;
                }

                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return inflationProgress.newExpandedView;
                }
            });
        } else {
            notificationContentView3 = notificationContentView2;
            entry2 = entry;
            z2 = true;
        }
        if ((i & 4) != 0 && inflationProgress.newHeadsUpView != null) {
            NotificationData.Entry entry8 = entry2;
            NotificationContentView notificationContentView7 = notificationContentView3;
            notificationContentView4 = notificationContentView7;
            entry3 = entry8;
            applyRemoteView(inflationProgress, i, 4, expandableNotificationRow, z, !canReapplyRemoteView(inflationProgress.newHeadsUpView, entry8.cachedHeadsUpContentView), onClickHandler, inflationCallback, entry8, notificationContentView7, notificationContentView7.getHeadsUpChild(), notificationContentView7.getVisibleWrapper(2), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.3
                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public void setResultView(View view) {
                    inflationProgress.inflatedHeadsUpView = view;
                }

                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return inflationProgress.newHeadsUpView;
                }
            });
        } else {
            notificationContentView4 = notificationContentView3;
            entry3 = entry2;
        }
        if ((i & 8) != 0) {
            NotificationData.Entry entry9 = entry3;
            NotificationContentView notificationContentView8 = notificationContentView;
            z3 = false;
            notificationContentView5 = notificationContentView8;
            entry4 = entry9;
            applyRemoteView(inflationProgress, i, 8, expandableNotificationRow, z, !canReapplyRemoteView(inflationProgress.newPublicView, entry9.cachedPublicContentView), onClickHandler, inflationCallback, entry9, notificationContentView8, notificationContentView8.getContractedChild(), notificationContentView8.getVisibleWrapper(0), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.4
                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public void setResultView(View view) {
                    inflationProgress.inflatedPublicView = view;
                }

                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return inflationProgress.newPublicView;
                }
            });
        } else {
            notificationContentView5 = notificationContentView;
            entry4 = entry3;
            z3 = false;
        }
        if ((i & 16) != 0) {
            NotificationContentView notificationContentView9 = z ? notificationContentView5 : notificationContentView4;
            if (!canReapplyAmbient(expandableNotificationRow, z)) {
                entry5 = entry4;
            } else {
                entry5 = entry4;
                if (canReapplyRemoteView(inflationProgress.newAmbientView, entry5.cachedAmbientContentView)) {
                    z4 = z3;
                }
                applyRemoteView(inflationProgress, i, 16, expandableNotificationRow, z, z4, onClickHandler, inflationCallback, entry5, notificationContentView9, notificationContentView9.getAmbientChild(), notificationContentView9.getVisibleWrapper(4), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.5
                    @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                    public void setResultView(View view) {
                        inflationProgress.inflatedAmbientView = view;
                    }

                    @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                    public RemoteViews getRemoteView() {
                        return inflationProgress.newAmbientView;
                    }
                });
            }
            z4 = z2;
            applyRemoteView(inflationProgress, i, 16, expandableNotificationRow, z, z4, onClickHandler, inflationCallback, entry5, notificationContentView9, notificationContentView9.getAmbientChild(), notificationContentView9.getVisibleWrapper(4), map, new ApplyCallback() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.5
                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public void setResultView(View view) {
                    inflationProgress.inflatedAmbientView = view;
                }

                @Override // com.android.systemui.statusbar.notification.NotificationInflater.ApplyCallback
                public RemoteViews getRemoteView() {
                    return inflationProgress.newAmbientView;
                }
            });
        }
        final HashMap map3 = map;
        finishIfDone(inflationProgress, i, map3, inflationCallback, expandableNotificationRow, z);
        CancellationSignal cancellationSignal = new CancellationSignal();
        cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$NotificationInflater$YqX46rBxwrjWs1TkaBTYm0eniC8
            @Override // android.os.CancellationSignal.OnCancelListener
            public final void onCancel() {
                map3.values().forEach($$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
            }
        });
        return cancellationSignal;
    }

    @VisibleForTesting
    static void applyRemoteView(final InflationProgress inflationProgress, final int i, final int i2, final ExpandableNotificationRow expandableNotificationRow, final boolean z, final boolean z2, final RemoteViews.OnClickHandler onClickHandler, final InflationCallback inflationCallback, final NotificationData.Entry entry, final NotificationContentView notificationContentView, final View view, final NotificationViewWrapper notificationViewWrapper, final HashMap<Integer, CancellationSignal> map, final ApplyCallback applyCallback) {
        CancellationSignal cancellationSignalReapplyAsync;
        final RemoteViews remoteView = applyCallback.getRemoteView();
        if (inflationCallback != null && inflationCallback.doInflateSynchronous()) {
            try {
                if (z2) {
                    View viewApply = remoteView.apply(inflationProgress.packageContext, notificationContentView, onClickHandler);
                    viewApply.setIsRootNamespace(true);
                    applyCallback.setResultView(viewApply);
                } else {
                    remoteView.reapply(inflationProgress.packageContext, view, onClickHandler);
                    notificationViewWrapper.onReinflated();
                }
                return;
            } catch (Exception e) {
                handleInflationError(map, e, entry.notification, inflationCallback);
                map.put(Integer.valueOf(i2), new CancellationSignal());
                return;
            }
        }
        RemoteViews.OnViewAppliedListener onViewAppliedListener = new RemoteViews.OnViewAppliedListener() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.6
            public void onViewApplied(View view2) {
                if (z2) {
                    view2.setIsRootNamespace(true);
                    applyCallback.setResultView(view2);
                } else if (notificationViewWrapper != null) {
                    notificationViewWrapper.onReinflated();
                }
                map.remove(Integer.valueOf(i2));
                NotificationInflater.finishIfDone(inflationProgress, i, map, inflationCallback, expandableNotificationRow, z);
            }

            public void onError(Exception exc) {
                try {
                    View viewApply2 = view;
                    if (z2) {
                        viewApply2 = remoteView.apply(inflationProgress.packageContext, notificationContentView, onClickHandler);
                    } else {
                        remoteView.reapply(inflationProgress.packageContext, view, onClickHandler);
                    }
                    Log.wtf("NotificationInflater", "Async Inflation failed but normal inflation finished normally.", exc);
                    onViewApplied(viewApply2);
                } catch (Exception e2) {
                    map.remove(Integer.valueOf(i2));
                    NotificationInflater.handleInflationError(map, exc, entry.notification, inflationCallback);
                }
            }
        };
        if (z2) {
            cancellationSignalReapplyAsync = remoteView.applyAsync(inflationProgress.packageContext, notificationContentView, EXECUTOR, onViewAppliedListener, onClickHandler);
        } else {
            cancellationSignalReapplyAsync = remoteView.reapplyAsync(inflationProgress.packageContext, view, EXECUTOR, onViewAppliedListener, onClickHandler);
        }
        map.put(Integer.valueOf(i2), cancellationSignalReapplyAsync);
    }

    private static void handleInflationError(HashMap<Integer, CancellationSignal> map, Exception exc, StatusBarNotification statusBarNotification, InflationCallback inflationCallback) {
        Assert.isMainThread();
        map.values().forEach($$Lambda$NotificationInflater$POlPJz26zF5Nt5Z2kVGSqFxN8Co.INSTANCE);
        if (inflationCallback != null) {
            inflationCallback.handleInflationException(statusBarNotification, exc);
        }
    }

    private static boolean finishIfDone(InflationProgress inflationProgress, int i, HashMap<Integer, CancellationSignal> map, InflationCallback inflationCallback, ExpandableNotificationRow expandableNotificationRow, boolean z) {
        Assert.isMainThread();
        NotificationData.Entry entry = expandableNotificationRow.getEntry();
        NotificationContentView privateLayout = expandableNotificationRow.getPrivateLayout();
        NotificationContentView publicLayout = expandableNotificationRow.getPublicLayout();
        if (!map.isEmpty()) {
            return false;
        }
        if ((i & 1) != 0) {
            if (inflationProgress.inflatedContentView != null) {
                privateLayout.setContractedChild(inflationProgress.inflatedContentView);
            }
            entry.cachedContentView = inflationProgress.newContentView;
        }
        if ((i & 2) != 0) {
            if (inflationProgress.inflatedExpandedView != null) {
                privateLayout.setExpandedChild(inflationProgress.inflatedExpandedView);
            } else if (inflationProgress.newExpandedView == null) {
                privateLayout.setExpandedChild(null);
            }
            entry.cachedBigContentView = inflationProgress.newExpandedView;
            expandableNotificationRow.setExpandable(inflationProgress.newExpandedView != null);
        }
        if ((i & 4) != 0) {
            if (inflationProgress.inflatedHeadsUpView != null) {
                privateLayout.setHeadsUpChild(inflationProgress.inflatedHeadsUpView);
            } else if (inflationProgress.newHeadsUpView == null) {
                privateLayout.setHeadsUpChild(null);
            }
            entry.cachedHeadsUpContentView = inflationProgress.newHeadsUpView;
        }
        if ((i & 8) != 0) {
            if (inflationProgress.inflatedPublicView != null) {
                publicLayout.setContractedChild(inflationProgress.inflatedPublicView);
            }
            entry.cachedPublicContentView = inflationProgress.newPublicView;
        }
        if ((i & 16) != 0) {
            if (inflationProgress.inflatedAmbientView != null) {
                NotificationContentView notificationContentView = z ? publicLayout : privateLayout;
                if (!z) {
                    privateLayout = publicLayout;
                }
                notificationContentView.setAmbientChild(inflationProgress.inflatedAmbientView);
                privateLayout.setAmbientChild(null);
            }
            entry.cachedAmbientContentView = inflationProgress.newAmbientView;
        }
        entry.headsUpStatusBarText = inflationProgress.headsUpStatusBarText;
        entry.headsUpStatusBarTextPublic = inflationProgress.headsUpStatusBarTextPublic;
        if (inflationCallback != null) {
            inflationCallback.onAsyncInflationFinished(expandableNotificationRow.getEntry());
        }
        return true;
    }

    private static RemoteViews createExpandedView(Notification.Builder builder, boolean z) {
        RemoteViews remoteViewsCreateBigContentView = builder.createBigContentView();
        if (remoteViewsCreateBigContentView != null) {
            return remoteViewsCreateBigContentView;
        }
        if (z) {
            RemoteViews remoteViewsCreateContentView = builder.createContentView();
            Notification.Builder.makeHeaderExpanded(remoteViewsCreateContentView);
            return remoteViewsCreateContentView;
        }
        return null;
    }

    private static RemoteViews createContentView(Notification.Builder builder, boolean z, boolean z2) {
        if (z) {
            return builder.makeLowPriorityContentView(false);
        }
        return builder.createContentView(z2);
    }

    @VisibleForTesting
    static boolean canReapplyRemoteView(RemoteViews remoteViews, RemoteViews remoteViews2) {
        return (remoteViews == null && remoteViews2 == null) || !(remoteViews == null || remoteViews2 == null || remoteViews2.getPackage() == null || remoteViews.getPackage() == null || !remoteViews.getPackage().equals(remoteViews2.getPackage()) || remoteViews.getLayoutId() != remoteViews2.getLayoutId() || remoteViews2.isReapplyDisallowed());
    }

    public void setInflationCallback(InflationCallback inflationCallback) {
        this.mCallback = inflationCallback;
    }

    public interface InflationCallback {
        void handleInflationException(StatusBarNotification statusBarNotification, Exception exc);

        void onAsyncInflationFinished(NotificationData.Entry entry);

        default boolean doInflateSynchronous() {
            return false;
        }
    }

    public void onDensityOrFontScaleChanged() {
        NotificationData.Entry entry = this.mRow.getEntry();
        entry.cachedAmbientContentView = null;
        entry.cachedBigContentView = null;
        entry.cachedContentView = null;
        entry.cachedHeadsUpContentView = null;
        entry.cachedPublicContentView = null;
        inflateNotificationViews();
    }

    private static boolean canReapplyAmbient(ExpandableNotificationRow expandableNotificationRow, boolean z) {
        return (z ? expandableNotificationRow.getPublicLayout() : expandableNotificationRow.getPrivateLayout()).getAmbientChild() != null;
    }

    public static class AsyncInflationTask extends AsyncTask<Void, Void, InflationProgress> implements InflationTask, InflationCallback {
        private final InflationCallback mCallback;
        private CancellationSignal mCancellationSignal;
        private final Context mContext;
        private Exception mError;
        private final boolean mIsChildInGroup;
        private final boolean mIsLowPriority;
        private int mReInflateFlags;
        private final boolean mRedactAmbient;
        private RemoteViews.OnClickHandler mRemoteViewClickHandler;
        private ExpandableNotificationRow mRow;
        private final StatusBarNotification mSbn;
        private final boolean mUsesIncreasedHeadsUpHeight;
        private final boolean mUsesIncreasedHeight;

        private AsyncInflationTask(StatusBarNotification statusBarNotification, int i, ExpandableNotificationRow expandableNotificationRow, boolean z, boolean z2, boolean z3, boolean z4, boolean z5, InflationCallback inflationCallback, RemoteViews.OnClickHandler onClickHandler) {
            this.mRow = expandableNotificationRow;
            this.mSbn = statusBarNotification;
            this.mReInflateFlags = i;
            this.mContext = this.mRow.getContext();
            this.mIsLowPriority = z;
            this.mIsChildInGroup = z2;
            this.mUsesIncreasedHeight = z3;
            this.mUsesIncreasedHeadsUpHeight = z4;
            this.mRedactAmbient = z5;
            this.mRemoteViewClickHandler = onClickHandler;
            this.mCallback = inflationCallback;
            expandableNotificationRow.getEntry().setInflationTask(this);
        }

        @VisibleForTesting
        public int getReInflateFlags() {
            return this.mReInflateFlags;
        }

        /* JADX DEBUG: Method merged with bridge method: doInBackground([Ljava/lang/Object;)Ljava/lang/Object; */
        @Override // android.os.AsyncTask
        protected InflationProgress doInBackground(Void... voidArr) {
            try {
                Notification.Builder builderRecoverBuilder = Notification.Builder.recoverBuilder(this.mContext, this.mSbn.getNotification());
                Context packageContext = this.mSbn.getPackageContext(this.mContext);
                Notification notification = this.mSbn.getNotification();
                if (notification.isMediaNotification()) {
                    new MediaNotificationProcessor(this.mContext, packageContext).processNotification(notification, builderRecoverBuilder);
                }
                return NotificationInflater.createRemoteViews(this.mReInflateFlags, builderRecoverBuilder, this.mIsLowPriority, this.mIsChildInGroup, this.mUsesIncreasedHeight, this.mUsesIncreasedHeadsUpHeight, this.mRedactAmbient, packageContext);
            } catch (Exception e) {
                this.mError = e;
                return null;
            }
        }

        /* JADX DEBUG: Method merged with bridge method: onPostExecute(Ljava/lang/Object;)V */
        @Override // android.os.AsyncTask
        protected void onPostExecute(InflationProgress inflationProgress) {
            if (this.mError == null) {
                this.mCancellationSignal = NotificationInflater.apply(inflationProgress, this.mReInflateFlags, this.mRow, this.mRedactAmbient, this.mRemoteViewClickHandler, this);
            } else {
                handleError(this.mError);
            }
        }

        private void handleError(Exception exc) {
            this.mRow.getEntry().onInflationTaskFinished();
            StatusBarNotification statusBarNotification = this.mRow.getStatusBarNotification();
            Log.e("StatusBar", "couldn't inflate view for notification " + (statusBarNotification.getPackageName() + "/0x" + Integer.toHexString(statusBarNotification.getId())), exc);
            this.mCallback.handleInflationException(statusBarNotification, new InflationException("Couldn't inflate contentViews" + exc));
        }

        @Override // com.android.systemui.statusbar.InflationTask
        public void abort() {
            cancel(true);
            if (this.mCancellationSignal != null) {
                this.mCancellationSignal.cancel();
            }
        }

        @Override // com.android.systemui.statusbar.InflationTask
        public void supersedeTask(InflationTask inflationTask) {
            if (inflationTask instanceof AsyncInflationTask) {
                this.mReInflateFlags = ((AsyncInflationTask) inflationTask).mReInflateFlags | this.mReInflateFlags;
            }
        }

        @Override // com.android.systemui.statusbar.notification.NotificationInflater.InflationCallback
        public void handleInflationException(StatusBarNotification statusBarNotification, Exception exc) {
            handleError(exc);
        }

        @Override // com.android.systemui.statusbar.notification.NotificationInflater.InflationCallback
        public void onAsyncInflationFinished(NotificationData.Entry entry) {
            this.mRow.getEntry().onInflationTaskFinished();
            this.mRow.onNotificationUpdated();
            this.mCallback.onAsyncInflationFinished(this.mRow.getEntry());
        }

        @Override // com.android.systemui.statusbar.notification.NotificationInflater.InflationCallback
        public boolean doInflateSynchronous() {
            return this.mCallback != null && this.mCallback.doInflateSynchronous();
        }
    }

    @VisibleForTesting
    static class InflationProgress {
        private CharSequence headsUpStatusBarText;
        private CharSequence headsUpStatusBarTextPublic;
        private View inflatedAmbientView;
        private View inflatedContentView;
        private View inflatedExpandedView;
        private View inflatedHeadsUpView;
        private View inflatedPublicView;
        private RemoteViews newAmbientView;
        private RemoteViews newContentView;
        private RemoteViews newExpandedView;
        private RemoteViews newHeadsUpView;
        private RemoteViews newPublicView;

        @VisibleForTesting
        Context packageContext;

        InflationProgress() {
        }
    }

    @VisibleForTesting
    static abstract class ApplyCallback {
        public abstract RemoteViews getRemoteView();

        public abstract void setResultView(View view);

        ApplyCallback() {
        }
    }

    private static class InflationExecutor implements Executor {
        private final ThreadPoolExecutor mExecutor;
        private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
        private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT + NotificationInflater.FLAG_REINFLATE_ALL, 4));
        private static final int MAXIMUM_POOL_SIZE = (CPU_COUNT * 2) + 1;
        private static final ThreadFactory sThreadFactory = new ThreadFactory() { // from class: com.android.systemui.statusbar.notification.NotificationInflater.InflationExecutor.1
            private final AtomicInteger mCount = new AtomicInteger(1);

            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "InflaterThread #" + this.mCount.getAndIncrement());
            }
        };

        private InflationExecutor() {
            this.mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue(), sThreadFactory);
            this.mExecutor.allowCoreThreadTimeOut(true);
        }

        @Override // java.util.concurrent.Executor
        public void execute(Runnable runnable) {
            this.mExecutor.execute(runnable);
        }
    }
}
