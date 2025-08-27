package com.android.systemui.statusbar.phone;

import android.view.View;

/* compiled from: lambda */
/* renamed from: com.android.systemui.statusbar.phone.-$$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8, reason: invalid class name */
/* loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8 implements View.OnLongClickListener {
    private final /* synthetic */ NavigationBarFragment f$0;

    /* JADX DEBUG: Marked for inline */
    /* JADX DEBUG: Method not inlined, still used in: [com.android.systemui.statusbar.phone.NavigationBarFragment.prepareNavigationBarView():void, com.android.systemui.statusbar.phone.NavigationBarFragment.updateScreenPinningGestures():void] */
    public /* synthetic */ $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8(NavigationBarFragment navigationBarFragment) {
        this.f$0 = navigationBarFragment;
    }

    /* JADX DEBUG: Class process forced to load method for inline: com.android.systemui.statusbar.phone.NavigationBarFragment.lambda$dtGeJfWz2E4_XAoQgX8peIw4kU8(com.android.systemui.statusbar.phone.NavigationBarFragment, android.view.View):boolean */
    @Override // android.view.View.OnLongClickListener
    public final boolean onLongClick(View view) {
        return this.f$0.onLongPressBackRecents(view);
    }
}
