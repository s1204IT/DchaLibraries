package com.android.launcher3.util;

/* compiled from: lambda */
/* renamed from: com.android.launcher3.util.-$$Lambda$ListViewHighlighter$LHpR61dAsd_XsB-T1HxobUwT3h4, reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4 implements Runnable {
    private final /* synthetic */ ListViewHighlighter f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.launcher3.util.ListViewHighlighter.<init>(android.widget.ListView, int):void, com.android.launcher3.util.ListViewHighlighter.onLayoutChange(android.view.View, int, int, int, int, int, int, int, int):void] */
    public /* synthetic */ $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4(ListViewHighlighter listViewHighlighter) {
        this.f$0 = listViewHighlighter;
    }

    /* JADX DEBUG: Class process forced to load method for inline: com.android.launcher3.util.ListViewHighlighter.lambda$LHpR61dAsd_XsB-T1HxobUwT3h4(com.android.launcher3.util.ListViewHighlighter):void */
    @Override // java.lang.Runnable
    public final void run() {
        this.f$0.tryHighlight();
    }
}
