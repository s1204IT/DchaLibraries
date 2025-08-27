package com.android.systemui.plugins.qs;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.plugins.annotations.ProvidesInterface;

@ProvidesInterface(version = 1)
/* loaded from: classes.dex */
public interface DetailAdapter {
    public static final int VERSION = 1;

    View createDetailView(Context context, View view, ViewGroup viewGroup);

    int getMetricsCategory();

    Intent getSettingsIntent();

    CharSequence getTitle();

    Boolean getToggleState();

    void setToggleState(boolean z);

    default boolean getToggleEnabled() {
        return true;
    }

    default boolean hasHeader() {
        return true;
    }
}
