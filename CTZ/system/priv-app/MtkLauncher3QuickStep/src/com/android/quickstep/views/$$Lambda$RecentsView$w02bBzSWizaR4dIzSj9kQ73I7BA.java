package com.android.quickstep.views;

import com.android.systemui.shared.recents.model.RecentsTaskLoadPlan;
import java.util.function.Consumer;

/* compiled from: lambda */
/* renamed from: com.android.quickstep.views.-$$Lambda$RecentsView$w02bBzSWizaR4dIzSj9kQ73I7BA, reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$RecentsView$w02bBzSWizaR4dIzSj9kQ73I7BA implements Consumer {
    private final /* synthetic */ RecentsView f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.quickstep.views.RecentsView.reloadIfNeeded():void, com.android.quickstep.views.RecentsView.setCurrentTask(int):void] */
    public /* synthetic */ $$Lambda$RecentsView$w02bBzSWizaR4dIzSj9kQ73I7BA(RecentsView recentsView) {
        this.f$0 = recentsView;
    }

    /* JADX DEBUG: Class process forced to load method for inline: com.android.quickstep.views.RecentsView.lambda$w02bBzSWizaR4dIzSj9kQ73I7BA(com.android.quickstep.views.RecentsView, com.android.systemui.shared.recents.model.RecentsTaskLoadPlan):void */
    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        this.f$0.applyLoadPlan((RecentsTaskLoadPlan) obj);
    }
}
