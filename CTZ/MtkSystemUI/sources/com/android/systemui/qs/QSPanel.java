package com.android.systemui.qs;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.metrics.LogMaker;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.DetailAdapter;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.PagedTileLayout;
import com.android.systemui.qs.QSDetail;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.qs.external.CustomTile;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.settings.ToggleSliderView;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.ext.IQuickSettingsPlugin;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class QSPanel extends LinearLayout implements QSHost.Callback, BrightnessMirrorController.BrightnessMirrorListener, TunerService.Tunable {
    private BrightnessController mBrightnessController;
    private BrightnessMirrorController mBrightnessMirrorController;
    protected final View mBrightnessView;
    private QSDetail.Callback mCallback;
    protected final Context mContext;
    private QSCustomizer mCustomizePanel;
    private Record mDetailRecord;
    private View mDivider;
    protected boolean mExpanded;
    protected QSSecurityFooter mFooter;
    private PageIndicator mFooterPageIndicator;
    private boolean mGridContentVisible;
    private final H mHandler;
    protected QSTileHost mHost;
    protected boolean mListening;
    private final MetricsLogger mMetricsLogger;
    private PageIndicator mPanelPageIndicator;
    private final QSTileRevealController mQsTileRevealController;
    private IQuickSettingsPlugin mQuickSettingsExt;
    protected final ArrayList<TileRecord> mRecords;
    protected QSTileLayout mTileLayout;

    public static final class TileRecord extends Record {
        public QSTile.Callback callback;
        public boolean scanState;
        public QSTile tile;
        public QSTileView tileView;
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mRecords = new ArrayList<>();
        this.mHandler = new H();
        this.mMetricsLogger = (MetricsLogger) Dependency.get(MetricsLogger.class);
        this.mGridContentVisible = true;
        this.mQuickSettingsExt = null;
        this.mContext = context;
        setOrientation(1);
        this.mBrightnessView = LayoutInflater.from(this.mContext).inflate(R.layout.quick_settings_brightness_dialog, (ViewGroup) this, false);
        addView(this.mBrightnessView);
        this.mQuickSettingsExt = OpSystemUICustomizationFactoryBase.getOpFactory(context).makeQuickSettings(context);
        if (this.mQuickSettingsExt != null) {
            this.mQuickSettingsExt.addOpViews(this);
        }
        this.mTileLayout = (QSTileLayout) LayoutInflater.from(this.mContext).inflate(R.layout.qs_paged_tile_layout, (ViewGroup) this, false);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout);
        this.mPanelPageIndicator = (PageIndicator) LayoutInflater.from(context).inflate(R.layout.qs_page_indicator, (ViewGroup) this, false);
        addView(this.mPanelPageIndicator);
        ((PagedTileLayout) this.mTileLayout).setPageIndicator(this.mPanelPageIndicator);
        this.mQsTileRevealController = new QSTileRevealController(this.mContext, this, (PagedTileLayout) this.mTileLayout);
        addDivider();
        this.mFooter = new QSSecurityFooter(this, context);
        addView(this.mFooter.getView());
        updateResources();
        this.mBrightnessController = new BrightnessController(getContext(), (ImageView) findViewById(R.id.brightness_icon), (ToggleSlider) findViewById(R.id.brightness_slider));
    }

    protected void addDivider() {
        this.mDivider = LayoutInflater.from(this.mContext).inflate(R.layout.qs_divider, (ViewGroup) this, false);
        this.mDivider.setBackgroundColor(Utils.applyAlpha(this.mDivider.getAlpha(), QSTileImpl.getColorForState(this.mContext, 2)));
        addView(this.mDivider);
    }

    public View getDivider() {
        return this.mDivider;
    }

    public View getPageIndicator() {
        return this.mPanelPageIndicator;
    }

    public QSTileRevealController getQsTileRevealController() {
        return this.mQsTileRevealController;
    }

    public boolean isShowingCustomize() {
        return this.mCustomizePanel != null && this.mCustomizePanel.isCustomizing();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this, "qs_show_brightness");
        if (this.mHost != null) {
            setTiles(this.mHost.getTiles());
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.addCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
        if (this.mHost != null) {
            this.mHost.removeCallback(this);
        }
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.removeCallbacks();
        }
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.removeCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onTilesChanged() {
        setTiles(this.mHost.getTiles());
    }

    @Override
    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            updateViewVisibilityForTuningValue(this.mBrightnessView, str2);
        }
    }

    private void updateViewVisibilityForTuningValue(View view, String str) {
        view.setVisibility((str == null || Integer.parseInt(str) != 0) ? 0 : 8);
    }

    public void openDetails(String str) {
        showDetailAdapter(true, getTile(str).getDetailAdapter(), new int[]{getWidth() / 2, 0});
    }

    private QSTile getTile(String str) {
        for (int i = 0; i < this.mRecords.size(); i++) {
            if (str.equals(this.mRecords.get(i).tile.getTileSpec())) {
                return this.mRecords.get(i).tile;
            }
        }
        return this.mHost.createTile(str);
    }

    public void setBrightnessMirror(BrightnessMirrorController brightnessMirrorController) {
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.removeCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        this.mBrightnessMirrorController = brightnessMirrorController;
        if (this.mBrightnessMirrorController != null) {
            this.mBrightnessMirrorController.addCallback((BrightnessMirrorController.BrightnessMirrorListener) this);
        }
        updateBrightnessMirror();
    }

    @Override
    public void onBrightnessMirrorReinflated(View view) {
        updateBrightnessMirror();
    }

    View getBrightnessView() {
        return this.mBrightnessView;
    }

    public void setCallback(QSDetail.Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        this.mHost = qSTileHost;
        this.mHost.addCallback(this);
        setTiles(this.mHost.getTiles());
        this.mFooter.setHostEnvironment(qSTileHost);
        this.mCustomizePanel = qSCustomizer;
        if (this.mCustomizePanel != null) {
            this.mCustomizePanel.setHost(this.mHost);
        }
        this.mQuickSettingsExt.setHostAppInstance(qSTileHost);
    }

    public void setFooterPageIndicator(PageIndicator pageIndicator) {
        if (this.mTileLayout instanceof PagedTileLayout) {
            this.mFooterPageIndicator = pageIndicator;
            updatePageIndicator();
        }
    }

    private void updatePageIndicator() {
        if (this.mTileLayout instanceof PagedTileLayout) {
            boolean z = getResources().getConfiguration().orientation == 2 && this.mFooterPageIndicator != null;
            this.mPanelPageIndicator.setVisibility(8);
            if (this.mFooterPageIndicator != null) {
                this.mFooterPageIndicator.setVisibility(8);
            }
            ((PagedTileLayout) this.mTileLayout).setPageIndicator(z ? this.mFooterPageIndicator : this.mPanelPageIndicator);
        }
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Resources resources = this.mContext.getResources();
        setPadding(0, resources.getDimensionPixelSize(R.dimen.qs_panel_padding_top), 0, resources.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom));
        updatePageIndicator();
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.clearState();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        if (this.mTileLayout != null) {
            this.mTileLayout.updateResources();
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mFooter.onConfigurationChanged();
        updateBrightnessMirror();
    }

    public void updateBrightnessMirror() {
        if (this.mBrightnessMirrorController != null) {
            ToggleSliderView toggleSliderView = (ToggleSliderView) findViewById(R.id.brightness_slider);
            toggleSliderView.setMirror((ToggleSliderView) this.mBrightnessMirrorController.getMirror().findViewById(R.id.brightness_slider));
            toggleSliderView.setMirrorController(this.mBrightnessMirrorController);
        }
    }

    public void setExpanded(boolean z) {
        if (this.mExpanded == z) {
            return;
        }
        this.mExpanded = z;
        if (!this.mExpanded && (this.mTileLayout instanceof PagedTileLayout)) {
            ((PagedTileLayout) this.mTileLayout).setCurrentItem(0, false);
        }
        this.mMetricsLogger.visibility(com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBar, this.mExpanded);
        if (!this.mExpanded) {
            closeDetail();
        } else {
            logTiles();
        }
    }

    public void setPageListener(PagedTileLayout.PageListener pageListener) {
        if (this.mTileLayout instanceof PagedTileLayout) {
            ((PagedTileLayout) this.mTileLayout).setPageListener(pageListener);
        }
    }

    public boolean isExpanded() {
        return this.mExpanded;
    }

    public void setListening(boolean z) {
        if (this.mListening == z) {
            return;
        }
        this.mListening = z;
        if (this.mTileLayout != null) {
            this.mTileLayout.setListening(z);
        }
        this.mFooter.setListening(this.mListening);
        if (this.mListening) {
            refreshAllTiles();
        }
        if (this.mBrightnessView.getVisibility() == 0) {
            if (z) {
                this.mBrightnessController.registerCallbacks();
            } else {
                this.mBrightnessController.unregisterCallbacks();
            }
        }
        if (this.mQuickSettingsExt != null) {
            if (z) {
                this.mQuickSettingsExt.registerCallbacks();
            } else {
                this.mQuickSettingsExt.unregisterCallbacks();
            }
        }
    }

    public void refreshAllTiles() {
        this.mBrightnessController.checkRestrictionAndSetEnabled();
        Iterator<TileRecord> it = this.mRecords.iterator();
        while (it.hasNext()) {
            it.next().tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean z, DetailAdapter detailAdapter, int[] iArr) {
        int i = iArr[0];
        int i2 = iArr[1];
        ((View) getParent()).getLocationInWindow(iArr);
        Record record = new Record();
        record.detailAdapter = detailAdapter;
        record.x = i - iArr[0];
        record.y = i2 - iArr[1];
        iArr[0] = i;
        iArr[1] = i2;
        showDetail(z, record);
    }

    protected void showDetail(boolean z, Record record) {
        this.mHandler.obtainMessage(1, z ? 1 : 0, 0, record).sendToTarget();
    }

    public void setTiles(Collection<QSTile> collection) {
        setTiles(collection, false);
    }

    public void setTiles(Collection<QSTile> collection, boolean z) {
        if (!z) {
            this.mQsTileRevealController.updateRevealedTiles(collection);
        }
        for (TileRecord tileRecord : this.mRecords) {
            this.mTileLayout.removeTile(tileRecord);
            tileRecord.tile.removeCallback(tileRecord.callback);
        }
        this.mRecords.clear();
        Iterator<QSTile> it = collection.iterator();
        while (it.hasNext()) {
            addTile(it.next(), z);
        }
    }

    protected void drawTile(TileRecord tileRecord, QSTile.State state) {
        tileRecord.tileView.onStateChanged(state);
    }

    protected QSTileView createTileView(QSTile qSTile, boolean z) {
        return this.mHost.createTileView(qSTile, z);
    }

    protected boolean shouldShowDetail() {
        return this.mExpanded;
    }

    protected TileRecord addTile(QSTile qSTile, boolean z) {
        final TileRecord tileRecord = new TileRecord();
        tileRecord.tile = qSTile;
        tileRecord.tileView = createTileView(qSTile, z);
        QSTile.Callback callback = new QSTile.Callback() {
            @Override
            public void onStateChanged(QSTile.State state) {
                QSPanel.this.drawTile(tileRecord, state);
            }

            @Override
            public void onShowDetail(boolean z2) {
                if (QSPanel.this.shouldShowDetail()) {
                    QSPanel.this.showDetail(z2, tileRecord);
                }
            }

            @Override
            public void onToggleStateChanged(boolean z2) {
                if (QSPanel.this.mDetailRecord == tileRecord) {
                    QSPanel.this.fireToggleStateChanged(z2);
                }
            }

            @Override
            public void onScanStateChanged(boolean z2) {
                tileRecord.scanState = z2;
                if (QSPanel.this.mDetailRecord == tileRecord) {
                    QSPanel.this.fireScanStateChanged(tileRecord.scanState);
                }
            }

            @Override
            public void onAnnouncementRequested(CharSequence charSequence) {
                if (charSequence != null) {
                    QSPanel.this.mHandler.obtainMessage(3, charSequence).sendToTarget();
                }
            }
        };
        tileRecord.tile.addCallback(callback);
        tileRecord.callback = callback;
        tileRecord.tileView.init(tileRecord.tile);
        tileRecord.tile.refreshState();
        this.mRecords.add(tileRecord);
        if (this.mTileLayout != null) {
            this.mTileLayout.addTile(tileRecord);
        }
        return tileRecord;
    }

    public void showEdit(final View view) {
        view.post(new Runnable() {
            @Override
            public void run() {
                if (QSPanel.this.mCustomizePanel != null && !QSPanel.this.mCustomizePanel.isCustomizing()) {
                    int[] iArr = new int[2];
                    view.getLocationInWindow(iArr);
                    QSPanel.this.mCustomizePanel.show(iArr[0] + (view.getWidth() / 2), iArr[1] + (view.getHeight() / 2));
                }
            }
        });
    }

    public void closeDetail() {
        if (this.mCustomizePanel != null && this.mCustomizePanel.isShown()) {
            this.mCustomizePanel.hide(this.mCustomizePanel.getWidth() / 2, this.mCustomizePanel.getHeight() / 2);
        } else {
            showDetail(false, this.mDetailRecord);
        }
    }

    protected void handleShowDetail(Record record, boolean z) {
        int i;
        if (record instanceof TileRecord) {
            handleShowDetailTile((TileRecord) record, z);
            return;
        }
        int i2 = 0;
        if (record != null) {
            i2 = record.x;
            i = record.y;
        } else {
            i = 0;
        }
        handleShowDetailImpl(record, z, i2, i);
    }

    private void handleShowDetailTile(TileRecord tileRecord, boolean z) {
        if ((this.mDetailRecord != null) == z && this.mDetailRecord == tileRecord) {
            return;
        }
        if (z) {
            tileRecord.detailAdapter = tileRecord.tile.getDetailAdapter();
            if (tileRecord.detailAdapter == null) {
                return;
            }
        }
        tileRecord.tile.setDetailListening(z);
        handleShowDetailImpl(tileRecord, z, tileRecord.tileView.getLeft() + (tileRecord.tileView.getWidth() / 2), tileRecord.tileView.getDetailY() + this.mTileLayout.getOffsetTop(tileRecord) + getTop());
    }

    private void handleShowDetailImpl(Record record, boolean z, int i, int i2) {
        setDetailRecord(z ? record : null);
        fireShowingDetail(z ? record.detailAdapter : null, i, i2);
    }

    protected void setDetailRecord(Record record) {
        if (record == this.mDetailRecord) {
            return;
        }
        this.mDetailRecord = record;
        fireScanStateChanged((this.mDetailRecord instanceof TileRecord) && ((TileRecord) this.mDetailRecord).scanState);
    }

    void setGridContentVisibility(boolean z) {
        int i = z ? 0 : 4;
        setVisibility(i);
        if (this.mQuickSettingsExt != null) {
            this.mQuickSettingsExt.setViewsVisibility(i);
        }
        if (this.mGridContentVisible != z) {
            this.mMetricsLogger.visibility(com.android.systemui.plugins.R.styleable.AppCompatTheme_windowActionBar, i);
        }
        this.mGridContentVisible = z;
    }

    private void logTiles() {
        for (int i = 0; i < this.mRecords.size(); i++) {
            QSTile qSTile = this.mRecords.get(i).tile;
            this.mMetricsLogger.write(qSTile.populate(new LogMaker(qSTile.getMetricsCategory()).setType(1)));
        }
    }

    private void fireShowingDetail(DetailAdapter detailAdapter, int i, int i2) {
        if (this.mCallback != null) {
            this.mCallback.onShowingDetail(detailAdapter, i, i2);
        }
    }

    private void fireToggleStateChanged(boolean z) {
        if (this.mCallback != null) {
            this.mCallback.onToggleStateChanged(z);
        }
    }

    private void fireScanStateChanged(boolean z) {
        if (this.mCallback != null) {
            this.mCallback.onScanStateChanged(z);
        }
    }

    public void clickTile(ComponentName componentName) {
        String spec = CustomTile.toSpec(componentName);
        int size = this.mRecords.size();
        for (int i = 0; i < size; i++) {
            if (this.mRecords.get(i).tile.getTileSpec().equals(spec)) {
                this.mRecords.get(i).tile.click();
                return;
            }
        }
    }

    QSTileLayout getTileLayout() {
        return this.mTileLayout;
    }

    QSTileView getTileView(QSTile qSTile) {
        for (TileRecord tileRecord : this.mRecords) {
            if (tileRecord.tile == qSTile) {
                return tileRecord.tileView;
            }
        }
        return null;
    }

    public QSSecurityFooter getFooter() {
        return this.mFooter;
    }

    public void showDeviceMonitoringDialog() {
        this.mFooter.showDeviceMonitoringDialog();
    }

    public void setMargins(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (childAt != this.mTileLayout) {
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) childAt.getLayoutParams();
                layoutParams.leftMargin = i;
                layoutParams.rightMargin = i;
            }
        }
    }

    private class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                QSPanel.this.handleShowDetail((Record) message.obj, message.arg1 != 0);
            } else if (message.what == 3) {
                QSPanel.this.announceForAccessibility((CharSequence) message.obj);
            }
        }
    }

    protected static class Record {
        DetailAdapter detailAdapter;
        int x;
        int y;

        protected Record() {
        }
    }

    public interface QSTileLayout {
        void addTile(TileRecord tileRecord);

        int getOffsetTop(TileRecord tileRecord);

        void removeTile(TileRecord tileRecord);

        void setListening(boolean z);

        boolean updateResources();

        default void setExpansion(float f) {
        }
    }
}
