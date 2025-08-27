package com.android.systemui.qs;

import android.content.Context;
import android.os.Handler;
import android.util.ArraySet;
import com.android.systemui.Prefs;
import com.android.systemui.plugins.qs.QSTile;
import com.android.systemui.qs.QSTileRevealController;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

/* loaded from: classes.dex */
public class QSTileRevealController {
    private final Context mContext;
    private final PagedTileLayout mPagedTileLayout;
    private final QSPanel mQSPanel;
    private final ArraySet<String> mTilesToReveal = new ArraySet<>();
    private final Handler mHandler = new Handler();
    private final Runnable mRevealQsTiles = new AnonymousClass1();

    /* renamed from: com.android.systemui.qs.QSTileRevealController$1 */
    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override // java.lang.Runnable
        public void run() {
            QSTileRevealController.this.mPagedTileLayout.startTileReveal(QSTileRevealController.this.mTilesToReveal, new Runnable() { // from class: com.android.systemui.qs.-$$Lambda$QSTileRevealController$1$gTMt7U-W3YL6K0ko8X3nSQ3r95I
                @Override // java.lang.Runnable
                public final void run() {
                    QSTileRevealController.AnonymousClass1.lambda$run$0(this.f$0);
                }
            });
        }

        public static /* synthetic */ void lambda$run$0(AnonymousClass1 anonymousClass1) {
            if (QSTileRevealController.this.mQSPanel.isExpanded()) {
                QSTileRevealController.this.addTileSpecsToRevealed(QSTileRevealController.this.mTilesToReveal);
                QSTileRevealController.this.mTilesToReveal.clear();
            }
        }
    }

    QSTileRevealController(Context context, QSPanel qSPanel, PagedTileLayout pagedTileLayout) {
        this.mContext = context;
        this.mQSPanel = qSPanel;
        this.mPagedTileLayout = pagedTileLayout;
    }

    public void setExpansion(float f) {
        if (f == 1.0f) {
            this.mHandler.postDelayed(this.mRevealQsTiles, 500L);
        } else {
            this.mHandler.removeCallbacks(this.mRevealQsTiles);
        }
    }

    public void updateRevealedTiles(Collection<QSTile> collection) {
        ArraySet<String> arraySet = new ArraySet<>();
        Iterator<QSTile> it = collection.iterator();
        while (it.hasNext()) {
            arraySet.add(it.next().getTileSpec());
        }
        Set<String> stringSet = Prefs.getStringSet(this.mContext, "QsTileSpecsRevealed", Collections.EMPTY_SET);
        if (stringSet.isEmpty() || this.mQSPanel.isShowingCustomize()) {
            addTileSpecsToRevealed(arraySet);
        } else {
            arraySet.removeAll(stringSet);
            this.mTilesToReveal.addAll((ArraySet<? extends String>) arraySet);
        }
    }

    private void addTileSpecsToRevealed(ArraySet<String> arraySet) {
        ArraySet arraySet2 = new ArraySet(Prefs.getStringSet(this.mContext, "QsTileSpecsRevealed", Collections.EMPTY_SET));
        arraySet2.addAll((ArraySet) arraySet);
        Prefs.putStringSet(this.mContext, "QsTileSpecsRevealed", arraySet2);
    }
}
