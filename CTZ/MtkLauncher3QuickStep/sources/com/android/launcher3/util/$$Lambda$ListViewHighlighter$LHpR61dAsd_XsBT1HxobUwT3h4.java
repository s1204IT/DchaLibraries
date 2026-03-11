package com.android.launcher3.util;

public final class $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4 implements Runnable {
    private final ListViewHighlighter f$0;

    public $$Lambda$ListViewHighlighter$LHpR61dAsd_XsBT1HxobUwT3h4(ListViewHighlighter listViewHighlighter) {
        this.f$0 = listViewHighlighter;
    }

    @Override
    public final void run() {
        this.f$0.tryHighlight();
    }
}
