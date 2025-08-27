package com.android.systemui.qs.tileimpl;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTile.State;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSHost;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public abstract class QSTileImpl<TState extends QSTile.State> implements QSTile {
    protected static final Object ARG_SHOW_TRANSIENT_ENABLING;
    protected static final boolean DEBUG;
    private boolean mAnnounceNextStateChange;
    protected final Context mContext;
    private RestrictedLockUtils.EnforcedAdmin mEnforcedAdmin;
    protected final QSHost mHost;
    private int mIsFullQs;
    private boolean mShowingDetail;
    private String mTileSpec;
    protected final String TAG = "Tile." + getClass().getSimpleName();
    protected QSTileImpl<TState>.H mHandler = new H((Looper) Dependency.get(Dependency.BG_LOOPER));
    protected final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private final ArraySet<Object> mListeners = new ArraySet<>();
    private final MetricsLogger mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
    private final ArrayList<QSTile.Callback> mCallbacks = new ArrayList<>();
    private final Object mStaleListener = new Object();
    protected TState mState = (TState) newTileState();
    private TState mTmpState = (TState) newTileState();

    public abstract Intent getLongClickIntent();

    @Override // com.android.systemui.plugins.qs.QSTile
    public abstract int getMetricsCategory();

    protected abstract void handleClick();

    protected abstract void handleSetListening(boolean z);

    protected abstract void handleUpdateState(TState tstate, Object obj);

    public abstract TState newTileState();

    static {
        DEBUG = Log.isLoggable("Tile", 3) || FeatureOptions.LOG_ENABLE;
        ARG_SHOW_TRANSIENT_ENABLING = new Object();
    }

    protected QSTileImpl(QSHost qSHost) {
        this.mHost = qSHost;
        this.mContext = qSHost.getContext();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setListening(Object obj, boolean z) {
        this.mHandler.obtainMessage(14, z ? 1 : 0, 0, obj).sendToTarget();
    }

    protected long getStaleTimeout() {
        return 600000L;
    }

    @VisibleForTesting
    protected void handleStale() {
        setListening(this.mStaleListener, true);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public String getTileSpec() {
        return this.mTileSpec;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setTileSpec(String str) {
        this.mTileSpec = str;
    }

    public QSHost getHost() {
        return this.mHost;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public QSIconView createTileView(Context context) {
        return new QSIconViewImpl(context);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public DetailAdapter getDetailAdapter() {
        return null;
    }

    protected DetailAdapter createDetailAdapter() {
        throw new UnsupportedOperationException();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public boolean isAvailable() {
        return true;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void addCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void removeCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(13, callback).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(12);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void click() {
        this.mMetricsLogger.write(populate(new LogMaker(925).setType(4)));
        this.mHandler.sendEmptyMessage(2);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void secondaryClick() {
        this.mMetricsLogger.write(populate(new LogMaker(926).setType(4)));
        this.mHandler.sendEmptyMessage(3);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void longClick() {
        this.mMetricsLogger.write(populate(new LogMaker(366).setType(4)));
        this.mHandler.sendEmptyMessage(4);
        Prefs.putInt(this.mContext, "QsLongPressTooltipShownCount", 2);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public LogMaker populate(LogMaker logMaker) {
        if (this.mState instanceof QSTile.BooleanState) {
            logMaker.addTaggedData(928, Integer.valueOf(((QSTile.BooleanState) this.mState).value ? 1 : 0));
        }
        return logMaker.setSubtype(getMetricsCategory()).addTaggedData(833, Integer.valueOf(this.mIsFullQs)).addTaggedData(927, Integer.valueOf(this.mHost.indexOf(this.mTileSpec)));
    }

    public void showDetail(boolean z) {
        this.mHandler.obtainMessage(6, z ? 1 : 0, 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void refreshState() {
        refreshState(null);
    }

    protected final void refreshState(Object obj) {
        this.mHandler.obtainMessage(5, obj).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void clearState() {
        this.mHandler.sendEmptyMessage(11);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void userSwitch(int i) {
        this.mHandler.obtainMessage(7, i, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean z) {
        this.mHandler.obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean z) {
        this.mHandler.obtainMessage(9, z ? 1 : 0, 0).sendToTarget();
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public TState getState() {
        return this.mState;
    }

    @Override // com.android.systemui.plugins.qs.QSTile
    public void setDetailListening(boolean z) {
    }

    private void handleAddCallback(QSTile.Callback callback) {
        this.mCallbacks.add(callback);
        callback.onStateChanged(this.mState);
    }

    private void handleRemoveCallback(QSTile.Callback callback) {
        this.mCallbacks.remove(callback);
    }

    private void handleRemoveCallbacks() {
        this.mCallbacks.clear();
    }

    protected void handleSecondaryClick() {
        handleClick();
    }

    protected void handleLongClick() {
        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(getLongClickIntent(), 0);
    }

    protected void handleClearState() {
        this.mTmpState = (TState) newTileState();
        this.mState = (TState) newTileState();
    }

    protected void handleRefreshState(Object obj) {
        handleUpdateState(this.mTmpState, obj);
        if (this.mTmpState.copyTo(this.mState)) {
            handleStateChanged();
        }
        this.mHandler.removeMessages(15);
        this.mHandler.sendEmptyMessageDelayed(15, getStaleTimeout());
        setListening(this.mStaleListener, false);
    }

    private void handleStateChanged() {
        String strComposeChangeAnnouncement;
        boolean zShouldAnnouncementBeDelayed = shouldAnnouncementBeDelayed();
        boolean z = false;
        if (this.mCallbacks.size() != 0) {
            QSTile.State stateNewTileState = newTileState();
            this.mState.copyTo(stateNewTileState);
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                this.mCallbacks.get(i).onStateChanged(stateNewTileState);
            }
            if (this.mAnnounceNextStateChange && !zShouldAnnouncementBeDelayed && (strComposeChangeAnnouncement = composeChangeAnnouncement()) != null) {
                this.mCallbacks.get(0).onAnnouncementRequested(strComposeChangeAnnouncement);
            }
        }
        if (this.mAnnounceNextStateChange && zShouldAnnouncementBeDelayed) {
            z = true;
        }
        this.mAnnounceNextStateChange = z;
    }

    protected boolean shouldAnnouncementBeDelayed() {
        return false;
    }

    protected String composeChangeAnnouncement() {
        return null;
    }

    private void handleShowDetail(boolean z) {
        this.mShowingDetail = z;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onShowDetail(z);
        }
    }

    protected boolean isShowingDetail() {
        return this.mShowingDetail;
    }

    private void handleToggleStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onToggleStateChanged(z);
        }
    }

    private void handleScanStateChanged(boolean z) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            this.mCallbacks.get(i).onScanStateChanged(z);
        }
    }

    protected void handleUserSwitch(int i) {
        handleRefreshState(null);
    }

    private void handleSetListeningInternal(Object obj, boolean z) {
        if (z) {
            if (this.mListeners.add(obj) && this.mListeners.size() == 1) {
                if (DEBUG) {
                    Log.d(this.TAG, "handleSetListening true");
                }
                handleSetListening(z);
                refreshState();
            }
        } else if (this.mListeners.remove(obj) && this.mListeners.size() == 0) {
            if (DEBUG) {
                Log.d(this.TAG, "handleSetListening false");
            }
            handleSetListening(z);
        }
        updateIsFullQs();
    }

    private void updateIsFullQs() {
        Iterator<Object> it = this.mListeners.iterator();
        while (it.hasNext()) {
            if (PagedTileLayout.TilePage.class.equals(it.next().getClass())) {
                this.mIsFullQs = 1;
                return;
            }
        }
        this.mIsFullQs = 0;
    }

    protected void handleDestroy() {
        if (this.mListeners.size() != 0) {
            handleSetListening(false);
        }
        this.mCallbacks.clear();
    }

    protected void checkIfRestrictionEnforcedByAdminOnly(QSTile.State state, String str) {
        RestrictedLockUtils.EnforcedAdmin enforcedAdminCheckIfRestrictionEnforced = RestrictedLockUtils.checkIfRestrictionEnforced(this.mContext, str, ActivityManager.getCurrentUser());
        if (enforcedAdminCheckIfRestrictionEnforced != null && !RestrictedLockUtils.hasBaseUserRestriction(this.mContext, str, ActivityManager.getCurrentUser())) {
            state.disabledByPolicy = true;
            this.mEnforcedAdmin = enforcedAdminCheckIfRestrictionEnforced;
        } else {
            state.disabledByPolicy = false;
            this.mEnforcedAdmin = null;
        }
    }

    public static int getColorForState(Context context, int i) {
        switch (i) {
            case 0:
                return Utils.getDisabled(context, Utils.getColorAttr(context, R.attr.textColorSecondary));
            case 1:
                return Utils.getColorAttr(context, R.attr.textColorSecondary);
            case 2:
                return Utils.getColorAttr(context, R.attr.colorPrimary);
            default:
                Log.e("QSTile", "Invalid state " + i);
                return 0;
        }
    }

    protected final class H extends Handler {
        @VisibleForTesting
        protected H(Looper looper) {
            super(looper);
        }

        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:225:0x0005 */
        /* JADX DEBUG: Failed to insert an additional move for type inference into block B:228:0x001c */
        /* JADX WARN: Multi-variable type inference failed */
        /* JADX WARN: Type inference failed for: r0v0 */
        /* JADX WARN: Type inference failed for: r0v1, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r0v3 */
        /* JADX WARN: Type inference failed for: r0v5 */
        /* JADX WARN: Type inference failed for: r1v0, types: [java.lang.StringBuilder] */
        /* JADX WARN: Type inference failed for: r1v34 */
        /* JADX WARN: Type inference failed for: r1v36 */
        /* JADX WARN: Type inference failed for: r1v37 */
        /* JADX WARN: Type inference failed for: r1v38 */
        /* JADX WARN: Type inference failed for: r1v39 */
        /* JADX WARN: Type inference failed for: r1v40 */
        /* JADX WARN: Type inference failed for: r1v41 */
        /* JADX WARN: Type inference failed for: r1v42 */
        /* JADX WARN: Type inference failed for: r1v43 */
        /* JADX WARN: Type inference failed for: r1v5, types: [int] */
        /* JADX WARN: Type inference failed for: r1v6 */
        /* JADX WARN: Type inference failed for: r7v10, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v11, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v17, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v18, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v19, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v23, types: [java.lang.String] */
        /* JADX WARN: Type inference failed for: r7v8, types: [java.lang.String] */
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            ?? r0 = null;
            try {
                ?? r1 = message.what;
                boolean z = true;
                try {
                    if (r1 == 1) {
                        String str = "handleAddCallback";
                        QSTileImpl.this.handleAddCallback((QSTile.Callback) message.obj);
                        r1 = str;
                    } else {
                        try {
                            if (message.what == 12) {
                                ?? r7 = "handleRemoveCallbacks";
                                QSTileImpl.this.handleRemoveCallbacks();
                                message = r7;
                            } else if (message.what == 13) {
                                String str2 = "handleRemoveCallback";
                                QSTileImpl.this.handleRemoveCallback((QSTile.Callback) message.obj);
                                r1 = str2;
                            } else if (message.what == 2) {
                                ?? r72 = "handleClick";
                                if (QSTileImpl.this.mState.disabledByPolicy) {
                                    ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(QSTileImpl.this.mContext, QSTileImpl.this.mEnforcedAdmin), 0);
                                    message = r72;
                                } else {
                                    QSTileImpl.this.handleClick();
                                    message = r72;
                                }
                            } else if (message.what == 3) {
                                ?? r73 = "handleSecondaryClick";
                                QSTileImpl.this.handleSecondaryClick();
                                message = r73;
                            } else if (message.what == 4) {
                                ?? r74 = "handleLongClick";
                                QSTileImpl.this.handleLongClick();
                                message = r74;
                            } else if (message.what == 5) {
                                String str3 = "handleRefreshState";
                                QSTileImpl.this.handleRefreshState(message.obj);
                                r1 = str3;
                            } else if (message.what == 6) {
                                String str4 = "handleShowDetail";
                                QSTileImpl qSTileImpl = QSTileImpl.this;
                                if (message.arg1 == 0) {
                                    z = false;
                                }
                                qSTileImpl.handleShowDetail(z);
                                r1 = str4;
                            } else if (message.what == 7) {
                                String str5 = "handleUserSwitch";
                                QSTileImpl.this.handleUserSwitch(message.arg1);
                                r1 = str5;
                            } else if (message.what == 8) {
                                String str6 = "handleToggleStateChanged";
                                QSTileImpl qSTileImpl2 = QSTileImpl.this;
                                if (message.arg1 == 0) {
                                    z = false;
                                }
                                qSTileImpl2.handleToggleStateChanged(z);
                                r1 = str6;
                            } else if (message.what == 9) {
                                String str7 = "handleScanStateChanged";
                                QSTileImpl qSTileImpl3 = QSTileImpl.this;
                                if (message.arg1 == 0) {
                                    z = false;
                                }
                                qSTileImpl3.handleScanStateChanged(z);
                                r1 = str7;
                            } else if (message.what == 10) {
                                ?? r75 = "handleDestroy";
                                QSTileImpl.this.handleDestroy();
                                message = r75;
                            } else if (message.what == 11) {
                                ?? r76 = "handleClearState";
                                QSTileImpl.this.handleClearState();
                                message = r76;
                            } else if (message.what == 14) {
                                String str8 = "handleSetListeningInternal";
                                QSTileImpl qSTileImpl4 = QSTileImpl.this;
                                Object obj = message.obj;
                                if (message.arg1 == 0) {
                                    z = false;
                                }
                                qSTileImpl4.handleSetListeningInternal(obj, z);
                                r1 = str8;
                            } else if (message.what == 15) {
                                ?? r77 = "handleStale";
                                QSTileImpl.this.handleStale();
                                message = r77;
                            } else {
                                throw new IllegalArgumentException("Unknown msg: " + message.what);
                            }
                        } catch (Throwable th) {
                            r0 = message;
                            th = th;
                            String str9 = "Error in " + r0;
                            Log.w(QSTileImpl.this.TAG, str9, th);
                            QSTileImpl.this.mHost.warn(str9, th);
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    r0 = r1;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public static class DrawableIcon extends QSTile.Icon {
        protected final Drawable mDrawable;
        protected final Drawable mInvisibleDrawable;

        public DrawableIcon(Drawable drawable) {
            this.mDrawable = drawable;
            this.mInvisibleDrawable = drawable.getConstantState().newDrawable();
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getInvisibleDrawable(Context context) {
            return this.mInvisibleDrawable;
        }
    }

    public static class ResourceIcon extends QSTile.Icon {
        private static final SparseArray<QSTile.Icon> ICONS = new SparseArray<>();
        protected final int mResId;

        private ResourceIcon(int i) {
            this.mResId = i;
        }

        public static QSTile.Icon get(int i) {
            QSTile.Icon icon = ICONS.get(i);
            if (icon == null) {
                ResourceIcon resourceIcon = new ResourceIcon(i);
                ICONS.put(i, resourceIcon);
                return resourceIcon;
            }
            return icon;
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        @Override // com.android.systemui.plugins.qs.QSTile.Icon
        public Drawable getInvisibleDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        public boolean equals(Object obj) {
            return (obj instanceof ResourceIcon) && ((ResourceIcon) obj).mResId == this.mResId;
        }

        public String toString() {
            return String.format("ResourceIcon[resId=0x%08x]", Integer.valueOf(this.mResId));
        }
    }
}
