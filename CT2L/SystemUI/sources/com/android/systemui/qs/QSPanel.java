package com.android.systemui.qs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.settings.BrightnessController;
import com.android.systemui.settings.ToggleSlider;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.statusbar.policy.BrightnessMirrorController;
import java.util.ArrayList;
import java.util.Collection;

public class QSPanel extends ViewGroup {
    private BrightnessController mBrightnessController;
    private int mBrightnessPaddingTop;
    private final View mBrightnessView;
    private Callback mCallback;
    private int mCellHeight;
    private int mCellWidth;
    private final QSDetailClipper mClipper;
    private boolean mClosingDetail;
    private int mColumns;
    private final Context mContext;
    private final View mDetail;
    private final ViewGroup mDetailContent;
    private final TextView mDetailDoneButton;
    private Record mDetailRecord;
    private final TextView mDetailSettingsButton;
    private int mDualTileUnderlap;
    private boolean mExpanded;
    private QSFooter mFooter;
    private boolean mGridContentVisible;
    private int mGridHeight;
    private final H mHandler;
    private final AnimatorListenerAdapter mHideGridContentWhenDone;
    private QSTileHost mHost;
    private int mLargeCellHeight;
    private int mLargeCellWidth;
    private boolean mListening;
    private int mPanelPaddingBottom;
    private final ArrayList<TileRecord> mRecords;
    private final AnimatorListenerAdapter mTeardownDetailWhenDone;

    public interface Callback {
        void onScanStateChanged(boolean z);

        void onShowingDetail(QSTile.DetailAdapter detailAdapter);

        void onToggleStateChanged(boolean z);
    }

    public QSPanel(Context context) {
        this(context, null);
    }

    public QSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRecords = new ArrayList<>();
        this.mHandler = new H();
        this.mGridContentVisible = true;
        this.mTeardownDetailWhenDone = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                QSPanel.this.mDetailContent.removeAllViews();
                QSPanel.this.setDetailRecord(null);
                QSPanel.this.mClosingDetail = false;
            }
        };
        this.mHideGridContentWhenDone = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                animation.removeListener(this);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (QSPanel.this.mDetailRecord != null) {
                    QSPanel.this.setGridContentVisibility(false);
                }
            }
        };
        this.mContext = context;
        this.mDetail = LayoutInflater.from(context).inflate(R.layout.qs_detail, (ViewGroup) this, false);
        this.mDetailContent = (ViewGroup) this.mDetail.findViewById(android.R.id.content);
        this.mDetailSettingsButton = (TextView) this.mDetail.findViewById(android.R.id.button2);
        this.mDetailDoneButton = (TextView) this.mDetail.findViewById(android.R.id.button1);
        updateDetailText();
        this.mDetail.setVisibility(8);
        this.mDetail.setClickable(true);
        this.mBrightnessView = LayoutInflater.from(context).inflate(R.layout.quick_settings_brightness_dialog, (ViewGroup) this, false);
        this.mFooter = new QSFooter(this, context);
        addView(this.mDetail);
        addView(this.mBrightnessView);
        addView(this.mFooter.getView());
        this.mClipper = new QSDetailClipper(this.mDetail);
        updateResources();
        this.mBrightnessController = new BrightnessController(getContext(), (ImageView) findViewById(R.id.brightness_icon), (ToggleSlider) findViewById(R.id.brightness_slider));
        this.mDetailDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QSPanel.this.closeDetail();
            }
        });
    }

    private void updateDetailText() {
        this.mDetailDoneButton.setText(R.string.quick_settings_done);
        this.mDetailSettingsButton.setText(R.string.quick_settings_more_settings);
    }

    public void setBrightnessMirror(BrightnessMirrorController c) {
        super.onFinishInflate();
        ToggleSlider brightnessSlider = (ToggleSlider) findViewById(R.id.brightness_slider);
        ToggleSlider mirror = (ToggleSlider) c.getMirror().findViewById(R.id.brightness_slider);
        brightnessSlider.setMirror(mirror);
        brightnessSlider.setMirrorController(c);
    }

    public void setCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void setHost(QSTileHost host) {
        this.mHost = host;
        this.mFooter.setHost(host);
    }

    public QSTileHost getHost() {
        return this.mHost;
    }

    public void updateResources() {
        Resources res = this.mContext.getResources();
        int columns = Math.max(1, res.getInteger(R.integer.quick_settings_num_columns));
        this.mCellHeight = res.getDimensionPixelSize(R.dimen.qs_tile_height);
        this.mCellWidth = (int) (this.mCellHeight * 1.2f);
        this.mLargeCellHeight = res.getDimensionPixelSize(R.dimen.qs_dual_tile_height);
        this.mLargeCellWidth = (int) (this.mLargeCellHeight * 1.2f);
        this.mPanelPaddingBottom = res.getDimensionPixelSize(R.dimen.qs_panel_padding_bottom);
        this.mDualTileUnderlap = res.getDimensionPixelSize(R.dimen.qs_dual_tile_padding_vertical);
        this.mBrightnessPaddingTop = res.getDimensionPixelSize(R.dimen.qs_brightness_padding_top);
        if (this.mColumns != columns) {
            this.mColumns = columns;
            postInvalidate();
        }
        if (this.mListening) {
            refreshAllTiles();
        }
        updateDetailText();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mDetailDoneButton, R.dimen.qs_detail_button_text_size);
        FontSizeUtils.updateFontSize(this.mDetailSettingsButton, R.dimen.qs_detail_button_text_size);
        int count = this.mRecords.size();
        for (int i = 0; i < count; i++) {
            View detailView = this.mRecords.get(i).detailView;
            if (detailView != null) {
                detailView.dispatchConfigurationChanged(newConfig);
            }
        }
        this.mFooter.onConfigurationChanged();
    }

    public void setExpanded(boolean expanded) {
        if (this.mExpanded != expanded) {
            this.mExpanded = expanded;
            if (!this.mExpanded) {
                closeDetail();
            }
        }
    }

    public void setListening(boolean listening) {
        if (this.mListening != listening) {
            this.mListening = listening;
            for (TileRecord r : this.mRecords) {
                r.tile.setListening(this.mListening);
            }
            this.mFooter.setListening(this.mListening);
            if (this.mListening) {
                refreshAllTiles();
            }
            if (listening) {
                this.mBrightnessController.registerCallbacks();
            } else {
                this.mBrightnessController.unregisterCallbacks();
            }
        }
    }

    public void refreshAllTiles() {
        for (TileRecord r : this.mRecords) {
            r.tile.refreshState();
        }
        this.mFooter.refreshState();
    }

    public void showDetailAdapter(boolean show, QSTile.DetailAdapter adapter) {
        Record r = new Record();
        r.detailAdapter = adapter;
        showDetail(show, r);
    }

    public void showDetail(boolean show, Record r) {
        this.mHandler.obtainMessage(1, show ? 1 : 0, 0, r).sendToTarget();
    }

    public void setTileVisibility(View v, int visibility) {
        this.mHandler.obtainMessage(2, visibility, 0, v).sendToTarget();
    }

    public void handleSetTileVisibility(View v, int visibility) {
        if (visibility != v.getVisibility()) {
            v.setVisibility(visibility);
        }
    }

    public void setTiles(Collection<QSTile<?>> tiles) {
        for (TileRecord record : this.mRecords) {
            removeView(record.tileView);
        }
        this.mRecords.clear();
        for (QSTile<?> tile : tiles) {
            addTile(tile);
        }
        if (isShowingDetail()) {
            this.mDetail.bringToFront();
        }
    }

    private void addTile(QSTile<?> tile) {
        final TileRecord r = new TileRecord();
        r.tile = tile;
        r.tileView = tile.createTileView(this.mContext);
        r.tileView.setVisibility(8);
        QSTile.Callback callback = new QSTile.Callback() {
            @Override
            public void onStateChanged(QSTile.State state) {
                int visibility = state.visible ? 0 : 8;
                if (state.visible && !QSPanel.this.mGridContentVisible) {
                    visibility = 4;
                }
                QSPanel.this.setTileVisibility(r.tileView, visibility);
                r.tileView.onStateChanged(state);
            }

            @Override
            public void onShowDetail(boolean show) {
                QSPanel.this.showDetail(show, r);
            }

            @Override
            public void onToggleStateChanged(boolean state) {
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireToggleStateChanged(state);
                }
            }

            @Override
            public void onScanStateChanged(boolean state) {
                r.scanState = state;
                if (QSPanel.this.mDetailRecord == r) {
                    QSPanel.this.fireScanStateChanged(r.scanState);
                }
            }

            @Override
            public void onAnnouncementRequested(CharSequence announcement) {
                QSPanel.this.announceForAccessibility(announcement);
            }
        };
        r.tile.setCallback(callback);
        View.OnClickListener click = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r.tile.click();
            }
        };
        View.OnClickListener clickSecondary = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                r.tile.secondaryClick();
            }
        };
        View.OnLongClickListener longClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                r.tile.longClick();
                return true;
            }
        };
        r.tileView.init(click, clickSecondary, longClick);
        r.tile.setListening(this.mListening);
        callback.onStateChanged(r.tile.getState());
        r.tile.refreshState();
        this.mRecords.add(r);
        addView(r.tileView);
    }

    public boolean isShowingDetail() {
        return this.mDetailRecord != null;
    }

    public void closeDetail() {
        showDetail(false, this.mDetailRecord);
    }

    public boolean isClosingDetail() {
        return this.mClosingDetail;
    }

    public int getGridHeight() {
        return this.mGridHeight;
    }

    public void handleShowDetail(Record r, boolean show) {
        if (r instanceof TileRecord) {
            handleShowDetailTile((TileRecord) r, show);
        } else {
            handleShowDetailImpl(r, show, getWidth(), 0);
        }
    }

    private void handleShowDetailTile(TileRecord r, boolean show) {
        if ((this.mDetailRecord != null) != show) {
            if (show) {
                r.detailAdapter = r.tile.getDetailAdapter();
                if (r.detailAdapter == null) {
                    return;
                }
            }
            int x = r.tileView.getLeft() + (r.tileView.getWidth() / 2);
            int y = r.tileView.getTop() + (r.tileView.getHeight() / 2);
            handleShowDetailImpl(r, show, x, y);
        }
    }

    private void handleShowDetailImpl(Record r, boolean show, int x, int y) {
        Animator.AnimatorListener listener;
        if ((this.mDetailRecord != null) != show) {
            QSTile.DetailAdapter detailAdapter = null;
            if (show) {
                detailAdapter = r.detailAdapter;
                r.detailView = detailAdapter.createDetailView(this.mContext, r.detailView, this.mDetailContent);
                if (r.detailView == null) {
                    throw new IllegalStateException("Must return detail view");
                }
                final Intent settingsIntent = detailAdapter.getSettingsIntent();
                this.mDetailSettingsButton.setVisibility(settingsIntent == null ? 8 : 0);
                this.mDetailSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        QSPanel.this.mHost.startSettingsActivity(settingsIntent);
                    }
                });
                this.mDetailContent.removeAllViews();
                this.mDetail.bringToFront();
                this.mDetailContent.addView(r.detailView);
                setDetailRecord(r);
                listener = this.mHideGridContentWhenDone;
            } else {
                this.mClosingDetail = true;
                setGridContentVisibility(true);
                listener = this.mTeardownDetailWhenDone;
                fireScanStateChanged(false);
            }
            sendAccessibilityEvent(32);
            if (!show) {
                detailAdapter = null;
            }
            fireShowingDetail(detailAdapter);
            this.mClipper.animateCircularClip(x, y, show, listener);
        }
    }

    public void setGridContentVisibility(boolean visible) {
        int newVis = visible ? 0 : 4;
        for (int i = 0; i < this.mRecords.size(); i++) {
            TileRecord tileRecord = this.mRecords.get(i);
            if (tileRecord.tileView.getVisibility() != 8) {
                tileRecord.tileView.setVisibility(newVis);
            }
        }
        this.mBrightnessView.setVisibility(newVis);
        this.mGridContentVisible = visible;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        this.mBrightnessView.measure(exactly(width), 0);
        int brightnessHeight = this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop;
        this.mFooter.getView().measure(exactly(width), 0);
        int r = -1;
        int c = -1;
        int rows = 0;
        boolean rowIsDual = false;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8) {
                if (r == -1 || c == this.mColumns - 1 || rowIsDual != record.tile.supportsDualTargets()) {
                    r++;
                    c = 0;
                    rowIsDual = record.tile.supportsDualTargets();
                } else {
                    c++;
                }
                record.row = r;
                record.col = c;
                rows = r + 1;
            }
        }
        for (TileRecord record2 : this.mRecords) {
            if (record2.tileView.setDual(record2.tile.supportsDualTargets())) {
                record2.tileView.handleStateChanged(record2.tile.getState());
            }
            if (record2.tileView.getVisibility() != 8) {
                int cw = record2.row == 0 ? this.mLargeCellWidth : this.mCellWidth;
                int ch = record2.row == 0 ? this.mLargeCellHeight : this.mCellHeight;
                record2.tileView.measure(exactly(cw), exactly(ch));
            }
        }
        int h = rows == 0 ? brightnessHeight : getRowTop(rows) + this.mPanelPaddingBottom;
        if (this.mFooter.hasFooter()) {
            h += this.mFooter.getView().getMeasuredHeight();
        }
        this.mDetail.measure(exactly(width), 0);
        if (this.mDetail.getMeasuredHeight() < h) {
            this.mDetail.measure(exactly(width), exactly(h));
        }
        this.mGridHeight = h;
        setMeasuredDimension(width, Math.max(h, this.mDetail.getMeasuredHeight()));
    }

    private static int exactly(int size) {
        return View.MeasureSpec.makeMeasureSpec(size, 1073741824);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int right;
        int w = getWidth();
        this.mBrightnessView.layout(0, this.mBrightnessPaddingTop, this.mBrightnessView.getMeasuredWidth(), this.mBrightnessPaddingTop + this.mBrightnessView.getMeasuredHeight());
        boolean isRtl = getLayoutDirection() == 1;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8) {
                int cols = getColumnCount(record.row);
                int cw = record.row == 0 ? this.mLargeCellWidth : this.mCellWidth;
                int extra = (w - (cw * cols)) / (cols + 1);
                int left = (record.col * cw) + ((record.col + 1) * extra);
                int top = getRowTop(record.row);
                int tileWith = record.tileView.getMeasuredWidth();
                if (isRtl) {
                    right = w - left;
                    left = right - tileWith;
                } else {
                    right = left + tileWith;
                }
                record.tileView.layout(left, top, right, record.tileView.getMeasuredHeight() + top);
            }
        }
        int dh = Math.max(this.mDetail.getMeasuredHeight(), getMeasuredHeight());
        this.mDetail.layout(0, 0, this.mDetail.getMeasuredWidth(), dh);
        if (this.mFooter.hasFooter()) {
            View footer = this.mFooter.getView();
            footer.layout(0, getMeasuredHeight() - footer.getMeasuredHeight(), footer.getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private int getRowTop(int row) {
        return row <= 0 ? this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop : (((this.mBrightnessView.getMeasuredHeight() + this.mBrightnessPaddingTop) + this.mLargeCellHeight) - this.mDualTileUnderlap) + ((row - 1) * this.mCellHeight);
    }

    private int getColumnCount(int row) {
        int cols = 0;
        for (TileRecord record : this.mRecords) {
            if (record.tileView.getVisibility() != 8 && record.row == row) {
                cols++;
            }
        }
        return cols;
    }

    private void fireShowingDetail(QSTile.DetailAdapter detail) {
        if (this.mCallback != null) {
            this.mCallback.onShowingDetail(detail);
        }
    }

    public void fireToggleStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onToggleStateChanged(state);
        }
    }

    public void fireScanStateChanged(boolean state) {
        if (this.mCallback != null) {
            this.mCallback.onScanStateChanged(state);
        }
    }

    public void setDetailRecord(Record r) {
        if (r != this.mDetailRecord) {
            this.mDetailRecord = r;
            boolean scanState = (this.mDetailRecord instanceof TileRecord) && ((TileRecord) this.mDetailRecord).scanState;
            fireScanStateChanged(scanState);
        }
    }

    private class H extends Handler {
        private H() {
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                QSPanel.this.handleShowDetail((Record) msg.obj, msg.arg1 != 0);
            } else if (msg.what == 2) {
                QSPanel.this.handleSetTileVisibility((View) msg.obj, msg.arg1);
            }
        }
    }

    private static class Record {
        QSTile.DetailAdapter detailAdapter;
        View detailView;

        private Record() {
        }
    }

    private static final class TileRecord extends Record {
        int col;
        int row;
        boolean scanState;
        QSTile<?> tile;
        QSTileView tileView;

        private TileRecord() {
            super();
        }
    }
}
