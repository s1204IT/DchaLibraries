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

    @Override
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

    @Override
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

    @Override
    public String getTileSpec() {
        return this.mTileSpec;
    }

    @Override
    public void setTileSpec(String str) {
        this.mTileSpec = str;
    }

    public QSHost getHost() {
        return this.mHost;
    }

    @Override
    public QSIconView createTileView(Context context) {
        return new QSIconViewImpl(context);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return null;
    }

    protected DetailAdapter createDetailAdapter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void addCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(1, callback).sendToTarget();
    }

    @Override
    public void removeCallback(QSTile.Callback callback) {
        this.mHandler.obtainMessage(13, callback).sendToTarget();
    }

    @Override
    public void removeCallbacks() {
        this.mHandler.sendEmptyMessage(12);
    }

    @Override
    public void click() {
        this.mMetricsLogger.write(populate(new LogMaker(925).setType(4)));
        this.mHandler.sendEmptyMessage(2);
    }

    @Override
    public void secondaryClick() {
        this.mMetricsLogger.write(populate(new LogMaker(926).setType(4)));
        this.mHandler.sendEmptyMessage(3);
    }

    @Override
    public void longClick() {
        this.mMetricsLogger.write(populate(new LogMaker(366).setType(4)));
        this.mHandler.sendEmptyMessage(4);
        Prefs.putInt(this.mContext, "QsLongPressTooltipShownCount", 2);
    }

    @Override
    public LogMaker populate(LogMaker logMaker) {
        if (this.mState instanceof QSTile.BooleanState) {
            logMaker.addTaggedData(928, Integer.valueOf(((QSTile.BooleanState) this.mState).value ? 1 : 0));
        }
        return logMaker.setSubtype(getMetricsCategory()).addTaggedData(833, Integer.valueOf(this.mIsFullQs)).addTaggedData(927, Integer.valueOf(this.mHost.indexOf(this.mTileSpec)));
    }

    public void showDetail(boolean z) {
        this.mHandler.obtainMessage(6, z ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void refreshState() {
        refreshState(null);
    }

    protected final void refreshState(Object obj) {
        this.mHandler.obtainMessage(5, obj).sendToTarget();
    }

    @Override
    public void clearState() {
        this.mHandler.sendEmptyMessage(11);
    }

    @Override
    public void userSwitch(int i) {
        this.mHandler.obtainMessage(7, i, 0).sendToTarget();
    }

    public void fireToggleStateChanged(boolean z) {
        this.mHandler.obtainMessage(8, z ? 1 : 0, 0).sendToTarget();
    }

    public void fireScanStateChanged(boolean z) {
        this.mHandler.obtainMessage(9, z ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void destroy() {
        this.mHandler.sendEmptyMessage(10);
    }

    @Override
    public TState getState() {
        return this.mState;
    }

    @Override
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

        @Override
        public void handleMessage(Message message) {
            ?? r1;
            boolean z;
            ?? r0 = 0;
            try {
                r1 = ((Message) message).what;
                z = true;
                try {
                } catch (Throwable th) {
                    th = th;
                    r0 = r1;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            if (r1 == 1) {
                String str = "handleAddCallback";
                QSTileImpl.this.handleAddCallback((QSTile.Callback) ((Message) message).obj);
                r1 = str;
            } else {
                try {
                } catch (Throwable th3) {
                    r0 = message;
                    th = th3;
                }
                if (((Message) message).what == 12) {
                    String str2 = "handleRemoveCallbacks";
                    QSTileImpl.this.handleRemoveCallbacks();
                    message = str2;
                } else if (((Message) message).what == 13) {
                    String str3 = "handleRemoveCallback";
                    QSTileImpl.this.handleRemoveCallback((QSTile.Callback) ((Message) message).obj);
                    r1 = str3;
                } else if (((Message) message).what == 2) {
                    String str4 = "handleClick";
                    if (QSTileImpl.this.mState.disabledByPolicy) {
                        ((ActivityStarter) Dependency.get(ActivityStarter.class)).postStartActivityDismissingKeyguard(RestrictedLockUtils.getShowAdminSupportDetailsIntent(QSTileImpl.this.mContext, QSTileImpl.this.mEnforcedAdmin), 0);
                        message = str4;
                    } else {
                        QSTileImpl.this.handleClick();
                        message = str4;
                    }
                } else if (((Message) message).what == 3) {
                    String str5 = "handleSecondaryClick";
                    QSTileImpl.this.handleSecondaryClick();
                    message = str5;
                } else if (((Message) message).what == 4) {
                    String str6 = "handleLongClick";
                    QSTileImpl.this.handleLongClick();
                    message = str6;
                } else if (((Message) message).what == 5) {
                    String str7 = "handleRefreshState";
                    QSTileImpl.this.handleRefreshState(((Message) message).obj);
                    r1 = str7;
                } else if (((Message) message).what == 6) {
                    String str8 = "handleShowDetail";
                    QSTileImpl qSTileImpl = QSTileImpl.this;
                    if (((Message) message).arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl.handleShowDetail(z);
                    r1 = str8;
                } else if (((Message) message).what == 7) {
                    String str9 = "handleUserSwitch";
                    QSTileImpl.this.handleUserSwitch(((Message) message).arg1);
                    r1 = str9;
                } else if (((Message) message).what == 8) {
                    String str10 = "handleToggleStateChanged";
                    QSTileImpl qSTileImpl2 = QSTileImpl.this;
                    if (((Message) message).arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl2.handleToggleStateChanged(z);
                    r1 = str10;
                } else if (((Message) message).what == 9) {
                    String str11 = "handleScanStateChanged";
                    QSTileImpl qSTileImpl3 = QSTileImpl.this;
                    if (((Message) message).arg1 == 0) {
                        z = false;
                    }
                    qSTileImpl3.handleScanStateChanged(z);
                    r1 = str11;
                } else if (((Message) message).what == 10) {
                    String str12 = "handleDestroy";
                    QSTileImpl.this.handleDestroy();
                    message = str12;
                } else if (((Message) message).what == 11) {
                    String str13 = "handleClearState";
                    QSTileImpl.this.handleClearState();
                    message = str13;
                } else {
                    if (((Message) message).what == 14) {
                        String str14 = "handleSetListeningInternal";
                        QSTileImpl qSTileImpl4 = QSTileImpl.this;
                        Object obj = ((Message) message).obj;
                        if (((Message) message).arg1 == 0) {
                            z = false;
                        }
                        qSTileImpl4.handleSetListeningInternal(obj, z);
                        r1 = str14;
                    } else if (((Message) message).what == 15) {
                        String str15 = "handleStale";
                        QSTileImpl.this.handleStale();
                        message = str15;
                    } else {
                        throw new IllegalArgumentException("Unknown msg: " + ((Message) message).what);
                    }
                    String str16 = "Error in " + r0;
                    Log.w(QSTileImpl.this.TAG, str16, th);
                    QSTileImpl.this.mHost.warn(str16, th);
                }
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

        @Override
        public Drawable getDrawable(Context context) {
            return this.mDrawable;
        }

        @Override
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

        @Override
        public Drawable getDrawable(Context context) {
            return context.getDrawable(this.mResId);
        }

        @Override
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
