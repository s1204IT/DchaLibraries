package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import com.android.systemui.R;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.phone.QSTileHost;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;

public class QuickQSPanel extends QSPanel {
    private QSPanel mFullPanel;
    private View mHeader;
    private int mMaxTiles;
    private final TunerService.Tunable mNumTiles;

    public QuickQSPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mNumTiles = new TunerService.Tunable() {
            @Override
            public void onTuningChanged(String key, String newValue) {
                QuickQSPanel.this.setMaxTiles(QuickQSPanel.this.getNumQuickTiles(QuickQSPanel.this.mContext));
            }
        };
        if (this.mTileLayout != null) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                this.mTileLayout.removeTile(this.mRecords.get(i));
            }
            removeView((View) this.mTileLayout);
        }
        this.mTileLayout = new HeaderTileLayout(context);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout, 1);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        TunerService.get(this.mContext).addTunable(this.mNumTiles, "sysui_qqs_count");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        TunerService.get(this.mContext).removeTunable(this.mNumTiles);
    }

    public void setQSPanelAndHeader(QSPanel fullPanel, View header) {
        this.mFullPanel = fullPanel;
        this.mHeader = header;
    }

    @Override
    protected boolean shouldShowDetail() {
        return !this.mExpanded;
    }

    @Override
    protected void drawTile(QSPanel.TileRecord r, QSTile.State state) {
        if (state instanceof QSTile.SignalState) {
            QSTile.State copy = r.tile.newTileState();
            state.copyTo(copy);
            ((QSTile.SignalState) copy).activityIn = false;
            ((QSTile.SignalState) copy).activityOut = false;
            state = copy;
        }
        super.drawTile(r, state);
    }

    @Override
    protected QSTileBaseView createTileView(QSTile<?> tile, boolean collapsedView) {
        return new QSTileBaseView(this.mContext, tile.createTileView(this.mContext), collapsedView);
    }

    @Override
    public void setHost(QSTileHost host, QSCustomizer customizer) {
        super.setHost(host, customizer);
        setTiles(this.mHost.getTiles());
    }

    public void setMaxTiles(int maxTiles) {
        this.mMaxTiles = maxTiles;
        if (this.mHost == null) {
            return;
        }
        setTiles(this.mHost.getTiles());
    }

    @Override
    protected void onTileClick(QSTile<?> tile) {
        tile.secondaryClick();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!key.equals("qs_show_brightness")) {
            return;
        }
        super.onTuningChanged(key, "0");
    }

    @Override
    public void setTiles(Collection<QSTile<?>> tiles) {
        ArrayList<QSTile<?>> quickTiles = new ArrayList<>();
        for (QSTile<?> tile : tiles) {
            quickTiles.add(tile);
            if (quickTiles.size() == this.mMaxTiles) {
                break;
            }
        }
        super.setTiles(quickTiles, true);
    }

    public int getNumQuickTiles(Context context) {
        return TunerService.get(context).getValue("sysui_qqs_count", 5);
    }

    private static class HeaderTileLayout extends LinearLayout implements QSPanel.QSTileLayout {
        private final Space mEndSpacer;
        private boolean mListening;
        protected final ArrayList<QSPanel.TileRecord> mRecords;

        public HeaderTileLayout(Context context) {
            super(context);
            this.mRecords = new ArrayList<>();
            setClipChildren(false);
            setClipToPadding(false);
            setGravity(16);
            setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
            this.mEndSpacer = new Space(context);
            this.mEndSpacer.setLayoutParams(generateLayoutParams());
            updateDownArrowMargin();
            addView(this.mEndSpacer);
            setOrientation(0);
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateDownArrowMargin();
        }

        private void updateDownArrowMargin() {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) this.mEndSpacer.getLayoutParams();
            params.setMarginStart(this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_expand_margin));
            this.mEndSpacer.setLayoutParams(params);
        }

        @Override
        public void setListening(boolean listening) {
            if (this.mListening == listening) {
                return;
            }
            this.mListening = listening;
            for (QSPanel.TileRecord record : this.mRecords) {
                record.tile.setListening(this, this.mListening);
            }
        }

        @Override
        public void addTile(QSPanel.TileRecord tile) {
            addView(tile.tileView, getChildCount() - 1, generateLayoutParams());
            addView(new Space(this.mContext), getChildCount() - 1, generateSpaceParams());
            this.mRecords.add(tile);
            tile.tile.setListening(this, this.mListening);
        }

        private LinearLayout.LayoutParams generateSpaceParams() {
            int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, size);
            lp.weight = 1.0f;
            lp.gravity = 17;
            return lp;
        }

        private LinearLayout.LayoutParams generateLayoutParams() {
            int size = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.gravity = 17;
            return lp;
        }

        @Override
        public void removeTile(QSPanel.TileRecord tile) {
            int childIndex = getChildIndex(tile.tileView);
            removeViewAt(childIndex);
            removeViewAt(childIndex);
            this.mRecords.remove(tile);
            tile.tile.setListening(this, false);
        }

        private int getChildIndex(QSTileBaseView tileView) {
            int N = getChildCount();
            for (int i = 0; i < N; i++) {
                if (getChildAt(i) == tileView) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getOffsetTop(QSPanel.TileRecord tile) {
            return 0;
        }

        @Override
        public boolean updateResources() {
            return false;
        }

        @Override
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            if (this.mRecords == null || this.mRecords.size() <= 0) {
                return;
            }
            View previousView = this;
            for (QSPanel.TileRecord record : this.mRecords) {
                if (record.tileView.getVisibility() != 8) {
                    previousView = record.tileView.updateAccessibilityOrder(previousView);
                }
            }
            this.mRecords.get(0).tileView.setAccessibilityTraversalAfter(R.id.alarm_status_collapsed);
            this.mRecords.get(this.mRecords.size() - 1).tileView.setAccessibilityTraversalBefore(R.id.expand_indicator);
        }
    }
}
