package com.android.systemui.qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.PluginManager;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import java.util.ArrayList;
import java.util.Collection;

public class QSPanel extends LinearLayout implements TunerService.Tunable, QSTile.Host.Callback {
    private BrightnessController mBrightnessController;
    private BrightnessMirrorController mBrightnessMirrorController;
    private int mBrightnessPaddingTop;
    protected final View mBrightnessView;
    private Callback mCallback;
    protected final Context mContext;
    private QSCustomizer mCustomizePanel;
    private Record mDetailRecord;
    protected boolean mExpanded;
    protected QSFooter mFooter;
    private boolean mGridContentVisible;
    private final H mHandler;
    protected QSTileHost mHost;
    protected boolean mListening;
    private int mPanelPaddingBottom;
    private IQuickSettingsPlugin mQuickSettingsPlugin;
    protected final ArrayList<TileRecord> mRecords;
    protected QSTileLayout mTileLayout;

    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(QSTile.DetailAdapter detailAdapter, int i, int i2);

        void onToggleStateChanged(boolean z);
    }

    public interface QSTileLayout {
        void addTile(TileRecord tileRecord);

        int getOffsetTop(TileRecord tileRecord);

        void removeTile(TileRecord tileRecord);

        void setListening(boolean z);

        boolean updateResources();
    }

    public static final class TileRecord extends Record {
        public QSTile.Callback callback;
        public boolean scanState;
        public QSTile<?> tile;
        public QSTileBaseView tileView;
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList<>();
        this.mHandler = new H(this, null);
        this.mGridContentVisible = true;
        this.mContext = context;
        setOrientation(1);
        this.mBrightnessView = LayoutInflater.from(context).inflate(R.layout.quick_settings_brightness_dialog, (ViewGroup) this, false);
        addView(this.mBrightnessView);
        this.mQuickSettingsPlugin = PluginManager.getQuickSettingsPlugin(this.mContext);
        this.mQuickSettingsPlugin.addOpViews(this);
        setupTileLayout();
        this.mFooter = new QSFooter(this, context);
        addView(this.mFooter.getView());
        updateResources();
        this.mBrightnessController = new BrightnessController(getContext(), (ImageView) findViewById(R.id.brightness_icon), (ToggleSlider) findViewById(R.id.brightness_slider));
    }

    protected void setupTileLayout() {
        this.mTileLayout = (QSTileLayout) LayoutInflater.from(this.mContext).inflate(R.layout.qs_paged_tile_layout, (ViewGroup) this, false);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout);
        findViewById(android.R.id.edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                QSPanel.this.m897com_android_systemui_qs_QSPanel_lambda$1(arg0);
            }
        });
    }

    void m897com_android_systemui_qs_QSPanel_lambda$1(final View view) {
        this.mHost.startRunnableDismissingKeyguard(new Runnable() {
            @Override
            public void run() {
                this.val$this.m898com_android_systemui_qs_QSPanel_lambda$2(view);
            }
        });
    }

    public boolean isShowingCustomize() {
        if (this.mCustomizePanel != null) {
            return this.mCustomizePanel.isCustomizing();
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TunerService.get(this.mContext).addTunable(this, "qs_show_brightness");
        if (this.mHost == null) {
            return;
        }
        setTiles(this.mHost.getTiles());
    }

    @Override
    protected void onDetachedFromWindow() {
        TunerService.get(this.mContext).removeTunable(this);
        this.mHost.removeCallback(this);
        for (TileRecord record : this.mRecords) {
            record.tile.removeCallbacks();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onTilesChanged() {
        setTiles(this.mHost.getTiles());
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        int i = 0;
        if (!"qs_show_brightness".equals(key)) {
            return;
        }
        View view = this.mBrightnessView;
        if (newValue != null && Integer.parseInt(newValue) == 0) {
            i = 8;
        }
        view.setVisibility(i);
    }

    public void openDetails(String subPanel) {
        QSTile<?> tile = getTile(subPanel);
        showDetailAdapter(true, tile.getDetailAdapter(), new int[]{getWidth() / 2, 0});
    }

    private QSTile<?> getTile(String subPanel) {
        for (int i = 0; i < this.mRecords.size(); i++) {
            if (subPanel.equals(this.mRecords.get(i).tile.getTileSpec())) {
                return this.mRecords.get(i).tile;
            }
        }
        return this.mHost.createTile(subPanel);
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        this.mBrightnessMirrorController = c;
        ToggleSlider brightnessSlider = (ToggleSlider) findViewById(R.id.brightness_slider);
        ToggleSlider mirror = (ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider);
        brightnessSlider.setMirror(mirror);
        brightnessSlider.setMirrorController(c);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost host, QSCustomizer customizer) {
        this.mHost = host;
        this.mHost.addCallback(this);
        setTiles(this.mHost.getTiles());
        this.mFooter.setHost(host);
        this.mCustomizePanel = customizer;
        if (this.mCustomizePanel == null) {
            return;
        }
        this.mCustomizePanel.setHost(this.mHost);
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Resources res = this.mContext.getResources();
        this.mPanelPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom);
        this.mBrightnessPaddingTop = res.getDimensionPixelSize(R.dimen.qs_brightness_padding_top);
        setPadding(0, this.mBrightnessPaddingTop, 0, this.mPanelPaddingBottom);
        for (TileRecord r : this.mRecords) {
            r.tile.clearState();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        if (this.mTileLayout == null) {
            return;
        }
        this.mTileLayout.updateResources();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.mFooter.onConfigurationChanged();
        if (this.mBrightnessMirrorController == null) {
            return;
        }
        setBrightnessMirror(this.mBrightnessMirrorController);
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded == expanded) {
            return;
        }
        this.mExpanded = expanded;
        if (!this.mExpanded && (this.mTileLayout instanceof PagedTileLayout)) {
            ((PagedTileLayout) this.mTileLayout).setCurrentItem(0, false);
        }
        MetricsLogger.visibility(this.mContext, 111, this.mExpanded);
        if (!this.mExpanded) {
            closeDetail();
        } else {
            logTiles();
        }
    }

    public void setListening(boolean listening) {
        if (this.mListening == listening) {
            return;
        }
        this.mListening = listening;
        if (this.mTileLayout != null) {
            this.mTileLayout.setListening(listening);
        }
        this.mFooter.setListening(this.mListening);
        if (this.mListening) {
            refreshAllTiles();
        }
        if (listening) {
            this.mBrightnessController.registerCallbacks();
            this.mQuickSettingsPlugin.registerCallbacks();
        } else {
            this.mBrightnessController.unregisterCallbacks();
            this.mQuickSettingsPlugin.unregisterCallbacks();
        }
    }

    public void refreshAllTiles() {
        for (TileRecord r : this.mRecords) {
            r.tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean show, QSTile.DetailAdapter adapter, int[] locationInWindow) {
        int xInWindow = locationInWindow[0];
        int yInWindow = locationInWindow[1];
        ((View) getParent()).getLocationInWindow(locationInWindow);
        Record r = new Record();
        r.detailAdapter = adapter;
        r.x = xInWindow - locationInWindow[0];
        r.y = yInWindow - locationInWindow[1];
        locationInWindow[0] = xInWindow;
        locationInWindow[1] = yInWindow;
        showDetail(show, r);
    }

    protected void showDetail(boolean show, Record r) {
        this.mHandler.obtainMessage(1, show ? 1 : 0, 0, r).sendToTarget();
    }

    public void setTiles(Collection<QSTile<?>> tiles) {
        setTiles(tiles, false);
    }

    public void setTiles(Collection<QSTile<?>> tiles, boolean collapsedView) {
        for (TileRecord record : this.mRecords) {
            this.mTileLayout.removeTile(record);
            record.tile.removeCallback(record.callback);
        }
        this.mRecords.clear();
        for (QSTile<?> tile : tiles) {
            addTile(tile, collapsedView);
        }
    }

    protected void drawTile(TileRecord r, QSTile.State state) {
        r.tileView.onStateChanged(state);
    }

    protected QSTileBaseView createTileView(QSTile<?> tile, boolean collapsedView) {
        return new QSTileView(this.mContext, tile.createTileView(this.mContext), collapsedView);
    }

    protected boolean shouldShowDetail() {
        return this.mExpanded;
    }

    protected void addTile(QSTile<?> tile, boolean collapsedView) {
        final TileRecord r = new TileRecord();
        r.tile = tile;
        r.tileView = createTileView(tile, collapsedView);
        QSTile.Callback callback = new QSTile.Callback() {
            @Override
            public void onStateChanged(QSTile.State state) {
                QSPanel.this.drawTile(r, state);
            }

            @Override
            public void onShowDetail(boolean show) {
                if (!QSPanel.this.shouldShowDetail()) {
                    return;
                }
                QSPanel.this.showDetail(show, r);
            }

            @Override
            public void onToggleStateChanged(boolean state) {
                if (QSPanel.this.mDetailRecord != r) {
                    return;
                }
                QSPanel.this.fireToggleStateChanged(state);
            }

            @Override
            public void onScanStateChanged(boolean state) {
                r.scanState = state;
                if (QSPanel.this.mDetailRecord != r) {
                    return;
                }
                QSPanel.this.fireScanStateChanged(r.scanState);
            }

            @Override
            public void onAnnouncementRequested(CharSequence announcement) {
                QSPanel.this.announceForAccessibility(announcement);
            }
        };
        r.tile.addCallback(callback);
        r.callback = callback;
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QSPanel.this.onTileClick(r.tile);
            }
        };
        View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                r.tile.longClick();
                return true;
            }
        };
        r.tileView.init(click, longClick);
        r.tile.refreshState();
        this.mRecords.add(r);
        if (this.mTileLayout == null) {
            return;
        }
        this.mTileLayout.addTile(r);
    }

    public void m898com_android_systemui_qs_QSPanel_lambda$2(final View v) {
        v.post(new Runnable() {
            @Override
            public void run() {
                if (QSPanel.this.mCustomizePanel == null || QSPanel.this.mCustomizePanel.isCustomizing()) {
                    return;
                }
                int[] loc = new int[2];
                v.getLocationInWindow(loc);
                int x = loc[0];
                int y = loc[1];
                QSPanel.this.mCustomizePanel.show(x, y);
            }
        });
    }

    protected void onTileClick(QSTile<?> tile) {
        tile.click();
    }

    public void closeDetail() {
        if (this.mCustomizePanel != null && this.mCustomizePanel.isCustomizing()) {
            this.mCustomizePanel.hide(this.mCustomizePanel.getWidth() / 2, this.mCustomizePanel.getHeight() / 2);
        } else {
            showDetail(false, this.mDetailRecord);
        }
    }

    public int getGridHeight() {
        return getMeasuredHeight();
    }

    protected void handleShowDetail(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowDetailTile((TileRecord) r, show);
            return;
        }
        int x = 0;
        int y = 0;
        if (r != null) {
            x = r.x;
            y = r.y;
        }
        handleShowDetailImpl(r, show, x, y);
    }

    private void handleShowDetailTile(TileRecord r, boolean show) {
        if ((this.mDetailRecord != null) == show && this.mDetailRecord == r) {
            return;
        }
        if (show) {
            r.detailAdapter = r.tile.getDetailAdapter();
            if (r.detailAdapter == null) {
                return;
            }
        }
        r.tile.setDetailListening(show);
        int x = r.tileView.getLeft() + (r.tileView.getWidth() / 2);
        int y = r.tileView.getTop() + this.mTileLayout.getOffsetTop(r) + (r.tileView.getHeight() / 2) + getTop();
        handleShowDetailImpl(r, show, x, y);
    }

    private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
        setDetailRecord(show ? r : null);
        fireShowingDetail(show ? r.detailAdapter : null, x, y);
    }

    private void setDetailRecord(Record r) {
        boolean scanState;
        if (r == this.mDetailRecord) {
            return;
        }
        this.mDetailRecord = r;
        if (!(this.mDetailRecord instanceof TileRecord)) {
            scanState = false;
        } else {
            scanState = ((TileRecord) this.mDetailRecord).scanState;
        }
        fireScanStateChanged(scanState);
    }

    void setGridContentVisibility(boolean visible) {
        int newVis = visible ? 0 : 4;
        setVisibility(newVis);
        this.mQuickSettingsPlugin.setViewsVisibility(newVis);
        if (this.mGridContentVisible != visible) {
            MetricsLogger.visibility(this.mContext, 111, newVis);
        }
        this.mGridContentVisible = visible;
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            TileRecord tileRecord = this.mRecords.get(i);
            MetricsLogger.visible(this.mContext, tileRecord.tile.getMetricsCategory());
        }
    }

    private void fireShowingDetail(QSTile.DetailAdapter detail, int x, int y) {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onShowingDetail(detail, x, y);
    }

    public void fireToggleStateChanged(boolean state) {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onToggleStateChanged(state);
    }

    public void fireScanStateChanged(boolean state) {
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.onScanStateChanged(state);
    }

    public void clickTile(ComponentName tile) {
        String spec = CustomTile.toSpec(tile);
        int N = this.mRecords.size();
        for (int i = 0; i < N; i++) {
            if (this.mRecords.get(i).tile.getTileSpec().equals(spec)) {
                this.mRecords.get(i).tile.click();
                return;
            }
        }
    }

    QSTileLayout getTileLayout() {
        return this.mTileLayout;
    }

    QSTileBaseView getTileView(QSTile<?> tile) {
        for (TileRecord r : this.mRecords) {
            if (r.tile == tile) {
                return r.tileView;
            }
        }
        return null;
    }

    private class H extends Handler {
        H(QSPanel this$0, H h) {
            this();
        }

        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                return;
            }
            QSPanel.this.handleShowDetail((Record) msg.obj, msg.arg1 != 0);
        }
    }

    protected static class Record {
        QSTile.DetailAdapter detailAdapter;
        int x;
        int y;

        protected Record() {
        }
    }
}
