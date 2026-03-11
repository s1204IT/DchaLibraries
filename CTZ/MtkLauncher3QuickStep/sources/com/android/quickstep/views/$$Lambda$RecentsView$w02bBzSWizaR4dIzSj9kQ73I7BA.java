package com.android.quickstep.views;

import com.android.systemui.shared.recents.model.RecentsTaskLoadPlan;
import java.util.function.Consumer;

public final class $$Lambda$RecentsView$w02bBzSWizaR4dIzSj9kQ73I7BA implements Consumer {
    private final RecentsView f$0;

    public $$Lambda$RecentsView$w02bBzSWizaR4dIzSj9kQ73I7BA(RecentsView recentsView) {
        this.f$0 = recentsView;
    }

    @Override
    public final void accept(Object obj) {
        this.f$0.applyLoadPlan((RecentsTaskLoadPlan) obj);
    }
}
