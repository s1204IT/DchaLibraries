package com.android.systemui.statusbar.phone;

import android.view.View;

public final class $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8 implements View.OnLongClickListener {
    private final NavigationBarFragment f$0;

    public $$Lambda$NavigationBarFragment$dtGeJfWz2E4_XAoQgX8peIw4kU8(NavigationBarFragment navigationBarFragment) {
        this.f$0 = navigationBarFragment;
    }

    @Override
    public final boolean onLongClick(View view) {
        return this.f$0.onLongPressBackRecents(view);
    }
}
