package com.mediatek.settings.ext;

import android.content.Context;
import android.view.View;

public class DefaultDataUsageSummaryExt implements IDataUsageSummaryExt {
    public DefaultDataUsageSummaryExt(Context context) {
    }

    @Override
    public boolean onDisablingData(int subId) {
        return true;
    }

    @Override
    public boolean isAllowDataEnable(int subId) {
        return true;
    }

    @Override
    public void onBindViewHolder(Context context, View view, View.OnClickListener listener) {
    }
}
