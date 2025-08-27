package com.android.settings.utils;

import android.content.Context;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.RecyclerView;
import com.android.setupwizardlib.DividerItemDecoration;

/* loaded from: classes.dex */
public class SettingsDividerItemDecoration extends DividerItemDecoration {
    public SettingsDividerItemDecoration(Context context) {
        super(context);
    }

    @Override // com.android.setupwizardlib.DividerItemDecoration
    protected boolean isDividerAllowedAbove(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof PreferenceViewHolder) {
            return ((PreferenceViewHolder) viewHolder).isDividerAllowedAbove();
        }
        return super.isDividerAllowedAbove(viewHolder);
    }

    @Override // com.android.setupwizardlib.DividerItemDecoration
    protected boolean isDividerAllowedBelow(RecyclerView.ViewHolder viewHolder) {
        if (viewHolder instanceof PreferenceViewHolder) {
            return ((PreferenceViewHolder) viewHolder).isDividerAllowedBelow();
        }
        return super.isDividerAllowedBelow(viewHolder);
    }
}
