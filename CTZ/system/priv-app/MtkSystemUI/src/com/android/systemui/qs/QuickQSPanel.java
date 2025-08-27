package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Space;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.plugins.qs.QSTileView;
import com.android.systemui.qs.QSPanel;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.tuner.TunerService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/* loaded from: classes.dex */
public class QuickQSPanel extends QSPanel {
    private boolean mDisabledByPolicy;
    protected QSPanel mFullPanel;
    private int mMaxTiles;
    private final TunerService.Tunable mNumTiles;

    public QuickQSPanel(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mNumTiles = new TunerService.Tunable() { // from class: com.android.systemui.qs.QuickQSPanel.1
            AnonymousClass1() {
            }

            @Override // com.android.systemui.tuner.TunerService.Tunable
            public void onTuningChanged(String str, String str2) {
                QuickQSPanel.this.setMaxTiles(QuickQSPanel.getNumQuickTiles(QuickQSPanel.this.mContext));
            }
        };
        if (this.mFooter != null) {
            removeView(this.mFooter.getView());
        }
        if (this.mTileLayout != null) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                this.mTileLayout.removeTile(this.mRecords.get(i));
            }
            removeView((View) this.mTileLayout);
        }
        this.mTileLayout = new HeaderTileLayout(context);
        this.mTileLayout.setListening(this.mListening);
        addView((View) this.mTileLayout, 0);
        super.setPadding(0, 0, 0, 0);
    }

    @Override // android.view.View
    public void setPadding(int i, int i2, int i3, int i4) {
    }

    @Override // com.android.systemui.qs.QSPanel
    protected void addDivider() {
    }

    @Override // com.android.systemui.qs.QSPanel, android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((TunerService) Dependency.get(TunerService.class)).addTunable(this.mNumTiles, "sysui_qqs_count");
    }

    @Override // com.android.systemui.qs.QSPanel, android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((TunerService) Dependency.get(TunerService.class)).removeTunable(this.mNumTiles);
    }

    public void setQSPanelAndHeader(QSPanel qSPanel, View view) {
        this.mFullPanel = qSPanel;
    }

    @Override // com.android.systemui.qs.QSPanel
    protected boolean shouldShowDetail() {
        return !this.mExpanded;
    }

    @Override // com.android.systemui.qs.QSPanel
    protected void drawTile(QSPanel.TileRecord tileRecord, QSTile.State state) {
        if (state instanceof QSTile.SignalState) {
            QSTile.SignalState signalState = new QSTile.SignalState();
            state.copyTo(signalState);
            signalState.activityIn = false;
            signalState.activityOut = false;
            state = signalState;
        }
        super.drawTile(tileRecord, state);
    }

    @Override // com.android.systemui.qs.QSPanel
    public void setHost(QSTileHost qSTileHost, QSCustomizer qSCustomizer) {
        super.setHost(qSTileHost, qSCustomizer);
        setTiles(this.mHost.getTiles());
    }

    public void setMaxTiles(int i) {
        this.mMaxTiles = i;
        if (this.mHost != null) {
            setTiles(this.mHost.getTiles());
        }
    }

    @Override // com.android.systemui.qs.QSPanel, com.android.systemui.tuner.TunerService.Tunable
    public void onTuningChanged(String str, String str2) {
        if ("qs_show_brightness".equals(str)) {
            super.onTuningChanged(str, "0");
        }
    }

    @Override // com.android.systemui.qs.QSPanel
    public void setTiles(Collection<QSTile> collection) {
        ArrayList arrayList = new ArrayList();
        Iterator<QSTile> it = collection.iterator();
        while (it.hasNext()) {
            arrayList.add(it.next());
            if (arrayList.size() == this.mMaxTiles) {
                break;
            }
        }
        super.setTiles(arrayList, true);
    }

    /* renamed from: com.android.systemui.qs.QuickQSPanel$1 */
    class AnonymousClass1 implements TunerService.Tunable {
        AnonymousClass1() {
        }

        @Override // com.android.systemui.tuner.TunerService.Tunable
        public void onTuningChanged(String str, String str2) {
            QuickQSPanel.this.setMaxTiles(QuickQSPanel.getNumQuickTiles(QuickQSPanel.this.mContext));
        }
    }

    public static int getNumQuickTiles(Context context) {
        return ((TunerService) Dependency.get(TunerService.class)).getValue("sysui_qqs_count", 6);
    }

    void setDisabledByPolicy(boolean z) {
        if (z != this.mDisabledByPolicy) {
            this.mDisabledByPolicy = z;
            setVisibility(z ? 8 : 0);
        }
    }

    @Override // android.view.View
    public void setVisibility(int i) {
        if (this.mDisabledByPolicy) {
            if (getVisibility() == 8) {
                return;
            } else {
                i = 8;
            }
        }
        super.setVisibility(i);
    }

    private static class HeaderTileLayout extends LinearLayout implements QSPanel.QSTileLayout {
        private boolean mListening;
        protected final ArrayList<QSPanel.TileRecord> mRecords;
        private int mTileDimensionSize;

        public HeaderTileLayout(Context context) {
            super(context);
            this.mRecords = new ArrayList<>();
            setClipChildren(false);
            setClipToPadding(false);
            this.mTileDimensionSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size);
            setGravity(17);
            setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        }

        @Override // android.view.View
        protected void onConfigurationChanged(Configuration configuration) {
            super.onConfigurationChanged(configuration);
            setGravity(17);
            LinearLayout.LayoutParams layoutParamsGenerateSpaceLayoutParams = generateSpaceLayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_space_width));
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childAt = getChildAt(i);
                if (childAt instanceof Space) {
                    childAt.setLayoutParams(layoutParamsGenerateSpaceLayoutParams);
                }
            }
        }

        private LinearLayout.LayoutParams generateSpaceLayoutParams(int i) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(i, this.mTileDimensionSize);
            if (i == 0) {
                layoutParams.weight = 1.0f;
            }
            layoutParams.gravity = 17;
            return layoutParams;
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout
        public void setListening(boolean z) {
            if (this.mListening == z) {
                return;
            }
            this.mListening = z;
            Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
            while (it.hasNext()) {
                it.next().tile.setListening(this, this.mListening);
            }
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout
        public void addTile(QSPanel.TileRecord tileRecord) {
            if (getChildCount() != 0) {
                addView(new Space(this.mContext), getChildCount(), generateSpaceLayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_space_width)));
            }
            addView(tileRecord.tileView, getChildCount(), generateTileLayoutParams());
            this.mRecords.add(tileRecord);
            tileRecord.tile.setListening(this, this.mListening);
        }

        private LinearLayout.LayoutParams generateTileLayoutParams() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(this.mTileDimensionSize, this.mTileDimensionSize);
            layoutParams.gravity = 17;
            return layoutParams;
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout
        public void removeTile(QSPanel.TileRecord tileRecord) {
            int childIndex = getChildIndex(tileRecord.tileView);
            removeViewAt(childIndex);
            if (getChildCount() != 0) {
                removeViewAt(childIndex);
            }
            this.mRecords.remove(tileRecord);
            tileRecord.tile.setListening(this, false);
        }

        private int getChildIndex(QSTileView qSTileView) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                if (getChildAt(i) == qSTileView) {
                    return i;
                }
            }
            return -1;
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout
        public int getOffsetTop(QSPanel.TileRecord tileRecord) {
            return 0;
        }

        @Override // com.android.systemui.qs.QSPanel.QSTileLayout
        public boolean updateResources() {
            return false;
        }

        @Override // android.view.View
        public boolean hasOverlappingRendering() {
            return false;
        }

        @Override // android.widget.LinearLayout, android.view.View
        protected void onMeasure(int i, int i2) {
            super.onMeasure(i, i2);
            if (this.mRecords != null && this.mRecords.size() > 0) {
                Iterator<QSPanel.TileRecord> it = this.mRecords.iterator();
                View viewUpdateAccessibilityOrder = this;
                while (it.hasNext()) {
                    QSPanel.TileRecord next = it.next();
                    if (next.tileView.getVisibility() != 8) {
                        viewUpdateAccessibilityOrder = next.tileView.updateAccessibilityOrder(viewUpdateAccessibilityOrder);
                    }
                }
                this.mRecords.get(0).tileView.setAccessibilityTraversalAfter(R.id.alarm_status_collapsed);
                this.mRecords.get(this.mRecords.size() - 1).tileView.setAccessibilityTraversalBefore(R.id.expand_indicator);
            }
        }
    }
}
